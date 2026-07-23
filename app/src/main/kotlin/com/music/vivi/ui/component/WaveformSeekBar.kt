/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.random.Random

/**
 * Deterministic pseudo-waveform: [count] bar heights in 0..1, stable for a given
 * [seed] (e.g. a song's mediaId hash) so every play of the same song draws the
 * same shape. Not real amplitude data — a synthetic SoundCloud-style envelope.
 */
fun waveformBars(seed: Int, count: Int): FloatArray {
    val rnd = Random(seed)
    var prev = 0.5f
    return FloatArray(count) {
        // Random-walk so neighbouring bars relate (reads like audio, not white noise).
        prev = (prev + (rnd.nextFloat() - 0.5f) * 0.6f).coerceIn(0.12f, 1f)
        prev
    }
}

/**
 * A small SoundCloud-style waveform that doubles as a seek bar: bars up to
 * [progress] are painted [playedColor], the rest [trackColor]; tap or horizontal
 * drag reports the target fraction (0..1) via [onSeek].
 */
@Composable
fun WaveformSeekBar(
    progress: () -> Float,
    onSeek: (Float) -> Unit,
    playedColor: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    bars: Int = 28,
    seed: Int = 0,
) {
    val heights = remember(seed, bars) { waveformBars(seed, bars) }
    var dragFraction by remember { mutableStateOf<Float?>(null) }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { o -> onSeek((o.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { o -> dragFraction = (o.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = { dragFraction?.let(onSeek); dragFraction = null },
                    onDragCancel = { dragFraction = null },
                ) { change, _ ->
                    dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                }
            }
    ) {
        val n = heights.size
        if (n == 0 || size.width <= 0f) return@Canvas
        // Read progress in the draw phase so a playing song repaints the fill
        // without recomposing the mini player.
        val shown = (dragFraction ?: progress()).coerceIn(0f, 1f)
        val gapTotal = size.width * 0.4f
        val gap = gapTotal / (n - 1).coerceAtLeast(1)
        val barW = (size.width - gap * (n - 1)) / n
        val midY = size.height / 2f
        val minH = size.height * 0.14f
        for (i in 0 until n) {
            val h = (heights[i] * size.height).coerceAtLeast(minH)
            val x = i * (barW + gap)
            val frac = (i + 0.5f) / n
            drawRoundRect(
                color = if (frac <= shown) playedColor else trackColor,
                topLeft = Offset(x, midY - h / 2f),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f),
            )
        }
    }
}

/**
 * A scrolling waveform that stays centered on the current playback position.
 * Shows only a fixed [visibleBars] window — bars scroll as the song plays.
 * Both edges fade out progressively via a horizontal alpha mask, so bars
 * dissolve into the background instead of popping in and out.
 * Bars are rounded pill shape.
 */
@Composable
fun ScrollingWaveformSeekBar(
    progress: () -> Float,
    onSeek: (Float) -> Unit,
    playedColor: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    totalBars: Int = 80,
    visibleBars: Int = 18,
    seed: Int = 0,
    edgeFade: Float = 0.22f,
) {
    val heights = remember(seed, totalBars) { waveformBars(seed, totalBars) }
    var dragOffset by remember { mutableStateOf<Float?>(null) }

    Canvas(
        modifier = modifier
            // Offscreen layer + DstIn gradient = progressive fade on both sides.
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to Color.Transparent,
                        edgeFade to Color.Black,
                        1f - edgeFade to Color.Black,
                        1f to Color.Transparent,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { o ->
                    val frac = (o.x / size.width).coerceIn(0f, 1f)
                    val shift = (frac - 0.5f) * visibleBars / totalBars
                    onSeek(((dragOffset ?: progress()) + shift).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { o ->
                        val frac = (o.x / size.width).coerceIn(0f, 1f)
                        dragOffset = ((dragOffset ?: progress()) + (frac - 0.5f) * visibleBars / totalBars).coerceIn(0f, 1f)
                    },
                    onDragEnd = { dragOffset?.let(onSeek); dragOffset = null },
                    onDragCancel = { dragOffset = null },
                ) { change, _ ->
                    val delta = -change.previousPosition.x + change.position.x
                    val shiftPerPx = visibleBars.toFloat() / totalBars / size.width
                    dragOffset = ((dragOffset ?: progress()) + delta * shiftPerPx).coerceIn(0f, 1f)
                }
            }
    ) {
        val n = heights.size
        if (n == 0 || size.width <= 0f || size.height <= 0f) return@Canvas

        val currentProgress = (dragOffset ?: progress()).coerceIn(0f, 1f)
        val midY = size.height / 2f
        val halfVisible = visibleBars / 2f
        val centerBar = currentProgress * (n - 1)

        // Evenly space bars across the full width
        val barWidth = size.width / visibleBars * 0.55f
        val gap = (size.width - barWidth * visibleBars) / (visibleBars + 1)

        for (j in 0 until visibleBars) {
            val barIndex = (centerBar - halfVisible + 0.5f + j).toInt()
            if (barIndex < 0 || barIndex >= n) continue

            val h = (heights[barIndex] * size.height * 0.9f).coerceAtLeast(size.height * 0.15f)
            val x = gap + j * (barWidth + gap) + barWidth / 2f

            val isPlayed = barIndex <= centerBar
            val color = if (isPlayed) playedColor else trackColor

            drawRoundRect(
                color = color,
                topLeft = Offset(x - barWidth / 2f, midY - h / 2f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}
