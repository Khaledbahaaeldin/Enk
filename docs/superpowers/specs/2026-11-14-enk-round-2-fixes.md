# Enk human-testing round 2: fix spec

Date: 2026-11-14. Status: APPROVED. All six decisions in section 9 were made on 2026-11-14.
Baseline: `main` at `639064f`.

This spec covers the team's second round of feedback: keyboard behaviour (Samsung, Gboard, SwiftKey), the missing motion and liquid glass, lamp realism, writer stability, and the lamp controls.

Status words used here:
- **Reproduced**: I made it happen in a test or on the emulator.
- **Read in code**: the cause is visible in the code, but I did not make it happen.
- **Not verified**: a hypothesis, or something that needs a real phone.

---

## 1. What is wrong, in one table

| # | Feedback | Root cause | Status |
|---|---|---|---|
| 1c | SwiftKey repeats the last word hundreds of times on space or Enter | The writer treats "the keyboard marked existing text as composing" as "the keyboard typed that text". It inserts the word again, then rewrites the keyboard's field, and the keyboard reacts to the rewrite. That is a loop. | Reproduced in a test. Not traced on a phone with SwiftKey. |
| 4a | With autocorrect, the corrected and the uncorrected word both appear | Same defect. Correcting a committed word is "mark the word as composing, then replace it". The mark is read as an insert. | Reproduced in a test: `teh ` becomes `teh the`. |
| 4b | The cursor is unstable | Partly the same defect (the caret follows the phantom insert). Partly section 4.4 (feed and rail animation). | First part reproduced. Second part read in code. |
| 4c | The page moves up sometimes fast, sometimes slow | The line feed is a fixed 200 ms animation that restarts on every line change, so speed depends on distance and on interruptions. The rail also jumps with no animation when the keyboard changes height. | Read in code. |
| 1a | Samsung Keyboard's click races with Enk's sound | Two things. An app cannot switch another keyboard's sound off. And the keyboard clicks on touch-down while Enk sounds after the keyboard delivers the text, so the two are heard as a double hit. | First part researched. Timing not measured. |
| 1b | Gboard is perfect once its tap sound is off | Expected. It confirms the sound path is right when only one sound plays. | Reported by the team. |
| 2 | Animations are nearly absent; the glass is not liquid | The design system (12.9) specified motion that was never built: screen changes, the context menu, the alert, press feedback on most controls, theme cross-fade. The glass uses the weakest settings of the library. | Read in code. See 5.1 for the list. |
| 3 | The lamp is only a little realistic | The lamp is a blurred trapezoid plus a circle, and the text is faded by an ellipse mask. Nothing is computed from a light source. | Read in code. |
| 4d | Lamp sliders should be a pop-up with liquid glass sliders | A design change, not a bug. | New requirement. |

**About my own audits:** the missing shell motion in row 2 was in the approved design system. Batches 7 and 8 were audited and merged without it, and I did not catch it. Section 5.1 lists every gap so that this round is checked against the list, not against a general impression.

---

## 2. Order of work

| Phase | Content | Why this position |
|---|---|---|
| R2-A | Input engine (section 3) and caret and feed (section 4) | It corrupts what people write. Nothing else matters until this is right. |
| R2-B | Keyboard sound guidance (section 6) | Small, and it closes the Samsung complaint as far as an app can. |
| R2-C | Effects levels, motion tokens, glass material v2 (section 5) | The foundation every later screen change uses. |
| R2-D | Motion on every interaction (section 5.4) | Applies R2-C screen by screen. |
| R2-E | Lamp lighting (section 7) | Independent of the shell work. |
| R2-F | Lamp pop-up and glass sliders (section 8) | Needs R2-C's glass and R2-E's lamp. |
| R2-G | Device pass on real phones, tuning, goldens | Last, because the numbers in sections 5 and 7 are starting values. |

Every phase ends with the full gate, an emulator walk, and its device checklist.

---

## 3. R2-A: the input engine

### 3.1 The defect

The writer has two copies of the text: the hidden text field the keyboard edits, and the page in `WriterState`. `ImeBridge` tries to guess what the keyboard did from a before and after snapshot, and keeps its own idea of where the composing word is. The reducer then applies the guess.

