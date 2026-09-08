package com.aliasgame.app.di

import android.content.Context
import com.aliasgame.app.data.repository.SettingsRepositoryImpl
import com.aliasgame.app.data.repository.WordsRepositoryImpl
import com.aliasgame.app.domain.engine.GameEngine
import com.aliasgame.app.domain.model.GameSettings
import com.aliasgame.app.domain.model.Team
import com.aliasgame.app.domain.model.Word
import com.aliasgame.app.domain.repository.SettingsRepository
import com.aliasgame.app.domain.repository.WordsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideWordsRepository(
        @ApplicationContext context: Context
    ): WordsRepository {
        return WordsRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideGameSettings(): GameSettings {
        return GameSettings(
            roundTime = 60,
            targetScore = 50,
            pointsPerCorrectAnswer = 1,
            pointsPerSkip = -1
        )
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
    fun provideGameEngine(
        repository: WordsRepository,
        settings: GameSettings
    ): GameEngine {
        // Initial setup with default parameters
        val words = runBlocking {
            repository.getWords("EN", "Easy")
        }

        val defaultTeams = listOf(
            Team("1", "Team 1"),
            Team("2", "Team 2")
        )

        val finalWords = words.ifEmpty {
            listOf(Word("Apple", "Easy"), Word("Banana", "Easy"))
        }

        return GameEngine(finalWords, defaultTeams, settings)
    }
}
