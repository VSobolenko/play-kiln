package com.gexetr.plinthproductphotos.w78

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.ui.studio.StudioBeforeAfterComparison
import com.gexetr.plinthproductphotos.ui.studio.StudioOperationActionButton
import com.gexetr.plinthproductphotos.ui.studio.StudioWorkflowPanel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StudioWorkflowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun sufficientBalanceShowsPhotoOptionalInstructionAndCostOnGenerate() {
        var generated = 0
        composeRule.setContent {
            MaterialTheme {
                StudioWorkflowPanel(
                    photoImage = null,
                    photoName = "Blue ceramic mug",
                    operationName = "Relight",
                    costCredits = 3,
                    balance = 10,
                    instruction = "",
                    onInstructionChange = {},
                    enabled = true,
                    waiting = false,
                    onGenerate = { generated++ },
                    onOpenWallet = {},
                    onChangeTool = {},
                )
            }
        }

        composeRule.onNodeWithText("Blue ceramic mug").assertIsDisplayed()
        composeRule.onNodeWithText("Relight").assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.studio_live_prompt))
            .assertIsDisplayed()
        val creditAmount = { count: Int ->
            composeRule.activity.resources.getQuantityString(R.plurals.studio_live_credit_amount, count, count)
        }
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.studio_live_action_relight, creditAmount(3)),
        ).assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(1, generated) }
        assertEquals(
            0,
            composeRule.onAllNodesWithText(composeRule.activity.getString(R.string.studio_live_insufficient_title))
                .fetchSemanticsNodes().size,
        )
    }

    @Test
    fun insufficientBalanceExplainsShortfallOnlyAfterGenerateAndOffersWallet() {
        var generated = 0
        var walletOpened = 0
        composeRule.setContent {
            MaterialTheme {
                StudioWorkflowPanel(
                    photoImage = null,
                    photoName = "Blue ceramic mug",
                    operationName = "Relight",
                    costCredits = 3,
                    balance = 1,
                    instruction = "",
                    onInstructionChange = {},
                    enabled = true,
                    waiting = false,
                    onGenerate = { generated++ },
                    onOpenWallet = { walletOpened++ },
                    onChangeTool = {},
                )
            }
        }

        val creditAmount = { count: Int ->
            composeRule.activity.resources.getQuantityString(R.plurals.studio_live_credit_amount, count, count)
        }
        val actionLabel = composeRule.activity.getString(R.string.studio_live_action_relight, creditAmount(3))
        assertEquals(
            0,
            composeRule.onAllNodesWithText(composeRule.activity.getString(R.string.studio_live_insufficient_title))
                .fetchSemanticsNodes().size,
        )
        composeRule.onNodeWithText(actionLabel).assertIsDisplayed().performClick()
        composeRule.onNodeWithText(
            composeRule.activity.getString(
                R.string.studio_live_insufficient_message,
                creditAmount(3),
                creditAmount(1),
                creditAmount(2),
            ),
        )
            .assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.w6_studio_top_up))
            .assertIsDisplayed().performClick()
        composeRule.runOnIdle {
            assertEquals(0, generated)
            assertEquals(1, walletOpened)
        }
    }

    @Test
    fun generationUsesOneCustomerFacingProgressIndicator() {
        composeRule.setContent {
            MaterialTheme {
                StudioWorkflowPanel(
                    photoImage = null,
                    photoName = "Blue ceramic mug",
                    operationName = "Relight",
                    costCredits = 3,
                    balance = 10,
                    instruction = "",
                    onInstructionChange = {},
                    enabled = false,
                    waiting = true,
                    onGenerate = {},
                    onOpenWallet = {},
                    onChangeTool = {},
                )
            }
        }

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.studio_live_generating))
            .assertIsDisplayed()
        assertEquals(
            1,
            composeRule.onAllNodesWithContentDescription(
                composeRule.activity.getString(R.string.studio_live_generating),
            ).fetchSemanticsNodes().size,
        )
        assertEquals(0, composeRule.onAllNodesWithText("QUEUED").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("RUNNING").fetchSemanticsNodes().size)
    }

    @Test
    fun deliveredPreviewKeepsSourceAndResultSideBySide() {
        composeRule.setContent {
            MaterialTheme {
                StudioBeforeAfterComparison(
                    before = { Text("Source photo") },
                    after = { Text("Generated result") },
                )
            }
        }

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.studio_live_before))
            .assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.studio_live_after))
            .assertIsDisplayed()
        val source = composeRule.onNodeWithText("Source photo").assertIsDisplayed().fetchSemanticsNode()
        val result = composeRule.onNodeWithText("Generated result").assertIsDisplayed().fetchSemanticsNode()
        assertTrue("The generated result should sit beside its source", source.boundsInRoot.center.x < result.boundsInRoot.center.x)
    }

    @Test
    fun choosingAStudioToolDoesNotStartGeneration() {
        var chosen = 0
        composeRule.setContent {
            MaterialTheme {
                StudioOperationActionButton(
                    primary = false,
                    enabled = true,
                    onClick = { chosen++ },
                )
            }
        }

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.studio_live_choose_tool))
            .performClick()
        composeRule.runOnIdle { assertEquals(1, chosen) }
    }

    @Test
    fun optionalGuidanceIsLimitedToAShortInstruction() {
        val instruction = mutableStateOf("")
        composeRule.setContent {
            MaterialTheme {
                StudioWorkflowPanel(
                    photoImage = null,
                    photoName = "Blue ceramic mug",
                    operationName = "Relight",
                    costCredits = 3,
                    balance = 10,
                    instruction = instruction.value,
                    onInstructionChange = { instruction.value = it },
                    enabled = true,
                    waiting = false,
                    onGenerate = {},
                    onOpenWallet = {},
                    onChangeTool = {},
                )
            }
        }

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.studio_live_prompt))
            .performTextInput("x".repeat(241))
        composeRule.runOnIdle { assertEquals(240, instruction.value.length) }
    }
}
