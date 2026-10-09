# Settings and credits

## Purpose

This capability owns the two places where Lumintra has to be honest and configurable at once: the
`Settings` surface (`ui/settings/SettingsScreen.kt`), where every preference a photographer can hold
about the app's units, its time format, its night-adapted palette, its reminder, its forecast, its
default frame and its language is stored and — without exception — read back by behaviour he can see;
and the `Credits` surface (`ui/settings/CreditsScreen.kt`), which carries the licences, attributions
and data practices the app is legally obliged to display. It also owns the one rating path Google
permits (an automatic Play in-app review at an earned moment, plus a `Rate us` row that opens the Play
listing) and the in-app privacy policy the factory publishes on the app's behalf. A paid app is judged
by these screens as much as by its hero: a preference that is stored and read by nothing is a lie told
quietly, an uncredited image is a licence breach, and a privacy policy that disagrees with the Data
safety form is a removal risk for the whole catalogue.

## ADDED Requirements

### Requirement: Every stored preference changes observable behaviour

The `Settings` surface SHALL be reachable from every main screen — `Night` (`ui/night/NightSceneScreen.kt`),
`Brief` (`ui/brief/BriefScreen.kt`), `Targets` (`ui/targets/TargetsScreen.kt`) and `Spots`
(`ui/spots/SpotsScreen.kt`) — in at most two taps from the screen itself, and every preference it
offers SHALL be stored in `data/SettingsStore.kt` and read by behaviour a user can observe without a
restart, and SHALL survive a restart. The app SHALL NOT contain a preference that is stored and read
by nothing; a key added to `SettingsStore.kt` with no reader SHALL be treated as a defect, not as
groundwork. The surface SHALL offer exactly these rows, each with the effect stated here:

| # | Row (a resource string) | Values | What changes |
|---:|---|---|---|
| P1 | Units | metric · imperial | Distances and heights — spot altitude, horizon-sample elevation, and any visibility distance — render in **metres (m) / kilometres (km)** or in **feet (ft) / miles (mi)**; temperature, where shown, renders in **degrees Celsius (°C)** or **degrees Fahrenheit (°F)**. Focal length and sensor dimensions are **millimetres (mm) in both settings** and never convert. |
| P2 | Time format | 24-hour · 12-hour | Every rendered instant is labelled with or without a meridiem marker and with or without a leading zero: the `Brief`, the scene's time-axis labels, the `glance-at-tonight` widget and the reminder notification all use the same rule. |
| P3 | Coordinate format | decimal degrees (signed) · degrees-minutes-seconds (hemisphere) | The **display** of a spot's latitude and longitude in `SpotEdit` and `Brief`. Either form accepts input; the coordinate that is stored is unaffected (§ the units requirement below). |
| P4 | Red night mode | off · on | The active theme state (§ the red night mode requirement below) for the whole app, the widget and the notification. |
| P5 | Remind me tonight | off · on | Whether the single local reminder for the current night's dark window is armed. Default **off**; the runtime `POST_NOTIFICATIONS` permission is requested **only** when this row is turned on, never at launch and never during `Welcome`. |
| P6 | Cloud forecast | off · on | Whether the app ever calls `api.met.no`. With this row **off**, no forecast request is made and therefore **no coordinate leaves the device**; the `Brief` renders the typed no-forecast state and every other answer is unchanged. |
| P7 | Default lens profile | one of the `LensPreset` values of `domain/Optics.kt` | Which `LensPreset` the lens frame on the `Night` scene opens with, and therefore which field of view every "does it fit my frame" answer is computed against until the user picks another preset for that session. The option list SHALL be exactly the declared `LensPreset` entries rather than a second list; the default is the first declared preset. |
| P8 | Language | the shipped locales, chosen through the platform's own per-app language picker | The app's language, as described in the language requirement below. This row stores **no field of its own**. |
| P9 | Sky brightness estimate | none (default) · an SQM reading in **mag/arcsec²** · a Bortle class **1–9** | Whether the darkness readout that renders `domain/SkyGrade.kt`'s `SkyGrade` — the `Night` scene's readout and the `Brief` — carries an additional line stating the user's own sky-brightness estimate. The default is **none**: with no estimate stored, the readout shows the computed `SkyGrade` alone and `data/SettingsStore.kt` holds no estimate. When a value is set, it is rendered with the words **"user estimate"** beside the computed `SkyGrade`, and the computed `SkyGrade` is unchanged. |

