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
    data class GoToPage(val index: Int) : WriterEvent
    object ToggleLamp : WriterEvent
}
