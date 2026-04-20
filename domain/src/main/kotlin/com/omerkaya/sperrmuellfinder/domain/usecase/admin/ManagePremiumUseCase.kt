package com.omerkaya.sperrmuellfinder.domain.usecase.admin

import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.repository.AdminRepository
import javax.inject.Inject

/**
 * Use case for managing premium memberships
 * Rules.md compliant - Clean Architecture domain layer
 */
class ManagePremiumUseCase @Inject constructor(
    private val adminRepository: AdminRepository,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "ManagePremiumUseCase"
        
        // Common premium durations (in days)
        const val DURATION_1_WEEK = 7
        const val DURATION_1_MONTH = 30
        const val DURATION_3_MONTHS = 90
        const val DURATION_6_MONTHS = 180
        const val DURATION_1_YEAR = 365
    }
    
    /**
     * Grant premium to a user
     * @param userId User to grant premium
     * @param durationDays Duration in days
     * @param reason Reason for granting
     */
    suspend fun grantPremium(
        userId: String,
        durationDays: Int,
        reason: String
    ): Result<Unit> {
        logger.d(TAG, "Granting premium to user $userId for $durationDays days")
        
        if (durationDays <= 0) {
            return Result.Error(Exception("Duration must be positive"))
        }
        
        if (reason.length < 5) {
            return Result.Error(Exception("Reason must be at least 5 characters"))
        }
        
        return adminRepository.grantPremium(userId, durationDays, reason)
    }
    
    /**
     * Revoke premium from a user
     */
    suspend fun revokePremium(userId: String, reason: String): Result<Unit> {
        logger.d(TAG, "Revoking premium from user $userId")
        
        if (reason.length < 5) {
            return Result.Error(Exception("Reason must be at least 5 characters"))
        }
        
        return adminRepository.revokePremium(userId, reason)
    }
    
    /**
     * Grant 1 week premium
     */
    suspend fun grant1Week(userId: String, reason: String): Result<Unit> {
        return grantPremium(userId, DURATION_1_WEEK, reason)
    }
    
    /**
     * Grant 1 month premium
     */
    suspend fun grant1Month(userId: String, reason: String): Result<Unit> {
        return grantPremium(userId, DURATION_1_MONTH, reason)
    }
    
    /**
     * Grant 1 year premium
     */
    suspend fun grant1Year(userId: String, reason: String): Result<Unit> {
        return grantPremium(userId, DURATION_1_YEAR, reason)
    }
}
