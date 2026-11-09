# Enk Start Animation Device QA Checklist

Manual verification checklist for the Enk Start Animation ("Rail becomes the writer"), splash handoff, timing gates, and transition behavior on Android virtual devices (AVDs) and physical hardware.

---

## Testing Environment & Test Runner Deviation

### Automated vs. Device Testing Deviation
- **Deviation Note**: The plan originally specified `app/src/androidTest/kotlin/app/enk/start/StartHandoffTest.kt` running as an instrumented emulator test. In this repository, `StartHandoffTest` is implemented and verified under Robolectric in `:app:testDebugUnitTest` (`app/src/test/kotlin/app/enk/start/StartHandoffTest.kt`).
- **Rationale**: Headless and CI environments run without reliable hardware GPU emulation for connected Android tests. The Robolectric Compose test runner executes virtual-clock UI node verification (`touchSkipsWithin150ms`, `coldStartEndsOnTheWriter`, `widgetLaunchSkipsAnimation`, `reducedMotionFadesOver250ms`) with deterministic time controls.
- **Requirement**: Because unit tests cannot capture the physical Android 12+ OS window transition, manual verification on an emulator or physical device using this checklist is required to validate the visual handoff.

---

## Device & Environment Information
- **Device Model / AVD Profile**:
- **Android OS Version / API Level**: (minSdk 31 / Android 12+)
- **System Theme Tested**: Light Theme / Dark Theme
- **Animator Duration Scale**: 1x (Normal) / 10x (Developer Options slow motion)
- **Reduce Motion Setting**: Disabled / Enabled
- **Build Type**: `debug` / `release`
- **Tester**:
- **Date**:

---

## 1. Splash Screen to StartOverlay Handoff (`noJumpAtHandoff`)

Enable **Animator duration scale = 10x** in Developer Options to observe the transition frame-by-frame.

- [ ] **1.1 Light Mode System Handoff**
  - Set system theme to **Light**.
  - Force stop the app (`adb shell am force-stop app.enk.notes`).
  - Cold launch the app from the launcher icon.
  - Observe the system splash screen (`#ECE8DF` background with `ic_splash_symbol_light`).
  - When loading finishes, observe the handoff to `StartOverlay` frame 0.
  - **Verification**: The symbol position, aperture size (96 dp), rail dimensions, and carriage outline must align with zero visible positional jump or flicker at the handoff boundary.
  - Observe the circular chassis reveal expanding from the center in light mode.

- [ ] **1.2 Dark Mode System Handoff**
  - Set system theme to **Dark**.
  - Force stop the app (`adb shell am force-stop app.enk.notes`).
  - Cold launch the app from the launcher icon.
  - Observe the system splash screen (`#161514` background with `ic_splash_symbol`).
  - Observe the handoff to `StartOverlay` frame 0.
  - **Verification**: The symbol position and colors match seamlessly with zero visible jump, stutter, or white/black flash between the OS window splash and the app overlay.

---

## 2. Touch Acceleration & Skip (< 150 ms)

- [ ] **2.1 Early Tap to Skip**
  - Cold launch the app with normal animator scale (1x).
  - Tap anywhere on the screen during the start animation (e.g., at 200–400 ms).
  - **Verification**:
    - The animation must immediately accelerate and complete within 120–150 ms of the touch.
    - It must not jump or freeze instantaneously; the easing smoothly settles the rail, carriage, and caret into place.
    - The typing surface becomes interactive and ready for input immediately upon completion.

---

## 3. Cold Start vs. Warm Start vs. Widget Launch

- [ ] **3.1 Cold Start (Normal Launch)**
  - Ensure the app process is completely stopped.
  - Tap the app icon to perform a cold launch.
  - When initial loading completes within 1500 ms, the start animation must play fully over 900 ms, ending smoothly on the writer with carriage and caret aligned to the restored note.

- [ ] **3.2 Slow Cold Start Gate (> 1500 ms Hard Cap)**
  - If background load or disk latency exceeds 1500 ms, `StartGate.shouldPlay` evaluates to false.
  - Verify that if the splash is held until the 1500 ms timeout, the overlay does not play late—the app lands immediately on the interactive writer screen without jarring animation delay.

- [ ] **3.3 Warm Start / Activity Recreation**
  - Open the app, type some text, then press Home (do not kill process).
  - Re-open Enk from the recent apps switcher or home screen.
  - **Verification**: The start animation must NOT play. The writer screen appears immediately with existing state preserved.

- [ ] **3.4 Widget / External Intent Launch**
  - Launch Enk via widget tap or direct note action intent.
  - **Verification**: `StartGate.shouldPlay` rejects non-main launches (`launchedFromWidgetOrIntent == true`). The start animation must be skipped entirely, displaying the target note immediately.

---

## 4. Reduced Motion (Accessibility 250 ms Fade)

- [ ] **4.1 Reduced Motion Active**
  - Enable **Remove animations** in Android System Settings (Accessibility > Text and display / Color and motion) or enable `Reduce effects` in Enk settings.
  - Cold launch the app.
  - **Verification**:
    - The complex mechanical rail expansion and carriage movement do not run.
    - Instead, the overlay smoothly cross-fades to the writer screen via a 250 ms opacity fade.
    - At completion, the writer is active, the carriage is in resting position, and the caret is solid (non-blinking).

---

## 5. Audio and Haptics Restraint

- [ ] **5.1 Zero Sound During Startup**
  - Cold launch the app with device volume turned up.
  - **Verification**: Absolutely zero audio playback occurs during the splash screen, overlay animation, or initial screen landing. Mechanical sound sets and bells only sound on explicit user keystrokes and gestures.

- [ ] **5.2 Zero Haptics During Startup**
  - Cold launch the app with haptic feedback enabled.
  - **Verification**: Absolutely zero vibration or haptic pulses are triggered during the startup timeline or handoff transition.
