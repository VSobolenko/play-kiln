package com.gexetr.coverkit.ui.settings

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gexetr.coverkit.R
import com.gexetr.coverkit.data.AppSettings
import com.gexetr.coverkit.data.ReviewPromptStore
import com.gexetr.coverkit.data.ThemeMode
import com.gexetr.coverkit.ui.review.InAppReview
import com.gexetr.coverkit.ui.theme.PlayKilnTheme
import com.gexetr.coverkit.ui.unlock.PurchaseGate
import java.util.Locale
import org.xmlpull.v1.XmlPullParser

/**
 * The settings route: state wiring only, so the screen below stays a pure function of its arguments.
 */
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    purchaseGate: PurchaseGate? = null,
    onUnlock: () -> Unit = {},
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
        purchaseGate = purchaseGate,
        onUnlock = onUnlock,
        modifier = modifier,
    )
}

/**
 * The settings surface: appearance, in-app language, behaviour, and Rate app (In-App Review).
 *
 * Language stays inside the app via [LanguagePickerDialog] + [applyAppLanguage] — never
 * `ACTION_APP_LOCALE_SETTINGS`. Rate uses Play's ReviewManager first; store listing is fallback only.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    purchaseGate: PurchaseGate? = null,
    onUnlock: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val coroutineScope = rememberCoroutineScope()
    val reviewStore = remember(context) { ReviewPromptStore(context) }
    var languagePickerOpen by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            SectionHeader(stringResource(R.string.settings_studio_header))
            Text(
                text = stringResource(R.string.settings_studio_supporting),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

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

            val dynamicColorAvailable =
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
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
                    Text(stringResource(R.string.settings_language_summary))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { languagePickerOpen = true },
            )

            SectionHeader(stringResource(R.string.settings_behaviour_header))

            SwitchRow(
                title = stringResource(R.string.settings_haptics_title),
                supporting = stringResource(R.string.settings_haptics_summary),
                checked = settings.hapticsEnabled,
                enabled = true,
                onCheckedChange = onHapticsChange,
            )

            SectionHeader(stringResource(R.string.settings_purchase_header))

            ListItem(
                headlineContent = { Text(stringResource(R.string.unlock_restore)) },
                supportingContent = {
                    Text(stringResource(R.string.settings_restore_summary))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable {
                        purchaseGate?.restore()
                    },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.unlock_title)) },
                supportingContent = {
                    Text(stringResource(R.string.settings_unlock_summary))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(onClick = onUnlock),
            )

            SectionHeader(stringResource(R.string.settings_about_header))

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_rate_title)) },
                supportingContent = {
                    Text(stringResource(R.string.settings_rate_summary))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable {
                        if (activity != null) {
                            InAppReview.requestReviewFromSettings(
                                activity = activity,
                                store = reviewStore,
                                scope = coroutineScope,
                            )
                        }
                    },
            )
        }
    }

    if (languagePickerOpen) {
        val declaredTags = remember { declaredLanguageTags(context) }
        val appliedTag = AppCompatDelegate.getApplicationLocales().let { locales ->
            if (locales.isEmpty) null else locales.get(0)?.toLanguageTag()
        }
        LanguagePickerDialog(
            declaredTags = declaredTags,
            appliedTag = appliedTag,
            onSelect = { tag ->
                languagePickerOpen = false
                applyAppLanguage(tag)
            },
            onDismiss = { languagePickerOpen = false },
        )
    }
}

@Composable
private fun LanguagePickerDialog(
    declaredTags: List<String>,
    appliedTag: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_language_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                LanguageOptionRow(
                    label = stringResource(R.string.settings_language_system),
                    selected = appliedTag == null,
                    onClick = { onSelect(null) },
                )
                for (tag in declaredTags) {
                    LanguageOptionRow(
                        label = languageDisplayName(tag),
                        selected = appliedTag == tag,
                        onClick = { onSelect(tag) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun LanguageOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .heightIn(min = 48.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
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
 * Switches the app's own language without leaving the app.
 *
 * Null means follow the phone. AppCompat is the published backport; on Android 13+ it writes the
 * same framework store the system would. The system settings screen is deliberately not opened.
 */
internal fun applyAppLanguage(languageTag: String?) {
    val locales = if (languageTag == null) {
        LocaleListCompat.getEmptyLocaleList()
    } else {
        LocaleListCompat.forLanguageTags(languageTag)
    }
    AppCompatDelegate.setApplicationLocales(locales)
}

/** Language tags from `res/xml/locales_config.xml` — the picker's data, not a system localeConfig. */
private fun declaredLanguageTags(context: Context): List<String> {
    return runCatching {
        val parser = context.resources.getXml(R.xml.locales_config)
        val tags = mutableListOf<String>()
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if (parser.name != LOCALE_TAG) continue
            val tag = parser.getAttributeValue(ANDROID_NAMESPACE, LOCALE_NAME_ATTRIBUTE)
            if (!tag.isNullOrBlank()) tags.add(tag)
        }
        parser.close()
        tags
    }.getOrDefault(emptyList())
}

private fun languageDisplayName(tag: String): String {
    val locale = Locale.forLanguageTag(tag)
    return locale.getDisplayName(locale).replaceFirstChar { first -> first.titlecase(locale) }
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is android.content.ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private const val ANDROID_NAMESPACE: String = "http://schemas.android.com/apk/res/android"
private const val LOCALE_TAG: String = "locale"
private const val LOCALE_NAME_ATTRIBUTE: String = "name"

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
