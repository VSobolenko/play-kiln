# Scout spots

## Purpose

The scout is the user's own setup: a saved observing spot, the horizon silhouette they will actually
stand behind, and the lens they will shoot with. It is the base every session brief is drawn against,
and it is the one artifact in Lumintra that cannot be re-derived from coordinates — a ridge line the
user drew by hand is not in any elevation dataset, and an equipment profile is a fact about the user's
bag. This capability gives the user a spot they can name, place, anchor a time zone to and give a
horizon to; a horizon editor inside `SpotEdit` that turns a free-hand gesture into a fixed numeric
profile; a store that survives process death, a reinstall and the move to another phone; and an export
and import path through which the whole setup leaves the device as a file the user owns. It is the
`before` stage of the job, and it exists so that a plan made in the field is not trapped inside one
install.

## ADDED Requirements

### Requirement: A saved spot records the observer's place, its sign conventions and its own zone

The app SHALL store a spot as `Scout` with the fields `id` (a stable string key that never changes for
the life of the record), `name`, `latDeg`, `lonDeg`, `elevationM`, `tzId`, `note` (optional) and an
optional `equipmentProfileId`. The units and signs are fixed and SHALL be stated on the editor: latitude
in degrees over `[−90.0, +90.0]` with **N positive and S negative**, longitude in degrees over
`[−180.0, +180.0]` with **E positive and W negative**, and `elevationM` in **metres above sea level**
(this is the observer convention every computation uses; a spot stored west-positive would silently
mirror the sky). `tzId` SHALL be an **IANA zone identifier** such as `Europe/Oslo`, never a numeric UTC
offset, so DST is resolved by the platform's time-zone database and moves with Android's updates;
`java.time.ZoneId` is available at the slot's `minSdk` of 26 and needs no desugaring. All computation
SHALL be in **UTC**; every instant SHALL be rendered in the **spot's** zone by default, and where the
device's zone differs the app SHALL state both in one line. The zone SHALL travel **inside the spot
record**, so a spot re-opened under a different device zone still shows its own times (E60, E63, E105).

Coordinate entry SHALL accept decimal degrees and degrees–minutes–seconds and SHALL produce one
canonical representation. Because a south/west sign is easy to drop from a typed `-`, the editor SHALL
require an explicit **hemisphere control** (N/S and E/W) rather than relying on a sign the user can
forget, and a comma-separated paste SHALL be validated against the stated ranges rather than silently
swapping latitude for longitude (E58). An address or place name SHALL be resolved to coordinates through
the platform's forward geocoder and the reverse lookup through the platform's reverse geocoder; both
SHALL be treated as **best-effort**: when the device has no network or no geocoder backend, the lookup
SHALL render a typed "address lookup unavailable — enter coordinates" state, and lookup failure SHALL
never block saving a spot that has a valid coordinate pair.

#### Scenario: Sign conventions are stored and shown

- **WHEN** the user enters a southern, eastern spot (for example latitude `−33.9`, longitude `+18.4`)
- **THEN** the stored `latDeg` is negative and `lonDeg` is positive, and the editor shows the N/S and
  E/W hemispheres explicitly rather than a bare sign

#### Scenario: A comma-separated paste is validated, not swapped

- **WHEN** the user pastes two comma-separated numbers
- **THEN** each is validated against the latitude and longitude ranges with a hemisphere control, and
  a value outside its range is refused rather than stored, and latitude and longitude are not silently
  interchanged

#### Scenario: The spot carries its own zone

- **WHEN** a spot created under one device zone is opened while the device zone is different
- **THEN** the spot's times are shown in the spot's `tzId` and one line names both the spot's zone and
  the device's differing zone (E60, E105)

#### Scenario: Address lookup is best-effort and never blocks a save

- **WHEN** the device has no network while the user is naming a spot
- **THEN** the lookup shows the typed "address lookup unavailable" state, the coordinate fields remain
  usable, and a spot with a valid coordinate pair saves normally

### Requirement: A pole is a legal spot, computed altitude-only and never with a fabricated azimuth

Latitude SHALL be validated to the **closed range `[−90.0, +90.0]`**, so a latitude of exactly `+90.0`
or exactly `−90.0` is a **legal** coordinate rather than an out-of-range value. A latitude outside that
closed range SHALL be quarantined by exactly the behaviour this file already specifies for an
out-of-range coordinate — the coordinate-validation path of *"The scout's edge states are typed, never
blank"* and its *"A saved spot with no valid coordinates is quarantined"* scenario, together with the
per-record quarantine and its durability in *"The scout store persists, writes atomically, and its
codec never throws"* — and that behaviour SHALL NOT be restated here (E08; extends E59, E98). A
**name-only spot SHALL NEVER be created**: a spot that carries a name but no coordinate pair has no
valid `latDeg`/`lonDeg`, its save SHALL be refused, and the app SHALL NOT invent a coordinate or a
`(0, 0)` position for it (E59).

At **exactly ±90°** — the geographic pole — the azimuth of every body is **undefined**, so for such a
spot the app SHALL compute **altitude only**. It SHALL NOT render an azimuth value, an azimuth scale,
or a compass-like label for that spot. The surface that would otherwise show an azimuth SHALL instead
state, in **one line**, that the azimuth axis is meaningless at the pole — never a blank and never a
zero (E08). What the scene draws and what it labels is the **night-scene** capability's *"The scene is
one computed frame of the real sky"* requirement; that requirement is cited here rather than restated.

No computation SHALL produce a `NaN` azimuth or a divide-by-zero for such a spot: the app SHALL take
the **pole branch before any azimuth arithmetic**, so no azimuth is derived from a horizontal direction
at `|φ| = 90°`, and the **unit test** that drives a spot at exactly `+90°` and one at exactly `−90°`
through the coordinate and scene computation SHALL exist and SHALL assert a finite altitude with no
`NaN`.

#### Scenario: A spot at the north pole is accepted and computed altitude-only

- **WHEN** a spot with latitude exactly `+90.0` is saved, or such a record is imported
- **THEN** the value is accepted as inside the closed range, every body's result carries an altitude,
  and the surface that would show an azimuth states in one line that the azimuth axis is meaningless at
  the pole — never an azimuth, a blank or a zero (E08)

#### Scenario: A spot at the south pole is accepted the same way

- **WHEN** a spot with latitude exactly `−90.0` is opened
- **THEN** altitude-only results are shown with the same one-line note, and no azimuth value, azimuth
  scale or compass-like label is rendered for it (E08)

#### Scenario: A latitude outside the closed range is quarantined like any other invalid coordinate

