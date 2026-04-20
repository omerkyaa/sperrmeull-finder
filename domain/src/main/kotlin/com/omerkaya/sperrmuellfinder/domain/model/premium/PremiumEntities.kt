package com.omerkaya.sperrmuellfinder.domain.model.premium

import java.util.Date

/**
 * Premium subscription status
 */
enum class PremiumStatus {
    ACTIVE,
    EXPIRED,
    GRACE_PERIOD,
    TRIAL,
    NEVER_PURCHASED
}

/**
 * Premium subscription type
 */
enum class PremiumType {
    PREMIUM_WEEK,
    PREMIUM_MONTH,
    PREMIUM_YEAR,
    PREMIUM_PLUS_MONTH
}

/**
 * Post extension types
 */
enum class PostExtensionType {
    POST_EXTEND_6H
}

/**
 * Premium entitlement information
 */
data class PremiumEntitlement(
    val isActive: Boolean = false,
    val status: PremiumStatus = PremiumStatus.NEVER_PURCHASED,
    val type: PremiumType? = null,
    val expirationDate: Date? = null,
    val latestPurchaseDate: Date? = null,
    val originalPurchaseDate: Date? = null,
    val isInTrial: Boolean = false,
    val trialEndDate: Date? = null,
    val isInGracePeriod: Boolean = false,
    val gracePeriodEndDate: Date? = null,
    val willRenew: Boolean = false,
    val unsubscribeDetectedAt: Date? = null,
    val billingIssueDetectedAt: Date? = null
) {
    /**
     * Check if premium is currently accessible
     */
    fun isAccessible(): Boolean {
        return when (status) {
            PremiumStatus.ACTIVE,
            PremiumStatus.TRIAL,
            PremiumStatus.GRACE_PERIOD -> true
            PremiumStatus.EXPIRED,
            PremiumStatus.NEVER_PURCHASED -> false
        }
    }
    
    /**
     * Check if premium will expire soon (within 3 days)
     */
    fun isExpiringSoon(): Boolean {
        if (!isActive || expirationDate == null) return false
        val threeDaysFromNow = Date(System.currentTimeMillis() + 3 * 24 * 60 * 60 * 1000)
        return expirationDate.before(threeDaysFromNow)
    }
    
    /**
     * Get days remaining until expiration
     */
    fun getDaysUntilExpiration(): Int? {
        if (!isActive || expirationDate == null) return null
        val now = System.currentTimeMillis()
        val expiration = expirationDate.time
        val diffInDays = ((expiration - now) / (24 * 60 * 60 * 1000)).toInt()
        return maxOf(0, diffInDays)
    }
}

/**
 * Available premium product
 */
data class PremiumProduct(
    val id: String,
    val type: PremiumType,
    val title: String,
    val description: String,
    val price: String,
    val priceAmountMicros: Long,
    val priceCurrencyCode: String,
    val subscriptionPeriod: String? = null,
    val freeTrialPeriod: String? = null,
    val introductoryPrice: String? = null,
    val introductoryPriceAmountMicros: Long? = null,
    val introductoryPricePeriod: String? = null,
    val introductoryPriceCycles: Int? = null,
    val xpBonus: Int = 0,
    val badgeIncluded: Boolean = false,
    val isPopular: Boolean = false
) {
    /**
     * Check if product has free trial
     */
    fun hasFreeTrial(): Boolean = !freeTrialPeriod.isNullOrEmpty()
    
    /**
     * Check if product has introductory pricing
     */
    fun hasIntroductoryPrice(): Boolean = introductoryPrice != null
    
    /**
     * Get formatted subscription period for display
     */
    fun getFormattedPeriod(): String {
        return when (type) {
            PremiumType.PREMIUM_WEEK -> "1 Woche"
            PremiumType.PREMIUM_MONTH -> "1 Monat"
            PremiumType.PREMIUM_YEAR -> "1 Jahr"
            PremiumType.PREMIUM_PLUS_MONTH -> "1 Monat"
        }
    }
}

/**
 * Post extension product
 */
data class PostExtensionProduct(
    val id: String,
    val type: PostExtensionType,
    val title: String,
    val description: String,
    val price: String,
    val priceAmountMicros: Long,
    val priceCurrencyCode: String,
    val extensionHours: Int = 6
)

/**
 * Purchase result
 */
sealed class PurchaseResult {
    data class Success(val entitlement: PremiumEntitlement) : PurchaseResult()
    data object UserCancelled : PurchaseResult()
    data object PaymentPending : PurchaseResult()
    data object ProductAlreadyOwned : PurchaseResult()
    data class Error(val message: String, val code: String? = null) : PurchaseResult()
    data object NetworkError : PurchaseResult()
    data object ProductNotAvailable : PurchaseResult()
    data object AlreadyOwned : PurchaseResult() // Deprecated, use ProductAlreadyOwned
}

/**
 * Restore purchases result
 */
sealed class RestoreResult {
    data class Success(val restoredCount: Int) : RestoreResult()
    data object NoRestorablePurchases : RestoreResult()
    data class Error(val message: String) : RestoreResult()
    data object NetworkError : RestoreResult()
}

/**
 * Premium feature gates
 */
enum class PremiumFeature {
    UNLIMITED_RADIUS,
    AVAILABILITY_PERCENTAGES,
    PREMIUM_FILTERS,
    FAVORITE_NOTIFICATIONS,
    EARLY_ACCESS,
    ARCHIVE_FULL_DETAIL,
    PREMIUM_MARKERS,
    LEADERBOARD_POSITION,
    FREE_POST_EXTENSION
}

/**
 * Premium gating result
 */
sealed class PremiumGatingResult {
    data object Allowed : PremiumGatingResult()
    data class Blocked(
        val feature: PremiumFeature,
        val reason: String,
        val upgradeMessage: String
    ) : PremiumGatingResult()
}

/**
 * XP boost configuration
 */
data class XpBoostConfig(
    val level: Int,
    val multiplier: Double
) {
    companion object {
        /**
         * Get XP boost multiplier for level
         */
        fun getMultiplierForLevel(level: Int): Double {
            return when {
                level in 1..4 -> 1.05    // +5%
                level in 5..9 -> 1.07    // +7%
                level in 10..14 -> 1.10  // +10%
                level >= 15 -> 1.20      // +20%
                else -> 1.0              // No boost
            }
        }
        
        /**
         * Calculate boosted XP amount
         */
        fun calculateBoostedXp(baseXp: Int, level: Int, isPremium: Boolean): Int {
            if (!isPremium) return baseXp
            val multiplier = getMultiplierForLevel(level)
            return (baseXp * multiplier).toInt()
        }
    }
}
