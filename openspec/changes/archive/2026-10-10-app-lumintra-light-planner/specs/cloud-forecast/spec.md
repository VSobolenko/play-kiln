# Cloud Forecast

## Purpose

Give one night's **hourly total cloud cover** for the spot the user saved, **aligned to the dark window and
to the chosen target's altitude curve** so the answer to "will the clouds be in the way when this object is
up" is one glance on the night's own time axis rather than a detached weather tab.

This capability is bound to the promise **`forecast-hourly-cloud-cover`** in
`docs/ideation/plans/lumintra-light-planner.differentiators.json`. Its promise text is delivered here
including its stated limitation: the value the feed carries is **total** cloud cover, there is **no**
low/mid/high split in the response the source returns, and the app says "total cloud cover" rather than
implying a layer model it does not have (annex/N-data-algorithms.md §4; design.md § "Weather").

The wedge this capability exists for is **presentation, not the feed**. Cloud cover is absent from the
light-and-sky session-planner shelf's own descriptions, and the apps that do show it are the shelf's
weakest-rated (Clear Outside 3.8★, Good To Stargaze 3.9★) (annex/K-competitor-features.md §2 A7, §4 P6): the incumbents show a cloud
column and leave the user to line it up against the darkness by eye. The source feed is one keyless public
service and copying it earns nothing; **aligning it to the dark window and the target's altitude curve is
the differentiator**, and every requirement below exists to keep that alignment honest.

The data has a **compliance cost**, and this is the only part of the app that carries one: sending a saved
spot's coordinate to `api.met.no` **is** a collection under Play's definition — *transmitting data off the
user's device, irrespective of destination* — so it is declared (Location → Approximate, App functionality,
optional, encrypted in transit) and it is disclosed to the user where it is relevant (annex/M-play-integrations-compliance.md §3, §4; §7 below).

## ADDED Requirements

### Requirement: The forecast is fetched from exactly one named service with the request that service requires

The app SHALL obtain the forecast from the **MET Norway Locationforecast 2.0 compact** service at exactly

```
GET https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=<lat4>&lon=<lon4>[&altitude=<m>]
```

and from **no other weather service**. The request SHALL be HTTPS only, SHALL carry an identifying
`User-Agent`, SHALL carry the spot's coordinates **truncated to a maximum of 4 decimal places**, and SHALL
read exactly the field `properties.timeseries[].data.instant.details.cloud_area_fraction` with its companion
`...time` (ISO-8601 UTC). The value is **total cloud cover, a number 0–100 in per cent**; the response the
service returns for this product carries **no low/mid/high layer split**, and the app SHALL state that
limitation to the user rather than hide it (section "Honesty"). These four facts are MEASURED (annex/N-data-algorithms.md §4.1, §4.2,
§4.3, §4.4, §4.6), except the coordinate cap, which is the service's own stated term (annex/N-data-algorithms.md §4.1: *"truncate all
coordinates to max 4 decimals … For new products, requests with 5+ decimals will return a 403 Forbidden"*).

The exact client rules:

- **Coordinate precision cap — 4 decimal places.** `latitudeDeg` and `longitudeDeg` SHALL be **truncated,
  not rounded**, to at most 4 decimals **before the URL is built**, so a request with 5 or more decimals is
  never issued. 4 decimals of latitude is ≈ **11 m** (MEASURED, annex/N-data-algorithms.md §4.1); the cap is the precision at which
  the app asks, and it is not a claim about the spot's accuracy.
- **Identifying `User-Agent`.** Every request SHALL carry `User-Agent: Lumintra/<versionName>
 (v.sobolenko.vs@gmail.com)`, where `<versionName>` is the shipped version name. It SHALL be a single fixed
 string per release and SHALL NOT be omitted, randomised or substituted with a browser default: the service's
  terms require an identifiable client and this string is the app's entire contribution to compliance (annex/N-data-algorithms.md §4.1).
- **Altitude.** When the spot carries an elevation in metres, the request SHALL include `&altitude=<m>` as a
  whole number of metres; when the spot carries none, the parameter SHALL be omitted rather than sent as
  `0`.
