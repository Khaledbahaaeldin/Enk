# Batch 7 handoff: app shell, navigation, start animation

Branch `batch-7/shell-and-start`, base `main` (`fe01bf5`). Tasks 20A and 20B. The first version of this handoff was written by the implementer; the external audit found it wrong in several places, and the auditor then fixed the branch. **This version replaces it and is generated from the code and the gate output.**

## 1. What the audit found, and what was done

| # | Finding | Fix | Proof |
|---|---|---|---|
| 1 | The overlay never drew the paper, note text or top bar; `paperOffsetY`, `textAlpha`, `topBarAlpha`, `ownPartsAlpha` were computed and read by nothing; the animation ended on flat grey and then cut to the writer | Three layers: `StartBackdrop` under the writer, the real writer in the middle (held back through `WriterIntro`: paper rises, text, top bar and own rail, carriage and caret fade in, own chassis withheld), `StartOverlay` on top | `StartGoldenTest` (12 frames, viewed); `StartHandoffTest.theIntroHoldsTheWriterBackThenReleasesItInOrder` |
| 2 | Hand-off targets came from `LocalConfiguration` height and font scale 1.0 with no insets, so the overlay ended up to 49 dp from the real rail | `StartLayout.targets` builds them from `WriterGeometry` inside the same safe-drawing insets and font scale the writer uses (`rememberStartTargets`); the caret rect comes from the new `CaretGeometry`, which `Caret` also uses | `StartHandoffTest.theLastFrameLandsOnTheWriters*` (real writer, 0.5 dp, 3 variants); broken on purpose: dropping the top inset made 4 tests fail (rail 506 vs 457) |
| 3 | `ShellScaffoldTest.switchingToBoxAndBack...` failed under load (5 s wall-clock wait on a real IO thread) | `MainViewModel` takes an injectable dispatcher; waits are 60 s hang guards that end when the work does; the test uses one `testContainer` with single-thread Room executors and flushes before the activity stops, then checks what was stored | 3 consecutive passes with `--rerun-tasks`, then the full gate (section 7) |
| 4 | Handoff written from memory | This document; signatures extracted by script | section 2 |
| 5 | No test that the overlay's last frame equals the writer's parts | see 2 | |
| 6 | `MainViewModel.onCleared` no longer closed the database | restored as `container.db.close()` | `MainViewModelTest.clearingTheStoreReleasesTheDatabaseAndThePlayer` |
| 7 | Dependency and release-test exclusions added without asking | Kept, disclosed in section 6; `core-splashscreen` is needed for `setKeepOnScreenCondition` and the exit listener | `:app:checkDebugAarMetadata :app:checkReleaseAarMetadata` pass |
| 8 | RTL test implied app support | Renamed `navBarMirrorsUnderRtlLayoutDirection`; the app keeps `supportsRtl="false"` because the product spec puts RTL out of scope for 1.0 | spec `2026-11-07-enk-design.md` line 32 |
| 9 | Minor: tautological rotation test; first frame used default theme settings; copied `1.42f` in `MainActivity`; wordmark was dark ink on the dark day chassis | rotation test checks the same view model, back from the restored route and a real container; `MainViewModel.initialSettings` is read before the first frame; `CaretGeometry`; the wordmark is always the light one (the chassis is graphite in day and night) | `MainViewModelTest.initialSettingsAreReadBeforeLoadedIsReported`, `ShellScaffoldTest.rotationKeepsRoute` |
| 10 | Nav bar had no stale-callback test (Batch 6 lesson 4) | `GlassNavBarTest.aCallbackReplacedMidDragIsTheOneThatIsCalled` | broken on purpose: reading `onSelect` directly instead of the `rememberUpdatedState` value made it fail |

| 11 | **Found on a device** (new Android 17 emulator): the soft keyboard rose halfway through the animation and resized the writer under the overlay | The writer's field takes focus only once the animation is over (`LaunchedEffect(intro == null)`) | recorded frames of a cold start at 5x animator scale: keyboard now arrives after the top bar. A Robolectric test could not discriminate this (it passed with the fix removed), so none is claimed |
| 12 | **Found on a device**: the "drag marker to scrub" hint was visible over the splash at the start | The hint fades in with the text (`introAlpha(textAlpha)`) | same recording |
| 13 | **Found on a device**: status bar icons were white on every light shell screen and on the light splash (unreadable) | `SystemBarIcons`: dark icons on light surfaces, light on dark; the writer is always a dark surface; `SplashHost` keeps them dark on the light splash until the chassis circle covers it (`StartTargets.coverRadius`) | `SystemBarIconsTest` (3), `StartTimelineTest.theLightSplashIsCovered...`; broken on purpose (inverted flag): 2 of 3 failed; screenshots of every shell screen |

