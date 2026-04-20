package com.omerkaya.sperrmuellfinder.domain.usecase.post

import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.domain.model.Comment
import com.omerkaya.sperrmuellfinder.domain.repository.PostRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting comments for a post with pagination.
 * Handles comment fetching with real-time updates and pagination.
 */
class GetCommentsUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val logger: Logger
) {

    /**
     * Gets comments for a post with pagination and real-time updates.
     * 
     * @param postId The post ID to fetch comments for
     * @param scope Coroutine scope for caching
     * @return Flow of PagingData containing comments
     */
    operator fun invoke(
        postId: String,
        scope: CoroutineScope
    ): Flow<PagingData<Comment>> {
        logger.d(Logger.TAG_DEFAULT, "Getting comments for post: $postId")
        
        return postRepository.getComments(postId)
            .cachedIn(scope)
    }
}
