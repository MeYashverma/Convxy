# Vivi Music — Roadmap & Backlog

Branch: `feature/echo-parity`. Living doc. Ship incrementally, one commit per item, verify each.

Legend — effort: S (≤1 file / hours), M (few files / migration), L (subsystem / multi-day), XL (flagship).

---

## 0. Bugs (fix first)

| # | Bug | Hypothesis / root cause | Effort |
|---|-----|-------------------------|--------|
| B1 | **YT liked / synced playlists not showing in Library** | `DatabaseDao.kt:1234` synced-playlist query is `WHERE isEditable AND bookmarkedAt IS NOT NULL`. Synced YT playlists arrive with `bookmarkedAt = null` → filtered out. Also `LibraryPlaylistsViewModel.sync()` only runs when `ytmSync` pref true + logged in. Verify: log in, check DB rows vs. what renders. | S–M |
| B2 | Glass RenderNode crash inside NavHost (known, see memory) — audit any new glass surface samples a screen-local UNATTACHED backdrop | prevention rule when adding glass | S |
| B3 | Low-RAM / API<31 glass fallback path — confirm new animated glass respects `isGlassAllowed()` | guard | S |

**B1 first — user-reported, blocks trust in Library.**

---

## 1. User-requested features

### F1 — Custom home background image + blur (S–M)
- **What:** user picks image → rendered blurred/dimmed behind Home LazyColumn. Toggle + intensity slider in settings.
- **Reuse:** existing "blurred/dark scroll backgrounds" (recent commit), `backdrop/effects/blur`, DataStore pref pattern.
- **New:** `HomeBackgroundImageUriKey` + `HomeBackgroundBlurKey` prefs; `PhotoPicker` (native `ActivityResultContracts.PickVisualMedia`, no dep); render `AsyncImage` + blur layer in `HomeScreen`.
- **Verify:** pick image → shows blurred behind home; toggle off → gone; survives process death.

### F2 — Liquid-glass physics & bouncy micro-animations (L)
Split into shippable sub-items:
- **F2a — Spring press/wobble on glass items (S):** upgrade `bounceClick`/`combinedBounceClick` from `tween` scale to `spring` (Compose `spring(dampingRatio = MediumBouncy)`). Optional slight skew/rotation deform on press for "wobble". Single file `ModifierExt.kt`, benefits 56+ call sites free.
- **F2b — iOS-style bouncy overscroll (M):** custom `OverscrollEffect` (Compose 1.7+ `rememberOverscrollEffect` / `Modifier.overscroll`) with spring rubber-band. Apply to Home/Library lists.
- **F2c — Pull-down image zoom / parallax stretch (M):** header artwork scales & translates with negative scroll offset (`graphicsLayer` driven by list `firstVisibleItemScrollOffset`). iOS "rubber stretch" on album/playlist headers.
- **F2d — Micro-animations (S, ongoing):** shared-element-ish transitions, animated tab icon fills, spring nav-bar. Incremental.
- **Reuse:** `graphicsLayer`, existing `GooeyTransition`, `combinedBounceClick`. No new deps.
- **Verify:** press item → springy squish; overscroll top/bottom → rubber-band; pull down header → zoom, snaps back.

### F3 — Custom playlist cover image + description (M) — local + synced
- **What:** user sets a local cover image and a description on any playlist (local OR synced YT). Local override, no server write.
- **DB:** Room migration on `PlaylistEntity` — add `customCoverUri: String?` + `description: String?`. Bump DB version + migration.
- **Reuse:** existing `SetCustomThumbnailAction` / `RemoveCustomThumbnailAction` (already for remote thumb); `PhotoPicker` from F1.
- **UI:** edit sheet in `LocalPlaylistScreen` — image picker + multiline description field; render cover (customCoverUri ?: thumbnailUrl) + description in header.
- **Verify:** set cover+desc on local and on synced playlist → persists, survives sync (sync must not clobber `customCoverUri`/`description` — see `update(PlaylistEntity, PlaylistItem)` at `DatabaseDao.kt:1827`, preserve new cols).

