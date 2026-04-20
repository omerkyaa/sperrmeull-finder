package com.omerkaya.sperrmuellfinder.domain.usecase.user

import android.net.Uri
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for handling profile photo operations
 * Rules.md compliant - Clean Architecture pattern
 */
@Singleton
class PhotoPickerUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    
    /**
     * Upload profile photo to Firebase Storage and get download URL
     * @param userId User ID for the photo
     * @param imageUri Local image URI to upload
     * @return Result with download URL or error
     */
    suspend fun uploadProfilePhoto(
        userId: String,
        imageUri: Uri
    ): Result<String> {
        return try {
            userRepository.uploadProfilePhoto(userId, imageUri)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Update user's profile photo URL in Firestore
     * @param userId User ID to update
     * @param photoUrl Download URL from Firebase Storage
     * @return Result indicating success or failure
     */
    suspend fun updateProfilePhotoUrl(
        userId: String,
        photoUrl: String
    ): Result<Unit> {
        return try {
            userRepository.updateProfilePhotoUrl(userId, photoUrl)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Complete profile photo update process
     * 1. Upload image to Firebase Storage
     * 2. Update user profile with download URL
     * @param userId User ID
     * @param imageUri Local image URI
     * @return Result with final download URL
     */
    suspend fun updateUserProfilePhoto(
        userId: String,
        imageUri: Uri
    ): Result<String> {
        return when (val uploadResult = uploadProfilePhoto(userId, imageUri)) {
            is Result.Success -> {
                val photoUrl = uploadResult.data
                when (val updateResult = updateProfilePhotoUrl(userId, photoUrl)) {
                    is Result.Success -> Result.Success(photoUrl)
                    is Result.Error -> updateResult
                    is Result.Loading -> Result.Loading
                }
            }
            is Result.Error -> uploadResult
            is Result.Loading -> uploadResult
        }
    }
}
