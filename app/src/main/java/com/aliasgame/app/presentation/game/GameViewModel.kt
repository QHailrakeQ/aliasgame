package com.aliasgame.app.presentation.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliasgame.app.domain.engine.GameEngine
import com.aliasgame.app.domain.model.GameState
import com.aliasgame.app.domain.model.Team
import com.aliasgame.app.domain.model.Word
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    private val engine: GameEngine
) : ViewModel() {
    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState = _gameState.asStateFlow()
    private var currentWord: Word? = null
    private var timeLeft = engine.settings.roundTime
    private var timerJob: Job? = null
    private var roundWinner: Team? = null
    private var isRoundOver = false

    init {
        currentWord = engine.getNextWord()
        updateState()
    }

    private fun updateState() {
        _gameState.value = engine.getCurrentState(
            timeLeft,
            currentWord = currentWord,
            isRoundOver = isRoundOver,
            winner = roundWinner
        )
    }

    fun startNextRound() {
        isRoundOver = false
        roundWinner = null
        timeLeft = engine.settings.roundTime
        currentWord = engine.getNextWord()
        updateState()
    }

    fun onStartTimer() {
        startTimer()
    }

    fun startTimer() {
        if (timerJob?.isActive == true) return
        timerJob = viewModelScope.launch {
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
                updateState()
            }
            onTimerFinished()
        }
    }

    private fun onTimerFinished() {
        updateState()
    }

    fun onWordSwiped(isCorrect: Boolean) {
        if (timeLeft <= 0) return

        currentWord = if(isCorrect) {
            engine.onCorrectAnswer()
        } else {
            engine.onSkipWord()
        }
        updateState()
    }

    fun onFinalWordProcessed(winnerTeamId: String?) {
        winnerTeamId?.let { engine.addPointToTeam(it) }
        finishRound()
    }

    private fun finishRound() {
        roundWinner = engine.rollNextTeam()
        isRoundOver = true
        updateState()
    }

    fun pauseGame() {
        timerJob?.cancel()
        engine.pause()
        updateState()
    }

    fun resumeGame() {
        engine.resume()
        startTimer()
    }
}
