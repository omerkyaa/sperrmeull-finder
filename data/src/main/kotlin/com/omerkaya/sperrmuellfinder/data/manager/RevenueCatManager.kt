package com.omerkaya.sperrmuellfinder.data.manager

import android.app.Activity
import android.app.Application
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.premium.PostExtensionProduct
import com.omerkaya.sperrmuellfinder.domain.model.premium.PremiumEntitlement
import com.omerkaya.sperrmuellfinder.domain.model.premium.PremiumProduct
import com.omerkaya.sperrmuellfinder.domain.model.premium.PremiumStatus
import com.omerkaya.sperrmuellfinder.domain.model.premium.PremiumType
import com.omerkaya.sperrmuellfinder.domain.model.premium.PurchaseResult
import com.omerkaya.sperrmuellfinder.domain.model.premium.RestoreResult
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.logInWith
import com.revenuecat.purchases.logOutWith
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases.restorePurchasesWith
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Manager for RevenueCat integration - SDK 9.21.0 (Modern API)
 * Handles SDK initialization, product fetching, purchases, and entitlement management.
 * 
 * According to rules.md:
 * - RevenueCat is the single source of truth for entitlements
 * - All purchases go through RevenueCat
 * - Server-side validation is handled by RevenueCat
 * - Products are managed via RevenueCat Dashboard
 * 
 * Modern Features:
 * - Coroutine-first API with suspendCancellableCoroutine
 * - Reactive Flow for real-time entitlement updates
 * - Paywalls support via RevenueCat UI
 * - Customer Center integration
 */
