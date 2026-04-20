package com.omerkaya.sperrmuellfinder.domain.model

import java.util.Date

/**
 * 🔔 NOTIFICATION DOMAIN MODEL - SperrmüllFinder
 * Instagram-style notifications with real-time Firebase integration
 * Rules.md compliant - Clean Architecture domain entity
 * 
 * Features:
 * - All notification types (like, comment, follow, system)
 * - Deep link data for navigation
 * - Read/unread status tracking
 * - User profile data for UI display
 * - Timestamp for ordering (newest first)
 * - FCM push notification support
 */
data class Notification(
    val id: String = "",
    val userId: String = "", // Recipient user ID
    val type: NotificationType = NotificationType.LIKE,
    val title: String = "",
    val body: String = "",
    val deeplink: String = "",
    val isRead: Boolean = false,
    val data: Map<String, Any> = emptyMap(),
    val createdAt: Date = Date(),
    
    // Actor (person who triggered the notification)
    val actorUserId: String = "",
    val actorUsername: String = "",
    val actorPhotoUrl: String? = null,
    val actorLevel: Int = 1,
    val actorIsPremium: Boolean = false,
    
    // Post data (for like/comment notifications)
    val postId: String? = null,
    val postImageUrl: String? = null,
    val postDescription: String? = null,
    
    // Comment data (for comment notifications)
    val commentId: String? = null,
    val commentText: String? = null
) {
    private fun mapString(key: String): String? = (data[key] as? String)?.trim()?.takeIf { it.isNotEmpty() }
    
    /**
     * Get the from user ID from notification data if available.
     */
    val fromUserId: String?
        get() = data["fromUserId"] as? String

    /**
     * Resolve the best actor user id from normalized and legacy payload keys.
     */
    val resolvedActorUserId: String?
        get() {
            val direct = actorUserId.trim().takeIf { it.isNotEmpty() }
            if (direct != null) return direct

            val fallback = listOf(
                fromUserId?.trim(),
                mapString("actorUserId"),
                mapString("userId"),
                mapString("uid")
            ).firstOrNull { !it.isNullOrBlank() }
            if (!fallback.isNullOrBlank()) return fallback

            val profileDeepLink = deeplink.takeIf { it.startsWith("profile:") }
            return profileDeepLink?.removePrefix("profile:")?.trim()?.takeIf { it.isNotEmpty() }
        }
    
    /**
     * Get the from user name from notification data if available.
     */
    val fromUserName: String?
        get() = data["fromUserName"] as? String
    
    /**
     * Check if this notification has valid deep link data.
     */
    val hasValidDeepLink: Boolean
        get() = deeplink.isNotBlank() || postId != null || resolvedActorUserId != null
}

/**
 * Notification types supported by the app.
 * Each type has specific UI behavior and deep link handling.
 */
enum class NotificationType(val value: String) {
    LIKE("like"),                    // Someone liked your post
    COMMENT("comment"),              // Someone commented on your post
    FOLLOW("follow"),                // Someone started following you
    ADMIN_PENALTY("admin_penalty"),  // Warning/penalty from admins
    PREMIUM_NEARBY_POST("premium_nearby_post"), // Premium nearby post alert
    POST_EXPIRED("post_expired"),    // Your post expired (72h)
    PREMIUM_EXPIRED("premium_expired"), // Premium subscription expired
    SYSTEM("system");                // System announcements (including admin)
    
    companion object {
        fun fromString(value: String): NotificationType {
            return values().find { it.value == value } ?: SYSTEM
        }
    }
    
    /**
     * Get the icon resource for this notification type
     */
    val iconRes: String
        get() = when (this) {
            LIKE -> "favorite"
            COMMENT -> "comment"
            FOLLOW -> "person_add"
            ADMIN_PENALTY -> "gavel"
            PREMIUM_NEARBY_POST -> "location_on"
            POST_EXPIRED -> "schedule"
            PREMIUM_EXPIRED -> "star"
            SYSTEM -> "info"
        }
}
