# Target catalogue

## Purpose

Give the night photographer a browsable, searchable, offline list of the famous things worth pointing
a camera at — the Messier objects and the showpiece NGC/IC nebulae, clusters and galaxies, the planets
and the Moon, and the Milky Way's core — and answer, for the saved spot and the chosen night, when each
one rises, transits and sets inside the dark window, whether it is even up at this latitude, and
whether it fits the lens. The list is authored from designations, coordinates, magnitudes and sizes,
which are facts, so no third-party catalogue licence attaches to the shipped data, and the whole
capability computes on the device with no network and no account.

## ADDED Requirements

### Requirement: The object table is authored and independently verified, not borrowed

The app SHALL ship its deep-sky targets as a single authored asset, `app/src/main/assets/catalog/objects.json`,
wrapped in `{"schemaVersion": 1, "objects": [...]}`, read by `data/CatalogueRepository.kt` in memory and
never downloaded. No row SHALL be copied from a third-party compiled catalogue: the shipped data are
designations, coordinates, magnitudes and apparent sizes, which are facts, authored and cross-checked,
and the following catalogues are named as forbidden and SHALL NOT be shipped or derived from as a
compiled list — **OpenNGC** and **HYG** (CC BY-SA 4.0: share-alike is incompatible with a closed-source
paid APK), **Hipparcos/Tycho** and **Gaia DR3** (CC BY-NC: non-commercial), **Stellarium's** deep-sky
data (GPL: copyleft), **NGC 2000.0** (non-commercial), and **Yale BSC5** (no affirmative licence
grant). Each row SHALL carry every one
of the following fields, with these names, types, units and sign conventions, and no row SHALL omit a
required field:

| Field | Type | Required | Unit / domain |
|---|---|---|---|
| `id` | string | yes | stable ASCII lowercase slug, e.g. `m31`, `ngc7000`, `ic434` |
| `messier` | string \| null | yes | `"M31"` with prefix, else null |
| `ngc` | string \| null | yes | `"NGC 224"`, else null |
| `ic` | string \| null | yes | `"IC 434"`, else null |
| `name` | string | yes | English common/proper name |
| `nameRu` | string \| null | yes | Russian name (hand-authored; `ru` is a declared locale) |
| `type` | enum | yes | the closed set in the next requirement |
| `raDeg` | number | yes | right ascension in **degrees**, `[0, 360)` |
| `decDeg` | number | yes | declination in **degrees**, `[-90, +90]` |
| `epoch` | string | yes | the constant `"J2000.0"` |
| `magV` | number \| null | yes | integrated **V** magnitude, where a V value exists |
| `magB` | number \| null | yes | **B** magnitude, only when no V exists |
| `magSystem` | enum `"V"` \| `"B"` \| null | yes | which of the two is authoritative |
| `magKind` | enum `"integrated"` \| `"peak"` \| `"unknown"` | yes | what the magnitude measures |
| `sizeMajArcmin` | number \| null | yes | **arcminutes**, major axis |
| `sizeMinArcmin` | number \| null | yes | **arcminutes**, minor axis |
| `positionAngleDeg` | number \| null | yes | degrees `[0, 180)`, north through east |
| `aliases` | string[] | yes | may be empty |
| `commonNames` | string[] | yes | may be empty |
| `sourceIds` | object | yes | `{wikidata, heasarc, simbad}` — the references' own ids |
| `verification` | object | yes | `{srcA, srcB, deltaArcmin}` — the independent check record |
| `magDisputed` | boolean | yes | true when two sources' magnitudes disagree by more than 0.50 |
| `notes` | string \| null | yes | free text; discrepancies and unresolved questions |

The `type` field SHALL be one of the closed set `galaxy`, `galaxy_group`, `globular_cluster`,
`open_cluster`, `emission_nebula`, `reflection_nebula`, `planetary_nebula`, `supernova_remnant`,
`dark_nebula`, `asterism`, `double_star` for authored rows; `planet`, `moon` and `sun` SHALL NOT appear
in this file. **Right ascension SHALL be stored in degrees**, never in hours, and `decDeg` in degrees
`[-90, +90]`; the hours/degrees conversion happens once, at the engine boundary, because the astronomy
engine's `horizon()` and `defineStar()` take RA in **hours** (`ra * 15.0`). Positions are ICRS-aligned
and J2000.0; no precession is applied to the stored numbers (`E28`). Each row SHALL carry a
`verification` record naming **two independent sources** — the two SHALL be named as the authors may
use them: **NASA HEASARC** (freely usable, US government; acknowledgement requested) and **Wikidata**
structured data (**CC0 1.0**, properties `P6257` right ascension, `P6258` declination, `P1215` apparent
magnitude), with the object's **discovery paper** as the second source where one exists — and the
computed angular separation `deltaArcmin` between
them, computed with the spherical cosine formula; a row whose separation exceeds **1.0 arcmin** for an
object with `sizeMajArcmin >= 5`, or **0.2 arcmin** for an unresolved object, SHALL be re-sourced or
dropped and SHALL NOT be averaged. A row that cannot be verified from **two independent sources** SHALL
be **omitted** from the shipped table rather than guessed. A magnitude pair disagreeing by more than **0.50 mag** SHALL NOT be
averaged: `magDisputed` SHALL be set true, both values kept in `notes`, and `magSystem`/`magKind` filled
from the source the entry names as primary. The app SHALL NEVER compare a V magnitude with a B
magnitude, nor an integrated magnitude with a peak magnitude: the brightness filter and the
brightest-first ordering SHALL operate within one `magSystem` and one `magKind`.

