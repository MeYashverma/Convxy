/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convxy.music.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.convxy.music.ui.component.backdrop.Backdrop
import com.convxy.music.ui.component.backdrop.backdrops.emptyBackdrop
import com.convxy.music.ui.component.backdrop.catalog.utils.InteractiveHighlight
import com.convxy.music.ui.component.backdrop.isRenderEffectSupported
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Geometry and material constants shared by every liquid glass control.
 *
 * The point of keeping these in one place is that a slider thumb, a toggle knob,
 * a pill and an icon button should all read as the same physical material — the
 * same rest size, the same growth on press, the same blur-to-refraction ramp.
 * When each component picked its own numbers they drifted apart and the UI read
 * as several approximations of glass instead of one.
 *
 * Provenance, so the numbers can be re-derived rather than trusted:
 *
 *  - Thumb/rail geometry and the press morph come from the vendored Kyant0
 *    catalog `LiquidSlider` (`ui/component/backdrop/catalog/components/`), which
 *    is the reference implementation the app already ships: a 40x24dp capsule
 *    thumb over a 6dp capsule rail, blurred and opaque at rest, clearing into a
 *    refracting lens as the press completes.
 *  - The growth ratios come from the same source's `DampedDragAnimation`
 *    (`pressedScale`) and the nav bar puck, which Convx tunes to 78/56.
 *  - Rest blur (8dp) and pressed lens (10dp height / 14dp amount) are the
 *    catalog's own values, kept as the 1.0 point of the user-preference scaling
 *    in [liquidThumbBlurScale] / [liquidThumbLensScale].
 */
object LiquidGlassTokens {

    /** Resting rail thickness. The catalog slider's 6dp capsule. */
    val SliderTrackHeight: Dp = 6f.dp

    /**
     * Thumb bounds while pressed. The catalog slider's capsule.
     *
     * The thumb's *layout* box is this size at all times; at rest it is scaled
     * down to [SliderThumbRestSize] by the layer block, which is what produces
     * the dot-to-capsule morph without a second surface or a re-measure.
     */
    val SliderThumbSize: DpSize = DpSize(40f.dp, 24f.dp)

    /**
     * Thumb diameter at rest. Non-uniform scaling of [SliderThumbSize] down to a
     * square this size turns the capsule into a circle, so a resting seek bar
     * shows a small round knob and a pressed one shows the full capsule — the
     * iOS 26 slider behaviour.
     */
    val SliderThumbRestSize: Dp = 12f.dp

    /**
     * Minimum height of a slider control. Small enough to sit in the player's
     * existing rhythm, large enough that the 24dp thumb has room to grow without
     * being clipped by a neighbour. Callers can raise it (44dp+) for a bigger
     * touch target; the rails and thumb stay vertically centred.
     */
    val SliderControlHeight: Dp = 32f.dp

    /** Rail blur at rest, in dp — the catalog slider's value. */
    const val SliderThumbRestBlurDp: Float = 8f

    /** Lens refraction height at full press, in dp — the catalog slider's value. */
    const val SliderThumbLensHeightDp: Float = 10f

    /** Lens refraction amount at full press, in dp — the catalog slider's value. */
    const val SliderThumbLensAmountDp: Float = 14f

    /** Drop shadow under a pressed thumb — the catalog slider's value. */
    val ThumbShadowRadius: Dp = 4f.dp

    /** Inner shadow radius at full press — the catalog slider's value. */
    val ThumbInnerShadowRadius: Dp = 4f.dp

    /**
     * Blur radius (dp) that [GlassEffectConfig] ships with. Used as the 1.0
     * reference point when scaling the catalog's fixed effect sizes by the
     * user's preference, so the defaults reproduce the reference slider exactly
     * and moving the preference moves it proportionally.
     */
    const val DefaultBlurRadiusDp: Float = 2f

    /** Lens amount (0..1) that [GlassEffectConfig] ships with — see above. */
    const val DefaultLensAmount: Float = 0.6f

    /** Lens height (0..1) that [GlassEffectConfig] ships with — see above. */
    const val DefaultLensHeight: Float = 0.4f

    /**
     * How much the thumb leans into a fast drag, relative to its own size. The
     * catalog slider divides the tracked velocity by 10 and clamps the resulting
     * stretch to +-0.2, which is what keeps a flick from tearing the thumb apart.
     */
    const val VelocityStretchDivisor: Float = 10f
    const val VelocityStretchMaxX: Float = 0.75f
    const val VelocityStretchMaxY: Float = 0.25f
    const val VelocityStretchClamp: Float = 0.2f
}

