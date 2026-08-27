/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.player

import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import com.convx.music.LocalPlayerConnection
import timber.log.Timber

/**
 * Renders the video track of the shared music player (full YouTube video
 * mode). Video only starts rendering once the current stream actually carries
 * a video track (muxed format selected by the resolver when video mode is on).
 *
 * Two surface backends:
 * - [TextureView] (default): draws inside the window, so it follows Compose
 *   transforms — required by [CanvasArtworkPlayer]/Thumbnail, which scale and
 *   clip the artwork during player morphs.
 * - [SurfaceView] (`useSurfaceView = true`): media3's default backend, pushed
 *   by the compositor rather than drawn into the window. Slower devices are
 *   far less likely to die natively in the video decoder with it, which is
 *   why the native YouTube watch screen opts in. It must only be used in
 *   static layouts — a SurfaceView does not reliably follow view transforms,
 *   so it is wrong for the morphing player artwork.
 *
 * Both backends sit in an [AspectRatioFrameLayout] (FIT) whose aspect ratio
 * tracks the player's [VideoSize], so fullscreen letterboxes correctly and a
 * fixed 16:9 container gets an undistorted picture.
 */
@UnstableApi
@Composable
fun VideoPlaybackSurface(
    modifier: Modifier = Modifier,
    useSurfaceView: Boolean = false,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val player = playerConnection.player
    val attachedView = remember { mutableStateOf<View?>(null) }
    val aspectFrame = remember { mutableStateOf<AspectRatioFrameLayout?>(null) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                val ratio = if (videoSize.height == 0) {
                    16f / 9f
                } else {
                    videoSize.width.toFloat() * videoSize.pixelWidthHeightRatio / videoSize.height
                }
                aspectFrame.value?.setAspectRatio(ratio)
            }

            override fun onRenderedFirstFrame() {
                Timber.tag("YouTubeVideo").i("first frame rendered (surfaceView=$useSurfaceView)")
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            attachedView.value?.let { view ->
                when (view) {
                    is TextureView -> player.clearVideoTextureView(view)
                    is SurfaceView -> player.clearVideoSurfaceView(view)
                }
            }
            attachedView.value = null
            aspectFrame.value = null
        }
    }

    if (useSurfaceView) {
        AndroidView(
            factory = { viewContext ->
                AspectRatioFrameLayout(viewContext).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setAspectRatio(16f / 9f)
                    aspectFrame.value = this

                    val surfaceView = SurfaceView(viewContext).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    }
                    addView(surfaceView)
                    player.setVideoSurfaceView(surfaceView)
                    attachedView.value = surfaceView
                    Timber.tag("YouTubeVideo").i("SurfaceView attached to player")
                }
            },
            modifier = modifier,
        )
    } else {
        AndroidView(
            factory = { viewContext ->
                AspectRatioFrameLayout(viewContext).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    aspectFrame.value = this

                    val textureView = TextureView(viewContext).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    }
                    addView(textureView)
                    player.setVideoTextureView(textureView)
                    attachedView.value = textureView
                }
            },
            modifier = modifier,
        )
    }
}
