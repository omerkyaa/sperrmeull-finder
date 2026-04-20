package com.omerkaya.sperrmuellfinder.domain.repository

import androidx.paging.PagingData
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.Post
import kotlinx.coroutines.flow.Flow

/**
 * 🏠 FEED REPOSITORY INTERFACE - SperrmüllFinder
 * Rules.md compliant - Clean Architecture domain layer
 * 
 * Features:
 * - Paging 3 integration for infinite scroll
 * - Real-time post updates with snapshot listeners
 * - Like/Unlike functionality with atomic operations
 * - Location-based filtering (Premium vs Basic radius)
 * - Post status filtering (active posts only)
 * - Optimistic UI support for likes
 */
interface FeedRepository {
    
    /**
     * Get paginated feed posts with real-time updates.
     * Posts are filtered by status="active" and sorted by createdAt desc.
     * 
     * @param userId Current user ID for personalization
     * @param radiusMeters Search radius in meters (Basic: 1500m, Premium: unlimited)
     * @param userLatitude User's current latitude
     * @param userLongitude User's current longitude
     * @return Flow of PagingData containing posts
     */
    fun getPagedFeed(
        userId: String,
        isPremium: Boolean,
        earlyAccessMinutes: Int,
        radiusMeters: Int = 1500,
        userLatitude: Double? = null,
        userLongitude: Double? = null
    ): Flow<PagingData<Post>>
    
    /**
     * Toggle like status for a post with atomic operations.
     * Creates/removes document in posts/{postId}/likes/{userId} subcollection.
     * Updates denormalized likesCount in post document.
     * Creates notification for post owner if liked.
     * 
     * @param postId The post ID to like/unlike
     * @param userId The user ID performing the action
     * @return Result containing the new like status (true = liked, false = unliked)
     */
    suspend fun toggleLike(postId: String, userId: String): Result<Boolean>
    
    /**
     * Check if a user has liked a specific post.
     * 
     * @param postId The post ID to check
     * @param userId The user ID to check
     * @return Result containing like status
     */
    suspend fun isPostLikedByUser(postId: String, userId: String): Result<Boolean>
    
    /**
     * Get real-time updates for a specific post.
     * Used for updating post cards when data changes.
     * 
     * @param postId The post ID to observe
     * @return Flow of Post updates
     */
    fun observePost(postId: String): Flow<Post?>
    
    /**
     * Refresh the feed data source.
     * Used for pull-to-refresh functionality.
     */
    suspend fun refreshFeed(): Result<Unit>
    
    /**
     * Increment view count for a post.
     * Called when user views a post detail.
     * 
     * @param postId The post ID to increment views for
     * @param userId The user ID viewing the post
     */
    suspend fun incrementViewCount(postId: String, userId: String): Result<Unit>
    
    /**
     * Increment share count for a post.
     * Called when user shares a post externally.
     * 
     * @param postId The post ID to increment shares for
     * @param userId The user ID sharing the post
     */
    suspend fun incrementShareCount(postId: String, userId: String): Result<Unit>
}
