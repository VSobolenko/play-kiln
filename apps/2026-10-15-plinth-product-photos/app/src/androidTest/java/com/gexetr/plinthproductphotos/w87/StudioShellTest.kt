package com.gexetr.plinthproductphotos.w87

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.data.api.OperationId
import com.gexetr.plinthproductphotos.ui.studio.StudioToolGrid
import com.gexetr.plinthproductphotos.ui.studio.StudioToolRow
import com.gexetr.plinthproductphotos.ui.studio.StudioWorkflowPanel
import com.gexetr.plinthproductphotos.ui.studio.studioPromptIsValid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StudioShellTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun eachCatalogueCardSelectsItsOwnToolWithoutChooseToolCta() {
        var selected: OperationId? = null
        val tools = listOf(
            StudioToolRow(
                OperationId.GENERATED_SCENE,
                R.string.studio_live_scene,
                R.string.studio_live_purpose_scene,
                R.drawable.ic_lucide_image,
                8,
            ),
            StudioToolRow(
                OperationId.RELIGHT,
                R.string.studio_operation_relight,
                R.string.w6_studio_purpose_relight,
                R.drawable.ic_lucide_camera,
                5,
            ),
        )
        composeRule.setContent {
            MaterialTheme {
                StudioToolGrid(tools = tools, onSelect = { selected = it })
            }
        }

        val sceneBounds = composeRule.onNodeWithTag("studio-tool-GENERATED_SCENE")
            .fetchSemanticsNode().boundsInRoot
        val relightBounds = composeRule.onNodeWithTag("studio-tool-RELIGHT")
            .fetchSemanticsNode().boundsInRoot
        assertEquals(sceneBounds.width, relightBounds.width, 0.5f)
        assertTrue(sceneBounds.center.x != relightBounds.center.x)
        composeRule.onNodeWithTag("studio-tool-RELIGHT").performClick()
        composeRule.runOnIdle { assertEquals(OperationId.RELIGHT, selected) }
        composeRule.onAllNodesWithText(composeRule.activity.getString(R.string.studio_live_choose_tool))
            .assertCountEquals(0)
        val relightCredits = composeRule.activity.resources.getQuantityString(R.plurals.studio_live_credit_amount, 5, 5)
        composeRule.onAllNodesWithText(
            composeRule.activity.getString(R.string.studio_live_action_relight, relightCredits),
        ).assertCountEquals(0)
        composeRule.onAllNodesWithText(composeRule.activity.getString(R.string.studio_operation_relight))
            .assertCountEquals(1)
    }

    @Test
    fun sceneSuggestionsFillTheRequiredFreeformPrompt() {
        var generated = 0
        composeRule.setContent {
            MaterialTheme {
                var prompt by remember { mutableStateOf("") }
                StudioWorkflowPanel(
                    photoImage = null,
                    photoName = "Blue ceramic mug",
                    operation = OperationId.GENERATED_SCENE,
                    operationName = "Generated scene",
                    costCredits = 8,
                    balance = 20,
                    instruction = prompt,
                    onInstructionChange = { prompt = it },
                    promptRequired = true,
                    enabled = studioPromptIsValid(OperationId.GENERATED_SCENE, prompt),
                    waiting = false,
                    onGenerate = { generated++ },
                    onOpenWallet = {},
                    onChangePhoto = {},
                )
            }
        }

        val credits = composeRule.activity.resources.getQuantityString(R.plurals.studio_live_credit_amount, 8, 8)
        val actionLabel = composeRule.activity.getString(R.string.studio_live_action_scene, credits)
        val campsite = composeRule.activity.getString(R.string.studio_live_scene_suggestion_campsite)
        composeRule.onNodeWithText(actionLabel).assertIsNotEnabled()
        composeRule.onNodeWithText(campsite).performClick()
        composeRule.onNodeWithTag("studio-prompt-input").assertTextContains(campsite)
        composeRule.onNodeWithText(actionLabel).assertIsEnabled().performClick()
        composeRule.runOnIdle { assertEquals(1, generated) }
    }
}