#### Scenario: Every shipped row carries the full schema

- **WHEN** `assets/catalog/objects.json` is parsed at build time
- **THEN** every row contains all 24 fields with the stated types, `epoch` is `"J2000.0"`, `raDeg` is in
  `[0, 360)`, `decDeg` is in `[-90, +90]`, and no row's `type` is `planet`, `moon` or `sun`

#### Scenario: Right ascension is stored in degrees, not hours

- **WHEN** a row's `raDeg` is read and the value is passed to the engine's `horizon()`
- **THEN** the conversion `raHours = raDeg / 15` happens at the single engine boundary, and no stored
  value is expressed in hours

#### Scenario: A row outside the tolerance is dropped, never averaged

- **WHEN** an authored row's `verification.deltaArcmin` is **1.4** for an object with
  `sizeMajArcmin = 30`
- **THEN** the build-time check fails and the row is re-sourced or removed, and no blended coordinate
  is stored

#### Scenario: A disputed magnitude is flagged, not blended

- **WHEN** two references give magnitudes 8.2 and 8.9 for the same row
- **THEN** `magDisputed` is `true`, `notes` carries both values, and the single stored magnitude is the
  one the named primary source gives

#### Scenario: V and B magnitudes are never mixed

- **WHEN** a magnitude-limit filter is applied to a list containing rows with `magSystem = "V"` and
  rows with `magSystem = "B"`
- **THEN** the comparison is made inside one system, and a row with no value in that system is treated
  as magnitude-unknown rather than compared across systems

#### Scenario: A denylisted catalogue is not the source

- **WHEN** the shipped asset and the repository are searched for the forbidden catalogues' names or
  their compiled data form
- **THEN** no OpenNGC, HYG, NGC 2000.0, Hipparcos/Tycho, Stellarium, Gaia DR3 or Yale BSC5 compiled
  data is present, and the shipped rows are the slot's own authored records

**Honest boundary, recorded rather than glossed.** The check proves that every shipped row carries the
schema, that each carries two named sources with a computed separation inside tolerance, and that no
denylisted catalogue's data is present. It cannot prove the coordinates and magnitudes themselves are
right: a plausible row with a fabricated `deltaArcmin` below tolerance passes. The `srcA`/`srcB` fields
name references a reader can re-open; the truth of the numbers stays a reading, exactly as for a
hand-authored table everywhere else in this app.

### Requirement: The object-selection rule is arithmetic and its count is recorded

The set of authored deep-sky rows SHALL be produced by a rule an author can apply, not by taste, and its
output SHALL be the **roughly 250 famous deep-sky objects** the contract promises. The checkable number
is this rule's output: **110 Messier rows plus 140 non-Messier rows = 250 authored deep-sky rows**,
presented together with the **10 computed solar-system records**. It SHALL contain **all Messier objects
M1–M110** (110 rows) and **the 140 brightest non-Messier NGC or IC objects whose sourced `magV <= 10.0`**;
the magnitude limit SHALL be raised to **10.5 only if fewer than 140 non-Messier objects qualify at
10.0**, and SHALL NOT be raised further, so the authored table reaches **250 rows** whenever the
non-Messier population at the active limit supplies 140 and falls short only if that population is
exhausted. M102 SHALL be shipped as a single documented entry — a duplicate observation of M101 or NGC
5866 — with the uncertainty written in its `notes`; it SHALL NOT be silently shipped twice or silently
omitted. The manifest SHALL record the shipped row count, the non-Messier cap applied, and the magnitude
limit actually used, and each row's `notes` SHALL name the limit it was selected under where the limit
mattered. A row that has no sourced `magV` SHALL NOT qualify under the rule.

#### Scenario: Every Messier object ships

- **WHEN** the shipped rows are checked for the Messier designations
- **THEN** all of `M1`…`M110` are present

#### Scenario: A bright non-Messier object is included

- **WHEN** a non-Messier NGC object carries a sourced `magV` of 9.4 and at least one non-Messier slot
  remains at the active limit
- **THEN** it is present in the shipped rows

#### Scenario: The non-Messier cap bounds the table

- **WHEN** more than 140 non-Messier NGC/IC objects carry a sourced `magV` at or below the active limit
- **THEN** only the 140 brightest are shipped, the table holds 250 authored rows in total, and the
  manifest records the cap that produced it

#### Scenario: A faint non-Messier object is excluded

- **WHEN** a non-Messier NGC object carries a sourced `magV` of 11.3 under the 10.0 limit
- **THEN** it is not present in the shipped rows

#### Scenario: The shipped count is the rule's output and is recorded