The guess is wrong whenever the composing region changes without the text changing. Reproduction, run on `639064f`:

| Keyboard does | Field text | Composing | Writer event | Page becomes |
|---|---|---|---|---|
| marks the word `hello` as composing | `hello` | 0..5 | `Insert("hello", composing)` | `hellohello` |
| marks `teh` in `teh ` to correct it | `teh ` | 0..3 | `Insert("teh", composing)` | `teh teh` |
| replaces it with `the` | `the ` | 0..3 | `Insert("the", composing)` | `teh the` |

After each wrong insert the page no longer equals the field, so `syncField` overwrites the field. The keyboard sees its text changed under it and marks the word again. SwiftKey does this on every space and Enter, which matches "hundreds of times".

### 3.2 The new model

**The keyboard's field is the source of truth for what was typed. The writer mirrors it with exact edits. It never guesses.**

1. **One edit type.** Replace `Insert(text, composing)` and the bridge's bookkeeping with one event:
   `WriterEvent.Replace(start: Int, end: Int, text: String, composing: IntRange?)`.
   It means "characters `start` until `end` of the page are now `text`". `composing` is the field's composing range, copied as is. It is display information only; it never causes an edit.
2. **A pure diff.** `EditDiff.between(old: String, new: String, newCaret: Int): TextEdit?` returns the smallest replaced span, or null when the texts are equal.
   - Common prefix and suffix, never splitting a surrogate pair.
   - When the same character repeats (`aa` to `aaa`), the edit is placed so it ends at `newCaret`. The text result is the same either way; this keeps the stamp and the struck ranges on the right character.
3. **Rules for `onValueChange`.**
   - R1. Texts equal: no page change. Update the caret from the selection, and the composing range from the field. Nothing else.
   - R2. Texts differ: apply exactly one `Replace`.
   - R3. Never write to the field while it has a composing range.
   - R4. The app writes to the field only for changes the app itself made: a page break, a page change, a loaded note, a strike-through (3.3), a length cap.
   - R5. Loop breaker. If the app has written the field more than 3 times in 250 ms, stop writing, take the field's text as the page, and count it in a debug counter. This cannot fix a wrong model, but it turns any future loop into one wrong word instead of hundreds.
4. **Delete `ImeBridge`.** `wasComposing`, `lastComposedText`, `composingStart` and `composingEnd` go. `syncField` shrinks to rule R4.
5. **The reducer** applies `Replace` to the page: shift struck ranges after the span, drop struck ranges inside it, set the caret, keep the composing range. Pagination waits until composing ends, as today.

### 3.3 Strike-through mode

In Strike-through mode a backspace keeps the letter on the paper, so here the page must differ from what the keyboard thinks.

- A delete inside the composing word is a real delete. The word is not on the paper yet.
- A delete of committed text (an edit with empty `text`, outside the composing range, not while composing) marks those characters struck, puts the caret before them, and writes the full text back to the field (allowed by R4, and R3 holds because nothing is composing).
- A replacement of committed text (autocorrect, a suggestion) is applied as a replacement. It is the keyboard fixing a word, not the user striking one.

### 3.4 Sound, stamp and haptics

`InsertFeedback` keeps its rules, fed by the edit: `added = text.length - (end - start)`. Added inside composing text is a key; removed is a backspace; a commit ending in a space or newline is a space or Enter; a paste is silent. A change of the composing range alone is silent.

### 3.5 Tests

- **`EditDiffTest`**: insert, delete, replace, repeated characters, surrogate pairs, empty strings, equal strings.
- **`KeyboardScriptTest`**: a small script format replays a list of field values through the real `onValueChange` logic and the reducer. One script per behaviour:
  - type a word letter by letter (composing)
  - commit with space; commit with Enter
  - mark a committed word as composing with no text change (the SwiftKey case)
  - autocorrect a committed word (the `teh` case)
  - pick a suggestion
  - glide typing: a whole word arrives at once
  - backspace into a committed word (the keyboard re-composes it)
  - double space to full stop (`"  "` becomes `". "`)
  - move the caret while composing
  - emoji and other surrogate pairs
  - a keyboard that never composes (one committed character at a time)
- **Invariants checked after every step of every script:**
  - Clean erase: page text equals field text.
  - The app never wrote the field while it was composing.
  - The app wrote the field zero times in a script with no page break and no strike-through.
