package com.omerkaya.sperrmuellfinder.ui.profile.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.omerkaya.sperrmuellfinder.domain.model.PremiumFrameType

/**
 * Premium badge component with animated frames
 * Implements rules.md specifications for premium frame types
 */
@Composable
fun PremiumBadge(
    frameType: PremiumFrameType,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    when (frameType) {
        PremiumFrameType.NONE -> {
            // No premium badge
        }
        PremiumFrameType.BRONZE -> {
            BronzePremiumBadge(
                size = size,
                modifier = modifier
            )
        }
        PremiumFrameType.SILVER -> {
            SilverPremiumBadge(
                size = size,
                modifier = modifier
            )
        }
        PremiumFrameType.GOLD -> {
            GoldPremiumBadge(
                size = size,
                modifier = modifier
            )
        }
        PremiumFrameType.PLATINUM -> {
            PlatinumPremiumBadge(
                size = size,
                modifier = modifier
            )
        }
        PremiumFrameType.DIAMOND -> {
            CrystalPremiumBadge(
                size = size,
                modifier = modifier
            )
        }
        PremiumFrameType.RAINBOW -> {
            RainbowPremiumBadge(
                size = size,
                modifier = modifier
            )
        }
        PremiumFrameType.FIRE -> {
            FirePremiumBadge(
                size = size,
                modifier = modifier
            )
        }
        PremiumFrameType.ICE -> {
            IcePremiumBadge(
                size = size,
                modifier = modifier
            )
        }
        PremiumFrameType.NATURE -> {
            NaturePremiumBadge(
                size = size,
                modifier = modifier
            )
        }
        PremiumFrameType.URBAN -> {
            UrbanPremiumBadge(
                size = size,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun GoldPremiumBadge(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD700), // Gold
                        Color(0xFFB8860B)  // Dark gold
                    )
                ),
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = Color(0xFFB8860B),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Premium Gold",
            modifier = Modifier.size(size * 0.6f),
            tint = Color.White
        )
    }
}

@Composable
private fun CrystalPremiumBadge(
    size: Dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Crystal Animation")
    
    // Rotation animation for crystal effect
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Crystal Rotation"
    )
    
    // Shimmer animation
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Crystal Shimmer"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Animated crystal background with Lottie-like effect
        Canvas(
            modifier = Modifier
                .size(size)
                .rotate(rotation)
        ) {
            val center = this.center
            val radius = size.toPx() / 2f
            
            // Draw crystal facets with shimmer effect
            rotate(degrees = rotation) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE6E6FA).copy(alpha = 0.3f + shimmer * 0.7f), // Lavender
                            Color(0xFF9370DB).copy(alpha = 0.5f + shimmer * 0.5f), // Medium purple
                            Color(0xFF4B0082).copy(alpha = 0.8f)  // Indigo
                        ),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
                
                // Draw crystal outline
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFFE6E6FA),
                            Color(0xFF9370DB),
                            Color(0xFF4B0082),
                            Color(0xFFE6E6FA)
                        ),
                        center = center
                    ),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
        
        // Star icon in the center
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Premium Crystal",
            modifier = Modifier.size(size * 0.6f),
            tint = Color.White
        )
    }
}

/**
 * Premium frame for profile pictures
 * Wraps around profile image with animated premium effects
 */
@Composable
fun PremiumFrame(
    frameType: PremiumFrameType,
    size: Dp = 120.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        when (frameType) {
            PremiumFrameType.NONE -> {
                content()
            }
            PremiumFrameType.BRONZE -> {
                BronzeFrame(size = size)
                content()
            }
            PremiumFrameType.SILVER -> {
                SilverFrame(size = size)
                content()
            }
            PremiumFrameType.GOLD -> {
                GoldFrame(size = size)
                content()
            }
            PremiumFrameType.PLATINUM -> {
                PlatinumFrame(size = size)
                content()
            }
            PremiumFrameType.DIAMOND -> {
                CrystalFrame(size = size)
                content()
            }
            PremiumFrameType.RAINBOW -> {
                RainbowFrame(size = size)
                content()
            }
            PremiumFrameType.FIRE -> {
                FireFrame(size = size)
                content()
            }
            PremiumFrameType.ICE -> {
                IceFrame(size = size)
                content()
            }
            PremiumFrameType.NATURE -> {
                NatureFrame(size = size)
                content()
            }
            PremiumFrameType.URBAN -> {
                UrbanFrame(size = size)
                content()
            }
        }
    }
}

@Composable
private fun GoldFrame(size: Dp) {
    Canvas(
        modifier = Modifier.size(size)
    ) {
        val center = this.center
        val radius = size.toPx() / 2f
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFD700).copy(alpha = 0.3f),
                    Color(0xFFB8860B).copy(alpha = 0.6f),
                    Color(0xFF8B7355).copy(alpha = 0.8f)
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
        
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFFFFD700),
                    Color(0xFFB8860B),
                    Color(0xFFFFD700)
                ),
                center = center
            ),
            radius = radius,
            center = center,
            style = Stroke(width = 4.dp.toPx())
        )
    }
}

