# Night glance

## Purpose

Give Lumintra a presence on the phone between sessions. A session planner is used episodically —
opened when the sky looks promising, then not opened for a week — and this capability is the two
surfaces that reach the user in that gap: a home-screen widget that states tonight's dark window, the
Moon and the cloud outlook for the saved spot without opening anything, and one opt-in, local
notification at the start of that dark window for a user who asked to be told. Both read only state
that is already on the device; neither ever fetches, because the weather source's own terms forbid a
mobile app from retrieving data while it is not in use, and because a widget or a push that announced a
stale number as a current one is worse than no widget at all.

## ADDED Requirements

### Requirement: The widget states tonight's four facts for one spot, or an honest state instead

The home-screen widget SHALL be a single `GlanceAppWidget` — `widget/NightGlanceWidget.kt` under the
slot's root package — registered in the manifest by a `GlanceAppWidgetReceiver`
(`widget/NightGlanceWidgetReceiver.kt`) with its `appwidget-provider` XML, and it SHALL render only
from persisted local state. For a spot whose tonight brief has been composed it SHALL state, together
and without opening the app:

1. the **spot's name** (the widget's own selected spot, defaulting to the most recently used spot);
2. tonight's **dark window** as its two bounds and the spot's zone abbreviation, e.g.
   `23:04–02:51 CEST`, or — for a night with no astronomical darkness — the same honest sentence the
   `session-brief` and `night-scene` capabilities use for that `DarkWindowKind`, abbreviated to a
   widget-sized string rather than replaced by a fabricated window or a blank;
3. the **Moon's state**: its illuminated fraction as a whole percentage `0–100 %` and whether it is up
   or down during the dark window, in one of the typed forms *up all night*, *down all night*, *rises*
   or *sets*;
4. the **cloud outlook** as the last cached retrieval, always with its age, e.g. `20 % cloud ·
   forecast 3 h old`, or the typed `no forecast` string when no reading exists.

The widget SHALL NOT carry a **target** row at any size, so a saved spot with no target chosen behaves
exactly as one with a target and the absence of a target changes nothing on the widget. All times
SHALL be rendered in the **spot's** IANA zone, never the device's, with the zone's abbreviation
beside them. The widget SHALL have exactly these states and no other: **no spot saved** →
`Set a spot in Lumintra to see tonight`; **a spot saved but no brief composed for tonight** (including
the case where the app has never been opened) → `Open Lumintra to see tonight's dark window`; **a brief
composed for a night other than tonight** → `Tonight's plan isn't ready — open Lumintra`, never
yesterday's window presented as tonight's; and **a brief composed for tonight** → the four facts
above. In no state SHALL the widget render an empty tile, a placeholder dash, or a value without the
input age it came from.

#### Scenario: The widget is a Glance widget with its own receiver

- **WHEN** the widget's sources and the manifest are read
- **THEN** the widget is a subclass of `GlanceAppWidget` at `widget/NightGlanceWidget.kt`, it is
  registered by a `GlanceAppWidgetReceiver` at `widget/NightGlanceWidgetReceiver.kt` declared in
  `AndroidManifest.xml` with an `appwidget-provider` XML, and no `AppWidgetProvider`-only path exists

#### Scenario: A composed brief shows all four facts in the spot's zone

- **WHEN** the widget is rendered for a spot with a composed brief carrying a dark window, a Moon
  state and a cached cloud reading
- **THEN** the spot's name, the dark window's two bounds with the spot's zone abbreviation, the Moon's
  percentage with its up/down form, and the cloud percentage with its age are all present, and every
  time is the spot's zone rather than the device's

#### Scenario: A night with no astronomical darkness keeps the honest sentence

- **WHEN** tonight's brief at the spot is `NO_ASTRO_DARK`
- **THEN** the widget shows the no-astronomical-darkness sentence for that spot instead of a dark
  window, and it shows no fabricated `00:00–00:00` window and no black-sky claim

#### Scenario: No spot saved invites the user, and fabricates nothing

- **WHEN** the widget is placed with no saved spot
- **THEN** it shows `Set a spot in Lumintra to see tonight` and computes no sky for a default or
  `(0, 0)` position

