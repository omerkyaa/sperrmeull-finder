package com.omerkaya.sperrmuellfinder.domain.usecase.notifications

import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 🔔 GET UNREAD COUNT USE CASE - SperrmüllFinder
 * Real-time unread notification count for badge display
 * Rules.md compliant - Clean Architecture use case
 */
class GetUnreadCountUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {

    /**
     * Get real-time unread notification count for a user
     * 
     * @param userId The user ID to get unread count for
     * @return Flow of Result containing unread count
     */
    operator fun invoke(userId: String): Flow<Result<Int>> {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        return notificationRepository.getUnreadCount(userId)
    }
}