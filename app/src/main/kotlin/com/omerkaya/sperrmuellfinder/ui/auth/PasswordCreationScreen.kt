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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.omerkaya.sperrmuellfinder.core.ui.theme.SperrmullPrimary

/**
 * Password creation screen - Step 2 of registration
 * Features: Progress indicator, password visibility toggle, strength validation
 */
@Composable
fun PasswordCreationScreen(
    email: String,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String, String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    
    // Animation states
    val contentAlpha = remember { Animatable(0f) }
    val progressAlpha = remember { Animatable(0f) }
    
    // Animate content appearance
    LaunchedEffect(Unit) {
        // Set email in viewmodel
        viewModel.updateEmail(email)
        
        contentAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        )
        progressAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(300, delayMillis = 200)
        )
        
        // Auto-focus password field
        focusRequester.requestFocus()
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
                                .fillMaxWidth(0.5f) // 50% progress (step 2 of 4)
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
                text = "Create a password",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Password Input Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Password",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = viewModel::updatePassword,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = { 
                        Text(
                            text = "Enter your password",
                            color = Color.Gray.copy(alpha = 0.6f)
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
                                    "Hide password"
                                } else {
                                    "Show password"
                                },
                                tint = Color.Gray
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
                    keyboardActions = KeyboardActions(),
                    isError = uiState.passwordError != null,
                    supportingText = uiState.passwordError?.let { error ->
                        {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (uiState.passwordError != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            SperrmullPrimary
                        },
                        unfocusedBorderColor = if (uiState.passwordError != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            Color.Gray.copy(alpha = 0.3f)
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                // Password Requirements
                if (uiState.password.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PasswordRequirement(
                            text = "At least 6 characters",
                            isMet = uiState.password.length >= 6
                        )
                        PasswordRequirement(
                            text = "Contains a letter and number",
                            isMet = uiState.password.any { it.isLetter() } && 
                                    uiState.password.any { it.isDigit() }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Continue Button
            Button(
                onClick = { 
                    if (uiState.passwordError == null && uiState.password.isNotBlank()) {
                        onNavigateToProfile(email, uiState.password)
                    }
                },
                enabled = uiState.passwordError == null && uiState.password.isNotBlank() && !uiState.isLoading,
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
                        text = "Continue",
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PasswordRequirement(
    text: String,
    isMet: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = if (isMet) SperrmullPrimary else Color.Gray.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(4.dp)
                )
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isMet) SperrmullPrimary else Color.Gray.copy(alpha = 0.7f)
        )
    }
}
