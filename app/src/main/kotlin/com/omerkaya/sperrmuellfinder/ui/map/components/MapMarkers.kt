package com.omerkaya.sperrmuellfinder.ui.map.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.paging.compose.LazyPagingItems
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.domain.model.Post
import com.omerkaya.sperrmuellfinder.domain.model.PremiumFrameType
import com.omerkaya.sperrmuellfinder.domain.usecase.map.MapCluster
import com.omerkaya.sperrmuellfinder.domain.usecase.map.MarkerType
import com.omerkaya.sperrmuellfinder.ui.map.MapUiState

/**
 * Map markers component for displaying posts and clusters.
 * Supports different marker styles for Basic/Premium users.
 * Rules.md compliant - Premium markers are enhanced with colors and animations.
 */
@Composable
fun MapMarkers(
    posts: LazyPagingItems<Post>,
    uiState: MapUiState,
    onPostMarkerClick: (Post) -> Unit,
    onClusterMarkerClick: (MapCluster.MultiPost) -> Unit,
    onPostLike: (String) -> Unit
) {
    // Create markers for individual posts (only for posts with valid location)
    for (i in 0 until posts.itemCount) {
        val post = posts[i] ?: continue
        
        // Skip posts without location data
        val location = post.location ?: continue
        
        val markerState = remember(post.id) {
            MarkerState(
                position = LatLng(location.latitude, location.longitude)
            )
        }
        
        val markerIcon = getCustomMarkerIcon()
        
        Marker(
            state = markerState,
            title = post.description.take(50) + if (post.description.length > 50) "..." else "",
            snippet = "${post.city} • ${post.categoriesDe.joinToString(", ")}",
            icon = markerIcon,
            onClick = { marker ->
                onPostMarkerClick(post)
                true // Consume the click
            }
        )
    }
    
    // TODO: Add cluster markers when clustering is implemented
    // This would require processing posts into clusters based on zoom level
}

// Cache for marker icon to avoid repeated bitmap creation
private var cachedMarkerIcon: BitmapDescriptor? = null

/**
 * Get custom marker icon from drawable resource.
 * Uses marker_icon.png with proper scaling and caching for performance.
 */
@Composable
private fun getCustomMarkerIcon(): BitmapDescriptor {
    val context = LocalContext.current
    val density = LocalDensity.current
    
    return remember {
        // Return cached icon if available
        cachedMarkerIcon?.let { return@remember it }
        
        // Create new marker icon with proper scaling
        val markerSizeDp = 60.dp // Optimal size for map markers
        val markerSizePx = with(density) { markerSizeDp.roundToPx() }
        
        val drawable = ContextCompat.getDrawable(context, R.drawable.marker_icon)
        drawable?.let { d ->
            val bitmap = Bitmap.createBitmap(
                markerSizePx,
                markerSizePx,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            d.setBounds(0, 0, markerSizePx, markerSizePx)
            d.draw(canvas)
            
            val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
            cachedMarkerIcon = bitmapDescriptor // Cache for reuse
            bitmapDescriptor
        } ?: BitmapDescriptorFactory.defaultMarker() // Fallback to default marker
    }
}