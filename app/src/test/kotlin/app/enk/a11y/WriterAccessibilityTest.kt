package app.enk.a11y

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import app.enk.box.NoteCard
import app.enk.data.NoteSummary
import app.enk.design.theme.EnkShellTheme
import app.enk.design.theme.ThemeId
import app.enk.design.theme.ThemeMode
import app.enk.settings.Settings
import app.enk.testContainer
import app.enk.text.StruckPage
import app.enk.writer.WriterScreen
import app.enk.writer.WriterViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** What TalkBack gets from the real writer screen and the Box's note cards. */
@RunWith(RobolectricTestRunner::class)
class WriterAccessibilityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun showWriter(text: String, onOpenBox: () -> Unit = {}): WriterViewModel {
        val container = testContainer(ApplicationProvider.getApplicationContext(), tempFolder.root)
        val viewModel = runBlocking {
            val note = container.repo.createNote(dividerId = null)
            container.repo.savePages(note.id, listOf(StruckPage(text)))
            WriterViewModel(note.id, container.repo, container.settingsStore, scope)
        }
        composeRule.setContent {
            WriterScreen(
                viewModel = viewModel,
                settings = Settings(),
                player = container.soundPlayer,
                haptics = container.haptics,
                onOpenBox = onOpenBox
            )
        }
        composeRule.waitForIdle()
        return viewModel
    }

    @Test
    fun theRealWriterExposesThePageTextAsEditableText() {
        val text = "The quick brown fox jumps over the lazy dog."
        showWriter(text)

        val editable = composeRule.onAllNodesWithEditableText()
        assertTrue("no node exposes the page text; found $editable", editable.any { it == text })
    }

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.onAllNodesWithEditableText(): List<String?> =
        onAllNodes(androidx.compose.ui.test.isEditable().or(androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)))
            .fetchSemanticsNodes().map { it.config.getOrNull(SemanticsProperties.EditableText)?.text }

    @Test
    fun theWordmarkIsAButtonLabelledOpenTheBoxAndOpensIt() {
        var opened = 0
        showWriter("hello") { opened++ }

        val node = composeRule.onNodeWithTag("wordmark").fetchSemanticsNode()
        assertEquals(Role.Button, node.config.getOrNull(SemanticsProperties.Role))
        assertEquals(listOf("Open the Box"), node.config.getOrNull(SemanticsProperties.ContentDescription))
        composeRule.runOnUiThread { node.config[SemanticsActions.OnClick].action?.invoke() }
        assertEquals(1, opened)
    }

    @Test
    fun theCarriageOffersToggleLampAsAnActionAndAnnouncesTheNewState() {
        val viewModel = showWriter("hello")

        fun node() = composeRule.onNodeWithTag("carriage").fetchSemanticsNode()
        assertEquals("Lamp off", node().config.getOrNull(SemanticsProperties.StateDescription))
        val action = node().config[SemanticsActions.CustomActions].firstOrNull { it.label == "Toggle lamp" }
        assertNotNull("a TalkBack user has no way to toggle the lamp", action)

        composeRule.runOnUiThread { action!!.action() }
        composeRule.waitForIdle()

        assertTrue(viewModel.state.value.lampOn)
        assertEquals("Lamp on", node().config.getOrNull(SemanticsProperties.StateDescription))
    }

    @Test
    fun aNoteCardOffersMoveToDividerAndMoveToTrashAsActionsThatWork() {
        var moved = 0
        var trashed = 0
        composeRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Light) {
                NoteCard(
                    summary = NoteSummary("n1", "Rain on the glass", 1, 1_700_000_000_000L),
                    onClick = {},
                    onHold = {},
                    lifted = false,
                    onMoveToDivider = { moved++ },
                    onMoveToTrash = { trashed++ }
                )
            }
        }
        composeRule.waitForIdle()

        val node = composeRule.onAllNodes(
            androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions)
        ).fetchSemanticsNodes().single()
        val actions = node.config[SemanticsActions.CustomActions]
        assertEquals(listOf("Open note actions", "Move to divider", "Move to trash"), actions.map { it.label })

        composeRule.runOnUiThread { actions.forEach { it.action() } }
        assertEquals(1, moved)
        assertEquals(1, trashed)
    }
}
