package com.omerkaya.sperrmuellfinder.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.annotation.StringRes
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.google.firebase.auth.FirebaseAuth
import com.omerkaya.sperrmuellfinder.data.messaging.NotificationTokenHelper
import com.omerkaya.sperrmuellfinder.domain.model.AppInfo
import com.omerkaya.sperrmuellfinder.domain.model.AppTheme
import com.omerkaya.sperrmuellfinder.domain.model.Language
import com.omerkaya.sperrmuellfinder.domain.model.NotificationSettings
import com.omerkaya.sperrmuellfinder.domain.model.PrivacySettings
import com.omerkaya.sperrmuellfinder.domain.model.SettingsCategory
import com.omerkaya.sperrmuellfinder.domain.model.SettingsItem
import com.omerkaya.sperrmuellfinder.domain.model.UserPreferences
import com.omerkaya.sperrmuellfinder.domain.manager.PremiumManager
import com.omerkaya.sperrmuellfinder.domain.repository.AdminRepository
import com.omerkaya.sperrmuellfinder.domain.usecase.auth.LogoutUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.settings.GetAppInfoUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.settings.GetUserPreferencesUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.settings.UpdateUserPreferencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🔧 SETTINGS VIEWMODEL - SperrmüllFinder
 * Rules.md compliant - UI layer ViewModel with state management
 */

/**
 * Settings UI State
 */
data class SettingsUiState(
    val userPreferences: UserPreferences = UserPreferences(),
    val appInfo: AppInfo = AppInfo(),
    val isLoading: Boolean = false,
    val isAdmin: Boolean = false,
    val isPremium: Boolean = false,
    val error: String? = null,
    val showThemeDialog: Boolean = false,
    val showLanguageDialog: Boolean = false,
    val showPrivacyDialog: Boolean = false,
    val showLogoutDialog: Boolean = false
)

/**
 * Settings Events
 */
sealed class SettingsEvent {
    object NavigateBack : SettingsEvent()
    object ShowThemeDialog : SettingsEvent()
    object ShowLanguageDialog : SettingsEvent()
    object ShowNotificationSettings : SettingsEvent()
    object ShowPrivacySettings : SettingsEvent()
    object ShowAboutPage : SettingsEvent()
    object ShowLogoutDialog : SettingsEvent()
    object NavigateToLogin : SettingsEvent()
    object NavigateToBlockedUsers : SettingsEvent()
    object NavigateToDeleteAccount : SettingsEvent()
    object NavigateToAdminDashboard : SettingsEvent()
    data class NavigateToPremium(val isPremium: Boolean) : SettingsEvent()
    data class OpenUrl(val url: String) : SettingsEvent()
    data class ShowError(@StringRes val messageResId: Int) : SettingsEvent()
    data class ShowSuccess(@StringRes val messageResId: Int) : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getUserPreferencesUseCase: GetUserPreferencesUseCase,
    private val updateUserPreferencesUseCase: UpdateUserPreferencesUseCase,
    private val getAppInfoUseCase: GetAppInfoUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val notificationTokenHelper: NotificationTokenHelper,
    private val premiumManager: PremiumManager,
    private val adminRepository: AdminRepository,
    private val logger: Logger
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    private val _events = Channel<SettingsEvent>()
    val events = _events.receiveAsFlow()
    
    init {
        logger.d(Logger.TAG_DEFAULT, "SettingsViewModel initialized")
        loadUserPreferences()
        loadAppInfo()
        observeAdminRole()
        observePremiumStatus()
    }

    private fun observeAdminRole() {
        viewModelScope.launch {
            adminRepository.observeAdminRole().collect { role ->
                _uiState.value = _uiState.value.copy(isAdmin = role != null)
            }
        }
    }

    private fun observePremiumStatus() {
        viewModelScope.launch {
            premiumManager.isPremium.collect { isPremium ->
                _uiState.value = _uiState.value.copy(isPremium = isPremium)
            }
        }
    }
    
