package com.omerkaya.sperrmuellfinder.ui.auth

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.omerkaya.sperrmuellfinder.ui.ban.BannedScreen

/**
 * Navigation host for authentication flow
 */
@Composable
fun AuthNavHost(
    onNavigateToHome: () -> Unit,
    onNavigateToBanned: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") // TODO: Will be implemented in future versions (v2.0.0)
    onGoogleSignIn: () -> Unit = {}
) {
    // TODO: Google Sign-In Integration (Planned for v2.0.0)
    // - Implement OAuth2 flow with Firebase
    // - Add Google Sign-In button to LoginScreen
    // - Handle Google user profile data
    // - Sync with Firestore user document
    // - Add Google account linking in settings
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                onNavigateToRegister = { navController.navigate("register") },
                onNavigateToHome = onNavigateToHome,
                onNavigateToBanned = { navController.navigate("banned") },
                onNavigateToForgotPassword = { navController.navigate("forgot_password") }
            )
        }
        
        composable("register") {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onNavigateToWellDone = { navController.navigate("well_done") }
            )
        }
        
        composable("well_done") {
            WellDoneScreen(
                onTimeout = onNavigateToHome
            )
        }
        
        composable("forgot_password") {
            ForgotPasswordScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("banned") {
            BannedScreen(
                banReason = null,
                onAcknowledge = {
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onContactSupport = {
                    onNavigateToBanned()
                }
            )
        }
    }
}
