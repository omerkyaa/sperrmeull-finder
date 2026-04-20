package com.omerkaya.sperrmuellfinder.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.data.datasource.FeedPagingSource
import com.omerkaya.sperrmuellfinder.data.mapper.PostMapper
import com.omerkaya.sperrmuellfinder.data.mapper.toPost
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants
import com.omerkaya.sperrmuellfinder.domain.model.Post
import com.omerkaya.sperrmuellfinder.domain.repository.FeedRepository
import com.omerkaya.sperrmuellfinder.domain.repository.NotificationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🏠 FEED REPOSITORY IMPLEMENTATION - SperrmüllFinder
 * Rules.md compliant - Clean Architecture data layer
 * 
 * Features:
 * - Paging 3 integration with Firestore
 * - Real-time post updates
 * - Atomic like/unlike operations
 * - Notification creation for interactions
 * - Location-based filtering
 * - Professional error handling and logging
 */
@Singleton
class FeedRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val postMapper: PostMapper,
    private val auth: FirebaseAuth,
    private val notificationRepository: NotificationRepository,
    private val logger: Logger
) : FeedRepository {
    
    companion object {
        private const val TAG = "FeedRepository"
        private const val PAGE_SIZE = 20
    }
    
    override fun getPagedFeed(
        userId: String,
        isPremium: Boolean,
        earlyAccessMinutes: Int,
        radiusMeters: Int,
        userLatitude: Double?,
        userLongitude: Double?
    ): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                prefetchDistance = 5
            ),
            pagingSourceFactory = {
                FeedPagingSource(
                    firestore = firestore,
                    postMapper = postMapper,
                    userId = userId,
                    isPremium = isPremium,
                    earlyAccessMinutes = earlyAccessMinutes,
                    radiusMeters = radiusMeters,
                    userLatitude = userLatitude,
                    userLongitude = userLongitude,
                    logger = logger
                )
            }
        ).flow
    }
    
    override suspend fun toggleLike(postId: String, userId: String): Result<Boolean> {
        return try {
            val currentUserId = auth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))
            var isLiked = false
            
            firestore.runTransaction { transaction ->
                // References
                val postRef = firestore.collection(FirestoreConstants.Collections.POSTS).document(postId)
                val likeRef = postRef.collection(FirestoreConstants.Subcollections.LIKES).document(currentUserId)
                
                // Get current state
                val postSnapshot = transaction.get(postRef)
                val likeSnapshot = transaction.get(likeRef)
                
                if (!postSnapshot.exists()) {
                    throw Exception("Post not found")
                }
                
                val post = postSnapshot.toPost()
                val postOwnerId = post.ownerId
                
                if (likeSnapshot.exists()) {
                    // Unlike: Remove like document and decrement count (floor at 0)
                    val currentLikesCount = postSnapshot.getLong(FirestoreConstants.FIELD_LIKES_COUNT) ?: 0L
                    transaction.delete(likeRef)
                    transaction.update(postRef, FirestoreConstants.FIELD_LIKES_COUNT, maxOf(0L, currentLikesCount - 1L))
                    isLiked = false
                    logger.d(TAG, "Post $postId unliked by user $userId")
                } else {
                    // Like: Create like document and increment count
                    val likeData = mapOf(
                        FirestoreConstants.Like.USER_ID to currentUserId,
                        FirestoreConstants.Like.POST_ID to postId,
                        FirestoreConstants.Like.CREATED_AT to FieldValue.serverTimestamp()
                    )
                    transaction.set(likeRef, likeData)
                    transaction.update(postRef, FirestoreConstants.FIELD_LIKES_COUNT, FieldValue.increment(1L))
                    isLiked = true
                    
                    // Create notification for post owner (if not self-like)
                    if (postOwnerId != currentUserId) {
                        // Note: We'll create notification after transaction completes
                        logger.d(TAG, "Post $postId liked by user $currentUserId, will notify owner $postOwnerId")
                    }
                }
                
                // Return the new like status
                isLiked
            }.await()
            
            // Create notification outside of transaction if it was a like (not unlike)
            if (isLiked) {
                val postSnapshot = firestore.collection(FirestoreConstants.Collections.POSTS).document(postId).get().await()
                val userSnapshot = firestore.collection(FirestoreConstants.Collections.USERS).document(currentUserId).get().await()
                
                if (postSnapshot.exists() && userSnapshot.exists()) {
                    val post = postSnapshot.toPost()
                    val userName = userSnapshot.getString(FirestoreConstants.FIELD_DISPLAY_NAME) ?: "Unknown User"
                    
                    if (post.ownerId != currentUserId) {
                        notificationRepository.createLikeNotification(
                            postId = postId,
                            postOwnerId = post.ownerId,
                            likerUserId = currentUserId,
                            likerUserName = userName
                        )
                    }
                }
            }
            
            Result.Success(isLiked)
        } catch (e: Exception) {
            logger.e(TAG, "Error toggling like for post $postId", e)
            Result.Error(e)
        }
    }
    
    override suspend fun isPostLikedByUser(postId: String, userId: String): Result<Boolean> {
        return try {
            val likeRef = firestore
                .collection(FirestoreConstants.Collections.POSTS)
                .document(postId)
                .collection(FirestoreConstants.Subcollections.LIKES)
                .document(userId)
            
            val snapshot = likeRef.get().await()
            Result.Success(snapshot.exists())
        } catch (e: Exception) {
            logger.e(TAG, "Error checking like status for post $postId", e)
            Result.Error(e)
        }
    }
    
    override fun observePost(postId: String): Flow<Post?> = callbackFlow {
        val postRef = firestore.collection(FirestoreConstants.Collections.POSTS).document(postId)
        
        val listener = postRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                logger.e(TAG, "Error observing post $postId", error)
                trySend(null)
                return@addSnapshotListener
            }
            
            if (snapshot != null && snapshot.exists()) {
                try {
                    val post = snapshot.toPost()
                    trySend(post)
                } catch (e: Exception) {
                    logger.e(TAG, "Error converting post $postId to domain model", e)
                    trySend(null)
                }
            } else {
                trySend(null)
            }
        }
        
        awaitClose { listener.remove() }
    }
    
    override suspend fun refreshFeed(): Result<Unit> {
        return try {
            // For Paging 3, refresh is handled by invalidating the PagingSource
            // This method can be used for any additional refresh logic if needed
            logger.d(TAG, "Feed refresh requested")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Error refreshing feed", e)
            Result.Error(e)
        }
    }
    
    override suspend fun incrementViewCount(postId: String, userId: String): Result<Unit> {
        logger.d(TAG, "View counter is server-managed. Skipping client write for post=$postId")
        return Result.Success(Unit)
    }
    
    override suspend fun incrementShareCount(postId: String, userId: String): Result<Unit> {
        logger.d(TAG, "Share counter is server-managed. Skipping client write for post=$postId")
        return Result.Success(Unit)
    }
}
