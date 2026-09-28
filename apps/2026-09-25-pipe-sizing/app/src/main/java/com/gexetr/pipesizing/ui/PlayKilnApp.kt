package com.gexetr.pipesizing.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.gexetr.pipesizing.BuildConfig
import com.gexetr.pipesizing.R
import com.gexetr.pipesizing.monetization.MonetizationConfig
import com.gexetr.pipesizing.monetization.MonetizationMessage
import com.gexetr.pipesizing.monetization.PlayKilnContainer
import com.gexetr.pipesizing.monetization.PurchaseOutcome
import com.gexetr.pipesizing.sizing.SizingEvaluation
import com.gexetr.pipesizing.ui.cases.CasesScreen
import com.gexetr.pipesizing.ui.cases.CasesViewModel
import com.gexetr.pipesizing.ui.cases.canAddCase
import com.gexetr.pipesizing.ui.guide.GuideScreen
import com.gexetr.pipesizing.ui.home.HomeEvent
import com.gexetr.pipesizing.ui.home.HomeScreen
import com.gexetr.pipesizing.ui.home.HomeViewModel
import com.gexetr.pipesizing.ui.home.buildReportFor
import com.gexetr.pipesizing.ui.home.shareCaseReport
import com.gexetr.pipesizing.ui.home.toStoredCase
import com.gexetr.pipesizing.ui.monetization.PaywallSheet
import com.gexetr.pipesizing.ui.monetization.ReportUnlockDialog
import com.gexetr.pipesizing.ui.tables.DataStoreTablesRepository
import com.gexetr.pipesizing.ui.tables.TablesScreen
import com.gexetr.pipesizing.ui.tables.TablesViewModel
import kotlinx.coroutines.launch

private sealed interface PlayKilnDestination

private data object CalculatorDestination : PlayKilnDestination

private data object CasesDestination : PlayKilnDestination

private data object GuideDestination : PlayKilnDestination

private data object TablesDestination : PlayKilnDestination

private const val CalculatorKey = "calculator"
private const val CasesKey = "cases"
private const val GuideKey = "guide"
private const val TablesKey = "tables"

private val DestinationBackStackSaver =
    listSaver<SnapshotStateList<PlayKilnDestination>, String>(
        save = { backStack ->
            backStack.map { destination ->
                when (destination) {
                    CalculatorDestination -> CalculatorKey
                    CasesDestination -> CasesKey
                    GuideDestination -> GuideKey
                    TablesDestination -> TablesKey
                }
            }
        },
        restore = { savedKeys ->
            mutableStateListOf<PlayKilnDestination>().apply {
                savedKeys.mapTo(this) { key ->
                    when (key) {
                        CasesKey -> CasesDestination
                        GuideKey -> GuideDestination
                        TablesKey -> TablesDestination
                        else -> CalculatorDestination
                    }
                }
                if (isEmpty()) add(CalculatorDestination)
            }
        },
    )

