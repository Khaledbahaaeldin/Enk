# Enk Shell Device QA Checklist

Manual verification checklist for the Enk App Shell (Navigation, Scaffold, Glass Nav Bar, Insets, and Accessibility) on Android virtual devices (AVDs) and physical hardware.

---

## Device & Environment Information
- **Device Model / AVD Profile**:
- **Android OS Version / API Level**:
- **Navigation Mode**: Gesture Navigation / 3-Button Navigation
- **Theme & Mode Tested**: Graphite / Apricot / Slate / Dynamic (Light & Dark)
- **Build Type**: `debug` / `release`
- **Tester**:
- **Date**:

---

## 1. Window Insets & Edge-to-Edge Layout

- [ ] **1.1 Status Bar Inset**
  - Verify edge-to-edge is enabled with translucent system bars.
  - On non-writer screens (Box, Find, Setup), verify that the Large Title top padding (70 dp) sits comfortably below the status bar cutout/icons.
  - Verify that the Compact Bar (when pinned at the top) sits below the status bar inset without colliding with status bar icons.
  - Check devices with camera cutouts/punch-holes (e.g., Pixel 7/8/9, Galaxy S series) to ensure titles and icons are not occluded.

- [ ] **1.2 System Navigation Bar Inset**
  - In **Gesture Navigation** mode: verify the floating Glass Nav Bar sits 22 dp above the navigation pill inset.
  - In **3-Button Navigation** mode: verify the Glass Nav Bar sits 22 dp above the 3-button navigation bar (no overlap or occlusion).
  - Verify the nav bar bottom clearance does not clip on any display density.

- [ ] **1.3 Full-Bleed Aura Background**
  - Verify the Shell background gradient/aura bleeds continuously behind both the status bar at the top and navigation bar at the bottom.
  - Verify no black or white letterboxing strips appear behind system bars.

- [ ] **1.4 Writer Screen Inset Isolation**
  - Start on Writer screen: verify typing paper and wordmark top bar sit comfortably inside `safeDrawing` insets.
  - Switch to Box and back to Writer: verify no sudden jump or double-inset occurs when transitioning between the Shell Scaffold and Writer Screen.

---

## 2. Screen Rotation & Configuration Changes

- [ ] **2.1 Route Preservation Across Rotation**
  - Navigate to Box, Find, or Setup.
  - Rotate device from Portrait to Landscape, then back to Portrait.
  - Verify the active screen destination and route remain unchanged.

- [ ] **2.2 Text and Caret Preservation Across Shell Navigation**
  - On Writer screen, type sample text and position the caret in the middle of a word.
  - Tap Box tab in the Glass Nav Bar: verify Box placeholder is displayed.
  - Rotate device while in Box screen.
  - Tap Write tab: verify returning to Writer screen restores the exact typed text and caret index without reset or loss.

- [ ] **2.3 Landscape Responsive Layout**
  - In Landscape orientation, verify the Glass Nav Bar width fits within screen constraints without edge clipping.
  - Verify Large Title and list items accommodate landscape widths cleanly.

---

## 3. Accessibility & TalkBack Pass

- [ ] **3.1 Selectable Group & Tab Semantics**
  - Enable TalkBack.
  - Navigate through the Glass Nav Bar: verify TalkBack announces the bar as a tab group ("Tab 1 of 4: Write", "Tab 2 of 4: Box", etc.).
  - Verify `Role.Tab` is announced for each item.

- [ ] **3.2 Tab Selection Announcements**
  - Double-tap a different tab (e.g., Box).
  - Verify TalkBack immediately announces the new tab as "Selected".
  - Verify double-tapping the currently selected tab does not trigger redundant navigation announcements.

- [ ] **3.3 Large Title & Meta Content Description**
  - Focus TalkBack on the Large Title header in Box or Setup.
  - Verify TalkBack reads both the title and the meta subtitle together (e.g., "The Box, 12 notes" or "Setup, Enk 1.0"), without stripping the meta text.

- [ ] **3.4 Touch Targets (48x48 dp Minimum)**
  - Verify each tab in the Glass Nav Bar provides at least a 48x48 dp touch target area.
  - Ensure easy activation with one thumb without mis-tapping adjacent tabs.

---

## 4. Glass Nav Bar & Lens Drag Physics

- [ ] **4.1 Quick Tap Selection**
  - Tap any destination tab: verify the lens smoothly glides to the selected item using Apple-style spring (`shellDefault`: response 0.35s, damping 1.0).
  - Verify no overshoot occurs on standard tap.

- [ ] **4.2 1:1 Lens Drag Tracking**
  - Press down on the lens and drag horizontally:
  - Verify after passing 8 dp drag slop, the lens follows the finger 1:1.
  - Verify pointer events are consumed so the underlying screen does not scroll horizontally.

- [ ] **4.3 Live Lens Highlight Brightening**
  - While dragging the lens across items, verify that the destination directly under the center of the lens immediately brightens (its icon and label turn `t1`).
  - When dragging past to another destination, verify the previous item dims back to `t2`.

- [ ] **4.4 Rubber Band Resistance**
  - Drag the lens past the leftmost item (Write in LTR) or rightmost item (Setup in LTR).
  - Verify rubber-band resistance slows lens travel (`Physics.rubber`, c = 0.55).
  - Release beyond the edge: verify lens springs back cleanly to the terminal tab.

- [ ] **4.5 Velocity Stretch & Momentum Flick**
  - Flick the lens vigorously across the bar:
  - Verify lens visibly stretches horizontally (`k = min(0.14, |v| / 6000)`).
  - On release faster than 350 dp/s: verify `shellFlick` spring (damping 0.8) projects momentum and settles on the projected target with subtle overshoot.

- [ ] **4.6 Drag Cancellation & Boundary Release**
  - Drag the lens horizontally, then move finger vertically 30 dp above or below the nav bar:
  - Verify the drag cancels and the lens smoothly returns to the original selected destination.

- [ ] **4.7 Reduced Effects Mode**
  - In Setup > Display, toggle "Reduce effects" ON.
  - Tap and drag between tabs:
  - Verify lens transitions snap instantly with no spring motion or horizontal stretch.

---

## 5. Scroll Collapse & Compact Bar

- [ ] **5.1 Scroll Collapse Curve (DS 12.7)**
  - On a scrollable screen (e.g., Box with cards), slowly scroll downward:
  - `scrollY = 0..30 dp`: Compact Bar title is completely transparent (alpha = 0f), Large Title is fully visible.
  - `scrollY = 30..66 dp`: Compact Bar title fades in smoothly from alpha 0.0 to 1.0. Large Title translates upward with content.
  - `scrollY >= 66 dp`: Compact Bar title is fully opaque (alpha = 1.0), Large Title is scrolled out of view.

- [ ] **5.2 Real-Time Glass Edge Blur**
  - As note cards scroll beneath the Compact Bar, verify card content is blurred in real-time under the glass bar surface.
  - In Reduced Effects mode, verify Compact Bar renders solid backdrop without blur.

- [ ] **5.3 Bottom Clearance (120 dp Padding)**
  - Scroll to the bottom of the note card list.
  - Verify the final card in the list clears the floating Glass Nav Bar completely with 120 dp end padding.
