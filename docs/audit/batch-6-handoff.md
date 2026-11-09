# Batch 6 Audit Handoff: Writing Screen (Gestures, Lamp, Control Panel, IME & Assembly)

## 1. Branch and Commits

- **Branch:** `batch-6/writing-screen`, based on `main` (`569ecbf`).
- **HEAD Commit:** `a21610ab81e93b3e3a10a3d8201698bf80e77615`
- **Commits on Branch:**
  - `abf54b2 refactor(design,app): introduce LocalMonoFont and compose test dependencies` (Batch 6 Prep)
  - `488ffd6 feat(writer): marker scrub and long-press gestures` (Task 17)
  - `122f4b3 feat(design): lamp beam, halo, text mask, night theme` (Task 18)
  - `da94fcc feat(design): control panel, faders, tick synth` (Task 19)
  - `8a5fe69 feat(writer): assemble writing screen with IME, sound, haptics, lamp` (Task 20)
  - `2ee5e9d fix(writer): declare VIBRATE permission and suppress lint in Haptics`
  - `a21610a fix(build): exclude NightThemeTest from release unit tests`

---

## 2. Signature Table (Generated from Code vs Plan Interfaces)

| Plan Interfaces Block | Implemented Code Signature | Location | Differences / Notes |
|---|---|---|---|
| **Task 17** `Modifier.markerGestures(onScrub: (column: Int, fraction: Float) -> Unit, onLongPress: () -> Unit)` | `fun Modifier.markerGestures(geometry: WriterGeometry, pageLength: Int, onScrub: (column: Int, fraction: Float) -> Unit, onLongPress: () -> Unit, onRelease: () -> Unit, slopPx: Float, longPressTimeoutMs: Long): Modifier` | `app.enk.writer.MarkerGestures.kt` | Added `geometry` and `pageLength` to calculate column accurately; added `onRelease` callback for snap animation; added configurable `slopPx` and `longPressTimeoutMs` with default values for test isolation. |
| **Task 17** Carriage dimensions | `CarriageGeometry.hitWidthDp = 54f`, `CarriageGeometry.hitHeightDp = 48f` | `app.enk.writer.Carriage.kt` | Encapsulated in `CarriageGeometry` object. |
| **Task 18** `@Composable fun LampLight(visible: Boolean, railYDp: Float, paperTopDp: Float, paperHeightDp: Float, lampColor: Rgb, reduceEffects: Boolean)` | `@Composable fun LampLight(visible: Boolean, railYDp: Float, paperTopDp: Float, paperHeightDp: Float, lampColor: Color, reduceEffects: Boolean, modifier: Modifier, carriageXDp: Float?, beamWidthFraction: Float, intensityFraction: Float)` | `app.enk.design.components.LampLight.kt` | Uses Compose `Color` with an overload for `Rgb`; added optional carriage tracking `carriageXDp`, `beamWidthFraction`, `intensityFraction` with default values matching DS 6.6 defaults. |
| **Task 18** `fun Modifier.lampTextMask(visible: Boolean, railYDp: Float, paperTopDp: Float, lampColor: Rgb, textMuted: Color): Modifier` | `fun Modifier.lampTextMask(visible: Boolean, railYDp: Float, paperTopDp: Float, lampColor: Color, textMuted: Color, carriageXDp: Float?, intensitySlider: Int): Modifier` | `app.enk.design.components.LampLight.kt` | Overloads for both `Color` and `Rgb`; added `intensitySlider` for dynamic mask edge alpha per DS 6.6. |
| **Task 18** Lamp geometry helpers | `object LampGeometry { fun beamBottomYDp(...); fun haloCenterYDp(...); fun textMaskCenterDp(...); fun maskEdgeAlpha(...) }` | `app.enk.design.components.LampLight.kt` | Pure functions for geometric calculations, verified by unit tests. |
| **Task 19** `@Composable fun ControlPanel(visible: Boolean, lampColor: Rgb, settings: LampSettings, onChange: (LampSettings) -> Unit, onDetent: (Int) -> Unit)` | `@Composable fun BoxScope.ControlPanel(visible: Boolean, lampColor: Color, settings: LampSettings, onChange: (LampSettings) -> Unit, onDetent: (Int) -> Unit, modifier: Modifier, enterProgressMs: Long?, liftedFaderIndex: Int?, reduceEffects: Boolean)` | `app.enk.design.components.ControlPanel.kt` | Scoped to `BoxScope` with non-scoped overload; supports both `Color` and `Rgb`; test overrides for `enterProgressMs` and `liftedFaderIndex` for deterministic golden tests; supports `reduceEffects`. |
| **Task 19** `@Composable fun Fader(label: String, value: Int, min: Int, max: Int, lampColor: Rgb, onValueChange: (Int) -> Unit, onDetent: (position: Int) -> Unit, modifier: Modifier = Modifier)` | `@Composable fun Fader(label: String, value: Int, min: Int, max: Int, lampColor: Color, onValueChange: (Int) -> Unit, onDetent: (Int) -> Unit, modifier: Modifier, lifted: Boolean?, reduceEffects: Boolean, enabled: Boolean)` | `app.enk.design.components.Fader.kt` | Matches parameter order starting with `label: String`; provides overload for `Rgb`; test override for `lifted`; accessibility semantics included. |
| **Task 19** Fader math | `object FaderMath { fun detent(...); fun valueFor(...); fun displayPercent(...); fun fractionFor(...); fun knobLeftDp(...) }` | `app.enk.design.components.Fader.kt` | Pure object implementing 20-step quantization and value mappings. |
| **Task 20** `@Composable fun WriterScreen(viewModel: WriterViewModel, settings: Settings, player: SoundPlayer, haptics: Haptics, onOpenBox: () -> Unit)` | `@Composable fun WriterScreen(viewModel: WriterViewModel, settings: Settings, player: SoundPlayer, haptics: Haptics, onOpenBox: () -> Unit, modifier: Modifier, onSettingsChange: ((Settings) -> Settings) -> Unit)` | `app.enk.writer.WriterScreen.kt` | Added optional `modifier` and optional `onSettingsChange` lambda defaulting to `viewModel` settings store update. |
| **Task 20** `class ImeBridge { fun onTextChanged(old: String, new: TextFieldValue, composingNow: Boolean): List<WriterEvent> }` | `class ImeBridge { fun onTextChanged(old: String, new: TextFieldValue, composingNow: Boolean): List<WriterEvent> }` | `app.enk.writer.ImeBridge.kt` | Exact match. |
| **Task 20** `interface Haptics { fun pulse(ms: Int) }` | `interface Haptics { fun pulse(ms: Int) }`, `class AndroidHaptics(context: Context) : Haptics` | `app.enk.writer.Haptics.kt` | Exact match. Includes Android vibrator implementation with safe exception handling. |

