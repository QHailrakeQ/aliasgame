package com.aliasgame.app.presentation.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliasgame.app.domain.engine.GameEngine
import com.aliasgame.app.domain.repository.GameSessionRepository
import com.aliasgame.app.domain.repository.SettingsRepository
import com.aliasgame.app.domain.repository.WordsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
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
    private var sessionSaveJob: Job? = null
    private var isExiting = false

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
                    // Snapshots from older versions lack isTurnInProgress; time left means a turn was running.
                    val turnInProgress = !snapshot.isRoundOver &&
                        (snapshot.isTurnInProgress || snapshot.timeRemaining > 0)
                    // An interrupted countdown resumes paused; last-word mode (no time left) resumes as is.
                    val restoreAsPaused = turnInProgress && snapshot.timeRemaining > 0
                    if (restoreAsPaused) engine.pause()
                    _uiState.update {
                        it.copy(
                            currentWord = snapshot.currentWord,
                            timeRemaining = snapshot.timeRemaining,
                            isRoundOver = snapshot.isRoundOver,
                            isTimerRunning = turnInProgress && !restoreAsPaused
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
        if (isExiting) return

        val previousSave = sessionSaveJob
        sessionSaveJob = viewModelScope.launch {
            previousSave?.cancelAndJoin()
            if (isExiting) return@launch

            val state = _uiState.value
            val lang = settingsRepository.selectedLanguage.first()
            val pack = settingsRepository.selectedPack.first()
            
            val snapshot = engine.createSessionSnapshot(
                timeRemaining = state.timeRemaining,
                isRoundOver = state.isRoundOver,
                language = lang,
                packId = pack,
                isTurnInProgress = !state.isRoundOver && (state.isTimerRunning || state.isPaused)
            )
            sessionRepository.saveSession(snapshot)
        }
    }

    // Phase checks: every user action is validated against the current phase, so a stray tap
    // (e.g. on a control visible behind an overlay) cannot move the game into an invalid state.

    /** The team is on the "ready" screen and has not started its countdown yet. */
    private fun GameUiState.isWaitingForTurnStart() =
        currentWord != null && !isRoundOver && !isPaused && !isTimerRunning && timeRemaining == 0L

    /** The countdown is actively running and words can be answered. */
    private fun GameUiState.isCountdownActive() =
        isTimerRunning && !isPaused && !isRoundOver && timeRemaining > 0

    fun onStartTimer() {
        if (timerJob?.isActive == true || !_uiState.value.isWaitingForTurnStart()) return
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
        if (!_uiState.value.isCountdownActive()) return

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
        if (!_uiState.value.isCountdownActive()) return
        timerJob?.cancel()
        _uiState.update { it.copy(isTimerRunning = false) }
        engine.pause()
        syncState()
    }

    fun resumeGame() {
        val state = _uiState.value
        // Only an interrupted countdown can be resumed; resuming with no time left would
        // jump straight into last-word mode and cost the team its turn.
        if (!state.isPaused || state.currentWord == null || state.timeRemaining <= 0) return
        _uiState.update { it.copy(isTimerRunning = true) }
        engine.resume()
        startTimer()
        syncState()
    }

    fun onExitGame() {
        clearSessionAndNavigate(GameUiEffect.NavigateBack)
    }

    fun onStartNewGame() {
        clearSessionAndNavigate(GameUiEffect.NavigateToSetup)
    }

    private fun clearSessionAndNavigate(effect: GameUiEffect) {
        if (isExiting) return
        isExiting = true
        timerJob?.cancel()

        viewModelScope.launch {
            sessionSaveJob?.cancelAndJoin()
            sessionRepository.clearSession()
            _uiEffect.send(effect)
        }
    }
}
