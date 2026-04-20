package com.omerkaya.sperrmuellfinder.ui.setup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.core.ui.theme.SperrmullPrimary

/**
 * Initial user setup screen for city selection and permissions.
 * 
 * According to rules.md:
 * - City selection with search functionality
 * - Permission explanations with rationale
 * - Modern Material 3 design
 * - Smooth animations and transitions
 */
@Composable
fun InitialSetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: InitialSetupViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentStep by remember { mutableStateOf(SetupStep.CITY_SELECTION) }

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SetupEvent.CitySelected -> {
                    currentStep = SetupStep.PERMISSIONS
                }
                is SetupEvent.SetupComplete -> {
                    onSetupComplete()
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SperrmullPrimary.copy(alpha = 0.1f),
                        Color.White
                    )
                )
            )
    ) {
        when (currentStep) {
            SetupStep.CITY_SELECTION -> {
                CitySelectionStep(
                    uiState = uiState,
                    onCitySearch = viewModel::searchCities,
                    onCitySelected = viewModel::selectCity,
                    onNext = { currentStep = SetupStep.PERMISSIONS }
                )
            }
            SetupStep.PERMISSIONS -> {
                PermissionsStep(
                    uiState = uiState,
                    onPermissionRequest = viewModel::requestPermission,
                    onComplete = viewModel::completeSetup
                )
            }
        }
    }
}

@Composable
private fun CitySelectionStep(
    uiState: InitialSetupUiState,
    onCitySearch: (String) -> Unit,
    onCitySelected: (String) -> Unit,
    onNext: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Header
        Text(
            text = stringResource(R.string.setup_city_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = SperrmullPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.setup_city_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { query ->
                searchQuery = query
                onCitySearch(query)
            },
            label = { Text(stringResource(R.string.search_city)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = SperrmullPrimary
                )
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SperrmullPrimary,
                focusedLabelColor = SperrmullPrimary,
                cursorColor = SperrmullPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // City suggestions
        if (uiState.isLoadingCities) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SperrmullPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.citySuggestions) { city ->
                    CityItem(
                        city = city,
                        isSelected = city == uiState.selectedCity,
                        onClick = { onCitySelected(city) }
                    )
                }
            }
        }

        // Next button
        AnimatedVisibility(
            visible = uiState.selectedCity.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SperrmullPrimary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.next),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun CityItem(
    city: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                SperrmullPrimary.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = if (isSelected) SperrmullPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = city,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                color = if (isSelected) SperrmullPrimary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = SperrmullPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionsStep(
    uiState: InitialSetupUiState,
    onPermissionRequest: (PermissionType) -> Unit,
    onComplete: () -> Unit
) {
    val permissions = listOf(
        PermissionInfo(
            type = PermissionType.LOCATION,
            title = stringResource(R.string.permission_location_title),
            description = stringResource(R.string.permission_location_description),
            icon = Icons.Default.LocationOn,
            isRequired = true
        ),
        PermissionInfo(
            type = PermissionType.CAMERA,
            title = stringResource(R.string.permission_camera_title),
            description = stringResource(R.string.permission_camera_description),
            icon = Icons.Default.PhotoCamera,
            isRequired = true
        ),
        PermissionInfo(
            type = PermissionType.NOTIFICATIONS,
            title = stringResource(R.string.permission_notifications_title),
            description = stringResource(R.string.permission_notifications_description),
            icon = Icons.Default.Notifications,
            isRequired = false
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Header
        Text(
            text = stringResource(R.string.setup_permissions_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = SperrmullPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.setup_permissions_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Permissions list
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(permissions) { permission ->
                PermissionCard(
                    permission = permission,
                    isGranted = uiState.grantedPermissions.contains(permission.type),
                    onRequest = { onPermissionRequest(permission.type) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Complete button
        val allRequiredGranted = permissions
            .filter { it.isRequired }
            .all { uiState.grantedPermissions.contains(it.type) }

        Button(
            onClick = onComplete,
            enabled = allRequiredGranted,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SperrmullPrimary,
                disabledContainerColor = SperrmullPrimary.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (uiState.isCompletingSetup) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = stringResource(R.string.complete_setup),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(
    permission: PermissionInfo,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) {
                Color.Green.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (isGranted) Color.Green else SperrmullPrimary,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isGranted) Icons.Default.Check else permission.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = permission.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (permission.isRequired) {
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Box(
                                modifier = Modifier
                                    .background(
                                        Color.Red.copy(alpha = 0.2f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.required),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Red,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = permission.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            if (!isGranted) {
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onRequest,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SperrmullPrimary.copy(alpha = 0.1f),
                        contentColor = SperrmullPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.grant_permission),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// Data classes and enums
enum class SetupStep {
    CITY_SELECTION,
    PERMISSIONS
}

enum class PermissionType {
    LOCATION,
    CAMERA,
    NOTIFICATIONS
}

data class PermissionInfo(
    val type: PermissionType,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isRequired: Boolean
)

// Events
sealed class SetupEvent {
    data object CitySelected : SetupEvent()
    data object SetupComplete : SetupEvent()
}

// UI State
data class InitialSetupUiState(
    val isLoadingCities: Boolean = false,
    val citySuggestions: List<String> = emptyList(),
    val selectedCity: String = "",
    val grantedPermissions: Set<PermissionType> = emptySet(),
    val isCompletingSetup: Boolean = false,
    val error: String? = null
)
