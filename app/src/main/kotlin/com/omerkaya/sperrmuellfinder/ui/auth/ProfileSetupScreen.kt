package com.omerkaya.sperrmuellfinder.ui.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.core.ui.theme.SperrmullPrimary
import com.omerkaya.sperrmuellfinder.domain.model.auth.AuthEvent
import com.omerkaya.sperrmuellfinder.domain.model.auth.AuthResult

/**
 * Profile setup screen - Final step of registration
 * Features: Name and city input, progress indicator, registration completion
 */
@Composable
fun ProfileSetupScreen(
    email: String,
    password: String,
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val firstNameFocusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    var nicknameInput by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(uiState.nickname))
    }
    var firstNameInput by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(uiState.firstName))
    }
    var lastNameInput by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(uiState.lastName))
    }
    var cityInput by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(uiState.city))
    }
    
    // Animation states
    val contentAlpha = remember { Animatable(0f) }
    val progressAlpha = remember { Animatable(0f) }
    
    // Animate content appearance
    LaunchedEffect(Unit) {
        // Set email and password in viewmodel
        viewModel.updateEmail(email)
        viewModel.updatePassword(password)
        
        contentAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        )
        progressAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(300, delayMillis = 200)
        )
        
        // Auto-focus first name field
        firstNameFocusRequester.requestFocus()
    }
    
    // Handle auth results
    LaunchedEffect(Unit) {
        viewModel.authResult.collect { result ->
            when (result) {
                is AuthResult.Success -> {
                    // Navigation is handled by navigation event
                }
                is AuthResult.Error -> {
                    snackbarHostState.showSnackbar(result.message)
                }
                else -> {
                    // Handle other results
                }
            }
        }
    }
    
    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { navigation ->
            when (navigation) {
                is com.omerkaya.sperrmuellfinder.domain.model.auth.AuthNavigation.NavigateToHome -> {
                    onNavigateToHome()
                }
                else -> {
                    // Handle other navigation events
                }
            }
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .imePadding()
                .alpha(contentAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar with Back Button and Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Gray.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Gray
                    )
                }
                
                // Progress Indicator
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .alpha(progressAlpha.value),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Custom progress bar using Box and background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.Gray.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(1f) // 100% progress (final step)
                                .fillMaxHeight()
                                .background(SperrmullPrimary)
                        )
                    }
                }
                
                // Spacer to balance the layout
                Spacer(modifier = Modifier.size(40.dp))
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Title
            Text(
                text = stringResource(R.string.auth_profile_setup_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Form Fields
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Nickname Field
                Column {
                    Text(
                        text = stringResource(R.string.auth_nickname),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = nicknameInput,
                        onValueChange = { value ->
                            nicknameInput = value
                            viewModel.updateNickname(value.text)
                            if (value.text.length >= 3) {
                                viewModel.checkNicknameAvailability(value.text)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(firstNameFocusRequester),
                        placeholder = { 
                            Text(
                                text = stringResource(R.string.auth_nickname),
                                color = Color.Gray.copy(alpha = 0.6f)
                            ) 
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(),
                        isError = uiState.nicknameError != null,
                        supportingText = uiState.nicknameError?.let { error ->
                            {
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (uiState.nicknameError != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                SperrmullPrimary
                            },
                            unfocusedBorderColor = if (uiState.nicknameError != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                Color.Gray.copy(alpha = 0.3f)
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
                
                // First Name Field
                Column {
                    Text(
                        text = stringResource(R.string.auth_first_name),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = firstNameInput,
                        onValueChange = { value ->
                            firstNameInput = value
                            viewModel.updateFirstName(value.text)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { 
                            Text(
                                text = stringResource(R.string.auth_first_name),
                                color = Color.Gray.copy(alpha = 0.6f)
                            ) 
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(),
                        isError = uiState.firstNameError != null,
                        supportingText = uiState.firstNameError?.let { error ->
                            {
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (uiState.firstNameError != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                SperrmullPrimary
                            },
                            unfocusedBorderColor = if (uiState.firstNameError != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                Color.Gray.copy(alpha = 0.3f)
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
                
                // Last Name Field
                Column {
                    Text(
                        text = stringResource(R.string.auth_last_name),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = lastNameInput,
                        onValueChange = { value ->
                            lastNameInput = value
                            viewModel.updateLastName(value.text)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { 
                            Text(
                                text = stringResource(R.string.auth_last_name),
                                color = Color.Gray.copy(alpha = 0.6f)
                            ) 
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(),
                        isError = uiState.lastNameError != null,
                        supportingText = uiState.lastNameError?.let { error ->
                            {
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (uiState.lastNameError != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                SperrmullPrimary
                            },
                            unfocusedBorderColor = if (uiState.lastNameError != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                Color.Gray.copy(alpha = 0.3f)
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
                
                // Birth Date Field
                Column {
                    Text(
                        text = stringResource(R.string.auth_birth_date),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = uiState.birthDate,
                        onValueChange = viewModel::updateBirthDate,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { 
                            Text(
                                text = stringResource(R.string.auth_date_placeholder),
                                color = Color.Gray.copy(alpha = 0.6f)
                            ) 
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(),
                        isError = uiState.birthDateError != null,
                        supportingText = uiState.birthDateError?.let { error ->
                            {
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (uiState.birthDateError != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                SperrmullPrimary
                            },
                            unfocusedBorderColor = if (uiState.birthDateError != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                Color.Gray.copy(alpha = 0.3f)
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
                
                // City Field
                Column {
                    Text(
                        text = stringResource(R.string.auth_city),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = cityInput,
                        onValueChange = { value ->
                            cityInput = value
                            viewModel.updateCity(value.text)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { 
                            Text(
                                text = stringResource(R.string.auth_city),
                                color = Color.Gray.copy(alpha = 0.6f)
                            ) 
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(),
                        isError = uiState.cityError != null,
                        supportingText = uiState.cityError?.let { error ->
                            {
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (uiState.cityError != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                SperrmullPrimary
                            },
                            unfocusedBorderColor = if (uiState.cityError != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                Color.Gray.copy(alpha = 0.3f)
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Finish Button
            Button(
                onClick = {
                    viewModel.onEvent(
                        AuthEvent.Register(
                            email = email,
                            password = password,
                            confirmPassword = password,
                            nickname = uiState.nickname,
                            firstName = uiState.firstName,
                            lastName = uiState.lastName,
                            city = uiState.city,
                            birthDate = uiState.birthDate,
                            profilePhotoUrl = uiState.profilePhotoUrl
                        )
                    )
                },
                enabled = uiState.nickname.isNotBlank() && 
                         uiState.firstName.isNotBlank() && 
                         uiState.lastName.isNotBlank() && 
                         uiState.birthDate.isNotBlank() && 
                         uiState.city.isNotBlank() && 
                         uiState.nicknameError == null && 
                         uiState.firstNameError == null && 
                         uiState.lastNameError == null && 
                         uiState.birthDateError == null && 
                         uiState.cityError == null && 
                         !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SperrmullPrimary,
                    disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = stringResource(R.string.auth_finish),
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.padding(16.dp)
        )
    }
}
