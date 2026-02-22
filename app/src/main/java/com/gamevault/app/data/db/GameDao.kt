package com.gamevault.app.data.db

import androidx.room.*
import com.gamevault.app.data.model.Game
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    @Query("SELECT * FROM games WHERE isInstalled = 1 AND isHidden = 0 ORDER BY name ASC")
    fun getAllVisibleGames(): Flow<List<Game>>

    @Query("SELECT * FROM games WHERE isInstalled = 1 ORDER BY name ASC")
    fun getAllInstalledGames(): Flow<List<Game>>

    @Query("SELECT * FROM games ORDER BY name ASC")
    fun getAllGames(): Flow<List<Game>>

    @Query("SELECT * FROM games ORDER BY name ASC")
    suspend fun getAllGamesDirect(): List<Game>

    @Query("SELECT * FROM games WHERE isFavorite = 1 AND isInstalled = 1 AND isHidden = 0 ORDER BY name ASC")
    fun getFavoriteGames(): Flow<List<Game>>

    @Query("SELECT * FROM games WHERE isHidden = 1 ORDER BY name ASC")
    fun getHiddenGames(): Flow<List<Game>>

    @Query("SELECT * FROM games WHERE packageName = :packageName")
    suspend fun getGame(packageName: String): Game?

    @Query("SELECT * FROM games WHERE packageName = :packageName")
    fun getGameFlow(packageName: String): Flow<Game?>

    @Query("SELECT * FROM games WHERE isInstalled = 1 AND isHidden = 0 AND name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchGames(query: String): Flow<List<Game>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGame(game: Game)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGames(games: List<Game>)

    @Update
    suspend fun updateGame(game: Game)

    @Query("UPDATE games SET isFavorite = :isFavorite WHERE packageName = :packageName")
    suspend fun setFavorite(packageName: String, isFavorite: Boolean)

    @Query("UPDATE games SET isHidden = :isHidden WHERE packageName = :packageName")
    suspend fun setHidden(packageName: String, isHidden: Boolean)

    @Query("UPDATE games SET rating = :rating WHERE packageName = :packageName")
    suspend fun setRating(packageName: String, rating: Float)

    @Query("UPDATE games SET notes = :notes WHERE packageName = :packageName")
    suspend fun setNotes(packageName: String, notes: String)

    @Query("UPDATE games SET tags = :tags WHERE packageName = :packageName")
    suspend fun setTags(packageName: String, tags: String)

    @Query("UPDATE games SET customCoverUri = :uri WHERE packageName = :packageName")
    suspend fun setCustomCover(packageName: String, uri: String?)

    @Query("UPDATE games SET lastPlayed = :lastPlayed, totalPlaytimeMs = :totalPlaytime WHERE packageName = :packageName")
    suspend fun updatePlaytime(packageName: String, lastPlayed: Long, totalPlaytime: Long)

    @Query("UPDATE games SET isInstalled = :isInstalled WHERE packageName = :packageName")
    suspend fun setInstalled(packageName: String, isInstalled: Boolean)

    @Query("UPDATE games SET isNew = :isNew WHERE packageName = :packageName")
    suspend fun setNew(packageName: String, isNew: Boolean)

    @Query("UPDATE games SET currentVersion = :version, appSizeBytes = :size WHERE packageName = :packageName")
    suspend fun updateVersionAndSize(packageName: String, version: String, size: Long)

    @Query("UPDATE games SET isInstalled = 0 WHERE packageName NOT IN (:installedPackages) AND isInstalled = 1")
    suspend fun markUninstalledExcept(installedPackages: List<String>)

    @Query("SELECT * FROM games WHERE isInstalled = 1 AND isHidden = 0 ORDER BY totalPlaytimeMs DESC LIMIT :limit")
    fun getMostPlayedGames(limit: Int = 10): Flow<List<Game>>

    @Query("SELECT * FROM games WHERE isNew = 1 AND isInstalled = 1")
    fun getNewGames(): Flow<List<Game>>

    @Delete
    suspend fun deleteGame(game: Game)

    @Query("DELETE FROM games WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)
}
