# Batch 10 Handoff: Release Readiness and 1.0 Acceptance

**Batch:** 10 of 10 (Final Batch: Tasks 27 & 28)  
**Branch:** `batch-10/release-and-acceptance`  
**Base commit:** `31649d9` (`main`, Batch 9 approved and merged)  
**Toolchain:** OpenJDK 21 (Adoptium 21.0.6), AGP 8.13.2, Gradle 8.13, Kotlin 2.4.21, compileSdk 35, minSdk 31.

---

## 1. What was built

Batch 10 delivers Tasks 27 and 28 from the Enk 1.0 Implementation Plan:
1. **Task 27: F-Droid Readiness, Release Build, and Packaging**
   - Release build configuration with R8 shrinking and obfuscation (`isMinifyEnabled = true`, `isShrinkResources = true`).
   - `proguard-rules.pro` keeping Room schemas and Glance widget receivers.
   - Pinned Gradle wrapper checksum in `gradle/wrapper/gradle-wrapper.properties`.
   - Strip VCS metadata and dependency-info blobs (`vcsInfo.include = false`, `dependenciesInfo.includeInApk = false`).
   - Release signing configuration with environment variables / `keystore.properties` fallback to unsigned build.
   - `docs/provenance-other.csv` tracking fonts, icons, and artwork under permissive licenses.
   - `scripts/fdroid-allowlist.txt` containing 142 allowlisted runtime dependency coordinates.
   - `scripts/check-fdroid-rules.sh` verifying allowlisted dependencies, zero network permissions, license tracking, and no dependency-info blob.
   - Fully reproducible release build verified bit-for-bit across multiple clean builds.
   - CI workflow `.github/workflows/ci.yml` running network and F-Droid rules checks.
   - Release workflow `.github/workflows/release.yml` triggering on `v*` tags with artifact hashing and signing.
   - Fastlane store metadata under `fastlane/metadata/android/en-US/` (title, short/full descriptions, changelog, 512x512 icon).
   - Documentation suite: `docs/CONTRIBUTING.md`, `docs/adding-a-sound-set.md`, `docs/release/RELEASING.md`, `docs/release/fdroid-submission.md`, `docs/qa/release-smoke.md`, and updated `README.md` reflecting JDK 21 requirement.

2. **Task 28: QA Matrix, Latency Test, and 1.0 Acceptance**
   - Pure frame timing and jank math functions (`FrameMetricsMath`) and unit tests (`FrameMetricsMathTest`).
   - Instrumented latency test `TypeToStampLatencyTest` measuring frame times (p95 <= 16 ms, jank <= 1.0%) and SoundPool dispatch.
   - Hot spots review on typing path (`WriterScreen`, `PaperText`, `Caret`, `Carriage`, `Grain`, `LampLight`) confirming zero per-frame draw allocations, memoized text layouts, pre-tiled grain bitmap, and phase-offset caret animation.
   - Acceptance checklist `docs/qa/1.0-acceptance.md` mapping all product success criteria, evidence, verification methods, device QA matrix template, and assigned ownership for hardware items.

---

## 2. Declarations Table

Generated directly by inspecting the source code:

| Element | Kind | Signature | File | Notes |
|---|---|---|---|---|
| `FrameMetricsMath` | `object` | `object FrameMetricsMath` | `app/src/main/kotlin/app/enk/perf/FrameMetricsMath.kt` | Pure percentile and jank rate calculator |
| `percentile` | `fun` | `fun percentile(durationsMs: List<Double>, p: Double): Double` | `app/src/main/kotlin/app/enk/perf/FrameMetricsMath.kt` | Linear interpolation, NaN/negative filtered |
| `jankFraction` | `fun` | `fun jankFraction(durationsMs: List<Double>, thresholdMs: Double = 16.666): Double` | `app/src/main/kotlin/app/enk/perf/FrameMetricsMath.kt` | Computes fraction of frames exceeding threshold |
| `FrameMetricsMathTest` | `class` | `class FrameMetricsMathTest` | `app/src/test/kotlin/app/enk/perf/FrameMetricsMathTest.kt` | 6 unit tests covering edge cases and normal datasets |
| `TypeToStampLatencyTest` | `class` | `class TypeToStampLatencyTest` | `app/src/androidTest/kotlin/app/enk/perf/TypeToStampLatencyTest.kt` | Instrumented test measuring frame metrics & audio dispatch |

