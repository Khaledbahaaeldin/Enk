# Enk liquid glass (batch 15) design

Date: 2026-11-15. Status: DRAFT for the author's approval. Companion to the design system (`2026-11-07-enk-design-system.md`, sections 12.4, 12.8, 12.9) and to batch 14 (`docs/audit/batch-14-handoff.md`). Mockups: `docs/mockups/enk-liquid-island.html` (the island, option C chosen) and `docs/mockups/enk-liquid-nav-controls.html` (the nav bar, option B chosen; the controls). Reference screenshots the author supplied: WWDC25 glass blobs melting, the Analytics lens over a dark bar, Mail's toolbar and round compose button, the Music and Podcasts tab bars, the iOS 26 controls sheet.

## 1. Intent

Batch 14 shipped "glass" that reads as frost: every surface blurs 14 to 44 dp before its lens, so the bending is buried, and the writer island has an opaque fill and cannot refract at all. Full mode is also laggy on the author's Galaxy Note 20 Ultra (Android 13, Mali-G77 class GPU), while Balanced and Light are smooth, so the cost is the refraction shaders, not the blur or the motion.

The author wants the real thing: Apple-style liquid glass, where controls are clear lenses that bend and magnify what is under them, with a lit rim and a colour fringe, that swell when touched, lean after the finger, and melt into each other and split apart like drops of water. The current frost stays as a second look, because the author likes it. Both looks get tuning sliders. And all of it must be smooth on a mid-range phone.

## 2. Goals, non-goals, anti-goals

Goals
- A **Liquid** look built on the recipe of Backdrop's own demo app (Kyant0/AndroidLiquidGlass "Catalog"): near-zero blur, rim lens with colour fringe, vibrancy, ambient and directional rim light, glass that samples other glass.
- **Frost** kept as the second look, unchanged in appearance, made cheaper.
- **Melting and splitting** shapes in the places listed in 6.
- **One press feel** for every touchable glass element (6.3).
- **Speed**: Liquid at Full holds 60 fps with no dropped frames on the Note 20 Ultra in every drag and transition; at 120 Hz it aims for 120 and accepts some drops only in the Write transition (8).
- Settings to choose the look and tune it (5).

Non-goals
- Glass on the writing surface itself (paper, rail, carriage stay as they are).
- Merging for sheets and full-screen panels (too large, nothing visibly splits).
- A new dependency: Backdrop 2.0.1 stays the only glass library.
- New screens or features.

Anti-goals (would be failures even if it works)
- Liquid that still reads as frost (blur doing the work instead of the lens).
- Any visible flicker or jump when a shape hands over between Backdrop's lens and our melt shader.
- Lag on the Note 20 Ultra at Full, or Auto demoting there in normal use.
- Text inside glass that is hard to read (icons and labels sit on top of the glass, never refracted).

## 3. Constraints

