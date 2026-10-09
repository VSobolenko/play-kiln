# Welcome

## Purpose

The first-run screen, and the only door into a paid app that has nothing to plan yet. `Welcome`
(`ui/welcome/WelcomeScreen.kt`) exists so that a buyer who has just installed Lumintra is never shown a
sky computed for the wrong place: it has exactly one job — to end at a working `Night` scene — and
exactly one action, setting a spot through the routes `scout-spots` already defines. It appears once,
on a cold start whose on-disk spot store holds no record, and never again once a spot exists, because
after that the app opens on the `Brief` already resolved for tonight at the most recently used spot.
It says one thing about the product that its neighbours cannot: Lumintra is a planner, it computes from
the spot's coordinates and the clock, and it does not require the phone to be pointed anywhere. It asks
for nothing — no language dialog, no permission, no account, no purchase, no network fetch and no
notification prompt — because each of those is owned by a capability that has already refused to put it
here.

## ADDED Requirements

### Requirement: Welcome appears exactly once, on a cold start whose spot store holds no record

On a cold start the app SHALL read the on-disk spot store (`data/ScoutStore.kt`, the
`scout-spots` capability's record of `Scout`) and SHALL open `Welcome` (`ui/welcome/WelcomeScreen.kt`)
**only** when the store holds no spot record. When the store holds at least one spot the app SHALL open
`Brief` (`ui/brief/BriefScreen.kt`) **already resolved for tonight at the most recently used spot**, with
zero configuration step, exactly as `session-brief`'s requirement *"The Tonight summary answers with no
configuration"* states; the app SHALL NOT open `Welcome` and SHALL NOT insert a configuration step before
the brief. A returning user whose store holds a spot SHALL never see `Welcome` again.

The trigger SHALL be the **absence of a spot record on disk**, not a stored "first launch" boolean, not
the absence of location, and not the app's install age: a launch whose store holds a spot SHALL NOT open
`Welcome`, whatever the store's history. A **force-stop** (E110) or a device **reboot** SHALL NOT
re-trigger `Welcome` while a spot exists, because the trigger is read from the store on each launch
rather than kept as session state. A store whose payload is corrupt or truncated SHALL read as
"nothing saved" for this decision and the app SHALL open normally, reporting the loss once exactly as
`scout-spots`'s store requirement defines (E98); `Welcome` SHALL NOT present a quarantined store as a
deliberate first run.

#### Scenario: A cold start with no spot opens Welcome

- **WHEN** the app is launched on a device whose spot store holds no record
- **THEN** `Welcome` is shown, and no sky is computed for any position

#### Scenario: A later start with a spot goes straight to the brief

- **WHEN** the app is launched and the store holds at least one spot
- **THEN** `Brief` opens already resolved for tonight at the most recently used spot, with no
  configuration step and no `Welcome`

#### Scenario: A returning user never sees Welcome again

- **WHEN** a user who has saved a spot launches the app on a later day
- **THEN** `Welcome` is not shown, and the brief is the first surface

#### Scenario: A force-stop does not re-trigger Welcome while a spot exists

- **WHEN** the app is force-stopped (E110) and then reopened with a spot still on disk
- **THEN** `Brief` opens for tonight at that spot and `Welcome` is not shown

#### Scenario: A reboot does not re-trigger Welcome while a spot exists

- **WHEN** the device is rebooted and the app is opened with a spot still on disk
- **THEN** `Welcome` is not shown, because the trigger is the store's contents and not a session flag

#### Scenario: An unreadable store is not a first run

- **WHEN** the stored spot payload is truncated or otherwise unreadable (E98)
- **THEN** the app opens normally, reports that there was nothing to resume once, and does not claim the
  user never set a spot

### Requirement: Welcome has one job and offers exactly one action

`Welcome` SHALL be a single screen whose job, and only job, is to end at a working `Night` scene
(`ui/night/NightSceneScreen.kt`). It SHALL offer **exactly one primary action** — set a spot — and SHALL
NOT present a second, competing call to action: no "sign in", no "watch a tour", no "skip", no "watch
this later" and no secondary button that competes with it. The action SHALL open `SpotEdit`
(`ui/spots/SpotEditScreen.kt`) and SHALL offer the routes `scout-spots` already defines: **manual
coordinates**, the **map picker** and **pasted coordinates** (E56, E58). The action's label SHALL name
what it does (a resource such as `Set a spot`), SHALL be a string resource and not a literal, and the
screen SHALL state that a spot is the one thing needed before a plan can be drawn.

`Welcome` SHALL NOT compute, request or draw a sky: it SHALL NOT show a value for the Sun, the Moon, a
target, a pass or the cloud, and it SHALL NOT render a scene. With no spot the app SHALL NEVER
substitute a default or `(0, 0)` position and SHALL NEVER compute a sky for a fabricated coordinate
(E107, E110). The `Brief`'s scoped empty state and the `Night` scene's own empty states are
`session-brief`'s and `scout-spots`'s contracts; this requirement cites them and does not restate them.

#### Scenario: One action and no competing call to action

- **WHEN** the `Welcome` screen is read for its interactive controls
- **THEN** exactly one primary call to action is present, and no second call to action competes with it

#### Scenario: The action opens SpotEdit with the three routes

- **WHEN** the primary action is taken
- **THEN** `SpotEdit` opens and manual coordinates, the map picker and pasted coordinates are each
  available

#### Scenario: Welcome draws no sky

- **WHEN** the screen is rendered
- **THEN** no Sun, Moon, target, pass, cloud or scene value is shown, and no scene is drawn

#### Scenario: No spot never becomes a fabricated position

- **WHEN** no spot exists
- **THEN** no sky is computed for a default or `(0, 0)` position, and the screen offers the one action
  instead (E107, E110)

### Requirement: Welcome states in one line that Lumintra does not use the compass

`Welcome` SHALL state, **in one line**, that Lumintra is a planner: it computes where objects will be
from the spot's coordinates and the clock, does **not** require the phone to be pointed anywhere, and
asks for **no location permission**. This is a product promise, not an apology — buyers arrive from
apps that draw a live compass — and the line is a stated requirement rather than optional copy. The
line SHALL be a string resource present in **both** `app/src/main/res/values/strings.xml` and
`app/src/main/res/values-ru/strings.xml` (see the locale requirement) and SHALL NOT be a literal in
Kotlin.

The line SHALL NOT promise a compass, an AR overlay or a "point your phone at the sky" surface, which
`proposal.md`'s Non-goals exclude, and SHALL NOT imply that the app reads the device's heading. It
SHALL say what the app does (a plan from coordinates and the clock) as plainly as what it does not.

#### Scenario: The line is present

- **WHEN** the `Welcome` screen is read
- **THEN** it carries one line stating that the app plans from the spot's coordinates and the clock, does
  not require the phone to be pointed anywhere, and asks for no location permission

#### Scenario: The line is present in both locales

- **WHEN** the compass line's string key is looked up in both resource trees
- **THEN** it exists with a non-empty value in `values/strings.xml` and in `values-ru/strings.xml`, and
  the screen fails its requirement if either is missing

#### Scenario: The line names no compass capability

- **WHEN** the compass line and the surrounding strings are read
- **THEN** none promises a compass, a heading read, an AR overlay or a point-your-phone view

### Requirement: Welcome carries no language dialog, permission, review, account, purchase, network fetch or notification prompt

Each of the following SHALL NOT appear on `Welcome`, and each rule is owned by the capability named
below and is cited rather than restated:

- **No language dialog and no language step.** The app's language is the one the platform resolves for
  it, chosen through the platform's per-app language picker: `settings-and-credits`'s requirement
  *"The app language follows the platform's per-app language picker, with no stored language field"*
  states that the app SHALL NOT show a language dialog, a language step in `Welcome`, or any other
  language prompt, and its scenario *"First run makes no choice for the user"* is the case this rule
  rests on.
- **No permission request of any kind, including location.** `scout-spots`'s requirement *"No location
  permission is required, and none is requested as a gate"* forbids a location request at launch, on
  first run or during onboarding (E56, E107), and `settings-and-credits` fixes that
  `POST_NOTIFICATIONS` is requested **only** when the reminder is turned on, never at launch and never
  during `Welcome`, per `settings-and-credits`'s requirement *"Every stored preference changes
  observable behaviour"* (row P5); `specs/night-glance/spec.md:437-441`.
- **No in-app review flow.** `settings-and-credits`'s requirement *"The automatic in-app review runs
  once, at an earned moment, and reports nothing"* forbids invoking the flow at launch, on install,
  during `Welcome`, or from any button, row, widget control or scene control; `Welcome` SHALL contain
  no trigger for it.
- **No account and no login.** The app has no account and no login
  (`settings-and-credits`'s About/data-practices requirement).
- **No purchase and no paywall.** The slot is `purchase_only_no_ads`, bought through Play with no
  in-app product and no billing library (`proposal.md`; `annex/CONTEXT.md` §2); `Welcome` SHALL show no price,
  no store link and no purchase control.
- **No network fetch.** With no spot there is nothing to forecast, and no outbound request SHALL be made
  from `Welcome` (E108; the no-network rules of `session-brief` and `cloud-forecast`).
- **No notification prompt.** No notification permission is requested and no reminder is offered on
  `Welcome` (`specs/night-glance/spec.md:437-441`).

The screen SHALL therefore carry no dialog, no permission request and no network call.

#### Scenario: No language dialog

- **WHEN** `Welcome` is shown on a device whose system language is neither declared locale
- **THEN** no language dialog, language step or language prompt appears, and the app renders in the
  language the platform resolves

#### Scenario: No permission request

- **WHEN** `Welcome` is shown and used
- **THEN** no permission dialog appears, and no location, notification or other permission is requested

#### Scenario: No review flow

- **WHEN** the app is launched through `Welcome`
- **THEN** no in-app review flow is requested or launched

#### Scenario: No account, purchase or paywall

- **WHEN** `Welcome` is read
- **THEN** it shows no sign-in, no price, no store link and no purchase or paywall control

#### Scenario: No network fetch

- **WHEN** `Welcome` is shown and a spot is created from it
- **THEN** no outbound request is made from `Welcome`, because there is no spot to forecast yet (E108)

#### Scenario: No notification prompt

- **WHEN** `Welcome` is shown
- **THEN** no notification permission is requested and no reminder is offered

### Requirement: Welcome's states are named, and none blocks or fabricates

`Welcome` SHALL define and render a typed state for each of the following, and none SHALL be a spinner
that never resolves, a blocking dialog or a fabricated value:

- **First run with nothing on disk** (E107). The screen shows the one action and the compass line and
  nothing else. It SHALL NOT block on a network, a permission, a location or a login, and no sky is
  computed.
- **No network** (E108). The same screen with the same one action. It SHALL NOT block, SHALL NOT show a
  spinner that never resolves, and SHALL NOT require a connection to set a spot: manual coordinates and
  pasted coordinates are entirely local, and the map picker's place-name lookup is best-effort and
  degrades to the typed "address lookup unavailable — enter coordinates" state without blocking the
  save, exactly as `scout-spots` defines (E56; "Address lookup is best-effort and never blocks a save").
- **The user abandons the action.** Returning to `Welcome` without a spot — backing out of `SpotEdit`,
  or cancelling it — SHALL return to the same screen in its first-run state, with no error and no
  partial spot. A cancelled `SpotEdit` SHALL be a no-op, no half-created spot and no committed horizon
  draft SHALL exist, and the in-progress draft SHALL be held in saveable state or discarded with a
  prompt rather than committed (`scout-spots`; E80).

#### Scenario: First run with nothing on disk

- **WHEN** the app is opened for the first time with no spot, no location and no network
- **THEN** `Welcome` shows its one action and the compass line, blocks on nothing, and computes no sky
  (E107)

#### Scenario: No network does not block setting a spot

- **WHEN** the device has no network and the user sets a spot by manual coordinates or pasted
  coordinates
- **THEN** the spot is set and the journey continues, with no blocking state and no spinner that never
  resolves (E108)

#### Scenario: Abandoning the action returns to Welcome unchanged

- **WHEN** the user opens the spot action and then backs out or cancels it without saving
- **THEN** `Welcome` is shown again in its first-run state, no error is shown, and no partial spot or
  committed horizon draft exists (E80)

### Requirement: Welcome ends at Night and is never reachable again while a spot exists

Reaching a spot, **however created** — manual coordinates, the map picker, pasted coordinates, or the
device's coarse fix offered by `scout-spots` (E56, E57) — SHALL dismiss `Welcome` and land on `Night`
(`ui/night/NightSceneScreen.kt`), the app's hero, with the scene drawn for the current instant. The new
spot carries no horizon profile yet, so the scene SHALL draw the **flat fallback horizon** and state it
once, exactly as `night-scene`'s requirement *"A flat horizon and a horizon that blocks everything are
legal and named"* and `scout-spots`'s *"No profile is its own state"* define (`hasProfile = false`, and
the sentence `No horizon profile for this spot — using a flat horizon.`); this requirement cites those
capabilities and does not redefine the scene.

`Brief` SHALL remain one tap away from `Night`, per `session-brief`'s Tonight requirement. `Night` SHALL
be the app's only destination for this transition — the save action SHALL NOT open any other screen —
and once a spot exists `Welcome` SHALL NOT be reachable by navigation: it SHALL NOT be a destination on
the back stack and SHALL NOT be reachable from a menu, a control or a deep link. It is reachable only by
the store holding no spot record (the first requirement).

#### Scenario: After a spot is saved, Night is shown

- **WHEN** a spot is created and saved from `Welcome`
- **THEN** `Welcome` is dismissed and `Night` is shown with the scene drawn for the current instant

#### Scenario: A new spot gets the flat fallback horizon

- **WHEN** the `Night` scene is shown for a spot that has no horizon profile
- **THEN** it draws the flat fallback horizon and states `No horizon profile for this spot — using a flat
  horizon.` once, rather than nothing or a fabricated ridge

#### Scenario: Brief stays one tap away

- **WHEN** `Night` is shown after the first spot is created
- **THEN** `Brief` is reachable in one tap from `Night`

#### Scenario: Welcome is not a navigation destination

- **WHEN** the navigation graph and the app's controls are read with a spot present
- **THEN** `Welcome` is not a destination on the back stack and is reachable from no control, menu or
  deep link, only by the store holding no spot record

### Requirement: Every Welcome string is a resource in both locales, and every control is named and at least 48 dp

Every user-facing string `Welcome` renders — the compass line, the single action's label, the sub-line
naming what a spot is for, and every state sentence — SHALL exist as a resource in **both**
`app/src/main/res/values/strings.xml` and `app/src/main/res/values-ru/strings.xml`, and SHALL NOT be a
literal in Kotlin. The compass line SHALL exist as a resource in both files (the requirement above).

Every interactive control on `Welcome` SHALL have a name for TalkBack (a content description or a
labelled role), SHALL be at least **48 dp** in both dimensions, and SHALL state any state that matters
with a word or a shape in addition to colour alone. The screen SHALL read the window size class and work
in portrait and landscape, SHALL respect the system insets, and its text SHALL reflow with no
fixed-height container and be legible at a **200 %** system font scale — the floor every sibling
capability states.

#### Scenario: Both locale files carry every key

- **WHEN** the strings used by `Welcome` are compared across the two resource trees
- **THEN** every key exists in `values/strings.xml` and in `values-ru/strings.xml`, with a non-empty
  value in each, and none is a hardcoded literal

#### Scenario: The compass line exists in both resource files

- **WHEN** the compass line's key is read from both trees
- **THEN** it is present and non-empty in both `values/strings.xml` and `values-ru/strings.xml`

#### Scenario: Every control meets the size and role floor

- **WHEN** `Welcome`'s interactive controls are measured and read by TalkBack
- **THEN** each is at least 48 dp in both dimensions and each has a name

#### Scenario: The screen works in portrait, landscape and at 200 % font scale

- **WHEN** `Welcome` is opened in portrait and then in landscape, and the system font scale is set to
  200 %
- **THEN** the layout reflows with no clipped text and no fixed-height container, and every control
  remains reachable

**Honest boundary, recorded rather than glossed.** What this capability can prove is structural: that
the trigger is read from the spot store, that exactly one call to action exists, that the compass line
is a resource present in both locales, that the refused surfaces (language, permission, review,
account, purchase, network, notification) are absent, and that the controls meet the size and role
floor. It cannot prove that the store reached the state "no spot record" on a given device — that is a
fact about that install, not about the source — and it cannot prove the compass line persuades a buyer,
which is the owner's judgement at checkpoint 1 (`.cursor/rules/visual-identity.mdc`). Whether the screen
reads as a single purposeful action rather than a form with a button is likewise a judgement, not a
measurement; this document's job is to ensure the screen has exactly one action to judge.

## MODIFIED Requirements

None. This change adds a new capability and modifies no existing spec.
