package com.aliasgame.app.domain.engine

import com.aliasgame.app.domain.model.Word
import com.aliasgame.app.domain.model.Team
import com.aliasgame.app.domain.model.GameSettings
import com.aliasgame.app.domain.model.GameState


class GameEngine(val allWords: List<Word>, val initialTeams: List<Team>, var settings: GameSettings) {
    private val teams = initialTeams.toMutableList()

    private var currentTeamIndex = 0
    private var usedWords = mutableListOf<Word>()
    private var currentScore = 0
    private var isPaused = false

    fun getNextWord(): Word? {
        val availableWords = allWords.filter { !usedWords.contains(it) }
        if (availableWords.isEmpty()) {
            return null
        }
        val nextWord = availableWords.random()
        usedWords.add(nextWord)
        return nextWord
    }

    fun onCorrectAnswer(): Word? {
        currentScore += settings.pointsPerCorrectAnswer
        return getNextWord()
    }

    fun onSkipWord(): Word? {
        currentScore += settings.pointsPerSkip
        return getNextWord()
    }

    fun pause() {
        isPaused = true
    }

    fun resume() {
        isPaused = false
    }

    fun rollNextTeam(): Team? {
        val team = teams[currentTeamIndex]
        val updatedTeam = team.copy(score = team.score + currentScore)
        teams[currentTeamIndex] = updatedTeam

        val winner = if (updatedTeam.score >= settings.targetScore) updatedTeam else null

        currentTeamIndex = (currentTeamIndex + 1) % teams.size
        currentScore = 0

        return winner
    }

    fun getTeams(): List<Team> {
        return teams.toList()
    }

    fun getCurrentState(
        timeRemaining: Long, currentWord: Word?,
        isRoundOver: Boolean = false,
        winner: Team? = null
    ): GameState {
        return GameState(
            currentTeam = teams[currentTeamIndex],
            currentWord = currentWord,
            score = currentScore,
            timeRemaining = timeRemaining,
            isPaused = isPaused,
            isLastWordMode = timeRemaining <= 0 && !isRoundOver,
            isRoundOver = isRoundOver,
            isGameFinished = winner != null,
            winner = winner,
            allTeams = teams.toList(),
            maxTime = settings.roundTime
        )
    }

    fun addPointToTeam(teamId: String) {
        val index = teams.indexOfFirst { it.id == teamId }
        if (index != -1) {
            teams[index] = teams[index].copy(score = teams[index].score + 1)
        }
    }

    fun setupGame(teamNames: List<String>, newSettings: GameSettings) {
        this.settings = newSettings
        this.teams.clear()
        teamNames.forEachIndexed { index, name ->
            teams.add(Team(id = (index + 1).toString(), name = name))
        }
        this.usedWords.clear()
        this.currentTeamIndex = 0
        this.currentScore = 0
        this.isPaused = false
    }
}
