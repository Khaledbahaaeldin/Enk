package app.enk.writer

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ApplicationProvider
import app.enk.settings.Settings
import app.enk.testContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The writer in landscape with the keyboard up. On a device the paper was one line tall and the hint
 * sat on the text. Robolectric's density is 1, so 120 px of keyboard here stands for the 300 px
 * (about 110 dp) a phone keyboard takes at density 2.75.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w820dp-h360dp")
class WriterCompactLayoutTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var view: View? = null

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun applyInsets(keyboardPx: Int) {
        composeTestRule.runOnUiThread {
            val builder = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, 24, 0, 0))
            if (keyboardPx > 0) {
                builder.setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, keyboardPx))
                builder.setVisible(WindowInsetsCompat.Type.ime(), true)
            }
            ViewCompat.dispatchApplyWindowInsets(view!!, builder.build())
        }
        composeTestRule.waitForIdle()
    }

    private fun showWriter(text: String) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val container = testContainer(context, tempFolder.root)
        val viewModel = runBlocking {
            val note = container.repo.createNote(dividerId = null)
            container.repo.savePages(note.id, listOf(app.enk.text.StruckPage(text)))
            WriterViewModel(note.id, container.repo, container.settingsStore, scope)
        }
        composeTestRule.setContent {
            view = LocalView.current
            WriterScreen(
                viewModel = viewModel,
                settings = Settings(),
                player = container.soundPlayer,
                haptics = container.haptics,
                onOpenBox = {}
            )
        }
        composeTestRule.waitForIdle()
    }

    private fun bottom(tag: String) = composeTestRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.bottom

    @Test
    fun withTheKeyboardUpTheCarriageAndTheCaretLineAreAboveTheKeyboardAndTheHintIsHidden() {
        showWriter("The quick brown fox")
        applyInsets(keyboardPx = 120)

        val windowHeight = 360f
        assertTrue(
            "carriage bottom ${bottom("carriage")} must be above the keyboard (${windowHeight - 120f})",
            bottom("carriage") <= windowHeight - 120f
        )
        composeTestRule.onAllNodesWithText("drag marker to scrub", substring = true)
            .fetchSemanticsNodes().let { assertEquals("the hint must be hidden on short paper", 0, it.size) }
    }

    @Test
    fun theCaretLineAlwaysFitsAndTheCarriageStaysInTheWindowWheneverBothCan() {
        for (height in listOf(1f, 60f, 100f, 119f, 143f, 216f, 300f, 336f, 359f)) {
            val g = WriterGeometry.compute(820f, height, 1f)
            // The caret line sits at railY - paperTop - gap - lineHeight below the top of the paper.
            val caretLineTop = g.railYDp - g.paperTopDp - WriterGeometry.RAIL_TEXT_GAP_DP - g.lineHeightDp
            assertTrue("h=$height: the caret line starts ${caretLineTop}dp into the paper, above its top edge", caretLineTop >= -0.01f)
            assertTrue("h=$height: ${g.fontSp}sp is below the compact minimum", g.fontSp >= WriterGeometry.COMPACT_MIN_FONT_SP - 0.001f || height < 1f)
            val carriageBottom = g.railYDp + WriterGeometry.CARRIAGE_BELOW_RAIL_DP
            // 119 dp is what a phone keeps above a 300 px keyboard in landscape (the case seen on a device).
            if (height >= 119f) assertTrue("h=$height: the carriage bottom ($carriageBottom) leaves the window", carriageBottom <= height + 0.01f)
        }
    }

    @Test
    fun aShortWindowWithoutTheKeyboardKeepsRoomyType() {
        // Landscape, no keyboard: 336 dp of height is plenty, so the font is not squeezed.
        assertEquals(WriterGeometry.compute(820f, 336f, 1f).fontSp, 22f, 0.001f)
        assertTrue(WriterGeometry.compute(820f, 119f, 1f).fontSp < 16f)
    }

    @Test
    fun aTallWindowKeepsTheOriginalGeometry() {
        val g = WriterGeometry.compute(400f, 820f, 1f)
        assertEquals((400f - 72f) / (28 * 0.6f), g.fontSp, 0.001f)
        assertEquals(Math.round(820f * 0.62f).toFloat(), g.railYDp, 0.001f)
        assertTrue(g.railYDp <= 820f - WriterGeometry.PANEL_CLEARANCE_DP)
        assertTrue(!WriterGeometry.isCompact(820f))
        assertTrue(!WriterGeometry.isCompact(Float.NaN))
        assertTrue(WriterGeometry.isCompact(359f))
    }
}
