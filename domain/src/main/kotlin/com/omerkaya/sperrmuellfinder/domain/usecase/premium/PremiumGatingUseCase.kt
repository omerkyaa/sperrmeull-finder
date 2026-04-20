package com.omerkaya.sperrmuellfinder.domain.usecase.premium

import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.domain.manager.PremiumManager
import com.omerkaya.sperrmuellfinder.domain.model.premium.PremiumFeature
import com.omerkaya.sperrmuellfinder.domain.model.premium.PremiumGatingResult
import javax.inject.Inject

/**
 * Use case for checking access to various premium features.
 * Provides a clear interface for UI and other domain layers to query premium feature access.
 * 
 * According to rules.md:
 * - Premium gating rules are centralized here
 * - Basic vs Premium feature access is clearly defined
 * - All gating decisions go through PremiumManager
 */
class PremiumGatingUseCase @Inject constructor(
    private val premiumManager: PremiumManager,
    private val logger: Logger
) {

    /**
     * Check if the user has access to a specific premium feature.
     * 
     * @param feature The PremiumFeature to check
     * @return PremiumGatingResult indicating access status and potential messages
     */
    operator fun invoke(feature: PremiumFeature): PremiumGatingResult {
        val result = premiumManager.checkFeatureAccess(feature)
        
        when (result) {
            is PremiumGatingResult.Allowed -> {
                logger.d(Logger.TAG_PREMIUM, "Feature access granted: ${feature.name}")
            }
            is PremiumGatingResult.Blocked -> {
                logger.d(Logger.TAG_PREMIUM, "Feature access blocked: ${feature.name} - ${result.reason}")
            }
        }
        
        return result
    }

    /**
     * Check if the user has access to a specific map radius.
     * Basic users are limited to BASIC_RADIUS_METERS.
     * 
     * @param requestedRadiusMeters The radius in meters the user is trying to use
     * @return PremiumGatingResult indicating access status
     */
    fun checkRadiusAccess(requestedRadiusMeters: Int): PremiumGatingResult {
        val maxRadius = premiumManager.getMaxMapRadius()
        
        return if (maxRadius == -1) {
            // Unlimited radius for premium users
            logger.d(Logger.TAG_PREMIUM, "Unlimited radius access granted")
            PremiumGatingResult.Allowed
        } else if (requestedRadiusMeters <= maxRadius) {
            // Within allowed radius for basic users
            logger.d(Logger.TAG_PREMIUM, "Radius access granted: ${requestedRadiusMeters}m <= ${maxRadius}m")
            PremiumGatingResult.Allowed
        } else {
            // Exceeds basic user limit
            logger.d(Logger.TAG_PREMIUM, "Radius access blocked: ${requestedRadiusMeters}m > ${maxRadius}m")
            PremiumGatingResult.Blocked(
                feature = PremiumFeature.UNLIMITED_RADIUS,
                reason = "Basic users are limited to ${maxRadius / 1000}km radius",
                upgradeMessage = "Upgrade to Premium for unlimited map radius"
            )
        }
    }

    /**
     * Check if the user has early access to new posts.
     * Premium users get EARLY_ACCESS_MINUTES minutes early access.
     * 
     * @return PremiumGatingResult indicating early access status
     */
    fun checkEarlyAccess(): PremiumGatingResult {
        return if (premiumManager.hasEarlyAccess()) {
            logger.d(Logger.TAG_PREMIUM, "Early access granted: ${PremiumManager.EARLY_ACCESS_MINUTES} minutes")
            PremiumGatingResult.Allowed
        } else {
            logger.d(Logger.TAG_PREMIUM, "Early access blocked: Premium required")
            PremiumGatingResult.Blocked(
                feature = PremiumFeature.EARLY_ACCESS,
                reason = "Early access is premium-only",
                upgradeMessage = "Upgrade to Premium for ${PremiumManager.EARLY_ACCESS_MINUTES} minutes early access to new posts"
            )
        }
    }

    /**
     * Check if the user has access to free post extensions.
     * Premium users get FREE_PREMIUM_EXTEND_HOURS hours free extension.
     * 
     * @return PremiumGatingResult indicating free extension status
     */
    fun checkFreePostExtension(): PremiumGatingResult {
        return if (premiumManager.hasFreePostExtension()) {
            logger.d(Logger.TAG_PREMIUM, "Free post extension granted: ${PremiumManager.FREE_PREMIUM_EXTEND_HOURS} hours")
            PremiumGatingResult.Allowed
        } else {
            logger.d(Logger.TAG_PREMIUM, "Free post extension blocked: Premium required")
            PremiumGatingResult.Blocked(
                feature = PremiumFeature.FREE_POST_EXTENSION,
                reason = "Free post extension is premium-only",
                upgradeMessage = "Upgrade to Premium for ${PremiumManager.FREE_PREMIUM_EXTEND_HOURS} hours free post extension"
            )
        }
    }

    /**
     * Check if the user can see availability percentages.
     * This is a premium-only feature.
     * 
     * @return PremiumGatingResult indicating availability percentage access
     */
    fun checkAvailabilityPercentagesAccess(): PremiumGatingResult {
        return if (premiumManager.canSeeAvailabilityPercentages()) {
            logger.d(Logger.TAG_PREMIUM, "Availability percentages access granted")
            PremiumGatingResult.Allowed
        } else {
            logger.d(Logger.TAG_PREMIUM, "Availability percentages access blocked: Premium required")
            PremiumGatingResult.Blocked(
                feature = PremiumFeature.AVAILABILITY_PERCENTAGES,
                reason = "Availability percentages are premium-only",
                upgradeMessage = "Upgrade to Premium to see availability percentages"
            )
        }
    }

    /**
     * Check if the user can see their leaderboard position.
     * This is a premium-only feature.
     * 
     * @return PremiumGatingResult indicating leaderboard position access
     */
    fun checkLeaderboardPositionAccess(): PremiumGatingResult {
        return if (premiumManager.canSeeLeaderboardPosition()) {
            logger.d(Logger.TAG_PREMIUM, "Leaderboard position access granted")
            PremiumGatingResult.Allowed
        } else {
            logger.d(Logger.TAG_PREMIUM, "Leaderboard position access blocked: Premium required")
            PremiumGatingResult.Blocked(
                feature = PremiumFeature.LEADERBOARD_POSITION,
                reason = "Leaderboard position is premium-only",
                upgradeMessage = "Upgrade to Premium to see your leaderboard position"
            )
        }
    }

    /**
     * Check if the user has access to premium filters.
     * This includes advanced search and filtering options.
     * 
     * @return PremiumGatingResult indicating premium filters access
     */
    fun checkPremiumFiltersAccess(): PremiumGatingResult {
        return invoke(PremiumFeature.PREMIUM_FILTERS)
    }

    /**
     * Check if the user has access to favorite region notifications.
     * Premium users can set up alerts for their favorite regions/categories.
     * 
     * @return PremiumGatingResult indicating favorite notifications access
     */
    fun checkFavoriteNotificationsAccess(): PremiumGatingResult {
        return invoke(PremiumFeature.FAVORITE_NOTIFICATIONS)
    }

    /**
     * Check if the user has access to full archive details.
     * Basic users see thumbnail-only, Premium users see full details.
     * 
     * @return PremiumGatingResult indicating archive detail access
     */
    fun checkArchiveFullDetailAccess(): PremiumGatingResult {
        return invoke(PremiumFeature.ARCHIVE_FULL_DETAIL)
    }

    /**
     * Check if the user has access to premium marker styles.
     * Premium users get enhanced marker styles and animations.
     * 
     * @return PremiumGatingResult indicating premium markers access
     */
    fun checkPremiumMarkersAccess(): PremiumGatingResult {
        return invoke(PremiumFeature.PREMIUM_MARKERS)
    }

    /**
     * Get a user-friendly message for a gating result.
     * These messages should be localized in the UI layer.
     * 
     * @param result The gating result
     * @return Message for display to user
     */
    fun getGatingMessage(result: PremiumGatingResult): String {
        return when (result) {
            is PremiumGatingResult.Allowed -> "Feature available"
            is PremiumGatingResult.Blocked -> result.upgradeMessage
        }
    }

    /**
     * Check if multiple features are accessible.
     * Useful for bulk feature checking.
     * 
     * @param features List of features to check
     * @return Map of feature to gating result
     */
    fun checkMultipleFeatures(features: List<PremiumFeature>): Map<PremiumFeature, PremiumGatingResult> {
        return features.associateWith { feature ->
            invoke(feature)
        }
    }

    /**
     * Get all blocked features for the current user.
     * Useful for showing upgrade prompts.
     * 
     * @return List of blocked premium features
     */
    fun getBlockedFeatures(): List<PremiumFeature> {
        return PremiumFeature.entries.filter { feature ->
            invoke(feature) is PremiumGatingResult.Blocked
        }
    }

    /**
     * Get all available features for the current user.
     * 
     * @return List of available premium features
     */
    fun getAvailableFeatures(): List<PremiumFeature> {
        return PremiumFeature.entries.filter { feature ->
            invoke(feature) is PremiumGatingResult.Allowed
        }
    }

    /**
     * Check if any premium features are blocked.
     * Useful for showing upgrade CTAs.
     * 
     * @return True if any premium features are blocked
     */
    fun hasBlockedFeatures(): Boolean {
        return getBlockedFeatures().isNotEmpty()
    }

    /**
     * Get premium status summary for UI display.
     * 
     * @return Map containing premium status information
     */
    fun getPremiumStatusSummary(): Map<String, Any> {
        val isPremium = premiumManager.isPremium.value
        val isExpiringSoon = premiumManager.isPremiumExpiringSoon()
        val daysUntilExpiration = premiumManager.getDaysUntilExpiration() ?: -1
        val isInTrial = premiumManager.isInTrial()
        val isInGracePeriod = premiumManager.isInGracePeriod()
        
        return mapOf(
            "isPremium" to isPremium as Any,
            "isExpiringSoon" to isExpiringSoon as Any,
            "daysUntilExpiration" to daysUntilExpiration as Any,
            "isInTrial" to isInTrial as Any,
            "isInGracePeriod" to isInGracePeriod as Any,
            "blockedFeaturesCount" to getBlockedFeatures().size as Any,
            "availableFeaturesCount" to getAvailableFeatures().size as Any
        )
    }
}