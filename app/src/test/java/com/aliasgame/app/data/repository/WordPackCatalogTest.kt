package com.aliasgame.app.data.repository

import com.aliasgame.app.domain.model.Word
import com.aliasgame.app.domain.model.WordPack
import org.junit.Assert.assertEquals
import org.junit.Test

class WordPackCatalogTest {

    private val words = listOf(
        Word("Cat", "Easy"),
        Word("Dog", "Easy"),
        Word("Entropy", "Insane"),
        Word("Kettle", "Easy"),
        Word("Quark", "Insane"),
        Word("Bridge", "Medium")
    )

    @Test
    fun `packsOf counts the words of each pack in file order and appends the mixed pack`() {
        assertEquals(
            listOf(
                WordPack("Easy", 3),
                WordPack("Insane", 2),
                WordPack("Medium", 1),
                WordPack(WordPack.MIXED_ID, 6)
            ),
            WordPackCatalog.packsOf(words)
        )
    }

    @Test
    fun `packsOf offers no mixed pack when the language has a single pack`() {
        val singlePack = listOf(Word("Cat", "Easy"), Word("Dog", "Easy"))

        assertEquals(listOf(WordPack("Easy", 2)), WordPackCatalog.packsOf(singlePack))
    }

    @Test
    fun `packsOf of no words is empty`() {
        assertEquals(emptyList<WordPack>(), WordPackCatalog.packsOf(emptyList()))
    }

    @Test
    fun `wordsOf returns only the words of a regular pack`() {
        assertEquals(
            listOf(Word("Entropy", "Insane"), Word("Quark", "Insane")),
            WordPackCatalog.wordsOf(words, "Insane")
        )
    }

    @Test
    fun `wordsOf the mixed pack returns every word of the language`() {
        assertEquals(words, WordPackCatalog.wordsOf(words, WordPack.MIXED_ID))
    }
}
