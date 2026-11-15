# Batch 14 Handoff: Liquid Glass and Motion (G1 to G6)

Branch `batch-14/glass`, base `main` (`70a21c7`). Liquid glass built with Backdrop 2.0.1 (blur, refraction lens with colour fringe, rim highlight that follows the finger, inner shadow), an effects-level setting (Auto, Full, Balanced, Light) replacing Reduce effects, every control and floating layer rebuilt on the glass kit, a button that morphs into its pop-up as one piece of glass, a 3-state top island with glass lamp sliders (the legacy steel lamp panel removed), a continuous rail transition between Box and Write, and spring motion across all screens.

---

## 1. Deliverables Summary (G1 to G6)

- **G1 (Effects Levels, Setting & Frame Governor):**
  - `EffectsLevel` enum: `Auto`, `Full`, `Balanced`, `Light`.
  - Settings migration from `reduceEffects: Boolean` to `effects: EffectsLevel`.
  - Frame governor (`FrameGovernor`) monitoring rolling frame times over 30 frames; drops down when >= 3 frame drops occur, steps back up after 120 smooth frames.
  - Pure resolver `resolveEffects(deviceProfile, batterySaver, userSetting, frameDropLevel, highContrast)`: high contrast forces `Light` (solid backend), battery saver clamps to `Light`/`Balanced`, slow frame times step down gracefully.
- **G2 (Glass Kit Foundation with Backdrop 2.0.1):**
  - Core modifier `Modifier.liquidGlass(...)` utilizing Backdrop 2.0.1 `BackdropFilter.blur`, `lens`, chromatic aberration, rim highlight following touch interaction, and inner shadow.
  - `GlassSpecs`: constants for nav bar, dialogs, cards, popups, and buttons with exact blur radii, lens curvature, and tints.
  - Coercion logic `coerceLens`: enforces minimum lens height >= 48 dp to avoid optical clipping and distortion artifacts.
  - Fallback solid rendering for `EffectsLevel.Light` and accessibility high-contrast environments.
- **G3 (Controls on the Glass Kit):**
  - Rebuilt controls in `:design`: `GlassButton`, `GlassSegmentedControl`, `GlassSlider`, `GlassSwitch`.
  - Touch targets guaranteed >= 48 dp; spring animations (`EnkMotion.springPress`, `EnkMotion.springSnappy`); haptics on toggle and detents.
- **G4 (Floating Layers and the Morph):**
  - Floating components: `GlassDialog`, `GlassBottomSheet`, `GlassDropdown`.
  - `GlassMorph`: seamless geometric and alpha morphing between trigger buttons and expanded glass dialogs/popups (`morphFrame` rect interpolation with stiffness 380, damping 0.82).
- **G5 (Top Island, Lamp View, Rail Transition, Steel Panel Removed):**
  - `TopIsland`: 3-state header in `WriterScreen` (`Rest`, `Actions`, `LampView`). Auto-collapses on typing or after 4.5 s idle timer.
  - `LampPopup`: glass popup containing brightness and warmth sliders; folds smoothly back to island.
  - `RailTransition`: physical transition linking Box screen and Writer screen with 3-beat haptic cadence and mechanical carriage ding at completion.
  - Legacy steel lamp panel completely excised.
- **G6 (Motion on Every Screen & Cleanup):**
  - `pressFeedback` modifier applied across all clickable surfaces (cards, pills, buttons, rows, theme tiles) with spring-driven scale and haptics.
  - Screen transitions: horizontal slide/fade with direction and page spring for nav tabs; vertical slide for note opening; trailing slide for pushed screens (`TrashScreen`, `AboutScreen`).
  - Lazy list item animations (`animateItem(fadeInSpec, placementSpec, fadeOutSpec)`).
  - Complete deletion of obsolete `Modifier.glass(...)` and `GlassLevel`.

---

## 2. Declarations (from the code)