The sky-brightness estimate is the only user-side darkness input the app accepts, and it is shown as the user's own estimate: every rendering of it SHALL carry the words **"user estimate"**, and it SHALL NOT alter any computed quantity of `SkyGrade` or of the brief. Because no light-pollution raster ships (the computed quantity is a `SkyGrade`, never a Bortle class), the app SHALL NOT state a computed Bortle class or a computed mag/arcsec² value — a Bortle class or an SQM reading on screen is only ever the value the user entered. The accepted SQM range is **16.0–22.0 mag/arcsec²** and the accepted Bortle classes are the integers **1–9**; an entry outside either SHALL be refused with the accepted range stated, or clamped to the nearest accepted bound and stated as clamped, and SHALL NOT be stored or rendered as entered.

Every change SHALL take effect on the next composition without a restart, and SHALL be persisted so
that it survives one. A preference whose stored value is unreadable SHALL fall back to its documented
default and be reported, rather than crashing the app or the `Settings` screen.

#### Scenario: A preference takes effect without a restart

- **WHEN** a user switches P1 from metric to imperial while the `Brief` is on screen
- **THEN** the distances and heights on that screen re-render in feet and miles, and no restart is required

#### Scenario: A preference survives a restart

- **WHEN** a user switches P2 to 12-hour, force-stops the app and reopens it
- **THEN** times are still rendered in 12-hour form on the `Brief`, the scene axis, the widget and the notification

#### Scenario: No preference is stored and read by nothing

- **WHEN** the keys written by `data/SettingsStore.kt` are read and each is searched for in the shipped sources
- **THEN** every key has at least one reader, and a key with none is reported as a finding

#### Scenario: A corrupt preference costs the preference, not the app

- **WHEN** the stored payload for a preference is truncated or holds a value outside its declared set
- **THEN** the app opens normally, the preference reads as its documented default, and the user is not shown a crash

#### Scenario: Settings is reachable from a main screen

- **WHEN** the user is on `Night`, `Brief`, `Targets` or `Spots`
- **THEN** `Settings` is reachable in at most two taps from that screen

#### Scenario: The estimate is shown as the user's own and changes nothing computed

- **WHEN** the user sets P9 to an SQM reading of `21.3` and opens the `Brief` for a spot, and separately the `Night` scene
- **THEN** the darkness readout renders an additional line stating `21.3` mag/arcsec² with the words "user estimate", and the computed `SkyGrade` is identical to its value with no estimate stored

#### Scenario: Clearing the estimate returns to the computed darkness

- **WHEN** the user clears P9 after having set it and reopens the `Brief`
- **THEN** no user-estimate line is rendered, the readout shows the computed `SkyGrade` alone, and `data/SettingsStore.kt` holds no estimate

#### Scenario: An out-of-range estimate is refused, not stored

- **WHEN** the user enters an SQM value outside `16.0–22.0` mag/arcsec², or a Bortle class outside `1–9`
- **THEN** the app refuses the entry and states the accepted range, or clamps it to the nearest accepted bound and states that it clamped, and in neither case is the out-of-range value stored or rendered

### Requirement: Red night mode is a fourth theme state, not a filter, and its palette is a measured channel constraint

Lumintra's theme SHALL have a fourth state, **red night mode**, declared in `ui/theme/NightVisionPalette.kt`
and selected through the theme entry point in `ui/theme/Theme.kt` — not a translucent overlay drawn over
the ordinary theme, and not a saturation or colour-matrix pass applied at render time to the ordinary
palette. While it is on, chrome, sky, timeline, chips, dials and every drawn element SHALL take their
colours from `NightVisionPalette.kt`, and a raster asset displayed in the mode (the Milky Way backdrop
`assets/sky/milky_way_eso0932a.webp` and the moon `assets/sky/moon_svs.webp`) SHALL be rendered through
a luminance-to-red mapping that satisfies the channel constraint below. A composable SHALL NOT produce
the mode by drawing the standard palette under a coloured scrim.

Every colour in the active mode SHALL satisfy three numeric constraints on its 8-bit sRGB channels:

- the **blue** channel SHALL be **exactly 0**;
- the **green** channel SHALL NOT exceed **8 of 255 (≈ 3.1 %)**;
- the **red** channel SHALL exceed the green channel.

Text and iconography SHALL still meet a contrast ratio of at least **4.5:1** against the surface they are
drawn on, and the mode SHALL always render on a dark surface. The constraint is derived from the mode's
purpose — preserving dark adaptation, which depends on light above roughly 620 nm and therefore on the
absence of blue — and it is stated as a channel constraint because channels are what a check can read.

