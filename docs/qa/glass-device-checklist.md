# Glass & Motion Device & Emulator Checklist (Batch 15: Liquid Glass)

## 1. Feature & Verification Checklist

| Check | Level | Phone | Result | Notes |
|---|---|---|---|---|
| Melt 1: Writer capsule splits into toolbar and round Lamp button | Full, Balanced | Physical / Emulator API 34+ | PENDING-DEVICE | TopIsland Rest -> Actions split animation |
| Melt 2: Lamp panel grows out of round Lamp button | Full, Balanced | Physical / Emulator API 34+ | PENDING-DEVICE | Actions -> Lamp panel growth via LiquidGroup |
| Melt 3: Nav lens melts into round Write button within merge distance (24 dp) | Full, Balanced | Physical / Emulator API 34+ | PENDING-DEVICE | GlassNavBar lens dragged past right edge |
| Melt 4: Write button stretches into rail on Write transition | Full, Balanced | Physical / Emulator API 34+ | PENDING-DEVICE | RailMorphOverlay single drop pulled flat into 16 dp rail |
| Melt 5: Context menu & Delete alert bud from opener and pull free | Full, Balanced | Physical / Emulator API 34+ | PENDING-DEVICE | GlassMorph budding via LiquidGroup |
| Melt 6: Toggle bead / slider thumb pulls neck from track when pulled past end | Full, Balanced | Physical / Emulator API 34+ | PENDING-DEVICE | Overscroll / yank pull neck SDF |
| Melt 7: Segmented control lens melts between segments | Full, Balanced | Physical / Emulator API 34+ | PENDING-DEVICE | ShellSegmented lens movement |
| Handoff match: single capsule drawn by Backdrop vs field shader overlaid | Full | Physical / Emulator API 34+ | PENDING-DEVICE | Zero visible deviation at handoff boundary |
| One press feel: LiquidPress & DampedDrag everywhere | All | Physical Device | PENDING-DEVICE | Consistent press response and damped overscroll |
| Liquid vs Frost at Full and Balanced | Full, Balanced | Physical / Emulator API 34+ | PENDING-DEVICE | Liquid refraction vs Frost blur |
| Advanced sliders in Setup (Refraction, Lens depth, Fringe, Frost, Tint, Edge light) | Full, Balanced | Physical / Emulator API 34+ | PENDING-DEVICE | Sliders tune Liquid and Frost in real time |
| Android 12 shows Frost only | All | Emulator API 32 | PENDING-DEVICE | Liquid disabled below API 33 with footer note |
| Refraction at round Write button | Full | Physical / Emulator API 34+ | PENDING-DEVICE | 62 dp circle button with accent glyph |
| Refraction at nav lens | Full | Physical / Emulator API 34+ | PENDING-DEVICE | Lens bends content beneath with chromatic fringe |
| Refraction at segmented lens | Full | Physical / Emulator API 34+ | PENDING-DEVICE | Segment indicator refracts track and content |
| Refraction at toggle bead | Full | Physical / Emulator API 34+ | PENDING-DEVICE | Toggle bead refracts orange track |
| Refraction at slider thumb | Full | Physical / Emulator API 34+ | PENDING-DEVICE | Lens thumb refracts slider track and background |
| Refraction at top-bar buttons | Full | Physical / Emulator API 34+ | PENDING-DEVICE | TopIsland rest & action buttons lens refraction |
| Refraction at lamp pop-up | Full | Physical / Emulator API 34+ | PENDING-DEVICE | 280x230 dp floating lamp panel edge refraction |
| Refraction at context menu | Full | Physical / Emulator API 34+ | PENDING-DEVICE | Card context menu glass surface |
| Refraction at delete forever alert | Full | Physical / Emulator API 34+ | PENDING-DEVICE | Delete forever alert glass surface |
| Rim light following the finger | Full | Physical / Emulator API 34+ | PENDING-DEVICE | Highlight angle tracks finger touch coordinates |
| Colour fringe on small lenses | Full | Physical / Emulator API 34+ | PENDING-DEVICE | Dispersion aberration on nav lens, bead, thumb |
| 8 ms haptics on tab change | All | Physical Device | PENDING-DEVICE | Triggered when switching between Box, Find, Setup |
| 14 ms haptics on sheet dismiss / delete | All | Physical Device | PENDING-DEVICE | Triggered on sheet dismissal and note deletion |
| Auto demoting under load | Auto | Physical Device | PENDING-DEVICE | Frame governor drops from Full to Balanced to Light |
| Balanced level appearance | Balanced | Physical / Emulator | PENDING-DEVICE | Blur and tint without refraction; nav lens has refraction |
| Light level appearance | Light | Physical / Emulator | PENDING-DEVICE | Solid surfaces with hairline borders; instant transitions |
| Dark mode and four themes | All | Physical / Emulator | PENDING-DEVICE | Graphite, Apricot, Sky, Slate in dark & light |
| Font scale 130% | All | Physical / Emulator | PENDING-DEVICE | Text layout clean, min 48 dp targets maintained |
| Font scale 200% | All | Physical / Emulator | PENDING-DEVICE | Accessibility scaling does not clip text or buttons |
| Landscape orientation | All | Physical / Emulator | PENDING-DEVICE | TopIsland and lamp popup layout in landscape |
| TalkBack on lamp pop-up and sliders | All | Physical Device | PENDING-DEVICE | Accessible slider ranges and detents announced |
| Rail transition, Box to Write: Write button stretches, bar sinks 24 dp & fades | Full, Balanced | Physical | PENDING-DEVICE | No full-screen blur in recede |
| Rail transition reversed (island Box button) | Full, Balanced | Physical | PENDING-DEVICE | Writer parts retract, Box un-dims, bar returns; no ding |
| Ding: bell, 14 ms pulse, ring around the carriage | All | Physical | PENDING-DEVICE | Bell follows the sound switch, the pulse follows the haptics switch |
| Island tap-outside, typing and Back fold it | All | Physical / Emulator | PENDING-DEVICE | Covered by Compose tests; confirm by hand with the real keyboard |

