# Keyboard device checklist

The Enk in-app keyboard needs a real phone for everything that depends on touch, sound, haptics and other keyboards. Fill in one row per phone and check. Result is `PENDING-DEVICE` until someone has run it.

| Check | Phone | Result | Notes |
|---|---|---|---|
| Typing speed with two thumbs (no dropped or doubled keys) | | PENDING-DEVICE | |
| The sound and haptic on every key (letters, Space, Enter, Backspace, Shift) | | PENDING-DEVICE | |
| The sound arrives at the same moment as the key animation (touch-down, not release) | | PENDING-DEVICE | |
| Shift once, caps lock (double tap) and the shift icon states | | PENDING-DEVICE | Once clears after one letter; caps stays until tapped again. |
| The numbers and symbols pages (123, #+=, ABC) | | PENDING-DEVICE | |
| Long-press alternatives for e, a, s and the punctuation; with shift once the alternatives shown are the uppercase ones that get inserted | | PENDING-DEVICE | |
| Backspace hold-repeat (first repeat after 400 ms, then 90 ms, then 45 ms after ten repeats) | | PENDING-DEVICE | |
| The space-bar cursor drag across a wrapped line | | PENDING-DEVICE | |
| Enter | | PENDING-DEVICE | |
| Landscape with the keyboard up: the writer still shows the caret line and the carriage | | PENDING-DEVICE | |
| A window about 360 dp tall (split screen or a small phone) | | PENDING-DEVICE | |
| TalkBack: explore by touch and double tap on three keys | | PENDING-DEVICE | |
| A Bluetooth or USB keyboard: typing, Backspace, Enter, arrows | | PENDING-DEVICE | |
| The globe key and the top-bar chip switch to the system keyboard and back with Gboard: type a sentence, Space, Enter, autocorrect on and off, no repeated word | | PENDING-DEVICE | |
| The same with Samsung Keyboard (its own key sound must not race Enk's) | | PENDING-DEVICE | |
| The same with SwiftKey (Space and Enter must not repeat the last word) | | PENDING-DEVICE | |
| Rotation mid-sentence (text, caret and keyboard page survive) | | PENDING-DEVICE | |
| Dark mode and the four themes | | PENDING-DEVICE | |
| Font scale 130% (keys keep their size; labels may clip) | | PENDING-DEVICE | |
| "Reduce effects" on (no press scale, instant page switch) | | PENDING-DEVICE | |
| Cold start: the writer does not resize or jump when the start animation ends | | PENDING-DEVICE | Checked on the emulator after the batch 12 audit; confirm on a phone. |
