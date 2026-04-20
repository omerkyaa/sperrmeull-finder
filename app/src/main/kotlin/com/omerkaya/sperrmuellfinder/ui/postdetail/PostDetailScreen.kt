package com.omerkaya.sperrmuellfinder.ui.postdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ChatBubbleOutline
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import com.omerkaya.sperrmuellfinder.ui.common.SafeGlideImage
import com.omerkaya.sperrmuellfinder.ui.common.rememberLiveUserDisplay
import com.omerkaya.sperrmuellfinder.ui.components.LargeLikeButton
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.core.ui.theme.SperrmullPrimary
import com.omerkaya.sperrmuellfinder.domain.model.Post
import com.omerkaya.sperrmuellfinder.domain.model.ReportReason
import com.omerkaya.sperrmuellfinder.domain.model.ReportTargetType
import com.omerkaya.sperrmuellfinder.ui.postdetail.components.ImageCarousel
import com.omerkaya.sperrmuellfinder.ui.postdetail.components.LikesBottomSheet
import com.omerkaya.sperrmuellfinder.ui.post.CommentsBottomSheet
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.model.UserFavorites
import com.omerkaya.sperrmuellfinder.ui.block.BlockUserDialog
import com.omerkaya.sperrmuellfinder.ui.post.DeletePostDialog
import com.omerkaya.sperrmuellfinder.ui.report.ReportBottomSheet
import com.omerkaya.sperrmuellfinder.core.util.DateTimeFormatters

