package com.omerkaya.sperrmuellfinder.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.omerkaya.sperrmuellfinder.R

/**
 * ❤️ INSTAGRAM-STYLE LIKE BUTTON - SperrmüllFinder
 * Rules.md compliant - Material3 design with heart animation
 * 
 * Features:
 * - Empty heart (♡) when not liked
 * - Red filled heart (❤️) when liked
 * - Smooth scale animation on tap
 * - Haptic feedback
 * - Accessibility support
 */
@Composable
fun LikeButton(
    isLiked: Boolean,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 24.dp,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val scale = remember { Animatable(1f) }
    
    // Debug logging
    LaunchedEffect(isLiked) {
        android.util.Log.d("LikeButton", "💖 Like status changed: $isLiked")
    }
    
    // Animate scale when like status changes
    LaunchedEffect(isLiked) {
        if (isLiked) {
            // Heart fill animation - quick scale up then down
            scale.animateTo(
                targetValue = 1.3f,
                animationSpec = tween(
                    durationMillis = 150,
                    easing = FastOutSlowInEasing
                )
            )
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 150,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }
    
    Box(
        modifier = modifier
            .size(size + 8.dp) // Extra touch target
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // No ripple for Instagram-style
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    // Haptic feedback for better UX
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLikeClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isLiked) {
                Icons.Filled.Favorite // Red filled heart
            } else {
                Icons.Outlined.FavoriteBorder // Empty heart outline
            },
            contentDescription = if (isLiked) {
                stringResource(R.string.unlike_button)
            } else {
                stringResource(R.string.like_button)
            },
            tint = if (isLiked) {
                Color(0xFFFF3040) // Bright Instagram red - more visible
            } else {
                MaterialTheme.colorScheme.onSurface
            }.also {
                android.util.Log.d("LikeButton", "💖 Heart color: isLiked=$isLiked, color=${if (isLiked) "RED" else "GRAY"}")
            },
            modifier = Modifier
                .size(size)
                .scale(scale.value)
        )
    }
}

/**
 * Large like button for PostDetail screen
 */
@Composable
fun LargeLikeButton(
    isLiked: Boolean,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    LikeButton(
        isLiked = isLiked,
        onLikeClick = onLikeClick,
        modifier = modifier,
        size = 24.dp, // Same size as other action icons
        enabled = enabled
    )
}

/**
 * Small like button for PostCard
 */
@Composable
fun SmallLikeButton(
    isLiked: Boolean,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    LikeButton(
        isLiked = isLiked,
        onLikeClick = onLikeClick,
        modifier = modifier,
        size = 24.dp, // Same size as other action icons
        enabled = enabled
    )
}
