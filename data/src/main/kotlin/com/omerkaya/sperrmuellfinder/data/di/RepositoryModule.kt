package com.omerkaya.sperrmuellfinder.data.di

import com.omerkaya.sperrmuellfinder.data.repository.CameraRepositoryImpl
import com.omerkaya.sperrmuellfinder.data.repository.FeedRepositoryImpl
import com.omerkaya.sperrmuellfinder.data.repository.FirestoreRepositoryImpl
import com.omerkaya.sperrmuellfinder.data.repository.LikesRepositoryImpl
import com.omerkaya.sperrmuellfinder.data.repository.LocationRepositoryImpl
import com.omerkaya.sperrmuellfinder.data.repository.NotificationRepositoryImpl
import com.omerkaya.sperrmuellfinder.data.repository.PostRepositoryImpl
import com.omerkaya.sperrmuellfinder.data.repository.SearchRepositoryImpl
import com.omerkaya.sperrmuellfinder.data.repository.SocialRepositoryImpl
import com.omerkaya.sperrmuellfinder.data.repository.UserRepositoryImpl
import com.omerkaya.sperrmuellfinder.domain.repository.CameraRepository
import com.omerkaya.sperrmuellfinder.domain.repository.FeedRepository
import com.omerkaya.sperrmuellfinder.domain.repository.FirestoreRepository
import com.omerkaya.sperrmuellfinder.domain.repository.LikesRepository
import com.omerkaya.sperrmuellfinder.domain.repository.LocationRepository
import com.omerkaya.sperrmuellfinder.domain.repository.NotificationRepository
import com.omerkaya.sperrmuellfinder.domain.repository.PostRepository
import com.omerkaya.sperrmuellfinder.domain.repository.SearchRepository
import com.omerkaya.sperrmuellfinder.domain.repository.SocialRepository
import com.omerkaya.sperrmuellfinder.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPostRepository(
        postRepositoryImpl: PostRepositoryImpl
    ): PostRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        locationRepositoryImpl: LocationRepositoryImpl
    ): LocationRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(
        searchRepositoryImpl: SearchRepositoryImpl
    ): SearchRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        notificationRepositoryImpl: NotificationRepositoryImpl
    ): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindFirestoreRepository(
        firestoreRepositoryImpl: FirestoreRepositoryImpl
    ): FirestoreRepository

    @Binds
    @Singleton
    abstract fun bindCameraRepository(
        cameraRepositoryImpl: CameraRepositoryImpl
    ): CameraRepository

    @Binds
    @Singleton
    abstract fun bindFeedRepository(
        feedRepositoryImpl: FeedRepositoryImpl
    ): FeedRepository

    @Binds
    @Singleton
    abstract fun bindSocialRepository(
        socialRepositoryImpl: SocialRepositoryImpl
    ): SocialRepository

    @Binds
    @Singleton
    abstract fun bindLikesRepository(
        likesRepositoryImpl: LikesRepositoryImpl
    ): LikesRepository
}