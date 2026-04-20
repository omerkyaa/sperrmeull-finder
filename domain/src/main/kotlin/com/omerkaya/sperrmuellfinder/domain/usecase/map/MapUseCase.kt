package com.omerkaya.sperrmuellfinder.domain.usecase.map

import androidx.paging.PagingData
import androidx.paging.map
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.manager.PremiumManager
import com.omerkaya.sperrmuellfinder.domain.model.Post
import com.omerkaya.sperrmuellfinder.core.model.PostLocation
import com.omerkaya.sperrmuellfinder.domain.model.map.LocationBounds
import com.omerkaya.sperrmuellfinder.domain.util.LocationUtils
import com.omerkaya.sperrmuellfinder.domain.repository.LocationRepository
import com.omerkaya.sperrmuellfinder.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case for map-related operations.
 * Handles post filtering by location, clustering, and map interactions.
 * Rules.md compliant - Professional map functionality with premium features.
 */
class MapUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val locationRepository: LocationRepository,
    private val premiumManager: PremiumManager,
    private val logger: Logger
) {

    /**
     * Get posts for map display with location filtering and clustering.
     * Basic: 1.5km radius, Premium: unlimited radius with enhanced markers.
     * 
     * @param userLocation Current user location
     * @return Flow of PagingData containing posts with distance calculations
     */
    fun getPostsForMap(userLocation: PostLocation): Flow<PagingData<Post>> {
        logger.d(Logger.TAG_DEFAULT, "Getting posts for map at: ${userLocation.latitude}, ${userLocation.longitude}")
        
        val isPremium = premiumManager.isPremium.value
        val radiusMeters = if (isPremium) {
            PREMIUM_RADIUS_METERS
        } else {
            BASIC_RADIUS_METERS
        }
        
        logger.d(Logger.TAG_DEFAULT, "Map radius: ${radiusMeters}m (Premium: $isPremium)")
        
        return if (isPremium) {
            // Premium: Distance-based sorting with enhanced markers
            postRepository.getPostsNearUserSortedByDistance(
                userLocation = userLocation,
                radiusMeters = radiusMeters
            ).map { pagingData ->
                pagingData.map { post ->
                    post.location?.let { location ->
                        val distance = LocationUtils.calculateDistance(userLocation, location)
                        post.copy(distanceFromUser = distance)
                    } ?: post // Keep post without distance if location is null
                }
            }
        } else {
            // Basic: Date-based sorting with standard markers
            postRepository.getPostsNearUserSortedByDate(
                userLocation = userLocation,
                radiusMeters = radiusMeters
            ).map { pagingData ->
                pagingData.map { post ->
                    post.location?.let { location ->
                        val distance = LocationUtils.calculateDistance(userLocation, location)
                        post.copy(distanceFromUser = distance)
                    } ?: post // Keep post without distance if location is null
                }
            }
        }
    }

    /**
     * Filter posts by viewport bounds for efficient map rendering.
     * Only shows posts visible in current map view.
     * 
     * @param posts List of posts to filter
     * @param bounds Current map viewport bounds
     * @return Filtered list of posts within bounds
     */
    fun filterPostsByViewport(
        posts: List<Post>,
        bounds: LocationBounds
    ): List<Post> {
        return posts.filter { post ->
            post.location?.let { location ->
                bounds.contains(location)
            } ?: false // Exclude posts without location from viewport
        }
    }

    /**
     * Group nearby posts into clusters for better map performance.
     * Premium users get more detailed clustering with custom markers.
     * 
     * @param posts List of posts to cluster
     * @param zoomLevel Current map zoom level (1-20)
     * @param isPremium Whether user has premium features
     * @return List of map clusters
     */
    fun clusterPosts(
        posts: List<Post>,
        zoomLevel: Float,
        isPremium: Boolean = premiumManager.isPremium.value
    ): List<MapCluster> {
        logger.d(Logger.TAG_DEFAULT, "Clustering ${posts.size} posts at zoom level $zoomLevel")
        
        // Filter out posts without location for clustering
        val validPosts = posts.filter { it.location != null }
        if (validPosts.isEmpty()) return emptyList()
        
        // Determine cluster distance based on zoom level
        val clusterDistance = when {
            zoomLevel >= 15 -> 50.0 // Very close clustering for high zoom
            zoomLevel >= 12 -> 100.0 // Medium clustering
            zoomLevel >= 9 -> 200.0 // Wide clustering
            else -> 500.0 // Very wide clustering for low zoom
        }
        
        val clusters = mutableListOf<MapCluster>()
        val processedPosts = mutableSetOf<String>()
        
        for (post in validPosts) {
            if (processedPosts.contains(post.id)) continue
            
            val nearbyPosts = mutableListOf<Post>()
            nearbyPosts.add(post)
            processedPosts.add(post.id)
            
            // Find nearby posts to cluster
            for (otherPost in validPosts) {
                if (processedPosts.contains(otherPost.id)) continue
                
                // Both posts have location (guaranteed by validPosts filter)
                val postLocation = post.location!!
                val otherPostLocation = otherPost.location!!
                
                val distance = LocationUtils.calculateDistance(postLocation, otherPostLocation)
                if (distance <= clusterDistance) {
                    nearbyPosts.add(otherPost)
                    processedPosts.add(otherPost.id)
                }
            }
            
            // Create cluster
            val cluster = if (nearbyPosts.size == 1) {
                // Single post cluster
                MapCluster.SinglePost(
                    post = nearbyPosts.first(),
                    markerType = getMarkerType(nearbyPosts.first(), isPremium)
                )
            } else {
                // Multi-post cluster - all posts have location (guaranteed by validPosts filter)
                val centerLocation = calculateClusterCenter(nearbyPosts.mapNotNull { it.location })
                MapCluster.MultiPost(
                    posts = nearbyPosts,
                    centerLocation = centerLocation,
                    count = nearbyPosts.size,
                    markerType = getClusterMarkerType(nearbyPosts, isPremium)
                )
            }
            
            clusters.add(cluster)
        }
        
        logger.d(Logger.TAG_DEFAULT, "Created ${clusters.size} clusters from ${validPosts.size} valid posts")
        return clusters
    }

    /**
     * Calculate optimal map bounds to show all posts.
     * 
     * @param posts List of posts to include in bounds
     * @param padding Additional padding around bounds
     * @return Map bounds containing all posts
     */
    fun calculateMapBounds(
        posts: List<Post>,
        padding: Double = 0.01 // ~1km padding
    ): LocationBounds? {
        // Filter posts with valid locations
        val validPosts = posts.filter { it.location != null }
        if (validPosts.isEmpty()) return null
        
        val latitudes = validPosts.mapNotNull { it.location?.latitude }
        val longitudes = validPosts.mapNotNull { it.location?.longitude }
        
        if (latitudes.isEmpty() || longitudes.isEmpty()) return null
        
        val minLat = latitudes.minOrNull()!! - padding
        val maxLat = latitudes.maxOrNull()!! + padding
        val minLng = longitudes.minOrNull()!! - padding
        val maxLng = longitudes.maxOrNull()!! + padding
        
        return LocationBounds(
            northeast = PostLocation(
                latitude = maxLat,
                longitude = maxLng,
                city = "NE",
                country = "DE",
                address = "NE"
            ),
            southwest = PostLocation(
                latitude = minLat,
                longitude = minLng,
                city = "SW",
                country = "DE",
                address = "SW"
            )
        )
    }

    /**
     * Get marker type based on post and user premium status.
     */
    private fun getMarkerType(post: Post, isPremium: Boolean): MarkerType {
        return when {
            isPremium && post.ownerPremiumFrameType != com.omerkaya.sperrmuellfinder.domain.model.PremiumFrameType.NONE -> {
                when (post.ownerPremiumFrameType) {
                    com.omerkaya.sperrmuellfinder.domain.model.PremiumFrameType.DIAMOND -> MarkerType.PREMIUM_CRYSTAL
                    com.omerkaya.sperrmuellfinder.domain.model.PremiumFrameType.GOLD -> MarkerType.PREMIUM_GOLD
                    else -> MarkerType.PREMIUM_STANDARD
                }
            }
            isPremium -> MarkerType.PREMIUM_STANDARD
            else -> MarkerType.BASIC
        }
    }

    /**
     * Get cluster marker type based on posts and premium status.
     */
    private fun getClusterMarkerType(posts: List<Post>, isPremium: Boolean): MarkerType {
        return if (isPremium) {
            val hasPremiumPosts = posts.any { 
                it.ownerPremiumFrameType != com.omerkaya.sperrmuellfinder.domain.model.PremiumFrameType.NONE 
            }
            if (hasPremiumPosts) MarkerType.CLUSTER_PREMIUM else MarkerType.CLUSTER_STANDARD
        } else {
            MarkerType.CLUSTER_BASIC
        }
    }

    /**
     * Calculate center location for a cluster of posts.
     */
    private fun calculateClusterCenter(locations: List<PostLocation>): PostLocation {
        val avgLat = locations.map { it.latitude }.average()
        val avgLng = locations.map { it.longitude }.average()
        return PostLocation(
            latitude = avgLat,
            longitude = avgLng,
            city = "Cluster Center",
            country = "DE",
            address = "Cluster"
        )
    }

    companion object {
        private const val BASIC_RADIUS_METERS = PremiumManager.BASIC_RADIUS_METERS
        private const val PREMIUM_RADIUS_METERS = Int.MAX_VALUE
    }
}

/**
 * Map cluster representation for efficient rendering.
 */
sealed class MapCluster {
    /**
     * Single post marker
     */
    data class SinglePost(
        val post: Post,
        val markerType: MarkerType
    ) : MapCluster()

    /**
     * Multiple posts clustered together
     */
    data class MultiPost(
        val posts: List<Post>,
        val centerLocation: PostLocation,
        val count: Int,
        val markerType: MarkerType
    ) : MapCluster()
}

/**
 * Marker types for different user tiers and post types.
 * Rules.md compliant - Premium markers are enhanced.
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
