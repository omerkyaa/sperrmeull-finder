package com.omerkaya.sperrmuellfinder.core.util

import android.app.Application
import android.content.Context
import android.os.IBinder
import android.os.RemoteException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BinderProxy Manager for handling death recipient cleanup.
 * 
 * CRASH FIX: Fixes "BinderProxy is being destroyed but the application did not call 
 * unlinkToDeath to unlink all of its death recipients beforehand" warnings.
 * 
 * The root cause: When using Google Services, RevenueCat, or other system services,
 * BinderProxy objects can be created with death recipients that need to be properly
 * unlinked when the service connection is no longer needed.
 * 
 * Solution: Track and cleanup death recipients proactively.
 */
@Singleton
class BinderProxyManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: Logger
) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Track death recipients to ensure proper cleanup
    private val deathRecipients = ConcurrentHashMap<String, WeakReference<IBinder.DeathRecipient>>()
    private val binderProxies = ConcurrentHashMap<String, WeakReference<IBinder>>()
    
    companion object {
        private const val TAG = "BinderProxyManager"
        
        // Known services that can cause BinderProxy leaks
        private val KNOWN_LEAK_SOURCES = setOf(
            "com.google.android.gms",
            "com.revenuecat.purchases",
            "com.google.android.play.integrity",
            "com.android.vending",
            "com.google.firebase"
        )
    }
    
    /**
     * Initialize BinderProxy monitoring and cleanup.
     */
    fun initialize() {
        try {
            logger.i(TAG, "Initializing BinderProxy cleanup manager")
            
            // Setup periodic cleanup
            scope.launch {
                performPeriodicCleanup()
            }
            
            // Setup application lifecycle cleanup
            setupLifecycleCleanup()
            
            logger.i(TAG, "BinderProxy manager initialized successfully")
            
        } catch (e: Exception) {
            logger.e(TAG, "Failed to initialize BinderProxy manager", e)
        }
    }
    
    /**
     * Register a death recipient for tracking.
     */
    fun registerDeathRecipient(
        serviceName: String,
        binder: IBinder?,
        deathRecipient: IBinder.DeathRecipient
    ) {
        if (binder == null) return
        
        try {
            val key = "${serviceName}_${System.identityHashCode(deathRecipient)}"
            
            // Store references for cleanup
            deathRecipients[key] = WeakReference(deathRecipient)
            binderProxies[key] = WeakReference(binder)
            
            // Link to death with our tracking
            binder.linkToDeath(deathRecipient, 0)
            
            logger.d(TAG, "Registered death recipient for service: $serviceName")
            
        } catch (e: RemoteException) {
            logger.w(TAG, "Failed to link death recipient for $serviceName", e)
        } catch (e: Exception) {
            logger.e(TAG, "Error registering death recipient for $serviceName", e)
        }
    }
    
    /**
     * Unregister a death recipient.
     */
    fun unregisterDeathRecipient(
        serviceName: String,
        binder: IBinder?,
        deathRecipient: IBinder.DeathRecipient
    ) {
        if (binder == null) return
        
        try {
            val key = "${serviceName}_${System.identityHashCode(deathRecipient)}"
            
            // Unlink from death
            binder.unlinkToDeath(deathRecipient, 0)
            
            // Remove from tracking
            deathRecipients.remove(key)
            binderProxies.remove(key)
            
            logger.d(TAG, "Unregistered death recipient for service: $serviceName")
            
        } catch (e: Exception) {
            logger.w(TAG, "Error unregistering death recipient for $serviceName", e)
        }
    }
    
    /**
     * Perform comprehensive cleanup of all tracked death recipients.
     */
    fun performCleanup() {
        try {
            logger.i(TAG, "Performing BinderProxy cleanup...")
            
            var cleanedCount = 0
            val keysToRemove = mutableListOf<String>()
            
            deathRecipients.forEach { (key, deathRecipientRef) ->
                val deathRecipient = deathRecipientRef.get()
                val binder = binderProxies[key]?.get()
                
                if (deathRecipient == null || binder == null) {
                    // References are null, mark for removal
                    keysToRemove.add(key)
                } else {
                    // Try to unlink
                    try {
                        binder.unlinkToDeath(deathRecipient, 0)
                        cleanedCount++
                        keysToRemove.add(key)
                    } catch (e: Exception) {
                        logger.d(TAG, "Death recipient already unlinked or binder dead: $key")
                        keysToRemove.add(key)
                    }
                }
            }
            
            // Remove cleaned entries
            keysToRemove.forEach { key ->
                deathRecipients.remove(key)
                binderProxies.remove(key)
            }
            
            logger.i(TAG, "BinderProxy cleanup completed: $cleanedCount recipients cleaned, ${keysToRemove.size} entries removed")
            
        } catch (e: Exception) {
            logger.e(TAG, "Error during BinderProxy cleanup", e)
        }
    }
    
    /**
     * Get cleanup statistics.
     */
    fun getCleanupStats(): BinderProxyStats {
        return BinderProxyStats(
            trackedRecipients = deathRecipients.size,
            trackedBinders = binderProxies.size,
            activeRecipients = deathRecipients.values.count { it.get() != null },
            activeBinders = binderProxies.values.count { it.get() != null }
        )
    }
    
    private suspend fun performPeriodicCleanup() {
        try {
            // Perform cleanup every 5 minutes
            kotlinx.coroutines.delay(5 * 60 * 1000)
            
            val stats = getCleanupStats()
            if (stats.trackedRecipients > 0) {
                logger.d(TAG, "Periodic cleanup check: ${stats.trackedRecipients} recipients tracked")
                performCleanup()
            }
            
            // Schedule next cleanup
            performPeriodicCleanup()
            
        } catch (e: Exception) {
            logger.e(TAG, "Error in periodic cleanup", e)
        }
    }
    
    private fun setupLifecycleCleanup() {
        try {
            // Register for application lifecycle events
            val application = context.applicationContext as Application
            application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
                override fun onActivityStarted(activity: android.app.Activity) {}
                override fun onActivityResumed(activity: android.app.Activity) {}
                override fun onActivityPaused(activity: android.app.Activity) {}
                override fun onActivityStopped(activity: android.app.Activity) {}
                override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
                
                override fun onActivityDestroyed(activity: android.app.Activity) {
                    // Perform cleanup when activities are destroyed
                    scope.launch {
                        performCleanup()
                    }
                }
            })
            
            logger.d(TAG, "Lifecycle cleanup registered")
            
        } catch (e: Exception) {
            logger.e(TAG, "Failed to setup lifecycle cleanup", e)
        }
    }
}

/**
 * Statistics for BinderProxy cleanup operations.
 */
data class BinderProxyStats(
    val trackedRecipients: Int,
    val trackedBinders: Int,
    val activeRecipients: Int,
    val activeBinders: Int
) {
    val hasLeaks: Boolean
        get() = trackedRecipients > activeRecipients || trackedBinders > activeBinders
        
    val leakCount: Int
        get() = (trackedRecipients - activeRecipients) + (trackedBinders - activeBinders)
}