/**
 * Post Detail Screen displaying full post information with comments and interactions.
 * Instagram-style design with image carousel, like/comment functionality, and user interactions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToUserProfile: (String) -> Unit,
    onNavigateToComments: ((String) -> Unit)? = null,
    onNavigateToLikes: ((String) -> Unit)? = null,
    onNavigateToMapLocation: (Double, Double) -> Unit = { _, _ -> },
    onNavigateToPremium: () -> Unit = {},
    viewModel: PostDetailViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val comments = viewModel.comments.collectAsLazyPagingItems()
    val postLikes = viewModel.postLikes.collectAsLazyPagingItems()
    val realTimeLikeStatus by viewModel.realTimeLikeStatus.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    
    var showDropdownMenu by remember { mutableStateOf(false) }
    var showReportSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    
    val commentsSheetState = rememberModalBottomSheetState()
    val likesSheetState = rememberModalBottomSheetState()

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PostDetailEvent.PostLiked -> {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.post_liked)
                    )
                }
                is PostDetailEvent.PostUnliked -> {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.post_unliked)
                    )
                }
                is PostDetailEvent.CommentAdded -> {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.comment_added)
                    )
                }
                is PostDetailEvent.PostShared -> {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.post_shared)
                    )
                }
                is PostDetailEvent.PostReported -> {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.post_reported)
                    )
                }
                is PostDetailEvent.PostDeleted -> {
                    onNavigateBack()
                }
                is PostDetailEvent.NavigateToProfile -> {
                    onNavigateToProfile()
                }
                is PostDetailEvent.NavigateToUserProfile -> {
                    onNavigateToUserProfile(event.userId)
                }
                is PostDetailEvent.NavigateToMapLocation -> {
                    onNavigateToMapLocation(event.latitude, event.longitude)
                }
                is PostDetailEvent.ShowPremiumRequiredForMap -> {
                    onNavigateToPremium()
                }
                is PostDetailEvent.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message
                    )
                }
            }
        }
    }

    // Show error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(message = error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { /* Başlık kaldırıldı */ },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = SperrmullPrimary
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showDropdownMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.cd_more_options),
                                tint = SperrmullPrimary
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showDropdownMenu,
                            onDismissRequest = { showDropdownMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { 
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                            tint = SperrmullPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.share_post))
                                    }
                                },
                                onClick = { 
                                    showDropdownMenu = false
                                    viewModel.onShareClick()
                                }
                            )
                            
                            if (uiState.post?.ownerId != viewModel.getCurrentUserId()) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Outlined.Flag,
                                                contentDescription = null,
                                                tint = Color.Red,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(stringResource(R.string.report_post))
                                        }
                                    },
                                    onClick = {
                                        showDropdownMenu = false
                                        showReportSheet = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Block,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(R.string.block_user_menu),
                                                color = MaterialTheme.colorScheme.error,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    },
                                    onClick = {
                                        showDropdownMenu = false
                                        showBlockDialog = true
                                    }
                                )
                            }
                            
                            // Delete Post (only for owner)
                            if (uiState.post?.ownerId == viewModel.getCurrentUserId()) {
                                DropdownMenuItem(
                                    text = { 
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(R.string.delete_post_menu),
                                                color = MaterialTheme.colorScheme.error,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    },
                                    onClick = { 
                                        showDropdownMenu = false
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingState()
                }
                
                uiState.post == null -> {
                    ErrorState(
                        error = uiState.error ?: stringResource(R.string.post_not_found),
                        onRetry = { viewModel.retry() }
                    )
                }
                
                else -> {
                    // Safe cast to avoid smart cast issues
                    uiState.post?.let { post ->
                        PostDetailContent(
                            post = post,
                            isLikeLoading = uiState.isLikeLoading,
                            realTimeLikeStatus = realTimeLikeStatus,
                            currentImageIndex = uiState.currentImageIndex,
                            onImageIndexChange = viewModel::updateCurrentImageIndex,
                            onLikeClick = {
                                try {
                                    viewModel.onLikeClick()
                                } catch (e: Exception) {
                                    // Prevent crash on like button click
                                    android.util.Log.e("PostDetailScreen", "Error on like click", e)
                                }
                            },
                            onCommentClick = { 
                                onNavigateToComments?.invoke(post.id) ?: viewModel.showComments()
                            },
                            onShareClick = viewModel::onShareClick,
                            onLikesClick = { 
                                onNavigateToLikes?.invoke(post.id) ?: viewModel.showLikes()
                            },
                            onUserClick = viewModel::onUserClick,
                            onLocationClick = viewModel::onPostLocationClick
                        )
                    }
                }
            }
        }
    }

    // Comments Bottom Sheet
    if (uiState.showComments) {
        CommentsBottomSheet(
            postId = uiState.post?.id ?: "",
            sheetState = commentsSheetState,
            onDismiss = viewModel::hideComments,
            onUserClick = viewModel::onUserClick
        )
    }

    // Likes Bottom Sheet
    if (uiState.showLikes) {
        ModalBottomSheet(
            onDismissRequest = viewModel::hideLikes,
            sheetState = likesSheetState
        ) {
            LikesBottomSheet(
                post = uiState.post!!,
                likes = postLikes,
                onUserClick = viewModel::onUserClick
            )
        }
    }
    
    // Report Bottom Sheet
    if (showReportSheet && uiState.post != null) {
        ReportBottomSheet(
            targetId = uiState.post!!.id,
            targetType = ReportTargetType.POST,
            onDismiss = { showReportSheet = false }
        )
    }
    
    // Delete Post Dialog
    if (showDeleteDialog && uiState.post != null) {
        DeletePostDialog(
            isLoading = uiState.isDeleteLoading,
            onConfirm = {
                viewModel.onDeletePost()
            },
            onDismiss = {
                if (!uiState.isDeleteLoading) {
                    showDeleteDialog = false
                }
            }
        )
    }

    // Block Post Owner Dialog
    if (showBlockDialog && uiState.post != null) {
        val ownerUser = User(
            uid = uiState.post!!.ownerId,
            email = "",
            displayName = uiState.post!!.ownerDisplayName ?: uiState.post!!.ownerId,
            photoUrl = uiState.post!!.ownerPhotoUrl,
            city = null,
            dob = null,
            gender = null,
            xp = 0,
            level = uiState.post!!.ownerLevel ?: 1,
            honesty = 100,
            isPremium = uiState.post!!.isOwnerPremium ?: false,
            premiumUntil = null,
            badges = emptyList(),
            favorites = UserFavorites(),
            fcmToken = null,
            deviceTokens = emptyList(),
            deviceLang = "",
            deviceModel = "",
            deviceOs = "",
            frameLevel = 0,
            createdAt = null,
            updatedAt = null,
            lastLoginAt = null
        )
        BlockUserDialog(
            user = ownerUser,
            isLoading = false,
            onConfirm = { reason ->
                viewModel.blockPostOwner(reason)
                showBlockDialog = false
            },
            onDismiss = { showBlockDialog = false }
        )
    }

}

