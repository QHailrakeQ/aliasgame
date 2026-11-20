package com.aliasgame.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.preferencesDataStore
import com.aliasgame.app.domain.model.AppLanguages
import com.aliasgame.app.domain.model.AppStyle
import com.aliasgame.app.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// Singleton delegate for DataStore access
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Unambiguous codec for team names that supports legacy CSV format.
 * Encodes names as a JSON array with a versioned prefix.
 */
internal object TeamNamesCodec {
    private const val PREFIX = "JSON_V1:"

    fun encode(names: List<String>): String {
        return PREFIX + Json.encodeToString(names)
    }

    fun decode(encoded: String): List<String> {
        if (encoded.startsWith(PREFIX)) {
            // New format: starts with prefix, followed by JSON array
            return Json.decodeFromString<List<String>>(encoded.removePrefix(PREFIX))
        }
        // Legacy format: comma-separated values
        return encoded.split(",")
    }
}

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
        val APP_STYLE = stringPreferencesKey("app_style")
    }

    override val selectedLanguage: Flow<String> = context.settingsDataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[Keys.LANGUAGE] ?: deviceLanguage() }

    /**
     * Until the player picks a language, follow the device: Android shows the interface in the
     * first supported language of this list, so the words have to come from the same one.
     */
    private fun deviceLanguage(): String {
        val locales = LocaleListCompat.getAdjustedDefault()
        return AppLanguages.fromDeviceLanguages(List(locales.size()) { locales[it]!!.toLanguageTag() })
    }

    override val roundTime: Flow<Long> = context.settingsDataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[Keys.ROUND_TIME] ?: 60L }

    override val targetScore: Flow<Int> = context.settingsDataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[Keys.TARGET_SCORE] ?: 50 }

    override val teamNames: Flow<List<String>> = context.settingsDataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[Keys.TEAM_NAMES]?.let { encoded -> TeamNamesCodec.decode(encoded) } ?: listOf("Team 1", "Team 2") }

    override val selectedPack: Flow<String> = context.settingsDataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[Keys.SELECTED_PACK] ?: "Easy" }

    override val isSoundEnabled: Flow<Boolean> = context.settingsDataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[Keys.SOUND_ENABLED] ?: true }

    override val isVibrationEnabled: Flow<Boolean> = context.settingsDataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[Keys.VIBRATION_ENABLED] ?: true }

    override val appStyle: Flow<AppStyle> = context.settingsDataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { AppStyle.fromId(it[Keys.APP_STYLE]) }

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
        context.settingsDataStore.edit { it[Keys.TEAM_NAMES] = TeamNamesCodec.encode(names) }
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

    override suspend fun saveAppStyle(style: AppStyle) {
        context.settingsDataStore.edit { it[Keys.APP_STYLE] = style.id }
    }
}
