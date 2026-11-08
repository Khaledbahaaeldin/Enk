# Enk design system

Date: 2026-11-07. Status: writer (sections 1 to 11) and interface shell with themes (section 12) approved by the author; a few items remain PROPOSED (marked inline and in 12.13). Companion to `2026-11-07-enk-design.md` (the product spec) and `../plans/2026-11-07-enk-1-0-plan.md` (the build plan).

This is the single source of truth for how Enk looks, moves, and sounds. Every developer follows it. If a screen needs something this document does not define, extend this document first (in a reviewed change), then build.

## 0. How to read this document

- **Source of values.** Every number in sections 2 to 11 (the writer) is taken from the approved prototype, `spike-throwaway/marker-prototype-v2.html`; every number in section 12 (the shell and themes) is taken from `docs/mockups/enk-screens-v4-themes.html`. The prototype is throwaway code and a reference only. When a value here and the prototype disagree, this document wins; when this document is silent, the prototype is the tiebreaker.
- **Units.** The prototype is drawn on a 400-wide phone where 1 CSS px = 1 dp (and 1 sp for text). All sizes below are in dp unless marked sp, ms, or %. On other widths, only the values marked "derived" scale.
- **Status tags.** `APPROVED` means the author reviewed it in the prototype. `PROPOSED` means a developer or I derived it from approved rules but the author has not seen it; a PROPOSED component must be shown to the author as a mockup or prototype and approved before it ships (the open items are listed in 12.13).
- **Naming.** Token names are the Kotlin names used in `:design` (`EnkColors.paper`). One name per concept; do not add synonyms.

## 1. Principles

1. **One machine.** Rail, carriage, panel, and knobs share one material family: brushed steel plus matte graphite. Nothing may look pasted on. (An all-orange marker was rejected for exactly this.)
2. **Orange is an indicator, never a surface.** Orange appears only as: the caret underline, the carriage's indicator strip, the knob index lines, and the wordmark dot. Nothing else is orange. No orange buttons, backgrounds, or text.
3. **Tactile and physical.** Heavy soft shadows, a 1 dp highlight on lit top edges, recessed slots with inner shadows, grooves for grip. Never flat fills where a material belongs.
4. **Restrained.** One accent, no gradients on paper, no ruler or tick marks on the rail or panel, no decorative icons. Quiet by default.
5. **Motion has weight.** Things lift, settle, and overshoot slightly like mechanical parts. No fades-only transitions for objects that should move.
6. **Never block the writer.** No modal interrupts the writing screen; nothing animates over the text line while typing.
7. **Original work only.** Do not reuse Platen's artwork, capsule shape, charms, flower tabs, sounds, copy, or name.

## 2. Color

### 2.1 Core tokens (`EnkColors`)

| Token | Value | Use |
|---|---|---|
| `chassisTop` | `#34322F` | Top of the day body gradient |
| `chassisBottom` | `#222120` | Bottom of the day body gradient |
| `chassisNightTop` | `#171615` | Night body gradient top |
| `chassisNightBottom` | `#0D0C0C` | Night body gradient bottom |
| `paper` | `#F2ECDF` | Day paper |
| `paperNight` | `#161513` | Night paper |
| `ink` | `#2B2824` | Day text |
| `accent` | `#FF5A1F` | Caret, indicator strips, index lines, wordmark dot |
| `accentGlowNight` | `#FF5A1F` at 70% | Indicator-strip glow at night (see 6.3) |
| `textOnChassis` | `#CFC8BA` | Wordmark, panel values |
| `textMuted` | `#8B8578` | Meta text, panel labels, hint text |
| `slotDark` | `#161514` | Carriage slot window |
| `trackDark` | `#121110` | Fader track |
| `panelTop` | `#3A3936` | Panel gradient top |
| `panelBottom` | `#262523` | Panel gradient bottom |
| `panelRing` | `#0D0C0B` | 1 dp outer ring around the panel |
| `slugLetter` | `#3A3A38` | Letter on the type-slug |
| `accentHi` / `accentLo` | `#FF8658` / `#CF3F0C` | RESERVED. Unused in 1.0. Do not use. |

### 2.2 Steel and graphite stops (`EnkMaterials`)

All gradients list stops from first to last in drawing direction.

| Name | Direction | Stops |
|---|---|---|
| `railSteel` | top to bottom | `#F4F4F2` 0%, `#A9A9A6` 40%, `#6F6F6C` 62%, `#C9C9C6` 100% |
| `bracketSteel` | left to right | `#8B8B88`, `#D8D8D5`, `#7A7A77` (evenly spaced) |
| `collarTop` | top to bottom | `#F0F0ED`, `#A9A9A6` |
| `collarBottom` | top to bottom | `#8D8D8A`, `#D2D2CF` |
| `graphiteCap` | left to right | `#2A2927` 0%, `#3B3A37` 20%, `#3B3A37` 80%, `#2A2927` 100% |
| `graphiteKnob` | left to right | `#2A2927` 0%, `#3B3A37` 25%, `#3B3A37` 75%, `#2A2927` 100% |
| `slugSteel` | left to right | `#9D9D9A`, `#EFEFEC`, `#8A8A87` |
| `panelFace` | top to bottom | `#3A3936`, `#262523` |
| `chassisDay` | top to bottom | `#34322F`, `#222120` |
| `chassisNight` | top to bottom | `#171615`, `#0D0C0C` |

### 2.3 The lamp color (derived, `LampColor`)

The beam, glow, fader fill, slot glow, and night text all use one color computed from the Warmth fader.

```
w = warmth / 100                     // warmth slider value 0..100, default 25
r = 255
g = round(236 + (150 - 236) * w)     // 236 at w=0, 150 at w=1
b = round(214 + (70  - 214) * w)     // 214 at w=0, 70  at w=1
```

Reference values: warmth 0 gives (255, 236, 214); warmth 25 gives (255, 215, 178); warmth 100 gives (255, 150, 70). `round` rounds half up.

### 2.4 Day and night

- Day: chassis gradient `chassisDay`, paper `paper`, text `ink`, grain at 35% (see 4.1).
- Night: chassis `chassisNight`, paper `paperNight`, text = lamp color, grain at 10%. All three switch with a 600 ms cross-fade (`EnkMotion.themeFade`).
- Night is entered two ways, which both produce the same visuals: the user long-presses the carriage (manual lamp), or the system theme is dark. In system-dark with the lamp off, the paper is `paperNight`, text is `textOnChassis`, and no beam is drawn; the lamp is still toggled by long-press. (PROPOSED: system-dark behaviour; confirm with the author.)

### 2.5 Contrast

