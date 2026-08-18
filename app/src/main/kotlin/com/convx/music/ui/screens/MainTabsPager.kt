/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.convx.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.convx.music.constants.DarkModeKey
import com.convx.music.constants.PureBlackKey
import com.convx.music.ui.screens.library.LibraryScreen
import com.convx.music.ui.screens.search.SearchScreen
import com.convx.music.ui.screens.settings.DarkMode
import com.convx.music.ui.screens.settings.SettingsScreen
import com.convx.music.utils.rememberEnumPreference
import com.convx.music.utils.rememberPreference

/**
 * Page order for [MainTabsPager] -- index into this list is the pager page index.
 * Matches the floating tab bar's own left-to-right order (Screens.MainScreens +
 * Settings appended, see MainActivity's navigationItems/floatingNavigationItems)
 * so a pager scroll slides the same direction the tapped icon actually sits in.
 */
val MainTabsScreens = listOf(
    Screens.Home,
    Screens.Search,
    Screens.ListenTogether,
    Screens.Library,
    Screens.Settings,
)

const val MainTabsRoute = "main_tabs"

/**
 * Hosts Home/Search/Library/ListenTogether/Settings as pages of one [HorizontalPager]
 * instead of five separate NavHost destinations. Switching between them becomes a
 * pager scroll -- no destination swap, no AnimatedContent transition, no backdrop
 * re-record -- while every screen still receives the exact same navController it
 * always did, so drilling into a detail screen (album, artist, a settings sub-page,
 * search results) from any of them is completely unchanged: it still goes through
 * the outer NavHost exactly as before.
 *
 * beyondViewportPageCount keeps every page composed at once rather than tearing one
 * down and rebuilding it on tab switch -- deliberately not adjacent-only, because
 * Convx's tab bar (unlike a plain swipe pager) promises a tab remembers exactly
 * where you left it even when you jump straight to a non-adjacent tab, not just a
 * neighboring one. userScrollEnabled is off: this bar has always been tap-only, and
 * several screens already use horizontal swipe for their own gestures (hero
 * carousels, swipe-to-remove) that a drag-to-switch-tabs pager would fight with.
 * animateScrollToPage still plays the same smooth slide on a tab tap either way.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainTabsPager(
    pagerState: PagerState,
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        beyondViewportPageCount = MainTabsScreens.size,
        userScrollEnabled = false,
    ) { page ->
        when (MainTabsScreens.getOrNull(page)) {
            Screens.Home -> HomeScreen(navController = navController, snackbarHostState = snackbarHostState)

            Screens.Search -> {
                val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = false)
                val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
                val isSystemInDarkTheme = isSystemInDarkTheme()
                val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
                    if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
                }
                val pureBlack = remember(pureBlackEnabled, useDarkTheme) {
                    pureBlackEnabled && useDarkTheme
                }
                SearchScreen(navController = navController, pureBlack = pureBlack)
            }

            Screens.Library -> LibraryScreen(navController)

            Screens.ListenTogether -> ListenTogetherScreen(navController, showTopBar = false)

            Screens.Settings -> SettingsScreen(navController, scrollBehavior)

            else -> Unit
        }
    }
}
