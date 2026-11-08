package app.enk.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BellPolicyTest {

    @Test
    fun bellFiresOncePerLine() {
        val policy = BellPolicy(cols = 28)

        // Threshold is cols - 4 = 24
        assertFalse(policy.onCaret(line = 0, col = 23))
        assertTrue(policy.onCaret(line = 0, col = 24))
        assertFalse(policy.onCaret(line = 0, col = 25))
        assertFalse(policy.onCaret(line = 0, col = 28))

        // New line re-arms and fires when >= 24
        assertFalse(policy.onCaret(line = 1, col = 10))
        assertTrue(policy.onCaret(line = 1, col = 24))
        assertFalse(policy.onCaret(line = 1, col = 25))
    }

    @Test
    fun bellRearmsOnReturnToLine() {
        val policy = BellPolicy(cols = 28)

        // Fire on line 0
        assertTrue(policy.onCaret(line = 0, col = 24))
        assertFalse(policy.onCaret(line = 0, col = 25))

        // Move to line 1
        assertFalse(policy.onCaret(line = 1, col = 5))

        // Return to line 0 re-arms for line 0
        assertFalse(policy.onCaret(line = 0, col = 20))
        assertTrue("Bell must rearm when returning to line 0", policy.onCaret(line = 0, col = 24))
        assertFalse(policy.onCaret(line = 0, col = 26))
    }

    @Test
    fun colsLessThanOrEqualToFourNeverFires() {
        val policy4 = BellPolicy(cols = 4)
        assertFalse(policy4.onCaret(line = 0, col = 0))
        assertFalse(policy4.onCaret(line = 0, col = 4))
        assertFalse(policy4.onCaret(line = 0, col = 10))

        val policyZero = BellPolicy(cols = 0)
        assertFalse(policyZero.onCaret(line = 0, col = 0))
    }

    @Test
    fun colPastColsFiresOnce() {
        val policy = BellPolicy(cols = 28)
        // Jumping straight to col 35 (past cols) on a fresh line
        assertTrue(policy.onCaret(line = 0, col = 35))
        assertFalse(policy.onCaret(line = 0, col = 36))
    }

    @Test
    fun backwardCaretMovementDoesNotRefire() {
        val policy = BellPolicy(cols = 28)
        assertTrue(policy.onCaret(line = 0, col = 24))
        // Caret moved back to col 10
        assertFalse(policy.onCaret(line = 0, col = 10))
        // Caret moved forward again to col 25 on same line
        assertFalse("Must not re-fire on same line", policy.onCaret(line = 0, col = 25))
    }

    @Test
    fun colsZeroOrNegativeNeverFires() {
        for (badCols in listOf(0, -1, -5, -100)) {
            val policy = BellPolicy(cols = badCols)
            assertFalse(policy.onCaret(line = 0, col = 0))
            assertFalse(policy.onCaret(line = 0, col = 10))
            assertFalse(policy.onCaret(line = 0, col = -5))
            assertFalse(policy.onCaret(line = 1, col = 20))
        }
    }

    @Test
    fun negativeColNeverFires() {
        val policy = BellPolicy(cols = 28)
        assertFalse(policy.onCaret(line = 0, col = -1))
        assertFalse(policy.onCaret(line = 0, col = -5))
        assertFalse(policy.onCaret(line = 0, col = -100))
    }

    @Test
    fun boundaryConditionColsFiveFiresAtColOne() {
        val policy = BellPolicy(cols = 5)
        // 5 - 4 = 1
        assertFalse(policy.onCaret(line = 0, col = 0))
        assertTrue("cols=5 must fire at col=1", policy.onCaret(line = 0, col = 1))
        assertFalse(policy.onCaret(line = 0, col = 2))
        assertFalse(policy.onCaret(line = 0, col = 5))
    }

    @Test
    fun caretMoving24Then28Then30FiresOnlyAt24() {
        val policy = BellPolicy(cols = 28)
        assertTrue(policy.onCaret(line = 0, col = 24))
        assertFalse("Moving to col 28 on same line must not fire again", policy.onCaret(line = 0, col = 28))
        assertFalse("Moving to col 30 on same line must not fire again", policy.onCaret(line = 0, col = 30))
    }

    @Test
    fun rapidJumpingBetweenMultipleLines() {
        val policy = BellPolicy(cols = 28)
        assertTrue("Line 0 fires at col 24", policy.onCaret(line = 0, col = 24))
        assertTrue("Line 1 fires at col 24", policy.onCaret(line = 1, col = 24))
        assertTrue("Line 2 fires at col 24", policy.onCaret(line = 2, col = 24))
        assertTrue("Return to line 0 rearms and fires at col 24", policy.onCaret(line = 0, col = 24))
        assertTrue("Return to line 1 rearms and fires at col 24", policy.onCaret(line = 1, col = 24))
    }
}
