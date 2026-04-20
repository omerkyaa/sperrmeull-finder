package com.omerkaya.sperrmuellfinder.data.dto

import com.google.firebase.firestore.PropertyName
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants
import java.util.Date

/**
 * 🔔 NOTIFICATION DTO - SperrmüllFinder
 * Firebase Firestore data transfer object for notifications
 * Rules.md compliant - Field mapping with @PropertyName
 */
data class NotificationDto(
    @PropertyName(FirestoreConstants.Notification.NOTIF_ID)
    val notifId: String = "",
    
    @PropertyName(FirestoreConstants.Notification.USER_ID)
    val userId: String = "",
    
    @PropertyName(FirestoreConstants.Notification.TYPE)
    val type: String = "",
    
    @PropertyName(FirestoreConstants.Notification.TITLE)
    val title: String = "",
    
    @PropertyName(FirestoreConstants.Notification.BODY)
    val body: String = "",
    
    @PropertyName(FirestoreConstants.Notification.DEEPLINK)
    val deeplink: String = "",
    
    @PropertyName(FirestoreConstants.Notification.IS_READ)
    val isRead: Boolean = false,
    
    @PropertyName(FirestoreConstants.Notification.DATA)
    val data: Map<String, Any> = emptyMap(),
    
    @PropertyName(FirestoreConstants.Notification.CREATED_AT)
    val createdAt: Date = Date(),
    
    // Actor (person who triggered the notification)
    @PropertyName(FirestoreConstants.Notification.ACTOR_USER_ID)
    val actorUserId: String = "",
    
    @PropertyName(FirestoreConstants.Notification.ACTOR_USERNAME)
    val actorUsername: String = "",
    
    @PropertyName(FirestoreConstants.Notification.ACTOR_PHOTO_URL)
    val actorPhotoUrl: String? = null,
    
    @PropertyName(FirestoreConstants.Notification.ACTOR_LEVEL)
    val actorLevel: Int = 1,
    
    @PropertyName(FirestoreConstants.Notification.ACTOR_IS_PREMIUM)
    val actorIsPremium: Boolean = false,
    
    // Post data (for like/comment notifications)
    @PropertyName(FirestoreConstants.Notification.POST_ID)
    val postId: String? = null,
    
    @PropertyName(FirestoreConstants.Notification.POST_IMAGE_URL)
    val postImageUrl: String? = null,
    
    @PropertyName(FirestoreConstants.Notification.POST_DESCRIPTION)
    val postDescription: String? = null,
    
    // Comment data (for comment notifications)
    @PropertyName(FirestoreConstants.Notification.COMMENT_ID)
    val commentId: String? = null,
    
    @PropertyName(FirestoreConstants.Notification.COMMENT_TEXT)
    val commentText: String? = null
)
