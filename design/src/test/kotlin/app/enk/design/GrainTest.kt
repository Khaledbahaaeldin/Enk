package app.enk.design

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.toPixelMap
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GrainTest {

    @Test
    fun grainConstants() {
        assertEquals(200, Grain.TILE)
        assertEquals(4, Grain.SEED)
    }

    @Test
    fun grainDeterministic() {
        val tile1 = Grain.tile()
        val tile2 = Grain.tile()
        assertArrayEquals(tile1.toPixelMap().buffer, tile2.toPixelMap().buffer)
    }

    @Test
    fun grainSize() {
        val t = Grain.tile()
        assertEquals(200, t.width)
        assertEquals(200, t.height)
    }

    @Test
    fun generateGrainSourceFileExistsAndMatchesSpec() {
        val candidatePaths = listOf(
            File("tools/GenerateGrain.kt"),
            File("../tools/GenerateGrain.kt"),
            File("../../tools/GenerateGrain.kt")
        )
        val file = candidatePaths.firstOrNull { it.exists() }
        assertTrue("tools/GenerateGrain.kt should exist", file != null && file.exists())

        val content = file!!.readText()
        assertTrue(content.contains("val tile = 200"))
        assertTrue(content.contains("val seed = 4"))
        assertTrue(content.contains("val octaves = 2"))
        assertTrue(content.contains("val baseFrequency = 0.9f"))
        assertTrue(content.contains("0x73"))
        assertTrue(content.contains("0x66"))
        assertTrue(content.contains("0x50"))
    }

    @Test
    fun generatorCreatesValidFileMatchingGrainTile() {
        val pixels = Grain.generatePixels(Grain.TILE, Grain.SEED)
        val bitmap = Bitmap.createBitmap(pixels, Grain.TILE, Grain.TILE, Bitmap.Config.ARGB_8888)

        val tempFile = File.createTempFile("test_grain_", ".png")
        tempFile.deleteOnExit()

        FileOutputStream(tempFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        assertTrue("Generated grain file should exist and have bytes", tempFile.exists() && tempFile.length() > 0)

        // Verify PNG magic header: 89 50 4E 47 0D 0A 1A 0A
        val header = tempFile.readBytes().take(8)
        val expectedHeader = listOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(), 0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte())
        assertEquals("File must be a valid PNG", expectedHeader, header)

        val options = BitmapFactory.Options().apply {
            inPremultiplied = false
        }
        val decoded = BitmapFactory.decodeFile(tempFile.absolutePath, options)
        assertEquals(200, decoded.width)
        assertEquals(200, decoded.height)

        val decodedPixels = IntArray(200 * 200)
        decoded.getPixels(decodedPixels, 0, 200, 0, 0, 200, 200)

        // Compare decoded PNG pixels: alpha is exact across all 40,000 pixels
        val tilePixelMap = Grain.tile().toPixelMap()
        for (y in 0 until 200) {
            for (x in 0 until 200) {
                val expected = pixels[y * 200 + x]
                val actual = decodedPixels[y * 200 + x]

                val expA = (expected ushr 24) and 0xFF
                val actA = (actual ushr 24) and 0xFF
                assertEquals("Alpha at ($x, $y) must match exactly", expA, actA)

                // For pixels with sufficient opacity (alpha >= 60), RGB roundtrips cleanly
                if (expA >= 60) {
                    val expR = (expected ushr 16) and 0xFF
                    val actR = (actual ushr 16) and 0xFF
                    assertTrue("Red at ($x, $y) must match within tolerance", kotlin.math.abs(expR - actR) <= 2)

                    val expG = (expected ushr 8) and 0xFF
                    val actG = (actual ushr 8) and 0xFF
                    assertTrue("Green at ($x, $y) must match within tolerance", kotlin.math.abs(expG - actG) <= 2)

                    val expB = expected and 0xFF
                    val actB = actual and 0xFF
                    assertTrue("Blue at ($x, $y) must match within tolerance", kotlin.math.abs(expB - actB) <= 2)
                }

                // Grain.tile() pixel map matches alpha
                val composeColor = tilePixelMap[x, y]
                val composeAlpha = (composeColor.alpha * 255f).toInt()
                assertEquals("Compose alpha at ($x, $y) must match", expA, composeAlpha)
            }
        }
    }

    @Test
    fun grainPixelColorAndAlphaBoundsAcrossAllPixels() {
        val pixels = Grain.generatePixels(Grain.TILE, Grain.SEED)
        for (i in pixels.indices) {
            val p = pixels[i]
            val rgb = p and 0x00FFFFFF
            assertEquals("Every pixel RGB must be 0x736650", 0x00736650, rgb)

            val alpha = (p ushr 24) and 0xFF
            assertTrue("Alpha must be between 0 and 128 (0.5 * noise)", alpha in 0..128)
        }
    }

    @Test
    fun grainHashDeterminismAndRange() {
        val h1 = Grain.hash2D(10, 20, Grain.SEED)
        val h2 = Grain.hash2D(10, 20, Grain.SEED)
        assertEquals(h1, h2, 0.000001f)
        assertTrue(h1 in 0f..1f)

        for (x in 0..20) {
            for (y in 0..20) {
                val h = Grain.hash2D(x, y, Grain.SEED)
                assertTrue("hash must be in 0..1", h in 0f..1f)
            }
        }
    }

    @Test
    fun grainBoundaryTileSize1() {
        val pixels = Grain.generatePixels(1, Grain.SEED)
        assertEquals(1, pixels.size)
        assertEquals(0x00736650, pixels[0] and 0x00FFFFFF)
    }
}
