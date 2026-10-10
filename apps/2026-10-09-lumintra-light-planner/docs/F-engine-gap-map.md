# F — Engine gap map: can the already-verified core deliver the four market gaps?

Run 2026-10-08. Worker: technical-feasibility (nightvector engine). Deliverable path:
`.kiln/scratch/positioning-2026-10-08/F-engine-gap-map.md`.

**One question:** for each of the four gaps the scout found — (a) night/darkness window,
(b) deep-sky catalogue, (c) satellite passes, (d) hourly cloud cover — does the *already
proven* core deliver it, or does the gap need something new and unproven?

**Short answer:** (a) the dark window is deliverable from the proven engine; (b) and (c) are
deliverable with bounded, named work over the proven engine; (d) is trivially deliverable
engineering-wise but carries a **commercial licence problem the economics file already lists as
unverified**. Nothing needs a subsystem invented from scratch. Two claims in the recon framing
are wrong and are corrected below, and one hazard in the satellite path was reproduced for the
first time.

---

## 0. Method and what "verified" means here

I read `.kiln/scratch/nv-core-proof/` and `.kiln/scratch/positioning-2026-10-08/` on disk, and I
recompiled and re-ran the existing Orekit probes against the on-disk jars in this session so the
central finding would be a run I performed and not a note I inherited.

- **Toolchain used:** `JAVA_HOME=C:\Program Files\Java\jdk-17` (resolved through
  `scripts/lib/java.mjs`, which printed `{"home":"C:\\Program Files\\Java\\jdk-17","major":17,"source":"JAVA_HOME"}`).
- **Probes re-run:** `probe/orekit/PassProbe.java`, `PassCrossCheck.java`, `PassVariants.java`
  compiled with `javac -cp .kiln/scratch/nv-core-proof/lib/*` to
  `.kiln/scratch/positioning-2026-10-08/out-probe`, run with `-Djava.io.tmpdir=` pointed at
  `.kiln/scratch/positioning-2026-10-08/jtmp` (workspace-hygiene: no system temp).
- **Orekit runtime data** used by every run below is
  `.kiln/scratch/positioning-2026-10-08/tiny-data/orekit-data/tai-utc.dat` = **3,321 bytes**.
- **Jars** are the ones already on disk at `.kiln/scratch/nv-core-proof/lib/`.
- **Endpoint probes** are live `Invoke-WebRequest` calls made in this session; raw bodies and
  headers were appended to `.kiln/scratch/positioning-2026-10-08/netcheck.txt`.

**No Android build was run, by anyone, at any point.** There is no Gradle invocation, no D8/R8
pass, no DEX and no device run anywhere in this material. Everything about APK size in §6 is
therefore `INFERRED` from measured bytecode, and is labelled as such.

---

## 1. Two corrections to the brief before the gaps

The brief I was handed says the proof "proved SGP4/SDP4 propagation ... 29 of 32 cases within
sub-millimetre precision; the 3 remaining were deliberate error-code tests". Two things on disk
say otherwise, and both matter.

### 1.1 There is a fourth divergence, and it is not an error-code test

`.kiln/scratch/nv-core-proof/RESULT.md` and reread in `.kiln/scratch/positioning-2026-10-08/corpus-tinydata.txt`
show **33** cases in `TCPPVER.OUT`, split across **33** TLEs. The 3 annotated error fixtures are
`33333#1`, `33334#1`, `33335#1` — those are fine to exclude. But there is a **fourth** failing
block, `20413#2`, which is the *same satellite* `20413` extrapolated to +1,844,000 s
(≈ 21.3 days) and it passes **1 of 70** vectors, worst position error **27,924 km**:

```text
CASE 20413#2 vectors=70 within=1 max_pos_km=27924.0197880 max_vel_kms=1.99701767930
AGGREGATE vectors=667 within=520 pos_tol_km=1.0e-4 vel_tol_kms=1.0e-7
WORST 20413@1844000.00000000 pos=27924.0197880
```

`RESULT.md` states this plainly: *"SGP4: FAIL for the complete valid-state corpus."* The near-range
block `20413#1` passes 26/26.

**Why this does not kill gap (c), and when it would.** The failing block extrapolates ~3 weeks
past its TLE epoch. CelesTrak GP data is refreshed on roughly a 2-hour cadence (measured: the two
GROUPs I fetched this session carry epochs of `2026-10-08T00:21:12` and `2026-10-08T08:02:18`,
i.e. same-day) and the app fetches at use time, so its extrapolation horizon is **hours to a few
days**, not 3 weeks. The risk is therefore **not** in the normal path. It lives in the fallback:
if the network is unavailable the app must either show nothing or predict from a stale cached TLE,
and if it predicts from a TLE cached a week ago it is in the region where the corpus says the
propagator diverges. That is a product decision the engine forces, not a bug.

### 1.2 Orekit's own recommendation on disk was not "use Orekit"

`RESULT.md`'s verdict line is **`OREKIT_RISKY_USE_VALLADO_SUBSET`**, and its reasoning is that for a
narrow SGP4 core the vendored Vallado/CSSI Java subset (3 files, 135,064 bytes, 3,601 lines,
measured, compiled with plain `javac` into `out/vallado/`) avoids 11.56 MB of jars plus their
reflection and R8 surface. It is explicitly a *risk-based* selection: no Android failure was
reproduced. This matters for §6 and for the verdict, and it is the main reason the answer is not
"Orekit, and it's proven".

---

## 2. Gap (a) — NIGHT / darkness window

| | |
|---|---|
| **Verdict** | **DELIVERABLE** for the dark window itself. **RISK** if the owner also wants light pollution / Bortle / SQM — that is a new asset, not new maths. |

### 2.1 Which already-verified capability covers it

Covered by the Astronomy Engine port, exercised in
`.kiln/scratch/nv-core-proof/src/main/kotlin/AstroCorpus.kt`, source at
`.kiln/scratch/nv-core-proof/data/astronomy.kt` (432,946 bytes, 10,675 lines). The concrete APIs,
all read directly in that file this session:

| Need | API in `astronomy.kt` | Evidence |
|---|---|---|
| astronomical darkness (Sun below −18°) | `searchAltitude(body, observer, direction, startTime, limitDays, altitude): Time?` | `astronomy.kt:6911` |
| moonrise / moonset | `searchRiseSet(body, observer, direction, startTime, limitDays, metersAboveGround)` | `astronomy.kt:4942`, `:5013` |
| moon phase / quarter | `moonPhase(time)`, `searchMoonQuarter(startTime)` | `searchMoonQuarter` present (1 definition) |
| moon illuminated fraction + magnitude | `illumination(body, time)` → `IlluminationInfo` | `astronomy.kt:2896` |
| target-vs-Sun separation | `angleFromSun(body, time)` | `astronomy.kt:6936` |
| target-vs-Moon separation | `horizon(time, observer, ra, dec, refraction)` → azimuth/altitude for any RA/Dec | `astronomy.kt` header list |
| Sun/Moon ephemeris | `sunPosition`, `geoVector`, `equator`, `eclipticGeoMoon` | `vsopModelMercury…Neptune` present; `helioVector`/`geoVector` present |

**What was actually exercised numerically:** one case only. `verbose-astro.txt` ends:

