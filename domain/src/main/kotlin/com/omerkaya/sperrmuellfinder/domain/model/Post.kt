package com.omerkaya.sperrmuellfinder.domain.model

import com.omerkaya.sperrmuellfinder.core.model.PostLocation
import java.util.Date

/**
 * Post entity representing a shared item in the SperrmüllFinder app.
 * Contains all information about a posted item including location, images, and interaction data.
 * Rules.md compliant - Clean Architecture domain layer with no external dependencies.
 */
data class Post(
    val id: String,
    val ownerId: String,
    // Denormalized user data for realtime display
    val ownerDisplayName: String? = null, // ownerName in requirements
    val ownerPhotoUrl: String? = null,
    val ownerLevel: Int? = null,
    val isOwnerPremium: Boolean? = null,
    val ownerPremiumFrameType: PremiumFrameType = PremiumFrameType.NONE,
    val images: List<String> = emptyList(), // imageUrls in requirements
    val description: String,
    val location: PostLocation? = null,
    val locationStreet: String? = null, // Street name for display
    val locationCity: String? = null, // City name for display
    val city: String, // Legacy field, use locationCity preferably
    val categoriesEn: List<String> = emptyList(), // Internal categories (EN)
    val categoriesDe: List<String> = emptyList(), // Display categories (DE)
    val categoryEn: String? = null, // Single category for compatibility
    val categoryDe: String? = null, // Single category for compatibility
    val availabilityPercent: Int? = null, // 0-100, only visible to Premium users
    val likesCount: Int = 0, // likeCount in requirements - denormalized
    val commentsCount: Int = 0, // commentCount in requirements - denormalized
    val viewsCount: Int = 0,
    val sharesCount: Int = 0,
    val status: PostStatus = PostStatus.ACTIVE,
    val createdAt: Date = Date(), // Will be converted to/from Long (epoch millis) in mapper
    val expiresAt: Date = Date(System.currentTimeMillis() + 72 * 60 * 60 * 1000), // +72 hours
    val updatedAt: Date = Date(),
    val isLikedByCurrentUser: Boolean = false, // isLikedByMe in requirements
    val isFavoritedByCurrentUser: Boolean = false, // isFavoritedByMe in requirements
    val distanceFromUser: Double? = null // Distance in meters, calculated client-side
) {
    /**
     * Check if post is still active and not expired
     */
    fun isActive(): Boolean {
        return status == PostStatus.ACTIVE && expiresAt.after(Date())
    }

    /**
     * Get time remaining until expiration
     */
    fun getTimeRemainingHours(): Long {
        val now = System.currentTimeMillis()
        val expiration = expiresAt.time
        val diffInHours = (expiration - now) / (1000 * 60 * 60)
        return maxOf(0, diffInHours)
    }

    /**
     * Get formatted distance string
     */
    fun getFormattedDistance(): String? {
        return distanceFromUser?.let { distance ->
            when {
                distance < 1000 -> "${distance.toInt()}m"
                distance < 10000 -> "${String.format("%.1f", distance / 1000)}km"
                else -> "${(distance / 1000).toInt()}km"
            }
        }
    }

    /**
     * Get primary category for display
     */
    fun getPrimaryCategoryDe(): String? = categoriesDe.firstOrNull()

    /**
     * Check if post has multiple images
     */
    fun hasMultipleImages(): Boolean = images.size > 1

    /**
     * Get availability status for Premium users
     */
    fun getAvailabilityStatus(): AvailabilityStatus {
        return when {
            availabilityPercent != null && availabilityPercent >= 80 -> AvailabilityStatus.VERY_LIKELY_AVAILABLE
            availabilityPercent != null && availabilityPercent >= 60 -> AvailabilityStatus.LIKELY_AVAILABLE
            availabilityPercent != null && availabilityPercent >= 40 -> AvailabilityStatus.MAYBE_AVAILABLE
            availabilityPercent != null && availabilityPercent >= 20 -> AvailabilityStatus.UNLIKELY_AVAILABLE
            else -> AvailabilityStatus.LIKELY_TAKEN
        }
    }

    /**
     * Get effective city name (prefer locationCity over legacy city)
     */
    fun getEffectiveCity(): String? {
        return locationCity?.takeIf { it.isNotBlank() } ?: city.takeIf { it.isNotBlank() }
    }

    /**
     * Get effective owner display name with fallback
     */
    fun getEffectiveOwnerDisplayName(): String {
        return ownerDisplayName?.takeIf { it.isNotBlank() } ?: "Unbekannter Nutzer"
    }

    /**
     * Get effective owner level with fallback
     */
    fun getEffectiveOwnerLevel(): Int {
        return ownerLevel ?: 1
    }

    /**
     * Get effective premium status with fallback
     */
    fun getEffectiveIsOwnerPremium(): Boolean {
        return isOwnerPremium ?: false
    }
}


/**
 * Availability status for Premium users
 */
enum class AvailabilityStatus {
    VERY_LIKELY_AVAILABLE,
    LIKELY_AVAILABLE,
    MAYBE_AVAILABLE,
    UNLIKELY_AVAILABLE,
    LIKELY_TAKEN
}

/**
 * Post interaction types
 */