- **WHEN** a stored record carries a latitude outside `[−90.0, +90.0]`
- **THEN** it is quarantined by *"The scout's edge states are typed, never blank"* — its *"A saved spot
  with no valid coordinates is quarantined"* scenario (E59) — and by the store requirement's per-record
  quarantine (E98), and no value is clamped, wrapped or coerced

#### Scenario: A name-only spot is never created

- **WHEN** the user attempts to save a spot that carries a name but no coordinate pair
- **THEN** the save is refused, no record is written, and no coordinate or `(0, 0)` position is invented
  for it (E59)

#### Scenario: The pole branch runs before any azimuth arithmetic

- **WHEN** the slot's unit test drives a spot at exactly `+90.0` and one at exactly `−90.0` through the
  coordinate and scene computation
- **THEN** the altitude is finite, no azimuth is produced, and no `NaN` or divide-by-zero occurs, because
  the pole branch is taken before any azimuth arithmetic (E08)

### Requirement: No location permission of any kind is declared or requested

Every way of creating or editing a spot SHALL work with **no location permission of any kind**: manual
coordinate entry, the map picker (place a pin or drag it), pasting coordinates, choosing an existing
saved spot, and a place-name search resolved through a geocoding service where one is used. The app
SHALL **declare no location permission** — `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION` and
`ACCESS_BACKGROUND_LOCATION` are all absent from the manifest — and SHALL request none: not at launch,
not on first run, not during onboarding, and not to satisfy a screen (E56, E107). The product's own use
case confirms this: the spot the user plans for is a site they are **not** standing at, so a device
fix buys nothing the manual paths do not already give. No screen SHALL be gated on a location grant,
and no runtime location prompt SHALL ever appear.

The job loses **nothing** without location: the dark window, the targets, the scene, the horizon, the
brief, the export and the import are all local (E107, E108). A saved spot SHALL be **independent** of
the device's position: moving the phone SHALL NOT move the spot and SHALL NOT change yesterday's brief
(E62).

Where a spot's coordinates come from a **place-name search** through a geocoding service rather than a
map pin, those coordinates are **approximate** and the app SHALL label the created spot `approximate`
and SHALL show that label to the user; the coordinates SHALL remain editable, because the horizon
belongs to a viewpoint a place name does not pin down. Coordinates entered by hand, pasted, or placed
as a map pin SHALL NOT be labelled approximate.

#### Scenario: Every spot-creation path works with no location permission

- **WHEN** the user creates or edits a spot with no location permission granted and none requested
- **THEN** manual coordinate entry, the app's own coordinate picker, pasted coordinates, choosing a saved spot and a
  place-name search all work, no feature is disabled, and no location prompt or blocking dialog is shown

### Requirement: The pin surface is the app's own drawing — no map SDK, no tile provider, no key

The map picker SHALL be drawn by the slot itself: a pan/zoom surface with a draggable pin, rendered
from the app's own code. It SHALL NOT introduce a map SDK, a tile provider, a map API key, or any
permission the app does not already declare, and SHALL NOT depend on a network call to place a pin.
The alternatives available on Android each fail one of this slot's own rules, and the failure is
**measured rather than assumed** (2026-10-08): **MapLibre Native Android** (`13.6.1`, BSD-2-Clause)
declares `ACCESS_COARSE_LOCATION` and `ACCESS_FINE_LOCATION` in its own library manifest, which
contradicts the requirement above, and pulls `androidx.fragment:fragment:1.8.9` beneath the factory's
declared floor; **osmdroid** (`6.1.20`, Apache-2.0) defaults to `tile.openstreetmap.org`, whose usage
policy forbids offline use and warns that commercial users may lose access; **Google Maps, Mapbox and
MapTiler** each require a client credential, which the factory's `client-secrets` rule forbids inside
`app/src/main/`. Where the picker shows a coastline backdrop it SHALL come from a **public-domain**
source committed with the slot (Natural Earth 1:110m), never from a live tile service, so the surface
carries no attribution obligation it cannot meet.

#### Scenario: A pin is placed offline, with no permission and no key

- **WHEN** the device has no network, no location permission has been granted, and the user opens the
  map picker
- **THEN** the picker renders, a pin can be placed, moved and dropped, and the coordinates are stored
  on the spot — with no tile request, no permission prompt, no credential and no error state

#### Scenario: No map dependency and no map credential enters the build

- **WHEN** the slot's `gradle/libs.versions.toml`, its `app/build.gradle.kts` and everything under
  `app/src/main/` are read
- **THEN** no map SDK or tile-library coordinate appears in either build file, and no map API key,
  token or client credential appears anywhere under `app/src/main/`

#### Scenario: A place-name result is labelled approximate

- **WHEN** a spot's coordinates come from a place-name search through a geocoding service
- **THEN** the created spot is labelled `approximate`, the label is shown to the user, and its
  coordinates remain editable

### Requirement: The place name comes from the platform geocoder, keyless and optional

Turning a chosen coordinate into a human place name SHALL use **`android.location.Geocoder`** and
nothing else. It SHALL NOT introduce a third-party geocoding API, a geocoding client credential, or a
permission the app does not already declare. `INTERNET` is the only permission this needs, and a keyed
geocoding service would breach the factory's `client-secrets` rule inside `app/src/main/`.

The reference measures the constraints that shape the call (MEASURED 2026-10-08,
`developer.android.com/reference/android/location/Geocoder`): the API *"may hit the network, and may
block for excessive amounts of time"* and *"it's strongly encouraged to use the asynchronous version"*;
a call returns *"null or empty list … if there is no backend service available"*; and *"Use the
`isPresent()` method to determine whether a Geocoder implementation exists on the current device. If no
implementation is present, any attempt to geocode will result in an error."*

Because the slot's `minSdk` is **26** while the asynchronous `GeocodeListener` overload is **API 33**,
the call SHALL branch: the asynchronous overload on API 33 and above, and the deprecated synchronous
overload executed **off the main thread** on API 26–32 — the fallback the reference itself permits
(*"this should be run on a background thread to avoid blocking other operations"*). `Geocoder.isPresent()`
(**API 9**) SHALL be checked before either branch.

The name is **enrichment, never a step**: it SHALL NOT gate placing, moving, saving or editing a spot,
and the app SHALL reach a complete, useful spot with the geocoder unavailable.

#### Scenario: A spot is completed with no geocoder and no network

- **WHEN** `Geocoder.isPresent()` returns false, or the device is offline, and the user places a pin
- **THEN** the pin is placed, moved and saved, the spot keeps its coordinates, the name is left to the
  user's own label, no error state is shown for the missing name, and no other feature is disabled

