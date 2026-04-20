package com.omerkaya.sperrmuellfinder.ui.home

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import com.omerkaya.sperrmuellfinder.BuildConfig
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.ui.ads.AdEligibilityInputs
import com.omerkaya.sperrmuellfinder.ui.home.components.PostCard
import com.omerkaya.sperrmuellfinder.ui.home.components.WelcomeBanner
import com.omerkaya.sperrmuellfinder.ui.ads.BannerAdHost
import com.omerkaya.sperrmuellfinder.ui.post.DeletePostDialog
import com.omerkaya.sperrmuellfinder.ui.report.ReportBottomSheet
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.omerkaya.sperrmuellfinder.domain.model.ReportTargetType

/**
 * 🏠 HOME SCREEN - SperrmüllFinder
 * Rules.md compliant - Clean Architecture UI layer
 * 
 * Features:
 * - Scrollable top app bar with notification badge
 * - Pull-to-refresh functionality
 * - Paging 3 infinite scroll feed
 * - Welcome banner with auto-dismiss
 * - Real-time like/comment interactions
 * - Professional loading/empty/error states
 * - Material 3 design with proper accessibility
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    onNavigateToLikes: (String) -> Unit,
    onNavigateToComments: (String) -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToMapLocation: (Double, Double) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Collect state
    val uiState by viewModel.uiState.collectAsState()
    val isPremiumUser by viewModel.isPremiumUser.collectAsState()
    val areAdsEnabled by viewModel.areAdsEnabled.collectAsState()
    val unreadCount by viewModel.notificationCount.collectAsState()
    val realTimeLikeStatus by viewModel.realTimeLikeStatus.collectAsState()
    val realTimeLikeCounts by viewModel.realTimeLikeCounts.collectAsState()
    val likeLoadingStates by viewModel.likeLoadingStates.collectAsState()
    val pagedPosts = viewModel.postFeed.collectAsLazyPagingItems()
    var postIdPendingDelete by remember { mutableStateOf<String?>(null) }
    var isDeleteLoading by remember { mutableStateOf(false) }
    var postIdPendingReport by remember { mutableStateOf<String?>(null) }
    val bannerAdUnitId = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/6300978111"
    } else {
        BuildConfig.ADMOB_HOME_BANNER_AD_UNIT_ID
    }
    val adEligibilityInputs = AdEligibilityInputs(
        adsEnabled = areAdsEnabled,
        isRevenueCatPremium = isPremiumUser,
        isFirestorePremium = false,
        hasRevenueCatSignal = true,
        hasConsent = false,
        forceTestMode = BuildConfig.DEBUG
    )
    val showBannerAds = areAdsEnabled && !isPremiumUser
    val shouldShowPersistentBanner = showBannerAds &&
        uiState.welcomeBannerDismissed &&
        !uiState.showWelcomeBanner
    
    // Scroll behavior for collapsing top bar
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAdsPolicy()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.NavigateToNotifications -> onNavigateToNotifications()
                is HomeEvent.NavigateToProfile -> onNavigateToProfile()
                is HomeEvent.NavigateToPostDetail -> onNavigateToPostDetail(event.postId)
                is HomeEvent.NavigateToUserProfile -> onNavigateToUserProfile(event.userId)
                is HomeEvent.NavigateToLikes -> onNavigateToLikes(event.postId)
                is HomeEvent.NavigateToComments -> onNavigateToComments(event.postId)
                is HomeEvent.SharePost -> sharePost(context, event.postId)
                is HomeEvent.NavigateToMapLocation ->
                    onNavigateToMapLocation(event.latitude, event.longitude)
                is HomeEvent.ShowPremiumRequiredForMap -> onNavigateToPremium()
                is HomeEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(
                        message = context.getString(event.messageResId)
                    )
                }
                is HomeEvent.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = context.getString(event.messageResId)
                    )
                }
                // Real-time Firebase integration - no manual refresh needed
            }
        }
    }
    
    
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        // App Logo
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = stringResource(R.string.cd_app_logo),
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        // App Name
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Notification icon with badge
                    BadgedBox(
                        badge = {
                            if (unreadCount > 0) {
                                Badge {
                                    Text(
                                        text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    ) {
                        IconButton(
                            onClick = viewModel::onNotificationClick
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = stringResource(R.string.cd_notification_icon),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    bottom = paddingValues.calculateBottomPadding() + 80.dp // Bottom nav space
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Welcome banner
                if (uiState.showWelcomeBanner && uiState.currentUser != null) {
                    item {
                        WelcomeBanner(
                            user = uiState.currentUser!!,
                            isVisible = uiState.showWelcomeBanner,
                            onDismiss = viewModel::dismissWelcomeBanner,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
                
                // Posts feed
                items(
                    count = pagedPosts.itemCount,
                    key = pagedPosts.itemKey { it.id }
                ) { index ->
                    val post = pagedPosts[index]
                    if (post != null) {
                        // Use like status from FeedPagingSource (already optimized with batch queries)
                        val currentLikeStatus = realTimeLikeStatus[post.id] ?: post.isLikedByCurrentUser
                        val isLikeLoading = likeLoadingStates[post.id] ?: false
                        
                        // Initialize real-time listeners for this post
                        LaunchedEffect(post.id) {
                            viewModel.initializeLikeStatusForPost(post.id)
                        }
                        
                        PostCard(
                            post = post,
                            onOwnerClick = { ownerUid, isSelf ->
                                viewModel.onUserClick(ownerUid, isSelf)
                            },
                            onPostClick = viewModel::onPostClick,
                            onLikeClick = { postId, isLiked ->
                                viewModel.onPostLike(postId, isLiked)
                            },
                            onFavoriteClick = { postId, isFavorited ->
                                // Favorite functionality disabled per requirements
                            },
                            onShareClick = viewModel::onShareClick,
                            onReportClick = { postId ->
                                postIdPendingReport = postId
                            },
                            onDeleteClick = { postId ->
                                postIdPendingDelete = postId
                            },
                            onLikesClick = viewModel::onLikesClick,
                            onCommentsClick = viewModel::onCommentsClick,
                            onLocationClick = { selectedPost ->
                                viewModel.onPostLocationClick(selectedPost)
                            },
                            onMoreClick = { /* TODO: Implement more options */ },
                            isLiked = currentLikeStatus,
                            isLikeLoading = isLikeLoading,
                            likesCount = realTimeLikeCounts[post.id],
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Loading state
                when (pagedPosts.loadState.append) {
                    is LoadState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                    is LoadState.Error -> {
                        item {
                            ErrorItem(
                                message = stringResource(R.string.feed_error_desc),
                                onRetry = { pagedPosts.retry() }
                            )
                        }
                    }
                    else -> {}
                }
                
                // Initial loading state
                if (pagedPosts.loadState.refresh is LoadState.Loading && pagedPosts.itemCount == 0) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                                CircularProgressIndicator()
            Text(
                                    text = stringResource(R.string.feed_loading),
                style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // Empty state
                if (pagedPosts.loadState.refresh is LoadState.NotLoading && pagedPosts.itemCount == 0) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyFeedState()
                        }
                    }
                }
                
                // Error state
                if (pagedPosts.loadState.refresh is LoadState.Error) {
            item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                                .height(400.dp),
                contentAlignment = Alignment.Center
            ) {
                            ErrorItem(
                                message = stringResource(R.string.feed_error_desc),
                                onRetry = { pagedPosts.refresh() }
                            )
                        }
                    }
                }
            }
            
            if (shouldShowPersistentBanner) {
                PersistentBannerSection(
                    adUnitId = bannerAdUnitId,
                    adEligibilityInputs = adEligibilityInputs,
                    onCloseRequested = onNavigateToPremium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }

    if (postIdPendingDelete != null) {
        DeletePostDialog(
            isLoading = isDeleteLoading,
            onConfirm = {
                val targetPostId = postIdPendingDelete ?: return@DeletePostDialog
                isDeleteLoading = true
                viewModel.onDeletePost(targetPostId) { success ->
                    isDeleteLoading = false
                    if (success) {
                        postIdPendingDelete = null
                        pagedPosts.refresh()
                    }
                }
            },
            onDismiss = {
                if (!isDeleteLoading) {
                    postIdPendingDelete = null
                }
            }
        )
    }

    if (postIdPendingReport != null) {
        ReportBottomSheet(
            targetId = postIdPendingReport!!,
            targetType = ReportTargetType.POST,
            onDismiss = { postIdPendingReport = null }
        )
    }
}

@Composable
private fun PersistentBannerSection(
    adUnitId: String,
    adEligibilityInputs: AdEligibilityInputs,
    onCloseRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            BannerAdHost(
                adUnitId = adUnitId,
                eligibilityInputs = adEligibilityInputs,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            )
            IconButton(
                onClick = onCloseRequested,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_close_banner),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Empty feed state component
 */
@Composable
private fun EmptyFeedState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Text(
            text = stringResource(R.string.feed_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.feed_empty_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Error item component with retry functionality
 */
@Composable
private fun ErrorItem(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Text(
            text = stringResource(R.string.feed_error_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        androidx.compose.material3.Button(
            onClick = onRetry
        ) {
            Text(stringResource(R.string.action_retry))
        }
    }
}

/**
 * Share post functionality
 */
private fun sharePost(context: Context, postId: String) {
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "Check out this post on SperrmüllFinder: https://sperrmuellfinder.app/post/$postId")
        putExtra(Intent.EXTRA_SUBJECT, "SperrmüllFinder Post")
    }
    
    val chooserIntent = Intent.createChooser(shareIntent, "Share Post")
    context.startActivity(chooserIntent)
}
