/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import com.music.vivi.ui.theme.AppleTokens
import com.music.vivi.ui.theme.LocalAccentTextColor
import com.music.vivi.ui.component.HeroBackground
import com.music.vivi.ui.utils.rememberHeroZoom
import com.music.vivi.ui.utils.heroPullZoom
import com.music.vivi.ui.utils.listOverscroll
import com.music.vivi.ui.component.HeroSource
import com.music.vivi.ui.component.LocalAppBackdrop
import com.music.vivi.ui.component.backdrop.backdrops.rememberLayerBackdrop
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.text.font.FontWeight
import com.music.vivi.ui.component.GlassCircleButton
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import com.music.vivi.ui.utils.bounceClick
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.music.vivi.utils.rememberPreference
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.NavigationTitle
import com.music.vivi.ui.component.shimmer.ListItemPlaceHolder
import com.music.vivi.ui.component.shimmer.ShimmerHost
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.viewmodels.MoodAndGenresViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodAndGenresScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: MoodAndGenresViewModel = hiltViewModel(),
) {
    val localConfiguration = LocalConfiguration.current
    val itemsPerRow = if (localConfiguration.orientation == ORIENTATION_LANDSCAPE) 3 else 2

    val moodAndGenresList by viewModel.moodAndGenres.collectAsState()

    val tint = Color.Black
    val onTint = AppleTokens.onColor(tint)

    val heroBackdrop = rememberLayerBackdrop()

    val heroZoom = rememberHeroZoom()

    HeroBackground(
        tint = tint,
        heroSource = HeroSource.Default,
        bottomGradient = true,
        heroScale = heroZoom.scale,
        modifier = Modifier.fillMaxSize(),
    ) {
      CompositionLocalProvider(
          LocalAppBackdrop provides heroBackdrop,
          LocalContentColor provides onTint,
          LocalAccentTextColor provides AppleTokens.onColorHeading(tint)
      ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                // No bounce here: the top pull drives the hero zoom instead.
                overscrollEffect = heroZoom.listOverscroll(),
                modifier = Modifier.heroPullZoom(heroZoom),
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            ) {
                item(key = "header") {
                    Spacer(Modifier.height(40.dp))
                    Text(
                        text = stringResource(R.string.mood_and_genres),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = onTint,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                    )
                }

                if (moodAndGenresList == null) {
                    item(key = "mood_and_genres_shimmer") {
                        ShimmerHost(
                            modifier = Modifier.animateItem()
                        ) {
                            repeat(8) {
                                ListItemPlaceHolder()
                            }
                        }
                    }
                }

                moodAndGenresList?.forEachIndexed { index, moodAndGenres ->
                    item(key = "mood_and_genres_section_$index") {
                        Column(
                            modifier = Modifier
                                .animateItem()
                                .padding(horizontal = 6.dp),
                        ) {
                            NavigationTitle(
                                title = moodAndGenres.title,
                            )
                            moodAndGenres.items.chunked(itemsPerRow).forEach { row ->
                                Row {
                                    row.forEach { item ->
                                        MoodAndGenresButton(
                                            title = item.title,
                                            stripeColor = item.stripeColor,
                                            onClick = {
                                                navController.navigate("youtube_browse/${item.endpoint.browseId}?params=${item.endpoint.params}")
                                            },
                                            modifier =
                                            Modifier
                                                .weight(1f)
                                                .padding(6.dp),
                                        )
                                    }

                                    repeat(itemsPerRow - row.size) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
                
                item {
                    Spacer(Modifier.height(100.dp))
                }
            }

            // Top bar logic
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassCircleButton(
                    onClick = { navController.navigateUp() },
                    onLongClick = { navController.backToMain() },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                    )
                }

                Spacer(Modifier.weight(1f))
            }
        }
      }
    }
}

@Composable
fun MoodAndGenresButton(
    title: String,
    stripeColor: Long? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (stripeColor != null) {
        Color(stripeColor).copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier =
        modifier
            .height(MoodAndGenresButtonHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .bounceClick(onClick = onClick)
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

val MoodAndGenresButtonHeight = 48.dp
