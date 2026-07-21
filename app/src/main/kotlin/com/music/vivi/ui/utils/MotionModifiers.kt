/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/**
 * iOS-style rubber-band overscroll for a scroll container (LazyColumn/Grid/Column).
 *
 * Absorbs the leftover scroll the child couldn't use (i.e. only at the top/bottom edges),
 * translating the content with progressive resistance, and springs it back on release.
 * It never consumes normal in-bounds scroll, so scrolling itself can't break — worst case
 * the bounce feel just needs tuning. Gated by [enabled] (a settings toggle).
 */
fun Modifier.iosOverscroll(enabled: Boolean): Modifier = composed {
    if (!enabled) return@composed this

    val maxPull = with(LocalDensity.current) { 160.dp.toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val connection = remember(maxPull) {
        object : NestedScrollConnection {
            // Dragging back toward rest first pays down any existing bounce offset.
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val curr = offset.value
                if (curr == 0f || available.y == 0f || sign(available.y) == sign(curr)) {
                    return Offset.Zero
                }
                val target = curr + available.y
                val crossedZero = sign(target) != sign(curr)
                val newValue = if (crossedZero) 0f else target
                scope.launch { offset.snapTo(newValue) }
                val consumedY = if (crossedZero) -curr else available.y
                return Offset(0f, consumedY)
            }

            // Leftover the list couldn't consume = we're at an edge → stretch.
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (available.y == 0f) return Offset.Zero
                // Progressive resistance: harder to pull the further it is stretched.
                val resistance = (1f - abs(offset.value) / maxPull).coerceIn(0f, 1f) * 0.5f
                val target = (offset.value + available.y * resistance).coerceIn(-maxPull, maxPull)
                scope.launch { offset.snapTo(target) }
                return available
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (offset.value != 0f) {
                    offset.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = 0.55f,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    )
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    this
        .nestedScroll(connection)
        .graphicsLayer { translationY = offset.value }
}
