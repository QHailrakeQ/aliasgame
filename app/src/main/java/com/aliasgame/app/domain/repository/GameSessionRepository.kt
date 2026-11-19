package com.aliasgame.app.domain.repository

import com.aliasgame.app.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository for the active game session. A new game is started by saving a fresh
 * snapshot, and the game screen always loads its state from here, so starting a game
 * and restoring one after process death follow the same path.
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
    val phase: GamePhase
)
