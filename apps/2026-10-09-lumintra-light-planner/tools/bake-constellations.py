#!/usr/bin/env python3
"""bake-constellations.py — write the two constellation assets from the fetched evidence.

Input (offline, no network): `.kiln/scratch/lumintra-assets/constellation-evidence.json`,
written by `tools/fetch-constellations.py`.

Output — exactly two files, and the formats are the **shipped loaders'**, not a wish:

  1. `app/src/main/assets/catalog/constellations.json`

         {"schemaVersion": 1, "manifest": {...}, "constellations": [
            {"id","nameEn","nameRu","nameLatin","abbrev",
             "raCenterDeg","decCenterDeg","areaRank"}, ... 88 rows ]}

     Read by `CatalogueRepository.readConstellations()`, which reads the root object's
     `constellations` array and, per row, requires `abbrev`, `raCenterDeg` and `decCenterDeg`,
     tolerating the rest (`specs/target-catalogue/spec.md` § "Constellations are 88 regions,
     not 88 points" makes all eight mandatory, so all eight are written). Every field is
     sourced, never invented:

       * `nameLatin`, `abbrev`, `nameEn` — the IAU archive's own 88-row table (Latin name,
         three-letter abbreviation, English name).
       * `areaRank` — the **published** IAU area rank, read from the table that reproduces the
         IAU values verbatim, because the spec asks for the published rank and not for one we
         derived. Our own area is the independent cross-check of the geometry, and its deviation
         from the published value is reported in `manifest.crossCheck`.
       * `raCenterDeg` / `decCenterDeg` — the **area-weighted centroid direction** of the
         region, integrated over the Delporte-derived IAU boundary rings. See "The centre is
         the area's, not the box's" below.
       * `nameRu` — the Wikidata `rdfs:label` @ `ru` for the constellation item (Wikidata
         structured data is CC0 1.0). A missing label is a hard failure, never a transliteration.

  2. `app/src/main/assets/sky/constellation_lines.json`

         {"And": [[[raHours, decDeg], [raHours, decDeg], ...], ...], ...}

     A **bare map keyed by the IAU three-letter abbreviation** — no wrapper, no `schemaVersion`,
     no credit key. `SkyAssetSource.constellationFigures()` iterates the root object's members
     and requires *every* value to be an array of polylines, so a metadata key would make the
     whole asset `Malformed` and silently delete the figures from the scene. The source, the
     licence and the credit therefore live beside the file, in `docs/asset-provenance.md`, and
     the credit itself is rendered from `R.string.credit_iau_constellations`.

     **Units: RA in HOURS, Dec in degrees** — the spec's `[raHours, decDegrees]`, and what the
     loader's `FigureVertex(raHours, decDeg)` holds. This is the one asset in the pack whose RA
     is *not* degrees; `docs/ASSET-FORMATS.md` §6 and `docs/asset-provenance.md` both record it.

     Segments are joined into polylines by **shared HR identity**, not by float equality: the
     source gives an endpoint as an HR number, or as `6938/6934` for a segment ending on the
     midpoint between two stars, and two segments that meet at that midpoint carry the *same
     string*. Joining is a rendering convenience only — the loader converts every vertex to
     alt/az independently, so no interpolation across right ascension ever happens and the
     0h seam needs no special case — but it keeps the asset an honest figure rather than a bag
     of chords.

## The boundary rings must be sampled before either integral runs

An IAU constellation boundary is drawn along arcs of **constant right ascension** and **constant
declination** (Delporte 1930). The archive's rings store only an arc's two endpoints. Both of the
integrals below join consecutive vertices with **great circles**, and a great circle between the
ends of a constant-declination arc dips *towards the pole* — so an unsampled ring does not
describe the boundary, it chords across it. Measured on the shipped rings: Octans (a polar cap
bounded by one long parallel) loses **14.5 %** of its area to the chord, and the 88 areas sum to
**41243.22 deg^2** against the sky's **41252.96 deg^2**, a 9.74 deg^2 hole that no amount of
quadrature accuracy can close.

`constellation_region.densify()` therefore samples every arc at `DEFAULT_STEP_DEG = 0.2` before
the integration, and both checks are run on the same sampled ring:

  * **the area**, by `region()` — a gnomonic projection about the ring's mean direction plus four
    line integrals around the projected polygon (Green's theorem on the sphere);
  * **the centre**, by `centre_deg()` — the 3-D centroid direction of that same integral, i.e.
    the point that balances the region's area, which is what a constellation label wants.

Both are validated against measurements whose answers are known before the bake reads a slot:
the full sky's area, the published area of each of the 88 constellations, and the fact that the
centroid must lie **inside** the region it names.

## The centre is the area's, not the box's

The widely reproduced "centres" table is not the centre of the *area*: for an elongated or
multi-part constellation it is close to the midpoint of the bounding box and can fall **outside
the figure entirely**, and for a constellation split across the sky it is not in either part.
Serpens is the extreme case — one constellation in two disjoint halves (Caput, Cauda) — but it is
not the only one. Those rows are still used, as a **cross-check with a reported deviation**
(`manifest.crossCheck.centreVsPublishedMaxArcmin`), because a value we can disagree with is
evidence; they are not used as the shipped centre. The shipped centre is guaranteed inside the
region by construction, and the bake asserts that per row rather than trusting it.
"""
from __future__ import annotations

