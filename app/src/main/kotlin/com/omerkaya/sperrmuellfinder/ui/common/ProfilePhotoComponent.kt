package com.omerkaya.sperrmuellfinder.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.core.ui.theme.SperrmullPrimary
import com.omerkaya.sperrmuellfinder.domain.model.PremiumFrameType
import com.skydoves.landscapist.ImageOptions
import com.omerkaya.sperrmuellfinder.ui.common.SafeGlideImage

/**
 * 🎯 REUSABLE PROFILE PHOTO COMPONENT
 * Mükemmel profil resmi yükleme sistemi - tüm ekranlarda tutarlı kullanım
 * 
 * Features:
 * - Optimized Glide image loading
 * - Enhanced loading states with proper styling
 * - Premium frame animations
 * - Fallback avatars with Material3 theming
 * - Accessibility support
 * - Performance optimized
 */
@Composable
fun ProfilePhotoComponent(
    photoUrl: String?,
    displayName: String = "",
    size: Dp = 40.dp,
    isPremium: Boolean = false,
    premiumFrameType: PremiumFrameType = PremiumFrameType.NONE,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Premium animated frame
        if (isPremium && premiumFrameType != PremiumFrameType.NONE) {
            PremiumFrameAnimation(
                frameType = premiumFrameType,
                size = size + 8.dp
            )
        }

        // Profile photo container
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (!photoUrl.isNullOrBlank()) {
                SafeGlideImage(
                    imageUrl = photoUrl,
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    contentDescription = stringResource(
                        R.string.cd_user_avatar_with_name,
                        displayName.ifBlank { "User" }
                    )
                )
            } else {
                ProfilePhotoFallback(
                    displayName = displayName,
                    size = size
                )
            }
        }
    }
}

@Composable
private fun PremiumFrameAnimation(
    frameType: PremiumFrameType,
    size: Dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "premium_frame")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val frameColors = when (frameType) {
        PremiumFrameType.GOLD -> listOf(
            Color(0xFFFFD700), // Gold
            Color(0xFFFFA500), // Orange
            Color(0xFFFFD700), // Gold
            Color(0xFFFF8C00), // Dark Orange
            Color(0xFFFFD700)  // Gold (complete circle)
        )
        PremiumFrameType.DIAMOND -> listOf(
            Color(0xFFE91E63), // Pink
            Color(0xFF9C27B0), // Purple
            Color(0xFF3F51B5), // Indigo
            Color(0xFF2196F3), // Blue
            Color(0xFF00BCD4), // Cyan
            Color(0xFF4CAF50), // Green
            Color(0xFFFFEB3B), // Yellow
            Color(0xFFFF9800), // Orange
            Color(0xFFE91E63)  // Pink (complete circle)
        )
        else -> listOf(Color.Transparent)
    }

    if (frameColors.first() != Color.Transparent) {
        Box(
            modifier = Modifier
                .size(size)
                .border(
                    width = 3.dp,
                    brush = Brush.sweepGradient(
                        colors = frameColors
                    ),
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun ProfilePhotoLoading(size: Dp) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(size * 0.4f),
            strokeWidth = 2.dp,
            color = SperrmullPrimary
        )
    }
}

@Composable
private fun ProfilePhotoFallback(
    displayName: String,
    size: Dp
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.primaryContainer,
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (displayName.isNotBlank()) {
            // Show first letter of display name
            androidx.compose.material3.Text(
                text = displayName.first().uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        } else {
            // Show person icon
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = stringResource(R.string.cd_user_avatar),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}

/**
 * Compact version for small profile photos (like in PostCard)
 */
@Composable
fun CompactProfilePhoto(
    photoUrl: String?,
    displayName: String = "",
    premiumFrameType: PremiumFrameType = PremiumFrameType.NONE,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    ProfilePhotoComponent(
        photoUrl = photoUrl,
        displayName = displayName,
        size = 40.dp,
        isPremium = premiumFrameType != PremiumFrameType.NONE,
        premiumFrameType = premiumFrameType,
        onClick = onClick,
        modifier = modifier
    )
}

/**
 * Large version for profile screens
 */
@Composable
fun LargeProfilePhoto(
    photoUrl: String?,
    displayName: String = "",
    isPremium: Boolean = false,
    premiumFrameType: PremiumFrameType = PremiumFrameType.NONE,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    ProfilePhotoComponent(
        photoUrl = photoUrl,
        displayName = displayName,
        size = 88.dp,
        isPremium = isPremium,
        premiumFrameType = premiumFrameType,
        onClick = onClick,
        modifier = modifier
    )
}
