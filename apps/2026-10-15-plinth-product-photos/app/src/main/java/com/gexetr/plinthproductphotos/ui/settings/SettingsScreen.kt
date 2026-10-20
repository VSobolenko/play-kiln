package com.gexetr.plinthproductphotos.ui.settings

import com.gexetr.plinthproductphotos.ui.theme.PlinthTheme

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.data.AppSettings
import com.gexetr.plinthproductphotos.data.ReviewPromptStore
import com.gexetr.plinthproductphotos.data.ThemeMode
import com.gexetr.plinthproductphotos.data.StudioDefaults
import com.gexetr.plinthproductphotos.ui.review.InAppReview
import com.gexetr.plinthproductphotos.ui.theme.PlayKilnTheme
import java.util.Locale
import org.xmlpull.v1.XmlPullParser

/**
 * The settings route: state wiring only, so the screen below stays a pure function of its arguments.
 */
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenWallet: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(LocalContext.current)),
) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val saveError by viewModel.saveError.collectAsStateWithLifecycle()
    SettingsScreen(
        settings = settings,
        onBack = onBack,
        onThemeModeChange = viewModel::setThemeMode,
        onHapticsChange = viewModel::setHapticsEnabled,
        onStudioDefaultsChange = viewModel::updateStudioDefaults,
        studioSaveError = saveError,
        modifier = modifier,
    )
}

