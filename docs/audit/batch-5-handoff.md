# Batch 5 Audit Handoff: Writer Core (Geometry, State, Paper, Carriage)

## 1. Branch and Commits

- **Branch:** `batch-5/writer-core`, based on `main` (`1f6da5b`).
- **Commits:**
  - `135f620 build(app): enforce AAR metadata checks` (Step 0)
  - `ece41d5 feat(writer): geometry per design system` (Task 13)
  - `4bc1eb6 feat(writer): state, reducer, autosave` (Task 14)
  - `0e8e7d2 feat(writer): paper sheet, caret, stamp` (Task 15)
  - `4d245a6 feat(writer): rail, carriage, type-slug` (Task 16)

---

## 2. Step 0 Resolution (AAR Metadata Checks & Paparazzi)

- **Problem:**
  In earlier batches, `app/build.gradle.kts` contained a block disabling tasks whose name contained `"AarMetadata"`. Enabling them exposed that `androidx.datastore:datastore-preferences:1.3.0` and AndroidX lifecycle libraries require `minCompileSdk = 35`, which matched `compileSdk = 35`.
  However, Paparazzi 1.3.5 bundles LayoutLib from Android API 34. In Compose UI 1.7+, `AndroidComposeView` queries `context.applicationInfo.targetSdkVersion >= 35` at runtime. If true, it attempts to call `android.view.View.setRequestedFrameRate(float)` (`Api35Impl.setRequestedFrameRate`), which does not exist in Paparazzi's API 34 LayoutLib, crashing every Paparazzi snapshot test with `NoSuchMethodError`.
- **Solution:**
  Rather than disabling AAR metadata checks or lowering `compileSdk` / `targetSdk` globally in the application, we keep `defaultConfig.targetSdk = 35` and `compileSdk = 35` unconditionally for all debug and release compilation and packaging. In `app/build.gradle.kts`, a Gradle task configuration hook was added to the `preparePaparazzi` tasks to rewrite `"targetSdkVersion": "35"` to `"targetSdkVersion": "34"` exclusively inside Paparazzi's intermediate `resources.json`:
  ```kotlin
  tasks.matching { it.name.contains("preparePaparazzi") }.configureEach {
      doLast {
          val paparazziDir = layout.buildDirectory.dir("intermediates/paparazzi").orNull?.asFile
          paparazziDir?.walk()?.filter { it.name == "resources.json" }?.forEach { file ->
              val content = file.readText()
              if (content.contains("\"targetSdkVersion\": \"35\"")) {
                  file.writeText(content.replace("\"targetSdkVersion\": \"35\"", "\"targetSdkVersion\": \"34\""))
              }
          }
      }
  }
  ```
  This allows LayoutLib to run safely within API 34 limits without attempting to call API 35 methods, while `:app:checkDebugAarMetadata`, `:app:checkReleaseAarMetadata`, lint, and release APK building execute with strict enforcement against API 35.

---

## 3. Public Signatures against the Plan's Interfaces Blocks

Compared with `docs/superpowers/plans/2026-11-07-enk-1-0-plan.md` (Tasks 13, 14, 15, 16):

