package com.omerkaya.sperrmuellfinder.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.core.ui.theme.SperrmullPrimary
import kotlinx.coroutines.launch

/**
 * Onboarding flow with 4 pages showing app features and permissions.
 * 
 * According to rules.md:
 * - Modern Material 3 design
 * - Feature introduction with visuals
 * - Permission explanations
 * - Smooth page transitions
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    val onboardingPages = listOf(
        OnboardingPage(
            title = stringResource(R.string.onboarding_welcome_title),
            description = stringResource(R.string.onboarding_welcome_description),
            imageRes = R.drawable.onboarding_welcome,
            backgroundColor = SperrmullPrimary
        ),
        OnboardingPage(
            title = stringResource(R.string.onboarding_discover_title),
            description = stringResource(R.string.onboarding_discover_description),
            imageRes = R.drawable.onboarding_discover,
            backgroundColor = Color(0xFF4CAF50)
        ),
        OnboardingPage(
            title = stringResource(R.string.onboarding_share_title),
            description = stringResource(R.string.onboarding_share_description),
            imageRes = R.drawable.onboarding_share,
            backgroundColor = Color(0xFF2196F3)
        ),
        OnboardingPage(
            title = stringResource(R.string.onboarding_permissions_title),
            description = stringResource(R.string.onboarding_permissions_description),
            imageRes = R.drawable.onboarding_permissions,
            backgroundColor = Color(0xFF9C27B0)
        )
    )

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            OnboardingPageContent(
                page = onboardingPages[page],
                modifier = Modifier.fillMaxSize()
            )
        }

        // Skip button
        AnimatedVisibility(
            visible = pagerState.currentPage < onboardingPages.size - 1,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            TextButton(
                onClick = onSkip,
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = stringResource(R.string.skip),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Bottom section with indicators and navigation
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(onboardingPages.size) { index ->
                    PageIndicator(
                        isSelected = index == pagerState.currentPage,
                        modifier = Modifier.size(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Navigation buttons
            if (pagerState.currentPage == onboardingPages.size - 1) {
                // Get Started button on last page
                Button(
                    onClick = onOnboardingComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = onboardingPages[pagerState.currentPage].backgroundColor
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.get_started),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                // Next button for other pages
                Button(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.next),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        page.backgroundColor,
                        page.backgroundColor.copy(alpha = 0.8f),
                        Color.Black.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Illustration
            Image(
                painter = painterResource(id = page.imageRes),
                contentDescription = null,
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(16.dp))
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Title
            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp
                ),
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
private fun PageIndicator(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.3f),
                shape = CircleShape
            )
    )
}

/**
 * Data class representing an onboarding page
 */
data class OnboardingPage(
    val title: String,
    val description: String,
    val imageRes: Int,
    val backgroundColor: Color
)
