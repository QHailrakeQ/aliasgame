package com.aliasgame.app.presentation.statistics

import com.aliasgame.app.domain.model.RoundResult
import com.aliasgame.app.domain.model.WordStat
import com.aliasgame.app.domain.repository.SettingsRepository
import com.aliasgame.app.domain.repository.WordStatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val statsRepository = FakeWordStatsRepository()
    private lateinit var viewModel: StatisticsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** uiState is shared only while collected, as it is by the screen. */
    private fun TestScope.openScreen(gameLanguage: String) {
        viewModel = StatisticsViewModel(FakeSettingsRepository(gameLanguage), statsRepository)
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()
    }

    @Test
    fun `shows the hardest words of the game language first`() = runTest {
        statsRepository.stats.value = listOf(
            WordStat("EN", "Kettle", shown = 3, skipped = 1),
            WordStat("UA", "Ентропія", shown = 2, skipped = 2),
            WordStat("UA", "Кіт", shown = 4, skipped = 0)
        )

        openScreen(gameLanguage = "UA")

        val state = viewModel.uiState.value
        assertEquals(listOf("EN", "UA"), state.languages)
        assertEquals("UA", state.selectedLanguage)
        assertEquals(listOf("Ентропія"), state.hardestWords.map { it.text })
    }

    @Test
    fun `falls back to the first language with skips when the game language has none`() = runTest {
        statsRepository.stats.value = listOf(WordStat("DE", "Schwer", shown = 1, skipped = 1))

        openScreen(gameLanguage = "EN")

        assertEquals("DE", viewModel.uiState.value.selectedLanguage)
    }

    @Test
    fun `picking a language switches the list`() = runTest {
        statsRepository.stats.value = listOf(
            WordStat("EN", "Kettle", shown = 3, skipped = 1),
            WordStat("DE", "Schwer", shown = 1, skipped = 1)
        )
        openScreen(gameLanguage = "EN")

        viewModel.onLanguageSelected("DE")
        advanceUntilIdle()

        assertEquals(listOf("Schwer"), viewModel.uiState.value.hardestWords.map { it.text })
    }

    @Test
    fun `resetting clears the statistics`() = runTest {
        statsRepository.stats.value = listOf(WordStat("EN", "Kettle", shown = 3, skipped = 1))
        openScreen(gameLanguage = "EN")

        viewModel.onResetConfirmed()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isLoaded)
        assertTrue(state.hardestWords.isEmpty())
        assertTrue(state.languages.isEmpty())
    }

    private class FakeWordStatsRepository : WordStatsRepository {
        override val stats = MutableStateFlow(emptyList<WordStat>())
        override suspend fun recordTurn(language: String, results: List<RoundResult>) {}
        override suspend fun clear() {
            stats.value = emptyList()
        }
    }

    private class FakeSettingsRepository(language: String) : SettingsRepository {
        override val selectedLanguage = flowOf(language)
        override val roundTime = flowOf(60L)
        override val targetScore = flowOf(50)
        override val teamNames = flowOf(listOf("Team 1", "Team 2"))
        override val selectedPack = flowOf("Easy")
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
}
