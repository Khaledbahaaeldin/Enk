# Enk in-app keyboard: spec

Date: 2026-11-14. Status: APPROVED 2026-11-14; all six decisions in section 10 are made. K0 is built; K1 to K4 are one Antigravity batch (`docs/antigravity/prompt-12-keyboard.md`). **Where this spec and that prompt differ, the prompt wins**: it fixes every number and name.
Baseline: `main` at `42dd2d6`; K0 added on top.

This spec answers the team's keyboard feedback with the smallest change that removes the cause: **the writer gets its own keyboard, drawn inside the app, and stops depending on whichever keyboard the phone has.** It builds nothing from scratch that a proven component already does, and it forks nothing.

Status words used here:
- **Researched**: read in a project's repository or documentation.
- **Not verified**: I have not seen it work, or I could not confirm it from the sources.

---

## 1. What this fixes

| Team feedback | Cause today | What fixes it |
|---|---|---|
| SwiftKey repeats the last word on space or Enter | The writer guesses edits from a hidden text field the keyboard owns; a keyboard that re-marks a word as composing gets the word inserted again (reproduced in a test, `docs/superpowers/specs/2026-11-14-enk-round-2-fixes.md` section 3) | No hidden text field for this keyboard: a key press becomes one exact writer event |
| Autocorrect shows the corrected and the uncorrected word | Same cause | Same fix. The in-app keyboard has no autocorrect, which is also what a typewriter has |
| Samsung's own click races Enk's sound | Enk cannot silence another keyboard, and it sounds after the keyboard has already clicked | Enk owns the keyboard: sound and haptic fire on touch-down, once |
| Gboard works only after its tap sound is switched off | Same | Same |
| Cursor and newline instability | Partly the same cause (the caret follows the phantom insert) | Partly this; the rest is the caret and feed work in the round 2 spec, section 4 |

## 2. The simplest design

The writer already has the pieces. The only change is where its input comes from.

```
today:   system keyboard -> hidden text field -> ImeBridge (guesses) -> WriterEvent -> reducer
now:     Enk keyboard ----------------------------------------------> WriterEvent -> reducer
         hardware keyboard -> onPreviewKeyEvent ------------------------^
         (optional) system keyboard -> hidden text field -> ImeBridge (kept, minimal fix) -^
```

- **The reducer, the page, the carriage, the sounds and the haptics do not change.** The keyboard emits the events the reducer already understands: `Insert(text)` for a character, `Backspace`, `Insert("\n")` for Enter, `Insert(" ")` for space, `MoveCaret` for the space-bar cursor drag.
- **No composing state exists on this path.** `composingRange` stays null, so every composing rule in the reducer is simply not used.
- **The system keyboard is kept as an option, not removed.** Setup gets **Keyboard: Enk or System**. It matters for voice typing, languages without a layout, and anyone who needs their own keyboard. The system path keeps the existing bridge, plus the small protective fix in section 7.

## 3. What is reused, and from where

| Need | Component | License | Cost |
|---|---|---|---|
| **Key layouts** (letters, symbols, numbers, popups) | FlorisBoard's English data: `characters/qwerty.json` (letter order), `popupMappings/en.json` (long-press alternatives, the `~right` punctuation list), `symbols/western.json` (symbol sets). I read the files; their format has selectors, currency slots and direction variants that we do not need, so the data is **rewritten into one small Enk JSON file** (`app/src/main/assets/keyboard/en.json`, fully specified in the prompt) rather than parsing their format | Apache-2.0 (the repository license), attributed in `NOTICE.txt`, `CREDITS.md` and the Credits screen | One data file |
| **JSON parsing** | `org.json`, which is part of Android; the loader validates strictly | none, no dependency | small |
| **Key shape, rows, widths** | The layout files give rows, a width as a fraction of the row, and long-press popup lists. A Compose `Row` and `Column` draw them | Compose, already a dependency | small |
| **Gestures** | Compose `awaitEachGesture` per key. Each key has its own pointer input, so two thumbs typing at once work without extra code | Compose | small |
| **Callout bubble** | Compose `Popup` | Compose | small |
| **Sound** | The existing `SoundPoolPlayer` and sound sets | ours | none |
| **Haptics** | Enk's existing `Haptics.pulse(ms)`, with the same pulses the system path uses today (8 ms letters and Backspace, 14 ms space and Enter). The platform key-press constants were considered and dropped: the existing path is tested, honours the Haptics setting, and the references I read disagree about the constants' values | ours | none |
| **Hardware keyboard** | Compose `Modifier.onPreviewKeyEvent` with `KeyEvent.utf16CodePoint` and `Key.Backspace` | Compose | small |
| **Accessibility** | Compose semantics: one button per key, role and label | Compose | small |
| **Colours, glass, springs** | The shell's tokens and the glass from the round 2 spec | ours | already planned |

