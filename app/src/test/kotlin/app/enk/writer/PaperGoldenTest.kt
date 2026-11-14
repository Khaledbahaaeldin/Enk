package app.enk.writer

import app.enk.EnkSnapshots
import androidx.compose.runtime.CompositionLocalProvider
import app.enk.design.LocalMonoFont
import app.enk.text.StruckPage
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PaperGoldenTest {

    @get:Rule
    val snapshots = EnkSnapshots(PaperGoldenTest::class.java)

    // The goldens load the real Courier Prime file directly for consistent font rasterization
    // across host environments.
    private val courier: FontFamily get() = FontFamily(Font(File("../design/src/main/res/font/courier_prime_regular.ttf")))

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
        snapshots.snapshot {
            CompositionLocalProvider(LocalMonoFont provides courier) {
                PaperSheet(
                state = state,
                geometry = referenceGeometry,
                nightProgress = 0f,
                reduceEffects = false,
            )
            }}
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
        snapshots.snapshot {
            CompositionLocalProvider(LocalMonoFont provides courier) {
                PaperSheet(
                state = state,
                geometry = referenceGeometry,
                nightProgress = 1f,
                reduceEffects = false,
            )
            }}
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
        snapshots.snapshot {
            CompositionLocalProvider(LocalMonoFont provides courier) {
                PaperSheet(
                state = state,
                geometry = referenceGeometry,
                nightProgress = 0f,
                reduceEffects = false,
            )
            }}
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
        snapshots.snapshot {
            CompositionLocalProvider(LocalMonoFont provides courier) {
                PaperSheet(
                state = state,
                geometry = referenceGeometry,
                nightProgress = 0f,
                reduceEffects = false,
            )
            }}
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
        snapshots.snapshot {
            CompositionLocalProvider(LocalMonoFont provides courier) {
                PaperSheet(
                state = state,
                geometry = referenceGeometry,
                nightProgress = 0f,
                reduceEffects = false,
            )
            }}
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
        snapshots.snapshot {
            CompositionLocalProvider(LocalMonoFont provides courier) {
                PaperSheet(
                state = state,
                geometry = scaledGeometry,
                nightProgress = 0f,
                reduceEffects = false,
            )
            }}
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
        snapshots.snapshot {
            CompositionLocalProvider(LocalMonoFont provides courier) {
                PaperSheet(
                state = state,
                geometry = referenceGeometry,
                nightProgress = 0f,
                reduceEffects = false,
            )
            }}
    }

    @Test
    fun struckCharactersInStrikeThroughMode() {
        val state = WriterState(
            noteId = "test-note-8",
            pages = listOf(StruckPage("Hello world", listOf(2..4, 8..8))),
            pageIndex = 0,
            caret = 11,
            loaded = true
        )
        snapshots.snapshot {
            CompositionLocalProvider(LocalMonoFont provides courier) {
                PaperSheet(
                state = state,
                geometry = referenceGeometry,
                nightProgress = 0f,
                reduceEffects = false,
            )
            }}
    }

    @Test
    fun stampedCharacterAt80ms() {
        val state = WriterState(
            noteId = "test-note-9",
            pages = listOf(StruckPage("Stamp")),
            pageIndex = 0,
            caret = 5,
            lastTypedIndex = 4,
            loaded = true
        )
        snapshots.snapshot {
            CompositionLocalProvider(LocalMonoFont provides courier) {
                PaperSheet(
                state = state,
                geometry = referenceGeometry,
                nightProgress = 0f,
                reduceEffects = false,
                stampFrame = 80f / 160f
            )
            }}
    }
}
