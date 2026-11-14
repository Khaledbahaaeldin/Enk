# Enk typing strike and liquid glass overhaul: spec

Date: 2026-11-14. Status: PROPOSED, waiting for the decisions in section 6.
Baseline: `main` at `f2e4404`. Mockup (open it first): `docs/mockups/enk-typing-and-liquid-glass.html`.

This spec extends `2026-11-14-enk-round-2-fixes.md` (sections 5, 7 and 8). Where the two differ on glass or typing, **this spec wins**; the effects levels (Auto, Full, Balanced, Light), the spring tokens and the screen-motion table in the round 2 spec stay.

Status words used here:
- **Measured**: a number I read from the mockup or the code.
- **Proposed**: a starting value to tune on a phone. None of the timings or sizes here has been seen on a phone.
- **Not verified**: I could not confirm it.

---

## 1. The typing strike

### 1.1 What happens today (measured)

- A new letter is drawn at once but **faded in and scaled down from 1.35** over 160 ms (`PaperText`: scale 1.35 to 1, alpha 0.2 to 1). In the mockup's "Today" style the glyph has fewer than 20 dark pixels until about 100 ms after the key and reaches full ink at 160 ms. It reads as a fade, not a strike.
- The slug is a 14 x 12 dp block that moves a few dp over 110 ms. It never reaches the paper.
- The carriage glides to the next column over 110 ms with no click.
- The paper does not react to a strike.
- Loaded text (opening a note, the start animation) fades in (`introAlpha`).

### 1.2 The strike (the proposal)

One key press, on a timeline in milliseconds from the key-down event. The sound and the haptic fire at 0, exactly as today.

| t (ms) | What happens | Values (Proposed) |
|---|---|---|
| 0 | The slug leaves its rest position in the carriage slot | rest at `railY - 12` dp |
| 0 to 26 | **Slug rises** to the paper line, accelerating | travel 17 dp, ease-in cubic (`t^3`) |
| **26** | **Impact.** The letter appears at once at full ink. It is not faded | glyph alpha = its existing per-glyph jitter alpha |
| 26 to 96 | Ink impression: the glyph is slightly large and dark, then settles | scale `1 + 0.10 * w` to 1 over 70 ms, ease-out cubic; colour 22% darker for the first 60 ms |
| 26 to 71 | Ink halo: a soft spread around the glyph that sharpens | blur radius `2.2 * w` dp to 0 over 45 ms |
| 26 to 96 | Impact flash on the paper under the slug | an ellipse with a vertical radius of 3 dp and a horizontal radius of 9 to 15 dp, alpha 0.35 to 0 |
| 26 | Paper thump: the sheet is nudged down and rings once | velocity impulse `55 * w` dp/s into a spring, response 0.09 s, damping 0.3; peak about `0.8 * w` dp. Applies to the paper's text and caret only, not the rail, carriage or top bar |
| 26 | **Carriage tick**: the carriage steps one column and overshoots slightly | spring, response 0.075 s, damping 0.5 |
| 26 to 110 | **Slug returns**, with a small rebound | ease-out cubic over 84 ms, 6% overshoot |

`w` is the key's weight (section 1.3). The numbers above are the ones in the mockup, so the behaviour you saw there is the behaviour specified here.

**Why 26 ms:** the sound is played on touch-down, and the phone's audio output adds about 20 to 40 ms before it is heard (Not verified on a device). Putting the impact at 26 ms makes the audible strike and the visible strike land together without any code that compares them. It is the first number to tune on a phone, within plus or minus 10 ms.

### 1.3 Weights