#### Scenario: No geocoder call blocks the main thread on any supported API level

- **WHEN** the app resolves a place name on API 26–32 and separately on API 33 or higher
- **THEN** no `Geocoder` call runs on the main thread on either branch, and the API 26–32 branch uses
  the background thread the deprecated overload requires

#### Scenario: No geocoding credential enters the build

- **WHEN** `gradle/libs.versions.toml`, `app/build.gradle.kts` and everything under `app/src/main/` are read
- **THEN** no third-party geocoding SDK coordinate appears in either build file, and no geocoding API key,
  token or client credential appears anywhere under `app/src/main/`

#### Scenario: The manifest declares no location permission at all

- **WHEN** the slot's own `AndroidManifest.xml` is read
- **THEN** none of `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION` or `ACCESS_BACKGROUND_LOCATION`
  appears in it — all three are absent

#### Scenario: A spot does not follow the phone

- **WHEN** a spot has been saved and the phone later moves
- **THEN** the spot's stored coordinates are unchanged and its brief is unchanged

### Requirement: The horizon line is drawn in `SpotEdit` and stored as a fixed 360-sample array

The horizon editor SHALL live **inside** `SpotEdit` (canonical file `ui/spots/SpotEditScreen.kt`) and
SHALL NOT be a separate navigation destination. The user SHALL draw the skyline they will stand behind
by dragging across the editor canvas, which maps azimuth `0…360` linearly across its full width and
elevation across a **declared vertical field** (default `0…30°`, adjustable). The drawn gesture SHALL be
stored as a `HorizonProfile` (`domain/HorizonProfile.kt`) that is a **quantised azimuth→elevation
array**, never a bitmap whose meaning depends on the phone's screen resolution, and the array SHALL be
the single source of truth of which the canvas is a view: the profile SHALL be exactly **360 samples,
one per whole degree of azimuth**, index `i` covering azimuth `i` degrees, with elevation stored in
**tenths of a degree** as a **signed integer** clamped to `[−10°, +90°]`; other azimuths SHALL be
interpolated **linearly** between samples, wrapping at `0/360` (E72, E79). At 360 samples of 2 bytes each
the profile is about 0.7 kB per spot (arithmetic on the stated widths, not a measurement). An in-progress
draft SHALL be held in saveable state so that a rotation preserves it, and a process death SHALL restore
the draft or discard it with a prompt — the app SHALL NEVER commit a half-drawn profile (E80).

**The sample count is settled at 360 (1.0° per sample), deliberately, over the 720 samples at 0.5° a
source report proposed** (`L-depth-retention-edges.md` §2.8, `E72`). Two reasons, both stated so they can
be audited. First, **the finer sampling is below the display resolution of a hand-drawn phone profile**:
the reference test phone draws the editor into a 1080 px-wide canvas, so 360 samples land at about 3 px
per sample and 720 samples at about 1.5 px per sample, while a hand-drawn silhouette has a stroke width
of several pixels — the 720-sample profile would therefore encode detail the gesture never produced
(INFERRED: `1080 px ÷ 360 = 3.0 px`, `1080 px ÷ 720 = 1.5 px`). Second, **the resolution is fixed so a
profile drawn on one device is exactly the profile read on another**: because the count is a constant of
the format and not a property of the canvas, a profile drawn on a phone re-renders **byte-identically** on
a tablet, and the ridge means the same degrees wherever it is opened (E73, E74).

The editor SHALL offer an **alternative to free-hand dragging** (see the accessibility requirement): at
minimum, selecting a sample or azimuth and setting its elevation numerically, so a user who cannot drag
can still author a profile. Panning or zooming the editor SHALL NOT change what the profile means — the
azimuth mapping is unaffected by the view transform and the currently visible azimuth range SHALL be
stated — and a pan SHALL NEVER shift the `0°` point (E81). The promise `draw-the-horizon-line` resolves
on this stored profile — the 360-sample `HorizonProfile` (`domain/HorizonProfile.kt`) authored by the
`SpotEdit` editor (`ui/spots/SpotEditScreen.kt`) and read by every later `SceneFrame` — and on no
separate overlay layer.

#### Scenario: The stored profile is exactly 360 samples

- **WHEN** a drawn horizon profile is written to the scout store
- **THEN** it holds exactly 360 signed integer samples, one per whole degree of azimuth, with elevation
  in tenths of a degree clamped to `[−10°, +90°]`

#### Scenario: The same profile reads identically on a phone and a tablet

- **WHEN** a profile drawn on the reference phone is opened on a tablet with a different screen
  resolution and aspect ratio
- **THEN** the same samples are loaded, the same ridge is drawn, and the elevation in degrees is
  identical on both devices (E73, E74)

#### Scenario: An extreme aspect ratio does not change the profile

- **WHEN** the editor is opened on a 21:9 phone and on a 4:3 tablet
- **THEN** the array is unchanged, the canvas is only a view of it, and the azimuth↔width mapping is
  linear on both (E73)

#### Scenario: A rotation or process death does not commit a half-drawing

- **WHEN** the user is mid-gesture and the device rotates, or the process is recreated
- **THEN** the draft is restored from saveable state or discarded with a prompt, and a partial profile
  is never written to the store (E80)

#### Scenario: Panning does not move the zero point

- **WHEN** the user pans or zooms the editor canvas
- **THEN** the azimuth mapping is unchanged, the visible azimuth range is stated, and the `0°` point
  does not move (E81)

### Requirement: The drawn horizon is one source of truth that occludes the sky

The foreground silhouette the Night scene draws SHALL be a **rendering of the same `HorizonProfile`
array** the calculations use, and SHALL NOT be a separate overlay layer that can disagree with it: the
picture and the numbers SHALL come from one array (E79). Wherever the app asks whether something is up,
it SHALL test against the **drawn horizon at that body's azimuth**, not against a constant `0°`:

- A target's "rise" SHALL be its crossing of the **local drawn horizon at its azimuth** — where the
  profile blocks `12°`, the event is "clears your horizon at 23:41" and the brief says so, not a
  geometric `0°` rise (E25).
- A satellite pass SHALL use the drawn profile as its elevation mask, so a pass behind the user's ridge
  is reported as "rises behind your horizon" rather than as visible (E42).
- A moonrise SHALL be masked by the profile: the app SHALL state that the Moon rises but stays behind
  the drawn horizon, and the moonless window SHALL be computed from the Moon's **apparent, masked**
  visibility rather than from an unobstructed horizon (E78).

