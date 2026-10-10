package com.gexetr.lumintralightplanner.ui.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gexetr.lumintralightplanner.R
import com.gexetr.lumintralightplanner.data.BundledAsset
import com.gexetr.lumintralightplanner.data.SkyAssetSource
import com.gexetr.lumintralightplanner.ui.components.LumintraPanel
import com.gexetr.lumintralightplanner.ui.components.LumintraScaffoldHeader
import com.gexetr.lumintralightplanner.ui.components.SectionStrip
import com.gexetr.lumintralightplanner.ui.theme.LumintraTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The credited assets the probe found packaged, or `false` for each one it did not.
 *
 * `specs/settings-and-credits`: "each row SHALL appear only when the asset it credits actually ships in
 * the build". Presence is measured through [SkyAssetSource], the same door the loader reads through, so
 * a row cannot claim an image the build does not carry — and an absent asset is a quiet capability loss
 * rather than a credit for something that is not there.
 */
@Immutable
private data class ShippedAssets(
    val milkyWay: Boolean,
    val moonImage: Boolean,
    val starTable: Boolean,
    val constellationFigures: Boolean,
) {
    companion object {
        /** Nothing is claimed while the probe is running, and nothing is claimed if it never answers. */
        val NONE: ShippedAssets = ShippedAssets(
            milkyWay = false,
            moonImage = false,
            starTable = false,
            constellationFigures = false,
        )
    }
}

/** One tappable licence address: the words the control carries and the URL it opens. */
@Immutable
private data class CreditLink(val labelRes: Int, val url: String)

/** One attribution row: the obliging line, an optional detail line, and the licence links it names. */
@Immutable
private data class CreditEntry(
    val credit: String,
    val detail: String? = null,
    val links: List<CreditLink> = emptyList(),
)

/**
 * Credits: every licence and attribution obligation the shipped build actually carries, with the About
 * block that states the app's own data practices.
 *
 * ### What is on the surface, and why each part is here
 *
 * - **Data and imagery** — the ESO Milky Way panorama (`ESO/S. Brunier`), the MET Norway forecast, the
 *   NASA SVS Moon imagery, the IAU / Sky & Telescope constellation figures and the FK6 star positions.
 *   Each line is the licence's own required attribution, verbatim, and each is a `translatable="false"`
 *   resource: **a licence credit is not translated**, and no locale tree may replace it. The rows for
 *   the asset-backed credits appear only when that asset is packaged.
 * - **Libraries** — the Play In-App Review library (Play Core SDK Terms of Service, which is not an
 *   open-source licence), Orekit + Hipparchus (Apache-2.0, whose notice files ship under
 *   `assets/licenses/`) and the vendored `astronomy.kt` (MIT). These are compiled in or declared on the
 *   classpath in every build, so their rows are unconditional.
 * - **Modifications** — what the licences require to be said about what this app does to the material
 *   it credits: the coordinate truncation before the forecast request, and the composition of the
 *   backdrop and the postcard from the credited panorama.
 * - **Licence text** — one row per licence address, each with the URL rendered as **selectable text**
 *   beside a tappable control, so a device without a browser still shows the reader where the licence
 *   lives.
 * - **About** — the version as built, the support address, and the data practices in plain language.
 *
 * ### Two rules it does not bend
 *
 * The app is never credited to an endorser: no NASA insignia, no ESO logo and no IAU logo is drawn, and
 * the forecast service is named only as MET Norway (met.no), never by its short trade name. And a link
 * that cannot be opened is **stated**, never silent — the
 * privacy-policy row renders its address as selectable text together with the reason.
 *
 * ### The honest edge
 *
 * The probe reads the packaged assets once, off the main thread, and answers "packaged" — not
 * "correct". A row appearing here means the build carries the asset this file credits; whether a
 * licence holder would accept the placement is a judgement no screen can make
 * (`specs/settings-and-credits`, "Honest boundary").
 */
