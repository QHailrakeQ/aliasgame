package com.aliasgame.app.presentation.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun GameScreen(
    viewModel: GameViewModel = hiltViewModel()
) {
    val state by viewModel.gameState.collectAsState()

    val currentState = state ?: return

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Team turn: ${currentState.currentTeam.name}")
        Text(text = "Time: ${currentState.timeRemaining}")
        Text(text = currentState.currentWord?.text ?: "No more words",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(32.dp))

        Row {
            Button(onClick = { viewModel.onWordSwiped(false) }) {
                Text(text = "Skip")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = { viewModel.onWordSwiped(true) }) {
                Text(text = "Correct")
            }
        }
        }

}