package com.omerkaya.sperrmuellfinder.ui.profile

import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.core.ui.theme.SperrmullPrimary
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants
import com.omerkaya.sperrmuellfinder.domain.model.AccountDeletionStatus
import com.omerkaya.sperrmuellfinder.domain.model.DeletionStatus
import com.omerkaya.sperrmuellfinder.domain.model.Post
import com.omerkaya.sperrmuellfinder.domain.model.PostStatus
import com.omerkaya.sperrmuellfinder.domain.model.PremiumFrameType
import com.omerkaya.sperrmuellfinder.ui.common.LargeProfilePhoto
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import com.omerkaya.sperrmuellfinder.ui.common.SafeGlideImage

/**
 * 🎯 INSTAGRAM-STYLE PROFILE SCREEN - Own Profile (HomeScreen Pattern)
 * Production-ready profile screen with complete Firestore data display
 * - Uses ProfileViewModel's simplePosts StateFlow for direct Firestore data
 * - Material3 compliant design with modern profile interface
 * - Pull-to-refresh functionality with SwipeRefresh
 * - Real-time Firestore listener for instant updates
 * - Localized strings and proper error handling
 * - Performance optimized with LazyColumn and LazyVerticalGrid
 * Features: Username dropdown, settings, stats, tabs, 3-column grid
 * MVP: XP/Level/Honesty systems removed
 */
@Composable
fun ProfileScreen(
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToFollowers: (String) -> Unit = {},
    onNavigateToFollowing: (String) -> Unit = {},
    onNavigateToAdminDashboard: () -> Unit = {},
    onNavigateToDeleteAccount: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    // Collect data from ViewModel - HomeScreen pattern
    val uiState by viewModel.uiState.collectAsState()
    val userPosts by viewModel.userPosts.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val currentUser = uiState.user
    val isProfileLoading = uiState.isLoading || loading
    val accountDeletionStatus by viewModel.accountDeletionStatus.collectAsState(initial = null)
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    // Pull-to-refresh state
    var refreshing by remember { mutableStateOf(false) }
    val swipeRefreshState = rememberSwipeRefreshState(refreshing || loading)
    
    // Load user data when screen opens
    LaunchedEffect(Unit) {
        try {
            viewModel.loadCurrentUserProfile()
            android.util.Log.d("ProfileScreen", "✅ loadCurrentUserProfile called successfully")
        } catch (e: Exception) {
            android.util.Log.e("ProfileScreen", "❌ Error in LaunchedEffect", e)
        }
    }

    // Keep logs concise and avoid false-negative error noise during transient auth/profile loads
    LaunchedEffect(currentUser, isProfileLoading, uiState.error) {
        android.util.Log.d(
            "ProfileScreen",
            "📊 State: currentUser=${currentUser?.displayName ?: "NULL"}, loading=$isProfileLoading, error=${uiState.error}"
        )
    }

            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = {
                    refreshing = true
                    try {
                        viewModel.refreshCurrentUserProfile {
                            refreshing = false
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ProfileScreen", "❌ Error in refresh", e)
                        refreshing = false
                    }
                },
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
            ) {
                when {
                // Loading state - show when initially loading or no user yet
                isProfileLoading && currentUser == null -> {
                    android.util.Log.d("ProfileScreen", "🔄 Showing LoadingContent")
                    LoadingContent()
                }
                // User available - show profile content
                currentUser != null -> {
                    android.util.Log.d("ProfileScreen", "✅ User available: ${currentUser.displayName}")
                    val user = currentUser
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        // Header with username and settings
                        item {
                            InstagramStyleHeader(
                                user = user,
                                onSettingsClick = onNavigateToSettings,
                                onMiniProfileClick = onNavigateToEditProfile,
                                onAdminClick = onNavigateToAdminDashboard,
                                isAdmin = uiState.isAdmin
                            )
                        }
                        
                        // Account Deletion Warning Banner
                        accountDeletionStatus?.let { status ->
                            if (status.status == DeletionStatus.PENDING) {
                                item {
                                    AccountDeletionBanner(
                                        deletionStatus = status,
                                        onCancelClick = onNavigateToDeleteAccount
                                    )
                                }
                            }
                        }

                        // Profile info section
                        item {
                            ProfileInfoSection(
                                user = user,
                                postsCount = userPosts.size,
                                followersCount = uiState.followersCount,
                                followingCount = uiState.followingCount,
                                isPremium = user.isPremium,
                                frameType = user.getPremiumFrameType(),
                                onFollowersClick = { onNavigateToFollowers(user.uid) },
                                onFollowingClick = { onNavigateToFollowing(user.uid) }
                            )
                        }

                        // Action buttons
                        item {
                            ActionButtonsSection(
                                onEditProfileClick = onNavigateToEditProfile,
                                onShareProfileClick = {
                                    shareProfile(context, user.displayName)
                                },
                                onAddFriendClick = {
                                    shareAppInvite(context)
                                }
                            )
                        }

                        // Content tabs
                        item {
                            ContentTabsSection(
                                selectedTabIndex = selectedTabIndex,
                                onTabSelected = { selectedTabIndex = it },
                                postsCount = userPosts.size,
                                archivedPostsCount = getArchivedPostsCount(userPosts),
                                favoritesCount = 0 // TODO: Implement favorites
                            )
                        }

                        // Posts grid
                        item {
                            PostsGridSection(
                                selectedTabIndex = selectedTabIndex,
                                posts = userPosts,
                                isPremium = user.isPremium,
                                onPostClick = onNavigateToPostDetail
                            )
                        }
                    }
                }
                uiState.error != null -> {
                    android.util.Log.e("ProfileScreen", "❌ Error state: ${uiState.error}")
                    ErrorContent(
                        error = uiState.error ?: "Failed to load profile",
                        onRetry = { 
                            android.util.Log.d("ProfileScreen", "🔄 Retry clicked")
                            viewModel.loadCurrentUserProfile() 
                        }
                    )
                }
                else -> {
                    // Transient empty state (e.g., auth/profile bootstrap): keep showing loading instead of false error
                    LoadingContent()
                }
            }
        }
    }
}

