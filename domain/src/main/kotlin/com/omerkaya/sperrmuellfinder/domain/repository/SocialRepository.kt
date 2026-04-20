package com.omerkaya.sperrmuellfinder.domain.repository

import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.Comment
import com.omerkaya.sperrmuellfinder.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * 🏆 SOCIAL REPOSITORY INTERFACE - SperrmüllFinder
 * Rules.md compliant - Clean Architecture domain layer
 * 
 * Features:
 * - Follow/Unfollow system
 * - Comments system with real-time updates
 * - User search and discovery
 * - Social interactions tracking
 * - Professional error handling
 */
interface SocialRepository {
    
    // ========================================
    // FOLLOW SYSTEM
    // ========================================
    
    /**
     * Follow a user.
     * Creates follow relationship and sends notification.
     * 
     * @param targetUserId The user ID to follow
     * @return Result indicating success or failure
     */
    suspend fun followUser(targetUserId: String): Result<Unit>
    
    /**
     * Unfollow a user.
     * Removes follow relationship.
     * 
     * @param targetUserId The user ID to unfollow
     * @return Result indicating success or failure
     */
    suspend fun unfollowUser(targetUserId: String): Result<Unit>
    
    /**
     * Check if current user is following a specific user.
     * 
     * @param targetUserId The user ID to check
     * @return Result containing follow status
     */
    suspend fun isFollowing(targetUserId: String): Result<Boolean>
    
    /**
     * Get followers list for a user.
     * Returns users who follow the specified user.
     * 
     * @param userId The user ID to get followers for
     * @return Flow of List containing follower users
     */
    fun getFollowers(userId: String): Flow<List<User>>
    
    /**
     * Get following list for a user.
     * Returns users that the specified user follows.
     * 
     * @param userId The user ID to get following for
     * @return Flow of List containing following users
     */
    fun getFollowing(userId: String): Flow<List<User>>
    
    /**
     * Get follower count for a user.
     * 
     * @param userId The user ID to get follower count for
     * @return Flow of follower count
     */
    fun getFollowerCount(userId: String): Flow<Int>
    
    /**
     * Get following count for a user.
     * 
     * @param userId The user ID to get following count for
     * @return Flow of following count
     */
    fun getFollowingCount(userId: String): Flow<Int>
    
    // ========================================
    // COMMENTS SYSTEM
    // ========================================
    
    /**
     * Add comment to a post.
     * Creates comment document and sends notification to post owner.
     * 
     * @param postId The post ID to comment on
     * @param text Comment text content
     * @return Result containing the created comment ID
     */
    suspend fun addComment(postId: String, text: String): Result<String>
    
    /**
     * Get comments for a post with real-time updates.
     * Returns comments ordered by creation date (newest first).
     * 
     * @param postId The post ID to get comments for
     * @return Flow of List containing comments
     */
    fun getComments(postId: String): Flow<List<Comment>>
    
    /**
     * Delete a comment.
     * Only comment author or post owner can delete.
     * 
     * @param commentId The comment ID to delete
     * @param postId The post ID the comment belongs to
     * @return Result indicating success or failure
     */
    suspend fun deleteComment(commentId: String, postId: String): Result<Unit>
    
    /**
     * Get comment count for a post.
     * 
     * @param postId The post ID to get comment count for
     * @return Flow of comment count
     */
    fun getCommentCount(postId: String): Flow<Int>
    
    // ========================================
    // USER SEARCH & DISCOVERY
    // ========================================
    
    /**
     * Search users by display name or username.
     * Returns users matching the search query.
     * 
     * @param query Search query string
     * @param limit Maximum number of results (default 20)
     * @return Result containing list of matching users
     */
    suspend fun searchUsers(query: String, limit: Int = 20): Result<List<User>>
    
    /**
     * Get suggested users to follow.
     * Returns users based on mutual follows, location, activity.
     * 
     * @param limit Maximum number of suggestions (default 10)
     * @return Result containing list of suggested users
     */
    suspend fun getSuggestedUsers(limit: Int = 10): Result<List<User>>
    
    /**
     * Get user profile by ID.
     * Returns complete user profile information.
     * 
     * @param userId The user ID to get profile for
     * @return Result containing user profile
     */
    suspend fun getUserProfile(userId: String): Result<User>
    
    /**
     * Get mutual followers between current user and target user.
     * Returns users that both users follow.
     * 
     * @param targetUserId The target user ID
     * @return Result containing list of mutual followers
     */
    suspend fun getMutualFollowers(targetUserId: String): Result<List<User>>
    
    // ========================================
    // BLOCK SYSTEM
    // ========================================
    
    /**
     * Block a user.
     * Automatically unfollows both ways.
     * 
     * @param userId The user ID to block
     * @param reason Optional reason for blocking
     * @return Result indicating success or failure
     */
    suspend fun blockUser(userId: String, reason: String?): Result<Unit>
    
    /**
     * Unblock a user.
     * 
     * @param userId The user ID to unblock
     * @return Result indicating success or failure
     */
    suspend fun unblockUser(userId: String): Result<Unit>
    
    /**
     * Check if current user has blocked a specific user.
     * 
     * @param userId The user ID to check
     * @return Result containing block status
     */
    suspend fun isBlocked(userId: String): Result<Boolean>
    
    /**
     * Check if current user is blocked by a specific user.
     * 
     * @param userId The user ID to check
     * @return Result containing blocked-by status
     */
    suspend fun isBlockedBy(userId: String): Result<Boolean>
    
    /**
     * Get list of blocked users.
     * Returns users that current user has blocked.
     * 
     * @return Flow of List containing blocked users
     */
    fun getBlockedUsers(): Flow<List<com.omerkaya.sperrmuellfinder.domain.model.BlockedUser>>
}
