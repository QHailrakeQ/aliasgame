package com.aliasgame.app.presentation.setup

import com.aliasgame.app.domain.engine.GameEngine
import com.aliasgame.app.domain.model.GameSettings
import com.aliasgame.app.domain.model.Word
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SetupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var viewModel: SetupViewModel
    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var wordsRepository: FakeWordsRepository
    private lateinit var sessionRepository: FakeGameSessionRepository
    private lateinit var engine: GameEngine

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        settingsRepository = FakeSettingsRepository()
        wordsRepository = FakeWordsRepository()
        sessionRepository = FakeGameSessionRepository()
        engine = GameEngine(emptyList(), emptyList(), GameSettings(60, 50))
        
        val getWordsUseCase = GetWordsUseCase(wordsRepository)
        val getPacksUseCase = GetPacksUseCase(wordsRepository)
        val saveSettingsUseCase = SaveSettingsUseCase(settingsRepository)

        viewModel = SetupViewModel(
            engine,
            wordsRepository,
            settingsRepository,
            sessionRepository,
            getWordsUseCase,
            getPacksUseCase,
            saveSettingsUseCase
        )
        
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
            engine, wordsRepository, settingsRepository, sessionRepository,
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

    private class FakeWordsRepository : WordsRepository {
        override suspend fun getWords(language: String, packId: String): List<Word> = emptyList()
        override suspend fun getLanguages(): List<String> = listOf("EN", "UA")
        override suspend fun getPacks(language: String): List<String> = listOf("Easy", "Medium", "Hard")
    }

    private class FakeGameSessionRepository : GameSessionRepository {
        override val sessionSnapshot = flowOf<GameSessionSnapshot?>(null)
        override suspend fun saveSession(snapshot: GameSessionSnapshot) {}
        override suspend fun clearSession() {}
    }
}
