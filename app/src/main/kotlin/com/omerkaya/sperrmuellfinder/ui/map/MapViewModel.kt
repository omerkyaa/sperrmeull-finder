package com.omerkaya.sperrmuellfinder.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.manager.LocationManager
import com.omerkaya.sperrmuellfinder.domain.manager.PremiumManager
import com.omerkaya.sperrmuellfinder.domain.model.Post
import com.omerkaya.sperrmuellfinder.core.model.PostLocation
import com.omerkaya.sperrmuellfinder.domain.model.map.LocationBounds
import com.omerkaya.sperrmuellfinder.domain.usecase.map.MapCluster
import com.omerkaya.sperrmuellfinder.domain.usecase.map.MapUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.post.TogglePostLikeUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.user.GetCurrentUserUseCase
import com.omerkaya.sperrmuellfinder.domain.repository.PostRepository
import com.omerkaya.sperrmuellfinder.domain.usecase.social.GetBlockedUsersUseCase
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.paging.filter
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Map Screen.
 * Manages map state, location, clustering, and user interactions.
 * Rules.md compliant - Professional map functionality with premium features.
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val mapUseCase: MapUseCase,
    private val locationManager: LocationManager,
    private val premiumManager: PremiumManager,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getBlockedUsersUseCase: GetBlockedUsersUseCase,
    private val togglePostLikeUseCase: TogglePostLikeUseCase,
    private val postRepository: PostRepository,
    private val logger: Logger
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _events = Channel<MapEvent>()
    val events = _events.receiveAsFlow()

    // Current map viewport bounds for optimization
    private val _mapBounds = MutableStateFlow<LocationBounds?>(null)

    // Post feed with pagination - optimized for map display
    private val _blockedUserIds = getBlockedUsersUseCase()
        .map { blocked -> blocked.map { it.blockedUserId }.toSet() }
        .distinctUntilChanged()

    val postFeed: Flow<PagingData<Post>> = combine(
        locationManager.currentLocation,
        premiumManager.isPremium,
        _mapBounds,
        _blockedUserIds
    ) { location, isPremium, bounds, blockedIds ->
        Pair(location, blockedIds)
    }.flatMapLatest { (location, blockedIds) ->
        val mapLocation = location ?: PostLocation.defaultLocation()
        logger.d(Logger.TAG_DEFAULT, "Getting posts for map at: ${mapLocation.latitude}, ${mapLocation.longitude}")
        mapUseCase.getPostsForMap(mapLocation).map { pagingData ->
            if (blockedIds.isEmpty()) pagingData
            else pagingData.filter { post -> post.ownerId !in blockedIds }
        }
    }.cachedIn(viewModelScope)

    init {
        logger.d(Logger.TAG_DEFAULT, "MapViewModel initialized")
        
        // Initialize location and permissions
        initializeLocation()
        
        // Observe premium status
        viewModelScope.launch {
            premiumManager.isPremium.collect { isPremium ->
                val newRadius = if (isPremium) 20000 else 1500 // 20km vs 1.5km
                _uiState.value = _uiState.value.copy(
                    isPremium = isPremium,
                    radiusMeters = newRadius
                )
                logger.d(Logger.TAG_DEFAULT, "Premium status updated: $isPremium, radius: ${newRadius}m")
            }
        }
        
        // Observe current user
        viewModelScope.launch {
            getCurrentUserUseCase().collect { user ->
                _uiState.value = _uiState.value.copy(
                    currentUser = user
                )
                logger.d(Logger.TAG_DEFAULT, "Current user updated: ${user?.displayName}")
            }
        }
        
        // Listen to post creation events for real-time refresh
        viewModelScope.launch {
            postRepository.postCreationEvents.collect { newPost ->
                logger.d(Logger.TAG_DEFAULT, "New post created, refreshing map: ${newPost.id}")
                updateMapClusters()
            }
        }
    }

    /**
     * Initialize location services and permissions.
     */
    fun initializeLocation() {
        viewModelScope.launch {
            logger.d(Logger.TAG_DEFAULT, "Initializing location for map")
            
            // Check location permissions
            when (val permissionResult = locationManager.requestLocationPermissions()) {
                is Result.Success -> {
                    val hasPermissions = permissionResult.data
                    _uiState.value = _uiState.value.copy(
                        hasLocationPermissions = hasPermissions
                    )
                    
                    if (hasPermissions) {
                        logger.i(Logger.TAG_DEFAULT, "Location permissions granted for map")
                        checkGpsStatus()
                    } else {
                        logger.w(Logger.TAG_DEFAULT, "Location permissions not granted for map")
                        _events.send(MapEvent.LocationPermissionRequired)
                    }
                }
                is Result.Error -> {
                    logger.e(Logger.TAG_DEFAULT, "Failed to check location permissions", permissionResult.exception)
                    _uiState.value = _uiState.value.copy(
                        hasLocationPermissions = false,
                        error = "Failed to check location permissions"
                    )
                }
                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }
        }
    }

    /**
     * Check GPS status and get current location.
     */
    private suspend fun checkGpsStatus() {
        logger.d(Logger.TAG_DEFAULT, "Checking GPS status for map")
        
        when (val gpsResult = locationManager.requestGpsEnable()) {
            is Result.Success -> {
                val isGpsEnabled = gpsResult.data
                _uiState.value = _uiState.value.copy(
                    isGpsEnabled = isGpsEnabled
                )
                
                if (isGpsEnabled) {
                    logger.i(Logger.TAG_DEFAULT, "GPS enabled for map")
                    getCurrentLocation()
                } else {
                    logger.w(Logger.TAG_DEFAULT, "GPS not enabled for map")
                    _events.send(MapEvent.GpsEnableRequired)
                }
            }
            is Result.Error -> {
                logger.e(Logger.TAG_DEFAULT, "Failed to check GPS status", gpsResult.exception)
                _uiState.value = _uiState.value.copy(
                    isGpsEnabled = false,
                    error = "Failed to check GPS status"
                )
            }
            is Result.Loading -> {
                // Handle loading state if needed
            }
        }
    }

    /**
     * Get current location.
     */
    private suspend fun getCurrentLocation() {
        logger.d(Logger.TAG_DEFAULT, "Getting current location for map")
        
        _uiState.value = _uiState.value.copy(isLocationLoading = true)
        
        when (val result = locationManager.getCurrentLocation()) {
            is Result.Success -> {
                val location = result.data
                logger.i(Logger.TAG_DEFAULT, "Current location updated for map: ${location.latitude}, ${location.longitude}")
                _uiState.value = _uiState.value.copy(
                    currentLocation = location,
                    isLoading = false,
                    isLocationLoading = false,
                    error = null
                )
            }
            is Result.Error -> {
                logger.e(Logger.TAG_DEFAULT, "Failed to get current location for map", result.exception)
                // Use default location to prevent crashes
                val defaultLocation = PostLocation.defaultLocation()
                _uiState.value = _uiState.value.copy(
                    currentLocation = defaultLocation,
                    isLoading = false,
                    isLocationLoading = false,
                    error = "Failed to get current location"
                )
            }
            is Result.Loading -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    isLocationLoading = true
                )
            }
        }
    }

    /**
     * Request location permissions.
     */
    fun requestLocationPermissions() {
        viewModelScope.launch {
            logger.d(Logger.TAG_DEFAULT, "Requesting location permissions for map")
            
            when (val result = locationManager.requestLocationPermissions()) {
                is Result.Success -> {
                    val granted = result.data
                    if (granted) {
                        logger.i(Logger.TAG_DEFAULT, "Location permissions granted for map")
                        _uiState.value = _uiState.value.copy(
                            hasLocationPermissions = true
                        )
                        checkGpsStatus()
                    } else {
                        logger.w(Logger.TAG_DEFAULT, "Location permissions denied for map")
                        _events.send(MapEvent.LocationPermissionDenied)
                    }
                }
                is Result.Error -> {
                    logger.e(Logger.TAG_DEFAULT, "Failed to request location permissions", result.exception)
                    _events.send(MapEvent.LocationPermissionError(result.exception.message ?: "Permission request failed"))
                }
                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(hasLocationPermissions = granted)
            if (granted) {
                logger.i(Logger.TAG_DEFAULT, "Location permission result granted for map")
                checkGpsStatus()
            } else {
                logger.w(Logger.TAG_DEFAULT, "Location permission result denied for map")
            }
        }
    }

    /**
     * Request GPS enable.
     */
    fun requestGpsEnable() {
        viewModelScope.launch {
            logger.d(Logger.TAG_DEFAULT, "Requesting GPS enable for map")
            
            when (val result = locationManager.requestGpsEnable()) {
                is Result.Success -> {
                    if (result.data) {
                        logger.i(Logger.TAG_DEFAULT, "GPS enabled for map")
                        initializeLocation()
                    } else {
                        logger.w(Logger.TAG_DEFAULT, "GPS enable request denied for map")
                        _events.send(MapEvent.GpsEnableRequestDenied)
                    }
                }
                is Result.Error -> {
                    logger.e(Logger.TAG_DEFAULT, "Failed to request GPS enable", result.exception)
                    _events.send(MapEvent.GpsEnableError(result.exception.message ?: "GPS enable request failed"))
                }
                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }
        }
    }

    /**
     * Update map zoom level and recalculate clusters.
     */
    fun updateMapZoom(zoomLevel: Float) {
        logger.d(Logger.TAG_DEFAULT, "Map zoom level changed: $zoomLevel")
        _uiState.value = _uiState.value.copy(
            zoomLevel = zoomLevel
        )
        updateMapClusters()
    }

    /**
     * Handle post marker click - Navigate to PostDetail.
     */
    fun onPostMarkerClick(post: Post) {
        viewModelScope.launch {
            logger.d(Logger.TAG_DEFAULT, "Post marker clicked: ${post.id}")
            _events.send(MapEvent.NavigateToPostDetail(post.id))
        }
    }

    /**
     * Handle cluster marker click.
     */
    fun onClusterMarkerClick(cluster: MapCluster.MultiPost) {
        viewModelScope.launch {
            logger.d(Logger.TAG_DEFAULT, "Cluster marker clicked with ${cluster.posts.size} posts")
            _events.send(MapEvent.ShowClusterPosts(cluster.posts))
        }
    }

    /**
     * Refresh map posts after a new post is created - NEW FUNCTION
     */
    fun refreshAfterPostCreated() {
        viewModelScope.launch {
            logger.d(Logger.TAG_DEFAULT, "Refreshing map posts after new post created")
            // Map posts will be automatically refreshed through the postFeed flow
            // Just trigger a manual update of clusters
            updateMapClusters()
        }
    }

    /**
     * Handle post like/unlike.
     */
    fun onPostLike(postId: String) {
        viewModelScope.launch {
            logger.d(Logger.TAG_DEFAULT, "Toggling like for post on map: $postId")
            
            when (val result = togglePostLikeUseCase(postId)) {
                is Result.Success -> {
                    val isLiked = result.data
                    logger.i(Logger.TAG_DEFAULT, "Post $postId ${if (isLiked) "liked" else "unliked"} on map")
                    
                    _events.send(
                        if (isLiked) MapEvent.PostLiked(postId) 
                        else MapEvent.PostUnliked(postId)
                    )
                }
                is Result.Error -> {
                    logger.e(Logger.TAG_DEFAULT, "Failed to toggle like for post $postId on map", result.exception)
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to update like status"
                    )
                }
                is Result.Loading -> {
                    // Keep loading state
                }
            }
        }
    }

    /**
     * Toggle filter bar visibility.
     */
    fun toggleFilterVisibility() {
        _uiState.value = _uiState.value.copy(
            isFilterVisible = !_uiState.value.isFilterVisible
        )
        logger.d(Logger.TAG_DEFAULT, "Filter visibility toggled: ${_uiState.value.isFilterVisible}")
    }

    /**
     * Update radius filter for map posts.
     */
    fun updateRadiusFilter(radiusMeters: Int) {
        _uiState.value = _uiState.value.copy(
            radiusMeters = radiusMeters
        )
        logger.d(Logger.TAG_DEFAULT, "Map radius filter updated: ${radiusMeters}m")
        // Trigger map refresh with new radius
        updateMapClusters()
    }

    /**
     * Update map clusters based on current zoom and bounds.
     */
    private fun updateMapClusters() {
        // TODO: Implement clustering algorithm
        // This would group nearby posts into clusters based on zoom level
        logger.d(Logger.TAG_DEFAULT, "Updating map clusters (TODO: implement clustering)")
    }

    /**
     * Clear any error messages.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI State for Map Screen
 */
