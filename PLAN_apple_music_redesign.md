# Plan of Action — Apple-Music redesign (default look) + carryover

Target reader: the implementing AI. This is the authoritative build spec. Everything here is **the new default** — not gated behind a toggle. Follow the STRICT RULES per phase and the GLOBAL RULES at the end, or unrelated features WILL break.

Build order is P0 → P8. Each phase is independently shippable. Do not start a phase until the one it depends on is done and verified on-device (the repo owner builds manually — you do NOT run Gradle).

---

## Locked decisions (do not re-litigate)

1. **Apple look is the app's default**, everywhere. Remove the "Apple Music UI" opt-in toggle; bake its values as unconditional defaults. Material tonal look is replaced.
2. **Accent**: default = Apple red `#FA2D48`. Still user-changeable in Theme (red is just the default seed). Keep the existing color-picker.
3. **Dark surfaces**: background `#000000`/`#0B0B0B`, card/sheet `#1C1C1E`, secondary `#2C2C2E`.
4. **Buttons**:
   - **Toggle controls everywhere** use Kyant's vendored catalog components — ALREADY in repo at `ui/component/backdrop/catalog/components/` (currently unused, "vendored for later use"):
     - Boolean on/off **switches** (settings switches, any true/false slider) → **`LiquidToggle(selected, onSelect, backdrop)`** — the Apple glass switch (damped-drag track color lerp + lens).
     - **Icon toggle buttons** (like/favorite, shuffle, repeat, subscribe, follow) → **`LiquidButton(onClick, backdrop) { Icon(...) }`** — the glass capsule w/ interactive highlight, showing the toggled/untoggled OUR icon + accent tint when on.
     - Use these catalog components for toggles ONLY. Do NOT use them for plain (non-toggle) buttons.
   - Non-toggle **Home** action buttons/icons → solid dark-grey Apple buttons (`#2C2C2E`), NOT glass.
   - Detail-screen chrome (back/share/play) → keep the existing **glass** `GlassCircleButton`.
   - Floating mini-player pill, search pill, and player sheet → glass.
   - **Backdrop for LiquidToggle/LiquidButton**: they take a `backdrop` param and sample it — MUST obey the parent-above-child rule (sample a backdrop the button is NOT inside), same as all glass here, or it RenderNode-cycles. Pass the appropriate screen-local or app backdrop per context.
11. **Icons**: use OUR icon set (`R.drawable.*`) wherever an equivalent exists. Replace Material default icons (`Icons.*`, `Icons.Filled/Outlined.*`) with our drawables. Only fall back to a Material icon when we have no matching drawable.
5. **Hero image priority** (every screen that shows a hero): existing static artwork → animated/canvas artwork if present → first song's artwork → next song's artwork → **our default music image** (`R.drawable.music_note` / `default_song_cover`) as last resort.
6. **Hero background**: NO gradient scrim behind the hero — use a **flat plane of the artwork's main color**. Hero image fades into that main color seamlessly (animated color transition).
7. **List rows**: one shared flat row style used by EVERY list screen AND search results — hairline divider, thumbnail, white bold title, grey "Type • Artist" subtitle, trailing 3-dot. No per-row card fill. No rounded press/selected surface.
8. **Progressive-blur text-over-image**: use a gradient scrim + light blur (cheap), not real per-pixel progressive blur.
9. **Adaptive contrast**: wherever a main-color/hero background is applied, text + dividers + icons pick white or dark by background luminance.
10. **Nav bar + mini player visible on ALL screens** except: (a) expanded player, (b) search while the field is focused/typing — they reappear when the search pill is closed; search *results* keep the bar.

---

## Design tokens (create ONE file, reference everywhere)

