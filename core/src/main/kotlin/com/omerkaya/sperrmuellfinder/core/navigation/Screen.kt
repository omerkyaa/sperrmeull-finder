package com.omerkaya.sperrmuellfinder.core.navigation

/**
 * Navigation destinations for the SperrmüllFinder app.
 * Comprehensive screen definitions following rules.md and PRD.md specifications.
 * Professional navigation architecture with Clean Architecture principles.
 */
sealed class Screen(val route: String) {
    
    // ========== AUTHENTICATION FLOW ==========
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object ForgotPassword : Screen("forgot_password")
    data object ProfileSetup : Screen("profile_setup")
    
    // ========== MAIN BOTTOM NAVIGATION FLOW ==========
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object Map : Screen("map")
    data object Camera : Screen("camera")
    data object Profile : Screen("profile")
    
    // ========== POST & CONTENT SCREENS ==========
    data object PostDetail : Screen("post_detail/{postId}") {
        fun createRoute(postId: String) = "post_detail/$postId"
        const val POST_ID_ARG = "postId"
    }
    
    data object PostCreate : Screen("post_create")
    data object PostEdit : Screen("post_edit/{postId}") {
        fun createRoute(postId: String) = "post_edit/$postId"
        const val POST_ID_ARG = "postId"
    }
    
    data object PostPreview : Screen("post_preview")
    
    // ========== USER & SOCIAL SCREENS ==========
    data object UserProfile : Screen("user_profile/{userId}") {
        fun createRoute(userId: String) = "user_profile/$userId"
        const val USER_ID_ARG = "userId"
    }
    
    data object EditProfile : Screen("edit_profile")
    data object ProfilePhoto : Screen("profile_photo")
    
    // ========== SOCIAL FEATURES ==========
    data object Comments : Screen("comments/{postId}") {
        fun createRoute(postId: String) = "comments/$postId"
        const val POST_ID_ARG = "postId"
    }
    
    data object Likes : Screen("likes/{postId}") {
        fun createRoute(postId: String) = "likes/$postId"
        const val POST_ID_ARG = "postId"
    }
    
    data object Followers : Screen("followers/{userId}") {
        fun createRoute(userId: String) = "followers/$userId"
        const val USER_ID_ARG = "userId"
    }
    
    data object Following : Screen("following/{userId}") {
        fun createRoute(userId: String) = "following/$userId"
        const val USER_ID_ARG = "userId"
    }
    
    // ========== SEARCH & DISCOVERY ==========
    data object SearchResults : Screen("search_results?query={query}&category={category}&city={city}") {
        fun createRoute(
            query: String = "",
            category: String = "",
            city: String = ""
        ) = "search_results?query=$query&category=$category&city=$city"
        const val QUERY_ARG = "query"
        const val CATEGORY_ARG = "category"
        const val CITY_ARG = "city"
    }
    
    data object SearchFilters : Screen("search_filters")
    data object SearchHistory : Screen("search_history")
    
    // ========== PREMIUM FEATURES & MONETIZATION ==========
    data object Premium : Screen("premium")
    data object Paywall : Screen("paywall/{source}") {
        fun createRoute(source: String = "general") = "paywall/$source"
        const val SOURCE_ARG = "source"
    }
    
    data object PremiumStatus : Screen("premium_status")
    data object PurchaseSuccess : Screen("purchase_success/{productId}") {
        fun createRoute(productId: String) = "purchase_success/$productId"
        const val PRODUCT_ID_ARG = "productId"
    }
    
    data object PostExtension : Screen("post_extension/{postId}") {
        fun createRoute(postId: String) = "post_extension/$postId"
        const val POST_ID_ARG = "postId"
    }
    
