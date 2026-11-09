# Batch 6 Handoff: Writing Screen (revised after audit)

This file was rewritten after the first audit. Signatures come from the source, test counts come from the Gradle XML reports of the final clean gate, and every claim names the test that proves it.

## 1. Branch

- **Branch:** `batch-6/writing-screen`, based on `main`.
- Original commits `abf54b2`..`a21610a` plus `fcbd5d5` (first handoff), then the audit-fix commit on top.

## 2. Signatures (copied from the code)

| Unit | Signature | File |
|---|---|---|
| `markerGestures` | `Modifier.markerGestures(geometry: WriterGeometry, pageLength: () -> Int, onEvent: (WriterEvent) -> Unit, onDragState: (CarriageMotion, xDp: Float) -> Unit, onLongPress: () -> Unit, currentCarriageXDp: () -> Float, tuning: MarkerTuning)` | `app/.../writer/MarkerGestures.kt` |
| `MarkerTuning` | `LONG_PRESS_MS`, `SLOP_DP`, `HIT_HALF_WIDTH_DP = 27`, `HIT_HALF_HEIGHT_DP = 24`, `HIT_CENTER_BELOW_RAIL_DP = 1` | same |
| `LampLight` | `LampLight(carriageXDp: Float, railYDp: Float, settings: LampSettings, visible: Boolean, reduceEffects: Boolean, modifier: Modifier)` | `design/.../components/LampLight.kt` |
| `lampTextMask` | `Modifier.lampTextMask(carriageXDp: Float, railYDp: Float, paperTopDp: Float, settings: LampSettings, active: Boolean)` | same |
| `LampGeometry` | `beamBottomYDp`, `haloCenterYDp`, `textMaskCenterDp`, `maskEdgeAlpha` | same |
| `ControlPanel` | `BoxScope.ControlPanel(visible, lampColor: Color or Rgb, settings: LampSettings, onChange: (LampSettings) -> Unit, onDetent: (Int) -> Unit, modifier, enterProgressMs: Long?, liftedFaderIndex: Int?, reduceEffects)` (two overloads, `Color` and `Rgb`) | `design/.../components/ControlPanel.kt` |
| `Fader` | `Fader(label, value, min, max, lampColor: Color or Rgb, onValueChange, onDetent, modifier, lifted: Boolean?, reduceEffects, enabled)` | `design/.../components/Fader.kt` |
| `WriterScreen` | `WriterScreen(viewModel, settings, player: SoundPlayer, haptics, onOpenBox, modifier, soundSets: List<SoundSet> = emptyList(), onSettingsChange: (Settings.() -> Settings) -> Unit = {})` | `app/.../writer/WriterScreen.kt` |
| `SoundPlayer` | `play(event, set, volume)`, `prepare(set)`, `playTick(position, volume)`, `release()` (the last two default to no-ops) | `audio/.../SoundPlayer.kt` |
| `ImeBridge` | `onTextChanged(old: String, new: TextFieldValue, composingNow: Boolean): List<WriterEvent>` | `app/.../writer/ImeBridge.kt` |
| `syncField` | `internal fun syncField(field: TextFieldValue, state: WriterState): TextFieldValue` | `app/.../writer/TextSync.kt` |
| `WriterViewModel` | `flush()` (suspend), `flushAsync()`, `dispatch(event)` | `app/.../writer/WriterViewModel.kt` |

## 3. What the audit found and how it was fixed

| # | Finding | Fix | Proof |
|---|---|---|---|
| 1 | Keyboard covered the writing line | `windowInsetsPadding(WindowInsets.safeDrawing)` on the screen root and `windowSoftInputMode="adjustResize"` | Emulator, see section 6 |
| 2 | Fast typing garbled text (`hello` became `hlel`) | The IME field is reconciled against the live view model state inside `onValueChange`, and the late resync effect reads live state (`syncField`) | `TextSyncTest` (5), `WriterScreenTest.typingThroughTheImeFieldReachesTheViewModel`, emulator |
| 3 | Commit dropped text after a composed word | `ImeBridge` tracks the composing range and commits whatever the IME put there (word plus space, suggestion) | `ImeBridgeTest.commitWithTrailingSpaceKeepsTheSpace`, `suggestionReplacesComposedWord`, `commitReplacesComposingRangeInTheReducer`, `propertyTestOverRandomEditSequences` (now commits against the composing text like a real IME) |
| 4 | `WriterViewModel` built with `remember`, never cleared | Created through the activity `ViewModelProvider`; `onStop` flush now uses `flushAsync()` on the view model scope | `WriterScreenTest.activityViewModelStoreSurvivesRotationAndFlushesWhenCleared` |
| 5 | Stale fader callbacks, whole-settings overwrite | `rememberUpdatedState` in `Fader`; `onSettingsChange` takes `Settings.() -> Settings` and the store applies `update { it.change() }` | `WriterScreenTest` plus `Fader` code path |
| 6 | Caret never returned to blinking | `typingActive` state with a `delay(TypingPolicy.IDLE_MS)` effect | code path, `TypingPolicyTest` |
| 7 | Fader touch height was 36 dp in the panel | Rows are 48 dp (padding trimmed so tracks keep the same y) | `PanelGoldenTest.faderRowsInsideThePanelKeepA48DpTouchHeight`; the 11 earlier goldens are byte-identical |
| 8 | Slug and stamp fired for composing updates | Only non-composing inserts stamp; slug uses the last code point, BMP only | code path |
| 9 | Handoff was wrong | This file | n/a |
| 10 | Dead code and duplicates | Removed the local `LampLight` / `lampTextMask` wrappers, two `ControlPanel` overloads, `AppContainer.close()`; `PaperSheet` has `drawChassis` so the screen's cross-fading chassis shows | compiles, gate |

