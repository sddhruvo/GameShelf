package com.gamevault.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.gamevault.app.R

enum class SortOption {
    NAME, LAST_PLAYED, MOST_PLAYED, INSTALL_DATE, SIZE
}

@Composable
fun SortMenu(
    currentSort: SortOption,
    onSortSelected: (SortOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.sort_by))
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        SortOption.entries.forEach { option ->
            DropdownMenuItem(
                text = {
                    Text(
                        when (option) {
                            SortOption.NAME -> stringResource(R.string.sort_name)
                            SortOption.LAST_PLAYED -> stringResource(R.string.sort_last_played)
                            SortOption.MOST_PLAYED -> stringResource(R.string.sort_most_played)
                            SortOption.INSTALL_DATE -> stringResource(R.string.sort_install_date)
                            SortOption.SIZE -> stringResource(R.string.sort_size)
                        }
                    )
                },
                onClick = {
                    onSortSelected(option)
                    expanded = false
                },
                leadingIcon = {
                    if (option == currentSort) {
                        RadioButton(selected = true, onClick = null)
                    } else {
                        RadioButton(selected = false, onClick = null)
                    }
                }
            )
        }
    }
}