**Not reused, on purpose:**
- **FlorisBoard's keyboard code.** It is a whole input-method app, built to talk to Android's input protocol, which is the thing we are leaving. Taking its code would bring that protocol with it.
- **HeliBoard, OpenBoard, Fossify, Unexpected Keyboard.** GPL-3.0. Their code or layout files cannot enter an Apache-2.0 app without making all of Enk GPL-3.0.
- **FUTO Keyboard.** Its "Source First" license is custom, not free software, and F-Droid would refuse it.
- **A Compose keyboard library.** I searched and found none that is production-ready with long-press popups. The closest, `Actinis/keyboard` (Apache-2.0), says it is for prototyping. The key grid is small enough to write.

## 4. The keyboard

### 4.1 Components

| Piece | Responsibility | Pure and testable |
|---|---|---|
| `KeyboardLayout` | The loaded layout: pages (letters, symbols, numbers), rows, keys | yes |
| `LayoutLoader` | Reads a layout JSON with `org.json` into `KeyboardLayout`; resolves popups from a popup mapping; skips `//` comment lines | yes |
| `KeyboardState` | Shift (off, once, caps lock), the current page, and the transitions | yes |
| `KeyAction` | `Char(String)`, `Backspace`, `Enter`, `Space`, `Shift`, `Page(page)`, `SystemKeyboard`, `CursorDrag(dx)` | yes |
| `EnkKeyboard` | The composable: draws the keys, handles touches, emits `KeyAction` | Compose test |
| `KeyboardToWriter` | Turns a `KeyAction` into a `WriterEvent` (and the sound and haptic for it) | yes |

`EnkKeyboard` knows nothing about the writer. It takes a layout and a callback.

### 4.2 Keys and states

- **Letters, space, Enter, Backspace, Shift, a page key, a globe key, comma and full stop beside the space bar.** One English letter page and two symbol pages (numbers, then more symbols) in the first release.
- **A character is typed on touch-down**, like a typewriter strike, with its sound, haptic and callout. A long-press alternative then replaces that character with the writer's `Replace` event. Space is typed on touch-up, because a drag on the space bar moves the caret instead.
- **Shift:** a tap shifts one letter; a quick double tap sets caps lock; a tap while caps lock is on clears it. The shift key shows its state.
- **Long press** on a letter opens its popup (accents, alternatives) from the popup mapping. Drag onto an alternative and release to pick it.
- **Backspace** repeats while held, starting after 400 ms and speeding up.
- **Space-bar drag** moves the caret by columns, as the cursor swipe on a phone keyboard does, using the writer's `MoveCaret`.
- **Enter** inserts a newline. The writer is multi-line, so it never submits.
- **Globe key and a small keyboard button in the writer's top bar:** switch to the system keyboard for this session, for emoji, voice typing or another language; the button switches back. No auto-capitalisation (a typewriter has none).
- **No emoji panel, no suggestions bar, no autocorrect, no swipe typing, no voice key** in the first release. They are the parts of a keyboard that need dictionaries and an engine; a typewriter has none of them.

### 4.3 Look (Apple-style)

Starting values to tune on a phone, in line with the round 2 glass work:

| Part | Spec |
|---|---|
| Tray | A glass bar the width of the screen, thick material, with a hairline on top |
| Letter keys | Rounded 8 dp, light fill in day and dark fill in night, a thin lower edge, label in the system font at about 22 sp |
| Function keys | A slightly darker fill than letter keys, as on the iOS keyboard |
| Gaps | 5 dp between keys, 8 dp between rows. The touch area of a key includes half its gap, so the effective target is at least 48 dp tall |
| Press | The key scales to 0.96 on touch-down with the `press` spring and returns with `default` |
| Callout | A rounded bubble above the pressed letter, with the letter at about 32 sp, drawn with `Popup`; none for function keys |
| Shift and symbols pages | A short slide of 120 ms, none at the Light effects level |
| Theme | Follows the shell: Light, Dark and the chosen theme's colours. The writer's steel chassis stays as it is; the keyboard sits under it as glass |

