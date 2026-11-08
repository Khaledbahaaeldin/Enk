# Third-Party Dependencies and Provenance

## Verified Libraries for Batch 2, Batch 3 & Batch 4

### 1. Backdrop
- **Coordinate:** `io.github.kyant0:backdrop:1.0.4`
- **Repository:** https://github.com/Kyant0/AndroidLiquidGlass
- **License:** Apache License, Version 2.0 (verified from `backdrop-1.0.4.pom`)
- **Min SDK:** API 21 (`minSdkVersion="21"` verified in AAR manifest; Enk `minSdk = 31`)
- **Target / Compile SDK Compatibility:**
  - Tested versions `2.0.1` and `1.0.5+`: Require `compileSdk >= 36` / `37` and AGP `9.1.0+` in AAR metadata, which fails AGP 8.13.2's `CheckAarMetadata` check under the pinned `compileSdk = 35`.
  - Tested version `1.0.4`: Passes `checkDebugAarMetadata` and `checkReleaseAarMetadata` cleanly with 0 issues under AGP `8.13.2` and `compileSdk = 35`.
- **Java:** Backdrop 1.0.4 classes are compiled to Java 21 bytecode, so the build and CI run on JDK 21 (the JVM screenshot tests load them). Enk's Android modules still emit Java 17 bytecode.
- **Glass Implementation Role:**
  - **API 33+ (Refraction):** Backdrop lens/refraction on top of its blur effect.
  - **API 31-32 (Blur):** Backdrop's blur effect on the captured backdrop layer without refraction.

### 2. AndroidX Room
- **Coordinates:**
  - `androidx.room:room-runtime:2.8.5`
  - `androidx.room:room-ktx:2.8.5`
  - `androidx.room:room-compiler:2.8.5`
- **Repository:** Google Maven (`dl.google.com/dl/android/maven2`)
- **License:** Apache License, Version 2.0
- **Version Verification:**
  - Google Maven release metadata lists `2.8.5` as current stable.
  - Tested under AGP `8.13.2` and `compileSdk = 35`: passes `checkDebugAarMetadata` and `checkReleaseAarMetadata` cleanly with 0 issues.
- **Role in Enk:** SQLite database abstraction, entity definitions, DAOs, schema generation, transaction management, and reactive Flow queries in `:data`. Added as `testImplementation` to `:app` (`androidx.room:room-runtime:2.8.5`) to support in-memory Room database instantiation during Robolectric testing (`WriterViewModelTest`).

### 3. KSP (Kotlin Symbol Processing)
- **Coordinate:** `com.google.devtools.ksp:2.3.13`
- **Repository:** Maven Central (`repo1.maven.org/maven2`)
- **License:** Apache License, Version 2.0
- **Version Verification:**
  - Maven Central release metadata lists `2.3.13` as the matching KSP 2 plugin release compatible with Kotlin 2.4.x toolchains.
  - Passes Gradle configuration and compilation cleanly.
- **Role in Enk:** Code generation for Room compiler without KAPT overhead.

### 4. AndroidX Preferences DataStore
- **Coordinate:** `androidx.datastore:datastore-preferences:1.3.0`
- **Repository:** Google Maven (`dl.google.com/dl/android/maven2`)
- **License:** Apache License, Version 2.0
- **Version Verification:**
  - Google Maven release metadata lists `1.3.0` as current stable release.
  - Tested under AGP `8.13.2` and `compileSdk = 35`: passes `checkDebugAarMetadata` and `checkReleaseAarMetadata` cleanly with 0 issues.
- **Role in Enk:** Asynchronous, transactional persistent preferences storage for app settings.

### 5. AndroidX Core, Activity Compose & Lifecycle (AAR Metadata Enforced)
- **Coordinates:**
  - `androidx.core:core-ktx:1.16.0`
  - `androidx.activity:activity-compose:1.10.1`
  - `androidx.lifecycle:lifecycle-runtime-ktx:2.10.0`
  - `androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0`
