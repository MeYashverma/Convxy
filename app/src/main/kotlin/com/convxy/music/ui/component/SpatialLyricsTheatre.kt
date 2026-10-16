/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Spatial Multi-Singer Lyric Theatre.
 *
 * When a track carries multi-singer metadata (Apple TTML ttm:agent registry,
 * `{singers:…}` LRC header, or `v#:` / `{agent:…}` line prefixes) and the user
 * has enabled Theatre mode on a screen wide enough to justify it, lead-vocalist
 * lines are anchored to fixed "microphone positions" around the hero artwork
 * instead of scrolling as a single left-aligned column:
 *
 *   - Lead v1 (most lines)    -> bottom-left microphone
 *   - Lead v2 (second voice)   -> bottom-right microphone
 *   - v3, v5, … (odd further)  -> mid-left
 *   - v4, v6, … (even further) -> mid-right
 *   - Group / shared (v1000)   -> center, blooms outward on chorus
 *   - Background vocals        -> center, lower emphasis, blurred past
 *
 * Lines spring from their anchor to a central active-line band when their turn
 * comes, then drift off toward the opposite edge (retaining their singer color)
 * as new lines arrive. Word-level karaoke fill, singer badges, translation and
 * romanised lines are preserved from the standard renderer.
 *
 * Single-singer songs and tracks without singer metadata render through the
 * existing lyrics LazyColumn unchanged (fallback handled in Lyrics.kt).
 */

package com.convxy.music.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.convxy.music.lyrics.GROUP_AGENT_ID
import com.convxy.music.lyrics.LyricsEntry
import com.convxy.music.lyrics.primaryAgentId
import com.convxy.music.ui.screens.settings.LyricsPosition
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Where in the theatre a vocalist "stands." Positions are specified as
 * normalized fractions (0f..1f) of the lyric stage's width/height so they
 * scale cleanly across phones, tablets and Ambient Mode.
 *
 *   x=0 is left edge, x=1 is right edge; y=0 is top (near artwork), y=1 is
 *   bottom edge of the lyrics stage.
 */
@Immutable
data class SingerAnchor(
    val id: String,
    val xFraction: Float,
    val yFraction: Float,
    val side: AnchorSide,
    val isChorus: Boolean = false,
)

enum class AnchorSide {
    LEFT,
    RIGHT,
    CENTER,
    BACK,
}

/**
 * Builds a deterministic anchor map for every distinct lead vocalist appearing
 * in [entries]. Odd-numbered v-ids land on the left, even on the right, and
 * group/chorus (v1000 / explicit composite ids) anchors to centre-back.
 *
 * The two microphones closest to centre stage are given to the two most
 * dominant voices (most lead lines); additional vocalists fan outward. This
 * keeps the two leads closest to the active-line band regardless of whether
 * Apple numbered them v1/v2 or v2/v1.
 */
@Stable
fun buildSingerAnchors(entries: List<LyricsEntry>): Map<String, SingerAnchor> {
    // Count lead-line dominance per agent (ignoring background vocals).
    val leadCounts = LinkedHashMap<String, Int>()
    entries.forEach { e ->
        if (e.isBackground) return@forEach
        val id = e.agent?.let { primaryAgentId(it) } ?: return@forEach
        leadCounts[id] = (leadCounts[id] ?: 0) + 1
    }
    if (leadCounts.size < 2) return emptyMap()

    // Order vocalists by how many lead lines they sing (most first).
    val ordered = leadCounts.keys.sortedByDescending { leadCounts[it] ?: 0 }

    // Microphone positions, ordered by proximity to centre stage. First two
    // leads always take the two primary downstage mics; any other voices
    // alternate outwards and further upstage.
    val micSlots: List<SingerAnchor> = listOf(
        SingerAnchor("__leadL__", 0.20f, 0.78f, AnchorSide.LEFT),
        SingerAnchor("__leadR__", 0.80f, 0.78f, AnchorSide.RIGHT),
        SingerAnchor("__midL__", 0.12f, 0.55f, AnchorSide.LEFT),
        SingerAnchor("__midR__", 0.88f, 0.55f, AnchorSide.RIGHT),
        SingerAnchor("__farL__", 0.05f, 0.32f, AnchorSide.LEFT),
        SingerAnchor("__farR__", 0.95f, 0.32f, AnchorSide.RIGHT),
    )

    val result = HashMap<String, SingerAnchor>()
    ordered.forEachIndexed { index, id ->
        when {
            id == GROUP_AGENT_ID -> {
                result[id] = SingerAnchor(
                    id = id,
                    xFraction = 0.50f,
                    yFraction = 0.60f,
                    side = AnchorSide.CENTER,
                    isChorus = true,
                )
            }
            index < micSlots.size -> {
                val slot = micSlots[index]
                result[id] = slot.copy(id = id)
            }
            else -> {
                val cycle = index - micSlots.size
                val leftSide = cycle % 2 == 0
                result[id] = SingerAnchor(
                    id = id,
                    xFraction = if (leftSide) 0.08f else 0.92f,
                    yFraction = 0.18f + (cycle / 2) * 0.07f,
                    side = if (leftSide) AnchorSide.LEFT else AnchorSide.RIGHT,
                )
            }
        }
    }
    return result
}

