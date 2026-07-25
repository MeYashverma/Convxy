/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.utils

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.convx.music.constants.IosOverscrollKey
import com.convx.music.utils.rememberPreference

/**
 * Everything a hero screen needs for pull-to-zoom, so each one is two lines
 * instead of five: pass [scale] to `HeroBackground(heroScale = …)` and
 * [Modifier.heroPullZoom] to its list.
 *
 * [pullState] is a plain [MutableFloatState] rather than an
 * [androidx.compose.animation.core.Animatable] mutated via `scope.launch { snapTo(...) }`
 * on every scroll delta — that pattern let a fast drag queue up dozens of
 * coroutines racing each other (same bug class as the app's overscroll had),
 * and the zoom could get stuck mid-scale when one of them applied a stale
 * value after a newer one. Scroll-time updates are now a direct synchronous
 * write; a coroutine (and a real spring) is only used once, on release.
 */
@Stable
class HeroZoom internal constructor(
    internal val pullState: MutableFloatState,
    val maxPull: Float,
    val enabled: Boolean,
) {
    val scale: Float
        get() = if (enabled) 1f + (pullState.floatValue / maxPull) * 0.18f else 1f
}

@Composable
fun rememberHeroZoom(maxPull: Dp = 220.dp): HeroZoom {
    val pullState = remember { mutableFloatStateOf(0f) }
    val maxPullPx = with(LocalDensity.current) { maxPull.toPx() }
    val enabled by rememberPreference(IosOverscrollKey, defaultValue = false)
    return remember(pullState, maxPullPx, enabled) { HeroZoom(pullState, maxPullPx, enabled) }
}

/**
 * What to pass as a hero list's `overscrollEffect`: null while pull-to-zoom owns
 * the top pull, otherwise the ambient effect, so switching the motion preference
 * off leaves the list with normal overscroll rather than none at all.
 */
@Composable
fun HeroZoom.listOverscroll(): OverscrollEffect? =
    if (enabled) null else rememberOverscrollEffect()

/**
 * Pull-to-zoom for a hero-header list: absorbs only the top-edge (pull-down) overscroll
 * into [HeroZoom.pullState] (0..maxPull px, progressive resistance) and springs back on
 * release. The caller maps [HeroZoom.scale] to a scale and passes it to
 * HeroBackground(heroScale=...). Top-only, so it must NOT be used on screens whose top
 * pull drives pull-to-refresh. No-ops when the iOS-motion preference is off.
 */
fun Modifier.heroPullZoom(zoom: HeroZoom): Modifier = composed {
    if (!zoom.enabled) return@composed this

    val pullState = zoom.pullState
    val maxPull = zoom.maxPull
    val connection = remember(zoom) {
        object : NestedScrollConnection {
            // Scrolling content up while pulled pays down the zoom first.
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val curr = pullState.floatValue
                if (curr > 0f && available.y < 0f) {
                    val newValue = (curr + available.y).coerceAtLeast(0f)
                    pullState.floatValue = newValue
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
                val resistance = (1f - pullState.floatValue / maxPull).coerceIn(0f, 1f) * 0.5f
                val newValue = (pullState.floatValue + available.y * resistance).coerceIn(0f, maxPull)
                pullState.floatValue = newValue
                return available
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullState.floatValue != 0f) {
                    // Snappy spring-back on release — StiffnessLow read as "waits,
                    // then drifts down"; Medium gives the immediate iOS rubber-band.
                    animate(
                        initialValue = pullState.floatValue,
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = 0.72f,
                            stiffness = Spring.StiffnessMedium,
                        ),
                    ) { value, _ -> pullState.floatValue = value }
                    return available
                }
                return Velocity.Zero
            }
        }
    }
    // Translate the WHOLE list down by the pull so the header and the content
    // below move as one unit (iOS stretch), not just the image scaling in place.
    this
        .graphicsLayer { translationY = pullState.floatValue }
        .nestedScroll(connection)
}
