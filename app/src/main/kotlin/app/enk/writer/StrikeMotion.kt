package app.enk.writer

import app.enk.text.GridLayout
import kotlin.math.PI
import kotlin.math.sin

object StrikeTiming {
    const val IMPACT_MS = 26f            // key-down to impact, normal strike
    const val FAST_IMPACT_MS = 18f       // when the previous strike was < FAST_GAP_MS ago
    const val SLUG_TRAVEL_DP = 17f
    const val RETURN_MS = 84f
    const val IMPRESSION_MS = 70f
    const val DARKEN_MS = 60f
    const val DARKEN_AMOUNT = 0.22f
    const val HALO_MS = 45f
    const val HALO_BASE_DP = 2.2f
    const val FLASH_MS = 70f
    const val FLASH_ALPHA = 0.35f
    const val FLASH_RADIUS_START_DP = 9f
    const val FLASH_RADIUS_END_DP = 15f
    const val FLASH_RY_DP = 3f
    const val FAST_GAP_MS = 95f
    const val MAX_ANIMATING = 8
    const val THUMP_RESPONSE_S = 0.09f
    const val THUMP_DAMPING = 0.3f
    const val THUMP_IMPULSE_DPS = 55f
    const val TICK_RESPONSE_S = 0.075f
    const val TICK_DAMPING = 0.5f
    const val TICK_SOFT_RESPONSE_S = 0.09f
    const val TICK_SOFT_DAMPING = 0.6f
    const val RETURN_RESPONSE_S = 0.24f
    const val RETURN_DAMPING = 0.72f
    const val WRAP_RETURN_RESPONSE_S = 0.20f   // 20% quicker than the Enter return
    const val FEED_RESPONSE_S = 0.22f
    const val ERASE_WIPE_MS = 90f
    const val STRUCK_LINE_MS = 40f
    /** Time after the last strike after which nothing is animating any more. */
    const val TOTAL_MS = 130f
}

data class StrikeFrame(
    val slugRiseDp: Float,      // 0 = at rest in the slot, SLUG_TRAVEL_DP = touching the paper
    val glyphVisible: Boolean,  // false before impact
    val glyphScale: Float,
    val glyphDarken: Float,     // 0..DARKEN_AMOUNT, blend toward black
    val haloDp: Float,
    val flashAlpha: Float,
    val flashRadiusDp: Float
) {
    companion object {
        val Rest: StrikeFrame = StrikeFrame(
            slugRiseDp = 0f,
            glyphVisible = true,
            glyphScale = 1f,
            glyphDarken = 0f,
            haloDp = 0f,
            flashAlpha = 0f,
            flashRadiusDp = StrikeTiming.FLASH_RADIUS_START_DP
        )
    }
}

fun strikeWeight(codePoint: Int): Float {
    if (Character.isUpperCase(codePoint)) return 1.4f
    when (codePoint) {
        '@'.code, '#'.code, '%'.code, '&'.code -> return 1.4f
        '.'.code, ','.code, '\''.code, '`'.code, ':'.code, ';'.code, '-'.code -> return 0.5f
    }
    return 1.0f
}

private fun clamp01(x: Float): Float = x.coerceIn(0f, 1f)
private fun easeOut(x: Float): Float {
    val inv = 1f - x
    return 1f - inv * inv * inv
}
private fun easeIn(x: Float): Float = x * x * x

fun stiffnessFor(responseS: Float): Float {
    val w0 = (2f * PI.toFloat()) / responseS
    return w0 * w0
}

