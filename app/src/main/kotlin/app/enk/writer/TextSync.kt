package app.enk.writer

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Brings the hidden IME field in line with the writer state without clobbering input in flight.
 *
 * The field is only replaced when its text really differs from the page (a page break, a capped
 * insert, a state that loaded late). A selection that differs is only moved when the IME is not
 * composing, so a half-typed word is never reset.
 */
internal fun syncField(field: TextFieldValue, state: WriterState): TextFieldValue {
    if (!state.loaded || state.missing) return field
    val pageText = state.currentPage.text
    val caret = state.caret.coerceIn(0, pageText.length)
    return when {
        field.text != pageText -> TextFieldValue(text = pageText, selection = TextRange(caret))
        field.composition == null && field.selection != TextRange(caret) ->
            field.copy(selection = TextRange(caret))
        else -> field
    }
}
