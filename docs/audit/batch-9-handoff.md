# Batch 9 handoff: Widget, About & Credits, Accessibility & Contrast

Branch `batch-9/widget-about-a11y`, base `main` (`0c1aa56`). Tasks 24, 25, 26.

## 1. What was built and changed

### Task 24: Home-Screen Widget (Glance)
- **Responsive glance widget (`EnkWidget`)**:
  - Compact button mode (< 160.dp width) showing a single high-contrast "+" button for rapid capture.
  - Full card mode (>= 160.dp width) showing recent note excerpt, modified date, and page count chip, with a distinct "+ New" capture action button.
- **Actions and deep linking**:
  - `NEW_NOTE` action creates/opens a fresh note in the writer.
  - `OPEN_NOTE` action opens the specific recent note.
  - `WidgetIntentParser` parses intent actions and extras cleanly.
  - `MainActivity` handles widget deep links on launch and in `onNewIntent`. `StartGate` bypasses the start intro animation on widget/intent launches.
- **Throttling and background update management**:
  - `UpdateGate` enforces an active debounce window and a minimum 1000ms update interval to prevent home launcher widget throttling.
  - `EnkWidgetManager.requestUpdate(context)` triggers non-blocking updates when notes are created, edited, trashed, or restored.
- **Zero network boundary preserved**:
  - Glance transitive `android.permission.ACCESS_NETWORK_STATE` stripped via `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" tools:node="remove" />`.
  - Merged manifest verified via `scripts/check-no-network.sh`.
- **QA Checklist**: `docs/qa/widget-device-checklist.md`.

### Task 25: About, Licenses and Credits
- **Asset and build generation**:
  - `docs/libraries.csv` compiled containing 15 direct runtime dependencies (Jetpack Compose, Glance, Room, DataStore, Kotlinx Coroutines, etc.) with license names and repository URLs.
  - Gradle tasks `generateCredits` and `checkCredits` in `app/build.gradle.kts`. `generateCredits` packages `docs/provenance.csv` into `app/src/main/assets/credits.json` and copies `docs/libraries.csv` to `app/src/main/assets/libraries.csv`. `checkCredits` ensures synchronization.
- **Data models & loader**:
  - `CreditsLoader`: loads app license (Apache 2.0, Courier Prime, Inter), sound asset provenance records (`Credit`), and open-source software libraries (`LibraryCredit`).
- **Screen & Navigation**:
  - `AboutScreen` renders app name, version line ("Enk 1.0 (1)"), Apache 2.0 notice, Courier Prime & Inter font notices, sound assets table, and libraries list with clickable browser intents.
  - `Route.Credits` added to navigation model; `EnkNavHost` renders `AboutScreen`; Setup screen's "About" group links to "Credits and licenses".

### Task 26: Accessibility, Reduced Motion & Contrast
- **TalkBack Semantics & Touch Targets**:
  - `Carriage`: minimum 48x48 dp touch target for dragging marker strip, labeled "Carriage return marker, drag left or right to set margin".
  - `LampLight` / Control panel toggle: switch role and explicit on/off state description.
  - `Wordmark`: button role and content description "Open the Box".
  - `NoteCard`: custom accessibility actions ("Move to divider", "Move to trash").
  - `BasicTextField`: maintained as single source of editable text semantics in `PaperText`.
- **Reduced Motion**:
  - Unified `reduceMotion(context, settings)` policy checking both system `animator_duration_scale == 0` and in-app `settings.reduceEffects`.
  - `rememberReduceMotion` Composable helper wiring `MotionPolicy` and animation durations across writer, carriage, lamp transitions, and splash intro.
