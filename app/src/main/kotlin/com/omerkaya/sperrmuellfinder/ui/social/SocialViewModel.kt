package com.omerkaya.sperrmuellfinder.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.Comment
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.usecase.social.CommentUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.social.FollowUserUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.social.GetFollowersUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.social.SearchUsersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🏆 SOCIAL VIEWMODEL - SperrmüllFinder
 * Rules.md compliant - Clean Architecture UI layer
 * 
 * Features:
 * - Follow/Unfollow users
 * - Comments management
 * - User search and discovery
 * - Real-time social interactions
 * - Professional error handling
 */
@HiltViewModel
class SocialViewModel @Inject constructor(
    private val followUserUseCase: FollowUserUseCase,
    private val getFollowersUseCase: GetFollowersUseCase,
    private val commentUseCase: CommentUseCase,
    private val searchUsersUseCase: SearchUsersUseCase,
    private val logger: Logger
) : ViewModel() {
    
    companion object {
        private const val TAG = "SocialViewModel"
    }
    
    // UI State
    private val _uiState = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()
    
    // Events
    private val _events = Channel<SocialEvent>()
    val events = _events.receiveAsFlow()
    
    // Search state
    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()
    
    // Comments state
    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()
    
    init {
        logger.d(TAG, "SocialViewModel initialized")
    }
    
    /**
     * Follow a user.
     */
    fun followUser(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            when (val result = followUserUseCase.follow(userId)) {
                is Result.Success -> {
                    logger.d(TAG, "Successfully followed user: $userId")
                    sendEvent(SocialEvent.ShowMessage("User followed successfully"))
                    sendEvent(SocialEvent.UserFollowed(userId))
                }
                is Result.Error -> {
                    logger.e(TAG, "Failed to follow user: $userId", result.exception)
                    sendEvent(SocialEvent.ShowError("Failed to follow user"))
                }
                is Result.Loading -> {
                    logger.d(TAG, "Following user: $userId...")
                }
            }
            
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
    
    /**
     * Unfollow a user.
     */
    fun unfollowUser(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            when (val result = followUserUseCase.unfollow(userId)) {
                is Result.Success -> {
                    logger.d(TAG, "Successfully unfollowed user: $userId")
                    sendEvent(SocialEvent.ShowMessage("User unfollowed successfully"))
                    sendEvent(SocialEvent.UserUnfollowed(userId))
                }
                is Result.Error -> {
                    logger.e(TAG, "Failed to unfollow user: $userId", result.exception)
                    sendEvent(SocialEvent.ShowError("Failed to unfollow user"))
                }
                is Result.Loading -> {
                    logger.d(TAG, "Unfollowing user: $userId...")
                }
            }
            
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
    
    /**
     * Toggle follow status for a user.
     */
    fun toggleFollow(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            when (val result = followUserUseCase.toggleFollow(userId)) {
                is Result.Success -> {
                    val isNowFollowing = result.data
                    val message = if (isNowFollowing) "User followed successfully" else "User unfollowed successfully"
                    val event = if (isNowFollowing) SocialEvent.UserFollowed(userId) else SocialEvent.UserUnfollowed(userId)
                    
                    logger.d(TAG, "Toggle follow result for $userId: $isNowFollowing")
                    sendEvent(SocialEvent.ShowMessage(message))
                    sendEvent(event)
                }
                is Result.Error -> {
                    logger.e(TAG, "Failed to toggle follow for user: $userId", result.exception)
                    sendEvent(SocialEvent.ShowError("Failed to update follow status"))
                }
                is Result.Loading -> {
                    logger.d(TAG, "Toggling follow for user: $userId...")
                }
            }
            
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
    
    /**
     * Add comment to a post.
     */
    fun addComment(postId: String, text: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            when (val result = commentUseCase.addComment(postId, text)) {
                is Result.Success -> {
                    val commentId = result.data
                    logger.d(TAG, "Successfully added comment $commentId to post $postId")
                    sendEvent(SocialEvent.ShowMessage("Comment added successfully"))
                    sendEvent(SocialEvent.CommentAdded(postId, commentId))
                }
                is Result.Error -> {
                    logger.e(TAG, "Failed to add comment to post $postId", result.exception)
                    sendEvent(SocialEvent.ShowError("Failed to add comment"))
                }
                is Result.Loading -> {
                    logger.d(TAG, "Adding comment to post: $postId...")
                }
            }
            
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
    
    /**
     * Delete a comment.
     */
    fun deleteComment(commentId: String, postId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            when (val result = commentUseCase.deleteComment(commentId, postId)) {
                is Result.Success -> {
                    logger.d(TAG, "Successfully deleted comment $commentId from post $postId")
                    sendEvent(SocialEvent.ShowMessage("Comment deleted successfully"))
                    sendEvent(SocialEvent.CommentDeleted(postId, commentId))
                }
                is Result.Error -> {
                    logger.e(TAG, "Failed to delete comment $commentId", result.exception)
                    sendEvent(SocialEvent.ShowError("Failed to delete comment"))
                }
                is Result.Loading -> {
                    logger.d(TAG, "Deleting comment: $commentId...")
                }
            }
            
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
    
    /**
     * Search users by query.
     */
    fun searchUsers(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)
            
            when (val result = searchUsersUseCase.searchUsers(query)) {
                is Result.Success -> {
                    val users = result.data
                    _searchResults.value = users
                    logger.d(TAG, "Found ${users.size} users for query: $query")
                }
                is Result.Error -> {
                    logger.e(TAG, "Failed to search users with query: $query", result.exception)
                    _searchResults.value = emptyList()
                    sendEvent(SocialEvent.ShowError("Failed to search users"))
                }
                is Result.Loading -> {
                    logger.d(TAG, "Searching users with query: $query...")
                }
            }
            
            _uiState.value = _uiState.value.copy(isSearching = false)
        }
    }
    
    /**
     * Get suggested users to follow.
     */
    fun getSuggestedUsers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            when (val result = searchUsersUseCase.getSuggestedUsers()) {
                is Result.Success -> {
                    val users = result.data
                    _searchResults.value = users
                    logger.d(TAG, "Found ${users.size} suggested users")
                }
                is Result.Error -> {
                    logger.e(TAG, "Failed to get suggested users", result.exception)
                    _searchResults.value = emptyList()
                    sendEvent(SocialEvent.ShowError("Failed to get suggested users"))
                }
                is Result.Loading -> {
                    logger.d(TAG, "Getting suggested users...")
                }
            }
            
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
    
    /**
     * Get user profile by ID.
     */
    fun getUserProfile(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            when (val result = searchUsersUseCase.getUserProfile(userId)) {
                is Result.Success -> {
                    val user = result.data
                    logger.d(TAG, "Retrieved user profile: ${user.displayName}")
                    sendEvent(SocialEvent.UserProfileLoaded(user))
                }
                is Result.Error -> {
                    logger.e(TAG, "Failed to get user profile: $userId", result.exception)
                    sendEvent(SocialEvent.ShowError("Failed to load user profile"))
                }
                is Result.Loading -> {
                    logger.d(TAG, "Loading user profile: $userId...")
                }
            }
            
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
    
    /**
     * Clear search results.
     */
    fun clearSearchResults() {
        _searchResults.value = emptyList()
        logger.d(TAG, "Search results cleared")
    }
    
    /**
     * Send an event to the UI.
     */
    private fun sendEvent(event: SocialEvent) {
        viewModelScope.launch {
            _events.send(event)
        }
    }
}

/**
 * UI State for Social features
 */
data class SocialUiState(
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val error: String? = null
)

/**
 * Events for Social features
 */
sealed class SocialEvent {
    data class ShowMessage(val message: String) : SocialEvent()
    data class ShowError(val error: String) : SocialEvent()
    data class UserFollowed(val userId: String) : SocialEvent()
    data class UserUnfollowed(val userId: String) : SocialEvent()
    data class CommentAdded(val postId: String, val commentId: String) : SocialEvent()
    data class CommentDeleted(val postId: String, val commentId: String) : SocialEvent()
    data class UserProfileLoaded(val user: User) : SocialEvent()
    data class NavigateToUserProfile(val userId: String) : SocialEvent()
}
