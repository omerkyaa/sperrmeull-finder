package com.omerkaya.sperrmuellfinder.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.domain.model.SettingsCategory
import com.omerkaya.sperrmuellfinder.domain.model.SettingsItem
import com.omerkaya.sperrmuellfinder.ui.settings.components.LanguageSelectionDialog
import com.omerkaya.sperrmuellfinder.ui.settings.components.LogoutConfirmationDialog
import com.omerkaya.sperrmuellfinder.ui.settings.components.ThemeSelectionDialog

private val SettingsIconOrange = Color(0xFFFF7A2F)

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToPremium: (Boolean) -> Unit = {},
    onNavigateToAdminDashboard: () -> Unit = {},
    onNavigateToBlockedUsers: () -> Unit = {},
    onNavigateToDeleteAccount: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.NavigateBack -> onNavigateBack()
                is SettingsEvent.ShowNotificationSettings -> onNavigateToNotifications()
                is SettingsEvent.ShowPrivacySettings -> Unit
                is SettingsEvent.ShowAboutPage -> onNavigateToAbout()
                is SettingsEvent.NavigateToBlockedUsers -> onNavigateToBlockedUsers()
                is SettingsEvent.NavigateToDeleteAccount -> onNavigateToDeleteAccount()
                is SettingsEvent.NavigateToAdminDashboard -> onNavigateToAdminDashboard()
                is SettingsEvent.NavigateToPremium -> onNavigateToPremium(event.isPremium)
                is SettingsEvent.ShowLogoutDialog -> viewModel.showLogoutDialog()
                is SettingsEvent.NavigateToLogin -> onNavigateToLogin()
                is SettingsEvent.OpenUrl -> {
                    runCatching {
                        val uri = Uri.parse(event.url)
                        val intent = if (event.url.startsWith("mailto:")) {
                            Intent(Intent.ACTION_SENDTO, uri)
                        } else {
                            Intent(Intent.ACTION_VIEW, uri)
                        }
                        context.startActivity(intent)
                    }.onFailure {
                        snackbarHostState.showSnackbar(event.url)
                    }
                }
                is SettingsEvent.ShowError -> snackbarHostState.showSnackbar(context.getString(event.messageResId))
                is SettingsEvent.ShowSuccess -> snackbarHostState.showSnackbar(context.getString(event.messageResId))
                else -> Unit
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SettingsIconOrange)
            }
        } else {
            val settingsItems = viewModel.getSettingsItems()
            val groupedItems = settingsItems.groupBy { it.category }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    SettingsHeader(onNavigateBack = onNavigateBack)
                }

                item {
                    SettingsProfileCard()
                }

                groupedItems.forEach { (category, itemsInCategory) ->
                    item {
                        SectionLabel(
                            text = category.getTitle(uiState.userPreferences.language)
                        )
                    }

                    item {
                        SettingsSectionCard {
                            itemsInCategory.forEachIndexed { index, item ->
                                SettingsItemRow(
                                    item = item,
                                    language = uiState.userPreferences.language,
                                    isNotificationItem = item.id == "notifications",
                                    notificationEnabled = uiState.userPreferences.notificationSettings.pushNotificationsEnabled,
                                    onNotificationToggle = viewModel::toggleNotifications,
                                    onClick = { viewModel.onSettingsItemClick(item) }
                                )
                                if (index != itemsInCategory.lastIndex) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 66.dp)
                                            .height(1.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = uiState.userPreferences.theme,
            language = uiState.userPreferences.language,
            onThemeSelected = viewModel::updateTheme,
            onDismiss = viewModel::hideThemeDialog
        )
    }

    if (uiState.showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = uiState.userPreferences.language,
            onLanguageSelected = viewModel::updateLanguage,
            onDismiss = viewModel::hideLanguageDialog
        )
    }

    if (uiState.showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = viewModel::logout,
            onDismiss = viewModel::hideLogoutDialog,
            language = uiState.userPreferences.language
        )
    }
}

@Composable
private fun SettingsHeader(onNavigateBack: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onNavigateBack) {
                Text(
                    text = stringResource(R.string.close),
                    color = SettingsIconOrange,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun SettingsProfileCard() {
    val firebaseUser = FirebaseAuth.getInstance().currentUser
    val displayName = firebaseUser?.displayName?.takeIf { it.isNotBlank() } ?: "SperrmüllFinder"
    val email = firebaseUser?.email?.takeIf { it.isNotBlank() } ?: "contact@spermuellfinder.com"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(SettingsIconOrange.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = SettingsIconOrange,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = SettingsIconOrange,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun SettingsSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun SettingsItemRow(
    item: SettingsItem,
    language: com.omerkaya.sperrmuellfinder.domain.model.Language,
    isNotificationItem: Boolean,
    notificationEnabled: Boolean,
    onNotificationToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isNotificationItem, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconChip(
            icon = settingsIconFor(item.id),
            modifier = Modifier.size(34.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.getTitle(language),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            item.getDescription(language)?.let { desc ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isNotificationItem) {
            Switch(
                checked = notificationEnabled,
                onCheckedChange = onNotificationToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                    checkedTrackColor = SettingsIconOrange,
                    uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                    uncheckedTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                )
            )
        } else if (item.hasNavigation) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun IconChip(
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = SettingsIconOrange.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SettingsIconOrange,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun settingsIconFor(itemId: String): ImageVector {
    return when (itemId) {
        "theme" -> Icons.Default.Palette
        "language" -> Icons.Default.Language
        "premium" -> Icons.Default.Star
        "logout" -> Icons.Default.Logout
        "admin_dashboard" -> Icons.Default.AdminPanelSettings
        "notifications" -> Icons.Default.Notifications
        "blocked_users" -> Icons.Default.Block
        "delete_account" -> Icons.Default.Delete
        "about" -> Icons.Default.Info
        else -> Icons.Default.Close
    }
}
