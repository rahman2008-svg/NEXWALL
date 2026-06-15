package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.WallpaperEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.WallpaperViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    viewModel: WallpaperViewModel,
    onBack: () -> Unit
) {
    val wallpaper by viewModel.selectedWallpaper.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    var showApplyDialog by remember { mutableStateOf(false) }

    // Intercept standard back action to clear selected wallpaper state cleanly
    BackHandler {
        onBack()
    }

    // React to action statuses (Toasts/Modals)
    LaunchedEffect(actionState) {
        when (actionState) {
            is WallpaperViewModel.ActionState.Success -> {
                Toast.makeText(context, (actionState as WallpaperViewModel.ActionState.Success).message, Toast.LENGTH_SHORT).show()
                viewModel.resetActionState()
            }
            is WallpaperViewModel.ActionState.Error -> {
                Toast.makeText(context, (actionState as WallpaperViewModel.ActionState.Error).message, Toast.LENGTH_LONG).show()
                viewModel.resetActionState()
            }
            else -> {}
        }
    }

    wallpaper?.let { wall ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Immersive edge-to-edge wallpaper viewport
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 4f)
                            if (scale > 1f) {
                                offset = Offset(
                                    x = offset.x + pan.x,
                                    y = offset.y + pan.y
                                )
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    }
                    .testTag("interactive_preview")
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(wall.url)
                        .crossfade(true)
                        .build(),
                    contentDescription = wall.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                )

                // Immersive high-contrast top & bottom gradient drops
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.65f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )
            }

            // Top action bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .testTag("preview_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = wall.category.uppercase(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Bottom dashboard overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Info block
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = wall.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Curated by ${wall.author}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Controls Layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Favorite Toggle
                    PreviewCircleButton(
                        imageVector = if (wall.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        tint = if (wall.isFavorite) StudioNeonMagenta else Color.White,
                        label = "Favorite",
                        onClick = { viewModel.toggleFavorite(wall.id, wall.isFavorite) },
                        testTag = "preview_fave_btn"
                    )

                    // Core CTA Set Wallpaper Button
                    Button(
                        onClick = { showApplyDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("apply_wallpaper_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wallpaper,
                            contentDescription = "Set wall icon",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "APPLY WALL",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimary,
                            letterSpacing = 1.sp
                        )
                    }

                    // Download Button
                    PreviewCircleButton(
                        imageVector = Icons.Default.Download,
                        tint = Color.White,
                        label = "Save",
                        onClick = { viewModel.downloadWallpaper(wall) },
                        testTag = "preview_download_btn"
                    )
                }

                // Double tap gesture helper description hint
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Pinch or drag screen to inspect full resolution",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.4f),
                    letterSpacing = 0.5.sp
                )
            }

            // Universal progress indicator
            if (actionState is WallpaperViewModel.ActionState.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = (actionState as WallpaperViewModel.ActionState.Loading).message,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Action options dialog (Dialog Modal layout)
            if (showApplyDialog) {
                AlertDialog(
                    onDismissRequest = { showApplyDialog = false },
                    title = {
                        Text(
                            text = "Configure Wallpaper Target",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Choose which display screen you would like to apply this HD artwork to.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            // Home target
                            DialogTargetRow(
                                title = "Home Screen",
                                icon = Icons.Default.Home,
                                onClick = {
                                    showApplyDialog = false
                                    viewModel.setWallpaper(wall, 1)
                                },
                                testTag = "target_home_btn"
                            )
                            // Lock target
                            DialogTargetRow(
                                title = "Lock Screen",
                                icon = Icons.Default.Lock,
                                onClick = {
                                    showApplyDialog = false
                                    viewModel.setWallpaper(wall, 2)
                                },
                                testTag = "target_lock_btn"
                            )
                            // Both target
                            DialogTargetRow(
                                title = "Both Displays",
                                icon = Icons.Default.AllInclusive,
                                onClick = {
                                    showApplyDialog = false
                                    viewModel.setWallpaper(wall, 3)
                                },
                                testTag = "target_both_btn"
                            )
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showApplyDialog = false }) {
                            Text("Cancel", color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(8.dp),
                    properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = true),
                    icon = {
                        Icon(imageVector = Icons.Default.SystemUpdateAlt, contentDescription = "System target set", tint = MaterialTheme.colorScheme.primary)
                    }
                )
            }
        }
    }
}

@Composable
fun DialogTargetRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Apply",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PreviewCircleButton(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .clickable { onClick() }
                .testTag(testTag),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}


