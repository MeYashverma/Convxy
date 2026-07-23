/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.runtime.Composable
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
import kotlinx.coroutines.launch
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
 */
class IosOverscrollEffect(
    density: Density,
    private val scope: CoroutineScope,
) : OverscrollEffect {

    private val maxPull = with(density) { MaxPull.toPx() }
    private val offset = Animatable(0f)

    override val isInProgress: Boolean
        get() = offset.value != 0f

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset,
    ): Offset {
        var selfConsumed = 0f
        val current = offset.value

        // Dragging back toward rest pays down the existing stretch before the
        // list itself gets to scroll — otherwise the content jumps.
        if (current != 0f && delta.y != 0f && sign(delta.y) != sign(current)) {
            val target = current + delta.y
            val settled = if (sign(target) != sign(current)) 0f else target
            selfConsumed = settled - current
            scope.launch { offset.snapTo(settled) }
        }

        val remaining = Offset(delta.x, delta.y - selfConsumed)
        val consumedByScroll = performScroll(remaining)
        val leftover = remaining - consumedByScroll

        // Leftover means the list hit an edge, so stretch — with progressive
        // resistance, so the further it is pulled the harder it gets.
        if (leftover.y != 0f) {
            val resistance = (1f - abs(offset.value) / maxPull).coerceIn(0f, 1f) * 0.5f
            val stretched = (offset.value + leftover.y * resistance).coerceIn(-maxPull, maxPull)
            scope.launch { offset.snapTo(stretched) }
            selfConsumed += leftover.y
        }

        return Offset(consumedByScroll.x, consumedByScroll.y + selfConsumed)
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity,
    ) {
        performFling(velocity)
        if (offset.value != 0f) {
            offset.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.55f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
    }

    override val node: DelegatableNode = IosOverscrollNode { offset.value }
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
    override fun createOverscrollEffect(): OverscrollEffect = IosOverscrollEffect(density, scope)

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
