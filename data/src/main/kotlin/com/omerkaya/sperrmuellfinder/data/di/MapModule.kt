package com.omerkaya.sperrmuellfinder.data.di

import com.omerkaya.sperrmuellfinder.data.repository.MapRepositoryImpl
import com.omerkaya.sperrmuellfinder.domain.repository.MapRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for map-related dependencies.
 * Provides repository implementations and data sources.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MapModule {
    
    /**
     * Bind MapRepositoryImpl to MapRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindMapRepository(
        mapRepositoryImpl: MapRepositoryImpl
    ): MapRepository
}
