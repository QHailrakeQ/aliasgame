package com.aliasgame.app.presentation.settings

import com.aliasgame.app.domain.model.AppStyle
import com.aliasgame.app.domain.repository.SettingsRepository
import com.aliasgame.app.domain.usecase.SaveSettingsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: SettingsViewModel
    private lateinit var settingsRepository: FakeSettingsRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        settingsRepository = FakeSettingsRepository()
        val saveSettingsUseCase = SaveSettingsUseCase(settingsRepository)
        viewModel = SettingsViewModel(settingsRepository, saveSettingsUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loaded from repository`() = runTest {
        runCurrent()
        val state = viewModel.uiState.value
        assertEquals("EN", state.selectedLanguage)
        assertEquals(true, state.isSoundEnabled)
    }

    @Test
    fun `onSoundToggled updates repository`() = runTest {
        viewModel.onSoundToggled(false)
        runCurrent()
        assertEquals(false, settingsRepository.savedSoundEnabled)
    }

    @Test
    fun `onVibrationToggled updates repository`() = runTest {
        viewModel.onVibrationToggled(false)
        runCurrent()
        assertEquals(false, settingsRepository.savedVibrationEnabled)
    }

    @Test
    fun `the stored style is shown`() = runTest {
        runCurrent()
        assertEquals(AppStyle.NEON_VIOLET, viewModel.uiState.value.appStyle)
    }

    @Test
    fun `selecting a style shows and stores it`() = runTest {
        runCurrent()
        viewModel.onStyleSelected(AppStyle.CLASSIC)
        runCurrent()
        assertEquals(AppStyle.CLASSIC, viewModel.uiState.value.appStyle)
        assertEquals(AppStyle.CLASSIC, settingsRepository.savedAppStyle)
    }

    @Test
    fun `selecting the current style stores nothing`() = runTest {
        runCurrent()
        viewModel.onStyleSelected(AppStyle.NEON_VIOLET)
        runCurrent()
        assertNull(settingsRepository.savedAppStyle)
    }

    private class FakeSettingsRepository : SettingsRepository {
        var savedSoundEnabled: Boolean? = null
        var savedVibrationEnabled: Boolean? = null
        var savedAppStyle: AppStyle? = null

        override val selectedLanguage = flowOf("EN")
        override val roundTime = flowOf(60L)
        override val targetScore = flowOf(50)
        override val teamNames = flowOf(listOf("T1", "T2"))
        override val selectedPack = flowOf("Easy")
        override val isSoundEnabled = flowOf(true)
        override val isVibrationEnabled = flowOf(true)
        override val appStyle = flowOf(AppStyle.NEON_VIOLET)

        override suspend fun saveLanguage(language: String) {}
        override suspend fun saveRoundTime(time: Long) {}
        override suspend fun saveTargetScore(score: Int) {}
        override suspend fun saveTeamNames(names: List<String>) {}
        override suspend fun savePack(packId: String) {}
        override suspend fun saveSoundEnabled(enabled: Boolean) { savedSoundEnabled = enabled }
        override suspend fun saveVibrationEnabled(enabled: Boolean) { savedVibrationEnabled = enabled }
        override suspend fun saveAppStyle(style: AppStyle) { savedAppStyle = style }
    }
}
