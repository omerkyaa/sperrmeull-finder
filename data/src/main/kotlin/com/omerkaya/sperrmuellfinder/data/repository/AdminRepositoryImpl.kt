package com.omerkaya.sperrmuellfinder.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.data.dto.ReportDto
import com.omerkaya.sperrmuellfinder.data.mapper.toAdminLog
import com.omerkaya.sperrmuellfinder.data.mapper.toAdminRole
import com.omerkaya.sperrmuellfinder.data.mapper.toDomain
import com.omerkaya.sperrmuellfinder.data.mapper.toFirestoreString
import com.omerkaya.sperrmuellfinder.data.mapper.toModerationQueue
import com.omerkaya.sperrmuellfinder.data.paging.AdminLogsPagingSource
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants
import com.omerkaya.sperrmuellfinder.data.paging.ModerationQueuePagingSource
import com.omerkaya.sperrmuellfinder.data.paging.ReportsPagingSource
import com.omerkaya.sperrmuellfinder.data.paging.UserSearchPagingSource
import com.omerkaya.sperrmuellfinder.domain.model.AdminLog
import com.omerkaya.sperrmuellfinder.domain.model.AdminRole
import com.omerkaya.sperrmuellfinder.domain.model.ModerationPriority
import com.omerkaya.sperrmuellfinder.domain.model.ModerationQueueEntry
import com.omerkaya.sperrmuellfinder.domain.model.Report
import com.omerkaya.sperrmuellfinder.domain.model.ReportStatus
import com.omerkaya.sperrmuellfinder.domain.model.ReportTargetType
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.repository.AdminRepository
import com.omerkaya.sperrmuellfinder.domain.repository.DashboardStats
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AdminRepository
 * Handles all admin-related operations with Firebase
 * Rules.md compliant - Clean Architecture data layer
 */
