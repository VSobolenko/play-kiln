# Lumintra bundled-asset formats

**This document is the complete loader contract.** A Kotlin loader author must be able to parse every
bundled asset from this file alone, without reading the bakers. It is authoritative for the *bytes*:
where it disagrees with a prose annex, this file governs and the disagreement is recorded in
`docs/asset-provenance.md`.

It describes the assets that ship under `app/src/main/assets/`:

| Asset | Kind | Format | Exact size |
|---|---|---|---|
| `orekit-data/tai-utc.dat` | text | IERS leap-second table | 3,321 B |
| `sky/sky_lut_a.bin` | binary | twilight LUT, antisolar meridian | 33,858 B |
| `sky/sky_lut_b.bin` | binary | twilight LUT, solar meridian | 33,858 B |
| `catalog/stars.bin` | binary | FK6 ⊕ supplement star table | `11 × (4151 + supplement_rows)` B |
| `catalog/objects.json` | text | authored deep-sky catalogue | variable |
| `catalog/constellations.json` | text | 88 IAU constellations | variable |
| `sky/constellation_lines.json` | text | IAU / Sky & Telescope figures | variable |
| `sky/milky_way_eso0932a.webp` | image | equirectangular panorama | < 4 MB |
| `sky/moon_svs.webp` | image | equirectangular Moon map | small |
| `geo/coastline_110m.bin` | binary | Natural Earth 1:110m coastline | variable |

## 0. Conventions that apply to every binary asset

- **Byte order is little-endian everywhere.** No asset uses big-endian.
- **`float32`** is IEEE 754-1985 binary32, little-endian (4 bytes).
- **`float16`** is IEEE 754-2008 binary16 (`half`), little-endian (2 bytes). Kotlin reads it with
  `java.lang.Float.float16ToFloat(java.lang.Short.toShort(...))` after a little-endian `Short` read,
  or `java.nio.ByteBuffer.order(LITTLE_ENDIAN).getShort()`.
- **`uint8` / `uint16` / `uint32`** are unsigned little-endian integers of 1 / 2 / 4 bytes.
- **Strings** are ASCII unless a field says otherwise; fixed-width fields are space- or zero-padded.
- **Angles** are degrees unless a field's name says otherwise. Right ascension is `[0, 360)`; the
  engine boundary converts to hours once (`raHours = raDeg / 15`) — a stored value is never in hours
  **except** in `sky/constellation_lines.json`, the single exception, whose vertices are written
  `[raHours, decDeg]` because that is what the shipped loader reads (§6).
- **Epoch is J2000.0 / ICRS-aligned** for every coordinate in every asset. No stored number is
  precessed; the ephemeris engine performs the time-dependent transformation.
- **Colours**, where they appear, are **linear** (linear-light sRGB primaries unless stated).

---

## 1. `orekit-data/tai-utc.dat`

- **Kind:** plain ASCII text, one record per line, LF line endings, no BOM.
- **Producer:** USNO / IERS Rapid Service/Prediction Center, `https://maia.usno.navy.mil/ser7/tai-utc.dat`.
- **Content:** the leap-second history as `TAI − UTC` records in Orekit's `UTC-TAI.history` /
  `tai-utc.dat` text format (one record per leap second, with the `JD` and `TAI-UTC` fields).
- **Size is asserted: 3,321 B** (sha256
  `3524E1AE34D67E858873A89E59983BBC5BD100221DA898E796C1B36036A310C3`).
- **How the app reads it:** not parsed by slot code. Orekit's `LazyLoadedTimeScales` /
  `UTCScale` provider loads this file as a directory of IERS data; the slot constructs its frames with
  `simpleEOP = true`, which removes the need for `finals.all`. The file exists only so Orekit has a
  `TAI − UTC` history; without it Orekit throws `no IERS UTC-TAI history data loaded`.
- **Refresh:** append-only; re-download from the URL above when a new leap second is announced and
  re-verify size and sha256. See `docs/asset-provenance.md`.

---

## 2. `sky/sky_lut_a.bin` and `sky/sky_lut_b.bin`

Two files with **one identical layout**, produced by `tools/bake-sky-lut.py`. **The app never computes
these tables at runtime and never precomputes them on first launch**; it only samples the two baked
files.