Every text and indicator color pair must be checked before release. Minimum: 4.5:1 for text under 18 sp, 3:1 for large text and for non-text indicators. `textMuted` (#8B8578) on `chassisBottom` and on `panelBottom` must be measured; if it fails, raise it to the nearest passing lightness and record the new value here. The lamp-lit text at low intensity must stay at 4.5:1 on `paperNight` inside the beam.

## 3. Typography

| Role | Font | Size / weight | Tracking | Color |
|---|---|---|---|---|
| Paper text | Courier Prime (SIL OFL 1.1), bundled | derived: `fs = textWidth / (cols * 0.6)` (see 7.1); about 19.5 sp on 400 dp | 0 | `ink`, night: lamp color |
| Wordmark "ENK" | Android system sans | 13 sp, weight 600 | 0.16 em | `textOnChassis` |
| Meta (page count, status) | Courier Prime | 10 sp, line height 1.3 | 0.04 em | `textMuted` |
| Panel label | Courier Prime | 9 sp | 0.16 em, uppercase | `textMuted`, with engrave shadow (6.4) |
| Panel value | Courier Prime | 9 sp | 0.04 em | `textOnChassis` |
| Hint | Courier Prime | 10 sp | 0 | `textMuted` |
| Slug letter | Courier Prime bold | 9 sp, line height 12 | 0 | `slugLetter` |

Rules: Paper text is monospace and fixed-grid. Character advance width is exactly 0.6 em. Never use proportional type on the paper. Courier Prime must be bundled as a font resource (the FOSS build has no network); keep its OFL license file in `app/src/main/assets/licenses/`. Text on the paper does not follow system font scale directly; see 7.1 for how scale changes the column count instead. Chrome text (wordmark, meta, panel) honors system font scale up to 130%, then clips with ellipsis; no layout may break at 200%.

Text appearance on paper ("imperfect ink"): each character gets a deterministic jitter from its index `i`:

```
hash(n) = frac( sin(n * 127.1) * 43758.5453 )        // frac(x) = x - floor(x)
r  = hash(i + 1)
r2 = hash(i + 99)
dy      = (r  - 0.5) * 1.1      // dp, vertical offset, range -0.55..+0.55
rot     = (r2 - 0.5) * 2.2      // degrees, range -1.1..+1.1
opacity = 0.84 + r * 0.16       // range 0.84..1.00
```

The jitter is stable: the same character at the same index always looks the same. It must not be re-randomized on redraw or on scroll.

## 4. Texture and paper

### 4.1 Grain

A tileable noise bitmap overlays the paper, multiplied into it. Generate it once at first launch (or ship a prebuilt PNG; see plan Task 3) with this recipe:

- Tile 200 x 200 px, fractal noise, base frequency 0.9, 2 octaves, seed 4.
- Color of each noise pixel: RGB `(0.45, 0.40, 0.30)` (that is `#736650`) with alpha = 0.5 x noise value.
- Drawn over the whole paper with blend mode Multiply, layer opacity 0.35 (day) or 0.10 (night), tiled.
- Transition between the two opacities uses `EnkMotion.themeFade` (600 ms).

If the bitmap is prebuilt, it must be created by the generator script and committed together with the script and seed; never a downloaded image.

### 4.2 Paper sheet

| Property | Value |
|---|---|
| Position | inset 12 dp from left and right of the screen, top at 64 dp, bottom inset 12 dp |
| Corner radii | top-left 10, top-right 10, bottom-right 26, bottom-left 26 |
| Day fill / night fill | `paper` / `paperNight` |
| Outer shadow | two layers: (0, -1, blur 0, white 12%) and (0, 8, blur 20, black 45%) |
| Top "curl" shade | top 46 dp of the sheet: vertical gradient black 22% to transparent |
| Text fade-in mask | the text layer is faded in from transparent at the paper's top edge to opaque at 70 dp below it, so lines slide up out of sight under the top edge |

The paper is flat. No curved-paper effect in 1.0.

### 4.3 Surfaces and bevels (shared recipe)

Lit top edge: an inset 1 dp line `white @ 35%` (`white @ 22%` on the panel). Recessed parts (slot window, fader track, panel interior) use inner shadows, never outlines. Material edges never use a flat 1 dp border except the panel ring (`panelRing`).

## 5. Elevation and shadow (`EnkShadows`)

A shadow is `(dx, dy, blur, spread, color)` in dp. CSS blur radius `B` maps to a Gaussian with sigma `B/2`; implement with the same `B` in a shadow helper and tune against the prototype screenshot (tolerance in the golden tests, section 11).

| Name | Layers |
|---|---|
| `paperShadow` | (0,-1,0,0, white 12%); (0,8,20,0, black 45%) |
| `railShadow` | (0,7,10,0, black 45%); (0,1,0,0, black 40%) |
| `capShadow` | inset (0,1,0,0, white 35%); (0,12,16,0, black 55%); (0,2,3,0, black 60%) |
| `knobShadow` | inset (0,1,0,0, white 30%); (0,6,8,0, black 55%); (0,1,2,0, black 100%) |
| `knobShadowLifted` | inset (0,1,0,0, white 30%); (0,12,14,0, black 60%); (0,2,3,0, black 100%) |
| `panelShadow` | inset (0,1,0,0, white 22%); inset (0,-1,0,0, black 50%); ring (0,0,0,1, `panelRing`); (0,14,24,0, black 60%) |
| `panelWell` | inset (0,2,6,0, black 50%), on an inner rectangle inset 4 dp, corner radius 12 |
| `slotShadow` | inset (0,2,3,0, black 100%); (0,1,0,0, white 15%) |
| `trackShadow` | inset (0,2,3,0, black 100%); (0,1,0,0, white 14%) |

Rule: a part that moves casts a shadow whose offset grows while it is lifted (see knob). Shadows are never colored except the lamp glows in section 6.

## 6. Components

All coordinates are in dp relative to the screen unless stated. `railY` is defined in 7.2.

### 6.1 Top bar `APPROVED`

Height 60. Horizontal padding 26. Left: dot 10 dp circle `accent`, gap 8, then wordmark "ENK". Right: two lines of meta text, right-aligned: line 1 is the page position (`"{chars} / 3000"` in the prototype; in the product, `"p.2 · 140 / 3000"` style, see plan Task 15), line 2 is a short status. Background: none (sits on the chassis). In the product the status line is empty unless a transient message is showing; the prototype's "SPIKE" text is not shipped.

### 6.2 Rail `APPROVED`

| Part | Spec |
|---|---|
| Rod | full screen width, height 12, vertical center at `railY`, fill `railSteel`, shadow `railShadow` |
| Brackets | two, one at each screen edge: width 12, height 24, top at `railY - 12`, corner radius 3, fill `bracketSteel` |

Note: the bracket's top is 6 dp above the rod's top edge, so the bracket is centred on the rod (rod top `railY - 6`, bracket top `railY - 12`, bracket bottom `railY + 12`). There is no ruler, tick scale, or numbering anywhere on or under the rail. Do not add any.

### 6.3 Carriage (marker) `APPROVED`

A single draggable object centred on the rail at column `c`: its centre x is `textLeft + (c + 0.5) * columnWidth` (7.2).

| Part | Spec |
|---|---|
| Body | 54 x 36 dp, left of centre 27, top at `railY - 17`, corner radius 9, shadow `capShadow` |
| Body fill | three layers: `collarTop` as a 5 dp band along the top; `collarBottom` as a 5 dp band along the bottom; `graphiteCap` between |
| Grip grooves | on the body, left/right inset 11, bottom inset 10, height 8, corner radius 2; vertical lines white 16%, each 1.5 dp wide, repeating every 5 dp |
| Indicator strip | 16 x 3 dp, horizontally centred, top inset 10 dp, corner radius 2, fill `accent`, 1 dp ring black 35%. Night: add glow (0,0,8,2, `accent` 70%) |
| Slot window | 18 x 11 dp, centred, sits on the body's top edge: left of centre 9, top at `railY - 23`; corner radii 3 top, 0 bottom; fill `slotDark`; shadow `slotShadow`; clips its contents. Night: fill = lamp color and glow (0,0,14,5, lamp color 75%) |
| Type-slug | 14 x 12 dp, centred in the slot, left of centre 7, top at `railY - 21`, corner radii 2 top, 0 bottom, fill `slugSteel`; the typed character is printed on its face (bold Courier Prime 9 sp, `slugLetter`); clipped to the slot window. Night: 1 dp ring black 25% |

States and motion (see section 9 for curves):

- **Rest:** slug sunk at translateY +9 dp inside the slot (only its top edge visible).
- **Key strike:** the slug travels up to translateY -5 dp at 45% of the animation and returns to +9, in 110 ms. The slug never travels higher than -5 dp, so it never touches the caret (which sits above the slot).
- **Typing move:** carriage glides to the next column in 110 ms.
- **Drag:** while the finger is down and has moved more than 6 dp, the carriage follows the finger with no animation.
- **Release (snap):** after a drag, it returns to the caret column in 400 ms with overshoot.
- **Long-press:** touch held 480 ms without moving more than 6 dp toggles the lamp; one 14 ms haptic and the lamp-toggle sound.
- **Hit area:** the interactive area is at least 54 x 48 dp, centred on the body.

### 6.4 Caret `APPROVED`

An underline beneath the next character: width 1 character cell, height 2 dp, fill `accent`. Vertical position: top at `lineTop + 1.42 * fs` (below the glyphs, in the clear gap above the carriage). It blinks: 1000 ms cycle, two steps, at 50% the alpha drops to 0.15. It stops blinking and stays at full alpha while the user is typing or dragging, and while reduced motion is on.

### 6.5 Paper text and stamp `APPROVED`

- Text layout and geometry: section 7.
- Stamp animation on the character just typed: 160 ms ease-out, from offset (0,-2 dp), scale 1.35, alpha 0.2, blur 1.5 dp to offset 0, scale 1, alpha final, blur 0. Only the newest character stamps; pasted text appears without stamps.
- Struck-through letters (strike-through mode): a 1.5 dp horizontal line through the middle of the glyph in the same color, opacity 0.9; the glyph itself keeps its jitter. PROPOSED: confirm look with the author.

### 6.6 Beam and halo `APPROVED`

Shown only when the lamp is on.

| Part | Spec |
|---|---|
| Beam shape | trapezoid rising from the slot: bottom edge from 44% to 56% of the beam width, top edge the full beam width; height 360 dp; bottom at `railY - 24` |
| Beam width | `beamWidth = 70 + beamSlider * 2.6` dp (slider 30..100; default 55 gives 213 dp), centred on the carriage x |
| Beam color | vertical gradient from the bottom: lamp color at alpha `0.55 * intensity` to alpha 0 at 92% of the height |
| Beam softening | blur 5 dp, blend mode Screen |
| Halo | circle 160 dp diameter centred at (carriage x, `railY - 26`); radial gradient lamp color at alpha `intensity * 0.55` fading to transparent at 65% of the radius; blend mode Screen |
| Text lighting | the text layer is masked by an ellipse of horizontal radius `0.8 * beamWidth` and vertical radius 250 dp, centred at (carriage x - 12, 90 dp above the halo centre); alpha 1.0 at the centre and `0.1 + 0.1 * intensity` at the edge |
| Fade | beam and halo fade in/out over 600 ms (`themeFade`) |

`intensity = intensitySlider / 100` (slider 20..100, default 80).

### 6.7 Lamp control panel `APPROVED`

A recessed hardware box at the bottom of the screen, visible only while the lamp is on.

| Part | Spec |
|---|---|
| Outer box | left and right inset 26, bottom inset 30, padding 14 top / 18 sides / 16 bottom, corner radius 16, fill `panelFace`, shadow `panelShadow` |
| Inner well | rectangle inset 4 on all sides, corner radius 12, shadow `panelWell` (it only draws the inner shadow; it is not a separate fill) |
| Rows | three rows in the order Warmth, Intensity, Beam; each 36 dp tall; gap between children 12; no divider lines |
| Row content | label (60 dp wide) then fader (fills) then value (22 dp wide, right-aligned) |
| Label | uppercase `WARMTH`, `INTENSITY`, `BEAM`; style per section 3; engrave shadow: (0,1,0, white 8%) and (0,-1,0, black 60%) |
| Value | the slider position as an integer percent 0..100 of its range |
| Defaults | warmth 25, intensity 80, beam 55 (these are the stored slider values; the displayed percent of Intensity is therefore 75 and of Beam 36, because their ranges start at 20 and 30) |

Open/close motion: the panel enters from `translateY +40 dp, scale 0.97, alpha 0` to rest in 560 ms with `EnkMotion.springPanel` while alpha reaches 1 in 350 ms; the three rows enter from `translateY +10 dp, alpha 0` in 400 ms (alpha) and 550 ms (translate, `springRow`), delayed 120, 200, and 280 ms. Closing reverses without the stagger. The hint text under the panel is hidden while the lamp is on.

### 6.8 Fader `APPROVED`

| Part | Spec |
|---|---|
| Hit area | height 32 dp (enlarge to 48 dp touch height without changing the visual), full width of its row slot |
| Track | top at 12 dp from the fader top, height 8, corner radius 5, fill `trackDark`, shadow `trackShadow`, clips its fill |
| Fill | from the track's left edge to the knob centre; horizontal gradient from lamp color at alpha 0.3 to lamp color at alpha 1.0; glow (0,0,8,0, lamp color at alpha 0.5) |
| Knob | 24 x 26 dp, top at 3 dp, corner radius 7, same three-layer body as the carriage (collars 4 dp tall instead of 5, `graphiteKnob`), shadow `knobShadow` |
| Knob index line | 2 x 12 dp vertical, horizontally centred, top inset 7, corner radius 1, fill `accent`, 1 dp ring black 30% |
| Knob grooves | left/right inset 5, bottom inset 7, height 3; vertical lines white 18%, 1 dp wide, repeating every 3 dp |
| Travel | the knob's left edge moves over `width - 24` dp |
| Detents | 20 steps (position is rounded to the nearest 1/20 of travel) |

Interaction: on touch-down, the knob jumps to the touch x, lifts (`translateY -2 dp`, scale 1.07, `knobShadowLifted`) and stays lifted while held; on release it settles back in 300 ms with `springKnob`. Each time the value crosses to a new detent: play the fader tick (section 10) and a 4 ms haptic. No tick when the value does not change.

### 6.9 Hint text `APPROVED`

Centered, bottom inset 22, 10 sp Courier Prime, `textMuted`: "drag marker to scrub · hold marker for lamp". Shown only until the user has used both gestures once (stored in preferences), and never while the lamp is on. PROPOSED: the once-only rule.

## 7. Layout geometry (derived)

### 7.1 Columns and type size

```
screenWidth   = W dp                               // full screen width
textLeft      = 36 dp                              // 12 paper inset + 24 inner padding
textWidth     = W - 72
fontScale     = system font scale, clamped to 0.85..1.3
cols          = clamp( round(28 / fontScale), 18, 28 )
fs            = min( textWidth / (cols * 0.6), 22 )     // sp
columnWidth   = 0.6 * fs
lineHeight    = 1.7 * fs
```

Reference: on W = 400 dp, fontScale 1.0: cols 28, textWidth 328, fs 19.52 sp, columnWidth 11.71. If `fs` hits the 22 sp cap (very wide screens), the text block is `cols * columnWidth` wide and is centred between the paper edges; the rail, carriage, and ruler-free track use that same block. (This cap and the font-scale rule are PLAN decisions, not seen by the author.)

### 7.2 Vertical layout

| Quantity | Value |
|---|---|
| Paper top | 64 dp |
| `railY` (centre of the rod) | `round(H * 0.62)` where H is the full screen height |
| Current line's bottom edge | `railY - 23` dp |
| Text block top (in paper coordinates) | `railY - 64 - 23 - (line + 1) * lineHeight`, where `line` is the zero-based line of the caret |
| Feed animation | when the caret's line changes, the text block slides with 200 ms `EnkMotion.feed` |

The writing screen's visual stack, back to front: chassis, paper (text, grain, curl), beam and halo, rail, carriage, top bar, panel, hint. The carriage and rail draw above the paper and its shadow.

### 7.3 Column mapping

Caret at index `i` on line `L` has column `col = min(i - L.start, cols)`. The carriage centre is `textLeft + (col + 0.5) * columnWidth`. Wrapping: greedy at word boundaries; a word longer than `cols` breaks at `cols`; a line ends after the trailing space of the last word that fits; a hard newline always ends a line; an empty paragraph is an empty line.

### 7.4 Scrub mapping

While dragging, the finger x clamps to `[textLeft, textLeft + cols * columnWidth]`, and the caret index is `round(fraction * pageLength)` within the current page. The carriage follows the finger; on release it snaps (400 ms) to the caret's column.

## 8. Writing-screen behavior reference

| Event | Result |
|---|---|
| Key typed | slug strikes with the character, key sound plays, character stamps, carriage advances one column, 8 ms haptic (section 10) |
| Space / Enter | space sound or return sound; Enter also moves the paper up one line |
| Col reaches `cols - 4` for the first time on a line | bell plays once for that line; resets when the caret moves to another line |
| Backspace, clean-erase mode | character removed; no animation except the carriage gliding back |
| Backspace, strike-through mode | character stays, gets the strike mark, caret moves back; counts toward page size |
| Tap an earlier line | caret moves there; paper feeds so that line sits above the rail |
| Lamp toggled | see 6.3, 6.6, 6.7 |

## 9. Motion (`EnkMotion`)

Cubic-bezier values are `(x1, y1, x2, y2)`. Implement as `CubicBezierEasing`, or as the nearest spring if a spring is needed for interruption and note the equivalent here.

| Token | Duration | Curve |
|---|---|---|
| `feed` | 200 ms | (0.25, 0.8, 0.3, 1.0) |
| `carriageGlide` | 110 ms | (0.2, 0.9, 0.3, 1.15) |
| `carriageSnap` | 400 ms | (0.3, 1.7, 0.5, 1.0) |
| `slugStrike` | 110 ms | (0.2, 0.9, 0.3, 1.0); keyframes 0%: +9 dp, 45%: -5 dp, 100%: +9 dp |
| `stamp` | 160 ms | ease-out |
| `caretBlink` | 1000 ms loop | two-step; alpha 1.0 then 0.15 |
| `themeFade` | 600 ms | linear |
| `springPanel` | 560 ms | (0.3, 1.35, 0.5, 1.0) |
| `springRow` | 550 ms | (0.3, 1.4, 0.5, 1.0) |
| `rowAlpha` | 400 ms | linear |
| `panelAlpha` | 350 ms | linear |
| `rowStagger` | delays 120, 200, 280 ms | per row, top to bottom |
| `springKnob` | 300 ms | (0.3, 1.7, 0.5, 1.0) |
| `knobLift` | 200 ms | ease-out (shadow only) |
| `longPress` | 480 ms threshold, 6 dp slop | |

**Reduced motion** (system "remove animations" on, or the in-app "reduce effects" switch): replace `carriageSnap`, `springPanel`, `springRow`, `springKnob`, `slugStrike`, and `stamp` with 100 ms linear fades or no animation; stop caret blinking; keep `feed` at 100 ms. Reduce effects also turns off the grain (opacity 0), the beam blur, and the halo's Screen blend (use plain alpha).

## 10. Sound and haptics

Production sound uses sampled recordings (plan Tasks 11 and 12). The prototype's synthesized clicks only illustrate timing.

| Event | Sound | Haptic |
|---|---|---|
| Letter key | set's `key` sample, random pick among its variants, no repeat twice in a row, slight random pitch variation within +/-3% | 8 ms |
| Space | `space` sample, about 3 dB quieter than a letter | 14 ms |
| Enter | `return` sample | 14 ms |
| Backspace | `backspace` sample | 8 ms |
| Line nearly full (`cols - 4`, once per line) | `bell` sample, volume = bell volume setting (default 60%) | none |
| Lamp toggled | one `space`-style low click | 14 ms |
| Fader detent | tick: a 20 ms noise burst with a fast decay, band-passed at `2200 + p * 18` Hz (p is the fader position 0..100), Q 2.5, plus a 140 Hz sine thump decaying over 30 ms; or a sample if one is cut | 4 ms |
| Carriage snap on release | none | none |

Rules: sounds use audio usage "assistance sonification" so they never steal audio focus; mute turns all of them off; haptics follow the system touch-feedback setting and have their own switch. Latency from key event to sound start is at most 50 ms on the reference device.

## 11. Components checklist for golden tests

Each approved component has a screenshot golden (JVM, day and night where it differs) compared to a reference rendered from the prototype with the tolerance in the plan (Task 3). The inventory: rail with brackets; carriage rest; carriage with slug up; slot glowing (night); caret; stamped character mid-animation (frame at 80 ms); beam and halo at default settings; panel at rest; fader at 0%, 25%, 100%; knob lifted; top bar.

## 12. Interface shell and themes (everything outside the writer)

Status: **APPROVED** by the author on 2026-11-07. Reference mockups: `docs/mockups/enk-screens-v4-themes.html` (interactive) and `docs/mockups/enk-themes-overview.html` (all 12 theme and mode combinations). Where this section and the mockup disagree, this section wins. Sections 1 to 11 describe the writer and are not affected by themes. The old "components not yet designed" list is replaced by this section.

### 12.1 Two languages, one app

The writer is vintage: steel, graphite, paper, and the orange caret and indicator (sections 1 to 11). The shell (Box, Find, Trash, Setup, sheets, menus, alerts, widget) is restrained and modern: translucent glass only on the floating layer, opaque content, system typography, springs, and one accent that belongs to the active theme. The two meet in two places: the opaque **paper note cards** (cream, grain, Courier Prime) and the **Write** nav icon, which carries a tiny accent strip.

Shell rules:
1. Glass appears only on floating chrome: the nav bar, the divider strip, the compact title bar, sheets, menus, alerts. Content (cards, grouped lists) is opaque.
2. Never stack a translucent surface on another translucent surface. Toggles, segmented controls, and faders sit on opaque grouped rows.
3. Material thickness encodes hierarchy: bigger surfaces are thicker (stronger blur, deeper shadow).
4. No decorative shine, rim gradients, tilt, or squash-and-morph. Radii come from the scale in 12.6.
5. Text on translucent surfaces uses the primary or secondary text color with weight 500, never a pale grey.
6. Every theme has exactly one accent (`ui`). Accent is used for: selected-row index marks, toggle "on", fader fill, caret in text fields, the primary button fill, the Write icon strip, and the selected theme ring. Nothing else.

### 12.2 Themes (`ShellColors`)

Five themes plus Dynamic. Each has a dark and a light mode. The user picks a theme and a mode (Light, Dark, System) in Setup > Appearance; defaults are **Graphite** and **System** (plan decision). A theme replaces only the tokens below. Colors are hex; alpha values are shown as `rgba`.

**Dark mode**

| Token | Graphite | Apricot | Sky | Slate | Mono |
|---|---|---|---|---|---|
| `bg` | `#161514` | `#1C120F` | `#1B1413` | `#0D0F11` | `#0B0B0B` |
| `g1` (grouped surface) | `#201F1D` | `#2A1A15` | `#2A201E` | `#171B1F` | `#1A1A1A` |
| `thick` (sheet, alert, menu fill) | `rgba(38,36,34,.66)` | `rgba(46,28,22,.70)` | `rgba(42,32,31,.70)` | `rgba(26,31,36,.70)` | `rgba(30,30,30,.70)` |
| `hair` (separators) | `rgba(255,255,255,.08)` | `rgba(255,218,185,.10)` | `rgba(193,219,232,.10)` | `rgba(229,228,226,.09)` | `rgba(255,255,255,.09)` |
| `t1` | `#F3EEE3` | `#FFDAB9` | `#FBF1C9` | `#E5E4E2` | `#F2F2F2` |
| `t2` | `#A9A398` | `#C9A58B` | `#B7C8D2` | `#9AA5AD` | `#9C9C9C` |
| `t3` (hints only) | `#77726A` | `#8F6F5D` | `#7D8A92` | `#66727B` | `#6A6A6A` |
| `ui` (accent) | `#FF5A1F` | `#E2725B` | `#C1DBE8` | `#8FA6B8` | `#F2F2F2` |
| `uiInk` (text on accent) | `#1A0A03` | `#2A1410` | `#1B1413` | `#0A0A0A` | `#0B0B0B` |
| `danger` | `#FF8A7D` | `#FF9A86` | `#FF9D8F` | `#FF948A` | `#FF8A80` |
| `auraTop` | `rgba(255,190,130,.09)` | `rgba(226,114,91,.30)` | `rgba(193,219,232,.26)` | `rgba(83,104,120,.40)` | `rgba(255,255,255,.06)` |
| `auraSide` | transparent | `rgba(78,44,35,.85)` | `rgba(67,47,46,.90)` | `rgba(10,10,10,.90)` | `rgba(255,255,255,.03)` |
| `auraBottom` | transparent | `rgba(255,218,185,.07)` | `rgba(255,240,181,.10)` | `rgba(229,228,226,.05)` | `rgba(255,255,255,.03)` |

**Light mode**

| Token | Graphite | Apricot | Sky | Slate | Mono |
|---|---|---|---|---|---|
| `bg` | `#ECE8DF` | `#FFEBDC` | `#FFF6D4` | `#E5E4E2` | `#EEEEEE` |
| `g1` | `#F7F4EC` | `#FFF7F0` | `#FFFBEA` | `#F3F2F0` | `#FAFAFA` |
| `thick` | `rgba(247,244,236,.78)` | `rgba(255,244,235,.76)` | `rgba(255,251,234,.78)` | `rgba(243,242,240,.78)` | `rgba(250,250,250,.78)` |
| `hair` | `rgba(43,40,36,.12)` | `rgba(78,44,35,.12)` | `rgba(67,47,46,.12)` | `rgba(10,10,10,.10)` | `rgba(0,0,0,.10)` |
| `t1` | `#2B2824` | `#4E2C23` | `#432F2E` | `#0A0A0A` | `#111111` |
| `t2` | `#6B665C` | `#85584A` | `#7A6361` | `#4F5A63` | `#6B6B6B` |
| `t3` | `#8A8375` | `#A97D6A` | `#9F8984` | `#758088` | `#868686` |
| `ui` | `#EE5416` | `#D4634C` | `#5E92B2` | `#536878` | `#111111` |
| `uiInk` | `#1A0A03` | `#2A1410` | `#1B1413` | `#FFFFFF` | `#FFFFFF` |
| `danger` | `#B3261E` | `#B3261E` | `#B3261E` | `#B3261E` | `#B3261E` |
| `auraTop` | `rgba(255,150,90,.20)` | `rgba(226,114,91,.38)` | `rgba(193,219,232,.95)` | `rgba(83,104,120,.34)` | `rgba(255,255,255,.80)` |
| `auraSide` | `rgba(255,255,255,.45)` | `rgba(255,218,185,.95)` | `rgba(193,219,232,.60)` | `rgba(83,104,120,.20)` | `rgba(0,0,0,.03)` |
| `auraBottom` | `rgba(200,190,170,.45)` | `rgba(255,218,185,.90)` | `rgba(255,240,181,.95)` | `rgba(255,255,255,.70)` | `rgba(255,255,255,.60)` |

Source palettes: Apricot is Espresso `#4E2C23`, Burnt Peach `#E2725B`, Soft Apricot `#FFDAB9`. Sky is Deep Mocha `#432F2E`, Pale Sky `#C1DBE8`, Vanilla Custard `#FFF0B5`. Slate is Onyx `#0A0A0A`, Blue Slate `#536878`, Alabaster Grey `#E5E4E2`. Graphite is the original Enk interface: warm graphite, cream paper, orange. Where a source color was too dark or too light to work as an accent or text, the derived value above is used (for example Blue Slate lightens to `#8FA6B8` in dark mode; in light mode, `t3` is darkened to meet the 3.0:1 contrast minimum on `bg`, and `uiInk` on mid-tone accents `#EE5416`, `#D4634C`, `#5E92B2` uses the theme's dark ink to meet 4.5:1).

**Mode material tokens** (shared by all themes; they change with dark or light only)

| Token | Dark | Light |
|---|---|---|
| `glassA` / `glassB` (glass fill, top to bottom) | `rgba(255,255,255,.12)` / `.05` | `rgba(255,255,255,.66)` / `.36` |
| `glassEdge` (0.5 dp top highlight) | `rgba(255,255,255,.42)` | `rgba(255,255,255,.95)` |
| `glassLine` (0.5 dp outline) | `rgba(255,255,255,.10)` | `rgba(0,0,0,.06)` |
| `sh1` / `sh2` (shadow tints) | `rgba(0,0,0,.30)` / `rgba(0,0,0,.50)` | `rgba(60,40,20,.14)` / `rgba(60,40,20,.22)` |
| `lensA` / `lensB` / `lensEdge` | `rgba(255,255,255,.22)` / `.11` / `.55` | `rgba(255,255,255,.98)` / `.80` / `1.0` |
| `track` (toggle off, fader track) | `rgba(255,255,255,.12)` | `rgba(0,0,0,.13)` |
| `field` (segmented, search) | `rgba(255,255,255,.08)` | `rgba(0,0,0,.055)` |
| `field2` (text field in a sheet) | `rgba(0,0,0,.28)` | `rgba(0,0,0,.06)` |
| `scrim` | `rgba(8,7,6,.50)` | `rgba(20,12,6,.30)` |
| `grab` (sheet grabber) | `rgba(255,255,255,.28)` | `rgba(0,0,0,.22)` |
| `cardShadow` | `rgba(0,0,0,.50)` | `rgba(70,40,20,.28)` |
| `auraGrain` opacity | 0.10 | 0.16 |

Constants that do not change with the theme: paper `#F2ECDF`, card body ink `#5D564A`, card meta `#8A8272`, paper-pill button fill `rgba(32,30,28,.92)` with text `#F3EEE3`, writer orange `#FF5A1F` (writer only).

**Background (aura).** Behind every non-writer screen: `bg`, then three radial gradients, then the grain tile (blend Overlay at `auraGrain`). Gradient 1: ellipse 420 x 380 dp centred at 88% x, 2% y, `auraTop` fading to transparent at 72%. Gradient 2: 360 x 420 dp at -8% x, 46% y, `auraSide`. Gradient 3: 440 x 340 dp at 70% x, 104% y, `auraBottom`. The aura cross-fades over 500 ms when the theme changes.

### 12.3 Dynamic color (Material You)

On Android 12 and later (always, since the minimum is API 31) the Dynamic theme reads the system scheme (`dynamicDarkColorScheme` or `dynamicLightColorScheme`) and maps it to `ShellColors`:

| Token | Dark source | Light source |
|---|---|---|
| `bg` | `surface` | `surface` |
| `g1` | `surfaceContainer` | `surfaceContainerLow` |
| `thick` | `surfaceContainerHigh` at 70% | `surfaceContainerLowest` at 78% |
| `hair` | `outlineVariant` at 50% | `outlineVariant` at 50% |
| `t1` / `t2` | `onSurface` / `onSurfaceVariant` | same |
| `t3` | `onSurfaceVariant` at 65% | same |
| `ui` / `uiInk` | `primary` / `onPrimary` | same |
| `danger` | `error` | `error` |
| `auraTop` / `auraSide` / `auraBottom` | `primary` at 30% / `tertiaryContainer` at 50% / `primaryContainer` at 7% | `primary` at 38% / `tertiaryContainer` at 60% / `primaryContainer` at 80% |

Contrast guard: after mapping, any pair in 12.11 that falls below its minimum is corrected by moving the foreground's lightness in 2% steps (toward lighter on dark backgrounds, darker on light) until it passes. In the mockup a hue stands in for the wallpaper (the Setup screen's five sample dots); that stand-in formula is a reference for the look only and is never shipped.

### 12.4 Materials

| Material | Used by | Fill | Blur | Saturation | Shadows (dp) |
|---|---|---|---|---|---|
| Glass (regular) | nav bar, divider strip, glass buttons | vertical gradient `glassA` to `glassB` | 24 dp | 1.5 | inset top 0.5 `glassEdge`; inset outline 0.5 `glassLine`; (0,1,2) `sh1`; (0,14,30, spread -8) `sh2` |
| Thick | sheet, alert, context menu | `thick` | 44 dp | 1.6 | inset top 0.5 `glassEdge`; inset outline 0.5 `glassLine`; (0,2,6) `sh1`; (0,30,60, spread -12) `sh2` |
| Lens | selected item inside nav, strip, segmented | gradient `lensA` to `lensB` | none | n/a | inset top 0.5 `lensEdge`; (0,1,3) `sh1`; (0,6,14, spread -4) `sh2` |
| Compact bar | collapsed large title | gradient `bg` at 84% to 50% (at 70% height) to 0% | 22 dp, saturation 1.4 | | none; the bar's bottom 30% fades out with a mask |
| Strip veil | sticky strips when scrolled | gradient `bg` at 72% to 0% | 18 dp | | none; bottom 40% fades with a mask |

The veil and compact bar opacity follow scroll progress `p = clamp((scrollY - 30) / 36, 0, 1)` (dp). At `p = 0` they are invisible; there are no hard divider lines anywhere.

**Reduce effects / reduced transparency:** every glass, thick, compact bar, and veil surface becomes a solid `g1` with no blur and no gradient. The aura grain turns off. Behavior is otherwise identical.

### 12.5 Typography (shell)

System sans (`FontFamily.Default`) except where noted; sizes in sp, line height in sp, tracking in em.

| Role | Size / line | Weight | Tracking | Color |
|---|---|---|---|---|
| Large title | 34 / 38 | 700 | -0.034 | `t1` |
| Compact title | 17 / 22 | 600 | -0.012 | `t1` |
| Large-title meta (Courier Prime) | 11 | 400 | 0 | `t2` |
| Section header (uppercase) | 12.5 | 600 | +0.04 | `t2` |
| Section footer | 12.5 / 17 | 400 | 0 | `t2` |
| List row | 16 | 400 | -0.012 | `t1` |
| List row value | 15 | 400 | 0 | `t2` |
| Nav label | 10.5 | 500 | +0.01 | `t2`, `t1` when selected |
| Strip item | 13 | 500 | -0.005 | `t2`, `t1` when selected |
| Button | 14 (small 13) | 500 | -0.005 | `t1` |
| Segmented item | 13 | 500 | 0 | `t2`, `t1` when selected |
| Sheet title | 20 | 700 | -0.022 | `t1` |
| Sheet / alert message | 13.5 / 13 | 400 | 0 | `t2` |
| Alert title | 17 / 22 | 600 | -0.015 | `t1` |
| Menu row, text field | 16 / 17 | 400 | 0 | `t1` |
| Hint (Courier Prime) | 11 | 400 | 0 | `t3` |
| Card title (Courier Prime) | 14 / 1.35 | 700 | 0 | `ink` |
| Card body (Courier Prime) | 12 / 1.45 | 400 | 0 | `#5D564A` |
| Card meta (Courier Prime) | 10.5 | 400 | 0 | `#8A8272` |

Text matched by search inside a card is set in weight 700, color `#14110D`, with a 1.5 dp underline of the same color. There is no highlight color.

### 12.6 Radii, spacing, elevation

- **Radii (dp):** 14 cards and text fields; 16 menu; 18 grouped lists; 22 divider strip, search field, alert; 26 widget (paper); 30 sheet top corners and small glass widget; 32 nav bar; full pill for buttons, lens, toggle, segmented. The screen corner radius shown in mockups (44) is the device's.
- **Spacing (dp):** screen side margin 20; list gap between cards 12; card padding 14 top, 16 sides, 12 bottom; grouped list margin 20 sides, row min height 52, row side padding 16, separators inset 16 from the left; section header padding 22 top, 8 bottom, 28 sides; footer padding 4 top, 28 sides.
- **Elevation:** cards use inset top highlight 0.5 white 60%, (0,1,2) black 25%, (0,8,18, spread -8) `cardShadow`. A lifted card (held for its menu) scales 1.03 and uses (0,24,50, spread -10) black 60%.

### 12.7 Page layout

Every non-writer screen is a scroll container with these parts, back to front: aura background, scroll content, compact bar, nav bar.

- **Large title:** top padding 70 (below the status bar inset), sides 20, bottom 12. Title left, Courier meta right on the baseline with 6 bottom padding.
- **Sticky strip:** the divider strip (Box) and the search field (Find) sit under the title and pin at `top = 50` below the compact bar when scrolled; their veil appears with `p`.
- **Compact bar:** 58 dp tall, title centered at 26 dp from the top, opacity `p`.
- **Scroll end padding:** 120 dp so the last item clears the nav bar.
- **Nav bar:** floating, left and right inset 22, bottom inset 22, height 64, radius 32, padding 5, glass. The writer screen has no nav bar.

### 12.8 Components

**Nav bar.** Four destinations in order: Write, Box, Find, Setup. Each is 22 dp line icon (stroke 1.6, round caps) above a 10.5 sp label, 3 dp apart. The lens (see 12.9) sits behind the selected item with 5 dp padding from the bar. Icons: Write is a rail with a small carriage whose strip is drawn in `ui`; Box is a rounded drawer with a tab line; Find is a magnifier; Setup is three slider lines with rings. Trash highlights Box.

**Divider strip.** Glass, height 44, radius 22, padding 4, horizontally scrollable. Items are 36 dp tall with 15 dp side padding; the last item is a 38 dp "+" that opens the new-divider sheet instead of selecting. A lens follows the selected item.

**Note card.** Paper `#F2ECDF`, radius 14, grain overlay (opacity 0.3, Multiply), elevation per 12.6. Content: title (one line, ellipsis), first line of body, and a footer with date left and page label right ("p.2"). Press: scale 0.985 in 120 ms. Hold 420 ms: lift and open the context menu.

**New-note ghost card.** No fill; 1 dp outline `hair`; radius 14; text "New note" 14 sp weight 500 `t2`, centered; padding 16. It is always first in a divider's list.

**Buttons.**
- Glass button: height 36 (small 30), side padding 18 (small 14), pill, glass material, label per 12.5. Press: scale 0.96 in 120 ms ease-out plus a 12% brightness lift, at pointer-down.
- Primary: fill `ui`, text `uiInk`, shadow (0,1,2) `sh1`; used once per sheet ("Add").
- Destructive: label color `danger`; never a filled button.
- Paper-pill (on a card): height 28, side padding 13, pill, fill `rgba(32,30,28,.92)`, text `#F3EEE3`, size 12.5 weight 500 (for Restore).

**Grouped list.** A group is a `g1` surface, radius 18, with an inset 0.5 dp top highlight `glassEdge` and (0,1,2) `sh1`. Rows are 52 dp minimum; separators are 0.5 dp `hair` inset 16 from the left. Section headers sit above, footers below.

**Toggle.** 52 x 32 dp track, radius 16, `track` when off and `ui` when on. Bead 28 dp circle, white, shadows (0,1,1) black 25% and (0,3,8) black 30%, 2 dp inset. On: bead travels 20 dp. While pressed the bead stretches to scaleX 1.12. Spring: response 0.28 s, damping 1.0.

**Segmented control.** Height 34, radius 17, padding 3, fill `field`; lens inside; items flex equally with 12 dp side padding. Draggable (12.9).

**Fader (bell volume).** Row of label left and fader right (fills the rest, 12 dp gap, 32 dp tall). Track 6 dp, radius 3, `track`; fill `ui` from the left to the knob center; knob 28 dp white circle with the same shadows as the bead; while held it scales 1.12. Continuous 1:1 tracking, no detents.

**Sound-set rows.** Four rows (Classic, Antique, Soft, Keyboard) in one group. The selected row shows an index mark on the right: 14 x 3 dp, radius 2, `ui`, scaling in from 0 to 1 horizontally over 350 ms with overshoot ease (0.2, 1.4, 0.4, 1).

**Theme tiles (Setup > Appearance).** A 3 x 2 grid (Graphite, Apricot, Sky / Slate, Mono, Dynamic), column gap 10, row gap 14, grid padding 14 top, 12 sides, 8 bottom. A tile is a 62 dp tall radius-16 swatch with a 0.5 dp `hair` outline and the label (12.5 sp weight 500) below. Swatch: for static themes a vertical gradient from the palette's dark color (solid for the first 18%) to its light color, with a radial glow of the mid color at 50% x, 78% y fading out at 68%; for Dynamic, a 165-degree gradient from a dark tone of the system primary through the primary to a light tone of the tertiary. Selected: label becomes `t1`, a 2 dp ring in `g1` plus a 1.5 dp ring in `ui` around the swatch, scale 1.03, and a 14 x 3 dp index mark under the label. Below the grid is a Light, Dark, System segmented control (row padding 8 x 16).

**Search field.** Height 44, radius 22, fill `field`, 16 dp side padding, a 18 dp magnifier icon in `t2`, then the text field (16 sp, caret `ui`). Placeholder "Search your notes".

**Context menu.** Thick material, width 232, radius 16, rows 46 dp, separators `hair`. The first row, "Move to divider", has a chevron in `t3`; the second, "Move to trash", uses `danger`. It opens 10 dp below the lifted card with its origin at the card's left edge, left-aligned to the card (at least 20 dp from the screen edge) and never lower than 600 dp from the top.

**Alert.** Thick, width 272, radius 22, centered. Title 17/22, message 13/18, 18 dp side padding, 20 top, 18 bottom. A button row below a 0.5 dp `hair` line: two equal cells 46 dp tall separated by a 0.5 dp vertical line; the destructive cell uses `danger` weight 600. Press highlight `field`.

**Sheet.** Thick, full width, bottom-anchored, height 320, top radius 30, padding 10 top and 20 sides. A grabber 36 x 5 dp, `grab`, centered, 14 below. Title 20 bold, message, a text field (48 dp, radius 14, `field2`, 17 sp, caret `ui`), then two buttons in a row, 10 apart: a glass Cancel and a primary Add, each 48 dp tall with radius 24 and 16 sp text. It drags down to dismiss (12.9).

**Scrim.** `scrim` fill; 300 ms fade; tapping it dismisses the topmost overlay.

**Hint.** "Hold a note for options" (Courier 11, `t3`, centered) at the end of a Box list.

### 12.9 Motion (shell)

Springs use Apple's model: **damping ratio** and **response** (seconds). For a mass of 1, `stiffness = (2π / response)²` and `damping coefficient = 4π × damping / response`. In Compose use `spring(dampingRatio = damping, stiffness = (2π/response)²)`. Default damping is 1.0 (no overshoot); use 0.8 only when the gesture carried momentum (a flick or drag release).

| Token | Response | Damping | Used for |
|---|---|---|---|
| `shellDefault` | 0.35 | 1.0 | lens moves, strip and nav selection |
| `shellFlick` | 0.35 | 0.8 | lens after a drag release faster than 350 dp/s |
| `shellToggle` | 0.28 | 1.0 | toggle bead |
| `shellSheet` | 0.38 | 0.82 | sheet open and dismiss |

Other motion: press feedback on pointer-down (button 0.96 / 120 ms ease-out; card 0.985 / 350 ms (0.2, 0.9, 0.3, 1); paper-pill 0.95 / 120 ms). Card entrance: translate 14 dp up and fade in over 500 ms (0.2, 0.9, 0.3, 1), staggered 40 ms per card. Context menu: scale from 0.6 to 1 over 400 ms (0.2, 1, 0.3, 1) with opacity over 200 ms, origin at the card's left edge. Alert: scale from 1.08 to 1 over 350 ms (0.2, 0.9, 0.3, 1). Scrim: 300 ms. Theme change: every color cross-fades over 500 ms. Screen change: cross-fade 250 ms.

**Lens (nav, strip, segmented).**
- Tap: spring the lens to the item's left and width (`shellDefault`).
- Drag (nav and segmented only): after 8 dp of movement the lens follows the finger 1:1, offset by where it was grabbed (if grabbed off the lens, it centers under the finger). Past either end, resistance: `rubber(overshoot, dimension, c = 0.55) = overshoot × dimension × c / (dimension + c × |overshoot|)`. The item under the lens centre brightens live (its label turns `t1`).
- Release: velocity v (dp/s) from the last 6 samples. Projected resting point = `x + project(v)` with `project(v) = (v / 1000) × d / (1 - d)` and `d = 0.998`. Pick the item whose centre is nearest to the projected lens centre. Spring to it starting at velocity v, using `shellFlick` when `|v| > 350`, else `shellDefault`.
- Stretch while moving: `k = min(0.14, |velocity| / 6000)`; scaleX = 1 + k, scaleY = 1 - k / 2.

**Sheet drag.** Follows the finger 1:1 downward from where it was grabbed; upward resistance via `rubber(y, 320)`. On release: dismiss if `y + project(v) > 160` dp, else spring back to 0; hand off the release velocity either way. The scrim fades out with a dismiss.

**Interruptibility.** Every spring re-targets from its live position and velocity; a gesture grabbing a moving object holds it where it is. Never use fixed-duration animations for gesture-driven motion.

**Reduced motion** (system animations off, or Reduce effects on): all springs snap to their targets; entrances and menus become 100 ms fades; the stretch is off; theme change is instant.

### 12.10 Screens

**Box.** Large title "The Box" with the note count on the right ("2 notes"). Divider strip (Unfiled, then the user's dividers, then "+"). List: the ghost card, then note cards. Hold a note: it lifts, the scrim appears, the context menu opens. Selecting a divider re-renders the list with the card entrance. The "+" opens the New divider sheet.

**Find.** Large title "Find" with the match count on the right ("4 matches", or "all notes" when empty). A pinned search field. Results are note cards with matches in the ink-weight style. Empty field: "Type to search your notes." No results: "Nothing found for “…”." Both centered in 14 sp `t3`.

**Trash.** Large title "Trash". Cards with the title struck through (1.5 dp line), a footer "26 days left" and a paper-pill Restore. Footer text below: "Notes in the trash are removed after 30 days." Then a centered glass destructive button "Delete all forever" that opens the Delete forever alert ("2 notes will be erased. This can’t be undone." / Keep, Delete).

**Setup.** Large title "Setup" with "Enk 1.0". Groups in order: **Appearance** (theme tiles, then the Light, Dark, System control; footer: "Themes colour menus and controls. The writing page always keeps its paper and steel. Dynamic follows your Android wallpaper."), **Writing** (Clean erase and Strike-through segmented; Autocorrect toggle; footer: "Strike-through keeps deleted letters on the paper, as a typewriter would."), **Sound** (the four sound-set rows; then a second group: Mute toggle, Bell fader, Haptics toggle), **Display** (Reduce effects toggle; footer: "Reduce effects turns off paper grain, glass blur, and extra motion."), **Data** (Export all notes, Import notes, each with a small glass button), **About** (Credits and licenses with a chevron, Version 1.0.0), and a centered footer "Free and open source · Apache-2.0 / No network, no tracking."

**Widgets.** Wide widget: paper card 360 x 160 dp, radius 26, grain, meta line "Recent · 7 Nov · p.2" (Courier 10.5), two lines of the note in Courier 15 / 1.55, and a paper-pill "New note" button bottom-right (14 dp insets). Small widget: 170 x 170 dp, radius 30, glass-style panel (solid graphite on the home screen because app widgets cannot blur what is behind them) with a miniature rail and carriage and a small glass-style "New note" button. Empty state: a 170 x 170 paper card, "Nothing yet / Tap to write your first note."

**Writer top bar.** Unchanged from 6.1. The wordmark opens the Box.

### 12.11 Accessibility

- Contrast minimums (WCAG): `t1` on `bg` and on `g1` at least 4.5; `t2` on `bg` and on `g1` at least 4.5; `uiInk` on `ui` at least 4.5; `danger` on `g1` and on `thick` at least 4.5; `ui` against `g1` at least 3.0 (indicators and fills); `t3` against `bg` at least 3.0 and it is for non-essential hints only. These are checked for every static theme and mode, and for Dynamic across a sweep of 12 evenly spaced seed hues in both modes.
- Touch targets are at least 48 dp: strip items and nav items are taller than 48 dp inside their glass, toggles and faders get a 48 dp hit height, buttons under 48 dp get invisible padding.
- Reduce effects, the system "remove animations" setting, and high-contrast text switch the shell to solid surfaces and snapped springs (12.4, 12.9). (Android has no system "reduce transparency" flag; the in-app Reduce effects switch covers it.)
- TalkBack: the nav bar is a tab row with selected state; the lens is decorative; the context menu opens from the standard "actions" gesture on a card as well as on hold.
- Text scales with system font size up to 130%; a 200% setting must not clip or overlap (large titles wrap to two lines).

### 12.12 Implementation notes (glass and expressive motion)

- **Glass rendering:** use the Backdrop library (io.github.kyant0:backdrop, Apache-2.0) for real refraction on Android 13 and later; on Android 12 and 12L (API 31 and 32) fall back to frosted blur (RenderEffect blur via Haze, Apache-2.0, or Backdrop's blur-only path). The look is defined by the table in 12.4 either way; refraction is an enhancement, never required. Confirm both libraries' current versions and licenses at build time and list them in the credits.
- **Material 3:** use `androidx.compose.material3` 1.5.x (the Expressive APIs, `MotionScheme`, and `ButtonGroup` graduated to stable in the 1.5 line; 1.4.0 stable does not include them). Do not rely on `LoadingIndicator` or `MaterialShapes`, which are still experimental. Pin the version and re-check the release notes when upgrading.
- **Widgets:** Glance cannot blur the home-screen wallpaper, so the glass widget is rendered as a solid panel that approximates the glass.

### 12.13 Still PROPOSED (need the author's approval before building)

The app icon artwork (a first sketch exists in `docs/mockups/enk-screens.html`, not restyled), the credits screen layout beyond a grouped list of rows, and any empty-state illustrations (none planned; empty states are text only).

## 13. Do and don't

Do: use the shared materials; round all dp values the way this document does; keep shadows; keep the carriage and rail always above the paper; test day and night for every new component.

Don't: add orange fills; add ruler or tick marks; use pure black (`#000`) or pure white (`#FFF`) as a flat fill; use Material 3 default components on the writing screen or panel (Material components are acceptable only for system-owned surfaces such as the file picker); animate text characters other than the stamp; add icons where text works; copy any Platen asset.

## 14. Change control

Any change to a value in this document needs: the new value, why, a screenshot, and the author's approval, and updates here and in the golden references in the same change.
