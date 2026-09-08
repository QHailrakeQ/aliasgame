package com.aliasgame.app.presentation.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aliasgame.app.R

/**
 * Entry point for game configuration.
 * Orchestrates session parameters and team management using SetupViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    viewModel: SetupViewModel = hiltViewModel(),
    onStartGame: () -> Unit
) {
    val funnyNames = listOf(
        stringResource(R.string.funny_name_1),
        stringResource(R.string.funny_name_2),
        stringResource(R.string.funny_name_3),
        stringResource(R.string.funny_name_4),
        stringResource(R.string.funny_name_5),
        stringResource(R.string.funny_name_6),
        stringResource(R.string.funny_name_7),
        stringResource(R.string.funny_name_8),
        stringResource(R.string.funny_name_9),
        stringResource(R.string.funny_name_10)
    )

    // Collecting state from ViewModel
    val roundTime by viewModel.roundTime.collectAsState()
    val targetScore by viewModel.targetScore.collectAsState()
    val teamNames by viewModel.teamNames.collectAsState()
    val languages by viewModel.languages.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val packs by viewModel.packs.collectAsState()
    val selectedPack by viewModel.selectedPack.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF6200EE), // Primary Brand Purple
                        Color(0xFF03DAC5)  // Secondary Accent Teal
                    )
                )
            )
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

            // Round duration and target score configuration
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White.copy(alpha = 0.9f))
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text(
                            stringResource(R.string.round_time, roundTime.toInt()),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Black
                        )
                        Slider(
                            value = roundTime,
                            onValueChange = { viewModel.onRoundTimeChanged(it) },
                            valueRange = 10f..120f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF6200EE),
                                activeTrackColor = Color(0xFF6200EE)
                            )
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
                            onValueChange = { viewModel.onTargetScoreChanged(it) },
                            valueRange = 10f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF6200EE),
                                activeTrackColor = Color(0xFF6200EE)
                            )
                        )
                    }
                }
            }

            // Localization and content pack selection
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White.copy(alpha = 0.9f))
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = stringResource(R.string.language), style = MaterialTheme.typography.titleMedium, color = Color.Black)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        languages.forEach { lang ->
                            FilterChip(
                                selected = selectedLanguage == lang,
                                onClick = { viewModel.onLanguageSelected(lang) },
                                label = { Text(lang) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Text(text = stringResource(R.string.category), style = MaterialTheme.typography.titleMedium, color = Color.Black)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(packs) { pack ->
                            FilterChip(
                                selected = selectedPack == pack,
                                onClick = { viewModel.onPackSelected(pack) },
                                label = { Text(pack) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            // Team management section
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White.copy(alpha = 0.9f))
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Teams", style = MaterialTheme.typography.titleMedium, color = Color.Black)
                    teamNames.forEachIndexed { index, name ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { newName ->
                                    val newList = teamNames.toMutableList()
                                    newList[index] = newName
                                    viewModel.onTeamNamesChanged(newList)
                                },
                                label = { Text(stringResource(R.string.team_name_hint, index + 1)) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6200EE),
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedLabelColor = Color(0xFF6200EE)
                                )
                            )
                            if (teamNames.size > 2) {
                                IconButton(onClick = {
                                    val newList = teamNames.toMutableList().apply { removeAt(index) }
                                    viewModel.onTeamNamesChanged(newList)
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red)
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            val nextName = funnyNames.filter { it !in teamNames }.randomOrNull()
                                ?: "Team ${teamNames.size + 1}"
                            viewModel.onTeamNamesChanged(teamNames + nextName)
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("+ " + stringResource(R.string.add_team), color = Color(0xFF6200EE))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary action to initialize session and transition
            Button(
                onClick = {
                    viewModel.startGame(onNavigate = onStartGame)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                enabled = teamNames.all { it.isNotBlank() }
            ) {
                Text(
                    stringResource(R.string.start_game),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF00332E)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
