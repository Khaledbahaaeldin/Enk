package app.enk.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class LayoutLoaderTest {

    private fun loadAssetJson(): String {
        val candidates = listOf(
            File("../app/src/main/assets/keyboard/en.json"),
            File("app/src/main/assets/keyboard/en.json"),
            File("src/main/assets/keyboard/en.json")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("Cannot find en.json in ${candidates.map { it.absolutePath }}")
        return file.readText(Charsets.UTF_8)
    }

    @Test
    fun shippedLayoutParsesSuccessfully() {
        val json = loadAssetJson()
        val layout = LayoutLoader.parse(json)

        assertEquals("en", layout.id)
        assertEquals("English", layout.name)
        assertEquals(3, layout.pages.size)
        assertTrue(layout.pages.containsKey(PageId.Letters))
        assertTrue(layout.pages.containsKey(PageId.Numbers))
        assertTrue(layout.pages.containsKey(PageId.Symbols))

        val lettersRows = layout.pages.getValue(PageId.Letters)
        assertEquals(4, lettersRows.size)
        assertEquals(10, lettersRows[0].keys.size)
        assertEquals(11, lettersRows[1].keys.size)
        assertEquals(9, lettersRows[2].keys.size)
        assertEquals(6, lettersRows[3].keys.size)

        // Every CharKey label is a single code point
        for ((_, rows) in layout.pages) {
            for (row in rows) {
                for (key in row.keys) {
                    if (key is CharKey) {
                        assertEquals(
                            "Label '${key.label}' should be 1 code point",
                            1,
                            key.label.codePointCount(0, key.label.length)
                        )
                        for (alt in key.popup) {
                            assertTrue("Popup alt cannot be empty", alt.isNotEmpty())
                            assertTrue("Popup alt at most 8 chars", alt.length <= 8)
                        }
                    }
                }
            }
        }

        // Key "e" has 5 alternatives
        val keyE = lettersRows[0].keys.filterIsInstance<CharKey>().first { it.label == "e" }
        assertEquals(5, keyE.popup.size)

        // Key "." has 10
        val keyDot = lettersRows[3].keys.filterIsInstance<CharKey>().first { it.label == "." }
        assertEquals(10, keyDot.popup.size)

        // Numbers page row 2 has 10 entries
        val numbersRows = layout.pages.getValue(PageId.Numbers)
        assertEquals(10, numbersRows[1].keys.size)

        // Every row of every page has widths that sum to exactly 10.0 (tolerance 0.001)
        for ((pageId, rows) in layout.pages) {
            for ((rowIndex, row) in rows.withIndex()) {
                val sumWidth = row.keys.sumOf { it.width.toDouble() }
                assertEquals(
                    "Page $pageId row $rowIndex width sum",
                    10.0,
                    sumWidth,
                    0.001
                )
            }
        }
    }

    private fun assertThrowsPath(expectedPath: String, json: String) {
        try {
            LayoutLoader.parse(json)
            fail("Expected LayoutException containing '$expectedPath' but parse succeeded")
        } catch (e: LayoutException) {
            assertTrue(
                "Expected message to contain '$expectedPath', but got: ${e.message}",
                e.message?.contains(expectedPath) == true
            )
        }
    }

    private fun baseValidJson(
        id: String = "\"en\"",
        name: String = "\"English\"",
        popups: String = "{}",
        pages: String = """{
            "letters": [["q", "w", "e", "r", "t", "y", "u", "i", "o", "p"]],
            "numbers": [["1", "2", "3", "4", "5", "6", "7", "8", "9", "0"]],
            "symbols": [["[", "]", "{", "}", "#", "%", "^", "*", "+", "="]]
        }"""
    ): String {
        return """{
            "id": $id,
            "name": $name,
            "popups": $popups,
            "pages": $pages
        }"""
    }

    @Test
    fun failureNotJson() {
        assertThrowsPath("root", "not a json string")
    }

    @Test
    fun failureMissingId() {
        assertThrowsPath("id", """{"name": "En", "pages": {}}""")
    }

    @Test
    fun failureBlankName() {
        assertThrowsPath("name", baseValidJson(name = "\"   \""))
    }

    @Test
    fun failureMissingPage() {
        assertThrowsPath("pages", baseValidJson(pages = """{
            "letters": [["q"]],
            "numbers": [["1"]]
        }"""))
    }

    @Test
    fun failureExtraPage() {
        assertThrowsPath("pages.extra", baseValidJson(pages = """{
            "letters": [["q"]],
            "numbers": [["1"]],
            "symbols": [["-"]],
            "extra": [["x"]]
        }"""))
    }

    @Test
    fun failureEmptyPage() {
        assertThrowsPath("pages.letters", baseValidJson(pages = """{
            "letters": [],
            "numbers": [["1"]],
            "symbols": [["-"]]
        }"""))
    }

    @Test
    fun failureSevenRows() {
        val seven = (1..7).joinToString(",") { "[\"a\"]" }
        assertThrowsPath("pages.letters", baseValidJson(pages = """{
            "letters": [$seven],
            "numbers": [["1"]],
            "symbols": [["-"]]
        }"""))
    }

    @Test
    fun failureRowOf17Entries() {
        val seventeen = (1..17).joinToString(",") { "\"a\"" }
        assertThrowsPath("pages.letters[0]", baseValidJson(pages = """{
            "letters": [[$seventeen]],
            "numbers": [["1"]],
            "symbols": [["-"]]
        }"""))
    }

    @Test
    fun failureUnknownType() {
        assertThrowsPath("pages.letters[0][0].type", baseValidJson(pages = """{
            "letters": [[{"type": "laser"}]],
            "numbers": [["1"]],
            "symbols": [["-"]]
        }"""))
    }

    @Test
    fun failurePageKeyWithoutTarget() {
        assertThrowsPath("pages.letters[0][0].target", baseValidJson(pages = """{
            "letters": [[{"type": "page", "label": "123"}]],
            "numbers": [["1"]],
            "symbols": [["-"]]
        }"""))
    }

    @Test
    fun failurePageKeyWithUnknownTarget() {
        assertThrowsPath("pages.letters[0][0].target", baseValidJson(pages = """{
            "letters": [[{"type": "page", "target": "galaxy", "label": "123"}]],
            "numbers": [["1"]],
            "symbols": [["-"]]
        }"""))
    }

    @Test
    fun failureWidthTooSmall() {
        assertThrowsPath("pages.letters[0][0].width", baseValidJson(pages = """{
            "letters": [[{"type": "char", "label": "a", "width": 0.1}]],
            "numbers": [["1"]],
            "symbols": [["-"]]
        }"""))
    }

    @Test
    fun failureWidthTooLarge() {
        assertThrowsPath("pages.letters[0][0].width", baseValidJson(pages = """{
            "letters": [[{"type": "char", "label": "a", "width": 9.0}]],
            "numbers": [["1"]],
            "symbols": [["-"]]
        }"""))
    }

    @Test
    fun failurePopupsEntryNotArray() {
        assertThrowsPath("popups.a", baseValidJson(popups = """{"a": "not-an-array"}"""))
    }

    @Test
    fun failureAlternativeLongerThan8Chars() {
        assertThrowsPath("popups.a[0]", baseValidJson(popups = """{"a": ["123456789"]}"""))
    }

    @Test
    fun failureEntryIsANumber() {
        assertThrowsPath("pages.letters[0][0]", baseValidJson(pages = """{
            "letters": [[42]],
            "numbers": [["1"]],
            "symbols": [["-"]]
        }"""))
    }

    @Test
    fun failureEmptyStringLabel() {
        assertThrowsPath("pages.letters[0][0]", baseValidJson(pages = """{
            "letters": [[""]],
            "numbers": [["1"]],
            "symbols": [["-"]]
        }"""))
    }

    @Test
    fun failure200kCharLabelString() {
        val huge = "a".repeat(200_000)
        assertThrowsPath("pages.letters[0][0]", baseValidJson(pages = """{
            "letters": [["$huge"]],
            "numbers": [["1"]],
            "symbols": [["-"]]
        }"""))
    }

    @Test
    fun unknownExtraFieldsAreIgnored() {
        val json = """{
            "id": "en",
            "name": "English",
            "customMeta": "foo",
            "popups": {},
            "pages": {
                "letters": [[{"type": "char", "label": "a", "unknownExtraProp": 123}]],
                "numbers": [["1"]],
                "symbols": [["-"]]
            }
        }"""
        val layout = LayoutLoader.parse(json)
        assertNotNull(layout)
        assertEquals("en", layout.id)
    }
}
