package com.omerkaya.sperrmuellfinder.domain.usecase.settings

import com.omerkaya.sperrmuellfinder.domain.model.AppInfo
import javax.inject.Inject

/**
 * 📦 GET APP INFO USE CASE - SperrmüllFinder
 * Rules.md compliant - Domain layer use case
 */
class GetAppInfoUseCase @Inject constructor() {
    
    /**
     * Get app information for About page
     */
    operator fun invoke(): AppInfo {
        return AppInfo()
    }
}
