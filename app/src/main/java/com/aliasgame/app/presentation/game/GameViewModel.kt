package com.aliasgame.app.presentation.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliasgame.app.domain.engine.GameEngine
import com.aliasgame.app.domain.repository.GameSessionRepository
import com.aliasgame.app.domain.repository.SettingsRepository
import com.aliasgame.app.domain.repository.WordsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Game screen.
 * Manages the active game session, timer, and unified UI state.
 * Supports session restoration to survive process death.
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    private val engine: GameEngine,
    private val settingsRepository: SettingsRepository,
    private val wordsRepository: WordsRepository,
    private val sessionRepository: GameSessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<GameUiEffect>()
    val uiEffect = _uiEffect.receiveAsFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            // Observe persistent preferences
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
            
            // Restore session if exists (to survive process death)
            val snapshot = sessionRepository.sessionSnapshot.first()
            if (snapshot != null) {
                try {
                    val words = wordsRepository.getWords(snapshot.language, snapshot.packId)
                    engine.restoreSession(snapshot, words)
                    _uiState.update { 
                        it.copy(
                            currentWord = snapshot.currentWord,
                            timeRemaining = snapshot.timeRemaining,
                            isRoundOver = snapshot.isRoundOver,
                            isPaused = true // Always restore in paused state for safety
                        )
                    }
                    syncState()
                } catch (e: Exception) {
                    // Fallback to new game if restoration fails
                    val firstWord = engine.getNextWord()
                    syncState(currentWord = firstWord)
                }
            } else {
                // Regular initialization
                val firstWord = engine.getNextWord()
                syncState(currentWord = firstWord)
            }
        }
    }

    fun onEntranceAnimationFinished() {
        _uiState.update { it.copy(isEntranceAnimationPlayed = true) }
    }

    /**
     * Synchronizes internal engine state with the observable UI state and persists the session.
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
            winners = winners,
            isTimerRunning = _uiState.value.isTimerRunning
        )
        
        _uiState.update { 
            it.copy(
                currentTeam = engineState.currentTeam,
                currentWord = engineState.currentWord,
                score = engineState.score,
                timeRemaining = engineState.timeRemaining,
                isTimerRunning = engineState.isTimerRunning,
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
        saveSession()
    }

    private fun saveSession() {
        viewModelScope.launch {
            val state = _uiState.value
            val lang = settingsRepository.selectedLanguage.first()
            val pack = settingsRepository.selectedPack.first()
            
            val snapshot = engine.createSessionSnapshot(
                timeRemaining = state.timeRemaining,
                isRoundOver = state.isRoundOver,
                language = lang,
                packId = pack
            )
            sessionRepository.saveSession(snapshot)
        }
    }

    fun onStartTimer() {
        if (timerJob?.isActive == true || _uiState.value.currentWord == null) return
        _uiState.update { it.copy(timeRemaining = engine.settings.roundTime, isTimerRunning = true) }
        startTimer()
    }

    private fun startTimer() {
        if (timerJob?.isActive == true || _uiState.value.currentWord == null) return
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
        _uiState.update { it.copy(isTimerRunning = false) }
        syncState(isRoundOver = true, winners = roundWinners)
    }

    fun toggleWordResult(index: Int) {
        if (index == _uiState.value.roundResults.size - 1) return
        
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
                timeRemaining = 0L,
                isTimerRunning = false
            ) 
        }
        syncState(currentWord = nextWord)
    }

    fun pauseGame() {
        timerJob?.cancel()
        _uiState.update { it.copy(isTimerRunning = false) }
        engine.pause()
        syncState()
    }

    fun resumeGame() {
        if (_uiState.value.currentWord == null) return
        _uiState.update { it.copy(isTimerRunning = true) }
        engine.resume()
        startTimer()
        syncState()
    }

    fun onExitGame() {
        viewModelScope.launch {
            sessionRepository.clearSession()
            _uiEffect.send(GameUiEffect.NavigateBack)
        }
    }
}
