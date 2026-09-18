package com.aliasgame.app.presentation.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliasgame.app.R

/**
 * Main Setup Screen.
 * Orchestrates localized cards and handles navigation/error side effects.
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

    // Handle One-time Side Effects (Navigation, Errors)
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is SetupUiEffect.NavigateToGame -> onStartGame()
                is SetupUiEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
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
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF6200EE), Color(0xFF03DAC5))
                    )
                )
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.setup_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                GameRulesCard(
                    roundTime = state.roundTime,
                    targetScore = state.targetScore,
                    onRoundTimeChange = viewModel::onRoundTimeChanged,
                    onTargetScoreChange = viewModel::onTargetScoreChanged
                )

                LocalizationCard(
                    languages = state.languages,
                    selectedLanguage = state.selectedLanguage,
                    selectedPack = state.selectedPack,
                    onLanguageSelect = viewModel::onLanguageSelected,
                    onPackClick = onSelectPack
                )

                TeamManagementCard(
                    teamNames = state.teamNames,
                    onTeamNamesChange = viewModel::onTeamNamesChanged
                )

                PreferencesCard(
                    isSoundEnabled = state.isSoundEnabled,
                    isVibrationEnabled = state.isVibrationEnabled,
                    onSoundToggle = viewModel::onSoundToggled,
                    onVibrationToggle = viewModel::onVibrationToggled
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = viewModel::startGame,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                    enabled = !state.isLoading && state.teamNames.all { it.isNotBlank() }
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(color = Color(0xFF00332E), modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            stringResource(R.string.start_game),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF00332E)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
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
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column {
                Text(
                    stringResource(R.string.round_time, roundTime.toInt()),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black
                )
                Slider(
                    value = roundTime,
                    onValueChange = onRoundTimeChange,
                    valueRange = 10f..120f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF6200EE), activeTrackColor = Color(0xFF6200EE))
                )
            }

            Column {
                Text(
                    stringResource(R.string.target_score, targetScore.toInt()),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black
                )
                Slider(
                    value = targetScore,
                    onValueChange = onTargetScoreChange,
                    valueRange = 10f..100f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF6200EE), activeTrackColor = Color(0xFF6200EE))
                )
            }
        }
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
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium, color = Color.Black)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                languages.forEach { lang ->
                    FilterChip(
                        selected = selectedLanguage == lang,
                        onClick = { onLanguageSelect(lang) },
                        label = { Text(lang) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Text(stringResource(R.string.category), style = MaterialTheme.typography.titleMedium, color = Color.Black)
            OutlinedCard(
                onClick = onPackClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = Color.White.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
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
                    Text(localizedPack, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.Black)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF6200EE))
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
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.teams_header), style = MaterialTheme.typography.titleMedium, color = Color.Black)
            teamNames.forEachIndexed { index, name ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { newName ->
                            val newList = teamNames.toMutableList().apply { set(index, newName) }
                            onTeamNamesChange(newList)
                        },
                        label = { Text(stringResource(R.string.team_name_hint, index + 1)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (teamNames.size > 2) {
                        IconButton(onClick = {
                            val newList = teamNames.toMutableList().apply { removeAt(index) }
                            onTeamNamesChange(newList)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red)
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
                Text("+ " + stringResource(R.string.add_team), color = Color(0xFF6200EE))
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
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.preferences_header), style = MaterialTheme.typography.titleMedium, color = Color.Black)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.sound_effects), color = Color.Black)
                Switch(isSoundEnabled, onSoundToggle)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.haptic_feedback), color = Color.Black)
                Switch(isVibrationEnabled, onVibrationToggle)
            }
        }
    }
}

@Composable
private fun SetupCard(content: @Composable () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White.copy(alpha = 0.9f))
    ) {
        Box(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}
