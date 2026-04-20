package com.omerkaya.sperrmuellfinder.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.data.dto.LikeDto
import com.omerkaya.sperrmuellfinder.data.mapper.LikeMapper.toDomain
import com.omerkaya.sperrmuellfinder.data.mapper.toUser
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants
import com.omerkaya.sperrmuellfinder.data.util.PublicUserResolver
import com.omerkaya.sperrmuellfinder.domain.model.Like
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.model.UserFavorites
import com.omerkaya.sperrmuellfinder.domain.repository.LikesRepository
import com.omerkaya.sperrmuellfinder.domain.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 💖 LIKES REPOSITORY IMPLEMENTATION - SperrmüllFinder
 * Real-time Firebase likes with denormalized user data fetching
 * Rules.md compliant - Clean Architecture data layer
 */
@Singleton
class LikesRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val notificationRepository: NotificationRepository
) : LikesRepository {
    private var blockedUserIdsCache: Set<String>? = null
    private var blockedUserIdsCacheAtMs: Long = 0L

    companion object {
        private const val TAG = "LikesRepository"
        private const val BLOCK_CACHE_TTL_MS = 30_000L
    }

    override fun getPostLikesUsers(postId: String): Flow<Result<List<User>>> = callbackFlow {
        Log.d(TAG, "💖 Getting likes for post: $postId")

        var listenerRegistration: ListenerRegistration? = null

        try {
            trySend(Result.Loading)

            listenerRegistration = firestore
                .collection(FirestoreConstants.Collections.POSTS)
                .document(postId)
                .collection(FirestoreConstants.Subcollections.LIKES)
                .orderBy(FirestoreConstants.Like.CREATED_AT, Query.Direction.DESCENDING)
                .limit(200)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "❌ Error listening to likes for post $postId", error)
                        trySend(Result.Error(error))
                        return@addSnapshotListener
                    }

                    if (snapshot == null || snapshot.isEmpty) {
                        trySend(Result.Success(emptyList()))
                        return@addSnapshotListener
                    }

                    CoroutineScope(Dispatchers.IO).launch {
                        val blockedUserIds = getBlockedUserIds()

                        // Parse like documents keeping denormalized user data as fallback
                        data class LikeEntry(
                            val userId: String,
                            val username: String,
                            val photoUrl: String?,
                            val level: Int,
                            val isPremium: Boolean
                        )

                        val likeEntries = snapshot.documents.mapNotNull { doc ->
                            val userId = doc.getString(FirestoreConstants.Like.USER_ID)
                                ?: doc.getString("user_id")
                                ?: doc.getString("userid")
                                ?: doc.getString("uid")
                                ?: doc.id
                            if (blockedUserIds.contains(userId)) return@mapNotNull null
                            LikeEntry(
                                userId = userId,
                                username = doc.getString(FirestoreConstants.Like.USERNAME)
                                    ?: doc.getString("username") ?: "",
                                photoUrl = doc.getString(FirestoreConstants.Like.USER_PHOTO_URL)
                                    ?: doc.getString("userPhotoUrl"),
                                level = (doc.getLong(FirestoreConstants.Like.USER_LEVEL)
                                    ?: doc.getLong("userLevel") ?: 1L).toInt(),
                                isPremium = doc.getBoolean(FirestoreConstants.Like.USER_IS_PREMIUM)
                                    ?: doc.getBoolean("userIsPremium") ?: false
                            )
                        }.distinctBy { it.userId }

                        if (likeEntries.isEmpty()) {
                            trySend(Result.Success(emptyList()))
                            return@launch
                        }

                        try {
                            // Try to resolve full profiles from users_public
                            val userIds = likeEntries.map { it.userId }
                            val resolvedUsers = PublicUserResolver.resolveUsersByIds(firestore, userIds)

                            // For any user not resolved, fall back to denormalized like data
                            val users = likeEntries.map { entry ->
                                resolvedUsers[entry.userId] ?: User(
                                    uid = entry.userId,
                                    email = "",
                                    displayName = entry.username.ifBlank { "User" },
                                    nickname = entry.username.ifBlank { "User" },
                                    photoUrl = entry.photoUrl,
                                    city = null,
                                    dob = null,
                                    gender = null,
                                    xp = 0,
                                    level = entry.level,
                                    honesty = 100,
                                    isPremium = entry.isPremium,
                                    premiumUntil = null,
                                    badges = emptyList(),
                                    favorites = UserFavorites(),
                                    fcmToken = null,
                                    deviceTokens = emptyList(),
                                    deviceLang = "",
                                    deviceModel = "",
                                    deviceOs = "",
                                    frameLevel = 0,
                                    followersCount = 0,
                                    followingCount = 0,
                                    createdAt = null,
                                    updatedAt = null,
                                    lastLoginAt = null
                                )
                            }
                            trySend(Result.Success(users))
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error resolving users for likes, using denormalized fallback", e)
                            val fallbackUsers = likeEntries.map { entry ->
                                User(
                                    uid = entry.userId,
                                    email = "",
                                    displayName = entry.username.ifBlank { "User" },
                                    nickname = entry.username.ifBlank { "User" },
                                    photoUrl = entry.photoUrl,
                                    city = null, dob = null, gender = null,
                                    xp = 0, level = entry.level, honesty = 100,
                                    isPremium = entry.isPremium, premiumUntil = null,
                                    badges = emptyList(), favorites = UserFavorites(),
                                    fcmToken = null, deviceTokens = emptyList(),
                                    deviceLang = "", deviceModel = "", deviceOs = "",
                                    frameLevel = 0, followersCount = 0, followingCount = 0,
                                    createdAt = null, updatedAt = null, lastLoginAt = null
                                )
                            }
                            trySend(Result.Success(fallbackUsers))
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up likes listener for post $postId", e)
            trySend(Result.Error(e))
        }

        awaitClose {
            listenerRegistration?.remove()
            Log.d(TAG, "Closed likes listener for post $postId")
        }
    }



    override suspend fun togglePostLike(postId: String, userId: String): Result<Boolean> {
        return try {
            Log.d(TAG, "Toggling like for user $userId on post $postId")

            val likeRef = firestore
                .collection(FirestoreConstants.Collections.POSTS)
                .document(postId)
                .collection(FirestoreConstants.Subcollections.LIKES)
                .document(userId)
            val existingLike = likeRef.get().await()

            if (!existingLike.exists()) {
                // User hasn't liked this post, so add a like
                Log.d(TAG, "💖 Adding like for user $userId on post $postId")
                
                // First, get user details for denormalized data
                val publicDoc = firestore
                    .collection(FirestoreConstants.Collections.USERS_PUBLIC)
                    .document(userId)
                    .get()
                    .await()
                val userDoc = if (publicDoc.exists()) {
                    publicDoc
                } else {
                    firestore
                        .collection(FirestoreConstants.Collections.USERS_PRIVATE)
                        .document(userId)
                        .get()
                        .await()
                }
                
                val user = try {
                    userDoc.toUser()
                } catch (e: Exception) {
                    val authUser = FirebaseAuth.getInstance().currentUser
                    User(
                        uid = userId,
                        email = authUser?.email ?: "",
                        displayName = authUser?.displayName ?: "User",
                        nickname = authUser?.displayName ?: "User",
                        photoUrl = authUser?.photoUrl?.toString(),
                        city = null,
                        dob = null,
                        gender = null,
                        xp = 0,
                        level = 0,
                        honesty = 100,
                        isPremium = false,
                        premiumUntil = null,
                        badges = emptyList(),
                        favorites = UserFavorites(),
                        fcmToken = null,
                        deviceTokens = emptyList(),
                        deviceLang = "",
                        deviceModel = "",
                        deviceOs = "",
                        frameLevel = 0,
                        followersCount = 0,
                        followingCount = 0,
                        createdAt = null,
                        updatedAt = null,
                        lastLoginAt = null
                    )
                }
                
                val likeDto = LikeDto(
                    postId = postId,
                    userId = userId,
                    username = user.displayName,
                    userPhotoUrl = user.photoUrl,
                    userLevel = user.level,
                    userIsPremium = user.isPremium,
                    createdAt = Date(),
                    updatedAt = Date()
                )
                likeRef.set(likeDto).await()
                
                Log.d(TAG, "✅ Successfully added like for user $userId on post $postId")
                
                // Create notification for post owner
                try {
                    val postDoc = firestore
                        .collection(FirestoreConstants.Collections.POSTS)
                        .document(postId)
                        .get()
                        .await()
                    
                    val postOwnerId = postDoc.getString(FirestoreConstants.Post.OWNER_ID)
                    if (postOwnerId != null && postOwnerId != userId) {
                        notificationRepository.createLikeNotification(
                            postId = postId,
                            postOwnerId = postOwnerId,
                            likerUserId = userId,
                            likerUserName = user.displayName
                        )
                        Log.d(TAG, "Like notification created for post owner: $postOwnerId")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating like notification", e)
                    // Don't fail the like operation if notification fails
                }
                
                Result.Success(true) // Now liked
                
            } else {
                // User has already liked this post, so remove the like
                Log.d(TAG, "Removing like for user $userId on post $postId")
                
                likeRef.delete().await()
                
                Log.d(TAG, "Successfully removed like for user $userId on post $postId")
                Result.Success(false) // Now unliked
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling like for user $userId on post $postId", e)
            Result.Error(e)
        }
    }

    override fun getPostLikes(postId: String): Flow<Result<List<Like>>> = callbackFlow {
        Log.d(TAG, "💖 Getting all likes for post: $postId")
        
        var listenerRegistration: ListenerRegistration? = null
        
        try {
            // RESTORED: Use orderBy for proper sorting (requires composite index)
            listenerRegistration = firestore
                .collection(FirestoreConstants.Collections.POSTS)
                .document(postId)
                .collection(FirestoreConstants.Subcollections.LIKES)
                .orderBy(FirestoreConstants.Like.CREATED_AT, Query.Direction.DESCENDING)
                .limit(200)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to likes for post $postId", error)
                        trySend(Result.Error(error))
                        return@addSnapshotListener
                    }
                    
                    if (snapshot == null) {
                        trySend(Result.Success(emptyList()))
                        return@addSnapshotListener
                    }
                    
                    CoroutineScope(Dispatchers.IO).launch {
                        val blockedUserIds = getBlockedUserIds()
                        val likes = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(LikeDto::class.java)
                                ?.toDomain()
                                ?.takeIf { like -> !blockedUserIds.contains(like.userId) }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error converting like document ${doc.id}", e)
                            null
                        }
                        }
                        Log.d(TAG, "✅ Successfully fetched ${likes.size} visible likes for post $postId")
                        trySend(Result.Success(likes))
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up likes listener for post $postId", e)
            trySend(Result.Error(e))
        }
        
        awaitClose {
            listenerRegistration?.remove()
            Log.d(TAG, "Closed likes listener for post $postId")
        }
    }

    /**
     * Check if current user has liked a specific post with real-time updates
     */
    override fun isPostLikedByUser(postId: String, userId: String): Flow<Result<Boolean>> = callbackFlow {
        Log.d(TAG, "Checking like status for post: $postId, user: $userId")
        
        val listenerRegistration = firestore
            .collection(FirestoreConstants.Collections.POSTS)
            .document(postId)
            .collection(FirestoreConstants.Subcollections.LIKES)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for like status: $postId, user: $userId", error)
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }
                
                val isLiked = snapshot?.exists() == true
                Log.d(TAG, "Like status for post $postId by user $userId: $isLiked")
                android.util.Log.d("LikesRepository", "💖 FIRESTORE LIKE STATUS: post=$postId, user=$userId, exists=${snapshot?.exists()}, isLiked=$isLiked")
                trySend(Result.Success(isLiked))
            }
        
        awaitClose {
            Log.d(TAG, "Stopping like status listener for post: $postId, user: $userId")
            listenerRegistration.remove()
        }
    }

    /**
     * Get real-time count of likes for a post
     */
    override fun getPostLikesCount(postId: String): Flow<Result<Int>> = callbackFlow {
        Log.d(TAG, "Getting like count for post: $postId")
        
        val listenerRegistration = firestore
            .collection(FirestoreConstants.Collections.POSTS)
            .document(postId)
            .collection(FirestoreConstants.Subcollections.LIKES)
            .limit(200)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for like count: $postId", error)
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }

                CoroutineScope(Dispatchers.IO).launch {
                    val blockedUserIds = getBlockedUserIds()
                    val count = snapshot?.documents
                        ?.mapNotNull { doc ->
                            doc.getString(FirestoreConstants.Like.USER_ID)
                                ?: doc.getString("user_id")
                                ?: doc.getString("userid")
                                ?: doc.getString("uid")
                                ?: doc.id
                        }
                        ?.filterNot { blockedUserIds.contains(it) }
                        ?.distinct()
                        ?.size
                        ?: 0
                    Log.d(TAG, "Like count for post $postId: $count")
                    trySend(Result.Success(count))
                }
            }
        
        awaitClose {
            Log.d(TAG, "Stopping like count listener for post: $postId")
            listenerRegistration.remove()
        }
    }

    private suspend fun getBlockedUserIds(): Set<String> {
        val now = System.currentTimeMillis()
        blockedUserIdsCache?.let { cached ->
            if (now - blockedUserIdsCacheAtMs < BLOCK_CACHE_TTL_MS) {
                return cached
            }
        }
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return emptySet()

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
                Log.e(TAG, "Error loading blockedByMe IDs", e)
            }
            emptySet()
        }

        // Reverse block lookup is not accessible with current Firestore rules.
        val blockedMe = emptySet<String>()

        return (blockedByMe + blockedMe).also {
            blockedUserIdsCache = it
            blockedUserIdsCacheAtMs = now
        }
    }
}