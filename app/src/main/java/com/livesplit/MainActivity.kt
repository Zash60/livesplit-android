package com.livesplit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.livesplit.ui.game.GameScreen
import com.livesplit.ui.games.GamesScreen
import com.livesplit.ui.settings.SettingsScreen
import com.livesplit.ui.splits.SplitsScreen
import com.livesplit.ui.theme.LiveSplitTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LiveSplitTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var permissionsGranted by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Games, contentDescription = "Games") },
                    label = { Text("Games") },
                    selected = currentRoute == "games" || currentRoute?.startsWith("game/") == true,
                    onClick = {
                        navController.navigate("games") {
                            popUpTo("games") { inclusive = true }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = currentRoute == "settings",
                    onClick = {
                        navController.navigate("settings") {
                            popUpTo("games")
                        }
                    }
                )
            }
        }
) { innerPadding ->
        if (!permissionsGranted) {
            com.livesplit.ui.components.PermissionRequestScreen(
                onComplete = { permissionsGranted = true }
            )
        } else {
            NavHost(
                navController = navController,
                startDestination = "games",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("games") {
                    GamesScreen(
                        onGameClick = { gameId ->
                            navController.navigate("game/$gameId")
                        }
                    )
                }
                composable("game/{gameId}") { backStackEntry ->
                    val gameId = backStackEntry.arguments?.getString("gameId")?.toLongOrNull() ?: return@composable
                    GameScreen(
                        _gameId = gameId,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToSplits = { categoryId ->
                            navController.navigate("splits/$categoryId")
                        },
                        onNavigateToTimer = { categoryId ->
                            val intent = android.content.Intent(context, com.livesplit.ui.timer.TimerActivity::class.java).apply {
                                putExtra("CATEGORY_ID", categoryId)
                                putExtra("CATEGORY_NAME", "Category $categoryId")
                            }
                            context.startActivity(intent)
                        }
                    )
                }
                composable("splits/{categoryId}") { backStackEntry ->
                    val categoryId = backStackEntry.arguments?.getString("categoryId")?.toLongOrNull() ?: return@composable
                    SplitsScreen(
                        _categoryId = categoryId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
