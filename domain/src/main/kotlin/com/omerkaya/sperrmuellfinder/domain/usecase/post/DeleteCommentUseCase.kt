package com.omerkaya.sperrmuellfinder.domain.usecase.post

import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.repository.PostRepository
import javax.inject.Inject

/**
 * Use case for deleting comments.
 * Ensures input validation and centralized logging.
 */
class DeleteCommentUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val logger: Logger
) {

    suspend operator fun invoke(commentId: String): Result<Unit> {
        if (commentId.isBlank()) {
            return Result.Error(IllegalArgumentException("Comment ID cannot be empty"))
        }

        return try {
            logger.d(Logger.TAG_DEFAULT, "Deleting comment: $commentId")
            val result = postRepository.deleteComment(commentId)
            when (result) {
                is Result.Success -> logger.i(Logger.TAG_DEFAULT, "Comment deleted: $commentId")
                is Result.Error -> logger.e(Logger.TAG_DEFAULT, "Failed to delete comment: $commentId", result.exception)
                is Result.Loading -> logger.d(Logger.TAG_DEFAULT, "Deleting comment in progress: $commentId")
            }
            result
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Error deleting comment: $commentId", e)
            Result.Error(e)
        }
    }
}
