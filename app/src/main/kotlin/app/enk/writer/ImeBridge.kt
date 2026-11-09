package app.enk.writer

import androidx.compose.ui.text.input.TextFieldValue

class ImeBridge {
    private var wasComposing: Boolean = false
    private var lastComposedText: String = ""
    private var lastSelectionStart: Int? = null

    fun onTextChanged(old: String, new: TextFieldValue, composingNow: Boolean): List<WriterEvent> {
        val events = mutableListOf<WriterEvent>()

        val isComposing = composingNow || new.composition != null

        if (isComposing) {
            val compText = if (new.composition != null) {
                val start = minOf(new.composition!!.start, new.composition!!.end).coerceIn(0, new.text.length)
                val end = maxOf(new.composition!!.start, new.composition!!.end).coerceIn(0, new.text.length)
                new.text.substring(start, end)
            } else {
                val prefix = commonPrefix(old, new.text)
                val suffix = commonSuffix(old, new.text, prefix)
                new.text.substring(prefix, new.text.length - suffix)
            }
            val normalized = compText.replace("\r\n", "\n").replace("\r", "\n")
            wasComposing = true
            lastComposedText = normalized
            lastSelectionStart = new.selection.start
            return listOf(WriterEvent.Insert(text = normalized, composing = true))
        }

        if (wasComposing) {
            wasComposing = false
            val committedText = if (lastComposedText.isNotEmpty()) {
                val t = lastComposedText
                lastComposedText = ""
                t
            } else {
                val prefix = commonPrefix(old, new.text)
                val suffix = commonSuffix(old, new.text, prefix)
                new.text.substring(prefix, new.text.length - suffix).replace("\r\n", "\n").replace("\r", "\n")
            }
            lastSelectionStart = new.selection.start
            return listOf(WriterEvent.Insert(text = committedText, composing = false))
        }

        if (new.text == old) {
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

        // Text grew
        if (new.text.length > old.length) {
            val insertLen = new.text.length - old.length
            val selEnd = new.selection.start
            val selStart = selEnd - insertLen

            val canUseSelection = selStart in 0..old.length &&
                old.regionMatches(0, new.text, 0, selStart) &&
                old.regionMatches(selStart, new.text, selEnd, old.length - selStart)

            val (insertAt, insertedRaw) = if (canUseSelection) {
                Pair(selStart, new.text.substring(selStart, selEnd))
            } else {
                val prefix = commonPrefix(old, new.text)
                val suffix = commonSuffix(old, new.text, prefix)
                Pair(prefix, new.text.substring(prefix, new.text.length - suffix))
            }

            if (prevCaret != insertAt) {
                events.add(WriterEvent.MoveCaret(insertAt))
            }

            val inserted = insertedRaw.replace("\r\n", "\n").replace("\r", "\n")
            events.add(WriterEvent.Insert(text = inserted, composing = false))

            lastSelectionStart = new.selection.start
            return events
        }

        // Text shrank
        val deleteLen = old.length - new.text.length
        val isBackspaceAtCaret = new.selection.start == prevCaret - deleteLen &&
            prevCaret in deleteLen..old.length &&
            old.regionMatches(0, new.text, 0, new.selection.start) &&
            old.regionMatches(prevCaret, new.text, new.selection.start, old.length - prevCaret)

        if (isBackspaceAtCaret) {
            val deletedCodePoints = old.codePointCount(new.selection.start, prevCaret)
            repeat(deletedCodePoints) {
                events.add(WriterEvent.Backspace)
            }
        } else {
            val prefix = commonPrefix(old, new.text)
            val suffix = commonSuffix(old, new.text, prefix)
            val deletedCodePoints = old.codePointCount(prefix, old.length - suffix)
            val caretAtDeletion = old.length - suffix

            val isAtCaret = prevCaret in prefix..caretAtDeletion
            if (!isAtCaret) {
                events.add(WriterEvent.MoveCaret(caretAtDeletion))
            }

            repeat(deletedCodePoints) {
                events.add(WriterEvent.Backspace)
            }

            if (!isAtCaret && new.selection.start != prefix) {
                events.add(WriterEvent.MoveCaret(new.selection.start))
            }
        }

        lastSelectionStart = new.selection.start
        return events
    }

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
        if (suffix > 0 && aSuffixStart < a.length &&
            Character.isHighSurrogate(a[aSuffixStart - 1]) &&
            Character.isLowSurrogate(a[aSuffixStart])
        ) {
            suffix--
        }
        return suffix
    }
}