@Composable
private fun PostDetailContent(
    post: Post,
    isLikeLoading: Boolean,
    realTimeLikeStatus: Boolean?,
    currentImageIndex: Int,
    onImageIndexChange: (Int) -> Unit,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onLikesClick: () -> Unit,
    onUserClick: (String) -> Unit,
    onLocationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val liveOwner = rememberLiveUserDisplay(
        userId = post.ownerId,
        fallbackDisplayName = post.ownerDisplayName,
        fallbackPhotoUrl = post.ownerPhotoUrl
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // User header
        PostHeader(
            post = post,
            ownerDisplayName = liveOwner.displayName,
            ownerPhotoUrl = liveOwner.photoUrl,
            onUserClick = onUserClick,
            onLocationClick = onLocationClick
        )

        // Image carousel
        if (post.images.isNotEmpty()) {
            ImageCarousel(
                images = post.images,
                currentIndex = currentImageIndex,
                onIndexChange = onImageIndexChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
        }

        // Post content
        PostContent(
            post = post
        )

        // Interaction buttons
        PostInteractions(
            post = post,
            isLikeLoading = isLikeLoading,
            realTimeLikeStatus = realTimeLikeStatus,
            onLikeClick = onLikeClick,
            onCommentClick = onCommentClick,
            onShareClick = onShareClick,
            onLikesClick = onLikesClick
        )

        // Timestamp pinned at the very bottom, same ordering as PostCard.
        Text(
            text = DateTimeFormatters.formatTimeAgo(post.createdAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PostHeader(
    post: Post,
    ownerDisplayName: String?,
    ownerPhotoUrl: String?,
    onUserClick: (String) -> Unit,
    onLocationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.size(56.dp)
        ) {
            SafeGlideImage(
                imageUrl = ownerPhotoUrl,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .clickable { onUserClick(post.ownerId) },
                contentScale = ContentScale.Crop,
                contentDescription = stringResource(R.string.cd_user_avatar),
                errorPlaceholder = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onUserClick(post.ownerId) },
                        contentAlignment = Alignment.Center
                    ) {
                        val initials = ownerDisplayName?.let { name ->
                            name.split(" ")
                                .take(2)
                                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                .joinToString("")
                        } ?: "?"
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )

            if (post.isOwnerPremium == true) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                            ),
                            CircleShape
                        )
                        .padding(2.dp)
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

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = ownerDisplayName ?: stringResource(R.string.unknown_user),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { onUserClick(post.ownerId) }
            )

            buildHeaderLocationText(post)?.let { locationText ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.clickable { onLocationClick() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = stringResource(R.string.cd_location),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = locationText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun buildHeaderLocationText(post: Post): String? {
    val street = post.locationStreet?.takeIf { it.isNotBlank() }
    val city = (post.locationCity ?: post.city).takeIf { it.isNotBlank() }
    return when {
        street != null && city != null -> "$street, $city"
        street != null -> street
        city != null -> city
        else -> null
    }
}

@Composable
private fun PostInteractions(
    post: Post,
    isLikeLoading: Boolean,
    realTimeLikeStatus: Boolean?,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onLikesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Instagram-style like button with heart animation
            if (isLikeLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = Color(0xFFE91E63), // Pink loading color
                    strokeWidth = 2.dp
                )
            } else {
                LargeLikeButton(
                    isLiked = realTimeLikeStatus ?: post.isLikedByCurrentUser,
                    onLikeClick = onLikeClick,
                    enabled = !isLikeLoading
                )
            }
            
            // Comment button - Improved with better touch target
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onCommentClick() }
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = stringResource(R.string.cd_comment_post),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp) // Same size as like button
                )
            }
            
            // Share button - Improved with better touch target
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onShareClick() }
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = stringResource(R.string.cd_share_post),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp) // Same size as like button
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Like count - Always show, even if 0
        Text(
            text = if (post.likesCount > 0) {
                stringResource(R.string.likes_count, post.likesCount)
            } else {
                stringResource(R.string.no_likes_yet)
            },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .clickable { if (post.likesCount > 0) onLikesClick() }
                .padding(vertical = 4.dp)
        )
        
        // Comment count - Show if there are comments
        if (post.commentsCount > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.view_comments_count, post.commentsCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier
                    .clickable { onCommentClick() }
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun PostContent(
    post: Post,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp)
    ) {
        // Description
        if (post.description.isNotEmpty()) {
            Text(
                text = post.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Categories
        if (post.categoriesDe.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                post.categoriesDe.take(3).forEach { category ->
                    Text(
                        text = "#$category",
                        style = MaterialTheme.typography.labelMedium,
                        color = SperrmullPrimary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .background(
                                SperrmullPrimary.copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Availability voting is removed for MVP.
        // Comments link removed - now handled in PostInteractions
    }
}

@Composable
private fun LocationSection(
    post: Post,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Location icon
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = stringResource(R.string.cd_location),
                tint = SperrmullPrimary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Main location (street/district)
                Text(
                    text = post.locationStreet ?: stringResource(R.string.location_not_specified),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // City
                Text(
                    text = post.city ?: stringResource(R.string.city_not_specified),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                // Distance (if available)
                post.getFormattedDistance()?.let { distance ->
                    Text(
                        text = stringResource(R.string.distance_away, distance),
                        style = MaterialTheme.typography.bodySmall,
                        color = SperrmullPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = SperrmullPrimary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.loading_post),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.error_loading_post_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = SperrmullPrimary
            )
        ) {
            Text(
                text = stringResource(R.string.retry),
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

