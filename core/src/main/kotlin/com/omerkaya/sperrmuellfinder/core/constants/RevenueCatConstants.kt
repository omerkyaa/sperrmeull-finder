package com.omerkaya.sperrmuellfinder.core.constants

/**
 * 🔥 REVENUECAT CONSTANTS - SperrmüllFinder
 * Rules.md compliant - RevenueCat SDK 7.3.5 configuration and product definitions
 * 
 * Features:
 * - Premium subscription product IDs
 * - XP pack product IDs  
 * - Post extension product IDs
 * - Entitlement names and configuration
 * - Purchase flow constants
 * - Error handling constants
 */
object RevenueCatConstants {
    
    // ========================================
    // ENTITLEMENT NAMES (Single Source of Truth)
    // ========================================
    
    const val ENTITLEMENT_PREMIUM = "premium"  // Main premium entitlement
    
    // ========================================
    // PREMIUM SUBSCRIPTION PRODUCT IDS
    // ========================================
    
    // Premium Subscriptions (from rules.md naming convention)
    const val PRODUCT_PREMIUM_WEEK = "premium_week"
    const val PRODUCT_PREMIUM_MONTH = "premium_month"
    const val PRODUCT_PREMIUM_YEAR = "premium_year"
    
    // Premium Plus Subscriptions (enhanced tier)
    const val PRODUCT_PREMIUM_PLUS_MONTH = "premium_plus_month"
    const val PRODUCT_PREMIUM_PLUS_YEAR = "premium_plus_year"
    
    // ========================================
    // XP PACK PRODUCT IDS (In-App Purchases)
    // ========================================
    
    const val PRODUCT_XP_500 = "xp_500"      // €0.99 → +500 XP
    const val PRODUCT_XP_1500 = "xp_1500"    // €1.49 → +1500 XP + badge
    
    // ========================================
    // POST EXTENSION PRODUCT IDS (In-App Purchases)
    // ========================================
    
    const val PRODUCT_POST_EXTEND_6H = "post_extend_6h"  // €0.99 → +6 hours extension
    
    // ========================================
    // PACKAGE IDENTIFIERS (RevenueCat Dashboard)
    // ========================================
    
    // Premium Package Identifiers
    const val PACKAGE_PREMIUM_WEEKLY = "premium_weekly"
    const val PACKAGE_PREMIUM_MONTHLY = "premium_monthly"
    const val PACKAGE_PREMIUM_YEARLY = "premium_yearly"
    
    // Premium Plus Package Identifiers
    const val PACKAGE_PREMIUM_PLUS_MONTHLY = "premium_plus_monthly"
    const val PACKAGE_PREMIUM_PLUS_YEARLY = "premium_plus_yearly"
    
    // XP Pack Package Identifiers
    const val PACKAGE_XP_SMALL = "xp_small"    // 500 XP
    const val PACKAGE_XP_LARGE = "xp_large"    // 1500 XP
    
    // Extension Package Identifiers
    const val PACKAGE_POST_EXTENSION = "post_extension"
    
    // ========================================
    // PRODUCT COLLECTIONS (for UI display)
    // ========================================
    
    // All Premium Subscription Products
    val PREMIUM_SUBSCRIPTION_PRODUCTS = listOf(
        PRODUCT_PREMIUM_WEEK,
        PRODUCT_PREMIUM_MONTH,
        PRODUCT_PREMIUM_YEAR,
        PRODUCT_PREMIUM_PLUS_MONTH,
        PRODUCT_PREMIUM_PLUS_YEAR
    )
    
    // All XP Pack Products
    val XP_PACK_PRODUCTS = listOf(
        PRODUCT_XP_500,
        PRODUCT_XP_1500
    )
    
    // All Extension Products
    val EXTENSION_PRODUCTS = listOf(
        PRODUCT_POST_EXTEND_6H
    )
    
    // All Products (for initialization)
    val ALL_PRODUCTS = PREMIUM_SUBSCRIPTION_PRODUCTS + XP_PACK_PRODUCTS + EXTENSION_PRODUCTS
    
    // ========================================
    // PURCHASE RESULT TYPES
    // ========================================
    
    const val PURCHASE_TYPE_PREMIUM = "premium"
    const val PURCHASE_TYPE_XP_PACK = "xp_pack"
    const val PURCHASE_TYPE_EXTENSION = "extension"
    
