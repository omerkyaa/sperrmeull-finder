package com.omerkaya.sperrmuellfinder.ui.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.omerkaya.sperrmuellfinder.R
import com.skydoves.landscapist.glide.GlideImage
import androidx.compose.foundation.background
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrokenImage
import com.bumptech.glide.request.RequestOptions
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoPager(
    photos: List<File>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        val pagerState = rememberPagerState(
            initialPage = 0,
            pageCount = { photos.size }
        )

        // Photos
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            GlideImage(
                imageModel = { photos[page] },
                modifier = Modifier.fillMaxSize(),
                loading = {
                    LoadingPlaceholder()
                },
                failure = {
                    ErrorPlaceholder()
                },
                previewPlaceholder = R.drawable.placeholder_image,
                requestOptions = {
                    RequestOptions().centerCrop()
                }
            )
        }

        // Page indicators
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(photos.size) { index ->
                Surface(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape),
                    color = if (pagerState.currentPage == index) {
                        Color.White
                    } else {
                        Color.White.copy(alpha = 0.5f)
                    }
                ) {}
            }
        }
    }
}

@Composable
private fun LoadingPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.LightGray.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.LightGray.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.BrokenImage,
            contentDescription = stringResource(R.string.cd_image_load_error),
            tint = Color.Gray
        )
    }
}