New file `app/src/main/kotlin/com/music/vivi/ui/theme/AppleTokens.kt`:
```kotlin
object AppleTokens {
    val AccentRed = Color(0xFFFA2D48)
    val Bg = Color(0xFF000000)
    val BgElevated = Color(0xFF0B0B0B)
    val Card = Color(0xFF1C1C1E)
    val CardSecondary = Color(0xFF2C2C2E)
    val Divider = Color(0x1AFFFFFF)          // hairline on dark
    val CardCorner = 22.dp                    // ContinuousRoundedRectangle
    val CardCornerLarge = 28.dp
    // adaptive contrast
    fun onColor(bg: Color): Color = if (bg.luminance() > 0.5f) Color(0xFF0A0A0A) else Color.White
    fun dividerOn(bg: Color): Color = if (bg.luminance() > 0.5f) Color(0x1A000000) else Color(0x1AFFFFFF)
}
```
Use `ContinuousRoundedRectangle` (already in repo, used by nav bar) for card shapes, not `RoundedCornerShape`, wherever "continuous rounded" is called for.

---

## Verified code anchors (trust these; the implementer still opens the file to edit)

| Area | File:line | Note |
|---|---|---|
| Theme builder | `ui/theme/Theme.kt:31` `vivimusicTheme` | `MaterialTheme(colorScheme=…)`. |
| pureBlack copy | `ui/theme/Theme.kt:99` | Pattern for surface override. |
| Default seed | `ui/theme/Theme.kt:29` `DefaultThemeColor` | Change/relate to AccentRed. |
| Palette color extract | `ui/theme/Theme.kt:73/83` `extractThemeColor`, `extractGradientColors` | Reuse for hero main-color. |
| Theme UI + picker | `ui/screens/settings/ThemeScreen.kt:91` `PaletteColors`, `ColorPickerDialog` | Accent section. |
| Apple-UI toggle (REMOVE) | `ui/screens/settings/AppearanceSettings.kt:200` `AppleMusicUiKey` + `onAppleMusicUiChange` | Flip defaults, drop the switch. |
| Nav bar visibility | `MainActivity.kt:666` `shouldShowNavigationBar`; `:709` `hasDockedPlayerAccessory`; `:695` `collapsedBound` | P6 fix here. |
| App backdrop (glass root) | `MainActivity.kt` `appBackdrop` (~869), provided ~884, `.layerBackdrop(appBackdrop)` on NavHost (~1293) | Sheet samples this. |
| Home sections | `ui/screens/HomeScreen.kt:171` `HomeSection`; `:185` `CommunityPlaylistCard`; `:433` `DailyDiscoverCard`; `:549` `HomeScreen` | P4. |
| Liked/auto playlist | `ui/screens/playlist/AutoPlaylistScreen.kt:122` screen, `:594` `AutoPlaylistHeader` | P3. |
| Library filters | `ui/screens/library/LibraryScreen.kt:23` `ChipsRow` | P8 card + library redesign. |
| Library mix list | `ui/screens/library/LibraryMixScreen.kt:~416` auto-playlist item pattern | P8 local card. |
| Shared list row | `ui/component/Items.kt` `ListItem`/`SongListItem`/`YouTubeListItem` | P1 `flat` param. |
| Glass | `ui/component/GlassEffect.kt` `liquidGlass`; `ui/component/GlassCircleButton.kt`; vendored `ui/component/backdrop/*` | P2/P3/P5. |
| Glass toggles (vendored, unused) | `ui/component/backdrop/catalog/components/LiquidToggle.kt` (switch), `LiquidButton.kt` (glass capsule) | Toggles only; take a `backdrop` param. |
| Scanner | `utils/LocalAudioScanner.kt:22` `MIN_DURATION_MS=30_000` | P8. |
| DAO | `db/DatabaseDao.kt:136` `likedSongs`, `:86` `songs` | P8 add `localSongs`. |
| Player sheet | symbol `BottomSheetPlayer` / `NewMiniPlayer` (locate in `ui/player/`) | P5. |

---

## P0 — Theme foundation (do first; everything reads it)

**Goal:** dark Apple palette + red accent + adaptive contrast are the default. No toggle.

