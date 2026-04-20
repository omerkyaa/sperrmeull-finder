package com.omerkaya.sperrmuellfinder.domain.usecase.auth

import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    
    /**
     * Register new user with email and password
     */
    suspend operator fun invoke(
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
        // Validate input
        val validationResult = validateRegisterInput(
            email, password, confirmPassword, nickname, firstName, lastName, city, birthDate
        )
        if (validationResult is Result.Error) {
            return validationResult
        }
        
        // Check if email is available
        val emailCheck = authRepository.isEmailAvailable(email.trim().lowercase())
        when (emailCheck) {
            is Result.Error -> return emailCheck
            is Result.Success -> {
                if (!emailCheck.data) {
                    return Result.Error(IllegalArgumentException("Email is already in use"))
                }
            }
            is Result.Loading -> {
                // This shouldn't happen in a suspend function, but handle it gracefully
                return Result.Error(IllegalStateException("Email availability check is still in progress"))
            }
        }
        
        return try {
            authRepository.register(
                email = email.trim().lowercase(),
                password = password,
                nickname = nickname.trim(),
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                city = city.trim(),
                birthDate = birthDate.trim(),
                profilePhotoUrl = profilePhotoUrl
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Validate registration input
     */
    private fun validateRegisterInput(
        email: String,
        password: String,
        confirmPassword: String,
        nickname: String,
        firstName: String,
        lastName: String,
        city: String,
        birthDate: String
    ): Result<Unit> {
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
            password.length > 128 -> {
                return Result.Error(IllegalArgumentException("Password cannot exceed 128 characters"))
            }
            !isStrongPassword(password) -> {
                return Result.Error(IllegalArgumentException("Password must contain at least one letter and one number"))
            }
            password != confirmPassword -> {
                return Result.Error(IllegalArgumentException("Passwords do not match"))
            }
            nickname.isBlank() -> {
                return Result.Error(IllegalArgumentException("Nickname cannot be empty"))
            }
            nickname.length < 3 -> {
                return Result.Error(IllegalArgumentException("Nickname must be at least 3 characters"))
            }
            nickname.length > 20 -> {
                return Result.Error(IllegalArgumentException("Nickname cannot exceed 20 characters"))
            }
            !isValidNickname(nickname) -> {
                return Result.Error(IllegalArgumentException("Nickname can only contain letters, numbers, dots, underscores and hyphens"))
            }
            firstName.isBlank() -> {
                return Result.Error(IllegalArgumentException("First name cannot be empty"))
            }
            firstName.length < 2 -> {
                return Result.Error(IllegalArgumentException("First name must be at least 2 characters"))
            }
            firstName.length > 50 -> {
                return Result.Error(IllegalArgumentException("First name cannot exceed 50 characters"))
            }
            !isValidName(firstName) -> {
                return Result.Error(IllegalArgumentException("First name can only contain letters and spaces"))
            }
            lastName.isBlank() -> {
                return Result.Error(IllegalArgumentException("Last name cannot be empty"))
            }
            lastName.length < 2 -> {
                return Result.Error(IllegalArgumentException("Last name must be at least 2 characters"))
            }
            lastName.length > 50 -> {
                return Result.Error(IllegalArgumentException("Last name cannot exceed 50 characters"))
            }
            !isValidName(lastName) -> {
                return Result.Error(IllegalArgumentException("Last name can only contain letters and spaces"))
            }
            city.isBlank() -> {
                return Result.Error(IllegalArgumentException("City cannot be empty"))
            }
            city.length < 2 -> {
                return Result.Error(IllegalArgumentException("City name must be at least 2 characters"))
            }
            city.length > 100 -> {
                return Result.Error(IllegalArgumentException("City name cannot exceed 100 characters"))
            }
            birthDate.isBlank() -> {
                return Result.Error(IllegalArgumentException("Birth date cannot be empty"))
            }
            !isValidBirthDate(birthDate) -> {
                return Result.Error(IllegalArgumentException("Invalid birth date format. Use DD.MM.YYYY"))
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
    
    /**
     * Check if password is strong enough
     */
    private fun isStrongPassword(password: String): Boolean {
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        return hasLetter && hasDigit
    }
    
    /**
     * Check if nickname contains only valid characters
     */
    private fun isValidNickname(nickname: String): Boolean {
        // Allow letters, numbers, dots, underscores and hyphens
        val validChars = Regex("^[a-zA-Z0-9._-]+$")
        return validChars.matches(nickname.trim())
    }
    
    /**
     * Check if name contains only valid characters (letters and spaces)
     */
    private fun isValidName(name: String): Boolean {
        // Allow letters and spaces only
        val validChars = Regex("^[a-zA-Z\\s]+$")
        return validChars.matches(name.trim())
    }
    
    /**
     * Validate birth date in DD.MM.YYYY format
     */
    private fun isValidBirthDate(birthDate: String): Boolean {
        return try {
            if (!Regex("^\\d{2}\\.\\d{2}\\.\\d{4}$").matches(birthDate)) {
                return false
            }
            
            val parts = birthDate.split(".")
            val day = parts[0].toInt()
            val month = parts[1].toInt()
            val year = parts[2].toInt()
            
            // Basic validation
            year in 1900..2010 && month in 1..12 && day in 1..31
        } catch (e: Exception) {
            false
        }
    }
}
