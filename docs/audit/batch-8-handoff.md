# Batch 8 handoff: The Box, Find, Trash and Setup

Branch `batch-8/box-find-setup`, base `main` (`21f2d68`). Tasks 21, 22, 23. The implementer's first handoff was wrong in several places (it described groups and options that do not exist, a debounce the code does not have and signatures that differ from the code). The external audit found real defects on a device, and the auditor then fixed them on this branch. **This version replaces it and is generated from the code and the gate output.**

## 1. What the audit found, and what was done

| # | Finding | Fix | Proof |
|---|---|---|---|
| 1 | Trash was unreachable: nothing navigated to `Route.Trash` | A "Trash" row ends the Box list ("Empty" or "N notes"); it opens the Trash; back returns to the Box | `ShellScreensLayoutTest.theTrashRowOpensTheTrashAndShowsTheCount`; device |
| 2 | The New divider sheet was unusable: the nav bar drew over Cancel and Add; with the keyboard up the sheet was hidden; the field was not focused | Menu, sheets and alerts are drawn above the scaffold (they cover the nav bar); sheets pad by `safeDrawing` bottom (keyboard or nav bar); the field takes focus on open; the keyboard's Done adds a valid name | `ShellScreensLayoutTest.theNewDividerSheetIsNotUnderTheNavigationBar`, `...RisesAboveTheKeyboard` (real insets); device |
| 3 | Large titles overlapped the status bar (Box), the search field (Find) and the first group (Setup) | Root cause in the Batch 7 scaffold: the title was measured inside the status bar padding, so content started that much too high. Measured outside it now. The Box was also not built on the scaffold at all; it is now (title, compact bar, nav bar, insets shared) | `ShellScreensLayoutTest` (4 tests with a 49 dp status bar); broken on purpose: the old modifier order made 3 of them fail |
| 4 | Export and import results were discarded; they ran on the composition scope (leaving the screen cancelled an import half way); a failed export threw an uncaught exception; fixed file name | `SettingsViewModel.exportNotes` and `importNotes` run on the view model, report `TransferState` (Running, Done, Failed) shown as a message pill, ignore a second request while one runs, and never throw; the picker name is `enk-notes-YYYY-MM-DD.zip` | `SettingsViewModelTest` (round trip, corrupt zip keeps data, unreadable and unwritable URIs, double request, summary and file name) |
| 5 | Wrong footer on Appearance (it described a behaviour that is not built); Writing and Display footers missing | Design system 12.10 text for all three | `AppearanceGroupTest`; goldens viewed |
| 6 | Tests that could not fail (`ShellControlsTest` asserted constants against themselves); search and match tests thin | Controls measured as composed nodes (`ShellControlsCompositionTest`, 12 tests); search view model tests for out-of-order results, failure and blank input; match tests for regex characters, surrogate pairs, length-changing lower case, overlap and a 10,000 character text; "delete all forever" proven to erase pages and search rows | the test classes named; broken on purpose: nav bar callback replaced mid-gesture (earlier batch) and the scaffold order above |
| 7 | Box goldens showed no note cards (entrance frozen at zero) | `BoxScreen(entranceProgress)`: an explicit parameter, null in the app, 1 in goldens; all 12 re-recorded and viewed | `BoxThemeGoldenTest` |
| 8 | Sound set choice prepared on the calling thread and had no preview | Saved, prepared and one key played on the IO scope, silent when muted | `SettingsViewModelTest.choosingASoundSet...`, `aMutedApp...` |
| 9 | Handoff from memory | this document | section 2 |
| 10 | Minor: match ink was a literal; `catch (Exception)` swallowed cancellation; raw exception text shown to the user; `deleteAllForever` re-entrant | `MatchInk` constant; `guarded` helper rethrows cancellation and shows fixed sentences; an atomic flag; failed new note leaves the Box as it is; the message pill clears itself | `BoxViewModelDeleteAndCreateTest`, `BoxViewModelTest` |

