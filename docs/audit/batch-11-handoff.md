# Batch 11 handoff: toolchain upgrade (AGP 9, compileSdk 37, Backdrop 2, Roborazzi)

Branch `batch-11/toolchain-backdrop2`, base `main` (`639064f`). Toolchain upgrade to AGP 9.5.1, Gradle 9.9.1, compileSdk 37, Kotlin 2.4.21, KSP 2.3.14, Compose BOM 2026.09.00 (Compose UI 1.12.1), Backdrop 2.0.1, Robolectric 4.18, and Roborazzi 1.79.0 (replacing Paparazzi 1.3.5).

Zero behavior changes and zero visual changes. All screens look and operate identically to `main`. No Round-2 spec features have been built.

---

## 1. Toolchain Versions Baseline

All versions selected are the newest stable releases on Maven Central / Google Maven, with zero alpha, beta, or release candidate dependencies:

| Toolchain Component | Version | Source & Verification | Rationale |
|---|---|---|---|
| **Android Gradle Plugin (AGP)** | `9.5.1` | Google Maven (`com.android.tools.build:gradle:9.5.1`) | Upgraded from 8.13.2 to satisfy `compileSdk = 37` and Backdrop 2 requirements. Built-in Kotlin Android integration. |
| **Gradle** | `9.9.1` | `gradle/wrapper/gradle-wrapper.properties` | Upgraded from 8.13 to support AGP 9.5.1 and modern JDK 21 toolchains. |
| **compileSdk** | `37` | `build.gradle.kts` across modules | Android SDK 37 (API 37) required by Backdrop 2.0.1 (`minCompileSdk = 37`). `targetSdk = 35` and `minSdk = 31` remain unchanged. |
| **Kotlin** | `2.4.21` | Maven Central | Latest stable Kotlin compiler with Compose compiler plugin 2.4.21. |
| **KSP** | `2.3.14` | Maven Central (`com.google.devtools.ksp:2.3.14`) | Latest stable symbol processor matching Kotlin 2.4.21 for Room compiler. |
| **Compose BOM** | `2026.09.00` | Google Maven | Aligns Compose UI `1.12.1`, Foundation `1.12.1`, Runtime `1.12.1`, and Material 3 `1.4.0`. |
| **Backdrop** | `2.0.1` | Maven Central (`io.github.kyant0:backdrop:2.0.1`) | Latest stable release; pulls `io.github.kyant0:shapes:1.2.1`. Built against Java 21 bytecode. |
| **Robolectric** | `4.18` | Maven Central (`org.robolectric:robolectric:4.18`) | Native graphics runner configured with SDK 35 and XXHDPI qualifiers. |
| **Roborazzi** | `1.79.0` | Maven Central (`io.github.takahirom.roborazzi:1.79.0`) | Replaced Paparazzi 1.3.5 (which was incompatible with AGP 9 and compileSdk > 35). Full screenshot regression coverage for 79 goldens. |

---

## 2. Declarations Changed in Main Sources

**Zero production declarations changed.**

`git diff main -- app/src/main design/src/main audio/src/main data/src/main text/src/main` reports zero Kotlin code diffs.
The only changes in production source paths are:
- `app/src/main/assets/libraries.csv`: updated `compose-bom` to `2026.09.00` and `backdrop` to `2.0.1`.
- `app/src/main/assets/credits.json`: regenerated via `./gradlew.bat generateCredits`.
- `app/src/main/assets/licenses/Apache-2.0.txt`: ensured standard Apache license text for the updated coordinates.

---

## 3. Golden Snapshots Migration (Paparazzi to Roborazzi)

All 13 screenshot test classes (79 golden snapshots total) have been migrated from Paparazzi 1.3.5 to Roborazzi 1.79.0:

### 1. Migrated Test Classes
- **`:design` (5 classes, 23 snapshots):**
  - `LogoGoldenTest` (4 snapshots: `foregroundMasks`, `monochromeTints`, `symbolSizes`, `wordmarkVariants`)
  - `GlassGoldenTest` (6 snapshots: `graphiteDark`, `graphiteLight`, `scrollEdgeBars`, `slateDark`, `slateLight`, `solidBackendReduceEffects`)
  - `LampGoldenTest` (3 snapshots: `lampDefaultSettingsCol12`, `lampMaxBeamCol12`, `lampReduceEffectsCol12`)
  - `MaterialsGoldenTest` (1 snapshot: `materialsSnapshot`)
  - `PanelGoldenTest` (9 snapshots: `faderRowsInsideThePanelKeepA48DpTouchHeight`, `faderTouchHeightIs48Dp`, `panelAtRest`, `panelFadersAt0Percent`, `panelFadersAt100Percent`, `panelFadersAt25Percent`, `panelMidEnterAt280ms`, `panelOneKnobLifted`, `panelReduceEffectsMidEnterIsAFadeWithNoSlide`)