---

## 3. Test Summary (From Gradle XML Reports)

**Grand Total: 859 unit tests, 0 failures, 0 skipped** across all 5 modules.

- `:app`: 502 tests (including 6 new tests in `FrameMetricsMathTest`)
- `:audio`: 42 tests
- `:data`: 85 tests
- `:design`: 178 tests
- `:text`: 52 tests

### Complete Test Results Breakdown

| Module | Test Class | Tests | Failures | Skipped |
|---|---|---|---|---|
| `:app` | `AccessibilityTest` | 4 | 0 | 0 |
| `:app` | `AppearanceGroupTest` | 5 | 0 | 0 |
| `:app` | `AppSmokeTest` | 1 | 0 | 0 |
| `:app` | `AssembledScreenGoldenTest` | 2 | 0 | 0 |
| `:app` | `AutosaverTest` | 8 | 0 | 0 |
| `:app` | `BoxInteractionTest` | 11 | 0 | 0 |
| `:app` | `BoxThemeGoldenTest` | 14 | 0 | 0 |
| `:app` | `BoxViewModelDeleteAndCreateTest` | 4 | 0 | 0 |
| `:app` | `BoxViewModelTest` | 12 | 0 | 0 |
| `:app` | `CarriageGoldenTest` | 7 | 0 | 0 |
| `:app` | `CorruptionHandlerTest` | 1 | 0 | 0 |
| `:app` | `CreditsLoaderTest` | 9 | 0 | 0 |
| `:app` | `FrameMetricsMathTest` | 6 | 0 | 0 |
| `:app` | `GlassNavBarTest` | 10 | 0 | 0 |
| `:app` | `HintPolicyTest` | 6 | 0 | 0 |
| `:app` | `ImeBridgeTest` | 20 | 0 | 0 |
| `:app` | `LargeTextLayoutTest` | 30 | 0 | 0 |
| `:app` | `LauncherIconTest` | 1 | 0 | 0 |
| `:app` | `MainViewModelTest` | 4 | 0 | 0 |
| `:app` | `ManifestTest` | 5 | 0 | 0 |
| `:app` | `MarkerGestureTest` | 18 | 0 | 0 |
| `:app` | `MatchTextEdgeCasesTest` | 8 | 0 | 0 |
| `:app` | `MatchTextTest` | 3 | 0 | 0 |
| `:app` | `MotionPolicyTest` | 3 | 0 | 0 |
| `:app` | `NightThemeTest` | 6 | 0 | 0 |
| `:app` | `PaperGoldenTest` | 9 | 0 | 0 |
| `:app` | `PaperTextTest` | 12 | 0 | 0 |
| `:app` | `ProvenanceTest` | 6 | 0 | 0 |
| `:app` | `RouteTest` | 15 | 0 | 0 |
| `:app` | `SearchGoldenTest` | 4 | 0 | 0 |
| `:app` | `SearchScreenTest` | 3 | 0 | 0 |
| `:app` | `SearchViewModelExtraTest` | 4 | 0 | 0 |
| `:app` | `SearchViewModelTest` | 2 | 0 | 0 |
| `:app` | `SettingsStoreTest` | 16 | 0 | 0 |
| `:app` | `SettingsViewModelTest` | 12 | 0 | 0 |
| `:app` | `SetupGoldenTest` | 4 | 0 | 0 |
| `:app` | `ShellControlsCompositionTest` | 12 | 0 | 0 |
| `:app` | `ShellGoldenTest` | 5 | 0 | 0 |
| `:app` | `ShellScaffoldTest` | 8 | 0 | 0 |
| `:app` | `ShellScreensLayoutTest` | 7 | 0 | 0 |
| `:app` | `SplashHoldTest` | 5 | 0 | 0 |
| `:app` | `StartGateTest` | 3 | 0 | 0 |
| `:app` | `StartGoldenTest` | 12 | 0 | 0 |
| `:app` | `StartHandoffTest` | 10 | 0 | 0 |
| `:app` | `StartLayoutTest` | 10 | 0 | 0 |
| `:app` | `StartTimelineTest` | 21 | 0 | 0 |
| `:app` | `SystemBarIconsTest` | 3 | 0 | 0 |
| `:app` | `TapCaretPolicyTest` | 7 | 0 | 0 |
| `:app` | `TextSyncTest` | 5 | 0 | 0 |
| `:app` | `TopBarFormatterTest` | 3 | 0 | 0 |
| `:app` | `TypeSlugTest` | 9 | 0 | 0 |
| `:app` | `TypingPolicyTest` | 4 | 0 | 0 |
| `:app` | `UpdateGateTest` | 4 | 0 | 0 |
| `:app` | `WidgetActionTest` | 4 | 0 | 0 |
| `:app` | `WidgetEntryPointTest` | 5 | 0 | 0 |
| `:app` | `WidgetIntentParserTest` | 11 | 0 | 0 |
| `:app` | `WidgetModelTest` | 4 | 0 | 0 |
| `:app` | `WriterAccessibilityTest` | 4 | 0 | 0 |
| `:app` | `WriterCompactLayoutTest` | 4 | 0 | 0 |
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
| `:design` | `ContrastTest` | 11 | 0 | 0 |
| `:design` | `DesignSmokeTest` | 1 | 0 | 0 |
| `:design` | `DynamicShellColorsTest` | 8 | 0 | 0 |
| `:design` | `EnkShellThemeTest` | 2 | 0 | 0 |
| `:design` | `FaderMathTest` | 8 | 0 | 0 |
| `:design` | `GlassBackendSelectionTest` | 10 | 0 | 0 |
| `:design` | `GlassGoldenTest` | 6 | 0 | 0 |
| `:design` | `GrainTest` | 8 | 0 | 0 |
| `:design` | `JitterTest` | 5 | 0 | 0 |
| `:design` | `LampColorTest` | 7 | 0 | 0 |
| `:design` | `LampGeometryTest` | 8 | 0 | 0 |
| `:design` | `LampGoldenTest` | 3 | 0 | 0 |
| `:design` | `LensControllerTest` | 13 | 0 | 0 |
| `:design` | `LogoGoldenTest` | 4 | 0 | 0 |
| `:design` | `LogoSpecTest` | 11 | 0 | 0 |
| `:design` | `MaterialsGoldenTest` | 1 | 0 | 0 |
| `:design` | `MaterialsTest` | 13 | 0 | 0 |
| `:design` | `PanelGoldenTest` | 9 | 0 | 0 |
| `:design` | `PhysicsTest` | 12 | 0 | 0 |
| `:design` | `ReducedMotionTest` | 2 | 0 | 0 |
| `:design` | `ScrollProgressTest` | 4 | 0 | 0 |
| `:design` | `ShellControlsTest` | 2 | 0 | 0 |
| `:design` | `ShellPalettesTest` | 16 | 0 | 0 |
| `:design` | `ShellReducedMotionTest` | 4 | 0 | 0 |
| `:design` | `SpringSimulationTest` | 6 | 0 | 0 |
| `:design` | `TokensTest` | 4 | 0 | 0 |
| `:text` | `GridLayoutTest` | 19 | 0 | 0 |
| `:text` | `PaginatorTest` | 21 | 0 | 0 |
| `:text` | `StruckPageTest` | 11 | 0 | 0 |
| `:text` | `TextSmokeTest` | 1 | 0 | 0 |

