# Settings information architecture

Research read on 2026-10-16. `ON RECORD` means the named source states the quoted
pattern. `DECISION` identifies this slot's application of that evidence, not an
externally prescribed taxonomy. These are documented product patterns, not a
claim that a marketing study measured conversion for this screen.

## Group by subject, not by control type

**ON RECORD:** Android's [Organize your settings](https://developer.android.com/develop/ui/views/components/settings/organize-your-settings)
says a `PreferenceCategory` "displays a category title and visually separates the
category." The [Android Settings design guidelines](https://source.android.com/docs/core/settings/settings-guidelines)
say dividers "cluster settings in a group, rather than separating individual
settings" and to avoid vague titles like "General settings." **DECISION:** use
Appearance, New image defaults, Wallet & credits, and About & support, with
headings, group-level dividers and spacing. Reject the previous Studio / Delivery /
App split: it put billing, support and interface preferences in the same bucket.
These category names are our mapping of subjects, not a mandated Material 3 list.

## Use Material 3 components without misattributing older guidance

**ON RECORD:** the current [Material 3 lists page](https://m3.material.io/components/lists/guidelines)
identifies the lists component; its JavaScript-only body was not readable in this
research tool. Google's [Compose Material 3 API reference](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary)
is the component reference, while the readable grouping rules above come from
Android Settings and Android Preferences, not a new M3-specific settings pattern.
The [legacy Material settings pattern](https://m1.material.io/patterns/settings.html)
says settings should be "well-organized, predictable, and contain a manageable
number of options" and puts Help and account management outside preferences.
**DECISION:** retain Material 3 `ListItem` headline/supporting/trailing slots for
rows, `Switch` for independent booleans, and separate wallet/help groups instead
of presenting their actions as preferences. Reject the claim that legacy Material
guidance mandates About or billing inside an Appearance section.

## Learn the separation from two shipped creative products

**ON RECORD:** Adobe's [Lightroom on mobile settings and preferences](https://helpx.adobe.com/lightroom/mobile/get-started/lightroom-for-mobile-app-settings.html),
Android tab, names **Account**, **Premium Features**, **Preferences**, **Local
Storage**, and **Help & Support**. Its Preferences entry says "Allows you to
customize the Lightroom on mobile settings"; Help & Support "Provides access to
help resources, support tools, and troubleshooting options." Photoroom's
[Web App UI guide](https://help.photoroom.com/en/articles/13673310-quick-guide-to-the-photoroom-ui-web-app)
names **Manage Space: Manage your account, billing, and preferences**,
**Preferences: Choose your preferred default settings**, and **Main editor:
Fine-tune your images**. Its [Android export-default instructions](https://help.photoroom.com/en/articles/15202077-set-the-default-file-format-for-exports-android)
use **Content > Settings > Export format** and state "Your file format preference
only applies to you." **DECISION:** adopt the separation of app preferences,
creation defaults, money and help, not these products' account/subscription
features. The Photoroom Web labels are explicitly Web evidence; they are not
asserted to be Android section names. The primary HTML bodies were read directly
because simplified page extraction omitted Adobe's tabs and Photoroom's article.

## Keep the overview short and disclose the image controls

**ON RECORD:** [Android Settings guidelines](https://source.android.com/docs/core/settings/settings-guidelines)
say "Place frequently used settings at the top of the screen," warn that
"more than 10-15 items can be overwhelming," and permit progressive disclosure of
at least three infrequent items. They also say current values should be visible.
**DECISION:** Appearance is first, as requested by the owner; this is not a claim
that slot telemetry established usage frequency. Show one theme row with its
current value and a dialog, then dynamic colour, language and haptics. Collapse
the seven image defaults behind an explicit Show/Hide row; their preview scrolls
with them, never above unrelated app preferences. This keeps the initial overview
to eleven actionable rows. A local disclosure avoids needing a new navigation
destination or changing the shell. Reject keeping every slider and preview
permanently visible, which recreates the reported mixture.

## Distinguish defaults from editing or delivery

**ON RECORD:** Photoroom's [UI guide](https://help.photoroom.com/en/articles/13673310-quick-guide-to-the-photoroom-ui-web-app)
separates "preferred default settings" from the main editor where users refine
an image. **DECISION:** explicitly call this group **New image defaults**, explain
that it affects new editor sessions, and retain all seven existing persisted
controls and their reset. Put the preview and marketplace/export guidance inside
this group; delivery is explanatory content, not an app preference or a new
delivery switch. Existing cards and open sessions remain outside its stated scope.
Reject silently reclassifying image background as the app's colour theme.

## Theme and choice controls follow platform conventions

**ON RECORD:** Android's [dark theme guidance](https://developer.android.com/develop/ui/views/theming/darktheme)
recommends Light, Dark and "System default (the recommended default option)."
[Android Settings](https://source.android.com/docs/core/settings/settings-guidelines)
says radio choices belong in a dialog or separate selection screen, not mixed
alongside sliders and switches. Google's [Compose segmented-button guide](https://developer.android.com/develop/ui/compose/components/segmented-button)
describes choosing options "side-by-side" using `SingleChoiceSegmentedButtonRow`;
the [Compose switch guide](https://developer.android.com/develop/ui/compose/components/switch)
describes toggling between checked and unchecked states. **DECISION:** preserve
Follow system / Light / Dark in a theme dialog with radio semantics and preserve
boolean switches with a single row-sized toggle target. Reject side-by-side
segments for theme: a dialog also follows Android Settings and leaves room for
Russian labels and enlarged text. Dynamic colour remains disabled below Android
12, preserving the existing availability guard rather than promising support.

## Language and haptics are app behaviour, not image properties

**ON RECORD:** Android's [per-app language guide](https://developer.android.com/guide/topics/resources/app-languages)
states Android 13 adds centralized per-app language selection and equivalent
AndroidX APIs support older releases. It explicitly names
`AppCompatDelegate.setApplicationLocales` for existing pickers.
The [haptic feedback guide](https://developer.android.com/develop/ui/views/haptics/haptic-feedback)
says feedback methods "respect the user's touch feedback settings by default."
**DECISION:** keep the existing in-app AppCompat picker, including System default,
show the currently selected language, and keep the existing haptics callback.
Appearance's summary explicitly covers app language and touch feedback as well
as appearance. The haptics explanation says "when enabled on your device";
the app preference does not promise to override system settings. No language
storage or haptic implementation is replaced in this settings-only change.

## Money, help and rare actions come last

**ON RECORD:** Lightroom's [mobile settings reference](https://helpx.adobe.com/lightroom/mobile/get-started/lightroom-for-mobile-app-settings.html)
separates Account / Premium Features from Help & Support, and Android's
[settings guidance](https://source.android.com/docs/core/settings/settings-guidelines)
prioritizes frequent settings above infrequent ones. **DECISION:** put the existing
wallet destination in Wallet & credits; put About, contact support, rating and
the privacy policy in About & support. Keep the existing deletion-request email
last and label it as a request, not an immediate delete or an ordinary toggle.
No measured claim about usage or destructive-action ordering is attributed to
Google: last-place deletion is this slot's application of the rare-action pattern
and the owner's explicit requirement. About and theme/language dialogs remain
local to Settings; wallet keeps its existing navigation callback.

## Verification boundary

This is an information-architecture and presentation change. DataStore keys,
ViewModel writes, editor-default transforms, slider ranges, theme host, language
application and haptic reader are not changed. Gradle, unit-test execution and
device inspection belong to the orchestrator for this task. Source checks cannot
prove rendered spacing, large-font behaviour, persistence across process death,
or a successful external email/browser/review interaction.

Observed non-Gradle source check: importing `settingsFindings` from
`scripts/core/settings.mjs` and `localizationFindings` from
`scripts/core/localization.mjs` for this slot returned
`{"settings":[],"localization":[]}`. This is source evidence only, not a build,
unit-test result or visual inspection. No business-logic unit tests were changed:
the preference transformations and persistence logic remain unchanged.
