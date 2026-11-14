# Keyboard Physical Device Verification Checklist

This checklist tracks physical on-device verification for the Enk in-app keyboard. Because the typewriter acoustic/haptic sync, multi-touch input, display latency, and TalkBack audio ducking require real hardware sensors and tactile feedback, these tests are executed on physical Android hardware.

| Check | Phone | Result | Notes |
|---|---|---|---|
| Touch latency: acoustic/haptic sync on Pixel 8a or similar | Pixel 8a / physical hardware | PENDING-DEVICE | Audio and tactile click should trigger immediately on touch-down with negligible perceptible latency. |
| Tap typing at 40+ wpm with thumb and index | Physical hardware | PENDING-DEVICE | Natural rapid typing without dropped keys or false multi-touch gesture triggers. |
| Long press popup selection on 'e' and 'a' | Physical hardware | PENDING-DEVICE | Long-press after 350 ms opens popup callout; drag left/right selects alternative accented character, release inputs it. |
| Space-bar cursor drag with 2+ rows of text | Physical hardware | PENDING-DEVICE | Dragging space bar past 10 dp slop smoothly traverses caret across columns and line wraps with subtle 4 ms ticks. |
| Backspace hold-to-repeat: accelerate after 10 repeats | Physical hardware | PENDING-DEVICE | 400 ms initial delay, 90 ms repeat cadence for first 10 repeats, accelerating to 45 ms thereafter. |
| Rotate to landscape: verify layout scales, does not exceed 60% of height | Physical hardware | PENDING-DEVICE | Screen rotation triggers compact/medium metrics scaling; total keyboard height remains <= 60% of window height. |
| Fast switch between Enk and system keyboard in top bar | Physical hardware | PENDING-DEVICE | Tapping top-bar keyboard switch icon toggles between Enk in-app keyboard and system IME smoothly with caret preserved. |
| Hardware keyboard: type 50 words with Shift, Backspace, Enter, arrows | Physical hardware / Bluetooth keyboard | PENDING-DEVICE | Hardware keystrokes route directly to Writer reducer; sound and haptic fire on key down; Shift/arrows handle navigation. |
| Contrast on OLED in dark and light themes (APCA Lc >= 45 for key labels) | Physical hardware (OLED) | PENDING-DEVICE | Key labels and icons maintain accessible contrast against key cap background across all 5 themes. |
| TalkBack navigation through all keys: labels and hints speak clearly | Physical hardware | PENDING-DEVICE | TalkBack accessibility focus moves cleanly across keys; custom labels speak ("Shift", "Delete", "Space", "Return"). |
