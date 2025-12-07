package com.example.catcom.di

import com.example.catcom.data.repository.AdoptionRepositoryImpl
import com.example.catcom.data.repository.AuthRepositoryImpl
import com.example.catcom.data.repository.ChatRepositoryImpl
import com.example.catcom.data.repository.EventRepositoryImpl
import com.example.catcom.data.repository.PostRepositoryImpl
import com.example.catcom.domain.repository.AdoptionRepository
import com.example.catcom.domain.repository.AuthRepository
import com.example.catcom.domain.repository.ChatRepository
import com.example.catcom.domain.repository.EventRepository
import com.example.catcom.domain.repository.PostRepository
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
    abstract fun bindAdoptionRepository(
        adoptionRepositoryImpl: AdoptionRepositoryImpl
    ): AdoptionRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindPostRepository(
        postRepositoryImpl: PostRepositoryImpl
    ): PostRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindEventRepository(
        eventRepositoryImpl: EventRepositoryImpl
    ): EventRepository
}
