package app.enk.keyboard

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.input.key.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HardwareKeysTest {

    private fun keyEventWithUnicode(codePoint: Int, action: Int = AndroidKeyEvent.ACTION_DOWN): KeyEvent {
        val native = object : AndroidKeyEvent(action, AndroidKeyEvent.KEYCODE_UNKNOWN) {
            override fun getUnicodeChar(): Int = codePoint
            override fun getUnicodeChar(metaState: Int): Int = codePoint
        }
        return KeyEvent(native)
    }

    @Test
    fun keyUpReturnsNull() {
        val event = KeyEvent(AndroidKeyEvent(AndroidKeyEvent.ACTION_UP, AndroidKeyEvent.KEYCODE_A))
        assertNull(hardwareKeyToAction(event, caret = 5, columns = 28))
    }

    @Test
    fun modifiersReturnNull() {
        // Ctrl+A
        val ctrlEvent = KeyEvent(AndroidKeyEvent(0L, 0L, AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_A, 0, AndroidKeyEvent.META_CTRL_ON))
        assertNull(hardwareKeyToAction(ctrlEvent, caret = 5, columns = 28))

        // Alt+A
        val altEvent = KeyEvent(AndroidKeyEvent(0L, 0L, AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_A, 0, AndroidKeyEvent.META_ALT_ON))
        assertNull(hardwareKeyToAction(altEvent, caret = 5, columns = 28))

        // Meta+A
        val metaEvent = KeyEvent(AndroidKeyEvent(0L, 0L, AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_A, 0, AndroidKeyEvent.META_META_ON))
        assertNull(hardwareKeyToAction(metaEvent, caret = 5, columns = 28))
    }

    @Test
    fun backspaceEnterSpace() {
        val bs = KeyEvent(AndroidKeyEvent(AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_DEL))
        assertEquals(KeyAction.Backspace, hardwareKeyToAction(bs, caret = 5, columns = 28))

        val enter = KeyEvent(AndroidKeyEvent(AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_ENTER))
        assertEquals(KeyAction.Enter, hardwareKeyToAction(enter, caret = 5, columns = 28))

        val numEnter = KeyEvent(AndroidKeyEvent(AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_NUMPAD_ENTER))
        assertEquals(KeyAction.Enter, hardwareKeyToAction(numEnter, caret = 5, columns = 28))

        val space = KeyEvent(AndroidKeyEvent(AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_SPACE))
        assertEquals(KeyAction.Space, hardwareKeyToAction(space, caret = 5, columns = 28))
    }

    @Test
    fun arrowKeysMoveCaretWithColumns() {
        val left = KeyEvent(AndroidKeyEvent(AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_DPAD_LEFT))
        assertEquals(KeyAction.MoveCaretBy(-1, 10), hardwareKeyToAction(left, caret = 10, columns = 28))

        val right = KeyEvent(AndroidKeyEvent(AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_DPAD_RIGHT))
        assertEquals(KeyAction.MoveCaretBy(1, 10), hardwareKeyToAction(right, caret = 10, columns = 28))

        val up = KeyEvent(AndroidKeyEvent(AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_DPAD_UP))
        assertEquals(KeyAction.MoveCaretBy(-28, 50), hardwareKeyToAction(up, caret = 50, columns = 28))

        val down = KeyEvent(AndroidKeyEvent(AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_DPAD_DOWN))
        assertEquals(KeyAction.MoveCaretBy(28, 50), hardwareKeyToAction(down, caret = 50, columns = 28))
    }

    @Test
    fun printableCharacters() {
        // Via KeyEvent(ACTION_DOWN, KEYCODE_A)
        val charA = KeyEvent(AndroidKeyEvent(AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_A))
        assertEquals(KeyAction.Character("a"), hardwareKeyToAction(charA, caret = 0, columns = 28))

        // Via unicode overload / unicode provider
        val unicodeZ = keyEventWithUnicode('Z'.code)
        assertEquals(KeyAction.Character("Z"), hardwareKeyToAction(unicodeZ, caret = 0, columns = 28))
    }

    @Test
    fun controlCharactersAndLoneSurrogatesReturnNull() {
        val ctrlChar = keyEventWithUnicode(1)
        assertNull(hardwareKeyToAction(ctrlChar, caret = 0, columns = 28))

        val loneSurrogate = keyEventWithUnicode(0xD800)
        assertNull(hardwareKeyToAction(loneSurrogate, caret = 0, columns = 28))
    }
}
