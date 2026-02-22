package com.gamevault.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gamevault.app.BuildConfig
import com.gamevault.app.ui.home.DetectionMode
import com.gamevault.app.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
    val dndOnLaunch by viewModel.dndOnLaunch.collectAsStateWithLifecycle()
    val adBlockOnLaunch by viewModel.adBlockOnLaunch.collectAsStateWithLifecycle()
    val dailyLimitMs by viewModel.dailyLimitMs.collectAsStateWithLifecycle()
    val hiddenGames by viewModel.hiddenGames.collectAsStateWithLifecycle()
    val detectionMode by viewModel.detectionMode.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var showThemeDialog by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var showHiddenDialog by remember { mutableStateOf(false) }
    var showDetectionModeDialog by remember { mutableStateOf(false) }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { /* permission re-evaluated on next compose */ }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.let { stream ->
                viewModel.exportJson(stream)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.let { stream ->
                viewModel.importJson(stream)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings", fontWeight = FontWeight.Bold) },
            windowInsets = WindowInsets(0)
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Appearance
            item {
                Text(
                    "Appearance",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = "Theme",
                    subtitle = themeMode.name.lowercase().replaceFirstChar { it.uppercase() },
                    onClick = { showThemeDialog = true }
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    SettingsToggle(
                        icon = Icons.Default.ColorLens,
                        title = "Dynamic Color",
                        subtitle = "Match your wallpaper colors",
                        checked = dynamicColor,
                        onCheckedChange = { viewModel.setDynamicColor(it) }
                    )
                }
            }

            // Game Detection
            item {
                Text(
                    "Game Detection",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.ManageSearch,
                    title = "Detection Mode",
                    subtitle = if (detectionMode == DetectionMode.AUTO) "Automatic" else "Manual",
                    onClick = { showDetectionModeDialog = true }
                )
            }

            // Permissions
            item {
                Text(
                    "Permissions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.QueryStats,
                    title = "Usage Access",
                    subtitle = if (viewModel.hasUsagePermission) "Granted" else "Required for playtime tracking",
                    onClick = { viewModel.openUsagePermissionSettings() },
                    statusColor = if (viewModel.hasUsagePermission) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.DoNotDisturb,
                    title = "DND Access",
                    subtitle = if (viewModel.hasDndPermission) "Granted" else "Required for DND on launch",
                    onClick = { viewModel.openDndPermissionSettings() },
                    statusColor = if (viewModel.hasDndPermission) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.PictureInPicture,
                    title = "Overlay Permission",
                    subtitle = if (viewModel.hasOverlayPermission) "Granted" else "Required for floating timer",
                    onClick = { viewModel.openOverlayPermissionSettings() },
                    statusColor = if (viewModel.hasOverlayPermission) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Shield,
                    title = "VPN Permission",
                    subtitle = if (viewModel.hasVpnPermission) "Granted" else "Required for ad blocking",
                    onClick = {
                        val intent = viewModel.prepareVpn()
                        if (intent != null) vpnPermissionLauncher.launch(intent)
                    },
                    statusColor = if (viewModel.hasVpnPermission) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Gaming Tools
            item {
                Text(
                    "Gaming Tools",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                SettingsToggle(
                    icon = Icons.Default.DoNotDisturb,
                    title = "DND on Launch",
                    subtitle = "Enable Do Not Disturb when launching a game",
                    checked = dndOnLaunch,
                    onCheckedChange = { viewModel.setDndOnLaunch(it) }
                )
            }

            item {
                SettingsToggle(
                    icon = Icons.Default.Shield,
                    title = "Block Ads on Launch",
                    subtitle = "Enable DNS ad blocker when launching a game",
                    checked = adBlockOnLaunch,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            val intent = viewModel.prepareVpn()
                            if (intent != null) {
                                vpnPermissionLauncher.launch(intent)
                                return@SettingsToggle
                            }
                        }
                        viewModel.setAdBlockOnLaunch(enabled)
                    }
                )
            }

            item {
                val limitHours = dailyLimitMs / 3600000
                val limitMinutes = (dailyLimitMs % 3600000) / 60000
                SettingsItem(
                    icon = Icons.Default.Timer,
                    title = "Daily Screen Time Goal",
                    subtitle = if (dailyLimitMs > 0) "${limitHours}h ${limitMinutes}m" else "Not set",
                    onClick = { showTimerDialog = true }
                )
            }

            // Data
            item {
                Text(
                    "Data",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Upload,
                    title = "Export Data",
                    subtitle = "Export all data as JSON",
                    onClick = { exportLauncher.launch("gamevault_export.json") }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Download,
                    title = "Import Data",
                    subtitle = "Restore from a previous export",
                    onClick = { importLauncher.launch(arrayOf("application/json")) }
                )
            }

            // Hidden Games
            item {
                SettingsItem(
                    icon = Icons.Default.VisibilityOff,
                    title = "Hidden Games",
                    subtitle = "${hiddenGames.size} games hidden",
                    onClick = { showHiddenDialog = true }
                )
            }

            // About
            item {
                Text(
                    "About",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "GameVault",
                    subtitle = "Version ${BuildConfig.VERSION_NAME}",
                    onClick = {}
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.BugReport,
                    title = "Report a Bug",
                    subtitle = "Send feedback via email",
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_EMAIL, arrayOf("dhruvo012@gmail.com"))
                            putExtra(
                                Intent.EXTRA_SUBJECT,
                                "GameVault Bug Report - v${BuildConfig.VERSION_NAME}"
                            )
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Please describe the bug:\n\n\n--- Device Info ---\n" +
                                    "App Version: ${BuildConfig.VERSION_NAME}\n" +
                                    "Device: ${Build.MANUFACTURER} ${Build.MODEL}\n" +
                                    "Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n"
                            )
                        }
                        context.startActivity(intent)
                    }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Favorite,
                    title = "Support Development",
                    subtitle = "Buy me a coffee via PayPal",
                    onClick = {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://paypal.me/sddhruvo")
                        )
                        context.startActivity(intent)
                    },
                    statusColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    // Theme Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Theme") },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = mode == themeMode,
                                onClick = {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Timer Dialog
    if (showTimerDialog) {
        var hours by remember { mutableIntStateOf((dailyLimitMs / 3600000).toInt()) }
        var minutes by remember { mutableIntStateOf(((dailyLimitMs % 3600000) / 60000).toInt()) }
        AlertDialog(
            onDismissRequest = { showTimerDialog = false },
            title = { Text("Daily Screen Time Goal") },
            text = {
                Column {
                    Text("Set your daily gaming time limit:")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Hours")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (hours > 0) hours-- }) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease")
                                }
                                Text("$hours", style = MaterialTheme.typography.titleLarge)
                                IconButton(onClick = { if (hours < 24) hours++ }) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase")
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Minutes")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (minutes > 0) minutes -= 15 }) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease")
                                }
                                Text("$minutes", style = MaterialTheme.typography.titleLarge)
                                IconButton(onClick = { if (minutes < 45) minutes += 15 }) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setDailyLimit(hours, minutes)
                    showTimerDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        viewModel.setDailyLimit(0, 0)
                        showTimerDialog = false
                    }) { Text("Remove Limit") }
                    TextButton(onClick = { showTimerDialog = false }) { Text("Cancel") }
                }
            }
        )
    }

    // Detection Mode Dialog
    if (showDetectionModeDialog) {
        AlertDialog(
            onDismissRequest = { showDetectionModeDialog = false },
            title = { Text("Game Detection Mode") },
            text = {
                Column {
                    DetectionMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = mode == detectionMode,
                                onClick = {
                                    viewModel.setDetectionMode(mode)
                                    showDetectionModeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    if (mode == DetectionMode.AUTO) "Automatic" else "Manual",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    if (mode == DetectionMode.AUTO)
                                        "Automatically find all your games"
                                    else
                                        "Manually pick which apps are games",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetectionModeDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Hidden Games Dialog
    if (showHiddenDialog) {
        AlertDialog(
            onDismissRequest = { showHiddenDialog = false },
            title = { Text("Hidden Games") },
            text = {
                if (hiddenGames.isEmpty()) {
                    Text("No hidden games")
                } else {
                    LazyColumn {
                        items(hiddenGames) { game ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(game.name, modifier = Modifier.weight(1f))
                                TextButton(onClick = { viewModel.unhideGame(game.packageName) }) {
                                    Text("Show")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHiddenDialog = false }) { Text("Done") }
            }
        )
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    statusColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = statusColor)
            }
        }
    }
}

@Composable
private fun SettingsToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
