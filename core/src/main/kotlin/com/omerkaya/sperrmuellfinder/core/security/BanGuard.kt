package com.omerkaya.sperrmuellfinder.core.security

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

sealed class BanGuardResult {
    object Allowed : BanGuardResult()
    data class Banned(val reason: String?) : BanGuardResult()
}

class BanGuard(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun checkAndEnforceBan(): BanGuardResult {
        val uid = auth.currentUser?.uid ?: return BanGuardResult.Allowed

        return try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            if (!snapshot.exists()) return BanGuardResult.Allowed

            val isBanned = snapshot.getBoolean("isBanned")
                ?: snapshot.getBoolean("isbanned")
                ?: false

            if (!isBanned) return BanGuardResult.Allowed

            val banUntil = parseBanUntil(snapshot.get("banUntil") ?: snapshot.get("banuntil"))
            val isStillBanned = banUntil == null || Date().before(banUntil)

            if (!isStillBanned) {
                BanGuardResult.Allowed
            } else {
                val reason = snapshot.getString("banReason") ?: snapshot.getString("banreason")
                auth.signOut()
                BanGuardResult.Banned(reason = reason)
            }
        } catch (_: Exception) {
            // Fail open to avoid locking out users on transient backend issues.
            BanGuardResult.Allowed
        }
    }

    private fun parseBanUntil(rawValue: Any?): Date? {
        return when (rawValue) {
            is Timestamp -> rawValue.toDate()
            is Date -> rawValue
            is Number -> Date(rawValue.toLong())
            else -> null
        }
    }
}