@Singleton
class RevenueCatManager @Inject constructor(
    private val application: Application,
    private val logger: Logger
) {

    @Volatile
    private var isInitialized = false
    
    // Real-time entitlement state
    private val _premiumEntitlement = MutableStateFlow<PremiumEntitlement>(
        PremiumEntitlement(isActive = false, status = PremiumStatus.NEVER_PURCHASED)
    )
    val premiumEntitlement: StateFlow<PremiumEntitlement> = _premiumEntitlement.asStateFlow()
    
    // Lock for thread-safe initialization
    private val initializationLock = Any()
    @Volatile
    private var offeringsConfigurationInvalid = false
    @Volatile
    private var offeringsConfigurationWarningLogged = false
    
    // RevenueCat configuration
    companion object {
        private const val DEFAULT_OFFERING_ID = "default"
    }

    /**
     * Initialize RevenueCat SDK 9.21.0 with modern API.
     * Should be called in Application.onCreate()
     */
    fun initialize(apiKey: String) {
        if (isInitialized) {
            logger.w(Logger.TAG_PREMIUM, "RevenueCat already initialized")
            return
        }

        synchronized(initializationLock) {
            // Double-check locking pattern
            if (isInitialized) {
                logger.w(Logger.TAG_PREMIUM, "RevenueCat already initialized (double-check)")
                return
            }

            try {
                logger.d(Logger.TAG_PREMIUM, "Initializing RevenueCat 9.21.0 with SDK API Key")

                // API key validation
                when {
                    apiKey.isBlank() -> {
                        logger.w(Logger.TAG_PREMIUM, "Empty API key - RevenueCat disabled")
                        isInitialized = false
                        return
                    }
                    apiKey == "placeholder_key" -> {
                        logger.w(Logger.TAG_PREMIUM, "Placeholder API key - RevenueCat disabled for development")
                        isInitialized = false
                        return
                    }
                    apiKey.length < 10 -> {
                        logger.w(Logger.TAG_PREMIUM, "Invalid API key format - RevenueCat disabled")
                        isInitialized = false
                        return
                    }
                }

                // Use the lowest available SDK log verbosity (NONE is not available in this SDK).
                Purchases.logLevel = LogLevel.WARN
                
                val configuration = PurchasesConfiguration.Builder(application, apiKey)
                    .build()

                Purchases.configure(configuration)
                
                // Set up real-time customer info listener
                setupCustomerInfoListener()
                
                // Fetch initial customer info
                fetchInitialCustomerInfo()

                isInitialized = true
                logger.i(Logger.TAG_PREMIUM, "RevenueCat 9.21.0 initialized successfully")
                
            } catch (e: Exception) {
                logger.e(Logger.TAG_PREMIUM, "Error initializing RevenueCat", e)
                isInitialized = false
                logger.i(Logger.TAG_PREMIUM, "App continuing without RevenueCat - premium features disabled")
            }
        }
    }
    
    /**
     * Setup real-time customer info listener for entitlement updates.
     */
    private fun setupCustomerInfoListener() {
        try {
            Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener { customerInfo ->
                try {
                    logger.d(
                        Logger.TAG_PREMIUM,
                        "Customer info updated: activeEntitlements=${customerInfo.entitlements.active.keys}"
                    )
                    val entitlement = mapCustomerInfoToEntitlement(customerInfo)
                    _premiumEntitlement.value = entitlement
                } catch (e: Exception) {
                    logger.e(Logger.TAG_PREMIUM, "Error processing customer info update", e)
                }
            }
        } catch (e: Exception) {
            logger.w(Logger.TAG_PREMIUM, "Failed to set up customer info listener", e)
        }
    }
    
    /**
     * Fetch initial customer info on startup.
     */
    private fun fetchInitialCustomerInfo() {
        try {
            Purchases.sharedInstance.getCustomerInfoWith(
                onError = { error ->
                    logger.e(Logger.TAG_PREMIUM, "Error fetching initial customer info: ${error.message}")
                },
                onSuccess = { customerInfo ->
                    logger.i(Logger.TAG_PREMIUM, "Initial customer info fetched successfully")
                    val entitlement = mapCustomerInfoToEntitlement(customerInfo)
                    _premiumEntitlement.value = entitlement
                }
            )
        } catch (e: Exception) {
            logger.e(Logger.TAG_PREMIUM, "Error fetching initial customer info", e)
        }
    }

    /**
     * Check if RevenueCat SDK is properly initialized and ready to use.
     */
    fun isReady(): Boolean {
        return try {
            isInitialized && Purchases.isConfigured
        } catch (e: Exception) {
            logger.w(Logger.TAG_PREMIUM, "RevenueCat readiness check failed")
            false
        }
    }

    /**
     * Set user ID for RevenueCat.
     * Should be called after user authentication.
     */
    suspend fun setUserId(userId: String): Result<Unit> = suspendCancellableCoroutine { continuation ->
        try {
            if (!isReady()) {
                continuation.resume(Result.Error(Exception("RevenueCat not initialized")))
                return@suspendCancellableCoroutine
            }
            
            Purchases.sharedInstance.logInWith(
                appUserID = userId,
                onError = { error ->
                    logger.e(Logger.TAG_PREMIUM, "Failed to set user ID: ${error.message}")
                    if (continuation.isActive) {
                        continuation.resume(Result.Error(Exception(error.message)))
                    }
                },
                onSuccess = { customerInfo, created ->
                    logger.i(Logger.TAG_PREMIUM, "User ID set: $userId (created: $created)")
                    val entitlement = mapCustomerInfoToEntitlement(customerInfo)
                    _premiumEntitlement.value = entitlement
                    if (continuation.isActive) {
                        continuation.resume(Result.Success(Unit))
                    }
                }
            )
        } catch (e: Exception) {
            logger.e(Logger.TAG_PREMIUM, "Error setting user ID", e)
            if (continuation.isActive) {
                continuation.resume(Result.Error(e))
            }
        }
    }

    /**
     * Log out current user from RevenueCat.
     */
    suspend fun logOut(): Result<Unit> = suspendCancellableCoroutine { continuation ->
        try {
            if (!isReady()) {
                continuation.resume(Result.Error(Exception("RevenueCat not initialized")))
                return@suspendCancellableCoroutine
            }
            
            Purchases.sharedInstance.logOutWith(
                onError = { error ->
                    logger.e(Logger.TAG_PREMIUM, "Failed to log out: ${error.message}")
                    if (continuation.isActive) {
                        continuation.resume(Result.Error(Exception(error.message)))
                    }
                },
                onSuccess = { customerInfo ->
                    logger.i(Logger.TAG_PREMIUM, "User logged out from RevenueCat")
                    _premiumEntitlement.value = PremiumEntitlement(
                        isActive = false,
                        status = PremiumStatus.NEVER_PURCHASED
                    )
                    if (continuation.isActive) {
                        continuation.resume(Result.Success(Unit))
                    }
                }
            )
        } catch (e: Exception) {
            logger.e(Logger.TAG_PREMIUM, "Error logging out", e)
            if (continuation.isActive) {
                continuation.resume(Result.Error(e))
            }
        }
    }

    /**
     * Observe premium entitlement changes in real-time.
     * This is the single source of truth for premium status.
     */
    fun observePremiumEntitlement(): Flow<PremiumEntitlement> {
        return premiumEntitlement
    }

    /**
     * Get cached premium entitlement (synchronous).
     */
    fun getCachedPremiumEntitlement(): PremiumEntitlement {
        return _premiumEntitlement.value
    }

    /**
     * Fetch available offerings from RevenueCat Dashboard.
     */
    suspend fun getOfferings(): Result<Offerings> = suspendCancellableCoroutine { continuation ->
        try {
            if (!isReady()) {
                logger.w(Logger.TAG_PREMIUM, "RevenueCat not initialized - returning empty offerings")
                continuation.resume(Result.Error(Exception("RevenueCat not initialized")))
                return@suspendCancellableCoroutine
            }

            if (offeringsConfigurationInvalid) {
                continuation.resume(
                    Result.Error(
                        IllegalStateException("RevenueCat offerings are not configured in dashboard")
                    )
                )
                return@suspendCancellableCoroutine
            }
            
            Purchases.sharedInstance.getOfferingsWith(
                onError = { error ->
                    if (isOfferingsConfigurationError(error)) {
                        offeringsConfigurationInvalid = true
                        if (!offeringsConfigurationWarningLogged) {
                            offeringsConfigurationWarningLogged = true
                            logger.w(
                                Logger.TAG_PREMIUM,
                                "RevenueCat offerings are not configured. Configure Play products in RevenueCat Dashboard."
                            )
                        }
                        if (continuation.isActive) {
                            continuation.resume(
                                Result.Error(
                                    IllegalStateException("RevenueCat offerings are not configured in dashboard")
                                )
                            )
                        }
                        return@getOfferingsWith
                    }
                    logger.e(Logger.TAG_PREMIUM, "Error fetching offerings: ${error.message}")
                    if (continuation.isActive) {
                        continuation.resume(Result.Error(Exception(error.message)))
                    }
                },
                onSuccess = { offerings ->
                    logger.i(Logger.TAG_PREMIUM, "Offerings fetched successfully: ${offerings.current?.identifier}")
                    if (continuation.isActive) {
                        continuation.resume(Result.Success(offerings))
                    }
                }
            )
        } catch (e: Exception) {
            logger.e(Logger.TAG_PREMIUM, "Error fetching offerings", e)
            if (continuation.isActive) {
                continuation.resume(Result.Error(e))
            }
        }
    }

    /**
     * Fetch available premium products from RevenueCat.
     */
    suspend fun getPremiumProducts(): Result<List<PremiumProduct>> {
        return try {
            when (val result = getOfferings()) {
                is Result.Success -> {
                    val products = result.data.current?.availablePackages
                        ?.filter { it.product.type == ProductType.SUBS }
                        ?.mapNotNull { mapPackageToPremiumProduct(it) }
                        ?: emptyList()
                    
                    logger.i(Logger.TAG_PREMIUM, "Premium products fetched: ${products.size}")
                    Result.Success(products)
                }
                is Result.Error -> {
                    logger.w(Logger.TAG_PREMIUM, "Error fetching premium products", result.exception)
                    Result.Success(emptyList())
                }
                Result.Loading -> Result.Success(emptyList())
            }
        } catch (e: Exception) {
            logger.e(Logger.TAG_PREMIUM, "Error fetching premium products", e)
            Result.Success(emptyList())
        }
    }

    /**
     * Purchase a package with Activity context.
     * Modern API with PurchaseParams.
     */
    suspend fun purchasePackageWithActivity(
        activity: Activity,
        packageToPurchase: Package
    ): PurchaseResult = suspendCancellableCoroutine { continuation ->
        try {
            if (!isReady()) {
                continuation.resume(PurchaseResult.Error("RevenueCat not initialized"))
                return@suspendCancellableCoroutine
            }
            
            logger.i(Logger.TAG_PREMIUM, "Initiating purchase for package: ${packageToPurchase.identifier}")
            
            Purchases.sharedInstance.purchaseWith(
                purchaseParams = PurchaseParams.Builder(activity, packageToPurchase).build(),
                onError = { error, userCancelled ->
                    if (userCancelled) {
                        logger.i(Logger.TAG_PREMIUM, "Purchase cancelled by user")
                        if (continuation.isActive) {
                            continuation.resume(PurchaseResult.UserCancelled)
                        }
                    } else {
                        logger.e(Logger.TAG_PREMIUM, "Purchase error: ${error.message}")
                        val errorMessage = when (error.code) {
                            PurchasesErrorCode.PurchaseCancelledError -> "Purchase cancelled"
                            PurchasesErrorCode.StoreProblemError -> "Store problem. Please try again."
                            PurchasesErrorCode.PurchaseNotAllowedError -> "Purchase not allowed"
                            PurchasesErrorCode.PurchaseInvalidError -> "Invalid purchase"
                            PurchasesErrorCode.ProductAlreadyPurchasedError -> "Product already purchased"
                            PurchasesErrorCode.NetworkError -> "Network error. Please check your connection."
                            else -> error.message
                        }
                        if (continuation.isActive) {
                            continuation.resume(PurchaseResult.Error(errorMessage))
                        }
                    }
                },
                onSuccess = { storeTransaction, customerInfo ->
                    logger.i(Logger.TAG_PREMIUM, "Purchase successful: ${storeTransaction?.productIds}")
                    val entitlement = mapCustomerInfoToEntitlement(customerInfo)
                    _premiumEntitlement.value = entitlement
                    if (continuation.isActive) {
                        continuation.resume(PurchaseResult.Success(entitlement))
                    }
                }
            )
        } catch (e: Exception) {
            logger.e(Logger.TAG_PREMIUM, "Unexpected error during purchase", e)
            if (continuation.isActive) {
                continuation.resume(PurchaseResult.Error("Unexpected error: ${e.message}"))
            }
        }
    }

    /**
     * Restore previous purchases.
     */
    suspend fun restorePurchases(): RestoreResult = suspendCancellableCoroutine { continuation ->
        try {
            if (!isReady()) {
                continuation.resume(RestoreResult.Error("RevenueCat not initialized"))
                return@suspendCancellableCoroutine
            }
            
            logger.i(Logger.TAG_PREMIUM, "Restoring purchases")
            
            Purchases.sharedInstance.restorePurchasesWith(
                onError = { error ->
                    logger.e(Logger.TAG_PREMIUM, "Restore error: ${error.message}")
                    if (continuation.isActive) {
                        continuation.resume(RestoreResult.Error(error.message))
                    }
                },
                onSuccess = { customerInfo ->
                    val entitlement = mapCustomerInfoToEntitlement(customerInfo)
                    _premiumEntitlement.value = entitlement
                    
                    if (entitlement.isActive) {
                        logger.i(Logger.TAG_PREMIUM, "Purchases restored successfully")
                        if (continuation.isActive) {
                            continuation.resume(RestoreResult.Success((entitlement.expirationDate?.time ?: 0).toInt()))
                        }
                    } else {
                        logger.i(Logger.TAG_PREMIUM, "No restorable purchases found")
                        if (continuation.isActive) {
                            continuation.resume(RestoreResult.NoRestorablePurchases)
                        }
                    }
                }
            )
        } catch (e: Exception) {
            logger.e(Logger.TAG_PREMIUM, "Unexpected error during restore", e)
            if (continuation.isActive) {
                continuation.resume(RestoreResult.Error("Unexpected error: ${e.message}"))
            }
        }
    }

    // Helper methods for mapping RevenueCat data to domain models

    private fun mapCustomerInfoToEntitlement(customerInfo: CustomerInfo): PremiumEntitlement {
        val premiumEntitlement = resolvePremiumEntitlement(customerInfo)
        
        return if (premiumEntitlement != null && premiumEntitlement.isActive) {
            val isCancelled = premiumEntitlement.unsubscribeDetectedAt != null
            PremiumEntitlement(
                isActive = !isCancelled,
                status = when {
                    isCancelled -> PremiumStatus.EXPIRED
                    premiumEntitlement.willRenew -> PremiumStatus.ACTIVE
                    premiumEntitlement.billingIssueDetectedAt != null -> PremiumStatus.GRACE_PERIOD
                    else -> PremiumStatus.ACTIVE
                },
                type = mapProductIdToPremiumType(premiumEntitlement.productIdentifier),
                expirationDate = premiumEntitlement.expirationDate,
                latestPurchaseDate = premiumEntitlement.latestPurchaseDate,
                originalPurchaseDate = premiumEntitlement.originalPurchaseDate,
                isInTrial = false,
                trialEndDate = null,
                isInGracePeriod = premiumEntitlement.billingIssueDetectedAt != null,
                gracePeriodEndDate = null,
                willRenew = premiumEntitlement.willRenew && !isCancelled,
                unsubscribeDetectedAt = premiumEntitlement.unsubscribeDetectedAt,
                billingIssueDetectedAt = premiumEntitlement.billingIssueDetectedAt
            )
        } else {
            PremiumEntitlement(
                isActive = false,
                status = if (premiumEntitlement?.expirationDate != null) {
                    PremiumStatus.EXPIRED
                } else {
                    PremiumStatus.NEVER_PURCHASED
                }
            )
        }
    }

    private fun mapProductIdToPremiumType(productId: String): PremiumType? {
        val normalizedId = productId.lowercase()
        return when {
            normalizedId.contains("plus") && normalizedId.contains("month") -> PremiumType.PREMIUM_PLUS_MONTH
            normalizedId.contains("week") -> PremiumType.PREMIUM_WEEK
            normalizedId.contains("month") -> PremiumType.PREMIUM_MONTH
            normalizedId.contains("year") || normalizedId.contains("annual") -> PremiumType.PREMIUM_YEAR
            else -> null
        }
    }

    private fun mapPackageToPremiumProduct(packageInfo: Package): PremiumProduct? {
        val product = packageInfo.product
        val premiumType = mapProductIdToPremiumType(packageInfo.identifier)
            ?: mapProductIdToPremiumType(product.id)
            ?: return null
        
        // RevenueCat SDK 9.21.0: Use product.period and presentedOfferingContext for subscription details
        return PremiumProduct(
            id = product.id,
            type = premiumType,
            title = product.title,
            description = product.description,
            price = product.price.formatted,
            priceAmountMicros = product.price.amountMicros,
            priceCurrencyCode = product.price.currencyCode,
            subscriptionPeriod = product.period?.iso8601,
            freeTrialPeriod = null, // TODO: Extract from presentedOfferingContext if available
            introductoryPrice = null, // TODO: Extract from presentedOfferingContext if available
            introductoryPriceAmountMicros = null,
            introductoryPricePeriod = null,
            introductoryPriceCycles = null,
            xpBonus = getXpBonusForPremiumType(premiumType),
            badgeIncluded = false,
            isPopular = premiumType == PremiumType.PREMIUM_MONTH
        )
    }

    private fun getXpBonusForPremiumType(type: PremiumType): Int {
        return when (type) {
            PremiumType.PREMIUM_WEEK -> 0
            PremiumType.PREMIUM_MONTH -> 800
            PremiumType.PREMIUM_YEAR -> 0
            PremiumType.PREMIUM_PLUS_MONTH -> 1500
        }
    }

    private fun isOfferingsConfigurationError(error: PurchasesError): Boolean {
        if (error.code == PurchasesErrorCode.ConfigurationError) {
            return true
        }
        val message = error.message.lowercase()
        val underlying = error.underlyingErrorMessage?.lowercase().orEmpty()
        return message.contains("why-are-offerings-empty") ||
            (message.contains("offerings") && message.contains("configuration")) ||
            underlying.contains("no play store products registered") ||
            underlying.contains("configure offerings")
    }

    private fun resolvePremiumEntitlement(customerInfo: CustomerInfo): EntitlementInfo? {
        val activeEntitlements = customerInfo.entitlements.active
        if (activeEntitlements.isEmpty()) return null

        // Dashboard-truth approach: pick active entitlement dynamically.
        val preferred = activeEntitlements.entries.firstOrNull { (id, _) ->
            id.lowercase().contains("premium")
        }?.value

        return preferred ?: activeEntitlements.values.firstOrNull()
    }
}
