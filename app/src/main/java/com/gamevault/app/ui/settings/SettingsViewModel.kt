package com.gamevault.app.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamevault.app.data.model.Game
import com.gamevault.app.data.model.PlaySession
import com.gamevault.app.data.repository.GameRepository
import com.gamevault.app.service.AdBlockManager
import com.gamevault.app.service.DataExportImportManager
import com.gamevault.app.service.DndManager
import com.gamevault.app.service.PlaytimeTracker
import com.gamevault.app.ui.home.HomeViewModel
import com.gamevault.app.ui.home.dataStore
import com.gamevault.app.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: GameRepository,
    private val playtimeTracker: PlaytimeTracker,
    private val dndManager: DndManager,
    private val exportImportManager: DataExportImportManager,
    private val adBlockManager: AdBlockManager
) : ViewModel() {

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode

    private val _dynamicColor = MutableStateFlow(false)
    val dynamicColor: StateFlow<Boolean> = _dynamicColor

    private val _dndOnLaunch = MutableStateFlow(false)
    val dndOnLaunch: StateFlow<Boolean> = _dndOnLaunch

    private val _adBlockOnLaunch = MutableStateFlow(false)
    val adBlockOnLaunch: StateFlow<Boolean> = _adBlockOnLaunch

    private val _dailyLimitMs = MutableStateFlow(0L)
    val dailyLimitMs: StateFlow<Long> = _dailyLimitMs

    val hasUsagePermission: Boolean get() = playtimeTracker.hasUsagePermission()
    val hasDndPermission: Boolean get() = dndManager.hasPermission()
    val hasOverlayPermission: Boolean get() = Settings.canDrawOverlays(context)
    val hasVpnPermission: Boolean get() = adBlockManager.hasPermission()

    val hiddenGames: StateFlow<List<Game>> = repository.getHiddenGames()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            context.dataStore.data
                .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
                .collect { prefs ->
                    _themeMode.value = try {
                        ThemeMode.valueOf(prefs[HomeViewModel.KEY_THEME_MODE] ?: "SYSTEM")
                    } catch (_: Exception) { ThemeMode.SYSTEM }
                    _dynamicColor.value = prefs[HomeViewModel.KEY_DYNAMIC_COLOR] ?: false
                    _dndOnLaunch.value = prefs[HomeViewModel.KEY_DND_ON_LAUNCH] ?: false
                    _adBlockOnLaunch.value = prefs[HomeViewModel.KEY_AD_BLOCK_ON_LAUNCH] ?: false
                    _dailyLimitMs.value = prefs[HomeViewModel.KEY_DAILY_LIMIT_MS] ?: 0L
                }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        viewModelScope.launch {
            context.dataStore.edit { it[HomeViewModel.KEY_THEME_MODE] = mode.name }
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        _dynamicColor.value = enabled
        viewModelScope.launch {
            context.dataStore.edit { it[HomeViewModel.KEY_DYNAMIC_COLOR] = enabled }
        }
    }

    fun setDndOnLaunch(enabled: Boolean) {
        _dndOnLaunch.value = enabled
        viewModelScope.launch {
            context.dataStore.edit { it[HomeViewModel.KEY_DND_ON_LAUNCH] = enabled }
        }
    }

    fun setAdBlockOnLaunch(enabled: Boolean) {
        _adBlockOnLaunch.value = enabled
        viewModelScope.launch {
            context.dataStore.edit { it[HomeViewModel.KEY_AD_BLOCK_ON_LAUNCH] = enabled }
        }
    }

    fun prepareVpn(): Intent? {
        return adBlockManager.prepareVpn()
    }

    fun setDailyLimit(hours: Int, minutes: Int) {
        val ms = (hours * 3600000L) + (minutes * 60000L)
        _dailyLimitMs.value = ms
        viewModelScope.launch {
            context.dataStore.edit { it[HomeViewModel.KEY_DAILY_LIMIT_MS] = ms }
        }
    }

    fun unhideGame(packageName: String) {
        viewModelScope.launch {
            repository.setHidden(packageName, false)
        }
    }

    fun openUsagePermissionSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openDndPermissionSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openOverlayPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun exportJson(outputStream: OutputStream) {
        viewModelScope.launch {
            exportImportManager.exportToJson(outputStream)
        }
    }

    fun importJson(inputStream: InputStream) {
        viewModelScope.launch {
            exportImportManager.importFromJson(inputStream)
        }
    }
}
