package com.omerkaya.sperrmuellfinder.core.model

/**
 * Post location information for geospatial operations.
 * Core model used across all layers for location-based functionality.
 * Rules.md compliant - Professional location data structure.
 */
data class PostLocation(
    val latitude: Double,
    val longitude: Double,
    val city: String? = null,
    val country: String? = null,
    val address: String? = null,
    val geohash: String? = null // For efficient location queries
) {
    
    /**
     * Validate location coordinates
     */
    fun isValid(): Boolean {
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }
    
    /**
     * Get formatted coordinates string
     */
    fun getFormattedCoordinates(): String {
        return "${String.format("%.6f", latitude)}, ${String.format("%.6f", longitude)}"
    }
    
    /**
     * Get display address or coordinates fallback
     */
    fun getDisplayText(): String {
        return when {
            !address.isNullOrBlank() -> address
            !city.isNullOrBlank() && !country.isNullOrBlank() -> "$city, $country"
            !city.isNullOrBlank() -> city
            else -> getFormattedCoordinates()
        }
    }
    
    /**
     * Create PostLocation with geohash
     */
    fun withGeohash(geohash: String): PostLocation {
        return copy(geohash = geohash)
    }
    
    companion object {
        /**
         * Create PostLocation from coordinates
         */
        fun fromCoordinates(
            latitude: Double, 
            longitude: Double, 
            city: String? = null,
            country: String? = null,
            address: String? = null
        ): PostLocation {
            return PostLocation(latitude, longitude, city, country, address)
        }
        
        /**
         * Berlin center as default location
         */
        fun berlinCenter(): PostLocation {
            return PostLocation(
                latitude = 52.520008,
                longitude = 13.404954,
                city = "Berlin",
                country = "Germany",
                address = "Berlin, Deutschland"
            )
        }
        
        /**
         * Create default location (Berlin) for fallback scenarios
         */
        fun defaultLocation(): PostLocation {
            return berlinCenter()
        }
        
        /**
         * Check if coordinates are valid
         */
        fun isValidCoordinates(latitude: Double, longitude: Double): Boolean {
            return latitude in -90.0..90.0 && longitude in -180.0..180.0
        }
    }
}
