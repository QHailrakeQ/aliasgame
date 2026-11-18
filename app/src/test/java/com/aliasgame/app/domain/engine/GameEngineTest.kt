package com.aliasgame.app.domain.engine

import com.aliasgame.app.domain.model.GamePhase
import com.aliasgame.app.domain.model.GameSettings
import com.aliasgame.app.domain.model.RoundResult
import com.aliasgame.app.domain.model.Word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for GameEngine: scoring, fair play, win conditions and phase transitions.
 */
class GameEngineTest {

    private val words = List(20) { Word("Word $it", "easy") }
    private val settings = GameSettings(
        roundTime = 60,
        targetScore = 5,
        pointsPerCorrectAnswer = 1,
        pointsPerSkip = -1
    )

    private fun newEngine(
        dictionary: List<Word> = words,
        teamNames: List<String> = listOf("Team A", "Team B")
    ) = GameEngine.restore(GameEngine.newSession(teamNames, settings, "EN", "easy"), dictionary)

    /** Starts the turn, answers, lets the time run out and resolves the last word. */
    private fun GameEngine.playTurn(correct: Int = 0, skipped: Int = 0, lastWordTeamId: String? = null) {
        assertTrue(startTurn())
        repeat(correct) { answer(isCorrect = true) }
        repeat(skipped) { answer(isCorrect = false) }
        runOutOfTime()
        assertTrue(decideLastWord(lastWordTeamId))
    }

    private fun GameEngine.runOutOfTime() {
        repeat(settings.roundTime.toInt()) { tick() }
        assertEquals(GamePhase.LAST_WORD, state.phase)
    }

    private fun GameEngine.score(teamId: String) = state.teams.first { it.id == teamId }.score

    // --- Scoring -------------------------------------------------------------------------

    @Test
    fun `a new game is ready to start for the first team with a word`() {
        val state = newEngine().state

        assertEquals(GamePhase.READY_TO_START, state.phase)
        assertEquals("Team A", state.currentTeam.name)
        assertTrue(state.currentWord in words)
        assertTrue(state.teams.all { it.score == 0 })
    }

    @Test
    fun `a correct answer adds to the turn score and draws the next word`() {
        val engine = newEngine()
        engine.startTurn()
        val firstWord = engine.state.currentWord

        assertTrue(engine.answer(isCorrect = true))

        assertEquals(1, engine.state.turnScore)
        assertNotEquals(firstWord, engine.state.currentWord)
    }

    @Test
    fun `a skipped word subtracts from the turn score`() {
        val engine = newEngine()
        engine.startTurn()

        engine.answer(isCorrect = false)

        assertEquals(-1, engine.state.turnScore)
    }

    @Test
    fun `ending a turn adds the turn score to the team and passes the turn on`() {
        val engine = newEngine()

        engine.playTurn(correct = 1)

        assertEquals(1, engine.score("1"))
        assertEquals("Team B", engine.state.currentTeam.name)
        assertEquals(0, engine.state.turnScore)
        assertEquals(GamePhase.ROUND_REVIEW, engine.state.phase)
    }

    // --- Fair play and winning -----------------------------------------------------------

    @Test
    fun `fair play - the game does not end until every team has played the cycle`() {
        val engine = newEngine()

        engine.playTurn(correct = 5)
        assertEquals(GamePhase.ROUND_REVIEW, engine.state.phase)
        engine.nextRound()

        engine.playTurn(correct = 1)
        assertEquals(GamePhase.FINISHED, engine.state.phase)
        assertEquals(listOf("Team A"), engine.state.winners.map { it.name })
    }

    @Test
    fun `draw - all teams with the best score at or above target win together`() {
        val engine = newEngine()

        engine.playTurn(correct = 5)
        engine.nextRound()
        engine.playTurn(correct = 5)

        assertEquals(GamePhase.FINISHED, engine.state.phase)
        assertEquals(2, engine.state.winners.size)
    }

    @Test
    fun `winners are only reported once the game is finished`() {
        val engine = newEngine()
        engine.playTurn(correct = 5)

        assertTrue(engine.state.winners.isEmpty())
    }