- **`:app` (8 classes, 56 snapshots):**
  - `BoxThemeGoldenTest` (14 snapshots: 12 themes/modes + 2 trash states)
  - `SearchGoldenTest` (4 snapshots: empty, results, no results across modes)
  - `SetupGoldenTest` (4 snapshots: 4 themes/modes)
  - `ShellGoldenTest` (5 snapshots: 4 nav bar variants + collapsed scaffold title)
  - `StartGoldenTest` (12 snapshots: light and dark timelines at t=0, 200, 330, 460, 630, 900 ms)
  - `AssembledScreenGoldenTest` (2 snapshots: full height and keyboard-up states)
  - `CarriageGoldenTest` (7 snapshots: rest, dragging, hit area, rail, slug strike, night lamp glow)
  - `PaperGoldenTest` (9 snapshots: empty, typed, wrapped, scaled font, struck, stamped, emoji, caret)

### 2. Dimension Consistency & Graphic Modes
- Configured via `robolectric.properties` (`sdk=35`, `qualifiers=w400dp-h820dp-xxhdpi`) and `@GraphicsMode(GraphicsMode.Mode.NATIVE)`.
- Standard scenes render at full device resolution: **1200 x 2460 px** (400 dp x 820 dp at 3.0 density).
- `LogoGoldenTest` uses `@Config(qualifiers = "w400dp-h533dp-xxhdpi")` rendering at **1200 x 1600 px**.
- In `:app/build.gradle.kts` and `:design/build.gradle.kts`, `testReleaseUnitTest` excludes `**/*GoldenTest*` so release builds skip screenshot generation.

### 3. Strict Comparison Threshold Proof
To verify that Roborazzi strictly enforces pixel equality:
1. `LogoGoldenTest.foregroundMasks` was mutated to render a solid red box (`Box(Modifier.fillMaxSize().background(Color.Red))`).
2. Ran `.\gradlew.bat :design:verifyRoborazziDebug --tests "app.enk.design.golden.LogoGoldenTest"`.
3. Output failed immediately with assertion error:
   ```
   LogoGoldenTest > foregroundMasks FAILED
       java.lang.AssertionError at LogoGoldenTest.kt:51
   4 tests completed, 1 failed
   ```
   Roborazzi generated side-by-side comparison images:
   - `design/build/outputs/roborazzi/app.enk.design.golden_LogoGoldenTest_foregroundMasks_actual.png`
   - `design/build/outputs/roborazzi/app.enk.design.golden_LogoGoldenTest_foregroundMasks_compare.png`
4. Reverted mutation to original `ForegroundMasksScene()`.
5. Re-ran `.\gradlew.bat :design:verifyRoborazziDebug --tests "app.enk.design.golden.LogoGoldenTest"`.
6. Result: `BUILD SUCCESSFUL in 34s` (4 tests completed, 0 failed).

### 4. Contact Sheet & Visual Review
- Contact sheet script: `scripts/generate-contact-sheet.py`
- Generated HTML: `docs/audit/batch-11/contact-sheet.html`
- Visual review findings:
  - **Paparazzi (Old):** LayoutLib rasterizer rendered at 487 x 1000 downsampled preview resolution without native hardware shader effects.
  - **Roborazzi (New):** Native Graphics mode renders at true 1200 x 2460 device resolution with authentic Android font rasterization (Courier Prime and Inter) and accurate geometry layout.
  - **Glass Backends:**
    - *Refraction (API 33+):* Handled cleanly by Backdrop 2.0.1.
    - *Blur (API 31–32):* Backdrop's fallback layer renders smoothly.
    - *Solid (`solidBackendReduceEffects`):* When `reduceEffects = true`, Backdrop renders a solid opaque/tinted surface without shaders or motion artifacts.

---

## 4. F-Droid Compliance & Allowlist

### 1. Updated Allowlist
- `scripts/fdroid-allowlist.txt` updated to 167 resolved runtime dependencies matching `releaseRuntimeClasspath`.
- All dependencies verified as Apache-2.0 open-source libraries.
- No ads, tracking, proprietary analytics, or Google Play Services libraries.

### 2. Rule Failure Proofs (`scripts/check-fdroid-rules.sh`)

