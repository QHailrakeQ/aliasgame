package com.aliasgame.app.domain.engine

import com.aliasgame.app.domain.model.GameSettings
import com.aliasgame.app.domain.model.Team
import com.aliasgame.app.domain.model.Word
import org.junit.Assert.assertEquals
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
}
