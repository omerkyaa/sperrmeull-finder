package com.omerkaya.sperrmuellfinder.ui.home.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.skydoves.landscapist.ImageOptions
import com.omerkaya.sperrmuellfinder.ui.common.SafeGlideImage
import com.omerkaya.sperrmuellfinder.ui.components.SmallLikeButton
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.core.util.DateTimeFormatters
import com.omerkaya.sperrmuellfinder.core.util.LocationFormatters
import com.omerkaya.sperrmuellfinder.core.util.NumberShortener
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants
import com.omerkaya.sperrmuellfinder.domain.model.Post
import com.omerkaya.sperrmuellfinder.domain.model.PostStatus
import com.omerkaya.sperrmuellfinder.domain.model.PremiumFrameType
import com.omerkaya.sperrmuellfinder.core.model.PostLocation
import com.omerkaya.sperrmuellfinder.ui.common.CompactProfilePhoto
import com.omerkaya.sperrmuellfinder.ui.common.rememberLiveUserDisplay
import java.util.Date
import java.util.Locale

/**
 * 🏠 HOME FEED POST CARD - SperrmüllFinder Design
 * Modern, professional post card with realtime data display and localization support.
 * Rules.md compliant - Clean Architecture UI component with proper string resources.
 * 
 * Features:
 * - Realtime post data with denormalized user info
 * - Localized date/time formatting (DE/EN)
 * - Professional location formatting
 * - Premium frame overlays for user avatars
 * - Level badges and premium indicators
 * - Navigation callbacks for profile/post clicks
 * - Like/Comment/Share/Save interactions
 * - Accessibility support with content descriptions
 * - Shimmer/skeleton loading states
 * - Error handling for image loading
 */
