package com.omerkaya.sperrmuellfinder.domain.usecase

import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.repository.LikesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 📊 GET POST LIKE COUNT USE CASE - SperrmüllFinder
 * Rules.md compliant - Clean Architecture domain layer
 *
 * Real-time like count tracking for live updates.
 */
class GetPostLikeCountUseCase @Inject constructor(
    private val likesRepository: LikesRepository
) {

    /**
     * Get real-time like count for a post.
     * Used for live like count updates in PostCard and PostDetail.
     *
     * @param postId The post ID to get count for
     * @return Flow of Result containing like count
     */
    operator fun invoke(postId: String): Flow<Result<Int>> {
        require(postId.isNotBlank()) { "Post ID cannot be blank" }
        return likesRepository.getPostLikesCount(postId)
    }
}