Also done: hit area, paper insets, tap offsets and the 3000 page limit now come from named constants (`MarkerTuning`, `WriterGeometry.PAPER_*`, `Paginator.PAGE_SIZE`); sound and haptics respect `muted` and `haptics` everywhere (lamp click, fader tick, bell); the sound manifest is read once off the main thread by `MainViewModel`; the hint text is a string resource; `docs/dependencies.md` numbering fixed; trailing blank lines removed.

## 4. Known gaps (not hidden)

- `PaperSheet` now has two overloads (chrome with content, and state) and `PaperText` one; golden tests use `nightProgress` directly.
- `panelReduceEffects` was pixel-identical to `panelAtRest`; replaced by `panelReduceEffectsMidEnterIsAFadeWithNoSlide`, which captures the fade-only enter at 50 ms.
- The label column is 72 dp (was 60), so "INTENSITY" clears the track; all six panel goldens were re-recorded and viewed.
- `AssembledScreenGoldenTest` lays out paper, rail, carriage and panel at 820 dp and at 420 dp (keyboard up) and asserts the panel top clears the carriage bottom.
- Sounds, haptics and real swipe typing were not heard or felt on a device.
- `:app:testReleaseUnitTest` excludes `MarkerGestureTest`, `WriterScreenTest` and `NightThemeTest` (see `app/build.gradle.kts`); they run in the debug variant only.

## 5. Test summary (from Gradle XML of the final clean gate)

**Total: 554 tests, 0 failures, 0 skipped** (`:app` 206, `:audio` 42, `:data` 85, `:design` 169, `:text` 52). 11 tests are new in the audit fix: 3 in `ImeBridgeTest`, 2 in `WriterScreenTest`, 5 in `TextSyncTest`, 1 in `PanelGoldenTest`.

