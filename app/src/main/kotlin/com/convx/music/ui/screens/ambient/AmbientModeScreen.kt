/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.screens.ambient

import android.app.Activity
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.convx.music.LocalPlayerConnection
import com.convx.music.R
import com.convx.music.models.MediaMetadata
import com.convx.music.playback.PlayerConnection
import com.convx.music.ui.component.AnimatedPlayPauseIcon
import com.convx.music.ui.player.InlineLyricsView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun AmbientModeScreen(navController: NavController) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val density = LocalDensity.current
    val audioManager = remember {
        context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
    }
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        val window = activity?.window
        var windowInsetsController: WindowInsetsControllerCompat? = null
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            activity?.requestedOrientation = originalOrientation
            if (window != null) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                windowInsetsController?.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler {
        navController.popBackStack()
    }

    // Positive means a rightward finger movement: the previous track enters from the
    // left. Negative means a leftward movement: the next track enters from the right.
    var transitionDirection by remember { mutableIntStateOf(1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var ringSeekPreview by remember { mutableStateOf<Float?>(null) }
    var showPlaybackFeedback by remember { mutableStateOf(false) }
    var feedbackIsPlaying by remember { mutableStateOf(false) }
    val latestIsPlayingState = rememberUpdatedState(isPlaying)
    var feedbackResetJob by remember { mutableStateOf<Job?>(null) }

    fun toggleWithFeedback() {
        feedbackIsPlaying = !latestIsPlayingState.value
        showPlaybackFeedback = true
        feedbackResetJob?.cancel()
        feedbackResetJob = coroutineScope.launch {
            delay(650L)
            showPlaybackFeedback = false
        }
    }

    val ringHitSlop = with(density) { 36.dp.toPx() }
    val ringInset = with(density) { 2.dp.toPx() }
    val ringCornerRadius = with(density) { 24.dp.toPx() }
    val touchSlop = with(density) { 18.dp.toPx() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Keep the existing ambient glow in one continuously animated layer. The
        // foreground scene slides over it, while the glow itself keeps its existing
        // palette cross-fade and does not flash to a blank background between songs.
        AmbientGlowBackground(
            mediaMetadata = mediaMetadata,
            modifier = Modifier.fillMaxSize()
        )

        // This is deliberately drawn before the content. It is a bezel detail, not a
        // player control, and the touch handling below only claims its narrow edge
        // hit area when the player reports a seekable duration.
        AmbientProgressRing(
            playerConnection = playerConnection,
            mediaId = mediaMetadata?.id,
            isPlaying = isPlaying,
            seekPreviewProgress = ringSeekPreview,
            modifier = Modifier.fillMaxSize()
        )

        val animatedDragOffset by androidx.compose.animation.core.animateFloatAsState(
            targetValue = dragOffset,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = 1100f
            ),
            label = "ambientDragOffset"
        )

        // A single gesture arena prevents a tap, a horizontal skip, a volume change,
        // and a ring seek from all reacting to the same pointer sequence.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(playerConnection, ringHitSlop, ringInset, ringCornerRadius, touchSlop) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val pointerId = down.id
                        val downPosition = down.position
                        val gestureSize = androidx.compose.ui.geometry.Size(
                            width = size.width.toFloat(),
                            height = size.height.toFloat()
                        )
                        val ringDuration = if (isNearBezel(
                                downPosition,
                                gestureSize,
                                ringHitSlop
                            )
                        ) {
                            runCatching {
                                val player = playerConnection.player
                                if (player.isCommandAvailable(
                                        androidx.media3.common.Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM
                                    )
                                ) {
                                    player.duration
                                } else {
                                    -1L
                                }
                            }.getOrDefault(-1L)
                        } else {
                            -1L
                        }
                        val isRingGesture = ringDuration > 0L
                        var totalX = 0f
                        var totalY = 0f
                        var verticalAccumulator = 0f
                        var axis: GestureAxis? = null
                        var lastRingFraction = -1f

                        fun seekFromRing(position: androidx.compose.ui.geometry.Offset, force: Boolean = false) {
                            if (!isRingGesture) return
                            val fraction = bezelProgressFor(
                                position = position,
                                size = gestureSize,
                                inset = ringInset,
                                cornerRadius = ringCornerRadius
                            )
                            ringSeekPreview = fraction
                            // Avoid flooding Media3 with effectively identical seek
                            // commands while still making the ring feel immediate.
                            if (force || lastRingFraction < 0f || abs(fraction - lastRingFraction) >= 0.003f) {
                                playerConnection.seekTo((ringDuration * fraction).toLong())
                                lastRingFraction = fraction
                            }
                        }

                        if (isRingGesture) {
                            down.consume()
                            seekFromRing(downPosition)
                        }

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            val delta = change.position - change.previousPosition
                            totalX += delta.x
                            totalY += delta.y

                            if (isRingGesture) {
                                change.consume()
                                seekFromRing(change.position, force = !change.pressed)
                                if (!change.pressed) break
                                continue
                            }

                            if (axis == null && (abs(totalX) > touchSlop || abs(totalY) > touchSlop)) {
                                axis = if (abs(totalX) >= abs(totalY)) {
                                    GestureAxis.Horizontal
                                } else {
                                    GestureAxis.Vertical
                                }
                            }

                            when (axis) {
                                GestureAxis.Horizontal -> {
                                    change.consume()
                                    // Keep the current scene attached to the finger.
                                    // The directional AnimatedContent transition takes
                                    // over when the skip is committed on release.
                                    dragOffset = totalX.coerceIn(
                                        -gestureSize.width * 0.42f,
                                        gestureSize.width * 0.42f
                                    )
                                }

                                GestureAxis.Vertical -> {
                                    change.consume()
                                    verticalAccumulator += delta.y
                                    while (abs(verticalAccumulator) >= 10f) {
                                        val direction = if (verticalAccumulator < 0f) 1 else -1
                                        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                        if (direction > 0 && currentVolume < maxVolume) {
                                            audioManager.adjustStreamVolume(
                                                AudioManager.STREAM_MUSIC,
                                                AudioManager.ADJUST_RAISE,
                                                AudioManager.FLAG_SHOW_UI
                                            )
                                        } else if (direction < 0 && currentVolume > 0) {
                                            audioManager.adjustStreamVolume(
                                                AudioManager.STREAM_MUSIC,
                                                AudioManager.ADJUST_LOWER,
                                                AudioManager.FLAG_SHOW_UI
                                            )
                                        }
                                        verticalAccumulator -= direction * 10f
                                    }
                                }

                                null -> Unit
                            }

                            if (!change.pressed) break
                        }

                        if (isRingGesture) {
                            ringSeekPreview = null
                        } else {
                            when (axis) {
                                GestureAxis.Horizontal -> {
                                    if (abs(totalX) >= 150f && abs(totalX) > abs(totalY)) {
                                        val direction = if (totalX > 0f) 1 else -1
                                        transitionDirection = direction
                                        dragOffset = 0f
                                        if (direction > 0) {
                                            // Keep Ambient Mode's established behavior: a
                                            // right swipe always moves to the previous
                                            // queue item rather than using the normal
                                            // "restart after three seconds" button rule.
                                            playerConnection.player.seekToPreviousMediaItem()
                                        } else {
                                            playerConnection.player.seekToNext()
                                        }
                                    } else {
                                        dragOffset = 0f
                                    }
                                }

                                GestureAxis.Vertical -> {
                                    dragOffset = 0f
                                }

                                // A short, non-directional release is the only gesture
                                // that toggles playback. Horizontal swipes never fall
                                // through to this branch.
                                null -> {
                                    playerConnection.togglePlayPause()
                                    toggleWithFeedback()
                                }
                            }
                        }
                    }
                }
        ) {
            AnimatedContent(
                targetState = mediaMetadata,
                transitionSpec = {
                    val direction = transitionDirection
                    (
                        slideInHorizontally(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = 850f
                            ),
                            initialOffsetX = { fullWidth -> -direction * fullWidth }
                        ) + fadeIn(tween(90)) + scaleIn(tween(180), initialScale = 0.985f)
                    ).togetherWith(
                        slideOutHorizontally(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = 850f
                            ),
                            targetOffsetX = { fullWidth -> direction * fullWidth }
                        ) + fadeOut(tween(120)) + scaleOut(tween(180), targetScale = 0.985f)
                    ).using(SizeTransform(clip = false))
                },
                contentKey = { it?.id },
                label = "ambientTrackTransition",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationX = animatedDragOffset }
            ) { currentMetadata ->
                AmbientForeground(
                    mediaMetadata = currentMetadata,
                    playerConnection = playerConnection,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Feedback is transient only; there is intentionally no persistent playback
        // button in Ambient Mode.
        androidx.compose.animation.AnimatedVisibility(
            visible = showPlaybackFeedback,
            enter = fadeIn(tween(120)) + scaleIn(tween(120), initialScale = 0.8f),
            exit = fadeOut(tween(220)) + scaleOut(tween(220), targetScale = 0.8f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.Black.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AnimatedPlayPauseIcon(
                    isPlaying = feedbackIsPlaying,
                    tint = Color.White.copy(alpha = 0.88f),
                    size = 28.dp
                )
            }
        }

        // This sits outside the gesture arena so a back-button tap cannot also be
        // interpreted as a play/pause tap.
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .safeDrawingPadding()
                .padding(16.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.arrow_back),
                contentDescription = "Back",
                tint = Color.White
            )
        }
    }
}

private enum class GestureAxis {
    Horizontal,
    Vertical,
}

@Composable
private fun AmbientForeground(
    mediaMetadata: MediaMetadata?,
    playerConnection: PlayerConnection,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.safeDrawingPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Side: Album Art
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = mediaMetadata?.thumbnailUrl,
                contentDescription = "Album Art",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxHeight(0.85f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
            )
        }

        // Right Side: Lyrics
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 16.dp, end = 32.dp, top = 32.dp, bottom = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            InlineLyricsView(
                mediaMetadata = mediaMetadata,
                showLyrics = true,
                positionProvider = { playerConnection.player.currentPosition }
            )
        }
    }
}
