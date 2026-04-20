package com.omerkaya.sperrmuellfinder.domain.model

import java.util.Date

/**
 * Moderation queue entry
 * Represents an item waiting for admin review
 */
data class ModerationQueue(
    val id: String,
    val type: ModerationType,
    val priority: ModerationPriority,
    val reportId: String?,
    val targetType: ReportTargetType,
    val targetId: String,
    val status: ModerationStatus,
    val assignedTo: String?,
    val assignedToName: String?,
    val createdAt: Date,
    val resolvedAt: Date?,
    val resolvedBy: String?,
    val resolvedByName: String?,
    // Denormalized target data for quick display
    val targetContent: String?,
    val targetImageUrl: String?,
    val targetOwnerId: String?,
    val targetOwnerName: String?
)

/**
 * Moderation type
 */
enum class ModerationType {
    REPORT,      // User-submitted report
    AUTO_FLAG;   // Automatically flagged content
    
    fun getDisplayName(isGerman: Boolean = true): String {
        return when (this) {
            REPORT -> if (isGerman) "Bericht" else "Report"
            AUTO_FLAG -> if (isGerman) "Auto-Markierung" else "Auto Flag"
        }
    }
}

/**
 * Moderation priority
 */
enum class ModerationPriority {
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
    
    /**
     * Get color for priority badge
     */
    fun getColorHex(): String {
        return when (this) {
            LOW -> "#4CAF50"      // Green
            MEDIUM -> "#FF9800"   // Orange
            HIGH -> "#F44336"     // Red
            CRITICAL -> "#9C27B0" // Purple
        }
    }
}

/**
 * Moderation status
 */
enum class ModerationStatus {
    PENDING,
    IN_REVIEW,
    RESOLVED,
    DISMISSED;
    
    fun getDisplayName(isGerman: Boolean = true): String {
        return when (this) {
            PENDING -> if (isGerman) "Ausstehend" else "Pending"
            IN_REVIEW -> if (isGerman) "In Prüfung" else "In Review"
            RESOLVED -> if (isGerman) "Gelöst" else "Resolved"
            DISMISSED -> if (isGerman) "Abgelehnt" else "Dismissed"
        }
    }
}
