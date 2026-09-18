package com.aliasgame.app.presentation.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliasgame.app.domain.engine.GameEngine
import com.aliasgame.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Game screen.
 * Manages the active game session, timer, and unified UI state.
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    private val engine: GameEngine,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<GameUiEffect>()
    val uiEffect = _uiEffect.receiveAsFlow()

    private var timerJob: Job? = null

    init {
        // Observe persistent preferences
        viewModelScope.launch {
            launch {
                settingsRepository.isSoundEnabled.collect { enabled ->
                    _uiState.update { it.copy(isSoundEnabled = enabled) }
                }
            }
            launch {
                settingsRepository.isVibrationEnabled.collect { enabled ->
                    _uiState.update { it.copy(isVibrationEnabled = enabled) }
                }
            }
        }
        
        // Initialize game session
        val firstWord = engine.getNextWord()
        syncState(currentWord = firstWord)
    }

    /**
     * Synchronizes internal engine state with the observable UI state.
     */
    private fun syncState(
        currentWord: com.aliasgame.app.domain.model.Word? = _uiState.value.currentWord,
        isRoundOver: Boolean = _uiState.value.isRoundOver,
        winners: List<com.aliasgame.app.domain.model.Team> = _uiState.value.winners
    ) {
        val engineState = engine.getCurrentState(
            timeRemaining = _uiState.value.timeRemaining,
            currentWord = currentWord,
            isRoundOver = isRoundOver,
            winners = winners
        )
        
        _uiState.update { 
            it.copy(
                currentTeam = engineState.currentTeam,
                currentWord = engineState.currentWord,
                score = engineState.score,
                timeRemaining = engineState.timeRemaining,
                maxTime = engineState.maxTime,
                isPaused = engineState.isPaused,
                isLastWordMode = engineState.isLastWordMode,
                isRoundOver = engineState.isRoundOver,
                isGameFinished = engineState.isGameFinished,
                winners = engineState.winners,
                allTeams = engineState.allTeams,
                roundResults = engineState.roundResults
            )
        }
    }

    fun onStartTimer() {
        if (timerJob?.isActive == true) return
        _uiState.update { it.copy(timeRemaining = engine.settings.roundTime) }
        startTimer()
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (_uiState.value.timeRemaining > 0) {
                delay(1000)
                _uiState.update { it.copy(timeRemaining = it.timeRemaining - 1) }
                syncState()
            }
            _uiEffect.send(GameUiEffect.PlayTimerEndSound)
            syncState()
        }
    }

    fun onWordSwiped(isCorrect: Boolean) {
        if (_uiState.value.timeRemaining <= 0) return

        viewModelScope.launch {
            _uiEffect.send(if (isCorrect) GameUiEffect.PlayCorrectSound else GameUiEffect.PlaySkipSound)
        }

        val nextWord = if (isCorrect) engine.onCorrectAnswer() else engine.onSkipWord()
        syncState(currentWord = nextWord)
    }

    fun onFinalWordProcessed(winnerTeamId: String?) {
        engine.addFinalWordResult(isCorrect = winnerTeamId != null)
        winnerTeamId?.let { engine.addPointToTeam(it) }
        
        val roundWinners = engine.rollNextTeam()
        syncState(isRoundOver = true, winners = roundWinners)
    }

    fun toggleWordResult(index: Int) {
        engine.toggleWordResult(index)
        syncState()
    }

    fun startNextRound() {
        engine.prepareForNextRound()
        val nextWord = engine.getNextWord()
        _uiState.update { 
            it.copy(
                isRoundOver = false, 
                winners = emptyList(), 
                timeRemaining = engine.settings.roundTime 
            ) 
        }
        syncState(currentWord = nextWord)
    }

    fun pauseGame() {
        timerJob?.cancel()
        engine.pause()
        syncState()
    }

    fun resumeGame() {
        engine.resume()
        startTimer()
        syncState()
    }

    fun onExitGame() {
        viewModelScope.launch {
            _uiEffect.send(GameUiEffect.NavigateBack)
        }
    }
}
