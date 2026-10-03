package com.gexetr.lawnroundlog.ui.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.gexetr.lawnroundlog.R
import com.gexetr.lawnroundlog.ui.AppearanceMode
import com.gexetr.lawnroundlog.ui.ScreenHeader
import java.util.Locale
import org.xmlpull.v1.XmlPullParser

/**
 * The settings surface: language, appearance, and the four preferences the rest of the app reads.
 *
 * It is a pure function of [settings] and its callbacks — no ViewModel is created here, because the
 * appearance has to be observed by the theme host and the only holder that can serve both is the
 * activity-scoped `SettingsViewModel` that `MainActivity` already owns. A second ViewModel inside this
 * route would be a second copy of one preference: one value, one owner.
 *
 * Nothing here names a DataStore type and no row writes a file. A preference is a callback, and the
 * callback lands in the ViewModel that owns the store — which is what keeps `persistence` a property
 * of the data layer rather than of this screen.
 */
@Composable
fun SettingsScreen(
    settings: SettingsUiState,
    problem: SettingsDraftProblem?,
    onAppearanceChange: (AppearanceMode) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onDefaultAmountChange: (String) -> Unit,
    onConfirmBeforeDeleteChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // Whether the in-app language picker is open. `rememberSaveable` because applying a language makes
    // AppCompat recreate the activity to re-read the resources, and a dialog that vanished under the
    // operator mid-choice would look like a tap that did nothing.
    var languagePickerOpen by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            // The IME is its own inset and `Scaffold` does not carry it. Two rows below are text
            // fields, and `adjustResize` is what makes the window shrink for the keyboard at all, so
            // without this the field being typed into can sit underneath it.
            .imePadding(),
    ) {
        ScreenHeader(
            title = stringResource(R.string.settings_title),
            onBack = onBack,
        )

        LazyColumn(
            // `weight` takes the height left below the header rather than the whole column, and the
            // navigation-bar inset ends the list viewport above the system bar. Without both, the last
            // rows — language and the two behaviour toggles — were laid out under the 48 dp three-button
            // bar and could not be read or tapped.
            modifier = Modifier.weight(1f).navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                SectionHeading(stringResource(R.string.settings_appearance_header))
            }
            item {
                AppearanceChoice(
                    selected = settings.appearance,
                    onSelect = onAppearanceChange,
                )
            }
            item {
                SupportingLine(
                    text = stringResource(R.string.settings_appearance_note),
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }

            item {
                SectionHeading(stringResource(R.string.settings_language_header))
            }
            item {
                ActionRow(
                    title = stringResource(R.string.settings_language),
                    body = stringResource(R.string.settings_language_summary),
                    onClick = { languagePickerOpen = true },
                )
            }

            item {
                SectionHeading(stringResource(R.string.settings_behaviour_header))
            }
            item {
                TextPreference(
                    title = stringResource(R.string.settings_currency),
                    body = stringResource(R.string.settings_currency_summary),
                    storedValue = settings.currencySymbol,
                    commit = onCurrencyChange,
                    error = if (problem == SettingsDraftProblem.CURRENCY_TOO_LONG) {
                        stringResource(R.string.settings_currency_error)
                    } else {
                        null
                    },
                )
            }
            item {
                TextPreference(
                    title = stringResource(R.string.settings_default_amount),
                    body = stringResource(R.string.settings_default_amount_summary),
                    storedValue = formatAmount(settings.defaultVisitAmountUsd),
                    commit = onDefaultAmountChange,
                    error = if (problem == SettingsDraftProblem.AMOUNT_NOT_A_NUMBER) {
                        stringResource(R.string.settings_amount_error)
                    } else {
                        null
                    },
                    keyboardType = KeyboardType.Decimal,
                )
            }
            item {
                ToggleRow(
                    title = stringResource(R.string.settings_confirm_delete),
                    body = stringResource(R.string.settings_confirm_delete_summary),
                    checked = settings.confirmBeforeDelete,
                    onCheckedChange = onConfirmBeforeDeleteChange,
                )
            }
            item {
                ToggleRow(
                    title = stringResource(R.string.settings_haptics),
                    body = stringResource(R.string.settings_haptics_summary),
                    checked = settings.hapticsEnabled,
                    onCheckedChange = onHapticsChange,
                )
            }
        }
    }

    if (languagePickerOpen) {
        // The declared tags are parsed once per opening rather than on every recomposition, and the
        // currently-applied tag is read from the one place that actually holds it — the AppCompat
        // runtime — so the dialog opens with the right row already selected.
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

/**
 * The appearance choice, as three radio rows rather than a segmented control.
 *
 * A segmented button divides its width between its segments, and these labels are one to three words
 * in every language this slot ships; at a large font scale that is exactly the container that cannot
 * grow, and the fix would be to shrink the type to fit — which is the defect, not the fix. A stacked
 * row per option wraps, keeps its 48 dp target, and reads the same to a screen reader.
 */
@Composable
private fun AppearanceChoice(
    selected: AppearanceMode,
    onSelect: (AppearanceMode) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        for (mode in AppearanceMode.entries) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selected == mode,
                        role = Role.RadioButton,
                        onClick = { onSelect(mode) },
                    )
                    .heightIn(min = 48.dp)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RadioButton(
                    selected = selected == mode,
                    // The row carries the click and the name. A second target for one choice is a
                    // second thing for TalkBack to announce and a second way to get it wrong.
                    onClick = null,
                )
                Text(
                    text = stringResource(mode.labelRes),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

/**
 * Picks one of the languages this app declares, without leaving the app.
 *
 * [appliedTag] is what the AppCompat runtime currently holds: `null` means "follow the phone", which
 * is the first row. Every row is one radio target at 48 dp, the same shape as [AppearanceChoice] — a
 * stacked row per option wraps at any font scale and reads the same to a screen reader, where a
 * segmented control would have to shrink the type to fit.
 */
@Composable
private fun LanguagePickerDialog(
    declaredTags: List<String>,
    appliedTag: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_language)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                LanguageOptionRow(
                    label = stringResource(R.string.settings_language_system),
                    selected = appliedTag == null,
                    onClick = { onSelect(null) },
                )
                for (tag in declaredTags) {
                    // A computed value, never a literal: a language's name in its own language is a
                    // fact the platform's locale data carries, and hardcoding it would be a string to
                    // translate into every language this app ever adds.
                    val label = languageDisplayName(tag)
                    LanguageOptionRow(
                        label = label,
                        selected = appliedTag == tag,
                        onClick = { onSelect(tag) },
                    )
                }
            }
        },
        // Each row applies its own choice, so there is no separate confirm step. Cancel is the only
        // other way out, and it changes nothing.
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
    )
}