```text
ASTRO case=sun_dc_eclipse az_err_arcmin=0.024758 alt_err_arcmin=0.001620
ASTRO_AGGREGATE cases=4 verified=1 unverified=3 max_error_arcmin=0.024758
```

So the **Sun** path is verified to 0.0248 arcmin against a JPL Horizons row, and the other three
supplied cases were correctly refused as `UNVERIFIED` because the reference file does not carry a
matching observer/target. **The Moon path was not verified numerically anywhere in this material** —
the three unverified cases were all `moon_*`. The functions exist and are MIT-licensed upstream
(`astronomy.kt:2-7`, "Astronomy Engine for Kotlin / JVM", MIT, © 2019-2025 Don Cross), but
"the function exists" is not "the number is right".

### 2.2 What must be NEWLY written

- **A few dozen lines over a proven engine.** A `DarkWindow` domain function that calls
  `searchAltitude(Body.Sun, …, −18.0)` twice (dusk/dawn) and intersects it with the Moon's
  rise/set and phase. No new dependency, no new asset.
- **The Moon accuracy check itself is work that must be scheduled, not assumed.** The engine's own
  upstream README claims ~1 arcmin for the Moon; that claim is **not on disk** in this material, so
  it is `UNVERIFIED`. Cheap to settle: run `searchRiseSet(Body.Moon, …)` and `illumination(Body.Moon, …)`
  against a handful of Horizons rows and record the arcminute error, exactly as `AstroCorpus.kt`
  already does for the Sun.
- **If light pollution / Bortle is wanted, that is a new bundled asset, not new maths.**
  `bortle|light pollution|sqm|skyglow|limiting magnitude` returns **zero** hits in `astronomy.kt`,
  and nothing light-pollution-shaped exists anywhere in the proof tree. It also appears in **no
  claim** of the NightVector promise contract (see §2.5). Delivering it means shipping a raster or
  a geocoded lookup (World Atlas 2015 / Falchi-style data) plus a point-sampling routine: a new
  data-pipeline item with a licence question, not a function call.

### 2.3 Does it need the network?

**No.** Entirely local. This is the one gap with a zero-network, zero-cost shape, which is worth
noting because the app is sold as *offline-capable* in its own distinctness sentence.

### 2.4 Permission or server?

**No server.** Observer position can be a manual entry or a saved `save-a-dark-sky-scout` location —
in which case **no permission at all**. If the app auto-locates instead, it needs
`ACCESS_COARSE_LOCATION`; the factory rule is that a runtime permission that buys nothing is a
compliance defect, and coarse-only manual entry avoids the question entirely. Note that
`.cursor/rules/ideation-gate.mdc` treats location as *not* in the "local and free" allowlist, so
auto-location has to justify itself.

### 2.5 Honesty check

- The dark window is **the strongest** of the four: local, permission-free, and sitting on an
  engine that is actually in this repository with an MIT header.
- The market claim "**none** computes light pollution, Bortle class, or a moonless-dark window"
  (scout, `C-sun-moon-light-shelf.md:96`) is scoped to *light-planners*, and read that way it holds.
  Read broadly it is **wrong**, and the correction is in this repository already: a dedicated
  light-pollution shelf exists and is monetized — `com.pa.lightpollutionmap` (*Light Pollution Map
  – Dark Sky*, Dunbar Technology) at **4.7★ / 7,330 reviews / 500K+ installs / free with IAP**, and
  `info.lightpollutionmap.mobile` (Deneb, Jurij Stare) at **$5.49 / 4.4★ / 80 reviews / 5K+
  installs**, the only purpose-built *paid* dark-sky product the scout found
  (`.kiln/scratch/positioning-2026-10-08/D-satellite-aurora-adjacent.md:88-96`). That cuts both
  ways and both halves matter: light pollution is **already served** by a strong free incumbent, so
  it is not an open field; and the paid entrant is **weak** (80 reviews at $5.49), which is the
  reachable-pocket signal the owner's own rule says to look for. What is *not* on the shelf is a
  dark-sky number **inside a session plan**, which is the actual wedge.
- What I can add from the engine side is the scoping problem: the promise contract as written only
  asks for *"darkness and moon windows"* (`compose-the-night-plan`); **Bortle is in no claim**. So
  gap (a) as the scout framed it is **larger than the promise list**. Either the owner accepts that
  the app ships a dark window without a Bortle number, or a claim is added and the asset work in
  §2.2 is scheduled. Silently implying light pollution in the listing while shipping no
  light-pollution number would be exactly the over-claim this factory forbids.

---

## 3. Gap (b) — DEEP-SKY object catalogue

| | |
|---|---|
| **Verdict** | **DELIVERABLE-WITH-WORK** — the ephemeris and window maths are already there; the catalogue is a bundled data file plus a list UI. |

### 3.1 Which already-verified capability covers it

Not the catalogue — the *per-target astronomy*. `astronomy.kt` provides exactly the primitives a
DSO window needs:

- `defineStar(body, ra, dec, distanceLightYears)` at `astronomy.kt:452` — "Any `Body` that has a
  `Star` associated with it ... retains that definition until re-defined by another call to
  `defineStar`" (`astronomy.kt:432`), with `ra` in hours [0,24), `dec` in degrees [−90,+90],
  distance ≥ 1 ly.
- Then `searchAltitude(star, observer, …)` / `searchRiseSet(star, …)` / `horizon(star, …)` — the
  doc for all three names a "user-defined star that was created by a call to `defineStar`"
  (`astronomy.kt:6343`, `:6783`, `:6883`). That is precisely "when is M31 above 30° tonight, and
  where".
- `constellation(ra, dec)` exists for a human-readable label.

So the *engine* side of gap (b) is done and was exercised only as far as the four-case corpus in
`AstroCorpus.kt` — **no `defineStar` call appears in the corpus runner**, so this specific path is
`UNVERIFIED` by execution even though the API is real and readable.

### 3.2 What must be NEWLY written

Three things, none of them a subsystem:

1. **A catalogue asset.** `Messier|NGC|magnitude` finds no catalogue table in `astronomy.kt` — the
   engine has no object data beyond planets and the 88 constellation boundaries.
   A keyless source exists and was fetched this session: **OpenNGC**
   `https://raw.githubusercontent.com/mattiaverga/OpenNGC/master/database_files/NGC.csv`
   → **HTTP 200, 3,877,591 bytes**, semicolon-separated, header measured as
   `Name;Type;RA;Dec;Const;MajAx;MinAx;PosAng;B-Mag;V-Mag;…;M;NGC;IC;…;Common names;NED notes;…`.
   That single file carries everything the gap asks for: `RA`/`Dec`, **`V-Mag`/`B-Mag` (magnitudes)**,
   **`MajAx`/`MinAx` (apparent size → framing scale)**, a `M` column for Messier cross-reference,
   and `Common names`.
2. **A trimmed subset** of that file. 3.88 MB raw is too much to bundle as-is; dropping the
   identifier/note columns and keeping Messier + the brightest few hundred NGC/IC entries is a
   build-time transform — `INFERRED`, roughly a few hundred KB, call it ~200–600 KB. **Licence is
   `UNVERIFIED`**: the obvious `…/master/LICENSE` on that repo returned **HTTP 404**, so the
   shipping rights have not been established by me and must be before this asset is committed.
