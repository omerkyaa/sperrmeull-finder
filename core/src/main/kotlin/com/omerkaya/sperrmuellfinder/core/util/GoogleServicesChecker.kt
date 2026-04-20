package com.omerkaya.sperrmuellfinder.core.util

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Services Checker for handling Google Play Services availability issues.
 * 
 * CRASH FIX: Provides comprehensive checking and fallback mechanisms for
 * Google Play Services related SecurityExceptions and availability issues.
 */
@Singleton
class GoogleServicesChecker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: Logger
) {
    
    private var lastCheckResult: GoogleServicesStatus? = null
    private var lastCheckTime: Long = 0
    
    // Cache check results for 30 seconds to avoid repeated checks
    private val cacheValidityMs = 30_000L
    
    /**
     * Comprehensive Google Services availability check.
     * 
     * @return GoogleServicesStatus indicating the availability and any issues
     */
    fun checkGoogleServicesAvailability(): GoogleServicesStatus {
        return resolveStatus(useCache = true)
    }

    private fun resolveStatus(useCache: Boolean): GoogleServicesStatus {
        val currentTime = System.currentTimeMillis()
        
        // Return cached result if still valid
        if (useCache && lastCheckResult != null && (currentTime - lastCheckTime) < cacheValidityMs) {
            return lastCheckResult!!
        }
        
        return try {
            val googleApiAvailability = GoogleApiAvailability.getInstance()
            val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
            
            val status = when (resultCode) {
                ConnectionResult.SUCCESS -> {
                    logger.i(Logger.TAG_DEFAULT, "Google Play Services are available and up to date")
                    GoogleServicesStatus.Available
                }
                
                ConnectionResult.SERVICE_MISSING -> {
                    logger.w(Logger.TAG_DEFAULT, "Google Play Services are not installed")
                    GoogleServicesStatus.NotInstalled(
                        "Google Play Services not installed",
                        isResolvable = googleApiAvailability.isUserResolvableError(resultCode)
                    )
                }
                
                ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> {
                    logger.w(Logger.TAG_DEFAULT, "Google Play Services need to be updated")
                    GoogleServicesStatus.UpdateRequired(
                        "Google Play Services update required",
                        isResolvable = googleApiAvailability.isUserResolvableError(resultCode)
                    )
                }
                
                ConnectionResult.SERVICE_DISABLED -> {
                    logger.w(Logger.TAG_DEFAULT, "Google Play Services are disabled")
                    GoogleServicesStatus.Disabled(
                        "Google Play Services disabled",
                        isResolvable = googleApiAvailability.isUserResolvableError(resultCode)
                    )
                }
                
                ConnectionResult.SERVICE_INVALID -> {
                    logger.w(Logger.TAG_DEFAULT, "Google Play Services version is invalid")
                    GoogleServicesStatus.Invalid(
                        "Invalid Google Play Services version",
                        isResolvable = googleApiAvailability.isUserResolvableError(resultCode)
                    )
                }
                
                else -> {
                    logger.w(Logger.TAG_DEFAULT, "Google Play Services error: $resultCode")
                    GoogleServicesStatus.Error(
                        "Google Play Services error (code: $resultCode)",
                        resultCode,
                        isResolvable = googleApiAvailability.isUserResolvableError(resultCode)
                    )
                }
            }
            
            // Cache the result
            lastCheckResult = status
            lastCheckTime = currentTime
            
            status
            
        } catch (e: SecurityException) {
            logger.e(Logger.TAG_DEFAULT, "SecurityException checking Google Services - package name mismatch?", e)
            val securityStatus = GoogleServicesStatus.SecurityError(
                "Security error: ${e.message}",
                e
            )
            lastCheckResult = securityStatus
            lastCheckTime = currentTime
            securityStatus
            
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Unexpected error checking Google Services", e)
            val errorStatus = GoogleServicesStatus.UnknownError(
                "Unknown error: ${e.message}",
                e
            )
            lastCheckResult = errorStatus
            lastCheckTime = currentTime
            errorStatus
        }
    }
    
    /**
     * Check if Google Services are available for use.
     * This is a simple boolean check for quick availability testing.
     */
    fun isGoogleServicesAvailable(): Boolean {
        return when (resolveStatus(useCache = false)) {
            is GoogleServicesStatus.Available -> true
            else -> false
        }
    }
    
    /**
     * Check if Google Services can be used safely without throwing exceptions.
     * This includes availability and security checks.
     */
    fun canUseGoogleServicesSafely(): Boolean {
        return when (val status = resolveStatus(useCache = false)) {
            is GoogleServicesStatus.Available -> true
            is GoogleServicesStatus.SecurityError -> {
                logger.w(Logger.TAG_DEFAULT, "Google Services security issue - cannot use safely")
                false
            }
            else -> {
                logger.w(Logger.TAG_DEFAULT, "Google Services not available: ${status.message}")
                false
            }
        }
    }
    
    /**
     * Get a user-friendly message about Google Services status.
     */
    fun getStatusMessage(): String {
        return when (val status = resolveStatus(useCache = false)) {
            is GoogleServicesStatus.Available -> "Google Services sind verfügbar"
            is GoogleServicesStatus.NotInstalled -> "Google Play Services sind nicht installiert"
            is GoogleServicesStatus.UpdateRequired -> "Google Play Services müssen aktualisiert werden"
            is GoogleServicesStatus.Disabled -> "Google Play Services sind deaktiviert"
            is GoogleServicesStatus.Invalid -> "Google Play Services Version ist ungültig"
            is GoogleServicesStatus.SecurityError -> "Sicherheitsfehler bei Google Services"
            is GoogleServicesStatus.Error -> "Google Services Fehler (Code: ${status.errorCode})"
            is GoogleServicesStatus.UnknownError -> "Unbekannter Google Services Fehler"
        }
    }
    
    /**
     * Clear cached results to force a fresh check.
     */
    fun clearCache() {
        lastCheckResult = null
        lastCheckTime = 0
        logger.d(Logger.TAG_DEFAULT, "Google Services check cache cleared")
    }
    
    /**
     * Perform a comprehensive diagnostic of Google Services.
     * This provides detailed information for debugging.
     */
    fun performDiagnostic(): GoogleServicesDiagnostic {
        val status = checkGoogleServicesAvailability()
        
        val packageInfo = try {
            val pm = context.packageManager
            val info = pm.getPackageInfo("com.google.android.gms", 0)
            GoogleServicesPackageInfo(
                isInstalled = true,
                versionName = info.versionName,
                versionCode = info.versionCode.toLong(),
                packageName = info.packageName
            )
        } catch (e: Exception) {
            logger.w(Logger.TAG_DEFAULT, "Could not get Google Services package info", e)
            GoogleServicesPackageInfo(
                isInstalled = false,
                versionName = null,
                versionCode = null,
                packageName = "com.google.android.gms"
            )
        }
        
        val appPackageInfo = try {
            val pm = context.packageManager
            val info = pm.getPackageInfo(context.packageName, 0)
            AppPackageInfo(
                packageName = info.packageName,
                versionName = info.versionName,
                versionCode = info.versionCode.toLong()
            )
        } catch (e: Exception) {
            logger.w(Logger.TAG_DEFAULT, "Could not get app package info", e)
            AppPackageInfo(
                packageName = context.packageName,
                versionName = "unknown",
                versionCode = 0L
            )
        }
        
        return GoogleServicesDiagnostic(
            status = status,
            googleServicesPackage = packageInfo,
            appPackage = appPackageInfo,
            timestamp = System.currentTimeMillis()
        )
    }
}

