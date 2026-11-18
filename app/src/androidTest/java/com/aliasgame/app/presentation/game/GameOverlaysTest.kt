package com.aliasgame.app.presentation.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aliasgame.app.R
import com.aliasgame.app.domain.engine.GameEngine
import com.aliasgame.app.domain.model.GameSettings
import com.aliasgame.app.domain.model.Team
import com.aliasgame.app.domain.model.Word
import com.aliasgame.app.domain.repository.GameSessionRepository
import com.aliasgame.app.domain.repository.GameSessionSnapshot
import com.aliasgame.app.domain.repository.SettingsRepository
import com.aliasgame.app.domain.repository.WordsRepository
import com.aliasgame.app.ui.theme.AliasGameTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies that full-screen game overlays block the controls drawn underneath them,
 * both for touch and for accessibility services.
 */
@RunWith(AndroidJUnit4::class)
class GameOverlaysTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var viewModel: GameViewModel

    @Before
    fun setUp() {
        val engine = GameEngine(
            allWords = List(10) { Word("Word $it", "Easy") },
            initialTeams = listOf(Team("1", "Team A"), Team("2", "Team B")),
            settings = GameSettings(roundTime = 60, targetScore = 50)
        )
        viewModel = GameViewModel(
            engine,
            FakeSettingsRepository(),
            FakeWordsRepository(),
            FakeGameSessionRepository()
        )
        composeTestRule.setContent {
            AliasGameTheme {
                GameScreen(viewModel = viewModel, onExit = {}, onNewGame = {})
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.ready_button).uppercase())
            .assertIsDisplayed()
    }

    @Test
    fun tappingTheExitButtonBehindTheReadyOverlayDoesNothing() {
        // Centre of the HUD exit button: column padding 24dp + HUD padding 12dp + half of a
        // 48dp IconButton horizontally; HUD top padding 12dp + 8dp + half the button vertically.
        composeTestRule.onRoot().performTouchInput {
            click(Offset(60.dp.toPx(), 44.dp.toPx()))
        }

        composeTestRule.onNodeWithText(context.getString(R.string.exit_confirmation_title))
            .assertDoesNotExist()
    }

    @Test
    fun gameControlsAreHiddenFromAccessibilityOnlyWhileAnOverlayIsShown() {
        val pause = context.getString(R.string.content_desc_pause)
        composeTestRule.onNodeWithContentDescription(pause).assertDoesNotExist()

        composeTestRule.onNodeWithText(context.getString(R.string.ready_button).uppercase())
            .performClick()

        composeTestRule.onNodeWithContentDescription(pause).assertIsDisplayed()
    }

    private class FakeSettingsRepository : SettingsRepository {
        override val selectedLanguage = flowOf("EN")
        override val roundTime = flowOf(60L)
        override val targetScore = flowOf(50)
        override val teamNames = flowOf(listOf("Team A", "Team B"))
        override val selectedPack = flowOf("Easy")
        override val isSoundEnabled = flowOf(false)
        override val isVibrationEnabled = flowOf(false)
        override suspend fun saveLanguage(language: String) {}
        override suspend fun saveRoundTime(time: Long) {}
        override suspend fun saveTargetScore(score: Int) {}
        override suspend fun saveTeamNames(names: List<String>) {}
        override suspend fun savePack(packId: String) {}
        override suspend fun saveSoundEnabled(enabled: Boolean) {}
        override suspend fun saveVibrationEnabled(enabled: Boolean) {}
    }

    private class FakeWordsRepository : WordsRepository {
        override suspend fun getWords(language: String, packId: String) = emptyList<Word>()
        override suspend fun getLanguages() = listOf("EN")
        override suspend fun getPacks(language: String) = listOf("Easy")
    }

    private class FakeGameSessionRepository : GameSessionRepository {
        override val sessionSnapshot = MutableStateFlow<GameSessionSnapshot?>(null)
        override suspend fun saveSession(snapshot: GameSessionSnapshot) {
            sessionSnapshot.value = snapshot
        }
        override suspend fun clearSession() {
            sessionSnapshot.value = null
        }
    }
}