| File | Package | Declaration |
|---|---|---|
| `EffectsLevel.kt` | `app.enk.settings` | `enum class EffectsLevel { Auto, Full, Balanced, Light }` |
| `EffectsLevel.kt` | `app.enk.settings` | `fun resolveEffects(deviceProfile: DeviceProfile, batterySaver: Boolean, userSetting: EffectsLevel, frameDropLevel: EffectsLevel = EffectsLevel.Full, highContrast: Boolean = false): EffectsLevel` |
| `FrameGovernor.kt` | `app.enk.settings` | `class FrameGovernor(windowSize: Int = 30)` |
| `GlassSpecs.kt` | `app.enk.design.glass` | `object GlassSpecs` |
| `GlassSpecs.kt` | `app.enk.design.glass` | `fun coerceLens(heightDp: Float, minLensHeightDp: Float = 48f): Float` |
| `LiquidGlass.kt` | `app.enk.design.glass` | `fun Modifier.liquidGlass(spec: GlassSpec, shape: Shape, highlightPosition: Offset? = null, fallbackSolid: Boolean = false): Modifier` |
| `GlassMaterials.kt` | `app.enk.design.glass` | `object GlassMaterials` |
| `HighlightStyle.kt` | `app.enk.design.glass` | `data class HighlightStyle(val angleDegrees: Float = 315f, val widthDp: Float = 1.5f, val color: Color)` |
| `GlassButton.kt` | `app.enk.design.glass` | `fun GlassButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit)` |
| `GlassSegmentedControl.kt` | `app.enk.design.glass` | `fun <T> GlassSegmentedControl(items: List<T>, selected: T, onSelect: (T) -> Unit, label: (T) -> String, modifier: Modifier = Modifier)` |
| `GlassSlider.kt` | `app.enk.design.glass` | `fun GlassSlider(value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier, valueRange: ClosedFloatingPointRange<Float> = 0f..1f, steps: Int = 0)` |
| `GlassSwitch.kt` | `app.enk.design.glass` | `fun GlassSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true)` |
| `GlassDialog.kt` | `app.enk.design.glass` | `fun GlassDialog(onDismissRequest: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit)` |
| `GlassBottomSheet.kt` | `app.enk.design.glass` | `fun GlassBottomSheet(onDismissRequest: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit)` |
| `GlassDropdown.kt` | `app.enk.design.glass` | `fun GlassDropdown(expanded: Boolean, onDismissRequest: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit)` |
| `GlassMorph.kt` | `app.enk.design.glass` | `fun morphFrame(source: Rect, target: Rect, progress: Float): Rect` |
| `TopIsland.kt` | `app.enk.writer` | `enum class TopIslandState { Rest, Actions, LampView }` |
| `TopIsland.kt` | `app.enk.writer` | `fun TopIsland(state: TopIslandState, onStateChange: (TopIslandState) -> Unit, onOpenBox: () -> Unit, lampSettings: LampSettings, onLampSettingsChange: (LampSettings) -> Unit, modifier: Modifier = Modifier)` |
| `LampPopup.kt` | `app.enk.writer` | `fun LampPopup(settings: LampSettings, onSettingsChange: (LampSettings) -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier)` |
| `RailTransition.kt` | `app.enk.shell` | `object RailTransition` |
| `PressFeedback.kt` | `app.enk.design.motion` | `enum class PressDepth { Button, Pill, Card, Row }` |
| `PressFeedback.kt` | `app.enk.design.motion` | `fun Modifier.pressFeedback(depth: PressDepth = PressDepth.Button, enabled: Boolean = true, onClick: (() -> Unit)? = null): Modifier` |
| `ShellMotion.kt` | `app.enk.shell` | `object ShellMotion` |
| `ThemeTransition.kt` | `app.enk.design.theme` | `fun Modifier.themeTransition(durationMs: Int = 200): Modifier` |

---

## 3. Commits (in strict required order)

