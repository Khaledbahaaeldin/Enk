# Batch 8 handoff: The Box, Find / Search, and Setup

Branch `batch-8/box-find-setup`, base `main` (`21f2d68`). Tasks 21, 22, and 23.
This document is generated directly from real code signatures, Gradle XML test reports, and verification gate outputs.

---

## 1. Scope & Summary of Completed Work

Batch 8 replaces the titled placeholders in `EnkNavHost` with fully implemented screens:

- **Task 21: The Box & Trash (`app.enk.box`)**
  - Large title "The Box" with live note counter and `DividerStrip` ("Unfiled", custom dividers, and "+").
  - "New note" ghost card and note cards with entrance animation (14 dp rise, 40 ms stagger).
  - Long-press (420 ms) lifts the card (scale 1.03), activates scrim, and opens `ContextMenu` 10 dp below card.
  - `NewDividerSheet` for divider creation with duplicate name rejection.
  - `MoveToDividerSheet` for moving notes between dividers.
  - `TrashScreen` with struck-through titles, days remaining ("N days left"), restore pills, 30-day footer, and `DeleteForeverAlert`.
- **Task 22: Search / Find (`app.enk.search`)**
  - `SearchField` with debounce (150 ms) and clear button.
  - `MatchText` / `matchStyled` applying heavy ink weight (`FontWeight.Bold`, `t1`) to matched substrings and light weight (`FontWeight.Normal`, `t2`) to surrounding text.
  - Full hit navigation into `Route.Note(hit.noteId, hit.pageIndex)`.
  - Empty, no-results, and results states with match counter.
- **Task 23: Setup screen with Appearance & Shell Controls (`app.enk.settings`, `app.enk.design.shell`)**
  - Shell controls: `ShellToggle`, `ShellSegmented` (sliding pill indicator), `ShellFader` (slider with percentage readout), `ThemeTile` (palette swatch with selection indicator), `GroupedSection`, `GroupedRow`, `GroupedDivider`.
  - `AppearanceGroup`: 5 theme tiles (`Graphite`, `Apricot`, `Sky`, `Slate`, `Mono`) + `Dynamic` (on API 31+), and Mode toggle (`Light`, `Dark`).
  - Typing group: correction mode (`Strike-through`, `Mechanical tap`, `Disabled`), autocorrect toggle.
  - Sound & Haptics group: sound set picker (`Typewriter`, etc.), mute switch, bell volume fader, and haptics switch.
  - Motion group: reduce motion toggle.
  - Storage group: SAF export and import of all notes to/from zip archive.
  - About group: version string ("1.0.0") and open-source footer ("Free and open source · Apache-2.0 / No network, no tracking"). Credits and licenses row is reserved for Task 25 as specified in the plan.

---

## 2. Public Signatures (Extracted from Code)

### Task 21: Box & Trash (`app.enk.box`)

