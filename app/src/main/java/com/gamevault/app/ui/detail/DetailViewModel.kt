package com.gamevault.app.ui.detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamevault.app.data.model.*
import com.gamevault.app.data.repository.GameRepository
import com.gamevault.app.service.AdBlockManager
import com.gamevault.app.service.AdBlockVpnService
import com.gamevault.app.service.PlaytimeTracker
import com.gamevault.app.util.FormatUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val repository: GameRepository,
    private val playtimeTracker: PlaytimeTracker,
    private val adBlockManager: AdBlockManager
) : ViewModel() {

    private val packageName: String = savedStateHandle.get<String>("packageName") ?: ""

    val game: StateFlow<Game?> = repository.getGameFlow(packageName)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val sessions: StateFlow<List<PlaySession>> = repository.getSessionsForGame(packageName)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val sessionCount: StateFlow<Int> = repository.getSessionCount(packageName)
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val updates: StateFlow<List<GameUpdate>> = repository.getUpdatesForGame(packageName)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val updateCount: StateFlow<Int> = repository.getUpdateCount(packageName)
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val collections: StateFlow<List<GameCollection>> = repository.getCollectionsForGame(packageName)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allCollections: StateFlow<List<GameCollection>> = repository.getAllCollections()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val isAdWhitelisted: StateFlow<Boolean> = adBlockManager.getWhitelistedPackages()
        .map { packageName in it }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    private val _playStreak = MutableStateFlow(0)
    val playStreak: StateFlow<Int> = _playStreak

    init {
        viewModelScope.launch {
            val dates = repository.getDistinctPlayDatesForGame(packageName)
            _playStreak.value = FormatUtils.calculateStreak(dates)
        }
    }

    fun launchGame() {
        viewModelScope.launch {
            repository.setNew(packageName, false)
        }
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        launchIntent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val current = game.value ?: return@launch
            repository.setFavorite(packageName, !current.isFavorite)
        }
    }

    fun setRating(rating: Float) {
        viewModelScope.launch {
            repository.setRating(packageName, rating)
        }
    }

    fun setNotes(notes: String) {
        viewModelScope.launch {
            repository.setNotes(packageName, notes)
        }
    }

    fun setTags(tags: String) {
        viewModelScope.launch {
            repository.setTags(packageName, tags)
        }
    }

    fun setCustomCover(uri: Uri?) {
        viewModelScope.launch {
            repository.setCustomCover(packageName, uri?.toString())
        }
    }

    fun toggleHidden() {
        viewModelScope.launch {
            val current = game.value ?: return@launch
            repository.setHidden(packageName, !current.isHidden)
        }
    }

    fun addToCollection(collectionId: Long) {
        viewModelScope.launch {
            repository.addGameToCollection(packageName, collectionId)
        }
    }

    fun removeFromCollection(collectionId: Long) {
        viewModelScope.launch {
            repository.removeGameFromCollection(packageName, collectionId)
        }
    }

    fun setChangelogNotes(updateId: Long, notes: String) {
        viewModelScope.launch {
            repository.setChangelogNotes(updateId, notes)
        }
    }

    fun clearStats() {
        viewModelScope.launch {
            repository.deleteSessionsForGame(packageName)
            repository.updatePlaytime(packageName, 0L, 0L)
        }
    }

    fun toggleAdWhitelist() {
        viewModelScope.launch {
            adBlockManager.toggleWhitelist(packageName)
            if (AdBlockVpnService.isRunning) {
                adBlockManager.rebuildVpn()
            }
        }
    }

    fun openAppSettings() {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
