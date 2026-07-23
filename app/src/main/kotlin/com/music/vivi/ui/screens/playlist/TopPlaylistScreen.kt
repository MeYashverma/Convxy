/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.playlist

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.music.vivi.ui.utils.rememberHeroZoom
import com.music.vivi.ui.utils.heroPullZoom
import com.music.vivi.ui.utils.listOverscroll
import com.music.vivi.utils.rememberPreference
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.util.fastSumBy
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.ui.utils.bounceClick
import com.music.vivi.ui.utils.combinedBounceClick
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.MyTopFilter
import com.music.vivi.db.entities.Song
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.playback.ExoDownloadService
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.ui.component.AnimatedPlayPauseIcon
import com.music.vivi.ui.component.DefaultDialog
import com.music.vivi.ui.component.DraggableScrollbar
import com.music.vivi.ui.component.EmptyPlaceholder
import com.music.vivi.ui.component.ExpandableText
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.SongListItem
import com.music.vivi.ui.component.SortHeader
import com.music.vivi.ui.menu.SelectionSongMenu
import com.music.vivi.ui.menu.SongMenu
import com.music.vivi.ui.menu.TopPlaylistMenu
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.listItemShape
import com.music.vivi.utils.makeTimeString
import com.music.vivi.viewmodels.TopPlaylistViewModel
import com.music.vivi.ui.component.HeroBackground
import com.music.vivi.ui.component.rememberHeroTopBlur
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
import com.music.vivi.ui.component.GlassCircleButton
import androidx.compose.ui.draw.clip
import java.time.LocalDateTime

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TopPlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: TopPlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val maxSize = viewModel.top

    val songs by viewModel.topSongs.collectAsState(null)
    val mutableSongs = remember { mutableStateListOf<Song>() }

    val likeLength = remember(songs) {
        songs?.fastSumBy { it.song.duration } ?: 0
    }

    var isSearching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }
    
    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection = rememberSaveable(
        saver = listSaver<MutableList<String>, String>(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf() }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
    }

    val filteredSongs = remember(songs, query) {
        if (query.text.isEmpty()) songs ?: emptyList()
        else songs?.filter { song ->
            song.title.contains(query.text, true) ||
                song.artists.any { it.name.contains(query.text, true) }
        } ?: emptyList()
    }

    LaunchedEffect(filteredSongs) {
        selection.fastForEachReversed { songId ->
            if (filteredSongs.find { it.id == songId } == null) {
                selection.remove(songId)
            }
        }
    }

    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    } else if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    val sortType by viewModel.topPeriod.collectAsState()
    val name = stringResource(R.string.my_top) + " $maxSize"

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }

    LaunchedEffect(songs) {
        mutableSongs.apply {
            clear()
            songs?.let { addAll(it) }
        }
        if (songs?.isEmpty() == true) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs?.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED } == true) {
                    Download.STATE_COMPLETED
                } else if (songs?.all {
                        downloads[it.song.id]?.state == Download.STATE_QUEUED ||
                                downloads[it.song.id]?.state == Download.STATE_DOWNLOADING ||
                                downloads[it.song.id]?.state == Download.STATE_COMPLETED
                    } == true
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    var showRemoveDownloadDialog by remember { mutableStateOf(false) }

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.remove_download_playlist_confirm, name),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(
                    onClick = { showRemoveDownloadDialog = false },
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        songs!!.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.song.id,
                                false,
                            )
                        }
                    },
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    val state = rememberLazyListState()

    val heroUrl = songs?.firstOrNull()?.song?.thumbnailUrl
    val heroSource = rememberHeroSource(
        staticArt = heroUrl,
        songs = songs?.map { it.song.thumbnailUrl to false } ?: emptyList()
    )
    val tint = rememberHeroTint(heroUrl)
    val onTint = AppleTokens.onColor(tint)

    val glassConfig = LocalGlassEffectConfig.current
    val useGlass = glassConfig.globalEnabled && isGlassAllowed()
    val heroBackdrop = rememberLayerBackdrop()

    val heroZoom = rememberHeroZoom()

    HeroBackground(
        tint = tint,
        heroSource = heroSource,
        blurArtwork = true,
        bottomGradient = true,
        topBlurProgress = rememberHeroTopBlur(state),
        heroScale = heroZoom.scale,
        modifier = Modifier.fillMaxSize(),
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

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = state,
                // No bounce here: the top pull drives the hero zoom instead.
                overscrollEffect = heroZoom.listOverscroll(),
                modifier = Modifier.heroPullZoom(heroZoom),
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            ) {
                if (songs != null) {
                    item(key = "header_title") {
                        Column {
                            Spacer(Modifier.height(40.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = onTint,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                            )
                        }
                    }

                    if (songs!!.isEmpty()) {
                        item(key = "empty_placeholder") {
                            EmptyPlaceholder(
                                icon = R.drawable.music_note,
                                text = stringResource(R.string.playlist_is_empty),
                                modifier = Modifier.padding(top = 100.dp)
                            )
                        }
                    } else {
                        if (!isSearching) {
                            item(key = "playlist_header") {
                                TopPlaylistHeader(
                                    name = name,
                                    songs = songs!!,
                                    likeLength = likeLength,
                                    downloadState = downloadState,
                                    onShowRemoveDownloadDialog = { showRemoveDownloadDialog = true },
                                    menuState = menuState,
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }

                        item(key = "songs_header") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                            ) {
                                SortHeader(
                                    sortType = sortType,
                                    sortDescending = false,
                                    onSortTypeChange = {
                                        viewModel.topPeriod.value = it
                                    },
                                    onSortDescendingChange = {},
                                    sortTypeText = { sortType ->
                                        when (sortType) {
                                            MyTopFilter.ALL_TIME -> R.string.all_time
                                            MyTopFilter.DAY -> R.string.past_24_hours
                                            MyTopFilter.WEEK -> R.string.past_week
                                            MyTopFilter.MONTH -> R.string.past_month
                                            MyTopFilter.YEAR -> R.string.past_year
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    showDescending = false,
                                )
                            }
                        }
                    }
                }

                if (filteredSongs.isNotEmpty()) {
                    itemsIndexed(
                        items = filteredSongs,
                        key = { _, song -> song.id },
                    ) { index, song ->
                        val onCheckedChange: (Boolean) -> Unit = {
                            if (it) {
                                selection.add(song.id)
                            } else {
                                selection.remove(song.id)
                            }
                        }

                        SongListItem(
                            song = song,
//                            albumIndex = index + 1,
                            isActive = song.song.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            showInLibraryIcon = true,
                            flat = true,
                            shape = listItemShape(index, filteredSongs.size),
                            trailingContent = {
                                if (inSelectMode) {
                                    Checkbox(
                                        checked = song.id in selection,
                                        onCheckedChange = onCheckedChange
                                    )
                                } else {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song,
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
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedBounceClick(
                                    onClick = {
                                        if (inSelectMode) {
                                            onCheckedChange(song.id !in selection)
                                        } else if (song.song.id == mediaMetadata?.id) {
                                            playerConnection.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = name,
                                                    items = songs!!.map { it.toMediaItem() },
                                                    startIndex = songs!!.indexOfFirst { it.id == song.id }
                                                ),
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        if (!inSelectMode) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            inSelectMode = true
                                            onCheckedChange(true)
                                        }
                                    },
                                )
                                .animateItem()
                        )
                    }
                }
                item(key = "bottom_spacer") {
                    Spacer(Modifier.height(100.dp))
                }
            }


            DraggableScrollbar(
                modifier = Modifier
                    .padding(
                        LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)
                            .asPaddingValues()
                    )
                    .align(Alignment.CenterEnd),
                scrollState = state,
                headerItems = 2
            )

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
                if (inSelectMode) {
                    GlassCircleButton(onClick = onExitSelectionMode) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                        )
                    }

                    Text(
                        text = pluralStringResource(R.plurals.n_song, selection.size, selection.size),
                        style = MaterialTheme.typography.titleMedium,
                        color = onTint,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        modifier = Modifier
                            .height(48.dp)
                            .clip(chromeShape)
                            .background(onTint.copy(alpha = 0.15f), chromeShape)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Checkbox(
                            checked = selection.size == filteredSongs.size && selection.isNotEmpty(),
                            onCheckedChange = {
                                if (selection.size == filteredSongs.size) {
                                    selection.clear()
                                } else {
                                    selection.clear()
                                    selection.addAll(filteredSongs.map { it.id })
                                }
                            }
                        )
                        IconButton(
                            enabled = selection.isNotEmpty(),
                            onClick = {
                                menuState.show {
                                    SelectionSongMenu(
                                        songSelection = filteredSongs.filter { it.id in selection },
                                        onDismiss = menuState::dismiss,
                                        clearAction = onExitSelectionMode,
                                    )
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null
                            )
                        }
                    }
                } else if (isSearching) {
                    GlassCircleButton(
                        onClick = {
                            isSearching = false
                            query = TextFieldValue()
                            focusManager.clearFocus()
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(chromeShape)
                            .background(onTint.copy(alpha = 0.15f), chromeShape)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.search),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = onTint.copy(alpha = 0.6f)
                                )
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleMedium.copy(color = onTint),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                cursorColor = onTint,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    }
                } else {
                    GlassCircleButton(
                        onClick = { navController.navigateUp() },
                        onLongClick = { navController.backToMain() },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    GlassCircleButton(onClick = { isSearching = true }) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null
                        )
                    }
                }
            }
        }
      }
    }
}

