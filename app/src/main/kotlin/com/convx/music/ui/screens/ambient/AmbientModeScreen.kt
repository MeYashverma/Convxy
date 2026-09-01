/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.screens.ambient

import android.app.Activity
import android.content.pm.ActivityInfo
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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.convx.music.utils.makeTimeString
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.floor

@Composable
fun AmbientModeScreen(navController: NavController) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
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
    var showBackButton by remember { mutableStateOf(true) }
    var chromeInteractionToken by remember { mutableIntStateOf(0) }
    var showTrackInfo by remember { mutableStateOf(false) }
    var ringSeekPreviewPosition by remember { mutableLongStateOf(0L) }
    var ringSeekPreviewDuration by remember { mutableLongStateOf(0L) }
    val latestIsPlayingState = rememberUpdatedState(isPlaying)
    val latestBackButtonVisible = rememberUpdatedState(showBackButton)
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

    LaunchedEffect(chromeInteractionToken) {
        showBackButton = true
        delay(3000L)
        showBackButton = false
    }

    LaunchedEffect(mediaMetadata?.id) {
        ringSeekPreview = null
        ringSeekPreviewPosition = 0L
        ringSeekPreviewDuration = 0L
        if (mediaMetadata == null) {
            showTrackInfo = false
            return@LaunchedEffect
        }
        showTrackInfo = true
        showBackButton = true
        chromeInteractionToken++
        delay(2400L)
        showTrackInfo = false
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

        // A single gesture arena prevents a tap, a horizontal skip, and a ring seek
        // from all reacting to the same pointer sequence. Vertical movement is left
        // available to the lyrics surface.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(
                    playerConnection,
                    mediaMetadata?.id,
                    ringHitSlop,
                    ringInset,
                    ringCornerRadius,
                    touchSlop,
                ) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val wasBackButtonVisible = latestBackButtonVisible.value
                        val startedInBackButtonArea = isAmbientBackButtonTouch(down.position, ringHitSlop)
                        showBackButton = true
                        chromeInteractionToken++

                        // When the back button is hidden, the first tap in its
                        // former area only reveals it. This prevents an accidental
                        // play/pause toggle while bringing navigation back.
                        if (!wasBackButtonVisible && startedInBackButtonArea) {
                            down.consume()
                            return@awaitEachGesture
                        }

                        val pointerId = down.id
                        val downPosition = down.position
                        val gestureSize = Size(
                            width = size.width.toFloat(),
                            height = size.height.toFloat()
                        )
                        val ringDuration = if (!startedInBackButtonArea && isNearBezel(
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
                        var axis: GestureAxis? = null
                        var lastRingFraction = -1f
                        var lastHapticMarker = -1
                        var tapWasConsumed = false

                        fun seekFromRing(position: Offset, force: Boolean = false) {
                            if (!isRingGesture) return
                            val fraction = bezelProgressFor(
                                position = position,
                                size = gestureSize,
                                inset = ringInset,
                                cornerRadius = ringCornerRadius
                            )
                            ringSeekPreview = fraction
                            ringSeekPreviewPosition = (ringDuration * fraction).toLong()
                            ringSeekPreviewDuration = ringDuration

                            // Give the user a quiet tactile cue at each quarter of
                            // the track while dragging, without buzzing on touch-down.
                            val hapticMarker = (floor(fraction * 4f)).toInt().coerceIn(0, 3)
                            if (lastHapticMarker >= 0 && hapticMarker != lastHapticMarker) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            lastHapticMarker = hapticMarker

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

                                GestureAxis.Vertical -> Unit

                                null -> Unit
                            }

                            if (!change.pressed) {
                                tapWasConsumed = change.isConsumed
                                break
                            }
                        }

                        if (isRingGesture) {
                            ringSeekPreview = null
                            ringSeekPreviewPosition = 0L
                            ringSeekPreviewDuration = 0L
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
                                    // Lyrics lines have their own tap-to-seek
                                    // behavior. If that child consumed the tap,
                                    // do not also toggle playback.
                                    if (!tapWasConsumed && !startedInBackButtonArea) {
                                        playerConnection.togglePlayPause()
                                        toggleWithFeedback()
                                    }
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

        // Track metadata is intentionally temporary. It gives the song change a
        // clear acknowledgement without leaving player controls on screen.
        androidx.compose.animation.AnimatedVisibility(
            visible = showTrackInfo && mediaMetadata != null,
            enter = slideInVertically(
                animationSpec = tween(260),
                initialOffsetY = { it / 2 },
            ) + fadeIn(tween(220)),
            exit = slideOutVertically(
                animationSpec = tween(220),
                targetOffsetY = { it / 3 },
            ) + fadeOut(tween(180)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp),
        ) {
            mediaMetadata?.let { metadata ->
                val artistText = metadata.artists
                    .joinToString(", ") { it.name }
                    .ifBlank { "Unknown artist" }
                Column(
                    modifier = Modifier
                        .widthIn(min = 180.dp, max = 420.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.30f),
                            shape = RoundedCornerShape(20.dp),
                        )
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = metadata.title,
                        color = Color.White.copy(alpha = 0.96f),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = artistText,
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Show the ring's exact seek target while dragging instead of making the
        // user estimate it from a very thin bezel line.
        androidx.compose.animation.AnimatedVisibility(
            visible = ringSeekPreview != null,
            enter = fadeIn(tween(120)) + scaleIn(tween(120), initialScale = 0.92f),
            exit = fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.92f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp),
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = Color.Black.copy(alpha = 0.28f),
                        shape = RoundedCornerShape(18.dp),
                    )
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    text = "${makeTimeString(ringSeekPreviewPosition)} / " +
                        makeTimeString(ringSeekPreviewDuration),
                    color = Color.White.copy(alpha = 0.90f),
                    style = MaterialTheme.typography.labelMedium,
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
        // interpreted as a play/pause tap. It fades away after idle time and is
        // revealed again by any interaction.
        androidx.compose.animation.AnimatedVisibility(
            visible = showBackButton,
            enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.86f),
            exit = fadeOut(tween(220)) + scaleOut(tween(220), targetScale = 0.86f),
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .safeDrawingPadding()
                    .padding(16.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }
        }
    }
}

private enum class GestureAxis {
    Horizontal,
    Vertical,
}

private fun isAmbientBackButtonTouch(position: Offset, hitSlop: Float): Boolean =
    position.x <= hitSlop * 2.6f && position.y <= hitSlop * 2.6f

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
