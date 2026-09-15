package com.aliasgame.app.presentation.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliasgame.app.domain.engine.GameEngine
import com.aliasgame.app.domain.model.GameState
import com.aliasgame.app.domain.model.Team
import com.aliasgame.app.domain.model.Word
import com.aliasgame.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Game screen.
 * Manages the active game session, timer, and reactive game state.
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    private val engine: GameEngine,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState = _gameState.asStateFlow()

    private var currentWord: Word? = null
    private var timeLeft = engine.settings.roundTime
    private var timerJob: Job? = null
    private var roundWinner: Team? = null
    private var isRoundOver = false

    // Persistent preferences observed during gameplay
    val isSoundEnabled: StateFlow<Boolean> = settingsRepository.isSoundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isVibrationEnabled: StateFlow<Boolean> = settingsRepository.isVibrationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        currentWord = engine.getNextWord()
        updateState()
    }

    private fun updateState() {
        _gameState.value = engine.getCurrentState(
            timeLeft,
            currentWord = currentWord,
            isRoundOver = isRoundOver,
            winner = roundWinner
        )
    }

    /**
     * Toggles the result of a specific word in the current round results.
     */
    fun toggleWordResult(index: Int) {
        engine.toggleWordResult(index)
        updateState()
    }

    /**
     * Resets parameters and prepares the engine for the next team's turn.
     */
    fun startNextRound() {
        engine.prepareForNextRound()
        isRoundOver = false
        roundWinner = null
        timeLeft = engine.settings.roundTime
        currentWord = engine.getNextWord()
        updateState()
    }

    fun onStartTimer() {
        startTimer()
    }

    private fun startTimer() {
        if (timerJob?.isActive == true) return
        timerJob = viewModelScope.launch {
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
                updateState()
            }
            onTimerFinished()
        }
    }

    private fun onTimerFinished() {
        updateState()
    }

    /**
     * Processes a word action (correct or skip) and fetches the next word.
     */
    fun onWordSwiped(isCorrect: Boolean) {
        if (timeLeft <= 0) return

        currentWord = if (isCorrect) {
            engine.onCorrectAnswer()
        } else {
            engine.onSkipWord()
        }
        updateState()
    }

    /**
     * Handles the result of the final "overtime" word.
     */
    fun onFinalWordProcessed(winnerTeamId: String?) {
        engine.addFinalWordResult(isCorrect = winnerTeamId != null)
        winnerTeamId?.let { engine.addPointToTeam(it) }
        finishRound()
    }

    private fun finishRound() {
        roundWinner = engine.rollNextTeam()
        isRoundOver = true
        updateState()
    }

    fun pauseGame() {
        timerJob?.cancel()
        engine.pause()
        updateState()
    }

    fun resumeGame() {
        engine.resume()
        startTimer()
    }
}
