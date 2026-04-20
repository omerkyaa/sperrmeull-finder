package com.omerkaya.sperrmuellfinder.domain.repository

import androidx.paging.PagingData
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.Comment
import com.omerkaya.sperrmuellfinder.domain.model.Post
import com.omerkaya.sperrmuellfinder.core.model.PostLocation
import com.omerkaya.sperrmuellfinder.domain.model.Report
import com.omerkaya.sperrmuellfinder.domain.model.ReportReason
import com.omerkaya.sperrmuellfinder.domain.model.ReportTargetType
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Repository interface for managing posts, comments, and related interactions.
 * Handles all post-related data operations including CRUD, interactions, and filtering.
 */
interface PostRepository {

    /**
     * Observes posts creation events for real-time updates across the app.
     * Emits whenever a new post is created.
     */
    val postCreationEvents: Flow<Post>

    /**
     * Listen to all posts in real-time with proper ordering (newest first).
     * Uses addSnapshotListener for instant updates when posts change.
     * This is the main feed for HomeScreen showing all posts sorted by creation date.
     * 
     * @return Flow of List containing all posts sorted by newest first
     */
    fun listenToPosts(): Flow<List<Post>>

    /**
     * Observes home feed posts with realtime updates from Firestore.
     * Returns posts sorted by creation date (newest first) with denormalized user data.
     * Automatically updates when posts or user data changes.
     * 
     * @param pageSize Number of posts to load per page (default: 20)
     * @param startAfter Timestamp to start after for pagination (null for first page)
     * @return Flow of List containing posts for home feed
     */
    fun listenHomePosts(
        pageSize: Int = 20,
        startAfter: Long? = null
    ): Flow<List<Post>>

    /**
     * Toggles like status for a post with optimistic UI support.
     * Updates both the likes subcollection and the denormalized likeCount.
     * 
     * @param postId The post ID to like/unlike
     * @param currentlyLiked Current like status for optimistic UI
     * @return Result containing the updated like status
     */
    suspend fun toggleLike(postId: String, currentlyLiked: Boolean): Result<Boolean>

    /**
     * Listens to comments count for a specific post (realtime).
     * 
     * @param postId The post ID to listen for comments count
     * @return Flow of comments count
     */
    fun listenCommentsCount(postId: String): Flow<Int>

    /**
     * Observes posts in the user's area with pagination support.
     * Posts are filtered by location and sorted by creation date (newest first).
     * 
     * @param userLocation Current user location for distance calculation and filtering
     * @param radiusMeters Search radius in meters (Basic: 1500m, Premium: unlimited)
     * @return Flow of PagingData containing posts
     */
    fun getPostsNearUser(
        userLocation: PostLocation,
        radiusMeters: Int = 1500
    ): Flow<PagingData<Post>>

    /**
     * Observes posts in the user's area sorted by date (newest first).
     * For basic users - 1.5km radius with date sorting.
     * 
     * @param userLocation Current user location for distance calculation and filtering
     * @param radiusMeters Search radius in meters
     * @return Flow of PagingData containing posts sorted by newest first
     */
    fun getPostsNearUserSortedByDate(
        userLocation: PostLocation,
        radiusMeters: Int = 1500
    ): Flow<PagingData<Post>>

    /**
     * Observes posts in the user's area sorted by distance (nearest first), then by date.
     * For premium users - 20km radius with distance + date sorting.
     * 
     * @param userLocation Current user location for distance calculation and filtering
     * @param radiusMeters Search radius in meters
     * @return Flow of PagingData containing posts sorted by distance then date
     */
    fun getPostsNearUserSortedByDistance(
        userLocation: PostLocation,
        radiusMeters: Int = 20000
    ): Flow<PagingData<Post>>

    /**
     * Observes posts by a specific user.
     * 
     * @param userId The user ID to fetch posts for
     * @param includeArchived Whether to include archived posts (Premium feature)
     * @return Flow of PagingData containing user's posts
     */
    fun getPostsByUser(
        userId: String,
        includeArchived: Boolean = false
    ): Flow<PagingData<Post>>

    /**
     * Gets a specific post by ID.
     * 
     * @param postId The post ID to fetch
     * @return Result containing the Post or an error
     */
    suspend fun getPostById(postId: String): Result<Post?>

    /**
     * Listen to a specific post in real-time with instant updates.
     * Uses addSnapshotListener for real-time updates when the post changes.
     * 
     * @param postId The post ID to listen to
     * @return Flow of Result containing the Post with real-time updates
     */
    fun listenToPost(postId: String): Flow<Result<Post?>>

    /**
     * Creates a new post.
     * 
     * @param images List of image URLs from Firebase Storage
     * @param description Post description
     * @param location Post location
     * @param city City name
     * @param categoriesEn Internal categories (English)
     * @param categoriesDe Display categories (German)
     * @return Result containing the created Post or an error
     */
    suspend fun createPost(
        images: List<String>,
        description: String,
        location: PostLocation,
        city: String,
        categoriesEn: List<String>,
        categoriesDe: List<String>
    ): Result<Post>

