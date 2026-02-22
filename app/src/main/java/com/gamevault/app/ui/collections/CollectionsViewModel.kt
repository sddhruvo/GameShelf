package com.gamevault.app.ui.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamevault.app.data.model.GameCollection
import com.gamevault.app.data.model.Game
import com.gamevault.app.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionsViewModel @Inject constructor(
    private val repository: GameRepository
) : ViewModel() {

    val collections: StateFlow<List<GameCollection>> = repository.getAllCollections()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allGames: StateFlow<List<Game>> = repository.getAllVisibleGames()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun getGamesInCollection(collectionId: Long): Flow<List<Game>> {
        return repository.getGamesInCollection(collectionId)
    }

    fun createCollection(name: String, description: String = "") {
        viewModelScope.launch {
            repository.insertCollection(GameCollection(name = name, description = description))
        }
    }

    fun deleteCollection(collection: GameCollection) {
        viewModelScope.launch {
            repository.deleteCollection(collection)
        }
    }

    fun renameCollection(collection: GameCollection, newName: String) {
        viewModelScope.launch {
            repository.updateCollection(collection.copy(name = newName))
        }
    }

    fun addGameToCollection(packageName: String, collectionId: Long) {
        viewModelScope.launch {
            repository.addGameToCollection(packageName, collectionId)
        }
    }

    fun removeGameFromCollection(packageName: String, collectionId: Long) {
        viewModelScope.launch {
            repository.removeGameFromCollection(packageName, collectionId)
        }
    }
}
