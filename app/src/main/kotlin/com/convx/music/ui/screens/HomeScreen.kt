/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.Text
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.crossfade
import coil3.request.ImageRequest
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.models.YTItem
import com.music.innertube.pages.ExplorePage
import com.music.innertube.pages.HomePage
import com.music.innertube.pages.MoodAndGenres
import com.music.innertube.utils.completed
import com.music.innertube.utils.parseCookieString
import com.music.innertube.YouTube
import com.convx.music.constants.GridItemSize
import com.convx.music.constants.GridItemsSizeKey
import com.convx.music.constants.GridThumbnailHeight
import com.convx.music.constants.InnerTubeCookieKey
import com.convx.music.constants.ListItemHeight
import com.convx.music.constants.ThumbnailRoundedShape
import com.convx.music.constants.ListThumbnailSize
import com.convx.music.constants.RandomizeHomeOrderKey
import com.convx.music.constants.ShowHomeFabKey
import com.convx.music.constants.HomeCardCornerRadiusOverrideKey
import com.convx.music.constants.SpeedDialCardHeightOverrideKey
import com.convx.music.constants.SpeedDialColumnsOverrideKey
import com.convx.music.constants.SmallGridThumbnailHeight
import com.convx.music.constants.ThumbnailCornerRadius
import com.convx.music.db.MusicDatabase
import com.convx.music.db.entities.Album
import com.convx.music.db.entities.Artist
import com.convx.music.db.entities.LocalItem
import com.convx.music.db.entities.Playlist
import com.convx.music.db.entities.PlaylistEntity
import com.convx.music.db.entities.PlaylistSongMap
import com.convx.music.db.entities.Song
import com.convx.music.extensions.toMediaItem
import com.convx.music.ui.utils.bleedStart
import com.convx.music.ui.utils.plusStart
import com.convx.music.ui.utils.bounceClick
import com.convx.music.ui.utils.combinedBounceClick
import com.convx.music.LocalDatabase
import com.convx.music.LocalPlayerAwareWindowInsets
import com.convx.music.LocalPlayerConnection
import com.convx.music.models.MediaMetadata
import com.convx.music.models.SimilarRecommendation
import com.convx.music.models.toMediaMetadata
import com.convx.music.playback.PlayerConnection
import com.convx.music.playback.queues.ListQueue
import com.convx.music.playback.queues.LocalAlbumRadio
import com.convx.music.playback.queues.YouTubeAlbumRadio
import com.convx.music.playback.queues.YouTubeQueue
import com.convx.music.R
import com.convx.music.ui.component.AlbumGridItem
import com.convx.music.ui.component.ArtistGridItem
import com.convx.music.ui.component.HideOnScrollFAB
import com.convx.music.ui.component.HomeHeroCard
import com.convx.music.ui.component.HomeImageBackground
import com.convx.music.ui.component.LocalBottomSheetPageState
import com.convx.music.ui.component.LocalMenuState
import com.convx.music.ui.component.MenuState
import com.convx.music.ui.component.NavigationTitle
import com.convx.music.ui.component.RandomizeGridItem
import com.convx.music.ui.component.rememberAppBackgroundTint
import com.convx.music.ui.component.shimmer.GridItemPlaceHolder
import com.convx.music.ui.component.shimmer.ShimmerHost
import com.convx.music.ui.component.shimmer.TextPlaceholder
import com.convx.music.ui.component.SongGridItem
import com.convx.music.ui.component.SongListItem
import com.convx.music.ui.component.SpeedDialGridItem
import com.convx.music.ui.component.YouTubeGridItem
import com.convx.music.ui.component.YouTubeListItem
import com.convx.music.ui.menu.AlbumMenu
import com.convx.music.ui.menu.ArtistMenu
import com.convx.music.ui.menu.SongMenu
import com.convx.music.ui.menu.YouTubeAlbumMenu
import com.convx.music.ui.menu.YouTubeArtistMenu
import com.convx.music.ui.menu.YouTubePlaylistMenu
import com.convx.music.ui.menu.YouTubeSongMenu
import com.convx.music.ui.utils.SnapLayoutInfoProvider
import com.convx.music.ui.component.shapes.ContinuousRoundedRectangle
import com.convx.music.ui.theme.AppleTokens
import com.convx.music.ui.utils.resize
import com.convx.music.utils.listItemShape
import com.convx.music.utils.rememberEnumPreference
import com.convx.music.utils.rememberPreference
import com.convx.music.viewmodels.CommunityPlaylistItem
import com.convx.music.viewmodels.HomeViewModel
import kotlin.math.min
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.convx.music.viewmodels.DailyDiscoverItem


/**
 * Home item budget. Content alone (glass bypassed) measured 14ms per frame against an
 * 8.33ms/120Hz budget, driven by how many rich rows are composed at once. The two song
 * grids used to stack 4 rows each, so ~40 rows could be live simultaneously. Two rows
 * matches the survey's "2x2 grid" preference for recent activity and halves that.
 */
private const val SongGridRows = 2
private const val QuickPicksGridRows = 4
private const val MaxSimilarSections = 2
private const val MaxHomePageSections = 3

sealed class HomeSection(val id: String, val baseWeight: Int) {
    /** The "star of the day" card. Always first, never shuffled. */
    data object Hero : HomeSection("hero", 110)
    data object SpeedDial : HomeSection("speed_dial", 100)
    data object QuickPicks : HomeSection("quick_picks", 90)
    data object DailyDiscover : HomeSection("daily_discover", 80)
    data object KeepListening : HomeSection("keep_listening", 50)
    data object AccountPlaylists : HomeSection("account_playlists", 40)
    data object ForgottenFavorites : HomeSection("forgotten_favorites", 30)
    data object FromTheCommunity : HomeSection("from_the_community", 20)
    data class SimilarRecommendation(val index: Int) : HomeSection("similar_recommendation_$index", 10)
    data class HomePageSection(val index: Int) : HomeSection("home_page_section_$index", 10)
    data object MoodAndGenres : HomeSection("mood_and_genres", 5)
}

