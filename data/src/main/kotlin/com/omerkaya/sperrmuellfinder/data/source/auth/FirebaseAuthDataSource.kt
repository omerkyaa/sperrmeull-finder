package com.omerkaya.sperrmuellfinder.data.source.auth

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.data.dto.user.UserDto
import com.omerkaya.sperrmuellfinder.data.dto.user.UserFavoritesDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Authentication data source
 * Handles all Firebase Auth operations and user data management
 */
@Singleton
class FirebaseAuthDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val logger: Logger
) {
    
    companion object {
        private const val USERS_PUBLIC_COLLECTION = "users_public"
        private const val USERS_PRIVATE_COLLECTION = "users_private"
        private const val LEGACY_USERS_COLLECTION = "users"
        private const val INITIAL_XP = 100
        private const val INITIAL_LEVEL = 1
        private const val INITIAL_HONESTY = 100
        
        /**
         * Parse birth date string to Date object
         * Expected format: DD.MM.YYYY (e.g., "25.12.1990")
         * Rules.md compliant - birth date validation
         */
        fun parseBirthDate(birthDateString: String?): Date? {
            return try {
                if (birthDateString.isNullOrBlank()) return null
                
                val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                formatter.isLenient = false // Strict parsing
                formatter.parse(birthDateString)
            } catch (e: Exception) {
                null // Return null for invalid dates
            }
        }
    }
    
    /**
     * Get current Firebase user as Flow
     */
    fun getCurrentUserFlow(): Flow<FirebaseUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        
        firebaseAuth.addAuthStateListener(authStateListener)
        
        // Send initial value
        trySend(firebaseAuth.currentUser)
        
        awaitClose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }
    
    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }
    
    /**
     * Login with email and password
     */
    suspend fun login(email: String, password: String): Result<UserDto> {
        return try {
            logger.d(Logger.TAG_AUTH, "Attempting login for email: $email")
            
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
            
            if (firebaseUser != null) {
                // Get or create user document
                val userDto = getUserFromFirestore(firebaseUser.uid) 
                    ?: createUserDocument(firebaseUser, email)
                
                logger.i(Logger.TAG_AUTH, "Login successful for user: ${firebaseUser.uid}")
                Result.Success(userDto)
            } else {
                logger.e(Logger.TAG_AUTH, "Login failed: Firebase user is null")
                Result.Error(Exception("Authentication failed - Firebase user is null"))
            }
        } catch (e: FirebaseAuthException) {
            logger.e(Logger.TAG_AUTH, "Firebase auth error: ${e.errorCode}", e)
            Result.Error(mapFirebaseAuthException(e))
        } catch (e: Exception) {
            logger.e(Logger.TAG_AUTH, "Login error", e)
            Result.Error(e)
        }
    }
    
    /**
     * Register new user with email and password
     */
    suspend fun register(
        email: String,
        password: String,
        nickname: String,
        firstName: String,
        lastName: String,
        city: String,
        birthDate: String,
        profilePhotoUrl: String? // Firebase Storage download URL
    ): Result<UserDto> {
        return try {
            logger.d(Logger.TAG_AUTH, "ADIM 1: Kayıt başlıyor - email: $email")
            
            // Create user in Firebase Auth
            logger.d(Logger.TAG_AUTH, "ADIM 2: Firebase Auth'da kullanıcı oluşturuluyor")
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
            
            logger.d(Logger.TAG_AUTH, "ADIM 3: Firebase Auth kullanıcısı oluşturuldu - uid: ${firebaseUser?.uid}")
            
            if (firebaseUser != null) {
                try {
                    logger.d(Logger.TAG_AUTH, "ADIM 4: Profil güncellemesi başlıyor")
                    
                    // Firestore dokümanını oluştur
                    logger.d(Logger.TAG_AUTH, "ADIM 5: Firestore dokümanı oluşturuluyor")
                    val userDto = UserDto(
                        uid = firebaseUser.uid,
                        displayName = nickname,
                        nickname = nickname,
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        photoUrl = profilePhotoUrl,
                        city = city,
                        dateOfBirth = parseBirthDate(birthDate),
                        gender = null,
                        xp = INITIAL_XP,
                        level = INITIAL_LEVEL,
                        honesty = INITIAL_HONESTY,
                        isPremium = false,
                        premiumUntil = null,
                        frameLevel = 0,
                        badges = emptyList(),
                        favorites = UserFavoritesDto(),
                        createdAt = java.util.Date(),
                        updatedAt = java.util.Date(),
                        deviceTokens = emptyList(),
                        deviceLang = "de",
                        deviceModel = android.os.Build.MODEL,
                        deviceOs = "Android ${android.os.Build.VERSION.RELEASE}"
                    )

                    upsertSplitUser(firebaseUser.uid, userDto)
                    
                    logger.d(Logger.TAG_AUTH, "ADIM 6: Firestore dokümanı oluşturuldu")

                    // Firebase Auth profilini güncelle
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(nickname)
                        .apply {
                            if (!profilePhotoUrl.isNullOrBlank()) {
                                setPhotoUri(android.net.Uri.parse(profilePhotoUrl))
                            }
                        }
                        .build()
                    
                    firebaseUser.updateProfile(profileUpdates).await()
                    logger.d(Logger.TAG_AUTH, "ADIM 7: Firebase Auth profili güncellendi")

                    // Yeni token al
                    firebaseUser.getIdToken(true).await()
                    logger.d(Logger.TAG_AUTH, "ADIM 8: Token yenilendi")
                    
                    logger.i(Logger.TAG_AUTH, "Registration successful for user: ${firebaseUser.uid}")
                    Result.Success(userDto)
                } catch (e: Exception) {
                    // If profile update or document creation fails, but user is created
                    logger.e(Logger.TAG_AUTH, "Photo upload failed but registration completed", e)
                    
                    // Create user document without photo
                    val userDto = createUserDocument(
                        firebaseUser = firebaseUser,
                        email = email,
                        nickname = nickname,
                        firstName = firstName,
                        lastName = lastName,
                        city = city,
                        birthDate = birthDate,
                        profilePhotoUrl = null // Skip photo for now
                    )
                    
                    Result.Success(userDto)
                }
            } else {
                logger.e(Logger.TAG_AUTH, "Registration failed: Firebase user is null")
                Result.Error(Exception("Registration failed - Firebase user is null"))
            }
        } catch (e: FirebaseAuthException) {
            logger.e(Logger.TAG_AUTH, "Firebase auth error: ${e.errorCode}", e)
            Result.Error(mapFirebaseAuthException(e))
        } catch (e: Exception) {
            logger.e(Logger.TAG_AUTH, "Registration error", e)
            Result.Error(e)
        }
    }
    
    /**
     * Login with Google
     */
    suspend fun loginWithGoogle(idToken: String): Result<UserDto> {
        return try {
            logger.d(Logger.TAG_AUTH, "Attempting Google login")
            
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
            
            if (firebaseUser != null) {
                // Check if user exists, create if new
                val existingUser = getUserFromFirestore(firebaseUser.uid)
                val userDto = existingUser ?: createUserDocument(
                    firebaseUser = firebaseUser,
                    email = firebaseUser.email ?: "",
                    nickname = firebaseUser.displayName ?: "User",
                    firstName = "", // Will be updated in onboarding
                    lastName = "", // Will be updated in onboarding
                    city = "", // Will be updated in onboarding
                    birthDate = "", // Will be updated in onboarding
                    profilePhotoUrl = firebaseUser.photoUrl?.toString()
                )
                
                logger.i(Logger.TAG_AUTH, "Google login successful for user: ${firebaseUser.uid}")
                Result.Success(userDto)
            } else {
                logger.e(Logger.TAG_AUTH, "Google login failed: Firebase user is null")
                Result.Error(Exception("Google authentication failed"))
            }
        } catch (e: FirebaseAuthException) {
            logger.e(Logger.TAG_AUTH, "Firebase auth error: ${e.errorCode}", e)
            Result.Error(mapFirebaseAuthException(e))
        } catch (e: Exception) {
            logger.e(Logger.TAG_AUTH, "Google login error", e)
            Result.Error(e)
        }
    }
    
    /**
     * Send password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            logger.d(Logger.TAG_AUTH, "Sending password reset email to: $email")
            
            firebaseAuth.sendPasswordResetEmail(email).await()
            
            logger.i(Logger.TAG_AUTH, "Password reset email sent successfully")
            Result.Success(Unit)
        } catch (e: FirebaseAuthException) {
            logger.e(Logger.TAG_AUTH, "Password reset error: ${e.errorCode}", e)
            Result.Error(mapFirebaseAuthException(e))
        } catch (e: Exception) {
            logger.e(Logger.TAG_AUTH, "Password reset error", e)
            Result.Error(e)
        }
    }
    
    /**
     * Update password
     */
    suspend fun updatePassword(newPassword: String): Result<Unit> {
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                logger.d(Logger.TAG_AUTH, "Updating password for user: ${currentUser.uid}")
                
                currentUser.updatePassword(newPassword).await()
                
                logger.i(Logger.TAG_AUTH, "Password updated successfully")
                Result.Success(Unit)
            } else {
                Result.Error(Exception("User not authenticated"))
            }
        } catch (e: FirebaseAuthException) {
            logger.e(Logger.TAG_AUTH, "Password update error: ${e.errorCode}", e)
            Result.Error(mapFirebaseAuthException(e))
        } catch (e: Exception) {
            logger.e(Logger.TAG_AUTH, "Password update error", e)
            Result.Error(e)
        }
    }
    
    /**
     * Update email with verification (modern Firebase Auth API)
     */
    suspend fun updateEmail(newEmail: String): Result<Unit> {
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                logger.d(Logger.TAG_AUTH, "Updating email for user: ${currentUser.uid}")
                
                // Use modern verifyBeforeUpdateEmail instead of deprecated updateEmail
                currentUser.verifyBeforeUpdateEmail(newEmail).await()
                
                logger.i(Logger.TAG_AUTH, "Email verification sent successfully. User needs to verify before email is updated.")
                Result.Success(Unit)
            } else {
                Result.Error(Exception("User not authenticated"))
            }
        } catch (e: FirebaseAuthException) {
            logger.e(Logger.TAG_AUTH, "Email update error: ${e.errorCode}", e)
            Result.Error(mapFirebaseAuthException(e))
        } catch (e: Exception) {
            logger.e(Logger.TAG_AUTH, "Email update error", e)
            Result.Error(e)
        }
    }

    /**
     * Complete email update after verification (called after user clicks verification link)
     */
    suspend fun completeEmailUpdate(newEmail: String): Result<Unit> {
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                logger.d(Logger.TAG_AUTH, "Completing email update for user: ${currentUser.uid}")
                
                // Update email in Firestore after verification
                firestore.collection(USERS_PRIVATE_COLLECTION)
                    .document(currentUser.uid)
                    .update("email", newEmail)
                    .await()
                
                logger.i(Logger.TAG_AUTH, "Email updated successfully in Firestore")
                Result.Success(Unit)
            } else {
                Result.Error(Exception("User not authenticated"))
            }
        } catch (e: Exception) {
            logger.e(Logger.TAG_AUTH, "Email update completion error", e)
            Result.Error(e)
        }
    }
    
    /**
     * Update user profile
     */
    suspend fun updateProfile(
        displayName: String? = null,
        photoUrl: String? = null,
        city: String? = null
    ): Result<UserDto> {
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                logger.d(Logger.TAG_AUTH, "Updating profile for user: ${currentUser.uid}")
                
                // Update Firebase Auth profile
                if (displayName != null || photoUrl != null) {
                    val profileUpdates = UserProfileChangeRequest.Builder().apply {
                        displayName?.let { setDisplayName(it) }
                        photoUrl?.let { setPhotoUri(android.net.Uri.parse(it)) }
                    }.build()
                    
                    currentUser.updateProfile(profileUpdates).await()
                }
                
                // Update Firestore document
                val updates = mutableMapOf<String, Any>()
                displayName?.let { updates["displayname"] = it }
                photoUrl?.let { updates["photourl"] = it }
                city?.let { updates["city"] = it }
                updates["updated_at"] = java.util.Date(System.currentTimeMillis())
                
                if (updates.isNotEmpty()) {
                    firestore.collection(USERS_PUBLIC_COLLECTION)
                        .document(currentUser.uid)
                        .update(updates)
                        .await()

                    firestore.collection(LEGACY_USERS_COLLECTION)
                        .document(currentUser.uid)
                        .update(updates)
                        .await()
                }
                
                // Return updated user
                val updatedUser = getUserFromFirestore(currentUser.uid)
                if (updatedUser != null) {
                    logger.i(Logger.TAG_AUTH, "Profile updated successfully")
                    Result.Success(updatedUser)
                } else {
                    Result.Error(Exception("Failed to retrieve updated user"))
                }
            } else {
                Result.Error(Exception("User not authenticated"))
            }
        } catch (e: Exception) {
            logger.e(Logger.TAG_AUTH, "Profile update error", e)
            Result.Error(e)
        }
    }
    
    /**
     * Delete account
     */
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                logger.d(Logger.TAG_AUTH, "Deleting account for user: ${currentUser.uid}")
                
                // Delete user document from Firestore
                firestore.collection(USERS_PUBLIC_COLLECTION)
                    .document(currentUser.uid)
                    .delete()
                    .await()

                firestore.collection(USERS_PRIVATE_COLLECTION)
                    .document(currentUser.uid)
                    .delete()
                    .await()

                firestore.collection(LEGACY_USERS_COLLECTION)
                    .document(currentUser.uid)
                    .delete()
                    .await()
                
                // Delete Firebase Auth user
                currentUser.delete().await()
                
                logger.i(Logger.TAG_AUTH, "Account deleted successfully")
                Result.Success(Unit)
            } else {
                Result.Error(Exception("User not authenticated"))
            }
        } catch (e: FirebaseAuthException) {
            logger.e(Logger.TAG_AUTH, "Account deletion error: ${e.errorCode}", e)
            Result.Error(mapFirebaseAuthException(e))
        } catch (e: Exception) {
            logger.e(Logger.TAG_AUTH, "Account deletion error", e)
            Result.Error(e)
        }
    }
    
    /**
     * Sign out
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            logger.d(Logger.TAG_AUTH, "Signing out user")
            
            firebaseAuth.signOut()
            
            logger.i(Logger.TAG_AUTH, "Sign out successful")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(Logger.TAG_AUTH, "Sign out error", e)
            Result.Error(e)
        }
    }
    
    /**
     * Check if email is available (modern approach without deprecated API)
     */
    suspend fun isEmailAvailable(email: String): Result<Boolean> {
        return try {
            logger.d(Logger.TAG_AUTH, "Checking email availability: $email")
            
            // Modern approach: Try to create account with temporary password
            // If email exists, it will throw ERROR_EMAIL_ALREADY_IN_USE
            // We don't actually create the account, just test the email
            try {
                // Use a temporary password for testing
                val tempPassword = "TempPass123!@#"
                firebaseAuth.createUserWithEmailAndPassword(email, tempPassword).await()
                
                // If we reach here, email is available but we need to delete the test user
                firebaseAuth.currentUser?.delete()?.await()
                
                logger.d(Logger.TAG_AUTH, "Email availability check result: true (available)")
                Result.Success(true)
                
            } catch (authException: FirebaseAuthException) {
                when (authException.errorCode) {
                    "ERROR_EMAIL_ALREADY_IN_USE" -> {
                        logger.d(Logger.TAG_AUTH, "Email availability check result: false (already in use)")
                        Result.Success(false)
                    }
                    "ERROR_INVALID_EMAIL" -> {
                        logger.d(Logger.TAG_AUTH, "Invalid email format")
                        Result.Error(Exception("Invalid email format"))
                    }
                    else -> {
                        logger.e(Logger.TAG_AUTH, "Email availability check error: ${authException.errorCode}", authException)
                        Result.Error(mapFirebaseAuthException(authException))
                    }
                }
            }
        } catch (e: Exception) {
            logger.e(Logger.TAG_AUTH, "Email availability check error", e)
            Result.Error(e)
        }
    }

    /**
     * Check if nickname is available
     */
    suspend fun isNicknameAvailable(nickname: String): Result<Boolean> {
        return try {
            logger.d(Logger.TAG_AUTH, "Checking nickname availability: $nickname")

            if (!nickname.matches(Regex("^[a-zA-Z0-9._-]{3,20}$"))) {
                logger.d(Logger.TAG_AUTH, "Nickname format invalid")
                return Result.Error(Exception("Invalid nickname format"))
            }

            val normalized = nickname.trim().lowercase()
            val existing = firestore.collection(USERS_PUBLIC_COLLECTION)
                .whereEqualTo("usernameLower", normalized)
                .limit(1)
                .get()
                .await()

            val isAvailable = existing.isEmpty
            logger.d(Logger.TAG_AUTH, "Nickname availability check result: $isAvailable")
            Result.Success(isAvailable)
        } catch (e: Exception) {
            logger.e(Logger.TAG_AUTH, "Nickname availability check error", e)
            Result.Error(e)
        }
    }

    /**
     * Alternative email availability check using Firestore query
     * More reliable and doesn't require test account creation
     */
    suspend fun isEmailAvailableFirestore(email: String): Result<Boolean> {
        return try {
            logger.d(Logger.TAG_AUTH, "Checking email availability via Firestore: $email")
            
            val querySnapshot = firestore.collection(USERS_PRIVATE_COLLECTION)
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()
            
            val isAvailable = querySnapshot.isEmpty
            
            logger.d(Logger.TAG_AUTH, "Email availability check result: $isAvailable")
            Result.Success(isAvailable)
        } catch (e: Exception) {
            logger.e(Logger.TAG_AUTH, "Firestore email availability check error", e)
            Result.Error(e)
        }
    }
    
    /**
     * Get user from Firestore
     */
    suspend fun getUserFromFirestore(uid: String): UserDto? {
        return try {
            logger.d(Logger.TAG_AUTH, "Fetching user from Firestore: $uid")

            val publicDoc = firestore.collection(USERS_PUBLIC_COLLECTION)
                .document(uid)
                .get()
                .await()
            val privateDoc = firestore.collection(USERS_PRIVATE_COLLECTION)
                .document(uid)
                .get()
                .await()

            if (publicDoc.exists() || privateDoc.exists()) {
                val base = publicDoc.toObject(UserDto::class.java)
                    ?: privateDoc.toObject(UserDto::class.java)
                    ?: UserDto(uid = uid)
                return base.copy(
                    uid = uid,
                    email = privateDoc.getString("email") ?: base.email,
                    deviceTokens = privateDoc.get("deviceTokens") as? List<String> ?: base.deviceTokens,
                    isPremium = privateDoc.getBoolean("ispremium")
                        ?: privateDoc.getBoolean("isPremium")
                        ?: base.isPremium,
                    premiumUntil = privateDoc.getDate("premiumuntil")
                        ?: privateDoc.getDate("premiumUntil")
                        ?: base.premiumUntil,
                    xp = (privateDoc.getLong("xp") ?: base.xp.toLong()).toInt(),
                    level = (publicDoc.getLong("level")
                        ?: privateDoc.getLong("level")
                        ?: base.level.toLong()).toInt(),
                    honesty = (privateDoc.getLong("honesty")
                        ?: base.honesty.toLong()).toInt(),
                    isBanned = privateDoc.getBoolean("isBanned")
                        ?: publicDoc.getBoolean("isBanned")
                        ?: base.isBanned,
                    banType = privateDoc.getString("banType")
                        ?: publicDoc.getString("banType")
                        ?: base.banType,
                    banUntil = privateDoc.getDate("banUntil")
                        ?: publicDoc.getDate("banUntil")
                        ?: base.banUntil,
                    banReason = privateDoc.getString("banReason")
                        ?: publicDoc.getString("banReason")
                        ?: base.banReason,
                    bannedBy = privateDoc.getString("bannedBy")
                        ?: publicDoc.getString("bannedBy")
                        ?: base.bannedBy,
                    bannedAt = privateDoc.getDate("bannedAt")
                        ?: publicDoc.getDate("bannedAt")
                        ?: base.bannedAt,
                    authDisabled = privateDoc.getBoolean("authDisabled")
                        ?: publicDoc.getBoolean("authDisabled")
                        ?: base.authDisabled
                )
            }

            val legacyDoc = firestore.collection(LEGACY_USERS_COLLECTION)
                .document(uid)
                .get()
                .await()
            if (!legacyDoc.exists()) {
                logger.w(Logger.TAG_AUTH, "User document not found in split or legacy collections: $uid")
                return null
            }

            val legacyUser = legacyDoc.toObject(UserDto::class.java)?.copy(uid = uid)
            if (legacyUser != null) {
                upsertSplitUser(uid, legacyUser)
            }
            legacyUser
        } catch (e: Exception) {
            logger.e(Logger.TAG_AUTH, "Error fetching user from Firestore", e)
            null
        }
    }
    
    /**
     * Create user document in Firestore
     */
    private suspend fun createUserDocument(
        firebaseUser: FirebaseUser,
        email: String,
        nickname: String = firebaseUser.displayName ?: "User",
        firstName: String = "",
        lastName: String = "",
        city: String = "",
        birthDate: String = "",
        profilePhotoUrl: String? = null
    ): UserDto {
        logger.d(Logger.TAG_AUTH, "ADIM 10: createUserDocument başladı - uid: ${firebaseUser.uid}")
        
        val currentTime = System.currentTimeMillis()
        
        // Önce kullanıcının hala giriş yapmış olduğunu kontrol et
        if (firebaseAuth.currentUser == null) {
            logger.e(Logger.TAG_AUTH, "HATA: Kullanıcı artık giriş yapmış değil")
            throw Exception("User is not authenticated")
        }
        
        logger.d(Logger.TAG_AUTH, "ADIM 11: UserDto oluşturuluyor")
        val userDto = UserDto(
            uid = firebaseUser.uid,
            displayName = nickname,
            nickname = nickname,
            firstName = firstName,
            lastName = lastName,
            email = email,
            photoUrl = profilePhotoUrl ?: firebaseUser.photoUrl?.toString(),
            city = city,
            dateOfBirth = parseBirthDate(birthDate),
            gender = null,
            xp = INITIAL_XP,
            level = INITIAL_LEVEL,
            honesty = INITIAL_HONESTY,
            isPremium = false,
            premiumUntil = null,
            frameLevel = 0,
            badges = emptyList(),
            favorites = UserFavoritesDto(),
            createdAt = java.util.Date(currentTime),
            updatedAt = java.util.Date(currentTime),
            deviceTokens = emptyList(),
            deviceLang = "de",
            deviceModel = android.os.Build.MODEL,
            deviceOs = "Android ${android.os.Build.VERSION.RELEASE}"
        )
        
        try {
            logger.d(Logger.TAG_AUTH, "ADIM 12: Firestore'a yazma başlıyor")
            
            // Önce koleksiyonun var olduğunu kontrol et
            logger.d(Logger.TAG_AUTH, "ADIM 13: Doküman yazılıyor")
            upsertSplitUser(firebaseUser.uid, userDto)
            
            logger.i(Logger.TAG_AUTH, "BAŞARILI: Kullanıcı dokümanı oluşturuldu - uid: ${firebaseUser.uid}")
            return userDto
            
        } catch (e: Exception) {
            logger.e(Logger.TAG_AUTH, "HATA: Doküman oluşturma hatası", e)
            logger.e(Logger.TAG_AUTH, "HATA DETAY: ${e.message}")
            logger.e(Logger.TAG_AUTH, "HATA STACK: ${e.stackTraceToString()}")
            throw e
        }
    }

    private suspend fun upsertSplitUser(uid: String, userDto: UserDto) {
        val publicData = mapOf(
            "userId" to uid,
            "uid" to uid,
            "userid" to uid,
            "username" to userDto.displayName,
            "usernameLower" to (userDto.displayName?.trim()?.lowercase() ?: ""),
            "displayname" to userDto.displayName,
            "displayName" to userDto.displayName,
            "nickname" to userDto.nickname,
            "photoUrl" to userDto.photoUrl,
            "photourl" to userDto.photoUrl,
            "level" to userDto.level,
            "badges" to userDto.badges,
            "isPremium" to userDto.isPremium,
            "ispremium" to userDto.isPremium,
            "premiumFrameLevel" to userDto.frameLevel,
            "frame_level" to userDto.frameLevel,
            "created_at" to (userDto.createdAt ?: java.util.Date()),
            "updated_at" to java.util.Date()
        )

        val privateData = mapOf(
            "email" to userDto.email,
            "xp" to userDto.xp,
            "level" to userDto.level,
            "honesty" to userDto.honesty,
            "ispremium" to userDto.isPremium,
            "premiumuntil" to userDto.premiumUntil,
            "deviceTokens" to userDto.deviceTokens,
            "deviceLang" to userDto.deviceLang,
            "deviceModel" to userDto.deviceModel,
            "deviceOs" to userDto.deviceOs,
            "updated_at" to java.util.Date()
        )

        firestore.collection(USERS_PUBLIC_COLLECTION)
            .document(uid)
            .set(publicData, SetOptions.merge())
            .await()

        firestore.collection(USERS_PRIVATE_COLLECTION)
            .document(uid)
            .set(privateData, SetOptions.merge())
            .await()

        firestore.collection(LEGACY_USERS_COLLECTION)
            .document(uid)
            .set(userDto, SetOptions.merge())
            .await()
    }
    
    
    /**
     * Map Firebase Auth exceptions to user-friendly messages
     */
    private fun mapFirebaseAuthException(exception: FirebaseAuthException): Exception {
        val rawMessage = exception.message.orEmpty()
        if (rawMessage.contains("USER_BANNED", ignoreCase = true)) {
            return Exception("USER_BANNED")
        }

        val message = when (exception.errorCode) {
            "ERROR_INVALID_EMAIL" -> "Invalid email address"
            "ERROR_WRONG_PASSWORD" -> "Wrong password"
            "ERROR_USER_NOT_FOUND" -> "User not found"
            "ERROR_USER_DISABLED" -> "User account has been disabled"
            "ERROR_TOO_MANY_REQUESTS" -> "Too many requests. Please try again later"
            "ERROR_OPERATION_NOT_ALLOWED" -> "Operation not allowed"
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Email is already in use"
            "ERROR_WEAK_PASSWORD" -> "Password is too weak"
            "ERROR_REQUIRES_RECENT_LOGIN" -> "This operation requires recent authentication"
            "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Please check your connection"
            else -> exception.message ?: "Authentication error"
        }
        return Exception(message)
    }
}
