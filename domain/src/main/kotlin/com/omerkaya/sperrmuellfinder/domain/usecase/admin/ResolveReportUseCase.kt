package com.omerkaya.sperrmuellfinder.domain.usecase.admin

import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.ReportAction
import com.omerkaya.sperrmuellfinder.domain.model.ReportActionType
import com.omerkaya.sperrmuellfinder.domain.repository.AdminRepository
import javax.inject.Inject

/**
 * Use case for resolving reports with various actions
 * Rules.md compliant - Clean Architecture domain layer
 * Updated for 3-Tier Ban System
 */
class ResolveReportUseCase @Inject constructor(
    private val adminRepository: AdminRepository,
    private val banUserUseCase: BanUserUseCase,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "ResolveReportUseCase"
    }
    
    /**
     * Resolve a report with the specified action
     * Supports 3-Tier Ban System:
     * - Soft Ban (3 days, 1 week, 1 month)
     * - Hard Ban (permanent, Auth disabled)
     * - Delete Account (complete removal)
     * 
     * @param reportId The report to resolve
     * @param action The action to take
     * @param userId The user ID (required for ban/delete actions)
     * @return Result of the operation
     */
    suspend operator fun invoke(
        reportId: String,
        action: ReportAction,
        userId: String? = null
    ): Result<Unit> {
        logger.d(TAG, "Resolving report $reportId with action ${action.action}")
        
        return when (action.action) {
            ReportActionType.DISMISS -> {
                adminRepository.dismissReport(reportId, action.reason)
            }
            
            ReportActionType.WARN_USER -> {
                adminRepository.warnUser(reportId, action.reason)
            }
            
            ReportActionType.DELETE_CONTENT -> {
                adminRepository.deleteReportedContent(reportId, action.reason)
            }
            
            // 🚫 Soft Ban - 3 Days
            ReportActionType.SOFT_BAN_3_DAYS -> {
                if (userId == null) {
                    return Result.Error(Exception("User ID required for ban action"))
                }
                banUserUseCase.softBan3Days(
                    userId = userId,
                    reason = action.reason,
                    reportId = reportId
                )
            }
            
            // 🚫 Soft Ban - 1 Week
            ReportActionType.SOFT_BAN_1_WEEK -> {
                if (userId == null) {
                    return Result.Error(Exception("User ID required for ban action"))
                }
                banUserUseCase.softBan1Week(
                    userId = userId,
                    reason = action.reason,
                    reportId = reportId
                )
            }
            
            // 🚫 Soft Ban - 1 Month
            ReportActionType.SOFT_BAN_1_MONTH -> {
                if (userId == null) {
                    return Result.Error(Exception("User ID required for ban action"))
                }
                banUserUseCase.softBan1Month(
                    userId = userId,
                    reason = action.reason,
                    reportId = reportId
                )
            }
            
            // 🔒 Hard Ban - Permanent
            ReportActionType.HARD_BAN -> {
                if (userId == null) {
                    return Result.Error(Exception("User ID required for ban action"))
                }
                banUserUseCase.hardBan(
                    userId = userId,
                    reason = action.reason,
                    reportId = reportId
                )
            }
            
            // 🗑️ Delete Account - Complete Removal
            ReportActionType.DELETE_ACCOUNT -> {
                if (userId == null) {
                    return Result.Error(Exception("User ID required for delete action"))
                }
                banUserUseCase.deleteAccount(
                    userId = userId,
                    reason = action.reason,
                    reportId = reportId
                )
            }
        }
    }
    
    /**
     * Quick dismiss a report
     */
    suspend fun dismissReport(reportId: String, reason: String): Result<Unit> {
        logger.d(TAG, "Quick dismissing report $reportId")
        return adminRepository.dismissReport(reportId, reason)
    }
    
    /**
     * Quick warn user from report
     */
    suspend fun warnUser(reportId: String, reason: String): Result<Unit> {
        logger.d(TAG, "Quick warning user for report $reportId")
        return adminRepository.warnUser(reportId, reason)
    }
    
    /**
     * Quick delete reported content
     */
    suspend fun deleteContent(reportId: String, reason: String): Result<Unit> {
        logger.d(TAG, "Quick deleting content for report $reportId")
        return adminRepository.deleteReportedContent(reportId, reason)
    }
    
    /**
     * Quick soft ban user for 3 days
     */
    suspend fun softBan3Days(reportId: String, userId: String, reason: String): Result<Unit> {
        logger.d(TAG, "Quick soft ban 3 days for report $reportId, user $userId")
        return banUserUseCase.softBan3Days(userId, reason, reportId)
    }
    
    /**
     * Quick soft ban user for 1 week
     */
    suspend fun softBan1Week(reportId: String, userId: String, reason: String): Result<Unit> {
        logger.d(TAG, "Quick soft ban 1 week for report $reportId, user $userId")
        return banUserUseCase.softBan1Week(userId, reason, reportId)
    }
    
    /**
     * Quick soft ban user for 1 month
     */
    suspend fun softBan1Month(reportId: String, userId: String, reason: String): Result<Unit> {
        logger.d(TAG, "Quick soft ban 1 month for report $reportId, user $userId")
        return banUserUseCase.softBan1Month(userId, reason, reportId)
    }
    
    /**
     * Quick hard ban user (permanent)
     */
    suspend fun hardBan(reportId: String, userId: String, reason: String): Result<Unit> {
        logger.d(TAG, "Quick hard ban for report $reportId, user $userId")
        return banUserUseCase.hardBan(userId, reason, reportId)
    }
    
    /**
     * Quick delete user account (irreversible)
     */
    suspend fun deleteUserAccount(reportId: String, userId: String, reason: String): Result<Unit> {
        logger.d(TAG, "Quick delete account for report $reportId, user $userId")
        return banUserUseCase.deleteAccount(userId, reason, reportId)
    }
}
