package com.omerkaya.sperrmuellfinder.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.AdminLog
import com.omerkaya.sperrmuellfinder.domain.model.AdminRole
import com.omerkaya.sperrmuellfinder.domain.model.ModerationQueueEntry
import com.omerkaya.sperrmuellfinder.domain.model.Report
import com.omerkaya.sperrmuellfinder.domain.repository.AdminRepository
import com.omerkaya.sperrmuellfinder.domain.usecase.admin.GetReportsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Admin Dashboard
 * Rules.md compliant - MVVM pattern with Clean Architecture
 */
@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val getReportsUseCase: GetReportsUseCase,
    private val logger: Logger
) : ViewModel() {

    companion object {
        private const val TAG = "AdminDashboardViewModel"
    }

    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    // Paging flows
    val pendingReports: Flow<PagingData<Report>> = 
        getReportsUseCase.getPendingReports().cachedIn(viewModelScope)
    
    val highPriorityReports: Flow<PagingData<Report>> = 
        getReportsUseCase.getHighPriorityReports().cachedIn(viewModelScope)
    
    val moderationQueue: Flow<PagingData<ModerationQueueEntry>> = 
        adminRepository.getModerationQueue().cachedIn(viewModelScope)
    
    val adminLogs: Flow<PagingData<AdminLog>> = 
        adminRepository.getAdminLogs().cachedIn(viewModelScope)

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Primary source of truth: auth custom claims
                // Fallback: users document flags for backward compatibility.
                when (val roleResult = adminRepository.getCurrentAdminRole()) {
                    is Result.Success -> {
                        val resolvedRole = roleResult.data ?: when (val fallback = adminRepository.getAdminRole("")) {
                            is Result.Success -> fallback.data
                            else -> null
                        }

                        if (resolvedRole == null) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "You do not have admin access"
                            )
                            return@launch
                        }

                        _uiState.value = _uiState.value.copy(
                            adminRole = resolvedRole,
                            isLoading = false
                        )
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = roleResult.exception.message
                        )
                    }
                    is Result.Loading -> { /* Continue */ }
                }
                
                logger.d(TAG, "Dashboard data loaded successfully")
            } catch (e: Exception) {
                logger.e(TAG, "Error loading dashboard data", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun refresh() {
        loadDashboardData()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI State for Admin Dashboard
 */
data class AdminDashboardUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val adminRole: AdminRole? = null,
    val stats: DashboardStats = DashboardStats()
)

/**
 * Dashboard statistics
 */
data class DashboardStats(
    val pendingReportsCount: Int = 0,
    val activeUsersCount: Int = 0,
    val bannedUsersCount: Int = 0,
    val postsToday: Int = 0,
    val premiumUsersCount: Int = 0
)
