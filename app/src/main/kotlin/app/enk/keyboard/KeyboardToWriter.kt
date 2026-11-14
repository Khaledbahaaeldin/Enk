package app.enk.keyboard

import app.enk.audio.SoundEvent
import app.enk.writer.WriterEvent
import app.enk.writer.WriterState
import app.enk.writer.currentPage

data class KeyFeedback(val sound: SoundEvent?, val pulseMs: Int)
data class KeyOutcome(val event: WriterEvent?, val strikes: Boolean, val slugChar: Char?)

object KeyboardToWriter {
    fun feedbackFor(action: KeyAction, state: WriterState): KeyFeedback {
        return when (action) {
            is KeyAction.Character -> KeyFeedback(SoundEvent.Key, 8)
            KeyAction.Space -> KeyFeedback(SoundEvent.Space, 14)
            KeyAction.Enter -> KeyFeedback(SoundEvent.Enter, 14)
            KeyAction.Backspace -> {
                if (state.pageIndex == 0 && state.caret == 0) {
                    KeyFeedback(null, 0)
                } else {
                    KeyFeedback(SoundEvent.Backspace, 8)
                }
            }
            is KeyAction.MoveCaretBy -> KeyFeedback(null, 3)
            KeyAction.SystemKeyboard -> KeyFeedback(null, 8)
        }
    }

    fun outcomeFor(action: KeyAction, state: WriterState): KeyOutcome {
        return when (action) {
            is KeyAction.Character -> {
                if (action.text.isEmpty()) {
                    KeyOutcome(null, strikes = false, slugChar = null)
                } else {
                    val cp = action.text.codePointBefore(action.text.length)
                    val slug = if (cp <= 0xFFFF) cp.toChar() else null
                    KeyOutcome(WriterEvent.Insert(action.text), strikes = true, slugChar = slug)
                }
            }
            KeyAction.Space -> KeyOutcome(WriterEvent.Insert(" "), strikes = true, slugChar = ' ')
            KeyAction.Enter -> KeyOutcome(WriterEvent.Insert("\n"), strikes = true, slugChar = null)
            KeyAction.Backspace -> KeyOutcome(WriterEvent.Backspace, strikes = false, slugChar = null)
            is KeyAction.MoveCaretBy -> {
                val clamped = (action.origin + action.delta).coerceIn(0, state.currentPage.text.length)
                KeyOutcome(WriterEvent.MoveCaret(clamped), strikes = false, slugChar = null)
            }
            KeyAction.SystemKeyboard -> KeyOutcome(null, strikes = false, slugChar = null)
        }
    }

    fun alternativeEvent(base: String, alternative: String, caretBefore: Int, state: WriterState): WriterEvent {
        val text = state.currentPage.text
        return if (state.caret == caretBefore + base.length &&
            caretBefore in 0..text.length &&
            caretBefore + base.length <= text.length &&
            text.regionMatches(caretBefore, base, 0, base.length)
        ) {
            WriterEvent.Replace(caretBefore, caretBefore + base.length, alternative)
        } else {
            WriterEvent.Insert(alternative)
        }
    }
}
