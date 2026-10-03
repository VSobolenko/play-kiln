package com.gexetr.lawnroundlog.ui

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.gexetr.lawnroundlog.R
import com.gexetr.lawnroundlog.domain.Customer
import com.gexetr.lawnroundlog.ui.about.AboutRoute
import com.gexetr.lawnroundlog.ui.components.GlyphBack
import com.gexetr.lawnroundlog.ui.customer.CustomerRoute
import com.gexetr.lawnroundlog.ui.history.HistoryRoute
import com.gexetr.lawnroundlog.ui.round.RoundRoute
import com.gexetr.lawnroundlog.ui.round.RoundViewModel
import com.gexetr.lawnroundlog.ui.settings.SettingsScreen
import com.gexetr.lawnroundlog.ui.settings.SettingsViewModel
import com.gexetr.lawnroundlog.ui.unlock.PurchaseGate
import com.gexetr.lawnroundlog.ui.unlock.UnlockScreen
import kotlinx.serialization.Serializable

/**
 * How the app chooses between its light and dark palettes.
 *
 * `FOLLOW_SYSTEM` is the default and the first option in Settings, because the platform's day/night
 * answer is what the user has already given the device. The two explicit modes exist for the operator
 * who wants this app dark while the phone is light — which is exactly the person standing in a
 * driveway at seven in the morning.
 *
 * It is declared beside the shell rather than inside the settings package because it has two readers
 * and neither owns the other: `MainActivity` resolves it into `darkTheme = …` before any screen
 * composes, and the settings surface offers it as a row. The label is part of the enum so a fourth
 * mode cannot be added without a word to show for it.
 */
enum class AppearanceMode(val labelRes: Int) {
    FOLLOW_SYSTEM(R.string.settings_appearance_system),
    LIGHT(R.string.settings_appearance_light),
    DARK(R.string.settings_appearance_dark),

    ;

    fun isDark(systemInDarkTheme: Boolean): Boolean = when (this) {
        FOLLOW_SYSTEM -> systemInDarkTheme
        LIGHT -> false
        DARK -> true
    }
}

/**
 * The destinations, as Navigation 3 keys.
 *
 * `@Serializable` and `NavKey` are what `rememberNavBackStack` requires: it stores the back stack
 * through Navigation 3's own serializer rather than a hand-rolled `listSaver`. The host owning the
 * back stack is what keeps `NavDisplay`'s transitions, predictive-back continuity and scene
 * strategies live — see `.cursor/rules/ui-ux-baseline.mdc`.
 *
 * `RoundDestination` is the leading pane and carries no payload, so the list-detail strategy can
 * keep it beside whatever is opened from it. `CustomerDestination` carries the customer id, which is
 * the only payload the app needs: every other screen is a peer of the round, not a thing inside a
 * customer.
 */
@Serializable
private data object RoundDestination : NavKey

@Serializable
private data class CustomerDestination(val customerId: String? = null) : NavKey

@Serializable
private data object HistoryDestination : NavKey

@Serializable
private data object UnlockDestination : NavKey

@Serializable
private data object SettingsDestination : NavKey

@Serializable
private data object AboutDestination : NavKey

