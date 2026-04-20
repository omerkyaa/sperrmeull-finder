package com.omerkaya.sperrmuellfinder.data.repository

import android.app.Activity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.data.manager.RevenueCatManager
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants
import com.omerkaya.sperrmuellfinder.domain.model.premium.PostExtensionProduct
import com.omerkaya.sperrmuellfinder.domain.model.premium.PremiumEntitlement
import com.omerkaya.sperrmuellfinder.domain.model.premium.PremiumFeature
import com.omerkaya.sperrmuellfinder.domain.model.premium.PremiumProduct
import com.omerkaya.sperrmuellfinder.domain.model.premium.PurchaseResult
import com.omerkaya.sperrmuellfinder.domain.model.premium.RestoreResult
import com.omerkaya.sperrmuellfinder.domain.model.premium.XpBoostConfig
import com.omerkaya.sperrmuellfinder.domain.repository.PremiumRepository
import com.revenuecat.purchases.Package
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PremiumRepository using RevenueCat and Firebase.
 * 
 * According to rules.md:
 * - RevenueCat is the single source of truth for entitlements
 * - Firestore is used for informational purposes only
 * - All purchase validation happens server-side via RevenueCat
 */
@Singleton
class PremiumRepositoryImpl @Inject constructor(
    private val revenueCatManager: RevenueCatManager,
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val logger: Logger
) : PremiumRepository {

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val PURCHASES_COLLECTION = "purchases"
        private const val ANALYTICS_COLLECTION = "analytics"
        
        // Premium constants from rules.md
        private const val BASIC_RADIUS_METERS = 1500
        private const val EARLY_ACCESS_MINUTES = 10
        private const val FREE_PREMIUM_EXTEND_HOURS = 6
    }

    override fun observePremiumEntitlement(): Flow<PremiumEntitlement> {
        return revenueCatManager.observePremiumEntitlement()
    }

    override suspend fun getCachedPremiumEntitlement(): PremiumEntitlement {
        return revenueCatManager.getCachedPremiumEntitlement()
    }

    override suspend fun getPremiumProducts(): Result<List<PremiumProduct>> {
        logger.d(Logger.TAG_PREMIUM, "Fetching premium products from RevenueCat")
        return revenueCatManager.getPremiumProducts()
    }

    override suspend fun getPostExtensionProducts(): Result<List<PostExtensionProduct>> {
        logger.d(Logger.TAG_PREMIUM, "Fetching post extension products from RevenueCat")
        // Post extension products are not implemented in RevenueCat yet
        return Result.Success(emptyList())
    }

    override suspend fun purchasePremiumProduct(productId: String): PurchaseResult {
        logger.d(Logger.TAG_PREMIUM, "Premium purchase requested: $productId")
        // Purchase requires Activity context - should be called from UI layer via ViewModel
        return PurchaseResult.Error("Purchase requires Activity context. Use purchasePackageWithActivity() from UI ViewModel.")
    }

    override suspend fun purchasePremiumProduct(activity: Activity, productId: String): PurchaseResult {
        val packageToPurchase = findPackageForProductId(productId)
            ?: return PurchaseResult.ProductNotAvailable
        return purchasePremiumProductWithActivity(activity, packageToPurchase)
    }

    override suspend fun purchasePostExtension(productId: String): PurchaseResult {
        logger.d(Logger.TAG_PREMIUM, "Post extension purchase requested: $productId")
        // Purchase requires Activity context - should be called from UI layer via ViewModel
        return PurchaseResult.Error("Purchase requires Activity context. Use purchasePackageWithActivity() from UI ViewModel.")
    }

    override suspend fun purchasePostExtension(activity: Activity, productId: String): PurchaseResult {
        val packageToPurchase = findPackageForProductId(productId)
            ?: return PurchaseResult.ProductNotAvailable
        return purchasePostExtensionWithActivity(activity, packageToPurchase)
    }

    override suspend fun restorePurchases(): RestoreResult {
        logger.d(Logger.TAG_PREMIUM, "Initiating restore purchases")
        val result = revenueCatManager.restorePurchases()
        if (result is RestoreResult.Success && result.restoredCount > 0) {
            logPurchaseEvent(
                productId = "restore",
                productType = "purchaseRestore",
                success = true
            )
            createInAppPurchaseNotification(
                title = "Purchases restored",
                message = "Your premium purchases were restored successfully.",
                type = "purchaseRestore"
            )
        }
        return result
    }

    /**
     * Purchase premium product with Activity context (proper implementation).
     * This method should be called from UI layer (ViewModel/Composable).
     */
    suspend fun purchasePremiumProductWithActivity(activity: Activity, packageToPurchase: Package): PurchaseResult {
        logger.d(Logger.TAG_PREMIUM, "Initiating premium purchase with Activity: ${packageToPurchase.identifier}")
        val result = revenueCatManager.purchasePackageWithActivity(activity, packageToPurchase)
        
        // Log purchase attempt
        logPurchaseEvent(
            productId = packageToPurchase.identifier,
            productType = "premium",
            success = result is PurchaseResult.Success,
            errorMessage = if (result is PurchaseResult.Error) result.message else null
        )
        
        // If successful, sync with Firestore and log transaction
        if (result is PurchaseResult.Success) {
            syncPremiumStatusWithFirestore(result.entitlement)
            logSuccessfulPurchase(
                packageToPurchase.identifier, 
                "premium", 
                packageToPurchase.product.price.formatted, 
                packageToPurchase.product.price.currencyCode
            )
            createInAppPurchaseNotification(
                title = "Premium activated",
                message = "Your premium subscription is now active.",
                type = "premiumActivated"
            )
        }
        
        return result
    }

    /**
     * Purchase XP product with Activity context (proper implementation).
     * This method should be called from UI layer (ViewModel/Composable).
     */
    suspend fun purchaseXpProductWithActivity(activity: Activity, packageToPurchase: Package): PurchaseResult {
        logger.d(Logger.TAG_PREMIUM, "Initiating XP purchase with Activity: ${packageToPurchase.identifier}")
        val result = revenueCatManager.purchasePackageWithActivity(activity, packageToPurchase)
        
        // Log purchase attempt
        logPurchaseEvent(
            productId = packageToPurchase.identifier,
            productType = "xp",
            success = result is PurchaseResult.Success,
            errorMessage = if (result is PurchaseResult.Error) result.message else null
        )
        
        // If successful, log transaction
        if (result is PurchaseResult.Success) {
            logSuccessfulPurchase(
                packageToPurchase.identifier, 
                "xp", 
                packageToPurchase.product.price.formatted, 
                packageToPurchase.product.price.currencyCode
            )
            createInAppPurchaseNotification(
                title = "Purchase successful",
                message = "Your XP package purchase is completed.",
                type = "purchaseSuccess"
            )
        }
        
        return result
    }

    /**
     * Purchase post extension with Activity context (proper implementation).
     * This method should be called from UI layer (ViewModel/Composable).
     */
    suspend fun purchasePostExtensionWithActivity(activity: Activity, packageToPurchase: Package): PurchaseResult {
        logger.d(Logger.TAG_PREMIUM, "Initiating post extension purchase with Activity: ${packageToPurchase.identifier}")
        val result = revenueCatManager.purchasePackageWithActivity(activity, packageToPurchase)
        
        // Log purchase attempt
        logPurchaseEvent(
            productId = packageToPurchase.identifier,
            productType = "post_extension",
            success = result is PurchaseResult.Success,
            errorMessage = if (result is PurchaseResult.Error) result.message else null
        )
        
        // If successful, log transaction
        if (result is PurchaseResult.Success) {
            logSuccessfulPurchase(
                packageToPurchase.identifier, 
                "post_extension", 
                packageToPurchase.product.price.formatted, 
                packageToPurchase.product.price.currencyCode
            )
            createInAppPurchaseNotification(
                title = "Purchase successful",
                message = "Your post extension purchase is completed.",
                type = "purchaseSuccess"
            )
        }
        
        return result
    }

    override suspend fun syncPremiumStatusWithFirestore(entitlement: PremiumEntitlement): Result<Unit> {
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                logger.w(Logger.TAG_PREMIUM, "Cannot sync premium status: user not authenticated")
                return Result.Error(Exception("User not authenticated"))
            }

            val premiumData = mapOf(
                "ispremium" to entitlement.isActive,
                "premiumType" to (entitlement.type?.name ?: ""),
                "premiumuntil" to entitlement.expirationDate,
                "isInTrial" to entitlement.isInTrial,
                "isInGracePeriod" to entitlement.isInGracePeriod,
                "willRenew" to entitlement.willRenew,
                "lastSyncedAt" to Date()
            )

            firestore.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .update(premiumData)
                .await()

            logger.d(Logger.TAG_PREMIUM, "Premium status synced with Firestore: isPremium=${entitlement.isActive}")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(Logger.TAG_PREMIUM, "Failed to sync premium status with Firestore", e)
            Result.Error(e)
        }
    }

    /**
     * Initialize RevenueCat with user authentication.
     * Should be called after user login.
     */
    override suspend fun initializeWithUser(userId: String): Result<Unit> {
        logger.d(Logger.TAG_PREMIUM, "Initializing RevenueCat with user: $userId")
        return revenueCatManager.setUserId(userId)
    }

    /**
     * Clean up RevenueCat when user logs out.
     */
    override suspend fun cleanup(): Result<Unit> {
        logger.d(Logger.TAG_PREMIUM, "Cleaning up RevenueCat")
        return revenueCatManager.logOut()
    }

    /**
     * Log successful purchase to Firestore for record keeping.
     */
    suspend fun logSuccessfulPurchase(
        productId: String,
        productType: String,
        price: String,
        currency: String
    ): Result<Unit> {
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                logger.w(Logger.TAG_PREMIUM, "Cannot log purchase: user not authenticated")
                return Result.Error(Exception("User not authenticated"))
            }

            val purchaseRecord = mapOf(
                "purchaseId" to UUID.randomUUID().toString(),
                "userId" to currentUser.uid,
                "productId" to productId,
                "productType" to productType,
                "price" to price,
                "currency" to currency,
                "purchaseDate" to Date(),
                "platform" to "android",
                "source" to "revenuecat"
            )

            val purchaseId = purchaseRecord["purchaseId"] as String
            firestore.collection(PURCHASES_COLLECTION)
                .document(currentUser.uid)
                .collection("items")
                .document(purchaseId)
                .set(purchaseRecord)
                .await()

            logger.i(Logger.TAG_PREMIUM, "Purchase logged successfully: $productId")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(Logger.TAG_PREMIUM, "Failed to log purchase", e)
            Result.Error(e)
        }
    }

    /**
     * Get feature access summary for UI display.
     */
    suspend fun getFeatureAccessSummary(): Map<String, Boolean> {
        val entitlement = getCachedPremiumEntitlement()
        val isPremium = entitlement.isAccessible()
        
        return mapOf(
            "isPremium" to isPremium,
            "unlimitedRadius" to isPremium,
            "availabilityPercentages" to isPremium,
            "premiumFilters" to isPremium,
            "favoriteNotifications" to isPremium,
            "earlyAccess" to isPremium,
            "archiveFullDetail" to isPremium,
            "premiumMarkers" to isPremium,
            "leaderboardPosition" to isPremium,
            "freePostExtension" to isPremium
        )
    }

    /**
     * Check if premium is expiring soon (within 3 days).
     */
    suspend fun isPremiumExpiringSoon(): Boolean {
        val entitlement = getCachedPremiumEntitlement()
        return entitlement.isExpiringSoon()
    }

    /**
     * Get days until premium expiration.
     */
    suspend fun getDaysUntilExpiration(): Int? {
        val entitlement = getCachedPremiumEntitlement()
        return entitlement.getDaysUntilExpiration()
    }

    /**
     * Get premium status for UI display.
     */
    suspend fun getPremiumStatusForUI(): Map<String, Any?> {
        val entitlement = getCachedPremiumEntitlement()
        
        return mapOf(
            "isActive" to entitlement.isActive,
            "status" to entitlement.status.name,
            "type" to entitlement.type?.name,
            "expirationDate" to entitlement.expirationDate,
            "isInTrial" to entitlement.isInTrial,
            "isInGracePeriod" to entitlement.isInGracePeriod,
            "willRenew" to entitlement.willRenew,
            "daysUntilExpiration" to entitlement.getDaysUntilExpiration(),
            "isExpiringSoon" to entitlement.isExpiringSoon()
        )
    }

    override suspend fun logPurchaseEvent(
        productId: String,
        productType: String,
        success: Boolean,
        errorMessage: String?
    ) {
        try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                logger.w(Logger.TAG_PREMIUM, "Cannot log purchase event: user not authenticated")
                return
            }

            val eventData = mapOf(
                "userId" to currentUser.uid,
                "productId" to productId,
                "productType" to productType,
                "success" to success,
                "errorMessage" to errorMessage,
                "timestamp" to Date(),
                "platform" to "android"
            )

            firestore.collection(ANALYTICS_COLLECTION)
                .document("purchases")
                .collection("events")
                .add(eventData)
                .await()

            logger.d(Logger.TAG_PREMIUM, "Purchase event logged: $productId, success: $success")
        } catch (e: Exception) {
            logger.e(Logger.TAG_PREMIUM, "Failed to log purchase event", e)
        }
    }

    override suspend fun isFeatureAvailable(feature: String): Boolean {
        val entitlement = getCachedPremiumEntitlement()
        return entitlement.isAccessible()
    }

    override suspend fun hasUnlimitedRadius(): Boolean {
        val entitlement = getCachedPremiumEntitlement()
        return entitlement.isAccessible()
    }

    override suspend fun canSeeAvailabilityPercentages(): Boolean {
        val entitlement = getCachedPremiumEntitlement()
        return entitlement.isAccessible()
    }

    override suspend fun hasEarlyAccess(): Boolean {
        val entitlement = getCachedPremiumEntitlement()
        return entitlement.isAccessible()
    }

    override suspend fun hasFreePostExtension(): Boolean {
        val entitlement = getCachedPremiumEntitlement()
        return entitlement.isAccessible()
    }

    override suspend fun getMaxMapRadius(): Int {
        val entitlement = getCachedPremiumEntitlement()
        return if (entitlement.isAccessible()) -1 else BASIC_RADIUS_METERS
    }

    private suspend fun findPackageForProductId(productId: String): Package? {
        val offeringsResult = revenueCatManager.getOfferings()
        if (offeringsResult !is Result.Success) {
            logger.w(Logger.TAG_PREMIUM, "Offerings unavailable while resolving product: $productId")
            return null
        }

        val availablePackages = offeringsResult.data.current?.availablePackages.orEmpty()
        return availablePackages.firstOrNull { pkg ->
            pkg.identifier == productId || pkg.product.id == productId
        }
    }

    private suspend fun createInAppPurchaseNotification(
        title: String,
        message: String,
        type: String
    ) {
        try {
            val uid = firebaseAuth.currentUser?.uid ?: return
            val notificationRef = firestore
                .collection(FirestoreConstants.Collections.NOTIFICATIONS)
                .document(uid)
                .collection(FirestoreConstants.SUBCOLLECTION_USER_NOTIFICATIONS)
                .document()

            notificationRef.set(
                mapOf(
                    "id" to notificationRef.id,
                    "toUserId" to uid,
                    "type" to type,
                    "title" to title,
                    "message" to message,
                    "deepLink" to "premium",
                    "createdAt" to Date(),
                    "isRead" to false,
                    "meta" to mapOf("source" to "revenuecat")
                )
            ).await()
        } catch (e: Exception) {
            logger.w(Logger.TAG_PREMIUM, "Failed to create in-app purchase notification", e)
        }
    }
}
