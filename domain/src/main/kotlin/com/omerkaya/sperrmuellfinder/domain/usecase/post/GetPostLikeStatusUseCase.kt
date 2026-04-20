package com.omerkaya.sperrmuellfinder.domain.usecase.post

import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.domain.repository.FirestoreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * 💖 GET POST LIKE STATUS USE CASE - Real-time Like Status
 * Provides real-time like status for posts to ensure persistent red heart
 * 
 * Features:
 * - Real-time Firestore listener for like status
 * - Professional error handling
 * - Persistent like state (red heart stays red)
 * - Optimized for performance
 */
class GetPostLikeStatusUseCase @Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    private val logger: Logger
) {

    /**
     * Get real-time like status for a post by current user.
     * Returns Flow<Boolean> that updates whenever like status changes.
     * 
     * @param postId The post ID to check
     * @param userId The user ID to check
     * @return Flow of Boolean indicating like status (true = liked, false = not liked)
     */
    operator fun invoke(postId: String, userId: String): Flow<Boolean> {
        // Input validation to prevent crashes
        if (postId.isBlank() || userId.isBlank()) {
            logger.w(Logger.TAG_DEFAULT, "⚠️ Invalid parameters: postId='$postId', userId='$userId'")
            return flow { emit(false) }
        }
        
        logger.d(Logger.TAG_DEFAULT, "🔍 Setting up real-time like status for post: $postId, user: $userId")
        
        return try {
            firestoreRepository.isPostLikedByUserFlow(postId, userId)
                .catch { e ->
                    logger.e(Logger.TAG_DEFAULT, "❌ Error in GetPostLikeStatusUseCase", e)
                    emit(false) // Default to not liked on error
                }
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "❌ Critical error in GetPostLikeStatusUseCase", e)
            flow { emit(false) } // Return safe flow on critical error
        }
    }
}
