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
 * Covers the main flow: Setup -> Category Selection -> Return to Setup -> Start Game.
 * This test assumes NAV-01 and GAME-02 are implemented.
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
    fun testSetupToGameFlow() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Resource strings for identifying UI elements
        val setupTitle = context.getString(R.string.setup_title).uppercase()
        val categoryTitle = context.getString(R.string.select_category_title)
        val easyPack = context.getString(R.string.difficulty_easy)
        val mediumPack = context.getString(R.string.difficulty_medium)
        val startGame = context.getString(R.string.start_game).uppercase()
        val readyButton = context.getString(R.string.ready_button).uppercase()

        // 1. Verify we are on the Setup Screen
        composeTestRule.onNodeWithText(setupTitle).assertIsDisplayed()

        // 2. Open Category Selection
        // The default pack is "Easy", so clicking it should open the selection screen
        composeTestRule.onNodeWithText(easyPack).performClick()

        // 3. Verify Selection Screen and pick "Medium"
        composeTestRule.onNodeWithText(categoryTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(mediumPack).performClick()

        // 4. Verify return to Setup Screen with updated selection
        // NAV-01 ensures this is immediate due to shared ViewModel
        composeTestRule.onNodeWithText(setupTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(mediumPack).assertIsDisplayed()

        // 5. Start Game
        // Default team names (Team 1, Team 2) should be present, so button is enabled
        composeTestRule.onNodeWithText(startGame).performClick()

        // 6. Verify Game Screen / Ready Overlay
        // We wait for the "I'm ready!" button to appear on the Game screen
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText(readyButton).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(readyButton).assertIsDisplayed()
    }
}
