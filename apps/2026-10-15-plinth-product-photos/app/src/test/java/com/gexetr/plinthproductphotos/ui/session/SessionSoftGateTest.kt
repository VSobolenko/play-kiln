package com.gexetr.plinthproductphotos.ui.session

import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.data.session.SessionConnectionState
import com.gexetr.plinthproductphotos.data.session.SessionFailure
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionSoftGateTest {
    @Test
    fun networkFailureUsesLocalServerCopyOnlyInDebug() {
        val state = SessionConnectionState.Failed(SessionFailure.NETWORK)
        assertEquals(R.string.session_network_debug, sessionFailureMessage(state.reason, debug = true))
        assertEquals(R.string.session_network, sessionFailureMessage(state.reason, debug = false))
    }

    @Test
    fun everyOtherFailureRetainsItsSpecificMessage() {
        val expected = mapOf(
            SessionFailure.CONFIGURATION to R.string.session_configuration,
            SessionFailure.INTEGRITY to R.string.session_integrity,
            SessionFailure.SERVER to R.string.session_server,
            SessionFailure.STORAGE to R.string.session_storage,
        )
        expected.forEach { (failure, message) ->
            assertEquals(message, sessionFailureMessage(failure, debug = true))
            assertEquals(message, sessionFailureMessage(failure, debug = false))
        }
    }

    @Test
    fun appContentIsUnconditionalSoFailedNetworkCannotHideLocalBrowsing() {
        val source = mainSource("MainActivity.kt")
        val themedContent = source.substringAfter("darkTheme = settings.themeMode.isDark(systemInDarkTheme),")
        val appContent = themedContent.substringBefore("SessionStatusBanner(")
        assertTrue(appContent.contains("PlayKilnApp(hapticsEnabled = settings.hapticsEnabled)"))
        assertFalse(appContent.contains("if ("))
        assertFalse(appContent.contains("when ("))
        assertFalse(source.contains("SessionConnectionScreen("))
        assertTrue(source.contains("sessionViewModel.createServerSession(integrity)"))
    }

    @Test
    fun localLibraryAndSettingsDestinationsDoNotRequireSession() {
        val source = mainSource("ui/PlayKilnApp.kt")
        assertTrue(source.contains("LibraryScreen("))
        assertTrue(source.contains("SettingsRoute("))
        assertFalse(source.contains("SessionConnectionState.Ready"))
    }

    private fun mainSource(relativePath: String): String {
        val slot = generateSequence(File(requireNotNull(System.getProperty("user.dir")))) { it.parentFile }
            .first { File(it, "economics.json").isFile }
        return File(
            slot,
            "app/src/main/java/com/gexetr/plinthproductphotos/$relativePath",
        ).readText()
    }
}
