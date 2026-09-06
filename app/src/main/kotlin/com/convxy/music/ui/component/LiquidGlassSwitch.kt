/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convxy.music.ui.component

import androidx.compose.material3.SwitchColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.convxy.music.ui.component.backdrop.Backdrop
import com.convxy.music.ui.component.backdrop.catalog.components.LiquidToggle
import com.convxy.music.ui.component.backdrop.isRenderEffectSupported

/**
 * Whether a switch may render as the library's [LiquidToggle] instead of the
 * house [GlassSwitch]. Pulled out as a plain function because it is a decision
 * with several independent reasons to say no, and every one of them has bitten
 * before.
 *
 * - `!componentEnabled` — the user turned settings glass off.
 * - `!glassAllowed` — below Android 12, or a low-RAM device.
 * - `!enabled` — LiquidToggle has no disabled state; GlassSwitch does.
 * - `!hasHandler` — a null handler means the switch is a read-out and the row
 *   around it owns the click. LiquidToggle's thumb installs a drag detector that
 *   consumes the down event, so the row would stop being clickable while the
 *   thumb appeared to work.
 * - `hasThumbContent` / `hasColors` — LiquidToggle's thumb is a solid glass
 *   capsule with no content slot and iOS colours of its own (the same green
 *   [GlassSwitch] uses). Rather than half-honour a customization, keep the
 *   component that can honour all of it.
 * - `translucentFallback` — the user chose the cheap style, or the platform
 *   cannot do a real blur; a "glass" thumb with nothing behind it is a white dot.
 */
internal fun liquidSwitchEligible(
    componentEnabled: Boolean,
    glassAllowed: Boolean,
    enabled: Boolean,
    hasHandler: Boolean,
    hasThumbContent: Boolean,
    hasColors: Boolean,
    translucentFallback: Boolean,
): Boolean = componentEnabled && glassAllowed && enabled && hasHandler &&
        !hasThumbContent && !hasColors && !translucentFallback

/**
 * A settings switch rendered as the liquid glass toggle from the vendored
 * Android Liquid Glass catalog — the same `LiquidToggle` the library ships in its
 * demo app: a track recorded into its own layer, and a thumb that refracts it
 * through a combined backdrop, with blur trading down to lens as the press
 * deepens, a squash on velocity and an ambient rim that appears under the finger.
 *
 * It is safe to put anywhere, including deep inside the recorded app layer, which
 * is what makes it usable on settings screens at all. The outer backdrop is
 * resolved through [rememberOuterBackdropSampler]: where the surface sits inside
 * the node recording that backdrop — every settings screen, the search overlay —
 * it is dropped and the thumb refracts only its own track. The track is recorded
 * by a sibling, so that half is always legal.
 *
 * Falls back to [GlassSwitch] (which falls back to a plain iOS-shaped toggle when
 * glass is unavailable) for every case [liquidSwitchEligible] rejects.
 *
 * Cost, measured when this was first tried: roughly 5ms of display-list recording
 * per visible toggle, the largest per-frame cost found on any screen. It is
 * therefore behind [GlassComponent.SETTINGS_CONTROLS], on by default.
 */
@Composable
fun LiquidGlassSwitch(
    checked: Boolean,
    // Nullable for the same reason it is on [GlassSwitch]: null is a read-out, the
    // surrounding row owns the click.
    onCheckedChange: ((Boolean) -> Unit)?,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    colors: SwitchColors? = null,
    backdrop: Backdrop? = LocalAppBackdrop.current,
) {
    val config = LocalGlassEffectConfig.current
    val eligible = liquidSwitchEligible(
        componentEnabled = config.isEnabledFor(GlassComponent.SETTINGS_CONTROLS),
        glassAllowed = isGlassAllowed(),
        enabled = enabled,
        hasHandler = onCheckedChange != null,
        hasThumbContent = thumbContent != null,
        hasColors = colors != null,
        translucentFallback = shouldUseTranslucentGlassFallback(
            config.style,
            isRenderEffectSupported(),
        ),
    )

    if (!eligible) {
        GlassSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = modifier,
        )
        return
    }

    val outer = rememberOuterBackdropSampler(backdrop)
    LiquidToggle(
        selected = { checked },
        onSelect = { newValue -> onCheckedChange?.invoke(newValue) },
        backdrop = outer.effective,
        modifier = modifier.then(outer.measureModifier),
    )
}