    // ========================================
    // PURCHASE STATES (RevenueCat SDK 7.3.5)
    // ========================================
    
    const val PURCHASE_STATE_PURCHASED = "purchased"
    const val PURCHASE_STATE_PENDING = "pending"
    const val PURCHASE_STATE_CANCELLED = "cancelled"
    const val PURCHASE_STATE_FAILED = "failed"
    const val PURCHASE_STATE_RESTORED = "restored"
    
    // ========================================
    // ERROR CODES (RevenueCat specific)
    // ========================================
    
    const val ERROR_USER_CANCELLED = "user_cancelled"
    const val ERROR_PAYMENT_PENDING = "payment_pending"
    const val ERROR_INVALID_CREDENTIALS = "invalid_credentials"
    const val ERROR_UNEXPECTED = "unexpected_error"
    const val ERROR_SERVICE_UNAVAILABLE = "service_unavailable"
    const val ERROR_NETWORK_ERROR = "network_error"
    const val ERROR_INVALID_RECEIPT = "invalid_receipt"
    const val ERROR_PRODUCT_NOT_AVAILABLE = "product_not_available"
    const val ERROR_PURCHASE_NOT_ALLOWED = "purchase_not_allowed"
    const val ERROR_PRODUCT_ALREADY_OWNED = "product_already_owned"
    
    // ========================================
    // PREMIUM TYPES (for analytics and display)
    // ========================================
    
    const val PREMIUM_TYPE_WEEKLY = "weekly"
    const val PREMIUM_TYPE_MONTHLY = "monthly"
    const val PREMIUM_TYPE_YEARLY = "yearly"
    const val PREMIUM_TYPE_PLUS_MONTHLY = "plus_monthly"
    const val PREMIUM_TYPE_PLUS_YEARLY = "plus_yearly"
    
    // ========================================
    // XP PACK CONFIGURATIONS
    // ========================================
    
    // XP Pack Values (must match AppConstants)
    const val XP_PACK_500_VALUE = 500
    const val XP_PACK_1500_VALUE = 1500
    
    // XP Pack Bonuses
    const val XP_PACK_500_BONUS_XP = 0      // No bonus XP
    const val XP_PACK_1500_BONUS_XP = 0     // No bonus XP (badge is the bonus)
    
    // XP Pack Badge Rewards
    const val XP_PACK_500_BADGE = false     // No badge
    const val XP_PACK_1500_BADGE = true     // Includes badge
    
    // ========================================
    // PREMIUM SUBSCRIPTION BENEFITS
    // ========================================
    
    // Premium Monthly Benefits
    const val PREMIUM_MONTHLY_XP_BONUS = 800
    const val PREMIUM_MONTHLY_NOTIFICATION = true
    
    // Premium Plus Monthly Benefits  
    const val PREMIUM_PLUS_MONTHLY_XP_BONUS = 1500
    const val PREMIUM_PLUS_MONTHLY_BADGE = true
    const val PREMIUM_PLUS_MONTHLY_NOTIFICATION = true
    
    // ========================================
    // TRIAL & INTRO OFFER CONFIGURATION
    // ========================================
    
    const val TRIAL_PERIOD_DAYS = 7
    const val INTRO_OFFER_ENABLED = true
    
    // ========================================
    // PURCHASE FLOW TIMEOUTS
    // ========================================
    
    const val PURCHASE_TIMEOUT_SECONDS = 60L
    const val RESTORE_TIMEOUT_SECONDS = 30L
    const val PRODUCT_FETCH_TIMEOUT_SECONDS = 15L
    
    // ========================================
    // ANALYTICS EVENTS (RevenueCat specific)
    // ========================================
    
    const val EVENT_PURCHASE_STARTED = "rc_purchase_started"
    const val EVENT_PURCHASE_SUCCESS = "rc_purchase_success"
    const val EVENT_PURCHASE_FAILED = "rc_purchase_failed"
    const val EVENT_PURCHASE_CANCELLED = "rc_purchase_cancelled"
    const val EVENT_RESTORE_STARTED = "rc_restore_started"
    const val EVENT_RESTORE_SUCCESS = "rc_restore_success"
    const val EVENT_RESTORE_FAILED = "rc_restore_failed"
    const val EVENT_ENTITLEMENT_GRANTED = "rc_entitlement_granted"
    const val EVENT_ENTITLEMENT_REVOKED = "rc_entitlement_revoked"
    const val EVENT_PAYWALL_SHOWN = "rc_paywall_shown"
    const val EVENT_PAYWALL_DISMISSED = "rc_paywall_dismissed"
    
