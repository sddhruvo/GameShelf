package com.gamevault.app.ui.updates

import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gamevault.app.data.model.Game
import com.gamevault.app.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesScreen(
    onGameDetail: (String) -> Unit,
    viewModel: UpdatesViewModel = hiltViewModel()
) {
    val feedItems by viewModel.feedItems.collectAsStateWithLifecycle()
    val newGames by viewModel.newGames.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalUpdateCount.collectAsStateWithLifecycle()
    val weekCount by viewModel.thisWeekCount.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var editingNoteId by remember { mutableStateOf<Long?>(null) }
    var editingNoteText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Updates", fontWeight = FontWeight.Bold) },
            actions = {
                IconButton(onClick = { viewModel.refreshGames() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Check for updates")
                }
            },
            windowInsets = WindowInsets(0)
        )

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshGames() },
            modifier = Modifier.fillMaxSize()
        ) {
            if (feedItems.isEmpty() && newGames.isEmpty()) {
                EmptyUpdatesState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Stats summary
                    if (totalCount > 0) {
                        item(key = "stats") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatChip(
                                    label = "This Week",
                                    value = "$weekCount",
                                    icon = Icons.Default.DateRange,
                                    modifier = Modifier.weight(1f)
                                )
                                StatChip(
                                    label = "All Time",
                                    value = "$totalCount",
                                    icon = Icons.Default.Update,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // New games section
                    if (newGames.isNotEmpty()) {
                        item(key = "new_header") {
                            Text(
                                "Newly Installed",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        item(key = "new_games") {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(newGames, key = { "new_${it.packageName}" }) { game ->
                                    NewGameChip(
                                        game = game,
                                        onClick = {
                                            viewModel.markGameSeen(game.packageName)
                                            onGameDetail(game.packageName)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Update feed
                    if (feedItems.isNotEmpty()) {
                        item(key = "feed_header") {
                            Text(
                                "Recent Updates",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(feedItems, key = { it.update.id }) { item ->
                            UpdateCard(
                                item = item,
                                onOpenPlayStore = { viewModel.openPlayStore(item.update.packageName) },
                                onSearchNews = { viewModel.searchGameNews(item.gameName) },
                                onTapGame = { onGameDetail(item.update.packageName) },
                                onEditNote = {
                                    editingNoteId = item.update.id
                                    editingNoteText = item.update.changelogNotes
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Edit note dialog
    editingNoteId?.let { id ->
        AlertDialog(
            onDismissRequest = { editingNoteId = null },
            title = { Text("Changelog Notes") },
            text = {
                OutlinedTextField(
                    value = editingNoteText,
                    onValueChange = { editingNoteText = it },
                    label = { Text("What changed in this update?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.saveNote(id, editingNoteText.trim())
                    editingNoteId = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingNoteId = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun EmptyUpdatesState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Update,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No updates yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Game updates will appear here when detected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun NewGameChip(
    game: Game,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val iconBitmap: ImageBitmap? = remember(game.packageName) {
        try {
            context.packageManager
                .getApplicationIcon(game.packageName)
                .toBitmap(64, 64)
                .asImageBitmap()
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = game.name,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    game.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "NEW",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun UpdateCard(
    item: UpdateFeedItem,
    onOpenPlayStore: () -> Unit,
    onSearchNews: () -> Unit,
    onTapGame: () -> Unit,
    onEditNote: () -> Unit
) {
    val context = LocalContext.current
    val iconBitmap: ImageBitmap? = remember(item.update.packageName) {
        try {
            context.packageManager
                .getApplicationIcon(item.update.packageName)
                .toBitmap(96, 96)
                .asImageBitmap()
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    val sizeDelta = item.update.newSizeBytes - item.update.oldSizeBytes
    val sizeDeltaText = when {
        sizeDelta > 0 -> "+${FormatUtils.formatSize(sizeDelta)}"
        sizeDelta < 0 -> "-${FormatUtils.formatSize(-sizeDelta)}"
        else -> null
    }
    val sizeDeltaColor = when {
        sizeDelta > 0 -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
        sizeDelta < 0 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header row: icon + name + time
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (iconBitmap != null) {
                    Card(
                        onClick = onTapGame,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Image(
                            bitmap = iconBitmap,
                            contentDescription = item.gameName,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.gameName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        FormatUtils.formatRelativeTime(item.update.updateTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Version + size row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.SystemUpdateAlt,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "${item.update.oldVersion}  →  ${item.update.newVersion}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            if (item.update.oldSizeBytes > 0 && item.update.newSizeBytes > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Storage,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        FormatUtils.formatSize(item.update.newSizeBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (sizeDeltaText != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "(${sizeDeltaText})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = sizeDeltaColor
                        )
                    }
                }
            }

            // Changelog notes (if any)
            if (item.update.changelogNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    onClick = onEditNote
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Notes,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            item.update.changelogNotes,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Action buttons
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = onOpenPlayStore,
                    label = { Text("What's New", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Shop,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(20.dp)
                )
                AssistChip(
                    onClick = onSearchNews,
                    label = { Text("News", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.TravelExplore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(20.dp)
                )
                AssistChip(
                    onClick = onEditNote,
                    label = {
                        Text(
                            if (item.update.changelogNotes.isBlank()) "Add Note" else "Edit Note",
                            fontSize = 11.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }
    }
}
