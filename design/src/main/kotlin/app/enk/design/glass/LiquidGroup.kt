package app.enk.design.glass

import kotlin.math.roundToInt
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.layout
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.enk.design.effects.GlassLook
import app.enk.design.effects.LocalEffects
import app.enk.design.effects.LocalGlassLook
import app.enk.design.effects.LocalLiquidTuning
import app.enk.design.effects.ResolvedEffects
import app.enk.design.shell.GlassBackendKind
import app.enk.design.shell.GlassSource
import app.enk.design.shell.rememberGlassBackend
import app.enk.design.theme.LocalShellColors
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.runtimeShaderEffect
import com.kyant.backdrop.effects.vibrancy

/**
 * Decides, from the current shapes and press, whether a group's shapes are drawn by the melt
 * shader (merging) or by their own Backdrop surfaces.
 *
 * [merging] is computed on read from the group's current inputs, so every reader (the group's
 * own draw, a sibling surface's graphics layer) sees the same answer in the same frame and the
 * two paths are never both drawn, or both missing, for a frame. The only memory is the
 * "stay merged while pressed" hysteresis, which is idempotent for equal inputs.
 */
class LiquidGroupState {
    internal var shapes: (() -> List<LiquidShape>)? by mutableStateOf(null)
    internal var pressed: (() -> Boolean)? by mutableStateOf(null)
    internal var blend: (() -> Float)? by mutableStateOf(null)
    internal var mergeDistancePx: Float by mutableStateOf(0f)

    private var wasMerging = false

    /** The smooth-union radius in pixels right now (merge distance scaled by the blend). */
    val kPx: Float
        get() = mergeDistancePx * (blend?.invoke() ?: 1f).coerceIn(0f, 1f)

    val merging: Boolean
        get() {
            val list = shapes?.invoke() ?: return false.also { wasMerging = false }
            val isPressed = pressed?.invoke() ?: false
            wasMerging = resolve(wasMerging, list, kPx, isPressed)
            return wasMerging
        }

    /** Kept for callers that drive the state directly (tests). */
    fun update(shapes: List<LiquidShape>, mergeDistancePx: Float, isPressed: Boolean) {
        wasMerging = resolve(wasMerging, shapes, mergeDistancePx, isPressed)
    }

    companion object {
        fun resolve(was: Boolean, shapes: List<LiquidShape>, kPx: Float, isPressed: Boolean): Boolean {
            // No reach left (the bridge has thinned away): the shapes are separate again, even while
            // a spring is still settling; holding the melt then would keep the costly shader on for
            // the whole tail of the animation.
            if (shapes.size < 2 || kPx <= 0f) return false
            val should = LiquidFieldMath.shouldMerge(shapes, kPx)
            return when {
                should -> true
                isPressed -> was
                else -> false
            }
        }
    }
}

private val NO_SHAPES: () -> List<LiquidShape> = { emptyList() }

@Composable
fun rememberLiquidGroupState(): LiquidGroupState = remember { LiquidGroupState() }

/**
 * Draws [shapes] (pixels, in this composable's own space) as one melted liquid surface while
 * they are within the merge distance. The surfaces in the group hide themselves with
 * `alpha = if (state.merging) 0f else 1f` in a graphics layer.
 *
 * [blend] scales the merge distance (and the smooth-union radius) from 0 to 1, so a bridge
 * can be grown and shrunk smoothly instead of popping when shapes sit still within reach.
 */
