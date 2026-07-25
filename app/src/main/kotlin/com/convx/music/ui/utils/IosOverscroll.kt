/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.utils

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlin.math.abs
import kotlin.math.sign

private val MaxPull = 160.dp

/**
 * iOS-style rubber-band overscroll, expressed as an [OverscrollEffect] so it can be
 * installed once via `LocalOverscrollFactory` instead of being applied per scroll
 * container. Every LazyColumn / LazyGrid / scrollable Column under the provider
 * bounces, including ones in screens nobody remembered to update.
 *
 * Replaces Android's stretch/glow edge effect while it is provided.
 *
 * The stretch itself is a plain mutable float mutated synchronously in
 * [applyToScroll] — not an [androidx.compose.animation.core.Animatable] — because
 * `applyToScroll` isn't suspend and every scroll delta during a drag used to launch
 * its own fire-and-forget `snapTo` coroutine. Under a fast drag that's dozens of
 * coroutines racing each other per second, and the offset could end up stuck on a
 * stale value from a coroutine that hadn't run yet. A coroutine (and a real spring)
 * is only needed once, on release, to animate back to rest.
 */
class IosOverscrollEffect(
    density: Density,
) : OverscrollEffect {

    private val maxPull = with(density) { MaxPull.toPx() }
    private val offsetState = mutableFloatStateOf(0f)

    override val isInProgress: Boolean
        get() = offsetState.floatValue != 0f

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset,
    ): Offset {
        var selfConsumed = 0f
        val current = offsetState.floatValue

        // Dragging back toward rest pays down the existing stretch before the
        // list itself gets to scroll — otherwise the content jumps.
        if (current != 0f && delta.y != 0f && sign(delta.y) != sign(current)) {
            val target = current + delta.y
            val settled = if (sign(target) != sign(current)) 0f else target
            selfConsumed = settled - current
            offsetState.floatValue = settled
        }

        val remaining = Offset(delta.x, delta.y - selfConsumed)
        val consumedByScroll = performScroll(remaining)
        val leftover = remaining - consumedByScroll

        // Leftover means the list hit an edge, so stretch — with progressive
        // resistance, so the further it is pulled the harder it gets. Starts
        // near 1:1 with the finger (iOS reads as immediate, not laggy) and
        // eases off as it nears maxPull.
        if (leftover.y != 0f) {
            val resistance = (1f - abs(offsetState.floatValue) / maxPull).coerceIn(0f, 1f).let { it * it * 0.85f + 0.15f }
            val stretched = (offsetState.floatValue + leftover.y * resistance).coerceIn(-maxPull, maxPull)
            offsetState.floatValue = stretched
            selfConsumed += leftover.y
        }

        return Offset(consumedByScroll.x, consumedByScroll.y + selfConsumed)
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity,
    ) {
        performFling(velocity)
        if (offsetState.floatValue != 0f) {
            // A new drag beats this: the scrollable cancels this suspend fling
            // (and this animate call with it) as soon as the next pointer-down
            // starts a fresh drag, so there's no coroutine to race against.
            animate(
                initialValue = offsetState.floatValue,
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.55f,
                    stiffness = Spring.StiffnessMedium,
                ),
            ) { value, _ -> offsetState.floatValue = value }
        }
    }

    override val node: DelegatableNode = IosOverscrollNode { offsetState.floatValue }
}

private class IosOverscrollNode(
    private val offsetY: () -> Float,
) : Modifier.Node(), LayoutModifierNode {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            // Read the offset inside the layer block so the bounce animates in the
            // draw phase, without re-laying-out the list every frame.
            placeable.placeWithLayer(0, 0) { translationY = offsetY() }
        }
    }
}

private class IosOverscrollFactory(
    private val density: Density,
    private val scope: CoroutineScope,
) : OverscrollFactory {
    override fun createOverscrollEffect(): OverscrollEffect = IosOverscrollEffect(density)

    override fun equals(other: Any?): Boolean =
        other is IosOverscrollFactory && other.density == density && other.scope === scope

    override fun hashCode(): Int = 31 * density.hashCode() + scope.hashCode()
}

/** Factory to hand to `LocalOverscrollFactory` to make the whole app bounce. */
@Composable
fun rememberIosOverscrollFactory(): OverscrollFactory {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    return remember(density, scope) { IosOverscrollFactory(density, scope) }
}
