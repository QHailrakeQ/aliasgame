package com.aliasgame.app.data.repository

import com.aliasgame.app.domain.model.Word
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WordPackParserTest {

    @Test
    fun `parse maps every entry to a domain word and ignores unknown keys`() {
        val document = """
            [
              {"text": "Cat", "packId": "Easy", "extra": 1},
              {"text": "Entropy", "packId": "Insane"}
            ]
        """.trimIndent()

        val words = WordPackParser.parse(document)

        assertEquals(listOf(Word("Cat", "Easy"), Word("Entropy", "Insane")), words)
    }

    @Test(expected = SerializationException::class)
    fun `parse fails loudly on malformed documents instead of returning an empty list`() {
        WordPackParser.parse("""[{"text": "Cat"}]""")
    }

    @Test
    fun `fileNameFor maps the UA setting to the uk asset and lowercases other codes`() {
        assertEquals("words_uk.json", WordPackParser.fileNameFor("UA"))
        assertEquals("words_en.json", WordPackParser.fileNameFor("EN"))
        assertEquals("words_de.json", WordPackParser.fileNameFor("de"))
    }

    @Test
    fun `parseTheme reads a plain list of words and tags them with the theme`() {
        assertEquals(
            listOf(Word("Espresso", "coffee_shop"), Word("Latte", "coffee_shop")),
            WordPackParser.parseTheme("""["Espresso", "Latte"]""", "coffee_shop")
        )
    }

    @Test
    fun `theme files are found per language and named after their file`() {
        assertEquals("themes/uk", WordPackParser.themeDirFor("UA"))
        assertEquals("themes/en", WordPackParser.themeDirFor("EN"))
        assertEquals("cinema", WordPackParser.themeIdOf("cinema.json"))
        assertEquals(null, WordPackParser.themeIdOf("notes.txt"))
    }

    /**
     * Validates the bundled theme files: every language offers the same themes, and each theme
     * has words without blanks or duplicates (compared ignoring case and surrounding spaces).
     */
    @Test
    fun `every language ships the same parseable themes without blank or duplicate words`() {
        val themeSets = mutableMapOf<String, Set<String>>()

        for (language in listOf("EN", "UA", "RU", "DE")) {
            val dir = File("src/main/assets", WordPackParser.themeDirFor(language))
            assertTrue("Missing theme folder ${dir.path}", dir.isDirectory)
            val files = dir.listFiles().orEmpty().filter { WordPackParser.themeIdOf(it.name) != null }
            themeSets[language] = files.map { it.name }.toSet()

            for (file in files) {
                val words = WordPackParser.parseTheme(file.readText(), WordPackParser.themeIdOf(file.name)!!)
                val label = "${dir.name}/${file.name}"
                assertTrue("Empty theme $label", words.isNotEmpty())
                assertTrue("Blank word in $label", words.none { it.text.isBlank() })
                val duplicates = words.groupBy { it.text.trim().lowercase() }.filterValues { it.size > 1 }.keys
                assertTrue("Duplicate words in $label: $duplicates", duplicates.isEmpty())
            }
        }

        assertEquals("Every language must offer the same themes", 1, themeSets.values.toSet().size)
    }

    /**
     * Validates the real bundled word packs, so a broken asset is caught by CI
     * rather than by players. Unit tests run with the module directory as working directory.
     */
    @Test
    fun `every supported language ships a parseable pack file with all categories`() {
        val assetsDir = File("src/main/assets")
        val expectedPacks = setOf("Easy", "Medium", "Hard", "Insane")

        for (language in listOf("EN", "UA", "RU", "DE")) {
            val file = File(assetsDir, WordPackParser.fileNameFor(language))
            assertTrue("Missing asset ${file.path}", file.exists())

            val words = WordPackParser.parse(file.readText())

            assertEquals("Packs in ${file.name}", expectedPacks, words.map { it.packId }.toSet())
            assertTrue("Blank word in ${file.name}", words.none { it.text.isBlank() })
        }
    }
}
