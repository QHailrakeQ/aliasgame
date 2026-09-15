package com.aliasgame.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.aliasgame.app.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// Singleton delegate for DataStore access
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val ROUND_TIME = longPreferencesKey("round_time")
        val TARGET_SCORE = intPreferencesKey("target_score")
        val TEAM_NAMES = stringPreferencesKey("team_names")
        val SELECTED_PACK = stringPreferencesKey("selected_pack")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
    }

    override val selectedLanguage: Flow<String> = context.settingsDataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[Keys.LANGUAGE] ?: "EN" }

    override val roundTime: Flow<Long> = context.settingsDataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[Keys.ROUND_TIME] ?: 60L }

    override val targetScore: Flow<Int> = context.settingsDataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[Keys.TARGET_SCORE] ?: 50 }

    override val teamNames: Flow<List<String>> = context.settingsDataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[Keys.TEAM_NAMES]?.split(",") ?: listOf("Team 1", "Team 2") }

    override val selectedPack: Flow<String> = context.settingsDataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[Keys.SELECTED_PACK] ?: "Easy" }

    override val isSoundEnabled: Flow<Boolean> = context.settingsDataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[Keys.SOUND_ENABLED] ?: true }

    override val isVibrationEnabled: Flow<Boolean> = context.settingsDataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[Keys.VIBRATION_ENABLED] ?: true }

    override suspend fun saveLanguage(language: String) {
        context.settingsDataStore.edit { it[Keys.LANGUAGE] = language }
    }

    override suspend fun saveRoundTime(time: Long) {
        context.settingsDataStore.edit { it[Keys.ROUND_TIME] = time }
    }

    override suspend fun saveTargetScore(score: Int) {
        context.settingsDataStore.edit { it[Keys.TARGET_SCORE] = score }
    }

    override suspend fun saveTeamNames(names: List<String>) {
        context.settingsDataStore.edit { it[Keys.TEAM_NAMES] = names.joinToString(",") }
    }

    override suspend fun savePack(packId: String) {
        context.settingsDataStore.edit { it[Keys.SELECTED_PACK] = packId }
    }

    override suspend fun saveSoundEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    override suspend fun saveVibrationEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }
}
