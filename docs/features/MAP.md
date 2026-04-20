# Map Module - SperrmüllFinder

## Overview

The Map Module provides location-based post discovery with Google Maps integration, clustering, premium features, and comprehensive user experience. Built with Clean Architecture principles and full Jetpack Compose UI.

## Architecture

### Domain Layer
- **Entities**: `PostMapItem`, `UserLocation`, `MapFilter`
- **Repository**: `MapRepository` interface
- **Use Cases**: `GetNearbyPostsUseCase`, `UpdateUserLocationUseCase`

### Data Layer
- **Repository**: `MapRepositoryImpl` with Firestore geo-queries
- **Data Source**: `GoogleMapsDataSource` with location services
- **Utilities**: `GeoHashUtils` for efficient location queries

### Presentation Layer
- **Screen**: `MapScreen` with Google Maps Compose
- **ViewModel**: `MapViewModel` with state management
- **Components**: Markers, clusters, filters, info sheets

## Features

### Core Functionality
- ✅ **Location-based post queries** with geohash optimization
- ✅ **Real-time clustering** based on zoom level and density
- ✅ **Premium vs Basic user restrictions** (radius, filters)
- ✅ **Caching system** for offline viewing and performance
- ✅ **FCM integration** for location-based notifications

### Premium Features
- **Extended radius**: Basic (1.5km) → Premium (20km)
- **Advanced filters**: Categories, availability, sorting
- **Enhanced markers**: Level-based colors and animations
- **Early access**: Premium users see new posts first
- **Location notifications**: Favorite areas and categories

### User Experience
- **Smooth clustering**: Animated marker grouping/ungrouping
- **Info sheets**: Post details with actions (Open, Directions, Report)
- **Filter bar**: Category selection, radius control, availability
- **Empty/Error states**: Branded illustrations with retry options
- **Accessibility**: Full screen reader and keyboard support

## Configuration

### Remote Config Keys
```kotlin
// Basic user limits
map_basic_radius_m = 1500          // 1.5 km radius limit
map_cluster_min_zoom = 10.0        // Start clustering below zoom 10
map_availability_threshold = 0.6    // 60% availability threshold

// Premium features
map_premium_radius_m = 20000       // 20 km radius for premium
map_premium_marker_enabled = true  // Enhanced marker styles
map_early_access_minutes = 10      // Premium early access window

// Performance settings
map_max_posts_per_query = 500      // Query result limit
map_cache_max_size_mb = 50         // Cache size limit
```

### Analytics Events
```kotlin
// Map interactions
map_view                    // Map screen opened
map_camera_move            // User moved map camera
map_filter_apply           // Filters applied
map_marker_open            // Post marker tapped
map_cluster_expand         // Cluster marker tapped

// Premium upsells
map_radius_gate_shown      // Basic user hit radius limit
map_premium_upsell_click   // Premium upgrade CTA tapped
map_filter_gate_shown      // Basic user tried premium filter
```

## Setup Instructions

### 1. Dependencies
Ensure these dependencies are in your `build.gradle.kts`:

```kotlin
// Google Maps
implementation("com.google.maps.android:maps-compose:2.15.0")
implementation("com.google.android.gms:play-services-maps:18.2.0")
implementation("com.google.android.gms:play-services-location:21.0.1")

// Maps Utils for clustering
implementation("com.google.maps.android:android-maps-utils:3.4.0")
```

### 2. Manifest Permissions
Add to `AndroidManifest.xml`:

```xml
<!-- Location permissions -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

<!-- Google Maps API key -->
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="${MAPS_API_KEY}" />
```

### 3. ProGuard Rules
Add to `proguard-rules.pro`:

```proguard
# Google Maps
-keep class com.google.android.gms.maps.** { *; }
-keep interface com.google.android.gms.maps.** { *; }

# Maps Utils
-keep class com.google.maps.android.** { *; }

# Location Services
-keep class com.google.android.gms.location.** { *; }
```

## Usage Examples

