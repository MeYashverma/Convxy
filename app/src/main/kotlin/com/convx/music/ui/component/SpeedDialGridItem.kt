package com.convx.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.YTItem
import com.convx.music.R
import com.convx.music.ui.component.shapes.ContinuousRoundedRectangle
import com.convx.music.ui.theme.AppleTokens

@Composable
fun SpeedDialGridItem(
    item: YTItem,
    isPinned: Boolean,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    thumbnailSizePx: Int = 544,
    cornerRadiusDp: Int = 24,
) {
    // Apple Music's browse tiles read noticeably rounder than the app's general
    // 12dp thumbnail corner — bumped close to AppleTokens.CardCornerLarge (28dp)
    // for this tile's default. Corner radius + tile height are read once at the
    // Home grid level and passed in, so no per-tile DataStore subscription.
    val shape = ContinuousRoundedRectangle(cornerRadiusDp.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
    ) {
        // Thumbnail
        ItemThumbnail(
            thumbnailUrl = item.thumbnail,
            isActive = isActive,
            isPlaying = isPlaying,
            shape = if (item is ArtistItem) CircleShape else shape,
            modifier = Modifier.fillMaxSize(),
            targetSizePx = thumbnailSizePx,
            // Always fill the tile edge-to-edge, like Apple Music's browse tiles —
            // independent of the user's general CropAlbumArtKey preference, which
            // otherwise defaults to Fit and left the art visibly inset/letterboxed.
            forceContentScale = ContentScale.Crop,
            // No static paused-play glyph on the tile — just the animated bars
            // while it's actually playing.
            showPausedPlayIcon = false,
        )

        // Gradient Overlay for Text Readability and Icon Contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f), // Top scrim for icon visibility on bright covers
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f),
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        // Title and Chevron
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(AppleTokens.ItemGap / 2) // Reduced padding for tighter layout
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall, // Smaller, punchier font
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            // Navigation Chevron for browsable items (Album, Playlist, Artist)
            if (item !is SongItem) {
                Icon(
                    painter = painterResource(R.drawable.navigate_next),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
        }
    }
        // Pinned Icon
        if (isPinned) {
            Icon(
                painter = painterResource(R.drawable.ic_push_pin),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(AppleTokens.ItemGap / 2)
                    .size(16.dp)
            )
        }


    }
}
