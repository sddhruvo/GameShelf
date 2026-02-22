package com.gamevault.app.data.db

import androidx.room.*
import com.gamevault.app.data.model.PlaySession
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Query("SELECT * FROM play_sessions WHERE packageName = :packageName ORDER BY startTime DESC")
    fun getSessionsForGame(packageName: String): Flow<List<PlaySession>>

    @Query("SELECT * FROM play_sessions ORDER BY startTime DESC LIMIT :limit")
    fun getRecentSessions(limit: Int = 50): Flow<List<PlaySession>>

    @Query("SELECT * FROM play_sessions WHERE startTime >= :since ORDER BY startTime DESC")
    fun getSessionsSince(since: Long): Flow<List<PlaySession>>

    @Query("SELECT SUM(durationMs) FROM play_sessions WHERE startTime >= :since")
    fun getTotalPlaytimeSince(since: Long): Flow<Long?>

    @Query("SELECT SUM(durationMs) FROM play_sessions WHERE packageName = :packageName")
    fun getTotalPlaytimeForGame(packageName: String): Flow<Long?>

    @Query("SELECT SUM(durationMs) FROM play_sessions WHERE packageName = :packageName AND startTime >= :since")
    fun getPlaytimeForGameSince(packageName: String, since: Long): Flow<Long?>

    @Query("""
        SELECT DISTINCT date(startTime / 1000, 'unixepoch', 'localtime') as playDate
        FROM play_sessions
        ORDER BY playDate DESC
    """)
    suspend fun getDistinctPlayDates(): List<String>

    @Query("""
        SELECT DISTINCT date(startTime / 1000, 'unixepoch', 'localtime') as playDate
        FROM play_sessions
        WHERE packageName = :packageName
        ORDER BY playDate DESC
    """)
    suspend fun getDistinctPlayDatesForGame(packageName: String): List<String>

    @Query("SELECT COUNT(*) FROM play_sessions WHERE packageName = :packageName AND startTime = :startTime AND endTime = :endTime")
    suspend fun sessionExists(packageName: String, startTime: Long, endTime: Long): Int

    @Query("SELECT COALESCE(SUM(durationMs), 0) FROM play_sessions WHERE packageName = :packageName")
    suspend fun getTotalPlaytimeForGameDirect(packageName: String): Long

    @Query("SELECT MAX(endTime) FROM play_sessions WHERE packageName = :packageName")
    suspend fun getLastSessionEnd(packageName: String): Long?

    @Insert
    suspend fun insertSession(session: PlaySession): Long

    @Delete
    suspend fun deleteSession(session: PlaySession)

    @Query("DELETE FROM play_sessions WHERE packageName = :packageName")
    suspend fun deleteSessionsForGame(packageName: String)

    @Query("DELETE FROM play_sessions")
    suspend fun deleteAllSessions()

    @Query("SELECT COUNT(*) FROM play_sessions WHERE packageName = :packageName")
    fun getSessionCount(packageName: String): Flow<Int>

    @Query("SELECT * FROM play_sessions ORDER BY startTime DESC")
    suspend fun getAllSessionsDirect(): List<PlaySession>
}
