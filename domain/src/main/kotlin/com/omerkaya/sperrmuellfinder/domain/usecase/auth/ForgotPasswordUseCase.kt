package com.omerkaya.sperrmuellfinder.domain.usecase.auth

import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ForgotPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    
    /**
     * Send password reset email
     */
    suspend operator fun invoke(email: String): Result<Unit> {
        // Validate email
        when {
            email.isBlank() -> {
                return Result.Error(IllegalArgumentException("Email cannot be empty"))
            }
            !isValidEmail(email) -> {
                return Result.Error(IllegalArgumentException("Invalid email format"))
            }
        }
        
        return try {
            authRepository.sendPasswordResetEmail(email.trim().lowercase())
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
}
