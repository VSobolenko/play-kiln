package com.gexetr.teachergradebook.ui.classes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gexetr.teachergradebook.domain.EditResult
import com.gexetr.teachergradebook.domain.FreeTier
import com.gexetr.teachergradebook.domain.RecordBookEditor
import com.gexetr.teachergradebook.domain.model.RecordBook
import com.gexetr.teachergradebook.ui.recordbook.RecordBookUiState
import com.gexetr.teachergradebook.ui.theme.PlayKilnTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The class list, on a real device.
 *
 * These tests exist because the interesting failures are the ones a JVM test cannot see: whether the
 * first-run screen actually renders, whether a dialog's confirm button is reachable, and whether the
 * text a teacher types reaches the callback unchanged. They run on the connected device rather than
 * an emulator, so they also prove the app installs and renders on a real Android 11 phone.
 */
@RunWith(AndroidJUnit4::class)
class ClassesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var added: String? = null
    private var renamed: Pair<String, String>? = null
    private var deleted: String? = null

    private fun show(state: RecordBookUiState, unlocked: Boolean = false) {
        composeRule.setContent {
            PlayKilnTheme {
                ClassesScreen(
                    state = state,
                    unlocked = unlocked,
                    onOpenClass = {},
                    onOpenAbout = {},
                    onShowUnlock = {},
                    onAddClass = { added = it },
                    onRenameClass = { id, name -> renamed = id to name },
                    onDeleteClass = { deleted = it },
                )
            }
        }
    }

    private fun bookWithClass(name: String): RecordBook =
        (RecordBookEditor.addClass(RecordBook(), "c1", name) as EditResult.Ok).book

    @Test
    fun theFirstRunExplainsWhatToDo() {
        show(RecordBookUiState(loaded = true))

        composeRule.onNodeWithText("No classes yet").assertIsDisplayed()
        composeRule.onNodeWithText("New class").assertIsDisplayed()
    }

    @Test
    fun aTypedClassNameReachesTheCallback() {
        show(RecordBookUiState(loaded = true))

        composeRule.onNodeWithText("New class").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("Physics 2")
        composeRule.onNodeWithText("Create").performClick()
        composeRule.waitForIdle()

        assertEquals("Physics 2", added)
    }

    @Test
    fun cancellingAClassDialogReportsNothing() {
        show(RecordBookUiState(loaded = true))

        composeRule.onNodeWithText("New class").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("Discarded")
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitForIdle()

        assertNull("a cancelled dialog must not create anything", added)
    }

    @Test
    fun anExistingClassIsListedWithItsRosterAndAverageState() {
        show(RecordBookUiState(book = bookWithClass("Physics"), loaded = true), unlocked = true)

        composeRule.onNodeWithText("Physics").assertIsDisplayed()
        composeRule.onNodeWithText("0 students").assertIsDisplayed()
        composeRule.onNodeWithText("0 assignments").assertIsDisplayed()
        composeRule.onNodeWithText("Class average: not enough data yet").assertIsDisplayed()
    }

    @Test
    fun theFreeLimitIsStatedWithoutHidingTheUnlock() {
        show(RecordBookUiState(book = bookWithClass("Physics"), loaded = true), unlocked = false)

        composeRule.onNodeWithText("Free version").assertIsDisplayed()
        composeRule
            .onNodeWithText("One class with up to ${FreeTier.MAX_STUDENTS_PER_CLASS} students, unlimited grades, attendance, notes and seating. You have your class, so a second one needs the unlock.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("See what the unlock includes").assertIsDisplayed()
    }

    @Test
    fun deletingAClassAsksFirstAndNamesWhatGoes() {
        show(RecordBookUiState(book = bookWithClass("Physics"), loaded = true), unlocked = true)

        composeRule.onNodeWithText("Options").performClick()
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Delete Physics?").assertIsDisplayed()
        composeRule.onNodeWithText("Delete the class").assertIsDisplayed()

        composeRule.onNodeWithText("Delete the class").performClick()
        composeRule.waitForIdle()

        assertEquals("c1", deleted)
    }

    @Test
    fun keepingAClassReportsNoDeletion() {
        show(RecordBookUiState(book = bookWithClass("Physics"), loaded = true), unlocked = true)

        composeRule.onNodeWithText("Options").performClick()
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Keep it").performClick()
        composeRule.waitForIdle()

        assertNull("a cancelled deletion must remove nothing", deleted)
    }
}
