package com.omerkaya.sperrmuellfinder.data.messaging

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.data.manager.FirebaseManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationTokenHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseManager: FirebaseManager,
    private val logger: Logger
) {

    private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Initialize FCM and request permissions
     */
    suspend fun initialize(): Boolean {
        return try {
            // Check notification permission for Android 13+
            if (!hasNotificationPermission()) {
                logger.w(Logger.TAG_FIREBASE, "Notification permission not granted")
                return false
            }

            // Get FCM token
            val token = getToken()
            if (token != null) {
                saveTokenToFirestore(token)
                logger.i(Logger.TAG_FIREBASE, "FCM initialized successfully with token")
                true
            } else {
                logger.e(Logger.TAG_FIREBASE, "Failed to get FCM token")
                false
            }
        } catch (e: Exception) {
            logger.e(Logger.TAG_FIREBASE, "Failed to initialize FCM", e)
            false
        }
    }

    /**
     * Get current FCM token
     */
    suspend fun getToken(): String? {
        return try {
            val token = messaging.token.await()
            logger.d(Logger.TAG_FIREBASE, "FCM token retrieved")
            token
        } catch (e: Exception) {
            logger.e(Logger.TAG_FIREBASE, "Failed to get FCM token", e)
            null
        }
    }

    /**
     * Update token when it changes
     */
    fun updateToken(newToken: String) {
        scope.launch {
            try {
                saveTokenToFirestore(newToken)
                logger.i(Logger.TAG_FIREBASE, "FCM token updated successfully")
            } catch (e: Exception) {
                logger.e(Logger.TAG_FIREBASE, "Failed to update FCM token", e)
            }
        }
    }

    /**
     * Save token to Firestore for current user
     */
    private suspend fun saveTokenToFirestore(token: String) {
        try {
            val userId = firebaseManager.getCurrentUserId()
            if (userId == null) {
                logger.w(Logger.TAG_FIREBASE, "No authenticated user to save token for")
                return
            }

            val tokenData = mapOf(
                "token" to token,
                "platform" to "android",
                "granted" to hasNotificationPermission(),
                "appVersion" to (context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"),
                "created_at" to com.google.firebase.Timestamp.now(),
                "updated_at" to com.google.firebase.Timestamp.now()
            )

            // Save to user's device_tokens subcollection
            firebaseManager.firestore
                .collection(FirebaseManager.COLLECTION_DEVICE_TOKENS)
                .document(userId)
                .collection("tokens")
                .document(token)
                .set(tokenData)
                .await()

            // Primary store: users_private/{uid}.fcmTokens.{token}=true
            firebaseManager.firestore
                .collection(FirebaseManager.COLLECTION_USERS_PRIVATE)
                .document(userId)
                .set(
                    mapOf(
                        "fcmTokens" to mapOf(token to true),
                        "updated_at" to com.google.firebase.Timestamp.now()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                .await()

            // Backward-compatible mirror for existing clients/functions.
            firebaseManager.firestore
                .collection(FirebaseManager.COLLECTION_USERS)
                .document(userId)
                .set(
                    mapOf(
                        "device_tokens" to com.google.firebase.firestore.FieldValue.arrayUnion(token),
                        "updated_at" to com.google.firebase.Timestamp.now()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                .await()

            logger.d(Logger.TAG_FIREBASE, "Token saved to Firestore successfully")

        } catch (e: Exception) {
            logger.e(Logger.TAG_FIREBASE, "Failed to save token to Firestore", e)
        }
    }

    /**
     * Remove token from Firestore (on logout)
     */
    suspend fun removeToken() {
        try {
            val token = getToken() ?: return
            val userId = firebaseManager.getCurrentUserId() ?: return

            // Remove from device_tokens collection
            firebaseManager.firestore
                .collection(FirebaseManager.COLLECTION_DEVICE_TOKENS)
                .document(userId)
                .collection("tokens")
                .document(token)
                .delete()
                .await()

            // Remove from users_private/{uid}.fcmTokens map
            firebaseManager.firestore
                .collection(FirebaseManager.COLLECTION_USERS_PRIVATE)
                .document(userId)
                .set(
                    mapOf(
                        "fcmTokens.$token" to com.google.firebase.firestore.FieldValue.delete(),
                        "updated_at" to com.google.firebase.Timestamp.now()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                .await()

            // Backward-compatible mirror: remove from legacy users/{uid}.device_tokens
            firebaseManager.firestore
                .collection(FirebaseManager.COLLECTION_USERS)
                .document(userId)
                .set(
                    mapOf(
                        "device_tokens" to com.google.firebase.firestore.FieldValue.arrayRemove(token),
                        "updated_at" to com.google.firebase.Timestamp.now()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                .await()

            logger.i(Logger.TAG_FIREBASE, "Token removed from Firestore successfully")

        } catch (e: Exception) {
            logger.e(Logger.TAG_FIREBASE, "Failed to remove token from Firestore", e)
        }
    }

    /**
     * Remove token for a specific user id.
     * Used in logout/delete flows before auth state is cleared.
     */
    suspend fun removeTokenForUser(userId: String) {
        try {
            val token = getToken() ?: return

            firebaseManager.firestore
                .collection(FirebaseManager.COLLECTION_DEVICE_TOKENS)
                .document(userId)
                .collection("tokens")
                .document(token)
                .delete()
                .await()

            firebaseManager.firestore
                .collection(FirebaseManager.COLLECTION_USERS_PRIVATE)
                .document(userId)
                .set(
                    mapOf(
                        "fcmTokens.$token" to com.google.firebase.firestore.FieldValue.delete(),
                        "updated_at" to com.google.firebase.Timestamp.now()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                .await()

            firebaseManager.firestore
                .collection(FirebaseManager.COLLECTION_USERS)
                .document(userId)
                .set(
                    mapOf(
                        "device_tokens" to com.google.firebase.firestore.FieldValue.arrayRemove(token),
                        "updated_at" to com.google.firebase.Timestamp.now()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                .await()
        } catch (e: Exception) {
            logger.e(Logger.TAG_FIREBASE, "Failed to remove token for user", e)
        }
    }

    /**
     * Subscribe to topic for premium users
     */
    suspend fun subscribeToTopic(topic: String): Boolean {
        return try {
            messaging.subscribeToTopic(topic).await()
            logger.i(Logger.TAG_FIREBASE, "Subscribed to topic: $topic")
            true
        } catch (e: Exception) {
            logger.e(Logger.TAG_FIREBASE, "Failed to subscribe to topic: $topic", e)
            false
        }
    }

    /**
     * Unsubscribe from topic
     */
    suspend fun unsubscribeFromTopic(topic: String): Boolean {
        return try {
            messaging.unsubscribeFromTopic(topic).await()
            logger.i(Logger.TAG_FIREBASE, "Unsubscribed from topic: $topic")
            true
        } catch (e: Exception) {
            logger.e(Logger.TAG_FIREBASE, "Failed to unsubscribe from topic: $topic", e)
            false
        }
    }

    /**
     * Subscribe to premium topics
     */
    suspend fun subscribeToPremiumTopics() {
        subscribeToTopic(TOPIC_PREMIUM_USERS)
        subscribeToTopic(TOPIC_EARLY_ACCESS)
    }

    /**
     * Unsubscribe from premium topics
     */
    suspend fun unsubscribeFromPremiumTopics() {
        unsubscribeFromTopic(TOPIC_PREMIUM_USERS)
        unsubscribeFromTopic(TOPIC_EARLY_ACCESS)
    }

    /**
     * Subscribe to city-specific notifications
     */
    suspend fun subscribeToCityTopic(city: String) {
        val cityTopic = "${TOPIC_CITY_PREFIX}${city.lowercase().replace(" ", "_")}"
        subscribeToTopic(cityTopic)
    }

    /**
     * Check if notification permission is granted
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission not required for older versions
        }
    }

    /**
     * Delete FCM token (on account deletion)
     */
    suspend fun deleteToken(): Boolean {
        return try {
            messaging.deleteToken().await()
            logger.i(Logger.TAG_FIREBASE, "FCM token deleted successfully")
            true
        } catch (e: Exception) {
            logger.e(Logger.TAG_FIREBASE, "Failed to delete FCM token", e)
            false
        }
    }

    /**
     * Unsubscribe from all topics (for cleanup)
     */
    suspend fun unsubscribeFromAllTopics() {
        try {
            // Unsubscribe from known topics
            unsubscribeFromTopic(TOPIC_ALL_USERS)
            unsubscribeFromTopic(TOPIC_PREMIUM_USERS)
            unsubscribeFromTopic(TOPIC_EARLY_ACCESS)
            unsubscribeFromTopic(TOPIC_MAINTENANCE)
            unsubscribeFromTopic(TOPIC_UPDATES)
            
            logger.i(Logger.TAG_FIREBASE, "Unsubscribed from all known topics")
        } catch (e: Exception) {
            logger.e(Logger.TAG_FIREBASE, "Failed to unsubscribe from all topics", e)
        }
    }

    companion object {
        // FCM topics
        const val TOPIC_ALL_USERS = "all_users"
        const val TOPIC_PREMIUM_USERS = "premium_users"
        const val TOPIC_EARLY_ACCESS = "early_access"
        const val TOPIC_CITY_PREFIX = "city_"
        const val TOPIC_MAINTENANCE = "maintenance"
        const val TOPIC_UPDATES = "app_updates"

        // Permission request code
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }
}
