package com.omerkaya.sperrmuellfinder.ui.postdetail.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.core.ui.theme.SperrmullPrimary
import com.omerkaya.sperrmuellfinder.domain.model.Post
import com.omerkaya.sperrmuellfinder.domain.model.PremiumFrameType
import com.omerkaya.sperrmuellfinder.domain.model.User

/**
 * Likes bottom sheet component for displaying users who liked the post.
 * Instagram-style likes interface with user profiles and premium indicators.
 */
@Composable
fun LikesBottomSheet(
    post: Post,
    likes: LazyPagingItems<User>,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Header
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Handle bar
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        RoundedCornerShape(2.dp)
                    )
                    .align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.likes_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Divider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
        
        // Likes list
        Box(
            modifier = Modifier.weight(1f)
        ) {
            when {
                likes.loadState.refresh is LoadState.Loading && likes.itemCount == 0 -> {
                    LoadingLikesState()
                }
                
                likes.loadState.refresh is LoadState.Error -> {
                    ErrorLikesState(
                        error = (likes.loadState.refresh as LoadState.Error).error.message 
                            ?: stringResource(R.string.error_loading_likes),
                        onRetry = { likes.retry() }
                    )
                }
                
                likes.itemCount == 0 -> {
                    EmptyLikesState()
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            count = likes.itemCount,
                            key = likes.itemKey { it.uid }
                        ) { index ->
                            val user = likes[index]
                            if (user != null) {
                                LikeItem(
                                    user = user,
                                    onUserClick = onUserClick
                                )
                            }
                        }
                        
                        // Loading more indicator
                        if (likes.loadState.append is LoadState.Loading) {
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
            }
        }
    }
}

@Composable
private fun LikeItem(
    user: User,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onUserClick(user.uid) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // User avatar with premium frame
        Box {
            if (!user.photoUrl.isNullOrEmpty()) {
                GlideImage(
                    imageModel = { user.photoUrl!! },
                    imageOptions = ImageOptions(
                        contentScale = ContentScale.Crop,
                        contentDescription = stringResource(R.string.cd_user_avatar)
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    failure = {
                        // Show initials if image fails to load
                        Box(
                            modifier = Modifier
                                .size(48.dp)
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
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            } else {
                // Show initials when no photo URL
                Box(
                    modifier = Modifier
                        .size(48.dp)
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
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Premium frame overlay
            if (user.isPremium) {
                val frameType = user.getPremiumFrameType()
                if (frameType != PremiumFrameType.NONE) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                brush = when (frameType) {
                                    PremiumFrameType.GOLD -> androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(
                                            androidx.compose.ui.graphics.Color(0xFFFFD700),
                                            androidx.compose.ui.graphics.Color(0xFFFFA500)
                                        )
                                    )
                                    PremiumFrameType.DIAMOND -> androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(
                                            androidx.compose.ui.graphics.Color(0xFFB9F2FF),
                                            androidx.compose.ui.graphics.Color(0xFF00CED1)
                                        )
                                    )
                                    else -> androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(
                                            androidx.compose.ui.graphics.Color.Transparent,
                                            androidx.compose.ui.graphics.Color.Transparent
                                        )
                                    )
                                },
                                shape = CircleShape
                            )
                    )
                }
            }
        }
        
        // User info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Level badge
                Text(
                    text = "Lv.${user.level}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SperrmullPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(
                            SperrmullPrimary.copy(alpha = 0.1f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                
                // Premium badge
                if (user.isPremium) {
                    Text(
                        text = "★",
                        style = MaterialTheme.typography.labelSmall,
                        color = androidx.compose.ui.graphics.Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(
                                androidx.compose.ui.graphics.Color(0xFFFFD700).copy(alpha = 0.1f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            
            if (!user.city.isNullOrEmpty()) {
                Text(
                    text = user.city!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun LoadingLikesState() {
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
                text = stringResource(R.string.loading_likes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ErrorLikesState(
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
            text = stringResource(R.string.error_loading_likes_title),
            style = MaterialTheme.typography.titleMedium,
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
        
        Spacer(modifier = Modifier.height(16.dp))
        
        androidx.compose.material3.TextButton(
            onClick = onRetry
        ) {
            Text(
                text = stringResource(R.string.retry),
                color = SperrmullPrimary
            )
        }
    }
}

@Composable
private fun EmptyLikesState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.no_likes_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.no_likes_description),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}
