package com.gexetr.laneset.ui.unlock

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gexetr.laneset.R
import com.gexetr.laneset.data.BillingAvailability
import com.gexetr.laneset.ui.components.BrandMark
import com.gexetr.laneset.ui.components.StatTile
import com.gexetr.laneset.ui.theme.DeckSlabShape
import com.gexetr.laneset.ui.theme.LaneRopeShape
import com.gexetr.laneset.ui.theme.PlayKilnTheme
import com.gexetr.laneset.ui.theme.laneSetColors

/**
 * The unlock route: state wiring only, so the screen below stays a pure function of its arguments.
 *
 * The `Activity` is read here with `LocalActivity` and handed to the ViewModel only for the moment it
 * takes to open Play's sheet — `launchPurchase` needs one, and a null or finished activity is reported
 * by the manager as `Unavailable` rather than throwing. When there is no activity (a preview, or a
 * host that is not an activity) the buy action is disabled and the screen still renders honestly.
 */
@Composable
fun UnlockRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UnlockViewModel = viewModel(factory = UnlockViewModel.factory(LocalContext.current)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    UnlockScreen(
        state = state,
        onBack = onBack,
        onBuy = { activity?.let(viewModel::buy) },
        onRestore = viewModel::restore,
        buyEnabled = activity != null,
        modifier = modifier,
    )
}

/**
 * The single-price unlock screen.
 *
 * It states three things plainly, because a paywall that hides any of them is the defect it exists to
 * answer: **what** the unlock gives (the other focuses, the guided pace test, the coach import and the
 * handoff), **what it costs**, and that it is **one payment and not a subscription**. The last is a
 * selling point on this shelf, where the leader's own reviewers complain about a recurring paywall, so
 * it is the largest statement on the screen rather than a footnote.
 *
 * It shows **no numeric price**. A hard-coded `$9.99` is wrong outside the dollar zone and a wrong
 * price is worse than none, so the price note says Google Play shows the exact localized price on the
 * confirmation sheet it opens — which is where the number genuinely comes from.
 *
 * The state is not flattened: an owned unlock says so instead of offering a second sale; a reachable
 * store offers the action; an unreachable one says the store cannot be reached and offers a retry
 * rather than a button that cannot work; and a cancellation is reported in the ordinary ink, not as an
 * error. Every one of those branches is a different sentence to the swimmer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockScreen(
    state: UnlockUiState,
    onBack: () -> Unit,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
    buyEnabled: Boolean = true,
) {
    val laneSet = laneSetColors
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            UnlockHero()

            BenefitList()

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    label = stringResource(R.string.unlock_purchase_label),
                    value = stringResource(R.string.unlock_purchase_value),
                    supporting = null,
                    icon = R.drawable.ic_check,
                    accent = laneSet.restOnSurface,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = stringResource(R.string.unlock_subscription_label),
                    value = stringResource(R.string.unlock_subscription_value),
                    supporting = null,
                    icon = R.drawable.ic_close,
                    accent = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }

            Text(
                text = stringResource(R.string.unlock_no_renewal),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.unlock_price_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            UnlockAction(
                state = state,
                buyEnabled = buyEnabled,
                onBuy = onBuy,
                onRestore = onRestore,
            )

            if (!state.isUnlocked) {
                state.notice?.let { notice -> NoticeLine(notice) }
            }

            Text(
                text = stringResource(R.string.unlock_account_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The hero: the identity's own gradient field with the mark, and the one sentence the shelf's
 * reviewers never get to read — that the app is bought once.
 */
@Composable
private fun UnlockHero(modifier: Modifier = Modifier) {
    val laneSet = laneSetColors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(listOf(laneSet.heroStart, laneSet.heroEnd)),
                shape = DeckSlabShape,
            )
            .padding(horizontal = 24.dp, vertical = 22.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BrandMark(
                modifier = Modifier.size(44.dp),
                tint = laneSet.onHero,
                accent = laneSet.pace,
            )
            Text(
                text = stringResource(R.string.unlock_hero_label),
                style = MaterialTheme.typography.labelMedium,
                color = laneSet.pace,
            )
            Text(
                text = stringResource(R.string.unlock_lede),
                style = MaterialTheme.typography.titleLarge,
                color = laneSet.onHero,
            )
        }
    }
}