- `sky_lut_a.bin` — the **antisolar** meridian, `Δaz = 180°`. It is the entire sky of the API<33
  fallback path.
- `sky_lut_b.bin` — the **solar** meridian, `Δaz = 0°`. Used only on the API 33+ path, to blend with A
  by `w(Δaz) = (1 + cos Δaz) / 2` in linear light.

### 2.1 The axes

| Axis | Index | Quantity | Samples | Values |
|---|---|---|---|---|
| **A1** | `i1` | solar altitude `ε`, degrees | **61** | `ε(i1) = 10.0 − 0.5 · i1` → `+10.0° … −20.0°` |
| **A2** | `i2` | view elevation `h`, degrees | **91** | `h(i2) = 0.0 + 1.0 · i2` → `0.0° … 90.0°` |

Any `ε` outside `[−20.0, +10.0]` clamps to the end row. `h < 0°` is never sampled (the ground
occludes it).

### 2.2 Header — 64 bytes, at offset 0

| Offset | Length | Field | Type | Value |
|---:|---:|---|---|---|
| 0 | 8 | `magic` | ASCII | `LUMSKY01` (exactly these 8 bytes, no NUL) |
| 8 | 4 | `formatVersion` | `uint32` | **1** |
| 12 | 2 | `a1Count` | `uint16` | **61** |
| 14 | 2 | `a2Count` | `uint16` | **91** |
| 16 | 4 | `a1StartDeg` | `float32` | **+10.0** |
| 20 | 4 | `a1StepDeg` | `float32` | **+0.5** (applied as `ε = a1StartDeg − a1StepDeg · i1`) |
| 24 | 4 | `a2StartDeg` | `float32` | **0.0** |
| 28 | 4 | `a2StepDeg` | `float32` | **+1.0** |
| 32 | 1 | `channelCount` | `uint8` | **3** (order **R, G, B**) |
| 33 | 31 | `reserved` | bytes | **all zero**, padded to the fixed 64-byte header |

A loader **validates the header before any other byte is read**: a wrong magic, `formatVersion`,
axis count, axis bound or `channelCount` is a typed error, never read past. A loader must also require
the file length to be exactly **33,858 B** (§2.5).

### 2.3 `E(ε)` — 61 × `float32`, at offset 64

- Offset `64 + 4 · i1`, length 244 B, `i1 = 0 … 60` in ascending solar-altitude order
  (`+10.0° → −20.0°`).
- **`E(ε)` is the model's absolute linear-light radiance at the zenith** at solar altitude `ε`, in the
  baker's linear units (`W·m⁻²·sr⁻¹` for a unit solar irradiance — model radiance, not calibrated
  photometry). It is the exposure the relative cells are normalised against.

### 2.4 `α_s(ε)` — 61 × `float32`, at offset 308

- Offset `308 + 4 · i1`, length 244 B, same `ε` order as `E`.
- **`α_s(ε)` is the altitude of the Earth-shadow edge, in degrees**, at solar altitude `ε`. It is the
  atmospheric function the renderer uses to place the Earth-shadow wedge (L8, applied *inside* L1's
  evaluation, never as an overlay). It is a function of `ε` only, so it is tabulated rather than
  computed per frame.

### 2.5 Cells — `61 × 91 × 3` × `float16`, at offset 552

- Offset `552 + 2 · (((i1 · 91) + i2) · 3 + c)`, length **33,306 B**.
- Loop order is **`i1` outermost, then `i2`, then channel `c` innermost**; `c = 0,1,2 = R,G,B`.
- Each cell is **`L_rel = L_abs / E(ε)`**: linear-light radiance **relative to the zenith exposure of
  the same `ε` row**, in **linear sRGB** primaries with **D65** white, stored as `float16`.
- **Reconstruction:** `L_abs ≈ L_rel · E[i1]`. Nothing in the render path knows about `E` except this
  multiply. Cell magnitudes are ~`10⁻³ … 3` because of the normalisation, which is exactly the range
  `float16` holds cleanly.
