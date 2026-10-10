# Bundled-asset provenance

Every asset shipped inside the APK, with where it came from, what licence it carries, what was
done to it, and how to reproduce it. Formats are in `docs/ASSET-FORMATS.md`; this file is the
record of the **sources and the transformations**, and of the places where a source, a brief or a
sibling document disagreed with the shipped artifact.

**The byte sizes and SHA-256 digests below were measured on the files as they sit on disk**, by

```powershell
$root = "apps\2026-10-09-lumintra-light-planner\app\src\main\assets"
Get-ChildItem -Path $root -Recurse -File |
  ForEach-Object { "{0}  {1}  {2}" -f $_.FullName.Substring($root.Length+1),
    $_.Length, (Get-FileHash $_.FullName -Algorithm SHA256).Hash.ToLower() }
```

Re-run that command and compare; a digest that no longer matches means the asset changed and this
document is stale about it. The **retrieval date is 2026-10-09** for every remote source.

Two of the fourteen files embed a generation date — `catalog/constellations.json` and
`catalog/objects.json` (`generatedAt`, `generatedBy`) — and neither takes it from the wall clock:
`bake-constellations.py` reads `evidence["retrieved"]` from the scratch evidence file and
`bake-objects.py` sets `BUILD_DATE` as a literal, so the date is a **pinned input, not a
re-bake artefact**. Every download the bakers perform goes through `common.fetch`, which is
cache-first (`refresh=False` by default), so with `.kiln/scratch/lumintra-assets/cache/` and
`constellation-evidence.json` still present a re-bake is expected to reproduce these digests
byte for byte. That expectation is **not** re-run in this session — the digests below were measured
on the files as they ship; the reproducibility argument rests on reading those two call sites and
`common.fetch`, not on a second bake.

---

## 1. Inventory

| Path (under `app/src/main/assets/`) | Bytes | SHA-256 |
|---|---:|---|
| `orekit-data/tai-utc.dat` | 3,321 | `3524e1ae34d67e858873a89e59983bbc5bd100221da898e796c1b36036a310c3` |
| `sky/sky_lut_a.bin` | 33,858 | `0e399fb9e1f69903782cf5d2f9225be7a279623371f88bee02cd18d87b983932` |
| `sky/sky_lut_b.bin` | 33,858 | `166d8d76b5dfe7f61c1c8354f0a69b529fe9e8f1ee54cab142bdbc6a094969af` |
| `catalog/stars.bin` | 45,694 | `472507684728f8cb3f3bddca61e5b874947ed8ff1bb0763ea33950034e86520a` |
| `catalog/objects.json` | 207,172 | `f26779cfb2d8ac0c44f0ff9d2f8e14f083d055b1598dd30fefa53d538dd9a8af` |
| `catalog/constellations.json` | 21,155 | `4bf398f59436bcc385236baf0ced87dab6a3a1d71b38cc4883d776235129aa3e` |
| `sky/constellation_lines.json` | 21,893 | `c75a6ccb131e51d40c7e68caac6a30ed1b57035f4b36d491db7c9829eff0bac6` |
| `sky/milky_way_eso0932a.webp` | 1,422,904 | `6acc22ae566eeec821e6028638c9356c799be77e9a8c879533872e9f05d2e1dd` |
| `sky/moon_svs.webp` | 282,518 | `562dd4573dfad86f7ef4395e37af57a93bb8e705dd0c89255fe092420bff344f` |
| `geo/coastline_110m.bin` | 37,712 | `a1478d810697e475bcf204e84156cfda1e2a59632327b6cb8b9f053ac18a0710` |
| `licenses/orekit-LICENSE.txt` | 11,358 | `cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30` |
| `licenses/orekit-NOTICE.txt` | 1,661 | `dc8ea1d4aed11cba83d3f38de87518b274260fb529f01072af9d6c553ce8c440` |
| `licenses/hipparchus-LICENSE.txt` | 24,364 | `734cdb2a3c7b796bc4cbdb0b63816bb21be811718273659f90cd6e00bdea6323` |
| `licenses/hipparchus-NOTICE.txt` | 358 | `d3f82a2956ae7ddf9d810baa084df421ec2dfef584648c98ad065996914e52b3` |

---

## 2. Per-asset record

### 2.1 `orekit-data/tai-utc.dat` — 3,321 B

- **What it is.** The USNO/IERS leap-second table: the `TAI − UTC` step function as a list of
  `YYYY MON D =JD … TAI-UTC= … S` records. Orekit reads it to place UTC on the TAI scale.
- **Source.** Pinned from the local Orekit minimal-data tree,
  `.kiln/scratch/positioning-2026-10-08/minimal-data/orekit-data/tai-utc.dat`. Upstream:
  <https://maia.usno.navy.mil/ser7/tai-utc.dat> (United States Naval Observatory).
- **Licence.** The table is a US Government publication; **public domain**. It carries no
  copyright notice and no attribution obligation. (The *Orekit* library that reads it is
  Apache-2.0, which is what `licenses/orekit-*` are for — separate from this data file.)
