/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.convx.music.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp

/**
 * Where the mini player's own pill sits on screen, in root coordinates.
 *
 * Module-level state for the same reason the artwork rects are: the pill lives in the
 * floating nav bar, the sheet lives in the player, they are far apart in the tree, and
 * only the overlay -- drawn once, in between -- reads it.
 */
private val miniContainerRect = mutableStateOf<Rect?>(null)

/**
 * Call on the mini player's outermost pill box. Purely observational: it records the
 * pill's bounds and changes nothing about how the pill renders.
 */
fun Modifier.registerMiniContainerRect(): Modifier = onGloballyPositioned {
    if (it.isAttached) miniContainerRect.value = it.boundsInRoot()
}

/**
 * The surface that grows from the mini pill into the full sheet.
 *
 * The sheet itself cannot do this. It is a full-width Box whose content is measured at
 * full width the whole way up, so starting it at the pill's rect would mean scaling the
 * real content -- every glyph and line of text distorting on the way. Instead this draws
 * a plain coloured rounded rect over the same journey, while the pill fades out beneath
 * it and the sheet's real content fades in above it. That is the same division of labour
 * [PlayerArtworkMorphOverlay] already uses for the artwork, and it is why the two can
 * share one progress window.
 *
 * Runs over 0..[handoffProgress] and is gone by the point the sheet's own content starts
 * arriving -- exactly where `BottomSheet` puts its crossfade. Past the handoff the real
 * sheet is opaque, and drawing this over it would only wash it out.
 *
 * `transformOrigin` is pinned top-left so a plain `translationX/Y` puts the scaled result
 * exactly at the interpolated rect, with no extra centre-offset maths. Same technique as
 * the artwork overlay.
 */
@Composable
fun PlayerContainerMorphOverlay(
    progress: Float,
    handoffProgress: Float,
    color: Color,
    expandedCornerRadius: Dp,
) {
    val mini = miniContainerRect.value ?: return
    if (progress <= 0f || progress >= handoffProgress) return
    // onGloballyPositioned can fire once with a degenerate size before layout settles.
    if (mini.width <= 0f || mini.height <= 0f) return

    val fraction = (progress / handoffProgress).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val full = Rect(0f, 0f, size.width, size.height)
                if (size.width <= 0f || size.height <= 0f) return@graphicsLayer
                val rect = sharedContainerRect(mini, full, fraction)

                transformOrigin = TransformOrigin(0f, 0f)
                translationX = rect.left
                translationY = rect.top
                scaleX = rect.width / size.width
                scaleY = rect.height / size.height

                val radius = sharedContainerCornerRadius(
                    // The nav bar draws the pill as a 50%-rounded capsule, so its
                    // radius is half its height by definition. Derived rather than
                    // passed in, so restyling the pill cannot leave the two out of
                    // step.
                    collapsedCornerRadius = mini.height / 2f,
                    expandedCornerRadius = expandedCornerRadius.toPx(),
                    progress = fraction,
                    screenCornerExpansionProgress = 0f,
                )
                // The shape is applied in the layer's own (unscaled) space and then
                // scaled with it, so a radius set naively would be squashed by exactly
                // the amount the layer is shrunk. Dividing it back out keeps the corner
                // the size it is meant to look on screen. scaleX alone: the two axes
                // differ, and matching the horizontal reads correctly on a pill that is
                // far wider than it is tall.
                val onScreenRadius = if (scaleX > 0f) radius / scaleX else radius
                shape = RoundedCornerShape(onScreenRadius)
                clip = true
                // Fades out into the sheet's own arrival rather than vanishing on the
                // frame the handoff is crossed.
                alpha = (1f - fraction * fraction).coerceIn(0f, 1f)
            }
            .background(color),
    )
}
