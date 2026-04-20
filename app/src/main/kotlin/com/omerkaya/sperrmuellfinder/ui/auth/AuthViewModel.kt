package com.omerkaya.sperrmuellfinder.ui.auth

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.DeletionStatus
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.model.auth.AuthEvent
import com.omerkaya.sperrmuellfinder.domain.model.auth.AuthNavigation
import com.omerkaya.sperrmuellfinder.domain.model.auth.AuthResult
import com.omerkaya.sperrmuellfinder.domain.model.auth.AuthState
import com.omerkaya.sperrmuellfinder.domain.model.auth.AuthUiState
import com.omerkaya.sperrmuellfinder.domain.repository.UserRepository
import com.omerkaya.sperrmuellfinder.domain.usecase.auth.AuthManager
import com.omerkaya.sperrmuellfinder.domain.usecase.user.PhotoPickerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for authentication screens
 * Manages authentication state, UI state, and user interactions
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val userRepository: UserRepository,
    private val photoPickerUseCase: PhotoPickerUseCase,
    private val logger: Logger
) : ViewModel() {
    private companion object {
        private const val BAN_ERROR_TOKEN = "USER_BANNED"
        private const val DISABLED_ERROR_TOKEN = "ACCOUNT_DISABLED"
    }
    
    // UI State
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    // Auth State
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    // One-time events
    private val _authResult = Channel<AuthResult>()
    val authResult = _authResult.receiveAsFlow()
    
    // Navigation events
    private val _navigationEvent = Channel<AuthNavigation>()
    val navigationEvent = _navigationEvent.receiveAsFlow()
    private var nicknameCheckJob: Job? = null
    
    init {
        // Observe authentication state
        observeAuthState()
        logger.d(Logger.TAG_AUTH, "AuthViewModel initialized")
    }
    
    /**
     * Handle authentication events from UI
     */
    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.Login -> login(event.email, event.password)
            is AuthEvent.LoginWithGoogle -> loginWithGoogle(event.idToken)
            is AuthEvent.Register -> register(
                event.email, event.password, event.confirmPassword,
                event.nickname, event.firstName, event.lastName, event.city, event.birthDate, event.profilePhotoUrl
            )
            is AuthEvent.SelectProfilePhoto -> selectProfilePhoto()
            is AuthEvent.ForgotPassword -> forgotPassword(event.email)
            is AuthEvent.Logout -> logout()
            is AuthEvent.RefreshUser -> refreshUser()
            is AuthEvent.ClearError -> clearError()
        }
    }
    
    /**
     * Update email field
     */
    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(
            email = email,
            emailError = validateEmail(email)
        )
    }
    
    /**
     * Update password field
     */
    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(
            password = password,
            passwordError = validatePassword(password)
        )
    }
    
    /**
     * Update confirm password field
     */
    fun updateConfirmPassword(confirmPassword: String) {
        val currentPassword = _uiState.value.password
        _uiState.value = _uiState.value.copy(
            confirmPassword = confirmPassword,
            confirmPasswordError = validateConfirmPassword(currentPassword, confirmPassword)
        )
    }
    
    /**
     * Update nickname field
     */
    fun updateNickname(nickname: String) {
        nicknameCheckJob?.cancel()
        _uiState.value = _uiState.value.copy(
            nickname = nickname,
            nicknameError = validateNickname(nickname),
            isCheckingNickname = false,
            isNicknameAvailable = null
        )
        if (nickname.length >= 3) {
            checkNicknameAvailability(nickname)
        }
    }
    
    /**
     * Update first name field
     */
    fun updateFirstName(firstName: String) {
        _uiState.value = _uiState.value.copy(
            firstName = firstName,
            firstNameError = validateFirstName(firstName)
        )
    }
    
    /**
     * Update last name field
     */
    fun updateLastName(lastName: String) {
        _uiState.value = _uiState.value.copy(
            lastName = lastName,
            lastNameError = validateLastName(lastName)
        )
    }
    
    /**
     * Update city field
     */
    fun updateCity(city: String) {
        _uiState.value = _uiState.value.copy(
            city = city,
            cityError = validateCity(city)
        )
    }
    
    /**
     * Update birth date field
     */
    fun updateBirthDate(birthDate: String) {
        _uiState.value = _uiState.value.copy(
            birthDate = birthDate,
            birthDateError = validateBirthDate(birthDate)
        )
    }
    
    /**
     * Update profile photo URI (for immediate preview only)
     */
    fun updateProfilePhotoUri(uri: String?) {
        _uiState.value = _uiState.value.copy(
            profilePhotoUri = uri, // Temporary URI for preview
            profilePhotoError = null // Reset error when photo is selected
        )
    }
    
    /**
     * Update profile photo URL (permanent Firebase Storage URL)
     */
    fun updateProfilePhotoUrl(url: String?) {
        _uiState.value = _uiState.value.copy(
            profilePhotoUrl = url, // Permanent Firebase URL
            profilePhotoUri = null // Clear temp URI when permanent URL is set
        )
    }
    
    /**
     * Toggle password visibility
     */
    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            isPasswordVisible = !_uiState.value.isPasswordVisible
        )
    }
    
    /**
     * Toggle confirm password visibility
     */
    fun toggleConfirmPasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            isConfirmPasswordVisible = !_uiState.value.isConfirmPasswordVisible
        )
    }
    
    /**
     * Switch between login and register forms
     */
    fun switchAuthMode() {
        _uiState.value = _uiState.value.copy(
            isLoginForm = !_uiState.value.isLoginForm,
            error = null,
            // Clear form validation errors when switching
            emailError = null,
            passwordError = null,
            confirmPasswordError = null,
            nicknameError = null,
            firstNameError = null,
            lastNameError = null,
            cityError = null
        )
    }
    
    /**
     * Show/hide forgot password form
     */
    fun toggleForgotPassword() {
        _uiState.value = _uiState.value.copy(
            showForgotPassword = !_uiState.value.showForgotPassword,
            error = null
        )
    }
    
    /**
     * Login with email and password
     * Includes ban check guard
     */
    private fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                logger.d(Logger.TAG_AUTH, "Attempting login")
                
                // Reset state
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null,
                    isLoginSuccessful = false
                )
                
                // Clear any previous auth state
                _authState.value = AuthState.Loading
                
                when (val result = authManager.login(email, password)) {
                    is Result.Success -> {
                        logger.i(Logger.TAG_AUTH, "Login successful, checking ban status")
                        var deletionAutoHandled = false
                        
                        // Wait for auth state to be updated
                        var retryCount = 0
                        var isAuthenticated = false
                        
                        while (retryCount < 3 && !isAuthenticated) {
                            when (val refreshResult = authManager.refreshUser()) {
                                is Result.Success -> {
                                    val user = refreshResult.data

                                    if (!deletionAutoHandled) {
                                        autoCancelDeletionIfPending()
                                        deletionAutoHandled = true
                                    }
                                    
                                    // ✅ BAN CHECK GUARD - 3-Tier System
                                    if (user.isBanned) {
                                        logger.w(Logger.TAG_AUTH, "User is banned: ${user.uid}")
                                        if (user.isCurrentlyBanned()) {
                                            // Log out immediately
                                            authManager.logout()
                                            
                                            // Update state
                                            _authState.value = AuthState.Unauthenticated
                                            _uiState.value = _uiState.value.copy(
                                                isLoading = false,
                                                error = BAN_ERROR_TOKEN,
                                                isLoginSuccessful = false,
                                                currentUser = null
                                            )
                                            
                                            _authResult.send(AuthResult.Error(BAN_ERROR_TOKEN))
                                            return@launch
                                        }
                                    }
                                    
                                    // No ban or ban expired - proceed with login
                                    // Update auth state first
                                    _authState.value = AuthState.Authenticated(user)
                                    
                                    // Then update UI state
                                    _uiState.value = _uiState.value.copy(
                                        isLoading = false,
                                        isLoginSuccessful = true,
                                        currentUser = user
                                    )
                                    
                                    isAuthenticated = true
                                }
                                is Result.Error -> {
                                    retryCount++
                                    if (retryCount < 3) {
                                        logger.w(Logger.TAG_AUTH, "Retry $retryCount: Auth state not ready, waiting...")
                                        kotlinx.coroutines.delay(500L * retryCount)
                                    }
                                }
                                is Result.Loading -> {
                                    // Continue waiting
                                    kotlinx.coroutines.delay(100L)
                                }
                            }
                        }
                        
                        if (isAuthenticated) {
                            // Send success result
                            _authResult.send(AuthResult.Success)
                            
                            // Finally navigate
                            _navigationEvent.send(AuthNavigation.NavigateToHome)
                        } else {
                            logger.e(Logger.TAG_AUTH, "Failed to verify auth state after login")
                            _authState.value = AuthState.Error(Exception("Failed to verify authentication"))
                            _authResult.send(AuthResult.Error("Login failed: Could not verify authentication"))
                        }
                    }
                    is Result.Error -> {
                        logger.e(Logger.TAG_AUTH, "Login failed", result.exception)
                        val errorMessage = result.exception.message ?: "Login failed"
                        val displayMessage = normalizeAuthError(errorMessage)
                        
                        // Update auth state
                        _authState.value = AuthState.Error(result.exception)
                        
                        // Update UI state
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = displayMessage,
                            isLoginSuccessful = false
                        )
                        
                        // Send error result
                        _authResult.send(AuthResult.Error(displayMessage))
                    }
                    is Result.Loading -> {
                        logger.d(Logger.TAG_AUTH, "Login in progress")
                        // Loading state is already handled
                    }
                }
            } catch (e: Exception) {
                logger.e(Logger.TAG_AUTH, "Login error", e)
                val displayMessage = normalizeAuthError(e.message ?: "Login failed")
                
                // Update auth state
                _authState.value = AuthState.Error(e)
                
                // Update UI state
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = displayMessage,
                    isLoginSuccessful = false
                )
                
                // Send error result
                _authResult.send(AuthResult.Error(displayMessage))
            }
        }
    }

    private suspend fun autoCancelDeletionIfPending() {
        try {
            val deletionStatus = userRepository.getAccountDeletionStatus().firstOrNull()
            if (deletionStatus?.status == DeletionStatus.PENDING && deletionStatus.canCancel()) {
                when (val cancelResult = userRepository.cancelAccountDeletion()) {
                    is Result.Success -> logger.i(Logger.TAG_AUTH, "Pending account deletion auto-cancelled after login")
                    is Result.Error -> logger.w(Logger.TAG_AUTH, "Auto-cancel deletion failed: ${cancelResult.exception.message}")
                    is Result.Loading -> Unit
                }
            }
        } catch (e: Exception) {
            logger.w(Logger.TAG_AUTH, "Auto-cancel deletion check skipped: ${e.message}")
        }
    }
    
    /**
     * Login with Google
     */
    private fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            try {
                logger.d(Logger.TAG_AUTH, "Attempting Google login")
                
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                when (val result = authManager.loginWithGoogle(idToken)) {
                    is Result.Success -> {
                        logger.i(Logger.TAG_AUTH, "Google login successful")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoginSuccessful = true
                        )
                        _authResult.send(AuthResult.Success)
                        _navigationEvent.send(AuthNavigation.NavigateToHome)
                    }
                    is Result.Error -> {
                        logger.e(Logger.TAG_AUTH, "Google login failed", result.exception)
                        val displayMessage = normalizeAuthError(result.exception.message ?: "Google login failed")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = displayMessage
                        )
                        _authResult.send(AuthResult.Error(displayMessage))
                    }
                    is Result.Loading -> {
                        // Loading state is already handled by setting isLoading = true above
                        logger.d(Logger.TAG_AUTH, "Google login in progress")
                    }
                }
            } catch (e: Exception) {
                logger.e(Logger.TAG_AUTH, "Google login error", e)
                val displayMessage = normalizeAuthError(e.message ?: "Google login failed")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = displayMessage
                )
                _authResult.send(AuthResult.Error(displayMessage))
            }
        }
    }
    
    /**
     * Register new user
     */
    private fun register(
        email: String,
        password: String,
        confirmPassword: String,
        nickname: String,
        firstName: String,
        lastName: String,
        city: String,
        birthDate: String,
        profilePhotoUrl: String? // Firebase Storage download URL
    ) {
        viewModelScope.launch {
            try {
                logger.d(Logger.TAG_AUTH, "Attempting registration")
                
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                if (_uiState.value.isNicknameAvailable == false) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        nicknameError = "Nickname is already taken"
                    )
                    _authResult.send(AuthResult.Error("Nickname is already taken"))
                    return@launch
                }

                when (val result = authManager.register(email, password, confirmPassword, nickname, firstName, lastName, city, birthDate, profilePhotoUrl)) {
                    is Result.Success -> {
                        logger.i(Logger.TAG_AUTH, "Registration successful")
                        
                        // Update user profile with photo URL if available (photo already uploaded)
                        val currentPhotoUrl = _uiState.value.profilePhotoUrl
                        if (!currentPhotoUrl.isNullOrBlank() && result.data.uid.isNotBlank()) {
                            viewModelScope.launch {
                                try {
                                    val updateResult = photoPickerUseCase.updateProfilePhotoUrl(result.data.uid, currentPhotoUrl)
                                    when (updateResult) {
                                        is Result.Success -> {
                                            logger.i(Logger.TAG_AUTH, "✅ Profile photo URL updated in Firestore")
                                        }
                                        is Result.Error -> {
                                            logger.e(Logger.TAG_AUTH, "❌ Failed to update photo URL in Firestore", updateResult.exception)
                                        }
                                        is Result.Loading -> {
                                            // Update in progress
                                        }
                                    }
                                } catch (e: Exception) {
                                    logger.e(Logger.TAG_AUTH, "❌ Error updating profile photo URL", e)
                                }
                            }
                        }
                        
                        // Clear any existing auth state
                        _authState.value = AuthState.Loading
                        
                        // Auto-login after successful registration
                        when (val loginResult = authManager.login(email, password)) {
                            is Result.Success -> {
                                logger.i(Logger.TAG_AUTH, "Auto-login after registration successful")
                                
                                // Update auth state first
                                _authState.value = AuthState.Authenticated(loginResult.data)
                                
                                // Then update UI state
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    isRegistrationSuccessful = true,
                                    isLoginSuccessful = true,
                                    currentUser = loginResult.data
                                )
                                
                    // Önce başarı sonucunu gönder
                    _authResult.send(AuthResult.Success)

                    // Biraz bekle (UI'ın güncellenmesi için)
                    kotlinx.coroutines.delay(300)

                    // WellDone ekranına yönlendir
                    _navigationEvent.send(AuthNavigation.NavigateToWellDone)

                    // Biraz daha bekle (animasyonun başlaması için)
                    kotlinx.coroutines.delay(300)
                            }
                            is Result.Error -> {
                                logger.e(Logger.TAG_AUTH, "Auto-login after registration failed", loginResult.exception)
                                
                                // Update auth state
                                _authState.value = AuthState.Unauthenticated
                                
                                // Update UI state
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    isRegistrationSuccessful = true,
                                    isLoginSuccessful = false,
                                    currentUser = null
                                )
                                
                                // Send success result for registration
                                _authResult.send(AuthResult.Success)
                                
                                // Navigate to login
                                _navigationEvent.send(AuthNavigation.NavigateToLogin)
                            }
                            is Result.Loading -> {
                                // Loading state is already handled
                            }
                        }
                    }
                    is Result.Error -> {
                        logger.e(Logger.TAG_AUTH, "Registration failed", result.exception)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.exception.message
                        )
                        _authResult.send(AuthResult.Error(result.exception.message ?: "Registration failed"))
                    }
                    is Result.Loading -> {
                        // Loading state is already handled by setting isLoading = true above
                        logger.d(Logger.TAG_AUTH, "Registration in progress")
                    }
                }
            } catch (e: Exception) {
                logger.e(Logger.TAG_AUTH, "Registration error", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
                _authResult.send(AuthResult.Error(e.message ?: "Registration failed"))
            }
        }
    }
    
    /**
     * Send password reset email
     */
    private fun forgotPassword(email: String) {
        viewModelScope.launch {
            try {
                logger.d(Logger.TAG_AUTH, "Sending password reset email")
                
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                when (val result = authManager.forgotPassword(email)) {
                    is Result.Success -> {
                        logger.i(Logger.TAG_AUTH, "Password reset email sent")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isPasswordResetSent = true,
                            showForgotPassword = false
                        )
                        _authResult.send(AuthResult.PasswordResetSent)
                    }
                    is Result.Error -> {
                        logger.e(Logger.TAG_AUTH, "Password reset failed", result.exception)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.exception.message
                        )
                        _authResult.send(AuthResult.Error(result.exception.message ?: "Password reset failed"))
                    }
                    is Result.Loading -> {
                        // Loading state is already handled by setting isLoading = true above
                        logger.d(Logger.TAG_AUTH, "Password reset in progress")
                    }
                }
            } catch (e: Exception) {
                logger.e(Logger.TAG_AUTH, "Password reset error", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
                _authResult.send(AuthResult.Error(e.message ?: "Password reset failed"))
            }
        }
    }
    
    /**
     * Logout current user
     */
    private fun logout() {
        viewModelScope.launch {
            try {
                logger.d(Logger.TAG_AUTH, "Logging out user")
                
                when (val result = authManager.logout()) {
                    is Result.Success -> {
                        logger.i(Logger.TAG_AUTH, "Logout successful")
                        _navigationEvent.send(AuthNavigation.NavigateToLogin)
                    }
                    is Result.Error -> {
                        logger.e(Logger.TAG_AUTH, "Logout failed", result.exception)
                        _authResult.send(AuthResult.Error(result.exception.message ?: "Logout failed"))
                    }
                    is Result.Loading -> {
                        // Logout loading state - typically very quick
                        logger.d(Logger.TAG_AUTH, "Logout in progress")
                    }
                }
            } catch (e: Exception) {
                logger.e(Logger.TAG_AUTH, "Logout error", e)
                _authResult.send(AuthResult.Error(e.message ?: "Logout failed"))
            }
        }
    }
    
    /**
     * Refresh current user data
     */
    private fun refreshUser() {
        viewModelScope.launch {
            try {
                logger.d(Logger.TAG_AUTH, "Refreshing user data")
                
                when (val result = authManager.refreshUser()) {
                    is Result.Success -> {
                        logger.i(Logger.TAG_AUTH, "User refresh successful")
                        _authState.value = AuthState.Authenticated(result.data)
                    }
                    is Result.Error -> {
                        logger.e(Logger.TAG_AUTH, "User refresh failed", result.exception)
                        _authResult.send(AuthResult.Error(result.exception.message ?: "Refresh failed"))
                    }
                    is Result.Loading -> {
                        // User refresh loading state
                        logger.d(Logger.TAG_AUTH, "User refresh in progress")
                        _authState.value = AuthState.Loading
                    }
                }
            } catch (e: Exception) {
                logger.e(Logger.TAG_AUTH, "User refresh error", e)
                _authResult.send(AuthResult.Error(e.message ?: "Refresh failed"))
            }
        }
    }
    
    /**
     * Clear error state
     */
    private fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun isBanError(message: String): Boolean {
        val normalized = message.lowercase(Locale.ROOT)
        return normalized.contains("user_banned") ||
            normalized.contains("banned") ||
            normalized.contains("blocked")
    }

    private fun isDisabledError(message: String): Boolean {
        val normalized = message.lowercase(Locale.ROOT)
        return normalized.contains("error_user_disabled") ||
            normalized.contains("account_disabled") ||
            normalized.contains("user account has been disabled") ||
            normalized.contains("disabled")
    }

    private fun normalizeAuthError(message: String): String {
        return when {
            isBanError(message) -> BAN_ERROR_TOKEN
            isDisabledError(message) -> DISABLED_ERROR_TOKEN
            else -> message
        }
    }
    
    /**
     * Clear login form state
     */
    fun clearLoginForm() {
        _uiState.value = _uiState.value.copy(
            email = "",
            password = "",
            emailError = null,
            passwordError = null,
            error = null,
            isLoading = false,
            isLoginSuccessful = false
        )
    }
    
    /**
     * Observe authentication state from domain layer
     */
    private fun observeAuthState() {
        viewModelScope.launch {
            authManager.getAuthState().collect { authState ->
                _authState.value = authState
                
                // Update UI state based on auth state
                when (authState) {
                    is AuthState.Loading -> {
                        _uiState.value = _uiState.value.copy(
                            isAuthenticated = false,
                            currentUser = null
                        )
                    }
                    is AuthState.Authenticated -> {
                        _uiState.value = _uiState.value.copy(
                            isAuthenticated = true,
                            currentUser = authState.user,
                            isLoading = false
                        )
                    }
                    is AuthState.Unauthenticated -> {
                        _uiState.value = _uiState.value.copy(
                            isAuthenticated = false,
                            currentUser = null,
                            isLoading = false
                        )
                    }
                    is AuthState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isAuthenticated = false,
                            currentUser = null,
                            isLoading = false,
                            error = authState.exception.message
                        )
                    }
                }
            }
        }
    }
    
    // Validation functions
    private fun validateEmail(email: String): String? {
        return when {
            email.isBlank() -> "Email cannot be empty"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Invalid email format"
            else -> null
        }
    }
    
    private fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Password cannot be empty"
            password.length < 6 -> "Password must be at least 6 characters"
            password.length > 128 -> "Password cannot exceed 128 characters"
            !password.any { it.isLetter() } || !password.any { it.isDigit() } -> 
                "Password must contain at least one letter and one number"
            else -> null
        }
    }
    
    private fun validateConfirmPassword(password: String, confirmPassword: String): String? {
        return when {
            confirmPassword.isBlank() -> "Confirm password cannot be empty"
            password != confirmPassword -> "Passwords do not match"
            else -> null
        }
    }
    
    private fun validateNickname(nickname: String): String? {
        return when {
            nickname.isBlank() -> "Nickname cannot be empty"
            nickname.length < 2 -> "Nickname must be at least 2 characters"
            nickname.length > 20 -> "Nickname cannot exceed 20 characters"
            !Regex("^[\\p{L}0-9._-]+$").matches(nickname.trim()) ->
                "Nickname can only contain letters, numbers, dots, underscores and hyphens"
            else -> null
        }
    }
    
    private fun validateFirstName(firstName: String): String? {
        return when {
            firstName.isBlank() -> "First name cannot be empty"
            firstName.length < 2 -> "First name must be at least 2 characters"
            firstName.length > 50 -> "First name cannot exceed 50 characters"
            !Regex("^[\\p{L}\\p{M}\\s'-]+$").matches(firstName.trim()) ->
                "First name can only contain letters and spaces"
            else -> null
        }
    }
    
    private fun validateLastName(lastName: String): String? {
        return when {
            lastName.isBlank() -> "Last name cannot be empty"
            lastName.length < 2 -> "Last name must be at least 2 characters"
            lastName.length > 50 -> "Last name cannot exceed 50 characters"
            !Regex("^[\\p{L}\\p{M}\\s'-]+$").matches(lastName.trim()) ->
                "Last name can only contain letters and spaces"
            else -> null
        }
    }
    
    private fun validateCity(city: String): String? {
        return when {
            city.isBlank() -> "City cannot be empty"
            city.length < 2 -> "City name must be at least 2 characters"
            city.length > 100 -> "City name cannot exceed 100 characters"
            !Regex("^[\\p{L}\\p{M}0-9\\s'\\-.,]+$").matches(city.trim()) ->
                "City contains invalid characters"
            else -> null
        }
    }
    
    /**
     * Validate birth date field (DD.MM.YYYY format)
     */
    private fun validateBirthDate(birthDate: String): String? {
        return when {
            birthDate.isBlank() -> "Birth date cannot be empty"
            !Regex("^\\d{2}\\.\\d{2}\\.\\d{4}$").matches(birthDate) -> "Invalid format. Use DD.MM.YYYY"
            else -> {
                try {
                    val parts = birthDate.split(".")
                    val day = parts[0].toInt()
                    val month = parts[1].toInt()
                    val year = parts[2].toInt()
                    
                    // Basic validation
                    when {
                        year < 1900 || year > 2010 -> "Invalid year. Must be between 1900-2010"
                        month < 1 || month > 12 -> "Invalid month. Must be between 1-12"
                        day < 1 || day > 31 -> "Invalid day. Must be between 1-31"
                        else -> null
                    }
                } catch (e: Exception) {
                    "Invalid date format"
                }
            }
        }
    }
    
    /**
     * Handle profile photo selection
     */
    private fun selectProfilePhoto() {
        // Reset error state when photo picker is opened
        _uiState.value = _uiState.value.copy(
            profilePhotoError = null
        )
        logger.d(Logger.TAG_AUTH, "Profile photo selection requested")
    }
    
    /**
     * Handle selected photo from picker
     * NEW FLOW: Immediate preview + Firebase upload + URL storage
     */
    fun onPhotoSelected(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                logger.d(Logger.TAG_AUTH, "🖼️ Processing selected photo with new secure flow")
                
                // Set loading state
                _uiState.value = _uiState.value.copy(isLoading = true)

                // Copy picker URI to app cache first to avoid transient permission issues with Glide/media picker URIs
                val safeLocalUri = copyUriToCache(context, uri)
                
                // 1) IMMEDIATE PREVIEW - Set temp URI for immediate Glide display
                _uiState.value = _uiState.value.copy(
                    profilePhotoUri = safeLocalUri.toString(), // App-owned local URI for reliable preview
                    profilePhotoError = null
                )
                
                // 2) PROCESS AND UPLOAD TO FIREBASE STORAGE
                val processedImageResult = com.omerkaya.sperrmuellfinder.core.util.ImageUtils.processProfilePhoto(context, safeLocalUri)
                
                when (processedImageResult) {
                    is Result.Success -> {
                        // Create temp file for upload
                        val tempFile = java.io.File.createTempFile("processed_photo", ".jpg", context.cacheDir)
                        tempFile.writeBytes(processedImageResult.data)
                        val tempUri = android.net.Uri.fromFile(tempFile)
                        
                        // 3) UPLOAD TO FIREBASE STORAGE IMMEDIATELY
                        val currentUserId = authManager.getCurrentUserId()
                        if (currentUserId != null) {
                            // Upload with existing user ID
                            uploadPhotoToFirebase(currentUserId, tempUri)
                        } else {
                            // Generate temp user ID for registration flow
                            val tempUserId = "temp_${System.currentTimeMillis()}"
                            uploadPhotoToFirebase(tempUserId, tempUri)
                        }
                        
                        // Clean up temp file
                        tempFile.delete()
                        
                        logger.i(Logger.TAG_AUTH, "✅ Photo processed and upload started")
                    }
                    is Result.Error -> {
                        logger.e(Logger.TAG_AUTH, "❌ Error processing photo", processedImageResult.exception)
                        _uiState.value = _uiState.value.copy(
                            profilePhotoError = "Error processing photo",
                            profilePhotoUri = null, // Clear temp URI on error
                            isLoading = false
                        )
                    }
                    is Result.Loading -> {
                        // Loading state already set
                    }
                }
            } catch (e: Exception) {
                logger.e(Logger.TAG_AUTH, "❌ Error handling photo selection", e)
                _uiState.value = _uiState.value.copy(
                    profilePhotoError = "Error selecting photo",
                    profilePhotoUri = null, // Clear temp URI on error
                    isLoading = false
                )
            }
        }
    }

    private fun copyUriToCache(context: Context, uri: Uri): Uri {
        val extension = when {
            context.contentResolver.getType(uri)?.contains("png", ignoreCase = true) == true -> ".png"
            else -> ".jpg"
        }
        val localFile = java.io.File.createTempFile("auth_profile_preview_", extension, context.cacheDir)
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Unable to open selected image URI" }
            localFile.outputStream().use { output -> input.copyTo(output) }
        }
        return Uri.fromFile(localFile)
    }
    
    /**
     * Upload photo to Firebase Storage and get download URL
     */
    private suspend fun uploadPhotoToFirebase(userId: String, imageUri: Uri) {
        try {
            logger.d(Logger.TAG_AUTH, "🔄 Uploading photo to Firebase Storage")
            
            val uploadResult = photoPickerUseCase.uploadProfilePhoto(userId, imageUri)
            
            when (uploadResult) {
                is Result.Success -> {
                    val downloadUrl = uploadResult.data
                    logger.i(Logger.TAG_AUTH, "✅ Photo uploaded successfully: $downloadUrl")
                    
                    // 4) STORE DOWNLOAD URL - Replace temp URI with permanent URL
                    _uiState.value = _uiState.value.copy(
                        profilePhotoUrl = downloadUrl, // Permanent Firebase URL
                        profilePhotoUri = null, // Clear temp URI
                        isLoading = false
                    )
                }
                is Result.Error -> {
                    logger.e(Logger.TAG_AUTH, "❌ Photo upload failed", uploadResult.exception)
                    _uiState.value = _uiState.value.copy(
                        profilePhotoError = "Upload failed: ${uploadResult.exception.message}",
                        profilePhotoUri = null, // Clear temp URI on error
                        isLoading = false
                    )
                }
                is Result.Loading -> {
                    logger.d(Logger.TAG_AUTH, "⏳ Photo upload in progress")
                }
            }
        } catch (e: Exception) {
            logger.e(Logger.TAG_AUTH, "❌ Error uploading photo", e)
            _uiState.value = _uiState.value.copy(
                profilePhotoError = "Upload error: ${e.message}",
                profilePhotoUri = null, // Clear temp URI on error
                isLoading = false
            )
        }
    }
    
    /**
     * Handle photo picker error
     */
    fun onPhotoPickerError(error: String) {
        _uiState.value = _uiState.value.copy(
            profilePhotoError = error
        )
        logger.e(Logger.TAG_AUTH, "Photo picker error: $error")
    }
    
    /**
     * Check if nickname is available
     */
    fun checkNicknameAvailability(nickname: String) {
        nicknameCheckJob?.cancel()
        nicknameCheckJob = viewModelScope.launch {
            try {
                val formatError = validateNickname(nickname)
                if (formatError != null) {
                    _uiState.value = _uiState.value.copy(
                        nicknameError = formatError,
                        isCheckingNickname = false,
                        isNicknameAvailable = null
                    )
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    isCheckingNickname = true,
                    isNicknameAvailable = null
                )
                delay(350)
                when (val result = userRepository.isNicknameAvailable(nickname.trim())) {
                    is Result.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isCheckingNickname = false,
                            isNicknameAvailable = result.data,
                            nicknameError = if (result.data) null else "Nickname is already taken"
                        )
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isCheckingNickname = false,
                            isNicknameAvailable = null,
                            nicknameError = result.exception.message ?: "Error checking nickname availability"
                        )
                    }
                    is Result.Loading -> Unit
                }
            } catch (e: Exception) {
                logger.e(Logger.TAG_AUTH, "Error checking nickname availability", e)
                _uiState.value = _uiState.value.copy(
                    isCheckingNickname = false,
                    isNicknameAvailable = null,
                    nicknameError = "Error checking nickname availability"
                )
            }
        }
    }
}
