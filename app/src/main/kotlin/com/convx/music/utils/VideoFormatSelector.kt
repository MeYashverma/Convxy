/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.utils

import com.music.innertube.models.response.PlayerResponse

/**
 * Picks the best progressive (muxed audio+video) format for full-video
 * playback.
 *
 * Muxed deliberately: the whole playback pipeline (cache, resolving data
 * source, single ExoPlayer track selector) works on one URL per song.
 * Adaptive video-only streams would need a second, synchronized audio track
 * — a much larger change for a quality bump muxed already covers.
 *
 * Ranks by resolution, then fps, then bitrate, and skips anything above
 * 1080p (progressive formats never exceed it in practice; the guard just
 * keeps a surprise 4K entry from destroying mobile data).
 */
fun selectMuxedVideoFormat(
    formats: List<PlayerResponse.StreamingData.Format>?,
): PlayerResponse.StreamingData.Format? =
    formats.orEmpty()
        .filter { format -> format.height != null && format.height in 1..1080 }
        .maxByOrNull { format ->
            (format.height ?: 0) * 1000 +
                (format.fps ?: 0) * 10 +
                (format.averageBitrate ?: format.bitrate) / 100_000
        }
