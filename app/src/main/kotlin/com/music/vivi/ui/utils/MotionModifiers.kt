/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
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
 *
 * @param allowTopPull set false on screens whose list is inside a PullToRefreshBox, so the
 *   top-edge pull is left for pull-to-refresh instead of being eaten by the bounce.
 */
fun Modifier.iosOverscroll(enabled: Boolean, allowTopPull: Boolean = true): Modifier = composed {
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
                // Leave the top-edge pull to pull-to-refresh when asked.
                if (available.y > 0f && !allowTopPull && offset.value <= 0f) return Offset.Zero
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

/** Shared pull state for [heroPullZoom]; read [Animatable.value] to derive a hero scale. */
@Composable
fun rememberHeroPull(): Animatable<Float, AnimationVector1D> = remember { Animatable(0f) }

/**
 * Pull-to-zoom for a hero-header list: absorbs only the top-edge (pull-down) overscroll
 * into [pull] (0..[maxPull] px, progressive resistance) and springs back on release. The
 * caller maps [pull] to a scale and passes it to HeroBackground(heroScale=...). Top-only,
 * so it must NOT be used on screens whose top pull drives pull-to-refresh.
 */
fun Modifier.heroPullZoom(
    pull: Animatable<Float, AnimationVector1D>,
    maxPull: Float,
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val connection = remember(maxPull, pull) {
        object : NestedScrollConnection {
            // Scrolling content up while pulled pays down the zoom first.
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val curr = pull.value
                if (curr > 0f && available.y < 0f) {
                    val newValue = (curr + available.y).coerceAtLeast(0f)
                    scope.launch { pull.snapTo(newValue) }
                    return Offset(0f, newValue - curr)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (available.y <= 0f) return Offset.Zero
                val resistance = (1f - pull.value / maxPull).coerceIn(0f, 1f) * 0.5f
                val newValue = (pull.value + available.y * resistance).coerceIn(0f, maxPull)
                scope.launch { pull.snapTo(newValue) }
                return available
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pull.value != 0f) {
                    pull.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = 0.6f,
                            stiffness = Spring.StiffnessLow,
                        ),
                    )
                    return available
                }
                return Velocity.Zero
            }
        }
    }
    this.nestedScroll(connection)
}
