# Batch 7 Handoff: App Shell, Navigation, and Start Animation

Handoff report for **Batch 7** (Tasks 20A and 20B) on branch `batch-7/shell-and-start`.
Signatures are copied directly from source, test counts are taken from Gradle XML test reports produced during the final clean gate, and golden snapshots were visually reviewed.

---

## 1. Branch and Commits

- **Base commit:** `fe01bf5` (`main`, Batch 6 approved and merged)
- **Branch:** `batch-7/shell-and-start`
- **Commits:**
  - `5c6cc20`: `feat(shell): scaffold, large title, compact bar, glass nav bar, routes` (Task 20A)
  - `8232be0`: `feat(start): splash hand-off and rail-becomes-the-writer animation` (Task 20B)
  - `7d0a3b8`: `fix(build): exclude Compose UI tests from release unit test suite` (Build fix)

---

## 2. Signatures (copied from the code)

| Unit / Contract | Signature | File |
|---|---|---|
| `Dest` | `enum class Dest { Write, Box, Find, Setup }` | `app/.../shell/Route.kt` |
| `Route` | `sealed interface Route { object Writer; object Box; object Find; object Setup; object Trash; data class Note(val id: String, val page: Int = 0) }` | `app/.../shell/Route.kt` |
| `navSelection` | `fun navSelection(route: Route): Dest?` | `app/.../shell/Route.kt` |
| `routeBack` | `fun routeBack(current: Route): Route?` | `app/.../shell/Route.kt` |
| `LargeTitle` | `@Composable fun LargeTitle(title: String, meta: String? = null, modifier: Modifier = Modifier)` | `app/.../shell/LargeTitle.kt` |
| `CompactBar` | `@Composable fun CompactBar(title: String, progress: Float, glassSource: GlassSource, modifier: Modifier = Modifier)` | `app/.../shell/CompactBar.kt` |
| `GlassNavBar` | `@Composable fun GlassNavBar(selected: Dest, onSelect: (Dest) -> Unit, glassSource: GlassSource? = null, modifier: Modifier = Modifier)` | `app/.../shell/GlassNavBar.kt` |
| `ShellScaffold` | `@Composable fun ShellScaffold(title: String, meta: String? = null, showNav: Boolean = true, selected: Dest? = null, onNavigate: (Dest) -> Unit = {}, listState: LazyListState = rememberLazyListState(), content: @Composable (listState: LazyListState, contentPadding: PaddingValues) -> Unit)` | `app/.../shell/ShellScaffold.kt` |
| `EnkNavHost` | `@Composable fun EnkNavHost(container: AppContainer, mainViewModel: MainViewModel? = null, modifier: Modifier = Modifier)` | `app/.../shell/EnkNavHost.kt` |
| `StartTargets` | `data class StartTargets(val railY: Float, val screenWidth: Float, val screenHeight: Float, val carriageX: Float, val caretRect: Rect, val paperHeight: Float, val lightSplash: Boolean)` | `app/.../start/StartTimeline.kt` |
| `StartFrame` | `data class StartFrame(val apertureRadius: Float, val rail: Rect, val linesAlpha: Float, val chassisMix: Float, val chassisCircleRadius: Float, val paperOffsetY: Float, val carriage: CarriageFrame, val caret: Rect, val textAlpha: Float, val topBarAlpha: Float, val overlayAlpha: Float, val ownPartsAlpha: Float)` | `app/.../start/StartTimeline.kt` |
| `CarriageFrame` | `data class CarriageFrame(val centre: Offset, val scale: Float)` | `app/.../start/StartTimeline.kt` |
| `StartTimeline` | `object StartTimeline { const val DURATION_MS = 900; fun at(tMs: Float, targets: StartTargets): StartFrame }` | `app/.../start/StartTimeline.kt` |
| `StartGate` | `object StartGate { fun shouldPlay(coldStart: Boolean, launchedFromWidgetOrIntent: Boolean, restoredState: Boolean, loadMs: Long): Boolean; fun reducedMotionDurationMs(): Int = 250 }` | `app/.../start/StartGate.kt` |
| `StartOverlay` | `@Composable fun StartOverlay(targets: StartTargets, modifier: Modifier = Modifier, reduceMotion: Boolean = false, timeMs: Float? = null, onFinished: () -> Unit = {})` | `app/.../start/StartOverlay.kt` |
| `SplashHost` | `@Composable fun SplashHost(targets: StartTargets?, shouldPlayAnimation: Boolean, reduceMotion: Boolean, onAnimationFinished: () -> Unit, content: @Composable () -> Unit)` | `app/.../start/SplashHost.kt` |

