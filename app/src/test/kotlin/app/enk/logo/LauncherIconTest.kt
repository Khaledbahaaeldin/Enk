package app.enk.logo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class LauncherIconTest {

    @Test
    fun adaptiveIconDeclaresAllLayers() {
        val resDir = File("src/main/res/mipmap-anydpi-v26").let {
            if (it.exists()) it else File("app/src/main/res/mipmap-anydpi-v26")
        }

        val iconFiles = listOf(
            File(resDir, "ic_launcher.xml"),
            File(resDir, "ic_launcher_round.xml")
        )

        val db = DocumentBuilderFactory.newInstance().newDocumentBuilder()

        for (file in iconFiles) {
            assertTrue("Adaptive icon file must exist: ${file.path}", file.exists())
            val doc = db.parse(file)
            val root = doc.documentElement
            assertEquals("adaptive-icon", root.nodeName)

            val backgrounds = root.getElementsByTagName("background")
            val foregrounds = root.getElementsByTagName("foreground")
            val monochromes = root.getElementsByTagName("monochrome")

            assertTrue("${file.name} must declare <background>", backgrounds.length >= 1)
            assertTrue("${file.name} must declare <foreground>", foregrounds.length >= 1)
            assertTrue("${file.name} must declare <monochrome>", monochromes.length >= 1)

            val bgElem = backgrounds.item(0) as Element
            val fgElem = foregrounds.item(0) as Element
            val monoElem = monochromes.item(0) as Element

            assertEquals("@drawable/ic_launcher_background", bgElem.getAttribute("android:drawable"))
            assertEquals("@drawable/ic_launcher_foreground", fgElem.getAttribute("android:drawable"))
            assertEquals("@drawable/ic_launcher_monochrome", monoElem.getAttribute("android:drawable"))
        }
    }
}