- **Fuzz test**: 2,000 random edit sequences with random composing ranges, same invariants, fixed seed.
- **Loop test**: a fake keyboard that re-marks the last word after every app write. The page must stop changing within 4 steps.

### 3.6 Real traces

The scripts above are my model of the three keyboards. To replace the model with facts:

- A recorder in the `debug` source set only (never in release) keeps the last 300 field values in memory.
- Holding the page counter for 2 seconds copies them to the clipboard as a script.
- The team records one trace per keyboard for: a sentence, an autocorrect, a backspace into a word. Each trace becomes a permanent test.

No network, no file, nothing in release builds.

### 3.7 Acceptance

| Keyboard | Type a sentence | Space, Enter | Autocorrect | Backspace into a word | Glide | Suggestion |
|---|---|---|---|---|---|---|
| Gboard | | | | | | |
| Samsung Keyboard | | | | | | |
| SwiftKey | | | | | | |

Every cell: the text on the paper equals what was typed, no repeated word, the caret is where the keyboard's cursor is. Filled in on real phones.

---

## 4. R2-A: caret, feed and rail

### 4.1 What happens today

- `PaperText` moves the page with `tween(200 ms)`, restarted on every line change. One line and ten lines both take 200 ms, and a restart mid-flight starts a new 200 ms from wherever it was. That is the "sometimes fast, sometimes slow".
- `railY = round(0.62 x height)`. When the keyboard changes height (a suggestion strip appears, the emoji panel opens, the keyboard hides), the rail and caret jump in one frame while the text takes 200 ms to follow.
- The caret snaps to its column while the carriage glides for 110 ms, so for a few frames they disagree.

### 4.2 Changes

| Part | Now | Change to |
|---|---|---|
| Line feed | tween 200 ms, restarted | one `Animatable` with a spring, response 0.22 s, damping 1.0, re-targeted with its velocity kept |
| Long jumps (more than 3 lines) | same 200 ms | same spring, response 0.30 s |
| Rail and caret on a height change | instant | `railY` is animated with a spring, response 0.30 s, damping 1.0. While the keyboard itself is animating, follow it frame by frame instead. |
| Caret column | snaps | drawn from the same animated x as the carriage |
| Caret while anything moves | may blink | solid until 500 ms after the last motion |

### 4.3 Tests

- A spring re-targeted mid-flight never reverses direction and never jumps (unit test on the animation maths, like `Physics`).
- Ten Enters in a row: the feed offset is monotonic.
- A height change of 48 dp: `railY` and the text offset differ by less than 1 dp on every frame.

### 4.4 Not verified

I have not seen the cursor problems on a phone. Sections 3 and 4 remove every cause I can find in the code. If the team still sees a jump after R2-A, the recorder in 3.6 gives the exact steps.

---

## 5. R2-C and R2-D: liquid glass and motion

### 5.1 What was specified and not built

| Design system 12.9 says | Built? |
|---|---|
| Screen change: cross-fade 250 ms | No. A plain `when` swaps screens in one frame. |
| Context menu: scale 0.6 to 1 from the card, 400 ms | No animation. |
| Alert: scale 1.08 to 1, 350 ms; scrim 300 ms | No animation. |
| Press feedback on pointer-down for buttons, cards, paper pills | Note cards only. |
| Theme change: colours cross-fade over 500 ms | Present in `EnkShellTheme`; not checked on a phone. |
| Card entrance, staggered | Present in the Box only. |
| Lens drag, flick, stretch on nav and segmented | Present. |
| Sheet drag with velocity hand-off | New divider sheet only. The Move sheet has none. |
| Refraction on Android 13 and later | Present but weak: one lens setting for everything, no highlight, no inner shadow, no depth, no dispersion. |

Find, Trash, Setup rows, About and the message pill have no motion at all.

### 5.2 Effects levels

One setting replaces the Reduce effects switch: **Setup > Display > Effects: Auto, Full, Balanced, Light.** Default Auto.