    // ========== SETTINGS & MANAGEMENT ==========
    data object Settings : Screen("settings")
    data object NotificationSettings : Screen("notification_settings")
    data object PrivacySettings : Screen("privacy_settings")
    data object AccountSettings : Screen("account_settings")
    data object LanguageSettings : Screen("language_settings")
    data object ThemeSettings : Screen("theme_settings")
    data object About : Screen("about")
    data object Help : Screen("help")
    data object TermsOfService : Screen("terms_of_service")
    data object PrivacyPolicy : Screen("privacy_policy")
    
    // ========== NOTIFICATIONS ==========
    data object Notifications : Screen("notifications")
    data object NotificationDetail : Screen("notification_detail/{notificationId}") {
        fun createRoute(notificationId: String) = "notification_detail/$notificationId"
        const val NOTIFICATION_ID_ARG = "notificationId"
    }
    
    // ========== MODERATION & REPORTS ==========
    data object ReportPost : Screen("report_post/{postId}") {
        fun createRoute(postId: String) = "report_post/$postId"
        const val POST_ID_ARG = "postId"
    }
    
    data object ReportUser : Screen("report_user/{userId}") {
        fun createRoute(userId: String) = "report_user/$userId"
        const val USER_ID_ARG = "userId"
    }
    
    data object ReportComment : Screen("report_comment/{commentId}") {
        fun createRoute(commentId: String) = "report_comment/$commentId"
        const val COMMENT_ID_ARG = "commentId"
    }
    
    data object BlockedUsers : Screen("blocked_users")
    
    // ========== ARCHIVE & HISTORY ==========
    data object Archive : Screen("archive")
    data object PostHistory : Screen("post_history")
    data object ActivityLog : Screen("activity_log")
    
    // ========== STATISTICS ==========
    data object UserStats : Screen("user_stats")
    data object PostStats : Screen("post_stats/{postId}") {
        fun createRoute(postId: String) = "post_stats/$postId"
        const val POST_ID_ARG = "postId"
    }
    
    // ========== UTILITY & MEDIA ==========
    data object ImageViewer : Screen("image_viewer?imageUrl={imageUrl}&postId={postId}&index={index}") {
        fun createRoute(
            imageUrl: String,
            postId: String = "",
            index: Int = 0
        ) = "image_viewer?imageUrl=$imageUrl&postId=$postId&index=$index"
        const val IMAGE_URL_ARG = "imageUrl"
        const val POST_ID_ARG = "postId"
        const val INDEX_ARG = "index"
    }
    
    data object ImageCarousel : Screen("image_carousel/{postId}/{startIndex}") {
        fun createRoute(postId: String, startIndex: Int = 0) = "image_carousel/$postId/$startIndex"
        const val POST_ID_ARG = "postId"
        const val START_INDEX_ARG = "startIndex"
    }
    
    data object WebView : Screen("webview?url={url}&title={title}") {
        fun createRoute(url: String, title: String = "") = "webview?url=$url&title=$title"
        const val URL_ARG = "url"
        const val TITLE_ARG = "title"
    }
    
    data object QrScanner : Screen("qr_scanner")
    data object ShareSheet : Screen("share_sheet/{postId}") {
        fun createRoute(postId: String) = "share_sheet/$postId"
        const val POST_ID_ARG = "postId"
    }
    
    // ========== ERROR & MAINTENANCE ==========
    data object Error : Screen("error/{errorType}") {
        fun createRoute(errorType: String) = "error/$errorType"
        const val ERROR_TYPE_ARG = "errorType"
    }
    
    data object NetworkError : Screen("network_error")
    data object Maintenance : Screen("maintenance")
    data object ForceUpdate : Screen("force_update")
    data object PermissionDenied : Screen("permission_denied/{permission}") {
        fun createRoute(permission: String) = "permission_denied/$permission"
        const val PERMISSION_ARG = "permission"
    }
    