---

## 3. Implementation Details & Design System Compliance

### Task 20A: App Shell and Navigation (DS 12.6, 12.7, 12.8)
- **Routes & Navigation**:
  - `Dest` items in canonical order: `Write`, `Box`, `Find`, `Setup`.
  - `navSelection`: `Writer` and `Note` map to `Dest.Write`; `Box` and `Trash` map to `Dest.Box`; `Find` maps to `Dest.Find`; `Setup` maps to `Dest.Setup`.
  - `routeBack`: `Box`, `Find`, `Setup` pop to `Writer`; `Trash` and `Note` pop to `Box`; `Writer` returns `null` (exits app).
  - Titled placeholder screens for Box, Find, Setup, and Trash show large title and empty-scaffold indication.
  - BackHandler integrated in `EnkNavHost`.
- **LargeTitle & CompactBar**:
  - `LargeTitle`: top padding 70 dp below status bar inset (`WindowInsets.statusBars`), horizontal 20 dp, bottom 12 dp. Title at 34 sp bold `t1`; meta right-aligned in Courier Prime at 12 sp `t3` with 6 dp bottom padding. Semantics `mergeDescendants = true` with tag `large_title`.
  - `CompactBar`: pinned at 58 dp height with status bar inset padding. Title centered at 26 dp, 17 sp bold `t1` with opacity driven by `collapseProgress(scrollY)`. Blur backdrop edge via `Modifier.scrollEdge(EdgeKind.CompactBar, progress, glassSource)`. Tag `compact_bar_title` with semantics `CompactTitleAlpha`.
- **GlassNavBar & Lens Physics**:
  - Floating pill: 64 dp height, 32 dp corner radius, 5 dp inner padding. Sits 22 dp above navigation bar inset (`WindowInsets.navigationBars`) and 22 dp from sides.
  - 4 destination tabs with touch targets >= 48x48 dp. Semantics: `Role.Tab`, `selected`, `contentDescription = dest.name`, enclosed in `Modifier.selectableGroup()`.
  - Custom line icons (22 dp, stroke 1.6 dp, round caps):
    - Write: carriage body with `LocalShellColors.current.ui` strip (never `#FF5A1F`).
    - Box: drawer box with top tab handle.
    - Find: magnifier circle with 45-degree handle.
    - Setup: three sliders with detent rings.
  - Interactive Lens: 54 dp height, 27 dp radius. LensController spring physics, top 0.5 dp edge highlight, dual drop shadows, live brightening under lens center to `t1` (`isHighlighted = (dest == (dragHighlightedDest ?: selected))`).
  - Active drag consumes pointer events (`change.consume()`); returns safely on drag cancel or outside bounds.
  - Respects `rememberGlassBackend() == GlassBackendKind.Solid` fallback.
- **Persistence Across Shell Switches**:
  - `WriterViewModel` stored in `MainActivity`'s `ViewModelStore` keyed by `"writer-$id"`, preserving typed text and caret when navigating to Box and back.

