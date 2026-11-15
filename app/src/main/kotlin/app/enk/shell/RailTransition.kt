package app.enk.shell

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.enk.design.effects.LocalEffects
import app.enk.design.effects.ResolvedEffects
import app.enk.design.motion.ShellMotion
import app.enk.design.theme.LocalShellColors
import app.enk.design.theme.LocalShellMaterial
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

object RailTransitionMath {
    fun seg(p: Float, a: Float, b: Float): Float =
        if (b == a) (if (p >= a) 1f else 0f) else ((p - a) / (b - a)).coerceIn(0f, 1f)

    fun ez(t: Float): Float = t * t * (3f - 2f * t)

    fun out(t: Float): Float = 1f - (1f - t) * (1f - t) * (1f - t)

    fun back(t: Float): Float = 1f + 2.9f * (t - 1f) * (t - 1f) * (t - 1f) + 1.9f * (t - 1f) * (t - 1f)

    fun boxScale(p: Float): Float = 1f - 0.04f * ez(seg(p, 0.05f, 0.70f))
    fun boxTranslationY(p: Float): Float = -8f * ez(seg(p, 0.05f, 0.70f))
    fun boxBlurDp(p: Float): Float = 8f * ez(seg(p, 0.05f, 0.70f))
    fun boxBrightness(p: Float): Float = 1f - 0.65f * ez(seg(p, 0.05f, 0.70f))
    fun boxAlpha(p: Float): Float = 1f - seg(p, 0.72f, 0.92f)

    fun navLeft(p: Float, startLeft: Float = 12f): Float = startLeft * (1f - ez(seg(p, 0.05f, 0.60f)))
    fun navWidth(p: Float, startW: Float = 336f, endW: Float = 360f): Float =
        startW + (endW - startW) * ez(seg(p, 0.05f, 0.60f))
    fun navHeight(p: Float, startH: Float = 64f, endH: Float = 16f): Float =
        startH + (endH - startH) * ez(seg(p, 0.05f, 0.60f))
    fun navTop(p: Float, startTop: Float, railY: Float): Float =
        startTop + ((railY - 8f) - startTop) * ez(seg(p, 0.05f, 0.60f))
    fun navRadius(p: Float, startR: Float = 32f, endR: Float = 8f): Float =
        startR + (endR - startR) * ez(seg(p, 0.05f, 0.60f))
    fun navContentAlpha(p: Float): Float = 1f - seg(p, 0f, 0.16f)

    /** The nav glass fades out where the writer's steel rail fades in. */
    fun steelRailAlpha(p: Float): Float = seg(p, 0.50f, 0.62f)

    fun paperClipU(p: Float): Float = out(seg(p, 0.40f, 0.92f))
    fun keyboardTranslationY(p: Float): Float = 240f * (1f - out(seg(p, 0.35f, 0.88f)))

    fun carriageTranslationY(p: Float): Float = -80f * (1f - back(seg(p, 0.72f, 0.96f)))
    fun carriageAlpha(p: Float): Float = seg(p, 0.72f, 0.80f)

    fun islandScale(p: Float): Float = 0.55f + 0.45f * back(seg(p, 0.80f, 1f))
    fun islandAlpha(p: Float): Float = seg(p, 0.80f, 1f)
    fun caretAlpha(p: Float): Float = seg(p, 0.94f, 1f)

    /** Progress at which the carriage lands and rings. */
    const val DING_AT = 0.90f

    /** Progress from which the island reacts to touch. */
    const val ISLAND_INTERACTIVE_AT = 0.95f
}

@Stable
class RailTransitionState(
    initialP: Float = 1f
) {
    val progress = Animatable(initialP)
    var landed by mutableStateOf(initialP >= RailTransitionMath.DING_AT)
        internal set
    private var dingFired = initialP >= RailTransitionMath.DING_AT

    suspend fun animateTo(
        target: Float,
        reduceEffects: Boolean = false,
        onDing: () -> Unit = {}
    ) {
        val forward = target > progress.value
        if (!forward) {
            dingFired = false
            landed = false
        }
        val spec: AnimationSpec<Float> = if (reduceEffects) {
            tween(150)
        } else {
            ShellMotion.railPage.toSpec()
        }

        progress.animateTo(target, spec) {
            if (forward && !dingFired && value >= RailTransitionMath.DING_AT) {
                dingFired = true
                landed = true
                onDing()
            }
        }
        if (target >= RailTransitionMath.DING_AT) {
            landed = true
        } else if (target <= 0.05f) {
            landed = false
            dingFired = false
        }
    }
}

enum class RailKind { None, Forward, Reverse }

/** Write from a tab runs the transition forward; the writer's own Box button runs it back. */
fun railKind(from: Route, to: Route): RailKind {
    fun isTab(r: Route) = r == Route.Box || r == Route.Find || r == Route.Setup
    return when {
        isTab(from) && to == Route.Writer -> RailKind.Forward
        from == Route.Writer && to == Route.Box -> RailKind.Reverse
        else -> RailKind.None
    }
}

