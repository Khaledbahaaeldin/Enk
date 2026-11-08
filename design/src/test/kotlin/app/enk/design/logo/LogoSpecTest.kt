package app.enk.design.logo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.sqrt

class LogoSpecTest {

    @Test
    fun linesMatchLogoSpec() {
        assertEquals(Rect(36f, 24f, 64f, 26.6f), LogoSpec.typedLines[0])
        assertEquals(Rect(36f, 29.5f, 58f, 32.1f), LogoSpec.typedLines[1])
        assertEquals(Rect(36f, 35f, 54f, 37.6f), LogoSpec.typedLines[2])
    }

    @Test
    fun caretFollowsLastLine() {
        assertEquals(55.5f, LogoSpec.caret.left, 0.001f)
        assertEquals(1.5f, LogoSpec.caret.left - LogoSpec.typedLines[2].right, 0.001f)
    }

    @Test
    fun contentInsideVisibleCircle() {
        val center = Offset(54f, 54f)
        val maxRadius = LogoSpec.VISIBLE_DIAMETER / 2f // 36f

        fun assertInside(point: Offset, name: String) {
            val dist = sqrt((point.x - center.x) * (point.x - center.x) + (point.y - center.y) * (point.y - center.y))
            assertTrue(
                "$name ($point) distance $dist exceeds max radius $maxRadius",
                dist <= maxRadius + 0.001f
            )
        }

        fun assertRectInside(rect: Rect, name: String) {
            assertInside(rect.topLeft, "$name topLeft")
            assertInside(rect.topRight, "$name topRight")
            assertInside(rect.bottomLeft, "$name bottomLeft")
            assertInside(rect.bottomRight, "$name bottomRight")
        }

        LogoSpec.typedLines.forEachIndexed { i, line ->
            assertRectInside(line, "typedLine[$i]")
        }
        assertRectInside(LogoSpec.caret, "caret")

        // Carriage body: center (61, 60.25), scale 1.25, unscaled body 30 x 21
        val halfW = 15f * LogoSpec.CARRIAGE_SCALE
        val halfH = 10.5f * LogoSpec.CARRIAGE_SCALE
        val carriageBody = Rect(
            left = LogoSpec.carriageCentre.x - halfW,
            top = LogoSpec.carriageCentre.y - halfH,
            right = LogoSpec.carriageCentre.x + halfW,
            bottom = LogoSpec.carriageCentre.y + halfH
        )
        assertRectInside(carriageBody, "carriageBody")
    }

    @Test
    fun railRunsPastTheMask() {
        assertTrue(LogoSpec.rail.left < 18f && LogoSpec.rail.right > 90f)
    }

    @Test
    fun orangeOnlyInCaretAndStrip() {
        val file = File("src/main/res/drawable/ic_launcher_foreground.xml").let {
            if (it.exists()) it else File("design/src/main/res/drawable/ic_launcher_foreground.xml")
        }
        assertTrue("ic_launcher_foreground.xml must exist", file.exists())

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val paths = doc.getElementsByTagName("path")
        val orangeHex = "#FF5A1F"

        var foundOrangeCount = 0
        for (i in 0 until paths.length) {
            val elem = paths.item(i) as Element
            val fill = elem.getAttribute("android:fillColor").uppercase()
            if (fill == orangeHex || fill == "#FFFF5A1F") {
                foundOrangeCount++
                val pathName = elem.getAttribute("android:name")
                assertTrue(
                    "Orange found on unexpected path '$pathName'",
                    pathName.contains("caret", ignoreCase = true) || pathName.contains("strip", ignoreCase = true)
                )
            }
        }
        assertEquals("Orange (#FF5A1F) should appear exactly twice (caret and strip)", 2, foundOrangeCount)
    }

    @Test
    fun splashVariantsDifferOnlyInTypedLines() {
        fun resolveFile(name: String): File {
            val f = File("src/main/res/drawable/$name")
            return if (f.exists()) f else File("design/src/main/res/drawable/$name")
        }

        val darkFile = resolveFile("ic_splash_symbol.xml")
        val lightFile = resolveFile("ic_splash_symbol_light.xml")
        assertTrue("ic_splash_symbol.xml must exist", darkFile.exists())
        assertTrue("ic_splash_symbol_light.xml must exist", lightFile.exists())

        val db = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val darkDoc = db.parse(darkFile)
        val lightDoc = db.parse(lightFile)

        val darkPaths = darkDoc.getElementsByTagName("path")
        val lightPaths = lightDoc.getElementsByTagName("path")
        assertEquals(darkPaths.length, lightPaths.length)

        for (i in 0 until darkPaths.length) {
            val dElem = darkPaths.item(i) as Element
            val lElem = lightPaths.item(i) as Element

            val dName = dElem.getAttribute("android:name")
            val lName = lElem.getAttribute("android:name")
            assertEquals(dName, lName)
            assertEquals(dElem.getAttribute("android:pathData"), lElem.getAttribute("android:pathData"))

            if (dName.contains("line", ignoreCase = true)) {
                assertEquals("#F3EEE3", dElem.getAttribute("android:fillColor").uppercase())
                assertEquals("#2B2824", lElem.getAttribute("android:fillColor").uppercase())
                assertEquals("0.34", dElem.getAttribute("android:fillAlpha"))
                assertEquals("0.34", lElem.getAttribute("android:fillAlpha"))
            } else {
                assertEquals(dElem.getAttribute("android:fillColor"), lElem.getAttribute("android:fillColor"))
                assertEquals(dElem.getAttribute("android:fillAlpha"), lElem.getAttribute("android:fillAlpha"))
            }
        }
    }

