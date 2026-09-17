package com.aliasgame.app.domain.engine

import com.aliasgame.app.domain.model.Word
import com.aliasgame.app.domain.model.Team
import com.aliasgame.app.domain.model.GameSettings
import com.aliasgame.app.domain.model.GameState
import com.aliasgame.app.domain.model.RoundResult

/**
 * Core engine managing game logic, scoring, and session state.
 * Implements a fair-play round system where every team gets an equal number of turns.
 */
class GameEngine(
    var allWords: List<Word>,
    val initialTeams: List<Team>,
    var settings: GameSettings
) {
    private val teams = initialTeams.toMutableList()
    private val currentRoundResults = mutableListOf<RoundResult>()

    private var currentWord: Word? = null
    private var currentTeamIndex = 0
    private var usedWords = mutableListOf<Word>()
    private var currentScore = 0
    private var isPaused = false

    /**
     * Retrieves the next available word from the dictionary.
     */
    fun getNextWord(): Word? {
        val availableWords = allWords.filter { !usedWords.contains(it) }
        if (availableWords.isEmpty()) {
            currentWord = null
            return null
        }
        val nextWord = availableWords.random()
        usedWords.add(nextWord)
        currentWord = nextWord
        return nextWord
    }

    /**
     * Records a correct answer and increments score based on settings.
     */
    fun onCorrectAnswer(): Word? {
        val word = currentWord ?: return null
        currentRoundResults.add(RoundResult(word, true))
        currentScore += settings.pointsPerCorrectAnswer
        return getNextWord()
    }

    /**
     * Records a skipped word and applies the configured penalty/points.
     */
    fun onSkipWord(): Word? {
        val word = currentWord ?: return null
        currentRoundResults.add(RoundResult(word, false))
        currentScore += settings.pointsPerSkip
        return getNextWord()
    }

    /**
     * Handles the outcome of the final word in a round.
     */
    fun addFinalWordResult(isCorrect: Boolean) {
        currentWord?.let {
            currentRoundResults.add(RoundResult(it, isCorrect))
            if (isCorrect) currentScore += settings.pointsPerCorrectAnswer
            else currentScore += settings.pointsPerSkip
        }
    }

    /**
     * Toggles a previous word result in the round overview.
     * Updates the associated team's score reactively.
     */
    fun toggleWordResult(index: Int) {
        if (index !in currentRoundResults.indices) return
        val result = currentRoundResults[index]
        val newStatus = !result.isCorrect

        // Calculate score delta based on settings
        val diff = if (newStatus) {
            settings.pointsPerCorrectAnswer - settings.pointsPerSkip
        } else {
            settings.pointsPerSkip - settings.pointsPerCorrectAnswer
        }

        // Target the team that just completed their turn
        val finishedTeamIndex = if (currentTeamIndex == 0) teams.size - 1 else currentTeamIndex - 1
        val team = teams[finishedTeamIndex]
        teams[finishedTeamIndex] = team.copy(score = team.score + diff)

        currentRoundResults[index] = result.copy(isCorrect = newStatus)
    }

    fun prepareForNextRound() {
        currentRoundResults.clear()
    }

    fun pause() {
        isPaused = true
    }

    fun resume() {
        isPaused = false
    }

    /**
     * Transitions turn to the next team and evaluates win conditions.
     * Winner is only determined after a complete round cycle where every team has played.
     */
    fun rollNextTeam(): Team? {
        val team = teams[currentTeamIndex]
        val updatedTeam = team.copy(score = team.score + currentScore)
        teams[currentTeamIndex] = updatedTeam

        currentTeamIndex = (currentTeamIndex + 1) % teams.size
        currentScore = 0

        // Check for winners only when a full cycle is complete (back to the first team)
        if (currentTeamIndex == 0) {
            val potentialWinners = teams.filter { it.score >= settings.targetScore }
            if (potentialWinners.isNotEmpty()) {
                return potentialWinners.maxByOrNull { it.score }
            }
        }

        return null
    }

    fun getTeams(): List<Team> {
        return teams.toList()
    }

    /**
     * Generates an immutable snapshot of the current game state for the UI layer.
     */
    fun getCurrentState(
        timeRemaining: Long, 
        currentWord: Word?,
        isRoundOver: Boolean = false,
        winner: Team? = null
    ): GameState {
        // Evaluate victory only at the end of a round cycle
        val finalWinner = winner ?: if (currentTeamIndex == 0) {
            teams.filter { it.score >= settings.targetScore }.maxByOrNull { it.score }
        } else null

        return GameState(
            currentTeam = teams[currentTeamIndex],
            currentWord = currentWord,
            score = currentScore,
            timeRemaining = timeRemaining,
            isPaused = isPaused,
            isLastWordMode = timeRemaining <= 0 && !isRoundOver,
            isRoundOver = isRoundOver,
            isGameFinished = finalWinner != null,
            winner = finalWinner,
            allTeams = teams.toList(),
            maxTime = settings.roundTime,
            roundResults = currentRoundResults.toList()
        )
    }

    fun addPointToTeam(teamId: String) {
        val index = teams.indexOfFirst { it.id == teamId }
        if (index != -1) {
            teams[index] = teams[index].copy(score = teams[index].score + 1)
        }
    }

    /**
     * Initializes a new game session with provided configuration.
     */
    fun setupGame(teamNames: List<String>,
                  newSettings: GameSettings,
                  newWords: List<Word>) {
        this.settings = newSettings
        this.allWords = newWords
        this.teams.clear()
        teamNames.forEachIndexed { index, name ->
            teams.add(Team(id = (index + 1).toString(), name = name))
        }
        this.usedWords.clear()
        this.currentWord = null
        this.currentTeamIndex = 0
        this.currentScore = 0
        this.isPaused = false
        currentRoundResults.clear()
    }
}
