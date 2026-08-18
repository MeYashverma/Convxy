/*
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.convx.music.ui.component.backdrop.backdrops

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

// Matches the slideIn/fadeIn tween(200) duration every NavHost transition in
// MainActivity uses, plus a buffer for frame-delivery slop.
private const val NavTransitionFreezeWindowNs = 300_000_000L

/**
 * Time-boxed freeze for the app-level [layerBackdrop] across a screen-to-screen
 * navigation transition. [BackdropFreeze] only reacts to scroll gestures — a plain
 * nav transition (slide + fade between two screens) is neither a scroll nor a
 * fling, so it never froze, and the backdrop re-recorded every frame of every
 * navigation for the transition's whole duration (the same per-frame cost already
 * measured and fixed for scroll).
 *
 * Elapsed-time based rather than tied to a real "transition running" callback:
 * Navigation-Compose's `AnimatedContent` doesn't expose one at the `NavHost` call
 * site, but the transition duration here is a known fixed constant, so a timer is
 * sufficient rather than a fallback.
 */
class NavTransitionFreeze {
    private val startedAtNs = longArrayOf(0L)

    val frozen: () -> Boolean = {
        val started = startedAtNs[0]
        started != 0L && System.nanoTime() - started < NavTransitionFreezeWindowNs
    }

    fun markTransitionStarted() {
        startedAtNs[0] = System.nanoTime()
    }
}

/** Marks a transition started every time [currentRoute] changes. */
@Composable
fun rememberNavTransitionFreeze(currentRoute: String?): NavTransitionFreeze {
    val freeze = remember { NavTransitionFreeze() }
    LaunchedEffect(currentRoute) {
        freeze.markTransitionStarted()
    }
    return freeze
}
