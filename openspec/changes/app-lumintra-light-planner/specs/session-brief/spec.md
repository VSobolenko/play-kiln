# Session brief

## Purpose

The Session brief is the answer Lumintra exists to give. It takes one saved spot, one night and — when
the user has one — one target, and composes a single chronological answer: when the sky is actually
dark and moonless, where the Moon is, when the golden, blue and twilight bands fall, what the cloud
outlook is, whether the user's targets are up, and whether a satellite will cross the frame. It opens
already resolved for tonight at the most recent spot with zero configuration, works with no network,
and never presents a forecast or a prediction as a measurement or a fabricated darkness the sky does
not have. This capability owns the dark window, the "Tonight" summary, the twilight table, the moon
state, the brief's ordered conditions, its time display, its freshness labels and its edge states; the
scrubbable scene that draws the brief is the `night-scene` capability.

## ADDED Requirements

### Requirement: The dark window is a computed interval with a typed outcome, never a bare interval

The app SHALL compute the session's dark window as the set of intervals inside the session window
`[localNoon, localNoon + 24 h)` for the spot in which the Sun's **geometric** altitude (no
refraction, no semidiameter) is at or below **−18.0°**. The Moon's altitude is **not** part of this
test; it defines the separate **moonless window** — the sub-intervals of the dark window in which the
Moon is below the horizon (see the moon requirement below). The window SHALL be a half-open interval
`[start, end)`, an
**array** of one or more intervals (two windows separated by a twilight dip are legal and both render,
E10), all instants in UTC, and all durations computed in UTC — never from two formatted date strings
(E67). The report SHALL be `DarkWindowReport` carrying a `kind` from the closed set
**`DARK`, `POLAR_DAY`, `POLAR_NIGHT`, `NO_ASTRO_DARK`, `NO_DARK`** (domain `DarkWindowKind`) and the
interval list, which MAY be empty. A `null` from an engine crossing search is a **case**, never an
error: the app SHALL NOT substitute a synthetic time, throw, or render a zero-length interval as an
interval (E01).

The five outcomes and their exact user-facing sentences (each a string resource that MUST exist in
both `values/strings.xml` and `values-ru/strings.xml`) are:

| `kind` | Condition | Sentence shown |
|---|---|---|
| `DARK` | at least one Sun ≤ −18.0° interval exists | the dark window's two bounds and duration are stated, not a sentence |
| `POLAR_DAY` | the Sun never sets (`searchRiseSet(Set)` is `null`) and `sunAltMinDeg > −0.8333°` | `NoSunsetTonight` = "The Sun does not set tonight at {spot} — there is no darkness." |
| `POLAR_NIGHT` | the Sun never rises (`searchRiseSet(Rise)` is `null`) and `−18.0° < sunAltMaxDeg < −0.8333°` | `NoSunriseToday` = "The Sun does not rise at {spot} today — the sky is dark except for a twilight dip around midday." |
| `NO_ASTRO_DARK` | no Sun ≤ −18.0° crossing exists but `sunAltMinDeg < −6.0°` | `NoAstroDark` = "No astronomical darkness at {spot} tonight — the sky only reaches {level} twilight (Sun {alt}° at {time})." where `{level}` is "nautical" when `sunAltMinDeg < −12.0°` and "civil" otherwise |
| `NO_DARK` | the Sun crosses the horizon tonight (both `searchRiseSet(Set)` and `searchRiseSet(Rise)` are non-`null`) but `sunAltMinDeg ≥ −6.0°`, so no Sun ≤ −18.0° crossing exists | `NoDark` = "No darkness at {spot} tonight — the Sun stays above civil twilight (lowest {alt}° at {time})." |

**The five outcomes are a partition of the possible nights, resolved by a fixed precedence — not five
independent tests that are shown together.** The report SHALL carry **exactly one** `kind`, chosen by
evaluating the conditions as an **ordered decision list, first match wins**, and a night SHALL NOT be
reported as two kinds:

1. **`POLAR_DAY`** — the Sun does not set and `sunAltMinDeg > −0.8333°`.
2. **`POLAR_NIGHT`** — the Sun does not rise and `−18.0° < sunAltMaxDeg < −0.8333°`.
3. **`DARK`** — at least one Sun ≤ −18.0° interval exists.
4. **`NO_ASTRO_DARK`** — no Sun ≤ −18.0° crossing exists and `sunAltMinDeg < −6.0°`.
5. **`NO_DARK`** — otherwise: the Sun crosses the horizon tonight but `sunAltMinDeg ≥ −6.0°`.

