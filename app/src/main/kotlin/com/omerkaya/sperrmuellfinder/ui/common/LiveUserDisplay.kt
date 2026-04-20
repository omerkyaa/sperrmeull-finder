package com.omerkaya.sperrmuellfinder.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants

data class LiveUserDisplay(
    val displayName: String?,
    val photoUrl: String?,
    val nickname: String?
)

private fun String?.normalizedOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

private fun firstNonBlank(vararg values: String?): String? =
    values.firstNotNullOfOrNull { it.normalizedOrNull() }

@Composable
fun rememberLiveUserDisplay(
    userId: String,
    fallbackDisplayName: String?,
    fallbackPhotoUrl: String?,
    fallbackNickname: String? = null
): LiveUserDisplay {
    val normalizedFallbackName = firstNonBlank(fallbackDisplayName, fallbackNickname, "User")
    val normalizedFallbackPhoto = fallbackPhotoUrl.normalizedOrNull()
    val normalizedFallbackNickname = fallbackNickname.normalizedOrNull()

    var display by remember(userId, fallbackDisplayName, fallbackPhotoUrl, fallbackNickname) {
        mutableStateOf(
            LiveUserDisplay(
                displayName = normalizedFallbackName,
                photoUrl = normalizedFallbackPhoto,
                nickname = normalizedFallbackNickname
            )
        )
    }

    DisposableEffect(userId, normalizedFallbackName, normalizedFallbackPhoto, normalizedFallbackNickname) {
        if (userId.isBlank()) {
            display = LiveUserDisplay(
                displayName = normalizedFallbackName,
                photoUrl = normalizedFallbackPhoto,
                nickname = normalizedFallbackNickname
            )
            onDispose { }
        } else {
            fun applySnapshot(snapshot: com.google.firebase.firestore.DocumentSnapshot?) {
                if (snapshot == null || !snapshot.exists()) return

                val freshDisplayName = firstNonBlank(
                    snapshot.getString("displayname"),
                    snapshot.getString("displayName"),
                    snapshot.getString("display_name"),
                    snapshot.getString("username"),
                    snapshot.getString("userName"),
                    snapshot.getString("nickname"),
                    snapshot.getString("nickName")
                )
                val freshPhotoUrl = firstNonBlank(
                    snapshot.getString("photourl"),
                    snapshot.getString("photoUrl"),
                    snapshot.getString("photoURL"),
                    snapshot.getString("profilePhotoUrl"),
                    snapshot.getString("avatarUrl")
                )
                val freshNickname = firstNonBlank(
                    snapshot.getString("nickname"),
                    snapshot.getString("nickName"),
                    snapshot.getString("username"),
                    snapshot.getString("userName")
                )

                display = LiveUserDisplay(
                    displayName = firstNonBlank(
                        freshDisplayName,
                        freshNickname,
                        display.displayName,
                        display.nickname,
                        normalizedFallbackName
                    ),
                    photoUrl = firstNonBlank(
                        freshPhotoUrl,
                        display.photoUrl,
                        normalizedFallbackPhoto
                    ),
                    nickname = firstNonBlank(
                        freshNickname,
                        display.nickname,
                        normalizedFallbackNickname
                    )
                )
            }

            val firestore = FirebaseFirestore.getInstance()
            val publicRegistration = firestore
                .collection(FirestoreConstants.Collections.USERS_PUBLIC)
                .document(userId)
                .addSnapshotListener { snapshot, _ ->
                    applySnapshot(snapshot)
                }

            // Legacy compatibility: some old users_public docs may not use uid as doc id.
            val publicByUserIdRegistration = firestore
                .collection(FirestoreConstants.Collections.USERS_PUBLIC)
                .whereEqualTo(FirestoreConstants.User.USER_ID, userId)
                .limit(1)
                .addSnapshotListener { snapshot, _ ->
                    applySnapshot(snapshot?.documents?.firstOrNull())
                }
            val publicByUidRegistration = firestore
                .collection(FirestoreConstants.Collections.USERS_PUBLIC)
                .whereEqualTo("uid", userId)
                .limit(1)
                .addSnapshotListener { snapshot, _ ->
                    applySnapshot(snapshot?.documents?.firstOrNull())
                }
            val publicByUserIdLegacyRegistration = firestore
                .collection(FirestoreConstants.Collections.USERS_PUBLIC)
                .whereEqualTo("userid", userId)
                .limit(1)
                .addSnapshotListener { snapshot, _ ->
                    applySnapshot(snapshot?.documents?.firstOrNull())
                }

            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            val privateRegistration = if (currentUserId == userId) {
                firestore
                    .collection(FirestoreConstants.Collections.USERS)
                    .document(userId)
                    .addSnapshotListener { snapshot, _ ->
                        applySnapshot(snapshot)
                    }
            } else {
                null
            }

            onDispose {
                publicRegistration.remove()
                publicByUserIdRegistration.remove()
                publicByUidRegistration.remove()
                publicByUserIdLegacyRegistration.remove()
                privateRegistration?.remove()
            }
        }
    }

    return display
}
