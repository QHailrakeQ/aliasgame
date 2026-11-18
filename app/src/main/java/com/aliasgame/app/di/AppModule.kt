package com.aliasgame.app.di

import android.content.Context
import com.aliasgame.app.data.repository.GameSessionRepositoryImpl
import com.aliasgame.app.data.repository.SettingsRepositoryImpl
import com.aliasgame.app.data.repository.WordsRepositoryImpl
import com.aliasgame.app.domain.repository.GameSessionRepository
import com.aliasgame.app.domain.repository.SettingsRepository
import com.aliasgame.app.domain.repository.WordsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideWordsRepository(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): WordsRepository {
        return WordsRepositoryImpl(context, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SettingsRepository {
        return SettingsRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideGameSessionRepository(
        @ApplicationContext context: Context
    ): GameSessionRepository {
        return GameSessionRepositoryImpl(context)
    }
}
