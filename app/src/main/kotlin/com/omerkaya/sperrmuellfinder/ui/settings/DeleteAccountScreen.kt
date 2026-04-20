package com.omerkaya.sperrmuellfinder.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.omerkaya.sperrmuellfinder.R

/**
 * Delete Account Screen - Instagram-style with 30-day grace period
 * Rules.md compliant - Material3 design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAccountScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: DeleteAccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Requested deletion means countdown has started on backend; immediately log out user.
    LaunchedEffect(uiState.deletionRequested) {
        if (uiState.deletionRequested) {
            onLogout()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.delete_account_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.deletionStatus != null -> {
                    DeletionPendingContent(
                        deletionStatus = uiState.deletionStatus!!,
                        isCancelling = uiState.isCancelling,
                        onCancelDeletion = viewModel::cancelDeletion
                    )
                }
                uiState.deletionRequested -> {
                    DeletionRequestedSuccessContent(
                        onLogout = onLogout
                    )
                }
                else -> {
                    DeleteAccountFormContent(
                        reason = uiState.reason,
                        isLoading = uiState.isLoading,
                        onReasonChange = viewModel::updateReason,
                        onRequestDeletion = viewModel::requestDeletion
                    )
                }
            }
            
            // Error Snackbar
            if (uiState.error != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error,
                    action = {
                        TextButton(onClick = viewModel::dismissError) {
                            Text(stringResource(R.string.close))
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(uiState.error ?: "")
                }
            }
        }
    }
}

@Composable
private fun DeleteAccountFormContent(
    reason: String,
    isLoading: Boolean,
    onReasonChange: (String) -> Unit,
    onRequestDeletion: () -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showReasonRequiredError by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // Warning Icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = CircleShape
                )
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.delete_account_warning_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.delete_account_grace_period),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // What happens section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.delete_account_what_happens),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                DeletionInfoItem(
                    icon = Icons.Default.CheckCircle,
                    text = stringResource(R.string.delete_account_info_1),
                    color = Color(0xFF4CAF50)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                DeletionInfoItem(
                    icon = Icons.Default.Schedule,
                    text = stringResource(R.string.delete_account_info_2),
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                DeletionInfoItem(
                    icon = Icons.Default.Warning,
                    text = stringResource(R.string.delete_account_info_3),
                    color = Color(0xFFFF9800)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                DeletionInfoItem(
                    icon = Icons.Default.Delete,
                    text = stringResource(R.string.delete_account_info_4),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Reason field
        Text(
            text = stringResource(R.string.delete_account_reason_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.delete_account_reason_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(
            value = reason,
            onValueChange = {
                onReasonChange(it)
                if (it.isNotBlank()) {
                    showReasonRequiredError = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { 
                Text(stringResource(R.string.delete_account_reason_hint)) 
            },
            minLines = 3,
            maxLines = 5,
            enabled = !isLoading,
            isError = showReasonRequiredError,
            supportingText = {
                if (showReasonRequiredError) {
                    Text(
                        text = stringResource(R.string.required),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Delete Button
        Button(
            onClick = {
                if (reason.trim().isBlank()) {
                    showReasonRequiredError = true
                } else {
                    showReasonRequiredError = false
                    showConfirmDialog = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = Color.White
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.delete_account_button),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
    
    // Confirmation Dialog
    if (showConfirmDialog) {
        DeleteAccountConfirmDialog(
            onConfirm = {
                showConfirmDialog = false
                onRequestDeletion()
            },
            onDismiss = { showConfirmDialog = false }
        )
    }
}

@Composable
private fun DeletionPendingContent(
    deletionStatus: com.omerkaya.sperrmuellfinder.domain.model.AccountDeletionStatus,
    isCancelling: Boolean,
    onCancelDeletion: () -> Unit
) {
    val daysRemaining = deletionStatus.getDaysRemaining()
    val isImminent = deletionStatus.isImminent()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        // Countdown Circle
        Box(
            modifier = Modifier
                .size(160.dp)
                .background(
                    color = if (isImminent) MaterialTheme.colorScheme.errorContainer
                           else MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = daysRemaining.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isImminent) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.delete_account_days_remaining),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isImminent) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = stringResource(R.string.delete_account_pending_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = stringResource(R.string.delete_account_pending_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Warning card
        if (isImminent) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.delete_account_imminent_warning),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // Cancel Button
        Button(
            onClick = onCancelDeletion,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isCancelling && deletionStatus.canCancel(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50),
                contentColor = Color.White
            )
        ) {
            if (isCancelling) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.delete_account_cancel_button),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DeletionRequestedSuccessContent(
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(100.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = stringResource(R.string.delete_account_success_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.delete_account_success_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = stringResource(R.string.logout),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DeletionInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DeleteAccountConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.delete_account_confirm_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = stringResource(R.string.delete_account_confirm_message),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = stringResource(R.string.delete_account_confirm_yes),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