| | Full | Balanced | Light |
|---|---|---|---|
| Glass | refraction, depth, dispersion on small lenses, highlight, inner shadow, frost grain | blur and tint, highlight, refraction on the nav lens only | solid tinted surfaces, hairline edge |
| Motion | all springs, stretch, materialize | all springs, no stretch, no blur animation | 100 ms fades, springs snap |
| Lamp | full shader (section 7) | shader without grain relief and bloom | gradients |
| Paper grain | on | on | off |
| Glass layers drawn at once | no limit set | at most 3 | 0 |

**Auto picks:**
- Light, if the system has animations removed, or high-contrast text is on, or `ActivityManager.isLowRamDevice`.
- Balanced, on Android 12 and 12L (no refraction there), or while battery saver is on.
- Full, otherwise.

**Auto also watches itself.** Using the frame-time maths already in the project, if the 95th percentile frame time over 120 frames is above 1.5 frame intervals, Auto drops one level for the rest of the session. It never climbs back in the same session.

**Migration:** `reduceEffects = true` becomes Light; false becomes Auto. System "remove animations" always forces Light motion whatever the setting says.

### 5.3 Glass material v2

The library stays at Backdrop 1.0.4. I checked its classes: lens with depth and chromatic aberration, highlight, inner shadow, shadow, combined backdrops, layer blocks and custom shaders are all there. Backdrop 2.x is not needed and is still blocked: Paparazzi's newest release is 2.0.0-alpha05.1.

Starting values, to be tuned on phones in R2-G:

| Surface | Blur | Lens (height, amount) | Extras |
|---|---|---|---|
| Nav pill body | 18 dp | 16 dp, 32 dp, depth on | vibrancy, highlight, inner shadow |
| Nav lens, segmented lens, slider thumb, toggle bead | 2 dp | 12 dp, 20 dp, depth on, dispersion on (Full) | highlight; refracts content and its own track together |
| Buttons, pills, search field | 14 dp | 10 dp, 16 dp | highlight |
| Sheets, menus, alerts, lamp pop-up (thick) | 40 dp | 8 dp, 12 dp at the edge | tint, frost grain 3%, deeper shadow |

