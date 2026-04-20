package com.omerkaya.sperrmuellfinder.domain.usecase.user

import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for updating user profile information
 * Handles validation and error management
 */
@Singleton
class UpdateUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val logger: Logger
) {
    
    /**
     * Update user profile with validation
     */
    suspend fun updateProfile(
        displayName: String? = null,
        firstName: String? = null,
        lastName: String? = null,
        city: String? = null,
        photoUrl: String? = null
    ): Result<Unit> {
        return try {
            logger.d(Logger.TAG_DEFAULT, "Updating user profile: displayName=$displayName, firstName=$firstName, lastName=$lastName, city=$city")
            
            // Validate display name
            displayName?.let { name ->
                val validation = validateDisplayName(name)
                if (validation != null) {
                    logger.w(Logger.TAG_DEFAULT, "Display name validation failed: $validation")
                    return Result.Error(Exception(validation))
                }
            }
            
            // Validate first name
            firstName?.let { name ->
                val validation = validateName(name, "First name")
                if (validation != null) {
                    logger.w(Logger.TAG_DEFAULT, "First name validation failed: $validation")
                    return Result.Error(Exception(validation))
                }
            }
            
            // Validate last name
            lastName?.let { name ->
                val validation = validateName(name, "Last name")
                if (validation != null) {
                    logger.w(Logger.TAG_DEFAULT, "Last name validation failed: $validation")
                    return Result.Error(Exception(validation))
                }
            }
            
            // Validate city
            city?.let { cityName ->
                val validation = validateCity(cityName)
                if (validation != null) {
                    logger.w(Logger.TAG_DEFAULT, "City validation failed: $validation")
                    return Result.Error(Exception(validation))
                }
            }
            
            // Update profile
            val result = userRepository.updateUserProfile(
                displayName = displayName,
                firstName = firstName,
                lastName = lastName,
                city = city,
                photoUrl = photoUrl
            )
            when (result) {
                is Result.Success -> {
                    logger.i(Logger.TAG_DEFAULT, "User profile updated successfully")
                    Result.Success(Unit)
                }
                is Result.Error -> {
                    logger.e(Logger.TAG_DEFAULT, "Failed to update user profile", result.exception)
                    Result.Error(result.exception)
                }
                is Result.Loading -> {
                    Result.Loading
                }
            }
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Error updating user profile", e)
            Result.Error(e)
        }
    }
    
    /**
     * Validate display name according to rules.md
     */
    private fun validateDisplayName(displayName: String): String? {
        return when {
            displayName.isBlank() -> "Display name cannot be empty"
            displayName.length < 2 -> "Display name must be at least 2 characters"
            displayName.length > 50 -> "Display name cannot exceed 50 characters"
            !Regex("^[a-zA-Z0-9\\s._-]+$").matches(displayName.trim()) -> 
                "Display name contains invalid characters"
            else -> null
        }
    }
    
    /**
     * Validate name (first name or last name)
     */
    private fun validateName(name: String, fieldName: String): String? {
        return when {
            name.isBlank() -> "$fieldName cannot be empty"
            name.length < 1 -> "$fieldName must be at least 1 character"
            name.length > 50 -> "$fieldName cannot exceed 50 characters"
            !Regex("^[a-zA-ZäöüÄÖÜß\\s.-]+$").matches(name.trim()) -> 
                "$fieldName contains invalid characters"
            else -> null
        }
    }
    
    /**
     * Validate city name
     */
    private fun validateCity(city: String): String? {
        return when {
            city.isBlank() -> "City cannot be empty"
            city.length < 2 -> "City name must be at least 2 characters"
            city.length > 100 -> "City name cannot exceed 100 characters"
            else -> null
        }
    }
}
