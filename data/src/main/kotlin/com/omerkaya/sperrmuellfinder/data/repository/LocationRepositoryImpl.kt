package com.omerkaya.sperrmuellfinder.data.repository

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.data.datasource.GoogleMapsDataSource
import com.omerkaya.sperrmuellfinder.core.model.PostLocation
import com.omerkaya.sperrmuellfinder.domain.model.map.LocationBounds
import com.omerkaya.sperrmuellfinder.domain.repository.LocationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Implementation of LocationRepository using Google Maps and Play Services.
 * Handles GPS, permissions, geocoding, and location updates.
 * Rules.md compliant - Professional location management implementation.
 */
@Singleton
class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val googleMapsDataSource: GoogleMapsDataSource,
    private val logger: Logger
) : LocationRepository {

    private val settingsClient: SettingsClient by lazy {
        LocationServices.getSettingsClient(context)
    }

    override suspend fun getCurrentLocation(highAccuracy: Boolean): Result<PostLocation> {
        logger.d(Logger.TAG_DEFAULT, "LocationRepositoryImpl: Getting current location")
        return googleMapsDataSource.getCurrentLocation(highAccuracy)
    }

    override fun observeLocationUpdates(
        intervalMs: Long,
        highAccuracy: Boolean
    ): Flow<PostLocation> {
        logger.d(Logger.TAG_DEFAULT, "LocationRepositoryImpl: Starting location updates")
        return googleMapsDataSource.observeLocationUpdates(intervalMs, highAccuracy)
    }

    override suspend fun hasLocationPermissions(): Result<Boolean> {
        return try {
            val hasPermissions = googleMapsDataSource.hasLocationPermissions()
            logger.d(Logger.TAG_DEFAULT, "Location permissions status: $hasPermissions")
            Result.Success(hasPermissions)
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Failed to check location permissions", e)
            Result.Error(e)
        }
    }

    override suspend fun requestLocationPermissions(): Result<Boolean> {
        return try {
            // Note: Actual permission request should be handled by the UI layer
            // This method just checks current status
            val hasPermissions = googleMapsDataSource.hasLocationPermissions()
            logger.d(Logger.TAG_DEFAULT, "Location permissions request result: $hasPermissions")
            Result.Success(hasPermissions)
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Failed to request location permissions", e)
            Result.Error(e)
        }
    }

    override suspend fun isGpsEnabled(): Result<Boolean> {
        return try {
            val isEnabled = googleMapsDataSource.isGpsEnabled()
            logger.d(Logger.TAG_DEFAULT, "GPS enabled status: $isEnabled")
            Result.Success(isEnabled)
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Failed to check GPS status", e)
            Result.Error(e)
        }
    }

    override suspend fun requestGpsEnable(): Result<Boolean> {
        return try {
            val locationRequest = LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                interval = 10000
                fastestInterval = 5000
            }

            val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true)

            val settingsResponse = checkLocationSettings(builder.build())
            
            when (settingsResponse) {
                is Result.Success -> {
                    logger.i(Logger.TAG_DEFAULT, "GPS is already enabled")
                    Result.Success(true)
                }
                is Result.Error -> {
                    if (settingsResponse.exception is ResolvableApiException) {
                        // GPS can be enabled via settings dialog
                        logger.d(Logger.TAG_DEFAULT, "GPS can be enabled via settings dialog")
                        Result.Success(false) // UI layer should handle the resolution
                    } else {
                        logger.e(Logger.TAG_DEFAULT, "GPS cannot be enabled", settingsResponse.exception)
                        Result.Error(settingsResponse.exception)
                    }
                }
                is Result.Loading -> Result.Error(Exception("Unexpected loading state"))
            }
            
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Failed to request GPS enable", e)
            Result.Error(e)
        }
    }

    override suspend fun getAddressFromLocation(location: PostLocation): Result<String> {
        logger.d(Logger.TAG_DEFAULT, "Getting address for location: ${location.latitude}, ${location.longitude}")
        return googleMapsDataSource.getAddressFromLocation(location)
    }

    override suspend fun getLocationFromAddress(address: String): Result<PostLocation> {
        logger.d(Logger.TAG_DEFAULT, "Geocoding address: $address")
        return googleMapsDataSource.getLocationFromAddress(address)
    }

    override fun calculateDistance(from: PostLocation, to: PostLocation): Double {
        return googleMapsDataSource.calculateDistance(from, to)
    }

    override fun isLocationWithinRadius(
        center: PostLocation,
        target: PostLocation,
        radiusMeters: Int
    ): Boolean {
        return googleMapsDataSource.isLocationWithinRadius(center, target, radiusMeters.toDouble())
    }

    fun getViewportBounds(
        center: PostLocation,
        radiusMeters: Int
    ): LocationBounds {
        return googleMapsDataSource.getViewportBounds(center, radiusMeters)
    }

    override suspend fun stopLocationUpdates(): Result<Unit> {
        logger.d(Logger.TAG_DEFAULT, "Stopping location updates")
        return googleMapsDataSource.stopLocationUpdates()
    }

    override suspend fun getLocationFromCoordinates(
        latitude: Double,
        longitude: Double
    ): Result<PostLocation> {
        logger.d(Logger.TAG_DEFAULT, "Getting location from coordinates: $latitude, $longitude")
        return googleMapsDataSource.getLocationFromCoordinates(latitude, longitude)
    }

    override suspend fun updateCurrentLocation(location: PostLocation): Result<Unit> {
        return try {
            logger.d(Logger.TAG_DEFAULT, "Updating current location: ${location.latitude}, ${location.longitude}")
            
            // Update location in Google Maps data source
            googleMapsDataSource.updateCurrentLocation(location)
            
            logger.i(Logger.TAG_DEFAULT, "Current location updated successfully")
            Result.Success(Unit)
            
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Failed to update current location", e)
            Result.Error(e)
        }
    }

    /**
     * Check location settings using Google Play Services.
     */
    private suspend fun checkLocationSettings(
        locationSettingsRequest: LocationSettingsRequest
    ): Result<LocationSettingsResponse> {
        return suspendCancellableCoroutine { continuation ->
            val task: Task<LocationSettingsResponse> = settingsClient.checkLocationSettings(locationSettingsRequest)
            
            task.addOnSuccessListener { response ->
                logger.i(Logger.TAG_DEFAULT, "Location settings are satisfied")
                continuation.resume(Result.Success(response))
            }
            
            task.addOnFailureListener { exception ->
                logger.w(Logger.TAG_DEFAULT, "Location settings check failed", exception)
                continuation.resume(Result.Error(exception))
            }
        }
    }
}
