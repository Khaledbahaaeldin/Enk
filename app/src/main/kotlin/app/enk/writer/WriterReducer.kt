package app.enk.writer

import app.enk.settings.CorrectionMode
import app.enk.text.Paginator
import app.enk.text.StruckPage

fun reduce(state: WriterState, event: WriterEvent, correction: CorrectionMode): WriterState {
    val committedState = if (event !is WriterEvent.Insert && state.composingRange != null) {
        commitComposingIfPresent(state)
    } else {
        state
    }

    return when (event) {
        is WriterEvent.ToggleLamp -> committedState.copy(lampOn = !committedState.lampOn)
        is WriterEvent.GoToPage -> {
            val newPageIndex = event.index.coerceIn(0, (committedState.pages.size - 1).coerceAtLeast(0))
            val targetPage = committedState.pages.getOrElse(newPageIndex) { StruckPage("") }
            val newCaret = targetPage.length
            committedState.copy(pageIndex = newPageIndex, caret = newCaret, lastTypedIndex = null)
        }
        is WriterEvent.MoveCaret -> {
            val currentPage = committedState.pages.getOrElse(committedState.pageIndex) { StruckPage("") }
            val newCaret = sanitizeCaret(currentPage.text, event.index.coerceIn(0, currentPage.length))
            committedState.copy(caret = newCaret, lastTypedIndex = null)
        }
        is WriterEvent.Backspace -> reduceBackspace(committedState, correction)
        is WriterEvent.Insert -> reduceInsert(state, event)
        is WriterEvent.Replace -> reduceReplace(committedState, event)
    }
}

private fun commitComposingIfPresent(state: WriterState): WriterState {
    if (state.composingRange == null) return state
    val committedState = state.copy(composingRange = null, lastTypedIndex = null)
    return paginateIfNeeded(
        state = committedState,
        candidateCaret = committedState.caret,
        candidateLastTyped = null,
        composingRange = null
    )
}

private fun sanitizeCaret(text: String, caret: Int): Int {
    if (caret in 1 until text.length &&
        Character.isHighSurrogate(text[caret - 1]) &&
        Character.isLowSurrogate(text[caret])
    ) {
        return caret - 1
    }
    return caret
}

private fun reduceBackspace(state: WriterState, correction: CorrectionMode): WriterState {
    val currentPage = state.pages.getOrElse(state.pageIndex) { StruckPage("") }
    if (state.caret == 0) {
        if (state.pageIndex > 0) {
            val prevPageIndex = state.pageIndex - 1
            val prevPage = state.pages[prevPageIndex]
            val newCaret = prevPage.length
            val newPages = if (currentPage.text.isEmpty()) {
                state.pages.toMutableList().apply { removeAt(state.pageIndex) }
            } else {
                state.pages
            }
            return state.copy(
                pages = newPages,
                pageIndex = prevPageIndex,
                caret = newCaret,
                lastTypedIndex = null
            )
        } else {
            return state.copy(lastTypedIndex = null)
        }
    }

    val text = currentPage.text
    return when (correction) {
        CorrectionMode.CleanErase -> {
            val isSurrogate = state.caret >= 2 &&
                Character.isHighSurrogate(text[state.caret - 2]) &&
                Character.isLowSurrogate(text[state.caret - 1])
            val deleteLen = if (isSurrogate) 2 else 1
            val deleteStart = state.caret - deleteLen
            val deleteEnd = state.caret

            val newText = text.substring(0, deleteStart) + text.substring(deleteEnd)
            val newStruck = mutableListOf<IntRange>()
            for (r in currentPage.struck) {
                if (r.last < deleteStart) {
                    newStruck.add(r)
                } else if (r.first >= deleteEnd) {
                    newStruck.add((r.first - deleteLen)..(r.last - deleteLen))
                } else {
                    val newStart = minOf(r.first, deleteStart)
                    val newLast = maxOf(r.last - deleteLen, deleteStart - 1)
                    if (newLast >= newStart) {
                        newStruck.add(newStart..newLast)
                    }
                }
            }
            val updatedPage = StruckPage(newText, newStruck)
            val newPages = state.pages.toMutableList().apply { set(state.pageIndex, updatedPage) }
            state.copy(
                pages = newPages,
                caret = deleteStart,
                lastTypedIndex = null
            )
        }
        CorrectionMode.StrikeThrough -> {
            fun isStruck(idx: Int, struck: List<IntRange>) = struck.any { idx in it }
            var targetEnd = state.caret
            while (targetEnd > 0 && isStruck(targetEnd - 1, currentPage.struck)) {
                targetEnd--
            }
            if (targetEnd == 0) {
                return state.copy(caret = 0, lastTypedIndex = null)
            }
            val isSurrogate = targetEnd >= 2 &&
                Character.isHighSurrogate(text[targetEnd - 2]) &&
                Character.isLowSurrogate(text[targetEnd - 1])
            val charLen = if (isSurrogate) 2 else 1
            val markStart = targetEnd - charLen
            val markEnd = targetEnd
            var updatedPage = currentPage
            for (i in markStart until markEnd) {
                updatedPage = updatedPage.withStruck(i)
            }
            val newPages = state.pages.toMutableList().apply { set(state.pageIndex, updatedPage) }
            state.copy(
                pages = newPages,
                caret = markStart,
                lastTypedIndex = null
            )
        }
    }
}