| Key class | `w` | Effect |
|---|---|---|
| Capitals and `@ # % & M W` | 1.4 | deeper ink, larger halo, a harder thump |
| Ordinary letters and digits | 1.0 | as the table |
| `. , ' ` : ; -` | 0.5 | light touch |
| Space | none | no slug and no impact (section 1.5) |

### 1.4 Fast typing

- If the previous strike was less than 95 ms ago, the new strike is "fast": halo times 0.3, thump times 0.6, slug rise shortened to 18 ms.
- Each glyph animates on its own, so overlapping strikes are independent.
- At most 8 glyphs animate at once (a ring of the newest 8). Older ones jump to their final look, so a very fast typist never queues work.

### 1.5 The other keys

| Key | Motion |
|---|---|
| Space | No slug. The carriage ticks one column, softer (response 0.09 s, damping 0.6). No thump |
| Enter | **Carriage return**: the carriage slides back to the left margin (response 0.24 s, damping 0.72, so it rebounds a few dp at the stop) while the paper feeds one line (response 0.22 s, damping 1.0). The margin bell rings as today |
| Backspace, Clean erase | The carriage steps back (tick). The glyph is **wiped left to right** in 90 ms by a 3 dp light bar, the part not yet wiped stays visible. No fade |
| Backspace, Strike-through | The slug strikes at 26 ms as for a letter, and the strike line is drawn across the glyph left to right over 40 ms from the impact |
| Automatic line wrap | The bell, then the same carriage return as Enter, 20% faster |
| Page break | Out of this spec; the paper roll is not changed |

### 1.6 Loaded text: the type-on

When a note opens, when a page changes, and in the start animation, the text is **typed on by the carriage**, not faded in.

- Each visible line is revealed by one carriage sweep. Line `i` starts at `120 + i * stagger` ms; the glyphs of a line appear at the carriage's x as it passes, over 112 ms; the carriage is back at the margin 85 ms after the sweep ends. Each revealed glyph gets the impression of section 1.2 (scale 1.08 to 1, darker for 60 ms) and no thump.
- `stagger = min(150, 900 / visibleLines)` ms, so the whole reveal takes at most about 1 second.
- A pasted insert of more than 1 character is revealed the same way, in one sweep of at most 320 ms.
- At the Light level, with Reduce effects, or with more than 2,000 visible characters, the text appears at once.

### 1.7 Implementation shape

- A pure function in `app.enk.writer`, in the style of `computePanelMotion`:
  `strikeFrame(msSinceKey: Float, weight: Float, fast: Boolean): StrikeFrame` with `slugRiseDp`, `glyphVisible`, `glyphScale`, `glyphDarken` (0 to 0.22), `haloDp`, `flashAlpha`, `flashWidthDp`. Everything else in 1.2 follows from it.
- `PaperText` draws the newest 8 glyphs from their `strikeAgeMs`; the old fade (`stampProgress`) is removed. The existing `stampFrame` parameter becomes `strikeMs` so goldens can freeze a frame.
- `TypeSlug` takes `msSinceKey` and draws the rise, the thin arm and the return; the carriage uses the tick spring; a `paperThump` spring offsets only the paper layer.
- The strike runs from the same `strikeKey` increment the writer already makes, so the in-app keyboard and the system keyboard both drive it.
- Effects levels: Full all of it; Balanced no halo and no flash, the thump stays; Light and Reduce effects: the glyph at once, no slug travel, no thump, the carriage snaps, the caret is solid.

### 1.8 Tests

- `strikeFrame` at 0, 13, 25.9, 26, 45, 96 and 130 ms: invisible before 26 and visible at 26; scale and darken monotonic after impact; weights 0.5, 1.0 and 1.4; fast typing; NaN and negative times give the rest state.
- Slug rise is monotonic to the impact and never exceeds 17 dp.
- A property test: 2,000 random key sequences never leave more than 8 animating glyphs.
- Goldens (Light level and frozen frames): `strikeMs` 14 (slug rising, glyph absent), 30 (glyph just struck), 60 (settling).
- The existing frame-time test must still pass on a physical phone.

---

## 2. Liquid glass

### 2.1 Principles

1. Glass is for **floating controls only**. Cards, paper, grouped lists and the writer's steel stay opaque (design system 12.1).
2. The glass that moves is a **lens**: it grows on touch, stretches with speed, and refracts what is under it.
3. One piece of glass changes shape instead of one piece closing and another opening: a button grows into its menu.
4. Never glass on glass, and never more refracting surfaces than the level allows (section 2.8).
5. Every glass surface keeps legible text: label colour from the shell tokens, medium weight or heavier.
6. Refraction is an enhancement. Balanced keeps blur and tint; Light is solid. The app never depends on refraction to be usable.

### 2.2 How it is drawn

On `Backdrop 2.0.1` (already in `main`). Facts from its documentation:

- Effects are applied in this order: colour, then blur, then lens.
- `lens(refractionHeight, refractionAmount, depthEffect, chromaticAberration)` needs a `CornerBasedShape` and **Android 13 or later**; `refractionHeight` must not exceed the shape's smallest corner radius; `refractionAmount` must not exceed the smaller side. Blur needs Android 12.
- Highlights, inner shadows and shadows exist as separate components, and a press scale belongs in the library's layer block, not `graphicsLayer`, so the backdrop does not slide under the glass.
- Not verified: the exact API of highlight and inner shadow in 2.0.1. The implementer reads its source and does not guess.

What the mockup shows, and the app reproduces with the lens: at the rim the picture behind the glass is bent toward the outside, strongest at the edge and falling to nothing a few dp inside, with a thin colour fringe on small lenses. The mockup draws it with an SVG displacement map; the app draws it with `lens` and `chromaticAberration`. They will not look identical; the mockup is the intent.

### 2.3 The glass kit (`:design`, package `app.enk.design.glass`)

One set of components replaces the scattered use of `Modifier.glass` and the hand-built controls in `ShellToggle`, `ShellFader`, `ShellSegmented` and `GlassNavBar`. Existing call sites move to it; nothing outside the kit builds glass.

| Component | What it is |
|---|---|
| `GlassSurface` | A shaped glass panel: body effects plus an optional lens. The base of all others |
| `GlassLens` | The small moving piece: press scale, stretch, refraction. Used by the nav, segmented control, toggle bead and slider thumb |
| `GlassButton` | A capsule that swells on touch |
| `GlassToggle` | A track with a lens bead |
| `GlassSlider` | A track with a lens thumb (also replaces the Bell fader and the lamp sliders) |
| `GlassSegmented` | Two to four items with a lens indicator |
| `GlassNavBar` | The bottom bar with a draggable lens |
| `GlassField` | The search field |
| `GlassMenu` | A button that grows into a menu (the morph in 2.5) |
| `GlassSheet`, `GlassAlert`, `GlassToast` | The sheets, the alert and the message pills |

### 2.4 Component values (Proposed)

The numbers are those of the mockup, in dp at one dp per mockup pixel. They replace the glass table in round 2 section 5.3 for these components.

| Component | Shape and size | Body | Lens (refractionHeight, refractionAmount) | Press |
|---|---|---|---|---|
| Nav bar | pill, 328 x 70, radius 35 | blur 8, tint | indicator lens 79 x 60, radius 30: 22, 46, blur 0.8, chromatic aberration | lens 1.08 |
| Segmented control | pill, 44 high, radius 22 | blur 8 | indicator 36 high, radius 18: 14, 30 | lens 1.08 |
| Toggle | track 70 x 44, radius 22 | blur 8, orange when on | bead 40 x 36, radius 18: 14, 28 | bead 1.16 |
| Slider | track 8 high; thumb 62 x 36, radius 18 | none | thumb: 16, 34 | thumb 1.14 |
| Button | capsule, 46 high, radius 23 | blur 4 | whole body: 14, 26 | 1.07 |
| Search field | capsule, 46 high, radius 23 | blur 8 | whole body: 12, 20 | field widens and grows 4 dp when focused |
| Menu, sheet, alert, pop-ups | radius 24 to 34 | blur 12, thick tint | edge only: 18, 34 | opens with the morph |
| Writer top-bar buttons | circle 44 | blur 14 | 10, 16 | 0.96 |
| Keyboard tray | top corners 22 | thick, solid at Light | none | none |

Lens stretch is the same everywhere: `k = min(0.14, |v| / 6000)` with `v` in dp/s along the movement; scale x by `1 + k` and y by `1 - k / 2`. Springs are the round 2 tokens (`press`, `default`, `flick`, `toggle`, `sheet`).

### 2.5 The morph

A button grows into its menu as **one surface**: its width, height, corner radius and position animate together with the `sheet` spring (response 0.38 s, damping 0.82) while the label fades out in the first quarter and the items fade and rise in one after another (from 35% of the progress, 8% apart). In the mockup, "Options" goes from 120 x 46 to 220 x 138 and rises 30 dp. In the app the lens parameters change smoothly with the size; nothing is rebuilt.

Used for: a button into its menu; the lamp button into the lamp pop-up; the Setup export and import buttons into their picker confirmation; the Trash "Delete all forever" button into its alert. The long-press context menu on a note instead grows from the touch point (scale 0.6 to 1), as in round 2.

### 2.6 Light that follows the finger (Full only)

The rim highlight on a glass piece points along the line from its centre to the last touch point while it is pressed or dragged, and rests at 135 degrees (top left) otherwise. It changes only while touched, so it costs nothing at rest.

### 2.7 Colour fringe

A small colour separation at the rim (red, green and blue sampled at slightly different strengths, 1.0, 0.94 and 0.88 in the mockup) on lenses up to 72 dp wide at Full only. It is what makes the nav lens and the beads read as glass. It is off on larger surfaces.

### 2.8 Budget by level

| | Full | Balanced | Light |
|---|---|---|---|
| Backdrop capture | one per screen | one per screen | none |
| Refracting surfaces at once | at most 2 large and 3 small lenses | none | none |
| Blur | yes | yes, at most 3 surfaces | none |
| Fringe and moving highlight | yes | no | no |
| Result | the mockup | blur and tint, same layout and motion | solid tinted surfaces, same layout |

Auto drops a level when frames are slow (round 2 section 5.2). The budgets are Proposed; they are measured with the frame-time test on a mid-range phone, which has not been done.

### 2.9 Where it appears

| Screen | Glass | Stays opaque |
|---|---|---|
| Writer | top-bar buttons (Box, lamp, keyboard), lamp pop-up and its sliders, the dark-paper question, the keyboard tray and its callouts, message pills | paper, rail, carriage, steel |
| Box | nav bar, divider strip and its lens, context menu, Move sheet, New divider sheet, Delete alert, large-title bar, message pill | note cards, the "New note" outline, Trash row |
| Find | nav bar, search field, large-title bar | result cards |
| Setup | nav bar, segmented controls, toggles, sliders, Export and Import buttons | theme tiles, grouped rows |
| Trash | nav bar, "Delete all forever" and its alert | note cards, Restore pills |
| Credits | the bar | the list |
| Start animation, widget | none (the widget cannot blur what is behind it) | all |

### 2.10 Tests

- Pure: lens stretch for 0, 3,000 and 20,000 dp/s and negative velocity; the morph interpolation at 0, 0.35, 0.5 and 1 for width, height, radius and item opacity; the specular angle for centre, edges and an untouched piece; the level decides refraction, blur and fringe exactly as 2.8.
- Compose: press scales a `GlassLens` on touch-down; dragging the nav lens follows the finger and a flick carries it; a toggle bead moves and the state changes; the menu morph opens and closes and a second tap during it lands correctly.
- Goldens only at the Light level (software rendering draws no blur or lens). Full and Balanced are checked by a fixed list of emulator screenshots and on phones.
- Frame time (p95 at most one frame interval) during the nav drag, the menu morph and a sheet drag, on a physical phone.

---

## 3. Order of work and who builds

| Phase | Content |
|---|---|
| T1 | The typing strike (section 1): self-contained, touches `PaperText`, `TypeSlug`, `Carriage`, the start animation |
| G1 | The glass kit and its tokens, with previews and Light goldens (section 2.3 and 2.4) |
| G2 | Shell controls on the kit: nav, segmented, toggle, slider, button, field |
| G3 | Floating layers: menu morph, sheets, alert, toast, the lamp pop-up |
| G4 | Screen integration, removal of the old controls, the effects-level setting, emulator screenshots |

T1 and G1 do not touch the same files and can be built at the same time. G2 to G4 follow G1 in order. This sits after the keyboard batch (`prompt-12`) and the other round 2 phases that are not replaced here. The round 2 phases R2-C and R2-D (glass material v2 and motion on every interaction) are replaced by G1 to G4; R2-E and R2-F (the lamp and its pop-up) use the kit.

The author asked for the keyboard in one batch. The same can be done here: one prompt per track (T1 and G1 to G4), each written with every name and number fixed like `prompt-12`. This is Decision D-G4.

---

## 4. What I could not check

- Everything is untested on a phone: the timings, the feel of the 26 ms impact against the sound, the thump amplitude, the lens strengths, the budgets.
- The mockup's refraction is an SVG filter in a Chromium browser. Backdrop's lens on Android is a different implementation; the rim strength and the colour fringe will need tuning by eye.
- The highlight and inner-shadow API of Backdrop 2.0.1 is not read yet.
- The emulator draws in software and cannot judge refraction.

## 5. Risks

- Refraction on several surfaces at once is the most likely cause of dropped frames on mid-range phones; the budgets in 2.8 and the Auto level are the mitigation.
- Moving the paper under a thump could blur text on some screens; the amplitude is below 1.1 dp and applies to the paper layer only.
- The type-on reveal touches the start animation, which has its own spec (`2026-11-07-enk-start-animation.md`); the reveal replaces only its text fade.

## 6. Decisions for the author

| # | Decision | Options | My recommendation |
|---|---|---|---|
| D-T1 | Default typing style | (a) Strike (b) Today | (a). It is the answer to "writes like a normal keyboard" |
| D-T2 | Weights (capitals harder, punctuation lighter) | (a) On (b) Off | (a) |
| D-T3 | The type-on for opening notes and the start animation | (a) Carriage sweep (b) Keep the fade | (a) |
| D-G1 | Light that follows the finger | (a) On at Full (b) Off | (a) |
| D-G2 | Colour fringe on small lenses | (a) On at Full (b) Off | (a); it is the most "liquid" detail and the cheapest to remove |
| D-G3 | Button grows into its menu | (a) Morph (b) Scale from the button | (a) |
| D-G4 | Build plan | (a) One prompt per track (T1, then G1 to G4) (b) One prompt for everything | (a). The strike and the glass kit are independent, so each is checked on its own |
