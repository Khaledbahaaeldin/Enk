package app.enk.design.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object Contrast {

    fun ratio(fg: Color, bg: Color): Double {
        val effectiveFg = if (fg.alpha < 1f) fg.compositeOver(bg) else fg
        val l1 = relativeLuminance(effectiveFg)
        val l2 = relativeLuminance(bg)
        val maxL = max(l1, l2)
        val minL = min(l1, l2)
        return (maxL + 0.05) / (minL + 0.05)
    }

    fun ensure(fg: Color, bg: Color, min: Double, towardLight: Boolean): Color {
        if (ratio(fg, bg) >= min) return fg
        val (h, s, initialL) = rgbToHsl(fg.red, fg.green, fg.blue)
        var l = initialL
        var current = fg
        while (ratio(current, bg) < min) {
            val nextL = if (towardLight) {
                (l + 0.02f).coerceAtMost(1f)
            } else {
                (l - 0.02f).coerceAtLeast(0f)
            }
            if (nextL == l) {
                break
            }
            l = nextL
            val (r, g, b) = hslToRgb(h, s, l)
            current = Color(red = r, green = g, blue = b, alpha = fg.alpha)
        }
        return current
    }

    private fun relativeLuminance(color: Color): Double {
        fun channel(c: Float): Double {
            val cd = c.toDouble()
            return if (cd <= 0.04045) cd / 12.92 else ((cd + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }

    private fun rgbToHsl(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
        val mx = max(r, max(g, b))
        val mn = min(r, min(g, b))
        val df = mx - mn
        val l = (mx + mn) / 2f
        if (df == 0f) {
            return Triple(0f, 0f, l)
        }
        val s = if (l > 0.5f) df / (2f - mx - mn) else df / (mx + mn)
        val h = when (mx) {
            r -> ((g - b) / df + (if (g < b) 6f else 0f)) * 60f
            g -> ((b - r) / df + 2f) * 60f
            else -> ((r - g) / df + 4f) * 60f
        }
        return Triple(h, s, l)
    }

    private fun hslToRgb(h: Float, s: Float, l: Float): Triple<Float, Float, Float> {
        if (s == 0f) {
            return Triple(l, l, l)
        }
        val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
        val p = 2f * l - q
        fun hue2rgb(tArg: Float): Float {
            var t = tArg
            if (t < 0f) t += 1f
            if (t > 1f) t -= 1f
            if (t < 1f / 6f) return p + (q - p) * 6f * t
            if (t < 1f / 2f) return q
            if (t < 2f / 3f) return p + (q - p) * (2f / 3f - t) * 6f
            return p
        }
        val hNorm = (h % 360f + 360f) % 360f / 360f
        val r = hue2rgb(hNorm + 1f / 3f).coerceIn(0f, 1f)
        val g = hue2rgb(hNorm).coerceIn(0f, 1f)
        val b = hue2rgb(hNorm - 1f / 3f).coerceIn(0f, 1f)
        return Triple(r, g, b)
    }
}
