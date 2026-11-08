package app.enk.design.logo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser

object LogoSpec {
    // 108 x 108 grid, values from the logo spec
    val typedLines: List<Rect> = listOf(
        Rect(36f, 24f, 36f + 28f, 24f + 2.6f),
        Rect(36f, 29.5f, 36f + 22f, 29.5f + 2.6f),
        Rect(36f, 35f, 36f + 18f, 35f + 2.6f)
    )
    val caret: Rect = Rect(55.5f, 35f, 55.5f + 11f, 35f + 2.1f)
    val rail: Rect = Rect(6f, 56f, 6f + 96f, 56f + 8.5f)
    val carriageCentre: Offset = Offset(61f, 60.25f)
    const val CARRIAGE_SCALE = 1.25f
    const val VISIBLE_DIAMETER = 72f
    const val SAFE_DIAMETER = 66f
}

data class SymbolParts(
    val lines: Boolean = true,
    val caret: Boolean = true,
    val rail: Boolean = true,
    val carriage: Boolean = true
) {
    companion object {
        val All = SymbolParts()
    }
}

@Composable
fun EnkSymbol(modifier: Modifier = Modifier, parts: SymbolParts = SymbolParts.All) {
    Canvas(modifier = modifier.aspectRatio(1f)) {
        val sx = size.width / 108f
        val sy = size.height / 108f

        withTransform({
            scale(sx, sy, Offset.Zero)
        }) {
            if (parts.lines) {
                val lineColor = Color(0xFFF3EEE3).copy(alpha = 0.34f)
                val cornerRadius = CornerRadius(1.3f, 1.3f)
                LogoSpec.typedLines.forEach { line ->
                    drawRoundRect(
                        color = lineColor,
                        topLeft = Offset(line.left, line.top),
                        size = Size(line.width, line.height),
                        cornerRadius = cornerRadius
                    )
                }
            }

            if (parts.caret) {
                drawRoundRect(
                    color = Color(0xFFFF5A1F),
                    topLeft = Offset(LogoSpec.caret.left, LogoSpec.caret.top),
                    size = Size(LogoSpec.caret.width, LogoSpec.caret.height),
                    cornerRadius = CornerRadius(1.0f, 1.0f)
                )
            }

            if (parts.rail) {
                drawRect(
                    color = Color.Black.copy(alpha = 0.42f),
                    topLeft = Offset(6f, 57.5f),
                    size = Size(96f, 4f)
                )

                val railBrush = Brush.verticalGradient(
                    0.0f to Color(0xFFF4F4F2),
                    0.4f to Color(0xFFA9A9A6),
                    0.62f to Color(0xFF6F6F6C),
                    1.0f to Color(0xFFC9C9C6),
                    startY = 56f,
                    endY = 64.5f
                )
                drawRect(
                    brush = railBrush,
                    topLeft = Offset(6f, 56f),
                    size = Size(96f, 8.5f)
                )
            }

            if (parts.carriage) {
                withTransform({
                    translate(LogoSpec.carriageCentre.x, LogoSpec.carriageCentre.y)
                    scale(LogoSpec.CARRIAGE_SCALE, LogoSpec.CARRIAGE_SCALE, Offset.Zero)
                }) {
                    drawOval(
                        color = Color.Black.copy(alpha = 0.55f),
                        topLeft = Offset(-14.5f, 12f),
                        size = Size(29f, 6f)
                    )

                    drawRoundRect(
                        color = Color(0xFF141312),
                        topLeft = Offset(-5.2f, -16.4f),
                        size = Size(10.4f, 7f),
                        cornerRadius = CornerRadius(1.7f, 1.7f)
                    )

                    val slugBrush = Brush.horizontalGradient(
                        0.0f to Color(0xFF9D9D9A),
                        0.5f to Color(0xFFEFEFEC),
                        1.0f to Color(0xFF8A8A87),
                        startX = -3.5f,
                        endX = 3.5f
                    )
                    drawRoundRect(
                        brush = slugBrush,
                        topLeft = Offset(-3.5f, -14.6f),
                        size = Size(7f, 5f),
                        cornerRadius = CornerRadius(1.0f, 1.0f)
                    )

                    val bodyPath = Path().apply {
                        addRoundRect(RoundRect(Rect(-15f, -10f, 15f, 11f), CornerRadius(4.5f, 4.5f)))
                    }
                    clipPath(bodyPath) {
                        val bodyBrush = Brush.horizontalGradient(
                            0.0f to Color(0xFF2A2927),
                            0.2f to Color(0xFF3B3A37),
                            0.8f to Color(0xFF3B3A37),
                            1.0f to Color(0xFF2A2927),
                            startX = -15f,
                            endX = 15f
                        )
                        drawRect(
                            brush = bodyBrush,
                            topLeft = Offset(-15f, -10f),
                            size = Size(30f, 21f)
                        )

                        val topCollarBrush = Brush.verticalGradient(
                            0.0f to Color(0xFFF0F0ED),
                            1.0f to Color(0xFFA9A9A6),
                            startY = -10f,
                            endY = -7f
                        )
                        drawRect(
                            brush = topCollarBrush,
                            topLeft = Offset(-15f, -10f),
                            size = Size(30f, 3f)
                        )

                        val botCollarBrush = Brush.verticalGradient(
                            0.0f to Color(0xFF8D8D8A),
                            1.0f to Color(0xFFD2D2CF),
                            startY = 8f,
                            endY = 11f
                        )
                        drawRect(
                            brush = botCollarBrush,
                            topLeft = Offset(-15f, 8f),
                            size = Size(30f, 3f)
                        )
                    }

                    drawLine(
                        color = Color.White.copy(alpha = 0.17f),
                        start = Offset(-8.4f, 4.1f),
                        end = Offset(8.4f, 4.1f),
                        strokeWidth = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.9f, 1.7f), 0f)
                    )

                    drawRoundRect(
                        color = Color(0xFFFF5A1F),
                        topLeft = Offset(-4.3f, -4.7f),
                        size = Size(8.6f, 1.9f),
                        cornerRadius = CornerRadius(0.95f, 0.95f)
                    )
                }
            }
        }
    }
}

