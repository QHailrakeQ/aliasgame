package com.aliasgame.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.aliasgame.app.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// extension property to create DataStore instance
private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val ROUND_TIME = longPreferencesKey("round_time")
        val TARGET_SCORE = intPreferencesKey("target_score")
        val TEAM_NAMES = stringPreferencesKey("team_names")
    }

    override val selectedLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.LANGUAGE] ?: "EN"
    }

    override val roundTime: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[Keys.ROUND_TIME] ?: 60L
    }

    override val targetScore: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[Keys.TARGET_SCORE] ?: 50
    }

    override val teamNames: Flow<List<String>> = context.dataStore.data.map { preferences ->
        preferences[Keys.TEAM_NAMES]?.split(",") ?: listOf("Team 1", "Team 2")
    }

    override suspend fun saveLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.LANGUAGE] = language
        }
    }

    override suspend fun saveRoundTime(time: Long) {
        context.dataStore.edit { preferences ->
            preferences[Keys.ROUND_TIME] = time
        }
    }

    override suspend fun saveTargetScore(score: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.TARGET_SCORE] = score
        }
    }

    override suspend fun saveTeamNames(names: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[Keys.TEAM_NAMES] = names.joinToString(",")
        }
    }
}
