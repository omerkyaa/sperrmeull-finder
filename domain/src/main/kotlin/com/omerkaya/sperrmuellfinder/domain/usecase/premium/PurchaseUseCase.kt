package com.omerkaya.sperrmuellfinder.domain.usecase.premium

import android.app.Activity
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.domain.model.premium.PostExtensionProduct
import com.omerkaya.sperrmuellfinder.domain.model.premium.PremiumProduct
import com.omerkaya.sperrmuellfinder.domain.model.premium.PurchaseResult
import com.omerkaya.sperrmuellfinder.domain.repository.PremiumRepository
import javax.inject.Inject

/**
 * Use case for handling product purchases (Premium, XP, Post Extension).
 * Orchestrates the purchase flow and handles logging/analytics.
 * 
 * According to rules.md:
 * - All purchases go through RevenueCat
 * - Analytics events are logged for all purchase attempts
 * - Server-side validation is handled by RevenueCat
 */
class PurchaseUseCase @Inject constructor(
    private val premiumRepository: PremiumRepository,
    private val logger: Logger
) {

    /**
     * Purchase a premium subscription product.
     * 
     * @param product The premium product to purchase
     * @return PurchaseResult indicating success, error, or cancellation
     */
    suspend operator fun invoke(activity: Activity, product: PremiumProduct): PurchaseResult {
        logger.d(Logger.TAG_PREMIUM, "Initiating premium purchase: ${product.id}")
        
        // Log purchase attempt
        premiumRepository.logPurchaseEvent(
            productId = product.id,
            productType = "premium_subscription",
            success = false // Will be updated on success
        )
        
        return try {
            val result = premiumRepository.purchasePremiumProduct(activity, product.id)
            
            when (result) {
                is PurchaseResult.Success -> {
                    logger.i(Logger.TAG_PREMIUM, "Premium purchase successful: ${product.id}")
                    
                    // Log successful purchase
                    premiumRepository.logPurchaseEvent(
                        productId = product.id,
                        productType = "premium_subscription",
                        success = true
                    )
                    
                    // Analytics: Premium activated
                    logPremiumActivated(product)
                }
                
                is PurchaseResult.UserCancelled -> {
                    logger.w(Logger.TAG_PREMIUM, "Premium purchase cancelled by user: ${product.id}")
                    
                    // Analytics: Purchase cancelled
                    logPurchaseCancelled(product.id, "premium_subscription")
                }
                
                is PurchaseResult.Error -> {
                    logger.e(Logger.TAG_PREMIUM, "Premium purchase failed: ${product.id}, Error: ${result.message}")
                    
                    // Log failed purchase
                    premiumRepository.logPurchaseEvent(
                        productId = product.id,
                        productType = "premium_subscription",
                        success = false,
                        errorMessage = result.message
                    )
                    
                    // Analytics: Purchase failed
                    logPurchaseFailed(product.id, "premium_subscription", result.message)
                }
                
                is PurchaseResult.PaymentPending -> {
                    logger.i(Logger.TAG_PREMIUM, "Premium purchase pending: ${product.id}")
                    
                    // Analytics: Purchase pending
                    logPurchasePending(product.id, "premium_subscription")
                }
                
                is PurchaseResult.NetworkError -> {
                    logger.e(Logger.TAG_PREMIUM, "Network error during premium purchase: ${product.id}")
                    
                    // Analytics: Network error
                    logPurchaseNetworkError(product.id, "premium_subscription")
                }
                
                is PurchaseResult.ProductNotAvailable -> {
                    logger.e(Logger.TAG_PREMIUM, "Premium product not available: ${product.id}")
                    
                    // Analytics: Product not available
                    logProductNotAvailable(product.id, "premium_subscription")
                }
                
                is PurchaseResult.ProductAlreadyOwned -> {
                    logger.w(Logger.TAG_PREMIUM, "Premium product already owned: ${product.id}")
                    
                    // Analytics: Product already owned
                    logProductAlreadyOwned(product.id, "premium_subscription")
                }
                
                is PurchaseResult.AlreadyOwned -> {
                    // Handle deprecated case for backward compatibility
                    logger.w(Logger.TAG_PREMIUM, "Premium product already owned (deprecated): ${product.id}")
                    
                    // Analytics: Already owned (deprecated)
                    logProductAlreadyOwned(product.id, "premium_subscription")
                }
            }
            
            result
        } catch (e: Exception) {
            logger.e(Logger.TAG_PREMIUM, "Unexpected error during premium purchase: ${product.id}", e)
            
            // Log unexpected error
            premiumRepository.logPurchaseEvent(
                productId = product.id,
                productType = "premium_subscription",
                success = false,
                errorMessage = e.message
            )
            
            PurchaseResult.Error("Unexpected error: ${e.message}")
        }
    }

    /**
     * Purchase a post extension product.
     * 
     * @param product The post extension product to purchase
     * @return PurchaseResult indicating success, error, or cancellation
     */
    suspend operator fun invoke(activity: Activity, product: PostExtensionProduct): PurchaseResult {
        logger.d(Logger.TAG_PREMIUM, "Initiating post extension purchase: ${product.id}")
        
        // Log purchase attempt
        premiumRepository.logPurchaseEvent(
            productId = product.id,
            productType = "post_extension",
            success = false
        )
        
        return try {
            val result = premiumRepository.purchasePostExtension(activity, product.id)
            
            when (result) {
                is PurchaseResult.Success -> {
                    logger.i(Logger.TAG_PREMIUM, "Post extension purchase successful: ${product.id}, Hours: ${product.extensionHours}")
                    
                    // Log successful purchase
                    premiumRepository.logPurchaseEvent(
                        productId = product.id,
                        productType = "post_extension",
                        success = true
                    )
                    
                    // Analytics: Post extension purchased
                    logPostExtensionPurchased(product)
                }
                
                is PurchaseResult.UserCancelled -> {
                    logger.w(Logger.TAG_PREMIUM, "Post extension purchase cancelled by user: ${product.id}")
                    logPurchaseCancelled(product.id, "post_extension")
                }
                
                is PurchaseResult.Error -> {
                    logger.e(Logger.TAG_PREMIUM, "Post extension purchase failed: ${product.id}, Error: ${result.message}")
                    
                    premiumRepository.logPurchaseEvent(
                        productId = product.id,
                        productType = "post_extension",
                        success = false,
                        errorMessage = result.message
                    )
                    
                    logPurchaseFailed(product.id, "post_extension", result.message)
                }
                
                is PurchaseResult.PaymentPending -> {
                    logger.i(Logger.TAG_PREMIUM, "Post extension purchase pending: ${product.id}")
                    logPurchasePending(product.id, "post_extension")
                }
                
                is PurchaseResult.NetworkError -> {
                    logger.e(Logger.TAG_PREMIUM, "Network error during post extension purchase: ${product.id}")
                    logPurchaseNetworkError(product.id, "post_extension")
                }
                
                is PurchaseResult.ProductNotAvailable -> {
                    logger.e(Logger.TAG_PREMIUM, "Post extension product not available: ${product.id}")
                    logProductNotAvailable(product.id, "post_extension")
                }
                
                is PurchaseResult.ProductAlreadyOwned -> {
                    logger.w(Logger.TAG_PREMIUM, "Post extension product already owned: ${product.id}")
                    logProductAlreadyOwned(product.id, "post_extension")
                }
                
                is PurchaseResult.AlreadyOwned -> {
                    // Handle deprecated case for backward compatibility
                    logger.w(Logger.TAG_PREMIUM, "Post extension product already owned (deprecated): ${product.id}")
                    logProductAlreadyOwned(product.id, "post_extension")
                }
            }
            
            result
        } catch (e: Exception) {
            logger.e(Logger.TAG_PREMIUM, "Unexpected error during post extension purchase: ${product.id}", e)
            
            premiumRepository.logPurchaseEvent(
                productId = product.id,
                productType = "post_extension",
                success = false,
                errorMessage = e.message
            )
            
            PurchaseResult.Error("Unexpected error: ${e.message}")
        }
    }

    // Analytics logging methods (TODO: Implement with actual analytics service)
    
    private fun logPremiumActivated(product: PremiumProduct) {
        logger.d(Logger.TAG_PREMIUM, "Analytics: Premium activated - ${product.type.name}")
        // TODO: Send to Firebase Analytics or other analytics service
        // Analytics.logEvent("premium_activated", mapOf(
        //     "product_id" to product.id,
        //     "product_type" to product.type.name,
        //     "price" to product.priceAmountMicros,
        //     "currency" to product.priceCurrencyCode
        // ))
    }
    
    private fun logPostExtensionPurchased(product: PostExtensionProduct) {
        logger.d(Logger.TAG_PREMIUM, "Analytics: Post extension purchased - ${product.id}")
        // TODO: Send to Firebase Analytics
        // Analytics.logEvent("post_extension_purchased", mapOf(
        //     "product_id" to product.id,
        //     "extension_hours" to product.extensionHours,
        //     "price" to product.priceAmountMicros
        // ))
    }
    
    private fun logPurchaseCancelled(productId: String, productType: String) {
        logger.d(Logger.TAG_PREMIUM, "Analytics: Purchase cancelled - $productId")
        // TODO: Send to Firebase Analytics
        // Analytics.logEvent("purchase_cancelled", mapOf(
        //     "product_id" to productId,
        //     "product_type" to productType
        // ))
    }
    
    private fun logPurchaseFailed(productId: String, productType: String, errorMessage: String) {
        logger.d(Logger.TAG_PREMIUM, "Analytics: Purchase failed - $productId: $errorMessage")
        // TODO: Send to Firebase Analytics
        // Analytics.logEvent("purchase_failed", mapOf(
        //     "product_id" to productId,
        //     "product_type" to productType,
        //     "error_message" to errorMessage
        // ))
    }
    
    private fun logPurchasePending(productId: String, productType: String) {
        logger.d(Logger.TAG_PREMIUM, "Analytics: Purchase pending - $productId")
        // TODO: Send to Firebase Analytics
    }
    
    private fun logPurchaseNetworkError(productId: String, productType: String) {
        logger.d(Logger.TAG_PREMIUM, "Analytics: Purchase network error - $productId")
        // TODO: Send to Firebase Analytics
    }
    
    private fun logProductNotAvailable(productId: String, productType: String) {
        logger.d(Logger.TAG_PREMIUM, "Analytics: Product not available - $productId")
        // TODO: Send to Firebase Analytics
    }
    
    private fun logProductAlreadyOwned(productId: String, productType: String) {
        logger.d(Logger.TAG_PREMIUM, "Analytics: Product already owned - $productId")
        // TODO: Send to Firebase Analytics
    }
}