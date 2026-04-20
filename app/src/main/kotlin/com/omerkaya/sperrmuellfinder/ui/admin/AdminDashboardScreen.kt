package com.omerkaya.sperrmuellfinder.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.domain.model.AdminRole

/**
 * Admin Dashboard Screen
 * Rules.md compliant - Material3 Compose UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateToReports: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToContent: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pendingReports = viewModel.pendingReports.collectAsLazyPagingItems()
    val moderationQueue = viewModel.moderationQueue.collectAsLazyPagingItems()
    val adminLogs = viewModel.adminLogs.collectAsLazyPagingItems()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_dashboard)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
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
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                uiState.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = uiState.error ?: stringResource(R.string.error_unknown),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(onClick = { viewModel.refresh() }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                
                uiState.adminRole == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = stringResource(R.string.admin_access_denied),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = stringResource(R.string.admin_access_denied_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Admin Role Badge
                        item {
                            val role = uiState.adminRole
                            if (role != null) {
                                AdminRoleBadge(role = role)
                            }
                        }
                        
                        // Quick Stats
                        item {
                            QuickStatsSection(stats = uiState.stats)
                        }
                        
                        // Quick Actions
                        item {
                            Text(
                                text = stringResource(R.string.admin_quick_actions),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        item {
                            QuickActionsGrid(
                                onNavigateToReports = onNavigateToReports,
                                onNavigateToUsers = onNavigateToUsers,
                                onNavigateToPremium = onNavigateToPremium,
                                onNavigateToContent = onNavigateToContent,
                                onNavigateToNotifications = onNavigateToNotifications,
                                onNavigateToLogs = onNavigateToLogs
                            )
                        }
                        
                        // Recent Activity
                        item {
                            Text(
                                text = stringResource(R.string.admin_recent_activity),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        item {
                            Text(
                                text = stringResource(R.string.admin_pending_reports_count, pendingReports.itemCount),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        item {
                            Text(
                                text = stringResource(R.string.admin_moderation_queue_count, moderationQueue.itemCount),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminRoleBadge(role: AdminRole) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (role) {
                AdminRole.SUPER_ADMIN -> Color(0xFFFFD700) // Gold
                AdminRole.ADMIN -> Color(0xFF87CEEB) // Sky Blue
                AdminRole.MODERATOR -> Color(0xFF90EE90) // Light Green
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = stringResource(R.string.admin_role),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = role.name.replace("_", " "),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun QuickStatsSection(stats: DashboardStats) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.admin_statistics),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Divider()
            
            StatItem(stringResource(R.string.admin_pending_reports), stats.pendingReportsCount.toString())
            StatItem(stringResource(R.string.admin_active_users), stats.activeUsersCount.toString())
            StatItem(stringResource(R.string.admin_banned_users), stats.bannedUsersCount.toString())
            StatItem(stringResource(R.string.admin_posts_today), stats.postsToday.toString())
            StatItem(stringResource(R.string.admin_premium_users), stats.premiumUsersCount.toString())
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun QuickActionsGrid(
    onNavigateToReports: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToContent: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToLogs: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Report,
                label = stringResource(R.string.admin_reports),
                onClick = onNavigateToReports
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.People,
                label = stringResource(R.string.admin_users),
                onClick = onNavigateToUsers
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Star,
                label = stringResource(R.string.admin_premium),
                onClick = onNavigateToPremium
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Delete,
                label = stringResource(R.string.admin_content),
                onClick = onNavigateToContent
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Notifications,
                label = stringResource(R.string.admin_notifications),
                onClick = onNavigateToNotifications
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.History,
                label = stringResource(R.string.admin_logs),
                onClick = onNavigateToLogs
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
