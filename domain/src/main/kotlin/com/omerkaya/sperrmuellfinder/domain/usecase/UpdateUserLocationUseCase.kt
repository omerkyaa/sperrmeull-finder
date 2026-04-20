package com.omerkaya.sperrmuellfinder.domain.usecase

import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.UserLocation
import com.omerkaya.sperrmuellfinder.domain.repository.LocationRepository
import com.omerkaya.sperrmuellfinder.domain.repository.MapRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Use case for updating user location and managing location-based features.
 * Handles location validation, caching, and notification subscriptions.
 */
class UpdateUserLocationUseCase @Inject constructor(
    private val mapRepository: MapRepository,
    private val locationRepository: LocationRepository,
    private val logger: Logger
) {
    
    /**
     * Update user location with validation and caching.
     * 
     * @param userLocation New user location
     * @return Result of location update operation
     */
    suspend operator fun invoke(userLocation: UserLocation): Result<Unit> {
        return try {
            logger.d(Logger.TAG_DEFAULT, "Updating user location: ${userLocation.latitude}, ${userLocation.longitude}")
            
            // Validate location
            val validationResult = validateLocation(userLocation)
            if (validationResult is Result.Error) {
                return validationResult
            }
            
            // Update location in repository
            val updateResult = mapRepository.updateUserLocation(userLocation)
            if (updateResult is Result.Error) {
                logger.e(Logger.TAG_DEFAULT, "Failed to update location in map repository", updateResult.exception)
                return updateResult
            }
            
            // Update location in location repository for consistency
            val postLocation = userLocation.toPostLocation()
            val locationUpdateResult = locationRepository.updateCurrentLocation(postLocation)
            if (locationUpdateResult is Result.Error) {
                logger.w(Logger.TAG_DEFAULT, "Failed to update location in location repository, but continuing", locationUpdateResult.exception)
                // Don't fail the entire operation, just log the warning
            }
            
            logger.i(Logger.TAG_DEFAULT, "User location updated successfully")
            Result.Success(Unit)
            
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Error updating user location", e)
            Result.Error(e)
        }
    }
    
    /**
     * Get current user location with fallback handling.
     * 
     * @return Flow of current user location
     */
    fun getCurrentLocation(): Flow<Result<UserLocation>> = flow {
        try {
            logger.d(Logger.TAG_DEFAULT, "Getting current user location")
            
            // Try to get last known location first
            val lastKnownResult = mapRepository.getLastKnownLocation()
            
            if (lastKnownResult is Result.Success && lastKnownResult.data.isCurrent) {
                logger.d(Logger.TAG_DEFAULT, "Using cached current location")
                emit(Result.Success(lastKnownResult.data))
                return@flow
            }
            
            // Get fresh location from location repository
            val locationResult = locationRepository.getCurrentLocation()
            
            // Handle location result with explicit type checking
            if (locationResult is Result.Success) {
                val postLocation = locationResult.data
                val userLocation = UserLocation.fromPostLocation(
                    postLocation,
                    accuracy = 50f // Default accuracy
                )
                
                // Update location in map repository
                mapRepository.updateUserLocation(userLocation)
                
                emit(Result.Success(userLocation))
            } else if (locationResult is Result.Error) {
                logger.w(Logger.TAG_DEFAULT, "Failed to get current location, using default", locationResult.exception)
                emit(Result.Success(UserLocation.defaultLocation()))
            } else {
                logger.w(Logger.TAG_DEFAULT, "Unexpected location result type, using default")
                emit(Result.Success(UserLocation.defaultLocation()))
            }
            
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Error getting current location", e)
            emit(Result.Error(e))
        }
    }.catch { exception ->
        logger.e(Logger.TAG_DEFAULT, "Flow error in getCurrentLocation", exception)
        emit(Result.Error(exception))
    }
    
    /**
     * Subscribe to location-based notifications for premium users.
     * 
     * @param favoriteLocations List of favorite locations
     * @param categories Favorite categories
     * @return Result of subscription operation
     */
    suspend fun subscribeToLocationNotifications(
        favoriteLocations: List<com.omerkaya.sperrmuellfinder.core.model.PostLocation>,
        categories: List<String>
    ): Result<Unit> {
        return try {
            logger.d(Logger.TAG_DEFAULT, "Subscribing to location notifications: ${favoriteLocations.size} locations, ${categories.size} categories")
            
            val result = mapRepository.subscribeToLocationNotifications(favoriteLocations, categories)
            
            if (result is Result.Success) {
                logger.i(Logger.TAG_DEFAULT, "Successfully subscribed to location notifications")
            } else if (result is Result.Error) {
                logger.w(Logger.TAG_DEFAULT, "Failed to subscribe to location notifications", result.exception)
            } else {
                logger.w(Logger.TAG_DEFAULT, "Unexpected result type from location notification subscription")
            }
            
            result
            
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Error subscribing to location notifications", e)
            Result.Error(e)
        }
    }
    
    /**
     * Unsubscribe from location-based notifications.
     * 
     * @return Result of unsubscription operation
     */
    suspend fun unsubscribeFromLocationNotifications(): Result<Unit> {
        return try {
            logger.d(Logger.TAG_DEFAULT, "Unsubscribing from location notifications")
            
            val result = mapRepository.unsubscribeFromLocationNotifications()
            
            if (result is Result.Success) {
                logger.i(Logger.TAG_DEFAULT, "Successfully unsubscribed from location notifications")
            } else if (result is Result.Error) {
                logger.w(Logger.TAG_DEFAULT, "Failed to unsubscribe from location notifications", result.exception)
            } else {
                logger.w(Logger.TAG_DEFAULT, "Unexpected result type from location notification unsubscription")
            }
            
            result
            
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Error unsubscribing from location notifications", e)
            Result.Error(e)
        }
    }
    
    /**
     * Validate user location for accuracy and reliability.
     */
    private fun validateLocation(userLocation: UserLocation): Result<Unit> {
        return when {
            // Check if coordinates are valid
            userLocation.latitude < -90 || userLocation.latitude > 90 -> {
                Result.Error(IllegalArgumentException("Invalid latitude: ${userLocation.latitude}"))
            }
            userLocation.longitude < -180 || userLocation.longitude > 180 -> {
                Result.Error(IllegalArgumentException("Invalid longitude: ${userLocation.longitude}"))
            }
            // Check if location is too inaccurate
            userLocation.accuracy > 1000f -> {
                logger.w(Logger.TAG_DEFAULT, "Location accuracy is poor: ${userLocation.accuracy}m")
                Result.Success(Unit) // Allow but warn
            }
            // Check if location is too old
            !userLocation.isCurrent -> {
                logger.w(Logger.TAG_DEFAULT, "Location is outdated: ${userLocation.timestamp}")
                Result.Success(Unit) // Allow but warn
            }
            else -> {
                Result.Success(Unit)
            }
        }
    }
    
    /**
     * Check if location has good enough accuracy for map operations.
     */
    fun isLocationAccurate(userLocation: UserLocation): Boolean {
        return userLocation.hasGoodAccuracy && userLocation.isCurrent
    }
    
    /**
     * Get formatted location info for debugging.
     */
    fun getLocationInfo(userLocation: UserLocation): String {
        return "Location: ${userLocation.latitude}, ${userLocation.longitude} " +
                "(±${userLocation.accuracy}m, ${userLocation.provider}, " +
                "${if (userLocation.isCurrent) "current" else "outdated"})"
    }
}