- **Repository:** Google Maven (`dl.google.com/dl/android/maven2`)
- **License:** Apache License, Version 2.0
- **Enforcement & Compatibility:**
  - `checkDebugAarMetadata` and `checkReleaseAarMetadata` were previously disabled in `:app` via `enabled = false` in `app/build.gradle.kts`. This workaround was removed in Step 0 of Batch 5.
  - Under `compileSdk = 35` and AGP `8.13.2`:
    - `androidx.activity:activity-compose:1.13.0` (and `1.12.x`, `1.11.x`) failed AAR checks because transitive dependencies (`androidx.activity:activity:1.11.0+`, `androidx.navigationevent:navigationevent-android:1.0.0+`) require `compileSdk >= 36`.
    - `androidx.lifecycle:2.11.0` failed AAR checks because it specifies `minCompileSdk = 37` and `minAndroidGradlePluginVersion = 9.1.0`.
    - `androidx.core:core-ktx:1.18.0` / `1.17.0` failed because `minCompileSdk = 36`.
  - Pinned newest stable versions compatible with `compileSdk = 35` and AGP `8.13.2`:
    - `core-ktx = 1.16.0` (`minCompileSdk = 35`, AGP `8.6.0`).
    - `activity-compose = 1.10.1` (`minCompileSdk = 35`, AGP `1.0.0`).
    - `lifecycle = 2.10.0` (`minCompileSdk = 35`, AGP `8.6.0`).
  - Verified: `lifecycle 2.10.0` contains `ViewModel`, `ViewModelStore`, and `viewModelScope` required by Task 14.
  - Verified: `:app:checkDebugAarMetadata` and `:app:checkReleaseAarMetadata` pass with 0 errors.

### 6. Kotlinx Coroutines Test
- **Coordinate:** `org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0`
- **Scope:** `testImplementation` only in `:app`.
- **License:** Apache License, Version 2.0
- **Version Verification:**
  - `1.11.0` is the newest stable on Maven Central (`maven-metadata.xml` `<release>`, checked 2026-11-09). It also lifts `kotlinx-coroutines-core` on the test classpath only.
  - Verified: `:app:checkDebugAarMetadata` and `:app:checkReleaseAarMetadata` pass with 0 errors.
- **Role in Enk:** Virtual time control (`runTest`, `advanceTimeBy`, `StandardTestDispatcher`) for `AutosaverTest` and `WriterViewModelTest`.

---

## Typography and Brand Fonts

### Inter (Wordmark Outlines)
- **Author:** Rasmus Andersson (https://github.com/rsms/inter)
- **License:** SIL Open Font License, Version 1.1 (OFL-1.1)
- **Role in Enk:** The wordmark "enk" is outlined directly from Inter Bold with tracking -4.5% and paired with the brand's orange caret bar. The full license text is preserved at `app/src/main/assets/licenses/OFL-Inter.txt`.

---

## Considered, Not Used

### Haze (`dev.chrisbanes.haze:haze`)
- **Reason Dropped:** Plan listed Haze, but testing demonstrated:
  - Haze `2.1.0`, `2.0.1`, `2.0.0` fail `checkDebugAarMetadata` and `checkReleaseAarMetadata` with 23 issues (require `compileSdk >= 37` and `AGP >= 9.1.0`).
  - Haze `1.7.0` to `1.7.3` fail with 3 to 22 issues (require `compileSdk >= 36` for transitive `androidx.activity:1.11.0`).
  - While Haze `1.6.10` passes, Backdrop 1.0.4's own blur natively covers the API 31-32 Blur path on the captured backdrop layer without needing an additional third-party library. Haze was therefore dropped from the catalog, build dependencies, and CREDITS.md to maintain a minimal, robust dependency footprint.

### 7. Paparazzi in `:app`: targetSdk workaround
- Paparazzi 1.3.5 bundles LayoutLib for Android API 34. Compose UI calls `View.setRequestedFrameRate` when the app's `targetSdkVersion` is 35 or higher, which does not exist there, so every snapshot would crash with `NoSuchMethodError`.
- `app/build.gradle.kts` therefore rewrites `"targetSdkVersion": "35"` to `"34"` only in Paparazzi's generated `build/intermediates/paparazzi/**/resources.json`, after the `preparePaparazzi*` tasks. The APK, lint and AAR checks still see `targetSdk = 35`.
- Remove this hook when a Paparazzi release bundles LayoutLib API 35 (toolchain upgrade batch).
- Paparazzi also cannot load `res/font` fonts: the writer goldens load `courier_prime_*.ttf` from `design/src/main/res/font` as a file font. The composables take a `fontFamily` parameter for this; the app uses the resource font.
