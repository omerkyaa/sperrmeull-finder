package com.omerkaya.sperrmuellfinder.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
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
import android.util.Log
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.core.util.ContentUriCleaner
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage

/**
 * Safe GlideImage wrapper that prevents SecurityException from content:// URIs
 * 
 * This component:
 * 1. Checks if the imageModel is a content:// URI
 * 2. If yes, shows placeholder instead of crashing
 * 3. If no, loads the image normally with Firebase Storage URLs
 * 
 * SECURITY FIX: Prevents "Calling uid does not have permission to access picker uri" crashes
 */
@Composable
fun SafeGlideImage(
    imageModel: Any? = null,
    imageUrl: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape? = null,
    placeholderIcon: ImageVector = Icons.Default.Person,
    placeholderColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentDescription: String? = null,
    loadingPlaceholder: (@Composable () -> Unit)? = null,
    errorPlaceholder: (@Composable () -> Unit)? = null
) {
    // Get safe image URL using ContentUriCleaner
    val actualImageModel = imageUrl ?: imageModel
    val safeImageUrl = when (actualImageModel) {
        is String -> {
            Log.d("SafeGlideImage", "🖼️ Loading image URL: ${actualImageModel.take(100)}")
            ContentUriCleaner.getSafeImageUrl(actualImageModel)
        }
        is android.net.Uri -> {
            Log.d("SafeGlideImage", "🖼️ Loading image URI: ${actualImageModel.toString().take(100)}")
            ContentUriCleaner.getSafeImageUrl(actualImageModel.toString())
        }
        else -> {
            Log.d("SafeGlideImage", "🖼️ Loading image model: $actualImageModel")
            actualImageModel
        }
    }
    
    if (safeImageUrl == null && actualImageModel != null) {
        // Show placeholder for problematic URIs to prevent SecurityException
        Log.w("SafeGlideImage", "🚫 Showing placeholder for problematic URI: ${actualImageModel.toString().take(50)}...")
        errorPlaceholder?.invoke() ?: Box(
            modifier = modifier
                .background(
                    color = placeholderColor,
                    shape = shape ?: CircleShape
                )
                .let { if (shape != null) it.clip(shape) else it },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = placeholderIcon,
                contentDescription = contentDescription ?: "Profile photo placeholder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxSize(0.6f)
            )
        }
    } else {
        // Safe to load - use Firebase Storage URLs or other safe URIs
        GlideImage(
            imageModel = { safeImageUrl },
            imageOptions = ImageOptions(
                contentScale = contentScale,
                contentDescription = contentDescription
            ),
            modifier = modifier.let { if (shape != null) it.clip(shape) else it },
            failure = {
                // Fallback on load failure
                errorPlaceholder?.invoke() ?: Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = placeholderColor,
                            shape = shape ?: CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = placeholderIcon,
                        contentDescription = contentDescription ?: "Image failed to load",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxSize(0.6f)
                    )
                }
            },
            loading = {
                // Loading placeholder
                loadingPlaceholder?.invoke() ?: Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = placeholderColor.copy(alpha = 0.3f),
                            shape = shape ?: CircleShape
                        )
                )
            }
        )
    }
}
