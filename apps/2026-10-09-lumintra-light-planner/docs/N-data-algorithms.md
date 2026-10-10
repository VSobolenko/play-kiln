# N — Lumintra: data and computation contracts

Product: **Lumintra** (`lumintra-light-planner`, `com.gexetr.lumintralightplanner`), a $9.99 one-time
paid Android light-and-sky session planner. This document pins the **data and computation contracts**
exactly, so an implementer never guesses.

**Scope of this document.** Object table, time/geometry, satellite passes, cloud forecast, field of
view, sky-brightness/darkness quality. Engine assignment and the project's verified/UNVERIFIED split
are summarised in §7 because every other section depends on them.

**Method.** Every licence, endpoint, header rule, protocol constant and API signature below was
**read from its own source in this session** (a live HTTP fetch, a live TAP query, `javap` against the
installed `android.jar`, or a `Select-String`/`Read` over a file on disk). Nothing here is a remembered
constant. Where a thing could not be settled it is labelled `UNVERIFIED` with the exact check that
would settle it. Claims are labelled MEASURED / ON RECORD / INFERRED / UNVERIFIED.

---

## 0. Evidence base (what was actually run this session)

| Probe | Result |
|---|---|
| `GET https://celestrak.org/usage-policy.php` | HTTP 200, 13,980 B, "Last updated: 2026 May 22" |
| `GET https://celestrak.org/NORAD/elements/gp.php?GROUP=STATIONS&FORMAT=JSON` | HTTP 200, 9,270 B, 17 OMM records |
| `GET https://celestrak.org/NORAD/documentation/gp-data-formats.php` | HTTP 200, 40,338 B (OMM/CSV/2-hour cadence FAQ) |
| `GET https://api.met.no/…/locationforecast/2.0/compact` (ToS page + one compact payload) | ToS HTTP 200, 14,446 B; payload `type=Feature`, 87 hourly points |
| `GET https://dc.g-vo.org/tap/sync` (ADQL over `fk6.fk6join`, `TAP_SCHEMA`) | HTTP 200 — row count, columns, magnitudes, sample stars |
| `GET https://dc.g-vo.org/fk6/q/cone/form` | HTTP 200, 15,237 B (CC0 1.0 rights block) |
| `GET https://aa.usno.navy.mil/faq/RST_defs` | HTTP 200, 27,057 B (twilight + rise/set definitions) |
| `GET https://pmc.ncbi.nlm.nih.gov/articles/PMC4928945/` | HTTP 200, 415,664 B (Falchi 2016 licence) |
| `GET https://eogdata.mines.edu/products/vnl/` | HTTP 200, 53,967 B (VIIRS VNL licence) |
| `javap -classpath …/android-35/android.jar android.hardware.camera2.CameraCharacteristics` | FOV constants' exact types |
| `Select-String`/`Read` over `.kiln/scratch/nv-core-proof/data/astronomy.kt` (432,946 B, 10,675 lines) | Refraction formula, `horizon`, `searchAltitude`, `searchRiseSet`, `moonPhase`, `illumination`, `defineStar`, `Observer`, `Topocentric` |
| `Read` over `.kiln/scratch/positioning-2026-10-08/{F,G,I}-*.md` | Staleness, segmentation, build proof, licence tables |

---

## 1. The object table (~250 objects)

### 1.1 Shape of the data: two tables plus one computed section

Three distinct kinds of thing must not be forced into one file:

1. **`assets/catalog/objects.json`** — fixed-coordinate deep-sky objects and double stars (**authored:**
   RA/Dec are constants). ~220 rows.
2. **`assets/catalog/constellations.json`** — the 88 IAU constellations (**authored, but a different
   record shape**: a region, not a point).
3. **Solar-system bodies (Sun, Moon, Mercury…Neptune, 10 records)** — **computed, never authored**:
   a planet has no fixed RA/Dec. They are generated at the observation time and *presented* with the
   same view fields as a catalogue row (§1.7).

**Format decision: JSON in `app/src/main/assets/catalog/`.**

