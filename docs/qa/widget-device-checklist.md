# QA Device Checklist: Home-Screen Widget (Task 24)

Execute these manual checks on a reference device (or emulator) before release.

## 1. Widget Placement & Layouts
- [ ] **Small Widget (170 x 170 dp, 2x2 cell)**:
  - Add the widget to the home screen.
  - Verify solid graphite background approximating glass (`#1C1A18`).
  - Verify miniature rail and carriage rendered with orange caret mark (`#FF5A1F`).
  - Verify excerpt of the most recent active note is readable in monospace font.
  - Verify "+ New" pill button is clearly visible.
- [ ] **Wide Widget (360 x 160 dp, 4x2 cell)**:
  - Resize or add a wide widget.
  - Verify paper card background (`#F2ECDF`) with 26 dp rounded corners.
  - Verify meta line format: `Recent · {d MMM} · p.{n}` (e.g. `Recent · 7 Nov · p.2`).
  - Verify first two non-empty lines of note text are displayed.
  - Verify "+ New note" paper pill button is displayed.
- [ ] **Empty State**:
  - Clear all notes or on fresh install.
  - Verify wide card displays "Nothing yet" with subtext "Tap to write your first note.".

## 2. Interaction & Actions
- [ ] **"+ New note" button tap**:
  - Tap the "+ New note" button on either widget.
  - App opens directly into the writer with a fresh unfiled note.
  - Verify start animation is skipped (`StartGate.shouldPlay` = false).
  - Verify no duplicate note is created if tapped while app is launching.
- [ ] **Note text tap**:
  - Tap the text of the note on the widget.
  - App opens directly to that note in the writer.
  - Verify start animation is skipped.

## 3. Updates & Coalescing
- [ ] **Edit & Autosave update**:
  - Edit text in the writer, then press Home.
  - Verify widget updates with the new text within ~5 seconds (`UpdateGate` 5000 ms rate limit).
- [ ] **Move to Trash**:
  - In the Box screen, move the active note to Trash.
  - Return to home screen: verify widget no longer shows the trashed note (shows the next most recent note or "Nothing yet").
- [ ] **Restore from Trash**:
  - Restore the note from Trash.
  - Return to home screen: verify widget updates to show the restored note.

## 4. System Conditions & Accessibility
- [ ] **Device Rotation**:
  - Rotate the home screen (or launcher). Verify widget retains layout without clipping or errors.
- [ ] **Wallpapers**:
  - Test on both pure dark wallpaper and bright white wallpaper.
  - Verify contrast and readability of both wide paper widget and small graphite widget.
- [ ] **TalkBack Walkthrough**:
  - Turn on TalkBack.
  - Focus the widget: verify it reads the meta line followed by the excerpt.
  - Focus "+ New note" button: verify announced as "New note, button".
- [ ] **Touch Target Size**:
  - Verify every interactive target (+ New note button and card body) has at least 48 dp touch height.
