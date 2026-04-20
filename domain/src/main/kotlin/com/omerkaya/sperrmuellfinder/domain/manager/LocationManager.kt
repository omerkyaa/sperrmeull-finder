package com.omerkaya.sperrmuellfinder.domain.manager

import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.core.model.PostLocation
import com.omerkaya.sperrmuellfinder.domain.repository.LocationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for GPS and location-related operations.
 * Handles permission requests, location updates, and location state management.
 * Rules.md compliant - Professional location management with premium features.
 */
@Singleton
class LocationManager @Inject constructor(
    private val locationRepository: LocationRepository,
    private val coroutineScope: CoroutineScope,
    private val logger: Logger
) {

    private val _currentLocation: MutableStateFlow<PostLocation?> = MutableStateFlow(null)
    val currentLocation: StateFlow<PostLocation?> = _currentLocation.asStateFlow()

    private val _hasLocationPermissions: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val hasLocationPermissions: StateFlow<Boolean> = _hasLocationPermissions.asStateFlow()

    private val _isGpsEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isGpsEnabled: StateFlow<Boolean> = _isGpsEnabled.asStateFlow()

    private val _locationError: MutableStateFlow<String?> = MutableStateFlow(null)
    val locationError: StateFlow<String?> = _locationError.asStateFlow()

    private val _isLocationLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isLocationLoading: StateFlow<Boolean> = _isLocationLoading.asStateFlow()

    private var isObservingLocation = false

    init {
        logger.d(Logger.TAG_DEFAULT, "LocationManager initialized")
        
        // Check initial permissions and GPS status
        coroutineScope.launch {
            val result = checkPermissionsAndGps()
            if (result is Result.Error) {
                logger.w(Logger.TAG_DEFAULT, "Initial permission/GPS check failed", result.exception)
            }
        }
    }

    /**
     * Initialize location services and start observing location updates.
     * Requests permissions if needed and starts location updates.
     */
    suspend fun initializeLocation(): Result<Unit> {
        logger.d(Logger.TAG_DEFAULT, "Initializing location services")
        
        try {
            _isLocationLoading.value = true
            _locationError.value = null

            // Check and request permissions
            val permissionsResult = ensureLocationPermissions()
            if (permissionsResult is Result.Error) {
                _locationError.value = "Location permissions required"
                return permissionsResult
            }

            // Check and request GPS
            val gpsResult = ensureGpsEnabled()
            if (gpsResult is Result.Error) {
                _locationError.value = "GPS must be enabled"
                return gpsResult
            }

            // Get initial location
            val locationResult = getCurrentLocation()
            if (locationResult is Result.Error) {
                _locationError.value = "Failed to get current location"
                return locationResult
            }

            // Start observing location updates
            startLocationUpdates()

            logger.i(Logger.TAG_DEFAULT, "Location services initialized successfully")
            return Result.Success(Unit)
            
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Failed to initialize location services", e)
            _locationError.value = e.message ?: "Location initialization failed"
            return Result.Error(e)
        } finally {
            _isLocationLoading.value = false
        }
    }

    /**
     * Get current device location once.
     * Uses high accuracy for premium users.
     */
    suspend fun getCurrentLocation(highAccuracy: Boolean = false): Result<PostLocation> {
        logger.d(Logger.TAG_DEFAULT, "Getting current location (highAccuracy: $highAccuracy)")
        
        if (!_hasLocationPermissions.value) {
            return Result.Error(Exception("Location permissions not granted"))
        }

        if (!_isGpsEnabled.value) {
            return Result.Error(Exception("GPS is not enabled"))
        }

        return when (val result = locationRepository.getCurrentLocation(highAccuracy)) {
            is Result.Success -> {
                _currentLocation.value = result.data
                _locationError.value = null
                logger.i(Logger.TAG_DEFAULT, "Current location: ${result.data.latitude}, ${result.data.longitude}")
                Result.Success(result.data)
            }
            is Result.Error -> {
                logger.e(Logger.TAG_DEFAULT, "Failed to get current location", result.exception)
                _locationError.value = result.exception.message
                Result.Error(result.exception)
            }
            is Result.Loading -> Result.Loading
        }
    }

    /**
     * Start observing location updates in real-time.
     * Updates are more frequent for premium users.
     */
    fun startLocationUpdates(
        intervalMs: Long = 30000, // 30 seconds default
        highAccuracy: Boolean = false
    ) {
        if (isObservingLocation) {
            logger.d(Logger.TAG_DEFAULT, "Location updates already active")
            return
        }

        if (!_hasLocationPermissions.value || !_isGpsEnabled.value) {
            logger.w(Logger.TAG_DEFAULT, "Cannot start location updates - permissions or GPS not available")
            return
        }

        logger.d(Logger.TAG_DEFAULT, "Starting location updates (interval: ${intervalMs}ms, highAccuracy: $highAccuracy)")
        
        isObservingLocation = true
        coroutineScope.launch {
            try {
                val locationFlow = locationRepository.observeLocationUpdates(intervalMs, highAccuracy)
                locationFlow.collect { location ->
                    _currentLocation.value = location
                    _locationError.value = null
                    logger.d(Logger.TAG_DEFAULT, "Location updated: ${location.latitude}, ${location.longitude}")
                }
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "Location updates failed", e)
                _locationError.value = e.message
                isObservingLocation = false
            }
        }
    }

    /**
     * Stop location updates to save battery.
     */
    suspend fun stopLocationUpdates(): Result<Unit> {
        if (!isObservingLocation) return Result.Success(Unit)
        
        logger.d(Logger.TAG_DEFAULT, "Stopping location updates")
        isObservingLocation = false
        
        return when (val result = locationRepository.stopLocationUpdates()) {
            is Result.Success -> {
                logger.i(Logger.TAG_DEFAULT, "Location updates stopped successfully")
                Result.Success(Unit)
            }
            is Result.Error -> {
                logger.e(Logger.TAG_DEFAULT, "Failed to stop location updates", result.exception)
                Result.Error(result.exception)
            }
            is Result.Loading -> Result.Loading
        }
    }

    /**
     * Request location permissions from user.
     */
    suspend fun requestLocationPermissions(): Result<Boolean> {
        logger.d(Logger.TAG_DEFAULT, "Requesting location permissions")
        
        return when (val result = locationRepository.requestLocationPermissions()) {
            is Result.Success -> {
                _hasLocationPermissions.value = result.data
                if (result.data) {
                    logger.i(Logger.TAG_DEFAULT, "Location permissions granted")
                    _locationError.value = null
                } else {
                    logger.w(Logger.TAG_DEFAULT, "Location permissions denied")
                    _locationError.value = "Location permissions denied"
                }
                result
            }
            is Result.Error -> {
                logger.e(Logger.TAG_DEFAULT, "Failed to request location permissions", result.exception)
                _locationError.value = result.exception.message
                result
            }
            is Result.Loading -> result
        }
    }

    /**
     * Request user to enable GPS.
     */
    suspend fun requestGpsEnable(): Result<Boolean> {
        logger.d(Logger.TAG_DEFAULT, "Requesting GPS enable")
        
        return when (val result = locationRepository.requestGpsEnable()) {
            is Result.Success -> {
                _isGpsEnabled.value = result.data
                if (result.data) {
                    logger.i(Logger.TAG_DEFAULT, "GPS enabled")
                    _locationError.value = null
                } else {
                    logger.w(Logger.TAG_DEFAULT, "GPS enable request denied")
                    _locationError.value = "GPS must be enabled"
                }
                result
            }
            is Result.Error -> {
                logger.e(Logger.TAG_DEFAULT, "Failed to request GPS enable", result.exception)
                _locationError.value = result.exception.message
                result
            }
            is Result.Loading -> result
        }
    }

    /**
     * Get address from coordinates (reverse geocoding).
     */
    suspend fun getAddressFromLocation(location: PostLocation): Result<String> {
        return locationRepository.getAddressFromLocation(location)
    }

    /**
     * Get coordinates from address (geocoding).
     */
    suspend fun getLocationFromAddress(address: String): Result<PostLocation> {
        return locationRepository.getLocationFromAddress(address)
    }

    /**
     * Calculate distance between two locations.
     */
    fun calculateDistance(from: PostLocation, to: PostLocation): Double {
        return locationRepository.calculateDistance(from, to)
    }

    /**
     * Check if location is within radius.
     */
    fun isLocationWithinRadius(
        center: PostLocation,
        target: PostLocation,
        radiusMeters: Int
    ): Boolean {
        return locationRepository.isLocationWithinRadius(center, target, radiusMeters)
    }

    /**
     * Clear location error state.
     */
    fun clearLocationError() {
        _locationError.value = null
    }

    /**
     * Check permissions and GPS status.
     */
    private suspend fun checkPermissionsAndGps(): Result<Unit> {
        // Check permissions
        val permissionsResult = when (val result = locationRepository.hasLocationPermissions()) {
            is Result.Success -> {
                _hasLocationPermissions.value = result.data
                Result.Success(Unit)
            }
            is Result.Error -> {
                logger.e(Logger.TAG_DEFAULT, "Failed to check location permissions", result.exception)
                _hasLocationPermissions.value = false
                Result.Error(result.exception)
            }
            is Result.Loading -> Result.Loading
        }

        if (permissionsResult is Result.Error) {
            return permissionsResult
        }

        // Check GPS
        return when (val result = locationRepository.isGpsEnabled()) {
            is Result.Success -> {
                _isGpsEnabled.value = result.data
                Result.Success(Unit)
            }
            is Result.Error -> {
                logger.e(Logger.TAG_DEFAULT, "Failed to check GPS status", result.exception)
                _isGpsEnabled.value = false
                Result.Error(result.exception)
            }
            is Result.Loading -> Result.Loading
        }
    }

    /**
     * Ensure location permissions are granted.
     */
    private suspend fun ensureLocationPermissions(): Result<Unit> {
        if (_hasLocationPermissions.value) {
            return Result.Success(Unit)
        }

        return when (val result = requestLocationPermissions()) {
            is Result.Success -> {
                if (result.data) {
                    Result.Success(Unit)
                } else {
                    Result.Error(Exception("Location permissions denied"))
                }
            }
            is Result.Error -> result.let { Result.Error(it.exception) }
            is Result.Loading -> Result.Error(Exception("Permission request in progress"))
        }
    }

    /**
     * Ensure GPS is enabled.
     */
    private suspend fun ensureGpsEnabled(): Result<Unit> {
        if (_isGpsEnabled.value) {
            return Result.Success(Unit)
        }

        return when (val result = requestGpsEnable()) {
            is Result.Success -> {
                if (result.data) {
                    Result.Success(Unit)
                } else {
                    Result.Error(Exception("GPS enable request denied"))
                }
            }
            is Result.Error -> result.let { Result.Error(it.exception) }
            is Result.Loading -> Result.Error(Exception("GPS enable request in progress"))
        }
    }
}
