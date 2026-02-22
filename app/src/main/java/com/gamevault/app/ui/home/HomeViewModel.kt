package com.gamevault.app.ui.home

import android.content.Context
import android.content.Intent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamevault.app.data.model.Game
import com.gamevault.app.data.repository.GameRepository
import com.gamevault.app.service.AdBlockManager
import com.gamevault.app.service.DndManager
import com.gamevault.app.service.GameDetector
import com.gamevault.app.service.OverlayService
import com.gamevault.app.service.PlaytimeTracker
import com.gamevault.app.service.TimerService
import com.gamevault.app.ui.components.SortOption
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ViewMode { GRID, LIST, ICON }

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: GameRepository,
    private val gameDetector: GameDetector,
    private val playtimeTracker: PlaytimeTracker,
    private val dndManager: DndManager,
    private val adBlockManager: AdBlockManager
) : ViewModel() {

    companion object {
        val KEY_VIEW_MODE = stringPreferencesKey("view_mode")
        val KEY_DND_ON_LAUNCH = booleanPreferencesKey("dnd_on_launch")
        val KEY_SORT_OPTION = stringPreferencesKey("sort_option")
        val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val KEY_DAILY_LIMIT_MS = longPreferencesKey("daily_limit_ms")
        val KEY_AD_BLOCK_ON_LAUNCH = booleanPreferencesKey("ad_block_on_launch")
        val KEY_AD_WHITELIST = stringPreferencesKey("ad_block_whitelist")
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _sortOption = MutableStateFlow(SortOption.NAME)
    val sortOption: StateFlow<SortOption> = _sortOption

    private val _viewMode = MutableStateFlow(ViewMode.GRID)
    val viewMode: StateFlow<ViewMode> = _viewMode

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _dndOnLaunch = MutableStateFlow(false)
    val dndOnLaunch: StateFlow<Boolean> = _dndOnLaunch

    private val _adBlockOnLaunch = MutableStateFlow(false)
    val adBlockOnLaunch: StateFlow<Boolean> = _adBlockOnLaunch

    private val _dailyLimitMs = MutableStateFlow(0L)

    // One-shot event: emits the game packageName when daily limit is exceeded
    private val _limitExceededEvent = MutableSharedFlow<String>()
    val limitExceededEvent: SharedFlow<String> = _limitExceededEvent

    val allGames: StateFlow<List<Game>> = repository.getAllVisibleGames()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val favoriteGames: StateFlow<List<Game>> = repository.getFavoriteGames()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val todayPlaytime: StateFlow<Long> = run {
        val startOfDay = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        repository.getTotalPlaytimeSince(startOfDay)
            .map { it ?: 0L }
            .stateIn(viewModelScope, SharingStarted.Lazily, 0L)
    }

    val filteredAndSortedGames: StateFlow<List<Game>> = combine(
        allGames,
        _searchQuery,
        _sortOption
    ) { games, query, sort ->
        val filtered = if (query.isBlank()) games
        else games.filter { it.name.contains(query, ignoreCase = true) }

        when (sort) {
            SortOption.NAME -> filtered.sortedBy { it.name.lowercase() }
            SortOption.LAST_PLAYED -> filtered.sortedByDescending { it.lastPlayed }
            SortOption.MOST_PLAYED -> filtered.sortedByDescending { it.totalPlaytimeMs }
            SortOption.INSTALL_DATE -> filtered.sortedByDescending { it.installDate }
            SortOption.SIZE -> filtered.sortedByDescending { it.appSizeBytes }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadPreferences()
        scanGames()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            context.dataStore.data
                .catch { emit(emptyPreferences()) }
                .collect { prefs ->
                    _viewMode.value = try {
                        ViewMode.valueOf(prefs[KEY_VIEW_MODE] ?: "GRID")
                    } catch (_: Exception) { ViewMode.GRID }
                    _dndOnLaunch.value = prefs[KEY_DND_ON_LAUNCH] ?: false
                    _adBlockOnLaunch.value = prefs[KEY_AD_BLOCK_ON_LAUNCH] ?: false
                    _sortOption.value = try {
                        SortOption.valueOf(prefs[KEY_SORT_OPTION] ?: "NAME")
                    } catch (_: Exception) { SortOption.NAME }
                    _dailyLimitMs.value = prefs[KEY_DAILY_LIMIT_MS] ?: 0L
                }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
        viewModelScope.launch {
            context.dataStore.edit { it[KEY_SORT_OPTION] = option.name }
        }
    }

    fun cycleViewMode() {
        _viewMode.value = when (_viewMode.value) {
            ViewMode.GRID -> ViewMode.LIST
            ViewMode.LIST -> ViewMode.ICON
            ViewMode.ICON -> ViewMode.GRID
        }
        viewModelScope.launch {
            context.dataStore.edit { it[KEY_VIEW_MODE] = _viewMode.value.name }
        }
    }

    fun toggleDndOnLaunch() {
        _dndOnLaunch.value = !_dndOnLaunch.value
        viewModelScope.launch {
            context.dataStore.edit { it[KEY_DND_ON_LAUNCH] = _dndOnLaunch.value }
        }
    }

    fun setAdBlockOnLaunch(enabled: Boolean) {
        _adBlockOnLaunch.value = enabled
        viewModelScope.launch {
            context.dataStore.edit { it[KEY_AD_BLOCK_ON_LAUNCH] = enabled }
        }
    }

    fun toggleFavorite(packageName: String, currentState: Boolean) {
        viewModelScope.launch {
            repository.setFavorite(packageName, !currentState)
        }
    }

    fun scanGames() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val detectedGames = gameDetector.detectInstalledGames()
                val installedPackages = detectedGames.map { it.packageName }

                repository.insertGames(detectedGames)
                repository.markUninstalledExcept(installedPackages)

                // Check for version changes and log updates
                detectedGames.forEach { game ->
                    val existing = repository.getGame(game.packageName)
                    if (existing != null && existing.currentVersion.isNotBlank()
                        && game.currentVersion.isNotBlank()
                        && existing.currentVersion != game.currentVersion
                    ) {
                        repository.insertUpdate(
                            com.gamevault.app.data.model.GameUpdate(
                                packageName = game.packageName,
                                oldVersion = existing.currentVersion,
                                newVersion = game.currentVersion,
                                oldSizeBytes = existing.appSizeBytes,
                                newSizeBytes = game.appSizeBytes
                            )
                        )
                    }
                    repository.updateVersionAndSize(game.packageName, game.currentVersion, game.appSizeBytes)
                }

                if (playtimeTracker.hasUsagePermission()) {
                    val dayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
                    val gamePackages = detectedGames.map { it.packageName }.toSet()
                    playtimeTracker.syncPlaytimeSince(dayAgo, gamePackages)
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun launchGame(packageName: String) {
        viewModelScope.launch {
            val dailyLimit = _dailyLimitMs.value
            val todayPlayed = todayPlaytime.value

            // Check daily limit
            if (dailyLimit > 0 && todayPlayed >= dailyLimit) {
                _limitExceededEvent.emit(packageName)
                return@launch
            }

            repository.setNew(packageName, false)

            if (_dndOnLaunch.value && dndManager.hasPermission()) {
                dndManager.enableDnd()
            }

            if (_adBlockOnLaunch.value && adBlockManager.hasPermission()) {
                adBlockManager.startVpn()
            }

            // Auto-start timer if daily limit is set
            if (dailyLimit > 0) {
                val remainingMs = dailyLimit - todayPlayed
                val gameName = allGames.value.find { it.packageName == packageName }?.name ?: ""
                val timerIntent = Intent(context, TimerService::class.java).apply {
                    action = TimerService.ACTION_START
                    putExtra(TimerService.EXTRA_DURATION_MS, remainingMs)
                    putExtra(TimerService.EXTRA_GAME_NAME, gameName)
                }
                context.startForegroundService(timerIntent)
            }

            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            launchIntent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)
            }
        }
    }

    /** Force launch even when over daily limit (user chose to continue) */
    fun forceLaunchGame(packageName: String) {
        viewModelScope.launch {
            repository.setNew(packageName, false)
            if (_dndOnLaunch.value && dndManager.hasPermission()) {
                dndManager.enableDnd()
            }
            if (_adBlockOnLaunch.value && adBlockManager.hasPermission()) {
                adBlockManager.startVpn()
            }
        }
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        launchIntent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
        }
    }

    fun hideGame(packageName: String) {
        viewModelScope.launch { repository.setHidden(packageName, true) }
    }

    fun unhideGame(packageName: String) {
        viewModelScope.launch { repository.setHidden(packageName, false) }
    }
}
