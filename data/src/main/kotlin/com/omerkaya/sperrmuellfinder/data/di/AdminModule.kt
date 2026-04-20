package com.omerkaya.sperrmuellfinder.data.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.data.repository.AdminRepositoryImpl
import com.omerkaya.sperrmuellfinder.domain.repository.AdminRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for admin-related dependencies
 * Rules.md compliant - Hilt DI
 */
@Module
@InstallIn(SingletonComponent::class)
object AdminModule {
    
    @Provides
    @Singleton
    fun provideAdminRepository(
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore,
        functions: FirebaseFunctions,
        logger: Logger
    ): AdminRepository {
        return AdminRepositoryImpl(
            firebaseAuth = firebaseAuth,
            firestore = firestore,
            functions = functions,
            logger = logger
        )
    }
}
