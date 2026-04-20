package com.omerkaya.sperrmuellfinder.domain.model

import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Domain model for account deletion status
 * Rules.md compliant - Clean Architecture domain layer
 */
data class AccountDeletionStatus(
    val userId: String,
    val requestedAt: Date,
    val scheduledDeletionDate: Date,
    val reason: String?,
    val status: DeletionStatus = DeletionStatus.PENDING
) {
    /**
     * Check if deletion can be cancelled (within 30 days)
     */
    fun canCancel(): Boolean {
        return status == DeletionStatus.PENDING && 
               scheduledDeletionDate.after(Date())
    }
    
    /**
     * Get days remaining until deletion
     */
    fun getDaysRemaining(): Int {
        val now = Date()
        if (scheduledDeletionDate.before(now)) return 0
        
        val diffMillis = scheduledDeletionDate.time - now.time
        return TimeUnit.MILLISECONDS.toDays(diffMillis).toInt()
    }
    
    /**
     * Check if deletion is imminent (less than 7 days)
     */
    fun isImminent(): Boolean {
        return getDaysRemaining() <= 7
    }
}

/**
 * Account deletion status enum
 */
enum class DeletionStatus {
    PENDING,
    CANCELLED,
    COMPLETED
}
