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

Antigravity changed no production Kotlin. The audit found one real bug and changed one production query:

- `data/src/main/kotlin/app/enk/data/Daos.kt`, `NoteDao.recent()`: `ORDER BY modifiedAt DESC LIMIT 1` became `ORDER BY modifiedAt DESC, createdAt DESC, rowid DESC LIMIT 1`. Two notes saved in the same millisecond used to come back in an arbitrary order (the cause of the flaky `WidgetModelTest`). Three new tests in `DaoTest` cover a `modifiedAt` tie, a full tie, and trashed notes; the widget test is back to its original form.

Other changes in production paths: `app/src/main/assets/libraries.csv` (versions), `credits.json` (regenerated).

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

### 2. Size, density and comparison strictness (auditor revision)
- Goldens are rendered by Robolectric native graphics at **w400dp x h820dp, hdpi (1.5x): 600 x 1230 px** (the logo scenes 600 x 799). The first version used xxhdpi (1200 x 2460) and put 34 MB of images in the repo; at hdpi they are 13.7 MB (`app` 11 MB, `design` 2.7 MB). Paparazzi's old images were 487 x 1000.
- Set in `robolectric.properties` (`sdk=35`, `qualifiers=w400dp-h820dp-hdpi`) and `@GraphicsMode(NATIVE)`; the rule `EnkSnapshots` (one per module) owns the setup.
- **Threshold: `changeThreshold = 0f`.** No pixel may differ. It first shipped as 0.01 (1% of all pixels, about 7,400 pixels at this size), which a 2 dp caret or a small carriage can vanish under. Rendering is software and deterministic: `verifyRoborazziDebug` passed twice in a row with `--rerun-tasks` at 0. It is verified on one machine only; goldens are not run on CI (CI never ran them under Paparazzi either).
- Proof that verify can fail, three ways, each restored afterwards:
  1. Antigravity: a solid red box in `LogoGoldenTest.foregroundMasks` failed.
  2. Auditor, at the old 1% threshold: the carriage moved one column (`CarriageGoldenTest.carriageAtRestDayMode`): `7 tests completed, 1 failed`.
  3. Auditor, at the final threshold: the caret moved one column on an empty page (`PaperGoldenTest.emptyPageOnlyCaret`, a 2 dp element): `9 tests completed, 1 failed`; restored: passes.
- `testReleaseUnitTest` excludes `**/*GoldenTest*` in `:app` and `:design`; goldens run in the debug variant through `verifyRoborazziDebug`.

### 3. Visual review (auditor)
- All 79 new goldens were viewed in six contact montages; four (glass graphite dark, glass graphite light, lamp default, scroll edge) were compared at the same scale with the old Paparazzi image. Layout, colours, text and glass look the same. Differences are resolution, text anti-aliasing and the engine's drawing of the glass.
- **What the glass goldens draw.** Under Robolectric native graphics the nav pill, sheets and the scroll-edge veil are drawn as translucent tinted surfaces with the blur visible on the scroll-edge image; there is no refraction. That matches what Paparazzi showed. Real refraction (API 33 and up) cannot be judged in a golden or on the software-rendered emulator; it needs a phone.
- The contact sheet and the 79 copies of the old images that the first version committed under `docs/audit/batch-11/` were removed from the repository (13 MB). Git history holds the old images.
- Known and unchanged: in the graphite-dark glass scene the "Write" label on the light nav pill has low contrast. It was the same under Paparazzi.

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

### 1. Final gate (auditor run, after the fixes)
```
./gradlew clean build test :design:verifyRoborazziDebug :app:verifyRoborazziDebug :app:processDebugManifest   :app:checkDebugAarMetadata :app:checkReleaseAarMetadata :app:assembleRelease :app:compileDebugAndroidTestKotlin --continue
BUILD SUCCESSFUL in 7m 1s
428 actionable tasks: 403 executed, 25 up-to-date
```
Then `bash scripts/check-no-network.sh` printed `SUCCESS: No network permissions declared in merged manifest.` and `bash scripts/check-fdroid-rules.sh` printed `OK`.

### 2. Release APK
- `app-release-unsigned.apk`: **3,912,896 bytes**. `main` (`639064f`), built the same way: **4,034,440 bytes**. The upgrade makes it 121,544 bytes (3.0%) smaller.
- SHA-256, two clean builds in a row: `5c186b5f31d90bdba808c2d924bebc5d66ff76033ae1defc1635585e2b8bd33f` both times. One machine, one JDK; not compared across machines.
- The R8 release build, signed with a throwaway debug key, was installed on the Android 17 emulator and walked through Write, Box, Find and Setup with no crash.

### 3. Dependency changes (runtime classpath, from `scripts/fdroid-allowlist.txt`)
- Compose 1.11.4 to 1.12.1 (androidx), BOM 2026.06.01 to 2026.09.00; `io.github.kyant0:backdrop` 1.0.4 to 2.0.1.
- **New with Backdrop 2:** `backdrop-android` 2.0.1, `shapes` and `shapes-android` 1.2.1, about 25 `org.jetbrains.compose.*` 1.12.0 coordinates (animation, foundation, runtime, ui and their `-android` variants), `org.jetbrains.androidx.lifecycle:*` 2.9.6, `org.jetbrains.androidx.savedstate:*` 1.3.6, `org.jetbrains:annotations` 26.1.0. All Apache-2.0 and on the allow-list. The Compose Multiplatform artifacts come from Backdrop 2 becoming a multiplatform library.
- **Gone:** `androidx.documentfile`, `dynamicanimation`, `legacy-support-core-utils`, `loader`, `localbroadcastmanager`, `print` and `transition` no longer resolve.

### 4. Release lint
`:app:lintRelease` (Antigravity): 0 errors, 12 informational warnings.

### 5. Release-variant unit tests (auditor finding, fixed)
AGP 9 creates no release unit tests unless asked, so the gate had silently stopped running them. `app`, `design`, `data` and `audio` now enable them in `beforeVariants` (`HostTestBuilder.UNIT_TEST_TYPE`), and the shared test folder is added through `sourceSets.kotlin.directories`. This exposed that the shared folder was not compiled for the release variant. The old `KotlinCompile` workaround is gone. `CreditsLoaderTest` runs under Robolectric because `org.json` is stubbed (empty) on the plain JVM classpath.

---

## 6. Complete Unit Test Suite (883 debug + 606 release test executions, 0 failures)

**Debug variant: 883 tests** (`app` 523, `design` 178, `data` 88, `audio` 42, `text` 52). **Release variant: 606** (`app` 321, `design` 155, `data` 88, `audio` 42); the golden classes run in debug only. Debug +3 over the first handoff are the new `DaoTest` tests. Per-class table below is the debug run of Antigravity's handoff, before those three:
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

- Device test instructions: `docs/qa/batch-11-device-walk.md` (rewritten by the auditor from the real UI).

---

## 8. Not verified, and what the auditor changed

- Not verified: refraction on a phone (the emulator draws in software), the widget on a launcher, export and import on the release build, a build on a second machine, TalkBack.
- Antigravity delivered one commit instead of one per task; it is kept as one.
- Auditor changes: release unit tests re-enabled; goldens re-recorded at hdpi with a zero-pixel threshold; old images and the contact sheet removed from the repository; widget ordering tie fixed in `NoteDao.recent()`; the `KotlinCompile` workaround removed; device walk and this handoff corrected.
