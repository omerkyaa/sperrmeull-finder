package com.omerkaya.sperrmuellfinder.ui.navigation

/**
 * Sealed class defining all app navigation destinations with type-safe route builders.
 * Single source of truth for navigation routes.
 */
sealed class AppDestinations(val route: String) {
    
    // Main bottom navigation destinations
    data object Home : AppDestinations("home")
    data object Search : AppDestinations("search") 
    data object Camera : AppDestinations("camera")
    data object Profile : AppDestinations("profile")
    
    // Secondary screens
    data object Notifications : AppDestinations("notifications")
    
    // Screens with arguments
    data object LikesList : AppDestinations("likes/{$ARG_POST_ID}") {
        fun createRoute(postId: String) = "likes/$postId"
    }
    
    data object Comments : AppDestinations("comments/{$ARG_POST_ID}") {
        fun createRoute(postId: String) = "comments/$postId"
    }
    
    data object PostDetail : AppDestinations("post_detail/{$ARG_POST_ID}") {
        fun createRoute(postId: String) = "post_detail/$postId"
    }
    
    data object UserProfile : AppDestinations("user_profile/{$ARG_USER_ID}") {
        fun createRoute(userId: String) = "user_profile/$userId"
    }
    
    data object Followers : AppDestinations("followers/{$ARG_USER_ID}") {
        fun createRoute(userId: String) = "followers/$userId"
    }
    
    data object Following : AppDestinations("following/{$ARG_USER_ID}") {
        fun createRoute(userId: String) = "following/$userId"
    }
    
    // Premium flow
    data object Premium : AppDestinations("premium")
    data object PremiumStatus : AppDestinations("premium_status")
    data object ModernPaywall : AppDestinations("modern_paywall")
    data object PurchaseSuccess : AppDestinations("purchase_success")
    data object RevenueCatPaywall : AppDestinations("revenuecat_paywall")
    data object CustomerCenter : AppDestinations("customer_center")
    
    // Settings and other screens
    data object Settings : AppDestinations("settings")
    data object About : AppDestinations("about")
    data object EditProfile : AppDestinations("edit_profile")
    data object BlockedUsers : AppDestinations("blocked_users")
    data object DeleteAccount : AppDestinations("delete_account")
    
    // Admin screens
    data object AdminDashboard : AppDestinations("admin/dashboard")
    data object AdminReports : AppDestinations("admin/reports")
    data object AdminUsers : AppDestinations("admin/users")
    data object AdminPremium : AppDestinations("admin/premium")
    data object AdminContent : AppDestinations("admin/content")
    data object AdminNotifications : AppDestinations("admin/notifications")
    data object AdminLogs : AppDestinations("admin/logs")
    
    companion object {
        // Navigation argument keys
        const val ARG_POST_ID = "postId"
        const val ARG_USER_ID = "userId"
        
        // Route builders for destinations without arguments
        fun notifications() = Notifications.route
        fun home() = Home.route
        fun search() = Search.route
        fun camera() = Camera.route
        fun profile() = Profile.route
        fun premium() = Premium.route
        fun premiumStatus() = PremiumStatus.route
        fun modernPaywall() = ModernPaywall.route
        fun revenueCatPaywall() = RevenueCatPaywall.route
        fun customerCenter() = CustomerCenter.route
        fun settings() = Settings.route
        fun about() = About.route
        fun editProfile() = EditProfile.route
        fun blockedUsers() = BlockedUsers.route
        fun deleteAccount() = DeleteAccount.route
        
        // Admin route builders
        fun adminDashboard() = AdminDashboard.route
        fun adminReports() = AdminReports.route
        fun adminUsers() = AdminUsers.route
        fun adminPremium() = AdminPremium.route
        fun adminContent() = AdminContent.route
        fun adminNotifications() = AdminNotifications.route
        fun adminLogs() = AdminLogs.route
        
        // Route builders for destinations with arguments
        fun likes(postId: String) = LikesList.createRoute(postId)
        fun comments(postId: String) = Comments.createRoute(postId)
        fun postDetail(postId: String) = PostDetail.createRoute(postId)
        fun userProfile(userId: String) = UserProfile.createRoute(userId)
        fun followers(userId: String) = Followers.createRoute(userId)
        fun following(userId: String) = Following.createRoute(userId)
    }
}
