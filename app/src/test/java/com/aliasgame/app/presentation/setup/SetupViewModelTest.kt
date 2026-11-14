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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onPackSelected updates UI state immediately`() = runTest {
        val newPack = "Hard"
        viewModel.onPackSelected(newPack)
        runCurrent()
        
        assertEquals(newPack, viewModel.uiState.value.selectedPack)
    }

    @Test
    fun `onPackSelected triggers persistence`() = runTest {
        val newPack = "Insane"
        viewModel.onPackSelected(newPack)
        
        advanceUntilIdle()
        
        assertEquals(newPack, settingsRepository.savedPack)
    }

    // Fakes
    private class FakeSettingsRepository : SettingsRepository {
        var savedPack: String? = null
        private val selectedPackFlow = MutableStateFlow("Easy")
        
        override val selectedLanguage = flowOf("EN")
        override val roundTime = flowOf(60L)
        override val targetScore = flowOf(50)
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
