package com.omerkaya.sperrmuellfinder.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.domain.model.auth.AuthEvent
import com.omerkaya.sperrmuellfinder.domain.model.auth.AuthResult

/**
 * Instagram-style modern login screen with email/password authentication
 * Features: Clean vertical layout, rounded text fields, modern design
 * Rules.md compliant - Professional UI with Material3 design
 */
@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToBanned: () -> Unit = {},
    onNavigateToForgotPassword: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val passwordResetSentText = stringResource(R.string.auth_password_reset_sent)
    val loginBlockedText = stringResource(R.string.msg_login_blocked)
    
    // Handle auth results
    LaunchedEffect(Unit) {
        viewModel.authResult.collect { result ->
            when (result) {
                is AuthResult.Success -> {
                    // Navigation is handled by navigation event
                }
                is AuthResult.Error -> {
                    if (isBannedError(result.message)) {
                        snackbarHostState.showSnackbar(loginBlockedText)
                        onNavigateToBanned()
                    } else {
                        snackbarHostState.showSnackbar(result.message)
                    }
                }
                is AuthResult.PasswordResetSent -> {
                    snackbarHostState.showSnackbar(message = passwordResetSentText)
                }
            }
        }
    }
    
    // Handle auth state changes
    val authState by viewModel.authState.collectAsState()
    LaunchedEffect(authState) {
        when (authState) {
            is com.omerkaya.sperrmuellfinder.domain.model.auth.AuthState.Authenticated -> {
                onNavigateToHome()
            }
            else -> {
                // Stay on current screen
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
                is com.omerkaya.sperrmuellfinder.domain.model.auth.AuthNavigation.NavigateToLogin -> {
                    // Reset form state
                    viewModel.clearLoginForm()
                }
                else -> {
                    // Handle other navigation events
                }
            }
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(80.dp))
                
                // Instagram Logo Placeholder - App Logo
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(120.dp)
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // Email Field - Instagram style
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = viewModel::updateEmail,
                    placeholder = { 
                        Text(
                            text = stringResource(R.string.auth_email_placeholder),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    isError = uiState.emailError != null,
                    supportingText = uiState.emailError?.let { { Text(it) } },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                        errorBorderColor = MaterialTheme.colorScheme.error
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Password Field - Instagram style
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = viewModel::updatePassword,
                    placeholder = { 
                        Text(
                            text = stringResource(R.string.auth_password_placeholder),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = viewModel::togglePasswordVisibility
                        ) {
                            Icon(
                                imageVector = if (uiState.isPasswordVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (uiState.isPasswordVisible) {
                                    stringResource(R.string.auth_hide_password)
                                } else {
                                    stringResource(R.string.auth_show_password)
                                }
                            )
                        }
                    },
                    visualTransformation = if (uiState.isPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (uiState.isLoginFormValid()) {
                                viewModel.onEvent(
                                    AuthEvent.Login(
                                        uiState.email,
                                        uiState.password
                                    )
                                )
                            }
                        }
                    ),
                    isError = uiState.passwordError != null,
                    supportingText = uiState.passwordError?.let { { Text(it) } },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                        errorBorderColor = MaterialTheme.colorScheme.error
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Login Button - Instagram style
                Button(
                    onClick = {
                        viewModel.onEvent(
                            AuthEvent.Login(
                                uiState.email,
                                uiState.password
                            )
                        )
                    },
                    enabled = uiState.isLoginFormValid() && !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = stringResource(R.string.auth_login),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Forgot Password Link
                TextButton(
                    onClick = onNavigateToForgotPassword,
                    enabled = !uiState.isLoading
                ) {
                    Text(
                        text = stringResource(R.string.auth_forgot_password),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Spacer to push content to bottom
                Spacer(modifier = Modifier.weight(1f))
                
                // Bottom Divider Line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    androidx.compose.material3.Divider(
                        color = Color.Gray.copy(alpha = 0.3f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Create Account Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.auth_dont_have_account),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(
                        onClick = onNavigateToRegister,
                        enabled = !uiState.isLoading
                    ) {
                        Text(
                            text = stringResource(R.string.auth_create_account),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

private fun isBannedError(message: String): Boolean {
    val normalized = message.lowercase()
    return normalized.contains("user_banned") ||
        normalized.contains("banned") ||
        normalized.contains("blocked")
}