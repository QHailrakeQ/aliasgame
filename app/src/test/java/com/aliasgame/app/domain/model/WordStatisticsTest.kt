package com.aliasgame.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WordStatisticsTest {

    private fun result(text: String, isCorrect: Boolean) = RoundResult(Word(text, "Easy"), isCorrect)

    @Test
    fun `record counts every result as shown and the not guessed ones as skipped`() {
        val stats = WordStatistics.record(
            emptyList(),
            "EN",
            listOf(result("Cat", isCorrect = true), result("Dog", isCorrect = false))
        )

        assertEquals(
            listOf(WordStat("EN", "Cat", shown = 1, skipped = 0), WordStat("EN", "Dog", shown = 1, skipped = 1)),
            stats
        )
    }

    @Test
    fun `record adds to existing counters of the same word and language only`() {
        val existing = listOf(
            WordStat("EN", "Cat", shown = 2, skipped = 1),
            WordStat("DE", "Cat", shown = 5, skipped = 5)
        )

        val stats = WordStatistics.record(existing, "EN", listOf(result("Cat", isCorrect = false)))

        assertEquals(
            listOf(WordStat("EN", "Cat", shown = 3, skipped = 2), WordStat("DE", "Cat", shown = 5, skipped = 5)),
            stats
        )
    }

    @Test
    fun `hardest ranks by skips, then by fewer showings, then alphabetically`() {
        val stats = listOf(
            WordStat("EN", "Often shown", shown = 10, skipped = 3),
            WordStat("EN", "Rarely shown", shown = 3, skipped = 3),
            WordStat("EN", "Most skipped", shown = 9, skipped = 6),
            WordStat("EN", "Beta", shown = 4, skipped = 2),
            WordStat("EN", "Alpha", shown = 4, skipped = 2)
        )

        assertEquals(
            listOf("Most skipped", "Rarely shown", "Often shown", "Alpha", "Beta"),
            WordStatistics.hardest(stats, "EN").map { it.text }
        )
    }

    @Test
    fun `hardest leaves out other languages and words that were never skipped`() {
        val stats = listOf(
            WordStat("EN", "Easy one", shown = 8, skipped = 0),
            WordStat("DE", "Schwer", shown = 2, skipped = 2),
            WordStat("EN", "Hard one", shown = 2, skipped = 1)
        )

        assertEquals(listOf("Hard one"), WordStatistics.hardest(stats, "EN").map { it.text })
    }

    @Test
    fun `hardest returns at most the requested number of words`() {
        val stats = List(30) { WordStat("EN", "Word $it", shown = 5, skipped = 1 + it) }

        val top = WordStatistics.hardest(stats, "EN", limit = 20)

        assertEquals(20, top.size)
        assertEquals("Word 29", top.first().text)
    }

    @Test
    fun `languagesWithSkips follows the app language order`() {
        val stats = listOf(
            WordStat("DE", "Schwer", shown = 1, skipped = 1),
            WordStat("RU", "Легко", shown = 1, skipped = 0),
            WordStat("EN", "Hard", shown = 1, skipped = 1)
        )

        assertEquals(listOf("EN", "DE"), WordStatistics.languagesWithSkips(stats))
    }
}