@Composable
fun LiquidGroup(
    state: LiquidGroupState,
    shapes: () -> List<LiquidShape>,
    spec: LiquidSpec,
    source: GlassSource,
    pressed: () -> Boolean,
    modifier: Modifier = Modifier,
    mergeDistance: Dp = 24.dp,
    blend: () -> Float = { 1f },
    fill: Color? = null
) {
    val density = LocalDensity.current
    val mergeDistancePx = with(density) { mergeDistance.toPx() }
    val liquidTuning = LocalLiquidTuning.current
    val effects = LocalEffects.current
    val colors = LocalShellColors.current
    val resolvedSpec = if (effects == ResolvedEffects.Balanced) LiquidSpecs.balanced(spec) else spec
    val tunedSpec = LiquidSpecs.tuned(resolvedSpec, liquidTuning)
    val tint = fill ?: LiquidSurfaceFill.color(colors.g1, tunedSpec, liquidTuning)
    val backdrop = source.backdrop ?: emptyBackdrop()
    // The melt shader is a RuntimeShader (API 33) and belongs to the Liquid look only: under
    // Frost, the Blur or Solid backends, or Android 12, the group never merges and each
    // surface keeps drawing itself.
    val meltOn = LocalGlassLook.current == GlassLook.Liquid &&
        rememberGlassBackend() == GlassBackendKind.Refraction &&
        Build.VERSION.SDK_INT >= 33
    if (!meltOn) {
        state.shapes = NO_SHAPES
        state.pressed = null
        return
    }

    // Inputs are only read in layer and draw phases, never in composition, so setting them
    // here does not recompose anything; it lets sibling surfaces resolve [merging] themselves.
    state.shapes = shapes
    state.pressed = pressed
    state.blend = blend
    state.mergeDistancePx = mergeDistancePx

    // The melt is drawn over the bounding box of its shapes (plus what the lens and blur reach),
    // not over the whole parent: the shader and the blur cost per pixel, and a small pair of
    // shapes in a big area used to pay for the whole area.
    val marginPx = with(density) { (tunedSpec.lensHeightDp + tunedSpec.blurDp * 2f + 4f).dp.toPx() }
    val origin = remember { IntArray(2) }

    Box(
        modifier = modifier.drawWithContent {
            if (state.merging) drawContent()
        }
    ) {
        Box(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val list = shapes()
                    var l = Float.MAX_VALUE
                    var t = Float.MAX_VALUE
                    var r = -Float.MAX_VALUE
                    var b = -Float.MAX_VALUE
                    for (shape in list) {
                        l = minOf(l, shape.cx - shape.halfW)
                        t = minOf(t, shape.cy - shape.halfH)
                        r = maxOf(r, shape.cx + shape.halfW)
                        b = maxOf(b, shape.cy + shape.halfH)
                    }
                    val m = marginPx + state.kPx
                    if (list.isEmpty()) {
                        l = 0f; t = 0f; r = 0f; b = 0f
                    } else {
                        l -= m; t -= m; r += m; b += m
                    }
                    origin[0] = l.roundToInt()
                    origin[1] = t.roundToInt()
                    val w = (r - l).roundToInt().coerceAtLeast(0)
                    val h = (b - t).roundToInt().coerceAtLeast(0)
                    val placeable = measurable.measure(Constraints.fixed(w, h))
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(origin[0], origin[1])
                    }
                }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RectangleShape },
                    effects = {
                        if (tunedSpec.vibrancy) vibrancy()
                        if (tunedSpec.blurDp > 0f) blur(tunedSpec.blurDp.dp.toPx())
                        runtimeShaderEffect(
                            key = "liquid_field",
                            shaderString = LiquidFieldShader,
                            uniformShaderName = "content"
                        ) {
                            val list = shapes()
                            val count = minOf(list.size, LIQUID_FIELD_MAX_SHAPES)
                            setIntUniform("count", count)
                            setFloatUniform("k", state.kPx)
                            setFloatUniform("refractionHeight", tunedSpec.lensHeightDp.dp.toPx())
                            setFloatUniform("refractionAmount", tunedSpec.lensAmountDp.dp.toPx())
                            setFloatUniform("depthEffect", if (tunedSpec.depth) 1f else 0f)
                            setFloatUniform("chromaticAberration", if (tunedSpec.fringe) 1f else 0f)
                            setFloatUniform("edgeLight", liquidTuning.edgeLight)
                            setFloatUniform("lightAngle", 45f)
                            setFloatUniform("tint", tint.red, tint.green, tint.blue, tint.alpha)

                            val shapesArray = FloatArray(LIQUID_FIELD_MAX_SHAPES * 4)
                            val radiiArray = FloatArray(LIQUID_FIELD_MAX_SHAPES)
                            for (i in 0 until count) {
                                val s = list[i]
                                // The shader works in this box's own pixels.
                                shapesArray[i * 4 + 0] = s.cx - origin[0]
                                shapesArray[i * 4 + 1] = s.cy - origin[1]
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
