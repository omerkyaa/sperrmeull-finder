package com.omerkaya.sperrmuellfinder.data.di

import com.omerkaya.sperrmuellfinder.data.repository.AuthRepositoryImpl
import com.omerkaya.sperrmuellfinder.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for authentication dependencies
 * Provides bindings for authentication-related interfaces
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    
    /**
     * Bind AuthRepository interface to AuthRepositoryImpl
     */
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
}
