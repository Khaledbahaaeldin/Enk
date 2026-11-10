# QA Device Checklist: Accessibility, Reduced Motion, & Contrast (Task 26)

Execute these manual checks on a reference device (or emulator) before release.

## 1. TalkBack & Screen Reader Walkthrough
- [ ] **Carriage Marker**:
  - Focus carriage in the writer.
  - Verify TalkBack announces: `"Writing marker. Drag to scrub, long press to toggle lamp"`.
  - Verify state description announces `"Lamp on"` or `"Lamp off"`.
  - Verify touch target is at least 48 dp tall.
- [ ] **Paper Text**:
  - Focus writing paper.
  - Verify page text is read out as editable text to TalkBack.
- [ ] **Wordmark**:
  - Focus wordmark button in top bar.
  - Verify TalkBack announces `"Open the Box, Button"`.
- [ ] **Lamp Faders**:
  - Turn lamp on to open the control panel.
  - Focus each fader (Warmth, Intensity, Beam).
  - Verify TalkBack announces fader label, percentage range (`ProgressBarRangeInfo`), and supports standard swipe volume/slider actions.
- [ ] **Shell Navigation Bar**:
  - Focus bottom navigation bar.
  - Verify tab-row semantics: announces tab position, selected state, and destination names (Write, Box, Find, Setup).
  - Verify each item has a touch target height of at least 48 dp.
- [ ] **Note Cards & Context Actions**:
  - In Box screen, focus a note card.
  - Open TalkBack actions menu (swipe up then right or context menu).
  - Verify custom actions: `"Open note actions"`, `"Move to divider"`, `"Move to trash"` are present and executable.

## 2. Reduced Motion & Reduce Effects
- [ ] **System "Remove Animations" setting**:
  - Enable "Remove animations" in Android Accessibility settings (animator duration scale = 0).
  - Open Enk: verify start animation is a fast 250 ms fade or skipped.
  - Typing in writer: verify stamp and paper feed animations clamp to <= 100 ms.
  - Carriage scrub: verify carriage snaps to caret position without spring overshoot.
  - Glass surfaces: verify `chooseGlassBackend` falls back to `Solid` background without blur.
- [ ] **In-App "Reduce Effects" setting**:
  - In Setup > Display, toggle "Reduce effects" ON.
  - Verify paper grain is disabled.
  - Verify glass blur is disabled (solid opaque surfaces).
  - Verify transitions snap or use 100 ms quick fades.

## 3. Large Font Scale & Display Scaling
- [ ] **130% Font Scale**:
  - Set Android font scale to 130%.
  - Verify writer columns scale gracefully (down to 18-22 cols) without text overlapping the rail or margins.
  - Verify large titles in Shell screens wrap properly and do not clip.
- [ ] **200% Font Scale**:
  - Set Android font scale to 200%.
  - Verify no text clipping in setup rows, grouped headers, search results, or divider strip.
- [ ] **Landscape Layout with Keyboard**:
  - Rotate device to landscape.
  - Bring up soft keyboard (~300 px height).
  - Verify minimum paper height is respected.
  - Verify scrub hint is hidden when paper height is short.
  - Verify caret and active line stay visible above keyboard.

## 4. Contrast & Readability
- [ ] **Static Themes**:
  - Verify all 10 theme combinations (Graphite, Apricot, Sky, Slate, Mono in Light and Dark) maintain WCAG AA contrast (>= 4.5:1 for body text, >= 3.0:1 for large text and UI accents).
- [ ] **Dynamic Color**:
  - Test on Android 12+ wallpaper with saturated background colors.
  - Verify dynamic shell theme maintains >= 4.5:1 contrast against surfaces.
- [ ] **Writer Elements**:
  - Verify `textMuted` (#948F83) on `chassisBottom` (>= 4.5:1) and `panelBottom` (>= 4.5:1).
  - Verify `textOnChassis` on `panelBottom` (>= 4.5:1).
  - Verify `ink` on `paper` (>= 4.5:1).
