package com.aliasgame.app.presentation.setup


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aliasgame.app.R


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
    var roundTime by remember { mutableStateOf(60f) }
    var targetScore by remember { mutableStateOf(50f) }
    var teamNames by remember { mutableStateOf( funnyNames.shuffled().take(2) ) }
    val languages by viewModel.languages.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val packs by viewModel.packs.collectAsState()
    val selectedPack by viewModel.selectedPack.collectAsState()





    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.round_time, roundTime.toInt()))
        Slider(value = roundTime, onValueChange = { roundTime = it }, valueRange = 10f..120f)

        Text(stringResource(R.string.target_score, targetScore.toInt()))
        Slider(value = targetScore, onValueChange = { targetScore = it }, valueRange = 10f..100f)

        Text(text = stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            languages.forEach { lang ->
                FilterChip(
                    selected = selectedLanguage == lang,
                    onClick = { viewModel.onLanguageSelected(lang) },
                    label = { Text(lang) }
                )
            }
        }

        Text(text = stringResource(R.string.category), style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(packs){ pack ->
                FilterChip(
                    selected = selectedPack == pack,
                    onClick = { viewModel.onPackSelected(pack) },
                    label = { Text(pack) }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        teamNames.forEachIndexed { index, name ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { newName ->
                        val newList = teamNames.toMutableList()
                        newList[index] = newName
                        teamNames = newList
                    },
                    label = { Text(stringResource(R.string.team_name_hint, index + 1)) },
                    modifier = Modifier.weight(1f)
                )

                if (teamNames.size > 2) {
                    IconButton(onClick = {
                        teamNames = teamNames.toMutableList().apply { removeAt(index) }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove")
                    }
                }
            }
        }

        Button(
            onClick = {
                val nextName = funnyNames.filter { it !in teamNames }.randomOrNull()
                    ?: "Team ${teamNames.size + 1}"
                teamNames = teamNames + nextName
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.add_team))
        }

        Button(onClick = {
            viewModel.startGame(
                teamNames,
                roundTime.toLong(),
                targetScore.toInt()
            )
            onStartGame()
        },
            modifier = Modifier.fillMaxWidth(),
            enabled = teamNames.all { it.isNotBlank() }) {
            Text(stringResource(R.string.start_game))
        }
    }
}