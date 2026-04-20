package com.omerkaya.sperrmuellfinder.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.domain.model.Post
import com.omerkaya.sperrmuellfinder.domain.model.PostStatus
import com.omerkaya.sperrmuellfinder.domain.model.PremiumFrameType
import com.omerkaya.sperrmuellfinder.core.model.PostLocation
import com.omerkaya.sperrmuellfinder.ui.profile.components.PostThumbnail
import java.util.Date

/**
 * 🔥 FAVORITES SCREEN - SperrmüllFinder
 * Rules.md compliant - User's favorited posts with Premium/Basic gating
 * 
 * Features:
 * - Grid layout for favorited posts
 * - Premium gating: Basic users see thumbnails only, Premium users can open full details
 * - Real-time updates when favorites change
 * - Empty state with helpful messaging
 * - Loading states with shimmer effect
 * - Professional Material3 design
 */
@Composable
fun FavoritesScreen(
    onPostClick: (postId: String) -> Unit,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        when {
            uiState.isLoading -> {
                FavoritesLoadingState()
            }
            
            uiState.error != null -> {
                FavoritesErrorState(
                    error = uiState.error!!,
                    onRetryClick = { viewModel.loadFavorites() }
                )
            }
            
            uiState.favorites.isEmpty() -> {
                FavoritesEmptyState()
            }
            
            else -> {
                FavoritesContent(
                    favorites = uiState.favorites,
                    isPremium = uiState.isPremium,
                    onPostClick = onPostClick,
                    onUpgradeClick = onUpgradeClick
                )
            }
        }
    }
}

@Composable
private fun FavoritesLoadingState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = stringResource(R.string.favorites_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FavoritesErrorState(
    error: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            
            Text(
                text = stringResource(R.string.favorites_error_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            androidx.compose.material3.Button(
                onClick = onRetryClick
            ) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun FavoritesEmptyState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = stringResource(R.string.favorites_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = stringResource(R.string.favorites_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FavoritesContent(
    favorites: List<Post>,
    isPremium: Boolean,
    onPostClick: (postId: String) -> Unit,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (isPremium) 2 else 3), // Premium: larger thumbnails, Basic: smaller
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = favorites,
            key = { post -> post.id }
        ) { post ->
            PostThumbnail(
                post = post,
                isPremium = isPremium,
                onClick = { 
                    if (isPremium) {
                        onPostClick(post.id)
                    } else {
                        // Basic users can't open full details - show upgrade prompt
                        onUpgradeClick()
                    }
                },
                showPremiumOverlay = !isPremium
            )
        }
    }
}

// Preview functions
@Preview(showBackground = true)
@Composable
private fun FavoritesScreenPreview() {
    val samplePosts = listOf(
        Post(
            id = "fav_post_1",
            ownerId = "user_123",
            ownerDisplayName = "Max Mustermann",
            ownerPhotoUrl = null,
            ownerLevel = 5,
            isOwnerPremium = false,
            ownerPremiumFrameType = PremiumFrameType.NONE,
            images = listOf("https://example.com/image1.jpg"),
            description = "Favorited post 1",
            location = PostLocation(52.5200, 13.4050),
            locationStreet = "Musterstraße 123",
            locationCity = "Berlin",
            city = "Berlin",
            likesCount = 15,
            commentsCount = 3,
            createdAt = Date(),
            isLikedByCurrentUser = false,
            isFavoritedByCurrentUser = true
        ),
        Post(
            id = "fav_post_2",
            ownerId = "user_456",
            ownerDisplayName = "Anna Premium",
            ownerPhotoUrl = null,
            ownerLevel = 12,
            isOwnerPremium = true,
            ownerPremiumFrameType = PremiumFrameType.GOLD,
            images = listOf("https://example.com/image2.jpg"),
            description = "Favorited post 2",
            location = PostLocation(52.5200, 13.4050),
            locationStreet = "Premiumstraße 456",
            locationCity = "München",
            city = "München",
            likesCount = 42,
            commentsCount = 8,
            createdAt = Date(),
            isLikedByCurrentUser = true,
            isFavoritedByCurrentUser = true
        )
    )
    
    FavoritesContent(
        favorites = samplePosts,
        isPremium = true,
        onPostClick = { },
        onUpgradeClick = { }
    )
}

@Preview(showBackground = true)
@Composable
private fun FavoritesEmptyPreview() {
    FavoritesEmptyState()
}

@Preview(showBackground = true)
@Composable
private fun FavoritesLoadingPreview() {
    FavoritesLoadingState()
}
