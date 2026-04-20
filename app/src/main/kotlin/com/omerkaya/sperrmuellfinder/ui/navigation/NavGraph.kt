package com.omerkaya.sperrmuellfinder.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.omerkaya.sperrmuellfinder.ui.notifications.NotificationsScreen
import com.omerkaya.sperrmuellfinder.ui.likes.LikesListScreen
import com.omerkaya.sperrmuellfinder.ui.comments.CommentsScreen
import com.omerkaya.sperrmuellfinder.ui.postdetail.PostDetailScreen
import com.omerkaya.sperrmuellfinder.ui.profile.EditProfileScreen
import com.omerkaya.sperrmuellfinder.ui.profile.UserProfileScreen
import com.omerkaya.sperrmuellfinder.ui.followers.FollowersScreen
import com.omerkaya.sperrmuellfinder.ui.followers.FollowingScreen

/**
 * Main navigation graph for the app.
 * Defines all navigation destinations and their composable screens.
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = AppDestinations.home(),
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Notifications screen
        composable(AppDestinations.notifications()) {
            NotificationsScreen(
                onNavigateBack = { navController.navigateBack() },
                onNavigateToPostDetail = { postId -> 
                    navController.navigate(AppDestinations.postDetail(postId))
                },
                onNavigateToUserProfile = { userId ->
                    navController.navigateToUserProfile(userId)
                },
                onNavigateToProfile = {
                    navController.navigate(AppDestinations.profile())
                },
                onNavigateToPremium = {
                    navController.navigate(AppDestinations.premium())
                }
            )
        }
        
        // Likes list screen with post ID argument
        composable(
            route = AppDestinations.LikesList.route,
            arguments = listOf(
                navArgument(AppDestinations.ARG_POST_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString(AppDestinations.ARG_POST_ID)
                ?: return@composable
            
            LikesListScreen(
                postId = postId,
                onNavigateBack = { navController.navigateBack() },
                onNavigateToUserProfile = { userId ->
                    navController.navigateToUserProfile(userId)
                },
                onNavigateToOwnProfile = {
                    navController.navigateToProfile()
                }
            )
        }
        
        // Comments screen with post ID argument
        composable(
            route = AppDestinations.Comments.route,
            arguments = listOf(
                navArgument(AppDestinations.ARG_POST_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString(AppDestinations.ARG_POST_ID)
                ?: return@composable
            
            CommentsScreen(
                postId = postId,
                onNavigateBack = { navController.navigateBack() },
                onNavigateToUserProfile = { userId ->
                    navController.navigateToUserProfile(userId)
                },
                onNavigateToProfile = {
                    navController.navigate(AppDestinations.profile())
                }
            )
        }
        
        // Post detail screen with post ID argument
        composable(
            route = AppDestinations.PostDetail.route,
            arguments = listOf(
                navArgument(AppDestinations.ARG_POST_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString(AppDestinations.ARG_POST_ID)
                ?: return@composable
            
            PostDetailScreen(
                onNavigateBack = { navController.navigateBack() },
                onNavigateToUserProfile = { userId ->
                    navController.navigateToUserProfile(userId)
                },
                onNavigateToComments = { postId ->
                    navController.navigateToComments(postId)
                },
                onNavigateToLikes = { postId ->
                    navController.navigateToLikes(postId)
                }
            )
        }
        
        // User profile screen with user ID argument
        composable(
            route = AppDestinations.UserProfile.route,
            arguments = listOf(
                navArgument(AppDestinations.ARG_USER_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString(AppDestinations.ARG_USER_ID)
                ?: return@composable
            
            UserProfileScreen(
                userId = userId,
                onNavigateBack = { navController.navigateBack() },
                onNavigateToPostDetail = { postId ->
                    navController.navigate(AppDestinations.postDetail(postId))
                },
                onNavigateToFollowers = { userId ->
                    navController.navigate(AppDestinations.followers(userId))
                },
                onNavigateToFollowing = { userId ->
                    navController.navigate(AppDestinations.following(userId))
                }
            )
        }
        
        // Followers screen with user ID argument
        composable(
            route = AppDestinations.Followers.route,
            arguments = listOf(
                navArgument(AppDestinations.ARG_USER_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString(AppDestinations.ARG_USER_ID)
                ?: return@composable
            
            FollowersScreen(
                userId = userId,
                onNavigateBack = { navController.navigateBack() },
                onNavigateToUserProfile = { userId ->
                    navController.navigateToUserProfile(userId)
                }
            )
        }
        
        // Following screen with user ID argument
        composable(
            route = AppDestinations.Following.route,
            arguments = listOf(
                navArgument(AppDestinations.ARG_USER_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString(AppDestinations.ARG_USER_ID)
                ?: return@composable
            
            FollowingScreen(
                userId = userId,
                onNavigateBack = { navController.navigateBack() },
                onNavigateToUserProfile = { userId ->
                    navController.navigateToUserProfile(userId)
                }
            )
        }
        
        // Premium screens
        composable(AppDestinations.premium()) {
            com.omerkaya.sperrmuellfinder.ui.premium.RevenueCatPaywallScreen(
                onDismiss = { navController.navigateBack() },
                onPurchaseSuccess = { navController.navigateBack() }
            )
        }
        composable(AppDestinations.premiumStatus()) {
            com.omerkaya.sperrmuellfinder.ui.premium.PremiumStatusScreen(
                onNavigateBack = { navController.navigateBack() },
                onManageSubscription = { navController.navigate(AppDestinations.customerCenter()) },
                onUpgrade = { navController.navigate(AppDestinations.premium()) }
            )
        }
        
        // RevenueCat published paywall
        composable(AppDestinations.modernPaywall()) {
            com.omerkaya.sperrmuellfinder.ui.premium.RevenueCatPaywallScreen(
                onDismiss = { navController.navigateBack() },
                onPurchaseSuccess = {
                    // Navigate back or to success screen
                    navController.navigateBack()
                }
            )
        }
        
        // RevenueCat Paywall
        composable(AppDestinations.revenueCatPaywall()) {
            com.omerkaya.sperrmuellfinder.ui.premium.RevenueCatPaywallScreen(
                onDismiss = { navController.navigateBack() },
                onPurchaseSuccess = {
                    navController.navigateBack()
                }
            )
        }
        
        // RevenueCat Customer Center (temporary fallback to RevenueCat paywall)
        composable(AppDestinations.customerCenter()) {
            com.omerkaya.sperrmuellfinder.ui.premium.RevenueCatCustomerCenterScreen(
                onNavigateBack = { navController.navigateBack() }
            )
        }
        
        // Settings screens
        composable(AppDestinations.settings()) {
            // TODO: Implement SettingsScreen
            // SettingsScreen(
            //     onNavigateBack = { navController.navigateBack() }
            // )
        }
        
        // Edit Profile screen
        composable(AppDestinations.editProfile()) {
            EditProfileScreen(
                onNavigateBack = { navController.navigateBack() }
            )
        }
        
        // Admin Dashboard
        composable(AppDestinations.adminDashboard()) {
            com.omerkaya.sperrmuellfinder.ui.admin.AdminDashboardScreen(
                onNavigateToReports = { navController.navigate(AppDestinations.adminReports()) },
                onNavigateToUsers = { navController.navigate(AppDestinations.adminUsers()) },
                onNavigateToPremium = { navController.navigate(AppDestinations.adminPremium()) },
                onNavigateToContent = { navController.navigate(AppDestinations.adminContent()) },
                onNavigateToNotifications = { navController.navigate(AppDestinations.adminNotifications()) },
                onNavigateToLogs = { navController.navigate(AppDestinations.adminLogs()) },
                onNavigateBack = { navController.navigateBack() }
            )
        }
        
        // Admin Reports (placeholder)
        composable(AppDestinations.adminReports()) {
            // TODO: Implement AdminReportsScreen
        }
        
        // Admin Users (placeholder)
        composable(AppDestinations.adminUsers()) {
            // TODO: Implement AdminUsersScreen
        }
        
        // Admin Premium (placeholder)
        composable(AppDestinations.adminPremium()) {
            // TODO: Implement AdminPremiumScreen
        }
        
        // Admin Content (placeholder)
        composable(AppDestinations.adminContent()) {
            // TODO: Implement AdminContentScreen
        }
        
        // Admin Notifications (placeholder)
        composable(AppDestinations.adminNotifications()) {
            // TODO: Implement AdminNotificationsScreen
        }
        
        // Admin Logs (placeholder)
        composable(AppDestinations.adminLogs()) {
            // TODO: Implement AdminLogsScreen
        }
    }
}