### Task 20B: Start Animation ("Rail becomes the writer")
- **Pure Timeline (`StartTimeline.at`)**:
  - Exact 900 ms animation driven by ease-in-out cubic `CubicBezierEasing(0.65f, 0f, 0.35f, 1f)` and linear windows from `docs/superpowers/specs/2026-11-07-enk-start-animation.md`.
  - Aperture radius expands from 96 dp to cover screen (0 to 350 ms).
  - Rail stretches from 72/256 dp to full width (0 to 350 ms), then descends from center Y 426.7 dp to writing height `targets.railY` and thins from 22.7 dp to 12 dp (250 to 650 ms).
  - Carriage glides from (218.7, 426.7) scale 1 to (`targets.carriageX`, `targets.railY + 1`) scale 0.54 (350 to 700 ms).
  - Caret bar morphs from (204, 359.3, 29.3, 5.6) to `targets.caretRect` (300 to 650 ms).
  - Dark splash fades into chassis (60 to 450 ms); Light splash grows chassis circle matching aperture (0 to 350 ms).
  - Paper rises from `targets.paperHeight` to 0 (150 to 600 ms). Note text fades in (450 to 700 ms). Top bar fades in (600 to 900 ms).
  - Cross-fade handoff at 650 to 750 ms; overlay finishes at 750 ms; real caret blinks at 800 ms.
- **StartGate & Interactions**:
  - Plays only on cold launches that load within 1500 ms; bypasses on warm start, widget/intent launch, restored state, or load time > 1500 ms.
  - Reduced motion: 250 ms fade with zero motion (`StartGate.reducedMotionDurationMs() == 250`).
  - Interactive skip: touching screen or pressing back eases animation to 1 over 120 ms.
  - Zero sound, zero haptics during startup.
- **Splash Integration (`MainActivity`)**:
  - `androidx.core:core-splashscreen:1.0.1` installed with `@style/Theme.Enk.Splash`.
  - Dark splash: `#161514` with `@drawable/ic_splash_symbol`.
  - Light splash: `#ECE8DF` with `@drawable/ic_splash_symbol_light`.
  - `setKeepOnScreenCondition` holds splash until settings and recent note load (`!isLoaded && elapsed < 1500L`).
  - `loadDurationMs` measured before handoff; dynamic targets compute `carriageX` and `caretRect` from restored note.

---

## 4. Golden Snapshots Review

All 15 golden snapshots (5 shell, 10 start animation) were recorded and verified with Paparazzi. Visual inspection confirms:

| Snapshot Test | Image | Verification & Review Notes |
|---|---|---|
| `ShellGoldenTest.navBarGraphiteDark` | `app.enk.shell_ShellGoldenTest_navBarGraphiteDark.png` | Dark graphite aura background; floating glass nav bar with 4 tabs; lens active over Write; Write strip in UI accent. |
| `ShellGoldenTest.navBarApricotLight` | `app.enk.shell_ShellGoldenTest_navBarApricotLight.png` | Warm Apricot light theme; paper cards beneath; glass nav bar clear and legible. |
| `ShellGoldenTest.navBarSlateDark` | `app.enk.shell_ShellGoldenTest_navBarSlateDark.png` | Slate dark palette; subtle glass gradient and crisp icon lines. |
| `ShellGoldenTest.navBarDynamicDark` | `app.enk.shell_ShellGoldenTest_navBarDynamicDark.png` | Dynamic color dark scheme; proper contrast across tabs. |
| `ShellGoldenTest.scaffoldTitleCollapsed` | `app.enk.shell_ShellGoldenTest_scaffoldTitleCollapsed.png` | Scrolled state: CompactBar pinned at top 58 dp with blurred edge and centered title "Box"; LargeTitle scrolled out; cards scrolled under bar; nav bar floating at bottom. |
| `StartGoldenTest.darkSplash_t0` | `app.enk.start_StartGoldenTest_darkSplash_t0.png` | t=0 ms: circular aperture mask (radius 96 dp) on dark background `#161514`; rail clipped; carriage and orange caret bar match splash vector. |
| `StartGoldenTest.darkSplash_t200` | `app.enk.start_StartGoldenTest_darkSplash_t200.png` | t=200 ms: aperture expanding; typed lines faded out; rail expanding towards screen width. |
| `StartGoldenTest.darkSplash_t460` | `app.enk.start_StartGoldenTest_darkSplash_t460.png` | t=460 ms: full width aperture; rail descending; carriage beginning glide; paper rising. |
| `StartGoldenTest.darkSplash_t630` | `app.enk.start_StartGoldenTest_darkSplash_t630.png` | t=630 ms: carriage and caret approaching writer targets; note text visible; paper landed. |
| `StartGoldenTest.darkSplash_t900` | `app.enk.start_StartGoldenTest_darkSplash_t900.png` | t=900 ms: complete handoff to writer screen; top bar visible; real writer caret ready. |
| `StartGoldenTest.lightSplash_t0` | `app.enk.start_StartGoldenTest_lightSplash_t0.png` | t=0 ms: light splash background `#ECE8DF`; ink typed lines; identical rail and carriage geometry. |
| `StartGoldenTest.lightSplash_t200` | `app.enk.start_StartGoldenTest_lightSplash_t200.png` | t=200 ms: dark writer chassis circle expanding from logo center matching aperture radius. |
| `StartGoldenTest.lightSplash_t460` | `app.enk.start_StartGoldenTest_lightSplash_t460.png` | t=460 ms: chassis circle covers screen; rail dropping to 62% height; carriage gliding. |
| `StartGoldenTest.lightSplash_t630` | `app.enk.start_StartGoldenTest_lightSplash_t630.png` | t=630 ms: carriage settling; caret at note position; paper text visible. |
| `StartGoldenTest.lightSplash_t900` | `app.enk.start_StartGoldenTest_lightSplash_t900.png` | t=900 ms: full transition to writer complete. |