3. **A target-list screen**: a list with magnitude/size, a visibility filter by magnitude limit,
   and an altitude-curve detail view. This is ordinary Compose work, and it is the piece that makes
   the "named deep-sky target altitude and azimuth curves" half of `compose-the-night-plan` real.

Total new surface: **a bundled data file + ordinary list/detail UI**. The hard part — sidereal
position, refraction, rise/set/altitude solving — is already proven code.

### 3.3 Does it need the network?

**At runtime: no.** The catalogue is a bundled asset; every calculation is local. The only network
is a **build-time** download of the upstream CSV, which is keyless. This is a plain advantage: the
distinctness sentence's "offline-capable" promise is intact.

### 3.4 Permission or server?

**None.** No permission, no server.

For gap (b) the factory's own `dependency-freshness`/`visual-identity` rules do apply to how the
asset is committed (provenance record), but that is ordinary slot hygiene, not a new capability.

### 3.5 Honesty check

- The **catalogue** is genuinely absent from everything on disk; do not let an earlier summary imply
  `astronomy.kt` has one. It does not.
- **Framing scale is a derived number, not a catalogue field.** `MajAx`/`MinAx` are arcminutes;
  converting them to "fills your 500 mm frame" needs the field-of-view maths from the user's
  focal length and sensor — that is the `save-a-dark-sky-scout` equipment profile doing real work,
  and the arithmetic should be unit-tested against a known case (e.g. 300 mm + APS-C ≈ 4.6° × 3.1°)
  rather than eyeballed.
- **Magnitudes arrive as `V-Mag`/`B-Mag` and the engine's own `magnitude()` is a different thing**
  (`astronomy.kt:778`, `:2897` apply to solar-system bodies). Do not mix the two: a DSO's brightness
  limit is a catalogue value, while `illumination()` only ever describes Sun/Moon/planets.
- Upstream `NGC.csv` is a **compiled amateur catalogue**: it is good enough for a planning app and
  is not an observatory-grade source. Say "catalogue magnitudes" in the brief, not "measured
  brightness".

---

## 4. Gap (c) — SATELLITE PASSES

| | |
|---|---|
| **Verdict** | **DELIVERABLE-WITH-WORK** — the propagation is proven and the TLE feed is live and keyless, **but the obvious Orekit API for finding passes is broken by default and I reproduced it three times.** The fix is known, cheap and proven. |

### 4.1 Which already-verified capability covers it

Both halves exist and both were exercised.

- **Propagation.** `TLEPropagator.selectExtrapolator(tle)` + `FramesFactory.getTEME()` in
  `PassProbe.java` (the same chain as the corpus runner
  `.kiln/scratch/nv-core-proof/src/main/java/proof/OrekitCorpus.java`). Corpus result in
  `corpus-tinydata.txt`: **29 of 33 blocks fully within tolerance** at
  `pos_tol = 1e-4 km` / `vel_tol = 1e-7 km/s`, worst in-tolerance error `1.97e-6 km`, with the
  exceptions being the 3 annotated error fixtures and `20413#2` (§1.1).
- **Topocentric look angles + pass events.** `OneAxisEllipsoid` + `TopocentricFrame` +
  `ElevationDetector` + `EventsLogger`, exercised in `PassProbe.java`, `PassCrossCheck.java`,
  `PassVariants.java`. Live output from this session, `.kiln/scratch/positioning-2026-10-08/run-tinydata.txt`:

  ```text
  TEME_AT_T+600s pos_km=(1280.749619,5592.447086,3605.982869) vel_kms=(-5.388644115,-2.061975140,5.053811958)
  ITRF_OK simpleEOP=true name=CIO/2010-based ITRF simple EOP
  TOPO_AT_T+600s elevation_deg=-44.136279 azimuth_deg=354.524835
  PROBE_EXIT_OK
  ```

  That is the whole chain running end-to-end: TLE → TEME state → ITRF → topocentric elevation and
  azimuth, with **3,321 bytes** of runtime data.

- **A local horizon profile, not just a flat mask.** `javap -public` over
  `orekit-13.1.5.jar` (run this session) shows `ElevationDetector` carries
  `withElevationMask(org.orekit.utils.ElevationMask)` / `getElevationMask()`, and
  `org.orekit.utils.ElevationMask` has a `ElevationMask(double[][])` constructor and
  `getElevation(double)`. That is the API for "obstructed to 20° in the north-west", and it maps
  directly onto the `save-a-dark-sky-scout` promise — *"a later plan uses **their horizon**,
  focal-length and tracking assumptions instead of a generic sky view"*
  (`docs/ideation/plans/nightvector-astro-plan.differentiators.json`, `claims[0]`). The saved
  horizon is therefore an input to pass detection, not just decoration. **This API's behaviour is
  `UNVERIFIED`** — it was read with `javap`, not executed; the probes all used
  `withConstantElevation`.

### 4.2 The hazard I reproduced — read this before planning the satellite feature

`ElevationDetector` + `EventsLogger` over a **single multi-day `propagate()` call** finds a fraction
of the real passes, and **`maxCheck` does not fix it**. I compiled the on-disk probe and ran it
three times; the output is byte-identical each time
(`.kiln/scratch/positioning-2026-10-08/run-passvariants.txt`):

```text
one-call maxCheck=600.0          events=2    aos=1
one-call maxCheck=300.0          events=2    aos=1
one-call maxCheck=60.0           events=2    aos=1
one-call maxCheck=10.0           events=2    aos=1
segmented-1h maxCheck=60         events=30   aos=15
```

And the independent brute-force cross-check, 60 s sampling over the same window and 10° mask
(`run-passcrosscheck.txt`):

```text
DETECTOR events=2 aos=1
BRUTE samples=4321 aos=15 max_elevation_deg=89.067 window_days=3.0 mask_deg=10.0
```

At 2 days it is `aos=1` against `BRUTE aos=10`. **The truth is 15 passes in 3 days; the one-call
detector reports 1.** Tightening `maxCheck` from 600 s to 10 s changes nothing.

**The fix is measured and it is cheap:** segment the propagation into ≤1-hour
`propagate(start, end)` calls. `segmented-1h maxCheck=60` reports `aos=15`, agreeing exactly with
the brute-force scan. So the app must either loop the propagation in short windows or scan at a
fixed step and refine the crossing — a few dozen lines, on proven code.

Two honesty notes on that table: the probe's final row is labelled `segmented-10m` but its source
passes `segmented=false` (`PassVariants.java:55`), so it is really another one-call run — the label
is a lie in the inherited probe, not a third experiment. And `PassProbe.java`'s own summary line
`PASSES_3D_10DEG=1` is affected by this same hazard; the number is `1`, the truth is `15`.

### 4.3 What must be NEWLY written

- **The segmented-or-scanned pass finder** described above. Small, and now designed by measurement.
- **An OMM/TLE parser** for the CelesTrak GP JSON (the response fields are `OBJECT_NAME`,
  `OBJECT_ID`, `EPOCH`, `MEAN_MOTION`, `ECCENTRICITY`, `INCLINATION`, `RA_OF_ASC_NODE`,
  `ARG_OF_PERICENTER`, `MEAN_ANOMALY`, `NORAD_CAT_ID`, `BSTAR`, … — measured in this session) into
  a `TLE`. Orekit's `TLE` constructor takes the two fixed-format lines, so this is a field mapping,
  not a parser project.