### F4 — Tab bar redesign (M)
- **What:** restyle existing `FloatingTabBar` / nav — new shape, glass tint, SF-style animated icons, layout. No new destination.
- **Reuse:** `FloatingTabBar.kt`, `GlassEffect`, `AppleTokens`.
- **Depends on:** F2d micro-animations for icon transitions.
- **Verify:** visual + all destinations reachable; glass fallback on unsupported devices.

### F5 — Lossless audio player, online + offline (XL) — HIGH DEMAND
- **Source:** `monochrome.tf` = open-source TIDAL Hi-Fi web UI over a **hifi-api** backend. Quality tiers `HI_RES_LOSSLESS / LOSSLESS / HIGH / LOW`; FLAC. Fallbacks: Qobuz, Deezer (16-bit FLAC), Amazon Music. Backends get TIDAL-banned → site rotates ~10 mirrors.
- **Plan:**
  - **Online:** new provider module `losslessapi` (mirror `innertube` structure) — Ktor client, configurable list of base URLs tried in order (mirror rotation), search + track stream-URL resolve. Settings: enable lossless, quality tier, custom mirror list.
  - **Playback:** ExoPlayer already handles FLAC. Route lossless stream URL through existing `MusicService` / `YTPlayerUtils` resolver as an alternate source.
  - **Offline:** extend existing download manager to fetch + cache FLAC; store quality tier in `FormatEntity`.
  - **UI:** lossless badge on now-playing + quality indicator; per-source toggle.
- **Risks:** mirror instability (build retry/rotation + graceful fallback to YT); legal/ToS (document, keep opt-in, no bundled keys); large file sizes for offline.
- **Verify:** search → play a FLAC online; download → play offline with network off; mirror failover when one base URL 4xx/5xx.
- **Sub-phases:** F5a online search+play → F5b quality selector+badge → F5c offline download → F5d mirror rotation/resilience.

---

## 2. Competitor gap features (Echo Music + Metrolist)

Already have: EQ, sleep timer, skip silence, audio normalization, tempo/pitch, Listen Together, Spotify import, song recognition (≈Echo Find), Canvas artwork, home widget, synced lyrics + AI translate, downloads, background playback, Discord RPC, Last.fm, wrapped.

Missing:

| # | Feature | Source | Effort | Notes |
|---|---------|--------|--------|-------|
| C1 | **Odesli / song.link share** | Echo | S | one API call → cross-platform share link in share sheet |
| C2 | **Set song as ringtone** | Echo | S | `RingtoneManager` + write-settings permission |
| C3 | **Crossfade between tracks** | Echo | M | ExoPlayer gapless + fade; player setting |
| C4 | **Podcast support** | Echo | L | browse + play YTM podcasts; new sections + episode model |
| C5 | **Echo Brain — auto-queue "momentum"** | Echo | XL | on-device: analyze recent plays → auto-append aligned radio tracks to queue. Flagship differentiator. |
| C6 | **Dynamic Island now-playing** | Echo | M | Android island-style overlay; device-dependent |
| C7 | **Preset color palettes + AMOLED black theme** | Metrolist | S–M | 19 presets + true-black mode; extend `ThemeScreen` |

---

## 3. Suggested execution order

1. **B1** (playlist bug) — user-blocking.
2. **F1** (home bg) — cheap, self-contained, visible win.
3. **F2a** (spring bounce) — one file, upgrades 56 call sites.
4. **F3** (playlist cover + desc) — DB migration, concrete.
5. **C1 + C2 + C7** (Odesli, ringtone, palettes/AMOLED) — quick wins batch.
6. **F2b/F2c** (overscroll + pull-zoom) — polish pass.
7. **F4** (tab redesign) — after micro-animations land.
8. **F5** (lossless) — XL, its own milestone; do sub-phases F5a→F5d.
9. **C5** (Echo Brain) — XL flagship, last / parallel research.
10. Backlog: **C3** crossfade, **C4** podcasts, **C6** dynamic island.

---

## Sources
- [Echo Music](https://github.com/EchoMusicApp/Echo-Music)
- [Metrolist](https://github.com/metrolistgroup/metrolist)
- [monochrome.tf](https://monochrome.tf/) · [monochrome-music/monochrome](https://github.com/monochrome-music/monochrome) · [INSTANCES.md (mirror list)](https://github.com/monochrome-music/monochrome/blob/main/INSTANCES.md)
