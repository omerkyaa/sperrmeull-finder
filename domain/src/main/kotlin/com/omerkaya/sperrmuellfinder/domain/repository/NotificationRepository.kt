package com.omerkaya.sperrmuellfinder.domain.repository

import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.Notification
import kotlinx.coroutines.flow.Flow

/**
 * 🔔 NOTIFICATION REPOSITORY INTERFACE - SperrmüllFinder
 * Real-time Firebase notifications with FCM push support
 * Rules.md compliant - Clean Architecture domain interface
 */
interface NotificationRepository {

    /**
     * Get real-time stream of notifications for a user (newest first)
     */
    fun getNotifications(userId: String, limit: Long = 20): Flow<Result<List<Notification>>>

    /**
     * Get unread notification count for badge display
     */
    fun getUnreadCount(userId: String): Flow<Result<Int>>

    /**
     * Mark a notification as read
     */
    suspend fun markAsRead(notificationId: String): Result<Unit>

    /**
     * Mark all notifications as read for a user
     */
    suspend fun markAllAsRead(userId: String): Result<Unit>

    /**
     * Create a like notification
     */
    suspend fun createLikeNotification(
        postId: String,
        postOwnerId: String,
        likerUserId: String,
        likerUserName: String
    ): Result<Unit>

    /**
     * Create a comment notification
     */
    suspend fun createCommentNotification(
        postId: String,
        postOwnerId: String,
        commenterUserId: String,
        commenterUserName: String,
        commentText: String
    ): Result<Unit>

    /**
     * Create a follow notification
     */
    suspend fun createFollowNotification(
        followedUserId: String,
        followerUserId: String,
        followerUserName: String
    ): Result<Unit>

    /**
     * Create a system notification
     */
    suspend fun createSystemNotification(
        userId: String,
        title: String,
        body: String,
        deeplink: String? = null
    ): Result<Unit>

    /**
     * Delete a notification
     */
    suspend fun deleteNotification(notificationId: String): Result<Unit>

    /**
     * Delete all notifications for a user
     */
    suspend fun deleteAllNotifications(userId: String): Result<Unit>
}