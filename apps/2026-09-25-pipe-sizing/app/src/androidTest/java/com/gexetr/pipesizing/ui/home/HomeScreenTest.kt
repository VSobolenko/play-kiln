package com.gexetr.pipesizing.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.gexetr.pipesizing.MainActivity
import com.gexetr.pipesizing.R
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun sizingFormAndRefusalAreVisible() {
        val title = composeRule.activity.getString(R.string.app_name)
        val primaryAction = composeRule.activity.getString(R.string.evaluate_action)

        // The app name now lives in the app-level top bar only, and the calculator is the start tab.
        composeRule.onAllNodesWithText(title)[0].assertIsDisplayed()
        composeRule.onNodeWithText(primaryAction).assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Enter material or system.").assertIsDisplayed()
    }
}