    /**
     * Updates an existing post (only owner can update).
     * 
     * @param postId The post ID to update
     * @param description New description
     * @param categoriesEn New internal categories
     * @param categoriesDe New display categories
     * @return Result indicating success or error
     */
    suspend fun updatePost(
        postId: String,
        description: String,
        categoriesEn: List<String>,
        categoriesDe: List<String>
    ): Result<Unit>

    /**
     * Deletes a post (only owner can delete).
     * 
     * @param postId The post ID to delete
     * @return Result indicating success or error
     */
    suspend fun deletePost(postId: String): Result<Unit>

    /**
     * Legacy toggleLike method for backward compatibility.
     * 
     * @param postId The post ID to like/unlike
     * @return Result containing the updated like status
     */
    suspend fun toggleLike(postId: String): Result<Boolean> {
        return toggleLike(postId, false) // Default to not liked for legacy calls
    }

    /**
     * Gets comments for a specific post with pagination.
     * 
     * @param postId The post ID to fetch comments for
     * @return Flow of PagingData containing comments
     */
    fun getComments(postId: String): Flow<PagingData<Comment>>

    /**
     * Gets users who liked a specific post with pagination.
     * 
     * @param postId The post ID to fetch likes for
     * @return Flow of PagingData containing users who liked the post
     */
    fun getPostLikes(postId: String): Flow<PagingData<com.omerkaya.sperrmuellfinder.domain.model.User>>

    /**
     * Adds a comment to a post.
     * 
     * @param postId The post ID to comment on
     * @param content Comment content
     * @return Result containing the created Comment or an error
     */
    suspend fun addComment(
        postId: String,
        content: String
    ): Result<Comment>

    /**
     * Deletes a comment (only author can delete).
     * 
     * @param commentId The comment ID to delete
     * @return Result indicating success or error
     */
    suspend fun deleteComment(commentId: String): Result<Unit>

    /**
     * Toggles like status for a comment.
     * 
     * @param commentId The comment ID to like/unlike
     * @return Result containing the updated like status
     */
    suspend fun toggleCommentLike(commentId: String): Result<Boolean>

    /**
     * Reports a post or comment.
     * 
     * @param targetType Type of target being reported (POST, COMMENT, USER)
     * @param targetId ID of the target being reported
     * @param reason Reason for the report
     * @param description Optional additional description
     * @return Result containing the created Report or an error
     */
    suspend fun reportContent(
        targetType: ReportTargetType,
        targetId: String,
        reason: ReportReason,
        description: String? = null
    ): Result<Report>

    /**
     * Extends post expiration time (Premium feature or paid extension).
     * 
     * @param postId The post ID to extend
     * @param hours Number of hours to extend (default: 6)
     * @return Result indicating success or error
     */
    suspend fun extendPostExpiration(
        postId: String,
        hours: Int = 6
    ): Result<Unit>

    /**
     * Searches posts with advanced filters (Premium feature).
     * 
     * @param query Search query for description
     * @param userLocation User location for distance calculation
     * @param radiusMeters Search radius in meters
     * @param categories Filter by categories (English)
     * @param city Filter by city
     * @param maxHoursOld Maximum age of posts in hours
     * @param sortBy Sort criteria
     * @return Flow of PagingData containing filtered posts
     */
    fun searchPosts(
        query: String? = null,
        userLocation: PostLocation,
        radiusMeters: Int = 1500,
        categories: List<String> = emptyList(),
        city: String? = null,
        maxHoursOld: Int = 72,
        sortBy: PostSortBy = PostSortBy.NEWEST
    ): Flow<PagingData<Post>>

    /**
     * Gets trending/popular posts in the area.
     * 
     * @param userLocation User location for area filtering
     * @param radiusMeters Search radius in meters
     * @return Flow of PagingData containing trending posts
     */
    fun getTrendingPosts(
        userLocation: PostLocation,
        radiusMeters: Int = 1500
    ): Flow<PagingData<Post>>

    /**
     * Refreshes the post feed (for pull-to-refresh).
     */
    suspend fun refreshFeed(): Result<Unit>

    /**
     * Increments view count for a post.
     * 
     * @param postId The post ID to increment views for
     */
    suspend fun incrementViewCount(postId: String): Result<Unit>
    
    /**
     * Increments share count for a post.
     * Tracks how many times a post has been shared for analytics.
     * 
     * @param postId The post ID to increment shares for
     */
    suspend fun incrementShareCount(postId: String): Result<Unit>

    /**
     * Uploads an image to Firebase Storage.
     * 
     * @param file The image file to upload
     * @return Result containing the uploaded image URL or an error
     */
    suspend fun uploadImage(file: File): Result<String>
}

/**
 * Post sorting options for search and feed
 */
enum class PostSortBy {
    NEWEST,           // created_at DESC
    OLDEST,           // created_at ASC
    NEAREST,          // distance ASC (client-side calculation)
    MOST_LIKED,       // likes_count DESC
    MOST_COMMENTED,   // comments_count DESC
    MOST_VIEWED,      // views_count DESC
    EXPIRING_SOON     // expires_at ASC
}