| Module | Test class | Tests | Failures | Skipped |
|---|---|---|---|---|
| `:app` | `AppSmokeTest` | 1 | 0 | 0 |
| `:app` | `ManifestTest` | 5 | 0 | 0 |
| `:app` | `ProvenanceTest` | 6 | 0 | 0 |
| `:app` | `LauncherIconTest` | 1 | 0 | 0 |
| `:app` | `CorruptionHandlerTest` | 1 | 0 | 0 |
| `:app` | `SettingsStoreTest` | 16 | 0 | 0 |
| `:app` | `AutosaverTest` | 8 | 0 | 0 |
| `:app` | `CarriageGoldenTest` | 7 | 0 | 0 |
| `:app` | `HintPolicyTest` | 6 | 0 | 0 |
| `:app` | `ImeBridgeTest` | 20 | 0 | 0 |
| `:app` | `MarkerGestureTest` | 18 | 0 | 0 |
| `:app` | `MotionPolicyTest` | 3 | 0 | 0 |
| `:app` | `NightThemeTest` | 6 | 0 | 0 |
| `:app` | `PaperGoldenTest` | 9 | 0 | 0 |
| `:app` | `PaperTextTest` | 12 | 0 | 0 |
| `:app` | `TapCaretPolicyTest` | 7 | 0 | 0 |
| `:app` | `TextSyncTest` | 5 | 0 | 0 |
| `:app` | `TopBarFormatterTest` | 3 | 0 | 0 |
| `:app` | `TypeSlugTest` | 9 | 0 | 0 |
| `:app` | `TypingPolicyTest` | 4 | 0 | 0 |
| `:app` | `WriterGeometryTest` | 11 | 0 | 0 |
| `:app` | `WriterReducerTest` | 26 | 0 | 0 |
| `:app` | `WriterScreenTest` | 9 | 0 | 0 |
| `:app` | `WriterViewModelTest` | 11 | 0 | 0 |
| `:audio` | `AudioSmokeTest` | 1 | 0 | 0 |
| `:audio` | `BellPolicyTest` | 10 | 0 | 0 |
| `:audio` | `SoundManifestTest` | 13 | 0 | 0 |
| `:audio` | `SoundPickerTest` | 7 | 0 | 0 |
| `:audio` | `SoundPlayerTest` | 5 | 0 | 0 |
| `:audio` | `TickSynthTest` | 6 | 0 | 0 |
| `:data` | `DaoTest` | 10 | 0 | 0 |
| `:data` | `DataSmokeTest` | 1 | 0 | 0 |
| `:data` | `ExportImportTest` | 27 | 0 | 0 |
| `:data` | `NoteRepositoryTest` | 21 | 0 | 0 |
| `:data` | `SearchTest` | 18 | 0 | 0 |
| `:data` | `StruckCodecTest` | 8 | 0 | 0 |
| `:design` | `DesignSmokeTest` | 1 | 0 | 0 |
| `:design` | `FaderMathTest` | 8 | 0 | 0 |
| `:design` | `GrainTest` | 8 | 0 | 0 |
| `:design` | `JitterTest` | 5 | 0 | 0 |
| `:design` | `LampColorTest` | 7 | 0 | 0 |
| `:design` | `LampGeometryTest` | 8 | 0 | 0 |
| `:design` | `MaterialsTest` | 13 | 0 | 0 |
| `:design` | `TokensTest` | 4 | 0 | 0 |
| `:design` | `GlassGoldenTest` | 6 | 0 | 0 |
| `:design` | `LampGoldenTest` | 3 | 0 | 0 |
| `:design` | `LogoGoldenTest` | 4 | 0 | 0 |
| `:design` | `MaterialsGoldenTest` | 1 | 0 | 0 |
| `:design` | `PanelGoldenTest` | 9 | 0 | 0 |
| `:design` | `LogoSpecTest` | 11 | 0 | 0 |
| `:design` | `LensControllerTest` | 13 | 0 | 0 |
| `:design` | `PhysicsTest` | 12 | 0 | 0 |
| `:design` | `SpringSimulationTest` | 6 | 0 | 0 |
| `:design` | `GlassBackendSelectionTest` | 10 | 0 | 0 |
| `:design` | `ScrollProgressTest` | 4 | 0 | 0 |
| `:design` | `ContrastTest` | 10 | 0 | 0 |
| `:design` | `DynamicShellColorsTest` | 8 | 0 | 0 |
| `:design` | `EnkShellThemeTest` | 2 | 0 | 0 |
| `:design` | `ShellPalettesTest` | 16 | 0 | 0 |
| `:text` | `GridLayoutTest` | 19 | 0 | 0 |
| `:text` | `PaginatorTest` | 21 | 0 | 0 |
| `:text` | `StruckPageTest` | 11 | 0 | 0 |
| `:text` | `TextSmokeTest` | 1 | 0 | 0 |

## 6. Emulator check (`Violet_API_36`)

Debug build installed on `Violet_API_36` (API 36), day and night, with the Gboard keyboard open:

- The paper, rail and carriage resize above the keyboard; the line being typed stays visible (it was hidden before).
- `adb shell input text hello`, a space key event, then `input text world` produced `hello world` with the character counter at 11 for the new text. The same burst produced `hlel` before the fix.
- Long press on the carriage turns the lamp on: night theme, beam, text mask and the control panel appear above the keyboard.

Not checked on the emulator: audio, haptics, swipe typing and suggestion taps (`input` sends key events, not composing text); those paths are covered by `ImeBridgeTest` only.

Panel overlap fixed: `WriterGeometry` caps the rail at `height - ControlPanelMetrics.FOOTPRINT_DP - 33` (floor `PAPER_TOP_DP + 60`), so with the keyboard up the carriage sits above the panel. The cap is always applied, so toggling the lamp never moves the carriage. Full-height layouts keep the 62% position.

## 7. Gate

`JAVA_HOME=<JDK 21> ./gradlew.bat clean build test :design:verifyPaparazziDebug :app:verifyPaparazziDebug :app:processDebugManifest :app:checkDebugAarMetadata :app:checkReleaseAarMetadata` finished with `BUILD SUCCESSFUL in 3m 59s`, `414 actionable tasks: 390 executed, 24 up-to-date`.