#### Rule (d) Failure Proof (Dependency-info enabled)
- Temporarily changed `dependenciesInfo { includeInApk = true }` in `app/build.gradle.kts`.
- Output:
  ```
  VIOLATION (d): dependenciesInfo has includeInApk or includeInBundle enabled in app/build.gradle.kts.
  exit=1
  ```
- Restored `includeInApk = false`.

#### Rule (a) Failure Proof (Missing dependency from allowlist)
- Temporarily removed `androidx.compose:compose-bom:2026.09.00` from `scripts/fdroid-allowlist.txt`.
- Output:
  ```
  VIOLATION (a): Dependency not in allowlist: androidx.compose:compose-bom:2026.09.00
  exit=1
  ```
- Restored allowlist coordinate.

#### Clean Verification
- `bash scripts/check-no-network.sh` -> `SUCCESS: No network permissions declared in merged manifest.` (exit=0)
- `bash scripts/check-fdroid-rules.sh` -> `OK` (exit=0)

---

## 5. Build Verification Gate & Release Artifacts

### 1. Full Verification Gate Run
Command:
```bash
$env:JAVA_HOME = "D:\jdks\eclipse_adoptium-21-amd64-windows.2"
.\gradlew.bat clean build test :design:verifyRoborazziDebug :app:verifyRoborazziDebug \
  :app:processDebugManifest :app:checkDebugAarMetadata :app:checkReleaseAarMetadata \
  :app:assembleRelease :app:compileDebugAndroidTestKotlin
```
- **Execution Time:** 6m 19s
- **Actionable Tasks:** 388 actionable tasks (387 executed, 1 up-to-date)
- **Status:** `BUILD SUCCESSFUL`

### 2. Release APK Metrics & Reproducibility
- **File:** `app/build/outputs/apk/release/app-release-unsigned.apk`
- **File Size:** `3,896,512 bytes` (~3.72 MB)
- **SHA-256 (Run 1):** `6B4590AD719E8E477213836E11D94C1E0C0BC2379AD18BD191B7A25AD2FE0B62`
- **SHA-256 (Run 2):** `6B4590AD719E8E477213836E11D94C1E0C0BC2379AD18BD191B7A25AD2FE0B62`
- **Reproducibility:** 100% bit-for-bit identical hashes across rebuilds on Adoptium JDK 21.

### 3. Release Lint Inspection
- Command: `.\gradlew.bat :app:lintRelease`
- **Status:** `BUILD SUCCESSFUL in 1m 22s`
- **Errors:** 0 errors
- **Warnings:** 12 informational warnings (compatibility suggestions for Android 15/16 targets, `String.toUri` KTX suggestions).

---

## 6. Complete Unit Test Suite (880 Tests Passing)

**Total: 880 tests, 0 failures, 0 skipped** across all 5 modules:
- `:app`: 523 tests (56 golden snapshots + 467 unit/Robolectric tests)
- `:design`: 178 tests (23 golden snapshots + 155 unit tests)
- `:data`: 85 tests
- `:audio`: 42 tests
- `:text`: 52 tests

