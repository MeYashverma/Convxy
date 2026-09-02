/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.component

import android.annotation.SuppressLint
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.convx.music.R
import com.convx.music.ui.screens.OptionStats

/**
 * Hairline edge for a glass chip, derived from what the chip is actually filled
 * with. Callers over a blurred hero pass transparent/tint colors — see
 * [ChipsRow] — and a translucent chip on a dark hero needs a light edge while
 * the same chip on a light surface needs a dark one, so the edge follows the
 * fill's own luminance rather than the theme's.
 */
@Composable
private fun rememberChipEdgeColor(container: Color): Color {
    val themeDark = MaterialTheme.colorScheme.surface.luminance() <= 0.5f
    return remember(container, themeDark) {
        val luminance = container.luminance()
        when {
            // Effectively transparent fill: the chip shows whatever is behind
            // it, which on this app is almost always a dark hero wash — a light
            // edge reads on both themes there. (A light-mode hero shows the
            // hairline faintly; the selected state still carries the accent
            // fill, so the control never loses its state signal.)
            container.alpha <= 0.05f -> Color.White.copy(alpha = 0.28f)
            luminance > 0.55f -> Color.Black.copy(alpha = 0.16f)
            else -> Color.White.copy(alpha = 0.22f)
        }
    }
}

/** The one chip silhouette in the app: a full pill on FilterChip's 32dp height. */
private val ChipShape = RoundedCornerShape(16.dp)

@Composable
fun <E> ChipsRow(
    chips: List<Pair<E, String>>,
    currentValue: E,
    onValueUpdate: (E) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    // Callers sitting over their own blurred hero backdrop (search results, over
    // a hero blur) pass transparent/tint-based colors here instead of the
    // Material defaults, so the chips read as part of the glass, not a card.
    selectedContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    labelColor: Color = Color.Unspecified,
    selectedLabelColor: Color = Color.Unspecified,
) {
    // The unselected fill is translucent so the chips sit IN the page instead
    // of on it — the same distinction the rest of the glass language makes
    // between chrome and content. Selected chips fill with the accent and drop
    // the edge: a filled glass chip catches its own light.
    val unselectedFill = remember(containerColor) {
        containerColor.copy(alpha = containerColor.alpha * 0.72f)
    }
    val edgeColor = rememberChipEdgeColor(containerColor)

    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
    ) {
        Spacer(Modifier.width(12.dp))

        chips.forEach { (value, label) ->
            val isSelected = currentValue == value

            FilterChip(
                label = { Text(label) },
                selected = isSelected,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = if (isSelected) selectedContainerColor else unselectedFill,
                    selectedContainerColor = selectedContainerColor,
                    labelColor = labelColor,
                    selectedLabelColor = selectedLabelColor,
                ),
                onClick = { onValueUpdate(value) },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = null,
                            tint = selectedLabelColor,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    }
                } else {
                    null
                },
                // One pill shape for both states. The old look morphed an 8dp
                // square into a 20dp pill on selection — the radius race drew
                // the eye to the shape instead of to the state change the check
                // icon and fill already announce.
                shape = ChipShape,
                border = if (isSelected) {
                    null
                } else {
                    BorderStroke(GlassPanelEdgeStroke, edgeColor)
                },
                modifier = Modifier.animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            )

            Spacer(Modifier.width(8.dp))
        }
    }
}

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
fun <Int> ChoiceChipsRow(
    chips: List<Pair<Int, String>>,
    options: List<Pair<OptionStats, String>>,
    selectedOption: OptionStats,
    onSelectionChange: (OptionStats) -> Unit,
    currentValue: Int,
    onValueUpdate: (Int) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    var expandIconDegree by remember { mutableFloatStateOf(0f) }
    val rotationAnimation by animateFloatAsState(
        targetValue = expandIconDegree,
        animationSpec = tween(durationMillis = 400),
        label = "rotation",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
        ) {
            Spacer(Modifier.width(12.dp))
            val unselectedFill = remember(containerColor) {
                containerColor.copy(alpha = containerColor.alpha * 0.72f)
            }
            val edgeColor = rememberChipEdgeColor(containerColor)

            Box(contentAlignment = Alignment.Center) {
                FilterChip(
                    selected = false,
                    modifier = Modifier
                        .padding(horizontal = 4.dp),
                    onClick = {
                        menuExpanded = !menuExpanded
                        expandIconDegree -= 180
                    },
                    label = {
                        Text(
                            text = when (selectedOption) {
                                OptionStats.WEEKS -> stringResource(id = R.string.weeks)
                                OptionStats.MONTHS -> stringResource(id = R.string.months)
                                OptionStats.YEARS -> stringResource(id = R.string.years)
                                OptionStats.CONTINUOUS -> stringResource(id = R.string.continuous)
                            }
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    },
                    trailingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.expand_more),
                            contentDescription = null,
                            modifier = Modifier
                                .graphicsLayer(rotationZ = rotationAnimation)
                        )
                    },
                    shape = ChipShape,
                    border = BorderStroke(GlassPanelEdgeStroke, edgeColor),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = unselectedFill,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = {
                        menuExpanded = false
                        expandIconDegree += 180
                    },
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(text = option.second) },
                            onClick = {
                                onSelectionChange(option.first)
                                expandIconDegree += 180
                                menuExpanded = false
                            },
                        )
                    }
                }
            }

            Box(
                Modifier
                    .height(FilterChipDefaults.Height)
                    .padding(horizontal = 4.dp)
                    .align(Alignment.CenterVertically)
            ) {
                VerticalDivider()
            }

            chips.forEach { (value, label) ->
                val isSelected = currentValue == value

                FilterChip(
                    label = { Text(label) },
                    selected = isSelected,
                    // Selected keeps FilterChipDefaults' own secondary-container
                    // fill, exactly as this row used before — only the resting
                    // chip becomes translucent glass with a hairline edge.
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = unselectedFill,
                    ),
                    onClick = { onValueUpdate(value) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Done,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        }
                    } else {
                        null
                    },
                    shape = ChipShape,
                    border = if (isSelected) {
                        null
                    } else {
                        BorderStroke(GlassPanelEdgeStroke, edgeColor)
                    },
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                )
            }
        }
    }
}