Also fixed while there: a post-splash theme (`Theme.Enk`) whose window background is the splash colour, so a frame between the system splash and the first composed frame cannot flash white or black; the system splash is released only after the first composed frame (`SplashHost.onFirstFrame`) with a 300 ms safety net; "cold start" is now "first activity of this process" instead of "no saved state", so rotation and re-opening never play the animation; the splash hold and its 1,500 ms cap are a pure class (`SplashHold`) tested with a fake clock.

## 2. Declarations (extracted from the code by script)

| File | Declaration (copied from the code) |
|---|---|
| `shell/Route.kt` | `enum class Dest { Write, Box, Find, Setup }` |
| `shell/Route.kt` | `sealed interface Route` |
| `shell/Route.kt` | `object Writer : Route` |
| `shell/Route.kt` | `object Box : Route` |
| `shell/Route.kt` | `object Find : Route` |
| `shell/Route.kt` | `object Setup : Route` |
| `shell/Route.kt` | `object Trash : Route` |
| `shell/Route.kt` | `data class Note(val id: String, val page: Int) : Route` |
| `shell/Route.kt` | `fun navSelection(route: Route): Dest? = when (route)` |
| `shell/Route.kt` | `fun routeBack(current: Route): Route? = when (current)` |
| `shell/LargeTitle.kt` | `@Composable fun LargeTitle( title: String, meta: String?, modifier: Modifier = Modifier )` |
| `shell/CompactBar.kt` | `@Composable fun CompactBar( title: String, progress: Float, glassSource: GlassSource, modifier: Modifier = Modifier )` |
| `shell/GlassNavBar.kt` | `@Composable fun GlassNavBar( selected: Dest, onSelect: (Dest) -> Unit, modifier: Modifier = Modifier, glassSource: GlassSource = rememberGlassSource() )` |
| `shell/GlassNavBar.kt` | `fun resolveDest(colIndex: Int): Dest?` |
| `shell/GlassNavBar.kt` | `fun resolveCol(dest: Dest): Int` |
| `shell/ShellScaffold.kt` | `@Composable fun ShellScaffold( title: String, meta: String?, showNav: Boolean, selected: Dest?, onNavigate: (Dest) -> Unit, modifier: Modifier = Modifier, listState: LazyListState = rememberLazyListState(), content: @Composable (listState: LazyListState, contentPadding: PaddingValues) -> Unit )` |
| `shell/EnkNavHost.kt` | `@Composable fun EnkNavHost( container: AppContainer, modifier: Modifier = Modifier, mainViewModel: MainViewModel = viewModel( factory = MainViewModel.factory(LocalContext.current) ), /** Passed to the writer while the start animation runs; null otherwise. */ intro: WriterIntro? = null )` |
| `start/StartTimeline.kt` | `data class StartTargets( val railY: Float, val screenWidth: Float, val screenHeight: Float, val carriageX: Float, val caretRect: Rect, val paperHeight: Float, val lightSplash: Boolean )` |
| `start/StartTimeline.kt` | `data class StartFrame( val apertureRadius: Float, val rail: Rect, val linesAlpha: Float, val chassisMix: Float, val chassisCircleRadius: Float, val paperOffsetY: Float, val carriage: CarriageFrame, val caret: Rect, val textAlpha: Float, val topBarAlpha: Float, val overlayAlpha: Float, val ownPartsAlpha: Float )` |
| `start/StartTimeline.kt` | `data class CarriageFrame(val centre: Offset, val scale: Float)` |
| `start/StartTimeline.kt` | `object StartTimeline` |
| `start/StartTimeline.kt` | `fun at(tMs: Float, rawTargets: StartTargets): StartFrame` |
| `start/StartTimeline.kt` | `internal fun StartTargets.coverRadius(): Float` |
| `start/StartGate.kt` | `object StartGate` |
| `start/StartGate.kt` | `fun shouldPlay( coldStart: Boolean, launchedFromWidgetOrIntent: Boolean, restoredState: Boolean, loadMs: Long ): Boolean = coldStart && !launchedFromWidgetOrIntent && !restoredState && loadMs <= 1500L` |
| `start/StartGate.kt` | `fun reducedMotionDurationMs(): Int = 250` |
| `start/SplashHold.kt` | `class SplashHold(private val startMs: Long, private val capMs: Long = CAP_MS)` |
| `start/SplashHold.kt` | `fun shouldHold(loaded: Boolean, nowMs: Long): Boolean` |
| `start/StartLayout.kt` | `data class InsetsDp(val left: Float, val top: Float, val right: Float, val bottom: Float)` |
| `start/StartLayout.kt` | `object StartLayout` |
| `start/StartLayout.kt` | `fun targets( rawWindowWidthDp: Float, rawWindowHeightDp: Float, insets: InsetsDp, fontScale: Float, firstPageText: String, lightSplash: Boolean ): StartTargets` |
| `start/StartLayout.kt` | `@Composable internal fun rememberStartTargets( maxWidth: Dp, maxHeight: Dp, firstPageText: String, lightSplash: Boolean ): StartTargets` |
| `start/StartOverlay.kt` | `@Composable internal fun StartBackdrop(frame: State<StartFrame>, targets: StartTargets, modifier: Modifier = Modifier)` |
| `start/StartOverlay.kt` | `@Composable internal fun StartOverlay( frame: State<StartFrame>, targets: StartTargets, onSkip: () -> Unit, modifier: Modifier = Modifier )` |
| `start/StartOverlay.kt` | `@Composable internal fun StartSplashFade( targets: StartTargets, alpha: () -> Float, onSkip: () -> Unit, modifier: Modifier = Modifier )` |
| `start/SplashHost.kt` | `@Composable fun SplashHost( firstPageText: String, lightSplash: Boolean, shouldPlay: Boolean, reduceMotion: Boolean, modifier: Modifier = Modifier, progressMs: Float? = null, onFirstFrame: () -> Unit = {}, onFinished: () -> Unit = {}, content: @Composable (intro: WriterIntro?) -> Unit )` |
| `writer/WriterIntro.kt` | `class WriterIntro( val paperOffsetDp: () -> Float, val textAlpha: () -> Float, val topBarAlpha: () -> Float, val ownPartsAlpha: () -> Float )` |
| `writer/WriterIntro.kt` | `fun Modifier.introPaper(intro: WriterIntro?): Modifier` |
| `writer/WriterIntro.kt` | `fun Modifier.introAlpha(alpha: (() -> Float)?): Modifier` |
| `writer/CaretGeometry.kt` | `object CaretGeometry` |
| `writer/CaretGeometry.kt` | `fun leftDp(geometry: WriterGeometry, col: Int): Float = geometry.textLeftDp + col * geometry.columnWidthDp` |
| `writer/CaretGeometry.kt` | `fun topDp(geometry: WriterGeometry): Float` |
| `writer/RememberReduceMotion.kt` | `@Composable fun rememberReduceMotion(reduceEffects: Boolean): Boolean` |
| `MainViewModel.kt` | `class MainViewModel( context: Context, val container: AppContainer = DefaultAppContainer(context.applicationContext), ioDispatcher: CoroutineDispatcher = Dispatchers.IO ) : ViewModel()` |
| `MainViewModel.kt` | `fun navigate(route: Route)` |
| `MainViewModel.kt` | `fun handleBack(): Boolean` |
| `MainViewModel.kt` | `fun factory( context: Context, container: AppContainer? = null, ioDispatcher: CoroutineDispatcher = Dispatchers.IO ) = object : ViewModelProvider.Factory` |