The **Earth-shadow wedge** and the **alpenglow band** drawn on the scene are **computed from the Sun's
position and are not derived from the horizon profile**: the wedge is the region of sky the Earth's
shadow occupies relative to the antisolar direction, and the alpenglow band follows the Sun's altitude
through the twilight model. They SHALL be drawn from the Sun and SHALL NOT be clipped, widened or
suppressed by the horizon samples; the horizon's only job against them is to occlude the sky **below**
the drawn line, exactly as it occludes stars, targets, satellites and the Moon.

#### Scenario: A target behind a drawn ridge is stated, not reported as risen

- **WHEN** the user's profile blocks 12° at a target's azimuth and the target's geometric altitude
  crosses 0° before it crosses the ridge
- **THEN** the event is reported as clearing the drawn horizon at that later instant, and no geometric
  0° rise is claimed (E25)

#### Scenario: A pass behind the horizon is not visible

- **WHEN** a satellite pass occurs at an elevation the drawn profile blocks
- **THEN** the pass is reported as rising behind the horizon and is not offered as a visible pass (E42)

#### Scenario: A masked moonrise is stated and the moonless window follows it

- **WHEN** the Moon rises but stays behind the drawn horizon
- **THEN** the app states the Moon rises but stays behind the horizon, and the moonless window is
  computed from the masked visibility (E78)

#### Scenario: The silhouette matches the array

- **WHEN** the scene's foreground silhouette and the horizon array are compared for one spot
- **THEN** the silhouette is a rendering of that same array and the two cannot disagree (E79)

### Requirement: Degenerate horizons are legal, stored, drawn and stated