- **WHEN** the catalogue manifest is read
- **THEN** it states the shipped row count and the magnitude limit used, and the count equals the rule's
  output for that limit

#### Scenario: M102 is resolved and documented once

- **WHEN** the rows are checked for `M102`
- **THEN** exactly one row carries the designation and its `notes` states the duplicate/ambiguous
  history

### Requirement: Solar-system bodies are computed for the instant, never authored

The Sun, the Moon, Mercury, Venus, Mars, Jupiter, Saturn, Uranus, Neptune and Pluto SHALL be presented
as **computed** records built at the observation time and SHALL NEVER be rows in `objects.json`. A body
SHALL be presented with the same view fields a catalogue row uses, plus `ephemeris = "computed"`, and
SHALL carry: `id` (`"planet-mars"`, `"moon"`, `"sun"`), `type` (`planet`/`moon`/`sun`), `raDeg` and
`decDeg` (the geocentric apparent position at the instant), `azimuthDeg` and `altitudeDeg` (from
`horizon()` with `Refraction.Normal`), `magnitude` (from `illumination(body, t).mag`), `phaseAngleDeg`
(from `illumination(body, t).phaseAngle`), and `illuminatedFraction` (from
`illumination(body, t).phaseFraction`, in `[0.0, 1.0]`). The bodies SHALL be computed by the
`astronomy.kt` port (VSOP87/ELP built in, no runtime data files), not by Orekit, whose planetary
ephemeris needs JPL DE kernels; the two engines are not interchangeable for a given body. When a body
is **below the effective horizon** it SHALL be shown as a computed record with a below-horizon state and
its computed altitude, and when it is **behind the Sun** (its angular separation from the Sun is small
enough that it is not usefully observable) it SHALL be shown as a behind-the-Sun state naming the
separation; in neither case SHALL the app hide the record, blank the field, or display a stale
coordinate.

#### Scenario: A planet carries magnitude, phase angle and illuminated fraction

- **WHEN** a planet record is built for a fixed instant
- **THEN** it carries its magnitude, its phase angle in degrees, and its illuminated fraction in
  `[0.0, 1.0]`, all from the same `illumination()` call

#### Scenario: A planet below the horizon is a stated state, not an absence

- **WHEN** a planet's computed altitude is below the user's effective horizon for the chosen instant
- **THEN** the record is still listed and shows its negative altitude with a "below the horizon" state

#### Scenario: A planet behind the Sun is stated, not blanked

- **WHEN** a planet's separation from the Sun places it behind the Sun
- **THEN** its record shows a "behind the Sun" state naming the geometry rather than a coordinate or a
  blank

#### Scenario: A solar-system body never appears in the authored table

- **WHEN** `assets/catalog/objects.json` is parsed
- **THEN** no row has `type` of `planet`, `moon` or `sun`, and the Sun, Moon and planets appear only as
  computed records

### Requirement: Constellations are 88 regions, not 88 points

The app SHALL ship the 88 IAU constellations in their own authored asset,
`app/src/main/assets/catalog/constellations.json`, as `{"schemaVersion": 1, "constellations": [...]}`,
each row carrying `{id, nameEn, nameRu, nameLatin, abbrev, raCenterDeg, decCenterDeg, areaRank}`, with
`abbrev` the three-letter IAU abbreviation and `raCenterDeg`/`decCenterDeg` in degrees. Every field SHALL
be read from its named reference rather than invented: the region identity and the centre coordinates
SHALL come from the **IAU constellation boundary data** (public domain — the Delporte-derived boundaries
the IAU publishes), the `areaRank` ordering SHALL come from the **published IAU area ranks**, and the
English and Latin names SHALL come from the **IAU WGSN** star/constellation naming (**CC BY 4.0**,
credited `IAU WGSN`). The figure polylines SHALL be loaded from
`app/src/main/assets/sky/constellation_lines.json`, a JSON object keyed by the IAU three-letter
constellation abbreviation whose every value is an array of polylines, each polyline an array of
`[raHours, decDegrees]` vertices, with that file's source and licence recorded beside it so exactly one
parser is built for it. The lines SHALL come from the **IAU / Sky & Telescope constellation figures**
(**CC BY 4.0**), credited in full as `Constellation figures: IAU and Sky & Telescope (Roger Sinnott,
Rick Fienberg; patterns by Alan MacRobert) — CC BY 4.0` with a link to `https://www.iau.org`, displayed
with **no IAU logo**, and shipped **reduced to line segments between identified stars** (the pattern is
what the licence covers; the geometry is data). Where boundaries or figures are drawn the derived
geometry SHALL carry the same IAU / Sky & Telescope CC BY 4.0 credit. Constellations SHALL
NOT be treated as point targets: they SHALL NOT be classified as rise/set/never-up/circumpolar like an
object, and they SHALL NOT appear in the target list's magnitude ordering or its magnitude filter. The
app SHALL use them to label and draw the constellation the aim point or the chosen target lies in.

#### Scenario: Eighty-eight constellation rows are present