---

## 5. Test Summary (from final clean gate XML reports)

**Total Test Count: 622 tests, 0 failures, 0 skipped** across all 5 modules:
- `:app`: 274 tests (68 new in Batch 7)
- `:design`: 169 tests
- `:data`: 85 tests
- `:text`: 52 tests
- `:audio`: 42 tests

### `:app` Module Test Suite (274 tests)

| Test Class | Tests | Failures | Skipped | Status |
|---|---|---|---|---|
| `app.enk.shell.RouteTest` | 14 | 0 | 0 | PASS |
| `app.enk.shell.GlassNavBarTest` | 9 | 0 | 0 | PASS |
| `app.enk.shell.ShellScaffoldTest` | 8 | 0 | 0 | PASS |
| `app.enk.shell.ShellGoldenTest` | 5 | 0 | 0 | PASS |
| `app.enk.start.StartTimelineTest` | 14 | 0 | 0 | PASS |
| `app.enk.start.StartGateTest` | 2 | 0 | 0 | PASS |
| `app.enk.start.StartHandoffTest` | 4 | 0 | 0 | PASS |
| `app.enk.start.StartGoldenTest` | 10 | 0 | 0 | PASS |
| `app.enk.MainViewModelTest` | 2 | 0 | 0 | PASS |
| `app.enk.writer.WriterGeometryTest` | 11 | 0 | 0 | PASS |
| `app.enk.writer.WriterReducerTest` | 26 | 0 | 0 | PASS |
| `app.enk.writer.WriterViewModelTest` | 11 | 0 | 0 | PASS |
| `app.enk.writer.WriterScreenTest` | 9 | 0 | 0 | PASS |
| `app.enk.writer.AutosaverTest` | 8 | 0 | 0 | PASS |
| `app.enk.writer.ImeBridgeTest` | 20 | 0 | 0 | PASS |
| `app.enk.writer.TextSyncTest` | 5 | 0 | 0 | PASS |
| `app.enk.writer.MarkerGestureTest` | 18 | 0 | 0 | PASS |
| `app.enk.writer.PaperTextTest` | 12 | 0 | 0 | PASS |
| `app.enk.writer.TypeSlugTest` | 9 | 0 | 0 | PASS |
| `app.enk.writer.PaperGoldenTest` | 9 | 0 | 0 | PASS |
| `app.enk.writer.CarriageGoldenTest` | 7 | 0 | 0 | PASS |
| `app.enk.writer.AssembledScreenGoldenTest` | 2 | 0 | 0 | PASS |
| `app.enk.writer.HintPolicyTest` | 6 | 0 | 0 | PASS |
| `app.enk.writer.MotionPolicyTest` | 3 | 0 | 0 | PASS |
| `app.enk.writer.NightThemeTest` | 6 | 0 | 0 | PASS |
| `app.enk.writer.TapCaretPolicyTest` | 7 | 0 | 0 | PASS |
| `app.enk.writer.TopBarFormatterTest` | 3 | 0 | 0 | PASS |
| `app.enk.writer.TypingPolicyTest` | 4 | 0 | 0 | PASS |
| `app.enk.settings.SettingsStoreTest` | 16 | 0 | 0 | PASS |
| `app.enk.settings.CorruptionHandlerTest` | 1 | 0 | 0 | PASS |
| `app.enk.logo.LauncherIconTest` | 1 | 0 | 0 | PASS |
| `app.enk.ProvenanceTest` | 6 | 0 | 0 | PASS |
| `app.enk.ManifestTest` | 5 | 0 | 0 | PASS |
| `app.enk.AppSmokeTest` | 1 | 0 | 0 | PASS |

