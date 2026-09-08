package com.aliasgame.app.presentation.game

import android.media.MediaPlayer
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aliasgame.app.R
import com.aliasgame.app.domain.model.GameState
import com.aliasgame.app.domain.model.RoundResult
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
    val haptic = LocalHapticFeedback.current
    
    val targetColor = when {
        offsetY.value < -150f -> Color(0xFF4CAF50).copy(alpha = 0.4f)
        offsetY.value > 150f -> Color(0xFFF44336).copy(alpha = 0.4f)
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
                            if (offsetY.value < -400f) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSwipeUp()
                            } else if (offsetY.value > 400f) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSwipeDown()
                            }
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
 * Dialog for selecting which team gets the point for the final word.
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
 * Main game session screen with audio feedback and state overlays.
 */
@Composable
fun GameScreen(
    viewModel: GameViewModel = hiltViewModel(),
    onExit: () -> Unit
) {
    val state by viewModel.gameState.collectAsState()
    val currentState = state ?: return
    
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Initialize sound players with null-safety
    val correctPlayer = remember {
        try { MediaPlayer.create(context, R.raw.correct_sound) } catch (e: Exception) { null }
    }
    val skipPlayer = remember {
        try { MediaPlayer.create(context, R.raw.skip_sound) } catch (e: Exception) { null }
    }

    // Clean up media resources
    DisposableEffect(Unit) {
        onDispose {
            correctPlayer?.release()
            skipPlayer?.release()
        }
    }

    // Centralized word action logic for both swipes and buttons
    val handleCorrect = {
        correctPlayer?.let { if (it.isPlaying) it.pause(); it.seekTo(0); it.start() }
        viewModel.onWordSwiped(true)
    }

    val handleSkip = {
        skipPlayer?.let { if (it.isPlaying) it.pause(); it.seekTo(0); it.start() }
        viewModel.onWordSwiped(false)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF6200EE), Color(0xFF03DAC5))
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // HUD: Top bar
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

            currentState.currentWord?.let { word ->
                WordCard(
                    word = word,
                    onSwipeUp = handleCorrect,
                    onSwipeDown = handleSkip
                )
            } ?: Text("No more words", color = Color.White, style = MaterialTheme.typography.headlineLarge)

            Spacer(modifier = Modifier.weight(1f))

            // Action Controls
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); handleSkip() },
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
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); handleCorrect() },
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    elevation = ButtonDefaults.buttonElevation(8.dp)
                ) {
                    Text("GOT IT", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Overlay implementations (Pause, Results, Ready, Victory)
        if (currentState.isPaused) {
            PauseOverlay(onContinue = { viewModel.resumeGame() })
        }

        if (currentState.isLastWordMode) {
            FinalWordDialog(
                word = currentState.currentWord?.text ?: "",
                teams = currentState.allTeams,
                onTeamSelected = { teamId: String? -> viewModel.onFinalWordProcessed(teamId) }
            )
        }

        if (currentState.isRoundOver) {
            RoundResultsOverlay(
                results = currentState.roundResults,
                teams = currentState.allTeams,
                onToggleResult = { viewModel.toggleWordResult(it) },
                onNextRound = { viewModel.startNextRound() }
            )
        }

        if (currentState.isReadyToStart()) {
            ReadyOverlay(
                teamName = currentState.currentTeam.name,
                onStart = { viewModel.onStartTimer() }
            )
        }

        if (currentState.isGameFinished && currentState.winner != null) {
            VictoryOverlay(
                winner = currentState.winner,
                onExit = onExit
            )
        }
    }
}

private fun GameState.isReadyToStart() = !isRoundOver && !isPaused && !isGameFinished && timeRemaining == maxTime

@Composable
private fun PauseOverlay(onContinue: () -> Unit) {
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
                    onClick = onContinue,
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

@Composable
private fun RoundResultsOverlay(
    results: List<RoundResult>,
    teams: List<Team>,
    onToggleResult: (Int) -> Unit,
    onNextRound: () -> Unit
) {
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
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    teams.forEach { team ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(team.name, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text("${team.score}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Text("Tap word to toggle:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    itemsIndexed(results) { index, result ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleResult(index) }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(result.word.text, style = MaterialTheme.typography.bodyLarge)
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
                    onClick = onNextRound,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5))
                ) {
                    Text(stringResource(R.string.next_round), style = MaterialTheme.typography.titleLarge, color = Color(0xFF00332E))
                }
            }
        }
    }
}

@Composable
private fun ReadyOverlay(teamName: String, onStart: () -> Unit) {
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
                    text = teamName,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF6200EE),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5))
                ) {
                    Text(stringResource(R.string.ready_button), style = MaterialTheme.typography.titleLarge, color = Color(0xFF00332E))
                }
            }
        }
    }
}

@Composable
private fun VictoryOverlay(winner: Team, onExit: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500)))
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text(text = "🏆", fontSize = 100.sp)
            Text(stringResource(R.string.game_finished), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = winner.name, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = Color.White)
            Text(stringResource(R.string.points_count, winner.score), style = MaterialTheme.typography.headlineSmall, color = Color.White.copy(alpha = 0.9f))
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
