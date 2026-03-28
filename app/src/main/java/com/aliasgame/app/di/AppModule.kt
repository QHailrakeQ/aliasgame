package com.aliasgame.app.di

import com.aliasgame.app.data.repository.WordsRepositoryImpl
import com.aliasgame.app.domain.engine.GameEngine
import com.aliasgame.app.domain.model.GameSettings
import com.aliasgame.app.domain.model.Team
import com.aliasgame.app.domain.model.Word
import com.aliasgame.app.domain.repository.WordsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideWordsRepository(): WordsRepository {
        return WordsRepositoryImpl()
    }


    @Provides
    fun provideGameEngine(): GameEngine {
        return GameEngine(
            allWords = listOf(
                Word("Cat", "1"),
                Word("Dog", "2"),
                Word("Bird", "3"),
                Word("Fish", "4"),

            ),
            initialTeams = listOf(
                Team("1", "Lions", 0),
                Team("2", "Tigers", 0)
            ),
            settings = GameSettings(roundTime = 60, targetScore = 50)
        )
    }
}