enum class PostInteractionType {
    LIKE,
    COMMENT,
    REPORT
}


/**
 * Report entity for posts and comments
 */
data class Report(
    val id: String,
    val type: ReportTargetType,
    val targetId: String, // postId, commentId, or userId
    val reporterId: String,
    val reporterName: String = "",
    val reporterPhotoUrl: String? = null,
    val reason: ReportReason,
    val description: String? = null,
    val createdAt: Date = Date(),
    val status: ReportStatus = ReportStatus.OPEN,
    val priority: ReportPriority = ReportPriority.MEDIUM,
    val assignedTo: String? = null,
    val assignedToName: String? = null,
    val adminNotes: String? = null,
    val resolvedAt: Date? = null,
    val resolvedBy: String? = null,
    val resolvedByName: String? = null,
    val moderationQueueId: String? = null,
    // Target details (denormalized for quick display)
    val targetOwnerId: String? = null,
    val targetOwnerName: String? = null,
    val targetContent: String? = null, // post description or comment text
    val targetImageUrl: String? = null // first image of post
)

/**
 * Report priority for admin queue
 */
enum class ReportPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;
    
    fun getDisplayName(isGerman: Boolean = true): String {
        return when (this) {
            LOW -> if (isGerman) "Niedrig" else "Low"
            MEDIUM -> if (isGerman) "Mittel" else "Medium"
            HIGH -> if (isGerman) "Hoch" else "High"
            CRITICAL -> if (isGerman) "Kritisch" else "Critical"
        }
    }
}

/**
 * Report action for admin resolution
 */
data class ReportAction(
    val action: ReportActionType,
    val reason: String,
    val banDuration: Int? = null // hours, -1 for permanent
)

/**
 * Report action types - 3-Tier Ban System
 */
enum class ReportActionType {
    DISMISS,                    // Report is invalid
    WARN_USER,                 // Send warning notification
    DELETE_CONTENT,            // Delete post/comment
    SOFT_BAN_3_DAYS,          // Soft ban for 3 days
    SOFT_BAN_1_WEEK,          // Soft ban for 1 week
    SOFT_BAN_1_MONTH,         // Soft ban for 1 month
    HARD_BAN,                 // Permanent ban (Auth disabled)
    DELETE_ACCOUNT;           // Complete account deletion
    
    fun getDisplayName(isGerman: Boolean = true): String {
        return when (this) {
            DISMISS -> if (isGerman) "Ablehnen" else "Dismiss"
            WARN_USER -> if (isGerman) "Benutzer warnen" else "Warn User"
            DELETE_CONTENT -> if (isGerman) "Inhalt löschen" else "Delete Content"
            SOFT_BAN_3_DAYS -> if (isGerman) "🚫 Soft Ban (3 Tage)" else "🚫 Soft Ban (3 Days)"
            SOFT_BAN_1_WEEK -> if (isGerman) "🚫 Soft Ban (1 Woche)" else "🚫 Soft Ban (1 Week)"
            SOFT_BAN_1_MONTH -> if (isGerman) "🚫 Soft Ban (1 Monat)" else "🚫 Soft Ban (1 Month)"
            HARD_BAN -> if (isGerman) "🔒 Hard Ban (Permanent)" else "🔒 Hard Ban (Permanent)"
            DELETE_ACCOUNT -> if (isGerman) "🗑️ Konto löschen" else "🗑️ Delete Account"
        }
    }
    
    fun getDescription(isGerman: Boolean = true): String {
        return when (this) {
            DISMISS -> if (isGerman) "Report als ungültig markieren" else "Mark report as invalid"
            WARN_USER -> if (isGerman) "Warnung senden (keine Sperrung)" else "Send warning (no ban)"
            DELETE_CONTENT -> if (isGerman) "Post/Kommentar löschen" else "Delete post/comment"
            SOFT_BAN_3_DAYS -> if (isGerman) "Zugriff für 3 Tage blockieren" else "Block access for 3 days"
            SOFT_BAN_1_WEEK -> if (isGerman) "Zugriff für 1 Woche blockieren" else "Block access for 1 week"
            SOFT_BAN_1_MONTH -> if (isGerman) "Zugriff für 1 Monat blockieren" else "Block access for 1 month"
            HARD_BAN -> if (isGerman) "Permanente Sperrung + Auth deaktiviert" else "Permanent ban + Auth disabled"
            DELETE_ACCOUNT -> if (isGerman) "Konto komplett löschen (unumkehrbar)" else "Delete account completely (irreversible)"
        }
    }
    
    fun getBanDurationDays(): Int? {
        return when (this) {
            SOFT_BAN_3_DAYS -> 3
            SOFT_BAN_1_WEEK -> 7
            SOFT_BAN_1_MONTH -> 30
            else -> null
        }
    }
    
    fun isDestructive(): Boolean {
        return this == HARD_BAN || this == DELETE_ACCOUNT
    }
}

/**
 * Report target type
 */
enum class ReportTargetType {
    POST,
    COMMENT,
    USER
}

// ReportReason moved to separate file: ReportReason.kt

/**
 * Report status
 */
enum class ReportStatus {
    OPEN,
    UNDER_REVIEW,
    APPROVED,
    DISMISSED,
    RESOLVED
}