---

## 4. Tests Broken on Purpose During Implementation

During the implementation of `FrameMetricsMathTest`:
- Initial test assertion:
  ```kotlin
  assertEquals(22.75, p95, 0.01)
  ```
- Failure observed:
  ```
  java.lang.AssertionError: expected:<22.75> but was:<10.75>
  ```
- Root cause: In a 100-element dataset containing 95 elements of `10.0` (indices 0..94) and 5 elements of `25.0` (indices 95..99), the 95th percentile rank is `(95 / 100) * 99 = 94.05`. Linear interpolation between `sorted[94]` (`10.0`) and `sorted[95]` (`25.0`) yields:
  $$10.0 + 0.05 \times (25.0 - 10.0) = 10.75$$
- Resolution: Test assertion corrected to `assertEquals(10.75, p95, 0.01)`.

---

## 5. Script Failure Proofs (`scripts/check-fdroid-rules.sh`)

Verified that each rule in `scripts/check-fdroid-rules.sh` fails cleanly when triggered:

1. **Rule (a) Prohibited/Unallowed Dependency:**
   - Simulated: Added `implementation("com.google.android.gms:play-services-base:18.5.0")`
   - Output:
     ```
     ERROR: Rule (a) failed: Dependency not in allowlist: com.google.android.gms:play-services-base:18.5.0
     ```
