package com.omerkaya.sperrmuellfinder.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 Shape system for SperrmüllFinder
 */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * Custom shapes for specific components
 */
object CustomShapes {
    // Card shapes
    val cardSmall = RoundedCornerShape(8.dp)
    val cardMedium = RoundedCornerShape(12.dp)
    val cardLarge = RoundedCornerShape(16.dp)
    
    // Button shapes
    val buttonSmall = RoundedCornerShape(20.dp)
    val buttonMedium = RoundedCornerShape(24.dp)
    val buttonLarge = RoundedCornerShape(28.dp)
    
    // Chip shapes
    val chipSmall = RoundedCornerShape(16.dp)
    val chipMedium = RoundedCornerShape(20.dp)
    
    // Bottom sheet shapes
    val bottomSheet = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
    
    // Modal shapes
    val modal = RoundedCornerShape(16.dp)
    
    // Image shapes
    val imageSmall = RoundedCornerShape(8.dp)
    val imageMedium = RoundedCornerShape(12.dp)
    val imageLarge = RoundedCornerShape(16.dp)
    val imageCircle = RoundedCornerShape(50)
    
    // Premium badge shapes
    val premiumBadge = RoundedCornerShape(8.dp)
    val levelBadge = RoundedCornerShape(12.dp)
    
    // Map marker shapes
    val mapMarker = RoundedCornerShape(8.dp)
    val mapCluster = RoundedCornerShape(50)
    
    // Post shapes
    val postCard = RoundedCornerShape(12.dp)
    val postImage = RoundedCornerShape(
        topStart = 12.dp,
        topEnd = 12.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
    
    // Search shapes
    val searchBar = RoundedCornerShape(24.dp)
    val searchFilter = RoundedCornerShape(20.dp)
    
    // Navigation shapes
    val bottomNavigation = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
    
    // Dialog shapes
    val dialog = RoundedCornerShape(16.dp)
    val alert = RoundedCornerShape(12.dp)
    
    // Progress shapes
    val progressBar = RoundedCornerShape(4.dp)
    val xpBar = RoundedCornerShape(8.dp)
    
    // Avatar shapes
    val avatarSmall = RoundedCornerShape(16.dp)
    val avatarMedium = RoundedCornerShape(20.dp)
    val avatarLarge = RoundedCornerShape(24.dp)
    val avatarCircle = RoundedCornerShape(50)
}
