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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
    progress: Float,
    onSeek: (Float) -> Unit,
    playedColor: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    bars: Int = 28,
    seed: Int = 0,
) {
    val heights = remember(seed, bars) { waveformBars(seed, bars) }
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val shown = (dragFraction ?: progress).coerceIn(0f, 1f)

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
