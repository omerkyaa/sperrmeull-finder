package com.omerkaya.sperrmuellfinder.domain.usecase.auth

import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.model.auth.AuthState
import com.omerkaya.sperrmuellfinder.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central authentication manager for domain layer
 * Coordinates all authentication operations and state management
 */
@Singleton
class AuthManager @Inject constructor(
    private val authRepository: AuthRepository,
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val forgotPasswordUseCase: ForgotPasswordUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) {
    
    /**
     * Get authentication state as Flow
     */
    fun getAuthState(): Flow<AuthState> {
        return getCurrentUserUseCase()
            .map { user ->
                when (user) {
                    null -> AuthState.Unauthenticated
                    else -> AuthState.Authenticated(user)
                }
            }
            .catch { exception ->
                emit(AuthState.Error(exception))
            }
    }
    
    /**
     * Login with email and password
     */
    suspend fun login(email: String, password: String): Result<User> {
        return loginUseCase(email, password)
    }
    
    /**
     * Login with Google
     */
    suspend fun loginWithGoogle(idToken: String): Result<User> {
        return loginUseCase.loginWithGoogle(idToken)
    }
    
    /**
     * Register new user
     */
    suspend fun register(
        email: String,
        password: String,
        confirmPassword: String,
        nickname: String,
        firstName: String,
        lastName: String,
        city: String,
        birthDate: String,
        profilePhotoUrl: String? // Firebase Storage download URL
    ): Result<User> {
        return registerUseCase(email, password, confirmPassword, nickname, firstName, lastName, city, birthDate, profilePhotoUrl)
    }
    
    /**
     * Send password reset email
     */
    suspend fun forgotPassword(email: String): Result<Unit> {
        return forgotPasswordUseCase(email)
    }
    
    /**
     * Logout current user
     */
    suspend fun logout(): Result<Unit> {
        return logoutUseCase()
    }
    
    /**
     * Get current user
     */
    fun getCurrentUser(): Flow<User?> {
        return getCurrentUserUseCase()
    }
    
    /**
     * Get current user ID
     */
    suspend fun getCurrentUserId(): String? {
        return getCurrentUserUseCase.getCurrentUserId()
    }
    
    /**
     * Refresh current user data
     */
    suspend fun refreshUser(): Result<User> {
        return getCurrentUserUseCase.refreshUser()
    }
    
    /**
     * Check if user is authenticated
     */
    suspend fun isAuthenticated(): Boolean {
        return getCurrentUserUseCase.isAuthenticated()
    }
    
    /**
     * Update user profile
     */
    suspend fun updateProfile(
        displayName: String? = null,
        photoUrl: String? = null,
        city: String? = null
    ): Result<User> {
        return try {
            authRepository.updateProfile(displayName, photoUrl, city)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Update password
     */
    suspend fun updatePassword(newPassword: String): Result<Unit> {
        // Validate password
        when {
            newPassword.isBlank() -> {
                return Result.Error(IllegalArgumentException("Password cannot be empty"))
            }
            newPassword.length < 6 -> {
                return Result.Error(IllegalArgumentException("Password must be at least 6 characters"))
            }
            newPassword.length > 128 -> {
                return Result.Error(IllegalArgumentException("Password cannot exceed 128 characters"))
            }
            !isStrongPassword(newPassword) -> {
                return Result.Error(IllegalArgumentException("Password must contain at least one letter and one number"))
            }
        }
        
        return try {
            authRepository.updatePassword(newPassword)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Update email
     */
    suspend fun updateEmail(newEmail: String): Result<Unit> {
        // Validate email
        when {
            newEmail.isBlank() -> {
                return Result.Error(IllegalArgumentException("Email cannot be empty"))
            }
            !isValidEmail(newEmail) -> {
                return Result.Error(IllegalArgumentException("Invalid email format"))
            }
        }
        
        return try {
            authRepository.updateEmail(newEmail.trim().lowercase())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Delete account
     */
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            authRepository.deleteAccount()
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Link authentication provider
     */
    suspend fun linkProvider(idToken: String): Result<Unit> {
        return try {
            authRepository.linkProvider(idToken)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Unlink authentication provider
     */
    suspend fun unlinkProvider(providerId: String): Result<Unit> {
        return try {
            authRepository.unlinkProvider(providerId)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Get linked providers
     */
    suspend fun getLinkedProviders(): Result<List<String>> {
        return try {
            authRepository.getLinkedProviders()
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Simple email validation
     */
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    /**
     * Check if password is strong enough
     */
    private fun isStrongPassword(password: String): Boolean {
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        return hasLetter && hasDigit
    }
}
