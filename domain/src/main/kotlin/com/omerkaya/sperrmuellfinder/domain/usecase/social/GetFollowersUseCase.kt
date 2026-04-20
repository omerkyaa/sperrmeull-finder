package com.omerkaya.sperrmuellfinder.domain.usecase.social

import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.repository.SocialRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 📋 GET FOLLOWERS USE CASE - SperrmüllFinder
 * Rules.md compliant - Clean Architecture domain layer
 * 
 * Features:
 * - Get followers list with real-time updates
 * - Get following list with real-time updates
 * - Get follower/following counts
 * - Mutual followers detection
 */
class GetFollowersUseCase @Inject constructor(
    private val socialRepository: SocialRepository
) {
    
    /**
     * Get followers list for a user.
     * Returns users who follow the specified user.
     * 
     * @param userId The user ID to get followers for
     * @return Flow of List containing follower users
     */
    fun getFollowers(userId: String): Flow<List<User>> {
        return socialRepository.getFollowers(userId)
    }
    
    /**
     * Get following list for a user.
     * Returns users that the specified user follows.
     * 
     * @param userId The user ID to get following for
     * @return Flow of List containing following users
     */
    fun getFollowing(userId: String): Flow<List<User>> {
        return socialRepository.getFollowing(userId)
    }
    
    /**
     * Get follower count for a user.
     * 
     * @param userId The user ID to get follower count for
     * @return Flow of follower count
     */
    fun getFollowerCount(userId: String): Flow<Int> {
        return socialRepository.getFollowerCount(userId)
    }
    
    /**
     * Get following count for a user.
     * 
     * @param userId The user ID to get following count for
     * @return Flow of following count
     */
    fun getFollowingCount(userId: String): Flow<Int> {
        return socialRepository.getFollowingCount(userId)
    }
}
