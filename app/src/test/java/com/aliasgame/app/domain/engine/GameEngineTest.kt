package com.aliasgame.app.domain.engine

import com.aliasgame.app.domain.model.GameSettings
import com.aliasgame.app.domain.model.Team
import com.aliasgame.app.domain.model.Word
import com.aliasgame.app.domain.model.RoundResult
import com.aliasgame.app.domain.repository.GameSessionSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for GameEngine covering scoring, fair play, and win conditions.
 */
class GameEngineTest {

    private lateinit var engine: GameEngine
    private val words = List(20) { Word("Word $it", "easy") }
    private val teamNames = listOf("Team A", "Team B")
    private val settings = GameSettings(
        roundTime = 60,
        targetScore = 5,
        pointsPerCorrectAnswer = 1,
        pointsPerSkip = -1
    )

    @Before
    fun setup() {
        engine = GameEngine(words, emptyList(), settings)
        engine.setupGame(teamNames, settings, words)
    }

    @Test
    fun `onCorrectAnswer increases current score and returns next word`() {
        engine.getNextWord() // Set initial word
        val resultWord = engine.onCorrectAnswer()
        
        assertEquals(1, engine.getCurrentState(60, null).score)
        assertTrue(words.contains(resultWord))
    }

    @Test
    fun `onSkipWord decreases current score and returns next word`() {
        engine.getNextWord()
        engine.onSkipWord()
        
        assertEquals(-1, engine.getCurrentState(60, null).score)
    }

    @Test
    fun `rollNextTeam updates team score and shifts turn`() {
        engine.getNextWord()
        engine.onCorrectAnswer() // +1
        
        val winners = engine.rollNextTeam()
        
        assertEquals(1, engine.getTeams()[0].score)
        assertEquals(engine.getTeams()[1], engine.getCurrentState(60, null).currentTeam)
        assertTrue(winners.isEmpty())
    }

    @Test
    fun `Fair Play logic - game does not end until round cycle is complete`() {
        // Team A reaches target score (5)
        engine.getNextWord()
        repeat(5) {
            engine.onCorrectAnswer()
        }
        val winnersA = engine.rollNextTeam()
        
        // Cycle not complete (Team B needs to play)
        assertTrue(winnersA.isEmpty())
        
        // Team B plays
        engine.getNextWord()
        engine.onCorrectAnswer()
        val winnersB = engine.rollNextTeam()
        
        // Cycle complete, Team A is winner
        assertEquals(1, winnersB.size)
        assertEquals("Team A", winnersB[0].name)
    }

    @Test
    fun `Draw logic - multiple winners return if scores are equal after cycle`() {
        // Team A gets 5
        engine.getNextWord()
        repeat(5) {
            engine.onCorrectAnswer()
        }
        engine.rollNextTeam()
        
        // Team B gets 5
        engine.getNextWord()
        repeat(5) {
            engine.onCorrectAnswer()
        }
        val winners = engine.rollNextTeam()
        
        assertEquals(2, winners.size)
    }

    @Test
    fun `toggleWordResult correctly adjusts finished team score`() {
        engine.getNextWord()
        engine.onCorrectAnswer() // +1
        engine.rollNextTeam()
        
        // Change Correct to Skip: current score 1 -> subtract 1 (correct) add -1 (skip) = -1
        engine.toggleWordResult(0)
        
        assertEquals(-1, engine.getTeams()[0].score)
    }

    @Test
    fun `final word scoring - active team selects themselves and gets exactly 1 point`() {
        engine.getNextWord()
        engine.onCorrectAnswer() // +1
        
        // Timer ends, final word processed
        engine.addFinalWordResult(isCorrect = true)
        engine.addPointToTeam(engine.getTeams()[0].id) // Active team selects themselves
        
        engine.rollNextTeam()
        
        // Expected: 1 (turn score) + 1 (final word point) = 2
        assertEquals(2, engine.getTeams()[0].score)
    }

