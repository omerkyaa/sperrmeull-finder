package com.omerkaya.sperrmuellfinder.ui.search

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import android.content.Intent
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.core.ui.theme.SperrmullPrimary
import com.omerkaya.sperrmuellfinder.core.model.PostLocation
import com.omerkaya.sperrmuellfinder.domain.model.SearchSuggestion
import com.omerkaya.sperrmuellfinder.domain.model.SearchSuggestionType
import com.omerkaya.sperrmuellfinder.ui.home.components.PostCard
import com.omerkaya.sperrmuellfinder.ui.search.components.FilterBottomSheet
import com.omerkaya.sperrmuellfinder.ui.search.components.PremiumPaywall
import com.omerkaya.sperrmuellfinder.ui.search.components.SearchBar
import com.omerkaya.sperrmuellfinder.ui.search.components.SearchResultSkeleton
import com.omerkaya.sperrmuellfinder.ui.search.components.SearchTabs
import com.omerkaya.sperrmuellfinder.ui.search.components.SortBottomSheet
import com.omerkaya.sperrmuellfinder.ui.search.components.UserSearchResults

/**
 * Search Screen with premium gating.
 * AllTrails-inspired design with advanced search and filtering capabilities.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToMapLocation: (Double, Double) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchResults = viewModel.searchResults.collectAsLazyPagingItems()
    val userSearchResults by viewModel.userSearchResults.collectAsState()
    val searchType by viewModel.searchType.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SearchEvent.ShowPaywall -> onNavigateToPremium()
                is SearchEvent.NavigateToPostDetail -> onNavigateToPostDetail(event.postId)
                is SearchEvent.NavigateToUserProfile -> onNavigateToUserProfile(event.userId)
                is SearchEvent.NavigateToMapLocation ->
                    onNavigateToMapLocation(event.latitude, event.longitude)
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

    var postIdPendingReport by remember { mutableStateOf<String?>(null) }
    var postIdPendingDelete by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.search_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = SperrmullPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = SperrmullPrimary
                        )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar - use direct query state for immediate UI updates
            SearchBar(
                query = uiState.searchFilters.query,
                onQueryChange = { newQuery ->
                    // Update query immediately for responsive UI
                    viewModel.updateSearchQuery(newQuery)
                },
                onSearch = viewModel::performSearch,
                onFiltersClick = viewModel::showFilters,
                onSortClick = viewModel::showSort,
                onClearClick = { viewModel.updateSearchQuery("") },
                searchFilters = uiState.searchFilters,
                isPremium = uiState.isPremium,
                isEnabled = !uiState.isLoading
            )
            
            // Search tabs
            SearchTabs(
                selectedTab = searchType,
                onTabSelected = viewModel::setSearchType,
                isPremium = uiState.isPremium
            )

            // Content area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    
                    // Show suggestions when not searching and query is empty
                    uiState.searchFilters.query.isEmpty() && uiState.suggestions.isNotEmpty() -> {
                        SearchSuggestions(
                            suggestions = uiState.suggestions,
                            onSuggestionClick = viewModel::onSuggestionSelected
                        )
                    }
                    
                    // Show search results when there's a query or active search
                    uiState.searchFilters.query.isNotEmpty() || uiState.showResults -> {
                        when (searchType) {
                            com.omerkaya.sperrmuellfinder.domain.model.SearchType.POSTS -> {
                                when {
                                    uiState.isSearching -> {
                                        LoadingState()
                                    }
                                    
                                    searchResults.loadState.refresh is LoadState.Loading && searchResults.itemCount == 0 -> {
                                        LoadingState()
                                    }
                                    
                                    searchResults.loadState.refresh is LoadState.Error -> {
                                        ErrorState(
                                            error = (searchResults.loadState.refresh as LoadState.Error).error.message 
                                                ?: stringResource(R.string.error_search_failed),
                                            onRetry = { searchResults.retry() }
                                        )
                                    }
                                    
                                    searchResults.itemCount == 0 -> {
                                        EmptySearchState(
                                            query = uiState.searchFilters.query
                                        )
                                    }
                                    
                                    else -> {
                                        SearchResults(
                                            searchResults = searchResults,
                                            isPremium = uiState.isPremium,
                                            viewModel = viewModel,
                                            onPostClick = onNavigateToPostDetail,
                                            onNavigateToProfile = onNavigateToProfile,
                                            onNavigateToUserProfile = onNavigateToUserProfile,
                                            onReportClick = { postIdPendingReport = it },
                                            onDeleteClick = { postIdPendingDelete = it },
                                            onMoreClick = onNavigateToPostDetail,
                                            context = context
                                        )
                                    }
                                }
                            }
                            
                            com.omerkaya.sperrmuellfinder.domain.model.SearchType.USERS -> {
                                UserSearchResults(
                                    users = userSearchResults,
                                    onUserClick = { userId ->
                                        // Check if it's current user's profile
                                        if (userId == uiState.currentUserId) {
                                            onNavigateToProfile()
                                        } else {
                                            onNavigateToUserProfile(userId)
                                        }
                                    },
                                    isLoading = uiState.isSearching
                                )
                            }
                            
                            com.omerkaya.sperrmuellfinder.domain.model.SearchType.ALL -> {
                                // ALL tab is removed from UI; keep safe fallback to posts.
                                when {
                                    uiState.isSearching -> {
                                        LoadingState()
                                    }
                                    
                                    searchResults.itemCount == 0 -> {
                                        EmptySearchState(
                                            query = uiState.searchFilters.query
                                        )
                                    }
                                    
                                    else -> {
                                        SearchResults(
                                            searchResults = searchResults,
                                            isPremium = uiState.isPremium,
                                            viewModel = viewModel,
                                            onPostClick = onNavigateToPostDetail,
                                            onNavigateToProfile = onNavigateToProfile,
                                            onNavigateToUserProfile = onNavigateToUserProfile,
                                            onReportClick = { postIdPendingReport = it },
                                            onDeleteClick = { postIdPendingDelete = it },
                                            onMoreClick = onNavigateToPostDetail,
                                            context = context
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Show empty state when no search is active
                    else -> {
                        EmptySearchState()
                    }
                }
            }
        }
        
        // Filter Bottom Sheet
        if (uiState.showFilters) {
            FilterBottomSheet(
                sheetState = androidx.compose.material3.rememberModalBottomSheetState(),
                filters = uiState.searchFilters,
                isPremium = uiState.isPremium,
                onDismiss = viewModel::hideFilters,
                onApplyFilters = { filters ->
                    viewModel.applyFilters(filters)
                },
                onClearFilters = viewModel::clearFilters,
                onPremiumRequired = viewModel::showPaywall
            )
        }
        
        // Sort Bottom Sheet  
        if (uiState.showSort) {
            SortBottomSheet(
                currentSortBy = uiState.searchFilters.sortBy,
                onSortSelected = { sortBy ->
                    viewModel.updateSortBy(sortBy)
                },
                onDismiss = viewModel::hideSort,
                isPremium = uiState.isPremium
            )
        }

        // Delete confirmation dialog
        postIdPendingDelete?.let { targetId ->
            com.omerkaya.sperrmuellfinder.ui.post.DeletePostDialog(
                isLoading = false,
                onConfirm = {
                    viewModel.deletePost(targetId)
                    postIdPendingDelete = null
                },
                onDismiss = { postIdPendingDelete = null }
            )
        }

        // Report bottom sheet
        postIdPendingReport?.let { targetId ->
            com.omerkaya.sperrmuellfinder.ui.report.ReportBottomSheet(
                targetId = targetId,
                targetType = com.omerkaya.sperrmuellfinder.domain.model.ReportTargetType.POST,
                onDismiss = { postIdPendingReport = null }
            )
        }
    }
}

@Composable
private fun SearchSuggestions(
    suggestions: List<SearchSuggestion>,
    onSuggestionClick: (SearchSuggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(suggestions) { suggestion ->
            SuggestionItem(
                suggestion = suggestion,
                onClick = { onSuggestionClick(suggestion) }
            )
        }
    }
}

@Composable
private fun SuggestionItem(
    suggestion: SearchSuggestion,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (suggestion.type) {
                SearchSuggestionType.QUERY -> Icons.Default.Search
                SearchSuggestionType.CATEGORY -> Icons.Default.Search
                SearchSuggestionType.CITY -> Icons.Default.Search
                SearchSuggestionType.USER -> Icons.Default.Search
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = suggestion.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        
        if (suggestion.count > 0) {
            Text(
                text = suggestion.count.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun SearchResults(
    searchResults: androidx.paging.compose.LazyPagingItems<com.omerkaya.sperrmuellfinder.domain.model.Post>,
    isPremium: Boolean,
    viewModel: SearchViewModel,
    onPostClick: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    onReportClick: (String) -> Unit = {},
    onDeleteClick: (String) -> Unit = {},
    onMoreClick: (String) -> Unit = {},
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    val likeStatusMap by viewModel.likeStatusMap.collectAsState()
    val likeLoadingMap by viewModel.likeLoadingMap.collectAsState()
    val likeCountDeltaMap by viewModel.likeCountDeltaMap.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            count = searchResults.itemCount,
            key = searchResults.itemKey { it.id }
        ) { index ->
            val post = searchResults[index]
            if (post != null) {
                PostCard(
                    post = post,
                    onOwnerClick = { ownerUid, isSelf ->
                        if (isSelf) {
                            onNavigateToProfile()
                        } else {
                            onNavigateToUserProfile(ownerUid)
                        }
                    },
                    onPostClick = onPostClick,
                    onLikeClick = { postId, isLiked -> 
                        val baseCount = post.likesCount
                        viewModel.onPostLike(
                            postId = postId,
                            isCurrentlyLiked = isLiked,
                            currentLikesCount = baseCount
                        )
                    },
                    onFavoriteClick = { _, _ -> },
                    onShareClick = { postId ->
                        val postToShare = searchResults[index]
                        postToShare?.let { sharePost(context, it) }
                    },
                    onReportClick = onReportClick,
                    onDeleteClick = onDeleteClick,
                    onMoreClick = onMoreClick,
                    onLocationClick = { selectedPost ->
                        viewModel.onPostLocationClick(selectedPost)
                    },
                    isLiked = likeStatusMap[post.id] ?: post.isLikedByCurrentUser,
                    isLikeLoading = likeLoadingMap[post.id] ?: false,
                    likesCount = post.likesCount + (likeCountDeltaMap[post.id] ?: 0)
                )
            }
        }

        // Loading more indicator
        if (searchResults.loadState.append is LoadState.Loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = SperrmullPrimary,
                        modifier = Modifier.size(24.dp)
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
                text = stringResource(R.string.searching),
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
            text = stringResource(R.string.search_error_title),
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
        
        androidx.compose.material3.Button(
            onClick = onRetry,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
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

@Composable
private fun EmptySearchState(
    query: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (query != null) Icons.Default.Search else Icons.Default.TrendingUp,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (query != null) {
                stringResource(R.string.no_search_results_title)
            } else {
                stringResource(R.string.search_empty_title)
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (query != null) {
                stringResource(R.string.no_search_results_description, query)
            } else {
                stringResource(R.string.search_empty_description)
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}

/**
 * Share post functionality using Android's native share intent
 */
private fun sharePost(context: android.content.Context, post: com.omerkaya.sperrmuellfinder.domain.model.Post) {
    // Build post description with location
    val postDescription = buildString {
        if (post.description.isNotBlank()) {
            append(post.description)
        }
        
        // Add location info
        val locationText = if (!post.locationStreet.isNullOrBlank() && !post.locationCity.isNullOrBlank()) {
            "${post.locationStreet}, ${post.locationCity}"
        } else if (!post.locationCity.isNullOrBlank()) {
            post.locationCity
        } else if (!post.city.isNullOrBlank()) {
            post.city
        } else {
            context.getString(R.string.location_unknown)
        }
        
        if (isNotEmpty()) append("\n")
        append("📍 $locationText")
    }
    
    // App store link (placeholder - should be actual Play Store link)
    val appStoreLink = "https://play.google.com/store/apps/details?id=com.omerkaya.sperrmuellfinder"
    
    // Use the updated string format
    val shareText = context.getString(
        R.string.share_post_text,
        postDescription,
        appStoreLink
    )
    
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_post_subject))
    }
    
    val chooserIntent = Intent.createChooser(
        shareIntent,
        context.getString(R.string.share_chooser_title)
    )
    
    context.startActivity(chooserIntent)
}
