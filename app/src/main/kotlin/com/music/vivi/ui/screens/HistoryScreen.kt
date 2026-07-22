/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import com.music.vivi.constants.IosOverscrollKey
import com.music.vivi.ui.utils.iosOverscroll
import com.music.vivi.ui.utils.rememberHeroPull
import com.music.vivi.ui.utils.heroPullZoom
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachReversed
import androidx.activity.compose.LocalActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import com.music.innertube.utils.parseCookieString
import com.music.vivi.ui.utils.bounceClick
import com.music.vivi.ui.utils.combinedBounceClick
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.HistorySource
import com.music.vivi.constants.InnerTubeCookieKey
import com.music.vivi.extensions.metadata
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.component.ChipsRow
import com.music.vivi.ui.component.HideOnScrollFAB
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.NavigationTitle
import com.music.vivi.ui.component.SongListItem
import com.music.vivi.ui.component.YouTubeListItem
import com.music.vivi.ui.component.GlassCircleButton
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.clip
import com.music.vivi.ui.component.HeroBackground
import com.music.vivi.ui.component.rememberHeroTopBlur
import com.music.vivi.ui.component.rememberHeroSource
import com.music.vivi.ui.component.rememberHeroTint
import com.music.vivi.ui.theme.AppleTokens
import com.music.vivi.ui.component.LocalAppBackdrop
import com.music.vivi.ui.component.LocalGlassEffectConfig
import com.music.vivi.ui.component.isGlassAllowed
import com.music.vivi.ui.component.liquidGlass
import com.music.vivi.ui.component.shapes.ContinuousRoundedRectangle
import com.music.vivi.ui.component.backdrop.backdrops.rememberLayerBackdrop
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.music.vivi.ui.menu.SelectionMediaMetadataMenu
import com.music.vivi.ui.menu.SongMenu
import com.music.vivi.ui.menu.YouTubeSongMenu
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.listItemShape
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.DateAgo
import com.music.vivi.viewmodels.HistoryViewModel
import java.time.format.DateTimeFormatter

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection = rememberSaveable(
        saver = listSaver<MutableList<Long>, Long>(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf() }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
    }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
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

    val historySource by viewModel.historySource.collectAsState()

    val filteredRemoteContent by viewModel.filteredRemoteContent.collectAsState()
    val filteredEvents by viewModel.filteredEvents.collectAsState()
    val allEvents by viewModel.filteredFlatEvents.collectAsState()

    LaunchedEffect(query.text) {
        viewModel.searchQuery.value = query.text
    }

    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }

    @SuppressLint("LocalContextGetResourceValueCall")
    fun dateAgoToString(dateAgo: DateAgo): String {
        return when (dateAgo) {
            DateAgo.Today -> context.getString(R.string.today)
            DateAgo.Yesterday -> context.getString(R.string.yesterday)
            DateAgo.ThisWeek -> context.getString(R.string.this_week)
            DateAgo.LastWeek -> context.getString(R.string.last_week)
            is DateAgo.Other -> dateAgo.date.format(DateTimeFormatter.ofPattern("yyyy/MM"))
        }
    }

    LaunchedEffect(allEvents) {
        selection.fastForEachReversed { eventId ->
            if (allEvents.find { it.event.id == eventId } == null) {
                selection.remove(eventId)
            }
        }
    }

    val lazyListState = rememberLazyListState()

    val heroUrl = remember(filteredRemoteContent, filteredEvents) {
        if (historySource == HistorySource.REMOTE) {
            filteredRemoteContent?.firstOrNull()?.songs?.firstOrNull()?.thumbnail
        } else {
            filteredEvents.values.firstOrNull()?.firstOrNull()?.song?.song?.thumbnailUrl
        }
    }
    val heroSource = rememberHeroSource(
        staticArt = heroUrl,
        songs = if (historySource == HistorySource.LOCAL) {
            allEvents.map { it.song.song.thumbnailUrl to false }
        } else emptyList()
    )
    val tint = rememberHeroTint(heroUrl)
    val onTint = AppleTokens.onColor(tint)

    val glassConfig = LocalGlassEffectConfig.current
    val useGlass = glassConfig.globalEnabled && isGlassAllowed()
    val heroBackdrop = rememberLayerBackdrop()

    val heroPull = rememberHeroPull()
    val heroMaxPull = with(LocalDensity.current) { 220.dp.toPx() }
    val heroScale = 1f + (heroPull.value / heroMaxPull) * 0.18f
    val overscrollEnabled = rememberPreference(IosOverscrollKey, false).value

    HeroBackground(
        tint = tint,
        heroSource = heroSource,
        blurArtwork = true,
        bottomGradient = true,
        topBlurProgress = rememberHeroTopBlur(lazyListState),
        heroScale = heroScale,
        modifier = Modifier.fillMaxSize(),
    ) {
      CompositionLocalProvider(
          LocalAppBackdrop provides heroBackdrop,
          LocalContentColor provides onTint
      ) {
        val chromeShape = ContinuousRoundedRectangle(percent = 50)
        
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .iosOverscroll(overscrollEnabled, allowTopPull = false)
                    .then(if (overscrollEnabled) Modifier.heroPullZoom(heroPull, heroMaxPull) else Modifier),
                contentPadding = LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                    .asPaddingValues(),
            ) {
                item(key = "history_header") {
                    Spacer(Modifier.height(40.dp))
                    Text(
                        text = stringResource(R.string.history).lowercase(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = onTint,
                        fontSize = 42.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                    )
                }

                item(key = "chips_row") {
                    ChipsRow(
                        chips = if (isLoggedIn) listOf(
                            HistorySource.LOCAL to stringResource(R.string.local_history),
                            HistorySource.REMOTE to stringResource(R.string.remote_history),
                        ) else {
                            listOf(HistorySource.LOCAL to stringResource(R.string.local_history))
                        },
                        currentValue = historySource,
                        onValueUpdate = {
                            viewModel.historySource.value = it
                            if (it == HistorySource.REMOTE){
                                viewModel.fetchRemoteHistory()
                            }
                        }
                    )
                }

                if (historySource == HistorySource.REMOTE && isLoggedIn) {
                    filteredRemoteContent?.forEach { section ->
                        stickyHeader {
                            NavigationTitle(
                                title = section.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(tint.copy(alpha = 0.8f))
                            )
                        }

                        itemsIndexed(
                            items = section.songs,
                            key = { index, song -> "${section.title}_${song.id}_$index" }
                        ) { index, song ->
                            YouTubeListItem(
                                item = song,
                                isActive = song.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                shape = listItemShape(index, section.songs.size),
                                flat = true,
                                trailingContent = {
                                    androidx.compose.material3.IconButton(
                                        onClick = {
                                            menuState.show {
                                                YouTubeSongMenu(
                                                    song = song,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                    onHistoryRemoved = {
                                                        viewModel.fetchRemoteHistory()
                                                    }
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.more_vert),
                                            contentDescription = null
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedBounceClick(
                                        onClick = {
                                            if (song.id == mediaMetadata?.id) {
                                                playerConnection.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    YouTubeQueue.radio(song.toMediaMetadata())
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            menuState.show {
                                                YouTubeSongMenu(
                                                    song = song,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                    onHistoryRemoved = {
                                                        viewModel.fetchRemoteHistory()
                                                    }
                                                )
                                            }
                                        }
                                    )
                                    .animateItem()
                            )
                        }
                    }
                } else {
                    filteredEvents.forEach { (dateAgo, dateEvents) ->
                        stickyHeader {
                            NavigationTitle(
                                title = dateAgoToString(dateAgo),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(tint.copy(alpha = 0.8f))
                            )
                        }

                        itemsIndexed(
                            items = dateEvents,
                            key = { _, event -> event.event.id }
                        ) { index, event ->
                            val onCheckedChange: (Boolean) -> Unit = remember(event.event.id) {
                                { checked ->
                                    if (checked) {
                                        selection.add(event.event.id)
                                    } else {
                                        selection.remove(event.event.id)
                                    }
                                }
                            }

                            SongListItem(
                                song = event.song,
                                isActive = event.song.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                showInLibraryIcon = true,
                                showDownloadIcon = false,
                                shape = listItemShape(index, dateEvents.size),
                                flat = true,
                                trailingContent = {
                                    if (inSelectMode) {
                                        Checkbox(
                                            checked = event.event.id in selection,
                                            onCheckedChange = onCheckedChange
                                        )
                                    } else {
                                        androidx.compose.material3.IconButton(
                                            onClick = {
                                                menuState.show {
                                                    SongMenu(
                                                        originalSong = event.song,
                                                        event = event.event,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss
                                                    )
                                                }
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = null
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedBounceClick(
                                        onClick = {
                                            if (inSelectMode) {
                                                onCheckedChange(event.event.id !in selection)
                                            } else if (event.song.id == mediaMetadata?.id) {
                                                playerConnection.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = dateAgoToString(dateAgo),
                                                        items = dateEvents.map { it.song.toMediaItem() },
                                                        startIndex = index
                                                    )
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            if (!inSelectMode) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                inSelectMode = true
                                                onCheckedChange(true)
                                            }
                                        }
                                    )
                                    .animateItem()
                            )
                        }
                    }
                }

                item(key = "bottom_spacer_history") {
                    Spacer(modifier = Modifier.height(16.dp).animateItem())
                }
            }

            HideOnScrollFAB(
                visible = if (historySource == HistorySource.REMOTE) {
                    filteredRemoteContent?.any { it.songs.isNotEmpty() } == true
                } else {
                    allEvents.isNotEmpty()
                },
                lazyListState = lazyListState,
                icon = R.drawable.shuffle,
                onClick = {
                    if (historySource == HistorySource.REMOTE && filteredRemoteContent != null) {
                        val songs = filteredRemoteContent?.flatMap { it.songs } ?: emptyList()
                        if (songs.isNotEmpty()) {
                            playerConnection.playQueue(
                                ListQueue(
                                    title = context.getString(R.string.history),
                                    items = songs.map { it.toMediaItem() }.shuffled()
                                )
                            )
                        }
                    } else {
                        playerConnection.playQueue(
                            ListQueue(
                                title = context.getString(R.string.history),
                                items = allEvents.map { it.song.toMediaItem() }.shuffled()
                            )
                        )
                    }
                }
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
                        text = pluralStringResource(R.plurals.n_selected, selection.size, selection.size),
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
                            checked = selection.size == allEvents.size && selection.isNotEmpty(),
                            onCheckedChange = {
                                if (selection.size == allEvents.size) {
                                    selection.clear()
                                } else {
                                    selection.clear()
                                    selection.addAll(allEvents.map { it.event.id })
                                }
                            }
                        )
                        androidx.compose.material3.IconButton(
                            enabled = selection.isNotEmpty(),
                            onClick = {
                                menuState.show {
                                    SelectionMediaMetadataMenu(
                                        songSelection = selection.mapNotNull { eventId ->
                                            allEvents.find { it.event.id == eventId }?.song?.toMediaItem()?.metadata
                                        },
                                        onDismiss = menuState::dismiss,
                                        clearAction = onExitSelectionMode,
                                        currentItems = emptyList()
                                    )
                                }
                            }
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
                        onClick = {
                            if (isSearching) {
                                isSearching = false
                                query = TextFieldValue()
                            } else {
                                navController.navigateUp()
                            }
                        },
                        onLongClick = {
                            if (!isSearching) {
                                navController.backToMain()
                            }
                        }
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
