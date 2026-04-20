package com.omerkaya.sperrmuellfinder.domain.model

import java.util.Date

/**
 * 🔧 SETTINGS DOMAIN MODELS - SperrmüllFinder
 * Rules.md compliant - Domain layer data models
 */

/**
 * App theme options
 */
enum class AppTheme(val displayNameDe: String, val displayNameEn: String) {
    LIGHT("Hell", "Light"),
    DARK("Dunkel", "Dark"),
    SYSTEM("System", "System");
    
    fun getDisplayName(language: Language): String {
        return when (language) {
            Language.GERMAN -> displayNameDe
            Language.ENGLISH -> displayNameEn
        }
    }
}

/**
 * App language options
 */
enum class Language(val code: String, val displayName: String, val flag: String) {
    GERMAN("de", "Deutsch", "🇩🇪"),
    ENGLISH("en", "English", "🇺🇸");
    
    companion object {
        fun fromCode(code: String): Language {
            return values().find { it.code == code } ?: GERMAN
        }
    }
}

/**
 * User preferences data class
 */
data class UserPreferences(
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: Language = Language.GERMAN,
    val notificationSettings: NotificationSettings = NotificationSettings(),
    val privacySettings: PrivacySettings = PrivacySettings(),
    val lastUpdated: Date = Date()
)

/**
 * Notification settings
 */
data class NotificationSettings(
    val pushNotificationsEnabled: Boolean = true,
    val newPostNotifications: Boolean = true,
    val commentNotifications: Boolean = true,
    val likeNotifications: Boolean = true,
    val followNotifications: Boolean = true,
    val premiumNotifications: Boolean = true,
    val marketingNotifications: Boolean = false,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "08:00"
) {
    /**
     * Check if notifications are allowed at current time
     */
    fun areNotificationsAllowed(): Boolean {
        if (!pushNotificationsEnabled) return false
        if (!quietHoursEnabled) return true
        
        // TODO: Implement quiet hours logic
        return true
    }
}

/**
 * Privacy settings
 */
data class PrivacySettings(
    val profileVisibility: ProfileVisibility = ProfileVisibility.PUBLIC,
    val showOnlineStatus: Boolean = true,
    val allowDirectMessages: Boolean = true,
    val shareLocationData: Boolean = true,
    val shareUsageData: Boolean = true,
    val personalizedAds: Boolean = true,
    val dataProcessingConsent: Boolean = true,
    val marketingConsent: Boolean = false,
    val analyticsConsent: Boolean = true
)

/**
 * Profile visibility options
 */
enum class ProfileVisibility(val displayNameDe: String, val displayNameEn: String) {
    PUBLIC("Öffentlich", "Public"),
    FRIENDS_ONLY("Nur Freunde", "Friends Only"),
    PRIVATE("Privat", "Private");
    
    fun getDisplayName(language: Language): String {
        return when (language) {
            Language.GERMAN -> displayNameDe
            Language.ENGLISH -> displayNameEn
        }
    }
}

/**
 * Settings category for UI organization
 */
enum class SettingsCategory(val titleDe: String, val titleEn: String, val icon: String) {
    ACCOUNT("Konto", "Account", "👤"),
    APPEARANCE("Erscheinungsbild", "Appearance", "🎨"),
    NOTIFICATIONS("Benachrichtigungen", "Notifications", "🔔"),
    PRIVACY("Datenschutz", "Privacy", "🔒"),
    DATA("Daten", "Data", "💾"),
    ABOUT("Über", "About", "ℹ️");
    
    fun getTitle(language: Language): String {
        return when (language) {
            Language.GERMAN -> titleDe
            Language.ENGLISH -> titleEn
        }
    }
}

/**
 * Settings item for UI display
 */
data class SettingsItem(
    val id: String,
    val category: SettingsCategory,
    val titleDe: String,
    val titleEn: String,
    val descriptionDe: String? = null,
    val descriptionEn: String? = null,
    val icon: String? = null,
    val hasSwitch: Boolean = false,
    val hasNavigation: Boolean = false,
    val isEnabled: Boolean = true,
    val isPremiumFeature: Boolean = false
) {
    fun getTitle(language: Language): String {
        return when (language) {
            Language.GERMAN -> titleDe
            Language.ENGLISH -> titleEn
        }
    }
    
    fun getDescription(language: Language): String? {
        return when (language) {
            Language.GERMAN -> descriptionDe
            Language.ENGLISH -> descriptionEn
        }
    }
}

/**
 * App info for About page
 */
data class AppInfo(
    val appName: String = "SperrmüllFinder",
    val version: String = "1.0.0",
    val buildNumber: String = "1",
    val website: String = "https://sperrmuellfinder.de",
    val supportEmail: String = "contact@spermuellfinder.com",
    val privacyPolicyUrl: String = "https://sperrmuellfinder.de/privacy.html",
    val termsOfServiceUrl: String = "",
    val cookiePolicyUrl: String = "https://sperrmuellfinder.de/cookie-richtlinie.html",
    val contactUrl: String = "https://sperrmuellfinder.de/contact.html",
    val faqUrl: String = "https://sperrmuellfinder.de/faq.html",
    val imprintUrl: String = "https://sperrmuellfinder.de/nutzungsbedingungen.html"
)
