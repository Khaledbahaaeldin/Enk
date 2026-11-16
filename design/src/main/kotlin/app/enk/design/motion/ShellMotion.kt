package app.enk.design.motion

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

data class ShellSpring(val response: Float, val damping: Float) {
    val stiffness: Float
        get() = (2f * Math.PI.toFloat() / response).let { it * it }

    fun <T> toSpec(): SpringSpec<T> = spring(dampingRatio = damping, stiffness = stiffness)
}

/**
 * The app's one motion vocabulary. Each token is a role; pick the role, never a number.
 *
 * | role     | response | damping | for                                                     |
 * |----------|----------|---------|---------------------------------------------------------|
 * | press    | 0.20     | 1.0     | touch feedback (swell, highlight)                       |
 * | default  | 0.35     | 1.0     | a thing moving from A to B ("move")                     |
 * | settle   | 0.30     | 1.0     | supporting moves that follow a lead                     |
 * | lens     | 0.24     | 1.0     | the nav lens between tabs                               |
 * | toggle   | 0.28     | 1.0     | the toggle bead                                         |
 * | page     | 0.32     | 1.0     | screens entering and leaving                            |
 * | sheet    | 0.38     | 0.86    | panels and sheets growing from their source             |
 * | flick    | 0.35     | 0.8     | release after a gesture that carried momentum           |
 * | release  | 0.35     | 0.8     | release of a press that was dragged                     |
 * | railPage | 0.72     | 0.76    | the writer's Rail transition only                       |
 *
 * Bounce (damping under 0.85) is only for [flick], [release] and [railPage], where the gesture
 * or the metaphor carries momentum. Everything else is critically damped or close to it.
 */
object ShellMotion {
    val default = ShellSpring(0.35f, 1f)
    val flick = ShellSpring(0.35f, 0.8f)
    val toggle = ShellSpring(0.28f, 1f)
    val sheet = ShellSpring(0.38f, 0.86f)
    val press = ShellSpring(0.20f, 1f)
    /** The nav lens travelling between tabs: crisp, critically damped. */
    val lens = ShellSpring(0.24f, 1f)
    /** Small supporting moves that follow a lead (tab content drifting in, panel rows). */
    val settle = ShellSpring(0.30f, 1f)
    val release = ShellSpring(0.35f, 0.8f)
    val page = ShellSpring(0.32f, 1f)
    val railPage = ShellSpring(0.72f, 0.76f)

    /** Every token by role name, for tests and tooling. */
    val all: Map<String, ShellSpring> = mapOf(
        "default" to default, "flick" to flick, "toggle" to toggle, "sheet" to sheet,
        "press" to press, "lens" to lens, "settle" to settle, "release" to release,
        "page" to page, "railPage" to railPage
    )

    /** The only roles allowed to bounce (damping under [MIN_UNBOUNCED_DAMPING]). */
    val momentumRoles: Set<String> = setOf("flick", "release", "railPage")
    const val MIN_UNBOUNCED_DAMPING = 0.85f

    const val LONG_PRESS_MS = 420L
    const val DRAG_SLOP_DP = 8f
    const val FLICK_THRESHOLD = 350f
}
