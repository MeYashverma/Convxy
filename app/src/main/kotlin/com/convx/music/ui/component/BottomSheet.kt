/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import com.convx.music.ui.player.SCREEN_CORNER_EXPANSION_MILLIS
import com.convx.music.ui.player.sharedContainerCornerRadius
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.convx.music.constants.NavigationBarAnimationSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.pow

/** Mirrors BackdropFreeze.kt's identical constant: a safety net for drags that
 *  never deliver onDragEnd/onDragCancel (e.g. gesture stolen elsewhere). */
/**
 * Progress at which the collapsed and expanded layers hand over.
 *
 * One value for both halves so they cannot drift into overlapping again: the mini player
 * is fully faded out here, and the expanded content starts fading in here.
 */
internal const val PLAYER_LAYER_HANDOFF_PROGRESS = 0.25f

private const val BackdropFreezeSafetyNs = 900_000_000L

/**
 * Bottom Sheet
 * Modified from [ViMusic](https://github.com/vfsfitvnm/ViMusic)
 */
@Composable
fun BottomSheet(
    state: BottomSheetState,
    modifier: Modifier = Modifier,
    background: @Composable (BoxScope.() -> Unit) = { },
    onDismiss: (() -> Unit)? = null,
    collapsedContent: @Composable BoxScope.() -> Unit,
    isExpandable: Boolean = true,
    /** Caps the expanded content to this width, centered horizontally, so the
     *  controls read as one phone-shaped panel on a wide screen instead of
     *  stretching edge to edge. [background] (the full-bleed artwork/wash) is
     *  deliberately NOT capped by this — it stays full-screen regardless, same
     *  as the mobile layout's own artwork treatment; only the controls above it
     *  get centered and width-limited. The collapsed/mini content (a full-width
     *  dock regardless) is unaffected either way.
     *  [Dp.Unspecified] (default) lets content fill the sheet exactly as before. */
    contentMaxWidth: Dp = Dp.Unspecified,
    /** Corner radius the sheet rounds to as it reaches full size. The player passes the
     *  device's physical screen radius so the sheet's corners land on the glass; other
     *  callers can leave it and keep the plain collapsed-to-square curve. */
    expandedCornerRadius: Dp = 0.dp,
    /** Corner radius while collapsed. */
    collapsedCornerRadius: Dp = 16.dp,
    /**
     * Drawn as a sibling of the sheet's own draggable content, NOT nested inside it --
     * the sheet applies its own `translationY` to track the drag, and this overlay needs
     * plain, unshifted root/window coordinates to place things by. Null for every caller
     * that doesn't need one (this is a generic sheet primitive; only the player's own
     * `BottomSheet(...)` call passes one, for the mini-to-full artwork morph).
     */
    overlayContent: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current

    // Second stage of the corner treatment: once the sheet has settled at full size, its
    // corners open out to square over a short beat of their own, so the player ends up
    // filling the display edge to edge with the device's own glass doing the rounding.
    // Separate from the sheet's motion on purpose -- the corners resolve after the sheet
    // arrives, not with it.
    val cornerExpansion = remember { Animatable(0f) }
    LaunchedEffect(state.isExpanded) {
        cornerExpansion.animateTo(
            targetValue = if (state.isExpanded) 1f else 0f,
            animationSpec = tween(SCREEN_CORNER_EXPANSION_MILLIS),
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                // background fades during about 10%-61% progress
                alpha = (1.4f * (state.progress.coerceAtLeast(0.1f) - 0.1f).pow(0.5f)).coerceIn(0f, 1f)
            }
            .fillMaxSize(),
    ) {
        Box(
            // Always full-bleed — see contentMaxWidth's doc above.
            modifier = Modifier.fillMaxSize(),
            content = background,
        )
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            // Use graphicsLayer for offset to ensure hardware acceleration and 120Hz support
            .graphicsLayer {
                val y = (state.expandedBound - state.value)
                    .toPx()
                    .coerceAtLeast(0f)
                translationY = y
            }
            .pointerInput(state, isExpandable) {
                if (!isExpandable) return@pointerInput
                val velocityTracker = VelocityTracker()

                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        state.dragClockNs[0] = System.nanoTime()
                        velocityTracker.addPointerInputChange(change)
                        state.dispatchRawDelta(dragAmount)
                    },
                    onDragCancel = {
                        state.dragClockNs[0] = 0L
                        velocityTracker.resetTracking()
                        state.snapTo(state.collapsedBound)
                    },
                    onDragEnd = {
                        state.dragClockNs[0] = 0L
                        val velocity = -velocityTracker.calculateVelocity().y
                        velocityTracker.resetTracking()
                        state.performFling(velocity, onDismiss)
                    }
                )
            }
            .graphicsLayer {
                // Was `if (!state.isExpanded) 16.dp else 0f` -- a hard switch, so the
                // corners popped square the moment the sheet latched open and popped back
                // on the first pixel of a drag. Both endpoints are unchanged; what is new
                // is that the radius now travels between them.
                val cornerRadius = sharedContainerCornerRadius(
                    collapsedCornerRadius = collapsedCornerRadius.toPx(),
                    expandedCornerRadius = expandedCornerRadius.toPx(),
                    progress = state.progress,
                    screenCornerExpansionProgress = cornerExpansion.value,
                )
                shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
                clip = true
            }
    ) {
        if (!state.isCollapsed && !state.isDismissed) {
            BackHandler(onBack = state::collapseSoft)
        }

        // main content
        if (!state.isCollapsed) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Starts where the mini player has finished leaving, not before
                        // it. The two used to overlap between 0.15 and 0.25, so for that
                        // slice of every open and close both were on screen at partial
                        // opacity and the artwork ghosted against itself.
                        alpha = ((state.progress - PLAYER_LAYER_HANDOFF_PROGRESS) / 0.2f)
                            .coerceIn(0f, 1f)
                    },
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxHeight()
                        .then(
                            if (contentMaxWidth.isSpecified) {
                                Modifier.widthIn(max = contentMaxWidth)
                            } else {
                                Modifier.fillMaxWidth()
                            }
                        ),
                    content = content,
                )
            }
        }

        if (!state.isExpanded && (onDismiss == null || !state.isDismissed)) {
            Box(
                modifier =
                Modifier
                    .graphicsLayer {
                        // Fully gone exactly where the expanded content starts to arrive.
                        alpha = 1f - (state.progress / PLAYER_LAYER_HANDOFF_PROGRESS)
                            .coerceIn(0f, 1f)
                    }.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { if (isExpandable) state.expandSoft() },
                    ).fillMaxWidth()
                    .height(state.collapsedBound),
                content = collapsedContent,
            )
        }
    }

    // Sibling of the translated sheet Box above, not a child of it -- see the
    // parameter doc. Unaffected by the sheet's own drag translationY, so the
    // overlay's own translations (root-relative rects it was handed) land where
    // they mean to instead of being shifted a second time.
    if (overlayContent != null) {
        Box(modifier = Modifier.fillMaxSize(), content = overlayContent)
    }
}

