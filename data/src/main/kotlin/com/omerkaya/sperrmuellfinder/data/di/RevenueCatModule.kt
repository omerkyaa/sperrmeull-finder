package com.omerkaya.sperrmuellfinder.data.di

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.data.manager.RevenueCatManager
import com.omerkaya.sperrmuellfinder.data.repository.PremiumRepositoryImpl
import com.omerkaya.sperrmuellfinder.domain.repository.PremiumRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing RevenueCat and Premium related dependencies.
 * 
 * According to rules.md:
 * - RevenueCat is configured as singleton
 * - PremiumRepository is bound to RevenueCat implementation
 * - All premium dependencies are centralized here
 */
@Module
@InstallIn(SingletonComponent::class)
object RevenueCatModule {

    @Provides
    @Singleton
    fun provideRevenueCatManager(
        application: Application,
        logger: Logger
    ): RevenueCatManager {
        return RevenueCatManager(application, logger)
    }

    @Provides
    @Singleton
    fun providePremiumRepository(
        revenueCatManager: RevenueCatManager,
        firestore: FirebaseFirestore,
        firebaseAuth: FirebaseAuth,
        logger: Logger
    ): PremiumRepository {
        return PremiumRepositoryImpl(
            revenueCatManager = revenueCatManager,
            firestore = firestore,
            firebaseAuth = firebaseAuth,
            logger = logger
        )
    }
}
