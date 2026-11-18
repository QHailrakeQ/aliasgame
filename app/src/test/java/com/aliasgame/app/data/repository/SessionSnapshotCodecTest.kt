package com.aliasgame.app.data.repository

import com.aliasgame.app.domain.model.GamePhase
import com.aliasgame.app.domain.model.GameSettings
import com.aliasgame.app.domain.model.RoundResult
import com.aliasgame.app.domain.model.Team
import com.aliasgame.app.domain.model.Word
import com.aliasgame.app.domain.repository.GameSessionSnapshot
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionSnapshotCodecTest {

    private val snapshot = GameSessionSnapshot(
        teams = listOf(Team("1", "Team \"A\", 1", 7), Team("2", "Команда Б", 3)),
        currentRoundResults = listOf(RoundResult(Word("Cat", "Easy"), true)),
        currentWord = Word("Dog", "Easy"),
        currentTeamIndex = 1,
        usedWords = listOf(Word("Cat", "Easy"), Word("Dog", "Easy")),
        currentScore = 2,
        settings = GameSettings(roundTime = 60, targetScore = 50),
        language = "UA",
        packId = "Easy",
        timeRemaining = 42,
        phase = GamePhase.PAUSED
    )

    @Test
    fun `encode and decode round-trip every field`() {
        assertEquals(snapshot, SessionSnapshotCodec.decode(SessionSnapshotCodec.encode(snapshot)))
    }

    @Test(expected = SerializationException::class)
    fun `decoding garbage fails loudly`() {
        SessionSnapshotCodec.decode("""{"teams": 1}""")
    }

    /** Snapshots written by app versions before the explicit phase model. */
    private fun legacy(timeRemaining: Long, isRoundOver: Boolean, isTurnInProgress: Boolean? = null) = """
        {"teams":[{"id":"1","name":"A","score":0},{"id":"2","name":"B","score":0}],
         "currentRoundResults":[],"currentWord":{"text":"Cat","packId":"Easy"},"currentTeamIndex":0,
         "usedWords":[],"currentScore":0,
         "settings":{"roundTime":60,"targetScore":50,"pointsPerCorrectAnswer":1,"pointsPerSkip":-1},
         "language":"EN","packId":"Easy","timeRemaining":$timeRemaining,"isRoundOver":$isRoundOver
         ${isTurnInProgress?.let { ""","isTurnInProgress":$it""" } ?: ""}}
    """.trimIndent()

    @Test
    fun `legacy snapshot on the round results screen migrates to ROUND_REVIEW`() {
        assertEquals(GamePhase.ROUND_REVIEW, SessionSnapshotCodec.decode(legacy(0, isRoundOver = true)).phase)
    }

    @Test
    fun `legacy snapshot with time left migrates to PAUSED`() {
        assertEquals(GamePhase.PAUSED, SessionSnapshotCodec.decode(legacy(30, isRoundOver = false)).phase)
    }

    @Test
    fun `legacy snapshot of a started turn without time left migrates to LAST_WORD`() {
        val decoded = SessionSnapshotCodec.decode(legacy(0, isRoundOver = false, isTurnInProgress = true))
        assertEquals(GamePhase.LAST_WORD, decoded.phase)
    }

    @Test
    fun `legacy snapshot of a turn not yet started migrates to READY_TO_START`() {
        assertEquals(GamePhase.READY_TO_START, SessionSnapshotCodec.decode(legacy(0, isRoundOver = false)).phase)
    }
}
