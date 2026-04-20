package com.omerkaya.sperrmuellfinder.core.navigation

import androidx.compose.runtime.Stable
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.omerkaya.sperrmuellfinder.core.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Navigation manager for handling app-wide navigation state and operations.
 * Provides centralized navigation control with state preservation and analytics.
 */
@Singleton
@Stable
class NavigationManager @Inject constructor(
    private val logger: Logger
) {
    private var navController: NavHostController? = null
    
    private val _navigationState = MutableStateFlow(NavigationState())
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()
    
    private val _currentDestination = MutableStateFlow<BottomNavDestination>(BottomNavDestination.Home)
    val currentDestination: StateFlow<BottomNavDestination> = _currentDestination.asStateFlow()
    
    private val _canNavigateBack = MutableStateFlow(false)
    val canNavigateBack: StateFlow<Boolean> = _canNavigateBack.asStateFlow()

    /**
     * Sets the navigation controller instance.
     */
    fun setNavController(navController: NavHostController) {
        this.navController = navController
        logger.d(Logger.TAG_DEFAULT, "Navigation controller set")
        
        // CRASH FIX: Update navigation state when controller is set
        updateCanNavigateBack()
    }

    /**
     * Navigates to a bottom navigation destination.
     */
    fun navigateToBottomNavDestination(destination: BottomNavDestination) {
        navController?.let { controller ->
            logger.d(Logger.TAG_DEFAULT, "Navigating to bottom nav destination: ${destination.route}")
            
            // Update current destination
            _currentDestination.value = destination
            
            // Save current state before navigation
            saveCurrentState()
            
            // Navigate with proper back stack management for bottom nav
            controller.navigate(destination.route) {
                // Use graph start destination for robust tab switching across all stacks.
                popUpTo(controller.graph.findStartDestination().id) {
                    saveState = true
                }
                // Avoid multiple copies of the same destination
                launchSingleTop = true
                // Restore state when reselecting a previously selected item
                restoreState = true
            }
            
            // Track analytics
            trackNavigation(NavigationAnalytics.TabSelected(destination.route))
        } ?: run {
            logger.e(Logger.TAG_DEFAULT, "Cannot navigate - NavController is null")
        }
    }

    /**
     * Navigates to a specific route.
     */
    fun navigateTo(route: String) {
        navController?.let { controller ->
            logger.d(Logger.TAG_DEFAULT, "Navigating to route: $route")
            
            controller.navigate(route)
            updateCanNavigateBack()
            
            // Track analytics
            trackNavigation(NavigationAnalytics.ScreenViewed(route))
        } ?: run {
            logger.e(Logger.TAG_DEFAULT, "Cannot navigate to $route - NavController is null")
        }
    }

    /**
     * Navigates to post detail screen.
     */
    fun navigateToPostDetail(postId: String) {
        val route = TopLevelDestination.PostDetail.createRoute(postId)
        navigateTo(route)
    }



    /**
     * Navigates to user profile screen.
     */
    fun navigateToUserProfile(userId: String) {
        val route = TopLevelDestination.UserProfile.createRoute(userId)
        navigateTo(route)
    }

    /**
     * Navigates to comments screen.
     */
    fun navigateToComments(postId: String) {
        val route = TopLevelDestination.Comments.createRoute(postId)
        navigateTo(route)
    }

    /**
     * Navigates to premium screen.
     */
    fun navigateToPremium() {
        navigateTo(TopLevelDestination.Premium.route)
    }

    /**
     * Navigates to settings screen.
     */
    fun navigateToSettings() {
        navigateTo(TopLevelDestination.Settings.route)
    }

    /**
     * Navigates to notifications screen.
     */
    fun navigateToNotifications() {
        navigateTo(TopLevelDestination.Notifications.route)
    }

    /**
     * Navigates to edit profile screen.
     */
    fun navigateToEditProfile() {
        navigateTo(TopLevelDestination.EditProfile.route)
    }

    /**
     * Navigates back in the navigation stack.
     */
    fun navigateBack() {
        navController?.let { controller ->
            if (controller.previousBackStackEntry != null) {
                logger.d(Logger.TAG_DEFAULT, "Navigating back")
                controller.popBackStack()
                updateCanNavigateBack()
            } else {
                logger.w(Logger.TAG_DEFAULT, "Cannot navigate back - no previous entry")
            }
        } ?: run {
            logger.e(Logger.TAG_DEFAULT, "Cannot navigate back - NavController is null")
        }
    }

    /**
     * Handles deep link navigation.
     */
    fun handleDeepLink(uri: String) {
        navController?.let { controller ->
            logger.i(Logger.TAG_DEFAULT, "Handling deep link: $uri")
            
            try {
                // Parse and navigate to deep link
                when {
                    uri.contains("/post/") -> {
                        val postId = uri.substringAfterLast("/")
                        navigateToPostDetail(postId)
                        trackNavigation(NavigationAnalytics.DeepLinkOpened(uri, "external"))
                    }
                    uri.contains("/user/") -> {
                        val userId = uri.substringAfterLast("/")
                        navigateToUserProfile(userId)
                        trackNavigation(NavigationAnalytics.DeepLinkOpened(uri, "external"))
                    }
                    uri.contains("/search") -> {
                        navigateToBottomNavDestination(BottomNavDestination.Search)
                        trackNavigation(NavigationAnalytics.DeepLinkOpened(uri, "external"))
                    }
                    uri.contains("/map") -> {
                        navigateToBottomNavDestination(BottomNavDestination.Map)
                        trackNavigation(NavigationAnalytics.DeepLinkOpened(uri, "external"))
                    }
                    uri.contains("/premium") -> {
                        navigateToPremium()
                        trackNavigation(NavigationAnalytics.DeepLinkOpened(uri, "external"))
                    }
                    else -> {
                        logger.w(Logger.TAG_DEFAULT, "Unknown deep link pattern: $uri")
                    }
                }
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "Error handling deep link: $uri", e)
            }
        } ?: run {
            logger.e(Logger.TAG_DEFAULT, "Cannot handle deep link - NavController is null")
        }
    }

    /**
     * Saves the current navigation state.
     */
    fun saveCurrentState() {
        val currentState = _navigationState.value
        val updatedState = currentState.copy(
            currentDestination = _currentDestination.value
        )
        _navigationState.value = updatedState
        
        logger.d(Logger.TAG_DEFAULT, "Navigation state saved: ${updatedState.currentDestination.route}")
    }

    /**
     * Restores the navigation state.
     */
    fun restoreState(state: NavigationState) {
        _navigationState.value = state
        _currentDestination.value = state.currentDestination
        
        logger.d(Logger.TAG_DEFAULT, "Navigation state restored: ${state.currentDestination.route}")
    }

    /**
     * Updates the scroll position for home screen.
     */
    fun updateHomeScrollPosition(position: Int) {
        val currentState = _navigationState.value
        _navigationState.value = currentState.copy(homeScrollPosition = position)
    }

    /**
     * Updates the search query.
     */
    fun updateSearchQuery(query: String) {
        val currentState = _navigationState.value
        _navigationState.value = currentState.copy(searchQuery = query)
    }

    /**
     * Updates the map camera position.
     */
    fun updateMapCameraPosition(latitude: Double, longitude: Double, zoom: Float) {
        val currentState = _navigationState.value
        val cameraState = MapCameraState(latitude, longitude, zoom)
        _navigationState.value = currentState.copy(mapCameraPosition = cameraState)
    }

    /**
     * Updates the profile scroll position.
     */
    fun updateProfileScrollPosition(position: Int) {
        val currentState = _navigationState.value
        _navigationState.value = currentState.copy(profileScrollPosition = position)
    }

    /**
     * Checks if premium is required for navigation.
     */
    fun requiresPremiumAccess(destination: BottomNavDestination): Boolean {
        return destination.requiresPremium
    }

    /**
     * Handles premium gate hit for analytics.
     */
    fun trackPremiumGateHit(feature: String) {
        val currentTab = _currentDestination.value.route
        trackNavigation(NavigationAnalytics.PremiumGateHit(feature, currentTab))
    }



    /**
     * Tracks navigation analytics.
     */
    private fun trackNavigation(analytics: NavigationAnalytics) {
        logger.d(Logger.TAG_DEFAULT, "Navigation analytics: ${analytics.eventName} - ${analytics.parameters}")
        
        // TODO: Implement actual analytics tracking
        // Analytics.track(analytics.eventName, analytics.parameters)
    }

    /**
     * Clears the navigation state.
     */
    fun clearState() {
        _navigationState.value = NavigationState()
        _currentDestination.value = BottomNavDestination.Home
        _canNavigateBack.value = false
        
        logger.d(Logger.TAG_DEFAULT, "Navigation state cleared")
    }

    /**
     * Gets the current route from the nav controller.
     */
    fun getCurrentRoute(): String? {
        return navController?.currentBackStackEntry?.destination?.route
    }

    /**
     * Checks if a route is a bottom navigation destination.
     * Handles nullable routes safely for navigation state checks.
     * CRASH FIX: Added null safety for destinations list and route access.
     */
    fun isBottomNavDestination(route: String?): Boolean {
        return try {
            if (route.isNullOrBlank()) {
                false
            } else {
                BottomNavDestination.destinations
                    .filterNotNull() // CRASH FIX: Remove any null destinations
                    .any { destination ->
                        // CRASH FIX: Safe access to route property
                        runCatching { destination.route == route }.getOrDefault(false)
                    }
            }
        } catch (e: Exception) {
            // CRASH FIX: Fallback to false if any error occurs
            logger.e(Logger.TAG_DEFAULT, "Error checking bottom nav destination for route: $route", e)
            false
        }
    }

    /**
     * Updates can navigate back state.
     */
    private fun updateCanNavigateBack() {
        navController?.let { controller ->
            _canNavigateBack.value = controller.previousBackStackEntry != null
        }
    }
}