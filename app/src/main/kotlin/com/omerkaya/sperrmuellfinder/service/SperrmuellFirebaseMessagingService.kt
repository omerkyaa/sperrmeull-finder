package com.omerkaya.sperrmuellfinder.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.data.messaging.NotificationTokenHelper
import com.omerkaya.sperrmuellfinder.domain.repository.SettingsRepository
import com.omerkaya.sperrmuellfinder.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Firebase Cloud Messaging service for SperrmüllFinder.
 * Handles incoming push notifications and token refresh.
 * Shows notifications with app logo and proper deep linking.
 * Rules.md compliant - Professional FCM implementation.
 */
@AndroidEntryPoint
class SperrmuellFirebaseMessagingService : FirebaseMessagingService() {
    
    @Inject
    lateinit var notificationTokenHelper: NotificationTokenHelper

    @Inject
    lateinit var settingsRepository: SettingsRepository
    
    companion object {
        private const val TAG = "SperrmuellFCM"
        const val CHANNEL_ID = "sperrmuell_notifications"
        const val CHANNEL_NAME = "SperrmüllFinder Benachrichtigungen"
        private const val NOTIFICATION_ID_BASE = 1000
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        if (!areInAppNotificationsEnabled()) {
            Log.d(TAG, "Push received but notifications are disabled in settings. Skipping display.")
            return
        }
        
        Log.d(TAG, "FCM message received from: ${remoteMessage.from}")
        
        // Extract notification data
        val title = remoteMessage.notification?.title 
            ?: remoteMessage.data["title"] 
            ?: getString(R.string.app_name)
            
        val body = remoteMessage.notification?.body 
            ?: remoteMessage.data["body"] 
            ?: ""
            
        val type = remoteMessage.data["type"] ?: "general"
        val postId = remoteMessage.data["postId"]
        val userId = remoteMessage.data["fromUserId"] ?: remoteMessage.data["userId"]
        
        Log.d(TAG, "Notification - Title: $title, Body: $body, Type: $type")
        
        // Show local notification
        showNotification(title, body, type, postId, userId)
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed: ${token.take(20)}...")

        if (areInAppNotificationsEnabled()) {
            // Update token in Firestore only when notifications are enabled.
            notificationTokenHelper.updateToken(token)
        } else {
            Log.d(TAG, "Token refresh ignored because notifications are disabled.")
        }
    }
    
    /**
     * Create notification channel for Android 8.0+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Benachrichtigungen für neue Likes, Kommentare und Aktivitäten"
                enableVibration(true)
                enableLights(true)
                lightColor = 0xFF9C27B0.toInt() // Purple color
                setShowBadge(true)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            
            Log.d(TAG, "Notification channel created: $CHANNEL_ID")
        }
    }
    
    /**
     * Show local notification with app logo and deep linking.
     */
    private fun showNotification(
        title: String,
        body: String,
        type: String,
        postId: String?,
        userId: String?
    ) {
        try {
            // Create deep link intent
            val intent = createDeepLinkIntent(type, postId, userId)
            val pendingIntent = PendingIntent.getActivity(
                this,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Build notification
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_app_logo) // App logo (white silhouette)
                .setLargeIcon(
                    androidx.core.content.ContextCompat.getDrawable(this, R.drawable.app_logo)?.let { drawable ->
                        androidx.core.graphics.drawable.DrawableCompat.wrap(drawable).toBitmap()
                    }
                ) // App logo (full color)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setColor(0xFF9C27B0.toInt()) // Purple color
                .build()
            
            // Show notification with proper permission handling
            val notificationId = NOTIFICATION_ID_BASE + System.currentTimeMillis().toInt()
            
            // Explicit permission check before notify()
            if (hasNotificationPermission()) {
                try {
                    // Double-check permission right before notify to satisfy Lint
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            NotificationManagerCompat.from(this).notify(notificationId, notification)
                            Log.d(TAG, "Notification shown with ID: $notificationId")
                        } else {
                            Log.w(TAG, "POST_NOTIFICATIONS permission not granted")
                        }
                    } else {
                        // Android 12 and below - no permission needed
                        NotificationManagerCompat.from(this).notify(notificationId, notification)
                        Log.d(TAG, "Notification shown with ID: $notificationId")
                    }
                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException when showing notification - permission denied", e)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to show notification", e)
                }
            } else {
                Log.w(TAG, "Notification permission not available, notification not shown")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show notification", e)
        }
    }
    
    /**
     * Check if notification permission is granted.
     * For Android 13+ (API 33+), POST_NOTIFICATIONS permission is required.
     * For Android 12 and below, notifications are always allowed.
     */
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // For Android 12 and below, no permission check needed
            true
        }
    }
    
    /**
     * Create deep link intent based on notification type.
     */
    private fun createDeepLinkIntent(
        type: String,
        postId: String?,
        userId: String?
    ): Intent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val deepLink = remoteMessageDeepLink(type, postId, userId)
        if (!deepLink.isNullOrBlank()) {
            intent.putExtra("deepLink", deepLink)
        }
        
        // Add deep link data based on notification type
        when (type) {
            "like", "comment", "premium_nearby_post" -> {
                if (postId != null) {
                    intent.putExtra("deep_link_type", type)
                    intent.putExtra("post_id", postId)
                }
            }
            "follow" -> {
                if (userId != null) {
                    intent.putExtra("deep_link_type", "user_profile")
                    intent.putExtra("user_id", userId)
                }
            }
            "xp", "premium", "honesty" -> {
                intent.putExtra("deep_link_type", "profile")
            }
            else -> {
                intent.putExtra("deep_link_type", "notifications")
            }
        }
        
        return intent
    }

    private fun remoteMessageDeepLink(type: String, postId: String?, userId: String?): String? {
        return when (type) {
            "like", "comment", "premium_nearby_post" -> postId?.let { "post:$it" }
            "follow" -> userId?.let { "profile:$it" }
            else -> "notifications"
        }
    }

    private fun areInAppNotificationsEnabled(): Boolean {
        return try {
            runBlocking {
                settingsRepository.getUserPreferences()
                    .first()
                    .notificationSettings
                    .pushNotificationsEnabled
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not read notification settings, defaulting to enabled.", e)
            true
        }
    }
    
}
