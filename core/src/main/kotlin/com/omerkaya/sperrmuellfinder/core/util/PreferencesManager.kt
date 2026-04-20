package com.omerkaya.sperrmuellfinder.core.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.omerkaya.sperrmuellfinder.core.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val logger: Logger
) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Failed to create encrypted preferences, falling back to normal", e)
            context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private val normalPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(NORMAL_PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * User Authentication State
     */
    suspend fun setUserLoggedIn(isLoggedIn: Boolean) {
        withContext(ioDispatcher) {
            encryptedPrefs.edit()
                .putBoolean(KEY_USER_LOGGED_IN, isLoggedIn)
                .apply()
        }
    }

    fun isUserLoggedIn(): Boolean = encryptedPrefs.getBoolean(KEY_USER_LOGGED_IN, false)

    suspend fun setUserId(userId: String?) {
        withContext(ioDispatcher) {
            encryptedPrefs.edit()
                .putString(KEY_USER_ID, userId)
                .apply()
        }
    }

    fun getUserId(): String? = encryptedPrefs.getString(KEY_USER_ID, null)

    /**
     * User Profile Data
     */
    suspend fun setUserDisplayName(displayName: String?) {
        withContext(ioDispatcher) {
            encryptedPrefs.edit()
                .putString(KEY_USER_DISPLAY_NAME, displayName)
                .apply()
        }
    }

    fun getUserDisplayName(): String? = encryptedPrefs.getString(KEY_USER_DISPLAY_NAME, null)

    suspend fun setUserEmail(email: String?) {
        withContext(ioDispatcher) {
            encryptedPrefs.edit()
                .putString(KEY_USER_EMAIL, email)
                .apply()
        }
    }

    fun getUserEmail(): String? = encryptedPrefs.getString(KEY_USER_EMAIL, null)

    suspend fun setUserCity(city: String?) {
        withContext(ioDispatcher) {
            encryptedPrefs.edit()
                .putString(KEY_USER_CITY, city)
                .apply()
        }
    }

    fun getUserCity(): String? = encryptedPrefs.getString(KEY_USER_CITY, null)

    /**
     * Premium Status
     */
    suspend fun setPremiumStatus(isPremium: Boolean) {
        withContext(ioDispatcher) {
            encryptedPrefs.edit()
                .putBoolean(KEY_IS_PREMIUM, isPremium)
                .apply()
        }
    }

    fun isPremium(): Boolean = encryptedPrefs.getBoolean(KEY_IS_PREMIUM, false)

    suspend fun setPremiumExpiryTime(expiryTime: Long) {
        withContext(ioDispatcher) {
            encryptedPrefs.edit()
                .putLong(KEY_PREMIUM_EXPIRY, expiryTime)
                .apply()
        }
    }

    fun getPremiumExpiryTime(): Long = encryptedPrefs.getLong(KEY_PREMIUM_EXPIRY, 0L)

    /**
     * User Level and XP
     */
    suspend fun setUserLevel(level: Int) {
        withContext(ioDispatcher) {
            normalPrefs.edit()
                .putInt(KEY_USER_LEVEL, level)
                .apply()
        }
    }

    fun getUserLevel(): Int = normalPrefs.getInt(KEY_USER_LEVEL, 1)

    suspend fun setUserXP(xp: Int) {
        withContext(ioDispatcher) {
            normalPrefs.edit()
                .putInt(KEY_USER_XP, xp)
                .apply()
        }
    }

    fun getUserXP(): Int = normalPrefs.getInt(KEY_USER_XP, 100) // Start with 100 XP

    suspend fun setUserHonesty(honesty: Int) {
        withContext(ioDispatcher) {
            normalPrefs.edit()
                .putInt(KEY_USER_HONESTY, honesty)
                .apply()
        }
    }

    fun getUserHonesty(): Int = normalPrefs.getInt(KEY_USER_HONESTY, 100) // Start with 100 honesty

    /**
     * App Settings
     */
    suspend fun setThemeMode(themeMode: String) {
        withContext(ioDispatcher) {
            normalPrefs.edit()
                .putString(KEY_THEME_MODE, themeMode)
                .apply()
        }
    }

    fun getThemeMode(): String = normalPrefs.getString(KEY_THEME_MODE, THEME_MODE_SYSTEM) ?: THEME_MODE_SYSTEM

    suspend fun setLanguage(language: String) {
        withContext(ioDispatcher) {
            normalPrefs.edit()
                .putString(KEY_LANGUAGE, language)
                .apply()
        }
    }

    fun getLanguage(): String = normalPrefs.getString(KEY_LANGUAGE, LANGUAGE_GERMAN) ?: LANGUAGE_GERMAN

    /**
     * Notification Settings
     */
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        withContext(ioDispatcher) {
            normalPrefs.edit()
                .putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled)
                .apply()
        }
    }

    fun areNotificationsEnabled(): Boolean = normalPrefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)

    suspend fun setPostNotificationsEnabled(enabled: Boolean) {
        withContext(ioDispatcher) {
            normalPrefs.edit()
                .putBoolean(KEY_POST_NOTIFICATIONS, enabled)
                .apply()
        }
    }

    fun arePostNotificationsEnabled(): Boolean = normalPrefs.getBoolean(KEY_POST_NOTIFICATIONS, true)

    suspend fun setSocialNotificationsEnabled(enabled: Boolean) {
        withContext(ioDispatcher) {
            normalPrefs.edit()
                .putBoolean(KEY_SOCIAL_NOTIFICATIONS, enabled)
                .apply()
        }
    }

    fun areSocialNotificationsEnabled(): Boolean = normalPrefs.getBoolean(KEY_SOCIAL_NOTIFICATIONS, true)

    suspend fun setPremiumNotificationsEnabled(enabled: Boolean) {
        withContext(ioDispatcher) {
            normalPrefs.edit()
                .putBoolean(KEY_PREMIUM_NOTIFICATIONS, enabled)
                .apply()
        }
    }

    fun arePremiumNotificationsEnabled(): Boolean = normalPrefs.getBoolean(KEY_PREMIUM_NOTIFICATIONS, true)

    /**
     * App State
     */
    suspend fun setOnboardingCompleted(completed: Boolean) {
        withContext(ioDispatcher) {
            normalPrefs.edit()
                .putBoolean(KEY_ONBOARDING_COMPLETED, completed)
                .apply()
        }
    }

    fun isOnboardingCompleted(): Boolean = normalPrefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    suspend fun setFirstLaunch(isFirstLaunch: Boolean) {
        withContext(ioDispatcher) {
            normalPrefs.edit()
                .putBoolean(KEY_FIRST_LAUNCH, isFirstLaunch)
                .apply()
        }
    }

    fun isFirstLaunch(): Boolean = normalPrefs.getBoolean(KEY_FIRST_LAUNCH, true)

    suspend fun setLastSyncTime(timestamp: Long) {
        withContext(ioDispatcher) {
            normalPrefs.edit()
                .putLong(KEY_LAST_SYNC_TIME, timestamp)
                .apply()
        }
    }

    fun getLastSyncTime(): Long = normalPrefs.getLong(KEY_LAST_SYNC_TIME, 0L)

    /**
     * FCM Token
     */
    suspend fun setFCMToken(token: String?) {
        withContext(ioDispatcher) {
            encryptedPrefs.edit()
                .putString(KEY_FCM_TOKEN, token)
                .apply()
        }
    }

    fun getFCMToken(): String? = encryptedPrefs.getString(KEY_FCM_TOKEN, null)

    /**
     * Camera Settings
     */
    suspend fun setCameraPermissionRequested(requested: Boolean) {
        withContext(ioDispatcher) {
            normalPrefs.edit()
                .putBoolean(KEY_CAMERA_PERMISSION_REQUESTED, requested)
                .apply()
        }
    }

    fun isCameraPermissionRequested(): Boolean = normalPrefs.getBoolean(KEY_CAMERA_PERMISSION_REQUESTED, false)

    suspend fun setLocationPermissionRequested(requested: Boolean) {
        withContext(ioDispatcher) {
            normalPrefs.edit()
                .putBoolean(KEY_LOCATION_PERMISSION_REQUESTED, requested)
                .apply()
        }
    }

    fun isLocationPermissionRequested(): Boolean = normalPrefs.getBoolean(KEY_LOCATION_PERMISSION_REQUESTED, false)

    /**
     * Clear all data (logout)
     */
    suspend fun clearAllData() {
        withContext(ioDispatcher) {
            encryptedPrefs.edit().clear().apply()
            normalPrefs.edit().clear().apply()
            logger.i(Logger.TAG_DEFAULT, "All preferences cleared")
        }
    }

    /**
     * Clear sensitive data only (keep settings)
     */
    suspend fun clearSensitiveData() {
        withContext(ioDispatcher) {
            encryptedPrefs.edit()
                .remove(KEY_USER_LOGGED_IN)
                .remove(KEY_USER_ID)
                .remove(KEY_USER_DISPLAY_NAME)
                .remove(KEY_USER_EMAIL)
                .remove(KEY_IS_PREMIUM)
                .remove(KEY_PREMIUM_EXPIRY)
                .remove(KEY_FCM_TOKEN)
                .apply()
            
            normalPrefs.edit()
                .remove(KEY_USER_LEVEL)
                .remove(KEY_USER_XP)
                .remove(KEY_USER_HONESTY)
                .remove(KEY_USER_CITY)
                .apply()
            
            logger.i(Logger.TAG_DEFAULT, "Sensitive data cleared")
        }
    }

    /**
     * Reactive preferences using Flow
     */
    fun isPremiumFlow(): Flow<Boolean> = flow {
        emit(isPremium())
    }.flowOn(ioDispatcher)

    fun getUserLevelFlow(): Flow<Int> = flow {
        emit(getUserLevel())
    }.flowOn(ioDispatcher)

    fun getThemeModeFlow(): Flow<String> = flow {
        emit(getThemeMode())
    }.flowOn(ioDispatcher)

    companion object {
        private const val ENCRYPTED_PREFS_NAME = "sperrmuell_finder_secure_prefs"
        private const val NORMAL_PREFS_NAME = "sperrmuell_finder_prefs"
        private const val FALLBACK_PREFS_NAME = "sperrmuell_finder_fallback_prefs"

        // Authentication keys
        private const val KEY_USER_LOGGED_IN = "user_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_DISPLAY_NAME = "user_display_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_CITY = "user_city"

        // Premium keys
        private const val KEY_IS_PREMIUM = "is_premium"
        private const val KEY_PREMIUM_EXPIRY = "premium_expiry"

        // User progress keys
        private const val KEY_USER_LEVEL = "user_level"
        private const val KEY_USER_XP = "user_xp"
        private const val KEY_USER_HONESTY = "user_honesty"

        // App settings keys
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_LANGUAGE = "language"

        // Notification keys
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_POST_NOTIFICATIONS = "post_notifications"
        private const val KEY_SOCIAL_NOTIFICATIONS = "social_notifications"
        private const val KEY_PREMIUM_NOTIFICATIONS = "premium_notifications"

        // App state keys
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"

        // FCM key
        private const val KEY_FCM_TOKEN = "fcm_token"

        // Permission keys
        private const val KEY_CAMERA_PERMISSION_REQUESTED = "camera_permission_requested"
        private const val KEY_LOCATION_PERMISSION_REQUESTED = "location_permission_requested"

        // Theme modes
        const val THEME_MODE_LIGHT = "light"
        const val THEME_MODE_DARK = "dark"
        const val THEME_MODE_SYSTEM = "system"

        // Languages
        const val LANGUAGE_GERMAN = "de"
        const val LANGUAGE_ENGLISH = "en"
    }
}