The mode SHALL apply to **every surface**, not only the foreground app: the `Night` scene, every other
screen's chrome, the `glance-at-tonight` widget, and the reminder notification. The mode SHALL be toggled
from the `Settings` surface **and** from a control on the `Night` scene; the `Settings` toggle is the
canonical state, the scene's control SHALL write to that same stored state, and no scene-local copy of
the flag SHALL exist. The state SHALL be remembered across restarts, and turning it on or off SHALL
update the widget without waiting for the platform's next refresh.

The mode SHALL NOT be named, described or labelled in the user interface or in any string resource as a
"filter", a "colour filter", a "night filter" or a "red filter": it is a theme state of the app.

#### Scenario: Red mode changes every surface

- **WHEN** the user turns red night mode on in `Settings`
- **THEN** the `Night` scene, the other screens, the widget and the next posted reminder notification all render from the red palette

#### Scenario: The scene's control and the Settings toggle are one state

- **WHEN** the user turns red night mode on from the `Night` scene's control, then opens `Settings`
- **THEN** the `Settings` row reads as on, and turning it off in `Settings` turns the scene back

#### Scenario: The mode survives a restart

- **WHEN** red night mode is on, the app is force-stopped and reopened
- **THEN** the app opens already in red night mode

#### Scenario: The palette satisfies the channel constraint

- **WHEN** the shipped colours of `NightVisionPalette.kt` are read as 8-bit channels
- **THEN** every one has a blue channel of exactly 0, a green channel of at most 8, and a red channel greater than its green channel

#### Scenario: The mode is not produced by an overlay

- **WHEN** the red-mode render path is read
- **THEN** no composable draws the standard palette under a coloured scrim to produce the mode

#### Scenario: The mode is not called a filter

- **WHEN** every string resource and every label of the theme setting is read
- **THEN** none of them names the mode a filter

### Requirement: Units and formats change the display and never the stored value

The stored representation of every quantity SHALL be independent of every display choice. A spot's
latitude and longitude SHALL be stored in **decimal degrees** with north and east positive and south and
west negative (`latitude [−90, +90]`, `longitude [−180, +180]`); a `HorizonProfile` elevation SHALL be
stored in **tenths of a degree**; an instant SHALL be stored as an **absolute UTC instant**; a focal
length and a sensor dimension SHALL be stored in **millimetres**. Choosing imperial units, a 12-hour
clock, or degrees-minutes-seconds SHALL change only what is rendered, and SHALL NOT rewrite a stored
record, SHALL NOT round a stored coordinate to what is displayed, and SHALL NOT alter any computation.
Switching the display back SHALL reproduce the original value exactly.

A display choice SHALL NOT be applied to a quantity that the choice does not own: focal length and sensor
dimensions SHALL render in millimetres under both P1 settings.

#### Scenario: Switching units does not rewrite stored data

- **WHEN** a user views a spot in imperial units, closes the app, and reopens it in metric
- **THEN** the spot's stored coordinate and altitude are the same values as before the switch, and only the rendering differed

#### Scenario: Coordinate format is presentation only

- **WHEN** P3 is switched between decimal degrees and degrees-minutes-seconds and a spot is re-opened
- **THEN** the same stored coordinate is displayed in the other form, and the stored value is byte-identical

#### Scenario: Focal length is never converted

- **WHEN** units are set to imperial and the `Night` scene states a lens frame
- **THEN** the focal length and the sensor dimensions are still stated in millimetres

#### Scenario: A DMS entry is validated, not accepted blindly

- **WHEN** a user enters a degrees-minutes-seconds coordinate whose hemisphere control and magnitude disagree with a valid latitude or longitude
- **THEN** the app refuses the value and states which component is out of range, rather than storing an out-of-range coordinate

### Requirement: The app language follows the platform's per-app language picker, with no stored language field

The app SHALL declare **`en`** and **`ru`** as its shipped locales in `locales_config.xml` and SHALL
ship a real translation for each in `res/values/strings.xml` and `res/values-ru/strings.xml`. The app
SHALL NOT store a language of its own anywhere in `SettingsStore.kt`: the `Language` row of the
`Settings` surface SHALL open the platform's own per-app language picker, and the app's language SHALL
be the one the platform resolves. The row SHALL display the currently effective language so that the
state is readable before the picker is opened.

On first run, before any choice has been made, the app SHALL render in the language the platform resolves
for the app — the system locale where it is `en` or `ru`, and `en` where the system locale is neither —
and the app SHALL NOT show a language dialog, a language step in `Welcome`, or any other language prompt.

