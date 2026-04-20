package com.omerkaya.sperrmuellfinder.core.navigation

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.vector.ImageVector
import android.util.Log

/**
 * Navigation destinations for the bottom navigation bar.
 * Defines all main screens accessible via bottom navigation.
 * UPDATED: Using custom drawable icons for professional branding.
 */
sealed class BottomNavDestination(
    val route: String,
    val titleResId: Int,
    @DrawableRes val iconRes: Int,
    val requiresPremium: Boolean = false,
    val showPremiumIndicator: Boolean = false
) {
    data object Home : BottomNavDestination(
        route = "home",
        titleResId = com.omerkaya.sperrmuellfinder.core.R.string.nav_home,
        iconRes = 0 // Will be resolved in UI layer
    )

    data object Search : BottomNavDestination(
        route = "search",
        titleResId = com.omerkaya.sperrmuellfinder.core.R.string.nav_search,
        iconRes = 0, // Will be resolved in UI layer
        requiresPremium = true,
        showPremiumIndicator = true
    )

    data object Map : BottomNavDestination(
        route = "map",
        titleResId = com.omerkaya.sperrmuellfinder.core.R.string.nav_map,
        iconRes = 0 // Will be resolved in UI layer
    )

    data object Camera : BottomNavDestination(
        route = "camera",
        titleResId = com.omerkaya.sperrmuellfinder.core.R.string.nav_camera,
        iconRes = 0 // Will be resolved in UI layer
    )

    data object Profile : BottomNavDestination(
        route = "profile",
        titleResId = com.omerkaya.sperrmuellfinder.core.R.string.nav_profile,
        iconRes = 0 // Will be resolved in UI layer
    )

    companion object {
        val destinations = listOf(Home, Search, Camera, Map, Profile)

        private fun safeLogDebug(message: String) {
            try {
                Log.d("Navigation", message)
            } catch (_: Throwable) {
                // Ignore local JVM test environments without android.util.Log implementation.
            }
        }

        private fun safeLogWarn(message: String, throwable: Throwable? = null) {
            try {
                if (throwable == null) {
                    Log.w("Navigation", message)
                } else {
                    Log.w("Navigation", message, throwable)
                }
            } catch (_: Throwable) {
                // Ignore local JVM test environments without android.util.Log implementation.
            }
        }

        private fun safeLogError(message: String, throwable: Throwable? = null) {
            try {
                if (throwable == null) {
                    Log.e("Navigation", message)
                } else {
                    Log.e("Navigation", message, throwable)
                }
            } catch (_: Throwable) {
                // Ignore local JVM test environments without android.util.Log implementation.
            }
        }
        
        /**
         * CRASH FIX: Bulletproof route resolution with comprehensive logging
         * Never returns null - always provides a valid destination
         */
        fun fromRoute(route: String?): BottomNavDestination {
            return try {
                when {
                    route.isNullOrBlank() -> {
                        safeLogDebug("Empty route provided, defaulting to Home")
                        Home
                    }
                    else -> {
                        // Safe route comparison with null protection
                        val destination = destinations.find { dest ->
                            try {
                                dest?.route == route
                            } catch (e: Exception) {
                                safeLogWarn("Error comparing route for destination", e)
                                false
                            }
                        }
                        
                        if (destination != null) {
                            safeLogDebug("Found destination for route: $route")
                            destination
                        } else {
                            safeLogWarn("Unknown route: $route, defaulting to Home")
                            Home
                        }
                    }
                }
            } catch (e: Exception) {
                safeLogError("Critical error in fromRoute for: $route", e)
                Home // CRITICAL: Never crash - always return Home as ultimate fallback
            }
        }
        
        /**
         * CRASH FIX: Safe destination resolution with premium validation
         * Ensures user can't access premium features without subscription
         */
        fun getSafeDestination(
            requestedDestination: BottomNavDestination?, 
            isPremium: Boolean
        ): BottomNavDestination {
            return try {
                val destination = requestedDestination ?: Home
                
                // Premium access validation
                when {
                    destination.requiresPremium && !isPremium -> {
                        safeLogWarn("Premium required for ${destination.route}, redirecting to Home")
                        Home
                    }
                    else -> {
                        safeLogDebug("Access granted to ${destination.route}")
                        destination
                    }
                }
            } catch (e: Exception) {
                safeLogError("Error in getSafeDestination", e)
                Home // CRITICAL: Always fallback to Home
            }
        }
        
        fun getAvailableDestinations(isPremium: Boolean): List<BottomNavDestination> {
            return listOf(
                Home,
                Search,
                Camera,
                Map,
                Profile
            ).filter { !it.requiresPremium || isPremium }
        }
    }
}

/**
 * Top level navigation destinations that can be accessed via deep links.
 */
sealed class TopLevelDestination(val route: String) {
    // Auth flow
    data object Auth : TopLevelDestination("auth")
    data object Login : TopLevelDestination("login")
    data object Register : TopLevelDestination("register")
    data object ForgotPassword : TopLevelDestination("forgot_password")
    
