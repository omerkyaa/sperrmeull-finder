package com.omerkaya.sperrmuellfinder.ui

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.omerkaya.sperrmuellfinder.BuildConfig
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.core.navigation.NavigationManager
import com.omerkaya.sperrmuellfinder.core.security.BanGuard
import com.omerkaya.sperrmuellfinder.core.security.BanGuardResult
import com.omerkaya.sperrmuellfinder.core.ui.theme.SperrmullFinderTheme
import com.omerkaya.sperrmuellfinder.core.ui.theme.ThemeMode
import com.omerkaya.sperrmuellfinder.domain.model.AppTheme
import com.omerkaya.sperrmuellfinder.domain.model.UserPreferences
import com.omerkaya.sperrmuellfinder.domain.model.auth.AuthState
import com.omerkaya.sperrmuellfinder.domain.repository.PremiumRepository
import com.omerkaya.sperrmuellfinder.domain.repository.SettingsRepository
import com.omerkaya.sperrmuellfinder.ui.auth.AuthNavHost
import com.omerkaya.sperrmuellfinder.ui.ban.BannedScreen
import com.omerkaya.sperrmuellfinder.ui.auth.AuthViewModel
import com.omerkaya.sperrmuellfinder.ui.main.MainScreen
import com.omerkaya.sperrmuellfinder.data.messaging.NotificationTokenHelper
import com.omerkaya.sperrmuellfinder.manager.OneSignalManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        private const val PERMISSION_PREFS = "permission_prefs"
        private const val KEY_NOTIFICATION_PERMISSION_ASKED = "notification_permission_asked"
    }
    
    private val authViewModel: AuthViewModel by viewModels()
    private val banGuard = BanGuard()
    private var showBannedScreen by mutableStateOf(false)
    private var currentBanReason by mutableStateOf<String?>(null)
    private var pendingDeepLink by mutableStateOf<String?>(null)
    private var lastTokenRegisteredUid: String? = null
    private val permissionPrefs by lazy { getSharedPreferences(PERMISSION_PREFS, MODE_PRIVATE) }
    
    @Inject
    lateinit var navigationManager: NavigationManager

    @Inject
    lateinit var notificationTokenHelper: NotificationTokenHelper

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var oneSignalManager: OneSignalManager

    @Inject
    lateinit var premiumRepository: PremiumRepository

    private val notificationsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionPrefs.edit().putBoolean(KEY_NOTIFICATION_PERMISSION_ASKED, true).apply()
        if (granted) {
            lifecycleScope.launch { notificationTokenHelper.initialize() }
        } else if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)
        ) {
            showNotificationSettingsDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge
        enableEdgeToEdge()
        
        setContent {
            val userPreferences by settingsRepository.getUserPreferences()
                .collectAsState(initial = UserPreferences())
            val selectedThemeMode = when (userPreferences.theme) {
                AppTheme.LIGHT -> ThemeMode.LIGHT
                AppTheme.DARK -> ThemeMode.DARK
                AppTheme.SYSTEM -> ThemeMode.SYSTEM
            }
            val selectedLanguageTag = userPreferences.language.code

            LaunchedEffect(selectedLanguageTag) {
                val targetLocales = LocaleListCompat.forLanguageTags(selectedLanguageTag)
                if (AppCompatDelegate.getApplicationLocales() != targetLocales) {
                    AppCompatDelegate.setApplicationLocales(targetLocales)
                }
            }

            SperrmullFinderTheme(
                themeMode = selectedThemeMode,
                dynamicColor = false
            ) {
                // Fix @Composable context by wrapping in proper Composable
                MainContent(authViewModel = authViewModel)
            }
        }

        extractDeepLinkFromIntent(intent)
        oneSignalManager.bindActivity(this)
        startTokenPipeline()
        runBanGuard()
    }

    override fun onResume() {
        super.onResume()
        runBanGuard()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractDeepLinkFromIntent(intent)
    }

    override fun onDestroy() {
        oneSignalManager.unbindActivity(this)
        super.onDestroy()
    }
    
    @Composable
    private fun MainContent(authViewModel: AuthViewModel) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val authState by authViewModel.authState.collectAsState()
            
            // Fix smart cast by using local variable
            val currentAuthState = authState

            if (showBannedScreen) {
                BannedScreen(
                    banReason = currentBanReason,
                    onAcknowledge = {
                        // Keep user logged out and return to auth flow.
                        showBannedScreen = false
                    },
                    onContactSupport = {
                        Toast.makeText(this, getString(com.omerkaya.sperrmuellfinder.R.string.msg_login_blocked), Toast.LENGTH_LONG).show()
                    }
                )
                return@Surface
            }
            
            when (currentAuthState) {
                is AuthState.Loading -> {
                    // Show loading state - Android splash screen handles initial loading
                    // This will quickly transition to auth state
                }
                is AuthState.Authenticated -> {
                    // User is logged in - go directly to home
                    // For now, show a placeholder until MainNavHost is implemented
                    AuthenticatedContent(authState = currentAuthState)
                }
                is AuthState.Unauthenticated -> {
                    // Show authentication flow
                    AuthNavHost(
                        onNavigateToHome = {
                            // This will be triggered when auth is successful
                            // The AuthState will change to Authenticated
                        },
                        onNavigateToBanned = {
                            Toast.makeText(this, getString(com.omerkaya.sperrmuellfinder.R.string.btn_contact_support), Toast.LENGTH_LONG).show()
                        },
                        onGoogleSignIn = {
                            // TODO: Implement Google Sign-In
                        }
                    )
                }
                is AuthState.Error -> {
                    // Show error screen or fallback to auth
                    AuthNavHost(
                        onNavigateToHome = {
                            // This will be triggered when auth is successful
                        },
                        onNavigateToBanned = {
                            Toast.makeText(this, getString(com.omerkaya.sperrmuellfinder.R.string.btn_contact_support), Toast.LENGTH_LONG).show()
                        },
                        onGoogleSignIn = {
                            // TODO: Implement Google Sign-In
                        }
                    )
                }
            }
        }
    }
    
    @Composable
    private fun AuthenticatedContent(authState: AuthState.Authenticated) {
        // Show main app with bottom navigation
        MainScreen(
            onNavigateToPostDetail = { postId ->
                navigationManager.navigateToPostDetail(postId)
            },
            onNavigateToUserProfile = { userId ->
                navigationManager.navigateToUserProfile(userId)
            },
            onNavigateToComments = { postId ->
                // TODO: Navigate to comments
            },
            onNavigateToNotifications = {
                // TODO: Navigate to notifications
            },
            onNavigateToLikes = { postId ->
                // TODO: Navigate to likes
            },
            onNavigateToPremium = {
                navigationManager.navigateToPremium()
            },
            navigationManager = navigationManager
        )

        LaunchedEffect(pendingDeepLink) {
            pendingDeepLink?.let { deepLink ->
                navigationManager.handleDeepLink(deepLink)
                pendingDeepLink = null
            }
        }
    }

    private fun runBanGuard() {
        lifecycleScope.launch {
            when (val result = banGuard.checkAndEnforceBan()) {
                is BanGuardResult.Allowed -> {
                    // no-op
                }
                is BanGuardResult.Banned -> {
                    currentBanReason = result.reason
                    showBannedScreen = true
                }
            }
        }
    }

    private fun startTokenPipeline() {
        lifecycleScope.launch {
            authViewModel.authState.collectLatest { state ->
                if (state is AuthState.Authenticated) {
                    val currentUid = state.user.uid
                    premiumRepository.initializeWithUser(currentUid)
                    oneSignalManager.login(currentUid)
                    oneSignalManager.setTags(
                        mapOf(
                            "platform" to "android",
                            "app_version" to BuildConfig.VERSION_NAME
                        )
                    )
                    state.user.email?.let { oneSignalManager.setEmail(it) }
                    if (lastTokenRegisteredUid != currentUid) {
                        ensureNotificationPermissionAndRegisterToken()
                        lastTokenRegisteredUid = currentUid
                    }
                } else {
                    premiumRepository.cleanup()
                    oneSignalManager.logout()
                    lastTokenRegisteredUid = null
                }
            }
        }
    }

    private fun ensureNotificationPermissionAndRegisterToken() {
        lifecycleScope.launch {
            val pushEnabled = settingsRepository.getUserPreferences()
                .first()
                .notificationSettings
                .pushNotificationsEnabled

            if (!pushEnabled) {
                notificationTokenHelper.removeToken()
                return@launch
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                val askedBefore = permissionPrefs.getBoolean(KEY_NOTIFICATION_PERMISSION_ASKED, false)
                if (!askedBefore) {
                    notificationsPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                } else if (!shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
                    showNotificationSettingsDialog()
                }
                return@launch
            }

            notificationTokenHelper.initialize()
        }
    }

    private fun showNotificationSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.notification_permission_settings_title))
            .setMessage(getString(R.string.notification_permission_settings_message))
            .setPositiveButton(getString(R.string.permission_settings)) { _, _ ->
                openAppSettings()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun extractDeepLinkFromIntent(incomingIntent: android.content.Intent?) {
        val explicitDeepLink = incomingIntent?.getStringExtra("deepLink")
        if (!explicitDeepLink.isNullOrBlank()) {
            pendingDeepLink = explicitDeepLink
            return
        }

        val deepLinkType = incomingIntent?.getStringExtra("deep_link_type")
        val postId = incomingIntent?.getStringExtra("post_id")
        val userId = incomingIntent?.getStringExtra("user_id")

        pendingDeepLink = when (deepLinkType) {
            "like", "comment", "premium_nearby_post", "post_detail" -> postId?.let { "post:$it" }
            "user_profile", "follow" -> userId?.let { "profile:$it" }
            "notifications" -> "notifications"
            else -> null
        }
    }
}
