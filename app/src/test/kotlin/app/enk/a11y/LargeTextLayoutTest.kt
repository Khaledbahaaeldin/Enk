package app.enk.a11y

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.enk.about.AboutScreen
import app.enk.box.BoxScreen
import app.enk.box.BoxUiState
import app.enk.box.TrashScreen
import app.enk.data.DividerEntity
import app.enk.data.NoteSummary
import app.enk.data.SearchHit
import app.enk.data.TrashedSummary
import app.enk.design.theme.EnkShellTheme
import app.enk.design.theme.ThemeId
import app.enk.design.theme.ThemeMode
import app.enk.search.SearchContent
import app.enk.settings.Settings
import app.enk.settings.SettingsContent
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Box, Find, Trash, Setup and Credits at font scale 1.3 and 2.0 in a 360 x 640 window between real
 * system bars: the content starts below the large title, nothing runs off the sides, and every
 * tappable thing is at least 48 dp. Robolectric's density is 1, so px and dp are the same.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h640dp")
class LargeTextLayoutTest(private val screen: String, private val fontScale: Float) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0} at {1}x")
        fun cases(): List<Array<Any>> =
            listOf("box", "find", "trash", "setup", "credits").flatMap { s ->
                listOf(1.3f, 2.0f).map { arrayOf<Any>(s, it) }
            }
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val statusBarPx = 49
    private val navBarPx = 24
    private var view: View? = null

    private val notes = listOf(
        NoteSummary("n1", "Rain on the glass\nSecond line", 1, 1_700_000_000_000L),
        NoteSummary("n2", "Thunder over the hill", 2, 1_700_001_000_000L)
    )

    @Composable
    private fun Screen() {
        when (screen) {
            "box" -> BoxScreen(
                state = BoxUiState(dividers = listOf(DividerEntity(1L, "Journal", 0)), notes = notes),
                entranceProgress = 1f
            )
            "find" -> SearchContent(
                query = "rain",
                results = listOf(SearchHit("n1", 0, "Rain on the glass")),
                onQueryChange = {},
                onHitClick = { _, _ -> }
            )
            "trash" -> TrashScreen(
                trashed = listOf(TrashedSummary("t1", "Old note", 1L, 5)),
                onRestore = {}, onDeleteAllForever = {}, onNavigate = {}, onNavigateBack = {}
            )
            "setup" -> SettingsContent(settings = Settings(), onSettingsChange = {})
            else -> AboutScreen(onNavigate = {}, onNavigateBack = {})
        }
    }

    private fun firstContentTag() = when (screen) {
        "box" -> "divider_strip"
        "find" -> "search_field"
        "setup" -> "group_appearance"
        "credits" -> "group_app_license"
        else -> null
    }

    private fun bounds(tag: String) = composeTestRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    private fun show() {
        composeTestRule.setContent {
            view = LocalView.current
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Light) { Screen() }
            }
        }
        composeTestRule.runOnUiThread {
            val insets = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, statusBarPx, 0, 0))
                .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, navBarPx))
                .build()
            ViewCompat.dispatchApplyWindowInsets(view!!, insets)
        }
        composeTestRule.waitForIdle()
    }

    private fun allNodes(): List<SemanticsNode> {
        val out = mutableListOf<SemanticsNode>()
        fun walk(n: SemanticsNode) {
            out += n
            n.children.forEach(::walk)
        }
        walk(composeTestRule.onNodeWithTag("large_title").fetchSemanticsNode().let {
            var root = it
            while (root.parent != null) root = root.parent!!
            root
        })
        return out
    }

    @Test
    fun theTitleClearsTheStatusBarAndTheContentClearsTheTitle() {
        show()
        val title = bounds("large_title")
        assertTrue("title top ${title.top} must be below the status bar", title.top >= statusBarPx)
        firstContentTag()?.let { tag ->
            assertTrue("$tag (${bounds(tag).top}) must start below the title (${title.bottom})", bounds(tag).top >= title.bottom)
        }
    }

    @Test
    fun nothingRunsOffTheSidesOfTheWindow() {
        show()
        for (node in allNodes()) {
            val b = node.boundsInRoot
            if (b.width <= 0f || b.height <= 0f) continue
            assertTrue("a node at $b leaves the 360 dp window", b.left >= -0.5f && b.right <= 360.5f)
        }
    }

    @Test
    fun everyTappableThingIsAtLeast48DpInBothDirections() {
        show()
        val tooSmall = allNodes()
            .filter { it.config.getOrNull(SemanticsActions.OnClick) != null }
            // touchBoundsInRoot is the area that takes the touch: Compose grows a small target to 48 dp.
            .filter { it.touchBoundsInRoot.width > 0f && it.touchBoundsInRoot.height > 0f }
            .filter { it.touchBoundsInRoot.width < 47.5f || it.touchBoundsInRoot.height < 47.5f }
            .map {
                val t = it.touchBoundsInRoot
                "${t.width.toInt()}x${t.height.toInt()} at ${t.topLeft} (text ${it.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Text)})"
            }
        assertTrue("touch targets under 48 dp on $screen at ${fontScale}x: $tooSmall", tooSmall.isEmpty())
    }
}