```
cb4910c feat(glass): motion on every screen and cleanup (G6)
babc327 feat(glass): top island, lamp view, Rail transition, steel panel removed (G5)
248220e feat(glass): floating layers and the morph (G4)
1b8a792 feat(glass): controls on the kit (G3)
67b4f40 feat(glass): glass kit foundation (G2)
0828a30 feat(glass): effects levels, setting and frame governor (G1)
```

---

## 4. Test Counts (from the Gradle XMLs of the clean gate run)

| Module | Variant | Tests | Failures | Errors | Skipped | Status |
|---|---|---|---|---|---|---|
| `:text` | `test` | 52 | 0 | 0 | 0 | PASSED |
| `:data` | `testDebugUnitTest` | 88 | 0 | 0 | 0 | PASSED |
| `:data` | `testReleaseUnitTest` | 88 | 0 | 0 | 0 | PASSED |
| `:audio` | `testDebugUnitTest` | 42 | 0 | 0 | 0 | PASSED |
| `:audio` | `testReleaseUnitTest` | 42 | 0 | 0 | 0 | PASSED |
| `:design` | `testDebugUnitTest` | 202 | 0 | 0 | 0 | PASSED |
| `:design` | `testReleaseUnitTest` | 175 | 0 | 0 | 0 | PASSED |
| `:app` | `testDebugUnitTest` | 724 | 0 | 0 | 0 | PASSED |
| `:app` | `testReleaseUnitTest` | 405 | 0 | 0 | 0 | PASSED |
| **Total Debug** | | **1,108** | **0** | **0** | **0** | **ALL GREEN** |
| **Total Release** | | **762** | **0** | **0** | **0** | **ALL GREEN** |

---

## 5. Deliberately Broken Tests (Verification of Test Sensitivity)

Three tests were deliberately modified to fail during development, verified to fail with clear assertion messages, and then restored:

1. **`resolveEffects` ignoring high contrast:**
   - *Breakage:* In `resolveEffects(...)`, commented out the high-contrast check `if (highContrast) return EffectsLevel.Light`.
   - *Test:* `EffectsLevelTest > resolveEffects_highContrastForcesLight`
   - *Captured Failure:*
     ```
     java.lang.AssertionError: expected:<EffectsLevel.Light> but was:<EffectsLevel.Full>
     at org.junit.Assert.fail(Assert.java:89)
     at org.junit.Assert.failNotEquals(Assert.java:835)
     at org.junit.Assert.assertEquals(Assert.java:120)
     at app.enk.settings.EffectsLevelTest.resolveEffects_highContrastForcesLight(EffectsLevelTest.kt:42)
     ```

2. **`coerceLens` uncoerced height:**
   - *Breakage:* In `coerceLens(heightDp)`, returned `heightDp` directly without clamping to `minLensHeightDp` (48 dp).
   - *Test:* `GlassSpecsCoercionTest > coerceLensReturnsMinimum48Dp`
   - *Captured Failure:*
     ```
     java.lang.AssertionError: expected:<48.0> but was:<32.0>
     at org.junit.Assert.fail(Assert.java:89)
     at org.junit.Assert.assertEquals(Assert.java:685)
     at app.enk.design.glass.GlassSpecsCoercionTest.coerceLensReturnsMinimum48Dp(GlassSpecsCoercionTest.kt:29)
     ```

3. **`morphFrame` inverted interpolation:**
   - *Breakage:* In `morphFrame()`, inverted target top interpolation (`lerp(target.top, source.top, progress)`).
   - *Test:* `GlassMorphTest > morphFrameInterpolatesIntermediateRect`
   - *Captured Failure:*
     ```
     java.lang.AssertionError: expected:<80.0> but was:<120.0>
     at org.junit.Assert.fail(Assert.java:89)
     at org.junit.Assert.assertEquals(Assert.java:685)
     at app.enk.box.GlassMorphTest.morphFrameInterpolatesIntermediateRect(GlassMorphTest.kt:38)
     ```

---

## 6. Roborazzi Goldens (every image verified)

All golden images verified via `:design:verifyRoborazziDebug` and `:app:verifyRoborazziDebug`:

- **Design Module Goldens:**
  - `GlassGoldenTest_graphiteDark.png`: Graphite dark glass card with liquid refraction rim.
  - `GlassGoldenTest_graphiteLight.png`: Graphite light glass card with soft rim specular highlight.
  - `GlassGoldenTest_slateDark.png`: Slate dark glass panel with chromatic lens fringe.
  - `GlassGoldenTest_slateLight.png`: Slate light glass surface over textured background.
  - `GlassGoldenTest_scrollEdgeBars.png`: Top/bottom scroll edge glass blur bars at 24 dp.
  - `GlassGoldenTest_solidBackendReduceEffects.png`: Solid fallback veil rendered when Light/ReduceEffects is active.
  - `GlassKitGoldenTest_graphiteDark.png`: Control suite (button, switch, slider, segmented control) in graphite dark.
  - `GlassKitGoldenTest_graphiteLight.png`: Control suite in graphite light with crisp specular borders.
  - `LampGoldenTest_lampDefaultSettingsCol12.png`: Lamp beam at column 12 with default warmth and brightness.
  - `LampGoldenTest_lampMaxBeamCol12.png`: Lamp beam at column 12 with maximum intensity.
  - `LampGoldenTest_lampReduceEffectsCol12.png`: Lamp beam under Light/ReduceEffects without chromatic dispersion.
  - `LogoGoldenTest_foregroundMasks.png`: Enk typewriter logo masks.
  - `LogoGoldenTest_monochromeTints.png`: Enk logo monochrome tinting.
  - `LogoGoldenTest_symbolSizes.png`: Logo symbols at standard sizes.
  - `LogoGoldenTest_wordmarkVariants.png`: Logo wordmark lockups.
  - `MaterialsGoldenTest_materialsSnapshot.png`: Paper, chassis, and metal material swatches.

- **App Module Goldens:**
  - `TopIslandGoldenTest_writerIslandRestDay.png`: Top island in rest state (collapsed pill) in day theme.
  - `TopIslandGoldenTest_writerIslandRestNight.png`: Top island in rest state in night theme with warm glow.
  - `TopIslandGoldenTest_islandActions.png`: Expanded top island showing Box and Lamp action buttons.
  - `TopIslandGoldenTest_islandLampSliders0.png`: Lamp popup open with brightness and warmth at 0%.
  - `TopIslandGoldenTest_islandLampSliders50.png`: Lamp popup open with sliders at 50%.
  - `TopIslandGoldenTest_islandLampSliders100.png`: Lamp popup open with sliders at 100%.
  - `RailTransitionGoldenTest_transitionP0.png`: Rail transition at p = 0.0 (Box view state).
  - `RailTransitionGoldenTest_transitionP035.png`: Rail transition at p = 0.35 (intermediate scale and translation).
  - `RailTransitionGoldenTest_transitionP060.png`: Rail transition at p = 0.60 (rail carriage rising into position).
  - `RailTransitionGoldenTest_transitionP1.png`: Rail transition at p = 1.0 (Writer view state reached).
  - `BoxThemeGoldenTest_*`: Box note cards and background across all theme variants.
  - `KeyboardGoldenTest_*`: Enk keyboard layouts (letters, numbers, caps lock, dark/light).
  - `SearchGoldenTest_*`: Search results, empty state, and no results states.
  - `SetupGoldenTest_*`: Settings screen showing effects level selector and audio sliders.
  - `ShellGoldenTest_*`: Glass nav bar across themes and collapsed scaffold header.
  - `StartGoldenTest_*`: Splash animation timeline frames (0 ms to 900 ms in dark/light).
  - `Writer_CarriageGoldenTest_*`: Carriage at rest, dragging, and slug impact/settling.
  - `Writer_PaperGoldenTest_*`: Paper typography, strike dynamics, and type-on sweep.

---

## 7. Where Each Function is Called in the Running App

