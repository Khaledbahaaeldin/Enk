package app.enk.writer

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.enk.text.StruckPage
import org.junit.Rule
import org.junit.Test

class PaperGoldenTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.NEXUS_5.copy(
            screenWidth = 1200,
            screenHeight = 2460
        ),
        maxPercentDifference = 2.0
    )

    private val referenceGeometry = WriterGeometry.compute(
        widthDp = 400f,
        heightDp = 820f,
        fontScale = 1.0f
    )

    @Test
    fun dayPaperWithOneLineTypedCaretCol12() {
        val state = WriterState(
            noteId = "test-note-1",
            pages = listOf(StruckPage("Hello Enk typewriter")),
            pageIndex = 0,
            caret = 12,
            loaded = true
        )
        paparazzi.snapshot {
            PaperSheet(
                state = state,
                geometry = referenceGeometry,
                night = false,
                reduceEffects = false
            )
        }
    }

    @Test
    fun nightPaperWithOneLineTypedCaretCol12() {
        val state = WriterState(
            noteId = "test-note-2",
            pages = listOf(StruckPage("Hello Enk typewriter")),
            pageIndex = 0,
            caret = 12,
            loaded = true
        )
        paparazzi.snapshot {
            PaperSheet(
                state = state,
                geometry = referenceGeometry,
                night = true,
                reduceEffects = false
            )
        }
    }

    @Test
    fun wrappedLineAt28Cols() {
        val state = WriterState(
            noteId = "test-note-3",
            pages = listOf(StruckPage("The quick brown fox jumps over the lazy dog")),
            pageIndex = 0,
            caret = 43,
            loaded = true
        )
        paparazzi.snapshot {
            PaperSheet(
                state = state,
                geometry = referenceGeometry,
                night = false,
                reduceEffects = false
            )
        }
    }

    @Test
    fun caretAtCol28() {
        val fullLine = "A".repeat(28)
        val state = WriterState(
            noteId = "test-note-4",
            pages = listOf(StruckPage(fullLine)),
            pageIndex = 0,
            caret = 28,
            loaded = true
        )
        paparazzi.snapshot {
            PaperSheet(
                state = state,
                geometry = referenceGeometry,
                night = false,
                reduceEffects = false
            )
        }
    }

    @Test
    fun emptyPageOnlyCaret() {
        val state = WriterState(
            noteId = "test-note-5",
            pages = listOf(StruckPage("")),
            pageIndex = 0,
            caret = 0,
            loaded = true
        )
        paparazzi.snapshot {
            PaperSheet(
                state = state,
                geometry = referenceGeometry,
                night = false,
                reduceEffects = false
            )
        }
    }

    @Test
    fun fontScale13_22Cols() {
        val scaledGeometry = WriterGeometry.compute(
            widthDp = 400f,
            heightDp = 820f,
            fontScale = 1.3f
        )
        val state = WriterState(
            noteId = "test-note-6",
            pages = listOf(StruckPage("Font scale 1.3 test line")),
            pageIndex = 0,
            caret = 24,
            loaded = true
        )
        paparazzi.snapshot {
            PaperSheet(
                state = state,
                geometry = scaledGeometry,
                night = false,
                reduceEffects = false
            )
        }
    }

    @Test
    fun emojiPage() {
        val state = WriterState(
            noteId = "test-note-7",
            pages = listOf(StruckPage("Enk 😀 Notes")),
            pageIndex = 0,
            caret = 11,
            loaded = true
        )
        paparazzi.snapshot {
            PaperSheet(
                state = state,
                geometry = referenceGeometry,
                night = false,
                reduceEffects = false
            )
        }
    }
}
