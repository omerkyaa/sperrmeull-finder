package com.omerkaya.sperrmuellfinder.ui.block

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.BlockedUser
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.usecase.social.BlockUserUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.social.GetBlockedUsersUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.social.UnblockUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlockedUsersViewModel @Inject constructor(
    private val getBlockedUsersUseCase: GetBlockedUsersUseCase,
    private val unblockUserUseCase: UnblockUserUseCase,
    private val logger: Logger
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlockedUsersUiState())
    val uiState: StateFlow<BlockedUsersUiState> = _uiState.asStateFlow()

    init {
        loadBlockedUsers()
    }

    private fun loadBlockedUsers() {
        viewModelScope.launch {
            getBlockedUsersUseCase()
                .catch { e ->
                    logger.e("BlockedUsersViewModel", "Error loading blocked users", e)
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            error = e.message ?: "Unknown error"
                        ) 
                    }
                }
                .collect { blockedUsers ->
                    _uiState.update { 
                        it.copy(
                            blockedUsers = blockedUsers,
                            isLoading = false,
                            error = null
                        ) 
                    }
                }
        }
    }

    fun unblockUser(userId: String) {
        _uiState.update { it.copy(unblockingUserId = userId) }
        
        viewModelScope.launch {
            when (val result = unblockUserUseCase(userId)) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            unblockingUserId = null,
                            successMessage = "User unblocked successfully"
                        ) 
                    }
                }
                is Result.Error -> {
                    logger.e("BlockedUsersViewModel", "Error unblocking user", result.exception)
                    _uiState.update { 
                        it.copy(
                            unblockingUserId = null,
                            error = result.exception.message ?: "Failed to unblock user"
                        ) 
                    }
                }
                Result.Loading -> { /* Handled by unblockingUserId */ }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun dismissSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
}

data class BlockedUsersUiState(
    val blockedUsers: List<BlockedUser> = emptyList(),
    val isLoading: Boolean = true,
    val unblockingUserId: String? = null,
    val error: String? = null,
    val successMessage: String? = null
)
