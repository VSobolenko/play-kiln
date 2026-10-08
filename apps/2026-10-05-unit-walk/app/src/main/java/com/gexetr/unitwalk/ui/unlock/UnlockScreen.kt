package com.gexetr.unitwalk.ui.unlock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gexetr.capability.billing.BillingController
import com.gexetr.capability.core.CapabilityAvailability
import com.gexetr.capability.core.PlayKilnResult
import com.gexetr.unitwalk.R
import com.gexetr.unitwalk.ui.components.HeroPanel
import com.gexetr.unitwalk.ui.components.PillKind
import com.gexetr.unitwalk.ui.components.StatusPill
import com.gexetr.unitwalk.ui.theme.PlayKilnTheme

/**
 * The single one-time unlock — the surface behind the `single-unlock` promise.
 *
 * One sheet, one price, one purchase path and one restore path. `economics.json` describes a
 * **non-consumable** unlock and the factory bans the `subscription` archetype outright, so there is
 * deliberately no trial clock, no renewal wording, no second paywall and nothing consumable here.
 *
 * The price is Play's own, when Play answers: `state.priceLabel` is the amount for the user's Play
 * country and currency, so a `ru` account sees rubles rather than the app's literal. The string
 * resource is the **fallback**, not the price — it is what a preview, an offline first frame or a
 * misconfigured product shows, and it keeps the pack and the screen from drifting.
 *
 * The split follows `ui/settings/SettingsScreen.kt`: [UnlockScreen] / [UnlockRoute] build the ViewModel and
 * collect its state; `UnlockContent` is a pure function of [UnlockUiState] that emits
 * [UnlockEvent]s. Ownership comes from the ViewModel's entitlement mirror, and a failure is shown
 * as the capability's own typed reason rather than a silent no-op.
 *
 * [billing] is the shell's one shared controller (`ui/UnitWalkApp.kt`), handed in rather than built
 * here so that the purchase this sheet makes is the same entitlement the unit cap and the report
 * watermark read. The sheet must not, and does not, shut it down.
 */
@Composable
fun UnlockScreen(
    onBack: () -> Unit,
    billing: BillingController,
    modifier: Modifier = Modifier,
) {
    val viewModel: UnlockViewModel = viewModel(
        factory = UnlockViewModel.factory(billing),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UnlockContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier,
    )
}