/**
 * The Rail transition as the screens see it. One progress value drives every beat; outside a flight a
 * screen sits at its rest value (the writer at 1, the tab screens at 0), so a route change that is not
 * a Rail transition never shows a half-played one.
 *
 * `flight` and the gate fields are plain fields set during composition (they are idempotent and must be
 * right on the first frame); the draw-phase readers re-run on every frame because they read the
 * animated progress, and on `tick`, which moves whenever a gate flips.
 */
@Stable
class RailTransitionHost(initialP: Float) {
    val state = RailTransitionState(initialP)

    var flight: Boolean = false
        private set
    private var forward = true
    private var dinged = initialP >= RailTransitionMath.DING_AT

    /** Moves when a gate flips, so composition-phase and draw-phase readers re-run. */
    var tick by mutableIntStateOf(0)
        private set

    /** Counts dings; the writer rings, buzzes and pulses on a change. */
    var dingToken by mutableIntStateOf(0)
        private set

    /** The nav bar's rectangle in window pixels, kept by the nav bar while it is on screen. */
    var navBounds: Rect? = null

    /** The rail's centre line and the writer area's top, in window pixels, kept by the writer. */
    var railCenterYPx: Float? = null

    /** The writer's chassis colours, so the backdrop behind the fading Box matches the writer's own. */
    var chassisTop: Color = Color.Unspecified
    var chassisBottom: Color = Color.Unspecified

    /**
     * Called during composition when the route changes. A Rail pair starts a flight; any other change ends
     * the previous one. A flight is not ended when its animation finishes: the exiting screen is still
     * composed for a moment, and at its rest value it would flash back over the new one.
     */
    fun begin(kind: RailKind) {
        if (kind == RailKind.None) {
            flight = false
            return
        }
        flight = true
        forward = kind == RailKind.Forward
        if (forward) dinged = false
    }

    private fun raw(): Float {
        tick
        return state.progress.value
    }

    /** The writer's beats: 1 at rest, the animated value in a flight. */
    fun writerP(): Float {
        val p = raw()
        return if (flight) p else 1f
    }

    /** The tab screens' beats: 0 at rest, the animated value in a flight. */
    fun boxP(): Float {
        val p = raw()
        return if (flight) p else 0f
    }

    /** The animated value while the bar is between the nav bar and the rail (0 and 1 are the two rests). */
    fun flightP(): Float? {
        val p = raw()
        return if (flight && p > 0f && p < 1f) p else null
    }

    /** False between the start of a forward flight and the ding: the loaded text is not drawn yet. */
    fun typeOnGate(): Boolean {
        tick
        return !(flight && forward && !dinged)
    }

    fun islandInteractive(): Boolean {
        tick
        return !flight || state.progress.value >= RailTransitionMath.ISLAND_INTERACTIVE_AT
    }

    /** Plays to [target] (1 forward, 0 back) from wherever the progress is now, keeping the flight state. */
    suspend fun run(target: Float, reduceEffects: Boolean) {
        coroutineScope {
            val watcher = launch {
                snapshotFlow { state.progress.value >= RailTransitionMath.ISLAND_INTERACTIVE_AT }
                    .distinctUntilChanged()
                    .collect { tick++ }
            }
            state.animateTo(target, reduceEffects) {
                dinged = true
                dingToken++
                tick++
            }
            watcher.cancel()
        }
        tick++
    }

    /** A route change that is not a Rail transition: sit at the screen's rest value at once. */
    suspend fun settle(writerShown: Boolean) {
        flight = false
        state.progress.snapTo(if (writerShown) 1f else 0f)
        dinged = writerShown
        tick++
    }
}

val LocalRailTransition = staticCompositionLocalOf<RailTransitionHost?> { null }

/**
 * The tab screens' side of the transition (beats 1 and 7): the screen recedes, dims, blurs and fades,
 * all in the draw phase. The nav bar is not part of it; it has its own morph.
 */
@Composable
fun Modifier.railRecede(): Modifier {
    val host = LocalRailTransition.current ?: return this
    val effects = LocalEffects.current
    val blur = effects != ResolvedEffects.Light
    return this
        .graphicsLayer {
            val p = host.boxP()
            if (p <= 0f) return@graphicsLayer
            val s = RailTransitionMath.boxScale(p)
            scaleX = s
            scaleY = s
            translationY = RailTransitionMath.boxTranslationY(p) * density
            alpha = RailTransitionMath.boxAlpha(p)
            val b = RailTransitionMath.boxBlurDp(p) * density
            if (blur && b > 0.5f) {
                renderEffect = BlurEffect(b, b, TileMode.Decal)
            }
        }
        .drawWithContent {
            drawContent()
            val p = host.boxP()
            val dim = 1f - RailTransitionMath.boxBrightness(p)
            if (p > 0f && blur && dim > 0f) {
                drawRect(Color.Black.copy(alpha = dim))
            }
        }
}

