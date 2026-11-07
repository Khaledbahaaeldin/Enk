package app.enk.design

import kotlin.math.floor
import kotlin.math.sin

data class InkJitter(val dyDp: Float, val rotDeg: Float, val alpha: Float)

object Jitter {
    fun hash(n: Int): Double {
        val x = sin(n * 127.1) * 43758.5453
        return x - floor(x)
    }

    fun forIndex(i: Int): InkJitter {
        val r = hash(i + 1)
        val r2 = hash(i + 99)
        val dy = ((r - 0.5) * 1.1).toFloat()
        val rot = ((r2 - 0.5) * 2.2).toFloat()
        val opacity = (0.84 + r * 0.16).toFloat()
        return InkJitter(dyDp = dy, rotDeg = rot, alpha = opacity)
    }
}