Not copied: Apple's SF font, its glyphs, or its exact artwork. The shell's font and our own drawn glyphs are used.

### 4.4 Sound and haptics

- A letter key fires `SoundEvent.Key`, space `Space`, Enter `Enter` and Backspace `Backspace`, **on touch-down**, from the same event that starts the key animation.
- The haptic fires in the same frame (8 ms on letters, 14 ms on space and Enter, as today).
- Because the keyboard is ours, there is exactly one sound per key. The Setup row "Keyboard click" and the keyboard-detection pill in the round 2 spec apply only when Keyboard is set to System.

### 4.5 Layout in the window

- The keyboard is part of the writer's screen: a `Column` with the writer on top and the keyboard below. The writer's available height shrinks by the keyboard's height, exactly as it does today when the system keyboard opens, so `WriterGeometry`, the compact layout and the rail position need no change.
- Height: about 5 rows of 48 dp keys plus gaps (about 260 dp at 360 dp width), a little less in landscape. In a compact landscape window the keyboard falls back to three rows with the number row folded into the symbols page.
- The system keyboard is never shown while Keyboard is set to Enk: no text field is focused.

## 5. Writer integration

- `WriterScreen` gets a `keyboardMode` (Enk or System) from settings.
- **Enk mode:** no `BasicTextField` is focused. The keyboard's `KeyboardToWriter` calls `viewModel.dispatch(...)`, plays the sound and the haptic, and sets `slugChar` and `strikeKey` for the carriage, as the Insert path does today.
- **Hardware keyboard:** `Modifier.onPreviewKeyEvent` on the writer's root turns a typed character, Backspace, Enter and the arrow keys into the same events in both modes.
- **System mode:** exactly today's path, plus section 7.
- A new note, a page change and the restored note keep working: they change the page text, not the keyboard.

## 6. Settings and defaults

- **Setup > Writing > Keyboard: Enk, System.** Default Enk.
- The Autocorrect switch applies only in System mode; in Enk mode it is shown disabled with the line "Enk's keyboard has no autocorrect."
- The "Keyboard click" row and the per-keyboard guidance (round 2 spec, section 6) apply only in System mode.

## 7. The system-keyboard path: the one small fix that stays

The full input-engine rewrite in the round 2 spec (section 3) is no longer needed for the main path. System mode keeps the existing bridge with one protective change:

- **A change of the composing range with no change of text produces no edit.** `ImeBridge` today turns "the same text, now composing" into an insert; that is the SwiftKey repeat. The text comparison moves ahead of the composing branch.
- **Loop breaker:** if the app writes the hidden field more than three times in 250 ms, it stops writing and takes the field's text as the page.
- **Tests:** the three keyboard scripts from the round 2 spec's `KeyboardScriptTest` (mark an existing word as composing with no text change; autocorrect a committed word; the feedback loop with a fake keyboard that re-marks after each write). They fail today and must pass.

About 40 lines of production code and the three tests. Nothing else of section 3 of the round 2 spec is built unless the team still reports problems in System mode.

## 8. Languages

- **First release:** English QWERTY, plus the languages the team names (decision D2). Each is one layout file and its popup mapping, copied from FlorisBoard's folders.
- **Right-to-left languages** need a check on a phone: key order, the Backspace and Enter positions, and caret movement in the writer. Not verified.
- A language the keyboard lacks is typed through the globe key and the system keyboard.
- **Adding a language later** is two JSON files and an entry in a list; no code. That is the benefit of reusing the layout format.

## 9. Phases

| Phase | Content | Done when |
|---|---|---|
| K0 | **Built.** The bridge fix (section 7), larger than first planned: the bridge now derives every edit from the text alone and a new `WriterEvent.Replace` handles corrections, so the SwiftKey repeat and the autocorrect double word cannot occur on the system path. 11 new tests fail on the old bridge and pass now; the full gate is green; typing walked with Gboard on the emulator | done |
| K1 | Layout files, `LayoutLoader`, `KeyboardState`, `KeyAction`, `KeyboardToWriter`, with their tests and the licence attribution | Every shipped layout parses; every popup resolves; the state machine is covered |
| K2 | `EnkKeyboard` with keys, shift, symbols page, callout, long-press popups, Backspace repeat, space drag; sound and haptic on touch-down | The Compose tests pass; the keyboard is walked on the emulator |
| K3 | Writer integration, the Keyboard setting, hardware keys, the globe key, the compact landscape layout | The writer works end to end in both modes; the full gate is green |
| K4 | The team's languages, accessibility, the device pass, tuning | The device checklist in section 11 is filled in on real phones |