A lower-precedence condition SHALL NOT be reported when a higher one matches, and no state the list did
not select SHALL contribute a sentence or a darkness claim to the brief. `POLAR_DAY` and `NO_DARK`
cannot match a night another state already claimed, because their own `searchRiseSet` tests exclude it;
the two overlaps that **do** occur are **`POLAR_NIGHT` against `DARK`** and **`POLAR_NIGHT` against
`NO_ASTRO_DARK`** — a polar night where the Sun never rises can also contain a Sun ≤ −18.0° interval
(the `DARK` overlap), or can also stay above −18.0° throughout (the `NO_ASTRO_DARK` overlap). Both are
resolved by `POLAR_NIGHT` standing ahead of them, and the resolution is deliberate: on a polar night the
fact that **the Sun does not rise** is the more specific fact about where the user is, while "there is a
dark interval" is what a mid-latitude night already does, so the more specific state and its single
sentence are the ones reported. The reported state's sentence and the darkness facts rendered beside it
SHALL then be consistent with each other — a `POLAR_NIGHT` report SHALL NOT also state a `DARK` night, a
`DARK` report SHALL NOT claim the Sun does not rise, and the brief SHALL never present two states'
sentences for one night.

For every non-`DARK` outcome the brief SHALL still state what the sky actually does, rather than a dark
window it does not have: on `POLAR_DAY` the brief's opening answer is the **light** window (the Sun's
minimum altitude and the instant it occurs); on `POLAR_NIGHT` the brief reports the dark window with
its **midday twilight dip** named as a sub-interval and the Sun's maximum altitude and its time; on
`NO_ASTRO_DARK` the brief reports the deepest twilight window actually reached (nautical or civil) and
the deepest Sun altitude and its instant; on `NO_DARK` the brief reports the civil twilight reached and
the Sun's minimum altitude and its instant. This is the ordinary mid-latitude June case — astronomical
darkness is absent at roughly 52.5 °N for about two months of summer (E07) — so it is on the main path,
never a latitude special case. The Night scene SHALL render the true sky from the Sun's altitude through
the **same twilight LUT as every other night**: for every non-`DARK` outcome the app SHALL NOT render a
black starry sky, SHALL NOT show an empty window or a `00:00–00:00` interval, and SHALL NOT offer the
dark-window reminder (E02, E03, E04, E05).

#### Scenario: A normal night produces one dark interval

- **WHEN** the brief is computed for a mid-latitude spot on a date with astronomical darkness
- **THEN** the report is `kind = DARK` with at least one interval whose start, end and duration are
  stated, and the instant the Sun reaches its lowest altitude is shown

#### Scenario: An ordinary June night has no astronomical darkness

- **WHEN** the brief is computed at roughly 52.5 °N in late June, where the Sun never falls below
  −18.0°
- **THEN** the report is `kind = NO_ASTRO_DARK`, the exact `NoAstroDark` sentence is shown with the
  deepest level reached and the Sun's altitude and instant, and no black starry sky is drawn

#### Scenario: Polar day reports the light, not a window

- **WHEN** the Sun does not set at the spot
- **THEN** the report is `kind = POLAR_DAY`, the exact `NoSunsetTonight` sentence is shown, the Sun's
  minimum altitude and its instant are stated, and no dark window and no reminder are offered

#### Scenario: Polar night keeps the dark window and names the dip

- **WHEN** the Sun does not rise and the sky still reaches −18.0° except for a midday dip
- **THEN** the report is `kind = POLAR_NIGHT`, the exact `NoSunriseToday` sentence is shown, the dark
  window is reported with the midday twilight dip named as a sub-interval, and the Sun's maximum
  altitude and its time are stated

#### Scenario: A deep polar night is one outcome, not two

- **WHEN** the Sun does not rise at the spot and the sky also falls to or below −18.0° around local
  midnight, so both the never-rising condition and a Sun ≤ −18.0° interval are true
- **THEN** the report is exactly one `kind = POLAR_NIGHT` and the exact `NoSunriseToday` sentence is
  shown
- **AND** the brief does not also report a `DARK` kind, its dark-window bounds or duration, or any
  second state for the same night

#### Scenario: A never-rising Sun outranks the no-astronomical-darkness outcome

