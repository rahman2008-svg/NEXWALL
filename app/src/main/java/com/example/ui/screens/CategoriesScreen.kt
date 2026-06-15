package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.ui.theme.StudioNeonElectricBlue
import com.example.ui.theme.StudioNeonMagenta
import com.example.ui.viewmodel.WallpaperViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: WallpaperViewModel,
    onWallpaperSelected: (String) -> Unit
) {
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categoryWallpapers by viewModel.categoryWallpapers.collectAsState()

    // Handle standard Android back button to exit category detail view safely
    BackHandler(enabled = selectedCategory != null) {
        viewModel.selectCategory(null)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = selectedCategory ?: "Categories",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    if (selectedCategory != null) {
                        IconButton(onClick = { viewModel.selectCategory(null) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to categories"
                            )
                        }
                    } else {
                        IconButton(onClick = {}, enabled = false) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = "Categories list",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedCategory == null) {
                // Show grid of available categories
                val categoryCards = listOf(
                    CategoryItem("Nature", "Green landscapes & mist peaks", "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=600&q=80", StudioNeonMagenta),
                    CategoryItem("Abstract", "Digital flows & 3D canvas art", "https://images.unsplash.com/photo-1541701494587-cb58502866ab?auto=format&fit=crop&w=600&q=80", StudioNeonElectricBlue),
                    CategoryItem("Anime", "Japan nights, skylines & pagodas", "https://images.unsplash.com/photo-1540959733332-eab4deceeaf7?auto=format&fit=crop&w=600&q=80", Color.Yellow),
                    CategoryItem("Dark", "Charcoal blocks & cosmic space", "https://images.unsplash.com/photo-1538370965046-79c0d6907d47?auto=format&fit=crop&w=600&q=80", Color.White),
                    CategoryItem("Minimal", "Uncluttered lines & pale architectures", "https://images.unsplash.com/photo-1473448912268-2022ce9509d8?auto=format&fit=crop&w=600&q=80", Color.LightGray),
                    CategoryItem("Tech", "Supercomputers & neon fiber wires", "https://images.unsplash.com/photo-1601987177651-8edfe6c20009?auto=format&fit=crop&w=600&q=80", StudioNeonElectricBlue)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(categoryCards) { categoryItem ->
                        CategoryCard(
                            item = categoryItem,
                            onClick = { viewModel.selectCategory(categoryItem.name) }
                        )
                    }
                }
            } else {
                // Category detail staggered view
                if (categoryWallpapers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp)
                    ) {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalItemSpacing = 10.dp,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(categoryWallpapers, key = { it.id }) { wallpaper ->
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
    }
}

data class CategoryItem(
    val name: String,
    val description: String,
    val imageUrl: String,
    val brandColor: Color
)

@Composable
fun CategoryCard(
    item: CategoryItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable { onClick() }
            .testTag("category_card_${item.name}"),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Linear Gradient overlay with slight brand coloring
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Black.copy(alpha = 0.45f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(item.brandColor, RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.name.uppercase(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.description,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(item.brandColor.copy(alpha = 0.2f), RoundedCornerShape(22.dp))
                        .clip(RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Explore category",
                        tint = item.brandColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