- **JSON is chosen** over CSV because a row has nested fields (`sourceIds`, arrays) and optionality.
- **`assets/` is chosen** over `res/raw/` because assets are not compiled into `R.java`, accept
  arbitrary names, and are read as a stream; 220 rows × ~200 B ≈ 50 KB (before the APK's own gzip).
- **Room is rejected**: 220 read-only rows need no query engine, no migration surface and no
  build-time schema step, and Room would add runtime size for no query the UI makes. All lookups are
  in-memory `Map<id, Object>` after parse.
- Each file carries `{"schemaVersion": 1, "objects": [...]}` so a future migration is detectable.
- The catalogue is a **build input** — it is never downloaded, so the object list works fully offline.

### 1.2 `objects.json` schema (exact)

| Field | Type | Required | Units / domain | Notes |
|---|---|---|---|---|
| `id` | string | ✔ | ASCII lowercase slug | stable key, e.g. `m31`, `ngc7000`, `ic434`, `m42`, `ngc5139` |
| `messier` | string \| null | ✔ | `"M31"` (with prefix) | null when not a Messier object |
| `ngc` | string \| null | ✔ | `"NGC 224"` | null when absent |
| `ic` | string \| null | ✔ | `"IC 434"` | null when absent |
| `name` | string | ✔ | English common/proper name | e.g. `"Andromeda Galaxy"` |
| `nameRu` | string \| null | ✔ | Russian name | hand-authored; `ru` is a declared locale |
| `type` | enum | ✔ | see §1.2.1 | drives the glyph and the filter |
| `raDeg` | number | ✔ | **degrees**, `[0, 360)` | ICRS **right ascension** |
| `decDeg` | number | ✔ | **degrees**, `[-90, +90]` | ICRS **declination** |
| `epoch` | string | ✔ | constant `"J2000.0"` | see §1.3 |
| `magV` | number \| null | ✔ | magnitudes | integrated **V**, where a V value exists |
| `magB` | number \| null | ✔ | magnitudes | **B**, only when no V exists |
| `magSystem` | enum `"V"` \| `"B"` \| `null` | ✔ | which of the two is the primary | see §1.3 |
| `magKind` | enum `"integrated"` \| `"peak"` \| `"unknown"` | ✔ | what the magnitude measures | a peak mag on a nebula is not comparable to an integrated one |
| `sizeMajArcmin` | number \| null | ✔ | **arcminutes** | major axis |
| `sizeMinArcmin` | number \| null | ✔ | **arcminutes** | minor axis |
| `positionAngleDeg` | number \| null | ✔ | degrees `[0, 180)` | north through east |
| `aliases` | string[] | ✔ | may be empty | other designations, for search |
| `commonNames` | string[] | ✔ | may be empty | extra searchable names |
| `sourceIds` | object | ✔ | `{wikidata, heasarc, simbad}` | provenance; values are the catalogues' own ids |
| `verification` | object | ✔ | `{srcA, srcB, deltaArcmin}` | the independent cross-check record (§1.4) |
| `magDisputed` | boolean | ✔ | default `false` | true when two sources' magnitudes disagree > 0.5 (§1.4) |
| `notes` | string \| null | ✔ | free text | discrepancies, unresolved questions |

**Not stored, derived at load:** `raHours = raDeg / 15` (see §1.3 — the engine wants hours),
`bestSeasonMonths` (from `raDeg` and the observer's latitude).

#### 1.2.1 The `type` enum (closed set)

`galaxy`, `galaxy_group`, `globular_cluster`, `open_cluster`, `emission_nebula`, `reflection_nebula`,
`planetary_nebula`, `supernova_remnant`, `dark_nebula`, `asterism`, `double_star`, `planet`, `moon`,
`sun`. The last three are used **only** by the computed solar-system section (§1.7), never authored
into `objects.json`.

### 1.3 Unit and sign conventions (the traps)

- **Right ascension: the table stores DEGREES; the engine takes HOURS.** `astronomy.kt`'s
  `horizon(time, observer, ra, dec, refraction)` converts internally with `sinra = dsin(ra * 15.0)`
  (measured at `astronomy.kt:5400-5413`), and `defineStar(body, ra, dec, distanceLightYears)` documents
  `ra` **in hours [0,24)** (`astronomy.kt:452`, `:191-196` of the F-map). Conversion happens **once**,
  at the engine boundary — never store hours.
- **Declination: degrees, `[-90,+90]` and no other convention.** `Topocentric` returns
  `ra` in **sidereal hours** and `dec` in degrees (measured `astronomy.kt:1685-1703`) — a second
  hours/degrees boundary the implementer must not miss.
- **`Observer(latitude, longitude, height)`** — latitude degrees **N+ / S−**, longitude degrees
  **E+ / W−**, `height` **metres** above sea level (measured `astronomy.kt:1412-1431`). A spot stored
  as west-positive would silently mirror the sky.
- **Epoch / frame: J2000.0, ICRS-aligned.** The catalogue declares `epoch="J2000.0"` and positions are
  ICRS. No precession is applied to the stored numbers; the engine's `horizon()` performs the
  time-dependent transformation. The spec must state this once, because a catalogue whose epoch is
  unstated is unusable.
- **Magnitude system must be explicit.** `magSystem` says whether `magV` or `magB` is authoritative;
  `magKind` says whether it is integrated or peak. **Never mix V and B values in one comparison, and
  never mix an integrated magnitude with a peak magnitude.** The engine's own `magnitude()` at
  `astronomy.kt:778` and `IlluminationInfo.mag` at `:2897-2901` apply **only to solar-system bodies**
  (MEASURED) — a deep-sky object's brightness is a catalogue value and must not be asked of
  `illumination()`.

### 1.4 Lawful sourcing, and the independent check that a row is right

**The governing facts.** No permissively-licensed complete deep-sky catalogue exists (OpenNGC and HYG
are CC BY-SA 4.0; Hipparcos/Tycho and Gaia DR3 are CC BY-NC 3.0 IGO; Stellarium's is GPL; NGC 2000.0
is non-commercial; the Yale BSC5 carries **no SPDX licence**). Wikidata is CC0 but was **measured not
to be enumerable as a catalogue** (instance-of-Messier returned 0; the global NGC/IC counts returned
HTTP 504/429). Designations, coordinates and magnitudes are **facts**, so the lawful route is to
*author* a small table of facts and *verify* each row against independent references.

**The assembly procedure (exact, per row):**

1. **Designation and name** — from the historical catalogues themselves (Messier 1781; Dreyer's NGC
   1888 and IC 1895–1908). Those listings are public domain by age; no modern compilation is copied.
2. **Object type** — authored from the same classification, recorded in `notes` when a source is
   uncertain (e.g. an object reclassified from planetary nebula to H II region).
3. **Coordinates** — for each object, query **Wikidata** for `P6257` (RA, degrees) and `P6258`
   (Dec, degrees) (CC0 1.0). Coverage is measured to be **not uniform**: three tested objects carried
   `P6257`/`P6258`, but NGC 7000 carried a single coarse value and **IC 434 carried none**. Rows where
   Wikidata is missing or coarse are sourced by hand and recorded as such.
4. **Second, independent source for coordinates** — cross-check against **NASA HEASARC**
   (freely usable, US-government) and/or the primary discovery paper.
5. **Compute and record the discrepancy** — `verification.deltaArcmin` = angular separation between
   the two positions:
   Δ = (180/π)·acos( sin δ₁ sin δ₂ + cos δ₁ cos δ₂ cos(α₁−α₂) ), with α,δ in radians.
   **Acceptance: Δ ≤ 1.0 arcmin for objects with `sizeMajArcmin ≥ 5`, Δ ≤ 0.2 arcmin for unresolved
   objects.** A row outside tolerance is **re-sourced or dropped, never averaged.**
6. **Magnitudes** — take the published V (or B) magnitude and cross-check against a second reference.
   **When two sources disagree by more than 0.50 mag, do not average**: set `magDisputed=true`, store
   both in `notes`, and prefer the reference the entry names as primary. `magSystem`/`magKind` are
   filled from the same source, never inferred.
7. **Size and position angle** — from the same references; `sizeMajArcmin`/`sizeMinArcmin` recorded
   for the **optical** extent used by every other reference (state which, because RCs differ).

**The two reference families, with their licences as measured in the recon's own file (`I` §1–§3):**

| Reference | Licence (measured) | Use |
|---|---|---|
| **Wikidata structured data** (`P6257`/`P6258`/`P1215`) | **CC0 1.0** (Wikidata Licensing) | primary coordinate/magnitude source |
| **NASA HEASARC** | freely usable (US gov), acknowledgement requested | independent cross-check |
| **FK6** (GAVO) | **CC0 1.0** (SPDX `CC0-1.0`, read live this session) | bright-star positions + proper motions |
| **2MASS** (IRSA) | acknowledgement requested; no commercial prohibition found; **an explicit "public domain" sentence was not found** → `UNVERIFIED` | faint field only, with a J/H/K → V mapping |
| **IAU charts** (Alan MacRobert / Sky & Telescope) | **CC BY 4.0** ("Please reproduce the credit in full") | constellation figures/boundaries |
| **IAU WGSN star names** | **CC BY 4.0** | proper star names |
| **ESO `eso0932a`** (Milky Way panorama) | **CC BY 4.0**; credit **`ESO/S. Brunier` is MANDATORY and must be visible** | the backdrop |
| **NASA SVS** (CGI Moon Kit, Dial-A-Moon, Deep Star Maps 2020) | **public domain** (no insignia, no implied endorsement) | Moon disc, star-map backstop |
| **Solar System Scope** textures | **CC BY 4.0**, "even commercially" | planet discs |
| **NOAA/EOG VIIRS VNL** | **CC BY 4.0** (see §6) | optional darkness layer |

**Denylist (do not ship):** OpenNGC (CC BY-SA 4.0), HYG (CC BY-SA 4.0), Hipparcos/Tycho (CC BY-NC 3.0
IGO), Gaia DR3 (CC BY-NC), Stellarium (GPL), NGC 2000.0 (non-commercial),
`dcf21/constellation_lines_iau.dat` (**GPL v3 repo footer vs CC BY 4.0 file description — conflict**).

### 1.5 Which objects belong, as a concrete selection rule

The rule is arithmetic, not taste, and it produces the count:

- **Messier: all M1–M110 → 110 rows.** (M102 is disputed — possibly a duplicate observation of M101,
  possibly NGC 5866. The spec must state how that one row is resolved; shipping it as a documented
  duplicate/`notes` entry is the honest option.)
- **Brightest non-Messier NGC/IC: every NGC or IC object with a sourced `magV ≤ 10.0` that is not a
  Messier object → ~110 rows.** If fewer than 110 qualify, extend the limit to 10.5; if more, keep the
  brightest 110. This is a **rule over magnitudes**, never a copied list (which is why the Caldwell
  catalogue — a *compilation* by Patrick Moore — is not used as the selection).
- **Solar-system: 10 computed records** — Sun, Moon, Mercury, Venus, Mars, Jupiter, Saturn, Uranus,
  Neptune, and (optional) Pluto. Not authored.
- **Constellations: 88 rows in a separate table** (§1.8).

**Total:** 110 + ~110 authored deep-sky rows = **~220**, plus 10 computed solar-system records →
**~230**; the ~250 in the brief is reached by the extension rule above. The spec must state the exact
count it ships and the magnitude limit used, because "about 250" is not checkable.

### 1.6 How a planet is represented (computed, not authored)

A planet has no fixed coordinate, so **it is never a row in `objects.json`**. At the observation time
the app builds a `SolarSystemRecord`:

| Field | Source |
|---|---|
| `id` | `"planet-mars"`, `"moon"`, `"sun"` |
| `type` | `planet` \| `moon` \| `sun` |
| `raDeg`, `decDeg` | computed geocentric apparent RA/Dec at `t` |
| `azimuthDeg`, `altitudeDeg` | `horizon(t, observer, raHours, decDeg, Refraction.Normal)` |
| `magnitude` | **`illumination(body, t).mag`** — MEASURED to exist, `astronomy.kt:7251`, field at `:2901` |
| `phaseAngleDeg` | `illumination(body, t).phaseAngle` — degrees, MEASURED `:2913-2916` |
| `illuminatedFraction` | `illumination(body, t).phaseFraction` — **[0.0, 1.0]**, MEASURED `:2918-2920` |
| `ephemeris` | the literal `"computed"` — so the UI can distinguish a computed row from a catalogue row |

**Engine assignment (important):** Sun, Moon and planets come from **`astronomy.kt`** (VSOP87/ELP
series built in, **zero data files**), **not** from Orekit, because Orekit's planetary ephemeris
requires JPL DE kernel files (tens of MB). Orekit is used **only for satellites** (§3), where its
runtime data cost is a single 3,321-byte file. The spec must state that the two engines are not
interchangeable for a given body.

**Unsettled:** whether `astronomy.kt` also exposes an angular-diameter for a planet disc; the F-map
did not record one. If it does not, a planet's **apparent disc diameter** needs a small authored table
or is omitted from the framing math (§5.5).

### 1.7 How a constellation is represented

Constellations are **regions, not points**, and live in their own file:

`assets/catalog/constellations.json` → `{ id, nameEn, nameRu, nameLatin, abbrev, raCenterDeg,
decCenterDeg, areaRank }`, 88 rows.

- The **names and the three-letter IAU abbreviations are facts**; the IAU WGSN/IAU website terms are
  **CC BY 4.0**, so a credit line is required.
- **Boundary line segments** ship inside the **IAU CC BY 4.0 chart pack** (the "Constellation Boundary
  TXT" of the same pack). If boundaries are drawn, the derived geometry must carry the IAU credit
  (Alan MacRobert / Sky & Telescope) and CC BY 4.0.
- **Do not ship `dcf21/constellation_lines_iau.dat`**: its own description says CC BY 4.0 while the
  repository footer says GPL v3 — a measured conflict, so it is not a licence-clean asset.

### 1.8 What the specification must state

- The catalogue ships as **two authored JSON files under `assets/catalog/`** plus a **computed**
  solar-system section; the schema fields, types, units and the closed `type` enum are exactly §1.2.
- **RA is stored in degrees, `[0,360)`; declination in degrees, `[-90,+90]`; epoch is J2000.0 / ICRS.**
  Every engine call that takes RA takes it **in hours**, and the conversion happens at exactly one
  boundary in the code.
- **`magSystem` and `magKind` are mandatory** and no comparison ever mixes V with B or integrated
  with peak.
- The list is **110 Messier rows + every non-Messier NGC/IC with `magV ≤ 10.0` (extended to 10.5 only
  if needed)**, and the shipped count and limit are stated.
- Every authored row carries a `verification` object with **two independent sources and the computed
  `deltaArcmin`**, and a row outside the stated tolerance is re-sourced or dropped, **never averaged**.
- **No third-party compiled catalogue is copied**; the allowed references and their licences are
  exactly the table in §1.4, and the denylist items are absent.
- A planet/Moon/Sun is a **computed** record (`ephemeris:"computed"`), never an authored row, and it
  is computed by `astronomy.kt`, not Orekit.
- Constellations are a **separate 88-row table**; boundary geometry, if drawn, carries the IAU
  CC BY 4.0 credit.

---

## 2. Time and geometry

### 2.1 Time model

- **All computation is in UTC.** Orekit uses `AbsoluteDate`; `astronomy.kt` uses `Time`. No local
  time ever enters a computation.
- A spot is stored as `{ id, name, latDeg, lonDeg, elevationM, tzId }`, where **`tzId` is an IANA
  zone id** (`"Europe/Lisbon"`), **never a numeric UTC offset**, so DST is resolved by the platform's
  tz database and moves with Android's updates (`java.time.ZoneId`, available on API 26; the slot's
  `minSdk` is 26 per the build proof, so no desugaring is required for `java.time`).
- **The session's night is keyed to the local date at the spot, defined as the local-noon-to-local-noon
  window**, not midnight-to-midnight, so a sunset on day *N* and the sunrise on *N+1* are one session.

### 2.2 Twilight definitions (MEASURED — USNO)

Source: **<https://aa.usno.navy.mil/faq/RST_defs>**, "Rise, Set, and Twilight Definitions" (official
U.S. Navy site). Read verbatim this session:

| Term | Definition (source's own words) | Solar **geometric** altitude |
|---|---|---|
| Civil twilight | "when the center of the Sun is geometrically **6 degrees** below the horizon" | **−6.0°** |
| Nautical twilight | "when the center of the sun is geometrically **12 degrees** below the horizon" | **−12.0°** |
| Astronomical twilight | "when the center of the Sun is geometrically **18 degrees** below the horizon" | **−18.0°** |
| Sunrise / sunset | "geometric zenith distance of center of the Sun is **90.8333 degrees**" = the centre "**50 arcminutes** below a horizontal plane" = 16′ semidiameter + 34′ refraction | **−0.8333°** |

Two consequences the spec must carry:

- **Twilight is defined on the Sun's *centre*, *geometrically*** — i.e. **no refraction, no
  semidiameter** for the −6/−12/−18° boundaries. Source: "geometric zenith distance … 96, 102 and 108
  degrees" (USNO). So the twilight tests must call the engine with `Refraction.None` semantics — which
  `searchAltitude` already does: it passes `bodyRadiusAu = 0.0` and no dip/refraction
  (MEASURED `astronomy.kt:6911-6918`).
- **Sunrise/sunset is a different test** — it uses the 50′-below-horizon rule, which
  `searchRiseSet` implements (semidiameter + `horizonDipAngle` + `REFRACTION_NEAR_HORIZON` scaled by
  density; MEASURED `astronomy.kt:6818-6845`). The app must **not** use −18° for sunset or
  `searchRiseSet` for the dark window; using the wrong one shifts the dark window by minutes.

### 2.3 The dark-window algorithm, including the hard cases

**Definition.** The dark window is the set of intervals in which the Sun's **geometric** altitude is
**≤ −18.0°**.

**Primitives (all MEASURED in `astronomy.kt`):**
`searchAltitude(body, observer, direction, startTime, limitDays, altitude): Time?` (`:6911`) — finds a
centre-of-body crossing of an arbitrary altitude; `searchRiseSet(body, observer, direction, startTime,
limitDays, metersAboveGround = 0.0): Time?` (`:6818`) — finds a rise/set with refraction, dip and
body radius; `Direction.Rise` / `Direction.Set` (`:1586`).

**Algorithm.**

1. `sunset = searchRiseSet(Sun, obs, Set, localNoon, 1.0)`; `sunrise = searchRiseSet(Sun, obs, Rise, sunset, 1.0)`.
2. Sample the Sun's altitude over `[localNoon, localNoon+24h]` at **10-minute steps**; record
   `sunAltMinDeg`, `sunAltMaxDeg` and the times of the extrema.
3. Scan for every crossing of **−18.0°** with a **1-hour re-armed probe loop** (the same discipline
   as §3.5 — a single long call can step over a short window). Each crossing pair becomes one
   `[start, end)` interval.
4. Emit a **`darkWindows` array plus a `kind` discriminator** — this is the important design point,
   because at 48–55° latitude near the solstice a session can have **two** dark windows separated by
   a twilight dip, and a single-interval output cannot express it.

**The outputs the app must produce, per case:**

| Case | Condition (all MEASURED-able from steps 1–3) | Required output |
|---|---|---|
| **A. Normal** | one or more −18° crossing pairs found | `{ kind:"dark", darkWindows:[{startUtc,endUtc,durationMin}], sunAltMinDeg }` |
| **B. No astronomical darkness** | no −18° crossing, but `sunAltMinDeg < −12.0` | `{ kind:"no-astro-dark", bestTwi:"nautical", twilightWindows:[…−12° intervals…], sunAltMinDeg }`; if `sunAltMinDeg` is between −12 and −6 use `bestTwi:"civil"` with −6° intervals; if `sunAltMinDeg > −6.0` emit `{ kind:"no-dark", sunAltMinDeg }` |
| **C. Sun never sets (polar day)** | `searchRiseSet(Sun, Set, …)` returns `null` **and** `sunAltMinDeg > −0.8333°` | `{ kind:"polar-day", sunAltMinDeg, sunAltMaxDeg }` — **no window at all**, and the UI must say so in words, not show an empty list |
| **D. Sun never rises (polar night)** | `searchRiseSet(Sun, Rise, …)` returns `null` **and** `sunAltMaxDeg < −0.8333°` | if `sunAltMaxDeg ≤ −18.0`: `{ kind:"dark", darkWindows:[{startUtc = localNoon, endUtc = localNoon+24h}], wholeNightDark:true }`; if `−18.0 < sunAltMaxDeg < −0.8333`: `{ kind:"polar-night", darkWindows:[…], middayTwilightDip:{startUtc,endUtc} }` |
| **E. Night spans midnight** | normal | output is an **interval pair in UTC**, never a date; `dateLabel` = the **local date of `startUtc`**; the UI must render a session that begins before and ends after local midnight as one row |
| **F. Sunset and sunrise on different calendar dates** | identical to E | same as E; the spec must forbid deriving a duration from two formatted date strings |
| **G. Two dark windows in one night** | two −18° crossing pairs inside the 24 h window | `darkWindows` is an **array of length 2**, each with its own duration; the "best" window is the longest, and the UI must say "two windows tonight" |

**Universal rules:** the window is `[start, end)`; durations are computed in UTC; a `null` from an
engine call is a **case**, not an error, and must map to a `kind` — never to an empty list shown as
"no passes/no darkness tonight".

### 2.4 Moon

| Quantity | Definition | Exact engine call (MEASURED) |
|---|---|---|
| **Phase (elongation)** | Moon's ecliptic longitude minus the Sun's, **degrees in `[0,360)`**; 0° = new, 180° = full | `moonPhase(time): Double` — `astronomy.kt:6173`, body `pairLongitude(Moon, Sun, time)` |
| **Illuminated fraction** | fraction of the apparent disc lit, **`[0.0, 1.0]`** | `illumination(Body.Moon, time).phaseFraction` — `astronomy.kt:7251`, field `:2918-2920` |
| **Phase angle** | Sun–Moon–observer, **degrees** | `illumination(...).phaseAngle` — `:2913-2916` |
| **Moon magnitude** | `illumination(...).mag` — `:2901` |
| **Moon altitude/azimuth** | — | `horizon(t, obs, moonEqu.ra, moonEqu.dec, Refraction.Normal)` → `Topocentric` |

- **Equivalent closed form** the implementation may assert in a unit test:
  `k = (1 + cos(i)) / 2`, with `i` = `phaseAngle` in degrees.
- **Phase name** (8 bins): map the elongation to `New, Waxing Crescent, First Quarter, Waxing
  Gibbous, Full, Waning Gibbous, Last Quarter, Waning Crescent` with 45° bins offset by 22.5° around
  each exact phase.
- **"Moonless" as a number.** Two independent booleans and one stated threshold:
  `moonUp = (moonAltDeg > 0.0)`;
  `darkMoon = (illuminatedFraction <= 0.10)` ← **a product threshold, stated as such**;
  `session.moonless = (!moonUp) || darkMoon`.
  The UI must show **both** numbers (fraction and altitude), because a 5 % crescent 10° above the
  horizon is *not* moonless in practice, and a single boolean would hide that.
- **The Moon's contribution to sky brightness: not a number in v1.** If it is ever added, the only
  licence-clean route is to **implement the Krisciunas & Schaefer (1991) model** (implementing a
  published method is not redistributing a paper's data), and it must be labelled a **model**, not a
  measurement. Its numerical accuracy is `UNVERIFIED` — no reference implementation exists to compare
  against — so it must never be presented as a measured sky brightness.

### 2.5 Target altitude / azimuth / refraction / circumpolar

- **Reference frame and the one call that produces each quantity:**
  `horizon(time, observer, ra, dec, refraction) → Topocentric{azimuth, altitude, ra, dec}`
  (MEASURED `:5400` signature, `:1685-1703` fields). `azimuth` is **0 = North, 90 = East**; `altitude`
  is **degrees above (+) or below (−) the horizon**; returned `ra` is in **sidereal hours**.
- **Refraction — applied or not, and where.** `astronomy.kt` ships a `Refraction` enum with three
  values (MEASURED `:1563-1586`):
  `None` (airless), `Normal` (recommended), `JplHor` (JPL-Horizons compatibility only).
  The **`Normal` model is Saemundsson's**, read verbatim from the source:
  `angle_deg = (1.02 / tan(h + 10.3/(h + 5.11))) / 60.0`, with `h = max(altitude, −1.0°)` and, for
  `altitude < −1.0°`, multiplied by `(altitude + 90.0) / 89.0` so refraction tapers to zero at the
  nadir (MEASURED `astronomy.kt:4015-4031`). `JplHor` clamps to −1° without the taper. The constant
  `REFRACTION_NEAR_HORIZON = 34.0 / 60.0` degrees (`:196`) matches USNO's 34′ exactly.
  **Contract:** use **`Refraction.Normal`** for a target's displayed altitude and for the user's
  altitude mask; use **no refraction** for the −6/−12/−18° twilight tests (§2.2). The toggle must be
  per-use, not global.
- **"Up" for a target.** A target is *above the mask* when
  `altitudeDeg(Refraction.Normal) ≥ maskDeg` (default mask **10.0°**, user-adjustable).
- **Rise / set / transit / circumpolar** — classify by sampling, then refine:
  sample `altitude(t)` at **10-minute** steps across the session window and compute `min`, `max`.
  - `min > maskDeg` → **`alwaysUp` (circumpolar)**: no rise/set; the target is usable all session.
  - `max < maskDeg` → **`neverUp`**: the target does not reach the mask; the UI must say so, not
    render an empty window.
  - otherwise → find boundaries with
    `searchRiseSet(starOrBody, obs, Rise|Set, startTime, limitDays, metersAboveGround)`
    (MEASURED `:6818`; it accounts for dip, refraction and body radius), then **refine a crossing to
    the mask** with `searchAltitude(starOrBody, obs, Rise|Set, … , maskDeg)` (`:6911`) — because
    `searchRiseSet` crosses the *horizon*, not the user's mask.
  - **Transit** = the time of maximum altitude, found by the 10-minute scan refined to **1 minute**.
    (`astronomy.kt` may expose `searchHourAngle`; presence was **not** checked this session →
    `UNVERIFIED`. If it exists, it may replace the scan; until then, the scan is the contract.)
- **`defineStar` path is *present but not exercised*.** `defineStar(body, ra, dec,
  distanceLightYears)` (`:452`) plus `searchAltitude`/`searchRiseSet`/`horizon`, whose docs name a
  `defineStar` user-defined star (`:6343`, `:6783`, `:6883`), is exactly the "when is M31 above 30°
  tonight" path — but **no `defineStar` call appears in the recon's corpus runner**, so it is
  `UNVERIFIED` end-to-end (§7).

### 2.6 Timezones, DST and how a session's times are displayed

- **Display zone:** the **spot's** `tzId` by default, with an explicit toggle to the device zone,
  shown as a badge ("device time"). If a spot has no `tzId`, fall back to the device zone and label it.
- **DST:** because all math is UTC, a DST transition changes **only the label**; the spec must state
  that a displayed clock time may jump ±1 h across a transition while the session duration does not,
  and that durations and intervals are computed in UTC and never from formatted strings.
- **Localisation:** times and dates are formatted with `DateTimeFormatter` with an explicit locale
  (`Locale.forLanguageTag(locale)`), because the slot is born multilingual (the factory's
  localisation rule) and a hardcoded `HH:mm` pattern is a defect.
- **The session's `dateLabel`** is the local date of the UTC start instant, converted with the spot's
  `ZoneId` — the one place a UTC instant becomes a local date.

### 2.7 What the specification must state

- **All computation is UTC**; a spot stores an IANA `tzId`, never a numeric offset; the night is the
  spot's local-noon-to-local-noon window.
- Twilight boundaries are **geometric Sun-centre altitudes −6.0° / −12.0° / −18.0°** (USNO), and
  sunrise/sunset is **−0.8333°** (90.8333° zenith = 50′ below the horizon) — two different tests that
  must not be interchanged.
- The dark-window output is a **`darkWindows` array plus a `kind` discriminator**, with the exact
  outputs for normal, no-astronomical-darkness, polar day, polar night, midnight-spanning,
  cross-date and two-window cases as tabulated in §2.3.
- Moon phase is the **elongation in degrees `[0,360)`** (`moonPhase`) and the **illuminated fraction
  `[0,1]`** (`illumination().phaseFraction`); `k = (1 + cos(phaseAngle))/2`; "moonless" is
  `(!moonUp) || (illuminatedFraction <= 0.10)` with the threshold stated as a product choice and both
  numbers always shown.
- A target's altitude/azimuth comes from `horizon(...)`, with **`Refraction.Normal` (Saemundsson)**
  for the display and the mask and **no refraction** for the twilight tests; circumpolarity is
  decided by sampling (`min`/`max` vs mask), not by a subtraction of rise and set.
- A `null`/absent crossing is a **case with a `kind`**, never an empty list presented as a result.

---

## 3. Satellite passes

### 3.1 Element source and its terms

**Endpoint (MEASURED):**
`GET https://celestrak.org/NORAD/elements/gp.php?GROUP=<GROUP>&FORMAT=JSON`
→ HTTP 200, 9,270 B for `GROUP=STATIONS`, 17 OMM records.

**The terms, read verbatim from <https://celestrak.org/usage-policy.php> (HTTP 200, 13,980 B,
"Last updated: 2026 May 22"):**

- "**Only download the data you need, when you are going to use it, and only download data once per
  update. For GP data, updates are once every 2 hours.**"
- "**M2M (machine-to-machine) software should immediately stop querying when it receives any
  non-HTTP 200 responses and report the results to a human for investigation.** Repeatedly ignoring
  them will end up sending your IP address to the firewall."
- "HTTP 301 … the URL is being redirected … HTTP 404s are also signs of using old URLs … HTTP 50x
  responses are server errors … Queries need to stop immediately."
- "**Users need to update their software to use the CSV formats**" — and, from the same site banner,
  the 5-digit catalog-number space was **exhausted on 2026-07-11**: "All newly cataloged objects have
  6-digit catalog numbers of 100000+ and **GP data will not be available for them using the TLE
  format**."
- From <https://celestrak.org/NORAD/documentation/gp-data-formats.php> (HTTP 200): GP is distributed as
  **OMM** (CCSDS 502.0-B-3), with **JSON and CSV "using the same keywords and definitions as provided
  in the OMM standard (… Table 4-1)"**; redundant mandatory fields
  (`CENTER_NAME=EARTH`, `REF_FRAME=TEME`, `TIME_SYSTEM=UTC`, `MEAN_ELEMENT_THEORY=SGP4`) are omitted.
  FAQ: "CelesTrak only checks for new GP data once every 2 hours."

**Licence status — honest.** The policy page is a **bandwidth policy**, not a licence. It states the
data are "freely available to all users" and it **neither forbids commercial use nor grants one**;
**no SPDX licence statement was found on the pages read**. → ON RECORD (the policy text) +
**UNVERIFIED** whether a **paid** app is affirmatively licensed.

- **The check that settles it:** read the footer/terms of `https://celestrak.org/` and
  `gp-data-formats.php` for a licence statement and any "commercial" clause; if absent, ask
  **TS.Kelso@celestrak.org** (the address the policy itself publishes).
- **Why CelesTrak anyway:** the alternative (Space-Track.org) requires a **free account**, and account
  credentials cannot live in a client (`client-secrets.mdc`), so it would force a factory-written
  server. CelesTrak is the **only keyless route**, so the recommendation is: use it, record the
  decision, and keep the fetch polite (§3.6).

**Format: OMM JSON.** Exact keys measured on a live `GROUP=STATIONS` payload:

```
OBJECT_NAME, OBJECT_ID, EPOCH, MEAN_MOTION, ECCENTRICITY, INCLINATION,
RA_OF_ASC_NODE, ARG_OF_PERICENTER, MEAN_ANOMALY, EPHEMERIS_TYPE,
CLASSIFICATION_TYPE, NORAD_CAT_ID, ELEMENT_SET_NO, REV_AT_EPOCH,
BSTAR, MEAN_MOTION_DOT, MEAN_MOTION_DDOT
```

Example (ISS): `EPOCH=2026-10-08T00:21:12.026592`, `MEAN_MOTION=15.48769982`,
`ECCENTRICITY=0.00068037`, `INCLINATION=51.6313`, `BSTAR=9.651885e-5`, `MEAN_MOTION_DOT=4.83e-5`.

**Groups to ship:** a curated, small set — `STATIONS` and `VISUAL` (the 100 brightest), plus a
`CATNR=<noradId>` fetch for a user-named satellite. Fetching the full `ACTIVE` list (tens of
thousands) is explicitly the traffic the policy asks users to avoid.

### 3.2 OMM → Orekit `TLE`: the unit contract (MEASURED javadoc)

**Which path is primary, settled 2026-10-08 (MEASURED).** Orekit ships its own CCSDS/OMM support, so
this field constructor is the **fallback**, not the plan. `javap -public` over `orekit-13.1.9.jar`
shows `org.orekit.files.ccsds.ndm.odm.omm.OmmParser` with `public Omm build()`, and `Omm` exposing
`generateTLE()`, `generateKeplerianOrbit()` and `generateSpacecraftState()`. The primary path is
therefore to parse with `OmmParser` and ask `Omm.generateTLE()`, which hands back a `TLE` built from
the parsed OMM fields. The constructor below is confirmed present on the same revision and is used
only for a record the parser refuses. (The section heading keeps its original wording so the record of
what was read when is not rewritten; only the role of the constructor changed.)

The Orekit field constructor (originally read from the 13.1.5 javadoc, and re-confirmed to exist on
13.1.9 by `javap`) is:

```
TLE(int satelliteNumber, char classification, int launchYear, int launchNumber, String launchPiece,
    int ephemerisType, int elementNumber, AbsoluteDate epoch,
    double meanMotion, double meanMotionFirstDerivative, double meanMotionSecondDerivative,
    double e, double i, double pa, double raan, double meanAnomaly,
    int revolutionNumberAtEpoch, double bStar)
```

with the javadoc's own units and range checks:

| Parameter | Unit (javadoc, verbatim) | Range check |
|---|---|---|
| `meanMotion` | **rad/s** | `≥ 0` |
| `meanMotionFirstDerivative` | **rad/s²** | — |
| `meanMotionSecondDerivative` | **rad/s³** | — |
| `e` | dimensionless | `0 ≤ e ≤ 1` |
| `i` | **rad** | `0 ≤ i ≤ π` |
| `pa` (argument of perigee) | **rad** | `0 ≤ pa ≤ 2π` |
| `raan` | **rad** | `0 ≤ raan ≤ 2π` |
| `meanAnomaly` | **rad** | `0 ≤ meanAnomaly ≤ 2π` |

**Therefore the OMM→Orekit conversion is exactly (OMM units are degrees and rev/day):**

```
i_rad    = INCLINATION        × π / 180
raan_rad = RA_OF_ASC_NODE     × π / 180
pa_rad   = ARG_OF_PERICENTER  × π / 180
ma_rad   = MEAN_ANOMALY       × π / 180
n_radps  = MEAN_MOTION        × 2π / 86400        # rev/day  → rad/s
nDot     = MEAN_MOTION_DOT    × 2π / 86400²       # rev/day² → rad/s²
nDDot    = MEAN_MOTION_DDOT   × 2π / 86400³       # rev/day³ → rad/s³
epoch    = AbsoluteDate(EPOCH, TimeScalesFactory.getUTC())
```

- `meanMotion` **is in rad/s, not rev/day** — passing `15.48769982` straight through yields a
  satellite that falls out of the sky. This single conversion is the most likely silent defect in the
  satellite path and the spec must state it as a formula, not a hint.
- Orekit **normalises** negative `meanAnomaly`/`raan`/`pa` into `[0, 2π)` and then range-checks them,
  so the converter must wrap, not clamp.
- `classification` = `CLASSIFICATION_TYPE[0]` (`"U"`); `launchYear`/`launchNumber`/`launchPiece` are
  parsed from `OBJECT_ID` (`"1998-067A"` → 1998, 67, `"A"`); `satelliteNumber` = `NORAD_CAT_ID`;
  `elementNumber` = `ELEMENT_SET_NO`; `revolutionNumberAtEpoch` = `REV_AT_EPOCH`.
- The two-line constructor `TLE(line1, line2)` exists (and `TLE.isFormatOK` must precede it), but it is
  **not usable** for the growing set of 6-digit catalog numbers (measured policy), so the **field
  constructor is the required route**.

### 3.3 Propagation, frames and the runtime data requirement

- `TLEPropagator.selectExtrapolator(tle)` → PV in **TEME**; then
  `FramesFactory.getTEME()` → `getITRF(IERSConventions.IERS_2010, simpleEOP = true)` →
  `TopocentricFrame`. Measured end-to-end sample:
  `TEME_AT_T+600s pos_km=(1280.749619, 5592.447086, 3605.982869)`,
  `ITRF_OK simpleEOP=true name=CIO/2010-based ITRF simple EOP`,
  `TOPO_AT_T+600s elevation_deg=−44.136279 azimuth_deg=354.524835`.
- **The app must bundle Orekit runtime data or it cannot run.** Measured: with no data provider
  Orekit throws **`no IERS UTC-TAI history data loaded`**; with **3,321 bytes** (a single
  `tai-utc.dat`) the whole chain runs. `simpleEOP=true` removes the need for `finals.all`.
  A malformed `itrf-versions.conf` also breaks ITRF ("The first column … is a plain prefix") — so the
  bundled file must use plain prefixes (`eopc04_05`, not `eopc04_05\.\d\d`).
- **Contract:** ship `assets/orekit-data/tai-utc.dat` (and only what the chain needs) — nowhere near
  the 11.56 MB full data set.

### 3.4 The visible-pass definition

A pass is **reported** only when **all** of these hold:

1. **Elevation:** `maxElevationDeg ≥ maskDeg`, default **maskDeg = 10.0°**, user-adjustable 0–45°.
   (The recon's cross-check used `mask_deg=10.0` and measured `max_elevation_deg=89.067`.)
2. **The satellite is sunlit:** it is not inside the Earth's umbra. Geometry: Sun–satellite–Earth
   occultation on the WGS84 ellipsoid. Orekit's `EclipseDetector` with `OneAxisEllipsoid` /
   `OccultationEngine` is the available primitive (MEASURED as present in the F-map: "the engine
   pieces exist (`sunPosition`, Orekit `EclipseDetector`/`OneAxisEllipsoid`)"), **but the visibility
   *policy* was unexercised** → the implementation of this test is `UNVERIFIED` until run.
3. **The observer is dark:** the Sun's **geometric** altitude at the pass instant
   `≤ −6.0°` (civil) — the app must state which threshold it used (`−6°` "visible in twilight" versus
   `−18°` "fully dark"). Prescribe **−6.0° as the default** and expose the choice.
4. **Geometry:** the pass's AOS/LOS pair comes from the elevation detector at the observer's
   `TopocentricFrame`.

**Brightness — the honest position.** CelesTrak's GP/OMM payload carries **no standard magnitude**
(measured: the key list has `BSTAR`, `MEAN_MOTION_DOT`, `MEAN_MOTION_DDOT` and **no magnitude field**).
Therefore:

- The app **must not compute a magnitude from `BSTAR`** — a ballistic coefficient is a drag proxy,
  not photometry.
- With no authored table, the pass row shows **no brightness claim**; the spec must say so rather
  than invent a number. A "bright/faint" label is admissible **only** with a curated per-satellite
  magnitude table recorded as facts (the same discipline as §1.4).
- Label: absolute brightness without a curated table = `UNVERIFIED`.

**Merging:** all fetched groups are merged into one list, de-duplicated by `NORAD_CAT_ID` (the same
object can appear in `STATIONS` and `VISUAL`), sorted by AOS.

### 3.5 The segmented propagation — the detector trap and the exact fix

**The trap (MEASURED).** A single long propagation with an `ElevationDetector` does not find the
passes:

```
DETECTOR  events=2  aos=1        # one propagate() over 3 days
BRUTE     samples=4321 aos=15 max_elevation_deg=89.067 window_days=3.0 mask_deg=10.0
```

The reference truth for that window is **15 passes in 3 days**, and the one-call detector reports
**1**. The root finder steps over acquisitions when the step is long.

**The fix (MEASURED).** Segment the propagation:

```
segmented-1h maxCheck=60   events=30   aos=15
```

which agrees **exactly** with the independent 60-second brute-force elevation scan.

**Precise enough to implement:**

1. Window = the session's dark window (§2.3), capped at 24 h, expressed as
   `[t0, t1]` in UTC.
2. Segment length = **3600 s**. Iterate `t = t0; while (t < t1) { tNext = min(t + 3600 s, t1); … }`.
3. For **each** segment, build a **fresh** `ElevationDetector` with
   `maxCheck = 60.0 s` and the default threshold (`1e-6`), attach it to a propagator **started at
   `t`**, and propagate to `tNext`. The re-arming is the point: a detector that has already fired is
   not re-armed inside one call.
4. Collect both directions — the acquisition (`g` increasing) and the loss (`g` decreasing) — and
   pair them into passes; keep a pass only if its `maxElevationDeg ≥ maskDeg`.
5. A one-hour segment is short enough that no pass is missed **and** long enough that the whole night
   costs ≤ 24 propagations. Do not fall back to a 60-second scan at runtime (4,321 samples per 3 days
   is the *check*, not the algorithm).

**A note the F-map recorded rather than hid:** one probe row labelled `segmented-10m` actually passed
`segmented=false`, i.e. it was another one-call run — so the label alone is not evidence. The
implementation must have a **test that asserts the segmented finder returns the same AOS count as a
brute-force scan**, or the trap silently returns.

### 3.6 Absent, stale or offline data, and the staleness threshold

**Absent** (a fetch fails, or a requested satellite has no element set):
the satellite is **omitted** and the section shows **"satellite data unavailable"** — never an empty
pass list rendered as "no passes tonight", which is a claim the app cannot support.

**Stale.** Compare the element set's `EPOCH` with the session instant:

| TLE age | Behaviour |
|---|---|
| `≤ 24 h` | normal, no badge |
| `24 h < age ≤ 72 h` | normal |
| `72 h < age ≤ 168 h (7 d)` | compute, but show **"element set is N days old — pass times may shift by minutes"** |
| `> 168 h (7 d)` | **refuse to compute passes**; show "data too old", offer refresh |

**These thresholds are chosen product values, not measured constants**, and the spec must say so. They
are grounded on two measured facts: the app fetches at use time, so a normal age is **hours** (two
CelesTrak groups fetched this session carried same-day epochs, `2026-10-08T00:21:12` and
`2026-10-08T08:02:18`) and the policy itself says the data only change **every 2 hours**; and the same
propagator's error reaches **27,924 km at ~21.3 days** (§3.7), so a week is already far past
usefulness while a day is comfortably inside it. The UI must name the threshold it applied.

**Offline.** No fetch. Use the last cached element sets and apply the **same age rules** — a cache hit
is never treated as fresh. Cache record: `{ noradId, group, epochUtc, fetchedAtUtc, payload }`. If the
cache is empty, the section is unavailable (`UNVERIFIED`), stated as such.

**Politeness, in code (from the measured policy).** Store `fetchedAtUtc` **per group** and **do not
re-fetch a group within 2 hours**; on **any non-200**, stop the fetch loop and log — never retry.
No request may be scheduled on the hour (best practice for `api.met.no`, and the same courtesy
applies here).

### 3.7 The accuracy the app may claim

Two different numbers exist and they must never be confused:

- **Propagator fidelity (MEASURED).** Orekit's SGP4 reproduces the reference implementation
  (Vallado's `TCPPVER`) to **`pos_tol = 1.0e-4 km` (0.1 m)** and **`vel_tol = 1.0e-7 km/s`**:
  **29 of 33 corpus blocks fully within tolerance**, worst in-tolerance error `1.97e-6 km`; the
  near-range block `20413#1` is **26/26 within**. This is agreement with a **reference implementation**,
  not with the sky.
- **Prediction error (MEASURED bound).** The same propagator diverges **27,924.0197880 km**
  (velocity error `1.99701767930 km/s`) at **+1,844,000 s ≈ 21.3 days** past epoch for object 20413
  (`CASE 20413#2 vectors=70 within=1`).

**What the app may say in the UI:**
"Pass times are computed from NORAD GP element sets. A fresh element set (hours old) is accurate to
within a few minutes; the age of the set is shown, and passes older than 7 days are not shown."

**What it may not say:** that a pass time is accurate to 0.1 m, or to a stated number of seconds.
The numeric bound for the "few minutes" claim is `UNVERIFIED`;
**the check that settles it** is a same-night comparison of the app's computed AOS against an
independently observed or third-party-predicted AOS for the same site and satellite (e.g. NASA's
Spot-the-Station for the ISS), recorded with the element-set epoch used.

### 3.8 What the specification must state

- The element source is **CelesTrak GP** at the exact `gp.php` URL, in **OMM JSON**, fetched
  **once per update** with the **2-hour** cache rule and **stop-on-non-200**; **no affirmative
  commercial licence was found** and the decision to use it is recorded (with the settling check).
- The **OMM→Orekit conversion formulas** of §3.2 are stated literally, including
  `n = MEAN_MOTION × 2π / 86400` (rad/s), and the `[0, 2π)` normalisation requirement.
- The app bundles `assets/orekit-data/tai-utc.dat` (**3,321 bytes**) because Orekit refuses to run
  without an IERS UTC-TAI provider; `simpleEOP = true`; the `itrf-versions.conf` prefixes are plain.
- A pass is reported only when **elevation ≥ mask (default 10.0°)**, the **satellite is sunlit**
  (umbra test) **and the observer's Sun altitude ≤ −6.0°** (stated which threshold was used).
- **No brightness is claimed** unless a curated magnitude table exists; **`BSTAR` is never converted
  to a magnitude**; CelesTrak's OMM carries no magnitude field.
- The pass finder **segments the propagation into 3600 s windows with `maxCheck = 60 s` and a freshly
  built detector per segment**, and a test asserts its AOS count equals a brute-force 60-second scan
  (the trap: one call gave `aos=1` where the truth is `aos=15`).
- The **staleness thresholds are 24 h / 72 h / 168 h**, stated as product choices; **> 168 h refuses to
  compute**; absent data shows "unavailable", never "no passes"; offline uses the cache **with the same
  age rules**.
- The only accuracy claim made is the **0.1 m agreement of the propagator with the reference
  implementation**, explicitly labelled as fidelity and **not** as prediction accuracy; the app's own
  pass-time accuracy claim stays `UNVERIFIED` pending a same-night AOS comparison.

---

## 4. Cloud forecast

### 4.1 Endpoint

```
GET https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=<lat>&lon=<lon>[&altitude=<m>]
```

- **Coordinates must be truncated to a maximum of 4 decimals.** The Terms of Service state verbatim:
  "When using requests with latitude/longitude, **truncate all coordinates to max 4 decimals** … For
  new products, **requests with 5+ decimals will return a 403 Forbidden**." (`lat=59.91387` is
  invalid; `lat=59.9138` is valid.)
- A recon payload of this exact endpoint was captured: `metno-compact-UA.json`, **38,302 bytes**.

### 4.2 Required custom headers

Read verbatim from the MET Norway **Terms of Service** (HTTP 200, 14,446 B):

- **`User-Agent`** — "**All requests must (if possible) include an identifying User Agent-string (UA)
  in the request with the application/domain name, optionally version number. You should also include a
  company email address or a link to the company website** … If we cannot contact you in case of
  problems, you risk being blocked without warning."
  Service's own examples: `"acmeweathersite.com support@acmeweathersite.com"` and
  `"AcmeWeatherApp/0.9 github.com/acmeweatherapp"`.
  **Contract value:** `User-Agent: Lumintra/<versionName> (v.sobolenko.vs@gmail.com)` — the developer
  account's public address, defined once in `scripts/store/model.py` (`DEFAULT_CONTACT_EMAIL`), not a
  secret.
- **`If-Modified-Since`** — "**Cache data locally and use the `If-Modified-Since` request header to
  avoid repeatedly downloading the same data.**"
- **Honour `Expires`** — "don't repeat requests until the time indicated in the `Expires` response
  header."
- **Do not use `HEAD` followed by `GET`** — "since this doubles the server processing load if it
  misses the cache."
- **Do not schedule on the hour** — "Don't schedule many requests at the same time, e.g. every hour on
  the dot … Add a random number of minutes to the time of the requests."
- **Mobile devices must not poll in the background** — "Applications on mobile devices must not
  retrieve new data as long as the application is not in use."
- Bandwidth ceiling: "Anything over **20 requests/second per application** (total, not per client)
  requires special agreement."
- A **203** response means the product version is deprecated — "this should be logged and/or shown as
  a warning" (versions are terminated after ~1 month).

### 4.3 The field to read (MEASURED against a live payload)

Response is GeoJSON `type = "Feature"`.

- `properties.meta.units.cloud_area_fraction = "%"` (also available: `air_temperature`,
  `wind_speed`, `relative_humidity`, `air_pressure_at_sea_level`, `precipitation_amount`).
- `properties.timeseries[]` — an array of hourly points (**87** measured in one response). Each point:
  - `.time` — ISO-8601 UTC, e.g. `"2026-10-08T19:00:00Z"`;
  - `.data.instant.details.cloud_area_fraction` — a number **0–100**, in **per cent** (measured value
    `100.0`).
- `next_1_hours.summary` carries a symbol code; `next_1_hours.details` carries the aggregated fields.
- **Only TOTAL cloud cover is available** — there is **no low/mid/high split**. The UI must therefore
  say **"total cloud cover"** and must never render the value as "clear sky" or as a seeing estimate.

### 4.4 Cache policy the terms demand

Store per spot: `{ lat4, lon4, fetchedAtUtc, expiresUtc, lastModified, etag, payload }`, keyed by the
**truncated** coordinates (two spots 100 m apart are the same request). Re-request only after
`expiresUtc`; send `If-Modified-Since` with `lastModified`; add a random offset of minutes to the
scheduled refresh time; never refresh while the app is not in use.

### 4.5 Offline behaviour

- No network → show the **cached** forecast with its **age** (`fetchedAtUtc`) and an explicit
  **"offline — showing data from <local time>"** label. Never fabricate a value, and never present a
  stale forecast as current.
- No cache at all → the cloud row is **unavailable** (`UNVERIFIED`), stated in words.
- The app must still function fully: the dark window, the Moon and the targets are all local
  computations, so a network failure degrades exactly one panel.

### 4.6 Mandatory attribution, and where it must appear

Read verbatim: "**All open data require attribution as specified in the CC BY 4.0 license: You must
give appropriate credit, provide a link to the license, and indicate if changes were made.**"

- **Required string:** `Weather data from MET Norway (api.met.no), licensed CC BY 4.0` — with a
  **link to the licence** (`https://www.met.no/en/free-meteorological-data/Licensing-and-credits`) and
  an "indicate if changes were made" note (the app shows the data unmodified, so state
  "shown unmodified").
- **Where it must appear:** (a) as a **visible footnote on the screen that shows the forecast** — not
  only in a menu — because the licence requires the credit to travel with the data; and (b) in the
  app's **About/Credits** screen.
- **Naming restriction (measured):** "You are not allowed to make services/sites that appear to be made
  by Yr, NRK or The Norwegian Meteorological Institute … you may not use the word `Yr` as a part of
  your service name, or attach the Yr logo." → the app must not name or brand itself with `Yr`, and
  must not imply endorsement.

### 4.7 What the specification must state

- The endpoint is `api.met.no …/locationforecast/2.0/compact`, with coordinates **truncated to 4
  decimals** (5+ → 403).
- The request carries **`User-Agent: Lumintra/<versionName> (v.sobolenko.vs@gmail.com)`** and
  **`If-Modified-Since`**, honours **`Expires`**, never uses `HEAD`+`GET`, and never refreshes while
  the app is not in use.
- The field read is **`properties.timeseries[].data.instant.details.cloud_area_fraction`**, a
  **percentage 0–100**, and the UI says **"total cloud cover"** (no low/mid/high split exists).
- The cache is keyed by truncated coordinates and records `fetchedAtUtc`/`expiresUtc`/`lastModified`.
- Offline shows the cached value **with its age and an "offline" label**, or "unavailable" — never a
  fabricated or silently stale value.
- The attribution **`Weather data from MET Norway (api.met.no), licensed CC BY 4.0`** appears
  **visibly beside the forecast** and in Credits, with a licence link and the "modified?" statement,
  and the app uses neither `Yr` nor any implication of endorsement.

---

## 5. Field of view

### 5.1 The math, exactly

For a rectilinear (normal, non-fisheye) lens, the angular field on one axis is

```
FOV_axis_deg = 2 · atan( d_axis / (2 · f) )
```

where `d_axis` is the sensor's extent on that axis in **millimetres** and `f` the focal length in
**millimetres**. Applied per axis and to the diagonal:

```
FOV_h = 2·atan(w / (2f))          w = sensor width  (mm)
FOV_v = 2·atan(h / (2f))          h = sensor height (mm)
FOV_d = 2·atan(√(w²+h²) / (2f))
```

Sanity check (arithmetic, INFERRED from the formula): 50 mm on full frame (36×24 mm) →
`2·atan(36/100) = 39.6°` horizontally; 14 mm → `2·atan(43.2666/28) = 114.3°` diagonally.

**The device camera's own frame.** Android exposes the two inputs, and both were **MEASURED present in
`android.jar` (android-35) by `javap`** with exactly these types:

| Constant | Type (measured) | Unit |
|---|---|---|
| `CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE` | `Key<android.util.SizeF>` | mm (physical sensor extent) |
| `CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS` | `Key<float[]>` | mm |
| `CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE` | `Key<android.graphics.Rect>` | pixels (active area) |

**Honest limit:** `SENSOR_INFO_PHYSICAL_SIZE` is the **full** sensor, while the delivered frame may use
a smaller area, so the FOV computed from it is an **upper bound** for the device camera. The spec must
say so and must not present the device FOV as exact.

**35 mm-equivalent focal length** (for the presets and for the user's own lens):
`f_equiv = f · (43.2666 / d)` where `d = √(w²+h²)` and `43.2666 mm` is the full-frame diagonal
(`√(36²+24²)`). Crop factor = `43.2666 / d`.

### 5.2 Lens presets to ship

Ship as a JSON asset with the FOV values **computed by the formula above, not typed in by hand**
(same discipline as the catalogue: a hand-typed table drifts from the formula).

| Preset | Sensor | Focal length | FOV_h | FOV_v | FOV_diag |
|---|---|---|---:|---:|---:|
| Full-frame fisheye | 36×24 | 8 mm | 132.1° | 112.6° | 139.4° |
| Ultra-wide | 36×24 | 14 mm | 104.3° | 81.2° | 114.3° |
| Wide | 36×24 | 20 mm | 84.0° | 61.9° | 94.5° |
| Wide | 36×24 | 24 mm | 73.7° | 53.1° | 84.1° |
| Standard | 36×24 | 35 mm | 54.4° | 37.8° | 63.4° |
| Standard | 36×24 | 50 mm | 39.6° | 27.0° | 46.8° |
| Portrait | 36×24 | 85 mm | 23.9° | 16.1° | 28.6° |
| Tele | 36×24 | 135 mm | 15.2° | 10.2° | 18.2° |
| APS-C (crop 1.5) | 23.5×15.6 | 18 mm | 66.3° | 46.9° | 76.4° |
| Micro 4/3 (crop 2.0) | 17.3×13.0 | 12 mm | 71.6° | 56.9° | 84.4° |

(The table's numbers are the formula's own outputs for the stated sensor and focal length —
INFERRED from §5.1 — and the implementation must generate them, so a wrong entry is impossible.)

### 5.3 The relationship between the on-scene frame and the real frame

- **Prescribed projection first, because the frame math is only exact in some projections.**
  Use an **equidistant** mapping (angle proportional to distance from the scene's aim point) for the
  sky scene; then the frame overlay is a **true 1:1 angular rectangle**:
  `pxPerDeg = canvasWidthPx / sceneFovHorizDeg`, and a lens frame of horizontal field `F_h` occupies
  `F_h · pxPerDeg` pixels wide.
- **If any other projection is used** (stereographic, equal-area, the "photo-real" look), the frame
  overlay **must be computed by projecting its four corners through the same projection** and drawing
  the resulting quad. Scaling a rectangle by a pixel ratio is only correct in the equidistant case and
  is a defect in every other one — hence the rule.
- **Rotation.** The frame is centred on the scene's aim point and rotated to the camera's roll relative
  to the horizon. Offer (a) horizon-aligned landscape, (b) horizon-aligned portrait, (c) free roll
  (the photographer's deliberate tilt). State which mode produced any exported frame.
- **Multi-frame framing.** For a mosaic/panorama the app may draw an N×M grid of frames at the same
  angular scale; each tile's overlap is a fraction the user sets, and the geometry is the same
  rectangle repeated by angular offsets.

### 5.4 How a target's apparent size is compared against the frame

```
frameFrac      = objectAngularSizeDeg / frameFovShortSideDeg     # fraction of the frame's short side
pixelsOnSensor = objectAngularSizeDeg · (imageWidthPx / fovHorizDeg)
```

- For an extended object, `objectAngularSizeDeg = sizeMajArcmin / 60.0`.
- For a **point source** (star) this is meaningless — a star has no angular size; the app shows the
  framing but must not draw a disc sized from the magnitude. For a **planet** the disc diameter is a
  real, small angle; the F-map did **not** record whether `astronomy.kt` supplies one, so the planet
  disc size is `UNVERIFIED` and needs either a small authored diameter table or omission.
- **The app must not equate magnitude with visibility.** The frame overlay answers "what fits in my
  frame"; it does not answer "you will see it". The brief's own bar is a *planner*, so the honest
  output is the framing, the altitude, the dark window and the moon/cloud state — never a
  "visible: yes/no" derived from a magnitude.
- The comparison is only meaningful **within the dark window** (§2.3), so the frame view must show the
  current scrubbed time's sun/moon state beside it.

### 5.5 What the specification must state

- `FOV_axis = 2·atan(d_axis/(2·f))`, computed per axis and for the diagonal; the 35 mm-equivalent is
  `f · 43.2666 / d`; the full-frame diagonal is **43.2666 mm**.
- The device camera reads `SENSOR_INFO_PHYSICAL_SIZE` (`SizeF`, mm) and
  `LENS_INFO_AVAILABLE_FOCAL_LENGTHS` (`float[]`, mm), and the resulting FOV is an **upper bound**
  because the delivered frame may crop the physical sensor.
- The **lens preset table** (the ten rows of §5.2) ships with FOVs **generated by the formula**.
- The scene uses an **equidistant projection** so the frame overlay is a true 1:1 angular rectangle
  (`pxPerDeg = canvasWidthPx / sceneFovHorizDeg`); under any other projection the overlay is drawn from
  the **projected four corners**, never by pixel-ratio scaling.
- Frame rotation modes are horizon-landscape / horizon-portrait / free-roll, and the mode is recorded
  with any export.
- `frameFrac = objectAngularSizeDeg / frameFovShortSideDeg` and
  `pixels = objectAngularSizeDeg · imageWidthPx / fovHorizDeg` are the only size comparisons; **no
  visibility claim is derived from magnitude**, and a star is never drawn as a disc sized from `magV`.

---

## 6. Sky brightness and darkness quality

### 6.1 What the recon found, and why it is RISK

The F-map rated the darkness-quality feature **RISK** because it "would need a new bundled
raster/lookup asset", and the competitor map (`K-competitor-features.md`) shows the job is owned by a
**free, 500K+, 4.7-rated** incumbent (`com.pa.lightpollutionmap`).

### 6.2 Whether a licence-clean source exists — measured

| Candidate | Licence (measured this session) | Shippable in a **paid** app? |
|---|---|---|
| **Falchi et al. 2016, "The new world atlas of artificial night sky brightness"** (the Bortle/SQM-grade raster everyone uses) | **CC BY-NC** — PMC4928945 states: "distributed under the terms of the **Creative Commons Attribution-NonCommercial** license … so long as the resultant use is **not for commercial advantage**" | **✗ NO** — a $9.99 app is commercial |
| **NOAA / Colorado School of Mines EOG, VIIRS Nighttime Lights (VNL)** | **CC BY 4.0** — `eogdata.mines.edu/products/vnl/` states: "**Nighttime Lights data are available under Creative Commons Attribution 4.0 International license**" (linking `EOG_products_CC_License.pdf`), and asks to credit EOG / Payne Institute and cite the product papers | **✔ YES** |

**Conclusion: a licence-clean *source* exists, but a licence-clean *product* does not yet.**
VIIRS VNL is a **radiance** product (nW/cm²/sr), not a sky-brightness map. Turning it into
mag/arcsec² or a Bortle class requires a **model**, and the standard model is the CC BY-NC paper's.
Implementing a published *method* is not redistributing the paper's *data* — but that distinction is a
**legal-review item, not an engineering step**, and must be recorded as one if the feature is built.

### 6.3 Recommendation

**v1: out of scope — do not bundle a light-pollution raster.**

Three measured reasons: (a) the incumbent is **free and 500K+**, so the feature does not differentiate;
(b) the only clean raster needs factory-side reduction work (a coarse grid) **and** a model whose legal
status is a review item; (c) the app's differentiator is the **dark window and the session plan**, not
a pollution map.

**What to report instead — truthful, measured, and free of bundled third-party data:**

| Shown | Source | Licence |
|---|---|---|
| **Darkest Sun altitude reached** in the session (`sunAltMinDeg`) | computed (§2.3) | none needed |
| **The dark window itself** (start/end/duration, `kind`) | computed (§2.3) | none needed |
| **Moon illuminated fraction × altitude** | computed (§2.4) | none needed |
| **Total cloud cover** for the hours | `api.met.no` | CC BY 4.0 + visible attribution (§4.6) |
| **User-entered SQM reading** (mag/arcsec²), stored with the spot | the **user's own** measurement | none needed |
| **User-selected Bortle class 1–9**, labelled **"user estimate"** | the **user's own** input — not bundled data | none needed |

The last two are the key move: a number the user supplies is not third-party data, so the app can show
a darkness quality without shipping anything it does not have the right to ship.

**If the owner later wants a real VIIRS layer**, the specification records it as a follow-up with two
prerequisites: (1) produce a **reduced, coarse** (e.g. 0.5°×0.5°) radiance grid from the CC BY 4.0 VNL
product, with the EOG/Payne credit and the product citations in the About screen; (2) clear the
radiance→brightness model question as a legal item before any mag/arcsec² number is shown, and until
then present the value as a **relative index**, never as mag/arcsec².

### 6.4 What the specification must state

- **The Falchi World Atlas raster is CC BY-NC and MUST NOT be bundled** in this paid app — the source
  is quoted, so the refusal is auditable.
- **The only licence-clean raster family is NOAA/EOG VIIRS VNL (CC BY 4.0)**, and it is a **radiance**
  product, so it yields at most a **relative** artificial-brightness index — never a Bortle class and
  never mag/arcsec² — until the model question is cleared as a legal item.
- **v1 ships no light-pollution raster.** Darkness quality is reported from **computed** quantities
  (darkest Sun altitude, dark-window kind/duration, moon fraction × altitude) and from the **user's
  own** SQM reading or **user-selected** Bortle class labelled "user estimate".
- Any future VNL layer carries the **EOG / Payne Institute** credit and the product citations, and its
  reduced asset is a build input, not a download.

---

## 7. Cross-cutting: engine assignment, and what is verified versus merely present

| Concern | Engine | Licence | Data files needed at runtime |
|---|---|---|---|
| Sun, Moon, planets | **`astronomy.kt`** (Astronomy Engine, Kotlin) | **MIT** (measured, `astronomy.kt:2-7`) | **none** |
| Twilight, target alt-az, rise/set/transit | **`astronomy.kt`** | MIT | none |
| Satellites (TLE → TEME → ITRF → topocentric) | **Orekit 13.1.9 + Hipparchus 4.0.3** | **Apache-2.0** | **`tai-utc.dat`, 3,321 B** (minimum) |

**Measured status of the `astronomy.kt` path:**

- **Verified numerically: only the Sun** — to **0.0248 arcmin** against a JPL Horizons row.
- **Present but not exercised:** the **Moon** path (`searchRiseSet(Body.Moon)`, `illumination(Body.Moon)`),
  the **planets**, and the **`defineStar` user-star** path (no `defineStar` call appears in the corpus
  runner). These are `UNVERIFIED` end-to-end, and no UI number may rest on them until they are checked.
  *The check that settles them:* run each against a handful of JPL Horizons rows and record the
  arcminute error, exactly as `AstroCorpus.kt` already does for the Sun.
- **`astronomy.kt` contains no Messier/NGC catalogue, no magnitude table and no Bortle/SQM code**
  (MEASURED by a `Select-String` sweep) — so the catalogue (§1) and any darkness reporting (§6) are
  the slot's own, never the library's.

**Measured status of the Orekit path:** builds and shrinks under **R8 full mode with zero keep rules**
(220/249 `org.orekit.*` and 49/59 `org.hipparchus.*` classes survived; one non-fatal R8 warning about
`META-INF/services/com.sun.source.util.Plugin`), runs on the **test device** `R58MB3SGZVL`
(`SM-A405FM`, Android 11), APK **4,379,153 B = 4.176 MiB**, sha256 `39FAE799…E25A`.
**75.7 % of that payload is Orekit/Hipparchus *assets*, not code** — so trimming the bundled data to
what the chain needs (§3.3) is the single largest size lever the slot has.

*(Build timings were observed and are recorded for transparency only; per the owner's 2026-10-08
instruction they are never a scope, ranking or refusal criterion.)*

---

## 8. Contracts that could not be settled (with the exact check that would settle each)

1. **CelesTrak's licence for a paid app.** The usage policy is a bandwidth policy; it states the data
   are "freely available to all users" and neither forbids nor grants commercial use, and **no SPDX
   licence text was found** in the pages read.
   *Settling check:* read the licence/terms footer of `https://celestrak.org/` and
   `gp-data-formats.php` for an explicit grant or a commercial clause; if none, ask
   **TS.Kelso@celestrak.org** (the address the policy publishes) and record the answer.
2. **The real-world accuracy of a computed pass time.** The propagator's *fidelity* is measured
   (≤1e-4 km against the reference implementation) and its *divergence* at 21.3 days is measured
   (27,924 km), but the honest number for a fresh TLE was not measured.
   *Settling check:* on one night, compare the app's computed AOS for the ISS with an independently
   observed or third-party-predicted AOS at the same site, recording the element-set epoch used.
3. **The Moon's (and planets') numerical accuracy from `astronomy.kt`.** Only the Sun is verified
   (0.0248 arcmin); the Moon path is present but unexercised.
   *Settling check:* run `searchRiseSet(Body.Moon, …)` and `illumination(Body.Moon, …)` against a
   handful of JPL Horizons rows and record the arcminute error, as the Sun case already does.
4. **The legal status of a VIIRS-radiance → mag/arcsec² conversion.** Implementing the published
   method is not redistributing the CC BY-NC raster, but that is a legal opinion, not a measurement.
   *Settling check:* a licence review of the specific model and output before any mag/arcsec² number
   is shown; until then, present a relative index only.
5. **Whether `astronomy.kt` exposes `searchHourAngle`** (which would replace the transit scan).
   *Settling check:* `Select-String -Path astronomy.kt -Pattern 'fun searchHourAngle'`.
6. **The commercial status of the Yale BSC5** (should a brighter star list than FK6 be wanted).
   *Settling check:* CDS `V/50/ReadMe` carries no copyright notice (measured); a decision to ship it
   is a decision to rely on an unlicensed-by-default catalogue and must be recorded as such.
7. **An inconsistency to resolve, not average:** the recon's `I` file states FK6 is **4,150** stars
   while the live TAP `SELECT COUNT(*) FROM fk6.fk6join` returned **4,151**. The live measurement
   governs; the spec must use 4,151 and the row is recorded here so the two numbers do not silently
   disagree.
8. **A measured defect in the planned star source:** FK6 (CC0) **does not contain Sirius, Arcturus,
   Capella or Betelgeuse** — its brightest entry is `alpha Car` at `vmag = −0.62`, and its range is
   `−0.62 … 9.71` (all 4,151 rows carry a `vmag`). FK6 is a *fundamental* astrometric catalogue, not a
   naked-eye list, so a **hand-authored supplement of the brightest stars** (with its own two-source
   verification, per §1.4) is required, or the scene will omit the sky's most recognisable stars.
   *Settling check:* `SELECT comname, vmag FROM fk6.fk6join ORDER BY vmag ASC` — which is exactly how
   this was found.

---

## 9. CLAIM / LABEL / EVIDENCE ledger

```text
CLAIM                                                                                       LABEL      EVIDENCE
CelesTrak GP JSON is keyless and reachable                                                  MEASURED   curl GET gp.php?GROUP=STATIONS&FORMAT=JSON -> HTTP 200, 9,270 B, 17 records
CelesTrak asks for one download per update, GP updates every 2 hours                        MEASURED   celestrak-policy.html: "only download data once per update. For GP data, updates are once every 2 hours"
CelesTrak asks M2M software to stop on any non-200 and not retry                            MEASURED   celestrak-policy.html: "should immediately stop querying when it receives any non-HTTP 200 responses"
CelesTrak states no SPDX licence and no commercial grant in the pages read                  MEASURED   celestrak-policy.php + gp-data-formats.php text read; no licence statement present
Whether a paid app is affirmatively licensed by CelesTrak                                   UNVERIFIED read celestrak.org terms + gp-data-formats.php footer for a licence clause; or ask TS.Kelso@celestrak.org
GP data are OMM (CCSDS 502.0-B-3) with JSON/CSV using the OMM keywords                      MEASURED   celestrak-gp-doc.html: "data will be provided in both JSON and CSV formats, using the same keywords and definitions as provided in the OMM standard"
5-digit catalog numbers were exhausted on 2026-07-11; TLE cannot hold 100000+              MEASURED   celestrak-policy.html banner + gp-data-formats.php
OMM JSON key list (OBJECT_NAME … MEAN_MOTION_DDOT)                                          MEASURED   celestrak-stations.json, first record
Orekit TLE field ctor takes meanMotion in rad/s and angles in rad                           MEASURED   orekit-tle-javadoc.html: "meanMotion - mean motion (rad/s)", "i - inclination (rad)", range checks listed
OMM MEAN_MOTION (rev/day) must be converted: n[rad/s] = MEAN_MOTION*2*pi/86400               INFERRED   the two measured facts above (OMM unit is rev/day; Orekit wants rad/s)
TLE->TEME->ITRF->topocentric runs with 3,321 bytes of data (tai-utc.dat), simpleEOP=true     MEASURED   run-tinydata.txt: "TEME_AT_T+600s pos_km=(1280.749619,...)", "ITRF_OK simpleEOP=true", file size 3,321 B
Orekit refuses to run with no IERS UTC-TAI provider                                          MEASURED   verbose-nodata.txt:981 "no IERS UTC-TAI history data loaded"
A single long propagate() with ElevationDetector finds 1 pass where 15 exist                 MEASURED   run-passcrosscheck.txt: "DETECTOR events=2 aos=1" vs "BRUTE samples=4321 aos=15 max_elevation_deg=89.067 window_days=3.0 mask_deg=10.0"
Segmenting into 1-hour windows with maxCheck=60 finds all 15 passes                          MEASURED   F-map §: "segmented-1h maxCheck=60 events=30 aos=15"
Orekit SGP4 matches the reference implementation to 1e-4 km / 1e-7 km/s (29/33 blocks)       MEASURED   corpus-tinydata.txt: "AGGREGATE vectors=667 within=520 pos_tol_km=1.0e-4 vel_tol_kms=1.0e-7"; 29/33 blocks within
Stale TLE diverges 27,924.0197880 km at +1,844,000 s (~21.3 days)                            MEASURED   corpus-tinydata.txt: "CASE 20413#2 vectors=70 within=1 worst... max_pos_km=27924.0197880"
Real-world TLE pass-time accuracy (the "few minutes" claim)                                  UNVERIFIED same-night AOS comparison vs an independent prediction, recording the element-set epoch
CelesTrak OMM carries no standard magnitude field                                            MEASURED   celestrak-stations.json key list contains BSTAR/MEAN_MOTION_DOT/MEAN_MOTION_DDOT and no magnitude key
api.met.no requires an identifying User-Agent with contact info                              MEASURED   metno-tos.html: "All requests must (if possible) include an identifying User Agent-string"
api.met.no requires cache + If-Modified-Since and honouring Expires                          MEASURED   metno-tos.html: "Cache data locally and use the If-Modified-Since request header"
api.met.no truncates coordinates to max 4 decimals; 5+ returns 403                           MEASURED   metno-tos.html: "truncate all coordinates to max 4 decimals ... will return a 403 Forbidden"
api.met.no requires CC BY 4.0 attribution (credit + licence link + changes)                  MEASURED   metno-tos.html: "All open data require attribution as specified in the CC BY 4.0 license"
api.met.no forbids using "Yr" in a service name or its logo                                  MEASURED   metno-tos.html: "you may not use the word 'Yr' as a part of your service name"
api.met.no mobile apps must not fetch while not in use                                       MEASURED   metno-tos.html: "must not retrieve new data as long as the application is not in use"
compact payload: properties.timeseries[].data.instant.details.cloud_area_fraction, percent   MEASURED   metno-compact-UA.json parsed: type=Feature, 87 points, units "%", value 100.0
Civil/nautical/astronomical twilight are geometric Sun-centre at -6/-12/-18 deg              MEASURED   usno-rst.html: "geometrically 6 degrees / 12 degrees / 18 degrees below the horizon"; zenith distances 96/102/108
Sunrise/sunset uses 90.8333 deg zenith (50 arcmin = 16' semidiameter + 34' refraction)       MEASURED   usno-rst.html: "geometric zenith distance of center of the Sun is 90.8333 degrees ... 50 arcminutes below"
astronomy.kt implements Saemundsson refraction: (1.02/tan(h+10.3/(h+5.11)))/60, clamp -1 deg  MEASURED   astronomy.kt:4015-4031
astronomy.kt horizon() takes RA in hours (multiplies by 15)                                  MEASURED   astronomy.kt:5410-5412 "sinra = dsin(ra * 15.0)"
astronomy.kt Observer is lat N+/S-, lon E+/W- (deg), height metres                           MEASURED   astronomy.kt:1412-1431
astronomy.kt moonPhase returns ecliptic elongation in [0,360) deg                            MEASURED   astronomy.kt:6173 "pairLongitude(Body.Moon, Body.Sun, time)"; doc "range 0..360 degrees"
astronomy.kt illumination() gives phaseAngle (deg) and phaseFraction [0,1]                    MEASURED   astronomy.kt:2913-2920; fun illumination at :7251
searchAltitude is centre-of-body (no radius/dip/refraction); searchRiseSet includes them      MEASURED   astronomy.kt:6911-6918 (bodyRadiusAu=0.0) vs :6818-6845 (radius + dip + REFRACTION_NEAR_HORIZON)
Camera2 exposes SENSOR_INFO_PHYSICAL_SIZE (SizeF) and LENS_INFO_AVAILABLE_FOCAL_LENGTHS      MEASURED   javap over android-35/android.jar: exact Key<> types printed
FOV = 2*atan(d/(2f)); FF diagonal 43.2666 mm; 50mm/FF horiz = 39.6 deg                       INFERRED   arithmetic on the stated geometric definition
Falchi 2016 World Atlas is CC BY-NC (no commercial use)                                      MEASURED   PMC4928945: "Creative Commons Attribution-NonCommercial license ... not for commercial advantage"
NOAA/EOG VIIRS Nighttime Lights are CC BY 4.0                                                MEASURED   eogdata.mines.edu/products/vnl/: "Nighttime Lights data are available under Creative Commons Attribution 4.0 International license"
FK6 (GAVO) is CC0 1.0                                                                        MEASURED   fk6-form.html rights block + "spdx.org/licenses/CC0-1.0.html"
FK6 has 4,151 rows and vmag range -0.62 .. 9.71                                              MEASURED   TAP: "SELECT COUNT(*) ... -> 4151"; "SELECT MIN(vmag),MAX(vmag) ... -> -0.62,9.71"
FK6 does not contain Sirius, Arcturus, Capella or Betelgeuse (highest vmag is alpha Car -0.62) MEASURED   TAP: brightest-12 list + comname LIKE '%CMa%' (no alpha CMa) + hipno IN (32349,69673,24608,27989) returned only alpha Lyr
FK6 is 4,151 rows, not the 4,150 stated in the recon's I file                                MEASURED   live TAP COUNT(*) = 4151 vs I-assets-and-data.md "4,150"
Only the Sun path of astronomy.kt is numerically verified (0.0248 arcmin)                     MEASURED   F-engine-gap-map.md §: Sun verified vs JPL Horizons; the three moon_* corpus cases were refused
astronomy.kt has no Messier/NGC catalogue, no magnitude table, no Bortle/SQM code            MEASURED   F-map: Select-String for Messier|NGC|magnitude|bortle|sqm returned only solar-system magnitude() at :778
Orekit 13.1.5 + Hipparchus 4.0.3 shrink under R8 full mode with zero keep rules (measured on the 13.1.5 smoke jars; the declared pin is 13.1.9) MEASURED   G-android-build-proof.md: 220/249 + 49/59 classes survived; proguard-rules.pro has no keep rule; APK 4,379,153 B
Orekit/Hipparchus assets are 75.7% of the smoke APK payload                                   MEASURED   G-android-build-proof.md §5 inventory (3,260,714 compressed bytes)
A licence-clean sky-brightness SOURCE exists but no clean ready-made PRODUCT does              INFERRED   the two licence measurements above + VNL being radiance, not mag/arcsec^2
astronomy.kt exposes searchHourAngle                                                        MEASURED   Select-String 'fun searchHourAngle' -> astronomy.kt:6363: "fun searchHourAngle(body: Body, observer: Observer, hourAngle: Double, startTime: Time, direction: Int = +1): HourAngleInfo"
astronomy.kt supplies the Moon's apparent disc diameter but no planet's              PARTLY MEASURED  Select-String '(?i)diameter|angularradius' -> astronomy.kt:1804 "The apparent angular diameter of the Moon, in degrees, as seen from the center of the Earth" and :7844; no planet-radius/angular-diameter function found, so a planet disc size needs an authored table
The commercial status of Yale BSC5                                                          UNVERIFIED CDS V/50/ReadMe carries no copyright notice (measured); decision to rely on it must be recorded
```
tai-utc.dat is the IERS/USNO leap-second table and is fetchable byte-identically from maia.usno.navy.mil MEASURED   GET https://maia.usno.navy.mil/ser7/tai-utc.dat -> HTTP 200, Content-Length 3321, Last-Modified Tue, 06 Oct 2026 20:23:20 GMT; bytes identical to the shipped copy, sha256 3524E1AE34D67E858873A89E59983BBC5BD100221DA898E796C1B36036A310C3
Orekit ships a CCSDS/OMM parser whose result yields a TLE directly                        MEASURED   javap -public over orekit-13.1.9.jar: OmmParser.build() -> Omm; Omm.generateTLE()/generateKeplerianOrbit()/generateSpacecraftState(); OmmTle.getBStar()/getMeanMotionDot()/getMeanMotionDotDot()/getElementSetNo()/getRevAtEpoch()