Also added: accessibility semantics on the shell controls (switch role and toggle state, fader range and set-progress action, segmented selected state), a `Dest.toRoute()` mapping used by every screen (it was copied five times in `EnkNavHost`), and screen view models keyed in the activity store (`box-vm`, `search-vm`, `settings-vm`), so a rotation keeps the divider, the query and the settings state.

## 2. Declarations (extracted from the code by script)

| File | Declaration (copied from the code) |
|---|---|
| `box/BoxScreen.kt` | `internal fun noteCountText(count: Int): String = when (count)` |
| `box/BoxScreen.kt` | `@Composable fun BoxScreen( state: BoxUiState, modifier: Modifier = Modifier, onSelectDivider: (Long?) -> Unit = {}, onAddDivider: (String) -> Unit = {}, onNewNote: () -> Unit = {}, onNoteClick: (String) -> Unit = {}, onTrashNote: (String) -> Unit = {}, onMoveToDivider: (String, Long?) -> Unit = { _, _ -> }, onOpenTrash: () -> Unit = {}, onMessageShown: () -> Unit = {}, onNavigateBack: () -> Unit = {}, onNavigate: (Dest) -> Unit = {}, entranceProgress: Float? = null )` |
| `box/BoxScreen.kt` | `@Composable internal fun TrashRow(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier)` |
| `box/BoxScreen.kt` | `@Composable fun BoxScreen( viewModel: BoxViewModel, onNavigate: (Dest) -> Unit, onNavigateToNote: (String) -> Unit, onNewNote: () -> Unit, onOpenTrash: () -> Unit, onNavigateBack: () -> Unit, modifier: Modifier = Modifier )` |
| `box/BoxViewModel.kt` | `data class BoxUiState( val dividers: List<DividerEntity> = emptyList(), val selectedDividerId: Long? = null, val notes: List<NoteSummary> = emptyList(), val trashCount: Int = 0, val trashed: List<TrashedSummary> = emptyList(), val userMessage: String? = null )` |
| `box/BoxViewModel.kt` | `sealed interface DividerValidationResult` |
| `box/BoxViewModel.kt` | `data class Valid(val name: String) : DividerValidationResult` |
| `box/BoxViewModel.kt` | `class BoxViewModel( val repo: NoteRepository, val dividers: DividerDao, private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO, externalScope: kotlinx.coroutines.CoroutineScope? = null ) : ViewModel()` |
| `box/BoxViewModel.kt` | `fun select(dividerId: Long?)` |
| `box/BoxViewModel.kt` | `fun dividerNameValid(name: String, existingNames: List<String>): DividerValidationResult` |
| `box/BoxViewModel.kt` | `fun dividerNameValid(name: String): Boolean` |
| `box/BoxViewModel.kt` | `fun validateDividerName(name: String, existingNames: List<String> = emptyList()): DividerValidationResult` |
| `box/BoxViewModel.kt` | `fun addDivider(name: String)` |
| `box/BoxViewModel.kt` | `fun renameDivider(id: Long, name: String)` |
| `box/BoxViewModel.kt` | `fun deleteDivider(id: Long)` |
| `box/BoxViewModel.kt` | `fun move(noteId: String, dividerId: Long?) = guarded("Couldn't move that note.") { repo.moveToDivider(noteId, dividerId) }` |
| `box/BoxViewModel.kt` | `fun trash(noteId: String) = guarded("Couldn't move that note to the trash.") { repo.trash(noteId) }` |
| `box/BoxViewModel.kt` | `fun restore(noteId: String) = guarded("Couldn't restore that note.") { repo.restore(noteId) }` |
| `box/BoxViewModel.kt` | `fun reorderNotes(ids: List<String>)` |
| `box/BoxViewModel.kt` | `fun reorderDividers(ids: List<Long>) = guarded("Couldn't reorder the dividers.") { repo.reorderDividers(ids) }` |
| `box/BoxViewModel.kt` | `fun deleteAllForever()` |
| `box/BoxViewModel.kt` | `fun newNote(onCreated: (String) -> Unit)` |
| `box/BoxViewModel.kt` | `fun clearUserMessage()` |
| `box/ContextMenu.kt` | `@Composable fun ContextMenu( cardBounds: Rect?, onMoveToDivider: () -> Unit, onMoveToTrash: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier, glassSource: GlassSource = rememberGlassSource() )` |
| `box/DeleteForeverAlert.kt` | `@Composable fun DeleteForeverAlert( count: Int, onConfirm: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier, isDeleting: Boolean = false, glassSource: GlassSource = rememberGlassSource() )` |
| `box/DividerStrip.kt` | `data class ItemBounds(val x: Float, val width: Float)` |
| `box/DividerStrip.kt` | `@Composable fun DividerStrip( dividers: List<DividerEntity>, selectedId: Long?, onSelect: (Long?) -> Unit, onAdd: () -> Unit, modifier: Modifier = Modifier, glassSource: GlassSource = rememberGlassSource() )` |
| `box/MoveToDividerSheet.kt` | `@Composable fun MoveToDividerSheet( dividers: List<DividerEntity>, onSelectDivider: (Long?) -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier, glassSource: GlassSource = rememberGlassSource() )` |
| `box/NewDividerSheet.kt` | `@Composable fun NewDividerSheet( onDismiss: () -> Unit, onAdd: (String) -> Unit, modifier: Modifier = Modifier, existingNames: List<String> = emptyList(), glassSource: GlassSource = rememberGlassSource() )` |
| `box/NoteCard.kt` | `val NoteCardLiftedKey = SemanticsPropertyKey<Boolean>("NoteCardLifted")` |
| `box/NoteCard.kt` | `val PaperColor = Color(0xFFF2ECDF)` |
| `box/NoteCard.kt` | `val CardTextColor = Color(0xFF14110D)` |
| `box/NoteCard.kt` | `val CardSubtextColor = Color(0xFF5C574F)` |
| `box/NoteCard.kt` | `@Composable fun NoteCard( summary: NoteSummary, onClick: () -> Unit, onHold: () -> Unit, lifted: Boolean, modifier: Modifier = Modifier, onMoveToDivider: (() -> Unit)? = null, onMoveToTrash: (() -> Unit)? = null )` |
| `box/NoteCard.kt` | `@Composable fun NoteCard( hit: SearchHit, query: String, onClick: () -> Unit, modifier: Modifier = Modifier )` |
| `box/TrashScreen.kt` | `val TrashPillFill = Color(0xEB201E1C)` |
| `box/TrashScreen.kt` | `val TrashPillText = Color(0xFFF3EEE3)` |
| `box/TrashScreen.kt` | `@Composable fun TrashContent( trashedNotes: List<TrashedSummary>, modifier: Modifier = Modifier, onRestore: (String) -> Unit = {}, onDeleteAllForever: () -> Unit = {}, listState: LazyListState = rememberLazyListState(), contentPadding: PaddingValues = PaddingValues(top = 124.dp, bottom = 120.dp), glassSource: GlassSource = rememberGlassSource() )` |
| `box/TrashScreen.kt` | `@Composable fun TrashScreen( trashed: List<TrashedSummary>, onRestore: (String) -> Unit, onDeleteAllForever: () -> Unit, onNavigate: (Dest) -> Unit, onNavigateBack: () -> Unit, modifier: Modifier = Modifier )` |
| `box/TrashScreen.kt` | `@Composable fun TrashScreen( viewModel: BoxViewModel, onNavigate: (Dest) -> Unit, onNavigateBack: () -> Unit, modifier: Modifier = Modifier )` |
| `search/MatchText.kt` | `internal val MatchInk = Color(0xFF14110D)` |
| `search/MatchText.kt` | `fun matchStyled(text: String, query: String): AnnotatedString` |
| `search/SearchField.kt` | `@Composable fun SearchField( query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String = "Search your notes", onSearch: () -> Unit = {} )` |
| `search/SearchScreen.kt` | `@Composable fun SearchContent( query: String, onQueryChange: (String) -> Unit, results: List<SearchHit>, onHitClick: (noteId: String, pageIndex: Int) -> Unit, modifier: Modifier = Modifier, listState: LazyListState = rememberLazyListState(), showNav: Boolean = true, onNavigate: (Dest) -> Unit = {} )` |
| `search/SearchScreen.kt` | `@Composable fun SearchScreen( viewModel: SearchViewModel, onNavigate: (Dest) -> Unit, onNavigateToNote: (noteId: String, pageIndex: Int) -> Unit, onNavigateBack: () -> Unit, modifier: Modifier = Modifier )` |
| `search/SearchViewModel.kt` | `class SearchViewModel( private val repo: SearchRepository, private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO, externalScope: CoroutineScope? = null ) : ViewModel()` |
| `settings/AppearanceGroup.kt` | `@Composable fun AppearanceGroup( selectedTheme: ThemeId, onThemeSelect: (ThemeId) -> Unit, selectedMode: ThemeMode, onModeSelect: (ThemeMode) -> Unit, modifier: Modifier = Modifier )` |
| `settings/Settings.kt` | `enum class CorrectionMode` |
| `settings/Settings.kt` | `enum class SoundSetId` |
| `settings/Settings.kt` | `data class Settings( val correction: CorrectionMode = CorrectionMode.CleanErase, val autocorrect: Boolean = false, val soundSet: SoundSetId = SoundSetId.Classic, val muted: Boolean = false, val bellVolume: Float = 0.6f, val haptics: Boolean = true, val reduceEffects: Boolean = false, val theme: ThemeId = ThemeId.Graphite, val themeMode: ThemeMode = ThemeMode.System, val warmth: Int = 25, val intensity: Int = 80, val beam: Int = 55, val hintDismissed: Boolean = false )` |
| `settings/SettingsScreen.kt` | `@Composable fun SettingsContent( settings: Settings, onSettingsChange: ((Settings) -> Settings) -> Unit, modifier: Modifier = Modifier, onExport: () -> Unit = {}, onImport: () -> Unit = {}, listState: LazyListState = rememberLazyListState(), showNav: Boolean = true, onNavigate: (Dest) -> Unit = {} )` |
| `settings/SettingsScreen.kt` | `@Composable fun SettingsScreen( viewModel: SettingsViewModel, onNavigate: (Dest) -> Unit, onNavigateBack: () -> Unit, modifier: Modifier = Modifier )` |
| `settings/SettingsStore.kt` | `class SettingsStore(private val dataStore: DataStore<Preferences>)` |
| `settings/SettingsViewModel.kt` | `val THEME_TILE_ORDER: List<ThemeId> = listOf( ThemeId.Graphite, ThemeId.Apricot, ThemeId.Sky, ThemeId.Slate, ThemeId.Mono, ThemeId.Dynamic )` |
| `settings/SettingsViewModel.kt` | `val MODE_ORDER: List<ThemeMode> = listOf( ThemeMode.Light, ThemeMode.Dark, ThemeMode.System )` |
| `settings/SettingsViewModel.kt` | `val SETUP_GROUP_ORDER: List<String> = listOf( "Appearance", "Writing", "Sound", "Display", "Data", "About" )` |
| `settings/SettingsViewModel.kt` | `sealed interface TransferState` |
| `settings/SettingsViewModel.kt` | `data class Running(val message: String) : TransferState` |
| `settings/SettingsViewModel.kt` | `data class Done(val message: String) : TransferState` |
| `settings/SettingsViewModel.kt` | `data class Failed(val message: String) : TransferState` |
| `settings/SettingsViewModel.kt` | `internal fun importSummary(result: ImportResult.Done): String` |
| `settings/SettingsViewModel.kt` | `internal fun exportFileName(date: java.time.LocalDate): String = "enk-notes-$date.zip"` |
| `settings/SettingsViewModel.kt` | `class SettingsViewModel( private val store: SettingsStore, private val exporter: Exporter, private val importer: Importer, private val soundPlayer: SoundPlayer? = null, private val soundManifest: List<SoundSet> = emptyList(), private val context: Context? = null, private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO, externalScope: CoroutineScope? = null ) : ViewModel()` |
| `settings/SettingsViewModel.kt` | `fun setTheme(t: ThemeId)` |
| `settings/SettingsViewModel.kt` | `fun setThemeMode(m: ThemeMode)` |
| `settings/SettingsViewModel.kt` | `fun setCorrection(m: CorrectionMode)` |
| `settings/SettingsViewModel.kt` | `fun setAutocorrect(b: Boolean)` |
| `settings/SettingsViewModel.kt` | `fun setSoundSet(id: SoundSetId)` |
| `settings/SettingsViewModel.kt` | `fun setMuted(b: Boolean)` |
| `settings/SettingsViewModel.kt` | `fun setBellVolume(v: Float)` |
| `settings/SettingsViewModel.kt` | `fun setHaptics(b: Boolean)` |
| `settings/SettingsViewModel.kt` | `fun setReduceEffects(b: Boolean)` |
| `settings/SettingsViewModel.kt` | `fun clearTransfer()` |
| `settings/SettingsViewModel.kt` | `fun exportNotes(uri: Uri)` |
| `settings/SettingsViewModel.kt` | `fun importNotes(uri: Uri)` |
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
| `shell/Route.kt` | `fun Dest.toRoute(): Route = when (this)` |
| `shell/MessagePill.kt` | `@Composable fun MessagePill(text: String, modifier: Modifier = Modifier, tag: String = "message_pill")` |
| `shell/ShellScaffold.kt` | `@Composable fun ShellScaffold( title: String, meta: String?, showNav: Boolean, selected: Dest?, onNavigate: (Dest) -> Unit, modifier: Modifier = Modifier, listState: LazyListState = rememberLazyListState(), content: @Composable (listState: LazyListState, contentPadding: PaddingValues) -> Unit )` |
| `shell/EnkNavHost.kt` | `@Composable fun EnkNavHost( container: AppContainer, modifier: Modifier = Modifier, mainViewModel: MainViewModel = viewModel( factory = MainViewModel.factory(LocalContext.current) ), intro: WriterIntro? = null )` |
| `design/shell/Glass.kt` | `class GlassSource(backdrop: LayerBackdrop? = null)` |
| `design/shell/Glass.kt` | `@Composable fun rememberGlassSource(): GlassSource` |
| `design/shell/Glass.kt` | `fun Modifier.glassSource(source: GlassSource): Modifier = composed` |
| `design/shell/Glass.kt` | `@Composable fun Modifier.glass( level: GlassLevel, shape: Shape, source: GlassSource ): Modifier` |
| `design/shell/GlassBackend.kt` | `enum class GlassLevel { Regular, Thick }` |
| `design/shell/GlassBackend.kt` | `enum class GlassBackendKind { Refraction, Blur, Solid }` |
| `design/shell/GlassBackend.kt` | `val LocalReduceEffects = compositionLocalOf { false }` |
| `design/shell/GlassBackend.kt` | `fun chooseGlassBackend( sdkInt: Int, reduceEffects: Boolean, highContrastText: Boolean ): GlassBackendKind` |
| `design/shell/GlassBackend.kt` | `@Composable fun rememberGlassBackend(): GlassBackendKind` |
| `design/shell/GroupedList.kt` | `@Composable fun GroupedSection( title: String, modifier: Modifier = Modifier, footer: String? = null, content: @Composable ColumnScope.() -> Unit )` |
| `design/shell/GroupedList.kt` | `@Composable fun GroupedRow( title: String, modifier: Modifier = Modifier, subtitle: String? = null, onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit = {} )` |
| `design/shell/GroupedList.kt` | `@Composable fun GroupedDivider( modifier: Modifier = Modifier )` |
| `design/shell/ScrollEdge.kt` | `fun collapseProgress(scrollY: Float): Float = ((scrollY - 30f) / 36f).coerceIn(0f, 1f)` |
| `design/shell/ScrollEdge.kt` | `enum class EdgeKind { CompactBar, StripVeil }` |
| `design/shell/ScrollEdge.kt` | `fun Modifier.scrollEdge(kind: EdgeKind, progress: Float, source: GlassSource): Modifier = composed` |
| `design/shell/ShellBackground.kt` | `@Composable fun ShellBackground( modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit )` |
| `design/shell/ShellFader.kt` | `object ShellFaderMath` |
| `design/shell/ShellFader.kt` | `fun fraction(value: Float): Float = if (value.isNaN()) 0f else value.coerceIn(0f, 1f)` |
| `design/shell/ShellFader.kt` | `@Composable fun ShellFader( value: Float, onChange: (Float) -> Unit, modifier: Modifier = Modifier )` |
| `design/shell/ShellSegmented.kt` | `object ShellSegmentedMath` |
| `design/shell/ShellSegmented.kt` | `fun nearestItem(x: Float, itemCenters: List<Float>): Int` |
| `design/shell/ShellSegmented.kt` | `@Composable fun ShellSegmented( items: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier )` |
| `design/shell/ShellToggle.kt` | `object ShellToggleMath` |
| `design/shell/ShellToggle.kt` | `@Composable fun ShellToggle( checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier )` |
| `design/shell/ThemeTile.kt` | `@Composable fun ThemeTile( theme: ThemeId, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier )` |