---

## 6. Deviations & QA Notes

1. **Test Runner for Compose UI Tests**:
   - The plan specified `androidTest` for `ShellScaffoldTest`, `GlassNavBarTest`, and `StartHandoffTest`. As established in Prompt 2 and Batch 6, with no emulator running in the headless agent workspace, these tests are written as Robolectric Compose unit tests in `app/src/test`.
   - Comprehensive device verification procedures have been documented in:
     - [docs/qa/shell-device-checklist.md](file:///C:/Users/Khaled/Documents/Personal%20Projects/Enk/docs/qa/shell-device-checklist.md)
     - [docs/qa/start-device-checklist.md](file:///C:/Users/Khaled/Documents/Personal%20Projects/Enk/docs/qa/start-device-checklist.md)
2. **Release Unit Test Exclusion**:
   - In `app/build.gradle.kts`, `GlassNavBarTest`, `ShellScaffoldTest`, and `StartHandoffTest` (along with `MarkerGestureTest`, `WriterScreenTest`, `NightThemeTest`) are excluded from `testReleaseUnitTest` because `androidx.compose.ui.test.manifest` is `debugImplementation` only. They run and are fully verified in `testDebugUnitTest`.

---

## 7. Clean Gate Run & Log Tail

### Gate Execution Command:
```powershell
$env:JAVA_HOME = "D:\jdks\eclipse_adoptium-21-amd64-windows.2"; .\gradlew.bat clean build test :design:verifyPaparazziDebug :app:verifyPaparazziDebug :app:processDebugManifest :app:checkDebugAarMetadata :app:checkReleaseAarMetadata
& "C:\Program Files\Git\bin\bash.exe" scripts/check-no-network.sh
```

### Tail of Gate Run:
```
> Task :app:lintAnalyzeDebugUnitTest
> Task :app:testDebugUnitTest
See the Paparazzi report at: file:///C:/Users/Khaled/Documents/Personal%20Projects/Enk/app/build/reports/paparazzi/debug/index.html

> Task :app:processReleaseUnitTestJavaRes
> Task :app:compileReleaseUnitTestJavaWithJavac NO-SOURCE
> Task :app:verifyPaparazziDebug

> Task :app:lintReportDebug
Wrote HTML report to file:///C:/Users/Khaled/Documents/Personal%20Projects/Enk/app/build/reports/lint-results-debug.html

> Task :app:lintDebug
> Task :app:lint
> Task :app:transformReleaseUnitTestClassesWithAsm
> Task :app:testReleaseUnitTest
See the Paparazzi report at: file:///C:/Users/Khaled/Documents/Personal%20Projects/Enk/app/build/reports/paparazzi/release/index.html

> Task :app:test
> Task :app:check
> Task :app:build

BUILD SUCCESSFUL in 8m 10s
414 actionable tasks: 390 executed, 24 up-to-date

Checking manifest: app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml
SUCCESS: No network permissions declared in merged manifest.
```
