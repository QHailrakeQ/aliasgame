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
