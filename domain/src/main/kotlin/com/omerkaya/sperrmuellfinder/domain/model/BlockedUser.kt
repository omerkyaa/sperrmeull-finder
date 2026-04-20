package com.omerkaya.sperrmuellfinder.domain.model

import java.util.Date

/**
 * Domain model for blocked user
 * Rules.md compliant - Clean Architecture domain layer
 */
data class BlockedUser(
    val id: String,
    val blockerId: String,
    val blockedUserId: String,
    val blockedUserName: String,
    val blockedUserPhotoUrl: String?,
    val blockedUserLevel: Int = 0,
    val blockedUserIsPremium: Boolean = false,
    val createdAt: Date,
    val reason: String? = null
) {
    /**
     * Check if block is recent (within 7 days)
     */
    fun isRecentBlock(): Boolean {
        val sevenDaysAgo = Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000)
        return createdAt.after(sevenDaysAgo)
    }
}
