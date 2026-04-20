package com.omerkaya.sperrmuellfinder.domain.usecase

import android.net.Uri
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.PhotoEntity
import com.omerkaya.sperrmuellfinder.domain.model.ProcessingStatus
import com.omerkaya.sperrmuellfinder.domain.repository.CameraRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Date
import java.util.UUID
import javax.inject.Inject

/**
 * Use case for taking and processing photos
 */
class TakePhotoUseCase @Inject constructor(
    private val cameraRepository: CameraRepository,
    private val logger: Logger
) {
    
    /**
     * Take and process photo
     */
    suspend operator fun invoke(): Flow<TakePhotoResult> = flow {
        try {
            emit(TakePhotoResult.Loading("Capturing photo..."))
            
            // Check permissions first
            val permissionResult = cameraRepository.checkCameraPermissions()
            if (permissionResult is Result.Error) {
                emit(TakePhotoResult.Error("Camera permission required"))
                return@flow
            }
            
            // Safe cast to Success and check data
            val hasPermission = (permissionResult as? Result.Success)?.data ?: false
            if (!hasPermission) {
                emit(TakePhotoResult.Error("Camera permission required"))
                return@flow
            }
            
            // Capture photo
            val captureResult = cameraRepository.capturePhoto()
            if (captureResult is Result.Error) {
                emit(TakePhotoResult.Error("Failed to capture photo: ${captureResult.exception.message}"))
                return@flow
            }
            
            // Safe cast to Success and extract data
            val photoUri = (captureResult as? Result.Success)?.data
            if (photoUri == null) {
                emit(TakePhotoResult.Error("Failed to capture photo: No photo data received"))
                return@flow
            }
            val photoId = UUID.randomUUID().toString()
            
            // Create initial photo entity
            val photoEntity = PhotoEntity(
                id = photoId,
                localUri = photoUri,
                captureTimestamp = getCurrentTimestamp(),
                processingStatus = ProcessingStatus.PROCESSING
            )
            
            emit(TakePhotoResult.PhotoCaptured(photoEntity))
            
            // Process photo (compression)
            emit(TakePhotoResult.Loading("Processing image..."))
            
            val compressResult = cameraRepository.compressPhoto(photoUri)
            if (compressResult is Result.Error) {
                logger.w(Logger.TAG_CAMERA, "Photo compression failed", compressResult.exception)
                emit(TakePhotoResult.Success(photoEntity.copy(processingStatus = ProcessingStatus.COMPLETED)))
                return@flow
            }
            
            // Safe cast to Success and extract compressed URI
            val compressedUri = (compressResult as? Result.Success)?.data
            if (compressedUri == null) {
                logger.w(Logger.TAG_CAMERA, "Photo compression returned no data")
                emit(TakePhotoResult.Success(photoEntity.copy(processingStatus = ProcessingStatus.COMPLETED)))
                return@flow
            }
            
            val finalPhoto = photoEntity.copy(
                localUri = compressedUri,
                processingStatus = ProcessingStatus.COMPLETED
            )
            
            emit(TakePhotoResult.Success(finalPhoto))
            
        } catch (e: Exception) {
            logger.e(Logger.TAG_CAMERA, "Error in TakePhotoUseCase", e)
            emit(TakePhotoResult.Error("Unexpected error: ${e.message}"))
        }
    }
    
    /**
     * Validate photo before processing
     */
    suspend fun validatePhoto(photoUri: Uri): Result<Boolean> {
        return try {
            cameraRepository.validatePhoto(photoUri)
        } catch (e: Exception) {
            logger.e(Logger.TAG_CAMERA, "Error validating photo", e)
            Result.Error(e)
        }
    }
    
    /**
     * Get photo metadata
     */
    suspend fun getPhotoMetadata(photoUri: Uri): Result<PhotoEntity> {
        return try {
            cameraRepository.getPhotoMetadata(photoUri)
        } catch (e: Exception) {
            logger.e(Logger.TAG_CAMERA, "Error getting photo metadata", e)
            Result.Error(e)
        }
    }
    
    /**
     * Compress photo for optimization
     */
    suspend fun compressPhoto(
        photoUri: Uri,
        maxWidth: Int = 1920,
        maxHeight: Int = 1920,
        quality: Int = 85
    ): Result<Uri> {
        return try {
            cameraRepository.compressPhoto(photoUri, maxWidth, maxHeight, quality)
        } catch (e: Exception) {
            logger.e(Logger.TAG_CAMERA, "Error compressing photo", e)
            Result.Error(e)
        }
    }
    
    /**
     * Get current timestamp in a safe way for all Android API levels
     * Rules.md compliant - Android-compatible timestamp using Date
     */
    private fun getCurrentTimestamp(): Date {
        return Date()
    }
}

/**
 * Result states for take photo operation
 */
sealed class TakePhotoResult {
    data class Loading(val message: String) : TakePhotoResult()
    data class PhotoCaptured(val photo: PhotoEntity) : TakePhotoResult()
    data class Success(val photo: PhotoEntity) : TakePhotoResult()
    data class Error(val message: String) : TakePhotoResult()
}