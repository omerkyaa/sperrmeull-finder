package com.omerkaya.sperrmuellfinder.domain.model

import com.omerkaya.sperrmuellfinder.core.model.PostLocation
import java.util.Date

/**
 * Map-specific representation of a post optimized for map display and clustering.
 * Contains only essential data needed for map rendering and interactions.
 */
data class PostMapItem(
    val id: String,
    val userId: String,
    val location: PostLocation,
    val imageUrl: String, // Primary image for map display
    val categoryEn: String,
    val categoryDe: String,
    val isPremiumOwner: Boolean,
    val ownerLevel: Int,
    val ownerFrameType: PremiumFrameType,
    val createdAt: Date, // Changed from LocalDateTime to Date for Android compatibility
    val availabilityPercent: Int,
    val isArchived: Boolean,
    val distanceFromUser: Double = 0.0, // Calculated distance in meters
    val city: String,
    val description: String, // Truncated for map display
    val likesCount: Int = 0,
    val commentsCount: Int = 0
) {
    /**
     * Check if post is still active (not expired)
     */
    val isActive: Boolean
        get() {
            val now = System.currentTimeMillis()
            val seventyTwoHoursAgo = now - (72 * 60 * 60 * 1000) // 72 hours in milliseconds
            return !isArchived && createdAt.time > seventyTwoHoursAgo
        }
    
    /**
     * Get display title for map marker
     */
    val displayTitle: String
        get() = description.take(50) + if (description.length > 50) "…" else ""
    
    /**
     * Get display snippet for map marker
     */
    val displaySnippet: String
        get() = "$city • $categoryDe"
    
    /**
     * Check if post is available (not taken)
     */
    val isAvailable: Boolean
        get() = availabilityPercent > 50
    
    /**
     * Get formatted distance for display
     */
    fun getFormattedDistance(): String {
        return when {
            distanceFromUser < 1000 -> "${distanceFromUser.toInt()} m"
            else -> String.format("%.1f km", distanceFromUser / 1000)
        }
    }
    
    /**
     * Get time since posted for display
     */
    fun getTimeSincePosted(): String {
        val now = System.currentTimeMillis()
        val postTime = createdAt.time
        val diffMillis = now - postTime
        val hours = diffMillis / (1000 * 60 * 60)
        
        return when {
            hours < 1 -> "Gerade eben"
            hours < 24 -> "${hours}h"
            else -> "${hours / 24}d"
        }
    }
    
    companion object {
        /**
         * Create PostMapItem from full Post entity with null-safe mapping
         * Rules.md compliant - Professional null-safe conversion
         */
        fun fromPost(post: Post): PostMapItem? {
            // Return null if essential location data is missing
            val location = post.location ?: return null
            
            return PostMapItem(
                id = post.id,
                userId = post.ownerId,
                location = location,
                imageUrl = post.images.firstOrNull() ?: "",
                categoryEn = post.categoriesEn.firstOrNull() ?: post.categoryEn ?: "other",
                categoryDe = post.categoriesDe.firstOrNull() ?: post.categoryDe ?: "Sonstiges",
                isPremiumOwner = post.getEffectiveIsOwnerPremium(),
                ownerLevel = post.getEffectiveOwnerLevel(),
                ownerFrameType = post.ownerPremiumFrameType,
                createdAt = post.createdAt,
                availabilityPercent = post.availabilityPercent ?: 100, // Default to 100% if null
                isArchived = post.status != PostStatus.ACTIVE,
                distanceFromUser = post.distanceFromUser ?: 0.0,
                city = post.getEffectiveCity() ?: "Unknown",
                description = post.description,
                likesCount = post.likesCount,
                commentsCount = post.commentsCount
            )
        }
        
        /**
         * Create PostMapItem list from Post list with null filtering
         * Rules.md compliant - Batch conversion with error handling
         */
        fun fromPostList(posts: List<Post>): List<PostMapItem> {
            return posts.mapNotNull { post ->
                try {
                    fromPost(post)
                } catch (e: Exception) {
                    // Log error in production, skip invalid posts
                    null
                }
            }
        }
    }
}
