package com.omerkaya.sperrmuellfinder.data.notification

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppNotificationService @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    suspend fun notifyLike(
        toUserId: String,
        fromUserId: String,
        fromUserName: String,
        postId: String,
        fromUserPhotoUrl: String? = null,
        fromUserNickname: String? = null,
        messageOverride: String? = null,
        likeCount: Int? = null
    ) {
        writeNotification(
            toUserId = toUserId,
            fromUserId = fromUserId,
            type = "like",
            postId = postId,
            commentId = null,
            title = "New Like",
            message = messageOverride ?: "$fromUserName liked your post",
            deepLink = "${FirestoreConstants.DeepLinks.POST_DETAIL_PREFIX}$postId",
            actorUserName = fromUserName,
            actorPhotoUrl = fromUserPhotoUrl,
            meta = mapOf(
                "fromUserName" to fromUserName,
                "fromUserNickname" to (fromUserNickname ?: ""),
                "likeCount" to (likeCount ?: 1)
            )
        )
    }

    suspend fun notifyComment(
        toUserId: String,
        fromUserId: String,
        fromUserName: String,
        postId: String,
        commentId: String?,
        commentText: String? = null,
        fromUserPhotoUrl: String? = null,
        fromUserNickname: String? = null
    ) {
        writeNotification(
            toUserId = toUserId,
            fromUserId = fromUserId,
            type = "comment",
            postId = postId,
            commentId = commentId,
            title = "New Comment",
            message = "$fromUserName commented on your post",
            deepLink = "${FirestoreConstants.DeepLinks.POST_DETAIL_PREFIX}$postId",
            actorUserName = fromUserName,
            actorPhotoUrl = fromUserPhotoUrl,
            commentText = commentText,
            meta = mapOf(
                "fromUserName" to fromUserName,
                "fromUserNickname" to (fromUserNickname ?: "")
            )
        )
    }

    suspend fun notifyFollow(
        toUserId: String,
        fromUserId: String,
        fromUserName: String,
        fromUserPhotoUrl: String? = null,
        fromUserNickname: String? = null
    ) {
        writeNotification(
            toUserId = toUserId,
            fromUserId = fromUserId,
            type = "follow",
            postId = null,
            commentId = null,
            title = "New Follower",
            message = "$fromUserName started following you",
            deepLink = "${FirestoreConstants.DeepLinks.USER_PROFILE_PREFIX}$fromUserId",
            actorUserName = fromUserName,
            actorPhotoUrl = fromUserPhotoUrl,
            meta = mapOf(
                "fromUserName" to fromUserName,
                "fromUserNickname" to (fromUserNickname ?: "")
            )
        )
    }

    suspend fun writeNotification(
        toUserId: String,
        fromUserId: String?,
        type: String,
        postId: String?,
        commentId: String?,
        title: String,
        message: String,
        deepLink: String?,
        actorUserName: String? = null,
        actorPhotoUrl: String? = null,
        commentText: String? = null,
        meta: Map<String, Any> = emptyMap()
    ) {
        val notificationRef = firestore
            .collection(FirestoreConstants.Collections.NOTIFICATIONS)
            .document(toUserId)
            .collection(FirestoreConstants.SUBCOLLECTION_USER_NOTIFICATIONS)
            .document()

        val payload = hashMapOf<String, Any>(
            "id" to notificationRef.id,
            "toUserId" to toUserId,
            "type" to type,
            "title" to title,
            "message" to message,
            "isRead" to false,
            "createdAt" to Timestamp.now(),
            "meta" to meta
        )

        fromUserId?.let { payload["fromUserId"] = it }
        actorUserName?.takeIf { it.isNotBlank() }?.let { payload["actorUsername"] = it }
        actorPhotoUrl?.takeIf { it.isNotBlank() }?.let { payload["actorPhotoUrl"] = it }
        postId?.let { payload["postId"] = it }
        commentId?.let { payload["commentId"] = it }
        commentText?.takeIf { it.isNotBlank() }?.let { payload["commentText"] = it }
        deepLink?.let { payload["deepLink"] = it }

        notificationRef.set(payload).await()
    }
}
