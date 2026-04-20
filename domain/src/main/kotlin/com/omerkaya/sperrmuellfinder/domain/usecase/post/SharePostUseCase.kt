package com.omerkaya.sperrmuellfinder.domain.usecase.post

import android.content.Context
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.core.util.ShareUtils
import com.omerkaya.sperrmuellfinder.domain.model.Post
import com.omerkaya.sperrmuellfinder.domain.repository.PostRepository
import javax.inject.Inject

/**
 * Use case for sharing posts.
 * Handles post sharing functionality and analytics tracking.
 */
class SharePostUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val logger: Logger
) {

    /**
     * Shares a post using the Android share dialog.
     * Also increments the share count for analytics.
     * 
     * @param context Android context for launching share intent
     * @param post The post to share
     * @param appStoreLink Link to the app in Play Store
     * @param shareTitle Title for the share chooser
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(
        context: Context,
        post: Post,
        appStoreLink: String,
        shareTitle: String
    ): Result<Unit> {
        logger.d(Logger.TAG_DEFAULT, "Sharing post: ${post.id}")
        
        return try {
            // Launch share dialog
            ShareUtils.sharePost(
                context = context,
                postId = post.id,
                postDescription = post.description,
                postCity = post.city,
                appStoreLink = appStoreLink,
                shareTitle = shareTitle
            )
            
            // Increment share count for analytics
            incrementShareCount(post.id)
            
            logger.i(Logger.TAG_DEFAULT, "Post shared successfully: ${post.id}")
            Result.Success(Unit)
            
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Failed to share post: ${post.id}", e)
            Result.Error(e)
        }
    }

    /**
     * Increments share count for a post.
     * This is used for analytics and tracking post engagement.
     * 
     * @param postId The post ID to increment shares for
     * @return Result indicating success or failure
     */
    private suspend fun incrementShareCount(postId: String): Result<Unit> {
        return try {
            val result = postRepository.incrementShareCount(postId)
            when (result) {
                is Result.Success -> {
                    logger.d(Logger.TAG_DEFAULT, "Share count incremented for post: $postId")
                    result
                }
                is Result.Error -> {
                    logger.e(Logger.TAG_DEFAULT, "Failed to increment share count for post: $postId", result.exception)
                    // Don't fail the share operation if analytics fails
                    Result.Success(Unit)
                }
                is Result.Loading -> result
            }
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Error incrementing share count for post: $postId", e)
            // Don't fail the share operation if analytics fails
            Result.Success(Unit)
        }
    }

    /**
     * Gets formatted share text for a post.
     * Can be used to preview share content before sharing.
     * 
     * @param post The post to format
     * @param platform Target platform for formatting
     * @return Formatted share text
     */
    fun getShareText(
        post: Post, 
        platform: com.omerkaya.sperrmuellfinder.core.util.SharePlatform = com.omerkaya.sperrmuellfinder.core.util.SharePlatform.GENERIC
    ): String {
        return ShareUtils.formatShareText(
            postId = post.id,
            postDescription = post.description,
            postCity = post.city,
            platform = platform
        )
    }
}
