package com.omerkaya.sperrmuellfinder.domain.model

import java.util.Date

/**
 * 👥 FOLLOW DOMAIN MODEL - SperrmüllFinder
 * Rules.md compliant - Clean Architecture domain entity
 * 
 * Features:
 * - Follow relationship tracking
 * - Timestamp for follow date
 * - Mutual follow detection
 * - Follow status management
 */
data class Follow(
    val id: String = "",
    val followerId: String = "",
    val followedId: String = "",
    val createdAt: Date = Date(),
    val isActive: Boolean = true
) {
    
    /**
     * Check if this is a mutual follow relationship.
     * Requires checking the reverse relationship separately.
     */
    fun isMutual(reverseFollow: Follow?): Boolean {
        return reverseFollow != null && 
               reverseFollow.followerId == this.followedId && 
               reverseFollow.followedId == this.followerId &&
               reverseFollow.isActive && this.isActive
    }
}

/**
 * 📊 FOLLOW STATS DOMAIN MODEL - SperrmüllFinder
 * Aggregated follow statistics for a user.
 */
data class FollowStats(
    val userId: String = "",
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val mutualFollowCount: Int = 0,
    val lastUpdated: Date = Date()
) {
    
    /**
     * Calculate follow ratio (followers/following).
     * Returns 0.0 if no following.
     */
    val followRatio: Double
        get() = if (followingCount > 0) followerCount.toDouble() / followingCount else 0.0
    
    /**
     * Check if user has high engagement (more followers than following).
     */
    val hasHighEngagement: Boolean
        get() = followerCount > followingCount
}