- **A visibility filter.** A pass that happens in daylight or with the satellite in Earth's shadow
  is not an observable pass; "visible passes only" needs Sun-vector + umbra test. The engine pieces
  exist (`sunPosition`, Orekit `EclipseDetector`/`OneAxisEllipsoid`); the *policy* (which passes
  count as visible) is a new product decision.
- **A cached-TLE policy.** See §1.1 and §4.5 — the app must state what it does with a stale TLE.

### 4.4 Does it need the network?

**Yes, for element sets only — and the feed is keyless.** Live this session
(`netcheck.txt`):

```text
PROBE celestrak-gp-json-stations  STATUS=200 BYTES=9270
PROBE celestrak-gp-json-weather   STATUS=200 BYTES=30271
```

`https://celestrak.org/NORAD/elements/gp.php?GROUP=<GROUP>&FORMAT=JSON` returns JSON with no key,
no token and no account. `STATIONS` (ISS, ~9 KB) and `WEATHER` (~30 KB) are both live and carry
same-day epochs. **Correction to the brief:** CelesTrak's *usage policy* page is **not** verified —
`https://celestrak.org/usage-policy/` returned **HTTP 500** when I asked for it, so the terms of
use for an app shipping at $10.99 are `UNVERIFIED`. That is a real gap; see §7.

The reconnaissance already agrees on both halves and states them better than a re-probe could:
`docs/ideation/2026-10-08-nightvector-astro-plan.md:69` records as `MEASURED` that *"CelesTrak
documents the endpoint and warns that GP data updates every two hours"* and as `ON RECORD` that
*"CelesTrak requires clients to respect its usage policy"*, and `:117` already carries the honest
counterpart — *"CelesTrak usage limits and long-term availability are **UNVERIFIED** as a product
dependency; caching and graceful stale-data display are mandatory mitigations."* My HTTP-500 probe
is the same gap observed from the other end. Note the *cadence* is documented and matches what I
measured (same-day epochs); it is the *terms* that remain unread.

### 4.5 Permission or server?

- **No server.** The app talks to CelesTrak directly; there is no key to hide, so
  `.cursor/rules/client-secrets.mdc` is satisfied by construction.
- **`android.permission.INTERNET`** is the only permission. No location permission is needed for
  the maths, because the observer position is already the saved scout location from gap (a).

### 4.6 Honesty check — the parts that could bite

1. **The detector hazard is real and reproducible.** Anyone who writes
   `propagator.addEventDetector(new ElevationDetector(...))` and calls `propagate(now, now+3d)` will
   ship an app that shows **one** pass where fifteen happen. This is the single most likely way the
   satellite feature ships broken while looking correct. It is now written down with a reproduction
   and a fix.
2. **Stale-TLE divergence.** §1.1's `20413#2` is the same propagator diverging by 27,924 km after
   ~3 weeks. With a live feed that is impossible; **offline or cached, it is the failure mode.** The
   app must show the TLE epoch and refuse (or visibly mark) predictions whose element set is older
   than a stated age. Do not silently predict from a week-old cache on a "dark-sky trip" app.
3. **"Satellite passes" is one word in a `core_operation` promise.** It carries no proof token of
   its own, and it is bundled with the dark window, DSO curves, weather and exposure guidance into a
   single claim. I did not verify that the *listing copy* keeps satellites at differentiator weight
   rather than headlining it: `E-money-repositioning.md:426` warns explicitly that the app must not
   become "a satellite tracker as its primary job". Keeping the listing wording inside the promise
   as written is the mitigation here.
4. **Orekit is 11.56 MB of jars for one job** and the proof's own verdict was to prefer the Vallado
   subset (§1.2, §6). If the subset is chosen instead, `TLEPropagator`/`ElevationDetector`/
   `TopocentricFrame` all disappear and each of the bullets in §4.3 becomes hand-written maths that
   needs its own validation against the same corpus. That trade is the owner's, and it should be made
   consciously rather than by whoever writes the first `build.gradle.kts`.

---

## 5. Gap (d) — HOURLY CLOUD COVER

| | |
|---|---|
| **Verdict** | **DELIVERABLE-WITH-WORK** technically (a client, a parser, a timeline UI). **RISK commercially** — the free weather tier explicitly excludes commercial use, and this app is a paid product. |

### 5.1 Which already-verified capability covers it

**None.** There is no weather code anywhere in the proof tree, and there should not be — it is not
an astronomy calculation. This gap is `UNVERIFIED` as an engine capability because it is not an
engine question: it is a network client plus a renderer.

### 5.2 What must be NEWLY written

- An HTTP client and JSON deserialiser for one endpoint (Kotlin serialization or OkHttp +
  `org.json`; the slot already has a Compose/AGP 9 build and a version catalog).
- A domain model `HourlyCondition(hourIso, cloudCoverTotal, cloudCoverLow/Mid/High, visibilityM,
  humidity, windSpeed)`.
- A timeline/ribbon UI hour-aligned against the dark window and the target curves from §2–§3.
  This is the actual product work — the value is the *alignment*, not the fetch.
- **A forecast, not a measurement, wording rule.** `E-money-repositioning.md:429` already says:
  say "forecast cloud cover", never "guaranteed clear sky". That must survive into the listing copy.

Size: ordinary app work, no engine risk. This is the smallest of the four in engineering terms and
the largest of the four in commercial terms.

### 5.3 Does it need the network? — yes, and this is where the problem is

Both probes are live this session (`netcheck.txt`):

```text
PROBE open-meteo-hourly STATUS=200 BYTES=3902
  hourly_units: cloud_cover %, cloud_cover_low %, cloud_cover_mid %, cloud_cover_high %,
                visibility m, relative_humidity_2m %, wind_speed_10m km/h
PROBE met-no-compact STATUS=200 BYTES=39280
  cloud_area_fraction = 100.0, air_temperature = 10.6, relative_humidity = 75.9,
  wind_speed = 2.5, 89 hourly entries
```

**Open-Meteo is confirmed keyless with the fields the app needs** — including the low/mid/high
split that a transparency model would actually want, which MET Norway's `compact` endpoint does
not provide. **But the licence does not fit this app.** From
`https://open-meteo.com/en/terms` (fetched this session, quoted):

> "By using the Free API for non-commercial use you agree to following terms: Less than 10'000 API
> calls per day, 5'000 per hour and 600 per minute. **You may only use the free API services for
> non-commercial purposes.** You accept to the CC-BY 4.0 licence…"
>
> Non-commercial includes "private or non-profit websites or apps **that do not have subscriptions
> or advertising**".

And from `https://open-meteo.com/en/pricing`:

> "Commercial Use Licence — A subscription grants a commercial use licence and an API key for the
> dedicated customer endpoint at `customer-api.open-meteo.com`. The API syntax is identical to the
> free tier — only the domain and key parameter differ."
> Plans: Standard 1M calls/month, Professional 5M calls/month, Enterprise 50M+.

