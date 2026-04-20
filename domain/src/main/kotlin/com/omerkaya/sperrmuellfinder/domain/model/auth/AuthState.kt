package com.omerkaya.sperrmuellfinder.domain.model.auth

import com.omerkaya.sperrmuellfinder.domain.model.User

/**
 * Authentication state representation
 */
sealed class AuthState {
    
    /**
     * Initial state - checking authentication
     */
    data object Loading : AuthState()
    
    /**
     * User is authenticated
     */
    data class Authenticated(val user: User) : AuthState()
    
    /**
     * User is not authenticated
     */
    data object Unauthenticated : AuthState()
    
    /**
     * Authentication error occurred
     */
    data class Error(val exception: Throwable) : AuthState()
}

/**
 * Authentication events from UI
 */
sealed class AuthEvent {
    
    /**
     * Login events
     */
    data class Login(
        val email: String,
        val password: String
    ) : AuthEvent()
    
    data class LoginWithGoogle(
        val idToken: String
    ) : AuthEvent()
    
    /**
     * Registration events
     */
    data class Register(
        val email: String,
        val password: String,
        val confirmPassword: String,
        val nickname: String,
        val firstName: String,
        val lastName: String,
        val city: String,
        val birthDate: String,
        val profilePhotoUrl: String? // Changed from URI to URL (Firebase Storage)
    ) : AuthEvent()
    
    /**
     * Profile photo selection event
     */
    data object SelectProfilePhoto : AuthEvent()
    
    /**
     * Password reset events
     */
    data class ForgotPassword(
        val email: String
    ) : AuthEvent()
    
    /**
     * Logout event
     */
    data object Logout : AuthEvent()
    
    /**
     * Refresh user data
     */
    data object RefreshUser : AuthEvent()
    
    /**
     * Clear error state
     */
    data object ClearError : AuthEvent()
}

/**
 * Authentication UI state for forms
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val currentUser: User? = null,
    val error: String? = null,
    val isLoginForm: Boolean = true, // true = login, false = register
    
    // Form fields
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val nickname: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val city: String = "",
    val birthDate: String = "",
    val profilePhotoUri: String? = null, // Temporary URI for immediate preview
    val profilePhotoUrl: String? = null, // Final Firebase Storage download URL
    
    // Form validation
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val nicknameError: String? = null,
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val cityError: String? = null,
    val birthDateError: String? = null,
    val profilePhotoError: String? = null,
    val isCheckingNickname: Boolean = false,
    val isNicknameAvailable: Boolean? = null,
    
    // UI flags
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val showForgotPassword: Boolean = false,
    val isGoogleSignInAvailable: Boolean = true,
    
    // Success states
    val isLoginSuccessful: Boolean = false,
    val isRegistrationSuccessful: Boolean = false,
    val isPasswordResetSent: Boolean = false
) {
    
    /**
     * Check if login form is valid
     */
    fun isLoginFormValid(): Boolean {
        return email.isNotBlank() && 
               password.isNotBlank() && 
               emailError == null && 
               passwordError == null
    }
    
    /**
     * Check if register form is valid
     */
    fun isRegisterFormValid(): Boolean {
        return email.isNotBlank() && 
               password.isNotBlank() && 
               confirmPassword.isNotBlank() && 
               nickname.isNotBlank() && 
               firstName.isNotBlank() && 
               lastName.isNotBlank() && 
               city.isNotBlank() && 
               birthDate.isNotBlank() && 
               emailError == null && 
               passwordError == null && 
               confirmPasswordError == null && 
               nicknameError == null && 
               firstNameError == null && 
               lastNameError == null && 
               cityError == null &&
               birthDateError == null &&
               isCheckingNickname.not() &&
               isNicknameAvailable != false
    }
    
    /**
     * Check if forgot password form is valid
     */
    fun isForgotPasswordFormValid(): Boolean {
        return email.isNotBlank() && emailError == null
    }
}

/**
 * Authentication result for one-time events
 */
sealed class AuthResult {
    data object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
    data object PasswordResetSent : AuthResult()
}