## 3. Tests

**Total: 766 tests, 0 failures, 0 skipped** (:app 416, :audio 42, :data 85, :design 171, :text 52)

Per class (from the Gradle XML reports, summed by `counts.py`):

| Module | Test class | Tests | Failures | Skipped |
|---|---|---|---|---|
| `:app` | `AppSmokeTest` | 1 | 0 | 0 |
| `:app` | `MainViewModelTest` | 4 | 0 | 0 |
| `:app` | `ManifestTest` | 5 | 0 | 0 |
| `:app` | `ProvenanceTest` | 6 | 0 | 0 |
| `:app` | `BoxInteractionTest` | 11 | 0 | 0 |
| `:app` | `BoxThemeGoldenTest` | 14 | 0 | 0 |
| `:app` | `BoxViewModelDeleteAndCreateTest` | 4 | 0 | 0 |
| `:app` | `BoxViewModelTest` | 12 | 0 | 0 |
| `:app` | `LauncherIconTest` | 1 | 0 | 0 |
| `:app` | `MatchTextEdgeCasesTest` | 8 | 0 | 0 |
| `:app` | `MatchTextTest` | 3 | 0 | 0 |
| `:app` | `SearchGoldenTest` | 4 | 0 | 0 |
| `:app` | `SearchScreenTest` | 3 | 0 | 0 |
| `:app` | `SearchViewModelExtraTest` | 4 | 0 | 0 |
| `:app` | `SearchViewModelTest` | 2 | 0 | 0 |
| `:app` | `AppearanceGroupTest` | 5 | 0 | 0 |
| `:app` | `CorruptionHandlerTest` | 1 | 0 | 0 |
| `:app` | `SettingsStoreTest` | 16 | 0 | 0 |
| `:app` | `SettingsViewModelTest` | 12 | 0 | 0 |
| `:app` | `SetupGoldenTest` | 4 | 0 | 0 |
| `:app` | `GlassNavBarTest` | 10 | 0 | 0 |
| `:app` | `RouteTest` | 14 | 0 | 0 |
| `:app` | `ShellControlsCompositionTest` | 12 | 0 | 0 |
| `:app` | `ShellGoldenTest` | 5 | 0 | 0 |
| `:app` | `ShellScaffoldTest` | 8 | 0 | 0 |
| `:app` | `ShellScreensLayoutTest` | 7 | 0 | 0 |
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
| `:design` | `ShellControlsTest` | 2 | 0 | 0 |
| `:design` | `ContrastTest` | 10 | 0 | 0 |
| `:design` | `DynamicShellColorsTest` | 8 | 0 | 0 |
| `:design` | `EnkShellThemeTest` | 2 | 0 | 0 |
| `:design` | `ShellPalettesTest` | 16 | 0 | 0 |
| `:text` | `GridLayoutTest` | 19 | 0 | 0 |
| `:text` | `PaginatorTest` | 21 | 0 | 0 |
| `:text` | `StruckPageTest` | 11 | 0 | 0 |
| `:text` | `TextSmokeTest` | 1 | 0 | 0 |

