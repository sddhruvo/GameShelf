package com.gamevault.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.preferences.core.edit
import androidx.navigation.compose.rememberNavController
import androidx.work.*
import com.gamevault.app.BuildConfig
import com.gamevault.app.service.WeeklyReportWorker
import com.gamevault.app.ui.home.HomeViewModel
import com.gamevault.app.ui.home.dataStore
import com.gamevault.app.ui.theme.GameVaultTheme
import com.gamevault.app.ui.theme.ThemeMode
import com.google.gson.JsonParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
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

            // Update checker state
            var showUpdateDialog by remember { mutableStateOf(false) }
            var updateVersion by remember { mutableStateOf("") }
            var updateChangelog by remember { mutableStateOf("") }
            var updateUrl by remember { mutableStateOf("") }

            // Check for updates on launch
            LaunchedEffect(Unit) {
                val result = checkForUpdate()
                if (result != null) {
                    updateVersion = result.version
                    updateChangelog = result.changelog
                    updateUrl = result.url
                    showUpdateDialog = true
                }
            }

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

                // Update dialog
                if (showUpdateDialog) {
                    val context = LocalContext.current
                    AlertDialog(
                        onDismissRequest = { showUpdateDialog = false },
                        title = {
                            Text("Update Available", fontWeight = FontWeight.Bold)
                        },
                        text = {
                            Column(
                                modifier = Modifier.verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "New version: $updateVersion",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                if (updateChangelog.isNotBlank()) {
                                    Text(
                                        "What's new:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        updateChangelog,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showUpdateDialog = false
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl))
                                )
                            }) { Text("Update") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showUpdateDialog = false }) {
                                Text("Later")
                            }
                        }
                    )
                }
            }
        }
    }
}

private data class UpdateInfo(
    val version: String,
    val changelog: String,
    val url: String
)

private suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://api.github.com/repos/sddhruvo/GameVault/releases/latest")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        if (connection.responseCode != 200) return@withContext null

        val response = connection.inputStream.bufferedReader().readText()
        connection.disconnect()

        val json = JsonParser.parseString(response).asJsonObject
        val tagName = json.get("tag_name")?.asString ?: return@withContext null
        val body = json.get("body")?.asString ?: ""
        val htmlUrl = json.get("html_url")?.asString ?: return@withContext null

        val remoteVersion = tagName.removePrefix("v")
        val localVersion = BuildConfig.VERSION_NAME

        if (isNewerVersion(remoteVersion, localVersion)) {
            UpdateInfo(
                version = remoteVersion,
                changelog = body,
                url = htmlUrl
            )
        } else null
    } catch (_: Exception) {
        null
    }
}

private fun isNewerVersion(remote: String, local: String): Boolean {
    val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
    val localParts = local.split(".").mapNotNull { it.toIntOrNull() }
    for (i in 0 until maxOf(remoteParts.size, localParts.size)) {
        val r = remoteParts.getOrElse(i) { 0 }
        val l = localParts.getOrElse(i) { 0 }
        if (r > l) return true
        if (r < l) return false
    }
    return false
}