/** The tab screens' bar is a glass pill at rest and a steel rail at the end; this is the glass half. */
@Composable
fun RailMorphOverlay(host: RailTransitionHost, modifier: Modifier = Modifier) {
    val colors = LocalShellColors.current
    val material = LocalShellMaterial.current
    Canvas(modifier = modifier.fillMaxSize()) {
        val p = host.flightP() ?: return@Canvas
        val nav = host.navBounds ?: return@Canvas
        val railY = host.railCenterYPx ?: return@Canvas
        val m = RailTransitionMath.ez(RailTransitionMath.seg(p, 0.05f, 0.60f))
        val left = nav.left * (1f - m)
        val width = nav.width + (size.width - nav.width) * m
        val h = 16.dp.toPx()
        val height = nav.height + (h - nav.height) * m
        val top = nav.top + ((railY - 8.dp.toPx()) - nav.top) * m
        val radius = 32.dp.toPx() + (8.dp.toPx() - 32.dp.toPx()) * m
        val alpha = 1f - RailTransitionMath.steelRailAlpha(p)
        if (alpha <= 0f) return@Canvas
        val rr = RoundRect(Rect(left, top, left + width, top + height), CornerRadius(minOf(radius, height / 2f)))
        val path = Path().apply { addRoundRect(rr) }
        drawPath(path, colors.g1.copy(alpha = colors.g1.alpha * alpha))
        drawPath(path, material.glassLine.copy(alpha = material.glassLine.alpha * alpha), style = Stroke(width = 1.dp.toPx()))
    }
}

/** A band around the rail that grows into the whole paper. */
class RailBandShape(
    private val top: Float,
    private val bottom: Float,
    private val topRadiusPx: Float,
    private val bottomRadiusPx: Float
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rounded(
            RoundRect(
                left = 0f,
                top = top,
                right = size.width,
                bottom = maxOf(top, bottom),
                topLeftCornerRadius = CornerRadius(topRadiusPx),
                topRightCornerRadius = CornerRadius(topRadiusPx),
                bottomRightCornerRadius = CornerRadius(bottomRadiusPx),
                bottomLeftCornerRadius = CornerRadius(bottomRadiusPx)
            )
        )
}

/** Beat 4: the paper is fed out of the rail slot. [paperTopDp] and [railYDp] come from the writer's geometry. */
fun Modifier.railPaper(host: RailTransitionHost?, paperTopDp: Float, railYDp: Float): Modifier {
    if (host == null) return this
    return graphicsLayer {
        val p = host.writerP()
        if (p >= 1f) {
            clip = false
            return@graphicsLayer
        }
        val u = RailTransitionMath.paperClipU(p)
        val paperTop = paperTopDp * density
        val lr = railYDp * density - paperTop
        val paperHeight = size.height - paperTop
        val top = paperTop + lr * (1f - u)
        val bottom = size.height - (paperHeight - lr) * (1f - u)
        clip = true
        shape = RailBandShape(top, bottom, 10f * density, 26f * density)
    }
}

/** Beat 4 (keyboard): rises with the paper. */
fun Modifier.railKeyboard(host: RailTransitionHost?): Modifier {
    if (host == null) return this
    return graphicsLayer {
        translationY = RailTransitionMath.keyboardTranslationY(host.writerP()) * density
    }
}

/** Beat 5: the carriage drops onto the rail. */
fun Modifier.railCarriage(host: RailTransitionHost?): Modifier {
    if (host == null) return this
    return graphicsLayer {
        val p = host.writerP()
        translationY = RailTransitionMath.carriageTranslationY(p) * density
        alpha = RailTransitionMath.carriageAlpha(p)
    }
}

/** Beat 3: the steel rail takes over from the glass. */
fun Modifier.railSteel(host: RailTransitionHost?): Modifier {
    if (host == null) return this
    return graphicsLayer { alpha = RailTransitionMath.steelRailAlpha(host.writerP()) }
}

/** Beat 6: the island arrives, scaling up from its top centre. */
fun Modifier.railIsland(host: RailTransitionHost?): Modifier {
    if (host == null) return this
    return graphicsLayer {
        val p = host.writerP()
        val s = RailTransitionMath.islandScale(p)
        scaleX = s
        scaleY = s
        alpha = RailTransitionMath.islandAlpha(p)
        transformOrigin = TransformOrigin(0.5f, 0f)
    }
}

/** Beat 6: the caret appears last. */
fun Modifier.railCaret(host: RailTransitionHost?): Modifier {
    if (host == null) return this
    return graphicsLayer { alpha = RailTransitionMath.caretAlpha(host.writerP()) }
}

/** The ding: a ring around the carriage that grows and fades over 600 ms. */
@Composable
fun DingRing(token: Int, centerX: () -> Float, centerY: () -> Float, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(1f) }
    val seen = remember { token }
    LaunchedEffect(token) {
        if (token != seen) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(600))
        }
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        val grow = 1f + 0.9f * t
        val w = 26.dp.toPx() * grow
        val h = 26.dp.toPx() * (1f + 1.6f * t)
        val alpha = 0.9f * (1f - t)
        drawOval(
            color = Color(0xFFFFB36B).copy(alpha = alpha),
            topLeft = Offset(centerX() - w / 2f, centerY() - h / 2f),
            size = Size(w, h),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