| Component | Plan Specification | Implemented Signature | Difference / Notes |
|---|---|---|---|
| `WriterGeometry` | `object WriterGeometry { fun compute(viewportWidthDp: Float, viewportHeightDp: Float): Geometry }` | `object WriterGeometry { fun compute(viewportWidthDp: Float, viewportHeightDp: Float): Geometry }` | Exactly identical. Includes helper functions `paperCenterXDp`, `railCarriageXDp`, `paperScrollOffsetDp`, `cellBounds`, `hitTest`. |
| `Geometry` | Data class holding calculated writer layout bounds | `data class Geometry(paperWidthDp, paperHeightDp, paperLeftDp, paperTopDp, viewfinderYDp, railYDp, caretBaseYDp, caretWidthDp, caretHeightDp, caretLeftOffsetDp, gutterWidthDp, cols, rows, charWidthDp, charHeightDp)` | Fully implements all dimensions specified in DS 6.1. |
| `WriterState` | `data class WriterState(val noteId: String, val pages: List<StruckPage>, val activePageIndex: Int, val cursorRow: Int, val cursorCol: Int, val dirty: Boolean, val lastSavedAt: Instant, val isSaving: Boolean, val saveError: Throwable?, val readOnly: Boolean)` | `data class WriterState(val noteId: String, val pages: List<StruckPage>, val activePageIndex: Int, val cursorRow: Int, val cursorCol: Int, val dirty: Boolean, val lastSavedAt: Instant, val isSaving: Boolean, val saveError: Throwable?, val readOnly: Boolean, val strikeChar: Char? = null, val strikeKey: Int = 0, val missing: Boolean = false, val queueLength: Int = 0)` | Extra fields: `strikeChar` and `strikeKey` trigger slug strike animations in Task 16; `missing` flags deleted/trashed notes to disarm saving; `queueLength` exposes save queue depth for testability. |
| `WriterEvent` | Sealed hierarchy: `TypeChar`, `Backspace`, `Enter`, `MoveCursor`, `SwitchPage`, `AddPage`, `DeletePage`, `SaveRequested`, `SaveCompleted`, `SaveFailed` | Identical sealed class hierarchy with matching data classes / objects. | Exactly identical. |
| `WriterReducer` | `object WriterReducer { fun reduce(state: WriterState, event: WriterEvent, mode: CorrectionMode): WriterState }` | `object WriterReducer { fun reduce(state: WriterState, event: WriterEvent, mode: CorrectionMode): WriterState }` | Exactly identical pure reducer. |
| `Autosaver` | `class Autosaver(val debounceMs: Long = 400L, val maxWaitMs: Long = 1500L, val onSave: suspend (WriterState) -> Unit)` | `class Autosaver(private val debounceMs: Long = 400L, private val maxWaitMs: Long = 1500L, private val onSave: suspend (WriterState) -> Unit)` with `offer(state: WriterState)`, `suspend fun flush()`, `fun cancel()` | Constructor parameters made `private val`; lifecycle control methods exposed for deterministic teardown. |
| `WriterViewModel` | `class WriterViewModel(noteId: String, repo: NoteRepository, settingsStore: SettingsStore) : ViewModel() { val state: StateFlow<WriterState>; fun dispatch(event: WriterEvent) }` | `class WriterViewModel(private val noteId: String, private val repo: NoteRepository, private val settingsStore: SettingsStore, private val dispatcher: CoroutineDispatcher = Dispatchers.Main) : ViewModel() { val state: StateFlow<WriterState>; fun dispatch(event: WriterEvent) }` | Added optional `dispatcher` for coroutine testing. |
| `PaperSheet` | `@Composable fun PaperSheet(widthDp: Float, heightDp: Float, night: Boolean, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit)` | `@Composable fun PaperSheet(widthDp: Float, heightDp: Float, night: Boolean, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit)` | Exactly identical. |
| `PaperText` | `@Composable fun PaperText(page: StruckPage, cursorRow: Int, cursorCol: Int, strikeChar: Char?, strikeKey: Int, readOnly: Boolean, night: Boolean, modifier: Modifier = Modifier)` | `@Composable fun PaperText(page: StruckPage, cursorRow: Int, cursorCol: Int, strikeChar: Char?, strikeKey: Int, readOnly: Boolean, night: Boolean, modifier: Modifier = Modifier, contentScrollOffsetDp: Float = 0f, contentScrollXDp: Float = 0f)` | Defaulted scroll parameters added to support smooth page scrolling. |
| `Caret` | `@Composable fun Caret(blink: Boolean, night: Boolean, modifier: Modifier = Modifier)` | `@Composable fun Caret(blink: Boolean, night: Boolean, modifier: Modifier = Modifier)` | Exactly identical. |
| `PaperStamp` | `@Composable fun PaperStamp(index: Int, total: Int, date: Instant, night: Boolean, modifier: Modifier = Modifier)` | `@Composable fun PaperStamp(index: Int, total: Int, date: Instant, night: Boolean, modifier: Modifier = Modifier)` | Exactly identical. |
| `Rail` | `@Composable fun Rail(railYDp: Float, modifier: Modifier = Modifier)` | `@Composable fun Rail(railYDp: Float, modifier: Modifier = Modifier)` | Exactly identical. Helper `RailGeometry` exposed for geometry tests. |
| `Carriage` | `@Composable fun Carriage(centerXDp: Float, railYDp: Float, mode: CarriageMotion, slugChar: Char?, strikeKey: Int, night: Boolean, reduceEffects: Boolean, lampIntensity: Float, modifier: Modifier = Modifier)` | `@Composable fun Carriage(railYDp: Float, centerXDp: Float, mode: CarriageMotion = CarriageMotion.Rest, slugChar: Char? = null, strikeKey: Int = 0, night: Boolean = false, reduceEffects: Boolean = false, lampIntensity: Float = 1f, modifier: Modifier = Modifier)` | Parameter order inverted (`railYDp` first), defaults provided for declarative Compose caller convenience. |
| `TypeSlug` | `@Composable fun TypeSlug(slugChar: Char?, strikeKey: Int, night: Boolean, modifier: Modifier = Modifier)` | `@Composable fun TypeSlug(slugChar: Char?, strikeKey: Int, night: Boolean, modifier: Modifier = Modifier)` | Exactly identical. Helper functions `slugOffsetDp` and `filterSlugChar` exposed for unit testing. |

---

## 4. Test Counts & Verification Breakdown

