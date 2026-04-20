package com.omerkaya.sperrmuellfinder.domain.usecase.feed

import com.omerkaya.sperrmuellfinder.domain.model.Post
import com.omerkaya.sperrmuellfinder.domain.repository.FeedRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 👁️ OBSERVE POST USE CASE - SperrmüllFinder
 * Rules.md compliant - Clean Architecture domain layer
 * 
 * Features:
 * - Real-time post updates
 * - Used for updating PostCard data when changes occur
 * - Handles post deletion/archiving gracefully
 */
class ObservePostUseCase @Inject constructor(
    private val feedRepository: FeedRepository
) {
    
    /**
     * Observe real-time updates for a specific post.
     * Returns null if post is deleted or archived.
     * 
     * @param postId The post ID to observe
     * @return Flow of Post updates (null if deleted/archived)
     */
    operator fun invoke(postId: String): Flow<Post?> {
        return feedRepository.observePost(postId)
    }
}