    companion object {
        // ========== NAVIGATION GROUPS ==========
        
        /**
         * Bottom Navigation Items (Main App Flow)
         */
        val bottomNavItems = listOf(Home, Search, Map, Camera, Profile)
        
        /**
         * Authentication Flow Screens
         */
        val authFlowRoutes = setOf(
            Splash.route,
            Onboarding.route,
            Login.route,
            Register.route,
            ForgotPassword.route,
            ProfileSetup.route
        )
        
        /**
         * Routes that don't show bottom navigation
         */
        val routesWithoutBottomNav = setOf(
            // Auth Flow
            Splash.route,
            Onboarding.route,
            Login.route,
            Register.route,
            ForgotPassword.route,
            ProfileSetup.route,
            
            // Full Screen Features
            Camera.route,
            ImageViewer.route,
            ImageCarousel.route,
            QrScanner.route,
            
            // Premium & Purchase Flow
            Premium.route,
            Paywall.route,
            PremiumStatus.route,
            PurchaseSuccess.route,
            
            // Utility & Error Screens
            WebView.route,
            Error.route,
            NetworkError.route,
            Maintenance.route,
            ForceUpdate.route,
            PermissionDenied.route,
            
            // Detail Screens (Full Screen)
            PostDetail.route,
            PostCreate.route,
            PostPreview.route,
            Comments.route,
            Likes.route,
            ShareSheet.route
        )
        
        /**
         * Routes that require authentication
         */
        val authRequiredRoutes = setOf(
            // Main Bottom Nav
            Home.route,
            Search.route,
            Map.route,
            Camera.route,
            Profile.route,
            
            // Post Management
            PostDetail.route,
            PostCreate.route,
            PostEdit.route,
            PostPreview.route,
            PostExtension.route,
            PostStats.route,
            
            // User & Social
            UserProfile.route,
            EditProfile.route,
            ProfilePhoto.route,
            Comments.route,
            Likes.route,
            Followers.route,
            Following.route,
            
            // Search & Discovery
            SearchResults.route,
            SearchFilters.route,
            SearchHistory.route,
            
            // Premium Features
            Premium.route,
            Paywall.route,
            PremiumStatus.route,
            PurchaseSuccess.route,
            
            // Settings
            Settings.route,
            NotificationSettings.route,
            PrivacySettings.route,
            AccountSettings.route,
            LanguageSettings.route,
            ThemeSettings.route,
            
            // Notifications
            Notifications.route,
            NotificationDetail.route,
            
            // Moderation
            ReportPost.route,
            ReportUser.route,
            ReportComment.route,
            BlockedUsers.route,
            
            // Archive & History
            Archive.route,
            PostHistory.route,
            ActivityLog.route,
            
            // Statistics
            UserStats.route,
            
            // Utility (Authenticated)
            ImageCarousel.route,
            QrScanner.route,
            ShareSheet.route
        )
        
        /**
         * Routes accessible to guests (no authentication required)
         */
        val guestRoutes = setOf(
            // Auth Flow
            Splash.route,
            Onboarding.route,
            Login.route,
            Register.route,
            ForgotPassword.route,
            
            // Public Information
            About.route,
            Help.route,
            TermsOfService.route,
            PrivacyPolicy.route,
            
            // Utility
            WebView.route,
            ImageViewer.route,
            
            // Error & Maintenance
            Error.route,
            NetworkError.route,
            Maintenance.route,
            ForceUpdate.route,
            PermissionDenied.route
        )
        
        /**
         * Routes that should clear back stack when navigated to
         */
        val clearBackStackRoutes = setOf(
            Home.route,
            Login.route,
            Splash.route,
            Maintenance.route,
            ForceUpdate.route,
            NetworkError.route
        )
        
        /**
         * Routes that need special handling for premium users
         */
        val premiumFeatureRoutes = setOf(
            // Premium Gated Features
            Search.route,
            SearchResults.route,
            SearchFilters.route,
            SearchHistory.route,
            
            // Premium Content Access
            Archive.route,
            PostStats.route,
            ActivityLog.route,
            
            // Premium Map Features
            Map.route, // For unlimited radius
            
            // Premium Social Features
            UserStats.route,
            
            // Premium Post Features
            PostDetail.route, // For availability percentage
            PostExtension.route,
            
            // Premium User Features
            UserProfile.route // For premium badges and frames
        )
        
        /**
         * Routes that support deep linking
         */
        val deepLinkableRoutes = setOf(
            PostDetail.route,
            UserProfile.route,
            Comments.route,
            Likes.route,
            ImageViewer.route,
            ImageCarousel.route,
            ShareSheet.route,
            Premium.route,
            Paywall.route
        )
        
        // ========== HELPER FUNCTIONS ==========
        
        /**
         * Check if route requires authentication
         */
        fun requiresAuth(route: String?): Boolean {
            if (route == null) return false
            return authRequiredRoutes.any { authRoute ->
                route.matches(Regex(authRoute.replace("\\{.*?\\}", ".*")))
            }
        }
        
        /**
         * Check if route should show bottom navigation
         */
        fun showsBottomNav(route: String?): Boolean {
            if (route == null) return false
            return !routesWithoutBottomNav.any { hiddenRoute ->
                route.matches(Regex(hiddenRoute.replace("\\{.*?\\}", ".*")))
            }
        }
        
        /**
         * Check if route requires premium access
         */
        fun requiresPremium(route: String?): Boolean {
            if (route == null) return false
            return premiumFeatureRoutes.any { premiumRoute ->
                route.matches(Regex(premiumRoute.replace("\\{.*?\\}", ".*")))
            }
        }
        
        /**
         * Check if route is part of authentication flow
         */
        fun isAuthFlow(route: String?): Boolean {
            if (route == null) return false
            return authFlowRoutes.contains(route)
        }
        
        /**
         * Check if route supports deep linking
         */
        fun isDeepLinkable(route: String?): Boolean {
            if (route == null) return false
            return deepLinkableRoutes.any { deepLinkRoute ->
                route.matches(Regex(deepLinkRoute.replace("\\{.*?\\}", ".*")))
            }
        }
        
        /**
         * Check if route should clear back stack
         */
        fun shouldClearBackStack(route: String?): Boolean {
            if (route == null) return false
            return clearBackStackRoutes.contains(route)
        }
        
        /**
         * Get bottom navigation item index for route
         */
        fun getBottomNavIndex(route: String?): Int {
            if (route == null) return 0
            return bottomNavItems.indexOfFirst { item ->
                route.startsWith(item.route)
            }.takeIf { it != -1 } ?: 0
        }
        
        /**
         * Get screen by route string
         */
        fun getScreenByRoute(route: String?): Screen? {
            if (route == null) return null
            
            return when {
                // Auth Flow
                route == Splash.route -> Splash
                route == Onboarding.route -> Onboarding
                route == Login.route -> Login
                route == Register.route -> Register
                route == ForgotPassword.route -> ForgotPassword
                route == ProfileSetup.route -> ProfileSetup
                
                // Main Flow
                route == Home.route -> Home
                route == Search.route -> Search
                route == Map.route -> Map
                route == Camera.route -> Camera
                route == Profile.route -> Profile
                
                // Post & Content
                route.startsWith("post_detail/") -> PostDetail
                route == PostCreate.route -> PostCreate
                route.startsWith("post_edit/") -> PostEdit
                route == PostPreview.route -> PostPreview
                
                // User & Social
                route.startsWith("user_profile/") -> UserProfile
                route == EditProfile.route -> EditProfile
                route == ProfilePhoto.route -> ProfilePhoto
                route.startsWith("comments/") -> Comments
                route.startsWith("likes/") -> Likes
                route.startsWith("followers/") -> Followers
                route.startsWith("following/") -> Following
                
                // Search & Discovery
                route.startsWith("search_results") -> SearchResults
                route == SearchFilters.route -> SearchFilters
                route == SearchHistory.route -> SearchHistory
                
                // Premium Features
                route == Premium.route -> Premium
                route.startsWith("paywall/") -> Paywall
                route == PremiumStatus.route -> PremiumStatus
                route.startsWith("purchase_success/") -> PurchaseSuccess
                route.startsWith("post_extension/") -> PostExtension
                
                // Settings
                route == Settings.route -> Settings
                route == NotificationSettings.route -> NotificationSettings
                route == PrivacySettings.route -> PrivacySettings
                route == AccountSettings.route -> AccountSettings
                route == LanguageSettings.route -> LanguageSettings
                route == ThemeSettings.route -> ThemeSettings
                route == About.route -> About
                route == Help.route -> Help
                route == TermsOfService.route -> TermsOfService
                route == PrivacyPolicy.route -> PrivacyPolicy
                
                // Notifications
                route == Notifications.route -> Notifications
                route.startsWith("notification_detail/") -> NotificationDetail
                
                // Moderation
                route.startsWith("report_post/") -> ReportPost
                route.startsWith("report_user/") -> ReportUser
                route.startsWith("report_comment/") -> ReportComment
                route == BlockedUsers.route -> BlockedUsers
                
                // Archive & History
                route == Archive.route -> Archive
                route == PostHistory.route -> PostHistory
                route == ActivityLog.route -> ActivityLog
                
                // Statistics
                route == UserStats.route -> UserStats
                route.startsWith("post_stats/") -> PostStats
                
                // Utility & Media
                route.startsWith("image_viewer") -> ImageViewer
                route.startsWith("image_carousel/") -> ImageCarousel
                route.startsWith("webview") -> WebView
                route == QrScanner.route -> QrScanner
                route.startsWith("share_sheet/") -> ShareSheet
                
                // Error & Maintenance
                route.startsWith("error/") -> Error
                route == NetworkError.route -> NetworkError
                route == Maintenance.route -> Maintenance
                route == ForceUpdate.route -> ForceUpdate
                route.startsWith("permission_denied/") -> PermissionDenied
                
                else -> null
            }
        }
        
        /**
         * Get all argument keys for a screen
         */
        fun getArgumentKeys(screen: Screen): List<String> {
            return when (screen) {
                PostDetail -> listOf(PostDetail.POST_ID_ARG)
                PostEdit -> listOf(PostEdit.POST_ID_ARG)
                UserProfile -> listOf(UserProfile.USER_ID_ARG)
                Comments -> listOf(Comments.POST_ID_ARG)
                Likes -> listOf(Likes.POST_ID_ARG)
                Followers -> listOf(Followers.USER_ID_ARG)
                Following -> listOf(Following.USER_ID_ARG)
                SearchResults -> listOf(SearchResults.QUERY_ARG, SearchResults.CATEGORY_ARG, SearchResults.CITY_ARG)
                Paywall -> listOf(Paywall.SOURCE_ARG)
                PurchaseSuccess -> listOf(PurchaseSuccess.PRODUCT_ID_ARG)
                PostExtension -> listOf(PostExtension.POST_ID_ARG)
                NotificationDetail -> listOf(NotificationDetail.NOTIFICATION_ID_ARG)
                ReportPost -> listOf(ReportPost.POST_ID_ARG)
                ReportUser -> listOf(ReportUser.USER_ID_ARG)
                ReportComment -> listOf(ReportComment.COMMENT_ID_ARG)
                PostStats -> listOf(PostStats.POST_ID_ARG)
                ImageViewer -> listOf(ImageViewer.IMAGE_URL_ARG, ImageViewer.POST_ID_ARG, ImageViewer.INDEX_ARG)
                ImageCarousel -> listOf(ImageCarousel.POST_ID_ARG, ImageCarousel.START_INDEX_ARG)
                WebView -> listOf(WebView.URL_ARG, WebView.TITLE_ARG)
                ShareSheet -> listOf(ShareSheet.POST_ID_ARG)
                Error -> listOf(Error.ERROR_TYPE_ARG)
                PermissionDenied -> listOf(PermissionDenied.PERMISSION_ARG)
                else -> emptyList()
            }
        }
    }
}
