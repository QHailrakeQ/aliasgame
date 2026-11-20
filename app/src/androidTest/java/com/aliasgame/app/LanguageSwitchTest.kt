package com.aliasgame.app

import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale

/**
 * Switching the language in the settings shows the new language right away. On Android 13+
 * the activity handles the locale change itself (configChanges in the manifest) instead of
 * being recreated, which made the whole screen flash; older versions recreate it.
 */
@RunWith(AndroidJUnit4::class)
class LanguageSwitchTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var activityScenario: ActivityScenario<MainActivity>

    @Before
    fun launchWithFreshSettings() {
        File(context.filesDir, "datastore").deleteRecursively()
        activityScenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun restoreDeviceLanguage() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        }
        activityScenario.close()
        File(context.filesDir, "datastore").deleteRecursively()
    }

    @Test
    fun switchingTheLanguageUpdatesTheScreenWithoutRecreatingTheActivity() {
        val ukrainian = context.createConfigurationContext(
            Configuration(context.resources.configuration).apply { setLocale(Locale("uk")) }
        )
        val ukrainianLanguageTitle = ukrainian.getString(R.string.language).uppercase()

        composeTestRule.onNodeWithText(context.getString(R.string.menu_settings)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.language).uppercase()).assertIsDisplayed()
        var activityBefore: Activity? = null
        activityScenario.onActivity { activityBefore = it }

        composeTestRule.onNodeWithText("UA").performClick()

        // DataStore and the locale change complete asynchronously.
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText(ukrainianLanguageTitle).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(ukrainianLanguageTitle).assertIsDisplayed()
        // Only Android 13+ switches in place; older versions recreate the screen on purpose.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activityScenario.onActivity { assertSame("The activity was recreated", activityBefore, it) }
        }
    }
}
