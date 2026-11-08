package app.enk.design

import kotlin.math.floor

data class Rgb(val r: Int, val g: Int, val b: Int)

object LampColor {
    fun fromWarmth(warmth: Int): Rgb {
        val w = warmth / 100.0
        val g = floor(236.0 + (150.0 - 236.0) * w + 0.5).toInt()
        val b = floor(214.0 + (70.0 - 214.0) * w + 0.5).toInt()
        return Rgb(255, g, b)
    }
}

object LampMath {
    fun intensity(slider: Int): Float = slider / 100f
    fun beamWidthDp(slider: Int): Float = 70f + slider * 2.6f
}