- **WHEN** the Sun does not rise at the spot and the sky also never falls to −18.0°, so both the
  never-rising condition and the no-astronomical-darkness condition hold
- **THEN** the report is the single `kind = POLAR_NIGHT` and `NO_ASTRO_DARK` is not reported for the
  same night

#### Scenario: The precedence is an ordered decision list

- **WHEN** more than one of the five conditions holds for one night
- **THEN** the reported kind is the first match in the order `POLAR_DAY`, `POLAR_NIGHT`, `DARK`,
  `NO_ASTRO_DARK`, `NO_DARK`, and exactly one kind is reported
- **AND** no state the list did not select contributes a sentence or a darkness claim

#### Scenario: A night that never reaches civil twilight

- **WHEN** the Sun's lowest altitude that night is above −6.0°
- **THEN** the report is `kind = NO_DARK` and the exact `NoDark` sentence is shown

#### Scenario: Two dark windows in one night

- **WHEN** the Sun crosses −18.0° down and up twice inside the session window
- **THEN** `darkWindows` has length 2, each interval carries its own duration, the longest is named as
  the best window, and the brief says there are two windows tonight

#### Scenario: Twilight and sunrise are never interchanged

- **WHEN** the dark window and the sunset instant are both computed for one night
- **THEN** the dark window derives from the Sun's geometric centre at −18.0° and the sunset derives
  from the 90.8333° zenith distance (50′ below the horizon = 16′ semidiameter + 34′ refraction), and
  neither threshold is used for the other test

#### Scenario: A missing crossing is a typed case, not a failure

- **WHEN** an engine crossing search returns `null` for the requested event
- **THEN** the caller returns a typed `None`, the brief renders the typed sentence for the missing
  event, and no exception is thrown and no substitute time is invented

### Requirement: The twilight and key-times table states its exact definitions and instants

The Session brief SHALL present a scrollable table of the night's key times for the chosen night, with
one row per band: **golden hour**, **blue hour**, **civil twilight**, **nautical twilight** and
**astronomical twilight**. The twilight rows SHALL use the USNO geometric Sun-centre angles — civil at
**−6.0°**, nautical at **−12.0°**, astronomical at **−18.0°**, each with no refraction and no
semidiameter — and each row SHALL show the band's **start and end instants** for that night. The table
SHALL state that these are three distinct twilight tests and three distinct bands, not synonyms: civil
is not nautical, nautical is not astronomical, and no twilight row is the sunset or the sunrise (which
is the separate 90.8333° test). The table SHALL state that sunrise and sunset are **different instants**
from the twilight boundaries and MAY show them as their own rows.

The golden and blue hours have no single official definition, so the app SHALL use, and the
requirement states as a **product decision rather than a measured fact**: **golden hour** is the
period in which the Sun's geometric altitude is between **+6.0° and −4.0°**, and **blue hour** is the
period in which it is between **−4.0° and −8.0°**. Golden hour and blue hour are therefore two distinct
rows that neither overlap nor repeat a twilight row, and the labels are the user's language (English or
Russian), not an untranslated term. Bands are half-open `[start, end)` intervals; a band of zero length
SHALL be dropped rather than drawn as a hairline or labelled on a colliding boundary (E11). A band that
does not occur on the chosen night (a polar night or day) SHALL be hidden, not drawn at zero width
(E03).

#### Scenario: The table carries every row with its instants

- **WHEN** the brief is shown for a chosen night with a full twilight cycle
- **THEN** golden hour, blue hour, civil, nautical and astronomical twilight each appear as a row with
  its start and end instants for that night

#### Scenario: Golden hour and blue hour are distinct

- **WHEN** the table is read
- **THEN** golden hour and blue hour are separate rows covering different Sun-altitude ranges, and
  neither is presented as a synonym of the other

#### Scenario: Twilights are never labelled as sunrise or sunset

- **WHEN** the table and the sunrise/sunset rows are compared
- **THEN** the civil, nautical and astronomical rows are labelled as twilight bands and their instants
  differ from the sunrise and sunset instants

#### Scenario: A band that does not occur is not drawn

- **WHEN** the chosen night has no civil twilight (a polar night)
- **THEN** the civil row is absent rather than rendered with zero width

#### Scenario: Every label is a resource in both locales

- **WHEN** the table's strings are read
- **THEN** each row label and each band name exists as a resource in both `values/strings.xml` and
  `values-ru/strings.xml`

