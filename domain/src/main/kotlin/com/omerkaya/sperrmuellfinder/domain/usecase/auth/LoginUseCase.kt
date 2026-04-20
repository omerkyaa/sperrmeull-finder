package com.omerkaya.sperrmuellfinder.domain.usecase.auth

import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    
    /**
     * Login with email and password
     */
    suspend operator fun invoke(email: String, password: String): Result<User> {
        // Validate input
        val validationResult = validateLoginInput(email, password)
        if (validationResult is Result.Error) {
            return validationResult
        }
        
        return try {
            authRepository.login(email.trim().lowercase(), password)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Login with Google
     */
    suspend fun loginWithGoogle(idToken: String): Result<User> {
        if (idToken.isBlank()) {
            return Result.Error(IllegalArgumentException("ID token cannot be empty"))
        }
        
        return try {
            authRepository.loginWithGoogle(idToken)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Validate login input
     */
    private fun validateLoginInput(email: String, password: String): Result<Unit> {
        when {
            email.isBlank() -> {
                return Result.Error(IllegalArgumentException("Email cannot be empty"))
            }
            !isValidEmail(email) -> {
                return Result.Error(IllegalArgumentException("Invalid email format"))
            }
            password.isBlank() -> {
                return Result.Error(IllegalArgumentException("Password cannot be empty"))
            }
            password.length < 6 -> {
                return Result.Error(IllegalArgumentException("Password must be at least 6 characters"))
            }
        }
        return Result.Success(Unit)
    }
    
    /**
     * Simple email validation
     */
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