/**
 * The app's composition root: one Scaffold owns the bars and the snackbar host, and every screen is
 * content only. All three ViewModels are created here rather than inside a destination so switching
 * tabs cannot drop the working case the user is in the middle of entering, nor the table they are
 * editing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayKilnApp(container: PlayKilnContainer, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(context))
    val casesViewModel: CasesViewModel = viewModel(factory = CasesViewModel.factory(container.cases))
    // The table surface's state holder is created here too, so switching tabs cannot drop a table
    // being edited. It reads the same device-local store the calculator's table picker reads.
    val tablesViewModel: TablesViewModel = viewModel(
        factory = TablesViewModel.factory(DataStoreTablesRepository(context.applicationContext)),
    )

    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val savedCases by casesViewModel.cases.collectAsStateWithLifecycle()
    val tablesState by tablesViewModel.uiState.collectAsStateWithLifecycle()
    val monetization by container.monetization.state.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var reportDialogVisible by remember { mutableStateOf(false) }
    // Rewarded video unlocks report sharing for the rest of the session, so a user who watched once
    // is not asked again for every case; Pro removes the gate entirely.
    var reportUnlockedThisSession by remember { mutableStateOf(false) }

    val showMessage: (String) -> Unit = { text ->
        coroutineScope.launch { snackbarHostState.showSnackbar(text) }
    }

    val backStack = rememberSaveable(saver = DestinationBackStackSaver) {
        mutableStateListOf<PlayKilnDestination>(CalculatorDestination)
    }

    LaunchedEffect(monetization.message) {
        val message = monetization.message
        if (message != null) {
            when (message) {
                MonetizationMessage.CONSENT_UNAVAILABLE ->
                    showMessage(context.getString(R.string.consent_unavailable))
                MonetizationMessage.RESTORE_SUCCESS ->
                    showMessage(context.getString(R.string.restore_success))
                MonetizationMessage.RESTORE_NONE ->
                    showMessage(context.getString(R.string.restore_none))
                else -> Unit
            }
            container.monetization.consumeMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    if (monetization.pro) {
                        Text(
                            text = stringResource(R.string.pro_badge),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        TextButton(onClick = { container.monetization.openPaywall() }) {
                            Text(stringResource(R.string.paywall_title))
                        }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                TabItem(
                    // Labels are short; the icon carries the meaning at a glance.
                    labelRes = R.string.nav_calculator,
                    selected = backStack.lastOrNull() == CalculatorDestination,
                    onSelect = { backStack.select(CalculatorDestination) },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                )
                TabItem(
                    labelRes = R.string.nav_cases,
                    selected = backStack.lastOrNull() == CasesDestination,
                    onSelect = { backStack.select(CasesDestination) },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                )
                TabItem(
                    labelRes = R.string.nav_guide,
                    selected = backStack.lastOrNull() == GuideDestination,
                    onSelect = { backStack.select(GuideDestination) },
                    icon = { Icon(Icons.Filled.Info, contentDescription = null) },
                )
                TabItem(
                    // The entry surface for the tables the calculator returns values from.
                    labelRes = R.string.nav_tables,
                    selected = backStack.lastOrNull() == TablesDestination,
                    onSelect = { backStack.select(TablesDestination) },
                    icon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
            entryProvider = { key ->
                when (key) {
                    CalculatorDestination -> NavEntry(key) {
                        HomeScreen(
                            state = homeState,
                            onEvent = { event ->
                                homeViewModel.onEvent(event)
                                // An interstitial is offered only after a case actually computed:
                                // a refusal is not a monetizable moment, and the app must never
                                // interrupt a user whose input still needs correcting.
                                if (event is HomeEvent.Evaluate &&
                                    homeViewModel.uiState.value.evaluation is SizingEvaluation.Computed
                                ) {
                                    coroutineScope.launch { container.monetization.onEvaluationCompleted() }
                                }
                            },
                            isPro = monetization.pro,
                            canSaveCase = canAddCase(savedCases.size, MonetizationConfig.FREE_CASE_LIMIT, monetization.pro),
                            savedCaseCount = savedCases.size,
                            freeCaseLimit = MonetizationConfig.FREE_CASE_LIMIT,
                            onSaveCase = { name ->
                                coroutineScope.launch {
                                    casesViewModel.save(name, homeState.toStoredCase())
                                    showMessage(context.getString(R.string.case_saved))
                                }
                            },
                            onLimitReached = {
                                showMessage(
                                    context.getString(R.string.cases_limit_reached, MonetizationConfig.FREE_CASE_LIMIT),
                                )
                                container.monetization.openPaywall()
                            },
                            onShareReport = {
                                // One decision for whatever is on screen: an arithmetic case or a
                                // value returned from the user's own recorded table both have a
                                // report; a refusal has none.
                                val report = buildReportFor(context, homeState, monetization.pro)
                                when {
                                    report == null ->
                                        showMessage(context.getString(R.string.report_unavailable))
                                    monetization.pro || reportUnlockedThisSession ->
                                        shareCaseReport(context, report)
                                    else -> reportDialogVisible = true
                                }
                            },
                            onOpenPaywall = { container.monetization.openPaywall() },
                        )
                    }

                    CasesDestination -> NavEntry(key) {
                        CasesScreen(
                            cases = savedCases,
                            isPro = monetization.pro,
                            freeCaseLimit = MonetizationConfig.FREE_CASE_LIMIT,
                            onLoad = { saved ->
                                homeViewModel.onEvent(HomeEvent.CaseLoaded(saved.fields))
                                backStack.select(CalculatorDestination)
                                showMessage(context.getString(R.string.cases_loaded))
                            },
                            onDelete = { saved ->
                                coroutineScope.launch {
                                    casesViewModel.delete(saved.id)
                                    showMessage(context.getString(R.string.cases_deleted))
                                }
                            },
                            onOpenPaywall = { container.monetization.openPaywall() },
                        )
                    }

                    GuideDestination -> NavEntry(key) {
                        GuideScreen(
                            versionName = BuildConfig.VERSION_NAME,
                            isPro = monetization.pro,
                            adsAvailable = monetization.adsAvailable,
                            consentResolved = monetization.consentResolved,
                            onReviewAdChoices = {
                                coroutineScope.launch {
                                    container.monetization.reviewAdChoices()
                                    showMessage(context.getString(R.string.consent_resolved))
                                }
                            },
                            onRestorePurchase = {
                                coroutineScope.launch { container.monetization.restore() }
                            },
                            onOpenPaywall = { container.monetization.openPaywall() },
                        )
                    }

                    TablesDestination -> NavEntry(key) {
                        TablesScreen(
                            state = tablesState,
                            onEvent = tablesViewModel::onEvent,
                        )
                    }
                }
            },
        )
    }

    if (monetization.paywallVisible) {
        PaywallSheet(
            isPro = monetization.pro,
            purchaseInFlight = monetization.purchaseInFlight,
            freeCaseLimit = MonetizationConfig.FREE_CASE_LIMIT,
            onBuy = {
                coroutineScope.launch {
                    when (container.monetization.purchasePro()) {
                        PurchaseOutcome.SUCCESS -> container.monetization.dismissPaywall()
                        PurchaseOutcome.PENDING ->
                            showMessage(context.getString(R.string.paywall_pending))
                        PurchaseOutcome.UNAVAILABLE ->
                            showMessage(context.getString(R.string.paywall_unavailable))
                        PurchaseOutcome.CANCELLED -> Unit
                        PurchaseOutcome.FAILED ->
                            showMessage(context.getString(R.string.paywall_failed))
                    }
                }
            },
            onRestore = {
                coroutineScope.launch {
                    if (container.monetization.restore()) container.monetization.dismissPaywall()
                }
            },
            onDismiss = { container.monetization.dismissPaywall() },
        )
    }

    if (reportDialogVisible) {
        ReportUnlockDialog(
            onWatchRewarded = {
                reportDialogVisible = false
                coroutineScope.launch {
                    if (container.monetization.watchRewarded()) {
                        reportUnlockedThisSession = true
                        // The unlock applies to the same report decision the share action uses, so a
                        // returned-from-table value is shareable after a rewarded video too.
                        buildReportFor(context, homeState, monetization.pro)?.let { report ->
                            shareCaseReport(context, report)
                        }
                    } else {
                        showMessage(context.getString(R.string.rewarded_failed))
                    }
                }
            },
            onOpenPaywall = {
                reportDialogVisible = false
                container.monetization.openPaywall()
            },
            onDismiss = { reportDialogVisible = false },
        )
    }
}

/**
 * Bottom-navigation selection.
 *
 * The back stack holds one tab, so choosing a tab never grows a history the back button would then
 * have to unwind through; back leaves the app, which is what a tab bar promises.
 */
private fun SnapshotStateList<PlayKilnDestination>.select(destination: PlayKilnDestination) {
    if (lastOrNull() == destination) return
    clear()
    add(destination)
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TabItem(
    labelRes: Int,
    selected: Boolean,
    onSelect: () -> Unit,
    icon: @Composable () -> Unit,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onSelect,
        icon = icon,
        label = { Text(stringResource(labelRes)) },
    )
}