- **Transformation.** **None** — copied byte for byte. Verified, not assumed:

  ```text
  src 3524E1AE34D67E858873A89E59983BBC5BD100221DA898E796C1B36036A310C3
  dst 3524E1AE34D67E858873A89E59983BBC5BD100221DA898E796C1B36036A310C3   identical: True
  ```

  Content check: it opens at `1961 JAN 1 =JD 2437300.5 TAI-UTC= 1.4228180 S …` and its last record
  is `2017 JAN 1 =JD 2457754.5 TAI-UTC= 37.0 S …`, i.e. the current (2026) leap-second count of
  37 s is present and no later leap second exists to be missing.
- **Refresh.** Download <https://maia.usno.navy.mil/ser7/tai-utc.dat> over the file. A new leap
  second is announced by IERS Bulletin C, so this is a **per-leap-second** action, not a
  per-release one; nothing in the app needs re-baking when it happens.

### 2.2 `sky/sky_lut_a.bin`, `sky/sky_lut_b.bin` — 33,858 B each

- **What they are.** The twilight tables: for each solar depression `ε` (the `a1` axis,
  +10.0 … −20.0 step 0.5, 61 samples) a zenith exposure `E(ε)`, a solar-meridian azimuth `α_s(ε)`,
  and a `61 × 91 × 3` grid of `float16` linear-light radiance over the view-elevation axis
  (0 … 90 step 1.0, 91 samples). Layout: `docs/ASSET-FORMATS.md` §2.
- **Source.** **Not a downloaded asset.** Both files are *computed* by `tools/bake-sky-lut.py`,
  which implements a Bruneton & Neyret (2008) style radiative-transfer model of the atmosphere:
  spherical geometry, Rayleigh scattering with the 1/λ⁴ spectrum, Mie with the
  Cornette–Shanks/Henyey–Greenstein phase (`g = 0.8`), and an ozone Chappuis-band absorption layer
  with a triangular profile. `LUT_A` is the antisolar meridian (Δaz = 180°), `LUT_B` the solar
  meridian (Δaz = 0°). No third-party table is bundled and no model output is redistributed, so
  there is no upstream licence — the artifact is this repository's own.
- **Licence.** The *model's coefficients* are the published figures from Bruneton & Neyret 2008
  (used as scientific constants, not redistributed as a dataset); the baked tables are original
  output of this repository and carry no third-party licence.
- **Transformation.** Model → `float16` grid, with `E(ε)` = the CIE luminance `Y` of the zenith
  view so the grid is relative to the zenith exposure; the integration is linear-spectral → linear
  CIE XYZ (D65) → linear sRGB via the published `XYZ_TO_SRGB` matrix, and cells outside the sRGB
  gamut are mapped into the non-negative octant before clipping (`gamut_map_linear_srgb`).
  The bake reported `clipped cells=0`, i.e. nothing was clamped to zero after the gamut map.
- **Three divergences from the paper, and their honest limits — recorded here because
  `tools/bake-sky-lut.py` names this section as where they live:**
  1. **The atmosphere ceiling is 120 km, not Bruneton's 60 km** (`RT = 6480.0` against
     `Rg + 60`). With a 60 km ceiling the terminator annulus — at altitude
     `R(1/cos ε − 1)`, so 35 km at ε = −6°, 88 km at −9.5°, 315 km at −18° — is not contained, and
     the bake measured every row below ≈ −16° integrating to **exactly zero** while the −6°…−15°
     rows came out red, because the only lit air the model could see was marginal high-altitude air
     lit by a path-grazing, Rayleigh-reddened beam. 120 km contains the whole annulus for every ε
     in the table. This is a deliberate divergence from the published model's geometry.
  2. **Multiple scattering is an isotropic successive-orders approximation, not the paper's full
     4-D precompute.** The first-order ambient, integrated over the whole sky at the observer,
     drives an isotropic source along each ray; the higher orders are folded in through an
     effective single-scattering albedo. This is the model's largest approximation and it is
     *not* equivalent to the paper's 4-D table.
  3. **The fold is a *finite* geometric series, `MS_ORDERS = 4`.** The infinite sum
     `amb/(1 − w₀)` is deliberately not taken: with an isotropic ambient and `w₀ → 1` in the blue
     (ozone is nearly transparent there) it diverges far past what an isotropic approximation can
     justify. Four terms keep the physically correct ordering (blue scatters more than red) with a
     bounded amplification of at most 4×.
- **What is not claimed.** Nobody has measured these tables against a real twilight photograph. The
  numbers are internally consistent (monotone `E(ε)`, `α_s` pinned to 90° where the sun's azimuth
  is degenerate) and the file layout is asserted byte-exactly, but the *photometric* accuracy of the
  sky is **UNVERIFIED** — the honest limit is the model, and the app's bar is stated in
  `docs/J-sky-realism-and-bar.md`.
- **Refresh.** `python tools/bake-sky-lut.py` (≈minutes; needs `numpy`). Deterministic — the same
  inputs give the same bytes.

### 2.3 `catalog/stars.bin` — 45,694 B

- **What it is.** 4,154 stars × 11 B = RA `float32`, Dec `float32`, V `float16`, colour-bucket
  `uint8` (`docs/ASSET-FORMATS.md` §3).
