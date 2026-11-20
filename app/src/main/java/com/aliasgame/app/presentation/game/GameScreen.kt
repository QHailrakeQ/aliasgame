package com.aliasgame.app.presentation.game

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliasgame.app.R
import com.aliasgame.app.domain.model.Word
import com.aliasgame.app.ui.components.PartyBackground
import com.aliasgame.app.ui.theme.*

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

    // Null until the player has been created in the background; the effect collector below
    // starts once, so it reads the latest value through rememberUpdatedState.
    val soundPlayer by rememberUpdatedState(rememberGameSoundPlayer())

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is GameUiEffect.PlaySound -> if (soundEnabled) soundPlayer?.play(effect.sound)
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
            GameItemContainer(progress = { entryProgress.value }, index = 0) {
                GameHUD(
                    currentTeamId = state.currentTeam?.id,
                    teamName = state.currentTeam?.name ?: "",
                    teams = state.allTeams,
                    score = state.score,
                    timeRemaining = state.timeRemaining,
                    roundTime = state.maxTime,
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

            GameItemContainer(progress = { entryProgress.value }, index = 2) {
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
    // Read only inside graphicsLayer, i.e. while drawing: the entrance animation then
    // moves and fades the item every frame without recomposing the screen content.
    progress: () -> Float,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val startThreshold = index * 0.15f

    Box(
        modifier = modifier
            .graphicsLayer {
                val itemAlpha = ((progress() - startThreshold) / 0.4f).coerceIn(0f, 1f)
                alpha = itemAlpha
                translationY = (1f - itemAlpha) * 50f
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
