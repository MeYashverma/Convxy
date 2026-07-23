/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.search

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import com.music.vivi.ui.utils.bounceClick
import com.music.vivi.ui.utils.combinedBounceClick

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.CONTENT_TYPE_LIST
import com.music.vivi.constants.ListItemHeight
import com.music.vivi.db.entities.Album
import com.music.vivi.db.entities.Artist
import com.music.vivi.db.entities.Playlist
import com.music.vivi.db.entities.Song
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.ui.component.AlbumListItem
import com.music.vivi.ui.component.ArtistListItem
import com.music.vivi.ui.component.ChipsRow
import com.music.vivi.ui.component.EmptyPlaceholder
import com.music.vivi.ui.component.HeroBackground
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.text.font.FontWeight
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.rememberHeroSource
import com.music.vivi.ui.component.rememberHeroTint
import com.music.vivi.ui.theme.AppleTokens
import com.music.vivi.ui.theme.LocalAccentTextColor
import com.music.vivi.ui.component.LocalAppBackdrop
import com.music.vivi.ui.component.LocalGlassEffectConfig
import com.music.vivi.ui.component.isGlassAllowed
import com.music.vivi.ui.component.liquidGlass
import com.music.vivi.ui.component.shapes.ContinuousRoundedRectangle
import com.music.vivi.ui.component.backdrop.backdrops.rememberLayerBackdrop
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import com.music.vivi.ui.component.PlaylistListItem
import com.music.vivi.ui.component.SongListItem
import com.music.vivi.ui.menu.SongMenu
import com.music.vivi.utils.listItemShape
import com.music.vivi.viewmodels.LocalFilter
import com.music.vivi.viewmodels.LocalSearchViewModel
import kotlinx.coroutines.flow.drop

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LocalSearchScreen(
    query: String,
    navController: NavController,
    onDismiss: () -> Unit,
    isFromCache: Boolean = false,
    pureBlack: Boolean,
    viewModel: LocalSearchViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val searchFilter by viewModel.filter.collectAsState()
    val result by viewModel.result.collectAsState()

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val lazyListState = rememberLazyListState()

    val heroUrl = result.map.values.flatten().firstOrNull()?.let {
        when (it) {
            is Song -> it.song.thumbnailUrl
            is Album -> it.album.thumbnailUrl
            is Artist -> it.artist.thumbnailUrl
            is Playlist -> it.thumbnails.firstOrNull()
            else -> null
        }
    }
    val heroSource = rememberHeroSource(
        staticArt = heroUrl,
        songs = result.map[LocalFilter.SONG]?.filterIsInstance<Song>()?.map { it.song.thumbnailUrl to false } ?: emptyList()
    )
    val tint = Color.Black
    val onTint = AppleTokens.onColor(tint)

    val glassConfig = LocalGlassEffectConfig.current
    val useGlass = glassConfig.globalEnabled && isGlassAllowed()
    val heroBackdrop = rememberLayerBackdrop()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tint),
    ) {
      CompositionLocalProvider(
          LocalAppBackdrop provides heroBackdrop,
          LocalContentColor provides onTint,
          LocalAccentTextColor provides AppleTokens.onColorHeading(tint)
      ) {
        val chromeShape = ContinuousRoundedRectangle(percent = 50)
        val chromeBackgroundModifier = if (useGlass) {
            Modifier.liquidGlass(config = glassConfig, shape = chromeShape, highlightAlpha = 0.3f)
        } else {
            Modifier.background(LocalContentColor.current.copy(alpha = 0.15f), chromeShape)
        }

        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Bottom)
                .asPaddingValues(),
            modifier = Modifier
                .fillMaxSize()
                .let { base ->
                    if (isLandscape) {
                        base.windowInsetsPadding(
                            WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
                        )
                    } else base
                }
        ) {
            item(key = "search_header") {
                Spacer(Modifier.height(40.dp))
                Text(
                    text = stringResource(R.string.search),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = onTint,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                )
            }

            stickyHeader {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(tint.copy(alpha = 0.8f))
                ) {
                    ChipsRow(
                        chips = listOf(
                            LocalFilter.ALL to stringResource(R.string.filter_all),
                            LocalFilter.SONG to stringResource(R.string.filter_songs),
                            LocalFilter.ALBUM to stringResource(R.string.filter_albums),
                            LocalFilter.ARTIST to stringResource(R.string.filter_artists),
                            LocalFilter.PLAYLIST to stringResource(R.string.filter_playlists),
                        ),
                        currentValue = searchFilter,
                        onValueUpdate = { viewModel.filter.value = it },
                    )
                }
            }

            result.map.forEach { (filter, items) ->
                if (result.filter == LocalFilter.ALL) {
                    item(key = filter) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ListItemHeight)
                                .bounceClick { viewModel.filter.value = filter }
                                .padding(start = 12.dp, end = 18.dp),
                        ) {
                            Text(
                                text = stringResource(
                                    when (filter) {
                                        LocalFilter.SONG -> R.string.filter_songs
                                        LocalFilter.ALBUM -> R.string.filter_albums
                                        LocalFilter.ARTIST -> R.string.filter_artists
                                        LocalFilter.PLAYLIST -> R.string.filter_playlists
                                        LocalFilter.ALL -> error("")
                                    }
                                ),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = onTint,
                                modifier = Modifier.weight(1f),
                            )

                            Icon(
                                painter = painterResource(R.drawable.navigate_next),
                                contentDescription = null,
                                tint = onTint
                            )
                        }
                    }
                }

            items(
                items = items.distinctBy { it.id },
                key = { it.id },
                contentType = { CONTENT_TYPE_LIST },
            ) { item ->
                when (item) {
                    is Song -> SongListItem(
                        song = item,
                        showInLibraryIcon = true,
                        isActive = item.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        flat = true,
                        shape = listItemShape(items.indexOfFirst { it.id == item.id }, items.size),
                        trailingContent = {
                            IconButton(
                                onClick = {
                                    menuState.show {
                                        SongMenu(
                                            originalSong = item,
                                            navController = navController,
                                            onDismiss = {
                                                onDismiss()
                                                menuState.dismiss()
                                            },
                                            isFromCache = isFromCache
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.more_vert),
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier
                            .combinedBounceClick(
                                onClick = {
                                    if (item.id == mediaMetadata?.id) {
                                        playerConnection.togglePlayPause()
                                    } else {
                                        val songs = result.map
                                            .getOrDefault(LocalFilter.SONG, emptyList())
                                            .filterIsInstance<Song>()
                                            .map { it.toMediaItem() }
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = context.getString(R.string.queue_searched_songs),
                                                items = songs,
                                                startIndex = songs.indexOfFirst { it.mediaId == item.id },
                                            )
                                        )
                                    }
                                },
                                onLongClick = {
                                    menuState.show {
                                        SongMenu(
                                            originalSong = item,
                                            navController = navController,
                                            onDismiss = {
                                                onDismiss()
                                                menuState.dismiss()
                                            },
                                            isFromCache = isFromCache
                                        )
                                    }
                                }
                            )
                            .animateItem(),
                    )

                    is Album -> AlbumListItem(
                        album = item,
                        isActive = item.id == mediaMetadata?.album?.id,
                        isPlaying = isPlaying,
                        flat = true,
                        modifier = Modifier
                            .bounceClick {
                                onDismiss()
                                navController.navigate("album/${item.id}")
                            }
                            .animateItem(),
                    )

                    is Artist -> ArtistListItem(
                        artist = item,
                        flat = true,
                        modifier = Modifier
                            .bounceClick {
                                onDismiss()
                                navController.navigate("artist/${item.id}")
                            }
                            .animateItem(),
                    )

                    is Playlist -> PlaylistListItem(
                        playlist = item,
                        flat = true,
                        modifier = Modifier
                            .bounceClick {
                                onDismiss()
                                navController.navigate("local_playlist/${item.id}")
                            }
                            .animateItem(),
                    )
                }
            }
            }

            if (result.query.isNotEmpty() && result.map.isEmpty()) {
                item(key = "no_result") {
                    EmptyPlaceholder(
                        icon = R.drawable.search,
                        text = stringResource(R.string.no_results_found),
                        modifier = Modifier.padding(top = 100.dp)
                    )
                }
            }
            item {
                Spacer(Modifier.height(100.dp))
            }
        }
      }
    }
}