- **Source.** FK6 (Wielen et al.), fetched over TAP from VizieR table `I/264` (`fk6_1` 878 rows +
  `fk6_3` 3,272 rows = **4,150 rows**), plus a **4-row hand-authored supplement** for Sirius,
  Arcturus, Capella and Betelgeuse — the bright stars FK6 omits, its brightest row being
  `alpha Car` at V = −0.62. Spectral classes came from VizieR `B/mk` (Skiff 2014) and, for the
  supplement, from the papers whose bibcodes are recorded per row.
- **Licence.** **FK6 is CC0 1.0.** `B/mk` is a CDS/VizieR catalogue and is used only as a
  verification reading. The supplement is this repository's own row set; each supplement row was
  checked against a second source (VizieR `B/mk`), and the measured separations are 0.00056′ …
  0.00126′.
- **Transformation.** RA/Dec → `float32`; V → `float16`; a **colour bucket 0…5** derived from the
  spectral class via the Pecaut & Mamajek (2013) main-sequence B–V calibration with a
  luminosity-class offset for giants and supergiants. Bucket thresholds are in
  `docs/ASSET-FORMATS.md` §3.
- **Honest limit.** The **spectral class is the measured input; the B–V value and the bucket are
  an INFERRED calibration** (`tools/stars_common.py` labels it so). The bucket is only a sprite
  selector (8 magnitude bins × 6 buckets = 48 sprites), so a mis-classified star changes which
  sprite is drawn and nothing else. Carbon/S/R/N/L/T/Y stars have no dwarf calibration at all and
  are mapped straight to bucket 5; Wolf-Rayet to 0; every other non-calibrated label takes the
  documented neutral default and is counted in the bake report (657 FK6 rows carry no spectral
  class, 6 carry one that does not map).
- **Observation, not this document's claim.** `tools/fetch-stars.py` recorded **4,150** FK6 rows
  and the bake shipped **4,154**, while `design.md` and the slot brief both state FK6 holds
  **4,151** rows and describe the live TAP count as governing over the recon's 4,150. The
  discrepancy is between the shipped table and the prose, and it is flagged here rather than
  silently reconciled; the shipped artifact's row count is asserted by its own baker (4,150 + 4).
- **Refresh.** `python tools/fetch-stars.py && python tools/bake-stars.py`.

### 2.4 `catalog/objects.json` — 207,172 B

- **What it is.** Exactly **250** deep-sky rows: all 110 Messier objects plus 140 non-Messier
  NGC/IC showpieces, each with `raDeg`/`decDeg`, `magV`, sizes, `aliases`, `nameRu`, and a
  `verification` object carrying two source ids and the measured `deltaArcmin`.
- **Source.** SIMBAD TAP (basic/allfluxes/ident), NASA HEASARC TAP (`messier` table), Wikidata
  structured data (**CC0 1.0**: P528/P3083, P6257 RA, P6258 Dec, P1215 magnitude), and VizieR B/ocl
  (Dias et al.) — plus three cluster-centre catalogues (MWSC, Cantat-Gaudin & Anders 2020, Hunt &
  Reffert 2023) used only to fix open-cluster centres. The full URLs, bibcodes and the column used
  from each are in the asset's own `manifest.sources` and `manifest.clusterCentreSources`.
- **Licence.** Wikidata CC0 1.0; SIMBAD, HEASARC and VizieR are consulted as **verification
  readings** and no table is redistributed. Coordinates, designations and magnitudes are facts.
- **Transformation.** Selection, not conversion: the 140 non-Messier rows are the brightest that
  pass `magV ≤ 10.0` **and** land within tolerance of a second independent source;
  `magnitudeLimitUsed: 10.0` (the 10.5 relaxation was not needed). Rows outside tolerance were
  re-sourced or dropped; `magDisputed` is set when two magnitude sources differ by > 0.50 — never
  averaged.
- **Denylist honoured** (`design.md`): OpenNGC, HYG (CC BY-SA), NGC 2000.0, Hipparcos/Tycho
  (non-commercial), Stellarium (GPL) and the Gaia panorama (CC BY-SA 3.0 IGO) are not used.
- **Refresh.** `python tools/fetch-objects.py && python tools/bake-objects.py`.

### 2.5 `catalog/constellations.json` — 21,155 B

- **What it is.** 88 rows, `{id, nameEn, nameRu, nameLatin, abbrev, raCenterDeg, decCenterDeg,
  areaRank}`, plus a provenance `manifest` (`docs/ASSET-FORMATS.md` §5).
- **Sources** — all four recorded in `manifest.sources`, all fetched on 2026-10-09:
  - **Names, Latin names, abbreviations** — the IAU archive's own 88-row table,
    <https://iauarchive.eso.org/public/themes/constellations/> (IAU WGSN names CC BY 4.0, credited
    `IAU WGSN`).
  - **Boundary rings** — <https://iauarchive.eso.org/static/public/constellations/txt/<abbr>.txt>,
    the Delporte-derived IAU boundary vertices. **Public domain (IAU).**
  - **`areaRank`** — the published IAU area rank, read from
    <https://en.wikipedia.org/wiki/List_of_constellations_by_area> (the tabulated numbers are IAU
    facts, public domain; the page is a cross-check source and is not bundled).
  - **`nameRu`** — Wikidata `rdfs:label` @ `ru` (Wikidata structured data is **CC0 1.0**; no
    attribution obligation). A missing label is a hard failure in the baker — never a
    transliteration.