### Requirement: The Tonight summary answers with no configuration

The `Brief` screen SHALL open on a **Tonight** summary that answers the four questions a user arrives
with — the spot's dark window, the Moon's state, the cloud outlook, and whether any of the user's
targets are up — with **zero configuration**: it resolves automatically to tonight at the most recently
used spot and the current instant. Before any spot exists the summary SHALL show a scoped empty state
with exactly one call to action that opens `SpotEdit`, and SHALL NOT compute a sky for a default or
`(0, 0)` position (E107, E110). Before any target is chosen the summary SHALL still show the dark
window, the Moon and the cloud outlook, and its target row SHALL state that no target has been chosen
yet.

The Tonight summary SHALL never block on the network: every one of its four rows SHALL render within
one frame from local computation and cache, no row SHALL await a fetch to appear, and an unavailable
network row SHALL render a typed "no data" state with its reason rather than a spinner that never
resolves (E108). The cloud-outlook row SHALL carry the source's hourly total cloud cover **drawn across
the session window's hourly points** and SHALL mark **the dark window within it**, aligning the row to
that mark — so the row's extent is the session window while the marked dark window is a shorter extent
inside it. **Which hourly points carry a cell is owned by the `cloud-forecast` capability** (its
requirement **"The cloud is presented aligned against the dark window and the target's altitude
curve"**, cited here rather than restated), and **the dark window and the mark that bounds it are this
capability's own** (the requirement **"The dark window is a computed interval with a typed outcome,
never a bare interval"** above). The row SHALL also carry the retrieval time and the word "forecast"
(see the freshness requirement); when there is no cached reading it SHALL read `NoForecast` = "No
forecast available." (E49).

#### Scenario: Cold start with no spot

- **WHEN** the app opens with no saved spot and no location
- **THEN** the summary shows a scoped empty state with one call to action that opens `SpotEdit`, and no
  sky is computed for a fabricated position

#### Scenario: Cold start with a spot but no target

- **WHEN** the app opens with one saved spot and no target chosen
- **THEN** the summary shows the spot's dark window, the Moon's state and the cloud outlook, and the
  target row states that no target has been chosen yet

#### Scenario: The summary never waits on the network

- **WHEN** the device has no network, or a fetch is outstanding
- **THEN** the summary renders its rows without waiting, the cloud row shows its typed no-data state,
  and every locally computed row is present

#### Scenario: The cloud row is labelled a forecast with its age

- **WHEN** the summary shows a cached forecast
- **THEN** the row states "total cloud cover", the retrieval time, and the word "forecast"

#### Scenario: The cloud row's extent is the session window, the dark window a mark inside it

- **WHEN** the summary shows the cloud row for a night whose dark window is shorter than the session window
- **THEN** the row is drawn across the session window's hourly points — including hours outside the dark
  window — and not only across the dark window
- **AND** the dark window is marked within the row and the row is aligned to that mark, so the row's
  extent and the marked dark window are two different extents

### Requirement: The Moon is stated as a state with both numbers, never a single boolean

The app SHALL compute the Moon from exactly one source (the vendored ephemeris: `moonPhase` for
elongation and `illumination()` for `phaseFraction`, `phaseAngle` and magnitude) with one refraction
convention shared by the brief, the scene and the widget (E21). The brief SHALL state the Moon's
**illuminated fraction** as a number in `[0.0, 1.0]` (shown as a percentage) **and** the Moon's
**altitude** in degrees, because a 5 % crescent ten degrees above the horizon is not moonless in
practice and one boolean would hide that. The app SHALL name the phase with eight 45°-wide bins
centred on each exact phase (offset 22.5°), so a waxing crescent near 359° is not bucketed as new
(E19).

The app SHALL compute the **moonless window** as the set of intervals inside the dark window in which
the Moon is below the horizon (E15, E16); "moonless" SHALL be the product threshold
**`illuminatedFraction ≤ 0.10`** — a **product decision, stated as such, not a measured fact** —
combined with the geometric test `moonUp = moonAltDeg > 0.0`, so `moonless = (!moonUp) ||
(illuminatedFraction ≤ 0.10)` (E18). When the Moon never rises the brief SHALL say so ("the Moon does not
rise tonight"); when it never sets it SHALL say so; both are typed outcomes, never a default rise or
set time (E20). When the Moon is up for the whole dark window the moonless interval is **empty** and
reported as a distinct typed outcome alongside the Moon's **minimum altitude and its instant inside
the window** — the actionable instant when the Moon is least in the way — never an empty panel, a
dash, or a fabricated zero-length interval (E14). The moonless window SHALL be computed from the
Moon's **apparent** (drawn-horizon-masked) visibility, so a rise behind the user's ridge is stated as
such rather than counted (E78).

#### Scenario: The Moon is down for the whole dark window

- **WHEN** the Moon's altitude is at or below 0.0° for the entire dark window
- **THEN** the moonless window equals the dark window and the brief states "Moonless all night" (E13)

#### Scenario: The Moon is up for the whole dark window

- **WHEN** the Moon is above the horizon for the entire dark window
- **THEN** the moonless interval is empty as a typed outcome, the brief reports the Moon's minimum
  altitude and its instant within the window, and no zero-length interval is shown (E14)

#### Scenario: The moonless threshold is a product decision

- **WHEN** the Moon is above the horizon with an illuminated fraction of 9 % on one night and 11 % on
  another
- **THEN** the 9 % night is reported moonless due to the `≤ 0.10` threshold, the 11 % night is not, and
  the brief states both the fraction and the altitude so the decision is visible

#### Scenario: The Moon never rises

- **WHEN** the Moon does not rise on the chosen night
- **THEN** the brief states that the Moon does not rise tonight, and no rise time is invented

#### Scenario: Phase naming wraps correctly

- **WHEN** the Moon's elongation is near 360° (just before new)
- **THEN** the phase is named as a waxing crescent, not as new, because the bins are centred on each
  exact phase (E19)

#### Scenario: Both numbers are always shown

- **WHEN** the Moon's state is rendered
- **THEN** the illuminated fraction and the altitude are both present, and neither is replaced by a
  single moonless boolean

### Requirement: A full Moon at the antisolar point washes out the sky and leaves no usable dark window

The washed-out case is the **other extreme** of the same illuminated-fraction constant this capability
already states: the moonless threshold (`illuminatedFraction ≤ 0.10`, E18), whose **complement** is the
product threshold for "washed out", so the app introduces no second number. When the Moon is **full** —
named full by the eight 45°-wide phase bins (elongation within the full-moon bin, ±22.5° of 180°, E19)
and carrying an illuminated fraction of **at or above 0.90** — and therefore sits near the **antisolar
point**, the brief SHALL state the sky is **washed out** and SHALL report the Moon's **maximum altitude
and the instant it occurs**. This is the deliberate opposite of the all-night-crescent case (E14),
which reports the Moon's **minimum** altitude: a full Moon is by construction above the horizon all
night and nearest its maximum altitude at local midnight, so the *least-in-the-way* instant does not
exist and the *most-in-the-way* instant is the one that matters (E17).

On that night the moonless window SHALL be **empty**, and the brief SHALL state that there is **no
usable dark window for deep-sky work**. It SHALL NOT report a moonless interval that does not exist,
and SHALL NOT render the empty case as a fabricated zero-length interval, a dash or a blank panel — the
empty window is the typed outcome the all-night Moon already carries (E17, E14). This does **not**
remove the dark window itself, which stays defined by the Sun alone (see the dark-window requirement).

A full Moon SHALL NOT suppress the brief's other rows: the dark window, the cloud outlook and the
target rows SHALL all remain. The target rows SHALL carry the honest **"washed out"** condition rather
than being silently dropped, and their classification is owned by the **`target-catalogue`** capability
(each target's rise, transit and set inside the dark window), cited here rather than restated. The
washed-out statement SHALL describe the sky's **brightness qualitatively** and SHALL NOT assert a
measured or forecast sky brightness, which this capability does not compute.

#### Scenario: A full-Moon night reports the maximum altitude, never a fabricated moonless window

- **WHEN** the Moon is full (illuminated fraction at or above 0.90, near the antisolar point) and above
  the horizon for the whole dark window
- **THEN** the brief states the sky is "washed out", reports the Moon's maximum altitude and the
  instant it occurs, and its moonless window is empty
- **AND** no moonless interval is invented and no zero-length interval or blank panel is shown (E17)

#### Scenario: The full-Moon and all-night-crescent cases are opposite extremes

- **WHEN** the brief is read on a full-Moon night and on an all-night crescent night
- **THEN** the full-Moon night reports the Moon's maximum altitude and its instant, and the all-night
  crescent night reports the Moon's minimum altitude and its instant (E17, E14)
- **AND** neither night's altitude statement is reported for the other

### Requirement: The brief is one chronological, ordered answer of named conditions

The Session brief SHALL be assembled as `SessionBrief`, a single ordered list of `BriefCondition`
records. Each condition SHALL carry a **kind** from the closed set **`DARK_WINDOW`, `MOON`, `CLOUD`,
`TARGET`, `SATELLITE`, `HORIZON`** (`ConditionKind`), a **state** that is one of **`GOOD`, `MARGINAL`
or `BLOCKING`**, and a **source** naming the input the value came from together with that input's age,
**or** an **absence** naming why the source is missing. The conditions SHALL be ordered
chronologically by the instant each one attaches to, ascending; a condition that is not attached to a
single instant (the cloud outlook, an all-night moon) SHALL be anchored to the dark window's start;
ties at the same instant SHALL be broken by the fixed precedence `DARK_WINDOW`, `MOON`, `CLOUD`,
`TARGET`, `SATELLITE`, `HORIZON`. A condition whose required external input is absent SHALL be marked
**`UNAVAILABLE`** with its typed reason, SHALL NOT be assigned any of the three assessment states, and
SHALL NOT be omitted: an unavailable condition is never silently dropped, and a marginal or grazing
target (a peak altitude equal to the user's mask, E26) SHALL be reported as `MARGINAL` with its peak
altitude and its duration above the mask rather than as a rise and a set a second apart.

#### Scenario: Every condition carries a state and a source

- **WHEN** the brief is read
- **THEN** each condition shows its state from the closed set and either the source and age of its
  value or the reason it has none

#### Scenario: Conditions are ordered by their anchor instant

- **WHEN** the brief lists a moonrise, a dark-window start and a cloud outlook
- **THEN** the conditions appear in ascending order of their anchor instants, with the cloud outlook
  anchored to the dark-window start

#### Scenario: An unavailable condition is shown, never omitted

- **WHEN** the cloud source is unavailable
- **THEN** the cloud condition still appears with its typed reason, is not assigned a good, marginal or
  blocking state, and is not dropped from the brief

#### Scenario: A grazing target is marginal, not two events

- **WHEN** a target's peak altitude equals the user's altitude mask
- **THEN** the target condition is `MARGINAL` and states the peak altitude and the duration above the
  mask

### Requirement: Times render in the spot's zone on an absolute axis

Every instant SHALL be stored and computed as an absolute instant in UTC, and the rendering zone SHALL
be the **spot's IANA `tzId`** by default, with an explicit toggle to the **device** zone shown as a
badge labelled "device time". When the device's zone differs from the spot's, the brief SHALL state
both in one line — for example `Times in Europe/Oslo (spot) · your device is Europe/Moscow` (E60) — so
a user planning a trip is never shown their phone's clock for a spot abroad. The scene's time axis
SHALL be an **absolute** axis whose labels are derived from the spot's zone, so no geometry depends on
which zone is displayed (E63).

The brief SHALL render correctly across a **DST transition**: on a spring-forward night the axis SHALL
simply never produce the hour that does not exist (displaying `01:59 → 03:00`), and on a fall-back
night the repeated hour SHALL be labelled twice with the offset shown once per label so the two are
distinguishable (E64, E65). A night that spans local midnight or crosses a calendar date SHALL be one
window labelled with both calendar days — `the night of Tue 8 → Wed 9 Sep` — and the scene's requested
date SHALL be the **local date of sunset**, not the device's current date, so asking at 01:30 resolves
to the night in progress (E06, E66, E67). The session's `dateLabel` SHALL be the local date of the UTC
start instant converted with the spot's `ZoneId` — the one place a UTC instant becomes a local date
(E09).
Durations SHALL be computed in UTC and SHALL NOT be derived from two formatted time strings. The
12-hour or 24-hour display SHALL follow one internal representation with the display governed by the
user's setting (defaulting from the platform's 24-hour preference), used by the brief and the scene
alike (E69). When a network response carries a `Date` header that disagrees with the device clock by
more than a few minutes, the brief SHALL raise a one-line warning that the device clock looks off
(E93).

#### Scenario: A spot abroad states the device's zone difference

- **WHEN** the spot's zone differs from the device's zone
- **THEN** every time is rendered in the spot's zone and one line states both zones

#### Scenario: The spring-forward night skips the missing hour

- **WHEN** the chosen night crosses a DST spring-forward transition
- **THEN** the axis steps from 01:59 to 03:00 and never labels 02:30 as a real instant

#### Scenario: The fall-back night is unambiguous

- **WHEN** the chosen night crosses a DST fall-back transition
- **THEN** the repeated hour is labelled twice with the offset shown once per label

#### Scenario: A night spanning midnight is one window

- **WHEN** the session's dark window begins before and ends after local midnight
- **THEN** it is rendered as one window labelled with both calendar days, and it is not cut at 00:00

#### Scenario: Durations come from UTC, not from formatted strings

- **WHEN** the dark window's duration is computed across a date change or a DST transition
- **THEN** the duration equals the difference of the two UTC instants and is unaffected by the labels

### Requirement: The brief states the age of every external input and never calls a forecast a measurement

The brief SHALL state, on every condition whose value comes from an external or cached input, the
**age of that input**: the cloud outlook SHALL carry its retrieval time (`fetchedAtUtc`) and the word
"forecast", and a satellite condition SHALL carry the element set's epoch and its age (E36). When a
value is served from cache with no network, the brief SHALL show the cached value **with its age and an
"offline" label** — `OfflineReadingFrom` = "No forecast available. Last reading {age} old:
{value} % cloud.", a distinct resource from the no-cache `NoForecast` above — and SHALL NEVER present a
stale reading as current (E49). When the response covers
none of the requested window the brief SHALL report a typed `no hours cover this window`, naming the
window and the range the response actually covered, rather than interpolating the nearest hour as if it
covered the window (E51).

The brief SHALL never present a **forecast** or a **prediction** as a **measurement**: cloud cover is
always labelled a forecast with its source and model time, a satellite pass time is always labelled a
prediction with its element-set age, and neither is reworded so that it reads as an observed value. The
MET Norway attribution `Data from MET Norway (met.no) — CC BY 4.0` SHALL appear
**visibly beside the forecast** as well as in Credits, with a link to the licence and the "shown
unmodified" statement, in every declared locale (E48, E55). The word "Yr" SHALL NOT appear in the
credit or anywhere else in the app.

#### Scenario: The cloud row states its age and the word forecast

- **WHEN** the brief shows the cloud outlook
- **THEN** it states the retrieval time and the word "forecast" beside the source's total cloud cover

#### Scenario: The satellite row states the element-set age

- **WHEN** the brief shows a satellite condition
- **THEN** it states the element set's epoch and how old the set is

#### Scenario: An offline cached reading is labelled with its age

- **WHEN** the device is offline and a cached forecast exists
- **THEN** the brief shows the cached value with its age and an "offline" label, and never a value that
  looks current

#### Scenario: No wording turns a forecast into a measurement

- **WHEN** every user-facing string that describes cloud or satellite data is read
- **THEN** each is labelled a forecast or a prediction with its source, and none claims an observed or
  measured value

### Requirement: The brief's edge states are typed, never blank

The brief SHALL define and render a typed state for every one of its edge conditions, and none SHALL be
rendered as a blank panel, a spinner that never resolves, or a plausible default:

- **No spot.** The scoped empty state with one call to action that opens `SpotEdit`; no sky is computed
  for a fabricated position (E107, E110).
- **No target.** The brief is complete without target rows and states that no target has been chosen;
  a target that never rises at the spot is a typed `NeverRises`, excluded from "what is up tonight",
  and its curve is not drawn as a flat line at zero (E22); a circumpolar target is a typed
  `Circumpolar(minAltitude, atInstant)` (E23).
- **No network.** The cloud and satellite conditions degrade to their typed unavailable states with
  their reasons, every locally computed condition remains present, and the brief still shares normally
  (E49, E108).
- **No darkness.** A typed `DarkWindowKind` (see the dark-window requirement) with the sentence for
  that outcome; never an empty list shown as "no darkness tonight".
- **An unreadable stored scout.** The store SHALL load **per record**: one bad record is quarantined,
  the other records load, and the quarantine is reported once — `SavedSpotUnreadable` = "1 saved spot
  could not be read." (E98).
- **A corrupted stored spot pointing at a target that no longer exists.** Targets are referenced by
  stable string id, never by list index; a missing id is dropped with a note and the spot survives —
  `SavedTargetMissing` = "1 saved reference could not be restored." (E99).

A catalogue asset that is unreadable or missing SHALL leave the app running: the Targets screen shows a
typed empty state and the brief omits target rows and says so (E97). The app SHALL NOT block any of
these states behind a permission, a login or a dialog.

#### Scenario: No spot is a scoped empty state, not a fabricated sky

- **WHEN** the brief is opened with no saved spot
- **THEN** it shows one call to action to create a spot and computes no sky for a default position

#### Scenario: A target that never rises is stated, not drawn flat

- **WHEN** a chosen target never rises at the spot's latitude
- **THEN** the target is reported as `NeverRises`, is excluded from tonight's list, and no flat curve
  or rise time is shown

#### Scenario: No network degrades exactly one region

- **WHEN** the device has no network
- **THEN** the cloud and satellite conditions are shown unavailable with their reasons and every
  locally computed condition is present

#### Scenario: One unreadable spot does not lose the store

- **WHEN** the stored spot file holds one corrupt record among valid ones
- **THEN** the valid records load, the corrupt record is quarantined, and the quarantine is reported
  once

#### Scenario: A removed target does not invalidate its spot

- **WHEN** a saved spot references a target id the compiled catalogue no longer contains
- **THEN** the missing reference is dropped with a note and the spot otherwise loads normally

#### Scenario: An unreadable catalogue leaves the app usable

- **WHEN** the bundled catalogue asset is missing or unparseable
- **THEN** the app starts, the Targets screen shows a typed empty state, and the brief omits target
  rows and states why

### Requirement: Every string is a resource in both locales and every control is named and reachable

Every user-facing string this capability renders — the dark-window sentences, the Tonight summary
labels, the twilight table labels, the moon labels and phase names, the brief condition labels, the
freshness and offline labels, the time-zone line, the edge-state messages and the satellite and cloud
labels — SHALL exist as a resource in **both** `values/strings.xml` and `values-ru/strings.xml`, and
SHALL NOT be a literal in code. Every interactive control this capability renders SHALL have a name for
TalkBack (a content description or a labelled role), SHALL be at least **48 dp** in both dimensions,
and SHALL state any state that matters (a selected spot, a toggled zone, a locked field) with a word or
a shape in addition to colour alone (E89, E90). The `Brief` screen's hero region SHALL expose a
semantic summary — the spot, the date, the Sun's altitude, the Moon's state, the dark window and the
current instant — because the scrubbable scene is a graphic and a silent `Canvas` is not accessible.
The brief SHALL reflow with no fixed-height text container and SHALL be legible at a 200 % system font
scale.

#### Scenario: Both locales carry every key

- **WHEN** the strings used by this capability are compared across the two resource files
- **THEN** every key exists in both `values/strings.xml` and `values-ru/strings.xml` and none is a
  hardcoded literal

#### Scenario: Every control meets the size and role floor

- **WHEN** the brief's interactive controls are measured
- **THEN** each is at least 48 dp in both dimensions and each has a TalkBack name

#### Scenario: The hero region has a semantic summary

- **WHEN** TalkBack reads the brief's scene region
- **THEN** it announces the spot, the date, the Sun's altitude, the Moon's state, the dark window and
  the current instant

#### Scenario: State is not carried by colour alone

- **WHEN** a user who cannot distinguish the palette's hues reads the brief
- **THEN** every state that matters is also written or shaped, not only coloured

**Honest boundary, recorded rather than glossed.** This capability specifies what the brief states and
how it is derived; it cannot prove the arithmetic behind those statements is correct. The ephemeris path
this brief depends on is **only numerically verified for the Sun** (0.0248 arcmin against a JPL Horizons
row); the **Moon** path (`illumination`, `searchRiseSet`) is present but has never been verified against
an independent source, so the moon half of the brief is `UNVERIFIED` until a build task runs the Moon
against a handful of Horizons rows exactly as the Sun case already does. The dark window's thresholds and
the twilight angles are USNO definitions (geometric Sun centre at −6/−12/−18°; sunrise/sunset at
90.8333°); the golden-hour and blue-hour bounds and the `≤ 0.10` moonless threshold are **product
decisions**, labelled as such in their requirements. The two dark-window engines (Sun/Moon v.
satellites) are not interchangeable, and a `null` crossing is a case, not an error. What this document
cannot do is prove the scene renders the true sky: that is `night-scene`'s contract and its own device
evidence.

## MODIFIED Requirements

None. This change adds a new capability and modifies no existing spec.
