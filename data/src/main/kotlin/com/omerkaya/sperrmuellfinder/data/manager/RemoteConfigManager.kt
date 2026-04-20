package com.omerkaya.sperrmuellfinder.data.manager

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
// BuildConfig is not available in data module, using debug detection alternative
import com.omerkaya.sperrmuellfinder.core.util.Logger
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigManager @Inject constructor(
    private val logger: Logger
) {

    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
    
    // Debug detection alternative to BuildConfig.DEBUG
    private val isDebugMode: Boolean by lazy {
        try {
            Class.forName("com.omerkaya.sperrmuellfinder.BuildConfig")
                .getDeclaredField("DEBUG")
                .getBoolean(null)
        } catch (e: Exception) {
            false // Default to production mode if detection fails
        }
    }
    
    // Analytics configuration callback
    private var analyticsConfigCallback: ((Boolean) -> Unit)? = null

    /**
     * Initialize Remote Config with default values
     */
    suspend fun initialize(): Boolean {
        return try {
            // Configure settings
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = if (isDebugMode) 0 else 3600 // 1 hour in production
            }
            remoteConfig.setConfigSettingsAsync(configSettings).await()

            // Set default values
            remoteConfig.setDefaultsAsync(getDefaultValues()).await()

            // Fetch and activate
            fetchAndActivate()

            logger.i(Logger.TAG_FIREBASE, "Remote Config initialized successfully")
            true
        } catch (e: Exception) {
            logger.e(Logger.TAG_FIREBASE, "Failed to initialize Remote Config", e)
            false
        }
    }

    /**
     * Set analytics configuration callback
     */
    fun setAnalyticsConfigCallback(callback: (Boolean) -> Unit) {
        analyticsConfigCallback = callback
        // Immediately call with current value
        callback(isPremiumEnabled())
    }
    
    /**
     * Fetch and activate remote config
     */
    suspend fun fetchAndActivate(): Boolean {
        return try {
            val result = remoteConfig.fetchAndActivate().await()
            logger.i(Logger.TAG_FIREBASE, "Remote Config fetch result: $result")
            
            // Notify analytics configuration update
            analyticsConfigCallback?.invoke(isPremiumEnabled())
            
            result
        } catch (e: Exception) {
            logger.e(Logger.TAG_FIREBASE, "Failed to fetch Remote Config", e)
            false
        }
    }

    /**
     * Get default values for Remote Config
     */
    private fun getDefaultValues(): Map<String, Any> {
        return mapOf(
            // Feature flags
            KEY_PREMIUM_ENABLED to true,
            KEY_ADS_ENABLED to true,
            KEY_PAYWALL_AB_TEST_BUCKET to "control",
            KEY_SHOW_XP_PACKS to true,

            // Basic user limits
            KEY_BASIC_RADIUS_METERS to 1500L,
            KEY_EARLY_ACCESS_MINUTES to 10L,
            KEY_AVAILABILITY_PENALTY_STEP to 20L,
            KEY_MAX_POST_IMAGES to 3L,
            KEY_POST_EXPIRE_HOURS to 72L,
            KEY_FREE_PREMIUM_EXTEND_HOURS to 6L,

            // XP rewards
            KEY_XP_SHARE_POST to 50L,
            KEY_XP_DAILY_LOGIN to 10L,
            KEY_XP_LIKE_RECEIVED to 15L,
            KEY_XP_COMMENT_RECEIVED to 20L,
            KEY_XP_COMMENT_WRITE to 15L,
            KEY_XP_SHARE_EXTERNAL to 25L,
            KEY_XP_PREMIUM_TASK to 500L,

            // XP boost multipliers
            KEY_XP_BOOST_LVL1_4 to 0.05,
            KEY_XP_BOOST_LVL5_9 to 0.07,
            KEY_XP_BOOST_LVL10_14 to 0.10,
            KEY_XP_BOOST_LVL15P to 0.20,

            // App configuration
            KEY_MIN_APP_VERSION to "1.0.0",
            KEY_FORCE_UPDATE_VERSION to "0.0.0",
            KEY_MAINTENANCE_MODE to false,
            KEY_MAINTENANCE_MESSAGE to "App is under maintenance. Please try again later.",

            // Content moderation
            KEY_MAX_DESCRIPTION_LENGTH to 500L,
            KEY_MAX_COMMENT_LENGTH to 300L,
            KEY_AUTO_MODERATION_ENABLED to true,
            KEY_SPAM_DETECTION_ENABLED to true,

            // Performance
            KEY_MAX_CACHE_SIZE_MB to 100L,
            KEY_IMAGE_COMPRESSION_QUALITY to 85L,
            KEY_MAX_IMAGE_SIZE_MB to 10L,

            // Map configuration
            KEY_MAP_BASIC_RADIUS_M to 1500L,
            KEY_MAP_PREMIUM_RADIUS_M to 20000L,
            KEY_MAP_CLUSTER_MIN_ZOOM to 10.0,
            KEY_MAP_CLUSTER_MAX_ZOOM to 16.0,
            KEY_MAP_AVAILABILITY_THRESHOLD to 0.6,
            KEY_MAP_PREMIUM_MARKER_ENABLED to true,
            KEY_MAP_EARLY_ACCESS_MINUTES to 10L,
            KEY_MAP_CACHE_MAX_SIZE_MB to 50L,
            KEY_MAP_MAX_POSTS_PER_QUERY to 500L
        )
    }

    // Getter methods for configuration values
    fun isPremiumEnabled(): Boolean = remoteConfig.getBoolean(KEY_PREMIUM_ENABLED)
    fun areAdsEnabled(): Boolean = remoteConfig.getBoolean(KEY_ADS_ENABLED)
    fun getPaywallAbTestBucket(): String = remoteConfig.getString(KEY_PAYWALL_AB_TEST_BUCKET)
    fun shouldShowXpPacks(): Boolean = remoteConfig.getBoolean(KEY_SHOW_XP_PACKS)

    fun getBasicRadiusMeters(): Long = remoteConfig.getLong(KEY_BASIC_RADIUS_METERS)
    fun getEarlyAccessMinutes(): Long = remoteConfig.getLong(KEY_EARLY_ACCESS_MINUTES)
    fun getAvailabilityPenaltyStep(): Long = remoteConfig.getLong(KEY_AVAILABILITY_PENALTY_STEP)
    fun getMaxPostImages(): Long = remoteConfig.getLong(KEY_MAX_POST_IMAGES)
    fun getPostExpireHours(): Long = remoteConfig.getLong(KEY_POST_EXPIRE_HOURS)
    fun getFreePremiumExtendHours(): Long = remoteConfig.getLong(KEY_FREE_PREMIUM_EXTEND_HOURS)

    fun getXpSharePost(): Long = remoteConfig.getLong(KEY_XP_SHARE_POST)
    fun getXpDailyLogin(): Long = remoteConfig.getLong(KEY_XP_DAILY_LOGIN)
    fun getXpLikeReceived(): Long = remoteConfig.getLong(KEY_XP_LIKE_RECEIVED)
    fun getXpCommentReceived(): Long = remoteConfig.getLong(KEY_XP_COMMENT_RECEIVED)
    fun getXpCommentWrite(): Long = remoteConfig.getLong(KEY_XP_COMMENT_WRITE)
    fun getXpShareExternal(): Long = remoteConfig.getLong(KEY_XP_SHARE_EXTERNAL)
    fun getXpPremiumTask(): Long = remoteConfig.getLong(KEY_XP_PREMIUM_TASK)

    fun getXpBoostLvl1To4(): Double = remoteConfig.getDouble(KEY_XP_BOOST_LVL1_4)
    fun getXpBoostLvl5To9(): Double = remoteConfig.getDouble(KEY_XP_BOOST_LVL5_9)
    fun getXpBoostLvl10To14(): Double = remoteConfig.getDouble(KEY_XP_BOOST_LVL10_14)
    fun getXpBoostLvl15Plus(): Double = remoteConfig.getDouble(KEY_XP_BOOST_LVL15P)

    fun getMinAppVersion(): String = remoteConfig.getString(KEY_MIN_APP_VERSION)
    fun getForceUpdateVersion(): String = remoteConfig.getString(KEY_FORCE_UPDATE_VERSION)
    fun isMaintenanceMode(): Boolean = remoteConfig.getBoolean(KEY_MAINTENANCE_MODE)
    fun getMaintenanceMessage(): String = remoteConfig.getString(KEY_MAINTENANCE_MESSAGE)

    fun getMaxDescriptionLength(): Long = remoteConfig.getLong(KEY_MAX_DESCRIPTION_LENGTH)
    fun getMaxCommentLength(): Long = remoteConfig.getLong(KEY_MAX_COMMENT_LENGTH)
    fun isAutoModerationEnabled(): Boolean = remoteConfig.getBoolean(KEY_AUTO_MODERATION_ENABLED)
    fun isSpamDetectionEnabled(): Boolean = remoteConfig.getBoolean(KEY_SPAM_DETECTION_ENABLED)

    fun getMaxCacheSizeMb(): Long = remoteConfig.getLong(KEY_MAX_CACHE_SIZE_MB)
    fun getImageCompressionQuality(): Long = remoteConfig.getLong(KEY_IMAGE_COMPRESSION_QUALITY)
    fun getMaxImageSizeMb(): Long = remoteConfig.getLong(KEY_MAX_IMAGE_SIZE_MB)

    // Map configuration
    fun getMapBasicRadiusMeters(): Long = remoteConfig.getLong(KEY_MAP_BASIC_RADIUS_M)
    fun getMapPremiumRadiusMeters(): Long = remoteConfig.getLong(KEY_MAP_PREMIUM_RADIUS_M)
    fun getMapClusterMinZoom(): Double = remoteConfig.getDouble(KEY_MAP_CLUSTER_MIN_ZOOM)
    fun getMapClusterMaxZoom(): Double = remoteConfig.getDouble(KEY_MAP_CLUSTER_MAX_ZOOM)
    fun getMapAvailabilityThreshold(): Double = remoteConfig.getDouble(KEY_MAP_AVAILABILITY_THRESHOLD)
    fun isMapPremiumMarkerEnabled(): Boolean = remoteConfig.getBoolean(KEY_MAP_PREMIUM_MARKER_ENABLED)
    fun getMapEarlyAccessMinutes(): Long = remoteConfig.getLong(KEY_MAP_EARLY_ACCESS_MINUTES)
    fun getMapCacheMaxSizeMB(): Long = remoteConfig.getLong(KEY_MAP_CACHE_MAX_SIZE_MB)
    fun getMapMaxPostsPerQuery(): Long = remoteConfig.getLong(KEY_MAP_MAX_POSTS_PER_QUERY)

    companion object {
        // Feature flags
        private const val KEY_PREMIUM_ENABLED = "feature_premium_enabled"
        private const val KEY_ADS_ENABLED = "ads_enabled"
        private const val KEY_PAYWALL_AB_TEST_BUCKET = "paywall_ab_test_bucket"
        private const val KEY_SHOW_XP_PACKS = "rc_show_xp_packs"

        // Basic limits
        private const val KEY_BASIC_RADIUS_METERS = "basic_radius_meters"
        private const val KEY_EARLY_ACCESS_MINUTES = "early_access_minutes"
        private const val KEY_AVAILABILITY_PENALTY_STEP = "availability_penalty_step"
        private const val KEY_MAX_POST_IMAGES = "max_post_images"
        private const val KEY_POST_EXPIRE_HOURS = "post_expire_hours"
        private const val KEY_FREE_PREMIUM_EXTEND_HOURS = "free_premium_extend_hours"

        // XP rewards
        private const val KEY_XP_SHARE_POST = "xp_share_post"
        private const val KEY_XP_DAILY_LOGIN = "xp_daily_login"
        private const val KEY_XP_LIKE_RECEIVED = "xp_like_received"
        private const val KEY_XP_COMMENT_RECEIVED = "xp_comment_received"
        private const val KEY_XP_COMMENT_WRITE = "xp_comment_write"
        private const val KEY_XP_SHARE_EXTERNAL = "xp_share_external"
        private const val KEY_XP_PREMIUM_TASK = "xp_premium_task"

        // XP boost multipliers
        private const val KEY_XP_BOOST_LVL1_4 = "xp_boost_lvl1_4"
        private const val KEY_XP_BOOST_LVL5_9 = "xp_boost_lvl5_9"
        private const val KEY_XP_BOOST_LVL10_14 = "xp_boost_lvl10_14"
        private const val KEY_XP_BOOST_LVL15P = "xp_boost_lvl15p"

        // App version control
        private const val KEY_MIN_APP_VERSION = "min_app_version"
        private const val KEY_FORCE_UPDATE_VERSION = "force_update_version"
        private const val KEY_MAINTENANCE_MODE = "maintenance_mode"
        private const val KEY_MAINTENANCE_MESSAGE = "maintenance_message"

        // Content moderation
        private const val KEY_MAX_DESCRIPTION_LENGTH = "max_description_length"
        private const val KEY_MAX_COMMENT_LENGTH = "max_comment_length"
        private const val KEY_AUTO_MODERATION_ENABLED = "auto_moderation_enabled"
        private const val KEY_SPAM_DETECTION_ENABLED = "spam_detection_enabled"

        // Performance
        private const val KEY_MAX_CACHE_SIZE_MB = "max_cache_size_mb"
        private const val KEY_IMAGE_COMPRESSION_QUALITY = "image_compression_quality"
        private const val KEY_MAX_IMAGE_SIZE_MB = "max_image_size_mb"

        // Map configuration
        private const val KEY_MAP_BASIC_RADIUS_M = "map_basic_radius_m"
        private const val KEY_MAP_PREMIUM_RADIUS_M = "map_premium_radius_m"
        private const val KEY_MAP_CLUSTER_MIN_ZOOM = "map_cluster_min_zoom"
        private const val KEY_MAP_CLUSTER_MAX_ZOOM = "map_cluster_max_zoom"
        private const val KEY_MAP_AVAILABILITY_THRESHOLD = "map_availability_threshold"
        private const val KEY_MAP_PREMIUM_MARKER_ENABLED = "map_premium_marker_enabled"
        private const val KEY_MAP_EARLY_ACCESS_MINUTES = "map_early_access_minutes"
        private const val KEY_MAP_CACHE_MAX_SIZE_MB = "map_cache_max_size_mb"
        private const val KEY_MAP_MAX_POSTS_PER_QUERY = "map_max_posts_per_query"
    }
}
