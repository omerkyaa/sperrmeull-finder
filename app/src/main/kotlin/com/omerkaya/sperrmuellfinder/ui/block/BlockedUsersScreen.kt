package com.omerkaya.sperrmuellfinder.ui.block

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.domain.model.BlockedUser
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Blocked Users Screen - Instagram-style list
 * Rules.md compliant - Material3 design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersScreen(
    onNavigateBack: () -> Unit,
    viewModel: BlockedUsersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var showUnblockDialog by remember { mutableStateOf<BlockedUser?>(null) }
    
    // Show success message
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.dismissSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.blocked_users_title),
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
                uiState.isLoading -> {
                    LoadingContent()
                }
                uiState.blockedUsers.isEmpty() -> {
                    EmptyBlockedUsersContent()
                }
                else -> {
                    BlockedUsersList(
                        blockedUsers = uiState.blockedUsers,
                        unblockingUserId = uiState.unblockingUserId,
                        onUnblockClick = { showUnblockDialog = it }
                    )
                }
            }
            
            // Success Message Snackbar
            if (uiState.successMessage != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = uiState.successMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            // Error Message
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
    
    // Unblock Dialog
    showUnblockDialog?.let { blockedUser ->
        val user = User(
            uid = blockedUser.blockedUserId,
            email = "", // Not needed for unblock dialog
            displayName = blockedUser.blockedUserName,
            photoUrl = blockedUser.blockedUserPhotoUrl,
            city = null,
            dob = null,
            gender = null,
            xp = 0,
            level = 1,
            honesty = 100,
            isPremium = false,
            premiumUntil = null,
            badges = emptyList(),
            favorites = com.omerkaya.sperrmuellfinder.domain.model.UserFavorites(),
            fcmToken = null,
            deviceTokens = emptyList(),
            deviceLang = "de",
            deviceModel = "",
            deviceOs = "Android",
            frameLevel = 0,
            createdAt = java.util.Date(),
            updatedAt = java.util.Date(),
            lastLoginAt = null
        )
        
        UnblockUserDialog(
            user = user,
            isLoading = uiState.unblockingUserId == blockedUser.blockedUserId,
            onConfirm = {
                viewModel.unblockUser(blockedUser.blockedUserId)
                showUnblockDialog = null
            },
            onDismiss = { showUnblockDialog = null }
        )
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun EmptyBlockedUsersContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Block,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.blocked_users_empty_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.blocked_users_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BlockedUsersList(
    blockedUsers: List<BlockedUser>,
    unblockingUserId: String?,
    onUnblockClick: (BlockedUser) -> Unit
) {
    val appLocale = LocalConfiguration.current.locales[0] ?: Locale.GERMAN

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(blockedUsers, key = { it.blockedUserId }) { blockedUser ->
            BlockedUserItem(
                blockedUser = blockedUser,
                appLocale = appLocale,
                isUnblocking = unblockingUserId == blockedUser.blockedUserId,
                onUnblockClick = { onUnblockClick(blockedUser) }
            )
        }
    }
}

@Composable
private fun BlockedUserItem(
    blockedUser: BlockedUser,
    appLocale: Locale,
    isUnblocking: Boolean,
    onUnblockClick: () -> Unit
) {
    val dateFormat = remember(appLocale) { SimpleDateFormat("dd MMM yyyy", appLocale) }
    val displayName = blockedUser.blockedUserName.ifBlank { stringResource(R.string.unknown_user) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // User Photo
        GlideImage(
            imageModel = { blockedUser.blockedUserPhotoUrl },
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
            imageOptions = ImageOptions(
                contentScale = ContentScale.Crop
            ),
            loading = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                )
            },
            failure = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayName.take(1).uppercase(appLocale),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // User Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = stringResource(
                    R.string.blocked_user_date,
                    dateFormat.format(blockedUser.createdAt)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            blockedUser.reason?.let { reasonText ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = reasonText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Unblock Button
        Button(
            onClick = onUnblockClick,
            enabled = !isUnblocking,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ),
            modifier = Modifier.height(36.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
        ) {
            if (isUnblocking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = stringResource(R.string.unblock_user_button),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
