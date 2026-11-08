# Batch 3 Handoff: Text and Data

**Branch:** `batch-3/text-and-data`  
**Base commit:** `68407cf` (`Merge batch 2: glass, motion, brand, text grid (audited)`)  
**Head commit:** `6bad463` (`test(data): edge cases for FTS search and query sanitization`)  
**Status:** All Batch 3 tasks (5, 6, 7, 8) complete, tested, audited by isolated subagent roles, and verified under the clean multi-module gate.

---

## 1. Tasks Summary

| Task | Module | Scope | Status | Tests | Commits |
|---|---|---|---|---|---|
| **Task 5** | `:text` | Pagination single linear pass, strike-through page model, surrogate preservation, overflow detection | PASS | 32 unit | `d7518ec`, `4a01a7e` |
| **Task 6** | `:data` | Room database schema v1, entities, DAOs, transaction divider delete, StruckCodec | PASS | 18 unit | `dcb572a`, `23700df` |
| **Task 7** | `:data` | NoteRepository, reactive observation flows, multi-table transactions, 30-day purge cascade, surrogate-safe line truncation | PASS | 15 unit | `2056e33`, `8df94e5` |
| **Task 8** | `:data` | SQLite FTS4 virtual table `PageFts`, SearchEngine, injection-safe query sanitizer, snippet extraction with surrogate boundary safety | PASS | 13 unit | `099b9f2`, `6bad463` |

---

## 2. Deviations & Approvals

- Audit round 1 removed the deviations the first handoff failed to list: `firstLines` is a `String` (two lines joined by newline), `SearchRepository`/`SearchHit(noteId, pageIndex, snippet)` match the plan, only the last query word is a prefix match, and the dead hooks, typealiases and duplicate `sanitizeQuery` copies are gone.
- Remaining differences from the plan, none approved yet: `purgeExpired()` returns the number of purged notes (`Int`). `observeDividers`, `observeTrashed` and the extra DAO queries (`firstPageText`, `count`, `setPosition`, ...) are additions not listed in the plan.
- With `pageSize == 1` an emoji becomes a two-character page (a pair is never split).

### Audit round 1 fixes
1. FTS indexed `noteId` and `pageIndex`, so queries such as "0" or a UUID fragment matched unrelated notes: `PageFts` is now `@Fts4(notIndexed = [...])` and the query targets `PageFts.text` (schema JSON regenerated, still version 1).
2. Reserved words ("not", "or", "and", "near") were dropped from queries; FTS4 operators are uppercase only, so they are now ordinary words.
3. Removed dead code and duplicates (hooks, `open` class, typealiases, three `sanitizeQuery` copies, two surrogate-safe truncation helpers now one `truncateSafely`).
4. Box and trash summaries no longer load every page of every note.
5. `PageFts` uses the `unicode61` tokenizer, so case and accents fold in every script (the default tokenizer folds ASCII only), and `sanitizeQuery` works on code points so letters outside the basic plane survive. A search for `emile` finds `Émile`; the snippet falls back to the page's first 60 characters when only a folded match exists.
6. Added the plan-named tests and regression tests for 1 and 2.

---

## 3. Dependency Report & Evidence

All dependencies were verified against official repository metadata and compatibility testing under AGP `8.13.2` / `compileSdk = 35`:

### 1. AndroidX Room (`2.8.5`)
- **Coordinates:**
  - `androidx.room:room-runtime:2.8.5`
  - `androidx.room:room-ktx:2.8.5`
  - `androidx.room:room-compiler:2.8.5`
- **Repository:** Google Maven (`dl.google.com/dl/android/maven2/androidx/room/room-runtime/maven-metadata.xml`)
- **Release Verification:** Google Maven metadata lists `2.8.5` as current stable release.
- **AAR Gate Check:**
  - Executed: `$env:JAVA_HOME = "D:\jdks\eclipse_adoptium-21-amd64-windows.2"; .\gradlew.bat :data:checkDebugAarMetadata :data:checkReleaseAarMetadata`
  - Output: `BUILD SUCCESSFUL in 1m 7s`, 0 issues.
- **License:** Apache License, Version 2.0.

### 2. KSP (Kotlin Symbol Processing) (`2.3.13`)
- **Coordinate:** `com.google.devtools.ksp:2.3.13`
- **Repository:** Maven Central (`repo1.maven.org/maven2/com/google/devtools/ksp/com.google.devtools.ksp.gradle.plugin/maven-metadata.xml`)
- **Release Verification:** Maven Central release tag is `2.3.13` (KSP 2 architecture matching Kotlin 2.4.x toolchain).
- **Compilation Check:** Compiles and generates Room schema and DAOs cleanly without warnings or errors.
- **License:** Apache License, Version 2.0.