@Composable
private fun BronzePremiumBadge(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFCD7F32), // Bronze
                        Color(0xFF8B4513)  // Dark bronze
                    )
                ),
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = Color(0xFF8B4513),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Premium Bronze",
            modifier = Modifier.size(size * 0.6f),
            tint = Color.White
        )
    }
}

@Composable
private fun SilverPremiumBadge(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFC0C0C0), // Silver
                        Color(0xFF808080)  // Dark silver
                    )
                ),
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = Color(0xFF808080),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Premium Silver",
            modifier = Modifier.size(size * 0.6f),
            tint = Color.White
        )
    }
}

@Composable
private fun PlatinumPremiumBadge(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE5E4E2), // Platinum
                        Color(0xFFA0A0A0)  // Dark platinum
                    )
                ),
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = Color(0xFFA0A0A0),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Premium Platinum",
            modifier = Modifier.size(size * 0.6f),
            tint = Color.White
        )
    }
}

@Composable
private fun RainbowPremiumBadge(
    size: Dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Rainbow Animation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rainbow Rotation"
    )

    Box(
        modifier = modifier
            .size(size)
            .background(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFFF0000), // Red
                        Color(0xFFFF7F00), // Orange
                        Color(0xFFFFFF00), // Yellow
                        Color(0xFF00FF00), // Green
                        Color(0xFF0000FF), // Blue
                        Color(0xFF4B0082), // Indigo
                        Color(0xFF8B00FF), // Violet
                        Color(0xFFFF0000)  // Red (to complete the circle)
                    )
                ),
                shape = CircleShape
            )
            .rotate(rotation),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Premium Rainbow",
            modifier = Modifier.size(size * 0.6f),
            tint = Color.White
        )
    }
}

@Composable
private fun FirePremiumBadge(
    size: Dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Fire Animation")
    val flicker by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Fire Flicker"
    )

    Box(
        modifier = modifier
            .size(size)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFF4500).copy(alpha = 0.8f + flicker * 0.2f), // Orange Red
                        Color(0xFFFF0000).copy(alpha = 0.9f)  // Red
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Premium Fire",
            modifier = Modifier.size(size * 0.6f),
            tint = Color.White
        )
    }
}

@Composable
private fun IcePremiumBadge(
    size: Dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Ice Animation")
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Ice Shimmer"
    )

    Box(
        modifier = modifier
            .size(size)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE0FFFF).copy(alpha = 0.7f + shimmer * 0.3f), // Light Cyan
                        Color(0xFF87CEEB).copy(alpha = 0.8f)  // Sky Blue
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Premium Ice",
            modifier = Modifier.size(size * 0.6f),
            tint = Color.White
        )
    }
}

@Composable
private fun NaturePremiumBadge(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF90EE90), // Light Green
                        Color(0xFF228B22)  // Forest Green
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Premium Nature",
            modifier = Modifier.size(size * 0.6f),
            tint = Color.White
        )
    }
}

@Composable
private fun UrbanPremiumBadge(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF808080), // Gray
                        Color(0xFF404040)  // Dark Gray
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Premium Urban",
            modifier = Modifier.size(size * 0.6f),
            tint = Color.White
        )
    }
}

@Composable
private fun BronzeFrame(size: Dp) {
    Canvas(
        modifier = Modifier.size(size)
    ) {
        val center = this.center
        val radius = size.toPx() / 2f
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFCD7F32).copy(alpha = 0.3f),
                    Color(0xFF8B4513).copy(alpha = 0.6f),
                    Color(0xFF654321).copy(alpha = 0.8f)
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
        
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFFCD7F32),
                    Color(0xFF8B4513),
                    Color(0xFFCD7F32)
                ),
                center = center
            ),
            radius = radius,
            center = center,
            style = Stroke(width = 4.dp.toPx())
        )
    }
}

@Composable
private fun SilverFrame(size: Dp) {
    Canvas(
        modifier = Modifier.size(size)
    ) {
        val center = this.center
        val radius = size.toPx() / 2f
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFC0C0C0).copy(alpha = 0.3f),
                    Color(0xFF808080).copy(alpha = 0.6f),
                    Color(0xFF696969).copy(alpha = 0.8f)
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
        
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFFC0C0C0),
                    Color(0xFF808080),
                    Color(0xFFC0C0C0)
                ),
                center = center
            ),
            radius = radius,
            center = center,
            style = Stroke(width = 4.dp.toPx())
        )
    }
}

@Composable
private fun PlatinumFrame(size: Dp) {
    Canvas(
        modifier = Modifier.size(size)
    ) {
        val center = this.center
        val radius = size.toPx() / 2f
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFE5E4E2).copy(alpha = 0.3f),
                    Color(0xFFA0A0A0).copy(alpha = 0.6f),
                    Color(0xFF808080).copy(alpha = 0.8f)
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
        
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFFE5E4E2),
                    Color(0xFFA0A0A0),
                    Color(0xFFE5E4E2)
                ),
                center = center
            ),
            radius = radius,
            center = center,
            style = Stroke(width = 4.dp.toPx())
        )
    }
}