    // Onboarding flow
    data object Splash : TopLevelDestination("splash")
    data object Onboarding : TopLevelDestination("onboarding")
    data object InitialSetup : TopLevelDestination("initial_setup")
    
    // Main app flow
    data object Main : TopLevelDestination("main")
    
    // Detail screens
    data object PostDetail : TopLevelDestination("post_detail/{postId}") {
        fun createRoute(postId: String) = "post_detail/$postId"
    }
    
    data object UserProfile : TopLevelDestination("user_profile/{userId}") {
        fun createRoute(userId: String) = "user_profile/$userId"
    }
    
    data object Comments : TopLevelDestination("comments/{postId}") {
        fun createRoute(postId: String) = "comments/$postId"
    }
    
    // Premium flow
    data object Premium : TopLevelDestination("premium")
    data object PurchaseSuccess : TopLevelDestination("purchase_success")
    
    // Settings and other screens
    data object Settings : TopLevelDestination("settings")
    data object Notifications : TopLevelDestination("notifications")
    data object EditProfile : TopLevelDestination("edit_profile")
    
    companion object {
        const val POST_ID_ARG = "postId"
        const val USER_ID_ARG = "userId"
    }
}


/**
 * Navigation state for preserving tab states.
 */
data class NavigationState(
    val currentDestination: BottomNavDestination = BottomNavDestination.Home,
    val homeScrollPosition: Int = 0,
    val searchQuery: String = "",
    val mapCameraPosition: MapCameraState? = null,
    val profileScrollPosition: Int = 0
)

/**
 * Map camera state for preserving map position.
 */
data class MapCameraState(
    val latitude: Double,
    val longitude: Double,
    val zoom: Float = 15f
)

/**
 * Navigation events that can be triggered from ViewModels.
 */
sealed class NavigationEvent {
    data object NavigateUp : NavigationEvent()
    data class NavigateTo(val destination: String) : NavigationEvent()
    data class NavigateToPostDetail(val postId: String) : NavigationEvent()
    data class NavigateToUserProfile(val userId: String) : NavigationEvent()
    data class NavigateToComments(val postId: String) : NavigationEvent()
    data object NavigateToPremium : NavigationEvent()
    data object NavigateToSettings : NavigationEvent()
    data object NavigateToNotifications : NavigationEvent()
    data object NavigateToEditProfile : NavigationEvent()
}

/**
 * Deep link configuration for the app.
 */
object DeepLinks {
    const val SCHEME = "sperrmuellfinder"
    const val HOST = "app"
    
    // Deep link patterns
    const val POST_DETAIL = "$SCHEME://$HOST/post/{${TopLevelDestination.POST_ID_ARG}}"
    const val USER_PROFILE = "$SCHEME://$HOST/user/{${TopLevelDestination.USER_ID_ARG}}"
    const val SEARCH = "$SCHEME://$HOST/search"
    const val MAP = "$SCHEME://$HOST/map"
    const val PREMIUM = "$SCHEME://$HOST/premium"
    
    /**
     * Creates a deep link for a post detail.
     */
    fun createPostDetailLink(postId: String): String {
        return "$SCHEME://$HOST/post/$postId"
    }
    
    /**
     * Creates a deep link for a user profile.
     */
    fun createUserProfileLink(userId: String): String {
        return "$SCHEME://$HOST/user/$userId"
    }
    
    /**
     * Creates a shareable link for a post with fallback to web.
     * This link will open the app if installed, otherwise redirect to Play Store.
     */
    fun createShareablePostLink(postId: String): String {
        return "https://sperrmuellfinder.app/post/$postId"
    }
    
    /**
     * Creates share text for a post.
     */
    fun createPostShareText(
        postDescription: String, 
        postCity: String,
        shareableLink: String,
        appStoreLink: String
    ): String {
        val shortDescription = if (postDescription.length > 100) {
            postDescription.take(97) + "..."
        } else {
            postDescription
        }
        
        return "Ich habe diesen interessanten Gegenstand in $postCity auf SperrmüllFinder gefunden:\n\n" +
                "\"$shortDescription\"\n\n" +
                "Schau es dir hier an: $shareableLink\n\n" +
                "App herunterladen: $appStoreLink"
    }
}

/**
 * Navigation analytics events.
 */
sealed class NavigationAnalytics(val eventName: String, val parameters: Map<String, Any> = emptyMap()) {
    data class TabSelected(val tab: String) : NavigationAnalytics(
        eventName = "tab_selected",
        parameters = mapOf("tab_name" to tab)
    )
    
    data class ScreenViewed(val screen: String, val source: String? = null) : NavigationAnalytics(
        eventName = "screen_viewed",
        parameters = buildMap {
            put("screen_name", screen)
            source?.let { put("source", it) }
        }
    )
    
    data class DeepLinkOpened(val link: String, val source: String) : NavigationAnalytics(
        eventName = "deep_link_opened",
        parameters = mapOf(
            "link" to link,
            "source" to source
        )
    )
    
    data class PremiumGateHit(val feature: String, val tab: String) : NavigationAnalytics(
        eventName = "premium_gate_hit",
        parameters = mapOf(
            "feature" to feature,
            "tab" to tab
        )
    )
}