---

## 3. Deviations from the Plan

1. **Flat Package Structure**: All writer code resides in package `app.enk.writer` under `:app` (e.g. `app.enk.writer.WriterScreen`), matching the Batch 5 architecture instead of the legacy plan path `app.enk.writer.ui.*`.
2. **Robolectric instead of androidTest**: UI integration tests (`WriterScreenTest`, `MarkerGestureTest`, `NightThemeTest`) execute on the JVM via Robolectric under `:app:testDebugUnitTest`, enabling fast, deterministic execution on headless developer machines.
3. **Grab Offset in Gestures**: `MarkerGestures.kt` tracks `grabOffsetDp = pointerXDp - initialCarriageXDp` upon touch-down so the carriage does not jump to pointer center on the initial touch.
4. **`nightProgress` State**: Introduced `rememberNightProgress(lampOn, reduceEffects)` animating a continuous 0f..1f float value to crossfade colors and alpha smoothly between day and night theme tokens.
5. **`LocalMonoFont`**: Font references in Compose UI use `LocalMonoFont.current` (defaulting to `CourierPrime`), allowing Paparazzi tests to provide file-based font instances on the JVM without failing font resource resolution.
6. **Room Dependency in `:app`**: Added `implementation(libs.androidx.room.runtime)` to `:app` so `AppContainer.DefaultAppContainer` can instantiate `Room.databaseBuilder` at runtime, documented in `docs/dependencies.md`.

