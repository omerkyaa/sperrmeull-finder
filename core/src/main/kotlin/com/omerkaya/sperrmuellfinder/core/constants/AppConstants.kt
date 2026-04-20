package com.omerkaya.sperrmuellfinder.core.constants

/**
 * 🔥 APP CONSTANTS - SperrmüllFinder
 * Rules.md compliant - Application-wide constants for XP, Honesty, Premium, and Configuration
 * 
 * Features:
 * - XP System: Rewards, Level calculation, Premium boosts
 * - Honesty System: Scoring, Penalties, Thresholds
 * - Premium Configuration: Limits, Features, Gating
 * - Remote Config: Default values and keys
 * - UI Configuration: Timeouts, Limits, Animations
 */
object AppConstants {
    
    // ========================================
    // XP SYSTEM CONSTANTS
    // ========================================
    
    // XP Rewards (from rules.md)
    const val XP_SHARE_POST = 50
    const val XP_DAILY_LOGIN = 10
    const val XP_LIKE_RECEIVED = 15
    const val XP_COMMENT_RECEIVED = 20
    const val XP_COMMENT_WRITE = 15
    const val XP_SHARE_EXTERNAL = 25
    const val XP_PREMIUM_TASK = 500
    
    // Leaderboard XP Rewards
    const val XP_LEADERBOARD_FIRST = 500
    const val XP_LEADERBOARD_SECOND = 250
    const val XP_LEADERBOARD_THIRD = 150
    
    // Level Calculation (Cumulative Formula: 50 + (level × level × 100))
    const val XP_BASE_REQUIREMENT = 50
    const val XP_LEVEL_MULTIPLIER = 100
    const val XP_INITIAL_GRANT = 100  // New users start with 100 XP (Level 1)
    
    // Premium XP Boost Percentages (from rules.md)
    const val XP_BOOST_LV1_4 = 0.05f     // 5% boost for levels 1-4
    const val XP_BOOST_LV5_9 = 0.07f     // 7% boost for levels 5-9
    const val XP_BOOST_LV10_14 = 0.10f   // 10% boost for levels 10-14
    const val XP_BOOST_LV15_PLUS = 0.20f // 20% boost for level 15+
    
    // ========================================
    // HONESTY SYSTEM CONSTANTS
    // ========================================
    
    // Honesty Starting Value
    const val HONESTY_INITIAL_VALUE = 100
    
    // Honesty Rewards & Penalties
    const val HONESTY_POST_72H_NO_COMPLAINTS = 5
    const val HONESTY_APPROVED_COMPLAINT_PENALTY = -20
    const val HONESTY_AVAILABILITY_CORRECT_VOTE = 2
    const val HONESTY_AVAILABILITY_WRONG_VOTE = -3
    
    // Honesty Thresholds
    const val HONESTY_POSTING_BAN_THRESHOLD = 30  // Below this = posting banned
    
    // Admin Ban Durations (in hours)
    const val BAN_DURATION_3_DAYS = 72
    const val BAN_DURATION_1_HOUR = 1
    const val BAN_DURATION_1_DAY = 24
    const val BAN_DURATION_3_DAYS_HOURS = 72
    const val BAN_DURATION_PERMANENT = -1
    
    // ========================================
    // PREMIUM SYSTEM CONSTANTS
    // ========================================
    
    // Basic User Limitations
    const val BASIC_RADIUS_METERS = 1500
    const val PREMIUM_UNLIMITED_RADIUS = -1
    
    // Premium Features
    const val EARLY_ACCESS_MINUTES = 10
    const val FREE_PREMIUM_EXTEND_HOURS = 6
    
    // Post Configuration
    const val POST_EXPIRE_HOURS = 72
    const val MAX_POST_IMAGES = 3
    
    // Availability System
    const val AVAILABILITY_PENALTY_STEP = 20  // "Taken" vote reduces availability by 20%
    const val AVAILABILITY_INITIAL_PERCENT = 100
    
    // ========================================
    // MONETIZATION CONSTANTS
    // ========================================
    
    // Premium Pricing (Euro)
    const val PREMIUM_MONTHLY_PRICE = 2.99f
    const val PREMIUM_PLUS_MONTHLY_PRICE = 3.99f
    
    // XP Pack Pricing
    const val XP_PACK_500_PRICE = 0.99f
    const val XP_PACK_1500_PRICE = 1.49f
    
    // Post Extension Pricing
    const val POST_EXTEND_6H_PRICE = 0.99f
    
    // XP Pack Values
    const val XP_PACK_500_VALUE = 500
    const val XP_PACK_1500_VALUE = 1500
    
    // Premium XP Bonuses
    const val PREMIUM_MONTHLY_XP_BONUS = 800
    const val PREMIUM_PLUS_XP_BONUS = 1500
    
