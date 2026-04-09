package com.aliasgame.app.presentation.setup


import androidx.compose.foundation.layout.*
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



@Composable
fun SetupScreen(
    viewModel: SetupViewModel = hiltViewModel(),
    onStartGame: () -> Unit
) {
    var roundTime by remember { mutableStateOf(60f) }
    var targetScore by remember { mutableStateOf(50f) }
    var teamNames by remember { mutableStateOf(listOf("Team1", "Team2")) }




    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Round Time (seconds): ${roundTime.toInt()}")
        Slider(value = roundTime, onValueChange = { roundTime = it }, valueRange = 10f..120f)

        Text("Target Score: ${targetScore.toInt()}")
        Slider(value = targetScore, onValueChange = { targetScore = it }, valueRange = 10f..100f)

        teamNames.forEachIndexed { index, name ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { newName ->
                        val newList = teamNames.toMutableList()
                        newList[index] = newName
                        teamNames = newList
                    },
                    label = { Text("Team ${index + 1} Name") },
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
            teamNames = teamNames + "Team ${teamNames.size + 1}"
        }) {
            Text("Add Team")
        }

        Button(onClick = {
            viewModel.startGame(
                teamNames,
                roundTime.toLong(),
                targetScore.toInt()
            )
            onStartGame()
        }) {
            Text("Start Game")
        }
    }
}