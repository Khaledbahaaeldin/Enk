package app.enk.writer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
class PaperTextTypeOnTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val referenceGeometry = WriterGeometry.compute(
        widthDp = 400f,
        heightDp = 820f,
        fontScale = 1.0f
    )

    @Test
    fun glyphsRevealedProgressivelyDuringTypeOn() {
        val page = StruckPage("Hello\nWorld")
        var currentTypeOnMs by mutableFloatStateOf(0f)

        composeTestRule.setContent {
            PaperText(
                page = page,
                caret = 0,
                geometry = referenceGeometry,
                textColor = Color.Black,
                reduceMotion = false,
                modifier = Modifier.fillMaxSize(),
                typeOnMs = { currentTypeOnMs }
            )
        }

        // 1. At 0 ms (before LEAD_MS = 120 ms), no glyphs are drawn
        val bitmap0 = composeTestRule.onRoot().captureToImage()
        val pixels0 = IntArray(bitmap0.width * bitmap0.height)
        bitmap0.readPixels(pixels0)
        val bg0 = pixels0[0]
        val hasInk0 = pixels0.any { it != bg0 }
        assertFalse("At 0 ms before lead time, no glyphs should be visible", hasInk0)

        // 2. At 150 ms, line 0 has started and reveals initial glyphs
        currentTypeOnMs = 150f
        composeTestRule.waitForIdle()

        val bitmap150 = composeTestRule.onRoot().captureToImage()
        val pixels150 = IntArray(bitmap150.width * bitmap150.height)
        bitmap150.readPixels(pixels150)
        val bg150 = pixels150[0]
        val hasInk150 = pixels150.any { it != bg150 }
        assertTrue("At 150 ms, first line glyphs should be revealed", hasInk150)

        // 3. At 600 ms, all glyphs are revealed
        currentTypeOnMs = 600f
        composeTestRule.waitForIdle()

        val bitmap600 = composeTestRule.onRoot().captureToImage()
        val pixels600 = IntArray(bitmap600.width * bitmap600.height)
        bitmap600.readPixels(pixels600)
        val bg600 = pixels600[0]
        val inkCount600 = pixels600.count { it != bg600 }
        val inkCount150 = pixels150.count { it != bg150 }
        assertTrue("At 600 ms, more ink must be drawn than at 150 ms", inkCount600 > inkCount150)
    }

    @Test
    fun reduceMotionDrawsAllGlyphsPlainlyIgnoringTypeOn() {
        val page = StruckPage("Hello")

        composeTestRule.setContent {
            PaperText(
                page = page,
                caret = 0,
                geometry = referenceGeometry,
                textColor = Color.Black,
                reduceMotion = true,
                modifier = Modifier.fillMaxSize(),
                typeOnMs = { 0f }
            )
        }

        val bitmap = composeTestRule.onRoot().captureToImage()
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.readPixels(pixels)
        val bg = pixels[0]
        val hasInk = pixels.any { it != bg }
        assertTrue("Reduce motion must draw all glyphs immediately", hasInk)
    }
}
