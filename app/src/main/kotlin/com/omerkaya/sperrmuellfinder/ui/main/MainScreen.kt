package com.omerkaya.sperrmuellfinder.ui.main

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import androidx.activity.compose.BackHandler
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.core.navigation.BottomNavDestination
import com.omerkaya.sperrmuellfinder.core.navigation.NavigationManager
import com.omerkaya.sperrmuellfinder.ui.home.HomeScreen
import com.omerkaya.sperrmuellfinder.ui.navigation.BottomNavigationBar
import com.omerkaya.sperrmuellfinder.ui.navigation.navigateToFollowers
import com.omerkaya.sperrmuellfinder.ui.navigation.navigateToFollowing
import com.omerkaya.sperrmuellfinder.ui.navigation.navigateToUserProfile
import com.omerkaya.sperrmuellfinder.ui.navigation.navigateToLikes
import com.omerkaya.sperrmuellfinder.ui.navigation.navigateToComments
import com.omerkaya.sperrmuellfinder.ui.navigation.navigateToDeleteAccount
import com.omerkaya.sperrmuellfinder.ui.navigation.navigateToNotifications
import com.omerkaya.sperrmuellfinder.ui.navigation.AppDestinations
import com.omerkaya.sperrmuellfinder.ui.notifications.NotificationsScreen
import com.omerkaya.sperrmuellfinder.ui.search.SearchScreen

/**
 * Main screen container with bottom navigation.
 * Manages the primary app navigation and state preservation.
 */
