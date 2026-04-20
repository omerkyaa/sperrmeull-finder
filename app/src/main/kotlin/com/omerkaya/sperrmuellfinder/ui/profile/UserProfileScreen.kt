package com.omerkaya.sperrmuellfinder.ui.profile

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import com.omerkaya.sperrmuellfinder.ui.common.SafeGlideImage
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.core.ui.theme.SperrmullPrimary
import com.omerkaya.sperrmuellfinder.domain.model.PostStatus
import com.omerkaya.sperrmuellfinder.domain.model.PremiumFrameType
import com.omerkaya.sperrmuellfinder.domain.model.ReportTargetType
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.ui.block.BlockUserDialog
import com.omerkaya.sperrmuellfinder.ui.block.UnblockUserDialog
import com.omerkaya.sperrmuellfinder.ui.common.rememberLiveUserDisplay
import com.omerkaya.sperrmuellfinder.ui.report.ReportBottomSheet
import com.skydoves.landscapist.glide.GlideImage

/**
 * Data class for post preview in profile grid
 */
data class PostPreview(
    val id: String,
    val imageUrl: String?,
    val description: String = "",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val createdAt: java.util.Date = java.util.Date(),
    val archivedAt: java.util.Date? = null,
    val availabilityPercent: Int? = null, // Only visible to Premium users
    val isPinned: Boolean? = false, // For pinned posts
    val status: PostStatus = PostStatus.ACTIVE // Post status for archived posts
)

