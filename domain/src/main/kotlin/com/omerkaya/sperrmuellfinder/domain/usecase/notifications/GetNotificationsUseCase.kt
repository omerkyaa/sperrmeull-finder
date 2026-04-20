package com.omerkaya.sperrmuellfinder.domain.usecase.notifications

import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.Notification
import com.omerkaya.sperrmuellfinder.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 🔔 GET NOTIFICATIONS USE CASE - SperrmüllFinder
 * Real-time notifications stream (newest first)
 * Rules.md compliant - Clean Architecture use case
 */
class GetNotificationsUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {

    /**
     * Get real-time stream of notifications for a user
     * 
     * @param userId The user ID to get notifications for
     * @return Flow of Result containing list of notifications (newest first)
     */
    operator fun invoke(userId: String, limit: Long = 20): Flow<Result<List<Notification>>> {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        return notificationRepository.getNotifications(userId, limit)
    }
}
