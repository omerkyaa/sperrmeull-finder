package com.omerkaya.sperrmuellfinder.domain.usecase.social

import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.repository.SocialRepository
import javax.inject.Inject

/**
 * 👥 FOLLOW USER USE CASE - SperrmüllFinder
 * Rules.md compliant - Clean Architecture domain layer
 * 
 * Features:
 * - Follow/Unfollow user functionality
 * - Validation and business logic
 * - Notification creation
 * - Analytics tracking
 */
class FollowUserUseCase @Inject constructor(
    private val socialRepository: SocialRepository
) {
    
    /**
     * Follow a user.
     * Creates follow relationship and sends notification.
     * 
     * @param targetUserId The user ID to follow
     * @return Result indicating success or failure
     */
    suspend fun follow(targetUserId: String): Result<Unit> {
        // Validate input
        if (targetUserId.isBlank()) {
            return Result.Error(IllegalArgumentException("Target user ID cannot be blank"))
        }
        
        return socialRepository.followUser(targetUserId)
    }
    
    /**
     * Unfollow a user.
     * Removes follow relationship.
     * 
     * @param targetUserId The user ID to unfollow
     * @return Result indicating success or failure
     */
    suspend fun unfollow(targetUserId: String): Result<Unit> {
        // Validate input
        if (targetUserId.isBlank()) {
            return Result.Error(IllegalArgumentException("Target user ID cannot be blank"))
        }
        
        return socialRepository.unfollowUser(targetUserId)
    }
    
    /**
     * Toggle follow status for a user.
     * Follows if not following, unfollows if following.
     * 
     * @param targetUserId The user ID to toggle follow for
     * @return Result containing new follow status (true = now following)
     */
    suspend fun toggleFollow(targetUserId: String): Result<Boolean> {
        // Validate input
        if (targetUserId.isBlank()) {
            return Result.Error(IllegalArgumentException("Target user ID cannot be blank"))
        }
        
        return when (val isFollowingResult = socialRepository.isFollowing(targetUserId)) {
            is Result.Success -> {
                val isCurrentlyFollowing = isFollowingResult.data
                
                val toggleResult = if (isCurrentlyFollowing) {
                    socialRepository.unfollowUser(targetUserId)
                } else {
                    socialRepository.followUser(targetUserId)
                }
                
                when (toggleResult) {
                    is Result.Success -> Result.Success(!isCurrentlyFollowing)
                    is Result.Error -> Result.Error(toggleResult.exception)
                    is Result.Loading -> Result.Loading
                }
            }
            is Result.Error -> Result.Error(isFollowingResult.exception)
            is Result.Loading -> Result.Loading
        }
    }
    
    /**
     * Check if current user is following a specific user.
     * 
     * @param targetUserId The user ID to check
     * @return Result containing follow status
     */
    suspend fun isFollowing(targetUserId: String): Result<Boolean> {
        if (targetUserId.isBlank()) {
            return Result.Error(IllegalArgumentException("Target user ID cannot be blank"))
        }
        
        return socialRepository.isFollowing(targetUserId)
    }
}