The app SHALL accept every one of the following horizon states as legal, and SHALL define what is
stored, what is drawn and what the brief says for each. A flat profile at a non-zero elevation, an
all-zero profile, a profile that blocks the whole sky, and a single-sample edit are all valid horizons
and SHALL NOT be rejected, coerced or treated as "no profile" (E75). **"No profile at all" is a
distinct state from a flat profile**: `hasProfile = false` SHALL be represented separately, every surface
that depends on the horizon SHALL say so once ("No horizon profile for this spot — using a flat
horizon."), and the two SHALL NEVER be collapsed into one state (E76).

| State | What is stored | What is drawn | What the brief says |
|---|---|---|---|
| **Flat profile at the chosen value** | 360 identical samples at that elevation | a flat foreground at that elevation | events use that constant elevation; no "no profile" note is shown (E75) |
| **All-zero profile** | 360 samples, all `0` tenths | a flat foreground at `0°` | events use `0°`; the profile is present, not absent |
| **Blocks the whole sky** (all samples at the top) | 360 samples at `+90°` | a foreground that fills the sky | every target and pass is reported as "behind your horizon", the reason is stated once, and a **single non-blocking confirmation** is offered because an all-90° profile is usually a mis-draw; the profile is still stored (E12, E77) |
| **Single-sample edit** | 359 default samples plus one changed sample | a spike at that one azimuth | interpolation between the edited sample and its neighbours produces a narrow feature that the maths and the drawing both use |
| **Extreme aspect ratio** | the same 360 samples | the same ridge at the scene's declared vertical field | the degrees are unchanged; the canvas is a view, not the truth (E73) |

#### Scenario: A flat profile is a horizon, not an absence

- **WHEN** the user draws a flat line at `+5°`
- **THEN** the profile is stored with 360 samples at `+5°`, the calculations use `+5°`, and the app does
  not show the "no horizon profile" note (E75)

#### Scenario: No profile and a flat profile are distinct states

- **WHEN** a spot has never had a horizon drawn, and another spot has an all-zero flat profile
- **THEN** the first reports `hasProfile = false` and says it is using a flat horizon once, and the
  second reports a present profile at `0°`; neither is presented as the other (E76)

#### Scenario: A sky-blocking profile is legal and stated

- **WHEN** every sample of the profile is at `+90°`
- **THEN** every target and pass is reported as behind the horizon, the reason is stated once, one
  non-blocking confirmation is offered, and the profile is stored (E12, E77)

#### Scenario: A single-sample edit is interpolated, not rejected

- **WHEN** the user edits exactly one sample and leaves the rest at their default
- **THEN** the profile stores that sample and interpolation to its neighbours produces a narrow feature
  used by both the maths and the drawing

### Requirement: The scout store persists, writes atomically, and its codec never throws

The app SHALL persist spots, their horizon profiles, the equipment profiles, the **active spot
selection** and the **favourite target ids** through `data/ScoutStore.kt` in a **versioned,
human-readable** on-device format. The store's codec SHALL be
total: it SHALL **never throw** on any payload; it SHALL **refuse an unknown `schemaVersion`** with a
typed state rather than best-effort parsing it (E101); and a corrupt or truncated payload SHALL read as
**"nothing saved"**, never as a crash — the app opens normally and reports that there is nothing to
resume (E98). Writes SHALL be **atomic** (write a temporary file, then rename), so a crash mid-write
leaves the previous good store intact and never a truncated one (E100). The store SHALL be loaded **per
record**: one unreadable spot SHALL be quarantined, the remaining records SHALL load, and the quarantine
SHALL be reported once (E98), and a quarantined record SHALL NOT be silently discarded on the next
save — it is kept aside or the user is told it will be discarded (E103).

A saved spot's selection — and the in-progress edit state — SHALL survive **process death**: the chosen
spot, the date, the instant and the lens SHALL live in `SavedStateHandle` / `rememberSaveable`, so the
app returns to the same frame rather than to first launch (E83). That is the **same-frame** restoration;
the **cold-start** default is a separate value persisted on disk — the store's `activeSpotId` — so a
fully terminated app reopens at the same spot rather than at first launch, and its resolution rules are
*"The `Spots` screen lists the saved spots and owns the active one"*. The durability proof token for this
requirement is the restore path (`SavedStateHandle` / `rememberSaveable`); the reinstall half is the
backup-rules requirement that follows.

#### Scenario: The process is recreated and the spot is restored

- **WHEN** the app is killed by the platform and reopened
- **THEN** the chosen spot, the date, the instant and the lens are restored and the app returns to the
  same frame (E83)

#### Scenario: A corrupt payload costs the record, not the app

- **WHEN** the stored scout payload is truncated or otherwise unreadable
- **THEN** the app opens normally, the corrupt record is quarantined, the other records load, and the
  quarantine is reported once (E98)

#### Scenario: An unknown schema version is refused

- **WHEN** the store on disk declares a `schemaVersion` newer than the app understands
- **THEN** the codec refuses it with a typed state and does not attempt best-effort parsing (E101)

#### Scenario: A crash mid-write leaves the previous store intact

- **WHEN** the app is interrupted part-way through a store write
- **THEN** the temporary file is discarded and the previous good store is the one read next time (E100)

### Requirement: The scout survives a reinstall through the backup rules

The app SHALL declare **both** Android backup-rule resources — `dataExtractionRules` (API 31+) and
`fullBackupContent` (API ≤ 30) — so the scout store, its horizon profiles and its equipment profiles are
restored after a reinstall (E102, E106). The rules SHALL **separate cloud backup from device transfer**
and SHALL include the plan store while **deliberately excluding regenerable caches** — satellite element
sets, cloud forecasts and any catalogue cache — so a cache is never restored and presented as fresh
(E102). The rules SHALL state that there are **no secrets** to exclude, so the exclusion list is
explicit rather than by omission. Where Auto Backup does not restore (backup disabled, or a device
without an account) the app SHALL NOT claim a restore happened: it SHALL show a truthful state, and the
user's exported file (the next requirement) is the path. The `durability` promise
`pin-the-session-scout` resolves on these two resources.

#### Scenario: A reinstall restores the saved setup

- **WHEN** the app is reinstalled on a device whose backup includes it
- **THEN** the saved spots, their horizon profiles and their equipment profiles are present again

#### Scenario: Regenerable caches are excluded from backup

- **WHEN** the backup rules are read
- **THEN** the plan store is included, and satellite element sets and cloud forecasts are excluded

#### Scenario: The app never claims a restore it did not get

- **WHEN** backup is disabled and the app is reinstalled with an empty store
- **THEN** the app shows an empty state and does not claim the setup was restored (E106)

### Requirement: The user's spots, horizons and equipment export to a file they own

The app SHALL export **every** saved spot, its horizon profile and the equipment profiles to a **single
versioned, human-readable** JSON document named `lumintra-scout-<yyyyMMdd>.json`, written through the
system file picker (`ACTION_CREATE_DOCUMENT`), or shared as an attachment through a `FileProvider`
`content://` URI; neither path SHALL require a runtime or storage permission. The document SHALL be
UTF-8 JSON, human-readable, and SHALL carry the app's schema version first, so a future migration is
detectable. The document SHALL contain exactly these fields and no others; it SHALL NOT carry session
data, caches, element sets or forecasts, which are regenerable and would bloat and date the file. The
**active spot selection** is a device-local default that is neither a `Scout` field nor a top-level key,
so it is not one of these fields and this document does not write it; the importing device **derives** it
by the rule in *"The `Spots` screen lists the saved spots and owns the active one"*, rather than reading
it from the file.

```jsonc
{
  "schemaVersion": 1,               // integer, required, the first key
  "app": "lumintra",                // string, the writer
  "exportedAtUtc": "2026-10-08T21:14:03Z",   // string, ISO-8601 UTC
  "spots": [
    {
      "id": "…",                    // string, stable key
      "name": "…",                  // string
      "latDeg": 67.93,              // number, [-90, +90], N+/S-
      "lonDeg": 13.09,              // number, [-180, +180], E+/W-
      "elevationM": 12.0,           // number, metres
      "tzId": "Europe/Oslo",        // string, IANA zone id
      "note": "…",                  // string or null
      "equipmentProfileId": "…",    // string or null
      "horizonProfile": {           // object or null (null == hasProfile false)
        "sampleCount": 360,         // integer, exactly 360
        "azimuthStepDeg": 1.0,      // number, exactly 1.0
        "elevationUnitDeg": 0.1,    // number, exactly 0.1
        "samples": [0, 0, 0, "…"]   // array of exactly 360 signed integers, tenths of a degree, [-100, +900]
      }
    }
  ],
  "equipmentProfiles": [
    {
      "id": "…",                    // string, stable key
      "name": "…",                  // string
      "focalLengthMm": 50.0,        // number, millimetres
      "sensorWidthMm": 36.0,        // number, millimetres
      "sensorHeightMm": 24.0        // number, millimetres
    }
  ],
  "favouriteTargetIds": ["m31", "ngc7000"]   // array of stable string ids, may be empty
}
```

Targets SHALL be referenced by **stable string id, never by list index**, so an object removed from the
catalogue can never silently point at a different object (E31). The derived field of view SHALL NOT be
trusted from the file: it SHALL be recomputed on import from focal length and sensor size (see the
equipment requirement). The `data_out` promise `carry-your-scout` resolves on `ACTION_CREATE_DOCUMENT`
and `FileProvider`.

#### Scenario: An export writes one readable file

- **WHEN** the user exports their setup and picks a location through the system file picker
- **THEN** a single UTF-8 JSON file named `lumintra-scout-<yyyyMMdd>.json` is written to that location
  with `schemaVersion` first, and it is human-readable

#### Scenario: The exported document carries every required field

- **WHEN** the exported document is read
- **THEN** it holds `schemaVersion`, `app`, `exportedAtUtc`, `spots` (each with `id`, `name`, `latDeg`,
  `lonDeg`, `elevationM`, `tzId`, `note`, `equipmentProfileId`, `horizonProfile`), `equipmentProfiles`
  (each with `id`, `name`, `focalLengthMm`, `sensorWidthMm`, `sensorHeightMm`) and `favouriteTargetIds`,
  and no session data, caches, element sets or forecasts

#### Scenario: The handoff needs no permission

- **WHEN** the slot's own `AndroidManifest.xml` is read
- **THEN** it declares no storage permission for the export or the share path

#### Scenario: A target is referenced by stable id, never by index

- **WHEN** the exported document's target references are read
- **THEN** each is a stable string id such as `m31`, not a list index (E31)

### Requirement: Import merges and refuses rather than corrupting

The app SHALL import through the system file picker (`ACTION_OPEN_DOCUMENT`) and SHALL **merge** the
imported records into the user's store rather than replacing them: it SHALL **skip** a record whose
`name` and `latDeg`/`lonDeg` match an existing record exactly (so re-importing the same file neither
duplicates nor overwrites), and SHALL show the count of records added and skipped before committing.
The import SHALL be **all-or-nothing**: a document that fails validation SHALL import **nothing** and
SHALL report the failing field and record, because a partial import is how half-corrupt state is
created. The import SHALL **refuse an unknown `schemaVersion`** with a typed message rather than
half-importing a future file (E101). Hard caps SHALL be validated **before any allocation** — file size,
spot count, horizon sample count and string lengths — so a crafted or truncated document produces a
**typed error and never an out-of-memory kill** (E104).

A malformed, truncated or crafted document SHALL be refused with a typed state — the sentence
`NotAScoutFile` = "That file is not a Lumintra scout file." — and SHALL NOT crash the app. Within a
validated document, an individual record that fails validation SHALL be refused by name rather than
coerced:

- **A spot whose coordinates are outside the valid range** (latitude outside `[−90.0, +90.0]`, or
  longitude outside `[−180.0, +180.0]`) SHALL cause the whole document to be refused under the
  all-or-nothing rule, naming the record and the field; the app SHALL NOT clamp, wrap or store it
  (E58).
- **A horizon profile whose sample array is not exactly 360 entries** SHALL be refused, naming the
  record and the observed length; it SHALL NOT be padded, truncated or interpolated to fit (E104).
- **A horizon sample outside `[−100, +900]` tenths of a degree** (outside `[−10°, +90°]`) SHALL be
  refused with the same naming discipline.

A cancelled file picker SHALL be a **no-op**: the store is unchanged, no error is shown, and no partial
state exists.

#### Scenario: Import merges and does not duplicate

- **WHEN** the user imports a file whose spot already exists with the same name and coordinates
- **THEN** the existing spot is skipped, the count of added and skipped records is shown, and no record
  is duplicated or overwritten

#### Scenario: An unknown version is refused

- **WHEN** the imported document declares a `schemaVersion` the app does not understand
- **THEN** the import is refused with a typed message and nothing is imported (E101)

#### Scenario: A crafted or truncated document is refused without crashing

- **WHEN** the imported document is malformed, or declares a horizon array of 10,000,000 samples
- **THEN** the caps are checked before allocation, the import is refused with `NotAScoutFile`, and the
  app does not allocate from the declared length and is not killed (E104)

#### Scenario: An out-of-range coordinate refuses the whole document

- **WHEN** a spot in the document carries a latitude outside `[−90.0, +90.0]`
- **THEN** the whole document is refused, the record and field are named, and no value is clamped or
  stored (E58)

#### Scenario: A wrong-length horizon profile is refused, not padded

- **WHEN** a spot's `horizonProfile.samples` array holds 359 or 361 entries
- **THEN** the document is refused, the record and the observed length are named, and the array is
  neither padded nor truncated (E104)

#### Scenario: A cancelled picker changes nothing

- **WHEN** the user cancels the system file picker during import
- **THEN** the store is unchanged, no error is shown, and no partial state is created

#### Scenario: A partial import never happens

- **WHEN** a document with one valid spot and one invalid spot is imported
- **THEN** neither spot is imported, and the failing record and field are reported

### Requirement: Equipment profiles carry focal length, sensor size and the derived field of view

The app SHALL store an `EquipmentProfile` as `id`, `name`, `focalLengthMm`, `sensorWidthMm` and
`sensorHeightMm`, all in **millimetres**, and SHALL derive the field of view for each profile from those
numbers rather than storing a hand-typed angle: horizontally `FOV_h = 2·atan(w / (2f))`, vertically
`FOV_v = 2·atan(h / (2f))` and diagonally `FOV_d = 2·atan(√(w² + h²) / (2f))`, where `w` and `h` are the
sensor extents and `f` the focal length. A profile SHALL be reusable and SHALL be selected by a spot
through `equipmentProfileId`, and the scene and the brief SHALL read the same derived field of view for
the chosen profile. Focal length and sensor dimensions SHALL **never be unit-converted** — they are
millimetres everywhere, and the framing arithmetic SHALL NOT convert them under an imperial display
setting (E71). The app MAY offer preset profiles, but each preset's field of view SHALL be **computed by
the same formula at load**, not typed in by hand, so a wrong preset is impossible.

#### Scenario: The field of view is derived, not typed

- **WHEN** an equipment profile with focal length `50.0 mm` and sensor `36.0 × 24.0 mm` is read
- **THEN** its `FOV_h`, `FOV_v` and `FOV_d` are the outputs of `2·atan(d / (2f))` for the respective
  extents and are not values stored by hand

#### Scenario: Focal length is never converted

- **WHEN** the display units are set to imperial
- **THEN** the focal length and sensor dimensions remain in millimetres and the framing arithmetic is
  unchanged (E71)

#### Scenario: The scene and the brief reuse one profile

- **WHEN** a scene and a brief are shown for the same spot and its `equipmentProfileId`
- **THEN** both use the same derived field of view from that one profile

### Requirement: The `Spots` screen lists the saved spots and owns the active one

The `Spots` screen (canonical file `ui/spots/SpotsScreen.kt`) SHALL list every saved spot as one row,
and that list SHALL read the **same store the widget and the reminder read** — one source of truth,
never a second copy. Each row SHALL show, at minimum:

- the spot's **name**;
- its **coordinates rendered in the user's chosen coordinate format** (decimal degrees or
  degrees–minutes–seconds, the format the coordinate-entry requirement defines);
