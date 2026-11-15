package app.enk.design.glass

import app.enk.design.motion.stretch as physicsStretch
import kotlin.math.atan2

object GlassLensMath {
    val NAV_PRESS = 1.08f
    val SEGMENTED_PRESS = 1.08f
    val BEAD_PRESS = 1.16f
    val THUMB_PRESS = 1.14f
    val BUTTON_PRESS = 1.07f

    fun stretch(velocityDpPerS: Float): Pair<Float, Float> {
        if (velocityDpPerS.isNaN() || velocityDpPerS.isInfinite()) {
            return Pair(1f, 1f)
        }
        return physicsStretch(velocityDpPerS)
    }

    fun specularAngleDeg(centreX: Float, centreY: Float, touchX: Float, touchY: Float): Float {
        if (centreX.isNaN() || centreY.isNaN() || touchX.isNaN() || touchY.isNaN()) {
            return 45f
        }
        if (centreX == touchX && centreY == touchY) {
            return 45f
        }
        val rad = atan2((touchY - centreY).toDouble(), (touchX - centreX).toDouble())
        var deg = Math.toDegrees(rad).toFloat()
        while (deg < 0f) deg += 360f
        while (deg >= 360f) deg -= 360f
        return deg
    }

    fun pressTarget(pressed: Boolean, depth: Float): Float {
        return if (pressed) depth else 1f
    }

    fun coerceLens(
        spec: GlassSpec,
        widthDp: Float,
        heightDp: Float,
        minCornerDp: Float
    ): Pair<Float, Float> {
        if (widthDp.isNaN() || heightDp.isNaN() || minCornerDp.isNaN() ||
            widthDp <= 0f || heightDp <= 0f || minCornerDp <= 0f ||
            spec.lensHeightDp <= 0f || spec.lensAmountDp <= 0f
        ) {
            return Pair(0f, 0f)
        }
        val coercedHeight = minOf(spec.lensHeightDp, minCornerDp)
        val coercedAmount = minOf(spec.lensAmountDp, minOf(widthDp, heightDp))
        return Pair(coercedHeight, coercedAmount)
    }
}
