package app.enk.keyboard

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint

fun hardwareKeyToAction(event: KeyEvent, caret: Int, columns: Int): KeyAction? {
    if (event.type != KeyEventType.KeyDown) return null
    if (event.isCtrlPressed || event.isMetaPressed || event.isAltPressed) return null

    return when (event.key) {
        Key.Backspace -> KeyAction.Backspace
        Key.Enter, Key.NumPadEnter -> KeyAction.Enter
        Key.DirectionLeft -> KeyAction.MoveCaretBy(-1, caret)
        Key.DirectionRight -> KeyAction.MoveCaretBy(1, caret)
        Key.DirectionUp -> KeyAction.MoveCaretBy(-columns, caret)
        Key.DirectionDown -> KeyAction.MoveCaretBy(columns, caret)
        Key.Spacebar -> KeyAction.Space
        else -> {
            val cp = event.utf16CodePoint
            if (cp > 0 && !Character.isISOControl(cp) && Character.isValidCodePoint(cp) && !Character.isSurrogate(cp.toChar())) {
                KeyAction.Character(String(Character.toChars(cp)))
            } else {
                null
            }
        }
    }
}