`docs/ideation/plans/nightvector-astro-plan.economics.json` declares
`"archetype": "purchase_only_no_ads"`, `"priceUsd": 10.99`. A paid app is a commercial use, so the
free Open-Meteo tier is **at best arguable and at worst a licence breach**, and the paid tier is a
**recurring cost that a one-time price cannot carry** — which is precisely the shape
`.cursor/rules/ideation-gate.mdc` says a plan must show and the archetype set cannot hide
(subscriptions are banned, so there is no recurring revenue to fund a recurring cost).

**Verified fallback:** `https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=…&lon=…`
returned **HTTP 200** keyless, 89 hourly entries, `cloud_area_fraction` = total cloud cover. It
worked both with and without a `User-Agent` header, though MET Norway's terms require an
identifying one. It gives **no low/mid/high split**, so a transparency model would be coarser. Its
own licence terms are `UNVERIFIED` — I did not read them in this session.

The economics file already lists this as an unverified assumption: *"That client-only astronomy
calculations plus Open-Meteo and CelesTrak data are sufficiently reliable for advisory planning at
the selected location and date."* My finding sharpens that from a **reliability** question to a
**licence** question, and the licence question has to be answered before the listing is written,
not after.

### 5.4 Permission or server?

- **`android.permission.INTERNET`** only. No location permission (the saved scout supplies the
  point). Note that fetching weather for a location the user typed does not require
  `getLastKnownLocation` at all.
- **No server of ours is needed** — but if Open-Meteo's commercial tier is the answer, the free
  option that avoids a server is a **different keyless source**; if the self-hosted Open-Meteo OSS
  server is the answer, that **is** a server we would have to write/run, and its cost belongs in the
  revenue arithmetic, not in a footnote.

### 5.5 Honesty check

- Technically this is the easiest gap. **Commercially it is the riskiest**, and the risk is a
  licence and a recurring cost, neither of which any gate in this factory currently measures.
- **It is not a NightVector-specific problem — it is already blocking two plans.** A disk sweep
  turned up a second ideation document, `docs/ideation/2026-10-08-skywindow-drone.md:33-35`, which
  proposes the *same* Open-Meteo endpoint for a drone launch-window app and reaches the same
  conclusion: *"the provider's rendered terms page describes Open-Meteo as an open-source weather
  API with free access for non-commercial use and no API key; **commercial-use permission and
  attribution obligations are therefore UNVERIFIED pending an owner decision or provider
  confirmation before implementation**"*, with `:88` listing it as a load-bearing unverified item
  and `:105` naming an "Open-Meteo commercial-use refusal" as a **falsifier for the whole
  proposal**. Two independent proposals have now hit the same wall from different directions. That
  makes it a **factory-level decision** (one weather policy, one answer) rather than a detail to be
  re-litigated at each slot — which is exactly the kind of repeated failure `AGENTS.md` says should
  become a harness improvement.
- The scout's framing for (d) — "absent entirely from light-planners, with one exception" — is a
  claim about incumbents, `ON RECORD` from `C-sun-moon-light-shelf.md:99`. What I can add is that
  the **market gap is not the same as the deliverable**: `Clear Outside` exists at 10K+ / 3.5★
  (per the same scout file) as a dedicated astronomy-cloud app, so "nobody does cloud cover" is
  overstated even by the scout's own evidence — the honest sentence is "the light-planners do not,
  and the one that does is the weakest-rated app on that shelf".
- A forecast in a paid app carries a **trust liability**: the app must never let a cloud forecast
  read as a promise. Attribute the source, label it a forecast, and show the model/observation time
  (Open-Meteo returns `generationtime_ms`; MET Norway returns `meta.updated_at`, measured
  `2026-10-08T13:16:55Z`).

---

## 6. APK size impact of the Orekit + Hipparchus runtime

**Headline: realistically ~1.4–2.8 MB added to the APK; floor ~0.9–1.2 MB; ceiling ~9.5–12 MB if
shrinking does nothing.** Every number below is `INFERRED` from `MEASURED` bytecode — no DEX or APK
was produced for this app, by anyone.

### 6.1 Measured inputs

Jars on disk (`.kiln/scratch/nv-core-proof/lib/`, measured this session):

| Jar | classes | class-byte bytes | jar bytes |
|---|---:|---:|---:|
| `orekit-13.1.5.jar` | 3504 | 13,664,605 | 8,527,112 |
| `hipparchus-core-4.0.3.jar` | 681 | 2,588,915 | 1,959,795 |
| `hipparchus-geometry-4.0.3.jar` | 153 | 672,848 | 276,264 |
| `hipparchus-ode-4.0.3.jar` | 160 | 608,213 | 282,008 |
| `hipparchus-optim-4.0.3.jar` | 139 | 406,973 | 228,802 |
| `hipparchus-stat-4.0.3.jar` | 120 | 387,893 | 222,980 |
| `hipparchus-fitting-4.0.3.jar` | 15 | 38,383 | 31,076 |
| `hipparchus-filtering-4.0.3.jar` | 23 | 41,487 | 31,650 |
| **total** | **4795** | **18,409,317** | **11,559,687** |

What one full pass-calculation actually loaded (`-Xlog:class+load=info` run recorded at
`.kiln/scratch/positioning-2026-10-08/verbose-orekit.txt`; per-class bytes resolved against the
jars, written to `runtimeloaded-orekit.csv`):

| Jar | unique classes loaded | class bytes |
|---|---:|---:|
| `orekit-13.1.5.jar` | 294 | 1,282,697 |
| `hipparchus-core-4.0.3.jar` | 63 | 462,256 |
| `hipparchus-geometry-4.0.3.jar` | 11 | 50,096 |
| `hipparchus-ode-4.0.3.jar` | 1 | 1,189 |
| **total** | **369** | **1,796,238** |

Note: **only 3 of the 7 Hipparchus modules were touched at all** — `fitting`, `optim`, `filtering`
and `stat` loaded **zero** classes in the whole pass calculation, so they are not proven to be
needed by this feature set.

Deflate behaviour of DEX in *this factory's own* release APKs (measured this session):

| APK | APK bytes | `classes.dex` uncompressed | compressed | ratio |
|---|---:|---:|---:|---:|
| `unit-walk-0.1.0-1-release.apk` | 2,616,511 | 3,474,160 | 1,755,030 | 0.505 |
| `laneset-0.1.0-1-release.apk` | 2,150,611 | 3,024,728 | 1,536,367 | 0.508 |

### 6.2 The arithmetic (INFERRED)

Assumption, stated as an assumption: DEX occupies roughly **1.0–1.3×** the sum of its input class
bytes, and this factory's DEX deflates at **0.505–0.508** (measured above). Therefore

`APK contribution ≈ class bytes × (1.0…1.3) × 0.505`

- **Floor** — if R8's reachable set were exactly the 369 classes one pass run touched:
  1,796,238 B → 1.80–2.34 MB DEX → **≈ 0.91–1.18 MB in the APK**.
