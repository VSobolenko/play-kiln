package com.gexetr.coverkit.ui.home

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gexetr.coverkit.R
import com.gexetr.coverkit.data.ReviewPromptStore
import com.gexetr.coverkit.ui.review.InAppReview
import com.gexetr.coverkit.ui.theme.PlayKilnTheme

@Composable
fun HomeRoute(
    onNavigateToAbout: () -> Unit,
    onNavigateToSettings: () -> Unit,
    hapticsEnabled: Boolean,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onAboutClick = onNavigateToAbout,
        onSettingsClick = onNavigateToSettings,
        hapticsEnabled = hapticsEnabled,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
    onAboutClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    hapticsEnabled: Boolean = false,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val reviewStore = remember(context) { ReviewPromptStore(context) }
    val reviewScope = rememberCoroutineScope()
    val snackbarText = when (state.snackbarMessage) {
        HomeSnackbarMessage.ActionCompleted -> stringResource(R.string.home_snackbar_completed)
        HomeSnackbarMessage.AlreadyCompleted -> {
            stringResource(R.string.home_snackbar_already_completed)
        }
        null -> null
    }

    // Soft In-App Review after the first successful primary action (see in-app-review.mdc).
    LaunchedEffect(state.snackbarMessage) {
        if (state.snackbarMessage == HomeSnackbarMessage.ActionCompleted && activity != null) {
            InAppReview.maybeSoftPromptAfterSuccess(
                activity = activity,
                store = reviewStore,
                scope = reviewScope,
            )
        }
    }

    LaunchedEffect(snackbarText) {
        if (snackbarText != null) {
            snackbarHostState.showSnackbar(snackbarText)
            onEvent(HomeEvent.SnackbarDismissed)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    TextButton(
                        onClick = onAboutClick,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.home_about_action))
                    }
                    TextButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.settings_title))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) {         innerPadding ->
        HomeContent(
            state = state,
            onPrimaryAction = {
                // The one preference that changes what the app *does* rather than how it looks.
                // `settings.behaviour` fails a toggle nothing reads, and this is where it is read.
                if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onEvent(HomeEvent.PrimaryActionClicked)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .verticalScroll(rememberScrollState()),
        )
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusText = stringResource(
        if (state.isStarterComplete) {
            R.string.home_status_complete
        } else {
            R.string.home_status_ready
        },
    )

    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = stringResource(R.string.home_intro),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.home_idea_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.app_idea),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { stateDescription = statusText },
        )
        Button(
            onClick = onPrimaryAction,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text(
                stringResource(
                    if (state.isStarterComplete) {
                        R.string.home_primary_action_again
                    } else {
                        R.string.home_primary_action
                    },
                ),
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@Preview(name = "Home light", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun HomeScreenLightPreview() {
    PlayKilnTheme(darkTheme = false, dynamicColor = false) {
        HomeScreen(
            state = HomeUiState(),
            onEvent = {},
        )
    }
}

@Preview(
    name = "Home dark",
    showBackground = true,
    widthDp = 412,
    heightDp = 892,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HomeScreenDarkPreview() {
    PlayKilnTheme(darkTheme = true, dynamicColor = false) {
        HomeScreen(
            state = HomeUiState(isStarterComplete = true),
            onEvent = {},
        )
    }
}