Every user-facing string of this capability — every row label, every value label, every help line, every
attribution line, every `contentDescription` — SHALL exist in **both** `res/values/strings.xml` and
`res/values-ru/strings.xml`. A string present in only one of the two SHALL be treated as a defect: a
missing translation falls back to English inside a Russian interface, which is not the product.

#### Scenario: The picker changes the app, and nothing is stored

- **WHEN** the user opens the `Language` row and chooses Russian through the platform picker
- **THEN** the app's interface is Russian, and `data/SettingsStore.kt` holds no language field

#### Scenario: First run makes no choice for the user

- **WHEN** a device whose system language is French installs and launches Lumintra for the first time
- **THEN** the app renders in `en`, `Welcome` shows no language dialog, and the `Language` row reflects the effective language

#### Scenario: Both trees carry every string

- **WHEN** the keys of `res/values/strings.xml` and `res/values-ru/strings.xml` are compared
- **THEN** every key needed by `Settings`, `Credits` and the About block exists in both, and a key missing from either is reported

#### Scenario: Both locales are declared and shipped together

- **WHEN** `locales_config.xml` and the `res/values-<locale>/` trees are read
- **THEN** `en` and `ru` are both declared and both carry a translation, and no locale is declared without its tree

### Requirement: The automatic in-app review runs once, at an earned moment, and reports nothing

The app SHALL depend on **`com.google.android.play:review:2.0.2`** and, because the slot is written in
Kotlin, **`com.google.android.play:review-ktx:2.0.2`**, both resolved from **Google's Maven repository
(`google()`)**, and SHALL NOT depend on the pre-split `com.google.android.play:core`. The review flow
SHALL be requested and launched from the hosting `ComponentActivity` — the factory takes a `Context`,
the launch takes an `Activity`, and a `ReviewInfo` is perishable, so the pre-cache and the launch SHALL
occur within the same user-visible moment. The dependency's licence is the **Play Core Software
Development Kit Terms of Service**, not an open-source licence, and it forbids modification, translation
or the creation of derivative works.

The flow SHALL be invoked at an **earned moment**, defined exactly as: a spot exists, the user has viewed
a `Brief` for it, and the user has just completed **either** a share of a brief, **or** an export of a
scout file, **or** the arming of the reminder. It SHALL NOT be invoked at launch, on install, during
`Welcome`, or from any button, row, widget control or scene control. It SHALL be invoked at most
**once per installed app version** (edge case `E91`).

Every constraint Google imposes is normative here, and each one is stated with its consequence:

| # | Constraint | Consequence in Lumintra |
|---:|---|---|
| R1 | A **time-bound quota** governs how often the dialog may be shown; calling it more than once in a short period (less than a month) may show **nothing**, and the quota's value is a secret Google may change without notice. | The call is made once, at one earned moment, never per screen and never per session, and its silence is expected. |
| R2 | A **call-to-action button must not trigger the API**; the documented remedy is to redirect the user to the Play Store instead. | The `Rate us` row opens the Play listing (§ the Rate us requirement); no control launches the flow. |
| R3 | The API **reports nothing back** — not whether the dialog appeared, not whether the user rated — and the app must not inform the user or change its normal flow on any outcome. | The result is ignored: **no toast, no thanks, no confirmation, no retry, no analytics event, no navigation change**, and the app looks identical whether the card appeared or not. |
| R4 | The app **must not ask the user any question before or while** presenting the rating card, including an opinion question or a predictive one. | There is **no** "do you like Lumintra?", no sentiment gate, no NPS step, no two-step "yes/no then rate", and no routing of happy users only. |
| R5 | Ratings and reviews **must not be incentivised or gated**; deceptive or forced prompts are named policy violations. | Nothing in the app is unlocked, discounted, granted or withheld on the basis of rating: a review is worth exactly zero in app currency, and the prompt never blocks, repeats until answered, or pretends a rating is required. |
| R6 | The card is **surfaced as-is**: no tampering with size, opacity or shape, no overlay around or over it, topmost layer, and no programmatic removal. | No scrim, no themed `Dialog`, no dim, no auto-dismiss timer and no code path that removes the card. |
| R7 | The flow requires the **Google Play Store to be installed**, on a phone, tablet or Google TV device running API 21 or higher. | On a device without the Play Store the request fails and the failure is **swallowed silently**; the app's flow continues unchanged. |
| R8 | The app must **wait for the flow to finish** before continuing its normal flow. | No navigation, activity teardown, animation restart or state reset is started while the card is up. |