- Backdrop 2.0.1 (`io.github.kyant0:backdrop`). AndroidLiquidGlass is the same project (its source repo), not another library. Its lens only supports rounded rectangles (`RoundedRectangularShape` or `CornerBasedShape`; anything else throws), so melting needs our own shader, plugged in through Backdrop's effect hook (`BackdropEffectScope.effect(RenderEffect)` / a `RuntimeShader` effect) on the same captured backdrop.
- Refraction needs `RuntimeShader` (Android 13, API 33). On Android 12 and 12L (API 31 and 32, minSdk is 31) Liquid is not available: the look shows as Frost and the Liquid choice is disabled with a one-line footer ("Liquid glass needs Android 13 or later").
- Code ported from the Catalog app (`LiquidButton`, `LiquidToggle`, `LiquidSlider`, `LiquidBottomTabs`, `InteractiveHighlight`, `DampedDragAnimation`) is Apache-2.0: keep its copyright header in each ported file and list it in the credits and `docs/dependencies.md` as ported source.
- F-Droid and no-network rules unchanged. Goldens stay at Light (Robolectric draws no shaders); look and speed are verified on the device.
- Existing behaviour (G1 to G6 of batch 14, the Rail transition's beats, the type-on gate) keeps working except where this document changes it.

## 4. Approaches (decided: A)

- **A. Backdrop for single shapes, our melt shader only while shapes join or split.** Every surface at rest or moving alone uses Backdrop's lens with the Catalog recipe. When two shapes come within the merge distance, a `LiquidGroup` swaps their Backdrop surfaces for one draw of our SDF melt shader over the same backdrop; when they are apart again, it hands back. Picked by the author: real melts where they show, Backdrop's tested lens on almost every frame, GPU cost of the custom shader only during merges. Risk: the handoff must be invisible (9).
- B. Our own shader for all glass, Backdrop only as the capture. One look everywhere, melts anytime, no handoff; but we own the whole lens and its speed on mid-range GPUs. Rejected: most work and most risk.
- C. Backdrop only, melts faked with a bridging capsule. Cheapest; reads as stacked pills. Rejected: fails "melt everywhere it fits".

## 5. Settings

Setup > Display:

| Row | Control | Values |
|---|---|---|
| Glass | segmented | **Liquid** (default on a fresh install), Frost. Liquid disabled below Android 13 (3) |
| Effects | segmented (exists) | Auto (default), Full, Balanced, Light. Now only a cost cap, see the table below |
| Advanced | collapsed group, closed by default | the sliders of the current look, plus "Reset to default" |

Effects level x look:

| | Liquid | Frost |
|---|---|---|
| Full | full recipe: lens, colour fringe, depth, ambient + directional rim light, glass-on-glass, melt shader | today's frost (blur, saturation, tint), cheaper (8) |
| Balanced | smaller lens (60 percent of its height and amount), no fringe, no depth, rim light Plain, melts still drawn but by the same cheaper shader path | blur only |
| Light | solid surfaces (as today) | solid surfaces |

Auto: picks by device as today and, when the frame governor sees sustained slow frames, demotes Full to Balanced keeping the look, then to Light. No message to the user.

Advanced sliders (each look remembers its own values; ranges are mine, tune on device):

| Slider | Liquid | Frost |
|---|---|---|
| Refraction | lens amount, 0 to 2x the recipe value, default 1x | n/a |
| Lens depth | lens height (how far in from the rim the bending reaches), 0.5x to 2x, default 1x | n/a |
| Colour fringe | off to 2x, default 1x | n/a |
| Frost | extra blur 0 to 12 dp, default 0 (the recipe's own 2 to 8 dp stays) | blur 0.5x to 1.5x of today's, default 1x |
| Tint | surface fill 0 to 40 percent, default the recipe's | tint 0.5x to 1.5x, default 1x |
| Edge light | rim light alpha 0 to 1.5x, default 1x | same |

The sliders change the specs at runtime (`GlassSpec` values scaled by a `GlassTuning` read from settings); Light ignores them. Stored in `Settings` as two small value groups with a migration that fills defaults.

## 6. What the user sees and touches

### 6.1 The writer island (mockup option C, "Mail")

- Rest: one small liquid capsule at the top of the page, 150 x 40, showing `p.1 · 85 / 3000` (Courier 11 sp) and the 6 dp lamp dot when the lamp is on.
- Tap: in one motion the capsule stretches left into a toolbar (Keyboard, Box, and the short count `85 / 3000`) while a round **Lamp** button (about 46 dp) pinches off its right end and settles beside it with a small gap. This split is drawn by the melt shader (it starts as one shape and ends as two).
- Tap Lamp: the lamp panel (toggle, warmth, intensity, beam) grows **out of the round Lamp button** into a liquid panel below it (melt shader for the first part of the growth); the toolbar stays. While the lamp is on, the round button glows in the lamp colour.
- Closing: the reverse; the round button slides back and melts into the capsule.
- Folding rules unchanged (4.5 s idle, typing, a tap anywhere outside, Back); not interactive until the Rail transition reaches 0.95 (unchanged).
- Glyphs and text are drawn on top of the glass in the theme's `t1`, never refracted.

### 6.2 The nav bar (mockup option B)

- A light liquid bar with **Box, Find, Setup**, and to its right, with a 10 dp gap, a separate round **Write** button (about 62 dp) with the Write glyph in the accent colour.
- The selected tab sits under a soft pill (surface fill at rest). On touch the pill becomes a clear lens bigger than the bar (about +16 dp tall, +14 dp wide), samples **the page and the bar together** (combined backdrop) so it bends the tab icons under it, follows the finger 1:1 from where it was grabbed, stretches with speed, rubber-bands past the ends, and on release projects with momentum and drops onto the nearest tab (existing `LensController` math). Its blur goes to 0 and its fringe and depth come in while pressed (Catalog `LiquidBottomTabs`).
- Dragged toward the Write button, the lens and the button melt together (melt shader while within the merge distance). Releasing over Write does not open the writer (only a tap does).
- Tapping Write opens the writer through the Rail transition (7).

### 6.3 Press feel, everywhere

Every touchable glass element (the island, its round Lamp button and toolbar items, nav tabs and lens, the Write button, glass buttons, slider thumbs, toggle beads, segmented lens, menu rows, the alert's buttons) uses one shared modifier, ported from the Catalog's `InteractiveHighlight` and `DampedDragAnimation`:

- On touch-down: the surface swells by 4 dp of its height (scale `1 + 4dp / height`), a soft glow (white, 8 percent plus a radial 15 percent falloff over 1.5x the shorter side) appears under the finger, and the rim light turns toward the finger.
- While pulled: it leans toward the finger, `translation = maxOffset * tanh(0.05 * offset / maxOffset)` with `maxOffset` the shorter side, and stretches along the pull by up to 4 dp; it follows less the further you pull.
- On release: springs back (press spring damping 0.5, stiffness 300) with a slight wobble.
- Thumbs and beads: a white capsule at rest that turns into a clear, bigger lens while held (surface alpha `1 - progress`, lens and fringe scaled by `progress`, blur `8 dp * (1 - progress)`), samples its own track so the track's colour shows through it, and stretches with velocity (`scaleX /= 1 - clamp(0.75 v)`, `scaleY *= 1 - clamp(0.25 v)`, clamp to 0.2).
- Frost gets the same swell, lean, stretch and glow, without the lens.
- Light: no swell or lean; a 100 ms highlight only.

### 6.4 Where shapes melt or split (the melt shader's list)

1. The island capsule splitting into the toolbar and the round Lamp button, and melting back.
2. The lamp panel growing out of the round Lamp button.
3. The nav lens melting into the Write button when dragged within the merge distance.
4. The Write button stretching into the rail (7).
5. The context menu and the Delete forever alert budding out of the button that opened them and pulling free (replaces the current `GlassMorph` growth).
6. A toggle bead or slider thumb pulling a short neck from its track when yanked fast past an end.
7. The segmented control's lens melting between segments as it moves.

Not merged: sheets, the keyboard tray, toasts, full-screen panels.

## 7. The Write transition, updated

Beats as in the design system 12.9, with beat 2 changed: instead of the whole nav bar becoming the rail, **the round Write button** swells (the press), then stretches sideways and flattens into the 16 dp rail rectangle on the rail's centre line (melt shader during the stretch, so it reads as one drop pulled flat), while the tab bar sinks 24 dp and fades out over the same window. Glass to steel (beat 3), paper feed, carriage drop with the ding, island arrival and the type-on gate are unchanged. Reverse: the rail shrinks back into the round button and the bar rises back. The Box recede beat keeps its scale, offset and dim but **drops its 8 dp full-screen blur at every level** (measured cost, see 8); if the author misses it on device it comes back as a downscaled blur at Full only.

## 8. Speed

Who measures: the author connects the Note 20 Ultra to the development PC over USB (USB debugging on) when asked; Claude runs the script and the frame stats, before the batch starts (baseline) and in the audit. Antigravity cannot measure and must not claim numbers.

Method (before any fix and after each): on the Note 20 Ultra, `adb shell dumpsys gfxinfo app.enk.notes framestats` and a Perfetto trace while running a fixed script: open the app, drag the nav lens across and back, open and close the island, open the lamp panel and drag a slider, run Write and back, open the context menu and the alert, change tabs. Record p50, p90, p95 frame time and janky-frame count per step at 60 Hz and 120 Hz, for Liquid and Frost at Full and Balanced. The numbers go in the batch handoff.

Targets: at 60 Hz no janky frames in any step at Full; at 120 Hz p95 under 8.3 ms except the Write transition (p95 under 11 ms).

Planned fixes (each kept only if the measurement shows it helps):
- No large blurs in Liquid (2 to 8 dp only), Frost's 44 dp blurs cut to the smallest value that keeps the look, measured.
- One backdrop layer per screen, re-recorded only when the content under the glass changes (scroll, typing, transition frames), not on every frame of a press or a lens drag over static content.
- The bar and its lens drawn from one shared backdrop; the lens samples the bar through an exported backdrop instead of a second full capture.
- Lens, fringe and rim-light shaders only on small surfaces; the bar body itself gets a light lens (12/24 dp) and no fringe.
- The melt shader runs only while a merge is in progress; it never runs on idle frames.
- No full-screen blur in the Write transition (7).
- No recomposition per frame anywhere in the glass kit (draw-phase reads only, as batch 14's audit established for the island).

## 9. The handoff between Backdrop's lens and the melt shader

The melt shader reproduces Backdrop's rounded-rect lens for a single shape: same refraction height and amount, same dispersion, same rim highlight and inner shadow, same colour controls. The batch must prove the match: a device capture of a single capsule drawn by each path at the same frame, overlaid, with no visible difference (the builder documents the comparison with screenshots in the handoff). The swap happens on the frame two shapes come within the merge distance (default 24 dp, the smooth-union radius) and back on the frame they leave it, never mid-press.

## 10. Testing

- JVM: the melt shader's SDF and smooth-union math (distance, union, merge radius, neck width) as pure Kotlin mirrored from the AGSL; press-feel math (lean, stretch, swell) at endpoints; settings migration and the Effects x look table.
- Compose: the island opens into toolbar plus Lamp button and closes back; the lamp panel opens from the Lamp button; the nav bar has Box, Find, Setup and a Write button; dragging the lens selects a tab; tapping Write runs the Rail transition (existing `RailNavigationTest` updated); Liquid is disabled below API 33.
- Goldens at Light for the new layouts (island open, nav bar with Write button, controls).
- Device checklist rows (added to `docs/qa/glass-device-checklist.md`): every item of 6.4, the handoff match (9), the press feel, the speed table (8), Liquid vs Frost at Full and Balanced, the Advanced sliders, Android 12 shows Frost.

## 11. Decided by the author in the brainstorm

Liquid and Frost as two looks with Advanced sliders; Liquid by default; Effects levels as a cost cap; Auto demotes quietly and keeps Liquid; island option C (toolbar plus round Lamp button); nav option B (bar plus round Write button); the round Write button becomes the rail; melts "everywhere it fits"; press swell and lean on everything; 60 fps solid with 120 where possible; approach A.

## 12. My calls (the author can overturn any)

- The slider list and ranges in 5; per-look memory.
- The lamp panel grows from the Lamp button; the Lamp button glows in the lamp colour when lit.
- The merge list in 6.4 and the exclusions; merge distance 24 dp.
- Dropping the Box blur in the Write transition (7).
- Liquid disabled below Android 13.
- Releasing the nav lens over Write does not open the writer.

## 13. Left to the builder

Exact sizes not stated here (follow the mockups and the Catalog), the AGSL of the melt shader, the structure of `LiquidGroup` and the press modifier, how the backdrop re-recording is gated, and the order of the speed fixes (driven by the measurements).
