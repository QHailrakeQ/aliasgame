package com.aliasgame.app.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aliasgame.app.domain.model.RoundResult
import com.aliasgame.app.domain.model.WordStat
import com.aliasgame.app.domain.model.WordStatistics
import com.aliasgame.app.domain.repository.WordStatsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.wordStatsDataStore: DataStore<Preferences> by preferencesDataStore(name = "word_stats")

@Serializable
private data class WordStatDto(
    val language: String,
    val text: String,
    val shown: Int,
    val skipped: Int
)

/** JSON encoding of word statistics, kept free of Android so it can be unit-tested. */
internal object WordStatsCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(stats: List<WordStat>): String =
        json.encodeToString(stats.map { WordStatDto(it.language, it.text, it.shown, it.skipped) })

    /** @throws SerializationException if [encoded] is not a valid statistics document. */
    fun decode(encoded: String): List<WordStat> =
        json.decodeFromString<List<WordStatDto>>(encoded)
            .map { WordStat(it.language, it.text, it.shown, it.skipped) }
}

@Singleton
class WordStatsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : WordStatsRepository {

    private val statsKey = stringPreferencesKey("word_stats")

    override val stats: Flow<List<WordStat>> = context.wordStatsDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences.readStats() }

    override suspend fun recordTurn(language: String, results: List<RoundResult>) {
        if (results.isEmpty()) return
        try {
            // edit() is atomic: read, update and write happen as one transaction.
            context.wordStatsDataStore.edit { preferences ->
                val updated = WordStatistics.record(preferences.readStats(), language, results)
                preferences[statsKey] = WordStatsCodec.encode(updated)
            }
        } catch (e: IOException) {
            // Statistics are a nice-to-have: a failed write must not break leaving the game.
            Log.w(TAG, "Could not save word statistics", e)
        }
    }

    override suspend fun clear() {
        context.wordStatsDataStore.edit { it.remove(statsKey) }
    }

    private fun Preferences.readStats(): List<WordStat> {
        val encoded = this[statsKey] ?: return emptyList()
        return try {
            WordStatsCodec.decode(encoded)
        } catch (e: SerializationException) {
            // Unreadable statistics are not worth crashing for; start counting again.
            Log.w(TAG, "Discarding unreadable word statistics", e)
            emptyList()
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Discarding unreadable word statistics", e)
            emptyList()
        }
    }

    private companion object {
        const val TAG = "WordStatsRepository"
    }
}
