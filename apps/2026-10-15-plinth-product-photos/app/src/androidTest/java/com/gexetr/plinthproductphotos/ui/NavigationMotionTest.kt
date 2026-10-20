package com.gexetr.plinthproductphotos.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import org.junit.Rule
import org.junit.Test

class NavigationMotionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun sectionReplacementSettlesWithin220Millis() {
        val stack = mutableStateListOf("studio")
        showNavigation(stack)
        composeRule.runOnIdle { stack[0] = "more" }
        assertSettled("more", "studio")
    }

    @Test
    fun settingsPushAndPopSettleWithin220Millis() {
        val stack = mutableStateListOf("more")
        showNavigation(stack)
        composeRule.runOnIdle { stack.add("settings") }
        assertSettled("settings", "more")
        composeRule.runOnIdle { stack.removeAt(stack.lastIndex) }
        assertSettled("more", "settings")
    }

    @Test
    fun aboutPushAndPopSettleWithin220Millis() {
        val stack = mutableStateListOf("more")
        showNavigation(stack)
        composeRule.runOnIdle { stack.add("about") }
        assertSettled("about", "more")
        composeRule.runOnIdle { stack.removeAt(stack.lastIndex) }
        assertSettled("more", "about")
    }

    @Test
    fun backDuringSettingsEntryDoesNotLeaveAnOutgoingScene() {
        val stack = mutableStateListOf("more")
        showNavigation(stack)
        composeRule.runOnIdle { stack.add("settings") }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(48)
        composeRule.runOnIdle { stack.removeAt(stack.lastIndex) }
        assertSettled("more", "settings")
    }

    @Test
    fun rapidRetargetDoesNotLeaveAnOutgoingScene() {
        val stack = mutableStateListOf("studio")
        showNavigation(stack)
        composeRule.runOnIdle { stack[0] = "library" }
        composeRule.mainClock.advanceTimeBy(48)
        composeRule.runOnIdle { stack[0] = "more" }
        assertSettled("more", "studio", "library")
    }

    private fun showNavigation(stack: List<String>) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            NavDisplay(
                backStack = stack,
                onBack = {},
                transitionSpec = {
                    snappyNavigationTransition(
                        isPop = false,
                        isSectionSwitch = initialState.entries.last().contentKey in sections &&
                            targetState.entries.last().contentKey in sections,
                        slideDistancePx = 24,
                    )
                },
                popTransitionSpec = {
                    snappyNavigationTransition(isPop = true, slideDistancePx = 24)
                },
                predictivePopTransitionSpec = {
                    snappyNavigationTransition(isPop = true, slideDistancePx = 24)
                },
                entryProvider = { key ->
                    NavEntry(key, contentKey = key) {
                        Box(Modifier.fillMaxSize().testTag(key))
                    }
                },
            )
        }
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun assertSettled(visible: String, vararg removed: String) {
        // Start the transition, then allow the owner's 220 ms ceiling plus frame rounding.
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(220)
        composeRule.onNodeWithTag(visible).assertIsDisplayed()
        removed.forEach { composeRule.onNodeWithTag(it).assertDoesNotExist() }
    }

    private val sections = setOf("studio", "library", "more")
}
