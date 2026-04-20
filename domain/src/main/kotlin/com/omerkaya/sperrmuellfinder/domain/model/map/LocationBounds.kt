package com.omerkaya.sperrmuellfinder.domain.model.map

import com.omerkaya.sperrmuellfinder.core.model.PostLocation

/**
 * Represents a geographical bounding box defined by southwest and northeast corners
 * Rules.md compliant - Clean Architecture domain layer
 */
data class LocationBounds(
    val southwest: PostLocation,
    val northeast: PostLocation
) {
    companion object {
        /**
         * Creates a bounding box around a center point with given radius in kilometers
         * @param center Center location
         * @param radiusKm Radius in kilometers
         * @return LocationBounds representing the area
         */
        fun fromCenterAndRadius(center: PostLocation, radiusKm: Double): LocationBounds {
            // Earth's radius in kilometers
            val earthRadius = 6371.0
            
            // Convert radius from kilometers to degrees
            val latChange = (radiusKm / earthRadius) * (180.0 / Math.PI)
            val lonChange = (radiusKm / earthRadius) * (180.0 / Math.PI) / Math.cos(center.latitude * Math.PI / 180.0)
            
            return LocationBounds(
                southwest = PostLocation(
                    latitude = (center.latitude - latChange).coerceIn(-90.0, 90.0),
                    longitude = (center.longitude - lonChange).coerceIn(-180.0, 180.0),
                    city = center.city,
                    country = center.country,
                    address = center.address
                ),
                northeast = PostLocation(
                    latitude = (center.latitude + latChange).coerceIn(-90.0, 90.0),
                    longitude = (center.longitude + lonChange).coerceIn(-180.0, 180.0),
                    city = center.city,
                    country = center.country,
                    address = center.address
                )
            )
        }

        /**
         * Creates a bounding box that contains all given locations
         * @param locations List of locations to include
         * @return LocationBounds containing all points
         */
        fun fromLocations(locations: List<PostLocation>): LocationBounds? {
            if (locations.isEmpty()) return null

            var minLat = 90.0
            var maxLat = -90.0
            var minLon = 180.0
            var maxLon = -180.0
            var city = locations.first().city
            var country = locations.first().country
            var address = locations.first().address

            locations.forEach { location ->
                minLat = minOf(minLat, location.latitude)
                maxLat = maxOf(maxLat, location.latitude)
                minLon = minOf(minLon, location.longitude)
                maxLon = maxOf(maxLon, location.longitude)
            }

            return LocationBounds(
                southwest = PostLocation(
                    latitude = minLat,
                    longitude = minLon,
                    city = city,
                    country = country,
                    address = address
                ),
                northeast = PostLocation(
                    latitude = maxLat,
                    longitude = maxLon,
                    city = city,
                    country = country,
                    address = address
                )
            )
        }
    }

    /**
     * Checks if a location is within these bounds
     * @param location Location to check
     * @return true if location is within bounds
     */
    fun contains(location: PostLocation): Boolean {
        return location.latitude in southwest.latitude..northeast.latitude &&
                location.longitude in southwest.longitude..northeast.longitude
    }

    /**
     * Expands bounds to include the given location
     * @param location Location to include
     * @return New LocationBounds that includes both the original bounds and the new location
     */
    fun includingLocation(location: PostLocation): LocationBounds {
        return LocationBounds(
            southwest = PostLocation(
                latitude = minOf(southwest.latitude, location.latitude),
                longitude = minOf(southwest.longitude, location.longitude),
                city = southwest.city,
                country = southwest.country,
                address = southwest.address
            ),
            northeast = PostLocation(
                latitude = maxOf(northeast.latitude, location.latitude),
                longitude = maxOf(northeast.longitude, location.longitude),
                city = northeast.city,
                country = northeast.country,
                address = northeast.address
            )
        )
    }

    /**
     * Calculates the center point of these bounds
     * @return Location at the center of the bounds
     */
    fun getCenter(): PostLocation {
        return PostLocation(
            latitude = (southwest.latitude + northeast.latitude) / 2.0,
            longitude = (southwest.longitude + northeast.longitude) / 2.0,
            city = southwest.city,
            country = southwest.country,
            address = southwest.address
        )
    }

    /**
     * Checks if these bounds intersect with other bounds
     * @param other Other LocationBounds to check
     * @return true if bounds intersect
     */
    fun intersects(other: LocationBounds): Boolean {
        return !(other.northeast.longitude < southwest.longitude ||
                other.southwest.longitude > northeast.longitude ||
                other.northeast.latitude < southwest.latitude ||
                other.southwest.latitude > northeast.latitude)
    }

    /**
     * Creates a new bounds that contains both this and other bounds
     * @param other Other LocationBounds to include
     * @return New LocationBounds containing both areas
     */
    fun union(other: LocationBounds): LocationBounds {
        return LocationBounds(
            southwest = PostLocation(
                latitude = minOf(southwest.latitude, other.southwest.latitude),
                longitude = minOf(southwest.longitude, other.southwest.longitude),
                city = southwest.city,
                country = southwest.country,
                address = southwest.address
            ),
            northeast = PostLocation(
                latitude = maxOf(northeast.latitude, other.northeast.latitude),
                longitude = maxOf(northeast.longitude, other.northeast.longitude),
                city = northeast.city,
                country = northeast.country,
                address = northeast.address
            )
        )
    }

    /**
     * Calculates the diagonal distance of the bounds in kilometers
     * @return Distance in kilometers
     */
    fun getDiagonalDistanceKm(): Double {
        val earthRadius = 6371.0 // Earth's radius in kilometers
        
        val lat1 = Math.toRadians(southwest.latitude)
        val lon1 = Math.toRadians(southwest.longitude)
        val lat2 = Math.toRadians(northeast.latitude)
        val lon2 = Math.toRadians(northeast.longitude)
        
        val dLat = lat2 - lat1
        val dLon = lon2 - lon1
        
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return earthRadius * c
    }
}