/**
 * True when [this] is a real recorded backdrop rather than the "nothing provided"
 * sentinel returned by [emptyBackdrop].
 *
 * Glass controls must check this before drawing: sampling an empty backdrop
 * yields a surface with no content behind it, which renders as a flat dark slab
 * — indistinguishable from the bug this replaced. Falling back to the control's
 * non-glass renderer is both cheaper and correct.
 */
fun Backdrop?.isLiveGlassBackdrop(): Boolean = this != null && this !== emptyBackdrop()

/** Horizontal inset of the rail from each edge, in px: a quarter of the thumb. */
internal fun liquidRailInsetPx(thumbWidthPx: Float): Float = thumbWidthPx * 0.25f

/** Usable rail span in px once both insets are removed. Never negative. */
internal fun liquidRailSpanPx(totalWidthPx: Float, thumbWidthPx: Float): Float =
    (totalWidthPx - 2f * liquidRailInsetPx(thumbWidthPx)).coerceAtLeast(0f)

/** 0..1 position along the rail, guarded against a zero-width or zero-range control. */
internal fun liquidSliderProgress(value: Float, rangeStart: Float, rangeSpan: Float): Float =
    if (rangeSpan <= 0f) 0f else ((value - rangeStart) / rangeSpan).coerceIn(0f, 1f)

/**
 * Thumb centre, in px from the control's left edge.
 *
 * The centre travels the rail span exactly, so the thumb sits on the boundary
 * between filled and unfilled rail at every progress value — including 0 and 1,
 * where the inset keeps it from hanging off the end. Mirrored for RTL.
 */
internal fun liquidThumbCenterXPx(
    progress: Float,
    totalWidthPx: Float,
    thumbWidthPx: Float,
    isLtr: Boolean
): Float {
    val inset = liquidRailInsetPx(thumbWidthPx)
    val span = liquidRailSpanPx(totalWidthPx, thumbWidthPx)
    val p = progress.coerceIn(0f, 1f)
    return if (isLtr) inset + span * p else totalWidthPx - inset - span * p
}

/** `translationX` for a thumb whose layout box starts at the control's left edge. */
internal fun liquidThumbTranslationXPx(
    progress: Float,
    totalWidthPx: Float,
    thumbWidthPx: Float,
    isLtr: Boolean
): Float = liquidThumbCenterXPx(progress, totalWidthPx, thumbWidthPx, isLtr) - thumbWidthPx / 2f

/** Rail progress at an x position, mirroring [liquidThumbCenterXPx]. */
internal fun liquidProgressAtXPx(
    xPx: Float,
    totalWidthPx: Float,
    thumbWidthPx: Float,
    isLtr: Boolean
): Float {
    val inset = liquidRailInsetPx(thumbWidthPx)
    val span = liquidRailSpanPx(totalWidthPx, thumbWidthPx)
    if (span <= 0f) return 0f
    val fromStart = if (isLtr) xPx - inset else totalWidthPx - inset - xPx
    return (fromStart / span).coerceIn(0f, 1f)
}

/** The value a touch at [xPx] maps to — the inverse of [liquidThumbCenterXPx]. */
internal fun liquidValueAtXPx(
    xPx: Float,
    totalWidthPx: Float,
    thumbWidthPx: Float,
    rangeStart: Float,
    rangeSpan: Float,
    isLtr: Boolean
): Float =
    rangeStart + rangeSpan * liquidProgressAtXPx(xPx, totalWidthPx, thumbWidthPx, isLtr)

/**
 * Whether a drag in progress should start scrubbing.
 *
 * A slider inside the player sheet competes with the sheet's own vertical drag.
 * Arming only once the gesture is past touch slop *and* predominantly horizontal
 * means dragging the sheet up from the seek bar never seeks, while a horizontal
 * scrub still starts immediately.
 */
internal fun liquidScrubArmed(accumulatedX: Float, accumulatedY: Float, touchSlop: Float): Boolean =
    abs(accumulatedX) > touchSlop && abs(accumulatedX) > abs(accumulatedY)

/**
 * Whether a completed gesture was a tap, and so should seek to its press point.
 *
 * Requires the gesture never to have armed *and* to have stayed within slop on
 * both axes: a vertical sheet drag that started on the slider is neither a scrub
 * nor a tap, and must not move the playhead.
 */
internal fun liquidIsTap(
    armed: Boolean,
    accumulatedX: Float,
    accumulatedY: Float,
    touchSlop: Float
): Boolean =
    !armed && abs(accumulatedX) <= touchSlop && abs(accumulatedY) <= touchSlop

