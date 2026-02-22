package com.gamevault.app.data.db

import androidx.room.*
import com.gamevault.app.data.model.GameUpdate
import kotlinx.coroutines.flow.Flow

@Dao
interface UpdateDao {

    @Query("SELECT * FROM game_updates WHERE packageName = :packageName ORDER BY updateTime DESC")
    fun getUpdatesForGame(packageName: String): Flow<List<GameUpdate>>

    @Query("SELECT * FROM game_updates ORDER BY updateTime DESC LIMIT :limit")
    fun getRecentUpdates(limit: Int = 50): Flow<List<GameUpdate>>

    @Query("SELECT COUNT(*) FROM game_updates WHERE packageName = :packageName")
    fun getUpdateCount(packageName: String): Flow<Int>

    @Query("SELECT * FROM game_updates WHERE packageName = :packageName ORDER BY updateTime DESC LIMIT 1")
    suspend fun getLatestUpdate(packageName: String): GameUpdate?

    @Insert
    suspend fun insertUpdate(update: GameUpdate): Long

    @Update
    suspend fun updateGameUpdate(update: GameUpdate)

    @Query("UPDATE game_updates SET changelogNotes = :notes WHERE id = :id")
    suspend fun setChangelogNotes(id: Long, notes: String)

    @Query("DELETE FROM game_updates WHERE packageName = :packageName")
    suspend fun deleteUpdatesForGame(packageName: String)

    @Query("DELETE FROM game_updates")
    suspend fun deleteAllUpdates()

    @Query("SELECT * FROM game_updates ORDER BY updateTime DESC")
    suspend fun getAllUpdatesDirect(): List<GameUpdate>
}
