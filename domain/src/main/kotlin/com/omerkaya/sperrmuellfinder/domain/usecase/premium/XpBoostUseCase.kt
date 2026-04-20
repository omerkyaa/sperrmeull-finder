package com.omerkaya.sperrmuellfinder.domain.usecase.premium

import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.domain.manager.PremiumManager
import com.omerkaya.sperrmuellfinder.domain.model.premium.XpBoostConfig
import javax.inject.Inject

/**
 * Use case for calculating XP boosts based on user level and premium status.
 * 
 * According to rules.md:
 * - Premium XP Boost: Lv1–4: +5%, Lv5–9: +7%, Lv10–14: +10%, Lv15+: +20%
 * - XP values: post +50, daily +10, like +15, comment_received +20, comment_write +15, 
 *   share_external +25, leaderboard (500/250/150), premium_task +500
 * - Only premium users get XP boosts
 */
class XpBoostUseCase @Inject constructor(
    private val premiumManager: PremiumManager,
    private val logger: Logger
) {

    /**
     * Calculate the XP boost for a given XP amount.
     * 
     * @param baseXp The base XP amount
     * @param userLevel The current level of the user
     * @return The boosted XP amount (same as base if not premium)
     */
    operator fun invoke(baseXp: Int, userLevel: Int): Int {
        val boostedXp = premiumManager.calculateBoostedXp(baseXp, userLevel)
        
        if (boostedXp != baseXp) {
            val boostPercentage = ((boostedXp.toDouble() / baseXp - 1) * 100).toInt()
            logger.d(Logger.TAG_PREMIUM, "XP boost applied: $baseXp -> $boostedXp (+$boostPercentage% for Level $userLevel)")
        }
        
        return boostedXp
    }

    /**
     * Calculate XP boost for post sharing.
     * Base XP: 50
     */
    fun calculatePostSharingXp(userLevel: Int): Int {
        return invoke(PremiumManager.XP_SHARE_POST, userLevel)
    }

    /**
     * Calculate XP boost for daily login.
     * Base XP: 10
     */
    fun calculateDailyLoginXp(userLevel: Int): Int {
        return invoke(PremiumManager.XP_DAILY_LOGIN, userLevel)
    }

    /**
     * Get the XP boost multiplier for a specific level.
     */
    fun getBoostMultiplierForLevel(userLevel: Int): Double {
        return XpBoostConfig.getMultiplierForLevel(userLevel)
    }

    /**
     * Get the XP boost percentage for a specific level.
     */
    fun getBoostPercentageForLevel(userLevel: Int): Int {
        val multiplier = getBoostMultiplierForLevel(userLevel)
        return ((multiplier - 1.0) * 100).toInt()
    }

    /**
     * Check if the user gets XP boost at their current level.
     */
    fun hasXpBoostAtLevel(userLevel: Int): Boolean {
        return premiumManager.isPremium.value && userLevel >= 1
    }
}