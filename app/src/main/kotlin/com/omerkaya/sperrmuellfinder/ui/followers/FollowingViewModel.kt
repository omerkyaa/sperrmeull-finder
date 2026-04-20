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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/**
 * 🎯 FOLLOWING VIEW MODEL - SperrmüllFinder
 * Professional real-time following list with Firebase integration
 * Rules.md compliant - Clean Architecture UI layer
 * 
 * Features:
 * - Real-time following list updates
 * - Unfollow functionality
 * - Current user detection (cannot follow yourself)
 * - Loading and error states
 * - Optimistic UI updates
 */
@HiltViewModel
class FollowingViewModel @Inject constructor(
    private val getFollowersUseCase: GetFollowersUseCase,
    private val followUserUseCase: FollowUserUseCase,
    private val getBlockedUsersUseCase: GetBlockedUsersUseCase,
    private val userRepository: UserRepository,
    private val logger: Logger
) : ViewModel() {
    
    companion object {
        private const val TAG = "FollowingViewModel"
    }
    
    private val _uiState = MutableStateFlow(FollowingUiState())
    val uiState: StateFlow<FollowingUiState> = _uiState.asStateFlow()
    private var followingJob: Job? = null
    private var currentViewedUserId: String? = null
    
    /**
     * Load following list for a user with real-time updates
     */
    fun loadFollowing(userId: String) {
        currentViewedUserId = userId
        followingJob?.cancel()
        followingJob = viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                // Get current user ID to detect self
                val currentUser = userRepository.getCurrentUser().first()
                val currentUserId = currentUser?.uid
                
                logger.d(TAG, "Loading following for user: $userId, current user: $currentUserId")
                
                // Combine following list with current user
                combine(
                    getFollowersUseCase.getFollowing(userId),
                    userRepository.getCurrentUser(),
                    getBlockedUsersUseCase()
                ) { following, _, blockedUsers ->
                    val blockedByMeIds = blockedUsers.map { it.blockedUserId }.toSet()
                    following
                        .filterNot { blockedByMeIds.contains(it.uid) }
                        .distinctBy { it.uid }
                        .sortedBy { "${it.displayName.lowercase()}_${it.uid}" }
                }
                    .catch { e ->
                        if (e is CancellationException) throw e
                        logger.e(TAG, "Error loading following", e)
                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                error = e.message ?: "Unknown error loading following"
                            ) 
                        }
                    }
                    .collect { following ->
                        // Enrich following with current user detection
                        val enrichedFollowing = following.map { followedUser ->
                            FollowingItem(
                                user = followedUser,
                                isCurrentUser = currentUserId == followedUser.uid
                            )
                        }
                        
                        _uiState.update { 
                            it.copy(
                                following = enrichedFollowing,
                                followingCount = enrichedFollowing.size,
                                isLoading = false,
                                isRefreshing = false,
                                error = null
                            ) 
                        }
                        
                        logger.d(TAG, "Loaded ${enrichedFollowing.size} unique following")
                    }
            } catch (e: CancellationException) {
                logger.d(TAG, "loadFollowing cancelled for userId=$userId")
                throw e
            } catch (e: Exception) {
                logger.e(TAG, "Error loading following", e)
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

    fun refreshFollowing() {
        val targetUserId = currentViewedUserId ?: return
        _uiState.update { it.copy(isRefreshing = true) }
        loadFollowing(targetUserId)
    }
    
    /**
     * Unfollow a user
     */
    fun unfollowUser(targetUserId: String) {
        viewModelScope.launch {
            try {
                val currentUser = userRepository.getCurrentUser().first()
                if (currentUser == null) {
                    logger.e(TAG, "Cannot unfollow: user not authenticated")
                    return@launch
                }
                
                // Prevent self-unfollow
                if (currentUser.uid == targetUserId) {
                    logger.w(TAG, "Cannot unfollow yourself")
                    return@launch
                }
                
                logger.d(TAG, "Unfollowing user: $targetUserId")
                _uiState.update { state ->
                    state.copy(
                        followActionLoadingUserIds = state.followActionLoadingUserIds + targetUserId
                    )
                }
                
                // Perform actual unfollow operation
                val result = followUserUseCase.unfollow(targetUserId)
                
                // Handle result
                when (result) {
                    is com.omerkaya.sperrmuellfinder.core.util.Result.Success -> {
                        logger.d(TAG, "Unfollow successful")
                    }
                    is com.omerkaya.sperrmuellfinder.core.util.Result.Error -> {
                        logger.e(TAG, "Unfollow failed: ${result.exception.message}")
                    }
                    is com.omerkaya.sperrmuellfinder.core.util.Result.Loading -> {
                        // Loading state, do nothing
                    }
                }
            } catch (e: Exception) {
                logger.e(TAG, "Error unfollowing user", e)
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
 * UI State for Following Screen
 */
data class FollowingUiState(
    val following: List<FollowingItem> = emptyList(),
    val followingCount: Int = 0,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val followActionLoadingUserIds: Set<String> = emptySet(),
    val error: String? = null
)

/**
 * Following item with enriched data
 */
data class FollowingItem(
    val user: User,
    val isCurrentUser: Boolean = false
)