private const val PATH_E = "M586 -24Q426 -24 307.5 46.5Q189 117 123.5 242.0Q58 367 58 527Q58 689 124.5 813.5Q191 938 308.0 1009.0Q425 1080 576 1080Q693 1080 788.0 1040.5Q883 1001 952.0 927.5Q1021 854 1058.5 754.0Q1096 654 1096 533V454H163V642H954L821 597Q821 677 792.0 735.0Q763 793 709.5 824.5Q656 856 582 856Q509 856 456.0 824.5Q403 793 374.0 735.0Q345 677 345 597V469Q345 387 374.5 325.5Q404 264 460.0 230.5Q516 197 595 197Q653 197 697.5 213.5Q742 230 772.5 260.5Q803 291 816 332H1085Q1066 227 996.0 147.0Q926 67 820.0 21.5Q714 -24 586 -24Z"
private const val PATH_N = "M399 587V0H104V1056H394V781H344Q394 921 487.0 999.5Q580 1078 724 1078Q837 1078 920.0 1029.5Q1003 981 1048.5 888.0Q1094 795 1094 661V0H800V607Q800 714 749.5 769.0Q699 824 606 824Q545 824 498.0 799.0Q451 774 425.0 721.5Q399 669 399 587Z"
private const val PATH_K = "M371 304V652H402L772 1056H1118L639 525H571ZM104 0V1490H399V0ZM795 0 461 460 656 665 1140 0Z"

@Composable
fun EnkWordmark(
    modifier: Modifier = Modifier,
    onDark: Boolean = true,
    caretAlpha: Float = 1f
) {
    val textColor = if (onDark) Color(0xFFF3EEE3) else Color(0xFF2B2824)
    val caretColor = Color(0xFFFF5A1F).copy(alpha = caretAlpha)

    val pathE = remember { PathParser().parsePathString(PATH_E).toPath() }
    val pathN = remember { PathParser().parsePathString(PATH_N).toPath() }
    val pathK = remember { PathParser().parsePathString(PATH_K).toPath() }

    Canvas(modifier = modifier.aspectRatio(4110f / 1724f)) {
        val sx = size.width / 4110f
        val sy = size.height / 1724f

        withTransform({
            scale(sx, sy, Offset.Zero)
        }) {
            withTransform({
                translate(0f, 1520f)
                scale(1f, -1f, Offset.Zero)
            }) {
                drawPath(pathE, color = textColor)
                withTransform({ translate(1058f, 0f) }) {
                    drawPath(pathN, color = textColor)
                }
                withTransform({ translate(2165f, 0f) }) {
                    drawPath(pathK, color = textColor)
                }
            }

            drawRoundRect(
                color = caretColor,
                topLeft = Offset(3414f, 1530f),
                size = Size(696f, 174f),
                cornerRadius = CornerRadius(81f, 81f)
            )
        }
    }
}
