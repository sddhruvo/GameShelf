package com.gamevault.app.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gamevault.app.R
import com.gamevault.app.data.model.Game
import com.gamevault.app.service.InstalledApp
import com.gamevault.app.ui.components.GameCard
import com.gamevault.app.ui.components.GameSearchBar
import com.gamevault.app.ui.components.SortMenu
import com.gamevault.app.ui.theme.LocalGameShelfColors
import com.gamevault.app.util.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    onGameDetail: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val games by viewModel.filteredAndSortedGames.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteGames.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val todayPlaytime by viewModel.todayPlaytime.collectAsStateWithLifecycle()
    val donateDismissed by viewModel.donateDismissed.collectAsStateWithLifecycle()
    val firstLaunchTime by viewModel.firstLaunchTime.collectAsStateWithLifecycle()
    val gvColors = LocalGameShelfColors.current
    val homeContext = LocalContext.current

    val showDonateCard = !donateDismissed &&
            firstLaunchTime > 0L &&
            (System.currentTimeMillis() - firstLaunchTime) > 14 * 24 * 60 * 60 * 1000L // 2 weeks

    var showContextMenu by remember { mutableStateOf<Game?>(null) }
    var hideConfirmTarget by remember { mutableStateOf<Game?>(null) }
    var limitExceededPackage by remember { mutableStateOf<String?>(null) }
    var showAppPicker by remember { mutableStateOf(false) }

    // Observe daily limit exceeded events
    LaunchedEffect(Unit) {
        viewModel.limitExceededEvent.collect { pkg ->
            limitExceededPackage = pkg
        }
    }

    val viewModeIcon = when (viewMode) {
        ViewMode.GRID -> Icons.Outlined.GridView
        ViewMode.LIST -> Icons.Outlined.ViewList
        ViewMode.ICON -> Icons.Outlined.Apps
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold
                    )
                    if (todayPlaytime > 0) {
                        Text(
                            "Today: ${FormatUtils.formatPlaytime(todayPlaytime)}",
                            fontSize = 12.sp,
                            color = if (gvColors.isGlass) gvColors.accentGlow
                            else MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            },
            actions = {
                IconButton(onClick = { viewModel.cycleViewMode() }) {
                    Icon(viewModeIcon, contentDescription = "Toggle view")
                }
                SortMenu(currentSort = sortOption, onSortSelected = viewModel::setSortOption)
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            ),
            windowInsets = WindowInsets(0)
        )

        // One-time donate card after 2 weeks
        if (showDonateCard) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Enjoying GameShelf?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This app is built by a solo developer. If GameShelf has been useful to you, consider supporting its development!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { viewModel.dismissDonateCard() }) {
                            Text("Maybe later")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                homeContext.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://paypal.me/sddhruvo"))
                                )
                                viewModel.dismissDonateCard()
                            }
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Support")
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.scanGames() },
                modifier = Modifier.fillMaxSize()
            ) {
                if (games.isEmpty() && favorites.isEmpty()) {
                    EmptyState(
                        searchQuery = searchQuery,
                        viewModel = viewModel,
                        onAddGames = { showAppPicker = true }
                    )
                } else {
                    when (viewMode) {
                        ViewMode.GRID -> GridViewContent(
                            games = games,
                            favorites = favorites,
                            searchQuery = searchQuery,
                            viewModel = viewModel,
                            onShowContextMenu = { showContextMenu = it }
                        )
                        ViewMode.LIST -> ListViewContent(
                            games = games,
                            favorites = favorites,
                            searchQuery = searchQuery,
                            viewModel = viewModel,
                            onShowContextMenu = { showContextMenu = it }
                        )
                        ViewMode.ICON -> IconViewContent(
                            games = games,
                            favorites = favorites,
                            searchQuery = searchQuery,
                            viewModel = viewModel,
                            onShowContextMenu = { showContextMenu = it }
                        )
                    }
                }
            }

            // FAB to manually add games
            FloatingActionButton(
                onClick = { showAppPicker = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_games))
            }
        }
    }

    // App Picker Dialog
    if (showAppPicker) {
        val allGamesSnapshot = games
        val existingPackages = remember(allGamesSnapshot) {
            allGamesSnapshot.map { it.packageName }.toSet()
        }
        var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
        LaunchedEffect(Unit) {
            installedApps = withContext(Dispatchers.IO) {
                viewModel.getInstalledApps()
            }
        }
        AppPickerDialog(
            onDismiss = { showAppPicker = false },
            installedApps = installedApps,
            existingPackages = existingPackages,
            onConfirm = { toAdd, toRemove ->
                toAdd.forEach { viewModel.addManualGame(it) }
                toRemove.forEach { viewModel.removeManualGame(it) }
            }
        )
    }

    // Context menu dialog
    showContextMenu?.let { game ->
        AlertDialog(
            onDismissRequest = { showContextMenu = null },
            title = { Text(game.name) },
            text = {
                Column {
                    TextButton(onClick = {
                        onGameDetail(game.packageName)
                        showContextMenu = null
                    }) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Details")
                    }
                    TextButton(onClick = {
                        viewModel.toggleFavorite(game.packageName, game.isFavorite)
                        showContextMenu = null
                    }) {
                        Icon(
                            if (game.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (game.isFavorite) "Remove from Favorites" else "Add to Favorites")
                    }
                    TextButton(onClick = {
                        hideConfirmTarget = game
                        showContextMenu = null
                    }) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Hide Game")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showContextMenu = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Hide confirmation dialog
    hideConfirmTarget?.let { game ->
        AlertDialog(
            onDismissRequest = { hideConfirmTarget = null },
            title = { Text("Hide Game") },
            text = { Text("Hide \"${game.name}\" from your library? You can unhide it later in Settings.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.hideGame(game.packageName)
                        hideConfirmTarget = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Hide") }
            },
            dismissButton = {
                TextButton(onClick = { hideConfirmTarget = null }) { Text("Cancel") }
            }
        )
    }

    // Daily limit exceeded dialog
    limitExceededPackage?.let { pkg ->
        val gameName = games.find { it.packageName == pkg }?.name ?: pkg
        AlertDialog(
            onDismissRequest = { limitExceededPackage = null },
            title = { Text("Daily Limit Reached") },
            text = {
                Text("You've played ${FormatUtils.formatPlaytime(todayPlaytime)} today and reached your daily limit. Continue anyway?")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.forceLaunchGame(pkg)
                    limitExceededPackage = null
                }) { Text("Play Anyway") }
            },
            dismissButton = {
                TextButton(onClick = { limitExceededPackage = null }) { Text("Stop") }
            }
        )
    }
}

@Composable
private fun EmptyState(
    searchQuery: String,
    viewModel: HomeViewModel,
    onAddGames: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        GameSearchBar(
            query = searchQuery,
            onQueryChange = viewModel::setSearchQuery
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.SportsEsports,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (searchQuery.isNotBlank())
                        stringResource(R.string.no_games_found)
                    else
                        stringResource(R.string.no_games_installed),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (searchQuery.isBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onAddGames) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.add_games))
                    }
                }
            }
        }
    }
}

