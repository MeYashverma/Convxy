package com.convxy.music.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the press physics behind [LiquidGlassIconButton],
 * [LiquidGlassButton] and therefore [GlassCircleButton].
 *
 * A button that grows the wrong way is not a crash and not a failed test run — it
 * just feels broken, and it is invisible on a CI machine. These pin the parts that
 * decide how a press reads: how far a surface swells, whether the lean toward the
 * finger saturates instead of sliding the button off its own footprint, and which
 * axis a drag stretches.
 */
class LiquidGlassButtonTest {

    // 4dp of growth at 2.75x density, the value the layer block computes.
    private val growthPx = 11f

    // A 44dp circle button at the same density: 121x121px.
    private val circleSize = 121

    // A pill: 120dp x 44dp.
    private val pillWidth = 330
    private val pillHeight = 121

    // region growth

    @Test
    fun `an unpressed surface is exactly its own size`() {
        assertEquals(1f, liquidPressGrowth(growthPx, circleSize, 0f), 0.0001f)
    }

    @Test
    fun `a full press adds the growth length to the surface height`() {
        // 11px of growth on a 121px-tall surface is a scale of 1 + 11/121.
        assertEquals(1f + growthPx / circleSize, liquidPressGrowth(growthPx, circleSize, 1f), 0.0001f)
    }

    @Test
    fun `growth is the same visual amount on a chip and on a play button`() {
        // 4dp on a 24dp chip and 4dp on a 100dp button are both "one step bigger",
        // which is what expressing the growth against the surface's own height buys.
        val chip = liquidPressGrowth(4f, 24, 1f)
        val button = liquidPressGrowth(4f, 100, 1f)
        assertTrue(chip > button)
        assertEquals(1f + 4f / 24f, chip, 0.0001f)
        assertEquals(1f + 4f / 100f, button, 0.0001f)
    }

    @Test
    fun `growth is linear in press progress`() {
        val half = liquidPressGrowth(growthPx, circleSize, 0.5f)
        val full = liquidPressGrowth(growthPx, circleSize, 1f)
        assertEquals(1f + (full - 1f) / 2f, half, 0.0001f)
    }

    @Test
    fun `growth never shrinks the surface`() {
        for (progress in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
            assertTrue(liquidPressGrowth(growthPx, circleSize, progress) >= 1f)
        }
    }

    @Test
    fun `a surface that has not been measured yet does not divide by zero`() {
        assertEquals(1f, liquidPressGrowth(growthPx, 0, 1f), 0.0001f)
    }

    // endregion

    // region lean toward the finger

    @Test
    fun `a finger in the centre does not move the surface`() {
        assertEquals(0f, liquidPressTranslationPx(0f, circleSize), 0.0001f)
    }

    @Test
    fun `the lean tracks the finger almost one to one near the centre`() {
        // Derivative 0.05 at the origin, so a 10px offset leans about 0.5px.
        assertEquals(0.5f, liquidPressTranslationPx(10f, circleSize), 0.01f)
    }

    @Test
    fun `the lean keeps the direction of the finger`() {
        assertTrue(liquidPressTranslationPx(-40f, circleSize) < 0f)
        assertTrue(liquidPressTranslationPx(40f, circleSize) > 0f)
    }

    @Test
    fun `the lean never carries the surface past its own footprint`() {
        // A linear offset would move a 121px button by 121px when the finger is at
        // its edge — entirely off itself — and by 2420px when the finger wanders
        // across the screen. tanh keeps the lean strictly inside the surface.
        for (offset in listOf(20f, 121f, 2420f, 24200f)) {
            val lean = liquidPressTranslationPx(offset, circleSize)
            assertTrue(lean < circleSize)
        }
    }

    @Test
    fun `the lean buys less per pixel the further the finger travels`() {
        val atEdge = liquidPressTranslationPx(circleSize.toFloat(), circleSize)
        val far = liquidPressTranslationPx(circleSize * 20f, circleSize)
        val farther = liquidPressTranslationPx(circleSize * 40f, circleSize)
        assertTrue(far - atEdge > 0f)
        assertTrue(farther - far < far - atEdge)
    }

    @Test
    fun `the lean is monotonic in finger travel`() {
        var previous = Float.NEGATIVE_INFINITY
        for (offset in listOf(-60f, -20f, 0f, 20f, 60f, 200f)) {
            val current = liquidPressTranslationPx(offset, circleSize)
            assertTrue(current > previous)
            previous = current
        }
    }

    @Test
    fun `a degenerate surface does not divide by zero when leaning`() {
        assertEquals(0f, liquidPressTranslationPx(30f, 0), 0.0001f)
    }

    // endregion

