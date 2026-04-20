package com.omerkaya.sperrmuellfinder.data.util

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.omerkaya.sperrmuellfinder.data.mapper.toUser
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.model.UserFavorites
import kotlinx.coroutines.tasks.await

object PublicUserResolver {

    suspend fun resolveUsersByIds(
        firestore: FirebaseFirestore,
        userIds: Collection<String>
    ): Map<String, User> {
        val ids = userIds.filter { it.isNotBlank() }.distinct()
        if (ids.isEmpty()) return emptyMap()

        val resolved = mutableMapOf<String, User>()

        // Fast path: users_public by document id.
        ids.chunked(10).forEach { chunk ->
            runCatching {
                firestore.collection(FirestoreConstants.Collections.USERS_PUBLIC)
                    .whereIn(FieldPath.documentId(), chunk)
                    .limit(chunk.size.toLong())
                    .get()
                    .await()
            }.getOrNull()?.documents?.forEach { doc ->
                runCatching { doc.toUser() }.getOrNull()?.let { user ->
                    resolved[user.uid] = user
                    if (!resolved.containsKey(doc.id)) {
                        resolved[doc.id] = user
                    }
                }
            }
        }

        // Fallback 1: users_public where userId field is legacy-mapped.
        ids.filterNot { resolved.containsKey(it) }.forEach { unresolvedId ->
            runCatching {
                firestore.collection(FirestoreConstants.Collections.USERS_PUBLIC)
                    .whereEqualTo(FirestoreConstants.User.USER_ID, unresolvedId)
                    .limit(1)
                    .get()
                    .await()
            }.getOrNull()?.documents?.firstOrNull()?.let { doc ->
                runCatching { doc.toUser() }.getOrNull()?.let { user ->
                    resolved[unresolvedId] = user
                    resolved[user.uid] = user
                    resolved[doc.id] = user
                }
            }
        }

        // Fallback 1b: additional legacy uid keys on users_public.
        ids.filterNot { resolved.containsKey(it) }.forEach { unresolvedId ->
            val legacyDoc = runCatching {
                firestore.collection(FirestoreConstants.Collections.USERS_PUBLIC)
                    .whereEqualTo("uid", unresolvedId)
                    .limit(1)
                    .get()
                    .await()
                    .documents
                    .firstOrNull()
            }.getOrNull() ?: runCatching {
                firestore.collection(FirestoreConstants.Collections.USERS_PUBLIC)
                    .whereEqualTo("userid", unresolvedId)
                    .limit(1)
                    .get()
                    .await()
                    .documents
                    .firstOrNull()
            }.getOrNull()

            legacyDoc?.let { doc ->
                runCatching { doc.toUser() }.getOrNull()?.let { user ->
                    resolved[unresolvedId] = user
                    resolved[user.uid] = user
                    resolved[doc.id] = user
                }
            }
        }

        // Fallback 2: derive minimal profile from latest post owner fields.
        ids.filterNot { resolved.containsKey(it) }.forEach { unresolvedId ->
            val postDoc = runCatching {
                val bySnake = firestore.collection(FirestoreConstants.Collections.POSTS)
                    .whereEqualTo(FirestoreConstants.Post.OWNER_ID, unresolvedId)
                    .limit(5)
                    .get()
                    .await()
                    .documents
                val byCamel = firestore.collection(FirestoreConstants.Collections.POSTS)
                    .whereEqualTo("ownerId", unresolvedId)
                    .limit(5)
                    .get()
                    .await()
                    .documents
                (bySnake + byCamel)
                    .distinctBy { it.id }
                    .maxByOrNull {
                        it.getTimestamp(FirestoreConstants.Post.CREATED_AT)?.toDate()?.time
                            ?: it.getTimestamp("createdAt")?.toDate()?.time
                            ?: 0L
                    }
            }.getOrNull()

            if (postDoc != null) {
                resolved[unresolvedId] = User(
                    uid = unresolvedId,
                    email = "",
                    displayName = postDoc.getString("owner_display_name")
                        ?: postDoc.getString("ownerDisplayName")
                        ?: postDoc.getString("ownername")
                        ?: "User",
                    nickname = postDoc.getString("owner_nickname")
                        ?: postDoc.getString("ownerNickname")
                        ?: "",
                    photoUrl = postDoc.getString("owner_photo_url")
                        ?: postDoc.getString("ownerPhotoUrl"),
                    city = postDoc.getString("city"),
                    dob = null,
                    gender = null,
                    xp = 0,
                    level = (postDoc.getLong("owner_level") ?: postDoc.getLong("ownerLevel") ?: 0L).toInt(),
                    honesty = 100,
                    isPremium = postDoc.getBoolean("is_owner_premium")
                        ?: postDoc.getBoolean("isOwnerPremium")
                        ?: false,
                    premiumUntil = null,
                    badges = emptyList(),
                    favorites = UserFavorites(),
                    fcmToken = null,
                    deviceTokens = emptyList(),
                    deviceLang = "",
                    deviceModel = "",
                    deviceOs = "",
                    frameLevel = 0,
                    followersCount = 0,
                    followingCount = 0,
                    createdAt = null,
                    updatedAt = null,
                    lastLoginAt = null
                )
            }
        }

        return resolved
    }
}
