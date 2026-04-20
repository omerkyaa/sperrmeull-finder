package com.omerkaya.sperrmuellfinder.domain.repository

import android.app.Activity
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.premium.PostExtensionProduct
import com.omerkaya.sperrmuellfinder.domain.model.premium.PremiumEntitlement
import com.omerkaya.sperrmuellfinder.domain.model.premium.PremiumProduct
import com.omerkaya.sperrmuellfinder.domain.model.premium.PurchaseResult
import com.omerkaya.sperrmuellfinder.domain.model.premium.RestoreResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing premium features and purchases.
 * This interface abstracts the underlying billing system (RevenueCat).
 * 
 * According to rules.md:
 * - RevenueCat is the single source of truth for entitlements
 * - Firestore is used for informational purposes only
 * - All purchase validation happens server-side via RevenueCat
 */
interface PremiumRepository {

    /**
     * Observes the user's current premium entitlement status in real-time.
     * This is the single source of truth for premium access.
     * 
     * @return Flow emitting the latest PremiumEntitlement status
     */
    fun observePremiumEntitlement(): Flow<PremiumEntitlement>

    /**
     * Gets the cached premium entitlement status.
     * Useful for graceful degradation during network issues.
     * 
     * @return Current cached PremiumEntitlement
     */
    suspend fun getCachedPremiumEntitlement(): PremiumEntitlement

    /**
     * Fetches available premium subscription products from RevenueCat.
     * Products are configured in RevenueCat Dashboard.
     * 
     * @return Result containing list of PremiumProduct or error
     */
    suspend fun getPremiumProducts(): Result<List<PremiumProduct>>

    /**
     * Fetches available post extension products from RevenueCat.
     * 
     * @return Result containing list of PostExtensionProduct or error
     */
    suspend fun getPostExtensionProducts(): Result<List<PostExtensionProduct>>

    /**
     * Initiates a purchase flow for a premium subscription.
     * 
     * @param productId The RevenueCat product identifier
     * @return PurchaseResult indicating success, error, or cancellation
     */
    suspend fun purchasePremiumProduct(productId: String): PurchaseResult

    /**
     * Initiates a purchase flow for a premium subscription with Activity context.
     *
     * @param activity Current foreground Activity required by Google Play Billing
     * @param productId RevenueCat package/product identifier selected in paywall
     */
    suspend fun purchasePremiumProduct(
        activity: Activity,
        productId: String
    ): PurchaseResult

    /**
     * Initiates a purchase flow for a post extension.
     * 
     * @param productId The RevenueCat product identifier
     * @return PurchaseResult indicating success, error, or cancellation
     */
    suspend fun purchasePostExtension(productId: String): PurchaseResult

    /**
     * Initiates a purchase flow for post extension with Activity context.
     *
     * @param activity Current foreground Activity required by Google Play Billing
     * @param productId RevenueCat package/product identifier selected in paywall
     */
    suspend fun purchasePostExtension(
        activity: Activity,
        productId: String
    ): PurchaseResult

    /**
     * Restores previous purchases for the current user.
     * This will sync with RevenueCat and update entitlement status.
     * 
     * @return RestoreResult indicating success, error, or no purchases to restore
     */
    suspend fun restorePurchases(): RestoreResult

    /**
     * Synchronizes the premium status with Firestore for informational purposes.
     * This should be called after any entitlement change.
     * Note: Firestore data is NOT used for access control decisions.
     * 
     * @param entitlement Current entitlement to sync
     * @return Result indicating success or error
     */
    suspend fun syncPremiumStatusWithFirestore(entitlement: PremiumEntitlement): Result<Unit>

    /**
     * Logs a purchase event for analytics purposes.
     * 
     * @param productId Product that was purchased
     * @param productType Type of product (premium, xp, extension)
     * @param success Whether the purchase was successful
     * @param errorMessage Error message if purchase failed
     */
    suspend fun logPurchaseEvent(
        productId: String,
        productType: String,
        success: Boolean,
        errorMessage: String? = null
    )

    /**
     * Checks if a specific feature is available based on current entitlement.
     * This is a convenience method that delegates to PremiumManager.
     * 
     * @param feature The feature to check
     * @return True if feature is available, false otherwise
     */
    suspend fun isFeatureAvailable(feature: String): Boolean

    /**
     * Checks if the user has access to unlimited map radius.
     * Basic users are limited to 1.5km radius.
     * 
     * @return True if unlimited radius is available
     */
    suspend fun hasUnlimitedRadius(): Boolean

    /**
     * Checks if the user can see availability percentages.
     * This is a premium-only feature.
     * 
     * @return True if availability percentages should be shown
     */
    suspend fun canSeeAvailabilityPercentages(): Boolean

    /**
     * Checks if the user has early access to new posts (10 minutes).
     * This is a premium-only feature.
     * 
     * @return True if user has early access
     */
    suspend fun hasEarlyAccess(): Boolean

    /**
     * Checks if the user can extend posts for free.
     * Premium users get 6 hours free extension.
     * 
     * @return True if free extension is available
     */
    suspend fun hasFreePostExtension(): Boolean

    /**
     * Gets the maximum map radius for the current user.
     * Basic: 1500 meters, Premium: unlimited (represented as -1)
     * 
     * @return Maximum radius in meters, or -1 for unlimited
     */
    suspend fun getMaxMapRadius(): Int

    /**
     * Bind RevenueCat SDK user to current authenticated app user.
     */
    suspend fun initializeWithUser(userId: String): Result<Unit>

    /**
     * Clear RevenueCat user session when app user logs out.
     */
    suspend fun cleanup(): Result<Unit>
}