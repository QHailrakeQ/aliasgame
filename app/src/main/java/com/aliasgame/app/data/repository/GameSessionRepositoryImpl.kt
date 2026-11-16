package com.aliasgame.app.data.repository

import android.content.Context
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
    val isRoundOver: Boolean
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
    isRoundOver = isRoundOver
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
    isRoundOver = isRoundOver
)

@Singleton
class GameSessionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : GameSessionRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val SESSION_KEY = stringPreferencesKey("session_snapshot")

    override val sessionSnapshot: Flow<GameSessionSnapshot?> = context.sessionDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[SESSION_KEY]?.let { encoded ->
                try {
                    json.decodeFromString<SessionSnapshotDto>(encoded).toDomain()
                } catch (e: Exception) {
                    null
                }
            }
        }

    override suspend fun saveSession(snapshot: GameSessionSnapshot) {
        val encoded = json.encodeToString(snapshot.toDto())
        context.sessionDataStore.edit { it[SESSION_KEY] = encoded }
    }

    override suspend fun clearSession() {
        context.sessionDataStore.edit { it.remove(SESSION_KEY) }
    }
}
