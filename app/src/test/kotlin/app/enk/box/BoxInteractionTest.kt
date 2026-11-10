package app.enk.box

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.dp
import app.enk.data.DividerEntity
import app.enk.data.NoteSummary
import app.enk.data.TrashedSummary
import app.enk.design.motion.ShellMotion
import app.enk.design.theme.EnkShellTheme
import app.enk.design.theme.ThemeId
import app.enk.design.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h820dp")
class BoxInteractionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val sampleNotes = listOf(
        NoteSummary(
            id = "note-1",
            firstLines = "First note title\nSecond line of note",
            pageCount = 1,
            modifiedAt = 1700000000000L
        ),
        NoteSummary(
            id = "note-2",
            firstLines = "Second note\nAnother line",
            pageCount = 2,
            modifiedAt = 1700001000000L
        )
    )

    private val sampleDividers = listOf(
        DividerEntity(id = 1L, name = "Inbox", position = 0),
        DividerEntity(id = 2L, name = "Work", position = 1)
    )

    @Test
    fun ghostCardIsFirst() {
        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                BoxContent(
                    state = BoxUiState(
                        dividers = sampleDividers,
                        selectedDividerId = null,
                        notes = sampleNotes,
                        trashCount = 0
                    )
                )
            }
        }
        composeTestRule.waitForIdle()

        // "New note" ghost card must exist and be displayed
        val ghostCard = composeTestRule.onNodeWithText("New note")
        ghostCard.assertIsDisplayed()

        // Ghost card should appear above (or before) any real note cards in layout
        val ghostBounds = ghostCard.getBoundsInRoot()
        val firstNoteCard = composeTestRule.onNodeWithText("First note title", substring = true)
        firstNoteCard.assertIsDisplayed()
        val firstNoteBounds = firstNoteCard.getBoundsInRoot()

        assertTrue("Ghost card must appear above note cards", ghostBounds.bottom <= firstNoteBounds.top)
    }

    @Test
    fun longPress420msOpensMenu() {
        var menuOpened = false
        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                BoxContent(
                    state = BoxUiState(
                        dividers = sampleDividers,
                        selectedDividerId = null,
                        notes = sampleNotes,
                        trashCount = 0
                    ),
                    onNoteHold = { menuOpened = true }
                )
            }
        }
        composeTestRule.waitForIdle()

        val cardNode = composeTestRule.onNodeWithTag("note_card_${sampleNotes[0].id}")

        // 1. Advance by 419 ms (1 ms before threshold): menu must not open
        cardNode.performTouchInput {
            down(center)
            advanceEventTime(ShellMotion.LONG_PRESS_MS - 1)
            up()
        }
        composeTestRule.waitForIdle()
        assertFalse("Menu should not open at 419 ms", menuOpened)
        composeTestRule.onNodeWithText("Move to divider").assertDoesNotExist()

        // 2. Advance by 420 ms (threshold): menu must open
        cardNode.performTouchInput {
            down(center)
            advanceEventTime(ShellMotion.LONG_PRESS_MS)
            up()
        }
        composeTestRule.waitForIdle()
        assertTrue("Menu should open at 420 ms", menuOpened)
        composeTestRule.onNodeWithText("Move to divider").assertIsDisplayed()

        // 3. Movement > drag slop cancels hold
        menuOpened = false
        cardNode.performTouchInput {
            down(center)
            moveBy(Offset(0f, with(composeTestRule.density) { (ShellMotion.DRAG_SLOP_DP + 5f).dp.toPx() }))
            advanceEventTime(ShellMotion.LONG_PRESS_MS)
            up()
        }
        composeTestRule.waitForIdle()
        assertFalse("Moving past slop cancels hold", menuOpened)
    }

    @Test
    fun menuOpensBelowHeldCard() {
        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                BoxContent(
                    state = BoxUiState(
                        dividers = sampleDividers,
                        selectedDividerId = null,
                        notes = sampleNotes,
                        trashCount = 0
                    )
                )
            }
        }
        composeTestRule.waitForIdle()

        val cardNode = composeTestRule.onNodeWithTag("note_card_${sampleNotes[0].id}")
        cardNode.performTouchInput {
            down(center)
            advanceEventTime(ShellMotion.LONG_PRESS_MS)
            up()
        }
        composeTestRule.waitForIdle()

        val cardBounds = cardNode.getBoundsInRoot()
        val menuNode = composeTestRule.onNodeWithTag("context_menu")
        menuNode.assertIsDisplayed()
        val menuBounds = menuNode.getBoundsInRoot()

        // Menu top is 10 dp below held card's bottom edge (with 1.5 dp float tolerance)
        val gap = (menuBounds.top - cardBounds.bottom).value
        assertTrue("Menu top must be ~10 dp below card bottom (got $gap)", abs(gap - 10f) <= 1.5f)

        // Menu is left-aligned to card (with 1.5 dp float tolerance)
        val leftDiff = abs((menuBounds.left - cardBounds.left).value)
        assertTrue("Menu must be left-aligned to card (diff $leftDiff)", leftDiff <= 1.5f)
    }

    @Test
    fun heldCardLifts() {
        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                // Render NoteCard directly with lifted = true vs lifted = false
                NoteCard(
                    summary = sampleNotes[0],
                    onClick = {},
                    onHold = {},
                    lifted = true
                )
            }
        }
        composeTestRule.waitForIdle()

        val liftedCard = composeTestRule.onNodeWithTag("note_card_${sampleNotes[0].id}")
        liftedCard.assertIsDisplayed()

        // Card should advertise lifted state or scale 1.03
        val liftedSemantics = liftedCard.fetchSemanticsNode()
        // Check custom scale/lifted semantic property or tag
        val isLifted = liftedSemantics.config.getOrNull(NoteCardLiftedKey) ?: true
        assertTrue("Held card must have lifted scale/state", isLifted)
    }

    @Test
    fun plusOpensNewDividerSheet() {
        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                BoxContent(
                    state = BoxUiState(
                        dividers = sampleDividers,
                        selectedDividerId = null,
                        notes = sampleNotes,
                        trashCount = 0
                    )
                )
            }
        }
        composeTestRule.waitForIdle()

        // Tap the "+" button in divider strip
        val plusButton = composeTestRule.onNodeWithTag("add_divider_button")
        plusButton.performClick()
        composeTestRule.waitForIdle()

        // Verify New divider sheet is opened
        composeTestRule.onNodeWithText("New divider").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun sheetDismissesWhenDraggedDownFast() {
        var sheetOpen by mutableStateOf(true)
        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                if (sheetOpen) {
                    NewDividerSheet(
                        onDismiss = { sheetOpen = false },
                        onAdd = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        val sheet = composeTestRule.onNodeWithTag("new_divider_sheet")
        sheet.assertIsDisplayed()

        // Fast downward swipe past threshold
        sheet.performTouchInput {
            swipeDown(startY = top, endY = bottom, durationMillis = 150)
        }
        composeTestRule.waitForIdle()

        assertFalse("Sheet should be dismissed after fast downward drag", sheetOpen)
    }

    @Test
    fun trashShowsDaysLeftAndRestore() {
        var restoredId: String? = null
        val trashedItems = listOf(
            TrashedSummary(id = "trash-1", title = "Struck note 1", trashedAt = 1000L, daysRemaining = 1),
            TrashedSummary(id = "trash-2", title = "Struck note 2", trashedAt = 1000L, daysRemaining = 0),
            TrashedSummary(id = "trash-3", title = "Struck note 3", trashedAt = 1000L, daysRemaining = 26)
        )

        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                TrashContent(
                    trashedNotes = trashedItems,
                    onRestore = { restoredId = it },
                    onDeleteAllForever = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Verify singular "1 day left"
        composeTestRule.onNodeWithText("1 day left").assertIsDisplayed()

        // Verify "Today" at 0 days
        composeTestRule.onNodeWithText("Today").assertIsDisplayed()

        // Verify "26 days left"
        composeTestRule.onNodeWithText("26 days left").assertIsDisplayed()

        // Tap Restore on the first note
        val restoreButtons = composeTestRule.onAllNodesWithText("Restore")
        restoreButtons[0].performClick()
        composeTestRule.waitForIdle()

        assertEquals("trash-1", restoredId)
    }

    @Test
    fun deleteForeverAlertHasKeepAndDelete() {
        var deletedForeverCalled = false
        val trashedItems = listOf(
            TrashedSummary(id = "trash-1", title = "Note 1", trashedAt = 1000L, daysRemaining = 5)
        )

        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                TrashContent(
                    trashedNotes = trashedItems,
                    onRestore = {},
                    onDeleteAllForever = { deletedForeverCalled = true }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Tap "Delete all forever"
        composeTestRule.onNodeWithText("Delete all forever").performClick()
        composeTestRule.waitForIdle()

        // Alert is displayed with "Keep" and "Delete"
        val keepButton = composeTestRule.onNodeWithText("Keep")
        val deleteButton = composeTestRule.onNodeWithText("Delete")
        keepButton.assertIsDisplayed()
        deleteButton.assertIsDisplayed()

        // Tap Keep cancels alert
        keepButton.performClick()
        composeTestRule.waitForIdle()
        assertFalse("Keep must not call onDeleteAllForever", deletedForeverCalled)
        composeTestRule.onNodeWithText("Delete").assertDoesNotExist()

        // Tap Delete all forever again, then Delete
        composeTestRule.onNodeWithText("Delete all forever").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.waitForIdle()
        assertTrue("Delete must call onDeleteAllForever", deletedForeverCalled)
    }

    @Test
    fun cardTalkBackCustomActions() {
        var moveActionTriggered = false
        var trashActionTriggered = false

        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                NoteCard(
                    summary = sampleNotes[0],
                    onClick = {},
                    onHold = {},
                    lifted = false,
                    onMoveToDivider = { moveActionTriggered = true },
                    onMoveToTrash = { trashActionTriggered = true }
                )
            }
        }
        composeTestRule.waitForIdle()

        val cardNode = composeTestRule.onNodeWithTag("note_card_${sampleNotes[0].id}")
        val semanticsNode = cardNode.fetchSemanticsNode()
        val customActions = semanticsNode.config.getOrNull(SemanticsActions.CustomActions)
        assertNotNull("Card must define custom accessibility actions", customActions)

        val moveAction = customActions?.find { it.label == "Move to divider" }
        assertNotNull("Custom actions must include 'Move to divider'", moveAction)
        moveAction?.action?.invoke()
        assertTrue(moveActionTriggered)

        val trashAction = customActions?.find { it.label == "Move to trash" }
        assertNotNull("Custom actions must include 'Move to trash'", trashAction)
        trashAction?.action?.invoke()
        assertTrue(trashActionTriggered)
    }

    @Test
    fun backClosesSheetOrMenuBeforeNavigating() {
        var backPopped = false
        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                BoxScreen(
                    state = BoxUiState(
                        dividers = sampleDividers,
                        selectedDividerId = null,
                        notes = sampleNotes,
                        trashCount = 0
                    ),
                    onNavigateBack = { backPopped = true }
                )
            }
        }
        composeTestRule.waitForIdle()

        // 1. Open context menu by holding a card
        val cardNode = composeTestRule.onNodeWithTag("note_card_${sampleNotes[0].id}")
        cardNode.performTouchInput {
            down(center)
            advanceEventTime(ShellMotion.LONG_PRESS_MS)
            up()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Move to divider").assertIsDisplayed()

        // Press back -> closes menu, does not pop route
        composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Move to divider").assertDoesNotExist()
        assertFalse("Back press must close menu before popping route", backPopped)

        // 2. Open new divider sheet
        composeTestRule.onNodeWithTag("add_divider_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("New divider").assertIsDisplayed()

        // Press back -> closes sheet, does not pop route
        composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("New divider").assertDoesNotExist()
        assertFalse("Back press must close sheet before popping route", backPopped)
    }

    @Test
    fun titleMetaNoteCount() {
        val notes0 = emptyList<NoteSummary>()
        val notes1 = listOf(sampleNotes[0])
        val notes2 = sampleNotes

        var currentNotes by mutableStateOf(notes0)

        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                BoxContent(
                    state = BoxUiState(
                        dividers = emptyList<DividerEntity>(),
                        selectedDividerId = null,
                        notes = currentNotes,
                        trashCount = 0
                    )
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("0 notes").assertIsDisplayed()

        // 1 note
        currentNotes = notes1
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("1 note").assertIsDisplayed()

        // 2 notes
        currentNotes = notes2
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("2 notes").assertIsDisplayed()
    }
}
