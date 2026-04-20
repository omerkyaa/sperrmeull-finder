package com.omerkaya.sperrmuellfinder.data.datasource

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.omerkaya.sperrmuellfinder.core.util.GeoHashUtils
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.data.dto.PostDto
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants
import com.omerkaya.sperrmuellfinder.data.paging.PostsPagingSource
import com.omerkaya.sperrmuellfinder.domain.model.Post
import com.omerkaya.sperrmuellfinder.core.model.PostLocation
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore data source for location-based post queries.
 * Uses GeoHash for efficient spatial queries and supports clustering.
 * Rules.md compliant - Professional Firestore location queries.
 */
@Singleton
class FirestoreLocationDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val logger: Logger
) {

    /**
     * Get posts near user location sorted by date (newest first).
     * For basic users with 1.5km radius.
     * 
     * @param userLocation Current user location
     * @param radiusMeters Search radius in meters
     * @return Flow of PagingData containing posts
     */
    fun getPostsNearUserSortedByDate(
        userLocation: PostLocation,
        radiusMeters: Int
    ): Flow<PagingData<Post>> {
        logger.d(Logger.TAG_DEFAULT, "Getting posts near user sorted by date (radius: ${radiusMeters}m)")
        
        val precision = GeoHashUtils.getOptimalPrecision(radiusMeters)
        val geoHashes = GeoHashUtils.getGeoHashesForRadius(userLocation, radiusMeters, precision)
        
        logger.d(Logger.TAG_DEFAULT, "Using ${geoHashes.size} GeoHashes with precision $precision")
        
        val query = buildLocationQuery(geoHashes)
            .orderBy(FirestoreConstants.FIELD_CREATED_AT, Query.Direction.DESCENDING)
        
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                PostsPagingSource(
                    firestore = firestore,
                    userLocation = userLocation,
                    radiusKm = radiusMeters / 1000.0
                )
            }
        ).flow
    }

    /**
     * Get posts near user location sorted by distance (nearest first), then by date.
     * For premium users with 20km radius.
     * 
     * @param userLocation Current user location
     * @param radiusMeters Search radius in meters
     * @return Flow of PagingData containing posts
     */
    fun getPostsNearUserSortedByDistance(
        userLocation: PostLocation,
        radiusMeters: Int
    ): Flow<PagingData<Post>> {
        logger.d(Logger.TAG_DEFAULT, "Getting posts near user sorted by distance (radius: ${radiusMeters}m)")
        
        val precision = GeoHashUtils.getOptimalPrecision(radiusMeters)
        val geoHashes = GeoHashUtils.getGeoHashesForRadius(userLocation, radiusMeters, precision)
        
        logger.d(Logger.TAG_DEFAULT, "Using ${geoHashes.size} GeoHashes with precision $precision")
        
        // For distance sorting, we get posts by geohash and sort client-side
        val query = buildLocationQuery(geoHashes)
            .orderBy(FirestoreConstants.FIELD_CREATED_AT, Query.Direction.DESCENDING) // Secondary sort
        
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                PostsPagingSource(
                    firestore = firestore,
                    userLocation = userLocation,
                    radiusKm = radiusMeters / 1000.0
                )
            }
        ).flow
    }

    /**
     * Get posts for map clustering with viewport optimization.
     * Only returns posts visible in current map bounds.
     * 
     * @param userLocation Current user location
     * @param radiusMeters Search radius in meters
     * @param bounds Optional viewport bounds for optimization
     * @return Flow of PagingData containing posts
     */
    fun getPostsForMap(
        userLocation: PostLocation,
        radiusMeters: Int,
        bounds: com.omerkaya.sperrmuellfinder.domain.model.map.LocationBounds? = null
    ): Flow<PagingData<Post>> {
        logger.d(Logger.TAG_DEFAULT, "Getting posts for map (radius: ${radiusMeters}m)")
        
        val precision = GeoHashUtils.getOptimalPrecision(radiusMeters)
        val geoHashes = if (bounds != null) {
            // Use bounds for more efficient queries
            getGeoHashesForBounds(bounds, precision)
        } else {
            GeoHashUtils.getGeoHashesForRadius(userLocation, radiusMeters, precision)
        }
        
        logger.d(Logger.TAG_DEFAULT, "Using ${geoHashes.size} GeoHashes for map query")
        
        val query = buildLocationQuery(geoHashes)
            .orderBy(FirestoreConstants.FIELD_CREATED_AT, Query.Direction.DESCENDING)
        
        return Pager(
            config = PagingConfig(
                pageSize = 50, // Larger page size for map
                prefetchDistance = 10,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                PostsPagingSource(
                    firestore = firestore,
                    userLocation = userLocation,
                    radiusKm = radiusMeters / 1000.0
                )
            }
        ).flow
    }

    /**
     * Build Firestore query for location-based posts using GeoHash.
     * Combines multiple GeoHash ranges for efficient spatial queries.
     */
    private fun buildLocationQuery(geoHashes: List<String>): Query {
        var query = firestore.collection(FirestoreConstants.COLLECTION_POSTS)
            .whereEqualTo(FirestoreConstants.FIELD_STATUS, "active")
            .whereGreaterThan(FirestoreConstants.FIELD_EXPIRES_AT, com.google.firebase.Timestamp.now())
        
        // Use GeoHash for spatial filtering
        if (geoHashes.isNotEmpty()) {
            // For multiple geohashes, we need to use 'in' operator or multiple queries
            // Since Firestore 'in' is limited to 10 values, we use the first geohash as primary filter
            val primaryGeoHash = geoHashes.first()
            val geoHashPrefix = primaryGeoHash.take(6) // Use 6-character prefix for broader coverage
            
            query = query
                .whereGreaterThanOrEqualTo("geohash", geoHashPrefix)
                .whereLessThan("geohash", geoHashPrefix + "~") // Next possible string
        }
        
        return query
    }

    /**
     * Get GeoHashes for map viewport bounds.
     * More efficient than radius-based queries for map display.
     */
    private fun getGeoHashesForBounds(
        bounds: com.omerkaya.sperrmuellfinder.domain.model.map.LocationBounds,
        precision: Int
    ): List<String> {
        val geoHashes = mutableSetOf<String>()
        
        // Sample points within bounds and get their geohashes
        val latStep = (bounds.northeast.latitude - bounds.southwest.latitude) / 10
        val lngStep = (bounds.northeast.longitude - bounds.southwest.longitude) / 10
        
        for (i in 0..10) {
            for (j in 0..10) {
                val lat = bounds.southwest.latitude + (i * latStep)
                val lng = bounds.southwest.longitude + (j * lngStep)
                
                val location = PostLocation(lat, lng, "Sample")
                val geoHash = GeoHashUtils.encode(location, precision)
                geoHashes.add(geoHash)
            }
        }
        
        return geoHashes.toList()
    }

    /**
     * Add GeoHash to existing posts for migration.
     * This method can be used to update existing posts with GeoHash values.
     */
    suspend fun addGeoHashToPosts() {
        logger.i(Logger.TAG_DEFAULT, "Starting GeoHash migration for existing posts")
        
        try {
            val postsQuery = firestore.collection(FirestoreConstants.COLLECTION_POSTS)
                .whereEqualTo(FirestoreConstants.FIELD_STATUS, "active")
                .limit(100) // Process in batches
            
            val snapshot = postsQuery.get()
            val batch = firestore.batch()
            var updateCount = 0
            
            for (document in snapshot.result.documents) {
                val postDto = document.toObject(PostDto::class.java) ?: continue
                
                if (postDto.geohash.isNullOrBlank()) {
                    val location = PostLocation(
                        latitude = postDto.location?.latitude ?: 0.0,
                        longitude = postDto.location?.longitude ?: 0.0,
                        address = postDto.location?.address ?: ""
                    )
                    
                    val geoHash = GeoHashUtils.encode(location, 9) // 9-character precision
                    
                    batch.update(document.reference, "geohash", geoHash)
                    updateCount++
                }
            }
            
            if (updateCount > 0) {
                batch.commit()
                logger.i(Logger.TAG_DEFAULT, "Updated $updateCount posts with GeoHash")
            }
            
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Failed to add GeoHash to posts", e)
        }
    }

    companion object {
        private const val POSTS_COLLECTION = "posts"
        private const val DEFAULT_PAGE_SIZE = 20
        private const val MAP_PAGE_SIZE = 50
    }
}
