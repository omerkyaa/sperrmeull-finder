package com.omerkaya.sperrmuellfinder.ui.notifications

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.domain.model.Notification
import com.omerkaya.sperrmuellfinder.domain.model.NotificationType
import com.omerkaya.sperrmuellfinder.ui.components.SafeGlideImage
import com.omerkaya.sperrmuellfinder.ui.components.ErrorState
import com.omerkaya.sperrmuellfinder.ui.components.EmptyState
import com.omerkaya.sperrmuellfinder.core.util.DateTimeFormatters

/**
 * 🔔 INSTAGRAM-STYLE NOTIFICATIONS SCREEN - SperrmüllFinder
 * Rules.md compliant - Live Firestore integration with professional UI
 *
 * Features:
 * - Real-time notification updates from Firestore
 * - Instagram-style UI design with user avatars and post thumbnails
 * - Live user data fetching for actor information
 * - Professional notification grouping and interaction handling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToPostDetail: (postId: String) -> Unit,
    onNavigateToUserProfile: (userId: String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToPremium: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.onScreenOpened()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is NotificationsEvent.NavigateToPost -> {
                    onNavigateToPostDetail(event.postId)
                }
                is NotificationsEvent.NavigateToProfile -> {
                    if (event.userId == uiState.currentUser?.uid) {
                        onNavigateToProfile()
                    } else {
                        onNavigateToUserProfile(event.userId)
                    }
                }
                NotificationsEvent.NavigateToPremium -> {
                    onNavigateToPremium()
                }
                NotificationsEvent.NavigateBack -> {
                    onNavigateBack()
                }
                is NotificationsEvent.HandleDeepLink -> {
                    when {
                        event.deeplink.startsWith("post:") -> {
                            onNavigateToPostDetail(event.deeplink.removePrefix("post:"))
                        }
                        event.deeplink.startsWith("profile:") -> {
                            val userId = event.deeplink.removePrefix("profile:")
                            if (userId == uiState.currentUser?.uid) {
                                onNavigateToProfile()
                            } else {
                                onNavigateToUserProfile(userId)
                            }
                        }
                        else -> onNavigateBack()
                    }
                }
                is NotificationsEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(context.getString(event.messageResId))
                }
                is NotificationsEvent.ShowError -> {
                    snackbarHostState.showSnackbar(context.getString(event.messageResId))
                }
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val total = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 5
        }.collect { shouldLoadMore ->
            if (shouldLoadMore) {
                viewModel.loadMoreNotifications()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.notifications_title),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack, 
                            contentDescription = stringResource(R.string.cd_back_button)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
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
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                uiState.error != null -> {
                    ErrorState(
                        description = uiState.error ?: stringResource(R.string.notifications_error_title),
                        onRetry = { viewModel.loadCurrentUser() }
                    )
                }
                uiState.notifications.isEmpty() -> {
                    EmptyState(
                        title = stringResource(R.string.notifications_empty_title),
                        description = stringResource(R.string.notifications_empty_desc)
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                    ) {
                        items(uiState.notifications, key = { it.id }) { notification ->
                            InstagramStyleNotificationItem(
                                notification = notification,
                                onNotificationClick = viewModel::onNotificationClick,
                                onUsernameClick = viewModel::onUsernameClick,
                                currentUserId = uiState.currentUser?.uid
                            )
                        }

                        if (uiState.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InstagramStyleNotificationItem(
    notification: Notification,
    onNotificationClick: (Notification) -> Unit,
    onUsernameClick: (String) -> Unit,
    currentUserId: String?
) {
    val isUnread = !notification.isRead
    val resolvedActorUserId = notification.resolvedActorUserId
    val actorDisplayName = notification.actorUsername.takeIf { it.isNotBlank() }
        ?: ""
    val actorNickname = (notification.data["fromUserNickname"] as? String)
        ?: ""
    val actorPhotoUrl = notification.actorPhotoUrl
    val behaviorText = buildNotificationBehaviorText(
        notification = notification,
        actorDisplayName = actorDisplayName
    )
    val timestampText = DateTimeFormatters.formatTimeAgo(notification.createdAt)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isUnread) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f) 
                else Color.Transparent
            )
            .clickable { onNotificationClick(notification) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Actor Avatar with Notification Type Indicator
        Box {
            if (actorPhotoUrl != null && actorPhotoUrl.isNotBlank()) {
                SafeGlideImage(
                    imageUrl = actorPhotoUrl,
                    contentDescription = stringResource(R.string.cd_user_profile_image, actorDisplayName),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !resolvedActorUserId.isNullOrBlank()) {
                            resolvedActorUserId?.let(onUsernameClick)
                        }
                )
            } else {
                // Default gradient avatar like Instagram
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !resolvedActorUserId.isNullOrBlank()) {
                            resolvedActorUserId?.let(onUsernameClick)
                        }
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF833AB4),
                                    Color(0xFFE1306C),
                                    Color(0xFFFD1D1D),
                                    Color(0xFFF77737)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = actorDisplayName.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            // Notification type indicator (bottom right)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getNotificationIcon(notification.type),
                    contentDescription = notification.type.value,
                    tint = getNotificationIconColor(notification.type),
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Notification Content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = actorDisplayName.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.anonymous_user),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF0095F6),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable {
                    resolvedActorUserId?.let(onUsernameClick)
                }
            )

            if (actorNickname.isNotBlank()) {
                Text(
                    text = "@$actorNickname",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = behaviorText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = timestampText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            
            // Comment text for comment notifications
            val commentText = notification.commentText
            if (notification.type == NotificationType.COMMENT && !commentText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = commentText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Post thumbnail (for like/comment notifications)
        val postImageUrl = notification.postImageUrl
        if (postImageUrl != null && postImageUrl.isNotBlank()) {
            Spacer(modifier = Modifier.width(12.dp))
            SafeGlideImage(
                imageUrl = postImageUrl,
                contentDescription = stringResource(R.string.cd_post_image),
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }

        // Unread indicator
        if (isUnread) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun buildNotificationBehaviorText(
    notification: Notification,
    actorDisplayName: String
): String {
    val actor = actorDisplayName.takeIf { it.isNotBlank() } ?: stringResource(R.string.anonymous_user)
    val likeCount = (notification.data["likeCount"] as? Number)?.toInt() ?: 1
    return when (notification.type) {
        NotificationType.LIKE -> {
            if (likeCount <= 1) {
                stringResource(R.string.notification_behavior_like_single, actor)
            } else {
                stringResource(R.string.notification_behavior_like_group, actor, likeCount - 1)
            }
        }
        NotificationType.COMMENT -> {
            if (!notification.commentText.isNullOrBlank()) {
                stringResource(R.string.notification_behavior_comment_with_text, actor, notification.commentText!!)
            } else {
                stringResource(R.string.notification_behavior_comment, actor)
            }
        }
        NotificationType.FOLLOW -> stringResource(R.string.notification_behavior_follow, actor)
        NotificationType.PREMIUM_NEARBY_POST -> stringResource(R.string.notification_behavior_premium_nearby)
        NotificationType.PREMIUM_EXPIRED -> stringResource(R.string.notification_behavior_premium_expired)
        NotificationType.POST_EXPIRED -> stringResource(R.string.notification_behavior_post_expired)
        NotificationType.ADMIN_PENALTY -> notification.body.ifBlank { stringResource(R.string.notification_behavior_admin_penalty) }
        NotificationType.SYSTEM -> notification.body.ifBlank { stringResource(R.string.notification_behavior_system) }
    }
}

@Composable
fun getNotificationIcon(type: NotificationType): ImageVector {
    return when (type) {
        NotificationType.LIKE -> Icons.Default.Favorite
        NotificationType.COMMENT -> Icons.Default.Comment
        NotificationType.FOLLOW -> Icons.Default.PersonAdd
        NotificationType.ADMIN_PENALTY -> Icons.Default.Gavel
        NotificationType.PREMIUM_NEARBY_POST -> Icons.Default.LocationOn
        else -> Icons.Default.Favorite
    }
}

@Composable
fun getNotificationIconColor(type: NotificationType): Color {
    return when (type) {
        NotificationType.LIKE -> Color(0xFFE91E63) // Pink like Instagram
        NotificationType.COMMENT -> MaterialTheme.colorScheme.primary
        NotificationType.FOLLOW -> MaterialTheme.colorScheme.primary
        NotificationType.ADMIN_PENALTY -> MaterialTheme.colorScheme.error
        NotificationType.PREMIUM_NEARBY_POST -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary
    }
}
