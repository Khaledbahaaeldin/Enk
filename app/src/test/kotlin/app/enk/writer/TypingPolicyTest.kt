package app.enk.writer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TypingPolicyTest {

    @Test
    fun draggingIsAlwaysTyping() {
        assertTrue(TypingPolicy.isTyping(nowMs = 1000L, lastKeyMs = 0L, dragging = true))
        assertTrue(TypingPolicy.isTyping(nowMs = 0L, lastKeyMs = 1000L, dragging = true))
    }

    @Test
    fun within600MsIsTyping() {
        assertTrue(TypingPolicy.isTyping(nowMs = 1000L, lastKeyMs = 1000L, dragging = false))
        assertTrue(TypingPolicy.isTyping(nowMs = 1500L, lastKeyMs = 1000L, dragging = false))
        assertTrue(TypingPolicy.isTyping(nowMs = 1600L, lastKeyMs = 1000L, dragging = false))
    }

    @Test
    fun after600MsIsNotTyping() {
        assertFalse(TypingPolicy.isTyping(nowMs = 1601L, lastKeyMs = 1000L, dragging = false))
        assertFalse(TypingPolicy.isTyping(nowMs = 2000L, lastKeyMs = 1000L, dragging = false))
    }

    @Test
    fun futureLastKeyIsNotTyping() {
        assertFalse(TypingPolicy.isTyping(nowMs = 1000L, lastKeyMs = 1001L, dragging = false))
    }
}
