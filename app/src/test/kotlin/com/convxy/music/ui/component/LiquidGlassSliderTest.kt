package com.convxy.music.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the pure geometry and gesture-decision maths behind
 * [LiquidGlassSlider].
 *
 * These are the parts that cannot be checked by eye on a CI machine but that
 * break the control completely when they are wrong: the thumb landing somewhere
 * other than the fill boundary, RTL scrubbing moving the playhead backwards, a
 * zero-duration track producing NaN, and a vertical sheet drag seeking.
 */
class LiquidGlassSliderTest {

    private val totalWidth = 1000f
    private val thumbWidth = 40f

    // A quarter of the thumb, matching the reference implementation's clamp.
    private val inset = 10f
    private val span = 980f

    @Test
    fun `rail inset is a quarter of the thumb width`() {
        assertEquals(10f, liquidRailInsetPx(40f), 0.0001f)
        assertEquals(6f, liquidRailInsetPx(24f), 0.0001f)
    }

    @Test
    fun `rail span removes both insets`() {
        assertEquals(span, liquidRailSpanPx(totalWidth, thumbWidth), 0.0001f)
    }

    @Test
    fun `rail span never goes negative on a control narrower than its thumb`() {
        assertEquals(0f, liquidRailSpanPx(12f, thumbWidth), 0.0001f)
        assertEquals(0f, liquidRailSpanPx(0f, thumbWidth), 0.0001f)
    }

    @Test
    fun `thumb centre sits on the rail start and end at the extremes`() {
        assertEquals(inset, liquidThumbCenterXPx(0f, totalWidth, thumbWidth, isLtr = true), 0.0001f)
        assertEquals(
            totalWidth - inset,
            liquidThumbCenterXPx(1f, totalWidth, thumbWidth, isLtr = true),
            0.0001f
        )
    }

    @Test
    fun `thumb centre tracks progress linearly`() {
        assertEquals(inset + span * 0.5f, liquidThumbCenterXPx(0.5f, totalWidth, thumbWidth, true), 0.0001f)
        assertEquals(inset + span * 0.25f, liquidThumbCenterXPx(0.25f, totalWidth, thumbWidth, true), 0.0001f)
    }

    @Test
    fun `thumb centre is clamped to the rail for out of range progress`() {
        assertEquals(inset, liquidThumbCenterXPx(-3f, totalWidth, thumbWidth, true), 0.0001f)
        assertEquals(totalWidth - inset, liquidThumbCenterXPx(9f, totalWidth, thumbWidth, true), 0.0001f)
    }

    @Test
    fun `thumb centre mirrors in rtl`() {
        val ltr = liquidThumbCenterXPx(0.3f, totalWidth, thumbWidth, isLtr = true)
        val rtl = liquidThumbCenterXPx(0.3f, totalWidth, thumbWidth, isLtr = false)
        assertEquals(totalWidth, ltr + rtl, 0.0001f)
    }

    @Test
    fun `translation x centres the thumb box on the rail position`() {
        assertEquals(
            liquidThumbCenterXPx(0.4f, totalWidth, thumbWidth, true) - thumbWidth / 2f,
            liquidThumbTranslationXPx(0.4f, totalWidth, thumbWidth, true),
            0.0001f
        )
    }

    @Test
    fun `value at x round trips against thumb centre`() {
        for (progress in floatArrayOf(0f, 0.1f, 0.37f, 0.5f, 0.83f, 1f)) {
            val x = liquidThumbCenterXPx(progress, totalWidth, thumbWidth, true)
            assertEquals(
                "ltr progress $progress",
                progress,
                liquidProgressAtXPx(x, totalWidth, thumbWidth, true),
                0.0001f
            )
            val xRtl = liquidThumbCenterXPx(progress, totalWidth, thumbWidth, false)
            assertEquals(
                "rtl progress $progress",
                progress,
                liquidProgressAtXPx(xRtl, totalWidth, thumbWidth, false),
                0.0001f
            )
        }
    }

    @Test
    fun `touching outside the rail clamps to the nearest end`() {
        assertEquals(0f, liquidProgressAtXPx(-50f, totalWidth, thumbWidth, true), 0.0001f)
        assertEquals(1f, liquidProgressAtXPx(totalWidth + 50f, totalWidth, thumbWidth, true), 0.0001f)
    }

    @Test
    fun `value at x maps into the caller range, not 0 to 1`() {
        // A 3 minute track in milliseconds.
        val v = liquidValueAtXPx(
            xPx = inset + span * 0.5f,
            totalWidthPx = totalWidth,
            thumbWidthPx = thumbWidth,
            rangeStart = 0f,
            rangeSpan = 180_000f,
            isLtr = true
        )
        assertEquals(90_000f, v, 1f)
    }

