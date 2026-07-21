/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.YTItem
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.GridItemSize
import com.music.vivi.constants.GridItemsSizeKey
import com.music.vivi.constants.GridThumbnailHeight
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.component.GlassCircleButton
import com.music.vivi.ui.component.HeroBackground
import com.music.vivi.ui.component.LocalAppBackdrop
import com.music.vivi.ui.component.LocalGlassEffectConfig
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.YouTubeGridItem
import com.music.vivi.ui.component.backdrop.backdrops.rememberLayerBackdrop
import com.music.vivi.ui.component.isGlassAllowed
import com.music.vivi.ui.component.liquidGlass
import com.music.vivi.ui.component.rememberHeroSource
import com.music.vivi.ui.component.rememberHeroTint
import com.music.vivi.ui.component.shapes.ContinuousRoundedRectangle
import com.music.vivi.ui.menu.YouTubeAlbumMenu
import com.music.vivi.ui.menu.YouTubeArtistMenu
import com.music.vivi.ui.menu.YouTubePlaylistMenu
import com.music.vivi.ui.menu.YouTubeSongMenu
import com.music.vivi.ui.theme.AppleTokens
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.ui.utils.combinedBounceClick
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.viewmodels.YouTubeBrowseViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun YouTubeBrowseScreen(
    navController: NavController,
    viewModel: YouTubeBrowseViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val browseResult by viewModel.result.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    val allItems = browseResult?.items?.flatMap { it.items }.orEmpty()

    val heroUrl = allItems.firstOrNull()?.let {
        when (it) {
            is SongItem -> it.thumbnail
            is AlbumItem -> it.thumbnail
            is ArtistItem -> it.thumbnail
            is PlaylistItem -> it.thumbnail
            else -> null
        }
    }
    val heroSource = rememberHeroSource(
        staticArt = heroUrl,
        songs = emptyList()
    )
    val tint = rememberHeroTint(heroUrl)
    val onTint = AppleTokens.onColor(tint)

    val glassConfig = LocalGlassEffectConfig.current
    val useGlass = glassConfig.globalEnabled && isGlassAllowed()
    val heroBackdrop = rememberLayerBackdrop()

    HeroBackground(
        tint = tint,
        heroSource = heroSource,
        modifier = Modifier.fillMaxSize(),
    ) {
      CompositionLocalProvider(
          LocalAppBackdrop provides heroBackdrop,
          LocalContentColor provides onTint
      ) {
        val chromeShape = ContinuousRoundedRectangle(percent = 50)

        Box(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp),
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                modifier = Modifier.fillMaxSize()
            ) {
                browseResult?.let { result ->
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        Column {
                            Spacer(Modifier.height(40.dp))
                            Text(
                                text = result.title.orEmpty().lowercase(),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = onTint,
                                fontSize = 42.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                            )
                        }
                    }

                    items(
                        items = allItems,
                        key = {
                            when (it) {
                                is SongItem -> "song_${it.id}"
                                is AlbumItem -> "album_${it.id}"
                                is ArtistItem -> "artist_${it.id}"
                                is PlaylistItem -> "playlist_${it.id}"
                                else -> it.hashCode()
                            }
                        },
                    ) { item ->
                        YouTubeGridItem(
                            item = item,
                            isActive = when (item) {
                                is SongItem -> mediaMetadata?.id == item.id
                                is AlbumItem -> mediaMetadata?.album?.id == item.id
                                else -> false
                            },
                            isPlaying = isPlaying,
                            fillMaxWidth = true,
                            coroutineScope = coroutineScope,
                            modifier =
                            Modifier
                                .combinedBounceClick(
                                    onClick = {
                                        when (item) {
                                            is SongItem ->
                                                playerConnection.playQueue(
                                                    YouTubeQueue(
                                                        com.music.innertube.models.WatchEndpoint(videoId = item.id),
                                                        item.toMediaMetadata()
                                                    ),
                                                )

                                            is AlbumItem -> navController.navigate("album/${item.id}")
                                            is ArtistItem -> navController.navigate("artist/${item.id}")
                                            is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            when (item) {
                                                is SongItem ->
                                                    YouTubeSongMenu(
                                                        song = item,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )

                                                is AlbumItem ->
                                                    YouTubeAlbumMenu(
                                                        albumItem = item,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )

                                                is ArtistItem ->
                                                    YouTubeArtistMenu(
                                                        artist = item,
                                                        onDismiss = menuState::dismiss,
                                                    )

                                                is PlaylistItem ->
                                                    YouTubePlaylistMenu(
                                                        playlist = item,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                            }
                                        }
                                    },
                                ),
                        )
                    }
                }
            }

            // Top bar logic
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassCircleButton(
                    onClick = { navController.navigateUp() },
                    onLongClick = { navController.backToMain() },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                    )
                }

                Spacer(Modifier.weight(1f))
            }
        }
      }
    }
}