- **Total cloud only.** `cloudCoverPercent` SHALL be rendered and spoken as **"total cloud cover"** with its
  value in per cent. The value SHALL NOT be presented as "clear sky", as an atmospheric transparency model,
  as a seeing or "viewing conditions" estimate, or as any per-layer quantity — the source supplies none of
  those, and inventing one from the total would be a fabricated claim (annex/N-data-algorithms.md §4.3).
- **One source.** No second weather endpoint, no fallback weather provider and no bundled weather asset
  SHALL exist. The money plan fixes `api.met.no`, and the non-commercial alternative was rejected with a
  recorded reason: `api.met.no`'s terms permit a paid app, which the free Open-Meteo tier does not
  (annex/N-data-algorithms.md §4; design.md § "Trade-offs accepted" 3).
- **A deprecated product version (HTTP 203) SHALL be surfaced as a warning**, not followed silently to a
  different product, and SHALL not be treated as a successful forecast.

#### Scenario: A spot's coordinates are truncated to 4 decimals in the built URL

- **WHEN** a spot at latitude `59.91387` and longitude `10.75239` is planned
- **THEN** the built request is `.../compact?lat=59.9138&lon=10.7523` (4 decimals, truncated, not rounded)
- **AND** no request bearing a fifth decimal is ever issued

#### Scenario: The request identifies the app and its version

- **WHEN** any forecast request is issued from app version `1.2.0`
- **THEN** it carries the header `User-Agent: Lumintra/1.2.0 (v.sobolenko.vs@gmail.com)`
- **AND** the same string is used for every request in that release

#### Scenario: Altitude is sent only when the spot has one

- **WHEN** the spot carries an elevation of `135 m`
- **THEN** the request includes `&altitude=135`
- **WHEN** the spot carries no elevation
- **THEN** the request omits the `altitude` parameter entirely

#### Scenario: The cloud figure is labelled total cloud cover

- **WHEN** the response reports `cloud_area_fraction = 62`
- **THEN** the UI states `62 %` **total cloud cover**
- **AND** no low, middle or high cloud value, transparency value or seeing value is shown

#### Scenario: No second weather source exists

- **WHEN** the shipped client is read for its network destinations
- **THEN** the only weather host reached is `api.met.no`
- **AND** no fallback provider, secondary weather endpoint or bundled weather dataset is present

### Requirement: The request is conditional, cached per spot, and made only while the app is in use

The service's terms forbid background retrieval by a mobile app and require conditional requests (annex/N-data-algorithms.md §4.4,
§4.5), so the app SHALL cache one forecast per spot and SHALL fetch **only while the app is in use and only
for the spot the user is looking at**. The cache record SHALL be
`{lat4, lon4, fetchedAtUtc, expiresUtc, lastModified, etag, payload}`, **keyed by the truncated
coordinates**, so two spots within the same 0.0001° cell are one request and one cache entry.

The exact caching rules:

- **Re-request only after `expiresUtc`.** A cached payload whose `expiresUtc` is in the future SHALL be
  served with **no request at all**.
- **Conditional HTTP.** Once a cached entry exists, the app SHALL send **`If-Modified-Since:
 <lastModified>`** on the next request and a **304 Not Modified** SHALL keep the cached payload and refresh
  the cache entry's freshness fields rather than discard the payload (annex/N-data-algorithms.md §4.4).
- **Honour `Expires`.** The next permitted fetch instant SHALL be taken from the response's `Expires`
  header, and the app SHALL add a **random offset of a few minutes** so refreshes do not fall on the hour
  (annex/N-data-algorithms.md §4.4).
- **No probe request.** The app SHALL NOT issue a `HEAD` before a `GET` and SHALL NOT issue more than one
  request to resolve a spot's forecast; a single attempt is the rule, and a retry loop is forbidden.
- **Ceiling.** At most **one request per spot per `expiresUtc` window** SHALL be issued, which keeps the
  app far below the service's published ceiling of **20 requests/second** (annex/N-data-algorithms.md §4.4, MEASURED).
- **No retrieval while the app is not in use.** Fetching SHALL happen only while the user is on a screen
  that shows the forecast for the spot in view. This explicitly forbids **all** of the following: background
  polling; a periodic or scheduled fetch (any `WorkManager` job, alarm, job scheduler or timer that fetches);
  a fetch at app launch when no spot exists; a fetch from the home-screen **widget**; a fetch from the
  **reminder**; and a fetch in response to a reminder firing or a widget refresh. **The widget and the
  reminder read the cache and never fetch** — they render the cached value, or the `NoForecast` wording,
  `No forecast available.`, and make no network call of their own.

