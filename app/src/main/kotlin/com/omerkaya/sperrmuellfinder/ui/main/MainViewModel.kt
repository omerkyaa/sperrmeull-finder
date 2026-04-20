package com.omerkaya.sperrmuellfinder.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omerkaya.sperrmuellfinder.core.navigation.BottomNavDestination
import com.omerkaya.sperrmuellfinder.core.navigation.NavigationManager
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.domain.manager.PremiumManager
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.usecase.user.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

/**
 * ViewModel for the main screen container.
 * Manages bottom navigation state, premium access, and navigation events.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val premiumManager: PremiumManager,
    private val logger: Logger
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _currentDestination = MutableStateFlow<BottomNavDestination?>(null)
    val currentDestination: StateFlow<BottomNavDestination?> = _currentDestination.asStateFlow()

    private val _events = Channel<MainEvent>()
    val events = _events.receiveAsFlow()

    init {
        logger.d(Logger.TAG_DEFAULT, "MainViewModel initialized - starting defensive startup")
        
        // CRASH FIX: Set safe default destination to prevent null crashes
        _currentDestination.value = BottomNavDestination.Home
        
        // CRASH FIX: Defensive startup sequence with timeout protection
        startDefensiveStartup()
    }
    
    /**
     * CRASH FIX: Defensive startup sequence that ensures app never gets stuck loading
     */
    private fun startDefensiveStartup() {
        viewModelScope.launch {
            try {
                logger.d(Logger.TAG_DEFAULT, "Starting defensive startup sequence...")
                
                // Phase 1: Initialize with timeout
                _uiState.value = _uiState.value.copy(
                    startupPhase = StartupPhase.Initializing,
                    isInitializing = true
                )
                
                // CRASH FIX: Use timeout to prevent infinite loading
                withTimeout(15_000) { // 15 second timeout for startup
                    
                    // Phase 2: Load user data
                    _uiState.value = _uiState.value.copy(startupPhase = StartupPhase.LoadingUser)
                    logger.d(Logger.TAG_DEFAULT, "Loading user data...")
                    
                    val userFlow = getCurrentUserUseCase()
                    
                    // Phase 3: Load premium status
                    _uiState.value = _uiState.value.copy(startupPhase = StartupPhase.LoadingPremium)
                    logger.d(Logger.TAG_DEFAULT, "Loading premium status...")
                    
                    val premiumFlow = premiumManager.isPremium
                    
                    // Combine both flows with individual error handling
                    combine(userFlow, premiumFlow) { user, isPremium ->
                        // CRASH FIX: Validate data before updating state
                        val safeUser = validateUser(user)
                        val safePremium = validatePremiumStatus(isPremium)
                        
                        _uiState.value = _uiState.value.copy(
                            currentUser = safeUser,
                            isPremium = safePremium,
                            isLoading = false,
                            isInitializing = false,
                            startupPhase = StartupPhase.Ready,
                            hasMinimumDataLoaded = true,
                            error = null
                        )
                        
                        logger.i(Logger.TAG_DEFAULT, "Startup completed - User: ${safeUser?.displayName}, Premium: $safePremium")
                        
            }.collect { }
                }
                
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                // CRASH FIX: Handle startup timeout gracefully
                logger.w(Logger.TAG_DEFAULT, "Startup timeout - continuing with defaults", e)
                handleStartupTimeout()
                
            } catch (e: Exception) {
                // CRASH FIX: Handle any startup errors gracefully
                logger.e(Logger.TAG_DEFAULT, "Startup error - continuing with defaults", e)
                handleStartupError(e)
            }
        }
        
        // Initialize notification count separately (non-critical)
        initializeNotificationCount()
    }
    
    /**
     * CRASH FIX: Validate user data to prevent null/invalid states
     */
    private fun validateUser(user: User?): User? {
        return try {
            user?.let { u ->
                // Basic validation - ensure required fields are not null/empty
                if (u.uid.isNotBlank()) {
                    u
                } else {
                    logger.w(Logger.TAG_DEFAULT, "Invalid user data - uid is blank")
                    null
                }
            }
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Error validating user data", e)
            null
        }
    }
    
    /**
     * CRASH FIX: Validate premium status to prevent invalid states
     */
    private fun validatePremiumStatus(isPremium: Boolean?): Boolean {
        return isPremium ?: false // Default to false if null
    }
    
    /**
     * CRASH FIX: Handle startup timeout by providing safe defaults
     */
    private fun handleStartupTimeout() {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isInitializing = false,
            startupPhase = StartupPhase.Ready,
            hasMinimumDataLoaded = true,
            currentUser = null,
            isPremium = false,
            error = null // Don't show error for timeout, just continue
        )
        logger.i(Logger.TAG_DEFAULT, "Startup timeout handled - app ready with defaults")
    }
    
    /**
     * CRASH FIX: Handle startup errors by providing safe defaults
     */
    private fun handleStartupError(error: Exception) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isInitializing = false,
            startupPhase = StartupPhase.Error,
            hasMinimumDataLoaded = false,
            currentUser = null,
            isPremium = false,
            error = "Startup error: ${error.message}"
        )
        logger.e(Logger.TAG_DEFAULT, "Startup error handled - showing error state")
    }
    
    /**
     * Initialize notification count (non-critical, separate from main startup)
     */
    private fun initializeNotificationCount() {
        viewModelScope.launch {
            try {
                // TODO: Implement real notification count from repository
                // For now, just set to 0
                _uiState.value = _uiState.value.copy(notificationCount = 0)
                logger.d(Logger.TAG_DEFAULT, "Notification count initialized")
            } catch (e: Exception) {
                logger.w(Logger.TAG_DEFAULT, "Failed to initialize notification count - using default", e)
            _uiState.value = _uiState.value.copy(notificationCount = 0)
            }
        }
    }

    /**
     * Handles destination selection from bottom navigation.
     */
    fun onDestinationSelected(
        destination: BottomNavDestination,
        navigationManager: NavigationManager
    ) {
        logger.d(Logger.TAG_DEFAULT, "Destination selected: ${destination.route}")
        
        // Check premium access
        if (destination.requiresPremium && !_uiState.value.isPremium) {
            logger.w(Logger.TAG_DEFAULT, "Premium required for ${destination.route}")
            
            // Track premium gate hit
            navigationManager.trackPremiumGateHit(destination.route)
            
            // Show premium required message and navigate to premium
            viewModelScope.launch {
                _events.send(MainEvent.ShowPremiumRequired)
                _events.send(MainEvent.NavigateToPremium)
            }
            return
        }
        
        // Update current destination
        _currentDestination.value = destination
        
        // Navigate using navigation manager
        navigationManager.navigateToBottomNavDestination(destination)
        
        // Save navigation state
        navigationManager.saveCurrentState()
    }

    /**
     * Updates the current destination (called from navigation events).
     */
    fun updateCurrentDestination(destination: BottomNavDestination) {
        if (_currentDestination.value != destination) {
            logger.d(Logger.TAG_DEFAULT, "Current destination updated: ${destination.route}")
            _currentDestination.value = destination
        }
    }

    /**
     * Handles post click navigation.
     */
    fun onPostClick(postId: String) {
        viewModelScope.launch {
            logger.d(Logger.TAG_DEFAULT, "Post clicked: $postId")
            _events.send(MainEvent.NavigateToPostDetail(postId))
        }
    }

    /**
     * Handles user profile click navigation.
     */
    fun onUserClick(userId: String) {
        viewModelScope.launch {
            logger.d(Logger.TAG_DEFAULT, "User clicked: $userId")
            _events.send(MainEvent.NavigateToUserProfile(userId))
        }
    }

    /**
     * Handles comments navigation.
     */
    fun onCommentsClick(postId: String) {
        viewModelScope.launch {
            logger.d(Logger.TAG_DEFAULT, "Comments clicked for post: $postId")
            _events.send(MainEvent.NavigateToComments(postId))
        }
    }

    /**
     * Handles premium upgrade navigation.
     */
    fun onPremiumClick() {
        viewModelScope.launch {
            logger.d(Logger.TAG_DEFAULT, "Premium upgrade clicked")
            _events.send(MainEvent.NavigateToPremium)
        }
    }

    /**
     * Shows a message to the user.
     */
    fun showMessage(message: String) {
        viewModelScope.launch {
            _events.send(MainEvent.ShowMessage(message))
        }
    }

    /**
     * Updates notification count.
     */
    fun updateNotificationCount(count: Int) {
        logger.d(Logger.TAG_DEFAULT, "Notification count updated: $count")
        _uiState.value = _uiState.value.copy(notificationCount = count)
    }

    /**
     * Handles deep link navigation.
     */
    fun handleDeepLink(uri: String, navigationManager: NavigationManager) {
        logger.i(Logger.TAG_DEFAULT, "Handling deep link in MainViewModel: $uri")
        navigationManager.handleDeepLink(uri)
    }

    /**
     * Clears any error states.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Handles app lifecycle events.
     */
    fun onAppResumed() {
        logger.d(Logger.TAG_DEFAULT, "App resumed")
        // Refresh user state and premium status
        // This will be handled automatically by the flows
    }

    fun onAppPaused() {
        logger.d(Logger.TAG_DEFAULT, "App paused")
        // Save any pending state
    }

    /**
     * Handles post creation - triggers refresh across all relevant screens
     */
    fun onPostCreated() {
        viewModelScope.launch {
            logger.d(Logger.TAG_DEFAULT, "Post created - triggering app-wide refresh")
            _events.send(MainEvent.PostCreated)
        }
    }
}

/**
 * UI State for Main Screen
 * CRASH FIX: Added defensive startup states
 */
data class MainUiState(
    val isLoading: Boolean = true,
    val isInitializing: Boolean = true,
    val error: String? = null,
    val currentUser: User? = null,
    val isPremium: Boolean = false,
    val notificationCount: Int = 0,
    val startupPhase: StartupPhase = StartupPhase.Initializing,
    val hasMinimumDataLoaded: Boolean = false
)

/**
 * Startup phases for defensive loading
 */
enum class StartupPhase {
    Initializing,      // App just started, loading critical data
    LoadingUser,       // Loading user data
    LoadingPremium,    // Loading premium status
    Ready,             // All critical data loaded, UI can be shown safely
    Error              // Critical error occurred, show error UI
}

/**
 * Events for Main Screen
 */
sealed class MainEvent {
    data class NavigateToPostDetail(val postId: String) : MainEvent()
    data class NavigateToUserProfile(val userId: String) : MainEvent()
    data class NavigateToComments(val postId: String) : MainEvent()
    data object NavigateToPremium : MainEvent()
    data class ShowMessage(val message: String) : MainEvent()
    data object ShowPremiumRequired : MainEvent()
    data object PostCreated : MainEvent()
}
