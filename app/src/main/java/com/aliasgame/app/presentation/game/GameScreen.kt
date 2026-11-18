package com.aliasgame.app.presentation.game

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.MediaPlayer
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliasgame.app.R
import com.aliasgame.app.domain.model.RoundResult
import com.aliasgame.app.domain.model.Team
import com.aliasgame.app.domain.model.Word
import com.aliasgame.app.ui.components.PartyBackground
import com.aliasgame.app.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Main Game Screen with responsive visual hierarchy and polished transitions.
 * Refined in Phase 6 for better accessibility and multi-device support.
 */
@Composable
fun GameScreen(
    viewModel: GameViewModel = hiltViewModel(),
    onExit: () -> Unit,
    onNewGame: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showExitConfirmation by remember { mutableStateOf(false) }
    var resumeAfterExitCancellation by remember { mutableStateOf(false) }
    val soundEnabled by rememberUpdatedState(state.isSoundEnabled)

    val requestExit = {
        if (!showExitConfirmation) {
            resumeAfterExitCancellation = state.isTimerRunning && state.timeRemaining > 0
            if (resumeAfterExitCancellation) viewModel.pauseGame()
            showExitConfirmation = true
        }
    }
    val cancelExit = {
        showExitConfirmation = false
        if (resumeAfterExitCancellation) viewModel.resumeGame()
        resumeAfterExitCancellation = false
    }

    BackHandler(enabled = !showExitConfirmation) {
        requestExit()
    }

    // Pause the countdown when the game leaves the screen, but not for a configuration change
    // (e.g. rotation), where the activity is only recreated and play continues.
    val activity = remember(context) { context.findActivity() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (activity?.isChangingConfigurations != true) viewModel.onAppBackgrounded()
    }

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
                GameUiEffect.PlayCorrectSound -> if (soundEnabled) correctPlayer?.start()
                GameUiEffect.PlaySkipSound -> if (soundEnabled) skipPlayer?.start()
                GameUiEffect.PlayTimerEndSound -> if (soundEnabled) timerEndPlayer?.start()
                GameUiEffect.NavigateBack -> onExit()
                GameUiEffect.NavigateToSetup -> onNewGame()
            }
        }
    }

    PartyBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                // While an overlay covers the game, hide the controls underneath from
                // accessibility services as well, not only from touch.
                .then(if (state.hasBlockingOverlay()) Modifier.clearAndSetSemantics {} else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GameItemContainer(progress = entryProgress.value, index = 0) {
                GameHUD(
                    currentTeamId = state.currentTeam?.id,
                    teamName = state.currentTeam?.name ?: "",
                    teams = state.allTeams,
                    score = state.score,
                    timeRemaining = state.timeRemaining,
                    onExitClick = requestExit
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Responsive Word Area: fills all space between HUD and Controls
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = state.currentWord,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                         scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
                            .togetherWith(fadeOut(animationSpec = tween(90)))
                    },
                    label = "WordTransition",
                    modifier = Modifier.fillMaxSize()
                ) { targetWord ->
                    if (targetWord != null) {
                        WordCard(
                            word = targetWord,
                            vibrationEnabled = state.isVibrationEnabled,
                            onSwipeUp = { viewModel.onWordSwiped(true) },
                            onSwipeDown = { viewModel.onWordSwiped(false) }
                        )
                    } else if (state.phase != null) {
                        // Only after loading: a loaded game without a word ran out of words.
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_more_words),
                                color = Color.White.copy(alpha = 0.7f),
                                style = Typography.headlineMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            GameItemContainer(progress = entryProgress.value, index = 2) {
                GameControls(
                    isVibrationEnabled = state.isVibrationEnabled,
                    onSkip = { viewModel.onWordSwiped(false) },
                    onPause = viewModel::pauseGame,
                    onCorrect = { viewModel.onWordSwiped(true) }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }

        // State Overlays
        if (state.isPaused) {
            PauseOverlay(onContinue = viewModel::resumeGame)
        }

        if (state.isLastWordMode) {
            FinalWordDialog(
                word = state.currentWord?.text ?: "",
                teams = state.allTeams,
                onTeamSelected = viewModel::onFinalWordProcessed,
                onRequestExit = requestExit
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

        if (state.isReadyToStart) {
            ReadyOverlay(
                teamName = state.currentTeam?.name ?: "",
                onStart = viewModel::onStartTimer
            )
        }

        if (state.isGameFinished && state.winners.isNotEmpty()) {
            VictoryOverlay(
                winners = state.winners,
                onNewGame = viewModel::onStartNewGame,
                onExit = viewModel::onExitGame
            )
        }

        if (showExitConfirmation) {
            AlertDialog(
                onDismissRequest = cancelExit,
                title = { Text(stringResource(R.string.exit_confirmation_title)) },
                text = { Text(stringResource(R.string.exit_confirmation_message)) },
                confirmButton = {
                    Button(
                        onClick = {
                            showExitConfirmation = false
                            resumeAfterExitCancellation = false
                            viewModel.onExitGame()
                        }
                    ) {
                        Text(stringResource(R.string.exit_confirmation_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = cancelExit) {
                        Text(stringResource(R.string.exit_confirmation_cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun GameItemContainer(
    progress: Float,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val startThreshold = index * 0.15f
    val itemAlpha = ((progress - startThreshold) / 0.4f).coerceIn(0f, 1f)
    val itemTranslation = (1f - itemAlpha) * 50f

    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = itemAlpha
                translationY = itemTranslation
            }
    ) {
        content()
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Full-screen overlays drawn in this composable (the last-word dialog is its own window). */
private fun GameUiState.hasBlockingOverlay() =
    isPaused || isRoundOver || isReadyToStart

/**
 * Consumes every pointer event that reaches the overlay, so controls drawn underneath
 * cannot be tapped through it. Children of the overlay still receive events first.
 * A Box with only a background does not take part in hit testing in Compose.
 */
private fun Modifier.blockUnderlyingTouches(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent().changes.forEach { it.consume() }
        }
    }
}

@Composable
private fun GameHUD(
    currentTeamId: String?,
    teamName: String,
    teams: List<Team>,
    score: Int,
    timeRemaining: Long,
    onExitClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        shape = RoundedCornerShape(GameShapeSize.card),
        color = GlassWhite,
        border = BorderStroke(1.dp, GlassWhiteBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(GameSpacing.controlGap)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onExitClick,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Close,
                        contentDescription = stringResource(R.string.content_desc_exit),
                        tint = Color.White
                    )
                }

                Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = teamName.uppercase(),
                        style = Typography.titleMedium.copy(letterSpacing = 1.sp),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.turn_score_label, score),
                        style = Typography.labelLarge,
                        color = GameSecondary
                    )
                }

                val isUrgent = timeRemaining < 10
                val timerBorderColor by animateColorAsState(
                    targetValue = if (isUrgent) GameAccent else Color.White.copy(alpha = 0.2f),
                    label = "TimerBorder"
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isUrgent) GameAccent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.widthIn(min = 64.dp),
                    border = BorderStroke(if (isUrgent) 2.dp else 1.dp, timerBorderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isUrgent) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp).padding(end = 4.dp)
                            )
                        }
                        Text(
                            text = timeRemaining.toString(),
                            style = Typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                teams.forEach { team ->
                    val teamScoreDescription = stringResource(
                        R.string.team_score_description,
                        team.name,
                        team.score
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (team.id == currentTeamId) {
                            GameSecondary.copy(alpha = 0.22f)
                        } else {
                            Color.White.copy(alpha = 0.07f)
                        },
                        modifier = Modifier.semantics(mergeDescendants = true) {
                            contentDescription = teamScoreDescription
                        },
                        border = BorderStroke(
                            1.dp,
                            if (team.id == currentTeamId) GameSecondary.copy(alpha = 0.7f)
                            else Color.White.copy(alpha = 0.12f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = team.name,
                                style = Typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = team.score.toString(),
                                style = Typography.labelMedium,
                                color = GameSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
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
        // Skip Button
        Surface(
            onClick = { triggerHaptic(); onSkip() },
            modifier = Modifier.weight(1f).height(64.dp),
            shape = RoundedCornerShape(GameShapeSize.control),
            color = GlassWhite,
            border = BorderStroke(1.dp, GlassWhiteBorder)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.skip_button),
                    style = Typography.titleMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        // Pause Button
        val pauseDesc = stringResource(R.string.content_desc_pause)
        Surface(
            onClick = onPause,
            modifier = Modifier
                .size(56.dp)
                .semantics { contentDescription = pauseDesc },
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, GlassWhiteBorder)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Pause, contentDescription = null, tint = Color.White)
            }
        }

        // Correct Button
        Button(
            onClick = { triggerHaptic(); onCorrect() },
            modifier = Modifier.weight(1f).height(64.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GameAccent),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.got_it_button),
                    style = Typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
        offsetY.value < -100f -> Color(0xFF4CAF50).copy(alpha = 0.3f) // Green for Correct
        offsetY.value > 100f -> GameAccent.copy(alpha = 0.3f)        // Pink/Red for Skip
        else -> GlassWhite
    }
    
    val backgroundColor by animateColorAsState(targetValue = targetColor, label = "cardColor")

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp)
    ) {
        val density = LocalDensity.current
        val swipeThreshold = with(density) { maxHeight.toPx() * 0.3f }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .pointerInput(word) {
                    detectDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (offsetY.value < -swipeThreshold) {
                                    if (vibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSwipeUp()
                                } else if (offsetY.value > swipeThreshold) {
                                    if (vibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSwipeDown()
                                }
                                offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessLow))
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch { offsetY.snapTo(offsetY.value + dragAmount.y) }
                        }
                    )
                },
            shape = RoundedCornerShape(GameShapeSize.card),
            color = backgroundColor,
            border = BorderStroke(2.dp, GlassWhiteBorder)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(GameSpacing.cardPadding),
                contentAlignment = Alignment.Center
            ) {
                // Adaptive text sizing for long words
                var textSize by remember(word) { mutableStateOf(42.sp) }

                Text(
                    text = word.text,
                    style = Typography.displaySmall.copy(fontSize = textSize),
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { textLayoutResult ->
                        if (textLayoutResult.hasVisualOverflow && textSize > 24.sp) {
                            textSize *= 0.9f
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinalWordDialog(
    word: String,
    teams: List<Team>,
    onTeamSelected: (String?) -> Unit,
    onRequestExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onRequestExit,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        ),
        modifier = Modifier.padding(GameSpacing.screenHorizontal).clip(
            RoundedCornerShape(GameShapeSize.card)
        ),
        content = {
            Surface(
                color = BgEnd,
                border = BorderStroke(1.dp, GlassWhiteBorder),
                shape = RoundedCornerShape(GameShapeSize.card)
            ) {
                Column(
                    modifier = Modifier
                        .heightIn(max = 640.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(GameSpacing.cardPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.last_word_label, word).uppercase(),
                        style = Typography.titleLarge,
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.who_got_point),
                        style = Typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    teams.forEach { team ->
                        Button(
                            onClick = { onTeamSelected(team.id) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 4.dp),
                            shape = RoundedCornerShape(GameShapeSize.control),
                            colors = ButtonDefaults.buttonColors(containerColor = GlassWhite)
                        ) { Text(team.name, fontWeight = FontWeight.Bold) }
                    }
                    
                    Button(
                        onClick = { onTeamSelected(null) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 4.dp),
                        shape = RoundedCornerShape(GameShapeSize.control),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) { 
                        Text(
                            text = stringResource(R.string.no_one_correct),
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun PauseOverlay(onContinue: () -> Unit) {
    Box(
        Modifier.fillMaxSize().blockUnderlyingTouches().background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.padding(GameSpacing.cardPadding),
            shape = RoundedCornerShape(GameShapeSize.card),
            color = BgStart,
            border = BorderStroke(1.dp, GlassWhiteBorder)
        ) {
            Column(
                Modifier.padding(GameSpacing.cardPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.pause_title).uppercase(),
                    style = Typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
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
    Box(
        Modifier.fillMaxSize().blockUnderlyingTouches().background(BgStart.copy(alpha = 0.95f)).padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(GameShapeSize.card),
            color = GlassWhite,
            border = BorderStroke(1.dp, GlassWhiteBorder)
        ) {
            Column(
                Modifier.fillMaxSize().padding(GameSpacing.cardPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.round_over).uppercase(),
                    style = Typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(GameSpacing.cardGap))
                
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(GameSpacing.cardGap)
                ) {
                    teams.forEach { team ->
                        Column(
                            modifier = Modifier.widthIn(min = 72.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                team.name,
                                style = Typography.labelLarge,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                team.score.toString(),
                                style = Typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = GameSecondary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(GameSpacing.cardGap))
                Text(stringResource(R.string.tap_to_toggle), style = Typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
                
                val correctDesc = stringResource(R.string.content_desc_correct_icon)
                val wrongDesc = stringResource(R.string.content_desc_wrong_icon)

                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    itemsIndexed(results) { index, result ->
                        val isLastWord = index == results.size - 1
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .then(
                                    if (!isLastWord) {
                                        Modifier.clickable { onToggleResult(index) }
                                            .semantics { role = Role.Button }
                                    } else {
                                        Modifier
                                    }
                                )
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = result.word.text + if (isLastWord) " (${stringResource(R.string.got_it_button)})" else "",
                                modifier = Modifier.weight(1f).padding(end = GameSpacing.controlGap),
                                style = Typography.bodyLarge,
                                color = if (isLastWord) Color.White.copy(alpha = 0.7f) else Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = if (result.isCorrect) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = if (result.isCorrect) correctDesc else wrongDesc,
                                tint = if (result.isCorrect) GameSecondary else GameAccent
                            )
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onNextRound,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(GameShapeSize.control),
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
    Box(
        Modifier.fillMaxSize().blockUnderlyingTouches().background(BgStart.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.padding(GameSpacing.cardPadding),
            shape = RoundedCornerShape(GameShapeSize.card),
            color = GlassWhite,
            border = BorderStroke(2.dp, GameSecondary)
        ) {
            Column(
                Modifier.padding(GameSpacing.cardPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.ready_title).uppercase(), style = Typography.labelLarge, color = Color.White.copy(alpha = 0.7f))
                Text(teamName.uppercase(), style = Typography.displaySmall, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(GameSpacing.section))
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                    shape = RoundedCornerShape(GameShapeSize.control),
                    colors = ButtonDefaults.buttonColors(containerColor = GameSecondary)
                ) {
                    Text(stringResource(R.string.ready_button).uppercase(), style = Typography.titleLarge, color = BgStart, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun VictoryOverlay(
    winners: List<Team>,
    onNewGame: () -> Unit,
    onExit: () -> Unit
) {
    val isDraw = winners.size > 1
    Box(
        modifier = Modifier
            .fillMaxSize()
            .blockUnderlyingTouches()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF35105F),
                        0.45f to Color(0xFF1F0B4D),
                        1f to Color(0xFF10052E)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GameAccent.copy(alpha = 0.32f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.22f),
                    radius = size.width * 0.82f
                ),
                radius = size.width * 0.82f,
                center = Offset(size.width * 0.5f, size.height * 0.22f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GameSecondary.copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.82f),
                    radius = size.width * 0.72f
                ),
                radius = size.width * 0.72f,
                center = Offset(size.width * 0.5f, size.height * 0.82f)
            )
        }
        ConfettiEffect()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(144.dp),
                shape = CircleShape,
                color = Color(0xFF29145A).copy(alpha = 0.88f),
                border = BorderStroke(2.dp, Color(0xFFFFD166).copy(alpha = 0.82f)),
                shadowElevation = 28.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        modifier = Modifier.size(116.dp),
                        shape = CircleShape,
                        color = GameAccent.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, GameSecondary.copy(alpha = 0.30f))
                    ) {}
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = stringResource(R.string.winner_title),
                        tint = Color(0xFFFFD166),
                        modifier = Modifier.size(82.dp)
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = if (isDraw) {
                    stringResource(R.string.draw_title)
                } else {
                    stringResource(R.string.winner_title)
                },
                style = Typography.headlineMedium.copy(fontSize = 25.sp, lineHeight = 32.sp),
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(24.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = Color(0xFF211044).copy(alpha = 0.90f),
                border = BorderStroke(1.dp, GameSecondary.copy(alpha = 0.30f)),
                shadowElevation = 24.dp
            ) {
                Box(
                    modifier = Modifier.background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.015f)
                            )
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isDraw) {
                            Text(
                                text = stringResource(R.string.winners_share),
                                style = Typography.labelLarge,
                                color = GameSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                        winners.forEachIndexed { index, winner ->
                            if (index > 0) Spacer(Modifier.height(10.dp))
                            Text(
                                text = winner.name.uppercase(),
                                style = Typography.displaySmall.copy(fontSize = 36.sp, lineHeight = 42.sp),
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(22.dp))
                        Box(
                            modifier = Modifier
                                .width(64.dp)
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(GameAccent, GameSecondary)
                                    )
                                )
                        )
                        Spacer(Modifier.height(18.dp))
                        Text(
                            text = stringResource(
                                R.string.points_count,
                                winners.firstOrNull()?.score ?: 0
                            ),
                            style = Typography.titleLarge.copy(fontSize = 24.sp),
                            color = Color(0xFFFFD166),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                onClick = onNewGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp),
                shape = RoundedCornerShape(GameShapeSize.control),
                color = GameAccent,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                shadowElevation = 14.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFF3478), Color(0xFF9C32FF))
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.new_game).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        style = Typography.titleMedium
                    )
                }
            }
            TextButton(
                onClick = onExit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(
                    text = stringResource(R.string.back_to_menu),
                    color = Color.White.copy(alpha = 0.78f),
                    style = Typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun ConfettiEffect() {
    val density = LocalDensity.current.density
    val particles = remember(density) {
        val random = Random(System.nanoTime())
        List(52) {
            ConfettiParticle(
                x = random.nextFloat(),
                startDelay = random.nextFloat() * 0.22f,
                fallDuration = 0.58f + random.nextFloat() * 0.24f,
                sway = (8f + random.nextFloat() * 20f) * density,
                rotation = random.nextFloat() * 360f,
                spin = -240f + random.nextFloat() * 480f,
                size = (5f + random.nextFloat() * 7f) * density,
                shape = random.nextInt(3),
                color = when (random.nextInt(4)) {
                    0 -> GameSecondary
                    1 -> GameAccent
                    2 -> Color(0xFFFFD166)
                    else -> Color.White
                }
            )
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 5200, easing = LinearEasing)
        )
    }

    Canvas(Modifier.fillMaxSize()) {
        particles.forEach { particle ->
            val particleProgress = (
                (progress.value - particle.startDelay) /
                    (1f - particle.startDelay)
                ).coerceIn(0f, 1f)
            val fall = (particleProgress / particle.fallDuration).coerceIn(0f, 1f)
            val y = fall * (size.height + 64f * density) - 32f * density
            val x = (
                particle.x * size.width +
                    sin((fall * 1.8f + particle.x) * (2f * Math.PI).toFloat()) * particle.sway
                ).coerceIn(0f, size.width)
            val fadeOut = (1f - ((fall - 0.82f) / 0.18f).coerceIn(0f, 1f))
            val color = particle.color.copy(alpha = fadeOut)
            val center = Offset(x, y)
            rotate(
                degrees = particle.rotation + fall * particle.spin,
                pivot = center
            ) {
                when (particle.shape) {
                    0 -> drawRoundRect(
                        color = color,
                        topLeft = Offset(x - particle.size, y - particle.size / 2f),
                        size = androidx.compose.ui.geometry.Size(
                            particle.size * 2f,
                            particle.size
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            particle.size / 3f
                        )
                    )
                    1 -> drawCircle(
                        color = color,
                        radius = particle.size * 0.65f,
                        center = center
                    )
                    else -> drawOval(
                        color = color,
                        topLeft = Offset(x - particle.size, y - particle.size / 2f),
                        size = androidx.compose.ui.geometry.Size(
                            particle.size * 2f,
                            particle.size
                        )
                    )
                }
            }
        }
    }
}

private data class ConfettiParticle(
    val x: Float,
    val startDelay: Float,
    val fallDuration: Float,
    val sway: Float,
    val rotation: Float,
    val spin: Float,
    val size: Float,
    val shape: Int,
    val color: Color
)
