package com.omerkaya.sperrmuellfinder.ui.social

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.domain.model.User

/**
 * 🔍 USER SEARCH SCREEN - SperrmüllFinder
 * Rules.md compliant - Clean Architecture UI layer
 * 
 * Features:
 * - Search users by name/username
 * - Suggested users to follow
 * - Follow/Unfollow actions
 * - User profile navigation
 * - Material 3 design with proper accessibility
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    socialViewModel: SocialViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // State
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by socialViewModel.searchResults.collectAsState()
    val uiState by socialViewModel.uiState.collectAsState()
    
    // Load suggested users on first load
    LaunchedEffect(Unit) {
        socialViewModel.getSuggestedUsers()
    }
    
    // Handle events
    LaunchedEffect(Unit) {
        socialViewModel.events.collect { event ->
            when (event) {
                is SocialEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is SocialEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.error)
                }
                is SocialEvent.NavigateToUserProfile -> {
                    onNavigateToUserProfile(event.userId)
                }
                else -> { /* Handle other events if needed */ }
            }
        }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.search_users_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
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
                .padding(paddingValues)
        ) {
            // Search bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { 
                    if (searchQuery.isNotBlank()) {
                        socialViewModel.searchUsers(searchQuery)
                        keyboardController?.hide()
                    }
                },
                onClear = {
                    searchQuery = ""
                    socialViewModel.clearSearchResults()
                    socialViewModel.getSuggestedUsers()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            
            // Results
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    // Loading state
                    uiState.isSearching || (uiState.isLoading && searchResults.isEmpty()) -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = if (uiState.isSearching) {
                                        stringResource(R.string.searching_users)
                                    } else {
                                        stringResource(R.string.loading_suggestions)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // Empty state
                    searchResults.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptySearchState(hasSearched = searchQuery.isNotBlank())
                        }
                    }
                    
                    // Results list
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Section header
                            item {
                                Text(
                                    text = if (searchQuery.isNotBlank()) {
                                        stringResource(R.string.search_results_title)
                                    } else {
                                        stringResource(R.string.suggested_users_title)
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            
                            // Users list
                            items(
                                items = searchResults,
                                key = { it.uid }
                            ) { user ->
                                UserSearchItem(
                                    user = user,
                                    onUserClick = { onNavigateToUserProfile(user.uid) },
                                    onFollowClick = { socialViewModel.toggleFollow(user.uid) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Search bar component
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = {
            Text(
                text = stringResource(R.string.search_users_hint),
                style = MaterialTheme.typography.bodyLarge
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.cd_clear),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = { onSearch() }
        ),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

/**
 * Empty search state component
 */
@Composable
private fun EmptySearchState(hasSearched: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (hasSearched) {
                stringResource(R.string.search_no_results_title)
            } else {
                stringResource(R.string.search_empty_title)
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (hasSearched) {
                stringResource(R.string.search_no_results_desc)
            } else {
                stringResource(R.string.search_empty_desc)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Individual user search item component
 */
@Composable
private fun UserSearchItem(
    user: User,
    onUserClick: () -> Unit,
    onFollowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFollowing by remember { mutableStateOf(false) } // This should be managed by ViewModel
    
    Card(
        modifier = modifier.clickable { onUserClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User avatar
            GlideImage(
                imageModel = { user.photoUrl ?: "" },
                imageOptions = ImageOptions(
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center
                ),
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ),
                failure = {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // User info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                user.city?.let { city ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = city,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // User stats
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.level_format, user.level),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Text(
                        text = stringResource(R.string.followers_count_format, user.followersCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (user.isPremium) {
                        Text(
                            text = stringResource(R.string.premium_badge),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFD700), // Gold color
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Follow button
            if (isFollowing) {
                OutlinedButton(
                    onClick = onFollowClick,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text(
                        text = stringResource(R.string.following),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            } else {
                Button(
                    onClick = onFollowClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text(
                        text = stringResource(R.string.follow),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
