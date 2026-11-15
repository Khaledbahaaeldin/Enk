package app.enk.design.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.enk.design.effects.LocalEffects
import app.enk.design.effects.LocalLiquidTuning
import app.enk.design.effects.ResolvedEffects
import app.enk.design.shell.GlassSource
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.runtimeShaderEffect

class LiquidGroupState {
    var merging: Boolean by mutableStateOf(false)
        internal set

    fun update(shapes: List<LiquidShape>, mergeDistancePx: Float, isPressed: Boolean) {
        val should = LiquidFieldMath.shouldMerge(shapes, mergeDistancePx)
        if (should) {
            merging = true
        } else if (!isPressed) {
            merging = false
        }
    }
}

@Composable
fun rememberLiquidGroupState(): LiquidGroupState = remember { LiquidGroupState() }

@Composable
fun LiquidGroup(
    state: LiquidGroupState,
    shapes: () -> List<LiquidShape>,
    spec: LiquidSpec,
    source: GlassSource,
    pressed: () -> Boolean,
    modifier: Modifier = Modifier,
    mergeDistance: Dp = 24.dp
) {
    val density = LocalDensity.current
    val mergeDistancePx = with(density) { mergeDistance.toPx() }
    val liquidTuning = LocalLiquidTuning.current
    val effects = LocalEffects.current
    val resolvedSpec = if (effects == ResolvedEffects.Balanced) LiquidSpecs.balanced(spec) else spec
    val tunedSpec = LiquidSpecs.tuned(resolvedSpec, liquidTuning)
    val backdrop = source.backdrop ?: emptyBackdrop()

    // Update state during composition
    val currentShapes = shapes()
    val isPressed = pressed()
    state.update(currentShapes, mergeDistancePx, isPressed)

    Box(
        modifier = modifier.drawWithContent {
            val list = shapes()
            state.update(list, mergeDistancePx, pressed())
            if (state.merging) {
                drawContent()
            }
        }
    ) {
        if (state.merging) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RectangleShape },
                        effects = {
                            runtimeShaderEffect(
                                key = "liquid_field",
                                shaderString = LiquidFieldShader,
                                uniformShaderName = "content"
                            ) {
                                val list = shapes()
                                val count = minOf(list.size, LIQUID_FIELD_MAX_SHAPES)
                                setIntUniform("count", count)
                                setFloatUniform("k", mergeDistancePx)
                                setFloatUniform("refractionHeight", tunedSpec.lensHeightDp.dp.toPx())
                                setFloatUniform("refractionAmount", tunedSpec.lensAmountDp.dp.toPx())
                                setFloatUniform("depthEffect", if (tunedSpec.depth) 1f else 0f)
                                setFloatUniform("chromaticAberration", if (tunedSpec.fringe) 1f else 0f)
                                setFloatUniform("edgeLight", liquidTuning.edgeLight)
                                setFloatUniform("lightAngle", 45f)
                                setFloatUniform("tint", 0f, 0f, 0f, liquidTuning.tint)

                                val shapesArray = FloatArray(LIQUID_FIELD_MAX_SHAPES * 4)
                                val radiiArray = FloatArray(LIQUID_FIELD_MAX_SHAPES)
                                for (i in 0 until count) {
                                    val s = list[i]
                                    shapesArray[i * 4 + 0] = s.cx
                                    shapesArray[i * 4 + 1] = s.cy
                                    shapesArray[i * 4 + 2] = s.halfW
                                    shapesArray[i * 4 + 3] = s.halfH
                                    radiiArray[i] = s.radius
                                }
                                setFloatUniform("shapes", shapesArray)
                                setFloatUniform("radii", radiiArray)
                            }
                        },
                        highlight = null,
                        shadow = null,
                        innerShadow = null
                    )
            )
        }
    }
}