- **WHEN** `constellations.json` is parsed
- **THEN** it contains 88 rows, each with a unique `abbrev` and a `raCenterDeg`/`decCenterDeg` pair

#### Scenario: A constellation is never ranked or filtered as an object

- **WHEN** the magnitude filter or the brightest-first ordering is applied
- **THEN** no constellation row is included in that comparison

#### Scenario: The figure polylines come from the credited asset

- **WHEN** a constellation figure is drawn
- **THEN** its polyline is read from `assets/sky/constellation_lines.json` and the IAU CC BY 4.0 credit
  is present in the in-app credits

### Requirement: The star table is FK6 plus a verified supplement, never FK6 alone

The app SHALL ship its star field as the authored asset `app/src/main/assets/catalog/stars.bin`, which
SHALL be the merged **FK6** catalogue and a **hand-authored supplement of the brightest stars FK6
omits**. FK6 is **CC0 1.0** and carries **4,151 rows**, V magnitude `−0.62 … 9.71`; it is a *fundamental
astrometric* catalogue, not a naked-eye list, and its brightest row is `alpha Car` at `−0.62`, so it
**lacks Sirius, Arcturus, Capella and Betelgeuse**. The supplement SHALL contain at least those four
stars, each verified against **two independent sources** by the same procedure the deep-sky rows use,
and the merged table SHALL be baked at build time with no network fetch.

#### Scenario: The supplement closes FK6's gaps

- **WHEN** the baked star table is read for Sirius, Arcturus, Capella and Betelgeuse
- **THEN** all four are present, each carrying its own two-source verification record

#### Scenario: The FK6 row count is the measured one

- **WHEN** FK6's row count is read
- **THEN** it is **4,151**, not the 4,150 the recon's earlier reading stated

### Requirement: The user can find a target by designation, name or alias, and filter the list

The `Targets` screen SHALL provide a search field and a filter bar. The filter bar SHALL carry the
magnitude filter and the type filter specified here, together with the favourites filter and the
visible-tonight filter specified by the two requirements that follow. The search field SHALL be a
**Material 3 `SearchBar`** (or a slot-owned search component that composes one) whose query state is a
named `searchQuery`; the capability SHALL NOT implement the search as a bare `TextField` holding an
unnamed local query, so the `find` capability resolves on that concrete control and its named query
state. Search SHALL match, at minimum, the
`messier` designation, the `ngc` and `ic` designations, `name`, `nameRu`, `aliases` and `commonNames`.
Matching SHALL be case-insensitive, SHALL fold accented characters to their unaccented form by Unicode
decomposition, and SHALL treat a leading `M` (with or without a space) followed by digits as a Messier
designation so that `m31`, `M 31` and `M31` all match the row whose `messier` is `"M31"`. The magnitude
filter SHALL accept a limit value and keep only rows whose primary magnitude is at or below it (within
one `magSystem`/`magKind`, per the first requirement); rows with no known magnitude SHALL be excluded
while a limit is active and SHALL be labelled `target_magnitude_unknown` when no limit is active. The
type filter SHALL offer the closed `type` set and keep only the chosen types. Search and every filter
SHALL combine. Results SHALL be ordered by magnitude ascending with magnitude-unknown rows **last**, then
by `id`. The query and the filter state SHALL be held in `SavedStateHandle`/`rememberSaveable` so a
rotation or a process recreation restores the same result list (`E30`). When nothing matches, the screen SHALL
show a typed empty state naming the query — `target_no_result` — and SHALL NOT show a blank list or a
fabricated row.

#### Scenario: A Messier number finds the object

- **WHEN** the user types `m31`
- **THEN** the result list contains the row whose `messier` is `"M31"`

#### Scenario: A proper name finds the object

- **WHEN** the user types the object's `name` or any entry of its `commonNames`
- **THEN** the object is returned

#### Scenario: Matching is case- and accent-insensitive

- **WHEN** the user types a query in a different case, or with the diacritics removed from a name that
  carries them
- **THEN** the same rows are returned as for the exact-case, exact-diacritic query

#### Scenario: The magnitude filter keeps only comparable rows

- **WHEN** the magnitude limit is set to 8.0
- **THEN** only rows whose primary magnitude is at or below 8.0 are shown, and rows with no known
  magnitude are excluded, and no V value is compared against a B value

#### Scenario: The type filter narrows the list

- **WHEN** the user selects `planetary_nebula` as the only type
- **THEN** the list contains only rows of that type

#### Scenario: No match is a stated empty state

- **WHEN** the user types a query that matches no row
- **THEN** the screen shows an empty state naming the query, not a blank list

#### Scenario: A search survives a process recreation

- **WHEN** the user searches, then the process is recreated and the screen returns
- **THEN** the query and the filter state are restored and the same result list is shown

### Requirement: The favourite control and the favourites filter are the list's surface for the stored favourite set

The stored favourite set, its persistence through `data/ScoutStore.kt`, its de-duplication by stable
string id and its import merge are owned by the **`scout-spots`** capability's requirement *"Favourite
targets are marked on the target list and carried with the scout store"*; that requirement is cited here
as the owner of the value and its rules SHALL NOT be restated. This capability owns the **surface** on
the screen of *"The user can find a target by designation, name or alias, and filter the list"*.

