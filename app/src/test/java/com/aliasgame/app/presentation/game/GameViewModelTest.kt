package com.aliasgame.app.presentation.game

import com.aliasgame.app.domain.engine.GameEngine
import com.aliasgame.app.domain.model.GamePhase
import com.aliasgame.app.domain.model.GameSettings
import com.aliasgame.app.domain.model.RoundResult
import com.aliasgame.app.domain.model.Word
import com.aliasgame.app.domain.model.WordPack
import com.aliasgame.app.domain.repository.GameSessionRepository
import com.aliasgame.app.domain.repository.GameSessionSnapshot
import com.aliasgame.app.domain.repository.SettingsRepository
import com.aliasgame.app.domain.repository.WordStatsRepository
import com.aliasgame.app.domain.repository.WordsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var wordsRepository: FakeWordsRepository
    private lateinit var sessionRepository: FakeGameSessionRepository
    private lateinit var wordStatsRepository: FakeWordStatsRepository
    private lateinit var viewModel: GameViewModel

    private val defaultSettings = GameSettings(roundTime = 60, targetScore = 50)
    private val testWord = Word("Test", "easy")

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        settingsRepository = FakeSettingsRepository()
        wordsRepository = FakeWordsRepository()
        sessionRepository = FakeGameSessionRepository()
        wordStatsRepository = FakeWordStatsRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Saves a fresh session, as the setup screen does, and opens the game screen. */
    private fun TestScope.startNewGame(words: List<Word> = listOf(testWord)) {
        wordsRepository.wordsResult = words
        sessionRepository.snapshotFlow.value =
            GameEngine.newSession(listOf("Team 1", "Team 2"), defaultSettings, "EN", "easy")
        createViewModel()
        advanceUntilIdle()
    }

    /** Opens the game screen on an existing session, e.g. after process death. */
    private fun TestScope.restoreFrom(snapshot: GameSessionSnapshot) {
        wordsRepository.wordsResult = listOf(testWord)
        sessionRepository.snapshotFlow.value = snapshot
        createViewModel()
        advanceUntilIdle()
    }

    private fun createViewModel() {
        viewModel = GameViewModel(settingsRepository, wordsRepository, sessionRepository, wordStatsRepository)
    }

    private fun snapshot(phase: GamePhase, timeRemaining: Long) =
        GameEngine.newSession(listOf("Team 1", "Team 2"), defaultSettings, "EN", "easy").copy(
            currentWord = testWord,
            usedWords = listOf(testWord),
            timeRemaining = timeRemaining,
            phase = phase
        )

    /** Starts the current team's turn and lets the countdown expire. */
    private fun TestScope.playUntilLastWord() {
        viewModel.onStartTimer()
        runCurrent()
        advanceTimeBy(defaultSettings.roundTime * 1000 + 1)
        runCurrent()
        assertTrue(viewModel.uiState.value.isLastWordMode)
    }

    // --- Loading -------------------------------------------------------------------------

    @Test
    fun `a new game opens on the ready screen of the first team`() = runTest {
        startNewGame()

        val state = viewModel.uiState.value
        assertEquals(GamePhase.READY_TO_START, state.phase)
        assertTrue(state.isReadyToStart)
        assertEquals("Team 1", state.currentTeam?.name)
        assertEquals(testWord, state.currentWord)
    }

    @Test
    fun `words are loaded for the language and pack the game was started with`() = runTest {
        sessionRepository.snapshotFlow.value =
            GameEngine.newSession(listOf("A", "B"), defaultSettings, "DE", "Hard")
        createViewModel()
        advanceUntilIdle()

        assertEquals("DE" to "Hard", wordsRepository.lastRequest)
    }

    @Test
    fun `without a saved session the screen navigates back`() = runTest {
        createViewModel()
        val navigation = async { viewModel.uiEffect.first() }
        advanceUntilIdle()

        assertEquals(GameUiEffect.NavigateBack, navigation.await())
    }

    @Test
    fun `init with saved session restores word and stays paused`() = runTest {
        restoreFrom(snapshot(GamePhase.RUNNING, timeRemaining = 30))

        val state = viewModel.uiState.value
        assertEquals(testWord, state.currentWord)
        assertEquals(30L, state.timeRemaining)
        assertTrue(state.isPaused)
        assertFalse(state.isTimerRunning)
    }

    // --- Timer ---------------------------------------------------------------------------

    @Test
    fun `onStartTimer without current word does not start timer`() = runTest {
        startNewGame(words = emptyList())
        assertNull(viewModel.uiState.value.currentWord)

        viewModel.onStartTimer()
        runCurrent()
        advanceTimeBy(2000)
        runCurrent()

        assertFalse(viewModel.uiState.value.isTimerRunning)
    }

    @Test
    fun `resumeGame without current word does not start timer`() = runTest {
        startNewGame(words = emptyList())

        viewModel.resumeGame()
        runCurrent()
        advanceTimeBy(2000)
        runCurrent()

        assertFalse(viewModel.uiState.value.isTimerRunning)
    }

    @Test
    fun `repeated start or resume calls do not create multiple timer loops`() = runTest {
        startNewGame()

        viewModel.onStartTimer()
        runCurrent()
        advanceTimeBy(500)
        runCurrent()
        viewModel.resumeGame()
        viewModel.onStartTimer()
        runCurrent()
        advanceTimeBy(1000)
        runCurrent()

        // After 1.5 s of virtual time the timer has ticked exactly once.
        assertEquals(defaultSettings.roundTime - 1, viewModel.uiState.value.timeRemaining)
    }

    @Test
    fun `onStartTimer resets time remaining but resumeGame continues it`() = runTest {
        startNewGame()

        viewModel.onStartTimer()
        runCurrent()
        assertEquals(defaultSettings.roundTime, viewModel.uiState.value.timeRemaining)

        advanceTimeBy(5000)
        runCurrent()
        val remaining = viewModel.uiState.value.timeRemaining
        assertEquals(defaultSettings.roundTime - 5, remaining)

        viewModel.pauseGame()
        runCurrent()
        assertFalse(viewModel.uiState.value.isTimerRunning)

        viewModel.resumeGame()
        runCurrent()
        assertEquals(remaining, viewModel.uiState.value.timeRemaining)

        advanceTimeBy(2000)
        runCurrent()
        assertEquals(remaining - 2, viewModel.uiState.value.timeRemaining)
    }

    @Test
    fun `onStartTimer while paused does not reset the remaining time`() = runTest {
        startNewGame()
        viewModel.onStartTimer()
        runCurrent()
        advanceTimeBy(5000)
        runCurrent()
        viewModel.pauseGame()
        runCurrent()

        viewModel.onStartTimer()
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.isPaused)
        assertEquals(defaultSettings.roundTime - 5, state.timeRemaining)
    }

    @Test
    fun `pauseGame on the ready screen is ignored and does not skip the turn`() = runTest {
        startNewGame()

        viewModel.pauseGame()
        viewModel.resumeGame()
        runCurrent()
        advanceTimeBy(2000)
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.isReadyToStart)
        assertFalse(state.isLastWordMode)
        assertEquals(0L, state.timeRemaining)
    }

    // --- Answers -------------------------------------------------------------------------

    @Test
    fun `answers are ignored while the game is paused`() = runTest {
        startNewGame()
        viewModel.onStartTimer()
        runCurrent()
        viewModel.pauseGame()
        runCurrent()
        val wordBefore = viewModel.uiState.value.currentWord

        viewModel.onWordSwiped(isCorrect = true)
        viewModel.onWordSwiped(isCorrect = false)
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(0, state.score)
        assertEquals(wordBefore, state.currentWord)
        assertTrue(state.roundResults.isEmpty())
    }

    @Test
    fun `processing the final word twice advances the turn only once`() = runTest {
        startNewGame()
        playUntilLastWord()

        viewModel.onFinalWordProcessed(winnerTeamId = "1")
        viewModel.onFinalWordProcessed(winnerTeamId = "1")
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals("2", state.currentTeam?.id)
        assertEquals(1, state.roundResults.size)
        assertEquals(1, state.allTeams.first { it.id == "1" }.score)
    }

    @Test
    fun `final word decision is ignored outside last-word mode`() = runTest {
        startNewGame()

        viewModel.onFinalWordProcessed(winnerTeamId = "1")
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals("1", state.currentTeam?.id)
        assertFalse(state.isRoundOver)
        assertTrue(state.allTeams.all { it.score == 0 })
    }

    @Test
    fun `starting the next round twice draws only one new word`() = runTest {
        startNewGame(words = List(5) { Word("Word $it", "easy") })
        playUntilLastWord()
        viewModel.onFinalWordProcessed(winnerTeamId = null)
        runCurrent()

        viewModel.startNextRound()
        runCurrent()
        val wordAfterFirstCall = viewModel.uiState.value.currentWord
        viewModel.startNextRound()
        runCurrent()

        assertEquals(wordAfterFirstCall, viewModel.uiState.value.currentWord)
        assertTrue(viewModel.uiState.value.isReadyToStart)
    }

    @Test
    fun `round results cannot be toggled while a turn is being played`() = runTest {
        startNewGame()
        viewModel.onStartTimer()
        runCurrent()
        viewModel.onWordSwiped(isCorrect = true)
        viewModel.onWordSwiped(isCorrect = true)
        runCurrent()

        viewModel.toggleWordResult(0)
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.roundResults.all { it.isCorrect })
        assertTrue(state.allTeams.all { it.score == 0 })
    }

    // --- Word statistics -----------------------------------------------------------------

    /** Plays one turn: guess, skip, then the last word nobody got. Ends on the review screen. */
    private fun TestScope.playTurnToReview() {
        viewModel.onStartTimer()
        runCurrent()
        viewModel.onWordSwiped(isCorrect = true)
        viewModel.onWordSwiped(isCorrect = false)
        runCurrent()
        advanceTimeBy(defaultSettings.roundTime * 1000 + 1)
        runCurrent()
        viewModel.onFinalWordProcessed(winnerTeamId = null)
        runCurrent()
        assertTrue(viewModel.uiState.value.isRoundOver)
    }

    @Test
    fun `the reviewed turn is recorded once with corrections when the next round starts`() = runTest {
        startNewGame(words = List(10) { Word("Word $it", "easy") })
        playTurnToReview()
        val reviewed = viewModel.uiState.value.roundResults
        viewModel.toggleWordResult(1) // the skipped word was guessed after all
        runCurrent()

        viewModel.startNextRound()
        advanceUntilIdle()
        viewModel.startNextRound() // ignored: no longer on the review screen
        advanceUntilIdle()

        assertEquals(1, wordStatsRepository.recordedTurns.size)
        val (language, results) = wordStatsRepository.recordedTurns.single()
        assertEquals("EN", language)
        assertEquals(reviewed.map { it.word }, results.map { it.word })
        assertEquals(listOf(true, true, false), results.map { it.isCorrect })
    }

    @Test
    fun `leaving the game from the review screen records the turn before navigating`() = runTest {
        startNewGame(words = List(10) { Word("Word $it", "easy") })
        playTurnToReview()

        // Sound effects of the turn are still queued in the effect channel ahead of navigation.
        val navigation = async { viewModel.uiEffect.first { it == GameUiEffect.NavigateBack } }
        viewModel.onExitGame()
        advanceUntilIdle()

        navigation.await()
        assertEquals(1, wordStatsRepository.recordedTurns.size)
    }

    @Test
    fun `leaving the game during a turn records nothing`() = runTest {
        startNewGame(words = List(10) { Word("Word $it", "easy") })
        viewModel.onStartTimer()
        runCurrent()
        viewModel.onWordSwiped(isCorrect = false)
        runCurrent()

        viewModel.onExitGame()
        advanceUntilIdle()

        assertTrue(wordStatsRepository.recordedTurns.isEmpty())
    }

    // --- Restoring sessions --------------------------------------------------------------

    @Test
    fun `restoring a session on the ready screen shows the ready state`() = runTest {
        restoreFrom(snapshot(GamePhase.READY_TO_START, timeRemaining = 0))

        val state = viewModel.uiState.value
        assertTrue(state.isReadyToStart)
        assertFalse(state.isPaused)
        assertFalse(state.isLastWordMode)
    }

    @Test
    fun `restoring a session in last-word mode shows the last-word decision`() = runTest {
        restoreFrom(snapshot(GamePhase.LAST_WORD, timeRemaining = 0))

        val state = viewModel.uiState.value
        assertFalse(state.isPaused)
        assertTrue(state.isLastWordMode)
    }

    @Test
    fun `restoring a session on round results allows the next team to start normally`() = runTest {
        restoreFrom(snapshot(GamePhase.ROUND_REVIEW, timeRemaining = 0))
        assertTrue(viewModel.uiState.value.isRoundOver)

        viewModel.startNextRound()
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.isReadyToStart)
        assertFalse(state.isPaused)
        assertFalse(state.isLastWordMode)
    }

    @Test
    fun `a restored paused turn resumes the countdown`() = runTest {
        restoreFrom(snapshot(GamePhase.PAUSED, timeRemaining = 30))

        viewModel.resumeGame()
        runCurrent()
        advanceTimeBy(1000)
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.isTimerRunning)
        assertEquals(29L, state.timeRemaining)
    }

    @Test
    fun `the saved session follows the game phase`() = runTest {
        startNewGame()
        assertEquals(GamePhase.READY_TO_START, sessionRepository.snapshotFlow.value?.phase)

        viewModel.onStartTimer()
        runCurrent()
        viewModel.pauseGame()
        advanceUntilIdle()

        assertEquals(GamePhase.PAUSED, sessionRepository.snapshotFlow.value?.phase)
    }

    // --- App lifecycle -------------------------------------------------------------------

    @Test
    fun `going to the background pauses a running countdown`() = runTest {
        startNewGame()
        viewModel.onStartTimer()
        runCurrent()
        advanceTimeBy(3000)
        runCurrent()

        viewModel.onAppBackgrounded()
        advanceTimeBy(10_000)
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.isPaused)
        assertEquals(defaultSettings.roundTime - 3, state.timeRemaining)
    }

    @Test
    fun `going to the background outside a running countdown changes nothing`() = runTest {
        startNewGame()

        viewModel.onAppBackgrounded()
        runCurrent()

        assertTrue(viewModel.uiState.value.isReadyToStart)
    }

    // --- Leaving the game ----------------------------------------------------------------

    @Test
    fun `onExitGame clears saved session before requesting navigation`() = runTest {
        startNewGame()
        sessionRepository.operations.clear()

        val effect = async { viewModel.uiEffect.first() }
        runCurrent()
        viewModel.onExitGame()
        advanceUntilIdle()

        assertEquals(GameUiEffect.NavigateBack, effect.await())
        assertNull(sessionRepository.snapshotFlow.value)
        assertEquals(listOf("clear"), sessionRepository.operations)
    }

    @Test
    fun `onStartNewGame clears saved session before requesting setup navigation`() = runTest {
        startNewGame()
        sessionRepository.operations.clear()

        val effect = async { viewModel.uiEffect.first() }
        runCurrent()
        viewModel.onStartNewGame()
        advanceUntilIdle()

        assertEquals(GameUiEffect.NavigateToSetup, effect.await())
        assertNull(sessionRepository.snapshotFlow.value)
        assertEquals(listOf("clear"), sessionRepository.operations)
    }

    // Fakes
    class FakeSettingsRepository : SettingsRepository {
        override val selectedLanguage = flowOf("EN")
        override val roundTime = flowOf(60L)
        override val targetScore = flowOf(50)
        override val teamNames = flowOf(listOf("Team 1", "Team 2"))
        override val selectedPack = flowOf("easy")
        override val isSoundEnabled = flowOf(true)
        override val isVibrationEnabled = flowOf(true)
        override suspend fun saveLanguage(language: String) {}
        override suspend fun saveRoundTime(time: Long) {}
        override suspend fun saveTargetScore(score: Int) {}
        override suspend fun saveTeamNames(names: List<String>) {}
        override suspend fun savePack(packId: String) {}
        override suspend fun saveSoundEnabled(enabled: Boolean) {}
        override suspend fun saveVibrationEnabled(enabled: Boolean) {}
    }

    class FakeWordsRepository : WordsRepository {
        var wordsResult = emptyList<Word>()
        var lastRequest: Pair<String, String>? = null
        override suspend fun getWords(language: String, packId: String): List<Word> {
            lastRequest = language to packId
            return wordsResult
        }
        override suspend fun getLanguages() = listOf("EN")
        override suspend fun getPacks(language: String) = listOf(WordPack("easy", 1))
    }

    class FakeWordStatsRepository : WordStatsRepository {
        val recordedTurns = mutableListOf<Pair<String, List<RoundResult>>>()
        override val stats = MutableStateFlow(emptyList<com.aliasgame.app.domain.model.WordStat>())
        override suspend fun recordTurn(language: String, results: List<RoundResult>) {
            recordedTurns += language to results
        }
        override suspend fun clear() {}
    }

    class FakeGameSessionRepository : GameSessionRepository {
        val snapshotFlow = MutableStateFlow<GameSessionSnapshot?>(null)
        val operations = mutableListOf<String>()
        override val sessionSnapshot = snapshotFlow
        override suspend fun saveSession(snapshot: GameSessionSnapshot) {
            operations += "save"
            snapshotFlow.value = snapshot
        }
        override suspend fun clearSession() {
            operations += "clear"
            snapshotFlow.value = null
        }
    }
}
