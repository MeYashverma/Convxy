# Liquid Glass Design Language

Convxy's UI is built on one glass system with two rendering families and a
three-rung intensity ladder. Everything interactive in the app — chrome, menus,
dialogs, chips, floating controls — is a surface from this system. If a new UI
surface doesn't trace back to a `GlassLevel`, it is off-language by definition.

## The ladder

Every glass surface belongs to one of three rungs (`ui/component/GlassLevels.kt`):

| Rung | What lives there | Frost | Edge | Fill |
| --- | --- | --- | --- | --- |
| **PRIMARY** | Surfaces that carry a layer: nav bar, tab bar, side panel, mini player, full player, and elevated panels above a dim (context menus, sheet menus, dialogs, sheet pages) | Deepest (blur ×2) | Brightest hairline | Most substantial (≈0.94) |
| **SECONDARY** | Floating controls on content: glass circle buttons, hero action pills, FABs | Clear (blur ×1.5) | Crisp | Medium (≈0.86) |
| **TERTIARY** | Inline elements inside a larger surface: menu tiles, settings groups, chips | Lightest (blur ×1) | Subtle | Lightest (≈0.72) |

The rung is what makes the hierarchy visible before any content is drawn: the
panel that carries a screen reads as the most solid piece of glass, the
controls floating over artwork read as clear refractive objects, and inline
elements stay quiet accents.

## The two rendering families

### 1. Sampled glass — `Modifier.liquidGlass(...)`

The real pipeline: it samples the actual pixels behind the surface and applies
the user-tuned blur, saturation, lens refraction, specular rim and shadow. Used
by the nav bar, tab bar, side panel, mini player, full player and
`GlassCircleButton`. Gated by `isGlassAllowed()` (API level + low-RAM) and by
the user's per-component switches in Settings → Glass.

Rules:

- Per-surface effect parameters come from the user's `GlassEffectConfig`.
  Never hardcode a blur radius; scale the config's by the level's
  `blurScale` (`GlassLevels.kt`) when a surface needs more or less frost.
- Small surfaces (buttons, pills) keep `applyEdgeEffects = true` — the rim and
  lens are what make them read as *objects*.
- Large surfaces (full player) pass `applyEdgeEffects = false` and a heavier
  blur; a rim there reads as a stray band of light.
- A surface that is permanently on screen must keep its lambdas remembered
  (see the notes in `GlassEffect.kt`); fresh lambdas re-capture the whole
  screen on every recomposition.
- When the pipeline can't run (device, or user chose the transparent style),
  `liquidGlass` draws the translucent-tint fallback itself. That fallback look
  is exactly what the second family renders on purpose — the two families meet
  in the middle and the app stays one material.

### 2. Translucent panels — `Modifier.glassPanelSurface(...)`

The un-sampled half. Used where the capture pipeline cannot or should not run:

- **Dialogs** live in their own window; there is no app backdrop to sample.
- **Root-level menus** (`OverlayMenu`) float above a full-screen scrim; running
  a capture pipeline there costs a whole-window re-capture per frame for a
  surface that is about to be covered anyway (see the note in `OverlayMenu.kt`).

A panel draws three things, in order: a tinted translucent fill at its rung's
alpha, a soft specular sheen across the top third (under the content), and a
0.7dp hairline edge around the rim. `rememberGlassPanelColors(level)` resolves
the recipe for the current theme; callers over an artwork tint can pass their
own `fill` base colour and keep the rung's translucency.

Every elevated surface in the app uses this recipe: `OverlayMenu`,
`BottomSheetMenu`, `BottomSheetPage`, `DefaultDialog`/`ListDialog` (and
therefore every dialog built on them), `Material3MenuGroup` tiles,
`Material3SettingsGroup` cards, and the non-glass fallback of
`GlassCircleButton`.

## Controls

| Control | File | Rung | Notes |
| --- | --- | --- | --- |
| `GlassCircleButton` | `GlassCircleButton.kt` | SECONDARY | Sampled glass; press wobble + ripple; falls back to the panel recipe |
| `GlassSwitch` / `GlassSwitchCompat` | `GlassSwitch.kt` | — | iOS toggle; track rim reproduced with a plain border (deliberately no pipeline — see its perf notes) |
| Chips (`ChipsRow`, `ChoiceChipsRow`) | `ChipsRow.kt` | TERTIARY | Pill shape; translucent resting fill + hairline edge; filled accent + check when selected; edge colour follows the fill's luminance so tinted chips over heroes stay legible |
| Menu tiles | `Menu.kt` | TERTIARY | Translucent tiles that stack on the PRIMARY panel behind them; custom `cardColors` rows keep their own look |
| Settings groups | `Material3SettingsGroup.kt` | SECONDARY | One grouped glass card with hairline dividers |

## Reading this when adding a surface

1. **What is it?** Layer-carrying chrome or elevated panel → PRIMARY. Floating
   control → SECONDARY. Inline element → TERTIARY.
2. **Can it sample the app behind it?** If yes and it's chrome/control, use the
   `liquidGlass` family with the level's blur scale. If it sits in its own
   window or above a full-screen scrim, use `glassPanelSurface` at PRIMARY and
   never pay for a pipeline that can't see the app.
3. **Content colour** comes from the theme (`onSurface`) on panels; sampled
   glass derives it by compositing what the surface actually shows
   (`glassContentColorFor`), which is why `GlassCircleButton` and the nav bar
   carry `glassConfig.textColor` rather than a scheme colour.
4. **Readability first.** Translucency is tuned per rung so text never fights
   the layer behind it. If a surface needs more contrast, raise its rung —
   don't hand-tune a one-off alpha.
5. **Motion** follows `AppleTokens.Motion`: springs for state changes, tweens
   for fades. Press feedback on glass controls is a scale wobble
   (`pressWobble`) plus ripple — never a hard tint flash.

## Performance contract

- No sampled-glass surface is added inside lazy rows; the pipeline captures a
  screen region per surface. Floating controls are fine (a handful per
  screen); per-row glass is not.
- Translucent panels do all their work in a few draw ops — no capture, no
  RenderEffect, no per-frame invalidation — so they are safe anywhere.
- Respect the freeze machinery (`BackdropFreeze`, `frozen`, `NavTransitionFreeze`)
  documented in `ui/component/backdrop/` when touching the sampled pipeline.
