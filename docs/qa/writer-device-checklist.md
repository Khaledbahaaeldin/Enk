# Enk Writer Device QA Checklist

Manual verification checklist for the Enk Writing Screen on Android virtual devices (AVDs) and physical hardware.

---

## Device & Environment Information
- **Device Model / AVD Profile**:
- **Android OS Version / API Level**:
- **Build Type**: `debug` / `release`
- **Tester**:
- **Date**:

---

## 1. Typing & IME Input

- [ ] **1.1 Single Character Entry**
  - Type individual letters, numbers, and symbols.
  - Verify characters immediately appear on paper at the caret position.
  - Verify caret advances by one column.
  - Verify type slug briefly strikes in the carriage slot.

- [ ] **1.2 Whitespace & Return**
  - Tap spacebar: caret advances, space audio sample plays, slug remains blank.
  - Tap Enter: caret drops to the start of the next line, paper scrolls down if rail position is reached.

- [ ] **1.3 Swipe / Gesture Typing**
  - Use Gboard or system keyboard swipe typing to enter a whole word (e.g., "typewriter").
  - Verify the word commits cleanly as a single unit without duplicating characters or stamping letters repeatedly.

- [ ] **1.4 Emoji & Unicode / Surrogate Pairs**
  - Insert single and multi-code unit emoji (e.g. `🖋️`, `😀`, `👨‍👩‍👧‍👦`).
  - Verify no crashes occur, no partial surrogate characters appear, and backspace removes the entire grapheme cluster cleanly.

- [ ] **1.5 Hardware Keyboard Input**
  - Connect a physical Bluetooth or USB keyboard (or use emulator physical keyboard passthrough).
  - Type alphanumeric keys, Space, Backspace, and Enter.
  - Verify keystrokes route directly to the active document with identical behavior.

---

## 2. Correction Modes

- [ ] **2.1 Clean Erase Mode**
  - In Settings, ensure Clean Erase mode is selected.
  - Type text and tap Backspace.
  - Verify character before caret is deleted and caret moves backward one column.

- [ ] **2.2 Strike-Through Mode**
  - In Settings, switch to Strike-Through mode.
  - Type characters and tap Backspace.
  - Verify characters remain visible with a distinct 1.5 dp strikethrough line at 1/4 em height.
  - Verify caret position moves backward over the struck characters.

---

## 3. Audio Sonics

- [ ] **3.1 Keystroke Pitch & Timbre Variation**
  - Type a sentence rapidly.
  - Verify key sounds feel mechanical with subtle pitch/playback rate variation (+/- 3%) avoiding machine-gun repetition.

- [ ] **3.2 Specialized Mechanical Sounds**
  - Spacebar produces a deeper body thump.
  - Enter produces a weighty carriage return latch sound.
  - Backspace produces an audible typewriter backspace mechanism click.

- [ ] **3.3 Margin Bell**
  - Type continuously across a single line.
  - Verify typewriter bell chimes when caret reaches 4 characters from the right margin (`col >= cols - 4`).
  - Verify bell only chimes once per line.

- [ ] **3.4 Mute Setting**
  - In Settings, enable "Mute".
  - Type text, space, enter, and backspace.
  - Verify audio output is completely silent across all actions.

---

## 4. Haptic Feedback

- [ ] **4.1 Standard Keypulses**
  - On a device with haptics enabled, type characters.
  - Verify an 8 ms subtle, crisp vibration pulse is felt on keystrokes and backspaces.

- [ ] **4.2 Action Pulses**
  - Tap Enter: verify a slightly heavier 14 ms haptic thump is delivered.
  - Hold carriage to toggle night lamp: verify a firm 14 ms latch pulse occurs.
  - Drag control panel faders: verify a crisp 4 ms detent tick is felt at each notch.

- [ ] **4.3 Graceful Degradation**
  - On a device or emulator without vibration hardware or permission, verify typing never crashes or stutters.

---

## 5. Carriage & Marker Gestures

- [ ] **5.1 Carriage Position & Caret Alignment**
  - Verify carriage sits directly over the active caret column when typing.
  - Verify carriage glides smoothly to column positions.

