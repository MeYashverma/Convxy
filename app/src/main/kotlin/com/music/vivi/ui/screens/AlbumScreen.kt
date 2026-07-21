/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.music.vivi.ui.utils.bounceClick
import com.music.vivi.ui.utils.combinedBounceClick

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.graphics.Color
import com.music.vivi.constants.AppBarHeight
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastForEachReversed
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.constants.HideVideoSongsKey
import com.music.vivi.constants.AlbumCanvasEnabledKey
import com.music.vivi.db.entities.Album
import com.music.vivi.playback.ExoDownloadService
import com.music.vivi.playback.queues.LocalAlbumRadio
import com.music.vivi.ui.component.AlbumGradient
import com.music.vivi.ui.component.ExpandableText
import com.music.vivi.ui.component.GlassCircleButton
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.LinkSegment
import com.music.vivi.ui.component.LocalAppBackdrop
import com.music.vivi.ui.component.LocalGlassEffectConfig
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.backdrop.backdrops.layerBackdrop
import com.music.vivi.ui.component.backdrop.backdrops.rememberLayerBackdrop
import com.music.vivi.ui.component.NavigationTitle
import com.music.vivi.ui.component.SongListItem
import com.music.vivi.ui.component.YouTubeGridItem
import com.music.vivi.ui.component.isGlassAllowed
import com.music.vivi.ui.component.liquidGlass
import com.music.vivi.ui.component.shapes.ContinuousRoundedRectangle
import com.music.vivi.ui.menu.AlbumMenu
import com.music.vivi.ui.menu.SelectionSongMenu
import com.music.vivi.ui.menu.SongMenu
import com.music.vivi.ui.menu.YouTubeAlbumMenu
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.ui.utils.fadingEdge
import com.music.vivi.ui.player.CanvasArtworkPlayer
import com.music.vivi.ui.theme.rememberArtworkTint
import com.music.vivi.utils.listItemShape
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.AlbumViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlbumScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return

    val scope = rememberCoroutineScope()

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlistId by viewModel.playlistId.collectAsState()
    val albumWithSongs by viewModel.albumWithSongs.collectAsState()
    val otherVersions by viewModel.otherVersions.collectAsState()
    val releasesForYou by viewModel.releasesForYou.collectAsState()
    val description by viewModel.description.collectAsState()
    val descriptionRuns by viewModel.descriptionRuns.collectAsState()
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val hideVideoSongs by rememberPreference(key = HideVideoSongsKey, defaultValue = false)
    val albumCanvasEnabled by rememberPreference(key = AlbumCanvasEnabledKey, defaultValue = false)

    val canvasArtwork = rememberAlbumCanvas(
        albumTitle = albumWithSongs?.album?.title,
        artistName = albumWithSongs?.artists?.joinToString { it.name }?.takeIf { it.isNotEmpty() },
        firstSongTitle = albumWithSongs?.songs?.firstOrNull()?.song?.title
    )

    val filteredSongs = remember(albumWithSongs, hideExplicit, hideVideoSongs) {
        var songs = albumWithSongs?.songs ?: emptyList()
        if (hideExplicit) {
            songs = songs.filter { !it.song.explicit }
        }
        if (hideVideoSongs) {
            songs = songs.filter { !it.song.isVideo }
        }
        songs
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
    if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    LaunchedEffect(filteredSongs) {
        selection.fastForEachReversed { songId ->
            if (filteredSongs.find { it.id == songId } == null) {
                selection.remove(songId)
            }
        }
    }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }

    LaunchedEffect(albumWithSongs) {
        val songs = albumWithSongs?.songs?.map { it.id }
        if (songs.isNullOrEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it]?.state == Download.STATE_QUEUED ||
                                downloads[it]?.state == Download.STATE_DOWNLOADING ||
                                downloads[it]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    val hasExplicitContent = remember(albumWithSongs) {
        albumWithSongs?.album?.explicit == true
    }

    val lazyListState = rememberLazyListState()

    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 100
        }
    }

    val artworkColors = rememberArtworkTint(albumWithSongs?.album?.thumbnailUrl)
    val tint = artworkColors.getOrNull(0) ?: MaterialTheme.colorScheme.surface
    val onTint = com.music.vivi.ui.theme.AppleTokens.onColor(tint)

    val glassConfig = LocalGlassEffectConfig.current
    val useGlass = glassConfig.globalEnabled && isGlassAllowed()
    val chromeShape = ContinuousRoundedRectangle(percent = 50)
    val chromeContentColor = if (useGlass) glassConfig.textColor else MaterialTheme.colorScheme.onSurface

    // Unattached backdrop (never .layerBackdrop'd): glass chrome sampling it
    // early-returns, so it draws a translucent frosted surface with no live
    // refraction — but crucially no RenderNode self-reference. See ArtistScreen.kt
    // for the full explanation of the cycle this avoids.
    val heroBackdrop = rememberLayerBackdrop()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tint)
    ) {
    CompositionLocalProvider(
        LocalAppBackdrop provides heroBackdrop,
        LocalContentColor provides onTint
    ) {
    // Built INSIDE the provider so liquidGlass captures heroBackdrop, not the
    // root appBackdrop — sampling appBackdrop here is the RenderNode cycle.
    val chromeBackgroundModifier = if (useGlass) {
        Modifier.liquidGlass(config = glassConfig, shape = chromeShape, highlightAlpha = 0.3f)
    } else {
        Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f), chromeShape)
    }
    LazyColumn(
        state = lazyListState,
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) {
        val albumWithSongs = albumWithSongs
        if (albumWithSongs != null && albumWithSongs.songs.isNotEmpty()) {
             item(key = "album_header") {
                val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
                val density = LocalDensity.current
                val headerOffset = with(density) {
                    -(systemBarsTopPadding + AppBarHeight).roundToPx()
                }

                val tintHeader = artworkColors.getOrNull(0) ?: MaterialTheme.colorScheme.surface
                val onTintHeader = com.music.vivi.ui.theme.AppleTokens.onColor(tintHeader)

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Album Image with offset (like ArtistScreen)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .offset {
                                IntOffset(x = 0, y = headerOffset)
                            }
                            .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.Black, Color.Transparent),
                                        startY = size.height * 0.4f,
                                        endY = size.height
                                    ),
                                    blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                                )
                            }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            AsyncImage(
                                model = albumWithSongs.album.thumbnailUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            if (albumCanvasEnabled && canvasArtwork != null) {
                                CanvasArtworkPlayer(
                                    primaryUrl = canvasArtwork.animated,
                                    fallbackUrl = canvasArtwork.videoUrl,
                                    isPlaying = true,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    // Content column positioned at bottom part of the image
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = LocalContext.current.resources.displayMetrics.widthPixels.let { screenWidth ->
                                    with(density) {
                                        ((screenWidth / 1.2f) - 144).toDp()
                                    }
                                }
                            )
                            .padding(bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                    // Metadata & Actions Section - Left Aligned
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                            .padding(bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val albumInfoText = buildString {
                            append(stringResource(R.string.album_text))
                            if (albumWithSongs.album.year != null) {
                                append(" • ${albumWithSongs.album.year}")
                            }
                            append(" • ${albumWithSongs.songs.size} Tracks")
                            val totalDuration = albumWithSongs.songs.sumOf { it.song.duration }
                            val hours = totalDuration / 3600
                            val minutes = (totalDuration % 3600) / 60
                            if (hours > 0) {
                                append(" • ${hours}h ${minutes}m")
                            } else {
                                append(" • ${minutes}m")
                            }
                        }

                        if (albumWithSongs.artists.size == 1) {
                            val artist = albumWithSongs.artists.first()
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = albumWithSongs.album.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 30.sp,
                                    color = onTint,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .combinedBounceClick(
                                            onClick = {
                                                navController.navigate("artist/${artist.id}")
                                            }
                                        )
                                ) {
                                    AsyncImage(
                                        model = artist.thumbnailUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = artist.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = onTint
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = albumInfoText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = onTint.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center
                                    )
                                    if (hasExplicitContent) {
                                        Text(
                                            text = " • ",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = onTint.copy(alpha = 0.7f)
                                        )
                                        Icon(
                                            painter = painterResource(R.drawable.explicit),
                                            contentDescription = stringResource(R.string.explicit),
                                            modifier = Modifier.size(14.dp),
                                            tint = onTint.copy(alpha = 0.7f)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = stringResource(R.string.explicit),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = onTint.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Album Title with Logo Icon for multiple artists
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.album),
                                        contentDescription = null,
                                        modifier = Modifier.size(30.dp),
                                        tint = onTint
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = albumWithSongs.album.title,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = onTint,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = albumInfoText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = onTint.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center
                                    )
                                    if (hasExplicitContent) {
                                        Text(
                                            text = " • ",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = onTint.copy(alpha = 0.7f)
                                        )
                                        Icon(
                                            painter = painterResource(R.drawable.explicit),
                                            contentDescription = stringResource(R.string.explicit),
                                            modifier = Modifier.size(14.dp),
                                            tint = onTint.copy(alpha = 0.7f)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = stringResource(R.string.explicit),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = onTint.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }

                    Spacer(Modifier.height(24.dp))

                    // Action Buttons Row
                    // Action Buttons Row — Redesigned for unified circular look
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Shuffle Button (Left - Circular)
                        GlassCircleButton(
                            onClick = {
                                playerConnection.service.getAutomix(playlistId)
                                playerConnection.playQueue(
                                    LocalAlbumRadio(albumWithSongs.copy(songs = albumWithSongs.songs.shuffled())),
                                )
                            },
                            size = 48.dp,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.shuffle),
                                contentDescription = stringResource(R.string.shuffle_label),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Play Button (Center - Large Circle)
                        Surface(
                            onClick = {
                                if (isPlaying && mediaMetadata?.album?.id == albumWithSongs.album.id) {
                                    playerConnection.player.pause()
                                } else if (mediaMetadata?.album?.id == albumWithSongs.album.id) {
                                    playerConnection.player.play()
                                } else {
                                    playerConnection.service.getAutomix(playlistId)
                                    playerConnection.playQueue(
                                        LocalAlbumRadio(albumWithSongs)
                                    )
                                }
                            },
                            shape = CircleShape,
                            color = LocalContentColor.current,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(
                                        if (isPlaying && mediaMetadata?.album?.id == albumWithSongs.album.id)
                                            R.drawable.pause
                                        else
                                            R.drawable.play
                                    ),
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = Modifier.size(32.dp).offset(x = if (isPlaying && mediaMetadata?.album?.id == albumWithSongs.album.id) 0.dp else 2.dp)
                                )
                            }
                        }

                        // Favorite/Save Button (Right - Circular)
                        GlassCircleButton(
                            onClick = {
                                database.query {
                                    update(albumWithSongs.album.toggleLike())
                                }
                            },
                            size = 48.dp,
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (albumWithSongs.album.bookmarkedAt != null) {
                                        R.drawable.favorite
                                    } else {
                                        R.drawable.favorite_border
                                    }
                                ),
                                contentDescription = if (albumWithSongs.album.bookmarkedAt != null) stringResource(R.string.saved) else stringResource(R.string.save),
                                modifier = Modifier.size(22.dp),
                                tint = if (albumWithSongs.album.bookmarkedAt != null) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    LocalContentColor.current
                                }
                            )
                        }
                    }


                    }

                    Spacer(Modifier.height(5.dp))

                    val staticDescription = remember(albumWithSongs) {
                        "${albumWithSongs.album.title} is an album by ${albumWithSongs.artists.joinToString { it.name }}${
                            if (albumWithSongs.album.year != null) ", released in ${albumWithSongs.album.year}" else ""
                        }. This collection features ${albumWithSongs.songs.size} tracks showcasing their musical artistry."
                    }

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
                            androidx.compose.material3.LocalContentColor provides onTint
                        ) {
                            ExpandableText(
                                text = description ?: staticDescription,
                                runs = descriptionRuns?.map {
                                    LinkSegment(
                                        text = it.text,
                                        url = it.navigationEndpoint?.urlEndpoint?.url
                                    )
                                },
                                collapsedMaxLines = 3
                            )
                        }
                    }

                    if (albumWithSongs.artists.size > 1) {
                        Spacer(Modifier.height(16.dp))
                        // Artist Names (clickable) - only for multiple artists
                        Text(
                            text = buildAnnotatedString {
                                append(stringResource(R.string.by_text))
                                append(" ")
                                albumWithSongs.artists.fastForEachIndexed { index, artist ->
                                    val link = LinkAnnotation.Clickable(artist.id) {
                                        navController.navigate("artist/${artist.id}")
                                    }
                                    withLink(link) {
                                        append(artist.name)
                                    }
                                    if (index != albumWithSongs.artists.lastIndex) {
                                        append(", ")
                                    }
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            textAlign = TextAlign.Start
                        )
                    }
                    }
                }
            }

            if (filteredSongs.isNotEmpty()) {
                item(key = "songs_title") {
                    NavigationTitle(
                        title = stringResource(R.string.songs),
                        modifier = Modifier.animateItem()
                    )
                }

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
//                        albumIndex = index + 1,
                        isActive = song.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        showInLibraryIcon = true,
                        shape = listItemShape(index, filteredSongs.size),
                        flat = true,
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
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .combinedBounceClick(
                                onClick = {
                                    if (inSelectMode) {
                                        onCheckedChange(song.id !in selection)
                                    } else if (song.id == mediaMetadata?.id) {
                                        playerConnection.togglePlayPause()
                                    } else {
                                        playerConnection.service.getAutomix(playlistId)
                                        playerConnection.playQueue(
                                            LocalAlbumRadio(albumWithSongs, startIndex = index),
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
                            ),
                    )
                }
            }

            if (otherVersions.isNotEmpty()) {
                item(key = "other_versions_title") {
                    NavigationTitle(
                        title = stringResource(R.string.other_versions),
                        modifier = Modifier.animateItem()
                    )
                }
                item(key = "other_versions_list") {
                    LazyRow(
                        contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                    ) {
                        items(
                            items = otherVersions.distinctBy { it.id },
                            key = { it.id },
                        ) { item ->
                            YouTubeGridItem(
                                item = item,
                                isActive = mediaMetadata?.album?.id == item.id,
                                isPlaying = isPlaying,
                                coroutineScope = scope,
                                modifier =
                                Modifier
                                    .combinedBounceClick(
                                        onClick = { navController.navigate("album/${item.id}") },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                YouTubeAlbumMenu(
                                                    albumItem = item,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    )
                                    .animateItem(),
                            )
                        }
                    }
                }
            }

            if (releasesForYou.isNotEmpty()) {
                item(key = "releases_for_you_title") {
                    NavigationTitle(
                        title = stringResource(R.string.releases_for_you),
                        modifier = Modifier.animateItem()
                    )
                }
                item(key = "releases_for_you_list") {
                    LazyRow(
                        contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                    ) {
                        items(
                            items = releasesForYou.distinctBy { it.id },
                            key = { it.id },
                        ) { item ->
                            YouTubeGridItem(
                                item = item,
                                isActive = mediaMetadata?.album?.id == item.id,
                                isPlaying = isPlaying,
                                coroutineScope = scope,
                                modifier =
                                Modifier
                                    .combinedBounceClick(
                                        onClick = { navController.navigate("album/${item.id}") },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                YouTubeAlbumMenu(
                                                    albumItem = item,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    )
                                    .animateItem(),
                            )
                        }
                    }
                }
            }

            item(key = "bottom_spacer") {
                Spacer(Modifier.height(50.dp))
            }
        } else {
            item(key = "loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ContainedLoadingIndicator()
                }
            }
        }
    }

        // Floating glass back/share buttons over the hero art, replacing the
        // Material TopAppBar — always visible, no title-bar-on-scroll behavior.
        // Selection mode swaps in a close button + centered count + select-all/menu
        // pill instead, same as the TopAppBar's title/actions used to.
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
                    color = chromeContentColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(chromeShape)
                        .then(chromeBackgroundModifier)
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
                                    songSelection = selection.mapNotNull { songId ->
                                        filteredSongs.find { it.id == songId }
                                    },
                                    onDismiss = menuState::dismiss,
                                    clearAction = onExitSelectionMode
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
            } else {
                GlassCircleButton(
                    onClick = { navController.navigateUp() },
                    onLongClick = { navController.backToMain() },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back_ios),
                        contentDescription = null
                    )
                }

                Spacer(Modifier.weight(1f))

                albumWithSongs?.let { albumWithSongs ->
                    Row(
                        modifier = Modifier
                            .height(48.dp)
                            .clip(chromeShape)
                            .then(chromeBackgroundModifier)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        android.content.Intent.EXTRA_TEXT,
                                        "https://music.youtube.com/playlist?list=${albumWithSongs.album.playlistId}"
                                    )
                                }
                                context.startActivity(
                                    android.content.Intent.createChooser(
                                        intent,
                                        null
                                    )
                                )
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ios_share),
                                contentDescription = stringResource(R.string.share),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                menuState.show {
                                    AlbumMenu(
                                        originalAlbum = Album(
                                            albumWithSongs.album,
                                            albumWithSongs.artists
                                        ),
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = stringResource(R.string.more_options),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    }
}