    /**
     * Load user preferences
     */
    private fun loadUserPreferences() {
        viewModelScope.launch {
            getUserPreferencesUseCase().collect { preferences ->
                _uiState.value = _uiState.value.copy(
                    userPreferences = preferences,
                    isLoading = false
                )
            }
        }
    }
    
    /**
     * Load app information
     */
    private fun loadAppInfo() {
        val appInfo = getAppInfoUseCase()
        _uiState.value = _uiState.value.copy(appInfo = appInfo)
    }
    
    /**
     * Update app theme
     */
    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch {
            logger.d(Logger.TAG_DEFAULT, "Updating theme to: $theme")
            
            val updatedPreferences = _uiState.value.userPreferences.copy(theme = theme)
            
            when (val result = updateUserPreferencesUseCase(updatedPreferences)) {
                is Result.Success -> {
                    logger.i(Logger.TAG_DEFAULT, "Theme updated successfully")
                    hideThemeDialog()
                }
                is Result.Error -> {
                    logger.e(Logger.TAG_DEFAULT, "Failed to update theme", result.exception)
                    _events.send(SettingsEvent.ShowError(R.string.settings_update_failed))
                }
                is Result.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }
    
    /**
     * Update app language
     */
    fun updateLanguage(language: Language) {
        viewModelScope.launch {
            logger.d(Logger.TAG_DEFAULT, "Updating language to: $language")
            
            val updatedPreferences = _uiState.value.userPreferences.copy(language = language)
            
            when (val result = updateUserPreferencesUseCase(updatedPreferences)) {
                is Result.Success -> {
                    logger.i(Logger.TAG_DEFAULT, "Language updated successfully")
                    hideLanguageDialog()
                }
                is Result.Error -> {
                    logger.e(Logger.TAG_DEFAULT, "Failed to update language", result.exception)
                    _events.send(SettingsEvent.ShowError(R.string.settings_update_failed))
                }
                is Result.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }
    
    /**
     * Update notification settings
     */
    fun updateNotificationSettings(settings: NotificationSettings) {
        viewModelScope.launch {
            logger.d(Logger.TAG_DEFAULT, "Updating notification settings")
            
            val updatedPreferences = _uiState.value.userPreferences.copy(
                notificationSettings = settings
            )
            
            when (val result = updateUserPreferencesUseCase(updatedPreferences)) {
                is Result.Success -> {
                    logger.i(Logger.TAG_DEFAULT, "Notification settings updated successfully")
                }
                is Result.Error -> {
                    logger.e(Logger.TAG_DEFAULT, "Failed to update notification settings", result.exception)
                    _events.send(SettingsEvent.ShowError(R.string.settings_update_failed))
                }
                is Result.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }
    
    /**
     * Update privacy settings
     */
    fun updatePrivacySettings(settings: PrivacySettings) {
        viewModelScope.launch {
            logger.d(Logger.TAG_DEFAULT, "Updating privacy settings")
            
            val updatedPreferences = _uiState.value.userPreferences.copy(
                privacySettings = settings
            )
            
            when (val result = updateUserPreferencesUseCase(updatedPreferences)) {
                is Result.Success -> {
                    logger.i(Logger.TAG_DEFAULT, "Privacy settings updated successfully")
                }
                is Result.Error -> {
                    logger.e(Logger.TAG_DEFAULT, "Failed to update privacy settings", result.exception)
                    _events.send(SettingsEvent.ShowError(R.string.settings_update_failed))
                }
                is Result.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }
    
    /**
     * Show theme selection dialog
     */
    fun showThemeDialog() {
        _uiState.value = _uiState.value.copy(showThemeDialog = true)
    }
    
    /**
     * Hide theme selection dialog
     */
    fun hideThemeDialog() {
        _uiState.value = _uiState.value.copy(showThemeDialog = false)
    }
    
    /**
     * Show language selection dialog
     */
    fun showLanguageDialog() {
        _uiState.value = _uiState.value.copy(showLanguageDialog = true)
    }
    
    /**
     * Hide language selection dialog
     */
    fun hideLanguageDialog() {
        _uiState.value = _uiState.value.copy(showLanguageDialog = false)
    }

    /**
     * Show privacy settings dialog
     */
    fun showPrivacyDialog() {
        _uiState.value = _uiState.value.copy(showPrivacyDialog = true)
    }

    /**
     * Hide privacy settings dialog
     */
    fun hidePrivacyDialog() {
        _uiState.value = _uiState.value.copy(showPrivacyDialog = false)
    }
    
    /**
     * Navigate to notification settings
     */
    fun navigateToNotificationSettings() {
        viewModelScope.launch {
            _events.send(SettingsEvent.ShowNotificationSettings)
        }
    }
    
    /**
     * Navigate to privacy settings
     */
    fun navigateToPrivacySettings() {
        viewModelScope.launch {
            _events.send(SettingsEvent.ShowPrivacySettings)
        }
    }
    
    /**
     * Navigate to about page
     */
    fun navigateToAboutPage() {
        viewModelScope.launch {
            _events.send(SettingsEvent.ShowAboutPage)
        }
    }
    
    /**
     * Open URL in browser
     */
    fun openUrl(url: String) {
        viewModelScope.launch {
            logger.d(Logger.TAG_DEFAULT, "Opening URL: $url")
            _events.send(SettingsEvent.OpenUrl(url))
        }
    }
    
    
    /**
     * Get settings items for UI display
     */
    fun getSettingsItems(): List<SettingsItem> {
        val isAdmin = _uiState.value.isAdmin
        val isPremium = _uiState.value.isPremium

        return buildList {
            // Account Settings
            add(SettingsItem(
                id = "theme",
                category = SettingsCategory.ACCOUNT,
                titleDe = "Design",
                titleEn = "Theme",
                descriptionDe = "Hell, Dunkel oder System",
                descriptionEn = "Light, Dark or System",
                icon = "🎨",
                hasNavigation = true
            ))
            add(SettingsItem(
                id = "language",
                category = SettingsCategory.ACCOUNT,
                titleDe = "Sprache",
                titleEn = "Language",
                descriptionDe = "Deutsch oder English",
                descriptionEn = "German or English",
                icon = "🌐",
                hasNavigation = true
            ))
            add(SettingsItem(
                id = "premium",
                category = SettingsCategory.ACCOUNT,
                titleDe = "Premium",
                titleEn = "Premium",
                descriptionDe = if (isPremium) "Plan verwalten oder upgraden" else "Premium freischalten",
                descriptionEn = if (isPremium) "Manage or upgrade your plan" else "Unlock Premium",
                icon = "⭐",
                hasNavigation = true
            ))
            add(SettingsItem(
                id = "logout",
                category = SettingsCategory.ACCOUNT,
                titleDe = "Abmelden",
                titleEn = "Logout",
                descriptionDe = "Vom Konto abmelden",
                descriptionEn = "Sign out of account",
                icon = "🚪",
                hasNavigation = true
            ))

            if (isAdmin) {
                add(SettingsItem(
                    id = "admin_dashboard",
                    category = SettingsCategory.ACCOUNT,
                    titleDe = "Admin-Dashboard",
                    titleEn = "Admin Dashboard",
                    descriptionDe = "Moderation und Admin-Tools",
                    descriptionEn = "Moderation and admin tools",
                    icon = "🛡️",
                    hasNavigation = true
                ))
            }
            
            // Notifications
            add(SettingsItem(
                id = "notifications",
                category = SettingsCategory.NOTIFICATIONS,
                titleDe = "Benachrichtigungen",
                titleEn = "Notifications",
                descriptionDe = "Push-Benachrichtigungen verwalten",
                descriptionEn = "Manage push notifications",
                icon = "🔔",
                hasNavigation = true
            ))
            
            // Privacy detail screen intentionally hidden for MVP.
            // Infrastructure stays in codebase for future messaging rollout.
            add(SettingsItem(
                id = "blocked_users",
                category = SettingsCategory.PRIVACY,
                titleDe = "Blockierte Nutzer",
                titleEn = "Blocked Users",
                descriptionDe = "Blockierte Nutzer verwalten",
                descriptionEn = "Manage blocked users",
                icon = "🚫",
                hasNavigation = true
            ))
            add(SettingsItem(
                id = "delete_account",
                category = SettingsCategory.PRIVACY,
                titleDe = "Konto löschen",
                titleEn = "Delete Account",
                descriptionDe = "Konto dauerhaft löschen",
                descriptionEn = "Permanently delete account",
                icon = "⚠️",
                hasNavigation = true
            ))
            
            
            // About
            add(SettingsItem(
                id = "about",
                category = SettingsCategory.ABOUT,
                titleDe = "Über die App",
                titleEn = "About App",
                descriptionDe = "Version, Entwickler, Links",
                descriptionEn = "Version, Developer, Links",
                icon = "ℹ️",
                hasNavigation = true
            ))
        }
    }
    
    /**
     * Handle settings item click
     */
    fun onSettingsItemClick(item: SettingsItem) {
        viewModelScope.launch {
            when (item.id) {
                "theme" -> showThemeDialog()
                "language" -> showLanguageDialog()
                "logout" -> showLogoutDialog()
                "notifications" -> _events.send(SettingsEvent.ShowNotificationSettings)
                "privacy" -> Unit // Hidden in MVP UI, kept for future infrastructure usage.
                "blocked_users" -> _events.send(SettingsEvent.NavigateToBlockedUsers)
                "delete_account" -> _events.send(SettingsEvent.NavigateToDeleteAccount)
                "admin_dashboard" -> _events.send(SettingsEvent.NavigateToAdminDashboard)
                "premium" -> _events.send(SettingsEvent.NavigateToPremium(_uiState.value.isPremium))
                "about" -> _events.send(SettingsEvent.ShowAboutPage)
            }
        }
    }
    
    /**
     * Show logout confirmation dialog
     */
    fun showLogoutDialog() {
        _uiState.value = _uiState.value.copy(showLogoutDialog = true)
    }
    
    /**
     * Hide logout confirmation dialog
     */
    fun hideLogoutDialog() {
        _uiState.value = _uiState.value.copy(showLogoutDialog = false)
    }
    
    /**
     * Perform logout
     */
    fun logout() {
        viewModelScope.launch {
            logger.d(Logger.TAG_DEFAULT, "Performing logout")
            FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                // Best-effort cleanup before auth state is invalidated.
                notificationTokenHelper.removeTokenForUser(uid)
            }

            when (val result = logoutUseCase()) {
                is Result.Success -> {
                    logger.i(Logger.TAG_DEFAULT, "Logout successful")
                    hideLogoutDialog()
                    _events.send(SettingsEvent.NavigateToLogin)
                }
                is Result.Error -> {
                    logger.e(Logger.TAG_DEFAULT, "Logout failed", result.exception)
                    _events.send(SettingsEvent.ShowError(R.string.settings_logout_failed))
                }
                is Result.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }
    
    /**
     * Toggle notifications
     */
    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            logger.d(Logger.TAG_DEFAULT, "Toggling notifications: $enabled")
            
            val currentPreferences = _uiState.value.userPreferences
            val updatedNotifications = currentPreferences.notificationSettings.copy(
                pushNotificationsEnabled = enabled
            )
            val updatedPreferences = currentPreferences.copy(
                notificationSettings = updatedNotifications
            )
            
            when (val result = updateUserPreferencesUseCase(updatedPreferences)) {
                is Result.Success -> {
                    logger.i(Logger.TAG_DEFAULT, "Notifications updated successfully")
                    if (enabled) {
                        notificationTokenHelper.initialize()
                    } else {
                        notificationTokenHelper.removeToken()
                        notificationTokenHelper.unsubscribeFromAllTopics()
                    }
                    _events.send(
                        SettingsEvent.ShowSuccess(
                            if (enabled) R.string.settings_notifications_enabled
                            else R.string.settings_notifications_disabled
                        )
                    )
                }
                is Result.Error -> {
                    logger.e(Logger.TAG_DEFAULT, "Failed to update notifications", result.exception)
                    _events.send(SettingsEvent.ShowError(R.string.settings_update_failed))
                }
                is Result.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }
}