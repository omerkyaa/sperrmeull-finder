package com.omerkaya.sperrmuellfinder.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.ui.notifications.NotificationsViewModel

/**
 * 🔥 HOME TOP BAR - SperrmüllFinder
 * Rules.md compliant - Pinned Instagram-like top bar that stays fixed while scrolling
 * 
 * Features:
 * - Left: App logo + "SperrmüllFinder" text with perfect baseline alignment
 * - Right: Notification bell icon with red dot indicator for unread notifications
 * - Pinned behavior (does not scroll with content)
 * - Professional Material3 design with FCM integration
 * - Accessibility support with content descriptions
 * - Localized strings (DE/EN)
 */
@Composable
fun HomeTopBar(
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier,
    notificationsViewModel: NotificationsViewModel = hiltViewModel()
) {
    val uiState by notificationsViewModel.uiState.collectAsStateWithLifecycle()
    val hasUnread = uiState.unreadCount > 0
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side: App logo + title
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App logo
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = stringResource(R.string.app_logo_cd),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // App name
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.25).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        // Right side: Notification bell with red dot indicator
        Box {
            IconButton(
                onClick = onNotificationsClick
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = stringResource(R.string.notifications_cd),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Red dot indicator for unread notifications
            if (hasUnread) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                )
            }
        }
    }
}

// Preview functions
@Preview(showBackground = true)
@Composable
private fun HomeTopBarPreview() {
    MaterialTheme {
        HomeTopBar(
            onNotificationsClick = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeTopBarDarkPreview() {
    MaterialTheme {
        HomeTopBar(
            onNotificationsClick = { }
        )
    }
}
