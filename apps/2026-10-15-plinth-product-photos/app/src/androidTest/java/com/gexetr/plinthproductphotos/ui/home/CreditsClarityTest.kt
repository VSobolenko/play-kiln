package com.gexetr.plinthproductphotos.ui.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.ui.components.BalanceChip
import com.gexetr.plinthproductphotos.ui.components.CreditsExplanation
import com.gexetr.plinthproductphotos.ui.components.PlinthCostChip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CreditsClarityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun balanceExplainsItsPurposeAndOpensWallet() {
        var walletOpens = 0
        composeRule.setContent {
            BalanceChip(balance = 90, onClick = { walletOpens++ })
        }
        composeRule.onNodeWithText("Credits · 90")
            .assertIsDisplayed()
        val balanceControl = composeRule.onNodeWithContentDescription(
            "Credits · 90. Opens Wallet to top up.",
        )
        val heightPx = balanceControl.fetchSemanticsNode().size.height
        val minimumHeightPx = (48 * composeRule.activity.resources.displayMetrics.density).toInt()
        assertTrue("Balance chip should be at least 48dp tall", heightPx >= minimumHeightPx)
        balanceControl.performClick()
        composeRule.runOnIdle { assertEquals(1, walletOpens) }
    }

    @Test
    fun unavailableBalanceStillOffersWalletWithoutInventingZeroCredits() {
        var walletOpens = 0
        composeRule.setContent {
            BalanceChip(balance = null, onClick = { walletOpens++ })
        }
        composeRule.onNodeWithText("Get credits").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Get credits. Opens Wallet to top up.")
            .performClick()
        composeRule.runOnIdle { assertEquals(1, walletOpens) }
    }

    @Test
    fun actionCostKeepsTheCreditUnitVisible() {
        composeRule.setContent { PlinthCostChip(cost = 1) }
        composeRule.onNodeWithText(
            composeRule.activity.resources.getQuantityString(R.plurals.home_cost_chip, 1, 1),
        ).assertIsDisplayed()
    }

    @Test
    fun explanationOpensWalletAndDismissalSurvivesSavedStateRestoration() {
        var walletOpens = 0
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent { CreditsExplanation(onOpenWallet = { walletOpens++ }) }
        val explanation = composeRule.activity.getString(R.string.shell_credits_explanation)
        composeRule.onNodeWithText(explanation).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.shell_credits_open_wallet))
            .performClick()
        composeRule.runOnIdle { assertEquals(1, walletOpens) }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.shell_credits_dismiss))
            .performClick()
        composeRule.onNodeWithText(explanation).assertDoesNotExist()
        restoration.emulateSavedInstanceStateRestore()
        composeRule.onNodeWithText(explanation).assertDoesNotExist()
    }
}
