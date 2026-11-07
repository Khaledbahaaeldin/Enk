package tools

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.roundToInt

fun main(args: Array<String>) {
    val tile = 200
    val seed = 4
    val outputFile = if (args.isNotEmpty()) File(args[0]) else File("grain-200.png")

    val img = BufferedImage(tile, tile, BufferedImage.TYPE_INT_ARGB)
    val octaves = 2
    val baseFrequency = 0.9f

    for (y in 0 until tile) {
        for (x in 0 until tile) {
            var totalNoise = 0f
            var maxAmplitude = 0f
            var freq = baseFrequency
            var amp = 1.0f

            for (oct in 0 until octaves) {
                val period = (tile * freq).roundToInt().coerceAtLeast(1)
                val u = x * freq
                val v = y * freq
                val x0 = u.toInt()
                val y0 = v.toInt()
                val fu = u - x0
                val fv = v - y0
                val su = fu * fu * (3f - 2f * fu)
                val sv = fv * fv * (3f - 2f * fv)

                val gx0 = (x0 % period + period) % period
                val gx1 = ((x0 + 1) % period + period) % period
                val gy0 = (y0 % period + period) % period
                val gy1 = ((y0 + 1) % period + period) % period

                val octSeed = seed + oct * 1013
                val v00 = hash(gx0, gy0, octSeed)
                val v10 = hash(gx1, gy0, octSeed)
                val v01 = hash(gx0, gy1, octSeed)
                val v11 = hash(gx1, gy1, octSeed)

                val nx0 = v00 + su * (v10 - v00)
                val nx1 = v01 + su * (v11 - v01)
                val valOct = nx0 + sv * (nx1 - nx0)

                totalNoise += valOct * amp
                maxAmplitude += amp

                freq *= 2f
                amp *= 0.5f
            }

            val noise = (totalNoise / maxAmplitude).coerceIn(0f, 1f)
            val alpha = 0.5f * noise
            val aInt = (alpha * 255f).roundToInt().coerceIn(0, 255)
            val argb = (aInt shl 24) or (0x73 shl 16) or (0x66 shl 8) or 0x50
            img.setRGB(x, y, argb)
        }
    }

    ImageIO.write(img, "PNG", outputFile)
    println("Saved grain tile (${tile}x${tile}) to ${outputFile.absolutePath}")
}

private fun hash(x: Int, y: Int, seed: Int): Float {
    var h = seed xor (x * 374761393) xor (y * 668265263)
    h = (h xor (h ushr 13)) * 1274126177
    h = h xor (h ushr 16)
    return (h and 0x7FFFFFFF).toFloat() / 0x7FFFFFFF.toFloat()
}
