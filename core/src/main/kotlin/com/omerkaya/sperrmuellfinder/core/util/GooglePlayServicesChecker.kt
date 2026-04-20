package com.omerkaya.sperrmuellfinder.core.util

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Professional Google Play Services availability checker
 * Handles GPS connection issues gracefully with proper fallbacks
 * Rules.md compliant - Clean error handling and user feedback
 */
@Singleton
class GooglePlayServicesChecker @Inject constructor(
    private val logger: Logger
) {
    
    companion object {
        private const val TAG = "GooglePlayServicesChecker"
        private const val REQUEST_CODE_GOOGLE_PLAY_SERVICES = 1000
    }
    
    /**
     * Check if Google Play Services is available and up to date
     */
    fun checkGooglePlayServicesAvailability(context: Context): GooglePlayServicesStatus {
        return try {
            val googleApiAvailability = GoogleApiAvailability.getInstance()
            val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
            
            when (resultCode) {
                ConnectionResult.SUCCESS -> {
                    logger.d(TAG, "Google Play Services is available and up to date")
                    GooglePlayServicesStatus.Available
                }
                ConnectionResult.SERVICE_MISSING -> {
                    logger.w(TAG, "Google Play Services is missing")
                    GooglePlayServicesStatus.Missing
                }
                ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> {
                    logger.w(TAG, "Google Play Services needs update")
                    GooglePlayServicesStatus.NeedsUpdate
                }
                ConnectionResult.SERVICE_DISABLED -> {
                    logger.w(TAG, "Google Play Services is disabled")
                    GooglePlayServicesStatus.Disabled
                }
                ConnectionResult.SERVICE_INVALID -> {
                    logger.w(TAG, "Google Play Services version is invalid")
                    GooglePlayServicesStatus.Invalid
                }
                else -> {
                    logger.w(TAG, "Google Play Services error: $resultCode")
                    GooglePlayServicesStatus.Error(resultCode)
                }
            }
        } catch (e: SecurityException) {
            logger.e(TAG, "SecurityException checking Google Play Services - likely emulator or system issue", e)
            GooglePlayServicesStatus.SecurityError(e)
        } catch (e: Exception) {
            logger.e(TAG, "Unexpected error checking Google Play Services", e)
            GooglePlayServicesStatus.UnknownError(e)
        }
    }
    
    /**
     * Check if Google Play Services is usable for our app
     */
    fun isGooglePlayServicesUsable(context: Context): Boolean {
        return when (checkGooglePlayServicesAvailability(context)) {
            is GooglePlayServicesStatus.Available -> true
            else -> false
        }
    }
    
    /**
     * Get user-friendly error message for GPS issues
     */
    fun getErrorMessage(status: GooglePlayServicesStatus, context: Context): String {
        return when (status) {
            is GooglePlayServicesStatus.Available -> ""
            is GooglePlayServicesStatus.Missing -> 
                "Google Play Services ist nicht installiert. Bitte installieren Sie es aus dem Play Store."
            is GooglePlayServicesStatus.NeedsUpdate -> 
                "Google Play Services muss aktualisiert werden. Bitte aktualisieren Sie es im Play Store."
            is GooglePlayServicesStatus.Disabled -> 
                "Google Play Services ist deaktiviert. Bitte aktivieren Sie es in den Einstellungen."
            is GooglePlayServicesStatus.Invalid -> 
                "Google Play Services Version ist ungültig. Bitte neu installieren."
            is GooglePlayServicesStatus.SecurityError -> 
                "Sicherheitsfehler bei Google Play Services. App funktioniert möglicherweise eingeschränkt."
            is GooglePlayServicesStatus.Error -> 
                "Google Play Services Fehler (Code: ${status.errorCode}). Versuchen Sie einen Neustart."
            is GooglePlayServicesStatus.UnknownError -> 
                "Unbekannter Google Play Services Fehler. App funktioniert möglicherweise eingeschränkt."
        }
    }
    
    /**
     * Handle GPS errors gracefully with appropriate user feedback
     */
    fun handleGooglePlayServicesError(
        context: Context,
        status: GooglePlayServicesStatus,
        onResolved: () -> Unit = {},
        onFallback: () -> Unit = {}
    ) {
        when (status) {
            is GooglePlayServicesStatus.Available -> {
                onResolved()
            }
            is GooglePlayServicesStatus.NeedsUpdate,
            is GooglePlayServicesStatus.Missing,
            is GooglePlayServicesStatus.Disabled -> {
                logger.w(TAG, "Google Play Services issue: $status - attempting resolution")
                // In production, show user dialog to fix GPS
                onFallback()
            }
            is GooglePlayServicesStatus.SecurityError,
            is GooglePlayServicesStatus.Invalid,
            is GooglePlayServicesStatus.Error,
            is GooglePlayServicesStatus.UnknownError -> {
                logger.e(TAG, "Serious Google Play Services issue: $status - using fallback")
                onFallback()
            }
        }
    }
}

/**
 * Google Play Services availability status
 */
sealed class GooglePlayServicesStatus {
    object Available : GooglePlayServicesStatus()
    object Missing : GooglePlayServicesStatus()
    object NeedsUpdate : GooglePlayServicesStatus()
    object Disabled : GooglePlayServicesStatus()
    object Invalid : GooglePlayServicesStatus()
    data class Error(val errorCode: Int) : GooglePlayServicesStatus()
    data class SecurityError(val exception: SecurityException) : GooglePlayServicesStatus()
    data class UnknownError(val exception: Exception) : GooglePlayServicesStatus()
}
