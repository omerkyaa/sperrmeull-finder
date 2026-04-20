package com.omerkaya.sperrmuellfinder.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.omerkaya.sperrmuellfinder.R
import kotlinx.coroutines.delay

/**
 * Well Done celebration screen shown after successful registration
 * Features beautiful confetti animation and modern design
 */
@Composable
fun WellDoneScreen(
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAnimation by remember { mutableStateOf(true) }
    
    // Handle timeout
    LaunchedEffect(Unit) {
        delay(5000L) // 5 seconds
        showAnimation = false
        delay(300L) // Animation duration
        onTimeout()
    }
    
    // Load Lottie animation
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.confetti)
    )
    
    AnimatedVisibility(
        visible = showAnimation,
        enter = fadeIn(animationSpec = tween(300, easing = LinearEasing)),
        exit = fadeOut(animationSpec = tween(300, easing = LinearEasing))
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                // Confetti animation
                LottieAnimation(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.size(200.dp)
                )
                
                // Welcome text
                Text(
                    text = stringResource(R.string.well_done_welcome),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 24.dp)
                )

                // Bonus info
                Text(
                    text = stringResource(R.string.well_done_bonus_title),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 18.sp
                    ),
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp)
                )

                // XP bonus
                Text(
                    text = stringResource(R.string.well_done_xp_bonus),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        letterSpacing = 1.sp
                    ),
                    color = Color(0xFFFFD700), // Gold color
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Honesty bonus
                Text(
                    text = stringResource(R.string.well_done_honesty_bonus),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        letterSpacing = 1.sp
                    ),
                    color = Color(0xFF00FF00), // Green color
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Earned text
                Text(
                    text = stringResource(R.string.well_done_bonus_earned),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 18.sp
                    ),
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Preparing text
                Text(
                    text = stringResource(R.string.well_done_preparing),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        }
    }
}