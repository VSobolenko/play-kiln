package com.gexetr.plinthproductphotos.w83

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.data.db.ProjectEntity
import com.gexetr.plinthproductphotos.data.db.ProjectSummary
import com.gexetr.plinthproductphotos.ui.library.ProjectTile
import com.gexetr.plinthproductphotos.ui.profile.ProfileHistoryCandidate
import com.gexetr.plinthproductphotos.ui.profile.ProfileHistoryStatus
import com.gexetr.plinthproductphotos.ui.profile.ProfileScreen
import com.gexetr.plinthproductphotos.ui.profile.ProfileUiState
import com.gexetr.plinthproductphotos.ui.profile.recentProfileResults
import com.gexetr.plinthproductphotos.ui.studio.StudioPhase
import com.gexetr.plinthproductphotos.ui.studio.StudioState
import com.gexetr.plinthproductphotos.ui.studio.StudioToolGrid
import com.gexetr.plinthproductphotos.ui.studio.StudioToolRow
import com.gexetr.plinthproductphotos.ui.studio.StudioWorkflowPanel
import com.gexetr.plinthproductphotos.ui.studio.studioConfirmationPreview
import com.gexetr.plinthproductphotos.data.api.OperationId
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class StudioEnablementAndLabelsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun relightCanBeChosenAndGeneratedAtTenCreditsAfterPreviousDelivery() {
        var generated = 0
        val state = StudioState(
            photoId = "source",
            balance = 10,
            hasPending = true,
            phase = StudioPhase.DONE,
            deliveryConfirmed = true,
        )
        composeRule.setContent {
            MaterialTheme {
                var selected by remember { mutableStateOf(false) }
                if (!selected) {
                    StudioToolGrid(
                        tools = listOf(
                            StudioToolRow(
                                operation = OperationId.RELIGHT,
                                name = R.string.studio_operation_relight,
                                purpose = R.string.w6_studio_purpose_relight,
                                icon = R.drawable.ic_lucide_camera,
                                credits = 5,
                            ),
                        ),
                        enabled = state.canChooseTool,
                        onSelect = { selected = true },
                    )
                } else {
                    val eligibility = studioConfirmationPreview(
                        displayedCredits = 5,
                        currentCredits = 5,
                        balance = state.balance,
                        operationEnabled = true,
                        killSwitchClear = true,
                        dailyCapClear = true,
                        rowBlocked = false,
                        providerReady = true,
                        hasPhoto = state.photoId != null,
                        promptValid = true,
                        busy = state.busy,
                        hasPending = state.blocksNewOperation,
                    )
                    StudioWorkflowPanel(
                        photoImage = null,
                        photoName = "Blue ceramic mug",
                        operation = OperationId.RELIGHT,
                        operationName = "Relight",
                        costCredits = 5,
                        balance = state.balance,
                        instruction = "",
                        onInstructionChange = {},
                        promptRequired = false,
                        enabled = eligibility.canPressGenerate,
                        waiting = false,
                        onGenerate = { generated++ },
                        onOpenWallet = {},
                        onChangePhoto = {},
                        photoChangeEnabled = true,
                    )
                }
            }
        }
        composeRule.onNodeWithTag("studio-tool-RELIGHT").assertIsEnabled().performClick()
        val cost = composeRule.activity.resources.getQuantityString(R.plurals.studio_live_credit_amount, 5, 5)
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.studio_live_using_photo))
            .assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.studio_live_action_relight, cost))
            .assertIsEnabled().performClick()
        composeRule.runOnIdle { assertEquals(1, generated) }
    }

    @Test
    fun profileRecentResultDoesNotExposeImportedUuidInTextOrImageDescription() {
        val file = File.createTempFile("w83-result", ".png", composeRule.activity.cacheDir)
        try {
            val results = recentProfileResults(
                listOf(ProfileHistoryCandidate(UUID_FILENAME, file.path, 1_792_200_000_000L)),
            )
            composeRule.setContent {
                MaterialTheme {
                    ProfileScreen(
                        state = ProfileUiState(history = results, historyStatus = ProfileHistoryStatus.READY),
                        onOpenWallet = {},
                        onOpenLibrary = {},
                        onRefresh = {},
                    )
                }
            }
            composeRule.onNodeWithText(results.single().title).assertExists()
            assertNoUuid()
        } finally {
            file.delete()
        }
    }

    @Test
    fun libraryTileAndSelectionDescriptionHideLegacyUuidTitle() {
        composeRule.setContent {
            MaterialTheme {
                Column {
                    ProjectTile(
                        project = ProjectSummary(
                            project = ProjectEntity(
                                id = "project",
                                name = UUID_FILENAME,
                                createdAt = 1_792_200_000_000L,
                                updatedAt = 1_792_200_000_000L,
                                previewPath = null,
                                category = null,
                                marketplaceId = null,
                                tags = null,
                            ),
                            itemCount = 1,
                            primarySku = null,
                        ),
                        selected = true,
                        selectionMode = true,
                        onToggle = {},
                        onOpen = {},
                    )
                }
            }
        }
        assertNoUuid()
    }

    private fun assertNoUuid() {
        composeRule.onAllNodes(
            hasText("a62f12d2", substring = true) or hasContentDescription("a62f12d2", substring = true),
            useUnmergedTree = true,
        ).assertCountEquals(0)
    }

    companion object {
        private const val UUID_FILENAME = "a62f12d2-f9fc-43cd-bcd4-55de26fd5162.jpg"
    }
}
