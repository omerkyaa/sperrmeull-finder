package com.omerkaya.sperrmuellfinder.domain.usecase

import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.repository.LikesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 💖 GET POST LIKE STATUS USE CASE - SperrmüllFinder
 * Rules.md compliant - Clean Architecture domain layer
 *
 * Real-time like status tracking for persistent heart color.
 */
class GetPostLikeStatusUseCase @Inject constructor(
    private val likesRepository: LikesRepository
) {

    /**
     * Get real-time like status for a post by a specific user.
     * Used for persistent heart color in PostCard and PostDetail.
     *
     * @param postId The post ID to check
     * @param userId The user ID to check
     * @return Flow of Result containing like status (true if liked, false if not)
     */
    operator fun invoke(postId: String, userId: String): Flow<Result<Boolean>> {
        require(postId.isNotBlank()) { "Post ID cannot be blank" }
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        return likesRepository.isPostLikedByUser(postId, userId)
    }
}
