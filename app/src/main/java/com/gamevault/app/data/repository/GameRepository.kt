package com.gamevault.app.data.repository

import com.gamevault.app.data.db.GameDao
import com.gamevault.app.data.db.SessionDao
import com.gamevault.app.data.db.CollectionDao
import com.gamevault.app.data.db.UpdateDao
import com.gamevault.app.data.model.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepository @Inject constructor(
    private val gameDao: GameDao,
    private val sessionDao: SessionDao,
    private val collectionDao: CollectionDao,
    private val updateDao: UpdateDao
) {
    // Games
    fun getAllVisibleGames(): Flow<List<Game>> = gameDao.getAllVisibleGames()
    fun getAllInstalledGames(): Flow<List<Game>> = gameDao.getAllInstalledGames()
    fun getAllGames(): Flow<List<Game>> = gameDao.getAllGames()
    fun getFavoriteGames(): Flow<List<Game>> = gameDao.getFavoriteGames()
    fun getHiddenGames(): Flow<List<Game>> = gameDao.getHiddenGames()
    fun getMostPlayedGames(limit: Int = 10): Flow<List<Game>> = gameDao.getMostPlayedGames(limit)
    fun getNewGames(): Flow<List<Game>> = gameDao.getNewGames()
    fun searchGames(query: String): Flow<List<Game>> = gameDao.searchGames(query)
    fun getGameFlow(packageName: String): Flow<Game?> = gameDao.getGameFlow(packageName)
    suspend fun getGame(packageName: String): Game? = gameDao.getGame(packageName)
    suspend fun insertGame(game: Game) = gameDao.insertGame(game)
    suspend fun insertGames(games: List<Game>) = gameDao.insertGames(games)
    suspend fun updateGame(game: Game) = gameDao.updateGame(game)
    suspend fun setFavorite(packageName: String, isFavorite: Boolean) = gameDao.setFavorite(packageName, isFavorite)
    suspend fun setHidden(packageName: String, isHidden: Boolean) = gameDao.setHidden(packageName, isHidden)
    suspend fun setRating(packageName: String, rating: Float) = gameDao.setRating(packageName, rating)
    suspend fun setNotes(packageName: String, notes: String) = gameDao.setNotes(packageName, notes)
    suspend fun setTags(packageName: String, tags: String) = gameDao.setTags(packageName, tags)
    suspend fun setCustomCover(packageName: String, uri: String?) = gameDao.setCustomCover(packageName, uri)
    suspend fun updatePlaytime(packageName: String, lastPlayed: Long, totalPlaytime: Long) = gameDao.updatePlaytime(packageName, lastPlayed, totalPlaytime)
    suspend fun setInstalled(packageName: String, isInstalled: Boolean) = gameDao.setInstalled(packageName, isInstalled)
    suspend fun setNew(packageName: String, isNew: Boolean) = gameDao.setNew(packageName, isNew)
    suspend fun updateVersionAndSize(packageName: String, version: String, size: Long) = gameDao.updateVersionAndSize(packageName, version, size)
    suspend fun markUninstalledExcept(installedPackages: List<String>) = gameDao.markUninstalledExcept(installedPackages)

    // Sessions
    fun getSessionsForGame(packageName: String): Flow<List<PlaySession>> = sessionDao.getSessionsForGame(packageName)
    fun getRecentSessions(limit: Int = 50): Flow<List<PlaySession>> = sessionDao.getRecentSessions(limit)
    fun getSessionsSince(since: Long): Flow<List<PlaySession>> = sessionDao.getSessionsSince(since)
    fun getTotalPlaytimeSince(since: Long): Flow<Long?> = sessionDao.getTotalPlaytimeSince(since)
    fun getTotalPlaytimeForGame(packageName: String): Flow<Long?> = sessionDao.getTotalPlaytimeForGame(packageName)
    fun getSessionCount(packageName: String): Flow<Int> = sessionDao.getSessionCount(packageName)
    suspend fun getDistinctPlayDates(): List<String> = sessionDao.getDistinctPlayDates()
    suspend fun getDistinctPlayDatesForGame(packageName: String): List<String> = sessionDao.getDistinctPlayDatesForGame(packageName)
    suspend fun sessionExists(packageName: String, startTime: Long, endTime: Long): Boolean = sessionDao.sessionExists(packageName, startTime, endTime) > 0
    suspend fun getTotalPlaytimeForGameDirect(packageName: String): Long = sessionDao.getTotalPlaytimeForGameDirect(packageName)
    suspend fun getLastSessionEnd(packageName: String): Long? = sessionDao.getLastSessionEnd(packageName)
    suspend fun insertSession(session: PlaySession): Long = sessionDao.insertSession(session)
    suspend fun deleteSessionsForGame(packageName: String) = sessionDao.deleteSessionsForGame(packageName)
    suspend fun deleteAllSessions() = sessionDao.deleteAllSessions()

    // Collections
    fun getAllCollections(): Flow<List<GameCollection>> = collectionDao.getAllCollections()
    fun getGamesInCollection(collectionId: Long): Flow<List<Game>> = collectionDao.getGamesInCollection(collectionId)
    fun getCollectionsForGame(packageName: String): Flow<List<GameCollection>> = collectionDao.getCollectionsForGame(packageName)
    suspend fun getCollection(id: Long): GameCollection? = collectionDao.getCollection(id)
    suspend fun insertCollection(collection: GameCollection): Long = collectionDao.insertCollection(collection)
    suspend fun updateCollection(collection: GameCollection) = collectionDao.updateCollection(collection)
    suspend fun deleteCollection(collection: GameCollection) = collectionDao.deleteCollection(collection)
    suspend fun addGameToCollection(packageName: String, collectionId: Long) = collectionDao.addGameToCollection(GameCollectionCrossRef(packageName, collectionId))
    suspend fun removeGameFromCollection(packageName: String, collectionId: Long) = collectionDao.removeGameFromCollectionById(packageName, collectionId)

    // Updates
    fun getUpdatesForGame(packageName: String): Flow<List<GameUpdate>> = updateDao.getUpdatesForGame(packageName)
    fun getRecentUpdates(limit: Int = 50): Flow<List<GameUpdate>> = updateDao.getRecentUpdates(limit)
    fun getUpdateCount(packageName: String): Flow<Int> = updateDao.getUpdateCount(packageName)
    suspend fun getLatestUpdate(packageName: String): GameUpdate? = updateDao.getLatestUpdate(packageName)
    suspend fun insertUpdate(update: GameUpdate): Long = updateDao.insertUpdate(update)
    suspend fun setChangelogNotes(id: Long, notes: String) = updateDao.setChangelogNotes(id, notes)
    suspend fun deleteUpdatesForGame(packageName: String) = updateDao.deleteUpdatesForGame(packageName)
}