**Steps**
1. Add `AppleTokens.kt` (above).
2. `Theme.kt vivimusicTheme`: after building `baseColorScheme`, override the container/surface tones for dark mode UNCONDITIONALLY:
   ```kotlin
   fun ColorScheme.appleSurfaces() = copy(
       background = AppleTokens.Bg, surface = AppleTokens.Bg,
       surfaceContainerLowest = AppleTokens.Bg, surfaceContainerLow = AppleTokens.BgElevated,
       surfaceContainer = AppleTokens.Card, surfaceContainerHigh = AppleTokens.Card,
       surfaceContainerHighest = AppleTokens.CardSecondary, surfaceVariant = AppleTokens.CardSecondary,
   )
   // in vivimusicTheme, dark branch: cs = cs.appleSurfaces(); pureBlack still applied after and wins.
   ```
   Light mode: leave as-is (Apple app is dark-first; light stays Material — do NOT invest there now).
3. Accent: keep `SelectedThemeColorKey`. Change default seed so a fresh install seeds `AccentRed`. In `ThemeScreen.kt`, add `AccentRed` as the first/selected preset in `PaletteColors` and keep `ColorPickerDialog` for custom. Do not remove the palette list.
4. Remove the Apple-UI toggle: in `AppearanceSettings.kt`, delete the `AppleMusicUi` switch row and set the glass/nav prefs' **defaults** to the Apple values that `onAppleMusicUiChange` used to set (floating nav = true, glass global = true, surface opacity 0.5, blur 20, lens 0.6, vibrancy 1.5). Keep the pref keys (other code reads them); only the defaults change and the switch is gone.

**Rules:** never override `primary`/`onPrimary`/error roles here (accent handles primary via seed). pureBlack must still win. Do not touch light scheme.

**Verify:** fresh state → dark bg `#000`, cards `#1C1C1E`, accent red on switches/tabs/active icons. No Apple toggle in settings. PureBlack still forces black.

---

## P1 — Shared flat list row (used by every list + search)

**Goal:** one row style app-wide matching the search-results reference (hairline divider, no card fill, no rounded press surface).

**Steps**
1. `Items.kt`: add `flat: Boolean = false` to `ListItem` (and thread through `SongListItem`/`YouTubeListItem`). When `flat`:
   - No `.background(...)` card fill, no grouped rounded corners.
   - Bottom hairline `Divider(color = AppleTokens.dividerOn(bg))`.
   - **Press/selected state = a subtle full-width overlay, NOT a rounded material surface.** Override the row's `indication` so the ripple/selected bg is a flat rect (or `Color.White.copy(0.06f)` overlay), never a `RoundedCornerShape` fill. This is the fix for "artist list tap reveals old material rounded theme."
2. Default `flat=false` → every current caller unchanged.
3. Flip `flat=true` at call sites in P3/P4/P7 screens.

**Rules:** additive param, default false. Do not restyle non-flat callers. The press-state fix applies only in flat mode.

**Verify:** flat rows look like the search-results screenshot; tapping shows a flat highlight, never a rounded card.

---

## P2 — Hero + main-color system (reusable, powers P3/P5/P7)

**Goal:** every hero uses the priority chain; background is a flat plane of the artwork's main color; hero fades into it seamlessly; contrast adapts.

**Steps**
1. New `ui/component/HeroArtwork.kt`:
   - `@Composable fun rememberHeroSource(staticArt, animatedArt, songs): HeroSource` — resolves per priority chain (P0 decision 5). Returns the URL/painter to show + whether it's animated.
   - `@Composable fun rememberHeroTint(url): Color` — Coil load → `bitmap.extractThemeColor()` (reuse `Theme.kt:73`). Animate color changes with `animateColorAsState` so hero→color is seamless.
   - Composable `HeroBackground(tint, heroSource, modifier)`: paints a flat `tint` plane, draws the hero image on top with a fade-to-`tint` at its bottom edge (vertical alpha fade of the IMAGE, not a black scrim), so the image dissolves into the color plane.
2. Provide `AppleTokens.onColor(tint)` / `dividerOn(tint)` to descendants (via a small `CompositionLocal` or explicit params) so titles/rows/icons over the hero adapt.