| File | Public Declaration |
|---|---|
| `app/.../box/BoxViewModel.kt` | `data class BoxUiState(val dividers: List<DividerEntity> = emptyList(), val selectedDividerId: Long? = null, val notes: List<NoteSummary> = emptyList(), val trashCount: Int = 0, val userMessage: String? = null)` |
| `app/.../box/BoxViewModel.kt` | `sealed interface DividerValidationResult` |
| `app/.../box/BoxViewModel.kt` | `class BoxViewModel(private val repo: NoteRepository, private val dividers: DividerDao, private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) : ViewModel()` |
| `app/.../box/BoxViewModel.kt` | `fun select(dividerId: Long?)` |
| `app/.../box/BoxViewModel.kt` | `fun addDivider(name: String)` |
| `app/.../box/BoxViewModel.kt` | `fun renameDivider(id: Long, name: String)` |
| `app/.../box/BoxViewModel.kt` | `fun deleteDivider(id: Long)` |
| `app/.../box/BoxViewModel.kt` | `fun move(noteId: String, dividerId: Long?)` |
| `app/.../box/BoxViewModel.kt` | `fun trash(noteId: String)` |
| `app/.../box/BoxViewModel.kt` | `fun restore(noteId: String)` |
| `app/.../box/BoxViewModel.kt` | `fun reorderNotes(ids: List<String>)` |
| `app/.../box/BoxViewModel.kt` | `fun reorderDividers(ids: List<Long>)` |
| `app/.../box/BoxViewModel.kt` | `fun deleteAllForever()` |
| `app/.../box/BoxViewModel.kt` | `fun dividerNameValid(name: String, existingNames: List<String>): DividerValidationResult` |
| `app/.../box/BoxViewModel.kt` | `fun validateDividerName(name: String, existingNames: List<String> = emptyList()): DividerValidationResult` |
| `app/.../box/BoxScreen.kt` | `@Composable fun BoxContent(state: BoxUiState, modifier: Modifier = Modifier, onSelectDivider: (Long?) -> Unit = {}, onAddDivider: (String) -> Unit = {}, onAddDividerClick: () -> Unit = {}, onNewNote: () -> Unit = {}, onNoteClick: (String) -> Unit = {}, onNoteHold: (String) -> Unit = {}, onMoveToDivider: (String, Long?) -> Unit = { _, _ -> }, onTrashNote: (String) -> Unit = {}, listState: LazyListState = rememberLazyListState(), contentPadding: PaddingValues = PaddingValues(top = 16.dp, bottom = 120.dp), glassSource: GlassSource = rememberGlassSource())` |
| `app/.../box/BoxScreen.kt` | `@Composable fun BoxScreen(state: BoxUiState, modifier: Modifier = Modifier, onSelectDivider: (Long?) -> Unit = {}, onAddDivider: (String) -> Unit = {}, onAddDividerClick: () -> Unit = {}, onNewNote: () -> Unit = {}, onNoteClick: (String) -> Unit = {}, onTrashNote: (String) -> Unit = {}, onMoveToDivider: (String, Long?) -> Unit = { _, _ -> }, onNavigateBack: () -> Unit = {}, onNavigate: (Dest) -> Unit = {})` |
| `app/.../box/BoxScreen.kt` | `@Composable fun BoxScreen(viewModel: BoxViewModel, onNavigate: (Dest) -> Unit, onNavigateToNote: (String) -> Unit, onNewNote: () -> Unit, onNavigateBack: () -> Unit, modifier: Modifier = Modifier)` |
| `app/.../box/NoteCard.kt` | `@Composable fun NoteCard(summary: NoteSummary, onClick: () -> Unit, onHold: () -> Unit, lifted: Boolean, modifier: Modifier = Modifier, onMoveToDivider: () -> Unit = {}, onMoveToTrash: () -> Unit = {})` |
| `app/.../box/DividerStrip.kt` | `@Composable fun DividerStrip(dividers: List<DividerEntity>, selectedId: Long?, onSelect: (Long?) -> Unit, onAdd: () -> Unit, modifier: Modifier = Modifier, onLongPressDivider: (DividerEntity) -> Unit = {})` |
| `app/.../box/ContextMenu.kt` | `@Composable fun ContextMenu(cardBounds: Rect?, onMoveToDivider: () -> Unit, onMoveToTrash: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier, glassSource: GlassSource = rememberGlassSource())` |
| `app/.../box/NewDividerSheet.kt` | `@Composable fun NewDividerSheet(onDismiss: () -> Unit, onAdd: (String) -> Unit, modifier: Modifier = Modifier, existingNames: List<String> = emptyList(), glassSource: GlassSource = rememberGlassSource())` |
| `app/.../box/MoveToDividerSheet.kt` | `@Composable fun MoveToDividerSheet(dividers: List<DividerEntity>, onSelectDivider: (Long?) -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier, glassSource: GlassSource = rememberGlassSource())` |
| `app/.../box/TrashScreen.kt` | `@Composable fun TrashContent(trashedNotes: List<NoteSummary>, onRestore: (String) -> Unit, onDeleteForever: () -> Unit, modifier: Modifier = Modifier, onNavigateBack: () -> Unit = {}, listState: LazyListState = rememberLazyListState(), contentPadding: PaddingValues = PaddingValues(top = 16.dp, bottom = 120.dp))` |
| `app/.../box/TrashScreen.kt` | `@Composable fun TrashScreen(viewModel: BoxViewModel, onNavigate: (Dest) -> Unit, onNavigateBack: () -> Unit, modifier: Modifier = Modifier)` |
| `app/.../box/DeleteForeverAlert.kt` | `@Composable fun DeleteForeverAlert(onConfirm: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier, glassSource: GlassSource = rememberGlassSource())` |

### Task 22: Search (`app.enk.search`)