/** The route form, mirroring `SettingsRoute`: same ViewModel, same state, one call site either way. */
@Composable
fun UnlockRoute(
    onBack: () -> Unit,
    billing: BillingController,
    modifier: Modifier = Modifier,
) {
    UnlockScreen(onBack = onBack, billing = billing, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnlockContent(
    state: UnlockUiState,
    onEvent: (UnlockEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val messageText = state.message?.let { message ->
        stringResource(
            when (message) {
                UnlockMessage.PurchaseSucceeded -> R.string.unlock_message_purchase_success
                UnlockMessage.PurchaseFailed -> R.string.unlock_message_purchase_failed
                UnlockMessage.AlreadyOwned -> R.string.unlock_message_already_owned
                UnlockMessage.RestoreSucceeded -> R.string.unlock_message_restore_success
                UnlockMessage.RestoreFailed -> R.string.unlock_message_restore_failed
                UnlockMessage.Unavailable -> R.string.unlock_message_unavailable
            },
        )
    }
    LaunchedEffect(messageText) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            onEvent(UnlockEvent.MessageDismissed)
        }
    }

    // Play's own localized price when it answered; the string resource is the fallback, not the source.
    val price = state.priceLabel ?: stringResource(R.string.unlock_price)
    val canAttemptPurchase = !state.isOwned &&
        !state.isWorking &&
        state.availability != CapabilityAvailability.UNSUPPORTED &&
        state.availability != CapabilityAvailability.FAILED

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.unlock_title)) },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.unlock_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HeroPanel(
                title = stringResource(R.string.unlock_hero_title),
                subtitle = stringResource(R.string.unlock_hero_subtitle),
                stat = price,
                statLabel = stringResource(R.string.unlock_price_caption),
            )

            Text(
                text = stringResource(R.string.unlock_pitch),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // The owned / not-owned state, as words plus a silhouette — never colour alone.
            StatusPill(
                label = stringResource(
                    if (state.isOwned) R.string.unlock_owned_status else R.string.unlock_not_owned_status,
                ),
                kind = if (state.isOwned) PillKind.OK else PillKind.NEUTRAL,
            )
            Text(
                text = stringResource(
                    if (state.isOwned) R.string.unlock_owned_detail else R.string.unlock_not_owned_detail,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.unlock_availability_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(availabilityResource(state.availability)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    // A typed failure is named, not swallowed: this is the difference between
                    // "nothing happened" and "here is why".
                    val failure = state.lastFailure
                    if (failure != null) {
                        Text(
                            text = stringResource(R.string.unlock_failure_label) + ": " +
                                stringResource(reasonResource(failure)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            // The busy state arrives and leaves rather than popping into place, so a slow Play
            // round-trip reads as something happening instead of a frozen sheet.
            AnimatedVisibility(visible = state.isWorking) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(
                        text = stringResource(R.string.unlock_working),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Button(
                onClick = { onEvent(UnlockEvent.PurchaseRequested) },
                enabled = canAttemptPurchase,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.unlock_buy_action, price))
            }

            OutlinedButton(
                onClick = { onEvent(UnlockEvent.RestoreRequested) },
                enabled = !state.isWorking,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.unlock_restore_action))
            }
        }
    }
}

private fun availabilityResource(availability: CapabilityAvailability): Int = when (availability) {
    CapabilityAvailability.UNINITIALIZED -> R.string.unlock_availability_uninitialized
    CapabilityAvailability.UNSUPPORTED -> R.string.unlock_availability_unsupported
    CapabilityAvailability.NEEDS_CONSENT -> R.string.unlock_availability_needs_consent
    CapabilityAvailability.READY -> R.string.unlock_availability_ready
    CapabilityAvailability.FAILED -> R.string.unlock_availability_failed
}

private fun reasonResource(reason: PlayKilnResult.Reason): Int = when (reason) {
    PlayKilnResult.Reason.NOT_AVAILABLE -> R.string.unlock_reason_not_available
    PlayKilnResult.Reason.NOT_CONFIGURED -> R.string.unlock_reason_not_configured
    PlayKilnResult.Reason.NO_CONSENT -> R.string.unlock_reason_no_consent
    PlayKilnResult.Reason.NETWORK -> R.string.unlock_reason_network
    PlayKilnResult.Reason.POLICY_BLOCKED -> R.string.unlock_reason_policy_blocked
    PlayKilnResult.Reason.UNKNOWN -> R.string.unlock_reason_unknown
}

@Preview(name = "Unlock not owned", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun UnlockContentNotOwnedPreview() {
    PlayKilnTheme(darkTheme = false, dynamicColor = false) {
        UnlockContent(
            state = UnlockUiState(
                availability = CapabilityAvailability.READY,
                isOwned = false,
            ),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Unlock owned", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun UnlockContentOwnedPreview() {
    PlayKilnTheme(darkTheme = true, dynamicColor = false) {
        UnlockContent(
            state = UnlockUiState(
                availability = CapabilityAvailability.READY,
                isOwned = true,
            ),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Unlock failed", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun UnlockContentFailedPreview() {
    PlayKilnTheme(darkTheme = false, dynamicColor = false) {
        UnlockContent(
            state = UnlockUiState(
                availability = CapabilityAvailability.FAILED,
                isOwned = false,
                message = UnlockMessage.PurchaseFailed,
                lastFailure = PlayKilnResult.Reason.NETWORK,
            ),
            onEvent = {},
            onBack = {},
        )
    }
}