import json
import math
import sys
import unicodedata
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import constellation_region as CR  # noqa: E402

from common import (  # noqa: E402
    SCRATCH,
    sep_arcmin,
    sha256_file,
    write_json,
)

SLOT = Path(__file__).resolve().parent.parent
CONSTELLATIONS_OUT = SLOT / "app" / "src" / "main" / "assets" / "catalog" / "constellations.json"
FIGURES_OUT = SLOT / "app" / "src" / "main" / "assets" / "sky" / "constellation_lines.json"
EVIDENCE = SCRATCH / "constellation-evidence.json"

# ---- sampling. 0.2 deg is the module default; the fan check re-samples finer so the two
# integrals are not agreeing merely because they share one discretisation.
DENSIFY_STEP_DEG = CR.DEFAULT_STEP_DEG
FAN_STEP_DEG = 0.05

# ---- tolerances. Every one of these compares two *independently produced* quantities, so a
# breach means the geometry, the join or the source table is wrong and the bake must stop.
AREA_TOLERANCE_PCT = 1.0                 # our area vs the published area, per constellation
GNOMONIC_FAN_TOLERANCE_PCT = 0.05        # our area vs our own second method, per constellation
SKY_SUM_TOLERANCE_SQ_DEG = 0.5           # the 88 areas must sum to the sphere
# Not a tolerance: the arcmin radius inside which we count the published centre as "the same
# neighbourhood". The published centre is a *different quantity* -- close to a bounding-box
# midpoint, which for an elongated, polar or two-part constellation has no reason to sit at the
# area's balance point -- so a large deviation is not evidence of an error and gating on it would
# be gating on a disagreement we expect. It is reported, with the distribution, instead.
CENTRE_NEIGHBOURHOOD_ARCMIN = 120.0
# Two segments sharing an HR key must agree on that star's position this closely.
NODE_AGREEMENT_DEG = 1e-3

# The sky's area, from the sphere: 4*pi steradians. `constellation_region` computes the same
# number and this asserts the identities 88 disjoint regions must satisfy.
SKY_AREA_SQ_DEG = 4.0 * math.pi * CR.SR_TO_DEG2

CREDIT = ("Constellation figures: IAU and Sky & Telescope (Roger Sinnott, Rick Fienberg; "
          "patterns by Alan MacRobert) — CC BY 4.0")


def slug(latin: str) -> str:
    """`Ursa Major` -> `ursa-major`, `Boötes` -> `bootes`: the row's ASCII lowercase id."""
    folded = unicodedata.normalize("NFKD", latin)
    ascii_only = "".join(c for c in folded if not unicodedata.combining(c))
    out = []
    for ch in ascii_only.lower():
        if ch.isalnum():
            out.append(ch)
        elif out and out[-1] != "-":
            out.append("-")
    return "".join(out).strip("-")


def join_trails(segments: list) -> tuple:
    """`([node_path, ...], max_node_disagreement_deg)`, every segment used exactly once.

    A node is an HR key (or an `a/b` midpoint key). Its position must be the same whichever
    segment reached it, and the disagreement is returned so the caller can report it.
    """
    adj: dict = {}
    pos: dict = {}
    disagree = 0.0

    for index, seg in enumerate(segments):
        ends = []
        for end in ("a", "b"):
            hr, ra, dec = seg[end]
            if hr in pos:
                pra, pdec = pos[hr]
                disagree = max(disagree, abs(pra - ra), abs(pdec - dec))
            else:
                pos[hr] = (ra, dec)
            ends.append(hr)
        adj.setdefault(ends[0], []).append((index, ends[1]))
        adj.setdefault(ends[1], []).append((index, ends[0]))

    if disagree > NODE_AGREEMENT_DEG:
        raise SystemExit(f"two segments disagree on a star's position by {disagree} deg")

    used = [False] * len(segments)
    trails: list = []
    for start in sorted(adj):
        while True:
            step = next((e for e in adj[start] if not used[e[0]]), None)
            if step is None:
                break
            path = [start]
            current = start
            while True:
                step = next((e for e in adj[current] if not used[e[0]]), None)
                if step is None:
                    break
                index, other = step
                used[index] = True
                path.append(other)
                current = other
            if len(path) >= 2:
                trails.append(path)

    if any(not u for u in used):
        raise SystemExit(f"{used.count(False)} segments were never used by a trail")
    return trails, disagree


