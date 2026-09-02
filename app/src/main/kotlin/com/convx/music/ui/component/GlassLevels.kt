/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * The Liquid Glass intensity ladder.
 *
 * Every glass surface in the app belongs to one of three rungs, from the glass
 * that carries the whole screen down to the small inline elements nested inside
 * it. The rung decides how much light the surface reflects, how solid its frost
 * reads, and how much edge definition it gets — so the hierarchy is visible
 * before any content is even drawn:
 *
 *  - [PRIMARY] — surfaces that carry an entire layer of UI: the nav bar, tab bar,
 *    side panel, mini player, full-screen player, and the elevated panels that
 *    float above a dim (context menus, bottom-sheet menus, dialogs). Deepest
 *    frost, brightest specular edge, most substantial fill.
 *  - [SECONDARY] — floating controls that sit directly on content and must read
 *    as physical objects: circular glass buttons, hero action pills, floating
 *    action buttons. Clearer frost than PRIMARY (more of the content shows
 *    through), with the crisp edge that makes a small control read as an object.
 *  - [TERTIARY] — inline elements that live on top of a larger glass surface or
 *    on the page itself: segmented menu groups, settings groups, chips. Lightest
 *    fill and edge; these are accents of glass, not blocks of it.
 *
 * Two rendering families sit on this ladder:
 *
 *  1. Sampled glass (the [Modifier.liquidGlass] pipeline) — the nav bar, player
 *     and floating buttons. Those surfaces take their [GlassLevel] from this
 *     file's blur-scale mapping ([GlassLevel.blurScale]) while every other
 *     parameter stays user-tunable in Settings.
 *
 *  2. Translucent panels — surfaces that deliberately do NOT run the capture
 *     pipeline because they sit above a dim scrim in their own window (dialogs)
 *     or would otherwise cost a whole-window re-capture per frame (root-level
 *     menus — see OverlayMenu). They get the exact look of the pipeline's
 *     cheap/unsupported fallback path: a tinted translucent fill, a hairline
 *     light edge and a faint top sheen. This keeps every elevated surface on
 *     the same material even where the full pipeline is impossible.
 *
 * The distinction is the same one Apple draws: menus and alerts in iOS are
 * heavily frosted *translucent* layers, while only the small controls on the
 * now-playing screen get the refractive liquid treatment.
 */
enum class GlassLevel {
    PRIMARY,
    SECONDARY,
    TERTIARY,
}

/**
 * How much the sampled-glass pipeline intensifies its blur at each rung.
 *
 * [GlassEffectConfig.blurRadius] stays the user's master value everywhere;
 * the rung only scales it for the surface's place in the hierarchy, so the
 * Settings sliders keep meaning the same thing on every surface.
 *
 * [SECONDARY] deliberately does not equal [PRIMARY]: small floating controls
 * read as glass objects through their *edges* (rim + hairline), not through a
 * heavier frost, which would hide the content they float over.
 */
val GlassLevel.blurScale: Float
    get() = when (this) {
        GlassLevel.PRIMARY -> 2f
        GlassLevel.SECONDARY -> 1.5f
        GlassLevel.TERTIARY -> 1f
    }

/** Fill opacity of an un-sampled translucent panel at this rung. */
internal val GlassLevel.panelFillAlpha: Float
    get() = when (this) {
        GlassLevel.PRIMARY -> 0.94f
        GlassLevel.SECONDARY -> 0.86f
        GlassLevel.TERTIARY -> 0.72f
    }

/** Opacity of the hairline edge at this rung. */
internal val GlassLevel.edgeAlpha: Float
    get() = when (this) {
        GlassLevel.PRIMARY -> 0.16f
        GlassLevel.SECONDARY -> 0.12f
        GlassLevel.TERTIARY -> 0.08f
    }

/** Opacity of the top sheen (a soft light source across the top third). */
internal val GlassLevel.sheenAlpha: Float
    get() = when (this) {
        GlassLevel.PRIMARY -> 0.09f
        GlassLevel.SECONDARY -> 0.07f
        GlassLevel.TERTIARY -> 0.04f
    }

/**
 * Everything one translucent glass surface needs to draw itself, resolved for
 * the current theme. Built once per surface via [rememberGlassPanelColors] so
 * every menu, dialog, sheet and card in the app shares one adaptive recipe
 * instead of each picking its own Material color.
 */
@Immutable
data class GlassPanelColors(
    /** The translucent fill (tint over whatever is behind the panel). */
    val fill: Color,
    /** Hairline light-catching edge, drawn around the shape's rim. */
    val edge: Color,
    /** Soft light falloff across the top of the panel, under the content. */
    val sheen: Color,
    /** Foreground colour guaranteed to read against [fill] on this theme. */
    val content: Color,
)

/**
 * Resolves the [GlassLevel] panel recipe for the current theme.
 *
 * @param fill when specified, this exact colour is used as the panel's base
 *   tint instead of the theme's container colour — for callers that sit over
 *   an artwork-tinted backdrop and pass their own tint. The level's translucency
 *   still applies, so a custom tint and an auto tint behave the same way.
 */
@Composable
fun rememberGlassPanelColors(
    level: GlassLevel,
    fill: Color = Color.Unspecified,
): GlassPanelColors {
    val dark = MaterialTheme.colorScheme.surface.luminance() <= 0.5f
    val container = MaterialTheme.colorScheme.surfaceContainerHigh
    val onSurface = MaterialTheme.colorScheme.onSurface

    return remember(level, dark, container, onSurface, fill) {
        val base = if (fill.isSpecified) fill else container
        // Panels keep the theme's onSurface: the fill is translucent but heavy
        // enough (and the layer below dimmed enough) that the theme's own text
        // roles stay the readable choice, exactly as they are on plain
        // containers. Translucent chrome elsewhere in the app derives its text
        // colour by compositing (glassContentColorFor); panels don't need that
        // because their fill dominates what sits behind them.
        GlassPanelColors(
            fill = base.copy(alpha = base.alpha * level.panelFillAlpha),
            edge = if (dark) {
                Color.White.copy(alpha = level.edgeAlpha)
            } else {
                Color.Black.copy(alpha = level.edgeAlpha)
            },
            sheen = Color.White.copy(alpha = level.sheenAlpha * if (dark) 1f else 0.8f),
            content = onSurface,
        )
    }
}

/**
 * The translucent panel recipe shared by every un-sampled glass surface.
 *
 * Paints, in order: the [colors.fill] tint clipped to [shape], a soft specular
 * sheen across the top third (drawn under the panel's content), and the hairline
 * [colors.edge] around the rim. A panel using this on a scrim, and a liquid-glass
 * surface using its fallback path, are indistinguishable — which is the point:
 * the whole app reads as one material whether or not the capture pipeline ran.
 */
fun Modifier.glassPanelSurface(
    shape: Shape,
    colors: GlassPanelColors,
): Modifier = this
    .clip(shape)
    .background(colors.fill)
    .drawBehind {
        if (colors.sheen.alpha > 0f) {
            // A soft light source that dies out by 38% of the panel's height.
            // Drawn behind the children so it tints the glass, not the text.
            drawRect(
                brush = Brush.verticalGradient(
                    0f to colors.sheen,
                    0.38f to Color.Transparent,
                ),
            )
        }
    }
    .border(width = GlassPanelEdgeStroke, color = colors.edge, shape = shape)

/**
 * Hairline edge width shared by every translucent glass panel — the same stroke
 * Material3MenuGroup tiles and (indirectly) chips draw, so every edge in the
 * glass language is the same weight.
 */
internal val GlassPanelEdgeStroke = 0.7.dp
