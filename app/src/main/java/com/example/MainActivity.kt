package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ToolsHubScreen
import com.example.ui.theme.GptBackground
import com.example.ui.theme.GptWhite
import com.example.ui.theme.SolyNextTheme
import com.example.ui.viewmodel.AssistantViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SolyNextTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: AssistantViewModel = viewModel()
                    val isAccountSetup by viewModel.preferencesRepository.isAccountSetupFlow.collectAsState(initial = null)

                    if (isAccountSetup == null) {
                        // Splash loading while preferences are read from DataStore
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(GptBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = GptWhite,
                                strokeWidth = 2.dp
                            )
                        }
                    } else {
                        val navController = rememberNavController()
                        val startDestination = if (isAccountSetup == true) "assistant" else "onboarding"

                        NavHost(
                            navController = navController,
                            startDestination = startDestination
                        ) {
                            composable("onboarding") {
                                OnboardingScreen(
                                    viewModel = viewModel,
                                    onAccountCreated = {
                                        navController.navigate("assistant") {
                                            popUpTo("onboarding") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("assistant") {
                                AssistantScreen(
                                    viewModel = viewModel,
                                    onNavigateToHistory = { navController.navigate("history") },
                                    onNavigateToMemory = { navController.navigate("memory") },
                                    onNavigateToReminders = { navController.navigate("reminders") },
                                    onNavigateToTools = { navController.navigate("tools") },
                                    onNavigateToSettings = { navController.navigate("settings") }
                                )
                            }
                            composable("history") {
                                HistoryScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable("memory") {
                                MemoryScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable("reminders") {
                                RemindersScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable("tools") {
                                ToolsHubScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable("settings") {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() },
                                    onResetAccount = {
                                        navController.navigate("onboarding") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