2. **Rule (b) Network Permission:**
   - Simulated: Added `<uses-permission android:name="android.permission.INTERNET" />` to release manifest
   - Output:
     ```
     ERROR: Rule (b) failed: Prohibited network permission found in release manifest: android.permission.INTERNET
     ```
3. **Rule (c) Unlisted Media Asset:**
   - Simulated: Added untracked file `app/src/main/res/raw/unlisted_test_sample.ogg`
   - Output:
     ```
     ERROR: Rule (c) failed: Media file not tracked in provenance docs: app/src/main/res/raw/unlisted_test_sample.ogg
     ```
4. **Rule (d) Dependency Info Blob:**
   - Simulated: Set `dependenciesInfo.includeInApk = true`
   - Output:
     ```
     ERROR: Rule (d) failed: Dependency info blob found in release APK: META-INF/app.enk.notes_release.kotlin_module
     ```

---

## 6. Bit-for-Bit Reproducible Build Verification

Release APK built across separate clean runs yields identical SHA-256 hash:

- **Build 1 SHA-256:** `92a9bec89f6865a70783f4779729866c5796833fac13a643109350b11a42bd2d`
- **Build 2 SHA-256:** `92a9bec89f6865a70783f4779729866c5796833fac13a643109350b11a42bd2d`
- **Final Gate SHA-256:** `92a9bec89f6865a70783f4779729866c5796833fac13a643109350b11a42bd2d`

---

## 7. New Files Added in Batch 10

| File | Purpose |
|---|---|
| `app/src/main/kotlin/app/enk/perf/FrameMetricsMath.kt` | Pure math function for percentile and jank fraction calculations |
| `app/src/test/kotlin/app/enk/perf/FrameMetricsMathTest.kt` | Unit tests for percentile and jank rate math |
| `app/src/androidTest/kotlin/app/enk/perf/TypeToStampLatencyTest.kt` | Instrumented test measuring frame times, jank rate, and audio dispatch latency |
| `app/proguard-rules.pro` | R8 ProGuard rules for release shrinking |
| `docs/provenance-other.csv` | Tracks Courier Prime fonts, launcher icons, and brand artwork |
| `scripts/fdroid-allowlist.txt` | Allowlist of 142 resolved runtime coordinates with license comments |
| `scripts/check-fdroid-rules.sh` | Bash script enforcing F-Droid packaging rules (a)-(d) |
| `.github/workflows/release.yml` | GitHub Actions workflow for tagged releases |
| `fastlane/metadata/android/en-US/title.txt` | Store listing title ("Enk") |
| `fastlane/metadata/android/en-US/short_description.txt` | Store listing short description (59 characters) |
| `fastlane/metadata/android/en-US/full_description.txt` | Store listing full description |
| `fastlane/metadata/android/en-US/changelogs/1.txt` | Release changelog for versionCode 1 (1.0.0) |
| `fastlane/metadata/android/en-US/images/phoneScreenshots/README.md` | Screenshot specifications and guidance |
| `scripts/capture-screenshots.sh` | Shell script for automated screenshot capture via adb |
| `docs/CONTRIBUTING.md` | Contribution guidelines, coding conventions, and PR checklist |
| `docs/adding-a-sound-set.md` | Guide for packaging and wiring new typewriter sound sets |
| `docs/release/RELEASING.md` | Step-by-step release process for maintainers |
| `docs/release/fdroid-submission.md` | F-Droid RFP metadata and submission template |
| `docs/qa/release-smoke.md` | 15-minute smoke test checklist for release builds |
| `docs/qa/1.0-acceptance.md` | Comprehensive 1.0 acceptance checklist, QA matrix, and gap ownership |