/** What the unlock gives, one benefit per row, each with a vendored icon and a 48 dp line. */
@Composable
private fun BenefitList(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.unlock_includes_header),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        BenefitRow(R.drawable.ic_pool, R.string.unlock_benefit_focuses)
        BenefitRow(R.drawable.ic_timer, R.string.unlock_benefit_pace_test)
        BenefitRow(R.drawable.ic_edit, R.string.unlock_benefit_import)
        BenefitRow(R.drawable.ic_share, R.string.unlock_benefit_handoff)
    }
}

/** One benefit line. The icon is decorative — the row's own text carries the meaning. */
@Composable
private fun BenefitRow(icon: Int, textRes: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * The action area, in the four shapes the state can honestly take.
 *
 * The branch order is the priority: an owned unlock outranks everything (no second sale), an
 * unreachable store is stated rather than offered, an unsettled connection says it is connecting, and
 * only a reachable store with nothing owned shows the buy action.
 */
@Composable
private fun UnlockAction(
    state: UnlockUiState,
    buyEnabled: Boolean,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            state.isUnlocked -> OwnedPanel()

            state.availability == BillingAvailability.UNAVAILABLE ->
                StoreUnavailablePanel(isRestoring = state.isRestoring, onRetry = onRestore)

            state.availability == BillingAvailability.UNINITIALIZED ->
                ConnectingPanel(isRestoring = state.isRestoring, onRetry = onRestore)

            else -> {
                Button(
                    onClick = onBuy,
                    enabled = buyEnabled && state.canBuy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(
                        text = stringResource(
                            if (state.isPurchasing) R.string.unlock_purchasing else R.string.unlock_buy,
                        ),
                    )
                }
                TextButton(
                    onClick = onRestore,
                    enabled = !state.isRestoring && !state.isPurchasing,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.unlock_action_restore))
                }
            }
        }
    }
}

/** The entitlement is owned: say so, and offer no second sale. */
@Composable
private fun OwnedPanel(modifier: Modifier = Modifier) {
    val laneSet = laneSetColors
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = laneSet.restContainer,
        contentColor = laneSet.onRestContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = laneSet.onRestContainer,
                modifier = Modifier.size(22.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.unlock_owned_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.unlock_owned_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/** The store cannot be reached: state it, and offer the reconnect rather than a dead button. */
@Composable
private fun StoreUnavailablePanel(
    isRestoring: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.unlock_unavailable_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.unlock_unavailable_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            RetryButton(isRestoring = isRestoring, onRetry = onRetry)
        }
    }
}

/** The connection has not settled yet: say so, and let the swimmer ask again. */
@Composable
private fun ConnectingPanel(
    isRestoring: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.unlock_connecting),
                style = MaterialTheme.typography.bodyLarge,
            )
            RetryButton(isRestoring = isRestoring, onRetry = onRetry)
        }
    }
}

@Composable
private fun RetryButton(isRestoring: Boolean, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onRetry,
        enabled = !isRestoring,
        modifier = modifier.heightIn(min = 48.dp),
    ) {
        Text(stringResource(R.string.unlock_action_retry))
    }
}

/**
 * One closing note. A cancellation is drawn in the ordinary surface ink because nothing went wrong;
 * only a failed or unattempted purchase takes the error container.
 */
@Composable
private fun NoticeLine(notice: UnlockNotice, modifier: Modifier = Modifier) {
    val isProblem = notice == UnlockNotice.FAILED || notice == UnlockNotice.UNAVAILABLE
    val container = if (isProblem) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val content = if (isProblem) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val textRes = when (notice) {
        UnlockNotice.CANCELLED -> R.string.unlock_notice_cancelled
        UnlockNotice.FAILED -> R.string.unlock_notice_failed
        UnlockNotice.UNAVAILABLE -> R.string.unlock_notice_unavailable
        UnlockNotice.NOT_RESTORED -> R.string.unlock_notice_not_restored
    }
    val icon = if (isProblem) R.drawable.ic_close else R.drawable.ic_info
    Surface(
        shape = LaneRopeShape,
        color = container,
        contentColor = content,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(textRes),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview(name = "Unlock offer", showBackground = true)
@Composable
private fun UnlockOfferPreview() {
    PlayKilnTheme {
        UnlockScreen(
            state = UnlockUiState(availability = BillingAvailability.READY),
            onBack = {},
            onBuy = {},
            onRestore = {},
        )
    }
}

@Preview(name = "Unlock owned", showBackground = true)
@Composable
private fun UnlockOwnedPreview() {
    PlayKilnTheme {
        UnlockScreen(
            state = UnlockUiState(isUnlocked = true, availability = BillingAvailability.READY),
            onBack = {},
            onBuy = {},
            onRestore = {},
        )
    }
}
