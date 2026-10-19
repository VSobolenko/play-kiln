# Settings control affordances

Settings now shows each row's interaction type in its trailing control. Picker and
navigation rows show a value or action label plus a chevron; preference toggles
retain a switch. Clickable rows keep a minimum 48 dp height and expose a button
role. The deletion-request action uses the error colour to distinguish it from
ordinary navigation.

| Row | Before | After |
|---|---|---|
| App theme | Current value and explanation in supporting text; no trailing cue | Explanation below the label; selected mode and chevron at the trailing edge |
| Wallpaper colours | Switch | Switch, still distinct from app theme |
| App language | Current language in supporting text; no trailing cue | Language explanation below the label; selected language and chevron at the trailing edge |
| Vibration feedback | Switch | Switch |
| Image composition defaults | Show/Hide text | Show/Hide text and chevron |
| Wallet & credits | Tappable wallet-opening row | Removed from Settings; purchasing remains outside Settings |
| About this app | No trailing cue | Chevron |
| Contact support | No trailing cue | Chevron |
| Rate app | No trailing cue | Chevron |
| Privacy policy | No trailing cue | Chevron |
| Request data deletion | No trailing cue | Error-coloured action label and chevron |

The theme picker and exported-image composition/background settings remain
separate controls. The wallet section was removed with its only action, so no
empty “Wallet & credits” heading remains. The new generic “Open” accessibility
label is translated in English and Russian.
