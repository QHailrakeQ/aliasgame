package com.aliasgame.app.presentation.setup

import com.aliasgame.app.domain.model.GamePhase
import com.aliasgame.app.domain.model.TeamNameError
import com.aliasgame.app.domain.model.Word
import com.aliasgame.app.domain.model.WordPack
import com.aliasgame.app.domain.repository.SettingsRepository
import com.aliasgame.app.domain.repository.WordsRepository
import com.aliasgame.app.domain.repository.GameSessionRepository
import com.aliasgame.app.domain.repository.GameSessionSnapshot
import com.aliasgame.app.domain.usecase.GetPacksUseCase
import com.aliasgame.app.domain.usecase.GetWordsUseCase
import com.aliasgame.app.domain.usecase.SaveSettingsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SetupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var viewModel: SetupViewModel
    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var wordsRepository: FakeWordsRepository
    private lateinit var sessionRepository: FakeGameSessionRepository
    private val namePool = listOf("Wise Owls", "Sleepy Pandas", "Iron Ducks", "Fast Snails")

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        settingsRepository = FakeSettingsRepository()
        wordsRepository = FakeWordsRepository()
        sessionRepository = FakeGameSessionRepository()
        
        val getWordsUseCase = GetWordsUseCase(wordsRepository)
        val getPacksUseCase = GetPacksUseCase(wordsRepository)
        val saveSettingsUseCase = SaveSettingsUseCase(settingsRepository)

        viewModel = SetupViewModel(
            wordsRepository,
            settingsRepository,
            sessionRepository,
            getWordsUseCase,
            getPacksUseCase,
            saveSettingsUseCase
        )
        
        viewModel.onTeamNamePoolChanged(namePool)
        // Ensure init block collectors process initial repository values
        testDispatcher.scheduler.runCurrent()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onRoundTimeChanged clamps value to max limit`() = runTest {
        viewModel.onRoundTimeChanged(200L) // Above 120
        runCurrent()
        assertEquals(120L, viewModel.uiState.value.roundTime)
    }

    @Test
    fun `onRoundTimeChanged clamps value to min limit`() = runTest {
        viewModel.onRoundTimeChanged(5L) // Below 10
        runCurrent()
        assertEquals(10L, viewModel.uiState.value.roundTime)
    }

    @Test
    fun `onTargetScoreChanged updates state correctly`() = runTest {
        viewModel.onTargetScoreChanged(80)
        runCurrent()
        assertEquals(80, viewModel.uiState.value.targetScore)
    }

    @Test
    fun `stepper values are normalized to the nearest configured step`() = runTest {
        viewModel.onRoundTimeChanged(18L)
        viewModel.onTargetScoreChanged(13)

        assertEquals(20L, viewModel.uiState.value.roundTime)
        assertEquals(10, viewModel.uiState.value.targetScore)
    }

    @Test
    fun `clamping works when loading from repository`() = runTest {
        // Setup repo with invalid values
        settingsRepository.roundTimeFlow.value = 300L
        settingsRepository.targetScoreFlow.value = 5
        
        // Re-initialize viewmodel to trigger collect with new values
        val getWordsUseCase = GetWordsUseCase(wordsRepository)
        val getPacksUseCase = GetPacksUseCase(wordsRepository)
        val saveSettingsUseCase = SaveSettingsUseCase(settingsRepository)
        viewModel = SetupViewModel(
            wordsRepository, settingsRepository, sessionRepository,
            getWordsUseCase, getPacksUseCase, saveSettingsUseCase
        )
        
        advanceUntilIdle()
        
        assertEquals(120L, viewModel.uiState.value.roundTime)
        assertEquals(10, viewModel.uiState.value.targetScore)
    }

    @Test
    fun `stored values are normalized to valid stepper values`() = runTest {
        settingsRepository.roundTimeFlow.value = 18L
        settingsRepository.targetScoreFlow.value = 13

        advanceUntilIdle()

        assertEquals(20L, viewModel.uiState.value.roundTime)
        assertEquals(10, viewModel.uiState.value.targetScore)
    }

    @Test
    fun `starting a game with an empty pack reports an error instead of navigating`() = runTest {
        val effect = async { viewModel.uiEffect.first() }

        viewModel.startGame()
        advanceUntilIdle()

        assertEquals(SetupUiEffect.ShowNoWordsAvailable, effect.await())
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun `starting a game saves a fresh session with the chosen setup and navigates`() = runTest {
        wordsRepository.words = listOf(Word("Cat", "Easy"))
        viewModel.onTeamNameChanged(0, "T1")
        viewModel.onTeamNameChanged(1, " T2 ") // trimmed when the game starts
        viewModel.onRoundTimeChanged(30L)
        viewModel.onTargetScoreChanged(20)
        runCurrent()
        val effect = async { viewModel.uiEffect.first() }

        viewModel.startGame()
        advanceUntilIdle()

        assertEquals(SetupUiEffect.NavigateToGame, effect.await())
        val session = sessionRepository.saved!!
        assertEquals(listOf("T1", "T2"), session.teams.map { it.name })
        assertEquals(30L, session.settings.roundTime)
        assertEquals(20, session.settings.targetScore)
        assertEquals("EN" to "Easy", session.language to session.packId)
        assertEquals(GamePhase.READY_TO_START, session.phase)
    }

    // --- Team names -----------------------------------------------------------------------

    @Test
    fun `teams start with different random names from the pool, as many as last time`() = runTest {
        val names = viewModel.uiState.value.teamNames

        assertEquals(2, names.size) // the saved setup had two teams
        assertTrue(names.all { it in namePool })
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun `rerolling gives a new name that no other team uses`() = runTest {
        repeat(20) {
            val before = viewModel.uiState.value.teamNames
            viewModel.onTeamNameRerolled(0)
            val after = viewModel.uiState.value.teamNames

            assertNotEquals(before[0], after[0])
            assertNotEquals(after[1], after[0])
            assertEquals(before[1], after[1])
            assertTrue(after[0] in namePool)
        }
    }

    @Test
    fun `a cleared name blocks the start until a name is entered`() = runTest {
        wordsRepository.words = listOf(Word("Cat", "Easy"))
        viewModel.onTeamNameCleared(1)

        assertEquals("", viewModel.uiState.value.teamNames[1])
        assertEquals(listOf(null, TeamNameError.BLANK), viewModel.uiState.value.teamNameErrors)
        assertFalse(viewModel.uiState.value.canStartGame)
        viewModel.startGame()
        advanceUntilIdle()
        assertNull(sessionRepository.saved)

        viewModel.onTeamNameChanged(1, "Our own name")
        assertTrue(viewModel.uiState.value.canStartGame)
    }

    @Test
    fun `two teams with the same name cannot start a game`() = runTest {
        wordsRepository.words = listOf(Word("Cat", "Easy"))
        viewModel.onTeamNameChanged(0, "Owls")
        viewModel.onTeamNameChanged(1, " owls")

        assertEquals(
            listOf(TeamNameError.DUPLICATE, TeamNameError.DUPLICATE),
            viewModel.uiState.value.teamNameErrors
        )
        viewModel.startGame()
        advanceUntilIdle()
        assertNull(sessionRepository.saved)
    }

    @Test
    fun `team names are limited in length`() = runTest {
        viewModel.onTeamNameChanged(0, "x".repeat(100))

        assertEquals(24, viewModel.uiState.value.teamNames[0].length)
    }

    @Test
    fun `added teams get an unused name and at least two teams always remain`() = runTest {
        viewModel.onTeamAdded()
        val names = viewModel.uiState.value.teamNames
        assertEquals(3, names.size)
        assertEquals(3, names.toSet().size)

        viewModel.onTeamRemoved(0)
        viewModel.onTeamRemoved(0)

        assertEquals(2, viewModel.uiState.value.teamNames.size)
    }

    @Test
    fun `switching the language rerolls rolled names and keeps typed ones`() = runTest {
        viewModel.onTeamNameChanged(1, "Our own name")
        val ukrainianPool = listOf("Мудрі сови", "Сонні панди", "Залізні качки")

        viewModel.onTeamNamePoolChanged(ukrainianPool)
        runCurrent()

        val names = viewModel.uiState.value.teamNames
        assertTrue(names[0] in ukrainianPool)
        assertEquals("Our own name", names[1])
    }

    // Fakes
    private class FakeSettingsRepository : SettingsRepository {
        var savedPack: String? = null
        private val selectedPackFlow = MutableStateFlow("Easy")
        val roundTimeFlow = MutableStateFlow(60L)
        val targetScoreFlow = MutableStateFlow(50)
        
        override val selectedLanguage = flowOf("EN")
        override val roundTime = roundTimeFlow
        override val targetScore = targetScoreFlow
        override val teamNames = flowOf(listOf("T1", "T2"))
        override val selectedPack = selectedPackFlow
        override val isSoundEnabled = flowOf(true)
        override val isVibrationEnabled = flowOf(true)

        override suspend fun saveLanguage(language: String) {}
        override suspend fun saveRoundTime(time: Long) {}
        override suspend fun saveTargetScore(score: Int) {}
        override suspend fun saveTeamNames(names: List<String>) {}
        override suspend fun savePack(packId: String) {
            savedPack = packId
            selectedPackFlow.value = packId
        }
        override suspend fun saveSoundEnabled(enabled: Boolean) {}
        override suspend fun saveVibrationEnabled(enabled: Boolean) {}
    }

    class FakeWordsRepository : WordsRepository {
        var words: List<Word> = emptyList()
        override suspend fun getWords(language: String, packId: String): List<Word> = words
        override suspend fun getLanguages(): List<String> = listOf("EN", "UA")
        override suspend fun getPacks(language: String): List<WordPack> =
            listOf(WordPack("Easy", 10), WordPack("Medium", 10), WordPack("Hard", 10))
    }

    class FakeGameSessionRepository : GameSessionRepository {
        var saved: GameSessionSnapshot? = null
        override val sessionSnapshot = flowOf<GameSessionSnapshot?>(null)
        override suspend fun saveSession(snapshot: GameSessionSnapshot) {
            saved = snapshot
        }
        override suspend fun clearSession() {}
    }
}
