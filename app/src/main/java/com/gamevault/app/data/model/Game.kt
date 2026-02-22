package com.gamevault.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class Game(
    @PrimaryKey val packageName: String,
    val name: String,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val rating: Float = 0f,
    val notes: String = "",
    val customCoverUri: String? = null,
    val tags: String = "", // comma-separated
    val firstDetected: Long = System.currentTimeMillis(),
    val lastPlayed: Long = 0L,
    val totalPlaytimeMs: Long = 0L,
    val installDate: Long = 0L,
    val appSizeBytes: Long = 0L,
    val currentVersion: String = "",
    val isInstalled: Boolean = true,
    val isNew: Boolean = false
)
