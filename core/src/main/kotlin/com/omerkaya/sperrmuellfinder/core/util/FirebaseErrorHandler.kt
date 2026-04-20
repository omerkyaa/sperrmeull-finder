package com.omerkaya.sperrmuellfinder.core.util

import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.StorageException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Professional Firebase error handler
 * Provides graceful degradation and user-friendly error messages
 * Rules.md compliant - Clean error handling with i18n support
 */
@Singleton
class FirebaseErrorHandler @Inject constructor(
    private val logger: Logger
) {
    
    companion object {
        private const val TAG = "FirebaseErrorHandler"
    }
    
    /**
     * Handle Firebase exceptions with appropriate user feedback
     */
    fun handleFirebaseError(
        exception: Exception,
        operation: String = "Firebase operation",
        onRetry: (() -> Unit)? = null,
        onFallback: (() -> Unit)? = null
    ): FirebaseErrorResult {
        
        return when (exception) {
            is FirebaseFirestoreException -> handleFirestoreError(exception, operation, onRetry, onFallback)
            is FirebaseAuthException -> handleAuthError(exception, operation, onRetry, onFallback)
            is StorageException -> handleStorageError(exception, operation, onRetry, onFallback)
            is SecurityException -> handleSecurityError(exception, operation, onFallback)
            is FirebaseException -> handleGenericFirebaseError(exception, operation, onRetry, onFallback)
            else -> handleUnknownError(exception, operation, onFallback)
        }
    }
    
    private fun handleFirestoreError(
        exception: FirebaseFirestoreException,
        operation: String,
        onRetry: (() -> Unit)?,
        onFallback: (() -> Unit)?
    ): FirebaseErrorResult {
        
        logger.e(TAG, "Firestore error during $operation", exception)
        
        return when (exception.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                FirebaseErrorResult.PermissionDenied(
                    userMessage = "Zugriff verweigert. Bitte melden Sie sich erneut an.",
                    technicalMessage = "Firestore permission denied: ${exception.message}",
                    canRetry = false,
                    onFallback = onFallback
                )
            }
            FirebaseFirestoreException.Code.UNAVAILABLE -> {
                FirebaseErrorResult.ServiceUnavailable(
                    userMessage = "Service vorübergehend nicht verfügbar. Versuchen Sie es später erneut.",
                    technicalMessage = "Firestore unavailable: ${exception.message}",
                    canRetry = true,
                    onRetry = onRetry,
                    onFallback = onFallback
                )
            }
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> {
                FirebaseErrorResult.Timeout(
                    userMessage = "Zeitüberschreitung. Überprüfen Sie Ihre Internetverbindung.",
                    technicalMessage = "Firestore timeout: ${exception.message}",
                    canRetry = true,
                    onRetry = onRetry,
                    onFallback = onFallback
                )
            }
            FirebaseFirestoreException.Code.FAILED_PRECONDITION -> {
                // This is likely the index error from logcat
                logger.w(TAG, "Firestore index missing - using fallback query")
                FirebaseErrorResult.IndexMissing(
                    userMessage = "Daten werden geladen...",
                    technicalMessage = "Firestore index missing: ${exception.message}",
                    canRetry = false,
                    onFallback = onFallback
                )
            }
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> {
                FirebaseErrorResult.QuotaExceeded(
                    userMessage = "Service vorübergehend überlastet. Versuchen Sie es später erneut.",
                    technicalMessage = "Firestore quota exceeded: ${exception.message}",
                    canRetry = true,
                    onRetry = onRetry,
                    onFallback = onFallback
                )
            }
            else -> {
                FirebaseErrorResult.FirestoreError(
                    userMessage = "Datenbankfehler. Versuchen Sie es erneut.",
                    technicalMessage = "Firestore error ${exception.code}: ${exception.message}",
                    canRetry = true,
                    onRetry = onRetry,
                    onFallback = onFallback
                )
            }
        }
    }
    
    private fun handleAuthError(
        exception: FirebaseAuthException,
        operation: String,
        onRetry: (() -> Unit)?,
        onFallback: (() -> Unit)?
    ): FirebaseErrorResult {
        
        logger.e(TAG, "Auth error during $operation", exception)
        
        return FirebaseErrorResult.AuthError(
            userMessage = "Authentifizierungsfehler. Bitte melden Sie sich erneut an.",
            technicalMessage = "Firebase Auth error: ${exception.message}",
            canRetry = false,
            onFallback = onFallback
        )
    }
    
    private fun handleStorageError(
        exception: StorageException,
        operation: String,
        onRetry: (() -> Unit)?,
        onFallback: (() -> Unit)?
    ): FirebaseErrorResult {
        
        logger.e(TAG, "Storage error during $operation", exception)
        
        return when (exception.errorCode) {
            StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> {
                FirebaseErrorResult.RetryLimitExceeded(
                    userMessage = "Upload fehlgeschlagen. Versuchen Sie es später erneut.",
                    technicalMessage = "Storage retry limit exceeded: ${exception.message}",
                    canRetry = true,
                    onRetry = onRetry,
                    onFallback = onFallback
                )
            }
            StorageException.ERROR_QUOTA_EXCEEDED -> {
                FirebaseErrorResult.QuotaExceeded(
                    userMessage = "Speicherplatz voll. Kontaktieren Sie den Support.",
                    technicalMessage = "Storage quota exceeded: ${exception.message}",
                    canRetry = false,
                    onFallback = onFallback
                )
            }
            else -> {
                FirebaseErrorResult.StorageError(
                    userMessage = "Upload fehlgeschlagen. Versuchen Sie es erneut.",
                    technicalMessage = "Storage error ${exception.errorCode}: ${exception.message}",
                    canRetry = true,
                    onRetry = onRetry,
                    onFallback = onFallback
                )
            }
        }
    }
    
    private fun handleSecurityError(
        exception: SecurityException,
        operation: String,
        onFallback: (() -> Unit)?
    ): FirebaseErrorResult {
        
        logger.e(TAG, "Security error during $operation - likely Google Play Services issue", exception)
        
        return FirebaseErrorResult.SecurityError(
            userMessage = "Sicherheitsfehler. App funktioniert möglicherweise eingeschränkt.",
            technicalMessage = "Security error (likely GPS): ${exception.message}",
            canRetry = false,
            onFallback = onFallback
        )
    }
    
    private fun handleGenericFirebaseError(
        exception: FirebaseException,
        operation: String,
        onRetry: (() -> Unit)?,
        onFallback: (() -> Unit)?
    ): FirebaseErrorResult {
        
        logger.e(TAG, "Generic Firebase error during $operation", exception)
        
        return FirebaseErrorResult.GenericFirebaseError(
            userMessage = "Firebase-Fehler. Versuchen Sie es erneut.",
            technicalMessage = "Firebase error: ${exception.message}",
            canRetry = true,
            onRetry = onRetry,
            onFallback = onFallback
        )
    }
    
    private fun handleUnknownError(
        exception: Exception,
        operation: String,
        onFallback: (() -> Unit)?
    ): FirebaseErrorResult {
        
        logger.e(TAG, "Unknown error during $operation", exception)
        
        return FirebaseErrorResult.UnknownError(
            userMessage = "Unbekannter Fehler. Versuchen Sie einen Neustart.",
            technicalMessage = "Unknown error: ${exception.message}",
            canRetry = false,
            onFallback = onFallback
        )
    }
}

