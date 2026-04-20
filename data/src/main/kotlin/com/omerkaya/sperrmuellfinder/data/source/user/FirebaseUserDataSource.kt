package com.omerkaya.sperrmuellfinder.data.source.user

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.data.dto.user.UserDto
import com.omerkaya.sperrmuellfinder.data.dto.user.UserFavoritesDto
import com.omerkaya.sperrmuellfinder.data.dto.user.XpTransactionDto
import com.omerkaya.sperrmuellfinder.data.dto.user.HonestyTransactionDto
import com.omerkaya.sperrmuellfinder.data.dto.user.LeaderboardEntryDto
import com.omerkaya.sperrmuellfinder.data.manager.UserBootstrapper
import com.google.firebase.firestore.SetOptions
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants.COLLECTION_USERS
import com.omerkaya.sperrmuellfinder.domain.model.UserFavorites
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase implementation of user data source
 * Rules.md compliant - Clean Architecture data layer
 */
@Singleton
class FirebaseUserDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val userBootstrapper: UserBootstrapper,
    private val logger: Logger
) {

    companion object {
        private const val TAG = "FirebaseUserDataSource"
    }

    /**
     * Get current user as Flow
     * Uses proper document ID based on auth.uid
     */
    fun getCurrentUser(): Flow<UserDto?> = callbackFlow {
        val currentUserId = firebaseAuth.currentUser?.uid
            ?: run {
                logger.w(TAG, "❌ No authenticated user found")
                trySend(null)
                return@callbackFlow
            }

        logger.d(TAG, "🔄 Starting user listener for: $currentUserId")

        val listener = firestore.collection(COLLECTION_USERS)
            .document(currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logger.e(TAG, "❌ Error observing user", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    try {
                        val userDto = snapshot.toObject(UserDto::class.java)
                        logger.d(TAG, "✅ User data updated: ${userDto?.displayName}, PhotoURL: ${userDto?.photoUrl}")
                        
                        trySend(userDto)
                    } catch (e: Exception) {
                        logger.e(TAG, "❌ Error parsing user data", e)
                        trySend(null)
                    }
                } else {
                    logger.w(TAG, "❌ User document not found, creating from Firebase Auth...")
                    // 🎯 AUTO-CREATE USER: If Firestore document doesn't exist, create from Firebase Auth
                    CoroutineScope(Dispatchers.IO).launch {
                        createUserFromFirebaseAuth()?.let { newUserDto ->
                            trySend(newUserDto)
                        } ?: trySend(null)
                    }
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get user by ID
     * Uses proper document ID based on provided uid
     */
    suspend fun getUserById(userId: String): Result<UserDto?> {
        return try {
            logger.d(TAG, "🔍 Getting user by ID: $userId")
            val isCurrentUser = userId == firebaseAuth.currentUser?.uid
            if (isCurrentUser) {
                val snapshot = firestore.collection(COLLECTION_USERS)
                    .document(userId)
                    .get()
                    .await()

                if (snapshot.exists()) {
                    val userDto = snapshot.toObject(UserDto::class.java)
                    logger.d(TAG, "✅ Current user found: ${userDto?.displayName}, PhotoURL: ${userDto?.photoUrl}")
                    val enhancedUserDto = if (userDto?.photoUrl.isNullOrBlank()) {
                        syncFirebaseAuthDataToUser(userDto)
                    } else {
                        userDto
                    }
                    return Result.Success(enhancedUserDto)
                }
            }

            val publicSnapshot = getPublicUserSnapshotByUid(userId)
            if (!publicSnapshot.exists()) {
                logger.w(TAG, "❌ Public user profile not found: $userId, trying post fallback")
                return Result.Success(buildPublicFallbackFromPosts(userId))
            }

            val userDto = mapPublicUserToDto(userId, publicSnapshot)
            logger.d(TAG, "✅ Public user found: ${userDto.displayName}, PhotoURL: ${userDto.photoUrl}")
            Result.Success(userDto)
        } catch (e: Exception) {
            logger.e(TAG, "❌ Error getting user by ID", e)
            Result.Error(e)
        }
    }
    
    /**
     * Get user by ID as Flow for real-time updates
     * Uses proper document ID based on userId parameter
     */
    fun getUserByIdFlow(userId: String): Flow<UserDto?> = callbackFlow {
        logger.d(TAG, "🔄 Starting real-time user listener for: $userId")
        val isCurrentUser = userId == firebaseAuth.currentUser?.uid
        val userCollection = if (isCurrentUser) COLLECTION_USERS else FirestoreConstants.Collections.USERS_PUBLIC
        var emittedDocPath: String? = null

        fun emitPublicSnapshot(snapshot: DocumentSnapshot?) {
            if (snapshot == null || !snapshot.exists()) return
            emittedDocPath = snapshot.reference.path
            val userDto = mapPublicUserToDto(userId, snapshot)
            logger.d(TAG, "✅ Real-time public user data updated: ${userDto.displayName}, PhotoURL: ${userDto.photoUrl}")
            trySend(userDto)
        }

        val listener = firestore.collection(userCollection)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logger.e(TAG, "❌ Error observing user by ID", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (isCurrentUser) {
                    if (snapshot == null || !snapshot.exists()) {
                        logger.w(TAG, "❌ Current user doc not found for ID: $userId in $userCollection, trying post fallback")
                        CoroutineScope(Dispatchers.IO).launch {
                            trySend(buildPublicFallbackFromPosts(userId))
                        }
                        return@addSnapshotListener
                    }
                    try {
                        val userDto = snapshot.toObject(UserDto::class.java)
                        logger.d(TAG, "✅ Real-time current user data updated: ${userDto?.displayName}, PhotoURL: ${userDto?.photoUrl}")
                        trySend(userDto)
                    } catch (e: Exception) {
                        logger.e(TAG, "❌ Error parsing current user data by ID", e)
                        trySend(null)
                    }
                    return@addSnapshotListener
                }

                // Non-current user:
                // 1) direct document path
                if (snapshot != null && snapshot.exists()) {
                    try {
                        emitPublicSnapshot(snapshot)
                    } catch (e: Exception) {
                        logger.e(TAG, "❌ Error parsing public user doc", e)
                    }
                    return@addSnapshotListener
                }

                // 2) fallback by legacy uid fields
                CoroutineScope(Dispatchers.IO).launch {
                    val fallbackDoc = queryPublicUserSnapshotByUidFields(userId)
                    if (fallbackDoc != null && fallbackDoc.exists()) {
                        emitPublicSnapshot(fallbackDoc)
                    } else {
                        logger.w(TAG, "❌ User document not found for ID: $userId in $userCollection, trying post fallback")
                        trySend(buildPublicFallbackFromPosts(userId))
                    }
                }
            }

        // Legacy field query listeners removed: only one direct document listener per user
        // to avoid N×4 open connections when viewing comments/followers lists.
        // Fallback for missing documents is handled via one-time queries in the main listener.

        awaitClose {
            listener.remove()
        }
    }

    private suspend fun buildPublicFallbackFromPosts(userId: String): UserDto? {
        return runCatching {
            val postsByOwnerId = firestore.collection(FirestoreConstants.Collections.POSTS)
                .whereEqualTo("ownerid", userId)
                .limit(10)
                .get()
                .await()
                .documents
            val postsByOwnerIdCamel = firestore.collection(FirestoreConstants.Collections.POSTS)
                .whereEqualTo("ownerId", userId)
                .limit(10)
                .get()
                .await()
                .documents

            val doc = (postsByOwnerId + postsByOwnerIdCamel)
                .distinctBy { it.id }
                .maxByOrNull {
                    it.getTimestamp("created_at")?.toDate()?.time
                        ?: it.getTimestamp("createdAt")?.toDate()?.time
                        ?: 0L
                } ?: return@runCatching UserDto(
                uid = userId,
                displayName = "User",
                nickname = "User",
                photoUrl = null,
                city = "",
                level = 0,
                isPremium = false,
                createdAt = null,
                updatedAt = null
            )
            val displayName = doc.getString("owner_display_name")
                ?: doc.getString("ownerDisplayName")
                ?: doc.getString("ownername")
                ?: "User"
            val photoUrl = doc.getString("owner_photo_url")
                ?: doc.getString("ownerPhotoUrl")
            val level = (doc.getLong("owner_level") ?: doc.getLong("ownerLevel") ?: 0L).toInt()
            val isPremium = doc.getBoolean("is_owner_premium")
                ?: doc.getBoolean("isOwnerPremium")
                ?: false

            UserDto(
                uid = userId,
                displayName = displayName,
                nickname = displayName,
                photoUrl = photoUrl,
                city = doc.getString("city") ?: "",
                level = level,
                isPremium = isPremium,
                createdAt = null,
                updatedAt = null
            )
        }.onFailure { e ->
            logger.w(TAG, "Fallback user load from posts failed for $userId", e)
        }.getOrNull() ?: UserDto(
            uid = userId,
            displayName = "User",
            nickname = "User",
            photoUrl = null,
            city = "",
            level = 0,
            isPremium = false,
            createdAt = null,
            updatedAt = null
        )
    }
    
    /**
     * 🎯 PERFECT FIRESTORE DATA SYNC - Sync Firebase Auth data to Firestore user
     * Ensures profile photo and display name are always up-to-date
     */
    private suspend fun syncFirebaseAuthDataToUser(userDto: UserDto?): UserDto? {
        return try {
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null && userDto != null) {
                var needsUpdate = false
                var updatedDto = userDto
                
                // 🚫 CLEAN PROBLEMATIC CONTENT URIs: Check if photoUrl is a problematic content:// URI
                if (!userDto.photoUrl.isNullOrBlank() && isProblematicContentUri(userDto.photoUrl!!)) {
                    logger.w(TAG, "🚫 Found problematic content URI in Firestore: ${userDto.photoUrl!!.take(50)}...")
                    
                    // Try to sync from Firebase Auth if available
                    val authPhotoUrl = firebaseUser.photoUrl?.toString()
                    if (!authPhotoUrl.isNullOrBlank() && !isProblematicContentUri(authPhotoUrl)) {
                        updatedDto = updatedDto.copy(photoUrl = authPhotoUrl)
                        needsUpdate = true
                        logger.d(TAG, "🔄 Replacing problematic URI with Firebase Auth photoURL: $authPhotoUrl")
                    } else {
                        // Clear the problematic URI
                        updatedDto = updatedDto.copy(photoUrl = null)
                        needsUpdate = true
                        logger.d(TAG, "🧹 Clearing problematic content URI from Firestore")
                    }
                }
                
                // Sync photo URL if missing in Firestore but available in Firebase Auth
                if (userDto.photoUrl.isNullOrBlank() && !firebaseUser.photoUrl?.toString().isNullOrBlank()) {
                    val authPhotoUrl = firebaseUser.photoUrl.toString()
                    if (!isProblematicContentUri(authPhotoUrl)) {
                        updatedDto = updatedDto.copy(photoUrl = authPhotoUrl)
                        needsUpdate = true
                        logger.d(TAG, "🔄 Syncing photoURL from Firebase Auth: $authPhotoUrl")
                    }
                }
                
                // Sync display name if missing in Firestore but available in Firebase Auth
                if (userDto.displayName.isNullOrBlank() && !firebaseUser.displayName.isNullOrBlank()) {
                    updatedDto = updatedDto.copy(displayName = firebaseUser.displayName!!)
                    needsUpdate = true
                    logger.d(TAG, "🔄 Syncing displayName from Firebase Auth: ${firebaseUser.displayName}")
                }
                
                // Update Firestore if needed
                if (needsUpdate) {
                    val userRef = firestore.collection(COLLECTION_USERS).document(firebaseUser.uid)
                    userRef.set(updatedDto, SetOptions.merge()).await()
                    logger.i(TAG, "✅ Firebase Auth data synced to Firestore successfully")
                }
                
                updatedDto
            } else {
                userDto
            }
        } catch (e: Exception) {
            logger.e(TAG, "❌ Error syncing Firebase Auth data", e)
            userDto // Return original on error
        }
    }
    
    /**
     * Check if a URI is a problematic content:// URI that causes SecurityException
     */
    private fun isProblematicContentUri(uri: String): Boolean {
        return uri.startsWith("content://media/picker") ||
               uri.startsWith("content://com.android.providers.media.photopicker") ||
               (uri.startsWith("content://") && 
                !uri.startsWith("https://") &&
                !uri.startsWith("http://"))
    }

    private fun mapPublicUserToDto(userId: String, snapshot: DocumentSnapshot): UserDto {
        val resolvedUid = snapshot.getString(FirestoreConstants.User.USER_ID)
            ?: snapshot.getString("uid")
            ?: snapshot.getString("userid")
            ?: userId
        val displayName = snapshot.getString("displayname")
            ?: snapshot.getString("displayName")
            ?: snapshot.getString("username")
            ?: snapshot.getString("nickname")
            ?: ""
        val photoUrl = snapshot.getString("photourl")
            ?: snapshot.getString("photoUrl")
        val level = (snapshot.getLong("level") ?: 0L).toInt()
        val isPremium = snapshot.getBoolean("ispremium")
            ?: snapshot.getBoolean("isPremium")
            ?: false
        val frameLevel = (snapshot.getLong("frame_level")
            ?: snapshot.getLong("premiumFrameLevel")
            ?: 0L).toInt()
        val badges = (snapshot.get("badges") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

        return UserDto(
            uid = resolvedUid,
            displayName = displayName,
            nickname = snapshot.getString("nickname") ?: displayName,
            photoUrl = photoUrl,
            city = snapshot.getString("city") ?: "",
            level = level,
            isPremium = isPremium,
            badges = badges,
            frameLevel = frameLevel,
            createdAt = snapshot.getTimestamp("createdAt") ?: snapshot.getTimestamp("created_at"),
            updatedAt = snapshot.getTimestamp("updatedAt") ?: snapshot.getTimestamp("updated_at"),
            followersCount = (snapshot.getLong("followers_count") ?: snapshot.getLong("followersCount") ?: 0L).toInt(),
            followingCount = (snapshot.getLong("following_count") ?: snapshot.getLong("followingCount") ?: 0L).toInt()
        )
    }

    private suspend fun getPublicUserSnapshotByUid(userId: String): DocumentSnapshot {
        val direct = firestore.collection(FirestoreConstants.Collections.USERS_PUBLIC)
            .document(userId)
            .get()
            .await()
        if (direct.exists()) return direct
        return queryPublicUserSnapshotByUidFields(userId) ?: direct
    }

    private suspend fun queryPublicUserSnapshotByUidFields(userId: String): DocumentSnapshot? {
        val byUserId = firestore.collection(FirestoreConstants.Collections.USERS_PUBLIC)
            .whereEqualTo(FirestoreConstants.User.USER_ID, userId)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
        if (byUserId != null) return byUserId

        val byUid = firestore.collection(FirestoreConstants.Collections.USERS_PUBLIC)
            .whereEqualTo("uid", userId)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
        if (byUid != null) return byUid

        return firestore.collection(FirestoreConstants.Collections.USERS_PUBLIC)
            .whereEqualTo("userid", userId)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
    }
    
    /**
     * 🎯 AUTO-CREATE USER FROM FIREBASE AUTH
     * Creates Firestore user document from Firebase Auth data when missing
     */
    private suspend fun createUserFromFirebaseAuth(): UserDto? {
        return try {
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                logger.d(TAG, "🔄 Creating user from Firebase Auth: ${firebaseUser.displayName}")
                
                val newUserDto = UserDto(
                    uid = firebaseUser.uid,
                    displayName = firebaseUser.displayName ?: "User",
                    email = firebaseUser.email ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString(),
                    city = "",
                    dateOfBirth = null,
                    isPremium = false,
                    premiumUntil = null,
                    xp = 100, // Starting XP
                    level = 1, // Starting level
                    honesty = 100, // Starting honesty
                    badges = emptyList(),
                    favorites = UserFavoritesDto(),
                    createdAt = com.google.firebase.Timestamp.now(),
                    updatedAt = com.google.firebase.Timestamp.now(),
                    deviceTokens = emptyList(),
                    frameLevel = 0
                )
                
                // Save to Firestore
                val userRef = firestore.collection(COLLECTION_USERS).document(firebaseUser.uid)
                userRef.set(newUserDto, SetOptions.merge()).await()
                syncToSplitCollections(firebaseUser.uid, newUserDto)
                
                logger.i(TAG, "✅ User created from Firebase Auth successfully: ${newUserDto.displayName}")
                newUserDto
            } else {
                logger.w(TAG, "❌ No Firebase Auth user available")
                null
            }
        } catch (e: Exception) {
            logger.e(TAG, "❌ Error creating user from Firebase Auth", e)
            null
        }
    }

    /**
     * Create or update user profile
     * Uses proper document ID based on auth.uid
     */
    suspend fun createOrUpdateUser(userDto: UserDto): Result<Unit> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))

            logger.d(TAG, "Creating/updating user: $currentUserId")

            val userRef = firestore.collection(COLLECTION_USERS).document(currentUserId)

            // Use merge to preserve existing data
            userRef.set(userDto, SetOptions.merge()).await()
            syncToSplitCollections(currentUserId, userDto)

            logger.i(TAG, "User created/updated successfully: ${userDto.displayName}")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Error creating/updating user", e)
            Result.Error(e)
        }
    }

    /**
     * Update user profile fields
     * Uses proper document ID based on auth.uid
     */
    suspend fun updateUserProfile(
        displayName: String? = null,
        firstName: String? = null,
        lastName: String? = null,
        city: String? = null,
        photoUrl: String? = null
    ): Result<Unit> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))

            logger.d(TAG, "Updating user profile: $currentUserId")

            val updates = mutableMapOf<String, Any>()
            displayName?.let { updates[FirestoreConstants.FIELD_DISPLAY_NAME] = it }
            firstName?.let { updates[FirestoreConstants.FIELD_FIRST_NAME] = it }
            lastName?.let { updates[FirestoreConstants.FIELD_LAST_NAME] = it }
            city?.let { updates[FirestoreConstants.FIELD_CITY] = it }
            photoUrl?.let { updates[FirestoreConstants.FIELD_PHOTO_URL] = it }
            updates[FirestoreConstants.FIELD_UPDATED_AT] = FieldValue.serverTimestamp()

            firestore.collection(FirestoreConstants.COLLECTION_USERS)
                .document(currentUserId)
                .update(updates)
                .await()
            val publicUpdates = mutableMapOf<String, Any>()
            publicUpdates["userId"] = currentUserId
            publicUpdates["uid"] = currentUserId
            publicUpdates["userid"] = currentUserId
            displayName?.let {
                publicUpdates["username"] = it
                publicUpdates["usernameLower"] = it.trim().lowercase()
                publicUpdates["displayname"] = it
                publicUpdates["displayName"] = it
                publicUpdates["nickname"] = it
            }
            city?.let { publicUpdates[FirestoreConstants.FIELD_CITY] = it }
            photoUrl?.let {
                publicUpdates["photoUrl"] = it
                publicUpdates["photourl"] = it
            }
            publicUpdates["updatedAt"] = FieldValue.serverTimestamp()
            firestore.collection(FirestoreConstants.Collections.USERS_PUBLIC)
                .document(currentUserId)
                .set(publicUpdates, SetOptions.merge())
                .await()

            // Legacy compatibility: update older users_public docs keyed by random ids.
            val legacyPublicDocs = mutableListOf<DocumentSnapshot>()
            firestore.collection(FirestoreConstants.Collections.USERS_PUBLIC)
                .whereEqualTo(FirestoreConstants.User.USER_ID, currentUserId)
                .limit(50)
                .get()
                .await()
                .documents
                .let { legacyPublicDocs.addAll(it) }
            firestore.collection(FirestoreConstants.Collections.USERS_PUBLIC)
                .whereEqualTo("uid", currentUserId)
                .limit(50)
                .get()
                .await()
                .documents
                .let { legacyPublicDocs.addAll(it) }
            firestore.collection(FirestoreConstants.Collections.USERS_PUBLIC)
                .whereEqualTo("userid", currentUserId)
                .limit(50)
                .get()
                .await()
                .documents
                .let { legacyPublicDocs.addAll(it) }

            legacyPublicDocs
                .distinctBy { it.reference.path }
                .filter { it.id != currentUserId }
                .forEach { doc ->
                    doc.reference.set(publicUpdates, SetOptions.merge()).await()
                }

            logger.i(TAG, "User profile updated successfully")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Error updating user profile", e)
            Result.Error(e)
        }
    }

    /**
     * Update user XP
     * Uses proper document ID based on auth.uid
     */
    suspend fun updateUserXp(xpDelta: Int, reason: String): Result<UserDto> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))

            logger.d(TAG, "Updating user XP: $currentUserId, delta: $xpDelta")

            val userRef = firestore.collection(COLLECTION_USERS).document(currentUserId)

            val updatedUser = firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentUser = snapshot.toObject(UserDto::class.java)
                    ?: throw Exception("User not found")

                val newXp = currentUser.xp + xpDelta
                val newLevel = calculateLevel(newXp)

                transaction.update(userRef, mapOf(
                    "xp" to newXp,
                    "level" to newLevel,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))

                // Create XP transaction record
                val xpTransactionRef = firestore.collection(FirestoreConstants.COLLECTION_XP_TRANSACTIONS)
                    .document(currentUserId)
                    .collection("transactions")
                    .document()

                transaction.set(xpTransactionRef, mapOf(
                    "userId" to currentUserId,
                    "delta" to xpDelta,
                    "reason" to reason,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "xpBefore" to currentUser.xp,
                    "xpAfter" to newXp,
                    "levelBefore" to currentUser.level,
                    "levelAfter" to newLevel
                ))

                currentUser.copy(xp = newXp, level = newLevel)
            }.await()
            
            Result.Success(updatedUser)

        } catch (e: Exception) {
            logger.e(TAG, "Error updating user XP", e)
            Result.Error(e)
        }
    }

    /**
     * Update user honesty score
     * Uses proper document ID based on auth.uid
     */
    suspend fun updateUserHonesty(honestyDelta: Int, reason: String): Result<UserDto> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))

            logger.d(TAG, "Updating user honesty: $currentUserId, delta: $honestyDelta")

            val userRef = firestore.collection(COLLECTION_USERS).document(currentUserId)

            val updatedUser = firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentUser = snapshot.toObject(UserDto::class.java)
                    ?: throw Exception("User not found")

                val newHonesty = (currentUser.honesty + honestyDelta).coerceIn(0, 100)

                transaction.update(userRef, mapOf(
                    "honesty" to newHonesty,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))

                // Create honesty transaction record
                val honestyTransactionRef = firestore.collection(FirestoreConstants.COLLECTION_HONESTY_TRANSACTIONS)
                    .document(currentUserId)
                    .collection("transactions")
                    .document()

                transaction.set(honestyTransactionRef, mapOf(
                    "userId" to currentUserId,
                    "delta" to honestyDelta,
                    "reason" to reason,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "honestyBefore" to currentUser.honesty,
                    "honestyAfter" to newHonesty
                ))

                currentUser.copy(honesty = newHonesty)
            }.await()
            
            Result.Success(updatedUser)

        } catch (e: Exception) {
            logger.e(TAG, "Error updating user honesty", e)
            Result.Error(e)
        }
    }

    /**
     * Update premium status
     * Uses proper document ID based on auth.uid
     */
    suspend fun updatePremiumStatus(isPremium: Boolean, premiumUntil: Date?): Result<Unit> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))

            logger.d(TAG, "Updating premium status: $currentUserId, isPremium: $isPremium")

            val updates = mutableMapOf<String, Any?>(
                "ispremium" to isPremium,
                "premiumuntil" to premiumUntil,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            firestore.collection(COLLECTION_USERS)
                .document(currentUserId)
                .update(updates)
                .await()
            firestore.collection(FirestoreConstants.Collections.USERS_PRIVATE)
                .document(currentUserId)
                .set(updates, SetOptions.merge())
                .await()
            firestore.collection(FirestoreConstants.Collections.USERS_PUBLIC)
                .document(currentUserId)
                .set(mapOf("isPremium" to isPremium, "ispremium" to isPremium, "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
                .await()

            logger.i(TAG, "Premium status updated successfully")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Error updating premium status", e)
            Result.Error(e)
        }
    }

    /**
     * Add badge to user
     * Uses proper document ID based on auth.uid
     */
    suspend fun addBadge(badgeId: String): Result<Unit> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))

            logger.d(TAG, "Adding badge: $badgeId to user: $currentUserId")

            firestore.collection(COLLECTION_USERS)
                .document(currentUserId)
                .update("badges", FieldValue.arrayUnion(badgeId))
                .await()

            logger.i(TAG, "Badge added successfully")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Error adding badge", e)
            Result.Error(e)
        }
    }

    /**
     * Remove badge from user
     * Uses proper document ID based on auth.uid
     */
    suspend fun removeBadge(badgeId: String): Result<Unit> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))

            logger.d(TAG, "Removing badge: $badgeId from user: $currentUserId")

            firestore.collection(COLLECTION_USERS)
                .document(currentUserId)
                .update("badges", FieldValue.arrayRemove(badgeId))
                .await()

            logger.i(TAG, "Badge removed successfully")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Error removing badge", e)
            Result.Error(e)
        }
    }

    /**
     * Update user favorites
     * Uses proper document ID based on auth.uid
     */
    suspend fun updateFavorites(favorites: UserFavorites): Result<Unit> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))

            logger.d(TAG, "Updating favorites for user: $currentUserId")

            val updates = mapOf(
                "favorites.regions" to favorites.regions,
                "favorites.categories" to favorites.categories,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            firestore.collection(COLLECTION_USERS)
                .document(currentUserId)
                .update(updates)
                .await()

            logger.i(TAG, "Favorites updated successfully")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Error updating favorites", e)
            Result.Error(e)
        }
    }

    /**
     * Update device tokens
     * Uses proper document ID based on auth.uid
     */
    suspend fun updateDeviceTokens(tokens: List<String>): Result<Unit> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))

            logger.d(TAG, "Updating device tokens for user: $currentUserId")

            firestore.collection(COLLECTION_USERS)
                .document(currentUserId)
                .update(mapOf(
                    "deviceTokens" to tokens,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))
                .await()
            firestore.collection(FirestoreConstants.Collections.USERS_PRIVATE)
                .document(currentUserId)
                .set(
                    mapOf(
                        "deviceTokens" to tokens,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                .await()

            logger.i(TAG, "Device tokens updated successfully")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Error updating device tokens", e)
            Result.Error(e)
        }
    }

    /**
     * Get user posts count
     * Uses proper document ID based on provided uid
     */
    suspend fun getUserPostsCount(userId: String): Result<Int> {
        return try {
            logger.d(TAG, "Getting posts count for user: $userId")
            // Count only visible user posts (ACTIVE + ARCHIVED). REMOVED posts are excluded.
            val activeCount = firestore.collection(FirestoreConstants.COLLECTION_POSTS)
                .whereEqualTo(FirestoreConstants.FIELD_OWNER_ID, userId)
                .whereEqualTo(FirestoreConstants.FIELD_STATUS, "active")
                .count()
                .get(AggregateSource.SERVER)
                .await()
                .count

            val archivedCount = firestore.collection(FirestoreConstants.COLLECTION_POSTS)
                .whereEqualTo(FirestoreConstants.FIELD_OWNER_ID, userId)
                .whereEqualTo(FirestoreConstants.FIELD_STATUS, "archived")
                .count()
                .get(AggregateSource.SERVER)
                .await()
                .count

            val totalVisibleCount = (activeCount + archivedCount).toInt()
            logger.d(TAG, "Visible posts count: $totalVisibleCount (active=$activeCount, archived=$archivedCount)")
            Result.Success(totalVisibleCount)
        } catch (e: Exception) {
            logger.e(TAG, "Error getting posts count", e)
            Result.Error(e)
        }
    }

    /**
     * Get followers count
     * Uses proper document ID based on provided uid
     */
    suspend fun getFollowersCount(userId: String): Result<Int> {
        return try {
            logger.d(TAG, "Getting followers count for user: $userId")

            val publicUserDoc = firestore.collection(FirestoreConstants.USERS_PUBLIC)
                .document(userId)
                .get()
                .await()
            val publicCount = publicUserDoc.getLong(FirestoreConstants.User.FOLLOWERS_COUNT)?.toInt()
            if (publicCount != null) {
                logger.d(TAG, "Followers count from users_public: $publicCount")
                return Result.Success(publicCount)
            }

            val fallbackCount = firestore.collection(FirestoreConstants.COLLECTION_FOLLOWERS)
                .document(userId)
                .collection("followers")
                .limit(200)
                .count()
                .get(AggregateSource.SERVER)
                .await()
                .count

            logger.d(TAG, "Followers count fallback: $fallbackCount")
            Result.Success(fallbackCount.toInt())
        } catch (e: Exception) {
            logger.e(TAG, "Error getting followers count", e)
            Result.Error(e)
        }
    }

    /**
     * Get following count
     * Uses proper document ID based on provided uid
     */
    suspend fun getFollowingCount(userId: String): Result<Int> {
        return try {
            logger.d(TAG, "Getting following count for user: $userId")

            val publicUserDoc = firestore.collection(FirestoreConstants.USERS_PUBLIC)
                .document(userId)
                .get()
                .await()
            val publicCount = publicUserDoc.getLong(FirestoreConstants.User.FOLLOWING_COUNT)?.toInt()
            if (publicCount != null) {
                logger.d(TAG, "Following count from users_public: $publicCount")
                return Result.Success(publicCount)
            }

            val fallbackCount = firestore.collection(FirestoreConstants.COLLECTION_FOLLOWERS)
                .document(userId)
                .collection("following")
                .limit(200)
                .count()
                .get(AggregateSource.SERVER)
                .await()
                .count

            logger.d(TAG, "Following count fallback: $fallbackCount")
            Result.Success(fallbackCount.toInt())
        } catch (e: Exception) {
            logger.e(TAG, "Error getting following count", e)
            Result.Error(e)
        }
    }

    /**
     * Check if user can perform action
     * Uses proper document ID based on auth.uid
     */
    suspend fun canUserPerformAction(action: String): Result<Boolean> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))

            logger.d(TAG, "Checking if user $currentUserId can perform action: $action")

            val snapshot = firestore.collection(COLLECTION_USERS)
                .document(currentUserId)
                .get()
                .await()

            if (!snapshot.exists()) {
                return Result.Error(Exception("User not found"))
            }

            val userDto = snapshot.toObject(UserDto::class.java)!!
            val canPerform = when (action) {
                "POST" -> userDto.honesty >= 30
                "COMMENT" -> userDto.honesty >= 30
                "VOTE" -> userDto.honesty >= 30
                else -> true
            }

            logger.d(TAG, "Can perform $action: $canPerform")
            Result.Success(canPerform)
        } catch (e: Exception) {
            logger.e(TAG, "Error checking action permission", e)
            Result.Error(e)
        }
    }

    /**
     * Get XP transactions
     * Uses proper document ID based on auth.uid
     */
    suspend fun getXpTransactions(limit: Int): Result<List<XpTransactionDto>> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))

            logger.d(TAG, "Getting XP transactions for user: $currentUserId")

            val transactions = firestore.collection(FirestoreConstants.COLLECTION_XP_TRANSACTIONS)
                .document(currentUserId)
                .collection("transactions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
                .toObjects(XpTransactionDto::class.java)

            logger.d(TAG, "Retrieved ${transactions.size} XP transactions")
            Result.Success(transactions)
        } catch (e: Exception) {
            logger.e(TAG, "Error getting XP transactions", e)
            Result.Error(e)
        }
    }

    /**
     * Get honesty transactions
     * Uses proper document ID based on auth.uid
     */
    suspend fun getHonestyTransactions(limit: Int): Result<List<HonestyTransactionDto>> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))

            logger.d(TAG, "Getting honesty transactions for user: $currentUserId")

            val transactions = firestore.collection(FirestoreConstants.COLLECTION_HONESTY_TRANSACTIONS)
                .document(currentUserId)
                .collection("transactions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
                .toObjects(HonestyTransactionDto::class.java)

            logger.d(TAG, "Retrieved ${transactions.size} honesty transactions")
            Result.Success(transactions)
        } catch (e: Exception) {
            logger.e(TAG, "Error getting honesty transactions", e)
            Result.Error(e)
        }
    }

    /**
     * Search users
     * Uses proper document ID based on auth.uid
     */
    suspend fun searchUsers(query: String, limit: Int): Result<List<UserDto>> {
        return try {
            logger.d(TAG, "Searching users with query: $query")

            val users = firestore.collection(COLLECTION_USERS)
                .whereGreaterThanOrEqualTo("displayName", query)
                .whereLessThanOrEqualTo("displayName", query + '\uf8ff')
                .limit(limit.toLong())
                .get()
                .await()
                .toObjects(UserDto::class.java)

            logger.d(TAG, "Found ${users.size} users")
            Result.Success(users)
        } catch (e: Exception) {
            logger.e(TAG, "Error searching users", e)
            Result.Error(e)
        }
    }

    /**
     * Get leaderboard
     * Uses proper document ID based on auth.uid
     */
    suspend fun getLeaderboard(limit: Int): Result<List<LeaderboardEntryDto>> {
        return try {
            logger.d(TAG, "Getting leaderboard, limit: $limit")

            val users = firestore.collection(COLLECTION_USERS)
                .orderBy("xp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
                .documents
                .mapIndexed { index, doc ->
                    LeaderboardEntryDto(
                        position = index + 1,
                        user = doc.toObject(UserDto::class.java) ?: UserDto(uid = doc.id),
                        xp = doc.getLong("xp")?.toInt() ?: 0,
                        level = doc.getLong("level")?.toInt() ?: 0,
                        weeklyXp = null,
                        monthlyXp = null
                    )
                }

            logger.d(TAG, "Retrieved ${users.size} leaderboard entries")
            Result.Success(users)
        } catch (e: Exception) {
            logger.e(TAG, "Error getting leaderboard", e)
            Result.Error(e)
        }
    }

    /**
     * Get user leaderboard position
     * Uses proper document ID based on provided uid
     */
    suspend fun getUserLeaderboardPosition(userId: String): Result<Int?> {
        return try {
            logger.d(TAG, "Getting leaderboard position for user: $userId")

            val userSnapshot = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .await()

            if (!userSnapshot.exists()) {
                return Result.Success(null)
            }

            val userXp = userSnapshot.getLong("xp") ?: 0

            val higherXpUsers = firestore.collection(COLLECTION_USERS)
                .whereGreaterThan("xp", userXp)
                .count()
                .get(AggregateSource.SERVER)
                .await()
                .count

            val position = (higherXpUsers + 1).toInt()
            logger.d(TAG, "User leaderboard position: $position")
            Result.Success(position as Int?)
        } catch (e: Exception) {
            logger.e(TAG, "Error getting user leaderboard position", e)
            Result.Error(e)
        }
    }

    /**
     * Delete user
     * Uses proper document ID based on auth.uid
     */
    suspend fun deleteUser(): Result<Unit> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))

            logger.d(TAG, "Deleting user: $currentUserId")

            // Delete user document
            firestore.collection(COLLECTION_USERS)
                .document(currentUserId)
                .delete()
                .await()

            // Delete related collections
            val batch = firestore.batch()

            // Delete XP transactions
            firestore.collection(FirestoreConstants.COLLECTION_XP_TRANSACTIONS)
                .document(currentUserId)
                .delete()

            // Delete honesty transactions
            firestore.collection(FirestoreConstants.COLLECTION_HONESTY_TRANSACTIONS)
                .document(currentUserId)
                .delete()

            // Delete followers/following
            firestore.collection(FirestoreConstants.COLLECTION_FOLLOWERS)
                .document(currentUserId)
                .delete()

            batch.commit().await()

            logger.i(TAG, "User deleted successfully")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Error deleting user", e)
            Result.Error(e)
        }
    }

    /**
     * Refresh user data
     * Uses proper document ID based on auth.uid
     */
    suspend fun refreshUserData(): Result<UserDto> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))

            logger.d(TAG, "Refreshing user data: $currentUserId")

            val snapshot = firestore.collection(COLLECTION_USERS)
                .document(currentUserId)
                .get()
                .await()

            if (!snapshot.exists()) {
                return Result.Error(Exception("User not found"))
            }

            val userDto = snapshot.toObject(UserDto::class.java)!!
            logger.i(TAG, "User data refreshed successfully")
            Result.Success(userDto)
        } catch (e: Exception) {
            logger.e(TAG, "Error refreshing user data", e)
            Result.Error(e)
        }
    }

    private fun calculateLevel(xp: Int): Int {
        // Level formula from rules.md: 50 + (level × level × 100)
        var level = 1
        while (true) {
            val requiredXp = 50 + (level * level * 100)
            if (xp < requiredXp) {
                return level - 1
            }
            level++
        }
    }

    private suspend fun syncToSplitCollections(uid: String, userDto: UserDto) {
        firestore.collection(FirestoreConstants.Collections.USERS_PUBLIC)
            .document(uid)
            .set(
                mapOf(
                    "userId" to uid,
                    "uid" to uid,
                    "userid" to uid,
                    "username" to userDto.displayName,
                    "usernameLower" to (userDto.displayName?.trim()?.lowercase() ?: ""),
                    "displayname" to userDto.displayName,
                    "displayName" to userDto.displayName,
                    "nickname" to (userDto.nickname.ifBlank { userDto.displayName ?: "" }),
                    "photoUrl" to userDto.photoUrl,
                    "photourl" to userDto.photoUrl,
                    "level" to userDto.level,
                    "badges" to userDto.badges,
                    "isPremium" to userDto.isPremium,
                    "ispremium" to userDto.isPremium,
                    "premiumFrameLevel" to userDto.frameLevel,
                    "frame_level" to userDto.frameLevel,
                    "createdAt" to (userDto.createdAt ?: java.util.Date()),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .await()

        firestore.collection(FirestoreConstants.Collections.USERS_PRIVATE)
            .document(uid)
            .set(
                mapOf(
                    "email" to userDto.email,
                    "xp" to userDto.xp,
                    "honesty" to userDto.honesty,
                    "premiumuntil" to userDto.premiumUntil,
                    "deviceTokens" to userDto.deviceTokens,
                    "deviceLang" to userDto.deviceLang,
                    "deviceModel" to userDto.deviceModel,
                    "deviceOs" to userDto.deviceOs,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .await()
    }
}