@Stable
class BottomSheetState(
    draggableState: DraggableState,
    private val coroutineScope: CoroutineScope,
    private val animatable: Animatable<Dp, AnimationVector1D>,
    private val onAnchorChanged: (Int) -> Unit,
    val collapsedBound: Dp,
) : DraggableState by draggableState {
    // Same technique as BackdropFreeze.kt: a plain array, not snapshot state,
    // read during the draw phase by a sampling glass surface's `frozen`
    // provider. A snapshot read there would register a draw dependency and
    // every write would re-invalidate the frame forever (the trap documented
    // at MainActivity.kt:1253 and BackdropFreeze.kt:21-23).
    internal val dragClockNs = longArrayOf(0L)

    /** Pass to a `background` slot's [layerBackdrop] to skip re-recording it
     *  while this sheet is being dragged, mirroring [BackdropFreeze]. */
    val backdropFrozen: () -> Boolean = {
        val started = dragClockNs[0]
        started != 0L && System.nanoTime() - started < BackdropFreezeSafetyNs
    }

    val dismissedBound: Dp
        get() = animatable.lowerBound!!

    val expandedBound: Dp
        get() = animatable.upperBound!!

    val value by animatable.asState()

    val isDismissed by derivedStateOf {
        value == animatable.lowerBound!!
    }

    val isCollapsed by derivedStateOf {
        value == collapsedBound
    }

    val isExpanded by derivedStateOf {
        value == animatable.upperBound
    }

    val progress by derivedStateOf {
        1f - (animatable.upperBound!! - animatable.value) / (animatable.upperBound!! - collapsedBound)
    }

    fun collapse(animationSpec: AnimationSpec<Dp>) {
        onAnchorChanged(collapsedAnchor)
        coroutineScope.launch {
            animatable.animateTo(collapsedBound, animationSpec)
        }
    }

    fun expand(animationSpec: AnimationSpec<Dp>) {
        onAnchorChanged(expandedAnchor)
        coroutineScope.launch {
            animatable.animateTo(animatable.upperBound!!, animationSpec)
        }
    }

    private fun collapse() {
        // Apple Music feel: bouncy spring for collapse
        collapse(
            spring(
                dampingRatio = 0.75f,
                stiffness = Spring.StiffnessLow,
            )
        )
    }

    private fun expand() {
        // Apple Music feel: bouncy spring for expand
        expand(
            spring(
                dampingRatio = 0.75f,
                stiffness = Spring.StiffnessLow,
            )
        )
    }

    fun collapseSoft() {
        // Apple Music feel: high damping + low stiffness = smooth, weighty settle.
        collapse(
            spring(
                dampingRatio = 0.85f,
                stiffness = Spring.StiffnessLow,
            ),
        )
    }

    fun expandSoft() {
        expand(
            spring(
                dampingRatio = 0.85f,
                stiffness = Spring.StiffnessLow,
            ),
        )
    }

    fun dismiss() {
        onAnchorChanged(dismissedAnchor)
        coroutineScope.launch {
            animatable.animateTo(animatable.lowerBound!!)
        }
    }
    
    suspend fun dismissAndWait() {
        onAnchorChanged(dismissedAnchor)
        animatable.animateTo(animatable.lowerBound!!)
    }

    fun snapTo(value: Dp) {
        coroutineScope.launch {
            animatable.snapTo(value)
        }
    }

    fun performFling(velocity: Float, onDismiss: (() -> Unit)?) {
        if (velocity > 250) {
            expand()
        } else if (velocity < -250) {
            if (value < collapsedBound && onDismiss != null) {
                dismiss()
                onDismiss.invoke()
            } else {
                collapse()
            }
        } else {
            val l0 = dismissedBound
            val l1 = (collapsedBound - dismissedBound) / 2
            val l2 = (expandedBound - collapsedBound) / 2
            val l3 = expandedBound

            when (value) {
                in l0..l1 -> {
                    if (onDismiss != null) {
                        dismiss()
                        onDismiss.invoke()
                    } else {
                        collapse()
                    }
                }

                in l1..l2 -> collapse()
                in l2..l3 -> expand()
                else -> Unit
            }
        }
    }

    val preUpPostDownNestedScrollConnection
        get() = object : NestedScrollConnection {
            var isTopReached = false

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isExpanded && available.y < 0) {
                    isTopReached = false
                }

                return if (isTopReached && available.y < 0 && source == NestedScrollSource.UserInput) {
                    dragClockNs[0] = System.nanoTime()
                    dispatchRawDelta(available.y)
                    available
                } else {
                    Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (!isTopReached) {
                    isTopReached = consumed.y == 0f && available.y > 0
                }

                return if (isTopReached && source == NestedScrollSource.UserInput) {
                    dragClockNs[0] = System.nanoTime()
                    dispatchRawDelta(available.y)
                    available
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                return if (isTopReached) {
                    val velocity = -available.y
                    performFling(velocity, null)

                    available
                } else {
                    Velocity.Zero
                }
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                isTopReached = false
                dragClockNs[0] = 0L
                return Velocity.Zero
            }
        }
}

