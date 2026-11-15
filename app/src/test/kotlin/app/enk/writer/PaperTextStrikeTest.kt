package app.enk.writer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import app.enk.text.StruckPage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PaperTextStrikeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val referenceGeometry = WriterGeometry.compute(
        widthDp = 400f,
        heightDp = 820f,
        fontScale = 1.0f
    )

    @Test
    fun glyphNotDrawnAtAge10msAndDrawnAt26ms() {
        val page = StruckPage("A")
        var frozenStrike by mutableStateOf(FrozenStrike(index = 0, ageMs = 10f))

        composeTestRule.setContent {
            PaperText(
                page = page,
                caret = 1,
                geometry = referenceGeometry,
                textColor = Color.Black,
                reduceMotion = false,
                modifier = Modifier.fillMaxSize(),
                frozen = frozenStrike
            )
        }
        val bitmap10 = composeTestRule.onRoot().captureToImage()
        val pixelArray10 = IntArray(bitmap10.width * bitmap10.height)
        bitmap10.readPixels(pixelArray10)
        val bg10 = pixelArray10[0]
        val hasInk10 = pixelArray10.any { it != bg10 }

        frozenStrike = FrozenStrike(index = 0, ageMs = 26f)
        composeTestRule.waitForIdle()

        val bitmap26 = composeTestRule.onRoot().captureToImage()
        val pixelArray26 = IntArray(bitmap26.width * bitmap26.height)
        bitmap26.readPixels(pixelArray26)
        val bg26 = pixelArray26[0]
        val hasInk26 = pixelArray26.any { it != bg26 }

        assertFalse("At 10 ms glyph must not be drawn", hasInk10)
        assertTrue("At 26 ms glyph must be drawn", hasInk26)
    }

    @Test
    fun reduceMotionDrawsAllGlyphsPlainly() {
        val page = StruckPage("A")

        composeTestRule.setContent {
            PaperText(
                page = page,
                caret = 1,
                geometry = referenceGeometry,
                textColor = Color.Black,
                reduceMotion = true,
                modifier = Modifier.fillMaxSize(),
                frozen = FrozenStrike(index = 0, ageMs = 10f)
            )
        }
        val bitmap = composeTestRule.onRoot().captureToImage()
        val pixelArray = IntArray(bitmap.width * bitmap.height)
        bitmap.readPixels(pixelArray)
        val bg = pixelArray[0]
        val hasInk = pixelArray.any { it != bg }

        assertTrue("Reduce motion draws glyphs plainly even if frozen age < 26 ms", hasInk)
    }

    @Test
    fun eraseEntryDrawsGlyphNotInTextAndStopsAt90ms() {
        val strikes = StrikeLog()
        var clock by mutableFloatStateOf(45f)

        val emptyPage = StruckPage("")
        strikes.recordErase(text = "Z", line = 0, col = 0, nowMs = 0f)

        composeTestRule.setContent {
            PaperText(
                page = emptyPage,
                caret = 0,
                geometry = referenceGeometry,
                textColor = Color.Black,
                reduceMotion = false,
                modifier = Modifier.fillMaxSize(),
                strikes = strikes,
                clockMs = { clock }
            )
        }
        val bitmap45 = composeTestRule.onRoot().captureToImage()
        val pixelArray45 = IntArray(bitmap45.width * bitmap45.height)
        bitmap45.readPixels(pixelArray45)
        val bg45 = pixelArray45[0]
        val hasInk45 = pixelArray45.any { it != bg45 }
        assertTrue("Erased glyph must be drawn at 45 ms", hasInk45)

        clock = 95f
        composeTestRule.waitForIdle()

        val bitmap95 = composeTestRule.onRoot().captureToImage()
        val pixelArray95 = IntArray(bitmap95.width * bitmap95.height)
        bitmap95.readPixels(pixelArray95)
        val bg95 = pixelArray95[0]
        val hasInk95 = pixelArray95.any { it != bg95 }
        assertFalse("Erased glyph must be gone after 90 ms", hasInk95)
    }

    private fun inkPixels(): Int {
        val bitmap = composeTestRule.onRoot().captureToImage()
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.readPixels(pixels)
        val bg = pixels[0]
        return pixels.count { it != bg }
    }

    @Test
    fun theStrikeThroughLineGrowsLeftToRightAfterImpact() {
        val strikes = StrikeLog()
        var clock by mutableFloatStateOf(10f)
        strikes.recordStruck(index = 0, weight = 1f, nowMs = 0f)
        // The impact flash belongs to the newest entry; a later key far away keeps it off this glyph,
        // so only the strike line changes the ink here.
        strikes.recordKey(index = 99, weight = 1f, nowMs = 1f)

        composeTestRule.setContent {
            PaperText(
                page = StruckPage("A", struck = listOf(0..0)),
                caret = 1,
                geometry = referenceGeometry,
                textColor = Color.Black,
                reduceMotion = false,
                modifier = Modifier.fillMaxSize(),
                strikes = strikes,
                clockMs = { clock }
            )
        }
        val beforeImpact = inkPixels()
        clock = 46f // 20 ms after impact: half the line
        composeTestRule.waitForIdle()
        val half = inkPixels()
        clock = 80f // 54 ms after impact: the whole line
        composeTestRule.waitForIdle()
        val whole = inkPixels()
        // The line is also drawn whole once nothing animates: the entry has expired by 130 ms.
        clock = 200f
        composeTestRule.waitForIdle()
        val settled = inkPixels()

        assertTrue("no line before impact ($beforeImpact) and a half line at 20 ms ($half)", half > beforeImpact)
        assertTrue("the whole line ($whole) is longer than the half line ($half)", whole > half)
        assertTrue("the settled strike-through ($settled) equals the full line ($whole)", settled == whole)
    }
}
