package com.omerkaya.sperrmuellfinder.domain.usecase.notifications

import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.repository.NotificationRepository
import javax.inject.Inject

/**
 * 🔔 MARK NOTIFICATION READ USE CASE - SperrmüllFinder
 * Mark single or all notifications as read
 * Rules.md compliant - Clean Architecture use case
 */
class MarkNotificationReadUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {

    /**
     * Mark a single notification as read
     * 
     * @param notificationId The notification ID to mark as read
     * @return Result indicating success or failure
     */
    suspend fun markSingle(notificationId: String): Result<Unit> {
        require(notificationId.isNotBlank()) { "Notification ID cannot be blank" }
        return notificationRepository.markAsRead(notificationId)
    }

    /**
     * Mark all notifications as read for a user
     * 
     * @param userId The user ID to mark all notifications as read
     * @return Result indicating success or failure
     */
    suspend fun markAll(userId: String): Result<Unit> {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        return notificationRepository.markAllAsRead(userId)
    }
}