#### Scenario: The app has never been opened

- **WHEN** the widget is placed on a fresh install and the app has never been launched
- **THEN** the widget's receiver starts, shows `Open Lumintra to see tonight's dark window`, and does
  not crash, spin, or show an empty tile

#### Scenario: Another night's brief is never shown as tonight's

- **WHEN** the persisted brief's local-date label is not tonight's local date at the spot
- **THEN** the widget shows `Tonight's plan isn't ready — open Lumintra` rather than the previous
  night's dark window, Moon or cloud reading

#### Scenario: No target changes nothing

- **WHEN** the widget is rendered for a spot with no target chosen
- **THEN** the rendered widget is identical to the one for the same spot and night with a target
  chosen, because the widget carries no target row

#### Scenario: No cloud reading is stated, not faked

- **WHEN** the app has never fetched a forecast for the spot
- **THEN** the widget's cloud line reads `no forecast` and shows no number and no percentage

### Requirement: The widget reads the locally cached brief and never performs a network request

The widget SHALL render from a persisted tonight snapshot written by the app when it composes the brief
(`widget/NightGlanceState.kt`: the selected spot's name and `tzId`, the session's local-date label, the
`DarkWindowKind` and the dark-window bounds as absolute UTC instants, the Moon's illuminated fraction
and its up/down form, the cached cloud fraction with its `fetchedAtUtc`, and the snapshot's own
`computedAtUtc`). The widget's code path SHALL contain **no network request of any kind** — no HTTP
client, no `WorkManager` or `JobScheduler` fetch, no foreground service, no `OkHttp`/`HttpURLConnection`
call and no socket — and SHALL NOT start any component whose purpose is retrieval. When the persisted
snapshot is absent, incomplete for tonight, or its input older than the reading it carries, the widget
SHALL render the typed state for that condition rather than reaching for fresh data.

The reason is recorded because it is the difference between a compliant widget and a policy problem:
MET Norway's terms state that *"Applications on mobile devices must not retrieve new data as long as
the application is not in use"*, and a home-screen widget updates while the app is not in use by
definition. A widget that fetched would therefore breach the forecast source's terms on every periodic
update, on a paid app whose only weather is that source.

#### Scenario: No network call exists on the widget's path

- **WHEN** the widget's sources are read for network use
- **THEN** the widget contains no HTTP client, no socket, no download and no task whose purpose is
  retrieval, and the only input the draw reads is the persisted snapshot

#### Scenario: A widget update while the app is not running fetches nothing

- **WHEN** the platform delivers a periodic update with the app process not running
- **THEN** the widget renders from the persisted snapshot, and no request to `api.met.no` or any other
  host is made — the device's network counters are unchanged by the update

#### Scenario: A missing or incomplete snapshot degrades, never fetches

- **WHEN** the persisted snapshot is absent or carries no dark window for tonight
- **THEN** the widget renders its typed not-ready state and performs no retrieval to fill the gap

#### Scenario: The forecast row is only ever a cached reading with its age

- **WHEN** the widget shows a cloud figure
- **THEN** that figure is the persisted reading, its retrieval time is stated as an age, and the value
  is labelled a forecast rather than a measurement

### Requirement: The widget's updates are inexact, and a stale reading is never called current

The widget SHALL be refreshed by exactly three triggers and no others:

1. **A periodic update**, declared as `updatePeriodMillis = 3_600_000` ms (1 hour) in the
   `appwidget-provider` XML. The platform enforces a floor of 1 800 000 ms (30 minutes) and this
   value sits above it deliberately; the platform MAY delay a periodic update, MAY deliver it less
   often than the declared period, MAY skip it entirely when the device is asleep and deliver it on
   the next wake, and MAY batch it across widgets, so the widget SHALL NOT depend on it for
   correctness;
2. **An explicit update after the app recomputes**, when the app writes a new tonight snapshot it
   SHALL push the widget (`AppWidgetManager.updateAppWidget` for the widget's ids, or the Glance
   equivalent), so a user who has just planned a session sees the widget agree immediately;
3. **An update after a reboot**, which the AppWidget framework requests when it re-binds the provider;
   the app SHALL NOT declare a `BOOT_COMPLETED` receiver for the widget and SHALL NOT depend on that
   re-request, because the widget is renderable from persisted state at any moment.

Because the periodic trigger is inexact, the widget SHALL NOT claim any time is current unless it is:
it SHALL date the cloud figure with the reading's age, it SHALL distinguish tonight's snapshot from a
previous night's (above), and when the snapshot is older than the night it describes it SHALL show the
not-ready state. The widget SHALL always label the **age** of a persisted cloud reading and SHALL NEVER
drop that age to fit a smaller layout; at a size that cannot carry both a cloud number and its age, the
cloud figure SHALL be omitted rather than shown bare.

#### Scenario: The periodic interval is above the platform floor

- **WHEN** the `appwidget-provider` XML is read
- **THEN** `updatePeriodMillis` is `3600000` and is therefore at or above the platform's 1800000 ms
  minimum

#### Scenario: A delayed periodic update cannot make the widget lie

- **WHEN** a periodic update is delivered 6 hours after the previous one
- **THEN** the widget shows the cloud reading with its age of 6 hours and the snapshot's own age, and
  nothing on the widget reads as a current or freshly measured value

#### Scenario: A stale night is a state, not a silent carry-over

- **WHEN** the persisted snapshot describes a night that has ended
- **THEN** the widget shows the not-ready state and does not display that night's dark window as though
  it were tonight's

#### Scenario: A recomputation pushes the widget

- **WHEN** the app composes a new tonight brief and writes the snapshot
- **THEN** it requests an update for the widget's ids in the same run, so the widget no longer shows
  the previous reading

#### Scenario: The reboot refresh is not relied upon

- **WHEN** the device is restarted
- **THEN** the widget renders from persisted state on the platform's next update request, the app
  declares no `BOOT_COMPLETED` receiver for it, and a missing update leaves the last snapshot on screen
  with its age rather than an empty tile

#### Scenario: A size that cannot carry the age omits the cloud

- **WHEN** the widget is rendered at a size whose text budget cannot hold both the cloud fraction and
  its age
- **THEN** the cloud line is absent, and no cloud number is rendered without its age

### Requirement: The widget carries the app's own identity across three palette states and two size classes

The widget SHALL be styled from the slot's own identity and SHALL NOT be a stock layout: it SHALL draw
the slot's own mark (`ui/components/LumintraMark.kt`) and use the colours declared for the slot, so a
widget is recognisably Lumintra's rather than a default `Surface` with system text. It SHALL render
correctly in **three palette states** — the **light** theme, the **dark** theme, and the **red night
mode** — and in **red night mode** it SHALL take its colours from the same declared red palette
(`ui/theme/NightVisionPalette.kt`) the app uses, with blue `= 0` and green `≤ 8/255` on every colour,
as the `night-scene` capability requires of every surface including this one.

The widget SHALL declare exactly **two size classes** and SHALL state what each carries:

| Size class | Cells | Approximate size | Shows |
|---|---|---|---|
| **Wide** | 4 × 2 | 250 × 110 dp | the spot, the dark window (or the no-darkness sentence), the Moon's state, and the cloud with its age |
| **Compact** | 2 × 2 | 110 × 110 dp | the spot (short), the dark window (or the no-darkness sentence), and the Moon's state — the cloud is omitted, because a 2 × 2 tile cannot carry both a cloud figure and its age at 12 sp and a bare figure is forbidden |

The widget SHALL declare these two sizes through a responsive size mode with exactly the two
`DpSize` values `250 × 110 dp` and `110 × 110 dp`, SHALL declare `targetCellWidth="4"` /
`targetCellHeight="2"` and `resizeMode` allowing both axes in the `appwidget-provider` XML, and SHALL
provide a **preview** (a `previewImage`/`previewLayout` for the widget picker plus a
`providePreview` render) that shows the wide size in the default theme, carrying the slot's identity
rather than a grey placeholder. The whole widget SHALL be one tap target that opens the app at the
`Brief` for that spot and tonight, or at `SpotEdit` when no spot exists.

#### Scenario: Three palette states render

- **WHEN** the widget is rendered with the system in light mode, in dark mode, and with red night mode
  on
- **THEN** all three render legibly, and in red night mode every widget colour has blue `= 0` and
  green `≤ 8/255`

#### Scenario: Two size classes, each stated

- **WHEN** the widget is placed at 4 × 2 and at 2 × 2
- **THEN** the wide size shows the four facts and the compact size shows the spot, the dark window and
  the Moon with no cloud figure, and neither size renders a bare cloud number

#### Scenario: The widget is Lumintra's, not a stock layout

- **WHEN** the widget's rendered output is compared with a default `Surface` and system text
- **THEN** it draws the slot's own mark and uses the slot's declared colours, so it is distinguishable
  from an unstyled widget

#### Scenario: A preview exists and carries the identity

- **WHEN** the widget picker is opened
- **THEN** the widget shows a preview of the wide size in the slot's colours, not a blank or generic
  tile

#### Scenario: The widget is one tap target

- **WHEN** the user taps anywhere on the widget with a spot saved
- **THEN** the app opens at the `Brief` for that spot and tonight; with no spot saved it opens
  `SpotEdit`

### Requirement: The reminder is one opt-in local notification at the start of the dark window (E111)

The reminder SHALL be **off until the user turns it on**, SHALL be a single notification rather than a
stream, and SHALL be armed by an explicit user action and never by launch, onboarding or a first-run
flow. The enabling control SHALL be a labelled control on the `Brief` screen
(`Remind me tonight`), and the same preference SHALL have an equivalent row in the `Settings` screen
(`ui/settings/SettingsScreen.kt`) that changes the same stored value in `data/SettingsStore.kt`. The
notification SHALL be scheduled for the **start of tonight's dark window at the saved spot** — the
absolute UTC instant the brief computes — and SHALL be delivered by `WorkManager` as one
`OneTimeWorkRequest` with an initial delay to that instant, enqueued under a single unique work name
with a replace policy so at most one reminder is ever pending; the notification SHALL be posted
through a `NotificationChannel` created on `NotificationManagerCompat` with a fixed channel id and a
fixed notification id, so a re-post replaces rather than stacks.

The notification's content SHALL be built entirely from state already on the device and SHALL carry:
the **spot's name**; the **dark window** bounds in the spot's zone; the **Moon's state** (illuminated
fraction and its up/down form); and, when a cloud reading exists, the **cloud cover as of its last
cached retrieval, with that retrieval time, labelled a forecast**. The notification's title SHALL be
the statement of the dark window's start, e.g. `Dark window begins at 23:04`; it SHALL NOT be an
instruction and SHALL NOT promise clear skies. Tapping the notification SHALL open the `Brief` for
that spot and tonight, through one `PendingIntent` declared `FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT`,
and the notification SHALL carry no action button that dismisses the reminder or performs a second
job. When the plan was composed offline, the notification SHALL say so rather than implying a fresh
forecast.

#### Scenario: The reminder is off until the user enables it

- **WHEN** the app is installed and opened for the first time
- **THEN** no reminder is scheduled, no notification permission is requested, and the enabling control
  reads as off

#### Scenario: Enabling it arms one reminder at the dark window's start

- **WHEN** the user enables `Remind me tonight` for a spot whose dark window starts at a known instant
- **THEN** exactly one pending work item exists, its delay resolves to that start instant, and no
  second reminder is pending

#### Scenario: The notification is scheduled by WorkManager and posted through a channel

- **WHEN** the reminder's sources are read
- **THEN** the schedule is a `WorkManager` `OneTimeWorkRequest` enqueued under one unique work name,
  and the notification is created on a `NotificationChannel` and posted with
  `NotificationManagerCompat`

#### Scenario: The notification states the spot, the window, the Moon and the cached cloud

- **WHEN** the reminder fires for a spot with a dark window, a Moon state and a cached cloud reading
- **THEN** the notification names the spot, both dark-window bounds in the spot's zone, the Moon's
  fraction and up/down form, and the cloud cover with its retrieval time labelled a forecast

#### Scenario: One tap target opens tonight's brief

- **WHEN** the user taps the notification
- **THEN** the app opens at the `Brief` for that spot and tonight, and the notification carries no
  second action

#### Scenario: An offline plan says so

- **WHEN** the brief the reminder is built from was computed without a network
- **THEN** the notification states that the plan was computed offline and does not present an absent
  cloud figure as a fresh one

#### Scenario: A re-post replaces rather than stacks

- **WHEN** the reminder is posted twice for the same night
- **THEN** one notification is present, because the channel id and the notification id are fixed

### Requirement: The reminder states the dark window and never promises clear skies

Every string the reminder renders SHALL be a **statement about the dark window**, not an instruction
and not a promise about the weather: the title SHALL name the dark-window start (`Dark window begins
at {time}`), the body SHALL state the spot and the window's bounds, and any cloud figure SHALL be
labelled a **forecast with its retrieval time**. The reminder SHALL NOT contain copy of the form
"clear tonight", "perfect conditions", "go outside now", "the sky will be clear" or any equivalent,
and SHALL NOT use an urgency or alarm tone; the sky can cloud over between the reading and the window,
and the forecast is total cloud only with no low/mid/high split, so a reminder that promised clarity
would be a claim the data cannot support.

The reminder SHALL be scheduled as an **inexact** delivery and SHALL NOT be an exact alarm. The app
SHALL declare neither `SCHEDULE_EXACT_ALARM` nor `USE_EXACT_ALARM`, SHALL contain no
`AlarmManager.setExact*` and no `AlarmManager.setAlarmClock` call, and SHALL NOT request the
exact-alarm special access. An inexact delivery is correct and loses nothing measurable here, because
the dark window is precise to the minute at best — it is derived from the sky, and the cloud over it
is a forecast — so a reminder delivered within the window is still a true reminder, while an exact
alarm would be a restricted permission taken for a precision the product does not have.

#### Scenario: No string promises clear skies or gives an order

- **WHEN** every reminder string in both resource files is read
- **THEN** each is a statement of the dark window or of the cached forecast, and none promises clear
  skies, none commands the user, and none uses an urgency tone

#### Scenario: The cloud figure is labelled a forecast with its time

- **WHEN** the notification shows a cloud value
- **THEN** the value is accompanied by the word for forecast and the reading's retrieval time

#### Scenario: No exact-alarm permission is declared

- **WHEN** the merged `AndroidManifest.xml` is read
- **THEN** it declares neither `SCHEDULE_EXACT_ALARM` nor `USE_EXACT_ALARM`

#### Scenario: No exact-alarm call exists in the sources

- **WHEN** the slot's sources are scanned for exact-scheduling calls
- **THEN** no `AlarmManager.setExact`, `setExactAndAllowWhileIdle` or `setAlarmClock` call exists, and
  the reminder's delivery is the inexact `WorkManager` one-time work

#### Scenario: A late delivery is still honest

- **WHEN** the reminder is delivered a few minutes after the dark-window start because the platform
  deferred it
- **THEN** the notification still states the window's real bounds and its true start instant, and it
  does not claim to have fired at the exact minute

### Requirement: The reminder's lifecycle re-arms, cancels and leaves nothing scheduled (E109)

The app SHALL keep **at most one** pending reminder and SHALL re-derive it (cancel the unique work and
re-enqueue if the target instant changed) on **every app open** and whenever the spot, the chosen
target or the plan changes, so a reminder can never fire for a plan the user has since changed. When
the derived dark-window start already lies in the past the app SHALL NOT enqueue it — a notification
for a window that has ended is wrong — and SHALL arm the next future start instead if the preference is
on. The reminder SHALL survive a **reboot**: `WorkManager` persists its queue and reschedules it after
the device restarts, so the app SHALL declare no `BOOT_COMPLETED` receiver of its own for it. A
**device time zone change** SHALL NOT move the armed instant, because the dark-window start is an
absolute instant and the spot's zone is a property of the spot; the displayed times SHALL stay in the
spot's zone, and the app SHALL re-derive on its next open. A **date change** SHALL be handled the same
way — the previous night's reminder is consumed, and the next night's is armed on the next open — and
the app SHALL NOT schedule a repeating work, a periodic alarm or a chain that fires without a user
open. At **fire time** the worker SHALL post only when the current instant lies inside the persisted
dark window `[start, end)`; outside that interval it SHALL post nothing and mark the reminder as
needing re-derivation, so a delivery displaced by a reboot or a clock change never announces a window
that is over. If the persisted night's `DarkWindowKind` is not `DARK`, the worker SHALL post nothing.
Turning the reminder **off** SHALL cancel the unique work and cancel any notification already posted,
leaving nothing scheduled.

#### Scenario: A spot change re-derives the reminder

- **WHEN** the user changes the spot while the reminder is on
- **THEN** the pending work is replaced with one for the new spot's dark-window start, and no reminder
  for the previous spot remains scheduled

#### Scenario: A past instant is never scheduled

- **WHEN** the derived dark-window start is already in the past
- **THEN** the app enqueues no reminder for it and arms the next future start instead

#### Scenario: The reminder survives a reboot

- **WHEN** a reminder is armed and the device is restarted
- **THEN** the reminder is still pending after the restart, delivered by `WorkManager`'s own
  rescheduling, and the app declares no `BOOT_COMPLETED` receiver for it

#### Scenario: A time zone change does not move the armed instant

- **WHEN** the device's time zone changes while a reminder is armed
- **THEN** the armed instant is unchanged, the notification's displayed times remain in the spot's
  zone, and the app re-derives on its next open

#### Scenario: A date change arms the next night, not a repeat

- **WHEN** the armed night passes and the user opens the app again with the reminder on
- **THEN** the app arms the next future dark-window start and schedules no repeating or periodic work

#### Scenario: A late delivery for a finished window posts nothing

- **WHEN** the worker runs and the current instant is after the persisted dark window's end
- **THEN** no notification is posted and the reminder is marked for re-derivation

#### Scenario: A night with no dark window posts nothing

- **WHEN** the persisted `DarkWindowKind` is not `DARK`
- **THEN** the worker posts nothing and the app does not offer or arm a reminder for that night

#### Scenario: Turning it off leaves nothing scheduled

- **WHEN** the user turns the reminder off
- **THEN** the unique work is cancelled, any posted notification is cancelled, and no reminder remains
  scheduled

### Requirement: The notification permission is asked only at the reminder, and the app is complete without it

`POST_NOTIFICATIONS` — a dangerous permission introduced in Android 13 / API 33 — SHALL be declared
because this capability was approved, and the app SHALL request it **only** when the user opts into
the reminder, from the `Activity` hosting the enabling control, through
`ActivityResultContracts.RequestPermission`, and never at launch, never on first run and never during
onboarding. On API 26–32 the permission does not exist as a runtime grant; the app SHALL still create
the `NotificationChannel` and the reminder works there without a prompt. The reminder's
`NotificationChannel` SHALL be created before the first post and its importance SHALL be
`IMPORTANCE_DEFAULT`, chosen so the notification is noticeable without a full-screen intent, a custom
sound or a vibration pattern of the app's own. The reminder SHALL request the permission only; the
widget renders on the home screen and requires no notification permission and declares none. The app
SHALL show one explanatory line before the system dialog and SHALL NOT show the system dialog more
than once per enable action. **Declining the permission SHALL be a normal state, not an error**: the
reminder control SHALL read as off, the rest of the app SHALL be fully usable, and no feature SHALL be
gated on the grant. The app SHALL declare and request **no location permission of any kind** for either
surface — not `ACCESS_COARSE_LOCATION`, not `ACCESS_FINE_LOCATION`, not `ACCESS_BACKGROUND_LOCATION`:
the spot's coordinates are already saved on the device, and a manual coordinate, the app's own coordinate picker and
`Geocoder` all work without one.

#### Scenario: The permission is asked only at enable

- **WHEN** the app is launched and used with the reminder left off
- **THEN** no `POST_NOTIFICATIONS` request is made, and the request appears only after the user
  enables the reminder on API 33 or higher

#### Scenario: Declining is a normal state

- **WHEN** the user declines `POST_NOTIFICATIONS`
- **THEN** the reminder control reads off, the rest of the app is fully usable, nothing is scheduled,
  and no feature is blocked behind the grant

#### Scenario: Declining is explainable and reversible

- **WHEN** the user has declined the permission and later wants the reminder
- **THEN** the app states in one line that notifications are off for Lumintra and offers a control
  that opens the app's notification settings, and enabling there makes the reminder work without a
  reinstall

#### Scenario: The channel is created with the stated importance

- **WHEN** the notification channel is created
- **THEN** its id is fixed, its name and description are string resources in both locales, and its
  importance is `IMPORTANCE_DEFAULT`

#### Scenario: No location permission is involved

- **WHEN** the merged `AndroidManifest.xml` is read
- **THEN** it declares no location permission of any kind — `ACCESS_COARSE_LOCATION` included — for the
  widget or the reminder, and neither surface reads the device's position

### Requirement: Every string is a resource in both locales and every control is named and reachable

Every user-facing string this capability renders — the widget's headline, its dark-window line, its
no-darkness sentences, its Moon forms, its cloud line and its `no forecast`, not-ready and no-spot
strings, the widget's picker description and preview caption, the reminder's title, body, channel
name, channel description, explanatory line, denial sentence and settings control — SHALL exist as a
string resource in **both** `app/src/main/res/values/strings.xml` and
`app/src/main/res/values-ru/strings.xml`, SHALL NOT be a literal in code, and SHALL be resolved in the
widget's `provideGlance` from the resource table rather than concatenated. Times and numbers SHALL be
formatted with the locale's own formatters, and the zone abbreviation SHALL come from the spot's zone
rather than a hard-coded string. Every interactive element SHALL be named for TalkBack and SHALL have
a touch target of at least **48 dp** in both dimensions: the whole widget SHALL be one target carrying
a content description naming the spot and the dark window; the `Remind me tonight` control, the
`Settings` reminder row and the control that opens the notification settings SHALL each be at least
48 dp and named, and the reminder's state SHALL be carried by a word in addition to any colour. The
widget's text SHALL remain legible at the smallest size the layout allows at a system font scale of
200 %, abbreviating rather than clipping.

#### Scenario: Both locales carry every key

- **WHEN** the keys used by this capability are compared across the two resource files
- **THEN** every key exists in both with a non-empty value, and none is a hardcoded literal

#### Scenario: Every control meets the size and role floor

- **WHEN** the widget, the `Remind me tonight` control, the `Settings` reminder row and the
  notification-settings control are measured
- **THEN** each interactive element is at least 48 dp in both dimensions and each has a name for
  TalkBack

#### Scenario: The widget has a content description

- **WHEN** TalkBack reads the widget
- **THEN** it announces a description naming the spot and the dark window, so the widget is not a
  silent tile

#### Scenario: Large fonts reflow the widget

- **WHEN** the system font scale is 200 %
- **THEN** the widget's text reflows or abbreviates without clipping and without becoming unreadable

#### Scenario: The reminder's state is not colour alone

- **WHEN** the reminder control is read in greyscale
- **THEN** whether it is on or off is still stated by a word

### Requirement: Every edge state of the widget and the reminder is typed, never blank

Each of the following situations SHALL render a defined state, and none SHALL be a blank tile, a
spinner that never resolves, a crash, or a plausible default:

- **Permission not granted.** The reminder control reads off, the explanatory line about system
  settings is available, nothing is scheduled, and the widget is unaffected.
- **Permission revoked after it was granted.** The pending work remains but the worker posts nothing
  (it checks `NotificationManagerCompat.areNotificationsEnabled()` before posting and completes
  without error and without retry), and the app's in-app state states that notifications are off for
  Lumintra with the control that opens the setting; the app SHALL NOT claim the reminder is armed.
- **No spot.** The widget shows `Set a spot in Lumintra to see tonight`, the reminder is not offered
  and not armed, and no sky is computed for a fabricated position.
- **A night with no darkness.** The widget shows the honest no-darkness sentence for the persisted
  `DarkWindowKind`; the reminder is not offered and not armed; and a previously armed reminder for a
  night that now resolves to no dark window is cancelled on re-derivation.
- **A stale cache.** The widget shows the cached cloud reading with its age and never a bare value; a
  snapshot for a night other than tonight shows the not-ready state; a snapshot older than the night
  it describes is never shown as current.
- **A reboot.** The widget re-renders from persisted state; the reminder is rescheduled by
  `WorkManager`; a pre-reboot delivery displaced past the window's end posts nothing.
- **A time zone change.** The armed instant is unchanged, displayed times follow the spot's zone, and
  re-derivation happens on the next open.
- **The widget placed with the app absent from recents.** The widget keeps rendering the last
  persisted snapshot with its age and never blanks; and where the app has been force-stopped the
  platform delivers no broadcast, so the reminder cannot fire until the app is opened again — this is
  stated as a limitation rather than hidden, and the widget's tile remains the last snapshot rather
  than an empty one.
- **Red night mode on.** The widget renders in the declared red palette, and the reminder's channel
  and copy are unchanged.

#### Scenario: A revoked permission is stated, not silently ignored

- **WHEN** the permission is revoked after a reminder was armed and the app is opened
- **THEN** the app states that notifications are off for Lumintra, offers the control that opens the
  notification settings, schedules nothing new, and does not claim the reminder is armed

#### Scenario: A widget with no spot invites and computes nothing

- **WHEN** the widget is rendered with no saved spot
- **THEN** it shows the invite string and no sky is computed for a default position

#### Scenario: A no-darkness night cancels an armed reminder on re-derivation

- **WHEN** the plan changes so that tonight has no dark window while the reminder is on
- **THEN** the armed reminder is cancelled, the reminder is no longer offered, and the widget shows
  the honest no-darkness sentence

#### Scenario: A stale snapshot is not shown as current

- **WHEN** the snapshot's inputs are older than the night it describes
- **THEN** the widget shows the not-ready or aged state and never a value that reads as current

#### Scenario: A rebooted reminder posts nothing for a finished window

- **WHEN** the device reboots, the delivery lands after the dark window has ended, and the worker runs
- **THEN** no notification is posted and the reminder awaits re-derivation

#### Scenario: A swipe-away does not blank the widget

- **WHEN** the app is removed from recents and the widget remains on the home screen
- **THEN** the widget keeps rendering the last persisted snapshot with its age and does not blank or
  crash

#### Scenario: A force-stopped app is stated, not hidden

- **WHEN** the app has been force-stopped
- **THEN** the documentation and the app state the limitation that the reminder cannot fire until the
  app is opened again, and the widget shows the last snapshot rather than an empty tile

#### Scenario: Red night mode reaches the widget

- **WHEN** red night mode is on
- **THEN** every widget colour comes from the declared red palette with blue `= 0` and green
  `≤ 8/255`, and the reminder's copy and channel are unchanged

**Honest boundary, recorded rather than glossed.** This capability specifies what the widget and the
reminder render and how they are scheduled; it cannot prove that the platform delivers either on time.
The AppWidget periodic update is inexact by design — the platform may delay, skip or batch it — and so
the widget's freshness is bounded by `updatePeriodMillis` and by the app's own pushes, which is why the
requirement above forbids the widget from ever calling a reading current rather than promising it will
be. The reminder's delivery is an inexact `WorkManager` one-time work, so a night whose dark window is
shorter than the system's deferral may produce no notification at all; that is stated rather than
papered over. Two facts are recorded as `UNVERIFIED` here and both are settled by a device run on
serial `R58MB3SGZVL` (Galaxy A40, Android 11 / API 30): whether a `WorkManager` one-time work with an
initial delay is delivered after a device reboot on that device, and whether a widget placed before the
app has ever been launched renders the invite state without a crash. The scheduling mechanism, the
channel configuration and the exact strings are what this document fixes; the delivery observations are
what a build task must record.

## MODIFIED Requirements

None. This change adds a new capability and modifies no existing spec.
