package com.aliasgame.app.domain.model

data class GameSettings(
    val roundTime: Long,
    val targetScore: Int,
    val pointsPerCorrectAnswer: Int = 1,
    val pointsPerSkip: Int = -1
)
