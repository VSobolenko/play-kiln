package com.gexetr.plinthproductphotos.ui.editor

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.ui.theme.PlayKilnTheme
import org.junit.Rule
import org.junit.Test

class EditorToolDockTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun backgroundIsSelectedOnEntryAndSaveRemainsAvailable() {
        composeRule.setContent {
            PlayKilnTheme {
                EditorScreen(
                    state = editorState(),
                    onBack = {},
                    onSaved = {},
                    onSave = {},
                    onPreviewSettingsChange = {},
                    onClearPreview = {},
                    onSettingsChange = { _, _ -> },
                    onShadowChange = {},
                    onUndo = {},
                    onRedo = {},
                )
            }
        }

        composeRule.onNodeWithText(activityString(R.string.plinth_settings_paper))
            .assertIsDisplayed()
        composeRule.onNodeWithText(activityString(R.string.plinth_background_section))
            .assertIsSelected()
        composeRule.onNodeWithText(activityString(R.string.editor_save_card))
            .assertIsDisplayed()
    }

    @Test
    fun selectingShadowHidesBackgroundAndBackgroundRestoresItsSwatches() {
        composeRule.setContent {
            PlayKilnTheme {
                EditorScreen(
                    state = editorState(),
                    onBack = {},
                    onSaved = {},
                    onSave = {},
                    onPreviewSettingsChange = {},
                    onClearPreview = {},
                    onSettingsChange = { _, _ -> },
                    onShadowChange = {},
                    onUndo = {},
                    onRedo = {},
                )
            }
        }

        val paper = activityString(R.string.plinth_settings_paper)
        composeRule.onNodeWithText(paper).assertIsDisplayed()
        composeRule.onNodeWithText(activityString(R.string.plinth_shadow_section))
            .performClick()
        composeRule.onNodeWithText(activityString(R.string.plinth_shadow_none)).assertIsDisplayed()
        composeRule.onNodeWithText(activityString(R.string.plinth_shadow_contact)).assertIsDisplayed()
        composeRule.onNodeWithText(activityString(R.string.plinth_shadow_drop)).assertIsDisplayed()
        composeRule.onNodeWithText(paper).assertDoesNotExist()
        composeRule.onNodeWithText(activityString(R.string.editor_save_card)).assertIsDisplayed()

        composeRule.onNodeWithText(activityString(R.string.plinth_background_section))
            .performClick()
        composeRule.onNodeWithText(paper).assertIsDisplayed()
    }

    private fun editorState(): EditorUiState {
        val image = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
        return EditorUiState(
            isLoading = false,
            photoName = "test-photo",
            settings = CompositionSettings(background = solidBackground(AndroidColor.WHITE)),
            original = image,
            composition = image,
        )
    }

    private fun activityString(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
}
