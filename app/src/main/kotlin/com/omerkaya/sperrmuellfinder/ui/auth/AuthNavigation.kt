package com.omerkaya.sperrmuellfinder.ui.auth

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.omerkaya.sperrmuellfinder.ui.ban.BannedScreen

/**
 * Authentication navigation routes
 */
object AuthRoutes {
    const val LOGIN_MODAL = "login_modal"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val WELL_DONE = "well_done"
    const val BANNED = "banned"
    const val EMAIL_INPUT = "email_input"
    const val PASSWORD_CREATION = "password_creation/{email}"
    const val PROFILE_SETUP = "profile_setup/{email}/{password}"
    
    fun passwordCreationRoute(email: String) = "password_creation/$email"
    fun profileSetupRoute(email: String, password: String) = "profile_setup/$email/$password"
}

/**
 * Modern authentication navigation host
 * Features: Multi-step registration flow, animated transitions
 */
@Composable
fun AuthNavHost(
    navController: NavHostController = rememberNavController(),
    onNavigateToHome: () -> Unit,
    onNavigateToBanned: () -> Unit = {},
    onGoogleSignIn: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = AuthRoutes.LOGIN_MODAL
    ) {
        // Login Modal - Initial screen
        composable(AuthRoutes.LOGIN_MODAL) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(AuthRoutes.REGISTER)
                },
                onNavigateToHome = onNavigateToHome,
                onNavigateToBanned = {
                    navController.navigate(AuthRoutes.BANNED)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(AuthRoutes.FORGOT_PASSWORD)
                }
            )
        }
        
        // Register Screen
        composable(AuthRoutes.REGISTER) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onNavigateToWellDone = {
                    navController.navigate(AuthRoutes.WELL_DONE) {
                        // Clear back stack so user can't go back to register after success
                        popUpTo(AuthRoutes.REGISTER) { inclusive = true }
                    }
                }
            )
        }
        
        // Well Done Success Screen
        composable(AuthRoutes.WELL_DONE) {
            WellDoneScreen(
                onTimeout = {
                    onNavigateToHome()
                }
            )
        }
        
        // Forgot Password Screen
        composable(AuthRoutes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(AuthRoutes.BANNED) {
            BannedScreen(
                banReason = null,
                onAcknowledge = {
                    navController.navigate(AuthRoutes.LOGIN_MODAL) {
                        popUpTo(AuthRoutes.LOGIN_MODAL) { inclusive = true }
                    }
                },
                onContactSupport = onNavigateToBanned
            )
        }
        
        // Email Input - Step 1 of registration
        composable(AuthRoutes.EMAIL_INPUT) {
            EmailInputScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPassword = { email ->
                    navController.navigate(AuthRoutes.passwordCreationRoute(email))
                }
            )
        }
        
        // Password Creation - Step 2 of registration
        composable(
            route = AuthRoutes.PASSWORD_CREATION,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            
            PasswordCreationScreen(
                email = email,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToProfile = { validatedEmail, password ->
                    navController.navigate(AuthRoutes.profileSetupRoute(validatedEmail, password))
                }
            )
        }
        
        // Profile Setup - Step 3 of registration (final)
        composable(
            route = AuthRoutes.PROFILE_SETUP,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType },
                navArgument("password") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val password = backStackEntry.arguments?.getString("password") ?: ""
            
            ProfileSetupScreen(
                email = email,
                password = password,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToHome = onNavigateToHome
            )
        }
    }
}
