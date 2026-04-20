package com.omerkaya.sperrmuellfinder.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.omerkaya.sperrmuellfinder.core.model.PostLocation
import com.omerkaya.sperrmuellfinder.core.util.GeoHashUtils
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants
import com.omerkaya.sperrmuellfinder.data.datasource.GoogleMapsDataSource
import com.omerkaya.sperrmuellfinder.data.manager.RemoteConfigManager
import com.omerkaya.sperrmuellfinder.data.messaging.NotificationTokenHelper
import com.omerkaya.sperrmuellfinder.domain.model.MapFilter
import com.omerkaya.sperrmuellfinder.domain.model.MapSortBy
import com.omerkaya.sperrmuellfinder.domain.model.PostMapItem
import com.omerkaya.sperrmuellfinder.domain.model.PremiumFrameType
import com.omerkaya.sperrmuellfinder.domain.model.UserLocation
import com.omerkaya.sperrmuellfinder.domain.model.LocationProvider
import com.omerkaya.sperrmuellfinder.domain.model.map.LocationBounds
import com.omerkaya.sperrmuellfinder.domain.repository.MapCluster
import com.omerkaya.sperrmuellfinder.domain.repository.MapConfiguration
import com.omerkaya.sperrmuellfinder.domain.repository.MapRepository
import com.omerkaya.sperrmuellfinder.domain.repository.MarkerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * Implementation of MapRepository with Firestore integration and clustering.
 * Handles geo-queries, caching, and premium feature enforcement.
 */
