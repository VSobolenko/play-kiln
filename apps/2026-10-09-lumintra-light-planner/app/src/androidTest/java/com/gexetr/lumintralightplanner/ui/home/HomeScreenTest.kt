package com.gexetr.lumintralightplanner.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.gexetr.lumintralightplanner.MainActivity
import com.gexetr.lumintralightplanner.R
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun starterContentAndSnackbarAreVisible() {
        val title = composeRule.activity.getString(R.string.app_name)
        val primaryAction = composeRule.activity.getString(R.string.home_primary_action)
        val snackbarMessage = composeRule.activity.getString(R.string.home_snackbar_completed)

        composeRule.onAllNodesWithText(title).assertCountEquals(2)[0].assertIsDisplayed()
        composeRule.onNodeWithText(primaryAction).assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(snackbarMessage)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText(snackbarMessage).assertIsDisplayed()
    }
}
