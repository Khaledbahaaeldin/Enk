package app.enk.design.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpringSimulationTest {

    @Test
    fun criticallyDampedSettles() {
        assertEquals(100f, settled(100f, ShellMotion.default, false, 0f, 1.0f), 1f)
    }

    @Test
    fun flickOvershootsLittle() {
        val peak = maxPositionDuring(ShellMotion.flick, 0f, 100f)
        assertTrue("Peak $peak should be > 100f and < 115f", peak > 100f && peak < 115f)
    }

    @Test
    fun reducedMotionSnaps() {
        assertEquals(100f, settled(100f, ShellMotion.default, true, 0f, 0f), 0.0001f)
    }

    @Test
    fun springSpecsPreserveParameters() {
        val defaultSpec = ShellMotion.default.toSpec<Float>()
        assertEquals(ShellMotion.default.damping, defaultSpec.dampingRatio, 0.0001f)
        assertEquals(ShellMotion.default.stiffness, defaultSpec.stiffness, 0.0001f)

        val flickSpec = ShellMotion.flick.toSpec<Float>()
        assertEquals(ShellMotion.flick.damping, flickSpec.dampingRatio, 0.0001f)
        assertEquals(ShellMotion.flick.stiffness, flickSpec.stiffness, 0.0001f)
    }

    @Test
    fun toggleAndSheetSpringSpecs() {
        val toggleSpec = ShellMotion.toggle.toSpec<Float>()
        assertEquals(ShellMotion.toggle.damping, toggleSpec.dampingRatio, 0.0001f)
        assertEquals(ShellMotion.toggle.stiffness, toggleSpec.stiffness, 0.0001f)

        val sheetSpec = ShellMotion.sheet.toSpec<Float>()
        assertEquals(ShellMotion.sheet.damping, sheetSpec.dampingRatio, 0.0001f)
        assertEquals(ShellMotion.sheet.stiffness, sheetSpec.stiffness, 0.0001f)
    }

    @Test
    fun settledWithNegativeStart() {
        assertEquals(0f, settled(0f, ShellMotion.default, false, -100f, 1.0f), 1f)
    }
}
