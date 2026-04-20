package com.omerkaya.sperrmuellfinder.ui.camera

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.ui.common.ErrorToast
import com.omerkaya.sperrmuellfinder.ui.common.LoadingOverlay
import com.omerkaya.sperrmuellfinder.core.ui.theme.SperrmullPrimary
import com.omerkaya.sperrmuellfinder.domain.model.Category
import com.skydoves.landscapist.glide.GlideImage
import java.io.File

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onNavigateToHome: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var hasRequestedPermissions by remember { mutableStateOf(false) }

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        android.util.Log.d("CameraScreen", "📸 Camera result: success=$success")
        if (success) {
            viewModel.onPhotoTaken()
        } else {
            android.util.Log.w("CameraScreen", "❌ Camera capture failed or was cancelled")
            // Note: We don't show error here as user might have just cancelled
        }
    }

    LaunchedEffect(Unit) {
        android.util.Log.d("CameraScreen", "🎯 CameraScreen launched")
        if (!permissionsState.allPermissionsGranted) {
            android.util.Log.d("CameraScreen", "📋 Requesting camera permissions...")
            hasRequestedPermissions = true
            permissionsState.launchMultiplePermissionRequest()
        }
    }
    
    // Handle permission changes and auto-launch camera (but not if navigating away)
    LaunchedEffect(permissionsState.allPermissionsGranted, uiState.photos.size, uiState.navigationEvent) {
        // Don't auto-launch camera if we're navigating away
        if (uiState.navigationEvent != null) {
            android.util.Log.d("CameraScreen", "🚫 Navigation event detected, skipping auto camera launch")
            return@LaunchedEffect
        }
        
        if (permissionsState.allPermissionsGranted && uiState.photos.isEmpty()) {
            android.util.Log.d("CameraScreen", "✅ Permissions granted, launching camera...")
            viewModel.prepareCameraCapture { uri ->
                android.util.Log.d("CameraScreen", "🚀 Launching camera with URI: $uri")
                cameraLauncher.launch(uri)
            }
        }
    }

    LaunchedEffect(uiState.navigationEvent) {
        when (val event = uiState.navigationEvent) {
            is CameraNavigationEvent.NavigateToHome -> {
                // Navigate to home and trigger refresh if needed
                onNavigateToHome()
            }
            null -> {}
        }
    }

    // Clear one-shot navigation event when leaving camera screen.
    // This avoids re-triggering camera auto-launch race right after share.
    DisposableEffect(Unit) {
        onDispose {
            viewModel.onNavigated()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (permissionsState.allPermissionsGranted) {
            if (uiState.photos.isEmpty()) {
                // Empty state - waiting for first photo
                CameraWaitingState(
                    onTakePhoto = {
                        viewModel.prepareCameraCapture { uri ->
                            cameraLauncher.launch(uri)
                        }
                    }
                )
            } else {
                // Post preview with photos
                PostPreviewContent(
                    uiState = uiState,
                    onAddMorePhotos = {
                        viewModel.prepareCameraCapture { uri ->
                            cameraLauncher.launch(uri)
                        }
                    },
                    onSharePost = viewModel::onShareClicked,
                    onDeletePhoto = viewModel::onPhotoDeleted,
                    onCategoryToggle = viewModel::toggleCategory,
                    onDescriptionChange = viewModel::updateDescription
                )
            }
        } else {
            PermissionsRequest(
                permissionsState = permissionsState,
                hasRequestedPermissions = hasRequestedPermissions,
                onRequestPermission = {
                    hasRequestedPermissions = true
                    permissionsState.launchMultiplePermissionRequest()
                },
                onOpenSettings = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            )
        }

        // Error toast with retry option for capture errors
        uiState.error?.let { error ->
            when (error) {
                CameraError.CAPTURE_ERROR, CameraError.PROCESSING_ERROR -> {
                    // Show error with retry button for camera issues
                    ErrorToastWithRetry(
                        message = when (error) {
                            CameraError.CAPTURE_ERROR -> stringResource(R.string.camera_error_capture_failed)
                            CameraError.PROCESSING_ERROR -> stringResource(R.string.camera_error_processing_failed)
                            else -> stringResource(R.string.camera_error_capture_failed)
                        },
                        onDismiss = viewModel::onErrorShown,
                        onRetry = {
                            viewModel.onErrorShown() // Clear error first
                            viewModel.prepareCameraCapture { uri ->
                                android.util.Log.d("CameraScreen", "🔄 Retrying camera capture with URI: $uri")
                                cameraLauncher.launch(uri)
                            }
                        }
                    )
                }
                else -> {
                    // Regular error toast for other errors
                    ErrorToast(
                        message = when (error) {
                            CameraError.MAX_PHOTOS -> stringResource(R.string.camera_error_max_photos)
                            CameraError.NO_PHOTOS -> stringResource(R.string.camera_error_no_photos)
                            CameraError.MAX_CATEGORIES -> stringResource(R.string.camera_categories_max)
                            CameraError.NO_CATEGORIES -> stringResource(R.string.camera_error_no_categories)
                            CameraError.LOCATION_ERROR -> stringResource(R.string.camera_error_no_location)
                            CameraError.UPLOAD_ERROR -> stringResource(R.string.camera_error_upload_failed)
                            CameraError.UPLOAD_FAILED -> stringResource(R.string.camera_error_upload_failed)
                            else -> stringResource(R.string.camera_error_capture_failed)
                        },
                        onDismiss = viewModel::onErrorShown
                    )
                }
            }
        }

        // Loading overlays
        if (uiState.isProcessing) {
            LoadingOverlay(
                message = stringResource(R.string.camera_processing)
            )
        }
        if (uiState.isUploading) {
            LoadingOverlay(
                message = stringResource(R.string.camera_uploading)
            )
        }
    }
}