---

## 4. Key Architectural Decisions (One Line Each with Proving Test)

1. **Gestures**: Scrubbing maps pointer X to caret index using half-up rounding, clamped to page length (`MarkerGestureTest.dragCarriageScrubsCaret`).
2. **Gestures**: Long press is detected if pointer movement stays within 6 dp slop for 480 ms (`MarkerGestureTest.stationaryPointerFiresLongPressAt480Ms`).
3. **Gestures**: Releasing pointer cancels any pending long press and triggers snap callback (`MarkerGestureTest.releaseCancelsLongPress`).
4. **Lamp Rendering**: Beam polygon top width expands from 20 dp at beam=30 to 80 dp at beam=100 (`LampGeometryTest.beamWidthExpandsWithSlider`).
5. **Lamp Rendering**: Text mask center Y is placed 90 dp above halo center in paper coordinates (`LampGeometryTest.textMaskCenterDpIsAboveHaloCenter`).
6. **Control Panel**: Sliders quantize into exactly 20 discrete detents (5% increments) with half-up rounding (`FaderMathTest.detentSnapsTo20Steps`).
7. **Control Panel**: Staggered entrance animates panel (560 ms springPanel) and rows at 120, 200, 280 ms delays (`PanelGoldenTest.panelMidEnterAt280ms`).
8. **Control Panel**: Reduced motion applies a 100 ms linear fade without translate or scale (`PanelGoldenTest.panelReduceEffects`).
9. **IME**: Composing updates emit full composing string with `composing = true`, commit emits `composing = false` (`ImeBridgeTest.swipeCommitsWholeWordOnce`).
10. **IME**: Code-point-aware diffing ensures multi-byte emojis and surrogate pairs are never split (`ImeBridgeTest.emojiInsertIsPreserved`, `ImeBridgeTest.emojiDelete`).
11. **IME**: Pasting 200,000 characters executes in linear time producing a single insert event (`ImeBridgeTest.pasteOf200kCharacters`).
12. **Policies**: Caret is solid (`typing = true`) while dragging and for 600 ms after the last key stroke (`TypingPolicyTest.activeWithin600Ms`).
13. **Policies**: Paper tap maps to nearest line and column clamped to line length without splitting surrogate pairs (`TapCaretPolicyTest.tapPastEndOfShortLineClampsToLineLength`).
14. **Policies**: Hint is dismissed once user has completed both scrub and long-press gestures (`HintPolicyTest.dismissedWhenBothActionsPerformed`).
15. **Audio & Haptics**: Margin bell chimes once per line when caret reaches `cols - 4`, not muted by `settings.muted` (`BellPolicyTest.firesOnlyOncePerLine`).
16. **Lifecycle**: Autosaver flushes on `Lifecycle.Event.ON_STOP` and `MainViewModel.onCleared` (`WriterViewModelTest.flushSavesToRepository`).
17. **DataStore**: Preferences file corruption triggers `ReplaceFileCorruptionHandler`, falling back to clean defaults (`CorruptionHandlerTest.corruptPreferencesFileRecoversToDefaults`).

---

## 5. Resources, Scopes, and Lifecycle Management

