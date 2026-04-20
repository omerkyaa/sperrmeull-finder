package com.omerkaya.sperrmuellfinder.domain.usecase.admin

import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.repository.AdminRepository
import javax.inject.Inject

/**
 * Use case for deleting posts and comments
 * Rules.md compliant - Clean Architecture domain layer
 */
class DeleteContentUseCase @Inject constructor(
    private val adminRepository: AdminRepository,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "DeleteContentUseCase"
    }
    
    /**
     * Delete a post
     * @param postId Post to delete
     * @param adminId Admin performing the deletion
     * @param reason Reason for deletion
     * @return Result of the operation
     */
    suspend fun deletePost(
        postId: String,
        adminId: String,
        reason: String
    ): Result<Unit> {
        logger.d(TAG, "Deleting post $postId")
        
        if (reason.length < 10) {
            return Result.Error(Exception("Deletion reason must be at least 10 characters"))
        }
        
        return adminRepository.deletePost(postId, adminId, reason)
    }
    
    /**
     * Delete a comment
     * @param commentId Comment to delete
     * @param adminId Admin performing the deletion
     * @param reason Reason for deletion
     * @return Result of the operation
     */
    suspend fun deleteComment(
        commentId: String,
        adminId: String,
        reason: String
    ): Result<Unit> {
        logger.d(TAG, "Deleting comment $commentId")
        
        if (reason.length < 10) {
            return Result.Error(Exception("Deletion reason must be at least 10 characters"))
        }
        
        return adminRepository.deleteComment(commentId, adminId, reason)
    }
}
