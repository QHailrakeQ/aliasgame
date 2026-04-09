package com.aliasgame.app.presentation.game

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aliasgame.app.domain.model.Team
import com.aliasgame.app.domain.model.Word
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun WordCard(
    word: Word,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit
) {
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(16.dp)
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
            .pointerInput(word) {
                detectDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (offsetY.value < -400f) onSwipeUp()
                            else if (offsetY.value > 400f) onSwipeDown()
                            offsetY.animateTo(0f)
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offsetY.snapTo(offsetY.value + dragAmount.y)
                        }
                    }
                )
            }
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = word.text, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
fun FinalWordDialog(
    word: String,
    teams: List<Team>,
    onTeamSelected: (String?) -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text("Last word: $word") },
        text = {
            Column {
                Text("Who got the point?")
                Spacer(modifier = Modifier.height(16.dp))
                teams.forEach { team ->
                    Button(
                        onClick = { onTeamSelected(team.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(team.name)
                    }
                }
                TextButton(
                    onClick = { onTeamSelected(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("No one is correct")
                }
            }
        },
        confirmButton = { }
    )
}

@Composable
fun GameScreen(
    viewModel: GameViewModel = hiltViewModel(),
    onExit: () -> Unit
) {
    val state by viewModel.gameState.collectAsState()
    val currentState = state ?: return

    Box(modifier = Modifier.fillMaxSize()) {
        IconButton(
            onClick = onExit,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Close,
                contentDescription = "Exit"
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Team: ${currentState.currentTeam.name}")
            Text(text = "Time: ${currentState.timeRemaining}")
            Text(
                text = currentState.currentWord?.text ?: "No more words",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(24.dp))

            currentState.currentWord?.let { word ->
                WordCard(
                    word = word,
                    onSwipeUp = { viewModel.onWordSwiped(true) },
                    onSwipeDown = { viewModel.onWordSwiped(false) }
                )
            } ?: Text("No more words")

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

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { viewModel.pauseGame() }) {
                Text("Pause")
            }
        }

        if (currentState.isPaused) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Button(onClick = { viewModel.resumeGame() }) {
                        Text("Continue")
                    }
                }
            }
        }

        if (currentState.isLastWordMode) {
            FinalWordDialog(
                word = currentState.currentWord?.text ?: "",
                teams = currentState.allTeams,
                onTeamSelected = { teamId -> viewModel.onFinalWordProcessed(teamId) }
            )
        }

        if (currentState.isRoundOver) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Round is over!", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    currentState.allTeams.forEach { team ->
                        Text("${team.name}: ${team.score} points")
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { viewModel.startNextRound() }) {
                        Text("Next round")
                    }
                }
            }
        }

        if (!currentState.isRoundOver &&
            !currentState.isPaused &&
            !currentState.isGameFinished &&
            currentState.timeRemaining == currentState.maxTime
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Next Team:")
                    Text(
                        currentState.currentTeam.name,
                        style = MaterialTheme.typography.displayMedium
                    )
                    Spacer(Modifier.height(32.dp))
                    Button(onClick = { viewModel.onStartTimer() }) {
                        Text("I'm ready!")
                    }
                }
            }
        }

        if (currentState.isGameFinished && currentState.winner != null) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Yellow) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Game is finished!", style = MaterialTheme.typography.displayLarge)
                    Text(currentState.winner.name, style = MaterialTheme.typography.headlineLarge)
                    Text("Score: ${currentState.winner.score}")
                    Button(onClick = onExit) {
                        Text("Main menu")
                    }
                }
            }
        }
    }
}
