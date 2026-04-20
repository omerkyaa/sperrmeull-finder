package com.omerkaya.sperrmuellfinder.domain.usecase.social

import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.repository.SocialRepository
import javax.inject.Inject

/**
 * 🔍 SEARCH USERS USE CASE - SperrmüllFinder
 * Rules.md compliant - Clean Architecture domain layer
 * 
 * Features:
 * - Search users by name/username
 * - Get suggested users to follow
 * - Get user profiles
 * - Mutual followers detection
 */
class SearchUsersUseCase @Inject constructor(
    private val socialRepository: SocialRepository
) {
    
    companion object {
        private const val MIN_SEARCH_LENGTH = 2
        private const val MAX_SEARCH_LENGTH = 50
        private const val DEFAULT_SEARCH_LIMIT = 20
        private const val DEFAULT_SUGGESTIONS_LIMIT = 10
    }
    
    /**
     * Search users by display name or username.
     * Returns users matching the search query.
     * 
     * @param query Search query string
     * @param limit Maximum number of results
     * @return Result containing list of matching users
     */
    suspend fun searchUsers(query: String, limit: Int = DEFAULT_SEARCH_LIMIT): Result<List<User>> {
        // Validate input
        val trimmedQuery = query.trim()
        if (trimmedQuery.length < MIN_SEARCH_LENGTH) {
            return Result.Error(IllegalArgumentException("Search query is too short (min $MIN_SEARCH_LENGTH characters)"))
        }
        
        if (trimmedQuery.length > MAX_SEARCH_LENGTH) {
            return Result.Error(IllegalArgumentException("Search query is too long (max $MAX_SEARCH_LENGTH characters)"))
        }
        
        if (limit <= 0 || limit > 100) {
            return Result.Error(IllegalArgumentException("Invalid limit: must be between 1 and 100"))
        }
        
        return socialRepository.searchUsers(trimmedQuery, limit)
    }
    
    /**
     * Get suggested users to follow.
     * Returns users based on mutual follows, location, activity.
     * 
     * @param limit Maximum number of suggestions
     * @return Result containing list of suggested users
     */
    suspend fun getSuggestedUsers(limit: Int = DEFAULT_SUGGESTIONS_LIMIT): Result<List<User>> {
        if (limit <= 0 || limit > 50) {
            return Result.Error(IllegalArgumentException("Invalid limit: must be between 1 and 50"))
        }
        
        return socialRepository.getSuggestedUsers(limit)
    }
    
    /**
     * Get user profile by ID.
     * Returns complete user profile information.
     * 
     * @param userId The user ID to get profile for
     * @return Result containing user profile
     */
    suspend fun getUserProfile(userId: String): Result<User> {
        if (userId.isBlank()) {
            return Result.Error(IllegalArgumentException("User ID cannot be blank"))
        }
        
        return socialRepository.getUserProfile(userId)
    }
    
    /**
     * Get mutual followers between current user and target user.
     * Returns users that both users follow.
     * 
     * @param targetUserId The target user ID
     * @return Result containing list of mutual followers
     */
    suspend fun getMutualFollowers(targetUserId: String): Result<List<User>> {
        if (targetUserId.isBlank()) {
            return Result.Error(IllegalArgumentException("Target user ID cannot be blank"))
        }
        
        return socialRepository.getMutualFollowers(targetUserId)
    }
}
