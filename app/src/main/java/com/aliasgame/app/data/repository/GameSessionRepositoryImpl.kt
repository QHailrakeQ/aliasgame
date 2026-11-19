package com.aliasgame.app.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.aliasgame.app.domain.model.*
import com.aliasgame.app.domain.repository.GameSessionRepository
import com.aliasgame.app.domain.repository.GameSessionSnapshot
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

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "game_session")

@Serializable
private data class SessionWordDto(val text: String, val packId: String)

@Serializable
private data class SessionTeamDto(val id: String, val name: String, val score: Int)

@Serializable
private data class SessionRoundResultDto(val word: SessionWordDto, val isCorrect: Boolean)

@Serializable
private data class SessionGameSettingsDto(
    val roundTime: Long,
    val targetScore: Int,
    val pointsPerCorrectAnswer: Int,
    val pointsPerSkip: Int
)

@Serializable
private data class SessionSnapshotDto(
    val teams: List<SessionTeamDto>,
    val currentRoundResults: List<SessionRoundResultDto>,
    val currentWord: SessionWordDto?,
    val currentTeamIndex: Int,
    val usedWords: List<SessionWordDto>,
    val currentScore: Int,
    val settings: SessionGameSettingsDto,
    val language: String,
    val packId: String,
    val timeRemaining: Long,
    /** Name of the [GamePhase]; absent in snapshots written before the phase model existed. */
    val phase: String? = null,
    // Legacy fields, read only to migrate snapshots that have no `phase`.
    val isRoundOver: Boolean = false,
    val isTurnInProgress: Boolean = false
)

private fun Word.toDto() = SessionWordDto(text, packId)
private fun SessionWordDto.toDomain() = Word(text, packId)

private fun Team.toDto() = SessionTeamDto(id, name, score)
private fun SessionTeamDto.toDomain() = Team(id, name, score)

private fun RoundResult.toDto() = SessionRoundResultDto(word.toDto(), isCorrect)
private fun SessionRoundResultDto.toDomain() = RoundResult(word.toDomain(), isCorrect)

private fun GameSettings.toDto() = SessionGameSettingsDto(roundTime, targetScore, pointsPerCorrectAnswer, pointsPerSkip)
private fun SessionGameSettingsDto.toDomain() = GameSettings(roundTime, targetScore, pointsPerCorrectAnswer, pointsPerSkip)

private fun GameSessionSnapshot.toDto() = SessionSnapshotDto(
    teams = teams.map { it.toDto() },
    currentRoundResults = currentRoundResults.map { it.toDto() },
    currentWord = currentWord?.toDto(),
    currentTeamIndex = currentTeamIndex,
    usedWords = usedWords.map { it.toDto() },
    currentScore = currentScore,
    settings = settings.toDto(),
    language = language,
    packId = packId,
    timeRemaining = timeRemaining,
    phase = phase.name
)

private fun SessionSnapshotDto.toDomain() = GameSessionSnapshot(
    teams = teams.map { it.toDomain() },
    currentRoundResults = currentRoundResults.map { it.toDomain() },
    currentWord = currentWord?.toDomain(),
    currentTeamIndex = currentTeamIndex,
    usedWords = usedWords.map { it.toDomain() },
    currentScore = currentScore,
    settings = settings.toDomain(),
    language = language,
    packId = packId,
    timeRemaining = timeRemaining,
    phase = phase?.let { name -> GamePhase.entries.firstOrNull { it.name == name } }
        ?: legacyPhase()
)

/** Derives the phase for snapshots saved by app versions without an explicit phase. */
private fun SessionSnapshotDto.legacyPhase(): GamePhase = when {
    isRoundOver -> GamePhase.ROUND_REVIEW
    timeRemaining > 0 -> GamePhase.PAUSED
    isTurnInProgress -> GamePhase.LAST_WORD
    else -> GamePhase.READY_TO_START
}

/** JSON encoding of session snapshots, kept free of Android so it can be unit-tested. */
internal object SessionSnapshotCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(snapshot: GameSessionSnapshot): String = json.encodeToString(snapshot.toDto())

    /** @throws SerializationException if [encoded] is not a valid snapshot. */
    fun decode(encoded: String): GameSessionSnapshot =
        json.decodeFromString<SessionSnapshotDto>(encoded).toDomain()
}

@Singleton
class GameSessionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : GameSessionRepository {

    private val sessionKey = stringPreferencesKey("session_snapshot")

    override val sessionSnapshot: Flow<GameSessionSnapshot?> = context.sessionDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[sessionKey]?.let { encoded ->
                try {
                    SessionSnapshotCodec.decode(encoded)
                } catch (e: SerializationException) {
                    // A corrupt snapshot cannot be resumed; report it as "no session".
                    Log.w(TAG, "Discarding unreadable game session", e)
                    null
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "Discarding unreadable game session", e)
                    null
                }
            }
        }

    override suspend fun saveSession(snapshot: GameSessionSnapshot) {
        val encoded = SessionSnapshotCodec.encode(snapshot)
        context.sessionDataStore.edit { it[sessionKey] = encoded }
    }

    override suspend fun clearSession() {
        context.sessionDataStore.edit { it.remove(sessionKey) }
    }

    private companion object {
        const val TAG = "GameSessionRepository"
    }
}
