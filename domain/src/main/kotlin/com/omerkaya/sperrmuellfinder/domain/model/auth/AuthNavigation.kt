package com.omerkaya.sperrmuellfinder.domain.model.auth

/**
 * Navigation events for authentication flow
 */
sealed class AuthNavigation {
    object NavigateToLogin : AuthNavigation()
    object NavigateToRegister : AuthNavigation()
    object NavigateToHome : AuthNavigation()
    object NavigateToWellDone : AuthNavigation()
    object NavigateToForgotPassword : AuthNavigation()
}