`WriterScreen` gained one parameter, `intro: WriterIntro? = null`; `Caret`, `Rail` and the writer's top bar take the intro through `introAlpha`, the paper through `introPaper`. `Rail` has the test tag `rail_rod` and `Caret` has `writer_caret`.

## 3. Tests

**Total: 659 {'app': 311, 'audio': 42, 'data': 85, 'design': 169, 'text': 52} fail 0 skipped 0

Per class (from the Gradle XML reports, summed by `counts.py`):

| Module | Test class | Tests | Failures | Skipped |
|---|---|---|---|---|
| `:app` | `AppSmokeTest` | 1 | 0 | 0 |
| `:app` | `MainViewModelTest` | 4 | 0 | 0 |
| `:app` | `ManifestTest` | 5 | 0 | 0 |
| `:app` | `ProvenanceTest` | 6 | 0 | 0 |
| `:app` | `LauncherIconTest` | 1 | 0 | 0 |
| `:app` | `CorruptionHandlerTest` | 1 | 0 | 0 |
| `:app` | `SettingsStoreTest` | 16 | 0 | 0 |
| `:app` | `GlassNavBarTest` | 10 | 0 | 0 |
| `:app` | `RouteTest` | 14 | 0 | 0 |
| `:app` | `ShellGoldenTest` | 5 | 0 | 0 |
| `:app` | `ShellScaffoldTest` | 8 | 0 | 0 |
| `:app` | `SystemBarIconsTest` | 3 | 0 | 0 |
| `:app` | `SplashHoldTest` | 5 | 0 | 0 |
| `:app` | `StartGateTest` | 3 | 0 | 0 |
| `:app` | `StartGoldenTest` | 12 | 0 | 0 |
| `:app` | `StartHandoffTest` | 10 | 0 | 0 |
| `:app` | `StartLayoutTest` | 10 | 0 | 0 |
| `:app` | `StartTimelineTest` | 21 | 0 | 0 |
| `:app` | `AssembledScreenGoldenTest` | 2 | 0 | 0 |
| `:app` | `AutosaverTest` | 8 | 0 | 0 |
| `:app` | `CarriageGoldenTest` | 7 | 0 | 0 |
| `:app` | `HintPolicyTest` | 6 | 0 | 0 |
| `:app` | `ImeBridgeTest` | 20 | 0 | 0 |
| `:app` | `MarkerGestureTest` | 18 | 0 | 0 |
| `:app` | `MotionPolicyTest` | 3 | 0 | 0 |
| `:app` | `NightThemeTest` | 6 | 0 | 0 |
| `:app` | `PaperGoldenTest` | 9 | 0 | 0 |
| `:app` | `PaperTextTest` | 12 | 0 | 0 |
| `:app` | `TapCaretPolicyTest` | 7 | 0 | 0 |
| `:app` | `TextSyncTest` | 5 | 0 | 0 |
| `:app` | `TopBarFormatterTest` | 3 | 0 | 0 |
| `:app` | `TypeSlugTest` | 9 | 0 | 0 |
| `:app` | `TypingPolicyTest` | 4 | 0 | 0 |
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
| `:design` | `DesignSmokeTest` | 1 | 0 | 0 |
| `:design` | `FaderMathTest` | 8 | 0 | 0 |
| `:design` | `GrainTest` | 8 | 0 | 0 |
| `:design` | `JitterTest` | 5 | 0 | 0 |
| `:design` | `LampColorTest` | 7 | 0 | 0 |
| `:design` | `LampGeometryTest` | 8 | 0 | 0 |
| `:design` | `MaterialsTest` | 13 | 0 | 0 |
| `:design` | `TokensTest` | 4 | 0 | 0 |
| `:design` | `GlassGoldenTest` | 6 | 0 | 0 |
| `:design` | `LampGoldenTest` | 3 | 0 | 0 |
| `:design` | `LogoGoldenTest` | 4 | 0 | 0 |
| `:design` | `MaterialsGoldenTest` | 1 | 0 | 0 |
| `:design` | `PanelGoldenTest` | 9 | 0 | 0 |
| `:design` | `LogoSpecTest` | 11 | 0 | 0 |
| `:design` | `LensControllerTest` | 13 | 0 | 0 |
| `:design` | `PhysicsTest` | 12 | 0 | 0 |
| `:design` | `SpringSimulationTest` | 6 | 0 | 0 |
| `:design` | `GlassBackendSelectionTest` | 10 | 0 | 0 |
| `:design` | `ScrollProgressTest` | 4 | 0 | 0 |
| `:design` | `ContrastTest` | 10 | 0 | 0 |
| `:design` | `DynamicShellColorsTest` | 8 | 0 | 0 |
| `:design` | `EnkShellThemeTest` | 2 | 0 | 0 |
| `:design` | `ShellPalettesTest` | 16 | 0 | 0 |
| `:text` | `GridLayoutTest` | 19 | 0 | 0 |
| `:text` | `PaginatorTest` | 21 | 0 | 0 |
| `:text` | `StruckPageTest` | 11 | 0 | 0 |
| `:text` | `TextSmokeTest` | 1 | 0 | 0 |

