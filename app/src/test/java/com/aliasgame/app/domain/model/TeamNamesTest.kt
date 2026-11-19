package com.aliasgame.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TeamNamesTest {

    private val pool = listOf("Wise Owls", "Sleepy Pandas", "Iron Ducks")

    @Test
    fun `randomUnused never returns a taken name, ignoring case and spaces`() {
        repeat(50) { seed ->
            val name = TeamNames.randomUnused(pool, listOf(" wise owls ", "IRON DUCKS"), Random(seed))
            assertEquals("Sleepy Pandas", name)
        }
    }

    @Test
    fun `randomUnused numbers a pool name when every name is taken`() {
        val name = TeamNames.randomUnused(listOf("Owls"), listOf("Owls", "Owls 2"))

        assertEquals("Owls 3", name)
    }

    @Test
    fun `randomDistinct returns different names even beyond the pool size`() {
        val names = TeamNames.randomDistinct(pool, count = 5, random = Random(1))

        assertEquals(5, names.size)
        assertEquals(5, names.map(TeamNames::normalize).toSet().size)
    }

    @Test
    fun `errors flags blank names and every copy of a duplicate`() {
        assertEquals(
            listOf(TeamNameError.DUPLICATE, null, TeamNameError.BLANK, TeamNameError.DUPLICATE, TeamNameError.BLANK),
            TeamNames.errors(listOf("Owls", "Pandas", "  ", "owls ", ""))
        )
    }

    @Test
    fun `isPoolName matches regardless of case and spaces`() {
        assertTrue(TeamNames.isPoolName(" iron ducks", pool))
        assertFalse(TeamNames.isPoolName("Our own name", pool))
    }
}
