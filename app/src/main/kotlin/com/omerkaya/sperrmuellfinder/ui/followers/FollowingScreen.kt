package com.omerkaya.sperrmuellfinder.ui.followers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.core.ui.theme.SperrmullPrimary
import com.omerkaya.sperrmuellfinder.domain.model.PremiumFrameType
import com.omerkaya.sperrmuellfinder.ui.common.SafeGlideImage
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

/**
 * 🎯 MODERN INSTAGRAM-STYLE FOLLOWING SCREEN
 * Professional real-time following list with Firebase integration
 * Rules.md compliant - Clean Architecture UI with proper string resources
 * 
 * Features:
 * - Real-time following list updates
 * - Unfollow functionality
 * - Navigate to user profiles
 * - Current user detection (cannot unfollow yourself)
 * - Loading and error states
 * - Empty state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowingScreen(
    userId: String,
    onNavigateBack: () -> Unit = {},
    onNavigateToUserProfile: (String) -> Unit = {},
    viewModel: FollowingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val swipeRefreshState = rememberSwipeRefreshState(uiState.isRefreshing)
    val visibleFollowingCount = uiState.following.size
    
    // Load following when screen opens
    LaunchedEffect(userId) {
        viewModel.loadFollowing(userId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${stringResource(R.string.following_title)} ($visibleFollowingCount)",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back_cd),
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { viewModel.refreshFollowing() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    uiState.isLoading -> {
                        LoadingContent()
                    }
                    uiState.error != null -> {
                        ErrorContent(
                            error = uiState.error ?: stringResource(R.string.error_unknown),
                            onRetry = { viewModel.loadFollowing(userId) }
                        )
                    }
                    uiState.following.isEmpty() -> {
                        EmptyContent()
                    }
                    else -> {
                        FollowingList(
                            following = uiState.following,
                            onUserClick = onNavigateToUserProfile
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// FOLLOWING LIST
// =============================================================================

@Composable
private fun FollowingList(
    following: List<FollowingItem>,
    onUserClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        itemsIndexed(
            items = following,
            key = { _, item -> item.user.uid }
        ) { index, followingItem ->
            FollowingListItem(
                followingItem = followingItem,
                onUserClick = { onUserClick(followingItem.user.uid) }
            )

            if (index < following.lastIndex) {
                Divider(
                    modifier = Modifier.padding(start = 84.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )
            }
        }
    }
}

@Composable
private fun FollowingListItem(
    followingItem: FollowingItem,
    onUserClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUserClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile photo (LikesListScreen style)
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            SafeGlideImage(
                imageUrl = followingItem.user.photoUrl,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                contentDescription = stringResource(R.string.cd_user_avatar),
                errorPlaceholder = {
                    // Gradient initials fallback (same visual language as likes list)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF6366F1),
                                        Color(0xFF8B5CF6)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val initials = followingItem.user.displayName.let { name ->
                            if (name.isBlank()) {
                                "U"
                            } else {
                                name.split(" ")
                                    .take(2)
                                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                    .joinToString("")
                                    .takeIf { it.isNotEmpty() } ?: name.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
                            }
                        }
                        
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    }
                }
            )

            // Premium frame overlay
            if (followingItem.user.isPremium) {
                val frameType = followingItem.user.getPremiumFrameType()
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Card(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 2.dp,
                            color = when (frameType) {
                                PremiumFrameType.GOLD -> Color(0xFFFFD700)
                                PremiumFrameType.DIAMOND -> Color(0xFF00FFFF)
                                PremiumFrameType.PLATINUM -> Color(0xFFC0C0C0)
                                PremiumFrameType.RAINBOW -> Color(0xFFFF6B6B)
                                else -> MaterialTheme.colorScheme.primary
                            }
                        ),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        )
                    ) {}
                }
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // User info (displayName + city)
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = followingItem.user.displayName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            followingItem.user.city?.takeIf { it.isNotBlank() }?.let { city ->
                Text(
                    text = city,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
    }
}

// =============================================================================
// LOADING, ERROR, AND EMPTY STATES
// =============================================================================

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = SperrmullPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.following_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = stringResource(R.string.following_error),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.Red,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SperrmullPrimary
                )
            ) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun EmptyContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.following_empty_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.following_empty_description),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}
