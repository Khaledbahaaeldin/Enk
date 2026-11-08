package tools

import app.enk.design.Grain
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

object GenerateGrain {
    fun generateTileImage(tile: Int = Grain.TILE, seed: Int = Grain.SEED): BufferedImage {
        val pixels = Grain.generatePixels(tile, seed)
        val img = BufferedImage(tile, tile, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until tile) {
            for (x in 0 until tile) {
                img.setRGB(x, y, pixels[y * tile + x])
            }
        }
        return img
    }

    fun writeTileToFile(outputFile: File, tile: Int = Grain.TILE, seed: Int = Grain.SEED) {
        val img = generateTileImage(tile, seed)
        ImageIO.write(img, "PNG", outputFile)
    }
}

fun main(args: Array<String>) {
    val outputFile = if (args.isNotEmpty()) File(args[0]) else File("grain-200.png")
    GenerateGrain.writeTileToFile(outputFile)
    println("Saved grain tile (${Grain.TILE}x${Grain.TILE}) to ${outputFile.absolutePath}")
}
