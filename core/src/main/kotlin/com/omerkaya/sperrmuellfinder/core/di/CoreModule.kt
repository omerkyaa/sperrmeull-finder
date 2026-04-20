package com.omerkaya.sperrmuellfinder.core.di

import android.content.Context
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.NetworkManager
import com.omerkaya.sperrmuellfinder.core.util.PreferencesManager
import com.omerkaya.sperrmuellfinder.core.util.FirebaseErrorHandler
import com.omerkaya.sperrmuellfinder.core.util.GooglePlayServicesChecker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifier annotations for dependency injection
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/**
 * Core module for dependency injection with Hilt + KSP
 * Provides core utilities and managers for the application
 * 
 * Migration: Kapt → KSP for better performance and Java 17+ compatibility
 */
@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    /**
     * Provides Logger instance
     */
    @Provides
    @Singleton
    fun provideLogger(): Logger = Logger()

    /**
     * Provides IO Dispatcher
     */
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    /**
     * Provides Main Dispatcher
     */
    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    /**
     * Provides Default Dispatcher
     */
    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    /**
     * Provides Application CoroutineScope for long-running operations
     */
    @Provides
    @Singleton
    fun provideApplicationScope(): kotlinx.coroutines.CoroutineScope {
        return kotlinx.coroutines.CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())
    }

    /**
     * Provides PreferencesManager instance
     */
    @Provides
    @Singleton
    fun providePreferencesManager(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        logger: Logger
    ): PreferencesManager {
        return PreferencesManager(context, ioDispatcher, logger)
    }

    /**
     * Provides NetworkManager instance
     */
    @Provides
    @Singleton
    fun provideNetworkManager(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        logger: Logger
    ): NetworkManager {
        return NetworkManager(context, ioDispatcher, logger)
    }

    /**
     * Provides FirebaseErrorHandler instance for professional error handling
     */
    @Provides
    @Singleton
    fun provideFirebaseErrorHandler(logger: Logger): FirebaseErrorHandler {
        return FirebaseErrorHandler(logger)
    }

    /**
     * Provides GooglePlayServicesChecker instance for GPS availability checking
     */
    @Provides
    @Singleton
    fun provideGooglePlayServicesChecker(logger: Logger): GooglePlayServicesChecker {
        return GooglePlayServicesChecker(logger)
    }
}