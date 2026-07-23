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
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.music.vivi.constants.IosOverscrollKey
import com.music.vivi.utils.rememberPreference
import kotlinx.coroutines.launch

/** Shared pull state for [heroPullZoom]; read [Animatable.value] to derive a hero scale. */
@Composable
fun rememberHeroPull(): Animatable<Float, AnimationVector1D> = remember { Animatable(0f) }

/**
 * Everything a hero screen needs for pull-to-zoom, so each one is two lines
 * instead of five: pass [scale] to `HeroBackground(heroScale = …)` and
 * [Modifier.heroPullZoom] to its list.
 */
@Stable
class HeroZoom(
    val pull: Animatable<Float, AnimationVector1D>,
    val maxPull: Float,
    val enabled: Boolean,
) {
    val scale: Float
        get() = if (enabled) 1f + (pull.value / maxPull) * 0.18f else 1f
}

@Composable
fun rememberHeroZoom(maxPull: Dp = 220.dp): HeroZoom {
    val pull = rememberHeroPull()
    val maxPullPx = with(LocalDensity.current) { maxPull.toPx() }
    val enabled by rememberPreference(IosOverscrollKey, defaultValue = false)
    return remember(pull, maxPullPx, enabled) { HeroZoom(pull, maxPullPx, enabled) }
}

/** Applies [heroPullZoom] only when the iOS-motion preference is on. */
fun Modifier.heroPullZoom(zoom: HeroZoom): Modifier =
    if (zoom.enabled) heroPullZoom(zoom.pull, zoom.maxPull) else this

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
