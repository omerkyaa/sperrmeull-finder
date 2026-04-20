package com.omerkaya.sperrmuellfinder.core.di

import com.omerkaya.sperrmuellfinder.core.navigation.NavigationManager
import com.omerkaya.sperrmuellfinder.core.util.Logger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing navigation-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object NavigationModule {

    @Provides
    @Singleton
    fun provideNavigationManager(
        logger: Logger
    ): NavigationManager {
        return NavigationManager(logger)
    }
}