### Basic Map Integration
```kotlin
@Composable
fun MapScreen(
    onNavigateToPostDetail: (String) -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    GoogleMap(
        // Map configuration
    ) {
        // Markers and clusters
        MapMarkers(
            posts = posts,
            uiState = uiState,
            onPostMarkerClick = { post ->
                onNavigateToPostDetail(post.id)
            }
        )
    }
}
```

### Premium Gating
```kotlin
// Check if user can access premium features
val canUsePremiumFeatures = getNearbyPostsUseCase.canAccessPremiumFeatures()

// Apply radius restrictions
val maxRadius = getNearbyPostsUseCase.getMaxAllowedRadius()
if (requestedRadius > maxRadius) {
    // Show premium upsell
}
```

### Location Updates
```kotlin
// Get current location
val locationResult = updateUserLocationUseCase.getCurrentLocation()
locationResult.collect { result ->
    when (result) {
        is Result.Success -> {
            val location = result.data
            // Update map center
        }
        is Result.Error -> {
            // Handle location error
        }
    }
}
```

## Testing

### Unit Tests
```kotlin
class GetNearbyPostsUseCaseTest {
    @Test
    fun `basic user should be limited to 1500m radius`() {
        // Test premium gating logic
    }
    
    @Test
    fun `premium user should access all filters`() {
        // Test premium features
    }
}
```

### Integration Tests
```kotlin
class MapRepositoryImplTest {
    @Test
    fun `should return posts within radius`() {
        // Test geo-queries
    }
    
    @Test
    fun `should cluster posts correctly`() {
        // Test clustering algorithm
    }
}
```

## Performance Optimization

### Caching Strategy
- **Location cache**: 5-minute expiry for user location
- **Posts cache**: Viewport-based caching with 5-minute expiry
- **Address cache**: Geocoded addresses cached for 5 minutes
- **Cluster cache**: Pre-calculated clusters cached per zoom level

### Query Optimization
- **Geohash queries**: Efficient bounding box queries
- **Result limits**: Maximum 500 posts per query
- **Debounced updates**: 300ms delay on camera movements
- **Viewport filtering**: Only query visible area

### Memory Management
- **Marker recycling**: Reuse marker instances
- **Image caching**: Glide integration for post thumbnails
- **Cluster optimization**: Off-main-thread calculations

## Troubleshooting

### Common Issues

#### Location Not Working
1. Check location permissions in manifest
2. Verify Google Play Services availability
3. Test on physical device (emulator may have issues)

#### Maps Not Loading
1. Verify Google Maps API key is correct
2. Check if API key has Maps SDK enabled
3. Ensure billing is enabled for Google Cloud project

#### Poor Performance
1. Reduce `map_max_posts_per_query` in Remote Config
2. Increase clustering thresholds
3. Clear map cache if it's too large

### Debug Logging
Enable debug logging to troubleshoot issues:

```kotlin
// In Application class
logger.setLogLevel(Logger.LEVEL_DEBUG)

// Check logs for:
// - "Getting nearby posts" - Query execution
// - "Calculating clusters" - Clustering operations  
// - "Location obtained" - GPS/location updates
```

## Future Enhancements

### Planned Features
- **Heatmap overlay**: Density visualization for premium users
- **Route planning**: Multi-stop collection routes
- **AR integration**: Augmented reality post discovery
- **Offline maps**: Download areas for offline viewing
- **Social features**: Share favorite locations with friends

### Performance Improvements
- **WebP images**: Smaller marker icons
- **Vector tiles**: Custom map styling
- **Background sync**: Pre-load nearby posts
- **ML clustering**: Smarter grouping algorithms

## Contributing

When contributing to the Map Module:

1. **Follow Clean Architecture**: Keep domain logic separate from UI
2. **Test premium gating**: Ensure Basic/Premium restrictions work
3. **Test on device**: Location features require physical device
4. **Update strings**: Add new strings to both DE and EN resources
5. **Performance test**: Verify smooth scrolling and clustering

## Support

For issues with the Map Module:

1. Check the troubleshooting section above
2. Review logs for error messages
3. Test on physical device with location enabled
4. Verify Google Maps API key and billing

---

**Map Module Status**: ✅ Production Ready
**Last Updated**: October 2024
**Version**: 1.0.0
