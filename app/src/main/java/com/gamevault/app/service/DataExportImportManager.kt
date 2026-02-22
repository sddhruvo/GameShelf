package com.gamevault.app.service

import com.gamevault.app.data.db.GameVaultDatabase
import com.gamevault.app.data.model.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class ExportData(
    val exportVersion: Int = 1,
    val exportDate: Long = System.currentTimeMillis(),
    val games: List<Game>,
    val sessions: List<PlaySession>,
    val collections: List<GameCollection>,
    val crossRefs: List<GameCollectionCrossRef>,
    val updates: List<GameUpdate>
)

@Singleton
class DataExportImportManager @Inject constructor(
    private val database: GameVaultDatabase
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportToJson(outputStream: OutputStream) {
        val gameDao = database.gameDao()
        val sessionDao = database.sessionDao()
        val collectionDao = database.collectionDao()
        val updateDao = database.updateDao()

        val exportData = ExportData(
            games = gameDao.getAllGamesDirect(),
            sessions = sessionDao.getAllSessionsDirect(),
            collections = collectionDao.getAllCollectionsDirect(),
            crossRefs = collectionDao.getAllCrossRefsDirect(),
            updates = updateDao.getAllUpdatesDirect()
        )

        val json = gson.toJson(exportData)
        outputStream.write(json.toByteArray(Charsets.UTF_8))
        outputStream.flush()
    }

    suspend fun importFromJson(inputStream: InputStream) {
        val json = inputStream.bufferedReader().readText()
        val data = gson.fromJson(json, ExportData::class.java) ?: return

        val gameDao = database.gameDao()
        val sessionDao = database.sessionDao()
        val collectionDao = database.collectionDao()
        val updateDao = database.updateDao()

        // Import in order: games first, then dependent entities
        data.games.forEach { gameDao.insertGame(it) }
        data.sessions.forEach { sessionDao.insertSession(it) }
        data.collections.forEach { collectionDao.insertCollection(it) }
        data.crossRefs.forEach { collectionDao.addGameToCollection(it) }
        data.updates.forEach { updateDao.insertUpdate(it) }
    }

    fun exportToCsv(outputStream: OutputStream, games: List<Game>, sessions: List<PlaySession>) {
        val writer = outputStream.bufferedWriter()
        writer.write("=== GAMES ===\n")
        writer.write("PackageName,Name,Favorite,Rating,TotalPlaytimeMs,LastPlayed,InstallDate,Version,Size\n")
        games.forEach { g ->
            writer.write("${g.packageName},\"${g.name.replace("\"", "\"\"")}\",${g.isFavorite},${g.rating},${g.totalPlaytimeMs},${g.lastPlayed},${g.installDate},${g.currentVersion},${g.appSizeBytes}\n")
        }
        writer.write("\n=== SESSIONS ===\n")
        writer.write("PackageName,StartTime,EndTime,DurationMs\n")
        sessions.forEach { s ->
            writer.write("${s.packageName},${s.startTime},${s.endTime},${s.durationMs}\n")
        }
        writer.flush()
    }
}
