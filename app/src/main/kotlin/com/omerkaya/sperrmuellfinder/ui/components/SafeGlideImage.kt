package com.omerkaya.sperrmuellfinder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.omerkaya.sperrmuellfinder.core.util.ContentUriCleaner
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage

/**
 * 🖼️ SAFE GLIDE IMAGE COMPONENT - SperrmüllFinder
 * Rules.md compliant - Landscapist-Glide with professional error handling
 */
@Composable
fun SafeGlideImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    contentScale: ContentScale = ContentScale.Crop,
    placeholderIcon: ImageVector = Icons.Default.Person,
    placeholderColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val safeImageUrl = ContentUriCleaner.getSafeImageUrl(imageUrl)

    if (safeImageUrl.isNullOrBlank()) {
        // Show placeholder when no image URL
        Box(
            modifier = modifier
                .background(placeholderColor, shape)
                .clip(shape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = placeholderIcon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    } else {
        GlideImage(
            imageModel = { safeImageUrl },
            imageOptions = ImageOptions(
                contentScale = contentScale,
                alignment = Alignment.Center
            ),
            modifier = modifier.clip(shape),
            loading = {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(placeholderColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = placeholderIcon,
                        contentDescription = contentDescription,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            failure = {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(placeholderColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = placeholderIcon,
                        contentDescription = contentDescription,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        )
    }
}

/**
 * Circular profile image variant
 */
@Composable
fun SafeCircularProfileImage(
    imageUrl: String?,
    contentDescription: String?,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    SafeGlideImage(
        imageUrl = imageUrl,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        shape = CircleShape,
        contentScale = ContentScale.Crop,
        placeholderIcon = Icons.Default.Person,
        placeholderColor = MaterialTheme.colorScheme.surfaceVariant
    )
}