The library declares **no permission** and **no manifest component**, so this requirement SHALL NOT add
anything to the app's permission surface or its manifest.

#### Scenario: The flow is not invoked at launch

- **WHEN** the app is launched, including on first run and through `Welcome`
- **THEN** no review flow is requested

#### Scenario: The flow is invoked at an earned moment

- **WHEN** the user has a spot, has viewed its `Brief`, and has just shared that brief
- **THEN** the review flow has been requested and launched from the hosting activity

#### Scenario: An exhausted quota is indistinguishable from a shown card

- **WHEN** the review flow is invoked and the quota causes no card to appear
- **THEN** the app shows no toast, no thanks, no confirmation and no retry, and its flow is unchanged

#### Scenario: No question precedes the prompt

- **WHEN** the shipped sources and strings of the review path are read
- **THEN** no opinion question or predictive question is shown before or during the prompt, and no sentiment gate exists

#### Scenario: Nothing is gated on a rating

- **WHEN** the review path and the strings around it are read
- **THEN** no feature, export, widget or reminder is unlocked, granted or withheld by a rating

#### Scenario: The card is not tampered with

- **WHEN** the app is in the review flow
- **THEN** no overlay is drawn over or around the card, the card is on the topmost layer, and no timer or code path removes it

#### Scenario: No Play Store installed is not an error state

- **WHEN** the earned moment occurs on a device with no Play Store installed
- **THEN** the request fails, the failure is swallowed silently, and the app continues its normal flow with no error shown

#### Scenario: The library adds no permission

- **WHEN** the app's own and merged manifests are read
- **THEN** the review library contributes no `uses-permission` and no manifest component

### Requirement: The Rate us row opens the Play listing and never the review flow

The `Settings` surface SHALL carry a **`Rate us`** row, and that row SHALL open the app's Play listing
through an explicit intent — `Intent(ACTION_VIEW)` with the data URI
`https://play.google.com/store/apps/details?id=com.gexetr.lumintralightplanner` and
`setPackage("com.android.vending")` so that the user lands in the Play Store app rather than an
application chooser. The row SHALL NOT invoke the in-app review flow, and no widget control and no
control on the `Night` scene SHALL invoke the in-app review flow either, for the reason stated in
requirement R2: a button that triggers the API can be a button that does nothing when the quota is
exhausted, and Google's documented remedy is to send the user to the Play Store.

When the Play Store is absent or the intent does not resolve, the row SHALL fall back to opening the same
listing URL without the package restriction through a browser. When neither the Store nor a browser
resolves, the row SHALL be placed in a disabled state that states in words that no store or browser is
reachable, rather than appearing to do nothing. The row SHALL carry a `contentDescription` naming what it
does, and SHALL NOT be labelled by an icon alone.

#### Scenario: The row opens the listing, pointed at the Store

- **WHEN** the user taps `Rate us` on a device with the Play Store installed
- **THEN** an `ACTION_VIEW` intent carrying the listing URI and `com.android.vending` as its package is started

#### Scenario: The row never triggers the review flow

- **WHEN** the tap handler of the `Rate us` row, and any widget or scene control, is read
- **THEN** none of them calls the review API

#### Scenario: The Store is absent and the browser answers

- **WHEN** the `Rate us` row is tapped on a device with no Play Store but with a browser
- **THEN** the listing URL is opened without the package restriction

#### Scenario: Neither Store nor browser resolves

- **WHEN** the row is tapped and neither the Store nor a browser can handle the intent
- **THEN** the row states in words that no store or browser is reachable, and it does nothing silently

### Requirement: Credits carries every licence and attribution obligation the app must display

The `Credits` screen (`ui/settings/CreditsScreen.kt`) SHALL be reachable from the `Settings` surface, and
SHALL carry one row per asset, source or library that actually ships, each with its licence name and a
live link where the licence requires a link. The screen SHALL state the following strings, and each row
SHALL appear only when the asset it credits actually ships in the build:

| Obligated item | Required string |
|---|---|
| ESO Milky Way panorama `eso0932a` (CC BY 4.0; credit mandatory, unaltered and clearly visible) | **`ESO/S. Brunier`** |
| Cloud forecast, `api.met.no` (CC BY 4.0) | **`Data from MET Norway (met.no) — CC BY 4.0`** |
| NASA SVS Moon Kit `moon_svs` (public domain; acknowledgement required, no NASA insignia, no implied endorsement) | **`Moon imagery: NASA Scientific Visualization Studio (SVS), public domain`** |
| IAU constellation figures (CC BY 4.0; full credit, clearly visible, no IAU logo) | **`Constellation figures: IAU and Sky & Telescope (Roger Sinnott, Rick Fienberg; patterns by Alan MacRobert) — CC BY 4.0`** |
| Star positions, FK6 (CC0 1.0) | **`Star positions: The Sixth Fundamental Catalogue (FK6), GAVO Data Centre — CC0 1.0`** |
| Play In-App Review library (Play Core SDK Terms of Service) | **`In-app review: Google Play Core SDK Terms of Service`** |
| Orekit 13.1.9 + Hipparchus 4.0.3 (Apache-2.0, standard licence notice) | **`Orekit 13.1.9 + Hipparchus 4.0.3 — Apache-2.0`** |
| `astronomy.kt`, Astronomy Engine (Kotlin) (MIT, standard licence notice) | **`astronomy.kt — Astronomy Engine (Kotlin), MIT`** |

Each licence that carries a URL requirement SHALL render its links as tappable links to the licence text:
`https://creativecommons.org/licenses/by/4.0/` for the CC BY 4.0 rows, `https://www.met.no` and
`https://api.met.no` for the forecast row, `https://www.eso.org/public/images/eso0932a/` for the ESO row,
`https://www.iau.org` for the constellation row. The screen SHALL state modifications where the licence
requires them: that the forecast coordinates are truncated to 4 decimal places before transmission.

The MET Norway attribution string **`Data from MET Norway (met.no) — CC BY 4.0`** SHALL be declared a
`translatable="false"` string resource: a licence credit is not translated, and no locale tree may
replace it.

The ESO credit is the one whose licence requires it to be **clear, readable and not separated from the
image**: wherever the Milky Way backdrop is displayed — in the `Night` scene and on the composed `Share`
postcard — the string **`ESO/S. Brunier`** SHALL be rendered on or immediately beside the image, in
addition to its row on `Credits`. The same rule applies to the forecast: wherever a cloud forecast is
displayed, the MET Norway attribution SHALL be rendered with it, not only on `Credits`.

The app SHALL NOT use the word **`Yr`** in the credits, the listing or any string, SHALL NOT place the
**NASA insignia**, the ESO logo or the IAU logo anywhere in the app or the store pack, and SHALL NOT
state or imply that ESO, NASA, the IAU or MET Norway endorses Lumintra.

#### Scenario: Every shipped asset has its row

- **WHEN** the assets bundled under `app/src/main/assets/` and the libraries on the classpath are read
- **THEN** every one that a licence obliges the app to credit has a row on `Credits` with its licence named

#### Scenario: The ESO credit is visible beside the image

- **WHEN** the Milky Way backdrop is displayed in the `Night` scene
- **THEN** `ESO/S. Brunier` is rendered on or immediately beside the image

#### Scenario: The forecast credit travels with the forecast

- **WHEN** a cloud forecast is displayed anywhere in the app
- **THEN** `Data from MET Norway (met.no) — CC BY 4.0` is rendered with it

#### Scenario: The prohibited words and marks are absent

- **WHEN** the shipped strings, the sources and the store pack are read
- **THEN** the word `Yr` appears nowhere as a service name, and no NASA, ESO or IAU logo is present

#### Scenario: Credits is reachable from Settings and from the asset's own surface

- **WHEN** the user is on `Settings`, and separately on the surface where a credited asset is used
- **THEN** `Credits` is reachable from `Settings`, and the surface that uses the credited asset states or links to its credit

### Requirement: The privacy policy is openable from inside the app, and About states the data practices

The app SHALL open its published privacy policy from inside the app through an external view intent — the
policy SHALL NOT be a screen of prose rendered from a local asset, and privacy prose inside the About
block SHALL NOT stand in for it. The policy URL SHALL be a string resource declared
`translatable="false"` so that no translation can replace it, and it SHALL be a public HTTPS URL with no
reserved-host placeholder. When no browser can resolve the intent, the app SHALL state the URL as
selectable text together with the reason, rather than failing silently.

The About block on the `Credits` screen SHALL state, in plain language, at least:

- the app's version, as the built `versionName` with its `versionCode` — **`1.0.0` (`1`)** for this slot;
- the developer support contact address **`v.sobolenko.vs@gmail.com`**;
- that the app has **no account and no login**, and collects no personal information to a server of the
  developer's;
- that a **saved coordinate is transmitted to the weather service `api.met.no` when the cloud forecast
  is used**, that the coordinate is truncated to 4 decimal places (about 11 m) before it is sent, and
  that turning the `Cloud forecast` preference off means no such request is made;