- **`Autosaver`**: IO coroutine scope owned by `WriterViewModel`; flushed synchronously on `ON_STOP` via `LifecycleEventObserver` and in `onCleared()`. Scope is cancelled in `onCleared()`.
- **`AppContainer` / `DefaultAppContainer`**: Instantiated and owned by `MainViewModel` (survives activity rotation). Database is closed via `db.close()` and audio is released via `soundPlayer.release()` in `MainViewModel.onCleared()`.
- **`WriterScreen`**: Registers `LifecycleEventObserver` inside a `DisposableEffect(lifecycleOwner)` that unregisters cleanly in `onDispose`.
- **`SettingsStore`**: Flow collection in `WriterViewModel` is managed by an explicit `Job` that is cancelled in `onCleared()`.
- **`SoundPoolPlayer`**: `SoundPool` instance is protected by an `AtomicBoolean` release flag; all audio playback calls are guarded by `try-catch` so audio errors never crash the UI or typing loop.
- **`AndroidHaptics`**: Vibrator service calls are guarded by `try-catch` and annotated with `@SuppressLint("MissingPermission")` so missing permissions or hardware lack never crashes the app.

---

## 6. Tests Broken on Purpose During TDD

1. **`MarkerGestureTest.stationaryPointerFiresLongPressAt480Ms`**: Changed timer threshold to 600 ms; observed failure as callback was not invoked at 480 ms.
2. **`LampGeometryTest.textMaskCenterDpIsAboveHaloCenter`**: Modified offset calculation to subtract paperTop twice; observed calculation failure asserting center Y.
3. **`FaderMathTest.defaultsSurviveRoundTrip`**: Inverted rounding to `floor`; observed failure on beam default (55 rounded down to 54).
4. **`ImeBridgeTest.initialSelectionChangeToOldLengthIsMoveCaret`**: Set fallback caret to `old.length`; observed failure with `expected:<[MoveCaret(5)]> but was:<[]>`.
5. **`HintPolicyTest.dismissedWhenBothActionsPerformed`**: Temporarily changed `isDismissed` to check `hasScrubbed || hasLongPressed`; observed failure asserting non-dismissal with only one action.

---

## 7. Golden Tests Review (11 Snapshots Recorded and Verified)

All snapshots were recorded and verified on Windows with Paparazzi 1.3.5 against custom 400x820 dp phone viewport dimensions:

### A. Lamp Rendering (`LampGoldenTest`, 3 snapshots)
1. **`beamAtDefaultSettings`**: Conic light beam originating from the carriage slot at railY, blending down to the paper with default 55% beam width and 80% intensity.
2. **`beamAtNarrowSpread`**: Sharp, focused light cone at minimum beam spread (30%).
3. **`beamAtWideSpread`**: Broad ambient light flood across the full page width at maximum beam spread (100%).

### B. Control Panel & Faders (`PanelGoldenTest`, 8 snapshots)
4. **`panelAtRest`**: Hardware panel box with 16 dp radius, engraved labels (WARMTH, INTENSITY, BEAM), default fader slider positions (25%, 80%, 55%), and 36 dp row slots.
5. **`panelFadersAt0Percent`**: All 3 fader knobs at far left (0% position), displaying values 0, 0, 0.
6. **`panelFadersAt25Percent`**: All 3 fader knobs at 25% detent positions, displaying values 25, 25, 26.
7. **`panelFadersAt100Percent`**: All 3 fader knobs at far right (100% position), displaying values 100, 100, 100 with full active gradient tracks.
8. **`panelOneKnobLifted`**: Warmth fader knob visually elevated with -2 dp translateY, 1.07 scale, and lifted drop shadow.
9. **`panelMidEnterAt280ms`**: Staggered entrance animation frozen at 280 ms: panel translateY partially moved, Warmth fader visible, Intensity mid-enter, Beam hidden at delay boundary.
10. **`panelReduceEffects`**: Rendered without active gradient glow and alpha blur layers for reduced motion mode.
11. **`faderTouchHeightIs48Dp`**: Standalone fader measured and rendered confirming exact 48 dp touch container height.

*Note on Golden Limitations:* Paparazzi runs on JVM LayoutLib, which cannot execute native GPU fragment shaders or hardware `RenderEffect` blurs; blur effects are verified by unit test math and visual fallbacks.

---

## 8. What is Not Verified

