package com.aliasgame.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.aliasgame.app.presentation.game.GameScreen
import dagger.hilt.android.AndroidEntryPoint
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aliasgame.app.presentation.setup.PackSelectionScreen
import com.aliasgame.app.presentation.setup.SetupScreen


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
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
                        startDestination = "setup",
                        enterTransition = {
                            slideInHorizontally (initialOffsetX = { it }) + fadeIn()
                        },
                        exitTransition = {
                            slideOutHorizontally (targetOffsetX = { -it }) + fadeOut()
                        },
                        popEnterTransition = {
                            slideInHorizontally (initialOffsetX = { -it }) + fadeIn()
                        },
                        popExitTransition = {
                            slideOutHorizontally (targetOffsetX = { it }) + fadeOut()
                        }

                    ) {
                        composable("setup") {
                            SetupScreen(onStartGame = {
                                navController.navigate("game")
                            },
                                        onSelectPack = {
                                            navController.navigate("pack_selection")
                                }
                            )
                        }
                        composable("game") {
                            GameScreen(onExit = {
                                navController.popBackStack("setup", inclusive = false)
                            })
                        }
                        composable("pack_selection") {
                            PackSelectionScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }

    }
}
