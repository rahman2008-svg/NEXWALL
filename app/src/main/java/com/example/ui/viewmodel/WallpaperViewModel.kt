package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.WallpaperEntity
import com.example.data.repository.WallpaperRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WallpaperViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WallpaperRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = WallpaperRepository(application, database.wallpaperDao())
    }

    // State flows
    val allWallpapers: StateFlow<List<WallpaperEntity>> = repository.allWallpapers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteWallpapers: StateFlow<List<WallpaperEntity>> = repository.favoriteWallpapers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedWallpapers: StateFlow<List<WallpaperEntity>> = repository.downloadedWallpapers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<WallpaperEntity>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.trim().isEmpty()) {
                flowOf(emptyList())
            } else {
                repository.searchWallpapers(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val categoryWallpapers: StateFlow<List<WallpaperEntity>> = _selectedCategory
        .flatMapLatest { category ->
            if (category == null) {
                flowOf(emptyList())
            } else {
                repository.getWallpapersByCategory(category)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedWallpaperId = MutableStateFlow<String?>(null)
    val selectedWallpaperId: StateFlow<String?> = _selectedWallpaperId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedWallpaper: StateFlow<WallpaperEntity?> = _selectedWallpaperId
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(null)
            } else {
                repository.getWallpaperById(id)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Action state
    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun selectWallpaper(id: String?) {
        _selectedWallpaperId.value = id
        if (id != null) {
            viewModelScope.launch {
                repository.incrementViews(id)
            }
        }
    }

    fun toggleFavorite(id: String, currentFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(id, !currentFavorite)
        }
    }

    fun downloadWallpaper(wallpaper: WallpaperEntity) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Downloading image to Gallery...")
            val result = repository.downloadWallpaper(wallpaper)
            result.fold(
                onSuccess = { path ->
                    _actionState.value = ActionState.Success("Wallpaper downloaded to Gallery!")
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.localizedMessage ?: "Download failed.")
                }
            )
        }
    }

    /**
     * target: 1 = Home Screen, 2 = Lock Screen, 3 = Both
     */
    fun setWallpaper(wallpaper: WallpaperEntity, target: Int) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Applying wallpaper...")
            val result = repository.setWallpaper(wallpaper, target)
            result.fold(
                onSuccess = {
                    _actionState.value = ActionState.Success("Wallpaper applied successfully!")
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.localizedMessage ?: "Failed to set wallpaper.")
                }
            )
        }
    }

    fun resetActionState() {
        _actionState.value = ActionState.Idle
    }

    sealed interface ActionState {
        object Idle : ActionState
        data class Loading(val message: String) : ActionState
        data class Success(val message: String) : ActionState
        data class Error(val message: String) : ActionState
    }
}