    private fun getPngDimensions(file: File): Pair<Int, Int> {
        val bytes = file.readBytes()
        require(bytes.size >= 24) { "PNG file is too short" }
        val width = (bytes[16].toInt() and 0xFF shl 24) or
                (bytes[17].toInt() and 0xFF shl 16) or
                (bytes[18].toInt() and 0xFF shl 8) or
                (bytes[19].toInt() and 0xFF)
        val height = (bytes[20].toInt() and 0xFF shl 24) or
                (bytes[21].toInt() and 0xFF shl 16) or
                (bytes[22].toInt() and 0xFF shl 8) or
                (bytes[23].toInt() and 0xFF)
        return Pair(width, height)
    }

    @Test
    fun storeIconIs512Square() {
        val root = if (File("docs/brand").exists()) File(".") else File("..")
        val storeIcon = File(root, "docs/brand/store-icon-512.png")
        assertTrue("store-icon-512.png must exist at ${storeIcon.path}", storeIcon.exists())

        val (storeW, storeH) = getPngDimensions(storeIcon)
        assertEquals(512, storeW)
        assertEquals(512, storeH)

        val fastlaneIcon = File(root, "fastlane/metadata/android/en-US/images/icon.png")
        assertTrue("fastlane icon.png must exist at ${fastlaneIcon.path}", fastlaneIcon.exists())
        val (fastlaneW, fastlaneH) = getPngDimensions(fastlaneIcon)
        assertEquals(512, fastlaneW)
        assertEquals(512, fastlaneH)
    }

    @Test
    fun lockupSvgsExist() {
        val root = if (File("docs/brand").exists()) File(".") else File("..")
        val svgs = listOf("logo.svg", "lockup-dark.svg", "lockup-light.svg")
        val db = DocumentBuilderFactory.newInstance().newDocumentBuilder()

        for (name in svgs) {
            val file = File(root, "docs/brand/$name")
            assertTrue("$name must exist at ${file.path}", file.exists())
            val doc = db.parse(file)
            assertEquals("svg", doc.documentElement.nodeName.lowercase())
        }
    }

    @Test
    fun interIsCredited() {
        val root = if (File("CREDITS.md").exists()) File(".") else File("..")
        val credits = File(root, "CREDITS.md").readText()
        val dependencies = File(root, "docs/dependencies.md").readText()

        assertTrue("CREDITS.md must mention Inter", credits.contains("Inter"))
        assertTrue("CREDITS.md must mention OFL", credits.contains("OFL"))
        assertTrue("docs/dependencies.md must mention Inter", dependencies.contains("Inter"))
        assertTrue("docs/dependencies.md must mention OFL", dependencies.contains("OFL"))

        val licenseFile = File(root, "app/src/main/assets/licenses/OFL-Inter.txt")
        assertTrue("OFL-Inter.txt must exist", licenseFile.exists())
        assertTrue(licenseFile.readText().contains("SIL OPEN FONT LICENSE Version 1.1"))
    }

    @Test
    fun monochromeLayerStructureAndAlpha() {
        val file = File("src/main/res/drawable/ic_launcher_monochrome.xml").let {
            if (it.exists()) it else File("design/src/main/res/drawable/ic_launcher_monochrome.xml")
        }
        assertTrue("ic_launcher_monochrome.xml must exist", file.exists())

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val paths = doc.getElementsByTagName("path")
        assertTrue("Must have paths in monochrome layer", paths.length >= 5)

        for (i in 0 until paths.length) {
            val elem = paths.item(i) as Element
            val fill = elem.getAttribute("android:fillColor").uppercase()
            assertEquals("All paths in monochrome icon must share the same fill color (#FFFFFFFF)", "#FFFFFFFF", fill)

            val name = elem.getAttribute("android:name")
            if (name.startsWith("line")) {
                assertEquals("Typed lines in monochrome layer must have 0.55 fillAlpha", "0.55", elem.getAttribute("android:fillAlpha"))
            }

            if (name == "carriage_body_with_strip_cutout") {
                assertEquals("Carriage body must use evenOdd fillType to cut out the strip", "evenOdd", elem.getAttribute("android:fillType"))
            }
        }
    }

    @Test
    fun wordmarkVectorIntegrity() {
        val file = File("src/main/res/drawable/enk_wordmark.xml").let {
            if (it.exists()) it else File("design/src/main/res/drawable/enk_wordmark.xml")
        }
        assertTrue("enk_wordmark.xml must exist", file.exists())

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val root = doc.documentElement
        assertEquals("vector", root.nodeName)
        assertEquals("4110", root.getAttribute("android:viewportWidth"))
        assertEquals("1724", root.getAttribute("android:viewportHeight"))

        val paths = doc.getElementsByTagName("path")
        var foundCaret = false
        for (i in 0 until paths.length) {
            val elem = paths.item(i) as Element
            if (elem.getAttribute("android:name") == "caret") {
                foundCaret = true
                val fill = elem.getAttribute("android:fillColor").uppercase()
                assertTrue("Caret fill must be orange (#FF5A1F)", fill == "#FF5A1F" || fill == "#FFFF5A1F")
            }
        }
        assertTrue("enk_wordmark.xml must contain a caret path", foundCaret)
    }
}