/**
 * Moves the struck ranges of a page after [removeLen] characters from [at] were replaced by [insertLen]
 * characters. A range wholly before the edit stays; one after it shifts; one the edit cuts keeps its
 * outside parts.
 */
internal fun shiftStruck(struck: List<IntRange>, at: Int, removeLen: Int, insertLen: Int): List<IntRange> {
    val delta = insertLen - removeLen
    val result = mutableListOf<IntRange>()
    for (r in struck) {
        if (r.last < at) {
            result.add(r)
        } else if (r.first >= at + removeLen) {
            result.add((r.first + delta)..(r.last + delta))
        } else {
            if (r.first < at) {
                result.add(r.first until at)
            }
            if (r.last >= at + removeLen) {
                result.add((at + insertLen)..(r.last + delta))
            }
        }
    }
    return result
}

private fun reduceReplace(state: WriterState, event: WriterEvent.Replace): WriterState {
    val currentPage = state.pages.getOrElse(state.pageIndex) { StruckPage("") }
    val text = currentPage.text
    val start = event.start.coerceIn(0, text.length)
    val end = event.end.coerceIn(start, text.length)
    val newText = text.substring(0, start) + event.text + text.substring(end)
    val newStruck = shiftStruck(currentPage.struck, start, end - start, event.text.length)
    val newPages = state.pages.toMutableList().apply { set(state.pageIndex, StruckPage(newText, newStruck)) }
    val caret = start + event.text.length
    return paginateIfNeeded(
        state = state.copy(pages = newPages, caret = caret, lastTypedIndex = null, composingRange = null),
        candidateCaret = caret,
        candidateLastTyped = null,
        composingRange = null
    )
}

private fun reduceInsert(state: WriterState, event: WriterEvent.Insert): WriterState {
    val currentPage = state.pages.getOrElse(state.pageIndex) { StruckPage("") }
    val (insertAt, replaceLen) = if (state.composingRange != null) {
        Pair(
            state.composingRange.first,
            state.composingRange.last - state.composingRange.first + 1
        )
    } else {
        Pair(state.caret, 0)
    }

    val safeInsertAt = insertAt.coerceIn(0, currentPage.text.length)
    val prefix = currentPage.text.substring(0, safeInsertAt)
    val suffix = currentPage.text.substring((safeInsertAt + replaceLen).coerceAtMost(currentPage.text.length))
    val newText = prefix + event.text + suffix

    val insertedLen = event.text.length

    val newStruck = shiftStruck(currentPage.struck, safeInsertAt, replaceLen, insertedLen)

    val newComposingRange = if (event.composing) {
        safeInsertAt until (safeInsertAt + insertedLen)
    } else {
        null
    }

    val candidateCaret = safeInsertAt + insertedLen
    val isSingleCodePoint = !event.composing &&
        state.composingRange == null &&
        event.text.isNotEmpty() &&
        event.text.codePointCount(0, event.text.length) == 1
    val candidateLastTypedIndex = if (isSingleCodePoint) safeInsertAt else null

    val updatedPage = StruckPage(newText, newStruck)
    val newPages = state.pages.toMutableList().apply { set(state.pageIndex, updatedPage) }
    val unpaginatedState = state.copy(
        pages = newPages,
        caret = candidateCaret,
        lastTypedIndex = candidateLastTypedIndex,
        composingRange = newComposingRange
    )

    if (event.composing) {
        return unpaginatedState
    }

    return paginateIfNeeded(
        state = unpaginatedState,
        candidateCaret = candidateCaret,
        candidateLastTyped = candidateLastTypedIndex,
        composingRange = newComposingRange
    )
}

