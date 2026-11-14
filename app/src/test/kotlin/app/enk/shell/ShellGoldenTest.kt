package app.enk.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.enk.EnkSnapshots
import app.enk.design.EnkColors
import app.enk.design.LocalMonoFont
import app.enk.design.shell.ShellBackground
import app.enk.design.shell.glassSource
import app.enk.design.shell.rememberGlassSource
import app.enk.design.theme.EnkShellTheme
import app.enk.design.theme.ThemeId
import app.enk.design.theme.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShellGoldenTest {

    @get:Rule
    val snapshots = EnkSnapshots(ShellGoldenTest::class.java)

    private val courier: FontFamily
        get() = FontFamily(Font(File("../design/src/main/res/font/courier_prime_regular.ttf")))

    @Composable
    private fun NavBarStripScene(theme: ThemeId, mode: ThemeMode) {
        CompositionLocalProvider(LocalMonoFont provides courier) {
            EnkShellTheme(theme = theme, mode = mode) {
                val glassSource = rememberGlassSource()
                ShellBackground(modifier = Modifier.fillMaxSize()) {
                    // Note card strip sample under the nav bar
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .glassSource(glassSource)
                            .padding(horizontal = 20.dp, vertical = 40.dp)
                    ) {
                        repeat(5) { i ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .background(EnkColors.paperDay, RoundedCornerShape(14.dp))
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Note card #${i + 1}",
                                    color = Color(0xFF22201E)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    GlassNavBar(
                        selected = Dest.Box,
                        onSelect = {},
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 22.dp)
                    )
                }
            }
        }
    }

    @Test
    fun navBarGraphiteDark() {
        snapshots.snapshot {
            NavBarStripScene(theme = ThemeId.Graphite, mode = ThemeMode.Dark)
        }
    }

    @Test
    fun navBarApricotLight() {
        snapshots.snapshot {
            NavBarStripScene(theme = ThemeId.Apricot, mode = ThemeMode.Light)
        }
    }

    @Test
    fun navBarSlateDark() {
        snapshots.snapshot {
            NavBarStripScene(theme = ThemeId.Slate, mode = ThemeMode.Dark)
        }
    }

    @Test
    fun navBarDynamicDark() {
        snapshots.snapshot {
            NavBarStripScene(theme = ThemeId.Dynamic, mode = ThemeMode.Dark)
        }
    }

    @Test
    fun scaffoldTitleCollapsed() {
        snapshots.snapshot {
            CompositionLocalProvider(LocalMonoFont provides courier) {
                EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                    val listState = LazyListState(firstVisibleItemIndex = 1, firstVisibleItemScrollOffset = 500)
                    ShellScaffold(
                        title = "Box",
                        meta = "12 notes",
                        showNav = true,
                        selected = Dest.Box,
                        onNavigate = {},
                        listState = listState
                    ) { state: LazyListState, contentPadding: PaddingValues ->
                        LazyColumn(
                            state = state,
                            contentPadding = contentPadding
                        ) {
                            items(30) { index ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(90.dp)
                                        .padding(horizontal = 20.dp, vertical = 6.dp)
                                        .background(EnkColors.paperDay, RoundedCornerShape(14.dp))
                                        .padding(14.dp)
                                ) {
                                    Text("Card $index", color = Color(0xFF22201E))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
