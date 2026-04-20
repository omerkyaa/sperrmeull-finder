package com.omerkaya.sperrmuellfinder.domain.usecase.post

import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.Post
import com.omerkaya.sperrmuellfinder.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Use case for getting post details with real-time updates.
 * Handles post fetching, distance calculation, and real-time data synchronization.
 */
class GetPostDetailUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val logger: Logger
) {

    /**
     * Gets post details with real-time updates from Firestore.
     * Uses addSnapshotListener for instant updates when the post changes.
     * 
     * @param postId The ID of the post to fetch
     * @return Flow of Result containing the Post with real-time updates
     */
    operator fun invoke(postId: String): Flow<Result<Post?>> {
        logger.d(Logger.TAG_DEFAULT, "Setting up real-time post detail listener for: $postId")
        
        return postRepository.listenToPost(postId)
            .catch { e ->
                logger.e(Logger.TAG_DEFAULT, "Flow error in GetPostDetailUseCase", e)
                emit(Result.Error(e))
            }
    }

    /**
     * Increments view count for the post.
     * 
     * @param postId The post ID to increment views for
     */
    suspend fun incrementViewCount(postId: String): Result<Unit> {
        logger.d(Logger.TAG_DEFAULT, "Incrementing view count for post: $postId")
        
        return try {
            val result = postRepository.incrementViewCount(postId)
            when (result) {
                is Result.Success -> {
                    logger.d(Logger.TAG_DEFAULT, "View count incremented for post: $postId")
                    result
                }
                is Result.Error -> {
                    logger.e(Logger.TAG_DEFAULT, "Failed to increment view count for post: $postId", result.exception)
                    result
                }
                is Result.Loading -> result
            }
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Error incrementing view count for post: $postId", e)
            Result.Error(e)
        }
    }

    /**
     * Increments share count for the post.
     * 
     * @param postId The post ID to increment shares for
     */
    suspend fun incrementShareCount(postId: String): Result<Unit> {
        logger.d(Logger.TAG_DEFAULT, "Incrementing share count for post: $postId")
        
        return try {
            val result = postRepository.incrementShareCount(postId)
            when (result) {
                is Result.Success -> {
                    logger.d(Logger.TAG_DEFAULT, "Share count incremented for post: $postId")
                    result
                }
                is Result.Error -> {
                    logger.e(Logger.TAG_DEFAULT, "Failed to increment share count for post: $postId", result.exception)
                    result
                }
                is Result.Loading -> result
            }
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Error incrementing share count for post: $postId", e)
            Result.Error(e)
        }
    }
}
