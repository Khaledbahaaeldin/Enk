package app.enk.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardStateTest {

    @Test
    fun tapShiftTransitions() {
        var state = KeyboardState()
        assertEquals(Shift.Off, state.shift)

        // 1. Off -> Once
        state = state.tapShift(1000L)
        assertEquals(Shift.Once, state.shift)
        assertEquals(1000L, state.lastShiftTapMs)

        // 2. Once -> Caps within double tap (exactly 350ms)
        val capsState = state.tapShift(1350L)
        assertEquals(Shift.Caps, capsState.shift)
        assertEquals(1350L, capsState.lastShiftTapMs)

        // 3. Caps -> Off on tap
        val offFromCaps = capsState.tapShift(2000L)
        assertEquals(Shift.Off, offFromCaps.shift)
        assertEquals(2000L, offFromCaps.lastShiftTapMs)

        // 4. Once -> Off when tap is outside double tap window (351ms)
        state = KeyboardState().tapShift(1000L)
        val offFromOnce = state.tapShift(1351L)
        assertEquals(Shift.Off, offFromOnce.shift)
        assertEquals(1351L, offFromOnce.lastShiftTapMs)
    }

    @Test
    fun tapShiftOnNonLettersPageDoesNothing() {
        val numbersState = KeyboardState(page = PageId.Numbers, shift = Shift.Off)
        assertEquals(numbersState, numbersState.tapShift(1000L))

        val symbolsState = KeyboardState(page = PageId.Symbols, shift = Shift.Off)
        assertEquals(symbolsState, symbolsState.tapShift(1000L))
    }

    @Test
    fun afterCharacterTransitions() {
        // Once becomes Off
        val onceState = KeyboardState(shift = Shift.Once)
        assertEquals(Shift.Off, onceState.afterCharacter().shift)

        // Off stays Off
        val offState = KeyboardState(shift = Shift.Off)
        assertEquals(Shift.Off, offState.afterCharacter().shift)

        // Caps stays Caps
        val capsState = KeyboardState(shift = Shift.Caps)
        assertEquals(Shift.Caps, capsState.afterCharacter().shift)
    }

    @Test
    fun toPageResetsShift() {
        val capsState = KeyboardState(page = PageId.Letters, shift = Shift.Caps)
        val toNumbers = capsState.toPage(PageId.Numbers)
        assertEquals(PageId.Numbers, toNumbers.page)
        assertEquals(Shift.Off, toNumbers.shift)

        val onceState = KeyboardState(page = PageId.Letters, shift = Shift.Once)
        val toSymbols = onceState.toPage(PageId.Symbols)
        assertEquals(PageId.Symbols, toSymbols.page)
        assertEquals(Shift.Off, toSymbols.shift)
    }

    @Test
    fun outputForRules() {
        val offLetters = KeyboardState(page = PageId.Letters, shift = Shift.Off)
        val onceLetters = KeyboardState(page = PageId.Letters, shift = Shift.Once)
        val capsLetters = KeyboardState(page = PageId.Letters, shift = Shift.Caps)
        val offNumbers = KeyboardState(page = PageId.Numbers, shift = Shift.Off)
        val onceNumbers = KeyboardState(page = PageId.Numbers, shift = Shift.Once)

        assertEquals("a", outputFor("a", offLetters))
        assertEquals("A", outputFor("a", onceLetters))
        assertEquals("A", outputFor("a", capsLetters))

        // Non-letters page never uppercases
        assertEquals("1", outputFor("1", offNumbers))
        assertEquals("1", outputFor("1", onceNumbers))
        assertEquals(".", outputFor(".", onceNumbers))

        // Empty string
        assertEquals("", outputFor("", onceLetters))

        // Surrogate pair
        assertEquals("😀", outputFor("😀", onceLetters))
    }

    @Test
    fun alternativeForRules() {
        val offLetters = KeyboardState(page = PageId.Letters, shift = Shift.Off)
        val onceLetters = KeyboardState(page = PageId.Letters, shift = Shift.Once)
        val capsLetters = KeyboardState(page = PageId.Letters, shift = Shift.Caps)
        val onceNumbers = KeyboardState(page = PageId.Numbers, shift = Shift.Once)

        // "é" -> "É" (same code point count: 1)
        assertEquals("é", alternativeFor("é", offLetters))
        assertEquals("É", alternativeFor("é", onceLetters))
        assertEquals("É", alternativeFor("é", capsLetters))

        // "ß" uppercase in root locale is "SS" (length 2 code points instead of 1), must stay "ß"
        assertEquals("ß", alternativeFor("ß", offLetters))
        assertEquals("ß", alternativeFor("ß", onceLetters))
        assertEquals("ß", alternativeFor("ß", capsLetters))

        // Non-letters page never uppercases
        assertEquals("é", alternativeFor("é", onceNumbers))

        // Empty string
        assertEquals("", alternativeFor("", onceLetters))

        // Surrogate pair string
        assertEquals("😀", alternativeFor("😀", onceLetters))
    }
}