@Singleton
class MapRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val googleMapsDataSource: GoogleMapsDataSource,
    private val remoteConfigManager: RemoteConfigManager,
    private val notificationTokenHelper: NotificationTokenHelper,
    private val logger: Logger
) : MapRepository {
    
    // In-memory cache for posts and clusters
    private val postsCache = ConcurrentHashMap<String, List<PostMapItem>>()
    private val clustersCache = ConcurrentHashMap<String, List<MapCluster>>()
    private val locationCache = ConcurrentHashMap<String, UserLocation>()
    
    // Cache expiry time (5 minutes)
    private val cacheExpiryMs = 5 * 60 * 1000L
    private val cacheTimestamps = ConcurrentHashMap<String, Long>()
    
    override fun getNearbyPosts(
        userLocation: UserLocation,
        filter: MapFilter
    ): Flow<List<PostMapItem>> = flow {
        try {
            logger.d(Logger.TAG_DEFAULT, "Getting nearby posts - Location: ${userLocation.latitude}, ${userLocation.longitude}, Radius: ${filter.radiusMeters}m")
            
            // Check cache first
            val cacheKey = "nearby_${userLocation.latitude}_${userLocation.longitude}_${filter.radiusMeters}"
            val cachedPosts = getCachedPosts(cacheKey)
            if (cachedPosts != null) {
                logger.d(Logger.TAG_DEFAULT, "Using cached posts: ${cachedPosts.size}")
                emit(cachedPosts)
                return@flow
            }
            
            // Calculate geohash bounds for efficient querying
            val geohashBounds = GeoHashUtils.getBoundingBoxHashes(
                userLocation.latitude,
                userLocation.longitude,
                filter.radiusMeters.toDouble()
            )
            
            logger.d(Logger.TAG_DEFAULT, "Using ${geohashBounds.size} geohash bounds for query")
            
            // Build Firestore query
            val query = buildNearbyPostsQuery(filter, geohashBounds)
            
            // Execute query
            val querySnapshot = query.get().await()
            val posts = mutableListOf<PostMapItem>()
            
            for (document in querySnapshot.documents) {
                try {
                    val post = mapDocumentToPostMapItem(document.data ?: continue, document.id)
                    
                    // Apply distance filtering (Firestore geo-queries are approximate)
                    val distance = calculateDistance(
                        userLocation.latitude, userLocation.longitude,
                        post.location.latitude, post.location.longitude
                    )
                    
                    if (distance <= filter.radiusMeters) {
                        posts.add(post.copy(distanceFromUser = distance))
                    }
                    
                } catch (e: Exception) {
                    logger.w(Logger.TAG_DEFAULT, "Failed to parse post document: ${document.id}", e)
                }
            }
            
            // Apply additional filtering
            val filteredPosts = applyAdditionalFilters(posts, filter)
            
            // Cache results
            cachePosts(cacheKey, filteredPosts)
            
            logger.i(Logger.TAG_DEFAULT, "Retrieved ${filteredPosts.size} nearby posts")
            emit(filteredPosts)
            
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Error getting nearby posts", e)
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO).catch { exception ->
        logger.e(Logger.TAG_DEFAULT, "Flow error in getNearbyPosts", exception)
        emit(emptyList())
    }
    
    override fun getPostsInBounds(
        bounds: LocationBounds,
        filter: MapFilter
    ): Flow<List<PostMapItem>> = flow {
        try {
            logger.d(Logger.TAG_DEFAULT, "Getting posts in bounds")
            
            // Check cache
            val cacheKey = "bounds_${bounds.southwest.latitude}_${bounds.southwest.longitude}_${bounds.northeast.latitude}_${bounds.northeast.longitude}"
            val cachedPosts = getCachedPosts(cacheKey)
            if (cachedPosts != null) {
                emit(cachedPosts)
                return@flow
            }
            
            // Build bounds query
            val query = buildBoundsQuery(bounds, filter)
            
            // Execute query
            val querySnapshot = query.get().await()
            val posts = mutableListOf<PostMapItem>()
            
            for (document in querySnapshot.documents) {
                try {
                    val post = mapDocumentToPostMapItem(document.data ?: continue, document.id)
                    
                    // Verify post is within bounds
                    if (isLocationInBounds(post.location, bounds)) {
                        posts.add(post)
                    }
                    
                } catch (e: Exception) {
                    logger.w(Logger.TAG_DEFAULT, "Failed to parse post document: ${document.id}", e)
                }
            }
            
            // Apply filtering
            val filteredPosts = applyAdditionalFilters(posts, filter)
            
            // Cache results
            cachePosts(cacheKey, filteredPosts)
            
            logger.i(Logger.TAG_DEFAULT, "Retrieved ${filteredPosts.size} posts in bounds")
            emit(filteredPosts)
            
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Error getting posts in bounds", e)
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO).catch { exception ->
        logger.e(Logger.TAG_DEFAULT, "Flow error in getPostsInBounds", exception)
        emit(emptyList())
    }
    
    override suspend fun updateUserLocation(userLocation: UserLocation): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                logger.d(Logger.TAG_DEFAULT, "Updating user location in repository")
                
                // Cache location
                locationCache["current"] = userLocation
                cacheTimestamps["location_current"] = System.currentTimeMillis()
                
                // Update in Google Maps data source
                googleMapsDataSource.updateCurrentLocation(userLocation.toPostLocation())

                // Persist latest user location for premium nearby notification targeting.
                auth.currentUser?.uid?.let { uid ->
                    try {
                        firestore.collection(FirestoreConstants.Collections.USERS)
                            .document(uid)
                            .update(
                                mapOf(
                                    "current_location" to mapOf(
                                        "lat" to userLocation.latitude,
                                        "lng" to userLocation.longitude,
                                        "updated_at" to FieldValue.serverTimestamp()
                                    )
                                )
                            )
                            .await()
                    } catch (e: Exception) {
                        logger.w(Logger.TAG_DEFAULT, "Could not persist current_location to users/$uid", e)
                    }
                }
                
                logger.i(Logger.TAG_DEFAULT, "User location updated successfully")
                Result.Success(Unit)
                
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "Error updating user location", e)
                Result.Error(e)
            }
        }
    }
    
    override suspend fun getLastKnownLocation(): Result<UserLocation> {
        return withContext(Dispatchers.IO) {
            try {
                // Check cache first
                val cachedLocation = locationCache["current"]
                val cacheTime = cacheTimestamps["location_current"] ?: 0L

                if (cachedLocation != null && (System.currentTimeMillis() - cacheTime) < cacheExpiryMs) {
                    return@withContext Result.Success(cachedLocation)
                }

                // Get from Google Maps data source
                val locationResult = googleMapsDataSource.getCurrentLocation()

                if (locationResult is Result.Success) {
                    val postLocation = locationResult.data
                    val userLocation = UserLocation(
                        latitude = postLocation.latitude,
                        longitude = postLocation.longitude,
                        accuracy = 50f,
                        timestamp = Date(),
                        provider = LocationProvider.FUSED
                    )

                    // Cache result
                    locationCache["current"] = userLocation
                    cacheTimestamps["location_current"] = System.currentTimeMillis()

                    return@withContext Result.Success(userLocation)
                } else if (locationResult is Result.Error) {
                    logger.w(Logger.TAG_DEFAULT, "Failed to get location, using default", locationResult.exception)
                    return@withContext Result.Success(UserLocation.defaultLocation())
                } else {
                    logger.w(Logger.TAG_DEFAULT, "Unexpected location result type, using default")
                    return@withContext Result.Success(UserLocation.defaultLocation())
                }

            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "Error getting last known location", e)
                return@withContext Result.Success(UserLocation.defaultLocation())
            }
        }
    }
    
    override suspend fun calculateClusters(
        posts: List<PostMapItem>,
        zoomLevel: Float,
        bounds: LocationBounds
    ): Result<List<MapCluster>> {
        return withContext(Dispatchers.IO) {
            try {
                logger.d(Logger.TAG_DEFAULT, "Calculating clusters for ${posts.size} posts at zoom level $zoomLevel")
                
                // Check cache
                val cacheKey = "clusters_${posts.size}_${zoomLevel}_${bounds.hashCode()}"
                val cachedClusters = clustersCache[cacheKey]
                val cacheTime = cacheTimestamps[cacheKey] ?: 0L
                
                if (cachedClusters != null && (System.currentTimeMillis() - cacheTime) < cacheExpiryMs) {
                    return@withContext Result.Success(cachedClusters)
                }
                
                // Calculate cluster distance based on zoom level
                val clusterDistance = calculateClusterDistance(zoomLevel)
                
                // Group posts into clusters
                val clusters = mutableListOf<MapCluster>()
                val processedPosts = mutableSetOf<String>()
                
                for (post in posts) {
                    if (processedPosts.contains(post.id)) continue
                    
                    // Find nearby posts for clustering
                    val nearbyPosts = posts.filter { otherPost ->
                        !processedPosts.contains(otherPost.id) &&
                        calculateDistance(
                            post.location.latitude, post.location.longitude,
                            otherPost.location.latitude, otherPost.location.longitude
                        ) <= clusterDistance
                    }
                    
                    if (nearbyPosts.size == 1) {
                        // Single post
                        clusters.add(
                            MapCluster.SinglePost(
                                post = nearbyPosts.first(),
                                markerType = getMarkerType(nearbyPosts.first())
                            )
                        )
                    } else {
                        // Multiple posts - create cluster
                        val centerLat = nearbyPosts.map { it.location.latitude }.average()
                        val centerLng = nearbyPosts.map { it.location.longitude }.average()
                        val premiumCount = nearbyPosts.count { it.isPremiumOwner }
                        val averageLevel = nearbyPosts.map { it.ownerLevel }.average().toInt()
                        
                        clusters.add(
                            MapCluster.MultiPost(
                                posts = nearbyPosts,
                                centerLocation = PostLocation(centerLat, centerLng),
                                count = nearbyPosts.size,
                                markerType = getClusterMarkerType(nearbyPosts),
                                averageLevel = averageLevel,
                                premiumCount = premiumCount
                            )
                        )
                    }
                    
                    // Mark posts as processed
                    nearbyPosts.forEach { processedPosts.add(it.id) }
                }
                
                // Cache results
                clustersCache[cacheKey] = clusters
                cacheTimestamps[cacheKey] = System.currentTimeMillis()
                
                logger.i(Logger.TAG_DEFAULT, "Created ${clusters.size} clusters from ${posts.size} posts")
                Result.Success(clusters)
                
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "Error calculating clusters", e)
                Result.Error(e)
            }
        }
    }
    
    override suspend fun getPostsInCluster(
        clusterCenter: PostLocation,
        radiusMeters: Int
    ): Result<List<PostMapItem>> {
        return withContext(Dispatchers.IO) {
            try {
                logger.d(Logger.TAG_DEFAULT, "Getting posts in cluster at ${clusterCenter.latitude}, ${clusterCenter.longitude}")
                
                // Use nearby posts query with cluster center
                val userLocation = UserLocation.fromPostLocation(clusterCenter)
                val filter = MapFilter(radiusMeters = radiusMeters)
                
                // Get posts synchronously
                val posts = mutableListOf<PostMapItem>()
                getNearbyPosts(userLocation, filter).collect { nearbyPosts ->
                    posts.addAll(nearbyPosts)
                }
                
                Result.Success(posts)
                
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "Error getting posts in cluster", e)
                Result.Error(e)
            }
        }
    }
    
    override suspend fun subscribeToLocationNotifications(
        favoriteLocations: List<PostLocation>,
        categories: List<String>
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                logger.d(Logger.TAG_DEFAULT, "Subscribing to location notifications")
                
                // Subscribe to FCM topics for each location and category combination
                for (location in favoriteLocations) {
                    for (category in categories) {
                        val topic = "location_${location.latitude.toInt()}_${location.longitude.toInt()}_$category"
                        notificationTokenHelper.subscribeToTopic(topic)
                    }
                }
                
                logger.i(Logger.TAG_DEFAULT, "Subscribed to ${favoriteLocations.size * categories.size} location notification topics")
                Result.Success(Unit)
                
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "Error subscribing to location notifications", e)
                Result.Error(e)
            }
        }
    }
    
    override suspend fun unsubscribeFromLocationNotifications(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                logger.d(Logger.TAG_DEFAULT, "Unsubscribing from location notifications")
                
                // This would require tracking subscribed topics
                // For now, we'll implement a basic unsubscribe
                notificationTokenHelper.unsubscribeFromAllTopics()
                
                logger.i(Logger.TAG_DEFAULT, "Unsubscribed from location notifications")
                Result.Success(Unit)
                
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "Error unsubscribing from location notifications", e)
                Result.Error(e)
            }
        }
    }
    
    override suspend fun getMapConfiguration(): Result<MapConfiguration> {
        return withContext(Dispatchers.IO) {
            try {
                logger.d(Logger.TAG_DEFAULT, "Getting map configuration from Remote Config")
                
                val config = MapConfiguration(
                    basicRadiusMeters = remoteConfigManager.getMapBasicRadiusMeters().toInt(),
                    premiumRadiusMeters = remoteConfigManager.getMapPremiumRadiusMeters().toInt(),
                    clusterMinZoom = remoteConfigManager.getMapClusterMinZoom().toFloat(),
                    clusterMaxZoom = remoteConfigManager.getMapClusterMaxZoom().toFloat(),
                    availabilityThreshold = remoteConfigManager.getMapAvailabilityThreshold().toFloat(),
                    premiumMarkerEnabled = remoteConfigManager.isMapPremiumMarkerEnabled(),
                    earlyAccessMinutes = remoteConfigManager.getMapEarlyAccessMinutes().toInt(),
                    cacheMaxSizeMB = remoteConfigManager.getMapCacheMaxSizeMB().toInt(),
                    maxPostsPerQuery = remoteConfigManager.getMapMaxPostsPerQuery().toInt()
                )
                
                logger.i(Logger.TAG_DEFAULT, "Retrieved map configuration: Basic radius=${config.basicRadiusMeters}m, Premium radius=${config.premiumRadiusMeters}m")
                Result.Success(config)
                
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "Error getting map configuration", e)
                Result.Success(MapConfiguration()) // Return default configuration
            }
        }
    }
    
    override suspend fun cachePosts(
        posts: List<PostMapItem>,
        bounds: LocationBounds
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val cacheKey = "bounds_cache_${bounds.hashCode()}"
                cachePosts(cacheKey, posts)
                Result.Success(Unit)
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "Error caching posts", e)
                Result.Error(e)
            }
        }
    }
    
    override suspend fun getCachedPosts(bounds: LocationBounds): Result<List<PostMapItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val cacheKey = "bounds_cache_${bounds.hashCode()}"
                val cachedPosts = getCachedPosts(cacheKey)
                Result.Success(cachedPosts ?: emptyList())
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "Error getting cached posts", e)
                Result.Success(emptyList())
            }
        }
    }
    
    override suspend fun clearCache(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                postsCache.clear()
                clustersCache.clear()
                locationCache.clear()
                cacheTimestamps.clear()
                
                logger.i(Logger.TAG_DEFAULT, "Map cache cleared")
                Result.Success(Unit)
                
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "Error clearing cache", e)
                Result.Error(e)
            }
        }
    }
    
    // Private helper methods
    
    private fun buildNearbyPostsQuery(filter: MapFilter, geohashBounds: List<String>): Query {
        // Calculate cutoff time for max age filter using Date (Android-safe)
        val maxAgeMillis = filter.maxAgeHours * 60 * 60 * 1000L
        val cutoffTime = Date(System.currentTimeMillis() - maxAgeMillis)
        
        var query = firestore.collection(FirestoreConstants.COLLECTION_POSTS)
            .whereEqualTo(FirestoreConstants.FIELD_STATUS, "active")
            .whereGreaterThan(FirestoreConstants.FIELD_CREATED_AT, com.google.firebase.Timestamp(cutoffTime))
            .orderBy(FirestoreConstants.FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .limit(200) // Firestore rules hard limit for list queries
        
        // Add category filter if specified
        if (filter.categories.isNotEmpty()) {
            query = query.whereArrayContainsAny("category_en", filter.categories)
        }
        
        // Add availability filter if specified
        if (filter.availabilityOnly) {
            query = query.whereGreaterThanOrEqualTo("availability_percent", filter.minAvailabilityPercent)
        }
        
        return query
    }
    
    private fun buildBoundsQuery(bounds: LocationBounds, filter: MapFilter): Query {
        // Calculate cutoff time for max age filter using Date (Android-safe)
        val maxAgeMillis = filter.maxAgeHours * 60 * 60 * 1000L
        val cutoffTime = Date(System.currentTimeMillis() - maxAgeMillis)
        
        return firestore.collection(FirestoreConstants.COLLECTION_POSTS)
            .whereEqualTo(FirestoreConstants.FIELD_STATUS, "active")
            .whereGreaterThan(FirestoreConstants.FIELD_CREATED_AT, com.google.firebase.Timestamp(cutoffTime))
            .whereGreaterThanOrEqualTo("location.lat", bounds.southwest.latitude)
            .whereLessThanOrEqualTo("location.lat", bounds.northeast.latitude)
            .whereGreaterThanOrEqualTo("location.lng", bounds.southwest.longitude)
            .whereLessThanOrEqualTo("location.lng", bounds.northeast.longitude)
            .orderBy("location.lat")
            .orderBy(FirestoreConstants.FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .limit(200)
    }
    
    private fun mapDocumentToPostMapItem(data: Map<String, Any>, documentId: String): PostMapItem {
        val location = data["location"] as? Map<String, Any> ?: throw IllegalArgumentException("Missing location")
        val lat = (location["lat"] as? Number)?.toDouble() ?: throw IllegalArgumentException("Missing latitude")
        val lng = (location["lng"] as? Number)?.toDouble() ?: throw IllegalArgumentException("Missing longitude")
        
        val images = data[FirestoreConstants.FIELD_IMAGES] as? List<String> ?: emptyList()
        val categoriesEn = data["category_en"] as? List<String> ?: emptyList()
        val categoriesDe = data["category_de"] as? List<String> ?: emptyList()
        
        val createdAtTimestamp = data[FirestoreConstants.FIELD_CREATED_AT] as? com.google.firebase.Timestamp
        val createdAt = createdAtTimestamp?.toDate() ?: Date() // Direct Date conversion, Android-safe
        
        return PostMapItem(
            id = documentId,
            userId = data[FirestoreConstants.FIELD_OWNER_ID] as? String ?: "",
            location = PostLocation(lat, lng),
            imageUrl = images.firstOrNull() ?: "",
            categoryEn = categoriesEn.firstOrNull() ?: "other",
            categoryDe = categoriesDe.firstOrNull() ?: "Sonstiges",
            isPremiumOwner = (data["owner_premium"] as? Boolean) ?: false,
            ownerLevel = (data["owner_level"] as? Number)?.toInt() ?: 1,
            ownerFrameType = PremiumFrameType.NONE, // TODO: Map from data
            createdAt = createdAt,
            availabilityPercent = (data["availability_percent"] as? Number)?.toInt() ?: 100,
            isArchived = (data[FirestoreConstants.FIELD_STATUS] as? String) != "active",
            city = data["city"] as? String ?: "",
            description = data["description"] as? String ?: "",
            likesCount = (data["likes_count"] as? Number)?.toInt() ?: 0,
            commentsCount = (data["comments_count"] as? Number)?.toInt() ?: 0
        )
    }
    
    private fun applyAdditionalFilters(posts: List<PostMapItem>, filter: MapFilter): List<PostMapItem> {
        var filteredPosts = posts
        
        // Apply availability filter
        if (filter.availabilityOnly) {
            filteredPosts = filteredPosts.filter { it.availabilityPercent >= filter.minAvailabilityPercent }
        }
        
        // Apply category filter
        if (filter.categories.isNotEmpty()) {
            filteredPosts = filteredPosts.filter { post ->
                filter.categories.any { category -> post.categoryEn.equals(category, ignoreCase = true) }
            }
        }
        
        // Sort based on filter
        filteredPosts = when (filter.sortBy) {
            MapSortBy.DISTANCE -> filteredPosts.sortedBy { it.distanceFromUser }
            MapSortBy.DATE -> filteredPosts.sortedByDescending { it.createdAt.time } // Use .time for Date comparison
            MapSortBy.POPULARITY -> filteredPosts.sortedByDescending { it.likesCount + it.commentsCount }
            MapSortBy.AVAILABILITY -> filteredPosts.sortedByDescending { it.availabilityPercent }
        }
        
        return filteredPosts
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // Earth's radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
    
    private fun isLocationInBounds(location: PostLocation, bounds: LocationBounds): Boolean {
        return location.latitude >= bounds.southwest.latitude &&
                location.latitude <= bounds.northeast.latitude &&
                location.longitude >= bounds.southwest.longitude &&
                location.longitude <= bounds.northeast.longitude
    }
    
    private fun calculateClusterDistance(zoomLevel: Float): Double {
        // Cluster distance decreases as zoom level increases
        return when {
            zoomLevel <= 10 -> 5000.0 // 5km
            zoomLevel <= 12 -> 2000.0 // 2km
            zoomLevel <= 14 -> 1000.0 // 1km
            zoomLevel <= 16 -> 500.0  // 500m
            else -> 200.0             // 200m
        }
    }
    
    private fun getMarkerType(post: PostMapItem): MarkerType {
        return when {
            !post.isPremiumOwner -> MarkerType.BASIC
            post.ownerLevel >= 15 -> MarkerType.PREMIUM_CRYSTAL
            post.ownerLevel >= 1 -> MarkerType.PREMIUM_GOLD
            else -> MarkerType.PREMIUM_STANDARD
        }
    }
    
    private fun getClusterMarkerType(posts: List<PostMapItem>): MarkerType {
        val premiumCount = posts.count { it.isPremiumOwner }
        val totalCount = posts.size
        val premiumRatio = premiumCount.toFloat() / totalCount
        
        return when {
            premiumRatio >= 0.7 -> MarkerType.CLUSTER_PREMIUM
            premiumRatio >= 0.3 -> MarkerType.CLUSTER_STANDARD
            else -> MarkerType.CLUSTER_BASIC
        }
    }
    
    private fun cachePosts(cacheKey: String, posts: List<PostMapItem>) {
        postsCache[cacheKey] = posts
        cacheTimestamps[cacheKey] = System.currentTimeMillis()
    }
    
    private fun getCachedPosts(cacheKey: String): List<PostMapItem>? {
        val cacheTime = cacheTimestamps[cacheKey] ?: return null
        return if (System.currentTimeMillis() - cacheTime < cacheExpiryMs) {
            postsCache[cacheKey]
        } else {
            postsCache.remove(cacheKey)
            cacheTimestamps.remove(cacheKey)
            null
        }
    }
}
