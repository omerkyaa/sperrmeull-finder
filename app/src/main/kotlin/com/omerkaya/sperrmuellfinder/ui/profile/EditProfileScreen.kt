package com.omerkaya.sperrmuellfinder.ui.profile

import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.core.ui.theme.SperrmullPrimary
import com.omerkaya.sperrmuellfinder.ui.components.PhotoPickerHelper
import com.omerkaya.sperrmuellfinder.ui.common.SafeGlideImage
import androidx.compose.ui.graphics.Brush
import kotlinx.coroutines.launch

/**
 * 🎯 MODERN EDIT PROFILE SCREEN - Instagram Style
 * Professional profile editing interface
 * Rules.md compliant - Material3 design with real-time Firebase updates
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val profileEvent by viewModel.profileEvent.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Form state
    var displayName by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf<String?>(null) }
    var isFormValid by remember { mutableStateOf(false) }
    var hasChanges by remember { mutableStateOf(false) }

    // Initialize form with current user data
    LaunchedEffect(uiState.user) {
        uiState.user?.let { user ->
            displayName = user.displayName
            firstName = user.firstName
            lastName = user.lastName
            city = user.city ?: ""
            photoUrl = user.photoUrl
        }
    }

    // Validate form and check for changes
    LaunchedEffect(displayName, firstName, lastName, city, photoUrl) {
        val user = uiState.user
        isFormValid = displayName.trim().isNotBlank() && displayName.trim().length >= 2
        
        hasChanges = user?.let {
            displayName.trim() != it.displayName ||
            firstName.trim() != it.firstName ||
            lastName.trim() != it.lastName ||
            city.trim() != (it.city ?: "") ||
            photoUrl != it.photoUrl
        } ?: false
    }

    // Handle profile events
    LaunchedEffect(profileEvent) {
        when (profileEvent) {
            is ProfileEvent.ProfileUpdated -> {
                snackbarHostState.showSnackbar("✅ ${context.getString(R.string.profile_changes_saved)}")
                // Wait a bit to show the success message, then navigate back
                kotlinx.coroutines.delay(1000)
                onNavigateBack()
                viewModel.clearEvent()
            }
            is ProfileEvent.Error -> {
                val errorEvent = profileEvent as ProfileEvent.Error
                snackbarHostState.showSnackbar("❌ ${context.getString(R.string.error)}: ${errorEvent.message}")
                viewModel.clearEvent()
            }
            else -> { /* Handle other events if needed */ }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { /* No title */ },
                navigationIcon = {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                actions = {
                        Button(
                            onClick = {
                                if (isFormValid && hasChanges) {
                                    viewModel.updateProfile(
                                        displayName = displayName.trim().takeIf { it != uiState.user?.displayName && it.isNotBlank() },
                                        firstName = firstName.trim().takeIf { it != uiState.user?.firstName },
                                        lastName = lastName.trim().takeIf { it != uiState.user?.lastName },
                                        city = city.trim().takeIf { it != (uiState.user?.city ?: "") },
                                        photoUrl = photoUrl?.takeIf { it != uiState.user?.photoUrl }
                                    )
                                }
                            },
                        enabled = isFormValid && hasChanges && !uiState.isUpdating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFormValid && hasChanges) SperrmullPrimary else Color.Gray,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (uiState.isUpdating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.save),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black,
                    actionIconContentColor = Color.Black
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                LoadingContent()
            }
            uiState.user != null -> {
                EditProfileContent(
                    modifier = Modifier.padding(paddingValues),
                    user = uiState.user!!,
                    displayName = displayName,
                    firstName = firstName,
                    lastName = lastName,
                    city = city,
                    photoUrl = photoUrl,
                    onDisplayNameChange = { displayName = it },
                    onFirstNameChange = { firstName = it },
                    onLastNameChange = { lastName = it },
                    onCityChange = { city = it },
                    onPhotoUrlChange = { photoUrl = it },
                    onPhotoSelected = { uri: Uri ->
                        viewModel.uploadProfilePhoto(
                            imageUri = uri,
                            onSuccess = { uploadedPhotoUrl: String ->
                                photoUrl = uploadedPhotoUrl
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.photo_picker_upload_success)
                                    )
                                }
                            },
                            onError = { errorMessage: String ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(errorMessage)
                                }
                            }
                        )
                    },
                    onPhotoError = { errorMessage: String ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(errorMessage)
                        }
                    },
                    isUpdating = uiState.isUpdating
                )
            }
            else -> {
                ErrorContent(
                    error = stringResource(R.string.error_loading_profile),
                    onRetry = { viewModel.loadCurrentUserProfile() }
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF), // White
                        Color(0xFFF97316), // Orange
                        Color(0xFF3B82F6), // Blue
                        Color(0xFF06B6D4)  // Cyan
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = SperrmullPrimary,
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.loading_profile),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
            )
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = error,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SperrmullPrimary
                )
            ) {
                Text(
                    text = stringResource(R.string.retry),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun EditProfileContent(
    modifier: Modifier = Modifier,
    user: com.omerkaya.sperrmuellfinder.domain.model.User,
    displayName: String,
    firstName: String,
    lastName: String,
    city: String,
    photoUrl: String?,
    onDisplayNameChange: (String) -> Unit,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onPhotoUrlChange: (String) -> Unit,
    onPhotoSelected: (Uri) -> Unit,
    onPhotoError: (String) -> Unit,
    isUpdating: Boolean
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFAFAFA), // Light gray
                        Color(0xFFFFFFFF), // White
                        Color(0xFFFFF8F0), // Very light orange
                        Color(0xFFF0F8FF), // Very light blue
                        Color(0xFFFFFFFF)  // White
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Profile Picture Section
        ProfilePictureSection(
            currentPhotoUrl = user.photoUrl,
            newPhotoUrl = photoUrl,
            displayName = displayName.ifBlank { user.displayName },
            onPhotoSelected = onPhotoSelected,
            onPhotoError = onPhotoError
        )

        // Form Section
        ProfileFormSection(
            displayName = displayName,
            firstName = firstName,
            lastName = lastName,
            city = city,
            onDisplayNameChange = onDisplayNameChange,
            onFirstNameChange = onFirstNameChange,
            onLastNameChange = onLastNameChange,
            onCityChange = onCityChange,
            enabled = !isUpdating
        )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ProfilePictureSection(
    currentPhotoUrl: String?,
    newPhotoUrl: String?,
    displayName: String,
    onPhotoSelected: (Uri) -> Unit,
    onPhotoError: (String) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Profile Picture with Instagram-style overlay
        PhotoPickerHelper(
            onPhotoSelected = onPhotoSelected,
            onError = onPhotoError
        ) { onClick ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.clickable { onClick() }
            ) {
            // Main profile image
            SafeGlideImage(
                imageUrl = newPhotoUrl ?: currentPhotoUrl,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                contentDescription = stringResource(R.string.profile_picture),
                errorPlaceholder = {
                    // Fallback avatar
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color.Gray),
                        contentAlignment = Alignment.Center
                    ) {
                        if (displayName.isNotBlank()) {
                            Text(
                                text = displayName.first().uppercase(),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 48.sp
                                ),
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(60.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            )

            // Overlay with camera icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = stringResource(R.string.change_profile_photo),
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.change_profile_photo),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = Color.Black
        )
    }
}
}

@Composable
private fun ProfileFormSection(
    displayName: String,
    firstName: String,
    lastName: String,
    city: String,
    onDisplayNameChange: (String) -> Unit,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    enabled: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Profile Section Header
        Text(
            text = stringResource(R.string.profile),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            ),
            color = Color.Black
        )

        // Name Field
        ModernTextField(
            value = displayName,
            onValueChange = onDisplayNameChange,
            label = stringResource(R.string.name),
            enabled = enabled,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            isError = displayName.isNotBlank() && displayName.length < 2,
            supportingText = if (displayName.isNotBlank() && displayName.length < 2) {
                stringResource(R.string.name_too_short)
            } else null
        )

        // First Name Field
        ModernTextField(
            value = firstName,
            onValueChange = onFirstNameChange,
            label = "First Name",
            enabled = enabled,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            )
        )

        // Last Name Field
        ModernTextField(
            value = lastName,
            onValueChange = onLastNameChange,
            label = "Last Name",
            enabled = enabled,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            )
        )

        // City Field
        ModernTextField(
            value = city,
            onValueChange = onCityChange,
            label = "City",
            enabled = enabled,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
            )
        )

    }
}

@Composable
private fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false,
    supportingText: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = Color.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.fillMaxWidth(),
            enabled = enabled,
            keyboardOptions = keyboardOptions,
            isError = isError,
            trailingIcon = trailingIcon,
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SperrmullPrimary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                cursorColor = SperrmullPrimary,
                focusedContainerColor = Color.White.copy(alpha = 0.9f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.8f)
            )
        )
        
        if (supportingText != null) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) Color.Red else Color.Gray,
                modifier = Modifier.padding(top = 4.dp, start = 16.dp)
            )
        }
    }
}