/**
 * Sealed class representing different Google Services availability states.
 */
sealed class GoogleServicesStatus(val message: String) {
    data object Available : GoogleServicesStatus("Google Services available")
    
    data class NotInstalled(
        val statusMessage: String,
        val isResolvable: Boolean
    ) : GoogleServicesStatus(statusMessage)
    
    data class UpdateRequired(
        val statusMessage: String,
        val isResolvable: Boolean
    ) : GoogleServicesStatus(statusMessage)
    
    data class Disabled(
        val statusMessage: String,
        val isResolvable: Boolean
    ) : GoogleServicesStatus(statusMessage)
    
    data class Invalid(
        val statusMessage: String,
        val isResolvable: Boolean
    ) : GoogleServicesStatus(statusMessage)
    
    data class SecurityError(
        val statusMessage: String,
        val exception: SecurityException
    ) : GoogleServicesStatus(statusMessage)
    
    data class Error(
        val statusMessage: String,
        val errorCode: Int,
        val isResolvable: Boolean
    ) : GoogleServicesStatus(statusMessage)
    
    data class UnknownError(
        val statusMessage: String,
        val exception: Exception
    ) : GoogleServicesStatus(statusMessage)
}

/**
 * Data class containing Google Services package information.
 */
data class GoogleServicesPackageInfo(
    val isInstalled: Boolean,
    val versionName: String?,
    val versionCode: Long?,
    val packageName: String
)

/**
 * Data class containing app package information.
 */
data class AppPackageInfo(
    val packageName: String,
    val versionName: String?,
    val versionCode: Long
)

/**
 * Comprehensive diagnostic information about Google Services.
 */
data class GoogleServicesDiagnostic(
    val status: GoogleServicesStatus,
    val googleServicesPackage: GoogleServicesPackageInfo,
    val appPackage: AppPackageInfo,
    val timestamp: Long
)