- Colour-space discipline (one stage per line, no stage skipped): the baker integrates in **linear
  spectral → linear CIE XYZ (D65) → linear sRGB**; interpolation at render (bilinear in `ε`/`h`, then
  the azimuth `lerp`) is **linear** and is the only interpolation over sky colour; white balance is one
  fixed linear von Kries matrix; the tonemap is the Narkowicz (2015) ACES approximation; the transfer
  is the sRGB piecewise curve (IEC 61966-2-1), not a 2.2 power; ±0.5/255 tiled blue-noise dither is
  added in the encoded domain; the 8-bit quantisation is the only 8-bit step.

### 2.6 File size (asserted by the baker and by the loader)

```
header   64
E        61 × 4          =   244
α_s      61 × 4          =   244
cells    61 × 91 × 3 × 2 = 33,306
─────────────────────────────────
one file                  = 33,858 B
pair                      = 67,716 B
```

---

## 3. `catalog/stars.bin`

The baked **FK6 ⊕ supplement** star table. No header, no footer.

- **11 bytes per star, fixed, repeated `N` times.**
- `N = 4150 (FK6) + 4 (supplement) = 4154`, file size **45,694 B**. This is the **measured** count:
  `SELECT COUNT(*)` on `I/264/fk6_1` returns **878** and on `I/264/fk6_3` returns **3,272**, and the
  TAP row fetch returns 4,150 rows with 4,150 distinct `FK6` numbers. The `4,151` a source report
  carried is a rounding; the live count governs (design.md § "The FK6 star catalogue count is live").
  The baker asserts the row count **and** the byte size, and both are recomputed from the count
  rather than hard-coded.
- Rows are written **FK6 first (`I/264/fk6_1` in FK6 order, then `I/264/fk6_3`), then the four
  supplement rows in the id order `sirius, arcturus, capella, betelgeuse`**. A loader treats the file
  as an unordered point cloud; ordering is only for reproducibility — `bake-stars.py` reproduces the
  same sha256 for the same inputs.


Per-star record (byte offsets within the 11-byte record):

| Offset | Length | Field | Type | Units / domain |
|---:|---:|---|---|---|
| 0 | 4 | `raDeg` | `float32` | degrees, `[0, 360)`, ICRS J2000.0 |
| 4 | 4 | `decDeg` | `float32` | degrees, `[−90, +90]`, ICRS J2000.0 |
| 8 | 2 | `vmag` | `float16` | V magnitude (can be negative) |
| 10 | 1 | `colourBucket` | `uint8` | `0 … 5`, see below |

**`colourBucket`** is a six-value colour-temperature bucket derived from the star's **B−V** colour
index at bake time. The mapping is fixed and total:

| `colourBucket` | B−V range | Character |
|---:|---|---|
| 0 | `< 0.00` | blue (O/B) |
| 1 | `0.00 … < 0.30` | blue-white (A) |
| 2 | `0.30 … < 0.60` | white (F/G) |
| 3 | `0.60 … < 0.90` | yellow (G/K) |
| 4 | `0.90 … < 1.40` | orange (K) |
| 5 | `≥ 1.40` | red (M) |

The B−V that feeds those thresholds is **not carried in the file**. It is derived at bake time from
the star's **spectral class**, which is the measured input:

1. **FK6 rows** — the spectral class comes from a positional cross-match of `I/264` against
   `B/mk/mktypes` (Skiff 2014, *Catalogue of Stellar Spectral Classifications*), radius **0.01°**
   (36″), executed server-side by the VizieR TAP endpoint. Where several classifications match, the
   **most recently published** one wins (``max`` over the Bibcode, which sorts chronologically
   because the year is its first four characters). Coverage is measured: **3,493 of 4,150** FK6 rows
   match (84.2 %) — a wider 0.02° radius only reaches 3,499, so the remainder is genuinely absent
   from the compilation rather than mis-positioned.
2. **Supplement rows** — the class comes from Wikidata (CC0) `P215` and is checked against the
   `B/mk/mktypes` rows for the same star.
3. **Class → B−V** uses the main-sequence Pecaut & Mamajek (2013) calibration with a luminosity-class
   offset (V 0, IV +0.05, III +0.20, II +0.40, I +0.65, VI −0.10). Carbon, S and late-type labels
   (C, R, N, S, L, T, Y) map straight to bucket 5 and Wolf-Rayet (W) to bucket 0.
