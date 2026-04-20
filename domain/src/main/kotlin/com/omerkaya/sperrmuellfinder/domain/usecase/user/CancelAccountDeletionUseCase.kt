package com.omerkaya.sperrmuellfinder.domain.usecase.user

import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.repository.UserRepository
import javax.inject.Inject

/**
 * Use case for cancelling account deletion
 * Rules.md compliant - Clean Architecture domain layer
 */
class CancelAccountDeletionUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "CancelAccountDeletionUseCase"
    }
    
    /**
     * Cancel account deletion request
     * @return Result of the operation
     */
    suspend operator fun invoke(): Result<Unit> {
        logger.d(TAG, "Cancelling account deletion")
        return userRepository.cancelAccountDeletion()
    }
}