@Composable
fun CreditsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val assets = remember(context) { SkyAssetSource(context) }
    val shipped by produceState(initialValue = ShippedAssets.NONE, assets) {
        value = withContext(Dispatchers.IO) { probeShippedAssets(assets) }
    }
    val wide = LocalConfiguration.current.screenWidthDp >= 600

    Scaffold(
        topBar = {
            LumintraScaffoldHeader(
                title = stringResource(R.string.credits_title),
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(modifier = Modifier.widthIn(max = CONTENT_MAX_WIDTH)) {
                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Attributions(shipped = shipped, context = context)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Modifications()
                            Spacer(Modifier.height(16.dp))
                            LicenceText(context = context)
                        }
                    }
                } else {
                    Attributions(shipped = shipped, context = context)
                    Spacer(Modifier.height(16.dp))
                    Modifications()
                    Spacer(Modifier.height(16.dp))
                    LicenceText(context = context)
                }
                Spacer(Modifier.height(16.dp))
                About(context = context)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// =================================================================================================
// Data and imagery, libraries
// =================================================================================================

@Composable
private fun Attributions(shipped: ShippedAssets, context: Context) {
    SectionStrip(title = stringResource(R.string.credits_section_data))
    Spacer(Modifier.height(8.dp))

    if (shipped.milkyWay) {
        // The ESO licence requires the credit to accompany the image; this row states it, and the Night
        // scene and the composed postcard render `ESO/S. Brunier` on the image itself.
        CreditRow(
            context = context,
            entry = CreditEntry(
                credit = stringResource(
                    R.string.credit_eso_what,
                    stringResource(R.string.credit_eso_credit),
                ),
                links = listOf(
                    CreditLink(R.string.credit_open_eso, stringResource(R.string.credit_link_eso)),
                    CreditLink(R.string.credit_open_cc_by, stringResource(R.string.credit_link_cc_by)),
                ),
            ),
        )
    }

    CreditRow(
        context = context,
        entry = CreditEntry(
            credit = stringResource(R.string.credit_met_norway),
            detail = stringResource(R.string.credit_met_norway_source),
            links = listOf(
                CreditLink(R.string.credit_open_met, stringResource(R.string.credit_link_met)),
                CreditLink(R.string.credit_open_api_met, stringResource(R.string.credit_link_api_met)),
            ),
        ),
    )

    if (shipped.moonImage) {
        CreditRow(
            context = context,
            entry = CreditEntry(credit = stringResource(R.string.credit_nasa_svs)),
        )
    }

    if (shipped.constellationFigures) {
        CreditRow(
            context = context,
            entry = CreditEntry(
                credit = stringResource(R.string.credit_iau_constellations),
                links = listOf(
                    CreditLink(R.string.credit_open_iau, stringResource(R.string.credit_link_iau)),
                    CreditLink(R.string.credit_open_cc_by, stringResource(R.string.credit_link_cc_by)),
                ),
            ),
        )
    }

    if (shipped.starTable) {
        CreditRow(
            context = context,
            entry = CreditEntry(
                credit = stringResource(R.string.credit_fk6),
                links = listOf(
                    CreditLink(R.string.credit_open_cc0, stringResource(R.string.credit_link_cc0)),
                ),
            ),
        )
    }

    Spacer(Modifier.height(16.dp))
    SectionStrip(title = stringResource(R.string.credits_section_code))
    Spacer(Modifier.height(8.dp))
    CreditRow(context = context, entry = CreditEntry(credit = stringResource(R.string.credit_orekit)))
    CreditRow(context = context, entry = CreditEntry(credit = stringResource(R.string.credit_astronomy_kt)))
    CreditRow(context = context, entry = CreditEntry(credit = stringResource(R.string.credit_play_review)))
}

/**
 * One attribution: the licence's own line, an optional detail line, and the licences it names.
 *
 * Every link is a `TextButton` — a 48 dp target whose accessible name is the licence it opens — so a
 * credit is never reachable by colour or glyph alone, and a row is never a dead end.
 */
@Composable
private fun CreditRow(context: Context, entry: CreditEntry) {
    LumintraPanel(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text(
            text = entry.credit,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        entry.detail?.let { detail ->
            Spacer(Modifier.height(2.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        for (link in entry.links) {
            TextButton(
                onClick = { openUrl(context, link.url) },
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Text(
                    text = stringResource(link.labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// =================================================================================================
// Modifications and licence text
// =================================================================================================

/** What the licences require to be said about what this app does to the material it credits. */
@Composable
private fun Modifications() {
    SectionStrip(title = stringResource(R.string.credits_section_modifications))
    Spacer(Modifier.height(8.dp))
    LumintraPanel(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.credit_mod_met_norway),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.credit_mod_imagery),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * The licence addresses, as **selectable text** beside a tappable control.
 *
 * The text is the durable half: on a device with no browser the reader still sees, and can copy, the
 * address the licence names, which is why the URL is rendered rather than only handed to an intent.
 * The label itself is the control, so the address is never reachable only through an unnamed glyph.
 */
@Composable
private fun LicenceText(context: Context) {
    SectionStrip(title = stringResource(R.string.credits_section_links))
    Spacer(Modifier.height(8.dp))
    LumintraPanel(modifier = Modifier.fillMaxWidth()) {
        for (link in licenceLinks()) {
            TextButton(
                onClick = { openUrl(context, link.url) },
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Text(
                    text = stringResource(link.labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            SelectionContainer {
                Text(
                    text = link.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = LumintraTheme.colors.moonlight,
                )
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

// =================================================================================================
// About
// =================================================================================================

/**
 * The About block: the version as built, the support address, and the data practices stated plainly.
 *
 * The privacy policy is **not** prose on this screen. It is opened outside the app through an external
 * view intent; when no browser can resolve it the address is rendered as selectable text with the
 * reason, and while the address is still the unpublished placeholder the row says there is nothing to
 * open instead of presenting a page as the policy.
 */
@Composable
private fun About(context: Context) {
    SectionStrip(title = stringResource(R.string.credits_about_header))
    Spacer(Modifier.height(8.dp))
    LumintraPanel(modifier = Modifier.fillMaxWidth(), accent = true) {
        Text(
            text = stringResource(
                R.string.credits_about_version,
                stringResource(R.string.credits_version_name),
                stringResource(R.string.credits_version_code),
            ),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.credits_about_contact,
                stringResource(R.string.credits_contact_email),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        AboutParagraph(R.string.credits_about_no_account)
        AboutParagraph(R.string.credits_about_no_location)
        AboutParagraph(R.string.credits_about_forecast)
        AboutParagraph(R.string.credits_about_elements)
        AboutParagraph(R.string.credits_about_no_ads)
        PrivacyRow(context = context)
    }
}

@Composable
private fun AboutParagraph(textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(8.dp))
}

/**
 * The policy row, in three stated states: published and openable, published with no browser, and not
 * yet published. None of the three is a silent failure.
 */
@Composable
private fun PrivacyRow(context: Context) {
    val policyUrl = stringResource(R.string.privacy_policy_url)
    val published = isPublishedPolicy(policyUrl)
    var openFailed by remember(policyUrl) { mutableStateOf(false) }

    if (!published) {
        Text(
            text = stringResource(R.string.credits_privacy),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        SelectableAddress(policyUrl)
        Text(
            text = stringResource(R.string.credits_privacy_unpublished),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    TextButton(
        onClick = { openFailed = !openUrl(context, policyUrl) },
        modifier = Modifier.minimumInteractiveComponentSize(),
    ) {
        Text(
            text = stringResource(R.string.credits_privacy),
            style = MaterialTheme.typography.labelLarge,
        )
    }
    if (openFailed) {
        // The address first: the sentence below says it is shown above, so the two read as one statement.
        SelectableAddress(policyUrl)
        Text(
            text = stringResource(R.string.credits_privacy_failed, policyUrl),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SelectableAddress(url: String) {
    SelectionContainer {
        Text(
            text = url,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// =================================================================================================
// Wiring
// =================================================================================================

/** The six licence addresses the shipped credits name, in the order the licence section lists them. */
@Composable
private fun licenceLinks(): List<CreditLink> = listOf(
    CreditLink(R.string.credit_open_cc_by, stringResource(R.string.credit_link_cc_by)),
    CreditLink(R.string.credit_open_met, stringResource(R.string.credit_link_met)),
    CreditLink(R.string.credit_open_api_met, stringResource(R.string.credit_link_api_met)),
    CreditLink(R.string.credit_open_eso, stringResource(R.string.credit_link_eso)),
    CreditLink(R.string.credit_open_iau, stringResource(R.string.credit_link_iau)),
    CreditLink(R.string.credit_open_cc0, stringResource(R.string.credit_link_cc0)),
)

/**
 * The presence probe: one pass over the packaged assets, on a background thread.
 *
 * It asks [SkyAssetSource] — the typed reader the loader uses — rather than a second path constant, so
 * "does this asset ship" has exactly one answer in this codebase. A reader that throws is treated as
 * absent, which is the honest reading: a row is not evidence that an asset works.
 */
private fun probeShippedAssets(assets: SkyAssetSource): ShippedAssets = runCatching {
    ShippedAssets(
        milkyWay = assets.milkyWayMap() is BundledAsset.Present<*>,
        moonImage = assets.moonImage() is BundledAsset.Present<*>,
        starTable = assets.starTable() is BundledAsset.Present<*>,
        constellationFigures = assets.constellationFigures() is BundledAsset.Present<*>,
    )
}.getOrDefault(ShippedAssets.NONE)

/**
 * True when the declared address is a real public HTTPS policy and not the factory's placeholder.
 *
 * A placeholder is shown as text and never opened, because opening a page that is not the policy would
 * present the policy as read — the same rule `pack.placeholders` enforces at pack validation.
 */
private fun isPublishedPolicy(url: String): Boolean {
    if (!url.startsWith("https://")) return false
    val lowered = url.lowercase()
    return !lowered.contains("placeholder") &&
        !lowered.contains("example.com") &&
        !lowered.contains("localhost")
}

/** Starts an external view intent for [url]; `false` means nothing on the device could receive it. */
private fun openUrl(context: Context, url: String): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    if (context !is Activity) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
        context.startActivity(intent)
        true
    } catch (missing: ActivityNotFoundException) {
        false
    }
}

/** The content column's ceiling: a long attribution line stays readable instead of running the width. */
private val CONTENT_MAX_WIDTH = 760.dp
