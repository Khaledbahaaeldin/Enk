# Enk start animation: "Rail becomes the writer"

Date: 2026-11-07. Status: approved by the author (storyboard 1 of 4; about 1.4 s on a cold start, skippable, silent). Reference: `docs/mockups/enk-startup.html` (all four storyboards with a scrubber; storyboard 1 is the one built). Depends on the logo (`2026-11-07-enk-logo.md`) and the writer layout (design system sections 6 and 7).

## What the user sees

1. **System splash (first moment).** The logo symbol, no tile, on a background that follows the system mode: **dark graphite (`#161514`) in dark mode and warm light grey (`#ECE8DF`) in light mode.** Three faint typed lines, the orange caret, the steel rail, and the carriage. The rail is clipped by the splash's circular mask, as Android does for every app. In light mode the typed lines are ink (`#2B2824` at 34%) instead of paper cream; everything else is identical.
2. **The aperture opens.** The circular mask grows to the full screen, so the rail stretches to full width. In light mode the dark writer chassis grows out of the logo as a circle with the same radius, so the light background is covered by the writer opening up from the logo; in dark mode there is no circle (the background is already dark and simply fades into the chassis).
3. **The rail becomes the writer's rail.** It drops to its real writing height (62% of the screen) and thins to 12 dp. The logo's typed lines fade out.
4. **The carriage and caret land.** The logo's carriage glides under the writer's caret and shrinks to its real size; the logo's orange caret bar morphs into the writer's real caret.
5. **The paper rises behind it** and the note's text fades in, then the top bar. The overlay hands over to the real writer.

It ends exactly on the writing screen as it will be used, at the last note's last caret position. There is no sound and no haptics (author decision).

## Timing

The budget on a typical cold start is about 1.4 s: up to about 500 ms of system splash while the app initialises, then a fixed **900 ms** in-app animation. If the app is ready sooner, the splash still ends and the 900 ms plays; if the app is slower, the splash simply lasts longer. The in-app animation never exceeds 900 ms.

Timeline in milliseconds **since the hand-off** (`t'`; the mockup's `t = t' + 500`). `u = 288 / 108 = 2.667 dp` is the logo unit on the splash's 288 dp canvas. "Reference" values are for a 400 x 820 dp screen; the targets are read live from the writer layout (`WriterGeometry`), not hard-coded.

| Element | From | To | Window (ms) | Easing |
|---|---|---|---|---|
| Aperture (mask circle radius) | 96 dp | the screen diagonal's half plus margin (760 dp on the reference) | 0 to 350 | ease-in-out cubic |
| Rail left and width | left 72, width 256 (x6 to x102 units) | left 0, width = screen width | 0 to 350 | ease-in-out cubic |
| Rail centre Y and height | centre 426.7 (60.25 u below the canvas top), height 22.7 (8.5 u) | centre = `railY` (62% of the height), height 12 | 250 to 650 | ease-in-out cubic |
| Typed lines opacity | 34% | 0 | 0 to 200 | linear |
| Splash background to writer chassis (dark splash only) | `#161514` | the writer chassis gradient | 60 to 450 | ease-in-out cubic |
| Chassis circle (light splash only) | radius 0, the writer chassis gradient, centred on the logo (screen centre) | the same radius as the aperture, which covers the screen | 0 to 350 | ease-in-out cubic (the same curve as the aperture, so the circle and the clip always match) |
| Paper translateY | the paper's height (760 on the reference) | 0 | 150 to 600 | ease-in-out cubic |
| Carriage centre and scale | centre (218.7, 426.7), scale 1 (3.33 dp per unit) | centre (carriage x for the caret column, `railY` + 1), scale 0.54 (1.8 dp per unit, the writer carriage size) | 350 to 700 | ease-in-out cubic |
| Caret bar | left 204, top 359.3, 29.3 x 5.6 | the writer caret rect (left = text left + column x width; top = line top + 1.42 x font size; 1 column x 2 dp) | 300 to 650 | ease-in-out cubic |
| Note text opacity | 0 | 1 | 450 to 700 | linear |
| Top bar opacity | 0 | 1 | 600 to 900 | linear |
| Hand-off swap | overlay rail, carriage, caret | the writer's own rail, carriage, caret | 650 to 750 (cross-fade) | linear |
| Overlay removed | | | 750 | |
| Caret starts blinking | | | 800 | |

Ease-in-out cubic is `cubic-bezier(0.65, 0, 0.35, 1)`. The whole animation is a pure function of `t'` (see the plan task), so it can be skipped, scrubbed, and tested.

## Android implementation

- **System splash:** `androidx.core:core-splashscreen`. `windowSplashScreenBackground` is `#ECE8DF` in `values/` (light mode) and `#161514` in `values-night/` (dark mode); `windowSplashScreenAnimatedIcon` is `ic_splash_symbol_light` in `values/` (ink typed lines) and `ic_splash_symbol` in `values-night/` (cream typed lines). Both are the symbol as a vector with a 300 ms appear (scale 0.9 to 1 and fade in) animation, on the 288 dp canvas with no icon background, so the icon sits in the 192 dp circle and the rail is clipped by it. The splash follows the **system** day or night setting (the only thing known at launch), not the app's Mode setting; the overlay is told which variant was shown (`lightSplash`) from the launch configuration so the animation matches. The minimum SDK is 31, so the system splash is always the Android 12 one and no legacy path is needed.
- **Hold:** the splash stays up (`setKeepOnScreenCondition`) until the last note and the settings have loaded, with a hard cap of 1500 ms, after which the writer starts normally with no animation.
- **Hand-off:** in `setOnExitAnimationListener`, remove the system splash view immediately and show the Compose `StartOverlay` in its place. The overlay draws the identical symbol from the same vector path data, at the same position and size, in its first frame, so the swap shows no jump (verified by a pixel comparison, below). Then the 900 ms timeline plays.
- **Overlay:** a full-screen layer above the writer, drawn on a single canvas using `graphicsLayer` transforms; the timeline progress is one animated value read in the draw phase, so nothing recomposes per frame. The writer underneath is composed normally and its own rail, carriage, and caret start hidden until the swap.
- **Skip:** any touch jumps to the end: the timeline value eases to 1 over 120 ms and the overlay is removed. Back press does the same.
- **Reduced motion** (system animations off, or Reduce effects on): no motion; the splash cross-fades to the writer over 250 ms.

## When it plays

- Plays: a cold start of the launcher activity (process was not alive), once per process.
- Never plays: returning to a running app; opening from the widget's New note or a note; a rotation or other configuration change; restoring after process death with saved state; opening from a share or other intent. These start on the writer directly.
- First launch (no notes): the paper is empty, the caret is at column 0, and the carriage lands at the start of the line.
- Lamp: the animation always runs with the lamp off; if the writer restores a lit lamp, it turns on after the overlay ends with the normal 600 ms fade.
- Theme: the splash color follows the system mode (light splash in light mode, dark splash in dark mode); the chassis, paper, and text it opens into are the writer's live day or night colors, whatever the app's Mode setting is. If the system is light but the app's Mode is Dark, the animation still plays the light splash variant, then lands on the dark writer paper state.

## Not in scope

The other three storyboards (type the name, logo materializes, minimal) were explored and not chosen; they stay in the mockup file as a record only. No sound and no haptics. A first-run-only longer variant is not built.
