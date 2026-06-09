package com.aliasgame.app.di

import android.content.Context
import com.aliasgame.app.data.repository.WordsRepositoryImpl
import com.aliasgame.app.domain.engine.GameEngine
import com.aliasgame.app.domain.model.GameSettings
import com.aliasgame.app.domain.model.Team
import com.aliasgame.app.domain.model.Word
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
    fun provideGameEngine(
        repository: WordsRepository,
        settings: GameSettings
    ): GameEngine {
        val words = runBlocking {
            repository.getWordsByPack("basic")
        }

        val defaultTeams = listOf(
            Team("1", "Team 1"),
            Team("2", "Team 2")
        )

        val finalWords = if (words.isEmpty()) {
            listOf(Word("Apple", "basic"), Word("Banana", "basic"))
        } else {
            words
        }

        return GameEngine(finalWords, defaultTeams, settings)
    }
}
