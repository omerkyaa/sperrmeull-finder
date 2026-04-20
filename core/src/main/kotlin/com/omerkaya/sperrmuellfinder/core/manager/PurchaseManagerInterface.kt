package com.omerkaya.sperrmuellfinder.core.manager

/**
 * PROFESSIONAL PURCHASE MANAGER INTERFACE
 * 
 * Clean architecture interface for purchase operations.
 * Implementation resides in data layer to avoid cross-module dependencies.
 * 
 * Features:
 * ✅ Clean separation of concerns
 * ✅ No external dependencies in core module
 * ✅ Testable interface
 * ✅ Implementation flexibility
 */
interface PurchaseManagerInterface {
    
    /**
     * Initialize the purchase system
     * @param apiKey RevenueCat API key
     */
    fun initialize(apiKey: String)
    
    /**
     * Check if purchase system is ready
     * @return true if ready, false otherwise
     */
    fun isReady(): Boolean
    
    /**
     * Check if user has premium subscription
     * @return true if premium, false otherwise
     */
    suspend fun isPremiumUser(): Boolean
    
    /**
     * Get initialization status for debugging
     * @return status string
     */
    fun getStatus(): String
    
    /**
     * Set user ID for purchase tracking (fire-and-forget)
     * @param userId user identifier
     */
    fun setUserId(userId: String)
    
    /**
     * Clear user data (fire-and-forget)
     */
    fun clearUserData()
}
