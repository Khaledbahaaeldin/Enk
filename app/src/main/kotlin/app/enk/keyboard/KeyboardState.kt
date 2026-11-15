package app.enk.keyboard

import java.util.Locale

enum class Shift { Off, Once, Caps }

data class KeyboardState(
    val page: PageId = PageId.Letters,
    val shift: Shift = Shift.Off,
    val lastShiftTapMs: Long = Long.MIN_VALUE
) {
    fun tapShift(nowMs: Long): KeyboardState {
        if (page != PageId.Letters) return this
        return when (shift) {
            Shift.Caps -> copy(shift = Shift.Off, lastShiftTapMs = nowMs)
            Shift.Once -> {
                if (nowMs - lastShiftTapMs <= DOUBLE_TAP_MS) {
                    copy(shift = Shift.Caps, lastShiftTapMs = nowMs)
                } else {
                    copy(shift = Shift.Off, lastShiftTapMs = nowMs)
                }
            }
            Shift.Off -> copy(shift = Shift.Once, lastShiftTapMs = nowMs)
        }
    }

    fun afterCharacter(): KeyboardState {
        return if (shift == Shift.Once) {
            copy(shift = Shift.Off)
        } else {
            this
        }
    }

    fun toPage(target: PageId): KeyboardState {
        return copy(page = target, shift = Shift.Off)
    }

    companion object {
        const val DOUBLE_TAP_MS = 350L
    }
}

/** What a character key outputs. Uppercases only when shift is on and the page is Letters. */
fun outputFor(label: String, state: KeyboardState): String {
    return if (state.page == PageId.Letters && state.shift != Shift.Off) {
        label.uppercase(Locale.ROOT)
    } else {
        label
    }
}

/** Uppercases a long-press alternative with the same rule, but keeps it unchanged if uppercasing changes its length in code points ("ß" must stay "ß"). */
fun alternativeFor(alt: String, state: KeyboardState): String {
    if (state.page != PageId.Letters || state.shift == Shift.Off) {
        return alt
    }
    val upper = alt.uppercase(Locale.ROOT)
    val altCodePoints = if (alt.isEmpty()) 0 else alt.codePointCount(0, alt.length)
    val upperCodePoints = if (upper.isEmpty()) 0 else upper.codePointCount(0, upper.length)
    return if (altCodePoints == upperCodePoints) {
        upper
    } else {
        alt
    }
}
