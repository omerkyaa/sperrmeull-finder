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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.unit.dp
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.domain.model.PremiumFrameType
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import com.omerkaya.sperrmuellfinder.ui.common.SafeGlideImage

/**
 * 🎯 COMPACT PROFILE PHOTO COMPONENT - SperrmüllFinder
 * Professional, reusable profile photo component for PostCard, Comments, etc.
 * Rules.md compliant - Material3 design with premium frame support
 * 
 * Features:
 * - Real-time profile photo display with Landscapist-Glide
 * - Premium frame animations (Gold/Diamond)
 * - Fallback to user initials when no photo
 * - Professional error handling
 * - Accessibility support
 * - Clickable with proper touch targets
 */
@Composable
fun CompactProfilePhoto(
    photoUrl: String?,
    displayName: String,
    premiumFrameType: PremiumFrameType = PremiumFrameType.NONE,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
    ) {
        // Premium animated frame
        if (premiumFrameType != PremiumFrameType.NONE) {
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

            val frameColors = when (premiumFrameType) {
                PremiumFrameType.GOLD -> listOf(
                    Color(0xFFFFD700), // Gold
                    Color(0xFFFFA500), // Orange
                    Color(0xFFFFD700)  // Gold
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
                else -> emptyList()
            }

            if (frameColors.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(size + 4.dp)
                        .border(
                            width = 2.dp,
                            brush = Brush.sweepGradient(colors = frameColors),
                            shape = CircleShape
                        )
                )
            }
        }

        // Profile photo
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
                    modifier = Modifier.size(size),
                    contentScale = ContentScale.Crop,
                    contentDescription = stringResource(R.string.cd_user_avatar),
                    loadingPlaceholder = {
                        // Loading placeholder
                        Box(
                            modifier = Modifier
                                .size(size)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(size * 0.5f)
                            )
                        }
                    },
                    errorPlaceholder = {
                        // Fallback to user initials
                        UserInitialsAvatar(
                            displayName = displayName,
                            size = size
                        )
                    }
                )
            } else {
                // No photo URL - show initials
                UserInitialsAvatar(
                    displayName = displayName,
                    size = size
                )
            }
        }
    }
}

/**
 * User initials avatar fallback component
 */
@Composable
private fun UserInitialsAvatar(
    displayName: String,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val initials = displayName.let { name ->
        if (name.isBlank()) {
            "U" // User fallback instead of "?"
        } else {
            name.split(" ")
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .joinToString("")
                .takeIf { it.isNotEmpty() } ?: name.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
        }
    }
    
    Box(
        modifier = modifier
            .size(size)
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = initials,
            style = when {
                size >= 56.dp -> MaterialTheme.typography.titleLarge
                size >= 40.dp -> MaterialTheme.typography.titleMedium
                else -> MaterialTheme.typography.titleSmall
            },
            color = MaterialTheme.colorScheme.primary
        )
    }
}
