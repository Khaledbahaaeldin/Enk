package app.enk.keyboard

sealed interface KeyAction {
    data class Character(val text: String) : KeyAction
    data object Backspace : KeyAction
    data object Enter : KeyAction
    data object Space : KeyAction
    data class MoveCaretBy(val delta: Int, val origin: Int) : KeyAction   // target = origin + delta, clamped
    data object SystemKeyboard : KeyAction
}
