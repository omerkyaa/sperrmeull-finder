package com.omerkaya.sperrmuellfinder.domain.usecase.post

import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.repository.PostRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject

/**
 * Use case for getting users who liked a post.
 * Handles fetching post likes with pagination.
 */
class GetPostLikesUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val logger: Logger
) {

    /**
     * Gets users who liked a post with pagination.
     * 
     * @param postId The post ID to fetch likes for
     * @param scope Coroutine scope for caching
     * @return Flow of PagingData containing users who liked the post
     */
    operator fun invoke(
        postId: String,
        scope: CoroutineScope
    ): Flow<PagingData<User>> {
        logger.d(Logger.TAG_DEFAULT, "Getting likes for post: $postId")
        
        return try {
            postRepository.getPostLikes(postId).cachedIn(scope)
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Error getting likes for post: $postId", e)
            emptyFlow()
        }
    }
}