fun strikeFrame(msSinceKey: Float, weight: Float, fast: Boolean): StrikeFrame {
    if (msSinceKey.isNaN() || msSinceKey.isInfinite()) {
        return StrikeFrame.Rest
    }
    if (msSinceKey < 0f) {
        return StrikeFrame.Rest.copy(glyphVisible = false)
    }
    val w = if (weight.isNaN() || weight.isInfinite() || weight <= 0f) 1.0f else weight
    val impact = if (fast) StrikeTiming.FAST_IMPACT_MS else StrikeTiming.IMPACT_MS

    val slugRiseDp: Float = if (msSinceKey <= impact) {
        StrikeTiming.SLUG_TRAVEL_DP * easeIn(clamp01(msSinceKey / impact))
    } else if (msSinceKey < impact + StrikeTiming.RETURN_MS) {
        val back = easeOut(clamp01((msSinceKey - impact) / StrikeTiming.RETURN_MS))
        val rise = StrikeTiming.SLUG_TRAVEL_DP * (1f - back - 0.06f * sin(back * PI.toFloat()))
        rise.coerceAtLeast(0f)
    } else {
        0f
    }

    val glyphVisible = msSinceKey >= impact
    val ia = msSinceKey - impact

    return if (!glyphVisible) {
        StrikeFrame(
            slugRiseDp = slugRiseDp,
            glyphVisible = false,
            glyphScale = 1f,
            glyphDarken = 0f,
            haloDp = 0f,
            flashAlpha = 0f,
            flashRadiusDp = StrikeTiming.FLASH_RADIUS_START_DP
        )
    } else {
        val glyphScale = 1f + 0.10f * w * (1f - easeOut(clamp01(ia / StrikeTiming.IMPRESSION_MS)))
        val glyphDarken = if (ia < StrikeTiming.DARKEN_MS) StrikeTiming.DARKEN_AMOUNT else 0f
        val fastHaloMultiplier = if (fast) 0.3f else 1.0f
        val haloDp = StrikeTiming.HALO_BASE_DP * w * fastHaloMultiplier * (1f - clamp01(ia / StrikeTiming.HALO_MS))
        val flashAlpha = if (ia < StrikeTiming.FLASH_MS) {
            StrikeTiming.FLASH_ALPHA * (1f - ia / StrikeTiming.FLASH_MS)
        } else {
            0f
        }
        val flashRadiusDp = StrikeTiming.FLASH_RADIUS_START_DP +
            (StrikeTiming.FLASH_RADIUS_END_DP - StrikeTiming.FLASH_RADIUS_START_DP) * clamp01(ia / StrikeTiming.FLASH_MS)

        StrikeFrame(
            slugRiseDp = slugRiseDp,
            glyphVisible = true,
            glyphScale = glyphScale,
            glyphDarken = glyphDarken,
            haloDp = haloDp,
            flashAlpha = flashAlpha,
            flashRadiusDp = flashRadiusDp
        )
    }
}

/** What the carriage should do after an edit. Rest = snap (no animation). */
fun carriageModeFor(event: WriterEvent, before: WriterState, after: WriterState, cols: Int): CarriageMotion {
    // 1. after is not loaded or before.pageIndex != after.pageIndex -> Rest.
    if (!after.loaded || before.pageIndex != after.pageIndex) return CarriageMotion.Rest

    // 2. Insert whose text is exactly "\n" -> Return.
    if (event is WriterEvent.Insert && event.text == "\n") return CarriageMotion.Return

    // 3. Insert of exactly one code point:
    if (event is WriterEvent.Insert && event.text.isNotEmpty() && event.text.codePointCount(0, event.text.length) == 1) {
        val lineBefore = GridLayout.caret(GridLayout.wrap(before.currentPage.text, cols), before.caret, cols).line
        val lineAfter = GridLayout.caret(GridLayout.wrap(after.currentPage.text, cols), after.caret, cols).line
        return if (lineAfter > lineBefore) {
            CarriageMotion.WrapReturn
        } else if (event.text == " ") {
            CarriageMotion.TickSoft
        } else {
            CarriageMotion.Tick
        }
    }

    // 4. Backspace:
    if (event is WriterEvent.Backspace) {
        val lineBefore = GridLayout.caret(GridLayout.wrap(before.currentPage.text, cols), before.caret, cols).line
        val lineAfter = GridLayout.caret(GridLayout.wrap(after.currentPage.text, cols), after.caret, cols).line
        return if (lineAfter < lineBefore) {
            CarriageMotion.Return
        } else {
            CarriageMotion.Tick
        }
    }

    // 5. Every other event (MoveCaret, Replace, multi-code-point Insert, GoToPage, ToggleLamp) -> Rest.
    return CarriageMotion.Rest
}