**Rules:** NO black/gradient scrim behind hero (decision 6). Default image only as last resort. Reuse `extractThemeColor`; do not add a new palette lib.

**Verify:** open items with/without art, with animated canvas art, and empty → correct hero picked each time; bg is the art's main color; text readable on light and dark art.

---

## P3 — Detail screens (Artist, Album, Playlist, Liked/Auto, and ALL list screens: top songs, album tracks, etc.)

**Goal:** identical treatment across all of them.

**Applies to EVERY list screen — no exceptions:**
- Screens that currently have **no hero image at all** MUST get one via the P2 priority chain (first song art → next song art → default image). A missing native hero is not a reason to skip — synthesize it.
- Screens that currently show a **small squared / cropped thumbnail header** (e.g. inset rounded card, side-by-side thumbnail) MUST be converted to the full-bleed `HeroBackground` faded look. No more square thumbnail headers anywhere.
- Audit all list screens for these two cases and fix each: LocalPlaylist (inset thumbnail card), any library sub-screen, top-songs, album tracks, artist items, mood/genre results — all use the same full faded hero + main-color background.

**Steps (same recipe each)**
1. Background: `HeroBackground` (P2) using the priority hero. Flat main-color plane; hero fades in.
2. List: flat rows (`flat=true`, P1). Same spacing/divider as search.
3. Chrome buttons (back/share/favorite/play/shuffle): keep existing **glass** buttons (`GlassCircleButton` + real glass via parent-above-child — chrome icons are an ANCESTOR overlay above the captured hero layer, sampling it, never inside its capture → no RenderNode cycle). Confirm against `glass-chrome-rendernode-cycle` note.
4. **Liked/AutoPlaylist** (`AutoPlaylistScreen`): rebuild header to the P2 hero (faded art + main-color bg) and P1 flat list. `AutoPlaylistHeader` is generic; restyle in place, keep its data/menu/download logic.
5. Fix any lingering old-material surfaces revealed on interaction (menus, selected states) → new UI.

**Rules:** do not change data/query/menu logic — skin only. Glass chrome must sample the screen-local hero backdrop, never the app-root `appBackdrop` (that cycles). Keep `flat=true` only here + P4/P7.

**Verify:** Artist/Album/Playlist/Liked/track-list screens all share the look; tap states flat; glass chrome refracts art; no crash on bright/dark art; no old rounded material anywhere.

---

## P4 — Home screen cards

**Anchor:** `HomeScreen.kt`.

