package com.aliasgame.app.presentation.game

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aliasgame.app.R
import com.aliasgame.app.domain.model.Team
import com.aliasgame.app.domain.model.Word
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Interactive card representing a single word to be guessed.
 * Supports vertical swipe gestures for quick scoring.
 */
@Composable
fun WordCard(
    word: Word,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit
) {
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    
    // Dynamic background color feedback based on swipe direction
    val targetColor = when {
        offsetY.value < -150f -> Color(0xFF4CAF50).copy(alpha = 0.4f) // Success Green
        offsetY.value > 150f -> Color(0xFFF44336).copy(alpha = 0.4f)  // Error Red
        else -> Color.White
    }
    
    val backgroundColor by animateColorAsState(targetValue = targetColor, label = "cardColor")

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)
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
                        scope.launch { offsetY.snapTo(offsetY.value + dragAmount.y) }
                    }
                )
            },
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = backgroundColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = word.text,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp)
            )
        }
    }
}

/**
 * Dialog shown when the time is up to process the very last word.
 */
@Composable
fun FinalWordDialog(
    word: String,
    teams: List<Team>,
    onTeamSelected: (String?) -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        shape = RoundedCornerShape(28.dp),
        title = { 
            Text(
                text = "Last word: $word",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column {
                Text("Who got the point?", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                teams.forEach { team ->
                    Button(
                        onClick = { onTeamSelected(team.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
                    ) {
                        Text(team.name)
                    }
                }
                TextButton(
                    onClick = { onTeamSelected(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("No one is correct", color = Color.Gray)
                }
            }
        },
        confirmButton = { }
    )
}

/**
 * Main game session screen.
 * Handles game state transitions, timer, and user interaction during the match.
 */
@Composable
fun GameScreen(
    viewModel: GameViewModel = hiltViewModel(),
    onExit: () -> Unit
) {
    val state by viewModel.gameState.collectAsState()
    val currentState = state ?: return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF6200EE), // Primary Brand Color
                        Color(0xFF03DAC5)  // Secondary Accent
                    )
                )
            )
    ) {
        // Main Gameplay Area
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Exit, Team Info and Circular Timer
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onExit,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Exit", tint = Color.White)
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currentState.currentTeam.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Score: ${currentState.score}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = if (currentState.timeRemaining < 10) Color(0xFFFF5252) else Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(56.dp),
                    border = if (currentState.timeRemaining < 10) BorderStroke(2.dp, Color.White) else null
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = currentState.timeRemaining.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Word Card Display
            currentState.currentWord?.let { word ->
                WordCard(
                    word = word,
                    onSwipeUp = { viewModel.onWordSwiped(true) },
                    onSwipeDown = { viewModel.onWordSwiped(false) }
                )
            } ?: Text("No more words", color = Color.White, style = MaterialTheme.typography.headlineLarge)

            Spacer(modifier = Modifier.weight(1f))

            // Action Controls
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { viewModel.onWordSwiped(false) },
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                    elevation = ButtonDefaults.buttonElevation(8.dp)
                ) {
                    Text("SKIP", fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = { viewModel.pauseGame() },
                    modifier = Modifier.size(64.dp).align(Alignment.CenterVertically),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.3f))
                ) {
                    Text("||", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Button(
                    onClick = { viewModel.onWordSwiped(true) },
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    elevation = ButtonDefaults.buttonElevation(8.dp)
                ) {
                    Text("GOT IT", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Overlay: Pause Screen
        if (currentState.isPaused) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                ElevatedCard(
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Game Paused", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.resumeGame() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
                        ) {
                            Text("Resume Game")
                        }
                    }
                }
            }
        }

        // Overlay: Last Word Mode (Time's Up)
        if (currentState.isLastWordMode) {
            FinalWordDialog(
                word = currentState.currentWord?.text ?: "",
                teams = currentState.allTeams,
                onTeamSelected = { teamId -> viewModel.onFinalWordProcessed(teamId) }
            )
        }

        // Overlay: Round Results
        if (currentState.isRoundOver) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.round_over),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6200EE)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Scoreboard
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            currentState.allTeams.forEach { team ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(team.name, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    Text("${team.score}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Words this round (tap to toggle):",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                        
                        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            itemsIndexed(currentState.roundResults) { index, result ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.toggleWordResult(index) }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = result.word.text,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Icon(
                                        imageVector = if (result.isCorrect) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        tint = if (result.isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336)
                                    )
                                }
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.startNextRound() },
                            modifier = Modifier.fillMaxWidth().height(64.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5))
                        ) {
                            Text(
                                stringResource(R.string.next_round),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFF00332E)
                            )
                        }
                    }
                }
            }
        }

        // Overlay: Pre-round Readiness Screen
        if (!currentState.isRoundOver &&
            !currentState.isPaused &&
            !currentState.isGameFinished &&
            currentState.timeRemaining == currentState.maxTime
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                ElevatedCard(
                    modifier = Modifier.padding(32.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(R.string.ready_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = currentState.currentTeam.name,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF6200EE),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { viewModel.onStartTimer() },
                            modifier = Modifier.fillMaxWidth().height(64.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5))
                        ) {
                            Text(
                                stringResource(R.string.ready_button),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFF00332E)
                            )
                        }
                    }
                }
            }
        }

        // Overlay: Winner/Game Over Screen
        if (currentState.isGameFinished && currentState.winner != null) {
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500)))
                ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(text = "🏆", fontSize = 100.sp)
                    Text(
                        text = stringResource(R.string.game_finished),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = currentState.winner.name,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.points_count, currentState.winner.score),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                    Button(
                        onClick = onExit,
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text(stringResource(R.string.back_to_menu), color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