- its **time zone** (`tzId`);
- **whether a horizon profile exists** for it — the present profile versus the `hasProfile = false`
  state the horizon requirement defines — carried by a word or a shape and never by colour alone;
- **whether the last brief assembled for this spot was for tonight**: the app SHALL keep, per spot,
  the local date of the most recent brief assembled for it (persisted with the spot and surviving
  process death), and the row SHALL render `Tonight` when that date equals the spot's current local
  date and an explicit "not tonight" or "no brief yet" state otherwise, so an absent brief is never a
  blank.

The list SHALL be ordered **alphabetically by `name`**, case-insensitively, and where two names compare
equal the **`id`** field SHALL decide the order (ascending), so the ordering is deterministic and never
depends on the order records were written.

Each row SHALL offer these actions, each with a name for TalkBack:

- **Open this spot in `Night`.** Tapping the row body opens the spot in `Night` (the hero scene).
- **Edit in `SpotEdit`.** Opens the spot in `SpotEdit`, including its horizon editor.
- **Duplicate.** Creates a new spot copied from this one with a **new stable `id`** and a name the user
  is asked to confirm, so two records never share an `id`.
- **Rename.** Changes the spot's `name` in place, leaving its `id` unchanged.
- **Delete.** Removes the spot **with an undo** the user can tap to restore it, and SHALL NOT raise a
  modal confirmation dialog.

