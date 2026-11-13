package com.aliasgame.app.presentation.game

import android.media.MediaPlayer
import androidx.compose.animation.*
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
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliasgame.app.R
import com.aliasgame.app.domain.model.RoundResult
import com.aliasgame.app.domain.model.Team
import com.aliasgame.app.domain.model.Word
import com.aliasgame.app.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Main Game Screen with coordinated entrance animations and polished transitions.
 */
@Composable
fun GameScreen(
    viewModel: GameViewModel = hiltViewModel(),
    onExit: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Entrance animation state synchronized with ViewModel
    val entryProgress = remember(state.isEntranceAnimationPlayed) {
        Animatable(if (state.isEntranceAnimationPlayed) 1f else 0f)
    }

    LaunchedEffect(state.isEntranceAnimationPlayed) {
        if (!state.isEntranceAnimationPlayed) {
            val result = entryProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800, easing = EaseOutQuart)
            )
            if (result.endReason == AnimationEndReason.Finished) {
                viewModel.onEntranceAnimationFinished()
            }
        }
    }

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
            .background(Brush.verticalGradient(listOf(BgStart, BgEnd)))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GameItemContainer(progress = entryProgress.value, index = 0) {
                GameHUD(
                    teamName = state.currentTeam?.name ?: "",
                    score = state.score,
                    timeRemaining = state.timeRemaining,
                    onExitClick = viewModel::onExitGame
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            GameItemContainer(progress = entryProgress.value, index = 1) {
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
                    style = Typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            GameItemContainer(progress = entryProgress.value, index = 2) {
                GameControls(
                    isVibrationEnabled = state.isVibrationEnabled,
                    onSkip = { viewModel.onWordSwiped(false) },
                    onPause = viewModel::pauseGame,
                    onCorrect = { viewModel.onWordSwiped(true) }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }

        // State Overlays with consistent Glassmorphism
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

/**
 * Coordinated container for game elements to ensure smooth and staggered entrance.
 */
@Composable
private fun GameItemContainer(
    progress: Float,
    index: Int,
    content: @Composable () -> Unit
) {
    val startThreshold = index * 0.15f
    val itemAlpha = ((progress - startThreshold) / 0.4f).coerceIn(0f, 1f)
    val itemTranslation = (1f - itemAlpha) * 50f

    Box(
        modifier = Modifier
            .graphicsLayer {
                alpha = itemAlpha
                translationY = itemTranslation
            }
    ) {
        content()
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
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
        shape = RoundedCornerShape(24.dp),
        color = GlassWhite,
        border = BorderStroke(1.dp, GlassWhiteBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onExitClick,
                modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Exit", tint = Color.White)
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(teamName.uppercase(), style = Typography.titleMedium, color = Color.White, fontWeight = FontWeight.Black)
                Text(stringResource(R.string.score_label, score), style = Typography.labelLarge, color = GameSecondary)
            }

            Surface(
                shape = CircleShape,
                color = if (timeRemaining < 10) Color(0xFFFF0266) else Color.White.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp),
                border = if (timeRemaining < 10) BorderStroke(2.dp, Color.White) else null
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = timeRemaining.toString(),
                        style = Typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                }
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = { triggerHaptic(); onSkip() },
            modifier = Modifier.weight(1f).height(64.dp),
            shape = RoundedCornerShape(20.dp),
            color = GlassWhite,
            border = BorderStroke(1.dp, GlassWhiteBorder)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.skip_button), style = Typography.titleMedium, color = Color.White)
            }
        }
        
        IconButton(
            onClick = onPause,
            modifier = Modifier.size(56.dp).background(Color.White.copy(alpha = 0.1f), CircleShape)
        ) {
            Text("||", fontWeight = FontWeight.Black, color = Color.White, fontSize = 20.sp)
        }

        Button(
            onClick = { triggerHaptic(); onCorrect() },
            modifier = Modifier.weight(1f).height(64.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GameAccent)
        ) {
            Text(stringResource(R.string.got_it_button), style = Typography.titleMedium, color = Color.White, fontWeight = FontWeight.Black)
        }
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
        offsetY.value < -150f -> Color(0xFF4CAF50).copy(alpha = 0.3f)
        offsetY.value > 150f -> Color(0xFFFF0266).copy(alpha = 0.3f)
        else -> GlassWhite
    }
    
    val backgroundColor by animateColorAsState(targetValue = targetColor, label = "cardColor")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
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
        shape = RoundedCornerShape(40.dp),
        color = backgroundColor,
        border = BorderStroke(2.dp, GlassWhiteBorder)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = word.text,
                style = Typography.displaySmall.copy(fontSize = 42.sp),
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                color = Color.White,
                modifier = Modifier.padding(32.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinalWordDialog(word: String, teams: List<Team>, onTeamSelected: (String?) -> Unit) {
    AlertDialog(
        onDismissRequest = { },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(24.dp).clip(RoundedCornerShape(32.dp)),
        content = {
            Surface(
                color = BgEnd,
                border = BorderStroke(1.dp, GlassWhiteBorder),
                shape = RoundedCornerShape(32.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.last_word_label, word).uppercase(), style = Typography.titleLarge, textAlign = TextAlign.Center, color = Color.White)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(stringResource(R.string.who_got_point), style = Typography.bodyLarge, color = Color.White.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(24.dp))
                    teams.forEach { team ->
                        Button(
                            onClick = { onTeamSelected(team.id) },
                            modifier = Modifier.fillMaxWidth().height(56.dp).padding(vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GlassWhite)
                        ) { Text(team.name, fontWeight = FontWeight.Bold) }
                    }
                    TextButton(onClick = { onTeamSelected(null) }, modifier = Modifier.padding(top = 8.dp)) {
                        Text(stringResource(R.string.no_one_correct), color = Color.White.copy(alpha = 0.5f))
                    }
                }
            }
        }
    )
}

