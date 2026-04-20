package com.omerkaya.sperrmuellfinder.domain.repository

import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    /**
     * Current user authentication state
     */
    fun getCurrentUserFlow(): Flow<User?>

    /**
     * Get current user ID
     */
    suspend fun getCurrentUserId(): String?

    /**
     * Login with email and password
     */
    suspend fun login(email: String, password: String): Result<User>

    /**
     * Register new user with email and password
     */
    suspend fun register(
        email: String,
        password: String,
        nickname: String,
        firstName: String,
        lastName: String,
        city: String,
        birthDate: String,
        profilePhotoUrl: String? // Firebase Storage download URL
    ): Result<User>

    /**
     * Login with Google
     */
    suspend fun loginWithGoogle(idToken: String): Result<User>

    /**
     * Send password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    /**
     * Update password
     */
    suspend fun updatePassword(newPassword: String): Result<Unit>

    /**
     * Update email
     */
    suspend fun updateEmail(newEmail: String): Result<Unit>

    /**
     * Update profile information
     */
    suspend fun updateProfile(
        displayName: String? = null,
        photoUrl: String? = null,
        city: String? = null
    ): Result<User>

    /**
     * Delete account
     */
    suspend fun deleteAccount(): Result<Unit>

    /**
     * Sign out
     */
    suspend fun signOut(): Result<Unit>

    /**
     * Check if email is available
     */
    suspend fun isEmailAvailable(email: String): Result<Boolean>

    /**
     * Refresh current user data
     */
    suspend fun refreshUser(): Result<User>

    /**
     * Link authentication provider
     */
    suspend fun linkProvider(idToken: String): Result<Unit>

    /**
     * Unlink authentication provider
     */
    suspend fun unlinkProvider(providerId: String): Result<Unit>

    /**
     * Get linked providers
     */
    suspend fun getLinkedProviders(): Result<List<String>>
}