    // region which axis a drag stretches

    @Test
    fun `a square stretches along both axes`() {
        assertEquals(1f, liquidPressAnisotropy(circleSize, circleSize, horizontal = true), 0.0001f)
        assertEquals(1f, liquidPressAnisotropy(circleSize, circleSize, horizontal = false), 0.0001f)
    }

    @Test
    fun `a pill stretches only along its length`() {
        assertEquals(1f, liquidPressAnisotropy(pillWidth, pillHeight, horizontal = true), 0.0001f)
        assertEquals(0f, liquidPressAnisotropy(pillWidth, pillHeight, horizontal = false), 0.0001f)
    }

    @Test
    fun `a tall surface stretches only vertically`() {
        assertEquals(0f, liquidPressAnisotropy(pillHeight, pillWidth, horizontal = true), 0.0001f)
        assertEquals(1f, liquidPressAnisotropy(pillHeight, pillWidth, horizontal = false), 0.0001f)
    }

    @Test
    fun `a zero sized surface stretches nowhere rather than throwing`() {
        assertEquals(0f, liquidPressAnisotropy(0, circleSize, horizontal = true), 0.0001f)
        assertEquals(0f, liquidPressAnisotropy(circleSize, 0, horizontal = false), 0.0001f)
    }

    @Test
    fun `dragging a pill across stretches it sideways and not up`() {
        // A drag exactly along +x: angle 0, so cos is 1 and sin is 0.
        val sideways = liquidPressStretch(
            growthPx = growthPx,
            heightPx = pillHeight,
            axisUnit = 1f,
            offsetPx = 40f,
            maxDimensionPx = pillWidth,
            anisotropy = liquidPressAnisotropy(pillWidth, pillHeight, horizontal = true),
        )
        val up = liquidPressStretch(
            growthPx = growthPx,
            heightPx = pillHeight,
            axisUnit = 0f,
            offsetPx = 40f,
            maxDimensionPx = pillWidth,
            anisotropy = liquidPressAnisotropy(pillWidth, pillHeight, horizontal = false),
        )
        assertTrue(sideways > 0f)
        assertEquals(0f, up, 0.0001f)
    }

    @Test
    fun `dragging a circle at 45 degrees splits the stretch evenly`() {
        val unit = 0.70710678f
        val x = liquidPressStretch(growthPx, circleSize, unit, 40f, circleSize, liquidPressAnisotropy(circleSize, circleSize, true))
        val y = liquidPressStretch(growthPx, circleSize, unit, 40f, circleSize, liquidPressAnisotropy(circleSize, circleSize, false))
        assertEquals(x, y, 0.0001f)
        assertTrue(x > 0f)
    }

    @Test
    fun `dragging one full surface width adds as much scale as the growth itself`() {
        // The stretch is calibrated against the growth: at an offset equal to the
        // surface's own extent the two are the same size, so a normal press-with-
        // drag reads as "a bit more than a press", not as a different animation.
        val stretch = liquidPressStretch(
            growthPx = growthPx,
            heightPx = circleSize,
            axisUnit = 1f,
            offsetPx = circleSize.toFloat(),
            maxDimensionPx = circleSize,
            anisotropy = 1f,
        )
        assertEquals(growthPx / circleSize, stretch, 0.0001f)
        assertEquals(liquidPressGrowth(growthPx, circleSize, 1f) - 1f, stretch, 0.0001f)
    }

    @Test
    fun `the drag stretch is proportional to finger travel`() {
        // InteractiveHighlight's offset is the raw finger travel from the press
        // point, unclamped — this is the reference implementation's behaviour, so
        // the stretch keeps scaling as the finger leaves the button.
        fun stretchAt(offset: Float) = liquidPressStretch(
            growthPx = growthPx,
            heightPx = circleSize,
            axisUnit = 1f,
            offsetPx = offset,
            maxDimensionPx = circleSize,
            anisotropy = 1f,
        )
        assertEquals(stretchAt(30f) * 2f, stretchAt(60f), 0.0001f)
        assertEquals(stretchAt(-30f), stretchAt(30f), 0.0001f)
    }

    @Test
    fun `no drag means no stretch`() {
        assertEquals(
            0f,
            liquidPressStretch(growthPx, circleSize, 1f, 0f, circleSize, 1f),
            0.0001f,
        )
    }

    @Test
    fun `stretch guards a degenerate surface`() {
        assertEquals(0f, liquidPressStretch(growthPx, 0, 1f, 40f, circleSize, 1f), 0.0001f)
        assertEquals(0f, liquidPressStretch(growthPx, circleSize, 1f, 40f, 0, 1f), 0.0001f)
    }

    // endregion
}
