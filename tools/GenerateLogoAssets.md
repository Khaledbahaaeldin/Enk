# Generation of Enk Logo and Brand Assets

This document describes how the brand assets, adaptive launcher icons, splash drawables, wordmark, and store icons were produced for Enk 1.0 per `docs/superpowers/specs/2026-11-07-enk-logo.md` and `docs/mockups/enk-logo-final.html`.

## 1. Source Specifications
- **Geometry Grid:** 108 x 108 adaptive launcher grid.
  - 72 dp visible circle.
  - 66 dp safe zone.
  - Rail extends from x=6 to x=102 at y=56 (height 8.5 dp), intentionally crossing the mask.
- **Colors:**
  - Steel gradient: `#F4F4F2` (0%), `#A9A9A6` (40%), `#6F6F6C` (62%), `#C9C9C6` (100%).
  - Caret and Carriage Strip accent: `#FF5A1F` (orange).
  - Background tile gradient: `#3A3835` (top) to `#1D1C1B` (bottom).
  - Typed lines: `#F3EEE3` at 34% opacity (dark splash/foreground) and `#2B2824` at 34% (light splash).
- **Typography:**
  - "enk" wordmark is set in **Inter Bold** (SIL Open Font License 1.1) with tracking -4.5%.
  - Caret bar: 0.34 em wide, 0.085 em tall, 0.06 em offset after the 'k', sitting just below baseline like an underscore.

## 2. Asset Pipeline (`tools/build_assets.py`)
A reproducible Python script (`tools/build_assets.py`) generates all assets:
1. **Font Outlining:**
   - Instantiates `Inter-Bold` at 700 weight from `tools/Inter.ttf`.
   - Extracts glyph vectors for `e`, `n`, `k` using `fontTools.pens.svgPathPen.SVGPathPen`.
   - Applies tracking of -4.5% (-92 font units per 2048 UPM).
   - Generates exact vector outlines for `design/src/main/res/drawable/enk_wordmark.xml` and lockup SVGs.
2. **Android Vector Drawables:**
   - `ic_launcher_background.xml`: Tile vertical gradient.
   - `ic_launcher_foreground.xml`: 108 dp viewport with typed lines, caret `#FF5A1F`, rail shadow, steel rail, and carriage assembly at center (61, 60.25) scale 1.25.
   - `ic_launcher_monochrome.xml`: Themed icon layer for Android 13+ with single `#FFFFFFFF` tint, lines at 55% alpha, and carriage strip cut out.
   - `ic_splash_symbol.xml` and `ic_splash_symbol_light.xml`: 288 dp canvas with 108 viewport (so 72 unit symbol corresponds exactly to the 192 dp system splash circle).
3. **Store Icon & Web Assets:**
   - `docs/brand/store-icon-512.png` and `fastlane/metadata/android/en-US/images/icon.png`: 512x512 square PNG with full-bleed tile gradient, top highlight, and symbol scaled to canvas.
   - `docs/brand/logo.svg`, `docs/brand/lockup-dark.svg`, `docs/brand/lockup-light.svg`: Scalable vector representations for documentation, README, and web.

## 3. Licenses & Credits
- Inter font software is licensed under SIL Open Font License 1.1 (`app/src/main/assets/licenses/OFL-Inter.txt`).
- Credited in `CREDITS.md` and `docs/dependencies.md`.