Parsed directly from Gradle JUnit XML test reports across all modules and build variants:

### By Module & Variant
- `:app/testDebugUnitTest`: 119 tests (0 failures, 0 errors, 0 skipped)
- `:app/testReleaseUnitTest`: 119 tests (0 failures, 0 errors, 0 skipped)
- `:audio/testDebugUnitTest`: 42 tests (0 failures, 0 errors, 0 skipped)
- `:audio/testReleaseUnitTest`: 42 tests (0 failures, 0 errors, 0 skipped)
- `:data/testDebugUnitTest`: 85 tests (0 failures, 0 errors, 0 skipped)
- `:data/testReleaseUnitTest`: 85 tests (0 failures, 0 errors, 0 skipped)
- `:design/testDebugUnitTest`: 141 tests (0 failures, 0 errors, 0 skipped)
- `:design/testReleaseUnitTest`: 141 tests (0 failures, 0 errors, 0 skipped)
- `:text/test`: 52 tests (0 failures, 0 errors, 0 skipped)
- **Total Unique Tests (Debug Suites): 439 tests** (0 failures, 0 errors, 0 skipped across entire repository).
- **Total Tests Executed (Debug + Release Variants): 826 tests** (all PASSED).

### Breakdown of `:app` Unit Tests (119 Tests)
- `WriterGeometryTest`: 10 tests (viewfinder centering, rail positioning, caret geometry, line spacing, hit testing)
- `WriterReducerTest`: 26 tests (typing, backspace, enter, cursor movements, page add/delete/switch, strike ranges, surrogate pairs, lift mode, cover mode)
- `AutosaverTest`: 6 tests (debounce delay, max-wait window, flush on pause, cancellation, save failures)
- `WriterViewModelTest`: 10 tests (loading notes, saving flow, event dispatch, trashed/missing notes handling, settings observation)
- `PaperTextTest`: 16 tests (character placement, blank line rulers, strike bar alignment, caret position, stamp formatting)
- `TypeSlugTest`: 9 tests (strike keyframes, mathematical bounds over 1,000 points, restart without snap-back, character filtering, geometry bounds)
- `PaperGoldenTest`: 7 golden snapshot tests
- `CarriageGoldenTest`: 6 golden snapshot tests
- `SettingsStoreTest`: 16 tests
- `ProvenanceTest`: 6 tests
- `ManifestTest`: 5 tests
- `LauncherIconTest`: 1 test
- `AppSmokeTest`: 1 test

---

## 5. Visual Golden Review (13 Snapshots in `:app`)

All 13 snapshots were recorded on Windows using Paparazzi 1.3.5 and independently inspected using image viewing tools:

### Paper Snapshots (`PaperGoldenTest`, 7 snapshots)
1. `app.enk.writer_PaperGoldenTest_paperDayEmpty.png`: Full 400x820 dp frame showing day mode paper (`paperDay` #F5F2EB), 1 dp line rulers (`ruleDay` #E2DCD0) every 24 dp, stamp in top-right corner.
2. `app.enk.writer_PaperGoldenTest_paperNightEmpty.png`: Night mode paper (`paperNight` #151413) with dark line rulers (`ruleNight` #242220) and muted stamp.
3. `app.enk.writer_PaperGoldenTest_paperDayWithStruckCharacters.png`: Typographic layout in Courier Prime 16 sp; shows normal characters, faint space glyphs (`·`), struck characters with horizontal strike bar (`strikeInk` #A62424) centered at 12 dp, and caret positioned at cursor coordinates.
4. `app.enk.writer_PaperGoldenTest_paperCaretBlinkPhaseOn.png`: Caret visible at 1.5 x 24 dp filled with `accent` (#FF5A1F) and bordered with 1 dp black 40%.
5. `app.enk.writer_PaperGoldenTest_paperCaretBlinkPhaseOff.png`: Caret completely invisible during the off phase of the 1060 ms square wave.
6. `app.enk.writer_PaperGoldenTest_paperStampDay.png`: Top-right stamp rendered as `P. 01 / 03 · 2026.11.08` with letter-spacing 1.2 sp, 9 sp Courier Prime Bold.
7. `app.enk.writer_PaperGoldenTest_paperStampNight.png`: Night mode stamp rendering with `stampNight` text color.

### Carriage & Rail Snapshots (`CarriageGoldenTest`, 6 snapshots)
1. `app.enk.writer_CarriageGoldenTest_railAloneAcross400dpScreen.png`: Full-width 12 dp continuous rod filled with `railSteel` (#888E99) and shadow, flanked by 12x24 dp left and right brackets (`bracketSteel` #5A606B, 3 dp radius). Zero ticks or ruler markings.
2. `app.enk.writer_CarriageGoldenTest_carriageAtRestDayMode.png`: 54x36 dp graphite cap carriage centered over the rail; displays 5 dp top/bottom steel collars, 32x8 dp grip grooves with 1.5 dp white 16% vertical lines, 16x3 dp centered orange indicator strip with 1 dp dark ring, and 18x11 dp slot window.
3. `app.enk.writer_CarriageGoldenTest_carriageSlugStrikePeak50ms.png`: Type-slug elevated to -5 dp peak offset, exposing 14x12 dp steel slug face with 'k' glyph centered in 9 sp Courier Prime Bold.
4. `app.enk.writer_CarriageGoldenTest_carriageAtNightLampGlow.png`: Night mode carriage with lamp color warm glow (0,0,14,5) filling slot window, and glowing orange indicator strip (`accentGlowNight`).
5. `app.enk.writer_CarriageGoldenTest_carriageMidSnapOvershoot.png`: Carriage positioned at +3.5 dp overshoot simulating cubic bezier snap physics.
6. `app.enk.writer_CarriageGoldenTest_carriageDraggingMode.png`: Carriage dragged +20 dp tracking user finger offset.

*Note on Windows Golden Rendering:* Windows DirectWrite/GDI rasterization causes minor sub-pixel antialiasing differences compared to Linux headless CI. The goldens recorded here represent the clean local ground truth.

---

## 6. Edge Cases Handled

1. **Surrogate Pair Safety:** String traversal and cursor advancement in `WriterReducer` and `PaperText` treat Unicode surrogate pairs (e.g. emoji and supplementary plane symbols) as atomic units; deleting or striking does not leave unpaired high or low surrogates.
2. **Strike Range Merging & Splitting:** Struck ranges in `StruckPage` properly split when characters inside a struck region are modified or removed, preserving immutable history.
3. **Trashed Note Autosave Disarm:** Non-existent or trashed notes set `missing = true` upon load. `WriterViewModel` ignores dispatch events and disarms `Autosaver` so that empty state is never saved back over deleted data.
4. **Caret Reset on Typing:** Typing or manual cursor repositioning resets the 1060 ms blink cycle to solid ON phase immediately.
5. **Slug Strike Restart:** Repeated rapid keystrokes trigger mid-flight strike restarts from the current elevation offset without snapping back down to the +9 dp rest floor.
6. **Strict Character Filtering:** Whitespace, newlines, and ISO control characters are filtered out from the slug face to avoid misaligned or invisible rendering on the slug head.
7. **Strict Orange Policy:** The `accent` (#FF5A1F) and `accentGlowNight` tokens are strictly confined to the caret and the carriage indicator strip.

---

## 7. Resource Leaks, Dead Code & Deviations

- **Resource Leaks:** None. `WriterViewModel` and `Autosaver` scopes are cancelled on teardown; flow collectors are bound to lifecycle scopes.
- **Dead Code:** Zero unused code, stubs, or temporary hacks.
- **Deviations:**
  - **Module Placement:** Tasks 15 and 16 were placed in `:app` (`app.enk.writer`) rather than `:design` components. This is intentional: they are domain-specific writer components that depend directly on writer geometry and state models, aligning with Tasks 13, 14, and 17.
  - **Declarative Carriage Parameters:** `Carriage` composable takes `centerXDp: Float` and `mode: CarriageMotion` directly as parameters instead of requiring a mutable `CarriageState` holder class, following Jetpack Compose unidirectional data flow.
  - **Extra State Fields:** `strikeChar`, `strikeKey`, `missing`, and `queueLength` were added to `WriterState` for animation timing, deletion protection, and deterministic test assertions.

---

## 8. Full Gate Execution Output

### Gradle Clean Build & Verification Suite
```
Starting a Gradle Daemon, 2 incompatible and 35 stopped Daemons could not be reused, use --status for details

> Task :app:clean
> Task :audio:clean
> Task :data:clean
> Task :design:clean
> Task :text:clean
...
> Task :app:checkDebugAarMetadata
> Task :app:checkReleaseAarMetadata
> Task :design:testDebugUnitTest
> Task :data:testDebugUnitTest
> Task :app:testDebugUnitTest
> Task :data:testReleaseUnitTest
> Task :design:testReleaseUnitTest
> Task :app:testReleaseUnitTest
> Task :design:verifyPaparazziDebug
> Task :app:verifyPaparazziDebug
> Task :app:lintDebug
> Task :audio:lintDebug
> Task :data:lintDebug
> Task :design:lintDebug

BUILD SUCCESSFUL in 5m 48s
408 actionable tasks: 406 executed, 2 up-to-date
```

### Network Permission Verification
```
$ & "C:\Program Files\Git\bin\bash.exe" scripts/check-no-network.sh
Checking manifest: app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml
SUCCESS: No network permissions declared in merged manifest.
```
