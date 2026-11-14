package app.enk.writer

import app.enk.text.StruckPage

data class WriterState(
    val noteId: String,
    val pages: List<StruckPage>,
    val pageIndex: Int,
    val caret: Int,
    val lampOn: Boolean = false,
    val lastTypedIndex: Int? = null,
    val composingRange: IntRange? = null,
    val missing: Boolean = false,
    val loaded: Boolean = false,
)

sealed interface WriterEvent {
    data class Insert(val text: String, val composing: Boolean = false) : WriterEvent
    object Backspace : WriterEvent
    data class MoveCaret(val index: Int) : WriterEvent

    /**
     * The keyboard replaced characters [start] until [end] of the page with [text] in one go: an
     * autocorrect, a suggestion, a typed character over a selection. Applied as a real replacement
     * (nothing is struck), the caret lands after [text], and it makes no key sound.
     */
    data class Replace(val start: Int, val end: Int, val text: String) : WriterEvent
    data class GoToPage(val index: Int) : WriterEvent
    object ToggleLamp : WriterEvent
}
