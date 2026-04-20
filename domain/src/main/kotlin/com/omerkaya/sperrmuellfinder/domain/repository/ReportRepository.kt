package com.omerkaya.sperrmuellfinder.domain.repository

import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.Report
import com.omerkaya.sperrmuellfinder.domain.model.ReportTargetType
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user-facing report functionality
 * Rules.md compliant - Clean Architecture domain layer
 */
interface ReportRepository {
    
    /**
     * Create a new report
     * @param report Report to create (id, reporterId, reporterName will be set by implementation)
     * @return Result containing the created report ID
     */
    suspend fun createReport(report: Report): Result<String>
    
    /**
     * Get reports created by current user
     * @return Flow of user's reports
     */
    fun getUserReports(): Flow<List<Report>>
    
    /**
     * Check if user can report a target (rate limiting - 1 report per 24 hours per target)
     * @param targetId Target to check
     * @param type Target type
     * @return Result containing true if user can report, false if already reported
     */
    suspend fun canUserReport(targetId: String, type: ReportTargetType): Result<Boolean>
}
