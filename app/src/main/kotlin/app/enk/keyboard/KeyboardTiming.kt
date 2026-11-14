package app.enk.keyboard

object KeyboardTiming {
    const val LONG_PRESS_MS = 350L
    const val BACKSPACE_FIRST_REPEAT_MS = 400L

    /** Delay before repeat number n (n = 1 is the first repeat after the initial 400 ms): 90 ms for n in 1..10, 45 ms after. */
    fun backspaceRepeatDelayMs(n: Int): Long {
        val effectiveN = if (n <= 0) 1 else n
        return if (effectiveN in 1..10) 90L else 45L
    }

    const val CURSOR_DRAG_SLOP_DP = 10f
    const val CURSOR_DRAG_PITCH_DP = 14f
    const val POPUP_SLOT_WIDTH_DP = 44f
}

fun cursorColumns(dxDp: Float): Int {
    if (dxDp.isNaN() || dxDp.isInfinite()) return 0
    return (dxDp / KeyboardTiming.CURSOR_DRAG_PITCH_DP).toInt()
}

/** Index of the popup slot under the finger, or null when outside. [fingerX] and [popupLeft] are in the same space; slots are POPUP_SLOT_WIDTH_DP wide, left to right. */
fun popupIndexAt(fingerX: Float, popupLeft: Float, count: Int): Int? {
    if (count <= 0 || fingerX.isNaN() || popupLeft.isNaN()) return null
    val dx = fingerX - popupLeft
    if (dx < 0f || dx >= count * KeyboardTiming.POPUP_SLOT_WIDTH_DP) return null
    val idx = (dx / KeyboardTiming.POPUP_SLOT_WIDTH_DP).toInt()
    return if (idx in 0 until count) idx else null
}

data class KeyboardMetrics(
    val keyHeightDp: Float,
    val gapXDp: Float,
    val gapYDp: Float,
    val sidePaddingDp: Float,
    val topPaddingDp: Float,
    val bottomPaddingDp: Float,
    val letterSp: Float,
    val functionSp: Float
) {
    fun totalHeightDp(rows: Int): Float {
        return if (rows <= 0) {
            topPaddingDp + bottomPaddingDp
        } else {
            topPaddingDp + rows * keyHeightDp + (rows - 1) * gapYDp + bottomPaddingDp
        }
    }

    companion object {
        fun forWindow(heightDp: Float): KeyboardMetrics {
            return when {
                heightDp.isNaN() || heightDp.isInfinite() || heightDp <= 0f -> FULL
                heightDp >= 640f -> FULL
                heightDp >= 480f -> MEDIUM
                else -> COMPACT
            }
        }

        private val FULL = KeyboardMetrics(
            keyHeightDp = 46f,
            gapXDp = 6f,
            gapYDp = 10f,
            sidePaddingDp = 4f,
            topPaddingDp = 10f,
            bottomPaddingDp = 8f, // 10 + 4*46 + 3*10 + 8 = 232.0f as required by K1.8 and K2.7
            letterSp = 22f,
            functionSp = 16f
        )

        private val MEDIUM = KeyboardMetrics(
            keyHeightDp = 40f,
            gapXDp = 5f,
            gapYDp = 8f,
            sidePaddingDp = 3f,
            topPaddingDp = 8f,
            bottomPaddingDp = 4f,
            letterSp = 20f,
            functionSp = 15f
        )

        private val COMPACT = KeyboardMetrics(
            keyHeightDp = 32f,
            gapXDp = 4f,
            gapYDp = 5f,
            sidePaddingDp = 3f,
            topPaddingDp = 5f,
            bottomPaddingDp = 3f,
            letterSp = 17f,
            functionSp = 13f
        )
    }
}
