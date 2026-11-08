# Batch 5 Audit Handoff: Writer Core (Geometry, State, Paper, Carriage)

This document was rewritten after the audit. The first version described types, goldens and behaviour that did not exist. Every table below is taken from the source and from the Gradle reports.

## 1. Branch and commits

- **Branch:** `batch-5/writer-core`, base `main` = `1f6da5b`.
- **Implementer commits:** Step 0 `135f620`; Task 13 `ece41d5`; Task 14 `4bc1eb6`; Task 15 `0e8e7d2`; Task 16 `4d245a6`; first handoff `2b1d9cc`.
- **Audit fixes:** in the working tree on top of `2b1d9cc`; committed in one commit once the author approves.

## 2. Step 0: AAR metadata checks

- The `enabled = false` block for `AarMetadata` tasks is removed; `:app:checkDebugAarMetadata` and `:app:checkReleaseAarMetadata` run and pass.
- Versions that pass at `compileSdk = 35` and AGP 8.13.2: `core-ktx` 1.16.0, `activity-compose` 1.10.1, `lifecycle` 2.10.0. The failing newer versions and their reasons are in `docs/dependencies.md` (section 5).
- `kotlinx-coroutines-test` 1.11.0 (newest stable on Maven Central, checked 2026-11-09), `testImplementation` in `:app` only. `androidx.room:room-runtime` is also `testImplementation` in `:app` (in-memory database for `WriterViewModelTest`).
- Paparazzi in `:app` needs a workaround: its LayoutLib is API 34, so a hook in `app/build.gradle.kts` rewrites `targetSdkVersion` 35 to 34 inside Paparazzi's generated `resources.json` only. The app, lint and AAR checks keep `targetSdk = 35`. Reason and removal condition: `docs/dependencies.md` section 7.
- Paparazzi cannot load `res/font` fonts (the text falls back to sans). The writer goldens therefore load `courier_prime_*.ttf` as a file font through the `fontFamily` parameters. The app uses the resource font.

## 3. Public signatures against the plan

Plan: `docs/superpowers/plans/2026-11-07-enk-1-0-plan.md`, Tasks 13 to 16.

| Component | Plan | Implemented | Difference |
|---|---|---|---|
| `WriterGeometry` | `data class` with the 8 fields, `textTopOffsetDp`, `carriageCenterXDp`, `colFromX`, `compute(widthDp, heightDp, fontScale)` | same | none. Non-finite or non-positive width gives finite geometry |
| `WriterState` | `noteId, pages, pageIndex, caret, lampOn = false, lastTypedIndex = null` | same, plus `composingRange: IntRange? = null`, `missing: Boolean = false`, `loaded: Boolean = false` | 3 extra fields (composing range; note missing or trashed; load finished) |
| `WriterEvent` | `Insert(text, composing = false)`, `Backspace`, `MoveCaret(index)`, `GoToPage(index)`, `ToggleLamp` | same | none |
| `reduce` | `fun reduce(state, event, correction): WriterState` | same (top-level function) | none |
| `Autosaver` | `Autosaver(scope, debounceMs = 500, save)` with `onChange`, `suspend flush` | same | `scope`, `debounceMs`, `save` are `private val` |
| `WriterViewModel` | `WriterViewModel(noteId, repo, settings) : ViewModel`, `state`, `dispatch` | same, plus optional 4th parameter `ioScope: CoroutineScope? = null`, plus `suspend fun flush()` | extra parameter for tests; `flush()` for the screen's `ON_STOP` |
| `PaperSheet` (frame) | `PaperSheet(night, reduceEffects, modifier, content)` | same, `content` defaults to `{}` | default only |
| `PaperSheet` (with state) | not in plan | `PaperSheet(state, geometry, night, reduceEffects, modifier, lamp: Rgb = fromWarmth(25), fontFamily = CourierPrime, typing = false, stampFrame: Float? = null)` | convenience overload that assembles sheet, text and caret |
| `PaperText` | `PaperText(page, caret, lastTypedIndex, geometry, textColor, reduceMotion, modifier)` | same, plus `fontFamily = CourierPrime`, `stampFrame: Float? = null` | `stampFrame` freezes the stamp for goldens; `null` runs the animation |
| `Caret` | `Caret(geometry, pos, typing, reduceMotion)` | same, plus trailing `modifier: Modifier = Modifier` | modifier only |
| `Rail` | `Rail(railYDp, modifier)` | same | none |
| `Carriage` | `Carriage(railYDp, centerXDp, mode, slugChar, strikeKey, night, lampColor: Rgb, reduceMotion, modifier)` | `Carriage(railYDp, centerXDp, mode, slugChar, strikeKey, night, reduceEffects, lampIntensity = 1f, lamp: Rgb = fromWarmth(25), modifier, slugProgress: Float? = null, slugFontFamily = CourierPrime, onHitAreaMeasured: ((Float, Float) -> Unit)? = null)` | `lampColor` is `lamp`; `reduceMotion` is `reduceEffects`; extra `lampIntensity`, and test and preview parameters (`slugProgress`, `slugFontFamily`, `onHitAreaMeasured`) |
| `CarriageMotion` | `Glide, Dragging, Snap` | `Rest, Glide, Dragging, Snap` | `Rest` added |
| `CarriageState` | `class CarriageState { centerXDp; dragging; snapping }` | not implemented | the composable is driven by parameters instead |
| `TypeSlug` | part of `Carriage` | `TypeSlug(slugChar, strikeKey, night, modifier, slugProgress: Float? = null, fontFamily = CourierPrime)` | separate composable; helpers `slugOffsetDp`, `filterSlugChar` |
| `PaperStamp` | not in plan | not implemented | the stamp is the per-character animation inside `PaperText` |