def main() -> int:
    evidence = json.loads(EVIDENCE.read_text(encoding="utf-8"))
    table = evidence["table"]
    boundaries = evidence["boundaries"]
    figures = evidence["figures"]
    published = evidence["published"]
    names_ru = evidence["wikidataNames"]

    if len(table) != 88 or len(boundaries) != 88 or len(published) != 88:
        raise SystemExit(f"evidence is not 88 rows: "
                         f"{len(table)}/{len(boundaries)}/{len(published)}")
    abbrevs = [row["abbrev"] for row in table]
    if len(set(abbrevs)) != 88:
        raise SystemExit("IAU abbreviations are not unique")

    # ------------------------------------------------------------ figures asset
    #
    # Mensa is the one constellation whose `conpath` record in the source is *empty*: the file
    # carries `conpath Men` with no connections at all and zero `conline Men` segments, so its
    # figure set has no lines for it. The key is written anyway, as an empty polyline list, so
    # the asset says "this constellation has no figure" rather than being indistinguishable from
    # a constellation the bake forgot. `SkyAssetSource.constellationFigures()` reads `[]` as
    # zero polylines and does not report the asset Malformed, so the empty list is the honest
    # encoding and not a second failure mode.
    positions = {abbrev: {end[0]: (end[1], end[2])
                          for seg in segments for end in (seg["a"], seg["b"])}
                 for abbrev, segments in figures.items()}
    document: dict = {}
    polyline_total = vertex_total = 0
    worst_disagreement = 0.0
    for abbrev in sorted(set(figures) | set(abbrevs)):
        trails, disagree = join_trails(figures[abbrev]) if abbrev in figures else ([], 0.0)
        worst_disagreement = max(worst_disagreement, disagree)
        polylines = []
        for trail in trails:
            polylines.append([[round(ra_deg / 15.0, 6), round(dec_deg, 6)]
                              for ra_deg, dec_deg in (positions[abbrev][node] for node in trail)])
        document[abbrev] = polylines
        polyline_total += len(polylines)
        vertex_total += sum(len(p) for p in polylines)
    empty_figure = sorted(a for a, p in document.items() if not p)
    if sorted(document) != sorted(abbrevs):
        raise SystemExit(f"the figures asset is not the 88 IAU abbreviations: "
                         f"extra {sorted(set(document) - set(abbrevs))}, "
                         f"missing {sorted(set(abbrevs) - set(document))}")
    write_json(FIGURES_OUT, document, compact=True)

    # ------------------------------------------------------ constellations asset
    rows = []
    worst_area = (0.0, "")
    worst_fan = (0.0, "")
    area_sum = 0.0
    centre_deviations = []          # (arcmin, abbrev) for every row, sorted and summarised
    published_centre_outside = []
    centroid_outside = []
    centre_fallback_rows = []       # rows whose union centroid fell outside, so the rule fired

    for row in sorted(table, key=lambda r: r["abbrev"]):
        abbrev, latin = row["abbrev"], row["latin"]
        rings = boundaries[abbrev]

        # Sample the constant-RA / constant-Dec boundary arcs before integrating; see the
        # module docstring -- a chorded ring is not the boundary the IAU defined.
        sampled = CR.densify(rings, DENSIFY_STEP_DEG)
        result = CR.region(sampled)
        area = result["areaSqDeg"]
        ra_centre, dec_centre = CR.centre_deg(sampled)
        ra_centre %= 360.0

        # Serpens is the one constellation whose region is two disjoint rings (Caput and Cauda),
        # so the area-weighted centroid of the *union* can land in the gap between them -- a
        # direction inside Ophiuchus, which is not where a label for Serpens belongs. The rule
        # is one rule and not a hand-kept exception list: use the union centroid, unless it
        # falls outside the region, in which case use the centroid of the region's largest ring.
        # Every row's shipped centre is asserted to be inside its own region below, so a
        # geometry change cannot quietly reintroduce the case.
        centre_fallback = False
        if not CR.inside_any(rings, ra_centre, dec_centre):
            centre_fallback = True
            biggest = max(sampled, key=lambda ring: CR.region([ring])["areaSqDeg"])
            ra_centre, dec_centre = CR.centre_deg([biggest])
            ra_centre %= 360.0

        # The independent check: the same sampled ring, a different algorithm (van Kampen's
        # spherical-triangle fan), re-sampled finer so the two do not share a discretisation.
        fan_area = sum(CR.fan_area_sq_deg(ring)
                       for ring in CR.densify(rings, FAN_STEP_DEG))
        fan_pct = abs(area - fan_area) / area * 100.0
        if fan_pct > worst_fan[0]:
            worst_fan = (fan_pct, abbrev)
        area_sum += area

        # The shipped centre must lie inside the region it names.
        if not CR.inside_any(rings, ra_centre, dec_centre):
            centroid_outside.append(abbrev)
        if centre_fallback:
            centre_fallback_rows.append(abbrev)

        pub = published[abbrev]
        area_pct = abs(area - pub["areaSqDeg"]) / pub["areaSqDeg"] * 100.0
        if area_pct > worst_area[0]:
            worst_area = (area_pct, abbrev)
        pub_ra, pub_dec = pub["raHours"] * 15.0, pub["decDeg"]
        centre_deviations.append((sep_arcmin(ra_centre, dec_centre, pub_ra, pub_dec), abbrev))
        if not CR.inside_any(rings, pub_ra, pub_dec):
            published_centre_outside.append(abbrev)

        name_ru = names_ru.get(latin)
        if not name_ru:
            raise SystemExit(f"no Wikidata ru label for {latin} ({abbrev})")
        rows.append({
            "id": slug(latin),
            "nameEn": row["english"],
            "nameRu": name_ru,
            "nameLatin": latin,
            "abbrev": abbrev,
            "raCenterDeg": round(ra_centre, 5),
            "decCenterDeg": round(dec_centre, 5),
            "areaRank": pub["rank"],
        })

    # -------------------------------------------------------------- the assertions
    centre_deviations.sort()
    centre_max = centre_deviations[-1]
    centre_median = centre_deviations[len(centre_deviations) // 2][0]
    centre_near = sum(1 for arcmin, _ in centre_deviations
                      if arcmin <= CENTRE_NEIGHBOURHOOD_ARCMIN)
    centre_far = [{"abbrev": abbrev, "arcmin": round(arcmin, 1)}
                  for arcmin, abbrev in centre_deviations[-5:]][::-1]

    if len({r["id"] for r in rows}) != 88:
        raise SystemExit("constellation ids are not unique")
    if sorted(r["areaRank"] for r in rows) != list(range(1, 89)):
        raise SystemExit("areaRank is not exactly 1..88")
    if abs(area_sum - SKY_AREA_SQ_DEG) > SKY_SUM_TOLERANCE_SQ_DEG:
        raise SystemExit(f"the 88 areas sum to {area_sum:.4f} deg^2, not the sky's "
                         f"{SKY_AREA_SQ_DEG:.4f} (off by {area_sum - SKY_AREA_SQ_DEG:+.4f})")
    if worst_area[0] > AREA_TOLERANCE_PCT:
        raise SystemExit(f"own area disagrees with the published value by {worst_area[0]:.4f}% "
                         f"on {worst_area[1]}")
    if worst_fan[0] > GNOMONIC_FAN_TOLERANCE_PCT:
        raise SystemExit(f"the two area methods disagree by {worst_fan[0]:.4f}% on {worst_fan[1]}")
    if centroid_outside:
        raise SystemExit(f"the computed centre is outside its own region for {centroid_outside}")

    write_json(CONSTELLATIONS_OUT, {
        "schemaVersion": 1,
        "manifest": {
            "rowCount": 88,
            "generatedBy": "tools/bake-constellations.py",
            "generatedAt": evidence["retrieved"],
            "sources": evidence["sources"],
            "crossCheck": {
                "ownAreaVsPublishedMaxPct": round(worst_area[0], 4),
                "ownAreaVsPublishedWorstAbbrev": worst_area[1],
                "gnomonicVsFanMaxPct": round(worst_fan[0], 4),
                "gnomonicVsFanWorstAbbrev": worst_fan[1],
                "areaSumSqDeg": round(area_sum, 4),
                "skyAreaSqDeg": round(SKY_AREA_SQ_DEG, 4),
                "centreVsPublishedMaxArcmin": round(centre_max[0], 1),
                "centreVsPublishedMaxAbbrev": centre_max[1],
                "centreVsPublishedMedianArcmin": round(centre_median, 1),
                "centreVsPublishedWithin2deg": centre_near,
                "centreVsPublishedFarthest": centre_far,
                "publishedCentreInsideOwnRegion": 88 - len(published_centre_outside),
                "computedCentreInsideOwnRegion": 88,
                "publishedCentreOutsideOwnRegion": published_centre_outside,
                "centreLargestRingFallbackRows": centre_fallback_rows,
            },
        },
        "constellations": rows,
    })

    figures_bytes = FIGURES_OUT.read_bytes()
    report = {
        "figures": {
            "constellationsWithFigures": len(document) - len(empty_figure),
            "constellationsWithEmptyFigure": empty_figure,
            "constellationKeys": len(document),
            "sourceSegments": sum(len(v) for v in figures.values()),
            "polylines": polyline_total,
            "vertices": vertex_total,
            "maxNodeDisagreementDeg": worst_disagreement,
        },
        "crossCheck": {
            "areaMaxPct": round(worst_area[0], 4),
            "areaWorstAbbrev": worst_area[1],
            "gnomonicVsFanMaxPct": round(worst_fan[0], 4),
            "gnomonicVsFanWorstAbbrev": worst_fan[1],
            "areaSumSqDeg": round(area_sum, 4),
            "skyAreaSqDeg": round(SKY_AREA_SQ_DEG, 4),
            "centreVsPublishedMaxArcmin": round(centre_max[0], 1),
            "centreVsPublishedMaxAbbrev": centre_max[1],
            "centreVsPublishedMedianArcmin": round(centre_median, 1),
            "centreVsPublishedWithin2deg": centre_near,
            "centreVsPublishedFarthest": centre_far,
            "publishedCentreOutsideOwnRegion": published_centre_outside,
            "computedCentreOutsideOwnRegion": centroid_outside,
            "centreLargestRingFallbackRows": centre_fallback_rows,
            "densifyStepDeg": DENSIFY_STEP_DEG,
            "fanStepDeg": FAN_STEP_DEG,
        },
        "constellationsJson": {
            "path": CONSTELLATIONS_OUT.relative_to(SLOT).as_posix(),
            "bytes": CONSTELLATIONS_OUT.stat().st_size,
            "sha256": sha256_file(CONSTELLATIONS_OUT),
        },
        "constellationLinesJson": {
            "path": FIGURES_OUT.relative_to(SLOT).as_posix(),
            "bytes": len(figures_bytes),
            "sha256": sha256_file(FIGURES_OUT),
        },
        "credit": CREDIT,
        "rows": rows,
    }
    write_json(SCRATCH / "constellation-bake-report.json", report)

    print(f"{report['constellationsJson']['path']}: 88 rows, "
          f"{report['constellationsJson']['bytes']} B, "
          f"sha256 {report['constellationsJson']['sha256']}")
    print(f"{report['constellationLinesJson']['path']}: {len(document)} keys, "
          f"{report['figures']['sourceSegments']} source segments -> {polyline_total} polylines, "
          f"{vertex_total} vertices, {len(figures_bytes)} B, "
          f"sha256 {report['constellationLinesJson']['sha256']}")
    print(f"  constellations with an empty figure in the source: {empty_figure}")
    print(f"  max node-position disagreement: {worst_disagreement:.9f} deg")
    print(f"  88 areas sum to {area_sum:.4f} deg^2 vs the sky's {SKY_AREA_SQ_DEG:.4f} "
          f"({area_sum - SKY_AREA_SQ_DEG:+.4f})")
    print(f"  own area vs published: {worst_area[0]:.4f}% worst ({worst_area[1]})")
    print(f"  gnomonic vs fan:       {worst_fan[0]:.4f}% worst ({worst_fan[1]})")
    print(f"  centre vs published:   {centre_median:.1f} arcmin median, "
          f"{centre_max[0]:.1f} arcmin worst ({centre_max[1]}); "
          f"{centre_near}/88 within {CENTRE_NEIGHBOURHOOD_ARCMIN:.0f} arcmin")
    print(f"  centres inside their own region: ours 88/88, published "
          f"{88 - len(published_centre_outside)}/88 "
          f"(outside: {published_centre_outside})")
    print(f"  largest-ring centre rule fired for: {centre_fallback_rows}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
