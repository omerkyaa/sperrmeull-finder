package com.omerkaya.sperrmuellfinder.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.data.datasource.GoogleMapsDataSource
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants
import com.omerkaya.sperrmuellfinder.domain.model.CategoryEntity
import com.omerkaya.sperrmuellfinder.domain.model.PhotoEntity
import com.omerkaya.sperrmuellfinder.domain.model.ProcessingStatus
import com.omerkaya.sperrmuellfinder.domain.model.UploadStatus
import com.omerkaya.sperrmuellfinder.domain.repository.CameraRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of CameraRepository
 */
@Singleton
class CameraRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val googleMapsDataSource: GoogleMapsDataSource,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val logger: Logger
) : CameraRepository {
    
    private val photoProcessingStates = mutableMapOf<String, MutableStateFlow<PhotoEntity>>()
    private val uploadProgressStates = mutableMapOf<String, MutableStateFlow<Float>>()
    
    override suspend fun capturePhoto(): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            // This would typically integrate with CameraX
            // For now, we'll create a placeholder implementation
            logger.d(Logger.TAG_CAMERA, "Capturing photo...")
            
            // Create a temporary file for the captured photo
            val photoFile = File(context.cacheDir, "captured_${System.currentTimeMillis()}.jpg")
            val photoUri = Uri.fromFile(photoFile)
            
            logger.i(Logger.TAG_CAMERA, "Photo captured: $photoUri")
            Result.Success(photoUri)
            
        } catch (e: Exception) {
            logger.e(Logger.TAG_CAMERA, "Error capturing photo", e)
            Result.Error(e)
        }
    }
    
    override suspend fun uploadPhoto(
        photoUri: Uri,
        userId: String,
        postId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            logger.d(Logger.TAG_CAMERA, "Uploading photo to Firebase Storage")
            
            val fileName = "posts/$userId/$postId/${UUID.randomUUID()}.jpg"
            val storageRef = storage.reference.child(fileName)
            
            // Compress photo before upload
            val compressedBytes = compressPhotoToBytes(photoUri)
            
            val uploadTask = storageRef.putBytes(compressedBytes).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()
            
            logger.i(Logger.TAG_CAMERA, "Photo uploaded successfully: $downloadUrl")
            Result.Success(downloadUrl)
            
        } catch (e: Exception) {
            logger.e(Logger.TAG_CAMERA, "Error uploading photo", e)
            Result.Error(e)
        }
    }
    
    override suspend fun uploadPhotos(
        photoUris: List<Uri>,
        userId: String,
        postId: String
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            logger.d(Logger.TAG_CAMERA, "Uploading ${photoUris.size} photos")
            
            val uploadedUrls = mutableListOf<String>()
            val totalPhotos = photoUris.size
            
            photoUris.forEachIndexed { index, uri ->
                val progress = (index.toFloat() / totalPhotos) * 100f
                updateUploadProgress(postId, progress)
                
                val uploadResult = uploadPhoto(uri, userId, postId)
                if (uploadResult is Result.Success) {
                    uploadedUrls.add(uploadResult.data)
                } else {
                    throw Exception("Failed to upload photo ${index + 1}")
                }
            }
            
            updateUploadProgress(postId, 100f)
            logger.i(Logger.TAG_CAMERA, "All photos uploaded successfully")
            Result.Success(uploadedUrls)
            
        } catch (e: Exception) {
            logger.e(Logger.TAG_CAMERA, "Error uploading photos", e)
            Result.Error(e)
        }
    }
    
    override suspend fun savePostMetadata(
        postId: String,
        userId: String,
        imageUrls: List<String>,
        description: String,
        location: Pair<Double, Double>,
        categoryEn: String,
        categoryDe: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            logger.d(Logger.TAG_CAMERA, "Saving post metadata to Firestore")
            logger.d(Logger.TAG_CAMERA, "🎯 DEBUG: postId=$postId, userId=$userId")
            logger.d(Logger.TAG_CAMERA, "🎯 DEBUG: FIELD_OWNER_ID='${FirestoreConstants.FIELD_OWNER_ID}'")
            
            // Fetch current user data from Firestore for denormalized fields
            val userDoc = firestore.collection(FirestoreConstants.COLLECTION_USERS)
                .document(userId)
                .get()
                .await()
            
            val userDisplayName = userDoc.getString(FirestoreConstants.FIELD_DISPLAY_NAME) 
                ?: firebaseAuth.currentUser?.displayName 
                ?: "Unknown User"
            val userPhotoUrl = userDoc.getString(FirestoreConstants.FIELD_PHOTO_URL) 
                ?: firebaseAuth.currentUser?.photoUrl?.toString()
            val userLevel = userDoc.getLong(FirestoreConstants.FIELD_LEVEL)?.toInt() ?: 1
            val isUserPremium = userDoc.getBoolean(FirestoreConstants.FIELD_IS_PREMIUM) ?: false
            
            logger.d(Logger.TAG_CAMERA, "🎯 User data for post: displayName=$userDisplayName, photoUrl=$userPhotoUrl, level=$userLevel, premium=$isUserPremium")
            
            val postData = hashMapOf(
                "id" to postId,
                FirestoreConstants.FIELD_OWNER_ID to userId,
                FirestoreConstants.FIELD_IMAGES to imageUrls,
                "description" to description,
                "location" to hashMapOf(
                    "latitude" to location.first,
                    "longitude" to location.second
                ),
                // Enhanced location data for PostCard display
                "locationStreet" to null, // Camera posts don't have street info initially
                "locationCity" to null,   // Will be reverse geocoded later if needed
                "city" to "",
                "category_en" to listOf(categoryEn),
                "category_de" to listOf(categoryDe),
                FirestoreConstants.FIELD_CREATED_AT to com.google.firebase.Timestamp.now(),
                FirestoreConstants.FIELD_EXPIRES_AT to com.google.firebase.Timestamp(
                    com.google.firebase.Timestamp.now().seconds + (72 * 60 * 60), // 72 hours
                    0 // nanoseconds
                ),
                FirestoreConstants.FIELD_STATUS to "active",
                FirestoreConstants.FIELD_AVAILABILITY_PERCENT to 100,
                FirestoreConstants.FIELD_LIKES_COUNT to 0,
                FirestoreConstants.FIELD_COMMENTS_COUNT to 0,
                FirestoreConstants.FIELD_VIEWS_COUNT to 0,
                FirestoreConstants.FIELD_SHARES_COUNT to 0,
            // Add denormalized user data for immediate display
            "ownerDisplayName" to userDisplayName,
            "ownerPhotoUrl" to userPhotoUrl,
            "ownerLevel" to userLevel,
            "isOwnerPremium" to isUserPremium
            )
            
            logger.d(Logger.TAG_CAMERA, "🎯 DEBUG: postData keys: ${postData.keys}")
            logger.d(Logger.TAG_CAMERA, "🎯 DEBUG: postData['${FirestoreConstants.FIELD_OWNER_ID}'] = '${postData[FirestoreConstants.FIELD_OWNER_ID]}'")
            
            firestore.collection(FirestoreConstants.COLLECTION_POSTS)
                .document(postId)
                .set(postData)
                .await()
            
            logger.i(Logger.TAG_CAMERA, "Post metadata saved successfully")
            Result.Success(Unit)
            
        } catch (e: Exception) {
            logger.e(Logger.TAG_CAMERA, "Error saving post metadata", e)
            Result.Error(e)
        }
    }
    
    override suspend fun getAllCategories(): Result<List<CategoryEntity>> {
        return try {
            val categories = CategoryEntity.getDefaultCategories()
            Result.Success(categories)
        } catch (e: Exception) {
            logger.e(Logger.TAG_CAMERA, "Error getting all categories", e)
            Result.Error(e)
        }
    }
    
    override fun observePhotoProcessing(photoId: String): Flow<PhotoEntity> {
        return photoProcessingStates.getOrPut(photoId) {
            MutableStateFlow(
                createPhotoEntity(
                    id = photoId,
                    localUri = Uri.EMPTY,
                    captureTimestamp = getCurrentTimestamp(),
                    processingStatus = ProcessingStatus.PENDING,
                    uploadStatus = null
                )
            )
        }.asStateFlow()
    }
    
    override fun observeUploadProgress(postId: String): Flow<Float> {
        return uploadProgressStates.getOrPut(postId) {
            MutableStateFlow(0f)
        }.asStateFlow()
    }
    
    override suspend fun deleteLocalPhoto(photoUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = File(photoUri.path ?: return@withContext Result.Error(Exception("Invalid URI")))
            if (file.exists() && file.delete()) {
                logger.d(Logger.TAG_CAMERA, "Local photo deleted: $photoUri")
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Failed to delete file"))
            }
        } catch (e: Exception) {
            logger.e(Logger.TAG_CAMERA, "Error deleting local photo", e)
            Result.Error(e)
        }
    }
    
    override suspend fun getPhotoMetadata(photoUri: Uri): Result<PhotoEntity> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Validate URI
            if (photoUri.path.isNullOrEmpty()) {
                return@withContext Result.Error(Exception("Invalid URI"))
            }
            
            val photoEntity = createPhotoEntity(
                id = UUID.randomUUID().toString(),
                localUri = photoUri,
                captureTimestamp = getCurrentTimestamp(),
                processingStatus = ProcessingStatus.PENDING,
                uploadStatus = UploadStatus.PENDING
            )
            
            Result.Success(photoEntity)
        } catch (e: Exception) {
            logger.e(Logger.TAG_CAMERA, "Error getting photo metadata", e)
            Result.Error(e)
        }
    }
    
    override suspend fun compressPhoto(
        photoUri: Uri,
        maxWidth: Int,
        maxHeight: Int,
        quality: Int
    ): Result<Uri> = withContext(Dispatchers.IO) {
        return@withContext try {
            logger.d(Logger.TAG_CAMERA, "Compressing photo: $photoUri")
            
            val inputStream = context.contentResolver.openInputStream(photoUri)
                ?: return@withContext Result.Error(Exception("Cannot open input stream"))
            
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            // Calculate new dimensions
            val ratio = minOf(
                maxWidth.toFloat() / originalBitmap.width,
                maxHeight.toFloat() / originalBitmap.height
            )
            
            val newWidth = (originalBitmap.width * ratio).toInt()
            val newHeight = (originalBitmap.height * ratio).toInt()
            
            // Resize bitmap
            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
            
            // Save compressed bitmap
            val compressedFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
            FileOutputStream(compressedFile).use { out ->
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            
            // Clean up
            originalBitmap.recycle()
            resizedBitmap.recycle()
            
            val compressedUri = Uri.fromFile(compressedFile)
            logger.i(Logger.TAG_CAMERA, "Photo compressed successfully")
            Result.Success(compressedUri)
            
        } catch (e: Exception) {
            logger.e(Logger.TAG_CAMERA, "Error compressing photo", e)
            Result.Error(e)
        }
    }
    
    override suspend fun validatePhoto(photoUri: Uri): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val inputStream = context.contentResolver.openInputStream(photoUri)
                ?: return@withContext Result.Success(false)
            
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            
            val isValid = options.outWidth > 0 && options.outHeight > 0 &&
                    options.outMimeType?.startsWith("image/") == true
            
            Result.Success(isValid)
        } catch (e: Exception) {
            logger.e(Logger.TAG_CAMERA, "Error validating photo", e)
            Result.Success(false)
        }
    }
    
    override suspend fun getCurrentLocation(): Result<Pair<Double, Double>> {
        return try {
            val locationResult = googleMapsDataSource.getCurrentLocation()
            when (locationResult) {
                is Result.Success -> {
                    val postLocation = locationResult.data
                    val coordinates = Pair(postLocation.latitude, postLocation.longitude)
                    logger.d(Logger.TAG_CAMERA, "Location retrieved: ${coordinates.first}, ${coordinates.second}")
                    Result.Success(coordinates)
                }
                is Result.Error -> {
                    logger.w(Logger.TAG_CAMERA, "Failed to get location", locationResult.exception)
                    Result.Error(locationResult.exception)
                }
                is Result.Loading -> {
                    logger.d(Logger.TAG_CAMERA, "Location request in progress")
                    Result.Loading
                }
            }
        } catch (e: Exception) {
            logger.e(Logger.TAG_CAMERA, "Error getting current location", e)
            Result.Error(e)
        }
    }
    
    override suspend fun checkCameraPermissions(): Result<Boolean> {
        return try {
            val cameraPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            
            Result.Success(cameraPermission)
        } catch (e: Exception) {
            logger.e(Logger.TAG_CAMERA, "Error checking camera permissions", e)
            Result.Error(e)
        }
    }
    
    override suspend fun requestCameraPermissions(): Result<Boolean> {
        // This would typically be handled by the UI layer
        // Return current permission status
        return checkCameraPermissions()
    }
    
    /**
     * Compress photo to byte array for upload
     */
    private suspend fun compressPhotoToBytes(photoUri: Uri): ByteArray = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(photoUri)!!
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        bitmap.recycle()
        
        outputStream.toByteArray()
    }
    
    /**
     * Update upload progress
     */
    private fun updateUploadProgress(postId: String, progress: Float) {
        uploadProgressStates[postId]?.value = progress
    }
    
    /**
     * Update photo processing state
     */
    private fun updatePhotoProcessing(photoId: String, photoEntity: PhotoEntity) {
        photoProcessingStates[photoId]?.value = photoEntity
    }
    
    /**
     * Get current timestamp as Date in a safe way for all Android API levels
     * Rules.md compliant - Android-compatible timestamp generation
     */
    private fun getCurrentTimestamp(): Date {
        return Date()
    }
    
    /**
     * Create PhotoEntity
     * Rules.md compliant - Android-compatible timestamp using Date
     */
    private fun createPhotoEntity(
        id: String,
        localUri: Uri,
        captureTimestamp: Date,
        processingStatus: ProcessingStatus,
        uploadStatus: UploadStatus?
    ): PhotoEntity {
        return PhotoEntity(
            id = id,
            localUri = localUri,
            captureTimestamp = captureTimestamp,
            processingStatus = processingStatus,
            uploadStatus = uploadStatus ?: UploadStatus.PENDING
        )
    }
}