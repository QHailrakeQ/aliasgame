package com.aliasgame.app.domain.model

/**
 * Immutable view of a game session at one moment, produced by the game engine.
 */
data class GameState(
    val phase: GamePhase,
    val currentTeam: Team,
    val currentWord: Word?,
    /** Points earned by [currentTeam] in the current turn (not yet added to its score). */
    val turnScore: Int,
    val timeRemaining: Long,
    val roundTime: Long,
    val teams: List<Team>,
    val roundResults: List<RoundResult>,
    /** Non-empty only in [GamePhase.FINISHED]; more than one team means a draw. */
    val winners: List<Team>
)
