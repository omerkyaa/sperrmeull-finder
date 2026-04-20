package com.omerkaya.sperrmuellfinder.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.manager.PremiumManager
import com.omerkaya.sperrmuellfinder.domain.model.PostStatus
import com.omerkaya.sperrmuellfinder.domain.model.ReportReason
import com.omerkaya.sperrmuellfinder.domain.model.ReportTargetType
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.model.PremiumFrameType
import com.omerkaya.sperrmuellfinder.domain.repository.PostRepository
import com.omerkaya.sperrmuellfinder.domain.repository.UserRepository
import com.omerkaya.sperrmuellfinder.domain.repository.FirestoreRepository
import com.omerkaya.sperrmuellfinder.domain.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * ViewModel for viewing other users' profiles
 * Shows only public information (no XP/Honesty scores)
 */
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val firestoreRepository: FirestoreRepository,
    private val socialRepository: SocialRepository,
    private val postRepository: PostRepository,
    private val premiumManager: PremiumManager,
    private val logger: Logger
) : ViewModel() {
    
    companion object {
        private const val TAG = "UserProfileViewModel"
    }

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    private val _events = Channel<UserProfileEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var userObserverJob: Job? = null
    private var postsObserverJob: Job? = null
    private var followersObserverJob: Job? = null
    private var followingObserverJob: Job? = null
    private var followStatusObserverJob: Job? = null

    /**
     * Load current user profile (for own profile view)
     */
    fun loadCurrentUserProfile() {
        userObserverJob?.cancel()
        userObserverJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                userRepository.getCurrentUser()
                    .catch { exception ->
                        if (exception is CancellationException) throw exception
                        logger.e(Logger.TAG_DEFAULT, "Error loading current user profile", exception)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load current user"
                        )
                    }
                    .collect { user ->
                        if (user != null) {
                            updateUserState(user)
                            loadUserStats(user.uid)
                            loadUserPosts(user.uid)
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Current user not found"
                            )
                        }
                    }
            } catch (e: CancellationException) {
                logger.d(Logger.TAG_DEFAULT, "loadCurrentUserProfile cancelled")
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load current user"
                )
                logger.e(Logger.TAG_DEFAULT, "Error loading current user profile", e)
            }
        }
    }

    /**
     * Load user profile by ID with real-time updates
     */
    fun loadUser(userId: String) {
        userObserverJob?.cancel()
        userObserverJob = viewModelScope.launch {
            println("🎯 UserProfileViewModel - loadUser called with userId: '$userId'")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // Check if userId is empty or if it's current user
            if (userId.isBlank()) {
                println("🎯 UserProfileViewModel - userId is blank, using current user")
                loadCurrentUser()
                return@launch
            }
            
            // Check if this is current user's own profile
            val currentUserId = firebaseAuth.currentUser?.uid
            val isCurrentUser = currentUserId == userId
            
            if (isCurrentUser) {
                println("🎯 UserProfileViewModel - Loading own profile, using getCurrentUser")
                _uiState.value = _uiState.value.copy(isCurrentUser = true)
                loadCurrentUser()
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isCurrentUser = false)

            // Block guard: if either side blocked, profile must not be visible.
            val blockedByMe = when (val result = socialRepository.isBlocked(userId)) {
                is Result.Success -> result.data
                else -> false
            }
            val blockedMe = when (val result = socialRepository.isBlockedBy(userId)) {
                is Result.Success -> result.data
                else -> false
            }

            // Store block status so unblock option can be shown
            _uiState.value = _uiState.value.copy(isBlockedByCurrentUser = blockedByMe)

            if (blockedByMe || blockedMe) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    user = null,
                    error = "This profile is not available."
                )
                return@launch
            }
            
            try {
                // Start real-time user data listener for other users
                userRepository.getUserByIdFlow(userId)
                    .catch { exception ->
                        if (exception is CancellationException) throw exception
                        logger.e(Logger.TAG_DEFAULT, "Error observing user profile", exception)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load user"
                        )
                    }
                    .collect { user ->
                        println("🎯 UserProfileViewModel - getUserByIdFlow collected user: ${user?.displayName ?: "NULL"}, PhotoURL: ${user?.photoUrl ?: "NULL"}")
                        if (user != null) {
                            logger.d(Logger.TAG_DEFAULT, "Real-time user data updated: ${user.displayName}, PhotoURL: ${user.photoUrl}")
                            updateUserState(user)
                            
                            // Always (re)start follow count listeners so counts are always fresh
                            loadFollowCounts(userId)

                            // Load stats and posts only once to avoid redundant round-trips
                            if (_uiState.value.postsCount == 0) {
                                loadUserStats(userId)
                                loadUserPosts(userId)

                                // Check following status if current user is available
                                if (!currentUserId.isNullOrBlank() && currentUserId != userId) {
                                    checkFollowingStatus(currentUserId, userId)
                                }
                            }
                        } else {
                            logger.w(Logger.TAG_DEFAULT, "Realtime user null, retrying one-shot lookup: $userId")
                            when (val lookup = userRepository.getUserById(userId)) {
                                is Result.Success -> {
                                    val fallbackUser = lookup.data
                                    if (fallbackUser != null) {
                                        updateUserState(fallbackUser)
                                        loadUserStats(userId)
                                        loadUserPosts(userId)
                                        loadFollowCounts(userId)
                                    } else {
                                        _uiState.value = _uiState.value.copy(
                                            isLoading = false,
                                            error = "User not found"
                                        )
                                    }
                                }
                                else -> {
                                    _uiState.value = _uiState.value.copy(
                                        isLoading = false,
                                        error = "User not found"
                                    )
                                }
                            }
                        }
                    }
            } catch (e: CancellationException) {
                logger.d(Logger.TAG_DEFAULT, "loadUser cancelled for userId=$userId")
                throw e
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "Error loading user profile", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load user"
                )
            }
        }
    }
    
    /**
     * Load current user (for own profile)
     */
    private fun loadCurrentUser() {
        userObserverJob?.cancel()
        userObserverJob = viewModelScope.launch {
            try {
                userRepository.getCurrentUser()
                    .catch { exception ->
                        if (exception is CancellationException) throw exception
                        println("🎯 UserProfileViewModel - Error loading current user: ${exception.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load current user"
                        )
                    }
                    .collect { user ->
                        println("🎯 UserProfileViewModel - getCurrentUser collected: ${user?.displayName ?: "NULL"}, PhotoURL: ${user?.photoUrl ?: "NULL"}")
                        if (user != null) {
                            updateUserState(user)
                            loadUserStats(user.uid)
                            loadUserPosts(user.uid)
                            loadFollowCounts(user.uid)
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Current user not found"
                            )
                        }
                    }
            } catch (e: CancellationException) {
                logger.d(Logger.TAG_DEFAULT, "loadCurrentUser cancelled")
                throw e
            } catch (e: Exception) {
                println("🎯 UserProfileViewModel - Exception in loadCurrentUser: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load current user"
                )
            }
        }
    }

    /**
     * Update user state with loaded data
     */
    private fun updateUserState(user: User) {
        val resolvedDisplayName = user.displayName.takeIf { it.isNotBlank() }
            ?: user.nickname.takeIf { it.isNotBlank() }
            ?: user.getFullName().takeIf { it.isNotBlank() }
            ?: user.email.substringBefore("@").takeIf { it.isNotBlank() }
            ?: "User"
        val normalizedUser = user.copy(displayName = resolvedDisplayName)

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = null,
            user = normalizedUser,
            isPremium = normalizedUser.isPremium,
            frameType = normalizedUser.getPremiumFrameType()
        )
        
        logger.d(Logger.TAG_DEFAULT, "User profile loaded: ${normalizedUser.displayName}, Level: ${normalizedUser.level}")
    }

    /**
     * Load user statistics
     */
    private suspend fun loadUserStats(userId: String) {
        try {
            val postsCountResult = userRepository.getUserPostsCount(userId)
            val postsCount = if (postsCountResult is Result.Success) postsCountResult.data else 0
            // Follower/following counts are managed exclusively by the real-time
            // loadFollowCounts() listener to avoid race conditions.
            _uiState.value = _uiState.value.copy(postsCount = postsCount)
            logger.d(Logger.TAG_DEFAULT, "User stats loaded: Posts=$postsCount")
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Error loading user stats", e)
        }
    }

    /**
     * Load user posts (mock data for now)
     */

    /**
     * Toggle follow/unfollow for this user with Firebase real-time updates
     */
    fun toggleFollow() {
        if (_uiState.value.isFollowActionLoading) {
            logger.d(TAG, "toggleFollow ignored: follow action already in progress")
            return
        }
        viewModelScope.launch {
            try {
                val currentUser = userRepository.getCurrentUser().first()
                val targetUser = _uiState.value.user
                
                if (currentUser == null || targetUser == null) {
                    logger.e(Logger.TAG_DEFAULT, "Cannot follow: current user or target user is null")
                    return@launch
                }

                if (currentUser.uid == targetUser.uid) {
                    logger.w(Logger.TAG_DEFAULT, "Blocked self-follow attempt for userId=${currentUser.uid}")
                    return@launch
                }
                
                val currentFollowingState = _uiState.value.isFollowing
                _uiState.value = _uiState.value.copy(isFollowActionLoading = true)
                
                // Perform actual follow/unfollow operation
                val result = if (currentFollowingState) {
                    unfollowUser(currentUser.uid, targetUser.uid)
                } else {
                    followUser(currentUser.uid, targetUser.uid)
                }
                
                when (result) {
                    is Result.Success -> {
                        logger.d(Logger.TAG_DEFAULT, "Follow operation successful: ${!currentFollowingState}")
                        // Force one-shot sync immediately after action; realtime listener keeps it fresh afterwards.
                        when (val syncResult = socialRepository.isFollowing(targetUser.uid)) {
                            is Result.Success -> {
                                _uiState.value = _uiState.value.copy(isFollowing = syncResult.data)
                            }
                            is Result.Error -> {
                                logger.w(TAG, "Failed to sync follow status after action", syncResult.exception)
                            }
                            is Result.Loading -> Unit
                        }
                        
                        // Send FCM notification for new follow
                        if (!currentFollowingState) {
                            sendFollowNotification(currentUser, targetUser)
                        }
                    }
                    is Result.Error -> {
                        logger.e(Logger.TAG_DEFAULT, "Error toggling follow", result.exception)
                    }
                    is Result.Loading -> {
                        // Handle loading state if needed
                    }
                }
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "Error toggling follow", e)
            } finally {
                _uiState.value = _uiState.value.copy(isFollowActionLoading = false)
            }
        }
    }
    
    /**
     * Follow a user
     */
    private suspend fun followUser(currentUserId: String, targetUserId: String): Result<Unit> {
        return try {
            logger.d(TAG, "Following user: $currentUserId -> $targetUserId")
            socialRepository.followUser(targetUserId)
        } catch (e: Exception) {
            logger.e(TAG, "Error following user", e)
            Result.Error(e)
        }
    }
    
    /**
     * Unfollow a user
     */
    private suspend fun unfollowUser(currentUserId: String, targetUserId: String): Result<Unit> {
        return try {
            logger.d(TAG, "Unfollowing user: $currentUserId -> $targetUserId")
            socialRepository.unfollowUser(targetUserId)
        } catch (e: Exception) {
            logger.e(TAG, "Error unfollowing user", e)
            Result.Error(e)
        }
    }
    
    /**
     * Send FCM notification for new follow
     */
    private suspend fun sendFollowNotification(follower: User, target: User) {
        try {
            // TODO: Implement FCM notification sending
            // This would typically involve:
            // 1. Create notification document in notifications/{targetUserId}
            // 2. Send FCM push notification
            // 3. Update notification badge count
            
            logger.d(Logger.TAG_DEFAULT, "Sending follow notification from ${follower.displayName} to ${target.displayName}")
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Error sending follow notification", e)
        }
    }

    /**
     * Select content tab
     */
    fun selectContentTab(tab: ContentTab) {
        _uiState.value = _uiState.value.copy(selectedContentTab = tab)
    }

    /**
     * Refresh user posts after a new post is created
     */
    fun refreshAfterPostCreated() {
        viewModelScope.launch {
            logger.d(Logger.TAG_DEFAULT, "Refreshing user profile posts after new post created")
            // Reload user posts
            _uiState.value.user?.let { user ->
                loadUserPosts(user.uid)
            }
        }
    }

    /**
     * Report user — writes to reports + moderation_queue collections
     */
    fun reportUser(reason: String) {
        viewModelScope.launch {
            try {
                val currentUser = userRepository.getCurrentUser().first()
                val targetUser = _uiState.value.user

                if (currentUser == null || targetUser == null) {
                    logger.e(TAG, "Cannot report: current user or target user is null")
                    return@launch
                }
                if (currentUser.uid == targetUser.uid) {
                    logger.w(TAG, "Blocked self-report attempt for userId=${currentUser.uid}")
                    return@launch
                }

                val reportReason = try {
                    ReportReason.valueOf(reason.uppercase())
                } catch (e: IllegalArgumentException) {
                    ReportReason.OTHER
                }

                val result = postRepository.reportContent(
                    targetType = ReportTargetType.USER,
                    targetId = targetUser.uid,
                    reason = reportReason,
                    description = null
                )

                when (result) {
                    is Result.Success -> logger.i(TAG, "User ${targetUser.uid} reported successfully")
                    is Result.Error -> logger.e(TAG, "Error reporting user ${targetUser.uid}", result.exception)
                    else -> Unit
                }
            } catch (e: Exception) {
                logger.e(TAG, "Error reporting user", e)
            }
        }
    }
    
    /**
     * Block user
     */
    fun blockUser() {
        blockUser(reason = null)
    }

    fun blockUser(reason: String?) {
        viewModelScope.launch {
            try {
                val currentUser = userRepository.getCurrentUser().first()
                val targetUser = _uiState.value.user
                
                if (currentUser == null || targetUser == null) {
                    logger.e(Logger.TAG_DEFAULT, "Cannot block: current user or target user is null")
                    return@launch
                }

                if (currentUser.uid == targetUser.uid) {
                    logger.w(Logger.TAG_DEFAULT, "Blocked self-block attempt for userId=${currentUser.uid}")
                    return@launch
                }
                
                when (val result = socialRepository.blockUser(targetUser.uid, reason)) {
                    is Result.Success -> {
                        logger.d(TAG, "Blocked user: ${targetUser.uid}")
                        _events.send(UserProfileEvent.NavigateBack)
                    }
                    is Result.Error -> {
                        logger.e(TAG, "Error blocking user", result.exception)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.exception.message ?: "Failed to block user"
                        )
                    }
                    is Result.Loading -> Unit
                }
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "Error blocking user", e)
            }
        }
    }
    
    /**
     * Setup real-time listeners for user data
     */
    private fun setupRealTimeListeners(userId: String) {
        viewModelScope.launch {
            try {
                // Listen to user data changes
                firestoreRepository.getUser(userId)
                    .catch { exception ->
                        logger.e(Logger.TAG_DEFAULT, "Error in user listener", exception)
                    }
                    .collect { user ->
                        user?.let { 
                            updateUserState(it)
                        }
                    }
            } catch (e: CancellationException) {
                logger.d(Logger.TAG_DEFAULT, "setupRealTimeListeners cancelled")
                throw e
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "Error setting up real-time listeners", e)
            }
        }
    }

    /**
     * Load user posts from Firestore with real-time updates
     */
    private fun loadUserPosts(userId: String) {
        postsObserverJob?.cancel()
        postsObserverJob = viewModelScope.launch {
            try {
                // Listen to user's posts with real-time updates
                firestoreRepository.getUserPosts(userId)
                    .catch { exception ->
                        if (exception is CancellationException) throw exception
                        logger.e(TAG, "Error loading user posts", exception)
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to load posts"
                        )
                    }
                    .collect { posts ->
                        logger.d(TAG, "Loaded ${posts.size} posts for user $userId")
                        
                        // Separate active and archived posts
                        val activePosts = posts.filter { it.status == PostStatus.ACTIVE }
                            .map { post ->
            PostPreview(
                                    id = post.id,
                                    imageUrl = post.images.firstOrNull(),
                                    description = post.description,
                                    likesCount = post.likesCount,
                                    commentsCount = post.commentsCount,
                                    createdAt = post.createdAt,
                                    availabilityPercent = post.availabilityPercent,
                                    status = post.status
                                )
                            }
                        
                        val archivedPosts = posts.filter { 
                            it.status == PostStatus.ARCHIVED 
                        }.map { post ->
            PostPreview(
                                id = post.id,
                                imageUrl = post.images.firstOrNull(),
                                description = post.description,
                                likesCount = post.likesCount,
                                commentsCount = post.commentsCount,
                                createdAt = post.createdAt,
                                archivedAt = post.updatedAt,
                                availabilityPercent = post.availabilityPercent,
                                status = post.status
                            )
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            posts = activePosts,
                            archivedPosts = archivedPosts,
                            postsCount = activePosts.size,
                            archivedPostsCount = archivedPosts.size
                        )
                    }
            } catch (e: CancellationException) {
                logger.d(TAG, "posts listener cancelled for userId=$userId")
                throw e
            } catch (e: Exception) {
                logger.e(TAG, "Error setting up posts listener", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load posts"
                )
            }
        }
    }

    /**
     * Load followers/following counts with real-time updates
     */
    private fun loadFollowCounts(userId: String) {
        followersObserverJob?.cancel()
        followersObserverJob = viewModelScope.launch {
            try {
                // Listen to followers count
                socialRepository.getFollowers(userId)
                    .catch { exception ->
                        if (exception is CancellationException) throw exception
                        logger.e(TAG, "Error loading followers count", exception)
                    }
                    .collect { followers ->
                        _uiState.value = _uiState.value.copy(
                            followersCount = followers.size
                        )
                    }
            } catch (e: CancellationException) {
                logger.d(TAG, "followers count listener cancelled for userId=$userId")
                throw e
            } catch (e: Exception) {
                logger.e(TAG, "Error setting up followers listener", e)
            }
        }
        
        followingObserverJob?.cancel()
        followingObserverJob = viewModelScope.launch {
            try {
                // Listen to following count
                socialRepository.getFollowing(userId)
                    .catch { exception ->
                        if (exception is CancellationException) throw exception
                        logger.e(TAG, "Error loading following count", exception)
                    }
                    .collect { following ->
                        _uiState.value = _uiState.value.copy(
                            followingCount = following.size
                        )
                    }
            } catch (e: CancellationException) {
                logger.d(TAG, "following count listener cancelled for userId=$userId")
                throw e
            } catch (e: Exception) {
                logger.e(TAG, "Error setting up following listener", e)
            }
        }
    }

    /**
     * Check if current user is following the target user
     */
    private fun checkFollowingStatus(currentUserId: String, targetUserId: String) {
        followStatusObserverJob?.cancel()
        followStatusObserverJob = viewModelScope.launch {
            try {
                firestoreRepository.isFollowing(currentUserId, targetUserId)
                    .catch { exception ->
                        if (exception is CancellationException) throw exception
                        logger.e(TAG, "Error checking following status", exception)
                    }
                    .collect { isFollowing ->
                        _uiState.value = _uiState.value.copy(
                            isFollowing = isFollowing
                        )
                    }
            } catch (e: CancellationException) {
                logger.d(TAG, "following status listener cancelled")
                throw e
            } catch (e: Exception) {
                logger.e(TAG, "Error setting up following status listener", e)
            }
        }
    }

    /**
     * Unblock a previously blocked user
     */
    fun unblockUser() {
        viewModelScope.launch {
            try {
                val targetUserId = _uiState.value.user?.uid ?: return@launch
                when (val result = socialRepository.unblockUser(targetUserId)) {
                    is Result.Success -> {
                        logger.d(TAG, "Unblocked user: $targetUserId")
                        _uiState.value = _uiState.value.copy(isBlockedByCurrentUser = false)
                    }
                    is Result.Error -> {
                        logger.e(TAG, "Error unblocking user", result.exception)
                        _uiState.value = _uiState.value.copy(
                            error = result.exception.message ?: "Failed to unblock user"
                        )
                    }
                    else -> Unit
                }
            } catch (e: Exception) {
                logger.e(TAG, "Error unblocking user", e)
            }
        }
    }

    override fun onCleared() {
        userObserverJob?.cancel()
        postsObserverJob?.cancel()
        followersObserverJob?.cancel()
        followingObserverJob?.cancel()
        followStatusObserverJob?.cancel()
        super.onCleared()
    }
}

/**
 * UI State for User Profile screen (other users)
 */
data class UserProfileUiState(
    val isLoading: Boolean = true,
    val error: String? = null,

    // User data (public only)
    val user: User? = null,
    val isPremium: Boolean = false,
    val frameType: PremiumFrameType = PremiumFrameType.NONE,
    val isCurrentUser: Boolean = false,
    val isBlockedByCurrentUser: Boolean = false,

    // Social data
    val isFollowing: Boolean = false,
    val isFollowActionLoading: Boolean = false,
    val postsCount: Int = 0,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val archivedPostsCount: Int = 0,

    // Content tabs and posts
    val selectedContentTab: ContentTab = ContentTab.CREATED,
    val posts: List<PostPreview> = emptyList(),
    val archivedPosts: List<PostPreview> = emptyList()
)

/**
 * One-shot navigation/action events for UserProfileScreen
 */
sealed class UserProfileEvent {
    data object NavigateBack : UserProfileEvent()
}