    @Test
    fun `value at x honours a non zero range start`() {
        val v = liquidValueAtXPx(
            xPx = inset,
            totalWidthPx = totalWidth,
            thumbWidthPx = thumbWidth,
            rangeStart = 1000f,
            rangeSpan = 5000f,
            isLtr = true
        )
        assertEquals(1000f, v, 0.0001f)
    }

    @Test
    fun `rtl scrubbing moves the value the other way`() {
        val left = liquidValueAtXPx(inset, totalWidth, thumbWidth, 0f, 100f, isLtr = false)
        val right = liquidValueAtXPx(totalWidth - inset, totalWidth, thumbWidth, 0f, 100f, isLtr = false)
        assertEquals(100f, left, 0.0001f)
        assertEquals(0f, right, 0.0001f)
    }

    @Test
    fun `progress is guarded against a zero range`() {
        assertEquals(0f, liquidSliderProgress(0f, 0f, 0f), 0.0001f)
        assertEquals(0f, liquidSliderProgress(1234f, 0f, 0f), 0.0001f)
    }

    @Test
    fun `progress is guarded against a zero width control`() {
        assertEquals(
            0f,
            liquidProgressAtXPx(0f, 0f, thumbWidth, true),
            0.0001f
        )
    }

    @Test
    fun `scrub arms only past slop and only when horizontal wins`() {
        val slop = 20f
        assertFalse(liquidScrubArmed(10f, 0f, slop))
        assertFalse(liquidScrubArmed(20f, 0f, slop))
        assertTrue(liquidScrubArmed(21f, 0f, slop))
        assertTrue(liquidScrubArmed(21f, 15f, slop))
        // The player sheet's own vertical drag: well past slop, but vertical.
        assertFalse(liquidScrubArmed(15f, 300f, slop))
        assertFalse(liquidScrubArmed(25f, 26f, slop))
    }

    @Test
    fun `a gesture counts as a tap only when it never armed and never moved`() {
        val slop = 20f
        assertTrue(liquidIsTap(armed = false, 0f, 0f, slop))
        assertTrue(liquidIsTap(armed = false, 5f, -8f, slop))
        // Armed means it scrubbed, so the value already moved: not a tap.
        assertFalse(liquidIsTap(armed = true, 0f, 0f, slop))
        // A vertical sheet drag must not be mistaken for a tap and seek.
        assertFalse(liquidIsTap(armed = false, 4f, 300f, slop))
        assertFalse(liquidIsTap(armed = false, 300f, 4f, slop))
    }

    @Test
    fun `blur scale is one at the shipped preference default`() {
        assertEquals(1f, liquidThumbBlurScale(LiquidGlassTokens.DefaultBlurRadiusDp), 0.0001f)
    }

    @Test
    fun `blur scale is zero when the user turns blur off and caps at one and a half`() {
        assertEquals(0f, liquidThumbBlurScale(0f), 0.0001f)
        assertEquals(1.5f, liquidThumbBlurScale(100f), 0.0001f)
        assertEquals(0.5f, liquidThumbBlurScale(1f), 0.0001f)
    }

    @Test
    fun `lens scales are one at the shipped preference defaults`() {
        assertEquals(1f, liquidThumbLensHeightScale(LiquidGlassTokens.DefaultLensHeight), 0.0001f)
        assertEquals(1f, liquidThumbLensAmountScale(LiquidGlassTokens.DefaultLensAmount), 0.0001f)
    }

    @Test
    fun `lens scales clamp instead of inverting at zero`() {
        assertEquals(0f, liquidThumbLensAmountScale(0f), 0.0001f)
        assertEquals(1.5f, liquidThumbLensAmountScale(5f), 0.0001f)
    }

    @Test
    fun `rest scale turns the capsule thumb box into a square dot`() {
        // 40x24dp thumb box, 12dp rest dot: 0.3 across, 0.5 down.
        assertEquals(0.3f, liquidThumbRestScaleX(40f, 12f), 0.0001f)
        assertEquals(0.5f, liquidThumbRestScaleY(24f, 12f), 0.0001f)
    }

    @Test
    fun `rest scale is clamped to something visible and never inverted`() {
        assertEquals(0.05f, liquidThumbRestScaleX(40f, 0f), 0.0001f)
        assertEquals(1f, liquidThumbRestScaleX(40f, 90f), 0.0001f)
        // A zero-width box (never laid out yet) must not divide by zero.
        assertEquals(1f, liquidThumbRestScaleX(0f, 12f), 0.0001f)
        assertEquals(1f, liquidThumbRestScaleY(0f, 12f), 0.0001f)
    }

    @Test
    fun `an empty backdrop is not a live glass backdrop`() {
        assertFalse(
            com.convxy.music.ui.component.backdrop.backdrops.emptyBackdrop()
                .isLiveGlassBackdrop()
        )
        assertFalse((null as com.convxy.music.ui.component.backdrop.Backdrop?).isLiveGlassBackdrop())
    }
}
