package com.omerkaya.sperrmuellfinder.core.util

import kotlin.math.*

/**
 * Utility class for geohash calculations and bounding box operations.
 * Used for efficient location-based Firestore queries.
 */
object GeoHashUtils {

    private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"
    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Generate geohash for given coordinates with specified precision.
     * 
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @param precision Geohash precision (default: 7 for ~150m accuracy)
     * @return Geohash string
     */
    fun encode(latitude: Double, longitude: Double, precision: Int = 7): String {
        val latRange = doubleArrayOf(-90.0, 90.0)
        val lonRange = doubleArrayOf(-180.0, 180.0)
        
        val geohash = StringBuilder()
        var bits = 0
        var bit = 0
        var even = true
        
        while (geohash.length < precision) {
            val range = if (even) lonRange else latRange
            val value = if (even) longitude else latitude
            
            val mid = (range[0] + range[1]) / 2
            
            if (value >= mid) {
                bits = (bits shl 1) or 1
                range[0] = mid
                } else {
                bits = bits shl 1
                range[1] = mid
            }
            
            even = !even
            
            if (++bit == 5) {
                geohash.append(BASE32[bits])
                bits = 0
                bit = 0
            }
        }
        
        return geohash.toString()
    }

    /**
     * Decode geohash to latitude/longitude coordinates.
     * 
     * @param geohash Geohash string
     * @return Pair of (latitude, longitude)
     */
    fun decode(geohash: String): Pair<Double, Double> {
        val latRange = doubleArrayOf(-90.0, 90.0)
        val lonRange = doubleArrayOf(-180.0, 180.0)
        
        var even = true
        
        for (char in geohash) {
            val cd = BASE32.indexOf(char)
            if (cd == -1) continue
            
            for (mask in arrayOf(16, 8, 4, 2, 1)) {
                val range = if (even) lonRange else latRange
                val mid = (range[0] + range[1]) / 2
                
                    if ((cd and mask) != 0) {
                    range[0] = mid
                    } else {
                    range[1] = mid
                }
                
                even = !even
            }
        }
        
        val latitude = (latRange[0] + latRange[1]) / 2
        val longitude = (lonRange[0] + lonRange[1]) / 2
        
        return Pair(latitude, longitude)
    }
    
    /**
     * Get geohash bounding box for efficient range queries.
     * 
     * @param centerLat Center latitude
     * @param centerLon Center longitude
     * @param radiusMeters Radius in meters
     * @param precision Geohash precision
     * @return List of geohash prefixes covering the area
     */
    fun getBoundingBoxHashes(
        centerLat: Double,
        centerLon: Double,
        radiusMeters: Double,
        precision: Int = 6
    ): List<String> {
        
        // Calculate bounding box
        val bounds = getBoundingBox(centerLat, centerLon, radiusMeters)
        
        // Generate geohashes for corners and center
        val hashes = mutableSetOf<String>()
        
        // Add center hash
        hashes.add(encode(centerLat, centerLon, precision))
        
        // Add corner hashes
        hashes.add(encode(bounds.minLat, bounds.minLon, precision))
        hashes.add(encode(bounds.minLat, bounds.maxLon, precision))
        hashes.add(encode(bounds.maxLat, bounds.minLon, precision))
        hashes.add(encode(bounds.maxLat, bounds.maxLon, precision))
        
        // Add intermediate points for better coverage
        val midLat = (bounds.minLat + bounds.maxLat) / 2
        val midLon = (bounds.minLon + bounds.maxLon) / 2
        
        hashes.add(encode(midLat, bounds.minLon, precision))
        hashes.add(encode(midLat, bounds.maxLon, precision))
        hashes.add(encode(bounds.minLat, midLon, precision))
        hashes.add(encode(bounds.maxLat, midLon, precision))
        
        // Get unique prefixes
        val prefixes = mutableSetOf<String>()
        for (hash in hashes) {
            for (i in 1..hash.length) {
                prefixes.add(hash.substring(0, i))
            }
        }
        
        // Return optimal prefixes (usually 4-6 characters for city-level queries)
        return prefixes.filter { it.length >= 4 && it.length <= 6 }.toList()
    }
    
    /**
     * Calculate bounding box for given center point and radius.
     */
    private fun getBoundingBox(centerLat: Double, centerLon: Double, radiusMeters: Double): BoundingBox {
        val latRadians = Math.toRadians(centerLat)
        
        // Calculate latitude bounds
        val deltaLat = radiusMeters / EARTH_RADIUS_METERS
        val minLat = centerLat - Math.toDegrees(deltaLat)
        val maxLat = centerLat + Math.toDegrees(deltaLat)
        
        // Calculate longitude bounds (adjusted for latitude)
        val deltaLon = radiusMeters / (EARTH_RADIUS_METERS * cos(latRadians))
        val minLon = centerLon - Math.toDegrees(deltaLon)
        val maxLon = centerLon + Math.toDegrees(deltaLon)
        
        return BoundingBox(minLat, maxLat, minLon, maxLon)
    }
    
