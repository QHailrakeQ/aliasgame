package com.aliasgame.app.domain.repository

import com.aliasgame.app.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository for persisting the active game session to survive process death.
 */
interface GameSessionRepository {
    val sessionSnapshot: Flow<GameSessionSnapshot?>
    suspend fun saveSession(snapshot: GameSessionSnapshot)
    suspend fun clearSession()
}

/**
 * Data class representing all information needed to restore a game session.
 */
data class GameSessionSnapshot(
    val teams: List<Team>,
    val currentRoundResults: List<RoundResult>,
    val currentWord: Word?,
    val currentTeamIndex: Int,
    val usedWords: List<Word>,
    val currentScore: Int,
    val settings: GameSettings,
    val language: String,
    val packId: String,
    val timeRemaining: Long,
    val isRoundOver: Boolean
)
