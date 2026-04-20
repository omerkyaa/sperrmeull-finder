package com.omerkaya.sperrmuellfinder.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.Notification
import com.omerkaya.sperrmuellfinder.domain.model.NotificationType
import com.omerkaya.sperrmuellfinder.domain.usecase.notifications.GetNotificationsUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.notifications.GetUnreadCountUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.notifications.MarkNotificationReadUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.user.GetCurrentUserUseCase
import com.omerkaya.sperrmuellfinder.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🔔 NOTIFICATIONS VIEWMODEL - SperrmüllFinder
 * Instagram-style notifications with real-time Firebase integration
 * Rules.md compliant - Clean Architecture UI layer
 */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val getUnreadCountUseCase: GetUnreadCountUseCase,
    private val markNotificationReadUseCase: MarkNotificationReadUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val notificationRepository: NotificationRepository,
    private val auth: FirebaseAuth,
    private val logger: Logger
) : ViewModel() {

    companion object {
        private const val TAG = "NotificationsViewModel"
        private const val PAGE_INCREMENT = 20L
        private const val MAX_QUERY_LIMIT = 200L
    }

    // UI State
    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    // Events
    private val _events = Channel<NotificationsEvent>(Channel.BUFFERED)
    val events: Flow<NotificationsEvent> = _events.receiveAsFlow()

    // Jobs for managing listeners
    private var notificationsJob: Job? = null
    private var unreadCountJob: Job? = null
    private var currentQueryLimit: Long = 20

    init {
        loadCurrentUserData()
    }

    private fun loadCurrentUserData() {
        viewModelScope.launch {
            getCurrentUserUseCase().collect { user ->
                _uiState.value = _uiState.value.copy(currentUser = user)
                user?.uid?.let { userId ->
                    loadNotifications(userId)
                    loadUnreadCount(userId)
                } ?: run {
                    _uiState.value = _uiState.value.copy(error = "User not authenticated")
                }
            }
        }
    }

    fun loadCurrentUser() {
        loadCurrentUserData()
    }

    /**
     * Called when notifications screen is opened.
     * Marks unseen notifications as seen so home bell badge is cleared immediately.
     */
    fun onScreenOpened() {
        onMarkAllAsRead(showFeedback = false)
    }

    private fun loadNotifications(userId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        notificationsJob?.cancel()
        notificationsJob = viewModelScope.launch {
            getNotificationsUseCase(userId, currentQueryLimit)
                .catch { e ->
                    if (e is CancellationException) {
                        logger.d(TAG, "Notifications listener cancelled for user: $userId")
                    } else {
                        logger.e(TAG, "Error loading notifications for user: $userId", e)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            error = e.message ?: "Unknown error"
                        )
                    }
                }
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            _uiState.value = _uiState.value.copy(
                                notifications = result.data,
                                isLoading = false,
                                isLoadingMore = false,
                                error = null
                            )
                            logger.d(TAG, "Loaded ${result.data.size} notifications for user: $userId")
                        }
                        is Result.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isLoadingMore = false,
                                error = result.exception.message ?: "Unknown error"
                            )
                            logger.e(TAG, "Error loading notifications for user: $userId", result.exception)
                        }
                        is Result.Loading -> {
                            _uiState.value = _uiState.value.copy(isLoading = true)
                        }
                    }
                }
        }
    }

    fun loadMoreNotifications() {
        val userId = _uiState.value.currentUser?.uid ?: return
        if (_uiState.value.isLoadingMore || _uiState.value.isLoading) return
        if (currentQueryLimit >= MAX_QUERY_LIMIT) return
        currentQueryLimit = (currentQueryLimit + PAGE_INCREMENT).coerceAtMost(MAX_QUERY_LIMIT)
        _uiState.value = _uiState.value.copy(isLoadingMore = true)
        loadNotifications(userId)
    }

    private fun loadUnreadCount(userId: String) {
        unreadCountJob?.cancel()
        unreadCountJob = viewModelScope.launch {
            getUnreadCountUseCase(userId)
                .catch { e ->
                    if (e is CancellationException) {
                        logger.d(TAG, "Unread count listener cancelled for user: $userId")
                    } else {
                        logger.e(TAG, "Error loading unread count for user: $userId", e)
                    }
                }
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            _uiState.value = _uiState.value.copy(unreadCount = result.data)
                            logger.d(TAG, "Unread count for user $userId: ${result.data}")
                        }
                        is Result.Error -> {
                            logger.e(TAG, "Error loading unread count for user: $userId", result.exception)
                        }
                        is Result.Loading -> {
                            // Handle loading state if needed
                        }
                    }
                }
        }
    }

    /**
     * Mark a notification as read
     */
    fun onNotificationClick(notification: Notification) {
        logger.d(TAG, "Notification clicked: ${notification.id}")

        // Mark as read if not already read
        if (!notification.isRead) {
            viewModelScope.launch {
                try {
                    val result = markNotificationReadUseCase.markSingle(notification.id)
                    when (result) {
                        is Result.Success -> {
                            logger.d(TAG, "Notification marked as read: ${notification.id}")
                        }
                        is Result.Error -> {
                            logger.e(TAG, "Error marking notification as read", result.exception)
                        }
                        is Result.Loading -> {
                            // Handle loading state if needed
                        }
                    }
                } catch (e: Exception) {
                    logger.e(TAG, "Exception marking notification as read", e)
                }
            }
        }

        // Navigate based on notification type and content
        when (notification.type) {
            NotificationType.LIKE, NotificationType.COMMENT -> {
                // Navigate to post detail if postId exists
                notification.postId?.let { postId ->
                    sendEvent(NotificationsEvent.NavigateToPost(postId))
                } ?: run {
                    // Fallback to user profile if no postId
                    notification.resolvedActorUserId?.let { actorUserId ->
                        sendEvent(NotificationsEvent.NavigateToProfile(actorUserId))
                    }
                }
            }
            NotificationType.FOLLOW -> {
                // Navigate to the follower's profile
                notification.resolvedActorUserId?.let { actorUserId ->
                    sendEvent(NotificationsEvent.NavigateToProfile(actorUserId))
                }
            }
            NotificationType.PREMIUM_NEARBY_POST -> {
                notification.postId?.let { postId ->
                    sendEvent(NotificationsEvent.NavigateToPost(postId))
                }
            }
            NotificationType.PREMIUM_EXPIRED -> {
                sendEvent(NotificationsEvent.NavigateToPremium)
            }
            NotificationType.POST_EXPIRED -> {
                // Navigate to user's own profile to see archived posts
                _uiState.value.currentUser?.uid?.let { currentUserId ->
                    sendEvent(NotificationsEvent.NavigateToProfile(currentUserId))
                }
            }
            NotificationType.SYSTEM -> {
                // Handle system notifications with deep links
                if (notification.deeplink.isNotBlank()) {
                    sendEvent(NotificationsEvent.HandleDeepLink(notification.deeplink))
                } else if (notification.hasValidDeepLink) {
                    sendEvent(NotificationsEvent.HandleDeepLink(notification.deeplink))
                }
            }
            NotificationType.ADMIN_PENALTY -> {
                // No navigation required; keep user on notifications screen.
            }
        }
    }

    /**
     * Mark all notifications as read
     */
    fun onMarkAllAsRead(showFeedback: Boolean = true) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            logger.w(TAG, "User not authenticated, cannot mark all as read")
            return
        }

        logger.d(TAG, "Marking all notifications as read")

        viewModelScope.launch {
            try {
                val result = markNotificationReadUseCase.markAll(userId)
                when (result) {
                    is Result.Success -> {
                        logger.d(TAG, "All notifications marked as read")
                        if (showFeedback) {
                            sendEvent(NotificationsEvent.ShowMessage(R.string.notifications_marked_all_read))
                        }
                    }
                    is Result.Error -> {
                        logger.e(TAG, "Error marking all notifications as read", result.exception)
                        sendEvent(NotificationsEvent.ShowError(R.string.error_mark_all_read))
                    }
                    is Result.Loading -> {
                        // Handle loading state if needed
                    }
                }
            } catch (e: Exception) {
                logger.e(TAG, "Exception marking all notifications as read", e)
                sendEvent(NotificationsEvent.ShowError(R.string.error_mark_all_read))
            }
        }
    }

    /**
     * Refresh notifications
     */
    fun onRefresh() {
        logger.d(TAG, "Refreshing notifications")
        currentQueryLimit = PAGE_INCREMENT
        _uiState.value.currentUser?.uid?.let { userId ->
            loadNotifications(userId)
            loadUnreadCount(userId)
        }
    }

    /**
     * Handle username click in notifications
     */
    fun onUsernameClick(actorUserId: String) {
        logger.d(TAG, "Username clicked for user: $actorUserId")
        
        if (actorUserId.isNotBlank()) {
            sendEvent(NotificationsEvent.NavigateToProfile(actorUserId))
        }
    }

    /**
     * Navigate back
     */
    fun onNavigateBack() {
        logger.d(TAG, "Navigate back")
        sendEvent(NotificationsEvent.NavigateBack)
    }

    private fun sendEvent(event: NotificationsEvent) {
        viewModelScope.launch {
            _events.send(event)
        }
    }

    override fun onCleared() {
        super.onCleared()
        notificationsJob?.cancel()
        unreadCountJob?.cancel()
        logger.d(TAG, "NotificationsViewModel cleared")
    }
}

/**
 * UI State for notifications screen
 */
data class NotificationsUiState(
    val notifications: List<Notification> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val currentUser: com.omerkaya.sperrmuellfinder.domain.model.User? = null
)

/**
 * Events for notifications screen
 */
sealed class NotificationsEvent {
    object NavigateBack : NotificationsEvent()
    data class NavigateToPost(val postId: String) : NotificationsEvent()
    data class NavigateToProfile(val userId: String) : NotificationsEvent()
    object NavigateToPremium : NotificationsEvent()
    data class HandleDeepLink(val deeplink: String) : NotificationsEvent()
    data class ShowMessage(val messageResId: Int) : NotificationsEvent()
    data class ShowError(val messageResId: Int) : NotificationsEvent()
}