    @Test
    fun `final word scoring - active team selects opponent and opponent gets 1 point`() {
        engine.getNextWord()
        engine.onCorrectAnswer() // +1
        
        // Timer ends, final word processed
        engine.addFinalWordResult(isCorrect = true)
        engine.addPointToTeam(engine.getTeams()[1].id) // Active team (A) selects opponent (B)
        
        engine.rollNextTeam()
        
        // Team A gets only their turn points
        assertEquals(1, engine.getTeams()[0].score)
        // Team B gets 1 point for the final word
        assertEquals(1, engine.getTeams()[1].score)
    }

    @Test
    fun `final word scoring - Nobody selected results in no extra points`() {
        engine.getNextWord()
        engine.onCorrectAnswer() // +1
        
        // Timer ends, final word processed as "Nobody"
        engine.addFinalWordResult(isCorrect = false)
        // No addPointToTeam called
        
        engine.rollNextTeam()
        
        assertEquals(1, engine.getTeams()[0].score)
        assertEquals(0, engine.getTeams()[1].score)
    }

    @Test
    fun `final word is not counted in turn score to avoid double counting`() {
        engine.getNextWord()
        // No words answered yet, currentScore = 0
        
        engine.addFinalWordResult(isCorrect = true) // This shouldn't change currentScore anymore
        assertEquals(0, engine.getCurrentState(0, null).score)
        
        engine.addPointToTeam(engine.getTeams()[0].id)
        engine.rollNextTeam()
        
        // Only the point from addPointToTeam should be there
        assertEquals(1, engine.getTeams()[0].score)
    }

    @Test
    fun `dictionary reshuffle - all distinct words are consumed before reshuffling`() {
        val smallWords = listOf(Word("A", "easy"), Word("B", "easy"))
        engine.setupGame(teamNames, settings, smallWords)
        
        val word1 = engine.getNextWord()
        val word2 = engine.getNextWord()
        
        // Verify both distinct words were picked
        assertNotNull(word1)
        assertNotNull(word2)
        assertNotEquals(word1, word2)
        assertTrue(smallWords.contains(word1))
        assertTrue(smallWords.contains(word2))
        
        // Third call triggers reshuffle
        val word3 = engine.getNextWord()
        assertNotNull(word3)
        assertTrue(smallWords.contains(word3))
    }

    @Test
    fun `dictionary reshuffle - one-word dictionary reshuffles safely`() {
        val oneWord = listOf(Word("Single", "easy"))
        engine.setupGame(teamNames, settings, oneWord)
        
        val word1 = engine.getNextWord()
        assertEquals("Single", word1?.text)
        
        // Second call triggers reshuffle and returns the same word
        val word2 = engine.getNextWord()
        assertEquals("Single", word2?.text)
        
        val word3 = engine.getNextWord()
        assertEquals("Single", word3?.text)
    }

    @Test
    fun `empty dictionary returns null and prevents game start`() {
        engine.setupGame(teamNames, settings, emptyList())
        
        val word = engine.getNextWord()
        assertNull(word)
    }

    @Test
    fun `restoreSession restores all fields correctly`() {
        val snapshot = GameSessionSnapshot(
            teams = listOf(Team("1", "Team 1", 10)),
            currentRoundResults = listOf(RoundResult(Word("W", "p"), true)),
            currentWord = Word("Restored Word", "p"),
            currentTeamIndex = 0,
            usedWords = listOf(Word("Used", "p")),
            currentScore = 5,
            settings = settings,
            language = "EN",
            packId = "p",
            timeRemaining = 45,
            isRoundOver = false
        )
        
        engine.restoreSession(snapshot, words)
        
        val teams = engine.getTeams()
        assertEquals(1, teams.size)
        assertEquals(10, teams[0].score)
        
        val state = engine.getCurrentState(45, snapshot.currentWord)
        assertEquals("Restored Word", state.currentWord?.text)
        assertEquals(5, state.score)
    }
}
