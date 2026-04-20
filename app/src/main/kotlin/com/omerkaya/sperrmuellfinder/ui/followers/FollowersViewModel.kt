package com.omerkaya.sperrmuellfinder.ui.followers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.repository.UserRepository
import com.omerkaya.sperrmuellfinder.domain.usecase.social.FollowUserUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.social.GetBlockedUsersUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.social.GetFollowersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/**
 * 🎯 FOLLOWERS VIEW MODEL - SperrmüllFinder
 * Professional real-time followers list with Firebase integration
 * Rules.md compliant - Clean Architecture UI layer
 * 
 * Features:
 * - Real-time followers list updates
 * - Follow/Unfollow functionality
 * - Current user detection (cannot follow yourself)
 * - Loading and error states
 * - Optimistic UI updates
 */
@HiltViewModel
class FollowersViewModel @Inject constructor(
    private val getFollowersUseCase: GetFollowersUseCase,
    private val followUserUseCase: FollowUserUseCase,
    private val getBlockedUsersUseCase: GetBlockedUsersUseCase,
    private val userRepository: UserRepository,
    private val logger: Logger
) : ViewModel() {
    
    companion object {
        private const val TAG = "FollowersViewModel"
    }
    
    private val _uiState = MutableStateFlow(FollowersUiState())
    val uiState: StateFlow<FollowersUiState> = _uiState.asStateFlow()
    private var followersJob: Job? = null
    private var currentViewedUserId: String? = null
    
    /**
     * Load followers list for a user with real-time updates
     */
    fun loadFollowers(userId: String) {
        currentViewedUserId = userId
        followersJob?.cancel()
        followersJob = viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                userRepository.getCurrentUser()
                    .flatMapLatest { currentUser ->
                        val currentUserId = currentUser?.uid
                        logger.d(TAG, "Loading followers for user: $userId, current user: $currentUserId")

                        val followingFlow = if (currentUserId != null) {
                            getFollowersUseCase.getFollowing(currentUserId)
                        } else {
                            flowOf(emptyList())
                        }

                        combine(
                            getFollowersUseCase.getFollowers(userId),
                            followingFlow,
                            getBlockedUsersUseCase()
                        ) { followers, myFollowing, blockedUsers ->
                            val blockedByMeIds = blockedUsers.map { it.blockedUserId }.toSet()
                            val visibleFollowers = followers
                                .filterNot { blockedByMeIds.contains(it.uid) }
                                .distinctBy { it.uid }
                                .sortedBy { "${it.displayName.lowercase()}_${it.uid}" }
                            val myFollowingIds = myFollowing
                                .distinctBy { it.uid }
                                .map { it.uid }
                                .toSet()
                            visibleFollowers.map { follower ->
                                FollowerItem(
                                    user = follower,
                                    isFollowing = myFollowingIds.contains(follower.uid),
                                    isCurrentUser = currentUserId == follower.uid
                                )
                            }
                        }
                    }
                    .catch { e ->
                        if (e is CancellationException) throw e
                        logger.e(TAG, "Error loading followers", e)
                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                error = e.message ?: "Unknown error loading followers"
                            ) 
                        }
                    }
                    .collect { enrichedFollowers ->
                        _uiState.update { 
                            it.copy(
                                followers = enrichedFollowers,
                                followersCount = enrichedFollowers.size,
                                isLoading = false,
                                isRefreshing = false,
                                error = null
                            ) 
                        }
                        
                        logger.d(TAG, "Loaded ${enrichedFollowers.size} unique followers")
                    }
            } catch (e: CancellationException) {
                logger.d(TAG, "loadFollowers cancelled for userId=$userId")
                throw e
            } catch (e: Exception) {
                logger.e(TAG, "Error loading followers", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        isRefreshing = false,
                        error = e.message ?: "Unknown error"
                    ) 
                }
            }
        }
    }

    fun refreshFollowers() {
        val targetUserId = currentViewedUserId ?: return
        _uiState.update { it.copy(isRefreshing = true) }
        loadFollowers(targetUserId)
    }
    
    /**
     * Toggle follow status for a user
     */
    fun toggleFollow(targetUserId: String) {
        viewModelScope.launch {
            try {
                val currentUser = userRepository.getCurrentUser().first()
                if (currentUser == null) {
                    logger.e(TAG, "Cannot follow: user not authenticated")
                    return@launch
                }
                
                // Prevent self-follow
                if (currentUser.uid == targetUserId) {
                    logger.w(TAG, "Cannot follow yourself")
                    return@launch
                }
                
                // Find the follower item
                val followerItem = _uiState.value.followers.find { it.user.uid == targetUserId }
                if (followerItem == null) {
                    logger.e(TAG, "Follower not found in list")
                    return@launch
                }
                
                val currentFollowingState = followerItem.isFollowing
                _uiState.update { state ->
                    state.copy(
                        followActionLoadingUserIds = state.followActionLoadingUserIds + targetUserId
                    )
                }
                
                // Optimistic UI update
                _uiState.update { state ->
                    state.copy(
                        followers = state.followers.map { item ->
                            if (item.user.uid == targetUserId) {
                                item.copy(isFollowing = !currentFollowingState)
                            } else {
                                item
                            }
                        }
                    )
                }
                
                logger.d(TAG, "Toggling follow for user: $targetUserId, current state: $currentFollowingState")
                
                // Perform actual follow/unfollow operation
                val result = if (currentFollowingState) {
                    followUserUseCase.unfollow(targetUserId)
                } else {
                    followUserUseCase.follow(targetUserId)
                }
                
                // Handle result
                when (result) {
                    is com.omerkaya.sperrmuellfinder.core.util.Result.Success -> {
                        logger.d(TAG, "Follow toggle successful")
                    }
                    is com.omerkaya.sperrmuellfinder.core.util.Result.Error -> {
                        logger.e(TAG, "Follow toggle failed: ${result.exception.message}")
                        // Revert optimistic update on error
                        _uiState.update { state ->
                            state.copy(
                                followers = state.followers.map { item ->
                                    if (item.user.uid == targetUserId) {
                                        item.copy(isFollowing = currentFollowingState)
                                    } else {
                                        item
                                    }
                                }
                            )
                        }
                    }
                    is com.omerkaya.sperrmuellfinder.core.util.Result.Loading -> {
                        // Loading state, do nothing
                    }
                }
            } catch (e: Exception) {
                logger.e(TAG, "Error toggling follow", e)
            } finally {
                _uiState.update { state ->
                    state.copy(
                        followActionLoadingUserIds = state.followActionLoadingUserIds - targetUserId
                    )
                }
            }
        }
    }
}

/**
 * UI State for Followers Screen
 */
data class FollowersUiState(
    val followers: List<FollowerItem> = emptyList(),
    val followersCount: Int = 0,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val followActionLoadingUserIds: Set<String> = emptySet(),
    val error: String? = null
)

/**
 * Follower item with enriched data
 */
data class FollowerItem(
    val user: User,
    val isFollowing: Boolean = false,
    val isCurrentUser: Boolean = false
)
