package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.WallpaperViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: WallpaperViewModel = viewModel()
                MainAppScaffold(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScaffold(viewModel: WallpaperViewModel) {
    var currentTab by remember { mutableStateOf(WallpaperTab.Home) }
    val selectedWallpaperId by viewModel.selectedWallpaperId.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                // Hide standard bottom navigation bar when viewing a wallpaper preview edge-to-edge
                if (selectedWallpaperId == null) {
                    NavigationBar(
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("bottom_nav_bar"),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 4.dp
                    ) {
                        WallpaperTab.values().forEach { tab ->
                            val isSelected = currentTab == tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) tab.activeIcon else tab.inactiveIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("tab_btn_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            // Beautiful crossfade transitions between viewport tabs
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Crossfade(
                    targetState = currentTab,
                    modifier = Modifier.fillMaxSize(),
                    label = "tab_fade"
                ) { tab ->
                    when (tab) {
                        WallpaperTab.Home -> HomeScreen(
                            viewModel = viewModel,
                            onWallpaperSelected = { id -> viewModel.selectWallpaper(id) },
                            onCategorySelected = { category ->
                                viewModel.selectCategory(category)
                                currentTab = WallpaperTab.Categories
                            }
                        )
                        WallpaperTab.Categories -> CategoriesScreen(
                            viewModel = viewModel,
                            onWallpaperSelected = { id -> viewModel.selectWallpaper(id) }
                        )
                        WallpaperTab.Favorites -> FavoritesScreen(
                            viewModel = viewModel,
                            onWallpaperSelected = { id -> viewModel.selectWallpaper(id) }
                        )
                        WallpaperTab.Downloads -> DownloadsScreen(
                            viewModel = viewModel,
                            onWallpaperSelected = { id -> viewModel.selectWallpaper(id) }
                        )
                        WallpaperTab.Settings -> SettingsScreen()
                    }
                }
            }
        }

        // Animated full fidelity preview sliding from bottom
        AnimatedVisibility(
            visible = selectedWallpaperId != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            PreviewScreen(
                viewModel = viewModel,
                onBack = { viewModel.selectWallpaper(null) }
            )
        }
    }
}

enum class WallpaperTab(
    val label: String,
    val activeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val inactiveIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Home(
        label = "Home",
        activeIcon = Icons.Filled.Home,
        inactiveIcon = Icons.Outlined.Home
    ),
    Categories(
        label = "Categories",
        activeIcon = Icons.Filled.Category,
        inactiveIcon = Icons.Outlined.Category
    ),
    Favorites(
        label = "Favorites",
        activeIcon = Icons.Filled.Favorite,
        inactiveIcon = Icons.Outlined.FavoriteBorder
    ),
    Downloads(
        label = "Downloads",
        activeIcon = Icons.Filled.Download,
        inactiveIcon = Icons.Outlined.Download
    ),
    Settings(
        label = "Settings",
        activeIcon = Icons.Filled.Settings,
        inactiveIcon = Icons.Outlined.Settings
    )
}
