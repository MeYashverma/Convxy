/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convxy.music.comments.soundcloud

import kotlin.math.abs

/**
 * A track the SoundCloud search endpoint offered as a possible match.
 *
 * Only the fields matching actually needs — the comments endpoint wants [id], the rest exist to
 * decide whether asking for it is justified.
 */
data class SoundCloudTrackCandidate(
    val id: String,
    val title: String,
    val uploaderName: String,
    val durationMs: Long,
    val permalinkUrl: String? = null,
    /** False when the uploader turned comments off, in which case there is nothing to fetch. */
    val commentable: Boolean? = null,
    val commentCount: Int? = null,
)

/**
 * Decides whether a SoundCloud search result is the same recording Convxy is playing.
 *
 * This mapping is the honest hard part of the feature. Convxy's tracks are YouTube/InnerTube ids,
 * local files, JioSaavn or Spotify imports — none of which SoundCloud has ever heard of — so the
 * only way onto its comments is to search by title/artist and then *prove* the hit is the same
 * recording. Getting that wrong is worse than getting nothing: it attaches strangers' reactions to
 * the wrong moment of the wrong song and presents them as real.
 *
 * So the bar is deliberately high and the fallback is deliberately null. A candidate has to match on
 * title AND uploader AND land inside a duration window; anything ambiguous returns nothing and the
 * UI says "no comments for this track", which is a true statement, instead of guessing.
 *
 * Pure functions over plain data — no client, no Context — so the thresholds are unit-tested.
 */
object SoundCloudTrackMatcher {

    /** Below this the match is not believed. 0.82 ≈ same title with a differing suffix or a typo. */
    const val MIN_TITLE_SIMILARITY = 0.82f

    /**
     * Duration window. Tight enough to reject a radio edit vs. an extended mix (which have genuinely
     * different comment timelines) and loose enough to absorb encoding padding and a differently
     * mastered upload. The larger of an absolute 8s or 4% of the track.
     */
    const val ABSOLUTE_DURATION_TOLERANCE_MS = 8_000L
    const val RELATIVE_DURATION_TOLERANCE = 0.04f

    /**
     * Words that decorate a title without changing which recording it is. Stripped before comparing,
     * because "Song (Official Video)" and "Song" are the same track and a raw string compare would
     * score them apart.
     */
    private val NOISE_TOKENS = setOf(
        "official", "video", "audio", "lyrics", "lyric", "visualizer", "visualiser",
        "hd", "hq", "4k", "8k", "remastered", "remaster", "remix", "edit", "version",
        "explicit", "clean", "mono", "stereo", "live", "acoustic", "instrumental",
        "mv", "m/v", "officialaudio", "officialvideo", "lyricvideo", "full", "track",
        "from", "the", "album", "single", "feat", "ft", "featuring", "with", "prod",
        "by", "vs", "and", "x",
    )

    /**
     * The best candidate for [title]/[artistNames]/[durationSeconds], or null when nothing clears the
     * bar. Ties break towards the closer duration, then the earlier search rank.
     */
    fun bestMatch(
        candidates: List<SoundCloudTrackCandidate>,
        title: String,
        artistNames: List<String>,
        durationSeconds: Int,
    ): SoundCloudTrackCandidate? {
        if (candidates.isEmpty()) return null
        val wantedTitle = normalizeTitle(title)
        if (wantedTitle.isEmpty()) return null
        val wantedArtists = artistNames.map { normalizeArtist(it) }.filter { it.isNotEmpty() }
        val wantedTokens = wantedArtists.flatMap { tokens(it) }.toSet()
        val durationMs = if (durationSeconds > 0) durationSeconds * 1000L else null

        var best: SoundCloudTrackCandidate? = null
        var bestScore = Float.MIN_VALUE
        var bestDurationDelta = Long.MAX_VALUE

        for (candidate in candidates) {
            val score = score(wantedTitle, wantedTokens, candidate, durationMs) ?: continue
            val delta = if (durationMs == null) 0L else abs(candidate.durationMs - durationMs)
            if (score > bestScore || (score == bestScore && delta < bestDurationDelta)) {
                best = candidate
                bestScore = score
                bestDurationDelta = delta
            }
        }
        return best
    }

    /**
     * Combined confidence in 0..1, or null when the candidate is disqualified outright.
     *
     * Title carries the weight; the uploader has to be *compatible* rather than identical, because
     * SoundCloud credits the uploading account, which for a label release is the label and for a
     * repost is whoever reposted it. Duration is a gate, not a score contribution — a wrong length is
     * a different recording no matter how perfect the strings look.
     */
    fun score(
        normalizedWantedTitle: String,
        normalizedWantedArtistTokens: Set<String>,
        candidate: SoundCloudTrackCandidate,
        wantedDurationMs: Long?,
    ): Float? {
        if (!durationCompatible(candidate.durationMs, wantedDurationMs)) return null

        val titleSimilarity = similarity(normalizedWantedTitle, normalizeTitle(candidate.title))
        if (titleSimilarity < MIN_TITLE_SIMILARITY) return null

        // An exact title match alone is enough — plenty of SoundCloud uploads carry no artist credit
        // in the uploader field at all, and rejecting those would reject most real matches.
        if (titleSimilarity >= 0.999f) return titleSimilarity

        val uploaderTokens = tokens(normalizeArtist(candidate.uploaderName)).toSet()
        val artistScore = when {
            normalizedWantedArtistTokens.isEmpty() || uploaderTokens.isEmpty() -> 0.5f
            else -> tokenOverlap(normalizedWantedArtistTokens, uploaderTokens)
        }
        // A complete artist mismatch cannot be rescued by a perfect title: two different artists
        // covering the same song have entirely different comment timelines.
        if (artistScore < 0.2f) return null

        return titleSimilarity * 0.75f + artistScore * 0.25f
    }

