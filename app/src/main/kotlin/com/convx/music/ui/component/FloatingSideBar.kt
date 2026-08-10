/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import coil3.compose.AsyncImage
import com.convx.music.R
import com.convx.music.ui.component.backdrop.backdrops.layerBackdrop
import com.convx.music.ui.component.backdrop.backdrops.rememberCombinedBackdrop
import com.convx.music.ui.component.backdrop.backdrops.rememberLayerBackdrop
import com.convx.music.ui.component.backdrop.catalog.utils.InteractiveHighlight
import com.convx.music.ui.component.backdrop.catalog.utils.DampedDragAnimation
import com.convx.music.ui.component.backdrop.drawBackdrop
import com.convx.music.ui.component.backdrop.effects.blur
import com.convx.music.ui.component.backdrop.effects.lens
import com.convx.music.ui.component.backdrop.effects.vibrancy
import com.convx.music.ui.component.backdrop.highlight.Highlight
import com.convx.music.ui.component.backdrop.shadow.InnerShadow
import com.convx.music.ui.component.backdrop.shadow.Shadow
import com.convx.music.ui.component.shapes.ContinuousRoundedRectangle
import com.convx.music.ui.player.FloatingMiniPlayer
import com.convx.music.ui.screens.Screens
import com.convx.music.ui.theme.BrandName
import com.convx.music.ui.theme.rememberBrandFontFamily
import com.convx.music.ui.utils.fadingEdge

/** Panel corner. A tall panel, so a rounded rect rather than the phone bar's capsule. */
private val SideBarShape = ContinuousRoundedRectangle(28.dp)

/** Row/puck corner inside the panel: a full capsule. */
private val SideRowShape = ContinuousRoundedRectangle(percent = 50)

/** Width of the whole floating panel. */
val SideBarWidth: Dp = 260.dp

/** Width when collapsed to an icon-only rail — just enough for the icon + padding. */
val SideBarCollapsedWidth: Dp = 72.dp

/**
 * Container width at or above which the app lays itself out for tab view on its
 * own. Below it the side bar only appears if the Appearance toggle forces it.
 */
val TabletWidthThreshold: Dp = 840.dp

/** Panel inset from the screen edge, and the gap it leaves to the content. */
val SideBarMargin: Dp = 12.dp

/**
 * Horizontal space content should reserve on the start edge.
 *
 * Applied as *content padding*, never as a layout inset: rows begin clear of the
 * panel at rest but the list still spans the full width, so scrolling carries
 * them under the glass rather than clipping them short of it.
 */
val SideBarContentInset: Dp = SideBarWidth + SideBarMargin * 2

/**
 * Rim and shadow the puck keeps when it is NOT pressed. Driving both straight
 * off pressProgress leaves the puck shapeless at rest, which is exactly how it
 * vanishes into the panel over dark content.
 */
private const val PuckRestHighlightAlpha = 0.5f
private const val PuckRestShadowAlpha = 0.35f

private val SideTabHeight = 48.dp
private val SideBarContentPadding = PaddingValues(6.dp)

/**
 * Fixed leading slot every row's icon/artwork/avatar is centred in, so the labels
 * all start at the same x. Without it a 22dp icon row and a 24dp artwork row put
 * their text 2dp apart, which is exactly what reads as "the icons don't line up".
 */
private val SideRowLeadingSlot = 24.dp
private val SideRowIconSize = 22.dp

/**
 * The free-floating now playing pill in tab view: 80% of the width left over
 * after the side bar, centred in it, with the height following from that width
 * so it keeps its proportions on any screen.
 */
const val FloatingMiniPlayerWidthFraction = 0.8f

/** Height as a share of the pill's own width, clamped to stay a bar. */
private const val FloatingMiniPlayerAspect = 0.11f
private val FloatingMiniPlayerMinHeight = 64.dp
private val FloatingMiniPlayerMaxHeight = 64.dp

/** A non-tab destination in the side bar: history, stats, a playlist, and so on. */
@Immutable
data class SideBarLink(
    val label: String,
    val iconRes: Int? = null,
    /** Playlist/album art, shown instead of [iconRes] when set. */
    val thumbnailUrl: String? = null,
    val onClick: () -> Unit,
)

