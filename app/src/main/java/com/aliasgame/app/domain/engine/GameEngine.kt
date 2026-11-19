package com.aliasgame.app.domain.engine

import com.aliasgame.app.domain.model.GamePhase
import com.aliasgame.app.domain.model.GameSettings
import com.aliasgame.app.domain.model.GameState
import com.aliasgame.app.domain.model.RoundResult
import com.aliasgame.app.domain.model.Team
import com.aliasgame.app.domain.model.Word
import com.aliasgame.app.domain.repository.GameSessionSnapshot

/**
 * Framework-free game rules: scoring, turn rotation, the countdown and the phase machine
 * described in [GamePhase].
 *
 * Every command is validated against the current phase and returns `false` (changing
 * nothing) when it is not allowed, so callers cannot drive the game into an invalid state.
 * The engine is not thread-safe; it is owned and called by a single ViewModel on the main thread.
 *
 * Create a new game with [newSession] and load it with [restore].
 */
class GameEngine private constructor(
    private val allWords: List<Word>,
    teams: List<Team>,
    private val settings: GameSettings,
    val language: String,
    private val packId: String
) {
    private val teams = teams.toMutableList()
    private val roundResults = mutableListOf<RoundResult>()
    private val usedWords = mutableListOf<Word>()

    private var phase = GamePhase.READY_TO_START
    private var currentWord: Word? = null
    private var currentTeamIndex = 0
    private var turnScore = 0
    private var timeRemaining = 0L

    val state: GameState
        get() = GameState(
            phase = phase,
            currentTeam = teams[currentTeamIndex],
            currentWord = currentWord,
            turnScore = turnScore,
            timeRemaining = timeRemaining,
            roundTime = settings.roundTime,
            teams = teams.toList(),
            roundResults = roundResults.toList(),
            winners = if (phase == GamePhase.FINISHED) findWinners() else emptyList()
        )

    /** READY_TO_START → RUNNING. Requires a word to explain. */
    fun startTurn(): Boolean {
        if (phase != GamePhase.READY_TO_START || currentWord == null) return false
        timeRemaining = settings.roundTime
        phase = GamePhase.RUNNING
        return true
    }

    /**
     * Advances the countdown by one second while RUNNING.
     * @return `true` when this tick ran the time out and the game entered LAST_WORD.
     */
    fun tick(): Boolean {
        if (phase != GamePhase.RUNNING) return false
        timeRemaining = (timeRemaining - 1).coerceAtLeast(0)
        if (timeRemaining == 0L) {
            phase = GamePhase.LAST_WORD
            return true
        }
        return false
    }

    /** RUNNING → PAUSED. */
    fun pause(): Boolean {
        if (phase != GamePhase.RUNNING) return false
        phase = GamePhase.PAUSED
        return true
    }

    /** PAUSED → RUNNING. */
    fun resume(): Boolean {
        if (phase != GamePhase.PAUSED) return false
        phase = if (timeRemaining > 0) GamePhase.RUNNING else GamePhase.LAST_WORD
        return true
    }

    /** Records the current word as guessed or skipped and draws the next one (RUNNING only). */
    fun answer(isCorrect: Boolean): Boolean {
        if (phase != GamePhase.RUNNING) return false
        val word = currentWord ?: return false
        roundResults += RoundResult(word, isCorrect)
        turnScore += if (isCorrect) settings.pointsPerCorrectAnswer else settings.pointsPerSkip
        currentWord = drawNextWord()
        return true
    }

    /**
     * Resolves the last word (LAST_WORD only): [winnerTeamId] gets one point, or nobody when
     * `null`. Ends the turn, adds the turn score to the team and passes the turn on. After the
     * last team of a cycle the game finishes if any team reached the target score.
     */
    fun decideLastWord(winnerTeamId: String?): Boolean {
        if (phase != GamePhase.LAST_WORD) return false
        currentWord?.let { roundResults += RoundResult(it, isCorrect = winnerTeamId != null) }
        winnerTeamId?.let { id -> updateTeam(teams.indexOfFirst { it.id == id }) { it + 1 } }

        updateTeam(currentTeamIndex) { it + turnScore }
        turnScore = 0
        timeRemaining = 0
        currentTeamIndex = (currentTeamIndex + 1) % teams.size

        phase = if (isCycleComplete() && findWinners().isNotEmpty()) {
            GamePhase.FINISHED
        } else {
            GamePhase.ROUND_REVIEW
        }
        return true
    }

    /**
     * Flips a reviewed result between guessed and skipped (ROUND_REVIEW only) and corrects the
     * score of the team that played the turn. The last-word result cannot be toggled because
     * its point may belong to another team. A correction that completes the win condition at
     * the end of a cycle finishes the game.
     */
    fun toggleResult(index: Int): Boolean {
        if (phase != GamePhase.ROUND_REVIEW || index !in 0 until roundResults.lastIndex) return false
        val result = roundResults[index]
        val pointDifference = settings.pointsPerCorrectAnswer - settings.pointsPerSkip
        val previousTeamIndex = (currentTeamIndex - 1 + teams.size) % teams.size
        updateTeam(previousTeamIndex) { if (result.isCorrect) it - pointDifference else it + pointDifference }
        roundResults[index] = result.copy(isCorrect = !result.isCorrect)

        if (isCycleComplete() && findWinners().isNotEmpty()) phase = GamePhase.FINISHED
        return true
    }

    /** ROUND_REVIEW → READY_TO_START for the next team, with a fresh word. */
    fun nextRound(): Boolean {
        if (phase != GamePhase.ROUND_REVIEW) return false
        roundResults.clear()
        currentWord = drawNextWord()
        phase = GamePhase.READY_TO_START
        return true
    }

    fun snapshot(): GameSessionSnapshot = GameSessionSnapshot(
        teams = teams.toList(),
        currentRoundResults = roundResults.toList(),
        currentWord = currentWord,
        currentTeamIndex = currentTeamIndex,
        usedWords = usedWords.toList(),
        currentScore = turnScore,
        settings = settings,
        language = language,
        packId = packId,
        timeRemaining = timeRemaining,
        phase = phase
    )

    /**
     * Draws a random unused word. When every word has been used the dictionary is reshuffled
     * so a long game never runs out of words.
     */
    private fun drawNextWord(): Word? {
        if (allWords.isEmpty()) return null
        val available = allWords.filter { it !in usedWords }.ifEmpty {
            usedWords.clear()
            allWords
        }
        return available.random().also { usedWords += it }
    }

    private fun updateTeam(index: Int, newScore: (Int) -> Int) {
        if (index !in teams.indices) return
        teams[index] = teams[index].let { it.copy(score = newScore(it.score)) }
    }

    /** Fair play: the game can only end once every team has had the same number of turns. */
    private fun isCycleComplete() = currentTeamIndex == 0

    /** Teams at or above the target with the highest score; several teams mean a draw. */
    private fun findWinners(): List<Team> {
        val qualified = teams.filter { it.score >= settings.targetScore }
        val best = qualified.maxOfOrNull { it.score } ?: return emptyList()
        return qualified.filter { it.score == best }
    }

    companion object {
        /** A fresh game: every team at zero, the first team about to start. */
        fun newSession(
            teamNames: List<String>,
            settings: GameSettings,
            language: String,
            packId: String
        ): GameSessionSnapshot {
            require(teamNames.isNotEmpty()) { "A game needs at least one team" }
            return GameSessionSnapshot(
                teams = teamNames.mapIndexed { index, name -> Team(id = (index + 1).toString(), name = name) },
                currentRoundResults = emptyList(),
                currentWord = null,
                currentTeamIndex = 0,
                usedWords = emptyList(),
                currentScore = 0,
                settings = settings,
                language = language,
                packId = packId,
                timeRemaining = 0,
                phase = GamePhase.READY_TO_START
            )
        }

        /**
         * Loads a game from [snapshot] with the pack's [words].
         *
         * A turn that was RUNNING when the snapshot was taken (e.g. before process death) is
         * restored PAUSED so that no time is lost while the app was gone.
         *
         * @throws IllegalArgumentException if the snapshot is internally inconsistent.
         */
        fun restore(snapshot: GameSessionSnapshot, words: List<Word>): GameEngine {
            require(snapshot.teams.isNotEmpty()) { "Snapshot has no teams" }
            require(snapshot.currentTeamIndex in snapshot.teams.indices) {
                "Current team index ${snapshot.currentTeamIndex} is out of range"
            }
            return GameEngine(words, snapshot.teams, snapshot.settings, snapshot.language, snapshot.packId).apply {
                roundResults += snapshot.currentRoundResults
                usedWords += snapshot.usedWords
                currentWord = snapshot.currentWord
                currentTeamIndex = snapshot.currentTeamIndex
                turnScore = snapshot.currentScore
                timeRemaining = snapshot.timeRemaining.coerceAtLeast(0)
                phase = when (snapshot.phase) {
                    GamePhase.RUNNING -> GamePhase.PAUSED
                    else -> snapshot.phase
                }
                if (phase == GamePhase.PAUSED && timeRemaining == 0L) phase = GamePhase.LAST_WORD
                if (phase == GamePhase.READY_TO_START && currentWord == null) currentWord = drawNextWord()
            }
        }
    }
}