@Composable
private fun PauseOverlay(onContinue: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.padding(32.dp),
            shape = RoundedCornerShape(32.dp),
            color = BgStart,
            border = BorderStroke(1.dp, GlassWhiteBorder)
        ) {
            Column(Modifier.padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.pause_title).uppercase(), style = Typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White)
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GameAccent)
                ) {
                    Text(stringResource(R.string.resume_button).uppercase(), fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun RoundResultsOverlay(results: List<RoundResult>, teams: List<Team>, onToggleResult: (Int) -> Unit, onNextRound: () -> Unit) {
    Box(Modifier.fillMaxSize().background(BgStart.copy(alpha = 0.95f)).padding(20.dp), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(32.dp),
            color = GlassWhite,
            border = BorderStroke(1.dp, GlassWhiteBorder)
        ) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.round_over).uppercase(), style = Typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White)
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    teams.forEach { team ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(team.name, style = Typography.labelLarge, color = Color.White.copy(alpha = 0.6f))
                            Text("${team.score}", style = Typography.titleLarge, fontWeight = FontWeight.Black, color = GameSecondary)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Text(stringResource(R.string.tap_to_toggle), style = Typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
                
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    itemsIndexed(results) { index, result ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onToggleResult(index) }.padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(result.word.text, style = Typography.bodyLarge, color = Color.White)
                            Icon(
                                imageVector = if (result.isCorrect) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (result.isCorrect) Color(0xFF4CAF50) else Color(0xFFFF0266)
                            )
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onNextRound,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GameAccent)
                ) {
                    Text(stringResource(R.string.next_round).uppercase(), style = Typography.titleMedium, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun ReadyOverlay(teamName: String, onStart: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(1.0f, 1.05f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "s")

    Box(Modifier.fillMaxSize().background(BgStart.copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.padding(24.dp).graphicsLayer(scaleX = scale, scaleY = scale),
            shape = RoundedCornerShape(40.dp),
            color = GlassWhite,
            border = BorderStroke(2.dp, GameSecondary)
        ) {
            Column(Modifier.padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.ready_title).uppercase(), style = Typography.labelLarge, color = Color.White.copy(alpha = 0.7f))
                Text(teamName.uppercase(), style = Typography.displaySmall, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(48.dp))
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GameSecondary)
                ) {
                    Text(stringResource(R.string.ready_button).uppercase(), style = Typography.titleLarge, color = BgStart, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun VictoryOverlay(winners: List<Team>, onExit: () -> Unit) {
    val isDraw = winners.size > 1
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFFFD700), BgStart))), contentAlignment = Alignment.Center) {
        ConfettiEffect()
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text(text = "🏆", fontSize = 100.sp)
            Text(if (isDraw) stringResource(R.string.draw_title) else stringResource(R.string.game_finished), style = Typography.displaySmall, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            winners.forEach { winner -> Text(winner.name.uppercase(), style = Typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White) }
            Text(stringResource(R.string.points_count, winners.firstOrNull()?.score ?: 0), style = Typography.titleLarge, color = GameSecondary)
            Spacer(modifier = Modifier.height(64.dp))
            Button(
                onClick = onExit,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text(stringResource(R.string.back_to_menu).uppercase(), color = BgStart, fontWeight = FontWeight.Black)
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
                    size = androidx.compose.ui.geometry.Size(20f, 10f)
                )
            }
        }
    }
}
