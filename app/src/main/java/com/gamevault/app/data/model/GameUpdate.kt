package com.gamevault.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "game_updates",
    foreignKeys = [
        ForeignKey(
            entity = Game::class,
            parentColumns = ["packageName"],
            childColumns = ["packageName"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("packageName")]
)
data class GameUpdate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val oldVersion: String,
    val newVersion: String,
    val oldSizeBytes: Long = 0,
    val newSizeBytes: Long = 0,
    val updateTime: Long = System.currentTimeMillis(),
    val changelogNotes: String = ""
)