4. **A star with no spectral class in either source gets bucket `2`** (neutral). That is **657 FK6
   rows plus 6 rows whose only classification carries no class letter** (`em`), i.e. 663 of 4,154
   rows (16.0 %). The count is printed by `tools/bake-stars.py` and recorded in
   `docs/asset-provenance.md`, so a bake that lost its colour is visible in the output rather than
   in the app.


---

## 4. `catalog/objects.json`

Authored fixed-coordinate deep-sky rows, in memory after parse, never downloaded.

- Top level: `{"schemaVersion": 1, "manifest": {…}, "objects": [ … ]}`.
- `manifest` records `{rowCount, messierCount, nonMessierCount, magnitudeLimitUsed, nonMessierCap,
  selectionRule, generatedBy}` so the count is checkable rather than "about 250".
- `objects` carries **110 Messier rows + 140 non-Messier rows = 250 rows** (§ the selection rule is in
  `openspec/changes/app-lumintra-light-planner/specs/target-catalogue/spec.md`).

Every row has **all 24 fields**, with these names, types and units:

| Field | Type | Unit / domain |
|---|---|---|
| `id` | string | stable ASCII lowercase slug, e.g. `m31`, `ngc7000`, `ic434` |
| `messier` | string \| null | `"M31"` with prefix, else null |
| `ngc` | string \| null | `"NGC 224"`, else null |
| `ic` | string \| null | `"IC 434"`, else null |
| `name` | string | English common/proper name |
| `nameRu` | string \| null | Russian name (hand-authored) |
| `type` | enum | one of `galaxy, galaxy_group, globular_cluster, open_cluster, emission_nebula, reflection_nebula, planetary_nebula, supernova_remnant, dark_nebula, asterism, double_star` |
| `raDeg` | number | degrees, `[0, 360)`, ICRS J2000.0 |
| `decDeg` | number | degrees, `[−90, +90]`, ICRS J2000.0 |
| `epoch` | string | constant `"J2000.0"` |
| `magV` | number \| null | integrated V magnitude, where a V value exists |
| `magB` | number \| null | B magnitude, only when no V exists |
| `magSystem` | `"V"` \| `"B"` \| null | which of the two is authoritative |
| `magKind` | `"integrated"` \| `"peak"` \| `"unknown"` | what the magnitude measures |
| `sizeMajArcmin` | number \| null | arcminutes, major axis |
| `sizeMinArcmin` | number \| null | arcminutes, minor axis |
| `positionAngleDeg` | number \| null | degrees, `[0, 180)`, north through east |
| `aliases` | string[] | may be empty |
| `commonNames` | string[] | may be empty |
| `sourceIds` | object | `{wikidata, heasarc, simbad}` — the references' own ids (null where absent) |
| `verification` | object | `{srcA, srcB, deltaArcmin}` — the two independent sources and their separation |
| `magDisputed` | boolean | true when two sources' magnitudes disagree by more than 0.50 |
| `notes` | string \| null | free text; discrepancies and unresolved questions |

Notes that a loader must honour:

- **RA is degrees, never hours.** `raHours = raDeg / 15` is derived at load and is never stored.
- `planet`, `moon` and `sun` **never appear** in this file; solar-system bodies are computed records.
- No comparison may mix a V with a B value, or an integrated with a peak magnitude.

---

## 5. `catalog/constellations.json`

The 88 IAU constellations — a **region** record, a different shape from an object row.

- Top level: `{"schemaVersion": 1, "manifest": { … }, "constellations": [ … ]}` with exactly **88**
  rows. `manifest` is provenance only — the bake's inputs, its cross-check readings and the output
  hash. A reader requires only `constellations`; the key is written so the numbers behind each row
  travel with the file rather than living in a script's console output.
- Row: `{id, nameEn, nameRu, nameLatin, abbrev, raCenterDeg, decCenterDeg, areaRank}`.
  - `id` — ASCII lowercase slug (e.g. `orion`).
  - `abbrev` — the three-letter IAU abbreviation (e.g. `Ori`), unique.
  - `raCenterDeg` / `decCenterDeg` — the constellation's **centre**, degrees, J2000.0. It is
    **computed** from the IAU boundary data, not read from a centres table: the area-weighted
    centroid direction of the boundary region, sampled along the constant-RA / constant-Dec arcs
    before integration (an unsampled ring is chorded across the boundary and mis-measures). The
    published "centre" table is a *different quantity* — close to a bounding-box midpoint, and
    outside its own constellation for 9 of the 88 — so it is used only as a cross-check with a
    reported deviation. The shipped value is asserted to lie **inside** the region it names, which
    for a two-part constellation is not automatic: Serpens' union centroid falls in the gap between
    Caput and Cauda, so the largest ring's centroid is used for it and the rule is recorded in
    `manifest.crossCheck.centreLargestRingFallbackRows`.
  - `areaRank` — the published **IAU area rank 1 … 88** (1 = largest area, Hydra).

