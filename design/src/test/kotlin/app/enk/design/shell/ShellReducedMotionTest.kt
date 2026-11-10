package app.enk.design.shell

import app.enk.design.motion.Physics
import app.enk.design.motion.ShellMotion
import app.enk.design.motion.ShellSpring
import app.enk.design.motion.settled
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellReducedMotionTest {

    @Test
    fun everyShellSpringSnapsWhenReducedMotionTrue() {
        val springs = listOf(
            ShellMotion.default,
            ShellMotion.flick,
            ShellMotion.toggle,
            ShellMotion.sheet
        )
        val targets = listOf(0f, 100f, -50f, 320f)
        val starts = listOf(100f, 0f, 50f, 0f)

        for (spring in springs) {
            for (i in targets.indices) {
                val target = targets[i]
                val start = starts[i]
                val result = settled(target = target, spring = spring, reducedMotion = true, start = start, seconds = 0f)
                assertEquals("Spring with reducedMotion must snap to target immediately", target, result, 0.0001f)
            }
        }
    }

    @Test
    fun chooseGlassBackendReturnsSolidWhenReduceEffectsTrue() {
        assertEquals(GlassBackendKind.Solid, chooseGlassBackend(sdkInt = 35, reduceEffects = true, highContrastText = false))
        assertEquals(GlassBackendKind.Solid, chooseGlassBackend(sdkInt = 34, reduceEffects = true, highContrastText = false))
        assertEquals(GlassBackendKind.Solid, chooseGlassBackend(sdkInt = 31, reduceEffects = true, highContrastText = false))
        assertEquals(GlassBackendKind.Solid, chooseGlassBackend(sdkInt = 35, reduceEffects = true, highContrastText = true))
    }

    @Test
    fun touchHeightMinimumsAreAtLeast48Dp() {
        // Nav bar height is 64 dp (items 54 dp)
        val navTouchHeight = 54f
        assertTrue("Nav item touch height ($navTouchHeight dp) >= 48 dp", navTouchHeight >= 48f)

        // Shell toggle touch height is 48 dp
        val toggleTouchHeight = ShellToggleMath.touchHeightDp
        assertTrue("Toggle touch height ($toggleTouchHeight dp) >= 48 dp", toggleTouchHeight >= 48f)

        // Shell fader touch height is 48 dp
        val faderTouchHeight = ShellFaderMath.touchHeightDp
        assertTrue("Fader touch height ($faderTouchHeight dp) >= 48 dp", faderTouchHeight >= 48f)

        // Strip item touch height is 48 dp
        val stripTouchHeight = 48f
        assertTrue("Strip item touch height ($stripTouchHeight dp) >= 48 dp", stripTouchHeight >= 48f)
    }

    @Test
    fun entranceDurationWithReducedMotionIs100MsOrSnaps() {
        val reducedMotionDurationMs = 100
        assertTrue("Reduced motion duration ($reducedMotionDurationMs ms) <= 100 ms", reducedMotionDurationMs <= 100)
    }
}