- **Realistic** — the app reaches more than one probe does (time scales, `AbsoluteDate`
  parsing/formatting, `IERSConventions`, `GeodeticPoint`, more detectors, eclipse/shadow tests, plus
  the app's own reachable engine paths), so 1.5–2.5× the sampled set:
  2.7–4.5 MB class bytes → 2.7–5.85 MB DEX → **≈ 1.36–2.95 MB in the APK**.
- **Ceiling** — if R8 keeps the reachable portion of both jars and little is pruned:
  18.41 MB class bytes → ≈ 19–24 MB DEX → **≈ 9.5–12 MB in the APK**.

**Context that makes the realistic case matter:** this factory's shipped release APKs measure
1,125,282 / 1,449,830 / 2,150,611 / 2,528,477 / 2,557,448 / 2,616,511 / 4,022,986 bytes. The
realistic case therefore **roughly doubles a typical PlayKiln app**, and the ceiling case would be
the largest APK this factory has ever produced by a wide margin.

**Runtime data is negligible.** The only Orekit data file the pass chain needed was
`tai-utc.dat` = **3,321 bytes** (`tiny-data/orekit-data/tai-utc.dat`), newest entry
`2017 JAN 1 TAI-UTC= 37.0`, i.e. current through the last real leap second. Two packaging traps
were reproduced and are worth writing into the build notes:

- `simpleEOP=true` avoids needing `finals.all` (`ITRF_OK simpleEOP=true name=CIO/2010-based ITRF
  simple EOP`), but the UTC/TAI history file **is** required — without a data provider the run dies
  with `OrekitException: no IERS UTC-TAI history data loaded` (`verbose-nodata.txt:980`).
- Shipping a malformed `itrf-versions.conf` **breaks** ITRF: `run-minimaldata.txt` and
  `run-fulldata.txt` both show
  `ITRF_FAIL … The first column of itrf-versions.conf is a plain prefix … Actual value: "eopc04_05\.\d\d"`.
  So the data set to ship is `tai-utc.dat` **alone**; more files is not safer, it is a new failure.

**The Astronomy Engine side is a rounding error:** `astro.jar` holds 74 classes / 385,296 bytes,
and the verified Sun case loaded 38 classes / 296,904 bytes → tens to ~150 KB in the APK. It also
brings the MIT licence with it (`astronomy.kt:2-7`).

### 6.3 The alternative, and its own unverified half

`RESULT.md` measured the vendored Vallado/CSSI subset at **135,064 bytes / 3,601 lines** across
`SGP4unit.java`, `SGP4SatData.java`, `SGP4utils.java`, compiling with plain `javac` (classes are on
disk at `nv-core-proof/out/vallado/`). Its DEX contribution would be tens of KB, i.e. **two orders
of magnitude smaller** than the Orekit path. Two things it does **not** have:

- **Its numerical accuracy against the corpus was never measured.** RESULT.md reports that it
  *compiled*; the corpus was run against Orekit only. So choosing Vallado means validating a
  propagator that has not yet been validated here.
- **Its shipping licence is `UNVERIFIED`** — RESULT.md states no standalone licence file was found
  in the inspected archive entries.

If the owner wants the small APK, Vallado is the route **and** it is a second, unproven derivation
task — not a free substitution for a proven one.

---

## 7. Cross-cutting risks, ranked

1. **A paid app on a free weather tier — and it blocks two plans, not one.** `purchase_only_no_ads`
   at $10.99 is a commercial use; Open-Meteo's free tier is licensed for non-commercial use only,
   and their commercial tier is a recurring subscription. With subscriptions banned, a recurring
   cost has no revenue to fund it. This needs an answer **before** listing copy mentions weather,
   and it is already load-bearing in `skywindow-drone` too (§5.5), so the right move is one factory
   decision rather than two slot decisions. Verified fallback exists (`api.met.no`, keyless, total
   cloud only) but its licence is `UNVERIFIED` and CelesTrak's usage policy page failed to load
   (`HTTP 500`).
2. **The pass-detector trap (§4.2).** Reproduced 3×, deterministic: the natural API returns 1 pass
   where 15 exist, and `maxCheck` does not help. Highest chance of shipping a feature that looks
   correct and is wrong.
3. **Orekit's APK cost versus the proof's own recommendation (§1.2, §6).** 11.56 MB of jars for one
   job, on a `purchase_only_no_ads` app in a category where a smaller download is a conversion
   advantage, with the proof's own verdict pointing at a 135 KB alternative that is itself
   unvalidated.
4. **Nothing has ever been built for Android.** No Gradle, no R8, no DEX, no device. The `java.beans`
   hazard was cleared by constant-pool scan and the bytecode is Java 8 (fine at minSdk 26), but
   `javax.xml` references and reflection use remain, and R8 keep rules are `UNVERIFIED`. This is the
   gate between "proven on a JVM" and "ships".
5. **Stale-TLE divergence (§1.1).** 27,924 km error after ~3 weeks of extrapolation. Only reachable
   via cache or offline, which is exactly the state this app advertises.
6. **Promise drift on gap (a).** The scout's market gap includes light pollution / Bortle; the
   promise contract does not. Either scope it in with its asset work or keep it out of the listing
   claims.

---

## 8. Verdict table

| Gap | Verdict | Covered by | New work | Network | Permission | Main risk |
|---|---|---|---|---|---|---|
| **(a) night / darkness window** | **DELIVERABLE** (dark window) / **RISK** (Bortle) | Astro Engine `searchAltitude`, `searchRiseSet`, `moonPhase`, `illumination`, `angleFromSun` — `astronomy.kt`; only the **Sun** verified numerically (0.0248 arcmin) | few dozen lines; **Bortle would need a new bundled raster/lookup asset** | none | none (manual/saved location); coarse location only if auto-locating | Moon path never verified numerically; Bortle is in no claim, and light pollution is already served by a 500K+ free incumbent |
| **(b) deep-sky catalogue** | **DELIVERABLE-WITH-WORK** | Astro Engine `defineStar` + `searchAltitude`/`horizon` (`astronomy.kt:452`, `:6343`) — API present, **not exercised** | bundled catalogue asset (OpenNGC CSV measured 3.88 MB raw → trimmed ~200–600 KB inferred) + list/detail UI | build-time only, keyless | none | OpenNGC licence `UNVERIFIED` (LICENSE probe 404) |
| **(c) satellite passes** | **DELIVERABLE-WITH-WORK** | Orekit `TLEPropagator` + `TopocentricFrame` + `ElevationDetector` (`PassProbe/PassCrossCheck/PassVariants`); corpus 29/33 blocks | segmented pass finder (proven fix), OMM→TLE mapping, visible-pass policy, stale-TLE policy | yes — CelesTrak GP JSON, **keyless, HTTP 200** | `INTERNET` | detector trap (1 vs 15 passes, reproduced); Orekit 11.56 MB; stale-TLE 27,924 km |
| **(d) hourly cloud cover** | **DELIVERABLE-WITH-WORK** (engineering) / **RISK** (licence) | **nothing** — not an engine capability | HTTP client + model + timeline UI aligned to the dark window | yes — Open-Meteo keyless **but non-commercial-only**; `api.met.no` keyless fallback (total cloud only) | `INTERNET` | commercial licence + recurring cost on a one-time price |

**Size finding:** Orekit + Hipparchus is **≈ 1.4–2.8 MB** added to the APK in the realistic case
(floor ≈ 0.9–1.2 MB if R8 reaches only the 369 classes one pass run touched; ceiling ≈ 9.5–12 MB if
shrinking does nothing), against measured existing release APKs of 1.1–4.0 MB — i.e. it roughly
doubles a typical PlayKiln app. Orekit runtime data needed is `tai-utc.dat` at **3,321 bytes**;
Astronomy Engine adds ~tens to ~150 KB.

**Single biggest risk:** the **weather licence** — a $10.99 `purchase_only_no_ads` app using
Open-Meteo's free tier is commercial use, the compliant tier is a recurring subscription with no
recurring revenue to fund it, and the same wall has already stopped a second proposal
(`skywindow-drone`) from being implementable as designed. It is the only risk on this list that can
invalidate the product *after* it is built, rather than a bug that can be fixed in it, and it is a
factory-level decision rather than a slot detail.

---

## 9. Claim ledger

| CLAIM | LABEL | EVIDENCE |
|---|---|---|
| SGP4 corpus has 4 divergences: 3 annotated error fixtures plus the long-range `20413#2` block, which passes 1 of 70 vectors with worst position error 27,924.0197880 km | MEASURED | `javac`/`java proof.OrekitCorpus` output captured at `.kiln/scratch/nv-core-proof/RESULT.md` and `.kiln/scratch/positioning-2026-10-08/corpus-tinydata.txt` (`CASE 20413#2 vectors=70 within=1`, `WORST 20413@1844000.00000000 pos=27924.0197880`); near-range `CASE 20413#1 vectors=26 within=26` |
| The proof's own verdict was `OREKIT_RISKY_USE_VALLADO_SUBSET`, not "Orekit is safe for Android" | MEASURED | `.kiln/scratch/nv-core-proof/RESULT.md`, "Verdict" section |
| The Vallado Java subset measures 135,064 bytes across 3 files / 3,601 lines and compiles with plain `javac` | MEASURED | `RESULT.md` table and `observed VALLADO_JAVAC=PASS`; compiled classes on disk at `.kiln/scratch/nv-core-proof/out/vallado/SGP4unit.class`, `SGP4SatData.class`, `SGP4utils.class` |
| The Vallado subset's numerical accuracy against the corpus was never measured here | MEASURED (absence) | `RESULT.md` reports compilation only; the corpus command targets `proof.OrekitCorpus`; no Vallado corpus output exists in the tree |
| `astronomy.kt` declares `defineStar(body, ra, dec, distanceLightYears)`, `searchAltitude(body, observer, direction, startTime, limitDays, altitude)`, `angleFromSun(body, time)`, `moonPhase`, `searchRiseSet`, `illumination`, `constellation` | MEASURED | Read in `.kiln/scratch/nv-core-proof/data/astronomy.kt`: `:452` (`defineStar`), `:6911` (`searchAltitude`), `:6936` (`angleFromSun`), `:2896` (`illumination`/`IlluminationInfo`); `searchAltitude`/`searchRiseSet`/`horizon` docs name a `defineStar` user-defined star at `:6343`, `:6783`, `:6883` |
| Astronomy Engine is MIT-licensed, "Astronomy Engine for Kotlin / JVM", © 2019-2025 Don Cross | MEASURED | `astronomy.kt:2-7` header |
| Only one astronomy case is numerically verified: `sun_dc_eclipse` at 0.024758 arcmin; the three Moon cases are `UNVERIFIED` | MEASURED | `.kiln/scratch/positioning-2026-10-08/verbose-astro.txt` tail: `ASTRO_AGGREGATE cases=4 verified=1 unverified=3 max_error_arcmin=0.024758` and three `status=UNVERIFIED` rows |
| `astronomy.kt` contains no Messier/NGC catalogue, no magnitude table and no Bortle/SQM/light-pollution code | MEASURED | `Select-String` over `astronomy.kt` for `Messier|NGC|magnitude|magTable` and `bortle|Bortle|light.?pollution|sqm|SQM|limiting magnitude|skyglow` — the only magnitude hits are the solar-system `magnitude()` at `:778` and `IlluminationInfo` at `:2896-2906`; the Bortle/SQM scan returned zero lines |
| `TLEPropagator` + `ElevationDetector` over one 3-day `propagate()` finds 1 AOS where 15 exist, identically for `maxCheck` 600/300/60/10 s, reproducible across 3 runs | MEASURED | `java -cp "out-probe;lib/*" probe.PassVariants tiny-data` run this session, output saved to `.kiln/scratch/positioning-2026-10-08/run-passvariants.txt`; four `one-call maxCheck=… aos=1` rows |
| Segmenting the propagation into 1-hour `propagate()` calls finds all 15 AOS, matching an independent 60 s brute-force elevation scan | MEASURED | Same run: `segmented-1h maxCheck=60 events=30 aos=15`; and `probe.PassCrossCheck tiny-data 3.0 10.0` → `DETECTOR events=2 aos=1` / `BRUTE samples=4321 aos=15 max_elevation_deg=89.067 window_days=3.0 mask_deg=10.0`, saved to `run-passcrosscheck.txt` |
| The TLE→TEME→ITRF→topocentric chain runs end-to-end with 3,321 bytes of Orekit runtime data | MEASURED | `.kiln/scratch/positioning-2026-10-08/run-tinydata.txt`: `TEME_AT_T+600s pos_km=(1280.749619,5592.447086,3605.982869)`, `ITRF_OK simpleEOP=true`, `TOPO_AT_T+600s elevation_deg=-44.136279 azimuth_deg=354.524835`, `PROBE_EXIT_OK`; file size measured at `tiny-data/orekit-data/tai-utc.dat` = 3,321 bytes |
| Orekit refuses to run the ITRF/UTC chain with no data provider, and a malformed `itrf-versions.conf` also breaks it | MEASURED | No provider: `verbose-nodata.txt:980` — `Exception in thread "main" org.orekit.errors.OrekitException: no IERS UTC-TAI history data loaded`. Malformed config: `run-minimaldata.txt` and `run-fulldata.txt` — `ITRF_FAIL org.orekit.errors.OrekitException: The first column of itrf-versions.conf is a plain prefix … Actual value: "eopc04_05\.\d\d"` |
| The shipped `tai-utc.dat` is current through the last real leap second (`2017 JAN 1 TAI-UTC= 37.0`) | MEASURED | `.kiln/scratch/positioning-2026-10-08/tiny-data/orekit-data/tai-utc.dat`, last matching row |
| Open-Meteo's free tier is licensed for **non-commercial** use only and its commercial tier is a subscription | MEASURED | Live fetch of `https://open-meteo.com/en/terms` and `https://open-meteo.com/en/pricing` this session, status 200, quoted text appended to `.kiln/scratch/positioning-2026-10-08/netcheck.txt` |
| The app is a paid, commercial product: `purchase_only_no_ads`, `priceUsd` 10.99 | MEASURED | `docs/ideation/plans/nightvector-astro-plan.economics.json` |
| The economics file already lists the Open-Meteo/CelesTrak reliability assumption as unverified | MEASURED | `docs/ideation/plans/nightvector-astro-plan.economics.json`, `assumptionsUnverified[2]` |
| `https://api.open-meteo.com/v1/forecast` returns hourly `cloud_cover`, `cloud_cover_low/mid/high`, `visibility`, `relative_humidity_2m`, `wind_speed_10m` keyless | MEASURED | Live `Invoke-WebRequest` this session: `PROBE open-meteo-hourly STATUS=200 BYTES=3902`, body head in `netcheck.txt` |
| `https://celestrak.org/NORAD/elements/gp.php?GROUP=…&FORMAT=JSON` returns GP JSON keyless with same-day epochs | MEASURED | Live probes this session: `STATUS=200 BYTES=9270` (`STATIONS`, first object `ISS (ZARYA)`, `EPOCH":"2026-10-08T00:21:12.026592`) and `BYTES=30271` (`WEATHER`) |
| `https://api.met.no/weatherapi/locationforecast/2.0/compact` returns hourly `cloud_area_fraction` keyless, 89 entries | MEASURED | Live probe this session, `STATUS=200 BYTES=39280`; parsed `cloud_area_fraction = 100.0`, `timeseries entries=89`, `meta.updated_at=2026-10-08T13:16:55Z` |
| OpenNGC's `NGC.csv` is fetchable keyless and carries RA, Dec, `V-Mag`/`B-Mag`, `MajAx`/`MinAx`, Messier cross-reference and common names | MEASURED | Live probe this session: `PROBE openngc-NGC.csv STATUS=200 BYTES=3877591`, header quoted in `netcheck.txt` |
| OpenNGC's shipping licence is not established | MEASURED (absence) | `PROBE openngc-LICENSE FAILED: … (404) Not Found`, `netcheck.txt` |
| The loaded-class set for one full pass calculation is 369 classes / 1,796,238 bytes; only 3 of 7 Hipparchus modules were touched | MEASURED | `-Xlog:class+load` log `.kiln/scratch/positioning-2026-10-08/verbose-orekit.txt`, resolved per-class against the jars; summary in `runtimeloaded-orekit.csv` (294 orekit/1,282,697 B; 63 core/462,256 B; 11 geometry/50,096 B; 1 ode/1,189 B; fitting/optim/filtering/stat = 0) |
| The jars total 4,795 classes / 18,409,317 class bytes / 11,559,687 jar bytes, `orekit-13.1.5.jar` alone 8,527,112 bytes | MEASURED | `System.IO.Compression.ZipFile` enumeration of `.kiln/scratch/nv-core-proof/lib/*.jar` this session |
| DEX in this factory's release APKs deflates at 0.505–0.508 | MEASURED | `unit-walk-0.1.0-1-release.apk` (3,474,160 → 1,755,030) and `laneset-0.1.0-1-release.apk` (3,024,728 → 1,536,367), measured this session; existing release APK sizes 1,125,282–4,022,986 bytes |
| The Astronomy Engine contributes 74 classes / 385,296 bytes of jar classes and 38 loaded classes / 296,904 bytes for the verified Sun case | MEASURED | `astro.jar` enumeration; `verbose-astro.txt` cosinekitty class-load lines resolved against `astro.jar` |
| Orekit+Hipparchus adds ≈ 1.4–2.8 MB to the APK in the realistic case (floor ≈ 0.9–1.2 MB, ceiling ≈ 9.5–12 MB) | INFERRED | From the measured class bytes above × an assumed 1.0–1.3× DEX inflation × the measured 0.505–0.508 deflate ratio; no DEX was produced for this app |
| Orekit + Hipparchus build, shrink and run correctly on the target Android configuration | UNVERIFIED | No Android/Gradle/D8/R8 build was run by anyone in this material. Settle with `./gradlew :app:assembleRelease` on a slot that includes the jars, then `unzip -l` the APK for `classes*.dex` and read the R8 keep rules |
| The Moon path of `astronomy.kt` is accurate to the ~1 arcmin its upstream README claims | UNVERIFIED | The README claim is not in this repository; the three `moon_*` corpus cases were refused. Settle with `searchRiseSet(Body.Moon, …)` / `illumination(Body.Moon, …)` against JPL Horizons rows, as `AstroCorpus.kt` already does for the Sun |
| CelesTrak's terms of use permit a commercial app to poll the GP endpoint | UNVERIFIED | `https://celestrak.org/usage-policy/` returned **HTTP 500** this session. Settle by reading the policy page in a browser |
| MET Norway's terms permit a commercial app, and require an identifying User-Agent | UNVERIFIED | The endpoint answered 200 with and without a `User-Agent`, which is not a licence. Settle by reading `https://api.met.no/doc/TermsOfService` |
| The scout's incumbent claims ("none computes light pollution/Bortle", "cloud cover absent except PhotoTime") | ON RECORD | `docs/ideation/2026-10-08-nightvector-astro-plan.md`, `.kiln/scratch/positioning-2026-10-08/C-sun-moon-light-shelf.md:96` and `:99` — the scout's own keyword scans; I did not re-survey the shelf |
| The four gaps map onto the promise contract as: (a) + (b) + (c) + (d) → `compose-the-night-plan`; light pollution → no claim | MEASURED | `docs/ideation/plans/nightvector-astro-plan.differentiators.json`, `claims[1]` (`compose-the-night-plan`, `core_operation`, `core`, `status: deferred`) |
| `ElevationDetector` exposes `withElevationMask(ElevationMask)` / `getElevationMask()`, and `org.orekit.utils.ElevationMask` has a `ElevationMask(double[][])` constructor and `getElevation(double)` | MEASURED | `javap -public` over `.kiln/scratch/nv-core-proof/lib/orekit-13.1.5.jar` in this session (javap resolved at `E:\Programm\Android\Android Studio\jbr\bin\javap.exe`); output quoted in §4.1. Behaviour not executed |
| A local horizon profile is a real input: the probe code uses only `withConstantElevation`, so the mask path is available but unexercised | MEASURED (absence) | `probe/orekit/PassProbe.java`, `PassCrossCheck.java`, `PassVariants.java` — all three call `withConstantElevation(...)`; no `ElevationMask` appears in any of them |
| A dedicated, monetized light-pollution shelf already exists: `com.pa.lightpollutionmap` 4.7★ / 7,330 reviews / 500K+ / free-with-IAP, and `info.lightpollutionmap.mobile` $5.49 / 4.4★ / 80 reviews / 5K+ | MEASURED | `.kiln/scratch/positioning-2026-10-08/D-satellite-aurora-adjacent.md:88-89` and `:96` (Play market scan the scout ran); consistent rows in `D-raw.md:126,131` and `.kiln/scratch/astro-recon-2026-10-08/market.json:5945,5952` |
| The same Open-Meteo commercial-use question already blocks a second proposal, and is named there as a falsifier for it | MEASURED | `docs/ideation/2026-10-08-skywindow-drone.md:33-35` (endpoint + "commercial-use permission and attribution obligations are therefore UNVERIFIED pending an owner decision"), `:88` (`UNVERIFIED` row), `:105` ("an Open-Meteo commercial-use refusal would also block the no-key data plan"); cross-reference in `docs/ideation/plans/skywindow-drone.economics.json:19` |
| CelesTrak's two-hour GP update cadence is documented upstream and its usage policy is treated as binding, while usage limits remain unverified | ON RECORD | `docs/ideation/2026-10-08-nightvector-astro-plan.md:69` and `:117`, `:130` — the scout's 2026-10-08 reading of CelesTrak's own GP documentation |