/**
 * 🎯 MODERN INSTAGRAM-STYLE USER PROFILE SCREEN
 * Professional, real-time user profile interface with Firebase integration
 * Features: Follow/Unfollow, Real-time data, FCM notifications, Report/Block functionality
 * Rules.md compliant - Clean Architecture UI with proper string resources
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    onNavigateBack: () -> Unit = {},
    onNavigateToPostDetail: (String) -> Unit = {},
    onNavigateToFollowers: (String) -> Unit = {},
    onNavigateToFollowing: (String) -> Unit = {},
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val resolvedLiveUser = rememberLiveUserDisplay(
        userId = userId.takeIf { it.isNotBlank() } ?: (uiState.user?.uid ?: ""),
        fallbackDisplayName = uiState.user?.displayName,
        fallbackPhotoUrl = uiState.user?.photoUrl,
        fallbackNickname = uiState.user?.nickname
    )
    val resolvedProfileName = resolvedLiveUser.displayName
        ?.takeIf { it.isNotBlank() }
        ?: resolvedLiveUser.nickname?.takeIf { it.isNotBlank() }
        ?: uiState.user?.displayName?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.unknown_user)
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showReportSheet by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var showUnblockDialog by remember { mutableStateOf(false) }

    // Load user data when screen opens
    LaunchedEffect(userId) {
        viewModel.loadUser(userId)
    }

    // Handle one-shot navigation events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UserProfileEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.user == null) {
                            stringResource(R.string.user_profile_loading)
                        } else {
                            resolvedProfileName
                        },
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
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.more_options),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // More options dropdown menu
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.share_profile)) },
                            onClick = {
                                showMoreMenu = false
                                uiState.user?.let {
                                    shareProfile(context, resolvedProfileName)
                                }
                            }
                        )
                        if (!uiState.isCurrentUser) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Flag,
                                            contentDescription = null,
                                            tint = Color.Red,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.report_user),
                                            color = Color.Red
                                        )
                                    }
                                },
                                onClick = {
                                    showMoreMenu = false
                                    showReportSheet = true
                                }
                            )
                            if (uiState.isBlockedByCurrentUser) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Block,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(R.string.unblock_user_button))
                                        }
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        showUnblockDialog = true
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Block,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(R.string.block_user),
                                                color = MaterialTheme.colorScheme.error,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        showBlockDialog = true
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
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
            uiState.error != null -> {
                uiState.error?.let { error ->
                    ErrorContent(
                        error = error,
                            onRetry = { viewModel.loadUser(userId) }
                    )
                }
            }
            uiState.user != null -> {
                uiState.user?.let { user ->
                    val effectiveUser = user.copy(
                        displayName = resolvedProfileName,
                        nickname = resolvedLiveUser.nickname?.takeIf { it.isNotBlank() } ?: user.nickname,
                        photoUrl = resolvedLiveUser.photoUrl ?: user.photoUrl
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        // Profile info section
                        item {
                            ProfileInfoSection(
                                user = effectiveUser,
                                postsCount = uiState.postsCount,
                                followersCount = uiState.followersCount,
                                followingCount = uiState.followingCount,
                                isPremium = uiState.isPremium,
                                frameType = uiState.frameType,
                                onFollowersClick = { onNavigateToFollowers(userId) },
                                onFollowingClick = { onNavigateToFollowing(userId) }
                            )
                        }

                    // Action buttons (hide for current user)
                    if (!uiState.isCurrentUser) {
                        item {
                            ActionButtonsSection(
                                isFollowing = uiState.isFollowing,
                                isLoading = uiState.isFollowActionLoading,
                                onFollowToggle = { viewModel.toggleFollow() }
                            )
                        }
                    }

                    // Content tabs — archive tab only for own profile
                    item {
                        ContentTabsSection(
                            selectedTabIndex = selectedTabIndex,
                            onTabSelected = { selectedTabIndex = it },
                            postsCount = uiState.postsCount,
                            archivedPostsCount = uiState.archivedPostsCount,
                            showArchiveTab = uiState.isCurrentUser
                        )
                    }

                    // Posts grid
                    item {
                        PostsGridSection(
                            selectedTabIndex = selectedTabIndex,
                            posts = uiState.posts,
                            archivedPosts = uiState.archivedPosts,
                            onPostClick = onNavigateToPostDetail
                        )
                        }
                    }
                }
            }
            }
        }
    }
    
    // Report Bottom Sheet
    if (showReportSheet && !uiState.isCurrentUser) {
        ReportBottomSheet(
            targetId = userId,
            targetType = ReportTargetType.USER,
            onDismiss = { showReportSheet = false }
        )
    }
    
    // Block User Dialog
    if (showBlockDialog && uiState.user != null) {
        BlockUserDialog(
            user = uiState.user!!.copy(
                displayName = resolvedProfileName,
                nickname = resolvedLiveUser.nickname?.takeIf { it.isNotBlank() } ?: uiState.user!!.nickname,
                photoUrl = resolvedLiveUser.photoUrl ?: uiState.user!!.photoUrl
            ),
            isLoading = uiState.isLoading,
            onConfirm = { reason ->
                viewModel.blockUser(reason)
                showBlockDialog = false
            },
            onDismiss = { showBlockDialog = false }
        )
    }

    // Unblock User Dialog
    if (showUnblockDialog && uiState.user != null) {
        UnblockUserDialog(
            user = uiState.user!!.copy(
                displayName = resolvedProfileName,
                photoUrl = resolvedLiveUser.photoUrl ?: uiState.user!!.photoUrl
            ),
            isLoading = uiState.isLoading,
            onConfirm = {
                viewModel.unblockUser()
                showUnblockDialog = false
            },
            onDismiss = { showUnblockDialog = false }
        )
    }
}

// =============================================================================
// PROFILE INFO SECTION
// =============================================================================

@Composable
private fun ProfileInfoSection(
    user: User,
    postsCount: Int,
    followersCount: Int,
    followingCount: Int,
    isPremium: Boolean,
    frameType: PremiumFrameType,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile photo with premium frame - using same system as PostDetailScreen
        // Debug: Log photo URL
        LaunchedEffect(user.photoUrl) {
            println("🎯 UserProfileScreen - user.photoUrl: '${user.photoUrl}', displayName: '${user.displayName}'")
        }
        
        Box(
            modifier = Modifier.size(88.dp)
        ) {
            // Profile photo
            SafeGlideImage(
                imageUrl = user.photoUrl,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                contentDescription = stringResource(R.string.cd_user_avatar),
                errorPlaceholder = {
                    // Default avatar with user initials - same as PostDetailScreen
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        val initials = user.displayName.let { name ->
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
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
            
            // Premium frame overlay (if applicable) - same as PostDetailScreen
            if (isPremium && frameType != PremiumFrameType.NONE) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFD700), // Gold
                                    Color(0xFFFFA500)  // Orange
                                )
                            ),
                            CircleShape
                        )
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(24.dp))

        // Stats section
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatColumn(
                count = postsCount,
                label = stringResource(R.string.posts),
                onClick = { /* Already showing posts */ }
            )
            StatColumn(
                count = followersCount,
                label = stringResource(R.string.followers),
                onClick = onFollowersClick
            )
            StatColumn(
                count = followingCount,
                label = stringResource(R.string.following),
                onClick = onFollowingClick
            )
        }
    }

        Spacer(modifier = Modifier.height(16.dp))

        // User info
        Column {
        Text(
            text = user.displayName,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        
            if (isPremium) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700), // Gold color
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
            Text(
                        text = stringResource(R.string.premium_member),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFD700)
                    )
                }
            }
        }
    }
}