    /**
     * Calculate distance between two points using Haversine formula.
     * 
     * @param lat1 First point latitude
     * @param lon1 First point longitude
     * @param lat2 Second point latitude
     * @param lon2 Second point longitude
     * @return Distance in meters
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    /**
     * Check if a point is within radius of another point.
     * 
     * @param centerLat Center point latitude
     * @param centerLon Center point longitude
     * @param pointLat Point to check latitude
     * @param pointLon Point to check longitude
     * @param radiusMeters Radius in meters
     * @return True if point is within radius
     */
    fun isWithinRadius(
        centerLat: Double, centerLon: Double,
        pointLat: Double, pointLon: Double,
        radiusMeters: Double
    ): Boolean {
        val distance = calculateDistance(centerLat, centerLon, pointLat, pointLon)
        return distance <= radiusMeters
    }

    /**
     * Get neighbors of a geohash (8 surrounding cells).
     * 
     * @param geohash Input geohash
     * @return List of neighboring geohashes
     */
    fun getNeighbors(geohash: String): List<String> {
        val neighbors = mutableListOf<String>()
        val (lat, lon) = decode(geohash)
        
        // Calculate approximate step size based on geohash precision
        val step = getStepSize(geohash.length)
        
        // Generate 8 neighbors
        for (dLat in arrayOf(-step, 0.0, step)) {
            for (dLon in arrayOf(-step, 0.0, step)) {
                if (dLat == 0.0 && dLon == 0.0) continue // Skip center
                
                val neighborLat = lat + dLat
                val neighborLon = lon + dLon
                
                // Check bounds
                if (neighborLat >= -90 && neighborLat <= 90 &&
                    neighborLon >= -180 && neighborLon <= 180) {
                    neighbors.add(encode(neighborLat, neighborLon, geohash.length))
                }
            }
        }
        
        return neighbors
    }
    
    /**
     * Get optimal geohash precision based on radius.
     * 
     * @param radiusMeters Search radius in meters
     * @return Optimal precision level for geohash
     */
    fun getOptimalPrecision(radiusMeters: Int): Int {
        return when {
            radiusMeters >= 20000 -> 4  // 20km+ -> ~39km precision
            radiusMeters >= 5000 -> 5   // 5-20km -> ~4.9km precision
            radiusMeters >= 1500 -> 6   // 1.5-5km -> ~1.2km precision
            radiusMeters >= 500 -> 7    // 500m-1.5km -> ~153m precision
            radiusMeters >= 100 -> 8    // 100-500m -> ~38m precision
            else -> 9                   // <100m -> ~9m precision
        }
    }

    /**
     * Get geohashes covering a radius around a location.
     * 
     * @param location Center location
     * @param radiusMeters Radius in meters
     * @param precision Geohash precision
     * @return List of geohashes covering the area
     */
    fun getGeoHashesForRadius(
        location: com.omerkaya.sperrmuellfinder.core.model.PostLocation,
        radiusMeters: Int,
        precision: Int
    ): List<String> {
        return getBoundingBoxHashes(
            location.latitude,
            location.longitude,
            radiusMeters.toDouble(),
            precision
        )
    }

    /**
     * Encode PostLocation to geohash.
     * 
     * @param location Location to encode
     * @param precision Geohash precision
     * @return Geohash string
     */
    fun encode(location: com.omerkaya.sperrmuellfinder.core.model.PostLocation, precision: Int): String {
        return encode(location.latitude, location.longitude, precision)
    }

    /**
     * Get approximate step size for geohash precision.
     */
    private fun getStepSize(precision: Int): Double {
        return when (precision) {
            1 -> 45.0      // ~5000km
            2 -> 11.25     // ~1250km
            3 -> 1.4       // ~156km
            4 -> 0.35      // ~39km
            5 -> 0.044     // ~4.9km
            6 -> 0.011     // ~1.2km
            7 -> 0.0014    // ~153m
            8 -> 0.00035   // ~38m
            else -> 0.00001 // ~1m
        }
    }

    /**
     * Data class for bounding box coordinates.
     */
    private data class BoundingBox(
        val minLat: Double,
        val maxLat: Double,
        val minLon: Double,
        val maxLon: Double
    )
}