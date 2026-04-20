package com.omerkaya.sperrmuellfinder.ui.admin.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.domain.model.*
import com.omerkaya.sperrmuellfinder.ui.common.SafeGlideImage
import java.text.SimpleDateFormat
import java.util.*

/**
 * Admin Reports Screen - View and manage user reports
 * Professional UI with filters, paging, and quick actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val reports = viewModel.reports.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedReport by remember { mutableStateOf<Report?>(null) }
    var showActionDialog by remember { mutableStateOf(false) }
    
    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AdminReportsEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is AdminReportsEvent.ActionSuccess -> {
                    snackbarHostState.showSnackbar("✅ Action completed successfully")
                    selectedReport = null
                    showActionDialog = false
                    reports.refresh()
                }
                is AdminReportsEvent.ActionError -> {
                    snackbarHostState.showSnackbar("❌ Error: ${event.message}")
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { reports.refresh() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4CAF50),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            // Filter chips
            FilterChipsRow(
                selectedStatus = uiState.selectedStatus,
                selectedPriority = uiState.selectedPriority,
                selectedType = uiState.selectedType,
                onStatusChange = { viewModel.updateFilters(status = it) },
                onPriorityChange = { viewModel.updateFilters(priority = it) },
                onTypeChange = { viewModel.updateFilters(type = it) }
            )
            
            // Reports list
            when {
                reports.loadState.refresh is LoadState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                reports.loadState.refresh is LoadState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Error, null, tint = Color.Red, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Error loading reports")
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { reports.retry() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                reports.itemCount == 0 -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("No reports found", style = MaterialTheme.typography.titleMedium)
                            Text("All clear! 🎉", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(reports.itemCount) { index ->
                            reports[index]?.let { report ->
                                ReportCard(
                                    report = report,
                                    onClick = {
                                        selectedReport = report
                                        showActionDialog = true
                                    }
                                )
                            }
                        }
                        
                        // Loading more indicator
                        if (reports.loadState.append is LoadState.Loading) {
                            item {
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(Modifier.padding(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Action Dialog
    if (showActionDialog && selectedReport != null) {
        ReportActionDialog(
            report = selectedReport!!,
            onDismiss = { showActionDialog = false },
            onAction = { action, reason ->
                viewModel.performAction(selectedReport!!.id, action, reason)
            }
        )
    }
}

@Composable
private fun FilterChipsRow(
    selectedStatus: ReportStatus?,
    selectedPriority: ModerationPriority?,
    selectedType: ReportTargetType?,
    onStatusChange: (ReportStatus?) -> Unit,
    onPriorityChange: (ModerationPriority?) -> Unit,
    onTypeChange: (ReportTargetType?) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Status filters
        item {
            FilterChip(
                selected = selectedStatus == null,
                onClick = { 
                    onStatusChange(null)
                    onPriorityChange(null)
                    onTypeChange(null)
                },
                label = { Text("All Status") }
            )
        }
        item {
            FilterChip(
                selected = selectedStatus == ReportStatus.OPEN,
                onClick = { 
                    onStatusChange(if (selectedStatus == ReportStatus.OPEN) null else ReportStatus.OPEN)
                },
                label = { Text("Open") }
            )
        }
        item {
            FilterChip(
                selected = selectedStatus == ReportStatus.UNDER_REVIEW,
                onClick = { 
                    onStatusChange(if (selectedStatus == ReportStatus.UNDER_REVIEW) null else ReportStatus.UNDER_REVIEW)
                },
                label = { Text("Under Review") }
            )
        }
        
        // Priority filters
        item { Divider(Modifier.width(1.dp).height(32.dp).background(Color.LightGray)) }
        item {
            FilterChip(
                selected = selectedPriority == ModerationPriority.HIGH,
                onClick = {
                    onPriorityChange(if (selectedPriority == ModerationPriority.HIGH) null else ModerationPriority.HIGH)
                },
                label = { Text("High Priority") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color.Red.copy(alpha = 0.2f)
                )
            )
        }
        
        // Type filters
        item { Divider(Modifier.width(1.dp).height(32.dp).background(Color.LightGray)) }
        ReportTargetType.values().forEach { type ->
            item {
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onTypeChange(if (selectedType == type) null else type) },
                    label = { Text(type.name) }
                )
            }
        }
    }
}

@Composable
private fun ReportCard(
    report: Report,
    onClick: () -> Unit
) {
    val priorityColor = when (report.priority) {
        ReportPriority.CRITICAL -> Color(0xFFD32F2F)
        ReportPriority.HIGH -> Color(0xFFF57C00)
        ReportPriority.MEDIUM -> Color(0xFFFFA726)
        ReportPriority.LOW -> Color(0xFF66BB6A)
    }
    
    val statusColor = when (report.status) {
        ReportStatus.OPEN -> Color(0xFF2196F3)
        ReportStatus.UNDER_REVIEW -> Color(0xFFFF9800)
        ReportStatus.APPROVED -> Color(0xFF4CAF50)
        ReportStatus.DISMISSED -> Color(0xFF9E9E9E)
        ReportStatus.RESOLVED -> Color(0xFF8BC34A)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reporter info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SafeGlideImage(
                        imageUrl = report.reporterPhotoUrl,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        shape = CircleShape
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = report.reporterName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = formatDate(report.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                
                // Priority badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = priorityColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = report.priority.getDisplayName(false),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = priorityColor
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Report type and reason
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when (report.type) {
                        ReportTargetType.POST -> Icons.Default.Image
                        ReportTargetType.COMMENT -> Icons.Default.Comment
                        ReportTargetType.USER -> Icons.Default.Person
                    },
                    contentDescription = null,
                    tint = priorityColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "${report.type.name} • ${report.reason.displayName}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            
            // Description
            report.description?.let { desc ->
                if (desc.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.DarkGray
                    )
                }
            }
            
            // Target preview
            if (report.targetContent != null || report.targetImageUrl != null) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF5F5F5)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (report.targetImageUrl != null) {
                            SafeGlideImage(
                                imageUrl = report.targetImageUrl,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = report.targetOwnerName ?: "Unknown User",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                            report.targetContent?.let { content ->
                                if (content.isNotBlank()) {
                                    Text(
                                        text = content,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = report.status.name.replace("_", " "),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }
                
                TextButton(onClick = onClick) {
                    Text("Take Action")
                    Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ReportActionDialog(
    report: Report,
    onDismiss: () -> Unit,
    onAction: (ReportActionType, String) -> Unit
) {
    var selectedAction by remember { mutableStateOf<ReportActionType?>(null) }
    var reason by remember { mutableStateOf("") }
    var showConfirmation by remember { mutableStateOf(false) }
    
    // Show destructive action confirmation dialog
    if (showConfirmation && selectedAction != null && selectedAction!!.isDestructive()) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning, 
                        contentDescription = null,
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("⚠️ DESTRUCTIVE ACTION", color = Color(0xFFD32F2F))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = when (selectedAction) {
                            ReportActionType.HARD_BAN -> 
                                "This will PERMANENTLY ban the user. Firebase Auth will be DISABLED and they cannot login."
                            ReportActionType.DELETE_ACCOUNT -> 
                                "This will COMPLETELY DELETE the user account. All data (posts, comments, likes) will be REMOVED. THIS CANNOT BE UNDONE!"
                            else -> "Are you sure?"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "User: ${report.targetOwnerName}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Reason: $reason",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmation = false
                        onAction(selectedAction!!, reason)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F)
                    )
                ) {
                    Text("CONFIRM DELETION")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Take Action on Report") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Report summary
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF5F5F5)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("${report.type.name} by ${report.targetOwnerName}", fontWeight = FontWeight.Bold)
                        Text("Reported by: ${report.reporterName}", style = MaterialTheme.typography.bodySmall)
                        Text("Reason: ${report.reason.displayName}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                
                // Action selection header
                Text("Select Action:", style = MaterialTheme.typography.titleSmall)
                
                // Non-destructive actions
                Text("Standard Actions:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                listOf(
                    ReportActionType.DISMISS,
                    ReportActionType.WARN_USER,
                    ReportActionType.DELETE_CONTENT
                ).forEach { action ->
                    Surface(
                        onClick = { selectedAction = action },
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedAction == action) Color(0xFFE3F2FD) else Color.White,
                        border = BorderStroke(
                            1.dp, 
                            if (selectedAction == action) Color(0xFF2196F3) else Color(0xFFE0E0E0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = action.getDisplayName(false),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = action.getDescription(false),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
                
                // Soft ban actions
                Text("🚫 Soft Ban (Temporary):", style = MaterialTheme.typography.labelMedium, color = Color(0xFFFF9800))
                listOf(
                    ReportActionType.SOFT_BAN_3_DAYS,
                    ReportActionType.SOFT_BAN_1_WEEK,
                    ReportActionType.SOFT_BAN_1_MONTH
                ).forEach { action ->
                    Surface(
                        onClick = { selectedAction = action },
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedAction == action) Color(0xFFFFF3E0) else Color.White,
                        border = BorderStroke(
                            1.dp, 
                            if (selectedAction == action) Color(0xFFFF9800) else Color(0xFFE0E0E0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = action.getDisplayName(false),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = action.getDescription(false),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
                
                // Destructive actions
                Text("⚠️ DESTRUCTIVE ACTIONS:", style = MaterialTheme.typography.labelMedium, color = Color(0xFFD32F2F))
                listOf(
                    ReportActionType.HARD_BAN,
                    ReportActionType.DELETE_ACCOUNT
                ).forEach { action ->
                    Surface(
                        onClick = { selectedAction = action },
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedAction == action) Color(0xFFFFEBEE) else Color.White,
                        border = BorderStroke(
                            2.dp, 
                            if (selectedAction == action) Color(0xFFD32F2F) else Color(0xFFE0E0E0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = action.getDisplayName(false),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD32F2F)
                                )
                                Text(
                                    text = action.getDescription(false),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
                
                // Reason input
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (required)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    placeholder = { Text("Explain why you're taking this action...") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedAction != null && reason.isNotBlank()) {
                        // Show confirmation for destructive actions
                        if (selectedAction!!.isDestructive()) {
                            showConfirmation = true
                        } else {
                            onAction(selectedAction!!, reason)
                            onDismiss()
                        }
                    }
                },
                enabled = selectedAction != null && reason.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedAction?.isDestructive() == true) 
                        Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (selectedAction?.isDestructive() == true) 
                        "⚠️ Execute (Destructive)" else "Execute"
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDate(date: Date): String {
    val now = Date()
    val diff = now.time - date.time
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        else -> SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(date)
    }
}
