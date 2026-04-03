package com.aliasgame.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.aliasgame.app.presentation.game.GameScreen
import dagger.hilt.android.AndroidEntryPoint
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aliasgame.app.presentation.setup.SetupScreen


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "setup"
                    ) {
                        composable("setup") {
                            SetupScreen(onStartGame = {
                                navController.navigate("game")
                            })
                        }
                        composable("game") {
                            GameScreen(onExit = {
                                navController.popBackStack("setup", inclusive = false)
                            })
                        }
                    }
                }
            }
        }

    }
}
