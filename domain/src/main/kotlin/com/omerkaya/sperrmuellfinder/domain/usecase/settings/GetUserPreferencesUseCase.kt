package com.omerkaya.sperrmuellfinder.domain.usecase.settings

import com.omerkaya.sperrmuellfinder.domain.model.UserPreferences
import com.omerkaya.sperrmuellfinder.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 📦 GET USER PREFERENCES USE CASE - SperrmüllFinder
 * Rules.md compliant - Domain layer use case
 */
class GetUserPreferencesUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    
    /**
     * Get user preferences as reactive Flow
     */
    operator fun invoke(): Flow<UserPreferences> {
        return settingsRepository.getUserPreferences()
    }
}
