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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    viewModel: SetupViewModel = hiltViewModel(),
    onStartGame: () -> Unit
) {
    var roundTime by remember { mutableStateOf(60f) }
    var targetScore by remember { mutableStateOf(50f) }
    var teamNames by remember { mutableStateOf(listOf("Team1", "Team2")) }
    val languages by viewModel.languages.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val packs by viewModel.packs.collectAsState()
    val selectedPack by viewModel.selectedPack.collectAsState()

    val labels = remember(selectedLanguage) {
        when (selectedLanguage) {
            "UK" -> mapOf(
                "roundTime" to "Час раунду (сек):",
                "targetScore" to "Очки для перемоги:",
                "language" to "Мова:",
                "category" to "Категорія:",
                "addTeam" to "Додати команду",
                "startGame" to "Почати гру",
                "teamName" to "Назва команди"
            )
            "DE" -> mapOf(
                "roundTime" to "Rundenzeit (sek):",
                "targetScore" to "Zielpunktzahl:",
                "language" to "Sprache:",
                "category" to "Kategorie:",
                "addTeam" to "Team hinzufügen",
                "startGame" to "Spiel starten",
                "teamName" to "Teamname"
            )
            "RU" -> mapOf(
                "roundTime" to "Время раунда (сек):",
                "targetScore" to "Очки для победы:",
                "language" to "Язык:",
                "category" to "Категория:",
                "addTeam" to "Добавить команду",
                "startGame" to "Начать игру",
                "teamName" to "Название команды"
            )
            else -> mapOf( // За замовчуванням англійська (EN)
                "roundTime" to "Round Time (sec):",
                "targetScore" to "Target Score:",
                "language" to "Language:",
                "category" to "Category:",
                "addTeam" to "Add Team",
                "startGame" to "Start Game",
                "teamName" to "Team Name"
            )
        }
    }


    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("${labels["roundTime"]} ${roundTime.toInt()}")
        Slider(value = roundTime, onValueChange = { roundTime = it }, valueRange = 10f..120f)

        Text("${labels["targetScore"]} ${targetScore.toInt()}")
        Slider(value = targetScore, onValueChange = { targetScore = it }, valueRange = 10f..100f)

        Text(text = labels["language"] ?: "", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            languages.forEach { lang ->
                FilterChip(
                    selected = selectedLanguage == lang,
                    onClick = { viewModel.onLanguageSelected(lang) },
                    label = { Text(lang) }
                )
            }
        }

        Text(text = labels["category"] ?: "", style = MaterialTheme.typography.titleMedium)
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
                    label = { Text("${labels["teamName"]} ${index + 1}") },
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

        Button(onClick =  {
            teamNames = teamNames + "Team ${teamNames.size + 1}"},
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(labels["addTeam"] ?: "")
        }

        Button(onClick = {
            viewModel.startGame(
                teamNames,
                roundTime.toLong(),
                targetScore.toInt()
            )
            onStartGame()
        },
            modifier = Modifier.fillMaxWidth()) {
            Text(labels["startGame"] ?: "")
        }
    }
}