/**
 * Firebase error result with user feedback and recovery options
 */
sealed class FirebaseErrorResult(
    open val userMessage: String,
    open val technicalMessage: String,
    open val canRetry: Boolean,
    open val onRetry: (() -> Unit)? = null,
    open val onFallback: (() -> Unit)? = null
) {
    data class PermissionDenied(
        override val userMessage: String,
        override val technicalMessage: String,
        override val canRetry: Boolean,
        override val onFallback: (() -> Unit)? = null
    ) : FirebaseErrorResult(userMessage, technicalMessage, canRetry, null, onFallback)
    
    data class ServiceUnavailable(
        override val userMessage: String,
        override val technicalMessage: String,
        override val canRetry: Boolean,
        override val onRetry: (() -> Unit)? = null,
        override val onFallback: (() -> Unit)? = null
    ) : FirebaseErrorResult(userMessage, technicalMessage, canRetry, onRetry, onFallback)
    
    data class Timeout(
        override val userMessage: String,
        override val technicalMessage: String,
        override val canRetry: Boolean,
        override val onRetry: (() -> Unit)? = null,
        override val onFallback: (() -> Unit)? = null
    ) : FirebaseErrorResult(userMessage, technicalMessage, canRetry, onRetry, onFallback)
    
    data class IndexMissing(
        override val userMessage: String,
        override val technicalMessage: String,
        override val canRetry: Boolean,
        override val onFallback: (() -> Unit)? = null
    ) : FirebaseErrorResult(userMessage, technicalMessage, canRetry, null, onFallback)
    
    data class QuotaExceeded(
        override val userMessage: String,
        override val technicalMessage: String,
        override val canRetry: Boolean,
        override val onRetry: (() -> Unit)? = null,
        override val onFallback: (() -> Unit)? = null
    ) : FirebaseErrorResult(userMessage, technicalMessage, canRetry, onRetry, onFallback)
    
    data class SecurityError(
        override val userMessage: String,
        override val technicalMessage: String,
        override val canRetry: Boolean,
        override val onFallback: (() -> Unit)? = null
    ) : FirebaseErrorResult(userMessage, technicalMessage, canRetry, null, onFallback)
    
    data class AuthError(
        override val userMessage: String,
        override val technicalMessage: String,
        override val canRetry: Boolean,
        override val onFallback: (() -> Unit)? = null
    ) : FirebaseErrorResult(userMessage, technicalMessage, canRetry, null, onFallback)
    
    data class FirestoreError(
        override val userMessage: String,
        override val technicalMessage: String,
        override val canRetry: Boolean,
        override val onRetry: (() -> Unit)? = null,
        override val onFallback: (() -> Unit)? = null
    ) : FirebaseErrorResult(userMessage, technicalMessage, canRetry, onRetry, onFallback)
    
    data class StorageError(
        override val userMessage: String,
        override val technicalMessage: String,
        override val canRetry: Boolean,
        override val onRetry: (() -> Unit)? = null,
        override val onFallback: (() -> Unit)? = null
    ) : FirebaseErrorResult(userMessage, technicalMessage, canRetry, onRetry, onFallback)
    
    data class RetryLimitExceeded(
        override val userMessage: String,
        override val technicalMessage: String,
        override val canRetry: Boolean,
        override val onRetry: (() -> Unit)? = null,
        override val onFallback: (() -> Unit)? = null
    ) : FirebaseErrorResult(userMessage, technicalMessage, canRetry, onRetry, onFallback)
    
    data class GenericFirebaseError(
        override val userMessage: String,
        override val technicalMessage: String,
        override val canRetry: Boolean,
        override val onRetry: (() -> Unit)? = null,
        override val onFallback: (() -> Unit)? = null
    ) : FirebaseErrorResult(userMessage, technicalMessage, canRetry, onRetry, onFallback)
    
    data class UnknownError(
        override val userMessage: String,
        override val technicalMessage: String,
        override val canRetry: Boolean,
        override val onFallback: (() -> Unit)? = null
    ) : FirebaseErrorResult(userMessage, technicalMessage, canRetry, null, onFallback)
}