@Composable
fun PostCard(
    post: Post,
    onOwnerClick: (ownerUid: String, isSelf: Boolean) -> Unit,
    onPostClick: (postId: String) -> Unit,
    onLikeClick: (postId: String, isLiked: Boolean) -> Unit,
    onFavoriteClick: (postId: String, isFavorited: Boolean) -> Unit, // Kept for compatibility but not used
    onShareClick: (postId: String) -> Unit,
    onReportClick: (postId: String) -> Unit = {},
    onDeleteClick: (postId: String) -> Unit = {},
    onBlockOwnerClick: (ownerId: String) -> Unit = {},
    onLikesClick: (postId: String) -> Unit = {},
    onCommentsClick: (postId: String) -> Unit = {},
    onLocationClick: (Post) -> Unit = {},
    onMoreClick: (postId: String) -> Unit = {},
    isLiked: Boolean = false, // Current like status for optimistic UI
    isLikeLoading: Boolean = false, // Loading state for like button
    isFavorited: Boolean = false, // Current favorite status
    likesCount: Int? = null, // Real-time likes count (null = use post.likesCount)
    modifier: Modifier = Modifier
) {
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid }
    val liveOwner = rememberLiveUserDisplay(
        userId = post.ownerId,
        fallbackDisplayName = post.ownerDisplayName,
        fallbackPhotoUrl = post.ownerPhotoUrl
    )
    val resolvedOwnerDisplayName = liveOwner.displayName
        ?.takeIf { it.isNotBlank() }
        ?: liveOwner.nickname?.takeIf { it.isNotBlank() }
        ?: post.ownerDisplayName?.takeIf { it.isNotBlank() }
        ?: post.getEffectiveOwnerDisplayName()
    val resolvedOwnerPhotoUrl = liveOwner.photoUrl ?: post.ownerPhotoUrl
    
    // Animation states for like interactions
    var showDoubleTapAnimation by remember { mutableStateOf(false) }
    var likeButtonScale by remember { mutableStateOf(1f) }
    var showMoreMenu by remember { mutableStateOf(false) }
    val isOwner = currentUserId == post.ownerId
    
    // Animated scale for like button
    val animatedLikeScale by animateFloatAsState(
        targetValue = likeButtonScale,
        animationSpec = tween(durationMillis = 150),
        finishedListener = { likeButtonScale = 1f }
    )
    
    // Double tap animation effect
    LaunchedEffect(showDoubleTapAnimation) {
        if (showDoubleTapAnimation) {
            delay(800) // Show animation for 800ms
            showDoubleTapAnimation = false
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onPostClick(post.id) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        shape = RoundedCornerShape(12.dp)
        ) {
            Column {
            // Header: Profile photo, name, level, premium badge, overflow menu
                PostHeader(
                    post = post,
                    ownerDisplayName = resolvedOwnerDisplayName,
                    ownerPhotoUrl = resolvedOwnerPhotoUrl,
                    currentUserId = currentUserId,
                    onOwnerClick = onOwnerClick,
                    onLocationClick = onLocationClick,
                    isOwner = isOwner,
                    showMoreMenu = showMoreMenu,
                    onShowMoreMenu = {
                        onMoreClick(post.id)
                        showMoreMenu = true
                    },
                    onDismissMoreMenu = { showMoreMenu = false },
                    onShareClick = {
                        showMoreMenu = false
                        onShareClick(post.id)
                    },
                    onReportClick = {
                        showMoreMenu = false
                        onReportClick(post.id)
                    },
                    onDeleteClick = {
                        showMoreMenu = false
                        onDeleteClick(post.id)
                    },
                    onBlockOwnerClick = {
                        showMoreMenu = false
                        onBlockOwnerClick(post.ownerId)
                    }
                )
            
            // Main image with double tap to like
            PostImage(
                imageUrl = post.images.firstOrNull(),
                imageCount = post.images.size,
                onImageClick = { onPostClick(post.id) },
                onDoubleTap = {
                    // Double-tap only adds a like, never removes
                    showDoubleTapAnimation = true
                    likeButtonScale = 1.3f
                    if (!isLiked) onLikeClick(post.id, isLiked)
                },
                showDoubleTapAnimation = showDoubleTapAnimation
            )

            // Action bar: Like, Comment, Share (Favorite removed per requirements)
            PostActionBar(
                post = post,
                onLikeClick = { 
                    likeButtonScale = 1.3f
                    onLikeClick(post.id, isLiked) 
                },
                onCommentClick = { onCommentsClick(post.id) }, // Navigate to comments screen
                onShareClick = { onShareClick(post.id) },
                isLiked = isLiked,
                isLikeLoading = isLikeLoading,
                likeButtonScale = animatedLikeScale
            )
            
            // Likes count - Use real-time count if available, otherwise use post count
            val currentLikesCount = likesCount ?: post.likesCount
            if (currentLikesCount > 0) {
                PostLikesCount(
                    likesCount = currentLikesCount,
                    onLikesClick = { onLikesClick(post.id) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            
            // Comments count
            if (post.commentsCount > 0) {
                PostCommentsCount(
                    commentsCount = post.commentsCount,
                    onCommentsClick = { onCommentsClick(post.id) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Timestamp pinned to the very bottom of the card (below likes/comments).
            Text(
                text = DateTimeFormatters.formatTimeAgo(post.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

        }
    }
}

@Composable
private fun PostHeader(
    post: Post,
    ownerDisplayName: String,
    ownerPhotoUrl: String?,
    currentUserId: String?,
    onOwnerClick: (String, Boolean) -> Unit,
    onLocationClick: (Post) -> Unit,
    isOwner: Boolean,
    showMoreMenu: Boolean,
    onShowMoreMenu: () -> Unit,
    onDismissMoreMenu: () -> Unit,
    onShareClick: () -> Unit,
    onReportClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onBlockOwnerClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {

        CompactProfilePhoto(
            photoUrl = ownerPhotoUrl,
            displayName = ownerDisplayName,
            premiumFrameType = post.ownerPremiumFrameType,
            onClick = {
                val isSelf = currentUserId == post.ownerId
                onOwnerClick(post.ownerId, isSelf)
            }
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = ownerDisplayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable {
                    val isSelf = currentUserId == post.ownerId
                    onOwnerClick(post.ownerId, isSelf)
                }
            )

            buildHeaderLocationText(post)?.let { locationText ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.clickable { onLocationClick(post) },
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
        
        // Sağ üst: More options menu (PostDetail-style)
        Box {
            IconButton(onClick = onShowMoreMenu) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.post_options_cd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = showMoreMenu,
                onDismissRequest = onDismissMoreMenu
            ) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.share_post))
                        }
                    },
                    onClick = onShareClick
                )
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
                    onClick = onReportClick
                )
                if (isOwner) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
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
                        onClick = onDeleteClick
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
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.block_user_menu),
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        },
                        onClick = onBlockOwnerClick
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
private fun PremiumFrameOverlay(
    frameType: PremiumFrameType,
    modifier: Modifier = Modifier
) {
    val frameColor = when (frameType) {
        PremiumFrameType.GOLD -> Color(0xFFFFD700)
        PremiumFrameType.DIAMOND -> Color(0xFF00FFFF)
        else -> Color.Transparent
    }
    
    if (frameColor != Color.Transparent) {
        Box(
            modifier = modifier
                .background(
                    color = frameColor.copy(alpha = 0.3f),
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun LevelBadge(
    level: Int,
    isPremium: Boolean
) {
    val backgroundColor = if (isPremium) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    
    val textColor = if (isPremium) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
                    Text(
            text = stringResource(R.string.level_short, level),
                        style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun PostImage(
    imageUrl: String?,
    imageCount: Int,
    onImageClick: () -> Unit,
    onDoubleTap: () -> Unit = {},
    showDoubleTapAnimation: Boolean = false
) {
    if (imageUrl != null) {
        Box(
        modifier = Modifier
            .fillMaxWidth()
                .aspectRatio(1f)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onImageClick() },
                        onDoubleTap = { onDoubleTap() }
                    )
                }
        ) {
            SafeGlideImage(
                imageModel = imageUrl,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                contentDescription = "Post image"
            )
            
            // Image count indicator
            if (imageCount > 1) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "1/$imageCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            // Double tap animation overlay
            if (showDoubleTapAnimation) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = stringResource(R.string.like_animation_content_description),
                        tint = Color.White,
                        modifier = Modifier
                            .size(80.dp)
                            .graphicsLayer {
                                scaleX = 1.2f
                                scaleY = 1.2f
                                alpha = 0.8f
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun PostActionBar(
    post: Post,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    isLiked: Boolean = false,
    isLikeLoading: Boolean = false,
    likeButtonScale: Float = 1f
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        // Instagram-style like button with heart animation and loading state
        if (isLikeLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(28.dp)
                    .padding(4.dp),
                color = Color(0xFFE91E63), // Pink loading color
                strokeWidth = 2.dp
            )
        } else {
            SmallLikeButton(
                isLiked = isLiked,
                onLikeClick = onLikeClick,
                enabled = !isLikeLoading,
                modifier = Modifier
                    .padding(4.dp)
                    .graphicsLayer {
                        scaleX = likeButtonScale
                        scaleY = likeButtonScale
                    }
            )
        }
        
        // Comment button
        IconButton(onClick = onCommentClick) {
            Icon(
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = stringResource(R.string.cd_comment_button),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp) // Same size as like button
            )
        }
        
        // Share button
        IconButton(onClick = onShareClick) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = stringResource(R.string.cd_share_button),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp) // Same size as like button
            )
        }
    }
}

@Composable
private fun PostLikesCount(
    likesCount: Int,
    onLikesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.likes_count, likesCount),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.clickable { onLikesClick() }
    )
}

@Composable
private fun PostCommentsCount(
    commentsCount: Int,
    onCommentsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.comments_count, commentsCount),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.clickable { onCommentsClick() }
    )
}