/** One selectable language row: the whole row is the target, and the radio is state, not a button. */
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
        RadioButton(
            selected = selected,
            // The row carries the click and the name, exactly as the appearance rows do.
            onClick = null,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

/** A row that opens something else, with a body line saying what it opens. */
@Composable
private fun ActionRow(
    title: String,
    body: String,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .heightIn(min = 48.dp)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                SupportingLine(body)
            }
        }
        HorizontalDivider()
    }
}

/**
 * A preference whose value is typed, with the message the ViewModel rejected an edit with.
 *
 * The field keeps a local draft and commits it when it loses focus or the IME action fires, rather
 * than writing on every keystroke. That is not an optimisation: the value round-trips through
 * DataStore, so a write per character means the field is re-set from a stored value that is one
 * keystroke behind and a fast typist watches characters disappear.
 *
 * The draft is replaced only when the *stored* value actually changes, which is what keeps a rejected
 * edit on screen: the operator sees the text he typed and the reason it was not accepted, instead of
 * a field that silently reverts to the old value under the error message.
 */
@Composable
private fun TextPreference(
    title: String,
    body: String,
    storedValue: String,
    commit: (String) -> Unit,
    error: String?,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    var draft by rememberSaveable { mutableStateOf(storedValue) }
    var lastStored by rememberSaveable { mutableStateOf(storedValue) }

    LaunchedEffect(storedValue) {
        if (storedValue != lastStored) {
            lastStored = storedValue
            draft = storedValue
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SupportingLine(body)
        OutlinedTextField(
            value = draft,
            onValueChange = { typed ->
                draft = typed
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focus ->
                    // Committing on focus loss rather than per keystroke: leaving the field, opening
                    // another row, or closing the keyboard all commit exactly once. The equality test
                    // is what keeps the first composition — which reports an unfocused field — from
                    // writing the fallback value over the stored one.
                    if (!focus.isFocused && draft != lastStored) commit(draft)
                },
            label = { Text(title) },
            isError = error != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { commit(draft) },
            ),
        )
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** A preference that is on or off, where the whole row is the target rather than only the switch. */
@Composable
private fun ToggleRow(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { onCheckedChange(!checked) })
                .heightIn(min = 48.dp)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // `weight` rather than `fillMaxWidth`: the label column has to leave the switch its own
            // width and take whatever is left, in that order.
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                SupportingLine(body)
            }
            Switch(
                checked = checked,
                // A switch is state, not a second action: the row already announces and toggles it.
                onCheckedChange = onCheckedChange,
            )
        }
        HorizontalDivider()
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SupportingLine(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** `12.5` rather than `12.50`: this is a number the operator edits, not a price on a receipt. */
private fun formatAmount(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

/**
 * Switches the app's own language, leaving the phone's language alone.
 *
 * A null [languageTag] means "follow the phone". `AppCompatDelegate` is the published backport: on
 * Android 13+ it forwards to the platform's per-app locale, and below it AppCompat stores the choice
 * and re-applies it before the first composition. The system settings screen is deliberately NOT
 * opened: the operator asked this app to change language, and sending him to a device-wide screen to
 * change every app he owns is a different request.
 */
internal fun applyAppLanguage(languageTag: String?) {
    val locales = if (languageTag == null) {
        LocaleListCompat.getEmptyLocaleList()
    } else {
        LocaleListCompat.forLanguageTags(languageTag)
    }
    AppCompatDelegate.setApplicationLocales(locales)
}

/**
 * The language tags this app declares, read from `res/xml/locales_config.xml`.
 *
 * That file is the single declaration of the app's languages: `android:localeConfig` is what the
 * system, the launcher and Play all read, so the picker is built from it rather than from a second
 * list in Kotlin that could silently disagree with it.
 *
 * `runCatching` is not decoration: a device or managed profile that refuses the resource would
 * otherwise crash the settings screen, and a picker offering only "System default" is better than a
 * screen that dies.
 */
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

/** A language's own name ("English", "Русский"), title-cased, from the platform's own locale data. */
private fun languageDisplayName(tag: String): String {
    val locale = Locale.forLanguageTag(tag)
    return locale.getDisplayName(locale).replaceFirstChar { first -> first.titlecase(locale) }
}

/** The `android:` namespace every attribute in `res/xml/` is written in. */
private const val ANDROID_NAMESPACE: String = "http://schemas.android.com/apk/res/android"

/** The element `res/xml/locales_config.xml` declares each language with. */
private const val LOCALE_TAG: String = "locale"

/** The attribute `res/xml/locales_config.xml` names each language tag in. */
private const val LOCALE_NAME_ATTRIBUTE: String = "name"
