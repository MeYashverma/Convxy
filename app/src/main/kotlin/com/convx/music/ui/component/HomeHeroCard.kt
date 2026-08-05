/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.convx.music.R
import com.convx.music.ui.component.shapes.ContinuousRoundedRectangle
import com.convx.music.ui.theme.AppleTokens
import com.convx.music.ui.utils.bounceClick

/**
 * The "star of the day" card that opens Home.
 *
 * Deliberately cheap to draw: one cropped image, one gradient scrim and two pills — no
 * blur, no glass, no backdrop sampling. Home's frame cost is dominated by how many rich
 * rows are composed at once, so the hero has to earn its place by replacing several rows
 * of tiles, not by adding to them.
 */
@Composable
fun HomeHeroCard(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(ContinuousRoundedRectangle(AppleTokens.CardCornerLarge))
            .bounceClick(onClick = onClick),
    ) {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Scrim only over the lower half, so the artwork stays legible up top while the
        // title below it keeps contrast regardless of how bright the cover is.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.35f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.78f),
                    )
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(AppleTokens.Gutter),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(AppleTokens.ItemGap),
                modifier = Modifier.padding(top = AppleTokens.ItemGap),
            ) {
                HeroPill(
                    iconRes = R.drawable.play,
                    label = stringResource(R.string.play),
                    onClick = onPlay,
                )
                HeroPill(
                    iconRes = R.drawable.shuffle,
                    label = stringResource(R.string.shuffle),
                    onClick = onShuffle,
                )
            }
        }
    }
}

@Composable
private fun HeroPill(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.22f))
            .bounceClick(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            maxLines = 1,
        )
    }
}
