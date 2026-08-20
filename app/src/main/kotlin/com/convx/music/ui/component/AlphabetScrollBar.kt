/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.convx.music.ui.component

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.convx.music.ui.component.shapes.ContinuousRoundedRectangle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Digits bucket first, then A-Z, then everything non-Latin under a single tail bucket. */
val AlphabetSections: List<String> = listOf("0") + ('A'..'Z').map(Char::toString) + listOf("#")

/**
 * Below this the rail is clutter: a list you can reach the end of in two flings does not
 * need a scrubber, and the rail costs real estate on every row it overlaps.
 */
const val AlphabetScrollBarMinItems = 30

/**
 * Fast-scrub rail pinned to the trailing edge of a long list: drag a finger down it and
 * the list jumps section by section, with a bubble showing the section under the finger.
 *
 * Two details make it feel like a native fast-scroller rather than a row of small
 * buttons:
 *
 * - The rail divides whatever height it is given by the number of sections, so the first
 *   and last targets stay pinned to the ends and only the spacing between them changes.
 *   Letters shrink to 4sp on a short window rather than the rail scrolling or clipping.
 * - Selection updates on drag position, not on per-letter hit testing, so a fast scrub
 *   never drops a section between two sampled pointer events.
 *
 * [sectionIndexMap] maps a section key to the list index where it starts. Sections with
 * no items are not skipped in the rail — they resolve to the next populated section
 * downward (see [findAlphabetTargetIndex]), which keeps the letters at fixed positions
 * so muscle memory survives a re-sort.
 */
