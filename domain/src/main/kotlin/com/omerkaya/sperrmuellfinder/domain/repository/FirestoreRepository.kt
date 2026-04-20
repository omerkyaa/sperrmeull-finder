package com.omerkaya.sperrmuellfinder.domain.repository

import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.Post
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.model.Notification
import com.omerkaya.sperrmuellfinder.domain.model.Purchase
import kotlinx.coroutines.flow.Flow

/**
 * 🔥 COMPREHENSIVE FIRESTORE REPOSITORY - SperrmüllFinder
 * Rules.md compliant - Central repository for all Firestore operations
 * 
 * Features:
 * - Likes system (toggle, count, users)
 * - Favorites system (toggle, user favorites)
 * - XP & Honesty transactions
 * - Notifications (in-app & push)
 * - Analytics & Views tracking
 * - Premium status sync
 * - User management
 */
interface FirestoreRepository {
    
    // ========================================
    // LIKES SYSTEM
    // ========================================
    
    /**
     * Toggle like status for a post with optimistic UI support.
     * Creates/removes document in posts/{postId}/likes/{userId} subcollection.
     * Updates denormalized likesCount in post document.
     * Creates notification for post owner.
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
     * Get users who liked a specific post with real-time updates.
     * 
     * @param postId The post ID to get likes for
     * @return Flow of List containing users who liked the post
     */
    fun getPostLikes(postId: String): Flow<List<User>>
    
    /**
     * Get users who liked a specific post with real-time updates (Flow version).
     * 
     * @param postId The post ID to get likes for
     * @return Flow of Result containing users who liked the post
     */
    fun getLikesUsersFlow(postId: String): Flow<Result<List<User>>>
    
    /**
     * Get like count for a specific post with real-time updates.
     * 
     * @param postId The post ID to get like count for
     * @return Flow of like count
     */
    fun getPostLikeCount(postId: String): Flow<Int>
    
    /**
     * Get real-time like status for a specific post by current user.
     * 
     * @param postId The post ID to check
     * @param userId The user ID to check
     * @return Flow of Boolean indicating like status
     */
    fun isPostLikedByUserFlow(postId: String, userId: String): Flow<Boolean>
    
    // ========================================
    // FAVORITES SYSTEM
    // ========================================
    
    /**
     * Toggle favorite status for a post.
     * Creates/removes document in favorites collection.
     * Creates notification for post owner.
     * 
     * @param postId The post ID to favorite/unfavorite
     * @param userId The user ID performing the action
     * @return Result containing the new favorite status (true = favorited, false = unfavorited)
     */
    suspend fun toggleFavorite(postId: String, userId: String): Result<Boolean>
    
    /**
     * Check if a user has favorited a specific post.
     * 
     * @param postId The post ID to check
     * @param userId The user ID to check
     * @return Result containing favorite status
     */
    suspend fun isPostFavoritedByUser(postId: String, userId: String): Result<Boolean>
    
    /**
     * Get user's favorited posts with real-time updates.
     * Returns posts sorted by favorite date (newest first).
     * 
     * @param userId The user ID to get favorites for
     * @return Flow of List containing favorited posts
     */
    fun getUserFavorites(userId: String): Flow<List<Post>>
    
    /**
     * Get favorite count for a specific post.
     * 
     * @param postId The post ID to get favorite count for
     * @return Flow of favorite count
     */
    fun getPostFavoriteCount(postId: String): Flow<Int>
    
    // ========================================
    // COMMENTS SYSTEM
    // ========================================
    
    /**
     * Add comment to a post.
     * Creates document in comments collection.
     * Updates denormalized commentsCount in post document.
     * Creates notification for post owner.
     * 
     * @param postId The post ID to comment on
     * @param text Comment text
     * @return Result indicating success or failure
     */
    suspend fun addComment(postId: String, text: String): Result<Unit>
    
