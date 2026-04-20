package com.omerkaya.sperrmuellfinder.data.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.data.repository.ReportRepositoryImpl
import com.omerkaya.sperrmuellfinder.domain.repository.ReportRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Report repository
 * Rules.md compliant - Dependency injection
 */
@Module
@InstallIn(SingletonComponent::class)
object ReportModule {

    @Provides
    @Singleton
    fun provideReportRepository(
        firestore: FirebaseFirestore,
        firebaseAuth: FirebaseAuth,
        logger: Logger
    ): ReportRepository {
        return ReportRepositoryImpl(firestore, firebaseAuth, logger)
    }
}