- [ ] **5.2 Marker Scrubbing**
  - Place finger on the carriage marker and drag horizontally.
  - Verify carriage enters drag mode and caret scrubs across document text.
  - Lift finger: verify carriage snaps smoothly to the nearest column with natural spring overshoot.

- [ ] **5.3 Marker Long-Press (Night Lamp Toggle)**
  - Press and hold carriage marker without moving (hold for ~480-500 ms).
  - Verify night lamp toggles ON/OFF accompanied by mechanical click sound and 14 ms haptic feedback.
  - Verify slop rejection: dragging more than 6 dp before 480 ms cancels long-press and initiates scrub instead.

- [ ] **5.4 Paper Tapping**
  - Tap any visible line of text on the paper sheet.
  - Verify caret immediately relocates to the tapped character position.
  - Verify software keyboard opens / regains focus if dismissed.

---

## 6. Night Mode & Lamp Optics

- [ ] **6.1 Day-to-Night Crossfade**
  - Long-press carriage to activate night mode.
  - Verify chassis smoothly transitions from day cream/graphite to deep night chassis gradient.
  - Verify paper color darkens from `paperDay` to `paperNight`.
  - Verify transition duration respects motion settings (500 ms standard or 100 ms with `reduceEffects`).

- [ ] **6.2 Conic Beam & Halo**
  - Verify luminous upward-pointing trapezoidal beam shines onto the paper above the carriage.
  - Verify radial light halo illuminates the carriage slot and immediate typing area.
  - Verify beam follows the carriage horizontally when typing or scrubbing.

- [ ] **6.3 Text Masking**
  - In night mode, verify text directly under the beam is vividly illuminated in warm amber tones.
  - Verify text outside the beam falls into a darkened, readable shadow.

---

## 7. Night Control Panel & Faders

- [ ] **7.1 Panel Presentation**
  - Verify Control Panel smoothly animates in from the bottom with staggered row springs upon entering night mode.
  - Verify Control Panel disappears completely when toggling back to day mode.

- [ ] **7.2 Warmth Fader**
  - Slide Warmth fader (0 to 100).
  - Verify lamp color temperature shifts from neutral white/tungsten to deep golden amber.
  - Verify audible tick synth clicks and 4 ms haptic pulses at detents.

- [ ] **7.3 Intensity Fader**
  - Slide Intensity fader (0 to 100).
  - Verify beam luminosity, halo brightness, and text illumination scale proportionally.

- [ ] **7.4 Beam Width Fader**
  - Slide Beam Width fader (0 to 100).
  - Verify conic beam width dynamically narrows or widens.

---

## 8. Top Bar & Navigation

- [ ] **8.1 Top Bar Layout**
  - Verify 10 dp accent dot is present on top-left.
  - Verify `EnkWordmark` is rendered crisply with creamy color in night mode and dark ink in day mode.
  - Verify top-right shows document counter: `p.<page> · <char_count> / 3000`.
  - Verify counter updates in real time as characters are added or removed.

- [ ] **8.2 Wordmark Touch Target**
  - Tap wordmark area (verifying minimum 48x48 dp touch target).
  - Verify `onOpenBox` navigation callback is dispatched cleanly.

- [ ] **8.3 Hint Bar**
  - Verify bottom hint `"drag marker to scrub · hold marker for lamp"` is visible on a fresh note during day mode.
  - Verify hint hides in night mode or once dismissed.

---

## 9. Lifecycle, Debounce & Autosave

- [ ] **9.1 Debounced Persistence**
  - Type several sentences into the writer.
  - Wait 500 ms without backgrounding.
  - Check Room database (or logcat): verify page edits are saved.

- [ ] **9.2 Backgrounding Flush (ON_STOP)**
  - Type a sentence and immediately press Home button or switch apps within 100 ms (before debounce interval).
  - Verify autosaver immediately flushes all pending edits on `ON_STOP`.
  - Re-open app: verify every character typed is fully preserved without data loss.

- [ ] **9.3 Process Termination & Relaunch**
  - Kill app process via terminal or task switcher (`adb shell am force-stop app.enk.notes`).
  - Re-launch Enk.
  - Verify the most recent note and caret position load accurately.