    // --- Review corrections --------------------------------------------------------------

    @Test
    fun `toggling a reviewed result corrects the score of the team that played`() {
        val engine = newEngine()
        engine.playTurn(correct = 1)

        assertTrue(engine.toggleResult(0))

        // Correct (+1) became skipped (-1).
        assertEquals(-1, engine.score("1"))
        assertFalse(engine.state.roundResults[0].isCorrect)
    }

    @Test
    fun `the last-word result cannot be toggled`() {
        val engine = newEngine()
        engine.playTurn(correct = 1, lastWordTeamId = "2")

        assertFalse(engine.toggleResult(engine.state.roundResults.lastIndex))
        assertEquals(1, engine.score("2"))
    }

    @Test
    fun `a correction that completes the win condition at the end of a cycle finishes the game`() {
        val engine = newEngine()
        engine.playTurn(correct = 4)
        engine.nextRound()
        engine.playTurn(correct = 4, skipped = 1)
        assertEquals(GamePhase.ROUND_REVIEW, engine.state.phase)

        // Team B's skipped word was actually guessed: 3 -> 5 points, reaching the target.
        engine.toggleResult(4)

        assertEquals(GamePhase.FINISHED, engine.state.phase)
        assertEquals(listOf("Team B"), engine.state.winners.map { it.name })
    }

    // --- Last word -----------------------------------------------------------------------

    @Test
    fun `last word - the active team selecting itself earns exactly one point`() {
        val engine = newEngine()

        engine.playTurn(correct = 1, lastWordTeamId = "1")

        assertEquals(2, engine.score("1"))
    }

    @Test
    fun `last word - selecting the opponent gives the point to the opponent`() {
        val engine = newEngine()

        engine.playTurn(correct = 1, lastWordTeamId = "2")

        assertEquals(1, engine.score("1"))
        assertEquals(1, engine.score("2"))
    }

    @Test
    fun `last word - nobody earns a point when no team is selected`() {
        val engine = newEngine()

        engine.playTurn(correct = 1, lastWordTeamId = null)

        assertEquals(1, engine.score("1"))
        assertEquals(0, engine.score("2"))
        assertFalse(engine.state.roundResults.last().isCorrect)
    }

    @Test
    fun `last word is recorded in the results but not counted in the turn score`() {
        val engine = newEngine()
        engine.startTurn()
        engine.runOutOfTime()
        assertEquals(0, engine.state.turnScore)

        engine.decideLastWord("1")

        assertEquals(1, engine.score("1"))
        assertTrue(engine.state.roundResults.single().isCorrect)
    }

    // --- Dictionary ----------------------------------------------------------------------

    @Test
    fun `dictionary reshuffle - all distinct words are used before any repeats`() {
        val smallWords = listOf(Word("A", "easy"), Word("B", "easy"))
        val engine = newEngine(dictionary = smallWords)
        engine.startTurn()

        val first = engine.state.currentWord
        engine.answer(isCorrect = true)
        val second = engine.state.currentWord
        engine.answer(isCorrect = true)
        val third = engine.state.currentWord

        assertNotEquals(first, second)
        assertEquals(smallWords.toSet(), setOf(first, second))
        assertTrue(third in smallWords)
    }

    @Test
    fun `dictionary reshuffle - a one-word dictionary keeps serving that word`() {
        val engine = newEngine(dictionary = listOf(Word("Single", "easy")))
        engine.startTurn()

        repeat(3) {
            assertEquals("Single", engine.state.currentWord?.text)
            engine.answer(isCorrect = true)
        }
    }

    @Test
    fun `an empty dictionary has no word and the turn cannot start`() {
        val engine = newEngine(dictionary = emptyList())

        assertNull(engine.state.currentWord)
        assertFalse(engine.startTurn())
    }

    // --- Phase machine -------------------------------------------------------------------

    @Test
    fun `the countdown enters last-word mode exactly when the time runs out`() {
        val engine = newEngine()
        engine.startTurn()

        repeat(settings.roundTime.toInt() - 1) { assertFalse(engine.tick()) }
        assertEquals(1L, engine.state.timeRemaining)
        assertTrue(engine.tick())

        assertEquals(GamePhase.LAST_WORD, engine.state.phase)
        assertFalse(engine.tick())
    }

