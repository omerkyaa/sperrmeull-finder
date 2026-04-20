package com.omerkaya.sperrmuellfinder.domain.util

import com.omerkaya.sperrmuellfinder.core.model.PostLocation
import kotlin.math.*

/**
 * Utility functions for location calculations
 * Rules.md compliant - Clean Architecture domain layer
 */
object LocationUtils {
    private const val EARTH_RADIUS_KM = 6371.0 // Earth's radius in kilometers

    /**
     * Calculate distance between two locations using Haversine formula
     * @param location1 First location
     * @param location2 Second location
     * @return Distance in kilometers
     */
    fun calculateDistance(location1: PostLocation, location2: PostLocation): Double {
        val lat1 = Math.toRadians(location1.latitude)
        val lon1 = Math.toRadians(location1.longitude)
        val lat2 = Math.toRadians(location2.latitude)
        val lon2 = Math.toRadians(location2.longitude)

        val dLat = lat2 - lat1
        val dLon = lon2 - lon1

        val a = sin(dLat / 2).pow(2) +
                cos(lat1) * cos(lat2) *
                sin(dLon / 2).pow(2)

        val c = 2 * asin(sqrt(a))

        return EARTH_RADIUS_KM * c
    }

    /**
     * Calculate bearing between two locations
     * @param location1 First location (start point)
     * @param location2 Second location (end point)
     * @return Bearing in degrees (0-360)
     */
    fun calculateBearing(location1: PostLocation, location2: PostLocation): Double {
        val lat1 = Math.toRadians(location1.latitude)
        val lon1 = Math.toRadians(location1.longitude)
        val lat2 = Math.toRadians(location2.latitude)
        val lon2 = Math.toRadians(location2.longitude)

        val dLon = lon2 - lon1

        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) -
                sin(lat1) * cos(lat2) * cos(dLon)

        var bearing = Math.toDegrees(atan2(y, x))
        if (bearing < 0) {
            bearing += 360.0
        }

        return bearing
    }

    /**
     * Calculate destination point given distance and bearing from start point
     * @param start Starting location
     * @param distanceKm Distance in kilometers
     * @param bearingDegrees Bearing in degrees
     * @return Destination location
     */
    fun calculateDestination(
        start: PostLocation,
        distanceKm: Double,
        bearingDegrees: Double
    ): PostLocation {
        val d = distanceKm / EARTH_RADIUS_KM
        val bearing = Math.toRadians(bearingDegrees)
        val lat1 = Math.toRadians(start.latitude)
        val lon1 = Math.toRadians(start.longitude)

        val lat2 = asin(
            sin(lat1) * cos(d) +
                    cos(lat1) * sin(d) * cos(bearing)
        )

        val lon2 = lon1 + atan2(
            sin(bearing) * sin(d) * cos(lat1),
            cos(d) - sin(lat1) * sin(lat2)
        )

        return PostLocation(
            latitude = Math.toDegrees(lat2),
            longitude = Math.toDegrees(lon2).coerceIn(-180.0, 180.0),
            city = start.city,
            country = start.country,
            address = start.address
        )
    }

    /**
     * Check if a location is within a radius of another location
     * @param center Center location
     * @param test Location to test
     * @param radiusKm Radius in kilometers
     * @return true if test location is within radius
     */
    fun isWithinRadius(center: PostLocation, test: PostLocation, radiusKm: Double): Boolean {
        return calculateDistance(center, test) <= radiusKm
    }

    /**
     * Format distance for display
     * @param distanceKm Distance in kilometers
     * @return Formatted string (e.g. "1.2 km" or "800 m")
     */
    fun formatDistance(distanceKm: Double): String {
        return when {
            distanceKm >= 1.0 -> String.format("%.1f km", distanceKm)
            else -> String.format("%d m", (distanceKm * 1000).roundToInt())
        }
    }
}
