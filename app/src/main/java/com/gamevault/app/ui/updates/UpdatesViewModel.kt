package com.gamevault.app.ui.updates

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamevault.app.data.model.Game
import com.gamevault.app.data.model.GameUpdate
import com.gamevault.app.data.repository.GameRepository
import com.gamevault.app.service.GameDetector
import com.gamevault.app.service.PlaytimeTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UpdateFeedItem(
    val update: GameUpdate,
    val gameName: String
)

@HiltViewModel
class UpdatesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: GameRepository,
    private val gameDetector: GameDetector,
    private val playtimeTracker: PlaytimeTracker
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    val newGames: StateFlow<List<Game>> = repository.getNewGames()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val allGames: StateFlow<List<Game>> = repository.getAllVisibleGames()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val recentUpdates: StateFlow<List<GameUpdate>> = repository.getRecentUpdates(100)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val feedItems: StateFlow<List<UpdateFeedItem>> = combine(
        recentUpdates, allGames
    ) { updates, games ->
        val gameMap = games.associateBy { it.packageName }
        updates.map { update ->
            UpdateFeedItem(
                update = update,
                gameName = gameMap[update.packageName]?.name ?: update.packageName
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalUpdateCount: StateFlow<Int> = recentUpdates.map { it.size }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val thisWeekCount: StateFlow<Int> = recentUpdates.map { updates ->
        val weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        updates.count { it.updateTime > weekAgo }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    fun refreshGames() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val detectedGames = gameDetector.detectInstalledGames()
                val installedPackages = detectedGames.map { it.packageName }

                repository.insertGames(detectedGames)
                repository.markUninstalledExcept(installedPackages)

                detectedGames.forEach { game ->
                    val existing = repository.getGame(game.packageName)
                    if (existing != null && existing.currentVersion.isNotBlank()
                        && game.currentVersion.isNotBlank()
                        && existing.currentVersion != game.currentVersion
                    ) {
                        repository.insertUpdate(
                            GameUpdate(
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
                    val dayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
                    val gamePackages = detectedGames.map { it.packageName }.toSet()
                    playtimeTracker.syncPlaytimeSince(dayAgo, gamePackages)
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun openPlayStore(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun searchGameNews(gameName: String) {
        val query = Uri.encode("$gameName game update news")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun saveNote(updateId: Long, note: String) {
        viewModelScope.launch {
            repository.setChangelogNotes(updateId, note)
        }
    }

    fun markGameSeen(packageName: String) {
        viewModelScope.launch {
            repository.setNew(packageName, false)
        }
    }
}
