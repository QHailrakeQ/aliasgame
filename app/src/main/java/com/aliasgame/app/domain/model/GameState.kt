package com.aliasgame.app.domain.model

/**
 * Immutable snapshot of the current game state.
 * @param isTimerRunning tracking if the countdown is actively ticking.
 */
data class GameState(
    val currentTeam: Team,
    val currentWord: Word?,
    val score: Int = 0,
    val timeRemaining: Long,
    val isTimerRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isLastWordMode: Boolean = false,
    val isGameFinished: Boolean = false,
    val isRoundOver: Boolean = false,
    val winners: List<Team> = emptyList(),
    val allTeams: List<Team> = emptyList(),
    val maxTime: Long,
    val roundResults: List<RoundResult> = emptyList()
)
