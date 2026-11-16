# Enk motion polish pass (batch 16): lamp panel, nav lens, Setup, one motion vocabulary

Date: 2026-11-16. Status: DRAFT for the author's approval. Claude builds this one itself (no Antigravity batch). Follows batch 15 (`2026-11-15-enk-liquid-glass-design.md`); the speed rounds on the Note 20 Ultra come after this, because polishing motion changes what is worth measuring.

## 1. Why

The author's verdict after testing batch 15 on the Note 20 Ultra: the lamp panel's pop-out is "not refined, skewed, apparent to a normal user"; moving between the three tabs "looks like a normal transition"; Setup's choices and animations "aren't refined"; the app overall is "40 percent UX-wise, late alpha, early beta".

The honest reading of the last point: the app has plenty of animation but no single motion language. Springs have been picked per screen, some things still snap (Setup), and the best-looking piece (the glass) is not yet carried by the surrounding motion. This pass is about consistency and finish, not more effects.

## 2. Research

- **Catalog's `LiquidBottomTabs`** (the reference for iOS 26 tab bars, Apache-2.0, same project as Backdrop; [source](https://github.com/Kyant0/AndroidLiquidGlass/blob/kmp/app/src/commonMain/kotlin/com/kyant/backdrop/catalog/components/LiquidBottomTabs.kt)). Differences from Enk's `GlassNavBar`, all of which are things the eye reads as "native":
  1. The lens position is a continuous value in *tab units* on a `(1, 1000)` spring, driven 1:1 by the finger and re-targeted on release. Enk springs a pixel offset with `ShellSpring(0.35, 1.0)` and snaps while dragging.
  2. **The whole bar reacts**: while pressed it swells by 16 dp of width and rubber-bands up to 4 dp toward the finger (`EaseOut`). Enk grows only the lens.
  3. **A second, tinted copy of the tab row is drawn inside the lens**: the glyph under the lens is accent-coloured and scaled 1.2x while pressed. Enk flips the ink colour instantly and never scales it.
  4. Velocity stretch uses the damped velocity spring `(0.5, 300)` with the width/height ratio 0.75/0.25.
- **Catalog's dialog**: the panel is one glass surface whose *content stays laid out at full size*; the glass is what changes. No content is re-measured during a reveal.
- **Apple's guidance** (WWDC "Designing Fluid Interfaces", via the apple-design skill): animate from the presented value so everything is interruptible; hand the finger's velocity to the spring; critically damped (1.0) by default, bounce (about 0.8) only when the gesture carried momentum; enter and exit along the same path from the source; materialise (blur and scale together) rather than cross-fade.
- **iOS 26 recreations**: community SwiftUI and Flutter recreations of Liquid Glass tab bars and expanding glass controls mostly use a bounce of about 0.2 and 0.3 to 0.5 s, and describe the selection as a lens that lifts, follows the finger and separates the bar, the resting selection and the moving lens into layers. Apple publishes no exact constants (a web search found none), so ours are tuned on the device.

## 3. Findings in Enk's code

1. **Lamp panel skew, root cause.** `TopIsland` places the panel's content box at the growing glass rectangle (`placeAt { panelRect() }`) and `LampPopupContent` is `requiredSize(280 x 230)`. A required size larger than its parent is *centred* on the parent, so during the growth the content slides across the panel (visible in the batch 15 recordings: the "Lamp" title sits left of the blob's centre for several frames). The glass also lerps its centre and its size separately, and uses the 0.82-damped sheet spring, so it overshoots in both axes and drifts. It reads as a wobbly, off-axis balloon.
2. **Nav**: no bar swell, no tinted copy, no glyph scale, ink flips at the half-way test (`isHighlighted`), pixel spring instead of a value in tab units. Page change is a full-width horizontal slide plus fade on the 0.40 s page spring, which makes a tab change feel like pushing a screen rather than switching a view.
3. **Setup**: nothing animates. "Advanced" is a bare `if (advancedExpanded)`, so its rows pop in and everything below jumps; the chevron rotates with no animation; swapping the Liquid and Frost slider sets is instant; the disabled Autocorrect row switches state without easing; the theme ring jumps between tiles; the screen has no entrance.
4. **Vocabulary**: 32 call sites use `ShellMotion` tokens, plus ad-hoc `tween`s; damping below 0.85 is used in places that carry no gesture momentum (sheet 0.82, release 0.8).

## 4. Goals, non-goals, anti-goals

Goals: every glass motion is interruptible, anchored to its source, and reads as one material; the three tabs feel like a physical lens on a bar; Setup eases everything that changes; one small motion vocabulary the whole app uses.

Non-goals: new features, new visuals, a new theme, speed work (comes after), changes to the writer's Rail transition.

Anti-goals: more bounce; motion that delays the user (nothing blocks input; no animation over about 400 ms except the Rail); anything that breaks Light and Reduce-motion (those paths stay 100 ms and static).

## 5. The work