Exactly one saved spot SHALL be **active** — the one `Night`, `Brief`, the widget and the reminder open
by default. A leading control on each row ("Set active") SHALL change which spot is active, and the
active row SHALL carry a named state (a word or a shape, never colour alone). Opening a spot with the
row body SHALL also make it active. Deleting the **active** spot SHALL select another saved spot as
active rather than leaving the app with none. Because `Set active` and opening a row both update it, the
active spot is the **most recently used** spot, and no separate recency order is stored.

The active selection SHALL be persisted in the scout store (`data/ScoutStore.kt`) as one **store-level
`activeSpotId`** holding the spot's stable `id` — never a row position — so it survives a **cold start**
(an app killed and relaunched, a device reboot, an App Backup restore) and not only process death. It
SHALL NOT be a field of a `Scout` record and SHALL NOT be a field of the export document *"The user's
spots, horizons and equipment export to a file they own"*; the importing device derives it. The app
SHALL resolve the active spot, whenever the store or the list changes, by exactly these rules:

- **No saved spot** (first run, or every spot deleted) — there is **no active spot**, the screen shows
  its scoped empty state, and no sky, brief, widget or reminder is computed for a fabricated position
  (E107, E110).
- **`activeSpotId` names a saved, non-quarantined spot** — that spot is active.
- **The first spot saved into an empty store** — it becomes active, so a spot the user has just created
  is never left inactive.
- **No `activeSpotId` has ever been set, or it names no spot that exists, or it names a quarantined
  record** — a *dangling* id, whether the record was removed outside the app or the store was edited by
  hand — the active spot SHALL be the **first saved spot in the list order this requirement defines**
  (alphabetical by `name`, then ascending `id`) that is not quarantined, and none when the store holds
  none. A quarantined record SHALL NEVER be active, a dangling id SHALL NEVER leave a valid spot
  unselected, and neither SHALL produce a fabricated or `(0, 0)` spot.
- **After a delete of the active spot** — the active spot SHALL be the first non-quarantined spot
  remaining in that same list order, and none when the delete was the last spot; undoing that delete
  SHALL restore the deleted spot as the active one.
- **After an import** — an active spot that still exists SHALL NOT change, and the imported file's own
  (absent) active selection SHALL NOT override the device's; where none was active, the first
  non-quarantined spot after the merge SHALL become active.

A record whose stored coordinates failed validation SHALL be shown in the list as a **quarantined**
row — never dropped silently and never coerced — in the quarantine state the store requirement defines
(E98, E103).

#### Scenario: A list with several spots shows one row each

- **WHEN** the user has saved three spots and opens the `Spots` screen
- **THEN** three rows are shown, ordered alphabetically by `name`, each showing that spot's name, its
  coordinates in the chosen coordinate format, its time zone, whether a horizon profile exists, and
  whether its last brief was for tonight

#### Scenario: A delete can be undone

- **WHEN** the user deletes a spot and taps the undo affordance
- **THEN** the spot is restored with its `id`, horizon profile and equipment profile unchanged, and no
  confirmation dialog was shown before the delete

#### Scenario: Deleting the active spot selects another

- **WHEN** the user deletes the active spot while at least one other saved spot remains
- **THEN** another saved spot becomes active — the first non-quarantined spot remaining in the list
  order — the list still holds the remaining spots, the app is never left with no active spot, and
  undoing that delete restores the deleted spot as the active one

#### Scenario: Deleting the last spot ends at the empty state

- **WHEN** the user deletes the only saved spot
- **THEN** the list shows the scoped empty state with its single call to action, and no spot is left
  active

#### Scenario: The active spot survives a cold start

- **WHEN** the user makes a spot active, then the app is terminated and relaunched, or the device is
  rebooted
- **THEN** that spot is still active, read from the store's persisted `activeSpotId`, and `Night`,
  `Brief`, the widget and the reminder still open it by default

#### Scenario: An empty store has no active spot and fabricates none

- **WHEN** the store holds no saved spot, or the user has never selected one
- **THEN** no spot is active, the screen shows its scoped empty state with its single call to action, and
  nothing computes a sky for a default position (E107)

#### Scenario: A dangling active id resolves to a real spot

- **WHEN** the persisted `activeSpotId` names a spot that no longer exists, or one the store has
  quarantined
- **THEN** the active spot becomes the first non-quarantined spot in the list order, the quarantined
  record is never shown as active, and no blank active state or fabricated `(0, 0)` spot appears

#### Scenario: An import does not carry an active selection

- **WHEN** a scout file is imported while an active spot is already chosen, and again into an empty store
- **THEN** the existing active spot is unchanged, and where none was active the first non-quarantined
  spot after the merge becomes active — because the document carries no active field to read

#### Scenario: A quarantined spot is shown, not silently dropped

- **WHEN** a stored record's coordinates fail validation and the store quarantines it
- **THEN** the `Spots` screen shows it as a quarantined row rather than removing it, and the store's
  quarantine state is the one reported

### Requirement: Favourite targets are marked on the target list and carried with the scout store

The user SHALL be able to mark a catalogue target as a **favourite** and to narrow the target list to
their favourites. Every row of the `Targets` screen and every computed solar-system record SHALL carry
one control that toggles the favourite state, named for TalkBack and at least 48 dp like every other
control in this capability, and its state SHALL be carried by a word or a shape in addition to colour
alone. The `Targets` screen's search and filter bar — the surface of *"The user can find a target by
designation, name or alias, and filter the list"*, which that capability owns — SHALL offer a
**favourites** filter that keeps only favourited rows and combines with the search and the existing
filters. A favourite SHALL be stored as the target's **stable string `id`** (`m31`, `ngc7000`), never as
a list index or a display name, and the set SHALL hold no duplicate id. Every string this control
renders SHALL exist in both `values/strings.xml` and `values-ru/strings.xml`, as the localisation rule
requires of every string in this product.

The favourite set SHALL be persisted through `data/ScoutStore.kt` as one **store-level value** beside the
active spot selection, so it survives a **cold start** and not only process death, and it SHALL travel in
the export document of *"The user's spots, horizons and equipment export to a file they own"* as that
document's `favouriteTargetIds` field — the same stable ids in the same form, so an export/import
round-trip restores the set exactly. The import SHALL **merge** the file's ids with the device's as a
union of stable ids, never duplicating an id and never removing a favourite the user already had.

An id in the stored set that the compiled catalogue no longer contains SHALL be **dropped with one note**
and SHALL NOT invalidate the set or any spot, exactly as a saved spot's dangling target reference is
handled (E31, E99); the set SHALL be re-written without it rather than kept as a permanently dead entry.
When the set is empty, the favourites filter SHALL show a typed empty state naming that there are no
favourites yet, and SHALL NOT show a blank list.

