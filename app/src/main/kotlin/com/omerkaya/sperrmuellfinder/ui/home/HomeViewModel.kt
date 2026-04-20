package com.omerkaya.sperrmuellfinder.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.firebase.auth.FirebaseAuth
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.data.manager.RemoteConfigManager
import com.omerkaya.sperrmuellfinder.domain.manager.LocationManager
import com.omerkaya.sperrmuellfinder.domain.manager.PremiumManager
import com.omerkaya.sperrmuellfinder.domain.model.Post
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.core.model.PostLocation
import com.omerkaya.sperrmuellfinder.domain.usecase.feed.GetPagedFeedUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.TogglePostLikeUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.GetPostLikeStatusUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.GetPostLikeCountUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.post.DeletePostUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.notifications.GetUnreadCountUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.social.GetBlockedUsersUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.user.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🏠 HOME SCREEN VIEWMODEL - SperrmüllFinder
 * Rules.md compliant - Clean Architecture UI layer
 * 
 * Features:
 * - Paging 3 integration for infinite scroll feed
 * - Real-time like/unlike with optimistic UI
 * - Welcome banner with auto-dismiss
 * - Pull-to-refresh support
 * - Notification badge count
 * - Premium vs Basic radius filtering
 * - Professional error handling and logging
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getPagedFeedUseCase: GetPagedFeedUseCase,
    private val togglePostLikeUseCase: TogglePostLikeUseCase,
    private val getPostLikeStatusUseCase: GetPostLikeStatusUseCase,
    private val getPostLikeCountUseCase: GetPostLikeCountUseCase,
    private val deletePostUseCase: DeletePostUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getBlockedUsersUseCase: GetBlockedUsersUseCase,
    private val getUnreadCountUseCase: GetUnreadCountUseCase,
    private val locationManager: LocationManager,
    private val premiumManager: PremiumManager,
    private val remoteConfigManager: RemoteConfigManager,
    private val auth: FirebaseAuth,
    private val logger: Logger
) : ViewModel() {
    
    companion object {
        private const val TAG = "HomeViewModel"
        private const val BASIC_RADIUS_METERS = PremiumManager.BASIC_RADIUS_METERS
        private const val PREMIUM_RADIUS_METERS = Int.MAX_VALUE
        private const val WELCOME_BANNER_DISMISS_DELAY = 5000L
        private const val MAX_ACTIVE_LIKE_LISTENERS = 40
        /** Minimum distance in meters before triggering a feed refresh */
        private const val LOCATION_THRESHOLD_METERS = 100.0
    }
    
    // UI State
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    // Events
    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = _events.receiveAsFlow()
    
    // Real-time like status tracking (postId -> isLiked)
    private val _realTimeLikeStatus = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val realTimeLikeStatus: StateFlow<Map<String, Boolean>> = _realTimeLikeStatus.asStateFlow()
    
    // Real-time like counts tracking (postId -> count)
    private val _realTimeLikeCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val realTimeLikeCounts: StateFlow<Map<String, Int>> = _realTimeLikeCounts.asStateFlow()
    
    // Like loading states (postId -> isLoading)
    private val _likeLoadingStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val likeLoadingStates: StateFlow<Map<String, Boolean>> = _likeLoadingStates.asStateFlow()
    
    // Active listeners tracking
    private val likeStatusJobs = mutableMapOf<String, Job>()
    private val likeCountJobs = mutableMapOf<String, Job>()
    private val likeListenerOrder = ArrayDeque<String>()

    val isPremiumUser: StateFlow<Boolean> = premiumManager.isPremium
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    private val _areAdsEnabled = MutableStateFlow(remoteConfigManager.areAdsEnabled())
    val areAdsEnabled: StateFlow<Boolean> = _areAdsEnabled.asStateFlow()
    
    val isFirestorePremiumUser: StateFlow<Boolean> = uiState
        .map { state -> state.currentUser?.isPremium == true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    // Location flow with 100m threshold — prevents feed reset on every GPS pulse
    private val locationWithThreshold: Flow<PostLocation?> = locationManager.currentLocation
        .distinctUntilChanged { old, new ->
            if (old == null || new == null) false
            else locationManager.calculateDistance(old, new) < LOCATION_THRESHOLD_METERS
        }

    // Feed data with Paging 3
    val postFeed: Flow<PagingData<Post>> = combine(
        getCurrentUserUseCase(),
        premiumManager.isPremium,
        locationWithThreshold,
        getBlockedUsersUseCase()
            .map { blocked -> blocked.map { it.blockedUserId }.sorted().joinToString(",") }
            .distinctUntilChanged()
    ) { user, isPremium, location, _ ->
        val radiusMeters = if (isPremium) PREMIUM_RADIUS_METERS else BASIC_RADIUS_METERS
        
        // Update UI state with current user
        _uiState.value = _uiState.value.copy(currentUser = user)
        
        FeedParams(
            user = user,
            isPremium = isPremium,
            radiusMeters = radiusMeters,
            location = location
        )
    }.flatMapLatest { feedParams ->
        val user = feedParams.user
        if (user != null) {
            getPagedFeedUseCase(
                userId = user.uid,
                isPremium = feedParams.isPremium,
                earlyAccessMinutes = PremiumManager.EARLY_ACCESS_MINUTES,
                radiusMeters = feedParams.radiusMeters,
                userLatitude = feedParams.location?.latitude,
                userLongitude = feedParams.location?.longitude
            ).cachedIn(viewModelScope)
        } else {
            flowOf(PagingData.empty())
        }
    }
    
    // Notification count
    val notificationCount: StateFlow<Int> = getCurrentUserUseCase()
        .flatMapLatest { user ->
            if (user != null) {
                getUnreadCountUseCase(user.uid).map { result ->
                    when (result) {
                        is Result.Success<*> -> result.data as Int
                        is Result.Error -> 0
                        is Result.Loading -> 0
                        else -> 0
                    }
                }
            } else {
                flowOf(0)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
    
    init {
        logger.d(TAG, "HomeViewModel initialized")
        refreshAdsPolicy()
        
        // Auto-dismiss welcome banner after delay
        viewModelScope.launch {
            kotlinx.coroutines.delay(WELCOME_BANNER_DISMISS_DELAY)
            if (_uiState.value.showWelcomeBanner && !_uiState.value.welcomeBannerDismissed) {
                dismissWelcomeBanner()
            }
        }
    }
    
    fun refreshAdsPolicy() {
        _areAdsEnabled.value = remoteConfigManager.areAdsEnabled()
    }
    
    /**
     * Manually dismiss the welcome banner.
     */
    fun dismissWelcomeBanner() {
        _uiState.value = _uiState.value.copy(
            showWelcomeBanner = false,
            welcomeBannerDismissed = true
        )
        logger.d(TAG, "Welcome banner dismissed")
    }
    
    /**
     * Navigate to notifications screen.
     */
    fun onNotificationClick() {
        logger.d(TAG, "Navigate to notifications")
        sendEvent(HomeEvent.NavigateToNotifications)
    }
    
    /**
     * Navigate to post detail screen.
     */
    fun onPostClick(postId: String) {
        logger.d(TAG, "Navigate to post detail: $postId")
        sendEvent(HomeEvent.NavigateToPostDetail(postId))
    }
    
    /**
     * Navigate to user profile screen.
     */
    fun onUserClick(userId: String, isSelf: Boolean) {
        logger.d(TAG, "Navigate to user profile: $userId (self: $isSelf)")
        if (isSelf) {
            sendEvent(HomeEvent.NavigateToProfile)
        } else {
            sendEvent(HomeEvent.NavigateToUserProfile(userId))
        }
    }
    
    /**
     * Navigate to likes screen for a post.
     */
    fun onLikesClick(postId: String) {
        logger.d(TAG, "Navigate to likes for post: $postId")
        sendEvent(HomeEvent.NavigateToLikes(postId))
    }
    
    /**
     * Navigate to comments screen for a post.
     */
    fun onCommentsClick(postId: String) {
        logger.d(TAG, "Navigate to comments for post: $postId")
        sendEvent(HomeEvent.NavigateToComments(postId))
    }
    
    /**
     * Handle share action for a post.
     */
    fun onShareClick(postId: String) {
        logger.d(TAG, "Share post: $postId")
        sendEvent(HomeEvent.SharePost(postId))
    }

    /**
     * Handle location click from PostCard with strict basic/premium radius gating.
     * Basic users can open map only within 1.5km of current location.
     */
    fun onPostLocationClick(post: Post) {
        val postLocation = post.location ?: return

        viewModelScope.launch {
            val isPremium = isPremiumUser.value
            if (isPremium) {
                sendEvent(HomeEvent.NavigateToMapLocation(postLocation.latitude, postLocation.longitude))
                return@launch
            }

            // Prefer live location; if missing, try one-shot fetch; finally fallback to precomputed post distance.
            val resolvedUserLocation = locationManager.currentLocation.value
                ?: when (val locationResult = locationManager.getCurrentLocation()) {
                    is Result.Success -> locationResult.data
                    else -> null
                }

            val distanceMeters = when {
                resolvedUserLocation != null -> locationManager.calculateDistance(resolvedUserLocation, postLocation)
                post.distanceFromUser != null -> post.distanceFromUser
                else -> null
            }

            if (distanceMeters != null && distanceMeters > BASIC_RADIUS_METERS) {
                logger.d(TAG, "Post location outside basic radius ($distanceMeters m). Showing premium gate.")
                sendEvent(HomeEvent.ShowPremiumRequiredForMap)
            } else {
                if (distanceMeters == null) {
                    logger.w(TAG, "Distance could not be resolved; allowing map navigation to avoid false premium gate.")
                }
                sendEvent(HomeEvent.NavigateToMapLocation(postLocation.latitude, postLocation.longitude))
            }
        }
    }

    /**
     * Delete own post directly from Home screen overflow menu.
     */
    fun onDeletePost(postId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = deletePostUseCase(postId)
            when (result) {
                is Result.Success -> {
                    logger.i(TAG, "✅ Post deleted from Home: $postId")
                    onComplete(true)
                }
                is Result.Error -> {
                    logger.e(TAG, "❌ Failed to delete post from Home: $postId", result.exception)
                    sendEvent(HomeEvent.ShowError(R.string.feed_error_desc))
                    onComplete(false)
                }
                is Result.Loading -> {
                    // no-op
                }
            }
        }
    }
    
    /**
     * Handle like/unlike action for a post - MÜKEMMEL YAPIYA GETİRİLDİ
     */
    fun onPostLike(postId: String, isCurrentlyLiked: Boolean) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            logger.w(TAG, "Cannot like post: user not authenticated")
            sendEvent(HomeEvent.ShowError(R.string.error_like_failed))
            return
        }
        
        logger.d(TAG, "💖 Toggle like for post: $postId (currently liked: $isCurrentlyLiked)")
        
        viewModelScope.launch {
            try {
                // Set loading state
                setLikeLoadingState(postId, true)
                
                // Optimistic UI update
                updateOptimisticLikeStatus(postId, !isCurrentlyLiked)
                
                val result = togglePostLikeUseCase(postId, currentUserId)
                when (result) {
                    is Result.Success -> {
                        val actualLikeStatus = result.data
                        logger.d(TAG, "✅ Like toggle successful: post=$postId, actualStatus=$actualLikeStatus")
                        
                        // Update real-time status with actual result
                        updateRealTimeLikeStatus(postId, actualLikeStatus)
                        
                        // Clear loading state
                        setLikeLoadingState(postId, false)
                        
                        // Initialize real-time listener if not already active
                        if (!likeStatusJobs.containsKey(postId)) {
                            startLikeStatusListener(postId, currentUserId)
                        }
                    }
                    is Result.Error -> {
                        logger.e(TAG, "❌ Error toggling like for post: $postId", result.exception)
                        
                        // Revert optimistic update
                        updateRealTimeLikeStatus(postId, isCurrentlyLiked)
                        
                        // Clear loading state
                        setLikeLoadingState(postId, false)
                        
                        sendEvent(HomeEvent.ShowError(R.string.error_like_failed))
                    }
                    is Result.Loading -> {
                        // Keep loading state
                    }
                }
            } catch (e: Exception) {
                logger.e(TAG, "💥 Exception toggling like for post: $postId", e)
                
                // Revert optimistic update and clear loading
                updateRealTimeLikeStatus(postId, isCurrentlyLiked)
                setLikeLoadingState(postId, false)
                
                sendEvent(HomeEvent.ShowError(R.string.error_like_failed))
            }
        }
    }
    
    /**
     * Initialize real-time like status and count listeners for a post.
     * Called when PostCard becomes visible to ensure persistent like status.
     */
    fun initializeLikeStatusForPost(postId: String) {
        val userId = auth.currentUser?.uid ?: return
        if (!likeStatusJobs.containsKey(postId)) {
            likeListenerOrder.remove(postId)
            likeListenerOrder.addLast(postId)
            while (likeListenerOrder.size > MAX_ACTIVE_LIKE_LISTENERS) {
                val evictedPostId = likeListenerOrder.removeFirst()
                likeStatusJobs.remove(evictedPostId)?.cancel()
                likeCountJobs.remove(evictedPostId)?.cancel()
                _realTimeLikeStatus.value = _realTimeLikeStatus.value.toMutableMap().apply {
                    remove(evictedPostId)
                }
                _realTimeLikeCounts.value = _realTimeLikeCounts.value.toMutableMap().apply {
                    remove(evictedPostId)
                }
                _likeLoadingStates.value = _likeLoadingStates.value.toMutableMap().apply {
                    remove(evictedPostId)
                }
            }
        }

        startLikeStatusListener(postId, userId)
        startLikeCountListener(postId)
    }
    
    /**
     * HELPER METHODS - Mükemmel like functionality için
     */
    
    private fun setLikeLoadingState(postId: String, isLoading: Boolean) {
        val currentStates = _likeLoadingStates.value.toMutableMap()
        if (isLoading) {
            currentStates[postId] = true
        } else {
            currentStates.remove(postId)
        }
        _likeLoadingStates.value = currentStates
        logger.d(TAG, "🔄 Like loading state: post=$postId, loading=$isLoading")
    }
    
    private fun updateOptimisticLikeStatus(postId: String, isLiked: Boolean) {
        val currentStatus = _realTimeLikeStatus.value.toMutableMap()
        currentStatus[postId] = isLiked
        _realTimeLikeStatus.value = currentStatus
        logger.d(TAG, "⚡ Optimistic like update: post=$postId, liked=$isLiked")
    }
    
    private fun updateRealTimeLikeStatus(postId: String, isLiked: Boolean) {
        val currentStatus = _realTimeLikeStatus.value.toMutableMap()
        currentStatus[postId] = isLiked
        _realTimeLikeStatus.value = currentStatus
        logger.d(TAG, "💖 Real-time like update: post=$postId, liked=$isLiked")
    }
    
    /**
     * Start real-time like status listener for a post.
     */
    private fun startLikeStatusListener(postId: String, userId: String) {
        // Cancel existing job for this post
        likeStatusJobs[postId]?.cancel()
        
        logger.d(TAG, "🔍 Setting up real-time like status for post: $postId, user: $userId")
        
        val job = viewModelScope.launch {
            getPostLikeStatusUseCase(postId, userId)
                .catch { e ->
                    if (e is CancellationException) {
                        logger.d(TAG, "💖 Like status listener cancelled for post: $postId")
                    } else {
                        logger.e(TAG, "❌ Error in real-time like status listener", e)
                    }
                }
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            val isLiked = result.data
                            _realTimeLikeStatus.value = _realTimeLikeStatus.value.toMutableMap().apply {
                                put(postId, isLiked)
                            }
                            logger.d(TAG, "💖 Real-time like status: post=$postId, user=$userId, liked=$isLiked")
                        }
                        is Result.Error -> {
                            logger.e(TAG, "❌ Error getting like status for post: $postId", result.exception)
                        }
                        is Result.Loading -> {
                            // Handle loading state if needed
                        }
                    }
                }
        }
        
        likeStatusJobs[postId] = job
    }
    
    /**
     * Start real-time like count listener for a post.
     */
    private fun startLikeCountListener(postId: String) {
        // Cancel existing job for this post
        likeCountJobs[postId]?.cancel()
        
        logger.d(TAG, "📊 Setting up real-time like count for post: $postId")
        
        val job = viewModelScope.launch {
            getPostLikeCountUseCase(postId)
                .catch { e ->
                    if (e is CancellationException) {
                        logger.d(TAG, "📊 Like count listener cancelled for post: $postId")
                    } else {
                        logger.e(TAG, "❌ Error in real-time like count listener", e)
                    }
                }
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            val count = result.data
                            _realTimeLikeCounts.value = _realTimeLikeCounts.value.toMutableMap().apply {
                                put(postId, count)
                            }
                            logger.d(TAG, "📊 Real-time like count: post=$postId, count=$count")
                        }
                        is Result.Error -> {
                            logger.e(TAG, "❌ Error getting like count for post: $postId", result.exception)
                        }
                        is Result.Loading -> {
                            // Handle loading state if needed
                        }
                    }
                }
        }
        
        likeCountJobs[postId] = job
    }
    
    override fun onCleared() {
        super.onCleared()
        // Cancel all active listeners
        likeStatusJobs.values.forEach { it.cancel() }
        likeCountJobs.values.forEach { it.cancel() }
        likeStatusJobs.clear()
        likeCountJobs.clear()
        logger.d(TAG, "HomeViewModel cleared. All like listeners cancelled.")
    }

    /**
     * Send an event to the UI.
     */
    private fun sendEvent(event: HomeEvent) {
        viewModelScope.launch {
            _events.send(event)
        }
    }
}

private data class FeedParams(
    val user: User?,
    val isPremium: Boolean,
    val radiusMeters: Int,
    val location: PostLocation?
)

/**
 * UI State for Home Screen
 */
data class HomeUiState(
    val currentUser: User? = null,
    val showWelcomeBanner: Boolean = true,
    val welcomeBannerDismissed: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Events for Home Screen
 */
sealed class HomeEvent {
    data object NavigateToNotifications : HomeEvent()
    data object NavigateToProfile : HomeEvent()
    data class NavigateToPostDetail(val postId: String) : HomeEvent()
    data class NavigateToUserProfile(val userId: String) : HomeEvent()
    data class NavigateToLikes(val postId: String) : HomeEvent()
    data class NavigateToComments(val postId: String) : HomeEvent()
    data class SharePost(val postId: String) : HomeEvent()
    data class NavigateToMapLocation(val latitude: Double, val longitude: Double) : HomeEvent()
    data object ShowPremiumRequiredForMap : HomeEvent()
    data class ShowError(val messageResId: Int) : HomeEvent()
    data class ShowMessage(val messageResId: Int) : HomeEvent()
}