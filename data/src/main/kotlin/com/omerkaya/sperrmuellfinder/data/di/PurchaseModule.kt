package com.omerkaya.sperrmuellfinder.data.di

import android.content.Context
import com.omerkaya.sperrmuellfinder.core.manager.PurchaseManagerInterface
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.data.manager.PurchaseManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * PROFESSIONAL HILT MODULE FOR PURCHASE MANAGER
 * 
 * Provides PurchaseManagerInterface implementation with proper dependency injection.
 * 
 * Features:
 * ✅ Clean architecture compliance
 * ✅ Interface-based dependency injection
 * ✅ Singleton scope for performance
 * ✅ Proper context injection
 * ✅ Logger integration
 */
@Module
@InstallIn(SingletonComponent::class)
object PurchaseModule {
    
    @Provides
    @Singleton
    fun providePurchaseManager(
        @ApplicationContext context: Context,
        logger: Logger
    ): PurchaseManagerInterface {
        return PurchaseManagerImpl(context, logger)
    }
}