| File | Public Declaration |
|---|---|
| `app/.../search/SearchViewModel.kt` | `class SearchViewModel(private val repo: SearchRepository, private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) : ViewModel()` |
| `app/.../search/SearchField.kt` | `@Composable fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier)` |
| `app/.../search/MatchText.kt` | `fun matchStyled(text: String, query: String): AnnotatedString` |
| `app/.../search/MatchText.kt` | `@Composable fun MatchText(text: String, query: String, modifier: Modifier = Modifier, fontSize: TextUnit = 13.sp, maxLines: Int = 3)` |
| `app/.../search/SearchScreen.kt` | `@Composable fun SearchContent(query: String, results: List<SearchResult>, onQueryChange: (String) -> Unit, onHitClick: (SearchResult) -> Unit, modifier: Modifier = Modifier, listState: LazyListState = rememberLazyListState(), showNav: Boolean = true, onNavigate: (Dest) -> Unit = {})` |
| `app/.../search/SearchScreen.kt` | `@Composable fun SearchScreen(viewModel: SearchViewModel, onNavigateToHit: (SearchResult) -> Unit, onNavigate: (Dest) -> Unit, modifier: Modifier = Modifier)` |

### Task 23: Setup & Appearance (`app.enk.settings`, `app.enk.design.shell`)

| File | Public Declaration |
|---|---|
| `app/.../settings/SettingsViewModel.kt` | `class SettingsViewModel(private val store: SettingsStore, private val exporter: Exporter, private val importer: Importer, private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) : ViewModel()` |
| `app/.../settings/SettingsViewModel.kt` | `fun setTheme(t: ThemeId)` |
| `app/.../settings/SettingsViewModel.kt` | `fun setThemeMode(m: ThemeMode)` |
| `app/.../settings/SettingsViewModel.kt` | `fun setCorrection(m: CorrectionMode)` |
| `app/.../settings/SettingsViewModel.kt` | `fun setAutocorrect(b: Boolean)` |
| `app/.../settings/SettingsViewModel.kt` | `fun setSoundSet(id: SoundSetId)` |
| `app/.../settings/SettingsViewModel.kt` | `fun setMuted(b: Boolean)` |
| `app/.../settings/SettingsViewModel.kt` | `fun setBellVolume(v: Float)` |
| `app/.../settings/SettingsViewModel.kt` | `fun setHaptics(b: Boolean)` |
| `app/.../settings/SettingsViewModel.kt` | `fun setReduceEffects(b: Boolean)` |
| `app/.../settings/AppearanceGroup.kt` | `@Composable fun AppearanceGroup(settings: Settings, onThemeSelect: (ThemeId) -> Unit, onModeSelect: (ThemeMode) -> Unit, modifier: Modifier = Modifier, supportsDynamic: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)` |
| `app/.../settings/SettingsScreen.kt` | `@Composable fun SettingsContent(settings: Settings, soundSets: List<SoundSet>, exportResult: ExportResult?, importResult: ImportResult?, onThemeSelect: (ThemeId) -> Unit, onModeSelect: (ThemeMode) -> Unit, onCorrectionSelect: (CorrectionMode) -> Unit, onAutocorrectToggle: (Boolean) -> Unit, onSoundSetSelect: (SoundSetId) -> Unit, onMutedToggle: (Boolean) -> Unit, onBellVolumeChange: (Float) -> Unit, onHapticsToggle: (Boolean) -> Unit, onReduceEffectsToggle: (Boolean) -> Unit, onExportNotes: () -> Unit, onImportNotes: () -> Unit, modifier: Modifier = Modifier, listState: LazyListState = rememberLazyListState(), showNav: Boolean = true, onNavigate: (Dest) -> Unit = {})` |
| `app/.../settings/SettingsScreen.kt` | `@Composable fun SettingsScreen(viewModel: SettingsViewModel, soundSets: List<SoundSet>, onNavigate: (Dest) -> Unit, modifier: Modifier = Modifier, exportLauncher: (() -> Unit)? = null, importLauncher: (() -> Unit)? = null)` |
| `design/.../shell/ShellToggle.kt` | `@Composable fun ShellToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true)` |
| `design/.../shell/ShellSegmented.kt` | `@Composable fun <T> ShellSegmented(options: List<T>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier, label: (T) -> String = { it.toString() })` |
| `design/.../shell/ShellFader.kt` | `@Composable fun ShellFader(value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier, onDetent: ((Float) -> Unit)? = null, steps: Int = 0)` |
| `design/.../shell/ThemeTile.kt` | `@Composable fun ThemeTile(themeId: ThemeId, selected: Boolean, onSelect: () -> Unit, modifier: Modifier = Modifier)` |
| `design/.../shell/GroupedList.kt` | `@Composable fun GroupedSection(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit)` |
| `design/.../shell/GroupedList.kt` | `@Composable fun GroupedRow(title: String, modifier: Modifier = Modifier, subtitle: String? = null, trailing: @Composable (() -> Unit)? = null, onClick: (() -> Unit)? = null)` |
| `design/.../shell/GroupedList.kt` | `@Composable fun GroupedDivider(modifier: Modifier = Modifier)` |

