package com.omerkaya.sperrmuellfinder.data.manager

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
// BuildConfig is not available in data module, we'll use a different approach
import com.omerkaya.sperrmuellfinder.core.util.Logger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsManager @Inject constructor(
    private val logger: Logger
) {

    private val analytics: FirebaseAnalytics by lazy { Firebase.analytics }
    
    // Analytics configuration - will be controlled via Remote Config
    private var isAnalyticsEnabled: Boolean = true

    /**
     * Initialize Analytics
     */
    fun initialize() {
        try {
            // Enable/disable analytics based on configuration and user consent
            analytics.setAnalyticsCollectionEnabled(isAnalyticsEnabled)
            
            logger.i(Logger.TAG_FIREBASE, "Analytics initialized. Enabled: $isAnalyticsEnabled")
        } catch (e: Exception) {
            logger.e(Logger.TAG_FIREBASE, "Failed to initialize Analytics", e)
        }
    }
    
    /**
     * Update analytics configuration (called from RemoteConfigManager)
     */
    fun updateConfiguration(enabled: Boolean) {
        isAnalyticsEnabled = enabled
        try {
            analytics.setAnalyticsCollectionEnabled(enabled)
            logger.i(Logger.TAG_FIREBASE, "Analytics configuration updated. Enabled: $enabled")
        } catch (e: Exception) {
            logger.e(Logger.TAG_FIREBASE, "Failed to update analytics configuration", e)
        }
    }

    /**
     * Set user properties for analytics
     */
    fun setUserProperties(userId: String?, isPremium: Boolean, level: Int, city: String?) {
        try {
            analytics.apply {
                setUserId(userId)
                setUserProperty(USER_PROPERTY_PREMIUM, if (isPremium) "true" else "false")
                setUserProperty(USER_PROPERTY_LEVEL, level.toString())
                setUserProperty(USER_PROPERTY_CITY, city)
            }
            
            logger.i(Logger.TAG_FIREBASE, "User properties set for analytics")
        } catch (e: Exception) {
            logger.e(Logger.TAG_FIREBASE, "Failed to set user properties", e)
        }
    }

    /**
     * Log authentication events
     */
    fun logLogin(method: String) {
        logEvent(EVENT_LOGIN, Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, method)
        })
    }

    fun logSignUp(method: String) {
        logEvent(EVENT_SIGN_UP, Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, method)
        })
    }

    /**
     * Log post-related events
     */
    fun logPostCreated(category: String, hasImages: Boolean, imageCount: Int) {
        logEvent(EVENT_POST_CREATED, Bundle().apply {
            putString(PARAM_CATEGORY, category)
            putBoolean(PARAM_HAS_IMAGES, hasImages)
            putInt(PARAM_IMAGE_COUNT, imageCount)
        })
    }

    fun logPostViewed(postId: String, ownerId: String, category: String) {
        logEvent(EVENT_POST_VIEWED, Bundle().apply {
            putString(PARAM_POST_ID, postId)
            putString(PARAM_OWNER_ID, ownerId)
            putString(PARAM_CATEGORY, category)
        })
    }

    fun logPostLiked(postId: String, ownerId: String) {
        logEvent(EVENT_POST_LIKED, Bundle().apply {
            putString(PARAM_POST_ID, postId)
            putString(PARAM_OWNER_ID, ownerId)
        })
    }

    fun logPostShared(postId: String, method: String) {
        logEvent(EVENT_POST_SHARED, Bundle().apply {
            putString(PARAM_POST_ID, postId)
            putString(FirebaseAnalytics.Param.METHOD, method)
        })
    }

    /**
     * Log premium and purchase events
     */
    fun logPurchaseSuccess(productId: String, productType: String, value: Double, currency: String) {
        logEvent(EVENT_PURCHASE_SUCCESS, Bundle().apply {
            putString(PARAM_PRODUCT_ID, productId)
            putString(PARAM_PRODUCT_TYPE, productType)
            putDouble(FirebaseAnalytics.Param.VALUE, value)
            putString(FirebaseAnalytics.Param.CURRENCY, currency)
        })
    }

    fun logPremiumActivated(period: String, source: String) {
        logEvent(EVENT_PREMIUM_ACTIVATED, Bundle().apply {
            putString(PARAM_PREMIUM_PERIOD, period)
            putString(PARAM_SOURCE, source)
        })
    }

    fun logPurchaseRestore(success: Boolean, itemCount: Int) {
        logEvent(EVENT_PURCHASE_RESTORE, Bundle().apply {
            putBoolean(PARAM_SUCCESS, success)
            putInt(PARAM_ITEM_COUNT, itemCount)
        })
    }

    /**
     * Log XP and level events
     */
    fun logXpEarned(amount: Int, reason: String, level: Int) {
        logEvent(EVENT_XP_EARNED, Bundle().apply {
            putInt(PARAM_XP_AMOUNT, amount)
            putString(PARAM_XP_REASON, reason)
            putInt(PARAM_LEVEL, level)
        })
    }

    fun logLevelUp(newLevel: Int, totalXp: Int) {
        logEvent(EVENT_LEVEL_UP, Bundle().apply {
            putInt(PARAM_NEW_LEVEL, newLevel)
            putInt(PARAM_TOTAL_XP, totalXp)
        })
    }

    /**
     * Log search and discovery events
     */
    fun logSearch(query: String, category: String?, resultCount: Int) {
        logEvent(EVENT_SEARCH, Bundle().apply {
            putString(FirebaseAnalytics.Param.SEARCH_TERM, query)
            putString(PARAM_CATEGORY, category)
            putInt(PARAM_RESULT_COUNT, resultCount)
        })
    }

    fun logMapUsage(zoomLevel: Float, markerCount: Int, filterUsed: Boolean) {
        logEvent(EVENT_MAP_USAGE, Bundle().apply {
            putFloat(PARAM_ZOOM_LEVEL, zoomLevel)
            putInt(PARAM_MARKER_COUNT, markerCount)
            putBoolean(PARAM_FILTER_USED, filterUsed)
        })
    }

    /**
     * Log social events
     */
    fun logFollowUser(targetUserId: String) {
        logEvent(EVENT_FOLLOW_USER, Bundle().apply {
            putString(PARAM_TARGET_USER_ID, targetUserId)
        })
    }

    fun logCommentPosted(postId: String, commentLength: Int) {
        logEvent(EVENT_COMMENT_POSTED, Bundle().apply {
            putString(PARAM_POST_ID, postId)
            putInt(PARAM_COMMENT_LENGTH, commentLength)
        })
    }

    /**
     * Log camera and ML Kit events
     */
    fun logCameraUsage(photoCount: Int, categoriesDetected: List<String>) {
        logEvent(EVENT_CAMERA_USAGE, Bundle().apply {
            putInt(PARAM_PHOTO_COUNT, photoCount)
            putString(PARAM_CATEGORIES_DETECTED, categoriesDetected.joinToString(","))
        })
    }

    fun logMlProcessing(faceCount: Int, textDetected: Boolean, objectCount: Int) {
        logEvent(EVENT_ML_PROCESSING, Bundle().apply {
            putInt(PARAM_FACE_COUNT, faceCount)
            putBoolean(PARAM_TEXT_DETECTED, textDetected)
            putInt(PARAM_OBJECT_COUNT, objectCount)
        })
    }

    /**
     * Log error and performance events
     */
    fun logError(errorType: String, errorMessage: String, screen: String) {
        logEvent(EVENT_ERROR, Bundle().apply {
            putString(PARAM_ERROR_TYPE, errorType)
            putString(PARAM_ERROR_MESSAGE, errorMessage)
            putString(PARAM_SCREEN, screen)
        })
    }

    fun logPerformance(operation: String, durationMs: Long, success: Boolean) {
        logEvent(EVENT_PERFORMANCE, Bundle().apply {
            putString(PARAM_OPERATION, operation)
            putLong(PARAM_DURATION_MS, durationMs)
            putBoolean(PARAM_SUCCESS, success)
        })
    }

    /**
     * Log app lifecycle events
     */
    fun logAppOpen(source: String) {
        logEvent(EVENT_APP_OPEN, Bundle().apply {
            putString(PARAM_SOURCE, source)
        })
    }

    fun logScreenView(screenName: String, previousScreen: String?) {
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(PARAM_PREVIOUS_SCREEN, previousScreen)
        })
    }

    /**
     * Generic event logging
     */
    private fun logEvent(eventName: String, parameters: Bundle) {
        try {
            if (isAnalyticsEnabled) {
                analytics.logEvent(eventName, parameters)
                logger.d(Logger.TAG_FIREBASE, "Analytics event logged: $eventName")
            }
        } catch (e: Exception) {
            logger.e(Logger.TAG_FIREBASE, "Failed to log analytics event: $eventName", e)
        }
    }

    companion object {
        // User properties
        private const val USER_PROPERTY_PREMIUM = "is_premium"
        private const val USER_PROPERTY_LEVEL = "user_level"
        private const val USER_PROPERTY_CITY = "user_city"

        // Custom events
        private const val EVENT_POST_CREATED = "post_created"
        private const val EVENT_POST_VIEWED = "post_viewed"
        private const val EVENT_POST_LIKED = "post_liked"
        private const val EVENT_POST_SHARED = "post_shared"
        private const val EVENT_PURCHASE_SUCCESS = "purchase_success"
        private const val EVENT_PREMIUM_ACTIVATED = "premium_activated"
        private const val EVENT_PURCHASE_RESTORE = "purchase_restore"
        private const val EVENT_XP_EARNED = "xp_earned"
        private const val EVENT_LEVEL_UP = "level_up"
        private const val EVENT_SEARCH = "search"
        private const val EVENT_MAP_USAGE = "map_usage"
        private const val EVENT_FOLLOW_USER = "follow_user"
        private const val EVENT_COMMENT_POSTED = "comment_posted"
        private const val EVENT_CAMERA_USAGE = "camera_usage"
        private const val EVENT_ML_PROCESSING = "ml_processing"
        private const val EVENT_ERROR = "app_error"
        private const val EVENT_PERFORMANCE = "app_performance"
        private const val EVENT_APP_OPEN = "app_open"

        // Standard Firebase events used
        private const val EVENT_LOGIN = FirebaseAnalytics.Event.LOGIN
        private const val EVENT_SIGN_UP = FirebaseAnalytics.Event.SIGN_UP

        // Custom parameters
        private const val PARAM_CATEGORY = "category"
        private const val PARAM_HAS_IMAGES = "has_images"
        private const val PARAM_IMAGE_COUNT = "image_count"
        private const val PARAM_POST_ID = "post_id"
        private const val PARAM_OWNER_ID = "owner_id"
        private const val PARAM_PRODUCT_ID = "product_id"
        private const val PARAM_PRODUCT_TYPE = "product_type"
        private const val PARAM_PREMIUM_PERIOD = "premium_period"
        private const val PARAM_SOURCE = "source"
        private const val PARAM_SUCCESS = "success"
        private const val PARAM_ITEM_COUNT = "item_count"
        private const val PARAM_XP_AMOUNT = "xp_amount"
        private const val PARAM_XP_REASON = "xp_reason"
        private const val PARAM_LEVEL = "level"
        private const val PARAM_NEW_LEVEL = "new_level"
        private const val PARAM_TOTAL_XP = "total_xp"
        private const val PARAM_RESULT_COUNT = "result_count"
        private const val PARAM_ZOOM_LEVEL = "zoom_level"
        private const val PARAM_MARKER_COUNT = "marker_count"
        private const val PARAM_FILTER_USED = "filter_used"
        private const val PARAM_TARGET_USER_ID = "target_user_id"
        private const val PARAM_COMMENT_LENGTH = "comment_length"
        private const val PARAM_PHOTO_COUNT = "photo_count"
        private const val PARAM_CATEGORIES_DETECTED = "categories_detected"
        private const val PARAM_FACE_COUNT = "face_count"
        private const val PARAM_TEXT_DETECTED = "text_detected"
        private const val PARAM_OBJECT_COUNT = "object_count"
        private const val PARAM_ERROR_TYPE = "error_type"
        private const val PARAM_ERROR_MESSAGE = "error_message"
        private const val PARAM_SCREEN = "screen"
        private const val PARAM_OPERATION = "operation"
        private const val PARAM_DURATION_MS = "duration_ms"
        private const val PARAM_PREVIOUS_SCREEN = "previous_screen"
    }
}
