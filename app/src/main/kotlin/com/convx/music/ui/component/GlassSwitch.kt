/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * iOS-style toggle switch with liquid glass track when glass is enabled.
 * Falls back to solid green/grey track on unsupported devices.
 */
@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val glassConfig = LocalGlassEffectConfig.current
    val useGlass = glassConfig.globalEnabled && isGlassAllowed()

    val thumbProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(200),
        label = "glassSwitchThumb",
    )

    val trackShape = RoundedCornerShape(16.dp)
    val trackWidth = 51.dp
    val trackHeight = 31.dp
    val thumbSize = 27.dp
    val thumbPadding = 2.dp
    val maxTravel = trackWidth - thumbSize - thumbPadding * 2

    Box(
        modifier = modifier
            .width(trackWidth)
            .height(trackHeight)
            .clip(trackShape)
            .then(
                if (useGlass && checked && enabled) {
                    Modifier.liquidGlass(
                        config = glassConfig,
                        shape = trackShape,
                        applyEdgeEffects = true,
                        blurRadiusDp = 4f,
                    )
                } else {
                    Modifier.background(
                        when {
                            !enabled -> Color(0xFF39393D).copy(alpha = 0.4f)
                            checked -> Color(0xFF34C759)
                            else -> Color(0xFF39393D)
                        }
                    )
                }
            )
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbPadding + (maxTravel * thumbProgress))
                .size(thumbSize)
                .shadow(3.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}