A constellation is never classified as rise/set/never-up/circumpolar and never appears in the target
list, its magnitude ordering or its magnitude filter. `nameEn`, `nameLatin` and `abbrev` are the IAU
archive's table (boundary data public domain; IAU WGSN names CC BY 4.0, credited `IAU WGSN`);
`areaRank` is the published IAU rank; `nameRu` is the Wikidata `rdfs:label` at `ru` for the
constellation item (Wikidata structured data is **CC0 1.0**, so it carries no attribution
obligation, and the credit line names the sources the app renders).

---

## 6. `sky/constellation_lines.json`

The IAU / Sky & Telescope figure polylines.

**Top level is a bare map**, keyed by the IAU three-letter abbreviation, with **no wrapper and no
metadata key of any kind**:

```json
{
  "And": [ [ [0.1398, 29.0904], [1.0, 31.5] ], [ [ … ] ] ],
  "UMa": [ [ … ] ]
}
```

- Keyed by the **IAU three-letter abbreviation** (matching `constellations.json.abbrev`), all **88**
  of them. A constellation whose figure set carries no lines is present with an **empty array** —
  `"Men": []` today — so "this constellation has no figure" is stated in the asset instead of being
  indistinguishable from a key the baker forgot.
- Each value is an **array of polylines**; each polyline is an **array of `[raHours, decDeg]`
  vertices** (right ascension `[0, 24)`, declination `[−90, +90]`), J2000.0 / ICRS.
- **Unit divergence, resolved: the vertices are in HOURS, not degrees.** This is the **only** asset
  in the pack whose stored right ascension is not degrees, and the exception to §0. It is not a
  choice — it is what the shipped loader reads:

  ```startLine:121:endLine:126:app/src/main/java/com/gexetr/lumintralightplanner/data/SkyAssetSource.kt
  data class FigureVertex(val raHours: Double, val decDeg: Double)
  ...
  * `assets/sky/constellation_lines.json`: a JSON object keyed by the IAU
  * three-letter abbreviation, whose every value is an array of polylines, each polyline an array of
  * `[raHours, decDegrees]` vertices.
  ```

  `specs/target-catalogue/spec.md` ("Constellations are 88 regions") writes the vertex the same way,
  `[raHours, decDegrees]`. An earlier revision of this section claimed degrees on the reading that
  the app stores RA in degrees everywhere; the loader is the authority and it does not, so the
  degrees claim was wrong and is corrected here and recorded in `docs/asset-provenance.md`.

- **Why there is no wrapper, and where the credit actually travels.** `SkyAssetSource` iterates the
  root object's members and requires **every** value to be a JSON array of polylines
  (`SkyAssetSource.kt:226–233`). A sibling key holding a string, a number or an object therefore
  makes the **whole asset `Malformed`**, and the scene silently loses every figure. So a
  `credit`/`license`/`source` wrapper is not a convention this file declines to follow — it is a
  document the loader rejects. The source, the licence, the credit string and the transformations
  live in **three places beside the file**:

  1. `docs/asset-provenance.md` — source URL, licence, credit, bake inputs and output hash;
  2. `R.string.credit_iau_constellations` — the string the app renders in Credits, reproduced **in
     full**, with no IAU logo;
  3. the loader's own KDoc, which quotes the credit so a reader of the parser meets it there.

  The credit string is:
  `Constellation figures: IAU and Sky & Telescope (Roger Sinnott, Rick Fienberg; patterns by Alan MacRobert) — CC BY 4.0`

---

## 7. `sky/milky_way_eso0932a.webp`

