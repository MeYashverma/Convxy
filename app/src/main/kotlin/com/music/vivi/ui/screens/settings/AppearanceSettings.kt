/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.edit
import com.music.vivi.ui.utils.bounceClick
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.CanvasSource
import com.music.vivi.constants.CanvasSourceKey
import com.music.vivi.constants.CanvasThumbnailAnimationKey
import com.music.vivi.constants.ChipSortTypeKey
import com.music.vivi.constants.CropAlbumArtKey
import com.music.vivi.constants.DefaultOpenTabKey
import com.music.vivi.constants.LiquidGlassBlurRadiusKey
import com.music.vivi.constants.LiquidGlassGlobalEnabledKey
import com.music.vivi.constants.LiquidGlassLensAmountKey
import com.music.vivi.constants.LiquidGlassMiniPlayerEnabledKey
import com.music.vivi.constants.LiquidGlassNavBarEnabledKey
import com.music.vivi.constants.LiquidGlassSurfaceOpacityKey
import com.music.vivi.constants.LiquidGlassVibrancyKey
import com.music.vivi.constants.UseFloatingNavBarKey
import com.music.vivi.constants.DensityScale
import com.music.vivi.constants.DensityScaleKey
import com.music.vivi.constants.DynamicThemeKey
import com.music.vivi.constants.EnableDynamicIconKey
import com.music.vivi.constants.EnableSettingsPopupKey
import com.music.vivi.constants.EnableHighRefreshRateKey
import com.music.vivi.constants.EnableLyricsThumbnailPlayPauseKey
import com.music.vivi.constants.GridItemSize
import com.music.vivi.constants.GridItemsSizeKey
import com.music.vivi.constants.HidePlayerThumbnailKey
import com.music.vivi.constants.LibraryFilter
import com.music.vivi.constants.ListenTogetherInTopBarKey
import com.music.vivi.constants.LyricsAnimationStyle
import com.music.vivi.constants.LyricsAnimationStyleKey
import com.music.vivi.constants.LyricsStandardBlurKey
import com.music.vivi.constants.LyricsTextPositionKey
import com.music.vivi.constants.LyricsTextSizeKey
import com.music.vivi.constants.PlayerBackgroundStyle
import com.music.vivi.constants.PlayerBackgroundStyleKey
import com.music.vivi.constants.PlayerButtonsStyle
import com.music.vivi.constants.PlayerButtonsStyleKey
import com.music.vivi.constants.RotatingThumbnailKey
import com.music.vivi.constants.ShowCachedPlaylistKey
import com.music.vivi.constants.ShowDownloadedPlaylistKey
import com.music.vivi.constants.ShowLikedPlaylistKey
import com.music.vivi.constants.ShowTopPlaylistKey
import com.music.vivi.constants.ShowUploadedPlaylistKey
import com.music.vivi.constants.SliderStyle
import com.music.vivi.constants.SliderStyleKey
import com.music.vivi.constants.SlimNavBarKey
import com.music.vivi.constants.SquigglySliderKey
import com.music.vivi.constants.SwipeSensitivityKey
import com.music.vivi.constants.SwipeThumbnailKey
import com.music.vivi.constants.SwipeLyricsKey
import com.music.vivi.constants.SwipeToRemoveSongKey
import com.music.vivi.constants.SwipeToSongKey
import com.music.vivi.constants.ThumbnailCornerRadiusKey
import com.music.vivi.constants.UseNewMiniPlayerDesignKey
import com.music.vivi.constants.UseNewPlayerDesignKey
import com.music.vivi.ui.component.ThumbnailCornerRadiusModal
import com.music.vivi.ui.component.DefaultDialog
import com.music.vivi.ui.component.EnumDialog
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.Material3SettingsGroup
import com.music.vivi.ui.component.Material3SettingsItem
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import kotlin.math.roundToInt
import com.music.vivi.constants.LyricsClickKey
import com.music.vivi.constants.AppleMusicLyricsBlurKey
import com.music.vivi.constants.LyricsGlowEffectKey
import com.music.vivi.constants.LyricsLineSpacingKey
import com.music.vivi.constants.LyricsScrollKey
import com.music.vivi.constants.ShowAudioQualityBadgeKey
import com.music.vivi.constants.ShowCommentButtonKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    activity: Activity,
    snackbarHostState: SnackbarHostState,
) {
    // Liquid Glass settings - promoted to top
    val (liquidGlassVibrancy, onLiquidGlassVibrancyChange) = rememberPreference(
        LiquidGlassVibrancyKey,
        defaultValue = 1.2f
    )
    val (liquidGlassBlurRadius, onLiquidGlassBlurRadiusChange) = rememberPreference(
        LiquidGlassBlurRadiusKey,
        defaultValue = 2f
    )
    val (liquidGlassLensAmount, onLiquidGlassLensAmountChange) = rememberPreference(
        LiquidGlassLensAmountKey,
        defaultValue = 0.6f
    )
    val (liquidGlassSurfaceOpacity, onLiquidGlassSurfaceOpacityChange) = rememberPreference(
        LiquidGlassSurfaceOpacityKey,
        defaultValue = 0.65f
    )

    // Hidden defaults enforced as Always-ON
    val (_, _) = rememberPreference(LiquidGlassGlobalEnabledKey, defaultValue = true)
    val (_, _) = rememberPreference(LiquidGlassNavBarEnabledKey, defaultValue = true)
    val (_, _) = rememberPreference(LiquidGlassMiniPlayerEnabledKey, defaultValue = true)
    val (_, _) = rememberPreference(UseFloatingNavBarKey, defaultValue = true)
    val (_, _) = rememberPreference(UseNewMiniPlayerDesignKey, defaultValue = true)
    val (_, _) = rememberPreference(DynamicThemeKey, defaultValue = true)
    val (_, _) = rememberPreference(EnableDynamicIconKey, defaultValue = true)
    val (_, _) = rememberPreference(UseNewPlayerDesignKey, defaultValue = true)

    val (enableHighRefreshRate, onEnableHighRefreshRateChange) = rememberPreference(
        EnableHighRefreshRateKey,
        defaultValue = true
    )
    val (enableSettingsPopup, onEnableSettingsPopupChange) = rememberPreference(
        EnableSettingsPopupKey,
        defaultValue = false
    )

    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) = rememberPreference(
        HidePlayerThumbnailKey,
        defaultValue = false
    )
    val (cropAlbumArt, onCropAlbumArtChange) = rememberPreference(
        CropAlbumArtKey,
        defaultValue = false
    )
    val (playerBackground, onPlayerBackgroundChange) =
        rememberEnumPreference(
            PlayerBackgroundStyleKey,
            defaultValue = PlayerBackgroundStyle.GRADIENT,
        )

    val (defaultOpenTab, onDefaultOpenTabChange) = rememberEnumPreference(
        DefaultOpenTabKey,
        defaultValue = NavigationTab.HOME
    )
    val (playerButtonsStyle, onPlayerButtonsStyleChange) = rememberEnumPreference(
        PlayerButtonsStyleKey,
        defaultValue = PlayerButtonsStyle.DEFAULT
    )
    val (lyricsPosition, onLyricsPositionChange) = rememberEnumPreference(
        LyricsTextPositionKey,
        defaultValue = LyricsPosition.LEFT
    )
    val (lyricsClick, onLyricsClickChange) = rememberPreference(LyricsClickKey, defaultValue = true)
    val (lyricsScroll, onLyricsScrollChange) = rememberPreference(
        LyricsScrollKey,
        defaultValue = true
    )
    val (lyricsAnimationStyle, onLyricsAnimationStyleChange) = rememberEnumPreference(
        LyricsAnimationStyleKey,
        defaultValue = LyricsAnimationStyle.VIVIMUSIC_1
    )
    val (lyricsTextSize, onLyricsTextSizeChange) = rememberPreference(LyricsTextSizeKey, defaultValue = 24f)
    val (lyricsLineSpacing, onLyricsLineSpacingChange) = rememberPreference(LyricsLineSpacingKey, defaultValue = 1.3f)
    val (lyricsGlowEffect, onLyricsGlowEffectChange) = rememberPreference(LyricsGlowEffectKey, defaultValue = false)
    val (appleMusicLyricsBlur, onAppleMusicLyricsBlurChange) = rememberPreference(AppleMusicLyricsBlurKey, defaultValue = true)
    val (lyricsStandardBlur, onLyricsStandardBlurChange) = rememberPreference(LyricsStandardBlurKey, defaultValue = false)
    val (swipeLyrics, onSwipeLyricsChange) = rememberPreference(SwipeLyricsKey, defaultValue = false)
    val (enableLyricsThumbnailPlayPause, onEnableLyricsThumbnailPlayPauseChange) = rememberPreference(EnableLyricsThumbnailPlayPauseKey, defaultValue = false)

    val (sliderStyle, onSliderStyleChange) = rememberEnumPreference(
        SliderStyleKey,
        defaultValue = SliderStyle.DEFAULT
    )
    val (squigglySlider, onSquigglySliderChange) = rememberPreference(
        SquigglySliderKey,
        defaultValue = false
    )
    val (swipeThumbnail, onSwipeThumbnailChange) = rememberPreference(
        SwipeThumbnailKey,
        defaultValue = true
    )
    val (swipeSensitivity, onSwipeSensitivityChange) = rememberPreference(
        SwipeSensitivityKey,
        defaultValue = 0.73f
    )
    val (canvasThumbnailAnimation, onCanvasThumbnailAnimationChange) = rememberPreference(
        CanvasThumbnailAnimationKey,
        defaultValue = true
    )
    val (canvasSource) = rememberEnumPreference(
        CanvasSourceKey,
        defaultValue = CanvasSource.AUTO
    )
    val (rotatingThumbnail, onRotatingThumbnailChange) = rememberPreference(
        RotatingThumbnailKey,
        defaultValue = false
    )
    val (gridItemSize, onGridItemSizeChange) = rememberEnumPreference(
        GridItemsSizeKey,
        defaultValue = GridItemSize.SMALL
    )

    // Density scale preferences
    val context = activity as Context
    val sharedPreferences = remember { context.getSharedPreferences("vivimusic_settings", Context.MODE_PRIVATE) }
    val prefDensityScale = remember(sharedPreferences) {
        sharedPreferences.getFloat("density_scale_factor", 1.0f)
    }
    val (densityScale, setDensityScale) = rememberPreference(DensityScaleKey, defaultValue = prefDensityScale)
    var showRestartDialog by rememberSaveable { mutableStateOf(false) }
    var showDensityScaleDialog by rememberSaveable { mutableStateOf(false) }

    val onDensityScaleChange: (Float) -> Unit = { newScale ->
        setDensityScale(newScale)
        sharedPreferences.edit {
            putFloat("density_scale_factor", newScale)
        }
        showRestartDialog = true
    }

    val (listenTogetherInTopBar, onListenTogetherInTopBarChange) = rememberPreference(
        ListenTogetherInTopBarKey,
        defaultValue = true
    )

    val (swipeToSong, onSwipeToSongChange) = rememberPreference(
        SwipeToSongKey,
        defaultValue = false
    )

    val (swipeToRemoveSong, onSwipeToRemoveSongChange) = rememberPreference(
        SwipeToRemoveSongKey,
        defaultValue = false
    )

    val (showLikedPlaylist, onShowLikedPlaylistChange) = rememberPreference(
        ShowLikedPlaylistKey,
        defaultValue = true
    )
    val (showDownloadedPlaylist, onShowDownloadedPlaylistChange) = rememberPreference(
        ShowDownloadedPlaylistKey,
        defaultValue = true
    )
    val (showTopPlaylist, onShowTopPlaylistChange) = rememberPreference(
        ShowTopPlaylistKey,
        defaultValue = true
    )
    val (showCachedPlaylist, onShowCachedPlaylistChange) = rememberPreference(
        ShowCachedPlaylistKey,
        defaultValue = true
    )
    val (showUploadedPlaylist, onShowUploadedPlaylistChange) = rememberPreference(
        ShowUploadedPlaylistKey,
        defaultValue = true
    )
    val (showCommentButton, onShowCommentButtonChange) = rememberPreference(
        ShowCommentButtonKey,
        defaultValue = true
    )

    val availableBackgroundStyles = PlayerBackgroundStyle.entries.filter {
        it != PlayerBackgroundStyle.BLUR || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }

    val (defaultChip, onDefaultChipChange) = rememberEnumPreference(
        key = ChipSortTypeKey,
        defaultValue = LibraryFilter.LIBRARY
    )

    var showPlayerBackgroundDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showPlayerButtonsStyleDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showLyricsPositionDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showLyricsAnimationStyleDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showLyricsTextSizeDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showLyricsLineSpacingDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showLyricsPositionDialog) {
        EnumDialog(
            onDismiss = { showLyricsPositionDialog = false },
            onSelect = {
                onLyricsPositionChange(it)
                showLyricsPositionDialog = false
            },
            title = stringResource(R.string.lyrics_text_position),
            current = lyricsPosition,
            values = LyricsPosition.entries,
            valueText = {
                when (it) {
                    LyricsPosition.LEFT -> stringResource(R.string.left)
                    LyricsPosition.CENTER -> stringResource(R.string.center)
                    LyricsPosition.RIGHT -> stringResource(R.string.right)
                }
            }
        )
    }

    if (showLyricsAnimationStyleDialog) {
        EnumDialog(
            onDismiss = { showLyricsAnimationStyleDialog = false },
            onSelect = {
                onLyricsAnimationStyleChange(it)
                showLyricsAnimationStyleDialog = false
            },
            title = stringResource(R.string.lyrics_animation_style),
            current = lyricsAnimationStyle,
            values = LyricsAnimationStyle.entries,
            valueText = {
                when (it) {
                    LyricsAnimationStyle.NONE -> stringResource(R.string.none)
                    LyricsAnimationStyle.FADE -> stringResource(R.string.fade)
                    LyricsAnimationStyle.GLOW -> stringResource(R.string.glow)
                    LyricsAnimationStyle.SLIDE -> stringResource(R.string.slide)
                    LyricsAnimationStyle.KARAOKE -> stringResource(R.string.karaoke)
                    LyricsAnimationStyle.APPLE -> stringResource(R.string.apple_music_style)
                    LyricsAnimationStyle.APPLE_V2 -> stringResource(R.string.apple_music_style_letter)
                    LyricsAnimationStyle.VIVIMUSIC_1 -> stringResource(R.string.vivimusic_1)
                    LyricsAnimationStyle.LYRICS_V2 -> stringResource(R.string.lyrics_v2_fluid)
                    LyricsAnimationStyle.METRO_LYRICS -> stringResource(R.string.lyrics_animation_metro)
                }
            }
        )
    }

    var showDefaultOpenTabDialog by rememberSaveable { mutableStateOf(false) }
    if (showDefaultOpenTabDialog) {
        EnumDialog(
            onDismiss = { showDefaultOpenTabDialog = false },
            onSelect = {
                onDefaultOpenTabChange(it)
                showDefaultOpenTabDialog = false
            },
            title = stringResource(R.string.default_open_tab),
            current = defaultOpenTab,
            values = NavigationTab.entries,
            valueText = {
                when (it) {
                    NavigationTab.HOME -> stringResource(R.string.home)
                    NavigationTab.SEARCH -> stringResource(R.string.search)
                    NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
                }
            }
        )
    }

    var showDefaultChipDialog by rememberSaveable { mutableStateOf(false) }
    if (showDefaultChipDialog) {
        EnumDialog(
            onDismiss = { showDefaultChipDialog = false },
            onSelect = {
                onDefaultChipChange(it)
                showDefaultChipDialog = false
            },
            title = stringResource(R.string.default_lib_chips),
            current = defaultChip,
            values = LibraryFilter.entries,
            valueText = {
                when (it) {
                    LibraryFilter.SONGS -> stringResource(R.string.songs)
                    LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                    LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                    LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                    LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                }
            }
        )
    }

    var showGridSizeDialog by rememberSaveable { mutableStateOf(false) }
    if (showGridSizeDialog) {
        EnumDialog(
            onDismiss = { showGridSizeDialog = false },
            onSelect = {
                onGridItemSizeChange(it)
                showGridSizeDialog = false
            },
            title = stringResource(R.string.grid_cell_size),
            current = gridItemSize,
            values = GridItemSize.entries,
            valueText = {
                when (it) {
                    GridItemSize.BIG -> stringResource(R.string.big)
                    GridItemSize.SMALL -> stringResource(R.string.small)
                }
            }
        )
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Material3SettingsGroup(
            title = stringResource(R.string.liquid_glass),
            items = buildList {
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.tune),
                        title = { Text(stringResource(R.string.liquid_glass_vibrancy)) },
                        description = { Text(String.format("%.1f", liquidGlassVibrancy)) },
                        trailingContent = {
                            Slider(
                                value = liquidGlassVibrancy,
                                onValueChange = onLiquidGlassVibrancyChange,
                                valueRange = 0f..2f,
                                modifier = Modifier.fillMaxWidth(0.6f)
                            )
                        },
                        onClick = {}
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.sliders),
                        title = { Text(stringResource(R.string.liquid_glass_blur_radius)) },
                        description = { Text("${liquidGlassBlurRadius.roundToInt()}dp") },
                        trailingContent = {
                            Slider(
                                value = liquidGlassBlurRadius,
                                onValueChange = onLiquidGlassBlurRadiusChange,
                                valueRange = 0f..100f,
                                modifier = Modifier.fillMaxWidth(0.6f)
                            )
                        },
                        onClick = {}
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.tune),
                        title = { Text(stringResource(R.string.liquid_glass_lens_amount)) },
                        description = { Text("${(liquidGlassLensAmount * 100).roundToInt()}%") },
                        trailingContent = {
                            Slider(
                                value = liquidGlassLensAmount,
                                onValueChange = onLiquidGlassLensAmountChange,
                                valueRange = 0f..1f,
                                modifier = Modifier.fillMaxWidth(0.6f)
                            )
                        },
                        onClick = {}
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.palette),
                        title = { Text(stringResource(R.string.liquid_glass_surface_opacity)) },
                        description = { Text("${(liquidGlassSurfaceOpacity * 100).roundToInt()}%") },
                        trailingContent = {
                            Slider(
                                value = liquidGlassSurfaceOpacity,
                                onValueChange = onLiquidGlassSurfaceOpacityChange,
                                valueRange = 0f..1f,
                                modifier = Modifier.fillMaxWidth(0.6f)
                            )
                        },
                        onClick = {}
                    )
                )
            }
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.appearance),
            items = buildList {
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.speed),
                        title = { Text(stringResource(R.string.enable_high_refresh_rate)) },
                        description = { Text(stringResource(R.string.enable_high_refresh_rate_desc)) },
                        trailingContent = {
                            Switch(
                                checked = enableHighRefreshRate,
                                onCheckedChange = onEnableHighRefreshRateChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            id = if (enableHighRefreshRate) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onEnableHighRefreshRateChange(!enableHighRefreshRate) }
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.settings),
                        title = { Text(stringResource(R.string.enable_settings_popup)) },
                        description = { Text(stringResource(R.string.enable_settings_popup_desc)) },
                        trailingContent = {
                            Switch(
                                checked = enableSettingsPopup,
                                onCheckedChange = onEnableSettingsPopupChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            id = if (enableSettingsPopup) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onEnableSettingsPopupChange(!enableSettingsPopup) }
                    )
                )
            }
        )

        Spacer(modifier = Modifier.height(27.dp))

        val (thumbnailCornerRadius, onThumbnailCornerRadiusChange) = rememberPreference(
            ThumbnailCornerRadiusKey,
            defaultValue = 3f
        )
        
        var showThumbnailCornerRadiusDialog by rememberSaveable { mutableStateOf(false) }

        Material3SettingsGroup(
            title = stringResource(R.string.player),
            items = listOfNotNull(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.gradient),
                    title = { Text(stringResource(R.string.player_background_style)) },
                    description = {
                        Text(
                            when (playerBackground) {
                                PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                                PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                                PlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                                PlayerBackgroundStyle.GLOW_ANIMATED -> stringResource(R.string.glow_animated)
                                PlayerBackgroundStyle.APPLE_MUSIC -> stringResource(R.string.apple_music)
                                PlayerBackgroundStyle.LIVE_MESH -> stringResource(R.string.live_mesh)
                    PlayerBackgroundStyle.LIQUID_GLASS -> stringResource(R.string.player_background_liquid_glass)
                }
                        )
                    },
                    onClick = { showPlayerBackgroundDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.hide_image),
                    title = { Text(stringResource(R.string.hide_player_thumbnail)) },
                    description = { Text(stringResource(R.string.hide_player_thumbnail_desc)) },
                    trailingContent = {
                        Switch(
                            checked = hidePlayerThumbnail,
                            onCheckedChange = onHidePlayerThumbnailChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (hidePlayerThumbnail) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onHidePlayerThumbnailChange(!hidePlayerThumbnail) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.image),
                    title = { Text(stringResource(R.string.thumbnail_corner_radius)) },
                    description = { Text(stringResource(R.string.thumbnail_corner_radius_desc)) },
                    trailingContent = {
                        Text(
                            text = "${thumbnailCornerRadius.roundToInt()}dp",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = { showThumbnailCornerRadiusDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.crop),
                    title = { Text(stringResource(R.string.crop_album_art)) },
                    description = { Text(stringResource(R.string.crop_album_art_desc)) },
                    trailingContent = {
                        Switch(
                            checked = cropAlbumArt,
                            onCheckedChange = onCropAlbumArtChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (cropAlbumArt) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onCropAlbumArtChange(!cropAlbumArt) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.palette),
                    title = { Text(stringResource(R.string.player_buttons_style)) },
                    description = {
                        Text(
                            when (playerButtonsStyle) {
                                PlayerButtonsStyle.DEFAULT -> stringResource(R.string.default_style)
                                PlayerButtonsStyle.PRIMARY -> stringResource(R.string.primary_color_style)
                                PlayerButtonsStyle.TERTIARY -> stringResource(R.string.tertiary_color_style)
                            }
                        )
                    },
                    onClick = { showPlayerButtonsStyleDialog = true }
                ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.canvas_art),
                        title = { Text(stringResource(R.string.vivimusic_canvas)) },
                    description = {
                        val summary = if (!canvasThumbnailAnimation) {
                            stringResource(R.string.disable)
                        } else {
                            when (canvasSource) {
                                CanvasSource.AUTO -> stringResource(R.string.canvas_source_auto)
                                CanvasSource.APPLE_MUSIC -> stringResource(R.string.canvas_source_apple_music)
                                CanvasSource.VIVIMUSIC -> stringResource(R.string.canvas_source_vivimusic)
                                CanvasSource.TIDAL -> stringResource(R.string.canvas_source_tidal)
                            }
                        }
                        Text(summary)
                    },
                    onClick = { navController.navigate("settings/appearance/canvas") }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.image),
                    title = { Text(stringResource(R.string.rotating_thumbnail)) },
                    description = { Text(stringResource(R.string.rotating_thumbnail_desc)) },
                    trailingContent = {
                        Switch(
                            checked = rotatingThumbnail,
                            onCheckedChange = onRotatingThumbnailChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (rotatingThumbnail) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onRotatingThumbnailChange(!rotatingThumbnail) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.chat_msg),
                    title = { Text(stringResource(R.string.show_comment_button)) },
                    description = { Text(stringResource(R.string.show_comment_button_description)) },
                    trailingContent = {
                        Switch(
                            checked = showCommentButton,
                            onCheckedChange = onShowCommentButtonChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (showCommentButton) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onShowCommentButtonChange(!showCommentButton) }
                )
            )
        )

        if (showThumbnailCornerRadiusDialog) {
            ThumbnailCornerRadiusModal(
                initialRadius = thumbnailCornerRadius,
                onDismiss = { showThumbnailCornerRadiusDialog = false },
                onRadiusSelected = { radius ->
                    onThumbnailCornerRadiusChange(radius)
                    showThumbnailCornerRadiusDialog = false
                }
            )
        }

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.lyrics),
            items = listOfNotNull(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.lyrics_text_position)) },
                    description = {
                        Text(
                            when (lyricsPosition) {
                                LyricsPosition.LEFT -> stringResource(R.string.left)
                                LyricsPosition.CENTER -> stringResource(R.string.center)
                                LyricsPosition.RIGHT -> stringResource(R.string.right)
                            }
                        )
                    },
                    onClick = { showLyricsPositionDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.lyrics_animation_style)) },
                    description = {
                        Text(
                            when (lyricsAnimationStyle) {
                                LyricsAnimationStyle.NONE -> stringResource(R.string.none)
                                LyricsAnimationStyle.FADE -> stringResource(R.string.fade)
                                LyricsAnimationStyle.GLOW -> stringResource(R.string.glow)
                                LyricsAnimationStyle.SLIDE -> stringResource(R.string.slide)
                                LyricsAnimationStyle.KARAOKE -> stringResource(R.string.karaoke)
                                LyricsAnimationStyle.VIVIMUSIC_1 -> stringResource(R.string.vivimusic_1)
                                LyricsAnimationStyle.APPLE -> stringResource(R.string.apple_music_style)
                                LyricsAnimationStyle.APPLE_V2 -> stringResource(R.string.apple_music_style_letter)
                                LyricsAnimationStyle.LYRICS_V2 -> stringResource(R.string.lyrics_v2_fluid)
                                LyricsAnimationStyle.METRO_LYRICS -> stringResource(R.string.lyrics_animation_metro)
                            }
                        )
                    },
                    onClick = { showLyricsAnimationStyleDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.lyrics_glow_effect)) },
                    description = { Text(stringResource(R.string.lyrics_glow_effect_desc)) },
                    trailingContent = {
                        Switch(
                            checked = lyricsGlowEffect,
                            onCheckedChange = onLyricsGlowEffectChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (lyricsGlowEffect) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onLyricsGlowEffectChange(!lyricsGlowEffect) }
                ),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && lyricsAnimationStyle == LyricsAnimationStyle.VIVIMUSIC_1) {
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.lyrics),
                        title = { Text(stringResource(R.string.apple_music_lyrics_blur)) },
                        description = { Text(stringResource(R.string.apple_music_lyrics_blur_desc)) },
                        trailingContent = {
                            Switch(
                                checked = appleMusicLyricsBlur,
                                onCheckedChange = onAppleMusicLyricsBlurChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            id = if (appleMusicLyricsBlur) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onAppleMusicLyricsBlurChange(!appleMusicLyricsBlur) }
                    )
                } else null,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.lyrics),
                        title = { Text(stringResource(R.string.standard_lyrics_blur)) },
                        description = { Text(stringResource(R.string.apple_music_lyrics_blur_desc)) },
                        trailingContent = {
                            Switch(
                                checked = lyricsStandardBlur,
                                onCheckedChange = onLyricsStandardBlurChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            id = if (lyricsStandardBlur) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onLyricsStandardBlurChange(!lyricsStandardBlur) }
                    )
                } else null,
                Material3SettingsItem(
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.lyrics_auto_scroll)) },
                    trailingContent = {
                        Switch(
                            checked = lyricsScroll,
                            onCheckedChange = onLyricsScrollChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (lyricsScroll) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onLyricsScrollChange(!lyricsScroll) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.swipe),
                    title = { Text(stringResource(R.string.lyrics_swipe_to_change_song)) },
                    description = { Text(stringResource(R.string.lyrics_swipe_to_change_song_desc)) },
                    trailingContent = {
                        Switch(
                            checked = swipeLyrics,
                            onCheckedChange = onSwipeLyricsChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (swipeLyrics) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onSwipeLyricsChange(!swipeLyrics) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.play),
                    title = { Text(stringResource(R.string.lyrics_thumbnail_play_pause)) },
                    description = { Text(stringResource(R.string.lyrics_thumbnail_play_pause_desc)) },
                    trailingContent = {
                        Switch(
                            checked = enableLyricsThumbnailPlayPause,
                            onCheckedChange = onEnableLyricsThumbnailPlayPauseChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (enableLyricsThumbnailPlayPause) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onEnableLyricsThumbnailPlayPauseChange(!enableLyricsThumbnailPlayPause) }
                )
            )
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.misc),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.nav_bar),
                    title = { Text(stringResource(R.string.default_open_tab)) },
                    description = {
                        Text(
                            when (defaultOpenTab) {
                                NavigationTab.HOME -> stringResource(R.string.home)
                                NavigationTab.SEARCH -> stringResource(R.string.search)
                                NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
                            }
                        )
                    },
                    onClick = { showDefaultOpenTabDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.tab),
                    title = { Text(stringResource(R.string.default_lib_chips)) },
                    description = {
                        Text(
                            when (defaultChip) {
                                LibraryFilter.SONGS -> stringResource(R.string.songs)
                                LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                                LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                                LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                                LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                            }
                        )
                    },
                    onClick = { showDefaultChipDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.swipe),
                    title = { Text(stringResource(R.string.swipe_song_to_add)) },
                    trailingContent = {
                        Switch(
                            checked = swipeToSong,
                            onCheckedChange = onSwipeToSongChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (swipeToSong) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onSwipeToSongChange(!swipeToSong) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.swipe),
                    title = { Text(stringResource(R.string.swipe_song_to_remove)) },
                    trailingContent = {
                        Switch(
                            checked = swipeToRemoveSong,
                            onCheckedChange = onSwipeToRemoveSongChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (swipeToRemoveSong) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onSwipeToRemoveSongChange(!swipeToRemoveSong) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.grid_view),
                    title = { Text(stringResource(R.string.grid_cell_size)) },
                    description = {
                        Text(
                            when (gridItemSize) {
                                GridItemSize.BIG -> stringResource(R.string.big)
                                GridItemSize.SMALL -> stringResource(R.string.small)
                            }
                        )
                    },
                    onClick = { showGridSizeDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.grid_view),
                    title = { Text(stringResource(R.string.display_density)) },
                    description = {
                        Text(DensityScale.fromValue(densityScale).label)
                    },
                    onClick = { showDensityScaleDialog = true }
                )
            )
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.auto_playlists),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.favorite),
                    title = { Text(stringResource(R.string.show_liked_playlist)) },
                    trailingContent = {
                        Switch(
                            checked = showLikedPlaylist,
                            onCheckedChange = onShowLikedPlaylistChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (showLikedPlaylist) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onShowLikedPlaylistChange(!showLikedPlaylist) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.offline),
                    title = { Text(stringResource(R.string.show_downloaded_playlist)) },
                    trailingContent = {
                        Switch(
                            checked = showDownloadedPlaylist,
                            onCheckedChange = onShowDownloadedPlaylistChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (showDownloadedPlaylist) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onShowDownloadedPlaylistChange(!showDownloadedPlaylist) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.trending_up),
                    title = { Text(stringResource(R.string.show_top_playlist)) },
                    trailingContent = {
                        Switch(
                            checked = showTopPlaylist,
                            onCheckedChange = onShowTopPlaylistChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (showTopPlaylist) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onShowTopPlaylistChange(!showTopPlaylist) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.cached),
                    title = { Text(stringResource(R.string.show_cached_playlist)) },
                    trailingContent = {
                        Switch(
                            checked = showCachedPlaylist,
                            onCheckedChange = onShowCachedPlaylistChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (showCachedPlaylist) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onShowCachedPlaylistChange(!showCachedPlaylist) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.backup),
                    title = { Text(stringResource(R.string.show_uploaded_playlist)) },
                    trailingContent = {
                        Switch(
                            checked = showUploadedPlaylist,
                            onCheckedChange = onShowUploadedPlaylistChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (showUploadedPlaylist) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onShowUploadedPlaylistChange(!showUploadedPlaylist) }
                )
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.appearance)) },
        navigationIcon = {
            IconButton(
                onClick = { navController.navigateUp() },
                onLongClick = { navController.backToMain() },
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        }
    )
}

enum class DarkMode {
    ON,
    OFF,
    AUTO,
}

enum class NavigationTab {
    HOME,
    SEARCH,
    LIBRARY,
}

enum class LyricsPosition {
    LEFT,
    CENTER,
    RIGHT,
}

enum class PlayerTextAlignment {
    SIDED,
    CENTER,
}