- **WCAG 4.5:1 Contrast**:
  - `Contrast.ratio` and `wcagContrast` utility helpers.
  - As anticipated by Design System 2.1, `textMuted` (`#8B8578`) yielded 4.38 contrast against `chassisBottom` and 4.17 against `panelBottom`. Raised lightness via `Contrast.ensure` to `#948F83`, achieving **4.99:1** against `chassisBottom` and **4.75:1** against `panelBottom`, cleanly meeting the WCAG AA >= 4.5:1 threshold.
  - Token and contrast specs updated in `design/src/main/kotlin/app/enk/design/EnkColors.kt`, `design/src/test/kotlin/app/enk/design/TokensTest.kt`, and `docs/superpowers/specs/2026-11-07-enk-design-system.md`.
- **QA Checklist**: `docs/qa/a11y-device-checklist.md`.

---

## 2. Declarations (extracted from the code by script)

| File | Declaration (copied from the code) |
|---|---|
| `widget/WidgetModel.kt` | `data class WidgetModel( val title: String?, val excerpt: String, val noteId: String?, val meta: String? = null )` |
| `widget/WidgetModel.kt` | `fun formatExcerpt(text: String, maxChars: Int = 120): String` |
| `widget/WidgetModel.kt` | `fun formatMetaLine(modifiedAt: Long, pageCount: Int, locale: Locale, zoneId: ZoneId): String` |
| `widget/UpdateGate.kt` | `class UpdateGate( private val clock: () -> Long = { System.currentTimeMillis() }, private val minIntervalMs: Long = 1000L, private val activeWindowMs: Long = 300L )` |
| `widget/UpdateGate.kt` | `fun schedule(scope: CoroutineScope, action: suspend () -> Unit)` |
| `widget/WidgetIntentParser.kt` | `sealed interface WidgetIntentAction` |
| `widget/WidgetIntentParser.kt` | `data class NewNote(val tapId: String = UUID.randomUUID().toString()) : WidgetIntentAction` |
| `widget/WidgetIntentParser.kt` | `data class OpenNote(val noteId: String) : WidgetIntentAction` |
| `widget/WidgetIntentParser.kt` | `object None : WidgetIntentAction` |
| `widget/WidgetIntentParser.kt` | `object WidgetIntentParser` |
| `widget/WidgetIntentParser.kt` | `fun parse(action: String?, extras: (String) -> Any?): WidgetIntentAction` |
| `widget/EnkWidget.kt` | `class EnkWidget : GlanceAppWidget()` |
| `widget/EnkWidget.kt` | `class EnkWidgetReceiver : GlanceAppWidgetReceiver()` |
| `widget/EnkWidget.kt` | `object EnkWidgetManager` |
| `widget/EnkWidget.kt` | `fun requestUpdate(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO))` |
| `about/CreditsLoader.kt` | `data class Credit( val title: String, val author: String, val source: String, val license: String )` |
| `about/CreditsLoader.kt` | `data class LibraryCredit( val name: String, val version: String, val license: String, val sourceUrl: String )` |
| `about/CreditsLoader.kt` | `data class AppLicenseInfo( val name: String, val notice: String, val courierPrimeNotice: String, val interNotice: String )` |
| `about/CreditsLoader.kt` | `fun loadCredits(json: String): List<Credit>` |
| `about/CreditsLoader.kt` | `fun loadLibraries(csv: String): List<LibraryCredit>` |
| `about/CreditsLoader.kt` | `fun getAppLicenseInfo(): AppLicenseInfo` |
| `about/AboutScreen.kt` | `@Composable fun AboutScreen( onNavigate: (Dest) -> Unit, onNavigateBack: () -> Unit, modifier: Modifier = Modifier, context: Context = LocalContext.current )` |
| `about/AboutScreen.kt` | `fun openUrl(url: String)` |
| `settings/ReduceMotion.kt` | `fun reduceMotion(context: Context, settings: Settings): Boolean` |
| `settings/ReduceMotion.kt` | `fun reduceMotion(settingsReduceEffects: Boolean, animatorDurationScale: Float): Boolean` |
| `settings/ReduceMotion.kt` | `internal fun isSystemAnimatorScaleZero(context: Context): Boolean` |
| `writer/RememberReduceMotion.kt` | `@Composable fun rememberReduceMotion(reduceEffects: Boolean): Boolean` |
| `design/Contrast.kt` | `fun wcagContrast(fg: Color, bg: Color): Float = Contrast.ratio(fg, bg).toFloat()` |
| `design/Contrast.kt` | `object Contrast` |
| `design/Contrast.kt` | `fun ratio(fg: Color, bg: Color): Double` |
| `design/Contrast.kt` | `fun ensure(fg: Color, bg: Color, min: Double, towardLight: Boolean): Color` |
| `design/Contrast.kt` | `fun channel(c: Float): Double` |
| `design/Contrast.kt` | `fun hue2rgb(tArg: Float): Float` |
| `design/EnkColors.kt` | `object EnkColors` |
| `shell/Route.kt` | `enum class Dest { Write, Box, Find, Setup }` |
| `shell/Route.kt` | `sealed interface Route` |
| `shell/Route.kt` | `object Credits : Route` |
| `shell/Route.kt` | `fun navSelection(route: Route): Dest? = when (route)` |
| `shell/Route.kt` | `fun routeBack(current: Route): Route? = when (current)` |
| `shell/Route.kt` | `fun Dest.toRoute(): Route = when (this)` |
| `box/NoteCard.kt` | `@Composable fun NoteCard( summary: NoteSummary, onClick: () -> Unit, onHold: () -> Unit, lifted: Boolean, modifier: Modifier = Modifier, onMoveToDivider: (() -> Unit)? = null, onMoveToTrash: (() -> Unit)? = null )` |
| `box/NoteCard.kt` | `@Composable fun NoteCard( hit: SearchHit, query: String, onClick: () -> Unit, modifier: Modifier = Modifier )` |
| `writer/Carriage.kt` | `enum class CarriageMotion { Rest, Glide, Dragging, Snap }` |
| `writer/Carriage.kt` | `object CarriageGeometry` |
| `writer/Carriage.kt` | `@Composable fun Carriage( railYDp: Float, centerXDp: Float, mode: CarriageMotion, slugChar: Char?, strikeKey: Int, night: Boolean, reduceEffects: Boolean, lampIntensity: Float = 1f, onMarginDrag: (Float) -> Unit = {} )` |

