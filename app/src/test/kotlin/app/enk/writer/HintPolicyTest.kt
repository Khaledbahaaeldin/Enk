package app.enk.writer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HintPolicyTest {

    @Test
    fun defaultStateNotDismissed() {
        val policy = HintPolicy()
        assertFalse(policy.hasScrubbed)
        assertFalse(policy.hasLongPressed)
        assertFalse(policy.isDismissed)
    }

    @Test
    fun scrubOnlyDoesNotDismiss() {
        val policy = HintPolicy().onScrub()
        assertTrue(policy.hasScrubbed)
        assertFalse(policy.hasLongPressed)
        assertFalse(policy.isDismissed)
    }

    @Test
    fun longPressOnlyDoesNotDismiss() {
        val policy = HintPolicy().onLongPress()
        assertFalse(policy.hasScrubbed)
        assertTrue(policy.hasLongPressed)
        assertFalse(policy.isDismissed)
    }

    @Test
    fun bothGesturesDismissesHint() {
        val policy = HintPolicy().onScrub().onLongPress()
        assertTrue(policy.hasScrubbed)
        assertTrue(policy.hasLongPressed)
        assertTrue(policy.isDismissed)
    }

    @Test
    fun idempotentGestures() {
        val policy = HintPolicy().onScrub().onScrub()
        assertTrue(policy.hasScrubbed)
        assertFalse(policy.isDismissed)
    }

    @Test
    fun statePreservationFromSettings() {
        val restored = HintPolicy(hasScrubbed = true, hasLongPressed = true)
        assertTrue(restored.isDismissed)
    }
}
