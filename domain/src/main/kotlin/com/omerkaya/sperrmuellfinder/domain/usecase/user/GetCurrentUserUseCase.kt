package com.omerkaya.sperrmuellfinder.domain.usecase.user

import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for getting current user profile
 * Provides reactive user data with error handling
 */
@Singleton
class GetCurrentUserUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val logger: Logger
) {
    
    /**
     * Get current user as Flow for reactive UI updates
     */
    operator fun invoke(): Flow<User?> {
        return userRepository.getCurrentUser()
            .catch { exception ->
                logger.e(Logger.TAG_DEFAULT, "Error getting current user", exception)
                emit(null)
            }
            .map { user ->
                user?.let {
                    logger.d(Logger.TAG_DEFAULT, "Current user loaded: ${it.displayName} (${if (it.isPremium) "Premium" else "Free"})")
                    it
                }
            }
    }
    
    /**
     * Get current user with statistics
     */
    fun getCurrentUserWithStats(): Flow<User?> {
        return userRepository.getCurrentUser()
            .catch { exception ->
                logger.e(Logger.TAG_DEFAULT, "Error getting current user with stats", exception)
                emit(null)
            }
            .map { user ->
                user?.let {
                    logger.d(Logger.TAG_DEFAULT, "Current user with stats: Premium: ${it.isPremium}, Followers: ${it.followersCount}, Following: ${it.followingCount}")
                    it
                }
            }
    }
}
