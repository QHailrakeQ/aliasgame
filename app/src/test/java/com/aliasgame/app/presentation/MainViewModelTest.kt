package com.aliasgame.app.presentation

import com.aliasgame.app.domain.model.AppStyle
import com.aliasgame.app.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val storedStyle = MutableSharedFlow<AppStyle>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the style is unknown until it has been read and then follows the settings`() = runTest {
        val viewModel = MainViewModel(FakeSettingsRepository(storedStyle))
        runCurrent()
        assertNull(viewModel.appStyle.value)

        storedStyle.emit(AppStyle.CLASSIC)
        runCurrent()
        assertEquals(AppStyle.CLASSIC, viewModel.appStyle.value)

        storedStyle.emit(AppStyle.NEON_VIOLET)
        runCurrent()
        assertEquals(AppStyle.NEON_VIOLET, viewModel.appStyle.value)
    }

    private class FakeSettingsRepository(override val appStyle: MutableSharedFlow<AppStyle>) : SettingsRepository {
        override val selectedLanguage = flowOf("EN")
        override val roundTime = flowOf(60L)
        override val targetScore = flowOf(50)
        override val teamNames = flowOf(listOf("T1", "T2"))
        override val selectedPack = flowOf("Easy")
        override val isSoundEnabled = flowOf(true)
        override val isVibrationEnabled = flowOf(true)
        override suspend fun saveSoundEnabled(enabled: Boolean) {}
        override suspend fun saveVibrationEnabled(enabled: Boolean) {}
        override suspend fun saveAppStyle(style: AppStyle) {}
        override suspend fun saveLanguage(language: String) {}
        override suspend fun saveRoundTime(time: Long) {}
        override suspend fun saveTargetScore(score: Int) {}
        override suspend fun saveTeamNames(names: List<String>) {}
        override suspend fun savePack(packId: String) {}
    }
}