Every row of the `Targets` list and every computed solar-system record of *"Solar-system bodies are
computed for the instant, never authored"* SHALL carry one favourite control that toggles that target's
membership in the stored set and reflects the set's current state. The control SHALL be named for
TalkBack and SHALL be at least **48 dp** in both dimensions, and a favourited row SHALL be distinguished
by a **word or a shape in addition to colour alone**, so a user who cannot distinguish the palette's hues
reads the same state. The control SHALL read its state from the stored set and SHALL NOT hold the state
in session-only state, so a row's mark survives a process recreation with the set.

The search control of *"The user can find a target by designation, name or alias, and filter the list"* —
the **Material 3 `SearchBar`** whose query state is the named `searchQuery` — SHALL carry a **favourites
filter** that keeps only rows whose id is in the stored set. The favourites filter SHALL compose with the
search query, the magnitude filter, the type filter and the visible-tonight filter rather than replacing
any of them. When the favourites filter is on and the stored set is empty, the list SHALL show a typed
empty state `target_favourites_empty` naming that there are no favourites yet, and SHALL NOT show a blank
list or a fabricated row.

#### Scenario: Marking a target as a favourite

- **WHEN** the user activates the favourite control on a target's row
- **THEN** the target is a member of the stored favourite set, the row reflects that state by a word or a
  shape in addition to colour, and the state is read from the stored set rather than from session state

#### Scenario: Unmarking a favourite

- **WHEN** the user activates the favourite control on a row that is already a favourite
- **THEN** the target is removed from the stored set and the row's state returns to not-favourite, again
  by a word or a shape and not by colour alone

#### Scenario: The favourites filter narrows to the set and composes

- **WHEN** the favourites filter is on while the magnitude filter and the type filter are also set
- **THEN** the list contains exactly the rows that are both favourited and inside the magnitude and type
  filters, and no filter replaces another

#### Scenario: A computed solar-system body carries the same control

- **WHEN** a computed solar-system record is shown in the list
- **THEN** it carries the same favourite control as a catalogue row, with the same state and size rules

#### Scenario: An empty favourites set is a typed empty state

- **WHEN** the favourites filter is on and the stored set holds no id
- **THEN** the screen shows `target_favourites_empty` naming that there are no favourites yet, and not a
  blank list or a fabricated row

### Requirement: Each target's rise, transit and set are computed inside the dark window

For the chosen spot and night, each fixed target SHALL be classified by sampling its altitude at
**10-minute** steps across the session window and refining the result, using
`horizon(time, observer, raHours, decDeg, Refraction.Normal)`, with the observer recorded as latitude
**N+ / S−**, longitude **E+ / W−**, height in **metres**. The effective horizon at the target's azimuth
SHALL be the greater of the user's drawn horizon sample at that azimuth and the altitude mask (default
**10.0°**, user-adjustable), and "rise"/"set" SHALL mean crossing that effective horizon with refraction
applied — never a bare 0°. All event searches SHALL be segmented or stepped-and-refined so a window
longer than a few hours returns **every** crossing as an ordered list (a single long search oversteps a
short window — the measured 1-crossing-where-15-exist defect); no event search SHALL be a single call
over the whole window. Transit SHALL be the time of maximum altitude, refined to one minute. Every
crossing SHALL be expressed as a half-open interval `[start, end)`, and a zero-length interval SHALL be
dropped rather than rendered. The result SHALL be a typed `TargetWindow` in `domain/TargetWindow.kt`, and
the three cases that are **not** a rise-to-set interval SHALL each be a typed outcome with a sentence the
user reads, never an empty list:

- **Never rises at this latitude** (the target's maximum altitude is below the effective horizon):
  `TargetWindow.NeverRises`, rendered with `target_never_rises` (`"<name> never rises from this spot."`),
  the target excluded from "what is up tonight".
- **Circumpolar, never sets** (the target's minimum altitude is at or above the effective horizon):
  `TargetWindow.Circumpolar(minimumAltitudeDeg, atInstant)`, rendered with `target_circumpolar`
  (`"<name> is up all night; lowest <N>° at <HH:MM>."`).
- **Up for only part of the night** (the target is above the effective horizon at the dark window's
  start or end, so the visible window is the intersection with the dark window and only one boundary is
  a rise or a set): a clipped interval whose clipped boundary is named as the dark-window boundary and
  whose other boundary is the actual rise or set, rendered with `target_partial_window`.

#### Scenario: A target that rises and sets inside the dark window

- **WHEN** a target crosses its effective horizon upward after dark start and downward before dark end
- **THEN** it is an interval carrying its rise, its transit and its set, all inside the dark window

#### Scenario: A target that never rises is a stated sentence

- **WHEN** a target's maximum altitude across the session is below its effective horizon
- **THEN** the app shows "never rises from this spot", excludes it from "what is up tonight", and draws
  no flat altitude line at zero

#### Scenario: A circumpolar target reports its lowest altitude

- **WHEN** a target's minimum altitude across the session is at or above its effective horizon
- **THEN** the app shows that it is up all night together with its lowest altitude and the instant it
  occurs, and shows no rise or set

#### Scenario: A target up for only part of the night shows the clipping boundary

- **WHEN** a target is already above its effective horizon at the dark window's start and sets before
  the dark window ends
- **THEN** the app shows a window that begins at the dark-window start and ends at the target's set,
  naming the dark-window boundary as the reason the window is clipped

#### Scenario: A target that rises and sets twice in one window lists both

- **WHEN** the session window contains two upward and two downward crossings of a target's effective
  horizon
- **THEN** both intervals are returned, in order, and neither is dropped (`E24`)

#### Scenario: A target exactly at the horizon is "clears your horizon"

- **WHEN** a target's crossing coincides with the user's drawn horizon elevation at its azimuth
- **THEN** the app states that it clears the drawn horizon at that instant (naming the elevation), rather
  than reporting a geometric rise at 0°

#### Scenario: A grazing target is marginal, not a second-long interval

- **WHEN** a target's maximum altitude equals its effective horizon
- **THEN** the app states that it grazes the horizon, with its peak altitude and the duration above the
  threshold, and does not report a rise and a set separated by seconds

### Requirement: The chosen night's visibility is a filter on the list, never a re-ordering

Each target's computed `TargetWindow` — the rise-to-set interval, `TargetWindow.Circumpolar`, the
clipped partial window, or `TargetWindow.NeverRises`, from *"Each target's rise, transit and set are
computed inside the dark window"* — SHALL be exposed on the `Targets` screen as a **filter** and SHALL
NOT be used to re-order the list. A control on the screen of *"The user can find a target by designation,
name or alias, and filter the list"* SHALL narrow the list to the targets whose computed window for the
chosen night at the active spot is **non-empty**: an interval inside the dark window, a circumpolar
window, or a clipped partial window. A target whose computed outcome is `TargetWindow.NeverRises` SHALL
be **excluded** from the filter; a circumpolar target, whose window is non-empty, SHALL be **included**.
The filter SHALL compose with the search query, the magnitude filter, the type filter and the favourites
filter rather than replacing any of them.

This is a filter and SHALL NOT be a re-ordering: the result list's order SHALL remain exactly the one
*"The user can find a target by designation, name or alias, and filter the list"* defines — magnitude
ascending with magnitude-unknown rows **last**, then by `id`. No per-night ranking SHALL be introduced.

When no target in the current filter set is up for the chosen night, the list SHALL show a typed empty
state `target_none_visible_tonight` naming the night, and SHALL NOT show a blank list or a fabricated
row. When no night is chosen or no spot is active, the filter SHALL NOT compute a window for a default
location or a stale instant; it SHALL present the typed missing-spot or missing-night state rather than
filtering to a fabricated result.

#### Scenario: A night with some targets up

- **WHEN** the visible-tonight filter is on for a night on which several targets in the current filter
  set have non-empty windows and others never rise
- **THEN** the list contains exactly the targets that are up, in the catalogue's existing order
  (magnitude ascending, magnitude-unknown last, then by `id`), and the never-rising targets are excluded

#### Scenario: A night with none up

- **WHEN** every target in the current filter set has a `TargetWindow.NeverRises` outcome or an empty
  window for the chosen night
- **THEN** the screen shows `target_none_visible_tonight` naming the night, and not a blank list or a
  fabricated row

#### Scenario: The polar night

- **WHEN** the chosen night is a polar night at a high-latitude spot, so the dark window spans nearly the
  whole day
- **THEN** circumpolar targets are included because their window is non-empty, targets that never rise at
  that latitude are excluded, and `target_none_visible_tonight` is shown only when no target in the
  filter set is up

### Requirement: The lens fit statement is a measurement, with its numbers shown

For each target with a known apparent size, the app SHALL state whether it fits the user's field of view
as one of **fits**, **marginal** or **too large**, and SHALL show the numbers behind the statement: the
object's angular size, the frame's angular dimensions, and the focal length and sensor extent that
produced them. The field of view SHALL be computed as `FOV_axis = 2·atan(d_axis / (2·f))` with `d_axis`
the sensor extent in **millimetres** and `f` the focal length in **millimetres**, and focal length and
sensor dimensions SHALL NEVER be converted between unit systems. The object's angular size SHALL be
`sizeMajArcmin / 60` degrees, and the fit SHALL be classified by comparing the major axis against the
frame's **shorter** side: **fits** when the ratio is at most 0.8, **marginal** when the ratio is more
than 0.8 and at most 1.0, and **too large** when it is more than 1.0. The app SHALL NOT derive a
visibility or a "you will see it" claim from a magnitude, and SHALL NOT draw a star as a disc sized from
its magnitude. A target with a null apparent size SHALL produce no fit statement and SHALL state that
its size is unknown rather than inventing one.

#### Scenario: A target that fits states the fit and the numbers

- **WHEN** a target's major axis is at most 0.8 of the frame's short side
- **THEN** the app states that it fits and shows the object size, the frame dimensions and the focal
  length and sensor extent

#### Scenario: A target too large for the frame is stated as such

- **WHEN** a target's major axis exceeds the frame's short side
- **THEN** the app states that it is too large for the frame, with both numbers

#### Scenario: A marginal target is a distinct statement

- **WHEN** a target's major axis is more than 0.8 and at most 1.0 of the frame's short side
- **THEN** the app states that it is marginal, distinct from fits and from too large

#### Scenario: A point source gets no fabricated disc

- **WHEN** a target has no apparent size
- **THEN** the app draws no disc sized from its magnitude and states that its size is unknown

#### Scenario: Focal length is never unit-converted

- **WHEN** the app is set to imperial units and a fit statement is produced
- **THEN** the focal length and sensor extent remain in millimetres and the computed fit is unchanged

### Requirement: The target detail surface exists on both window sizes

Every catalogue target SHALL have a target detail surface that carries its catalogue facts and its computed
per-target facts for the active spot and night, and that surface SHALL exist on **both** a narrow and a
wide window. On a **narrow** window the target list and the target detail SHALL be two destinations: the
action on a target row SHALL open the detail, and back SHALL return to the list with the search query, the
filter state and the scroll position preserved. On a **wide** window — the size class at which the design
makes `Targets` a list-and-detail pair, read from the window size class and never assumed — the detail SHALL
be shown **beside** the list rather than replacing it: the action on a target row SHALL select that row into
the detail pane without pushing a second destination, the selected row SHALL be visibly selected, and back
SHALL clear the selection — returning to the list with no target shown — rather than leaving the `Targets`
destination. When no row is selected on a wide window the pane SHALL show `target_detail_select_prompt`
rather than a blank pane or an invented target.

The detail SHALL show, for the selected target at the active spot and night:

- the **catalogue facts**: `id`; the `messier`, `ngc` and `ic` designations; `name` and `nameRu`; `type`;
  the magnitude with its `magSystem` and `magKind`, or a stated unknown; the apparent size where known;
  `aliases` and `commonNames`; and the two `verification` sources a reader can re-open;
- the **rise, transit and set table** for the night — the interval with its rise, transit and set, or one
  of the three typed non-interval outcomes with its sentence — from the requirement that owns the window
  computation, never a second, independently computed window;
- the **lens-fit verdict** with its numbers (the object size, the frame dimensions, and the focal length
  and sensor extent), or the "size unknown" statement when `sizeMajArcmin` is null, from the requirement
  that owns the fit;
- **whether the target is visible tonight at the active spot** — its computed altitude and azimuth for the
  chosen instant, and its inclusion in or exclusion from "what is up tonight".

When **no spot is active** or **no night is chosen**, the detail SHALL show a typed empty state naming what
is missing — `target_detail_needs_spot` or `target_detail_needs_night` — and SHALL NOT compute or display a
window for a default location such as `(0, 0)`, for a stale instant, or for another spot's night. When the
catalogue asset is unreadable the detail SHALL show the same `target_list_unavailable` state the list shows
rather than a blank pane.

#### Scenario: A target row opens the detail on a narrow window

- **WHEN** the window is narrow and the user invokes the action on a target row
- **THEN** the target detail opens for that target, and back returns to the list with its search query,
  filter state and scroll position preserved

#### Scenario: A wide window shows the detail beside the list

- **WHEN** the window is wide and the user selects a target row
- **THEN** the detail is shown beside the still-visible list, the row is visibly selected, and back clears
  the selection rather than leaving the `Targets` destination

#### Scenario: The detail holds the catalogue facts and the computed facts together

- **WHEN** a target detail is shown for an active spot and night
- **THEN** it shows the catalogue facts, the rise/transit/set table, the lens-fit verdict and whether the
  target is visible tonight, using the same window and the same fit the list uses

#### Scenario: With no active spot the detail is a stated empty state

- **WHEN** the detail is opened while no spot is active or no night is chosen
- **THEN** it shows a typed empty state naming the missing spot or night, and shows no window computed for
  a default location

### Requirement: The Milky Way core is a first-class target

The galactic centre SHALL be present in the same browseable list as the deep-sky objects, as a **fixed**
entry with the stable id `milky-way-core` and `type` from the closed set, carrying an ICRS J2000
coordinate (the direction of galactic longitude 0°, latitude 0°) verified against two references like
every authored row, and SHALL be presented with the same view fields as a catalogue row: its computed
altitude and azimuth for the chosen instant, and its rise, transit and set inside the dark window under
the same visibility rules as any other fixed target. It SHALL be searchable by name and SHALL be subject
to the same three non-interval outcomes as any target. The scene SHALL orient the Milky Way band for the
chosen date and place from this same geometry.

#### Scenario: The core appears beside the deep-sky objects

- **WHEN** the target list is shown for a spot and night
- **THEN** `milky-way-core` is present in the list with its altitude and azimuth

#### Scenario: The core has a visibility window like any fixed target

- **WHEN** the Milky Way core's window is computed for the chosen night
- **THEN** it reports its rise, transit and set inside the dark window, or one of the three typed
  non-interval outcomes, using the same rules as any other fixed target

#### Scenario: The core is findable by search

- **WHEN** the user searches for the Milky Way core by its name
- **THEN** the entry is returned

### Requirement: The catalogue's edge states are typed, not blank

The capability SHALL handle its edge states as typed outcomes with a sentence, never as a crash, a blank
panel or a fabricated value.

- **The asset is missing or unreadable** (`E97`): the app SHALL start, the `Targets` screen SHALL show
  `target_list_unavailable`, and the brief SHALL omit targets and say so; a build-time test SHALL assert
  the asset is packaged and parses.
- **A saved reference points at a target no longer in the list** (`E31`, `E99`): targets SHALL be stored
  by stable string id, never by list index, and a missing id SHALL be dropped with a single note
  `target_reference_missing`, leaving the saved spot usable.
- **A target sits exactly at the horizon** (`E25`, `E26`): the app SHALL state that it clears the drawn
  horizon at that instant, or that it grazes it, and SHALL drop a zero-length interval rather than
  render a hairline.
- **A device with a very large system font** (`E87`): the target list and every text surface in this
  capability SHALL reflow with no fixed-height text container, so the list and the window statements
  remain readable at **200 %** font scale; labels inside the scene may abbreviate but SHALL stay legible.

#### Scenario: A missing asset degrades to a typed empty state

- **WHEN** `assets/catalog/objects.json` cannot be read
- **THEN** the app opens, the `Targets` screen shows "target list unavailable", and the brief says the
  target list is unavailable rather than showing none

#### Scenario: A removed target leaves the spot intact

- **WHEN** a saved spot references a target id that is not in the current catalogue
- **THEN** the reference is dropped with one note and the spot opens normally

#### Scenario: A target at the horizon is described, not rendered as a zero interval

- **WHEN** a target's computed crossing coincides with the horizon within the sampling resolution
- **THEN** the app states the clearing instant (or that it grazes) and renders no zero-length interval

#### Scenario: The list reflows at a very large font

- **WHEN** the system font scale is 200 %
- **THEN** the target list and its window statements are readable with no clipped or collided text

### Requirement: Every user-facing string is a localised resource and every control is named and 48 dp

Every string this capability renders SHALL exist as a named resource in **both**
`app/src/main/res/values/strings.xml` and `app/src/main/res/values-ru/strings.xml`, and SHALL NOT appear
as a literal in code. The resource names SHALL include at least `target_search_hint`,
`target_search_clear`, `target_filter_magnitude`, `target_filter_type`, `target_no_result`,
`target_list_unavailable`, `target_never_rises`, `target_circumpolar`, `target_partial_window`,
`target_at_horizon`, `target_graze`, `target_magnitude_unknown`, `target_fit_fits`,
`target_fit_marginal`, `target_fit_too_large`, `target_fit_point_source`, `target_reference_missing`,
`target_detail_needs_spot`, `target_detail_needs_night`, `target_detail_select_prompt`,
`target_favourite_toggle`, `target_filter_favourites`, `target_favourites_empty`,
`target_filter_visible_tonight`, `target_none_visible_tonight`,
`milky_way_core_name` and `catalogue_count_label`. Every interactive control in this capability — the
search field, the magnitude control, each type toggle, each row, each row's action, each favourite
control, the favourites filter and the visible-tonight filter — SHALL have an
accessible name and a TalkBack role (search field, adjustable, checkbox or button as appropriate), and
SHALL be at least **48 dp** in both dimensions. The search results list SHALL announce its size and the
active filters as a semantic summary, because the altitude visualisation itself is a graphic.

#### Scenario: Both locales carry every string

- **WHEN** the two string files are read
- **THEN** every resource name in this capability's list exists in both `values/strings.xml` and
  `values-ru/strings.xml`

#### Scenario: No capability string is a literal in code

- **WHEN** the capability's Kotlin sources are scanned
- **THEN** no user-facing sentence from the list above appears as a string literal

#### Scenario: Every control is named and meets the target floor

- **WHEN** the `Targets` screen is measured
- **THEN** every interactive control is at least 48 dp in both dimensions and reports a TalkBack role
  and name

**Honest boundary, recorded rather than glossed.** The catalogue is data and arithmetic, so these
requirements can be checked: the schema is complete on every row; the selection rule's count is
reproducible from the recorded limit; a filter never crosses a magnitude system; the three non-interval
visibility cases produce a typed outcome with a sentence; the fit statement's classification follows
from the two numbers it shows; the search matches a designation, a name and an alias across case and
diacritics; and every string exists in both locales. What none of these can prove is that the catalogue
is **correct** — that a coordinate or a magnitude is the object's true value — because a table of
authored facts is verified by reading two references, and no program here can re-read them. Nor can the
fit statement prove the user's lens is the lens they own; it proves only that the arithmetic from the
stated focal length and sensor extent is performed and shown. Both limits are the same shape: the app
states its derivation and its numbers, and the truth of those numbers stays a reading.

## MODIFIED Requirements

None. This change adds a new capability and modifies no existing spec.
