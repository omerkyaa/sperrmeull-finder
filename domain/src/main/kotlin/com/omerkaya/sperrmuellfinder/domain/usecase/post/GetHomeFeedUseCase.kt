package com.omerkaya.sperrmuellfinder.domain.usecase.post

import androidx.paging.PagingData
import androidx.paging.filter
import androidx.paging.map
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.domain.manager.PremiumManager
import com.omerkaya.sperrmuellfinder.domain.model.Post
import com.omerkaya.sperrmuellfinder.core.model.PostLocation
import com.omerkaya.sperrmuellfinder.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case for getting the home feed posts.
 * Handles location-based filtering and premium radius limitations.
 */
class GetHomeFeedUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val premiumManager: PremiumManager,
    private val logger: Logger
) {

    /**
     * Gets posts for the home feed based on user location and premium status.
     * 
     * Basic users: 1.5km radius, sorted by newest first
     * Premium users: unlimited radius, sorted by distance (nearest first), then by newest
     * 
     * @param userLocation Current user location
     * @return Flow of PagingData containing posts with distance calculations and proper sorting
     */
    operator fun invoke(userLocation: PostLocation): Flow<PagingData<Post>> {
        logger.d(Logger.TAG_DEFAULT, "Getting home feed for location: ${userLocation.latitude}, ${userLocation.longitude}")
        
        val isPremium = premiumManager.isPremium.value
        
        // Determine radius based on premium status
        val radiusMeters = if (isPremium) {
            PREMIUM_RADIUS_METERS // 20km for premium users
        } else {
            BASIC_RADIUS_METERS // 1.5km for basic users
        }
        
        logger.d(Logger.TAG_DEFAULT, "Using radius: ${radiusMeters}m (Premium: $isPremium)")
        
        // Get posts from repository with location filtering and sorting
        return if (isPremium) {
            // Premium: Sort by distance (nearest first), then by newest
            postRepository.getPostsNearUserSortedByDistance(
                userLocation = userLocation,
                radiusMeters = radiusMeters
            ).map { pagingData ->
                pagingData.map { post ->
                    // Calculate and add distance to post (only for posts with valid location)
                    post.location?.let { location ->
                        val distance = calculateDistance(
                            userLocation.latitude, userLocation.longitude,
                            location.latitude, location.longitude
                        )
                        post.copy(distanceFromUser = distance)
                    } ?: post // Keep post without distance if location is null
                }
            }
        } else {
            // Basic: Sort by newest first only
            postRepository.getPostsNearUserSortedByDate(
                userLocation = userLocation,
                radiusMeters = radiusMeters
            ).map { pagingData ->
                val visibilityCutoffMs = System.currentTimeMillis() - (EARLY_ACCESS_MINUTES * 60 * 1000)
                pagingData
                    .filter { post -> post.createdAt.time <= visibilityCutoffMs }
                    .map { post ->
                    // Calculate and add distance to post for display (only for posts with valid location)
                    post.location?.let { location ->
                        val distance = calculateDistance(
                            userLocation.latitude, userLocation.longitude,
                            location.latitude, location.longitude
                        )
                        post.copy(distanceFromUser = distance)
                    } ?: post // Keep post without distance if location is null
                    }
            }
        }
    }

    /**
     * Calculates distance between two points using Haversine formula.
     * 
     * @param lat1 User latitude
     * @param lon1 User longitude
     * @param lat2 Post latitude
     * @param lon2 Post longitude
     * @return Distance in meters
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // Earth radius in meters
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        
        return earthRadius * c
    }

    companion object {
        private const val BASIC_RADIUS_METERS = PremiumManager.BASIC_RADIUS_METERS
        private const val PREMIUM_RADIUS_METERS = Int.MAX_VALUE
        private const val EARLY_ACCESS_MINUTES = PremiumManager.EARLY_ACCESS_MINUTES.toLong()
    }
}