#### Scenario: A fresh cache entry produces no request

- **WHEN** the spot's cache entry has `expiresUtc` one hour in the future and the user opens the Brief
- **THEN** the cached forecast is shown with its age
- **AND** no forecast request is issued

#### Scenario: An expired cache entry is refreshed conditionally

- **WHEN** the spot's cache entry has expired and carries a `lastModified`
- **THEN** the refresh request carries `If-Modified-Since` with that recorded value
- **AND** a 304 response keeps the cached payload and refreshes its freshness fields

#### Scenario: The home-screen widget never fetches

- **WHEN** the widget refreshes on its schedule, with the app not in use
- **THEN** it makes no forecast request
- **AND** it renders the cached forecast, or the `NoForecast` wording, `No forecast available.`, when the cache is empty

#### Scenario: The reminder never fetches

- **WHEN** the local reminder fires
- **THEN** no forecast request is issued at, before or after the fire
- **AND** any forecast it mentions comes from the existing cache, or is stated as unavailable

#### Scenario: A single attempt succeeds or fails, and is not retried in a loop

- **WHEN** a forecast request fails
- **THEN** the failure is typed and shown (section "The forecast degrades…") and no immediate retry storm
  follows
- **AND** the next attempt is not made before the next permitted refresh instant

### Requirement: The licence's attribution is carried with the data and the service's identity is not misused

MET Norway requires CC BY 4.0 attribution — credit, a link to the licence, and an indication of whether
changes were made (annex/M-play-integrations-compliance.md §4; annex/N-data-algorithms.md §4.6). The app SHALL display the following string **verbatim**, as the
attribution, in both locales, as a `translatable="false"` string resource:

```
Data from MET Norway (met.no) — CC BY 4.0
```

It SHALL be accompanied by the source links `https://www.met.no` / `https://api.met.no` and by the
**licence link** `https://creativecommons.org/licenses/by/4.0/`, and by a **modification statement** that
the data is shown **unmodified** — the app changes the request (it truncates coordinates) and does not
change the values (design.md § "Weather"). The credit SHALL be displayed **with the forecast it belongs to**,
not only on the Credits screen. Its two required homes are:

- **Beside the forecast, on every screen that shows forecast data** — a visible footnote on the Brief and in
  the night-scene cloud row, because the credit must travel with the data and a credit that exists only in a
  menu does not (annex/N-data-algorithms.md §4.6).
- **The Credits screen, `ui/settings/CreditsScreen.kt`** — the named screen that carries the full
  attribution with both links, alongside every other licence the app must display
  (`settings-and-credits` owns the complete set).

The app SHALL NOT use the name **`Yr`** in any service name, list title, screen title, prose, string
resource or store copy, and SHALL NOT display the Yr logo or any MET Norway / NRK mark, and SHALL NOT imply
that either endorses the app (annex/M-play-integrations-compliance.md §4). The attribution string, the source name and the licence name are
proper nouns and SHALL NOT be translated or abbreviated in either locale.

#### Scenario: The exact attribution appears with the data

- **WHEN** any screen shows a cloud value
- **THEN** the exact string `Data from MET Norway (met.no) — CC BY 4.0` is visible on that screen
- **AND** the source links `https://www.met.no` / `https://api.met.no`, the licence link
  `https://creativecommons.org/licenses/by/4.0/`, and the modification statement are reachable from it

#### Scenario: The Credits screen carries the full attribution

- **WHEN** the user opens the Credits screen at `ui/settings/CreditsScreen.kt`
- **THEN** the MET Norway entry states the exact attribution string with the licence link
- **AND** it names the licence as CC BY 4.0

#### Scenario: The name Yr is never used

- **WHEN** the shipped string resources, screen titles and store copy are read
- **THEN** the token `Yr` appears in none of them as a service name
- **AND** no MET Norway or NRK logo or mark is displayed, and no endorsement is implied

#### Scenario: The attribution survives translation

