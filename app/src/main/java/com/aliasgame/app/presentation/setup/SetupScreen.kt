package com.aliasgame.app.presentation.setup

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliasgame.app.R
import com.aliasgame.app.ui.components.GlassCard
import com.aliasgame.app.ui.components.PartyBackground
import com.aliasgame.app.ui.components.PartyScreenHeader
import com.aliasgame.app.ui.theme.*

/**
 * Modernized Setup Screen focusing on game rules and teams.
 * Rules are now managed via bounded steppers for better precision.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    viewModel: SetupViewModel = hiltViewModel(),
    onStartGame: () -> Unit,
    onSelectPack: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val noWordsMessage = stringResource(R.string.no_words_for_category)

    val entryProgress = remember {
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

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is SetupUiEffect.NavigateToGame -> onStartGame()
                is SetupUiEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                SetupUiEffect.ShowNoWordsAvailable -> snackbarHostState.showSnackbar(noWordsMessage)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        PartyBackground(modifier = Modifier.padding(padding)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = GameSpacing.screenHorizontal)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(GameSpacing.section)
        ) {
            PartyScreenHeader(
                title = stringResource(R.string.setup_title),
                onBack = onBack,
                modifier = Modifier.padding(top = GameSpacing.screenVertical)
            )

                // Game Rules (Time and Target Score)
                SetupItemContainer(progress = entryProgress.value, index = 0) {
                    GameRulesCard(
                        roundTime = state.roundTime,
                        targetScore = state.targetScore,
                        onRoundTimeChange = { viewModel.onRoundTimeChanged(it) },
                        onTargetScoreChange = { viewModel.onTargetScoreChanged(it) }
                    )
                }

                // Word Pack Selection
                SetupItemContainer(progress = entryProgress.value, index = 1) {
                    PackSelectionCard(
                        selectedPack = state.selectedPack,
                        onPackClick = onSelectPack
                    )
                }

                // Team Management
                SetupItemContainer(progress = entryProgress.value, index = 2) {
                    TeamManagementCard(
                        teamNames = state.teamNames,
                        onTeamNamesChange = viewModel::onTeamNamesChanged
                    )
                }

                // Start Game button
                SetupItemContainer(progress = entryProgress.value, index = 3) {
                    Button(
                        onClick = viewModel::startGame,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GameAccent,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                        enabled = !state.isLoading && state.teamNames.all { it.isNotBlank() }
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    stringResource(R.string.start_game).uppercase(),
                                    style = Typography.headlineMedium.copy(fontSize = 22.sp),
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(Modifier.width(12.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(24.dp))
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(GameSpacing.section))
        }
        }
    }
}

@Composable
private fun SetupItemContainer(
    progress: Float,
    index: Int,
    content: @Composable () -> Unit
) {
    val startThreshold = index * 0.12f
    val itemAlpha = ((progress - startThreshold) / 0.35f).coerceIn(0f, 1f)
    val itemTranslation = (1f - itemAlpha) * 40f

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

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Icon(icon, null, tint = GameSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            style = Typography.labelLarge,
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun GameRulesCard(
    roundTime: Long,
    targetScore: Int,
    onRoundTimeChange: (Long) -> Unit,
    onTargetScoreChange: (Int) -> Unit
) {
    SetupCard {
        Column {
            SectionHeader(stringResource(R.string.preferences_header), Icons.Default.Settings)
            
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                ValueStepper(
                    title = stringResource(R.string.round_time_label),
                    valueText = stringResource(R.string.round_time_value, roundTime),
                    value = roundTime,
                    minValue = SetupViewModel.MIN_ROUND_TIME,
                    maxValue = SetupViewModel.MAX_ROUND_TIME,
                    step = SetupViewModel.STEP_ROUND_TIME,
                    decrementDescription = stringResource(R.string.content_desc_decrement),
                    incrementDescription = stringResource(R.string.content_desc_increment),
                    onValueChange = onRoundTimeChange
                )
                
                ValueStepper(
                    title = stringResource(R.string.target_score_label),
                    valueText = stringResource(R.string.target_score_value, targetScore),
                    value = targetScore.toLong(),
                    minValue = SetupViewModel.MIN_TARGET_SCORE.toLong(),
                    maxValue = SetupViewModel.MAX_TARGET_SCORE.toLong(),
                    step = SetupViewModel.STEP_TARGET_SCORE.toLong(),
                    decrementDescription = stringResource(R.string.content_desc_decrement),
                    incrementDescription = stringResource(R.string.content_desc_increment),
                    onValueChange = { onTargetScoreChange(it.toInt()) }
                )
            }
        }
    }
}

@Composable
private fun ValueStepper(
    title: String,
    valueText: String,
    value: Long,
    minValue: Long,
    maxValue: Long,
    step: Long,
    decrementDescription: String,
    incrementDescription: String,
    onValueChange: (Long) -> Unit
) {
    Column {
        Text(
            text = title,
            style = Typography.titleMedium,
            color = Color.White.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StepperButton(
                icon = Icons.Default.Remove,
                contentDescription = decrementDescription,
                enabled = value > minValue,
                onClick = { onValueChange(value - step) }
            )
            
            Text(
                text = valueText,
                modifier = Modifier.weight(1f),
                style = Typography.displaySmall.copy(fontSize = 32.sp),
                color = Color.White,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            
            StepperButton(
                icon = Icons.Default.Add,
                contentDescription = incrementDescription,
                enabled = value < maxValue,
                onClick = { onValueChange(value + step) }
            )
        }
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val alpha by animateFloatAsState(if (enabled) 1f else 0.3f, label = "StepperAlpha")
    
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(56.dp),
        shape = RoundedCornerShape(16.dp),
        color = GlassWhite,
        border = BorderStroke(1.dp, if (enabled) GameSecondary.copy(alpha = 0.5f) else GlassWhiteBorder)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) GameSecondary else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.graphicsLayer { this.alpha = alpha }
            )
        }
    }
}

@Composable
private fun PackSelectionCard(
    selectedPack: String,
    onPackClick: () -> Unit
) {
    SetupCard {
        Column {
            SectionHeader(stringResource(R.string.category), Icons.Default.Translate)
            
            Surface(
                onClick = onPackClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = GlassWhite,
                border = BorderStroke(1.dp, GlassWhiteBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val localizedPack = when (selectedPack.lowercase()) {
                        "easy" -> stringResource(R.string.difficulty_easy)
                        "medium" -> stringResource(R.string.difficulty_medium)
                        "hard" -> stringResource(R.string.difficulty_hard)
                        "insane" -> stringResource(R.string.difficulty_insane)
                        else -> selectedPack
                    }
                    
                    Text(localizedPack, style = Typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = GameSecondary)
                }
            }
        }
    }
}

@Composable
private fun TeamManagementCard(
    teamNames: List<String>,
    onTeamNamesChange: (List<String>) -> Unit
) {
    val funnyNames = listOf(
        stringResource(R.string.funny_name_1), stringResource(R.string.funny_name_2),
        stringResource(R.string.funny_name_3), stringResource(R.string.funny_name_4),
        stringResource(R.string.funny_name_5), stringResource(R.string.funny_name_6),
        stringResource(R.string.funny_name_7), stringResource(R.string.funny_name_8),
        stringResource(R.string.funny_name_9), stringResource(R.string.funny_name_10)
    )

    SetupCard {
        Column {
            SectionHeader(stringResource(R.string.teams_header), Icons.Default.Group)
            
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                teamNames.forEachIndexed { index, name ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextField(
                            value = name,
                            onValueChange = { newName ->
                                val newList = teamNames.toMutableList().apply { set(index, newName) }
                                onTeamNamesChange(newList)
                            },
                            placeholder = { Text(stringResource(R.string.team_name_hint, index + 1), color = Color.White.copy(alpha = 0.4f)) },
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = GlassWhiteBorder,
                                unfocusedContainerColor = GlassWhite,
                                focusedIndicatorColor = GameSecondary,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )
                        if (teamNames.size > 2) {
                            IconButton(
                                onClick = {
                                    val newList = teamNames.toMutableList().apply { removeAt(index) }
                                    onTeamNamesChange(newList)
                                },
                                modifier = Modifier.background(Color.Red.copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5252))
                            }
                        }
                    }
                }

                TextButton(
                    onClick = {
                        val nextName = funnyNames.filter { it !in teamNames }.randomOrNull() ?: "Team ${teamNames.size + 1}"
                        onTeamNamesChange(teamNames + nextName)
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("+ " + stringResource(R.string.add_team).uppercase(), color = GameSecondary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SetupCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    GlassCard(modifier = modifier) {
        Box(modifier = Modifier.padding(GameSpacing.cardPadding)) {
            content()
        }
    }
}
