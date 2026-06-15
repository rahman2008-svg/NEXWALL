package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.WallpaperEntity
import com.example.ui.theme.StudioNeonMagenta
import com.example.ui.viewmodel.WallpaperViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: WallpaperViewModel,
    onWallpaperSelected: (String) -> Unit,
    onCategorySelected: (String) -> Unit
) {
    val wallpapers by viewModel.allWallpapers.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    val featuredWallpaper = remember(wallpapers) {
        wallpapers.find { it.id == "drk_1" } ?: wallpapers.firstOrNull()
    }

    val categories = listOf("Nature", "Abstract", "Anime", "Dark", "Minimal", "Tech")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // App Sticky Header with Brand Identity
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "NEXWALL",
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Studio X",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(
                onClick = { /* Search field focuses */ },
                modifier = Modifier.background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(22.dp)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Active Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("search_input"),
            placeholder = { Text("Search stunning wallpaper, tags...") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            singleLine = true
        )

        AnimatedVisibility(
            visible = searchQuery.isEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                // Category Tag Chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        Surface(
                            modifier = Modifier
                                .clickable { onCategorySelected(category) }
                                .testTag("cat_chip_$category"),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(3.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = category,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // Curated Hero Spotlight
                featuredWallpaper?.let { hero ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .clickable { onWallpaperSelected(hero.id) }
                            .testTag("hero_spotlight"),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(hero.thumbnailUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Hero Spotlight",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Aesthetic gradient overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                            startY = 100f
                                        )
                                    )
                            )
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        Text(
                                            text = "SPOTLIGHT OF THE DAY",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.Black,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = hero.title,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = "By ${hero.author}",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(22.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Preview Spotlight Wallpaper",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Main Pinterest-style Grid (Staggered Grid)
        val displayWalls = if (searchQuery.isNotEmpty()) searchResults else wallpapers

        if (displayWalls.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = "Empty search",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Wallpapers Found",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Try search words like Green, Nature, Matrix...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = if (searchQuery.isNotEmpty()) "Search Results" else "Explore Fresh Walls",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 12.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )

                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalItemSpacing = 10.dp,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(displayWalls, key = { it.id }) { wallpaper ->
                        WallpaperCard(
                            wallpaper = wallpaper,
                            onClick = { onWallpaperSelected(wallpaper.id) },
                            onFavoriteClick = { viewModel.toggleFavorite(wallpaper.id, wallpaper.isFavorite) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WallpaperCard(
    wallpaper: WallpaperEntity,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    // Generate heights dynamically based on ID signature to achieve Pinterest dynamic looks
    val heightDp = remember(wallpaper.id) {
        val hash = wallpaper.id.hashCode()
        when {
            hash % 3 == 0 -> 260.dp
            hash % 5 == 0 -> 290.dp
            else -> 190.dp
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp)
            .clickable { onClick() }
            .testTag("wall_card_${wallpaper.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(wallpaper.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = wallpaper.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dynamic Gradient Backsplash
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                            startY = 180f
                        )
                    )
            )

            // Absolute favorite icon tag
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(36.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                    .clickable { onFavoriteClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (wallpaper.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Toggle favorite",
                    tint = if (wallpaper.isFavorite) StudioNeonMagenta else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Bottom title metadata
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = wallpaper.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = wallpaper.category,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Views",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${wallpaper.views}",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}
