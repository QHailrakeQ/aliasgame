package com.aliasgame.app.data.repository

import com.aliasgame.app.domain.model.WordStat
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Test

class WordStatsCodecTest {

    @Test
    fun `statistics survive an encode and decode round trip`() {
        val stats = listOf(
            WordStat("EN", "Cat", shown = 3, skipped = 1),
            WordStat("UA", "Ентропія", shown = 2, skipped = 2)
        )

        assertEquals(stats, WordStatsCodec.decode(WordStatsCodec.encode(stats)))
    }

    @Test(expected = SerializationException::class)
    fun `decode fails loudly on a malformed document`() {
        WordStatsCodec.decode("""[{"text": "Cat"}]""")
    }
}