Rules:
- Effect order is colour, then blur, then lens (the library's rule).
- Lens height never exceeds the corner radius.
- Never put a light glass surface on another glass surface.
- Text on glass uses `t1` at Medium weight or heavier.
- Press and stretch scale go in the library's layer block, not `graphicsLayer`, so the backdrop does not slide under the glass.
- Surfaces materialize: blur radius and scale animate together on enter and exit.

### 5.4 Motion on every interaction

Tokens (added to `ShellMotion`; springs use response and damping as today):

| Token | Response | Damping | Use |
|---|---|---|---|
| `default` | 0.35 | 1.0 | moves with no momentum |
| `flick` | 0.35 | 0.8 | after a drag or flick release |
| `toggle` | 0.28 | 1.0 | toggles, checkmarks |
| `sheet` | 0.38 | 0.82 | sheets, pop-ups, menus |
| `press` | 0.20 | 1.0 | touch-down scale |
| `page` | 0.40 | 1.0 | screen changes |

Every spring re-targets from its live value and velocity. Nothing a finger drives uses a fixed duration. Enter and exit use the same path.

| Interaction | Motion |
|---|---|
| Any button, pill, row, tile, card: touch-down | scale to 0.96 (buttons), 0.985 (cards), 0.95 (pills) with `press`, on pointer-down, not on release; releases with `default` |
| Tab change in the shell (Box, Find, Setup) | old screen fades and moves 12 dp away from the lens direction, new one arrives from the other side, `page` |
| Box to writer (open a note) | the note card grows into the paper sheet: bounds, corner radius and text cross-fade, `page`. Back reverses it. If the shared-element API in this Compose version is still experimental, compute the bounds by hand. |
| Writer to Box (Box button) | the paper shrinks toward its card; if the card is off screen, toward the top bar button |
| New note | the ghost card grows into the paper the same way |
| Trash, About, Credits (pushed screens) | slide in from the trailing edge 24 dp with fade, `page`; back is the mirror; follows the back gesture progress on Android 14 and later |
| Context menu | scale 0.6 to 1 from the touch point, blur 0 to full, `sheet`; the card lifts to 1.03; scrim 300 ms |
| Alert | scale 1.08 to 1 and fade, `sheet`; scrim 300 ms |
| Sheets (New divider, Move to divider) | rise with `sheet`; drag 1:1; rubber band upward; dismiss when position plus projected flick passes 160 dp; release velocity handed to the spring |
| List changes (note added, deleted, moved, restored) | items animate to their new places; a removed card collapses and fades; an added card uses the entrance |
| Divider change | card entrance, 14 dp up and fade, 40 ms stagger, at most 8 cards staggered |
| Search | field lifts and widens on focus; results cross-fade per keystroke without moving cards that stay |
| Toggle, segmented, fader | lens moves with `toggle` or `default`; stretches with velocity (`k = min(0.14, |v| / 6000)`) |
| Message pill, dark-paper question | rise 16 dp and materialize; leave the same way |
| Large title | collapse into the bar tied to scroll, as built; add the scroll-edge blur |
| Theme and mode change | colours cross-fade 500 ms |
| Lamp on and off | section 7.6 |

Haptics fire on the same frame as the visual commit: 8 ms on a tab change, 14 ms on a sheet dismiss or delete. Nowhere else.

### 5.5 Budgets and tests

- Frame time, 95th percentile, on a physical mid-range phone: within one frame interval during every transition in 5.4. Measured with the existing `FrameMetrics` test, extended to drive the Box, a tab change and a sheet.
- Unit tests: each transition's values at 0, mid and end (as `computePanelMotion` is tested today); re-target without a jump; Light level gives 100 ms fades.
- Compose tests: press scale starts on pointer-down; a sheet dragged past the threshold dismisses; a second tap during a screen change lands on the right screen.
- Paparazzi cannot draw blur or shaders, so goldens only cover Light. Full and Balanced are checked by a fixed list of emulator screenshots and on phones.

---

## 6. R2-B: other keyboards' sounds

### 6.1 Facts

- Each keyboard stores and plays its own key sound. Android gives apps no way to turn it off.
- Muting the system sound stream from Enk is rejected. On many phones that stream is tied to the ringer, the call can be refused, and it would silence things the user did not ask to silence.
- The keyboard clicks when the finger lands. Enk sounds when the text arrives. Two sounds a few tens of milliseconds apart read as a race. Not measured.

### 6.2 Changes

1. **Know the keyboard.** Read the current keyboard's id (`Settings.Secure.DEFAULT_INPUT_METHOD`, no permission needed).
2. **Tell the user once per keyboard.** The first time the writer opens with a keyboard not seen before, a pill shows: "Samsung Keyboard has its own key sound. Turn it off to hear only the typewriter." Buttons: "Show me" and "Later". Muted Enk: no pill.
3. **"Show me"** opens a short sheet with that keyboard's steps and a button that opens the system keyboard settings.

   | Keyboard | Steps (to be checked on each phone) |
   |---|---|
   | Samsung Keyboard | Samsung Keyboard settings > Swipe, touch and feedback > Touch feedback > Sound: off |
   | Gboard | Gboard settings > Preferences > Sound on keypress: off |
   | SwiftKey | SwiftKey settings > Sound and vibration > Keypress sound: off |
   | Any other | "Open your keyboard's settings and turn off key sound." |
4. **Setup > Sound > Keyboard click** (built last round) opens the same sheet.
5. **Tighter timing.** Measure tap-to-sound with a microphone on one phone before changing anything. Then: keep all key samples loaded, and start each sound from the input callback with no hop to another thread. Target: Enk's sound starts within 40 ms of the keyboard delivering the key.

### 6.3 The only complete fix

A keyboard built into Enk would own the sound, play it on touch-down, and remove every difference between Gboard, Samsung and SwiftKey. It is a large feature (layouts, languages, autocorrect or the lack of it, accessibility). It is not in this spec. See decision D3.

---

## 7. R2-E: lamp lighting

### 7.1 Principle

Compute the picture from one light source. On Android 13 and later this is one shader pass over the paper. On Android 12, and at the Light level, it falls back to gradients shaped to match.

### 7.2 The light

- **Source:** the lamp slot on the carriage, at (carriage x, `railY` - 24 dp), a virtual 28 dp above the paper, aimed up the page.
- **Fall-off:** brightness at a paper point is `cos(angle) / (distance² + height²)`, normalised so the brightest point equals the Intensity slider. This gives a bright pool near the carriage that fades smoothly up the page, instead of a hard-edged trapezoid.
- **Cone:** the Beam slider is the cone's half-angle, 18° at 30 to 46° at 100, with a soft edge between the inner and outer cone.
- **Colour:** the Warmth slider is a colour temperature, about 4000 K at 0 to 1900 K at 100, from a small table. It replaces the straight RGB mix.
- **Intensity** is mapped perceptually, so each slider step looks like an equal step.

### 7.3 What the light falls on

- **Paper:** paper colour x (ambient + light). Ambient is a faint cool tone, so the warm pool has something to contrast with.
- **Grain relief (Full):** the existing grain tile also tilts the surface slightly, so light raking up the page shows the paper's texture, within ±6%.
- **Ink (decided, D1):** ink absorbs light, so inside the pool the letters are dark on lit paper, as on a real page under a lamp. This replaces today's letters that glow in the lamp colour on dark paper.
- **Steel:** the rail gets a highlight streak that is strongest beside the carriage and fades along the rail; the carriage top catches the lamp colour; the end caps get a small glint.
- **Slot:** the lamp slot itself glows, with a soft bloom of 12 dp at 15% (Full).
- **Air (Full):** a faint scatter in the cone, 4%, replaces the solid beam.

### 7.4 Keeping it clean

- Tone mapping, so the centre never burns out to flat white.
- Dither with the grain, so the fall-off does not band on OLED screens.
- The paper outside the pool keeps enough light that the nearest lines stay readable. Target: contrast of at least 4.5 to 1 for the caret line and the three lines above it at default settings. Lines far from the pool fade, as today.

### 7.5 Cost

One pass, uniforms only (position, cone, colour, intensity, time). Target under 1.5 ms on the GPU at 1080 x 2400. Balanced drops grain relief, bloom and scatter. Not measured yet.

### 7.6 On and off

- **On:** the lamp warms up over 420 ms. It starts redder and dimmer and settles at the set colour, as a filament does. The paper darkens around it over the same time.
- **Off:** 250 ms, with the slot glowing a little longer than the pool.
- Light level: 100 ms fade.
- The pool is fixed to the carriage. No lag: it is one rigid object.

### 7.7 Tests

- Unit: fall-off is 1 at the centre and falls with the square of distance; cone edges; the temperature table at 5 points; perceptual intensity is monotonic.
- Golden: the gradient fallback.
- Emulator and phone: a fixed set of captures at 3 warmth, 3 intensity and 3 beam values, and an OLED phone in a dark room.

---

## 8. R2-F: lamp pop-up and glass sliders

### 8.1 What changes

- The steel control panel under the rail is removed. So are `Fader`, `ControlPanel`, their goldens, and the panel clearance in `WriterGeometry`. The rail no longer moves up when the lamp is on.
- A **lamp button** joins the top bar, left of the Box button. 48 dp target, glass capsule, a small bulb mark that glows in the lamp colour when the lamp is on.
- Tapping it opens the **lamp pop-up**. Holding the marker still toggles the lamp, as today.

This brings glass into the writer for floating controls only: the two top-bar buttons, the pop-up, and the question pill. Paper, rail and carriage stay steel. It changes the earlier rule that glass lives only outside the writer.

### 8.2 The pop-up

- Thick glass, 300 dp wide, radius 28 dp, anchored under the lamp button, right edges aligned.
- Content: a row "Lamp" with a toggle, then three sliders: Warmth, Intensity, Beam, each with its value.
- Opens from the button: scale 0.6 to 1 with the top-right corner fixed, blur and opacity together, `sheet` spring. Closes the same way.
- Closes on a tap outside, Back, or typing.
- The paper stays visible and live behind it, so every slider change shows on the page while the finger moves.
- Short windows (landscape, keyboard up): the pop-up becomes a row of three compact sliders under the top bar.

### 8.3 The glass slider

| Part | Spec |
|---|---|
| Track | 6 dp capsule; the filled part in the lamp colour, the rest `field2` |
| Thumb | 56 x 32 dp capsule lens: height 12 dp, amount 16 dp, dispersion at Full; it refracts the track and the content behind it together |
| Touch-down | thumb grows to 1.12 with `press`, and its tint clears so the track reads through it |
| Drag | 1:1 from where it was grabbed; a tap on the track springs the thumb there with `default` |
| Stretch | `k = min(0.14, |v| / 6000)`; wider by `k`, shorter by `k / 2` |
| Detents | the existing tick sound and 4 ms pulse, at the spacing the steel faders use today |
| Release | settles with `flick` if moving, else `default` |
| Hit area | 48 dp tall, full row width |
| Accessibility | slider semantics with value and range; TalkBack can adjust it; labels "Warmth", "Intensity", "Beam" |
| Light level | solid thumb, no lens, no stretch |

The same slider replaces the Bell fader in Setup, so the app has one slider.

### 8.4 Tests

- Compose: the button opens and closes the pop-up; an outside tap closes it; typing closes it; a slider drag changes the setting and the tick fires at each detent; values survive closing.
- Geometry: `railY` is the same with the lamp on and off.
- Accessibility: 48 dp targets; slider semantics; the pop-up is announced as a dialog.
- Goldens: the pop-up at the Light level, light and dark.

---

## 9. Decisions for the author

| # | Decision | Options | Outcome |
|---|---|---|---|
| D1 | The lamp's look | (a) Lit paper, dark ink: the real thing. (b) Keep glowing ink on dark paper, with the new pool shape. | **DECIDED 2026-11-14: (a), lit paper and dark ink**, chosen from the mockup `docs/mockups/enk-round2-lamp-glass-motion.html`. |
| D2 | The lamp button | (a) Always in the top bar. (b) Only while the lamp is on. | **DECIDED: (a), always in the top bar.** |
| D3 | A keyboard built into Enk | (a) Its own spec after 1.0. (b) Now. (c) Never. | **CHANGED 2026-11-14: built now, as an in-app keyboard; see `2026-11-14-enk-in-app-keyboard.md`. Section 3 of this spec is reduced to that spec's K0 (already built); section 6 applies only to the system keyboard.** |
| D4 | Default effects level | (a) Auto. (b) Full everywhere. | **DECIDED: (a), Auto.** |
| D5 | Mockups before building | (a) Lamp (both D1 options), pop-up and slider, and the motion set, as interactive mockups first. (b) Build straight from this spec. | **DONE:** `docs/mockups/enk-round2-lamp-glass-motion.html`. |
| D6 | Who builds | (a) Antigravity in batches, audited as before. (b) I build R2-A directly, Antigravity the rest. | **DECIDED: (b). Claude builds R2-A; Antigravity builds R2-B to R2-G in audited batches.** |

---

## 10. Risks and unknowns

- SwiftKey's real behaviour is modelled, not recorded. Section 3.6 closes this.
- The cursor complaints may have a cause I have not found. Section 4.4.
- The steps in 6.2 differ between One UI versions.
- All glass and lamp numbers are starting values. Nothing here has been seen on a phone.
- Frame-time and GPU targets are targets. Nothing is measured.
- The note-to-paper transition depends on an API whose stability in this Compose version I have not checked.
- Removing the steel faders deletes approved work (design system 6.7 and 6.8). The design system needs updating in the same change: 6.6 to 6.8, 9, 12.4, 12.9, 12.10.

## 11. Sources

- Backdrop effects and their Android version limits: https://kyant.gitbook.io/backdrop/api/backdrop-effects
- Backdrop interactive bottom bar (press scale in the layer block): https://kyant.gitbook.io/backdrop/tutorials/interactive-glass-bottom-bar
- Backdrop glass slider (lens thumb, combined backdrops): https://kyant.gitbook.io/backdrop/tutorials/glass-slider
- Backdrop releases: https://github.com/Kyant0/AndroidLiquidGlass/releases
- Paparazzi releases (2.x is alpha only): https://github.com/cashapp/paparazzi/releases
- Compose text input and composition, community notes: https://slack-chats.kotlinlang.org/t/9610579/is-there-any-way-to-force-the-composition-field-of-a-textfie
- Compose custom text input sessions: https://developer.android.com/reference/kotlin/androidx/compose/ui/platform/PlatformTextInputModifierNode
- Keyboard sounds are per keyboard: https://www.sammobile.com/news/why-does-my-phone-keyboard-make-clicking-sounds-when-i-type/
