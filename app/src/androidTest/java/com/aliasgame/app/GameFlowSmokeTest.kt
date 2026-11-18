package com.aliasgame.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * UI Smoke test for AliasGame.
 * Updated in Phase 4 to cover Home, Rules, and Game Setup.
 */
@RunWith(AndroidJUnit4::class)
class GameFlowSmokeTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private lateinit var activityScenario: ActivityScenario<MainActivity>

    @Before
    fun clearDataStoreBeforeLaunchingActivity() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val datastoreDir = File(context.filesDir, "datastore")
        if (datastoreDir.exists()) {
            datastoreDir.deleteRecursively()
        }
        activityScenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun closeActivity() {
        activityScenario.close()
    }

    @Test
    fun testHomeToRulesFlow() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val rulesButton = context.getString(R.string.menu_rules)
        val rulesTitle = context.getString(R.string.rules_title)
        val goalSection = context.getString(R.string.rules_section_goal).uppercase()
        val backDesc = context.getString(R.string.content_desc_back)
        val appName = context.getString(R.string.app_name).uppercase()

        // 1. From Home, navigate to Rules
        composeTestRule.onNodeWithText(rulesButton).performClick()

        // 2. Verify Rules screen content
        composeTestRule.onNodeWithText(rulesTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(goalSection).assertIsDisplayed()

        // 3. Go back to Home
        composeTestRule.onNodeWithContentDescription(backDesc).performClick()
        composeTestRule.onNodeWithText(appName).assertIsDisplayed()
    }

    @Test
    fun testHomeToSettingsAndBack() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val settingsButton = context.getString(R.string.menu_settings)
        val languageTitle = context.getString(R.string.language).uppercase()
        val soundSetting = context.getString(R.string.sound_effects)
        val vibrationSetting = context.getString(R.string.haptic_feedback)
        val backDesc = context.getString(R.string.content_desc_back)
        val appName = context.getString(R.string.app_name).uppercase()

        composeTestRule.onNodeWithText(settingsButton).performClick()
        composeTestRule.onNodeWithText(languageTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(soundSetting).assertIsDisplayed()
        composeTestRule.onNodeWithText(vibrationSetting).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(backDesc).performClick()
        composeTestRule.onNodeWithText(appName).assertIsDisplayed()
    }

    @Test
    fun testHomeToGameFlow() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Resource strings for identifying UI elements
        val playButton = context.getString(R.string.menu_play)
        val setupTitle = context.getString(R.string.setup_title)
        val categoryTitle = context.getString(R.string.select_category_title)
        val easyPack = context.getString(R.string.difficulty_easy)
        val mediumPack = context.getString(R.string.difficulty_medium)
        val startGame = context.getString(R.string.start_game).uppercase()
        val readyButton = context.getString(R.string.ready_button).uppercase()

        // 1. Verify we are on the Home Screen and click Play
        composeTestRule.onNodeWithText(playButton).assertIsDisplayed().performClick()

        // 2. Verify we are on the Setup Screen
        composeTestRule.onNodeWithText(setupTitle).assertIsDisplayed()

        // 3. Open Category Selection
        // The default pack is "Easy", so clicking it should open the selection screen
        composeTestRule.onNodeWithText(easyPack).performClick()

        // 4. Verify Selection Screen and pick "Medium"
        composeTestRule.onNodeWithText(categoryTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(mediumPack).performClick()

        // 5. Verify return to Setup Screen with updated selection
        composeTestRule.onNodeWithText(setupTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(mediumPack).assertIsDisplayed()

        // 6. Start Game
        composeTestRule.onNodeWithText(startGame).performScrollTo().performClick()

        // 7. Verify Game Screen / Ready Overlay
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText(readyButton).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(readyButton).assertIsDisplayed()
    }

    @Test
    fun testExitButtonAndSystemBackRequireConfirmation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val exitDescription = context.getString(R.string.content_desc_exit)
        val exitTitle = context.getString(R.string.exit_confirmation_title)
        val cancelExit = context.getString(R.string.exit_confirmation_cancel)
        val confirmExit = context.getString(R.string.exit_confirmation_confirm)
        val playButton = context.getString(R.string.menu_play)
        val startGame = context.getString(R.string.start_game).uppercase()
        val readyButton = context.getString(R.string.ready_button).uppercase()
        val homeTitle = context.getString(R.string.app_name).uppercase()

        composeTestRule.onNodeWithText(playButton).performClick()
        composeTestRule.onNodeWithText(startGame).performScrollTo().performClick()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText(readyButton).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(readyButton).performClick()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithContentDescription(exitDescription)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithContentDescription(exitDescription).performClick()
        composeTestRule.onNodeWithText(exitTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(cancelExit).performClick()
        composeTestRule.onNodeWithContentDescription(exitDescription).assertIsDisplayed()

        pressSystemBack()
        composeTestRule.onNodeWithText(exitTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(cancelExit).performClick()

        pressSystemBack()
        composeTestRule.onNodeWithText(exitTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(confirmExit).performClick()
        // Exiting clears the saved session in DataStore (I/O the Compose test rule does not track)
        // before navigating, so wait for the home screen explicitly.
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText(homeTitle).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(homeTitle).assertIsDisplayed()
    }

    /**
     * Sends Back through the activity's OnBackPressedDispatcher, which is what the game's
     * BackHandler listens to. Unlike injecting a key event, this does not depend on the app
     * window having input focus, which slow CI emulators often lose to system
     * "not responding" dialogs.
     *
     * Waits for pending recompositions first: the game's BackHandler is only enabled again
     * after closing the exit dialog has been recomposed.
     */
    private fun pressSystemBack() {
        composeTestRule.waitForIdle()
        activityScenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeTestRule.waitForIdle()
    }
}
