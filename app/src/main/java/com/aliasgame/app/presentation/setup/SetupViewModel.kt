package com.aliasgame.app.presentation.setup

import androidx.lifecycle.ViewModel
import com.aliasgame.app.domain.engine.GameEngine
import com.aliasgame.app.domain.model.GameSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val engine: GameEngine
) : ViewModel() {

    fun startGame(teamNames: List<String>, roundTime: Long, targetScore: Int) {
        engine.setupGame(
            teamNames = teamNames,
            newSettings = engine.settings.copy(
                roundTime = roundTime,
                targetScore = targetScore
            )
        )
    }
}