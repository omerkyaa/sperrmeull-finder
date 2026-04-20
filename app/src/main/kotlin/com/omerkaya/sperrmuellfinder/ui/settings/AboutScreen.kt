package com.omerkaya.sperrmuellfinder.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.core.ui.theme.SperrmullPrimary

/**
 * ℹ️ ABOUT SCREEN - SperrmüllFinder
 * Rules.md compliant - Material3 Compose UI
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val appInfo = uiState.appInfo

    fun openExternalLink(url: String) {
        runCatching {
            val uri = Uri.parse(url)
            val intent = if (url.startsWith("mailto:")) {
                Intent(Intent.ACTION_SENDTO, uri)
            } else {
                Intent(Intent.ACTION_VIEW, uri)
            }
            context.startActivity(intent)
        }.onFailure {
            Toast.makeText(context, context.getString(R.string.error_unknown), Toast.LENGTH_SHORT).show()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.about_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = SperrmullPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = SperrmullPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // App Logo and Name
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // App Logo
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = appInfo.appName,
                            modifier = Modifier.size(80.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // App Name
                        Text(
                            text = appInfo.appName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = SperrmullPrimary,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Version
                        Text(
                            text = "${stringResource(R.string.about_version)} ${appInfo.version} (${appInfo.buildNumber})",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Description
                        Text(
                            text = stringResource(R.string.about_app_description),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Links
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.about_links),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SperrmullPrimary
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Website
                        LinkItem(
                            icon = "🌐",
                            title = stringResource(R.string.about_website),
                            url = appInfo.website,
                            onClick = { openExternalLink(appInfo.website) }
                        )
                        
                        // Support
                        LinkItem(
                            icon = "📧",
                            title = stringResource(R.string.about_support),
                            url = appInfo.supportEmail,
                            onClick = { openExternalLink("mailto:${appInfo.supportEmail}") }
                        )
                        
                        // Privacy Policy
                        LinkItem(
                            icon = "🔒",
                            title = stringResource(R.string.about_privacy_policy),
                            url = appInfo.privacyPolicyUrl,
                            onClick = { openExternalLink(appInfo.privacyPolicyUrl) }
                        )
                        
                        // Terms of Service
                        LinkItem(
                            icon = "🍪",
                            title = stringResource(R.string.about_cookie_policy),
                            url = appInfo.cookiePolicyUrl,
                            onClick = { openExternalLink(appInfo.cookiePolicyUrl) }
                        )

                        LinkItem(
                            icon = "☎️",
                            title = stringResource(R.string.about_contact),
                            url = appInfo.contactUrl,
                            onClick = { openExternalLink(appInfo.contactUrl) }
                        )

                        LinkItem(
                            icon = "❓",
                            title = stringResource(R.string.about_faq),
                            url = appInfo.faqUrl,
                            onClick = { openExternalLink(appInfo.faqUrl) }
                        )
                        
                        // Imprint
                        LinkItem(
                            icon = "ℹ️",
                            title = stringResource(R.string.about_imprint),
                            url = appInfo.imprintUrl,
                            onClick = { openExternalLink(appInfo.imprintUrl) }
                        )
                    }
                }
            }
            
            // Copyright
            item {
                Text(
                    text = stringResource(R.string.about_copyright),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun LinkItem(
    icon: String,
    title: String,
    url: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
