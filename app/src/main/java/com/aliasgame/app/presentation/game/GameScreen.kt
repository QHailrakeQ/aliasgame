package com.aliasgame.app.presentation.game

import android.media.MediaPlayer
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliasgame.app.R
import com.aliasgame.app.domain.model.RoundResult
import com.aliasgame.app.domain.model.Team
import com.aliasgame.app.domain.model.Word
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Main Game Screen implementation.
 * Manages side effects (sounds, navigation) and orchestrates game sub-components.
 */
@Composable
fun GameScreen(
    viewModel: GameViewModel = hiltViewModel(),
    onExit: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Audio Players management
    val correctPlayer = remember { try { MediaPlayer.create(context, R.raw.correct_sound) } catch (e: Exception) { null } }
    val skipPlayer = remember { try { MediaPlayer.create(context, R.raw.skip_sound) } catch (e: Exception) { null } }
    val timerEndPlayer = remember { try { MediaPlayer.create(context, R.raw.last_word_sound) } catch (e: Exception) { null } }

    DisposableEffect(Unit) {
        onDispose {
            correctPlayer?.release()
            skipPlayer?.release()
            timerEndPlayer?.release()
        }
    }

    // Handle Side Effects
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                GameUiEffect.PlayCorrectSound -> if (state.isSoundEnabled) correctPlayer?.start()
                GameUiEffect.PlaySkipSound -> if (state.isSoundEnabled) skipPlayer?.start()
                GameUiEffect.PlayTimerEndSound -> if (state.isSoundEnabled) timerEndPlayer?.start()
                GameUiEffect.NavigateBack -> onExit()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF6200EE), Color(0xFF03DAC5))))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GameHUD(
                teamName = state.currentTeam?.name ?: "",
                score = state.score,
                timeRemaining = state.timeRemaining,
                onExitClick = viewModel::onExitGame
            )

            Spacer(modifier = Modifier.weight(1f))

            state.currentWord?.let { word ->
                WordCard(
                    word = word,
                    vibrationEnabled = state.isVibrationEnabled,
                    onSwipeUp = { viewModel.onWordSwiped(true) },
                    onSwipeDown = { viewModel.onWordSwiped(false) }
                )
            } ?: Text(
                text = stringResource(R.string.no_more_words),
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.weight(1f))

            GameControls(
                isVibrationEnabled = state.isVibrationEnabled,
                onSkip = { viewModel.onWordSwiped(false) },
                onPause = viewModel::pauseGame,
                onCorrect = { viewModel.onWordSwiped(true) }
            )
        }

        // State Overlays
        if (state.isPaused) {
            PauseOverlay(onContinue = viewModel::resumeGame)
        }

        if (state.isLastWordMode) {
            FinalWordDialog(
                word = state.currentWord?.text ?: "",
                teams = state.allTeams,
                onTeamSelected = viewModel::onFinalWordProcessed
            )
        }

        if (state.isRoundOver) {
            RoundResultsOverlay(
                results = state.roundResults,
                teams = state.allTeams,
                onToggleResult = viewModel::toggleWordResult,
                onNextRound = viewModel::startNextRound
            )
        }

        if (state.isReadyToStart()) {
            ReadyOverlay(
                teamName = state.currentTeam?.name ?: "",
                onStart = viewModel::onStartTimer
            )
        }

        if (state.isGameFinished && state.winners.isNotEmpty()) {
            VictoryOverlay(
                winners = state.winners,
                onExit = viewModel::onExitGame
            )
        }
    }
}

private fun GameUiState.isReadyToStart() = 
    !isRoundOver && !isPaused && !isGameFinished && timeRemaining == 0L && currentTeam != null

