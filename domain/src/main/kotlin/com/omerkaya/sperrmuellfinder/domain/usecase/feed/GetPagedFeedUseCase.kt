package com.omerkaya.sperrmuellfinder.domain.usecase.feed

import androidx.paging.PagingData
import com.omerkaya.sperrmuellfinder.domain.model.Post
import com.omerkaya.sperrmuellfinder.domain.repository.FeedRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 🏠 GET PAGED FEED USE CASE - SperrmüllFinder
 * Rules.md compliant - Clean Architecture domain layer
 * 
 * Features:
 * - Paging 3 integration for infinite scroll
 * - Location-based filtering with radius
 * - Premium vs Basic radius enforcement
 * - Real-time post updates
 */
class GetPagedFeedUseCase @Inject constructor(
    private val feedRepository: FeedRepository
) {
    
    /**
     * Get paginated feed posts for the current user.
     * 
     * @param userId Current user ID
     * @param radiusMeters Search radius in meters (Basic: 1500m, Premium: unlimited)
     * @param userLatitude User's current latitude (optional)
     * @param userLongitude User's current longitude (optional)
     * @return Flow of PagingData containing posts
     */
    operator fun invoke(
        userId: String,
        isPremium: Boolean,
        earlyAccessMinutes: Int,
        radiusMeters: Int = 1500,
        userLatitude: Double? = null,
        userLongitude: Double? = null
    ): Flow<PagingData<Post>> {
        return feedRepository.getPagedFeed(
            userId = userId,
            isPremium = isPremium,
            earlyAccessMinutes = earlyAccessMinutes,
            radiusMeters = radiusMeters,
            userLatitude = userLatitude,
            userLongitude = userLongitude
        )
    }
}