@Composable
private fun CameraWaitingState(
    onTakePhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.AddAPhoto,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = SperrmullPrimary.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.camera_take_first_photo),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.camera_take_first_photo_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onTakePhoto,
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = SperrmullPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.AddAPhoto,
                contentDescription = stringResource(R.string.cd_take_photo),
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun PostPreviewContent(
    uiState: CameraUiState,
    onAddMorePhotos: () -> Unit,
    onSharePost: () -> Unit,
    onDeletePhoto: (File) -> Unit = {},
    onCategoryToggle: (String) -> Unit = {},
    onDescriptionChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Instagram-style post preview card - Full screen height
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Photos section
                InstagramStylePhotoSection(
                    photos = uiState.photos,
                    onDeletePhoto = onDeletePhoto,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                // Description input
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = onDescriptionChange,
                    label = { Text(stringResource(R.string.camera_description_hint)) },
                    placeholder = { Text(stringResource(R.string.camera_description_placeholder)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    maxLines = 3,
                    singleLine = false,
                    shape = RoundedCornerShape(12.dp)
                )

                // Category selection section
                CategorySelectionSection(
                    selectedCategories = uiState.selectedCategories,
                    onCategoryToggle = onCategoryToggle,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // Location section
                uiState.location?.let { location ->
                    LocationSection(
                        location = location,
                        modifier = Modifier.padding(16.dp)
                    )
                } ?: run {
                    if (uiState.isLoadingLocation) {
                        LoadingLocationSection(
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // Action buttons
                ActionButtonsSection(
                    canAddMore = uiState.photos.size < 3,
                    onAddMorePhotos = onAddMorePhotos,
                    onSharePost = onSharePost,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InstagramStylePhotoSection(
    photos: List<File>,
    onDeletePhoto: (File) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize() // Use all available space
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        if (photos.isNotEmpty()) {
            if (photos.size == 1) {
                // Single photo
                GlideImage(
                    imageModel = { photos.first() },
                    modifier = Modifier.fillMaxSize(),
                    imageOptions = com.skydoves.landscapist.ImageOptions(
                        contentScale = ContentScale.Crop
                    )
                )
                // Delete button for single photo
                Button(
                    onClick = { onDeletePhoto(photos.first()) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(36.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black.copy(alpha = 0.7f)
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cd_delete_photo),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                // Multiple photos with pager
                val pagerState = rememberPagerState(
                    initialPage = 0,
                    pageCount = { photos.size }
                )

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    GlideImage(
                        imageModel = { photos[page] },
                        modifier = Modifier.fillMaxSize(),
                        imageOptions = com.skydoves.landscapist.ImageOptions(
                            contentScale = ContentScale.Crop
                        )
                    )
                }

                // Photo count indicator (current/total)
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1}/${photos.size}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Page indicators (dots)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(photos.size) { index ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == index) {
                                        Color.White
                                    } else {
                                        Color.White.copy(alpha = 0.5f)
                                    }
                                )
                        )
                    }
                }
                
                // Delete button (top right) - delete current photo in pager
                Button(
                    onClick = { 
                        onDeletePhoto(photos[pagerState.currentPage])
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(36.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black.copy(alpha = 0.7f)
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cd_delete_photo),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationSection(
    location: com.omerkaya.sperrmuellfinder.core.model.PostLocation,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.LocationOn,
            contentDescription = null,
            tint = SperrmullPrimary,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = buildString {
                location.address?.let { address ->
                    append(address)
                    if (location.city != null) append(", ")
                }
                location.city?.let { city ->
                    append(city)
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun LoadingLocationSection(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = SperrmullPrimary
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = stringResource(R.string.camera_location_detecting),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun ActionButtonsSection(
    canAddMore: Boolean,
    onAddMorePhotos: () -> Unit,
    onSharePost: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Add more photos button (left)
        if (canAddMore) {
            Button(
                onClick = onAddMorePhotos,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SperrmullPrimary // Changed to blue (SperrmullPrimary)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AddAPhoto,
                    contentDescription = stringResource(R.string.cd_add_more_photos),
                    tint = Color.White, // White icon on blue background
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                    Text(
                    text = stringResource(R.string.camera_add_more),
                    color = Color.White, // White text on blue background
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Spacer(modifier = Modifier.width(1.dp))
        }
        
        // Share button (right) - Using Send icon
        Button(
            onClick = onSharePost,
            colors = ButtonDefaults.buttonColors(
                containerColor = SperrmullPrimary
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Send, // Changed to Send icon
                contentDescription = stringResource(R.string.cd_share_post_action),
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.camera_share),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionsRequest(
    permissionsState: MultiplePermissionsState,
    hasRequestedPermissions: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPermanentlyDenied = hasRequestedPermissions && permissionsState.permissions.any { permissionState ->
        when (val status = permissionState.status) {
            is PermissionStatus.Denied -> !status.shouldShowRationale
            PermissionStatus.Granted -> false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.AddAPhoto,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = SperrmullPrimary.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.camera_permission_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.camera_permission_description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(
                containerColor = SperrmullPrimary
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(
                text = stringResource(R.string.camera_permission_grant),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        if (isPermanentlyDenied) {
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onOpenSettings) {
                Text(
                    text = stringResource(R.string.permission_settings),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// =============================================================================
// CATEGORY SELECTION SECTION
// =============================================================================

@Composable
private fun CategorySelectionSection(
    selectedCategories: List<String>,
    onCategoryToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Section title
        Text(
            text = stringResource(R.string.select_category_title),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Category selection info
        Text(
            text = stringResource(
                R.string.categories_selected,
                selectedCategories.size
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Scrollable category chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(Category.POPULAR_CATEGORIES) { category ->
                CategoryChip(
                    category = category,
                    isSelected = selectedCategories.contains(category.name.lowercase()),
                    onToggle = { onCategoryToggle(category.name.lowercase()) },
                    enabled = selectedCategories.size < 3 || selectedCategories.contains(category.name.lowercase())
                )
            }
        }
        
        // Warning if max categories reached
        if (selectedCategories.size >= 3) {
            Text(
                text = stringResource(R.string.category_limit_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun CategoryChip(
    category: Category,
    isSelected: Boolean,
    onToggle: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        Color(android.graphics.Color.parseColor(category.colorHex))
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val textColor = if (isSelected) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    Surface(
        modifier = modifier
            .clickable(enabled = enabled) { 
                if (enabled) onToggle() 
            },
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        border = if (!isSelected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        } else null,
        shadowElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Category icon (placeholder - you can add actual icons)
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        color = if (isSelected) Color.White.copy(alpha = 0.3f) else Color.Transparent,
                        shape = CircleShape
                    )
            )
            
            // Category name
            Text(
                text = stringResource(
                    id = when (category.nameResKey) {
                        "category_furniture" -> R.string.category_furniture
                        "category_electronics" -> R.string.category_electronics
                        "category_appliances" -> R.string.category_kitchen // Using kitchen as appliances
                        "category_clothing" -> R.string.category_clothing
                        "category_books_media" -> R.string.category_books
                        "category_toys_games" -> R.string.category_toys
                        "category_sports_outdoor" -> R.string.category_sports
                        "category_home_garden" -> R.string.category_garden
                        "category_art_crafts" -> R.string.category_art
                        else -> R.string.category_other
                    }
                ),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = textColor
            )
        }
    }
}

// =============================================================================
// ERROR HANDLING COMPONENTS
// =============================================================================

@Composable
private fun ErrorToastWithRetry(
    message: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Retry button
            TextButton(
                onClick = onRetry,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text(
                    text = stringResource(R.string.retry),
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Dismiss button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.dismiss),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