    /** True when [candidateDurationMs] is close enough to [wantedDurationMs] to be the same take. */
    fun durationCompatible(candidateDurationMs: Long, wantedDurationMs: Long?): Boolean {
        if (wantedDurationMs == null || wantedDurationMs <= 0L) return true // unknown → do not gate
        if (candidateDurationMs <= 0L) return true // provider omitted it → do not gate
        val tolerance = maxOf(
            ABSOLUTE_DURATION_TOLERANCE_MS,
            (wantedDurationMs * RELATIVE_DURATION_TOLERANCE).toLong(),
        )
        return abs(candidateDurationMs - wantedDurationMs) <= tolerance
    }

    /** The search query to send for a track: primary artist first, then the title. */
    fun searchQuery(title: String, artistNames: List<String>): String {
        val cleanedTitle = stripDecorations(title)
        val artist = artistNames.firstOrNull { it.isNotBlank() }?.let { stripDecorations(it) }
        return listOfNotNull(artist?.takeIf { it.isNotBlank() }, cleanedTitle.takeIf { it.isNotBlank() })
            .joinToString(" ")
            .trim()
    }

    // ── string normalisation ───────────────────────────────────────────────

    /** Removes bracketed/parenthesised decorations and trailing "- official …" clauses. */
    fun stripDecorations(raw: String): String {
        var out = raw
        // Balanced-group noise: (Official Video), [Remastered], 【MV】, "feat. X"
        out = out.replace(Regex("""\([^)]*\)"""), " ")
        out = out.replace(Regex("""\[[^]]*]"""), " ")
        out = out.replace(Regex("""【[^】]*】"""), " ")
        out = out.replace(Regex("""\{[^}]*}"""), " ")
        out = out.replace(Regex("""\s*\|\s*.*$"""), " ")
        out = out.replace(Regex("""\s*[-–—]\s*(official|audio|video|lyric|lyrics|visualizer|visualiser|remaster\w*|remix|live|acoustic|explicit|clean|hd|hq|4k).*$""", RegexOption.IGNORE_CASE), " ")
        out = out.replace(Regex("""\s*\b(feat\.?|ft\.?|featuring|with)\b.*$""", RegexOption.IGNORE_CASE), " ")
        return out.trim()
    }

    /** Lowercase, decorations removed, noise words dropped, punctuation collapsed to spaces. */
    fun normalizeTitle(raw: String): String =
        tokens(stripDecorations(raw).lowercase())
            .filter { it !in NOISE_TOKENS }
            .joinToString(" ")

    /** Lowercase, decorations removed, punctuation collapsed — noise words KEPT (they are names). */
    fun normalizeArtist(raw: String): String =
        tokens(stripDecorations(raw).lowercase()).joinToString(" ")

    /** Letter/digit runs, so "AC/DC" → [ac, dc] and "G-Dragon" → [g, dragon]. */
    fun tokens(raw: String): List<String> =
        raw.lowercase().split(Regex("""[^0-9a-z\u00C0-\u024F\u0400-\u04FF\u0370-\u03FF\u0590-\u05FF\u0600-\u06FF\u0900-\u097F\u3040-\u30FF\u4E00-\u9FFF\uAC00-\uD7AF]+"""))
            .filter { it.isNotEmpty() }

    /**
     * Sørensen–Dice over character bigrams: order-sensitive like a real title compare, but forgiving
     * of a dropped word or a transliteration difference, and cheap enough to run over 20 candidates.
     */
    fun similarity(a: String, b: String): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        if (a == b) return 1f
        val ga = bigrams(a)
        val gb = bigrams(b)
        if (ga.isEmpty() || gb.isEmpty()) return 0f
        val pool = gb.toMutableList()
        var hits = 0
        for (g in ga) {
            val at = pool.indexOf(g)
            if (at >= 0) {
                hits++
                pool.removeAt(at)
            }
        }
        return (2f * hits) / (ga.size + gb.size)
    }

    private fun bigrams(s: String): List<String> =
        if (s.length < 2) listOf(s) else (0..s.length - 2).map { s.substring(it, it + 2) }

    /** Jaccard over word tokens — used for artists, where word identity matters more than order. */
    fun tokenOverlap(a: Set<String>, b: Set<String>): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        val intersection = a.intersect(b).size
        val union = (a + b).size
        return if (union == 0) 0f else intersection.toFloat() / union.toFloat()
    }
}
