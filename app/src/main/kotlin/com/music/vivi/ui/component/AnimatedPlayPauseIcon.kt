package com.music.vivi.ui.component

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.music.vivi.R

@Composable
fun AnimatedPlayPauseIcon(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    size: Dp = 32.dp,
) {
    val blurAnim = remember { Animatable(0f) }

    LaunchedEffect(isPlaying) {
        blurAnim.snapTo(12f)
        blurAnim.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        )
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = isPlaying,
            transitionSpec = {
                (fadeIn(tween(250)) togetherWith fadeOut(tween(150)))
                    .using(SizeTransform(clip = false))
            },
            contentKey = { it },
            label = "play_pause",
        ) { playing ->
            val res = if (playing) R.drawable.pause else R.drawable.play
            val blur = blurAnim.value

            Icon(
                painter = painterResource(res),
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(size)
                    .graphicsLayer {
                        scaleX = 1f - blur * 0.008f
                        scaleY = 1f - blur * 0.008f
                        alpha = 1f - blur * 0.04f
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blur > 0.1f) {
                            renderEffect = android.graphics.RenderEffect
                                .createBlurEffect(blur, blur, android.graphics.Shader.TileMode.CLAMP)
                                .asComposeRenderEffect()
                        } else if (blur <= 0.1f) {
                            renderEffect = null
                        }
                    },
            )
        }
    }
}
