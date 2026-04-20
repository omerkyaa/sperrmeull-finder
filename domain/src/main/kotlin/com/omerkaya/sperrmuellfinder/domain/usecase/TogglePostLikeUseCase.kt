package com.omerkaya.sperrmuellfinder.domain.usecase

import com.omerkaya.sperrmuellfinder.domain.repository.LikesRepository
import com.omerkaya.sperrmuellfinder.core.util.Result
import javax.inject.Inject

/**
 * 💖 TOGGLE POST LIKE USE CASE - SperrmüllFinder
 * Handles like/unlike functionality with optimistic updates
 * Rules.md compliant - Clean Architecture use case
 */
class TogglePostLikeUseCase @Inject constructor(
    private val likesRepository: LikesRepository
) {
    
    /**
     * Toggle like status for a post (like if not liked, unlike if already liked)
     * 
     * @param postId The ID of the post to toggle like for
     * @param userId The ID of the user performing the action
     * @return Result<Boolean> - true if post is now liked, false if unliked
     */
    suspend operator fun invoke(postId: String, userId: String): Result<Boolean> {
        require(postId.isNotBlank()) { "Post ID cannot be blank" }
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        
        return likesRepository.togglePostLike(postId, userId)
    }
}
