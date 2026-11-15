package app.enk.shell

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.test.core.app.ApplicationProvider
import app.enk.EnkSnapshots
import app.enk.MainViewModel
import app.enk.design.LocalMonoFont
import app.enk.design.effects.LocalEffects
import app.enk.design.effects.ResolvedEffects
import app.enk.design.shell.LocalReduceEffects
import app.enk.design.theme.EnkShellTheme
import app.enk.design.theme.ThemeId
import app.enk.design.theme.ThemeMode
import app.enk.testContainer
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The real shell playing the Rail transition: the Box, then Write. Frames are taken at fixed times of the
 * test clock; the progress spring reaches 0.35 at about 130 ms, 0.6 at 200 ms and 0.9 (the ding) at 325 ms.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RailTransitionGoldenTest {

    @get:Rule
    val snapshots = EnkSnapshots(RailTransitionGoldenTest::class.java)

    private val courier: FontFamily
        get() = FontFamily(Font(File("../design/src/main/res/font/courier_prime_regular.ttf")))

    private fun frame(name: String, atMs: Long) {
        val rule = snapshots.composeRule
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = testContainer(context, context.cacheDir)
        val vm = MainViewModel(context, container, Dispatchers.Unconfined)
        vm.navigate(Route.Box)
        rule.setContent {
            CompositionLocalProvider(
                LocalMonoFont provides courier,
                LocalEffects provides ResolvedEffects.Light,
                LocalReduceEffects provides false
            ) {
                EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Light) {
                    EnkNavHost(container = container, mainViewModel = vm)
                }
            }
        }
        // The Box lists the note the view model created; the frames must not depend on that race.
        rule.waitUntil(5_000) { rule.onAllNodesWithText("1 note").fetchSemanticsNodes().isNotEmpty() }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        vm.navigate(Route.Writer)
        rule.mainClock.advanceTimeBy(atMs)

        val file = File("src/test/snapshots/images/app.enk.shell_RailTransitionGoldenTest_$name.png")
        file.parentFile?.mkdirs()
        rule.onRoot().captureRoboImage(
            filePath = file.path,
            roborazziOptions = RoborazziOptions(
                compareOptions = RoborazziOptions.CompareOptions(changeThreshold = EnkSnapshots.DEFAULT_THRESHOLD)
            )
        )
    }

    @Test
    fun theBoxBeforeWrite() = frame("theBoxBeforeWrite", 0)

    @Test
    fun writeAt130ms() = frame("writeAt130ms", 130)

    @Test
    fun writeAt200ms() = frame("writeAt200ms", 200)

    @Test
    fun writeAt330ms() = frame("writeAt330ms", 330)

    @Test
    fun writeSettled() = frame("writeSettled", 3_000)
}