@Composable
private fun StatColumn(
    count: Int,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = formatCount(count),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// =============================================================================
// ACTION BUTTONS SECTION
// =============================================================================

@Composable
private fun ActionButtonsSection(
    isFollowing: Boolean,
    isLoading: Boolean,
    onFollowToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = onFollowToggle,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowing) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f) else Color(0xFF0095F6),
                        contentColor = if (isFollowing) MaterialTheme.colorScheme.onSurface else Color.White
                    )
                ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = if (isFollowing) MaterialTheme.colorScheme.onSurface else Color.White
                )
            } else {
                Text(
                    text = if (isFollowing) {
                        stringResource(R.string.following)
                    } else {
                        stringResource(R.string.follow)
                    }
                )
            }
        }
    }
}

// =============================================================================
// CONTENT TABS SECTION
// =============================================================================

@Composable
private fun ContentTabsSection(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    postsCount: Int,
    archivedPostsCount: Int,
    showArchiveTab: Boolean = false
) {
    val tabCount = if (showArchiveTab) 2 else 1
    TabRow(
        selectedTabIndex = selectedTabIndex.coerceAtMost(tabCount - 1),
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        indicator = { tabPositions ->
            if (tabPositions.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[selectedTabIndex.coerceAtMost(tabPositions.size - 1)])
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.onSurface)
                )
            }
        }
    ) {
        Tab(
            selected = selectedTabIndex == 0,
            onClick = { onTabSelected(0) },
            icon = {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = null,
                    tint = if (selectedTabIndex == 0) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        )

        if (showArchiveTab) {
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { onTabSelected(1) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (selectedTabIndex == 1) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            )
        }
    }
}

// =============================================================================
// POSTS GRID SECTION
// =============================================================================

@Composable
private fun PostsGridSection(
    selectedTabIndex: Int,
    posts: List<PostPreview>,
    archivedPosts: List<PostPreview>,
    onPostClick: (String) -> Unit
) {
    val currentPosts = when (selectedTabIndex) {
        0 -> posts
        1 -> archivedPosts
        else -> posts
    }

    if (currentPosts.isEmpty()) {
        EmptyStateSection(selectedTabIndex)
    } else {
        // Non-lazy grid to avoid nested scrolling conflicts with LazyColumn
        val rows = currentPosts.chunked(3)
        Column(modifier = Modifier.fillMaxWidth()) {
            rows.forEach { rowPosts ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    rowPosts.forEach { post ->
                        Box(modifier = Modifier.weight(1f)) {
                            PostGridItem(
                                post = post,
                                onPostClick = { onPostClick(post.id) }
                            )
                        }
                    }
                    repeat(3 - rowPosts.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PostGridItem(
    post: PostPreview,
    onPostClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onPostClick() }
    ) {
        if (!post.imageUrl.isNullOrBlank()) {
            SafeGlideImage(
                imageUrl = post.imageUrl,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                contentDescription = null
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Pin icon for pinned posts
        if (post.isPinned == true) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Pinned post",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(16.dp)
            )
        }
    }
}

@Composable
private fun EmptyStateSection(selectedTabIndex: Int) {
    val (title, description) = when (selectedTabIndex) {
        0 -> stringResource(R.string.profile_no_posts_title) to stringResource(R.string.profile_no_posts_description)
        1 -> stringResource(R.string.profile_no_archived_title) to stringResource(R.string.profile_no_archived_description)
        else -> stringResource(R.string.profile_empty_content) to stringResource(R.string.profile_empty_content_desc)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = when (selectedTabIndex) {
                0 -> Icons.Default.GridView
                1 -> Icons.Default.Lock
                else -> Icons.Default.Person
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// =============================================================================
// UTILITY FUNCTIONS
// =============================================================================

private fun shareProfile(context: android.content.Context, displayName: String) {
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_profile_message, displayName))
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_profile)))
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1000000 -> "${count / 1000000}M"
        count >= 1000 -> "${count / 1000}K"
        else -> count.toString()
    }
}

// =============================================================================
// LOADING AND ERROR STATES
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
                text = stringResource(R.string.user_profile_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                text = stringResource(R.string.user_profile_error),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.Red,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
