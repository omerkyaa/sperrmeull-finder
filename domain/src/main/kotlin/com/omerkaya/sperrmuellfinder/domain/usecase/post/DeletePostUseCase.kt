package com.omerkaya.sperrmuellfinder.domain.usecase.post

import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.repository.PostRepository
import javax.inject.Inject

/**
 * Use case for deleting own posts
 * Rules.md compliant - Clean Architecture domain layer
 */
class DeletePostUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "DeletePostUseCase"
    }
    
    /**
     * Delete a post (only owner can delete)
     * @param postId Post to delete
     * @return Result of the operation
     */
    suspend operator fun invoke(postId: String): Result<Unit> {
        logger.d(TAG, "Deleting post $postId")
        
        // Repository will check if current user is the owner
        return postRepository.deletePost(postId)
    }
}