    // ========================================
    // SEARCH CONFIGURATION
    // ========================================
    
    const val SEARCH_PAGE_SIZE = 20
    const val SEARCH_DEBOUNCE_MS = 500L
    const val SEARCH_MAX_HOURS_BACK = 72
    
    // ========================================
    // UI CONFIGURATION
    // ========================================
    
    // Animation Durations
    const val ANIMATION_DURATION_SHORT = 200L
    const val ANIMATION_DURATION_MEDIUM = 400L
    const val ANIMATION_DURATION_LONG = 600L
    
    // Welcome Banner
    const val WELCOME_BANNER_DISPLAY_SECONDS = 5
    
    // Timeouts
    const val NETWORK_TIMEOUT_SECONDS = 30L
    const val IMAGE_LOAD_TIMEOUT_SECONDS = 15L
    
    // Pagination
    const val DEFAULT_PAGE_SIZE = 20
    const val HOME_FEED_PAGE_SIZE = 20
    const val COMMENTS_PAGE_SIZE = 50
    const val LIKES_PAGE_SIZE = 50
    
    // Image Configuration
    const val MAX_IMAGE_SIZE_MB = 10
    const val IMAGE_COMPRESSION_QUALITY = 85
    const val THUMBNAIL_SIZE = 200
    
    // ========================================
    // REMOTE CONFIG KEYS
    // ========================================
    
    const val RC_FEATURE_PREMIUM_ENABLED = "feature_premium_enabled"
    const val RC_BASIC_RADIUS_METERS = "basic_radius_meters"
    const val RC_EARLY_ACCESS_MINUTES = "early_access_minutes"
    const val RC_AVAILABILITY_PENALTY_STEP = "availability_penalty_step"
    const val RC_MAX_POST_IMAGES = "max_post_images"
    const val RC_POST_EXPIRE_HOURS = "post_expire_hours"
    const val RC_FREE_PREMIUM_EXTEND_HOURS = "free_premium_extend_hours"
    
    // Search Remote Config
    const val RC_SEARCH_ENABLED = "search_enabled"
    const val RC_SEARCH_PAGE_SIZE = "search_page_size"
    const val RC_SEARCH_DEBOUNCE_MS = "search_debounce_ms"
    const val RC_SEARCH_MAX_HOURS_BACK = "search_max_hours_back"
    
    // XP Remote Config
    const val RC_XP_SHARE_POST = "xp_share_post"
    const val RC_XP_DAILY_LOGIN = "xp_daily_login"
    const val RC_XP_LIKE_RECEIVED = "xp_like_received"
    const val RC_XP_COMMENT_RECEIVED = "xp_comment_received"
    const val RC_XP_COMMENT_WRITE = "xp_comment_write"
    const val RC_XP_SHARE_EXTERNAL = "xp_share_external"
    const val RC_XP_PREMIUM_TASK = "xp_premium_task"
    
    // XP Boost Remote Config
    const val RC_XP_BOOST_LVL1_4 = "xp_boost_lvl1_4"
    const val RC_XP_BOOST_LVL5_9 = "xp_boost_lvl5_9"
    const val RC_XP_BOOST_LVL10_14 = "xp_boost_lvl10_14"
    const val RC_XP_BOOST_LVL15P = "xp_boost_lvl15p"
    
    // Premium Remote Config
    const val RC_PAYWALL_AB_TEST_BUCKET = "paywall_ab_test_bucket"
    const val RC_SHOW_XP_PACKS = "rc_show_xp_packs"
    
    // Category Configuration
    const val RC_MAX_CATEGORY_SELECTION = "max_category_selection"
    
    // ========================================
    // REMOTE CONFIG DEFAULT VALUES
    // ========================================
    
    val REMOTE_CONFIG_DEFAULTS = mapOf(
        RC_FEATURE_PREMIUM_ENABLED to true,
        RC_BASIC_RADIUS_METERS to BASIC_RADIUS_METERS,
        RC_EARLY_ACCESS_MINUTES to EARLY_ACCESS_MINUTES,
        RC_AVAILABILITY_PENALTY_STEP to AVAILABILITY_PENALTY_STEP,
        RC_MAX_POST_IMAGES to MAX_POST_IMAGES,
        RC_POST_EXPIRE_HOURS to POST_EXPIRE_HOURS,
        RC_FREE_PREMIUM_EXTEND_HOURS to FREE_PREMIUM_EXTEND_HOURS,
        
        RC_SEARCH_ENABLED to true,
        RC_SEARCH_PAGE_SIZE to SEARCH_PAGE_SIZE,
        RC_SEARCH_DEBOUNCE_MS to SEARCH_DEBOUNCE_MS,
        RC_SEARCH_MAX_HOURS_BACK to SEARCH_MAX_HOURS_BACK,
        
        RC_XP_SHARE_POST to XP_SHARE_POST,
        RC_XP_DAILY_LOGIN to XP_DAILY_LOGIN,
        RC_XP_LIKE_RECEIVED to XP_LIKE_RECEIVED,
        RC_XP_COMMENT_RECEIVED to XP_COMMENT_RECEIVED,
        RC_XP_COMMENT_WRITE to XP_COMMENT_WRITE,
        RC_XP_SHARE_EXTERNAL to XP_SHARE_EXTERNAL,
        RC_XP_PREMIUM_TASK to XP_PREMIUM_TASK,
        
        RC_XP_BOOST_LVL1_4 to XP_BOOST_LV1_4,
        RC_XP_BOOST_LVL5_9 to XP_BOOST_LV5_9,
        RC_XP_BOOST_LVL10_14 to XP_BOOST_LV10_14,
        RC_XP_BOOST_LVL15P to XP_BOOST_LV15_PLUS,
        
        RC_PAYWALL_AB_TEST_BUCKET to "control",
        RC_SHOW_XP_PACKS to true,
        RC_MAX_CATEGORY_SELECTION to 3
    )
    
