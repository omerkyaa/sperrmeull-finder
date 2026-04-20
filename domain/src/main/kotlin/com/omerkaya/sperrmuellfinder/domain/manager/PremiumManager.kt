package com.omerkaya.sperrmuellfinder.domain.manager

import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.domain.model.premium.PremiumEntitlement
import com.omerkaya.sperrmuellfinder.domain.model.premium.PremiumFeature
import com.omerkaya.sperrmuellfinder.domain.model.premium.PremiumGatingResult
import com.omerkaya.sperrmuellfinder.domain.model.premium.XpBoostConfig
import com.omerkaya.sperrmuellfinder.domain.repository.PremiumRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the user's premium status and provides access control for premium features.
 * This class acts as the single source of truth for premium entitlement.
 * 
 * According to rules.md:
 * - RevenueCat is the single source of truth for entitlements
 * - Firestore sync is informational only
 * - Premium gating rules are enforced here
 * - XP boost calculations are handled here
 */
@Singleton
class PremiumManager @Inject constructor(
    private val premiumRepository: PremiumRepository,
    private val logger: Logger,
    private val applicationScope: CoroutineScope
) {

    private val _premiumEntitlement = MutableStateFlow(
        PremiumEntitlement(
            isActive = false,
            status = com.omerkaya.sperrmuellfinder.domain.model.premium.PremiumStatus.NEVER_PURCHASED
        )
    )
    val premiumEntitlement: StateFlow<PremiumEntitlement> = _premiumEntitlement.asStateFlow()

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    init {
        observePremiumEntitlement()
        logger.d(Logger.TAG_PREMIUM, "PremiumManager initialized")
    }

    /**
     * Observe premium entitlement changes from RevenueCat
     */
    private fun observePremiumEntitlement() {
        applicationScope.launch {
            premiumRepository.observePremiumEntitlement()
                .catch { exception ->
                    logger.e(Logger.TAG_PREMIUM, "Error observing premium entitlement, falling back to cache", exception)
                    handleEntitlementFallback()
                }
                .collect { entitlement ->
                    val previousEntitlement = _premiumEntitlement.value
                    _premiumEntitlement.value = entitlement
                    _isPremium.value = entitlement.isAccessible()
                    
                    logger.i(Logger.TAG_PREMIUM, "Premium entitlement updated: isActive=${entitlement.isActive}, Status=${entitlement.status}")
                    
                    // Sync with Firestore for informational purposes
                    syncPremiumStatusWithFirestore(entitlement)
                    
                    // Track entitlement changes for analytics
                    trackEntitlementChange(previousEntitlement, entitlement)
                }
        }
    }

    /**
     * Handle fallback when entitlement observation fails
     */
    private suspend fun handleEntitlementFallback() {
        try {
            val cachedEntitlement = premiumRepository.getCachedPremiumEntitlement()
            _premiumEntitlement.value = cachedEntitlement
            _isPremium.value = cachedEntitlement.isAccessible()
            logger.w(Logger.TAG_PREMIUM, "Using cached premium status as fallback: isActive=${cachedEntitlement.isActive}")
        } catch (e: Exception) {
            logger.e(Logger.TAG_PREMIUM, "Failed to get cached premium status", e)
        }
    }

    /**
     * Sync premium status with Firestore for informational purposes
     */
    private suspend fun syncPremiumStatusWithFirestore(entitlement: PremiumEntitlement) {
        try {
            premiumRepository.syncPremiumStatusWithFirestore(entitlement)
            logger.d(Logger.TAG_PREMIUM, "Premium status synced with Firestore")
        } catch (e: Exception) {
            logger.e(Logger.TAG_PREMIUM, "Failed to sync premium status with Firestore", e)
        }
    }

    /**
     * Track entitlement changes for analytics
     */
    private fun trackEntitlementChange(previous: PremiumEntitlement, current: PremiumEntitlement) {
        // Premium activated
        if (!previous.isActive && current.isActive) {
            logger.i(Logger.TAG_PREMIUM, "Premium activated: ${current.type}")
            // TODO: Analytics event: premiumActivated(current.type, current.originalPurchaseDate)
        }
        
        // Premium deactivated
        if (previous.isActive && !current.isActive) {
            logger.i(Logger.TAG_PREMIUM, "Premium deactivated: ${previous.type}")
            // TODO: Analytics event: premiumDeactivated(previous.type, current.expirationDate)
        }
        
        // Premium renewed
        if (previous.isActive && current.isActive && previous.expirationDate != current.expirationDate) {
            logger.i(Logger.TAG_PREMIUM, "Premium renewed: ${current.type}")
            // TODO: Analytics event: premiumRenewed(current.type, current.expirationDate)
        }
        
        // Trial started
        if (!previous.isInTrial && current.isInTrial) {
            logger.i(Logger.TAG_PREMIUM, "Premium trial started: ${current.type}")
            // TODO: Analytics event: premiumTrialStarted(current.type, current.trialEndDate)
        }
        
        // Grace period started
        if (!previous.isInGracePeriod && current.isInGracePeriod) {
            logger.w(Logger.TAG_PREMIUM, "Premium grace period started: ${current.type}")
            // TODO: Analytics event: premiumGracePeriodStarted(current.type, current.gracePeriodEndDate)
        }
    }

    /**
     * Check if the user has access to a specific premium feature.
     * This is the main gating method used throughout the app.
     * 
     * @param feature The PremiumFeature to check
     * @return PremiumGatingResult indicating access status and potential messages
     */
    fun checkFeatureAccess(feature: PremiumFeature): PremiumGatingResult {
        val currentEntitlement = _premiumEntitlement.value
        val isPremiumActive = currentEntitlement.isAccessible()
        
        return when (feature) {
            PremiumFeature.UNLIMITED_RADIUS -> {
                if (isPremiumActive) {
                    PremiumGatingResult.Allowed
                } else {
                    PremiumGatingResult.Blocked(
                        feature = feature,
                        reason = "Basic users are limited to ${BASIC_RADIUS_METERS / 1000}km radius",
                        upgradeMessage = "Upgrade to Premium for unlimited map radius"
                    )
                }
            }
            
            PremiumFeature.AVAILABILITY_PERCENTAGES -> {
                if (isPremiumActive) {
                    PremiumGatingResult.Allowed
                } else {
                    PremiumGatingResult.Blocked(
                        feature = feature,
                        reason = "Availability percentages are premium-only",
                        upgradeMessage = "Upgrade to Premium to see availability percentages"
                    )
                }
            }
            
            PremiumFeature.PREMIUM_FILTERS -> {
                if (isPremiumActive) {
                    PremiumGatingResult.Allowed
                } else {
                    PremiumGatingResult.Blocked(
                        feature = feature,
                        reason = "Advanced filters are premium-only",
                        upgradeMessage = "Upgrade to Premium for advanced search filters"
                    )
                }
            }
            
            PremiumFeature.FAVORITE_NOTIFICATIONS -> {
                if (isPremiumActive) {
                    PremiumGatingResult.Allowed
                } else {
                    PremiumGatingResult.Blocked(
                        feature = feature,
                        reason = "Favorite region alerts are premium-only",
                        upgradeMessage = "Upgrade to Premium for favorite region notifications"
                    )
                }
            }
            
            PremiumFeature.EARLY_ACCESS -> {
                if (isPremiumActive) {
                    PremiumGatingResult.Allowed
                } else {
                    PremiumGatingResult.Blocked(
                        feature = feature,
                        reason = "Early access is premium-only",
                        upgradeMessage = "Upgrade to Premium for ${EARLY_ACCESS_MINUTES} minutes early access to new posts"
                    )
                }
            }
            
            PremiumFeature.ARCHIVE_FULL_DETAIL -> {
                if (isPremiumActive) {
                    PremiumGatingResult.Allowed
                } else {
                    PremiumGatingResult.Blocked(
                        feature = feature,
                        reason = "Full archive details are premium-only",
                        upgradeMessage = "Upgrade to Premium to view full archive details"
                    )
                }
            }
            
            PremiumFeature.PREMIUM_MARKERS -> {
                if (isPremiumActive) {
                    PremiumGatingResult.Allowed
                } else {
                    PremiumGatingResult.Blocked(
                        feature = feature,
                        reason = "Premium marker styles are premium-only",
                        upgradeMessage = "Upgrade to Premium for exclusive marker styles"
                    )
                }
            }
            
            PremiumFeature.LEADERBOARD_POSITION -> {
                if (isPremiumActive) {
                    PremiumGatingResult.Allowed
                } else {
                    PremiumGatingResult.Blocked(
                        feature = feature,
                        reason = "Leaderboard position is premium-only",
                        upgradeMessage = "Upgrade to Premium to see your leaderboard position"
                    )
                }
            }
            
            PremiumFeature.FREE_POST_EXTENSION -> {
                if (isPremiumActive) {
                    PremiumGatingResult.Allowed
                } else {
                    PremiumGatingResult.Blocked(
                        feature = feature,
                        reason = "Free post extension is premium-only",
                        upgradeMessage = "Upgrade to Premium for ${FREE_PREMIUM_EXTEND_HOURS} hours free post extension"
                    )
                }
            }
        }
    }

    /**
     * Calculate the XP boost for a given XP amount based on the user's current level and premium status.
     * 
     * @param baseXp The base XP amount
     * @param userLevel The current level of the user
     * @return The boosted XP amount
     */
    fun calculateBoostedXp(baseXp: Int, userLevel: Int): Int {
        val isPremiumActive = _isPremium.value
        if (!isPremiumActive) return baseXp

        val multiplier = XpBoostConfig.getMultiplierForLevel(userLevel)
        val boostedXp = (baseXp * multiplier).toInt()
        
        logger.d(Logger.TAG_PREMIUM, "XP boost applied: $baseXp -> $boostedXp (Level $userLevel, ${(multiplier - 1) * 100}% boost)")
        
        return boostedXp
    }

    /**
     * Get the maximum map radius for the current user.
     * 
     * @return Maximum radius in meters, or -1 for unlimited
     */
    fun getMaxMapRadius(): Int {
        return if (_isPremium.value) {
            -1 // Unlimited
        } else {
            BASIC_RADIUS_METERS
        }
    }


    /**
     * Check if the user has unlimited map radius.
     * 
     * @return True if unlimited radius is available
     */
    fun hasUnlimitedRadius(): Boolean {
        return checkFeatureAccess(PremiumFeature.UNLIMITED_RADIUS) is PremiumGatingResult.Allowed
    }

    /**
     * Check if the user can see availability percentages.
     * 
     * @return True if availability percentages should be shown
     */
    fun canSeeAvailabilityPercentages(): Boolean {
        return checkFeatureAccess(PremiumFeature.AVAILABILITY_PERCENTAGES) is PremiumGatingResult.Allowed
    }

    /**
     * Check if the user has early access to new posts.
     * 
     * @return True if user has early access
     */
    fun hasEarlyAccess(): Boolean {
        return checkFeatureAccess(PremiumFeature.EARLY_ACCESS) is PremiumGatingResult.Allowed
    }

    /**
     * Check if the user can extend posts for free.
     * 
     * @return True if free extension is available
     */
    fun hasFreePostExtension(): Boolean {
        return checkFeatureAccess(PremiumFeature.FREE_POST_EXTENSION) is PremiumGatingResult.Allowed
    }

    /**
     * Check if the user can see their leaderboard position.
     * 
     * @return True if leaderboard position should be shown
     */
    fun canSeeLeaderboardPosition(): Boolean {
        return checkFeatureAccess(PremiumFeature.LEADERBOARD_POSITION) is PremiumGatingResult.Allowed
    }

    /**
     * Get the premium frame level based on user level and premium status.
     * 
     * @param userLevel Current user level
     * @return Frame level (0 = none, 1 = gold, 2 = crystal)
     */
    fun getPremiumFrameLevel(userLevel: Int): Int {
        if (!_isPremium.value) return 0
        
        return when {
            userLevel >= 15 -> 2 // Crystal frame with Lottie animation
            userLevel >= 1 -> 1  // Gold frame
            else -> 0            // No frame
        }
    }

    /**
     * Check if premium is expiring soon and needs user attention.
     * 
     * @return True if premium is expiring within 3 days
     */
    fun isPremiumExpiringSoon(): Boolean {
        return _premiumEntitlement.value.isExpiringSoon()
    }

    /**
     * Get days until premium expiration.
     * 
     * @return Number of days until expiration, or null if not applicable
     */
    fun getDaysUntilExpiration(): Int? {
        return _premiumEntitlement.value.getDaysUntilExpiration()
    }

    /**
     * Check if user is currently in trial period.
     * 
     * @return True if in trial
     */
    fun isInTrial(): Boolean {
        return _premiumEntitlement.value.isInTrial
    }

    /**
     * Check if user is currently in grace period.
     * 
     * @return True if in grace period
     */
    fun isInGracePeriod(): Boolean {
        return _premiumEntitlement.value.isInGracePeriod
    }

    // Constants from rules.md for easy access
    companion object {
        const val BASIC_RADIUS_METERS = 1500
        const val EARLY_ACCESS_MINUTES = 10
        const val FREE_PREMIUM_EXTEND_HOURS = 6

        // XP values from rules.md
        const val XP_SHARE_POST = 50
        const val XP_DAILY_LOGIN = 10
        const val XP_LIKE_RECEIVED = 15
        const val XP_COMMENT_RECEIVED = 20
        const val XP_COMMENT_WRITE = 15
        const val XP_SHARE_EXTERNAL = 25
        const val XP_PREMIUM_TASK = 500

        // XP Boost percentages from rules.md
        const val BOOST_LV1_4 = 0.05f
        const val BOOST_LV5_9 = 0.07f
        const val BOOST_LV10_14 = 0.10f
        const val BOOST_LV15_PLUS = 0.20f
    }
}