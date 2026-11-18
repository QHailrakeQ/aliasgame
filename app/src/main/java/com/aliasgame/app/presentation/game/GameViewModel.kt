package com.aliasgame.app.presentation.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliasgame.app.domain.engine.GameEngine
import com.aliasgame.app.domain.model.GamePhase
import com.aliasgame.app.domain.repository.GameSessionRepository
import com.aliasgame.app.domain.repository.SettingsRepository
import com.aliasgame.app.domain.repository.WordsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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
 *
 * Owns the [GameEngine] for the session shown on screen. The engine is always loaded from
 * the saved session: the setup screen saves a fresh session before navigating here, and the
 * same path restores a game after process death. Game rules and phase transitions live in the
 * engine; this class only drives the one-second countdown, persists the session and turns
 * engine state into UI state.
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val wordsRepository: WordsRepository,
    private val sessionRepository: GameSessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<GameUiEffect>()
    val uiEffect = _uiEffect.receiveAsFlow()

    /** `null` until the session has been loaded. */
    private var engine: GameEngine? = null
    private var timerJob: Job? = null
    private var sessionSaveJob: Job? = null
    private var isExiting = false

    init {
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
            loadSession()
        }
    }

    private suspend fun loadSession() {
        val snapshot = sessionRepository.sessionSnapshot.first()
        val loadedEngine = try {
            snapshot?.let { GameEngine.restore(it, wordsRepository.getWords(it.language, it.packId)) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Missing word pack or an inconsistent snapshot: the game cannot be continued.
            null
        }
        if (loadedEngine == null) {
            clearSessionAndNavigate(GameUiEffect.NavigateBack)
            return
        }
        engine = loadedEngine
        publish()
    }

    fun onEntranceAnimationFinished() {
        _uiState.update { it.copy(isEntranceAnimationPlayed = true) }
    }

    fun onStartTimer() = perform({ startTurn() }) { startTicking() }

    fun onWordSwiped(isCorrect: Boolean) = perform({ answer(isCorrect) }) {
        viewModelScope.launch {
            _uiEffect.send(if (isCorrect) GameUiEffect.PlayCorrectSound else GameUiEffect.PlaySkipSound)
        }
    }

    fun pauseGame() = perform({ pause() }) { timerJob?.cancel() }

    fun resumeGame() = perform({ resume() }) { startTicking() }

    fun onFinalWordProcessed(winnerTeamId: String?) = perform({ decideLastWord(winnerTeamId) })

    fun toggleWordResult(index: Int) = perform({ toggleResult(index) })

    fun startNextRound() = perform({ nextRound() })

    fun onExitGame() {
        clearSessionAndNavigate(GameUiEffect.NavigateBack)
    }

    fun onStartNewGame() {
        clearSessionAndNavigate(GameUiEffect.NavigateToSetup)
    }

    /**
     * Runs an engine [command]; only when the engine accepts it (the command is valid in the
     * current phase) is the UI updated, the session saved and [onAccepted] executed.
     */
    private inline fun perform(command: GameEngine.() -> Boolean, onAccepted: () -> Unit = {}) {
        val currentEngine = engine ?: return
        if (isExiting || !currentEngine.command()) return
        publish()
        onAccepted()
    }

    private fun startTicking() {
        if (timerJob?.isActive == true) return
        timerJob = viewModelScope.launch {
            while (engine?.state?.phase == GamePhase.RUNNING) {
                delay(1000)
                val timeRanOut = engine?.tick() == true
                publish()
                if (timeRanOut) _uiEffect.send(GameUiEffect.PlayTimerEndSound)
            }
        }
    }

    /** Copies the engine state into the UI state and persists the session. */
    private fun publish() {
        val currentEngine = engine ?: return
        val state = currentEngine.state
        _uiState.update {
            it.copy(
                phase = state.phase,
                currentTeam = state.currentTeam,
                currentWord = state.currentWord,
                score = state.turnScore,
                timeRemaining = state.timeRemaining,
                maxTime = state.roundTime,
                winners = state.winners,
                allTeams = state.teams,
                roundResults = state.roundResults
            )
        }
        saveSession(currentEngine)
    }

    private fun saveSession(currentEngine: GameEngine) {
        if (isExiting) return
        // Taken synchronously so the saved state matches what was just published.
        val snapshot = currentEngine.snapshot()
        val previousSave = sessionSaveJob
        sessionSaveJob = viewModelScope.launch {
            previousSave?.cancelAndJoin()
            if (isExiting) return@launch
            sessionRepository.saveSession(snapshot)
        }
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
