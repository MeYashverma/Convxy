package com.convx.music.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.convx.music.ui.component.LocalAppBackdrop
import com.convx.music.ui.component.backdrop.Backdrop

/** saturation floor shared with [AppleTokens.shiftedForContrast] (0.08f). */
internal fun Color.isHueless(): Boolean {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(toArgb(), hsl)
    return hsl[1] < 0.08f
}

/**
 * Wraps [content] in a nested [MaterialTheme] whose colour scheme is derived from
 * the screen's own hero [tint], so every `MaterialTheme.colorScheme` read on that
 * screen agrees with the background plane.
 *
 * When [tint] is hueless (grey / black / near-white), the app-wide scheme is kept
 * intact — re-theming from a grey produces a strictly worse result.
 *
 * Also provides [LocalAppBackdrop], [LocalContentColor] and [LocalAccentTextColor]
 * so glass chrome and explicit colour references stay in sync.
 */
@Composable
fun HeroTintedContent(
    tint: Color,
    backdrop: Backdrop,
    content: @Composable () -> Unit,
) {
    val base = MaterialTheme.colorScheme
    val onDark = tint.luminance() <= 0.5f
    val scheme = remember(base, tint, onDark) { base.accentText(tint, onDark) }

    if (tint.isHueless()) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalAppBackdrop provides backdrop,
            LocalContentColor provides AppleTokens.onColor(tint),
            LocalAccentTextColor provides AppleTokens.onColorHeading(tint),
        ) {
            content()
        }
    } else {
        MaterialTheme(
            colorScheme = scheme,
            typography = MaterialTheme.typography,
            shapes = MaterialTheme.shapes,
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalAppBackdrop provides backdrop,
                LocalContentColor provides AppleTokens.onColor(tint),
                LocalAccentTextColor provides AppleTokens.onColorHeading(tint),
            ) {
                content()
            }
        }
    }
}
