package com.omerkaya.sperrmuellfinder.core.util

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for managing FCM tokens and Firestore synchronization.
 * Handles token registration, refresh, and multi-device support.
 * Rules.md compliant - Professional FCM token management.
 */
@Singleton
class NotificationTokenHelper @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    
    companion object {
        private const val TAG = "NotificationTokenHelper"
        private const val USERS_COLLECTION = "users"
        private const val DEVICE_TOKENS_COLLECTION = "device_tokens"
    }
    
    /**
     * Register current FCM token for the authenticated user.
     * Called on login and token refresh.
     */
    suspend fun registerCurrentToken() {
        try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.w(TAG, "No authenticated user, skipping token registration")
                return
            }
            
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d(TAG, "FCM Token obtained: ${token.take(20)}...")
            
            saveTokenToFirestore(currentUser.uid, token)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register FCM token", e)
        }
    }
    
    /**
     * Handle FCM token refresh.
     * Called from FirebaseMessagingService.onNewToken().
     */
    suspend fun onTokenRefresh(newToken: String) {
        try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.w(TAG, "No authenticated user during token refresh")
                return
            }
            
            Log.d(TAG, "FCM Token refreshed: ${newToken.take(20)}...")
            saveTokenToFirestore(currentUser.uid, newToken)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle token refresh", e)
        }
    }
    
    /**
     * Save FCM token to Firestore under user's device tokens.
     * Supports multiple devices per user.
     */
    private suspend fun saveTokenToFirestore(userId: String, token: String) {
        try {
            val deviceInfo = mapOf(
                "token" to token,
                "platform" to "android",
                "granted" to true,
                "created_at" to Date(),
                "updated_at" to Date()
            )
            
            // Save to users/{uid}/device_tokens/{token_hash}
            val tokenHash = token.hashCode().toString()
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(DEVICE_TOKENS_COLLECTION)
                .document(tokenHash)
                .set(deviceInfo)
                .await()
            
            Log.d(TAG, "FCM token saved to Firestore for user: $userId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save token to Firestore", e)
        }
    }
    
    /**
     * Remove FCM token from Firestore.
     * Called on logout or token invalidation.
     */
    suspend fun removeCurrentToken() {
        try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.w(TAG, "No authenticated user, skipping token removal")
                return
            }
            
            val token = FirebaseMessaging.getInstance().token.await()
            val tokenHash = token.hashCode().toString()
            
            firestore.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .collection(DEVICE_TOKENS_COLLECTION)
                .document(tokenHash)
                .delete()
                .await()
            
            Log.d(TAG, "FCM token removed from Firestore")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove token from Firestore", e)
        }
    }
    
    /**
     * Clean up old/invalid tokens for a user.
     * Called periodically to maintain token hygiene.
     */
    suspend fun cleanupOldTokens(userId: String) {
        try {
            val thirtyDaysAgo = Date(System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000))
            
            val oldTokens = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(DEVICE_TOKENS_COLLECTION)
                .whereLessThan("updated_at", thirtyDaysAgo)
                .get()
                .await()
            
            oldTokens.documents.forEach { document ->
                document.reference.delete().await()
            }
            
            Log.d(TAG, "Cleaned up ${oldTokens.size()} old tokens for user: $userId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old tokens", e)
        }
    }
    
    /**
     * Check if notifications are enabled for the current user.
     */
    suspend fun areNotificationsEnabled(userId: String): Boolean {
        return try {
            val tokens = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(DEVICE_TOKENS_COLLECTION)
                .whereEqualTo("granted", true)
                .get()
                .await()
            
            tokens.documents.isNotEmpty()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check notification status", e)
            false
        }
    }
}
