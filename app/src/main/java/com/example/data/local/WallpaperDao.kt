package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WallpaperDao {
    @Query("SELECT * FROM wallpapers ORDER BY timestamp DESC")
    fun getAllWallpapers(): Flow<List<WallpaperEntity>>

    @Query("SELECT * FROM wallpapers WHERE category = :category ORDER BY timestamp DESC")
    fun getWallpapersByCategory(category: String): Flow<List<WallpaperEntity>>

    @Query("SELECT * FROM wallpapers WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteWallpapers(): Flow<List<WallpaperEntity>>

    @Query("SELECT * FROM wallpapers WHERE isDownloaded = 1 ORDER BY timestamp DESC")
    fun getDownloadedWallpapers(): Flow<List<WallpaperEntity>>

    @Query("SELECT * FROM wallpapers WHERE id = :id")
    fun getWallpaperById(id: String): Flow<WallpaperEntity?>

    @Query("SELECT * FROM wallpapers WHERE id = :id")
    suspend fun getWallpaperByIdSync(id: String): WallpaperEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(wallpapers: List<WallpaperEntity>)

    @Query("UPDATE wallpapers SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean)

    @Query("UPDATE wallpapers SET isDownloaded = :isDownloaded, localPath = :localPath WHERE id = :id")
    suspend fun updateDownloadStatus(id: String, isDownloaded: Boolean, localPath: String?)

    @Query("UPDATE wallpapers SET views = views + 1 WHERE id = :id")
    suspend fun incrementViews(id: String)

    @Query("UPDATE wallpapers SET downloadsCount = downloadsCount + 1 WHERE id = :id")
    suspend fun incrementDownloads(id: String)

    @Query("SELECT * FROM wallpapers WHERE title LIKE :searchQuery OR category LIKE :searchQuery ORDER BY timestamp DESC")
    fun searchWallpapers(searchQuery: String): Flow<List<WallpaperEntity>>
}