- that satellite orbital elements are fetched from CelesTrak and that this request carries no
  coordinates;
- that the app shows no advertising.

The About block SHALL state these in both shipped locales.

#### Scenario: The policy opens outside the app

- **WHEN** the user taps the privacy policy row
- **THEN** an external view intent carrying the declared HTTPS policy URL is started

#### Scenario: The policy URL cannot be translated or replaced by a placeholder

- **WHEN** the string resources are read
- **THEN** the policy URL is declared `translatable="false"` and holds no `example.com`, `localhost` or non-HTTPS value

#### Scenario: About names the version, the contact and the forecast transmission

- **WHEN** the About block is read in either shipped locale
- **THEN** it states the `versionName` **`1.0.0`** and `versionCode` **1**, `v.sobolenko.vs@gmail.com`, and that a saved coordinate is sent to `api.met.no` when the forecast is used

#### Scenario: No browser resolves the policy URL

- **WHEN** the policy row is tapped on a device where no browser can handle the intent
- **THEN** the app states the URL as selectable text with the reason, and does not fail silently

### Requirement: The app's declarations agree with each other and with the app

Lumintra's data practices SHALL be stated identically in three places — the code, the privacy policy, and
the Data safety answers — and the app SHALL NOT contain anything that contradicts them. Specifically:

- The app **collects one thing**: the saved spot's **approximate location**, transmitted to `api.met.no`
  with the cloud-forecast request and only with it. The request SHALL fabricate no other data.
- The app has **no account, no login, no user identifier, no analytics SDK and no advertising**; it
  SHALL NOT declare `com.android.billingclient`, any ad SDK, any analytics or crash-reporting SDK, or the
  pre-split `com.google.android.play:core`.
- The app SHALL declare **no storage permission** — no `READ_EXTERNAL_STORAGE`, no
  `WRITE_EXTERNAL_STORAGE`, no `MANAGE_EXTERNAL_STORAGE` — for the share, export or import handoffs,
  which SHALL use the system intents. It SHALL declare **no location permission of any kind** — no
  `ACCESS_COARSE_LOCATION`, no `ACCESS_FINE_LOCATION`, no `ACCESS_BACKGROUND_LOCATION` — no `CAMERA`,
  no `ACTIVITY_RECOGNITION` and no exact-alarm permission.
  `POST_NOTIFICATIONS` SHALL be declared **only** because the reminder and the widget ship, SHALL be
  requested only when the user turns the reminder on, and SHALL NOT gate any other feature.

The privacy policy and the Data safety answers SHALL agree with each other and with the shipped app; a
policy that says the app collects nothing while a coordinate travels in a query string SHALL be treated
as a false statement, not as a drafting preference.

#### Scenario: The policy, the form and the code state the same thing

- **WHEN** the shipped permission set, the policy text and the Data safety declarations are compared
- **THEN** the coordinate transmitted to `api.met.no` is declared in both the policy and the form, and neither claims the app collects nothing

#### Scenario: No account, no analytics, no ads

- **WHEN** the dependencies and the shipped sources are read
- **THEN** no analytics, advertising, account or identifier dependency is present, and no billing library is declared

#### Scenario: No storage permission for the handoffs

- **WHEN** the app's own `AndroidManifest.xml` is read
- **THEN** it declares no storage permission, and the share, export and import paths use system intents instead

#### Scenario: The notification permission gates nothing but the reminder

- **WHEN** `POST_NOTIFICATIONS` is denied
- **THEN** only the reminder is unavailable; every other feature, including the widget's cached content and the whole `Brief`, works unchanged

#### Scenario: No exact-alarm permission

- **WHEN** the app's own `AndroidManifest.xml` is read
- **THEN** it declares neither `SCHEDULE_EXACT_ALARM` nor `USE_EXACT_ALARM`

### Requirement: Every settings, credits, review and policy path holds its edge states

Each of the following situations SHALL be a defined state with a stated outcome, and the app SHALL NOT
leave any of them to a silent failure, a blank surface, a spinner that never resolves or a crash.

#### Scenario: No Play Store installed

- **WHEN** `Rate us` is tapped on a device with no Play Store
- **THEN** the browser fallback opens the listing, and the review flow's failure at an earned moment is swallowed silently

#### Scenario: The Store is present but the listing is unreachable

- **WHEN** the app is brand new and its listing is not yet retrievable, and `Rate us` is tapped
- **THEN** the Store is opened on the listing URL and the app makes no claim about whether the page resolved, because it cannot know

