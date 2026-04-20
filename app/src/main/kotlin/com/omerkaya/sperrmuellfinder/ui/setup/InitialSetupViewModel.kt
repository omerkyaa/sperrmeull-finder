package com.omerkaya.sperrmuellfinder.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omerkaya.sperrmuellfinder.core.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Initial Setup Screen.
 * Handles city selection and permission management.
 * 
 * According to rules.md:
 * - Clean Architecture pattern
 * - State management with StateFlow
 * - Event handling for UI feedback
 */
@HiltViewModel
class InitialSetupViewModel @Inject constructor(
    private val logger: Logger
) : ViewModel() {

    private val _uiState = MutableStateFlow(InitialSetupUiState())
    val uiState: StateFlow<InitialSetupUiState> = _uiState.asStateFlow()

    private val _events = Channel<SetupEvent>()
    val events = _events.receiveAsFlow()

    // German cities for suggestions
    private val germanCities = listOf(
        "Berlin", "Hamburg", "München", "Köln", "Frankfurt am Main",
        "Stuttgart", "Düsseldorf", "Dortmund", "Essen", "Leipzig",
        "Bremen", "Dresden", "Hannover", "Nürnberg", "Duisburg",
        "Bochum", "Wuppertal", "Bielefeld", "Bonn", "Münster",
        "Karlsruhe", "Mannheim", "Augsburg", "Wiesbaden", "Gelsenkirchen",
        "Mönchengladbach", "Braunschweig", "Chemnitz", "Kiel", "Aachen",
        "Halle (Saale)", "Magdeburg", "Freiburg im Breisgau", "Krefeld", "Lübeck",
        "Oberhausen", "Erfurt", "Mainz", "Rostock", "Kassel",
        "Hagen", "Hamm", "Saarbrücken", "Mülheim an der Ruhr", "Potsdam",
        "Ludwigshafen am Rhein", "Oldenburg", "Leverkusen", "Osnabrück", "Solingen"
    )

    init {
        // Initialize with some popular cities
        _uiState.value = _uiState.value.copy(
            citySuggestions = germanCities.take(10)
        )
    }

    /**
     * Search for cities based on query
     */
    fun searchCities(query: String) {
        viewModelScope.launch {
            logger.d(Logger.TAG_DEFAULT, "Searching cities with query: $query")
            
            _uiState.value = _uiState.value.copy(isLoadingCities = true)
            
            try {
                // Simulate network delay
                kotlinx.coroutines.delay(300)
                
                val filteredCities = if (query.isBlank()) {
                    germanCities.take(10)
                } else {
                    germanCities.filter { city ->
                        city.contains(query, ignoreCase = true)
                    }.take(10)
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoadingCities = false,
                    citySuggestions = filteredCities,
                    error = null
                )
                
                logger.d(Logger.TAG_DEFAULT, "Found ${filteredCities.size} cities for query: $query")
                
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "Error searching cities", e)
                _uiState.value = _uiState.value.copy(
                    isLoadingCities = false,
                    error = e.message
                )
            }
        }
    }

    /**
     * Select a city
     */
    fun selectCity(city: String) {
        viewModelScope.launch {
            logger.i(Logger.TAG_DEFAULT, "City selected: $city")
            
            _uiState.value = _uiState.value.copy(
                selectedCity = city,
                error = null
            )
            
            _events.send(SetupEvent.CitySelected)
        }
    }

    /**
     * Request a specific permission
     */
    fun requestPermission(permissionType: PermissionType) {
        viewModelScope.launch {
            logger.d(Logger.TAG_DEFAULT, "Requesting permission: $permissionType")
            
            try {
                // Simulate permission request
                // In real implementation, this would use Android permission APIs
                kotlinx.coroutines.delay(500)
                
                // For demo purposes, always grant permission
                val currentPermissions = _uiState.value.grantedPermissions.toMutableSet()
                currentPermissions.add(permissionType)
                
                _uiState.value = _uiState.value.copy(
                    grantedPermissions = currentPermissions,
                    error = null
                )
                
                logger.i(Logger.TAG_DEFAULT, "Permission granted: $permissionType")
                
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "Error requesting permission: $permissionType", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to request permission: ${e.message}"
                )
            }
        }
    }

    /**
     * Complete the initial setup
     */
    fun completeSetup() {
        viewModelScope.launch {
            logger.i(Logger.TAG_DEFAULT, "Completing initial setup")
            
            _uiState.value = _uiState.value.copy(
                isCompletingSetup = true,
                error = null
            )
            
            try {
                // Simulate setup completion (save to preferences, etc.)
                kotlinx.coroutines.delay(1000)
                
                // Save selected city and permissions to preferences
                saveUserPreferences()
                
                _uiState.value = _uiState.value.copy(
                    isCompletingSetup = false
                )
                
                _events.send(SetupEvent.SetupComplete)
                
                logger.i(Logger.TAG_DEFAULT, "Initial setup completed successfully")
                
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "Error completing setup", e)
                _uiState.value = _uiState.value.copy(
                    isCompletingSetup = false,
                    error = "Failed to complete setup: ${e.message}"
                )
            }
        }
    }

    /**
     * Save user preferences after setup
     */
    private suspend fun saveUserPreferences() {
        val currentState = _uiState.value
        
        // TODO: Save to SharedPreferences or DataStore
        // - Selected city
        // - Granted permissions
        // - Setup completion flag
        
        logger.d(Logger.TAG_DEFAULT, "Saving user preferences:")
        logger.d(Logger.TAG_DEFAULT, "  Selected city: ${currentState.selectedCity}")
        logger.d(Logger.TAG_DEFAULT, "  Granted permissions: ${currentState.grantedPermissions}")
    }

    /**
     * Check if all required permissions are granted
     */
    fun areRequiredPermissionsGranted(): Boolean {
        val requiredPermissions = setOf(
            PermissionType.LOCATION,
            PermissionType.CAMERA
        )
        
        return requiredPermissions.all { permission ->
            _uiState.value.grantedPermissions.contains(permission)
        }
    }

    /**
     * Get setup progress as percentage
     */
    fun getSetupProgress(): Float {
        val currentState = _uiState.value
        var progress = 0f
        
        // City selection (50% of progress)
        if (currentState.selectedCity.isNotEmpty()) {
            progress += 0.5f
        }
        
        // Required permissions (50% of progress)
        val requiredPermissions = setOf(
            PermissionType.LOCATION,
            PermissionType.CAMERA
        )
        
        val grantedRequiredCount = requiredPermissions.count { permission ->
            currentState.grantedPermissions.contains(permission)
        }
        
        progress += (grantedRequiredCount.toFloat() / requiredPermissions.size) * 0.5f
        
        return progress
    }
}
