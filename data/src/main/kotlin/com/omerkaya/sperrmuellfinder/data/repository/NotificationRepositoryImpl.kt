package com.omerkaya.sperrmuellfinder.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.snapshots
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.data.notification.AppNotificationService
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants
import com.omerkaya.sperrmuellfinder.domain.model.Notification
import com.omerkaya.sperrmuellfinder.domain.model.NotificationType
import com.omerkaya.sperrmuellfinder.domain.repository.NotificationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val appNotificationService: AppNotificationService
) : NotificationRepository {

    companion object {
        private const val LIKE_NOTIFICATION_BATCH_SIZE = 5
        private const val MAX_NOTIFICATION_LIST_LIMIT = 200L
    }

    override fun getNotifications(userId: String, limit: Long): Flow<Result<List<Notification>>> = callbackFlow {
        var registration: ListenerRegistration? = null
        try {
            registration = firestore
                .collection(FirestoreConstants.Collections.NOTIFICATIONS)
                .document(userId)
                .collection(FirestoreConstants.SUBCOLLECTION_USER_NOTIFICATIONS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.coerceAtMost(MAX_NOTIFICATION_LIST_LIMIT))
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.Error(error))
                        return@addSnapshotListener
                    }
                    val mapped = snapshot?.documents.orEmpty().mapNotNull { doc ->
                        try {
                            val createdAt = doc.getTimestamp("createdAt")?.toDate()
                                ?: doc.getTimestamp("created_at")?.toDate()
                                ?: Date()
                            val typeValue = doc.getString("type") ?: NotificationType.SYSTEM.value
                            val fromUserId = doc.getString("fromUserId").orEmpty()
                            val meta = (doc.get("meta") as? Map<String, Any>) ?: emptyMap()
                            val fromUserName = (doc.getString("actorUsername")
                                ?: meta["fromUserName"] as? String).orEmpty()
                            val actorPhotoUrl = doc.getString("actorPhotoUrl")
                                ?: (meta["fromUserPhotoUrl"] as? String)
                            Notification(
                                id = doc.getString("id") ?: doc.id,
                                userId = doc.getString("toUserId") ?: userId,
                                type = NotificationType.fromString(typeValue),
                                title = doc.getString("title").orEmpty(),
                                body = doc.getString("message")
                                    ?: doc.getString("body")
                                    ?: "",
                                deeplink = doc.getString("deepLink").orEmpty(),
                                isRead = doc.getBoolean("isRead") ?: false,
                                data = meta,
                                createdAt = createdAt,
                                actorUserId = fromUserId,
                                actorUsername = fromUserName,
                                actorPhotoUrl = actorPhotoUrl,
                                actorLevel = 1,
                                actorIsPremium = false,
                                postId = doc.getString("postId"),
                                postImageUrl = doc.getString("imageUrl"),
                                postDescription = null,
                                commentId = doc.getString("commentId"),
                                commentText = doc.getString("commentText")
                                    ?: (meta["commentText"] as? String)
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        val hydrated = hydrateActorProfiles(mapped)
                        trySend(Result.Success(hydrated))
                    }
                }
        } catch (e: Exception) {
            trySend(Result.Error(e))
        }

        awaitClose { registration?.remove() }
    }

    override fun getUnreadCount(userId: String): Flow<Result<Int>> = callbackFlow {
        var registration: ListenerRegistration? = null
        try {
            registration = firestore
                .collection(FirestoreConstants.Collections.NOTIFICATIONS)
                .document(userId)
                .collection(FirestoreConstants.SUBCOLLECTION_USER_NOTIFICATIONS)
                .whereEqualTo("isRead", false)
                .limit(MAX_NOTIFICATION_LIST_LIMIT)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.Error(error))
                        return@addSnapshotListener
                    }
                    trySend(Result.Success(snapshot?.size() ?: 0))
                }
        } catch (e: Exception) {
            trySend(Result.Error(e))
        }
        awaitClose { registration?.remove() }
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> {
        val currentUserId = auth.currentUser?.uid ?: return Result.Error(Exception("User not authenticated"))
        return try {
            firestore
                .collection(FirestoreConstants.Collections.NOTIFICATIONS)
                .document(currentUserId)
                .collection(FirestoreConstants.SUBCOLLECTION_USER_NOTIFICATIONS)
                .document(notificationId)
                .update("isRead", true)
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun markAllAsRead(userId: String): Result<Unit> {
        val currentUserId = auth.currentUser?.uid ?: return Result.Error(Exception("User not authenticated"))
        if (currentUserId != userId) return Result.Error(Exception("Unauthorized"))
        return try {
            do {
                val unreadSnapshot = firestore
                    .collection(FirestoreConstants.Collections.NOTIFICATIONS)
                    .document(userId)
                    .collection(FirestoreConstants.SUBCOLLECTION_USER_NOTIFICATIONS)
                    .whereEqualTo("isRead", false)
                    .limit(MAX_NOTIFICATION_LIST_LIMIT)
                    .get()
                    .await()

                if (unreadSnapshot.isEmpty) break

                firestore.runBatch { batch ->
                    unreadSnapshot.documents.forEach { doc ->
                        batch.update(doc.reference, "isRead", true)
                    }
                }.await()
            } while (true)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun createLikeNotification(
        postId: String,
        postOwnerId: String,
        likerUserId: String,
        likerUserName: String
    ): Result<Unit> {
        if (postOwnerId == likerUserId) return Result.Success(Unit)
        // Firestore rules make notification creation server-only (create: false).
        // Avoid client-side writes that cause PERMISSION_DENIED noise.
        return Result.Success(Unit)
    }

    override suspend fun createCommentNotification(
        postId: String,
        postOwnerId: String,
        commenterUserId: String,
        commenterUserName: String,
        commentText: String
    ): Result<Unit> {
        if (postOwnerId == commenterUserId) return Result.Success(Unit)
        return Result.Success(Unit)
    }

    override suspend fun createFollowNotification(
        followedUserId: String,
        followerUserId: String,
        followerUserName: String
    ): Result<Unit> {
        if (followedUserId == followerUserId) return Result.Success(Unit)
        return try {
            val notificationId = firestore
                .collection(FirestoreConstants.Collections.NOTIFICATIONS)
                .document(followedUserId)
                .collection(FirestoreConstants.SUBCOLLECTION_USER_NOTIFICATIONS)
                .document().id

            val notificationData = hashMapOf(
                "id" to notificationId,
                "toUserId" to followedUserId,
                "fromUserId" to followerUserId,
                "type" to NotificationType.FOLLOW.value,
                "title" to followerUserName,
                "message" to followerUserName,
                "isRead" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )

            firestore
                .collection(FirestoreConstants.Collections.NOTIFICATIONS)
                .document(followedUserId)
                .collection(FirestoreConstants.SUBCOLLECTION_USER_NOTIFICATIONS)
                .document(notificationId)
                .set(notificationData)
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Success(Unit) // Non-critical: don't fail follow on notification error
        }
    }

    override suspend fun createSystemNotification(
        userId: String,
        title: String,
        body: String,
        deeplink: String?
    ): Result<Unit> {
        return Result.Success(Unit)
    }

    override suspend fun deleteNotification(notificationId: String): Result<Unit> {
        val currentUserId = auth.currentUser?.uid ?: return Result.Error(Exception("User not authenticated"))
        return try {
            firestore
                .collection(FirestoreConstants.Collections.NOTIFICATIONS)
                .document(currentUserId)
                .collection(FirestoreConstants.SUBCOLLECTION_USER_NOTIFICATIONS)
                .document(notificationId)
                .delete()
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteAllNotifications(userId: String): Result<Unit> {
        val currentUserId = auth.currentUser?.uid ?: return Result.Error(Exception("User not authenticated"))
        if (currentUserId != userId) return Result.Error(Exception("Unauthorized"))
        return try {
            do {
                val docs = firestore
                    .collection(FirestoreConstants.Collections.NOTIFICATIONS)
                    .document(userId)
                    .collection(FirestoreConstants.SUBCOLLECTION_USER_NOTIFICATIONS)
                    .limit(MAX_NOTIFICATION_LIST_LIMIT)
                    .get()
                    .await()

                if (docs.isEmpty) break

                firestore.runBatch { batch ->
                    docs.documents.forEach { batch.delete(it.reference) }
                }.await()
            } while (true)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun getCurrentLikeCount(postId: String): Int {
        val postDoc = firestore
            .collection(FirestoreConstants.Collections.POSTS)
            .document(postId)
            .get()
            .await()
        return postDoc.getLong(FirestoreConstants.Post.LIKES_COUNT)?.toInt() ?: 0
    }

    private fun shouldCreateLikeNotification(likeCount: Int): Boolean {
        return likeCount == 1 || (likeCount > 1 && likeCount % LIKE_NOTIFICATION_BATCH_SIZE == 0)
    }

    private suspend fun getActorProfile(userId: String, fallbackName: String): ActorProfile {
        return try {
            val doc = firestore
                .collection(FirestoreConstants.Collections.USERS_PUBLIC)
                .document(userId)
                .get()
                .await()

            ActorProfile(
                displayName = doc.getString(FirestoreConstants.User.DISPLAY_NAME)
                    ?: doc.getString("username")
                    ?: fallbackName,
                nickname = doc.getString(FirestoreConstants.User.NICKNAME) ?: "",
                photoUrl = doc.getString(FirestoreConstants.User.PHOTO_URL)
                    ?: doc.getString("photoUrl")
            )
        } catch (_: Exception) {
            ActorProfile(displayName = fallbackName, nickname = "", photoUrl = null)
        }
    }

    private data class ActorProfile(
        val displayName: String,
        val nickname: String,
        val photoUrl: String?
    )

    private suspend fun hydrateActorProfiles(notifications: List<Notification>): List<Notification> {
        if (notifications.isEmpty()) return notifications

        val actorIds = notifications
            .mapNotNull { it.resolvedActorUserId }
            .filter { it.isNotBlank() }
            .distinct()
        if (actorIds.isEmpty()) return notifications

        val actorById = mutableMapOf<String, ActorProfile>()
        actorIds.chunked(10).forEach { chunk ->
            runCatching {
                firestore.collection(FirestoreConstants.Collections.USERS_PUBLIC)
                    .whereIn(FieldPath.documentId(), chunk)
                    .limit(chunk.size.toLong())
                    .get()
                    .await()
            }.getOrNull()?.documents?.forEach { doc ->
                val uid = doc.id
                val profile = ActorProfile(
                    displayName = doc.getString(FirestoreConstants.User.DISPLAY_NAME)
                        ?: doc.getString("displayName")
                        ?: doc.getString("username")
                        ?: doc.getString(FirestoreConstants.User.NICKNAME)
                        ?: "",
                    nickname = doc.getString(FirestoreConstants.User.NICKNAME)
                        ?: doc.getString("nickname")
                        ?: "",
                    photoUrl = doc.getString(FirestoreConstants.User.PHOTO_URL)
                        ?: doc.getString("photoUrl")
                )
                actorById[uid] = profile
            }
        }

        return notifications.map { notification ->
            val actorId = notification.resolvedActorUserId
            val profile = actorId?.let { actorById[it] }
            if (profile == null) {
                notification
            } else {
                val newData = notification.data.toMutableMap().apply {
                    if (!containsKey("fromUserNickname") && profile.nickname.isNotBlank()) {
                        put("fromUserNickname", profile.nickname)
                    }
                }
                notification.copy(
                    actorUsername = profile.displayName.takeIf { it.isNotBlank() } ?: notification.actorUsername,
                    actorPhotoUrl = profile.photoUrl ?: notification.actorPhotoUrl,
                    data = newData
                )
            }
        }
    }
}