@Composable
fun CommunityPlaylistCard(
    item: CommunityPlaylistItem,
    onClick: () -> Unit,
    onSongClick: (SongItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current
    val scope = rememberCoroutineScope()

    val containerColor = Color.Transparent
    val onSurface = LocalContentColor.current

    val dbPlaylistFlow = remember(item.playlist.id) { database.playlistByBrowseId(item.playlist.id) }
    val dbPlaylist by dbPlaylistFlow.collectAsStateWithLifecycle(initialValue =null)
    val isBookmarked = dbPlaylist?.playlist?.bookmarkedAt != null

    Card(
        modifier = modifier
            .width(360.dp)
            .height(470.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        shape = ContinuousRoundedRectangle(AppleTokens.CardCornerLarge),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .background(onSurface.copy(alpha = 0.05f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppleTokens.Gutter),
                horizontalArrangement = Arrangement.spacedBy(AppleTokens.ItemGap)
            ) {
                // 2x2 Grid of thumbnails
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(ThumbnailRoundedShape)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.weight(1f)) {
                            AsyncImage(
                                model = item.songs.getOrNull(0)?.thumbnail?.resize(544, 544),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                            )
                            AsyncImage(
                                model = item.songs.getOrNull(1)?.thumbnail?.resize(544, 544),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                            )
                        }
                        Row(modifier = Modifier.weight(1f)) {
                            AsyncImage(
                                model = item.songs.getOrNull(2)?.thumbnail?.resize(544, 544),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                            )
                            AsyncImage(
                                model = item.songs.getOrNull(3)?.thumbnail?.resize(544, 544),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = item.playlist.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.playlist.author?.name ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = AppleTokens.Gutter)
            ) {
                item.songs.take(3).forEachIndexed { idx, song ->
                    if (idx > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 68.dp),
                            color = AppleTokens.Divider,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedBounceClick(onClick = { onSongClick(song) }),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AsyncImage(
                            model = song.thumbnail.resize(544, 544),
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(ThumbnailRoundedShape),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = onSurface,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artists.joinToString(", ") { it.name },
                                style = MaterialTheme.typography.bodySmall,
                                color = onSurface.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppleTokens.Gutter),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                IconButton(
                    onClick = {
                        item.playlist.playEndpoint?.let {
                            playerConnection?.playQueue(YouTubeQueue(it))
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(AppleTokens.CardSecondary, CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_widget_play),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = {
                        item.playlist.radioEndpoint?.let {
                            playerConnection?.playQueue(YouTubeQueue(it))
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(AppleTokens.CardSecondary, CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.radio),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            if (dbPlaylist?.playlist == null) {
                                database.transaction {
                                    val playlistEntity = PlaylistEntity(
                                        name = item.playlist.title,
                                        browseId = item.playlist.id,
                                        thumbnailUrl = item.playlist.thumbnail,
                                        remoteSongCount = item.playlist.songCountText?.split(" ")?.firstOrNull()?.toIntOrNull(),
                                        playEndpointParams = item.playlist.playEndpoint?.params,
                                        shuffleEndpointParams = item.playlist.shuffleEndpoint?.params,
                                        radioEndpointParams = item.playlist.radioEndpoint?.params
                                    ).toggleLike()
                                    insert(playlistEntity)
                                    scope.launch(Dispatchers.IO) {
                                        item.songs.ifEmpty {
                                            YouTube.playlist(item.playlist.id).completed()
                                                .getOrNull()?.songs.orEmpty()
                                        }.map { it.toMediaMetadata() }
                                            .onEach(::insert)
                                            .mapIndexed { index, song ->
                                                PlaylistSongMap(
                                                    songId = song.id,
                                                    playlistId = playlistEntity.id,
                                                    position = index,
                                                    setVideoId = song.setVideoId
                                                )
                                            }
                                            .forEach(::insert)
                                    }
                                }
                            } else {
                                database.transaction {
                                    val currentPlaylist = dbPlaylist!!.playlist
                                    update(currentPlaylist.toggleLike())
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(AppleTokens.CardSecondary, CircleShape)
                ) {
                    Icon(
                        painter = painterResource(if (isBookmarked) R.drawable.library_add_check else R.drawable.library_add),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DailyDiscoverCard(
    dailyDiscover: com.convx.music.viewmodels.DailyDiscoverItem,
    onClick: () -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val database = LocalDatabase.current
    val playCountFlow = remember(dailyDiscover.recommendation.id) { database.getLifetimePlayCount(dailyDiscover.recommendation.id) }
    val playCount by playCountFlow.collectAsStateWithLifecycle(initialValue =0)
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current

    val song = dailyDiscover.recommendation as? SongItem
    val playsString = stringResource(R.string.plays)

    Card(
        modifier = modifier
            .fillMaxSize()
            .clip(ContinuousRoundedRectangle(AppleTokens.CardCornerLarge))
            .combinedBounceClick(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (song != null) {
                        menuState.show {
                            YouTubeSongMenu(
                                song = song,
                                navController = navController,
                                onDismiss = { menuState.dismiss() }
                            )
                        }
                    }
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
        ),
        shape = ContinuousRoundedRectangle(AppleTokens.CardCornerLarge)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(dailyDiscover.recommendation.thumbnail?.resize(1200, 1200))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
            )

            if (maxWidth > 200.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = dailyDiscover.recommendation.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = buildString {
                                append((dailyDiscover.recommendation as? SongItem)?.artists?.joinToString(", ") { it.name } ?: "")
                                if (playCount > 0) {
                                    append(" • $playCount $playsString")
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    val messages = listOf(
                        R.string.daily_discover_sounds_like,
                        R.string.daily_discover_because_you_listen_to,
                        R.string.daily_discover_similar_to,
                        R.string.daily_discover_based_on,
                        R.string.daily_discover_for_fans_of
                    )
                    val messageRes = remember(dailyDiscover.seed.id) {
                        messages[kotlin.math.abs(dailyDiscover.seed.id.hashCode()) % messages.size]
                    }

                    Text(
                        text = stringResource(messageRes, "${dailyDiscover.seed.title} • ${dailyDiscover.seed.artists.joinToString(", ") { it.name }}"),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    val quickPicks by viewModel.quickPicks.collectAsStateWithLifecycle()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsStateWithLifecycle()
    val keepListening by viewModel.keepListening.collectAsStateWithLifecycle()
    val similarRecommendations by viewModel.similarRecommendations.collectAsStateWithLifecycle()
    val accountPlaylists by viewModel.accountPlaylists.collectAsStateWithLifecycle()
    val homePage by viewModel.homePage.collectAsStateWithLifecycle()
    val explorePage by viewModel.explorePage.collectAsStateWithLifecycle()
    val dailyDiscover by viewModel.dailyDiscover.collectAsStateWithLifecycle()
    val communityPlaylists by viewModel.communityPlaylists.collectAsStateWithLifecycle()

    val allLocalItems by viewModel.allLocalItems.collectAsStateWithLifecycle()
    val allYtItems by viewModel.allYtItems.collectAsStateWithLifecycle()
    val speedDialItems by viewModel.speedDialItems.collectAsStateWithLifecycle()
    val selectedChip by viewModel.selectedChip.collectAsStateWithLifecycle()

    val isLoading: Boolean by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isRandomizing by viewModel.isRandomizing.collectAsStateWithLifecycle()
    val pullRefreshState = rememberPullToRefreshState()

    val quickPicksLazyGridState = rememberLazyGridState()
    val forgottenFavoritesLazyGridState = rememberLazyGridState()
    val keepListeningLazyGridState = rememberLazyGridState()

    val accountName by viewModel.accountName.collectAsStateWithLifecycle()
    val accountImageUrl by viewModel.accountImageUrl.collectAsStateWithLifecycle()
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    // Default flipped to false: 2 of 3 survey responses asked for a fixed order
    // ("fixed order, I can find things faster"); the third wanted only the top section
    // pinned. Users who explicitly enabled shuffling keep it.
    val (randomizeHomeOrder) = rememberPreference(RandomizeHomeOrderKey, false)

    val shouldShowWrappedCard by viewModel.showWrappedCard.collectAsStateWithLifecycle()
    val wrappedState by viewModel.wrappedManager.state.collectAsStateWithLifecycle()
    val isWrappedDataReady = wrappedState.isDataReady

    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val url = if (isLoggedIn) accountImageUrl else null

    val scope = rememberCoroutineScope()
    // Track randomization job. MutableState<Job?> so SpeedDial's section composable
    // (a LazyListScope extension, not a @Composable) can swap the job in and out.
    val randomizeJob = remember { mutableStateOf<Job?>(null) }

    val lazylistState = rememberLazyListState()
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    val currentGridHeight = if (gridItemSize == GridItemSize.BIG) GridThumbnailHeight else SmallGridThumbnailHeight
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsStateWithLifecycle()

    val wrappedDismissed by backStackEntry?.savedStateHandle?.getStateFlow("wrapped_seen", false)
        ?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }

    var randomSeed by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            randomSeed = System.currentTimeMillis()
        }
    }

    val foundInSettings = stringResource(R.string.found_in_settings_content)
    LaunchedEffect(wrappedDismissed) {
        if (wrappedDismissed) {
            viewModel.markWrappedAsSeen()
            scope.launch {
                snackbarHostState.showSnackbar(foundInSettings)
            }
            backStackEntry?.savedStateHandle?.set("wrapped_seen", false) // Reset the value
        }
    }

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazylistState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { lazylistState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                val len = lazylistState.layoutInfo.totalItemsCount
                if (lastVisibleIndex != null && lastVisibleIndex >= len - 3) {
                    viewModel.loadMoreYouTubeItems(homePage?.continuation)
                }
            }
    }

    NetworkReload(
        onReload = viewModel::refresh
    )

    if (selectedChip != null) {
        BackHandler {
            // if a chip is selected, go back to the normal homepage first
            viewModel.toggleChip(selectedChip)
        }
    }

    val localGridItem: @Composable (LocalItem) -> Unit = {
        when (it) {
            is Song -> SongGridItem(
                song = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedBounceClick(
                        onClick = {
                            if (it.id == mediaMetadata?.id) {
                                playerConnection.togglePlayPause()
                            } else {
                                playerConnection.playQueue(
                                    YouTubeQueue.radio(it.toMediaMetadata()),
                                )
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(
                                HapticFeedbackType.LongPress,
                            )
                            menuState.show {
                                SongMenu(
                                    originalSong = it,
                                    navController = navController,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                    ),
                isActive = it.id == mediaMetadata?.id,
                isPlaying = isPlaying,
            )

            is Album -> AlbumGridItem(
                album = it,
                isActive = it.id == mediaMetadata?.album?.id,
                isPlaying = isPlaying,
                coroutineScope = scope,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedBounceClick(
                        onClick = {
                            navController.navigate("album/${it.id}")
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                AlbumMenu(
                                    originalAlbum = it,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    )
            )

            is Artist -> ArtistGridItem(
                artist = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedBounceClick(
                        onClick = {
                            navController.navigate("artist/${it.id}")
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(
                                HapticFeedbackType.LongPress,
                            )
                            menuState.show {
                                ArtistMenu(
                                    originalArtist = it,
                                    coroutineScope = scope,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                    ),
            )

            is Playlist -> {}
        }
    }

    val ytGridItem: @Composable (YTItem) -> Unit = { item ->
        val isActive = remember(item.id, mediaMetadata?.album?.id, mediaMetadata?.id) {
            item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id)
        }
        YouTubeGridItem(
            item = item,
            isActive = isActive,
            isPlaying = isPlaying,
            coroutineScope = scope,
            thumbnailRatio = 1f,
            modifier = Modifier
                .combinedBounceClick(
                    onClick = {
                        when (item) {
                            is SongItem -> playerConnection.playQueue(
                                YouTubeQueue(
                                    item.endpoint ?: WatchEndpoint(
                                        videoId = item.id
                                    ), item.toMediaMetadata()
                                )
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
                                is SongItem -> YouTubeSongMenu(
                                    song = item,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )

                                is AlbumItem -> YouTubeAlbumMenu(
                                    albumItem = item,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )

                                is ArtistItem -> YouTubeArtistMenu(
                                    artist = item,
                                    onDismiss = menuState::dismiss
                                )

                                is PlaylistItem -> YouTubePlaylistMenu(
                                    playlist = item,
                                    coroutineScope = scope,
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    }
                )
        )
    }

    val homeSections = remember(
        randomizeHomeOrder,
        randomSeed,
        speedDialItems,
        quickPicks,
        dailyDiscover,
        keepListening,
        accountPlaylists,
        forgottenFavorites,
        communityPlaylists,
        similarRecommendations,
        homePage?.sections,
        explorePage?.moodAndGenres
    ) {
        val list = mutableListOf<HomeSection>()

        if (quickPicks?.isNotEmpty() == true) list.add(HomeSection.Hero)
        if (speedDialItems.isNotEmpty()) list.add(HomeSection.SpeedDial)
        if (quickPicks?.isNotEmpty() == true) list.add(HomeSection.QuickPicks)
        if (communityPlaylists?.isNotEmpty() == true) list.add(HomeSection.FromTheCommunity)
        if (dailyDiscover?.isNotEmpty() == true) list.add(HomeSection.DailyDiscover)
        if (keepListening?.isNotEmpty() == true) list.add(HomeSection.KeepListening)
        if (accountPlaylists?.isNotEmpty() == true) list.add(HomeSection.AccountPlaylists)
        if (forgottenFavorites?.isNotEmpty() == true) list.add(HomeSection.ForgottenFavorites)

        // Capped. The survey's most concrete complaint was "I have to see 4 to 6
        // similar-to sections that I don't want", and each extra section is ~12 more
        // simultaneously composed rows — the single largest driver of Home's frame cost
        // (content alone measured 14ms/frame against an 8.33ms budget).
        similarRecommendations?.indices?.take(MaxSimilarSections)?.forEach { i ->
            list.add(HomeSection.SimilarRecommendation(i))
        }

        homePage?.sections?.indices?.take(MaxHomePageSections)?.forEach { i ->
            list.add(HomeSection.HomePageSection(i))
        }

        if (explorePage?.moodAndGenres != null) list.add(HomeSection.MoodAndGenres)

        if (randomizeHomeOrder) {
            list.sortedByDescending { section ->
                // Use a stable seed for each section based on the session seed + section ID hash
                // This ensures the weight for a specific section remains constant during a session (until refresh)
                // even if other sections appear/disappear, preventing jumping.
                val sectionRandom = Random(randomSeed + section.id.hashCode())

                // Flatten the base values to allow for more overlap and variation
                // All "main" sections start closer together
                val base = when (section) {
                    // Pinned above everything, even with shuffling on — the one survey
                    // response that wanted shuffling still asked to keep the top fixed.
                    HomeSection.Hero -> 10_000

                    HomeSection.SpeedDial,
                    HomeSection.QuickPicks,
                    HomeSection.DailyDiscover -> 500 // Top tier starts equal

                    HomeSection.KeepListening,
                    HomeSection.AccountPlaylists,
                    HomeSection.ForgottenFavorites,
                    HomeSection.FromTheCommunity -> 300 // Middle tier starts equal

                    else -> 100 // Bottom tier
                }

                val modifier = when (section) {
                    // Top tier: High variance to allow shuffling among themselves
                    // Range: [500-200, 500+400] = [300, 900]
                    HomeSection.SpeedDial,
                    HomeSection.QuickPicks,
                    HomeSection.DailyDiscover -> sectionRandom.nextInt(-200, 400)

                    // Middle tier: Can jump up to challenge top tier, or drop lower
                    // Range: [300-100, 300+400] = [200, 700]
                    // This allows them to occasionally appear above a "bad roll" top tier item
                    HomeSection.KeepListening,
                    HomeSection.AccountPlaylists,
                    HomeSection.ForgottenFavorites,
                    HomeSection.FromTheCommunity -> sectionRandom.nextInt(-100, 400)

                    // Bottom tier: Standard variance
                    else -> sectionRandom.nextInt(-50, 50)
                }
                base + modifier
            }
        } else {
            val defaultOrder = mapOf(
                HomeSection.Hero to 110,
                HomeSection.SpeedDial to 100,
                HomeSection.QuickPicks to 90,
                HomeSection.FromTheCommunity to 80,
                HomeSection.DailyDiscover to 70,
                HomeSection.KeepListening to 60,
                HomeSection.AccountPlaylists to 50,
                HomeSection.ForgottenFavorites to 40,
                HomeSection.MoodAndGenres to 10
            )

            list.sortedByDescending { section ->
                when(section) {
                    is HomeSection.SimilarRecommendation -> 30 - section.index
                    is HomeSection.HomePageSection -> 20 - section.index
                    else -> defaultOrder[section] ?: 0
                }
            }
        }
    }

    // One batched query per list instead of a per-row database.song() Flow. The rows
    // below were each opening their own Room observation for the same ~10 ids, and
    // every visible tile subscribed for the whole time it stayed composed. A single
    // songsByIds flow still re-emits when any of those rows change, so the live-update
    // behaviour is unchanged — just one observer instead of N.
    val quickPicksSongMap = rememberSongsById(
        database,
        remember(quickPicks) { quickPicks.orEmpty().map { it.id } },
    )
    val forgottenFavoritesSongMap = rememberSongsById(
        database,
        remember(forgottenFavorites) { forgottenFavorites.orEmpty().map { it.id } },
    )

    LaunchedEffect(quickPicks) {
        quickPicksLazyGridState.scrollToItem(0)
    }

    LaunchedEffect(forgottenFavorites) {
        forgottenFavoritesLazyGridState.scrollToItem(0)
    }

    PullToRefreshBox(
        state = pullRefreshState,
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        indicator = {
            // Material3's expressive LoadingIndicator morphs its shape on an
            // infinite animation for as long as it is composed — it does not
            // gate on isRefreshing. Left in the tree it awaits a frame forever,
            // so the whole app recomposed and redrew every vsync at idle (~50
            // draws/s on a motionless Home) for an indicator scaled to nothing.
            // Compose it only while it can actually be seen.
            if (isRefreshing || pullRefreshState.distanceFraction > 0f) {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
                )
            }
        }
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopStart
        ) {
            // Flat fill behind everything — a no-op (matches the default
            // Scaffold background) unless the user picked a specific custom
            // theme color, in which case that becomes Home's actual background.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(rememberAppBackgroundTint(MaterialTheme.colorScheme.background))
            )
            HomeImageBackground(contentLoaded = homePage != null)

            val horizontalLazyGridItemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
            val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor
            val quickPicksSnapLayoutInfoProvider = remember(quickPicksLazyGridState) {
                SnapLayoutInfoProvider(
                    lazyGridState = quickPicksLazyGridState,
                    positionInLayout = { layoutSize, itemSize ->
                        (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                    }
                )
            }
            val forgottenFavoritesSnapLayoutInfoProvider = remember(forgottenFavoritesLazyGridState) {
                SnapLayoutInfoProvider(
                    lazyGridState = forgottenFavoritesLazyGridState,
                    positionInLayout = { layoutSize, itemSize ->
                        (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                    }
                )
            }

            // Space the floating side bar occupies. The vertical list keeps it as
            // content padding so rows stay clear of the panel; the carousels below
            // give it back with bleedStart so their cards run underneath the glass
            // instead of stopping at its edge.
            val sideInset = LocalPlayerAwareWindowInsets.current
                .asPaddingValues()
                .calculateStartPadding(LocalLayoutDirection.current)

            val deps = HomeSectionDeps(
                viewModel = viewModel,
                navController = navController,
                playerConnection = playerConnection,
                menuState = menuState,
                haptic = haptic,
                scope = scope,
                sideInset = sideInset,
                availableWidth = maxWidth,
                horizontalLazyGridItemWidth = horizontalLazyGridItemWidth,
                quickPicksGridState = quickPicksLazyGridState,
                quickPicksSnapLayoutInfoProvider = quickPicksSnapLayoutInfoProvider,
                forgottenFavoritesGridState = forgottenFavoritesLazyGridState,
                forgottenFavoritesSnapLayoutInfoProvider = forgottenFavoritesSnapLayoutInfoProvider,
                keepListeningGridState = keepListeningLazyGridState,
                currentGridHeight = currentGridHeight,
                localGridItem = localGridItem,
                ytGridItem = ytGridItem,
            )

            LazyColumn(
                state = lazylistState,
                modifier = Modifier,
                // Sections had nothing but NavigationTitle's own 12dp padding
                // between them — ran into each other, felt cramped.
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            ) {
                if (selectedChip == null) {
                    item(key = "wrapped_card") {
                        AnimatedVisibility(visible = shouldShowWrappedCard) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = LocalContentColor.current.copy(alpha = 0.1f),
                                ),
                                shape = ContinuousRoundedRectangle(AppleTokens.CardCornerLarge)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isWrappedDataReady) {
                                        val bbhFont = try {
                                            FontFamily(Font(R.font.bbh_bartle_regular))
                                        } catch (e: Exception) {
                                            FontFamily.Default
                                        }
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                                        ) {
                                            Text(
                                                text = stringResource(R.string.wrapped_ready_title),
                                                style = MaterialTheme.typography.headlineLarge.copy(
                                                    fontFamily = bbhFont,
                                                    textAlign = TextAlign.Center
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = stringResource(R.string.wrapped_ready_subtitle),
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    textAlign = TextAlign.Center
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Button(onClick = {
                                                navController.navigate("wrapped")
                                            }) {
                                                Text(stringResource(R.string.open))
                                            }
                                        }
                                    } else {
                                        ContainedLoadingIndicator()
                                    }
                                }
                            }
                        }
                    }
                }

                homeSectionsContent(
                    sections = homeSections,
                    deps = deps,
                    speedDialItems = speedDialItems,
                    quickPicks = quickPicks,
                    forgottenFavorites = forgottenFavorites,
                    dailyDiscover = dailyDiscover,
                    keepListening = keepListening,
                    accountPlaylists = accountPlaylists,
                    communityPlaylists = communityPlaylists,
                    similarRecommendations = similarRecommendations,
                    homePage = homePage,
                    explorePage = explorePage,
                    mediaMetadata = mediaMetadata,
                    isPlaying = isPlaying,
                    isRandomizing = isRandomizing,
                    randomizeJob = randomizeJob,
                    quickPicksSongMap = quickPicksSongMap,
                    forgottenFavoritesSongMap = forgottenFavoritesSongMap,
                    accountName = accountName,
                    url = url,
                )
                // Only while a fetch is actually in flight. The old condition also kept this
                // composed whenever a continuation merely EXISTED, so parking at the bottom of
                // Home left the shimmer mounted forever — and its infinite transition drove
                // ~65 recompositions/s with nothing on screen moving (328 Recomposer:animation
                // sections in a 5s trace). Reaching the bottom triggers loadMoreYouTubeItems
                // immediately, so the placeholder still appears for every real load.
                if (isLoading) {
                    item(key = "loading_shimmer") {
                        ShimmerHost(
                            modifier = Modifier.animateItem()
                        ) {
                            repeat(2) {
                                TextPlaceholder(
                                    height = 36.dp,
                                    modifier = Modifier
                                        .padding(AppleTokens.Gutter)
                                        .width(250.dp),
                                )
                                LazyRow(
                                    modifier = Modifier.bleedStart(sideInset),
                                    contentPadding = PaddingValues(horizontal = AppleTokens.ItemGap / 2)
                                        .plusStart(sideInset),
                                ) {
                                    items(4) {
                                        GridItemPlaceHolder()
                                    }
                                }
                            }

                            TextPlaceholder(
                                height = 36.dp,
                                modifier = Modifier
                                    .padding(AppleTokens.Gutter)
                                    .width(250.dp),
                            )
                            repeat(4) {
                                Row {
                                    repeat(2) {
                                        TextPlaceholder(
                                            height = MoodAndGenresButtonHeight,
                                            shape = RoundedCornerShape(AppleTokens.Control),
                                            modifier = Modifier
                                                .padding(AppleTokens.ItemGap / 2)
                                                .width(200.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }

            val (showHomeFab) = rememberPreference(ShowHomeFabKey, true)
            if (showHomeFab) {
                HideOnScrollFAB(
                    visible = allLocalItems.isNotEmpty() || allYtItems.isNotEmpty(),
                    lazyListState = lazylistState,
                    icon = R.drawable.shuffle,
                    onClick = {
                        val local = when {
                            allLocalItems.isNotEmpty() && allYtItems.isNotEmpty() -> Random.nextFloat() < 0.5
                            allLocalItems.isNotEmpty() -> true
                            else -> false
                        }
                        scope.launch(Dispatchers.Main) {
                            if (local) {
                                when (val luckyItem = allLocalItems.random()) {
                                    is Song -> playerConnection.playQueue(YouTubeQueue.radio(luckyItem.toMediaMetadata()))
                                    is Album -> {
                                        val albumWithSongs = withContext(Dispatchers.IO) {
                                            database.albumWithSongs(luckyItem.id).first()
                                        }
                                        albumWithSongs?.let {
                                            playerConnection.playQueue(LocalAlbumRadio(it))
                                        }
                                    }
                                    is Artist -> {}
                                    is Playlist -> {}
                                }
                            } else {
                                when (val luckyItem = allYtItems.random()) {
                                    is SongItem -> playerConnection.playQueue(YouTubeQueue.radio(luckyItem.toMediaMetadata()))
                                    is AlbumItem -> playerConnection.playQueue(YouTubeAlbumRadio(luckyItem.playlistId))
                                    is ArtistItem -> luckyItem.radioEndpoint?.let {
                                        playerConnection.playQueue(YouTubeQueue(it))
                                    }
                                    is PlaylistItem -> luckyItem.playEndpoint?.let {
                                        playerConnection.playQueue(YouTubeQueue(it))
                                    }
                                }
                            }
                        }
                    },
                    onRecognitionClick = {
                        navController.navigate("recognition")
                    }
                )
            }
        }
    }
}

/**
 * Stable cross-section plumbing. Cheap plain-data re-created on recomposition; holds
 * the non-UI references and per-row composables each section needs. Volatile data
 * (media metadata, song maps, lists) travels as explicit parameters instead so the
 * sections stay free of composition-local reads.
 */
@OptIn(ExperimentalFoundationApi::class)
private data class HomeSectionDeps(
    val viewModel: HomeViewModel,
    val navController: NavController,
    val playerConnection: PlayerConnection,
    val menuState: MenuState,
    val haptic: HapticFeedback,
    val scope: CoroutineScope,
    val sideInset: Dp,
    val availableWidth: Dp,
    val horizontalLazyGridItemWidth: Dp,
    val quickPicksGridState: LazyGridState,
    val quickPicksSnapLayoutInfoProvider: SnapLayoutInfoProvider,
    val forgottenFavoritesGridState: LazyGridState,
    val forgottenFavoritesSnapLayoutInfoProvider: SnapLayoutInfoProvider,
    val keepListeningGridState: LazyGridState,
    val currentGridHeight: Dp,
    val localGridItem: @Composable (LocalItem) -> Unit,
    val ytGridItem: @Composable (YTItem) -> Unit,
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
private fun LazyListScope.homeSectionsContent(
    sections: List<HomeSection>,
    deps: HomeSectionDeps,
    speedDialItems: List<YTItem>,
    quickPicks: List<Song>?,
    forgottenFavorites: List<Song>?,
    dailyDiscover: List<DailyDiscoverItem>?,
    keepListening: List<LocalItem>?,
    accountPlaylists: List<PlaylistItem>?,
    communityPlaylists: List<CommunityPlaylistItem>?,
    similarRecommendations: List<SimilarRecommendation>?,
    homePage: HomePage?,
    explorePage: ExplorePage?,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    isRandomizing: Boolean,
    randomizeJob: MutableState<Job?>,
    quickPicksSongMap: Map<String, Song>,
    forgottenFavoritesSongMap: Map<String, Song>,
    accountName: String,
    url: String?,
) {
    sections.forEach { section ->
        when (section) {
            HomeSection.Hero -> heroSection(deps, quickPicks)
            HomeSection.SpeedDial -> speedDialSection(
                deps = deps,
                items = speedDialItems,
                mediaMetadata = mediaMetadata,
                isPlaying = isPlaying,
                isRandomizing = isRandomizing,
                randomizeJob = randomizeJob,
            )
            HomeSection.QuickPicks -> quickPicksSection(deps, quickPicks, mediaMetadata, isPlaying, quickPicksSongMap)
            HomeSection.FromTheCommunity -> communityPlaylistsSection(deps, communityPlaylists)
            HomeSection.DailyDiscover -> dailyDiscoverSection(deps, dailyDiscover)
            HomeSection.KeepListening -> keepListeningSection(deps, keepListening, mediaMetadata, isPlaying)
            HomeSection.AccountPlaylists -> accountPlaylistsSection(
                deps = deps,
                accountPlaylists = accountPlaylists,
                accountName = accountName,
                url = url,
            )
            HomeSection.ForgottenFavorites -> forgottenFavoritesSection(
                deps, forgottenFavorites, mediaMetadata, isPlaying, forgottenFavoritesSongMap,
            )
            is HomeSection.SimilarRecommendation -> similarRecommendationsSection(
                section, deps, similarRecommendations,
            )
            is HomeSection.HomePageSection -> homePageSection(section, deps, homePage, mediaMetadata, isPlaying)
            HomeSection.MoodAndGenres -> moodAndGenresSection(deps, explorePage)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.heroSection(
    deps: HomeSectionDeps,
    quickPicks: List<Song>?,
) {
    quickPicks?.firstOrNull()?.let { top ->
        item(key = "hero_card") {
            val heroSubtitle = remember(top.id) {
                top.artists.joinToString { it.name }
            }
            HomeHeroCard(
                title = top.song.title,
                subtitle = heroSubtitle,
                thumbnailUrl = top.song.thumbnailUrl,
                onPlay = {
                    deps.playerConnection.playQueue(
                        ListQueue(
                            title = top.song.title,
                            items = quickPicks.orEmpty().map { it.toMediaItem() },
                        )
                    )
                },
                onShuffle = {
                    deps.playerConnection.playQueue(
                        ListQueue(
                            title = top.song.title,
                            items = quickPicks.orEmpty().shuffled().map { it.toMediaItem() },
                        )
                    )
                },
                onClick = {
                    deps.playerConnection.playQueue(
                        ListQueue(
                            title = top.song.title,
                            items = listOf(top.toMediaItem()),
                        )
                    )
                },
                modifier = Modifier
                    .padding(horizontal = AppleTokens.Gutter)
                    .animateItem(),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
private fun LazyListScope.speedDialSection(
    deps: HomeSectionDeps,
    items: List<YTItem>,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    isRandomizing: Boolean,
    randomizeJob: MutableState<Job?>,
) {
    items.takeIf { it.isNotEmpty() }?.let { items ->
        item(key = "speed_dial_title") {
            NavigationTitle(
                title = stringResource(R.string.speed_dial),
                modifier = Modifier.animateItem()
            )
        }

        item(key = "speed_dial_list") {
            val targetItemSize = 160.dp
            val availableWidth = deps.availableWidth - 32.dp
            val (speedDialColumnsOverride) = rememberPreference(SpeedDialColumnsOverrideKey, 0)
            val columns = if (speedDialColumnsOverride > 0) {
                speedDialColumnsOverride
            } else {
                (availableWidth / targetItemSize).toInt().coerceAtLeast(3)
            }
            val rows = if (columns >= 6) 1 else if (columns >= 4) 2 else 3
            val itemsPerPage = columns * rows
            val itemWidth = availableWidth / columns
            // Decode/upload only what the tile actually needs — 544 (the
            // shared ItemThumbnail default, sized for larger art like the
            // hero card) is ~1.6x more pixels than a ~160dp grid tile ever
            // renders, which showed up as extra decode + hardware-bitmap
            // upload cost during Home scroll profiling.
            val speedDialThumbnailSizePx = with(LocalDensity.current) {
                itemWidth.roundToPx().coerceAtLeast(64)
            }
            val (speedDialHeightOverride) = rememberPreference(SpeedDialCardHeightOverrideKey, 0)
            val (speedDialCornerOverride) = rememberPreference(HomeCardCornerRadiusOverrideKey, 0)
            val speedDialTileHeight = if (speedDialHeightOverride > 0) {
                speedDialHeightOverride.dp
            } else {
                itemWidth / 0.8f // taller than wide, Apple Music card proportions
            }

            // Page slicing done once per (items, itemsPerPage) instead of re-drop/
            // re-taking inside the pager lambda for every composed page.
            val pages = remember(items, itemsPerPage) { items.chunked(itemsPerPage) }
            val pagerState = rememberPagerState(pageCount = { pages.size })

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .animateItem(),
            ) {
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 16.dp).plusStart(deps.sideInset),
                    pageSpacing = 16.dp,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(speedDialTileHeight * rows)
                            .bleedStart(deps.sideInset),
                ) { page ->
                    val pageItems = pages[page]

                    Column(modifier = Modifier.fillMaxSize()) {
                        for (row in 0 until rows) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                for (col in 0 until columns) {
                                    val itemIndex = row * columns + col

                                    val isRandomizeSlot = (page == 0 && itemIndex == itemsPerPage - 1)

                                    if (isRandomizeSlot) {
                                        Box(
                                            modifier = Modifier
                                                .width(itemWidth)
                                                .height(speedDialTileHeight)
                                                .padding(AppleTokens.ItemGap / 2)
                                        ) {
                                            RandomizeGridItem(
                                                isLoading = isRandomizing,
                                                onClick = {
                                                    if (isRandomizing) {
                                                        randomizeJob.value?.cancel()
                                                    } else {
                                                        randomizeJob.value = deps.scope.launch {
                                                            val randomItem = deps.viewModel.getRandomItem()
                                                            if (randomItem != null) {
                                                                when (randomItem) {
                                                                    is SongItem -> deps.playerConnection.playQueue(
                                                                        YouTubeQueue(
                                                                            randomItem.endpoint ?: WatchEndpoint(videoId = randomItem.id),
                                                                            randomItem.toMediaMetadata()
                                                                        )
                                                                    )
                                                                    is AlbumItem -> deps.navController.navigate("album/${randomItem.id}")
                                                                    is ArtistItem -> deps.navController.navigate("artist/${randomItem.id}")
                                                                    is PlaylistItem -> deps.navController.navigate("online_playlist/${randomItem.id}") //patched directly shows corresponding screens
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    } else if (itemIndex < pageItems.size) {
                                        val item = pageItems[itemIndex]

                                        Box(
                                            modifier = Modifier
                                                .width(itemWidth)
                                                .height(speedDialTileHeight)
                                                .padding(AppleTokens.ItemGap / 2)
                                        ) {
                                            SpeedDialGridItem(
                                                item = item,
                                                isPinned = false,
                                                isActive = remember(item.id, mediaMetadata?.album?.id, mediaMetadata?.id) {
                                                    item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id)
                                                },
                                                isPlaying = isPlaying,
                                                thumbnailSizePx = speedDialThumbnailSizePx,
                                                cornerRadiusDp = if (speedDialCornerOverride > 0) speedDialCornerOverride else 24,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .combinedBounceClick(
                                                        onClick = {
                                                            when (item) {
                                                                is SongItem -> deps.playerConnection.playQueue(
                                                                    YouTubeQueue(
                                                                        item.endpoint ?: WatchEndpoint(videoId = item.id),
                                                                        item.toMediaMetadata()
                                                                    )
                                                                )
                                                                is AlbumItem -> deps.navController.navigate("album/${item.id}")
                                                                is ArtistItem -> deps.navController.navigate("artist/${item.id}")

                                                                is PlaylistItem -> deps.navController.navigate("online_playlist/${item.id}") //patched navigation to correct screens
                                                            }
                                                        },
                                                        onLongClick = {
                                                            deps.haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            deps.menuState.show {
                                                                when (item) {
                                                                    is SongItem -> YouTubeSongMenu(
                                                                        song = item,
                                                                        navController = deps.navController,
                                                                        onDismiss = deps.menuState::dismiss
                                                                    )
                                                                    is AlbumItem -> YouTubeAlbumMenu(
                                                                        albumItem = item,
                                                                        navController = deps.navController,
                                                                        onDismiss = deps.menuState::dismiss
                                                                    )
                                                                    is ArtistItem -> YouTubeArtistMenu(
                                                                        artist = item,
                                                                        onDismiss = deps.menuState::dismiss
                                                                    )
                                                                    is PlaylistItem -> YouTubePlaylistMenu(
                                                                        playlist = item,
                                                                        coroutineScope = deps.scope,
                                                                        onDismiss = deps.menuState::dismiss
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    )
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.width(itemWidth))
                                    }
                                }
                            }
                        }
                    }
                }

                if (pagerState.pageCount > 1) {
                    Row(
                        modifier = Modifier
                            .height(24.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(pagerState.pageCount) { iteration ->
                            val color = if (pagerState.currentPage == iteration)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .size(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.quickPicksSection(
    deps: HomeSectionDeps,
    quickPicks: List<Song>?,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    songMap: Map<String, Song>,
) {
    quickPicks?.takeIf { it.isNotEmpty() }?.let { quickPicks ->
        item(key = "quick_picks_title") {
            val quickPicksTitle = stringResource(R.string.quick_picks)
            NavigationTitle(
                title = quickPicksTitle,
                modifier = Modifier.animateItem(),
                onPlayAllClick = {
                    deps.playerConnection.playQueue(
                        ListQueue(
                            title = quickPicksTitle,
                            items = quickPicks.distinctBy { it.id }.map { it.toMediaItem() }
                        )
                    )
                }
            )
        }

        item(key = "quick_picks_list") {
            val distinctQuickPicks =
                remember(quickPicks) { quickPicks.distinctBy { it.id } }
            LazyHorizontalGrid(
                state = deps.quickPicksGridState,
                rows = GridCells.Fixed(QuickPicksGridRows),
                flingBehavior = rememberSnapFlingBehavior(deps.quickPicksSnapLayoutInfoProvider),
                contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
                    .asPaddingValues().plusStart(deps.sideInset),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ListItemHeight * QuickPicksGridRows)
                    .animateItem().bleedStart(deps.sideInset)
            ) {
                itemsIndexed(
                    items = distinctQuickPicks,
                    key = { _, it -> it.id }
                ) { index, originalSong ->
                    val song = songMap[originalSong.id] ?: originalSong

                    SongListItem(
                        song = song,
                        showInLibraryIcon = true,
                        isActive = song.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        isSwipeable = false,
                        flat = true,
                        shape = listItemShape(index = index % QuickPicksGridRows, count = QuickPicksGridRows),
                        trailingContent = {
                            IconButton(
                                onClick = {
                                    deps.menuState.show {
                                        SongMenu(
                                            originalSong = song,
                                            navController = deps.navController,
                                            onDismiss = deps.menuState::dismiss
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
                            .width(deps.horizontalLazyGridItemWidth)
                            .combinedBounceClick(
                                onClick = {
                                    if (song.id == mediaMetadata?.id) {
                                        deps.playerConnection.togglePlayPause()
                                    } else {
                                        deps.playerConnection.playQueue(
                                            YouTubeQueue.radio(
                                                song.toMediaMetadata()
                                            )
                                        )
                                    }
                                },
                                onLongClick = {
                                    deps.haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    deps.menuState.show {
                                        SongMenu(
                                            originalSong = song,
                                            navController = deps.navController,
                                            onDismiss = deps.menuState::dismiss
                                        )
                                    }
                                }
                            )
                    )
                }
            }
        }
    }
}

private fun LazyListScope.communityPlaylistsSection(
    deps: HomeSectionDeps,
    communityPlaylists: List<CommunityPlaylistItem>?,
) {
    communityPlaylists?.takeIf { it.isNotEmpty() }?.let { playlists ->
        item(key = "community_playlists_title") {
            NavigationTitle(
                title = stringResource(R.string.from_the_community),
                modifier = Modifier.animateItem()
            )
        }

        item(key = "community_playlists_content") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp).plusStart(deps.sideInset),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.animateItem().bleedStart(deps.sideInset)
            ) {
                items(items = playlists, key = { it.playlist.id }) { item ->
                    CommunityPlaylistCard(
                        item = item,
                        onClick = {
                            deps.navController.navigate("online_playlist/${item.playlist.id.removePrefix("VL")}")
                        },
                        onSongClick = { song ->
                            deps.playerConnection.playQueue(
                                YouTubeQueue(
                                    song.endpoint ?: WatchEndpoint(videoId = song.id),
                                    song.toMediaMetadata()
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun LazyListScope.dailyDiscoverSection(
    deps: HomeSectionDeps,
    dailyDiscover: List<DailyDiscoverItem>?,
) {
    dailyDiscover?.takeIf { it.isNotEmpty() }?.let { discoverList ->
        //added a tittle new update
        item(key = "daily_discover_title") {
            val title = stringResource(R.string.your_daily_discover)
            NavigationTitle(
                title = title,
                onPlayAllClick = {
                    val queueItems = discoverList.mapNotNull {
                        (it.recommendation as? SongItem)?.toMediaMetadata()
                    }

                    if (queueItems.isNotEmpty()) {
                        deps.playerConnection.playQueue(
                            ListQueue(
                                title = title,
                                items = queueItems.map { it.toMediaItem() }
                            )
                        )
                    }
                }
            )
        }
        item(key = "daily_discover_content") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                val carouselState = rememberCarouselState { discoverList.size }
                HorizontalMultiBrowseCarousel(
                    state = carouselState,
                    preferredItemWidth = 320.dp,
                    itemSpacing = 16.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) { i ->
                    val item = discoverList[i]
                    DailyDiscoverCard(
                        dailyDiscover = item,
                        onClick = {
                            val song = item.recommendation as? SongItem
                            val mediaMetadata = song?.toMediaMetadata()
                            if (mediaMetadata != null) {
                                deps.playerConnection.playQueue(
                                    YouTubeQueue(
                                        song.endpoint ?: WatchEndpoint(videoId = song.id),
                                        mediaMetadata
                                    )
                                )
                            }
                        },
                        navController = deps.navController,
                        modifier = Modifier.maskClip(MaterialTheme.shapes.extraLarge)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.keepListeningSection(
    deps: HomeSectionDeps,
    keepListening: List<LocalItem>?,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
) {
    keepListening?.takeIf { it.isNotEmpty() }?.let { keepListening ->
        item(key = "keep_listening_title") {
            NavigationTitle(
                title = stringResource(R.string.keep_listening),
                modifier = Modifier.animateItem()
            )
        }

        item(key = "keep_listening_list") {
            val rows = if (keepListening.size > 6) 2 else 1
            LazyHorizontalGrid(
                // Hoisted to the Home composable: an inline rememberLazyGridState()
                // here dropped and rebuilt the scroll position whenever the grid left
                // the composition while the user scrolled the Keep Listening row.
                state = deps.keepListeningGridState,
                rows = GridCells.Fixed(rows),
                // GridItem pads itself by half the gap;
                // this supplies the other half so the
                // first tile lines up on the gutter.
                contentPadding = PaddingValues(horizontal = AppleTokens.ItemGap / 2)
                    .plusStart(deps.sideInset),
                modifier = Modifier
                    .fillMaxWidth()
                    .height((deps.currentGridHeight + with(LocalDensity.current) {
                        MaterialTheme.typography.bodyLarge.lineHeight.toDp() * 2 +
                                MaterialTheme.typography.bodyMedium.lineHeight.toDp() * 2
                    }) * rows)
                    .animateItem().bleedStart(deps.sideInset)
            ) {
                items(items = keepListening, key = { it.id }) {
                    deps.localGridItem(it)
                }
            }
        }
    }
}

private fun LazyListScope.accountPlaylistsSection(
    deps: HomeSectionDeps,
    accountPlaylists: List<PlaylistItem>?,
    accountName: String,
    url: String?,
) {
    accountPlaylists?.takeIf { it.isNotEmpty() }?.let { accountPlaylists ->
        item(key = "account_playlists_title") {
            NavigationTitle(
                label = stringResource(R.string.your_youtube_playlists),
                title = accountName,
                thumbnail = {
                    if (url != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(url)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .diskCacheKey(url)
                                .crossfade(false)
                                .build(),
                            placeholder = painterResource(id = R.drawable.person),
                            error = painterResource(id = R.drawable.person),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(ListThumbnailSize)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.person),
                            contentDescription = null,
                            modifier = Modifier.size(ListThumbnailSize)
                        )
                    }
                },
                onClick = {
                    deps.navController.navigate("account")
                },
                modifier = Modifier.animateItem()
            )
        }

        item(key = "account_playlists_list") {
            val distinctAccountPlaylists =
                remember(accountPlaylists) { accountPlaylists.distinctBy { it.id } }
            LazyRow(
                contentPadding = WindowInsets.systemBars
                    .only(WindowInsetsSides.Horizontal)
                    .asPaddingValues().plusStart(deps.sideInset),
                modifier = Modifier.animateItem().bleedStart(deps.sideInset)
            ) {
                items(
                    items = distinctAccountPlaylists,
                    key = { it.id },
                ) { item ->
                    deps.ytGridItem(item)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.forgottenFavoritesSection(
    deps: HomeSectionDeps,
    forgottenFavorites: List<Song>?,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    songMap: Map<String, Song>,
) {
    forgottenFavorites?.takeIf { it.isNotEmpty() }?.let { forgottenFavorites ->
        item(key = "forgotten_favorites_title") {
            val forgottenFavoritesTitle = stringResource(R.string.forgotten_favorites)
            NavigationTitle(
                title = forgottenFavoritesTitle,
                modifier = Modifier.animateItem(),
                onPlayAllClick = {
                    deps.playerConnection.playQueue(
                        ListQueue(
                            title = forgottenFavoritesTitle,
                            items = forgottenFavorites.distinctBy { it.id }.map { it.toMediaItem() }
                        )
                    )
                }
            )
        }

        item(key = "forgotten_favorites_list") {
            // take min in case list size is less than 4
            val rows = min(SongGridRows, forgottenFavorites.size)
            val distinctForgottenFavorites =
                remember(forgottenFavorites) { forgottenFavorites.distinctBy { it.id } }
            LazyHorizontalGrid(
                state = deps.forgottenFavoritesGridState,
                rows = GridCells.Fixed(rows),
                contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
                    .asPaddingValues().plusStart(deps.sideInset),
                flingBehavior = rememberSnapFlingBehavior(
                    deps.forgottenFavoritesSnapLayoutInfoProvider
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ListItemHeight * rows)
                    .animateItem().bleedStart(deps.sideInset)
            ) {
                itemsIndexed(
                    items = distinctForgottenFavorites,
                    key = { _, it -> it.id }
                ) { index, originalSong ->
                    val song = songMap[originalSong.id] ?: originalSong

                    SongListItem(
                        song = song,
                        showInLibraryIcon = true,
                        isActive = song.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        isSwipeable = false,
                        shape = listItemShape(index = index % rows, count = rows),
                        trailingContent = {
                            IconButton(
                                onClick = {
                                    deps.haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    deps.menuState.show {
                                        SongMenu(
                                            originalSong = song,
                                            navController = deps.navController,
                                            onDismiss = deps.menuState::dismiss
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
                            .width(deps.horizontalLazyGridItemWidth)
                            .combinedBounceClick(
                                onClick = {
                                    if (song.id == mediaMetadata?.id) {
                                        deps.playerConnection.togglePlayPause()
                                    } else {
                                        deps.playerConnection.playQueue(
                                            YouTubeQueue.radio(
                                                song.toMediaMetadata()
                                            )
                                        )
                                    }
                                },
                                onLongClick = {
                                    deps.haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    deps.menuState.show {
                                        SongMenu(
                                            originalSong = song,
                                            navController = deps.navController,
                                            onDismiss = deps.menuState::dismiss
                                        )
                                    }
                                }
                            )
                    )
                }
            }
        }
    }
}

private fun LazyListScope.similarRecommendationsSection(
    section: HomeSection.SimilarRecommendation,
    deps: HomeSectionDeps,
    similarRecommendations: List<SimilarRecommendation>?,
) {
    val recommendation = similarRecommendations?.getOrNull(section.index)
    recommendation?.let {
        item(key = "similar_to_title_${section.index}") {
            NavigationTitle(
                label = stringResource(R.string.similar_to),
                title = recommendation.title.title,
                thumbnail = recommendation.title.thumbnailUrl?.let { thumbnailUrl ->
                    {
                        val shape =
                            if (recommendation.title is Artist) CircleShape else RoundedCornerShape(
                                ThumbnailCornerRadius
                            )
                        AsyncImage(
                            model = thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(ListThumbnailSize)
                                .clip(shape)
                        )
                    }
                },
                onClick = {
                    when (recommendation.title) {
                        is Song -> deps.navController.navigate("album/${recommendation.title.album!!.id}")
                        is Album -> deps.navController.navigate("album/${recommendation.title.id}")
                        is Artist -> deps.navController.navigate("artist/${recommendation.title.id}")
                        is Playlist -> {}
                    }
                },
                modifier = Modifier.animateItem()
            )
        }

        item(key = "similar_to_list_${section.index}") {
            LazyRow(
                contentPadding = WindowInsets.systemBars
                    .only(WindowInsetsSides.Horizontal)
                    .asPaddingValues().plusStart(deps.sideInset),
                modifier = Modifier.animateItem().bleedStart(deps.sideInset)
            ) {
                items(items = recommendation.items, key = { it.id }) { item ->
                    deps.ytGridItem(item)
                }
            }
        }
    }
}

private fun LazyListScope.homePageSection(
    section: HomeSection.HomePageSection,
    deps: HomeSectionDeps,
    homePage: HomePage?,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
) {
    val sectionData = homePage?.sections?.getOrNull(section.index)
    sectionData?.let {
        // Check if section contains songs for Play All functionality
        val sectionSongs = sectionData.items.filterIsInstance<SongItem>()
        val hasPlayableSongs = sectionSongs.isNotEmpty()
        // Check if this section contains ONLY songs (like Quick picks, Trending songs)
        val isSongsOnlySection = sectionData.items.isNotEmpty() &&
                sectionData.items.all { it is SongItem }

        item(key = "home_section_title_${section.index}") {
            NavigationTitle(
                title = sectionData.title,
                label = sectionData.label,
                thumbnail = sectionData.thumbnail?.let { thumbnailUrl ->
                    {
                        val shape =
                            if (sectionData.endpoint?.isArtistEndpoint == true) CircleShape else RoundedCornerShape(
                                ThumbnailCornerRadius
                            )
                        AsyncImage(
                            model = thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(ListThumbnailSize)
                                .clip(shape)
                        )
                    }
                },
                onClick = sectionData.endpoint?.let { endpoint ->
                    {
                        when {
                            endpoint.browseId == "FEmusic_moods_and_genres" ->
                                deps.navController.navigate("mood_and_genres")
                            endpoint.params != null ->
                                deps.navController.navigate("youtube_browse/${endpoint.browseId}?params=${endpoint.params}")
                            else ->
                                deps.navController.navigate("browse/${endpoint.browseId}")
                        }
                    }
                },
                onPlayAllClick = if (hasPlayableSongs) {
                    {
                        deps.playerConnection.playQueue(
                            ListQueue(
                                title = sectionData.title,
                                items = sectionSongs.map { it.toMediaMetadata().toMediaItem() }
                            )
                        )
                    }
                } else null,
                modifier = Modifier.animateItem()
            )
        }

        if (isSongsOnlySection) {
            // Render songs as a horizontal scrollable list (like Quick picks in YouTube Music)
            item(key = "home_section_list_${section.index}") {
                val distinctSectionSongs =
                    remember(sectionSongs) { sectionSongs.distinctBy { it.id } }
                LazyHorizontalGrid(
                    state = rememberLazyGridState(),
                    rows = GridCells.Fixed(SongGridRows),
                    contentPadding = WindowInsets.systemBars
                        .only(WindowInsetsSides.Horizontal)
                        .asPaddingValues().plusStart(deps.sideInset),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ListItemHeight * SongGridRows)
                        .animateItem().bleedStart(deps.sideInset)
                ) {
                    itemsIndexed(
                        items = distinctSectionSongs,
                        key = { _, it -> it.id }
                    ) { index, song ->
                        YouTubeListItem(
                            item = song,
                            isActive = song.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            isSwipeable = false,
                            shape = listItemShape(index = index % SongGridRows, count = SongGridRows),
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        deps.menuState.show {
                                            YouTubeSongMenu(
                                                song = song,
                                                navController = deps.navController,
                                                onDismiss = deps.menuState::dismiss
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
                                .width(deps.horizontalLazyGridItemWidth)
                                .combinedBounceClick(
                                    onClick = {
                                        if (song.id == mediaMetadata?.id) {
                                            deps.playerConnection.togglePlayPause()
                                        } else {
                                            deps.playerConnection.playQueue(
                                                YouTubeQueue.radio(song.toMediaMetadata())
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        deps.haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        deps.menuState.show {
                                            YouTubeSongMenu(
                                                song = song,
                                                navController = deps.navController,
                                                onDismiss = deps.menuState::dismiss
                                            )
                                        }
                                    }
                                )
                        )
                    }
                }
            }
        } else {
            // Render mixed content as horizontal grid items (albums, playlists, artists, etc.)
            item(key = "home_section_list_${section.index}") {
                LazyRow(
                    contentPadding = WindowInsets.systemBars
                        .only(WindowInsetsSides.Horizontal)
                        .asPaddingValues().plusStart(deps.sideInset),
                    modifier = Modifier.animateItem().bleedStart(deps.sideInset)
                ) {
                    items(items = sectionData.items, key = { it.id }) { item ->
                        deps.ytGridItem(item)
                    }
                }
            }
        }
    }
}

private fun LazyListScope.moodAndGenresSection(
    deps: HomeSectionDeps,
    explorePage: ExplorePage?,
) {
    // Deduplicated by title: YouTube can return the same
    // genre twice (seen with "Desi Hip-Hop"), and the grid
    // below keys on title — a repeat crashed Home outright
    // with IllegalArgumentException from the lazy list.
    // Rendering it twice would be wrong regardless, so this
    // fixes the display and the crash in one place.
    explorePage?.moodAndGenres?.distinctBy { it.title }?.let { moodAndGenres ->
        item(key = "mood_and_genres_title") {
            NavigationTitle(
                title = stringResource(R.string.mood_and_genres),
                onClick = {
                    deps.navController.navigate("mood_and_genres")
                },
                modifier = Modifier.animateItem()
            )
        }
        item(key = "mood_and_genres_list") {
            LazyHorizontalGrid(
                rows = GridCells.Fixed(4),
                contentPadding = PaddingValues(AppleTokens.ItemGap / 2).plusStart(deps.sideInset),
                modifier = Modifier
                    .height((MoodAndGenresButtonHeight + AppleTokens.ItemGap) * 4 + AppleTokens.ItemGap)
                    .animateItem().bleedStart(deps.sideInset)
            ) {
                items(items = moodAndGenres, key = { it.title }) {
                    MoodAndGenresButton(
                        title = it.title,
                        onClick = {
                            deps.navController.navigate("youtube_browse/${it.endpoint.browseId}?params=${it.endpoint.params}")
                        },
                        modifier = Modifier
                            .padding(AppleTokens.ItemGap / 2)
                            .width(180.dp)
                    )
                }
            }
        }
    }
}

/**
 * Fetches one batched songsByIds flow for a whole list's ids and indexes it into a
 * Map<String, Song>. Re-emits whenever any of those rows change, so rows stay as live
 * as the old per-row database.song() subscriptions — with a single observer per list.
 */
@Composable
private fun rememberSongsById(
    database: MusicDatabase,
    songIds: List<String>,
): Map<String, Song> {
    val flow = remember(songIds) {
        if (songIds.isEmpty()) flowOf(emptyList()) else database.songsByIds(songIds)
    }
    val songs by flow.collectAsStateWithLifecycle(initialValue = emptyList())
    return remember(songs) { songs.associateBy { it.id } }
}
