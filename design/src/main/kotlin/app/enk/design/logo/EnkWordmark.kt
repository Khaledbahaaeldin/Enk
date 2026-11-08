package app.enk.design.logo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import app.enk.design.EnkColors

// Inter Bold outlines (SIL OFL 1.1, see app/src/main/assets/licenses/OFL-Inter.txt), font units, y up.
private const val PATH_E = "M586 -24Q426 -24 307.5 46.5Q189 117 123.5 242.0Q58 367 58 527Q58 689 124.5 813.5Q191 938 308.0 1009.0Q425 1080 576 1080Q693 1080 788.0 1040.5Q883 1001 952.0 927.5Q1021 854 1058.5 754.0Q1096 654 1096 533V454H163V642H954L821 597Q821 677 792.0 735.0Q763 793 709.5 824.5Q656 856 582 856Q509 856 456.0 824.5Q403 793 374.0 735.0Q345 677 345 597V469Q345 387 374.5 325.5Q404 264 460.0 230.5Q516 197 595 197Q653 197 697.5 213.5Q742 230 772.5 260.5Q803 291 816 332H1085Q1066 227 996.0 147.0Q926 67 820.0 21.5Q714 -24 586 -24Z"
private const val PATH_N = "M399 587V0H104V1056H394V781H344Q394 921 487.0 999.5Q580 1078 724 1078Q837 1078 920.0 1029.5Q1003 981 1048.5 888.0Q1094 795 1094 661V0H800V607Q800 714 749.5 769.0Q699 824 606 824Q545 824 498.0 799.0Q451 774 425.0 721.5Q399 669 399 587Z"
private const val PATH_K = "M371 304V652H402L772 1056H1118L639 525H571ZM104 0V1490H399V0ZM795 0 461 460 656 665 1140 0Z"

@Composable
fun EnkWordmark(
    modifier: Modifier = Modifier,
    onDark: Boolean = true,
    caretAlpha: Float = 1f
) {
    val textColor = if (onDark) LogoSpec.CREAM else EnkColors.ink
    val caretColor = EnkColors.accent.copy(alpha = caretAlpha)

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
