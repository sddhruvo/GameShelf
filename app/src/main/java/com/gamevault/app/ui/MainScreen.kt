package com.gamevault.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gamevault.app.R
import com.gamevault.app.ui.home.HomeContent
import com.gamevault.app.ui.settings.SettingsScreen
import com.gamevault.app.ui.stats.StatsScreen
import com.gamevault.app.ui.theme.LocalGameVaultColors
import com.gamevault.app.ui.updates.UpdatesScreen
import kotlinx.coroutines.launch

data class TabItem(
    val label: String,
    val icon: @Composable () -> Unit
)

@Composable
fun MainScreen(
    onGameDetail: (String) -> Unit
) {
    val gvColors = LocalGameVaultColors.current
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()
    val selectedPage by remember { derivedStateOf { pagerState.currentPage } }

    val tabs = listOf(
        TabItem(stringResource(R.string.all_games)) { Icon(Icons.Filled.SportsEsports, contentDescription = null) },
        TabItem(stringResource(R.string.stats)) { Icon(Icons.Filled.BarChart, contentDescription = null) },
        TabItem("Updates") { Icon(Icons.Filled.NewReleases, contentDescription = null) },
        TabItem(stringResource(R.string.settings)) { Icon(Icons.Filled.Settings, contentDescription = null) }
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = if (gvColors.isGlass) gvColors.glassSurface
                else MaterialTheme.colorScheme.surfaceContainer
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        icon = tab.icon,
                        label = { Text(tab.label) },
                        selected = selectedPage == index,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            beyondViewportPageCount = 1,
            key = { it }
        ) { page ->
            when (page) {
                0 -> HomeContent(onGameDetail = onGameDetail)
                1 -> StatsScreen()
                2 -> UpdatesScreen(onGameDetail = onGameDetail)
                3 -> SettingsScreen()
            }
        }
    }
}
