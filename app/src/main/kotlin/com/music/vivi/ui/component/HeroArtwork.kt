package com.music.vivi.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBars
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.music.vivi.R
import com.music.vivi.ui.theme.AppleTokens
import com.music.vivi.ui.theme.extractThemeColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Resolved hero image source with priority chain:
 * existing static artwork → animated/canvas artwork → first song artwork →
 * next song artwork → default music image as last resort.
 */
sealed class HeroSource {
    data class Artwork(val url: String, val isAnimated: Boolean = false) : HeroSource()
    data object Default : HeroSource()
}

/** Blur radius for [HeroBackground]'s Apple-Music-style blurred artwork. */
private val HeroBlurRadius = 48.dp

/**
 * Resolves the hero image per the priority chain.
 *
 * @param staticArt Primary artwork URL (e.g. album/playlist/artist art)
 * @param animatedArt Optional animated/canvas artwork URL
 * @param songs List of songs to fall back to for artwork
 */
@Composable
fun rememberHeroSource(
    staticArt: String?,
    animatedArt: String? = null,
    songs: List<Pair<String?, Boolean>> = emptyList(), // (thumbnailUrl, isAnimated)
): HeroSource {
    return remember(staticArt, animatedArt, songs) {
        when {
            !staticArt.isNullOrBlank() -> HeroSource.Artwork(staticArt, isAnimated = false)
            !animatedArt.isNullOrBlank() -> HeroSource.Artwork(animatedArt, isAnimated = true)
            else -> {
                val firstSongArt = songs.firstOrNull { !it.first.isNullOrBlank() }
                if (firstSongArt != null) {
                    HeroSource.Artwork(firstSongArt.first!!, isAnimated = firstSongArt.second)
                } else {
                    HeroSource.Default
                }
            }
        }
    }
}

/**
 * Extracts the dominant tint color from a hero artwork URL.
 * Returns [AppleTokens.AccentRed] as fallback until extraction completes.
 */
/**
 * Clamps an extracted artwork color into a pleasant deep background tint:
 * caps lightness (pale/skin/whitish colors become rich, legible) and floors
 * saturation (avoids washed-out grey). Keeps the hue.
 */
fun Color.asDeepTint(): Color {
    val hsl = FloatArray(3)
    androidx.core.graphics.ColorUtils.colorToHSL(toArgb(), hsl)
    hsl[1] = hsl[1].coerceAtLeast(0.28f)   // floor saturation
    hsl[2] = hsl[2].coerceIn(0.14f, 0.34f) // cap lightness → deep, not pale
    return Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
}

@Composable
fun rememberHeroTint(url: String?): Color {
    val context = LocalContext.current
    // Black until the artwork color is extracted (no red flash).
    var tint by remember(url) { mutableStateOf(Color.Black) }

    LaunchedEffect(url) {
        if (url == null) {
            tint = Color.Black
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(100, 100)
                    .allowHardware(false)
                    .build()
                val result = context.imageLoader.execute(request)
                val bitmap = result.image?.toBitmap()
                if (bitmap != null) {
                    tint = bitmap.extractThemeColor().asDeepTint()
                }
            } catch (_: Exception) {
                // Fallback stays as AccentRed
            }
        }
    }

    val animatedTint by androidx.compose.animation.animateColorAsState(
        targetValue = tint,
        animationSpec = androidx.compose.animation.core.tween(1000),
        label = "heroTint"
    )
    return animatedTint
}

/**
 * Album-screen style hero header: a full-width square artwork that fades
 * (DstIn vertical gradient) into whatever tint plane sits behind it, so the
 * image dissolves into the color. Meant to be the FIRST item of a LazyColumn
 * whose parent paints [com.music.vivi.ui.theme.AppleTokens] tint behind it.
 *
 * Falls back to the default music image when [artworkUrl] is null/blank.
 */
@Composable
fun AlbumStyleHeroImage(
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    // Extra pull-up beyond the status bar, to lift the square higher like AlbumScreen.
    extraPullUp: androidx.compose.ui.unit.Dp = 48.dp,
) {
    val topInset = androidx.compose.foundation.layout.WindowInsets.systemBars
        .asPaddingValues().calculateTopPadding()
    val density = LocalDensity.current
    val pullUpPx = with(density) { (topInset + extraPullUp).roundToPx() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            // Report a shorter height and draw shifted up: the image top tucks
            // under the status bar and following content sits tight (no gap).
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val h = (placeable.height - pullUpPx).coerceAtLeast(0)
                layout(placeable.width, h) {
                    placeable.place(0, -pullUpPx)
                }
            }
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black, Color.Transparent),
                        startY = size.height * 0.45f,
                        endY = size.height,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            },
    ) {
        if (!artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Image(
                painter = painterResource(R.drawable.music_note),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize(0.4f)
                    .align(Alignment.Center)
                    .graphicsLayer { alpha = 0.3f },
            )
        }
    }
}

/**
 * Full-bleed hero background: flat tint plane with the hero image on top,
 * fading vertically into the tint at its bottom edge.
 *
 * NO gradient scrim — the image dissolves into the color plane.
 * Adaptive contrast: descendants should use [AppleTokens.onColor] for text.
 */
@Composable
fun HeroBackground(
    tint: Color,
    heroSource: HeroSource,
    modifier: Modifier = Modifier,
    // Apple Music browse/player style: full-bleed heavily-blurred artwork with a
    // darkening scrim, instead of the default sharp top-hero fading to tint.
    blurArtwork: Boolean = false,
    // The [HeroSource.Default] placeholder music-note. Off for screens that want
    // a clean flat tint behind glass (e.g. search).
    showDefaultIcon: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier = modifier.background(tint)) {
        when (heroSource) {
            is HeroSource.Artwork -> {
                // Fade-in animation for the hero image (the "smooth transition"
                // as the artwork resolves).
                var visible by remember { mutableStateOf(false) }
                val alpha by animateFloatAsState(
                    targetValue = if (visible) 1f else 0f,
                    animationSpec = tween(durationMillis = 600),
                    label = "heroFadeIn",
                )

                if (blurArtwork) {
                    // Blurred lower layer: fills behind the list and dissolves
                    // into the tint (the primary-color gradient) toward the
                    // bottom.
                    AsyncImage(
                        model = heroSource.url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(HeroBlurRadius)
                            .graphicsLayer {
                                this.alpha = alpha
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.Black, Color.Transparent),
                                        startY = size.height * 0.5f,
                                        endY = size.height,
                                    ),
                                    blendMode = BlendMode.DstIn,
                                )
                            },
                        onSuccess = { visible = true },
                    )
                    // Sharp upper layer: the top half stays crisp, then dissolves
                    // so the blur takes over from roughly where the list begins.
                    AsyncImage(
                        model = heroSource.url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                this.alpha = alpha
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.Black, Color.Transparent),
                                        startY = size.height * 0.35f,
                                        endY = size.height * 0.6f,
                                    ),
                                    blendMode = BlendMode.DstIn,
                                )
                            },
                    )
                } else {
                    AsyncImage(
                        model = heroSource.url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                this.alpha = alpha
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.Black, Color.Transparent),
                                        startY = size.height * 0.4f,
                                        endY = size.height
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            },
                        onSuccess = { visible = true },
                    )
                }
            }
            is HeroSource.Default -> {
                // Default music image centered, tinted to blend
                if (showDefaultIcon) {
                    Image(
                        painter = painterResource(R.drawable.music_note),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize(0.35f)
                            .align(Alignment.Center)
                            .graphicsLayer { alpha = 0.25f },
                    )
                }
            }
        }
        content()
    }
}
