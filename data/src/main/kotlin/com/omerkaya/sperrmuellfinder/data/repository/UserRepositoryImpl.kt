package com.omerkaya.sperrmuellfinder.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.data.manager.UserBootstrapper
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants
import com.omerkaya.sperrmuellfinder.data.mapper.toDomainModel
import com.omerkaya.sperrmuellfinder.data.source.user.FirebaseUserDataSource
import com.omerkaya.sperrmuellfinder.domain.model.AccountDeletionStatus
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.model.UserFavorites
import com.omerkaya.sperrmuellfinder.domain.repository.UserRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.map

/**
 * Implementation of UserRepository interface
 * Handles user data operations with Firebase
 * Rules.md compliant - Clean Architecture data layer
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firebaseUserDataSource: FirebaseUserDataSource,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val functions: FirebaseFunctions,
    private val firebaseAuth: FirebaseAuth,
    private val userBootstrapper: UserBootstrapper,
    private val logger: Logger
) : UserRepository {
    
    companion object {
        private const val TAG = "UserRepository"
    }
    
    override fun getCurrentUser(): Flow<User?> {
        return firebaseUserDataSource.getCurrentUser()
            .map { userDto -> userDto?.toDomainModel() }
    }
    
    override suspend fun getUserById(userId: String): Result<User?> {
        return try {
            val result = firebaseUserDataSource.getUserById(userId)
            when (result) {
                is Result.Success -> Result.Success(result.data?.toDomainModel())
                is Result.Error -> result
                is Result.Loading -> result
            }
        } catch (e: Exception) {
            logger.e(TAG, "Error getting user by ID", e)
            Result.Error(e)
        }
    }
    
    override fun getUserByIdFlow(userId: String): Flow<User?> {
        return firebaseUserDataSource.getUserByIdFlow(userId)
            .map { userDto -> 
                userDto?.let { dto ->
                    val domainUser = dto.toDomainModel()
                    
                    // 🎯 FIREBASE AUTH PHOTO SYNC & CONTENT URI CLEANUP
                    try {
                        val firebaseUser = firebaseAuth.currentUser
                        val authPhotoUrl = firebaseUser?.photoUrl?.toString()
                        
                        // 🚫 CLEAN PROBLEMATIC CONTENT URIs: Check if photoUrl is problematic
                        val hasProblematicUri = !domainUser.photoUrl.isNullOrBlank() && 
                                              isProblematicContentUri(domainUser.photoUrl!!)
                        
                        if (hasProblematicUri) {
                            logger.w(TAG, "🚫 Found problematic content URI for user $userId: ${domainUser.photoUrl!!.take(50)}...")
                        }
                        
                        // Only sync if this is the current user
                        if (userId == firebaseUser?.uid) {
                            var shouldUpdate = false
                            var newPhotoUrl: String? = null
                            
                            when {
                                // Case 1: Has problematic URI, try to replace with Firebase Auth
                                hasProblematicUri -> {
                                    if (!authPhotoUrl.isNullOrBlank() && !isProblematicContentUri(authPhotoUrl)) {
                                        newPhotoUrl = authPhotoUrl
                                        shouldUpdate = true
                                        logger.d(TAG, "🔄 Replacing problematic URI with Firebase Auth photoURL for user: $userId")
                                    } else {
                                        newPhotoUrl = null
                                        shouldUpdate = true
                                        logger.d(TAG, "🧹 Clearing problematic content URI for user: $userId")
                                    }
                                }
                                // Case 2: Missing photoUrl but Firebase Auth has one
                                domainUser.photoUrl.isNullOrBlank() && !authPhotoUrl.isNullOrBlank() -> {
                                    if (!isProblematicContentUri(authPhotoUrl)) {
                                        newPhotoUrl = authPhotoUrl
                                        shouldUpdate = true
                                        logger.d(TAG, "🔄 Syncing photoURL from Firebase Auth for user: $userId")
                                    }
                                }
                                // Case 3: Different photoUrl in Firebase Auth
                                !authPhotoUrl.isNullOrBlank() && domainUser.photoUrl != authPhotoUrl -> {
                                    if (!isProblematicContentUri(authPhotoUrl)) {
                                        newPhotoUrl = authPhotoUrl
                                        shouldUpdate = true
                                        logger.d(TAG, "🔄 Updating photoURL from Firebase Auth for user: $userId")
                                    }
                                }
                            }
                            
                            if (shouldUpdate) {
                                // Update Firestore asynchronously
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                    try {
                                        firebaseUserDataSource.updateUserProfile(
                                            displayName = null,
                                            city = null,
                                            photoUrl = newPhotoUrl
                                        )
                                        logger.i(TAG, "✅ PhotoURL updated successfully for user: $userId")
                                    } catch (e: Exception) {
                                        logger.e(TAG, "❌ Failed to update photoURL for user: $userId", e)
                                    }
                                }
                                
                                // Return user with updated photoUrl immediately
                                domainUser.copy(photoUrl = newPhotoUrl)
                            } else {
                                domainUser
                            }
                        } else {
                            domainUser
                        }
                    } catch (e: Exception) {
                        logger.e(TAG, "Error during Firebase Auth photo sync for user: $userId", e)
                        domainUser
                    }
                }
            }
    }
    
    /**
     * Check if a URI is a problematic content:// URI that causes SecurityException
     */
    private fun isProblematicContentUri(uri: String): Boolean {
        return uri.startsWith("content://media/picker") ||
               uri.startsWith("content://com.android.providers.media.photopicker") ||
               (uri.startsWith("content://") && 
                !uri.startsWith("https://") &&
                !uri.startsWith("http://"))
    }
    
    /**
     * Fix missing ownerPhotoUrl in existing posts
     * This method updates all posts where ownerPhotoUrl is null but user has photoUrl
     */
    override suspend fun fixMissingOwnerPhotoUrls(): Result<Unit> {
        return try {
            val currentUser = firebaseAuth.currentUser ?: return Result.Error(Exception("User not authenticated"))
            
            logger.d(TAG, "🔧 Fixing missing ownerPhotoUrl for user: ${currentUser.uid}")
            
            // Get current user's photoUrl from Firestore
            val userDoc = firestore.collection(FirestoreConstants.COLLECTION_USERS)
                .document(currentUser.uid)
                .get()
                .await()
            
            val userPhotoUrl = userDoc.getString(FirestoreConstants.FIELD_PHOTO_URL)
            val userDisplayName = userDoc.getString(FirestoreConstants.FIELD_DISPLAY_NAME)
            val userLevel = userDoc.getLong(FirestoreConstants.FIELD_LEVEL)?.toInt()
            val isUserPremium = userDoc.getBoolean(FirestoreConstants.FIELD_IS_PREMIUM)
            
            if (userPhotoUrl.isNullOrBlank()) {
                logger.d(TAG, "User has no photoUrl, skipping fix")
                return Result.Success(Unit)
            }
            
            // Find all posts by this user where ownerPhotoUrl is null or empty
            val userPosts = firestore.collection(FirestoreConstants.COLLECTION_POSTS)
                .whereEqualTo(FirestoreConstants.FIELD_OWNER_ID, currentUser.uid)
                .get()
                .await()
            
            val batch = firestore.batch()
            var updatedCount = 0
            
            for (postDoc in userPosts.documents) {
                val currentOwnerPhotoUrl = postDoc.getString(FirestoreConstants.FIELD_OWNER_PHOTO_URL)
                
                if (currentOwnerPhotoUrl.isNullOrBlank()) {
                    val updates = mutableMapOf<String, Any?>()
                    updates[FirestoreConstants.FIELD_OWNER_PHOTO_URL] = userPhotoUrl
                    
                    // Also update other user fields if they're missing
                    if (postDoc.getString(FirestoreConstants.FIELD_OWNER_DISPLAY_NAME).isNullOrBlank() && !userDisplayName.isNullOrBlank()) {
                        updates[FirestoreConstants.FIELD_OWNER_DISPLAY_NAME] = userDisplayName
                    }
                    if (postDoc.getLong(FirestoreConstants.FIELD_OWNER_LEVEL) == null && userLevel != null) {
                        updates[FirestoreConstants.FIELD_OWNER_LEVEL] = userLevel
                    }
                    if (postDoc.getBoolean(FirestoreConstants.FIELD_IS_OWNER_PREMIUM) == null && isUserPremium != null) {
                        updates[FirestoreConstants.FIELD_IS_OWNER_PREMIUM] = isUserPremium
                    }
                    
                    batch.update(postDoc.reference, updates)
                    updatedCount++
                }
            }
            
            if (updatedCount > 0) {
                batch.commit().await()
                logger.i(TAG, "✅ Fixed ownerPhotoUrl for $updatedCount posts")
            } else {
                logger.d(TAG, "No posts needed ownerPhotoUrl fix")
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Error fixing missing ownerPhotoUrls", e)
            Result.Error(e)
        }
    }
    
    override suspend fun updateUserProfile(
        displayName: String?,
        firstName: String?,
        lastName: String?,
        city: String?,
        photoUrl: String?
    ): Result<Unit> {
        return try {
            val currentUser = firebaseAuth.currentUser ?: return Result.Error(Exception("User not authenticated"))
            
            logger.d(TAG, "🎯 Updating user profile for: ${currentUser.uid}")

            if (displayName == null && firstName == null && lastName == null && city == null && photoUrl == null) {
                logger.w(TAG, "No updates provided")
                return Result.Success(Unit)
            }

            when (val updateResult = firebaseUserDataSource.updateUserProfile(
                displayName = displayName,
                firstName = firstName,
                lastName = lastName,
                city = city,
                photoUrl = photoUrl
            )) {
                is Result.Error -> return updateResult
                else -> Unit
            }

            logger.i(TAG, "✅ User profile updated successfully (users + users_public)")
            
            // Sync denormalized data if displayName or photoUrl changed (with throttling)
            if (displayName != null || photoUrl != null) {
                // Use a coroutine with delay to prevent rapid successive calls
                kotlinx.coroutines.delay(1000) // Wait 1 second before syncing
                syncDenormalizedUserDataThrottled(displayName, photoUrl)
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "❌ Error updating user profile", e)
            Result.Error(e)
        }
    }
    
    /**
     * Throttled sync to prevent rapid successive calls
     */
    private suspend fun syncDenormalizedUserDataThrottled(
        displayName: String?,
        photoUrl: String?
    ) {
        try {
            logger.d(TAG, "🔄 Starting throttled denormalized data sync")
            syncDenormalizedUserData(displayName, photoUrl)
        } catch (e: Exception) {
            logger.e(TAG, "❌ Error in throttled sync", e)
        }
    }
    
    /**
     * Sync denormalized user data across all collections where user info is stored
     * This ensures real-time consistency across PostCard, PostDetail, Comments, etc.
     */
    private suspend fun syncDenormalizedUserData(
        displayName: String?,
        photoUrl: String?
    ) {
        try {
            val currentUser = firebaseAuth.currentUser ?: return
            logger.d(TAG, "🔄 Syncing denormalized user data for user: ${currentUser.uid}")
            
            // Only sync if we have actual changes
            if (displayName.isNullOrBlank() && photoUrl.isNullOrBlank()) {
                logger.d(TAG, "⏭️ No changes to sync, skipping")
                return
            }
            
            var totalUpdates = 0

            // 1) Posts (all historical posts by owner)
            val userPosts = firestore.collection(FirestoreConstants.COLLECTION_POSTS)
                .whereEqualTo(FirestoreConstants.FIELD_OWNER_ID, currentUser.uid)
                .get()
                .await()
                .documents
            totalUpdates += updateDocumentsInChunks(userPosts) {
                mutableMapOf<String, Any>().apply {
                    displayName?.let {
                        this[FirestoreConstants.FIELD_OWNER_DISPLAY_NAME] = it
                        this["ownerDisplayName"] = it
                    }
                    photoUrl?.let {
                        this[FirestoreConstants.FIELD_OWNER_PHOTO_URL] = it
                        this["ownerPhotoUrl"] = it
                    }
                }
            }

            // 2) Comments (support both camelCase and snake_case author fields)
            val commentsByAuthorId = firestore.collectionGroup("comments")
                .whereEqualTo("authorId", currentUser.uid)
                .get()
                .await()
                .documents
            val commentsByAuthorIdSnake = firestore.collectionGroup("comments")
                .whereEqualTo("author_id", currentUser.uid)
                .get()
                .await()
                .documents
            val allComments = (commentsByAuthorId + commentsByAuthorIdSnake)
                .associateBy { it.reference.path }
                .values
                .toList()
            totalUpdates += updateDocumentsInChunks(allComments) {
                mutableMapOf<String, Any>().apply {
                    displayName?.let {
                        this["authorName"] = it
                        this["author_name"] = it
                    }
                    photoUrl?.let {
                        this["authorPhotoUrl"] = it
                        this["author_photo_url"] = it
                    }
                }
            }

            // 3) Like document denormalization is server-managed.
            // Client-side likes update is intentionally skipped to stay rules-compatible.

            if (totalUpdates > 0) {
                logger.i(TAG, "✅ Successfully synced $totalUpdates denormalized records")
            } else {
                logger.d(TAG, "⏭️ No records needed updating")
            }
            
        } catch (e: Exception) {
            logger.e(TAG, "❌ Error syncing denormalized user data", e)
            // Don't throw - profile update was successful, sync is best effort
        }
    }

    private suspend fun updateDocumentsInChunks(
        documents: List<com.google.firebase.firestore.DocumentSnapshot>,
        updatesBuilder: () -> Map<String, Any>
    ): Int {
        val updates = updatesBuilder()
        if (updates.isEmpty() || documents.isEmpty()) return 0

        var updatedCount = 0
        documents.chunked(400).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { doc ->
                batch.update(doc.reference, updates)
                updatedCount++
            }
            batch.commit().await()
        }
        return updatedCount
    }
    
    override suspend fun updatePremiumStatus(isPremium: Boolean, premiumUntil: Date?): Result<Unit> {
        return try {
            firebaseUserDataSource.updatePremiumStatus(isPremium, premiumUntil)
        } catch (e: Exception) {
            logger.e(TAG, "Error updating premium status", e)
            Result.Error(e)
        }
    }
    
    override suspend fun addBadge(badgeId: String): Result<Unit> {
        return try {
            firebaseUserDataSource.addBadge(badgeId)
        } catch (e: Exception) {
            logger.e(TAG, "Error adding badge", e)
            Result.Error(e)
        }
    }
    
    override suspend fun removeBadge(badgeId: String): Result<Unit> {
        return try {
            firebaseUserDataSource.removeBadge(badgeId)
        } catch (e: Exception) {
            logger.e(TAG, "Error removing badge", e)
            Result.Error(e)
        }
    }
    
    override suspend fun updateFavorites(favorites: UserFavorites): Result<Unit> {
        return try {
            firebaseUserDataSource.updateFavorites(favorites)
        } catch (e: Exception) {
            logger.e(TAG, "Error updating favorites", e)
            Result.Error(e)
        }
    }
    
    override suspend fun updateDeviceTokens(tokens: List<String>): Result<Unit> {
        return try {
            firebaseUserDataSource.updateDeviceTokens(tokens)
        } catch (e: Exception) {
            logger.e(TAG, "Error updating device tokens", e)
            Result.Error(e)
        }
    }
    
    override suspend fun getUserPostsCount(userId: String): Result<Int> {
        return try {
            firebaseUserDataSource.getUserPostsCount(userId)
        } catch (e: Exception) {
            logger.e(TAG, "Error getting user posts count", e)
            Result.Error(e)
        }
    }
    
    override suspend fun getFollowersCount(userId: String): Result<Int> {
        return try {
            firebaseUserDataSource.getFollowersCount(userId)
        } catch (e: Exception) {
            logger.e(TAG, "Error getting followers count", e)
            Result.Error(e)
        }
    }
    
    override suspend fun getFollowingCount(userId: String): Result<Int> {
        return try {
            firebaseUserDataSource.getFollowingCount(userId)
        } catch (e: Exception) {
            logger.e(TAG, "Error getting following count", e)
            Result.Error(e)
        }
    }
    
    override suspend fun searchUsers(query: String, limit: Int): Result<List<User>> {
        return try {
            val result = firebaseUserDataSource.searchUsers(query, limit)
            when (result) {
                is Result.Success -> {
                    val users = result.data.map { it.toDomainModel() }
                    Result.Success(users)
                }
                is Result.Error -> result
                is Result.Loading -> result
            }
        } catch (e: Exception) {
            logger.e(TAG, "Error searching users", e)
            Result.Error(e)
        }
    }
    
    override suspend fun deleteUser(): Result<Unit> {
        return try {
            firebaseUserDataSource.deleteUser()
        } catch (e: Exception) {
            logger.e(TAG, "Error deleting user", e)
            Result.Error(e)
        }
    }
    
    override suspend fun refreshUserData(): Result<User> {
        return try {
            val result = firebaseUserDataSource.refreshUserData()
            when (result) {
                is Result.Success -> Result.Success(result.data.toDomainModel())
                is Result.Error -> result
                is Result.Loading -> result
            }
        } catch (e: Exception) {
            logger.e(TAG, "Error refreshing user data", e)
            Result.Error(e)
        }
    }
    
    /**
     * Check if nickname is available (unique)
     * Rules.md compliant - Firestore query for nickname availability
     */
    override suspend fun isNicknameAvailable(nickname: String): Result<Boolean> {
        return try {
            logger.d(TAG, "Checking nickname availability: $nickname")

            val normalizedNickname = nickname.trim().lowercase()
            val querySnapshot = firestore.collection(FirestoreConstants.Collections.USERS_PUBLIC)
                .whereEqualTo("usernameLower", normalizedNickname)
                .limit(1)
                .get()
                .await()
            
            val isAvailable = querySnapshot.isEmpty
            logger.d(TAG, "Nickname '$nickname' available: $isAvailable")
            
            Result.Success(isAvailable)
            
        } catch (e: Exception) {
            logger.e(TAG, "Error checking nickname availability", e)
            Result.Error(e)
        }
    }
    
    /**
     * Upload profile photo to Firebase Storage
     * Rules.md compliant - Firebase Storage integration
     */
    override suspend fun uploadProfilePhoto(
        userId: String,
        imageUri: Uri
    ): Result<String> {
        return try {
            logger.d(TAG, "Uploading profile photo for user: $userId")
            
            // Create storage reference
            val fileName = "profile_photos/${userId}_${System.currentTimeMillis()}.jpg"
            val storageRef = storage.reference.child(fileName)
            
            // Upload file with retry mechanism
            var retryCount = 0
            val maxRetries = 3
            
            while (retryCount < maxRetries) {
                try {
                    // Upload file
                    val uploadTask = storageRef.putFile(imageUri).await()
                    
                    // Get download URL
                    val downloadUrl = uploadTask.storage.downloadUrl.await()
                    
                    logger.i(TAG, "Profile photo uploaded successfully: $downloadUrl")
                    return Result.Success(downloadUrl.toString())
                } catch (e: Exception) {
                    retryCount++
                    if (retryCount >= maxRetries) {
                        throw e
                    }
                    logger.w(TAG, "Upload attempt $retryCount failed, retrying...", e)
                    kotlinx.coroutines.delay(1000L * retryCount) // Exponential backoff
                }
            }
            
            throw Exception("Upload failed after $maxRetries attempts")
        } catch (e: Exception) {
            logger.e(TAG, "Error uploading profile photo", e)
            Result.Error(e)
        }
    }
    
    /**
     * Update user's profile photo URL in Firestore
     * Rules.md compliant - Firestore update operation
     */
    override suspend fun updateProfilePhotoUrl(
        userId: String,
        photoUrl: String
    ): Result<Unit> {
        return try {
            logger.d(TAG, "Updating profile photo URL for user: $userId")
            
            // Start batch operation for atomic updates
            val batch = firestore.batch()
            
            // Update user document
            val userRef = firestore.collection(FirestoreConstants.COLLECTION_USERS).document(userId)
            batch.update(userRef, FirestoreConstants.FIELD_PHOTO_URL, photoUrl)
            
            // Update all user's posts with new photo URL (denormalized data)
            val userPosts = firestore.collection(FirestoreConstants.COLLECTION_POSTS)
                .whereEqualTo(FirestoreConstants.FIELD_OWNER_ID, userId)
                .get()
                .await()
            
            logger.d(TAG, "Found ${userPosts.size()} posts to update with new photo URL")
            
            // Update each post's denormalized owner photo URL
            for (postDoc in userPosts.documents) {
                batch.update(postDoc.reference, FirestoreConstants.FIELD_OWNER_PHOTO_URL, photoUrl)
            }
            
            // Update all user's comments with new photo URL (denormalized data)
            val userCommentsBySnake = firestore.collectionGroup("comments")
                .whereEqualTo("author_id", userId)
                .get()
                .await()
                .documents
            val userCommentsByCamel = firestore.collectionGroup("comments")
                .whereEqualTo("authorId", userId)
                .get()
                .await()
                .documents
            val userComments = (userCommentsBySnake + userCommentsByCamel)
                .associateBy { it.reference.path }
                .values

            logger.d(TAG, "Found ${userComments.size} comments to update with new photo URL")
            
            // Update each comment's denormalized author photo URL
            for (commentDoc in userComments) {
                batch.update(
                    commentDoc.reference,
                    mapOf(
                        "author_photo_url" to photoUrl,
                        "authorPhotoUrl" to photoUrl
                    )
                )
            }
            
            // Commit all updates atomically
            batch.commit().await()
            
            logger.i(TAG, "Profile photo URL updated successfully across all collections")
            Result.Success(Unit)
            
        } catch (e: Exception) {
            logger.e(TAG, "Error updating profile photo URL", e)
            Result.Error(e)
        }
    }
    
    /**
     * Ensure user document exists and is properly initialized
     * Rules.md compliant - Idempotent user bootstrapping
     */
    override suspend fun ensureUserDocument(fcmToken: String?): Result<User> {
        return try {
            val authUser = firebaseAuth.currentUser
                ?: return Result.Error(Exception("User not authenticated"))
            
            logger.d(TAG, "Ensuring user document exists for: ${authUser.uid}")
            
            val userDto = userBootstrapper.ensureUserDocument(authUser) { fcmToken }
            
            logger.i(TAG, "User document ensured successfully")
            Result.Success(userDto.toDomainModel())
            
        } catch (e: Exception) {
            logger.e(TAG, "Error ensuring user document", e)
            Result.Error(e)
        }
    }
    
    // ========================================
    // ACCOUNT DELETION SYSTEM
    // ========================================
    
    override suspend fun requestAccountDeletion(reason: String?): Result<Unit> {
        return try {
            val currentUser = firebaseAuth.currentUser
                ?: return Result.Error(Exception("User not authenticated"))
            
            logger.d(TAG, "Requesting account deletion for user: ${currentUser.uid}")

            val payload = hashMapOf<String, Any?>(
                "reason" to reason
            )
            functions
                .getHttpsCallable("requestAccountDeletion")
                .call(payload)
                .await()
            
            logger.i(TAG, "Account deletion request submitted successfully")
            Result.Success(Unit)
            
        } catch (e: Exception) {
            logger.e(TAG, "Error requesting account deletion", e)
            Result.Error(e)
        }
    }
    
    override suspend fun cancelAccountDeletion(): Result<Unit> {
        return try {
            val currentUser = firebaseAuth.currentUser
                ?: return Result.Error(Exception("User not authenticated"))
            
            logger.d(TAG, "Cancelling account deletion for user: ${currentUser.uid}")

            functions
                .getHttpsCallable("cancelAccountDeletion")
                .call()
                .await()
            
            logger.i(TAG, "Account deletion cancelled successfully")
            Result.Success(Unit)
            
        } catch (e: Exception) {
            logger.e(TAG, "Error cancelling account deletion", e)
            Result.Error(e)
        }
    }
    
    override fun getAccountDeletionStatus(): Flow<com.omerkaya.sperrmuellfinder.domain.model.AccountDeletionStatus?> = callbackFlow {
        val currentUser = firebaseAuth.currentUser
        
        if (currentUser == null) {
            logger.w(TAG, "User not authenticated for deletion status flow")
            trySend(null)
            close()
            return@callbackFlow
        }
        
        logger.d(TAG, "Starting account deletion status flow for user: ${currentUser.uid}")
        
        val listenerRegistration = firestore
            .collection(FirestoreConstants.ACCOUNT_DELETIONS)
            .document(currentUser.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logger.e(TAG, "Error in deletion status listener", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    try {
                        val userId = snapshot.getString("userId") ?: currentUser.uid
                        val requestedAt = snapshot.getTimestamp("requestedAt")?.toDate() ?: java.util.Date()
                        val scheduledFor = snapshot.getTimestamp("scheduledFor")?.toDate() ?: java.util.Date()
                        val reason = snapshot.getString("reason")
                        val statusStr = snapshot.getString("status") ?: "PENDING"
                        
                        val status = com.omerkaya.sperrmuellfinder.domain.model.DeletionStatus.valueOf(statusStr)
                        
                        val deletionStatus = com.omerkaya.sperrmuellfinder.domain.model.AccountDeletionStatus(
                            userId = userId,
                            requestedAt = requestedAt,
                            scheduledDeletionDate = scheduledFor,
                            reason = reason,
                            status = status
                        )

                        // Only active pending deletions should be exposed to UI.
                        // Cancelled/completed/expired requests must not block delete-account page usage.
                        if (deletionStatus.status == com.omerkaya.sperrmuellfinder.domain.model.DeletionStatus.PENDING &&
                            deletionStatus.canCancel()
                        ) {
                            logger.d(TAG, "Emitting active deletion status: ${deletionStatus.status}")
                            trySend(deletionStatus)
                        } else {
                            logger.d(TAG, "Deletion status is not active. Emitting null.")
                            trySend(null)
                        }
                    } catch (e: Exception) {
                        logger.e(TAG, "Error parsing deletion status", e)
                        trySend(null)
                    }
                } else {
                    logger.d(TAG, "No deletion scheduled for user")
                    trySend(null)
                }
            }
        
        awaitClose {
            logger.d(TAG, "Closing deletion status listener")
            listenerRegistration.remove()
        }
    }
}