### P1. Lamp panel (and the same fix for the other panels)
- Content never re-measured or re-centred: it is laid out once at 280 x 230, anchored top-end, and *revealed* by clipping to the live glass shape (same rounded rectangle as the glass). The glass is what grows.
- Growth is defined by edges, not centre and size: the **right edge stays on the Lamp button's right edge** the whole time (it is the anchor), the left edge, top edge and bottom edge each have their own progress, with width leading height by about 40 ms (menus open sideways first). No separate centre drift.
- Size springs are critically damped (response 0.36); the only softness is a 4 percent height settle (damping 0.88 on the bottom edge). Corner radius runs from the button's round radius to 28 dp on the same progress, so the melt hands back to the real panel with the same shape.
- Content children fade in last (progress above 0.55), each row 20 ms apart, rising 6 dp. On close, content fades first (below 0.5), then the glass drains into the button.
- Interrupting (tap Lamp, tap outside, type) retargets from the current value in both directions; nothing waits for the animation to finish.
- Same treatment checked for `GlassMorph` (context menu, Delete forever alert): content fixed-size and revealed, anchored to the opener.

### P2. Nav lens (port the Catalog behaviour, keep Enk's sizes)
- Lens position becomes `lensValue` in tab units on `DampedDrag` (the batch 15 port, springs from the Catalog). Drag writes it 1:1, release projects with velocity (already in `LensController`) and animates to the nearest tab.
- Bar reaction while pressed: the bar swells (+8 dp each side) and rubber-bands up to 4 dp toward the finger; the lens swell stays.
- **Tinted copy under the lens**: the tab row is drawn a second time in the accent colour, clipped to the lens shape (through the shared bar backdrop already in place), glyphs scaled 1.2x while pressed. The base row stays t2; the ink under the lens is accent, so the glyph visibly changes colour as the lens passes over it, not at a threshold.
- A light haptic tick each time the lens crosses a tab boundary during a drag (existing haptics setting).
- Tab changes drop the full-width slide: the incoming tab fades in with a 24 dp parallax in the direction of travel on a quicker spring (response 0.30), starting when the lens is about 40 percent across, so the lens leads and the content follows. Writer and pushed-screen transitions are untouched.

### P3. Setup
- "Advanced": expand with a spring on height (response 0.36), rows fade in 20 ms apart, the section below glides instead of jumping (`animateContentSize` on the grouped section), the chevron rotates with a spring.
- Liquid and Frost slider sets crossfade (keyed by look) inside the same height animation.
- Disabled and enabled states (Autocorrect and friends) ease in 160 ms; the theme-tile selection ring is one shared ring that moves between tiles on the move spring; tiles swell 4 dp on press like the other glass.
- Entrance: the six groups rise 8 dp and fade in 25 ms apart the first time Setup is opened in a session only (not on every tab revisit).
- Segmented and toggle controls keep their batch 15 behaviour; checked again on device.

### P4. One motion vocabulary
- Replace the ad-hoc springs with five named roles in `ShellMotion`: **press** (0.20, 1.0), **move** (0.35, 1.0), **settle** (0.30, 1.0), **sheet** (0.38, 0.86), **page** (0.30, 1.0); bounce below 0.85 is allowed only for gestures that carry momentum (`flick`, the Rail). Each of the 32 call sites is assigned a role; outliers listed in the handoff.
- Audit pass: every animation retargets from the presented value (spring or `Animatable`; each remaining `tween` listed with the reason); enter and exit use the same path (context menu, alerts, toasts, sheets, search); reduced-motion and Light paths intact.

## 6. Verification

- JVM tests: panel edge geometry (right edge constant, content bounds constant, radius endpoints), tab-unit lens mapping and release projection, tinted-copy clip rectangle, vocabulary table (every token has a role; none below the damping floor except the named exceptions).
- Roborazzi: lamp panel at progress 0.25 / 0.5 / 0.75 (content never outside the glass); nav lens pressed at tab 1 with the tinted copy; Setup with Advanced half open.
- Emulator slow-motion (animator scale 6) recordings and contact sheets for each piece, as in batch 14.
- Note 20 Ultra: the author tries each piece; Claude records and reads frame times afterwards. Acceptance is the author's eye first, numbers second.

## 7. What building it changed in the plan

- **Root cause of the nav complaint (found while recording):** each tab screen draws its own nav bar, so a tab change cross-faded two bars and the lens never travelled; it just dissolved from one tab to the other (and the bar briefly dimmed). The lens position is now shared between screens (`NavLensState`), and tab changes use a dissolve-over (old screen held opaque underneath, new one fades in on top) so the identical bar never dips. The 24 dp parallax and the "content drifts in" part are dropped: any slide would drag the bar with it. The spec's earlier claim that tabs slid a full width was wrong (it was 12 dp).
- The lamp panel keeps its right edge on the Lamp button's *current* right edge (the button slides left while the island folds), and its glass is drawn below the button so the glyph is never covered.
- The 4 percent height settle is about 2 percent (damping 0.78): 0.88 would give 0.3 percent, which is invisible.
- Setup: the shared sliding ring between theme tiles became an eased per-tile ring (a shared ring needs the tiles' bounds); Advanced rows fade together, not 20 ms apart.

## 8. Decisions and my calls

- Claude builds it, one commit per P1 to P4, on a new branch `batch-16/motion` from `batch-15/liquid` after batch 15 is merged.
- My call: tab changes have no sideways slide at all (see 7). If the content feels too plain on the device, hoisting the nav bar out of the screens (so content alone can drift) is the next step.
- My call: the tick haptic is on by default (follows the haptics setting).
- Open for the author: none blocking. Exact constants (the 40 ms stagger, 24 dp parallax, 0.88 settle) are starting values to tune on the device.
