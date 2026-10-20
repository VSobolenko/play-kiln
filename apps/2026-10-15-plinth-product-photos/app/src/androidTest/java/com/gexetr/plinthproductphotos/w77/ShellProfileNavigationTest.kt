package com.gexetr.plinthproductphotos.w77

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.ui.PlayKilnApp
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ShellProfileNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()
    private lateinit var context: Context

    @Test
    fun phoneOpensProfileAndItsWallet() {
        showShell(DpSize(400.dp, 800.dp))
        assertProfileAndWallet()
    }

    @Test
    fun wideRailOpensProfileAndItsWallet() {
        showShell(DpSize(1000.dp, 800.dp))
        assertProfileAndWallet()
    }

    @Test
    fun phoneCreateOpensW74PhotoImport() {
        showShell(DpSize(400.dp, 800.dp))
        assertCreateImport()
    }

    @Test
    fun wideRailCreateOpensW74PhotoImport() {
        showShell(DpSize(1000.dp, 800.dp))
        assertCreateImport()
    }

    @Test
    fun batchIsSecondaryToLibrary() {
        showShell(DpSize(400.dp, 800.dp))
        composeRule.onNodeWithText(text(R.string.shell_batch)).assertDoesNotExist()
        click(R.string.shell_library)
        click(R.string.shell_batch)
        composeRule.onNodeWithText(text(R.string.shell_batch_choose_project)).assertIsDisplayed()
    }

    @Test
    fun moreOpensSettings() {
        showShell(DpSize(400.dp, 800.dp))
        click(R.string.shell_more)
        click(R.string.plinth_settings_title)
        composeRule.onNodeWithText(text(R.string.plinth_settings_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.profile_w76_credits_title)).assertDoesNotExist()
        composeRule.onNode(hasText(text(R.string.shell_more)) and hasClickAction())
            .assertIsSelected()
    }

    private fun showShell(size: DpSize) {
        composeRule.setContent {
            context = LocalContext.current
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(size)) {
                MaterialTheme { PlayKilnApp() }
            }
        }
    }

    private fun assertProfileAndWallet() {
        composeRule.onNodeWithText(text(R.string.shell_batch)).assertDoesNotExist()
        click(R.string.shell_profile)
        composeRule.onNode(hasText(text(R.string.shell_profile)) and hasClickAction())
            .assertIsSelected()
        composeRule.onNodeWithText(text(R.string.profile_w76_credits_title)).assertIsDisplayed()
        click(R.string.profile_w76_open_wallet)
        composeRule.onNodeWithText(text(R.string.wallet_title)).assertIsDisplayed()
    }

    private fun assertCreateImport() {
        click(R.string.shell_profile)
        click(R.string.shell_create_label)
        composeRule.onNodeWithText(text(R.string.w74_create_heading)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.w74_choose_photo)).assertIsDisplayed()
    }

    private fun click(resource: Int) {
        composeRule.onNode(hasText(text(resource)) and hasClickAction()).performClick()
    }

    private fun text(resource: Int): String = context.getString(resource)
}
