package com.omerkaya.sperrmuellfinder.domain.usecase.social

import com.omerkaya.sperrmuellfinder.domain.model.BlockedUser
import com.omerkaya.sperrmuellfinder.domain.repository.SocialRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting blocked users list
 * Rules.md compliant - Clean Architecture domain layer
 */
class GetBlockedUsersUseCase @Inject constructor(
    private val socialRepository: SocialRepository
) {
    /**
     * Get list of blocked users
     * @return Flow of blocked users list
     */
    operator fun invoke(): Flow<List<BlockedUser>> {
        return socialRepository.getBlockedUsers()
    }
}
