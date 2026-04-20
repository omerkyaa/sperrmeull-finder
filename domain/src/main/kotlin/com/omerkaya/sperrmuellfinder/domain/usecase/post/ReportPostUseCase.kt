package com.omerkaya.sperrmuellfinder.domain.usecase.post

import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.Report
import com.omerkaya.sperrmuellfinder.domain.model.ReportReason
import com.omerkaya.sperrmuellfinder.domain.model.ReportTargetType
import com.omerkaya.sperrmuellfinder.domain.repository.PostRepository
import javax.inject.Inject

/**
 * Use case for reporting posts and comments.
 * Handles content reporting with proper validation and logging.
 */
class ReportPostUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val logger: Logger
) {

    /**
     * Reports a post for inappropriate content or other violations.
     * 
     * @param postId The post ID to report
     * @param reason The reason for reporting
     * @param description Optional additional description
     * @return Result containing the created Report or an error
     */
    suspend fun reportPost(
        postId: String,
        reason: ReportReason,
        description: String? = null
    ): Result<Report> {
        logger.d(Logger.TAG_DEFAULT, "Reporting post: $postId for reason: $reason")
        
        return try {
            // Validate inputs
            if (postId.isBlank()) {
                return Result.Error(IllegalArgumentException("Post ID cannot be empty"))
            }
            
            val result = postRepository.reportContent(
                targetType = ReportTargetType.POST,
                targetId = postId,
                reason = reason,
                description = description
            )
            
            when (result) {
                is Result.Success -> {
                    logger.i(Logger.TAG_DEFAULT, "Post $postId reported successfully with ID: ${result.data.id}")
                    
                    // TODO: Analytics event
                    // Analytics.track("post_reported", mapOf(
                    //     "post_id" to postId,
                    //     "reason" to reason.name,
                    //     "has_description" to (description != null)
                    // ))
                    
                    result
                }
                is Result.Error -> {
                    logger.e(Logger.TAG_DEFAULT, "Failed to report post $postId", result.exception)
                    result
                }
                is Result.Loading -> {
                    logger.d(Logger.TAG_DEFAULT, "Report submission in progress for post $postId")
                    result
                }
            }
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Error reporting post $postId", e)
            Result.Error(e)
        }
    }

    /**
     * Reports a comment for inappropriate content or other violations.
     * 
     * @param commentId The comment ID to report
     * @param reason The reason for reporting
     * @param description Optional additional description
     * @return Result containing the created Report or an error
     */
    suspend fun reportComment(
        commentId: String,
        reason: ReportReason,
        description: String? = null
    ): Result<Report> {
        logger.d(Logger.TAG_DEFAULT, "Reporting comment: $commentId for reason: $reason")
        
        return try {
            // Validate inputs
            if (commentId.isBlank()) {
                return Result.Error(IllegalArgumentException("Comment ID cannot be empty"))
            }
            
            val result = postRepository.reportContent(
                targetType = ReportTargetType.COMMENT,
                targetId = commentId,
                reason = reason,
                description = description
            )
            
            when (result) {
                is Result.Success -> {
                    logger.i(Logger.TAG_DEFAULT, "Comment $commentId reported successfully with ID: ${result.data.id}")
                    
                    // TODO: Analytics event
                    // Analytics.track("comment_reported", mapOf(
                    //     "comment_id" to commentId,
                    //     "reason" to reason.name,
                    //     "has_description" to (description != null)
                    // ))
                    
                    result
                }
                is Result.Error -> {
                    logger.e(Logger.TAG_DEFAULT, "Failed to report comment $commentId", result.exception)
                    result
                }
                is Result.Loading -> {
                    logger.d(Logger.TAG_DEFAULT, "Report submission in progress for comment $commentId")
                    result
                }
            }
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Error reporting comment $commentId", e)
            Result.Error(e)
        }
    }
}