## 4. Goldens

Every PNG was opened. Box (12 theme and mode combinations): large title with the note count, the divider strip, the ghost card, two paper cards with title line, date and page chip, "Hold a note for options", the Trash row and the nav bar. Trash (2): struck titles, days left, Restore pills, the 30-day footer and the destructive button. Setup (4): groups with the design system footers. Find (4): unchanged. What they do not show: the real insets (Paparazzi has none; the layout tests above cover them), the keyboard, any gesture or animation.

## 5. Scopes, jobs, listeners and resources created in this batch

| Created | Released |
|---|---|
| `BoxViewModel`, `SearchViewModel`, `SettingsViewModel` (each with its own `SupervisorJob` scope) | activity `ViewModelStore` (keys `box-vm`, `search-vm`, `settings-vm`); `onCleared` cancels the scope |
| Export and import coroutines | the settings view model's scope, never the composition |
| Message pill timers (`LaunchedEffect` with `delay`) | the composition; they clear the message through the view model |
| Activity result launchers (`CreateDocument`, `OpenDocument`) | the composition |
| `BackHandler`s for sheets, menu and alert | enabled only while something is open |

## 6. Deviations and gaps

- Compose tests are Robolectric tests in `app/src/test`; `ShellScreensLayoutTest` and `ShellControlsCompositionTest` join the release-test exclusion list with the other Compose classes.
- The 1.12 stretch of a pressed toggle bead and fader knob is not unit tested: layout bounds ignore a graphics-layer scale and `captureToImage` does not work in this Robolectric setup. It is on the device checklist.
- The underline of a match is the standard text underline, not 1.5 dp (a `SpanStyle` cannot set a thickness).
- `SearchRepository` is `open` so tests can fake it (kept).
- A failed "New note" is tested at the view model only by the success path and the fixed message path for other mutations; a failing `createNote` cannot be injected without making `NoteRepository` open, and closing the database under live Room flows leaks an exception into other tests, so that test was removed.
- Drag-to-reorder is not built (the design system does not define it); the reorder functions and their tests exist.
- The "Credits and licenses" row is left out until Task 25. The system-dark writer paper (design system 2.4, PROPOSED) is not built.
- **Device pass (Android 17 emulator `Enk_API_37`) after the fixes:** titles clear the status bar and the content on Box, Find, Trash and Setup; the Trash row shows "Empty" then "1 note" and opens the Trash; hold-to-menu, Move to trash, "30 days left", Restore pill, the Delete forever alert over the whole screen with Keep and Delete; after Delete the Trash reads "Nothing in the trash." and Find no longer matches the erased note; the New divider sheet opens with the keyboard up and the field focused, sits above the keyboard with Cancel and Add visible, and Done adds the divider; Setup shows the three footers; the export picker proposes `enk-notes-2026-11-10.zip`. Not exercised on a device: the import and export results themselves (message pills), sound, TalkBack and the pressed stretch.
- Not exercised on a device (continued): see the list above. `docs/qa/box-find-setup-device-checklist.md` lists what to check by hand.

## 7. Gate

```
JAVA_HOME=<JDK 21> ./gradlew.bat clean build test :design:verifyPaparazziDebug :app:verifyPaparazziDebug :app:processDebugManifest :app:checkDebugAarMetadata :app:checkReleaseAarMetadata
BUILD SUCCESSFUL in 5m 29s
414 actionable tasks: 390 executed, 24 up-to-date
```
