package com.omerkaya.sperrmuellfinder.core.util

import androidx.compose.ui.graphics.Color
import com.omerkaya.sperrmuellfinder.core.ui.theme.MarkerBasic
import com.omerkaya.sperrmuellfinder.core.ui.theme.MarkerPremium
import com.omerkaya.sperrmuellfinder.core.ui.theme.MarkerPremiumCrystal

object MarkerUtils {

    /**
     * Marker types for different user levels
     */
    enum class MarkerType {
        BASIC,
        PREMIUM_GOLD,
        PREMIUM_CRYSTAL
    }

    /**
     * Get marker color based on user premium status and level
     */
    fun getMarkerColor(isPremium: Boolean, level: Int): Color {
        return when {
            !isPremium -> MarkerBasic
            level >= 15 -> MarkerPremiumCrystal
            else -> MarkerPremium
        }
    }

    /**
     * Get marker type based on user status
     */
    fun getMarkerType(isPremium: Boolean, level: Int): MarkerType {
        return when {
            !isPremium -> MarkerType.BASIC
            level >= 15 -> MarkerType.PREMIUM_CRYSTAL
            else -> MarkerType.PREMIUM_GOLD
        }
    }

    /**
     * Get marker size multiplier (Premium markers are 10% bigger)
     */
    fun getMarkerSizeMultiplier(isPremium: Boolean): Float {
        return if (isPremium) 1.1f else 1.0f
    }

    /**
     * Check if marker should have bounce animation
     */
    fun shouldAnimateMarker(isPremium: Boolean): Boolean {
        return isPremium
    }

    /**
     * Get cluster color based on contained markers
     */
    fun getClusterColor(premiumCount: Int, basicCount: Int): Color {
        val totalCount = premiumCount + basicCount
        val premiumRatio = if (totalCount > 0) premiumCount.toFloat() / totalCount else 0f
        
        return when {
            premiumRatio >= 0.7f -> MarkerPremium
            premiumRatio >= 0.3f -> Color(0xFFFFA726) // Orange mix
            else -> MarkerBasic
        }
    }

    /**
     * Calculate cluster size based on item count
     */
    fun getClusterSize(itemCount: Int): Float {
        return when {
            itemCount <= 5 -> 40f
            itemCount <= 10 -> 50f
            itemCount <= 20 -> 60f
            itemCount <= 50 -> 70f
            else -> 80f
        }
    }

    /**
     * Get cluster text size based on item count
     */
    fun getClusterTextSize(itemCount: Int): Float {
        return when {
            itemCount <= 9 -> 14f
            itemCount <= 99 -> 12f
            else -> 10f
        }
    }

    /**
     * Format cluster count text
     */
    fun formatClusterCount(count: Int): String {
        return when {
            count <= 999 -> count.toString()
            count <= 9999 -> "${count / 1000}K"
            else -> "9K+"
        }
    }

    /**
     * Calculate marker priority for clustering
     * Higher priority = less likely to be clustered
     */
    fun getMarkerPriority(isPremium: Boolean, level: Int, postAge: Long): Int {
        var priority = 0
        
        // Premium bonus
        if (isPremium) priority += 10
        
        // Level bonus
        priority += (level / 5)
        
        // Recency bonus (newer posts get higher priority)
        val ageHours = (System.currentTimeMillis() - postAge) / (1000 * 60 * 60)
        priority += when {
            ageHours < 1 -> 5
            ageHours < 6 -> 3
            ageHours < 24 -> 1
            else -> 0
        }
        
        return priority
    }

    /**
     * Determine if markers should be clustered based on zoom level
     */
    fun shouldCluster(zoomLevel: Float): Boolean {
        return zoomLevel < 15f // Cluster when zoomed out
    }

    /**
     * Calculate clustering radius based on zoom level
     */
    fun getClusteringRadius(zoomLevel: Float): Double {
        return when {
            zoomLevel < 10f -> 100.0 // 100px radius for very zoomed out
            zoomLevel < 12f -> 80.0
            zoomLevel < 14f -> 60.0
            zoomLevel < 16f -> 40.0
            else -> 20.0 // Small radius for zoomed in
        }
    }
}
