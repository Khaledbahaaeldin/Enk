package app.enk.writer

import androidx.compose.ui.text.input.TextFieldValue

/**
 * Turns what a keyboard did to the hidden text field into writer events, from the text alone.
 *
 * The keyboard's composing region is deliberately not used. A keyboard may mark a word it already
 * committed as composing again (SwiftKey does it on every space and Enter) or correct a committed word
 * (autocorrect); read as "this text was typed", that inserts the word a second time, and the
 * rewrite of the field that follows makes the keyboard do it again. Only a change of the text is an edit.
 */
class ImeBridge {
    private var lastSelectionStart: Int? = null

    /** [composingNow] is accepted for the callers and ignored on purpose, see the class comment. */
    @Suppress("UNUSED_PARAMETER")
    fun onTextChanged(old: String, new: TextFieldValue, composingNow: Boolean): List<WriterEvent> {
        val newText = new.text

        if (newText == old) {
            // Same text: at most the cursor moved. A changed composing range is not an edit.
            val prevCaret = lastSelectionStart
            lastSelectionStart = new.selection.start
            if (prevCaret != null) {
                if (new.selection.start != prevCaret) {
                    return listOf(WriterEvent.MoveCaret(new.selection.start))
                }
            } else {
                // Initial event on non-empty text with non-zero caret position
                if (new.selection.start != 0 && old.isNotEmpty()) {
                    return listOf(WriterEvent.MoveCaret(new.selection.start))
                }
            }
            return emptyList()
        }

        val prevCaret = lastSelectionStart ?: old.length
        val prefix = commonPrefix(old, newText)
        val suffix = commonSuffix(old, newText, prefix)
        val removedEnd = old.length - suffix
        val insertedEnd = newText.length - suffix
        val removedAny = removedEnd > prefix
        val insertedAny = insertedEnd > prefix

        // Characters were replaced by others: an autocorrect, a suggestion, a typed character over a selection.
        if (removedAny && insertedAny) {
            lastSelectionStart = new.selection.start
            val replaced = newText.substring(prefix, insertedEnd).normalizeNewlines()
            val replaceEvents = mutableListOf<WriterEvent>(WriterEvent.Replace(prefix, removedEnd, replaced))
            // The writer's caret lands after the new text; follow the keyboard if it put its cursor elsewhere.
            if (replaced.length == newText.length - suffix - prefix && new.selection.start != prefix + replaced.length) {
                replaceEvents.add(WriterEvent.MoveCaret(new.selection.start))
            }
            return replaceEvents
        }

        val events = mutableListOf<WriterEvent>()

        if (newText.length > old.length) {
            // Text grew. The selection tells where, which matters when the same character repeats.
            val insertLen = newText.length - old.length
            val selEnd = new.selection.start
            val selStart = selEnd - insertLen

            val canUseSelection = selStart in 0..old.length &&
                old.regionMatches(0, newText, 0, selStart) &&
                old.regionMatches(selStart, newText, selEnd, old.length - selStart)

            val (insertAt, insertedRaw) = if (canUseSelection) {
                Pair(selStart, newText.substring(selStart, selEnd))
            } else {
                Pair(prefix, newText.substring(prefix, newText.length - suffix))
            }

            if (prevCaret != insertAt) {
                events.add(WriterEvent.MoveCaret(insertAt))
            }
            val inserted = insertedRaw.normalizeNewlines()
            events.add(WriterEvent.Insert(text = inserted, composing = false))
            // Follow the keyboard's cursor when it is not right after the inserted text (a double space
            // that becomes ". " puts it one further on).
            if (inserted.length == insertedRaw.length && new.selection.start != insertAt + inserted.length) {
                events.add(WriterEvent.MoveCaret(new.selection.start))
            }
            lastSelectionStart = new.selection.start
            return events
        }

        // Text shrank.
        val deleteLen = old.length - newText.length
        val isBackspaceAtCaret = new.selection.start == prevCaret - deleteLen &&
            prevCaret in deleteLen..old.length &&
            old.regionMatches(0, newText, 0, new.selection.start) &&
            old.regionMatches(prevCaret, newText, new.selection.start, old.length - prevCaret)

        if (isBackspaceAtCaret) {
            val deletedCodePoints = old.codePointCount(new.selection.start, prevCaret)
            repeat(deletedCodePoints) { events.add(WriterEvent.Backspace) }
        } else {
            val deletedCodePoints = old.codePointCount(prefix, removedEnd)
            val caretAtDeletion = removedEnd

            val isAtCaret = prevCaret in prefix..caretAtDeletion
            if (!isAtCaret) {
                events.add(WriterEvent.MoveCaret(caretAtDeletion))
            }
            repeat(deletedCodePoints) { events.add(WriterEvent.Backspace) }
            if (!isAtCaret && new.selection.start != prefix) {
                events.add(WriterEvent.MoveCaret(new.selection.start))
            }
        }

        lastSelectionStart = new.selection.start
        return events
    }

    private fun String.normalizeNewlines(): String = replace("\r\n", "\n").replace("\r", "\n")

    private fun commonPrefix(a: String, b: String): Int {
        var prefix = 0
        val max = minOf(a.length, b.length)
        while (prefix < max && a[prefix] == b[prefix]) {
            prefix++
        }
        if (prefix > 0 && prefix < a.length &&
            Character.isHighSurrogate(a[prefix - 1]) &&
            Character.isLowSurrogate(a[prefix])
        ) {
            prefix--
        }
        return prefix
    }

    private fun commonSuffix(a: String, b: String, prefix: Int): Int {
        var suffix = 0
        val maxSuffix = minOf(a.length - prefix, b.length - prefix)
        while (suffix < maxSuffix && a[a.length - 1 - suffix] == b[b.length - 1 - suffix]) {
            suffix++
        }
        val aSuffixStart = a.length - suffix
        if (suffix > 0 && aSuffixStart > 0 && aSuffixStart < a.length &&
            Character.isHighSurrogate(a[aSuffixStart - 1]) &&
            Character.isLowSurrogate(a[aSuffixStart])
        ) {
            suffix--
        }
        return suffix
    }
}
