package com.omerkaya.sperrmuellfinder.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.layout.Arrangement
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.omerkaya.sperrmuellfinder.R
import java.io.File

/**
 * Photo picker helper component for profile photo selection
 * Uses Activity Result API for camera and gallery access
 * Rules.md compliant - Professional permission handling
 */
@Composable
fun PhotoPickerHelper(
    onPhotoSelected: (Uri) -> Unit,
    onError: (String) -> Unit,
    content: @Composable (onClick: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<PhotoAction?>(null) }

    // Create temp file for camera
    val tempFile = remember {
        File.createTempFile("profile_photo", ".jpg", context.cacheDir)
    }
    val tempUri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }

    // Modern Photo Picker launcher (Android 13+)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { 
            Log.d("PhotoPickerHelper", "📷 Photo selected via Photo Picker: ${uri.toString().take(50)}...")
            onPhotoSelected(it) 
        } ?: onError("No image selected")
    }
    
    // Legacy gallery launcher (Android 12 and below)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { 
            Log.d("PhotoPickerHelper", "📷 Photo selected via Gallery: ${uri.toString().take(50)}...")
            onPhotoSelected(it) 
        } ?: onError("No image selected")
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            onPhotoSelected(tempUri)
        } else {
            onError("Camera capture failed")
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingAction?.let { action ->
                when (action) {
                    PhotoAction.CAMERA -> cameraLauncher.launch(tempUri)
                    PhotoAction.GALLERY -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        } else {
                            galleryLauncher.launch("image/*")
                        }
                    }
                }
            }
        } else {
            onError("Permission denied")
        }
        pendingAction = null
    }

    // Permission check function
    fun checkPermissionAndExecute(action: PhotoAction) {
        val permission = if (action == PhotoAction.CAMERA) {
            Manifest.permission.CAMERA
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
        }

        when (ContextCompat.checkSelfPermission(context, permission)) {
            PackageManager.PERMISSION_GRANTED -> {
                when (action) {
                    PhotoAction.CAMERA -> cameraLauncher.launch(tempUri)
                    PhotoAction.GALLERY -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        } else {
                            galleryLauncher.launch("image/*")
                        }
                    }
                }
            }
            else -> {
                pendingAction = action
                permissionLauncher.launch(permission)
            }
        }
    }

    // Content with click handler
    content {
        showDialog = true
    }

    // Photo source selection dialog
    if (showDialog) {
        PhotoSourceDialog(
            onDismiss = { showDialog = false },
            onCameraClick = {
                showDialog = false
                checkPermissionAndExecute(PhotoAction.CAMERA)
            },
            onGalleryClick = {
                showDialog = false
                checkPermissionAndExecute(PhotoAction.GALLERY)
            }
        )
    }
}

/**
 * Dialog for selecting photo source (camera or gallery)
 */
@Composable
private fun PhotoSourceDialog(
    onDismiss: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.change_profile_photo),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Wählen Sie eine Option:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Camera option
                OutlinedButton(
                    onClick = onCameraClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Kamera")
                    }
                }

                // Gallery option
                OutlinedButton(
                    onClick = onGalleryClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Galerie")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * Photo action enum
 */
private enum class PhotoAction {
    CAMERA,
    GALLERY
}
