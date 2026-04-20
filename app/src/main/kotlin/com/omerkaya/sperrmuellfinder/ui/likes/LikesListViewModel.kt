package com.omerkaya.sperrmuellfinder.ui.likes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.usecase.GetPostLikesUsersUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.social.GetBlockedUsersUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.user.GetCurrentUserUseCase
import com.omerkaya.sperrmuellfinder.ui.navigation.AppDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 💖 LIKES LIST VIEWMODEL - SperrmüllFinder
 * Rules.md compliant - Clean Architecture UI layer
 *
 * Manages the state and logic for displaying a list of users who liked a specific post.
 */
@HiltViewModel
class LikesListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPostLikesUsersUseCase: GetPostLikesUsersUseCase,
    private val getBlockedUsersUseCase: GetBlockedUsersUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val logger: Logger
) : ViewModel() {

    private val postId: String = savedStateHandle[AppDestinations.ARG_POST_ID] ?: ""

    private val _uiState = MutableStateFlow(LikesListUiState())
    val uiState: StateFlow<LikesListUiState> = _uiState.asStateFlow()

    private var likesListenerJob: Job? = null
    private var currentUserJob: Job? = null
    private var blockedUsersJob: Job? = null

    init {
        if (postId.isEmpty()) {
            _uiState.update { it.copy(error = "Post ID is missing.", isLoading = false) }
            logger.e(TAG, "Post ID is missing for LikesListViewModel.")
        } else {
            loadCurrentUser()
            loadPostLikesUsers()
            observeBlockedUsers()
        }
    }

    private fun observeBlockedUsers() {
        blockedUsersJob?.cancel()
        blockedUsersJob = viewModelScope.launch {
            getBlockedUsersUseCase()
                .catch { e ->
                    if (e !is CancellationException) {
                        logger.e(TAG, "Error observing blocked users for likes refresh", e)
                    }
                }
                .collect {
                    logger.d(TAG, "Blocked users changed, refreshing likes list for post: $postId")
                    loadPostLikesUsers()
                }
        }
    }

    private fun loadCurrentUser() {
        currentUserJob?.cancel()
        currentUserJob = viewModelScope.launch {
            getCurrentUserUseCase()
                .catch { e ->
                    logger.e(TAG, "Error loading current user", e)
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect { user ->
                    _uiState.update { it.copy(currentUser = user) }
                    logger.d(TAG, "Current user loaded: ${user?.uid}")
                }
        }
    }

    private fun loadPostLikesUsers() {
        // Only show loading if we don't have data yet
        if (_uiState.value.users.isEmpty()) {
            _uiState.update { it.copy(isLoading = true, error = null) }
        }
        
        likesListenerJob?.cancel() // Cancel any previous listener
        likesListenerJob = viewModelScope.launch {
            getPostLikesUsersUseCase(postId)
                .catch { e ->
                    if (e is CancellationException) {
                        logger.d(TAG, "Likes list listener cancelled for post: $postId")
                    } else {
                        logger.e(TAG, "Error loading post likes users for post: $postId", e)
                        _uiState.update { it.copy(error = e.message, isLoading = false) }
                    }
                }
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            val users = result.data
                            _uiState.update {
                                it.copy(
                                    users = users,
                                    isLoading = false,
                                    error = null
                                )
                            }
                            logger.d(TAG, "Loaded ${users.size} likes for post: $postId")
                        }
                        is Result.Error -> {
                            _uiState.update {
                                it.copy(
                                    error = result.exception.message,
                                    isLoading = false
                                )
                            }
                            logger.e(TAG, "Error loading likes for post: $postId", result.exception)
                        }
                        is Result.Loading -> {
                            // Only show loading for initial load
                            if (_uiState.value.users.isEmpty()) {
                                _uiState.update { it.copy(isLoading = true) }
                            }
                        }
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        likesListenerJob?.cancel()
        currentUserJob?.cancel()
        blockedUsersJob?.cancel()
        logger.d(TAG, "LikesListViewModel cleared. Listeners cancelled.")
    }

    companion object {
        private const val TAG = "LikesListViewModel"
    }
}

data class LikesListUiState(
    val currentUser: User? = null,
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false, // Start with false for faster UI
    val error: String? = null
)