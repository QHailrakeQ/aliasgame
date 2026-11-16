package com.aliasgame.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class TeamNamesCodecTest {

    @Test
    fun `encode and decode round-trip safely`() {
        val names = listOf("Team 1", "Team 2", "Team, With, Commas")
        val encoded = TeamNamesCodec.encode(names)
        val decoded = TeamNamesCodec.decode(encoded)
        
        assertEquals(names, decoded)
    }

    @Test
    fun `decode legacy CSV format correctly`() {
        val legacyValue = "Team 1,Team 2"
        val expected = listOf("Team 1", "Team 2")
        val decoded = TeamNamesCodec.decode(legacyValue)
        
        assertEquals(expected, decoded)
    }

    @Test
    fun `encode and decode with Unicode characters`() {
        val names = listOf("Команда 1", "Команда, з комами", "🚀 Team")
        val encoded = TeamNamesCodec.encode(names)
        val decoded = TeamNamesCodec.decode(encoded)
        
        assertEquals(names, decoded)
    }

    @Test
    fun `decode legacy CSV with empty values`() {
        val legacyValue = "Team 1,,Team 2"
        val expected = listOf("Team 1", "", "Team 2")
        val decoded = TeamNamesCodec.decode(legacyValue)
        
        assertEquals(expected, decoded)
    }
}