    // ========================================
    // DEEP LINK CONSTANTS
    // ========================================
    
    const val DEEPLINK_POST = "sperrmuellfinder://post/"
    const val DEEPLINK_PROFILE = "sperrmuellfinder://profile/"
    const val DEEPLINK_PREMIUM = "sperrmuellfinder://premium"
    const val DEEPLINK_SEARCH = "sperrmuellfinder://search"
    const val DEEPLINK_MAP = "sperrmuellfinder://map"
    const val DEEPLINK_NOTIFICATIONS = "sperrmuellfinder://notifications"
    
    // ========================================
    // ERROR CODES
    // ========================================
    
    const val ERROR_NETWORK = "ERROR_NETWORK"
    const val ERROR_PERMISSION_DENIED = "ERROR_PERMISSION_DENIED"
    const val ERROR_USER_NOT_FOUND = "ERROR_USER_NOT_FOUND"
    const val ERROR_POST_NOT_FOUND = "ERROR_POST_NOT_FOUND"
    const val ERROR_PREMIUM_REQUIRED = "ERROR_PREMIUM_REQUIRED"
    const val ERROR_HONESTY_TOO_LOW = "ERROR_HONESTY_TOO_LOW"
    const val ERROR_RATE_LIMIT = "ERROR_RATE_LIMIT"
    const val ERROR_INVALID_CATEGORY = "ERROR_INVALID_CATEGORY"
    const val ERROR_MAX_CATEGORIES_EXCEEDED = "ERROR_MAX_CATEGORIES_EXCEEDED"
    
    // ========================================
    // ANALYTICS EVENT NAMES
    // ========================================
    
    const val EVENT_POST_CREATED = "post_created"
    const val EVENT_POST_LIKED = "post_liked"
    const val EVENT_POST_SHARED = "post_shared"
    const val EVENT_POST_FAVORITED = "post_favorited"
    const val EVENT_PREMIUM_ACTIVATED = "premium_activated"
    const val EVENT_PURCHASE_SUCCESS = "purchase_success"
    const val EVENT_SEARCH_PERFORMED = "search_performed"
    const val EVENT_CATEGORY_SELECTED = "category_selected"
    const val EVENT_XP_EARNED = "xp_earned"
    const val EVENT_LEVEL_UP = "level_up"
    const val EVENT_HONESTY_CHANGED = "honesty_changed"
    
    // ========================================
    // SHARED PREFERENCES KEYS
    // ========================================
    
    const val PREF_USER_ONBOARDED = "user_onboarded"
    const val PREF_LAST_DAILY_LOGIN = "last_daily_login"
    const val PREF_SELECTED_LANGUAGE = "selected_language"
    const val PREF_THEME_MODE = "theme_mode"
    const val PREF_NOTIFICATION_ENABLED = "notification_enabled"
    const val PREF_LOCATION_PERMISSION_ASKED = "location_permission_asked"
    const val PREF_CAMERA_PERMISSION_ASKED = "camera_permission_asked"
    const val PREF_LAST_KNOWN_LOCATION = "last_known_location"
    const val PREF_PREMIUM_PAYWALL_SHOWN_COUNT = "premium_paywall_shown_count"
    
    // ========================================
    // WORKER TAGS
    // ========================================
    
    const val WORKER_TAG_DAILY_XP = "daily_xp_worker"
    const val WORKER_TAG_POST_EXPIRY = "post_expiry_worker"
    const val WORKER_TAG_PREMIUM_SYNC = "premium_sync_worker"
    const val WORKER_TAG_ANALYTICS_UPLOAD = "analytics_upload_worker"
    const val WORKER_TAG_NOTIFICATION_CLEANUP = "notification_cleanup_worker"
}