// =============================================================================
// INSTAGRAM-STYLE HEADER SECTION
// =============================================================================

@Composable
private fun InstagramStyleHeader(
    user: com.omerkaya.sperrmuellfinder.domain.model.User,
    onSettingsClick: () -> Unit,
    onMiniProfileClick: () -> Unit,
    onAdminClick: () -> Unit = {},
    isAdmin: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Username with dropdown arrow - centered
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clickable { /* TODO: Show account switcher */ }
        ) {
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }

        // Admin icon (if user is admin)
        if (isAdmin) {
            IconButton(onClick = onAdminClick) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = stringResource(R.string.admin_dashboard),
                    tint = Color(0xFFFFD700), // Gold color for admin
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Settings icon
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// =============================================================================
// PROFILE INFO SECTION
// =============================================================================

@Composable
private fun ProfileInfoSection(
    user: com.omerkaya.sperrmuellfinder.domain.model.User,
    postsCount: Int,
    followersCount: Int,
    followingCount: Int,
    isPremium: Boolean,
    frameType: PremiumFrameType,
    onFollowersClick: () -> Unit = {},
    onFollowingClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
        // Profile photo with premium frame - using new component
        LargeProfilePhoto(
            photoUrl = user.photoUrl,
            displayName = user.displayName,
            isPremium = isPremium,
            premiumFrameType = frameType
        )

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

    // User info below photo - only nickname
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = user.displayName,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
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
            text = count.toString(),
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
    onEditProfileClick: () -> Unit,
    onShareProfileClick: () -> Unit,
    onAddFriendClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Edit profile and Share profile buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onEditProfileClick,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            ) {
                Text(stringResource(R.string.edit_profile))
            }
            
            OutlinedButton(
                onClick = onShareProfileClick,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            ) {
                Text(stringResource(R.string.share_profile))
            }
                    }

            Spacer(modifier = Modifier.height(8.dp))

        // Add friend button
        OutlinedButton(
            onClick = onAddFriendClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.profile_add_friend))
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
    favoritesCount: Int
) {
    TabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        indicator = { tabPositions ->
            Box(
                modifier = Modifier
                    .tabIndicatorOffset(tabPositions[selectedTabIndex])
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.onSurface)
            )
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
        
        Tab(
            selected = selectedTabIndex == 2,
            onClick = { onTabSelected(2) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = if (selectedTabIndex == 2) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        )
    }
}

// =============================================================================
// POSTS GRID SECTION
// =============================================================================