Module placement: Tasks 15 and 16 live in `:app` (`app.enk.writer`), not under `ui/` or in `:design` as the plan's file lists say. The Paparazzi goldens are therefore `:app` goldens.

## 4. Behaviour decided and covered by tests

- **Reducer:** surrogate pairs are never split (insert, backspace, strike, caret move); composing text commits as one insert; overflow creates a page right after the current one; a paste over twice the page size is split with `Paginator.split`; strike-through backspace skips characters already struck; `MoveCaret` clamps and never lands inside a pair (`WriterReducerTest`, 26 tests).
- **Autosaver** (`AutosaverTest`, 8 tests): 20 rapid changes make one save after 500 ms; saves never overlap; a failed background save is retried every debounce interval until it succeeds; `flush()` after a background failure saves instead of throwing; a failed `flush()` throws and keeps the pages pending (and re-arms the retry); a second flush with no change saves nothing.
- **ViewModel** (`WriterViewModelTest`, 11 tests): events dispatched before the note loads are held and applied after loading, with the stored correction mode; a missing or trashed note is flagged `missing` and never saved; opening without typing leaves `modifiedAt` unchanged; `onCleared` flushes unsaved text (`unsavedTextSurvivesViewModelClear`, which waits up to 5 s for the write instead of sleeping). A ViewModel that creates its own scope cancels it after the final flush; a failing final flush is swallowed so it cannot crash the process.
- **Slug:** a `strikeKey` present at first composition does not strike; a new key interrupts from the current offset.
- **Carriage:** Snap animates to the caret column; the overshoot comes only from `EnkMotion.carriageSnap` (tested to overshoot and end at 1). Hit area measured at 54 by 48 dp (`hitAreaIs54By48Dp`).
- **Strike line:** drawn a quarter em above the font baseline, through the middle of lowercase letters.

## 5. Clean gate

```
JAVA_HOME=<JDK 21> ./gradlew.bat clean build test :design:verifyPaparazziDebug :app:verifyPaparazziDebug :app:processDebugManifest :app:checkDebugAarMetadata :app:checkReleaseAarMetadata
```
Result (auditor run, after the fixes):
```
BUILD SUCCESSFUL in 4m 4s
408 actionable tasks: 384 executed, 24 up-to-date
```
`scripts/check-no-network.sh`: `SUCCESS: No network permissions declared in merged manifest.`

## 6. Test counts (Gradle XML reports, debug variant)

- `:app` 122: AppSmokeTest 1, ManifestTest 5, ProvenanceTest 6, LauncherIconTest 1, SettingsStoreTest 16, AutosaverTest 8, CarriageGoldenTest 7, PaperGoldenTest 9, PaperTextTest 12, TypeSlugTest 9, WriterGeometryTest 11, WriterReducerTest 26, WriterViewModelTest 11
- `:audio` 42, `:data` 85, `:design` 141, `:text` 52
- **Total 442, 0 failures, 0 errors, 0 skipped.**

## 7. Goldens (`app/src/test/snapshots/images`, 15 images)

All were inspected by eye after recording. Courier Prime is loaded from the font file (section 2).

Paper (9): `dayPaperWithOneLineTypedCaretCol12`, `nightPaperWithOneLineTypedCaretCol12`, `wrappedLineAt28Cols`, `caretAtCol28`, `emptyPageOnlyCaret`, `fontScale13_22Cols`, `emojiPage`, `struckCharactersInStrikeThroughMode`, `stampedCharacterAt80ms`.

Carriage and rail (6): `railAloneAcross400dpScreen`, `carriageAtRestDayMode`, `carriageSlugStrikePeak50ms` (slug frozen at progress 0.45), `carriageAtNightLampGlow`, `carriageDraggingMode`, `hitAreaIs54By48Dp`.

## 8. Not verified

- Goldens were recorded on Windows only. Linux CI with JDK 21 has not run, so a rasteriser difference there is unknown.
- The goldens are compared against the design system text, not a prototype render.
- Nothing runs on a device: feed, stamp, snap and slug motion are verified only as pure functions and frozen frames, not as live animation.
- The slug's own letter is tiny in the carriage golden; its shape is not checked closely.
- `WriterViewModel` still finds a trashed note by scanning the trash list on load; a direct repository query is a later cleanup.
- The screen that calls `flush()` on `ON_STOP` does not exist yet (a later task).