**Steps**
1. **Quick Picks / quick playlist rows** → the P1 flat list-with-spacing rows (like search results).
2. **CommunityPlaylistCard** (`:185`): keep the layout; swap `containerColor` → `AppleTokens.Card`; corners → `ContinuousRoundedRectangle(CardCornerLarge)`; OUR icon set on every button. Play/radio = solid dark-grey (`CardSecondary`); the **save/bookmark toggle = glass** (decision 4).
3. **Keep Listening + other carousels**: bigger cards, `ContinuousRoundedRectangle` slightly more rounded; move title/subtitle INSIDE the card at the bottom over a gradient scrim + light blur (P0 decision 8); adaptive text color.
4. **Mood & Genres**: bigger cards, each with a representative genre image (use the section's thumbnail; if none, a color tile from the genre name hash). Label inset bottom-left.
5. **Daily Discover** (`:433`): keep layout; swap icons to our set + retheme colors only. It already has a scrim — leave it.
6. Home buttons/icons → **solid dark-grey Apple buttons** — EXCEPT toggle buttons (like/save/shuffle/follow), which are **glass** (decision 4). Icons → our `R.drawable.*` set.

**Rules:** Home buttons are the ONLY place we force solid-grey over glass. Keep card data/click logic. Reuse `NavigationTitle` for section headers (bold + chevron).

**Verify:** Home matches refs — flat quick rows, dark cards with our icons, bigger rounded cards with bottom-inset blurred titles, mood tiles with images.

---

## P5 — Player bottom sheet

**Goal:** glass sheet sampling the whole nav stack, Apple icons/font/buttons, spring animation.

**Steps**
1. Locate `BottomSheetPlayer` (in `ui/player/`). Give its background real glass sampling `appBackdrop` (`MainActivity` root). The sheet renders ABOVE the NavHost (sibling over the captured `layerBackdrop(appBackdrop)` subtree) → it observes the whole stack WITHOUT being inside the capture → no cycle. This is the same parent-above-child rule as chrome.
2. Icons + typography → Apple set/weights. Buttons → Apple style (solid dark-grey for secondary; accent for primary play).
3. Sheet motion → spring (`spring(dampingRatio = MediumBouncy, stiffness = Low/Medium)`) on the expand/collapse offset.

**Rules:** verify no RenderNode cycle (sheet must be a true sibling above the NavHost's `layerBackdrop`, not a child). Do not change playback logic.

**Verify:** sheet glass shows blurred content behind incl. nav bar; springy open/close; Apple icons; no crash.

---

## P6 — Nav bar + mini player everywhere

**Anchor:** `MainActivity.kt:666`.

**Steps**
1. Extend `shouldShowNavigationBar` so it stays true on detail routes (album/artist/playlist/auto_playlist/etc.) — effectively: show unless the route is a full-screen context. Keep it hidden only when the **expanded player** covers the screen and when **search field is focused/typing**.
2. Because the mini player docks into the floating bar (`collapsedBound=0.dp`, `:695`), keeping the bar visible restores the mini player automatically via `hasDockedPlayerAccessory` (`:709`).
3. Search: bar hidden while the pill/field is focused; reappears when pill closed; search-results view keeps the bar.

**Rules:** don't regress the expanded-player and search-typing hide cases. Recheck `playerAwareWindowInsets` (`:711`) so content padding stays correct now that the bar shows on more routes (bottom inset must include nav+mini-player on those routes).

**Edge progressive blur (iOS-style — add here, applies app-wide):**
- **Bottom edge**: a light progressive-blur strip UNDER the mini player + tab bar, so scrolling content blurs more as it descends into/behind the bar (clear at the top of the strip → blurred at the bottom). Height ≈ mini-player + tab-bar zone.
- **Top edge**: a light progressive-blur strip under the status bar so content passing up behind it is blurred → status bar stays clean/legible over any content.
- Implementation: a `drawBackdrop`/backdrop-blur overlay (vendored `ui/component/backdrop`) with a **vertical alpha-gradient mask** so blur ramps in progressively (not a hard-edged blurred rect). Keep it LIGHT (small blur radius). This is a fixed overlay pinned to the top and bottom insets, above content, below/around the bars.
- These overlays are ancestors above the scroll content (they sample the content backdrop) — obey the parent-above-child glass rule; never place them inside their own captured layer.
- Adaptive: the strips are just blur + a faint fade of the bg color, no hardcoded black — so they read well over light and dark content.

**Verify:** scroll any list — content softly blurs as it slides under the tab bar/mini player and under the status bar; both bars and the status bar stay crisp; no hard blur seam; no cycle/crash.

**Verify:** open album/artist/playlist → bar + mini player present; expand player → both hidden; focus search → hidden, close pill → back; content isn't clipped behind the bar.

---

## P7 — Search

**Steps**
1. Remove the keyboard-auto-open `LaunchedEffect` in `SearchScreen.kt` (land on Explore, no IME).
2. Morph: `SharedTransitionLayout` above the NavHost (MainActivity), `sharedElement` key `"search-pill"` on the nav-bar search circle ↔ SearchScreen pill.
3. Results view → P1 flat rows (same as whole app). Mini player + sheet glass identical to rest of app.

**Rules:** body logic already correct (`query.isEmpty()` → Explore; else results) — keep. Don't reintroduce auto-focus.

**Verify:** tap circle → morph to pill, no keyboard, Explore shows; type → flat results; back → home; bar behavior per P6.

---

## P8 — Carryover: Local Music + scanner

**B. Local Music card in Library** (now in the redesigned Liked style)
1. DAO `db/DatabaseDao.kt`: add `localSongs(sortType,descending)` mirroring `likedSongs` (`:136`) with `WHERE isLocal` (SELECT-only, no schema change, no migration). Confirm `isLocal` column on `song`.
2. `AutoPlaylistViewModel`: add `"local"` branch → `database.localSongs(...)`, same flow shape as `likedSongs`.
3. `AutoPlaylistScreen`: map `"local"` → display name `R.string.local` (add string) and `PlaylistType.OTHER` (skips sync). No new enum member.
4. `LibraryMixScreen.kt` (~416): add one `PlaylistListItem(autoPlaylist=true, … navigate("auto_playlist/local"))`, built like the existing uploaded/downloaded cards. Route `auto_playlist/{playlist}` already exists.

**C. Scanner** (`utils/LocalAudioScanner.kt`)
1. Investigate the reported "songs not all" on-device first. Likely `MIN_DURATION_MS=30_000` cutting short tracks — expose/lower it. MediaStore already accepts `audio/%`.
2. Formats (wav/mpe/…): playback is Media3 (already decodes these); scanner already mime-filters. Confirm the real missing case before changing — may be scan coverage only, not decode.

**Rules:** additive only; reuse `PlaylistType.OTHER`; no Room version bump; the Local screen inherits P3 styling automatically.

**Verify:** Library shows Local Music card → opens redesigned hero+flat-list screen with all local songs; short/wav songs appear after rescan.

---

## GLOBAL STRICT RULES

1. **Skin, don't rewire.** Change visuals only. Never alter playback, queue, DB queries, sync, download, or navigation logic except the explicit P6/P8 additions.
2. **No Room schema change / migration** anywhere. P8 is SELECT-only.
3. **Glass sampling rule (crash-critical):** any `liquidGlass` must sample a backdrop it is NOT inside. Chrome/sheet = ancestor/sibling ABOVE the captured hero/app layer. A glass surface inside its own captured subtree = native RenderNode cycle = SIGSEGV. Verify each new glass usage against `glass-chrome-rendernode-cycle`.
4. **Reuse first:** `ListItem`/`SongListItem`, `GlassCircleButton`, `ContinuousRoundedRectangle`, `extractThemeColor`, `NavigationTitle`, existing icons. No new list-row, no new palette lib, no new glass impl.
5. **One shared row** for all lists + search. One shared hero system. One tokens file. Don't fork per screen.
6. **Adaptive contrast is mandatory** on every hero-color background — no hardcoded white text on a possibly-light color.
7. **Additive params default to old behavior** (`flat=false`) so untouched callers don't change.
8. **Do not run Gradle / launch the app.** Leave code compiling-clean; repo owner builds and verifies on-device per phase.
9. **If code contradicts an anchor here, STOP and report** — don't force the plan onto a structure that changed.
10. **Phase gating:** finish + on-device verify each phase before the next. P0 and P1/P2 (foundations) before any screen phase.

## Files touched (superset)
- New: `ui/theme/AppleTokens.kt`, `ui/component/HeroArtwork.kt`.
- Edit: `ui/theme/Theme.kt`, `ui/screens/settings/ThemeScreen.kt`, `ui/screens/settings/AppearanceSettings.kt` (remove toggle), `MainActivity.kt` (P5 sheet glass, P6 nav visibility, P7 shared-transition), `ui/component/Items.kt` (flat), `ui/screens/HomeScreen.kt`, `ui/screens/playlist/AutoPlaylistScreen.kt`, `ui/screens/AlbumScreen.kt`, `ui/screens/artist/ArtistScreen.kt`, `ui/screens/playlist/{Local,Online}PlaylistScreen.kt`, `ui/screens/search/SearchScreen.kt`, player sheet file (`ui/player/…`), `ui/screens/library/{LibraryScreen,LibraryMixScreen}.kt`, `db/DatabaseDao.kt`, `viewmodels/AutoPlaylistViewModel.kt`, `utils/LocalAudioScanner.kt`, `res/values/strings.xml` (`R.string.local`).
- No deletions beyond the Apple-UI toggle row. No migrations.
