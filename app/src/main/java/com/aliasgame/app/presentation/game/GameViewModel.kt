package com.aliasgame.app.presentation.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliasgame.app.domain.engine.GameEngine
import com.aliasgame.app.domain.model.GamePhase
import com.aliasgame.app.domain.model.RoundResult
import com.aliasgame.app.domain.repository.GameSessionRepository
import com.aliasgame.app.domain.repository.SettingsRepository
import com.aliasgame.app.domain.repository.WordStatsRepository
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
 *
 * The session is saved after every accepted command (start, answer, pause, last word, review
 * corrections, next round) and when the time runs out, but not on every countdown tick. A
 * restored turn is always paused, and going to the background pauses and saves the turn, so
 * after the process is killed in the background the game continues with the exact remaining
 * time. Only a crash while the countdown is on screen gives the team back the seconds since
 * the last saved event.
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val wordsRepository: WordsRepository,
    private val sessionRepository: GameSessionRepository,
    private val wordStatsRepository: WordStatsRepository
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

    fun onStartTimer() = perform({ startTurn() }) {
        playSound(GameSound.TURN_START)
        startTicking()
    }

    fun onWordSwiped(isCorrect: Boolean) = perform({ answer(isCorrect) }) {
        playSound(if (isCorrect) GameSound.CORRECT else GameSound.SKIP)
    }

    fun pauseGame() = perform({ pause() }) { timerJob?.cancel() }

    fun resumeGame() = perform({ resume() }) { startTicking() }

    /**
     * The game screen left the foreground (home button, incoming call, screen off). A running
     * countdown is paused so the team does not lose time while nobody is playing; the players
     * resume it explicitly. Has no effect in any other phase.
     */
    fun onAppBackgrounded() = pauseGame()

    fun onFinalWordProcessed(winnerTeamId: String?) = perform({ decideLastWord(winnerTeamId) }) {
        playSound(
            when {
                isGameFinished() -> GameSound.VICTORY
                winnerTeamId != null -> GameSound.CORRECT
                else -> GameSound.SKIP
            }
        )
    }

    // A correction on the review screen can complete the win condition and finish the game.
    fun toggleWordResult(index: Int) = perform({ toggleResult(index) }) {
        if (isGameFinished()) playSound(GameSound.VICTORY)
    }

    fun startNextRound() {
        // Taken before nextRound() clears the reviewed results.
        val reviewedTurn = reviewedTurnOrNull()
        perform({ nextRound() }) {
            reviewedTurn?.let { (language, results) ->
                viewModelScope.launch { wordStatsRepository.recordTurn(language, results) }
            }
        }
    }

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
                // A plain tick only updates the screen; running out of time changes the phase.
                publish(persist = timeRanOut)
                if (timeRanOut) {
                    playSound(GameSound.TIMER_END)
                } else {
                    countdownSoundFor(_uiState.value.timeRemaining)?.let(::playSound)
                }
            }
        }
    }

    private fun isGameFinished() = engine?.state?.phase == GamePhase.FINISHED

    /**
     * Requests [sound] without suspending the caller: the countdown must keep its pace even
     * when nobody is collecting effects at that moment (for example in unit tests).
     */
    private fun playSound(sound: GameSound) {
        viewModelScope.launch { _uiEffect.send(GameUiEffect.PlaySound(sound)) }
    }

    /** Copies the engine state into the UI state and, if [persist] is set, saves the session. */
    private fun publish(persist: Boolean = true) {
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
        if (persist) saveSession(currentEngine)
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

    /**
     * The language and final results of the turn on the review screen, or `null` outside of it.
     * A turn is recorded exactly once: when the review is left, after any corrections.
     */
    private fun reviewedTurnOrNull(): Pair<String, List<RoundResult>>? {
        val currentEngine = engine ?: return null
        val state = currentEngine.state
        if (state.phase != GamePhase.ROUND_REVIEW && state.phase != GamePhase.FINISHED) return null
        return state.roundResults.takeIf { it.isNotEmpty() }?.let { currentEngine.language to it }
    }

    private fun clearSessionAndNavigate(effect: GameUiEffect) {
        if (isExiting) return
        isExiting = true
        timerJob?.cancel()
        // Leaving from the results or victory screen still counts the turn that was reviewed.
        val reviewedTurn = reviewedTurnOrNull()

        viewModelScope.launch {
            // Recorded before navigating: the ViewModel scope ends when the screen is left.
            reviewedTurn?.let { (language, results) -> wordStatsRepository.recordTurn(language, results) }
            sessionSaveJob?.cancelAndJoin()
            sessionRepository.clearSession()
            _uiEffect.send(effect)
        }
    }

    internal companion object {
        /** The countdown ticks audibly during the last [COUNTDOWN_SECONDS] seconds of a turn. */
        const val COUNTDOWN_SECONDS = 5L

        /** The last [FINAL_COUNTDOWN_SECONDS] seconds tick in a higher pitch. */
        const val FINAL_COUNTDOWN_SECONDS = 3L

        /** The tick for [secondsLeft] remaining seconds of a running turn, or `null` for silence. */
        fun countdownSoundFor(secondsLeft: Long): GameSound? = when (secondsLeft) {
            in 1..FINAL_COUNTDOWN_SECONDS -> GameSound.COUNTDOWN_TICK_FINAL
            in 1..COUNTDOWN_SECONDS -> GameSound.COUNTDOWN_TICK
            else -> null
        }
    }
}
