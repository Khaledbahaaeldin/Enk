package app.enk.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import app.enk.EnkSnapshots
import app.enk.design.LocalMonoFont
import app.enk.design.theme.EnkShellTheme
import app.enk.design.theme.LocalShellColors
import app.enk.design.theme.ThemeId
import app.enk.design.theme.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class KeyboardGoldenTest {

    @get:Rule
    val snapshots = EnkSnapshots(KeyboardGoldenTest::class.java, screenWidthDp = 400, screenHeightDp = 820)

    private val courier: FontFamily
        get() = FontFamily(Font(File("../design/src/main/res/font/courier_prime_regular.ttf")))

    private val layout: KeyboardLayout by lazy {
        LayoutLoader.load(RuntimeEnvironment.getApplication())
    }

    private val metrics = KeyboardMetrics.forWindow(820f)

    @Composable
    private fun KeyboardScene(
        themeMode: ThemeMode,
        state: KeyboardState
    ) {
        CompositionLocalProvider(LocalMonoFont provides courier) {
            EnkShellTheme(theme = ThemeId.Graphite, mode = themeMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LocalShellColors.current.bg),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    EnkKeyboard(
                        layout = layout,
                        metrics = metrics,
                        onFeedback = {},
                        onApply = {},
                        onAlternative = { _, _ -> },
                        caretProvider = { 0 },
                        initialState = state
                    )
                }
            }
        }
    }

    @Test
    fun lettersLight() {
        snapshots.snapshot {
            KeyboardScene(
                themeMode = ThemeMode.Light,
                state = KeyboardState(page = PageId.Letters, shift = Shift.Off)
            )
        }
    }

    @Test
    fun lettersDarkShiftOn() {
        snapshots.snapshot {
            KeyboardScene(
                themeMode = ThemeMode.Dark,
                state = KeyboardState(page = PageId.Letters, shift = Shift.Once)
            )
        }
    }

    @Test
    fun numbersLight() {
        snapshots.snapshot {
            KeyboardScene(
                themeMode = ThemeMode.Light,
                state = KeyboardState(page = PageId.Numbers, shift = Shift.Off)
            )
        }
    }

    @Test
    fun capsLockDark() {
        snapshots.snapshot {
            KeyboardScene(
                themeMode = ThemeMode.Dark,
                state = KeyboardState(page = PageId.Letters, shift = Shift.Caps)
            )
        }
    }
}
