package com.omerkaya.sperrmuellfinder.domain.usecase.user

import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.repository.UserRepository
import javax.inject.Inject

/**
 * Use case for requesting account deletion
 * Rules.md compliant - Clean Architecture domain layer
 */
class DeleteAccountUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "DeleteAccountUseCase"
    }
    
    /**
     * Request account deletion (30-day grace period)
     * @param reason Optional reason for deletion
     * @return Result of the operation
     */
    suspend operator fun invoke(reason: String?): Result<Unit> {
        logger.d(TAG, "Requesting account deletion")
        return userRepository.requestAccountDeletion(reason)
    }
}
