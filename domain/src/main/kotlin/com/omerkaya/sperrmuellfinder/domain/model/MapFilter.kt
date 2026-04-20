package com.omerkaya.sperrmuellfinder.domain.model

/**
 * Map filter configuration for location-based post queries.
 * Handles Basic vs Premium user restrictions and filter validation.
 */
data class MapFilter(
    val radiusMeters: Int,
    val categories: List<String> = emptyList(), // Category IDs (EN)
    val availabilityOnly: Boolean = false, // Premium: show only available items
    val showPremiumHighlights: Boolean = false, // Premium: enhanced markers
    val minAvailabilityPercent: Int = 0, // Premium: minimum availability threshold
    val maxAgeHours: Int = 72, // Maximum age of posts to show
    val sortBy: MapSortBy = MapSortBy.DISTANCE,
    val includeArchived: Boolean = false // Admin only
) {
    
    /**
     * Validate filter against user's premium status and apply restrictions
     */
    fun validateForUser(isPremium: Boolean): MapFilter {
        return if (isPremium) {
            // Premium users: no restrictions
            this
        } else {
            // Basic users: apply restrictions
            this.copy(
                radiusMeters = minOf(radiusMeters, BASIC_MAX_RADIUS_METERS),
                categories = emptyList(), // No category filtering for basic
                availabilityOnly = false, // No availability filtering for basic
                showPremiumHighlights = false, // No premium highlights for basic
                minAvailabilityPercent = 0, // No availability threshold for basic
                sortBy = MapSortBy.DATE // Basic users get date-based sorting
            )
        }
    }
    
    /**
     * Check if filter has any active filters
     */
    val hasActiveFilters: Boolean
        get() = categories.isNotEmpty() || 
                availabilityOnly || 
                minAvailabilityPercent > 0 ||
                radiusMeters != DEFAULT_RADIUS_METERS
    
    /**
     * Get filter summary for display
     */
    fun getFilterSummary(): String {
        val parts = mutableListOf<String>()
        
        // Radius
        val radiusKm = radiusMeters / 1000.0
        parts.add(if (radiusKm >= 1.0) "${radiusKm.toInt()} km" else "${radiusMeters} m")
        
        // Categories
        if (categories.isNotEmpty()) {
            parts.add("${categories.size} Kategorien")
        }
        
        // Availability
        if (availabilityOnly) {
            parts.add("Nur verfügbar")
        }
        
        return parts.joinToString(" • ")
    }
    
    /**
     * Create filter for basic user with safe defaults
     */
    fun toBasicFilter(): MapFilter {
        return MapFilter(
            radiusMeters = minOf(radiusMeters, BASIC_MAX_RADIUS_METERS),
            categories = emptyList(),
            availabilityOnly = false,
            showPremiumHighlights = false,
            minAvailabilityPercent = 0,
            maxAgeHours = 72,
            sortBy = MapSortBy.DATE,
            includeArchived = false
        )
    }
    
    /**
     * Create filter for premium user with all features
     */
    fun toPremiumFilter(): MapFilter {
        return this.copy(
            showPremiumHighlights = true,
            sortBy = MapSortBy.DISTANCE // Premium gets distance-based sorting
        )
    }
    
    companion object {
        const val BASIC_MAX_RADIUS_METERS = 1500 // 1.5 km for basic users
        const val PREMIUM_MAX_RADIUS_METERS = 20000 // 20 km for premium users
        const val DEFAULT_RADIUS_METERS = 1500
        
        /**
         * Default filter for basic users
         */
        fun defaultBasic(): MapFilter {
            return MapFilter(
                radiusMeters = BASIC_MAX_RADIUS_METERS,
                sortBy = MapSortBy.DATE
            )
        }
        
        /**
         * Default filter for premium users
         */
        fun defaultPremium(): MapFilter {
            return MapFilter(
                radiusMeters = 5000, // 5 km default for premium
                showPremiumHighlights = true,
                sortBy = MapSortBy.DISTANCE
            )
        }
        
        /**
         * Create filter from remote config values
         */
        fun fromRemoteConfig(
            basicRadiusMeters: Int = BASIC_MAX_RADIUS_METERS,
            maxAgeHours: Int = 72,
            availabilityThreshold: Int = 60
        ): MapFilter {
            return MapFilter(
                radiusMeters = basicRadiusMeters,
                maxAgeHours = maxAgeHours,
                minAvailabilityPercent = availabilityThreshold
            )
        }
    }
}

/**
 * Map sorting options
 */
enum class MapSortBy {
    DISTANCE,   // Sort by distance from user (Premium)
    DATE,       // Sort by creation date (Basic default)
    POPULARITY, // Sort by likes + comments (Premium)
    AVAILABILITY // Sort by availability percentage (Premium)
}
