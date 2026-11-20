package com.aliasgame.app.presentation.game

import com.aliasgame.app.domain.model.GamePhase
import com.aliasgame.app.domain.model.RoundResult
import com.aliasgame.app.domain.model.Team
import com.aliasgame.app.domain.model.Word

/**
 * Unified UI State for the Game screen.
 *
 * [phase] is the single source of truth for what the screen shows; the boolean properties
 * below are derived from it for readability. `null` means the session is still loading.
 */
data class GameUiState(
    val phase: GamePhase? = null,
    val currentTeam: Team? = null,
    val currentWord: Word? = null,
    val score: Int = 0,
    val timeRemaining: Long = 0,
    val maxTime: Long = 0,
    val winners: List<Team> = emptyList(),
    val allTeams: List<Team> = emptyList(),
    val roundResults: List<RoundResult> = emptyList(),
    val isSoundEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true,
    val isEntranceAnimationPlayed: Boolean = false
) {
    val isReadyToStart: Boolean get() = phase == GamePhase.READY_TO_START && currentWord != null
    val isTimerRunning: Boolean get() = phase == GamePhase.RUNNING
    val isPaused: Boolean get() = phase == GamePhase.PAUSED
    val isLastWordMode: Boolean get() = phase == GamePhase.LAST_WORD
    /** True on the round-results screen and after the game ended (results stay underneath). */
    val isRoundOver: Boolean get() = phase == GamePhase.ROUND_REVIEW || phase == GamePhase.FINISHED
    val isGameFinished: Boolean get() = phase == GamePhase.FINISHED
}

/**
 * Side effects for the Game screen.
 */
sealed interface GameUiEffect {
    /** Plays [sound] unless sound effects are turned off in the settings. */
    data class PlaySound(val sound: GameSound) : GameUiEffect
    data object NavigateBack : GameUiEffect
    data object NavigateToSetup : GameUiEffect
}
