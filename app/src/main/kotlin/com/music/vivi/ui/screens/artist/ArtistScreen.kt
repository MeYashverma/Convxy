/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.artist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ripple
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalListenTogetherManager
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.AppBarHeight
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.constants.ShowArtistDescriptionKey
import com.music.vivi.constants.ShowArtistSubscriberCountKey
import com.music.vivi.constants.ShowMonthlyListenersKey
import com.music.vivi.db.entities.ArtistEntity
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.component.AnimatedPlayPauseIcon
import com.music.vivi.ui.component.AlbumGridItem
import com.music.vivi.ui.component.ExpandableText
import com.music.vivi.ui.component.GlassCircleButton
import com.music.vivi.ui.component.HideOnScrollFAB
import com.music.vivi.ui.component.LinkSegment
import com.music.vivi.ui.component.LocalAppBackdrop
import com.music.vivi.ui.component.LocalGlassEffectConfig
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.backdrop.backdrops.layerBackdrop
import com.music.vivi.ui.component.backdrop.backdrops.rememberLayerBackdrop
import com.music.vivi.ui.component.NavigationTitle
import com.music.vivi.ui.component.SongListItem
import com.music.vivi.ui.component.YouTubeGridItem
import com.music.vivi.ui.component.YouTubeListItem
import com.music.vivi.ui.component.isGlassAllowed
import com.music.vivi.ui.component.liquidGlass
import com.music.vivi.ui.component.shapes.ContinuousRoundedRectangle
import com.music.vivi.ui.theme.rememberArtworkTint
import com.music.vivi.ui.component.shimmer.ButtonPlaceholder
import com.music.vivi.ui.component.shimmer.ListItemPlaceHolder
import com.music.vivi.ui.component.shimmer.ShimmerHost
import com.music.vivi.ui.component.shimmer.TextPlaceholder
import com.music.vivi.ui.menu.AlbumMenu
import com.music.vivi.ui.menu.SongMenu
import com.music.vivi.ui.menu.YouTubeAlbumMenu
import com.music.vivi.ui.menu.YouTubeArtistMenu
import com.music.vivi.ui.menu.YouTubePlaylistMenu
import com.music.vivi.ui.menu.YouTubeSongMenu
import androidx.compose.ui.graphics.graphicsLayer
import com.music.vivi.ui.theme.AppleTokens
import com.music.vivi.ui.theme.rememberBrandFontFamily
import com.music.vivi.ui.theme.LocalAccentTextColor
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.ui.utils.rememberHeroZoom
import com.music.vivi.ui.utils.heroPullZoom
import com.music.vivi.ui.utils.listOverscroll
import com.music.vivi.ui.utils.fadingEdge
import com.music.vivi.ui.utils.isScrollingUp
import com.music.vivi.ui.utils.resize
import com.music.vivi.utils.listItemShape
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.ArtistViewModel
import com.valentinilk.shimmer.shimmer
import com.music.vivi.artistvideo.ArtistVideo
import com.music.vivi.constants.ShowArtistVideoKey
import com.music.vivi.constants.ShowArtistBackgroundVideoKey
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.music.vivi.canvas.AppleMusicArtistBackgroundProvider

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ArtistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: ArtistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return
    val listenTogetherManager = LocalListenTogetherManager.current
    val isGuest = listenTogetherManager?.isInRoom == true && !listenTogetherManager.isHost
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val artistPage = viewModel.artistPage
    val libraryArtist by viewModel.libraryArtist.collectAsState()
    val librarySongs by viewModel.librarySongs.collectAsState()
    val libraryAlbums by viewModel.libraryAlbums.collectAsState()
    val artistVideoUrl by viewModel.artistVideoUrl.collectAsState()
    val artistVideoSong by viewModel.artistVideoSong.collectAsState()
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val showArtistDescription by rememberPreference(key = ShowArtistDescriptionKey, defaultValue = true)
    val showArtistSubscriberCount by rememberPreference(key = ShowArtistSubscriberCountKey, defaultValue = true)
    val showMonthlyListeners by rememberPreference(key = ShowMonthlyListenersKey, defaultValue = true)
    val showArtistVideo by rememberPreference(key = ShowArtistVideoKey, defaultValue = true)
    val showArtistBackgroundVideo by rememberPreference(key = ShowArtistBackgroundVideoKey, defaultValue = true)

    val lazyListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLocal by rememberSaveable { mutableStateOf(false) }
    val density = LocalDensity.current

    // Calculate the offset value outside of the offset lambda
    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val headerOffset = with(density) {
        -(systemBarsTopPadding + AppBarHeight).roundToPx()
    }

    LaunchedEffect(libraryArtist) {
        // always show local page for local artists. Show local page remote artist when offline
        showLocal = libraryArtist?.artist?.isLocal == true
    }

    // Apple Music style: a single dominant color pulled from the artist's own
    // artwork washes the screen instead of a flat Material surface, and the
    // circular back/share/subscribe chrome is real liquid glass sampling this
    // screen's own content behind it (same Modifier.liquidGlass + LocalAppBackdrop
    // mechanism the floating nav bar puck uses) rather than a flat fallback color.
    val artistThumbnail = artistPage?.artist?.thumbnail ?: libraryArtist?.artist?.thumbnailUrl
    val artworkColors = rememberArtworkTint(artistThumbnail)
    val screenBackground = MaterialTheme.colorScheme.background

    val glassConfig = LocalGlassEffectConfig.current
    val useGlass = glassConfig.globalEnabled && isGlassAllowed()
    val chromeShape = ContinuousRoundedRectangle(percent = 50)
    val chromeContentColor = if (useGlass) glassConfig.textColor else MaterialTheme.colorScheme.onSurface

    // Glass chrome (back/share buttons, chips) samples LocalAppBackdrop. The app
    // root's backdrop (MainActivity's Modifier.layerBackdrop(appBackdrop)) captures
    // the WHOLE NavHost — so a glass surface INSIDE this screen sampling it makes
    // the capture include itself: a native RenderNode cycle (stack overflow in
    // prepareTreeImpl). A screen-local layerBackdrop doesn't help either: this
    // screen is itself inside appBackdrop, so its layer is re-recorded and
    // re-drawn within appBackdrop's own draw pass, re-forming the cycle. So we
    // provide an UNATTACHED backdrop (never .layerBackdrop'd onto anything): its
    // drawBackdrop early-returns, drawing no live refraction, but the glass still
    // renders its translucent surface tint + specular highlight — frosted chrome,
    // no self-reference. (True artwork-refracting glass here would need a capture
    // layer rendered OUTSIDE the NavHost.)
    val heroBackdrop = rememberLayerBackdrop()
    val heroZoom = rememberHeroZoom()

    val tint = artworkColors.getOrNull(0) ?: MaterialTheme.colorScheme.surface
    val onTint = com.music.vivi.ui.theme.AppleTokens.onColor(tint)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tint)
    ) {
    CompositionLocalProvider(
        LocalAppBackdrop provides heroBackdrop,
        LocalContentColor provides onTint,
        LocalAccentTextColor provides AppleTokens.onColorHeading(tint)
    ) {
        // Built INSIDE the provider so liquidGlass captures heroBackdrop, not the
        // root appBackdrop — sampling appBackdrop here is the RenderNode cycle.
        val chromeBackgroundModifier = if (useGlass) {
            Modifier.liquidGlass(config = glassConfig, shape = chromeShape, highlightAlpha = 0.3f)
        } else {
            Modifier.background(LocalContentColor.current.copy(alpha = 0.15f), chromeShape)
        }
        LazyColumn(
            state = lazyListState,
            // No bounce here: the top pull drives the hero zoom instead.
            overscrollEffect = heroZoom.listOverscroll(),
            modifier = Modifier.heroPullZoom(heroZoom),
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            if (artistPage == null && !showLocal) {
                item(key = "shimmer") {
                    ShimmerHost (
                        modifier = Modifier
                            .offset {
                                IntOffset(x = 0, y = headerOffset)
                            }
                    ) {
                        // Artist Image Placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.1f),
                        ) {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .shimmer()
                                    .background(MaterialTheme.colorScheme.onSurface)
                                    .fadingEdge(
                                        top = systemBarsTopPadding + AppBarHeight,
                                        bottom = 200.dp,
                                    ),
                            )
                        }
                        // Artist Name and Controls Section
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Artist Name Placeholder
                            TextPlaceholder(
                                height = 36.dp,
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .padding(bottom = 16.dp)
                            )

                            // Buttons Row Placeholder
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Subscribe Button Placeholder
                                ButtonPlaceholder(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .height(52.dp)
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                // Right side buttons
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Radio Button Placeholder
                                    ButtonPlaceholder(
                                        modifier = Modifier
                                            .width(100.dp)
                                            .height(52.dp)
                                    )

                                    // Shuffle Button Placeholder
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .shimmer()
                                            .background(
                                                MaterialTheme.colorScheme.onSurface,
                                                RoundedCornerShape(26.dp)
                                            )
                                    )
                                }
                            }
                        }
                        // Songs List Placeholder
                        repeat(6) {
                            ListItemPlaceHolder()
                        }
                    }
                }
            } else {
                item(key = "header") {
                    val thumbnail = artistPage?.artist?.thumbnail ?: libraryArtist?.artist?.thumbnailUrl
                    val artistName = artistPage?.artist?.title ?: libraryArtist?.artist?.name

                    var backgroundVideoUrl by remember { mutableStateOf<String?>(null) }
                    LaunchedEffect(artistName, showArtistBackgroundVideo) {
                        if (artistName != null && showArtistBackgroundVideo) {
                            withContext(Dispatchers.IO) {
                                backgroundVideoUrl = AppleMusicArtistBackgroundProvider.getByArtistName(artistName)
                            }
                        }
                    }

                    Box {
                        // Artist Image with offset
                        if (thumbnail != null || backgroundVideoUrl != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .offset {
                                        IntOffset(x = 0, y = headerOffset)
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = heroZoom.scale
                                            scaleY = heroZoom.scale
                                        }
                                        .fadingEdge(
                                            bottom = 200.dp,
                                        )
                                ) {
                                    if (thumbnail != null) {
                                        AsyncImage(
                                            model = thumbnail.resize(1200, 1200),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    }
                                    if (backgroundVideoUrl != null && showArtistBackgroundVideo) {
                                        ArtistVideo(
                                            videoUrl = backgroundVideoUrl!!,
                                            modifier = Modifier.fillMaxSize(),
                                            onClick = { }
                                        )
                                    }
                                }
                            }
                        }

                        // Artist Name and Controls Section - positioned at bottom of image

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = if (thumbnail != null) {
                                        // Position content at the bottom part of the image
                                        // Using screen width to calculate aspect ratio height minus overlap
                                        LocalResources.current.displayMetrics.widthPixels.let { screenWidth ->
                                            with(density) {
                                                ((screenWidth / 1.2f) - 144).toDp()
                                            }
                                        }
                                    } else {
                                        16.dp
                                    }
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {

                                    //artist video
                                    if (showArtistVideo && !(showArtistBackgroundVideo && backgroundVideoUrl != null)) {
                                        artistVideoUrl?.let { videoUrl ->
                                            artistPage?.artist?.radioEndpoint?.let { radioEndpoint ->
                                                Spacer(modifier = Modifier.width(5.dp))
                                                ArtistVideo(
                                                    videoUrl = videoUrl,
                                                    modifier = Modifier
                                                        .width(45.dp)
                                                        .height(45.dp),
                                                    onClick = {
                                                        val watchEndpoint = artistVideoSong?.endpoint
                                                            ?: artistPage?.artist?.radioEndpoint
                                                        watchEndpoint?.let {
                                                            playerConnection.playQueue(YouTubeQueue(it))
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(5.dp))

                                    // Artist Name
                                    Text(
                                        text = artistName?.lowercase() ?: "unknown",
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontFamily = rememberBrandFontFamily(),
                                        fontWeight = FontWeight.SemiBold,
                                        // The page's biggest heading: carries the artwork
                                        // tint plainly rather than flat content colour.
                                        color = LocalAccentTextColor.current,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 42.sp,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    if (showArtistSubscriberCount) {
                                        artistPage?.subscriberCountText?.let { subscribers ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clip(chromeShape)
                                                    .then(chromeBackgroundModifier)
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.artist_screen),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = chromeContentColor
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "${subscribers.split(' ').firstOrNull() ?: ""} ${stringResource(R.string.subscribers)}",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = chromeContentColor,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }

                                    if (showMonthlyListeners) {
                                        artistPage?.monthlyListenerCount?.let { monthlyListeners ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clip(chromeShape)
                                                    .then(chromeBackgroundModifier)
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.graphic_eq),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = chromeContentColor
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "${monthlyListeners.split(' ').firstOrNull() ?: ""} ${stringResource(R.string.monthly_listeners)}",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = chromeContentColor,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }

                                if (!showLocal && showArtistDescription && artistPage != null) {
                                    val description = artistPage?.description
                                    val descriptionRuns = artistPage?.descriptionRuns
                                    
                                    if (!description.isNullOrEmpty() || !descriptionRuns.isNullOrEmpty()) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 16.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.about_artist),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = onTint,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )
                                            
                                            androidx.compose.runtime.CompositionLocalProvider(
                                                androidx.compose.material3.LocalContentColor provides onTint,
                                            ) {
                                                ExpandableText(
                                                    text = description.orEmpty(),
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
                                    }
                                }

                                // Buttons Row — Redesigned Play (Large) - Favorite
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    // Info Button (Left)
                                    GlassCircleButton(
                                        onClick = { /* Could show description or bio */ },
                                        size = 48.dp,
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.info),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Primary Play Button (Center - Large)
                                    val artistId = artistPage?.artist?.id
                                    Surface(
                                        onClick = {
                                            val isCurrentArtist = artistId != null &&
                                                mediaMetadata?.artists?.any { it.id == artistId } == true
                                            if (isPlaying && isCurrentArtist) {
                                                playerConnection.player.pause()
                                            } else if (isCurrentArtist) {
                                                playerConnection.player.play()
                                            } else {
                                                // Play artist top songs or radio
                                                val songSection = artistPage?.sections?.find { section ->
                                                    (section.items.firstOrNull() as? SongItem)?.album != null
                                                }
                                                val items = songSection?.items?.filterIsInstance<SongItem>()
                                                if (!items.isNullOrEmpty()) {
                                                    playerConnection.playQueue(
                                                        ListQueue(
                                                            title = artistName ?: "Artist",
                                                            items = items.map { it.toMediaItem() }
                                                        )
                                                    )
                                                } else {
                                                    artistPage?.artist?.radioEndpoint?.let {
                                                        playerConnection.playQueue(YouTubeQueue(it))
                                                    }
                                                }
                                            }
                                        },
                                        shape = CircleShape,
                                        color = onTint,
                                        modifier = Modifier.size(72.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            AnimatedPlayPauseIcon(
                                                isPlaying = isPlaying && artistId != null &&
                                                    mediaMetadata?.artists?.any { it.id == artistId } == true,
                                                tint = tint,
                                                size = 32.dp,
                                                modifier = Modifier.offset(x = 2.dp)
                                            )
                                        }
                                    }

                                    // Favorite/Subscribe Button (Right)
                                    val isSubscribed = libraryArtist?.artist?.bookmarkedAt != null
                                    GlassCircleButton(
                                        onClick = {
                                            database.transaction {
                                                val artist = libraryArtist?.artist
                                                if (artist != null) {
                                                    update(artist.toggleLike())
                                                } else {
                                                    artistPage?.artist?.let {
                                                        insert(
                                                            ArtistEntity(
                                                                id = it.id,
                                                                name = it.title,
                                                                channelId = it.channelId,
                                                                thumbnailUrl = it.thumbnail,
                                                            ).toggleLike()
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        size = 48.dp,
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                if (isSubscribed) R.drawable.favorite else R.drawable.favorite_border
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = if (isSubscribed) MaterialTheme.colorScheme.error else LocalContentColor.current
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }


                if (showLocal) {
                    if (librarySongs.isNotEmpty()) {
                        item(key = "local_songs_title") {
                            NavigationTitle(
                                title = stringResource(R.string.songs),
                                modifier = Modifier.animateItem(),
                                onClick = {
                                    navController.navigate("artist/${viewModel.artistId}/songs")
                                }
                            )
                        }

                        val filteredLibrarySongs = if (hideExplicit) {
                            librarySongs.filter { !it.song.explicit }
                        } else {
                            librarySongs
                        }
                        itemsIndexed(
                            items = filteredLibrarySongs,
                            key = { index, item -> "local_song_${item.id}_$index" }
                        ) { index, song ->
                            SongListItem(
                                song = song,
                                showInLibraryIcon = true,
                                isActive = song.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                shape = listItemShape(index, filteredLibrarySongs.size),
                                flat = true,
                                trailingContent = {
                                    androidx.compose.material3.IconButton(
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
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedBounceClick(
                                        onClick = {
                                            if (song.id == mediaMetadata?.id) {
                                                playerConnection.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = libraryArtist?.artist?.name ?: "Unknown Artist",
                                                        items = librarySongs.map { it.toMediaItem() },
                                                        startIndex = index
                                                    )
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song,
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

                    if (libraryAlbums.isNotEmpty()) {
                        item(key = "local_albums_title") {
                            NavigationTitle(
                                title = stringResource(R.string.albums),
                                modifier = Modifier.animateItem(),
                                onClick = {
                                    navController.navigate("artist/${viewModel.artistId}/albums")
                                }
                            )
                        }

                        item(key = "local_albums_list") {
                            val filteredLibraryAlbums = if (hideExplicit) {
                                libraryAlbums.filter { !it.album.explicit }
                            } else {
                                libraryAlbums
                            }
                            LazyRow(
                                contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                            ) {
                                items(
                                    items = filteredLibraryAlbums,
                                    key = { "local_album_${it.id}_${filteredLibraryAlbums.indexOf(it)}" }
                                ) { album ->
                                    AlbumGridItem(
                                        album = album,
                                        isActive = mediaMetadata?.album?.id == album.id,
                                        isPlaying = isPlaying,
                                        coroutineScope = coroutineScope,
                                        modifier = Modifier
                                            .combinedBounceClick(
                                                onClick = {
                                                    navController.navigate("album/${album.id}")
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        AlbumMenu(
                                                            originalAlbum = album,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss
                                                        )
                                                    }
                                                }
                                            )
                                            .animateItem()
                                    )
                                }
                            }
                        }
                    }
                } else {
                    artistPage?.sections?.fastForEach { section ->
                        if (section.items.isNotEmpty()) {
                            item(key = "section_${section.title}") {
                                // Redesigned header to match mockup "Section >" style
                                NavigationTitle(
                                    title = when (section.title) {
                                        "Songs" -> "Top Songs"
                                        "Popular" -> "Popular"
                                        else -> section.title
                                    },
                                    modifier = Modifier.animateItem(),
                                    onClick = section.moreEndpoint?.let {
                                        {
                                            navController.navigate(
                                                "artist/${viewModel.artistId}/items?browseId=${it.browseId}?params=${it.params}",
                                            )
                                        }
                                    },
                                )
                            }
                        }

                        // Check if this is a "Latest Release" style section to render as a card
                        val firstItem = section.items.firstOrNull()
                        if (section.title.contains("Latest", ignoreCase = true) && firstItem is AlbumItem) {
                            item(key = "featured_release") {
                                FeaturedReleaseCard(
                                    album = firstItem,
                                    onTint = onTint,
                                    onClick = { navController.navigate("album/${firstItem.id}") },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).animateItem()
                                )
                            }
                        } else if ((section.items.firstOrNull() as? SongItem)?.album != null) {
                            itemsIndexed(
                                items = section.items.distinctBy { it.id },
                                key = { _, it -> "youtube_song_${it.id}" },
                            ) { index, song ->
                                YouTubeListItem(
                                    item = song as SongItem,
                                    isActive = mediaMetadata?.id == song.id,
                                    isPlaying = isPlaying,
                                    shape = listItemShape(index, section.items.distinctBy { it.id }.size),
                                    flat = true,
                                    trailingContent = {
                                        androidx.compose.material3.IconButton(
                                            onClick = {
                                                menuState.show {
                                                    YouTubeSongMenu(
                                                        song = song,
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
                                    modifier = Modifier
                                        .combinedBounceClick(
                                            onClick = {
                                                if (song.id == mediaMetadata?.id) {
                                                    playerConnection.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        YouTubeQueue(
                                                            WatchEndpoint(videoId = song.id),
                                                            song.toMediaMetadata()
                                                        ),
                                                    )
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    YouTubeSongMenu(
                                                        song = song,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                        .animateItem(),
                                )
                            }
                        } else {
                            item(key = "section_list_${section.title}") {
                                LazyRow(
                                    contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                                ) {
                                    items(
                                        items = section.items.distinctBy { it.id },
                                        key = { "youtube_album_${it.id}" },
                                    ) { item ->
                                        YouTubeGridItem(
                                            item = item,
                                            isActive = when (item) {
                                                is SongItem -> mediaMetadata?.id == item.id
                                                is AlbumItem -> mediaMetadata?.album?.id == item.id
                                                else -> false
                                            },
                                            isPlaying = isPlaying,
                                            coroutineScope = coroutineScope,
                                            thumbnailRatio = 1f, // Use square thumbnails for all items in horizontal scroll
                                            modifier = Modifier
                                                .combinedBounceClick(
                                                    onClick = {
                                                        when (item) {
                                                            is SongItem ->
                                                                playerConnection.playQueue(
                                                                    YouTubeQueue(
                                                                        WatchEndpoint(videoId = item.id),
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
                                                )
                                                .animateItem(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val isScrollingUp = lazyListState.isScrollingUp()
        val showLocalFab = librarySongs.isNotEmpty() && libraryArtist?.artist?.isLocal != true
        
        // Library/Local Toggle FAB
        HideOnScrollFAB(
            visible = showLocalFab,
            lazyListState = lazyListState,
            icon = if (showLocal) R.drawable.language else R.drawable.library_music,
            onClick = {
                showLocal = showLocal.not()
                if (!showLocal && artistPage == null) viewModel.fetchArtistsFromYTM()
            }
        )
        
        // Play All FAB (Stacked above Library/Local FAB if visible)
        val canPlayAll = !isGuest && (
            (showLocal && librarySongs.isNotEmpty()) || 
            (!showLocal && artistPage?.sections?.any { 
                (it.items.firstOrNull() as? SongItem)?.album != null 
            } == true)
        )

        if (canPlayAll) {
             androidx.compose.animation.AnimatedVisibility(
                visible = isScrollingUp,
                enter = androidx.compose.animation.slideInVertically { it * 2 },
                exit = androidx.compose.animation.slideOutVertically { it * 2 },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .windowInsetsPadding(
                        LocalPlayerAwareWindowInsets.current
                            .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                    )
                    // Add padding to position it above the other FAB (56dp height + 16dp padding + 8dp spacing)
                    // If the other FAB is visible.
                    .padding(bottom = if (showLocalFab) 64.dp else 0.dp)
            ) {
                val onPlayAllClick: () -> Unit = {
                     if (showLocal) {
                         if (librarySongs.isNotEmpty()) {
                            playerConnection.playQueue(
                                ListQueue(
                                    title = libraryArtist?.artist?.name ?: "Unknown Artist",
                                    items = librarySongs.map { it.toMediaItem() }
                                )
                            )
                        }
                    } else if (artistPage != null) {
                        val songSection = artistPage.sections.find { section ->
                            (section.items.firstOrNull() as? SongItem)?.album != null
                        }
                        
                        val moreEndpoint = songSection?.moreEndpoint
                        if (moreEndpoint != null) {
                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                val result = YouTube.artistItems(moreEndpoint).getOrNull()
                                withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    if (result != null && result.items.isNotEmpty()) {
                                        val songs = result.items.filterIsInstance<SongItem>().map { it.toMediaItem() }
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = artistPage.artist.title,
                                                items = songs
                                            )
                                        )
                                    } else {
                                        // Fallback to loaded items
                                        val songs = songSection.items.filterIsInstance<SongItem>().map { it.toMediaItem() }
                                        if (songs.isNotEmpty()) {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = artistPage.artist.title,
                                                    items = songs
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            } else if (songSection != null) {
                            // Use loaded items if no more endpoint
                            val songs = songSection.items.filterIsInstance<SongItem>().map { it.toMediaItem() }
                            playerConnection.playQueue(
                                ListQueue(
                                    title = artistPage.artist.title,
                                    items = songs
                                )
                            )
                        } else {
                            // Fallback to shuffle endpoint (stripped) if no song section found
                            val shuffleEndpoint = artistPage.artist.shuffleEndpoint
                            if (shuffleEndpoint != null) {
                                val endpoint = if (shuffleEndpoint.playlistId != null) {
                                    WatchEndpoint(
                                        playlistId = shuffleEndpoint.playlistId,
                                        params = null, // Remove shuffle params to play in order
                                        videoId = null // Ensure videoId is null to start from beginning of playlist
                                    )
                                } else {
                                    shuffleEndpoint
                                }
                                playerConnection.playQueue(YouTubeQueue(endpoint))
                            }
                        }
                    }
                }

                val fabArtistId = artistPage?.artist?.id
                val isCurrentArtistForFab = fabArtistId != null &&
                    mediaMetadata?.artists?.any { it.id == fabArtistId } == true
                if (showLocalFab) {
                     androidx.compose.material3.SmallFloatingActionButton(
                        modifier = Modifier.padding(16.dp).offset(x = (-4).dp), // Align center with standard FAB (56dp vs 48dp)
                        onClick = {
                            if (isPlaying && isCurrentArtistForFab) {
                                playerConnection.player.pause()
                            } else if (isCurrentArtistForFab) {
                                playerConnection.player.play()
                            } else {
                                onPlayAllClick()
                            }
                        },
                        containerColor = onTint,
                        contentColor = tint
                    ) {
                        AnimatedPlayPauseIcon(
                            isPlaying = isPlaying && isCurrentArtistForFab,
                            tint = tint,
                            size = 24.dp,
                        )
                    }
                } else {
                    androidx.compose.material3.FloatingActionButton(
                        modifier = Modifier.padding(16.dp),
                        onClick = {
                            if (isPlaying && isCurrentArtistForFab) {
                                playerConnection.player.pause()
                            } else if (isCurrentArtistForFab) {
                                playerConnection.player.play()
                            } else {
                                onPlayAllClick()
                            }
                        },
                        containerColor = onTint,
                        contentColor = tint
                    ) {
                        AnimatedPlayPauseIcon(
                            isPlaying = isPlaying && isCurrentArtistForFab,
                            tint = tint,
                            size = 32.dp,
                        )
                    }
                }
            }
        }


        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .align(Alignment.BottomCenter)
        )

        // Floating glass back/share buttons over the hero art, replacing the
        // Material TopAppBar — always visible, no title-bar-on-scroll behavior.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            GlassCircleButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }

            GlassCircleButton(
                onClick = {
                    viewModel.artistPage?.artist?.shareLink?.let { link ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Artist Link", link)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, R.string.link_copied, Toast.LENGTH_SHORT).show()
                    }
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.link),
                    contentDescription = null,
                )
            }
        }
      }
    }
}

@Composable
fun FeaturedReleaseCard(
    album: AlbumItem,
    onTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = album.thumbnail.resize(400, 400),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.year?.toString() ?: "Latest Release",
                    style = MaterialTheme.typography.labelMedium,
                    color = onTint.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = onTint,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Album",
                    style = MaterialTheme.typography.bodySmall,
                    color = onTint.copy(alpha = 0.5f),
                    maxLines = 1
                )
            }

            IconButton(onClick = onClick) {
                Icon(
                    painter = painterResource(R.drawable.add),
                    contentDescription = null,
                    tint = onTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
