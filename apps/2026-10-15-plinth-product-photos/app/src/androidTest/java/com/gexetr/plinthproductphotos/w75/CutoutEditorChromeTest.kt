package com.gexetr.plinthproductphotos.w75

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.ui.cutout.CutoutPhase
import com.gexetr.plinthproductphotos.ui.cutout.CutoutScreen
import com.gexetr.plinthproductphotos.ui.cutout.CutoutUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CutoutEditorChromeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun resultEditorUsesIconActionsAndOpensZoomViewer() {
        val clicks = mutableListOf<String>()
        val image = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
        composeRule.setContent {
            MaterialTheme {
                CutoutScreen(
                    state = CutoutUiState(
                        phase = CutoutPhase.SAVED,
                        selectedName = "Product",
                        sourceBitmap = image,
                        maskOverlay = image,
                        resultBitmap = image,
                        canUndo = true,
                        canRedo = true,
                    ),
                    onChoosePhoto = {},
                    onRefreshWallet = {},
                    onStartCutout = {},
                    onSetBrushMode = {},
                    onBrushStroke = {},
                    onUndo = { clicks += "undo" },
                    onRedo = { clicks += "redo" },
                    onDismissWallet = {},
                )
            }
        }

        val undo = composeRule.activityString(R.string.cutout_undo)
        val redo = composeRule.activityString(R.string.cutout_redo)
        val zoom = composeRule.activityString(R.string.cutout_zoom)

        composeRule.onNodeWithContentDescription(undo).assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription(redo).assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription(zoom).assertIsDisplayed().performClick()
        composeRule.onNodeWithText(composeRule.activityString(R.string.cutout_zoom_title))
            .assertIsDisplayed()

        assertEquals(listOf("undo", "redo"), clicks)
    }

    @Test
    fun historyActionsAreDisabledWhileMaskRevisionIsSaving() {
        val image = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
        composeRule.setContent {
            MaterialTheme {
                CutoutScreen(
                    state = CutoutUiState(
                        phase = CutoutPhase.SAVED,
                        selectedName = "Product",
                        sourceBitmap = image,
                        maskOverlay = image,
                        resultBitmap = image,
                        isBusy = true,
                        isSavingMask = true,
                        canUndo = true,
                        canRedo = true,
                    ),
                    onChoosePhoto = {},
                    onRefreshWallet = {},
                    onStartCutout = {},
                    onSetBrushMode = {},
                    onBrushStroke = {},
                    onUndo = {},
                    onRedo = {},
                    onDismissWallet = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription(composeRule.activityString(R.string.cutout_undo))
            .assertIsNotEnabled()
        composeRule.onNodeWithContentDescription(composeRule.activityString(R.string.cutout_redo))
            .assertIsNotEnabled()
        composeRule.onNodeWithText(composeRule.activityString(R.string.cutout_mask_saving))
            .assertIsDisplayed()
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.activityString(id: Int): String =
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .targetContext.getString(id)
}