    /**
     * Get comments for a specific post with real-time updates.
     * 
     * @param postId The post ID to get comments for
     * @return Flow of Result containing list of comments
     */
    fun getComments(postId: String): Flow<Result<List<com.omerkaya.sperrmuellfinder.domain.model.Comment>>>
    
    /**
     * Get users who liked a specific post.
     * 
     * @param postId The post ID to get likes for
     * @return Result containing list of users who liked the post
     */
    suspend fun getLikesUsers(postId: String): Result<List<User>>
    
    /**
     * Mark notification as read (simplified version).
     * 
     * @param notificationId The notification ID to mark as read
     * @return Result indicating success or failure
     */
    suspend fun markNotificationAsRead(notificationId: String): Result<Unit>
    
    // ========================================
    // XP & HONESTY TRANSACTIONS
    // ========================================
    
    // ========================================
    // NOTIFICATIONS SYSTEM
    // ========================================
    
    /**
     * Add in-app notification for a user.
     * Creates document in notifications/{userId}/{notificationId}.
     * 
     * @param userId The user ID to send notification to
     * @param type Notification type (from FirestoreConstants)
     * @param title Notification title (localized)
     * @param body Notification body (localized)
     * @param data Additional data for deep linking
     * @return Result containing the notification ID
     */
    suspend fun addNotification(
        userId: String,
        type: String,
        title: String,
        body: String,
        data: Map<String, Any> = emptyMap()
    ): Result<String>
    
    /**
     * Mark notification as read.
     * 
     * @param userId The user ID
     * @param notificationId The notification ID to mark as read
     * @return Result indicating success or failure
     */
    suspend fun markNotificationAsRead(userId: String, notificationId: String): Result<Unit>
    
    /**
     * Get user's notifications with real-time updates.
     * 
     * @param userId The user ID to get notifications for
     * @return Flow of List containing notifications (newest first)
     */
    fun getUserNotifications(userId: String): Flow<List<Notification>>
    
    /**
     * Get unread notification count for a user.
     * 
     * @param userId The user ID to get unread count for
     * @return Flow of unread notification count
     */
    fun getUnreadNotificationCount(userId: String): Flow<Int>
    
    // ========================================
    // ANALYTICS & VIEWS TRACKING
    // ========================================
    
    /**
     * Increment view count for a post.
     * Creates document in post_views/{postId}/{viewId}.
     * Updates denormalized viewsCount in post document.
     * 
     * @param postId The post ID to increment views for
     * @param viewerId The user ID viewing the post (null for anonymous)
     * @return Result indicating success or failure
     */
    suspend fun incrementViewCount(postId: String, viewerId: String? = null): Result<Unit>
    
    /**
     * Increment share count for a post.
     * Updates denormalized sharesCount in post document.
     * 
     * @param postId The post ID to increment shares for
     * @param sharerId The user ID sharing the post
     * @return Result indicating success or failure
     */
    suspend fun incrementShareCount(postId: String, sharerId: String): Result<Unit>
    
    /**
     * Track analytics event.
     * Creates document in analytics collection.
     * 
     * @param userId The user ID performing the action (null for anonymous)
     * @param eventType Event type (from AppConstants)
     * @param eventData Additional event data
     * @return Result indicating success or failure
     */
    suspend fun trackAnalyticsEvent(
        userId: String?,
        eventType: String,
        eventData: Map<String, Any> = emptyMap()
    ): Result<Unit>
    
    // ========================================
    // USER MANAGEMENT
    // ========================================
    
    /**
     * Update user's premium status in Firestore.
     * This is for informational purposes only - RevenueCat is the source of truth.
     * 
     * @param userId The user ID to update
     * @param isPremium Premium status
     * @param premiumUntil Premium expiration date (null if not premium)
     * @param premiumType Premium type (from RevenueCatConstants)
     * @return Result indicating success or failure
     */
    suspend fun updateUserPremiumStatus(
        userId: String,
        isPremium: Boolean,
        premiumUntil: Long? = null,
        premiumType: String? = null
    ): Result<Unit>
    
