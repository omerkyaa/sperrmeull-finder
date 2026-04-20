package com.omerkaya.sperrmuellfinder.domain.usecase.auth

import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    
    /**
     * Get current user as Flow
     */
    operator fun invoke(): Flow<User?> {
        return authRepository.getCurrentUserFlow()
    }
    
    /**
     * Get current user ID
     */
    suspend fun getCurrentUserId(): String? {
        return authRepository.getCurrentUserId()
    }
    
    /**
     * Refresh current user data
     */
    suspend fun refreshUser(): Result<User> {
        return try {
            authRepository.refreshUser()
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Check if user is authenticated
     */
    suspend fun isAuthenticated(): Boolean {
        return authRepository.getCurrentUserId() != null
    }
}
