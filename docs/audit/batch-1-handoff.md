# Batch 1 Hand-off: Foundation and Themes

**Batch:** 1 (`batch-1/foundation-and-themes`)  
**Scope:** Task 1, Task 2, Task 3, Task 3A  
**Date:** 2026-11-08  
**Status:** ALL 4 TASKS PASSED INTERNAL AUDIT; FULL TEST SUITE GREEN

---

## 1. Tasks Done

| Task | Name | Commit Hash | Verdict | Fix Rounds | Test Count (Module / Suite) |
|---|---|---|---|---|---|
| **Task 1** | Repository scaffold, CI, and compliance checks | `47b12dc` | PASS WITH NOTES (minor) | 1 | 9 / 9 |
| **Task 2** | Design tokens, lamp color, and ink jitter (`:design`) | `e4857cc` | PASS WITH NOTES (minor) | 1 | 17 / 26 |
| **Task 3** | Materials, shadows, grain, fonts, and screenshot harness (`:design`) | `4c27072` | PASS | 1 | 22 / 48 |
| **Task 3A** | Shell theme system (`:design`) | `55897a2` | PASS | 1 | 23 / 71 |

---

## 2. Verification Evidence

### End-of-Batch Full Build & Test Suite
- **Command:** `.\gradlew.bat build test`
- **Result:** `BUILD SUCCESSFUL in 1m 28s`
- **Actionable Tasks:** 370 actionable tasks (190 executed, 180 up-to-date)
- **Unit Tests Run & Passed:** 71 test methods across `:app`, `:text`, `:audio`, `:data`, `:design` (100% passed, 0 failures, 0 skipped)
  - `:app`: `ManifestTest` (4 tests), `AppSmokeTest` (1 test)
  - `:text`: `TextSmokeTest` (1 test)
  - `:audio`: `AudioSmokeTest` (1 test)
  - `:data`: `DataSmokeTest` (1 test)
  - `:design`: 63 tests:
    - `DesignSmokeTest` (1 test)
    - `LampColorTest` (7 tests)
    - `JitterTest` (5 tests)
    - `TokensTest` (4 tests)
    - `MaterialsTest` (13 tests)
    - `GrainTest` (8 tests)
    - `MaterialsGoldenTest` (1 Paparazzi screenshot test, maxPercentDifference = 2.0)
    - `ShellPalettesTest` (14 tests)
    - `DynamicShellColorsTest` (5 tests)
    - `ContrastTest` (4 tests)

### No-Network Compliance Script
- **Command:** `"C:\Program Files\Git\bin\bash.exe" scripts/check-no-network.sh`
- **Output:**
  ```text
  Checking manifest: app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml
  SUCCESS: No network permissions declared in merged manifest.
  ```
- **Exit code:** 0

### Intentional Negative Permission Verification (Task 1)
- Injected `<uses-permission android:name="android.permission.INTERNET" />` into debug manifest:
  - `scripts/check-no-network.sh` exited 1 with `Network permission check FAILED (1 violations found).`
  - `:app:testDebugUnitTest --tests "app.enk.ManifestTest"` failed with `AssertionError`.
  - Reverted injection, restored clean state, and verified both passed cleanly.

### Golden Screenshot Verification (Task 3)
- Recorded once with `.\gradlew.bat :design:recordPaparazziDebug`: `design/src/test/snapshots/images/app.enk.design.golden_MaterialsGoldenTest_materialsSnapshot.png`.
- Visual comparison with `spike-throwaway/marker-prototype-v2.html` confirmed exact cylinder shading on rail rod, brushed-steel brackets, graphite carriage body placeholder, and panel face with outer ring and inner recessed well.

---

## 3. Deviations

**None from specifications or rules.**

Per the explicit instruction in Task 3A:
> "If a static combo fails a contrast minimum, change only the failing token by the smallest amount that passes, update DS section 12.2 in the same commit, and flag it prominently in the handoff. Never loosen the test or the minimums."

**Documented Token Adjustments in DS 12.2 (Commit `55897a2`):**
During contrast evaluation against WCAG minimums (DS 12.11):
1. **Light Mode `t3` (Hint text):**
   - The initial draft values in DS 12.2 failed the $\ge 3.0:1$ requirement on light backgrounds (`bg`):
     - Graphite: `#9D978B` on `#ECE8DF` was 2.37:1 $\to$ minimally darkened to `#8A8375` (3.08:1)
     - Apricot: `#B08877` on `#FFEBDC` was 2.74:1 $\to$ minimally darkened to `#A97D6A` (3.12:1)
     - Sky: `#A8948F` on `#FFF6D4` was 2.66:1 $\to$ minimally darkened to `#9F8984` (3.04:1)
     - Slate: `#8B949B` on `#E5E4E2` was 2.43:1 $\to$ minimally darkened to `#758088` (3.18:1)
     - Mono: `#A0A0A0` on `#EEEEEE` was 2.25:1 $\to$ minimally darkened to `#868686` (3.14:1)
