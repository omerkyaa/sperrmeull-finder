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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.domain.model.PremiumFrameType
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import com.omerkaya.sperrmuellfinder.ui.common.SafeGlideImage

/**
 * 🎯 LARGE PROFILE PHOTO COMPONENT - SperrmüllFinder
 * Professional, large profile photo component for ProfileScreen, UserProfileScreen
 * Rules.md compliant - Material3 design with premium frame support
 * 
 * Features:
 * - Large profile photo display (88dp default) with Landscapist-Glide
 * - Premium frame animations (Gold/Diamond) with enhanced effects
 * - Fallback to user initials when no photo
 * - Professional error handling and loading states
 * - Accessibility support
 * - Clickable with proper touch targets
 */
@Composable
fun LargeProfilePhoto(
    photoUrl: String?,
    displayName: String,
    isPremium: Boolean = false,
    premiumFrameType: PremiumFrameType = PremiumFrameType.NONE,
    size: androidx.compose.ui.unit.Dp = 88.dp,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size + if (isPremium && premiumFrameType != PremiumFrameType.NONE) 8.dp else 0.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
    ) {
        // Premium animated frame
        if (isPremium && premiumFrameType != PremiumFrameType.NONE) {
            val infiniteTransition = rememberInfiniteTransition(label = "premium_frame")
            val rotationAngle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )

            val frameColors = when (premiumFrameType) {
                PremiumFrameType.GOLD -> listOf(
                    Color(0xFFFFD700), // Gold
                    Color(0xFFFFA500), // Orange
                    Color(0xFFFFD700), // Gold
                    Color(0xFFFFE55C), // Light Gold
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
                        .size(size + 8.dp)
                        .border(
                            width = 4.dp,
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
                        // Loading placeholder with shimmer effect
                        Box(
                            modifier = Modifier
                                .size(size)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(size * 0.4f)
                            )
                        }
                    },
                    errorPlaceholder = {
                        // Fallback to user initials
                        LargeUserInitialsAvatar(
                            displayName = displayName,
                            size = size,
                            isPremium = isPremium
                        )
                    }
                )
            } else {
                // No photo URL - show initials
                LargeUserInitialsAvatar(
                    displayName = displayName,
                    size = size,
                    isPremium = isPremium
                )
            }
        }
    }
}

/**
 * Large user initials avatar fallback component
 */
@Composable
private fun LargeUserInitialsAvatar(
    displayName: String,
    size: androidx.compose.ui.unit.Dp,
    isPremium: Boolean = false,
    modifier: Modifier = Modifier
) {
    val initials = displayName.let { name ->
        if (name.isBlank()) {
            "?"
        } else {
            name.split(" ")
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .joinToString("")
                .takeIf { it.isNotEmpty() } ?: name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        }
    }
    
    val backgroundColor = if (isPremium) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFD700).copy(alpha = 0.2f), // Gold
                Color(0xFFFFA500).copy(alpha = 0.2f)  // Orange
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )
        )
    }
    
    val textColor = if (isPremium) {
        Color(0xFFFFD700) // Gold text for premium
    } else {
        MaterialTheme.colorScheme.primary
    }
    
    Box(
        modifier = modifier
            .size(size)
            .background(backgroundColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = initials,
            style = when {
                size >= 88.dp -> MaterialTheme.typography.headlineMedium
                size >= 64.dp -> MaterialTheme.typography.headlineSmall
                size >= 48.dp -> MaterialTheme.typography.titleLarge
                else -> MaterialTheme.typography.titleMedium
            },
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}
