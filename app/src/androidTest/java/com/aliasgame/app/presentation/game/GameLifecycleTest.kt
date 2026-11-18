package com.aliasgame.app.presentation.game

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aliasgame.app.R
import com.aliasgame.app.ui.theme.AliasGameTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies that the countdown pauses when the game screen leaves the foreground. */
@RunWith(AndroidJUnit4::class)
class GameLifecycleTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun movingTheAppToTheBackgroundPausesTheRunningCountdown() {
        val viewModel = newGameViewModelForTest()
        composeTestRule.setContent {
            AliasGameTheme {
                GameScreen(viewModel = viewModel, onExit = {}, onNewGame = {})
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.ready_button).uppercase())
            .performClick()
        composeTestRule.waitForIdle()
        assertTrue(viewModel.uiState.value.isTimerRunning)

        // CREATED = stopped, i.e. the activity is no longer visible (home button, call, screen off).
        composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

        assertTrue(viewModel.uiState.value.isPaused)
        composeTestRule.onNodeWithText(context.getString(R.string.resume_button).uppercase())
            .assertIsDisplayed()
    }
}
