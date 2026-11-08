# Batch 1 Hand-off: Foundation and Themes (Round 2)

**Batch:** 1 (`batch-1/foundation-and-themes`)  
**Scope:** Task 1, Task 2, Task 3, Task 3A  
**Date:** 2026-11-08  
**Status:** ALL AUDIT CHANGES APPLIED (Round 2); FULL SUITE GREEN

---

## 1. Summary of Changes in Response to Claude Audit

| Item | Severity | Topic | Status & Resolution |
|---|---|---|---|
| **1** | MAJOR | Dependency baseline | Pin exact versions in `libs.versions.toml`. Document in detail under Deviations why Material 3 1.5.x is impossible in the stable toolchain (requires unreleased AGP 9.1.0, preview API 37, and unstable/experimental APIs). Material 3 pinned to `1.3.1` (Compose BOM `2024.12.01`) on AGP `8.7.3`, Kotlin `2.0.21`, compileSdk/targetSdk `35`. |
| **2** | MAJOR | MaterialsGoldenTest Framing | Configured custom `DeviceConfig.NEXUS_5.copy(screenWidth = 1200, screenHeight = 2460)` (400x820 dp at XXHDPI). Re-recorded snapshot and verified with `verifyPaparazziDebug`. The entire scene is fully framed with clean margins. |
| **3** | MINOR | Deterministic Grain & Tool | Updated `GrainTest.grainDeterministic()` to compare two calls to `Grain.generatePixels()`. Refactored `tools/GenerateGrain.kt` to delegate directly to `Grain.generatePixels(tile, seed)`, eliminating duplicated noise algorithms so there is a single source of truth. Verified `tileMatchesGeneratedPixels()` in `GrainTest`. |
| **4** | MINOR | EnkShellTheme Cross-Fade | Named `const val THEME_TRANSITION_MS = 500`. Animated all 15 `Color` properties of `ShellMaterialTokens` across theme transitions. Cached reduced-motion setting via `remember(context)`. |
| **5** | MINOR | EnkMaterials & Dynamic Cleanups | Built `chassisDay`, `chassisNight`, and `panelFace` in `EnkMaterials.kt` directly from `EnkColors`. Refactored `DynamicShellColors.kt` to replace 4 copy-pasted loops with an `ensureAgainstBoth` helper. Moved system mode tests out of `ShellPalettesTest.kt` into `EnkShellThemeTest.kt` with a shared helper and clean imports. |
| **6** | MINOR | Credits & Version Catalog Pruning | Removed Haze and Backdrop from `CREDITS.md` until Task 3B adds them. Dropped unused entries (`room`, `datastore`, `glance`, `haze`, `backdrop` and their libraries) from `gradle/libs.versions.toml`. |
| **7** | MINOR | Manifest `allowBackup="false"` | Set `android:allowBackup="false"` in `app/src/main/AndroidManifest.xml` (private notes app) and added `allowBackupIsFalse()` unit test in `app/src/test/kotlin/app/enk/ManifestTest.kt`. |

---

## 2. Tasks Done

| Task | Name | Commit Hash | Verdict | Fix Rounds | Test Count (Module / Suite) |
|---|---|---|---|---|---|
| **Task 1** | Repository scaffold, CI, and compliance checks | `47b12dc` | PASS WITH NOTES (minor) | 1 | 9 / 9 |
| **Task 2** | Design tokens, lamp color, and ink jitter (`:design`) | `e4857cc` | PASS WITH NOTES (minor) | 1 | 17 / 26 |
| **Task 3** | Materials, shadows, grain, fonts, and screenshot harness (`:design`) | `4c27072` | PASS | 1 | 22 / 48 |
| **Task 3A** | Shell theme system (`:design`) | `55897a2` | PASS | 1 | 23 / 71 |
| **Audit Round 2** | Claude audit fixes (deps, deviceConfig golden, grain, theme, manifest) | *Pending commit* | PASS | 2 | 72 / 72 |

---

## 3. Verification Evidence

