package app.enk.start

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import app.enk.MainViewModel
import app.enk.design.theme.EnkShellTheme
import app.enk.design.theme.ThemeId
import app.enk.design.theme.ThemeMode
import app.enk.shell.EnkNavHost
import app.enk.testContainer
import app.enk.text.StruckPage
import app.enk.writer.WriterIntro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h820dp")
class StartHandoffTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    /** Only a guard against a hang: the waits end as soon as the work is done. */
    private val hangGuardMs = 60_000L

    // ---- the host, with a stand-in writer -------------------------------------------------

    private class Probe {
        var intro: WriterIntro? = null
        var contentRuns = 0
        var finished = 0
    }

    private fun setHost(probe: Probe, shouldPlay: Boolean = true, reduceMotion: Boolean = false, progressMs: Float? = null) {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            SplashHost(
                firstPageText = "Hello",
                lightSplash = false,
                shouldPlay = shouldPlay,
                reduceMotion = reduceMotion,
                progressMs = progressMs,
                onFinished = { probe.finished++ }
            ) { intro ->
                probe.intro = intro
                probe.contentRuns++
                Text(text = "Writer", modifier = Modifier.testTag("writer_screen"))
            }
        }
    }

    private fun advance(ms: Long) = composeTestRule.mainClock.advanceTimeBy(ms)

    @Test
    fun touchSkipsWithin150ms() {
        val probe = Probe()
        setHost(probe)
        advance(100)
        assertNotNull("the writer is held back while the animation runs", probe.intro)
        composeTestRule.onNodeWithTag("start_overlay").performClick()

        advance(50)
        assertEquals("still easing at 50 ms after the touch", 0, probe.finished)
        advance(100)
        assertEquals("the skip must be over within 150 ms of the touch", 1, probe.finished)
        composeTestRule.onAllNodesWithTag("start_overlay").assertCountEquals(0)
        assertNull("the writer is released", probe.intro)
    }

    @Test
    fun coldStartEndsOnTheWriter() {
        val probe = Probe()
        setHost(probe)
        composeTestRule.onNodeWithTag("start_overlay").assertExists()
        composeTestRule.onNodeWithTag("start_backdrop").assertExists()

        advance(950)
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithTag("start_overlay").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("start_backdrop").assertCountEquals(0)
        composeTestRule.onNodeWithTag("writer_screen").assertIsDisplayed()
        assertNull(probe.intro)
        assertEquals(1, probe.finished)
    }

    @Test
    fun widgetLaunchSkipsTheAnimationEntirely() {
        val probe = Probe()
        setHost(probe, shouldPlay = false)
        composeTestRule.onAllNodesWithTag("start_overlay").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("start_backdrop").assertCountEquals(0)
        composeTestRule.onNodeWithTag("writer_screen").assertIsDisplayed()
        assertNull(probe.intro)
        assertEquals("nothing played, nothing to finish", 0, probe.finished)
    }

    @Test
    fun reducedMotionIsAStillFadeOverTheFinishedWriter() {
        val probe = Probe()
        setHost(probe, reduceMotion = true)
        composeTestRule.onNodeWithTag("start_overlay").assertExists()
        composeTestRule.onAllNodesWithTag("start_backdrop").assertCountEquals(0)
        assertNull("no rise and no fade-in: the writer is already as it will be", probe.intro)

        advance(150)
        assertEquals(0, probe.finished)
        advance(110)
        assertEquals("the fade is 250 ms", 1, probe.finished)
        advance(32)
        composeTestRule.onAllNodesWithTag("start_overlay").assertCountEquals(0)
    }

    @Test
    fun theWriterIsNotRecomposedOncePerFrame() {
        val probe = Probe()
        setHost(probe)
        val before = probe.contentRuns
        repeat(60) { advance(16) }
        assertTrue(
            "the content lambda ran ${probe.contentRuns - before} times in 60 frames; progress must be read in the draw phase",
            probe.contentRuns - before <= 3
        )
    }

    @Test
    fun theIntroHoldsTheWriterBackThenReleasesItInOrder() {
        val probe = Probe()
        setHost(probe)
        val intro = probe.intro!!
        val start = intro.paperOffsetDp()
        assertTrue("paper starts below the screen", start > 0f)
        assertEquals(0f, intro.textAlpha(), 0f)
        assertEquals(0f, intro.topBarAlpha(), 0f)
        assertEquals(0f, intro.ownPartsAlpha(), 0f)

        advance(300)
        val midPaper = intro.paperOffsetDp()
        assertTrue("the paper is rising ($midPaper of $start)", midPaper > 0f && midPaper < start)

        advance(450) // 750 ms
        assertEquals("paper landed", 0f, intro.paperOffsetDp(), 0.001f)
        assertEquals("text is in", 1f, intro.textAlpha(), 0.001f)
        assertEquals("own rail, carriage and caret are in", 1f, intro.ownPartsAlpha(), 0.01f)
        assertTrue("the top bar is still fading in", intro.topBarAlpha() < 1f)
    }

    @Test
    fun aFrozenProgressNeverAdvances() {
        val probe = Probe()
        setHost(probe, progressMs = 450f)
        val intro = probe.intro!!
        advance(2000)
        assertEquals(0f, intro.textAlpha(), 0f)
        assertEquals("a frozen frame never finishes", 0, probe.finished)
        composeTestRule.onNodeWithTag("start_overlay").assertExists()
    }

    // ---- the real writer -------------------------------------------------------------------

    private class Measured {
        var targets: StartTargets? = null
        var view: View? = null
    }

    /**
     * Runs the real writer inside the same window, insets and font scale as [rememberStartTargets]
     * and checks that the animation's last frame is exactly where the writer draws its own rail,
     * carriage and caret. Robolectric's density is 1, so px and dp are the same here.
     */
    private fun assertLandsOnTheWriter(topInsetPx: Int, bottomInsetPx: Int, fontScale: Float) {
        val activity = composeTestRule.activity
        val container = testContainer(activity, activity.cacheDir)
        val text = "Hello typewriter"
        runBlocking {
            val note = container.repo.createNote(dividerId = null)
            container.repo.savePages(note.id, listOf(StruckPage(text)))
        }
        val viewModel = ViewModelProvider(activity, MainViewModel.factory(activity, container, Dispatchers.Unconfined))[MainViewModel::class.java]
        val measured = Measured()

        composeTestRule.setContent {
            measured.view = LocalView.current
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val targets = rememberStartTargets(maxWidth, maxHeight, text, lightSplash = false)
                        measured.targets = targets
                        val end = StartTimeline.at(StartTimeline.DURATION_MS.toFloat(), targets)
                        val intro = WriterIntro(
                            paperOffsetDp = { end.paperOffsetY },
                            textAlpha = { end.textAlpha },
                            topBarAlpha = { end.topBarAlpha },
                            ownPartsAlpha = { end.ownPartsAlpha }
                        )
                        EnkNavHost(container = container, mainViewModel = viewModel, intro = intro)
                    }
                }
            }
        }
        if (topInsetPx > 0 || bottomInsetPx > 0) {
            composeTestRule.runOnUiThread {
                val insets = WindowInsetsCompat.Builder()
                    .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, topInsetPx, 0, 0))
                    .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, bottomInsetPx))
                    .build()
                ViewCompat.dispatchApplyWindowInsets(measured.view!!, insets)
            }
        }
        composeTestRule.waitUntil(hangGuardMs) {
            listOf("rail_rod", "carriage", "writer_caret").all {
                composeTestRule.onAllNodesWithTag(it).fetchSemanticsNodes().isNotEmpty()
            }
        }
        composeTestRule.waitForIdle()

        val targets = measured.targets!!
        val expectedInsets = StartLayout.targets(400f, 820f, InsetsDp(0f, topInsetPx.toFloat(), 0f, bottomInsetPx.toFloat()), fontScale, text, false)
        assertEquals("the host saw the insets the test applied", expectedInsets.railY, targets.railY, 0.01f)

        val frame = StartTimeline.at(StartTimeline.DURATION_MS.toFloat(), targets)
        val rail = composeTestRule.onNodeWithTag("rail_rod").fetchSemanticsNode().boundsInRoot
        val carriage = composeTestRule.onNodeWithTag("carriage").fetchSemanticsNode().boundsInRoot
        val caret = composeTestRule.onNodeWithTag("writer_caret").fetchSemanticsNode().boundsInRoot

        val label = "insets=($topInsetPx,$bottomInsetPx) fontScale=$fontScale"
        assertEquals("$label rail left", frame.rail.left, rail.left, 0.5f)
        assertEquals("$label rail top", frame.rail.top, rail.top, 0.5f)
        assertEquals("$label rail width", frame.rail.width, rail.width, 0.5f)
        assertEquals("$label rail height", frame.rail.height, rail.height, 0.5f)
        assertEquals("$label carriage x", frame.carriage.centre.x, carriage.center.x, 0.5f)
        assertEquals("$label carriage y", frame.carriage.centre.y, carriage.center.y, 0.5f)
        assertEquals("$label caret left", frame.caret.left, caret.left, 0.5f)
        assertEquals("$label caret top", frame.caret.top, caret.top, 0.5f)
        assertEquals("$label caret width", frame.caret.width, caret.width, 0.5f)
        assertEquals("$label caret height", frame.caret.height, caret.height, 0.5f)
    }

    @Test
    fun theLastFrameLandsOnTheWritersOwnPartsWithNoInsets() = assertLandsOnTheWriter(0, 0, 1f)

    @Test
    fun theLastFrameLandsOnTheWritersOwnPartsBetweenTheSystemBars() = assertLandsOnTheWriter(49, 24, 1f)

    @Test
    fun theLastFrameLandsOnTheWritersOwnPartsAtFontScale13() = assertLandsOnTheWriter(49, 24, 1.3f)
}
