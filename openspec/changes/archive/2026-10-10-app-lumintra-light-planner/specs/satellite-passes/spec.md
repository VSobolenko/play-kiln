# Satellite passes

## Purpose

Let a night photographer see, inside the same session brief as the darkness, the moon and the
targets, when a tracked satellite such as the ISS will be over their saved spot, whether that pass is
actually observable from behind their own drawn horizon, and — the capability's headline — whether
that pass will draw a streak across the lens frame they are shooting with, during the exposure they
set. A satellite is one more condition inside one night's plan, never a tracker the app is built
around. Because a wrong pass time or a confident wrong "nothing will cross" is worse than an honest
gap, this capability is deliberately fail-closed: it refuses to compute from an element set too old
to trust, it refuses to invent a brightness the element data does not carry, and it states the age of
every element set beside every prediction. The element data is fetched at use time from CelesTrak as
OMM JSON, cached per group, and never bundled frozen into the app.

## ADDED Requirements

### Requirement: The element data is CelesTrak OMM JSON, never fixed-width TLE

The app SHALL fetch orbital element sets from
`GET https://celestrak.org/NORAD/elements/gp.php?GROUP=<GROUP>&FORMAT=JSON`, where `<GROUP>` is one of
`STATIONS`, `VISUAL` (the 100 brightest), or `CATNR=<noradId>` for a user-named object, and SHALL
parse the response as **OMM JSON** — an array of element records in the CCSDS OMM keyword set — and
SHALL NOT parse the fixed-width two-line TLE format (edge case `E32`). The fields this capability
depends on are exactly:
`OBJECT_NAME`, `OBJECT_ID`, `EPOCH`, `MEAN_MOTION`, `ECCENTRICITY`, `INCLINATION`, `RA_OF_ASC_NODE`,
`ARG_OF_PERICENTER`, `MEAN_ANOMALY`, `EPHEMERIS_TYPE`, `CLASSIFICATION_TYPE`, `NORAD_CAT_ID`,
`ELEMENT_SET_NO`, `REV_AT_EPOCH`, `BSTAR`, `MEAN_MOTION_DOT`, `MEAN_MOTION_DDOT`. The app SHALL NOT
assume a 5-digit catalogue number: records with 6-digit `NORAD_CAT_ID` values (100000+) SHALL be parsed
and shown, because CelesTrak exhausted the 5-digit space on **2026-07-11** and states that GP data is
not available for newly catalogued objects in TLE format (ON RECORD — the source's own banner).

OMM units are degrees for angles and revolutions per day for mean motion, while the propagator's field
constructor takes radians and radians per second; the conversion SHALL be exactly:

```text
i_rad    = INCLINATION        × π / 180
raan_rad = RA_OF_ASC_NODE     × π / 180
pa_rad   = ARG_OF_PERICENTER  × π / 180
ma_rad   = MEAN_ANOMALY       × π / 180
n_radps  = MEAN_MOTION        × 2π / 86400        # rev/day → rad/s
nDot     = MEAN_MOTION_DOT    × 2π / 86400²       # rev/day² → rad/s²
nDDot    = MEAN_MOTION_DDOT   × 2π / 86400³       # rev/day³ → rad/s³
epoch    = the EPOCH instant on the UTC time scale
```

`MEAN_MOTION` SHALL NOT be passed through unconverted — a value of `15.48769982` is rev/day, and the
propagator wants `15.48769982 × 2π / 86400` rad/s. Negative `MEAN_ANOMALY`, `RA_OF_ASC_NODE` and
`ARG_OF_PERICENTER` SHALL be **wrapped** into `[0, 2π)` (add or subtract `2π` until in range), never
clamped. `CLASSIFICATION_TYPE` supplies the classification character, and `OBJECT_ID`
(`"1998-067A"`) supplies the launch year, number and piece; `ELEMENT_SET_NO` and `REV_AT_EPOCH` are
carried as the element number and revolution number at epoch.

**The response carries no magnitude field.** The key list above is the complete set of keys the app
depends on, and none of them is a brightness. The app SHALL therefore treat brightness as an explicit
unknown: it SHALL NOT derive a magnitude or a bright/faint label from `BSTAR`, `MEAN_MOTION_DOT` or any
other element, and it SHALL NOT show a brightness claim on a pass row unless a **curated per-satellite
magnitude table**, authored and verified against two independent references as facts (the same
discipline the object catalogue uses, is present (`E27`). With no such table the pass row SHALL show no
brightness figure and no bright/faint label.

#### Scenario: A 6-digit catalogue number is parsed, not skipped

- **WHEN** a fetched record carries `NORAD_CAT_ID` of `100123` (six digits)
- **THEN** the record is parsed and its object is available to the pass finder, because the parser
  reads OMM keys rather than a fixed 5-digit column

#### Scenario: Mean motion is converted before it reaches the propagator

- **WHEN** a record with `MEAN_MOTION = 15.48769982` and `INCLINATION = 51.6313` is converted
- **THEN** the propagator receives `15.48769982 × 2π / 86400` rad/s and `51.6313 × π / 180` rad, and
  never the unconverted rev/day or degrees values

#### Scenario: A negative angle is wrapped, not clamped

- **WHEN** `ARG_OF_PERICENTER` is negative
- **THEN** the converted value is added to `2π` until it lies in `[0, 2π)` rather than being forced to `0`

#### Scenario: Brightness is never invented

- **WHEN** a pass row is rendered for an object with no curated magnitude entry
- **THEN** the row states no magnitude, no "bright" label and no "faint" label, and `BSTAR` is never
  shown or converted to brightness

#### Scenario: A 6-digit catalogue number survives Orekit's own OMM path

- **WHEN** the slot's unit test feeds a CelesTrak-shaped OMM record whose `NORAD_CAT_ID` is `100123`
  through Orekit's `OmmParser` → `Omm.generateTLE()`
- **THEN** either the catalogue number survives into the parsed orbit, or the test fails and the record
  is handled through the documented field mapping — in no case is the object silently dropped, and in
  no case is a truncated or substituted identity shown

**Settled rather than assumed, 2026-10-08.** The propagation library is Orekit, pinned at
`org.orekit:orekit:13.1.9` (the design's `satellites` section fixes the coordinate), and it **does**
ship a CCSDS/OMM parser that yields a `TLE` directly: `javap -public` over `orekit-13.1.9.jar` shows
`org.orekit.files.ccsds.ndm.odm.omm.OmmParser` with **`public Omm build()`**, and `Omm` exposing
**`generateTLE()`**, `generateKeplerianOrbit()` and `generateSpacecraftState()` (MEASURED). `OmmTle`
carries the SGP4 element set — `getBStar()`, `getMeanMotionDot()`, `getMeanMotionDotDot()`,
`getElementSetNo()`, `getRevAtEpoch()`, `getEphemerisType()` with the `EPHEMERIS_TYPE_*` constants,
`getNoradID()` and `getClassificationType()`. The app SHALL therefore obtain the orbit from Orekit's
own OMM support — parse with `OmmParser` and ask `Omm.generateTLE()` — rather than hand-mapping fields.
The conversion in the first requirement is the **fallback contract**, used for a record the parser
refuses, and the invariant the unit test asserts either way. Two things the measurement does **not**
establish are required tests rather than assumptions, and both come from the same `javap` run: whether
`generateTLE()` tolerates a 6-digit `NORAD_CAT_ID` (the scenario above), and whether a record whose
`EPHEMERIS_TYPE` is not SGP4 is refused explicitly rather than silently mis-propagated.

### Requirement: The element fetch is fail-closed, one download per update, and never in the background

The app SHALL fetch **at most one** element download per update cycle, SHALL record each group's
`fetchedAtUtc`, and SHALL NOT re-fetch the same group within **2 hours** — the source's own cadence,
"For GP data, updates are once every 2 hours" (ON RECORD; edge case `E35`). On **any** non-200 response
(301, 403, 404, 429, any 50x) the app SHALL stop the fetch loop immediately, SHALL NOT retry inside that
cycle, and SHALL surface a typed unavailable state; it SHALL NOT re-attempt before the next permitted
cadence. This is the source's own rule for a machine client (`E34`), and it warns that repeatedly
ignoring non-200 responses sends the client's address to a firewall (ON RECORD). The app SHALL send an
identifying `User-Agent` on every request — `Lumintra/<versionName> (v.sobolenko.vs@gmail.com)`, the
developer account's public address, defined once and not a secret, the same identifying-header duty the
forecast carries (`E47`) — and SHALL NOT schedule a fetch on the hour.

The app SHALL NOT fetch while it is not in use, because a mobile client must not retrieve data when the
app is not in use; a fetch happens only while the app is open, for the spot the user is looking at.

When the network is absent the app SHALL use the **last cached element set** and SHALL apply the same
age bands as the next requirement; a cache hit is never treated as fresh (`E37`). The cache entry is
`{ noradId, group, epochUtc, fetchedAtUtc, payload }`, and it SHALL be excluded from Auto Backup
because an element set is regenerable and a restored one would be presented as fresh. The app SHALL
NOT bundle a frozen element set to compensate for a failed or refused fetch; bundling the **object
list** — names and catalogue ids, which carry no elements — is permitted, bundling **elements** is not.

**The source's commercial licence is an open item.** CelesTrak's usage policy is a **bandwidth
policy**, not a licence: it states the data are "freely available to all users" and it neither forbids
nor grants commercial use, and no SPDX licence text was found on the pages read (MEASURED). Whether a
paid app is affirmatively licensed is therefore **UNVERIFIED**. The app SHALL NOT depend on the source
being contractually granted: the pass section is fail-closed and the whole rest of the brief — dark
window, moon, targets, scene, cloud — is complete without it. The fallback, if the source is not
confirmed for a paid app, is that the **capability ships disabled** and the app states that satellite
passes are unavailable, rather than shipping a feed whose terms are unresolved. The open item is
settled before release by reading the terms footer of `https://celestrak.org/` and
`gp-data-formats.php` for an explicit grant or a commercial clause, and, if none is found, by asking
`TS.Kelso@celestrak.org` (the address the policy publishes) and recording the answer.

#### Scenario: A non-200 stops the fetch

- **WHEN** the endpoint answers 429 or 503
- **THEN** the loop stops with no retry, the section shows its typed unavailable state, and no further
  request is made until the next permitted cadence

#### Scenario: Two opens within the cadence cause one download

- **WHEN** the user opens the brief, closes it, and opens it again 20 minutes later
- **THEN** the second open serves the cached set and makes no request, because the group was fetched
  less than 2 hours ago

#### Scenario: No fetch while the app is not in use

- **WHEN** the app is closed or backgrounded
- **THEN** no element request is issued, regardless of the elapsed time

#### Scenario: Offline with a cache uses the cache under the age bands

- **WHEN** the device has no network and the cached element set is 20 hours old
- **THEN** passes are computed from the cache, the element-set age is shown, and no request is attempted

#### Scenario: Offline with no cache is a typed unavailable state

- **WHEN** the device has no network and no cache exists
- **THEN** the section shows that satellite passes need one connection to download element data, and it
  does not show a frozen or invented element set

#### Scenario: An unresolved licence disables the capability rather than shipping it

- **WHEN** the source's commercial terms are still unresolved at release
- **THEN** the capability ships disabled and the app says so, and no element set is bundled in the APK

### Requirement: An element set is used only inside its age bands

The app SHALL compute the element set's age as `sessionInstantUtc − EPOCH` (both UTC) and SHALL apply
exactly these bands, once, everywhere the capability computes a pass (`E36`). `sessionInstantUtc` is the
session's "now" — captured once when the brief is opened and advanced by the UI, never re-read from the
system clock on each recomputation (`E70`), so the age of a set cannot twitch under the user.

| Element-set age | Behaviour |
|---|---|
| `age ≤ 24 h` | normal — compute, and show no age badge |
| `24 h < age ≤ 72 h` | compute, **and show the element set's age prominently** beside every prediction, with the sentence "element set is N days old — pass times may shift by minutes" |
| `age > 72 h` | **refuse to compute passes** and say so: "element set expired — connect once to refresh"; no pass time of any kind is shown |

**Why this boundary.** A wrong prediction is worse than no prediction, so the boundary is set on the
evidence and the cost of being wrong. The same propagator is measured to diverge **27,924 km** at
≈ 21.3 days from its epoch (MEASURED), and the penalty for a wrong minute — a missed pass, on an app
that advertises offline use — is asymmetric; a day is comfortably inside usefulness while three days
is where the app stops trusting the set. These bands are **product choices, not measured constants**,
and the UI ever names the band it applied.

**The looser reading exists and is rejected.** One source report (`N` §3.6) badges an element set up to
**168 h** (7 days) old and refuses only beyond it; the tighter **72 h** refusal — the one `L` (`E36`)
and `design.md` carry — is what this capability adopts, and the divergence is settled here once rather
than averaged in two places. The measurement that would replace the 72 h line with a curve — freezing
one live element set and comparing its predictions to each day's fresh set for ≈ 21 days — is named as
an open item below.

#### Scenario: A fresh set needs no badge

- **WHEN** the element set is 12 hours old
- **THEN** passes are computed and no age badge is shown

#### Scenario: A set between one and three days is computed with its age shown

- **WHEN** the element set is 40 hours old
- **THEN** passes are computed and each prediction carries the age badge naming the number of days

#### Scenario: A set past three days is refused

- **WHEN** the element set is 80 hours old
- **THEN** no pass time is computed, the section says the element set expired and offers a refresh, and
  no predicted time is shown

#### Scenario: A cache hit is judged by its age, not by being a hit

- **WHEN** a cached element set is used offline and it is 90 hours old
- **THEN** it is refused exactly as a freshly fetched set of the same age would be

**Honest boundary, recorded rather than glossed.** The bands are a product decision; the
timing-error-versus-age curve is **UNVERIFIED**, and the experiment that would replace the 72 h line
with a measured curve and a confidence interval is: freeze one live ISS element set, store it, and each
day for ≈ 21 days propagate both the frozen set and that day's fresh set over the same next-24 h window,
recording the maximum AOS-time difference.

### Requirement: A pass is reported only when it is geometrically and optically observable

A pass is a `SatellitePass` with a `PassVisibility` and it SHALL be reported as **observable** only when
**all** of the following hold, and it SHALL otherwise be reported as **occurring but not visible**, with
the reason — never silently dropped, and never offered as observable:

1. **Elevation.** `maxElevationDeg ≥ maskDeg`. `maskDeg` defaults to **10.0°** and is user-adjustable
   from **0.0° to 45.0°**. Where the spot has a drawn `HorizonProfile` (the user's horizon line), the
   profile's elevation at the pass azimuth SHALL be the mask in place of the constant, so a pass behind
   the user's ridge is not "visible" (`E42`; the same drawn-horizon convention the targets use, `E25`).
2. **The satellite is sunlit.** The satellite SHALL NOT be inside the Earth's umbra, tested as a
   Sun–satellite–Earth occultation against the WGS84 ellipsoid. The umbra means not visible; the
   **penumbra means visible-but-fading** — one model, used by the pass finder and the scene alike, never
   two different shadow rules (`E41`).
3. **The observer is dark.** The Sun's **geometric** altitude at the pass instant SHALL be
   `≤ −6.0°` (civil) by default, with the user able to select **−6.0°** ("visible in twilight") or
   **−18.0°** ("fully dark"); the app SHALL state which threshold it used.
4. **Minimum duration.** The **observable interval** — the intersection of (above the mask) ∧
   (satellite sunlit) ∧ (observer dark) — SHALL be at least **30 s** (`E40`); a pass whose observable
   interval is shorter SHALL NOT be reported as visible. (30 s is a product threshold, chosen so a pass
   too short to be photographed is not offered as visible.)
5. **Geometry.** The acquisition and loss instants SHALL come from the elevation crossings at the
   observer's topocentric frame, paired into one pass.

Units and signs: elevation is degrees above (+) or below (−) the horizon; azimuth is degrees with
**0 = North, 90 = East**; the Sun's altitude is its geometric altitude in degrees; all instants are UTC,
displayed in the spot's zone.

The pass is therefore a two-state result: **occurs** (the geometry happens) and **observable** (it can
be seen from this spot at this instant). A pass that occurs but is not observable SHALL be shown as
occurring with its reason, because silently dropping it reads as a missed pass (`E38`, `E39`).

#### Scenario: A sunlit pass over a dark observer is observable

- **WHEN** a pass peaks at 42° (mask 10°), the satellite is sunlit, and the Sun is at −14° geometric
  altitude
- **THEN** it is reported as observable with its maximum elevation and duration

#### Scenario: A pass in the Earth's shadow is not visible

- **WHEN** the satellite is inside the Earth's umbra for the whole pass
- **THEN** the pass is reported as occurring but not visible, with the reason "satellite in Earth's
  shadow", and it is not offered as observable

#### Scenario: A daylight pass is not visible

- **WHEN** the Sun's geometric altitude at the pass instant is −2.0°
- **THEN** the pass is reported as occurring but not visible, with the reason "the sky is too bright"

#### Scenario: A pass below the mask is not reported

- **WHEN** a pass peaks at 7° with a 10° mask
- **THEN** it is not reported at all as a pass

#### Scenario: The drawn horizon is the mask

- **WHEN** the horizon line at the pass azimuth is 25° and the pass peaks at 12°
- **THEN** the pass is reported as occurring behind the user's horizon, not as visible

#### Scenario: An observable interval shorter than the floor is refused

- **WHEN** the intersection of the three conditions lasts 18 seconds
- **THEN** the pass is not reported as visible, and the reason names the short observable interval

**Honest boundary, recorded rather than glossed.** The umbra/penumbra visible rule is a **product
decision**, and the drawn horizon as an elevation mask is **UNVERIFIED** end to end — the engine's
elevation-mask primitive exists but no probe exercises it. The check that settles it is a pass computed
with the drawn profile against the same pass computed with a constant mask.

### Requirement: Every event search is segmented, and a test proves it against a brute-force scan

**The measured trap.** A single propagation over a long window, with an elevation detector attached,
finds a fraction of the passes that exist: over **3 days** at a 10° mask it reported **1** acquisition
where a 60-second brute-force elevation scan found **15**, and tightening `maxCheck` from 600 s to 10 s
changed nothing (MEASURED, reproduced three times; edge case `E43`, which is also `E01` and `E10` seen
from another direction). Re-segmenting the same propagation into 1-hour windows with `maxCheck = 60 s`
found all **15**, agreeing exactly with the brute-force scan.

The app SHALL therefore segment **every event search this capability performs** — the window is never
walked by a single call — exactly as follows:

1. The search window is the session's dark window, capped at **24 h**, expressed as `[t0, t1]` in UTC.
2. Segment length is **3600 s**. Iterate `t = t0`; while `t < t1`, `tNext = min(t + 3600 s, t1)`.
3. For **each** segment a **fresh** elevation detector is built with `maxCheck = 60.0 s` and the default
   threshold (`1e-6`), attached to a propagator **started at `t`**, and propagated to `tNext`. The
   re-arm is the point: a detector that has already fired is not re-armed inside one call.
4. Both directions are collected — the acquisition and the loss — and paired into passes; a pass is kept
   only if `maxElevationDeg ≥ maskDeg`.
5. A 60-second brute scan SHALL NOT be the runtime algorithm; it is the *check*.

A test SHALL assert that the segmented finder returns the **same acquisition count as a brute-force
60-second scan** over the same window, mask and element set — the 3-day / 10° case returns **15**
acquisitions and **30** events (`E43`) — because one probe row labelled as segmented was in fact another
one-call run, so the label alone is not evidence.

#### Scenario: The segmented finder matches the brute-force scan

- **WHEN** the fixture element set is searched over 3 days with a 10° mask through the segmented finder
  and through a 60-second brute-force scan
- **THEN** both return 15 acquisitions, and the test fails otherwise

#### Scenario: The single-call implementation fails the test

- **WHEN** the segmentation is replaced by one propagation over the whole window
- **THEN** the comparison test fails, because the single call returns 1 acquisition against the
  brute-force 15

#### Scenario: The runtime search is segmented, not a 60-second scan

- **WHEN** a whole night is searched at runtime
- **THEN** the work is at most 24 one-hour propagations, and the 60-second scan exists only in the test

**Honest boundary, recorded rather than glossed.** The same failure shape is ON RECORD for this
capability's own search; the requirement above proves the count for a satellite pass and proves nothing
about the other event searches in the app (twilight, moonrise, target rise/set), which carry the same
rule in their own capabilities. A one-hour segment is asserted short enough that no pass is missed and
long enough that the cost is bounded; that assertion is a test, not a proof for every element set.

### Requirement: The only accuracy claim is propagator fidelity, and prediction accuracy is never claimed

Two different numbers exist and SHALL NOT be conflated: the propagation reproduces the reference
implementation to `pos ≈ 1.0e-4 km` (MEASURED) — **propagator fidelity, never real-world accuracy** —
while the same propagation **drifts 27,924 km at ≈ 21.3 days** from its epoch (MEASURED).

The app **may show** the user exactly this sentence, and no stronger one:

> "Pass times are computed from NORAD GP element sets. A fresh element set (hours old) is accurate to
> within a few minutes; the age of the element set is shown, and passes are not computed from an
> element set older than 72 hours."

The app **may never show**: that a pass time is accurate to 0.1 m, or to a stated number of seconds;
and it SHALL NOT surface the `1e-4 km` fidelity figure as accuracy on the sky, in any screen, store
copy or share text.

#### Scenario: The accuracy copy is the permitted sentence

- **WHEN** the satellite section's accuracy copy is read in `en` or `ru`
- **THEN** it contains the permitted sentence and no "0.1 m", no metre figure, and no stated number of
  seconds

#### Scenario: The fidelity number never reaches a user

- **WHEN** every user-facing string in the capability is searched
- **THEN** the `1e-4 km` propagator-fidelity figure appears in none of them

#### Scenario: The element-set age is always on screen

- **WHEN** any pass time is shown
- **THEN** the element set's age is shown beside it

**Honest boundary, recorded rather than glossed.** The permitted sentence's "within a few minutes" is
itself **UNVERIFIED** — the numeric bound for a fresh element set was never measured. The check that
settles it is a same-night comparison of the app's computed acquisition time for the ISS against an
independently observed or third-party-predicted acquisition time at the same site, recording the
element-set epoch used. Until that is recorded, the app claims fidelity and shows the age; it does not
claim a number of seconds. The "7 days" wording of the inherited sentence is aligned here to the
settled **72 h** band rather than repeated, so the copy and the rule cannot disagree.

### Requirement: The app predicts whether a pass will cross the lens frame, and says how confident it is

This is the capability's headline. For the user's chosen lens frame and the session's exposure
interval, the app SHALL determine **whether a pass crosses the frame during the exposure, and where**.

**Geometry.** The frame is the camera's angular `FieldOfView` (`fovH` × `fovV` in degrees, computed in
`domain/Optics.kt` from focal length and sensor size in millimetres — millimetres everywhere, never
unit-converted, `E71`; the same framing arithmetic the target fit uses, `E29`) rotated to the chosen
roll and centred on the scene's aim point. The satellite's topocentric track is sampled at **1 s** over the
pass's observable interval, giving `(altitudeDeg, azimuthDeg)` per sample; each sample is projected into
the scene through the **same equidistant projection** the frame overlay uses
(`pxPerDeg = canvasWidthPx / sceneFovHorizDeg`), so the frame is a true 1:1 angular rectangle. A
**crossing** is when a projected sample lies inside that rectangle.

**Exposure intervals considered.** The exposure interval `E` is a positive duration in seconds taken
from the session's equipment profile. The app SHALL evaluate every exposure-length sub-interval of the
observable window whose samples lie inside the frame, reporting the maximal run of inside-frame samples
as the crossing, with its **entry and exit instants** and each sample's altitude and azimuth. Where `E`
is at least as long as the observable interval, the whole observable interval is one exposure.

**What is drawn on the scene.** One fading satellite trail per crossing, split at horizon crossings,
drawn **only over the observable sub-interval** so the picture and the words agree (`E40`). The number
of crossings drawn on the scene is **capped**, the cap is stated in the layer legend, and the underlying
pass list stays complete even when the drawing is capped (`E44`).

**The honest label.** Every streak result SHALL carry the label that it is a **predicted crossing of a
predicted track**, with the element set's age attached. Because the element data carries no brightness,
the app SHALL NOT state that the streak will be visible; it states that a crossing is predicted.

**Several satellites.** The result SHALL list every crossing the tracked objects produce in the
exposure window, and SHALL state the cap on drawn streaks where the picture is capped (`E44`).

**No crossing.** A confident wrong "none" is worse than no answer, so the app SHALL use confidence
wording that names what it checked and what it did not. It SHALL say the result is a prediction from
the tracked objects and the element set fetched at the named time, that the set is the stated age, and
that an object outside the tracked groups, or catalogued after the set was fetched, is not covered. It
SHALL NOT say "no satellite will cross your frame".

#### Scenario: A crossing is predicted with its label and its time

- **WHEN** the satellite's projected track passes inside the user's frame during the exposure
- **THEN** the result states the crossing with its entry and exit times (UTC and in the spot's zone,
  with units), and carries the label "predicted crossing of a predicted track" with the element-set age

#### Scenario: The streak is drawn only over the observable sub-interval

- **WHEN** part of the pass is in the Earth's shadow or in daylight
- **THEN** the drawn streak covers only the observable sub-interval, matching the text

#### Scenario: Several satellites can cross

- **WHEN** two tracked objects cross the frame in the same night
- **THEN** both are listed, and any cap on the number drawn is stated in the layer legend

#### Scenario: No crossing uses confidence wording, never a bare "none"

- **WHEN** no tracked object crosses the frame during the exposure
- **THEN** the app states that it is a prediction from the tracked objects and the element set fetched
  at a named time (age shown), that an untracked or later-catalogued object is not covered, and it does
  not state that no satellite will cross

#### Scenario: Brightness is not claimed for a streak

- **WHEN** a crossing is predicted for an object with no curated magnitude
- **THEN** the result states a crossing is predicted and states no brightness and no "visible" claim

**Honest boundary, recorded rather than glossed.** The match is **textual and geometric over the
sampled track**: a projected sample inside the frame rectangle is not a photograph of a real streak, and
the result is explicitly labelled a predicted crossing of a predicted track. The wedge itself is a
market fact — no shipped Android paid app was found bundling satellite-streak forecasting into a
light-planner (INFERRED, competitor read §1.3) — and that fact is evidence of an opening, not a
correctness proof.

### Requirement: Every pass carries the same readout, each number with its unit

For each pass the app SHALL show, each value with its unit:

- **Start (acquisition)** — the instant in UTC and in the spot's zone, with the satellite's altitude in
  degrees and azimuth in degrees at that instant.
- **Peak** — the instant, the **maximum elevation** in degrees, and the azimuth in degrees.
- **End (loss)** — the instant in UTC and in the spot's zone, with altitude in degrees and azimuth in
  degrees.
- **Maximum elevation** — degrees.
- **Duration** — minutes and seconds.
- **Direction of travel** — the compass bearing from the acquisition azimuth to the loss azimuth, named
  as a cardinal pair ("NW → SE" in English, the localised equivalent in `ru`).
- **Element-set age** — hours or days, with the band it falls in (the next requirement's bands).
- **State** — observable, or occurring-with-reason.

No pass row SHALL show a bare number without its unit.

#### Scenario: The readout is complete and unit-bearing

- **WHEN** a pass is shown
- **THEN** start, peak and end times, the maximum elevation, the duration, the direction of travel and
  the element-set age are all present, and every number carries its unit

#### Scenario: An unobservable pass still reads out

- **WHEN** a pass occurs but is in the Earth's shadow
- **THEN** its geometry is shown together with the reason it is not visible, rather than an empty row

### Requirement: The pass detail is the one surface for every pass, and it has an empty state

The pass detail SHALL be the single surface that presents this capability's passes, and it SHALL be a
**modal bottom sheet** overlaying the surface it was opened from, never a Navigation 3 destination of its
own. Its cited entry point is the **pass warning** row on the `Night` screen
(`ui/night/NightSceneScreen.kt`), whose own wording is "the pass warning row (focusable, opens the pass
detail)" (`specs/night-scene/spec.md`). Dismissing the detail — by the back gesture, a drag down, or its
close control — SHALL return to the `Night` screen at the scrub instant it held, with the pass warning
row focused, and SHALL NOT push or pop any destination.

**Contents and ordering.** The pass detail SHALL list every pass in the session window for the tracked
objects, ordered by **acquisition instant ascending** (soonest first); a pass that occurs but is not
observable SHALL stay in that same list, in that same order, with its reason rather than being omitted or
moved. Each entry SHALL carry the full readout the "Every pass carries the same readout" requirement
defines — start, peak and end with their altitude and azimuth, the maximum elevation, the duration, the
direction of travel, the element-set age with its band, and the state — and the list SHALL state the
element set's fetched time and its age once at its head, in addition to each entry's own age. The list
SHALL scroll and SHALL work in portrait and landscape.

**No pass in the window.** The empty state SHALL NOT be a blank sheet, a zero-length row or a spinner
that never resolves. It SHALL state "No passes in tonight's window for the tracked objects." as a
resource in both locales, and SHALL carry the same confidence wording the no-crossing case carries: that
it is a prediction from the tracked objects and the element set fetched at the named time (its age
shown), and that an object outside the tracked groups, or catalogued after the set was fetched, is not
covered. It SHALL NOT say that no satellite will pass.

#### Scenario: The pass warning row opens the pass detail and back returns to the scene

- **WHEN** the user activates the `Night` screen's pass warning row and then dismisses the pass detail
- **THEN** the sheet opens over the scene, and on dismissal the `Night` screen is shown at the same scrub
  instant with the pass warning row focused, with no destination pushed or popped

#### Scenario: The passes are listed soonest-first, and an unobservable pass is not moved

- **WHEN** one observable pass at 22:10 and one shadowed pass at 21:40 fall in the same window
- **THEN** the shadowed 21:40 pass is listed first with its shadow reason and the observable 22:10 pass
  second, each with the full readout

#### Scenario: No pass in the window is a typed empty state, never a blank sheet

- **WHEN** the tracked objects produce no pass inside the session window
- **THEN** the sheet states "No passes in tonight's window for the tracked objects." with the element-set
  age and the confidence wording, and it shows no empty list, dash or spinner

**Honest boundary, recorded rather than glossed.** This requirement proves the surface's entry point,
its ordering, its content and its empty state; the citation it answers is `specs/night-scene/spec.md`'s
pass warning row, and whether the sheet's motion keeps the scene's own state intact on the A40 is read
from the device run, not from this document.

### Requirement: The edge states are typed outcomes, never empty lists

Each of the following SHALL be a typed, named state with one sentence the user sees, and each SHALL
refuse what the right-hand column refuses:

| Situation | Edge case | What the user sees | What is refused |
|---|---|---|---|
| **No network at first run** | `E37` | "Satellite passes need one connection to download element data." The brief is otherwise complete and shares normally. | A bundled frozen element set presented as current; an empty pass list shown as "no passes tonight" |
| **Stale set** (age > 72 h) | `E36` | "Element set expired — connect once to refresh." | Any computed pass time from a set older than 72 hours |
| **No elements for the chosen object** | `E32` | The object is omitted and the section says the satellite data are unavailable for it; never "no passes tonight" | An empty list presented as a result; a guessed element set |
| **A pass entirely in daylight** | `E39` | "A pass occurs at HH:MM but the sky is too bright." In polar summer the app says this once for the night, not per pass | Presenting it as observable; a list of passes the user cannot see |
| **A pass in the Earth's shadow** | `E38` | "A pass occurs at HH:MM but the satellite is in the Earth's shadow." | Presenting it as observable; silently dropping it |
| **A pass below the horizon** (the drawn horizon hides it) | `E42`, `E25` | "Rises behind your horizon." | Counting it as visible; ignoring the drawn horizon line |
| **An element set whose epoch is in the future** | *not in the register — added here* | "Element set epoch is in the future — check the device clock; passes are not computed." | Computing a pass from an epoch later than the session instant (beyond a small tolerance) |
| **A device whose clock is wrong** | `E93` | Every computed instant is shown **with its UTC instant**, and when a response carries a `Date` header differing from the device clock by more than a few minutes the app shows one line: "Your device clock looks N h M m off (vs the network)." | Silently computing a correct-looking brief for the wrong day; hiding the mismatch |
| **A `200` body the parser cannot read** (not valid JSON, or a record missing a field the parser reads) | *not in the register — added here* | "Satellite data could not be read — passes are unavailable for now." The last valid element set is still used under its age bands when one exists. | Any partial or guessed parse; a defaulted OMM field; caching or re-serving the malformed body as a set |

The two rows marked *not in the register* are written here rather than left unnamed: the edge-case
register covers a badly wrong device clock for the forecast (`E51`) and a wrong system clock generally
(`E93`), but a future element set and a `200` response this capability cannot parse are specific to this
capability.

#### Scenario: First run with no network

- **WHEN** the app is opened for the first time with no network and no cached element set
- **THEN** the satellite section shows its unavailable sentence and the rest of the brief is complete,
  with no spinner that never resolves and no crash

#### Scenario: A stale set is refused, not shown as an empty list

- **WHEN** the only cached element set is 90 hours old
- **THEN** the section says the element set expired and offers a refresh, and it does not render an
  empty pass list as "no passes tonight"

#### Scenario: A chosen object with no element set is omitted, not faked

- **WHEN** the user asks for a specific object that the fetched groups do not contain
- **THEN** the object is omitted with the unavailable sentence, and no element set is fabricated

#### Scenario: A daylight-only pass says why

- **WHEN** every pass of the night is in daylight
- **THEN** the app says the sky is too bright once for the night, and does not offer those passes as
  visible

#### Scenario: A shadowed pass states the shadow, not an empty row

- **WHEN** a pass occurs while the satellite is inside the Earth's umbra
- **THEN** the row shows the pass's geometry with the sentence "the satellite is in the Earth's shadow",
  and the pass is not offered as visible

#### Scenario: A pass behind the drawn horizon says so

- **WHEN** the pass is hidden by the drawn horizon line at its azimuth
- **THEN** the row says "rises behind your horizon", and the pass is not counted as visible

#### Scenario: A future epoch is refused

- **WHEN** an element set's `EPOCH` is later than the session instant beyond the tolerance
- **THEN** the app refuses to compute, names the future epoch, and points at the device clock

#### Scenario: A wrong device clock is visible

- **WHEN** the device clock is 2 h 13 m off a network response's `Date` header
- **THEN** the app shows the one-line clock warning next to the UTC instants, rather than a brief that
  looks correct for the wrong night

### Requirement: A 200 response this capability cannot parse is discarded, never half-read

A `200` status is not proof of usable element data. When the body is not valid JSON, or is valid JSON but
a record lacks any field the first requirement's OMM list reads (for example `MEAN_MOTION`, `EPOCH`,
`INCLINATION` or `NORAD_CAT_ID`), the app SHALL treat the retrieval as failed and SHALL enter the typed
state **`MALFORMED_RESPONSE`**, whose one user-facing sentence is "Satellite data could not be read —
passes are unavailable for now."

While `MALFORMED_RESPONSE` holds:

- the malformed body SHALL NOT be parsed partially, SHALL NOT have its missing fields defaulted, and
  SHALL NOT be cached in place of a valid element set;
- the **last valid element set for the same group** SHALL still be used, exactly as an offline cache hit,
  under the age bands above, with its age shown — it is never presented as the malformed response's own
  data;
- when no valid set exists for that group, the pass section SHALL show the sentence and refuse to
  compute, and SHALL NOT render an empty pass list as "no passes tonight";
- no retry SHALL be attempted inside the cycle, the failed attempt counts as that cycle's single
  download, and the group's `fetchedAtUtc` SHALL NOT be advanced as though a fresh set had been stored;
- the rest of the brief SHALL be unaffected: the dark window, the Moon, the targets, the cloud outlook
  and the scene are local or independently sourced and SHALL NOT degrade with the pass section.

A response that parses but carries no record for the requested group is the **no elements for the chosen
object** state of the table above, not this one; `MALFORMED_RESPONSE` is specifically a body the parser
cannot read.

#### Scenario: A 200 response that is not JSON

- **WHEN** the endpoint answers `200` with a body that is not valid JSON
- **THEN** the pass section shows "Satellite data could not be read — passes are unavailable for now.",
  nothing partial is parsed or cached, and a previously fetched valid set for the group is still used
  under its age bands when one exists

#### Scenario: A JSON body missing a field the parser reads

- **WHEN** a `200` response is valid JSON but one record omits `MEAN_MOTION`
- **THEN** the state is `MALFORMED_RESPONSE`, no pass is computed from that response, no missing field is
  defaulted, and the sentence above is shown

**Honest boundary, recorded rather than glossed.** This requirement proves the state and the refusal; it
cannot prove the source never sends a body that parses into a self-consistent but wrong propagator —
that is the OMM-and-units contract of the first requirement, and a schema change at the source is caught
only when the next fetch fails to parse.

### Requirement: The satellite card is hosted by the Brief and returns to it

The satellite card — the surface carrying the elevation-mask control, the observer-dark threshold
choice, the exposure input, the object picker and the refresh action — SHALL be hosted by the **`Brief`**
screen (`ui/brief/BriefScreen.kt`), under whose scrolled answers the pass section already sits, and SHALL
NOT be a Navigation 3 destination of its own, for the same reason the horizon editor lives inside
`SpotEdit`. It SHALL be presented as a **modal bottom sheet** opened from the brief's `SATELLITE`
condition row (`domain/Brief.kt`'s `ConditionKind.SATELLITE`), which is a focusable control that
announces its role and opens the card.

While the card is open it SHALL overlay the `Brief`, and the brief's rows and the scene behind the sheet
SHALL keep their state. Dismissing the card — by the back gesture, a drag down, or its close control —
SHALL return to the `Brief` at the same scroll position with the `SATELLITE` condition row focused, SHALL
NOT push or pop any other destination, and SHALL NOT recompute or reset the brief's other rows. A change
made in the card (the mask, the threshold, the exposure or the tracked objects) SHALL be reflected in the
brief's `SATELLITE` condition and the scene's streaks once the card is dismissed, computed from the same
session instant the brief already holds.

#### Scenario: The satellite condition row opens the card

- **WHEN** the user activates the `SATELLITE` condition row on the `Brief` screen
- **THEN** the satellite card opens as a sheet over the brief, focus moves into the card, and no
  destination is pushed onto the back stack

#### Scenario: Dismissing the card returns to the brief

- **WHEN** the card is dismissed after the elevation mask was changed
- **THEN** the `Brief` is shown at the same scroll position with the satellite row focused, the row and
  the scene's streaks reflect the new mask, and no other screen was left behind

**Honest boundary, recorded rather than glossed.** This requirement proves the owner, the entry point and
the back behaviour; it does not prove the sheet's motion is smooth on the A40's GPU — that is the device
run's reading — and the card's own control geometry and strings are the next requirement's contract.

### Requirement: The satellite card is accessible and every string exists in en and ru

Every user-facing string this capability produces SHALL be a resource present in **both**
`app/src/main/res/values/strings.xml` and `app/src/main/res/values-ru/strings.xml` — the elevation-mask
threshold labels, the observer-dark threshold labels, the state sentences, the age badge, the accuracy
sentence, the confidence wording, the refresh action and every unit — and SHALL NOT be a literal in
Kotlin. Every interactive control (the elevation-mask control, the observer-dark threshold choice, the
exposure input, the object picker and the refresh action) SHALL be named for TalkBack, SHALL declare a
role, and SHALL be at least **48 dp** in both dimensions. Any state carried by colour SHALL also carry a
word or a shape, and text SHALL meet a contrast ratio of at least **4.5:1** against its background. The
card SHALL read the window size class and work in both portrait and landscape.

#### Scenario: Every string exists in both locales

- **WHEN** the capability's resources are read
- **THEN** every user-facing string has a matching entry in `values/strings.xml` and
  `values-ru/strings.xml`, and no string is hardcoded in Kotlin

#### Scenario: Every control meets the target floor and is announced

- **WHEN** the mask control, the threshold choice, the exposure input and the refresh action are measured
  and traversed with TalkBack
- **THEN** each is at least 48 dp in both dimensions, has a role, and announces its name and current
  value

#### Scenario: An unseen state is not carried by colour alone

- **WHEN** a pass is not visible because the satellite is in the Earth's shadow
- **THEN** the state is written in words as well as coloured

**Honest boundary, recorded rather than glossed.** This requirement proves the resources and the control
geometry exist; it does not prove the Russian wording is idiomatic — that is a human read — and the
colour-alone and contrast claims are measured by the factory's own visual and accessibility checks, not
by this document.

## MODIFIED Requirements

None. This change adds a new capability and modifies no existing spec.
