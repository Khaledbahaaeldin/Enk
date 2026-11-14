package app.enk.keyboard

import app.enk.settings.CorrectionMode
import app.enk.text.StruckPage
import app.enk.writer.WriterState
import app.enk.writer.currentPage
import app.enk.writer.reduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class KeyboardPropertyTest {

    @Test
    fun propertyTestRandomActionsMatchModel() {
        val random = Random(42)
        var state = WriterState(
            noteId = "test-prop",
            pages = listOf(StruckPage("")),
            pageIndex = 0,
            caret = 0,
            loaded = true
        )

        val sb = StringBuilder()
        var modelCaret = 0

        val letters = ('a'..'z').map { it.toString() }

        repeat(2000) { step ->
            val action = when (random.nextInt(5)) {
                0 -> {
                    val ch = letters[random.nextInt(letters.size)]
                    KeyAction.Character(ch)
                }
                1 -> KeyAction.Space
                2 -> KeyAction.Enter
                3 -> KeyAction.Backspace
                else -> {
                    val delta = random.nextInt(21) - 10 // -10..10
                    KeyAction.MoveCaretBy(delta, state.caret)
                }
            }

            // Update model
            when (action) {
                is KeyAction.Character -> {
                    sb.insert(modelCaret, action.text)
                    modelCaret += action.text.length
                }
                KeyAction.Space -> {
                    sb.insert(modelCaret, " ")
                    modelCaret += 1
                }
                KeyAction.Enter -> {
                    sb.insert(modelCaret, "\n")
                    modelCaret += 1
                }
                KeyAction.Backspace -> {
                    if (modelCaret > 0) {
                        sb.deleteCharAt(modelCaret - 1)
                        modelCaret -= 1
                    }
                }
                is KeyAction.MoveCaretBy -> {
                    modelCaret = (action.origin + action.delta).coerceIn(0, sb.length)
                }
                KeyAction.SystemKeyboard -> {}
            }

            // Apply through outcomeFor and reduce
            val outcome = KeyboardToWriter.outcomeFor(action, state)
            if (outcome.event != null) {
                state = reduce(state, outcome.event, CorrectionMode.CleanErase)
            }

            // Invariants
            val currentText = state.currentPage.text
            assertTrue("Caret must be within 0..text.length at step $step", state.caret in 0..currentText.length)
            assertEquals("Caret must match model at step $step", modelCaret, state.caret)
            assertEquals("Text must match model at step $step", sb.toString(), currentText)
        }
    }
}
