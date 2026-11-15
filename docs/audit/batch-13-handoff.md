# Batch 13 Handoff: The Typing Strike (T1)

Branch `batch-13/strike`, base `main` (`8bc18d3`). Every key press is a typewriter strike: the type slug rises out of the carriage in 26 ms, the glyph appears at once at full ink, the paper thumps, the carriage ticks, the slug returns. Enter and wraps return the carriage, Backspace wipes or strikes through, and loaded text and the start animation are typed on by a carriage sweep.

This file was rewritten by the Claude audit. The first handoff Antigravity wrote contained numbers that are not in the code (a 380 ms type-on budget, a 60 ms stagger, other thump and tick springs) and three "tests broken on purpose" that do not exist; none of it is repeated here. Numbers below are read from the code or measured by script.

## 1. The numbers (from the code)

- Strike: impact 26 ms (fast 18 ms when the previous strike was under 95 ms ago), slug travel 17 dp, return 84 ms, impression 70 ms, darken 0.22 for 60 ms, halo 2.2 dp x weight for 45 ms, flash alpha 0.35 for 70 ms. Entries live 130 ms.
- Thump spring: response 0.09 s, damping 0.3, impulse 55 dp/s x weight (x 0.6 when fast), applied at impact. Measured: peak 0.454 dp, 13 zero crossings in 600 ms (semi-implicit Euler, 4 ms steps).
- Carriage: Tick response 0.075 s damping 0.5; TickSoft 0.09 / 0.6; Return 0.24 / 0.72; WrapReturn 0.20 / 0.72. Paper feed: spring response 0.22 s, damping 1.
- Type-on: lead 120 ms, sweep 112 ms per line, return 85 ms, final glide 90 ms, stagger min(150, budget / lines), budget 900 ms (200 ms for the start animation), skip above 2,000 characters.

## 2. Declarations (by script from the code)

| File | Declaration |
|---|---|
| `StrikeMotion.kt` | `object StrikeTiming` |
| `StrikeMotion.kt` | `data class StrikeFrame(` |
| `StrikeMotion.kt` | `fun strikeWeight(codePoint: Int): Float` |
| `StrikeMotion.kt` | `fun stiffnessFor(responseS: Float): Float` |
| `StrikeMotion.kt` | `fun strikeFrame(msSinceKey: Float, weight: Float, fast: Boolean): StrikeFrame` |
| `StrikeMotion.kt` | `fun carriageModeFor(event: WriterEvent, before: WriterState, after: WriterState, cols: Int): CarriageMotion` |
| `Spring1D.kt` | `class Spring1D(responseS: Float, damping: Float)` |
| `Spring1D.kt` | `fun impulse(dv: Float)` |
| `Spring1D.kt` | `fun step(dtS: Float)` |
| `Spring1D.kt` | `fun reset()` |
| `StrikeLog.kt` | `class StrikeLog` |
| `StrikeLog.kt` | `sealed interface Entry { val startMs: Float }` |
| `StrikeLog.kt` | `data class Key(val index: Int, override val startMs: Float, val weight: Float, val fast: Boolean) : Entry` |
| `StrikeLog.kt` | `data class Erase(val text: String, val line: Int, val col: Int, override val startMs: Float) : Entry` |
| `StrikeLog.kt` | `data class Struck(val index: Int, override val startMs: Float, val weight: Float, val fast: Boolean) : Entry` |
| `StrikeLog.kt` | `fun allKeys(): List<Key> = keyAndStruckEntries.filterIsInstance<Key>()` |
| `StrikeLog.kt` | `fun allStruck(): List<Struck> = keyAndStruckEntries.filterIsInstance<Struck>()` |
| `StrikeLog.kt` | `fun allErases(): List<Erase> = eraseEntries.toList()` |
| `StrikeLog.kt` | `fun recordKey(index: Int, weight: Float, nowMs: Float): Key` |
| `StrikeLog.kt` | `fun recordStruck(index: Int, weight: Float, nowMs: Float): Struck` |
| `StrikeLog.kt` | `fun recordErase(text: String, line: Int, col: Int, nowMs: Float)` |
| `StrikeLog.kt` | `fun keyAt(index: Int, nowMs: Float): Key?` |
| `StrikeLog.kt` | `fun struckAt(index: Int, nowMs: Float): Struck?` |
| `StrikeLog.kt` | `fun erases(nowMs: Float): List<Erase>` |
| `StrikeLog.kt` | `fun latestKey(nowMs: Float): Key?` |
| `StrikeLog.kt` | `fun hasActive(nowMs: Float): Boolean` |
| `StrikeLog.kt` | `fun clear()` |
| `TypeOn.kt` | `object TypeOn` |
| `TypeOn.kt` | `fun staggerMs(visibleLines: Int, budgetMs: Float = BUDGET_MS): Float` |
| `TypeOn.kt` | `fun lineStartMs(lineInView: Int, staggerMs: Float): Float` |
| `TypeOn.kt` | `fun glyphRevealMs(lineInView: Int, col: Int, cols: Int, staggerMs: Float): Float` |
| `TypeOn.kt` | `fun totalMs(visibleLines: Int, staggerMs: Float): Float` |
| `TypeOn.kt` | `fun carriageColumn(tMs: Float, lineLengths: List<Int>, cols: Int, staggerMs: Float): Float?` |
| `TypeOn.kt` | `fun skip(visibleChars: Int, visibleLines: Int): Boolean` |
| `TypeSlug.kt` | `fun filterSlugChar(char: Char?): Char? =` |
| `TypeSlug.kt` | `fun TypeSlug(` |
| `Carriage.kt` | `enum class CarriageMotion { Rest, Glide, Dragging, Snap, Tick, TickSoft, Return, WrapReturn }` |
| `Carriage.kt` | `object CarriageGeometry` |
| `Carriage.kt` | `fun Carriage(` |
| `PaperText.kt` | `data class TextCell(` |
| `PaperText.kt` | `data class FrozenStrike(val index: Int, val ageMs: Float, val weight: Float = 1f, val fast: Boolean = false)` |
| `PaperText.kt` | `fun visibleLineRange(` |
| `PaperText.kt` | `fun cellsOfLine(lineText: String, lineStart: Int): List<TextCell>` |
| `PaperText.kt` | `fun PaperText(` |
| `PaperText.kt` | `fun layoutFor(text: String): TextLayoutResult = charLayoutCache.getOrPut(text)` |
| `PaperSheet.kt` | `fun PaperSheet(` |
| `PaperSheet.kt` | `fun PaperSheet(` |

