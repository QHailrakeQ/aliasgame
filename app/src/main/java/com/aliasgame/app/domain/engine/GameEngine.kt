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

    fun onCorrectAnswer(): Word? {
        val word = currentWord ?: return null
        currentRoundResults.add(RoundResult(word, true))
        currentScore += settings.pointsPerCorrectAnswer
        return getNextWord()
    }

    fun onSkipWord(): Word? {
        val word = currentWord ?: return null
        currentRoundResults.add(RoundResult(word, false))
        currentScore += settings.pointsPerSkip
        return getNextWord()
    }

    fun addFinalWordResult(isCorrect: Boolean) {
        currentWord?.let {
            currentRoundResults.add(RoundResult(it, isCorrect))
            if (isCorrect) currentScore += settings.pointsPerCorrectAnswer
            else currentScore += settings.pointsPerSkip
        }
    }

    fun toggleWordResult(index: Int) {
        if (index !in currentRoundResults.indices) return
        val result = currentRoundResults[index]
        val newStatus = !result.isCorrect

        val diff = if (newStatus) {
            settings.pointsPerCorrectAnswer - settings.pointsPerSkip
        } else {
            settings.pointsPerSkip - settings.pointsPerCorrectAnswer
        }

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
     * Shifts turn and evaluates win conditions at the end of a round cycle.
     * Returns a list of winners if the game is finished (can be multiple for a draw).
     */
    fun rollNextTeam(): List<Team> {
        val team = teams[currentTeamIndex]
        val updatedTeam = team.copy(score = team.score + currentScore)
        teams[currentTeamIndex] = updatedTeam

        currentTeamIndex = (currentTeamIndex + 1) % teams.size
        currentScore = 0

        if (currentTeamIndex == 0) {
            val potentialWinners = teams.filter { it.score >= settings.targetScore }
            if (potentialWinners.isNotEmpty()) {
                val maxScore = potentialWinners.maxOf { it.score }
                return potentialWinners.filter { it.score == maxScore }
            }
        }

        return emptyList()
    }

    fun getTeams(): List<Team> {
        return teams.toList()
    }

    fun getCurrentState(
        timeRemaining: Long, 
        currentWord: Word?,
        isRoundOver: Boolean = false,
        winners: List<Team> = emptyList()
    ): GameState {
        val finalWinners = if (winners.isNotEmpty()) {
            winners
        } else if (currentTeamIndex == 0) {
            val potentialWinners = teams.filter { it.score >= settings.targetScore }
            if (potentialWinners.isNotEmpty()) {
                val maxScore = potentialWinners.maxOf { it.score }
                potentialWinners.filter { it.score == maxScore }
            } else emptyList()
        } else emptyList()

        return GameState(
            currentTeam = teams[currentTeamIndex],
            currentWord = currentWord,
            score = currentScore,
            timeRemaining = timeRemaining,
            isPaused = isPaused,
            isLastWordMode = timeRemaining <= 0 && !isRoundOver,
            isRoundOver = isRoundOver,
            isGameFinished = finalWinners.isNotEmpty(),
            winners = finalWinners,
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