    @Test
    fun `pausing stops the countdown and resuming continues from the same time`() {
        val engine = newEngine()
        engine.startTurn()
        repeat(5) { engine.tick() }

        assertTrue(engine.pause())
        assertFalse(engine.tick())
        assertEquals(55L, engine.state.timeRemaining)

        assertTrue(engine.resume())
        assertEquals(GamePhase.RUNNING, engine.state.phase)
        assertEquals(55L, engine.state.timeRemaining)
    }

    @Test
    fun `commands outside their phase are rejected and change nothing`() {
        val engine = newEngine()
        val ready = engine.state

        assertFalse(engine.pause())
        assertFalse(engine.resume())
        assertFalse(engine.answer(isCorrect = true))
        assertFalse(engine.decideLastWord("1"))
        assertFalse(engine.toggleResult(0))
        assertFalse(engine.nextRound())
        assertEquals(ready, engine.state)

        engine.startTurn()
        assertFalse(engine.startTurn())
        assertFalse(engine.decideLastWord("1"))
        assertFalse(engine.nextRound())

        engine.pause()
        assertFalse(engine.answer(isCorrect = true))
        assertTrue(engine.state.roundResults.isEmpty())
    }

    @Test
    fun `the last word can be decided only once`() {
        val engine = newEngine()
        engine.startTurn()
        engine.runOutOfTime()

        assertTrue(engine.decideLastWord("1"))
        assertFalse(engine.decideLastWord("1"))

        assertEquals("Team B", engine.state.currentTeam.name)
        assertEquals(1, engine.state.roundResults.size)
    }

    @Test
    fun `next round starts the next team on the ready screen with a fresh word`() {
        val engine = newEngine()
        engine.playTurn(correct = 2)

        assertTrue(engine.nextRound())

        assertEquals(GamePhase.READY_TO_START, engine.state.phase)
        assertTrue(engine.state.roundResults.isEmpty())
        assertNotNull(engine.state.currentWord)
        assertFalse(engine.nextRound())
    }

    // --- Persistence ---------------------------------------------------------------------

    @Test
    fun `restore brings back every persisted field`() {
        val engine = newEngine()
        engine.playTurn(correct = 3, skipped = 1)
        engine.nextRound()
        engine.startTurn()
        engine.answer(isCorrect = true)
        repeat(15) { engine.tick() }
        engine.pause()

        val restored = GameEngine.restore(engine.snapshot(), words)

        assertEquals(engine.state, restored.state)
        assertEquals(engine.snapshot(), restored.snapshot())
    }

    @Test
    fun `a turn that was running when saved is restored paused`() {
        val engine = newEngine()
        engine.startTurn()
        repeat(10) { engine.tick() }

        val restored = GameEngine.restore(engine.snapshot(), words)

        assertEquals(GamePhase.PAUSED, restored.state.phase)
        assertEquals(50L, restored.state.timeRemaining)
    }

    @Test
    fun `a paused snapshot without time left is restored in last-word mode`() {
        val snapshot = newEngine().snapshot().copy(phase = GamePhase.PAUSED, timeRemaining = 0)

        assertEquals(GamePhase.LAST_WORD, GameEngine.restore(snapshot, words).state.phase)
    }

    @Test
    fun `restore keeps the round results under review`() {
        val results = listOf(RoundResult(Word("W", "easy"), true))
        val snapshot = newEngine().snapshot().copy(
            phase = GamePhase.ROUND_REVIEW,
            currentTeamIndex = 1,
            currentRoundResults = results
        )

        val state = GameEngine.restore(snapshot, words).state

        assertEquals(GamePhase.ROUND_REVIEW, state.phase)
        assertEquals(results, state.roundResults)
        assertEquals("Team B", state.currentTeam.name)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `restore rejects a snapshot whose current team does not exist`() {
        GameEngine.restore(newEngine().snapshot().copy(currentTeamIndex = 5), words)
    }
}