@Composable
fun MainScreen(
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    onNavigateToComments: (String) -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToLikes: (String) -> Unit,
    onNavigateToPremium: () -> Unit,
    navigationManager: NavigationManager,
    viewModel: MainViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    
    // Navigation callbacks using NavController extensions
    val navigateToNotifications = { navController.navigateToNotifications() }
    val navigateToLikes = { postId: String -> navController.navigateToLikes(postId) }
    val navigateToComments = { postId: String -> navController.navigateToComments(postId) }
    
    val uiState by viewModel.uiState.collectAsState()
    val currentDestination by viewModel.currentDestination.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val activity = context as? Activity
    
    // Set up navigation manager
    LaunchedEffect(navController) {
        navigationManager.setNavController(navController)
    }
    
    // CRITICAL CRASH FIX: Safe event handling with flow error recovery
    LaunchedEffect(Unit) {
        viewModel.events
            .catch { exception ->
                Log.e("MainScreen", "Critical error in events flow", exception)
                // Don't crash - continue with app functionality
            }
            .collect { event ->
                try {
                    Log.d("MainScreen", "Processing event: ${event::class.simpleName}")
                    
                    when (event) {
                        is MainEvent.NavigateToPostDetail -> {
                            runCatching { onNavigateToPostDetail(event.postId) }
                                .onFailure { Log.e("MainScreen", "Navigation to post detail failed", it) }
                        }
                        is MainEvent.NavigateToUserProfile -> {
                            runCatching { onNavigateToUserProfile(event.userId) }
                                .onFailure { Log.e("MainScreen", "Navigation to user profile failed", it) }
                        }
                        is MainEvent.NavigateToComments -> {
                            runCatching { onNavigateToComments(event.postId) }
                                .onFailure { Log.e("MainScreen", "Navigation to comments failed", it) }
                        }
                        is MainEvent.NavigateToPremium -> {
                            runCatching { onNavigateToPremium() }
                                .onFailure { Log.e("MainScreen", "Navigation to premium failed", it) }
                        }
                        is MainEvent.ShowMessage -> {
                            runCatching { 
                                snackbarHostState.showSnackbar(event.message) 
                            }.onFailure { Log.e("MainScreen", "Failed to show message", it) }
                        }
                        is MainEvent.ShowPremiumRequired -> {
                            runCatching { 
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.premium_required_message)
                                )
                            }.onFailure { Log.e("MainScreen", "Failed to show premium message", it) }
                        }
                        is MainEvent.PostCreated -> {
                            // Navigate to home and trigger refresh across all relevant ViewModels
                            Log.d("MainScreen", "Post created - navigating to home and triggering refresh")
                            runCatching {
                                navController.navigate(BottomNavDestination.Home.route) {
                                    popUpTo(BottomNavDestination.Home.route) {
                                        inclusive = true
                                    }
                                }
                                Log.d("MainScreen", "✅ Successfully navigated to Home after post creation")
                            }.onFailure { Log.e("MainScreen", "Failed to navigate to home after post creation", it) }
                            // The refresh will be handled by individual ViewModels listening to data changes
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainScreen", "Unexpected error processing event: ${event::class.simpleName}", e)
                    // Continue processing other events
                }
            }
    }
    
    // CRITICAL CRASH FIX: Bulletproof navigation flow with comprehensive error handling
    LaunchedEffect(navBackStackEntry, uiState.isPremium) {
        try {
            // Safe route extraction with multiple fallback layers
            val route = runCatching {
                navBackStackEntry?.destination?.route
            }.getOrElse { exception ->
                Log.e("MainScreen", "Failed to get route from navBackStackEntry", exception)
                null 
            }
            
            Log.d("MainScreen", "Processing navigation to route: $route")
            
            // Only process bottom navigation routes - skip detail screens
            if (route != null && isBottomNavRoute(route)) {
                // Safe destination resolution with extra null checks
                val destination = try {
                    BottomNavDestination.fromRoute(route)
                } catch (e: Exception) {
                    Log.e("MainScreen", "Critical error in fromRoute, defaulting to Home", e)
                    BottomNavDestination.Home
                }
                
                // Premium-aware destination validation with safety checks
                val safeDestination = try {
                    BottomNavDestination.getSafeDestination(destination, uiState.isPremium)
                } catch (e: Exception) {
                    Log.e("MainScreen", "Error in getSafeDestination, defaulting to Home", e)
                    BottomNavDestination.Home
                }
                
                Log.d("MainScreen", "Final destination: ${safeDestination.route}")
                
                // Safe ViewModel update with error handling
                runCatching {
                    viewModel.updateCurrentDestination(safeDestination)
                }.onFailure { error ->
                    Log.e("MainScreen", "Failed to update current destination", error)
                    // Continue execution - don't crash the app
                }
            } else {
                Log.d("MainScreen", "Skipping non-bottom-nav route: $route")
            }
            
        } catch (e: Exception) {
            Log.e("MainScreen", "Critical error in navigation LaunchedEffect", e)
            // CRITICAL: Ensure we always have a valid destination even if everything fails
            runCatching {
                viewModel.updateCurrentDestination(BottomNavDestination.Home)
            }.onFailure { fallbackError ->
                Log.e("MainScreen", "Even fallback destination update failed", fallbackError)
            }
        }
    }

    // Global back behavior:
    // 1) If on Home -> exit app
    // 2) Else if there is history -> go one screen back
    // 3) Else -> navigate to Home as final fallback
    BackHandler {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        when {
            currentRoute == BottomNavDestination.Home.route -> {
                activity?.finish()
            }
            navController.previousBackStackEntry != null -> {
                navController.popBackStack()
            }
            else -> {
                navController.navigate(BottomNavDestination.Home.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
                viewModel.updateCurrentDestination(BottomNavDestination.Home)
            }
        }
    }
    
    // 🎯 ALWAYS SHOW MAIN UI WITH BOTTOM NAV: No more blocking screens
    Scaffold(
        bottomBar = {
            // 🎯 ALWAYS SHOW BOTTOM NAV: Fixed position at bottom, always visible
            BottomNavigationBar(
                currentDestination = currentDestination,
                isPremium = uiState.isPremium,
                onDestinationSelected = { destination ->
                    viewModel.onDestinationSelected(destination, navigationManager)
                },
                notificationCount = uiState.notificationCount
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        // Show content based on state, but never hide the navigation
        when {
            uiState.isInitializing -> {
                // Show loading in content area, but keep navigation visible
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    DefensiveLoadingScreen(
                        phase = uiState.startupPhase,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            uiState.startupPhase == StartupPhase.Error && !uiState.hasMinimumDataLoaded -> {
                // Show error in content area, but keep navigation visible
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    DefensiveErrorScreen(
                        error = uiState.error,
                        onRetry = { viewModel.clearError() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            else -> {
                // Show main navigation content
                MainNavHost(
                    navController = navController,
                    onNavigateToPostDetail = onNavigateToPostDetail,
                    onNavigateToUserProfile = onNavigateToUserProfile,
                    onNavigateToComments = onNavigateToComments,
                    onNavigateToPremium = onNavigateToPremium,
                    navigationManager = navigationManager,
                    viewModel = viewModel,
                    paddingValues = paddingValues
                )
            }
        }
    }
}

@Composable
private fun MainNavHost(
    navController: NavHostController,
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    onNavigateToComments: (String) -> Unit,
    onNavigateToPremium: () -> Unit,
    navigationManager: NavigationManager,
    viewModel: MainViewModel,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavDestination.Home.route,
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // Home destination
        composable(BottomNavDestination.Home.route) {
            HomeScreen(
                onNavigateToPostDetail = onNavigateToPostDetail,
                onNavigateToUserProfile = onNavigateToUserProfile,
                onNavigateToProfile = {
                    navController.navigate(BottomNavDestination.Profile.route)
                },
                onNavigateToNotifications = { navController.navigateToNotifications() },
                onNavigateToLikes = { postId -> navController.navigateToLikes(postId) },
                onNavigateToComments = { postId -> navController.navigateToComments(postId) },
                onNavigateToPremium = onNavigateToPremium,
                onNavigateToMapLocation = { latitude, longitude ->
                    navigationManager.updateMapCameraPosition(latitude, longitude, 16f)
                    navController.navigate(BottomNavDestination.Map.route) {
                        launchSingleTop = true
                    }
                }
            )
        }
        
        // Search destination
        composable(BottomNavDestination.Search.route) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPostDetail = onNavigateToPostDetail,
                onNavigateToUserProfile = onNavigateToUserProfile,
                onNavigateToProfile = {
                    navController.navigate(BottomNavDestination.Profile.route)
                },
                onNavigateToPremium = onNavigateToPremium,
                onNavigateToMapLocation = { latitude, longitude ->
                    navigationManager.updateMapCameraPosition(latitude, longitude, 16f)
                    navController.navigate(BottomNavDestination.Map.route) {
                        launchSingleTop = true
                    }
                }
            )
        }
        
        // Map destination
        composable(BottomNavDestination.Map.route) {
            com.omerkaya.sperrmuellfinder.ui.map.MapScreen(
                onNavigateToPostDetail = onNavigateToPostDetail,
                navigationManager = navigationManager
            )
        }

        // Premium routes (single source: RevenueCat published paywall)
        composable(AppDestinations.premium()) {
            com.omerkaya.sperrmuellfinder.ui.premium.RevenueCatPaywallScreen(
                onDismiss = { navController.popBackStack() },
                onPurchaseSuccess = { navController.popBackStack() }
            )
        }
        composable(AppDestinations.premiumStatus()) {
            com.omerkaya.sperrmuellfinder.ui.premium.PremiumStatusScreen(
                onNavigateBack = { navController.popBackStack() },
                onManageSubscription = { navController.navigate(AppDestinations.customerCenter()) },
                onUpgrade = { navController.navigate(AppDestinations.premium()) }
            )
        }
        composable(AppDestinations.modernPaywall()) {
            com.omerkaya.sperrmuellfinder.ui.premium.RevenueCatPaywallScreen(
                onDismiss = { navController.popBackStack() },
                onPurchaseSuccess = { navController.popBackStack() }
            )
        }
        composable(AppDestinations.revenueCatPaywall()) {
            com.omerkaya.sperrmuellfinder.ui.premium.RevenueCatPaywallScreen(
                onDismiss = { navController.popBackStack() },
                onPurchaseSuccess = { navController.popBackStack() }
            )
        }
        composable(AppDestinations.customerCenter()) {
            com.omerkaya.sperrmuellfinder.ui.premium.RevenueCatCustomerCenterScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Camera destination
        composable(BottomNavDestination.Camera.route) {
            com.omerkaya.sperrmuellfinder.ui.camera.CameraScreen(
                onNavigateToHome = {
                    // Navigate to home and refresh feed after post creation
                    navController.navigate(BottomNavDestination.Home.route) {
                        popUpTo(BottomNavDestination.Camera.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                    // Trigger refresh in HomeViewModel via MainViewModel
                    viewModel.onPostCreated()
                }
            )
        }
        
        // Profile destination
        composable(BottomNavDestination.Profile.route) {
            com.omerkaya.sperrmuellfinder.ui.profile.ProfileScreen(
                onNavigateToPostDetail = onNavigateToPostDetail,
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToEditProfile = {
                    navController.navigate("edit_profile")
                },
                onNavigateToFollowers = { userId ->
                    navController.navigateToFollowers(userId)
                },
                onNavigateToAdminDashboard = {
                    navController.navigate(com.omerkaya.sperrmuellfinder.ui.navigation.AppDestinations.adminDashboard())
                },
                onNavigateToFollowing = { userId ->
                    navController.navigateToFollowing(userId)
                },
                onNavigateToDeleteAccount = {
                    navController.navigateToDeleteAccount()
                }
            )
        }
        
        // Settings destination
        composable("settings") {
            com.omerkaya.sperrmuellfinder.ui.settings.SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToNotifications = {
                    // TODO: Navigate to notifications
                },
                onNavigateToPrivacy = {
                    // TODO: Navigate to privacy
                },
                onNavigateToAbout = {
                    navController.navigate(AppDestinations.about())
                },
                onNavigateToPremium = { isPremium ->
                    if (isPremium) {
                        navController.navigate(AppDestinations.premiumStatus())
                    } else {
                        navController.navigate(AppDestinations.modernPaywall())
                    }
                },
                onNavigateToAdminDashboard = {
                    navController.navigate(com.omerkaya.sperrmuellfinder.ui.navigation.AppDestinations.adminDashboard())
                },
                onNavigateToBlockedUsers = {
                    navController.navigate(AppDestinations.blockedUsers())
                },
                onNavigateToDeleteAccount = {
                    navController.navigate(AppDestinations.deleteAccount())
                },
                onNavigateToLogin = {
                    // Navigate to login and clear back stack
                    navController.navigate("auth") {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                }
            )
        }
        
        // Edit Profile destination
        composable("edit_profile") {
            com.omerkaya.sperrmuellfinder.ui.profile.EditProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // About destination
        composable(AppDestinations.about()) {
            com.omerkaya.sperrmuellfinder.ui.settings.AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Blocked Users destination
        composable(AppDestinations.blockedUsers()) {
            com.omerkaya.sperrmuellfinder.ui.block.BlockedUsersScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Delete Account destination
        composable(AppDestinations.deleteAccount()) {
            com.omerkaya.sperrmuellfinder.ui.settings.DeleteAccountScreen(
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("auth") {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }
        
        // Notifications destination
        composable("notifications") {
            NotificationsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPostDetail = onNavigateToPostDetail,
                onNavigateToUserProfile = onNavigateToUserProfile,
                onNavigateToProfile = {
                    navController.navigate(BottomNavDestination.Profile.route)
                },
                onNavigateToPremium = onNavigateToPremium
            )
        }
        
        // Likes destination
        composable("likes/{postId}") { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            com.omerkaya.sperrmuellfinder.ui.likes.LikesListScreen(
                postId = postId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToUserProfile = { userId ->
                    navController.navigateToUserProfile(userId)
                },
                onNavigateToOwnProfile = {
                    navController.navigate("profile")
                }
            )
        }
        
        // Comments destination
        composable("comments/{postId}") { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            com.omerkaya.sperrmuellfinder.ui.comments.CommentsScreen(
                postId = postId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToUserProfile = { userId ->
                    navController.navigateToUserProfile(userId)
                },
                onNavigateToProfile = {
                    navController.navigate(BottomNavDestination.Profile.route)
                }
            )
        }
        
        // Post Detail destination
        composable("post_detail/{postId}") { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            com.omerkaya.sperrmuellfinder.ui.postdetail.PostDetailScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToUserProfile = onNavigateToUserProfile,
                onNavigateToComments = { postId -> navController.navigateToComments(postId) },
                onNavigateToLikes = { postId -> navController.navigateToLikes(postId) },
                onNavigateToPremium = onNavigateToPremium,
                onNavigateToMapLocation = { latitude, longitude ->
                    navigationManager.updateMapCameraPosition(latitude, longitude, 16f)
                    navController.navigate(BottomNavDestination.Map.route) {
                        launchSingleTop = true
                    }
                }
            )
        }
        
        // User Profile destination
        composable("user_profile/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserId != null && userId == currentUserId) {
                com.omerkaya.sperrmuellfinder.ui.profile.ProfileScreen(
                    onNavigateToPostDetail = onNavigateToPostDetail,
                    onNavigateToSettings = { navController.navigate(AppDestinations.settings()) },
                    onNavigateToEditProfile = { navController.navigate("edit_profile") },
                    onNavigateToFollowers = { uid -> navController.navigateToFollowers(uid) },
                    onNavigateToFollowing = { uid -> navController.navigateToFollowing(uid) },
                    onNavigateToAdminDashboard = { navController.navigate(AppDestinations.adminDashboard()) },
                    onNavigateToDeleteAccount = { navController.navigateToDeleteAccount() }
                )
            } else {
                com.omerkaya.sperrmuellfinder.ui.profile.UserProfileScreen(
                    userId = userId,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToPostDetail = onNavigateToPostDetail,
                    onNavigateToFollowers = { targetUserId ->
                        navController.navigateToFollowers(targetUserId)
                    },
                    onNavigateToFollowing = { targetUserId ->
                        navController.navigateToFollowing(targetUserId)
                    }
                )
            }
        }
        
        // Followers destination
        composable("followers/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            com.omerkaya.sperrmuellfinder.ui.followers.FollowersScreen(
                userId = userId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToUserProfile = { targetUserId ->
                    navController.navigateToUserProfile(targetUserId)
                }
            )
        }
        
        // Following destination
        composable("following/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            com.omerkaya.sperrmuellfinder.ui.followers.FollowingScreen(
                userId = userId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToUserProfile = { targetUserId ->
                    navController.navigateToUserProfile(targetUserId)
                }
            )
        }

        // Admin Dashboard destination
        composable(AppDestinations.adminDashboard()) {
            AdminOnlyRoute(onNavigateBack = { navController.popBackStack() }) {
                com.omerkaya.sperrmuellfinder.ui.admin.AdminDashboardScreen(
                    onNavigateToReports = { navController.navigate(AppDestinations.adminReports()) },
                    onNavigateToUsers = { navController.navigate(AppDestinations.adminUsers()) },
                    onNavigateToPremium = { navController.navigate(AppDestinations.adminPremium()) },
                    onNavigateToContent = { navController.navigate(AppDestinations.adminContent()) },
                    onNavigateToNotifications = { navController.navigate(AppDestinations.adminNotifications()) },
                    onNavigateToLogs = { navController.navigate(AppDestinations.adminLogs()) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        // Admin sub-routes (all behind route guard; non-V1 pages fallback to reports)
        composable(AppDestinations.adminReports()) {
            AdminOnlyRoute(onNavigateBack = { navController.popBackStack() }) {
                com.omerkaya.sperrmuellfinder.ui.admin.reports.AdminReportsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        composable(AppDestinations.adminUsers()) {
            AdminOnlyRoute(onNavigateBack = { navController.popBackStack() }) {
                com.omerkaya.sperrmuellfinder.ui.admin.reports.AdminReportsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        composable(AppDestinations.adminPremium()) {
            AdminOnlyRoute(onNavigateBack = { navController.popBackStack() }) {
                com.omerkaya.sperrmuellfinder.ui.admin.reports.AdminReportsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        composable(AppDestinations.adminContent()) {
            AdminOnlyRoute(onNavigateBack = { navController.popBackStack() }) {
                com.omerkaya.sperrmuellfinder.ui.admin.reports.AdminReportsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        composable(AppDestinations.adminNotifications()) {
            AdminOnlyRoute(onNavigateBack = { navController.popBackStack() }) {
                com.omerkaya.sperrmuellfinder.ui.admin.reports.AdminReportsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        composable(AppDestinations.adminLogs()) {
            AdminOnlyRoute(onNavigateBack = { navController.popBackStack() }) {
                com.omerkaya.sperrmuellfinder.ui.admin.reports.AdminReportsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun AdminOnlyRoute(
    onNavigateBack: () -> Unit,
    content: @Composable () -> Unit
) {
    val isAdmin = produceState<Boolean?>(initialValue = null) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            value = false
            return@produceState
        }
        value = runCatching {
            val claims = user.getIdToken(false).await().claims
            claims["admin"] == true || claims["moderator"] == true || claims["super_admin"] == true
        }.getOrDefault(false)
    }.value

    when (isAdmin) {
        null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        true -> content()
        false -> {
            LaunchedEffect(Unit) { onNavigateBack() }
        }
    }
}

// Placeholder screens for destinations not yet implemented

@Composable
private fun CameraPlaceholderScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Camera,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.camera_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.camera_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.camera_coming_soon),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * CRASH FIX: Defensive loading screen shown during app initialization
 */
@Composable
private fun DefensiveLoadingScreen(
    phase: StartupPhase,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Loading indicator
        androidx.compose.material3.CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Phase-specific loading text
        Text(
            text = when (phase) {
                StartupPhase.Initializing -> stringResource(R.string.startup_initializing)
                StartupPhase.LoadingUser -> stringResource(R.string.startup_loading_user)
                StartupPhase.LoadingPremium -> stringResource(R.string.startup_loading_premium)
                StartupPhase.Ready -> stringResource(R.string.startup_almost_ready)
                StartupPhase.Error -> stringResource(R.string.startup_error_loading)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.startup_please_wait),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/**
 * CRASH FIX: Defensive error screen shown when startup fails critically
 */
@Composable
private fun DefensiveErrorScreen(
    error: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Error icon
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.error_startup_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = error ?: stringResource(R.string.error_unknown),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        androidx.compose.material3.Button(
            onClick = onRetry,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = stringResource(R.string.error_retry),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.error_restart_app_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/**
 * Navigation extensions for MainScreen
 */
private fun NavHostController.navigateToNotifications() {
    navigate("notifications")
}

private fun NavHostController.navigateToLikes(postId: String) {
    navigate("likes/$postId")
}

private fun NavHostController.navigateToComments(postId: String) {
    navigate("comments/$postId")
}

/**
 * Check if a route is a bottom navigation route
 */
private fun isBottomNavRoute(route: String): Boolean {
    return route in listOf("home", "search", "map", "camera", "profile")
}

