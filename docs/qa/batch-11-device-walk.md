# Batch 11 QA: Toolchain Upgrade Device Walk

**Target:** Zero visual change, zero behavioral change under upgraded toolchain (AGP 9.5.1, compileSdk 37, Gradle 9.9.1, Compose UI 1.12.1 / Compose BOM 2026.09.00, Backdrop 2.0.1, Robolectric 4.18, Roborazzi 1.79.0).

---

## Device & Environment
- **Platform:** Android 15 (API 35) & Android 16/17 (API 36/37 preview)
- **Display Configurations:** Standard phone (e.g. Pixel 8 / 9, 400–412 dp width) and compact width (360 dp)
- **Build Under Test:** Release APK (`app-release-unsigned.apk` signed with debug key for sideloading)

---

## 1. Launch & Introduction
- [ ] **Cold Launch:** App launches cleanly through `SplashHost` into the writer start animation.
- [ ] **Start Animation:** No stutter, dropped frames, or flickering during the paper insert sequence.
- [ ] **First-Focus Invariant:** Hidden text field acquires focus only after the intro animation finishes.

## 2. Typewriter Typing & Stamping
- [ ] **Carriage Motion:** Carriage tracks text column accurately across the 400 dp rail.
- [ ] **Mechanical Stamp:** Stamping animation peaks at 50 ms and settles at 80 ms.
- [ ] **Strike-Through Mode:** Struck characters render with strike line without jumping columns.
- [ ] **Caret Color:** Brand orange caret (`#FF5A1F`) stays visible at current column.
- [ ] **Word Wrapping & Margin Bell:** Carriage bell sounds when approaching right margin; wraps cleanly without clipping.

## 3. Acoustics & Sound Engine
- [ ] **Sound Pools:** Keystrokes, carriage return, spacebar, and margin bell play without audible clipping or latency spikes.
- [ ] **Sound Sets:** Switching sound sets (Classic, Antique, Soft, Keyboard) in Setup updates playback immediately.
- [ ] **Volume & Mute:** Respects system volume and in-app sound toggle.

## 4. Shell & Navigation (Glass UI)
- [ ] **Navigation Bar:** Glass nav bar renders with background refraction on API 33+ and blur on API 31–32.
- [ ] **Theme Switching:** Switching themes (Apricot, Slate, Graphite, Sky, Mono, Dynamic) transitions smoothly across all screens.
- [ ] **Box Screen:** Dividers, note cards, and page counts display identically to Batch 10.
- [ ] **Search / Find Screen:** Instant search with FTS4 highlights queries; empty and no-results states render cleanly.
- [ ] **Setup Screen:** Appearance, acoustics, mechanics, and about options remain fully functional.
- [ ] **Credits Screen:** Displays updated library version catalog, license viewer, and provenance links.

## 5. Night Lamp & Control Panel
- [ ] **Lamp Glow:** Night lamp beam casts smooth radial gradient across carriage and paper.
- [ ] **Control Panel Drawer:** Drags smoothly from top edge; faders maintain 48 dp touch targets.
- [ ] **Reduced Effects:** Turning on "Reduce effects" drops blur/refraction shaders to solid backdrop and disables sliding motion.

## 6. Widget & Background Persistence
- [ ] **Home-Screen Widget:** Glance widget renders note preview, page count, and launch touch action.
- [ ] **DataStore & Room:** Notes auto-save via `Autosaver` within 300 ms; surviving process kill and restart.
- [ ] **Export & Import:** Export note archive to JSON/text and restore from file works without network permissions.