@Composable
private fun CompactLocationSection(
    post: Post,
    modifier: Modifier = Modifier
) {
    // Only show if we have location data
    val hasLocationData = !post.locationStreet.isNullOrBlank() || !post.city.isNullOrBlank()
    
    if (hasLocationData) {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Location icon
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = stringResource(R.string.cd_location),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            
            Spacer(modifier = Modifier.width(6.dp))
            
            // Location text - compact format
            val locationText = buildString {
                if (!post.locationStreet.isNullOrBlank()) {
                    append(post.locationStreet)
                    if (!post.city.isNullOrBlank()) {
                        append(", ")
                    }
                }
                if (!post.city.isNullOrBlank()) {
                    append(post.city)
                }
            }
            
            Text(
                text = locationText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            // Distance (if available)
            post.getFormattedDistance()?.let { distance ->
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = distance,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun PostMetadata(
    post: Post,
    locale: Locale,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Date - using relative time formatting with string resources
        val dateText = DateTimeFormatters.formatTimeAgo(post.createdAt)
        Text(
            text = dateText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Location
        val locationText = LocationFormatters.formatStreetCity(
            street = post.locationStreet,
            city = post.locationCity ?: post.city,
            locale = locale
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = locationText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Format relative time using string resources for localization
 */

// Preview functions for testing different scenarios
@Preview(showBackground = true)
@Composable
private fun PostCardPreview() {
    val samplePost = Post(
        id = "sample_post_1",
        ownerId = "user_123",
        ownerDisplayName = "Max Mustermann",
        ownerPhotoUrl = null,
        ownerLevel = 7,
        isOwnerPremium = false,
        ownerPremiumFrameType = PremiumFrameType.NONE,
        images = listOf("https://example.com/image1.jpg"),
        description = "Schöner alter Stuhl, noch in gutem Zustand",
        location = PostLocation(52.5200, 13.4050),
        locationStreet = "Musterstraße 123",
        locationCity = "Berlin",
        city = "Berlin",
        likesCount = 42,
        commentsCount = 8,
        createdAt = Date(),
        isLikedByCurrentUser = false
    )
    
    PostCard(
        post = samplePost,
        onOwnerClick = { _, _ -> },
        onPostClick = { },
        onLikeClick = { _, _ -> },
        onFavoriteClick = { _, _ -> },
        onShareClick = { },
        onReportClick = { },
        onDeleteClick = { },
        onMoreClick = { }
    )
}

@Preview(showBackground = true)
@Composable
private fun PostCardPremiumPreview() {
    val samplePost = Post(
        id = "sample_post_2",
        ownerId = "user_456",
        ownerDisplayName = "Anna Premium",
        ownerPhotoUrl = null,
        ownerLevel = 15,
        isOwnerPremium = true,
        ownerPremiumFrameType = PremiumFrameType.DIAMOND,
        images = listOf("https://example.com/image1.jpg", "https://example.com/image2.jpg"),
        description = "Premium user post with multiple images",
        location = PostLocation(52.5200, 13.4050),
        locationStreet = "Premiumstraße 456",
        locationCity = "München",
        city = "München",
        likesCount = 1234,
        commentsCount = 89,
        createdAt = Date(System.currentTimeMillis() - 3600000), // 1 hour ago
        isLikedByCurrentUser = true
    )
    
    PostCard(
        post = samplePost,
        onOwnerClick = { _, _ -> },
        onPostClick = { },
        onLikeClick = { _, _ -> },
        onFavoriteClick = { _, _ -> },
        onShareClick = { },
        onReportClick = { },
        onDeleteClick = { },
        onMoreClick = { }
    )
}