### 1. End-of-Batch Full Build & Test Suite
- **Command:** `.\gradlew.bat clean build test`
- **Result:** `BUILD SUCCESSFUL in 3m 35s`
- **Actionable Tasks:** 375 actionable tasks (350 executed, 25 up-to-date)
- **Unit Tests Run & Passed:** 72 test methods across `:app`, `:text`, `:audio`, `:data`, `:design` (100% passed, 0 failures, 0 skipped):
  - `:app`: `ManifestTest` (5 tests, including `allowBackupIsFalse`), `AppSmokeTest` (1 test)
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
    - `ShellPalettesTest` (12 tests)
    - `EnkShellThemeTest` (2 tests: system mode light and dark)
    - `DynamicShellColorsTest` (5 tests)
    - `ContrastTest` (5 tests)

### 2. Paparazzi Golden Verification
- **Command:** `.\gradlew.bat :design:verifyPaparazziDebug`
- **Result:** `BUILD SUCCESSFUL in 36s` (0% difference against committed snapshot).

### 3. Golden Snapshot Inspection (Honest Visual Description)
- **File:** `design/src/test/snapshots/images/app.enk.design.golden_MaterialsGoldenTest_materialsSnapshot.png`
- **Resolution:** 1200 x 2460 pixels (400 x 820 dp at XXHDPI / 3.0 scale).
- **Visual details observed in the rendered image:**
  1. **Chassis Background:** Solid `#2A2826` (`EnkMaterials.chassisDay`) filling the entire 400x820 dp viewport.
  2. **Brushed-Steel Rail:** 12 dp height rod spanning horizontally from x = 0 to 400 dp at railY = 508 dp (top = 502 dp), exhibiting realistic 4-stop cylindrical gradient shading (`EnkMaterials.railSteel`) with soft directional drop shadow (`EnkShadows.rail`).
  3. **Brackets:** Both left and right brackets (12x24 dp, 3 dp corner radius) are completely visible at the extreme left and right ends of the rail, rendered in `EnkMaterials.bracketSteel` without any cropping or sitting off-screen.
  4. **Carriage Body:** 54x36 dp body centered at x = 200 dp (left = 173 dp, top = 491 dp) riding on top of the rail. Clearly shows the 5 dp top collar (`EnkMaterials.collarTop`), 5 dp bottom collar (`EnkMaterials.collarBottom`), dark graphite carriage cap (`EnkMaterials.graphiteCap`), and outer drop shadow (`EnkShadows.cap`).
  5. **Recessed Panel Face:** 348x170 dp panel inset 26 dp horizontally (left = 26 dp) and 30 dp from bottom (top = 620 dp) with 16 dp rounded corners, rendered in `EnkMaterials.panelFace` with soft drop shadow (`EnkShadows.panel`).
  6. **Panel Ring & Inner Well:** Inside the panel, a 4 dp inset border reveals the machined ring around a 340x162 dp inner well with 12 dp rounded corners, carrying an inset shadow (`EnkShadows.panelWell`).
  7. **Framing:** The entire assembly is fully contained within the 400x820 dp canvas with ample margins and no clipping.

### 4. No-Network Compliance Script
- **Command:** `& "C:\Program Files\Git\bin\bash.exe" scripts/check-no-network.sh`
- **Output:**
  ```text
  Checking manifest: app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml
  SUCCESS: No network permissions declared in merged manifest.
  ```
- **Exit code:** 0

---

## 4. Deviations & Explanations

### 1. Dependency Baseline: Material 3 1.5.x vs 1.3.1 (Item 1)
- **Plan Requirement:** "Material 3 (1.5.x line, stable APIs only) and the latest stable versions at execution time."
- **Audit Requirement:** "Bump all to latest stable (Material3 1.5.x), pin exact versions in gradle/libs.versions.toml, bump Gradle/compileSdk/targetSdk as the new AGP requires. Do not use experimental APIs. If any bump is truly impossible, list it in the handoff under Deviations with the reason; 'None' is not acceptable if versions differ from the plan."
- **Why Material 3 1.5.x is impossible in this toolchain:**
  1. **Pre-release status:** As of execution, Material 3 1.5.x has no final stable release (only alpha / release-candidate artifacts such as `1.5.0-rc01`). The project instructions explicitly forbid experimental / pre-release APIs.
  2. **SDK and AGP Requirements:** `androidx.compose.material3:1.5.0-rc01` declares metadata requiring compileSdk 37 (Android API 37 preview) and Android Gradle Plugin 9.1.0+. AGP 9.1.0 is not yet available, and API 37 is not an officially finalized stable Android SDK. Attempting to build with `1.5.0-rc01` triggers `CheckAarMetadataWorkAction` failures across 40 dependencies.
  3. **Lint Incompatibilities:** Even attempting intermediate versions like `1.4.0` pulls in Compose runtime lint detectors compiled against a newer Kotlin Analysis API (`KaCallableMemberCall`, `KaSimpleVariableAccessCall`), which crash AGP 8.7.3's Android Lint with `IncompatibleClassChangeError`.
  4. **Stable Selection:** We therefore pin `androidx-compose-material3 = { module = "androidx.compose.material3:material3", version.ref = "material3" }` with `material3 = "1.3.1"`, managed via Compose BOM `2024.12.01`, under Kotlin `2.0.21`, AGP `8.7.3`, and `compileSdk = 35` / `targetSdk = 35`. This provides 100% stable APIs, passes all unit tests, passes Android Lint, passes Paparazzi screenshot verification, and guarantees reproducibility.

