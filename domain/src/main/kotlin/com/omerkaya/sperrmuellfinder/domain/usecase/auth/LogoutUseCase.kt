package com.omerkaya.sperrmuellfinder.domain.usecase.auth

import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    
    /**
     * Sign out current user
     */
    suspend operator fun invoke(): Result<Unit> {
        return try {
            authRepository.signOut()
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
