package com.omerkaya.sperrmuellfinder.domain.usecase.admin

import androidx.paging.PagingData
import com.omerkaya.sperrmuellfinder.domain.model.ModerationPriority
import com.omerkaya.sperrmuellfinder.domain.model.Report
import com.omerkaya.sperrmuellfinder.domain.model.ReportStatus
import com.omerkaya.sperrmuellfinder.domain.model.ReportTargetType
import com.omerkaya.sperrmuellfinder.domain.repository.AdminRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting reports with filtering
 * Rules.md compliant - Clean Architecture domain layer
 */
class GetReportsUseCase @Inject constructor(
    private val adminRepository: AdminRepository
) {
    /**
     * Get reports with optional filtering
     * @param status Filter by report status
     * @param priority Filter by priority level
     * @param type Filter by target type (post/comment/user)
     * @return Flow of paginated reports
     */
    operator fun invoke(
        status: ReportStatus? = null,
        priority: ModerationPriority? = null,
        type: ReportTargetType? = null
    ): Flow<PagingData<Report>> {
        return adminRepository.getReports(status, priority, type)
    }
    
    /**
     * Get reports with pending status
     */
    fun getPendingReports(): Flow<PagingData<Report>> {
        return adminRepository.getReports(status = ReportStatus.OPEN)
    }
    
    /**
     * Get high priority reports
     */
    fun getHighPriorityReports(): Flow<PagingData<Report>> {
        return adminRepository.getReports(priority = ModerationPriority.HIGH)
    }
    
    /**
     * Get critical reports
     */
    fun getCriticalReports(): Flow<PagingData<Report>> {
        return adminRepository.getReports(priority = ModerationPriority.CRITICAL)
    }
}