@Composable
private fun GameHUD(
    teamName: String,
    score: Int,
    timeRemaining: Long,
    onExitClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onExitClick,
            modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Exit", tint = Color.White)
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(teamName, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.score_label, score), style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.8f))
        }

        Surface(
            shape = CircleShape,
            color = if (timeRemaining < 10) Color(0xFFFF5252) else Color.White.copy(alpha = 0.2f),
            modifier = Modifier.size(56.dp),
            border = if (timeRemaining < 10) BorderStroke(2.dp, Color.White) else null
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(timeRemaining.toString(), style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun GameControls(
    isVibrationEnabled: Boolean,
    onSkip: () -> Unit,
    onPause: () -> Unit,
    onCorrect: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val triggerHaptic = { if (isVibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ControlButton(stringResource(R.string.skip_button), Color(0xFFFF5252), 100.dp) {
            triggerHaptic(); onSkip()
        }
        
        IconButton(
            onClick = onPause,
            modifier = Modifier.size(64.dp).background(Color.White.copy(alpha = 0.3f), CircleShape).align(Alignment.CenterVertically)
        ) {
            Text("||", fontWeight = FontWeight.Bold, color = Color.White)
        }

        ControlButton(stringResource(R.string.got_it_button), Color(0xFF4CAF50), 100.dp) {
            triggerHaptic(); onCorrect()
        }
    }
}

@Composable
private fun ControlButton(text: String, color: Color, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(size),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        elevation = ButtonDefaults.buttonElevation(8.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
fun WordCard(
    word: Word,
    vibrationEnabled: Boolean,
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
                                if (vibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSwipeUp()
                            } else if (offsetY.value > 400f) {
                                if (vibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
            Text(word.text, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
        }
    }
}

@Composable
fun FinalWordDialog(word: String, teams: List<Team>, onTeamSelected: (String?) -> Unit) {
    AlertDialog(
        onDismissRequest = { },
        shape = RoundedCornerShape(28.dp),
        title = { Text(stringResource(R.string.last_word_label, word), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(stringResource(R.string.who_got_point), style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                teams.forEach { team ->
                    Button(
                        onClick = { onTeamSelected(team.id) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
                    ) { Text(team.name) }
                }
                TextButton(onClick = { onTeamSelected(null) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.no_one_correct), color = Color.Gray)
                }
            }
        },
        confirmButton = { }
    )
}

@Composable
private fun PauseOverlay(onContinue: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
        ElevatedCard(shape = RoundedCornerShape(32.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
            Column(Modifier.padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.pause_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))) {
                    Text(stringResource(R.string.resume_button))
                }
            }
        }
    }
}

@Composable
private fun RoundResultsOverlay(results: List<RoundResult>, teams: List<Team>, onToggleResult: (Int) -> Unit, onNextRound: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).padding(24.dp), contentAlignment = Alignment.Center) {
        ElevatedCard(Modifier.fillMaxSize(), shape = RoundedCornerShape(32.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.round_over), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF6200EE))
                Spacer(modifier = Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    teams.forEach { team ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(team.name, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text("${team.score}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(stringResource(R.string.tap_to_toggle), style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    itemsIndexed(results) { index, result ->
                        Row(Modifier.fillMaxWidth().clickable { onToggleResult(index) }.padding(vertical = 12.dp, horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(result.word.text, style = MaterialTheme.typography.bodyLarge)
                            Icon(imageVector = if (result.isCorrect) Icons.Default.Check else Icons.Default.Close, contentDescription = null, tint = if (result.isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336))
                        }
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onNextRound, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5))) {
                    Text(stringResource(R.string.next_round), style = MaterialTheme.typography.titleLarge, color = Color(0xFF00332E))
                }
            }
        }
    }
}

@Composable
private fun ReadyOverlay(teamName: String, onStart: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(initialValue = 1.0f, targetValue = 1.05f, animationSpec = infiniteRepeatable(animation = tween(800, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "scale")

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
        ElevatedCard(Modifier.padding(32.dp), shape = RoundedCornerShape(32.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
            Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.ready_title), style = MaterialTheme.typography.titleMedium)
                Text(teamName, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = Color(0xFF6200EE), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(64.dp).graphicsLayer(scaleX = scale, scaleY = scale), shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5))) {
                    Text(stringResource(R.string.ready_button), style = MaterialTheme.typography.titleLarge, color = Color(0xFF00332E))
                }
            }
        }
    }
}

@Composable
private fun VictoryOverlay(winners: List<Team>, onExit: () -> Unit) {
    val isDraw = winners.size > 1
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500)))), contentAlignment = Alignment.Center) {
        ConfettiEffect()
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text(text = "🏆", fontSize = 100.sp)
            Text(if (isDraw) stringResource(R.string.draw_title) else stringResource(R.string.game_finished), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(16.dp))
            if (isDraw) Text(stringResource(R.string.winners_share), style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.9f))
            winners.forEach { winner -> Text(winner.name, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center) }
            Text(stringResource(R.string.points_count, winners.firstOrNull()?.score ?: 0), style = MaterialTheme.typography.headlineSmall, color = Color.White.copy(alpha = 0.9f))
            Spacer(Modifier.height(48.dp))
            Button(onClick = onExit, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White)) {
                Text(stringResource(R.string.back_to_menu), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ConfettiEffect() {
    val progress by rememberInfiniteTransition(label = "conf").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing)),
        label = "p"
    )
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        repeat(70) { i ->
            val y = ((progress + (i * 0.015f)) % 1f) * size.height
            val x = (i * 0.014f) * size.width
            rotate(progress * 360) {
                drawRect(
                    color = Color(android.graphics.Color.HSVToColor(floatArrayOf((i * 5f) % 360, 1f, 1f))),
                    topLeft = Offset(x, y),
                    size = Size(20f, 10f)
                )
            }
        }
    }
}
