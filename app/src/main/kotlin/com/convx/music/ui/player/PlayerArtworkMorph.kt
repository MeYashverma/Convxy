/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.convx.music.ui.player

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size as CoilSize

/**
 * Registered bounds of the mini player's own artwork and the full player's own artwork,
 * in root/window coordinates -- updated from `onGloballyPositioned` on each, read once
 * by [PlayerArtworkMorphOverlay].
 *
 * Plain module-level state, not a hoisted parameter, for the same reason
 * `SharedArtwork.kt`'s `placedArtwork` map is plain state: the two artworks live in
 * composables that are far apart in the tree (`MiniPlayer.kt`'s collapsed content,
 * `Thumbnail.kt`'s expanded content, orchestrated from `BottomSheet.kt` in between) and
 * neither needs to compose off the other's position -- only the overlay, drawn once,
 * reads both.
 */
private val miniArtworkRect = mutableStateOf<Rect?>(null)
private val fullArtworkRect = mutableStateOf<Rect?>(null)

/**
 * Call on the mini player's own artwork box. Purely observational: records where the
 * artwork is on screen and changes nothing about how the mini player itself renders.
 */
fun Modifier.registerMiniArtworkRect(): Modifier = onGloballyPositioned {
    if (it.isAttached) miniArtworkRect.value = it.boundsInRoot()
}

/** Call on the full player's own artwork container. Same contract as
 *  [registerMiniArtworkRect]. */
fun Modifier.registerFullArtworkRect(): Modifier = onGloballyPositioned {
    if (it.isAttached) fullArtworkRect.value = it.boundsInRoot()
}

/**
 * Draws ONE artwork image growing from the mini player's registered rect to the full
 * player's, as [progress] runs 0..[handoffProgress].
 *
 * Sized to the FULL (target) rect, not the mini one -- scaling DOWN from there to the
 * mini size at fraction 0, then back up to 1:1 (its native size) at the handoff. This
 * is the opposite of the first version, which sized to the small mini rect and scaled
 * UP: scaling up stretches whatever resolution the underlying image happens to be,
 * which read as soft mid-growth even after requesting a larger decode, because the
 * COMPOSE LAYOUT SIZE (what a bitmap gets stretched across) was still the tiny mini
 * box the whole time. Sizing to full and scaling down means the on-screen image is
 * never asked to be bigger than the pixels it actually has -- downsampling stays
 * sharp at any fraction, which is exactly the technique Melox's own
 * `PlayerSheetArtworkOverlay` uses (see the scratchpad clone read for this fix).
 *
 * `transformOrigin` pinned to the top-left (not the layer's default center) so a
 * plain `translationX/Y = rect.left/top` places the SCALED result exactly at
 * [sharedArtworkRect]'s interpolated rect, with no extra center-offset math needed.
 *
 * A fresh [AsyncImage] of the same URL, not a captured GraphicsLayer of the mini
 * player's own rendering (Melox's technique) -- both artworks show identical content,
 * so a pixel capture buys nothing a positioned image doesn't, and Coil's cache is
 * already warm from the mini player's own load.
 *
 * Circular throughout its growth, matching the mini player's own shape — it does not
 * attempt to morph into the full player's (different) shape. The existing
 * `PLAYER_LAYER_HANDOFF_PROGRESS` alpha crossfade already in `BottomSheet.kt` is what
 * swaps to the real full-player artwork and its own shape once this overlay's growth
 * finishes, so the shape change happens under a fade rather than being animated
 * explicitly.
 */
@Composable
fun PlayerArtworkMorphOverlay(
    thumbnailUrl: String?,
    progress: Float,
    handoffProgress: Float,
) {
    val mini = miniArtworkRect.value
    val full = fullArtworkRect.value
    if (mini == null || full == null || thumbnailUrl == null) return
    if (progress <= 0f || progress >= handoffProgress) return
    // Both rects are registered from onGloballyPositioned, which can fire once with
    // a degenerate (zero or negative) size before the real layout pass settles --
    // measured a crash on a Samsung device: coil3.size.Dimension throws on any px
    // <= 0, and full.width/height fed CoilSize directly with no floor.
    if (mini.width <= 0f || mini.height <= 0f || full.width <= 0f || full.height <= 0f) return

    val fraction = (progress / handoffProgress).coerceIn(0f, 1f)
    val density = LocalDensity.current
    val boxWidthDp = with(density) { full.width.toDp() }
    val boxHeightDp = with(density) { full.height.toDp() }

    Box(
        modifier = Modifier
            .size(boxWidthDp, boxHeightDp)
            .graphicsLayer {
                val rect = sharedArtworkRect(mini, full, fraction)
                transformOrigin = TransformOrigin(0f, 0f)
                translationX = rect.left
                translationY = rect.top
                scaleX = rect.width / full.width
                scaleY = rect.height / full.height
            }
            .clip(CircleShape)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(thumbnailUrl)
                // Full player's own pixel size -- this Box is laid out at that size
                // now (see the class doc), so this is what it will actually be drawn
                // at, at fraction 1. Coil's memory cache is keyed by request size, so
                // reusing the SAME size the full player's own artwork already
                // requested reuses that cache entry instead of triggering a second
                // decode.
                .size(CoilSize(full.width.toInt(), full.height.toInt()))
                .crossfade(false)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(boxWidthDp, boxHeightDp),
        )
    }
}
