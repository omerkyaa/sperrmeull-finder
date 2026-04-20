package com.omerkaya.sperrmuellfinder.domain.repository

import com.omerkaya.sperrmuellfinder.core.model.PostLocation
import com.omerkaya.sperrmuellfinder.core.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for location operations
 * Rules.md compliant - Clean Architecture domain layer
 */
interface LocationRepository {
    /**
     * Get current device location with reverse geocoding
     * @param highAccuracy Whether to use high accuracy mode (for premium users)
     * @return Result containing Location with city and district info
     */
    suspend fun getCurrentLocation(highAccuracy: Boolean = false): Result<PostLocation>

    /**
     * Get location from coordinates with reverse geocoding
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @return Result containing Location with city and district info
     */
    suspend fun getLocationFromCoordinates(latitude: Double, longitude: Double): Result<PostLocation>

    /**
     * Observe location updates in real-time
     * @param intervalMs Update interval in milliseconds
     * @param highAccuracy Whether to use high accuracy mode
     * @return Flow of location updates
     */
    fun observeLocationUpdates(intervalMs: Long, highAccuracy: Boolean): Flow<PostLocation>

    /**
     * Stop observing location updates
     */
    suspend fun stopLocationUpdates(): Result<Unit>

    /**
     * Get address from location coordinates
     */
    suspend fun getAddressFromLocation(location: PostLocation): Result<String>

    /**
     * Get location from address string
     */
    suspend fun getLocationFromAddress(address: String): Result<PostLocation>

    /**
     * Calculate distance between two locations in meters
     */
    fun calculateDistance(from: PostLocation, to: PostLocation): Double

    /**
     * Check if target location is within radius of center location
     */
    fun isLocationWithinRadius(center: PostLocation, target: PostLocation, radiusMeters: Int): Boolean

    /**
     * Check if location permissions are granted
     */
    suspend fun hasLocationPermissions(): Result<Boolean>

    /**
     * Request location permissions from user
     */
    suspend fun requestLocationPermissions(): Result<Boolean>

    /**
     * Check if GPS is enabled
     */
    suspend fun isGpsEnabled(): Result<Boolean>

    /**
     * Request user to enable GPS
     */
    suspend fun requestGpsEnable(): Result<Boolean>

    /**
     * Update current location in repository
     * @param location New location to update
     * @return Result of update operation
     */
    suspend fun updateCurrentLocation(location: PostLocation): Result<Unit>
}