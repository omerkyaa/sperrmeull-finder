package com.omerkaya.sperrmuellfinder.domain.repository

import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.AppTheme
import com.omerkaya.sperrmuellfinder.domain.model.Language
import com.omerkaya.sperrmuellfinder.domain.model.NotificationSettings
import com.omerkaya.sperrmuellfinder.domain.model.PrivacySettings
import com.omerkaya.sperrmuellfinder.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/**
 * 🔧 SETTINGS REPOSITORY INTERFACE - SperrmüllFinder
 * Rules.md compliant - Domain layer repository interface
 * 
 * Features:
 * - User preferences management (theme, language, notifications)
 * - Privacy settings control
 * - Remote Config integration
 * - Data persistence and synchronization
 */
interface SettingsRepository {
    
    // ========================================
    // USER PREFERENCES
    // ========================================
    
    /**
     * Get user preferences as Flow for reactive updates
     */
    fun getUserPreferences(): Flow<UserPreferences>
    
    /**
     * Update user preferences
     */
    suspend fun updateUserPreferences(preferences: UserPreferences): Result<Unit>
    
    /**
     * Reset user preferences to defaults
     */
    suspend fun resetUserPreferences(): Result<Unit>
    
    // ========================================
    // THEME SETTINGS
    // ========================================
    
    /**
     * Get current app theme
     */
    suspend fun getAppTheme(): Result<AppTheme>
    
    /**
     * Set app theme
     */
    suspend fun setAppTheme(theme: AppTheme): Result<Unit>
    
    // ========================================
    // LANGUAGE SETTINGS
    // ========================================
    
    /**
     * Get current app language
     */
    suspend fun getAppLanguage(): Result<Language>
    
    /**
     * Set app language
     */
    suspend fun setAppLanguage(language: Language): Result<Unit>
    
    // ========================================
    // NOTIFICATION SETTINGS
    // ========================================
    
    /**
     * Get notification settings
     */
    suspend fun getNotificationSettings(): Result<NotificationSettings>
    
    /**
     * Update notification settings
     */
    suspend fun updateNotificationSettings(settings: NotificationSettings): Result<Unit>
    
    // ========================================
    // PRIVACY SETTINGS
    // ========================================
    
    /**
     * Get privacy settings
     */
    suspend fun getPrivacySettings(): Result<PrivacySettings>
    
    /**
     * Update privacy settings
     */
    suspend fun updatePrivacySettings(settings: PrivacySettings): Result<Unit>
    
    // ========================================
    // DATA MANAGEMENT
    // ========================================
    
    /**
     * Export user data for GDPR compliance
     */
    suspend fun exportUserData(): Result<String>
    
    /**
     * Delete all user data
     */
    suspend fun deleteAllUserData(): Result<Unit>
    
    /**
     * Clear app cache
     */
    suspend fun clearAppCache(): Result<Unit>
    
    // ========================================
    // REMOTE CONFIG
    // ========================================
    
    /**
     * Sync settings with Remote Config
     */
    suspend fun syncWithRemoteConfig(): Result<Unit>
    
    /**
     * Get Remote Config value
     */
    suspend fun getRemoteConfigValue(key: String): Result<String>
}
