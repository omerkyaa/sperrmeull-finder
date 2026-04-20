package com.omerkaya.sperrmuellfinder.di

import com.omerkaya.sperrmuellfinder.data.repository.SettingsRepositoryImpl
import com.omerkaya.sperrmuellfinder.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 🔧 SETTINGS MODULE - SperrmüllFinder
 * Rules.md compliant - Hilt dependency injection module
 */

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {
    
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository
}