K1 to K4 are **one** Antigravity batch with four commits (decision D3), written as a prompt with every name, number and test fixed, so the coder makes no decisions.

This replaces round 2 phases R2-A (the input engine) and R2-B (keyboard sound guidance, for the Enk keyboard only). Phases R2-C to R2-G continue unchanged; the keyboard's glass and motion use their tokens.

## 10. Decisions for the author

| # | Decision | Options | My recommendation |
|---|---|---|---|
| D1 | Default keyboard on a fresh install | (a) Enk (b) System | **DECIDED: (a)** |
| D2 | Languages in the first release | English plus the ones you name | **DECIDED: English only** |
| D3 | Who builds K1 to K4 | (a) Antigravity (b) Claude | **DECIDED: (a), in one go, no further batches; K0 built by Claude** |
| D4 | Emoji | (a) Through the globe key and the system keyboard (b) An emoji panel later | **DECIDED: (a)** |
| D5 | The keyboard's surface | (a) Glass tray (b) Solid | **DECIDED: (a), with (b) at the Light effects level** |
| D6 | Autocorrect in Enk mode | (a) None, as a typewriter (b) A dictionary later | **DECIDED: (a)** |

## 11. Tests and the device checklist

- **JVM:** layout parsing (every shipped file, comment lines, missing fields, huge and empty files); popup resolution; `KeyboardState` transitions (shift once, caps lock, page changes, shift clears after a letter); `KeyboardToWriter` for every `KeyAction` (empty page, a full page, surrogate-pair keys, Backspace at the start of page one).
- **Compose:** a touch-down on a key fires its sound before the touch-up; two simultaneous touches both register; a long press opens the popup and a drag onto an alternative inserts it; Backspace repeat starts after 400 ms; every key is one accessibility button with a label.
- **Property test:** a random sequence of 2,000 key actions leaves the page equal to what a simple model of the same keys produces.
- **Latency:** the existing `TypeToStampLatencyTest` is extended to type through the keyboard; the frame budget stays enforced on a physical phone only.
- **Device pass (on real phones):** typing speed with two thumbs, the sound on every key, haptics, shift and caps lock, the space-bar drag, the long-press popup, landscape with the keyboard up, TalkBack (explore by touch and double-tap), a Bluetooth keyboard, the globe key to the system keyboard and back, and one RTL language.

## 12. Risks and unknowns

- **Resolved:** the FlorisBoard files use selectors and direction variants, so Enk converts the English data to its own format instead of parsing theirs.
- **Not verified:** whether the layout files carry their own license headers; check each file we copy.
- **Not verified:** TalkBack on a custom keyboard. The design uses standard button semantics, but this is the part most likely to need work.
- **Lost in Enk mode:** voice typing, swipe typing, suggestions, emoji search, and any language without a layout. System mode keeps all of them.
- **Hardware keyboards** that need an input method for composition (some Asian languages) must use System mode.
- **Rough size** of K1 to K4 is a guess.

## 13. What changes in the other specs

- **Round 2 spec** (`2026-11-14-enk-round-2-fixes.md`): section 3 is reduced to the small bridge fix in section 7 of this spec; section 6 applies only to System mode; D3 ("a keyboard built into Enk, its own spec after 1.0") is replaced by this spec, as the author asked.
- **Design system:** the keyboard tray joins the glass components in section 12 once K2 is done.

## 14. Sources

- FlorisBoard repository (Apache-2.0, layouts and popup mappings under `app/src/main/assets/ime/keyboard/`): https://github.com/florisboard/florisboard
- HeliBoard, GPL-3.0, layout format notes (not reused): https://github.com/Helium314/HeliBoard/blob/main/layouts.md
- AnySoftKeyboard, Apache-2.0, add-on model (reference): https://github.com/AnySoftKeyboard/AnySoftKeyboard
- Simple Keyboard, Apache-2.0, minimal reference: https://github.com/rkkr/simple-keyboard
- FUTO Keyboard, Source First 1.1 (not usable): https://github.com/futo-org/android-keyboard
- Keyboard haptic constants: https://developer.android.com/reference/android/view/HapticFeedbackConstants
- Compose haptic types: https://developer.android.com/reference/kotlin/androidx/compose/ui/hapticfeedback/HapticFeedbackType