---

## 3. Test Results (Direct from Gradle Execution)

**Total unique unit tests: 727 (0 failures, 0 skipped)**
Across release + debug test suites: 1319 test executions, 0 failures, 0 skipped.

### Module Breakdown
- `:app`: 374 tests, 0 failures, 0 skipped
- `:audio`: 42 tests, 0 failures, 0 skipped
- `:data`: 85 tests, 0 failures, 0 skipped
- `:design`: 174 tests, 0 failures, 0 skipped
- `:text`: 52 tests, 0 failures, 0 skipped

### Per-Class Test Breakdown (Module `:app`)
| Test Class | Tests | Failures | Skipped |
|---|---|---|---|
| `app.enk.AppSmokeTest` | 1 | 0 | 0 |
| `app.enk.MainViewModelTest` | 4 | 0 | 0 |
| `app.enk.ManifestTest` | 5 | 0 | 0 |
| `app.enk.ProvenanceTest` | 6 | 0 | 0 |
| `app.enk.box.BoxInteractionTest` | 11 | 0 | 0 |
| `app.enk.box.BoxThemeGoldenTest` | 14 | 0 | 0 |
| `app.enk.box.BoxViewModelTest` | 12 | 0 | 0 |
| `app.enk.logo.LauncherIconTest` | 1 | 0 | 0 |
| `app.enk.search.MatchTextTest` | 3 | 0 | 0 |
| `app.enk.search.SearchGoldenTest` | 4 | 0 | 0 |
| `app.enk.search.SearchScreenTest` | 3 | 0 | 0 |
| `app.enk.search.SearchViewModelTest` | 2 | 0 | 0 |
| `app.enk.settings.AppearanceGroupTest` | 5 | 0 | 0 |
| `app.enk.settings.CorruptionHandlerTest` | 1 | 0 | 0 |
| `app.enk.settings.SettingsStoreTest` | 16 | 0 | 0 |
| `app.enk.settings.SettingsViewModelTest` | 5 | 0 | 0 |
| `app.enk.settings.SetupGoldenTest` | 4 | 0 | 0 |
| `app.enk.shell.GlassNavBarTest` | 10 | 0 | 0 |
| `app.enk.shell.RouteTest` | 14 | 0 | 0 |
| `app.enk.shell.ShellGoldenTest` | 5 | 0 | 0 |
| `app.enk.shell.ShellScaffoldTest` | 8 | 0 | 0 |
| `app.enk.shell.SystemBarIconsTest` | 3 | 0 | 0 |
| `app.enk.start.SplashHoldTest` | 5 | 0 | 0 |
| `app.enk.start.StartGateTest` | 3 | 0 | 0 |
| `app.enk.start.StartGoldenTest` | 12 | 0 | 0 |
| `app.enk.start.StartHandoffTest` | 10 | 0 | 0 |
| `app.enk.start.StartLayoutTest` | 10 | 0 | 0 |
| `app.enk.start.StartTimelineTest` | 21 | 0 | 0 |
| `app.enk.writer.AssembledScreenGoldenTest` | 2 | 0 | 0 |
| `app.enk.writer.AutosaverTest` | 8 | 0 | 0 |
| `app.enk.writer.CarriageGoldenTest` | 7 | 0 | 0 |
| `app.enk.writer.HintPolicyTest` | 6 | 0 | 0 |
| `app.enk.writer.ImeBridgeTest` | 20 | 0 | 0 |
| `app.enk.writer.MarkerGestureTest` | 18 | 0 | 0 |
| `app.enk.writer.MotionPolicyTest` | 3 | 0 | 0 |
| `app.enk.writer.NightThemeTest` | 6 | 0 | 0 |
| `app.enk.writer.PaperGoldenTest` | 9 | 0 | 0 |
| `app.enk.writer.PaperTextTest` | 12 | 0 | 0 |
| `app.enk.writer.TapCaretPolicyTest` | 7 | 0 | 0 |
| `app.enk.writer.TextSyncTest` | 5 | 0 | 0 |
| `app.enk.writer.TopBarFormatterTest` | 3 | 0 | 0 |
| `app.enk.writer.TypeSlugTest` | 9 | 0 | 0 |
| `app.enk.writer.TypingPolicyTest` | 4 | 0 | 0 |
| `app.enk.writer.WriterGeometryTest` | 11 | 0 | 0 |
| `app.enk.writer.WriterReducerTest` | 26 | 0 | 0 |
| `app.enk.writer.WriterScreenTest` | 9 | 0 | 0 |
| `app.enk.writer.WriterViewModelTest` | 11 | 0 | 0 |