@Composable
fun AlphabetScrollBar(
    sectionIndexMap: Map<String, Int>,
    itemCount: Int,
    isAtTarget: (Int) -> Boolean,
    scrollToItem: suspend (Int) -> Unit,
    modifier: Modifier = Modifier,
    sections: List<String> = AlphabetSections,
) {
    val view = LocalView.current
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val scope = rememberCoroutineScope()

    var scrollJob by remember { mutableStateOf<Job?>(null) }
    var selectedSection by remember { mutableStateOf<String?>(null) }
    var indicatorSection by remember { mutableStateOf<String?>(null) }
    var indicatorVisible by remember { mutableStateOf(false) }
    var lastSelectedIndex by remember { mutableIntStateOf(-1) }

    val currentItemCount by rememberUpdatedState(itemCount)
    val currentSectionIndexMap by rememberUpdatedState(sectionIndexMap)
    val currentSections by rememberUpdatedState(sections)
    val currentIsAtTarget by rememberUpdatedState(isAtTarget)
    val currentScrollToItem by rememberUpdatedState(scrollToItem)

    fun updateSelection(index: Int) {
        if (index !in currentSections.indices || index == lastSelectedIndex) return
        lastSelectedIndex = index
        val section = currentSections[index]
        selectedSection = section
        indicatorSection = section
        indicatorVisible = true

        val maxIndex = currentItemCount - 1
        if (maxIndex < 0) return
        val target = findAlphabetTargetIndex(
            section = section,
            sectionIndexMap = currentSectionIndexMap,
            sections = currentSections,
        ).coerceIn(0, maxIndex)

        if (!currentIsAtTarget(target)) {
            scrollJob?.cancel()
            scrollJob = scope.launch {
                try {
                    val latestMax = currentItemCount - 1
                    if (latestMax >= 0) currentScrollToItem(target.coerceAtMost(latestMax))
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: IndexOutOfBoundsException) {
                    // A re-sort can replace the lazy layout between validation and scrolling.
                } catch (_: IllegalArgumentException) {
                    // Lazy layouts reject an index from a concurrently replaced item provider.
                }
            }
        }
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun clearSelection() {
        selectedSection = null
        indicatorVisible = false
        lastSelectedIndex = -1
    }

    BoxWithConstraints(modifier = modifier) {
        if (sections.isEmpty() || maxHeight <= 0.dp) return@BoxWithConstraints

        // Captured into a local: BoxWithConstraintsScope and RowScope both carry
        // @LayoutScopeMarker, so inside the Row below the outer scope is shadowed and
        // maxHeight stops resolving.
        val railHeight = maxHeight
        val cellSize = railHeight / sections.size.toFloat()
        val cellHeightPx = with(LocalDensity.current) { cellSize.toPx() }

        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .height(railHeight),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlphabetIndicator(section = indicatorSection, visible = indicatorVisible)
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .width(cellSize.coerceAtLeast(20.dp))
                    .height(railHeight)
                    // The rail is a scrub surface, not 28 buttons. Announcing every
                    // letter would bury the list itself in the accessibility tree, and
                    // a scrub gesture is not reachable that way regardless.
                    .clearAndSetSemantics {}
                    .pointerInput(sections, cellHeightPx, sectionIndexMap) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            var dragged = false
                            updateSelection((down.position.y / cellHeightPx).toInt())
                            try {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                        ?: break
                                    if (!change.pressed) break
                                    change.consume()
                                    if (!dragged &&
                                        (change.position - down.position).getDistance() >= touchSlop
                                    ) {
                                        dragged = true
                                    }
                                    if (dragged) {
                                        updateSelection(
                                            (change.position.y / cellHeightPx)
                                                .toInt()
                                                .coerceIn(0, sections.lastIndex),
                                        )
                                    }
                                }
                            } finally {
                                clearSelection()
                            }
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                sections.forEach { section ->
                    AlphabetCell(
                        section = section,
                        selected = selectedSection == section,
                        size = cellSize,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlphabetIndicator(section: String?, visible: Boolean) {
    AnimatedVisibility(
        visible = visible && section != null,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                    shape = ContinuousRoundedRectangle(25.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = section.orEmpty(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun AlphabetCell(section: String, selected: Boolean, size: Dp) {
    // The rail always spans the full available height, so on a short window the cells
    // get small rather than the rail getting shorter. Type steps down to match.
    val fontSize = when {
        size < 8.dp -> 4.sp
        size < 12.dp -> 6.sp
        size < 16.dp -> 8.sp
        else -> 9.sp
    }
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = section,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = fontSize),
            color = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/**
 * Resolve a rail section to a list index.
 *
 * An empty section falls forward to the next populated one, so tapping "Q" in a library
 * with no Q artists lands on R rather than doing nothing. Past the end it falls back to
 * the last populated section instead of index 0, which is what makes a scrub to the
 * bottom of the rail reach the bottom of the list.
 */
fun findAlphabetTargetIndex(
    section: String,
    sectionIndexMap: Map<String, Int>,
    sections: List<String> = AlphabetSections,
): Int {
    if (sectionIndexMap.isEmpty()) return 0
    sectionIndexMap[section]?.let { return it }

    val requestedIndex = sections.indexOf(section)
    if (requestedIndex < 0) return 0

    sections
        .drop(requestedIndex + 1)
        .firstNotNullOfOrNull(sectionIndexMap::get)
        ?.let { return it }

    return sections.asReversed().firstNotNullOfOrNull(sectionIndexMap::get) ?: 0
}

/** Bucket a display title into one of [AlphabetSections]. */
fun alphabetSectionKey(title: String): String {
    val first = title.trimStart().firstOrNull() ?: return "#"
    return when {
        first.isDigit() -> "0"
        first in 'A'..'Z' -> first.toString()
        first in 'a'..'z' -> first.uppercaseChar().toString()
        else -> "#"
    }
}

/** Build the section -> first-index map a rail needs from an already-sorted list. */
fun <T> buildAlphabetSectionIndex(items: List<T>, titleOf: (T) -> String): Map<String, Int> {
    val map = LinkedHashMap<String, Int>()
    items.forEachIndexed { index, item ->
        map.putIfAbsent(alphabetSectionKey(titleOf(item)), index)
    }
    return map
}