@Singleton
class AdminRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val logger: Logger
) : AdminRepository {
    
    companion object {
        private const val TAG = "AdminRepository"
        private const val PAGE_SIZE = 20
    }
    
    // ========================================
    // ADMIN ROLE MANAGEMENT
    // ========================================
    
    override suspend fun isAdmin(userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val targetUserId = if (userId.isEmpty()) {
                firebaseAuth.currentUser?.uid ?: return@withContext Result.Success(false)
            } else {
                userId
            }
            
            val userDoc = firestore.collection(FirestoreConstants.Collections.USERS)
                .document(targetUserId)
                .get()
                .await()
            
            val isAdmin = userDoc.getBoolean("isAdmin") ?: false
            val isModerator = userDoc.getBoolean("isModerator") ?: false
            val isSuperAdmin = userDoc.getBoolean("isSuperAdmin") ?: false
            
            Result.Success(isAdmin || isModerator || isSuperAdmin)
        } catch (e: Exception) {
            logger.e(TAG, "Error checking admin status", e)
            Result.Error(e)
        }
    }
    
    override suspend fun getAdminRole(userId: String): Result<AdminRole?> = withContext(Dispatchers.IO) {
        try {
            val targetUserId = if (userId.isEmpty()) {
                firebaseAuth.currentUser?.uid ?: return@withContext Result.Success(null)
            } else {
                userId
            }
            
            val userDoc = firestore.collection(FirestoreConstants.Collections.USERS)
                .document(targetUserId)
                .get()
                .await()
            
            val role = when {
                userDoc.getBoolean("isSuperAdmin") == true -> AdminRole.SUPER_ADMIN
                userDoc.getBoolean("isAdmin") == true -> AdminRole.ADMIN
                userDoc.getBoolean("isModerator") == true -> AdminRole.MODERATOR
                else -> null
            }
            
            Result.Success(role)
        } catch (e: Exception) {
            logger.e(TAG, "Error getting admin role", e)
            Result.Error(e)
        }
    }
    
    override suspend fun getCurrentAdminRole(): Result<AdminRole?> = withContext(Dispatchers.IO) {
        try {
            val user = firebaseAuth.currentUser
            if (user == null) {
                logger.d(TAG, "No authenticated user")
                return@withContext Result.Success(null)
            }
            
            val tokenResult = user.getIdToken(true).await()
            val claims = tokenResult.claims
            
            val role = when {
                claims["super_admin"] == true -> AdminRole.SUPER_ADMIN
                claims["admin"] == true -> AdminRole.ADMIN
                claims["moderator"] == true -> AdminRole.MODERATOR
                else -> null
            }
            
            logger.d(TAG, "Current admin role: $role")
            Result.Success(role)
        } catch (e: Exception) {
            logger.e(TAG, "Error getting admin role", e)
            Result.Error(e)
        }
    }
    
    override fun observeAdminRole(): Flow<AdminRole?> = callbackFlow {
        val listener = FirebaseAuth.IdTokenListener { auth ->
            auth.currentUser?.getIdToken(false)?.addOnSuccessListener { tokenResult ->
                val role = when {
                    tokenResult.claims["super_admin"] == true -> AdminRole.SUPER_ADMIN
                    tokenResult.claims["admin"] == true -> AdminRole.ADMIN
                    tokenResult.claims["moderator"] == true -> AdminRole.MODERATOR
                    else -> null
                }
                trySend(role)
            }?.addOnFailureListener { exception ->
                logger.e(TAG, "Error observing admin role", exception)
                trySend(null)
            }
        }
        
        firebaseAuth.addIdTokenListener(listener)
        
        // Emit initial value
        firebaseAuth.currentUser?.getIdToken(false)?.addOnSuccessListener { tokenResult ->
            val role = when {
                tokenResult.claims["super_admin"] == true -> AdminRole.SUPER_ADMIN
                tokenResult.claims["admin"] == true -> AdminRole.ADMIN
                tokenResult.claims["moderator"] == true -> AdminRole.MODERATOR
                else -> null
            }
            trySend(role)
        }
        
        awaitClose { firebaseAuth.removeIdTokenListener(listener) }
    }
    
    override suspend fun setAdminRole(userId: String, role: AdminRole): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val data = hashMapOf(
                "uid" to userId,
                "role" to role.toFirestoreString()
            )
            
            val result = functions
                .getHttpsCallable("setAdminRole")
                .call(data)
                .await()
            
            logger.i(TAG, "Admin role set successfully: $role for user $userId")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Error setting admin role", e)
            Result.Error(e)
        }
    }
    
    override suspend fun removeAdminRole(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val data = hashMapOf("uid" to userId)
            
            val result = functions
                .getHttpsCallable("removeAdminRole")
                .call(data)
                .await()
            
            logger.i(TAG, "Admin role removed successfully for user $userId")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Error removing admin role", e)
            Result.Error(e)
        }
    }
    
    // ========================================
    // REPORT MANAGEMENT
    // ========================================
    
    override fun getReports(
        status: ReportStatus?,
        priority: ModerationPriority?,
        type: ReportTargetType?
    ): Flow<PagingData<Report>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                ReportsPagingSource(
                    firestore = firestore,
                    status = status,
                    priority = priority,
                    type = type,
                    logger = logger
                )
            }
        ).flow
    }
    
    override suspend fun getReport(reportId: String): Result<Report> = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("reports")
                .document(reportId)
                .get()
                .await()
            
            if (!doc.exists()) {
                return@withContext Result.Error(Exception("Report not found"))
            }

            val reportDto = doc.toObject(ReportDto::class.java)
                ?: return@withContext Result.Error(Exception("Failed to parse report"))

            Result.Success(reportDto.toDomain())
        } catch (e: Exception) {
            logger.e(TAG, "Error getting report", e)
            Result.Error(e)
        }
    }
    
    override suspend fun assignReport(reportId: String, adminId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("reports")
                .document(reportId)
                .update(
                    mapOf(
                        "assignedTo" to adminId,
                        "status" to "in_review"
                    )
                )
                .await()
            
            logger.i(TAG, "Report $reportId assigned to admin $adminId")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Error assigning report", e)
            Result.Error(e)
        }
    }
    
    override suspend fun dismissReport(reportId: String, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val data = hashMapOf(
                "reportId" to reportId,
                "reason" to reason
            )
            
            functions.getHttpsCallable("dismissReport")
                .call(data)
                .await()
            
            logger.i(TAG, "Report dismissed: $reportId")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Error dismissing report", e)
            Result.Error(e)
        }
    }
    
    override suspend fun warnUser(reportId: String, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val data = hashMapOf(
                "reportId" to reportId,
                "reason" to reason
            )
            
            functions.getHttpsCallable("warnUser")
                .call(data)
                .await()
            
            logger.i(TAG, "User warned for report: $reportId")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Error warning user", e)
            Result.Error(e)
        }
    }
    
    override suspend fun deleteReportedContent(reportId: String, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val data = hashMapOf(
                "reportId" to reportId,
                "reason" to reason
            )
            
            functions.getHttpsCallable("deleteReportedContent")
                .call(data)
                .await()
            
            logger.i(TAG, "Reported content deleted: $reportId")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Error deleting reported content", e)
            Result.Error(e)
        }
    }
    
    // ========================================
    // USER MANAGEMENT - 3-Tier Ban System
    // ========================================
    
    override fun searchUsers(query: String): Flow<PagingData<User>> {
        return Pager(
            config = PagingConfig(
                pageSize = 50,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                UserSearchPagingSource(
                    firestore = firestore,
                    query = query,
                    logger = logger
                )
            }
        ).flow
    }
    
    override suspend fun getUserById(userId: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("users")
                .document(userId)
                .get()
                .await()
            
            if (!doc.exists()) {
                return@withContext Result.Error(Exception("User not found"))
            }
            
            // TODO: Map document to User domain model
            // This will use existing UserMapper
            Result.Error(Exception("Not implemented yet"))
        } catch (e: Exception) {
            logger.e(TAG, "Error getting user", e)
            Result.Error(e)
        }
    }
    
    override suspend fun softBanUser(
        reportId: String?,
        userId: String,
        durationDays: Int,
        reason: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val data = hashMapOf(
                "userId" to userId,
                "durationDays" to durationDays,
                "reason" to reason
            )
            if (reportId != null) {
                data["reportId"] = reportId
            }
            
            functions.getHttpsCallable("softBanUser")
                .call(data)
                .await()
            
            logger.i(TAG, "User soft banned: $userId for $durationDays days")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Error soft banning user", e)
            Result.Error(e)
        }
    }
    
    override suspend fun hardBanUser(
        reportId: String?,
        userId: String,
        reason: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val data = hashMapOf(
                "userId" to userId,
                "reason" to reason
            )
            if (reportId != null) {
                data["reportId"] = reportId
            }
            
            functions.getHttpsCallable("hardBanUser")
                .call(data)
                .await()
            
            logger.i(TAG, "User hard banned (permanent): $userId")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Error hard banning user", e)
            Result.Error(e)
        }
    }
    
    override suspend fun deleteUserAccount(
        reportId: String?,
        userId: String,
        reason: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val data = hashMapOf(
                "userId" to userId,
                "reason" to reason
            )
            if (reportId != null) {
                data["reportId"] = reportId
            }
            
            functions.getHttpsCallable("deleteUserAccount")
                .call(data)
                .await()
            
            logger.i(TAG, "User account deleted: $userId")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Error deleting user account", e)
            Result.Error(e)
        }
    }
    
    override suspend fun unbanUser(userId: String, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val data = hashMapOf(
                "userId" to userId,
                "reason" to reason
            )
            
            functions.getHttpsCallable("unbanUser")
                .call(data)
                .await()
            
            logger.i(TAG, "User unbanned: $userId")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Error unbanning user", e)
            Result.Error(e)
        }
    }
    
    // ========================================
    // PREMIUM MANAGEMENT
    // ========================================
    
    override suspend fun grantPremium(
        userId: String,
        durationDays: Int,
        reason: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val data = hashMapOf(
                "userId" to userId,
                "duration" to durationDays,
                "reason" to reason
            )
            
            functions.getHttpsCallable("grantPremium")
                .call(data)
                .await()
            
            logger.i(TAG, "Premium granted to user: $userId for $durationDays days")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Error granting premium", e)
            Result.Error(e)
        }
    }
    
    override suspend fun revokePremium(userId: String, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val data = hashMapOf(
                "userId" to userId,
                "reason" to reason
            )
            
            functions.getHttpsCallable("revokePremium")
                .call(data)
                .await()
            
            logger.i(TAG, "Premium revoked from user: $userId")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Error revoking premium", e)
            Result.Error(e)
        }
    }
    
    // ========================================
    // CONTENT DELETION
    // ========================================
    
    override suspend fun deletePost(postId: String, reason: String, reportId: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val data = hashMapOf(
                "postId" to postId,
                "reason" to reason
            )
            if (reportId != null) {
                data["reportId"] = reportId
            }
            
            functions.getHttpsCallable("deletePost")
                .call(data)
                .await()
            
            logger.i(TAG, "Post deleted: $postId")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Error deleting post", e)
            Result.Error(e)
        }
    }
    
    override suspend fun deleteComment(commentId: String, reason: String, reportId: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val data = hashMapOf(
                "commentId" to commentId,
                "reason" to reason
            )
            if (reportId != null) {
                data["reportId"] = reportId
            }
            
            functions.getHttpsCallable("deleteComment")
                .call(data)
                .await()
            
            logger.i(TAG, "Comment deleted: $commentId")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Error deleting comment", e)
            Result.Error(e)
        }
    }
    
    // ========================================
    // NOTIFICATIONS
    // ========================================
    
    override suspend fun sendBroadcastNotification(
        targetType: String,
        targetIds: List<String>?,
        title: String,
        body: String,
        deeplink: String?
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val data = hashMapOf<String, Any>(
                "targetType" to targetType,
                "title" to title,
                "body" to body
            )
            if (targetIds != null) {
                data["targetIds"] = targetIds
            }
            if (deeplink != null) {
                data["deeplink"] = deeplink
            }
            
            val result = functions.getHttpsCallable("sendAdminNotification")
                .call(data)
                .await()
            
            val count = (result.data as? Map<*, *>)?.get("count") as? Int ?: 0
            
            logger.i(TAG, "Broadcast notification sent to $count users")
            Result.Success(count)
        } catch (e: Exception) {
            logger.e(TAG, "Error sending broadcast notification", e)
            Result.Error(e)
        }
    }
    
    // ========================================
    // ANALYTICS & LOGS
    // ========================================
    
    override fun getAdminLogs(
        adminId: String?,
        action: String?
    ): Flow<PagingData<AdminLog>> {
        return Pager(
            config = PagingConfig(
                pageSize = 100,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                AdminLogsPagingSource(
                    firestore = firestore,
                    adminId = adminId,
                    action = action,
                    logger = logger
                )
            }
        ).flow
    }
    
    override suspend fun getDashboardStats(): Result<DashboardStats> = withContext(Dispatchers.IO) {
        try {
            // TODO: Implement dashboard stats aggregation
            // This will query multiple collections and aggregate data
            Result.Error(Exception("Not implemented yet"))
        } catch (e: Exception) {
            logger.e(TAG, "Error getting dashboard stats", e)
            Result.Error(e)
        }
    }
    
    override fun getModerationQueue(): Flow<PagingData<ModerationQueueEntry>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                ModerationQueuePagingSource(
                    firestore = firestore,
                    logger = logger
                )
            }
        ).flow
    }
}
