package com.omerkaya.sperrmuellfinder.ui.admin.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.*
import com.omerkaya.sperrmuellfinder.domain.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Admin Reports Screen
 * Manages report filtering, paging, and admin actions
 */
@HiltViewModel
class AdminReportsViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val logger: Logger
) : ViewModel() {
    
    companion object {
        private const val TAG = "AdminReportsViewModel"
    }
    
    private val _uiState = MutableStateFlow(AdminReportsUiState())
    val uiState: StateFlow<AdminReportsUiState> = _uiState.asStateFlow()
    
    private val _events = Channel<AdminReportsEvent>(Channel.BUFFERED)
    val events: Flow<AdminReportsEvent> = _events.receiveAsFlow()
    
    // Paging flow for reports
    val reports: Flow<PagingData<Report>> = _uiState
        .map { state ->
            Triple(state.selectedStatus, state.selectedPriority, state.selectedType)
        }
        .distinctUntilChanged()
        .flatMapLatest { (status, priority, type) ->
            adminRepository.getReports(
                status = status,
                priority = priority,
                type = type
            )
        }
        .cachedIn(viewModelScope)
    
    init {
        logger.d(TAG, "AdminReportsViewModel initialized")
    }
    
    /**
     * Update filter selections
     */
    fun updateFilters(
        status: ReportStatus? = null,
        priority: ModerationPriority? = null,
        type: ReportTargetType? = null
    ) {
        _uiState.update { current ->
            current.copy(
                selectedStatus = status ?: current.selectedStatus,
                selectedPriority = priority ?: current.selectedPriority,
                selectedType = type ?: current.selectedType
            )
        }
    }
    
    /**
     * Perform admin action on a report - 3-Tier Ban System
     */
    fun performAction(
        reportId: String,
        action: ReportActionType,
        reason: String,
        banDuration: Int? = null
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                val result = when (action) {
                    ReportActionType.DISMISS -> {
                        adminRepository.dismissReport(reportId, reason)
                    }
                    ReportActionType.WARN_USER -> {
                        adminRepository.warnUser(reportId, reason)
                    }
                    ReportActionType.DELETE_CONTENT -> {
                        adminRepository.deleteReportedContent(reportId, reason)
                    }
                    // 🚫 Soft Ban - 3 Days
                    ReportActionType.SOFT_BAN_3_DAYS -> {
                        val reportResult = adminRepository.getReport(reportId)
                        if (reportResult is Result.Success) {
                            val userId = reportResult.data.targetOwnerId
                                ?: return@launch _events.send(AdminReportsEvent.ActionError("User ID not found"))
                            
                            adminRepository.softBanUser(
                                reportId = reportId,
                                userId = userId,
                                durationDays = 3,
                                reason = reason
                            )
                        } else {
                            Result.Error(Exception("Failed to get report"))
                        }
                    }
                    // 🚫 Soft Ban - 1 Week
                    ReportActionType.SOFT_BAN_1_WEEK -> {
                        val reportResult = adminRepository.getReport(reportId)
                        if (reportResult is Result.Success) {
                            val userId = reportResult.data.targetOwnerId
                                ?: return@launch _events.send(AdminReportsEvent.ActionError("User ID not found"))
                            
                            adminRepository.softBanUser(
                                reportId = reportId,
                                userId = userId,
                                durationDays = 7,
                                reason = reason
                            )
                        } else {
                            Result.Error(Exception("Failed to get report"))
                        }
                    }
                    // 🚫 Soft Ban - 1 Month
                    ReportActionType.SOFT_BAN_1_MONTH -> {
                        val reportResult = adminRepository.getReport(reportId)
                        if (reportResult is Result.Success) {
                            val userId = reportResult.data.targetOwnerId
                                ?: return@launch _events.send(AdminReportsEvent.ActionError("User ID not found"))
                            
                            adminRepository.softBanUser(
                                reportId = reportId,
                                userId = userId,
                                durationDays = 30,
                                reason = reason
                            )
                        } else {
                            Result.Error(Exception("Failed to get report"))
                        }
                    }
                    // 🔒 Hard Ban - Permanent
                    ReportActionType.HARD_BAN -> {
                        val reportResult = adminRepository.getReport(reportId)
                        if (reportResult is Result.Success) {
                            val userId = reportResult.data.targetOwnerId
                                ?: return@launch _events.send(AdminReportsEvent.ActionError("User ID not found"))
                            
                            adminRepository.hardBanUser(
                                reportId = reportId,
                                userId = userId,
                                reason = reason
                            )
                        } else {
                            Result.Error(Exception("Failed to get report"))
                        }
                    }
                    // 🗑️ Delete Account - Complete Removal
                    ReportActionType.DELETE_ACCOUNT -> {
                        val reportResult = adminRepository.getReport(reportId)
                        if (reportResult is Result.Success) {
                            val userId = reportResult.data.targetOwnerId
                                ?: return@launch _events.send(AdminReportsEvent.ActionError("User ID not found"))
                            
                            adminRepository.deleteUserAccount(
                                reportId = reportId,
                                userId = userId,
                                reason = reason
                            )
                        } else {
                            Result.Error(Exception("Failed to get report"))
                        }
                    }
                }
                
                when (result) {
                    is Result.Success -> {
                        logger.i(TAG, "Action $action completed successfully for report $reportId")
                        _events.send(AdminReportsEvent.ActionSuccess)
                    }
                    is Result.Error -> {
                        logger.e(TAG, "Action $action failed for report $reportId", result.exception)
                        _events.send(AdminReportsEvent.ActionError(
                            result.exception.message ?: "Unknown error"
                        ))
                    }
                    is Result.Loading -> {
                        // Ignore loading state
                    }
                }
            } catch (e: Exception) {
                logger.e(TAG, "Unexpected error performing action", e)
                _events.send(AdminReportsEvent.ActionError(e.message ?: "Unknown error"))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}

/**
 * UI State for Admin Reports Screen
 */
data class AdminReportsUiState(
    val isLoading: Boolean = false,
    val selectedStatus: ReportStatus? = null,
    val selectedPriority: ModerationPriority? = null,
    val selectedType: ReportTargetType? = null
)

/**
 * Events for Admin Reports Screen
 */
sealed class AdminReportsEvent {
    data class ShowMessage(val message: String) : AdminReportsEvent()
    object ActionSuccess : AdminReportsEvent()
    data class ActionError(val message: String) : AdminReportsEvent()
}
