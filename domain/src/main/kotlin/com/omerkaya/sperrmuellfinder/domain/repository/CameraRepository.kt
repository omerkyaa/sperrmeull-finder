package com.omerkaya.sperrmuellfinder.domain.repository

import android.net.Uri
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.CategoryEntity
import com.omerkaya.sperrmuellfinder.domain.model.PhotoEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for camera and photo operations
 */
interface CameraRepository {
    
    /**
     * Capture a photo and return the local URI
     */
    suspend fun capturePhoto(): Result<Uri>
    
    /**
     * Upload processed photo to Firebase Storage
     */
    suspend fun uploadPhoto(
        photoUri: Uri,
        userId: String,
        postId: String
    ): Result<String>
    
    /**
     * Upload multiple photos
     */
    suspend fun uploadPhotos(
        photoUris: List<Uri>,
        userId: String,
        postId: String
    ): Result<List<String>>
    
    /**
     * Save post metadata to Firestore
     */
    suspend fun savePostMetadata(
        postId: String,
        userId: String,
        imageUrls: List<String>,
        description: String,
        location: Pair<Double, Double>,
        categoryEn: String,
        categoryDe: String
    ): Result<Unit>
    
    /**
     * Get all available categories
     */
    suspend fun getAllCategories(): Result<List<CategoryEntity>>
    
    /**
     * Observe photo processing status
     */
    fun observePhotoProcessing(photoId: String): Flow<PhotoEntity>
    
    /**
     * Observe upload progress
     */
    fun observeUploadProgress(postId: String): Flow<Float>
    
    /**
     * Delete local photo file
     */
    suspend fun deleteLocalPhoto(photoUri: Uri): Result<Unit>
    
    /**
     * Get photo metadata
     */
    suspend fun getPhotoMetadata(photoUri: Uri): Result<PhotoEntity>
    
    /**
     * Compress photo for upload optimization
     */
    suspend fun compressPhoto(
        photoUri: Uri,
        maxWidth: Int = 1920,
        maxHeight: Int = 1920,
        quality: Int = 85
    ): Result<Uri>
    
    /**
     * Validate photo before processing
     */
    suspend fun validatePhoto(photoUri: Uri): Result<Boolean>
    
    /**
     * Get current location for post
     */
    suspend fun getCurrentLocation(): Result<Pair<Double, Double>>
    
    /**
     * Check camera permissions
     */
    suspend fun checkCameraPermissions(): Result<Boolean>
    
    /**
     * Request camera permissions
     */
    suspend fun requestCameraPermissions(): Result<Boolean>
}