@Composable
private fun TopPlaylistHeader(
    name: String,
    songs: List<Song>,
    likeLength: Int,
    downloadState: Int,
    onShowRemoveDownloadDialog: () -> Unit,
    menuState: com.music.vivi.ui.component.MenuState,
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val heroUrl = songs.firstOrNull()?.thumbnailUrl
    val heroSource = rememberHeroSource(
        staticArt = heroUrl,
        songs = songs.map { it.thumbnailUrl to false },
    )
    val tint = rememberHeroTint(heroUrl)
    val onTint = AppleTokens.onColor(tint)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(50.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = onTint,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = buildString {
                    append(pluralStringResource(R.plurals.n_song, songs.size, songs.size))
                    if (likeLength > 0) {
                        append(" • ")
                        append(makeTimeString(likeLength * 1000L))
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = onTint.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (isPlaying) {
                            playerConnection.player.pause()
                        } else {
                            playerConnection.playQueue(
                                ListQueue(
                                    title = name,
                                    items = songs.map { it.toMediaItem() },
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LocalContentColor.current,
                        contentColor = tint,
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AnimatedPlayPauseIcon(
                            isPlaying = isPlaying,
                            modifier = Modifier.size(20.dp),
                            tint = tint,
                            size = 20.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.play),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = tint,
                        )
                    }
                }

                Button(
                    onClick = {
                        playerConnection.playQueue(
                            ListQueue(
                                title = name,
                                items = songs.shuffled().map { it.toMediaItem() },
                            )
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LocalContentColor.current.copy(alpha = 0.15f),
                        contentColor = LocalContentColor.current,
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.shuffle),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = LocalContentColor.current,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.shuffle),
                            style = MaterialTheme.typography.labelLarge,
                            color = LocalContentColor.current,
                        )
                    }
                }

                GlassCircleButton(
                    onClick = {
                        menuState.show {
                            TopPlaylistMenu(
                                downloadState = downloadState,
                                onQueue = {
                                    playerConnection.addToQueue(songs.map { it.toMediaItem() })
                                },
                                onDownload = {
                                    when (downloadState) {
                                        Download.STATE_COMPLETED -> onShowRemoveDownloadDialog()
                                        Download.STATE_DOWNLOADING -> {
                                            songs.forEach { song ->
                                                DownloadService.sendRemoveDownload(
                                                    context,
                                                    ExoDownloadService::class.java,
                                                    song.id,
                                                    false,
                                                )
                                            }
                                        }

                                        else -> {
                                            songs.forEach { song ->
                                                val downloadRequest = DownloadRequest
                                                    .Builder(song.id, song.id.toUri())
                                                    .setCustomCacheKey(song.id)
                                                    .setData(song.title.toByteArray())
                                                    .build()
                                                DownloadService.sendAddDownload(
                                                    context,
                                                    ExoDownloadService::class.java,
                                                    downloadRequest,
                                                    false,
                                                )
                                            }
                                        }
                                    }
                                },
                                onDismiss = { menuState.dismiss() }
                            )
                        }
                    },
                    size = 48.dp,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = stringResource(R.string.about_album),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = onTint,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides onTint,
                ) {
                    ExpandableText(
                        text = "$name is a playlist featuring your ${songs.size} most played tracks. Keep listening to discover how your top songs evolve over time.",
                        collapsedMaxLines = 3
                    )
                }
            }
        }
    }
}
