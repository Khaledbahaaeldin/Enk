package app.enk.keyboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import app.enk.design.shell.LocalReduceEffects
import app.enk.design.theme.EnkShellTheme
import app.enk.design.theme.ThemeId
import app.enk.design.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h820dp")
class EnkKeyboardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val layout: KeyboardLayout by lazy {
        LayoutLoader.load(RuntimeEnvironment.getApplication())
    }

    private val metrics = KeyboardMetrics.forWindow(820f)

    @Test
    fun downOnKeyE() {
        val feedbackCalls = mutableListOf<KeyAction>()
        val applyCalls = mutableListOf<KeyAction>()

        composeRule.setContent {
            EnkShellTheme(ThemeId.Graphite, ThemeMode.Light) {
                EnkKeyboard(
                    layout = layout,
                    metrics = metrics,
                    onFeedback = { feedbackCalls.add(it) },
                    onApply = { applyCalls.add(it) },
                    onAlternative = { _, _ -> },
                    caretProvider = { 0 }
                )
            }
        }

        composeRule.onNodeWithTag("key_e").performTouchInput {
            down(center)
        }

        // Must fire synchronously on down before up
        assertEquals(listOf(KeyAction.Character("e")), feedbackCalls)
        assertEquals(listOf(KeyAction.Character("e")), applyCalls)

        composeRule.onNodeWithTag("key_e").performTouchInput {
            up()
        }

        // Up does not emit duplicates
        assertEquals(listOf(KeyAction.Character("e")), feedbackCalls)
        assertEquals(listOf(KeyAction.Character("e")), applyCalls)
    }

    @Test
    fun twoSimultaneousDowns() {
        val feedbackCalls = mutableListOf<KeyAction>()
        val applyCalls = mutableListOf<KeyAction>()

        composeRule.setContent {
            EnkShellTheme(ThemeId.Graphite, ThemeMode.Light) {
                EnkKeyboard(
                    layout = layout,
                    metrics = metrics,
                    onFeedback = { feedbackCalls.add(it) },
                    onApply = { applyCalls.add(it) },
                    onAlternative = { _, _ -> },
                    caretProvider = { 0 }
                )
            }
        }

        composeRule.onNodeWithTag("key_e").performTouchInput {
            down(0, center)
        }
        composeRule.onNodeWithTag("key_t").performTouchInput {
            down(1, center)
        }

        assertEquals(listOf(KeyAction.Character("e"), KeyAction.Character("t")), feedbackCalls)
        assertEquals(listOf(KeyAction.Character("e"), KeyAction.Character("t")), applyCalls)

        composeRule.onNodeWithTag("key_e").performTouchInput { up(0) }
        composeRule.onNodeWithTag("key_t").performTouchInput { up(1) }
    }

    @Test
    fun shiftOnceThenKeyE() {
        val applyCalls = mutableListOf<KeyAction>()

        composeRule.setContent {
            EnkShellTheme(ThemeId.Graphite, ThemeMode.Light) {
                EnkKeyboard(
                    layout = layout,
                    metrics = metrics,
                    onFeedback = {},
                    onApply = { applyCalls.add(it) },
                    onAlternative = { _, _ -> },
                    caretProvider = { 0 }
                )
            }
        }

        // Tap shift
        composeRule.onNodeWithTag("key_shift").performTouchInput { click() }

        // Next character emits uppercase E
        composeRule.onNodeWithTag("key_e").performTouchInput { click() }
        assertEquals(listOf(KeyAction.Character("E")), applyCalls)

        // Following character returns to lowercase
        composeRule.onNodeWithTag("key_e").performTouchInput { click() }
        assertEquals(listOf(KeyAction.Character("E"), KeyAction.Character("e")), applyCalls)
    }

    @Test
    fun shiftTwiceCaps() {
        val applyCalls = mutableListOf<KeyAction>()
        var virtualClock = 1000L

        composeRule.setContent {
            EnkShellTheme(ThemeId.Graphite, ThemeMode.Light) {
                EnkKeyboard(
                    layout = layout,
                    metrics = metrics,
                    onFeedback = {},
                    onApply = { applyCalls.add(it) },
                    onAlternative = { _, _ -> },
                    caretProvider = { 0 },
                    clock = { virtualClock }
                )
            }
        }

        // Tap shift twice within 300 ms
        composeRule.onNodeWithTag("key_shift").performTouchInput { click() }
        virtualClock += 180L
        composeRule.onNodeWithTag("key_shift").performTouchInput { click() }

        // Three letters all uppercase in caps lock
        composeRule.onNodeWithTag("key_e").performTouchInput { click() }
        composeRule.onNodeWithTag("key_r").performTouchInput { click() }
        composeRule.onNodeWithTag("key_t").performTouchInput { click() }

        assertEquals(
            listOf(
                KeyAction.Character("E"),
                KeyAction.Character("R"),
                KeyAction.Character("T")
            ),
            applyCalls
        )

        // Tap shift once exits caps lock
        virtualClock += 500L
        composeRule.onNodeWithTag("key_shift").performTouchInput { click() }
        composeRule.onNodeWithTag("key_e").performTouchInput { click() }

        assertEquals(
            listOf(
                KeyAction.Character("E"),
                KeyAction.Character("R"),
                KeyAction.Character("T"),
                KeyAction.Character("e")
            ),
            applyCalls
        )
    }

    @Test
    fun pageSwitching() {
        composeRule.setContent {
            EnkShellTheme(ThemeId.Graphite, ThemeMode.Light) {
                EnkKeyboard(
                    layout = layout,
                    metrics = metrics,
                    onFeedback = {},
                    onApply = {},
                    onAlternative = { _, _ -> },
                    caretProvider = { 0 }
                )
            }
        }

        // Initially on Letters page
        composeRule.onNodeWithTag("key_q").assertExists()

        // Switch to Numbers
        composeRule.onNodeWithTag("key_page_numbers").performTouchInput { click() }
        composeRule.onNodeWithTag("key_1").assertExists()
        composeRule.onNodeWithTag("key_q").assertDoesNotExist()

        // Switch to Symbols
        composeRule.onNodeWithTag("key_page_symbols").performTouchInput { click() }
        composeRule.onNodeWithTag("key_~").assertExists()
        composeRule.onNodeWithTag("key_1").assertDoesNotExist()

        // Switch back to Letters
        composeRule.onNodeWithTag("key_page_letters").performTouchInput { click() }
        composeRule.onNodeWithTag("key_q").assertExists()
        composeRule.onNodeWithTag("key_~").assertDoesNotExist()
    }

    @Test
    fun spaceTapVsDrag() {
        val feedbackCalls = mutableListOf<KeyAction>()
        val applyCalls = mutableListOf<KeyAction>()

        composeRule.setContent {
            EnkShellTheme(ThemeId.Graphite, ThemeMode.Light) {
                EnkKeyboard(
                    layout = layout,
                    metrics = metrics,
                    onFeedback = { feedbackCalls.add(it) },
                    onApply = { applyCalls.add(it) },
                    onAlternative = { _, _ -> },
                    caretProvider = { 5 }
                )
            }
        }

        // Tap space: down gives feedback(Space) and no apply; up gives apply(Space)
        composeRule.onNodeWithTag("key_space").performTouchInput {
            down(center)
        }
        assertEquals(listOf(KeyAction.Space), feedbackCalls)
        assertEquals(emptyList<KeyAction>(), applyCalls)

        composeRule.onNodeWithTag("key_space").performTouchInput {
            up()
        }
        assertEquals(listOf(KeyAction.Space), applyCalls)

        feedbackCalls.clear()
        applyCalls.clear()

        // Drag 40 dp horizontally: no onApply(Space), at least one MoveCaretBy with positive delta
        val px40dp = with(composeRule.density) { 40.dp.toPx() }
        composeRule.onNodeWithTag("key_space").performTouchInput {
            down(center)
            moveBy(Offset(px40dp, 0f))
            up()
        }

        assertFalse(applyCalls.contains(KeyAction.Space))
        val moves = applyCalls.filterIsInstance<KeyAction.MoveCaretBy>()
        assertTrue("Expected at least one MoveCaretBy call", moves.isNotEmpty())
        assertTrue("Expected delta > 0", moves.last().delta > 0)
        assertEquals(5, moves.last().origin)
    }

    @Test
    fun enterKey() {
        val feedbackCalls = mutableListOf<KeyAction>()
        val applyCalls = mutableListOf<KeyAction>()

        composeRule.setContent {
            EnkShellTheme(ThemeId.Graphite, ThemeMode.Light) {
                EnkKeyboard(
                    layout = layout,
                    metrics = metrics,
                    onFeedback = { feedbackCalls.add(it) },
                    onApply = { applyCalls.add(it) },
                    onAlternative = { _, _ -> },
                    caretProvider = { 0 }
                )
            }
        }

        composeRule.onNodeWithTag("key_enter").performTouchInput {
            down(center)
        }

        assertEquals(listOf(KeyAction.Enter), feedbackCalls)
        assertEquals(listOf(KeyAction.Enter), applyCalls)

        composeRule.onNodeWithTag("key_enter").performTouchInput {
            up()
        }
    }

    @Test
    fun backspaceHeld() {
        val applyCalls = mutableListOf<KeyAction>()

        composeRule.setContent {
            EnkShellTheme(ThemeId.Graphite, ThemeMode.Light) {
                EnkKeyboard(
                    layout = layout,
                    metrics = metrics,
                    onFeedback = {},
                    onApply = { applyCalls.add(it) },
                    onAlternative = { _, _ -> },
                    caretProvider = { 0 }
                )
            }
        }

        composeRule.mainClock.autoAdvance = false

        // Down immediately emits 1 backspace
        composeRule.onNodeWithTag("key_backspace").performTouchInput {
            down(center)
        }
        assertEquals(1, applyCalls.size)

        // After 400 ms: second backspace
        composeRule.mainClock.advanceTimeBy(400)
        assertEquals(2, applyCalls.size)

        // After 400 + 90 + 90 ms: four backspaces
        composeRule.mainClock.advanceTimeBy(180)
        assertEquals(4, applyCalls.size)

        // Lifting stops repeats
        composeRule.onNodeWithTag("key_backspace").performTouchInput {
            up()
        }
        composeRule.mainClock.advanceTimeBy(1000)
        assertEquals(4, applyCalls.size)

        composeRule.mainClock.autoAdvance = true
    }

    @Test
    fun longPressOnKeyE() {
        val alternatives = mutableListOf<Pair<String, String>>()
        var shiftState by mutableStateOf(KeyboardState(page = PageId.Letters, shift = Shift.Off))

        composeRule.setContent {
            EnkShellTheme(ThemeId.Graphite, ThemeMode.Light) {
                EnkKeyboard(
                    layout = layout,
                    metrics = metrics,
                    onFeedback = {},
                    onApply = {},
                    onAlternative = { base, alt -> alternatives.add(base to alt) },
                    caretProvider = { 0 },
                    initialState = shiftState
                )
            }
        }

        composeRule.mainClock.autoAdvance = false

        // Hold key_e for 400 ms
        composeRule.onNodeWithTag("key_e").performTouchInput {
            down(center)
        }
        composeRule.mainClock.advanceTimeBy(400)

        // Move to the third slot (index 2: "ê").
        // key_e is around x=100dp. Slots start at popupLeft (~84dp). Slot 2 center is around 194dp (+94dp).
        val px94dp = with(composeRule.density) { 94.dp.toPx() }
        composeRule.onNodeWithTag("key_e").performTouchInput {
            moveBy(Offset(px94dp, 0f))
            up()
        }

        assertEquals(listOf("e" to "ê"), alternatives)
        alternatives.clear()

        // Test releasing outside: hold, move far left, release -> nothing called
        composeRule.onNodeWithTag("key_e").performTouchInput {
            down(center)
        }
        composeRule.mainClock.advanceTimeBy(400)
        val farLeftPx = with(composeRule.density) { (-300).dp.toPx() }
        composeRule.onNodeWithTag("key_e").performTouchInput {
            moveBy(Offset(farLeftPx, 0f))
            up()
        }
        assertTrue("Releasing outside popup should emit nothing", alternatives.isEmpty())

        // With shift once, long press key_e -> onAlternative("E", "É")
        composeRule.mainClock.autoAdvance = true
        composeRule.onNodeWithTag("key_shift").performTouchInput { click() }
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithTag("key_e").performTouchInput {
            down(center)
        }
        composeRule.mainClock.advanceTimeBy(400)
        // Second slot (index 1: "é"). Slot 1 center is around 150dp (+50dp from key center).
        val px50dp = with(composeRule.density) { 50.dp.toPx() }
        composeRule.onNodeWithTag("key_e").performTouchInput {
            moveBy(Offset(px50dp, 0f))
            up()
        }
        assertEquals(listOf("E" to "É"), alternatives)

        composeRule.mainClock.autoAdvance = true
    }

    @Test
    fun longPressOnKeyS() {
        val alternatives = mutableListOf<Pair<String, String>>()

        composeRule.setContent {
            EnkShellTheme(ThemeId.Graphite, ThemeMode.Light) {
                EnkKeyboard(
                    layout = layout,
                    metrics = metrics,
                    onFeedback = {},
                    onApply = {},
                    onAlternative = { base, alt -> alternatives.add(base to alt) },
                    caretProvider = { 0 },
                    initialState = KeyboardState(page = PageId.Letters, shift = Shift.Once)
                )
            }
        }

        composeRule.mainClock.autoAdvance = false

        // Long press s with shift once: slot 0 is "ß"
        composeRule.onNodeWithTag("key_s").performTouchInput {
            down(center)
        }
        composeRule.mainClock.advanceTimeBy(400)

        // Slot 0 is selected on popup open; release over slot 0
        composeRule.onNodeWithTag("key_s").performTouchInput {
            up()
        }

        assertEquals(listOf("S" to "ß"), alternatives)
        composeRule.mainClock.autoAdvance = true
    }

    @Test
    fun globeKey() {
        val feedbackCalls = mutableListOf<KeyAction>()
        val applyCalls = mutableListOf<KeyAction>()

        composeRule.setContent {
            EnkShellTheme(ThemeId.Graphite, ThemeMode.Light) {
                EnkKeyboard(
                    layout = layout,
                    metrics = metrics,
                    onFeedback = { feedbackCalls.add(it) },
                    onApply = { applyCalls.add(it) },
                    onAlternative = { _, _ -> },
                    caretProvider = { 0 }
                )
            }
        }

        composeRule.onNodeWithTag("key_globe").performTouchInput { click() }

        assertTrue(feedbackCalls.contains(KeyAction.SystemKeyboard))
        assertTrue(applyCalls.contains(KeyAction.SystemKeyboard))
    }

    @Test
    fun semantics() {
        val feedbackCalls = mutableListOf<KeyAction>()
        val applyCalls = mutableListOf<KeyAction>()

        composeRule.setContent {
            EnkShellTheme(ThemeId.Graphite, ThemeMode.Light) {
                EnkKeyboard(
                    layout = layout,
                    metrics = metrics,
                    onFeedback = { feedbackCalls.add(it) },
                    onApply = { applyCalls.add(it) },
                    onAlternative = { _, _ -> },
                    caretProvider = { 0 }
                )
            }
        }

        // Test every key has Role.Button and non-empty content description
        val keys = listOf("key_a", "key_shift", "key_backspace", "key_space", "key_enter", "key_globe", "key_page_numbers")
        for (tag in keys) {
            val node = composeRule.onNodeWithTag(tag)
            node.assertExists()
            node.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            val config = node.fetchSemanticsNode().config
            val desc = config[SemanticsProperties.ContentDescription].firstOrNull()
            assertTrue("Key $tag must have non-empty description", !desc.isNullOrEmpty())
        }

        // performSemanticsAction(OnClick) on key_a calls feedback then apply once
        composeRule.onNodeWithTag("key_a").performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(listOf(KeyAction.Character("a")), feedbackCalls)
        assertEquals(listOf(KeyAction.Character("a")), applyCalls)

        // performSemanticsAction(OnClick) on key_shift toggles shift ("Shift, on")
        composeRule.onNodeWithTag("key_shift").performSemanticsAction(SemanticsActions.OnClick)
        val shiftDesc = composeRule.onNodeWithTag("key_shift").fetchSemanticsNode().config[SemanticsProperties.ContentDescription].first()
        assertEquals("Shift, on", shiftDesc)
    }

    @Test
    fun height() {
        composeRule.setContent {
            EnkShellTheme(ThemeId.Graphite, ThemeMode.Light) {
                EnkKeyboard(
                    layout = layout,
                    metrics = metrics,
                    onFeedback = {},
                    onApply = {},
                    onAlternative = { _, _ -> },
                    caretProvider = { 0 }
                )
            }
        }

        val expectedHeight = metrics.totalHeightDp(4)
        val actualHeight = composeRule.onNodeWithTag("enk_keyboard").getUnclippedBoundsInRoot().height.value
        assertEquals(expectedHeight, actualHeight, 0.5f)
    }

    @Test
    fun reduceEffects() {
        val feedbackCalls = mutableListOf<KeyAction>()
        val applyCalls = mutableListOf<KeyAction>()

        composeRule.setContent {
            CompositionLocalProvider(LocalReduceEffects provides true) {
                EnkShellTheme(ThemeId.Graphite, ThemeMode.Light) {
                    EnkKeyboard(
                        layout = layout,
                        metrics = metrics,
                        onFeedback = { feedbackCalls.add(it) },
                        onApply = { applyCalls.add(it) },
                        onAlternative = { _, _ -> },
                        caretProvider = { 0 }
                    )
                }
            }
        }

        // Test 1: down on key_e
        composeRule.onNodeWithTag("key_e").performTouchInput {
            down(center)
        }
        assertEquals(listOf(KeyAction.Character("e")), feedbackCalls)
        assertEquals(listOf(KeyAction.Character("e")), applyCalls)
        composeRule.onNodeWithTag("key_e").performTouchInput { up() }

        // Test 5: page switching works without animation delays
        composeRule.onNodeWithTag("key_page_numbers").performTouchInput { click() }
        composeRule.onNodeWithTag("key_1").assertExists()
        composeRule.onNodeWithTag("key_q").assertDoesNotExist()
    }

    private fun checkAccessibilityForPage(pageId: PageId) {
        val pageState = KeyboardState(page = pageId, shift = Shift.Off)
        composeRule.setContent {
            EnkShellTheme(ThemeId.Graphite, ThemeMode.Light) {
                EnkKeyboard(
                    layout = layout,
                    metrics = metrics,
                    onFeedback = {},
                    onApply = {},
                    onAlternative = { _, _ -> },
                    caretProvider = { 0 },
                    initialState = pageState
                )
            }
        }
        composeRule.waitForIdle()

        val seenDescriptions = mutableSetOf<String>()
        val rows = layout.pages.getValue(pageId)

        for ((r, row) in rows.withIndex()) {
            val expectedMinHeight = if (r == 0 || r == rows.size - 1) {
                metrics.keyHeightDp + metrics.gapYDp / 2f
            } else {
                metrics.keyHeightDp + metrics.gapYDp
            }

            for (spec in row.keys) {
                if (spec is SpacerKey) continue
                val tag = when (spec) {
                    is CharKey -> "key_${spec.label}"
                    is ShiftKey -> "key_shift"
                    is BackspaceKey -> "key_backspace"
                    is SpaceKey -> "key_space"
                    is EnterKey -> "key_enter"
                    is GlobeKey -> "key_globe"
                    is PageKey -> "key_page_${spec.target.name.lowercase()}"
                    else -> continue
                }

                val node = composeRule.onNodeWithTag(tag)
                node.assertExists()
                node.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))

                val config = node.fetchSemanticsNode().config
                val desc = config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
                assertTrue("Key $tag must have non-empty description", !desc.isNullOrEmpty())
                assertTrue("No duplicate content descriptions on $pageId: duplicate '$desc'", seenDescriptions.add(desc!!))

                val heightDp = node.getUnclippedBoundsInRoot().height.value
                assertTrue("Key $tag height $heightDp must be >= $expectedMinHeight - 0.5", heightDp >= expectedMinHeight - 0.5f)
            }
        }
    }

    @Test
    fun accessibilityLettersPage() {
        checkAccessibilityForPage(PageId.Letters)
    }

    @Test
    fun accessibilityNumbersPage() {
        checkAccessibilityForPage(PageId.Numbers)
    }

    @Test
    fun accessibilitySymbolsPage() {
        checkAccessibilityForPage(PageId.Symbols)
    }

    @Test
    fun textScaleDoesNotChangeKeyboardHeight() {
        var fontScale by mutableStateOf(1.0f)

        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f, fontScale = fontScale)
            ) {
                EnkShellTheme(ThemeId.Graphite, ThemeMode.Light) {
                    EnkKeyboard(
                        layout = layout,
                        metrics = metrics,
                        onFeedback = {},
                        onApply = {},
                        onAlternative = { _, _ -> },
                        caretProvider = { 0 }
                    )
                }
            }
        }
        val h1 = composeRule.onNodeWithTag("enk_keyboard").getUnclippedBoundsInRoot().height.value

        fontScale = 2.0f
        composeRule.waitForIdle()
        val h2 = composeRule.onNodeWithTag("enk_keyboard").getUnclippedBoundsInRoot().height.value

        assertEquals(h1, h2, 0.01f)
    }

    @Test
    fun reduceEffectsNoCrossfadeDelay() {
        composeRule.setContent {
            CompositionLocalProvider(LocalReduceEffects provides true) {
                EnkShellTheme(ThemeId.Graphite, ThemeMode.Light) {
                    EnkKeyboard(
                        layout = layout,
                        metrics = metrics,
                        onFeedback = {},
                        onApply = {},
                        onAlternative = { _, _ -> },
                        caretProvider = { 0 }
                    )
                }
            }
        }

        composeRule.onNodeWithTag("key_page_numbers").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("key_1").assertExists()
        composeRule.onNodeWithTag("key_q").assertDoesNotExist()
    }
}
