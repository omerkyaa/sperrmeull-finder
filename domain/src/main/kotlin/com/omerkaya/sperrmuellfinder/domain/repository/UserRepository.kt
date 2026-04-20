package com.omerkaya.sperrmuellfinder.domain.repository

import android.net.Uri
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.model.UserFavorites
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Repository interface for User operations
 * Rules.md compliant - Clean Architecture domain layer
 */
interface UserRepository {
    fun getCurrentUser(): Flow<User?>
    
    suspend fun getUserById(userId: String): Result<User?>
    
    fun getUserByIdFlow(userId: String): Flow<User?>
    
    suspend fun updateUserProfile(
        displayName: String? = null,
        firstName: String? = null,
        lastName: String? = null,
        city: String? = null,
        photoUrl: String? = null
    ): Result<Unit>
    
    suspend fun updatePremiumStatus(isPremium: Boolean, premiumUntil: Date?): Result<Unit>
    
    suspend fun addBadge(badgeId: String): Result<Unit>
    
    suspend fun removeBadge(badgeId: String): Result<Unit>
    
    suspend fun updateFavorites(favorites: UserFavorites): Result<Unit>
    
    suspend fun updateDeviceTokens(tokens: List<String>): Result<Unit>
    
    suspend fun getUserPostsCount(userId: String): Result<Int>
    
    suspend fun getFollowersCount(userId: String): Result<Int>
    
    suspend fun getFollowingCount(userId: String): Result<Int>
    
    suspend fun searchUsers(query: String, limit: Int): Result<List<User>>
    
    suspend fun deleteUser(): Result<Unit>
    
    suspend fun refreshUserData(): Result<User>
    
    suspend fun isNicknameAvailable(nickname: String): Result<Boolean>
    
    suspend fun uploadProfilePhoto(userId: String, imageUri: Uri): Result<String>
    
    suspend fun updateProfilePhotoUrl(userId: String, photoUrl: String): Result<Unit>
    
    suspend fun ensureUserDocument(fcmToken: String?): Result<User>
    
    suspend fun fixMissingOwnerPhotoUrls(): Result<Unit>
    
    // ========================================
    // ACCOUNT DELETION
    // ========================================
    
    /**
     * Request account deletion (30-day grace period)
     * This will call Cloud Function to schedule deletion
     */
    suspend fun requestAccountDeletion(reason: String?): Result<Unit>
    
    /**
     * Cancel account deletion request
     * This will call Cloud Function to cancel scheduled deletion
     */
    suspend fun cancelAccountDeletion(): Result<Unit>
    
    /**
     * Get account deletion status
     * Returns Flow of AccountDeletionStatus or null if no deletion scheduled
     */
    fun getAccountDeletionStatus(): Flow<com.omerkaya.sperrmuellfinder.domain.model.AccountDeletionStatus?>
}
