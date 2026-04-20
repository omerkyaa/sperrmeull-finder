package com.omerkaya.sperrmuellfinder.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.data.mapper.toDomain
import com.omerkaya.sperrmuellfinder.data.mapper.toDto
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants
import com.omerkaya.sperrmuellfinder.domain.model.Report
import com.omerkaya.sperrmuellfinder.domain.model.ReportPriority
import com.omerkaya.sperrmuellfinder.domain.model.ReportTargetType
import com.omerkaya.sperrmuellfinder.domain.repository.ReportRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ReportRepository
 * Rules.md compliant - Clean Architecture data layer
 */
@Singleton
class ReportRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val logger: Logger
) : ReportRepository {

    companion object {
        private const val TAG = "ReportRepositoryImpl"
        private const val RATE_LIMIT_HOURS = 24
        private const val TARGET_CONTENT_MAX_LENGTH = 100
    }

    override suspend fun createReport(report: Report): Result<String> {
        return try {
            val currentUser = firebaseAuth.currentUser
                ?: return Result.Error(Exception("User not authenticated"))

            // Get current user info
            val userDoc = firestore.collection(FirestoreConstants.USERS)
                .document(currentUser.uid)
                .get()
                .await()

            val reporterName = userDoc.getString(FirestoreConstants.User.DISPLAY_NAME) ?: "Unknown"
            val reporterPhotoUrl = userDoc.getString(FirestoreConstants.User.PHOTO_URL)

            // Get target details for denormalization
            val (targetOwnerId, targetOwnerName, targetContent, targetImageUrl) = 
                getTargetDetails(report.targetId, report.type)

            // Create report document
            val reportRef = firestore.collection(FirestoreConstants.REPORTS).document()
            
            val reportData = report.copy(
                id = reportRef.id,
                reporterId = currentUser.uid,
                reporterName = reporterName,
                reporterPhotoUrl = reporterPhotoUrl,
                targetOwnerId = targetOwnerId,
                targetOwnerName = targetOwnerName,
                targetContent = targetContent,
                targetImageUrl = targetImageUrl
            ).toDto()

            reportRef.set(reportData).await()

            // Create admin notification for high priority reports
            if (report.priority == ReportPriority.HIGH || report.priority == ReportPriority.CRITICAL) {
                createAdminNotification(reportRef.id, report.type, report.reason.displayName)
            }

            logger.d(TAG, "Report created successfully: ${reportRef.id}")
            Result.Success(reportRef.id)
        } catch (e: Exception) {
            logger.e(TAG, "Error creating report", e)
            Result.Error(e)
        }
    }

    override fun getUserReports(): Flow<List<Report>> = callbackFlow {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(FirestoreConstants.REPORTS)
            .whereEqualTo(FirestoreConstants.Report.REPORTER_ID, currentUser.uid)
            .orderBy(FirestoreConstants.Report.CREATED_AT, com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logger.e(TAG, "Error listening to user reports", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val reports = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(com.omerkaya.sperrmuellfinder.data.dto.ReportDto::class.java)
                                ?.toDomain()
                        } catch (e: Exception) {
                            logger.e(TAG, "Error parsing report", e)
                            null
                        }
                    }
                    trySend(reports)
                }
            }

        awaitClose { listener.remove() }
    }

    override suspend fun canUserReport(targetId: String, type: ReportTargetType): Result<Boolean> {
        return try {
            val currentUser = firebaseAuth.currentUser
                ?: return Result.Error(Exception("User not authenticated"))

            // Check if user has reported this target in the last 24 hours
            val cutoffTime = Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(RATE_LIMIT_HOURS.toLong()))

            val existingReports = firestore.collection(FirestoreConstants.REPORTS)
                .whereEqualTo(FirestoreConstants.Report.REPORTER_ID, currentUser.uid)
                .whereEqualTo(FirestoreConstants.Report.TARGET_ID, targetId)
                .whereEqualTo(FirestoreConstants.Report.TYPE, type.name)
                .whereGreaterThan(FirestoreConstants.Report.CREATED_AT, cutoffTime)
                .get()
                .await()

            val canReport = existingReports.isEmpty
            Result.Success(canReport)
        } catch (e: Exception) {
            logger.e(TAG, "Error checking report eligibility", e)
            Result.Error(e)
        }
    }

    /**
     * Get target details for denormalization
     */
    private suspend fun getTargetDetails(
        targetId: String,
        type: ReportTargetType
    ): TargetDetails {
        return try {
            when (type) {
                ReportTargetType.POST -> {
                    val postDoc = firestore.collection(FirestoreConstants.POSTS)
                        .document(targetId)
                        .get()
                        .await()

                    val ownerId = postDoc.getString(FirestoreConstants.Post.OWNER_ID)
                    val ownerName = postDoc.getString(FirestoreConstants.Post.OWNER_DISPLAY_NAME)
                    val description = postDoc.getString(FirestoreConstants.Post.DESCRIPTION)
                        ?.take(TARGET_CONTENT_MAX_LENGTH)
                    val images = postDoc.get(FirestoreConstants.Post.IMAGES) as? List<*>
                    val firstImage = images?.firstOrNull() as? String

                    TargetDetails(ownerId, ownerName, description, firstImage)
                }
                
                ReportTargetType.COMMENT -> {
                    val topLevelComment = firestore.collection(FirestoreConstants.COMMENTS)
                        .document(targetId)
                        .get()
                        .await()
                    val commentDoc = if (topLevelComment.exists()) {
                        topLevelComment
                    } else {
                        firestore.collectionGroup(FirestoreConstants.SUBCOLLECTION_POST_COMMENTS)
                            .whereEqualTo(FieldPath.documentId(), targetId)
                            .limit(1)
                            .get()
                            .await()
                            .documents
                            .firstOrNull()
                    }

                    val authorId = commentDoc?.getString(FirestoreConstants.Comment.AUTHOR_ID)
                        ?: commentDoc?.getString("author_id")
                    val authorName = commentDoc?.getString(FirestoreConstants.Comment.AUTHOR_NAME)
                        ?: commentDoc?.getString("author_name")
                    val text = commentDoc?.getString(FirestoreConstants.Comment.CONTENT)
                        ?.take(TARGET_CONTENT_MAX_LENGTH)

                    TargetDetails(authorId, authorName, text, null)
                }
                
                ReportTargetType.USER -> {
                    val userDoc = firestore.collection(FirestoreConstants.USERS)
                        .document(targetId)
                        .get()
                        .await()

                    val displayName = userDoc.getString(FirestoreConstants.User.DISPLAY_NAME)
                    val photoUrl = userDoc.getString(FirestoreConstants.User.PHOTO_URL)

                    TargetDetails(targetId, displayName, null, photoUrl)
                }
            }
        } catch (e: Exception) {
            logger.e(TAG, "Error getting target details", e)
            TargetDetails(null, null, null, null)
        }
    }

    /**
     * Create admin notification for high priority reports
     */
    private suspend fun createAdminNotification(
        reportId: String,
        type: ReportTargetType,
        reason: String
    ) {
        try {
            // Get all admin users (users with admin custom claim)
            // Note: This is a simplified version. In production, you'd query users with admin role
            // For now, we'll create a notification in a general admin collection
            
            firestore.collection("admin_notifications").add(
                mapOf(
                    "type" to "new_report",
                    "reportId" to reportId,
                    "targetType" to type.name,
                    "reason" to reason,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "isRead" to false
                )
            ).await()
            
            logger.d(TAG, "Admin notification created for report $reportId")
        } catch (e: Exception) {
            logger.e(TAG, "Error creating admin notification", e)
            // Don't fail the report creation if notification fails
        }
    }

    /**
     * Data class for target details
     */
    private data class TargetDetails(
        val ownerId: String?,
        val ownerName: String?,
        val content: String?,
        val imageUrl: String?
    )
}