---

## 8. Hot Spots Review (Typing Path)

Conducted review across `WriterScreen`, `PaperText`, `Caret`, `Carriage`, `Grain`, and `LampLight`:
- **Draw loop allocations:** Zero per-frame object allocations. Reusable draw brushes and shapes are memoized via `remember` or defined outside draw scopes.
- **Text layout measurement:** `PaperText` memoizes measured characters in `charLayoutCache` per font and color, exploiting monospaced font uniformity.
- **Viewport culling:** `visibleLineRange()` culls offscreen lines, ensuring 3,000-character pages draw only visible cells.
- **Grain shader:** `Grain.tile()` uses a lazy pre-rendered `ImageBitmap` (tiled 200x200 ARGB_8888 bitmap with fixed seed 4), avoiding runtime Perlin noise generation.
- **Lamp & text mask:** `LampLight` uses basic geometric gradients; `lampTextMask` leverages GPU compositing (`CompositingStrategy.Offscreen`) with radial gradients.
- **Caret blink:** Handled via `infiniteRepeatable` with phase offset, completely suppressed (`alpha = 1f`) during typing and when reduce-motion is active.

---

## 9. Deviations and Gaps, Stated Plainly

1. **Physical Sound and Acoustic Latency:**
   - Unverified on hardware. `SoundPoolPlayer` dispatches asynchronously; true acoustic latency depends on device OEM audio HAL buffers.
   - Owner: Author (Device pass on real hardware).
2. **Physical Launcher Widget Integration:**
   - Unit tested via Glance test harness; widget appearance, resizing behavior, and interaction on OEM launchers (OneUI, Pixel Launcher, MIUI) unverified on phone.
   - Owner: Author (Device pass on real hardware).
3. **Compact Landscape Layout with Soft Keyboard:**
   - In extreme landscape with soft keyboard visible, paper height is constrained (~40 dp). Text survives rotation cleanly.
   - Owner: Author / Future UX iteration.
4. **1.12x Stretch of Pressed Bead and Fader Knobs:**
   - Graphics-layer scale transforms not measurable via Robolectric bounds assertions.
   - Owner: Author (Visual inspection on device).
5. **Application ID & Trademark Availability:**
   - `app.enk.notes` used as package ID; availability on Google Play / trademark registers unverified.
   - Owner: Author (Pre-release administrative task).
6. **Play Store Paid Features & Drive Sync:**
   - Deferred by design for 1.0 (anti-goal for F-Droid core).
   - Owner: Post-1.0 Roadmap (Separate closed extension module).

---

## 10. Clean Verification Gate Output

```
$env:JAVA_HOME = "D:\jdks\eclipse_adoptium-21-amd64-windows.2"; .\gradlew.bat clean build test :design:verifyPaparazziDebug :app:verifyPaparazziDebug :app:processDebugManifest :app:checkDebugAarMetadata :app:checkReleaseAarMetadata :app:assembleRelease :app:compileDebugAndroidTestKotlin

BUILD SUCCESSFUL in 12m 11s
421 actionable tasks: 396 executed, 25 up-to-date
```

### Verification Scripts Output

```
$ scripts/check-no-network.sh
Checking manifest: app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml
SUCCESS: No network permissions declared in merged manifest.

$ scripts/check-fdroid-rules.sh
OK
```

### Release Lint Status

```
$ .\gradlew.bat :app:lintRelease
BUILD SUCCESSFUL in 2m 7s
0 errors, 14 warnings, 2 hints
```
