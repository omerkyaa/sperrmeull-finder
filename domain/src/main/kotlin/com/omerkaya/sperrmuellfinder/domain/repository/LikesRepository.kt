package com.omerkaya.sperrmuellfinder.domain.repository

import com.omerkaya.sperrmuellfinder.domain.model.Like
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.core.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * 💖 LIKES REPOSITORY INTERFACE - SperrmüllFinder
 * Real-time Firebase likes with user data fetching
 * Rules.md compliant - Clean Architecture domain interface
 */
interface LikesRepository {
    
    /**
     * Get real-time stream of users who liked a specific post
     * Returns Flow of users with profile data for likes list
     */
    fun getPostLikesUsers(postId: String): Flow<Result<List<User>>>
    
    /**
     * Get real-time count of likes for a post
     */
    fun getPostLikesCount(postId: String): Flow<Result<Int>>
    
    /**
     * Check if current user has liked a specific post
     */
    fun isPostLikedByUser(postId: String, userId: String): Flow<Result<Boolean>>
    
    /**
     * Toggle like status for a post (like/unlike)
     */
    suspend fun togglePostLike(postId: String, userId: String): Result<Boolean>
    
    /**
     * Get all likes for a specific post (admin/debug purposes)
     */
    fun getPostLikes(postId: String): Flow<Result<List<Like>>>
}