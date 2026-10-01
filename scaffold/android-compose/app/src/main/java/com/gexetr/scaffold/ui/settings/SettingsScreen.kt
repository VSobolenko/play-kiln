package com.gexetr.scaffold.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gexetr.scaffold.R
import com.gexetr.scaffold.data.AppSettings
import com.gexetr.scaffold.data.ThemeMode
import com.gexetr.scaffold.ui.theme.PlayKilnTheme
import kotlinx.coroutines.launch

/**
 * The settings route: state wiring only, so the screen below stays a pure function of its arguments.
 */
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(LocalContext.current)),
) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreen(
        settings = settings,
        onBack = onBack,
        onThemeModeChange = viewModel::setThemeMode,
        onDynamicColorChange = viewModel::setDynamicColor,
        onHapticsChange = viewModel::setHapticsEnabled,
        modifier = modifier,
    )
}

/**
 * The settings surface.
 *
 * Three groups, and every row changes something the app actually reads — a control that changes
 * nothing is worse than no control, because it teaches the user that Settings is decoration.
 *
 * Accessibility decisions here are deliberate and are the reason the rows are shaped this way:
 *
 * - each row is one focus target, not two. The `Switch` and the `RadioButton` are drawn with a null
 *   handler and the *row* carries the click, so a screen reader announces one selectable item with
 *   its label, its value and its role instead of a label and an unlabelled control;
 * - every row declares `heightIn(min = 48.dp)`, so the whole row is the touch target rather than the
 *   trailing control alone;
 * - every user-visible string is a `stringResource`, so the screen is translated with the rest of the
 *   app, and the language row is the route to the picker that changes which one is used.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val languageUnavailable = stringResource(R.string.settings_language_unavailable)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.settings_back))
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
                .padding(bottom = 24.dp),
        ) {
            SectionHeader(stringResource(R.string.settings_appearance_header))

            Text(
                text = stringResource(R.string.settings_theme_supporting),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            ThemeMode.entries.forEach { mode ->
                ThemeModeRow(
                    label = stringResource(mode.labelResource()),
                    selected = settings.themeMode == mode,
                    onSelect = { onThemeModeChange(mode) },
                )
            }

            val dynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            SwitchRow(
                title = stringResource(R.string.settings_dynamic_color_title),
                supporting = stringResource(
                    if (dynamicColorAvailable) {
                        R.string.settings_dynamic_color_supported
                    } else {
                        R.string.settings_dynamic_color_unsupported
                    },
                ),
                checked = settings.dynamicColor && dynamicColorAvailable,
                enabled = dynamicColorAvailable,
                onCheckedChange = onDynamicColorChange,
            )

            SectionHeader(stringResource(R.string.settings_language_header))

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_language_title)) },
                supportingContent = {
                    Text(
                        stringResource(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                R.string.settings_language_summary
                            } else {
                                R.string.settings_language_summary_legacy
                            },
                        ),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable {
                        if (!context.openAppLanguageSettings()) {
                            coroutineScope.launch { snackbarHostState.showSnackbar(languageUnavailable) }
                        }
                    },
            )

            SectionHeader(stringResource(R.string.settings_behaviour_header))

            SwitchRow(
                title = stringResource(R.string.settings_haptics_title),
                supporting = stringResource(R.string.settings_haptics_summary),
                checked = settings.hapticsEnabled,
                enabled = true,
                onCheckedChange = onHapticsChange,
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp),
    )
}

/**
 * One mutually exclusive choice.
 *
 * The radio is drawn with a null handler and the row carries `selectable` plus `Role.RadioButton`, so
 * the accessible item is the labelled row rather than an unlabelled control beside it. `onClick` is
 * required to be non-null here, which is why the role is stated explicitly instead of being inferred.
 */
@Composable
private fun ThemeModeRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { RadioButton(selected = selected, onClick = null) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onSelect,
            ),
    )
}

/**
 * One on/off setting.
 *
 * The switch is drawn with a null handler and the row carries `toggleable`, so one tap, one focus
 * target and one announcement cover both the label and the control. A disabled row keeps its
 * supporting text, which is where the reason it is unavailable is written.
 */
@Composable
private fun SwitchRow(
    title: String,
    supporting: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(supporting) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
    )
}

private fun ThemeMode.labelResource(): Int = when (this) {
    ThemeMode.FOLLOW_SYSTEM -> R.string.settings_theme_system
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
}

/**
 * Opens Android's own per-app language settings for this app.
 *
 * The system picker is the right picker, and this app is in a position to use it: it declares
 * `android:localeConfig`, so Android 13 and newer already lists its languages and remembers the
 * choice in the same place the picker writes. A second picker built inside the app would be a second
 * source of truth for one value, which is how an app ends up in a language the user did not choose.
 *
 * Below Android 13 there is no per-app picker to open, so the app's own settings page is opened
 * instead and the row's supporting text says so. Pretending the two are the same screen would be the
 * honest-looking defect.
 *
 * Returns false when the platform has nothing to handle the intent, so the caller can say so rather
 * than leaving a tap with no visible effect.
 */
internal fun Context.openAppLanguageSettings(): Boolean {
    val packageUri = Uri.fromParts("package", packageName, null)
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Intent(Settings.ACTION_APP_LOCALE_SETTINGS).setData(packageUri)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(packageUri)
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

@Preview(name = "Settings light", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun SettingsScreenLightPreview() {
    PlayKilnTheme(darkTheme = false, dynamicColor = false) {
        SettingsScreen(
            settings = AppSettings(),
            onBack = {},
            onThemeModeChange = {},
            onDynamicColorChange = {},
            onHapticsChange = {},
        )
    }
}

@Preview(name = "Settings dark", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun SettingsScreenDarkPreview() {
    PlayKilnTheme(darkTheme = true, dynamicColor = false) {
        SettingsScreen(
            settings = AppSettings(themeMode = ThemeMode.DARK, dynamicColor = false, hapticsEnabled = false),
            onBack = {},
            onThemeModeChange = {},
            onDynamicColorChange = {},
            onHapticsChange = {},
        )
    }
}
