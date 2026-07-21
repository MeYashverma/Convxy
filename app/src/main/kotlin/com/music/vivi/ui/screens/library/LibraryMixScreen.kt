/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.AlbumViewTypeKey
import com.music.vivi.constants.CONTENT_TYPE_HEADER
import com.music.vivi.constants.CONTENT_TYPE_PLAYLIST
import com.music.vivi.constants.GridItemSize
import com.music.vivi.constants.GridItemsSizeKey
import com.music.vivi.constants.GridThumbnailHeight
import com.music.vivi.constants.LibraryViewType
import com.music.vivi.constants.MixSortDescendingKey
import com.music.vivi.constants.MixSortType
import com.music.vivi.constants.MixSortTypeKey
import com.music.vivi.constants.ShowCachedPlaylistKey
import com.music.vivi.constants.ShowDownloadedPlaylistKey
import com.music.vivi.constants.ShowLikedPlaylistKey
import com.music.vivi.constants.ShowLocalPlaylistKey
import com.music.vivi.constants.ShowTopPlaylistKey
import com.music.vivi.constants.ShowUploadedPlaylistKey
import com.music.vivi.constants.YtmSyncKey
import com.music.vivi.db.entities.Album
import com.music.vivi.db.entities.Artist
import com.music.vivi.db.entities.Playlist
import com.music.vivi.db.entities.PlaylistEntity
import com.music.vivi.extensions.reversed
import com.music.vivi.ui.component.AlbumGridItem
import com.music.vivi.ui.component.AlbumListItem
import com.music.vivi.ui.component.ArtistGridItem
import com.music.vivi.ui.component.ArtistListItem
import com.music.vivi.ui.component.HeroBackground
import com.music.vivi.ui.component.LocalAppBackdrop
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.PlaylistGridItem
import com.music.vivi.ui.component.CreatePlaylistDialog
import com.music.vivi.ui.component.HideOnScrollFAB
import com.music.vivi.ui.component.PlaylistListItem
import com.music.vivi.ui.component.SortHeader
import com.music.vivi.ui.component.backdrop.backdrops.rememberLayerBackdrop
import com.music.vivi.ui.component.rememberHeroSource
import com.music.vivi.ui.component.rememberHeroTint
import com.music.vivi.ui.menu.AlbumMenu
import com.music.vivi.ui.menu.ArtistMenu
import com.music.vivi.ui.menu.PlaylistMenu
import com.music.vivi.ui.theme.AppleTokens
import com.music.vivi.ui.utils.bounceClick
import com.music.vivi.ui.utils.combinedBounceClick
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.LibraryMixViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID
import androidx.compose.ui.platform.LocalLocale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryMixScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    viewModel: LibraryMixViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    var viewType by rememberEnumPreference(AlbumViewTypeKey, LibraryViewType.GRID)
    val (sortType, onSortTypeChange) = rememberEnumPreference(
        MixSortTypeKey,
        MixSortType.CREATE_DATE
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(MixSortDescendingKey, true)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    val topSize by viewModel.topValue.collectAsState(initial = 50)
    
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    val heroUrl = (albums + artists + playlists).firstOrNull()?.let {
        when (it) {
            is Album -> it.album.thumbnailUrl
            is Artist -> it.artist.thumbnailUrl
            is Playlist -> it.thumbnails.firstOrNull()
            else -> null
        }
    }
    val heroSource = rememberHeroSource(staticArt = heroUrl)
    val tint = rememberHeroTint(heroUrl)
    val onTint = AppleTokens.onColor(tint)
    val heroBackdrop = rememberLayerBackdrop()

    val likedPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.liked)
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val downloadPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.offline)
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val topPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.my_top) + " $topSize"
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val cachePlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.cached_playlist)
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val uploadedPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.uploaded_playlist)
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val localPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.filter_local)
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val (showLiked) = rememberPreference(ShowLikedPlaylistKey, true)
    val (showDownloaded) = rememberPreference(ShowDownloadedPlaylistKey, true)
    val (showTop) = rememberPreference(ShowTopPlaylistKey, true)
    val (showCached) = rememberPreference(ShowCachedPlaylistKey, true)
    val (showUploaded) = rememberPreference(ShowUploadedPlaylistKey, true)
    val (showLocal) = rememberPreference(ShowLocalPlaylistKey, true)

    var allItems = albums + artists + playlists
    val collator = Collator.getInstance(LocalLocale.current.platformLocale)
    collator.strength = Collator.PRIMARY
    allItems =
        when (sortType) {
            MixSortType.CREATE_DATE ->
                allItems.sortedBy { item ->
                    when (item) {
                        is Album -> item.album.bookmarkedAt
                        is Artist -> item.artist.bookmarkedAt
                        is Playlist -> item.playlist.createdAt
                        else -> LocalDateTime.now()
                    }
                }

            MixSortType.NAME ->
                allItems.sortedWith(
                    compareBy(collator) { item ->
                        when (item) {
                            is Album -> item.album.title
                            is Artist -> item.artist.name
                            is Playlist -> item.playlist.name
                            else -> ""
                        }
                    },
                )

            MixSortType.LAST_UPDATED ->
                allItems.sortedBy { item ->
                    when (item) {
                        is Album -> item.album.lastUpdateTime
                        is Artist -> item.artist.lastUpdateTime
                        is Playlist -> item.playlist.lastUpdateTime
                        else -> LocalDateTime.now()
                    }
                }
        }.reversed(sortDescending)

    val coroutineScope = rememberCoroutineScope()

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onPlaylistCreated = { playlistId ->
                showCreatePlaylistDialog = false
                navController.navigate("local_playlist/$playlistId")
            }
        )
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            when (viewType) {
                LibraryViewType.LIST -> lazyListState.animateScrollToItem(0)
                LibraryViewType.GRID -> lazyGridState.animateScrollToItem(0)
            }
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LaunchedEffect(Unit) {
         if (ytmSync) {
             withContext(Dispatchers.IO) {
                 viewModel.syncAllLibrary()
             }
         }
    }

    val headerContent = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp),
        ) {
            SortHeader(
                sortType = sortType,
                sortDescending = sortDescending,
                onSortTypeChange = onSortTypeChange,
                onSortDescendingChange = onSortDescendingChange,
                sortTypeText = { sortType ->
                    when (sortType) {
                        MixSortType.CREATE_DATE -> R.string.sort_by_create_date
                        MixSortType.LAST_UPDATED -> R.string.sort_by_last_updated
                        MixSortType.NAME -> R.string.sort_by_name
                    }
                },
            )

            Spacer(Modifier.weight(1f))

            FlowRow(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                LibraryViewType.entries.forEachIndexed { index, type ->
                    ToggleButton(
                        checked = viewType == type,
                        onCheckedChange = { viewType = type },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            LibraryViewType.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        modifier = Modifier.semantics { role = Role.RadioButton },
                    ) {
                        Icon(
                            painter = painterResource(
                                when (type) {
                                    LibraryViewType.LIST -> R.drawable.list
                                    LibraryViewType.GRID -> R.drawable.grid_view
                                }
                            ),
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    HeroBackground(
        tint = tint,
        heroSource = heroSource,
        modifier = Modifier.fillMaxSize(),
    ) {
      CompositionLocalProvider(
          LocalAppBackdrop provides heroBackdrop,
          LocalContentColor provides onTint
      ) {
        PullToRefreshBox(
            state = pullRefreshState,
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        ) {
            when (viewType) {
                LibraryViewType.LIST ->
                    LazyColumn(
                        state = lazyListState,
                        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                    ) {
                        item(key = "header_title") {
                            Column {
                                Spacer(Modifier.height(40.dp))
                                Text(
                                    text = stringResource(R.string.filter_library).lowercase(),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = onTint,
                                    fontSize = 42.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                                )
                            }
                        }

                        item(
                            key = "filter",
                            contentType = CONTENT_TYPE_HEADER,
                        ) {
                            filterContent()
                        }

                        item(
                            key = "header",
                            contentType = CONTENT_TYPE_HEADER,
                        ) {
                            headerContent()
                        }

                        if (showLiked) {
                            item(
                                key = "likedPlaylist",
                                contentType = { CONTENT_TYPE_PLAYLIST },
                            ) {
                                PlaylistListItem(
                                    playlist = likedPlaylist,
                                    autoPlaylist = true,
                                    flat = true,
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .bounceClick {
                                            navController.navigate("auto_playlist/liked")
                                        }
                                        .animateItem(),
                                )
                            }
                        }

                        if (showDownloaded) {
                            item(
                                key = "downloadedPlaylist",
                                contentType = { CONTENT_TYPE_PLAYLIST },
                            ) {
                                PlaylistListItem(
                                    playlist = downloadPlaylist,
                                    autoPlaylist = true,
                                    flat = true,
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .bounceClick {
                                            navController.navigate("auto_playlist/downloaded")
                                        }
                                        .animateItem(),
                                )
                            }
                        }

                        if (showTop) {
                            item(
                                key = "TopPlaylist",
                                contentType = { CONTENT_TYPE_PLAYLIST },
                            ) {
                                PlaylistListItem(
                                    playlist = topPlaylist,
                                    autoPlaylist = true,
                                    flat = true,
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .bounceClick {
                                            navController.navigate("top_playlist/$topSize")
                                        }
                                        .animateItem(),
                                )
                            }
                        }

                        if (showCached) {
                            item(
                                key = "cachePlaylist",
                                contentType = { CONTENT_TYPE_PLAYLIST },
                            ) {
                                PlaylistListItem(
                                    playlist = cachePlaylist,
                                    autoPlaylist = true,
                                    flat = true,
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .bounceClick {
                                            navController.navigate("cache_playlist/cached")
                                        }
                                        .animateItem(),
                            )
                        }
                    }

                    if (showUploaded) {
                        item(
                            key = "uploadedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistListItem(
                                playlist = uploadedPlaylist,
                                autoPlaylist = true,
                                flat = true,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .bounceClick {
                                            navController.navigate("auto_playlist/uploaded")
                                        }
                                        .animateItem(),
                            )
                        }
                    }

                    if (showLocal) {
                        item(
                            key = "localPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistListItem(
                                playlist = localPlaylist,
                                autoPlaylist = true,
                                flat = true,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .bounceClick {
                                            navController.navigate("auto_playlist/local")
                                        }
                                        .animateItem(),
                            )
                        }
                    }

                    items(
                        items = allItems.distinctBy { it.id },
                        key = { it.id },
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) { item ->
                        when (item) {
                            is Playlist -> {
                                PlaylistListItem(
                                    playlist = item,
                                    flat = true,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    PlaylistMenu(
                                                        playlist = item,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = null,
                                            )
                                        }
                                    },
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .combinedBounceClick(
                                            onClick = {
                                                navController.navigate("local_playlist/${item.id}")
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    PlaylistMenu(
                                                        playlist = item,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                        .animateItem(),
                                )
                            }

                            is Artist -> {
                                ArtistListItem(
                                    artist = item,
                                    flat = true,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    ArtistMenu(
                                                        originalArtist = item,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = null,
                                            )
                                        }
                                    },
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .combinedBounceClick(
                                            onClick = {
                                                navController.navigate("artist/${item.id}")
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    ArtistMenu(
                                                        originalArtist = item,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                        .animateItem(),
                                )
                            }

                            is Album -> {
                                AlbumListItem(
                                    album = item,
                                    isActive = item.id == mediaMetadata?.album?.id,
                                    isPlaying = isPlaying,
                                    flat = true,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    AlbumMenu(
                                                        originalAlbum = item,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = null,
                                            )
                                        }
                                    },
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .combinedBounceClick(
                                            onClick = {
                                                navController.navigate("album/${item.id}")
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    AlbumMenu(
                                                        originalAlbum = item,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                        .animateItem(),
                                )
                            }

                            else -> {}
                        }
                    }
                }

            LibraryViewType.GRID ->
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns =
                    GridCells.Adaptive(
                        minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp,
                    ),
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                ) {
                    item(key = "header_title", span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            Spacer(Modifier.height(40.dp))
                            Text(
                                text = stringResource(R.string.filter_library).lowercase(),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = onTint,
                                fontSize = 42.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                            )
                        }
                    }

                    item(
                        key = "filter",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        filterContent()
                    }

                    item(
                        key = "header",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        headerContent()
                    }
                    if (showLiked) {
                        item(
                            key = "likedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistGridItem(
                                playlist = likedPlaylist,
                                fillMaxWidth = true,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .bounceClick {
                                        navController.navigate("auto_playlist/liked")
                                    }
                                    .animateItem(),
                            )
                        }
                    }

                    if (showDownloaded) {
                        item(
                            key = "downloadedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistGridItem(
                                playlist = downloadPlaylist,
                                fillMaxWidth = true,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .bounceClick {
                                        navController.navigate("auto_playlist/downloaded")
                                    }
                                    .animateItem(),
                            )
                        }
                    }

                    if (showTop) {
                        item(
                            key = "TopPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistGridItem(
                                playlist = topPlaylist,
                                fillMaxWidth = true,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .bounceClick {
                                        navController.navigate("top_playlist/$topSize")
                                    }
                                    .animateItem(),
                            )
                        }
                    }

                    if (showCached) {
                        item(
                            key = "cachePlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistGridItem(
                                playlist = cachePlaylist,
                                fillMaxWidth = true,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .bounceClick {
                                        navController.navigate("cache_playlist/cached")
                                    }
                                    .animateItem(),
                            )
                        }
                    }

                    if (showUploaded) {
                        item(
                            key = "uploadedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistGridItem(
                                playlist = uploadedPlaylist,
                                fillMaxWidth = true,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .bounceClick {
                                        navController.navigate("auto_playlist/uploaded")
                                    }
                                    .animateItem(),
                            )
                        }
                    }

                    if (showLocal) {
                        item(
                            key = "localPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistGridItem(
                                playlist = localPlaylist,
                                fillMaxWidth = true,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .bounceClick {
                                        navController.navigate("auto_playlist/local")
                                    }
                                    .animateItem(),
                            )
                        }
                    }

                    items(
                        items = allItems.distinctBy { it.id },
                        key = { it.id },
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) { item ->
                        when (item) {
                            is Playlist -> {
                                PlaylistGridItem(
                                    playlist = item,
                                    fillMaxWidth = true,
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .combinedBounceClick(
                                            onClick = {
                                                navController.navigate("local_playlist/${item.id}")
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    PlaylistMenu(
                                                        playlist = item,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                        .animateItem(),
                                )
                            }

                            is Artist -> {
                                ArtistGridItem(
                                    artist = item,
                                    fillMaxWidth = true,
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .combinedBounceClick(
                                            onClick = {
                                                navController.navigate("artist/${item.id}")
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    ArtistMenu(
                                                        originalArtist = item,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                        .animateItem(),
                                )
                            }

                            is Album -> {
                                AlbumGridItem(
                                    album = item,
                                    isActive = item.id == mediaMetadata?.album?.id,
                                    isPlaying = isPlaying,
                                    coroutineScope = coroutineScope,
                                    fillMaxWidth = true,
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .combinedBounceClick(
                                            onClick = {
                                                navController.navigate("album/${item.id}")
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    AlbumMenu(
                                                        originalAlbum = item,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                        .animateItem(),
                                )
                            }

                            else -> {}
                        }
                    }
                }
            }

            when (viewType) {
                LibraryViewType.LIST -> HideOnScrollFAB(
                    lazyListState = lazyListState,
                    icon = R.drawable.add,
                    onClick = { showCreatePlaylistDialog = true },
                )
                LibraryViewType.GRID -> HideOnScrollFAB(
                    lazyListState = lazyGridState,
                    icon = R.drawable.add,
                    onClick = { showCreatePlaylistDialog = true },
                )
            }
        }
      }
    }
}
