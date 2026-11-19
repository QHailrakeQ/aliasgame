package com.aliasgame.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AppLanguagesTest {

    @Test
    fun `stored UA code maps to the ISO uk language tag`() {
        assertEquals("uk", AppLanguages.toLanguageTag("UA"))
        assertEquals("uk", AppLanguages.toLanguageTag("ua"))
    }

    @Test
    fun `other codes map to their lowercase language tag`() {
        assertEquals("en", AppLanguages.toLanguageTag("EN"))
        assertEquals("ru", AppLanguages.toLanguageTag("RU"))
        assertEquals("de", AppLanguages.toLanguageTag("DE"))
    }

    @Test
    fun `default language is supported`() {
        assertTrue(AppLanguages.DEFAULT in AppLanguages.supported)
    }

    /** Keeps the system language list (Android 13+) in sync with the in-app language list. */
    @Test
    fun `locales_config declares exactly the supported languages`() {
        val config = File("src/main/res/xml/locales_config.xml").readText()
        val declared = Regex("""android:name="([a-z]+)"""").findAll(config).map { it.groupValues[1] }.toSet()

        assertEquals(AppLanguages.supported.map(AppLanguages::toLanguageTag).toSet(), declared)
    }
}