- `EffectsLevel` & `resolveEffects`: `MainViewModel` (initialized from DataStore, updated on setting change, resolved with system battery/contrast states); passed down through `CompositionLocalProvider(LocalEffectsLevel provides resolved)`.
- `FrameGovernor`: Attached to `MainActivity`'s frame callback loop / Compose `withFrameNanos` to dynamically step down effects level during rendering spikes.
- `Modifier.liquidGlass`: `GlassNavBar`, `ShellScaffold` compact bar, `GlassButton`, `GlassSlider`, `GlassSwitch`, `GlassSegmentedControl`, `GlassDialog`, `TopIsland`, `LampPopup`, `ScrollEdge`.
- `GlassButton`: `TopIsland` (Box and Lamp buttons), `TrashScreen` (Restore/Empty actions), `SettingsScreen` (Reset options), `AboutScreen`.
- `GlassSegmentedControl`: `AppearanceGroup` in `SettingsScreen` for `EffectsLevel` selection (`Auto`, `Full`, `Balanced`, `Light`) and Theme selection (`System`, `Light`, `Dark`).
- `GlassSlider`: `LampPopup` (Brightness, Warmth sliders) and `SettingsScreen` (Sound volume sliders).
- `GlassSwitch`: `SettingsScreen` (Haptics, Key clicks, Hint toggle switches).
- `TopIsland`: Anchored at top of `WriterScreen`; switches between `Rest`, `Actions`, and `LampView`.
- `RailTransition`: `EnkNavHost` when transitioning between `Route.Box` and `Route.Note`.
- `pressFeedback`: Applied to `NoteCard`, `ThemeTile`, `GroupedRow`, `GlassButton`, paper restore pills.

---

## 8. G6 Motion Table

| Item | File | Test Class / Test Name |
|---|---|---|
| Press Feedback on Buttons | `GlassButton.kt` | `PressEverywhereTest > glassButtonRegistersSingleClickAndPressFeedback` |
| Press Feedback on Note Cards | `NoteCard.kt` | `PressEverywhereTest > noteCardPressFeedbackClickCountIsOne` |
| Press Feedback on Rows | `GroupedRow.kt` | `PressEverywhereTest > groupedRowPressFeedbackClickCountIsOne` |
| Press Feedback on Theme Tiles | `ThemeTile.kt` | `PressEverywhereTest > themeTilePressFeedbackClickCountIsOne` |
| Press Feedback on Paper Pills | `TrashScreen.kt`, `BoxScreen.kt` | `PressEverywhereTest > paperPillPressFeedbackClickCountIsOne` |
| Press Feedback under Light/ReduceEffects | `PressFeedback.kt` | `PressEverywhereTest > reduceEffectsPressFeedbackFiresClicksInstantlyWithoutAnimation` |
| Nav Tab Horizontal Transitions | `EnkNavHost.kt` | `TabTransitionTest > switchingTabsShowsBothScreensDuringTransitionAndOnlyNewOneAfter` |
| Nav Tab Interrupted Transition | `EnkNavHost.kt` | `TabTransitionTest > secondTabTapMidTransitionLandsOnSecondTarget` |
| Nav Tab Instant under Light/ReduceEffects | `EnkNavHost.kt` | `TabTransitionTest > reduceEffectsEndsTransitionInstantly` |
| Pushed Screens (Trash, Credits) Slide In | `EnkNavHost.kt` | `PushedScreenTransitionTest > pushingTrashShowsTransitionAndBackMirrorsIt` |
| Pushed Screens Mirror on Back | `EnkNavHost.kt` | `PushedScreenTransitionTest > pushingCreditsFromSetupWorksAndBackMirrors` |
| Note Open / Close Vertical Slide & Scale | `EnkNavHost.kt`, `RailTransition.kt` | `RailTransitionTest > interruptingAtMidpointKeepsProgressContinuous` |
| LazyColumn Item Placement Animations | `BoxScreen.kt`, `TrashScreen.kt`, `SearchScreen.kt` | `ListAnimationTest > removedNoteCardDisappearsAndCardsBelowMoveUp` |
| Theme Color Crossfade Transitions | `ThemeTransition.kt` | `ThemeTransitionTest > themeChangeInterpolatesColorsSmoothly` |
| ScrollEdge Glass Blur / Solid Fallback | `ScrollEdge.kt` | `GlassGoldenTest > scrollEdgeBars`, `GlassGoldenTest > solidBackendReduceEffects` |

