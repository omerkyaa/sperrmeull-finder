package com.omerkaya.sperrmuellfinder.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.AccountDeletionStatus
import com.omerkaya.sperrmuellfinder.domain.repository.UserRepository
import com.omerkaya.sperrmuellfinder.domain.usecase.user.CancelAccountDeletionUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.user.DeleteAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeleteAccountViewModel @Inject constructor(
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val cancelAccountDeletionUseCase: CancelAccountDeletionUseCase,
    private val userRepository: UserRepository,
    private val logger: Logger
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeleteAccountUiState())
    val uiState: StateFlow<DeleteAccountUiState> = _uiState.asStateFlow()

    init {
        checkDeletionStatus()
    }

    private fun checkDeletionStatus() {
        viewModelScope.launch {
            userRepository.getAccountDeletionStatus()
                .catch { e ->
                    logger.e("DeleteAccountViewModel", "Error checking deletion status", e)
                }
                .collect { status ->
                    _uiState.update { it.copy(deletionStatus = status) }
                }
        }
    }

    fun updateReason(reason: String) {
        _uiState.update { it.copy(reason = reason) }
    }

    fun requestDeletion() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            when (val result = deleteAccountUseCase(uiState.value.reason.takeIf { it.isNotBlank() })) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            deletionRequested = true,
                            error = null
                        ) 
                    }
                }
                is Result.Error -> {
                    logger.e("DeleteAccountViewModel", "Error requesting deletion", result.exception)
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = result.exception.message ?: "Failed to request deletion"
                        ) 
                    }
                }
                Result.Loading -> { /* Handled by isLoading */ }
            }
        }
    }

    fun cancelDeletion() {
        _uiState.update { it.copy(isCancelling = true, error = null) }
        
        viewModelScope.launch {
            when (val result = cancelAccountDeletionUseCase()) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isCancelling = false,
                            deletionStatus = null,
                            error = null
                        ) 
                    }
                }
                is Result.Error -> {
                    logger.e("DeleteAccountViewModel", "Error cancelling deletion", result.exception)
                    _uiState.update { 
                        it.copy(
                            isCancelling = false,
                            error = result.exception.message ?: "Failed to cancel deletion"
                        ) 
                    }
                }
                Result.Loading -> { /* Handled by isCancelling */ }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class DeleteAccountUiState(
    val deletionStatus: AccountDeletionStatus? = null,
    val reason: String = "",
    val isLoading: Boolean = false,
    val isCancelling: Boolean = false,
    val deletionRequested: Boolean = false,
    val error: String? = null
)
