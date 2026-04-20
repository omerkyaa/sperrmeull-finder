package com.omerkaya.sperrmuellfinder.data.manager

import android.os.Build
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.data.dto.user.UserDto
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants.COLLECTION_USERS
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles user document initialization and synchronization in Firestore
 * Rules.md compliant - Idempotent user bootstrapping
 */
@Singleton
class UserBootstrapper @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "UserBootstrapper"
        private const val INITIAL_XP = 100
        private const val INITIAL_LEVEL = 1
        private const val INITIAL_HONESTY = 100
    }

    private fun safeDeviceModel(): String {
        return try {
            Build.MODEL ?: ""
        } catch (_: Throwable) {
            ""
        }
    }

    private fun safeDeviceOs(): String {
        return try {
            Build.VERSION.RELEASE ?: ""
        } catch (_: Throwable) {
            ""
        }
    }

    /**
     * Ensures user document exists in Firestore with proper initialization
     * Idempotent operation - safe to call multiple times
     */
    suspend fun ensureUserDocument(
        authUser: FirebaseUser,
        fcmTokenProvider: () -> String?
    ): UserDto {
        val uid = authUser.uid
        logger.d(TAG, "Ensuring user document exists: $uid")

        return try {
            // Try to get existing document
            val docRef = firestore.collection(COLLECTION_USERS).document(uid)
            val snapshot = docRef.get().await()

            if (snapshot.exists()) {
                // Document exists - patch if needed
                val existingUser = snapshot.toObject(UserDto::class.java)!!
                patchExistingUser(docRef, existingUser, authUser, fcmTokenProvider())
            } else {
                // Document doesn't exist - create new
                createNewUser(docRef, authUser, fcmTokenProvider())
            }
        } catch (e: Exception) {
            logger.e(TAG, "Error ensuring user document", e)
            throw e
        }
    }

    private suspend fun createNewUser(
        docRef: DocumentReference,
        authUser: FirebaseUser,
        fcmToken: String?
    ): UserDto {
        logger.d(TAG, "Creating new user document: ${authUser.uid}")

        val newUser = UserDto(
            uid = authUser.uid,
            email = authUser.email ?: "",
            displayName = authUser.displayName ?: "",
            photoUrl = authUser.photoUrl?.toString() ?: "",
            xp = INITIAL_XP,
            level = INITIAL_LEVEL,
            honesty = INITIAL_HONESTY,
            isPremium = false,
            premiumUntil = null,
            city = "",
            deviceLang = Locale.getDefault().language.let { if (it == "de") "de" else "en" },
            deviceModel = safeDeviceModel(),
            deviceOs = safeDeviceOs(),
            fcmToken = fcmToken,
            createdAt = FieldValue.serverTimestamp(),
            updatedAt = FieldValue.serverTimestamp(),
            lastLoginAt = FieldValue.serverTimestamp()
        )

        docRef.set(newUser.toMap()).await()
        logger.i(TAG, "New user document created successfully")

        return newUser
    }

    private suspend fun patchExistingUser(
        docRef: DocumentReference,
        existingUser: UserDto,
        authUser: FirebaseUser,
        fcmToken: String?
    ): UserDto {
        logger.d(TAG, "Patching existing user document: ${authUser.uid}")

        // Only update fields that need updating
        val updates = mutableMapOf<String, Any?>()

        // Sync basic info if changed
        if (existingUser.email != authUser.email && !authUser.email.isNullOrBlank()) {
            updates["email"] = authUser.email!!
        }
        if (existingUser.displayName != authUser.displayName && !authUser.displayName.isNullOrBlank()) {
            updates["displayName"] = authUser.displayName!!
        }
        if (existingUser.photoUrl != authUser.photoUrl?.toString() && !authUser.photoUrl?.toString().isNullOrBlank()) {
            updates["photoUrl"] = authUser.photoUrl.toString()
        }

        // Update FCM token if changed
        if (fcmToken != null && existingUser.fcmToken != fcmToken) {
            updates["fcmToken"] = fcmToken
        }

        // Always update timestamps
        updates["updatedAt"] = FieldValue.serverTimestamp()
        updates["lastLoginAt"] = FieldValue.serverTimestamp()

        // Update device info if changed
        val currentLang = Locale.getDefault().language.let { if (it == "de") "de" else "en" }
        if (existingUser.deviceLang != currentLang) {
            updates["deviceLang"] = currentLang
        }
        val currentDeviceModel = safeDeviceModel()
        if (existingUser.deviceModel != currentDeviceModel) {
            updates["deviceModel"] = currentDeviceModel
        }
        val currentDeviceOs = safeDeviceOs()
        if (existingUser.deviceOs != currentDeviceOs) {
            updates["deviceOs"] = currentDeviceOs
        }

        // Apply updates if any
        if (updates.isNotEmpty()) {
            docRef.set(updates, SetOptions.merge()).await()
            logger.i(TAG, "User document patched successfully")
        } else {
            logger.d(TAG, "No updates needed for user document")
        }

        return existingUser.copy(
            email = updates["email"]?.toString() ?: existingUser.email,
            displayName = updates["displayName"]?.toString() ?: existingUser.displayName,
            photoUrl = updates["photoUrl"]?.toString() ?: existingUser.photoUrl,
            fcmToken = updates["fcmToken"]?.toString() ?: existingUser.fcmToken,
            deviceLang = updates["deviceLang"]?.toString() ?: existingUser.deviceLang,
            deviceModel = updates["deviceModel"]?.toString() ?: existingUser.deviceModel,
            deviceOs = updates["deviceOs"]?.toString() ?: existingUser.deviceOs
        )
    }
}
