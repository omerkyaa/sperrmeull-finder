package com.omerkaya.sperrmuellfinder.domain.usecase

import android.net.Uri
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.CategoryEntity
import com.omerkaya.sperrmuellfinder.domain.model.PhotoEntity
import com.omerkaya.sperrmuellfinder.domain.model.UploadStatus
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.repository.CameraRepository
import com.omerkaya.sperrmuellfinder.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

/**
 * Use case for uploading posts with photos to Firebase
 */
class UploadPostUseCase @Inject constructor(
    private val cameraRepository: CameraRepository,
    private val userRepository: UserRepository,
    private val logger: Logger
) {
    
    /**
     * Upload post with photos and metadata
     */
    suspend operator fun invoke(
        photos: List<PhotoEntity>,
        description: String,
        selectedCategory: CategoryEntity,
        location: Pair<Double, Double>? = null
    ): Flow<UploadPostResult> = flow {
        try {
            emit(UploadPostResult.Loading("Preparing upload...", 0f))
            
            // Get current user - getCurrentUser returns Flow<User?>
            val currentUser = userRepository.getCurrentUser().first()
            
            if (currentUser == null) {
                emit(UploadPostResult.Error("User not authenticated"))
                return@flow
            }
            
            val userId = currentUser.uid
            val postId = UUID.randomUUID().toString()
            
            // Get current location if not provided
            val postLocation = location ?: run {
                emit(UploadPostResult.Loading("Getting location...", 10f))
                val locationResult = cameraRepository.getCurrentLocation()
                if (locationResult is Result.Error) {
                    logger.w(Logger.TAG_CAMERA, "Failed to get location, using default", locationResult.exception)
                    Pair(0.0, 0.0) // Default location
                } else {
                    val successLocationResult = locationResult as? Result.Success
                    val locationData = successLocationResult?.data
                    locationData ?: Pair(0.0, 0.0) // Fallback if data is null
                }
            }
            
            // Validate photos
            emit(UploadPostResult.Loading("Validating photos...", 20f))
            val validPhotos = photos.filter { photo ->
                val validationResult = cameraRepository.validatePhoto(photo.localUri)
                validationResult is Result.Success && validationResult.data == true
            }
            
            if (validPhotos.isEmpty()) {
                emit(UploadPostResult.Error("No valid photos to upload"))
                return@flow
            }
            
            // Compress photos for upload
            emit(UploadPostResult.Loading("Optimizing photos...", 30f))
            val compressedPhotoUris = mutableListOf<Uri>()
            
            validPhotos.forEachIndexed { index, photo ->
                val progress = 30f + (index.toFloat() / validPhotos.size) * 20f
                emit(UploadPostResult.Loading("Compressing photo ${index + 1}/${validPhotos.size}...", progress))
                
                val compressResult = cameraRepository.compressPhoto(photo.localUri)
                if (compressResult is Result.Success) {
                    compressedPhotoUris.add(compressResult.data)
                } else {
                    // Use original if compression fails
                    compressedPhotoUris.add(photo.localUri)
                    val errorException = (compressResult as? Result.Error)?.exception
                    logger.w(Logger.TAG_CAMERA, "Photo compression failed, using original", errorException)
                }
            }
            
            // Upload photos to Firebase Storage
            emit(UploadPostResult.Loading("Uploading photos...", 50f))
            val uploadResult = cameraRepository.uploadPhotos(compressedPhotoUris, userId, postId)
            
            if (uploadResult is Result.Error) {
                emit(UploadPostResult.Error("Failed to upload photos: ${uploadResult.exception.message}"))
                return@flow
            }
            
            val successUploadResult = uploadResult as? Result.Success
            val imageUrls = successUploadResult?.data
            if (imageUrls == null) {
                emit(UploadPostResult.Error("Failed to get uploaded image URLs"))
                return@flow
            }
            
            // Save post metadata to Firestore
            emit(UploadPostResult.Loading("Saving post data...", 80f))
            val metadataResult = cameraRepository.savePostMetadata(
                postId = postId,
                userId = userId,
                imageUrls = imageUrls,
                description = description,
                location = postLocation,
                categoryEn = selectedCategory.nameEn,
                categoryDe = selectedCategory.nameDe
            )
            
            if (metadataResult is Result.Error) {
                emit(UploadPostResult.Error("Failed to save post: ${metadataResult.exception.message}"))
                return@flow
            }
            
            // Clean up local files
            emit(UploadPostResult.Loading("Cleaning up...", 90f))
            compressedPhotoUris.forEach { uri ->
                try {
                    cameraRepository.deleteLocalPhoto(uri)
                } catch (e: Exception) {
                    logger.w(Logger.TAG_CAMERA, "Failed to delete local photo", e)
                }
            }
            
            emit(UploadPostResult.Success(postId, imageUrls))
            
        } catch (e: Exception) {
            logger.e(Logger.TAG_CAMERA, "Error in UploadPostUseCase", e)
            emit(UploadPostResult.Error("Unexpected error: ${e.message}"))
        }
    }
    
    /**
     * Get all available categories
     */
    suspend fun getAllCategories(): Result<List<CategoryEntity>> {
        return try {
            cameraRepository.getAllCategories()
        } catch (e: Exception) {
            logger.e(Logger.TAG_CAMERA, "Error getting all categories", e)
            Result.Error(e)
        }
    }
    
    /**
     * Observe upload progress
     */
    fun observeUploadProgress(postId: String): Flow<Float> {
        return cameraRepository.observeUploadProgress(postId)
    }
    
    /**
     * Validate post data before upload
     */
    suspend fun validatePostData(
        photos: List<PhotoEntity>,
        description: String,
        category: CategoryEntity?
    ): Result<Boolean> {
        return try {
            when {
                photos.isEmpty() -> Result.Error(Exception("At least one photo is required"))
                photos.size > 3 -> Result.Error(Exception("Maximum 3 photos allowed"))
                description.isBlank() -> Result.Error(Exception("Description is required"))
                description.length > 500 -> Result.Error(Exception("Description too long (max 500 characters)"))
                category == null -> Result.Error(Exception("Category selection is required"))
                else -> Result.Success(true)
            }
        } catch (e: Exception) {
            logger.e(Logger.TAG_CAMERA, "Error validating post data", e)
            Result.Error(e)
        }
    }
}

/**
 * Result states for upload post operation
 */
sealed class UploadPostResult {
    data class Loading(val message: String, val progress: Float) : UploadPostResult()
    data class Success(val postId: String, val imageUrls: List<String>) : UploadPostResult()
    data class Error(val message: String) : UploadPostResult()
}