const val expandedAnchor = 2
const val collapsedAnchor = 1
const val dismissedAnchor = 0

@Composable
fun rememberBottomSheetState(
    dismissedBound: Dp,
    expandedBound: Dp,
    collapsedBound: Dp = dismissedBound,
    initialAnchor: Int = dismissedAnchor,
): BottomSheetState {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    var previousAnchor by rememberSaveable {
        mutableIntStateOf(initialAnchor)
    }
    val animatable = remember {
        Animatable(0.dp, Dp.VectorConverter)
    }

    return remember(dismissedBound, expandedBound, collapsedBound, coroutineScope) {
        val initialValue = when (previousAnchor) {
            expandedAnchor -> expandedBound
            collapsedAnchor -> collapsedBound
            dismissedAnchor -> dismissedBound
            else -> error("Unknown BottomSheet anchor")
        }

        animatable.updateBounds(dismissedBound.coerceAtMost(expandedBound), expandedBound)
        coroutineScope.launch {
            animatable.animateTo(initialValue, NavigationBarAnimationSpec)
        }

        BottomSheetState(
            draggableState = DraggableState { delta ->
                coroutineScope.launch {
                    animatable.snapTo(animatable.value - with(density) { delta.toDp() })
                }
            },
            onAnchorChanged = { previousAnchor = it },
            coroutineScope = coroutineScope,
            animatable = animatable,
            collapsedBound = collapsedBound
        )
    }
}
