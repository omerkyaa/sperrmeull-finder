package com.omerkaya.sperrmuellfinder.domain.usecase.post

import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.Comment
import com.omerkaya.sperrmuellfinder.domain.repository.PostRepository
import javax.inject.Inject

/**
 * Use case for adding comments to posts.
 * Handles comment creation with validation and error handling.
 */
class AddCommentUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val logger: Logger
) {

    /**
     * Adds a comment to a post.
     * 
     * @param postId The post ID to comment on
     * @param content The comment content
     * @return Result containing the created Comment or an error
     */
    suspend operator fun invoke(
        postId: String,
        content: String
    ): Result<Comment> {
        logger.d(Logger.TAG_DEFAULT, "Adding comment to post: $postId")
        
        return try {
            // Validate input
            if (postId.isBlank()) {
                return Result.Error(IllegalArgumentException("Post ID cannot be empty"))
            }
            
            if (content.isBlank()) {
                return Result.Error(IllegalArgumentException("Comment content cannot be empty"))
            }
            
            if (content.length > MAX_COMMENT_LENGTH) {
                return Result.Error(IllegalArgumentException("Comment is too long (max $MAX_COMMENT_LENGTH characters)"))
            }
            
            val result = postRepository.addComment(postId, content.trim())
            
            when (result) {
                is Result.Success -> {
                    logger.i(Logger.TAG_DEFAULT, "Comment added successfully to post: $postId")
                    
                    // TODO: Analytics event
                    // Analytics.track("comment_added", mapOf(
                    //     "post_id" to postId,
                    //     "comment_length" to content.length
                    // ))
                    
                    result
                }
                is Result.Error -> {
                    logger.e(Logger.TAG_DEFAULT, "Failed to add comment to post: $postId", result.exception)
                    result
                }
                is Result.Loading -> {
                    logger.d(Logger.TAG_DEFAULT, "Adding comment in progress for post: $postId")
                    result
                }
            }
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Error adding comment to post: $postId", e)
            Result.Error(e)
        }
    }

    companion object {
        private const val MAX_COMMENT_LENGTH = 500
    }
}
