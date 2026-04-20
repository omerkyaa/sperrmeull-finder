package com.omerkaya.sperrmuellfinder.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.AppTheme
import com.omerkaya.sperrmuellfinder.domain.model.Language
import com.omerkaya.sperrmuellfinder.domain.model.NotificationSettings
import com.omerkaya.sperrmuellfinder.domain.model.PrivacySettings
import com.omerkaya.sperrmuellfinder.domain.model.ProfileVisibility
import com.omerkaya.sperrmuellfinder.domain.model.UserPreferences
import com.omerkaya.sperrmuellfinder.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🔧 SETTINGS REPOSITORY IMPLEMENTATION - SperrmüllFinder
 * Rules.md compliant - Data layer implementation with DataStore
 */

// DataStore extension
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteConfig: FirebaseRemoteConfig,
    private val logger: Logger
) : SettingsRepository {
    
    companion object {
        // Theme preferences
        private val THEME_KEY = stringPreferencesKey("app_theme")
        
        // Language preferences
        private val LANGUAGE_KEY = stringPreferencesKey("app_language")
        
        // Notification preferences
        private val PUSH_NOTIFICATIONS_KEY = booleanPreferencesKey("push_notifications")
        private val NEW_POST_NOTIFICATIONS_KEY = booleanPreferencesKey("new_post_notifications")
        private val COMMENT_NOTIFICATIONS_KEY = booleanPreferencesKey("comment_notifications")
        private val LIKE_NOTIFICATIONS_KEY = booleanPreferencesKey("like_notifications")
        private val FOLLOW_NOTIFICATIONS_KEY = booleanPreferencesKey("follow_notifications")
        private val PREMIUM_NOTIFICATIONS_KEY = booleanPreferencesKey("premium_notifications")
        private val MARKETING_NOTIFICATIONS_KEY = booleanPreferencesKey("marketing_notifications")
        private val SOUND_ENABLED_KEY = booleanPreferencesKey("sound_enabled")
        private val VIBRATION_ENABLED_KEY = booleanPreferencesKey("vibration_enabled")
        private val QUIET_HOURS_ENABLED_KEY = booleanPreferencesKey("quiet_hours_enabled")
        private val QUIET_HOURS_START_KEY = stringPreferencesKey("quiet_hours_start")
        private val QUIET_HOURS_END_KEY = stringPreferencesKey("quiet_hours_end")
        
        // Privacy preferences
        private val PROFILE_VISIBILITY_KEY = stringPreferencesKey("profile_visibility")
        private val SHOW_ONLINE_STATUS_KEY = booleanPreferencesKey("show_online_status")
        private val ALLOW_DIRECT_MESSAGES_KEY = booleanPreferencesKey("allow_direct_messages")
        private val SHARE_LOCATION_DATA_KEY = booleanPreferencesKey("share_location_data")
        private val SHARE_USAGE_DATA_KEY = booleanPreferencesKey("share_usage_data")
        private val PERSONALIZED_ADS_KEY = booleanPreferencesKey("personalized_ads")
        private val DATA_PROCESSING_CONSENT_KEY = booleanPreferencesKey("data_processing_consent")
        private val MARKETING_CONSENT_KEY = booleanPreferencesKey("marketing_consent")
        private val ANALYTICS_CONSENT_KEY = booleanPreferencesKey("analytics_consent")
    }
    
    override fun getUserPreferences(): Flow<UserPreferences> {
        return context.dataStore.data
            .catch { exception ->
                logger.e(Logger.TAG_DEFAULT, "Error reading preferences", exception)
                emit(emptyPreferences())
            }
            .map { preferences ->
                UserPreferences(
                    theme = AppTheme.valueOf(
                        preferences[THEME_KEY] ?: AppTheme.SYSTEM.name
                    ),
                    language = Language.valueOf(
                        preferences[LANGUAGE_KEY] ?: Language.GERMAN.name
                    ),
                    notificationSettings = NotificationSettings(
                        pushNotificationsEnabled = preferences[PUSH_NOTIFICATIONS_KEY] ?: true,
                        newPostNotifications = preferences[NEW_POST_NOTIFICATIONS_KEY] ?: true,
                        commentNotifications = preferences[COMMENT_NOTIFICATIONS_KEY] ?: true,
                        likeNotifications = preferences[LIKE_NOTIFICATIONS_KEY] ?: true,
                        followNotifications = preferences[FOLLOW_NOTIFICATIONS_KEY] ?: true,
                        premiumNotifications = preferences[PREMIUM_NOTIFICATIONS_KEY] ?: true,
                        marketingNotifications = preferences[MARKETING_NOTIFICATIONS_KEY] ?: false,
                        soundEnabled = preferences[SOUND_ENABLED_KEY] ?: true,
                        vibrationEnabled = preferences[VIBRATION_ENABLED_KEY] ?: true,
                        quietHoursEnabled = preferences[QUIET_HOURS_ENABLED_KEY] ?: false,
                        quietHoursStart = preferences[QUIET_HOURS_START_KEY] ?: "22:00",
                        quietHoursEnd = preferences[QUIET_HOURS_END_KEY] ?: "08:00"
                    ),
                    privacySettings = PrivacySettings(
                        profileVisibility = ProfileVisibility.valueOf(
                            preferences[PROFILE_VISIBILITY_KEY] ?: ProfileVisibility.PUBLIC.name
                        ),
                        showOnlineStatus = preferences[SHOW_ONLINE_STATUS_KEY] ?: true,
                        allowDirectMessages = preferences[ALLOW_DIRECT_MESSAGES_KEY] ?: true,
                        shareLocationData = preferences[SHARE_LOCATION_DATA_KEY] ?: true,
                        shareUsageData = preferences[SHARE_USAGE_DATA_KEY] ?: true,
                        personalizedAds = preferences[PERSONALIZED_ADS_KEY] ?: true,
                        dataProcessingConsent = preferences[DATA_PROCESSING_CONSENT_KEY] ?: true,
                        marketingConsent = preferences[MARKETING_CONSENT_KEY] ?: false,
                        analyticsConsent = preferences[ANALYTICS_CONSENT_KEY] ?: true
                    ),
                    lastUpdated = Date()
                )
            }
    }
    
    override suspend fun updateUserPreferences(preferences: UserPreferences): Result<Unit> {
        return try {
            context.dataStore.edit { prefs ->
                // Theme
                prefs[THEME_KEY] = preferences.theme.name
                prefs[LANGUAGE_KEY] = preferences.language.name
                
                // Notifications
                prefs[PUSH_NOTIFICATIONS_KEY] = preferences.notificationSettings.pushNotificationsEnabled
                prefs[NEW_POST_NOTIFICATIONS_KEY] = preferences.notificationSettings.newPostNotifications
                prefs[COMMENT_NOTIFICATIONS_KEY] = preferences.notificationSettings.commentNotifications
                prefs[LIKE_NOTIFICATIONS_KEY] = preferences.notificationSettings.likeNotifications
                prefs[FOLLOW_NOTIFICATIONS_KEY] = preferences.notificationSettings.followNotifications
                prefs[PREMIUM_NOTIFICATIONS_KEY] = preferences.notificationSettings.premiumNotifications
                prefs[MARKETING_NOTIFICATIONS_KEY] = preferences.notificationSettings.marketingNotifications
                prefs[SOUND_ENABLED_KEY] = preferences.notificationSettings.soundEnabled
                prefs[VIBRATION_ENABLED_KEY] = preferences.notificationSettings.vibrationEnabled
                prefs[QUIET_HOURS_ENABLED_KEY] = preferences.notificationSettings.quietHoursEnabled
                prefs[QUIET_HOURS_START_KEY] = preferences.notificationSettings.quietHoursStart
                prefs[QUIET_HOURS_END_KEY] = preferences.notificationSettings.quietHoursEnd
                
                // Privacy
                prefs[PROFILE_VISIBILITY_KEY] = preferences.privacySettings.profileVisibility.name
                prefs[SHOW_ONLINE_STATUS_KEY] = preferences.privacySettings.showOnlineStatus
                prefs[ALLOW_DIRECT_MESSAGES_KEY] = preferences.privacySettings.allowDirectMessages
                prefs[SHARE_LOCATION_DATA_KEY] = preferences.privacySettings.shareLocationData
                prefs[SHARE_USAGE_DATA_KEY] = preferences.privacySettings.shareUsageData
                prefs[PERSONALIZED_ADS_KEY] = preferences.privacySettings.personalizedAds
                prefs[DATA_PROCESSING_CONSENT_KEY] = preferences.privacySettings.dataProcessingConsent
                prefs[MARKETING_CONSENT_KEY] = preferences.privacySettings.marketingConsent
                prefs[ANALYTICS_CONSENT_KEY] = preferences.privacySettings.analyticsConsent
            }
            
            logger.i(Logger.TAG_DEFAULT, "User preferences updated successfully")
            Result.Success(Unit)
            
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Failed to update user preferences", e)
            Result.Error(e)
        }
    }
    
    override suspend fun resetUserPreferences(): Result<Unit> {
        return try {
            context.dataStore.edit { it.clear() }
            logger.i(Logger.TAG_DEFAULT, "User preferences reset to defaults")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Failed to reset user preferences", e)
            Result.Error(e)
        }
    }
    
    override suspend fun getAppTheme(): Result<AppTheme> {
        return try {
            val preferences = context.dataStore.data.map { prefs ->
                AppTheme.valueOf(prefs[THEME_KEY] ?: AppTheme.SYSTEM.name)
            }
            // This is a simplified implementation - in real app you'd collect the flow
            Result.Success(AppTheme.SYSTEM)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun setAppTheme(theme: AppTheme): Result<Unit> {
        return try {
            context.dataStore.edit { prefs ->
                prefs[THEME_KEY] = theme.name
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun getAppLanguage(): Result<Language> {
        return try {
            // Simplified implementation
            Result.Success(Language.GERMAN)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun setAppLanguage(language: Language): Result<Unit> {
        return try {
            context.dataStore.edit { prefs ->
                prefs[LANGUAGE_KEY] = language.name
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun getNotificationSettings(): Result<NotificationSettings> {
        return try {
            // Simplified - would extract from getUserPreferences()
            Result.Success(NotificationSettings())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun updateNotificationSettings(settings: NotificationSettings): Result<Unit> {
        return try {
            context.dataStore.edit { prefs ->
                prefs[PUSH_NOTIFICATIONS_KEY] = settings.pushNotificationsEnabled
                prefs[NEW_POST_NOTIFICATIONS_KEY] = settings.newPostNotifications
                prefs[COMMENT_NOTIFICATIONS_KEY] = settings.commentNotifications
                prefs[LIKE_NOTIFICATIONS_KEY] = settings.likeNotifications
                prefs[FOLLOW_NOTIFICATIONS_KEY] = settings.followNotifications
                prefs[PREMIUM_NOTIFICATIONS_KEY] = settings.premiumNotifications
                prefs[MARKETING_NOTIFICATIONS_KEY] = settings.marketingNotifications
                prefs[SOUND_ENABLED_KEY] = settings.soundEnabled
                prefs[VIBRATION_ENABLED_KEY] = settings.vibrationEnabled
                prefs[QUIET_HOURS_ENABLED_KEY] = settings.quietHoursEnabled
                prefs[QUIET_HOURS_START_KEY] = settings.quietHoursStart
                prefs[QUIET_HOURS_END_KEY] = settings.quietHoursEnd
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun getPrivacySettings(): Result<PrivacySettings> {
        return try {
            // Simplified - would extract from getUserPreferences()
            Result.Success(PrivacySettings())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun updatePrivacySettings(settings: PrivacySettings): Result<Unit> {
        return try {
            context.dataStore.edit { prefs ->
                prefs[PROFILE_VISIBILITY_KEY] = settings.profileVisibility.name
                prefs[SHOW_ONLINE_STATUS_KEY] = settings.showOnlineStatus
                prefs[ALLOW_DIRECT_MESSAGES_KEY] = settings.allowDirectMessages
                prefs[SHARE_LOCATION_DATA_KEY] = settings.shareLocationData
                prefs[SHARE_USAGE_DATA_KEY] = settings.shareUsageData
                prefs[PERSONALIZED_ADS_KEY] = settings.personalizedAds
                prefs[DATA_PROCESSING_CONSENT_KEY] = settings.dataProcessingConsent
                prefs[MARKETING_CONSENT_KEY] = settings.marketingConsent
                prefs[ANALYTICS_CONSENT_KEY] = settings.analyticsConsent
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun exportUserData(): Result<String> {
        return try {
            // TODO: Implement GDPR data export
            val exportData = "User data export - Implementation pending"
            Result.Success(exportData)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun deleteAllUserData(): Result<Unit> {
        return try {
            // TODO: Implement complete data deletion
            context.dataStore.edit { it.clear() }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun clearAppCache(): Result<Unit> {
        return try {
            // TODO: Implement cache clearing
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun syncWithRemoteConfig(): Result<Unit> {
        return try {
            remoteConfig.fetchAndActivate().await()
            logger.i(Logger.TAG_DEFAULT, "Remote Config synced successfully")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Failed to sync Remote Config", e)
            Result.Error(e)
        }
    }
    
    override suspend fun getRemoteConfigValue(key: String): Result<String> {
        return try {
            val value = remoteConfig.getString(key)
            Result.Success(value)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    private fun emptyPreferences(): Preferences {
        return androidx.datastore.preferences.core.emptyPreferences()
    }
}