- **Content:** the ESO "Milky Way panorama" `eso0932a` (S. Brunier), an **equirectangular** panorama.
- **Format:** WebP (lossy), 24-bit, no alpha.
- **Dimensions:** at most **4096 × 2048**, aspect exactly **2:1** (equirectangular: longitude
  `−180° … +180°` maps left → right, latitude `+90° … −90°` maps top → bottom).
- **Size:** < 4 MB.
- **Credit `ESO/S. Brunier`** must be rendered beside the image wherever it is drawn, and in Credits.
- Any transformation (resize, crop, re-encode, any generative edit) is declared in
  `docs/asset-provenance.md`.

## 8. `sky/moon_svs.webp`

- **Content:** the NASA SVS CGI Moon Kit colour map (LROC colour mosaic), **equirectangular**.
- **Format:** WebP (lossy), 24-bit, no alpha. Small (a 2:1 texture, 2048 × 1024 or smaller).
- **Credit:** "Moon imagery: NASA Scientific Visualization Studio (public domain)"; no NASA insignia,
  no implied endorsement.
- Any transformation (resize, re-encode) is declared in `docs/asset-provenance.md`.

---

## 9. `geo/coastline_110m.bin`

The Natural Earth **1:110m coastline** (public domain), simplified, in a compact binary.

All integers little-endian; coordinates are `float32` degrees.

| Offset | Length | Field | Type | Value |
|---:|---:|---|---|---|
| 0 | 8 | `magic` | ASCII | `LUMGEO01` |
| 8 | 4 | `formatVersion` | `uint32` | **1** |
| 12 | 4 | `lineCount` | `uint32` | number of polylines |
| 16 | 4 | `pointCount` | `uint32` | total `float32` **pairs** across all lines |
| 20 | 4 | `reserved` | `uint32` | 0 |
| 24 | … | lines | — | § below |

Each line: a `uint32 pointCount_n` followed by `pointCount_n` pairs of `float32 (lonDeg, latDeg)`,
`lonDeg ∈ [−180, +180]` (east positive), `latDeg ∈ [−90, +90]` (north positive), WGS84 geographic.
Lines are not closed; a renderer closes them by drawing `stroke` (not `fill`) — the coastline is a
line, not a polygon, and the app draws only polylines. The total file size is
`24 + Σ_n (4 + 8 · pointCount_n)` bytes.

Simplification: Douglas–Peucker at a document tolerance, applied at bake time to the Natural Earth
1:110m geometry; the resulting total point count is recorded in `docs/asset-provenance.md`.

---

## 10. Regeneration

Every asset is reproducible from the committed scripts (see `docs/asset-provenance.md` for URLs,
licences, dates, hashes and refresh procedures):

```powershell
# No fetch script exists for tai-utc.dat: it is pinned from the local minimal-data tree and
# refreshed by downloading <https://maia.usno.navy.mil/ser7/tai-utc.dat> over the file (see
# docs/asset-provenance.md section 2.1). There is no `tools/fetch-orekit-data.py`.
python tools/bake-sky-lut.py           # -> assets/sky/sky_lut_a.bin, sky_lut_b.bin
python tools/fetch-stars.py            # -> scratch: FK6 I/264 + B/mk spectral classes
python tools/bake-stars.py             # -> assets/catalog/stars.bin
python tools/fetch-objects.py          # -> scratch: candidate list + per-row sources
python tools/fetch-cluster-centres.py  # -> scratch: MWSC / Cantat-Gaudin / Hunt-Reffert centres
python tools/bake-objects.py           # -> assets/catalog/objects.json
python tools/fetch-constellations.py   # -> scratch: IAU table + boundary rings, S&T figures, Wikidata ru names
python tools/bake-constellations.py    # -> assets/catalog/constellations.json, sky/constellation_lines.json
python tools/bake-milkyway.py          # -> assets/sky/milky_way_eso0932a.webp
python tools/bake-moon.py              # -> assets/sky/moon_svs.webp
python tools/bake-geo.py               # -> assets/geo/coastline_110m.bin
```

There is no `tools/fetch-imagery.py`: the two webp assets are produced by `bake-milkyway.py` and
`bake-moon.py`, each of which downloads its own source. `tools/_probe.py` is an exploratory
source-schema probe (`HANDOFF.md` §4) and bakes nothing.

All scratch and cache writes go to `<repo>/.kiln/scratch/lumintra-assets/`; nothing is written to the
system temp directory.
