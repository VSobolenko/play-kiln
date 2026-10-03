package com.gexetr.lawnroundlog.ui.unlock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gexetr.lawnroundlog.R
import com.gexetr.lawnroundlog.ui.ScreenHeader
import com.gexetr.lawnroundlog.ui.components.RoundMark
import com.gexetr.lawnroundlog.ui.round.FREE_TIER_CUSTOMER_LIMIT

/**
 * The single unlock: one price, no trial clock, and a restore path.
 *
 * The screen is deliberately the smallest surface in the app. It has one product, it states one
 * price, and it never counts down: the free tier is a number of customers rather than a number of
 * days, so there is nothing to expire and nothing to insist on. A purchase that is already owned is
 * stated as owned instead of being sold again.
 */
@Composable
fun UnlockScreen(
    gate: PurchaseGate,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: UnlockViewModel = viewModel(factory = UnlockViewModel.factory(gate))
    val owned by viewModel.owned.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()

    // Connecting is how this screen learns what the operator already owns, and it is idempotent, so
    // returning to the screen does not start a second billing client.
    LaunchedEffect(gate) {
        viewModel.connect()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            // The inset is applied to the scroll viewport, before `verticalScroll`, so the viewport
            // itself ends above the system navigation bar. Placed after the scroll it would only pad
            // content that already scrolls, and the purchase button, the restore text button and the
            // failure message at the bottom — the revenue surface of the slot — could never be
            // scrolled clear of the bar.
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(
            title = stringResource(R.string.unlock_title),
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
                text = stringResource(R.string.unlock_body, FREE_TIER_CUSTOMER_LIMIT),
                style = MaterialTheme.typography.bodyLarge,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (line in UNLOCK_BULLETS) {
                    Text(
                        text = stringResource(line),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            PricePanel(
                owned = owned,
                // The price is a resource rather than a value read back from Play: the capability
                // this slot is built against exposes entitlements and the purchase flow and no
                // product details, so there is no price surface to read. It is one number in one
                // file, and the store listing quotes the same one.
                price = stringResource(R.string.unlock_price),
                priceNote = stringResource(R.string.unlock_price_note),
            )

            if (!owned) {
                val busy = status == UnlockStatus.BUSY || status == UnlockStatus.CONNECTING

                Button(
                    onClick = { viewModel.purchase() },
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) {
                    Text(stringResource(R.string.unlock_action))
                }

                TextButton(
                    onClick = { viewModel.restore() },
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) {
                    Text(stringResource(R.string.unlock_restore))
                }
            }

            if (status == UnlockStatus.BUSY || status == UnlockStatus.CONNECTING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.unlock_busy),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            // The failure message is a state rather than a toast: it stays on screen until it is
            // read or another attempt starts, because a purchase that did not complete is the one
            // thing here the operator must not miss.
            val message = when (status) {
                UnlockStatus.FAILED -> R.string.unlock_failed
                UnlockStatus.UNAVAILABLE -> R.string.unlock_unavailable
                UnlockStatus.IDLE, UnlockStatus.CONNECTING, UnlockStatus.BUSY -> null
            }
            if (message != null) {
                Text(
                    text = stringResource(message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(
                    onClick = { viewModel.dismissMessage() },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.action_dismiss))
                }
            }
        }
    }
}

/** What the unlock covers, in the order the plan sells it. One entry per sentence on the screen.
 *  It names only what the purchase buys — the customer limit and the export. The visit history and
 *  the owed balances are free in every tier (they are what the free app is for), so a bullet for
 *  them would sell the user what he already has. */
private val UNLOCK_BULLETS: List<Int> = listOf(
    R.string.unlock_bullet_customers,
    R.string.unlock_bullet_export,
)

/** The price, or the fact that it has already been paid. */
@Composable
private fun PricePanel(
    owned: Boolean,
    price: String,
    priceNote: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (owned) {
                Text(
                    text = stringResource(R.string.unlock_unlocked),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.unlock_owned_body),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Text(
                    text = stringResource(R.string.unlock_price_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = price,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = priceNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