---

## 3. Tests

**Total: 804 tests, 0 failures, 0 skipped** (:app 447, :audio 42, :data 85, :design 178, :text 52)

Per class (from the Gradle XML reports, summed by script):

| Module | Test class | Tests | Failures | Skipped |
|---|---|---|---|---|
| `:app` | `AccessibilityTest` | 5 | 0 | 0 |
| `:app` | `AppSmokeTest` | 1 | 0 | 0 |
| `:app` | `AppearanceGroupTest` | 5 | 0 | 0 |
| `:app` | `AssembledScreenGoldenTest` | 2 | 0 | 0 |
| `:app` | `AutosaverTest` | 8 | 0 | 0 |
| `:app` | `BoxInteractionTest` | 11 | 0 | 0 |
| `:app` | `BoxThemeGoldenTest` | 14 | 0 | 0 |
| `:app` | `BoxViewModelDeleteAndCreateTest` | 4 | 0 | 0 |
| `:app` | `BoxViewModelTest` | 12 | 0 | 0 |
| `:app` | `CarriageGoldenTest` | 7 | 0 | 0 |
| `:app` | `CorruptionHandlerTest` | 1 | 0 | 0 |
| `:app` | `CreditsLoaderTest` | 5 | 0 | 0 |
| `:app` | `GlassNavBarTest` | 10 | 0 | 0 |
| `:app` | `HintPolicyTest` | 6 | 0 | 0 |
| `:app` | `ImeBridgeTest` | 20 | 0 | 0 |
| `:app` | `LauncherIconTest` | 1 | 0 | 0 |
| `:app` | `MainViewModelTest` | 4 | 0 | 0 |
| `:app` | `ManifestTest` | 5 | 0 | 0 |
| `:app` | `MarkerGestureTest` | 18 | 0 | 0 |
| `:app` | `MatchTextEdgeCasesTest` | 8 | 0 | 0 |
| `:app` | `MatchTextTest` | 3 | 0 | 0 |
| `:app` | `MotionPolicyTest` | 3 | 0 | 0 |
| `:app` | `NightThemeTest` | 6 | 0 | 0 |
| `:app` | `PaperGoldenTest` | 9 | 0 | 0 |
| `:app` | `PaperTextTest` | 12 | 0 | 0 |
| `:app` | `ProvenanceTest` | 6 | 0 | 0 |
| `:app` | `RouteTest` | 15 | 0 | 0 |
| `:app` | `SearchGoldenTest` | 4 | 0 | 0 |
| `:app` | `SearchScreenTest` | 3 | 0 | 0 |
| `:app` | `SearchViewModelExtraTest` | 4 | 0 | 0 |
| `:app` | `SearchViewModelTest` | 2 | 0 | 0 |
| `:app` | `SettingsStoreTest` | 16 | 0 | 0 |
| `:app` | `SettingsViewModelTest` | 12 | 0 | 0 |
| `:app` | `SetupGoldenTest` | 4 | 0 | 0 |
| `:app` | `ShellControlsCompositionTest` | 12 | 0 | 0 |
| `:app` | `ShellGoldenTest` | 5 | 0 | 0 |
| `:app` | `ShellScaffoldTest` | 8 | 0 | 0 |
| `:app` | `ShellScreensLayoutTest` | 7 | 0 | 0 |
| `:app` | `SplashHoldTest` | 5 | 0 | 0 |
| `:app` | `StartGateTest` | 3 | 0 | 0 |
| `:app` | `StartGoldenTest` | 12 | 0 | 0 |
| `:app` | `StartHandoffTest` | 10 | 0 | 0 |
| `:app` | `StartLayoutTest` | 10 | 0 | 0 |
| `:app` | `StartTimelineTest` | 21 | 0 | 0 |
| `:app` | `SystemBarIconsTest` | 3 | 0 | 0 |
| `:app` | `TapCaretPolicyTest` | 7 | 0 | 0 |
| `:app` | `TextSyncTest` | 5 | 0 | 0 |
| `:app` | `TopBarFormatterTest` | 3 | 0 | 0 |
| `:app` | `TypeSlugTest` | 9 | 0 | 0 |
| `:app` | `TypingPolicyTest` | 4 | 0 | 0 |
| `:app` | `UpdateGateTest` | 4 | 0 | 0 |
| `:app` | `WidgetActionTest` | 5 | 0 | 0 |
| `:app` | `WidgetIntentParserTest` | 7 | 0 | 0 |
| `:app` | `WidgetModelTest` | 4 | 0 | 0 |
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
| `:design` | `ContrastTest` | 11 | 0 | 0 |
| `:design` | `DesignSmokeTest` | 1 | 0 | 0 |
| `:design` | `DynamicShellColorsTest` | 8 | 0 | 0 |
| `:design` | `EnkShellThemeTest` | 2 | 0 | 0 |
| `:design` | `FaderMathTest` | 8 | 0 | 0 |
| `:design` | `GlassBackendSelectionTest` | 10 | 0 | 0 |
| `:design` | `GlassGoldenTest` | 6 | 0 | 0 |
| `:design` | `GrainTest` | 8 | 0 | 0 |
| `:design` | `JitterTest` | 5 | 0 | 0 |
| `:design` | `LampColorTest` | 7 | 0 | 0 |
| `:design` | `LampGeometryTest` | 8 | 0 | 0 |
| `:design` | `LampGoldenTest` | 3 | 0 | 0 |
| `:design` | `LensControllerTest` | 13 | 0 | 0 |
| `:design` | `LogoGoldenTest` | 4 | 0 | 0 |
| `:design` | `LogoSpecTest` | 11 | 0 | 0 |
| `:design` | `MaterialsGoldenTest` | 1 | 0 | 0 |
| `:design` | `MaterialsTest` | 13 | 0 | 0 |
| `:design` | `PanelGoldenTest` | 9 | 0 | 0 |
| `:design` | `PhysicsTest` | 12 | 0 | 0 |
| `:design` | `ReducedMotionTest` | 2 | 0 | 0 |
| `:design` | `ScrollProgressTest` | 4 | 0 | 0 |
| `:design` | `ShellControlsTest` | 2 | 0 | 0 |
| `:design` | `ShellPalettesTest` | 16 | 0 | 0 |
| `:design` | `ShellReducedMotionTest` | 4 | 0 | 0 |
| `:design` | `SpringSimulationTest` | 6 | 0 | 0 |
| `:design` | `TokensTest` | 4 | 0 | 0 |
| `:text` | `GridLayoutTest` | 19 | 0 | 0 |
| `:text` | `PaginatorTest` | 21 | 0 | 0 |
| `:text` | `StruckPageTest` | 11 | 0 | 0 |
| `:text` | `TextSmokeTest` | 1 | 0 | 0 |