data class MapUiState(
    val isLoading: Boolean = true,
    val isLocationLoading: Boolean = false, // Added missing field
    val error: String? = null,
    val currentUser: com.omerkaya.sperrmuellfinder.domain.model.User? = null,
    val currentLocation: PostLocation? = null,
    val hasLocationPermissions: Boolean = false,
    val isGpsEnabled: Boolean = false,
    val isPremium: Boolean = false,
    val zoomLevel: Float = 14f,
    val isFilterVisible: Boolean = false,
    val radiusMeters: Int = 1500, // Default Basic radius
    val clusters: List<MapCluster> = emptyList(),
    val selectedCluster: MapCluster? = null
)

/**
 * Events for Map Screen
 */
sealed class MapEvent {
    data class NavigateToPostDetail(val postId: String) : MapEvent()
    data class ShowClusterPosts(val posts: List<Post>) : MapEvent()
    data object LocationPermissionRequired : MapEvent()
    data object LocationPermissionDenied : MapEvent()
    data class LocationPermissionError(val message: String) : MapEvent()
    data object GpsEnableRequired : MapEvent()
    data object GpsEnableRequestDenied : MapEvent()
    data class GpsEnableError(val message: String) : MapEvent()
    data class PostLiked(val postId: String) : MapEvent()
    data class PostUnliked(val postId: String) : MapEvent()
    data class ZoomToArea(val bounds: LocationBounds) : MapEvent()
}
