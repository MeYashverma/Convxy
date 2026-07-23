package com.music.vivi.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

/**
 * Apple Music design tokens — the single source of truth for colors, shapes, and
 * adaptive contrast helpers. Every screen references these instead of hardcoding values.
 */
object AppleTokens {
    // Accent
    val AccentRed = Color(0xFFFA2D48)

    // Surfaces — Apple-style soft dark greys, not pure black (pure black stays an
    // opt-in via the PureBlack setting for OLED). Monotonic elevation ladder.
    val Bg = Color(0xFF121212)
    val BgElevated = Color(0xFF1A1A1A)
    val Card = Color(0xFF1C1C1E)
    val CardSecondary = Color(0xFF2C2C2E)

    // Dividers
    val Divider = Color(0x1AFFFFFF)

    // Shapes
    val CardCorner = 22.dp
    val CardCornerLarge = 28.dp

    // Adaptive contrast helpers

    /**
     * Primary text/icon color for content sitting on [bg] — typically a screen's
     * own hero tint, extracted from its artwork.
     *
     * Keeps [bg]'s hue and pushes lightness to a readable extreme, rather than
     * dropping to flat white/near-black. The text then belongs to the screen's
     * color instead of being pasted on top of it. Saturation is capped low: this
     * is body copy, so it should read as white/black that happens to be warm or
     * cool, not as colored text.
     */
    fun onColor(bg: Color): Color = bg.shiftedForContrast(saturation = 0.16f, lightness = 0.96f to 0.10f)

    /**
     * Secondary text on [bg]: same hue, but the tint is allowed to show. Use for
     * subtitles, counts and metadata — the role that reads as washed-out grey
     * when it comes from Material's neutral palette.
     */
    fun onColorSecondary(bg: Color): Color =
        bg.shiftedForContrast(saturation = 0.42f, lightness = 0.76f to 0.34f)

    /**
     * @param lightness (on-dark, on-light) target lightness.
     */
    private fun Color.shiftedForContrast(saturation: Float, lightness: Pair<Float, Float>): Color {
        val hsl = FloatArray(3)
        androidx.core.graphics.ColorUtils.colorToHSL(toArgb(), hsl)
        val onDark = luminance() <= 0.5f
        // A near-grey tint has no meaningful hue to carry, so don't invent one.
        hsl[1] = if (hsl[1] < 0.08f) 0f else minOf(hsl[1], saturation)
        hsl[2] = if (onDark) lightness.first else lightness.second
        return Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
    }

    fun dividerOn(bg: Color): Color =
        if (bg.luminance() > 0.5f) Color(0x1A000000) else Divider
}
