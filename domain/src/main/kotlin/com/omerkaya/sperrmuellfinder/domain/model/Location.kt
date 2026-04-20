package com.omerkaya.sperrmuellfinder.domain.model

import com.omerkaya.sperrmuellfinder.core.model.PostLocation

/**
 * Domain model for Location
 * Rules.md compliant - Clean Architecture domain layer
 */
data class Location(
    val latitude: Double,
    val longitude: Double,
    val city: String,
    val district: String,
    val postalCode: String,
    val country: String = "DE" // Default to Germany as per rules.md
) {
    /**
     * Convert to PostLocation for map operations
     */
    fun toPostLocation(): PostLocation {
        return PostLocation(
            latitude = latitude,
            longitude = longitude,
            city = city,
            country = country,
            address = "$district, $postalCode $city"
        )
    }
}
