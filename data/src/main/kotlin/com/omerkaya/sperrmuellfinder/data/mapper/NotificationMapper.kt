package com.omerkaya.sperrmuellfinder.data.mapper

import com.omerkaya.sperrmuellfinder.data.dto.NotificationDto
import com.omerkaya.sperrmuellfinder.domain.model.Notification
import com.omerkaya.sperrmuellfinder.domain.model.NotificationType

/**
 * 🔔 NOTIFICATION MAPPER - SperrmüllFinder
 * Maps between Notification DTO and Domain model
 * Rules.md compliant - Clean Architecture data mapping
 */
object NotificationMapper {
    
    /**
     * Convert NotificationDto to Notification domain model
     */
    fun NotificationDto.toDomain(): Notification {
        return Notification(
            id = this.notifId,
            userId = this.userId,
            type = NotificationType.fromString(this.type),
            title = this.title,
            body = this.body,
            deeplink = this.deeplink,
            isRead = this.isRead,
            data = this.data,
            createdAt = this.createdAt,
            
            // Actor data
            actorUserId = this.actorUserId,
            actorUsername = this.actorUsername,
            actorPhotoUrl = this.actorPhotoUrl,
            actorLevel = this.actorLevel,
            actorIsPremium = this.actorIsPremium,
            
            // Post data
            postId = this.postId,
            postImageUrl = this.postImageUrl,
            postDescription = this.postDescription,
            
            // Comment data
            commentId = this.commentId,
            commentText = this.commentText
        )
    }
    
    /**
     * Convert Notification domain model to NotificationDto
     */
    fun Notification.toDto(): NotificationDto {
        return NotificationDto(
            notifId = this.id,
            userId = this.userId,
            type = this.type.value,
            title = this.title,
            body = this.body,
            deeplink = this.deeplink,
            isRead = this.isRead,
            data = this.data,
            createdAt = this.createdAt,
            
            // Actor data
            actorUserId = this.actorUserId,
            actorUsername = this.actorUsername,
            actorPhotoUrl = this.actorPhotoUrl,
            actorLevel = this.actorLevel,
            actorIsPremium = this.actorIsPremium,
            
            // Post data
            postId = this.postId,
            postImageUrl = this.postImageUrl,
            postDescription = this.postDescription,
            
            // Comment data
            commentId = this.commentId,
            commentText = this.commentText
        )
    }
    
    /**
     * Convert list of NotificationDto to list of Notification domain models
     */
    fun List<NotificationDto>.toDomain(): List<Notification> {
        return this.map { it.toDomain() }
    }
}