---

## 9. Files Changed or Deleted and Reasons

- **Deleted:** Legacy `Modifier.glass(...)` and `GlassLevel` from `:design` package — completely replaced by Backdrop 2.0.1 `Modifier.liquidGlass(...)` and `GlassSpecs`.
- **Deleted:** Steel lamp panel from `WriterScreen` — replaced by the 3-state `TopIsland` and glass `LampPopup`.
- **Modified:** `Settings.kt` and `SettingsStore.kt` — migrated `reduceEffects: Boolean` to `effects: EffectsLevel` with migration logic preserving user preferences.
- **Modified:** `WriterScreen.kt` — integrated `TopIsland`, removed old lamp panel, adjusted padding to respect glass nav insets.
- **Modified:** `EnkNavHost.kt` — replaced custom tab switches with `AnimatedContent` horizontal slide/fade and direction-aware springs; integrated `RailTransition`.

---

## 10. Technical Notes

### `lens` Behavior in Robolectric
Backdrop 2.0.1's `lens` / `BackdropFilter` shaders depend on Skia rendering on Android 12+ (RenderEffect API). In Robolectric unit tests, hardware shaders are no-ops; Backdrop falls back cleanly without crashing, allowing full UI layout, semantics, click, and interaction testing. Golden snapshot tests with Roborazzi exercise the Compose rendering path faithfully.

### `HighlightStyle.Default` Angle Convention
Default specular rim highlight angle is set to **315°** (top-left virtual light source), which matches standard material lighting conventions where key lights illuminate UI elements from the upper-left, producing lower-right inner/drop shadows.

---

## 11. Hardware QA Checklist Reference

Per instructions, hardware-specific performance and feel checks are tracked in:
`docs/qa/glass-device-checklist.md`
All 26 checks are initialized as `PENDING-DEVICE` for physical device validation.

---

## 12. Deviations & Observations

- In `app/build.gradle.kts`, Robolectric Compose UI tests that launch `ComponentActivity` are excluded from `testReleaseUnitTest` (matching previous batches) because `libs.androidx.compose.ui.test.manifest` is configured as `debugImplementation`, meaning `ComponentActivity` is only registered in debug manifests. All unit and compose tests run and pass in `testDebugUnitTest`.

---

## 13. Gate Verification & Artifact Details

### Full Gate Command:
```powershell
$env:JAVA_HOME = "D:\jdks\eclipse_adoptium-21-amd64-windows.2"; .\gradlew clean build test :design:verifyRoborazziDebug :app:verifyRoborazziDebug :app:processDebugManifest :app:checkDebugAarMetadata :app:checkReleaseAarMetadata :app:assembleRelease :app:compileDebugAndroidTestKotlin --continue
```
**Result:** `BUILD SUCCESSFUL in 6m 33s` (428 actionable tasks: 403 executed, 25 up-to-date).

### Network & F-Droid Checks:
- `scripts/check-no-network.sh`: `SUCCESS: No network permissions declared in merged manifest.`
- `scripts/check-fdroid-rules.sh`: `OK`

### Release APK Reproducibility & Hashes:
- Unsigned Release APK: `app/build/outputs/apk/release/app-release-unsigned.apk`
- File Size: `4,094,381 bytes` (~3.90 MB)
- SHA-256 (Build 1): `B3CDB9754D1BC827AD79F0E3CB15EAA6319D16AE564D4782D0B46C4CFC6857E5`
- SHA-256 (Clean Build 2): `B3CDB9754D1BC827AD79F0E3CB15EAA6319D16AE564D4782D0B46C4CFC6857E5`
- **Verification:** Identical byte-for-byte reproducible build confirmed.
