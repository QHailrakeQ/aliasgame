package com.aliasgame.app.domain.model

data class GameState(
    val currentTeam: Team,
    val currentWord: Word?,
    val score: Int = 0,
    val timeRemaining: Long,
    val isPaused: Boolean = false,
    val isLastWordMode: Boolean = false,
    val isGameFinished: Boolean = false
)
