package com.example.data.repository

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.data.local.WallpaperDao
import com.example.data.local.WallpaperEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class WallpaperRepository(
    private val context: Context,
    private val dao: WallpaperDao
) {
    private val client = OkHttpClient()

    init {
        // Run seed on initialization asynchronously
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            seedDatabaseIfEmpty()
        }
    }

    val allWallpapers: Flow<List<WallpaperEntity>> = dao.getAllWallpapers()
    val favoriteWallpapers: Flow<List<WallpaperEntity>> = dao.getFavoriteWallpapers()
    val downloadedWallpapers: Flow<List<WallpaperEntity>> = dao.getDownloadedWallpapers()

    fun getWallpapersByCategory(category: String): Flow<List<WallpaperEntity>> {
        return dao.getWallpapersByCategory(category)
    }

    fun getWallpaperById(id: String): Flow<WallpaperEntity?> {
        return dao.getWallpaperById(id)
    }

    fun searchWallpapers(query: String): Flow<List<WallpaperEntity>> {
        return dao.searchWallpapers("%$query%")
    }

    suspend fun toggleFavorite(id: String, isFavorite: Boolean) {
        withContext(Dispatchers.IO) {
            dao.updateFavoriteStatus(id, isFavorite)
        }
    }

    suspend fun incrementViews(id: String) {
        withContext(Dispatchers.IO) {
            dao.incrementViews(id)
        }
    }

    /**
     * Downloads/caches wallpaper to internal storage for offline browsing & sets flag.
     * Also exports the file directly to public pictures gallery.
     */
    suspend fun downloadWallpaper(wallpaper: WallpaperEntity): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 1. Download the bytes
            val request = Request.Builder().url(wallpaper.url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful || response.body == null) {
                return@withContext Result.failure(Exception("Failed to connect to image source: ${response.code}"))
            }

            val inputStream: InputStream = response.body!!.byteStream()

            // 2. Save offline to internal cache/files for deep previews
            val filename = "nexwall_${wallpaper.id}.jpg"
            val internalFile = File(context.filesDir, filename)
            FileOutputStream(internalFile).use { outStream ->
                inputStream.copyTo(outStream)
            }

            // 3. Export to Public MediaStore Gallery
            val publicUri = saveImageToGallery(internalFile, wallpaper.title)

            // 4. Update Room Database
            dao.updateDownloadStatus(wallpaper.id, true, internalFile.absolutePath)
            dao.incrementDownloads(wallpaper.id)

            Result.success(internalFile.absolutePath)
        } catch (e: Exception) {
            Log.e("WallpaperRepository", "Error downloading wallpaper", e)
            Result.failure(e)
        }
    }

    /**
     * Set wallpaper via native WallpaperManager.
     * targets: 1 = Home, 2 = Lock, 3 = Both
     */
    suspend fun setWallpaper(wallpaper: WallpaperEntity, target: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            
            // Resolve image source: prefer downloaded local file, fallback to dynamic stream
            val bitmap = if (wallpaper.isDownloaded && !wallpaper.localPath.isNullOrEmpty()) {
                val file = File(wallpaper.localPath)
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)
                } else null
            } else {
                // Read from network Stream
                val request = Request.Builder().url(wallpaper.url).build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful && response.body != null) {
                    BitmapFactory.decodeStream(response.body!!.byteStream())
                } else null
            }

            if (bitmap == null) {
                return@withContext Result.failure(Exception("Unable to decode or download image source"))
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                when (target) {
                    1 -> wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                    2 -> wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                    3 -> {
                        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                    }
                }
            } else {
                // Legacy support
                wallpaperManager.setBitmap(bitmap)
            }

            Result.success(true)
        } catch (e: Exception) {
            Log.e("WallpaperRepository", "Error applying wallpaper", e)
            Result.failure(e)
        }
    }

    /**
     * Saves file into the shared Android Gallery using MediaStore API.
     */
    private fun saveImageToGallery(sourceFile: File, title: String): Uri? {
        val resolver = context.contentResolver
        val filename = "${title.replace(" ", "_")}_${System.currentTimeMillis()}.jpg"

        val imageDetails = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/NexWall Studio X")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val uri = resolver.insert(collectionUri, imageDetails) ?: return null

        try {
            resolver.openOutputStream(uri)?.use { outStream: OutputStream ->
                sourceFile.inputStream().use { inStream ->
                    inStream.copyTo(outStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                imageDetails.clear()
                imageDetails.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, imageDetails, null, null)
            }
            return uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            Log.e("WallpaperRepository", "Failed to save to Gallery", e)
            return null
        }
    }

    private suspend fun seedDatabaseIfEmpty() {
        val current = dao.getAllWallpapers().firstOrNull()
        if (current.isNullOrEmpty()) {
            val seedList = listOf(
                // Nature
                WallpaperEntity(
                    id = "nat_1",
                    title = "Mist Mountain Peak",
                    category = "Nature",
                    author = "Unsplash / Prince Rahman",
                    url = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=1080&h=1920&q=80",
                    thumbnailUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=360&h=640&q=80"
                ),
                WallpaperEntity(
                    id = "nat_2",
                    title = "Golden Forest Sunbeams",
                    category = "Nature",
                    author = "Unsplash / NexVora",
                    url = "https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=1080&h=1920&q=80",
                    thumbnailUrl = "https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=360&h=640&q=80"
                ),
                WallpaperEntity(
                    id = "nat_3",
                    title = "Tropical Ocean Breeze",
                    category = "Nature",
                    author = "Unsplash / Abdur Rahman",
                    url = "https://images.unsplash.com/photo-1505118380757-91f5f5632de0?auto=format&fit=crop&w=1080&h=1920&q=80",
                    thumbnailUrl = "https://images.unsplash.com/photo-1505118380757-91f5f5632de0?auto=format&fit=crop&w=360&h=640&q=80"
                ),
                WallpaperEntity(
                    id = "nat_4",
                    title = "Iceland Glacier Ice",
                    category = "Nature",
                    author = "Unsplash / Prince Rahman",
                    url = "https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?auto=format&fit=crop&w=1080&h=1920&q=80",
                    thumbnailUrl = "https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?auto=format&fit=crop&w=360&h=640&q=80"
                ),
                WallpaperEntity(
                    id = "nat_5",
                    title = "Autumn Forest Pathway",
                    category = "Nature",
                    author = "Unsplash / NexVora",
                    url = "https://images.unsplash.com/photo-1506126613408-eca07ce68773?auto=format&fit=crop&w=1080&h=1920&q=80",
                    thumbnailUrl = "https://images.unsplash.com/photo-1506126613408-eca07ce68773?auto=format&fit=crop&w=360&h=640&q=80"
                ),

                // Abstract
                WallpaperEntity(
                    id = "abs_1",
                    title = "Holographic Fluid Art",
                    category = "Abstract",
                    author = "Unsplash / NexVora Lab",
                    url = "https://images.unsplash.com/photo-1541701494587-cb58502866ab?auto=format&fit=crop&w=1080&h=1920&q=80",
                    thumbnailUrl = "https://images.unsplash.com/photo-1541701494587-cb58502866ab?auto=format&fit=crop&w=360&h=640&q=80"
                ),
                WallpaperEntity(
                    id = "abs_2",
                    title = "Pastel Acrylic Canvas",
                    category = "Abstract",
                    author = "Unsplash / Prince Rahman",
                    url = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?auto=format&fit=crop&w=1080&h=1920&q=80",
                    thumbnailUrl = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?auto=format&fit=crop&w=360&h=640&q=80"
                ),
                WallpaperEntity(
                    id = "abs_3",
                    title = "Golden Dream Waves",
                    category = "Abstract",
                    author = "Unsplash / Abdur Rahman",
                    url = "https://images.unsplash.com/photo-1518531933037-91b2f5f229cc?auto=format&fit=crop&w=1080&h=1920&q=80",
                    thumbnailUrl = "https://images.unsplash.com/photo-1518531933037-91b2f5f229cc?auto=format&fit=crop&w=360&h=640&q=80"
                ),
                WallpaperEntity(
                    id = "abs_4",
                    title = "Cyan Cyber Grid",
                    category = "Abstract",
                    author = "Unsplash / NexVora",
                    url = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?auto=format&fit=crop&w=1080&h=1920&q=80",
                    thumbnailUrl = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?auto=format&fit=crop&w=360&h=640&q=80"
                ),
                WallpaperEntity(
                    id = "abs_5",
                    title = "Vibrant Liquid Splash",
                    category = "Abstract",
                    author = "Unsplash / Prince Rahman",
                    url = "https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?auto=format&fit=crop&w=1080&h=1920&q=80",
                    thumbnailUrl = "https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?auto=format&fit=crop&w=360&h=640&q=80"
                ),

                // Anime
                WallpaperEntity(
                    id = "ani_1",
                    title = "Tokyo Midnight Neon",
                    category = "Anime",
                    author = "Unsplash / NexVora Lab",
                    url = "https://images.unsplash.com/photo-1540959733332-eab4deceeaf7?auto=format&fit=crop&w=1080&h=1920&q=80",
                    thumbnailUrl = "https://images.unsplash.com/photo-1540959733332-eab4deceeaf7?auto=format&fit=crop&w=360&h=640&q=80"
                ),
                WallpaperEntity(
                    id = "ani_2",
                    title = "Mount Fuji Twilight",
                    category = "Anime",
                    author = "Unsplash / Prince Rahman",
                    url = "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?auto=format&fit=crop&w=1080&h=1920&q=80",
                    thumbnailUrl = "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?auto=format&fit=crop&w=360&h=640&q=80"
                ),
                WallpaperEntity(
                    id = "ani_3",
                    title = "Traditional Kyoto Pagoda",
                    category = "Anime",
                    author = "Unsplash / Abdur Rahman",
                    url = "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?auto=format&fit=crop&w=1080&h=1920&q=80",
                    thumbnailUrl = "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?auto=format&fit=crop&w=360&h=640&q=80"
                ),
                WallpaperEntity(
                    id = "ani_4",
                    title = "Cyberpunk Transit Way",
                    category = "Anime",
                    author = "Unsplash / NexVora",
                    url = "https://images.unsplash.com/photo-1515621061946-eff1c2a352bd?auto=format&fit=crop&w=1080&h=1920&q=80",
                    thumbnailUrl = "https://images.unsplash.com/photo-1515621061946-eff1c2a352bd?auto=format&fit=crop&w=360&h=640&q=80"
                ),
                WallpaperEntity(
                    id = "ani_5",
                    title = "Quiet Railway Crossing",
                    category = "Anime",
                    author = "Unsplash / Prince Rahman",
                    url = "https://images.unsplash.com/photo-1528127269322-539801943592?auto=format&fit=crop&w=1080&h=1920&q=80",
                    thumbnailUrl = "https://images.unsplash.com/photo-1528127269322-539801943592?auto=format&fit=crop&w=360&h=640&q=80"
                ),

                // Dark
                WallpaperEntity(
                    id = "drk_1",
                    title = "Cosmic Dark Eclipse",
                    category = "Dark",
                    author = "Unsplash / NexVora Lab",
                    url = "https://images.unsplash.com/photo-1538370965046-79c0d6907d47?auto=format&fit=crop&w=1080&h=1920&q=80",
                    thumbnailUrl = "https://images.unsplash.com/photo-1538370965046-79c0d6907d47?auto=format&fit=crop&w=360&h=640&q=80"
                ),
                WallpaperEntity(
                    id = "drk_2",
                    title = "Charcoal Obsidian Glass",
                    category = "Dark",
                    author = "Unsplash / Prince Rahman",
                    url = "https://images.unsplash.com/photo-1502134249126-9f3755a50d78?auto=format&fit=crop&w=1080&h=1920&q=80",
                    thumbnailUrl = "https://images.unsplash.com/photo-1502134249126-9f3755a50d78?auto=format&fit=crop&w=360&h=640&q=80"
                ),
                WallpaperEntity(
                    id = "drk_3",
                    title = "Gothic Spire Castle",
                    category = "Dark",
                    author = "Unsplash / Abdur Rahman",
                    url = "https://images.unsplash.com/photo-1519074002996-a69e7ac46a42?auto=format&fit=crop&w=1080&h=1920&q=80",
                    thumbnailUrl = "https://images.unsplash.com/photo-1519074002996-a69e7ac46a42?auto=format&fit=crop&w=360&h=640&q=80"
                ),
                WallpaperEntity(
                    id = "drk_4",
                    title = "Purple Neon Alleyway",
                    category = "Dark",
                    author = "Unsplash / NexVora",
                    url = "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?auto=format&fit=crop&w=1080&h=1920&q=80",
                    thumbnailUrl = "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?auto=format&fit=crop&w=360&h=640&q=80"
                ),
                WallpaperEntity(
                    id = "drk_5",
                    title = "Silent Forest Night",
                    category = "Dark",
                    author = "Unsplash / Prince AR Abdur Rahman",
                    url = "https://images.unsplash.com/photo-1482862549707-f63cb32c5fd9?auto=format&fit=crop&w=1080&h=1920&q=80",
                    thumbnailUrl = "https://images.unsplash.com/photo-1482862549707-f63cb32c5fd9?auto=format&fit=crop&w=360&h=640&q=80"
                ),

                // Minimal
                WallpaperEntity(
                    id = "min_1",
                    title = "Earthy Sand Dunes",
                    category = "Minimal",
                    author = "Unsplash / Prince Rahman",
                    url = "https://images.unsplash.com/photo-1473448912268-2022ce9509d8?auto=format&fit=crop&w=1080&h=1920&q=80",
                    thumbnailUrl = "https://images.unsplash.com/photo-1473448912268-2022ce9509d8?auto=format&fit=crop&w=360&h=640&q=80"
                ),
                WallpaperEntity(
                    id = "min_2",
                    title = "Clean Geometry Lines",
                    category = "Minimal",
                    author = "Unsplash / NexVora",
                    url = "https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=1080&h=1920&q=80",
                    thumbnailUrl = "https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=360&h=640&q=80"
                ),
                WallpaperEntity(
                    id = "min_3",
                    title = "Symmetrical Pale Archway",
                    category = "Minimal",
                    author = "Unsplash / Abdur Rahman",
                    url = "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?auto=format&fit=crop&w=1080&h=1920&q=80",
                    thumbnailUrl = "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?auto=format&fit=crop&w=360&h=640&q=80"
                ),
                WallpaperEntity(
                    id = "min_4",
                    title = "Foggy Sage Meadow",
                    category = "Minimal",
                    author = "Unsplash / NexVora Lab",
                    url = "https://images.unsplash.com/photo-1434064511983-18c6dae20ed5?auto=format&fit=crop&w=1080&h=1920&q=80",
                    thumbnailUrl = "https://images.unsplash.com/photo-1434064511983-18c6dae20ed5?auto=format&fit=crop&w=360&h=640&q=80"
                ),
                WallpaperEntity(
                    id = "min_5",
                    title = "Peach Pastel Single Chair",
                    category = "Minimal",
                    author = "Unsplash / Prince Rahman",
                    url = "https://images.unsplash.com/photo-1533090161767-e6ffed986c88?auto=format&fit=crop&w=1080&h=1920&q=80",
                    thumbnailUrl = "https://images.unsplash.com/photo-1533090161767-e6ffed986c88?auto=format&fit=crop&w=360&h=640&q=80"
                ),

                // Tech
                WallpaperEntity(
                    id = "tch_1",
                    title = "Golden Circuit Blueprint",
                    category = "Tech",
                    author = "Unsplash / NexVora Lab",
                    url = "https://images.unsplash.com/photo-1601987177651-8edfe6c20009?auto=format&fit=crop&w=1080&h=1920&q=80",
                    thumbnailUrl = "https://images.unsplash.com/photo-1601987177651-8edfe6c20009?auto=format&fit=crop&w=360&h=640&q=80"
                ),
                WallpaperEntity(
                    id = "tch_2",
                    title = "Cybernetic Digital Matrix",
                    category = "Tech",
                    author = "Unsplash / Prince Rahman",
                    url = "https://images.unsplash.com/photo-1563089145-599997674d42?auto=format&fit=crop&w=1080&h=1920&q=80",
                    thumbnailUrl = "https://images.unsplash.com/photo-1563089145-599997674d42?auto=format&fit=crop&w=360&h=640&q=80"
                ),
                WallpaperEntity(
                    id = "tch_3",
                    title = "Deep Hologram Interface",
                    category = "Tech",
                    author = "Unsplash / Abdur Rahman",
                    url = "https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=1080&h=1920&q=80",
                    thumbnailUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=360&h=640&q=80"
                ),
                WallpaperEntity(
                    id = "tch_4",
                    title = "Supercomputer Server Unit",
                    category = "Tech",
                    author = "Unsplash / NexVora",
                    url = "https://images.unsplash.com/photo-1558494949-ef010cbdcc31?auto=format&fit=crop&w=1080&h=1920&q=80",
                    thumbnailUrl = "https://images.unsplash.com/photo-1558494949-ef010cbdcc31?auto=format&fit=crop&w=360&h=640&q=80"
                ),
                WallpaperEntity(
                    id = "tch_5",
                    title = "Glow Optical Fiber",
                    category = "Tech",
                    author = "Unsplash / Prince Rahman",
                    url = "https://images.unsplash.com/photo-1544256718-3bcf237f3974?auto=format&fit=crop&w=1080&h=1920&q=80",
                    thumbnailUrl = "https://images.unsplash.com/photo-1544256718-3bcf237f3974?auto=format&fit=crop&w=360&h=640&q=80"
                )
            )
            dao.insertAll(seedList)
            Log.d("WallpaperRepository", "Successfully pre-populated database with ${seedList.size} walls")
        }
    }
}
