package com.omerkaya.sperrmuellfinder.domain.usecase.location

import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.core.model.PostLocation
import com.omerkaya.sperrmuellfinder.domain.repository.LocationRepository
import javax.inject.Inject

/**
 * Use case to get the current location of the device
 * Rules.md compliant - Clean Architecture domain layer
 */
class GetLocationUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(): Result<PostLocation> {
        return locationRepository.getCurrentLocation()
    }
}
