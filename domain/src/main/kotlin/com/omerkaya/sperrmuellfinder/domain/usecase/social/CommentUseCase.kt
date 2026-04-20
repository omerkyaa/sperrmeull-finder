package com.omerkaya.sperrmuellfinder.domain.usecase.social

import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.Comment
import com.omerkaya.sperrmuellfinder.domain.repository.SocialRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 💬 COMMENT USE CASE - SperrmüllFinder
 * Rules.md compliant - Clean Architecture domain layer
 * 
 * Features:
 * - Add comments to posts
 * - Get comments with real-time updates
 * - Delete comments (author/post owner only)
 * - Comment validation and business logic
 */
class CommentUseCase @Inject constructor(
    private val socialRepository: SocialRepository
) {
    
    companion object {
        private const val MIN_COMMENT_LENGTH = 1
        private const val MAX_COMMENT_LENGTH = 500
    }
    
    /**
     * Add comment to a post.
     * Validates comment content and creates comment document.
     * 
     * @param postId The post ID to comment on
     * @param text Comment text content
     * @return Result containing the created comment ID
     */
    suspend fun addComment(postId: String, text: String): Result<String> {
        // Validate input
        if (postId.isBlank()) {
            return Result.Error(IllegalArgumentException("Post ID cannot be blank"))
        }
        
        val trimmedText = text.trim()
        if (trimmedText.length < MIN_COMMENT_LENGTH) {
            return Result.Error(IllegalArgumentException("Comment is too short"))
        }
        
        if (trimmedText.length > MAX_COMMENT_LENGTH) {
            return Result.Error(IllegalArgumentException("Comment is too long (max $MAX_COMMENT_LENGTH characters)"))
        }
        
        // Check for inappropriate content (basic validation)
        if (containsInappropriateContent(trimmedText)) {
            return Result.Error(IllegalArgumentException("Comment contains inappropriate content"))
        }
        
        return socialRepository.addComment(postId, trimmedText)
    }
    
    /**
     * Get comments for a post with real-time updates.
     * Returns comments ordered by creation date (newest first).
     * 
     * @param postId The post ID to get comments for
     * @return Flow of List containing comments
     */
    fun getComments(postId: String): Flow<List<Comment>> {
        return socialRepository.getComments(postId)
    }
    
    /**
     * Delete a comment.
     * Only comment author or post owner can delete.
     * 
     * @param commentId The comment ID to delete
     * @param postId The post ID the comment belongs to
     * @return Result indicating success or failure
     */
    suspend fun deleteComment(commentId: String, postId: String): Result<Unit> {
        // Validate input
        if (commentId.isBlank()) {
            return Result.Error(IllegalArgumentException("Comment ID cannot be blank"))
        }
        
        if (postId.isBlank()) {
            return Result.Error(IllegalArgumentException("Post ID cannot be blank"))
        }
        
        return socialRepository.deleteComment(commentId, postId)
    }
    
    /**
     * Get comment count for a post.
     * 
     * @param postId The post ID to get comment count for
     * @return Flow of comment count
     */
    fun getCommentCount(postId: String): Flow<Int> {
        return socialRepository.getCommentCount(postId)
    }
    
    /**
     * Basic inappropriate content detection.
     * In production, this would use ML Kit or external service.
     */
    private fun containsInappropriateContent(text: String): Boolean {
        val inappropriateWords = listOf<String>(
            // Add inappropriate words here
            // This is a basic implementation
            "spam", "test" // Example words - replace with actual inappropriate words
        )
        
        val lowerText = text.lowercase()
        return inappropriateWords.any { word: String ->
            lowerText.contains(other = word, ignoreCase = false)
        }
    }
}