/**
 * Returns the absolute anchor [Offset] in pixels for a [SingerAnchor] within a
 * [stageWidth] × [stageHeight] px stage.
 */
fun anchorOffset(
    anchor: SingerAnchor,
    stageWidthPx: Float,
    stageHeightPx: Float,
): Offset = Offset(
    x = anchor.xFraction * stageWidthPx,
    y = anchor.yFraction * stageHeightPx,
)

private const val ACTIVE_BAND_Y_FRACTION = 0.55f

/**
 * Calculates a per-line on-stage offset for a lyric line based on its singer's
 * anchor and whether the line is upcoming, active, or past.
 */
fun lineStageOffset(
    anchor: SingerAnchor,
    stageWidthPx: Float,
    stageHeightPx: Float,
    distanceFromCurrent: Int,
    isActive: Boolean,
): Offset {
    val anchorPx = anchorOffset(anchor, stageWidthPx, stageHeightPx)
    val activeBandY = ACTIVE_BAND_Y_FRACTION * stageHeightPx
    // Active line blooms to a centered wide slice regardless of which mic
    // the singer is at, so the approach target is center-stage. (The actual
    // slice is re-centered in SpatialLyricsLine when isActive.)
    val activeBandX = 0.50f * stageWidthPx

    return when {
        distanceFromCurrent < 0 -> {
            val pastDepth = (-distanceFromCurrent).coerceAtMost(5)
            val driftX = when (anchor.side) {
                AnchorSide.LEFT -> anchorPx.x + pastDepth * 0.12f * stageWidthPx
                AnchorSide.RIGHT -> anchorPx.x - pastDepth * 0.12f * stageWidthPx
                AnchorSide.CENTER, AnchorSide.BACK -> anchorPx.x
            }
            val driftY = anchorPx.y - pastDepth * 0.10f * stageHeightPx
            Offset(driftX, driftY)
        }
        isActive -> Offset(activeBandX, activeBandY)
        distanceFromCurrent in 1..2 -> {
            val approach = 1f - distanceFromCurrent / 3f
            val x = anchorPx.x + (activeBandX - anchorPx.x) * approach * 0.5f
            val y = anchorPx.y + (activeBandY - anchorPx.y) * approach * 0.5f
            Offset(x, y)
        }
        else -> anchorPx
    }
}

/**
 * Spatial lyric line container: positions a single lyric line at its singer's
 * anchor (or on the active band when it is currently being sung), with the
 * same spring, glow and karaoke-fill styling as the existing Apple Music line.
 */
