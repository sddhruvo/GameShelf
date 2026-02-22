package com.gamevault.app.data.db

import androidx.room.*
import com.gamevault.app.data.model.GameCollection
import com.gamevault.app.data.model.Game
import com.gamevault.app.data.model.GameCollectionCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Query("SELECT * FROM collections ORDER BY name ASC")
    fun getAllCollections(): Flow<List<GameCollection>>

    @Query("SELECT * FROM collections WHERE id = :id")
    suspend fun getCollection(id: Long): GameCollection?

    @Query("""
        SELECT g.* FROM games g
        INNER JOIN game_collection_cross_ref gc ON g.packageName = gc.packageName
        WHERE gc.collectionId = :collectionId AND g.isInstalled = 1
        ORDER BY g.name ASC
    """)
    fun getGamesInCollection(collectionId: Long): Flow<List<Game>>

    @Query("""
        SELECT c.* FROM collections c
        INNER JOIN game_collection_cross_ref gc ON c.id = gc.collectionId
        WHERE gc.packageName = :packageName
        ORDER BY c.name ASC
    """)
    fun getCollectionsForGame(packageName: String): Flow<List<GameCollection>>

    @Insert
    suspend fun insertCollection(collection: GameCollection): Long

    @Update
    suspend fun updateCollection(collection: GameCollection)

    @Delete
    suspend fun deleteCollection(collection: GameCollection)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addGameToCollection(crossRef: GameCollectionCrossRef)

    @Delete
    suspend fun removeGameFromCollection(crossRef: GameCollectionCrossRef)

    @Query("DELETE FROM game_collection_cross_ref WHERE packageName = :packageName AND collectionId = :collectionId")
    suspend fun removeGameFromCollectionById(packageName: String, collectionId: Long)

    @Query("SELECT * FROM collections ORDER BY name ASC")
    suspend fun getAllCollectionsDirect(): List<GameCollection>

    @Query("SELECT * FROM game_collection_cross_ref")
    suspend fun getAllCrossRefsDirect(): List<GameCollectionCrossRef>
}