/**
 * The app shell: the back stack, the list-detail policy, and the one place the nine-customer rule
 * is decided.
 *
 * The list-detail strategy is what makes the form factor matter. Below its own breakpoint the round
 * fills the window exactly as a phone expects; above it the round stays in the leading pane while
 * the customer it opens appears beside it instead of replacing it. Without a strategy the same back
 * stack renders one stretched column on a tablet, which is the defect this rule exists for.
 *
 * The ViewModels are passed in rather than created here so the shell stays a function of its
 * arguments and a preview can supply fakes; the activity creates them, which is also what lets the
 * settings it collects strand outside the navigation host where the theme needs them.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun PlayKilnApp(
    round: RoundViewModel,
    settings: SettingsViewModel,
    purchaseGate: PurchaseGate,
) {
    val backStack: NavBackStack<NavKey> = rememberNavBackStack(RoundDestination)
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

    val settingsState by settings.state.collectAsStateWithLifecycle()
    val settingsProblem by settings.problem.collectAsStateWithLifecycle()
    val pendingCustomer by round.pendingCustomer.collectAsStateWithLifecycle()
    val unlocked by purchaseGate.owned.collectAsStateWithLifecycle(initialValue = false)

    // The ninth customer is staged before the unlock sheet is shown, and committed the moment the
    // purchase completes. Keeping him staged — rather than asking the operator to type him again — is
    // the whole difference between an upsell and a lost record.
    LaunchedEffect(unlocked, pendingCustomer) {
        if (unlocked && pendingCustomer != null) {
            round.commitPendingCustomer()
            backStack.popToStart()
        }
    }

    val commitCustomer: (Customer, Boolean) -> Unit = { customer, isNew ->
        if (isNew && round.requiresUnlock(unlocked)) {
            round.stagePendingCustomer(customer)
            backStack.push(UnlockDestination)
        } else {
            round.upsertCustomer(customer)
            backStack.popOne()
        }
    }

    // The app's content is rooted in a Surface so that every destination inherits a content colour.
    // Without it there is no `LocalContentColor` between the window and the first screen, so Compose
    // falls back to `Color.Black` and any Text or Icon that omits an explicit colour is invisible on
    // this slot's dark background. `Surface` derives `contentColor` from its `color`, so rooting the
    // host in the theme's own `background` gives every default-coloured glyph `onBackground` — and
    // the drawn background becomes the theme's rather than the XML window background behind it.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.popOne() },
            sceneStrategies = listOf(listDetailStrategy),
            // A real transition, not the instant swap an unconfigured host performs: a pushed screen
            // arrives from the end of the stack and fades back out the way it came. Only the incoming
            // screen moves — the outgoing one fades in place — because two full-screen layers moving
            // every frame is the stutter the owner measured on the test phone, and a one-third slide
            // still says which direction the screen came from. `popTransitionSpec` is the mirror
            // image, so predictive back has a continuous gesture to preview.
            transitionSpec = {
                slideInHorizontally(
                    animationSpec = tween(durationMillis = 180, easing = LinearOutSlowInEasing),
                    initialOffsetX = { width -> width / 3 },
                ) + fadeIn(tween(durationMillis = 140)) togetherWith
                    fadeOut(tween(durationMillis = 120))
            },
            popTransitionSpec = {
                slideInHorizontally(
                    animationSpec = tween(durationMillis = 180, easing = LinearOutSlowInEasing),
                    initialOffsetX = { width -> -width / 3 },
                ) + fadeIn(tween(durationMillis = 140)) togetherWith
                    fadeOut(tween(durationMillis = 120))
            },
            entryProvider = entryProvider {
                entry<RoundDestination>(
                    metadata = ListDetailSceneStrategy.listPane(
                        detailPlaceholder = { DetailPlaceholder() },
                    ),
                ) {
                    RoundRoute(
                        round = round,
                        settings = settingsState,
                        unlocked = unlocked,
                        onOpenCustomer = { customerId -> backStack.push(CustomerDestination(customerId)) },
                        onAddCustomer = { backStack.push(CustomerDestination(null)) },
                        onOpenHistory = { backStack.push(HistoryDestination) },
                        onOpenUnlock = { backStack.push(UnlockDestination) },
                        onOpenSettings = { backStack.push(SettingsDestination) },
                        onOpenAbout = { backStack.push(AboutDestination) },
                    )
                }
                entry<CustomerDestination>(metadata = ListDetailSceneStrategy.detailPane()) { destination ->
                    CustomerRoute(
                        round = round,
                        customerId = destination.customerId,
                        settings = settingsState,
                        unlocked = unlocked,
                        onBack = { backStack.popOne() },
                        onRequestUnlock = { backStack.push(UnlockDestination) },
                        onSave = commitCustomer,
                    )
                }
                entry<HistoryDestination>(metadata = ListDetailSceneStrategy.detailPane()) {
                    HistoryRoute(
                        round = round,
                        settings = settingsState,
                        unlocked = unlocked,
                        onBack = { backStack.popOne() },
                        onRequestUnlock = { backStack.push(UnlockDestination) },
                    )
                }
                entry<UnlockDestination>(metadata = ListDetailSceneStrategy.detailPane()) {
                    UnlockScreen(gate = purchaseGate, onBack = { backStack.popOne() })
                }
                // The settings state is collected here, at the host, and handed to the surface as a value
                // with callbacks. The activity-scoped ViewModel stays the one owner of the preferences —
                // a second one inside this entry would be a second copy of the same stored value.
                entry<SettingsDestination>(metadata = ListDetailSceneStrategy.detailPane()) {
                    SettingsScreen(
                        settings = settingsState,
                        problem = settingsProblem,
                        onAppearanceChange = settings::setAppearance,
                        onCurrencyChange = settings::setCurrencySymbol,
                        onDefaultAmountChange = settings::setDefaultVisitAmount,
                        onConfirmBeforeDeleteChange = settings::setConfirmBeforeDelete,
                        onHapticsChange = settings::setHapticsEnabled,
                        onBack = { backStack.popOne() },
                    )
                }
                entry<AboutDestination>(metadata = ListDetailSceneStrategy.detailPane()) {
                    AboutRoute(onBack = { backStack.popOne() })
                }
            },
        )
    }
}

/** One pop, and only when there is something to pop. */
private fun NavBackStack<NavKey>.popOne() {
    if (size > 1) removeLastOrNull()
}

/** Back to the round, used once a staged customer has been committed. */
private fun NavBackStack<NavKey>.popToStart() {
    while (size > 1) removeLastOrNull()
}

/** One push, and never a duplicate of the destination already on top. */
private fun NavBackStack<NavKey>.push(destination: NavKey) {
    if (lastOrNull() != destination) add(destination)
}

/**
 * The chrome every screen except the round shares.
 *
 * It lives beside the shell rather than in the slot's component library because it is *navigation*
 * chrome — a back affordance and a title — not a piece of the product's visual vocabulary, and it is
 * owned by the same file that owns the back stack it pops.
 *
 * The back control is 48 dp and the title takes the remaining width with two lines available, so a
 * long Russian screen name grows the bar instead of being cut off. `subtitle` exists because several
 * screens need to say which record they are showing.
 */
@Composable
internal fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
    ) {
        // The status-bar inset is applied here, in the one header every non-round screen shares, so a
        // new screen cannot be born with its title under the clock and the punch-hole camera. The
        // Surface above deliberately carries no inset: its `surfaceContainer` colour still paints to
        // the window edge behind the status bar, and only the header's *content* moves down — which is
        // the modern edge-to-edge look, decided once instead of per screen.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                val backLabel = stringResource(R.string.action_back)
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = backLabel },
                ) {
                    GlyphBack()
                }
            } else {
                Spacer(Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 48.dp) {
                actions()
            }
        }
    }
}

/** The leading pane's own words for "nothing is open yet", shown only on a wide window. */
@Composable
private fun DetailPlaceholder() {
    Text(
        text = stringResource(R.string.round_detail_placeholder),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