Tests broken on purpose and watched failing (then restored): the nav bar's stale callback; the start layout ignoring the top inset; the status bar icon flag inverted.

## 4. Goldens

Every PNG was opened and looked at. `StartGoldenTest` renders the whole animation (backdrop, a stand-in writer built from the real `PaperSheet`, `PaperText`, `Caret`, `Rail`, `Carriage` and the real `introPaper` and `introAlpha` modifiers, and the overlay) at 0, 200, 330, 460, 630 and 900 ms, dark and light. What they show: the logo on the splash colour at 0; the rail at full width and the typed lines gone by 200; the paper rising from the bottom at 330 with the rail still the logo's; the paper landed with the text fading in and the carriage still logo-sized at 460; the carriage and caret nearly home and the top bar fading in at 630; the finished writer at 900. In the light variant the dark chassis grows as a circle (already covering the screen by 200 ms, as the spec's curve gives). What they do not show: the system splash (Paparazzi cannot render it, so t = 0 is the overlay's first frame, not a comparison with the splash), or the real `WriterScreen` (it needs Room and an IME).

The five `ShellGoldenTest` images are unchanged from the implementer's version.

## 5. Scopes, jobs, listeners and resources created in this batch

| Created | Released |
|---|---|
| `SplashHost` progress `Animatable` and its `LaunchedEffect`s | with the composition; ends itself when the animation finishes or is skipped |
| `MainActivity` post-delayed `releaseSplashRunnable` | removed in `onDestroy` |
| `SplashScreen` exit listener and `systemSplash` view | the view is removed on the first composed frame or after 300 ms |
| `BackHandler` in `SplashHost` | only while the animation plays |
| `MainViewModel` init coroutine | `viewModelScope`, cancelled on clear; `onCleared` releases the sound player and closes the database |
| `WriterViewModel` per note id | activity `ViewModelStore` (unchanged from Batch 6) |
| `testContainer` single-thread executor, DataStore and database | tests only; they live until the JVM ends |

## 6. Deviations and gaps

- Compose tests are Robolectric tests in `app/src/test`, not `androidTest` (no emulator in the implementer's environment). `GlassNavBarTest`, `ShellScaffoldTest`, `StartHandoffTest` and `SystemBarIconsTest` are excluded from `testReleaseUnitTest` like the earlier Compose tests (the auditor did not widen that list further).
- `StartOverlay` is two layers plus a reduced-motion layer, not one canvas: the paper has to sit between the chassis and the rail, so one canvas above the writer cannot draw it correctly. Progress is still one animated value read only in the draw phase (`theWriterIsNotRecomposedOncePerFrame`).
- `core-splashscreen` 1.0.1 was added without asking first (documented in `docs/dependencies.md`; both AAR metadata checks pass).
- **Device pass (Android 17 emulator `Enk_API_37`, API 37, host GPU; the earlier Android 16 emulator was replaced because it crashed and stalled).** Done: a cold start recorded at 5x animator scale and read frame by frame (system splash, chassis circle, rail stretch and drop, paper rising, carriage gliding, text, top bar, hint, then the keyboard); wordmark to Box, Find, Setup, Back to the writer (text kept), nav Write; rotation (text and counter kept); the lamp (night theme, beam and control panel above the keyboard); a touch during the animation skips it; fast typing with `adb input text` (19 of 19 correct on a warm emulator). Not done: sound, haptics, swipe typing, TalkBack, real frame pacing at 1x, nav-bar drag by hand.
- **Typing under load.** Early in this session, right after installing, `adb input text "hello world"` came out as `hlello` and `helo world` in 2 of 4 runs while the emulator was busy; after warm-up it was correct in 19 of 19 clean runs and 18 of 18 runs with logging (a log of every IME callback showed no divergence). The code path is the Batch 6 one and was not changed here. Treat as a watch item on a real phone.
- **Landscape with the keyboard up** leaves the paper about 40 dp tall and the hint overlaps the text (Batch 6 layout, not changed here). The text and counter survive rotation.

## 7. Gate

```
JAVA_HOME=<JDK 21> ./gradlew.bat clean build test :design:verifyPaparazziDebug :app:verifyPaparazziDebug :app:processDebugManifest :app:checkDebugAarMetadata :app:checkReleaseAarMetadata
BUILD SUCCESSFUL in 5m 30s
414 actionable tasks: 390 executed, 24 up-to-date
```
