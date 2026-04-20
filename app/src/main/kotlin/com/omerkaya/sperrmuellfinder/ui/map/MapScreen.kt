package com.omerkaya.sperrmuellfinder.ui.map

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.omerkaya.sperrmuellfinder.core.navigation.NavigationManager
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.ui.map.components.LocationPermissionRequired
import com.omerkaya.sperrmuellfinder.ui.map.components.MapFilterBar
import com.omerkaya.sperrmuellfinder.ui.map.components.MapLoadingState
import com.omerkaya.sperrmuellfinder.ui.map.components.MapMarkers

/**
 * Map Screen displaying posts on Google Maps with clustering.
 * Professional map interface with premium features and location-based filtering.
 * Rules.md compliant - Material3 design with premium enhancements.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    onNavigateToPostDetail: (String) -> Unit,
    navigationManager: NavigationManager,
    viewModel: MapViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val navigationState by navigationManager.navigationState.collectAsState()
    val posts = viewModel.postFeed.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var hasRequestedLocationPermission by remember { mutableStateOf(false) }
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    val isLocationPermanentlyDenied = hasRequestedLocationPermission &&
        locationPermissionsState.permissions.any { permissionState ->
            when (val status = permissionState.status) {
                is PermissionStatus.Denied -> !status.shouldShowRationale
                PermissionStatus.Granted -> false
            }
        }

    // Camera position state
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(52.5200, 13.4050), // Berlin default
            14f
        )
    }

    // Update camera when location changes
    LaunchedEffect(uiState.currentLocation) {
        if (navigationState.mapCameraPosition != null) return@LaunchedEffect
        uiState.currentLocation?.let { location ->
            val latLng = LatLng(location.latitude, location.longitude)
            cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 14f)
        }
    }

    // Apply focused map location from navigation (e.g. Home > location tap)
    LaunchedEffect(navigationState.mapCameraPosition) {
        navigationState.mapCameraPosition?.let { mapCameraState ->
            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                LatLng(mapCameraState.latitude, mapCameraState.longitude),
                mapCameraState.zoom
            )
        }
    }

    // Monitor camera position changes for zoom updates
    LaunchedEffect(cameraPositionState.position) {
        viewModel.updateMapZoom(cameraPositionState.position.zoom)
    }

    LaunchedEffect(Unit) {
        if (!locationPermissionsState.allPermissionsGranted) {
            hasRequestedLocationPermission = true
            locationPermissionsState.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        viewModel.onLocationPermissionResult(locationPermissionsState.allPermissionsGranted)
    }

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MapEvent.NavigateToPostDetail -> {
                    onNavigateToPostDetail(event.postId)
                }
                is MapEvent.PostLiked -> {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.post_liked)
                    )
                }
                is MapEvent.PostUnliked -> {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.post_unliked)
                    )
                }
                is MapEvent.LocationPermissionRequired -> {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.location_permission_required)
                    )
                }
                is MapEvent.LocationPermissionDenied -> {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.location_permission_denied)
                    )
                }
                is MapEvent.LocationPermissionError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message
                    )
                }
                is MapEvent.GpsEnableRequired -> {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.gps_enable_required)
                    )
                }
                is MapEvent.GpsEnableRequestDenied -> {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.gps_enable_denied)
                    )
                }
                is MapEvent.GpsEnableError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message
                    )
                }
                is MapEvent.ZoomToArea -> {
                    // Handle zoom to area
                    val bounds = event.bounds
                    val northeast = LatLng(bounds.northeast.latitude, bounds.northeast.longitude)
                    val southwest = LatLng(bounds.southwest.latitude, bounds.southwest.longitude)
                    // TODO: Animate camera to bounds
                }
                is MapEvent.ShowClusterPosts -> {
                    // TODO: Show cluster posts in bottom sheet
                }
            }
        }
    }

    // Show error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(message = error)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column {
                // Filter FAB (Premium feature)
                if (uiState.isPremium) {
                    FloatingActionButton(
                        onClick = { viewModel.toggleFilterVisibility() },
                        modifier = Modifier.padding(bottom = 8.dp),
                        containerColor = MaterialTheme.colorScheme.secondary
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = stringResource(R.string.map_filters)
                        )
                    }
                }
                
                // My Location FAB
                FloatingActionButton(
                    onClick = { 
                        if (uiState.hasLocationPermissions && uiState.isGpsEnabled) {
                            viewModel.initializeLocation()
                        } else {
                            hasRequestedLocationPermission = true
                            locationPermissionsState.launchMultiplePermissionRequest()
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = if (uiState.hasLocationPermissions && uiState.isGpsEnabled) {
                            Icons.Default.MyLocation
                        } else {
                            Icons.Default.LocationOff
                        },
                        contentDescription = stringResource(R.string.my_location)
                    )
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                !uiState.hasLocationPermissions -> {
                    LocationPermissionRequired(
                        onRequestPermission = {
                            hasRequestedLocationPermission = true
                            locationPermissionsState.launchMultiplePermissionRequest()
                        },
                        showSettingsAction = isLocationPermanentlyDenied,
                        onOpenSettings = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
                
                uiState.isLocationLoading -> {
                    MapLoadingState()
                }
                
                else -> {
                    // Google Map
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(
                            mapType = MapType.NORMAL,
                            isMyLocationEnabled = uiState.hasLocationPermissions && uiState.isGpsEnabled
                        ),
                        uiSettings = MapUiSettings(
                            myLocationButtonEnabled = false, // We use custom FAB
                            zoomControlsEnabled = false,
                            compassEnabled = true,
                            rotationGesturesEnabled = true,
                            scrollGesturesEnabled = true,
                            tiltGesturesEnabled = true,
                            zoomGesturesEnabled = true
                        ),
                        onMapLoaded = {
                            // Map loaded successfully
                        },
                        onMapClick = { latLng ->
                            // Handle map click if needed
                        }
                    ) {
                        // Map Markers and Clusters
                        MapMarkers(
                            posts = posts,
                            uiState = uiState,
                            onPostMarkerClick = viewModel::onPostMarkerClick,
                            onClusterMarkerClick = viewModel::onClusterMarkerClick,
                            onPostLike = viewModel::onPostLike
                        )
                    }
                }
            }
            
            // Filter Bar (Premium feature)
            if (uiState.isPremium && uiState.isFilterVisible) {
                MapFilterBar(
                    radiusMeters = uiState.radiusMeters,
                    onRadiusChange = viewModel::updateRadiusFilter,
                    onDismiss = { viewModel.toggleFilterVisibility() },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                )
            }
        }
    }
}