@Composable
private fun GridViewContent(
    games: List<Game>,
    favorites: List<Game>,
    searchQuery: String,
    viewModel: HomeViewModel,
    onShowContextMenu: (Game) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            GameSearchBar(
                query = searchQuery,
                onQueryChange = viewModel::setSearchQuery
            )
        }

        if (favorites.isNotEmpty() && searchQuery.isBlank()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.favorites),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            favorites.forEach { game ->
                item(key = "fav_${game.packageName}", span = { GridItemSpan(maxLineSpan) }) {
                    GameCard(
                        game = game,
                        viewMode = ViewMode.LIST,
                        onLaunch = { viewModel.launchGame(game.packageName) },
                        onLongPress = { onShowContextMenu(game) },
                        onFavoriteToggle = { viewModel.toggleFavorite(game.packageName, game.isFavorite) }
                    )
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "${stringResource(R.string.all_games)} (${games.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(games, key = { it.packageName }) { game ->
            GameCard(
                game = game,
                viewMode = ViewMode.GRID,
                onLaunch = { viewModel.launchGame(game.packageName) },
                onLongPress = { onShowContextMenu(game) },
                onFavoriteToggle = { viewModel.toggleFavorite(game.packageName, game.isFavorite) }
            )
        }
    }
}

@Composable
private fun ListViewContent(
    games: List<Game>,
    favorites: List<Game>,
    searchQuery: String,
    viewModel: HomeViewModel,
    onShowContextMenu: (Game) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            GameSearchBar(
                query = searchQuery,
                onQueryChange = viewModel::setSearchQuery
            )
        }

        if (favorites.isNotEmpty() && searchQuery.isBlank()) {
            item {
                Text(
                    text = stringResource(R.string.favorites),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            items(favorites, key = { "fav_${it.packageName}" }) { game ->
                GameCard(
                    game = game,
                    viewMode = ViewMode.LIST,
                    onLaunch = { viewModel.launchGame(game.packageName) },
                    onLongPress = { onShowContextMenu(game) },
                    onFavoriteToggle = { viewModel.toggleFavorite(game.packageName, game.isFavorite) }
                )
            }
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }

        item {
            Text(
                text = "${stringResource(R.string.all_games)} (${games.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(games, key = { it.packageName }) { game ->
            GameCard(
                game = game,
                viewMode = ViewMode.LIST,
                onLaunch = { viewModel.launchGame(game.packageName) },
                onLongPress = { onShowContextMenu(game) },
                onFavoriteToggle = { viewModel.toggleFavorite(game.packageName, game.isFavorite) }
            )
        }
    }
}

@Composable
private fun IconViewContent(
    games: List<Game>,
    favorites: List<Game>,
    searchQuery: String,
    viewModel: HomeViewModel,
    onShowContextMenu: (Game) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 76.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            GameSearchBar(
                query = searchQuery,
                onQueryChange = viewModel::setSearchQuery
            )
        }

        if (favorites.isNotEmpty() && searchQuery.isBlank()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.favorites),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            items(favorites, key = { "fav_${it.packageName}" }) { game ->
                GameCard(
                    game = game,
                    viewMode = ViewMode.ICON,
                    onLaunch = { viewModel.launchGame(game.packageName) },
                    onLongPress = { onShowContextMenu(game) },
                    onFavoriteToggle = { viewModel.toggleFavorite(game.packageName, game.isFavorite) }
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "${stringResource(R.string.all_games)} (${games.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        items(games, key = { it.packageName }) { game ->
            GameCard(
                game = game,
                viewMode = ViewMode.ICON,
                onLaunch = { viewModel.launchGame(game.packageName) },
                onLongPress = { onShowContextMenu(game) },
                onFavoriteToggle = { viewModel.toggleFavorite(game.packageName, game.isFavorite) }
            )
        }
    }
}
