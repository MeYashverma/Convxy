/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.settings

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.PlayerArtworkStyle
import com.music.vivi.constants.PlayerArtworkStyleKey
import com.music.vivi.constants.PlayerBackgroundStyle
import com.music.vivi.constants.PlayerBackgroundStyleKey
import com.music.vivi.constants.PlayerGradientBottomKey
import com.music.vivi.constants.PlayerGradientTopKey
import com.music.vivi.constants.PlayerStaticColorKey
import com.music.vivi.ui.component.ColorPickerDialog
import com.music.vivi.ui.component.IconButton as AppIconButton
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import kotlinx.coroutines.flow.MutableStateFlow

private val PresetCardWidth = 148.dp

/**
 * Player theme picker: preset cards that render a live miniature of the real
 * player, so the artwork shape and the background style are chosen by looking
 * at them rather than by reading a list of names.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerThemeScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (artworkStyle, onArtworkStyleChange) = rememberEnumPreference(
        PlayerArtworkStyleKey, defaultValue = PlayerArtworkStyle.CARD
    )
    val (background, onBackgroundChange) = rememberEnumPreference(
        PlayerBackgroundStyleKey, defaultValue = PlayerBackgroundStyle.APPLE_MUSIC
    )
    val (staticColorInt, onStaticColorChange) = rememberPreference(
        PlayerStaticColorKey, defaultValue = 0xFF1A1A1A.toInt()
    )
    val (gradientTopInt, onGradientTopChange) = rememberPreference(
        PlayerGradientTopKey, defaultValue = 0xFF3A1C71.toInt()
    )
    val (gradientBottomInt, onGradientBottomChange) = rememberPreference(
        PlayerGradientBottomKey, defaultValue = 0xFF0B0B0B.toInt()
    )

    var showStaticPicker by rememberSaveable { mutableStateOf(false) }
    var showTopPicker by rememberSaveable { mutableStateOf(false) }
    var showBottomPicker by rememberSaveable { mutableStateOf(false) }

    // Preview the song that is actually playing; fall back to the app icon.
    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by remember(playerConnection) {
        playerConnection?.mediaMetadata ?: MutableStateFlow(null)
    }.collectAsState()
    val artworkUrl = mediaMetadata?.thumbnailUrl

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState()),
    ) {
        SectionTitle(stringResource(R.string.player_theme_artwork))
        PresetRow {
            PlayerArtworkStyle.entries.forEach { style ->
                PresetCard(
                    label = when (style) {
                        PlayerArtworkStyle.CARD -> stringResource(R.string.player_theme_card)
                        PlayerArtworkStyle.VINYL -> stringResource(R.string.player_theme_vinyl)
                        PlayerArtworkStyle.CLOVER -> stringResource(R.string.player_theme_clover)
                    },
                    selected = artworkStyle == style,
                    onClick = { onArtworkStyleChange(style) },
                ) {
                    PlayerPreview(
                        artworkStyle = style,
                        background = background,
                        artworkUrl = artworkUrl,
                        staticColor = Color(staticColorInt),
                        gradientTop = Color(gradientTopInt),
                        gradientBottom = Color(gradientBottomInt),
                    )
                }
            }
        }

        SectionTitle(stringResource(R.string.player_background_style))
        PresetRow {
            PlayerBackgroundStyle.entries.filter {
                it != PlayerBackgroundStyle.BLUR || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            }.forEach { style ->
                PresetCard(
                    label = backgroundLabel(style),
                    selected = background == style,
                    onClick = { onBackgroundChange(style) },
                ) {
                    PlayerPreview(
                        artworkStyle = artworkStyle,
                        background = style,
                        artworkUrl = artworkUrl,
                        staticColor = Color(staticColorInt),
                        gradientTop = Color(gradientTopInt),
                        gradientBottom = Color(gradientBottomInt),
                    )
                }
            }
        }

        if (background == PlayerBackgroundStyle.STATIC) {
            ColorRow(
                title = stringResource(R.string.player_theme_static_color),
                color = Color(staticColorInt),
                onClick = { showStaticPicker = true },
            )
        }

        if (background == PlayerBackgroundStyle.CUSTOM_GRADIENT) {
            ColorRow(
                title = stringResource(R.string.player_theme_gradient_top),
                color = Color(gradientTopInt),
                onClick = { showTopPicker = true },
            )
            ColorRow(
                title = stringResource(R.string.player_theme_gradient_bottom),
                color = Color(gradientBottomInt),
                onClick = { showBottomPicker = true },
            )
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showStaticPicker) {
        ColorPickerDialog(
            initialColor = Color(staticColorInt),
            title = stringResource(R.string.player_theme_static_color),
            onDismiss = { showStaticPicker = false },
            onConfirm = { onStaticColorChange(it.toArgb()); showStaticPicker = false },
            defaultColor = Color(0xFF1A1A1A),
        )
    }
    if (showTopPicker) {
        ColorPickerDialog(
            initialColor = Color(gradientTopInt),
            title = stringResource(R.string.player_theme_gradient_top),
            onDismiss = { showTopPicker = false },
            onConfirm = { onGradientTopChange(it.toArgb()); showTopPicker = false },
            defaultColor = Color(0xFF3A1C71),
        )
    }
    if (showBottomPicker) {
        ColorPickerDialog(
            initialColor = Color(gradientBottomInt),
            title = stringResource(R.string.player_theme_gradient_bottom),
            onDismiss = { showBottomPicker = false },
            onConfirm = { onGradientBottomChange(it.toArgb()); showBottomPicker = false },
            defaultColor = Color(0xFF0B0B0B),
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.player_theme)) },
        navigationIcon = {
            AppIconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun backgroundLabel(style: PlayerBackgroundStyle) = when (style) {
    PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
    PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
    PlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
    PlayerBackgroundStyle.GLOW_ANIMATED -> stringResource(R.string.glow_animated)
    PlayerBackgroundStyle.APPLE_MUSIC -> stringResource(R.string.apple_music)
    PlayerBackgroundStyle.LIVE_MESH -> stringResource(R.string.live_mesh)
    PlayerBackgroundStyle.STATIC -> stringResource(R.string.player_theme_static)
    PlayerBackgroundStyle.CUSTOM_GRADIENT -> stringResource(R.string.player_theme_custom_gradient)
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun PresetRow(content: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) { content() }
}

@Composable
private fun PresetCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    preview: @Composable () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(PresetCardWidth)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(bottom = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.62f)
                .clip(RoundedCornerShape(18.dp))
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(18.dp),
                )
        ) { preview() }

        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                )
                .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    painterResource(R.drawable.check),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

/** Miniature of the real player: background wash + artwork shape + fake controls. */
@Composable
private fun PlayerPreview(
    artworkStyle: PlayerArtworkStyle,
    background: PlayerBackgroundStyle,
    artworkUrl: String?,
    staticColor: Color,
    gradientTop: Color,
    gradientBottom: Color,
) {
    val accent = MaterialTheme.colorScheme.primary
    val backdrop: Brush = when (background) {
        PlayerBackgroundStyle.DEFAULT -> Brush.verticalGradient(
            listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface)
        )
        PlayerBackgroundStyle.STATIC -> Brush.verticalGradient(listOf(staticColor, staticColor))
        PlayerBackgroundStyle.CUSTOM_GRADIENT ->
            Brush.verticalGradient(listOf(gradientTop, gradientBottom))
        PlayerBackgroundStyle.LIVE_MESH ->
            Brush.linearGradient(listOf(accent, Color.Black, accent.copy(alpha = 0.4f)))
        PlayerBackgroundStyle.GLOW_ANIMATED ->
            Brush.radialGradient(listOf(accent, Color.Black))
        else -> Brush.verticalGradient(listOf(accent.copy(alpha = 0.55f), Color.Black))
    }
    val onBackdrop = if (background == PlayerBackgroundStyle.DEFAULT) {
        MaterialTheme.colorScheme.onSurface
    } else {
        Color.White
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(backdrop)
            .padding(12.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(
                    when (artworkStyle) {
                        PlayerArtworkStyle.CARD -> RoundedCornerShape(10.dp)
                        else -> CircleShape
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                fallback = painterResource(R.drawable.vivi_music_icon),
                error = painterResource(R.drawable.vivi_music_icon),
                modifier = Modifier.fillMaxSize(),
            )
            if (artworkStyle == PlayerArtworkStyle.VINYL) {
                Canvas(Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2f
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.82f),
                        radius = r * 0.67f,
                        style = Stroke(width = r * 0.66f),
                    )
                    drawCircle(Color.Black, radius = r * 0.07f)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Bar(onBackdrop.copy(alpha = 0.9f), widthFraction = 0.8f, height = 7.dp)
        Spacer(Modifier.height(5.dp))
        Bar(onBackdrop.copy(alpha = 0.45f), widthFraction = 0.55f, height = 5.dp)
        Spacer(Modifier.height(12.dp))
        Bar(onBackdrop.copy(alpha = 0.35f), widthFraction = 1f, height = 4.dp)
        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Dot(onBackdrop.copy(alpha = 0.6f), 8.dp)
            Dot(onBackdrop, 20.dp)
            Dot(onBackdrop.copy(alpha = 0.6f), 8.dp)
        }
    }
}

@Composable
private fun Bar(color: Color, widthFraction: Float, height: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun Dot(color: Color, size: androidx.compose.ui.unit.Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}

@Composable
private fun ColorRow(title: String, color: Color, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
        )
    }
}