- **Transformation.**
  - **`raCenterDeg`/`decCenterDeg` are computed from the boundary rings**, not read from a centres
    table: the **area-weighted centroid direction** of the region, by the gnomonic line integrals of
    `tools/constellation_region.py`, on rings sampled at 0.2° first (§3 below). The published
    "centre" column is used only as a cross-check, and the reason is measured: **it falls outside
    its own constellation for 9 of the 88** (Cam, Cep, Cir, Dra, Eri, Hya, Pup, Sco, Ser), because
    it is close to a bounding-box midpoint while the area centroid is not. Ours is asserted to lie
    **inside** the region it names — 88/88.
  - **`areaRank` is copied, never re-derived**, because the spec asks for the *published* rank. Our
    own area is the independent cross-check of it.
- **Refresh.** `python tools/fetch-constellations.py && python tools/bake-constellations.py`
  (offline after the first fetch; the evidence is cached under `.kiln/scratch/lumintra-assets/`).

### 2.6 `sky/constellation_lines.json` — 21,893 B

- **What it is.** The IAU / Sky & Telescope figure polylines: a **bare map** of all **88** IAU
  abbreviations to arrays of polylines of `[raHours, decDeg]` vertices. 758 source `conline`
  segments → 199 polylines → 957 vertices. `"Men": []` — see below.