@Immutable
data class SideBarSection(
    /** null renders the rows with no heading. */
    val title: String? = null,
    val links: List<SideBarLink>,
)

@Immutable
private data class SideTab(
    val screen: Screens,
    val selected: Boolean,
    val onClick: () -> Unit,
)

/**
 * The tab-view side bar: a full-height floating glass panel.
 *
 * Three zones, as in the reference. The wordmark is pinned at the top and the
 * account/settings row at the bottom; everything between them scrolls, and fades
 * out under both so rows slide beneath rather than being cut off.
 *
 * The primary tab group inside it is the phone's floating nav bar rebuilt on the
 * vertical axis — same [GlassEffectConfig] glass, the same glass selection puck,
 * the same [InteractiveHighlight] finger glow, and the same draggable puck, on
 * the vertical axis.
 *
 * The panel floats OVER the content: nothing reserves layout width for it, so
 * screens run full width and scroll underneath its glass.
 */
@Composable
fun AppFloatingSideBar(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    sections: List<SideBarSection>,
    footer: @Composable (collapsed: Boolean) -> Unit,
    collapsed: Boolean,
    onToggleCollapsed: () -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
) {
    val glassConfig = LocalGlassEffectConfig.current
    val useGlass = glassConfig.isEnabledFor(GlassComponent.SIDE_PANEL) && isGlassAllowed()
    val animationScope = rememberCoroutineScope()
    val targetPanelWidth = if (collapsed) SideBarCollapsedWidth else SideBarWidth
    val panelWidth by animateDpAsState(
        targetValue = targetPanelWidth,
        animationSpec = spring(0.9f, 400f),
        label = "sideBarWidth",
    )

    // Same finger-tracking glow the mini player carries: it follows the touch
    // across the whole panel, so every row lights up under the finger, not just
    // the tab group.
    // radiusScale is much smaller than the default 1.5f: minDimension here is the
    // panel's full width, not a row's — at the default scale the glow bloom
    // covered several rows and buried the icon/label under the finger.
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope, radiusScale = 0.3f)
    }

    val backgroundColor = when {
        useGlass -> Color.Transparent
        pureBlack -> Color.Black
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    // Collapsed rail is narrow and tall — a full capsule (same shape as the
    // tab rows/puck) reads as a pill, where the expanded panel's fixed 28dp
    // corner radius would look like a barely-rounded rectangle instead.
    val panelShape = if (collapsed) SideRowShape else SideBarShape
    val panelSurface = if (useGlass) {
        Modifier.liquidGlass(
            config = glassConfig.forSidePanel(),
            shape = panelShape,
            highlightAlpha = 0.3f,
            // The fold is a width animation on the app's largest glass surface:
            // re-capturing and re-blurring the whole screen on every frame of it
            // is what makes the collapse stutter. Hold the last capture until the
            // width settles — the panel is moving, so a frame-old backdrop behind
            // it is not perceptible.
            frozen = { panelWidth != targetPanelWidth },
        )
    } else {
        Modifier
    }

    Column(
        modifier
            .width(panelWidth)
            .shadow(shape = panelShape, elevation = 10.dp)
            .background(backgroundColor, panelShape)
            .clip(panelShape)
            .then(panelSurface)
            .then(interactiveHighlight.modifier)
            .then(interactiveHighlight.gestureModifier)
            .padding(vertical = 14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (collapsed) 0.dp else 22.dp, bottom = 10.dp),
            horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.SpaceBetween,
        ) {
            if (!collapsed) {
                Text(
                    text = BrandName,
                    fontFamily = rememberBrandFontFamily(),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    letterSpacing = 1.5.sp,
                    color = glassConfig.textColor,
                )
            }
            Icon(
                painter = painterResource(if (collapsed) R.drawable.chevron_right_px else R.drawable.chevron_leftpx),
                contentDescription = stringResource(
                    if (collapsed) R.string.expand_side_panel else R.string.collapse_side_panel
                ),
                tint = glassConfig.textColor.copy(alpha = 0.7f),
                modifier = Modifier
                    .padding(end = if (collapsed) 0.dp else 16.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleCollapsed),
            )
        }

        // Pinned above the scroll area, not inside it. The puck is dragged
        // vertically and a scrollable ancestor consumes exactly that gesture —
        // inside the scroll column the drag was stolen the moment it started.
        SideNavTabs(
            navigationItems = navigationItems,
            currentRoute = currentRoute,
            onItemClick = onItemClick,
            useGlass = useGlass,
            backgroundColor = backgroundColor,
            collapsed = collapsed,
            modifier = Modifier.padding(horizontal = 6.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                // Rows dissolve into the pinned wordmark and footer instead of
                // being clipped by a hard edge.
                .fadingEdge(vertical = 18.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 6.dp),
        ) {
            // The library sections (playlists, history, ...) need a text label to
            // mean anything — an icon-only rail collapses to just the primary tabs,
            // same as NavigationRail never showing arbitrary link lists either.
            if (!collapsed) {
                sections.forEach { section ->
                    section.title?.let { title ->
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = glassConfig.textColor.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 4.dp),
                        )
                    }
                    section.links.forEach { link ->
                        SideBarLinkRow(link = link, contentColor = glassConfig.textColor)
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        footer(collapsed)
    }
}

/**
 * The phone's floating nav bar, stood on its end — drag puck included.
 *
 * Same [DampedDragAnimation] the phone bar uses, on the vertical axis: the puck
 * can be grabbed and dragged down the rail, grows while pressed, springs to the
 * row it is dropped on, and only then navigates. Tapping a row still works and
 * moves the puck the same way.
 */
@Composable
private fun SideNavTabs(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    useGlass: Boolean,
    backgroundColor: Color,
    collapsed: Boolean,
    modifier: Modifier = Modifier,
) {
    // Deliberately the global config, not forSidePanel(): the tab group is the
    // phone nav bar stood on its end, so its puck answers to the same "normal
    // liquid glass" settings the phone bar's does. Only the PANEL behind it uses
    // the side-panel tuning.
    val glassConfig = LocalGlassEffectConfig.current
    val appleMusicUi = LocalAppleMusicUi.current

    val selectedContentColor = glassConfig.textColor
    val unselectedContentColor = glassConfig.textColor.copy(alpha = 0.75f)
    val accentColor = selectedContentColor

    // Same wash the phone puck uses: the configured colour, or one that follows
    // the theme rather than assuming a dark bar.
    val puckWash = if (glassConfig.puckColor.isSpecified) {
        glassConfig.puckColor
    } else if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
        Color(0xFFF2F2F2)
    } else {
        Color(28, 27, 28)
    }
    val puckRestAlpha = glassConfig.puckOpacity.coerceIn(0f, 1f)

    // Puck + tint follow the sticky selection so a non-tab destination holds the
    // last tab instead of snapping to Home; the click passes the REAL route state
    // so tapping a held tab from a sub-screen still navigates to it.
    val selectedRoute = rememberStickySelectedRoute(currentRoute, navigationItems)
    val tabs = navigationItems.map { screen ->
        SideTab(
            screen = screen,
            selected = screen.route == selectedRoute,
            onClick = { onItemClick(screen, isRouteSelected(currentRoute, screen.route, navigationItems)) },
        )
    }
    val tabsCount = tabs.size
    if (tabsCount == 0) return

    val density = LocalDensity.current
    val backdrop = if (useGlass) LocalAppBackdrop.current else null
    val tabHeightPx = with(density) { SideTabHeight.toPx() }

    val selectedIndex = tabs.indexOfFirst { it.selected }.coerceAtLeast(0)
    val animationScope = rememberCoroutineScope()

    // The tab list changes identity every recomposition; the drag callbacks are
    // remembered once, so they read the current list through this holder rather
    // than capturing a stale one (same trick the phone bar uses).
    val currentTabs = remember { mutableStateOf(tabs) }
    currentTabs.value = tabs

    // The puck's position, in tab indices. Dragging updates it live; navigation
    // fires only when the finger lifts and it settles on a row.
    val puck = remember(animationScope, tabsCount) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedIndex.toFloat(),
            valueRange = 0f..(tabsCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            // The phone bar's ratio, kept as a ratio so the grow reads the same
            // at this row height as it does at that tab width.
            pressedScale = 78f / 56f,
            onDragStarted = {},
            onDragStopped = {
                val targetIndex = targetValue.fastRoundToInt().coerceIn(0, tabsCount - 1)
                updateValue(targetIndex.toFloat())
                currentTabs.value.getOrNull(targetIndex)?.onClick?.invoke()
            },
            onDrag = { _, dragAmount ->
                updateValue(
                    (targetValue + dragAmount.y / tabHeightPx)
                        .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                )
            },
        )
    }
    // Keeps the puck on the selected row when selection changes from a tap or
    // from navigation happening elsewhere.
    var hasSyncedSelection by remember(tabsCount) { mutableStateOf(false) }
    LaunchedEffect(selectedIndex, tabsCount) {
        if (!hasSyncedSelection) {
            hasSyncedSelection = true
            puck.updateValue(selectedIndex.toFloat())
            return@LaunchedEffect
        }
        // animateToValue, so a tap gets the same press-grow-and-settle a drop does.
        puck.animateToValue(selectedIndex.toFloat())
    }

    // Invisible accent-tinted copy of the tabs, sampled back through the puck's
    // glass so the selected row shows inside it.
    val tabsBackdrop = rememberLayerBackdrop()

    // Which row the puck currently covers. Derived state, so crossing into a new
    // row recomposes the overlay instead of every frame of a drag doing it.
    val puckIndex by remember(tabsCount) {
        derivedStateOf { puck.value.fastRoundToInt().coerceIn(0, tabsCount - 1) }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(SideTabHeight * tabsCount + SideBarContentPadding.calculateTopPadding() * 2)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(SideBarContentPadding),
        ) {
            tabs.forEachIndexed { index, tab ->
                SideTabRow(
                    tab = tab,
                    appleMusicUi = appleMusicUi,
                    contentColor = if (tab.selected) selectedContentColor else unselectedContentColor,
                    alpha = if (tab.selected) 1f else 0.6f,
                    collapsed = collapsed,
                    contentScale = { if (index == puckIndex) lerp(1f, 1.12f, puck.pressProgress) else 1f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SideTabHeight)
                        .clip(SideRowShape)
                        .tapClickable(onClick = tab.onClick),
                )
            }
        }

        if (backdrop != null) {
            val tabsBackdropScale = glassResolutionScale(1f)
            Column(
                Modifier
                    .fillMaxSize()
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .padding(SideBarContentPadding)
                    // Not a flat tinted cutout: a real blurred, lensed sample of
                    // what sits behind the panel. That is what makes the puck read
                    // as cut from the panel's own material instead of as a third
                    // sharp copy of the row stacked over it.
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { SideRowShape },
                        effects = {
                            val progress = puck.pressProgress
                            vibrancy()
                            blur(glassConfig.blurRadius.dp.toPx() * tabsBackdropScale)
                            lens(
                                15f.dp.toPx() * progress * tabsBackdropScale,
                                18f.dp.toPx() * progress * tabsBackdropScale,
                            )
                        },
                        highlight = { Highlight.Default.copy(alpha = puck.pressProgress) },
                        onDrawSurface = { drawRect(backgroundColor) },
                        backdropScale = tabsBackdropScale,
                    )
                    .graphicsLayer(colorFilter = ColorFilter.tint(accentColor)),
            ) {
                tabs.forEachIndexed { index, tab ->
                    SideTabRow(
                        tab = tab,
                        appleMusicUi = appleMusicUi,
                        contentColor = unselectedContentColor,
                        alpha = 1f,
                        collapsed = collapsed,
                        // Must track the visible row's zoom exactly: this is what the
                        // puck samples, so drift shows up as a ghosted double icon.
                        contentScale = { if (index == puckIndex) lerp(1f, 1.12f, puck.pressProgress) else 1f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(SideTabHeight),
                    )
                }
            }
        }

        // The selection puck, same recipe as the phone bar's: refraction strongest
        // at rest, easing back while pressed so the moving puck stays legible.
        Box(
            Modifier
                .padding(SideBarContentPadding)
                .graphicsLayer { translationY = puck.value * tabHeightPx }
                .fillMaxWidth()
                .height(SideTabHeight)
                .then(puck.modifier)
                .then(
                    if (backdrop != null) {
                        Modifier.drawBackdrop(
                            backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                            shape = { SideRowShape },
                            effects = {
                                val progress = puck.pressProgress
                                blur(3f.dp.toPx() * (1f - progress))
                                lens(
                                    lerp(32f.dp.toPx(), 4f.dp.toPx(), progress),
                                    lerp(28f.dp.toPx(), 6f.dp.toPx(), progress),
                                    chromaticAberration = true,
                                )
                            },
                            highlight = {
                                Highlight.Default.copy(
                                    alpha = lerp(PuckRestHighlightAlpha, 1f, puck.pressProgress)
                                )
                            },
                            shadow = { Shadow(alpha = lerp(PuckRestShadowAlpha, 1f, puck.pressProgress)) },
                            innerShadow = {
                                InnerShadow(radius = 8f.dp * puck.pressProgress, alpha = puck.pressProgress)
                            },
                            layerBlock = {
                                scaleX = puck.scaleX
                                scaleY = puck.scaleY
                                // Squash along the axis of travel, which is Y here.
                                val velocity = puck.velocity / 10f
                                scaleY /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                                scaleX *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                            },
                            onDrawSurface = {
                                // Lands ON TOP of the sampled icon, so it can only ever
                                // be a wash - anything opaque crushes what shows through.
                                drawRect(
                                    puckWash.copy(
                                        alpha = puckRestAlpha * (1f - 0.75f * puck.pressProgress)
                                    )
                                )
                            },
                        )
                    } else {
                        Modifier
                            .graphicsLayer {
                                scaleX = puck.scaleX
                                scaleY = puck.scaleY
                            }
                            .shadow(shape = SideRowShape, elevation = 6.dp * puck.pressProgress)
                            .background(
                                backgroundColor.copy(alpha = 0.5f + 0.5f * puck.pressProgress),
                                SideRowShape,
                            )
                            .clip(SideRowShape)
                    }
                )
        )

        // The selected row redrawn above the puck, and ONLY while settled. What
        // shows through the puck is the sampled copy, which the wash necessarily
        // dims; this keeps it crisp at rest. It fades about three times faster
        // than the press ramp, so a drag shows two layers, never three.
        Box(
            Modifier
                .padding(SideBarContentPadding)
                .graphicsLayer {
                    translationY = puck.value * tabHeightPx
                    scaleX = puck.scaleX
                    scaleY = puck.scaleY
                    alpha = (1f - puck.pressProgress * 3f).fastCoerceIn(0f, 1f)
                }
                .fillMaxWidth()
                .height(SideTabHeight)
                .clearAndSetSemantics {},
        ) {
            tabs.getOrNull(puckIndex)?.let { tab ->
                SideTabRow(
                    tab = tab,
                    appleMusicUi = appleMusicUi,
                    contentColor = selectedContentColor,
                    alpha = 1f,
                    collapsed = collapsed,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/**
 * The now playing pill for tab view: the same [FloatingMiniPlayer] the phone bar
 * docks as its accessory, on the same glass and the same capsule, floating free
 * in the content rather than attached to anything.
 *
 * Place it inside a container that has already padded out [SideBarContentInset]
 * — it fills that container's width by [FloatingMiniPlayerWidthFraction] and
 * centres itself in it, so "centred" means the content area, not the screen.
 */
@Composable
fun BoxWithConstraintsScope.AppFloatingNowPlayingPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    onLyricsClick: (() -> Unit)? = null,
    onQueueClick: (() -> Unit)? = null,
) {
    val glassConfig = LocalGlassEffectConfig.current
    val useGlass = glassConfig.isEnabledFor(GlassComponent.SIDE_PANEL) && isGlassAllowed()
    val pillShape = ContinuousRoundedRectangle(percent = 50)

    val width = maxWidth * FloatingMiniPlayerWidthFraction
    val height = (width * FloatingMiniPlayerAspect)
        .coerceIn(FloatingMiniPlayerMinHeight, FloatingMiniPlayerMaxHeight)

    val background = when {
        useGlass -> Color.Transparent
        pureBlack -> Color.Black
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val surface = if (useGlass) {
        // This is the mini player pill, not the side panel — it uses the
        // global glass settings directly, same as the phone's docked
        // accessory. (forSidePanel() here was a bug: dialing the side panel
        // rail's glass was leaking into this pill too.)
        Modifier.liquidGlass(config = glassConfig, shape = pillShape, highlightAlpha = 0.3f)
    } else {
        Modifier.background(background, pillShape)
    }

    FloatingMiniPlayer(
        isInline = false,
        // See AppNavigation: hardcoded white disappeared on light themes.
        contentColor = if (useGlass) glassConfig.textColor else MaterialTheme.colorScheme.onSurface,
        onClick = onClick,
        onLyricsClick = onLyricsClick,
        onQueueClick = onQueueClick,
        modifier = modifier
            .width(width)
            .height(height)
            .shadow(shape = pillShape, elevation = 10.dp)
            .clip(pillShape)
            .then(surface),
    )
}

/**
 * The pinned bottom row: the account avatar when signed in, otherwise a settings
 * cog. Same destination as the phone top bar's trailing button.
 */
@Composable
fun SideBarAccountRow(
    accountImageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    collapsed: Boolean = false,
) {
    val contentColor = LocalGlassEffectConfig.current.textColor
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .height(SideTabHeight)
            .clip(SideRowShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
    ) {
        Box(
            modifier = Modifier.size(SideRowLeadingSlot),
            contentAlignment = Alignment.Center,
        ) {
            if (accountImageUrl != null) {
                AsyncImage(
                    model = accountImageUrl,
                    contentDescription = stringResource(R.string.account),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.settings),
                    contentDescription = stringResource(R.string.settings),
                    tint = contentColor,
                    modifier = Modifier.size(SideRowIconSize),
                )
            }
        }
        if (!collapsed) {
            Spacer(Modifier.width(14.dp))
            Text(
                text = stringResource(
                    if (accountImageUrl != null) R.string.account else R.string.settings
                ),
                color = contentColor,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SideTabRow(
    tab: SideTab,
    appleMusicUi: Boolean,
    contentColor: Color,
    alpha: Float,
    modifier: Modifier = Modifier,
    collapsed: Boolean = false,
    // A lambda, not a value: read in the draw phase so a press does not
    // recompose the row on every frame.
    contentScale: () -> Float = { 1f },
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.Start,
        modifier = modifier
            .graphicsLayer {
                this.alpha = alpha
                val scale = contentScale()
                scaleX = scale
                scaleY = scale
            }
            .padding(horizontal = if (collapsed) 0.dp else 16.dp),
    ) {
        Box(
            modifier = Modifier.size(SideRowLeadingSlot),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(
                    tab.screen.icon(appleMusicUi)
                ),
                contentDescription = stringResource(tab.screen.titleId),
                tint = contentColor,
                modifier = Modifier.size(SideRowIconSize),
            )
        }
        if (!collapsed) {
            Spacer(Modifier.width(14.dp))
            Text(
                text = stringResource(tab.screen.titleId),
                color = contentColor,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SideBarLinkRow(link: SideBarLink, contentColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(SideTabHeight)
            .clip(SideRowShape)
            .clickable(onClick = link.onClick)
            .padding(horizontal = 16.dp),
    ) {
        Box(
            modifier = Modifier.size(SideRowLeadingSlot),
            contentAlignment = Alignment.Center,
        ) {
            when {
                link.thumbnailUrl != null -> AsyncImage(
                    model = link.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(ContinuousRoundedRectangle(6.dp)),
                )

                link.iconRes != null -> Icon(
                    painter = painterResource(link.iconRes),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(SideRowIconSize),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = link.label,
            color = contentColor,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Release-triggered [clickable], same as the phone bar's: firing on finger-DOWN
 * navigates even when the touch turns into a puck drag or a scroll.
 */
@Composable
private fun Modifier.tapClickable(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return clickable(
        onClick = onClick,
        indication = null,
        interactionSource = interactionSource,
    )
}