/**
 * Scales the catalog's fixed rest blur by the user's blur preference.
 *
 * At the shipped default (2dp) this returns 1.0, so the thumb blurs by exactly
 * the catalog's 8dp. Turning blur down to 0 removes it; turning it up caps at
 * 1.5x so a heavy preference cannot smear the thumb into its own rail.
 */
internal fun liquidThumbBlurScale(configuredBlurRadiusDp: Float): Float =
    (configuredBlurRadiusDp / LiquidGlassTokens.DefaultBlurRadiusDp).coerceIn(0f, 1.5f)

/** Lens height scale — see [liquidThumbBlurScale]. */
internal fun liquidThumbLensHeightScale(configuredLensHeight: Float): Float =
    (configuredLensHeight / LiquidGlassTokens.DefaultLensHeight).coerceIn(0f, 1.5f)

/** Lens amount scale — see [liquidThumbBlurScale]. */
internal fun liquidThumbLensAmountScale(configuredLensAmount: Float): Float =
    (configuredLensAmount / LiquidGlassTokens.DefaultLensAmount).coerceIn(0f, 1.5f)

/**
 * Non-uniform rest scale for a capsule thumb box, so it reads as a circle of
 * [restSize] while pressed geometry stays a [thumbSize] capsule.
 */
internal fun liquidThumbRestScaleX(thumbWidthPx: Float, restSizePx: Float): Float =
    if (thumbWidthPx <= 0f) 1f else (restSizePx / thumbWidthPx).coerceIn(0.05f, 1f)

internal fun liquidThumbRestScaleY(thumbHeightPx: Float, restSizePx: Float): Float =
    if (thumbHeightPx <= 0f) 1f else (restSizePx / thumbHeightPx).coerceIn(0.05f, 1f)

/**
 * Whether a surface should render as real liquid glass right now.
 *
 * One definition, because four things have to agree before a surface can refract
 * and every component was re-deriving them slightly differently: the user's
 * per-component switch, platform capability (API 31 + not low-RAM), the chosen
 * [GlassStyle], and whether anything is actually recorded behind the surface.
 *
 * Components use this to pick between their glass and flat renderers. Note that
 * [Modifier.liquidGlass] already degrades on its own — this exists so a component
 * can also change its *content* colour and skip gesture-only-glass work, which
 * requires knowing the answer before building the modifier chain.
 */
@Composable
fun rememberLiquidGlassActive(
    component: GlassComponent,
    config: GlassEffectConfig = LocalGlassEffectConfig.current,
    backdrop: Backdrop? = LocalAppBackdrop.current,
): Boolean =
    config.isEnabledFor(component) &&
            isGlassAllowed() &&
            !shouldUseTranslucentGlassFallback(config.style, isRenderEffectSupported()) &&
            backdrop.isLiveGlassBackdrop()

/**
 * The press transform every liquid glass control shares: a small growth plus a
 * lean toward the finger, damped so it saturates instead of tracking linearly.
 *
 * This is the reference implementation's own button physics (its `LiquidButton`
 * applies it inside `drawBackdrop`'s layer block). Two details are worth keeping:
 *
 *  - the lean goes through `tanh`, so a finger dragged to the edge of the button
 *    pushes the surface toward that edge and *stops* — a linear offset would slide
 *    the whole button off its own footprint;
 *  - the extra scale is anisotropic, derived from the drag angle and the button's
 *    aspect ratio, so a wide pill stretches horizontally under a horizontal drag
 *    and a circle stays a circle.
 *
 * Applied as an outer [androidx.compose.ui.graphics.graphicsLayer] rather than
 * through the backdrop's own layer block, because [Modifier.liquidGlass] does not
 * expose one — and because scaling the whole surface (glass, rim and icon
 * together) is what the layer block does anyway.
 *
 * Remember the result on the interaction: returning a fresh lambda per
 * recomposition would make the modifier unequal and re-run its node every frame.
 */
fun liquidGlassPressLayerBlock(
    interaction: InteractiveHighlight
): GraphicsLayerScope.() -> Unit = {
    val progress = interaction.pressProgress
    val offset = interaction.offset
    if (progress > 0f && size.height > 0f) {
        val growth = lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)
        val maxOffset = size.minDimension
        val initialDerivative = 0.05f
        translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
        translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)
        val maxDragScale = 4f.dp.toPx() / size.height
        val offsetAngle = atan2(offset.y, offset.x)
        val widthOverHeight = (size.width / size.height).fastCoerceAtMost(1f)
        val heightOverWidth = (size.height / size.width).fastCoerceAtMost(1f)
        scaleX = growth +
                maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) * widthOverHeight
        scaleY = growth +
                maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) * heightOverWidth
    }
}