/**
 * App preferences, new-image defaults, credits and support are separate subjects.
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
    onHapticsChange: (Boolean) -> Unit,
    onStudioDefaultsChange: ((StudioDefaults) -> StudioDefaults) -> Unit,
    studioSaveError: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val privacyPolicyUrl = stringResource(R.string.privacy_policy_url)
    val productSupportEmail = stringResource(R.string.product_support_email)
    val deleteSubject = stringResource(R.string.settings_product_delete_subject)
    val deleteBody = stringResource(R.string.settings_product_delete_body)
    val activity = context.findActivity()
    val coroutineScope = rememberCoroutineScope()
    val reviewStore = remember(context) { ReviewPromptStore(context) }
    var themePickerOpen by rememberSaveable { mutableStateOf(false) }
    var languagePickerOpen by rememberSaveable { mutableStateOf(false) }
    var imageDefaultsOpen by rememberSaveable { mutableStateOf(false) }
    var aboutOpen by rememberSaveable { mutableStateOf(false) }
    val studio = settings.studioDefaults
    val appliedTag = AppCompatDelegate.getApplicationLocales().let { locales ->
        if (locales.isEmpty) null else locales.get(0)?.toLanguageTag()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.plinth_settings_title)) },
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
            SectionHeader(
                title = stringResource(R.string.plinth_settings_app),
                summary = stringResource(R.string.plinth_settings_app_note),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.plinth_settings_theme_title)) },
                supportingContent = { Text(stringResource(R.string.plinth_settings_theme_note)) },
                trailingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(settings.themeMode.labelResource()),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        NavigationChevron()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button) { themePickerOpen = true },
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_language_title)) },
                supportingContent = { Text(stringResource(R.string.plinth_settings_language_note)) },
                trailingContent = {
                    Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                    Text(
                        text = appliedTag?.let { languageDisplayName(it) }
                            ?: stringResource(R.string.settings_language_system),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    NavigationChevron()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button) { languagePickerOpen = true },
            )

            SwitchRow(
                title = stringResource(R.string.settings_haptics_title),
                supporting = stringResource(R.string.plinth_settings_haptics_note),
                checked = settings.hapticsEnabled,
                enabled = true,
                onCheckedChange = onHapticsChange,
            )

            SectionHeader(
                title = stringResource(R.string.plinth_settings_studio),
                summary = stringResource(R.string.plinth_settings_tryout_note),
            )
            val disclosureLabel = stringResource(
                if (imageDefaultsOpen) R.string.plinth_settings_hide_defaults
                else R.string.plinth_settings_show_defaults,
            )
            val disclosureState = stringResource(
                if (imageDefaultsOpen) R.string.plinth_settings_expanded
                else R.string.plinth_settings_collapsed,
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.plinth_settings_composition)) },
                supportingContent = {
                    Text(stringResource(R.string.plinth_settings_composition_note))
                },
                trailingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(disclosureLabel)
                        NavigationChevron()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics { stateDescription = disclosureState }
                    .clickable(
                        role = Role.Button,
                        onClickLabel = disclosureLabel,
                    ) { imageDefaultsOpen = !imageDefaultsOpen },
            )
            if (studioSaveError) {
                Text(
                    stringResource(R.string.plinth_settings_save_error),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            AnimatedVisibility(visible = imageDefaultsOpen) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    StudioPreview(studio, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    StudioControls(studio, onStudioDefaultsChange)
                    Text(
                        stringResource(R.string.plinth_settings_delivery),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    DeliveryNotes()
                }
            }

            SectionHeader(
                title = stringResource(R.string.plinth_settings_support),
                summary = stringResource(R.string.plinth_settings_support_note),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.about_title)) },
                supportingContent = { Text(stringResource(R.string.plinth_settings_about_note)) },
                trailingContent = { NavigationChevron() },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button) { aboutOpen = true },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.plinth_settings_contact)) },
                supportingContent = { Text(productSupportEmail) },
                trailingContent = { NavigationChevron() },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button) {
                        context.startActivity(
                            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$productSupportEmail")),
                        )
                    },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_rate_title)) },
                supportingContent = {
                    Text(stringResource(R.string.settings_product_review_summary))
                },
                trailingContent = { NavigationChevron() },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button) {
                        if (activity != null) {
                            InAppReview.requestReviewFromSettings(
                                activity = activity,
                                store = reviewStore,
                                scope = coroutineScope,
                            )
                        }
                    },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_product_privacy_title)) },
                supportingContent = {
                    Text(stringResource(R.string.settings_product_privacy_summary))
                },
                trailingContent = { NavigationChevron() },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button) {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(privacyPolicyUrl),
                            ),
                        )
                    },
            )

            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(R.string.plinth_settings_delete_request),
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                supportingContent = {
                    Text(stringResource(R.string.settings_product_delete_summary))
                },
                trailingContent = { NavigationChevron() },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button) {
                        val emailIntent = Intent(
                            Intent.ACTION_SENDTO,
                            Uri.parse("mailto:$productSupportEmail"),
                        ).apply {
                            putExtra(
                                Intent.EXTRA_SUBJECT,
                                deleteSubject,
                            )
                            putExtra(
                                Intent.EXTRA_TEXT,
                                deleteBody,
                            )
                        }
                        context.startActivity(emailIntent)
                    },
            )
        }
    }

    if (themePickerOpen) {
        AlertDialog(
            onDismissRequest = { themePickerOpen = false },
            title = { Text(stringResource(R.string.plinth_settings_theme_title)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .selectableGroup(),
                ) {
                    ThemeMode.entries.forEach { mode ->
                        PreferenceOptionRow(
                            label = stringResource(mode.labelResource()),
                            selected = settings.themeMode == mode,
                            onClick = {
                                themePickerOpen = false
                                onThemeModeChange(mode)
                            },
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { themePickerOpen = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (aboutOpen) {
        AlertDialog(
            onDismissRequest = { aboutOpen = false },
            title = { Text(stringResource(R.string.about_title)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(stringResource(R.string.about_body, stringResource(R.string.app_name)))
                    Text(stringResource(R.string.about_product_credit_model))
                    Text(stringResource(R.string.about_product_provenance))
                    Text(
                        stringResource(
                            R.string.about_product_version,
                            com.gexetr.plinthproductphotos.BuildConfig.VERSION_NAME,
                            com.gexetr.plinthproductphotos.BuildConfig.VERSION_CODE,
                        ),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { aboutOpen = false }) {
                    Text(stringResource(R.string.plinth_settings_close))
                }
            },
        )
    }

    if (languagePickerOpen) {
        val declaredTags = remember { declaredLanguageTags(context) }
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .selectableGroup(),
            ) {
                PreferenceOptionRow(
                    label = stringResource(R.string.settings_language_system),
                    selected = appliedTag == null,
                    onClick = { onSelect(null) },
                )
                for (tag in declaredTags) {
                    PreferenceOptionRow(
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
private fun PreferenceOptionRow(
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
private fun SectionHeader(title: String, summary: String) {
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HorizontalDivider()
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = PlinthTheme.accentInk,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
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

@Composable
private fun NavigationChevron() {
    val openLabel = stringResource(R.string.settings_open)
    Text(
        text = "›",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { contentDescription = openLabel },
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
    PlayKilnTheme(darkTheme = false) {
        SettingsScreen(
            settings = AppSettings(),
            onBack = {},
            onThemeModeChange = {},
            onHapticsChange = {},
            onStudioDefaultsChange = {},
        )
    }
}

@Preview(name = "Settings dark", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun SettingsScreenDarkPreview() {
    PlayKilnTheme(darkTheme = true) {
        SettingsScreen(
            settings = AppSettings(themeMode = ThemeMode.DARK,             hapticsEnabled = false),
                        onBack = {},
                        onThemeModeChange = {},
                        onHapticsChange = {},
            onStudioDefaultsChange = {},
        )
    }
}
