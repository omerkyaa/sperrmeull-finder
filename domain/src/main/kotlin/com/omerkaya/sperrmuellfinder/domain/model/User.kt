package com.omerkaya.sperrmuellfinder.domain.model

import java.util.Date

/**
 * Domain model for User
 * Rules.md compliant - Clean Architecture domain layer
 */
data class User(
    val uid: String,
    val email: String,
    val displayName: String,
    val nickname: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val photoUrl: String?,
    val city: String?,
    val dob: Date?,
    val gender: String?,
    val xp: Int,
    val level: Int,
    val honesty: Int,
    val isPremium: Boolean,
    val premiumUntil: Date?,
    val badges: List<String>,
    val favorites: UserFavorites,
    val fcmToken: String?,
    val deviceTokens: List<String>,
    val deviceLang: String,
    val deviceModel: String,
    val deviceOs: String,
    val frameLevel: Int,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val createdAt: Date?,
    val updatedAt: Date?,
    val lastLoginAt: Date?,
    // Ban fields - 3-Tier Ban System
    val isBanned: Boolean = false,
    val banType: String? = null, // "SOFT_BAN", "HARD_BAN", or null
    val banUntil: Date? = null,  // For soft bans only
    val banReason: String? = null,
    val bannedBy: String? = null,
    val bannedAt: Date? = null,
    val authDisabled: Boolean = false // For hard bans (Firebase Auth disabled)
) {
    /**
     * Get user's honesty status based on current honesty score
     */
    fun getHonestyStatus(): HonestyStatus {
        return HonestyStatus.fromScore(honesty)
    }

    /**
     * Check if user can post
     * MVP: Honesty-based gating removed, only check ban status
     */
    fun canPost(): Boolean {
        // MVP: Simplified - only check if user is not banned
        return !isBanned
    }

    /**
     * Get user's premium frame type based on premium status
     * MVP: Level-based frames removed, only premium/non-premium distinction
     */
    fun getPremiumFrameType(): PremiumFrameType {
        // MVP: Simplified - only check premium status, not level
        return if (isPremium) {
            PremiumFrameType.GOLD // Default premium frame
        } else {
            PremiumFrameType.NONE
        }
    }

    /**
     * Get user's full name from firstName and lastName
     * Returns empty string if both are empty
     */
    fun getFullName(): String {
        return listOfNotNull(
            firstName.takeIf { it.isNotBlank() },
            lastName.takeIf { it.isNotBlank() }
        ).joinToString(" ")
    }
    
    /**
     * Check if user is currently banned
     */
    fun isCurrentlyBanned(): Boolean {
        if (!isBanned) return false
        if (banUntil == null) return true // permanent ban
        return Date().before(banUntil)
    }
    
    /**
     * Get remaining ban duration in milliseconds
     * Returns null if not banned, -1 for permanent ban
     */
    fun getBanDurationRemaining(): Long? {
        if (!isCurrentlyBanned()) return null
        if (banUntil == null) return -1 // permanent
        return banUntil.time - Date().time
    }
    
    /**
     * Get formatted ban duration string
     */
    fun getFormattedBanDuration(isGerman: Boolean = true): String? {
        val remaining = getBanDurationRemaining() ?: return null
        
        if (remaining == -1L) {
            return if (isGerman) "Permanent" else "Permanent"
        }
        
        val hours = remaining / (1000 * 60 * 60)
        val days = hours / 24
        
        return when {
            days > 0 -> if (isGerman) "$days Tage" else "$days days"
            hours > 0 -> if (isGerman) "$hours Stunden" else "$hours hours"
            else -> if (isGerman) "Weniger als 1 Stunde" else "Less than 1 hour"
        }
    }
}