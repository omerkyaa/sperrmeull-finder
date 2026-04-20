package com.omerkaya.sperrmuellfinder.ui.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.omerkaya.sperrmuellfinder.core.ui.theme.SperrmullPrimary

/**
 * Email input screen - Step 1 of registration
 * Inspired by AllTrails clean design
 */
@Composable
fun EmailInputScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPassword: (String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    
    // Animation states
    val contentAlpha = remember { Animatable(0f) }
    val contentOffset = remember { Animatable(30f) }
    
    // Animate content appearance
    LaunchedEffect(Unit) {
        contentAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        )
        contentOffset.animateTo(
            targetValue = 0f,
            animationSpec = tween(500, delayMillis = 100, easing = FastOutSlowInEasing)
        )
        
        // Auto-focus email field
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
            // Top Bar with Back Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
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
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // App Logo
            Image(
                painter = painterResource(id = com.omerkaya.sperrmuellfinder.R.drawable.app_logo),
                contentDescription = stringResource(com.omerkaya.sperrmuellfinder.R.string.app_name),
                modifier = Modifier.size(60.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Title
            Text(
                text = "Let's start with your email",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Email Input Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Email",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = viewModel::updateEmail,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = { 
                        Text(
                            text = "Enter your email address",
                            color = Color.Gray.copy(alpha = 0.6f)
                        ) 
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(),
                    isError = uiState.emailError != null,
                    supportingText = uiState.emailError?.let { error ->
                        {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (uiState.emailError != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            SperrmullPrimary
                        },
                        unfocusedBorderColor = if (uiState.emailError != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            Color.Gray.copy(alpha = 0.3f)
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Continue Button
            Button(
                onClick = { 
                    if (uiState.emailError == null && uiState.email.isNotBlank()) {
                        onNavigateToPassword(uiState.email)
                    }
                },
                enabled = uiState.emailError == null && uiState.email.isNotBlank() && !uiState.isLoading,
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
