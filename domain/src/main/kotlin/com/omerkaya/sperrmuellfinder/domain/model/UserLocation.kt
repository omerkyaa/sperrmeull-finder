package com.omerkaya.sperrmuellfinder.domain.model

import com.omerkaya.sperrmuellfinder.core.model.PostLocation
import java.util.Date

/**
 * Represents user's current location with accuracy and timestamp information.
 * Used for location-based queries and map positioning.
 */
data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float, // Accuracy in meters
    val timestamp: Date, // Changed from LocalDateTime to Date for Android compatibility
    val provider: LocationProvider,
    val isFromCache: Boolean = false
) {
    /**
     * Convert to PostLocation for compatibility
     */
    fun toPostLocation(): PostLocation {
        return PostLocation(
            latitude = latitude,
            longitude = longitude,
            // City and country will be filled by reverse geocoding in LocationRepository
            city = null,
            country = null,
            address = null,
            geohash = null // Will be calculated in repository
        )
    }
    
    /**
     * Check if location is recent enough to be considered current
     */
    val isCurrent: Boolean
        get() {
            val fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000)
            return timestamp.time > fiveMinutesAgo
        }
    
    /**
     * Check if location has good accuracy
     */
    val hasGoodAccuracy: Boolean
        get() = accuracy <= 100f // Within 100 meters
    
    /**
     * Check if location is reliable for map operations
     */
    val isReliable: Boolean
        get() = isCurrent && hasGoodAccuracy && !isFromCache
    
    /**
     * Get formatted accuracy for display
     */
    fun getFormattedAccuracy(): String {
        return when {
            accuracy < 10 -> "Sehr genau"
            accuracy < 50 -> "Genau"
            accuracy < 100 -> "Mäßig genau"
            else -> "Ungenau"
        }
    }
    
    companion object {
        /**
         * Create UserLocation from PostLocation with default values
         */
        fun fromPostLocation(
            postLocation: PostLocation,
            accuracy: Float = 50f,
            provider: LocationProvider = LocationProvider.FUSED
        ): UserLocation {
            return UserLocation(
                latitude = postLocation.latitude,
                longitude = postLocation.longitude,
                accuracy = accuracy,
                timestamp = Date(), // Use Date() instead of LocalDateTime.now()
                provider = provider
            )
        }
        
        /**
         * Default location (Berlin) for fallback
         */
        fun defaultLocation(): UserLocation {
            return UserLocation(
                latitude = 52.5200,
                longitude = 13.4050,
                accuracy = 1000f,
                timestamp = Date(), // Use Date() instead of LocalDateTime.now()
                provider = LocationProvider.DEFAULT,
                isFromCache = true
            )
        }
    }
}

/**
 * Location provider types
 */
enum class LocationProvider {
    GPS,        // GPS provider
    NETWORK,    // Network provider
    FUSED,      // Fused location provider (recommended)
    PASSIVE,    // Passive provider
    DEFAULT     // Default/fallback location
}
