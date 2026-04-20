package com.omerkaya.sperrmuellfinder.core.util

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Crash Reporting Manager for handling error telemetry and crash reporting.
 * 
 * CRASH FIX: Provides comprehensive error tracking and reporting to help
 * identify and fix crashes in production.
 */
@Singleton
class CrashReportingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: Logger
) {
    
    private var isInitialized = false
    private val crashlytics: FirebaseCrashlytics? by lazy {
        try {
            FirebaseCrashlytics.getInstance()
        } catch (e: Exception) {
            logger.w(Logger.TAG_DEFAULT, "Firebase Crashlytics not available", e)
            null
        }
    }
    
    /**
     * Initialize crash reporting.
     */
    fun initialize() {
        try {
            crashlytics?.let { crashlytics ->
                // Enable crash collection
                crashlytics.setCrashlyticsCollectionEnabled(true)
                
                // Set app version and build info
                crashlytics.setCustomKey("app_version", getAppVersion())
                crashlytics.setCustomKey("build_type", getBuildType())
                crashlytics.setCustomKey("device_info", getDeviceInfo())
                crashlytics.setCustomKey("version_name", getVersionName())
                crashlytics.setCustomKey("version_code", getVersionCode())
                
                isInitialized = true
                logger.i(Logger.TAG_DEFAULT, "Crash reporting initialized successfully")
            } ?: run {
                logger.w(Logger.TAG_DEFAULT, "Crashlytics not available - crash reporting disabled")
            }
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Failed to initialize crash reporting", e)
        }
    }
    
    /**
     * Report a non-fatal error with context.
     */
    fun reportNonFatalError(
        error: Throwable,
        context: String? = null,
        additionalData: Map<String, String> = emptyMap()
    ) {
        try {
            crashlytics?.let { crashlytics ->
                // Add context information
                context?.let { crashlytics.setCustomKey("error_context", it) }
                
                // Add additional data
                additionalData.forEach { (key, value) ->
                    crashlytics.setCustomKey(key, value)
                }
                
                // Record the error
                crashlytics.recordException(error)
                
                logger.w(Logger.TAG_DEFAULT, "Non-fatal error reported: ${error.message}")
            }
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Failed to report non-fatal error", e)
        }
    }
    
    /**
     * Report a crash fix event (when we successfully handle a potential crash).
     */
    fun reportCrashFixEvent(
        fixType: CrashFixType,
        originalError: Throwable? = null,
        description: String? = null
    ) {
        try {
            crashlytics?.let { crashlytics ->
                crashlytics.setCustomKey("crash_fix_type", fixType.name)
                description?.let { crashlytics.setCustomKey("fix_description", it) }
                
                // Create a breadcrumb for the fix
                val message = "CRASH_FIX: ${fixType.description} - ${description ?: "No description"}"
                crashlytics.log(message)
                
                // If there was an original error, report it as handled
                originalError?.let { error ->
                    crashlytics.setCustomKey("original_error", error.message ?: "Unknown")
                    crashlytics.recordException(HandledCrashException(fixType, error))
                }
                
                logger.i(Logger.TAG_DEFAULT, "Crash fix event reported: $fixType")
            }
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Failed to report crash fix event", e)
        }
    }
    
    /**
     * Set user identifier for crash reports.
     */
    fun setUserId(userId: String?) {
        try {
            crashlytics?.setUserId(userId ?: "anonymous")
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Failed to set user ID for crash reporting", e)
        }
    }
    
    /**
     * Add breadcrumb for debugging.
     */
    fun addBreadcrumb(message: String, category: String = "general") {
        try {
            crashlytics?.let { crashlytics ->
                val breadcrumb = "[$category] $message"
                crashlytics.log(breadcrumb)
                logger.d(Logger.TAG_DEFAULT, "Breadcrumb added: $breadcrumb")
            }
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Failed to add breadcrumb", e)
        }
    }
    
    /**
     * Set custom key-value data for crash reports.
     */
    fun setCustomData(key: String, value: String) {
        try {
            crashlytics?.setCustomKey(key, value)
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Failed to set custom data for crash reporting", e)
        }
    }
    
    /**
     * Report startup metrics.
     */
    fun reportStartupMetrics(
        startupDuration: Long,
        startupPhase: String,
        wasSuccessful: Boolean,
        errors: List<String> = emptyList()
    ) {
        try {
            crashlytics?.let { crashlytics ->
                crashlytics.setCustomKey("startup_duration_ms", startupDuration)
                crashlytics.setCustomKey("startup_phase", startupPhase)
                crashlytics.setCustomKey("startup_successful", wasSuccessful)
                crashlytics.setCustomKey("startup_errors_count", errors.size)
                
                if (errors.isNotEmpty()) {
                    crashlytics.setCustomKey("startup_errors", errors.joinToString("; "))
                }
                
                val message = "STARTUP_METRICS: Duration=${startupDuration}ms, Phase=$startupPhase, Success=$wasSuccessful"
                crashlytics.log(message)
                
                logger.i(Logger.TAG_DEFAULT, "Startup metrics reported: $message")
            }
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Failed to report startup metrics", e)
        }
    }
    
    /**
     * Report RevenueCat error.
     */
    fun reportRevenueCatError(error: Throwable, operation: String) {
        reportNonFatalError(
            error = error,
            context = "RevenueCat Operation",
            additionalData = mapOf(
                "operation" to operation,
                "error_type" to "revenuecat"
            )
        )
    }
    
    /**
     * Report Google Services error.
     */
    fun reportGoogleServicesError(error: Throwable, service: String) {
        reportNonFatalError(
            error = error,
            context = "Google Services",
            additionalData = mapOf(
                "service" to service,
                "error_type" to "google_services"
            )
        )
    }
    
    /**
     * Report navigation error.
     */
    fun reportNavigationError(error: Throwable, route: String?) {
        reportNonFatalError(
            error = error,
            context = "Navigation Error",
            additionalData = mapOf(
                "route" to (route ?: "unknown"),
                "error_type" to "navigation"
            )
        )
    }
    
    /**
     * Test crash reporting (debug only).
     */
    fun testCrashReporting() {
        if (isDebugBuild()) {
            logger.w(Logger.TAG_DEFAULT, "Testing crash reporting...")
            crashlytics?.log("Test crash reporting event")
            
            // Don't actually crash in production
            reportNonFatalError(
                error = Exception("Test crash reporting exception"),
                context = "Debug Test"
            )
        }
    }
    
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.versionCode})"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    private fun getBuildType(): String {
        return try {
            // Try to detect build type from application info
            if (isDebugBuild()) "debug" else "release"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    private fun getDeviceInfo(): String {
        return try {
            "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (API ${android.os.Build.VERSION.SDK_INT})"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    private fun getVersionName(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    private fun getVersionCode(): Long {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.longVersionCode
        } catch (e: Exception) {
            0L
        }
    }
    
    private fun isDebugBuild(): Boolean {
        return try {
            // Check application flags for debug mode
            context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Types of crash fixes for telemetry.
 */
enum class CrashFixType(val description: String) {
    NULL_SAFETY("Null safety fix applied"),
    REVENUECAT_ERROR("RevenueCat error handled"),
    GOOGLE_SERVICES_ERROR("Google Services error handled"),
    NAVIGATION_ERROR("Navigation error handled"),
    RESOURCE_ERROR("Resource error handled"),
    NETWORK_ERROR("Network error handled"),
    STARTUP_ERROR("Startup error handled"),
    NATIVE_LIBRARY_ERROR("Native library error handled"),
    PERMISSION_ERROR("Permission error handled"),
    GENERAL_ERROR("General error handled")
}

/**
 * Custom exception for handled crashes.
 */
class HandledCrashException(
    val fixType: CrashFixType,
    val originalError: Throwable
) : Exception("Handled crash: ${fixType.description} - Original: ${originalError.message}", originalError)
