package com.omerkaya.sperrmuellfinder.domain.usecase.post

import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.repository.PostRepository
import javax.inject.Inject

/**
 * Use case for toggling like status on posts.
 * Handles like/unlike operations with proper error handling and logging.
 */
class TogglePostLikeUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val logger: Logger
) {

    /**
     * Toggles like status for a post.
     * 
     * @param postId The post ID to like/unlike
     * @return Result containing the new like status (true if liked, false if unliked)
     */
    suspend operator fun invoke(postId: String): Result<Boolean> {
        logger.d(Logger.TAG_DEFAULT, "Toggling like for post: $postId")
        
        return try {
            val result = postRepository.toggleLike(postId)
            when (result) {
                is Result.Success -> {
                    val isLiked = result.data
                    logger.i(Logger.TAG_DEFAULT, "Post $postId ${if (isLiked) "liked" else "unliked"} successfully")
                    
                    // TODO: Analytics event
                    // Analytics.track("post_${if (isLiked) "liked" else "unliked"}", mapOf("post_id" to postId))
                    
                    result
                }
                is Result.Error -> {
                    logger.e(Logger.TAG_DEFAULT, "Failed to toggle like for post $postId", result.exception)
                    result
                }
                is Result.Loading -> {
                    logger.d(Logger.TAG_DEFAULT, "Toggle like in progress for post $postId")
                    result
                }
            }
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Error toggling like for post $postId", e)
            Result.Error(e)
        }
    }
}
