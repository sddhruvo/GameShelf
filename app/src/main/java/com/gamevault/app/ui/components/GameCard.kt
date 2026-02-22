package com.gamevault.app.ui.components

import android.content.pm.PackageManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import com.gamevault.app.data.model.Game
import com.gamevault.app.ui.home.ViewMode
import com.gamevault.app.ui.theme.LocalGameShelfColors
import com.gamevault.app.util.FormatUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameCard(
    game: Game,
    viewMode: ViewMode,
    onLaunch: () -> Unit,
    onLongPress: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val iconBitmap: ImageBitmap? = remember(game.packageName) {
        try {
            context.packageManager
                .getApplicationIcon(game.packageName)
                .toBitmap(128, 128)
                .asImageBitmap()
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    when (viewMode) {
        ViewMode.GRID -> GameGridCard(game, iconBitmap, onLaunch, onLongPress, onFavoriteToggle, modifier)
        ViewMode.LIST -> GameListCard(game, iconBitmap, onLaunch, onLongPress, onFavoriteToggle, modifier)
        ViewMode.ICON -> GameIconCard(game, iconBitmap, onLaunch, onLongPress, modifier)
    }
}

@Composable
private fun glassModifier(shape: RoundedCornerShape): Modifier {
    val gvColors = LocalGameShelfColors.current
    return if (gvColors.isGlass) {
        Modifier
            .background(gvColors.glassSurface, shape)
            .border(1.dp, gvColors.glassBorder, shape)
    } else {
        Modifier
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GameGridCard(
    game: Game,
    iconBitmap: ImageBitmap?,
    onLaunch: () -> Unit,
    onLongPress: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gvColors = LocalGameShelfColors.current
    val shape = RoundedCornerShape(16.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .then(glassModifier(shape))
            .combinedClickable(
                onClick = onLaunch,
                onLongClick = onLongPress
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (gvColors.isGlass) Color.Transparent
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (gvColors.isGlass) 0.dp else 2.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box {
                    if (game.customCoverUri != null) {
                        AsyncImage(
                            model = game.customCoverUri,
                            contentDescription = game.name,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else if (iconBitmap != null) {
                        Image(
                            bitmap = iconBitmap,
                            contentDescription = game.name,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    if (game.isNew) {
                        Badge(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp),
                            containerColor = MaterialTheme.colorScheme.secondary
                        ) {
                            Text("NEW", fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = game.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Thin playtime bar at the bottom of the card
            if (game.totalPlaytimeMs > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(20.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                            ),
                            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = FormatUtils.formatPlaytime(game.totalPlaytimeMs),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f),
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GameListCard(
    game: Game,
    iconBitmap: ImageBitmap?,
    onLaunch: () -> Unit,
    onLongPress: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gvColors = LocalGameShelfColors.current
    val shape = RoundedCornerShape(12.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(glassModifier(shape))
            .combinedClickable(
                onClick = onLaunch,
                onLongClick = onLongPress
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (gvColors.isGlass) Color.Transparent
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (gvColors.isGlass) 0.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (game.customCoverUri != null) {
                AsyncImage(
                    model = game.customCoverUri,
                    contentDescription = game.name,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = game.name,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = game.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (game.isNew) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                            Text("NEW", fontSize = 8.sp)
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (game.totalPlaytimeMs > 0) {
                        Text(
                            text = FormatUtils.formatPlaytime(game.totalPlaytimeMs),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (gvColors.isGlass) gvColors.accentGlow
                            else MaterialTheme.colorScheme.secondary
                        )
                    }
                    if (game.lastPlayed > 0) {
                        Text(
                            text = FormatUtils.formatRelativeTime(game.lastPlayed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (game.rating > 0) {
                    Row {
                        repeat(5) { index ->
                            Icon(
                                imageVector = if (index < game.rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (index < game.rating) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            IconButton(onClick = onFavoriteToggle) {
                val color by animateColorAsState(
                    if (game.isFavorite) {
                        if (gvColors.isGlass) gvColors.neon else Color(0xFFFF6B6B)
                    } else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "favorite"
                )
                Icon(
                    imageVector = if (game.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favorite",
                    tint = color
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GameIconCard(
    game: Game,
    iconBitmap: ImageBitmap?,
    onLaunch: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gvColors = LocalGameShelfColors.current

    Column(
        modifier = modifier
            .width(72.dp)
            .combinedClickable(
                onClick = onLaunch,
                onLongClick = onLongPress
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            if (game.customCoverUri != null) {
                AsyncImage(
                    model = game.customCoverUri,
                    contentDescription = game.name,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .then(
                            if (gvColors.isGlass) Modifier.border(
                                1.dp, gvColors.glassBorder, RoundedCornerShape(14.dp)
                            ) else Modifier
                        ),
                    contentScale = ContentScale.Crop
                )
            } else if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = game.name,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .then(
                            if (gvColors.isGlass) Modifier.border(
                                1.dp, gvColors.glassBorder, RoundedCornerShape(14.dp)
                            ) else Modifier
                        ),
                    contentScale = ContentScale.Crop
                )
            }

            if (game.isNew) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(8.dp)
                        .background(
                            MaterialTheme.colorScheme.secondary,
                            RoundedCornerShape(50)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = game.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 12.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
