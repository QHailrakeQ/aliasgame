package com.aliasgame.app.presentation.game

import com.aliasgame.app.domain.model.RoundResult
import com.aliasgame.app.domain.model.Team
import com.aliasgame.app.domain.model.Word

/**
 * Unified UI State for the Game screen.
 */
data class GameUiState(
    val currentTeam: Team? = null,
    val currentWord: Word? = null,
    val score: Int = 0,
    val timeRemaining: Long = 0,
    val maxTime: Long = 0,
    val isPaused: Boolean = false,
    val isLastWordMode: Boolean = false,
    val isRoundOver: Boolean = false,
    val isGameFinished: Boolean = false,
    val winners: List<Team> = emptyList(),
    val allTeams: List<Team> = emptyList(),
    val roundResults: List<RoundResult> = emptyList(),
    val isSoundEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true
)

/**
 * Side effects for the Game screen.
 */
sealed interface GameUiEffect {
    data object PlayCorrectSound : GameUiEffect
    data object PlaySkipSound : GameUiEffect
    data object PlayTimerEndSound : GameUiEffect
    data object NavigateBack : GameUiEffect
}
