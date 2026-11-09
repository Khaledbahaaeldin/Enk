package app.enk.shell

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import app.enk.AppContainer
import app.enk.DefaultAppContainer
import app.enk.MainViewModel
import app.enk.data.EnkDatabase
import app.enk.data.NoteRepository
import app.enk.data.SystemClock
import app.enk.design.theme.EnkShellTheme
import app.enk.design.theme.ThemeId
import app.enk.design.theme.ThemeMode
import app.enk.settings.SettingsStore
import app.enk.text.StruckPage
import app.enk.writer.WriterEvent
import app.enk.writer.WriterViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h820dp")
class ShellScaffoldTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun compactTitleHiddenAtTop() {
        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                ShellScaffold(
                    title = "Box",
                    meta = "12 notes",
                    showNav = true,
                    selected = Dest.Box,
                    onNavigate = {}
                ) { listState: LazyListState, padding: PaddingValues ->
                    LazyColumn(state = listState, contentPadding = padding) {
                        items(50) { index ->
                            Text("Item $index", modifier = Modifier.height(40.dp))
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        // At scrollY = 0, large title is visible
        composeTestRule.onNodeWithTag("large_title").assertIsDisplayed()

        // Compact bar title has alpha 0
        composeTestRule.onNodeWithTag("compact_bar_title")
            .assert(SemanticsMatcher.expectValue(CompactTitleAlpha, 0f))
    }

    @Test
    fun compactTitleShownWhenScrolled() {
        lateinit var state: LazyListState
        lateinit var scope: CoroutineScope
        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                state = rememberLazyListState()
                scope = rememberCoroutineScope()
                ShellScaffold(
                    title = "Box",
                    meta = "12 notes",
                    showNav = true,
                    selected = Dest.Box,
                    onNavigate = {}
                ) { listState: LazyListState, padding: PaddingValues ->
                    state = listState
                    LazyColumn(state = listState, contentPadding = padding) {
                        items(50) { index ->
                            Text("Item $index", modifier = Modifier.height(40.dp))
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        // Scroll content 66 dp (which sets collapseProgress = (66-30)/36 = 1.0)
        val density = composeTestRule.density
        val scrollPx = with(density) { 66.dp.toPx() }
        composeTestRule.runOnIdle {
            scope.launch {
                state.scrollBy(scrollPx)
            }
        }
        composeTestRule.waitForIdle()

        // After scrolling 66 dp, compact bar title has alpha 1
        composeTestRule.onNodeWithTag("compact_bar_title")
            .assert(SemanticsMatcher.expectValue(CompactTitleAlpha, 1f))
    }

    @Test
    fun navBarHiddenWhenShowNavFalse() {
        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                ShellScaffold(
                    title = "Writer",
                    meta = null,
                    showNav = false,
                    selected = Dest.Write,
                    onNavigate = {}
                ) { _: LazyListState, _: PaddingValues ->
                    Text("Writer Bleed Content")
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("glass_nav_bar").assertDoesNotExist()
        composeTestRule.onAllNodesWithText("Write").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Box").assertCountEquals(0)
    }

    @Test
    fun rtlLayout() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                    ShellScaffold(
                        title = "صندوق",
                        meta = "١٢",
                        showNav = true,
                        selected = Dest.Box,
                        onNavigate = {}
                    ) { _: LazyListState, _: PaddingValues ->
                        Text("محتوى")
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        // Under RTL layout direction, nav items mirror order: Write is on the right, Setup is on the left
        val writeLeft = composeTestRule.onNodeWithText("Write").fetchSemanticsNode().boundsInRoot.left
        val setupLeft = composeTestRule.onNodeWithText("Setup").fetchSemanticsNode().boundsInRoot.left
        assertTrue("In RTL, Setup ($setupLeft) should be to the left of Write ($writeLeft)", setupLeft < writeLeft)
    }

    @Test
    fun fontScale13() {
        composeTestRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = currentDensity.density, fontScale = 1.3f)
            ) {
                EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                    ShellScaffold(
                        title = "Box",
                        meta = "12 notes",
                        showNav = true,
                        selected = Dest.Box,
                        onNavigate = {}
                    ) { _: LazyListState, _: PaddingValues ->
                        Text("Content")
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        for (label in listOf("Write", "Box", "Find", "Setup")) {
            val node = composeTestRule.onNode(hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Tab))
            node.assertIsDisplayed()
            val bounds = node.fetchSemanticsNode().boundsInRoot
            assertTrue("Label $label must have positive size without clipping", bounds.width > 0f && bounds.height > 0f)
        }
    }

    @Test
    fun window360x640() {
        composeTestRule.setContent {
            Box(modifier = Modifier.size(360.dp, 640.dp)) {
                EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                    ShellScaffold(
                        title = "Box",
                        meta = "12 notes",
                        showNav = true,
                        selected = Dest.Box,
                        onNavigate = {}
                    ) { listState: LazyListState, padding: PaddingValues ->
                        LazyColumn(state = listState, contentPadding = padding) {
                            items(20) { index ->
                                Text("Card $index", modifier = Modifier.height(50.dp))
                            }
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("large_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("glass_nav_bar").assertIsDisplayed()
        val navBounds = composeTestRule.onNodeWithTag("glass_nav_bar").fetchSemanticsNode().boundsInRoot
        val density = composeTestRule.density
        val widthDp = with(density) { navBounds.width.toDp() }
        assertTrue("Nav bar width ($widthDp) must fit within 360 dp window", widthDp <= 360.dp)
    }

    @Test
    fun rotationKeepsRoute() {
        val context: Context = RuntimeEnvironment.getApplication()
        val viewModelStore = ViewModelStore()

        val factory = MainViewModel.factory(context)
        val vm1 = ViewModelProvider(viewModelStore, factory)[MainViewModel::class.java]

        vm1.navigate(Route.Box)
        assertEquals(Route.Box, vm1.currentRoute.value)

        // Configuration change: recreate ViewModel using the exact same activity ViewModelStore
        val vm2 = ViewModelProvider(viewModelStore, factory)[MainViewModel::class.java]
        assertEquals(Route.Box, vm2.currentRoute.value)

        viewModelStore.clear()
    }

    @Test
    fun switchingToBoxAndBackPreservesWriterTextAndCaret() {
        val activity = composeTestRule.activity
        val inMemoryDb = Room.inMemoryDatabaseBuilder(activity, EnkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val tempSettingsFile = File(activity.cacheDir, "shell_test_settings_${System.nanoTime()}.preferences_pb")
        val testContainer = object : AppContainer {
            override val db: EnkDatabase = inMemoryDb
            override val repo: NoteRepository = NoteRepository(inMemoryDb, SystemClock)
            override val settingsStore: SettingsStore = SettingsStore(
                PreferenceDataStoreFactory.create(produceFile = { tempSettingsFile })
            )
            override val soundManifest: List<app.enk.audio.SoundSet> = emptyList()
            override val soundPlayer: app.enk.audio.SoundPlayer = object : app.enk.audio.SoundPlayer {
                override fun play(event: app.enk.audio.SoundEvent, set: app.enk.audio.SoundSet, volume: Float) {}
                override fun release() {}
            }
            override val haptics: app.enk.writer.Haptics = object : app.enk.writer.Haptics {
                override fun pulse(ms: Int) {}
            }
        }

        val mainVm = ViewModelProvider(activity, MainViewModel.factory(activity, testContainer)).get(MainViewModel::class.java)

        composeTestRule.setContent {
            EnkNavHost(
                container = testContainer,
                mainViewModel = mainVm
            )
        }
        composeTestRule.waitForIdle()

        // Wait until mainVm initialized note
        composeTestRule.waitUntil(5000) { mainVm.currentNoteId.value != null }
        val noteId = mainVm.currentNoteId.value!!

        val writerVm = ViewModelProvider(activity, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                WriterViewModel(noteId, testContainer.repo, testContainer.settingsStore) as T
        }).get("writer-$noteId", WriterViewModel::class.java)

        composeTestRule.waitUntil(5000) { writerVm.state.value.loaded }

        // Insert text into writer and advance caret
        composeTestRule.runOnIdle {
            writerVm.dispatch(WriterEvent.Insert("Preserved typed text"))
        }
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(5000) {
            writerVm.state.value.pages.isNotEmpty() &&
            writerVm.state.value.pages[0].text == "Preserved typed text"
        }

        val caretBefore = writerVm.state.value.caret
        assertEquals("Preserved typed text", writerVm.state.value.pages[0].text)

        // Navigate to Box
        composeTestRule.runOnIdle {
            mainVm.navigate(Route.Box)
        }
        composeTestRule.waitForIdle()

        // Verify Box placeholder is shown
        composeTestRule.onNode(hasTestTag("large_title") and hasText("The Box")).assertIsDisplayed()

        // Navigate back to Writer
        composeTestRule.runOnIdle {
            mainVm.navigate(Route.Writer)
        }
        composeTestRule.waitForIdle()

        // Verify text and caret are preserved
        assertEquals("Preserved typed text", writerVm.state.value.pages[0].text)
        assertEquals(caretBefore, writerVm.state.value.caret)
    }
}
