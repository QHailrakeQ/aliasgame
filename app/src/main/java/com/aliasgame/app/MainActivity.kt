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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.aliasgame.app.presentation.game.GameScreen
import dagger.hilt.android.AndroidEntryPoint
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.aliasgame.app.presentation.home.HomeScreen
import com.aliasgame.app.presentation.rules.RulesScreen
import com.aliasgame.app.presentation.settings.SettingsScreen
import com.aliasgame.app.presentation.statistics.StatisticsScreen
import com.aliasgame.app.presentation.setup.PackSelectionScreen
import com.aliasgame.app.presentation.setup.SetupScreen
import com.aliasgame.app.presentation.setup.SetupViewModel
import com.aliasgame.app.ui.theme.AliasGameTheme


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // installSplashScreen has to be called before super.onCreate
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        setContent {
            AliasGameTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        enterTransition = {
                            slideInHorizontally(initialOffsetX = { it }) + fadeIn()
                        },
                        exitTransition = {
                            slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                        },
                        popEnterTransition = {
                            slideInHorizontally(initialOffsetX = { -it }) + fadeIn()
                        },
                        popExitTransition = {
                            slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                        }
                    ) {
                        composable("home") {
                            HomeScreen(
                                onPlayClick = { navController.navigate("setup") },
                                onRulesClick = { navController.navigate("rules") },
                                onStatisticsClick = { navController.navigate("statistics") },
                                onSettingsClick = { navController.navigate("settings") }
                            )
                        }
                        composable("rules") {
                            RulesScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("statistics") {
                            StatisticsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("setup") {
                            SetupScreen(
                                onStartGame = { navController.navigate("game") },
                                onSelectPack = { navController.navigate("pack_selection") },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("game") {
                            GameScreen(onExit = {
                                navController.popBackStack("home", inclusive = false)
                            }, onNewGame = {
                                navController.navigate("setup") {
                                    popUpTo("home") { inclusive = false }
                                }
                            })
                        }
                        composable("pack_selection") { backStackEntry ->
                            // Obtain SetupViewModel scoped to the "setup" destination
                            val parentEntry = remember(backStackEntry) {
                                navController.getBackStackEntry("setup")
                            }
                            val sharedViewModel: SetupViewModel = hiltViewModel(parentEntry)
                            
                            PackSelectionScreen(
                                viewModel = sharedViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
