package com.omerkaya.sperrmuellfinder.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omerkaya.sperrmuellfinder.core.navigation.BottomNavDestination
import com.omerkaya.sperrmuellfinder.core.ui.theme.SfOrange500
import com.omerkaya.sperrmuellfinder.core.ui.theme.SfOrange600
import com.omerkaya.sperrmuellfinder.core.ui.theme.SfOrange200
import com.omerkaya.sperrmuellfinder.core.ui.theme.SfGray500
import androidx.compose.runtime.remember

/**
 * 🎯 SPERRMÜLLFINDER BOTTOM NAVIGATION - HTML REFERENCE DESIGN
 * Exact replica of the provided HTML design:
 * - Floating camera button in center
 * - Active state with primary color
 * - Clean icon-only design with labels
 * - Backdrop blur effect
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavigationBar(
    currentDestination: BottomNavDestination?,
    isPremium: Boolean,
    onDestinationSelected: (BottomNavDestination) -> Unit,
    notificationCount: Int = 0,
    modifier: Modifier = Modifier
) {
    // Safe current destination with fallback
    val safeCurrentDestination = currentDestination ?: BottomNavDestination.Home

    // Get available destinations based on premium status
    val availableDestinations = remember(isPremium) {
        BottomNavDestination.getAvailableDestinations(isPremium)
    }

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // Background with backdrop blur effect (from HTML)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White.copy(alpha = 0.8f), // Card/80 backdrop blur from HTML
            shadowElevation = 0.dp,
            shape = RectangleShape
        ) {
            // Border top (from HTML)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFFAFAFA)) // Background color
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp) // Increased height for better visibility
                    .padding(horizontal = 8.dp, vertical = 8.dp), // Added padding for better spacing
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.Top // Changed to Top for better icon positioning
            ) {
                // Home
                ModernNavItem(
                    destination = BottomNavDestination.Home,
                    isSelected = currentDestination == BottomNavDestination.Home,
                    onClick = { onDestinationSelected(BottomNavDestination.Home) },
                    icon = Icons.Filled.Home,
                    label = "Home"
                )
                
                // Map
                ModernNavItem(
                    destination = BottomNavDestination.Map,
                    isSelected = currentDestination == BottomNavDestination.Map,
                    onClick = { onDestinationSelected(BottomNavDestination.Map) },
                    icon = Icons.Filled.Map,
                    label = "Map"
                )
                
                // Spacer for floating camera button
                Spacer(modifier = Modifier.width(64.dp))
                
                // Alerts (Search replacement for now)
                ModernNavItem(
                    destination = BottomNavDestination.Search,
                    isSelected = currentDestination == BottomNavDestination.Search,
                    onClick = { onDestinationSelected(BottomNavDestination.Search) },
                    icon = Icons.Filled.Search,
                    label = "Alerts"
                )
                
                // Profile
                ModernNavItem(
                    destination = BottomNavDestination.Profile,
                    isSelected = currentDestination == BottomNavDestination.Profile,
                    onClick = { onDestinationSelected(BottomNavDestination.Profile) },
                    icon = Icons.Filled.Person,
                    label = "Profile",
                    notificationCount = notificationCount
                )
            }
        }
        
        // 🎯 FLOATING CAMERA BUTTON - Exact HTML replica
        FloatingCameraButton(
            onClick = { onDestinationSelected(BottomNavDestination.Camera) },
            isSelected = currentDestination == BottomNavDestination.Camera,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-28).dp) // Adjusted for better positioning with increased height
        )
    }
}

/**
 * 🎯 MODERN NAV ITEM - HTML Reference Design
 * Exact replica of HTML navigation items
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernNavItem(
    destination: BottomNavDestination,
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    notificationCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top, // Changed to Top for better positioning
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // Use Material3 default ripple
            ) { onClick() }
            .padding(horizontal = 4.dp, vertical = 4.dp) // Reduced padding for better fit
) {
    BadgedBox(
        badge = {
            // Notification badge for Profile
            if (destination == BottomNavDestination.Profile && notificationCount > 0) {
                Badge(
                    containerColor = Color(0xFFFF4444),
                    contentColor = Color.White,
                        modifier = Modifier.offset(x = 8.dp, y = (-8).dp)
                ) {
                    Text(
                        text = if (notificationCount > 99) "99+" else notificationCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp
                    )
                }
            }
            }
        ) {
                    Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) SfOrange500 else SfGray500, // Turuncu seçili durum teması – SperrmüllFinder
                modifier = Modifier.size(28.dp) // Increased icon size for better visibility
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
            fontWeight = FontWeight.Bold
            ),
            color = if (isSelected) SfOrange500 else SfGray500, // Turuncu seçili durum teması – SperrmüllFinder
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 🎯 FLOATING CAMERA BUTTON - SperrmüllFinder Professional Design
 * Always orange with white icon, enhanced with subtle glow effect.
 * Provides consistent visual hierarchy as the primary action button.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
private fun FloatingCameraButton(
    onClick: () -> Unit,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(64.dp) // w-16 h-16 from HTML
            .shadow(
                elevation = 12.dp, // Daha belirgin gölge
                shape = CircleShape,
                ambientColor = SfOrange500.copy(alpha = 0.3f), // Turuncu ambient glow
                spotColor = SfOrange500.copy(alpha = 0.5f) // Turuncu spot glow
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // Use Material3 default ripple
            ) { onClick() },
        color = SfOrange500, // Her zaman turuncu – SperrmüllFinder kamera butonu
        shape = CircleShape, // rounded-full from HTML
        shadowElevation = 0.dp // Shadow modifier'da halledildi
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = Icons.Filled.Camera, // Using camera instead of add_a_photo
                contentDescription = "Camera",
                tint = Color.White, // Her zaman beyaz ikon – SperrmüllFinder kamera butonu
                modifier = Modifier.size(32.dp) // text-3xl equivalent
            )
        }
    }
}