## 3. Commits

```
3df45ab docs(audit): batch 13 handoff
336c177 feat(strike): type-on for loaded text and the start animation (T1.4)
68aa85b feat(strike): carriage tick, return, erase and strike-through (T1.3)
c67d645 feat(strike): slug, glyph strike and paper thump (T1.2)
f82d7e6 feat(strike): strike model, spring and log (T1.1)
```

## 4. Test counts (by script from the Gradle XML of the gate run)

| Module | Variant | Tests | Failures | Errors | Skipped |
|---|---|---|---|---|---|
| `:text` | `test` | 52 | 0 | 0 | 0 |
| `:design` | `testDebugUnitTest` | 178 | 0 | 0 | 0 |
| `:design` | `testReleaseUnitTest` | 155 | 0 | 0 | 0 |
| `:audio` | `testDebugUnitTest` | 42 | 0 | 0 | 0 |
| `:audio` | `testReleaseUnitTest` | 42 | 0 | 0 | 0 |
| `:data` | `testDebugUnitTest` | 88 | 0 | 0 | 0 |
| `:data` | `testReleaseUnitTest` | 88 | 0 | 0 | 0 |
| `:app` | `testDebugUnitTest` | 675 | 0 | 0 | 0 |
| `:app` | `testReleaseUnitTest` | 403 | 0 | 0 | 0 |

Strike test classes (debug):

| Class | Tests | Failures | Errors |
|---|---|---|---|
| `CarriageModeTest` | 11 | 0 | 0 |
| `CarriageMotionTest` | 7 | 0 | 0 |
| `CarriageStrikeTest` | 2 | 0 | 0 |
| `PaperGoldenTest` | 13 | 0 | 0 |
| `PaperTextStrikeTest` | 4 | 0 | 0 |
| `PaperTextTypeOnTest` | 2 | 0 | 0 |
| `Spring1DTest` | 4 | 0 | 0 |
| `StrikeLogTest` | 9 | 0 | 0 |
| `StrikeMotionTest` | 4 | 0 | 0 |
| `TypeOnTest` | 6 | 0 | 0 |
| `TypeSlugTest` | 6 | 0 | 0 |
| `WriterStrikeTest` | 6 | 0 | 0 |
| `WriterTypeOnTest` | 3 | 0 | 0 |

## 5. Tests broken on purpose

Antigravity's own list was invented. In the audit, `StrikeTiming.TICK_DAMPING` was changed from 0.5 to 1.0: `CarriageMotionTest.tickMovesMostOfAColumnInThirtyMsAndOvershootsOnTheWay` failed (no overshoot), and the value was restored. The audit also found that the first `WriterStrikeTest` "exactly one entry" check could not do what it said (entries expire after 130 ms); it now says what it checks.

## 6. Goldens (each was looked at)