2. **Light Mode `uiInk` (Text on accent):**
   - The initial draft value (`#FFFFFF`) on mid-tone orange/peach/blue light accents failed the $\ge 4.5:1$ requirement:
     - Graphite: White on `#EE5416` was 3.55:1 $\to$ set to theme dark ink `#1A0A03` (5.43:1)
     - Apricot: White on `#D4634C` was 3.69:1 $\to$ set to theme dark ink `#2A1410` (4.72:1)
     - Sky: White on `#5E92B2` was 3.37:1 $\to$ set to theme dark ink `#1B1413` (5.39:1)
     - Slate (`#536878`) and Mono (`#111111`) already exceeded 4.5:1 with White and remained `#FFFFFF`.
3. Both adjustments were documented directly in `docs/superpowers/specs/2026-11-07-enk-design-system.md` in commit `55897a2`.

---

## 4. Decisions Made
1. **Dependency Versions Pinned (`gradle/libs.versions.toml`):**
   - Android Gradle Plugin: `8.7.3`
   - Kotlin: `2.0.21` (with Compose compiler Gradle plugin `org.jetbrains.kotlin.plugin.compose`)
   - Compose BOM: `2024.12.01`
   - Material 3: `1.3.1` (stable line)
   - Paparazzi: `1.3.5`
   - JUnit: `4.13.2`
   - Robolectric: `4.14.1`
2. **Deterministic Grain Implementation:**
   - Standalone generator script `tools/GenerateGrain.kt` and runtime `Grain.tile()` share the identical 2-octave value noise algorithm (base frequency 0.9, seed 4, tint `#736650`, alpha `0.5 * noise`).
3. **Fonts & Licenses:**
   - Bundled official Google Fonts release of Courier Prime (Regular and Bold) under SIL Open Font License 1.1. License text placed in `app/src/main/assets/licenses/OFL-CourierPrime.txt` and attributed in `CREDITS.md`.

---

## 5. Open Questions & Risks
- **Paparazzi Windows vs. Linux CI Rendering:** Goldens were recorded locally on Windows 11 with `maxPercentDifference = 2.0`. On GitHub Actions (Ubuntu runner), font anti-aliasing can have minor subpixel variations. The 2.0% threshold is configured to accommodate minor font rendering variances across platforms without loosening design standards.

---

## 6. Files Changed (Summary)

```
.gitattributes
.github/workflows/ci.yml
.gitignore
CREDITS.md
LICENSE
README.md
app/
  build.gradle.kts
  src/main/AndroidManifest.xml
  src/main/assets/licenses/OFL-CourierPrime.txt
  src/main/kotlin/app/enk/MainActivity.kt
  src/test/kotlin/app/enk/AppSmokeTest.kt
  src/test/kotlin/app/enk/ManifestTest.kt
audio/
  build.gradle.kts
  consumer-rules.pro
  src/test/kotlin/app/enk/audio/AudioSmokeTest.kt
data/
  build.gradle.kts
  consumer-rules.pro
  src/test/kotlin/app/enk/data/DataSmokeTest.kt
design/
  build.gradle.kts
  consumer-rules.pro
  src/main/kotlin/app/enk/design/
    EnkColors.kt
    EnkMaterials.kt
    EnkMotion.kt
    EnkShadows.kt
    EnkType.kt
    Grain.kt
    Jitter.kt
    LampColor.kt
    theme/
      Contrast.kt
      DynamicShellColors.kt
      EnkShellTheme.kt
      ShellColors.kt
      ShellMaterialTokens.kt
      ShellPalettes.kt
      ThemeId.kt
  src/main/res/font/
    courier_prime_bold.ttf
    courier_prime_regular.ttf
  src/test/kotlin/app/enk/design/
    DesignSmokeTest.kt
    GrainTest.kt
    JitterTest.kt
    LampColorTest.kt
    MaterialsTest.kt
    TokensTest.kt
    golden/
      MaterialsGoldenTest.kt
    theme/
      ContrastTest.kt
      DynamicShellColorsTest.kt
      ShellPalettesTest.kt
  src/test/snapshots/images/
    app.enk.design.golden_MaterialsGoldenTest_materialsSnapshot.png
gradle/
  libs.versions.toml
  wrapper/
    gradle-wrapper.jar
    gradle-wrapper.properties
gradle.properties
gradlew
gradlew.bat
scripts/
  check-no-network.sh
settings.gradle.kts
text/
  build.gradle.kts
  src/test/kotlin/app/enk/text/TextSmokeTest.kt
tools/
  GenerateGrain.kt
docs/superpowers/plans/2026-11-07-enk-1-0-plan.md (checked off Tasks 1, 2, 3, 3A)
docs/superpowers/specs/2026-11-07-enk-design-system.md (updated DS 12.2 Light mode contrast tokens)
```

---

## 7. How to Reproduce & Verify

From the repository root on Windows:
```powershell
# 1. Run full build and test suite across all modules
.\gradlew.bat build test

# 2. Run no-network permission compliance check
& "C:\Program Files\Git\bin\bash.exe" scripts/check-no-network.sh

# 3. Verify Paparazzi golden tests in :design
.\gradlew.bat :design:verifyPaparazziDebug
```

On Linux / macOS / CI:
```bash
./gradlew build test
./scripts/check-no-network.sh
```
