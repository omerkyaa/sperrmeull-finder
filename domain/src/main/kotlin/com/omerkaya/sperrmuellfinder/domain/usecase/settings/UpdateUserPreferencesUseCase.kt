package com.omerkaya.sperrmuellfinder.domain.usecase.settings

import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.UserPreferences
import com.omerkaya.sperrmuellfinder.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * 📦 UPDATE USER PREFERENCES USE CASE - SperrmüllFinder
 * Rules.md compliant - Domain layer use case
 */
class UpdateUserPreferencesUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    
    /**
     * Update user preferences
     */
    suspend operator fun invoke(preferences: UserPreferences): Result<Unit> {
        return settingsRepository.updateUserPreferences(preferences)
    }
}
