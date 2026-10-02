package com.gexetr.lawnroundlog.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gexetr.lawnroundlog.BuildConfig
import com.gexetr.lawnroundlog.R
import com.gexetr.lawnroundlog.ui.ScreenHeader
import com.gexetr.lawnroundlog.ui.components.RoundMark

/**
 * About, and the in-app half of the privacy policy Play requires.
 *
 * The store listing carries the policy as a URL; Play additionally requires that a user can read it
 * from inside the app, and prose *about* privacy in an About screen is not the policy. So the URL is
 * declared once as a string resource and opened here — this file both names
 * `R.string.about_privacy_url` and is able to open a URI, which is exactly the pair
 * `scripts/lib/in-app-policy.mjs` measures.
 */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val privacyPolicyUrl = stringResource(R.string.about_privacy_url)

    Column(
        modifier = modifier
            .fillMaxSize()
            // The inset is applied to the scroll viewport, before `verticalScroll`, so the viewport
            // itself ends above the system navigation bar. Placed after the scroll it would only pad
            // content that already scrolls, and the privacy-policy button and its URL at the bottom
            // could never be scrolled clear of the bar.
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(
            title = stringResource(R.string.about_title),
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RoundMark()

            Text(
                text = stringResource(R.string.about_body, stringResource(R.string.app_name)),
                style = MaterialTheme.typography.bodyLarge,
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(
                    onClick = {
                        // A device with no browser — a stripped emulator image, a work profile that
                        // blocks the intent — would otherwise take the screen down with an
                        // ActivityNotFoundException. The URL is printed below either way, so the
                        // policy stays reachable by hand.
                        runCatching { uriHandler.openUri(privacyPolicyUrl) }
                    },
                ) {
                    Text(stringResource(R.string.about_privacy_policy))
                }
                Text(
                    text = privacyPolicyUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = stringResource(R.string.about_contact),
                style = MaterialTheme.typography.bodyMedium,
            )

            Text(
                text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The destination the shell registers.
 *
 * It exists as its own name because every other destination has a `*Route` and the navigation host
 * reads one per screen: a bare `AboutScreen` in the `entryProvider` would be the one entry with a
 * different shape, and there is nowhere here for a ViewModel to live — this screen is a pure function
 * of the strings it shows.
 */
@Composable
fun AboutRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AboutScreen(onBack = onBack, modifier = modifier)
}