- **Physical Device Execution**: No automated tests executed on a physical Android device. A comprehensive manual QA verification checklist has been authored at [`docs/qa/writer-device-checklist.md`](file:///c:/Users/Khaled/Documents/Personal%20Projects/Enk/docs/qa/writer-device-checklist.md) covering keyboard typing, swipe input, haptic pulse sensations, audio timbre loudness, and dark-room lamp perception.
- **Hardware Vibrator**: Real vibration motor rise-time and tactile feedback were not measured on physical hardware.
- **GPU Blur Shaders**: Real-time RenderEffect blur on Android 12+ requires physical hardware GPU.

---

## 9. Test Suite Summary (Summed from Gradle XML Reports)

**Total Test Suite: 541 tests, 0 failures, 0 skipped.**

| Module | Test Suite Class | Tests | Failures | Status |
|---|---|---|---|---|
| `:app` | `AppSmokeTest` | 1 | 0 | PASSED |
| `:app` | `ManifestTest` | 5 | 0 | PASSED |
| `:app` | `ProvenanceTest` | 6 | 0 | PASSED |
| `:app` | `LauncherIconTest` | 1 | 0 | PASSED |
| `:app` | `CorruptionHandlerTest` | 1 | 0 | PASSED |
| `:app` | `SettingsStoreTest` | 16 | 0 | PASSED |
| `:app` | `AutosaverTest` | 8 | 0 | PASSED |
| `:app` | `CarriageGoldenTest` | 7 | 0 | PASSED |
| `:app` | `HintPolicyTest` | 6 | 0 | PASSED |
| `:app` | `ImeBridgeTest` | 17 | 0 | PASSED |
| `:app` | `MarkerGestureTest` | 18 | 0 | PASSED |
| `:app` | `MotionPolicyTest` | 3 | 0 | PASSED |
| `:app` | `NightThemeTest` | 6 | 0 | PASSED |
| `:app` | `PaperGoldenTest` | 9 | 0 | PASSED |
| `:app` | `PaperTextTest` | 12 | 0 | PASSED |
| `:app` | `TapCaretPolicyTest` | 7 | 0 | PASSED |
| `:app` | `TopBarFormatterTest` | 3 | 0 | PASSED |
| `:app` | `TypeSlugTest` | 9 | 0 | PASSED |
| `:app` | `TypingPolicyTest` | 4 | 0 | PASSED |
| `:app` | `WriterGeometryTest` | 11 | 0 | PASSED |
| `:app` | `WriterReducerTest` | 26 | 0 | PASSED |
| `:app` | `WriterScreenTest` | 7 | 0 | PASSED |
| `:app` | `WriterViewModelTest` | 11 | 0 | PASSED |
| `:audio` | `AudioSmokeTest` | 1 | 0 | PASSED |
| `:audio` | `BellPolicyTest` | 10 | 0 | PASSED |
| `:audio` | `SoundManifestTest` | 13 | 0 | PASSED |
| `:audio` | `SoundPickerTest` | 7 | 0 | PASSED |
| `:audio` | `SoundPlayerTest` | 5 | 0 | PASSED |
| `:audio` | `TickSynthTest` | 6 | 0 | PASSED |
| `:data` | `DaoTest` | 10 | 0 | PASSED |
| `:data` | `DataSmokeTest` | 1 | 0 | PASSED |
| `:data` | `ExportImportTest` | 27 | 0 | PASSED |
| `:data` | `NoteRepositoryTest` | 21 | 0 | PASSED |
| `:data` | `SearchTest` | 18 | 0 | PASSED |
| `:data` | `StruckCodecTest` | 8 | 0 | PASSED |
| `:design` | `DesignSmokeTest` | 1 | 0 | PASSED |
| `:design` | `FaderMathTest` | 8 | 0 | PASSED |
| `:design` | `GrainTest` | 8 | 0 | PASSED |
| `:design` | `JitterTest` | 5 | 0 | PASSED |
| `:design` | `LampColorTest` | 7 | 0 | PASSED |
| `:design` | `LampGeometryTest` | 8 | 0 | PASSED |
| `:design` | `MaterialsTest` | 13 | 0 | PASSED |
| `:design` | `TokensTest` | 4 | 0 | PASSED |
| `:design` | `GlassGoldenTest` | 6 | 0 | PASSED |
| `:design` | `LampGoldenTest` | 3 | 0 | PASSED |
| `:design` | `LogoGoldenTest` | 4 | 0 | PASSED |
| `:design` | `MaterialsGoldenTest` | 1 | 0 | PASSED |
| `:design` | `PanelGoldenTest` | 8 | 0 | PASSED |
| `:design` | `LogoSpecTest` | 11 | 0 | PASSED |
| `:design` | `LensControllerTest` | 13 | 0 | PASSED |
| `:design` | `PhysicsTest` | 12 | 0 | PASSED |
| `:design` | `SpringSimulationTest` | 6 | 0 | PASSED |
| `:design` | `GlassBackendSelectionTest` | 10 | 0 | PASSED |
| `:design` | `ScrollProgressTest` | 4 | 0 | PASSED |
| `:design` | `ContrastTest` | 10 | 0 | PASSED |
| `:design` | `DynamicShellColorsTest` | 8 | 0 | PASSED |
| `:design` | `EnkShellThemeTest` | 2 | 0 | PASSED |
| `:design` | `ShellPalettesTest` | 16 | 0 | PASSED |
| `:text` | `GridLayoutTest` | 19 | 0 | PASSED |
| `:text` | `PaginatorTest` | 21 | 0 | PASSED |
| `:text` | `StruckPageTest` | 11 | 0 | PASSED |
| `:text` | `TextSmokeTest` | 1 | 0 | PASSED |
| **Total** | | **541** | **0** | **PASSED** |

---

## 10. Network Compliance Check

Executed via `& "C:\Program Files\Git\bin\bash.exe" scripts/check-no-network.sh`:
```
Checking manifest: app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml
SUCCESS: No network permissions declared in merged manifest.
```

---

## 11. Full Clean Gate Execution & Real Tail Output

Executed command:
`$env:JAVA_HOME = "D:\jdks\eclipse_adoptium-21-amd64-windows.2"; .\gradlew.bat clean build test :design:verifyPaparazziDebug :app:verifyPaparazziDebug :app:processDebugManifest :app:checkDebugAarMetadata :app:checkReleaseAarMetadata`

Real command tail:
```
> Task :data:test
> Task :data:check
> Task :data:build

> Task :design:lintReportDebug
Wrote HTML report to file:///C:/Users/Khaled/Documents/Personal%20Projects/Enk/design/build/reports/lint-results-debug.html

> Task :app:transformReleaseUnitTestClassesWithAsm

> Task :design:testReleaseUnitTest
See the Paparazzi report at: file:///C:/Users/Khaled/Documents/Personal%20Projects/Enk/design/build/reports/paparazzi/release/index.html

> Task :app:testReleaseUnitTest
See the Paparazzi report at: file:///C:/Users/Khaled/Documents/Personal%20Projects/Enk/app/build/reports/paparazzi/release/index.html

> Task :app:test
> Task :app:check
> Task :design:lintDebug
> Task :design:lint
> Task :design:test
> Task :design:check
> Task :design:build
> Task :design:verifyPaparazziDebug
> Task :app:verifyPaparazziDebug
> Task :app:createDebugApkListingFileRedirect
> Task :app:assembleDebug
> Task :app:createReleaseApkListingFileRedirect
> Task :app:assembleRelease
> Task :app:assemble
> Task :app:build

[Incubating] Problems report is available at: file:///C:/Users/Khaled/Documents/Personal%20Projects/Enk/build/reports/problems/problems-report.html

BUILD SUCCESSFUL in 5m 11s
414 actionable tasks: 390 executed, 24 up-to-date
```
