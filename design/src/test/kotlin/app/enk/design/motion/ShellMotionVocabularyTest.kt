package app.enk.design.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellMotionVocabularyTest {

    @Test
    fun onlyMomentumRolesBounce() {
        for ((name, spring) in ShellMotion.all) {
            if (name in ShellMotion.momentumRoles) {
                assertTrue("$name is a momentum role and may bounce", spring.damping < 1f)
            } else {
                assertTrue(
                    "$name must not bounce (damping ${spring.damping})",
                    spring.damping >= ShellMotion.MIN_UNBOUNCED_DAMPING
                )
            }
        }
    }

    @Test
    fun everyMomentumRoleIsInTheVocabulary() {
        assertTrue(ShellMotion.all.keys.containsAll(ShellMotion.momentumRoles))
    }

    @Test
    fun responsesAreSnappyEnoughForUi() {
        for ((name, spring) in ShellMotion.all) {
            if (name == "railPage") continue // the Rail is a deliberate, slow set piece
            assertTrue("$name response ${spring.response} should be 0.45 s or less", spring.response <= 0.45f)
        }
    }

    @Test
    fun stiffnessFollowsResponse() {
        val s = ShellSpring(0.25f, 1f)
        val omega = (2.0 * Math.PI / 0.25).toFloat()
        assertEquals(omega * omega, s.stiffness, 0.5f)
    }
}
