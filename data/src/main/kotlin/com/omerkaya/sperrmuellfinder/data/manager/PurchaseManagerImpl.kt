package com.omerkaya.sperrmuellfinder.data.manager

import android.content.Context
import android.content.pm.ApplicationInfo
// BuildConfig will be passed as parameter since data module doesn't have access to app BuildConfig
import com.omerkaya.sperrmuellfinder.core.manager.PurchaseManagerInterface
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * BULLETPROOF PURCHASE MANAGER IMPLEMENTATION
 * 
 * Production-ready implementation of PurchaseManagerInterface with comprehensive error handling.
 * 
 * Features:
 * ✅ Never crashes the app - all operations are safe
 * ✅ Debug mode bypass for development
 * ✅ Graceful degradation when billing unavailable
 * ✅ Comprehensive logging for debugging
 * ✅ Coroutine-safe async operations
 * ✅ Thread-safe initialization
 * ✅ Clean architecture compliance
 * 
 * Usage:
 * - Injected via Hilt in data layer
 * - Called through interface from domain/presentation layers
 * - All methods handle errors gracefully
 */
@Singleton
class PurchaseManagerImpl @Inject constructor(
    private val context: Context,
    private val logger: Logger
) : PurchaseManagerInterface {
    
    companion object {
        private const val TAG = "PurchaseManager"
        
        // Entitlement IDs (must match RevenueCat Dashboard)
        const val PREMIUM_ENTITLEMENT_ID = "premium"
        
        // Debug mode settings
        private const val DEBUG_PREMIUM_ACCESS = true // Set to true for debug builds
    }
    
    @Volatile
    private var isInitialized = false
    
    @Volatile
    private var initializationFailed = false
    
    private val initializationLock = Any()
    
    /**
     * CRITICAL: Initialize RevenueCat with bulletproof error handling
     * Never crashes - always continues app execution
     */
    override fun initialize(apiKey: String) {
        if (isInitialized) {
            logger.d(TAG, "Already initialized")
            return
        }
        
        synchronized(initializationLock) {
            if (isInitialized) return
            
            try {
                logger.d(TAG, "Initializing PurchaseManager...")
                
                // Debug mode bypass using system detection
                if (isDebugBuild()) {
                    logger.d(TAG, "DEBUG MODE: RevenueCat initialization skipped")
                    isInitialized = true
                    return
                }
                
                // Validate API key
                when {
                    apiKey.isBlank() -> {
                        logger.w(TAG, "Empty API key - purchases disabled")
                        initializationFailed = true
                        return
                    }
                    apiKey == "placeholder_key" -> {
                        logger.w(TAG, "Placeholder API key - purchases disabled for development")
                        initializationFailed = true
                        return
                    }
                    apiKey.length < 10 -> {
                        logger.w(TAG, "Invalid API key format - purchases disabled")
                        initializationFailed = true
                        return
                    }
                }
                
                // Configure RevenueCat with modern log level API
                Purchases.logLevel = if (isDebugBuild()) {
                    LogLevel.DEBUG
                } else {
                    LogLevel.INFO
                }
                Purchases.configure(
                    PurchasesConfiguration.Builder(context, apiKey)
                        .observerMode(false)
                        .build()
                )
                
                isInitialized = true
                logger.i(TAG, "RevenueCat initialized successfully")
                
            } catch (e: Exception) {
                handleInitializationError(e)
            }
        }
    }
    
    /**
     * CRITICAL: Categorize and handle initialization errors gracefully
     */
    private fun handleInitializationError(error: Exception) {
        initializationFailed = true
        
        when {
            error.message?.contains("ConfigurationError") == true -> {
                logger.w(TAG, "RevenueCat configuration error - dashboard not configured", error)
            }
            error.message?.contains("StoreProblemError") == true -> {
                logger.w(TAG, "Google Play Billing unavailable - normal in emulator", error)
            }
            error.message?.contains("NetworkError") == true -> {
                logger.w(TAG, "Network error during initialization - will retry later", error)
            }
            error.message?.contains("SecurityException") == true -> {
                logger.w(TAG, "Google Play Services security error - check manifest", error)
            }
            else -> {
                logger.e(TAG, "Unexpected initialization error", error)
            }
        }
        
        logger.i(TAG, "App continues without premium features")
    }
    
    /**
     * Check if RevenueCat is ready for use
     */
    override fun isReady(): Boolean {
        return try {
            isInitialized && !initializationFailed
        } catch (e: Exception) {
            logger.w(TAG, "Error checking readiness", e)
            false
        }
    }
    
    /**
     * BULLETPROOF: Check if user has premium subscription
     * Never crashes - always returns a boolean
     */
    override suspend fun isPremiumUser(): Boolean {
        return try {
            // Debug mode override
            if (isDebugBuild() && DEBUG_PREMIUM_ACCESS) {
                logger.d(TAG, "DEBUG MODE: Returning premium access = true")
                return true
            }
            
            // Check initialization status
            if (!isReady()) {
                logger.d(TAG, "RevenueCat not ready - returning false")
                return false
            }
            
            // Safe customer info retrieval
            val customerInfo = getCustomerInfoSafely()
            val isPremium = customerInfo?.entitlements?.get(PREMIUM_ENTITLEMENT_ID)?.isActive == true
            
            logger.d(TAG, "Premium status: $isPremium")
            return isPremium
            
        } catch (e: Exception) {
            logger.e(TAG, "Error checking premium status", e)
            false // Graceful degradation
        }
    }
    
    /**
     * Safe coroutine wrapper for customer info
     */
    private suspend fun getCustomerInfoSafely(): CustomerInfo? {
        return try {
            suspendCancellableCoroutine { continuation ->
                Purchases.sharedInstance.getCustomerInfoWith(
                    onSuccess = { customerInfo ->
                        if (continuation.isActive) {
                            continuation.resume(customerInfo)
                        }
                    },
                    onError = { error ->
                        logger.w(TAG, "Customer info error: ${error.message}")
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                )
            }
        } catch (e: Exception) {
            logger.e(TAG, "Exception getting customer info", e)
            null
        }
    }
    
    /**
     * Set user ID for RevenueCat (safe, fire-and-forget)
     */
    override fun setUserId(userId: String) {
        try {
            if (!isReady()) {
                logger.d(TAG, "RevenueCat not ready - skipping setUserId")
                return
            }
            
            if (userId.isBlank()) {
                logger.w(TAG, "Empty userId provided")
                return
            }
            
            logger.d(TAG, "Setting RevenueCat user ID: $userId")
            // RevenueCat 7.3.5 - Simple fire-and-forget approach
            runCatching {
                // Use the simpler API without callback for fire-and-forget behavior
                Purchases.sharedInstance.logIn(userId, null)
                logger.d(TAG, "RevenueCat logIn initiated successfully")
            }.onFailure { error ->
                logger.w(TAG, "RevenueCat logIn call failed: ${error.message}")
            }
            
        } catch (e: Exception) {
            logger.e(TAG, "Error setting user ID", e)
        }
    }
    
    /**
     * Clear user data (safe, fire-and-forget)
     */
    override fun clearUserData() {
        try {
            if (!isReady()) {
                logger.d(TAG, "RevenueCat not ready - skipping logout")
                return
            }
            
            logger.d(TAG, "Logging out RevenueCat user")
            // RevenueCat 7.3.5 - Simple fire-and-forget approach
            runCatching {
                // Use the simpler API without callback for fire-and-forget behavior
                Purchases.sharedInstance.logOut(null)
                logger.d(TAG, "RevenueCat logOut initiated successfully")
            }.onFailure { error ->
                logger.w(TAG, "RevenueCat logOut call failed: ${error.message}")
            }
            
        } catch (e: Exception) {
            logger.e(TAG, "Error clearing user data", e)
        }
    }
    
    /**
     * Get initialization status for debugging
     */
    override fun getStatus(): String {
        return when {
            isDebugBuild() -> "DEBUG_MODE"
            !isInitialized && !initializationFailed -> "NOT_INITIALIZED"
            initializationFailed -> "INITIALIZATION_FAILED"
            isInitialized -> "READY"
            else -> "UNKNOWN"
        }
    }
    
    /**
     * PROFESSIONAL: Debug mode detection without BuildConfig dependency
     * Uses system properties and application info for reliable debug detection
     */
    private fun isDebugBuild(): Boolean {
        return try {
            // Method 1: Check application flags
            val applicationInfo = context.applicationInfo
            val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            
            // Method 2: Check system property (backup method)
            val debugProperty = System.getProperty("ro.debuggable", "0") == "1"
            
            // Return true if either method indicates debug mode
            val result = isDebuggable || debugProperty
            
            logger.d(TAG, "Debug mode detection: isDebuggable=$isDebuggable, debugProperty=$debugProperty, result=$result")
            result
            
        } catch (e: Exception) {
            logger.w(TAG, "Error detecting debug mode, assuming release", e)
            false // Default to release mode if detection fails
        }
    }
}
