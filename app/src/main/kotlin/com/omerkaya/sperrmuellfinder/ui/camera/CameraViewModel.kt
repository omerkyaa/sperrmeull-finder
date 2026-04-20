package com.omerkaya.sperrmuellfinder.ui.camera

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import com.omerkaya.sperrmuellfinder.core.model.PostLocation
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.Category
import com.omerkaya.sperrmuellfinder.domain.repository.PostRepository
import com.omerkaya.sperrmuellfinder.domain.usecase.location.GetLocationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val postRepository: PostRepository,
    private val getLocationUseCase: GetLocationUseCase,
    private val logger: Logger
) : ViewModel() {
    
    companion object {
        private const val TAG = "CameraViewModel"
    }
    
    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState

    private var currentPhotoUri: Uri? = null

    init {
        fetchLocation()
    }

    private fun fetchLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLocation = true) }
            
            when (val result = getLocationUseCase()) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            location = result.data,
                            isLoadingLocation = false
                        )
                    }
                }
                is Result.Error -> {
                    logger.e(Logger.TAG_CAMERA, "Error getting location", result.exception)
                    _uiState.update { state ->
                        state.copy(
                            error = CameraError.LOCATION_ERROR,
                            isLoadingLocation = false
                        )
                    }
                }
                is Result.Loading -> {
                    // Already handled by initial update
                }
            }
        }
    }

    fun prepareCameraCapture(onUriReady: (Uri) -> Unit) {
        try {
            logger.d(TAG, "🎯 Preparing camera capture...")
            
            // Ensure cache directory exists
            val cacheDir = context.cacheDir
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
                logger.d(TAG, "📁 Created cache directory: ${cacheDir.absolutePath}")
            }
            
            val photoFile = File(
                cacheDir,
                "camera_photo_${System.currentTimeMillis()}.jpg"
            )
            
            logger.d(TAG, "📸 Creating photo file: ${photoFile.absolutePath}")
            
            // Create the file to ensure it exists
            if (!photoFile.exists()) {
                photoFile.createNewFile()
                logger.d(TAG, "✅ Photo file created successfully")
            }
            
            val photoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            
            logger.d(TAG, "🔗 FileProvider URI created: $photoUri")
            
            currentPhotoUri = photoUri
            onUriReady(photoUri)
            
            logger.i(TAG, "✅ Camera capture prepared successfully")
            
        } catch (e: SecurityException) {
            logger.e(TAG, "🚫 SecurityException preparing camera capture", e)
            _uiState.update { it.copy(error = CameraError.CAPTURE_ERROR) }
        } catch (e: IllegalArgumentException) {
            logger.e(TAG, "❌ IllegalArgumentException - FileProvider configuration issue", e)
            _uiState.update { it.copy(error = CameraError.CAPTURE_ERROR) }
        } catch (e: Exception) {
            logger.e(TAG, "❌ Unexpected error preparing camera capture", e)
            _uiState.update { it.copy(error = CameraError.CAPTURE_ERROR) }
        }
    }

    fun onPhotoTaken() {
        logger.d(TAG, "🎯 Processing captured photo...")
        
        // Guard against duplicate calls
        val uri = currentPhotoUri
        if (uri == null) {
            logger.w(TAG, "⚠️ onPhotoTaken() called but no URI available - ignoring duplicate call")
            return
        }
        
        // Clear URI immediately to prevent duplicate processing
        currentPhotoUri = null
        
        try {
            logger.d(TAG, "📸 Photo URI received: $uri (scheme: ${uri.scheme})")
            
            // Set processing state
            _uiState.update { it.copy(isProcessing = true, error = null) }
                
                // Convert URI to file path
                val photoFile = when {
                    // Handle FileProvider URIs (our own camera captures)
                    uri.toString().contains(".fileprovider/") -> {
                        logger.d(TAG, "📁 Processing FileProvider URI...")
                        
                        // For FileProvider URIs, we can get the file path directly
                        val uriString = uri.toString()
                        val fileName = uriString.substringAfterLast("/")
                        val cacheFile = File(context.cacheDir, fileName)
                        
                        logger.d(TAG, "📂 FileProvider file path: ${cacheFile.absolutePath}")
                        logger.d(TAG, "📂 File exists: ${cacheFile.exists()}, Size: ${if (cacheFile.exists()) cacheFile.length() else "N/A"}")
                        cacheFile
                    }
                    // Handle content URIs (copy to temp file)
                    uri.scheme == "content" -> {
                        logger.d(TAG, "🔄 Processing content URI...")
                        
                        // Check if this is a problematic content URI (but allow FileProvider)
                        if (com.omerkaya.sperrmuellfinder.core.util.ContentUriCleaner.isProblematicContentUri(uri.toString())) {
                            logger.e(TAG, "🚫 Blocked problematic content URI in camera: ${uri.toString().take(50)}...")
                            _uiState.update { it.copy(isProcessing = false, error = CameraError.CAPTURE_ERROR) }
                            return
                        }
                        
                        // For safe content URIs, copy the file
                        try {
                            val inputStream = context.contentResolver.openInputStream(uri)
                                ?: throw IllegalStateException("Cannot open input stream for URI: $uri")
                            
                            val tempFile = File(context.cacheDir, "processed_photo_${System.currentTimeMillis()}.jpg")
                            
                            inputStream.use { input ->
                                tempFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            
                            logger.d(TAG, "✅ Content URI copied to file: ${tempFile.absolutePath}")
                            tempFile
                        } catch (e: SecurityException) {
                            logger.e(TAG, "🚫 SecurityException accessing content URI", e)
                            _uiState.update { it.copy(isProcessing = false, error = CameraError.CAPTURE_ERROR) }
                            return
                        } catch (e: Exception) {
                            logger.e(TAG, "❌ Error copying content URI to file", e)
                            _uiState.update { it.copy(isProcessing = false, error = CameraError.PROCESSING_ERROR) }
                            return
                        }
                    }
                    // Handle file URIs
                    uri.scheme == "file" -> {
                        logger.d(TAG, "📁 Processing file URI...")
                        val filePath = uri.path
                        if (filePath.isNullOrBlank()) {
                            logger.e(TAG, "❌ File URI has no path")
                            _uiState.update { it.copy(isProcessing = false, error = CameraError.CAPTURE_ERROR) }
                            return
                        }
                        File(filePath)
                    }
                    else -> {
                        logger.e(TAG, "❌ Unsupported URI scheme: ${uri.scheme}")
                        _uiState.update { it.copy(isProcessing = false, error = CameraError.CAPTURE_ERROR) }
                        return
                    }
                }
                
                // Validate photo file
                logger.d(TAG, "🔍 Validating photo file: ${photoFile.absolutePath}")
                
                when {
                    !photoFile.exists() -> {
                        logger.e(TAG, "❌ Photo file does not exist: ${photoFile.absolutePath}")
                        _uiState.update { it.copy(isProcessing = false, error = CameraError.CAPTURE_ERROR) }
                        return
                    }
                    photoFile.length() == 0L -> {
                        logger.e(TAG, "❌ Photo file is empty: ${photoFile.absolutePath}")
                        _uiState.update { it.copy(isProcessing = false, error = CameraError.CAPTURE_ERROR) }
                        return
                    }
                    photoFile.length() < 1024 -> { // Less than 1KB
                        logger.w(TAG, "⚠️ Photo file is very small (${photoFile.length()} bytes): ${photoFile.absolutePath}")
                    }
                    else -> {
                        logger.i(TAG, "✅ Photo file validated: ${photoFile.length()} bytes")
                    }
                }
                
                // Check photo limit
                if (_uiState.value.photos.size >= 3) {
                    logger.w(TAG, "⚠️ Maximum photos reached (3)")
                    _uiState.update { it.copy(isProcessing = false, error = CameraError.MAX_PHOTOS) }
                    return
                }
                
                // Add photo to list
                _uiState.update { state ->
                    state.copy(
                        photos = state.photos + photoFile,
                        isProcessing = false,
                        error = null
                    )
                }
                
                logger.i(TAG, "✅ Photo added successfully: ${photoFile.absolutePath} (${photoFile.length()} bytes)")
                
            } catch (e: Exception) {
                logger.e(TAG, "❌ Unexpected error processing photo", e)
                _uiState.update { it.copy(isProcessing = false, error = CameraError.PROCESSING_ERROR) }
            }
    }

    fun onPhotoDeleted(photoToDelete: File) {
            _uiState.update { state ->
                state.copy(
                photos = state.photos.filter { it != photoToDelete },
                    error = null
                )
            }
        
        // Clean up the file
        try {
            if (photoToDelete.exists()) {
                photoToDelete.delete()
            }
        } catch (e: Exception) {
            logger.w(Logger.TAG_CAMERA, "Failed to delete photo file", e)
        }
    }

    fun onShareClicked() {
        // Validate post data before upload
        if (!validatePostData()) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true) }

            try {
                logger.d(Logger.TAG_CAMERA, "Starting post upload process...")
                
                // Upload photos to Firebase Storage with defensive error handling
                val currentState = _uiState.value
                val uploadResults = currentState.photos.mapIndexed { index, photo ->
                    try {
                        logger.d(Logger.TAG_CAMERA, "Uploading photo ${index + 1}/${currentState.photos.size}")
                        when (val result = postRepository.uploadImage(photo)) {
                            is Result.Success<String> -> {
                                logger.d(Logger.TAG_CAMERA, "Photo ${index + 1} uploaded successfully")
                                result.data
                            }
                            is Result.Error -> {
                                logger.e(Logger.TAG_CAMERA, "Photo ${index + 1} upload failed", result.exception)
                                throw result.exception
                            }
                            is Result.Loading -> {
                                logger.w(Logger.TAG_CAMERA, "Photo ${index + 1} upload still in progress")
                                throw IllegalStateException("Upload still in progress")
                            }
                        }
                    } catch (e: SecurityException) {
                        logger.e(Logger.TAG_CAMERA, "Google Play Services SecurityException during upload", e)
                        throw Exception("Google Play Services authentication failed. Please update Google Play Services.")
                    } catch (e: Exception) {
                        logger.e(Logger.TAG_CAMERA, "Unexpected error during photo upload", e)
                        throw e
                    }
                }

                logger.d(Logger.TAG_CAMERA, "All photos uploaded, creating post...")
                
                // Create post in Firestore with defensive error handling
                val createResult = try {
                postRepository.createPost(
                    images = uploadResults,
                    description = currentState.description,
                    location = currentState.location ?: return@launch,
                    city = currentState.location?.city ?: "",
                    categoriesEn = currentState.selectedCategories,
                    categoriesDe = currentState.selectedCategories.mapNotNull { categoryEn ->
                        com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants.CATEGORY_EN_TO_DE[categoryEn]
                    }
                    )
                } catch (e: SecurityException) {
                    logger.e(Logger.TAG_CAMERA, "Google Play Services SecurityException during post creation", e)
                    throw Exception("Google Play Services authentication failed. Please restart the app.")
                } catch (e: Exception) {
                    logger.e(Logger.TAG_CAMERA, "Error creating post", e)
                    throw e
                }

                when (createResult) {
                    is Result.Success -> {
                        logger.i(Logger.TAG_CAMERA, "Post created successfully: ${createResult.data.id}")

                        // Navigate back to Home with immediate post display
                        _uiState.update { it.copy(
                            isUploading = false,
                            navigateToHome = true,
                            navigationEvent = CameraNavigationEvent.NavigateToHome(shouldRefreshFeed = true),
                            photos = emptyList(),
                            error = null
                        )}
                        
                        // Log success for debugging
                        logger.i(Logger.TAG_CAMERA, "Camera post creation completed, navigating to home")
                    }
                    is Result.Error -> {
                        logger.e(Logger.TAG_CAMERA, "Post creation failed", createResult.exception)
                        _uiState.update { it.copy(
                            isUploading = false,
                            error = CameraError.UPLOAD_FAILED
                        )}
                    }
                    is Result.Loading -> {
                        logger.d(Logger.TAG_CAMERA, "Post creation still in progress")
                        // Keep uploading state
                    }
                }

            } catch (e: SecurityException) {
                logger.e(Logger.TAG_CAMERA, "Google Play Services SecurityException", e)
                _uiState.update { 
                    it.copy(
                        isUploading = false,
                        error = CameraError.UPLOAD_ERROR
                    )
                }
            } catch (e: Exception) {
                logger.e(Logger.TAG_CAMERA, "Error uploading post", e)
                _uiState.update { 
                    it.copy(
                        isUploading = false,
                        error = CameraError.UPLOAD_ERROR
                    )
                }
            }
        }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(error = null) }
    }

    fun onNavigated() {
        _uiState.update { it.copy(navigationEvent = null, navigateToHome = false) }
        logger.d(TAG, "🧹 Navigation event cleared")
    }

    /**
     * Toggle category selection (max 3 categories)
     */
    fun toggleCategory(categoryEn: String) {
        _uiState.update { state ->
            val currentCategories = state.selectedCategories.toMutableList()
            
            if (currentCategories.contains(categoryEn)) {
                // Remove category
                currentCategories.remove(categoryEn)
            } else {
                // Add category if under limit
                if (currentCategories.size < com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants.MAX_CATEGORIES_PER_POST) {
                    currentCategories.add(categoryEn)
                } else {
                    // Show error for max categories reached
                    state.copy(error = CameraError.MAX_CATEGORIES)
                    return@update state
                }
            }
            
            state.copy(selectedCategories = currentCategories)
        }
    }

    /**
     * Update post description
     */
    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    /**
     * Validate post data before upload
     */
    private fun validatePostData(): Boolean {
        val state = _uiState.value
        
        return when {
            state.photos.isEmpty() -> {
                _uiState.update { it.copy(error = CameraError.NO_PHOTOS) }
                false
            }
            state.selectedCategories.isEmpty() -> {
                _uiState.update { it.copy(error = CameraError.NO_CATEGORIES) }
                false
            }
            state.location == null -> {
                _uiState.update { it.copy(error = CameraError.LOCATION_ERROR) }
                false
            }
            else -> true
        }
    }
}

data class CameraUiState(
    val photos: List<File> = emptyList(),
    val location: PostLocation? = null,
    val isLoadingLocation: Boolean = false,
    val isProcessing: Boolean = false,
    val isUploading: Boolean = false,
    val navigateToHome: Boolean = false,
    val error: CameraError? = null,
    val navigationEvent: CameraNavigationEvent? = null,
    val selectedCategories: List<String> = emptyList(), // English category IDs
    val description: String = ""
)

enum class CameraError {
    MAX_PHOTOS,
    NO_PHOTOS,
    MAX_CATEGORIES,
    NO_CATEGORIES,
    LOCATION_ERROR,
    CAPTURE_ERROR,
    PROCESSING_ERROR,
    UPLOAD_ERROR,
    UPLOAD_FAILED
}

sealed class CameraNavigationEvent {
    data class NavigateToHome(val shouldRefreshFeed: Boolean = false) : CameraNavigationEvent()
}