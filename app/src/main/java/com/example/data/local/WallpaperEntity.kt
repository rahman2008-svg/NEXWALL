package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallpapers")
data class WallpaperEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val author: String,
    val url: String,
    val thumbnailUrl: String,
    val isFavorite: Boolean = false,
    val isDownloaded: Boolean = false,
    val localPath: String? = null,
    val views: Int = 0,
    val downloadsCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
