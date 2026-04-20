package com.omerkaya.sperrmuellfinder.domain.usecase.feed

import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.repository.FeedRepository
import javax.inject.Inject

/**
 * ❤️ TOGGLE LIKE USE CASE - SperrmüllFinder
 * Rules.md compliant - Clean Architecture domain layer
 * 
 * Features:
 * - Atomic like/unlike operations
 * - Notification creation for post owners
 * - Optimistic UI support
 * - Professional error handling
 */
class ToggleLikeUseCase @Inject constructor(
    private val feedRepository: FeedRepository
) {
    
    /**
     * Toggle like status for a post.
     * Creates notification for post owner if liked (not if unliked).
     * 
     * @param postId The post ID to like/unlike
     * @param userId The user ID performing the action
     * @return Result containing the new like status (true = liked, false = unliked)
     */
    suspend operator fun invoke(postId: String, userId: String): Result<Boolean> {
        return feedRepository.toggleLike(postId, userId)
    }
}
