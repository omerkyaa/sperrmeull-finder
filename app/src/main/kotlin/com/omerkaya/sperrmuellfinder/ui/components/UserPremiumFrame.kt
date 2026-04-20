package com.omerkaya.sperrmuellfinder.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 👑 USER PREMIUM FRAME - SperrmüllFinder
 * Rules.md compliant - Premium frame component
 */
@Composable
fun UserPremiumFrame(
    isPremium: Boolean,
    level: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        if (isPremium) {
            val frameColor = when {
                level >= 15 -> Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFD700), // Gold
                        Color(0xFFFFA500), // Orange
                        Color(0xFFFFD700)  // Gold
                    )
                )
                else -> Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            }
            
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .border(2.dp, frameColor, CircleShape)
                    .padding(2.dp)
            ) {
                content()
            }
        } else {
            content()
        }
    }
}