    /**
     * Get user by ID with real-time updates.
     * 
     * @param userId The user ID to get
     * @return Flow of User or null if not found
     */
    fun getUser(userId: String): Flow<User?>
    
    /**
     * Update user's favorite regions and categories.
     * 
     * @param userId The user ID to update
     * @param favoriteRegions List of favorite region names
     * @param favoriteCategories List of favorite category IDs (English)
     * @return Result indicating success or failure
     */
    suspend fun updateUserFavorites(
        userId: String,
        favoriteRegions: List<String>,
        favoriteCategories: List<String>
    ): Result<Unit>
    
    // ========================================
    // PURCHASE TRACKING
    // ========================================
    
    /**
     * Record a successful purchase.
     * Creates document in purchases/{userId}/{purchaseId}.
     * 
     * @param userId The user ID who made the purchase
     * @param productId Product ID from RevenueCat
     * @param purchaseToken Purchase token
     * @param revenueCatTransactionId RevenueCat transaction ID
     * @param purchaseTime Purchase timestamp
     * @return Result containing the purchase document ID
     */
    suspend fun recordPurchase(
        userId: String,
        productId: String,
        purchaseToken: String,
        revenueCatTransactionId: String,
        purchaseTime: Long
    ): Result<String>
    
    /**
     * Get user's purchase history.
     * 
     * @param userId The user ID to get purchases for
     * @return Flow of List containing purchase records
     */
    fun getUserPurchases(userId: String): Flow<List<Purchase>>
    
    // ========================================
    // FOLLOW SYSTEM
    // ========================================
    
    /**
     * Follow a user.
     * Creates documents in both followers and following collections.
     * 
     * @param followerId The user ID who is following
     * @param followedId The user ID being followed
     * @return Result indicating success or failure
     */
    suspend fun followUser(followerId: String, followedId: String): Result<Unit>
    
    /**
     * Unfollow a user.
     * Removes documents from both followers and following collections.
     * 
     * @param followerId The user ID who is unfollowing
     * @param followedId The user ID being unfollowed
     * @return Result indicating success or failure
     */
    suspend fun unfollowUser(followerId: String, followedId: String): Result<Unit>
    
    /**
     * Check if a user is following another user.
     * 
     * @param followerId The user ID who might be following
     * @param followedId The user ID being checked
     * @return Flow of Boolean indicating follow status
     */
    fun isFollowing(followerId: String, followedId: String): Flow<Boolean>
    
    /**
     * Get followers count for a user with real-time updates.
     * 
     * @param userId The user ID to get followers count for
     * @return Flow of followers count
     */
    fun getFollowersCount(userId: String): Flow<Int>
    
    /**
     * Get following count for a user with real-time updates.
     * 
     * @param userId The user ID to get following count for
     * @return Flow of following count
     */
    fun getFollowingCount(userId: String): Flow<Int>
    
    /**
     * Get followers list for a user with real-time updates.
     * 
     * @param userId The user ID to get followers for
     * @return Flow of List containing follower users
     */
    fun getFollowers(userId: String): Flow<List<User>>
    
    /**
     * Get following list for a user with real-time updates.
     * 
     * @param userId The user ID to get following for
     * @return Flow of List containing followed users
     */
    fun getFollowing(userId: String): Flow<List<User>>
    
    // ========================================
    // POSTS SYSTEM
    // ========================================
    
    /**
     * Get posts by user ID with real-time updates.
     * 
     * @param userId The user ID to get posts for
     * @return Flow of List containing user's posts
     */
    fun getUserPosts(userId: String): Flow<List<Post>>
    
    /**
     * Get user's archived posts with real-time updates.
     * 
     * @param userId The user ID to get archived posts for
     * @return Flow of List containing archived posts
     */
    fun getUserArchivedPosts(userId: String): Flow<List<Post>>
    
    /**
     * Get user's posts count with real-time updates.
     * 
     * @param userId The user ID to get posts count for
     * @return Flow of posts count
     */
    fun getUserPostsCount(userId: String): Flow<Int>
}

