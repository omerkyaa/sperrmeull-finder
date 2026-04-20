package com.omerkaya.sperrmuellfinder.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.ReportReason
import com.omerkaya.sperrmuellfinder.domain.model.ReportTargetType
import com.omerkaya.sperrmuellfinder.domain.usecase.report.ReportContentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Report functionality
 * Rules.md compliant - MVVM pattern with Clean Architecture
 */
@HiltViewModel
class ReportViewModel @Inject constructor(
    private val reportContentUseCase: ReportContentUseCase,
    private val logger: Logger
) : ViewModel() {

    companion object {
        private const val TAG = "ReportViewModel"
    }

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    fun reportContent(
        targetId: String,
        type: ReportTargetType,
        reason: ReportReason,
        description: String?
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = when (type) {
                ReportTargetType.POST -> reportContentUseCase.reportPost(targetId, reason, description)
                ReportTargetType.COMMENT -> reportContentUseCase.reportComment(targetId, reason, description)
                ReportTargetType.USER -> reportContentUseCase.reportUser(targetId, reason, description)
            }

            when (result) {
                is Result.Success -> {
                    logger.d(TAG, "Report submitted successfully")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        error = null
                    )
                }
                is Result.Error -> {
                    logger.e(TAG, "Error submitting report", result.exception)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = false,
                        error = result.exception.message
                    )
                }
                is Result.Loading -> {
                    // Already in loading state
                }
            }
        }
    }

    fun clearState() {
        _uiState.value = ReportUiState()
    }
}

/**
 * UI State for Report screen
 */
data class ReportUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)