@Composable
fun SpatialLyricsLine(
    entry: LyricsEntry,
    singerId: String,
    anchor: SingerAnchor,
    stageWidthPx: Float,
    stageHeightPx: Float,
    isActive: Boolean,
    distanceFromCurrent: Int,
    effectivePlaybackPosition: Long,
    nextEntryTime: Long?,
    isSynced: Boolean,
    textColor: Color,
    textSize: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val targetOffset = remember(anchor, stageWidthPx, stageHeightPx, isActive, distanceFromCurrent) {
        lineStageOffset(anchor, stageWidthPx, stageHeightPx, distanceFromCurrent, isActive)
    }

    val animatedOffset by animateOffsetAsState(
        targetValue = targetOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = if (isActive) Spring.StiffnessMedium else Spring.StiffnessLow,
        ),
        label = "spatialLineOffset",
    )

    val activeAlpha = when {
        !isSynced -> 1f
        isActive -> 1f
        distanceFromCurrent < 0 -> {
            val d = (-distanceFromCurrent).coerceAtMost(4)
            0.55f - d * 0.13f
        }
        distanceFromCurrent in 1..3 -> 0.85f - (distanceFromCurrent - 1) * 0.15f
        else -> 0.45f
    }.coerceIn(0f, 1f)

    val lineAlpha by animateFloatAsState(
        targetValue = activeAlpha,
        animationSpec = tween(durationMillis = 350),
        label = "spatialLineAlpha",
    )

    val lineScale by animateFloatAsState(
        targetValue = if (isActive) 1.05f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "spatialLineScale",
    )

    val blurRadius = when {
        !isSynced || isActive -> 0.dp
        distanceFromCurrent < 0 -> (2 * (-distanceFromCurrent).coerceAtMost(3)).dp
        distanceFromCurrent > 2 -> 1.dp
        else -> 0.dp
    }

    // Side alignment/tint depends on anchor side. The active line blooms to
    // a wider, centered column across the middle of the stage (like a real
    // performer stepping up to the mic); waiting/past lines stay in narrower
    // slices closer to their mic.
    val isLeftAnchor = anchor.side == AnchorSide.LEFT
    val isRightAnchor = anchor.side == AnchorSide.RIGHT
    val sideAlignment = when {
        isActive -> Alignment.Center
        isLeftAnchor -> Alignment.CenterStart
        isRightAnchor -> Alignment.CenterEnd
        else -> Alignment.Center
    }
    val textAlign = when {
        isActive -> TextAlign.Center
        isLeftAnchor -> TextAlign.Left
        isRightAnchor -> TextAlign.Right
        else -> TextAlign.Center
    }
    // Active lines get a wider center column; waiting/past lines stay in
    // narrower anchor slices. This prevents the overlap/drift that happens
    // when two 42%-wide slices collide in the middle of the stage.
    val sliceFraction = when {
        isActive -> 0.70f
        anchor.side == AnchorSide.CENTER || anchor.side == AnchorSide.BACK -> 0.55f
        else -> 0.36f
    }
    val sliceWidth = stageWidthPx * sliceFraction
    // When the line is active, center its slice on the stage horizontally;
    // otherwise center it on its anchor (so LEFT/RIGHT anchors stay in their
    // own stage halves rather than overlapping in the middle).
    val anchorX = if (isActive) stageWidthPx / 2f else animatedOffset.x

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    x = (anchorX - sliceWidth / 2f).roundToInt(),
                    y = (animatedOffset.y).roundToInt(),
                )
            }
            .fillMaxWidth(sliceFraction)
            .alpha(lineAlpha)
            .blur(blurRadius)
            .scale(lineScale),
    ) {
        AppleMusicLyricsLine(
            entry = entry,
            nextEntryTime = nextEntryTime,
            effectivePlaybackPosition = effectivePlaybackPosition,
            isSynced = isSynced,
            isActive = isActive,
            distanceFromCurrent = distanceFromCurrent,
            lyricsTextPosition = LyricsPosition.CENTER,
            textColor = textColor,
            textSize = textSize * if (isActive) 1f else 0.88f,
            lineSpacing = 1.1f,
            onClick = onClick,
            onLongClick = onLongClick,
            overrideAlignment = sideAlignment,
            overrideTextAlign = textAlign,
            // IMPORTANT: do NOT call fillMaxWidth(fraction) again here. The
            // outer Box already caps the slice to `sliceFraction` of the
            // stage; applying another fractional fillMaxWidth would multiply
            // (0.36 * 0.36 = 13% of stage, etc.) and cause letters to wrap
            // one character per line.
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    when {
                        isLeftAnchor && !isActive -> Modifier.padding(end = 18.dp)
                        isRightAnchor && !isActive -> Modifier.padding(start = 18.dp)
                        else -> Modifier.padding(horizontal = 12.dp)
                    }
                ),
        )
    }
}

