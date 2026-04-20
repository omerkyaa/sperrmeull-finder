package com.omerkaya.sperrmuellfinder.domain.usecase

import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.manager.PremiumManager
import com.omerkaya.sperrmuellfinder.domain.model.MapFilter
import com.omerkaya.sperrmuellfinder.domain.model.PostMapItem
import com.omerkaya.sperrmuellfinder.domain.model.UserLocation
import com.omerkaya.sperrmuellfinder.domain.repository.MapRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.collect
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Use case for getting nearby posts with proper Basic/Premium gating.
 * Enforces radius limits, filter restrictions, and premium features.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetNearbyPostsUseCase @Inject constructor(
    private val mapRepository: MapRepository,
    private val premiumManager: PremiumManager,
    private val logger: Logger
) {
    
    /**
     * Get nearby posts with automatic premium gating and filter validation.
     * 
     * @param userLocation Current user location
     * @param requestedFilter Requested filter configuration
     * @return Flow of nearby posts with premium restrictions applied
     */
    operator fun invoke(
        userLocation: UserLocation,
        requestedFilter: MapFilter
    ): Flow<Result<List<PostMapItem>>> {
        
        return combine(
            premiumManager.isPremium,
            premiumManager.premiumEntitlement
        ) { isPremium, entitlement ->
            Pair(isPremium, entitlement)
        }.flatMapLatest { (isPremium, entitlement) ->
            
            flow {
                try {
                    logger.d(Logger.TAG_DEFAULT, "Getting nearby posts - Premium: $isPremium, Location: ${userLocation.latitude}, ${userLocation.longitude}")
                    
                    // Validate and adjust filter based on premium status
                    val validatedFilter = validateFilter(requestedFilter, isPremium, entitlement)
                    
                    // Log filter application
                    logger.d(Logger.TAG_DEFAULT, "Applied filter - Radius: ${validatedFilter.radiusMeters}m, Categories: ${validatedFilter.categories.size}")
                    
                    // Get posts from repository and process them
                    mapRepository.getNearbyPosts(userLocation, validatedFilter).collect { posts ->
                        
                        // Apply additional premium processing
                        val processedPosts = if (isPremium && entitlement.isActive) {
                            applyPremiumEnhancements(posts)
                        } else {
                            applyBasicRestrictions(posts)
                        }
                        
                        logger.i(Logger.TAG_DEFAULT, "Retrieved ${processedPosts.size} nearby posts")
                        emit(Result.Success(processedPosts))
                    }
                    
                } catch (exception: Exception) {
                    logger.e(Logger.TAG_DEFAULT, "Error getting nearby posts", exception)
                    emit(Result.Error(exception))
                }
            }
            
        }
    }
    
    /**
     * Get nearby posts with bounds-based query for viewport optimization.
     * 
     * @param bounds Map viewport bounds
     * @param requestedFilter Requested filter configuration
     * @return Flow of posts within bounds
     */
    fun getPostsInBounds(
        bounds: com.omerkaya.sperrmuellfinder.domain.model.map.LocationBounds,
        requestedFilter: MapFilter
    ): Flow<Result<List<PostMapItem>>> {
        
        return combine(
            premiumManager.isPremium,
            premiumManager.premiumEntitlement
        ) { isPremium, entitlement ->
            Pair(isPremium, entitlement)
        }.flatMapLatest { (isPremium, entitlement) ->
            
            flow {
                try {
                    logger.d(Logger.TAG_DEFAULT, "Getting posts in bounds - Premium: $isPremium")
                    
                    // Validate filter
                    val validatedFilter = validateFilter(requestedFilter, isPremium, entitlement)
                    
                    // Get posts in bounds
                    mapRepository.getPostsInBounds(bounds, validatedFilter).collect { posts ->
                        
                        val processedPosts = if (isPremium && entitlement.isActive) {
                            applyPremiumEnhancements(posts)
                        } else {
                            applyBasicRestrictions(posts)
                        }
                        
                        logger.i(Logger.TAG_DEFAULT, "Retrieved ${processedPosts.size} posts in bounds")
                        emit(Result.Success(processedPosts))
                    }
                    
                } catch (exception: Exception) {
                    logger.e(Logger.TAG_DEFAULT, "Error getting posts in bounds", exception)
                    emit(Result.Error(exception))
                }
            }
            
        }
    }
    
    /**
     * Validate and adjust filter based on user's premium status and entitlements.
     */
    private fun validateFilter(
        requestedFilter: MapFilter,
        isPremium: Boolean,
        entitlement: com.omerkaya.sperrmuellfinder.domain.model.premium.PremiumEntitlement
    ): MapFilter {
        
        return if (isPremium && entitlement.isActive) {
            // Premium users: validate against premium limits
            requestedFilter.copy(
                radiusMeters = minOf(requestedFilter.radiusMeters, MapFilter.PREMIUM_MAX_RADIUS_METERS),
                showPremiumHighlights = true
            )
        } else {
            // Basic users: apply strict restrictions
            requestedFilter.toBasicFilter()
        }
    }
    
    /**
     * Apply premium enhancements to posts.
     */
    private fun applyPremiumEnhancements(posts: List<PostMapItem>): List<PostMapItem> {
        return posts.map { post ->
            post.copy(
                // Premium users see all availability data
                availabilityPercent = post.availabilityPercent
            )
        }.sortedBy { it.distanceFromUser } // Premium gets distance-based sorting
    }
    
    /**
     * Apply basic user restrictions to posts.
     */
    private fun applyBasicRestrictions(posts: List<PostMapItem>): List<PostMapItem> {
        return posts.map { post ->
            post.copy(
                // Basic users don't see availability percentages
                availabilityPercent = if (post.availabilityPercent > 50) 100 else 0
            )
        }.sortedByDescending { it.createdAt } // Basic gets date-based sorting
    }
    
    /**
     * Check if user can access premium map features.
     */
    fun canAccessPremiumFeatures(): Boolean {
        val isPremium = premiumManager.isPremium.value
        val entitlement = premiumManager.premiumEntitlement.value
        return isPremium && entitlement.isActive
    }
    
    /**
     * Get maximum allowed radius for current user.
     */
    fun getMaxAllowedRadius(): Int {
        return if (canAccessPremiumFeatures()) {
            MapFilter.PREMIUM_MAX_RADIUS_METERS
        } else {
            MapFilter.BASIC_MAX_RADIUS_METERS
        }
    }
    
    /**
     * Check if user can use category filters.
     */
    fun canUseCategoryFilters(): Boolean {
        return canAccessPremiumFeatures()
    }
    
    /**
     * Check if user can see availability percentages.
     */
    fun canSeeAvailability(): Boolean {
        return canAccessPremiumFeatures()
    }
}