@Composable
private fun PostsGridSection(
    selectedTabIndex: Int,
    posts: List<Post>,
    isPremium: Boolean,
    onPostClick: (String) -> Unit
) {
    when (selectedTabIndex) {
        0 -> {
            // Active posts
            val filteredPosts = posts.filter { it.status == PostStatus.ACTIVE }
            if (filteredPosts.isEmpty()) {
                EmptyStateSection(selectedTabIndex)
            } else {
                PostsGrid(filteredPosts, isPremium, onPostClick)
            }
        }
        1 -> {
            // Archived posts
            val filteredPosts = posts.filter { it.status == PostStatus.ARCHIVED }
            if (filteredPosts.isEmpty()) {
                EmptyStateSection(selectedTabIndex)
            } else {
                PostsGrid(filteredPosts, isPremium, onPostClick)
            }
        }
        2 -> {
            // Favorites - placeholder for now
            EmptyStateSection(selectedTabIndex)
        }
        else -> {
            if (posts.isEmpty()) {
                EmptyStateSection(selectedTabIndex)
            } else {
                PostsGrid(posts, isPremium, onPostClick)
            }
        }
    }
}

@Composable
private fun PostsGrid(
    posts: List<Post>,
    isPremium: Boolean,
    onPostClick: (String) -> Unit
) {
    // Non-lazy grid — avoids nested scrolling conflict with the outer LazyColumn
    val rows = posts.chunked(3)
    Column(modifier = Modifier.fillMaxWidth()) {
        rows.forEach { rowPosts ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowPosts.forEach { post ->
                    Box(modifier = Modifier.weight(1f)) {
                        PostGridItem(
                            post = post,
                            isPremium = isPremium,
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


@Composable
private fun PostGridItem(
    post: Post,
    isPremium: Boolean,
    onPostClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable {
                if (post.status == PostStatus.ARCHIVED && !isPremium) return@clickable
                onPostClick()
            },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (post.images.isNotEmpty()) {
                SafeGlideImage(
                    imageUrl = post.images.first(),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                    contentDescription = "Post image",
                    loadingPlaceholder = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    },
                    errorPlaceholder = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "No image",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
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
                        contentDescription = "No image",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            // Pin icon for pinned posts
            // TODO: Implement pinned posts logic when available
            /*
            if (post.isPinned == true) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Pinned post",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(16.dp)
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            CircleShape
                        )
                        .padding(2.dp)
                )
            }
            */

            // Archived post overlay
            if (post.status == PostStatus.ARCHIVED) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Archived post",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Post stats overlay (bottom-left corner)
            if (post.likesCount > 0 || post.commentsCount > 0) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (post.likesCount > 0) {
                            Text(
                            text = "❤️${post.likesCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                            fontSize = 10.sp
                            )
                        }
                        if (post.commentsCount > 0) {
                            Text(
                            text = "💬${post.commentsCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                            fontSize = 10.sp
                            )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateSection(selectedTabIndex: Int) {
    val (title, description) = when (selectedTabIndex) {
        0 -> stringResource(R.string.profile_no_posts_title) to stringResource(R.string.profile_no_posts_description)
        1 -> stringResource(R.string.profile_no_archived_title) to stringResource(R.string.profile_no_archived_description)
        2 -> stringResource(R.string.profile_no_favorites_title) to stringResource(R.string.profile_no_favorites_description)
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
                2 -> Icons.Default.Favorite
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

private fun shareAppInvite(context: android.content.Context) {
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.invite_friends_message))
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.profile_add_friend)))
}

private fun getArchivedPostsCount(posts: List<Post>): Int {
    return posts.count { 
        it.status == PostStatus.ARCHIVED 
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
                text = stringResource(R.string.loading_profile),
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
                text = stringResource(R.string.error_loading_profile),
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

// =============================================================================
// ACCOUNT DELETION WARNING BANNER
// =============================================================================

@Composable
private fun AccountDeletionBanner(
    deletionStatus: AccountDeletionStatus,
    onCancelClick: () -> Unit
) {
    val daysUntilDeletion = remember(deletionStatus.scheduledDeletionDate) {
        val diffMillis = deletionStatus.scheduledDeletionDate.time - System.currentTimeMillis()
        java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffMillis) + 1
    }
    
    val isImminent = daysUntilDeletion <= 7
    val backgroundColor = if (isImminent) {
        Color(0xFFFF5252) // Urgent red
    } else {
        Color(0xFFFFA726) // Warning orange
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⚠️",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.account_deletion_pending_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (daysUntilDeletion > 0) {
                            Text(
                                text = stringResource(R.string.days_until_deletion, daysUntilDeletion),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            Text(
                text = stringResource(R.string.account_deletion_banner_message),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.95f)
            )
            
            Spacer(Modifier.height(12.dp))
            
            Button(
                onClick = onCancelClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = backgroundColor
                )
            ) {
                Text(
                    text = stringResource(R.string.cancel_deletion),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
