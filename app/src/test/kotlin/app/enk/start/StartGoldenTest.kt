package app.enk.start

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.enk.design.EnkColors
import app.enk.design.EnkMaterials
import app.enk.design.EnkType
import app.enk.design.LocalMonoFont
import app.enk.design.logo.EnkWordmark
import app.enk.text.GridLayout
import app.enk.text.StruckPage
import app.enk.writer.Caret
import app.enk.writer.Carriage
import app.enk.writer.CarriageMotion
import app.enk.writer.PaperSheet
import app.enk.writer.PaperText
import app.enk.writer.Rail
import app.enk.writer.TopBarFormatter
import app.enk.writer.WriterGeometry
import app.enk.writer.WriterIntro
import app.enk.writer.introAlpha
import app.enk.writer.introPaper
import app.enk.writer.textColor
import java.io.File
import org.junit.Rule
import org.junit.Test

/**
 * Frames of the whole start animation: backdrop, the writer's own parts (held back through the real
 * [introPaper] and [introAlpha] modifiers) and the overlay, frozen with [SplashHost]'s `progressMs`.
 *
 * Paparazzi cannot run [app.enk.writer.WriterScreen] (it needs Room and an IME), so the writer here is
 * a stand-in built from the same PaperSheet, PaperText, Caret, Rail and Carriage composables and the
 * same intro modifiers. It cannot render the system splash either: t = 0 shows the overlay's first
 * frame, which is what must match the splash.
 */
class StartGoldenTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.NEXUS_5.copy(screenWidth = 1200, screenHeight = 2460),
        maxPercentDifference = 2.0
    )

    private val courier: FontFamily get() = FontFamily(Font(File("../design/src/main/res/font/courier_prime_regular.ttf")))
    private val note = "Dear diary, the rail has\nbecome the writer."

    private fun frame(timeMs: Float, light: Boolean) {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalMonoFont provides courier) {
                SplashHost(
                    firstPageText = note,
                    lightSplash = light,
                    shouldPlay = true,
                    reduceMotion = false,
                    progressMs = timeMs
                ) { intro -> StandInWriter(intro, note) }
            }
        }
    }

    @Test fun darkSplash_t0() = frame(0f, light = false)
    @Test fun darkSplash_t200() = frame(200f, light = false)
    @Test fun darkSplash_t330() = frame(330f, light = false)
    @Test fun darkSplash_t460() = frame(460f, light = false)
    @Test fun darkSplash_t630() = frame(630f, light = false)
    @Test fun darkSplash_t900() = frame(900f, light = false)
    @Test fun lightSplash_t0() = frame(0f, light = true)
    @Test fun lightSplash_t200() = frame(200f, light = true)
    @Test fun lightSplash_t330() = frame(330f, light = true)
    @Test fun lightSplash_t460() = frame(460f, light = true)
    @Test fun lightSplash_t630() = frame(630f, light = true)
    @Test fun lightSplash_t900() = frame(900f, light = true)
}

@Composable
private fun StandInWriter(intro: WriterIntro?, text: String) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .then(if (intro == null) Modifier.background(EnkMaterials.chassisDay) else Modifier)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val geometry = WriterGeometry.compute(maxWidth.value, maxHeight.value, LocalDensity.current.fontScale)
        val page = StruckPage(text)
        val caretPos = GridLayout.caret(GridLayout.wrap(text, geometry.cols), text.length, geometry.cols)

        PaperSheet(
            nightProgress = 0f,
            reduceEffects = false,
            drawChassis = false,
            modifier = Modifier.fillMaxSize().introPaper(intro)
        ) {
            PaperText(
                page = page,
                caret = text.length,
                lastTypedIndex = null,
                geometry = geometry,
                textColor = textColor(0f, app.enk.design.LampColor.fromWarmth(25)),
                reduceMotion = true,
                modifier = Modifier.fillMaxSize().introAlpha(intro?.textAlpha)
            )
            Caret(geometry, caretPos, typing = true, reduceMotion = true, modifier = Modifier.introAlpha(intro?.ownPartsAlpha))
        }
        Rail(railYDp = geometry.railYDp, modifier = Modifier.introAlpha(intro?.ownPartsAlpha))
        Carriage(
            railYDp = geometry.railYDp,
            centerXDp = geometry.carriageCenterXDp(caretPos.col),
            mode = CarriageMotion.Rest,
            slugChar = null,
            strikeKey = 0,
            night = false,
            reduceEffects = true,
            modifier = Modifier.introAlpha(intro?.ownPartsAlpha)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .introAlpha(intro?.topBarAlpha)
                .padding(horizontal = 26.dp)
        ) {
            Row(modifier = Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(EnkColors.accent, CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                EnkWordmark(modifier = Modifier.height(18.dp), onDark = true)
            }
            Text(
                text = TopBarFormatter.formatMeta(0, text.length),
                style = EnkType.meta.copy(fontFamily = LocalMonoFont.current),
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}