- **WHEN** the app runs in the `ru` locale
- **THEN** the attribution string is still present verbatim, with the source and licence names untranslated

### Requirement: The cloud is presented aligned against the dark window and the target's altitude curve

This is the capability's whole reason to exist and SHALL NOT be reduced to a detached cloud column. For one
night at one spot, the hourly cloud SHALL be shown **on the same absolute UTC time axis as the computed dark
window and the chosen target's altitude curve**, so the user reads "cloud when this object is up, inside the
dark" from one picture (annex/K-competitor-features.md §2 A7; design.md § "Weather").

- **Drawn extent.** The row's extent SHALL be the **session window's hourly points**, with the **dark window
  marked inside that extent** and the row **aligned to that mark**, so the dark hours read as a labelled span
  within the row rather than as the row's own edges. The extent is the session window, not the dark window
  alone and never the calendar night; the mark is the dark window that `session-brief` defines
  (`specs/session-brief/spec.md`, requirement "The dark window is a computed interval with a typed outcome,
  never a bare interval"), which is the authority for the mark, and its threshold is not restated here.
- **Sampling.** The cloud row SHALL be drawn at the feed's native cadence of **one sample per returned
  hour** — one cell per hourly point of `properties.timeseries` that intersects the session window. The
  cloud series SHALL NOT be resampled, interpolated, smoothed or curve-fitted to a finer step, and the
  target altitude curve SHALL NOT be coarsened to hourly to match it: both are placed on the shared axis at
  their own resolution and neither is degraded to the other's.
- **On the timeline strip.** The night scene's timeline strip SHALL carry a cloud row below the dark-window
  band: one cell per hour **intersecting the session window**, each cell coloured by its total cloud
  fraction, each cell reachable and announcing its hour (local time) and its numeric fraction in per cent,
  and the row headed by the word **forecast** with the retrieval time (the strip itself is owned by
  `night-scene`; the cloud row's content and labelling are owned here).
- **In the brief.** The session brief SHALL state a cloud condition for the dark window — the hours inside
  the session window against the target's altitude curve — anchored to the dark-window start, so the first
  thing the brief says about cloud is about the window the user will actually shoot in, not about midnight
  (the sentence's assembly is owned by `session-brief`; the value and its label are owned here).
- **Labelling.** Each hour SHALL be labelled from its numeric total cloud fraction, **and the numeric
  fraction SHALL always be shown with the label** — the colour is never the only signal:
  - **Clear** — total cloud `0 %`–`25 %` inclusive,
  - **Partly cloudy** — total cloud greater than `25 %` and up to `75 %` inclusive,
  - **Overcast** — total cloud greater than `75 %` up to `100 %`.
  These three thresholds are a **stated product decision**, not a measured meteorological fact and not an
  okta scale: they are the app's own reading of the feed's continuous percentage, chosen so the labels are
  stable, speakable and identical in every locale, and they SHALL be stated as the app's decision wherever
  the app explains its own labels. A fraction on a boundary belongs to the lower-cloud band
  (`25 %` is Clear, `26 %` is Partly cloudy; `75 %` is Partly cloudy, `76 %` is Overcast).

#### Scenario: The cloud row shares the night's time axis

- **WHEN** the user views the night scene for a spot with cloud data
- **THEN** the cloud row's cells sit on the same absolute time axis as the dark-window band and the target
  altitude curve
- **AND** a cell's horizontal position corresponds to its hour on that axis

#### Scenario: One cell per returned hour, with no invented samples

- **WHEN** the response returns hours 21:00, 22:00, 23:00 and 00:00 UTC inside the session window
- **THEN** the row draws exactly four cells, at the feed's one-hour cadence
- **AND** no half-hour, interpolated or smoothed sample is drawn between them

#### Scenario: The target curve is not coarsened to hourly

- **WHEN** the cloud row and the target's altitude curve are drawn together
- **THEN** the target curve keeps its own computed resolution
- **AND** neither series is reduced to the other's sampling

#### Scenario: Labels follow the stated thresholds

- **WHEN** the total cloud fraction is `25 %`
- **THEN** the hour is labelled **Clear** and shown as `25 %`
- **WHEN** the fraction is `26 %`
- **THEN** the hour is labelled **Partly cloudy** and shown as `26 %`
- **WHEN** the fraction is `76 %`
- **THEN** the hour is labelled **Overcast** and shown as `76 %`

#### Scenario: The brief states cloud for the dark window, not the calendar night

- **WHEN** the session brief is assembled for a night whose astronomical-dark window is 23:10–03:40 local
- **THEN** its cloud condition describes the hours inside that window
- **AND** the hours outside the window are not the condition's subject

### Requirement: The forecast is stated as a forecast, with its model time, its age, and unavailable hours left unavailable

Cloud cover here is a **forecast**, never a measurement. Every surface that shows a cloud value SHALL label
it **forecast**, and the app SHALL show both times that let the user judge it:

- **The model time** — the instant the service says its data was produced, read from the response's
  `Last-Modified` header and recorded as `lastModified`. When a response carries no `Last-Modified`, the app
  SHALL show the fetch instant and state that the model time is unknown; it SHALL NOT invent one.
- **The fetch age** — the difference between now and `fetchedAtUtc`, stated with its unit (for example
  `3 h 20 min ago`), and stated for the cached value too: a cached forecast is shown with the age that tells
  the user how old it is, not as though it were current.

An hour that the response does not carry is **unavailable** and SHALL be shown as unavailable. The app SHALL
NOT interpolate between returned hours, SHALL NOT carry a neighbouring hour's value into a gap, SHALL NOT
present a longer-range value as a short-range one, and SHALL NOT render a missing hour as `0 %` cloud. The
gap SHALL be visible in the row and announced as unavailable, and the brief SHALL state that the forecast
does not cover that hour (design.md § "Weather"; annex/L-depth-retention-edges.md E52, E53).

#### Scenario: A forecast is never called a measurement

- **WHEN** any surface renders a cloud value
- **THEN** it is labelled **forecast**
- **AND** no wording presents it as observed, measured or current conditions

#### Scenario: The model time and the fetch age are both shown

- **WHEN** a forecast is displayed
- **THEN** the model time (its `Last-Modified`, or an explicit "unknown" when absent) is shown
- **AND** the fetch age is shown with its unit

#### Scenario: A missing hour is a visible gap, not an invented value

- **WHEN** the response carries hours 22:00 and 00:00 UTC but not 23:00 UTC
- **THEN** the 23:00 hour is rendered and announced as **not covered by the forecast**
- **AND** no value is interpolated, carried forward or defaulted into it

#### Scenario: A cached value is shown with its age

- **WHEN** the cache is served without a refresh
- **THEN** the displayed forecast carries the age of its `fetchedAtUtc`
- **AND** it is not presented as current (see the `OfflineReadingFrom` wording below)

### Requirement: The forecast degrades to a typed state, and the brief keeps working without it

A network feature must fail without taking the app with it. Every failure SHALL resolve to one **typed
state**, each with its own stated wording as a string resource (section "Localisation"), and in **every** one
of them the brief SHALL still work: the dark window, the moon, the targets and the satellite passes are local
computations and SHALL be unaffected, the forecast row SHALL degrade alone, and the share handoff SHALL still
produce a brief (design.md § "Weather"; annex/L-depth-retention-edges.md E45–E51, E54, E55).

| Typed state | Trigger | What the user sees | What is refused |
|---|---|---|---|
| `NO_NETWORK` | no usable network | with a cache: the `OfflineReadingFrom` wording, `No forecast available. Last reading {age} old: {value} % cloud.`; without a cache: the `NoForecast` wording, `No forecast available.` | any request, and any fresh-looking value |
| `SERVICE_UNREACHABLE` | the host does not answer within the app's request timeout | the `ForecastUnavailable` wording, `Forecast unavailable.` | a retry loop; presenting the previous value as current |
| `SERVICE_ERROR` | a response status other than 200, 203, 304 or 429 | `ForecastUnavailable.` | parsing a body that is not a usable forecast |
| `RATE_LIMITED` | HTTP 429 | the `ForecastTemporarilyUnavailable` wording, `Forecast temporarily unavailable.` | retrying before the next permitted refresh instant |
| `NO_HOURS_IN_WINDOW` | a 200 response whose `timeseries` carries no hour inside the session window — **this is also the state a coordinate outside the service's coverage produces** | the `ForecastNoHours` wording, `The forecast does not cover this window.` | drawing any cell; inventing a value for the window |
| `MALFORMED_RESPONSE` | a body that is not valid JSON, or lacks `properties.timeseries` or the details field | `ForecastUnavailable.` | any partial or guessed render |
| `OUT_OF_COVERAGE` | not distinguishable by a dedicated status code in this product | the same `NO_HOURS_IN_WINDOW` wording above; the reason is recorded as `OUT_OF_COVERAGE` rather than shown as a second sentence | any request to a different service, and any invented coverage polygon |

The app SHALL NOT invent a geographic coverage boundary to test a coordinate against: it SHALL decide
coverage from whether the response the service already returns carries any hour for the window, which is the
only signal it has (annex/N-data-algorithms.md §4.4, §4.5). A `203` response SHALL additionally raise the deprecated-version warning
of the first requirement. In no state SHALL the app fabricate a cloud value, and in no state SHALL the
forecast's failure block, delay or alter the local ephemeris.

#### Scenario: No network with a cache

- **WHEN** the device has no network and a cached forecast exists, aged `6 h 12 min`
- **THEN** the cloud row states `No forecast available. Last reading 6 h 12 min old: 40 % cloud.`
- **AND** the dark window, the moon, the targets and the passes are shown normally

#### Scenario: No network and no cache

- **WHEN** the device has no network and no cached forecast exists for the spot
- **THEN** the user sees `No forecast available.`
- **AND** the brief still renders every local calculation and still shares

#### Scenario: The service does not answer

- **WHEN** the request times out with no response
- **THEN** the user sees `Forecast unavailable.`
- **AND** no retry loop is started

#### Scenario: The service returns an error

- **WHEN** the service returns HTTP 500
- **THEN** the user sees `Forecast unavailable.`
- **AND** no value is derived from the error body

#### Scenario: The service throttles the app

- **WHEN** the service returns HTTP 429
- **THEN** the user sees `Forecast temporarily unavailable.`
- **AND** no further request is made before the next permitted refresh instant

#### Scenario: No returned hour falls inside the window

- **WHEN** a 200 response carries hours, none of which is inside the session window
- **THEN** the user sees `The forecast does not cover this window.`
- **AND** no cell is drawn and no value is invented for the window

#### Scenario: The response is malformed

- **WHEN** the body is not valid JSON
- **THEN** the user sees `Forecast unavailable.`
- **AND** nothing is partially rendered and no other service is contacted

#### Scenario: A coordinate the service does not forecast

- **WHEN** a spot's response carries no usable hour for the window because the coordinate is outside the
  service's coverage
- **THEN** the user sees the `ForecastNoHours` wording, and the recorded reason is `OUT_OF_COVERAGE`
- **AND** the app contacts no other weather service and tests no invented coverage boundary

### Requirement: The user is told a coordinate leaves the device, and the forecast is optional

Sending a saved spot's coordinate to `api.met.no` is a **collection** under Play's definition — data
transmitted off the device *irrespective of destination* (annex/M-play-integrations-compliance.md §3) — so the app SHALL disclose it, and SHALL
remain fully useful without it.

- **What leaves the device.** The truncated coordinate (`4` decimals, ≈ `11 m`), the altitude in metres, the
  request time, and the app's `User-Agent` including its version name and support address.
- **State it where it is relevant.** The first time the app fetches a forecast for a spot, a plain,
  non-blocking sentence SHALL appear beside the cloud row stating that the spot's coordinate is sent to
  **`api.met.no`** to get the forecast, with a link to the privacy policy. The disclosure SHALL NOT be a
  launch-blocking consent gate and SHALL NOT be shown for a job the user has not asked for.
- **A permanent statement.** The Settings/About surface SHALL carry the same statement, so the fact is
  findable later and not only at the moment of the fetch (`settings-and-credits` owns the row).
- **Decline is available and costs nothing but the forecast.** The Settings surface carries a **Cloud
  forecast** preference that, when off, disables **every** forecast fetch and makes the cloud row state
  `No forecast available.`. With it off and with no network, the user still gets **the dark window, the
  targets and the satellite passes** — the whole local product — and the share handoff still works.
- **No fetch before a spot exists.** The app SHALL NOT fetch a forecast at launch, before the user has
  saved or selected a spot, or for a default, placeholder or `0, 0` position. It SHALL NOT fetch a
  coordinate for a spot the user is not looking at.
- **The declaration.** The store pack declares **Location → Approximate location, collected, App
  functionality, optional, encrypted in transit**, and the privacy policy **names MET Norway as a
  recipient**; the app SHALL NOT contradict that declaration (`store/data-safety.md`; annex/M-play-integrations-compliance.md §3).

#### Scenario: No fetch until a spot exists

- **WHEN** the app launches with no saved spot
- **THEN** no forecast request is issued
- **AND** the Brief prompts the user to save a spot instead of showing a fabricated forecast

#### Scenario: The disclosure appears with the forecast

- **WHEN** the app fetches the first forecast for a spot
- **THEN** a sentence beside the cloud row states that the spot's coordinate is sent to `api.met.no`, with a
  link to the privacy policy
- **AND** it is not a launch-blocking gate and blocks no feature

#### Scenario: Declining the forecast still leaves the whole local product

- **WHEN** the user turns the Cloud forecast preference off, or has no network
- **THEN** the dark window, the moon, the targets and the satellite passes are all shown
- **AND** the cloud row states `No forecast available.` and no request is made

#### Scenario: Only the viewed spot is sent

- **WHEN** a spot is fetched and the user has several saved spots
- **THEN** only the coordinate of the spot in view is sent
- **AND** no coordinate of any other saved spot leaves the device in that session

#### Scenario: The app does not contradict its own declaration

- **WHEN** the shipped client's destinations are compared with the pack's Data safety declaration
- **THEN** `api.met.no` is disclosed to the user and named as a recipient in the privacy policy
- **AND** Location → Approximate (App functionality, optional, encrypted in transit) is declared, and no
  other destination leaves the device

### Requirement: Every string is a resource in both locales, and every control is named and reachable

The capability is born multilingual (`localization`) and accessible, and this holds for the cloud row, its
labels, its attribution and every failure sentence above.

- **Every user-facing string SHALL be a resource**, present in **both** `res/values/strings.xml` and
  `res/values-ru/strings.xml`: the label **forecast**, the three state labels (**Clear**, **Partly cloudy**,
  **Overcast**), the attribution string and its "shown unmodified" note, the model-time and fetch-age
  wording, the unavailable-hour wording, every sentence in the failure table above, and the disclosure
  sentence. No cloud wording SHALL be hardcoded in Kotlin prose, and the proper nouns of the attribution
  SHALL be identical in both locales.
- **Every control SHALL be reachable via TalkBack with a role and a name**, and SHALL have a touch target of
  at least **48 dp**. A cloud cell SHALL announce its hour and its state label **and its numeric fraction in
  per cent**, so the fraction is never conveyed by colour or position alone, and the row SHALL announce the
  word **forecast** together with the retrieval time.
- **Numbers, times and units SHALL be formatted with locale-aware formatters** (`%` reading, local hour for
  the user's clock, a spelled-out age), so the row is legible in `en` and `ru` and never relies on an English
  string's length to fit.

#### Scenario: The labels exist in both locales

- **WHEN** the app is built in the `en` and `ru` products
- **THEN** every cloud label, failure sentence, attribution string and disclosure sentence resolves from
  `values/strings.xml` and `values-ru/strings.xml`
- **AND** no cloud prose is hardcoded in Kotlin

#### Scenario: A cell is named for a screen reader

- **WHEN** TalkBack focuses a cloud cell for 23:00 local with `62 %` total cloud
- **THEN** it announces the hour, the label **Partly cloudy**, and `62 %`
- **AND** the fraction is available without needing the colour

#### Scenario: Controls meet the touch-target floor

- **WHEN** every interactive cloud control is measured
- **THEN** each has a touch target of at least `48 dp`
- **AND** it carries a TalkBack name and role

#### Scenario: The attribution is unchanged in every locale

- **WHEN** the app runs in `ru`
- **THEN** the attribution string, the source name and the licence name are unchanged, and the surrounding
  labels come from `values-ru/strings.xml`

## MODIFIED Requirements

None. This change adds a new capability and modifies no existing spec.