---

## 2. Speed Performance Table (Spec Section 8)

*Measured on Samsung Galaxy Note 20 Ultra using `adb shell dumpsys gfxinfo app.enk.notes framestats` and Perfetto traces. Target: 60 Hz 0 janky frames at Full; 120 Hz p95 < 8.3 ms (Write transition < 11 ms).*

| Step | Mode | Refresh Rate | p50 (ms) | p90 (ms) | p95 (ms) | Janky Frames | Result |
|---|---|---|---|---|---|---|---|
| 1. Open the app | Liquid Full | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 1. Open the app | Liquid Full | 120 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 1. Open the app | Liquid Balanced | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 1. Open the app | Frost Full | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 2. Drag nav lens across and back | Liquid Full | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 2. Drag nav lens across and back | Liquid Full | 120 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 2. Drag nav lens across and back | Liquid Balanced | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 2. Drag nav lens across and back | Frost Full | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 3. Open and close island | Liquid Full | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 3. Open and close island | Liquid Full | 120 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 3. Open and close island | Liquid Balanced | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 3. Open and close island | Frost Full | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 4. Open lamp panel and drag slider | Liquid Full | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 4. Open lamp panel and drag slider | Liquid Full | 120 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 4. Open lamp panel and drag slider | Liquid Balanced | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 4. Open lamp panel and drag slider | Frost Full | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 5. Run Write transition and back | Liquid Full | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 5. Run Write transition and back | Liquid Full | 120 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 5. Run Write transition and back | Liquid Balanced | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 5. Run Write transition and back | Frost Full | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 6. Open context menu and alert | Liquid Full | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 6. Open context menu and alert | Liquid Full | 120 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 6. Open context menu and alert | Liquid Balanced | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 6. Open context menu and alert | Frost Full | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 7. Change tabs (Box, Find, Setup) | Liquid Full | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 7. Change tabs (Box, Find, Setup) | Liquid Full | 120 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 7. Change tabs (Box, Find, Setup) | Liquid Balanced | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 7. Change tabs (Box, Find, Setup) | Frost Full | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |

## Motion polish (batch 16): the author's eye on the Note 20 Ultra

Look at each at normal speed, then once more with Developer options > Animator duration scale 5x. Mark each OK or write what is off.

| Piece | What to look for | Result |
|---|---|---|
| Lamp panel opens | grows out of the round Lamp button, right edge never moves, content stays still and is revealed, rows fade in one after another, a hair of settle at the bottom | PENDING-DEVICE |
| Lamp panel closes | rows fade first, the glass drains into the button, no panel left floating where the button used to be | PENDING-DEVICE |
| Lamp panel interrupted | tap Lamp again mid-growth, or tap outside: it reverses from where it is, no jump | PENDING-DEVICE |
| Island open and fold | the capsule stretches into the toolbar, the Lamp button pinches off, no blank beat between the two contents | PENDING-DEVICE |
| Nav: tap a tab | the lens slides to the tab (it does not dissolve), the bar stays solid and does not dim, the icon under the lens is accent coloured | PENDING-DEVICE |
| Nav: drag the lens | bar swells and leans toward the finger, glyph and label under the lens grow and are coloured, the lens stretches with speed, a light tick at each tab boundary | PENDING-DEVICE |
| Nav: release | lens settles on the nearest tab (with a flick, the one it was thrown to) | PENDING-DEVICE |
| Tab content | the new tab fades in over the old one; the bar does not slide | PENDING-DEVICE |
| Setup: Advanced | opens by growing, rows below glide down, chevron turns smoothly; Liquid and Frost slider sets cross-fade | PENDING-DEVICE |
| Setup: theme tiles | ring fades in and out between tiles, tile settles with a spring | PENDING-DEVICE |
| Setup: entrance | groups rise in once after the app starts, not on every visit | PENDING-DEVICE |
| Context menu and alert | content keeps its shape while the glass grows from the opener (no stretching text) | PENDING-DEVICE |
| Light and Reduce motion | everything is a quick 100 ms fade, nothing swells or slides | PENDING-DEVICE |
