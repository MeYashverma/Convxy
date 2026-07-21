package com.music.vivi.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * Apple Music design tokens — the single source of truth for colors, shapes, and
 * adaptive contrast helpers. Every screen references these instead of hardcoding values.
 */
object AppleTokens {
    // Accent
    val AccentRed = Color(0xFFFA2D48)

    // Surfaces
    val Bg = Color(0xFF000000)
    val BgElevated = Color(0xFF0B0B0B)
    val Card = Color(0xFF1C1C1E)
    val CardSecondary = Color(0xFF2C2C2E)

    // Dividers
    val Divider = Color(0x1AFFFFFF)

    // Shapes
    val CardCorner = 22.dp
    val CardCornerLarge = 28.dp

    // Adaptive contrast helpers
    fun onColor(bg: Color): Color =
        if (bg.luminance() > 0.5f) Color(0xFF0A0A0A) else Color.White

    fun dividerOn(bg: Color): Color =
        if (bg.luminance() > 0.5f) Color(0x1A000000) else Divider
}
