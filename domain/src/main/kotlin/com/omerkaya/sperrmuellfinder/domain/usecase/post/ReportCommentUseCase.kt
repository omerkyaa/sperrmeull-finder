package com.omerkaya.sperrmuellfinder.domain.usecase.post

import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.Report
import com.omerkaya.sperrmuellfinder.domain.model.ReportReason
import com.omerkaya.sperrmuellfinder.domain.model.ReportTargetType
import com.omerkaya.sperrmuellfinder.domain.repository.PostRepository
import javax.inject.Inject

/**
 * Use case for reporting comments.
 * Handles comment reporting with validation and error handling.
 * Rules.md compliant - Professional comment moderation system.
 */
class ReportCommentUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val logger: Logger
) {

    /**
     * Reports a comment for inappropriate content.
     * 
     * @param commentId The comment ID to report
     * @param reason The reason for reporting
     * @param description Optional additional description
     * @return Result containing the created Report or an error
     */
    suspend operator fun invoke(
        commentId: String,
        reason: ReportReason,
        description: String? = null
    ): Result<Report> {
        logger.d(Logger.TAG_DEFAULT, "Reporting comment: $commentId for reason: ${reason.displayName}")
        
        return try {
            // Validate input
            if (commentId.isBlank()) {
                return Result.Error(IllegalArgumentException("Comment ID cannot be empty"))
            }
            
            val result = postRepository.reportContent(
                targetType = ReportTargetType.COMMENT,
                targetId = commentId,
                reason = reason,
                description = description?.trim()
            )
            
            when (result) {
                is Result.Success -> {
                    logger.i(Logger.TAG_DEFAULT, "Comment reported successfully: $commentId")
                    
                    // TODO: Analytics event
                    // Analytics.track("comment_reported", mapOf(
                    //     "comment_id" to commentId,
                    //     "reason" to reason.name,
                    //     "has_description" to (description != null)
                    // ))
                    
                    result
                }
                is Result.Error -> {
                    logger.e(Logger.TAG_DEFAULT, "Failed to report comment: $commentId", result.exception)
                    result
                }
                is Result.Loading -> {
                    logger.d(Logger.TAG_DEFAULT, "Reporting comment in progress: $commentId")
                    result
                }
            }
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Error reporting comment: $commentId", e)
            Result.Error(e)
        }
    }
}
