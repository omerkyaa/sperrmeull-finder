package com.omerkaya.sperrmuellfinder.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.domain.model.ReportReason
import com.omerkaya.sperrmuellfinder.domain.model.ReportTargetType

/**
 * Instagram-style Report Bottom Sheet
 * Rules.md compliant - Material3 design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportBottomSheet(
    targetId: String,
    targetType: ReportTargetType,
    onDismiss: () -> Unit,
    viewModel: ReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedReason by remember { mutableStateOf<ReportReason?>(null) }
    var description by remember { mutableStateOf("") }
    
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            kotlinx.coroutines.delay(1500)
            onDismiss()
            viewModel.clearState()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        when {
            uiState.isSuccess -> ReportSuccessContent()
            else -> ReportFormContent(
                selectedReason = selectedReason,
                description = description,
                isLoading = uiState.isLoading,
                error = uiState.error,
                onReasonSelected = { selectedReason = it },
                onDescriptionChanged = { description = it },
                onSubmit = {
                    selectedReason?.let { reason ->
                        viewModel.reportContent(
                            targetId = targetId,
                            type = targetType,
                            reason = reason,
                            description = description.takeIf { it.isNotBlank() }
                        )
                    }
                },
                onDismissError = { viewModel.clearState() }
            )
        }
    }
}

@Composable
private fun ReportFormContent(
    selectedReason: ReportReason?,
    description: String,
    isLoading: Boolean,
    error: String?,
    onReasonSelected: (ReportReason) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismissError: () -> Unit
) {
    // Get report reasons outside remember (composable call)
    val reportReasons = getReportReasons()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        // Header
        Text(
            text = stringResource(R.string.report_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )
        
        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        
        // Subtitle
        Text(
            text = stringResource(R.string.report_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        )
        
        // Reasons List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(reportReasons) { reasonItem ->
                ReportReasonItem(
                    icon = reasonItem.icon,
                    title = reasonItem.title,
                    description = reasonItem.description,
                    isSelected = selectedReason == reasonItem.reason,
                    onClick = { onReasonSelected(reasonItem.reason) }
                )
            }
        }
        
        // Description Field (optional)
        if (selectedReason != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.report_description_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { 
                        Text(stringResource(R.string.report_description_hint)) 
                    },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                )
                
                Text(
                    text = stringResource(R.string.report_description_optional),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        
        // Error Message
        if (error != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    IconButton(
                        onClick = onDismissError,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        
        // Submit Button
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .height(56.dp),
            enabled = selectedReason != null && !isLoading,
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
                Text(
                    text = stringResource(R.string.report_submit),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ReportReasonItem(
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else Color.Transparent
            )
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary 
                  else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary 
                       else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ReportSuccessContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.report_success_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.report_success_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun getReportReasons(): List<ReportReasonItem> {
    return remember {
        listOf(
            ReportReasonItem(
                icon = Icons.Default.Block,
                title = "Spam",
                description = "Unwanted commercial content or repetitive messages",
                reason = ReportReason.SPAM
            ),
            ReportReasonItem(
                icon = Icons.Default.Warning,
                title = "Harassment",
                description = "Bullying, harassment, or abusive behavior",
                reason = ReportReason.HARASSMENT
            ),
            ReportReasonItem(
                icon = Icons.Default.Dangerous,
                title = "Dangerous Items",
                description = "Posting dangerous or illegal items",
                reason = ReportReason.DANGEROUS_GOODS
            ),
            ReportReasonItem(
                icon = Icons.Default.Info,
                title = "Misinformation",
                description = "False or misleading information",
                reason = ReportReason.MISINFORMATION
            ),
            ReportReasonItem(
                icon = Icons.Default.Share,
                title = "Scam",
                description = "Fraudulent or deceptive content",
                reason = ReportReason.SCAM
            ),
            ReportReasonItem(
                icon = Icons.Default.PersonOff,
                title = "Inappropriate Content",
                description = "Content that violates community guidelines",
                reason = ReportReason.INAPPROPRIATE_CONTENT
            ),
            ReportReasonItem(
                icon = Icons.Default.MoreHoriz,
                title = "Other",
                description = "Other violation not covered above",
                reason = ReportReason.OTHER
            )
        )
    }
}

private data class ReportReasonItem(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val reason: ReportReason
)