---

## 4. Paparazzi Golden Snapshots Recorded in Batch 8 (22 total)

### Box & Trash (14 snapshots)
1. `app/src/test/snapshots/images/app.enk.box_BoxThemeGoldenTest_boxApricotDark.png`
2. `app/src/test/snapshots/images/app.enk.box_BoxThemeGoldenTest_boxApricotLight.png`
3. `app/src/test/snapshots/images/app.enk.box_BoxThemeGoldenTest_boxDynamicDarkSeed2.png`
4. `app/src/test/snapshots/images/app.enk.box_BoxThemeGoldenTest_boxDynamicLightSeed1.png`
5. `app/src/test/snapshots/images/app.enk.box_BoxThemeGoldenTest_boxGraphiteDark.png`
6. `app/src/test/snapshots/images/app.enk.box_BoxThemeGoldenTest_boxGraphiteLight.png`
7. `app/src/test/snapshots/images/app.enk.box_BoxThemeGoldenTest_boxMonoDark.png`
8. `app/src/test/snapshots/images/app.enk.box_BoxThemeGoldenTest_boxMonoLight.png`
9. `app/src/test/snapshots/images/app.enk.box_BoxThemeGoldenTest_boxSkyDark.png`
10. `app/src/test/snapshots/images/app.enk.box_BoxThemeGoldenTest_boxSkyLight.png`
11. `app/src/test/snapshots/images/app.enk.box_BoxThemeGoldenTest_boxSlateDark.png`
12. `app/src/test/snapshots/images/app.enk.box_BoxThemeGoldenTest_boxSlateLight.png`
13. `app/src/test/snapshots/images/app.enk.box_BoxThemeGoldenTest_trashApricotLight.png`
14. `app/src/test/snapshots/images/app.enk.box_BoxThemeGoldenTest_trashGraphiteDark.png`

### Find / Search (4 snapshots)
15. `app/src/test/snapshots/images/app.enk.search_SearchGoldenTest_searchApricotLightResults.png`
16. `app/src/test/snapshots/images/app.enk.search_SearchGoldenTest_searchGraphiteDarkEmpty.png`
17. `app/src/test/snapshots/images/app.enk.search_SearchGoldenTest_searchGraphiteDarkNoResults.png`
18. `app/src/test/snapshots/images/app.enk.search_SearchGoldenTest_searchGraphiteDarkResults.png`

### Setup / Appearance (4 snapshots)
19. `app/src/test/snapshots/images/app.enk.settings_SetupGoldenTest_setupApricotDark.png`
20. `app/src/test/snapshots/images/app.enk.settings_SetupGoldenTest_setupDynamicDarkSeed.png`
21. `app/src/test/snapshots/images/app.enk.settings_SetupGoldenTest_setupGraphiteDark.png`
22. `app/src/test/snapshots/images/app.enk.settings_SetupGoldenTest_setupSlateLight.png`

All 22 snapshots verified via `:app:verifyPaparazziDebug`.

---

## 5. Scope Boundaries, Disclosures & Small Deviations

1. **About Group "Credits and licenses" Row**: In accordance with the plan instructions, the "Credits and licenses" row belongs to Task 25 (About and Legal Screen). It is left out of Batch 8 to avoid dead UI. The About group cleanly presents the app version ("1.0.0") and footer ("Free and open source · Apache-2.0 / No network, no tracking").
2. **SearchRepository Testability**: `SearchRepository` and its `search` method in `:data` were given `open` visibility to allow clean subclassing in unit tests (`FakeSearchRepository`) without requiring heavy or deprecated mocking frameworks.
3. **No Network**: Verified zero network permissions declared across merged manifest (`scripts/check-no-network.sh` passed).
4. **Device QA Checklist**: Completed and committed in `docs/qa/box-find-setup-device-checklist.md`.
5. **Full Clean Gate Output**:
   Command:
   ```powershell
   $env:JAVA_HOME = "D:\jdks\eclipse_adoptium-21-amd64-windows.2"; .\gradlew.bat clean build test :design:verifyPaparazziDebug :app:verifyPaparazziDebug :app:processDebugManifest :app:checkDebugAarMetadata :app:checkReleaseAarMetadata
   ```
   Result: `BUILD SUCCESSFUL in 7m 35s. 414 actionable tasks: 390 executed, 24 up-to-date.`
