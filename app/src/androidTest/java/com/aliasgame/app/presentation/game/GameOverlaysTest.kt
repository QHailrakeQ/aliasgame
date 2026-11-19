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
import com.aliasgame.app.ui.theme.AliasGameTheme
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
        viewModel = newGameViewModelForTest()
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
    fun readyButtonWorksWhenTheFingerMovesSlightlyDuringTheTap() {
        // A real finger always moves a little between down and up. The overlay must not consume
        // those move events, otherwise the button below the finger cancels its click.
        composeTestRule.onNodeWithText(context.getString(R.string.ready_button).uppercase())
            .performTouchInput {
                down(center)
                moveBy(Offset(2.dp.toPx(), 2.dp.toPx()))
                moveBy(Offset(-1.dp.toPx(), 1.dp.toPx()))
                up()
            }

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.content_desc_pause))
            .assertIsDisplayed()
    }

    @Test
    fun gameControlsAreHiddenFromAccessibilityOnlyWhileAnOverlayIsShown() {
        val pause = context.getString(R.string.content_desc_pause)
        composeTestRule.onNodeWithContentDescription(pause).assertDoesNotExist()

        composeTestRule.onNodeWithText(context.getString(R.string.ready_button).uppercase())
            .performClick()

        composeTestRule.onNodeWithContentDescription(pause).assertIsDisplayed()
    }
}