private fun paginateIfNeeded(
    state: WriterState,
    candidateCaret: Int,
    candidateLastTyped: Int?,
    composingRange: IntRange? = null
): WriterState {
    val currentPage = state.pages.getOrElse(state.pageIndex) { StruckPage("") }
    val newText = currentPage.text
    val newStruck = currentPage.struck

    if (newText.length <= Paginator.PAGE_SIZE) {
        return state.copy(
            caret = candidateCaret,
            lastTypedIndex = candidateLastTyped,
            composingRange = composingRange
        )
    }

    if (newText.length > Paginator.PAGE_SIZE * 2) {
        val splitTexts = Paginator.split(newText)
        var offset = 0
        val splitPages = splitTexts.map { txt ->
            val pageEnd = offset + txt.length
            val pageStruck = mutableListOf<IntRange>()
            for (r in newStruck) {
                val start = maxOf(r.first, offset)
                val end = minOf(r.last, pageEnd - 1)
                if (start <= end) {
                    pageStruck.add((start - offset)..(end - offset))
                }
            }
            offset = pageEnd
            StruckPage(txt, pageStruck)
        }
        val newPages = state.pages.toMutableList().apply {
            removeAt(state.pageIndex)
            addAll(state.pageIndex, splitPages)
        }
        var accumulated = 0
        var targetPageIdx = state.pageIndex
        var targetCaret = candidateCaret
        for ((idx, sp) in splitPages.withIndex()) {
            if (candidateCaret <= accumulated + sp.length || idx == splitPages.lastIndex) {
                targetPageIdx = state.pageIndex + idx
                targetCaret = candidateCaret - accumulated
                break
            }
            accumulated += sp.length
        }
        return state.copy(
            pages = newPages,
            pageIndex = targetPageIdx,
            caret = targetCaret,
            lastTypedIndex = candidateLastTyped,
            composingRange = composingRange
        )
    }

    val breakIdx = Paginator.overflowBreak(newText)
    val splitPoint = if (breakIdx != null && breakIdx < newText.length) breakIdx else Paginator.PAGE_SIZE
    if (splitPoint < newText.length) {
        val p1Text = newText.substring(0, splitPoint)
        val p2Text = newText.substring(splitPoint)
        val p1Struck = mutableListOf<IntRange>()
        val p2Struck = mutableListOf<IntRange>()
        for (r in newStruck) {
            if (r.last < splitPoint) {
                p1Struck.add(r)
            } else if (r.first >= splitPoint) {
                p2Struck.add((r.first - splitPoint)..(r.last - splitPoint))
            } else {
                if (splitPoint > r.first) {
                    p1Struck.add(r.first until splitPoint)
                }
                if (r.last >= splitPoint) {
                    p2Struck.add(0..(r.last - splitPoint))
                }
            }
        }

        val p1 = StruckPage(p1Text, p1Struck)
        val p2 = StruckPage(p2Text, p2Struck)
        val newPages = state.pages.toMutableList().apply {
            set(state.pageIndex, p1)
            add(state.pageIndex + 1, p2)
        }

        val (finalPageIdx, finalCaret, finalLastTyped) = if (candidateCaret >= splitPoint) {
            Triple(
                state.pageIndex + 1,
                candidateCaret - splitPoint,
                if (candidateLastTyped != null) candidateLastTyped - splitPoint else null
            )
        } else {
            Triple(state.pageIndex, candidateCaret, candidateLastTyped)
        }

        return state.copy(
            pages = newPages,
            pageIndex = finalPageIdx,
            caret = finalCaret,
            lastTypedIndex = finalLastTyped,
            composingRange = composingRange
        )
    }

    return state.copy(
        caret = candidateCaret,
        lastTypedIndex = candidateLastTyped,
        composingRange = composingRange
    )
}
