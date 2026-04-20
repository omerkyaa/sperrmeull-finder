package com.omerkaya.sperrmuellfinder.data.repository

import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.data.mapper.toDomainModel
import com.omerkaya.sperrmuellfinder.data.dto.user.UserDto
import com.omerkaya.sperrmuellfinder.data.source.auth.FirebaseAuthDataSource
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AuthRepository
 * Bridges domain layer with Firebase authentication data source
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuthDataSource: FirebaseAuthDataSource,
    private val logger: Logger
) : AuthRepository {
    
    /**
     * Get current user authentication state as Flow
     */
    override fun getCurrentUserFlow(): Flow<User?> {
        return firebaseAuthDataSource.getCurrentUserFlow()
            .map { firebaseUser ->
                if (firebaseUser != null) {
                    try {
                        // Get user data from Firestore with retry mechanism
                        var retryCount = 0
                        var userDto: UserDto? = null
                        
                        while (retryCount < 3 && userDto == null) {
                            userDto = firebaseAuthDataSource.getUserFromFirestore(firebaseUser.uid)
                            if (userDto == null) {
                                retryCount++
                                if (retryCount < 3) {
                                    logger.w(Logger.TAG_AUTH, "Retry $retryCount: User document not found, waiting...")
                                    kotlinx.coroutines.delay(500L * retryCount)
                                }
                            }
                        }
                        
                        if (userDto != null) {
                            userDto.toDomainModel()
                        } else {
                            logger.e(Logger.TAG_AUTH, "User document not found after retries")
                            null
                        }
                    } catch (e: Exception) {
                        logger.e(Logger.TAG_AUTH, "Error getting user document", e)
                        null
                    }
                } else {
                    null
                }
            }
            .catch { exception ->
                logger.e(Logger.TAG_AUTH, "Error in getCurrentUserFlow", exception)
                emit(null)
            }
    }
    
    /**
     * Get current user ID
     */
    override suspend fun getCurrentUserId(): String? {
        return try {
            firebaseAuthDataSource.getCurrentUserId()
        } catch (e: Exception) {
            logger.e(Logger.TAG_AUTH, "Error getting current user ID", e)
            null
        }
    }
    
    /**
     * Login with email and password
     */
    override suspend fun login(email: String, password: String): Result<User> {
        return when (val result = firebaseAuthDataSource.login(email, password)) {
            is Result.Success -> {
                logger.i(Logger.TAG_AUTH, "Login successful in repository")
                Result.Success(result.data.toDomainModel())
            }
            is Result.Error -> {
                logger.e(Logger.TAG_AUTH, "Login failed in repository", result.exception)
                result
            }
            is Result.Loading -> {
                logger.d(Logger.TAG_AUTH, "Login in progress")
                result
            }
        }
    }
    
    /**
     * Register new user with email and password
     */
    override suspend fun register(
        email: String,
        password: String,
        nickname: String,
        firstName: String,
        lastName: String,
        city: String,
        birthDate: String,
        profilePhotoUrl: String? // Firebase Storage download URL
    ): Result<User> {
        try {
            // First check if nickname is available
            when (val nicknameResult = firebaseAuthDataSource.isNicknameAvailable(nickname)) {
                is Result.Success -> {
                    if (!nicknameResult.data) {
                        logger.e(Logger.TAG_AUTH, "Nickname $nickname is already taken")
                        return Result.Error(Exception("Nickname is already taken"))
                    }
                }
                is Result.Error -> {
                    logger.e(Logger.TAG_AUTH, "Error checking nickname availability", nicknameResult.exception)
                    return nicknameResult
                }
                is Result.Loading -> {
                    // Continue with registration
                }
            }
            
            // If nickname is available, proceed with registration
            return when (val result = firebaseAuthDataSource.register(email, password, nickname, firstName, lastName, city, birthDate, profilePhotoUrl)) {
                is Result.Success -> {
                    logger.i(Logger.TAG_AUTH, "Registration successful in repository")
                    Result.Success(result.data.toDomainModel())
                }
                is Result.Error -> {
                    logger.e(Logger.TAG_AUTH, "Registration failed in repository", result.exception)
                    result
                }
                is Result.Loading -> {
                    logger.d(Logger.TAG_AUTH, "Registration in progress")
                    result
                }
            }
        } catch (e: Exception) {
            logger.e(Logger.TAG_AUTH, "Registration error", e)
            return Result.Error(e)
        }
    }
    
    /**
     * Login with Google
     */
    override suspend fun loginWithGoogle(idToken: String): Result<User> {
        return when (val result = firebaseAuthDataSource.loginWithGoogle(idToken)) {
            is Result.Success -> {
                logger.i(Logger.TAG_AUTH, "Google login successful in repository")
                Result.Success(result.data.toDomainModel())
            }
            is Result.Error -> {
                logger.e(Logger.TAG_AUTH, "Google login failed in repository", result.exception)
                result
            }
            is Result.Loading -> {
                logger.d(Logger.TAG_AUTH, "Google login in progress")
                result
            }
        }
    }
    
    /**
     * Send password reset email
     */
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return firebaseAuthDataSource.sendPasswordResetEmail(email)
    }
    
    /**
     * Update password
     */
    override suspend fun updatePassword(newPassword: String): Result<Unit> {
        return firebaseAuthDataSource.updatePassword(newPassword)
    }
    
    /**
     * Update email
     */
    override suspend fun updateEmail(newEmail: String): Result<Unit> {
        return firebaseAuthDataSource.updateEmail(newEmail)
    }
    
    /**
     * Update profile information
     */
    override suspend fun updateProfile(
        displayName: String?,
        photoUrl: String?,
        city: String?
    ): Result<User> {
        return when (val result = firebaseAuthDataSource.updateProfile(displayName, photoUrl, city)) {
            is Result.Success -> {
                logger.i(Logger.TAG_AUTH, "Profile update successful in repository")
                Result.Success(result.data.toDomainModel())
            }
            is Result.Error -> {
                logger.e(Logger.TAG_AUTH, "Profile update failed in repository", result.exception)
                result
            }
            is Result.Loading -> {
                logger.d(Logger.TAG_AUTH, "Profile update in progress")
                result
            }
        }
    }
    
    /**
     * Delete account
     */
    override suspend fun deleteAccount(): Result<Unit> {
        return firebaseAuthDataSource.deleteAccount()
    }
    
    /**
     * Sign out
     */
    override suspend fun signOut(): Result<Unit> {
        return firebaseAuthDataSource.signOut()
    }
    
    /**
     * Check if email is available
     */
    override suspend fun isEmailAvailable(email: String): Result<Boolean> {
        return firebaseAuthDataSource.isEmailAvailable(email)
    }
    
    /**
     * Refresh current user data
     */
    override suspend fun refreshUser(): Result<User> {
        return try {
            val currentUserId = firebaseAuthDataSource.getCurrentUserId()
            if (currentUserId != null) {
                val userDto = firebaseAuthDataSource.getUserFromFirestore(currentUserId)
                if (userDto != null) {
                    logger.i(Logger.TAG_AUTH, "User refresh successful")
                    Result.Success(userDto.toDomainModel())
                } else {
                    logger.e(Logger.TAG_AUTH, "User document not found during refresh")
                    Result.Error(Exception("User not found"))
                }
            } else {
                logger.e(Logger.TAG_AUTH, "No authenticated user during refresh")
                Result.Error(Exception("User not authenticated"))
            }
        } catch (e: Exception) {
            logger.e(Logger.TAG_AUTH, "Error refreshing user", e)
            Result.Error(e)
        }
    }
    
    /**
     * Link authentication provider
     */
    override suspend fun linkProvider(idToken: String): Result<Unit> {
        return try {
            // This would be implemented when we add provider linking functionality
            // For now, return success as it's not a critical feature
            logger.w(Logger.TAG_AUTH, "Provider linking not yet implemented")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(Logger.TAG_AUTH, "Error linking provider", e)
            Result.Error(e)
        }
    }
    
    /**
     * Unlink authentication provider
     */
    override suspend fun unlinkProvider(providerId: String): Result<Unit> {
        return try {
            // This would be implemented when we add provider unlinking functionality
            // For now, return success as it's not a critical feature
            logger.w(Logger.TAG_AUTH, "Provider unlinking not yet implemented")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(Logger.TAG_AUTH, "Error unlinking provider", e)
            Result.Error(e)
        }
    }
    
    /**
     * Get linked providers
     */
    override suspend fun getLinkedProviders(): Result<List<String>> {
        return try {
            // This would be implemented when we add provider management functionality
            // For now, return empty list as it's not a critical feature
            logger.w(Logger.TAG_AUTH, "Provider listing not yet implemented")
            Result.Success(emptyList())
        } catch (e: Exception) {
            logger.e(Logger.TAG_AUTH, "Error getting linked providers", e)
            Result.Error(e)
        }
    }
}