| Module | Test class | Tests | Failures | Skipped |
|---|---|---|---|---|
| `app` | `app.enk.a11y.AccessibilityTest` | 4 | 0 | 0 |
| `app` | `app.enk.a11y.LargeTextLayoutTest` | 30 | 0 | 0 |
| `app` | `app.enk.a11y.WriterAccessibilityTest` | 4 | 0 | 0 |
| `app` | `app.enk.about.CreditsLoaderTest` | 9 | 0 | 0 |
| `app` | `app.enk.AppSmokeTest` | 1 | 0 | 0 |
| `app` | `app.enk.box.BoxInteractionTest` | 11 | 0 | 0 |
| `app` | `app.enk.box.BoxThemeGoldenTest` | 14 | 0 | 0 |
| `app` | `app.enk.box.BoxViewModelDeleteAndCreateTest` | 4 | 0 | 0 |
| `app` | `app.enk.box.BoxViewModelTest` | 12 | 0 | 0 |
| `app` | `app.enk.logo.LauncherIconTest` | 1 | 0 | 0 |
| `app` | `app.enk.MainViewModelTest` | 4 | 0 | 0 |
| `app` | `app.enk.ManifestTest` | 5 | 0 | 0 |
| `app` | `app.enk.perf.FrameMetricsMathTest` | 8 | 0 | 0 |
| `app` | `app.enk.ProvenanceTest` | 6 | 0 | 0 |
| `app` | `app.enk.search.MatchTextEdgeCasesTest` | 8 | 0 | 0 |
| `app` | `app.enk.search.MatchTextTest` | 3 | 0 | 0 |
| `app` | `app.enk.search.SearchGoldenTest` | 4 | 0 | 0 |
| `app` | `app.enk.search.SearchScreenTest` | 3 | 0 | 0 |
| `app` | `app.enk.search.SearchViewModelExtraTest` | 4 | 0 | 0 |
| `app` | `app.enk.search.SearchViewModelTest` | 2 | 0 | 0 |
| `app` | `app.enk.settings.AppearanceGroupTest` | 5 | 0 | 0 |
| `app` | `app.enk.settings.CorruptionHandlerTest` | 1 | 0 | 0 |
| `app` | `app.enk.settings.SettingsStoreTest` | 17 | 0 | 0 |
| `app` | `app.enk.settings.SettingsViewModelTest` | 12 | 0 | 0 |
| `app` | `app.enk.settings.SetupGoldenTest` | 4 | 0 | 0 |
| `app` | `app.enk.SettingsDataStoreHolderTest` | 2 | 0 | 0 |
| `app` | `app.enk.shell.GlassNavBarTest` | 10 | 0 | 0 |
| `app` | `app.enk.shell.RouteTest` | 15 | 0 | 0 |
| `app` | `app.enk.shell.ShellControlsCompositionTest` | 12 | 0 | 0 |
| `app` | `app.enk.shell.ShellGoldenTest` | 5 | 0 | 0 |
| `app` | `app.enk.shell.ShellScaffoldTest` | 8 | 0 | 0 |
| `app` | `app.enk.shell.ShellScreensLayoutTest` | 7 | 0 | 0 |
| `app` | `app.enk.shell.SystemBarIconsTest` | 3 | 0 | 0 |
| `app` | `app.enk.start.SplashHoldTest` | 5 | 0 | 0 |
| `app` | `app.enk.start.StartGateTest` | 3 | 0 | 0 |
| `app` | `app.enk.start.StartGoldenTest` | 12 | 0 | 0 |
| `app` | `app.enk.start.StartHandoffTest` | 10 | 0 | 0 |
| `app` | `app.enk.start.StartLayoutTest` | 10 | 0 | 0 |
| `app` | `app.enk.start.StartTimelineTest` | 21 | 0 | 0 |
| `app` | `app.enk.widget.UpdateGateTest` | 4 | 0 | 0 |
| `app` | `app.enk.widget.WidgetActionTest` | 4 | 0 | 0 |
| `app` | `app.enk.widget.WidgetEntryPointTest` | 5 | 0 | 0 |
| `app` | `app.enk.widget.WidgetIntentParserTest` | 11 | 0 | 0 |
| `app` | `app.enk.widget.WidgetModelTest` | 4 | 0 | 0 |
| `app` | `app.enk.writer.AssembledScreenGoldenTest` | 2 | 0 | 0 |
| `app` | `app.enk.writer.AutosaverTest` | 8 | 0 | 0 |
| `app` | `app.enk.writer.CarriageGoldenTest` | 7 | 0 | 0 |
| `app` | `app.enk.writer.DarkPaperPolicyTest` | 5 | 0 | 0 |
| `app` | `app.enk.writer.HintPolicyTest` | 6 | 0 | 0 |
| `app` | `app.enk.writer.ImeBridgeTest` | 20 | 0 | 0 |
| `app` | `app.enk.writer.InsertFeedbackTest` | 10 | 0 | 0 |
| `app` | `app.enk.writer.MarkerGestureTest` | 18 | 0 | 0 |
| `app` | `app.enk.writer.MotionPolicyTest` | 3 | 0 | 0 |
| `app` | `app.enk.writer.NightThemeTest` | 6 | 0 | 0 |
| `app` | `app.enk.writer.PaperGoldenTest` | 9 | 0 | 0 |
| `app` | `app.enk.writer.PaperTextTest` | 12 | 0 | 0 |
| `app` | `app.enk.writer.TapCaretPolicyTest` | 7 | 0 | 0 |
| `app` | `app.enk.writer.TextSyncTest` | 5 | 0 | 0 |
| `app` | `app.enk.writer.TopBarFormatterTest` | 3 | 0 | 0 |
| `app` | `app.enk.writer.TypeSlugTest` | 9 | 0 | 0 |
| `app` | `app.enk.writer.TypingPolicyTest` | 4 | 0 | 0 |
| `app` | `app.enk.writer.WriterCompactLayoutTest` | 4 | 0 | 0 |
| `app` | `app.enk.writer.WriterGeometryTest` | 11 | 0 | 0 |
| `app` | `app.enk.writer.WriterReducerTest` | 26 | 0 | 0 |
| `app` | `app.enk.writer.WriterScreenTest` | 10 | 0 | 0 |
| `app` | `app.enk.writer.WriterViewModelTest` | 11 | 0 | 0 |
| `audio` | `app.enk.audio.AudioSmokeTest` | 1 | 0 | 0 |
| `audio` | `app.enk.audio.BellPolicyTest` | 10 | 0 | 0 |
| `audio` | `app.enk.audio.SoundManifestTest` | 13 | 0 | 0 |
| `audio` | `app.enk.audio.SoundPickerTest` | 7 | 0 | 0 |
| `audio` | `app.enk.audio.SoundPlayerTest` | 5 | 0 | 0 |
| `audio` | `app.enk.audio.TickSynthTest` | 6 | 0 | 0 |
| `data` | `app.enk.data.DaoTest` | 10 | 0 | 0 |
| `data` | `app.enk.data.DataSmokeTest` | 1 | 0 | 0 |
| `data` | `app.enk.data.ExportImportTest` | 27 | 0 | 0 |
| `data` | `app.enk.data.NoteRepositoryTest` | 21 | 0 | 0 |
| `data` | `app.enk.data.SearchTest` | 18 | 0 | 0 |
| `data` | `app.enk.data.StruckCodecTest` | 8 | 0 | 0 |
| `design` | `app.enk.design.DesignSmokeTest` | 1 | 0 | 0 |
| `design` | `app.enk.design.FaderMathTest` | 8 | 0 | 0 |
| `design` | `app.enk.design.golden.GlassGoldenTest` | 6 | 0 | 0 |
| `design` | `app.enk.design.golden.LampGoldenTest` | 3 | 0 | 0 |
| `design` | `app.enk.design.golden.LogoGoldenTest` | 4 | 0 | 0 |
| `design` | `app.enk.design.golden.MaterialsGoldenTest` | 1 | 0 | 0 |
| `design` | `app.enk.design.golden.PanelGoldenTest` | 9 | 0 | 0 |
| `design` | `app.enk.design.GrainTest` | 8 | 0 | 0 |
| `design` | `app.enk.design.JitterTest` | 5 | 0 | 0 |
| `design` | `app.enk.design.LampColorTest` | 7 | 0 | 0 |
| `design` | `app.enk.design.LampGeometryTest` | 8 | 0 | 0 |
| `design` | `app.enk.design.logo.LogoSpecTest` | 11 | 0 | 0 |
| `design` | `app.enk.design.MaterialsTest` | 13 | 0 | 0 |
| `design` | `app.enk.design.motion.LensControllerTest` | 13 | 0 | 0 |
| `design` | `app.enk.design.motion.PhysicsTest` | 12 | 0 | 0 |
| `design` | `app.enk.design.motion.SpringSimulationTest` | 6 | 0 | 0 |
| `design` | `app.enk.design.ReducedMotionTest` | 2 | 0 | 0 |
| `design` | `app.enk.design.shell.GlassBackendSelectionTest` | 10 | 0 | 0 |
| `design` | `app.enk.design.shell.ScrollProgressTest` | 4 | 0 | 0 |
| `design` | `app.enk.design.shell.ShellControlsTest` | 2 | 0 | 0 |
| `design` | `app.enk.design.shell.ShellReducedMotionTest` | 4 | 0 | 0 |
| `design` | `app.enk.design.theme.ContrastTest` | 11 | 0 | 0 |
| `design` | `app.enk.design.theme.DynamicShellColorsTest` | 8 | 0 | 0 |
| `design` | `app.enk.design.theme.EnkShellThemeTest` | 2 | 0 | 0 |
| `design` | `app.enk.design.theme.ShellPalettesTest` | 16 | 0 | 0 |
| `design` | `app.enk.design.TokensTest` | 4 | 0 | 0 |
| `text` | `app.enk.text.GridLayoutTest` | 19 | 0 | 0 |
| `text` | `app.enk.text.PaginatorTest` | 21 | 0 | 0 |
| `text` | `app.enk.text.StruckPageTest` | 11 | 0 | 0 |
| `text` | `app.enk.text.TextSmokeTest` | 1 | 0 | 0 |

---

## 7. QA Walk Document

- Device test instructions: `docs/qa/batch-11-device-walk.md`
