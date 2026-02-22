package com.gamevault.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gamevault.app.data.model.Game
import com.gamevault.app.data.model.GameCollection
import com.gamevault.app.data.model.GameCollectionCrossRef
import com.gamevault.app.data.model.GameUpdate
import com.gamevault.app.data.model.PlaySession

@Database(
    entities = [
        Game::class,
        PlaySession::class,
        GameCollection::class,
        GameCollectionCrossRef::class,
        GameUpdate::class
    ],
    version = 1,
    exportSchema = true
)
abstract class GameShelfDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun sessionDao(): SessionDao
    abstract fun collectionDao(): CollectionDao
    abstract fun updateDao(): UpdateDao
}