#### Scenario: The review quota is exhausted

- **WHEN** the earned moment passes and the quota causes no card to appear
- **THEN** no user-facing sign of the attempt exists anywhere in the app

#### Scenario: The notification permission is denied

- **WHEN** the user turns the reminder on and denies `POST_NOTIFICATIONS`
- **THEN** the `Settings` row states in words that the reminder cannot be shown without the permission, the row returns to off or stays on with that statement, and every other feature continues to work

#### Scenario: The policy page is unreachable

- **WHEN** the privacy policy row is tapped and no browser resolves, or the page cannot be reached
- **THEN** the app states the URL as selectable text and says the page could not be opened, and does not present the policy as read

#### Scenario: A very large system font

- **WHEN** the system font scale is set to 200 % and `Settings` and `Credits` are opened
- **THEN** every row label, value and help line reflows with no clipped text and no fixed-height container that truncates a value

#### Scenario: A screen reader reads the surface

- **WHEN** TalkBack is on and the user moves through `Settings`, `Credits` and About
- **THEN** every row announces its label, its current value and its role, and the theme, units, time-format and language rows announce their state as words rather than only as a position

#### Scenario: The settings screen in landscape

- **WHEN** `Settings` is opened in landscape, including on a window whose height is small
- **THEN** every row is reachable by scrolling, no row is clipped, and no control is pushed outside the window

### Requirement: Every string is a resource and every control is named and reachable

Every user-facing string of this capability — each `Settings` row and its values, each `Credits`
attribution and licence name, the About block, every error, refusal and help sentence, and every
accessibility label — SHALL be a string resource in `res/values/strings.xml` and
`res/values-ru/strings.xml`, and SHALL NOT be a literal compiled into a composable. This includes
`contentDescription` text.

Every interactive control on `Settings` and `Credits` SHALL have a name a screen reader can read, SHALL
carry an appropriate role, and SHALL be at least **48 dp** in its smallest dimension. No setting SHALL
be labelled by an icon alone: every setting SHALL carry a word carrying its meaning, whether or not an
icon accompanies it. Colour SHALL NOT be the only signal of a state — the on/off state of the red night
mode, the reminder and the forecast SHALL each be stated in words as well as shown.

#### Scenario: No prose literal reaches a composable

- **WHEN** the sources of `Settings`, `Credits` and the review and policy paths are read
- **THEN** every user-facing string is a `stringResource` reference, with no literal prose in `Text(...)` or `contentDescription`

#### Scenario: No setting is labelled only by an icon

- **WHEN** the rows of `Settings` are read
- **THEN** each row carries a word that states its meaning

#### Scenario: Every control meets the target floor

- **WHEN** `Settings` and `Credits` are measured
- **THEN** every interactive control is at least 48 dp in both dimensions

#### Scenario: A toggle's state is readable without colour

- **WHEN** the red night mode, reminder and forecast rows are read without relying on hue
- **THEN** each states its state as a word

#### Scenario: The pseudolocale stretches every string

- **WHEN** the `en-XA` pseudolocale is enabled and `Settings` and `Credits` are opened
- **THEN** every string stretches, and any string that stays English and unstretched is reported as a hardcoded literal

**Honest boundary, recorded rather than glossed.** Four things here are requirements this document
cannot itself prove. First, the review requirement is enforced by reading the sources and by the
manifest and dependency inventory: a check can prove that the API is called from an earned-moment path
and not from a control, that no toast or navigation follows it, and that no question precedes it — it
cannot prove that the resulting user experience is pleasant, and it cannot observe what Google's secret
quota does on a given device. The in-app review card's appearance for an app installed from a source
other than Play is `UNVERIFIED` and is settled by a device run on the test phone after a closed-track
release. Second, the red-mode channel constraint is measurable in the declared palette and in the
shipped pixels, but the claim that the mode preserves a given user's dark adaptation is a physiological
claim this document asserts from the channel arithmetic and does not measure. Third, whether a raster
remapped to a luminance-to-red ramp keeps its subject recognisable is a judgement made by looking at it,
and the requirement states only that the channel constraint holds and that no overlay produces the mode.
Fourth, the attribution strings are measured as present and correct in both locales; nothing here proves
that a licence holder would accept the placement, and the ESO row's demand that the credit be "clear and
readable … not hidden or disassociated from the image" is met by the requirement to render it beside the
image rather than by any automatic test.

## MODIFIED Requirements

None. This change adds a new capability and modifies no existing spec.
