/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import coil3.compose.AsyncImage
import com.music.vivi.constants.HomeBackgroundBlurKey
import com.music.vivi.constants.HomeBackgroundDimKey
import com.music.vivi.constants.HomeBackgroundEnabledKey
import com.music.vivi.constants.HomeBackgroundPathKey
import com.music.vivi.utils.rememberPreference
import java.io.File

/**
 * The user's custom home background image (blurred + dimmed), shared by the Home and
 * Library screens. Draws nothing when disabled or unset. Must be placed as a layer
 * behind the screen content inside a [BoxScope] (uses [matchParentSize]).
 *
 * @param withGradient adds the bottom primary-color wash on top of the image.
 */
@Composable
fun BoxScope.HomeImageBackground(withGradient: Boolean = false) {
    val (enabled) = rememberPreference(HomeBackgroundEnabledKey, false)
    val (path) = rememberPreference(HomeBackgroundPathKey, "")
    val (blur) = rememberPreference(HomeBackgroundBlurKey, 20f)
    val (dim) = rememberPreference(HomeBackgroundDimKey, 0.4f)
    if (!enabled || path.isEmpty()) return

    AsyncImage(
        model = File(path),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .matchParentSize()
            .blur(blur.dp),
    )
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(Color.Black.copy(alpha = dim)),
    )
    if (withGradient) {
        val primary = MaterialTheme.colorScheme.primary
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0.55f to Color.Transparent,
                        1f to primary.copy(alpha = 0.55f),
                    ),
                ),
        )
    }
}
