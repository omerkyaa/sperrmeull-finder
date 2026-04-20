package com.omerkaya.sperrmuellfinder.ui.likes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.Divider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.model.PremiumFrameType
import com.omerkaya.sperrmuellfinder.domain.model.UserFavorites
import com.omerkaya.sperrmuellfinder.ui.common.SafeGlideImage

/**
 * ❤️ LIKES LIST SCREEN - SperrmüllFinder
 * Real-time Firestore integration with professional navigation
 * Shows users who liked a post with Instagram-style design
 * Navigation: Own profile -> ProfileScreen, Others -> UserProfileScreen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LikesListScreen(
    postId: String,
    onNavigateBack: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    onNavigateToOwnProfile: () -> Unit,
    viewModel: LikesListViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "LIKES",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = uiState.error!!,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            !uiState.isLoading && uiState.error == null && uiState.users.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = stringResource(R.string.empty_likes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(
                        items = uiState.users,
                        key = { it.uid }
                    ) { user ->
                        UserLikeItem(
                            user = user,
                            onUserClick = {
                                // Navigation logic: own profile vs other profile
                                if (uiState.currentUser?.uid == user.uid) {
                                    onNavigateToOwnProfile()
                                } else {
                                    onNavigateToUserProfile(user.uid)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Resimdeki gibi ince ayırıcı çizgi
                        if (uiState.users.indexOf(user) < uiState.users.size - 1) {
                            Divider(
                                modifier = Modifier.padding(start = 84.dp), // Avatar + spacing kadar indent
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserLikeItem(
    user: User,
    onUserClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onUserClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // User avatar - resimdeki gibi 56dp boyutunda
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
                    // Show initials when no photo URL - resimdeki gibi gradient background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF6366F1),
                                        Color(0xFF8B5CF6)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val initials = user.displayName.let { name ->
                            if (name.isBlank()) {
                                "U"
                            } else {
                                name.split(" ")
                                    .take(2)
                                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                    .joinToString("")
                                    .takeIf { it.isNotEmpty() } ?: name.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
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
            
            // Premium frame overlay - resimdeki gibi subtle
            if (user.isPremium) {
                val frameType = user.getPremiumFrameType()
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Premium frame border - resimdeki gibi ince çerçeve
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 2.dp,
                            color = when (frameType) {
                                PremiumFrameType.GOLD -> Color(0xFFFFD700)
                                PremiumFrameType.DIAMOND -> Color(0xFF00FFFF)
                                PremiumFrameType.PLATINUM -> Color(0xFFC0C0C0)
                                PremiumFrameType.RAINBOW -> Color(0xFFFF6B6B)
                                else -> MaterialTheme.colorScheme.primary
                            }
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        )
                    ) {}
                }
            }
        }
        
        // User info - resimdeki gibi sadece isim, temiz tasarım
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LikesListScreenPreview() {
    MaterialTheme {
        LikesListScreen(
            postId = "post123",
            onNavigateBack = {},
            onNavigateToUserProfile = {},
            onNavigateToOwnProfile = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UserLikeItemPreview() {
    MaterialTheme {
        UserLikeItem(
            user = User(
                uid = "user1",
                email = "john@example.com",
                displayName = "John Doe",
                photoUrl = "",
                city = "Berlin",
                dob = null,
                gender = null,
                xp = 1250,
                level = 5,
                honesty = 95,
                isPremium = true,
                premiumUntil = null,
                badges = emptyList(),
                favorites = UserFavorites(),
                fcmToken = null,
                deviceTokens = emptyList(),
                deviceLang = "de",
                deviceModel = "Android",
                deviceOs = "Android 13",
                frameLevel = 1,
                createdAt = null,
                updatedAt = null,
                lastLoginAt = null
            ),
            onUserClick = {}
        )
    }
}