- **Source.** `constellation.figures.txt`, compiled by Martin Krzywinski
  (<https://mk.bcgsc.ca/constellations/data/constellation.figures.txt>,
  <http://mkweb.bcgsc.ca/constellations>), transcribing into RA/Dec (degrees, J2000) the figure
  designs **Alan MacRobert** made for **Sky & Telescope** and which appear on the IAU charts. Its
  own header also credits Roger Sinnott (consultation, original S&T data file) and Paulina Rowicka
  (the Microscopium "wishbone"); Microscopium is Krzywinski's own addition and Telescopium an
  extension of his.
- **Licence.** **CC BY 4.0**, credit reproduced **in full** and with **no IAU logo**:

  > Constellation figures: IAU and Sky & Telescope (Roger Sinnott, Rick Fienberg; patterns by Alan
  > MacRobert) — CC BY 4.0

- **Where the credit is carried — and why it is not a key in the JSON.** `SkyAssetSource`
  iterates the file's root object and requires **every** value to be an array of polylines, so a
  `credit`/`license`/`source` sibling key makes the *whole asset* `Malformed` and silently deletes
  every figure. The credit therefore travels in three places beside the file: the resource
  `R.string.credit_iau_constellations` (verified present at `values/strings.xml:676`,
  `translatable="false"`, character-for-character the string above), the loader's own KDoc, and this
  document. `design.md` requires the figures to ship "carrying the exact credit … reproduced in
  full"; that is satisfied, and the carrier is the app's credits, not a JSON key the parser rejects.
- **Transformation.** Two steps, both reversible-by-inspection:
  1. **Units** — the source is RA/Dec **degrees**; the asset's vertices are `[raHours, decDeg]`
     (`raDeg / 15`, 6 decimal places). This is the **only** asset whose stored RA is not degrees
     (`docs/ASSET-FORMATS.md` §0, §6).
  2. **Segment joining** — pairwise connections are joined into polylines by **shared HR
     identity**, not float equality: a segment ending on the midpoint between two stars has an
     endpoint key like `6938/6934`, and the two segments that meet there carry the *same string*.
     Joining is a rendering convenience only — the loader converts each vertex to alt/az
     independently, so no interpolation across right ascension happens and the 0 h seam needs no
     special case. Measured: across all 758 segments the **maximum node-position disagreement is
     0.000000000°** — no two segments disagree about a star, so no join was guessed.
- **`"Men": []` is the source's fact, not a bake failure.** The source carries a `conpath Men`
  record with **no connections at all** and **zero `conline Men` segments** (measured: 87 distinct
  constellation tags on `conline` lines, `Men` absent), even though its header says the set is
  complete for all 88. Writing the key with an empty array makes "this constellation has no figure"
  legible in the asset itself instead of being indistinguishable from a constellation the baker
  forgot. The loader reads `[]` as zero polylines and does **not** report the asset malformed.
- **The rejected source.** `dcf21/constellation_lines_iau.dat` is **not** used: its description says
  CC BY 4.0 while its repository footer says GPL v3, and a measured licence conflict is not a
  licence (`design.md` § "Sky assets and attribution").
- **Refresh.** Part of `python tools/bake-constellations.py`.

### 2.7 `sky/milky_way_eso0932a.webp` — 1,422,904 B

- **Source.** ESO image `eso0932a`, "The Milky Way panorama" (S. Brunier):
  <https://cdn.eso.org/images/large/eso0932a.jpg> — the 8,228,817 B, 6000 × 3000 original.
  <https://www.eso.org/public/images/eso0932a/>
- **Licence.** **CC BY 4.0**, credit **`ESO/S. Brunier`**, rendered beside the image and in Credits.
  A credit line is the whole obligation; the app must not imply ESO endorsement.
- **Transformation.** Resized to **4096 × 2048** and re-encoded as **lossy WebP, quality 78**
  (1,422,904 B, under the 4 MB bar). **`aiModified: false`** — no pixel content was changed by any
  generative step; the operation is a resample and a re-encode, and the equirectangular 2:1 aspect
  is preserved exactly.
- **Refresh.** `python tools/bake-milkyway.py` (re-downloads; requires Pillow).

### 2.8 `sky/moon_svs.webp` — 282,518 B

- **Source.** NASA SVS CGI Moon Kit colour map (LROC colour mosaic),
  <https://svs.gsfc.nasa.gov/vis/a000000/a004700/a004720/lroc_color_poles_2k.tif> — 3,339,438 B,
  2048 × 1024.
- **Licence.** **Public domain (NASA)**. Acknowledged in Credits as
  `Moon imagery: NASA Scientific Visualization Studio (SVS), public domain`
  (`values/strings.xml:675`) — with **no NASA insignia** and **never credited to JPL-Caltech**.
- **Transformation.** Re-encoded as lossy WebP, quality 82; dimensions unchanged at 2048 × 1024.
  Resample and re-encode only; **no pixel content changed**.
- **Refresh.** `python tools/bake-moon.py`.

### 2.9 `geo/coastline_110m.bin` — 37,712 B

- **Source.** Natural Earth 1:110m physical coastline,
  <https://naciscdn.org/naturalearth/110m/physical/ne_110m_coastline.zip>. Archive members and the
  raw counts are in `.kiln/scratch/lumintra-assets/geo-bake-report.json`.
- **Licence.** **Public domain.** Natural Earth states no rights reserved.
- **Transformation.** Douglas–Peucker simplification at **0.05°** tolerance: 134 lines / 5,128 raw
  points → **134 lines / 4,644 points**, then the compact binary of `docs/ASSET-FORMATS.md` §9
  (24-byte header + per line a `uint32` count and `float32 (lonDeg, latDeg)` pairs). Lines are
  **not** closed — the renderer strokes them. The header itself states the same counts
  (`LUMGEO01`, version 1, 134 lines, 4,644 points), so the inventory is checkable without parsing the
  body. Final extent read back from the shipped bytes: `lon [−180.0, 180.0]`, `lat [−85.609, 83.645]`.
  The raw Natural Earth geometry's easternmost longitude is 180.0000004, but `float32` has ~2.1 × 10⁻⁵
  resolution at 180, so **it stores as exactly 180.0** — the extent must be quoted as the file reads,
  not as the pre-encoding `float64` value.
- **Refresh.** `python tools/bake-geo.py`.

### 2.10 `licenses/*` — four files

- **What they are.** The Apache-2.0 licence text and NOTICE for **Orekit** (`orekit-LICENSE.txt`,
  `orekit-NOTICE.txt`) and for **Hipparchus** (`hipparchus-LICENSE.txt`, `hipparchus-NOTICE.txt`),
  the two libraries the ephemeris engine is built from. Apache-2.0 requires both the licence text
  and the NOTICE to travel with a binary distribution, so they ship inside the APK rather than only
  in the repository.
- **Source.** The upstream distributions of the exact versions the slot pins, as they appear at
  <https://gitlab.orekit.org/orekit/orekit> and <https://github.com/Hipparchus-Math/hipparchus>.
- **Licence.** **Apache License 2.0** (both). The NOTICE files carry the upstream attributions
  (CS GROUP, Bruce R. Bowman, Luc Maisonobe, David A. Vallado et al. for Orekit; the Apache
  Software Foundation and CS Systèmes d'Information for Hipparchus).
- **Transformation.** None — copied verbatim.
- **Refresh.** Replace from the pinned versions' distributions when the engine dependency moves.

---

## 3. PROOF — the constellation geometry

`tools/constellation_region.py` names this section as where its measurements live. Everything here
was measured by `python tools/bake-constellations.py` on the fetched IAU rings, and the figures
below are the bake's own printed cross-check lines.

**The method.** Each closed boundary ring is projected gnomonic-ally about its own mean direction, so
a ring that encircles a pole becomes a plain polygon around the projection origin and no branch cut
or special case is needed. Area and centroid come from four line integrals around the projected
polygon (Green's theorem on the sphere), evaluated with 24-node Gauss–Legendre quadrature; `φ` is
integrated as a continuous signed angle.

**Edges are arcs, so they are sampled.** The IAU boundary is drawn along lines of **constant
declination** (most edges) or constant **right ascension**. A constant-declination arc is *not* a
great circle, so in the gnomonic plane it is a curve, and the straight chord between its endpoints is
not that curve — the polygon differs from the region by an area whose **sign depends on which way the
arc bulges**, away from the interior or toward it. Measured on the shipped rings: **35 of the 88**
under-report with chords and **53 over-report**, the gross discrepancy is **368.16 deg²**, and the
worst single case is Octans, bounded by one long parallel, which loses **14.65 %** of its area to the
chord (248.826 → 291.544 deg², against a published 291.045). `densify()` samples every edge at
`DEFAULT_STEP_DEG = 0.2` before the integration.

**The whole-sky sum hides this, and must not be quoted as if it did not.** Chording all 88 rings sums
to **41,252.89 deg²**, only **0.07 deg²** off the sphere's 41,252.96, because the 35 losses and 53
gains very nearly cancel (net −0.374 deg²). That near-total is a property of the errors cancelling
across the sky, not evidence that the chords are adequate: a single region can still be 14.65 % wrong.
The per-region measurement above is the one that matters, and it is why densification is not optional.

**Where 0.2° was chosen — the step-sensitivity measurement.** `region()` over all 88 rings:

| step (deg) | worst published-area deviation | median | whole-sky sum (deg²) |
|---:|---|---:|---:|
| 1.00 | 0.4430 % (Cha) | 0.0220 % | 41,252.5129 |
| 0.50 | 0.4534 % (Cha) | 0.0229 % | 41,252.5130 |
| **0.20** | **0.4545 % (Cha)** | **0.0234 %** | **41,252.5129** |
| 0.10 | 0.4545 % (Cha) | 0.0235 % | 41,252.5129 |
| 0.05 | 0.4545 % (Cha) | 0.0235 % | 41,252.5129 |
| 0.02 | 0.4545 % (Cha) | 0.0235 % | 41,252.5129 |

Everything below 0.2° agrees to four decimals, and a 0.02° step reproduces the 0.2° whole-sky sum to
0.0001 deg². The **−0.4483 deg² residual (41,252.5129 against the sphere's 41,252.9612, −0.0011 %) is
not sampling error and never shrinks**: it is the IAU rings not tiling the sphere exactly, because
the published vertex lists round each Delporte corner.

**An independent second algorithm.** `fan_area_sq_deg()` computes the same area a completely
different way — the ring cut into spherical triangles about a fixed apex, each triangle's solid angle
from van Kampen's formula `2·atan2(a·(b×c), 1 + a·b + b·c + c·a)`. The bake runs both, on the same
rings but with the fan **re-sampled at 0.05°** so the two do not share a discretisation, and gates on
their agreement. Measured over all 88: **worst disagreement 0.0072 %, on Ursa Minor** — 1 part in
~14,000, on the one ring in the set that encircles a pole.

**The centre, and why the published one is not it.** The published centres table is a *different
quantity* — close to a bounding-box midpoint — and the measurement says so plainly: of the 88
published centres, **9 fall outside the constellation they name** (`Cam`, `Cep`, `Cir`, `Dra`,
`Eri`, `Hya`, `Pup`, `Sco`, `Ser`). The shipped centres are **88/88 inside** their own region,
asserted per row. The median separation between the two quantities is 65.2′, the worst 1,148.4′
(Cepheus), and 63/88 agree within 2° — a distribution that is reported, never gated, because gating
on it would gate on a disagreement that is expected.

**The one constellation where the centroid is not the answer.** Serpens is the only constellation
whose region is **two disjoint rings** (Caput and Cauda, measured: 2 rings of 15 and 25 vertices), so
the area-weighted centroid of the union lands in the gap between them — a direction inside
Ophiuchus, which is not where a Serpens label belongs. One rule covers it, not a hand-kept exception
list: use the union centroid **unless it falls outside the region**, in which case use the centroid
of the region's largest ring. The rule fired for `Ser` and only `Ser`
(`manifest.crossCheck.centreLargestRingFallbackRows`), and the 88/88 inside-assertion means a future
geometry change cannot quietly reintroduce the case.

**Area rank is copied and cross-checked.** Our computed areas reproduce the published **rank order
1…88 with zero disagreements** — measured by ranking the 88 computed areas and comparing each rank to
the published one, since the bake itself only carries the published rank through — and the worst
per-constellation area deviation from the published value is **0.4545 % (Chamaeleon)**.

**A third method that shares nothing with either.** The two integrals above are both evaluations of
surface integrals; agreement between them is agreement about a discretisation. A **Monte Carlo
rejection sampler** shares none of that: it draws directions uniformly on the sphere, tests them with
the shipped membership predicate `inside()`, and takes the hit fraction times the band's exact
spherical area. `inside()` re-projects the whole ring per call, so the probe hoists that work into a
closure **and asserts the hoisted version against the shipped `inside()` on 2,000 random directions
per predicate — measured: 0 disagreements in all four cases** (raw and sampled, both constellations),
so the fast path is the shipped geometry and not a re-derivation of it. Measured, `N = 200,000`:

| | region() spherical | MC on **sampled** rings | MC on **raw** rings | published |
|---|---:|---:|---:|---:|
| Octans | 291.544 | **292.70 ± 1.18** (+0.98σ) | 249.51 ± 1.11 (−37.7σ) | 291.045 (+1.40σ) |
| Ursa Minor | 255.769 | **256.05 ± 1.79** (+0.15σ) | 243.73 ± 1.75 (−6.9σ) | 255.864 (+0.10σ) |

The sampler agrees with the spherical integral and with the published area on both, decisively
rejects the chorded rings, and rejects the plane integral on Ursa Minor. It cannot resolve the
**0.007 %** the fan check resolves — 200,000 samples resolve ~0.4 % — so it is evidence about the
*direction* of the answer, not about the fourth decimal. What it rules out is the possibility that
both integrals share a common error, which is the one failure two integrals from one worker cannot
see. (The first version of this probe tested only the **raw** rings, which measures the chorded
polygon rather than the arc-bounded region and therefore *agreed* with the raw integral; sampling the
rings for the membership test is what makes the test about the shipped region.)

**A method that was wrong and is not used.** `common.ring_area_sq_deg` / `region_centre_deg` do the
integral in the (RA, Dec) **plane**. Measured over all 88 rings it agrees with the spherical method
to within **0.0002 %** on **86** of them, and the exceptions are the two whose boundary encircles a
celestial pole — neither vertex list reaches ±90°, but both regions contain the pole:

| constellation | plane | spherical | published | error |
|---|---:|---:|---:|---:|
| **Ursa Minor** | 269.286 | 255.769 | 255.864 | **+5.2851 %** |
| Octans | 291.565 | 291.544 | 291.045 | +0.0069 % |

The spherical method lands **0.037 %** from the published UMi value where the plane one is 5.25 %
away, and the Monte Carlo above independently rejects the plane number at **−7.4σ**. Nothing in the
shipped path calls it. *Two claims are withdrawn here: that the plane method put UMi's centroid
outside its own region (measured, both methods leave exactly one centroid outside — `Ser`, the
disjoint two-ring constellation the largest-ring rule handles for both), and an earlier "every
constellation that encircles a pole" — measured, it is **two rows out of 88**, and Octans is wrong by
only 0.007 % while UMi is wrong by 5 %.*

---

## 4. Divergences, conflicts and gaps — recorded, not smoothed over

| # | What disagreed | Resolution |
|---|---|---|
| 1 | **Figure vertex units.** The slot brief says the polylines are in RA/Dec **degrees**; `design.md` is silent; `specs/target-catalogue/spec.md` says `[raHours, decDegrees]`. | **Hours**, because the **shipped loader** reads hours: `FigureVertex(raHours, decDeg)` (`SkyAssetSource.kt:121`, KDoc at `:126`). A degrees asset would render every figure at 1/15 of its true RA spread — silently wrong — and the loader is not this worker's to edit. The spec and the loader agree; the brief is the outlier. `ASSET-FORMATS.md` §0/§6 corrected to match, and §6's earlier claim that the loader used degrees was wrong and is withdrawn here. |
| 2 | **The credit string "in the asset".** The brief requires the file to *carry* `Constellation figures: …`. | It cannot be a key: `SkyAssetSource` requires **every** root value to be a polyline array, so a `credit` key makes the whole asset `Malformed` and deletes all 88 figures. The credit is carried character-for-character in `R.string.credit_iau_constellations` (`values/strings.xml:676`), in the loader's KDoc, and here. `design.md` requires the credit to travel with the figures — it does. |
| 3 | **The asset's top-level shape.** `ASSET-FORMATS.md` §6 specified a wrapper (`schemaVersion`, `credit`, `license`, `licenseUrl`, `source`, `units`, `coordinates`, `figures`). | Corrected to the **bare map** the loader and the spec require. The wrapper would have been rejected outright. |
| 4 | **Mensa.** The figures source header says "complete with figures for all 88", but `conpath Men` is empty and there are zero `conline Men` records. | Measured, then encoded: `"Men": []`. The gap is the **source's**, and the asset now states it instead of hiding it. |
| 5 | **FK6 row count.** `design.md` and the brief say 4,151; `tools/fetch-stars.py` recorded 4,150; `stars.bin` ships 4,154 total (4,150 FK6 + 4 supplement). | **Flagged, not silently reconciled** — it is another worker's artifact, outside this worker's remit. See §2.3. |
| 6 | **`ASSET-FORMATS.md` §10 named two scripts that do not exist** — `tools/fetch-orekit-data.py` and `tools/fetch-imagery.py` — and omitted four that do (`fetch-stars.py`, `fetch-cluster-centres.py`, `bake-milkyway.py`, `bake-moon.py`). | §10 rewritten to the real command list. There is no fetch script for `tai-utc.dat`: it is pinned from the local minimal-data tree and refreshed from the USNO URL (§2.1). The two webp assets come from `bake-milkyway.py` and `bake-moon.py`, each of which downloads its own source; `tools/_probe.py` is an exploratory probe and bakes nothing. |
| 7 | **Stale claims in `constellation_region.py`'s own docstrings.** The `DEFAULT_STEP_DEG` comment said the whole-sky sum was "0.0000 % off the sphere" — measured, it is −0.0011 %. A comment in `fan_area_sq_deg` carried three unmeasured numbers. The module docstring then repeated two of the §3 errors: that a chord always "cuts inside the region and under-reports it" (one-directional; measured 35 under / 53 over) and that the plane method over-counted UMi "by 5.246 %". | All replaced with the measured values in §3, including the two found in this pass (the chording direction and 5.285 %). A number in a docstring is still a claim, and a docstring is where the next reader forms their expectation. |
| 8 | **Three figures in §3 were stale, and one conclusion was wrong.** Octans' chord loss was 14.5 % (measured 14.65 %); "the 88 unsampled areas sum to 41,243.22 deg² — a 9.74 deg² hole" (measured: 41,252.89, a 0.07 deg² gap, because 35 regions under-report and 53 over-report and the errors very nearly cancel); and the plane method was said to over-count UMi "by 5.246 % (+13.42 deg²) **and** put UMi's centroid outside its own region". | All three re-measured. The first two corrected above; the **containment half of the third is withdrawn**, because it does not reproduce: over all 88 rings the plane method leaves exactly one centre outside (`Ser`, for the disjoint-ring reason both methods share), and UMi's plane centre is inside. The area error alone is the defect. Recorded rather than deleted, because the withdrawn sentence was load-bearing in a rejection argument. |
| 9 | **The whole-sky sum was being read as evidence the chords were adequate.** | Explicitly contradicted in §3: the near-total is the 35 losses and 53 gains cancelling, not the chords being fine, and a single region can still be 14.65 % wrong. Measured gross discrepancy 368.16 deg², net −0.374 deg². |
| 10 | **§2.9 quoted the coastline extent as `lon [−180.0, 180.0000004]`.** | That is the **pre-encoding `float64` value** from `geo-bake-report.json` (`bounds.lon = [−180.0, 180.00000044181039]`), not what ships: the `float32` format resolves ~2.1 × 10⁻⁵ at 180, so the stored value is **exactly 180.0**. Re-read from the shipped bytes and corrected. The edit is faintly ironic given row 1 of this table and is recorded for the same reason: a number copied from an intermediate report is a claim about the report, not about the artifact. |
| 11 | **§3 described the plane integral's failure as applying to constellations "that encircle a pole".** | Measured, it is **2 rows of 88**, not a class: UMi is wrong by 5.2851 % and Octans by 0.0069 %, and every other row agrees to ≤ 0.0002 %. Both regions contain a pole but neither vertex list reaches ±90°, so "encircles a pole" was a mechanism inferred from one failing example rather than a measured set — and it over-generalised. Replaced with the two-row table. This is the failure mode the rule is written against, applied to this document: a plausible explanation for a number is not a measurement of the number's scope. |

---

## 5. Refresh, in one place

```powershell
cd apps\2026-10-09-lumintra-light-planner

python tools\fetch-orekit-data.py  # does not exist -- see section 2.1; refresh from the USNO URL
python tools\bake-sky-lut.py       # -> sky/sky_lut_a.bin, sky/sky_lut_b.bin
python tools\fetch-stars.py        # -> scratch: FK6 I/264 + B/mk spectral classes
python tools\bake-stars.py         # -> catalog/stars.bin
python tools\fetch-objects.py      # -> scratch: candidate rows + per-row sources
python tools\fetch-cluster-centres.py
python tools\bake-objects.py       # -> catalog/objects.json
python tools\fetch-constellations.py   # -> scratch: IAU table + rings, S&T figures, Wikidata ru labels
python tools\bake-constellations.py    # -> catalog/constellations.json, sky/constellation_lines.json
python tools\bake-milkyway.py      # -> sky/milky_way_eso0932a.webp
python tools\bake-moon.py          # -> sky/moon_svs.webp
python tools\bake-geo.py           # -> geo/coastline_110m.bin
```

All scratch and cache writes go to `<repo>/.kiln/scratch/lumintra-assets/`; nothing is written to the
system temp directory (`.cursor/rules/workspace-hygiene.mdc`). Each baker asserts its own output
size and prints its SHA-256, so a refresh that changes the bytes is visible in its own output
rather than only here.

---

## 6. What this document cannot establish

- It proves **where each byte came from, under which licence, and what was done to it**. It cannot
  prove the *science*: nobody has compared the twilight tables to a measured sky, and the FK6
  colour buckets are an inferred calibration (§2.2, §2.3).
- **§3's three geometry methods are all this worker's own code.** The gnomonic integral, the van
  Kampen fan and the Monte Carlo sampler share no algebra, and the sampler's membership predicate was
  asserted against the shipped `inside()` — but all three are written here, and their agreement is
  therefore agreement *within one implementation*, not with an independent authority. The only
  genuinely external check is the **published IAU areas**, which the spherical method matches to
  0.4545 % worst-case (Chamaeleon) and 0.0234 % median. Two workers could share a misreading of the
  boundary format and never see it in any of these three numbers.
- The digests are a **snapshot**. They are evidence that the files have not changed since
  2026-10-09, and nothing more; re-run the command at the top before trusting them for a release.
- For the assets whose bakers belong to other workers (§2.2, §2.3, §2.4, §2.7, §2.8, §2.9), the
  **structural numbers below were re-derived from the shipped bytes** — the LUT grid is `552 B` of
  header plus `61 × 91 × 3` `float16`; `objects.json` really is 250 rows and they really are 110
  Messier plus 140 other; the coastline's 134 lines / 4,644 points and its header's own repeated
  counts; the webp dimensions 4096 × 2048 and 2048 × 1024; `tai-utc.dat`'s 41 records opening at
  1961 and closing at `2017 JAN 1 … TAI-UTC= 37.0`. Their **source URLs, licences and
  transformations**, by contrast, are read from their own recorded reports and scripts and were
  **not** independently confirmed, and their bakes were **not** re-run.
- Nothing here is a **legal opinion**. Where a licence obligation is stated (BY 4.0 credit lines,
  Apache-2.0 NOTICE, public domain), this document records that the obligation is discharged by a
  credit line or a notice file that is actually shipped; whether that discharge is sufficient in any
  particular jurisdiction is not a question this file answers.