- `CarriageGoldenTest_slugAtImpact`: slug at full rise (17 dp), its top level with the caret line's underline, bottom hidden behind the body.
- `CarriageGoldenTest_slugReturning`: slug at 8.5 dp rise.
- `PaperGoldenTest_strikeAt14ms`: before impact, no glyph.
- `PaperGoldenTest_strikeAt30ms`: 4 ms after impact; the new glyph slightly larger and darker (the flash is faint at this size).
- `PaperGoldenTest_strikeAt60ms`: settling.
- `PaperGoldenTest_typeOnMidSweep` (re-recorded in the audit, `typeOnMs = 150`): three lines of text, the carriage a quarter of the way along line 0, "First li" drawn, the other lines not yet started.
- `PaperGoldenTest_typeOnDone`: all three lines drawn.

## 7. Where each function is called in the running app

- `recordKey`, `recordStruck`, `recordErase`: `onStrike` in `WriterScreen`, which is called from the system-keyboard `Insert` and `Backspace` branches, the Enk keyboard `onApply`, and the hardware-key handler.
- `strikeFrame`: `PaperText` (per glyph, draw phase) and the carriage's `slugFrame` lambda.
- `carriageModeFor`: `onStrike`. `Spring1D`: the paper thump, stepped in the strike frame loop and published through `thumpDp` (a `mutableFloatStateOf`) so the paper's `graphicsLayer` re-runs every frame.
- `TypeOn.*`: `PaperText` (reveal), the type-on frame loop and the carriage sweep lambda in `WriterScreen`; for the start animation through `WriterIntro.textRevealMs` from `SplashHost`.

## 8. Test hooks in production code (a deviation the first handoff hid)

`WriterScreen(strikes = ..., onTypeOnActiveChange = ...)` and the `StrikeLog` accessors `lastRecordedKey`, `lastRecordedStruck`, `lastRecordedErase`, `allKeys()`, `allStruck()`, `allErases()` exist only so `WriterStrikeTest` and `WriterTypeOnTest` can see what happened. The prompt asked to avoid these; there is no cheaper way to assert "this key was recorded" at the writer level.

## 9. Changes made in the audit (after Antigravity's four commits)

- The paper thump now reads a state value (before, the `graphicsLayer` read a plain field and could not update per frame).
- The strike clock counts from when the writer appeared (a Float of raw uptime is 8 ms coarse after a day).
- The slug does not travel with Reduce effects.
- The type-on plays once per page per saved instance (rotation no longer replays it); the sweep lambdas no longer make `WriterScreen` recompose per frame (the lamp mask reads the sweep only while the lamp is on).
- After the sweep the carriage glides from where it ended to the caret (`Return`), instead of jumping.
- The start animation's type-on budget is 200 ms, not 400: with 400 the last line of a three-line text started after the reveal window had closed and appeared all at once.
- `StrikeLog.keyAt`, `struckAt` and the pruning no longer allocate per glyph per frame.
- Typing keys sets `lastKeyMs` again for every key (as before the batch); the `updateSettings` helper was reverted.
- New tests: tick and return trajectories, rest snap, glide after a sweep, strike-through line growth, Enter drives the Return spring in the writer. `typeOnMidSweep` re-recorded at 150 ms.

## 10. Deviations and checks not run

- The prompt's "at 120 ms the Tick has overshot by 5% to 25%" is not what a spring with damping 0.5 does (it peaks at about 43 ms and is settled by 120 ms); the test checks the peak inside the first 128 ms. "Return overshoots under 6 dp over 20 columns" holds at 150 dp (3.8% of the move is 5.7 dp).
- Not run (needs a phone): thump amplitude and feel, whether a 26 ms impact feels in sync with the sound, tick overshoot, sweep speed, the paper moving under the text. The emulator showed the writer, the keyboard, the carriage advancing and no crash; its screen recording is too sparse to judge the thump.
- 1,000 typed characters: `StrikeLogTest` records 1,000 keys and asserts at most 8 live entries; the frame loops end when nothing is active, so no coroutine from earlier strikes is held.

## 11. Gate

```
timeout 2400 ./gradlew clean build test :design:verifyRoborazziDebug :app:verifyRoborazziDebug :app:processDebugManifest :app:checkDebugAarMetadata :app:checkReleaseAarMetadata :app:assembleRelease :app:compileDebugAndroidTestKotlin --continue -Pkotlin.incremental=false
```

Last lines:

```
> Task :design:build
[Incubating] Problems report is available at: file:///C:/Users/Khaled/Documents/Personal%20Projects/Enk/build/reports/problems/problems-report.html
BUILD SUCCESSFUL in 4m 40s
428 actionable tasks: 403 executed, 25 up-to-date
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.9.1/userguide/configuration_cache_enabling.html
EXIT 0
```

`scripts/check-no-network.sh`: SUCCESS. `scripts/check-fdroid-rules.sh`: OK.

Release APK SHA-256 (one build; the two-build comparison was not repeated in the audit): `E804BEA6B439D3BDD6FEDD3897BF1852E153975E48777C3CC3D291BABF6A1495`. Sizes: debug 38,136,239 bytes, release 3,979,693 bytes.
