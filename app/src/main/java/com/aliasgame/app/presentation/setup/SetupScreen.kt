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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliasgame.app.R
import com.aliasgame.app.ui.theme.*

/**
 * Modernized Setup Screen with optimized performance and coordinated transitions.
 * Entrance animations are persisted via SetupViewModel to ensure smooth UX during
 * navigation and configuration changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    viewModel: SetupViewModel = hiltViewModel(),
    onStartGame: () -> Unit,
    onSelectPack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Initialize animation progress. If already played, we start at 1f (fully visible).
    val entryProgress = remember {
        Animatable(if (state.isEntranceAnimationPlayed) 1f else 0f) 
    }

    // Trigger entrance animation only if it hasn't been played yet.
    LaunchedEffect(state.isEntranceAnimationPlayed) {
        if (!state.isEntranceAnimationPlayed) {
            val result = entryProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800, easing = EaseOutQuart)
            )
            // Persist the completion state only if the animation finished naturally.
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
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(BgStart, BgEnd)))
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = stringResource(R.string.setup_title).uppercase(),
                    style = Typography.displaySmall,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            // Coordinate title with the same entry progress
                            alpha = entryProgress.value
                            translationY = (1f - entryProgress.value) * -30f
                        }
                )

                // Coordinated transitions for all items
                SetupItemContainer(progress = entryProgress.value, index = 0) {
                    GameRulesCard(
                        roundTime = state.roundTime,
                        targetScore = state.targetScore,
                        onRoundTimeChange = viewModel::onRoundTimeChanged,
                        onTargetScoreChange = viewModel::onTargetScoreChanged
                    )
                }

                SetupItemContainer(progress = entryProgress.value, index = 1) {
                    LocalizationCard(
                        languages = state.languages,
                        selectedLanguage = state.selectedLanguage,
                        selectedPack = state.selectedPack,
                        onLanguageSelect = viewModel::onLanguageSelected,
                        onPackClick = onSelectPack
                    )
                }

                SetupItemContainer(progress = entryProgress.value, index = 2) {
                    TeamManagementCard(
                        teamNames = state.teamNames,
                        onTeamNamesChange = viewModel::onTeamNamesChanged
                    )
                }

                SetupItemContainer(progress = entryProgress.value, index = 3) {
                    PreferencesCard(
                        isSoundEnabled = state.isSoundEnabled,
                        isVibrationEnabled = state.isVibrationEnabled,
                        onSoundToggle = viewModel::onSoundToggled,
                        onVibrationToggle = viewModel::onVibrationToggled
                    )
                }

                // Start Game button follows the exact same coordinated entry system
                SetupItemContainer(progress = entryProgress.value, index = 4) {
                    Button(
                        onClick = viewModel::startGame,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(24.dp),
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
                
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

/**
 * Coordinates item visibility and movement during the entrance sequence.
 * Uses index-based staggering synchronized with global entrance progress.
 */
@Composable
private fun SetupItemContainer(
    progress: Float,
    index: Int,
    content: @Composable () -> Unit
) {
    // Smoother stagger logic with localized thresholds
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
    roundTime: Float,
    targetScore: Float,
    onRoundTimeChange: (Float) -> Unit,
    onTargetScoreChange: (Float) -> Unit
) {
    SetupCard {
        Column {
            SectionHeader(stringResource(R.string.preferences_header), Icons.Default.Settings)
            
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                RuleSlider(
                    label = stringResource(R.string.round_time, roundTime.toInt()),
                    value = roundTime,
                    range = 10f..120f,
                    onValueChange = onRoundTimeChange
                )
                RuleSlider(
                    label = stringResource(R.string.target_score, targetScore.toInt()),
                    value = targetScore,
                    range = 10f..100f,
                    onValueChange = onTargetScoreChange
                )
            }
        }
    }
}

@Composable
private fun RuleSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column {
        Text(label, style = Typography.titleMedium, color = Color.White)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = GameSecondary,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalizationCard(
    languages: List<String>,
    selectedLanguage: String,
    selectedPack: String,
    onLanguageSelect: (String) -> Unit,
    onPackClick: () -> Unit
) {
    SetupCard {
        Column {
            SectionHeader(stringResource(R.string.language), Icons.Default.Translate)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                languages.forEach { lang ->
                    val isSelected = selectedLanguage == lang
                    val backgroundColor by animateColorAsState(
                        targetValue = if (isSelected) GameSecondary else GlassWhite,
                        label = "LangBg"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) BgStart else Color.White,
                        label = "LangText"
                    )
                    
                    Surface(
                        onClick = { onLanguageSelect(lang) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = backgroundColor,
                        border = if (isSelected) null else BorderStroke(1.dp, GlassWhiteBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                lang,
                                style = Typography.titleSmall,
                                color = textColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(stringResource(R.string.category), style = Typography.titleMedium, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            
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
                    
                    Crossfade(targetState = localizedPack, label = "PackCrossfade") { text ->
                        Text(text, style = Typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    
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
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
private fun PreferencesCard(
    isSoundEnabled: Boolean,
    isVibrationEnabled: Boolean,
    onSoundToggle: (Boolean) -> Unit,
    onVibrationToggle: (Boolean) -> Unit
) {
    SetupCard {
        Column {
            SectionHeader(stringResource(R.string.preferences_header), Icons.Default.Settings)
            
            PreferenceRow(stringResource(R.string.sound_effects), isSoundEnabled, onSoundToggle)
            PreferenceRow(stringResource(R.string.haptic_feedback), isVibrationEnabled, onVibrationToggle)
        }
    }
}

@Composable
private fun PreferenceRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = Typography.bodyLarge, color = Color.White)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GameSecondary,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = GlassWhite
            )
        )
    }
}

@Composable
fun SetupCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = GlassWhite,
        border = BorderStroke(1.dp, GlassWhiteBorder)
    ) {
        Box(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}
