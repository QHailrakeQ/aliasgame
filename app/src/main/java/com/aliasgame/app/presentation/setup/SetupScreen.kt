package com.aliasgame.app.presentation.setup

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun SetupScreen(
    viewModel: SetupViewModel = hiltViewModel(),
    onStartGame: () -> Unit
) {
    var roundTime by remember { mutableStateOf(60f) }
    var targetScore by remember { mutableStateOf(50f) }

    Column {
        Text("Round Time (seconds): ${roundTime.toInt()}")
        Slider(value = roundTime, onValueChange = { roundTime = it }, valueRange = 10f..120f)

        Text("Target Score: ${targetScore.toInt()}")
        Slider(value = targetScore, onValueChange = { targetScore = it }, valueRange = 10f..100f)

        Button(onClick = {
            viewModel.startGame(roundTime.toLong(), targetScore.toInt())
            onStartGame()
        }) {
            Text("Start Game")
        }
    }
}