/**
 * Full-screen Theatre stage. Replaces the standard LazyColumn when the user
 * has opted in, the caller is a wide/fullscreen layout, and the track has
 * multi-singer metadata.
 */
@Composable
fun SpatialLyricsTheatre(
    lines: List<LyricsEntry>,
    currentLineIndex: Int,
    isLineActive: (Int) -> Boolean,
    effectivePlaybackPositionProvider: () -> Long,
    singerColorMap: Map<String, Color>,
    singerAnchors: Map<String, SingerAnchor>,
    singerBadgeName: (index: Int, singerId: String) -> String?,
    onLineClick: (index: Int) -> Unit,
    onLineLongClick: (index: Int) -> Unit,
    textColor: Color,
    textSize: Float,
    isSynced: Boolean,
    stageHeightDp: Dp = 520.dp,
    stageContent: @Composable () -> Unit = {},
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(stageHeightDp),
    ) {
        val density = LocalDensity.current
        val stageWidthPx = with(density) { maxWidth.toPx() }
        val stageHeightPx = with(density) { stageHeightDp.toPx() }

        // Defensive guard: don't draw the spatial layout on stages narrower
        // than ~400dp. At phone-portrait widths or mid-rotation the slices
        // get too narrow and text wraps vertically; fall back to letting
        // the caller's LazyColumn render. (PreferTheatre should already keep
        // us off portrait, but sensor/rotation race conditions can still
        // briefly deliver a narrow width mid-transition.)
        val stageIsWideEnough = maxWidth >= 400.dp

        Box(Modifier.fillMaxSize()) { stageContent() }

        if (stageIsWideEnough && currentLineIndex >= 0 && lines.isNotEmpty()) {
            val windowStart = (currentLineIndex - 4).coerceAtLeast(0)
            val windowEnd = (currentLineIndex + 4).coerceAtMost(lines.lastIndex)
            val activeSliceWidth = stageWidthPx * 0.70f

            for (index in windowStart..windowEnd) {
                key(index) {
                    val entry = lines[index]
                    val singerId = entry.agent?.let { primaryAgentId(it) }
                    val anchor = singerId?.let { singerAnchors[it] }
                    if (singerId != null && anchor != null) {
                        val lineAccent = singerColorMap[singerId] ?: textColor
                        val isActive = isLineActive(index)
                        val distance = index - currentLineIndex
                        val effectivePosition =
                            if (abs(index - currentLineIndex) <= 2) {
                                effectivePlaybackPositionProvider()
                            } else {
                                0L
                            }
                        val badgeName = singerBadgeName(index, singerId)

                        if (badgeName != null && isActive) {
                            // Center the singer badge directly over the active
                            // line's wide center slice.
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.70f)
                                    .offset {
                                        val offset = lineStageOffset(
                                            anchor, stageWidthPx, stageHeightPx,
                                            distance, isActive,
                                        )
                                        IntOffset(
                                            x = (stageWidthPx / 2f - activeSliceWidth / 2f).roundToInt(),
                                            y = (offset.y - 56f).roundToInt(),
                                        )
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                SingerBadge(name = badgeName, color = lineAccent)
                            }
                        }

                        SpatialLyricsLine(
                            entry = entry,
                            singerId = singerId,
                            anchor = anchor,
                            stageWidthPx = stageWidthPx,
                            stageHeightPx = stageHeightPx,
                            isActive = isActive,
                            distanceFromCurrent = distance,
                            effectivePlaybackPosition = effectivePosition,
                            nextEntryTime = lines.getOrNull(index + 1)?.time,
                            isSynced = isSynced,
                            textColor = lineAccent,
                            textSize = textSize,
                            onClick = { onLineClick(index) },
                            onLongClick = { onLineLongClick(index) },
                        )
                    }
                }
            }
        }
    }
}
