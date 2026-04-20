package com.omerkaya.sperrmuellfinder.domain.usecase.admin

import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.repository.AdminRepository
import javax.inject.Inject

/**
 * Use case for sending admin notifications
 * Rules.md compliant - Clean Architecture domain layer
 */
class SendAdminNotificationUseCase @Inject constructor(
    private val adminRepository: AdminRepository,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "SendAdminNotificationUseCase"
    }
    
    /**
     * Send notification to a specific user
     * @param userId Target user
     * @param title Notification title
     * @param body Notification body
     * @param deeplink Optional deeplink
     */
    suspend fun sendToUser(
        userId: String,
        title: String,
        body: String,
        deeplink: String? = null
    ): Result<Unit> {
        logger.d(TAG, "Sending notification to user $userId")
        
        if (title.isBlank() || body.isBlank()) {
            return Result.Error(Exception("Title and body cannot be blank"))
        }
        
        // Use broadcast notification with "specific" targetType
        return when (val result = adminRepository.sendBroadcastNotification(
            targetType = "specific",
            targetIds = listOf(userId),
            title = title,
            body = body,
            deeplink = deeplink
        )) {
            is Result.Success -> {
                logger.d(TAG, "Notification sent to user $userId: ${result.data} recipient(s)")
                Result.Success(Unit)
            }
            is Result.Error -> {
                logger.e(TAG, "Failed to send notification to user $userId", result.exception)
                Result.Error(result.exception)
            }
            Result.Loading -> Result.Loading
        }
    }
    
    /**
     * Send broadcast notification to all users
     * @param title Notification title
     * @param body Notification body
     * @param deeplink Optional deeplink
     */
    suspend fun sendBroadcast(
        title: String,
        body: String,
        deeplink: String? = null
    ): Result<Unit> {
        logger.d(TAG, "Sending broadcast notification")
        
        if (title.isBlank() || body.isBlank()) {
            return Result.Error(Exception("Title and body cannot be blank"))
        }
        
        return when (val result = adminRepository.sendBroadcastNotification(
            targetType = "all",
            targetIds = null,
            title = title,
            body = body,
            deeplink = deeplink
        )) {
            is Result.Success -> {
                logger.d(TAG, "Broadcast notification sent to ${result.data} users")
                Result.Success(Unit)
            }
            is Result.Error -> {
                logger.e(TAG, "Failed to send broadcast notification", result.exception)
                Result.Error(result.exception)
            }
            Result.Loading -> Result.Loading
        }
    }
    
    /**
     * Send notification to premium users only
     * @param title Notification title
     * @param body Notification body
     * @param deeplink Optional deeplink
     */
    suspend fun sendToPremiumUsers(
        title: String,
        body: String,
        deeplink: String? = null
    ): Result<Unit> {
        logger.d(TAG, "Sending notification to premium users")
        
        if (title.isBlank() || body.isBlank()) {
            return Result.Error(Exception("Title and body cannot be blank"))
        }
        
        return when (val result = adminRepository.sendBroadcastNotification(
            targetType = "premium",
            targetIds = null,
            title = title,
            body = body,
            deeplink = deeplink
        )) {
            is Result.Success -> {
                logger.d(TAG, "Notification sent to ${result.data} premium users")
                Result.Success(Unit)
            }
            is Result.Error -> {
                logger.e(TAG, "Failed to send notification to premium users", result.exception)
                Result.Error(result.exception)
            }
            Result.Loading -> Result.Loading
        }
    }
    
    /**
     * Send notification to users in a specific city
     * @param cityName Target city
     * @param title Notification title
     * @param body Notification body
     * @param deeplink Optional deeplink
     */
    suspend fun sendToCity(
        cityName: String,
        title: String,
        body: String,
        deeplink: String? = null
    ): Result<Unit> {
        logger.d(TAG, "Sending notification to users in $cityName")
        
        if (title.isBlank() || body.isBlank()) {
            return Result.Error(Exception("Title and body cannot be blank"))
        }
        
        return when (val result = adminRepository.sendBroadcastNotification(
            targetType = "city",
            targetIds = listOf(cityName),
            title = title,
            body = body,
            deeplink = deeplink
        )) {
            is Result.Success -> {
                logger.d(TAG, "Notification sent to ${result.data} users in $cityName")
                Result.Success(Unit)
            }
            is Result.Error -> {
                logger.e(TAG, "Failed to send notification to users in $cityName", result.exception)
                Result.Error(result.exception)
            }
            Result.Loading -> Result.Loading
        }
    }
    
    /**
     * Send notification to specific list of users
     * @param userIds Target user IDs
     * @param title Notification title
     * @param body Notification body
     * @param deeplink Optional deeplink
     */
    suspend fun sendToSpecificUsers(
        userIds: List<String>,
        title: String,
        body: String,
        deeplink: String? = null
    ): Result<Unit> {
        logger.d(TAG, "Sending notification to ${userIds.size} specific users")
        
        if (title.isBlank() || body.isBlank()) {
            return Result.Error(Exception("Title and body cannot be blank"))
        }
        
        if (userIds.isEmpty()) {
            return Result.Error(Exception("User list cannot be empty"))
        }
        
        return when (val result = adminRepository.sendBroadcastNotification(
            targetType = "specific",
            targetIds = userIds,
            title = title,
            body = body,
            deeplink = deeplink
        )) {
            is Result.Success -> {
                logger.d(TAG, "Notification sent to ${result.data} specific users")
                Result.Success(Unit)
            }
            is Result.Error -> {
                logger.e(TAG, "Failed to send notification to specific users", result.exception)
                Result.Error(result.exception)
            }
            Result.Loading -> Result.Loading
        }
    }
}
