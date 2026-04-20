package com.omerkaya.sperrmuellfinder.ui.postdetail.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import com.omerkaya.sperrmuellfinder.R

/**
 * Image carousel component for displaying multiple post images.
 * Instagram-style image viewer with page indicators and pinch-to-zoom support.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageCarousel(
    images: List<String>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (images.isEmpty()) return
    
    val pagerState = rememberPagerState(
        initialPage = currentIndex,
        pageCount = { images.size }
    )
    
    // Sync pager state with external index
    LaunchedEffect(currentIndex) {
        if (pagerState.currentPage != currentIndex) {
            pagerState.animateScrollToPage(currentIndex)
        }
    }
    
    // Notify external state of page changes
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != currentIndex) {
            onIndexChange(pagerState.currentPage)
        }
    }
    
    Box(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            GlideImage(
                imageModel = { images[page] },
                imageOptions = ImageOptions(
                    contentScale = ContentScale.Crop,
                    contentDescription = stringResource(R.string.cd_post_image)
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(0.dp))
            )
        }
        
        // Page indicators
        if (images.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(images.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (index == pagerState.currentPage) {
                                    Color.White
                                } else {
                                    Color.White.copy(alpha = 0.5f)
                                },
                                shape = CircleShape
                            )
                    )
                }
            }
        }
        
        // Image counter
        if (images.size > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(
                        Color.Black.copy(alpha = 0.6f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1}/${images.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
