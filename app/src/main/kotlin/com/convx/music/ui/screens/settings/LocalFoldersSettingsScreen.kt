/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import com.convx.music.ui.component.GlassSwitchCompat as Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.convx.music.LocalPlayerAwareWindowInsets
import com.convx.music.R
import com.convx.music.constants.LocalExcludedFoldersKey
import com.convx.music.ui.screens.library.LocalMusicViewModel
import com.convx.music.utils.LocalFolderIndex
import com.convx.music.utils.dataStore
import com.convx.music.utils.decodeExcludedFolders
import com.convx.music.utils.encodeExcludedFolders
import com.convx.music.utils.rememberPreference
import kotlinx.coroutines.launch

/**
 * Which on-device folders local-only mode is allowed to scan. Backed by a single
 * excluded-paths preference (see LocalExcludedFoldersKey) — toggling a folder off
 * triggers an immediate rescan so it actually disappears, not just stops growing.
 */
@Composable
fun LocalFoldersSettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val viewModel: LocalMusicViewModel = hiltViewModel()
    val coroutineScope = rememberCoroutineScope()

    var folders by remember { mutableStateOf<List<LocalFolderIndex.Folder>>(emptyList()) }
    LaunchedEffect(Unit) {
        folders = LocalFolderIndex.load(context)
    }

    val (excludedRaw, _) = rememberPreference(LocalExcludedFoldersKey, defaultValue = "")
    val excluded = remember(excludedRaw) { decodeExcludedFolders(excludedRaw) }

    Column(
        Modifier
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
            )
        )

        Text(
            text = stringResource(R.string.excluded_folders_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp, top = 16.dp)
        )

        if (folders.isEmpty()) {
            Text(
                text = stringResource(R.string.excluded_folders_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            folders.forEachIndexed { index, folder ->
                val included = folder.path !in excluded
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val next = if (included) excluded + folder.path else excluded - folder.path
                            coroutineScope.launch {
                                // Write and await before rescanning — scanDevice() reads this same
                                // key straight off disk, and racing it against the old fire-and-forget
                                // write meant the rescan could still see the pre-toggle excluded set
                                // and never actually drop the folder's songs. The switch looked like
                                // it flipped; the folder didn't actually leave the library.
                                context.dataStore.edit { it[LocalExcludedFoldersKey] = encodeExcludedFolders(next) }
                                viewModel.scanDevice(context)
                            }
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState())
                            .padding(end = 12.dp),
                    )
                    Switch(checked = included, onCheckedChange = null)
                }
                if (index != folders.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}