    // ========================================
    // PRODUCT METADATA KEYS
    // ========================================
    
    const val METADATA_XP_AMOUNT = "xp_amount"
    const val METADATA_INCLUDES_BADGE = "includes_badge"
    const val METADATA_EXTENSION_HOURS = "extension_hours"
    const val METADATA_PREMIUM_TYPE = "premium_type"
    const val METADATA_TRIAL_ELIGIBLE = "trial_eligible"
    
    // ========================================
    // CUSTOMER INFO ATTRIBUTES
    // ========================================
    
    const val ATTRIBUTE_USER_ID = "user_id"
    const val ATTRIBUTE_EMAIL = "email"
    const val ATTRIBUTE_DISPLAY_NAME = "display_name"
    const val ATTRIBUTE_CITY = "city"
    const val ATTRIBUTE_LEVEL = "level"
    const val ATTRIBUTE_XP = "xp"
    const val ATTRIBUTE_HONESTY = "honesty"
    const val ATTRIBUTE_DEVICE_LANG = "device_lang"
    const val ATTRIBUTE_REGISTRATION_DATE = "registration_date"
    
    // ========================================
    // PAYWALL CONFIGURATION
    // ========================================
    
    const val PAYWALL_IDENTIFIER_MAIN = "main_paywall"
    const val PAYWALL_IDENTIFIER_SEARCH = "search_paywall"
    const val PAYWALL_IDENTIFIER_MAP = "map_paywall"
    const val PAYWALL_IDENTIFIER_PROFILE = "profile_paywall"
    
    // ========================================
    // SUBSCRIPTION PERIODS (for display)
    // ========================================
    
    const val PERIOD_WEEKLY = "P1W"
    const val PERIOD_MONTHLY = "P1M"
    const val PERIOD_YEARLY = "P1Y"
    
    // ========================================
    // GRACE PERIOD CONFIGURATION
    // ========================================
    
    const val GRACE_PERIOD_DAYS = 3
    const val GRACE_PERIOD_WARNING_DAYS = 1
    
    // ========================================
    // HELPER FUNCTIONS FOR PRODUCT IDENTIFICATION
    // ========================================
    
    /**
     * Check if a product ID is a premium subscription
     */
    fun isPremiumSubscription(productId: String): Boolean {
        return productId in PREMIUM_SUBSCRIPTION_PRODUCTS
    }
    
    /**
     * Check if a product ID is an XP pack
     */
    fun isXpPack(productId: String): Boolean {
        return productId in XP_PACK_PRODUCTS
    }
    
    /**
     * Check if a product ID is a post extension
     */
    fun isPostExtension(productId: String): Boolean {
        return productId in EXTENSION_PRODUCTS
    }
    
    /**
     * Get XP amount for an XP pack product
     */
    fun getXpAmountForProduct(productId: String): Int {
        return when (productId) {
            PRODUCT_XP_500 -> XP_PACK_500_VALUE
            PRODUCT_XP_1500 -> XP_PACK_1500_VALUE
            else -> 0
        }
    }
    
    /**
     * Get premium type for a subscription product
     */
    fun getPremiumTypeForProduct(productId: String): String {
        return when (productId) {
            PRODUCT_PREMIUM_WEEK -> PREMIUM_TYPE_WEEKLY
            PRODUCT_PREMIUM_MONTH -> PREMIUM_TYPE_MONTHLY
            PRODUCT_PREMIUM_YEAR -> PREMIUM_TYPE_YEARLY
            PRODUCT_PREMIUM_PLUS_MONTH -> PREMIUM_TYPE_PLUS_MONTHLY
            PRODUCT_PREMIUM_PLUS_YEAR -> PREMIUM_TYPE_PLUS_YEARLY
            else -> ""
        }
    }
    
    /**
     * Check if a product includes a badge reward
     */
    fun includesBadge(productId: String): Boolean {
        return when (productId) {
            PRODUCT_XP_1500 -> true
            PRODUCT_PREMIUM_PLUS_MONTH, PRODUCT_PREMIUM_PLUS_YEAR -> true
            else -> false
        }
    }
}
