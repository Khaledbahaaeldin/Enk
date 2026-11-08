package app.enk.design

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object EnkMaterials {
    val railStopsForTest: List<Pair<Float, Color>> = listOf(
        0f to Color(0xFFF4F4F2),
        0.40f to Color(0xFFA9A9A6),
        0.62f to Color(0xFF6F6F6C),
        1f to Color(0xFFC9C9C6)
    )

    val railSteel: Brush = Brush.verticalGradient(
        colorStops = railStopsForTest.toTypedArray()
    )

    val bracketSteel: Brush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF8B8B88),
            Color(0xFFD8D8D5),
            Color(0xFF7A7A77)
        )
    )

    val collarTop: Brush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF0F0ED),
            Color(0xFFA9A9A6)
        )
    )

    val collarBottom: Brush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF8D8D8A),
            Color(0xFFD2D2CF)
        )
    )

    val graphiteCap: Brush = Brush.horizontalGradient(
        0f to Color(0xFF2A2927),
        0.20f to Color(0xFF3B3A37),
        0.80f to Color(0xFF3B3A37),
        1f to Color(0xFF2A2927)
    )

    val graphiteKnob: Brush = Brush.horizontalGradient(
        0f to Color(0xFF2A2927),
        0.25f to Color(0xFF3B3A37),
        0.75f to Color(0xFF3B3A37),
        1f to Color(0xFF2A2927)
    )

    val slugSteel: Brush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF9D9D9A),
            Color(0xFFEFEFEC),
            Color(0xFF8A8A87)
        )
    )

    val panelFace: Brush = Brush.verticalGradient(
        colors = listOf(
            EnkColors.panelTop,
            EnkColors.panelBottom
        )
    )

    val chassisDay: Brush = Brush.verticalGradient(
        colors = listOf(
            EnkColors.chassisTop,
            EnkColors.chassisBottom
        )
    )

    val chassisNight: Brush = Brush.verticalGradient(
        colors = listOf(
            EnkColors.chassisNightTop,
            EnkColors.chassisNightBottom
        )
    )
}
