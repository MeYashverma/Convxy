/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.theme

import android.graphics.Bitmap
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score

val DefaultThemeColor = AppleTokens.AccentRed

/**
 * The user's chosen accent color, provided app-wide. ONLY the nav bar uses this
 * as an accent; the rest of the UI is contrast-based (white/black by luminance).
 * Kept separate from [ColorScheme.primary], which is neutralized to white in the
 * dark Apple theme so generic M3 controls don't render red.
 */
val LocalAccentColor = androidx.compose.runtime.compositionLocalOf { DefaultThemeColor }

@Composable
fun vivimusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = true,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    val baseColorScheme = rememberDynamicColorScheme(
        seedColor = themeColor,
        isDark = darkTheme,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
        style = PaletteStyle.TonalSpot,
    )

    val colorScheme = remember(baseColorScheme, pureBlack, darkTheme) {
        val withApple = if (darkTheme) baseColorScheme.appleSurfaces() else baseColorScheme
        if (darkTheme && pureBlack) {
            withApple.pureBlack(true)
        } else {
            withApple
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalAccentColor provides themeColor) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}

fun Bitmap.extractThemeColor(): Color {
    val colorsToPopulation = Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .swatches
        .associate { it.rgb to it.population }
    val rankedColors = Score.score(colorsToPopulation)
    return Color(rankedColors.first())
}

fun Bitmap.extractGradientColors(): List<Color> {
    val extractedColors = Palette.from(this)
        .maximumColorCount(64)
        .generate()
        .swatches
        .associate { it.rgb to it.population }

    val orderedColors = Score.score(extractedColors, 2, 0xff4285f4.toInt(), true)
        .sortedByDescending { Color(it).luminance() }

    return if (orderedColors.size >= 2)
        listOf(Color(orderedColors[0]), Color(orderedColors[1]))
    else
        listOf(Color(0xFF595959), Color(0xFF0D0D0D))
}

/**
 * Apple-dark surface overrides applied unconditionally in dark mode.
 * pureBlack is applied AFTER this and wins for surface/background when enabled.
 */
fun ColorScheme.appleSurfaces() = copy(
    background = AppleTokens.Bg,
    surface = AppleTokens.Bg,
    surfaceContainerLowest = AppleTokens.Bg,
    surfaceContainerLow = AppleTokens.BgElevated,
    surfaceContainer = AppleTokens.Card,
    surfaceContainerHigh = AppleTokens.Card,
    surfaceContainerHighest = AppleTokens.CardSecondary,
    surfaceVariant = AppleTokens.CardSecondary,
    // Neutralize the seed accent everywhere except the nav bar: generic M3
    // controls (switches, sliders, selected states) render white-on-dark, not
    // red. The nav bar reads the real accent via LocalAccentColor instead.
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = AppleTokens.CardSecondary,
    onPrimaryContainer = Color.White,
)

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
