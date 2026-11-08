package app.enk.writer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.enk.design.EnkColors
import app.enk.design.EnkMaterials
import app.enk.design.EnkShadows
import app.enk.design.LampColor
import app.enk.design.enkShadow
import app.enk.design.paperGrain
import app.enk.text.GridLayout
import app.enk.text.StruckPage

@Composable
fun PaperSheet(
    night: Boolean,
    reduceEffects: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val paperColor = if (night) EnkColors.paperNight else EnkColors.paper
    val chassisBrush = if (night) EnkMaterials.chassisNight else EnkMaterials.chassisDay
    val grainOpacity = if (night) 0.10f else 0.35f
    val paperShape = RoundedCornerShape(
        topStart = 10.dp,
        topEnd = 10.dp,
        bottomEnd = 26.dp,
        bottomStart = 26.dp
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(chassisBrush)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, top = 64.dp, end = 12.dp, bottom = 12.dp)
                .enkShadow(EnkShadows.paper, paperShape)
                .clip(paperShape)
                .background(paperColor)
                .then(
                    if (!reduceEffects) Modifier.paperGrain(grainOpacity) else Modifier
                )
        ) {
            content()

            // Text fade-in mask: top 70 dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(paperColor, Color.Transparent)
                        )
                    )
            )

            // Top curl shade: top 46 dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.22f), Color.Transparent)
                        )
                    )
            )
        }
    }
}

@Composable
fun PaperSheet(
    state: WriterState,
    geometry: WriterGeometry,
    night: Boolean,
    reduceEffects: Boolean,
    modifier: Modifier = Modifier
) {
    val page = state.pages.getOrElse(state.pageIndex) { StruckPage("") }
    val textColor = if (night) {
        val rgb = LampColor.fromWarmth(25)
        Color(red = rgb.r, green = rgb.g, blue = rgb.b)
    } else {
        EnkColors.ink
    }

    val lines = remember(page.text, geometry.cols) {
        GridLayout.wrap(page.text, geometry.cols)
    }
    val caretPos = remember(lines, state.caret, geometry.cols) {
        GridLayout.caret(lines, state.caret, geometry.cols)
    }

    PaperSheet(
        night = night,
        reduceEffects = reduceEffects,
        modifier = modifier
    ) {
        PaperText(
            page = page,
            caret = state.caret,
            lastTypedIndex = state.lastTypedIndex,
            geometry = geometry,
            textColor = textColor,
            reduceMotion = reduceEffects,
            modifier = Modifier.fillMaxSize()
        )
        Caret(
            geometry = geometry,
            pos = caretPos,
            typing = false,
            reduceMotion = reduceEffects
        )
    }
}
