# Contributing to Enk

Thank you for contributing to Enk. Enk is a focused, offline typewriter notebook built with extreme care for typography, tactile acoustics, physical motion, and complete user privacy.

## Development Setup

1. **JDK**: JDK 21 is required (`JAVA_HOME` pointing to JDK 21).
2. **Android SDK**: Compile SDK 35, Min SDK 31.
3. **Toolchain**: AGP 8.13.2, Kotlin 2.4.21, Gradle 8.13.

## Architectural Boundaries

- `:text`: Pure Kotlin JVM module. Implements grid geometry, pagination, and struck-text representation. Must never depend on Android or Compose libraries.
- `:design`: Design tokens, colors, glass materials, shaders, and physics animations. Must never depend on `:app` or `:data`.
- `:audio`: Sound sets, audio manifests, and sound pool playback.
- `:data`: Room database (FTS4), DataStore persistence, and note repositories.
- `:app`: Android application container, Jetpack Compose screens, navigation, and Glance widgets.

## Core Invariants

1. **Zero Network Permissions**:
   - Enk never connects to the internet. Do not add `INTERNET`, `ACCESS_NETWORK_STATE`, or any network-related permissions.
   - Any transitive dependency that requests network permissions must have them stripped via `tools:node="remove"` in `app/src/main/AndroidManifest.xml`.
2. **Strict Color Palette**:
   - The brand orange accent (`#FF5A1F`) is reserved exclusively for:
     1. The text caret
     2. The carriage marker strip
     3. The fader knob index line
     4. The Enk wordmark dot
   - All other colors must use tokens defined in `EnkColors` (writer) or `ShellPalettes` (shell).
3. **Asset Provenance**:
   - Every audio file added under `app/src/main/assets/sounds/` must have a row in `docs/provenance.csv` with a CC0/free license and an archived source in `docs/archive/`.
   - Every font or artwork asset must be documented in `docs/provenance-other.csv`.
4. **No Test Hooks in Production Code**:
   - Never expose public flags, test-only constructors, or mocks inside production classes.

## Pull Request Verification

Before opening a pull request, run the full verification gate:
```bash
# 1. Full clean build and test suite
./gradlew clean build test :design:verifyPaparazziDebug :app:verifyPaparazziDebug \
  :app:processDebugManifest :app:checkDebugAarMetadata :app:checkReleaseAarMetadata \
  :app:assembleRelease :app:compileDebugAndroidTestKotlin

# 2. Check no network permissions
bash scripts/check-no-network.sh

# 3. Check F-Droid compliance and dependencies
bash scripts/check-fdroid-rules.sh
```
All commands must pass cleanly with zero errors.
