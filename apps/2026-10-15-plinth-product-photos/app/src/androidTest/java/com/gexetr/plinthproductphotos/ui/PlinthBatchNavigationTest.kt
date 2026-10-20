package com.gexetr.plinthproductphotos.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.gexetr.plinthproductphotos.ui.components.PlinthBottomNavBar
import com.gexetr.plinthproductphotos.ui.components.PlinthNavigationRail
import com.gexetr.plinthproductphotos.ui.components.PlinthSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

class PlinthBatchNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()
    private lateinit var context: android.content.Context

    @Test
    fun bottomBarShowsFiveEqualSlotsAndRoutesProfileSelection() {
        val selectedSection = mutableStateOf(PlinthSection.STUDIO.name)
        var createOpened = false
        composeRule.setContent {
            context = LocalContext.current
            MaterialTheme {
                PlinthBottomNavBar(
                    selected = PlinthSection.STUDIO,
                    onSelect = { selectedSection.value = it.name },
                    onCreate = { createOpened = true },
                )
            }
        }

        val labels = listOf(
            localizedString("shell_studio"),
            localizedString("shell_library"),
            localizedString("shell_create_label"),
            localizedString("shell_profile"),
            localizedString("shell_more"),
        )
        assertAlignedLabels(labels, evenlySpaced = true)

        composeRule.onNodeWithText(labels[3]).performClick()
        composeRule.runOnIdle { assertEquals("PROFILE", selectedSection.value) }
        composeRule.onNodeWithText(labels[2]).performClick()
        composeRule.runOnIdle { assertTrue(createOpened) }
    }

    @Test
    fun railShowsProfileAndCreateAsAlignedSectionEntries() {
        val selectedSection = mutableStateOf(PlinthSection.STUDIO.name)
        var createOpened = false
        composeRule.setContent {
            context = LocalContext.current
            MaterialTheme {
                Row(Modifier.fillMaxSize()) {
                    PlinthNavigationRail(
                        selected = PlinthSection.STUDIO,
                        onSelect = { selectedSection.value = it.name },
                        onCreate = { createOpened = true },
                        modifier = Modifier.fillMaxHeight(),
                    )
                }
            }
        }

        val labels = listOf(
            localizedString("shell_studio"),
            localizedString("shell_library"),
            localizedString("shell_create_label"),
            localizedString("shell_profile"),
            localizedString("shell_more"),
        )
        assertAlignedLabels(labels, evenlySpaced = false)

        composeRule.onNodeWithText(labels[3]).performClick()
        composeRule.runOnIdle { assertEquals("PROFILE", selectedSection.value) }
        composeRule.onNodeWithText(labels[2]).performClick()
        composeRule.runOnIdle { assertTrue(createOpened) }
    }

    @Test
    fun selectingBatchFromLibraryOpensTheBatchProjectPickerHub() {
        composeRule.setContent {
            context = LocalContext.current
            MaterialTheme {
                PlayKilnApp()
            }
        }

        val batchLabel = localizedString("shell_batch")
        composeRule.onNodeWithText(batchLabel).assertDoesNotExist()
        composeRule.onNodeWithText(localizedString("shell_library")).performClick()
        composeRule.onNodeWithText(batchLabel).performClick()
        composeRule.onNodeWithText(localizedString("shell_batch_choose_project")).assertIsDisplayed()
    }

    private fun localizedString(name: String): String {
        val id = context.resources.getIdentifier(name, "string", context.packageName)
        assertTrue("Missing string resource $name", id != 0)
        return context.getString(id)
    }

    private fun assertAlignedLabels(labels: List<String>, evenlySpaced: Boolean) {
        val bounds: List<Rect> = labels.map { label ->
            val node = composeRule.onNodeWithText(label).assertIsDisplayed().fetchSemanticsNode()
            node.boundsInRoot
        }
        if (evenlySpaced) {
            val yCenters = bounds.map { it.center.y }
            assertTrue(
                "Bottom navigation labels should share a baseline: $yCenters",
                yCenters.all { abs(it - yCenters.first()) <= 1.5f },
            )
            val xCenters = bounds.map { it.center.x }
            val gaps = xCenters.zipWithNext { left, right -> right - left }
            assertTrue(
                "Bottom navigation slots should have equal centers: $xCenters",
                gaps.all { abs(it - gaps.first()) <= 1.5f },
            )
        } else {
            val yCenters = bounds.map { it.center.y }
            val gaps = yCenters.zipWithNext { top, bottom -> bottom - top }
            assertTrue(
                "Rail entries should be evenly aligned in order: $yCenters",
                gaps.all { abs(it - gaps.first()) <= 1.5f },
            )
        }
    }
}
