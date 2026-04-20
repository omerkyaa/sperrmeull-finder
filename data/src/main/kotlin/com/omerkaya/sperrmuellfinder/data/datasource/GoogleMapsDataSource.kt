package com.omerkaya.sperrmuellfinder.data.datasource

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.core.util.GoogleServicesChecker
import com.omerkaya.sperrmuellfinder.core.model.PostLocation
import com.omerkaya.sperrmuellfinder.core.util.GeoHashUtils
import com.omerkaya.sperrmuellfinder.domain.model.UserLocation
import com.omerkaya.sperrmuellfinder.domain.model.LocationProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Enhanced data source for Google Maps and location services integration.
 * Handles GPS, permissions, geocoding, location updates, and geohash calculations.
 * Rules.md compliant - Professional Google Play Services integration with caching.
 */
@Singleton
class GoogleMapsDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: Logger,
    private val googleServicesChecker: GoogleServicesChecker
) {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        try {
            LocationServices.getFusedLocationProviderClient(context)
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Failed to initialize FusedLocationProviderClient - emulator issue", e)
            // Return a mock client or handle gracefully
            LocationServices.getFusedLocationProviderClient(context)
        }
    }

    private val geocoder: Geocoder by lazy {
        Geocoder(context, Locale.getDefault())
    }

    private val locationManager: LocationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private var locationCallback: LocationCallback? = null
    
    // Location caching
    private val locationCache = ConcurrentHashMap<String, UserLocation>()
    private val addressCache = ConcurrentHashMap<String, String>()
    private val cacheTimestamps = ConcurrentHashMap<String, Long>()
    private val cacheExpiryMs = 5 * 60 * 1000L // 5 minutes
    
    // Current location state
    private var currentLocation: UserLocation? = null

    /**
     * Get current device location using Google Play Services.
     * 
     * @param highAccuracy Whether to use high accuracy GPS (Premium feature)
     * @return Result containing current location or error
     */
    suspend fun getCurrentLocation(highAccuracy: Boolean = false): Result<PostLocation> {
        logger.d(Logger.TAG_DEFAULT, "Getting current location (highAccuracy: $highAccuracy)")

        // CRASH FIX: Check Google Services availability first
        if (!googleServicesChecker.canUseGoogleServicesSafely()) {
            val statusMessage = googleServicesChecker.getStatusMessage()
            logger.w(Logger.TAG_DEFAULT, "Google Services not available: $statusMessage")
            return Result.Error(Exception("Google Services unavailable: $statusMessage"))
        }

        if (!hasLocationPermissions()) {
            return Result.Error(Exception("Location permissions not granted"))
        }

        if (!isGpsEnabled()) {
            return Result.Error(Exception("GPS is not enabled"))
        }

        return try {
            val locationRequest = createLocationRequest(highAccuracy, oneTime = true)
            val location = getCurrentLocationInternal(locationRequest, highAccuracy)
            
            val postLocation = PostLocation(
                latitude = location.latitude,
                longitude = location.longitude,
                address = getAddressFromCoordinates(location.latitude, location.longitude)
            )
            
            logger.i(Logger.TAG_DEFAULT, "Current location obtained: ${postLocation.latitude}, ${postLocation.longitude}")
            Result.Success(postLocation)
            
        } catch (e: SecurityException) {
            logger.e(Logger.TAG_DEFAULT, "SecurityException getting location - Google Services issue", e)
            Result.Error(Exception("Security error accessing location services: ${e.message}"))
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Failed to get current location", e)
            Result.Error(e)
        }
    }

    /**
     * Observe location updates in real-time.
     * 
     * @param intervalMs Location update interval in milliseconds
     * @param highAccuracy Whether to use high accuracy GPS
     * @return Flow of location updates
     */
    fun observeLocationUpdates(
        intervalMs: Long = 30000,
        highAccuracy: Boolean = false
    ): Flow<PostLocation> = callbackFlow {
        logger.d(Logger.TAG_DEFAULT, "Starting location updates (interval: ${intervalMs}ms, highAccuracy: $highAccuracy)")

        // CRASH FIX: Check Google Services availability for location updates
        if (!googleServicesChecker.canUseGoogleServicesSafely()) {
            val statusMessage = googleServicesChecker.getStatusMessage()
            logger.w(Logger.TAG_DEFAULT, "Google Services not available for location updates: $statusMessage")
            close(Exception("Google Services unavailable: $statusMessage"))
            return@callbackFlow
        }

        if (!hasLocationPermissions()) {
            close(Exception("Location permissions not granted"))
            return@callbackFlow
        }

        if (!isGpsEnabled()) {
            close(Exception("GPS is not enabled"))
            return@callbackFlow
        }

        val locationRequest = createLocationRequest(highAccuracy, intervalMs)
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val postLocation = PostLocation(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        address = getAddressFromCoordinatesSync(location.latitude, location.longitude)
                    )
                    
                    logger.d(Logger.TAG_DEFAULT, "Location update: ${postLocation.latitude}, ${postLocation.longitude}")
                    trySend(postLocation)
                }
            }

            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                if (!locationAvailability.isLocationAvailable) {
                    logger.w(Logger.TAG_DEFAULT, "Location became unavailable")
                }
            }
        }

        try {
            @Suppress("MissingPermission")
            val locationTask = fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            
            locationTask?.addOnFailureListener { exception ->
                logger.e(Logger.TAG_LOCATION, "Failed to request location updates", exception)
                close(exception)
            }
            
            locationTask?.addOnSuccessListener {
                logger.d(Logger.TAG_LOCATION, "Location updates started successfully")
            }
            
        } catch (e: SecurityException) {
            logger.e(Logger.TAG_LOCATION, "Security exception in location updates", e)
            close(e)
            return@callbackFlow
        } catch (e: Exception) {
            logger.e(Logger.TAG_LOCATION, "Unexpected error in location updates", e)
            close(e)
            return@callbackFlow
        }

        awaitClose {
            logger.d(Logger.TAG_DEFAULT, "Stopping location updates")
            locationCallback?.let { callback ->
                fusedLocationClient.removeLocationUpdates(callback)
            }
            locationCallback = null
        }
    }

    /**
     * Stop location updates to save battery.
     */
    suspend fun stopLocationUpdates(): Result<Unit> {
        return try {
            locationCallback?.let { callback ->
                fusedLocationClient.removeLocationUpdates(callback)
                locationCallback = null
                logger.i(Logger.TAG_DEFAULT, "Location updates stopped")
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Failed to stop location updates", e)
            Result.Error(e)
        }
    }

    /**
     * Check if location permissions are granted.
     */
    fun hasLocationPermissions(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation && coarseLocation
    }

    /**
     * Check if GPS is enabled on device.
     */
    fun isGpsEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Get address from coordinates (reverse geocoding).
     */
    suspend fun getAddressFromLocation(location: PostLocation): Result<String> {
        return try {
            val address = getAddressFromCoordinates(location.latitude, location.longitude)
            Result.Success(address)
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Failed to get address from location", e)
            Result.Error(e)
        }
    }

    /**
     * Get coordinates from address (geocoding).
     */
    suspend fun getLocationFromAddress(address: String): Result<PostLocation> {
        return try {
            if (!Geocoder.isPresent()) {
                return Result.Error(Exception("Geocoder not available"))
            }

            val addresses = geocoder.getFromLocationName(address, 1)
            if (addresses.isNullOrEmpty()) {
                return Result.Error(Exception("Address not found"))
            }

            val location = addresses.first()
            val postLocation = PostLocation(
                latitude = location.latitude,
                longitude = location.longitude,
                address = address
            )

            logger.i(Logger.TAG_DEFAULT, "Geocoded address '$address' to: ${postLocation.latitude}, ${postLocation.longitude}")
            Result.Success(postLocation)
            
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Failed to geocode address: $address", e)
            Result.Error(e)
        }
    }

    // Note: calculateDistance and isLocationWithinRadius methods are implemented below with enhanced signatures

    /**
     * Get viewport bounds for map display.
     * Uses simplified calculation for approximate bounds.
     */
    fun getViewportBounds(
        center: PostLocation,
        radiusMeters: Int
    ): com.omerkaya.sperrmuellfinder.domain.model.map.LocationBounds {
        // Simplified calculation: 1 degree ≈ 111km
        val latOffset = radiusMeters / 111000.0 // Approximate meters per degree latitude
        val lngOffset = radiusMeters / 111000.0 // Simplified longitude offset (not accounting for latitude)
        
        val northeast = PostLocation(
            latitude = center.latitude + latOffset,
            longitude = center.longitude + lngOffset,
            address = "NE"
        )
        
        val southwest = PostLocation(
            latitude = center.latitude - latOffset,
            longitude = center.longitude - lngOffset,
            address = "SW"
        )
        
        return com.omerkaya.sperrmuellfinder.domain.model.map.LocationBounds(northeast, southwest)
    }

    /**
     * Create location request with specified parameters.
     */
    private fun createLocationRequest(
        highAccuracy: Boolean,
        intervalMs: Long = 30000,
        oneTime: Boolean = false
    ): LocationRequest {
        val priority = if (highAccuracy) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        val builder = LocationRequest.Builder(priority, intervalMs).apply {
            setMinUpdateIntervalMillis(intervalMs / 2)
            setMaxUpdateDelayMillis(intervalMs * 2)
            
            if (oneTime) {
                setMaxUpdates(1)
            }
        }

        return builder.build()
    }

    /**
     * Get current location using suspending function.
     */
    private suspend fun getCurrentLocationInternal(locationRequest: LocationRequest, highAccuracy: Boolean = false): android.location.Location {
        return suspendCancellableCoroutine { continuation ->
            try {
                // Use the modern getCurrentLocation API with CancellationToken
                val cancellationTokenSource = CancellationTokenSource()
                
                val priority = if (highAccuracy) {
                    Priority.PRIORITY_HIGH_ACCURACY
                } else {
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY
                }
                
                @Suppress("MissingPermission")
                val task: Task<android.location.Location> = fusedLocationClient.getCurrentLocation(
                    priority,
                    cancellationTokenSource.token
                )
                
                task.addOnSuccessListener { location ->
                    if (location != null) {
                        logger.d(Logger.TAG_LOCATION, "Current location obtained successfully")
                        continuation.resume(location)
                    } else {
                        logger.w(Logger.TAG_LOCATION, "Current location is null, using fallback")
                        continuation.resume(
                            android.location.Location("fallback").apply {
                                latitude = 52.5200 // Berlin fallback
                                longitude = 13.4050
                            }
                        )
                    }
                }
                
                task.addOnFailureListener { exception ->
                    logger.e(Logger.TAG_LOCATION, "Failed to get current location", exception)
                    if (continuation.isActive) {
                        continuation.resume(
                            android.location.Location("fallback").apply {
                                latitude = 52.5200 // Berlin fallback
                                longitude = 13.4050
                            }
                        )
                    }
                }
                
                // Handle cancellation
                continuation.invokeOnCancellation {
                    logger.d(Logger.TAG_LOCATION, "Location request cancelled")
                    cancellationTokenSource.cancel()
                }
                
            } catch (e: SecurityException) {
                logger.e(Logger.TAG_LOCATION, "Security exception getting current location", e)
                if (continuation.isActive) {
                    continuation.resume(
                        android.location.Location("error").apply {
                            latitude = 52.5200 // Berlin fallback
                            longitude = 13.4050
                        }
                    )
                }
            }
        }
    }

    /**
     * Get enhanced current location with UserLocation details.
     * 
     * @param highAccuracy Whether to use high accuracy GPS
     * @return Result containing UserLocation with accuracy and timestamp
     */
    suspend fun getCurrentUserLocation(highAccuracy: Boolean = false): Result<UserLocation> {
        return withContext(Dispatchers.IO) {
            try {
                logger.d(Logger.TAG_DEFAULT, "Getting current user location with details")
                
                // Check cache first
                val cacheKey = "current_user_location"
                val cachedLocation = getCachedUserLocation(cacheKey)
                if (cachedLocation != null && cachedLocation.isCurrent) {
                    logger.d(Logger.TAG_DEFAULT, "Using cached user location")
                    return@withContext Result.Success(cachedLocation)
                }
                
                // Check Google Services availability
                if (!googleServicesChecker.canUseGoogleServicesSafely()) {
                    logger.w(Logger.TAG_DEFAULT, "Google Services not available, using default location")
                    return@withContext Result.Success(UserLocation.defaultLocation())
                }
                
                if (!hasLocationPermissions()) {
                    return@withContext Result.Error(Exception("Location permissions not granted"))
                }
                
                // Get location with details
                val locationRequest = createLocationRequest(highAccuracy, oneTime = true)
                val location = getCurrentLocationInternal(locationRequest, highAccuracy)
                
                val userLocation = UserLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy,
                    timestamp = Date(), // Simple Date() - always works on Android
                    provider = when (location.provider) {
                        "gps" -> LocationProvider.GPS
                        "network" -> LocationProvider.NETWORK
                        "fused" -> LocationProvider.FUSED
                        "passive" -> LocationProvider.PASSIVE
                        else -> LocationProvider.FUSED
                    }
                )
                
                // Cache the location
                cacheUserLocation(cacheKey, userLocation)
                currentLocation = userLocation
                
                logger.i(Logger.TAG_DEFAULT, "Current user location obtained: ${userLocation.latitude}, ${userLocation.longitude} (±${userLocation.accuracy}m)")
                Result.Success(userLocation)
                
            } catch (e: SecurityException) {
                logger.e(Logger.TAG_DEFAULT, "SecurityException getting user location", e)
                Result.Error(Exception("Security error accessing location services: ${e.message}"))
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "Failed to get current user location", e)
                Result.Success(UserLocation.defaultLocation()) // Fallback to default
            }
        }
    }
    
    /**
     * Update current location in cache.
     * 
     * @param location New location to cache
     */
    fun updateCurrentLocation(location: PostLocation) {
        try {
            val userLocation = UserLocation.fromPostLocation(location)
            currentLocation = userLocation
            cacheUserLocation("current_user_location", userLocation)
            logger.d(Logger.TAG_DEFAULT, "Current location updated in cache")
        } catch (e: Exception) {
            logger.w(Logger.TAG_DEFAULT, "Failed to update current location", e)
        }
    }
    
    /**
     * Get geohash for location with specified precision.
     * 
     * @param location Location to encode
     * @param precision Geohash precision (default: 7)
     * @return Geohash string
     */
    fun getGeohash(location: PostLocation, precision: Int = 7): String {
        return GeoHashUtils.encode(location.latitude, location.longitude, precision)
    }
    
    /**
     * Get multiple geohashes for area coverage.
     * 
     * @param centerLocation Center location
     * @param radiusMeters Radius in meters
     * @param precision Geohash precision
     * @return List of geohashes covering the area
     */
    fun getAreaGeohashes(
        centerLocation: PostLocation,
        radiusMeters: Double,
        precision: Int = 6
    ): List<String> {
        return GeoHashUtils.getBoundingBoxHashes(
            centerLocation.latitude,
            centerLocation.longitude,
            radiusMeters,
            precision
        )
    }
    
    /**
     * Calculate distance between two locations.
     * 
     * @param from Starting location
     * @param to Destination location
     * @return Distance in meters
     */
    fun calculateDistance(from: PostLocation, to: PostLocation): Double {
        return GeoHashUtils.calculateDistance(
            from.latitude, from.longitude,
            to.latitude, to.longitude
        )
    }
    
    /**
     * Check if location is within radius of another location.
     * 
     * @param center Center location
     * @param target Target location
     * @param radiusMeters Radius in meters
     * @return True if target is within radius
     */
    fun isLocationWithinRadius(
        center: PostLocation,
        target: PostLocation,
        radiusMeters: Double
    ): Boolean {
        return GeoHashUtils.isWithinRadius(
            center.latitude, center.longitude,
            target.latitude, target.longitude,
            radiusMeters
        )
    }
    
    /**
     * Clear location and address caches.
     */
    fun clearCache() {
        locationCache.clear()
        addressCache.clear()
        cacheTimestamps.clear()
        logger.i(Logger.TAG_DEFAULT, "Location cache cleared")
    }
    
    // Private helper methods
    
    // Note: Timestamp methods removed - now using simple Date() constructor
    // which always works on all Android versions without any compatibility issues.
    
    private fun cacheUserLocation(key: String, location: UserLocation) {
        locationCache[key] = location
        cacheTimestamps[key] = System.currentTimeMillis()
    }
    
    private fun getCachedUserLocation(key: String): UserLocation? {
        val cacheTime = cacheTimestamps[key] ?: return null
        return if (System.currentTimeMillis() - cacheTime < cacheExpiryMs) {
            locationCache[key]
        } else {
            locationCache.remove(key)
            cacheTimestamps.remove(key)
            null
        }
    }
    
    private fun cacheAddress(key: String, address: String) {
        addressCache[key] = address
        cacheTimestamps[key] = System.currentTimeMillis()
    }
    
    private fun getCachedAddress(key: String): String? {
        val cacheTime = cacheTimestamps[key] ?: return null
        return if (System.currentTimeMillis() - cacheTime < cacheExpiryMs) {
            addressCache[key]
        } else {
            addressCache.remove(key)
            cacheTimestamps.remove(key)
            null
        }
    }

    /**
     * Get address string from coordinates synchronously (for callbacks).
     */
    private fun getAddressFromCoordinatesSync(latitude: Double, longitude: Double): String {
        return try {
            val cacheKey = "address_${latitude}_${longitude}"
            val cachedAddress = getCachedAddress(cacheKey)
            if (cachedAddress != null) {
                return cachedAddress
            }
            
            if (!Geocoder.isPresent()) {
                return "Location: $latitude, $longitude"
            }

            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
            val address = if (addresses.isNullOrEmpty()) {
                "Location: $latitude, $longitude"
            } else {
                val addr = addresses.first()
                buildString {
                    addr.thoroughfare?.let { append("$it ") }
                    addr.locality?.let { append("$it ") }
                    addr.adminArea?.let { append("$it ") }
                    addr.countryName?.let { append(it) }
                }.trim().ifEmpty { "Location: $latitude, $longitude" }
            }
            
            // Cache the address
            cacheAddress(cacheKey, address)
            address
            
        } catch (e: Exception) {
            logger.w(Logger.TAG_DEFAULT, "Failed to get address for coordinates", e)
            "Location: $latitude, $longitude"
        }
    }

    /**
     * Get address string from coordinates with caching (async version).
     */
    /**
     * Get location from coordinates with reverse geocoding.
     */
    suspend fun getLocationFromCoordinates(latitude: Double, longitude: Double): Result<PostLocation> {
        return try {
            val address = getAddressFromCoordinates(latitude, longitude)
            val postLocation = PostLocation(
                latitude = latitude,
                longitude = longitude,
                address = address,
                city = null,
                country = null
            )
            Result.Success(postLocation)
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Failed to get location from coordinates", e)
            Result.Error(e)
        }
    }

    private suspend fun getAddressFromCoordinates(latitude: Double, longitude: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val cacheKey = "address_${latitude}_${longitude}"
                val cachedAddress = getCachedAddress(cacheKey)
                if (cachedAddress != null) {
                    return@withContext cachedAddress
                }
                
                if (!Geocoder.isPresent()) {
                    return@withContext "Location: $latitude, $longitude"
                }

                val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
                val address = if (addresses.isNullOrEmpty()) {
                    "Location: $latitude, $longitude"
                } else {
                    val addr = addresses.first()
                    buildString {
                        addr.thoroughfare?.let { append("$it ") }
                        addr.locality?.let { append("$it ") }
                        addr.adminArea?.let { append("$it ") }
                        addr.countryName?.let { append(it) }
                    }.trim().ifEmpty { "Location: $latitude, $longitude" }
                }
                
                // Cache the address
                cacheAddress(cacheKey, address)
                address
            
        } catch (e: Exception) {
            logger.w(Logger.TAG_DEFAULT, "Failed to get address for coordinates", e)
            "Location: $latitude, $longitude"
            }
        }
    }
}