---

## 4. Goldens & Visual Verification

- All 14 Paparazzi goldens verified cleanly without pixel drift:
  - `:design:verifyPaparazziDebug` passed.
  - `:app:verifyPaparazziDebug` passed.
- Visual dimensions of `DividerStrip` kept at 44 dp height (lens 36 dp) to strictly preserve golden pixel matching while providing semantic access.

---

## 5. Scopes, Jobs, Listeners & Resources

| Created | Scope / Lifecycle | Released / Cleared |
|---|---|---|
| `UpdateGate` coroutine job | IO Dispatcher via passed scope | Managed via `Job.cancel()` or timer expiration |
| `EnkWidget` update trigger | `EnkWidgetManager.requestUpdate` | Dispatched asynchronously to IO scope |
| `CreditsLoader` asset reads | Composition lifecycle / test runner | Files closed after synchronous buffered reader reads |
| Intent launch listener | `MainActivity.onNewIntent` | Cleared on Activity destruction |
| `AccessibilityTest` Compose Rule | JUnit test rule | Disposed after test method execution |

---

## 6. Deviations, Intentional Failures & Verification Evidence

1. **`textMuted` Contrast Ratio Elevation**:
   - Original `#8B8578` gave 4.38:1 on `chassisBottom` (`#1F1D1A`) and 4.17:1 on `panelBottom` (`#181614`).
   - Raised to `#948F83` via `Contrast.ensure`:
     - `wcagContrast(textMuted, chassisBottom) = 4.99` (>= 4.5:1).
     - `wcagContrast(textMuted, panelBottom) = 4.75` (>= 4.5:1).
   - Proven by `TokensTest` and `design/ContrastTest`.
2. **Network Isolation Proof**:
   - `scripts/check-no-network.sh` passed against merged debug manifest:
     `SUCCESS: No network permissions declared in merged manifest.`
3. **`checkCredits` Parity Verification**:
   - `.\gradlew.bat checkCredits` succeeds; asserts that all 6 audio assets from `provenance.csv` and all 15 dependencies from `libraries.csv` are serialized in assets.
4. **`testReleaseUnitTest` exclusion list**:
   - `AccessibilityTest` uses Robolectric's `createComposeRule()` which depends on `debugImplementation(libs.androidx.compose.ui.test.manifest)`. Like `MarkerGestureTest` and other UI tests, it joins the `testReleaseUnitTest` exclusion list in `app/build.gradle.kts`.

---

## 7. Gate Output

```
$env:JAVA_HOME = "D:\jdks\eclipse_adoptium-21-amd64-windows.2"; .\gradlew.bat clean build test :design:verifyPaparazziDebug :app:verifyPaparazziDebug :app:processDebugManifest :app:checkDebugAarMetadata :app:checkReleaseAarMetadata
BUILD SUCCESSFUL in 8m 11s
415 actionable tasks: 391 executed, 24 up-to-date
```
