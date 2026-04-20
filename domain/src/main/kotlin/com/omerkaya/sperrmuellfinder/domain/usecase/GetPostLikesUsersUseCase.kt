package com.omerkaya.sperrmuellfinder.domain.usecase

import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.repository.LikesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 💖 GET POST LIKES USERS USE CASE - SperrmüllFinder
 * Rules.md compliant - Clean Architecture domain layer
 * 
 * Fetches users who liked a specific post with real-time updates
 */
class GetPostLikesUsersUseCase @Inject constructor(
    private val likesRepository: LikesRepository,
    private val logger: Logger
) {

    /**
     * Gets users who liked a specific post with real-time updates.
     * 
     * @param postId The post ID to get likes for
     * @return Flow of Result containing users who liked the post (newest first)
     */
    operator fun invoke(postId: String): Flow<Result<List<User>>> {
        logger.d(TAG, "💖 Getting likes users for post: $postId")
        
        return try {
            likesRepository.getPostLikesUsers(postId)
        } catch (e: Exception) {
            logger.e(TAG, "❌ Error getting likes users for post: $postId", e)
            throw e
        }
    }

    companion object {
        private const val TAG = "GetPostLikesUsersUseCase"
    }
}