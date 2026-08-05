/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.component

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.convx.music.constants.HomeBackgroundAnimateKey
import com.convx.music.constants.HomeBackgroundBlurKey
import com.convx.music.constants.HomeBackgroundDimKey
import com.convx.music.constants.HomeBackgroundEnabledKey
import com.convx.music.constants.HomeBackgroundIsVideoKey
import com.convx.music.constants.HomeBackgroundPathKey
import com.convx.music.utils.rememberPreference
import java.io.File

/** Process-wide: the intro blur ramp plays only the first time this session. */
private var blurAnimatedThisSession = false

/**
 * True when the user has a custom background image set and enabled.
 *
 * Screens that draw their own backdrop (blurred album art, theme wash) must check
 * this and suppress those layers — otherwise they render underneath the custom
 * image along with their scrims, and the user's picture comes out looking nothing
 * like it does on a screen that has no backdrop of its own.
 */
@Composable
fun hasCustomHomeBackground(): Boolean {
    val (enabled) = rememberPreference(HomeBackgroundEnabledKey, false)
    val (path) = rememberPreference(HomeBackgroundPathKey, "")
    return enabled && path.isNotEmpty()
}

/**
 * The user's custom home background image (blurred + dimmed), shared by the Home and
 * Library screens. Draws nothing when disabled or unset. Must be placed as a layer
 * behind the screen content inside a [BoxScope] (uses [matchParentSize]).
 *
 * Blur always runs via realtime Modifier.blur. A prior version tried to bake the
 * blur into the decoded bitmap via a Coil Transformation (downscale/upscale) to
 * avoid the per-frame cost — confirmed on-device to render unblurred regardless
 * of algorithm/cache-key/hardware-bitmap fixes, while this path (proven by the
 * "animate" mode using the same Modifier.blur) reliably works. Correctness over
 * the optimization.
 *
 * @param withGradient adds the bottom primary-color wash on top of the image.
 * @param contentLoaded when animate is on, the blur eases in once this flips true
 *   (i.e. when the screen's content items appear), not when the image itself loads.
 */
@Composable
fun BoxScope.HomeImageBackground(
    withGradient: Boolean = false,
    contentLoaded: Boolean = true,
) {
    val (enabled) = rememberPreference(HomeBackgroundEnabledKey, false)
    val (path) = rememberPreference(HomeBackgroundPathKey, "")
    val (blur) = rememberPreference(HomeBackgroundBlurKey, 20f)
    val (dim) = rememberPreference(HomeBackgroundDimKey, 0.4f)
    val (animate) = rememberPreference(HomeBackgroundAnimateKey, false)
    val (isVideo) = rememberPreference(HomeBackgroundIsVideoKey, false)
    if (!enabled || path.isEmpty()) return

    if (isVideo) {
        // No blur-in animation for video — the loop is already motion, ramping
        // blur on top of it double-animates for no benefit.
        HomeVideoBackground(path = path, blur = blur, dim = dim, withGradient = withGradient)
        return
    }

    // The intro blur ramp plays once per app session, not on every navigation to a screen
    // with this background. `appeared` starts already-true when it has run before, so the
    // animateFloatAsState inits straight at the blur target (static, no re-animation).
    val shouldAnimate = animate && !blurAnimatedThisSession
    var appeared by remember { mutableStateOf(!shouldAnimate) }
    LaunchedEffect(shouldAnimate) {
        if (shouldAnimate) {
            appeared = true
            blurAnimatedThisSession = true
        }
    }
    val animatedBlur by animateFloatAsState(
        targetValue = if (appeared && contentLoaded) blur else 0f,
        // Long, gentle ease-out (easeOutExpo-like) for a fluid, unhurried settle.
        animationSpec = tween(
            durationMillis = 2200,
            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f),
        ),
        label = "homeBgBlur",
    )
    val effectiveBlur = if (animate) animatedBlur else blur
    val context = LocalContext.current
    val imageRequest = remember(path) {
        ImageRequest.Builder(context)
            .data(File(path))
            .size(1080, 1920)
            .crossfade(false)
            .build()
    }

    AsyncImage(
        model = imageRequest,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .matchParentSize()
            .blur(effectiveBlur.dp),
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
