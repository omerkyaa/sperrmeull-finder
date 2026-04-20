package com.omerkaya.sperrmuellfinder.ui.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.model.PremiumFrameType
import com.omerkaya.sperrmuellfinder.ui.common.SafeGlideImage

/**
 * 👥 USER SEARCH RESULTS - SperrmüllFinder
 * Rules.md compliant - Instagram-style user list
 * 
 * Features:
 * - Instagram-style user cards with avatars
 * - Premium frame overlays for premium users
 * - Empty and loading states
 */
@Composable
fun UserSearchResults(
    users: List<User>,
    onUserClick: (String) -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    when {
        isLoading -> {
            UserSearchLoadingState()
        }
        users.isEmpty() -> {
            UserSearchEmptyState()
        }
        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(
                    items = users,
                    key = { it.uid }
                ) { user ->
                    UserSearchItem(
                        user = user,
                        onClick = { onUserClick(user.uid) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (users.indexOf(user) < users.size - 1) {
                        Divider(
                            modifier = Modifier.padding(start = 84.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual user search result item
 */
@Composable
private fun UserSearchItem(
    user: User,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            SafeGlideImage(
                imageUrl = user.photoUrl,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                contentDescription = "Profile photo of ${user.displayName}",
                errorPlaceholder = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val initials = user.displayName.let { name ->
                            if (name.isBlank()) "U" else {
                                name.split(" ")
                                    .take(2)
                                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                    .joinToString("")
                                    .ifBlank { name.firstOrNull()?.uppercaseChar()?.toString() ?: "U" }
                            }
                        }
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            )

            if (user.isPremium) {
                val frameType = user.getPremiumFrameType()
                androidx.compose.material3.Card(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape),
                    shape = CircleShape,
                    border = BorderStroke(
                        width = 2.dp,
                        color = when (frameType) {
                            PremiumFrameType.GOLD -> Color(0xFFFFD700)
                            PremiumFrameType.DIAMOND -> Color(0xFF00FFFF)
                            PremiumFrameType.PLATINUM -> Color(0xFFC0C0C0)
                            PremiumFrameType.RAINBOW -> Color(0xFFFF6B6B)
                            else -> MaterialTheme.colorScheme.primary
                        }
                    ),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    )
                ) {}
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            user.city?.takeIf { it.isNotBlank() }?.let { city ->
                Text(
                    text = city,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Loading state for user search
 */
@Composable
private fun UserSearchLoadingState() {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(5) { // Show 5 skeleton items
            UserSearchSkeletonItem()
        }
    }
}

/**
 * Skeleton item for loading state
 */
@Composable
private fun UserSearchSkeletonItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = CircleShape
                )
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(20.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )
        }
    }
}

/**
 * Empty state for user search
 */
@Composable
private fun UserSearchEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.search_users_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.search_users_empty_description),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
