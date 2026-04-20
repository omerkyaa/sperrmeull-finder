package com.omerkaya.sperrmuellfinder.domain.usecase.admin

import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.repository.AdminRepository
import javax.inject.Inject

/**
 * Use case for 3-Tier Ban System
 * Rules.md compliant - Clean Architecture domain layer
 * 
 * Tier 1: Soft Ban (Temporary - 3 days, 1 week, 1 month)
 * Tier 2: Hard Ban (Permanent - Auth disabled)
 * Tier 3: Delete Account (Complete removal)
 */
class BanUserUseCase @Inject constructor(
    private val adminRepository: AdminRepository,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "BanUserUseCase"
        
        // Soft ban durations (in days)
        const val SOFT_BAN_3_DAYS = 3
        const val SOFT_BAN_1_WEEK = 7
        const val SOFT_BAN_1_MONTH = 30
        
        // Minimum reason length
        const val MIN_REASON_LENGTH = 10
    }
    
    /**
     * Soft Ban - Temporary suspension (3 days, 1 week, or 1 month)
     * User document updated with ban info
     * Firebase Auth stays active (app-level control)
     * 
     * @param userId User to ban
     * @param durationDays Duration in days (3, 7, or 30)
     * @param reason Reason for ban (min 10 characters)
     * @param reportId Optional report ID if ban is from a report
     */
    suspend fun softBan(
        userId: String,
        durationDays: Int,
        reason: String,
        reportId: String? = null
    ): Result<Unit> {
        logger.d(TAG, "Soft banning user $userId for $durationDays days")
        
        // Validate reason length
        if (reason.length < MIN_REASON_LENGTH) {
            return Result.Error(Exception("Ban reason must be at least $MIN_REASON_LENGTH characters"))
        }
        
        // Validate duration
        if (durationDays !in listOf(SOFT_BAN_3_DAYS, SOFT_BAN_1_WEEK, SOFT_BAN_1_MONTH)) {
            return Result.Error(Exception("Invalid soft ban duration. Must be 3, 7, or 30 days"))
        }
        
        return adminRepository.softBanUser(
            reportId = reportId,
            userId = userId,
            durationDays = durationDays,
            reason = reason
        )
    }
    
    /**
     * Soft Ban for 3 days (minor violations)
     */
    suspend fun softBan3Days(userId: String, reason: String, reportId: String? = null): Result<Unit> {
        return softBan(userId, SOFT_BAN_3_DAYS, reason, reportId)
    }
    
    /**
     * Soft Ban for 1 week (moderate violations)
     */
    suspend fun softBan1Week(userId: String, reason: String, reportId: String? = null): Result<Unit> {
        return softBan(userId, SOFT_BAN_1_WEEK, reason, reportId)
    }
    
    /**
     * Soft Ban for 1 month (serious violations)
     */
    suspend fun softBan1Month(userId: String, reason: String, reportId: String? = null): Result<Unit> {
        return softBan(userId, SOFT_BAN_1_MONTH, reason, reportId)
    }
    
    /**
     * Hard Ban - Permanent ban with Firebase Auth disabled
     * User cannot login, all access blocked
     * Reversible: Yes (via unban + re-enable Auth)
     * 
     * @param userId User to ban permanently
     * @param reason Reason for permanent ban (min 10 characters)
     * @param reportId Optional report ID if ban is from a report
     */
    suspend fun hardBan(
        userId: String,
        reason: String,
        reportId: String? = null
    ): Result<Unit> {
        logger.d(TAG, "Hard banning user $userId (permanent)")
        
        // Validate reason length
        if (reason.length < MIN_REASON_LENGTH) {
            return Result.Error(Exception("Ban reason must be at least $MIN_REASON_LENGTH characters"))
        }
        
        return adminRepository.hardBanUser(
            reportId = reportId,
            userId = userId,
            reason = reason
        )
    }
    
    /**
     * Delete Account - Complete removal (IRREVERSIBLE)
     * Firebase Auth deleted
     * Firestore user doc deleted
     * All user data removed (posts, comments, likes, follows)
     * Requires SUPER_ADMIN permission
     * 
     * @param userId User account to delete completely
     * @param reason Reason for deletion (min 10 characters)
     * @param reportId Optional report ID if deletion is from a report
     */
    suspend fun deleteAccount(
        userId: String,
        reason: String,
        reportId: String? = null
    ): Result<Unit> {
        logger.d(TAG, "Deleting user account $userId (IRREVERSIBLE)")
        
        // Validate reason length
        if (reason.length < MIN_REASON_LENGTH) {
            return Result.Error(Exception("Deletion reason must be at least $MIN_REASON_LENGTH characters"))
        }
        
        return adminRepository.deleteUserAccount(
            reportId = reportId,
            userId = userId,
            reason = reason
        )
    }
    
    /**
     * Unban a user (for soft/hard bans only)
     * Cannot restore deleted accounts
     * 
     * @param userId User to unban
     * @param reason Reason for unban (min 10 characters)
     */
    suspend fun unban(userId: String, reason: String): Result<Unit> {
        logger.d(TAG, "Unbanning user $userId")
        
        // Validate reason length
        if (reason.length < MIN_REASON_LENGTH) {
            return Result.Error(Exception("Unban reason must be at least $MIN_REASON_LENGTH characters"))
        }
        
        return adminRepository.unbanUser(userId, reason)
    }
}

