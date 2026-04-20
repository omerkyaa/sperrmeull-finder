package com.omerkaya.sperrmuellfinder.domain.usecase.social

import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.repository.SocialRepository
import javax.inject.Inject

/**
 * Use case for blocking users
 * Rules.md compliant - Clean Architecture domain layer
 */
class BlockUserUseCase @Inject constructor(
    private val socialRepository: SocialRepository,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "BlockUserUseCase"
    }
    
    /**
     * Block a user
     * @param userId User to block
     * @param reason Optional reason for blocking
     * @return Result of the operation
     */
    suspend operator fun invoke(
        userId: String,
        reason: String? = null
    ): Result<Unit> {
        logger.d(TAG, "Blocking user $userId")
        
        // Validation: Can't block yourself
        // This will be checked in repository with current user ID
        
        return socialRepository.blockUser(userId, reason)
    }
}
