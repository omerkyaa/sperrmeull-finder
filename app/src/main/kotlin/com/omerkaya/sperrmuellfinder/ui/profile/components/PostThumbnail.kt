package com.omerkaya.sperrmuellfinder.ui.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.domain.model.Post
import com.omerkaya.sperrmuellfinder.domain.model.PostStatus
import com.omerkaya.sperrmuellfinder.domain.model.PremiumFrameType
import com.omerkaya.sperrmuellfinder.core.model.PostLocation
import java.util.Date

/**
 * 🔥 POST THUMBNAIL COMPONENT - SperrmüllFinder
 * Rules.md compliant - Thumbnail view for posts in grid layouts
 * 
 * Features:
 * - Premium gating: Basic users see lock overlay, Premium users see full thumbnails
 * - Professional Material3 design with rounded corners
 * - Landscapist-Glide image loading with error handling
 * - Multiple image indicator
 * - Accessibility support
 * - Optimized for grid layouts (Pinterest-style)
 */
@Composable
fun PostThumbnail(
    post: Post,
    isPremium: Boolean,
    onClick: () -> Unit,
    showPremiumOverlay: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Post image
            PostThumbnailImage(
                imageUrl = post.images.firstOrNull(),
                imageCount = post.images.size,
                modifier = Modifier.fillMaxSize()
            )
            
            // Premium overlay for Basic users
            if (showPremiumOverlay && !isPremium) {
                PremiumOverlay(
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Multiple images indicator
            if (post.images.size > 1) {
                MultipleImagesIndicator(
                    imageCount = post.images.size,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun PostThumbnailImage(
    imageUrl: String?,
    imageCount: Int,
    modifier: Modifier = Modifier
) {
    if (imageUrl != null) {
        GlideImage(
            imageModel = { imageUrl },
            imageOptions = ImageOptions(
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center
            ),
            modifier = modifier.clip(RoundedCornerShape(8.dp)),
            failure = {
                // Error placeholder
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.cd_image_load_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    } else {
        // No image placeholder
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_image_available),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PremiumOverlay(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = stringResource(R.string.premium_required),
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun MultipleImagesIndicator(
    imageCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.7f)
    ) {
        Text(
            text = "+$imageCount",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

// Preview functions
@Preview(showBackground = true)
@Composable
private fun PostThumbnailPreview() {
    val samplePost = Post(
        id = "sample_post_1",
        ownerId = "user_123",
        ownerDisplayName = "Max Mustermann",
        ownerPhotoUrl = null,
        ownerLevel = 7,
        isOwnerPremium = false,
        ownerPremiumFrameType = PremiumFrameType.NONE,
        images = listOf("https://example.com/image1.jpg", "https://example.com/image2.jpg"),
        description = "Sample post for thumbnail",
        location = PostLocation(52.5200, 13.4050),
        locationStreet = "Musterstraße 123",
        locationCity = "Berlin",
        city = "Berlin",
        likesCount = 42,
        commentsCount = 8,
        createdAt = Date(),
        isLikedByCurrentUser = false,
        isFavoritedByCurrentUser = true
    )
    
    PostThumbnail(
        post = samplePost,
        isPremium = true,
        onClick = { },
        showPremiumOverlay = false,
        modifier = Modifier.size(120.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun PostThumbnailBasicUserPreview() {
    val samplePost = Post(
        id = "sample_post_2",
        ownerId = "user_456",
        ownerDisplayName = "Anna User",
        ownerPhotoUrl = null,
        ownerLevel = 3,
        isOwnerPremium = false,
        ownerPremiumFrameType = PremiumFrameType.NONE,
        images = listOf("https://example.com/image1.jpg"),
        description = "Sample post for Basic user",
        location = PostLocation(52.5200, 13.4050),
        locationStreet = "Basicstraße 456",
        locationCity = "München",
        city = "München",
        likesCount = 15,
        commentsCount = 3,
        createdAt = Date(),
        isLikedByCurrentUser = false,
        isFavoritedByCurrentUser = true
    )
    
    PostThumbnail(
        post = samplePost,
        isPremium = false,
        onClick = { },
        showPremiumOverlay = true,
        modifier = Modifier.size(120.dp)
    )
}
