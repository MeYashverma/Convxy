/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.convxy.music.ui.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.convxy.music.constants.SliderStyle
import com.convxy.music.constants.SliderStyleKey
import com.convxy.music.utils.rememberEnumPreference

/**
 * The app's slider: liquid glass when the user asked for it and this surface can
 * genuinely refract, the Material slider otherwise.
 *
 * Drop-in for [Slider] at the call shape settings rows use, so a row keeps its
 * value, range, steps and colours and only the rendering changes. Four gates have
 * to agree before it goes liquid, and they are the same four the chrome uses:
 *
 *  - the seek-bar style preference is [SliderStyle.LIQUID] — the other styles
 *    (default, wavy, slim, waveform) are looks the user picked and stay intact;
 *  - the per-component glass switch is on, [GlassComponent.SETTINGS] by default,
 *    which is the escape hatch for a screen that composes several of these at
 *    once: each surface costs a capture plus a RenderEffect chain every frame;
 *  - the platform can do it at all (API 31+, not a low-RAM device);
 *  - something is actually recorded behind it.
 *
 * The fallback is the Material slider rather than [LiquidGlassSlider]'s own
 * painted rail. That rail exists so the *player* seek bar stays recognisable when
 * glass is unavailable — same geometry, same springs, no refraction. A settings
 * row was designed around a Material slider, so that is what it gets back.
 *
 * Not for a [androidx.compose.ui.window.Dialog], ModalBottomSheet or Popup: those
 * compose into their own window, and the app backdrop is recorded in the app's.
 * A slider there inherits [LocalAppBackdrop] through the composition but has no
 * way to sample it, so call sites inside dialogs keep using [Slider] directly.
 *
 * @param colors used by the Material path only. The liquid path takes its colours
 *   from the glass config so it matches the rest of the chrome; pass
 *   [activeColor], [inactiveColor] and [thumbColor] to override that.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    steps: Int = 0,
    colors: SliderColors = SliderDefaults.colors(),
    component: GlassComponent = GlassComponent.SETTINGS,
    activeColor: Color = Color.Unspecified,
    inactiveColor: Color = Color.Unspecified,
    thumbColor: Color = Color.White,
) {
    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.LIQUID)
    val config = LocalGlassEffectConfig.current

    if (sliderStyle == SliderStyle.LIQUID &&
        config.isEnabledFor(component) &&
        isGlassAllowed() &&
        LocalAppBackdrop.current.isLiveGlassBackdrop()
    ) {
        LiquidGlassSlider(
            value = { value },
            onValueChange = onValueChange,
            modifier = modifier,
            valueRange = valueRange,
            onValueChangeFinished = onValueChangeFinished,
            enabled = enabled,
            steps = steps,
            activeColor = activeColor,
            inactiveColor = inactiveColor,
            thumbColor = thumbColor,
            config = config,
            component = component,
        )
    } else {
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            valueRange = valueRange,
            onValueChangeFinished = onValueChangeFinished,
            steps = steps,
            colors = colors,
        )
    }
}
