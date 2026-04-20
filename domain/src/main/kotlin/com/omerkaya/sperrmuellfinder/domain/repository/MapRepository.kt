package com.omerkaya.sperrmuellfinder.domain.repository

import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.MapFilter
import com.omerkaya.sperrmuellfinder.domain.model.PostMapItem
import com.omerkaya.sperrmuellfinder.domain.model.UserLocation
import com.omerkaya.sperrmuellfinder.core.model.PostLocation
import com.omerkaya.sperrmuellfinder.domain.model.map.LocationBounds
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for map-related operations.
 * Handles location-based post queries, clustering, and map interactions.
 */
interface MapRepository {
    
    /**
     * Get nearby posts for map display with filtering and clustering support.
     * Applies Basic vs Premium restrictions automatically.
     * 
     * @param userLocation Current user location
     * @param filter Map filter configuration
     * @return Flow of nearby posts optimized for map display
     */
    fun getNearbyPosts(
        userLocation: UserLocation,
        filter: MapFilter
    ): Flow<List<PostMapItem>>
    
    /**
     * Get posts within specific bounds for viewport-based queries.
     * More efficient for map camera movements.
     * 
     * @param bounds Map viewport bounds
     * @param filter Map filter configuration
     * @return Flow of posts within bounds
     */
    fun getPostsInBounds(
        bounds: LocationBounds,
        filter: MapFilter
    ): Flow<List<PostMapItem>>
    
    /**
     * Update user's current location for location-based features.
     * 
     * @param userLocation New user location
     * @return Result of location update operation
     */
    suspend fun updateUserLocation(userLocation: UserLocation): Result<Unit>
    
    /**
     * Get user's last known location.
     * 
     * @return Result containing last known location or error
     */
    suspend fun getLastKnownLocation(): Result<UserLocation>
    
    /**
     * Calculate cluster groups for efficient map rendering.
     * Groups nearby posts based on zoom level and density.
     * 
     * @param posts List of posts to cluster
     * @param zoomLevel Current map zoom level
     * @param bounds Current map bounds
     * @return List of cluster groups
     */
    suspend fun calculateClusters(
        posts: List<PostMapItem>,
        zoomLevel: Float,
        bounds: LocationBounds
    ): Result<List<MapCluster>>
    
    /**
     * Get posts for a specific cluster when user taps on cluster marker.
     * 
     * @param clusterCenter Center location of the cluster
     * @param radiusMeters Radius to search for posts
     * @return Result containing posts in cluster
     */
    suspend fun getPostsInCluster(
        clusterCenter: PostLocation,
        radiusMeters: Int
    ): Result<List<PostMapItem>>
    
    /**
     * Subscribe to FCM topics for location-based notifications.
     * Premium feature: get notified of new posts in favorite areas.
     * 
     * @param favoriteLocations List of favorite locations
     * @param categories Favorite categories
     * @return Result of subscription operation
     */
    suspend fun subscribeToLocationNotifications(
        favoriteLocations: List<PostLocation>,
        categories: List<String>
    ): Result<Unit>
    
    /**
     * Unsubscribe from location-based notifications.
     * 
     * @return Result of unsubscription operation
     */
    suspend fun unsubscribeFromLocationNotifications(): Result<Unit>
    
    /**
     * Get map configuration from Remote Config.
     * 
     * @return Result containing map configuration
     */
    suspend fun getMapConfiguration(): Result<MapConfiguration>
    
    /**
     * Cache posts for offline map viewing.
     * 
     * @param posts Posts to cache
     * @param bounds Cache bounds
     * @return Result of cache operation
     */
    suspend fun cachePosts(
        posts: List<PostMapItem>,
        bounds: LocationBounds
    ): Result<Unit>
    
    /**
     * Get cached posts for offline viewing.
     * 
     * @param bounds Bounds to get cached posts for
     * @return Result containing cached posts
     */
    suspend fun getCachedPosts(bounds: LocationBounds): Result<List<PostMapItem>>
    
    /**
     * Clear map cache to free up storage.
     * 
     * @return Result of cache clear operation
     */
    suspend fun clearCache(): Result<Unit>
}

/**
 * Map cluster representation for efficient rendering
 */
sealed class MapCluster {
    /**
     * Single post marker
     */
    data class SinglePost(
        val post: PostMapItem,
        val markerType: MarkerType
    ) : MapCluster()
    
    /**
     * Multiple posts clustered together
     */
    data class MultiPost(
        val posts: List<PostMapItem>,
        val centerLocation: PostLocation,
        val count: Int,
        val markerType: MarkerType,
        val averageLevel: Int = 0,
        val premiumCount: Int = 0
    ) : MapCluster()
}

/**
 * Marker types for different user tiers and post types
 */
enum class MarkerType {
    BASIC,              // Basic user markers (gray)
    PREMIUM_STANDARD,   // Premium user standard markers (colored)
    PREMIUM_GOLD,       // Premium gold frame markers (Lv1-14)
    PREMIUM_CRYSTAL,    // Premium crystal frame markers (Lv15+, animated)
    CLUSTER_BASIC,      // Basic user cluster markers
    CLUSTER_STANDARD,   // Standard cluster markers
    CLUSTER_PREMIUM     // Premium cluster markers (enhanced)
}

/**
 * Map configuration from Remote Config
 */
data class MapConfiguration(
    val basicRadiusMeters: Int = 1500,
    val premiumRadiusMeters: Int = 20000,
    val clusterMinZoom: Float = 10f,
    val clusterMaxZoom: Float = 16f,
    val availabilityThreshold: Float = 0.6f,
    val premiumMarkerEnabled: Boolean = true,
    val earlyAccessMinutes: Int = 10,
    val cacheMaxSizeMB: Int = 50,
    val maxPostsPerQuery: Int = 500
)
