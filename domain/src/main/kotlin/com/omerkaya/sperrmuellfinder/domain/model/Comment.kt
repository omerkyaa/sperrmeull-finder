package com.omerkaya.sperrmuellfinder.domain.model

import java.util.Date

/**
 * Comment entity for posts.
 * Represents user comments on posts with interaction data.
 * Rules.md compliant - Professional comment system.
 */
data class Comment(
    val id: String,
    val postId: String,
    val authorId: String,
    val authorName: String,
    val authorPhotoUrl: String? = null,
    val authorLevel: Int = 0,
    val content: String,
    val likesCount: Int = 0,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val isLikedByCurrentUser: Boolean = false,
    val authorCity: String? = null
) {
    
    /**
     * Check if comment can be edited (within 5 minutes of creation)
     */
    fun canBeEdited(): Boolean {
        val fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000)
        return createdAt.time > fiveMinutesAgo
    }
    
    /**
     * Get formatted time since creation
     * TODO: Use string resources for localization
     */
    fun getFormattedTime(): String {
        val now = System.currentTimeMillis()
        val diff = now - createdAt.time
        val minutes = diff / (1000 * 60)
        val hours = diff / (1000 * 60 * 60)
        val days = diff / (1000 * 60 * 60 * 24)
        
        return when {
            minutes < 1 -> "Jetzt"
            minutes < 60 -> "${minutes}m"
            hours < 24 -> "${hours}h"
            days < 7 -> "${days}d"
            else -> "${days / 7}w"
        }
    }
    
    /**
     * Check if comment is by the current user
     */
    fun isOwnComment(currentUserId: String): Boolean {
        return authorId == currentUserId
    }
}