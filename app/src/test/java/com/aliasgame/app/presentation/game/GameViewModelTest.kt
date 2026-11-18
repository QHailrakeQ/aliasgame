package com.aliasgame.app.presentation.game

import com.aliasgame.app.domain.engine.GameEngine
import com.aliasgame.app.domain.model.GameSettings
import com.aliasgame.app.domain.model.Team
import com.aliasgame.app.domain.model.Word
import com.aliasgame.app.domain.repository.GameSessionRepository
import com.aliasgame.app.domain.repository.GameSessionSnapshot
import com.aliasgame.app.domain.repository.SettingsRepository
import com.aliasgame.app.domain.repository.WordsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var engine: GameEngine
    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var wordsRepository: FakeWordsRepository
    private lateinit var sessionRepository: FakeGameSessionRepository
    private lateinit var viewModel: GameViewModel

    private val defaultSettings = GameSettings(roundTime = 60, targetScore = 50)
    private val testWord = Word("Test", "easy")
    private val testTeams = listOf(Team("1", "Team 1"), Team("2", "Team 2"))

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        engine = GameEngine(listOf(testWord), testTeams, defaultSettings)
        settingsRepository = FakeSettingsRepository()
        wordsRepository = FakeWordsRepository()
        sessionRepository = FakeGameSessionRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = GameViewModel(
            engine,
            settingsRepository,
            wordsRepository,
            sessionRepository
        )
    }

    @Test
    fun `init with saved session restores word and stays paused`() = runTest {
        val restoredWord = Word("Restored", "easy")
        val snapshot = GameSessionSnapshot(
            teams = testTeams,
            currentRoundResults = emptyList(),
            currentWord = restoredWord,
            currentTeamIndex = 0,
            usedWords = emptyList(),
            currentScore = 0,
            settings = defaultSettings,
            language = "EN",
            packId = "easy",
            timeRemaining = 30,
            isRoundOver = false
        )
        sessionRepository.snapshotFlow.value = snapshot
        wordsRepository.wordsResult = listOf(restoredWord)

        createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(restoredWord, state.currentWord)
        assertEquals(30L, state.timeRemaining)
        assertTrue(state.isPaused)
        assertFalse(state.isTimerRunning)
    }

    @Test
    fun `onStartTimer without current word does not start timer`() = runTest {
        // Setup engine with no words
        engine = GameEngine(emptyList(), testTeams, defaultSettings)
        createViewModel()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.currentWord)

        viewModel.onStartTimer()
        runCurrent()
        advanceTimeBy(2000)
        runCurrent()

        assertFalse(viewModel.uiState.value.isTimerRunning)
    }

    @Test
    fun `resumeGame without current word does not start timer`() = runTest {
        engine = GameEngine(emptyList(), testTeams, defaultSettings)
        createViewModel()
        advanceUntilIdle()

        viewModel.resumeGame()
        runCurrent()
        advanceTimeBy(2000)
        runCurrent()

        assertFalse(viewModel.uiState.value.isTimerRunning)
    }

    @Test
    fun `repeated start or resume calls do not create multiple timer loops`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onStartTimer()
        runCurrent()
        advanceTimeBy(500)
        runCurrent()

        // Call resume multiple times while running
        viewModel.resumeGame()
        viewModel.resumeGame()
        runCurrent()

        advanceTimeBy(1000)
        runCurrent()
        
        // After total > 1s of virtual time, timer should have ticked exactly once (at 1s mark)
        assertEquals(defaultSettings.roundTime - 1, viewModel.uiState.value.timeRemaining)
    }

    @Test
    fun `onStartTimer resets time remaining but resumeGame continues it`() = runTest {
        createViewModel()
        advanceUntilIdle()

        // Fresh start
        viewModel.onStartTimer()
        runCurrent()
        assertEquals(defaultSettings.roundTime, viewModel.uiState.value.timeRemaining)

        // Wait for 5 ticks
        advanceTimeBy(5000)
        runCurrent()
        val remaining = viewModel.uiState.value.timeRemaining
        assertEquals(defaultSettings.roundTime - 5, remaining)

        viewModel.pauseGame()
        runCurrent()
        assertFalse(viewModel.uiState.value.isTimerRunning)

        // Resume from where we left off
        viewModel.resumeGame()
        runCurrent()
        assertEquals(remaining, viewModel.uiState.value.timeRemaining)

        advanceTimeBy(2000)
        runCurrent()
        assertEquals(remaining - 2, viewModel.uiState.value.timeRemaining)
    }

    @Test
    fun `onExitGame clears saved session before requesting navigation`() = runTest {
        createViewModel()
        advanceUntilIdle()
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
        createViewModel()
        advanceUntilIdle()
        sessionRepository.operations.clear()

        val effect = async { viewModel.uiEffect.first() }
        runCurrent()
        viewModel.onStartNewGame()
        advanceUntilIdle()

        assertEquals(GameUiEffect.NavigateToSetup, effect.await())
        assertNull(sessionRepository.snapshotFlow.value)
        assertEquals(listOf("clear"), sessionRepository.operations)
    }

    @Test
    fun `pauseGame on the ready screen is ignored and does not skip the turn`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.pauseGame()
        viewModel.resumeGame()
        runCurrent()
        advanceTimeBy(2000)
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isPaused)
        assertFalse(state.isTimerRunning)
        assertFalse(state.isLastWordMode)
        assertEquals(0L, state.timeRemaining)
    }

    @Test
    fun `answers are ignored while the game is paused`() = runTest {
        createViewModel()
        advanceUntilIdle()
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
    fun `onStartTimer while paused does not reset the remaining time`() = runTest {
        createViewModel()
        advanceUntilIdle()
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
    fun `restoring a session on the ready screen shows the ready state`() = runTest {
        restoreFrom(snapshot(timeRemaining = 0, isRoundOver = false, isTurnInProgress = false))

        val state = viewModel.uiState.value
        assertFalse(state.isPaused)
        assertFalse(state.isTimerRunning)
        assertFalse(state.isLastWordMode)
        assertEquals(0L, state.timeRemaining)
    }

    @Test
    fun `restoring a session in last-word mode shows the last-word decision`() = runTest {
        restoreFrom(snapshot(timeRemaining = 0, isRoundOver = false, isTurnInProgress = true))

        val state = viewModel.uiState.value
        assertFalse(state.isPaused)
        assertTrue(state.isLastWordMode)
    }

    @Test
    fun `restoring a session on round results allows the next team to start normally`() = runTest {
        restoreFrom(snapshot(timeRemaining = 0, isRoundOver = true, isTurnInProgress = false))
        assertFalse(viewModel.uiState.value.isPaused)
        assertTrue(viewModel.uiState.value.isRoundOver)

        viewModel.startNextRound()
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isRoundOver)
        assertFalse(state.isPaused)
        assertFalse(state.isLastWordMode)
        assertEquals(0L, state.timeRemaining)
    }

    @Test
    fun `restoring a legacy mid-turn session without the turn flag resumes the timer`() = runTest {
        restoreFrom(snapshot(timeRemaining = 30, isRoundOver = false, isTurnInProgress = false))
        assertTrue(viewModel.uiState.value.isPaused)

        viewModel.resumeGame()
        runCurrent()
        advanceTimeBy(1000)
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isPaused)
        assertTrue(state.isTimerRunning)
        assertEquals(29L, state.timeRemaining)
    }

    @Test
    fun `saved session records whether the turn is in progress`() = runTest {
        createViewModel()
        advanceUntilIdle()
        assertEquals(false, sessionRepository.snapshotFlow.value?.isTurnInProgress)

        viewModel.onStartTimer()
        runCurrent()
        viewModel.pauseGame()
        advanceUntilIdle()

        assertEquals(true, sessionRepository.snapshotFlow.value?.isTurnInProgress)
    }

    private fun snapshot(
        timeRemaining: Long,
        isRoundOver: Boolean,
        isTurnInProgress: Boolean
    ) = GameSessionSnapshot(
        teams = testTeams,
        currentRoundResults = emptyList(),
        currentWord = testWord,
        currentTeamIndex = 0,
        usedWords = listOf(testWord),
        currentScore = 0,
        settings = defaultSettings,
        language = "EN",
        packId = "easy",
        timeRemaining = timeRemaining,
        isRoundOver = isRoundOver,
        isTurnInProgress = isTurnInProgress
    )

    private fun TestScope.restoreFrom(snapshot: GameSessionSnapshot) {
        sessionRepository.snapshotFlow.value = snapshot
        wordsRepository.wordsResult = listOf(testWord)
        createViewModel()
        advanceUntilIdle()
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
        override suspend fun getWords(language: String, packId: String) = wordsResult
        override suspend fun getLanguages() = listOf("EN")
        override suspend fun getPacks(language: String) = listOf("easy")
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