@Composable
private fun RainbowFrame(size: Dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "Rainbow Frame Animation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rainbow Frame Rotation"
    )

    Canvas(
        modifier = Modifier
            .size(size)
            .rotate(rotation)
    ) {
        val center = this.center
        val radius = size.toPx() / 2f
        
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFFFF0000),
                    Color(0xFFFF7F00),
                    Color(0xFFFFFF00),
                    Color(0xFF00FF00),
                    Color(0xFF0000FF),
                    Color(0xFF4B0082),
                    Color(0xFF8B00FF),
                    Color(0xFFFF0000)
                ),
                center = center
            ),
            radius = radius,
            center = center,
            style = Stroke(width = 5.dp.toPx())
        )
    }
}

@Composable
private fun FireFrame(size: Dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "Fire Frame Animation")
    val flicker by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Fire Frame Flicker"
    )

    Canvas(
        modifier = Modifier.size(size)
    ) {
        val center = this.center
        val radius = size.toPx() / 2f
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFF4500).copy(alpha = 0.3f + flicker * 0.2f),
                    Color(0xFFFF0000).copy(alpha = 0.6f),
                    Color(0xFF8B0000).copy(alpha = 0.8f)
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
        
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFFFF4500).copy(alpha = 0.8f + flicker * 0.2f),
                    Color(0xFFFF0000),
                    Color(0xFFFF4500).copy(alpha = 0.8f + flicker * 0.2f)
                ),
                center = center
            ),
            radius = radius,
            center = center,
            style = Stroke(width = 4.dp.toPx())
        )
    }
}

@Composable
private fun IceFrame(size: Dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "Ice Frame Animation")
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Ice Frame Shimmer"
    )

    Canvas(
        modifier = Modifier.size(size)
    ) {
        val center = this.center
        val radius = size.toPx() / 2f
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFE0FFFF).copy(alpha = 0.3f + shimmer * 0.2f),
                    Color(0xFF87CEEB).copy(alpha = 0.6f),
                    Color(0xFF4682B4).copy(alpha = 0.8f)
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
        
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFFE0FFFF).copy(alpha = 0.8f + shimmer * 0.2f),
                    Color(0xFF87CEEB),
                    Color(0xFFE0FFFF).copy(alpha = 0.8f + shimmer * 0.2f)
                ),
                center = center
            ),
            radius = radius,
            center = center,
            style = Stroke(width = 4.dp.toPx())
        )
    }
}

@Composable
private fun NatureFrame(size: Dp) {
    Canvas(
        modifier = Modifier.size(size)
    ) {
        val center = this.center
        val radius = size.toPx() / 2f
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF90EE90).copy(alpha = 0.3f),
                    Color(0xFF228B22).copy(alpha = 0.6f),
                    Color(0xFF006400).copy(alpha = 0.8f)
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
        
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFF90EE90),
                    Color(0xFF228B22),
                    Color(0xFF90EE90)
                ),
                center = center
            ),
            radius = radius,
            center = center,
            style = Stroke(width = 4.dp.toPx())
        )
    }
}

@Composable
private fun UrbanFrame(size: Dp) {
    Canvas(
        modifier = Modifier.size(size)
    ) {
        val center = this.center
        val radius = size.toPx() / 2f
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF808080).copy(alpha = 0.3f),
                    Color(0xFF404040).copy(alpha = 0.6f),
                    Color(0xFF202020).copy(alpha = 0.8f)
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
        
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFF808080),
                    Color(0xFF404040),
                    Color(0xFF808080)
                ),
                center = center
            ),
            radius = radius,
            center = center,
            style = Stroke(width = 4.dp.toPx())
        )
    }
}

@Composable
private fun CrystalFrame(size: Dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "Crystal Frame Animation")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Crystal Frame Rotation"
    )
    
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Crystal Frame Shimmer"
    )

    Canvas(
        modifier = Modifier
            .size(size)
            .rotate(rotation * 0.5f) // Slower rotation for frame
    ) {
        val center = this.center
        val radius = size.toPx() / 2f
        
        // Animated crystal background
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFE6E6FA).copy(alpha = 0.2f + shimmer * 0.3f),
                    Color(0xFF9370DB).copy(alpha = 0.4f + shimmer * 0.4f),
                    Color(0xFF4B0082).copy(alpha = 0.6f + shimmer * 0.2f)
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
        
        // Rotating crystal outline
        rotate(degrees = rotation) {
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFE6E6FA).copy(alpha = 0.8f + shimmer * 0.2f),
                        Color(0xFF9370DB).copy(alpha = 0.9f + shimmer * 0.1f),
                        Color(0xFF4B0082),
                        Color(0xFF6A0DAD),
                        Color(0xFFE6E6FA).copy(alpha = 0.8f + shimmer * 0.2f)
                    ),
                    center = center
                ),
                radius = radius,
                center = center,
                style = Stroke(width = 5.dp.toPx())
            )
        }
        
        // Inner glow effect
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF9370DB).copy(alpha = 0.1f + shimmer * 0.2f)
                ),
                center = center,
                radius = radius * 0.8f
            ),
            radius = radius * 0.8f,
            center = center
        )
    }
}
