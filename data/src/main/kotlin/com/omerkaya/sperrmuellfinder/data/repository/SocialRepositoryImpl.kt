package com.omerkaya.sperrmuellfinder.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.data.mapper.toComment
import com.omerkaya.sperrmuellfinder.data.mapper.toUser
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants
import com.omerkaya.sperrmuellfinder.data.util.PublicUserResolver
import com.omerkaya.sperrmuellfinder.domain.model.BlockedUser
import com.omerkaya.sperrmuellfinder.domain.model.Comment
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.repository.NotificationRepository
import com.omerkaya.sperrmuellfinder.domain.repository.SocialRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import kotlin.math.max
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🏆 SOCIAL REPOSITORY IMPLEMENTATION - SperrmüllFinder
 * Rules.md compliant - Clean Architecture data layer
 * 
 * Features:
 * - Follow/Unfollow system with Firestore
 * - Real-time comments with optimistic updates
 * - User search with Firestore queries
 * - Professional error handling and logging
 * - Notification integration
 * - Analytics tracking
 */
@Singleton
class SocialRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val notificationRepository: NotificationRepository,
    private val logger: Logger
) : SocialRepository {
    private var interactionBlockedUserIdsCache: Set<String>? = null
    private var interactionBlockedUserIdsCacheAtMs: Long = 0L
    
    companion object {
        private const val TAG = "SocialRepository"
        private const val BLOCK_CACHE_TTL_MS = 30_000L
    }
    
    // ========================================
    // FOLLOW SYSTEM
    // ========================================
    
    override suspend fun followUser(targetUserId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))
            
            if (currentUserId == targetUserId) {
                return Result.Error(Exception("Cannot follow yourself"))
            }
            
            // Get current user and target user info for denormalization (like system)
            val currentUserRef = firestore
                .collection(FirestoreConstants.Collections.USERS_PRIVATE)
                .document(currentUserId)
            val targetUserRef = firestore
                .collection(FirestoreConstants.Collections.USERS_PUBLIC)
                .document(targetUserId)
            
            val currentUserDoc = currentUserRef.get().await()
            val targetUserDoc = targetUserRef.get().await()
            
            val currentUserResolvedDoc = if (currentUserDoc.exists()) {
                currentUserDoc
            } else {
                firestore.collection(FirestoreConstants.Collections.USERS_PUBLIC)
                    .document(currentUserId)
                    .get()
                    .await()
            }

            if (!currentUserResolvedDoc.exists()) {
                return Result.Error(Exception("User not found"))
            }
            
            // Current user info (follower)
            val currentUserDisplayName = currentUserResolvedDoc.getString(FirestoreConstants.User.DISPLAY_NAME) ?: "Anonymous"
            val currentUserNickname = currentUserResolvedDoc.getString(FirestoreConstants.User.NICKNAME) ?: ""
            val currentUserPhotoUrl = currentUserResolvedDoc.getString(FirestoreConstants.User.PHOTO_URL)
            val currentUserLevel = currentUserResolvedDoc.getLong(FirestoreConstants.User.LEVEL)?.toInt() ?: 1
            val currentUserIsPremium = currentUserResolvedDoc.getBoolean(FirestoreConstants.User.IS_PREMIUM)
                ?: currentUserResolvedDoc.getBoolean("isPremium")
                ?: false
            val currentUserCity = currentUserResolvedDoc.getString(FirestoreConstants.User.CITY)
            
            // Target user info (followed)
            val targetUserDisplayName = targetUserDoc.getString(FirestoreConstants.User.DISPLAY_NAME) ?: "Anonymous"
            val targetUserNickname = targetUserDoc.getString(FirestoreConstants.User.NICKNAME) ?: ""
            val targetUserPhotoUrl = targetUserDoc.getString(FirestoreConstants.User.PHOTO_URL)
            val targetUserLevel = targetUserDoc.getLong(FirestoreConstants.User.LEVEL)?.toInt() ?: 1
            val targetUserIsPremium = targetUserDoc.getBoolean(FirestoreConstants.User.IS_PREMIUM)
                ?: targetUserDoc.getBoolean("isPremium")
                ?: false
            val targetUserCity = targetUserDoc.getString(FirestoreConstants.User.CITY)
            
            val followId = "${currentUserId}_${targetUserId}"
            
            // Denormalized follow data (like system style)
            val followData = mapOf(
                FirestoreConstants.Follow.FOLLOW_ID to followId,
                FirestoreConstants.Follow.FOLLOWER_ID to currentUserId,
                FirestoreConstants.Follow.FOLLOWED_ID to targetUserId,
                FirestoreConstants.Follow.CREATED_AT to FieldValue.serverTimestamp(),
                FirestoreConstants.Follow.IS_ACTIVE to true,
                // Denormalized follower (current user) info
                "followerDisplayName" to currentUserDisplayName,
                "followerNickname" to currentUserNickname,
                "followerPhotoUrl" to currentUserPhotoUrl,
                "followerLevel" to currentUserLevel,
                "followerIsPremium" to currentUserIsPremium,
                "followerCity" to currentUserCity,
                // Denormalized followed (target user) info
                "followedDisplayName" to targetUserDisplayName,
                "followedNickname" to targetUserNickname,
                "followedPhotoUrl" to targetUserPhotoUrl,
                "followedLevel" to targetUserLevel,
                "followedIsPremium" to targetUserIsPremium,
                "followedCity" to targetUserCity
            )
            
            val followRef = firestore
                .collection(FirestoreConstants.Collections.FOLLOWS)
                .document(followId)
            val currentUserDocRef = firestore
                .collection(FirestoreConstants.Collections.USERS)
                .document(currentUserId)
            val targetUserDocRef = firestore
                .collection(FirestoreConstants.Collections.USERS)
                .document(targetUserId)

            // Atomic batch: create follow + increment denormalized counters
            val batch = firestore.batch()
            batch.set(followRef, followData)
            batch.update(currentUserDocRef, FirestoreConstants.FIELD_FOLLOWING_COUNT, FieldValue.increment(1L))
            batch.update(targetUserDocRef, FirestoreConstants.FIELD_FOLLOWERS_COUNT, FieldValue.increment(1L))
            batch.commit().await()

            try {
                notificationRepository.createFollowNotification(
                    followedUserId = targetUserId,
                    followerUserId = currentUserId,
                    followerUserName = currentUserDisplayName
                )
            } catch (notificationError: Exception) {
                logger.w(TAG, "Follow created but notification failed", notificationError)
            }
            
            logger.d(TAG, "✅ User $currentUserId ($currentUserDisplayName) followed user $targetUserId ($targetUserDisplayName) with denormalized data")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Error following user $targetUserId", e)
            Result.Error(e)
        }
    }
    
    override suspend fun unfollowUser(targetUserId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))
            
            // Find existing follow relationship (supports deterministic and legacy random ids)
            val followQuery = firestore
                .collection(FirestoreConstants.Collections.FOLLOWS)
                .whereEqualTo(FirestoreConstants.Follow.FOLLOWER_ID, currentUserId)
                .whereEqualTo(FirestoreConstants.Follow.FOLLOWED_ID, targetUserId)
                .whereEqualTo(FirestoreConstants.Follow.IS_ACTIVE, true)
                .limit(1)
            
            val followSnapshot = followQuery.get().await()
            if (followSnapshot.isEmpty) {
                return Result.Error(Exception("Follow relationship not found"))
            }
            
            val followDocs = followSnapshot.documents
            val followDocRef = followDocs.first().reference
            val currentUserDocRef = firestore
                .collection(FirestoreConstants.Collections.USERS)
                .document(currentUserId)
            val targetUserDocRef = firestore
                .collection(FirestoreConstants.Collections.USERS)
                .document(targetUserId)

            // Atomic transaction: delete follow + decrement counters with floor-0 protection
            firestore.runTransaction { transaction ->
                val currentUserSnap = transaction.get(currentUserDocRef)
                val targetUserSnap = transaction.get(targetUserDocRef)
                val currentFollowing = currentUserSnap.getLong(FirestoreConstants.FIELD_FOLLOWING_COUNT) ?: 0L
                val targetFollowers = targetUserSnap.getLong(FirestoreConstants.FIELD_FOLLOWERS_COUNT) ?: 0L
                transaction.delete(followDocRef)
                transaction.update(currentUserDocRef, FirestoreConstants.FIELD_FOLLOWING_COUNT, maxOf(0L, currentFollowing - 1L))
                transaction.update(targetUserDocRef, FirestoreConstants.FIELD_FOLLOWERS_COUNT, maxOf(0L, targetFollowers - 1L))
            }.await()

            logger.d(TAG, "User $currentUserId unfollowed user $targetUserId")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Error unfollowing user $targetUserId", e)
            Result.Error(e)
        }
    }
    
    override suspend fun isFollowing(targetUserId: String): Result<Boolean> {
        return try {
            val currentUserId = auth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))
            
            val followQuery = firestore
                .collection(FirestoreConstants.Collections.FOLLOWS)
                .whereEqualTo(FirestoreConstants.Follow.FOLLOWER_ID, currentUserId)
                .whereEqualTo(FirestoreConstants.Follow.FOLLOWED_ID, targetUserId)
                .whereEqualTo(FirestoreConstants.Follow.IS_ACTIVE, true)
                .limit(1)
            
            val followSnapshot = followQuery.get().await()
            val isFollowing = !followSnapshot.isEmpty
            
            Result.Success(isFollowing)
        } catch (e: Exception) {
            logger.e(TAG, "Error checking follow status for user $targetUserId", e)
            Result.Error(e)
        }
    }
    
    override fun getFollowers(userId: String): Flow<List<User>> = callbackFlow {
        val lock = Any()
        var sourceTopLevelFollowedId: List<FollowEdge> = emptyList()
        var sourceTopLevelFollowingIdLegacy: List<FollowEdge> = emptyList()
        var sourceLegacySubcollection: List<FollowEdge> = emptyList()
        val userRegistrations = mutableListOf<ListenerRegistration>()

        fun mergedFollowerIds(): Set<String> {
            val merged = mutableSetOf<String>()
            synchronized(lock) {
                (sourceTopLevelFollowedId + sourceTopLevelFollowingIdLegacy + sourceLegacySubcollection)
                    .forEach { merged.add(it.otherUserId) }
            }
            return merged
        }

        suspend fun emitMergedFollowers() {
            val merged = mutableMapOf<String, Long>()
            synchronized(lock) {
                (sourceTopLevelFollowedId + sourceTopLevelFollowingIdLegacy + sourceLegacySubcollection)
                    .forEach { edge ->
                        merged[edge.otherUserId] = max(merged[edge.otherUserId] ?: 0L, edge.createdAtMillis)
                    }
            }

            val blockedUserIds = getInteractionBlockedUserIds()
            val usersById = fetchUsersByIds(merged.keys, blockedUserIds)
            val sortedFollowers = usersById.sortedByDescending { user -> merged[user.uid] ?: 0L }
            trySend(sortedFollowers)
            logger.d(TAG, "✅ Loaded ${sortedFollowers.size} visible followers for user $userId")
        }

        fun refreshUserListeners() {
            userRegistrations.forEach { it.remove() }
            userRegistrations.clear()

            val ids = mergedFollowerIds().toList().distinct()
            ids.chunked(10).forEach { chunk ->
                val registration = firestore
                    .collection(FirestoreConstants.Collections.USERS_PUBLIC)
                    .whereIn(FieldPath.documentId(), chunk)
                    .limit(chunk.size.toLong())
                    .addSnapshotListener { _, _ ->
                        launch(Dispatchers.IO) { emitMergedFollowers() }
                    }
                userRegistrations.add(registration)
            }
        }

        val registrations = mutableListOf<ListenerRegistration>()

        registrations += firestore
            .collection(FirestoreConstants.Collections.FOLLOWS)
            .whereEqualTo(FirestoreConstants.Follow.FOLLOWED_ID, userId)
            .limit(200)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logger.e(TAG, "Error listening followers (follows.followedId)", error)
                    return@addSnapshotListener
                }
                synchronized(lock) {
                    sourceTopLevelFollowedId = snapshot?.documents
                        ?.asSequence()
                        ?.filter { it.getBoolean(FirestoreConstants.Follow.IS_ACTIVE) != false }
                        ?.mapNotNull { doc ->
                            val followerId = doc.getString(FirestoreConstants.Follow.FOLLOWER_ID)
                                ?: doc.getString(FirestoreConstants.Follow.USER_ID)
                                ?: return@mapNotNull null
                            FollowEdge(
                                otherUserId = followerId,
                                createdAtMillis = getFollowCreatedAtMillis(doc)
                            )
                        }?.toList().orEmpty()
                }
                refreshUserListeners()
                launch(Dispatchers.IO) { emitMergedFollowers() }
            }

        registrations += firestore
            .collection(FirestoreConstants.Collections.FOLLOWS)
            .whereEqualTo(FirestoreConstants.Follow.FOLLOWING_ID, userId)
            .limit(200)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logger.e(TAG, "Error listening followers (follows.followingId legacy)", error)
                    return@addSnapshotListener
                }
                synchronized(lock) {
                    sourceTopLevelFollowingIdLegacy = snapshot?.documents
                        ?.asSequence()
                        ?.filter { it.getBoolean(FirestoreConstants.Follow.IS_ACTIVE) != false }
                        ?.mapNotNull { doc ->
                            val followerId = doc.getString(FirestoreConstants.Follow.FOLLOWER_ID)
                                ?: doc.getString(FirestoreConstants.Follow.USER_ID)
                                ?: return@mapNotNull null
                            FollowEdge(
                                otherUserId = followerId,
                                createdAtMillis = getFollowCreatedAtMillis(doc)
                            )
                        }?.toList().orEmpty()
                }
                refreshUserListeners()
                launch(Dispatchers.IO) { emitMergedFollowers() }
            }

        registrations += firestore
            .collection(FirestoreConstants.COLLECTION_FOLLOWERS)
            .document(userId)
            .collection(FirestoreConstants.SUBCOLLECTION_FOLLOWERS)
            .limit(200)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logger.e(TAG, "Error listening followers (legacy followers/{uid}/followers)", error)
                    return@addSnapshotListener
                }
                synchronized(lock) {
                    sourceLegacySubcollection = snapshot?.documents
                        ?.mapNotNull { doc ->
                            val followerId =
                                doc.getString("sourceUserId")
                                    ?: doc.getString(FirestoreConstants.Follow.FOLLOWER_ID)
                                    ?: doc.getString(FirestoreConstants.Follow.USER_ID)
                                    ?: doc.id
                            if (followerId.isBlank()) return@mapNotNull null
                            FollowEdge(
                                otherUserId = followerId,
                                createdAtMillis = getFollowCreatedAtMillis(doc)
                            )
                        }.orEmpty()
                }
                refreshUserListeners()
                launch(Dispatchers.IO) { emitMergedFollowers() }
            }

        awaitClose {
            registrations.forEach { it.remove() }
            userRegistrations.forEach { it.remove() }
        }
    }
    
    override fun getFollowing(userId: String): Flow<List<User>> = callbackFlow {
        val lock = Any()
        var sourceTopLevelFollowerId: List<FollowEdge> = emptyList()
        var sourceTopLevelUserIdLegacy: List<FollowEdge> = emptyList()
        var sourceLegacySubcollection: List<FollowEdge> = emptyList()
        val userRegistrations = mutableListOf<ListenerRegistration>()

        fun mergedFollowingIds(): Set<String> {
            val merged = mutableSetOf<String>()
            synchronized(lock) {
                (sourceTopLevelFollowerId + sourceTopLevelUserIdLegacy + sourceLegacySubcollection)
                    .forEach { merged.add(it.otherUserId) }
            }
            return merged
        }

        suspend fun emitMergedFollowing() {
            val merged = mutableMapOf<String, Long>()
            synchronized(lock) {
                (sourceTopLevelFollowerId + sourceTopLevelUserIdLegacy + sourceLegacySubcollection)
                    .forEach { edge ->
                        merged[edge.otherUserId] = max(merged[edge.otherUserId] ?: 0L, edge.createdAtMillis)
                    }
            }

            val blockedUserIds = getInteractionBlockedUserIds()
            val usersById = fetchUsersByIds(merged.keys, blockedUserIds)
            val sortedFollowing = usersById.sortedByDescending { user -> merged[user.uid] ?: 0L }
            trySend(sortedFollowing)
            logger.d(TAG, "✅ Loaded ${sortedFollowing.size} visible following for user $userId")
        }

        fun refreshUserListeners() {
            userRegistrations.forEach { it.remove() }
            userRegistrations.clear()

            val ids = mergedFollowingIds().toList().distinct()
            ids.chunked(10).forEach { chunk ->
                val registration = firestore
                    .collection(FirestoreConstants.Collections.USERS_PUBLIC)
                    .whereIn(FieldPath.documentId(), chunk)
                    .limit(chunk.size.toLong())
                    .addSnapshotListener { _, _ ->
                        launch(Dispatchers.IO) { emitMergedFollowing() }
                    }
                userRegistrations.add(registration)
            }
        }

        val registrations = mutableListOf<ListenerRegistration>()

        registrations += firestore
            .collection(FirestoreConstants.Collections.FOLLOWS)
            .whereEqualTo(FirestoreConstants.Follow.FOLLOWER_ID, userId)
            .limit(200)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logger.e(TAG, "Error listening following (follows.followerId)", error)
                    return@addSnapshotListener
                }
                synchronized(lock) {
                    sourceTopLevelFollowerId = snapshot?.documents
                        ?.asSequence()
                        ?.filter { it.getBoolean(FirestoreConstants.Follow.IS_ACTIVE) != false }
                        ?.mapNotNull { doc ->
                            val followedId = doc.getString(FirestoreConstants.Follow.FOLLOWED_ID)
                                ?: doc.getString(FirestoreConstants.Follow.FOLLOWING_ID)
                                ?: return@mapNotNull null
                            FollowEdge(
                                otherUserId = followedId,
                                createdAtMillis = getFollowCreatedAtMillis(doc)
                            )
                        }?.toList().orEmpty()
                }
                refreshUserListeners()
                launch(Dispatchers.IO) { emitMergedFollowing() }
            }

        registrations += firestore
            .collection(FirestoreConstants.Collections.FOLLOWS)
            .whereEqualTo(FirestoreConstants.Follow.USER_ID, userId)
            .limit(200)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logger.e(TAG, "Error listening following (follows.userId legacy)", error)
                    return@addSnapshotListener
                }
                synchronized(lock) {
                    sourceTopLevelUserIdLegacy = snapshot?.documents
                        ?.asSequence()
                        ?.filter { it.getBoolean(FirestoreConstants.Follow.IS_ACTIVE) != false }
                        ?.mapNotNull { doc ->
                            val followedId = doc.getString(FirestoreConstants.Follow.FOLLOWED_ID)
                                ?: doc.getString(FirestoreConstants.Follow.FOLLOWING_ID)
                                ?: return@mapNotNull null
                            FollowEdge(
                                otherUserId = followedId,
                                createdAtMillis = getFollowCreatedAtMillis(doc)
                            )
                        }?.toList().orEmpty()
                }
                refreshUserListeners()
                launch(Dispatchers.IO) { emitMergedFollowing() }
            }

        registrations += firestore
            .collection(FirestoreConstants.COLLECTION_FOLLOWERS)
            .document(userId)
            .collection(FirestoreConstants.SUBCOLLECTION_FOLLOWING)
            .limit(200)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logger.e(TAG, "Error listening following (legacy followers/{uid}/following)", error)
                    return@addSnapshotListener
                }
                synchronized(lock) {
                    sourceLegacySubcollection = snapshot?.documents
                        ?.mapNotNull { doc ->
                            val followedId =
                                doc.getString("targetUserId")
                                    ?: doc.getString(FirestoreConstants.Follow.FOLLOWED_ID)
                                    ?: doc.getString(FirestoreConstants.Follow.FOLLOWING_ID)
                                    ?: doc.id
                            if (followedId.isBlank()) return@mapNotNull null
                            FollowEdge(
                                otherUserId = followedId,
                                createdAtMillis = getFollowCreatedAtMillis(doc)
                            )
                        }.orEmpty()
                }
                refreshUserListeners()
                launch(Dispatchers.IO) { emitMergedFollowing() }
            }

        awaitClose {
            registrations.forEach { it.remove() }
            userRegistrations.forEach { it.remove() }
        }
    }
    
    override fun getFollowerCount(userId: String): Flow<Int> = callbackFlow {
        val listener = firestore
            .collection(FirestoreConstants.Collections.FOLLOWS)
            .whereEqualTo(FirestoreConstants.Follow.FOLLOWED_ID, userId)
            .whereEqualTo(FirestoreConstants.Follow.IS_ACTIVE, true)
            .limit(200)
            .addSnapshotListener { snapshot, error ->
            if (error != null) {
                logger.e(TAG, "Error listening to follower count for user $userId", error)
                trySend(0)
                return@addSnapshotListener
            }
            
            val count = snapshot?.size() ?: 0
            trySend(count)
        }
        
        awaitClose { listener.remove() }
    }
    
    override fun getFollowingCount(userId: String): Flow<Int> = callbackFlow {
        val listener = firestore
            .collection(FirestoreConstants.Collections.FOLLOWS)
            .whereEqualTo(FirestoreConstants.Follow.FOLLOWER_ID, userId)
            .whereEqualTo(FirestoreConstants.Follow.IS_ACTIVE, true)
            .limit(200)
            .addSnapshotListener { snapshot, error ->
            if (error != null) {
                logger.e(TAG, "Error listening to following count for user $userId", error)
                trySend(0)
                return@addSnapshotListener
            }
            
            val count = snapshot?.size() ?: 0
            trySend(count)
        }
        
        awaitClose { listener.remove() }
    }
    
    // ========================================
    // COMMENTS SYSTEM
    // ========================================
    
    override suspend fun addComment(postId: String, text: String): Result<String> {
        return try {
            val currentUserId = auth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))
            
            val commentId = UUID.randomUUID().toString()
            
            // Get current user info
            val currentUserDoc = firestore
                .collection(FirestoreConstants.Collections.USERS_PRIVATE)
                .document(currentUserId)
                .get()
                .await()
            val authUser = auth.currentUser
            
            val currentUserName = currentUserDoc.getString(FirestoreConstants.User.DISPLAY_NAME)
                ?: currentUserDoc.getString("displayName")
                ?: currentUserDoc.getString(FirestoreConstants.User.NICKNAME)
                ?: currentUserDoc.getString("nickname")
                ?: currentUserDoc.getString(FirestoreConstants.User.EMAIL)
                ?: authUser?.displayName
                ?: "Anonymous"
            val currentUserPhotoUrl = currentUserDoc.getString(FirestoreConstants.User.PHOTO_URL)
                ?: currentUserDoc.getString("photoUrl")
                ?: currentUserDoc.getString("photoURL")
                ?: authUser?.photoUrl?.toString()
            val currentUserCity = currentUserDoc.getString(FirestoreConstants.User.CITY)
                ?: currentUserDoc.getString("City")
            val currentUserLevel = currentUserDoc.getLong(FirestoreConstants.User.LEVEL)?.toInt() ?: 1
            
            val commentData = mapOf(
                FirestoreConstants.Comment.COMMENT_ID to commentId,
                FirestoreConstants.Comment.POST_ID to postId,
                FirestoreConstants.Comment.AUTHOR_ID to currentUserId,
                FirestoreConstants.Comment.AUTHOR_NAME to currentUserName,
                FirestoreConstants.Comment.AUTHOR_PHOTO_URL to currentUserPhotoUrl,
                FirestoreConstants.Comment.AUTHOR_CITY to currentUserCity,
                FirestoreConstants.Comment.AUTHOR_LEVEL to currentUserLevel,
                FirestoreConstants.Comment.CONTENT to text,
                FirestoreConstants.Comment.LIKES_COUNT to 0,
                FirestoreConstants.Comment.CREATED_AT to FieldValue.serverTimestamp(),
                FirestoreConstants.Comment.UPDATED_AT to FieldValue.serverTimestamp()
            )
            
            // Use batch write for atomicity
            val batch = firestore.batch()

            // Create comment document in nested path: posts/{postId}/comments/{commentId}
            val postRef = firestore
                .collection(FirestoreConstants.Collections.POSTS)
                .document(postId)
            val commentRef = postRef
                .collection(FirestoreConstants.Subcollections.COMMENTS)
                .document(commentId)
            batch.set(commentRef, commentData)
            batch.update(postRef, FirestoreConstants.Post.COMMENTS_COUNT, FieldValue.increment(1L))
            
            batch.commit().await()
            
            // Get post owner for notification
            val postDoc = firestore
                .collection(FirestoreConstants.Collections.POSTS)
                .document(postId)
                .get()
                .await()
            val postOwnerId = postDoc.getString(FirestoreConstants.Post.OWNER_ID)
            
            // Create comment notification (if not commenting on own post)
            if (postOwnerId != null && postOwnerId != currentUserId) {
                notificationRepository.createCommentNotification(
                    postId = postId,
                    postOwnerId = postOwnerId,
                    commenterUserId = currentUserId,
                    commenterUserName = currentUserName,
                    commentText = text
                )
            }
            
            logger.d(TAG, "Comment $commentId added to post $postId by user $currentUserId")
            Result.Success(commentId)
        } catch (e: Exception) {
            logger.e(TAG, "Error adding comment to post $postId", e)
            Result.Error(e)
        }
    }
    
    override fun getComments(postId: String): Flow<List<Comment>> = callbackFlow {
        val query = firestore
            .collection(FirestoreConstants.Collections.POSTS)
            .document(postId)
            .collection(FirestoreConstants.Subcollections.COMMENTS)
            .orderBy(FirestoreConstants.Comment.CREATED_AT, Query.Direction.DESCENDING)
            .limit(200)

        val userRegistrations = mutableListOf<ListenerRegistration>()
        var latestComments: List<Comment> = emptyList()

        suspend fun emitEnrichedComments() {
            val comments = latestComments
            if (comments.isEmpty()) {
                trySend(emptyList())
                return
            }

            val authorIds = comments.map { it.authorId }.filter { it.isNotBlank() }.distinct()
            val latestUserMap = try {
                PublicUserResolver.resolveUsersByIds(firestore, authorIds)
            } catch (e: Exception) {
                logger.e(TAG, "Error enriching comments with latest user data", e)
                emptyMap()
            }

            val enrichedComments = comments.map { comment ->
                val latestUser = latestUserMap[comment.authorId]
                if (latestUser != null) {
                    comment.copy(
                        authorName = latestUser.displayName.takeIf { it.isNotBlank() } ?: comment.authorName,
                        authorPhotoUrl = latestUser.photoUrl ?: comment.authorPhotoUrl,
                        authorCity = latestUser.city ?: comment.authorCity
                    )
                } else {
                    comment
                }
            }

            trySend(enrichedComments)
            logger.d(TAG, "Loaded ${enrichedComments.size} comments for post $postId")
        }

        fun refreshUserListeners() {
            userRegistrations.forEach { it.remove() }
            userRegistrations.clear()
            val authorIds = latestComments.map { it.authorId }.filter { it.isNotBlank() }.distinct()
            authorIds.chunked(10).forEach { chunk ->
                val registrationByDocId = firestore
                    .collection(FirestoreConstants.Collections.USERS_PUBLIC)
                    .whereIn(FieldPath.documentId(), chunk)
                    .limit(chunk.size.toLong())
                    .addSnapshotListener { _, _ ->
                        CoroutineScope(Dispatchers.IO).launch {
                            emitEnrichedComments()
                        }
                    }
                userRegistrations.add(registrationByDocId)
            }
        }
        
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                logger.e(TAG, "Error listening to comments for post $postId", error)
                trySend(emptyList())
                return@addSnapshotListener
            }
            
            if (snapshot != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    latestComments = snapshot.documents.mapNotNull { document ->
                        try {
                            document.toComment()
                        } catch (e: Exception) {
                            logger.e(TAG, "Error converting comment ${document.id} to domain model", e)
                            null
                        }
                    }
                    refreshUserListeners()
                    emitEnrichedComments()
                }
            } else {
                trySend(emptyList())
            }
        }
        
        awaitClose {
            listener.remove()
            userRegistrations.forEach { it.remove() }
        }
    }
    
    override suspend fun deleteComment(commentId: String, postId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))

            // Primary path: posts/{postId}/comments/{commentId}
            val nestedCommentRef = firestore
                .collection(FirestoreConstants.Collections.POSTS)
                .document(postId)
                .collection(FirestoreConstants.Subcollections.COMMENTS)
                .document(commentId)
            val nestedCommentDoc = nestedCommentRef.get().await()

            // Get post to check if current user is post owner
            val postDoc = firestore
                .collection(FirestoreConstants.Collections.POSTS)
                .document(postId)
                .get()
                .await()
            val postOwnerId = postDoc.getString(FirestoreConstants.Post.OWNER_ID)

            val postRef = firestore
                .collection(FirestoreConstants.Collections.POSTS)
                .document(postId)

            if (nestedCommentDoc.exists()) {
                val commentAuthorId = nestedCommentDoc.getString(FirestoreConstants.Comment.AUTHOR_ID)
                if (commentAuthorId != currentUserId && postOwnerId != currentUserId) {
                    return Result.Error(Exception("Unauthorized: Cannot delete this comment"))
                }
                val deleteBatch = firestore.batch()
                deleteBatch.delete(nestedCommentRef)
                deleteBatch.update(postRef, FirestoreConstants.Post.COMMENTS_COUNT, FieldValue.increment(-1L))
                deleteBatch.commit().await()
            } else {
                // Legacy fallback: top-level comments/{commentId}
                val topLevelCommentRef = firestore
                    .collection(FirestoreConstants.Collections.COMMENTS)
                    .document(commentId)
                val topLevelCommentDoc = topLevelCommentRef.get().await()
                if (!topLevelCommentDoc.exists()) {
                    return Result.Error(Exception("Comment not found"))
                }

                val commentAuthorId = topLevelCommentDoc.getString(FirestoreConstants.Comment.AUTHOR_ID)
                if (commentAuthorId != currentUserId && postOwnerId != currentUserId) {
                    return Result.Error(Exception("Unauthorized: Cannot delete this comment"))
                }
                val deleteBatch = firestore.batch()
                deleteBatch.delete(topLevelCommentRef)
                deleteBatch.update(postRef, FirestoreConstants.Post.COMMENTS_COUNT, FieldValue.increment(-1L))
                deleteBatch.commit().await()
            }
            
            logger.d(TAG, "Comment $commentId deleted from post $postId by user $currentUserId")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Error deleting comment $commentId", e)
            Result.Error(e)
        }
    }
    
    override fun getCommentCount(postId: String): Flow<Int> = callbackFlow {
        val postRef = firestore
            .collection(FirestoreConstants.Collections.POSTS)
            .document(postId)
        
        val listener = postRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                logger.e(TAG, "Error listening to comment count for post $postId", error)
                trySend(0)
                return@addSnapshotListener
            }
            
            val count = snapshot?.getLong(FirestoreConstants.Post.COMMENTS_COUNT)?.toInt() ?: 0
            trySend(count)
        }
        
        awaitClose { listener.remove() }
    }
    
    // ========================================
    // USER SEARCH & DISCOVERY
    // ========================================
    
    override suspend fun searchUsers(query: String, limit: Int): Result<List<User>> {
        return try {
            logger.d(TAG, "🎯 Searching users with query: '$query', limit: $limit")
            
            // Use actual Firestore field names for user search
            val searchQuery = firestore
                .collection(FirestoreConstants.Collections.USERS_PUBLIC)
                .orderBy(FirestoreConstants.User.DISPLAY_NAME)
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(limit.coerceAtMost(50).toLong())
            
            val snapshot = searchQuery.get().await()
            val users = snapshot.documents.mapNotNull { document ->
                try {
                    document.toUser()
                } catch (e: Exception) {
                    logger.e(TAG, "Error converting user ${document.id} to domain model", e)
                    null
                }
            }
            
            logger.d(TAG, "Found ${users.size} users for query: $query")
            Result.Success(users)
        } catch (e: Exception) {
            logger.e(TAG, "Error searching users with query: $query", e)
            Result.Error(e)
        }
    }
    
    override suspend fun getSuggestedUsers(limit: Int): Result<List<User>> {
        return try {
            val currentUserId = auth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))
            
            // Simple suggestion: get users with high activity (most posts/followers)
            // In production, this would be more sophisticated with ML recommendations
            val snapshot = firestore
                .collection(FirestoreConstants.Collections.USERS_PUBLIC)
                .limit(limit.coerceAtMost(50).toLong())
                .get()
                .await()
            val users = snapshot.documents
                .filterNot { it.id == currentUserId }
                .mapNotNull { document ->
                try {
                    document.toUser()
                } catch (e: Exception) {
                    logger.e(TAG, "Error converting user ${document.id} to domain model", e)
                    null
                }
            }.sortedByDescending { it.followersCount }
            
            logger.d(TAG, "Found ${users.size} suggested users")
            Result.Success(users)
        } catch (e: Exception) {
            logger.e(TAG, "Error getting suggested users", e)
            Result.Error(e)
        }
    }
    
    override suspend fun getUserProfile(userId: String): Result<User> {
        return try {
            val userDoc = firestore
                .collection(FirestoreConstants.Collections.USERS_PUBLIC)
                .document(userId)
                .get()
                .await()
            
            if (!userDoc.exists()) {
                return Result.Error(Exception("User not found"))
            }
            
            val user = userDoc.toUser()
            Result.Success(user)
        } catch (e: Exception) {
            logger.e(TAG, "Error getting user profile for $userId", e)
            Result.Error(e)
        }
    }
    
    override suspend fun getMutualFollowers(targetUserId: String): Result<List<User>> {
        return try {
            val currentUserId = auth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))
            
            // Get users that current user follows
            val currentUserFollowingQuery = firestore
                .collection(FirestoreConstants.Collections.FOLLOWS)
                .whereEqualTo(FirestoreConstants.Follow.FOLLOWER_ID, currentUserId)
                .whereEqualTo(FirestoreConstants.Follow.IS_ACTIVE, true)
                .limit(200)
            
            val currentUserFollowingSnapshot = currentUserFollowingQuery.get().await()
            val currentUserFollowingIds = currentUserFollowingSnapshot.documents.mapNotNull { doc ->
                doc.getString(FirestoreConstants.Follow.FOLLOWED_ID)
            }
            
            if (currentUserFollowingIds.isEmpty()) {
                return Result.Success(emptyList())
            }
            
            // Get users that target user follows
            val targetUserFollowingQuery = firestore
                .collection(FirestoreConstants.Collections.FOLLOWS)
                .whereEqualTo(FirestoreConstants.Follow.FOLLOWER_ID, targetUserId)
                .whereEqualTo(FirestoreConstants.Follow.IS_ACTIVE, true)
                .limit(200)
            
            val targetUserFollowingSnapshot = targetUserFollowingQuery.get().await()
            val targetUserFollowingIds = targetUserFollowingSnapshot.documents.mapNotNull { doc ->
                doc.getString(FirestoreConstants.Follow.FOLLOWED_ID)
            }
            
            // Find mutual follows
            val mutualFollowIds = currentUserFollowingIds.intersect(targetUserFollowingIds.toSet())
            
            if (mutualFollowIds.isEmpty()) {
                return Result.Success(emptyList())
            }
            
            // Get user details for mutual follows
            val users = mutableListOf<User>()
            mutualFollowIds.toList().chunked(10).forEach { chunk ->
                val usersSnapshot = firestore
                    .collection(FirestoreConstants.Collections.USERS_PUBLIC)
                    .whereIn(FieldPath.documentId(), chunk)
                    .limit(chunk.size.toLong())
                    .get()
                    .await()

                users += usersSnapshot.documents.mapNotNull { document ->
                    try {
                        document.toUser()
                    } catch (e: Exception) {
                        logger.e(TAG, "Error converting user ${document.id} to domain model", e)
                        null
                    }
                }
            }
            
            logger.d(TAG, "Found ${users.size} mutual followers between $currentUserId and $targetUserId")
            Result.Success(users)
        } catch (e: Exception) {
            logger.e(TAG, "Error getting mutual followers for $targetUserId", e)
            Result.Error(e)
        }
    }
    
    // ========================================
    // BLOCK SYSTEM
    // ========================================
    
    override suspend fun blockUser(userId: String, reason: String?): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))
            
            if (currentUserId == userId) {
                return Result.Error(Exception("Cannot block yourself"))
            }
            
            logger.d(TAG, "🚫 Blocking user $userId by $currentUserId")
            
            // Get target user info for denormalization
            val targetUserDoc = firestore
                .collection(FirestoreConstants.Collections.USERS_PUBLIC)
                .document(userId)
                .get()
                .await()
            
            if (!targetUserDoc.exists()) {
                return Result.Error(Exception("User not found"))
            }
            
            val targetUserDisplayName = targetUserDoc.getString(FirestoreConstants.User.DISPLAY_NAME)
                ?: targetUserDoc.getString("displayName")
                ?: targetUserDoc.getString(FirestoreConstants.User.NICKNAME)
                ?: ""
            val targetUserPhotoUrl = targetUserDoc.getString(FirestoreConstants.User.PHOTO_URL)
            
            // Create block document
            val blockData = hashMapOf(
                FirestoreConstants.BlockedUser.BLOCKER_ID to currentUserId,
                FirestoreConstants.BlockedUser.BLOCKED_USER_ID to userId,
                FirestoreConstants.BlockedUser.BLOCKED_USER_NAME to targetUserDisplayName,
                FirestoreConstants.BlockedUser.BLOCKED_USER_PHOTO_URL to targetUserPhotoUrl,
                FirestoreConstants.FIELD_CREATED_AT to FieldValue.serverTimestamp(),
                FirestoreConstants.BlockedUser.REASON to reason
            )
            
            // Use subcollection structure: blocked_users/{blockerId}/blocks/{blockedUserId}
            firestore
                .collection(FirestoreConstants.BLOCKED_USERS)
                .document(currentUserId)
                .collection("blocks")
                .document(userId)
                .set(blockData)
                .await()

            // Write reciprocal record so blocked user can detect they are blocked
            // blocked_users/{blockedUserId}/blocked_by/{blockerId}
            firestore
                .collection(FirestoreConstants.BLOCKED_USERS)
                .document(userId)
                .collection("blocked_by")
                .document(currentUserId)
                .set(mapOf(FirestoreConstants.FIELD_CREATED_AT to FieldValue.serverTimestamp()))
                .await()

            // Invalidate local cache
            interactionBlockedUserIdsCache = null
            interactionBlockedUserIdsCacheAtMs = 0L
            
            // Automatically unfollow both ways
            try {
                // Current user unfollows blocked user
                firestore
                    .collection(FirestoreConstants.Collections.FOLLOWS)
                    .whereEqualTo(FirestoreConstants.Follow.FOLLOWER_ID, currentUserId)
                    .whereEqualTo(FirestoreConstants.Follow.FOLLOWED_ID, userId)
                    .limit(200)
                    .get()
                    .await()
                    .documents
                    .forEach { it.reference.delete().await() }
                
                // Blocked user unfollows current user
                firestore
                    .collection(FirestoreConstants.Collections.FOLLOWS)
                    .whereEqualTo(FirestoreConstants.Follow.FOLLOWER_ID, userId)
                    .whereEqualTo(FirestoreConstants.Follow.FOLLOWED_ID, currentUserId)
                    .limit(200)
                    .get()
                    .await()
                    .documents
                    .forEach { it.reference.delete().await() }
                
                logger.d(TAG, "✅ Auto-unfollowed both ways during block")
            } catch (e: Exception) {
                logger.w(TAG, "Warning: Could not unfollow during block", e)
                // Continue even if unfollow fails
            }
            
            logger.d(TAG, "✅ Successfully blocked user $userId")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "❌ Error blocking user $userId", e)
            Result.Error(e)
        }
    }
    
    override suspend fun unblockUser(userId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))

            logger.d(TAG, "✅ Unblocking user $userId by $currentUserId")

            val batch = firestore.batch()

            // Delete block document
            batch.delete(
                firestore.collection(FirestoreConstants.BLOCKED_USERS)
                    .document(currentUserId).collection("blocks").document(userId)
            )

            // Delete reciprocal blocked_by record
            batch.delete(
                firestore.collection(FirestoreConstants.BLOCKED_USERS)
                    .document(userId).collection("blocked_by").document(currentUserId)
            )

            batch.commit().await()

            // Invalidate local cache
            interactionBlockedUserIdsCache = null
            interactionBlockedUserIdsCacheAtMs = 0L

            logger.d(TAG, "✅ Successfully unblocked user $userId")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "❌ Error unblocking user $userId", e)
            Result.Error(e)
        }
    }
    
    override suspend fun isBlocked(userId: String): Result<Boolean> {
        return try {
            val currentUserId = auth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))
            
            val blockDoc = firestore
                .collection(FirestoreConstants.BLOCKED_USERS)
                .document(currentUserId)
                .collection("blocks")
                .document(userId)
                .get()
                .await()
            
            Result.Success(blockDoc.exists())
        } catch (e: Exception) {
            if (e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true) {
                Result.Success(false)
            } else {
                logger.e(TAG, "Error checking if user $userId is blocked", e)
                Result.Error(e)
            }
        }
    }
    
    override suspend fun isBlockedBy(userId: String): Result<Boolean> {
        return try {
            val currentUserId = auth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))

            // Check our own blocked_by subcollection — written by the other user when they blocked us
            val doc = firestore
                .collection(FirestoreConstants.BLOCKED_USERS)
                .document(currentUserId)
                .collection("blocked_by")
                .document(userId)
                .get()
                .await()

            Result.Success(doc.exists())
        } catch (e: Exception) {
            if (e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true) {
                Result.Success(false)
            } else {
                logger.e(TAG, "Error checking isBlockedBy for user $userId", e)
                Result.Error(e)
            }
        }
    }
    
    override fun getBlockedUsers(): Flow<List<com.omerkaya.sperrmuellfinder.domain.model.BlockedUser>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid
        
        if (currentUserId == null) {
            logger.e(TAG, "User not authenticated for blocked users flow")
            close()
            return@callbackFlow
        }
        
        logger.d(TAG, "📡 Starting real-time blocked users flow for user $currentUserId")
        
        val listenerRegistration = firestore
            .collection(FirestoreConstants.BLOCKED_USERS)
            .document(currentUserId)
            .collection("blocks")
            .orderBy(FirestoreConstants.FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .limit(200)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logger.e(TAG, "Error in blocked users listener", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val blockedUsers = snapshot.documents.mapNotNull { doc ->
                        try {
                            val blockedUserId = doc.getString(FirestoreConstants.BlockedUser.BLOCKED_USER_ID) ?: return@mapNotNull null
                            val blockedUserName = doc.getString(FirestoreConstants.BlockedUser.BLOCKED_USER_NAME)
                                ?.takeIf { it.isNotBlank() }
                                ?: ""
                            val blockedUserPhotoUrl = doc.getString(FirestoreConstants.BlockedUser.BLOCKED_USER_PHOTO_URL)
                            val createdAt = doc.getTimestamp(FirestoreConstants.FIELD_CREATED_AT)?.toDate() ?: java.util.Date()
                            val reason = doc.getString(FirestoreConstants.BlockedUser.REASON)
                            
                            com.omerkaya.sperrmuellfinder.domain.model.BlockedUser(
                                id = blockedUserId,
                                blockerId = currentUserId,
                                blockedUserId = blockedUserId,
                                blockedUserName = blockedUserName,
                                blockedUserPhotoUrl = blockedUserPhotoUrl,
                                createdAt = createdAt,
                                reason = reason
                            )
                        } catch (e: Exception) {
                            logger.e(TAG, "Error converting blocked user ${doc.id}", e)
                            null
                        }
                    }
                    
                    logger.d(TAG, "📦 Emitting ${blockedUsers.size} blocked users")
                    trySend(blockedUsers)
                } else {
                    logger.w(TAG, "Blocked users snapshot is null")
                    trySend(emptyList())
                }
            }
        
        awaitClose {
            logger.d(TAG, "🔌 Closing blocked users listener")
            listenerRegistration.remove()
        }
    }

    private data class FollowEdge(
        val otherUserId: String,
        val createdAtMillis: Long
    )

    private fun getFollowCreatedAtMillis(doc: com.google.firebase.firestore.DocumentSnapshot): Long {
        return doc.getTimestamp(FirestoreConstants.Follow.CREATED_AT)?.toDate()?.time
            ?: doc.getTimestamp("createdAt")?.toDate()?.time
            ?: 0L
    }

    private suspend fun fetchUsersByIds(userIds: Set<String>, blockedUserIds: Set<String>): List<User> {
        val visibleIds = userIds.filterNot { blockedUserIds.contains(it) }.distinct()
        if (visibleIds.isEmpty()) return emptyList()

        val usersById = PublicUserResolver.resolveUsersByIds(firestore, visibleIds)
        return visibleIds.mapNotNull { id -> usersById[id] }
    }

    private suspend fun getInteractionBlockedUserIds(): Set<String> {
        val now = System.currentTimeMillis()
        interactionBlockedUserIdsCache?.let { cached ->
            if (now - interactionBlockedUserIdsCacheAtMs < BLOCK_CACHE_TTL_MS) {
                return cached
            }
        }

        val currentUserId = auth.currentUser?.uid ?: return emptySet()
        val blockedByMe = try {
            firestore.collection(FirestoreConstants.BLOCKED_USERS)
                .document(currentUserId)
                .collection("blocks")
                .limit(200)
                .get()
                .await()
                .documents
                .mapNotNull { it.getString(FirestoreConstants.BlockedUser.BLOCKED_USER_ID) }
                .toSet()
        } catch (e: Exception) {
            if (e.message?.contains("PERMISSION_DENIED", ignoreCase = true) != true) {
                logger.e(TAG, "Error loading blockedByMe IDs", e)
            }
            emptySet()
        }

        // Reverse block lookup is not accessible with current Firestore rules.
        val blockedMe = emptySet<String>()

        return (blockedByMe + blockedMe).also {
            interactionBlockedUserIdsCache = it
            interactionBlockedUserIdsCacheAtMs = now
        }
    }
}
