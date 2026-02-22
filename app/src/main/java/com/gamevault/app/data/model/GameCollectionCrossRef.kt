package com.gamevault.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "game_collection_cross_ref",
    primaryKeys = ["packageName", "collectionId"],
    foreignKeys = [
        ForeignKey(
            entity = Game::class,
            parentColumns = ["packageName"],
            childColumns = ["packageName"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GameCollection::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("collectionId")]
)
data class GameCollectionCrossRef(
    val packageName: String,
    val collectionId: Long
)