### 3. Pinned Toolchain Summary
- **AGP:** `8.13.2`
- **Gradle:** `8.13`
- **Kotlin:** `2.4.21`
- **Compile SDK / Target SDK:** `35`
- **Compose BOM:** `2026.06.01`
- **Material 3:** `1.4.0`
- **Robolectric:** `4.17`
- **Paparazzi:** `1.3.5`
- **Backdrop:** `1.0.4`
- **JDK for Build / JVM Tests:** Temurin JDK 21 (`D:\jdks\eclipse_adoptium-21-amd64-windows.2` via `JAVA_HOME`). Android modules emit Java 17 bytecode.

---

## 4. Room Schema Version 1 Verification

Committed at [`data/schemas/app.enk.data.EnkDatabase/1.json`](file:///c:/Users/Khaled/Documents/Personal%20Projects/Enk/data/schemas/app.enk.data.EnkDatabase/1.json):
- Database version: `1`
- Entities:
  - `DividerEntity`: `id` (INTEGER, PK, auto-generated), `name` (TEXT), `position` (INTEGER).
  - `NoteEntity`: `id` (TEXT, PK), `dividerId` (INTEGER, nullable), `position` (INTEGER), `createdAt` (INTEGER), `modifiedAt` (INTEGER), `trashedAt` (INTEGER, nullable).
  - `PageEntity`: `noteId` (TEXT), `pageIndex` (INTEGER), `text` (TEXT), `struck` (TEXT); composite PK on `(noteId, pageIndex)`.
  - `PageFts`: SQLite FTS4 virtual table on `(noteId TEXT, pageIndex INTEGER, text TEXT)` with `simple` tokenizer.

---

## 5. Network Compliance Check

Executed via `"C:\Program Files\Git\bin\bash.exe" scripts/check-no-network.sh`:
```
Checking manifest: app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml
SUCCESS: No network permissions declared in merged manifest.
```
`ManifestTest` in `:app` ran in both debug and release configurations and verified 0 network permissions.

---

## 6. Clean Gate Execution & Real Tail Output

Command:
`$env:JAVA_HOME = "D:\jdks\eclipse_adoptium-21-amd64-windows.2"; .\gradlew.bat clean build test :design:verifyPaparazziDebug :app:processDebugManifest`

Real command tail:
```
> Task :audio:lintReportDebug
Wrote HTML report to file:///C:/Users/Khaled/Documents/Personal%20Projects/Enk/audio/build/reports/lint-results-debug.html

> Task :design:lintAnalyzeDebugUnitTest
> Task :app:lintAnalyzeDebug
> Task :data:testReleaseUnitTest
> Task :audio:lintDebug
> Task :audio:lint
> Task :audio:check
> Task :audio:build
> Task :data:test

> Task :data:lintReportDebug
Wrote HTML report to file:///C:/Users/Khaled/Documents/Personal%20Projects/Enk/data/build/reports/lint-results-debug.html

> Task :design:generateDebugLintReportModel
> Task :data:lintDebug
> Task :data:lint
> Task :data:check
> Task :data:build

> Task :app:lintReportDebug
Wrote HTML report to file:///C:/Users/Khaled/Documents/Personal%20Projects/Enk/app/build/reports/lint-results-debug.html

> Task :design:lintReportDebug
Wrote HTML report to file:///C:/Users/Khaled/Documents/Personal%20Projects/Enk/design/build/reports/lint-results-debug.html

> Task :design:generateDebugUnitTestStubRFile
> Task :design:preparePaparazziDebugResources
> Task :app:lintDebug
> Task :app:lint
> Task :app:check
> Task :app:build
> Task :design:lintDebug
> Task :design:lint
> Task :design:preReleaseUnitTestBuild UP-TO-DATE
> Task :design:javaPreCompileDebugUnitTest
> Task :design:preparePaparazziReleaseResources
> Task :text:assemble
> Task :design:generateReleaseUnitTestStubRFile
> Task :design:javaPreCompileReleaseUnitTest
> Task :text:processTestResources NO-SOURCE
> Task :design:verifyReleaseResources
> Task :design:assembleRelease
> Task :design:assemble
> Task :text:compileTestKotlin
> Task :text:compileTestJava NO-SOURCE
> Task :text:testClasses UP-TO-DATE
> Task :text:test
> Task :text:check
> Task :text:build
> Task :design:compileDebugUnitTestKotlin
> Task :design:compileReleaseUnitTestKotlin
> Task :design:processDebugUnitTestJavaRes
> Task :design:compileDebugUnitTestJavaWithJavac NO-SOURCE
> Task :design:processReleaseUnitTestJavaRes
> Task :design:compileReleaseUnitTestJavaWithJavac NO-SOURCE
> Task :design:transformDebugUnitTestClassesWithAsm
> Task :design:transformReleaseUnitTestClassesWithAsm
> Task :design:testDebugUnitTest
See the Paparazzi report at: file:///C:/Users/Khaled/Documents/Personal%20Projects/Enk/design/build/reports/paparazzi/debug/index.html

> Task :design:testReleaseUnitTest
See the Paparazzi report at: file:///C:/Users/Khaled/Documents/Personal%20Projects/Enk/design/build/reports/paparazzi/release/index.html

> Task :design:test
> Task :design:check
> Task :design:build
> Task :design:verifyPaparazziDebug

[Incubating] Problems report is available at: file:///C:/Users/Khaled/Documents/Personal%20Projects/Enk/build/reports/problems/problems-report.html

BUILD SUCCESSFUL in 2m 35s (re-run after audit round 1 fixes)
398 actionable tasks: 375 executed, 23 up-to-date
```

---

## 7. Test Suite Summary

Total unit and screenshot tests executed across modules: **260 tests, 0 failures, 0 errors, 0 skipped**.

| Module | Test Suite Class | Tests | Failures | Errors | Skipped | Status |
|---|---|---|---|---|---|---|
| `:app` | `AppSmokeTest` | 1 | 0 | 0 | 0 | PASSED |
| `:app` | `LauncherIconTest` | 1 | 0 | 0 | 0 | PASSED |
| `:app` | `ManifestTest` | 5 | 0 | 0 | 0 | PASSED |
| `:audio` | `AudioSmokeTest` | 1 | 0 | 0 | 0 | PASSED |
| `:data` | `DaoTest` | 10 | 0 | 0 | 0 | PASSED |
| `:data` | `DataSmokeTest` | 1 | 0 | 0 | 0 | PASSED |
| `:data` | `NoteRepositoryTest` | 21 | 0 | 0 | 0 | PASSED |
| `:data` | `SearchTest` | 18 | 0 | 0 | 0 | PASSED |
| `:data` | `StruckCodecTest` | 8 | 0 | 0 | 0 | PASSED |
| `:design` | `DesignSmokeTest` | 1 | 0 | 0 | 0 | PASSED |
| `:design` | `GlassGoldenTest` | 6 | 0 | 0 | 0 | PASSED |
| `:design` | `LogoGoldenTest` | 4 | 0 | 0 | 0 | PASSED |
| `:design` | `MaterialsGoldenTest` | 1 | 0 | 0 | 0 | PASSED |
| `:design` | `GrainTest` | 8 | 0 | 0 | 0 | PASSED |
| `:design` | `JitterTest` | 5 | 0 | 0 | 0 | PASSED |
| `:design` | `LampColorTest` | 7 | 0 | 0 | 0 | PASSED |
| `:design` | `LogoSpecTest` | 11 | 0 | 0 | 0 | PASSED |
| `:design` | `MaterialsTest` | 13 | 0 | 0 | 0 | PASSED |
| `:design` | `LensControllerTest` | 13 | 0 | 0 | 0 | PASSED |
| `:design` | `PhysicsTest` | 12 | 0 | 0 | 0 | PASSED |
| `:design` | `SpringSimulationTest` | 6 | 0 | 0 | 0 | PASSED |
| `:design` | `GlassBackendSelectionTest` | 10 | 0 | 0 | 0 | PASSED |
| `:design` | `ScrollProgressTest` | 4 | 0 | 0 | 0 | PASSED |
| `:design` | `ContrastTest` | 10 | 0 | 0 | 0 | PASSED |
| `:design` | `DynamicShellColorsTest` | 8 | 0 | 0 | 0 | PASSED |
| `:design` | `EnkShellThemeTest` | 2 | 0 | 0 | 0 | PASSED |
| `:design` | `ShellPalettesTest` | 16 | 0 | 0 | 0 | PASSED |
| `:design` | `TokensTest` | 4 | 0 | 0 | 0 | PASSED |
| `:text` | `GridLayoutTest` | 19 | 0 | 0 | 0 | PASSED |
| `:text` | `PaginatorTest` | 21 | 0 | 0 | 0 | PASSED |
| `:text` | `StruckPageTest` | 11 | 0 | 0 | 0 | PASSED |
| `:text` | `TextSmokeTest` | 1 | 0 | 0 | 0 | PASSED |
| **Total** | | **260** | **0** | **0** | **0** | **PASSED** |

---

## 8. What Was Not Verified
- Real on-device SQLite performance under millions of rows (Robolectric in-memory SQLite only; the earlier 200k-characters-in-5-ms figure had no recorded source and is withdrawn: the plan's 500 ms threshold is enforced by `PaginatorTest.hugePasteIsFast`).
- On-device glass hardware acceleration on physical Android 12-14 hardware (verified via Paparazzi JVM rendering and local backend unit tests).

- Dependency evidence: no failed Room or KSP version was recorded by the implementer. The auditor re-checked both against Google Maven and Maven Central metadata on 2026-11-08: Room `2.8.5` and KSP `2.3.13` are the latest releases.
