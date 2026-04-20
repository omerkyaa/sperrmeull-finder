package com.omerkaya.sperrmuellfinder.domain.usecase.social

import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.repository.SocialRepository
import javax.inject.Inject

/**
 * Use case for unblocking users
 * Rules.md compliant - Clean Architecture domain layer
 */
class UnblockUserUseCase @Inject constructor(
    private val socialRepository: SocialRepository,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "UnblockUserUseCase"
    }
    
    /**
     * Unblock a user
     * @param userId User to unblock
     * @return Result of the operation
     */
    suspend operator fun invoke(userId: String): Result<Unit> {
        logger.d(TAG, "Unblocking user $userId")
        return socialRepository.unblockUser(userId)
    }
}