#### Scenario: A target is favourited and the list is filtered to favourites

- **WHEN** the user favourites `m31` on the `Targets` screen and turns on the favourites filter
- **THEN** `m31` is the only row shown, its favourite state is visible without relying on colour, and the
  state is read from the scout store rather than from session state

#### Scenario: Favourites survive a cold start and an export/import round-trip

- **WHEN** the user favourites a target, then the app is terminated and relaunched, and later the setup
  is exported and imported on a fresh install
- **THEN** the same target is still a favourite after the relaunch and after the import, because its
  stable id round-tripped through `favouriteTargetIds`

#### Scenario: An import merges favourites and removes none

- **WHEN** a scout file carrying two favourite ids is imported into a store that already holds one
- **THEN** the resulting set is their union with no duplicate id, and no existing favourite is removed

#### Scenario: A favourite whose target left the catalogue is dropped with a note

- **WHEN** the stored set contains an id the compiled catalogue no longer contains
- **THEN** the id is dropped with one note, the remaining favourites and every spot load normally, and
  the set no longer carries the dead id

### Requirement: The scout's edge states are typed, never blank

The app SHALL define and render a typed, named state for every one of the scout's edge conditions, and
none SHALL be rendered as a blank panel or a plausible default:

- **No spot yet.** The `Spots` screen SHALL show a scoped empty state with one call to action that opens
  `SpotEdit`; no sky SHALL be computed for a fabricated `(0, 0)` position (E107, E110).
- **A spot with no coordinates.** This is impossible by schema: the save action requires a valid
  coordinate pair, and a record already on disk that fails validation is **quarantined** rather than
  coerced (E59); the app never invents a spot at `(0, 0)`.
- **A corrupt stored scout.** The store loads per record; the corrupt record is quarantined and reported
  once (E98), and it is not silently discarded on the next save (E103).
- **A spot whose time zone cannot be resolved.** Where a spot's `tzId` cannot be resolved on the device,
  the app SHALL fall back to the device's zone, label the times as device time, and state the fallback;
  it SHALL NOT silently re-derive the zone from the device and re-label every time (E60, E105).
- **Storage full.** A store or export write that fails for lack of space SHALL surface a typed error and
  SHALL leave the previous good store intact (the atomic-write rule, E100); no partial file is left as
  the store.
- **A cancelled file picker.** A no-op: store unchanged, no error, no partial state.
- **A saved target no longer in the catalogue.** Targets are referenced by stable string id; a missing
  id is dropped with a note and the spot survives (E31, E99).

#### Scenario: No spot is a scoped empty state

- **WHEN** the `Spots` screen is opened with no saved spot
- **THEN** it shows one call to action to create a spot and computes no sky for a default position

#### Scenario: A saved spot with no valid coordinates is quarantined

- **WHEN** a record on disk carries a coordinate outside the valid range
- **THEN** the record is quarantined and reported, and no spot at `(0, 0)` is created (E59)

#### Scenario: An unresolvable zone falls back and says so

- **WHEN** a spot's `tzId` cannot be resolved on the device
- **THEN** times are shown in the device zone with a fallback label, and the spot's own zone id is not
  mutated (E60, E105)

#### Scenario: A full disk does not corrupt the store

- **WHEN** a store write fails for lack of storage
- **THEN** the app surfaces a typed error and the previous good store remains the one read next time
  (E100)

#### Scenario: A removed target does not invalidate its spot

- **WHEN** a saved spot references a target id the compiled catalogue no longer contains
- **THEN** the missing reference is dropped with a note and the spot otherwise loads normally (E31, E99)

### Requirement: Every string is a resource in both locales and every control is named and reachable

Every user-facing string this capability renders — the spot field labels, the hemisphere controls, the
sign and unit hints, the address-lookup states, the horizon editor labels and its numeric-entry labels,
the "no horizon profile" note, the all-90° confirmation, the export and import labels and their refusal
sentences, the equipment labels and the edge-state messages — SHALL exist as a resource in **both**
`values/strings.xml` and `values-ru/strings.xml`, and SHALL NOT be a literal in code. Every interactive
control this capability renders SHALL have a name for TalkBack (a content description or a labelled
role), SHALL be at least **48 dp** in both dimensions, and SHALL state any state that matters (the
selected spot, the active hemisphere, whether a horizon profile exists) with a word or a shape in
addition to colour alone.

The horizon editor SHALL be usable **without free-hand dragging**: it SHALL offer an alternative —
at minimum, selecting a sample or an azimuth and setting its elevation through a numeric or stepped
control — so a user who cannot perform a drag gesture can still author a profile, and every editor
action SHALL be reachable by TalkBack. Any text SHALL reflow with no fixed-height container and SHALL be
legible at a 200 % system font scale, and the editor SHALL read the window size class and work in
portrait and landscape.

#### Scenario: Both locales carry every key

- **WHEN** the strings used by this capability are compared across the two resource files
- **THEN** every key exists in both `values/strings.xml` and `values-ru/strings.xml` and none is a
  hardcoded literal

#### Scenario: Every control meets the size and role floor

- **WHEN** the scout screens' interactive controls are measured
- **THEN** each is at least 48 dp in both dimensions and each has a TalkBack name

#### Scenario: The horizon can be authored without dragging

- **WHEN** a user who cannot perform a free-hand drag uses the horizon editor
- **THEN** they can select an azimuth or sample and set its elevation numerically, and the resulting
  profile is stored identically to a drawn one

#### Scenario: State is not carried by colour alone

- **WHEN** a user who cannot distinguish the palette's hues reads the scout screens
- **THEN** every state that matters is also written or shaped, not only coloured

**Honest boundary, recorded rather than glossed.** This capability specifies the shape, the storage, the
handoffs and the refusals of the scout; it cannot prove that a drawn profile is a *correct* horizon — a
profile a user drew badly is stored faithfully and used faithfully, which is the point. Two limits are
stated rather than implied. First, the export/import format is a **contract this capability defines**;
it is not a competitor's format, and importing another app's file is out of scope here (the survey that
would settle whether any incumbent exports a machine-readable format is recorded as `UNVERIFIED` in
`L-depth-retention-edges.md` §3.1). Second, the durability half proves that the backup resources are
**declared and correctly scoped**; no program here can prove that a given device's backup actually
restored the store, and the app is required to say so rather than claim a restore it did not receive
(E106). The judgement that a horizon looks right, and that the exported file is worth keeping, stays with
the user and with the owner at checkpoint 1.

## MODIFIED Requirements

None. This change adds a new capability and modifies no existing spec.