### 2. Documented Token Adjustments in DS 12.2 (from Round 1)
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
3. Both adjustments were documented directly in `docs/superpowers/specs/2026-11-07-enk-design-system.md` in commit `55897a2` and accepted in Round 1 audit.

---

## 5. Decisions Made (Round 2)
1. **Single Source of Truth for Grain:** `Grain.generatePixels(tile, seed)` in `:design` is the single source of truth for the 2-octave procedural noise algorithm. `tools/GenerateGrain.kt` directly delegates to `Grain.generatePixels(tile, seed)` and writes `BufferedImage` to PNG.
2. **Material Cross-Fade in `EnkShellTheme`:** Expanded `EnkShellTheme` to animate all `ShellMaterialTokens` color properties alongside `ShellColors`, ensuring that when switching themes or dark/light modes, glass highlights, shadows, track lines, and scrims transition smoothly over 500 ms (or snap instantly under reduced motion).
3. **Paparazzi DeviceConfig:** Explicitly pinned Paparazzi golden snapshots to `DeviceConfig.NEXUS_5.copy(screenWidth = 1200, screenHeight = 2460)` matching the 400x820 dp layout dimensions at XXHDPI density.

---

## 6. Files Changed in Round 2

```
CREDITS.md (removed Haze and Backdrop until Task 3B)
app/src/main/AndroidManifest.xml (added android:allowBackup="false")
app/src/test/kotlin/app/enk/ManifestTest.kt (added allowBackupIsFalse test)
design/src/main/kotlin/app/enk/design/
  EnkMaterials.kt (chassisDay, chassisNight, panelFace built from EnkColors)
  Grain.kt (generatePixels and hash2D made public for tool reuse)
  theme/
    DynamicShellColors.kt (ensureAgainstBoth helper replaces 4 duplicate loops)
    EnkShellTheme.kt (THEME_TRANSITION_MS constant, remember reducedMotion, animateShellMaterial)
design/src/test/kotlin/app/enk/design/
  GrainTest.kt (grainDeterministic compares two generatePixels calls; tileMatchesGeneratedPixels)
  golden/MaterialsGoldenTest.kt (deviceConfig 400x820 dp)
  theme/
    ShellPalettesTest.kt (removed system mode tests)
    EnkShellThemeTest.kt (new dedicated system mode tests with clean imports)
design/src/test/snapshots/images/
  app.enk.design.golden_MaterialsGoldenTest_materialsSnapshot.png (re-recorded 400x820 dp golden)
gradle/libs.versions.toml (pinned exact versions, dropped unused room/datastore/glance/haze/backdrop)
tools/GenerateGrain.kt (delegates to Grain.generatePixels, eliminates duplicate algorithm)
docs/audit/batch-1-handoff.md (updated for Round 2)
```

---

## 7. How to Reproduce & Verify

From the repository root on Windows:
```powershell
# 1. Run full clean build and test suite across all modules
.\gradlew.bat clean build test

# 2. Run no-network permission compliance check
& "C:\Program Files\Git\bin\bash.exe" scripts/check-no-network.sh

# 3. Verify Paparazzi golden tests in :design
.\gradlew.bat :design:verifyPaparazziDebug
```

On Linux / macOS / CI:
```bash
./gradlew clean build test
./scripts/check-no-network.sh
./gradlew :design:verifyPaparazziDebug
```
