package com.gamevault.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.preferences.core.edit
import androidx.navigation.compose.rememberNavController
import androidx.work.*
import com.gamevault.app.service.WeeklyReportWorker
import com.gamevault.app.ui.home.HomeViewModel
import com.gamevault.app.ui.home.dataStore
import com.gamevault.app.ui.theme.GameVaultTheme
import com.gamevault.app.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule weekly report worker
        val workRequest = PeriodicWorkRequestBuilder<WeeklyReportWorker>(
            7, TimeUnit.DAYS
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "weekly_report",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        setContent {
            val scope = rememberCoroutineScope()

            // Read theme preferences
            val themeMode by dataStore.data
                .map {
                    try {
                        ThemeMode.valueOf(it[HomeViewModel.KEY_THEME_MODE] ?: "SYSTEM")
                    } catch (_: Exception) { ThemeMode.SYSTEM }
                }
                .collectAsState(initial = ThemeMode.SYSTEM)

            val dynamicColor by dataStore.data
                .map { it[HomeViewModel.KEY_DYNAMIC_COLOR] ?: false }
                .collectAsState(initial = false)

            val onboardingComplete by dataStore.data
                .map { it[HomeViewModel.KEY_ONBOARDING_COMPLETE] ?: false }
                .collectAsState(initial = true) // default true to avoid flash

            val startDestination = if (onboardingComplete) Routes.HOME else Routes.ONBOARDING

            GameVaultTheme(
                themeMode = themeMode,
                dynamicColor = dynamicColor
            ) {
                val navController = rememberNavController()

                // Handle onboarding completion
                LaunchedEffect(Unit) {
                    navController.addOnDestinationChangedListener { _, destination, _ ->
                        if (destination.route == Routes.HOME && !onboardingComplete) {
                            scope.launch {
                                dataStore.edit { it[HomeViewModel.KEY_ONBOARDING_COMPLETE] = true }
                            }
                        }
                    }
                }

                GameVaultNavHost(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }
}
